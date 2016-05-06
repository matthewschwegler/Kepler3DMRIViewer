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

package org.sdm.spa;

// Ptolemy packages
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// CommandLine
/**
 * Given a command string, the <I>CommandLine</I> actor executes it using the
 * java Runtime class.
 * <p>
 * <b>SUPPORTED COMMAND TYPES:</b>
 * <ul>
 * <li>command
 * <li>command < infile > outfile
 * <li>command > outfile
 * <li>command < infile
 * <li>command [arg1..argn] > outfile
 * <li>command [arg1..argn]
 * <li>command [arg1..argn] < infile > outfile
 * <li>command1 | command 2 <I>(<B>Warning</B>: This type of commands doesn't
 * give the output of all the commands. Instead it outputs only the result of
 * the last one.)</I>
 * </ul>
 * <p>
 * <b>Example commands:</b>
 * <ul>
 * <li>C:/Program Files/Internet Explorer/IEXPLORE.EXE <br>
 * To generate this command, just double click on the actor and type this in the
 * <i>command</i> parameter field.
 * <li>C:/cygwin/bin/perl.exe c:/project/kepler/test/workflows/example.pl >
 * c:/project/kepler/test/workflows/example.out
 * <li>C:/cygwin/bin/dir.exe > dirTemp.txt
 * </ul>
 * 
 * @author Ilkay Altintas
 * @version $Id: CommandLine.java 24234 2010-05-06 05:21:26Z welker $
 * @category.name external execution
 * @category.name local
 */

public class CommandLine extends TypedAtomicActor {

	/**
	 * Construct a CommandLine actor with the given container and name.
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
	public CommandLine(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());

		// Construct parameters.
		command = new PortParameter(this, "command");

		// command.setExpression("Please type your command here...");
		outputFile = new FileParameter(this, "outputFile");
		// Construct input ports.
		arguments = new TypedIOPort(this, "arguments", true, false);
		arguments.setMultiport(true);
		infileHandle = new TypedIOPort(this, "infileHandle", true, false);
		infileHandle.setTypeEquals(BaseType.STRING);
		trigger = new TypedIOPort(this, "trigger", true, false);
		new Attribute(arguments, "_showName");
		new Attribute(infileHandle, "_showName");
		new Attribute(command, "_showName");
		// Attribute hide = new SingletonAttribute(trigger, "_hide");
		// hide.setPersistent(false);
		hide = new SingletonParameter(trigger, "_hide"); // DFH
		hide.setToken(BooleanToken.TRUE); // DFH
		// Construct output ports.
		outfileHandle = new TypedIOPort(this, "outfileHandle", false, true);
		outfileHandle.setTypeEquals(BaseType.STRING);
		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);
		exitCode = new TypedIOPort(this, "exitCode", false, true);
		exitCode.setTypeEquals(BaseType.BOOLEAN);
		new Attribute(output, "_showName");
		new Attribute(outfileHandle, "_showName");
		new Attribute(exitCode, "_showName");

		// Set the trigger Flag.
		outputLineByLine = new Parameter(this, "outputLineByLine",
				new BooleanToken(false));
		outputLineByLine.setTypeEquals(BaseType.BOOLEAN);

		hasTrigger = new Parameter(this, "hasTrigger", new BooleanToken(false));
		hasTrigger.setTypeEquals(BaseType.BOOLEAN);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"20\" y=\"25\" "
				+ "style=\"font-size:30; fill:blue; font-family:SansSerif\">"
				+ "$</text>\n" + "</svg>\n");

	} // constructor

	// //////////////// Public ports and parameters ///////////////////////

	public SingletonParameter hide;
	/**
	 * @entity.description The command to execute. <BR>
	 *                     <I>FIX ME: </I>The style of the command will be noted
	 *                     here.
	 */
	public PortParameter command;

	/**
	 * @entity.description Needs to be filled in if the user wants the command to
	 *                     output to a file.
	 */
	public FileParameter outputFile;

	/**
	 * @entity.description The arguments to the command. Implemented as a
	 *                     multi/input port to support more than one argument.
	 *                     It concatanates the inputs in all the channels. <BR>
	 *                     <I>If there is an input file, this port can be
	 *                     empty.</I>
	 */
	public TypedIOPort arguments;

	/**
	 * @entity.description This is an optional port. Used if the file accepts an
	 *                     input file instead of a list of arguments.
	 */
	public TypedIOPort infileHandle;

	/**
	 * @entity.description The trigger port. The type of this port is undeclared,
	 *                     meaning that it will resolve to any data type.
	 */
	public TypedIOPort trigger;

	/**
	 * @entity.description A string that forwards the outputFile parameter if it
	 *                     exists.
	 */
	public TypedIOPort outfileHandle;
	/**
	 * @entity.description The result stream of the command.
	 */
	public TypedIOPort output;

	/**
	 * @entity.description Exit code will be 1 if the command executes
	 *                     successfully.
	 */
	public TypedIOPort exitCode;

	/**
	 * @entity.description If selected, broadcasts the output of the command
	 *                     line by line.
	 */
	public Parameter outputLineByLine;

	/**
	 * @entity.description Unhide the trigger port when this parameter is true.
	 *                     This Parameter is type of boolean. <BR>
	 *                     <b>NOTE: </b>in fact, user can use the port
	 *                     configuration window to hide or unhide a port. This
	 *                     paremeter is here to provide a more intuitive
	 *                     interface for this actor.
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
				try {
					trigger.setContainer(this);
					// new Attribute(trigger, "_showName");
					hide.setToken(BooleanToken.FALSE); // DFH
				} catch (NameDuplicationException ndex) {
					_debug("111: " + ndex.getMessage());
				}
			} else {
				List inPortList = this.inputPortList();
				Iterator ports = inPortList.iterator();
				while (ports.hasNext()) {
					IOPort p = (IOPort) ports.next();
					if (p.isInput()) {
						try {
							if (p.getName().equals("trigger")) {
								// new Attribute(trigger, "_hideName"); //DFH
								// p.setContainer(null); //DFH
								hide.setToken(BooleanToken.TRUE); // DFH
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
	 * ... Send the exitCode and outputFileHandle(optional) to the result port.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		_lineFlag = ((BooleanToken) outputLineByLine.getToken()).booleanValue();
		_debug("<TRIGGER_FLAG>" + _lineFlag + "</TRIGGER_FLAG>");

		// Get the main command from the command parameter.
		// _commandStr = ((StringToken)command.getToken()).stringValue();
		command.update();
		String _commandStr = ((StringToken) command.getToken()).stringValue();

		// simply consume the trigger token if there is some.
		if (_triggerFlag) {
			if (trigger.getWidth() > 0) {
				if (trigger.hasToken(0)) {
					trigger.get(0);
					_debug("consume the token at the trigger port.");
				}
			}
		}

		/*
		 * Consume the input file token if there's one.
		 */
		Token tokenFile = null;
		String value = null;
		try {
			tokenFile = infileHandle.get(0);
			if (tokenFile != null) {
				value = new String(tokenFile.toString());
				_debug("infileHandle(i) = " + value);
				value = value.substring(1, value.length() - 1);
				_commandStr += " < " + value;
			}
		} catch (Exception ex) {
			_debug("Input file is null.");
		}

		/*
		 * The arguments can only be accepted if there's no input file. So the
		 * "value" of the infile handle is checked here and arguments are
		 * consumed only if it is null.
		 */
		// if (value == null) {
		/*
		 * Create the argument string. Consume data in all the channels an
		 * combine them.
		 */
		String argString = "";
		value = new String("");
		int i = 0;
		int width = arguments.getWidth();
		for (i = 0; i < width; i++) {
			if (arguments.hasToken(i)) {
				Token tokenArg = arguments.get(i);
				value = tokenArg.toString();
				_debug("arguments(i) = " + value);
				value = value.substring(1, value.length() - 1);

				while (value.indexOf("\\\"") != -1) {
					int ind = value.indexOf("\\\"");
					value = value.substring(0, ind)
							+ value.substring(ind + 1, value.length());
					_debug(value);
				}
				argString += value + " ";
				_debug("argString = " + argString);
			}
		}
		_commandStr += " " + argString;
		// }

		int commandCount = getSystemProps();

		/*
		 * Get the output file path if there's one and add it to the command
		 * string.
		 */
		if (outputFile.asURL() == null) {
			_debug("Output file is null.");
		} else {
			String outFilePath = outputFile.asURL().toString();
			if (outFilePath.startsWith("file:///")) {
				if (_charsToSkip == 6) {
					_charsToSkip = 8;
				} else if (_charsToSkip == 5) {
					_charsToSkip = 7;
				}
			}
			outFilePath = outputFile.asURL().toString().substring(_charsToSkip);
			_commandStr += " > " + outFilePath;
			outfileHandle.broadcast(new StringToken(outFilePath));
		}

		_commandArr[commandCount] = _commandStr;
		_debug("<COMMAND>" + _commandArr[commandCount] + "</COMMAND>");
		// EXECUTION OF THE GENERATED COMMAND.
		_debug("Executing the command...");
		try {

			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(_commandArr);

			DataInputStream inStream = new DataInputStream(proc
					.getInputStream());
			String result; // Temp for each line of output.
			StringBuffer outBuff = new StringBuffer("");
			try {
				while ((result = inStream.readLine()) != null) {
					_debug(result);
					if (_lineFlag) {
						output.broadcast(new StringToken(result.toString()));
					} else {
						outBuff.append(result + "\n");
					}
				}
			} catch (IOException ioe) {
				_debug("<IOException> when reading the input: " + ioe
						+ "</IOException>");
			}
			if (!_lineFlag) {
				output.broadcast(new StringToken(outBuff.toString()));
			}
			// any error?
			int exitVal = proc.waitFor();
			_debug("ExitValue: " + exitVal);
			// Broadcast the exit status.
			if (exitVal == 0) {
				exitCode.broadcast(new BooleanToken(true));
			} else {
				exitCode.broadcast(new BooleanToken(false));
			}
		} catch (Exception ex) {
			_debug("<EXCEPTION> An exception occured when executing the command. "
					+ " \n\t Exception: " + ex + "\n</EXCEPTION>");
		}
	} // end-of-fire

	public int getSystemProps() {
		// Get OS name
		String osName = System.getProperty("os.name");
		_debug("<OS>" + osName + "</OS>");
		if (osName.equals("Windows NT") || osName.equals("Windows XP")
				|| osName.equals("Windows 2000")) {
			_commandArr[0] = "cmd.exe";
			_commandArr[1] = "/C";
			_charsToSkip = 6;
			return 2;
		} else if (osName.equals("Windows 95")) {
			_commandArr[0] = "command.com";
			_commandArr[1] = "/C";
			_charsToSkip = 6;
			return 2;
		}
		/*
		 * else if (osName.equals("Linux")) { _commandArr[0] = "/bin/sh";
		 * _commandArr[1] = "-c"; _charsToSkip = 5; return 2; } else if
		 * (osName.equals("Mac OS X")) { _commandArr[0] = "/bin/sh";
		 * _commandArr[1] = "-c"; _charsToSkip = 5; return 2; }
		 */
		else {
			_commandArr[0] = "/bin/sh";
			_commandArr[1] = "-c";
			_charsToSkip = 5;
			return 2;
			/* return 0; */
		}
	} // end-of-getSystemProps

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////

	// The combined command to execute.
	private String _commandStr = "";
	private String _commandArr[] = new String[3];
	private boolean _lineFlag = false;
	private boolean _triggerFlag = false;
	private int _charsToSkip = 6;

} // end-of-class-CommandLine