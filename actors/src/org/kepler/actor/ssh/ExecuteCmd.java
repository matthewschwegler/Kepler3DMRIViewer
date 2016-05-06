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

import java.io.ByteArrayOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.ExecFactory;
import org.kepler.ssh.ExecInterface;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// ExecuteCmd 
/**
 * <p>
 * Connects to a remote host using Ssh protocol (or does nothing for the local
 * host) and executes a command. It returns the stdout, stderr and exit code
 * after the command terminates.
 * 
 * </p>
 * <p>
 * This actor uses the org.kepler.ssh package to have longlasting connections.
 * 
 * </p>
 * <p>
 * If the <i>target</i> is empty string or equals <b>local</b>, the Java Runtime
 * will be used for local execution instead of ssh. It behaves similarly to
 * other local command-line exec actors of Kepler but you do not need to change
 * your workflow for remote/local executions by using this actor.
 * 
 * </p>
 * <p>
 * If the <i>timeoutSeconds</i> is set greater than zero, the command will be
 * timeouted after the specified amount of time (in seconds).
 * 
 * </p>
 * <p>
 * In case there is an ssh connection related error (or timeout) the
 * <i>exitcode</i> will be -32767, <i>errors</i> will contain the error message,
 * <i>stdout</i> and <i>stderr</i> will be empty string.
 * 
 * </p>
 * <p>
 * To ensure fixed rate of token production for SDF, the actor emits an empty
 * string on <i>errors</i> if the command is executed without ssh related
 * errors.
 * 
 * </p>
 * <p>
 * If <i>cleanupAfterError</i> is set, the remote process and its children will
 * be killed (provided, we have the connection still alive). Very useful in case
 * of timeout because that leaves remote processes running. Use only when
 * connecting to a unix machine. In case of <i>local</i>, this flag is not used.
 * 
 * </p>
 * <p>
 * Streaming of output during the command execution is not supported by this
 * actor.
 * 
 * </p>
 * <p>
 * <b>Third party operation</b><br>
 * If the remote command is expected to ask for a password (or passphrase when
 * connecting to a remote host with public-key authentication) set the expert
 * parameter <i>thirdParty</i> for the user@host:port of that third party (it
 * can be the same as <i>target</i> if a sudo command is executed).
 * 
 * </p>
 * <p>
 * The authentication to the third party should be the same from the target host
 * and from Kepler's local host. Kepler authenticates (by opening a channel) to
 * the third party and then it provides the password/passphrase used for the
 * authentication to the command on the target host. Therefore, this actor
 * cannot be used to reach a remote host through a proxy machine and execute a
 * command there.
 * 
 * </p>
 * <p>
 * The third party execution can be used e.g. to execute and ssh/scp command
 * that connects to another host, also reachable from Kepler's host, to execute
 * external data transfer commands (bbcp, GridFTP, SRM-Lite etc) or sudo
 * commands.
 * 
 * </p>
 * <p>
 * The actor will first authenticate Kepler to the third party host (if not yet
 * done by other actors, e.g. SshSession). During the execution of the command,
 * it looks for the appearance of the string 'password' or 'passphrase' in the
 * stdout/stderr streams (case-insensitively). If such string is found, it
 * writes the authentication code stored within Kepler used for the
 * authentication. Therefore, the command must read the password on the standard
 * input, not directly from the terminal device. This process is performed only
 * once!
 * 
 * </p>
 * <p>
 * The underlying java code does not have pseudo-terminal emulation, so if you
 * cannot force the command to read passwords from the stdin (e.g. scp command),
 * you have to use an external tool to execute the command through a
 * pseudo-terminal. <b>ptyexec</b> is provided in the org.kepler.ssh package, a
 * C program, that should be compiled and put into the path on the target
 * machine. Then you can execute <i>"ptyexec scp ..."</i>.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Revision: 24234 $
 * @category.name remote
 * @category.name connection
 * @category.name external execution
 */

public class ExecuteCmd extends TypedAtomicActor {

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
	public ExecuteCmd(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// target selects the machine where to connect to
		target = new PortParameter(this, "target", new StringToken(
				"[user@]host[:port]"));
		new Parameter(target.getPort(), "_showName", BooleanToken.TRUE);

		command = new TypedIOPort(this, "command", true, false);
		command.setTypeEquals(BaseType.STRING);
		new Parameter(command, "_showName", BooleanToken.TRUE);

		stdout = new TypedIOPort(this, "stdout", false, true);
		stdout.setTypeEquals(BaseType.STRING);
		new Parameter(stdout, "_showName", BooleanToken.TRUE);

		stderr = new TypedIOPort(this, "stderr", false, true);
		stderr.setTypeEquals(BaseType.STRING);
		new Parameter(stderr, "_showName", BooleanToken.TRUE);

		exitcode = new TypedIOPort(this, "exitcode", false, true);
		exitcode.setTypeEquals(BaseType.INT);
		new Parameter(exitcode, "_showName", BooleanToken.TRUE);

		errors = new TypedIOPort(this, "errors", false, true);
		errors.setTypeEquals(BaseType.STRING);
		new Parameter(errors, "_showName", BooleanToken.TRUE);

		timeoutSeconds = new Parameter(this, "timeoutSeconds", new IntToken(0));
		timeoutSeconds.setTypeEquals(BaseType.INT);

		cleanupAfterError = new Parameter(this, "cleanupAfterError",
				new BooleanToken(false));
		cleanupAfterError.setTypeEquals(BaseType.BOOLEAN);

		/*
		 * isThirdPartyOperation = new Parameter(this, "isThirdPartyOperation",
		 * new BooleanToken(false));
		 * isThirdPartyOperation.setTypeEquals(BaseType.BOOLEAN);
		 */

		/*
		 * Hidden, expert PortParameter, directed to SOUTH by default to not to
		 * disturb the port layout of the actor
		 */
		thirdParty = new PortParameter(this, "thirdParty", new StringToken(""));
		/*
		 * shownameTP = new SingletonParameter(thirdParty.getPort(),
		 * "_showname"); shownameTP.setToken(BooleanToken.FALSE); hideTP = new
		 * SingletonParameter(thirdParty.getPort(), "_hide");
		 * hideTP.setToken(BooleanToken.TRUE);
		 */
		new Parameter(thirdParty.getPort(), "_showName", BooleanToken.FALSE);
		new Parameter(thirdParty.getPort(), "_hide", BooleanToken.TRUE);
		// DOES NOT WORK: new Parameter(thirdParty.getPort(), "_cardinal", new
		// StringToken("SOUTH"));

		/*
		 * streamingMode = new Parameter(this, "streaming mode", new
		 * BooleanToken(false)); streamingMode.setTypeEquals(BaseType.BOOLEAN);
		 */

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"75\" height=\"50\" style=\"fill:blue\"/>\n"
				+ "<text x=\"5\" y=\"30\""
				+ "style=\"font-size:14; fill:yellow; font-family:SansSerif\">"
				+ "ExecCmd</text>\n" + "</svg>\n");
	}

	// //////////////// Public ports and parameters ///////////////////////

	/**
	 * Target in user@host:port format. If user is not provided, the local
	 * username will be used. If port is not provided, the default port 22 will
	 * be applied. If target is "local" or empty string, the command will be
	 * executed locally, using Java Runtime.
	 */
	public PortParameter target;

	/**
	 * The command to be executed on the remote host. It needs to be provided as
	 * a string.
	 */
	public TypedIOPort command;

	/**
	 * Third party target in user@host:port format. If user is not provided, the
	 * local username will be used. If port is not provided, the default port 22
	 * will be applied.
	 */
	public PortParameter thirdParty;

	/** _hide parameter of thirdParty. */
	public SingletonParameter hideTP;

	/** _showname parameter of thirdParty. */
	public SingletonParameter shownameTP;

	/**
	 * Output of the command as it would output to the standard shell output.
	 */
	public TypedIOPort stdout;

	/**
	 * The error that were reported by the remote execution or while connecting.
	 */
	public TypedIOPort stderr;

	/**
	 * The exit code of the command.
	 */
	public TypedIOPort exitcode;

	/**
	 * The string representation of all the errors that happened during the
	 * execution of the actor, if there are any.
	 */
	public TypedIOPort errors;

	/**
	 * Timeout in seconds for the command to be executed. 0 means waiting
	 * indefinitely for command termination.
	 */
	public Parameter timeoutSeconds;

	/**
	 * Enforce killing remote process(es) after an error or timeout. Unix
	 * specific solution is used, therefore you should not set this flag if
	 * connecting to other servers. But it is very useful for unix as timeout
	 * leaves processes living there, and sometimes errors too. All processes
	 * belonging to the same group as the remote command (i.e. its children)
	 * will be killed.
	 */
	public Parameter cleanupAfterError;

	/**
	 * Specifying whether the output should be sent in a streaming mode.
	 * Streaming is not implemented yet.
	 */
	public Parameter streamingMode;

	/**
	 * Specifying whether third party is to be defined. If false, the
	 * portparameter thirdParty is hidden, otherwise it is shown.
	 */
	public Parameter isThirdPartyOperation;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * If the specified attribute is <i>showTriggerPort</i>, then get the value
	 * of it and re-render the trigger port. If it is true, show the trigger
	 * port; if it is false, hide the trigger port.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == isThirdPartyOperation) {
			BooleanToken useTP = (BooleanToken) isThirdPartyOperation
					.getToken();
			log.debug("flag isThirdPartyOperation has changed to "
					+ useTP.booleanValue());
			try {
				if (useTP.booleanValue()) {
					thirdParty.setContainer(this);
					thirdParty.getPort().setContainer(this);
				} else {
					thirdParty.getPort().setContainer(null);
					thirdParty.setContainer(null);
				}
				// hideTP.setToken(useTP.not());
				// shownameTP.setToken(useTP);
			} catch (NameDuplicationException ndex) {
				log.error("Trouble with thirdParty portparameter: "
						+ ndex.getMessage());
			}
		}
	}

	/**
	 * Send the token in the <i>value</i> parameter to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		// process inputs
		target.update();
		StringToken tg = (StringToken) target.getToken();
		String strTarget = tg.stringValue();
		String strCommand = ((StringToken) command.get(0)).stringValue();
		int timeout = ((IntToken) timeoutSeconds.getToken()).intValue();
		boolean cleanup = ((BooleanToken) cleanupAfterError.getToken())
				.booleanValue();
		/*
		 * boolean streaming = ((BooleanToken)
		 * streamingMode.getToken()).booleanValue();
		 */

		// third party target
		thirdParty.update();
		String strThirdParty = ((StringToken) thirdParty.getToken())
				.stringValue();

		int exitCode = 0;
		ByteArrayOutputStream cmdStdout = new ByteArrayOutputStream();
		ByteArrayOutputStream cmdStderr = new ByteArrayOutputStream();

		// execute command
		try {
			// get the execution object
            log.info("Get exec object for " + strTarget);
			ExecInterface execObj = ExecFactory.getExecObject(strTarget);
			
			execObj.setTimeout(timeout, false, false);
			execObj.setForcedCleanUp(cleanup);
			
			log.info("Exec cmd: " + strCommand);
			exitCode = execObj.executeCmd(strCommand, cmdStdout, cmdStderr,
					strThirdParty);

		} catch (ExecException e) {
			String errText = new String("ExecuteCmd error:\n" + e.getMessage());

			if (isDebugging)
				log.debug(errText);

			stdout.send(0, new StringToken(""));
			stderr.send(0, new StringToken(""));
			exitcode.send(0, new IntToken(-32767));
			errors.send(0, new StringToken(errText));
			return;
		}

		if (isDebugging)
			log.debug("exit code = " + exitCode);

		// send stdout, stderr and empty string as internal errors
		exitcode.send(0, new IntToken(exitCode));
		stdout.send(0, new StringToken(cmdStdout.toString()));
		stderr.send(0, new StringToken(cmdStderr.toString()));
		errors.send(0, new StringToken(""));

	} // end-method fire()

	private static final Log log = LogFactory
			.getLog(ExecuteCmd.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
}

// vim: sw=4 ts=4 et
