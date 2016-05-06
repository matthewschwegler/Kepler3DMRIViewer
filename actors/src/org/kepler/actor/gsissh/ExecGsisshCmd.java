/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:19:36 -0800 (Mon, 26 Nov 2012) $'
 * '$Revision: 31113 $'
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
package org.kepler.actor.gsissh;

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
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
* Connects to a remote host using gsissh and execute commands.
* It also provides functionalities to copy files between two
* machines using sftp. Currently doesn't support recursive copy.
* <p>
* If the <i>target</i> is empty string or equals <b>local</b>, the Java Runtime
* will be used for local execution instead of ssh. It behaves similarly to
* other local command-line exec actors of Kepler but you do not need to change
* your workflow for remote/local executions by using this actor.
* <p>
* This actor works similar to ExecuteCmd( org.kepler.actor.ssh.ExecuteCmd )
* but additionally supports connecting to grid server using grid certificates
*
* @author Chandrika Sivaramakrishnan
*
*/
public class ExecGsisshCmd extends TypedAtomicActor {
	private static Log log = LogFactory.getLog(ExecGsisshCmd.class);
	boolean isDebugging = log.isDebugEnabled();

	public ExecGsisshCmd(CompositeEntity container, String name)
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

		thirdParty = new PortParameter(this, "thirdParty", new StringToken(""));
		new Parameter(thirdParty.getPort(), "_showName", BooleanToken.TRUE);

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


	///////////////////////////////////////////////////////////////////
	//// public methods ////

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

		// third party target
		thirdParty.update();
		String strThirdParty = ((StringToken) thirdParty.getToken())
				.stringValue();

		// get the execution object
		//ExecInterface execObj = ExecFactory.getExecObject(strTarget);
		//ExecInterface execObj = new GsiSshExec(strTarget);
		ExecInterface execObj;
		try {
			execObj = ExecFactory.getExecObject(strTarget);
		} catch (ExecException e1) {
			String errText = new String("ExecuteCmd error:\n" + e1.getMessage());
			log.error(errText);
			stdout.send(0, new StringToken(""));
			stderr.send(0, new StringToken(""));
			exitcode.send(0, new IntToken(-32767));
			errors.send(0, new StringToken(errText));
			return;
		}
		execObj.setTimeout(timeout, false, false);
		execObj.setForcedCleanUp(cleanup);

		int exitCode = 0;
		ByteArrayOutputStream cmdStdout = new ByteArrayOutputStream();
		ByteArrayOutputStream cmdStderr = new ByteArrayOutputStream();

		// execute command
		try {
			log.info("Exec cmd: " + strCommand);
			exitCode = execObj.executeCmd(strCommand, cmdStdout, cmdStderr,
					strThirdParty);

		} catch (ExecException e) {
			String errText = new String("ExecuteCmd error:\n" + e.getMessage());
			log.error(errText);
			stdout.send(0, new StringToken(""));
			stderr.send(0, new StringToken(""));
			exitcode.send(0, new IntToken(-32767));
			errors.send(0, new StringToken(errText));
			return;
		}

		if (isDebugging)
			log.debug("exit code = " + exitCode);

		System.out.println("From cmdStderr="+cmdStderr.toString());
		System.out.println("From cmdStdout="+cmdStdout.toString());
		// send stdout, stderr and empty string as internal errors
		exitcode.send(0, new IntToken(exitCode));
		stdout.send(0, new StringToken(cmdStdout.toString()));
		stderr.send(0, new StringToken(cmdStderr.toString()));
		errors.send(0, new StringToken(""));

	} // end-method fire()

}
