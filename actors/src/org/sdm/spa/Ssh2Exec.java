/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-05-23 11:17:50 -0700 (Thu, 23 May 2013) $' 
 * '$Revision: 32080 $'
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

package org.sdm.spa;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

//////////////////////////////////////////////////////////////////////////
//// Ssh2Exec
/**
 * <p>
 * Connects to a remote host using Ssh2 protocol.
 * 
 * </p>
 * <p>
 * Error conditions this actor must respond robustly to:
 * <ul>
 * <li>Wrong identity file given.</li>
 * <li>Host unreachable.</li>
 * <li>Login unsuccessful.</li>
 * <li>Session dies prematurely.</li>
 * </ul>
 * 
 * </p>
 * <p>
 * This actor will keep the session open until it receives a different username
 * and host combination.
 * 
 * </p>
 * <p>
 * Modifications:
 * <ul>
 * <li>Added support for password authentication</li>
 * <li>When no identity is specified, the connection will revert to password
 * authentication.</li>
 * <li>The actor retains the password information for user@host, so the user
 * will only be prompted for passwd once only for each user@host. It is
 * implemented through a static hashtable.</li>
 * </ul>
 * </p>
 * Reference: Ant version 1.6.2.
 * 
 * @author Ilkay Altintas, Xiaowen Xin
 * @version $Id: Ssh2Exec.java 32080 2013-05-23 18:17:50Z jianwu $
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


public class Ssh2Exec extends TypedAtomicActor {

	/**
	 * Construct an SSH2 actor with the given container and name. Create the
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
	public Ssh2Exec(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// initialize our variables
		_jsch = new JSch();
		_setIdentities = new HashSet();

		// create all the ports
		user = new PortParameter(this, "user");
		host = new PortParameter(this, "host");
		paramIdentity = new FileParameter(this, "identity");
		identity = new TypedIOPort(this, "identity", true, false);
		command = new TypedIOPort(this, "command", true, false);
		stdout = new TypedIOPort(this, "stdout", false, true);
		stderr = new TypedIOPort(this, "stderr", false, true);
		returncode = new TypedIOPort(this, "returncode", false, true);
		errors = new TypedIOPort(this, "errors", false, true);
		streamingMode = new Parameter(this, "streaming mode", new BooleanToken(
				false));
		streamingMode.setTypeEquals(BaseType.BOOLEAN);

		// Set the type constraints.
		user.setTypeEquals(BaseType.STRING);
		host.setTypeEquals(BaseType.STRING);
		identity.setTypeEquals(BaseType.STRING);
		command.setTypeEquals(BaseType.STRING);
		stdout.setTypeEquals(BaseType.STRING);
		stderr.setTypeEquals(BaseType.STRING);
		returncode.setTypeEquals(BaseType.INT);
		errors.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"75\" height=\"50\" style=\"fill:gray\"/>\n"
				+ "<text x=\"5\" y=\"30\""
				+ "style=\"font-size:25; fill:yellow; font-family:SansSerif\">"
				+ "SSH2</text>\n" + "</svg>\n");
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
	 * The file path for <i>userName</i>'s ssh identity file if the user wants
	 * to connect without having to enter the password all the time.
	 * 
	 * <p>
	 * The user can browse this file as it is a parameter.
	 * </p>
	 */
	public FileParameter paramIdentity;
	/**
	 * The string representation of the file path for <i>userName</i>'s ssh
	 * identity file if the user wants to connect without having to enter the
	 * password all the time.
	 * 
	 * <p>
	 * This is the input option for the identity file.
	 * </p>
	 */
	public TypedIOPort identity;
	/**
	 * The command to be executed on the remote host.
	 * 
	 * <p>
	 * It needs to be provided as a string.
	 * </p>
	 */
	public TypedIOPort command;

	/**
	 * Output of the command as it would output to the standard shell output.
	 */
	public TypedIOPort stdout;
	/**
	 * The error that were reported by the remote execution or while connecting.
	 */
	public TypedIOPort stderr;
	/**
	 * The return code of the execution.
	 * 
	 * <p>
	 * This port will return <i>0 (zero)</i> if the execution is not succesfull,
	 * and a positive integer if it is successful.
	 * </p>
	 */
	public TypedIOPort returncode;
	/**
	 * The string representation of all the errors that happened during the
	 * execution of the actor, if there are any.
	 */
	public TypedIOPort errors;

	/**
	 * Specifying whether the output should be sent in a streaming mode.
	 */
	public Parameter streamingMode;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Callback for changes in attribute values Get the WSDL from the given URL.
	 * 
	 * @param at
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute at) throws IllegalActionException {
		/*
		 * if ((at == user) || (at == host)) { if (!
		 * (user.getExpression().equals(""))) { String temp1 =
		 * user.getExpression(); if (! (host.getExpression().equals(""))) {
		 * String temp2 = ( (StringToken) host.getToken()).stringValue();
		 * _attachText( "_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
		 * + "width=\"90\" height=\"60\" style=\"fill:gray\"/>\n" +
		 * "<text x=\"5\" y=\"15\"" +
		 * "style=\"font-size:12; fill:yellow; font-family:SansSerif\">" + temp1
		 * + "\n@\n" + temp2 + "</text>\n" + "</svg>\n"); } else { _attachText(
		 * "_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" " +
		 * "width=\"90\" height=\"60\" style=\"fill:gray\"/>\n" +
		 * "<text x=\"5\" y=\"15\"" +
		 * "style=\"font-size:12; fill:yellow; font-family:SansSerif\">" + temp1
		 * + "\n@\nunknown_host</text>\n" + "</svg>\n"); } } else { if
		 * (host.getExpression().equals("")){ _attachText( "_iconDescription",
		 * "<svg>\n" + "<rect x=\"0\" y=\"0\" " +
		 * "width=\"75\" height=\"50\" style=\"fill:gray\"/>\n" +
		 * "<text x=\"5\" y=\"30\"" +
		 * "style=\"font-size:25; fill:yellow; font-family:SansSerif\">" +
		 * "SSH2</text>\n" + "</svg>\n"); } else { String temp2 = (
		 * (StringToken) host.getToken()).stringValue(); _attachText(
		 * "_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" " +
		 * "width=\"90\" height=\"60\" style=\"fill:gray\"/>\n" +
		 * "<text x=\"5\" y=\"15\"" +
		 * "style=\"font-size:12; fill:yellow; font-family:SansSerif\">" +
		 * "unknown_user\n@\n" + temp2 + "</text>\n" + "</svg>\n"); } }
		 * 
		 * }
		 */
	} // end-of-attributeChanged

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
		
		Token token = user.getToken();
		if(token == null) {
			throw new IllegalActionException(this, "No user specified.");
		}
		String strUser = ((StringToken) token).stringValue();
		
		token = host.getToken();
		if(token == null) {
			throw new IllegalActionException(this, "No host specified.");
		}
		String strHost = ((StringToken) token).stringValue();
		
		token = command.get(0);
		if(token == null) {
			throw new IllegalActionException(this, "No command specified.");
		}
		String strCommand = ((StringToken) token).stringValue();

		token = streamingMode.getToken();
		if(token == null) {
			throw new IllegalActionException(this, "Stream mode not specified.");
		}
		streaming = ((BooleanToken) token).booleanValue();

		String strIdentity;
		if (identity.getWidth() > 0) {
			strIdentity = ((StringToken) identity.get(0)).stringValue();
		} else {
			strIdentity = ((StringToken) paramIdentity.getToken())
					.stringValue();
		}

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

			_exec(strCommand);
		} catch (IllegalActionException e) {
			// caught an exception, so output it to the errors port
			stdout.send(0, new StringToken(""));
			stderr.send(0, new StringToken(""));
			returncode.send(0, new IntToken(0));
			errors.send(0, new StringToken(e.getMessage()));
			throw e;
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
	}

	// //////////////// Private Methods ///////////////////////

	/**
	 * @throws IllegalActionException
	 *             If the connection fails.
	 *             
	 *             
	 * FIXME See FIXME at top of file
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
	 *             
	 * FIXME See FIXME at top of file
	 *             
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
	private void _exec(String execCommand) throws IllegalActionException {
		if (null == _session) {
			// no session, so way to execute
			return;
		}

		try {
			final ChannelExec channel = (ChannelExec) _session
					.openChannel("exec");
			channel.setCommand(execCommand);

			streamOut = new ByteArrayOutputStream();
			ByteArrayOutputStream streamErr = new ByteArrayOutputStream();

			channel.setOutputStream(streamOut);
			channel.setErrStream(streamErr);

			channel.connect();

			// wait for it to finish
			_thread = new Thread() {
				public void run() {
					int offset = 0;
					String current = "";
					while (!channel.isEOF()) {
						if (_thread == null) {
							return;
						}
						try {
							sleep(500);
							if (streaming) {
								// System.out.println(streamOut.size());
								byte[] stream = streamOut.toByteArray();
								int len = stream.length;
								current += new String(stream, offset, len
										- offset);
								// System.out.println(current);
								offset = len;
								current = _sendStreamOutput(current);
								// stdout.send(0,new StringToken(current));
							}

						} catch (Exception e) {
							System.out.println(e.getMessage());
						}
					}
					try {
						if (streaming) {
							byte[] stream = streamOut.toByteArray();
							int len = stream.length;
							current += new String(stream, offset, len - offset);
							// System.out.println(current);
							offset = len;
							current = _sendStreamOutput(current);
							if (!current.equals("")) {
								// there was some output that wasn't sent yet.
								stdout.send(0, new StringToken(current));
							}
						}
					} catch (Exception ex) {
					}
				}
			};

			_thread.start();
			_thread.join(0); // set this to a different value to time out

			if (_thread.isAlive()) {
				// ran out of time
				_thread = null;
				throw new IllegalActionException("In " + this.getFullName()
						+ ": Remote operation timed out!");
			}
			// completed successfully

			// this is the wrong test if the remote OS is OpenVMS,
			// but there doesn't seem to be a way to detect it.
			int ec = channel.getExitStatus();
			channel.disconnect();

			if (!streaming) {
				stdout.send(0, new StringToken(streamOut.toString()));
			}
			stderr.send(0, new StringToken(streamErr.toString()));
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

	/**
	 * This function streams the output line by line. If the current stream
	 * doesn't contain any line breaks and it is longer then a certain
	 * threshold, sends the stream content. Otherwise, if there is no line end,
	 * then it returns the content of the last line.
	 * 
	 * @param currentStream
	 * 	 */
	public String _sendStreamOutput(String currentStream)
			throws IllegalActionException {
		String line;
		int crInd;
		while ((crInd = currentStream.indexOf("\n")) > -1) {
			line = currentStream.substring(0, crInd);
			// System.out.println(line);
			try {
				currentStream = currentStream.substring(crInd + 1);
			} catch (Exception ex) {
				// reached end of string.
				// System.out.println(currentStream);
				currentStream = "";
			}
			stdout.send(0, new StringToken(line));
		}

		// if the whole output is a single line, stream it once the string
		// length reaches a certain threshold.
		if (currentStream.length() > 2048) {
			stdout.send(0, new StringToken(currentStream));
		} else {
			// this line doesn't reach the threshold and contains no line break
			// it will be concatenated to the further output.
			return currentStream;
		}
		return "";
	}

	// //////////////// Private variables ///////////////////////

	private JSch _jsch = null;
	private Session _session = null;
	private Thread _thread = null;
	private HashSet _setIdentities = null;
	private String _strOldUser = null;
	private String _strOldHost = null;

	// ////////////////Public Static ///////////////////////
	// Used to store connection info
	public static Hashtable hash = new Hashtable();

	// //////////////// Inner classes ///////////////////////

	public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {

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
			// MessageHandler.message(message);
		}

		//
		// Extensions for supporting keyboard-interactive logins
		// Norbert Podhorszki pnorbert@cs.ucdavis.edu
		// Taken from example
		// http://www.jcraft.com/jsch/examples/UserAuthKI.java
		//

		final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0);
		private Container panel;

		public String[] promptKeyboardInteractive(String destination,
				String name, String instruction, String[] prompt, boolean[] echo) {

			// System.out.println("SSH: promptKI called\n"+
			// "\tDestination: " + destination +
			// "\n\tName: " + name +
			// "\n\tinstruction: " + instruction +
			// "\n\tpromptlen: " + prompt.length);

			panel = new JPanel();
			panel.setLayout(new GridBagLayout());

			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.gridx = 0;
			panel.add(new JLabel(instruction), gbc);
			gbc.gridy++;

			gbc.gridwidth = GridBagConstraints.RELATIVE;

			JTextField[] texts = new JTextField[prompt.length];
			for (int i = 0; i < prompt.length; i++) {
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridx = 0;
				gbc.weightx = 1;
				panel.add(new JLabel(prompt[i]), gbc);

				gbc.gridx = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weighty = 1;
				if (echo[i]) {
					texts[i] = new JTextField(20);
				} else {
					texts[i] = new JPasswordField(20);
				}
				panel.add(texts[i], gbc);
				gbc.gridy++;
			}

			if (JOptionPane.showConfirmDialog(null, panel, destination + ": "
					+ name, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
				String[] response = new String[prompt.length];
				for (int i = 0; i < prompt.length; i++) {
					response[i] = texts[i].getText();
				}
				return response;
			} else {
				return null; // cancel
			}
		}

	}

	private ByteArrayOutputStream streamOut;
	private boolean streaming = false;
}

// vim: sw=4 ts=4 et