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

package org.srb;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import ptolemy.actor.IOPort;
import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;

//////////////////////////////////////////////////////////////////////////
//// SProxy
/**
 * <p>
 * Executes a proxy command. Currently supporteds command: 'list directory',
 * 'copy', 'move', 'remove', 'replicate', 'create directory', 'remove
 * directory', 'change mode'.
 * </p>
 * <p>
 * The actor accepts a reference to the SRB files system, and a desired command
 * with its input ports. Outputs the command result along with an exit status.
 * </p>
 * <p>
 * <b> COMMANDS DESCRIPTIONS:</b>
 * </p>
 * <p>
 * <b>List directory:</b> Lists a remote directory content.
 * </p>
 * <p>
 * Inputs: path: [string]. Remote paths to list.
 * </p>
 * <p>
 * Output: listedFiles : [string]. Arrays of the contained files paths.
 * </p>
 * <p>
 * exitCode : string.
 * </p>
 * <p>
 * Option: outputEachFileSeparately. Whether to broadcast each file path
 * sepearately or the whole list at once.
 * </p>
 * <p>
 * <b>Copy/Move:</b> Copys or moves files to a new path. Returns the new file
 * paths. recursively copies/moves directories.
 * </p>
 * <p>
 * Inputs: path: [string]. Original remote file paths.
 * </p>
 * <p>
 * newPath: string. Location to copy/move.
 * </p>
 * <p>
 * Output: copiedFiles/movedFiles : [string]. Arrays of the new file paths.
 * </p>
 * <p>
 * <b>Remove/Remove directory:</b> Removes files/directories. Non-empty
 * directories are recursively removed by remove r.
 * </p>
 * <p>
 * Inputs: path: [string]. Remote file paths.
 * </p>
 * <p>
 * Option: -r ; recursively removes files.
 * </p>
 * <p>
 * forward ; output an array of the removed files parent directory paths.
 * </p>
 * <p>
 * <b>Create directory:</b> Creates new directories. Returns the new directory
 * path.
 * </p>
 * <p>
 * Inputs: path: [string]. New directories paths.
 * </p>
 * <p>
 * Output: dirPath: Created directories paths.
 * </p>
 * <p>
 * <b>Replicate:</b> Replicates a file/directory to a new resource.
 * </p>
 * <p>
 * Inputs: path: [string]. The files to be replicated.
 * </p>
 * <p>
 * newPath: [string]. The resource to replicate to.
 * </p>
 * <p>
 * Output: newResource: string. The files new resource.
 * </p>
 * <p>
 * <b>Change mode:</b> Changes the permissions of a file or a directory.
 * </p>
 * <p>
 * Inputs: path: [string]. Files paths.
 * </p>
 * <p>
 * permission: string
 * </p>
 * <p>
 * userName: string. To grant permission to.
 * </p>
 * <p>
 * mdasDomain: string. The metadata domain.
 * </p>
 * <p>
 * Output: exitPath: [string]. The files paths.
 * 
 * </p>
 * 
 * @author Efrat Jaeger
 * @version $Id: SProxy.java 24234 2010-05-06 05:21:26Z welker $
 * @category.name srb
 */

public class SProxy extends TypedAtomicActor {

	/**
	 * Construct a constant source with the given container and name. Create the
	 * <i>value</i> parameter, initialize its value to the default value of an
	 * IntToken with value 1.
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
	public SProxy(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		SRBFileSystem = new TypedIOPort(this, "SRBFileSystem", true, false);
		SRBFileSystem.setTypeEquals(BaseType.GENERAL);
		new Attribute(SRBFileSystem, "_showName");

		path = new TypedIOPort(this, "path", true, false);
		path.setTypeEquals(new ArrayType(BaseType.STRING));
		Attribute tmp = new Attribute(path, "_showName");

		newPath = new TypedIOPort(this, "newPath", true, false);
		// newPath.setContainer(null);
		newPath.setTypeEquals(BaseType.STRING);
		// new Attribute(newPath, "_showName");
		hideNewPath = new SingletonParameter(newPath, "_hide");
		hideNewPath.setToken(BooleanToken.TRUE);

		permission = new TypedIOPort(this, "permission", true, false);
		// permission.setContainer(null);
		permission.setTypeEquals(BaseType.STRING);
		// new Attribute(permission, "_showName");
		hidePermission = new SingletonParameter(permission, "_hide");
		hidePermission.setToken(BooleanToken.TRUE);

		userName = new TypedIOPort(this, "userName", true, false);
		// userName.setContainer(null);
		userName.setTypeEquals(BaseType.STRING);
		// new Attribute(userName, "_showName");
		hideUserName = new SingletonParameter(userName, "_hide");
		hideUserName.setToken(BooleanToken.TRUE);

		mdasDomain = new TypedIOPort(this, "mdasDomain", true, false);
		// mdasDomain.setContainer(null);
		mdasDomain.setTypeEquals(BaseType.STRING);
		// new Attribute(mdasDomain, "_showName");
		hideMdasDomain = new SingletonParameter(mdasDomain, "_hide");
		hideMdasDomain.setToken(BooleanToken.TRUE);

		// output = new TypedIOPort(this, "output", false, true);
		// output.setTypeEquals(new ArrayType(BaseType.STRING));
		// new Attribute(output, "_showName");
		// TODO: OUTPUT SHOULD BE SET DYNAMICALLY LIKE IN THE WS.

		exitCode = new TypedIOPort(this, "exitCode", false, true);
		exitCode.setTypeEquals(BaseType.STRING);
		new Attribute(exitCode, "_showName");

		command = new StringParameter(this, "command");
		command.setExpression("list directory");
		command.addChoice("list directory");
		command.addChoice("copy");
		command.addChoice("move");
		command.addChoice("remove");
		command.addChoice("replicate");
		command.addChoice("create directory");
		command.addChoice("remove directory");
		command.addChoice("change mode");

		// corrected way to create Parameters
		outputEachFileSeparately = new Parameter(this,
				"outputEachFileSeparately", new BooleanToken(false));
		outputEachFileSeparately
				.setDisplayName("output each path separately (for Sls)");
		outputEachFileSeparately.setTypeEquals(BaseType.BOOLEAN);

		// corrected way to create Parameters
		forwardParentDir = new Parameter(this, "forwardParentDir",
				new BooleanToken(false));
		forwardParentDir
				.setDisplayName("forward parent directory (for Srm/Srmdir)");
		forwardParentDir.setTypeEquals(BaseType.BOOLEAN);

		// corrected way to createParameters
		_r = new Parameter(this, "_r", new BooleanToken(false));
		_r.setDisplayName("-r (for Srm)");
		_r.setTypeEquals(BaseType.BOOLEAN);

		trigger = new TypedIOPort(this, "trigger", true, false);
		// new Attribute(trigger, "_showName");
		hideTrigger = new SingletonParameter(trigger, "_hide");
		hideTrigger.setToken(BooleanToken.TRUE);

		// Set the trigger Flag.
		hasTrigger = new Parameter(this, "hasTrigger", new BooleanToken(false));
		hasTrigger.setTypeEquals(BaseType.BOOLEAN);

		singletons = new TreeMap();

		singletons.put("trigger", hideTrigger);
		singletons.put("newPath", hideNewPath);
		singletons.put("permission", hidePermission);
		singletons.put("userName", hideUserName);
		singletons.put("mdasDomain", hideMdasDomain);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	public SingletonParameter hideTrigger;
	public SingletonParameter hideNewPath;
	public SingletonParameter hidePermission;
	public SingletonParameter hideUserName;
	public SingletonParameter hideMdasDomain;

	/**
	 * SRB file system reference.
	 */
	public TypedIOPort SRBFileSystem;

	/**
	 * Path to SRB file.
	 */
	public TypedIOPort path;

	/**
	 * New file path.
	 */
	public TypedIOPort newPath;

	/**
	 * Files permissions.
	 */
	public TypedIOPort permission;
	// public TypedIOPort output;

	/**
	 * Execution exit code.
	 */
	public TypedIOPort exitCode;
	/**
	 * User name.
	 */
	public TypedIOPort userName;

	/**
	 * Meta data domain.
	 */
	public TypedIOPort mdasDomain;

	/**
	 * The proxy command to be performed.
	 */
	public StringParameter command;

	/**
	 * Specify whether to display the complete Sls result at once or each file
	 * separately.
	 */
	public Parameter outputEachFileSeparately;

	/**
	 * Specify whether to output the parent directories of removed
	 * files/directories or have a sink actor.
	 */
	public Parameter forwardParentDir;

	/**
	 * Specify whether to remove recursively.
	 */
	public Parameter _r;

	/**
	 * An input trigger.
	 */
	public TypedIOPort trigger;

	/**
	 * Specify whether to add a trigger port.
	 */
	public Parameter hasTrigger;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Callback for changes in attribute values. Set the action specified by the
	 * selected command.
	 * 
	 * @param a
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 *                If the offsets array is not nondecreasing and nonnegative.
	 */
	public void attributeChanged(Attribute at) throws IllegalActionException {
		if (at == command) {
			// command changed flag
			boolean flag = true;// (!prevCmd.equals("") &&
								// !prevCmd.equals(command.getExpression()));
			String cmd = command.stringValue();
			prevCmd = cmd;
			// lists for adding removing ports by command.
			// The ports necessary for all commands are srbFileSystem, path and
			// exitCode.
			List reqInputPorts = new LinkedList();
			// List reqOutputPorts = new LinkedList();
			if (cmd.equals("list directory")) {
				if (flag) {
					// reqOutputPorts.add("output");
					_setInputPorts(reqInputPorts); // "listedFiles");
					// add output port if not exists.
					_setOutputPort("listedFiles");
					// _addPorts(this.outputPortList(), reqOutputPorts);

					/*
					 * if (output.getContainer() == null) { try {
					 * output.setContainer(this); } catch
					 * (NameDuplicationException ex) {
					 * GraphicalMessageHandler.message(ex.getMessage() +
					 * "Could not add input ports in the actor: " +
					 * this.getName() + " for command " + cmd); } }
					 */
				}
				_cmd = _LS;
			} else if (cmd.equals("copy")) {
				if (flag) {
					reqInputPorts.add("newPath");
					// reqOutputPorts.add("output");
					_setInputPorts(reqInputPorts); // "copiedFiles"); // FIXME:
													// or should this be new
													// location??
					// _addPorts(this.outputPortList(), reqOutputPorts);
					_setOutputPort("copiedFiles");
					try {
						// newPath.setContainer(this);
						hideNewPath.setToken(BooleanToken.FALSE);
						// if (output.getContainer() == null)
						// output.setContainer(this);
					} catch (Exception ex) {
						GraphicalMessageHandler.message(ex.getMessage()
								+ "Could not add input ports in the actor: "
								+ this.getName() + " for command " + cmd);
					}
				}
				_cmd = _CP;
			} else if (cmd.equals("move")) {
				if (flag) {
					reqInputPorts.add("newPath");
					// reqOutputPorts.add("output");
					_setInputPorts(reqInputPorts); // "movedFiles"); // FIXME:
													// or should this be new
													// location??
					// _addPorts(this.outputPortList(), reqOutputPorts);
					_setOutputPort("movedFiles");
					try {
						// newPath.setContainer(this);
						hideNewPath.setToken(BooleanToken.FALSE);
						// if (output.getContainer() == null)
						// output.setContainer(this);
					} catch (Exception ex) {
						GraphicalMessageHandler.message(ex.getMessage()
								+ "Could not add input ports in the actor: "
								+ this.getName() + " for command " + cmd);
					}
				}
				_cmd = _MV;
			} else if (cmd.equals("remove")) {
				if (flag) {
					_setInputPorts(reqInputPorts); // "parentDir");
					// forwardParentDir.setToken(new BooleanToken(false));
					attributeChanged(forwardParentDir);
				}
				_cmd = _RM;
			} else if (cmd.equals("replicate")) {
				if (flag) {
					reqInputPorts.add("newPath");
					// reqOutputPorts.add("output");
					_setInputPorts(reqInputPorts); // "newResource");
					// _addPorts(this.outputPortList(), reqOutputPorts);
					_setOutputPort("newResource");
					try {
						// newPath.setContainer(this);
						hideNewPath.setToken(BooleanToken.FALSE);
						// if (output.getContainer() == null)
						// output.setContainer(this);
					} catch (Exception ex) {
						GraphicalMessageHandler.message(ex.getMessage()
								+ "Could not add input ports in the actor: "
								+ this.getName() + " for command " + cmd);
					}
				}
				_cmd = _REP;
			} else if (cmd.equals("create directory")) {
				if (flag) {
					// reqOutputPorts.add("output");
					_setInputPorts(reqInputPorts); // "dirPath");
					// _addPorts(this.outputPortList(), reqOutputPorts);
					_setOutputPort("dirPath");
					/*
					 * if (output.getContainer() == null) { try {
					 * output.setContainer(this); } catch
					 * (NameDuplicationException ex) {
					 * GraphicalMessageHandler.message(ex.getMessage() +
					 * "Could not add output ports in the actor: " +
					 * this.getName() + " for command " + cmd); } }
					 */
				}
				_cmd = _MKDIR;
			} else if (cmd.equals("remove directory")) {
				if (flag) {
					_setInputPorts(reqInputPorts); // "parentDir");
					// forwardParentDir.setToken(new BooleanToken(false));
					attributeChanged(forwardParentDir);
				}
				_cmd = _RMDIR;
			} else if (cmd.equals("change mode")) {
				if (flag) {
					reqInputPorts.add("permission");
					reqInputPorts.add("mdasDomain");
					reqInputPorts.add("userName");
					// reqOutputPorts.add("output");
					_setInputPorts(reqInputPorts); // "exitPath");
					// _addPorts(this.outputPortList(), reqOutputPorts);
					_setOutputPort("exitPath");
					try {
						// permission.setContainer(this);
						hidePermission.setToken(BooleanToken.FALSE);
						// mdasDomain.setContainer(this);
						hideMdasDomain.setToken(BooleanToken.FALSE);
						// userName.setContainer(this);
						hideUserName.setToken(BooleanToken.FALSE);

						// if (output.getContainer() == null) {
						// output.setContainer(this);
						// }
					} catch (Exception ex) {
						GraphicalMessageHandler.message(ex.getMessage()
								+ "Could not add input ports in the actor: "
								+ this.getName() + " for command " + cmd);
					}
				}
				_cmd = _CHMOD;
			} else {
				// execute proxy command.
			}
		} else if (at == hasTrigger) {
			_triggerFlag = ((BooleanToken) hasTrigger.getToken())
					.booleanValue();
			_debug("<TRIGGER_FLAG>" + _triggerFlag + "</TRIGGER_FLAG>");
			if (_triggerFlag) {
				// trigger.setContainer(this);
				hideTrigger.setToken(BooleanToken.FALSE);
			} else {
				List inPortList = this.inputPortList();
				Iterator ports = inPortList.iterator();
				while (ports.hasNext()) {
					IOPort p = (IOPort) ports.next();
					if (p.isInput()) {
						try {
							if (p.getName().equals("trigger")) {
								// p.setContainer(null);
								hideTrigger.setToken(BooleanToken.TRUE);
							}
						} catch (Exception e) {
							GraphicalMessageHandler
									.message(e.getMessage()
											+ "Could not delete the trigger port in the actor: "
											+ this.getName());
						}
					}
				}
			}
		} else if (at == outputEachFileSeparately) {
			_separate = ((BooleanToken) outputEachFileSeparately.getToken())
					.booleanValue();
		} else if (at == forwardParentDir) {
			_forward = ((BooleanToken) forwardParentDir.getToken())
					.booleanValue();
			if ((command.stringValue()).startsWith("remove")) {
				if (_forward) {
					_setOutputPort("parentDirs");

					/*
					 * try { if (output.getContainer() == null)
					 * output.setContainer(this); } catch
					 * (NameDuplicationException ndex) {
					 * GraphicalMessageHandler.message(ndex.getMessage() +
					 * "Could not create output port."); }
					 */
				} else {
					List outPorts = this.outputPortList();
					Iterator ports = outPorts.iterator();
					while (ports.hasNext()) {
						IOPort p = (IOPort) ports.next();
						if (!p.getName().equals("exitCode")) {// this is the
																// output port.
							try {
								p.setContainer(null);
							} catch (Exception ndex) {
								GraphicalMessageHandler.message(ndex
										.getMessage()
										+ "Could not delete output port.");
							}
						}
					}
				}
			}
		} else {
			super.attributeChanged(at);
		}
	}

	/**
	 * Upload the file to the SRB. If the SRB file path is not specified, upload
	 * to the current working directory. Output the current working directory.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown if the SRB file cannot be accessed or the
	 *                current directory cannot be broadcasted.
	 */
	public void fire() throws IllegalActionException {
		String srcPathStr;
		String newLoc;
		SRBFile srcSrbFile;
		_exitCode = "";

		List inPortList = this.inputPortList();
		Iterator ports = inPortList.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (p.getName().equals("trigger")) {
				if (p.getWidth() > 0) {
					for (int i = 0; i < p.getWidth(); i++) {
						p.get(0);
					}
				}
			}
		}

		try {
			srbFileSystem.getHost();
		} catch (Exception ex) { // connection was closed.
			srbFileSystem = null;
			ObjectToken SRBConOT = null;
			try { // try to get a new connection in case the previous one has
					// terminated.
				SRBConOT = (ObjectToken) SRBFileSystem.get(0);
			} catch (NoTokenException ntex) {
			}
			if (SRBConOT != null) {
				srbFileSystem = (SRBFileSystem) SRBConOT.getValue();
			}
		}
		if (srbFileSystem == null) {
			throw new IllegalActionException(this,
					"No SRB connection available in actor " + this.getName()
							+ ".");
		}
		ArrayToken atPath = (ArrayToken) path.get(0);
		Token[] paths = atPath.arrayValue();
		IOPort output = _getOutputPort();

		switch (_cmd) {
		case _LS:

			System.out.println("actor name ==> " + this.getName() + " : Sls");
			// TODO: output each row seperately or as a list. output to a
			// browser display (add the buttons).
			if (paths.length > 1) {
				_exitCode += paths.length + " directories were displayed.\n";
			}
			for (int i = 0; i < paths.length; i++) {
				srcPathStr = ((StringToken) paths[i]).stringValue();
				srcSrbFile = new SRBFile(srbFileSystem, srcPathStr);
				if (srcSrbFile.exists()) {
					if (srcSrbFile.isDirectory()) {
						String[] files = srcSrbFile.list();
						if (files != null && files.length > 0) {
							if (_separate) {
								for (int j = 0; j < files.length; j++) {
									String absPath = srcSrbFile
											.getAbsolutePath()
											+ "/" + files[j];
									Token[] _file = new Token[1];
									_file[0] = new StringToken(absPath);
									output.broadcast(new ArrayToken(_file));
								}
							} else {
								Token _files[] = new Token[files.length];
								for (int j = 0; j < files.length; j++) {
									String absPath = srcSrbFile
											.getAbsolutePath()
											+ "/" + files[j];
									_files[j] = new StringToken(absPath);
								}
								output.broadcast(new ArrayToken(_files));
							}
						}
					} else { // file is not a directory
						GraphicalMessageHandler.message(srcSrbFile
								.getAbsolutePath()
								+ " is not a directory.");
					}
				} else {
					GraphicalMessageHandler
							.message("Directory "
									+ srcSrbFile.getAbsolutePath()
									+ " does not exist.");
				}
			}
			if (_exitCode.equals("")) {
				_exitCode = "success";
			}
			exitCode.broadcast(new StringToken(_exitCode));
			break;
		case _CP:
			System.out.println("actor name ==> " + this.getName() + " : Scp");

			_cp_mv(srbFileSystem, paths);
			break;
		case _MV:
			System.out.println("actor name ==> " + this.getName() + " : Smv");

			_cp_mv(srbFileSystem, paths);
			// TODO: move a whole directory..
			break;
		case _RM:
			System.out.println("actor name ==> " + this.getName() + " : Srm");

			recursive = ((BooleanToken) _r.getToken()).booleanValue();
			for (int i = 0; i < paths.length; i++) {
				srcPathStr = ((StringToken) paths[i]).stringValue();
				SRBFile srbFile = new SRBFile(srbFileSystem, srcPathStr);
				String parent = srbFile.getParent();
				if (_rm(srbFileSystem, srcPathStr))
					deletedFiles.add(new StringToken(parent));
			}
			if (_forward) { // currently output parent dirs.
				if (deletedFiles.size() > 0) {
					Token _output[] = new Token[deletedFiles.size()];
					deletedFiles.toArray(_output);
					output.broadcast(new ArrayToken(_output));
				} else
					System.out.println("no files were removed.");
			}
			if (_exitCode.equals(""))
				_exitCode = "success";
			exitCode.broadcast(new StringToken(_exitCode));
			// FIXME: what should be returned here?? the files are not
			// necessarily from the same dir.
			break;
		case _REP:
			System.out.println("actor name ==> " + this.getName() + " : Srep");

			String newResource = ((StringToken) newPath.get(0)).stringValue();
			Token[] reps = new Token[paths.length];
			for (int i = 0; i < paths.length; i++) {
				srcPathStr = ((StringToken) paths[i]).stringValue();
				srcSrbFile = new SRBFile(srbFileSystem, srcPathStr);
				try {
					srcSrbFile.replicate(newResource);
					reps[i] = new StringToken(newResource + "/" + srcPathStr);
				} catch (IOException e) {
					srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
					srcSrbFile = null;
					GraphicalMessageHandler.message("Could not replicate file "
							+ srcSrbFile.getAbsolutePath() + " to resource "
							+ newResource + ".");
				}
			}
			output.broadcast(new ArrayToken(reps));
			exitCode.broadcast(new StringToken("success"));
			break;
		case _MKDIR: // take care of creating several directories at once.
			System.out
					.println("actor name ==> " + this.getName() + " : Smkdir");

			for (int i = 0; i < paths.length; i++) {
				srcPathStr = ((StringToken) paths[i]).stringValue();
				srcSrbFile = new SRBFile(srbFileSystem, srcPathStr);
				System.out.println("file " + srcPathStr + " is a directory ? "
						+ srcSrbFile.isDirectory());
				if (!srcSrbFile.mkdir()) {
					System.out
							.println("Directory "
									+ srcSrbFile.getAbsolutePath()
									+ " already exists.");
					_exitCode += "Directory " + srcSrbFile.getAbsolutePath()
							+ " already exists.";
				}
			}
			output.broadcast(atPath);
			if (_exitCode.equals(""))
				_exitCode = "success";
			exitCode.broadcast(new StringToken(_exitCode));
			break;
		case _RMDIR:
			System.out
					.println("actor name ==> " + this.getName() + " : Srmdir");

			Token[] fwd = new Token[paths.length];
			for (int i = 0; i < paths.length; i++) {
				srcPathStr = ((StringToken) paths[i]).stringValue();
				srcSrbFile = new SRBFile(srbFileSystem, srcPathStr);
				String parent = srcSrbFile.getParent();
				if (srcSrbFile.exists())
					if (srcSrbFile.isDirectory()) {
						String[] files = srcSrbFile.list();
						if (files.length > 0) {// directory is not empty!
							srbFileSystem = SRBUtil
									.closeConnection(srbFileSystem);
							srcSrbFile = null;
							throw new IllegalActionException(this,
									"Cannot remove directory "
											+ srcSrbFile.getName()
											+ ". Directory "
											+ srcSrbFile.getAbsolutePath()
											+ " is not empty.");
						} else {
							srcSrbFile.delete();
							fwd[i] = new StringToken(parent);
						}
					} else
						GraphicalMessageHandler.message("directory "
								+ srcSrbFile.getAbsolutePath()
								+ " does not exist.");
			}
			if (_forward)
				output.broadcast(new ArrayToken(fwd));
			exitCode.broadcast(new StringToken("success"));
			// TODO: what should be outputed here? remove non-empty dirs flag.
			break;
		case _CHMOD:
			System.out
					.println("actor name ==> " + this.getName() + " : Schmod");

			String permissionStr = ((StringToken) permission.get(0))
					.stringValue();
			String userNameStr = ((StringToken) userName.get(0)).stringValue();
			String mdasDomainStr = ((StringToken) mdasDomain.get(0))
					.stringValue();
			for (int i = 0; i < paths.length; i++) {
				srcPathStr = ((StringToken) paths[i]).stringValue();
				srcSrbFile = new SRBFile(srbFileSystem, srcPathStr);
				if (srcSrbFile.exists())
					try {
						srcSrbFile.changePermissions(permissionStr,
								userNameStr, mdasDomainStr);
					} catch (IOException e) {
						srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
						srcSrbFile = null;
						e.printStackTrace();
						throw new IllegalActionException(this,
								"Could not change premissions for file "
										+ srcSrbFile.getName() + " in actor "
										+ this.getName() + ".");
					}
				else {
					srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
					GraphicalMessageHandler
							.message("file " + srcSrbFile.getAbsolutePath()
									+ " does not exist.");
				}
			}
			output.broadcast(atPath);
			exitCode.broadcast(new StringToken("success"));
			break;
		default:
			// Execute proxy command???
			// System.out.println(command.getExpression() +
			// " is not supported");
		}

	}// end of fire

	/**
	 * Initialize the srb file system to null.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		srbFileSystem = null;
	}

	/**
	 * Disconnect from SRB.
	 */
	public void wrapup() {
		srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Copy or move a file to a new location. Recursively copies/moves non empty
	 * directories
	 * 
	 * @param srbFileSystem
	 *            The srb file system.
	 * @param paths
	 *            Source files paths.
	 * @throws IllegalActionException
	 *             If the new path cannot be consumed.
	 */
	private void _cp_mv(SRBFileSystem srbFileSystem, Token[] paths)
			throws IllegalActionException {
		// take care of files already exist. exception within rename - returns
		// boolean.
		StringToken newPathToken = (StringToken) newPath.get(0);
		String newLoc = newPathToken.stringValue();
		SRBFile newFilePath = new SRBFile(srbFileSystem, newLoc);
		Vector _outputVec = new Vector();

		for (int i = 0; i < paths.length; i++) {
			if (!newFilePath.isDirectory() && paths.length > 1) {
				// Several files are copied to a single file.
				GraphicalMessageHandler
						.message("Trying to copy several files to file "
								+ newFilePath.getAbsolutePath());
			}
			String srcPathStr = ((StringToken) paths[i]).stringValue();
			SRBFile srcSrbFile = new SRBFile(srbFileSystem, srcPathStr);
			if (!newFilePath.isDirectory() && srcSrbFile.isDirectory()) {
				// Trying to copy a directory to a single file.
				GraphicalMessageHandler.message("Trying to copy directory "
						+ srcPathStr + " to file "
						+ newFilePath.getAbsolutePath());
			}
			if (srcSrbFile.exists()) {
				try {
					String newPathStr = newFilePath.getAbsolutePath(); // if
																		// this
																		// is a
																		// file
																		// name.
					if (newFilePath.isDirectory()) { // if this is a directory
														// add the original file
														// name.
						newPathStr += "/" + srcSrbFile.getName();
					}
					if (_cmd == _CP) {
						srcSrbFile
								.copyTo(new SRBFile(srbFileSystem, newPathStr));
					} else if (_cmd == _MV) {
						srcSrbFile.renameTo(new SRBFile(srbFileSystem,
								newPathStr));
					}
					_outputVec.add(new StringToken(newPathStr));
				} catch (IOException e) {
					srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
					srcSrbFile = null;
					GraphicalMessageHandler.message("cannot copy/move file "
							+ srcSrbFile.getAbsolutePath() + " to " + newPath
							+ ".");
				}
			} else
				_exitCode += "file " + srcSrbFile.getAbsolutePath()
						+ " does not exist.";
		}
		// if files were copied.
		if (_outputVec.size() > 0) {
			Token[] _outputs = new Token[_outputVec.size()];
			_outputVec.toArray(_outputs);
			IOPort output = _getOutputPort();
			output.broadcast(new ArrayToken(_outputs));
		} else {
			_exitCode = "no files were copied/moved";
		}

		if (_exitCode.equals("")) {
			_exitCode = "success";
		}
		exitCode.broadcast(new StringToken(_exitCode));
	}

	/**
	 * Returns the output port if exists.
	 * 
	 * @return output port/null.
	 */
	private IOPort _getOutputPort() {
		List outPorts = this.outputPortList();
		Iterator ports = outPorts.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (!p.getName().equals("exitCode")) { // this is the output port.
				return p;
			}
		}
		return null; // no output port exists.
	}

	/**
	 * Removes a file or a directory. For non-empty directories, recursively
	 * removes the files depending on the -r flag.
	 * 
	 * @param srbFileSystem
	 *            The srb file system.
	 * @param path
	 *            File path.
	 * @throws IllegalActionException
	 *             If the recursive parameter cannot be consumed.
	 */
	private boolean _rm(SRBFileSystem srbFileSystem, String path)
			throws IllegalActionException {
		SRBFile srbFile = new SRBFile(srbFileSystem, path);
		if (srbFile.exists()) {
			if (srbFile.isDirectory()) {
				String files[] = srbFile.list();
				if (files.length > 0) {
					if (recursive) {
						// directory is non-empty - recursively deletes files.
						for (int i = 0; i < files.length; i++) {
							String filePath = srbFile.getAbsolutePath() + "/"
									+ files[i];
							_rm(srbFileSystem, filePath);
						}
						// now this dir is empty.
						srbFile.delete();
						return true;
					} else {
						_exitCode += "Directory " + srbFile.getAbsolutePath()
								+ " is not empty\n";
						return false;
					}
				} else { // this is an empty directory
					srbFile.delete();
					return true;
				}
			} else {
				// this is a file.
				srbFile.delete();
				return true;
			}
		} else {
			_exitCode += "file " + srbFile.getAbsolutePath()
					+ " does not exist.";
			return false;
		}
	}

	/**
	 * Set the actor's ports. All commands share the path input port. Add/remove
	 * input ports specific to the command. Set the output port name specific to
	 * the command.
	 * 
	 * @param outName
	 *            Set the output port name commandwise.
	 * @throws IllegalActionException
	 *             If the output name cannot be set.
	 */

	// private void _setPorts(String outName) throws IllegalActionException {
	private void _setInputPorts(List reqInputs) throws IllegalActionException {
		List inPortList = this.inputPortList();
		_triggerFlag = ((BooleanToken) hasTrigger.getToken()).booleanValue();
		reqInputs.add("SRBFileSystem");
		reqInputs.add("path");
		if (_triggerFlag)
			reqInputs.add("trigger");
		// _addPorts(inPortList, reqInputs);
		_removePorts(inPortList, reqInputs);
	}

	/**
	 * Set the output port container and name.
	 */
	private void _setOutputPort(String outName) throws IllegalActionException {
		List outPortList = this.outputPortList();
		Iterator ports = outPortList.iterator();
		boolean portExists = false; // checking whether the output port exists.
		while (ports.hasNext()) {
			TypedIOPort p = (TypedIOPort) ports.next();
			if (!p.getName().equals("exitCode")) { // this is the output port.
				p.setTypeEquals(new ArrayType(BaseType.STRING));
				portExists = true;
				if (!p.getName().equals(outName)) { // this output port doesn't
													// have the desired name.
					try {
						p.setName(outName);
					} catch (NameDuplicationException ex) {
						srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
						GraphicalMessageHandler.message(ex.getMessage()
								+ " Actor " + this.getName()
								+ " already contains a port " + "named "
								+ outName);
					}
				}
			}
		}
		if (!portExists) {
			try {
				TypedIOPort p = new TypedIOPort(this, outName, false, true);
				p.setTypeEquals(new ArrayType(BaseType.STRING));
				new Attribute(p, "_showName");
			} catch (NameDuplicationException ex) {
				srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
				GraphicalMessageHandler.message(ex.getMessage()
						+ " Could not add port " + outName + " to actor "
						+ this.getName());
			}
		}
	}

	/*
	 * private void _addPorts(List portList, List reqPortsList) { Iterator
	 * reqPorts = reqPortsList.iterator(); // for each required port, make sure
	 * it exists in the input ports and if its container is null set it to this.
	 * while (reqPorts.hasNext()) { boolean exists = false; Iterator ports =
	 * portList.iterator(); String reqPortName = (String)reqPorts.next(); while
	 * (ports.hasNext()) { IOPort p = (IOPort) ports.next(); if
	 * (p.getName().equals(reqPortName) && !p.getContainer().equals(this)) {//
	 * the required port is within the portList. try { p.setContainer(this); }
	 * catch (Exception ex) { GraphicalMessageHandler.message(ex.getMessage() +
	 * "Could not delete input port " + p.getName() + " in actor: " +
	 * this.getName() + "for command " + command.getExpression()); } } } }
	 * 
	 * }
	 */

	/**
	 * Remove unnecessary port for a specific command.
	 */
	private void _removePorts(List portList, List reqPortsList) {
		Iterator ports = portList.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			String portName = p.getName();
			try {
				if (!reqPortsList.contains(portName)) { // this port is not
														// required.
					SingletonParameter sp = (SingletonParameter) singletons
							.get(p.getName());
					// p.setContainer(null); /// input
					sp.setToken(BooleanToken.TRUE);
				}
			} catch (Exception ex) {
				srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
				GraphicalMessageHandler.message(ex.getMessage()
						+ "Could not delete input port " + portName
						+ " in actor: " + this.getName() + "for command "
						+ command.getExpression());
			}

		}

	}

	// FIXME: TEMPORARILY REMOVED. PROBLEMS WITH CHANGING
	// THE OUTPUT PORT NAME WHEN RELOADING A WORKFLOW.
	/*
	 * try { output.setName(outName); } catch (NameDuplicationException ex) {
	 * GraphicalMessageHandler.message(ex.getMessage() + "Actor " +
	 * this.getName() + " contains a port " + "named " + outName) ; }
	 */

	// /////////////////////////////////////////////////////////////////
	// // private members ////
	/** User selected command */
	private int _cmd;
	String prevCmd = "";
	String _exitCode = "";
	Vector deletedFiles = new Vector();

	/**
	 * Output separately indicator parameter.
	 */
	private boolean _separate = false;

	/**
	 * Broadcast results indicator parameter.
	 */
	private boolean _forward = false;

	/**
	 * Indicates the existence of an input trigger
	 */
	private boolean _triggerFlag;

	/**
	 * Indicates recursively remove.
	 */
	private boolean recursive = false;

	private SRBFileSystem srbFileSystem = null;

	// Constants used for more efficient execution.
	private static final int _LS = 0;
	private static final int _CP = 1;
	private static final int _MV = 2;
	private static final int _RM = 3;
	private static final int _REP = 4;
	private static final int _MKDIR = 5;
	private static final int _RMDIR = 6;
	private static final int _CHMOD = 7;

	private Map singletons;
}