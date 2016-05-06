/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
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

package org.resurgence.actor;

// Ptolemy packages
import java.io.DataInputStream;
import java.io.IOException;

import org.sdm.spa.CommandLine;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

//////////////////////////////////////////////////////////////////////////
//// StatusChecker
/**
 * FIXME: Add documents here. FIXME: Make it more generic... FIXME: If we
 * decided to extend from CommandLine, then we should re-code CommandLine for
 * code reuse.
 * 
 * @author Yang Zhao
 * @author Ilkay Altintas
 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: StatusChecker.java 24234 2010-05-06 05:21:26Z welker $
 */

public class StatusChecker extends CommandLine {

	/**
	 * Construct a StatusChecker actor with the given container and name.
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
	public StatusChecker(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		iterationOutput = new TypedIOPort(this, "iterationOutput", false, true);
		iterationOutput.setTypeEquals(BaseType.STRING);
		new Attribute(iterationOutput, "_showName");

		// Hide the outputLineByLine parameter.
		outputLineByLine.setVisibility(Settable.EXPERT);

		sleepTime = new Parameter(this, "sleepTime", new LongToken(0));
		sleepTime.setTypeEquals(BaseType.LONG);

		checkCondition = new StringParameter(this, "checkCondition");
		checkCondition.setTypeEquals(BaseType.STRING);

		maxChecks = new Parameter(this, "maxChecks", new IntToken(1));
		maxChecks.setTypeEquals(BaseType.INT);
	}

	// /////////////////////////////////////////////////////////////////
	// // Public ports and parameters ////

	/**
	 * The output in each iteration.
	 */
	public TypedIOPort iterationOutput;

	/**
	 * The sleep time amount, in milliseconds, between two checks. This
	 * parameter must contain a LongToken. The default value of this parameter
	 * is 0, meaning that this actor will not sleep between checks.
	 */
	public Parameter sleepTime;

	/**
	 * A regular expression for which to check in the output.
	 */
	public StringParameter checkCondition;

	/**
	 * The max amount of checks. This parameter is type of int. The default
	 * value of this parameter is -1, meaning that this actor will keep on
	 * checking until the condition is satisfied.
	 */
	public Parameter maxChecks;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * If the specified attribute is <i>sleepTime</i> or <i>maxChecks</i>, then
	 * get the value of them.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		// FIXME: should check whether the value is valid...
		if (attribute == sleepTime) {
			_sleepTime = ((LongToken) sleepTime.getToken()).longValue();
		} else if (attribute == maxChecks) {
			_maxChecks = ((IntToken) maxChecks.getToken()).intValue();
			if (_maxChecks < 0)
				_maxChecks = Integer.MAX_VALUE;
		} else if (attribute == checkCondition) {
			_condition = checkCondition.stringValue();
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
		// simply consume the trigger token if there is some.
		if (trigger.getWidth() > 0) {
			if (trigger.hasToken(0)) {
				trigger.get(0);
				_debug("consume the tokne at the trigger port.");
			}
		}

		// Get the main command from the command parameter.
		command.update();
		_commandStr = ((StringToken) command.getToken()).stringValue();
		_createCommand();

		int i = 0;
		boolean isSatisfied = false;
		String result = " ";

		while (!isSatisfied && i < _maxChecks) {
			result = _executeCommand();
			_debug("the execution result is : " + result);
			if (result.matches(_condition))
				isSatisfied = true;
			if (!_lineFlag)
				iterationOutput.broadcast(new StringToken(result));
			i++;
			try {
				if (_debugging)
					_debug(getName() + ": Wait for " + _sleepTime
							+ " milliseconds.");
				Thread.sleep(_sleepTime);
			} catch (InterruptedException e) {
				// Ignore...
			}
		}

		if (!_lineFlag)
			output.broadcast(new StringToken(result));
		if (isSatisfied) {
			exitCode.broadcast(new BooleanToken(true));
		} else {
			exitCode.broadcast(new BooleanToken(false));
		}
	} // end-of-fire

	// FIXME: The reason I keep this method here is because the redefined
	// private variable from the CommandLine actor. This should be changed...
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
		} else if (osName.equals("Linux")) {
			_commandArr[0] = "/bin/sh";
			_commandArr[1] = "-c";
			_charsToSkip = 5;
			return 2;
		} else if (osName.equals("Mac OS X")) {
			_commandArr[0] = "/bin/sh";
			_commandArr[1] = "-c";
			_charsToSkip = 5;
			return 2;
		} else {
			return 0;
		}
	} // end-of-getSystemProps

	private String _executeCommand() throws IllegalActionException {
		int commandCount = getSystemProps();
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
				return outBuff.toString();
			} catch (IOException ioe) {
				throw new IllegalActionException(this,
						"<IOException> when reading the input: " + ioe
								+ "</IOException>");
			}
		} catch (Exception ex) {
			throw new IllegalActionException(this,
					"An exception occured when executing the command: " + ex);
		}
	}

	private void _createCommand() throws IllegalActionException {
		/*
		 * Consume the input file token if there's one.
		 */
		Token tokenFile = null;
		String value = null;
		try {
			if (infileHandle.getWidth() > 0) {
				if (infileHandle.hasToken(0)) {
					tokenFile = infileHandle.get(0);
					_debug("consume the tokne at the trigger port.");
				}
			}

			if (tokenFile != null) {
				value = new String(tokenFile.toString());
				_debug("infileHandle(i) = " + value);
				value = value.substring(1, value.length() - 1);
				_commandStr += " < " + value;
			}
		} catch (Exception ex) {
			throw new IllegalActionException(this, "Input file is null.");
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
	}

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////
	// The combined command to execute.
	private String _commandStr = "";
	private String _commandArr[] = new String[3];
	private boolean _lineFlag = false;
	private boolean _triggerFlag = false;
	private int _charsToSkip = 6;
	private long _sleepTime = 0;
	private int _maxChecks = Integer.MAX_VALUE;
	private String _condition = ".*";

} // end-of-class-StatusChecker