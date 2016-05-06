/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2010-11-18 11:13:42 -0800 (Thu, 18 Nov 2010) $' 
 * '$Revision: 26327 $'
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

package org.kepler.ssh;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * This class provides functionality to open/close an SSH2 connection to a
 * remote machine.
 * 
 * A session can be opened using one the following authentication protocols -
 * public-key - password - keyboard-interactive
 * 
 * This class should be used through SshSessionFactory class, which puts
 * sessions into a hashtable so you can refer to them with user@host later
 * 
 * There are three ways to enter the password. By default, a pop-up dialog is
 * used to enter it, but the stdin can be used for that as well or a socket
 * server. The password input method can be chosen through an environment
 * variable: KEPLER_PWD_INPUT_METHOD=[ POPUP | STDIN | SOCKET] default if not
 * defined: POPUP POPUP: pop-up dialog, good for runs within Vergil STDIN: print
 * on stdout, read pwd on stdin. Good for command-line runs. SOCKET: print to a
 * socket and read (plain) pwd from it. KEPLER_PWD_TERM_HOST=host and
 * KEPLER_PWD_TERM_PORT=port define the socket server which should send back the
 * pwd. Used when kepler is submitted as a job from a script on a cluster.
 * 
 * 
 * <p>
 * 
 * @author Norbert Podhorszki
 * 
 *         Based on Ssh2Exec Kepler actor and JSch examples Author of the Kepler
 *         actor: Ilkay Altintas, Xiaowen Xin
 */

class SshSession {

	private Session session = null;
	private JSch jsch;
	private String user;
	private String host;
	private int port = 22; // ssh port, default is 22
	protected MyUserInfo userInfo = null;

	private final static int timeout = 60000;

	// port forwarding variables
	Vector<String> Rfwds = new Vector<String>();
	Vector<String> Lfwds = new Vector<String>();
	
	Set<Integer> LFwdPorts = new HashSet<Integer>();
	Set<Integer> RFwdPorts = new HashSet<Integer>();


	private static final Log log = LogFactory
			.getLog(SshSession.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	protected SshSession(String user, String host, int port) {
		/* get the single instance of JSch object */
		this.jsch = new JSch();
		this.user = user;
		this.host = host;
		if (port > 0)
			this.port = port;
	}

	/**
	 * Set a local port forwarding before the connection is made. Format of the
	 * specification: lport:rhost:rport (int:string:int). lport on this host
	 * will be forwarded through this session and the remote host to the
	 * specified rhost:rport address.
	 */
	protected void setPortForwardingL(String spec) throws SshException {
		if (spec == null || spec.length() == 0)
			throw new SshException("Port forwarding specification is empty: "
					+ spec);
		Lfwds.add(spec);
	}

	/**
	 * Set a remote port forwarding before the connection is made. Format of the
	 * specification: rport:lhost:lport (int:string:int). rport on remote host
	 * will be forwarded through this session and our local host to the
	 * specified lhost:lport address.
	 */
	protected void setPortForwardingR(String spec) throws SshException {
		if (spec == null || spec.length() == 0)
			throw new SshException("Port forwarding specification is empty: "
					+ spec);
		Rfwds.add(spec);
	}

	/**
	 * Get an existing opened session or open a new session to user@host
	 * 
	 */
	protected synchronized Session open() throws SshException {
		// System.out.println(" ++ SshSession.open() called");
		if (session == null || !session.isConnected()) {
			if (isDebugging)
				log.debug("SSH session " + user + "@" + host + ":" + port
						+ " is not connected; should be (re)connected now.");
			connect();
			SshEventRegistry.instance.notifyListeners(new SshEvent(
					SshEvent.SESSION_OPENED, user + "@" + host + ":" + port));

			// forward ports
			if (isDebugging)
				log.debug("Forward local ports:");
			for (String foo : Lfwds) {
				addPortForwardL(foo);
			}

			if (isDebugging)
				log.debug("Forward remote ports:");
			for (String foo : Rfwds) {
				addPortForwardR(foo);
			}

		}
		return session;
	}
	
	/**
	 * Add a local port forwarding to an open connection. Format of the
	 * specification: lport:rhost:rport (int:string:int). lport on this host
	 * will be forwarded through this session and the remote host to the
	 * specified rhost:rport address.
	 */
	protected synchronized void addPortForwardL(String spec) {
		if(session != null && session.isConnected()) {
			int lport = Integer.parseInt(spec.substring(0, spec.indexOf(':')));
			String foo = spec.substring(spec.indexOf(':') + 1);
			String host = foo.substring(0, foo.indexOf(':'));
			int rport = Integer.parseInt(foo.substring(foo.indexOf(':') + 1));
			if (isDebugging)
				log.debug(" --> local " + lport
						+ " forwarded through remote host to " + host + ":"
						+ rport + ".");
 
			try {
				session.setPortForwardingL(lport, host, rport);
				LFwdPorts.add(lport);
			} catch (JSchException e) {
				log.warn("Port forwarding request failed on session " + user
						+ "@" + host + ": " + e);
			}
		}
	}
	
	/**
	 * Add a remote port forwarding to an open connection. Format of the
	 * specification: rport:lhost:lport (int:string:int). rport on remote host
	 * will be forwarded through this session and our local host to the
	 * specified lhost:lport address.
	 */
	protected synchronized void addPortForwardR(String spec) {
		if(session != null && session.isConnected()) {
			int rport = Integer.parseInt(spec.substring(0, spec.indexOf(':')));
			String foo = spec.substring(spec.indexOf(':') + 1);
			String host = foo.substring(0, foo.indexOf(':'));
			int lport = Integer.parseInt(foo.substring(foo.indexOf(':') + 1));
			if (isDebugging)
				log.debug(" --> remote " + rport
						+ " forwarded through localhost to " + host + ":" + lport
						+ ".");
			try {
				session.setPortForwardingR(rport, host, lport);
				RFwdPorts.add(rport);
			} catch (JSchException e) {
				log.warn("Port forwarding request failed on session " + user + "@"
						+ host + ": " + e);
			}
		}
	}
	
	/**
	 * Remove a local port forwarding.
	 * @param port the local port that was forwarded.
	 * @param closeIfLast If true, and there are no additional local ports
	 * forwarded, close the connection.
	 */
	protected synchronized void removePortForwardL(int port, boolean closeIfLast) throws SshException {
		if(session != null && session.isConnected() && LFwdPorts.contains(port)) {
		   
		    // close the forwarded port
		    try {
                session.delPortForwardingL(port);
            } catch (JSchException e) {
                String msg = "Error stopping local forwarded port " + port +
                    " on session " + user + "@" + host + ": " + e;
                log.error(msg);
                throw new SshException(msg);
            }
            // remove port of set of forwarded ports and 
            // see if we should close connection
			LFwdPorts.remove(port);
			if(closeIfLast && LFwdPorts.size() == 0) {
				close();
			}
		}
	}

	/**
	 * Remove a remote port forwarding.
	 * @param port the remote port that was forwarded.
	 * @param closeIfLast If true, and there are no additional remote ports
	 * forwarded, close the connection.
	 */
	protected synchronized void removePortForwardR(int port, boolean closeIfLast) throws SshException {
		if(session != null && session.isConnected() && RFwdPorts.contains(port)) {
	        // close the forwarded port
            try {
                session.delPortForwardingR(port);
            } catch (JSchException e) {
                String msg = "Error stopping remote forwarded port " + port +
                    " on session " + user + "@" + host + ": " + e;
                log.error(msg);
                throw new SshException(msg);
            }
            // remove port of set of forwarded ports and 
            // see if we should close connection
			RFwdPorts.remove(port);
			if(closeIfLast && RFwdPorts.size() == 0) {
				close();
			}
		}
	}

	protected synchronized void close() {
		if (session != null && session.isConnected()) {
			try {
				session.disconnect();
			} catch (Exception e) {
				System.err
						.println("Exception caught in disconnecting the SSH session to "
								+ user + "@" + host);
				e.printStackTrace();
			}
			SshEventRegistry.instance.notifyListeners(new SshEvent(
					SshEvent.SESSION_CLOSED, user + "@" + host + ":" + port));
		}
		session = null;
		LFwdPorts.clear();
		RFwdPorts.clear();
	}

	protected String getUser() {
		return user;
	}

	protected String getHost() {
		return host;
	}

	/** Add an identity file, that can be used at ssh connections */
	protected void addIdentity(String identity) throws SshException {
		String strIdentity = identity;
		if (strIdentity != null && strIdentity.length() > 0) {
			// Hack the path because we can't deal with "file:" or "file://"
			if (strIdentity.startsWith("file:")) {
				strIdentity = strIdentity.substring(5);
				if (strIdentity.startsWith("//")) {
					strIdentity = strIdentity.substring(2);
				}
			}

			// Add identity file string to the JSch object
			strIdentity = strIdentity.trim();
			if (!strIdentity.equals("")) {
				try {
					jsch.addIdentity(strIdentity);
				} catch (JSchException e) {
					log.error("Exception caught for file " + strIdentity + ": "
							+ e);
					throw new SshException(
							"Exception caught in JschSingleton.addIdentity of file "
									+ strIdentity + "\n("
									+ e.getClass().getName() + ")\n"
									+ e.getMessage());
				}
			}
		}
	}

	/**
	 * Get the password/passphrase/passcode for the specified third party
	 * machine.
	 */
	protected static String getPwdToThirdParty(String target)
			throws SshException {

		if (target == null || target.trim().length() == 0)
			return null;

		// Get a session to the third party host
		String user, host;
		int port = 22;

		int atPos = target.indexOf('@');
		if (atPos >= 0)
			user = target.substring(0, target.indexOf('@'));
		else
			user = System.getProperty("user.name");

		// get the HOST and PORT
		int colonPos = target.indexOf(':');
		if (colonPos >= 0 && colonPos > atPos) {
			host = target.substring(atPos + 1, colonPos);
			String portStr = target.substring(colonPos + 1);
			try {
				port = Integer.parseInt(portStr);
			} catch (java.lang.NumberFormatException ex) {
				log
						.error("The port should be a number or omitted in "
								+ target);
			}
		} else
			host = target.substring(atPos + 1);

		// Open the session to the third party host, so it will be authenticated
		// at last here
		// if not yet authenticated
		SshSession thirdpartysession = SshSessionFactory.getSession(user, host,
				port);
		Session jschSession = thirdpartysession.open();
		try {
			// send alive messages every 30 seconds to avoid
			// drops when running long-lasting commands without much stdout
			jschSession.setServerAliveInterval(30000);
		} catch (JSchException ex) {
			log.warn("ServerAliveInterval could not be set for session "
					+ thirdpartysession.getUser() + "@"
					+ thirdpartysession.getHost() + ": " + ex);
		}

		// TODO: now close it if we do not use this session in the workflow
		// elsewhere

		// Now squeeze the secret out from the session
		String pwd = thirdpartysession.userInfo.getPassword();
		if (pwd == null)
			pwd = thirdpartysession.userInfo.getPassphrase();
		if (pwd == null)
			pwd = thirdpartysession.userInfo.getPassPKI();
		if (pwd == null) {
			log
					.error("Tried to use a third party authentication to "
							+ target
							+ " but there is no password, no private-key passphrase and no passcode known to it");
			return null;
		}

		return pwd;
	}

	// //////////////// Private Methods ///////////////////////

	/**
	 * @throws Exception
	 *             If the connection fails.
	 */
	private void connect() throws SshException {
		log.info("Connecting to " + user + "@" + host + ":" + port);
		try {
			session = jsch.getSession(user, host, port);
			// check whether ui is already set
			if (userInfo == null) {
				userInfo = new MyUserInfo();
			}
			session.setUserInfo(userInfo);
			session.connect(timeout);
		} catch (Exception e) {
			// a couple of possible exception messages that could happen here:
			// 1. java.io.FileNotFoundException
			// 2. session is down
			log.error("Exception caught in SshSession.connect to " + user + "@"
					+ host + ":" + port + ". " + e);
			// e.printStackTrace();
			throw new SshException("Exception caught in SshSession.connect to "
					+ user + "@" + host + ":" + port + "\n("
					+ e.getClass().getName() + ")\n" + e.getMessage());
		}
		// Connection succeeded, password/passhphrase was okay (do not know what
		// was used)
		userInfo.authWasSuccessful();
	}

	// //////////////// Inner classes ///////////////////////

	protected static class MyUserInfo implements UserInfo,
			UIKeyboardInteractive {

		private final int PWD_POPUP = 0; // pop-up a dialog to ask a password
		private final int PWD_STDIN = 1; // ask a password on stdin/stdout
		private final int PWD_SOCKET = 2; // ask a password on a socket
											// connection

		private int pwdInputMethod; // only for tests, textual input instead of
									// dialog boxes
		private String socket_server; // server in case of socket communication
		private int socket_port; // port in case of socket communication
		private boolean authSucc = false; // set to true if
											// pwd/passphrase/passcode was used
											// okay

		MyUserInfo() {
			super();

			// determine which method to ask for password
			String inputMethod = System.getProperty("KEPLER_PWD_INPUT_METHOD");
			if (inputMethod != null)
				inputMethod = inputMethod.toUpperCase();

			if (inputMethod == null || inputMethod.startsWith("POPUP")
					|| inputMethod.startsWith("\"POPUP")) {
				pwdInputMethod = PWD_POPUP;
				log.debug("Password input method to be used: POPUP");
			} else if (inputMethod.startsWith("STD")
					|| inputMethod.startsWith("\"STD")) {
				pwdInputMethod = PWD_STDIN;
				log.debug("Password input method to be used: STDIN");
			} else if (inputMethod.startsWith("SOCKET")
					|| inputMethod.startsWith("\"SOCKET")) {
				pwdInputMethod = PWD_POPUP; // fall-back value in case of
											// problems
				socket_server = System.getProperty("KEPLER_PWD_TERM_HOST");
				String port = System.getProperty("KEPLER_PWD_TERM_PORT");
				if (socket_server != null && port != null) {
					try {
						socket_port = Integer.parseInt(port);
						pwdInputMethod = PWD_SOCKET;
						log
								.debug("Password input method to be used: SOCKET. Server="
										+ socket_server
										+ " port="
										+ socket_port);
					} catch (NumberFormatException ex) {
						log
								.error("Password input method SOCKET is requested but the KEPLER_PWD_TERM_PORT"
										+ " value is not a number: server="
										+ socket_server + " port=" + port);
					}
				} else {
					log
							.error("Password input method SOCKET is requested but the KEPLER_PWD_TERM_HOST or"
									+ " KEPLER_PWD_TERM_PORT is not defined: server="
									+ socket_server + " port=" + port);
				}
			}
		}

		public void authWasSuccessful() {
			authSucc = true;
		}

		public boolean promptYesNo(String str) {
			// This method gets called to answer the question similar to
			// "are you sure you want to connect to host whose key
			// is not in database ..."
			return true;
		}

		/*  Support for Passphrase protected PUBLICKEY Authentication */
		private String passphrase = null;
		JTextField passphraseField = (JTextField) new JPasswordField(20);

		public String getPassphrase() {
			return passphrase;
		}

		public boolean promptPassphrase(String message) {
			if (passphrase != null && authSucc) {
				return true;
			}

			if (pwdInputMethod == PWD_STDIN) {
				char password[] = null;
				try {
					password = MaskedTextPasswordField.getPassword(System.in,
							message + ": ");
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				if (password != null) {
					passphrase = String.valueOf(password);
					return true;
				} else {
					return false;
				}

			} else if (pwdInputMethod == PWD_POPUP) {
				Object[] ob = { passphraseField };
				int result = JOptionPane.showConfirmDialog(null, ob, message,
						JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION) {
					passphrase = passphraseField.getText();
					return true;
				} else {
					return false;
				}

			} else if (pwdInputMethod == PWD_SOCKET) {
				BufferedReader in;
				PrintWriter out;
				Socket socket;
				try {
					socket = new Socket(socket_server, socket_port);
					out = new PrintWriter(socket.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(socket
							.getInputStream()));

					out.println(message + ": ");
					passphrase = in.readLine();

					out.close();
					in.close();
					socket.close();

				} catch (IOException ioe) {
					log
							.error("Error during getting the password from a socket server: "
									+ ioe);
					return false;
				}
				return true;
			}

			log.error("No input method for passphrase asking is given.");
			return false; // we have no method to ask for password ???
		}

		// public String getPassphrase() { return null; }
		// public boolean promptPassphrase(String message) { return false; }

		/*  Support for PASSWORD Authentication */
		private String passwd = null;

		public String getPassword() {
			return passwd;
		}

		public boolean promptPassword(String message) {
			if (passwd != null && authSucc) {
				return true;
			}

			if (pwdInputMethod == PWD_STDIN) {
				char password[] = null;
				try {
					password = MaskedTextPasswordField.getPassword(System.in,
							message + ": ");
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				if (password != null) {
					passwd = String.valueOf(password);
					return true;
				} else {
					return false;
				}

			} else if (pwdInputMethod == PWD_POPUP) {
				JTextField passwordField = (JTextField) new JPasswordField(20);
				Object[] ob = { passwordField };

				int result = JOptionPane.showConfirmDialog(null, ob, message,
						JOptionPane.OK_CANCEL_OPTION);

				if (result == JOptionPane.OK_OPTION) {
					passwd = passwordField.getText();
					return true;
				} else {
					return false;
				}
			} else if (pwdInputMethod == PWD_SOCKET) {
				BufferedReader in;
				PrintWriter out;
				Socket socket;
				try {
					socket = new Socket(socket_server, socket_port);
					out = new PrintWriter(socket.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(socket
							.getInputStream()));

					out.println(message + ": ");
					passwd = in.readLine();

					out.close();
					in.close();
					socket.close();

				} catch (IOException ioe) {
					log
							.error("Error during getting the password from a socket server: "
									+ ioe);
					return false;
				}
				return true;
			}

			log.error("No input method for password asking is given.");
			return false; // we have no method to ask for password ???
		}

		public void showMessage(String message) {
			// This method gets called when the server sends over a MOTD.
			// JOptionPane.showMessageDialog(null, message);
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

		/*  Support for KEYBOARD-INTERACTIVE Authentication */
		private String passpki = null; // LIMITATION: only the very last
										// password typed, not all!

		public String getPassPKI() {
			return passpki;
		}

		public String[] promptKeyboardInteractive(String destination,
				String name, String instruction, String[] prompt, boolean[] echo) {

			// System.out.println("SSH: promptKI called\n"+
			// "\tDestination: " + destination +
			// "\n\tName: " + name +
			// "\n\tinstruction: " + instruction +
			// "\n\tpromptlen: " + prompt.length);

			if (pwdInputMethod == PWD_STDIN) {
				System.out.print("Authentication required to " + destination);
				if (name != null && name.length() > 0)
					System.out.print(": " + name);
				System.out.println();
				if (instruction != null && instruction.length() > 0)
					System.out.println(": " + instruction);
				String response[] = new String[prompt.length];
				int i = 0;
				try {
					for (i = 0; i < prompt.length; i++) {
						char password[] = null;
						password = MaskedTextPasswordField.getPassword(
								System.in, prompt[i] + " ");
						if (password != null) {
							response[i] = String.valueOf(password);
							passpki = response[i];
						} else {
							break;
						}
					}
				} catch (IOException e) {
					System.err.println("Error at reading password: " + e);
				}
				if (i == prompt.length)
					return response;
				else
					return null;

			} else if (pwdInputMethod == PWD_POPUP) {
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

				if (JOptionPane.showConfirmDialog(null, panel, destination
						+ ": " + name, JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
					String[] response = new String[prompt.length];
					for (int i = 0; i < prompt.length; i++) {
						response[i] = texts[i].getText();
						passpki = response[i];
					}
					return response;
				} else {
					return null; // cancel
				}

			} else if (pwdInputMethod == PWD_SOCKET) {
				BufferedReader in;
				PrintWriter out;
				Socket socket;
				String response[] = new String[prompt.length];
				int i = 0;
				try {
					socket = new Socket(socket_server, socket_port);
					out = new PrintWriter(socket.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(socket
							.getInputStream()));

					// do not println here. use flush to send the message
					out.print("Authentication required to " + destination
							+ "\n");
					if (name != null && name.length() > 0)
						out.print(": " + name + "\n");
					if (instruction != null && instruction.length() > 0)
						out.print(": " + instruction + "\n");
					try {
						for (i = 0; i < prompt.length; i++) {
							out.print(prompt[i]);
							out.flush();
							response[i] = in.readLine();
							passpki = response[i];
						}
					} catch (IOException e) {
						log.error("Error at reading passcode: " + e);
					}

					out.close();
					in.close();
					socket.close();

				} catch (IOException ioe) {
					log
							.error("Error during getting the password from a socket server: "
									+ ioe);
					return null;
				}
				if (i == prompt.length)
					return response;
				else
					return null;
			}

			log.error("No input method for passcode asking is given.");
			return null; // we have no method to ask for password ???
		}

	} // end of subclass MyUserInfo

}
