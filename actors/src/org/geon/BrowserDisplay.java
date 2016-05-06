/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.gui.MessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.attributes.URIAttribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// BrowserDisplay
/**
 * This actor displays a file or a URL using the BrowserLauncher class. The URL
 * to display is specified through the inputURL port.
 * 
 * @UserLevelDocumentation This actor displays a file or a URL specified by
 *                         inputURL using the the appropriate application.
 * @author Efrat Jaeger
 * @version $Id: BrowserDisplay.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */

public class BrowserDisplay extends TypedAtomicActor {

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public BrowserDisplay(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		// fileOrURL = new FileParameter(this, "fileOrURL");
		inputURL = new TypedIOPort(this, "inputURL", true, false);
		inputURL.setTypeEquals(BaseType.STRING);

		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setTypeEquals(BaseType.BOOLEAN);

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	// public FileParameter fileOrURL;

	/**
	 * The file name or URL to be displayed.
	 * 
	 * @UserLevelDocumentation The input file or URL to be displayed.
	 */
	public TypedIOPort inputURL;

	/**
	 * A trigger to invoke the actor.
	 * 
	 * @UserLevelDocumentation This port is used to trigger the actor.
	 */
	public TypedIOPort trigger;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Display the input file or URL if it is other than null.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */

	public void fire() throws IllegalActionException {
		boolean val = false;
		for (int i = 0; i < trigger.getWidth(); i++) {
			if (trigger.hasToken(i)) {
				val = ((BooleanToken) trigger.get(i)).booleanValue();
				if (val == false) {
					System.out.println("value is false!!!");
					inputURL.get(0);
					return;
				} else
					System.out.println("TRUE!!!");
			}
		}
		try {
			System.out.println("Reading from the browser - val = " + val);
			StringToken fileToken = null;
			try {
				// Check whether a token has been consumed.
				fileToken = (StringToken) inputURL.get(0);
			} catch (Exception ex) {
			}
			if (fileToken != null) {
				strFileOrURL = fileToken.stringValue();
				// the following line replaces all '\\' (double backslashes)
				// with single forward slashes
				// this is sometimes needed on Windows platforms
				// Dan Higgins April 2006
				strFileOrURL = strFileOrURL.replaceAll("\\\\\\\\", "/");
				int lineEndInd = strFileOrURL.indexOf("\n");
				if (lineEndInd != -1) { // Read until the "\n".
					strFileOrURL = strFileOrURL.substring(0, lineEndInd);
				}
				if (!strFileOrURL.trim().toLowerCase().startsWith("http")) {
					File toDisplay = new File(strFileOrURL);
					if (!toDisplay.isAbsolute()) {
						// Try to resolve the base directory.
						URI modelURI = URIAttribute.getModelURI(this);
						if (modelURI != null) {
							URI newURI = modelURI.resolve(strFileOrURL);
							toDisplay = new File(newURI);
							strFileOrURL = toDisplay.getAbsolutePath();
						}
					}
					String canonicalPath = toDisplay.getCanonicalPath(); // Dan
																			// Higgins

					// strFileOrURL = "file:///" + strFileOrURL;
					strFileOrURL = "file:///" + canonicalPath; // Dan Higgins
				}
				/*
				 * else { // The file to display is a file attribute. URL url =
				 * fileOrURL.asURL(); strFileOrURL = url.toString(); String
				 * decoded = URLDecoder.decode(strFileOrURL); if
				 * (decoded.toLowerCase().startsWith("file")) { url = new
				 * URL(decoded); if (decoded.charAt(6) != '/') { // should be
				 * "file://". strFileOrURL = decoded.substring(0, 6) +
				 * decoded.substring(5); //url = new URL(file); } } }
				 */

				BareBonesBrowserLaunch bl = new BareBonesBrowserLaunch(); // Dan
																			// Higgins
				bl.openURL(strFileOrURL); // Dan Higgins
				// LaunchBrowser lb = new LaunchBrowser(); // Dan Higgins
				// lb.displayFileOrURL(strFileOrURL); // Dan Higgins
			} else {
				// There are no more tokens to consume.
				reFire = false;
			}
		} catch (Exception e) {
			MessageHandler.error("Error opening browser", e);
		}
	}

	/**
	 * If there are no more URLs to display (the input token was null) returns
	 * false.
	 * 
	 * @exception IllegalActionException
	 *                If thrown by the super class.
	 */
	public boolean postfire() throws IllegalActionException {
		return reFire;
	}

	/**
	 * set reFire to true.
	 */
	public void wrapup() {
		reFire = true;
	}

	// /////////////////////////////////////////////////////////////////
	// // inner classes ////

	/**
	 * Launch the default browser from within java.
	 */

	public class LaunchBrowser {
		public void displayFileOrURL(String strFileOrURL) {
			String cmd = null;
			try {
				if (isWindows()) {
					String cmdStr = "C:/Program Files/Internet Explorer/IEXPLORE.exe ";
					cmdStr += strFileOrURL;
					// cmd = _winAct + " " + _winFlag + " " + strFileOrURL;
					Process p = Runtime.getRuntime().exec(cmdStr);
					/*
					 * try { p.waitFor(); } catch (Exception ex) {
					 * MessageHandler.error("Error in waitFor", ex); }
					 */
				} else {
					cmd = _unixAct + " " + _unixFlag + "(" + strFileOrURL + ")";
					Process p = Runtime.getRuntime().exec(cmd);
					try {
						int exitCode = p.waitFor();
						if (exitCode != 0) {
							cmd = _unixAct + " " + strFileOrURL;
							Runtime.getRuntime().exec(cmd);
						}
					} catch (InterruptedException ex) {
						MessageHandler.error("Error opening browser, cmd='"
								+ cmd, ex);
					}
				}
			} catch (IOException ex) {
				MessageHandler.error("Error invoking browser, cmd=" + cmd, ex);
			}
		}

		public boolean isWindows() {
			String osName = System.getProperty("os.name");
			if (osName != null)
				return osName.startsWith(_winOS);
			else
				return false;
		}

		private static final String _winOS = "Windows";
		private static final String _winAct = "rundll32";
		private static final String _winFlag = "url.dll,FileProtocolHandler";

		private static final String _unixAct = "netscape";
		private static final String _unixFlag = "-remote openURL";
	}

	// added by Dan Higgins

	// ///////////////////////////////////////////////////////
	// Bare Bones Browser Launch //
	// Version 1.5 //
	// December 10, 2005 //
	// Supports: Mac OS X, GNU/Linux, Unix, Windows XP //
	// Example Usage: //
	// String url = "http://www.centerkey.com/"; //
	// BareBonesBrowserLaunch.openURL(url); //
	// Public Domain Software -- Free to Use as You Like //
	// ///////////////////////////////////////////////////////

	public class BareBonesBrowserLaunch {

		public void openURL(String url) {
			String osName = System.getProperty("os.name");
			try {
				if (osName.startsWith("Mac OS")) {
					Class fileMgr = Class.forName("com.apple.eio.FileManager");
					Method openURL = fileMgr.getDeclaredMethod("openURL",
							new Class[] { String.class });
					openURL.invoke(null, new Object[] { url });
				} else if (osName.startsWith("Windows"))
					Runtime.getRuntime().exec(
							"rundll32 url.dll,FileProtocolHandler " + url);
				else { // assume Unix or Linux
					String[] browsers = { "firefox", "opera", "konqueror",
							"epiphany", "mozilla", "netscape" };
					String browser = null;
					for (int count = 0; count < browsers.length
							&& browser == null; count++)
						if (Runtime.getRuntime().exec(
								new String[] { "which", browsers[count] })
								.waitFor() == 0)
							browser = browsers[count];
					if (browser == null)
						throw new Exception("Could not find web browser");
					else
						Runtime.getRuntime()
								.exec(new String[] { browser, url });
				}
			} catch (Exception e) {
				System.out.println("error in BrowserLauncher - "
						+ e.getLocalizedMessage());
			}
		}
	}

	// end added by Dan Higgins

	/** Represent the URL to be display. */
	private String strFileOrURL;
	/** Indicator that there are more tokens to consume. */
	private boolean reFire = true;
}