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
//// DirectoryCreator

/**
 * <p>
 * Actor for creating a local/remote directory.
 * </p>
 * 
 * <p>
 * The input should define:
 * <ul>
 * <li>the target machine, either "" or "local" to denote the local machine to
 * be used by Java I/O commands, OR "[user@]host[:port]" to denote a remote
 * machine to be used by an ssh connection.</li>
 * <li>the directory to be created (given as String).</li>
 * </ul>
 * 
 * </p>
 * <p>
 * A relative path is relative to the home directory in case of remote
 * operations and relative to the current directory in case of local operations.
 * 
 * </p>
 * <p>
 * The actor creates the directory and emits 'true' on the 'succ' port if it is
 * successful. On the 'error' port, an empty string is emitted, so that this
 * actor can be used under SDF.
 * 
 * </p>
 * <p>
 * On error, 'false' is emitted on 'succ' and the error output of the commands
 * on the 'error' port.
 * 
 * </p>
 * <p>
 * If the 'parent' flag is set the operation is considered to be successful even
 * if the directory already exists. Intermediate directories in the provided
 * path will be created if necessary (as the 'mkdir -p' unix command and the
 * File.mkdirs() method work).
 * 
 * </p>
 * <p>
 * This actor uses org.kepler.ssh.SshExec class to create a remote directory or
 * org.kepler.ssh.LocalExec class to create a local directory.In the first case,
 * the unix command "mkdir" or "mkdir -p" is used, depending on the parent flag.
 * In the latter case, File.mkdir() or File.mkdirs() method is used to create
 * the directory.
 * </p>
 * 
 * @author Norbert Podhorszki
 * @version $Id: DirectoryCreator.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 5.0.1
 */
public class DirectoryCreator extends TypedAtomicActor {
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
	public DirectoryCreator(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());

		// target selects the machine where the directory is to be accessed
		target = new PortParameter(this, "target", new StringToken(
				"[ local | [user@]host[:port] ]"));
		new Parameter(target.getPort(), "_showName", BooleanToken.TRUE);

		// dir is the path to the directory to be listed on the target machine
		dir = new PortParameter(this, "dir", new StringToken("/path/to/dir"));
		new Parameter(dir.getPort(), "_showName", BooleanToken.TRUE);

		// parent parameter
		parent = new Parameter(this, "parent", new BooleanToken(true));
		parent.setTypeEquals(BaseType.BOOLEAN);

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

	/***********************************************************
	 * ports and parameters
	 */

	/**
	 * The machine to be used at job submission. It should be null, "" or
	 * "local" for the local machine or [user@]host[:port] to denote a remote
	 * machine accessible with ssh.
	 * 
	 * This parameter is read once at initialize.
	 */
	public PortParameter target;

	/**
	 * The path to the directory to be read on the target machines. This
	 * parameter is read once at initialize.
	 */
	public PortParameter dir;

	/**
	 * Specifying whether parent directories should be created recursively if
	 * necessary.
	 */
	public Parameter parent;

	/**
	 * The flag of successful operation. It is a port of type Boolean token.
	 */
	public TypedIOPort succ;

	/**
	 * The string representation of all the errors that happened during the
	 * execution of the actor, if there are any, otherwise an empty string. A
	 * port of type String token.
	 */
	public TypedIOPort error;

	/***********************************************************
	 * public methods
	 */

	/**
	 * initialize() runs once before first exec
	 * 
	 * @exception IllegalActionException
	 *                If the parent class throws it.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
	}

	/**
	 * fire
	 * 
	 * @exception IllegalActionException
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		// update PortParameters
		target.update();
		dir.update();

		_target = ((StringToken) target.getToken()).stringValue().trim();
		_dir = ((StringToken) dir.getToken()).stringValue().trim();
		_parent = ((BooleanToken) parent.getToken()).booleanValue();

		if (isDebugging)
			log.debug("Create Directory: " + "target = " + _target + "; dir = "
					+ _dir + "; parent = " + _parent);


		// execute the directory create command
		boolean _succ;
		String _error;
		try {
			
			// get the actual execution object (local or remote)
			ExecInterface execObj = ExecFactory.getExecObject(_target);
			_succ = execObj.createDir(_dir, _parent);

			if (isDebugging)
				log
						.debug(_succ ? "Directory created: " + _target + ":"
								+ _dir : "Directory operation failed: "
								+ _target + ":" + _dir);
			_error = new String("");

		}catch (ExecException ex) {
			_error = new String(
					"DirectoryCreator error when creating directory " + _target
							+ ":" + _dir + ".\n" + ex);
			_succ = false;
			log.error(_error);
		}

		succ.send(0, new BooleanToken(_succ));
		error.send(0, new StringToken(_error));

	}

	private String _target;
	private String _dir;
	private boolean _parent;

	private static final Log log = LogFactory.getLog(DirectoryCreator.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

}