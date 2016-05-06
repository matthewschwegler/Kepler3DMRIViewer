/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.kepler.actor.ssh;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.ExecFactory;
import org.kepler.ssh.ExecInterface;
import org.kepler.ssh.LocalExec;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// FileCopier 
/**
 * Connects to a remote host using Ssh protocol (or does nothing for the local
 * host) and copies a file to or from there.
 * 
 * <p>
 * This actor uses the org.kepler.ssh package for longlasting connections
 * 
 * <p>
 * The file references should be in the format: [[user@]host:]path. E.g.
 * <ul>
 * <li><it>foo.txt</it> foo.txt in the current dir on the local machine</li>
 * <li><it>playdir/foo.txt</it> relative path to current dir on the local
 * machine</li>
 * <li><it>/home/littleboy/playdir/foo.txt<it> absolute path on the local
 * machine</li>
 * <li><it>local:playdir/foo.txt</it> relative path to $HOME on the local
 * machine</li>
 * <li><it>localhost:playdir/foo.txt</it> relative path to $HOME on the
 * 'localhost' machine (it counts to be a remote file!)</li>
 * <li><it>john@farmachine:playdir/foo.txt</it> relative path to $HOME on the
 * 'farmachine' machine of user 'john'</li>
 * </ul>
 * 
 * <p>
 * The target becomes overwritten if exists, just like with scp and cp, in case
 * of single files. For directories, a subdirectory will be created with the
 * name of the source within the existing directory (again, just like with scp
 * and cp).
 * 
 * <p>
 * If the source refers to a directory, you should set the parameter 'recursive'
 * to true, and then the whole directory will be copied to target.
 * 
 * <p>
 * Either the source or the target file should be local. This actor cannot copy
 * remote files to remote places. For such operations, you need to use ExecCmd
 * actor with executing remote scp commands.
 * 
 * <p>
 * If both source and target refers to local files/directories, the Java File
 * class will be used for local copy instead of ssh. It behaves similarly to
 * other local file copier actors of Kepler but you do not need to change your
 * workflow for remote/local executions by using this actor.
 * 
 * <p>
 * This actor produces a Boolean token on 'succ' port. TRUE indicates successful
 * operation, while false indicates an error. The actor also produces a String
 * token on the 'error' port; an empty string on success, internal error
 * messages on failure.
 * 
 * @author Norbert Podhorszki
 * @version $Revision: 24234 $
 * @category.name remote
 * @category.name connection
 * @category.name file operation
 */

public class FileCopier extends TypedAtomicActor {

	/**
	 * Construct an ExecuteCmd actor with the given container and name. Create
	 * the parameters, initialize their values.
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
	public FileCopier(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		/*
		 * Input ports
		 */

		// source file/dir
		source = new PortParameter(this, "source", new StringToken(
				"[[user]@host:]path"));
		new Parameter(source.getPort(), "_showName", BooleanToken.TRUE);

		// target file/dir
		target = new PortParameter(this, "target", new StringToken(
				"[[user]@host:]path"));
		new Parameter(target.getPort(), "_showName", BooleanToken.TRUE);

		// recursive parameter
		recursive = new Parameter(this, "recursive", new BooleanToken(false));
		recursive.setTypeEquals(BaseType.BOOLEAN);

		/*
		 * Output ports
		 */

		succ = new TypedIOPort(this, "succ", false, true);
		succ.setTypeEquals(BaseType.BOOLEAN);
		new Parameter(succ, "_showName", BooleanToken.TRUE);

		error = new TypedIOPort(this, "error", false, true);
		error.setTypeEquals(BaseType.STRING);
		new Parameter(error, "_showName", BooleanToken.TRUE);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"75\" height=\"50\" style=\"fill:blue\"/>\n"
				+ "<text x=\"5\" y=\"30\""
				+ "style=\"font-size:14; fill:yellow; font-family:SansSerif\">"
				+ "SshExec</text>\n" + "</svg>\n");
	}

	// //////////////// Public ports and parameters ///////////////////////

	/**
	 * Source in user@host:path format. If user is not provided, the local
	 * username will be used. If host is "local" or empty string, the path is
	 * handled as local path.
	 */
	public PortParameter source;

	/**
	 * Target in user@host:path format. If user is not provided, the local
	 * username will be used. If host is "local:" or empty string, the path is
	 * handled as local path.
	 */
	public PortParameter target;

	/**
	 * The flag of successful copy. It is a port of type Boolean token.
	 */
	public TypedIOPort succ;

	/**
	 * The string representation of all the errors that happened during the
	 * execution of the actor, if there are any. A port of type String token.
	 */
	public TypedIOPort error;

	/**
	 * Specifying whether directories can be copied recursively.
	 */
	public Parameter recursive;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Perform copying.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		// get inputs
		source.update();
		String strSource = ((StringToken) source.getToken()).stringValue()
				.trim();

		target.update();
		String strTarget = ((StringToken) target.getToken()).stringValue()
				.trim();

		boolean recursiveFlag = ((BooleanToken) recursive.getToken())
				.booleanValue();

		/*
		 * process source string
		 */
		String userSource;
		String hostSource;
		//int portSource = 22;
		int portSource = -1;
		String pathSource;
		boolean relativeToCurrentSource = false; // true: local file, relative
													// to current dir

		if (strSource.indexOf(":\\") == -1) { // not windows path like D:\Temp

			// get USER
			int atPos = strSource.indexOf('@');
			if (atPos >= 0)
				userSource = strSource.substring(0, atPos);
			else
				userSource = System.getProperty("user.name");

			// get HOST
			int colonPos = strSource.indexOf(':');
			if (colonPos >= 0)
				if (atPos >= 0)
					hostSource = strSource.substring(atPos + 1, colonPos);
				else
					hostSource = strSource.substring(0, colonPos);
			else {
				hostSource = new String("local");
				relativeToCurrentSource = true;
			}

			// get PORT (default 22 is already set)
			String subS = strSource.substring(colonPos + 1);
			if (colonPos >= 0) {
				colonPos = subS.indexOf(':'); // look for second occurence of :
				if (colonPos >= 0) {
					String portStr = subS.substring(0, colonPos);
					if (!portStr.trim().equals("")) {
						try {
							portSource = Integer.parseInt(portStr);
						} catch (java.lang.NumberFormatException ex) {
							throw new IllegalActionException(
									"The port should be a number or omitted in source path "
											+ strSource);
						}
					}
				}
			}
			pathSource = subS.substring(colonPos + 1); // the rest of the string

		} else { // windows path means local path
			userSource = System.getProperty("user.name");
			hostSource = new String("local");
			pathSource = strSource;
			relativeToCurrentSource = true;
		}

		if (isDebugging)
			log
					.debug("Source: user=[" + userSource + "], host=["
							+ hostSource + "], port=[" + portSource
							+ "], path=[" + pathSource + "]");

		/*
		 * process target string
		 */
		String userTarget;
		String hostTarget;
		//int portTarget = 22;
		int portTarget = -1;
		String pathTarget;
		boolean relativeToCurrentTarget = false; // true: local file, relative
													// to current dir

		if (strTarget.indexOf(":\\") == -1) { // not windows path like D:\Temp

			// get USER
			int atPos = strTarget.indexOf('@');
			if (atPos >= 0)
				userTarget = strTarget.substring(0, atPos);
			else
				userTarget = System.getProperty("user.name");

			// get HOST
			int colonPos = strTarget.indexOf(':');
			if (colonPos >= 0)
				if (atPos >= 0)
					hostTarget = strTarget.substring(atPos + 1, colonPos);
				else
					hostTarget = strTarget.substring(0, colonPos);
			else {
				hostTarget = new String("local");
				relativeToCurrentTarget = true;
			}

			// get PORT (default 22 is already set)
			String subS = strTarget.substring(colonPos + 1);
			if (colonPos >= 0) {
				colonPos = subS.indexOf(':'); // look for second occurence of :
				if (colonPos >= 0) {
					String portStr = subS.substring(0, colonPos);
					if (!portStr.trim().equals("")) {
						try {
							portTarget = Integer.parseInt(portStr);
						} catch (java.lang.NumberFormatException ex) {
							throw new IllegalActionException(
									"The port should be a number or omitted in target path "
											+ strTarget);
						}
					}
				}
			}
			pathTarget = subS.substring(colonPos + 1); // the rest of the string

		} else { // windows path means local path
			userTarget = System.getProperty("user.name");
			hostTarget = new String("local");
			pathTarget = strTarget;
			relativeToCurrentTarget = true;
		}

		if (isDebugging)
			log
					.debug("Target: user=[" + userTarget + "], host=["
							+ hostTarget + "], port=[" + portTarget
							+ "], path=[" + pathTarget + "]");

		// error check: not both refers to remote place
		if (!hostSource.equals("local") && !hostTarget.equals("local")) {
			String msg = new String(
					"One of the source and target should be local.");
			log.error(msg);
			succ.send(0, new BooleanToken(false));
			error.send(0, new StringToken(msg));
			return;
		}

		// error check: source reference is empty
		if (pathSource.length() == 0) {
			String msg = new String("Source path is empty in " + strSource);
			log.error(msg);
			succ.send(0, new BooleanToken(false));
			error.send(0, new StringToken(msg));
			return;
		}

		// semantic check: target reference is empty: replace with .
		// SshExec's copyTo does not work with empty path string but it works
		// the same
		// on . (scp works the same on empty string and .)
		if (pathTarget.length() == 0) {
			pathTarget = new String(".");
		}

		// source case of local:relativePath --> relative to $HOME, so
		// absolutize now
		if (hostSource.equals("local") && !relativeToCurrentSource
				&& pathSource.charAt(0) != File.separatorChar) {

			pathSource = System.getProperty("user.home") + File.separator
					+ pathSource;
			if (isDebugging)
				log.debug("Source path absolutized to $HOME: " + pathSource);
		}

		// target case of local:relativePath --> relative to $HOME, so
		// absolutize now
		if (hostTarget.equals("local") && !relativeToCurrentTarget
				&& pathTarget.charAt(0) != File.separatorChar) {

			pathTarget = System.getProperty("user.home") + File.separator
					+ pathTarget;
			if (isDebugging)
				log.debug("Target path absolutized to $HOME: " + pathTarget);
		}

		// select the appropriate execution setting for the current source and
		// target
		ExecInterface execObj; // local or remote copy
		boolean copyTo = true; // copyTo (local->remote) or copyFrom
								// (remote->local)
		try{
			if (hostSource.equals("local") && hostTarget.equals("local")) {
				// local copy
				if (isDebugging)
					log.debug("Execution mode: local using Java File class");
				execObj = new LocalExec();
				copyTo = true;
			} else if (hostSource.equals("local")) {
				// create an SshExec object, and set copyTo to true
				if (isDebugging)
					log.debug("Execution mode: remote copyTo using ssh");
				//execObj = new SshExec(userTarget, hostTarget, portTarget);
				execObj = ExecFactory.getExecObject(userTarget, hostTarget, portTarget);
				copyTo = true;
			} else {
				// create an SshExec object, and set copyTo to false
				if (isDebugging)
					log.debug("Execution mode: remote copyFrom using ssh");
				//execObj = new SshExec(userSource, hostSource, portSource);
				execObj = ExecFactory.getExecObject(userSource, hostSource, portSource);
				copyTo = false;
			}
		}catch(ExecException e){
			String errText = new String("Error connecting to remote host:\n"
					+ e.getMessage());

			log.error(errText);
			succ.send(0, new BooleanToken(false));
			error.send(0, new StringToken(errText));
			return;
		}

		File lfile;
		int numberOfCopiedFiles = 0;

		long startTime = System.currentTimeMillis();
		try {
			if (copyTo) {
				lfile = new File(pathSource);
				numberOfCopiedFiles = execObj.copyTo(lfile, pathTarget,
						recursiveFlag);
			} else {
				lfile = new File(pathTarget);
				numberOfCopiedFiles = execObj.copyFrom(pathSource, lfile,
						recursiveFlag);
			}

		} catch (ExecException e) {
			String errText = new String("Error at copy execution:\n"
					+ e.getMessage());

			if (isDebugging)
				log.debug(errText);
			succ.send(0, new BooleanToken(false));
			error.send(0, new StringToken(errText));
			return;
		}
		long endTime = System.currentTimeMillis();

		if (isDebugging)
			log.debug("Number of copied files = " + numberOfCopiedFiles
					+ ". Time to copy: " + (endTime - startTime) + " msec.");

		if (numberOfCopiedFiles <= 0) {
			String errText = new String(
					"No file(s) were copied for unknown reasons\n");
			if (isDebugging)
				log.debug(errText);
			succ.send(0, new BooleanToken(false));
			error.send(0, new StringToken(errText));
		} else {
			// finally, good news can be reported
			succ.send(0, new BooleanToken(true));
			error.send(0, new StringToken(""));
		}

	} // end-method fire()

	private static final Log log = LogFactory
			.getLog(FileCopier.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
}

// vim: sw=4 ts=4 et
