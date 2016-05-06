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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.ExecFactory;
import org.kepler.ssh.ExecInterface;

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
//// FileRemover
/**
 * <p>
 * Connects to a remote host using Ssh protocol (or does nothing for the local
 * host) and deletes files/directories matching a given mask.
 * 
 * </p>
 * <p>
 * This actor uses the org.kepler.ssh package for longlasting connections. If
 * the host is empty string or equals "local", the Java Runtime will be used for
 * local execution instead of ssh.
 * 
 * </p>
 * <p>
 * The input should define:
 * <ul>
 * <li>the target machine, either "" or "local" to denote the local machine to
 * be used by Java I/O commands, OR "[user@]host[:port]" to denote a remote
 * machine to be used by an ssh connection.</li>
 * <li>the file mask to be deleted (given as String).</li>
 * <li>The flag 'recursive' should be set if you want to delete directories.</li>
 * <li>The flag 'allowMask' should be set if you want to use wildcards.</li>
 * </ul>
 * 
 * </p>
 * <p>
 * A file mask can contain wildcards in the path expressions as well as in the
 * file name part. E.g. /path/d*r/../sub??/./f*.txt is a valid expression. In
 * case of remote operations, the command will be actually the 'rm -rf' command
 * (without -r if 'recursive' is not set).
 * 
 * </p>
 * <p>
 * A relative path in the file mask is relative to the home directory in case of
 * remote operations and relative to the current directory in case of local
 * operations.
 * 
 * </p>
 * <p>
 * Note, that symbolic links will also be deleted, but if they are referring to
 * a directory, they are not followed. This is how 'rm -rf' works and the local
 * version implements the same behaviour.
 * 
 * </p>
 * <p>
 * This actor produces a Boolean token on 'succ' port. TRUE indicates successful
 * operation, while false indicates an error. The actor also produces a String
 * token on the 'error' port; an empty string on success, internal error
 * messages on failure.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Revision: 24234 $
 * @category.name remote
 * @category.name connection
 * @category.name file operation
 */

public class FileRemover extends TypedAtomicActor {

	/**
	 * Construct an FileRemover actor with the given container and name. Create
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
	public FileRemover(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		/*
		 * Input ports
		 */

		// target machine
		target = new PortParameter(this, "target", new StringToken(
				"[user@]host[:port]"));
		new Parameter(target.getPort(), "_showName", BooleanToken.TRUE);

		// file mask
		mask = new PortParameter(this, "mask", new StringToken("none.txt"));
		new Parameter(mask.getPort(), "_showName", BooleanToken.TRUE);

		// recursive parameter
		recursive = new Parameter(this, "recursive", new BooleanToken(false));
		recursive.setTypeEquals(BaseType.BOOLEAN);

		// allowMask parameter
		allowMask = new Parameter(this, "allowMask", new BooleanToken(false));
		allowMask.setTypeEquals(BaseType.BOOLEAN);

		/*
		 * Output ports
		 */

		succ = new TypedIOPort(this, "succ", false, true);
		succ.setTypeEquals(BaseType.BOOLEAN);
		new Parameter(succ, "_showName", BooleanToken.TRUE);

		error = new TypedIOPort(this, "error", false, true);
		error.setTypeEquals(BaseType.STRING);
		new Parameter(error, "_showName", BooleanToken.TRUE);

	}

	// //////////////// Public ports and parameters ///////////////////////

	/**
	 * Target in user@host:port format. If user is not provided, the local
	 * username will be used. If port is not provided, the default port 22 will
	 * be applied. If host is "local" or empty string, the path is handled as
	 * local path.
	 */
	public PortParameter target;

	/**
	 * File mask as String. Path expressions are allowed.
	 */
	public PortParameter mask;

	/**
	 * The flag of successful removal. It will be true if ALL matched files and
	 * directories are deleted. If 'recursive' is not set and directories are
	 * also matched, the value will be false. Note, that files will be still
	 * removed in the latter case. It is a port of type Boolean token.
	 */
	public TypedIOPort succ;

	/**
	 * The string representation of all the errors that happened during the
	 * execution of the actor, if there are any. A port of type String token.
	 */
	public TypedIOPort error;

	/**
	 * Specifying whether directories can be removed recursively.
	 */
	public Parameter recursive;

	/**
	 * Specifying whether wildcards (* and ?) are allowed in the mask.
	 */
	public Parameter allowMask;

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
		target.update();
		String strTarget = ((StringToken) target.getToken()).stringValue()
				.trim();

		mask.update();
		String strMask = ((StringToken) mask.getToken()).stringValue().trim();

		boolean recursiveFlag = ((BooleanToken) recursive.getToken())
				.booleanValue();
		boolean allowFlag = ((BooleanToken) allowMask.getToken())
				.booleanValue();


		// execute the file removal command
		boolean result = false;
		try {
			// select the appropriate execution setting for the current source and
			// target
			ExecInterface execObj = ExecFactory.getExecObject(strTarget);
			result = execObj.deleteFile(strMask, recursiveFlag, allowFlag);
		
		} catch (ExecException e) {
			String errText = new String("Error at execution:\n"
					+ e.getMessage());

			log.error(errText);
			succ.send(0, new BooleanToken(false));
			error.send(0, new StringToken(errText));
			return;
		}

		// report
		if (result) {
			// finally, good news can be reported
			succ.send(0, new BooleanToken(true));
			error.send(0, new StringToken(""));
		} else {
			String errText = new String(
					"Some file(s) were not deleted for unknown reasons on host "
							+ strTarget + " with mask " + strMask
							+ " with flags recursive=" + recursiveFlag
							+ " and allowMask=" + allowFlag);
			log.warn(errText);
			succ.send(0, new BooleanToken(false));
			error.send(0, new StringToken(errText));
		}

	} // end-method fire()

	private static final Log log = LogFactory.getLog(FileRemover.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
}