/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-09-17 20:53:22 -0700 (Mon, 17 Sep 2012) $' 
 * '$Revision: 30698 $'
 * 
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies
 * of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 * THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 * CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS.
 *
 */

package org.geon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.FilePortParameter;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

//////////////////////////////////////////////////////////////////////////
//// Scp
/**
 * Connects to a remote host using Ssh2 protocol.
 * 
 * Error conditions this actor must respond robustly to:
 * 
 * Wrong identity file given. Host unreachable. Login unsuccessful. Session dies
 * prematurely.
 * 
 * This actor will keep the session open until it receives a different username
 * and host combination.
 * 
 * @author Efrat Jaeger
 * @version $Id: Scp.java 30698 2012-09-18 03:53:22Z barseghian $
 * @category.name remote
 * @category.name connection
 * @category.name file transfer
 */

/**
 * 
 * 
 * FIXME
 * THIS ACTOR SHARES DUPLICATE CODE WITH Ssh2Exec. BEFORE MAKING CHANGES HERE
 * FACTOR OUT THE DUPLICATED CODE FROM BOTH CLASSES.
 * 
 * 
 */

public class Scp extends TypedAtomicActor {

	/**
	 * Construct an SCP actor with the given container and name. Create the
	 * parameters, initialize their values.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public Scp(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// initialize our variables
		_jsch = new JSch();
		_setIdentities = new HashSet();

		// create all the ports
		host = new PortParameter(this, "host");
		host.setStringMode(true);
		user = new PortParameter(this, "user");
		user.setStringMode(true);
		direction = new StringParameter(this, "direction");
		direction.setDisplayName("scp to/from remote");
		direction.addChoice("TO");
		direction.addChoice("FROM");
		direction.setExpression("TO");
		localFilePath = new FilePortParameter(this, "localFilePath");
		remoteFilePath = new FilePortParameter(this, "remoteFilePath");
		identity = new FilePortParameter(this, "identity");
		stdout = new TypedIOPort(this, "stdout", false, true);
		stderr = new TypedIOPort(this, "stderr", false, true);
		returncode = new TypedIOPort(this, "returncode", false, true);
		errors = new TypedIOPort(this, "errors", false, true);

		// Set the type constraints.
		user.setTypeEquals(BaseType.STRING);
		host.setTypeEquals(BaseType.STRING);
		identity.setTypeEquals(BaseType.STRING);
		stdout.setTypeEquals(BaseType.STRING);
		stderr.setTypeEquals(BaseType.STRING);
		returncode.setTypeEquals(BaseType.INT);
		errors.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"65\" height=\"50\" style=\"fill:gray\"/>\n"
				+ "<text x=\"5\" y=\"30\""
				+ "style=\"font-size:25; fill:yellow; font-family:SansSerif\">"
				+ "SCP</text>\n" + "</svg>\n");
	}

	// //////////////// Public ports and parameters ///////////////////////

	/**
	 * Username on the SSH host to be connected to.
	 */
	public PortParameter user;
	/**
	 * Host to connect to.
	 */
	public PortParameter host;
	/**
	 * scp direction, from/to.
	 */
	public StringParameter direction;
	/**
	 * Local file path.
	 */
	public FilePortParameter localFilePath;
	/**
	 * Remote file path.
	 */
	public FilePortParameter remoteFilePath;
	/**
	 * The file path for <i>userName</i>'s ssh identity file if the user wants
	 * to connect without having to enter the password all the time.
	 * 
	 * <P>
	 * The user can browse this file as it is a parameter.
	 */
	public FilePortParameter identity;
	/**
	 * The string representation of the file path for <i>userName</i>'s ssh
	 * identity file if the user wants to connect without having to enter the
	 * password all the time.
	 * 
	 * <P>
	 * This is the input option for the identity file.
	 */
	public TypedIOPort stdout;
	/**
	 * The error that were reported by the remote execution or while connecting.
	 */
	public TypedIOPort stderr;
	/**
	 * The return code of the execution.
	 * 
	 * <P>
	 * This port will return <i>0 (zero)</i> if the execution is not succesfull,
	 * and a positive integer if it is successful.
	 */
	public TypedIOPort returncode;
	/**
	 * The string representation of all the errors that happened during the
	 * execution of the actor, if there are any.
	 */
	public TypedIOPort errors;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Send the token in the <i>value</i> parameter to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		// by default we will try to connect with private/public keys.
		boolean isPassAuth = false;

		user.update();
		host.update();
		String strUser = ((StringToken) user.getToken()).stringValue();
		String strHost = ((StringToken) host.getToken()).stringValue();

		localFilePath.update();
		remoteFilePath.update();
		String lfile = ((StringToken) localFilePath.getToken()).stringValue();
		String rfile = ((StringToken) remoteFilePath.getToken()).stringValue();

		// Hack the path because we can't deal with "file:" or "file://"
		if (lfile.startsWith("file:")) {
			lfile = lfile.substring(5);

			if (lfile.startsWith("//")) {
				lfile = lfile.substring(2);
			}
		}

		if (rfile.startsWith("file:")) {
			rfile = rfile.substring(5);

			if (rfile.startsWith("//")) {
				rfile = rfile.substring(2);
			}
		}

		identity.update();
		String strIdentity;
		strIdentity = ((StringToken) identity.getToken()).stringValue();

		if (strIdentity != null && strIdentity.length() > 0) {
			// Hack the path because we can't deal with "file:" or "file://"
			if (strIdentity.startsWith("file:")) {
				strIdentity = strIdentity.substring(5);

				if (strIdentity.startsWith("//")) {
					strIdentity = strIdentity.substring(2);
				}
			}
		} else {
			// We are now need to connect with password
			isPassAuth = true;
		}

		try {
			if (isPassAuth) {
				// Use password authentication, where use will be prompted
				// for password. The password should be valid for the session

				_connect(strUser, strHost);
			} else {
				_connect(strUser, strHost, strIdentity);
			}

			String direct = direction.getExpression();
			if (direct.equals("TO")) {
				scpTo(lfile, rfile);
			} else if (direct.equals("FROM")) {
				scpFrom(lfile, rfile);
			} else
				throw new IllegalActionException(this, "invalid command");

		} catch (IllegalActionException e) {
			// caught an exception, so output it to the errors port
			stdout.send(0, new StringToken(""));
			stderr.send(0, new StringToken(""));
			returncode.send(0, new IntToken(0));
			errors.send(0, new StringToken(e.getMessage()));
		}
	}

	/**
	 * Terminate any sessions. This method is invoked exactly once per execution
	 * of an application. None of the other action methods should be be invoked
	 * after it.
	 * 
	 * @exception IllegalActionException
	 *                Not thrown in this base class.
	 */
	public void wrapup() throws IllegalActionException {
		_disconnect();
		_ackError = "";
	}

	// //////////////// Private Methods ///////////////////////

	/**
	 * @throws IllegalActionException
	 *             If the connection fails.
	 *             
	 * FIXME See FIXME at top of file            
	 * 
	 */
	private void _connect(String strUser, String strHost, String strIdentity)
			throws IllegalActionException {

		_debug("Connecting with " + strUser + "@" + strHost
				+ " with identity: " + strIdentity);

		try {

			strIdentity = strIdentity.trim();
			if (!strIdentity.equals("")) {
				if (_setIdentities.add(strIdentity)) {
					// we haven't seen this identity before
					_jsch.addIdentity(strIdentity);
				}
			}

			if (!strUser.equals(_strOldUser) || !strHost.equals(_strOldHost)
					|| !_session.isConnected()) {

				if (null != _session && _session.isConnected()) {
					_disconnect();
				}

				_session = _jsch.getSession(strUser, strHost, 22);
				_strOldUser = strUser;
				_strOldHost = strHost;

				// username and passphrase will be given via UserInfo interface.
				UserInfo ui = new MyUserInfo();
				_session.setUserInfo(ui);
				_session.connect(30000);
			}

		} catch (Exception e) {
			// a couple of possible exception messages that could happen here:
			// 1. java.io.FileNotFoundException
			// 2. session is down
			System.err.println("Exception caught in " + this.getFullName());
			System.err.println("I was trying to connect with " + strUser + "@"
					+ strHost + " with identity: " + strIdentity);
			e.printStackTrace();
			throw new IllegalActionException("Exception caught in "
					+ this.getFullName() + "\n(" + e.getClass().getName()
					+ ")\n" + e.getMessage());
		}
	}

	/**
	 * Connect with password 1. When connected for the first time, it will
	 * prompt for password. 2. When execute for the same user@host, it should
	 * use the stored password. 3. When connect to a different user@host, it can
	 * prompt password again.
	 * 
	 * @throws IllegalActionException
	 *             If the connection fails.
	 *             
	 * FIXME See FIXME at top of file
	 *             
	 */
	private void _connect(String strUser, String strHost)
			throws IllegalActionException {

		_debug("Connecting with " + strUser + "@" + strHost + " with password.");

		try {

			if (!strUser.equals(_strOldUser) || !strHost.equals(_strOldHost)
					|| !_session.isConnected()) {

				if (null != _session && _session.isConnected()) {
					_disconnect();
				}

				_session = _jsch.getSession(strUser, strHost, 22);
				_strOldUser = strUser;
				_strOldHost = strHost;

				// username and passphrase will be given via UserInfo interface.

				// check whether ui is already set
				UserInfo ui;
				ui = (UserInfo) hash.get(strUser + "@" + strHost);
				if (ui == null) {
					ui = new MyUserInfo();
				}

				// If it is already there. We will use that.
				// Hopefully we can use the info for connect to the
				// same user@host

				_session.setUserInfo(ui);
				_session.connect();
				// add to the hashtable
				hash.put(strUser + "@" + strHost, ui);

			}

		} catch (Exception e) {
			// a couple of possible exception messages that could happen here:
			// 1. java.io.FileNotFoundException
			// 2. session is down
			System.err.println("Exception caught in " + this.getFullName());
			System.err.println("I was trying to connect with " + strUser + "@"
					+ strHost + " with password.");
			e.printStackTrace();
			throw new IllegalActionException("Exception caught in "
					+ this.getFullName() + "\n(" + e.getClass().getName()
					+ ")\n" + e.getMessage());
		}
	}

	/**
	 * 
	 * @throws IllegalActionException
	 *             if disconnect fails.
	 * FIXME See FIXME at top of file
	 */
	private void _disconnect() throws IllegalActionException {
		if (null == _session) {
			// no session, so nothing to disconnect
			return;
		}

		try {
			_session.disconnect();
		} catch (Exception e) {
			System.err.println("Exception caught in " + this.getFullName());
			e.printStackTrace();
			throw new IllegalActionException("Exception caught in "
					+ this.getFullName() + "\n(" + e.getClass().getName()
					+ ")\n" + e.getMessage());
		}
	}

	/**
	 * 
	 * @throws IllegalActionException
	 */
	private void scpTo(String lfile, String rfile)
			throws IllegalActionException {
		if (null == _session) {
			// no session, so way to execute
			return;
		}

		try {

			// exec 'scp -t rfile' remotely
			String command = "scp -p -t " + rfile;
			ChannelExec channel = (ChannelExec) _session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();
			InputStream err = channel.getErrStream();

			channel.connect();

			byte[] tmp = new byte[1];

			if (checkAck(in) != 0) {
				throw new IllegalActionException(this, "Acknowledgment error "
						+ _ackError);
			}

			// send "C0644 filesize filename", where filename should not include
			// '/'
			int filesize = (int) (new File(lfile)).length();
			command = "C0644 " + filesize + " ";
			if (lfile.lastIndexOf('/') > 0) {
				command += lfile.substring(lfile.lastIndexOf('/') + 1);
			} else {
				command += lfile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();

			if (checkAck(in) != 0) {
				throw new IllegalActionException(this, "Acknowledgment error "
						+ _ackError);
			}

			// send a content of lfile
			FileInputStream fis = new FileInputStream(lfile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len);
				out.flush();
			}

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			if (checkAck(in) != 0) {
				throw new IllegalActionException(this, "Acknowledgment error "
						+ _ackError);
			}

			int ec = channel.getExitStatus();
			channel.disconnect();

			stdout.send(0, new StringToken(rfile));
			stderr.send(0, new StringToken(err.toString()));
			returncode.send(0, new IntToken(ec));
			errors.send(0, new StringToken(""));

		} catch (Exception e) {
			System.err.println("Exception caught in " + this.getFullName());
			e.printStackTrace();
			throw new IllegalActionException("Exception caught in "
					+ this.getFullName() + "\n(" + e.getClass().getName()
					+ ")\n" + e.getMessage());
		}
	}

	private void scpFrom(String lfile, String rfile)
			throws IllegalActionException {
		if (null == _session) {
			// no session, so way to execute
			return;
		}

		try {

			String prefix = null;
			File localFile = new File(lfile);
			if (localFile.isDirectory()) {
				prefix = lfile + File.separator;
			}

			// exec 'scp -f rfile' remotely
			String command = "scp -f " + rfile;
			ChannelExec channel = (ChannelExec) _session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();
			InputStream err = channel.getErrStream();

			channel.connect();

			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				int filesize = 0;
				while (true) {
					in.read(buf, 0, 1);
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10 + (buf[0] - '0');
				}

				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of lfile
				FileOutputStream fos = new FileOutputStream(
						prefix == null ? lfile : prefix + file);
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = filesize;
					in.read(buf, 0, foo);
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0)
						break;
				}
				fos.close();

				byte[] tmp = new byte[1];

				if (checkAck(in) != 0) {
					throw new IllegalActionException(this,
							"Acknowledgment error " + _ackError);
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}

			int ec = channel.getExitStatus();
			channel.disconnect();

			stdout.send(0, new StringToken(localFile.getAbsolutePath()));
			stderr.send(0, new StringToken(err.toString()));
			returncode.send(0, new IntToken(ec));
			errors.send(0, new StringToken(""));

		} catch (Exception e) {
			System.err.println("Exception caught in " + this.getFullName());
			e.printStackTrace();
			throw new IllegalActionException("Exception caught in "
					+ this.getFullName() + "\n(" + e.getClass().getName()
					+ ")\n" + e.getMessage());
		}
	}

	private int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				_ackError = sb.toString();
			}
			if (b == 2) { // fatal error
				_ackError = sb.toString();
			}
		}
		return b;
	}

	// //////////////// Private variables ///////////////////////

	private JSch _jsch = null;
	private Session _session = null;
	private HashSet _setIdentities = null;
	private String _strOldUser = null;
	private String _strOldHost = null;
	private String _ackError = "";
	// ////////////////Public Static ///////////////////////
	// Used to store connection info
	public static Hashtable hash = new Hashtable();

	// //////////////// Inner classes ///////////////////////

	public static class MyUserInfo implements UserInfo {

		public String getPassword() {
			return passwd;
		}

		String passwd = null;
		JTextField passwordField = (JTextField) new JPasswordField(20);

		public boolean promptYesNo(String str) {
			// This method gets called to answer the question similar to
			// "are you sure you want to connect to host whose key
			// is not in database ..."
			return true;
		}

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return false;
		}

		public boolean promptPassword(String message) {
			if (passwd != null) {
				return true;
			}

			Object[] ob = { passwordField };
			int result = JOptionPane.showConfirmDialog(null, ob, message,
					JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				passwd = passwordField.getText();
				return true;
			} else {
				return false;
			}
		}

		public void showMessage(String message) {
			// This method gets called when the server sends over a MOTD.
			// MessageHandler(message);
		}
	}

}