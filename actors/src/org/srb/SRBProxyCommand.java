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

// Ptolemy packages
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import ptolemy.actor.IOPort;
import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.util.MessageHandler;
import edu.sdsc.grid.io.srb.SRBFileSystem;

//////////////////////////////////////////////////////////////////////////
//// SRBProxyCommand
/**
 * <p>
 * SProxyCommand is a Kepler Actor which has a functionality similar to the SRB
 * command namely "Spcommand". Spcommand performs the remote execution of
 * arbitrary commands (executables) installed in a specific predefined directory
 * on the remote host. SProxyCommand actor executes proxy commands on the SRB.
 * The following actor expects as input a reference to the SRB file system. This
 * reference connection is obtained via the SRBConnect Actor in Kepler. <i>See
 * SRBConnect and its documentation.</i>
 * </p>
 * <p>
 * The file reference system is created with a unique SRB user account and with
 * this connection reference as input the SProxyCommand actor is able to gain
 * access to the SRB file space. Once an alive SRB file connection system has
 * been established the actor gets the command port and parameters to be
 * executed as input from the user.The arguments to the command are take in as
 * input and implemented as a multi/input port to support more than one
 * argument. It concatanates the inputs in all the channels.
 * </p>
 * <p>
 * <B>Actor Input:</B> Accepts a reference to the SRB files system, command and
 * arguments to be executed as well as the outputfile name (optional)
 * </p>
 * <p>
 * <B>Actor Output:</B> Outputs the result of execution of the proxy command as
 * well as an outputfilehandle (if the outfile path exists)
 * 
 * </p>
 * <p>
 * The following actor accesses SRB file reference system and SRB file space
 * with the SRB Jargon API provided. The JARGON is a pure API for developing
 * programs with a data grid interface and I/O for SRB file systems.
 * </p>
 * <A href="http://www.sdsc.edu/srb"><I>Further information on SRB</I> </A>
 * 
 * 
 @author Efrat Jaeger
 * @version $Id: SRBProxyCommand.java 24234 2010-05-06 05:21:26Z welker $
 */

public class SRBProxyCommand extends TypedAtomicActor {

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
	public SRBProxyCommand(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		SRBFileSystem = new TypedIOPort(this, "SRBFileSystem", true, false);
		SRBFileSystem.setTypeEquals(BaseType.GENERAL);
		new Attribute(SRBFileSystem, "_showName");

		commandParameter = new StringParameter(this, "commandParameter");
		commandParameter.setExpression("Please type your command here...");

		command = new TypedIOPort(this, "command", true, false);
		command.setTypeEquals(BaseType.STRING);
		new Attribute(command, "_showName");

		outputFile = new FileParameter(this, "outputFile");
		// Construct input ports.
		arguments = new TypedIOPort(this, "arguments", true, false);
		arguments.setMultiport(true);
		arguments.setTypeEquals(BaseType.STRING);
		new Attribute(arguments, "_showName");

		outputFileName = new TypedIOPort(this, "outputFileName", true, false);
		outputFileName.setTypeEquals(BaseType.STRING);
		new Attribute(outputFileName, "_showName");

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);
		new Attribute(output, "_showName");

		outfileHandle = new TypedIOPort(this, "outfileHandle", false, true);
		outfileHandle.setTypeEquals(BaseType.STRING);
		new Attribute(outfileHandle, "_showName");

		outputLineByLine = new Parameter(this, "outputLineByLine",
				new BooleanToken(false));
		outputLineByLine.setTypeEquals(BaseType.BOOLEAN);

		hasTrigger = new Parameter(this, "hasTrigger", new BooleanToken(false));
		hasTrigger.setTypeEquals(BaseType.BOOLEAN);

		trigger = new TypedIOPort(this, "trigger", true, false);
		hide = new SingletonParameter(trigger, "_hide");
		hide.setToken(BooleanToken.TRUE);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"40\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"4\" y=\"25\" "
				+ "style=\"font-size:16; fill:blue; font-family:SansSerif\">"
				+ "[SRB]</text>\n" + "<text x=\"45\" y=\"27\" "
				+ "style=\"font-size:20; fill:blue; font-family:SansSerif\">"
				+ "$</text>\n" + "</svg>\n");

	} // constructor

	// //////////////// Public ports and parameters ///////////////////////

	public SingletonParameter hide;

	/**
	 * pointer to the SRB file system.
	 */
	public TypedIOPort SRBFileSystem;

	/**
	 * command port to be executed.
	 */
	public TypedIOPort command;

	/**
	 * command parameter to be executed.
	 */
	public StringParameter commandParameter;

	/**
	 * Filled in if the user wants the command to output to a file.
	 */
	public FileParameter outputFile;

	/**
	 * The output file name is set by previous processes.
	 */
	public TypedIOPort outputFileName;

	/**
	 * The arguments to the command. Implemented as a multi/input port to
	 * support more than one argument. It concatanates the inputs in all the
	 * channels.
	 */
	public TypedIOPort arguments;

	/**
	 * The trigger port.
	 */
	public TypedIOPort trigger;

	/**
	 * The output file path, if exists.
	 */
	public TypedIOPort outfileHandle;
	/**
	 * The result stream of the command.
	 */
	public TypedIOPort output;
	// ** exitCode will be 1 if the command executes successfully.
	// */
	// public TypedIOPort exitCode;
	/**
	 * If selected, broadcasts the output of the command line by line.
	 */
	public Parameter outputLineByLine;

	/**
	 * Unhide the trigger port when this parameter is true. This Parameter is
	 * type of boolean. NOTE: in fact, user can use the port configuration
	 * window to hide or unhide a port. This paremeter is here to provide a more
	 * intuitive interface for this actor.
	 */
	public Parameter hasTrigger;

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
		if (attribute == hasTrigger) {
			_triggerFlag = ((BooleanToken) hasTrigger.getToken())
					.booleanValue();
			_debug("<TRIGGER_FLAG>" + _triggerFlag + "</TRIGGER_FLAG>");
			if (_triggerFlag) {
				hide.setToken(BooleanToken.FALSE);
			} else {
				List inPortList = this.inputPortList();
				Iterator ports = inPortList.iterator();
				while (ports.hasNext()) {
					IOPort p = (IOPort) ports.next();
					if (p.isInput()) {
						try {
							if (p.getName().equals("trigger")) {
								// new Attribute(trigger, "_hideName");
								// p.setContainer(null);
								hide.setToken(BooleanToken.TRUE);
							}
						} catch (Exception e) {
							throw new IllegalActionException(this, e
									.getMessage());
						}
					}
				}// while
			}// else
		} else {
			super.attributeChanged(attribute);
		}
	}

	/**
	 * Sends a proxy command to be executed on SRB.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		if (_triggerFlag) {
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
		}
		// make sure there is an alive connection.
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

		_lineFlag = ((BooleanToken) outputLineByLine.getToken()).booleanValue();
		_debug("<TRIGGER_FLAG>" + _lineFlag + "</TRIGGER_FLAG>");

		if (command.getWidth() > 0) {
			commandParameter.setExpression(((StringToken) command.get(0))
					.stringValue());
		}

		_commandStr = ((StringToken) commandParameter.getToken()).stringValue();

		String argString = "";
		int i = 0;
		int width = arguments.getWidth();
		for (i = 0; i < width; i++) {
			if (arguments.hasToken(i)) {
				String argument = ((StringToken) arguments.get(i))
						.stringValue();
				_debug("arguments(i) = " + argument);

				while (argument.indexOf("\\\"") != -1) {
					int ind = argument.indexOf("\\\"");
					argument = argument.substring(0, ind)
							+ argument.substring(ind + 1, argument.length());
					_debug(argument);
				}
				argString += argument + " ";
				_debug("argString = " + argString);
			}
		}

		StringBuffer outBuff = new StringBuffer("");
		DataInputStream inStream = null;
		OutputStream out = null;
		byte[] bytesRead = new byte[20000];
		;
		int nBytesRead;
		String outFilePath = "";
		try {
			if (outputFileName.getWidth() > 0) {
				String outFileName = ((StringToken) outputFileName.get(0))
						.stringValue();
				outputFile.setExpression(outFileName);
			}
			// opening output file stream
			if (!outputFile.getExpression().equals("")) {
				outFilePath = outputFile.asURL().toString();
				File outFile = outputFile.asFile();
				File parent = outFile.getParentFile();
				if (!parent.exists()) {
					if (!MessageHandler.yesNoQuestion("OK to create directory "
							+ parent.getAbsolutePath() + "?")) {
						srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
						throw new IllegalActionException(this,
								"Please select another output directory name.");
					}
				}

				parent.mkdirs();
				if (outFile.exists()) {
					if (!MessageHandler.yesNoQuestion("OK to overwrite "
							+ outFilePath + "?")) {
						srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
						throw new IllegalActionException(this,
								"Please select another output file name.");
					}
				}

				out = new FileOutputStream(outFile);
			}

			// Executing the proxy command.
			try {
				inStream = (DataInputStream) srbFileSystem.executeProxyCommand(
						_commandStr, argString);
			} catch (IOException ioex) {
				srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
				ioex.printStackTrace();
				throw new IllegalActionException(this,
						"Failed to execute SRB proxy command " + _commandStr
								+ "in actor " + this.getName() + ": "
								+ ioex.getMessage() + ".");
			}

			// processing the result.
			nBytesRead = inStream.read(bytesRead);
			while (nBytesRead > 0) {
				// if there is a specified file, write to it.
				if (out != null) {
					out.write(bytesRead, 0, nBytesRead);
				}
				// append binary result to a string buffer.
				outBuff.append(new String(bytesRead, 0, nBytesRead));
				nBytesRead = inStream.read(bytesRead);
			}
			if (out != null)
				out.close();

			// process string result.
			if (_lineFlag) { // output each line separately.
				BufferedReader br = new BufferedReader(new StringReader(outBuff
						.toString()));
				String line;
				while ((line = br.readLine()) != null) {
					output.broadcast(new StringToken(line));
				}
				// output the whole result string at once.
			} else {
				output.broadcast(new StringToken(outBuff.toString()));
			}
		} catch (IOException ioe) {
			srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
			_debug("<IOException> when reading the input: " + ioe
					+ "</IOException>");
			throw new IllegalActionException(this,
					"IOException when reading the input: " + ioe);
		}

		// output out file handle if exists.
		if (!outFilePath.equals("")) {
			outfileHandle.broadcast(new StringToken(outFilePath));
		}
	}

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

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////

	/**
	 * Command to be executed.
	 */
	private String _commandStr = "";

	/**
	 * Indicator to output each line separately.
	 */
	private boolean _lineFlag = false;

	/**
	 * Has trigger indicator
	 */
	private boolean _triggerFlag = false;
	// private int _charsToSkip = 6;

	/**
	 * An srb file system variable.
	 */
	private SRBFileSystem srbFileSystem = null;
}