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

package org.dart.matlab;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.style.TextStyle;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// MatlabExpression
/**
 * <p>
 * Allows the user to input a matlab script to be executed when the actor fires.
 * The following actor takes into consideration differnt startup routines for
 * MatlabSoftware for Unix/Windows os.
 * </p>
 * <p>
 * Input ports can be added and are automatically loaded into variables in
 * matlab which can be referenced by the port name.
 * </p>
 * <p>
 * Similarly output can be made by adding output ports to the actor. The output
 * values are taken from variables with the same names as the output ports.
 * </p>
 * <p>
 * <B>NOTE</B>: windows is a bit more tempermental than unix system. the EXE
 * file must be directly pointed to by the mlCmd property. E.g
 * c:\\matlab7\\bin\\win32\\matlab.exe
 * </p>
 * <p>
 * Also, windows command line matlab doesn't use the standard in and out,
 * instead it uses it's own command window, which makes it impossible to read
 * and write to the matlab process using the process input and output streams.
 * So instead the actor writes the data to a file and read in the outputs from
 * the file. The file is created with a random integer at the end of the file
 * name to (in theory) allow multiple matlab actors to run at the same time. The
 * file is deleted once it's been read.
 * </p>
 * <p>
 * <B>TODO</B>: currently this actor only works with standard single value and
 * array results. support for all forms of matlab output needs to be implemented
 * </p>
 * <p>
 * <B>NOTE</B>: now with java 1.4 complience!! using the ProcessBuilder in java
 * 1.5 makes things a lot easier but since we need to compile under 1.4 here it
 * is.
 * </p>
 * <p>
 * <B>Changelog 27/04/06</B>: * check for existance of executable under windows
 * * kill matlab process when stop() is called * check to make sure variable for
 * output port name exists if it doesn't, then set it to 0 * changed expression
 * to PortParameter * changed error handling: sends error message to output port
 * for some things
 * </p>
 * 
 * @author Tristan King, Nandita Mangal
 * @version $Id: MatlabExpression.java 24234 2010-05-06 05:21:26Z welker $
 * 
 */
public class MatlabExpression extends TypedAtomicActor {

	public MatlabExpression(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		expression = new PortParameter(this, "expression");
		// make a text style, so that the parameter has multiple lines
		new TextStyle(expression, "Matlab Expression");
		// set the default script
		expression.setExpression("Enter Matlab function or script here...");
		// set to string mode, so the parameter doesn't have to have surrounding
		// ""s
		expression.setStringMode(true);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);

		mlCmd = new StringParameter(this, "mlCmd");
		mlCmd.setDisplayName("Matlab Executable");
		// set default for specific os such as
		// "c:\\matlab7\\bin\\win32\\matlab.exe"
		mlCmd.setExpression("matlab");

		triggerSwitch = new TypedIOPort(this, "triggerSwitch", true, false);

	}

	// /////////////////////////////////////////////////////////////////
	// // logging variables ////

	private static final Log log = LogFactory
			.getLog("org.dart.matlab.MatlabExpression");
	// private static final boolean isDebugging = log.isDebugEnabled();

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The output port. Outputs the matlab results.
	 */
	public TypedIOPort output;

	/**
	 * The expression that is evaluated : Matlab Function or Script from the
	 * parameter dialog box or input port.
	 */
	public PortParameter expression;

	/**
	 * Path to matlab execuatble. defaults to "matlab"
	 */
	public StringParameter mlCmd;

	/**
	 * The trigger switch ,whether to execute the actor or not. (True or False /
	 * 0 or 1(or more) as input enable the switch)
	 */
	public TypedIOPort triggerSwitch;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	public synchronized void fire() throws IllegalActionException {
		super.fire();
		boolean fireSwitch = true;

		if (triggerSwitch.getWidth() > 0) {
			Object inputSwitch = triggerSwitch.get(0);
			if (inputSwitch instanceof IntToken) {
				if (((IntToken) inputSwitch).intValue() >= 1) {
					fireSwitch = true;
				} else {
					fireSwitch = false;
				}

			} else if (inputSwitch instanceof BooleanToken) {

				if (((BooleanToken) inputSwitch).booleanValue()) {
					fireSwitch = true;
				} else {
					fireSwitch = false;
				}
			}

		}
		// if switch is set, then we execute actor
		// else do nothing.
		if (fireSwitch) {

			// get the script commands needed to pass into -r command
			String script = buildScript();

			String outputString = "";
			String randomFilename = tempFilename + "."
					+ Math.abs((new Random()).nextInt());
			boolean isWindows = (System.getProperty("os.name").indexOf(
					"Windows") > -1);

			// build the command array

			if (isWindows) {
				File f = new File(mlCmd.getExpression());
				if (!f.exists()) {
					throw new IllegalActionException("Matlab process "
							+ mlCmd.getExpression() + " doesn't exist");
				}
			}
			// TODO: same as above under unix

			// List argList = new ArrayList();
			String[] argList;

			// for some reason windows matlab doesn't work when supplied with
			// an array of arguments, and linux matlab doesn't work when
			// supplied
			// a single string with all the arguments.......
			if (isWindows) {
				argList = new String[1];
				argList[0] = mlCmd.getExpression() + " -r" + " \"" + script
						+ "\"" + " -nodesktop" + " -nosplash" + " -logfile "
						+ randomFilename;
			} else {
				argList = new String[4];

				argList[0] = mlCmd.getExpression();
				argList[1] = "-nodesktop";
				argList[2] = "-nosplash" + " ";
				argList[3] = "-r" + " \"" + script + "\"";
			}

			// run the process and get output
			try {
				// Process p;
				if (isWindows) {

					_p = Runtime.getRuntime().exec(argList[0]);
				} else {
					_p = Runtime.getRuntime().exec(argList);
				}

				BufferedInputStream inputstream = null;
				int result = -1;

				// if we are running under windows
				if (isWindows) {
					// wait for the process to end
					result = _p.waitFor();
					if (result == 0) {
						// create the input stream from the log file created by
						// matlab
						inputstream = new BufferedInputStream(
								new FileInputStream(randomFilename));
					}

					// if not running under windows
				} else {
					// get the processes input stream
					inputstream = new BufferedInputStream(_p.getInputStream());
				}

				// read the stream until there is nothing left to read
				if (inputstream != null) {
					while (true) {
						int in;
						try {
							in = inputstream.read();
						} catch (NullPointerException e) {
							in = -1;
						}
						if (in == -1) {
							break;
						} else {
							outputString += (char) in;
						}
					}

					// close the input stream
					inputstream.close();
				}

				if (isWindows) {
					// delete the temp file. if it doesn't exist, then it will
					// just return false
					(new File(randomFilename)).delete();
				} else {
					// make sure matlab exited OK.
					result = _p.waitFor();
				}

				switch (result) {
				case 0:
					break;
				case -113:
					output.send(0, new StringToken(
							"Matlab process was forcfully killed"));
					return;
				case 129:
					output.send(0, new StringToken(
							"Matlab process was forcfully killed"));
					return;
				default:
					output.send(0, new StringToken("Matlab process returned \""
							+ result + "\".\nSomething must have gone wrong"));
					return;
				}

			} catch (IOException e) {
				log.error("IOException: " + e.getMessage());
			} catch (InterruptedException e) {
				log.debug("interupted!");
			}

			// process the output from matlab
			parseOutput(outputString);
		}

	}

	public void stop() {
		if (_p != null) {
			_p.destroy();
		}
	}

	/**
	 * builds the script for matlab to run
	 * 
	 * @return commands to run under matlab
	 * @throws IllegalActionException
	 */
	private String buildScript() throws IllegalActionException {
		List ipList = inputPortList();
		Iterator ipListIt = ipList.iterator();
		String inputs = "";

		while (ipListIt.hasNext()) {
			TypedIOPort tiop = (TypedIOPort) ipListIt.next();

			// if the input port is not the script itself.
			if (!(tiop.getName().equals("expression"))
					&& !(tiop.getName().equals("triggerSwitch"))) {
				// get the token waiting on the port
				Token[] token = new Token[1];

				// TODO: check to make sure token exists
				token[0] = tiop.get(0);

				// setup the variable assignment
				// looks like: port_name = [ token_values ]
				inputs += tiop.getName() + " = ";
				inputs += "[" + getTokenValue(token) + "]\n";
			}
		}

		List opList = outputPortList();
		Iterator opListIt = opList.iterator();
		String outputs = "";

		while (opListIt.hasNext()) {
			TypedIOPort tiop = (TypedIOPort) opListIt.next();

			// make sure the output port found isn't output
			if (!tiop.equals(output)) {
				// add it to the list
				outputs += "if exist('" + tiop.getName() + "'),"
						+ tiop.getName() + ",else," + tiop.getName()
						+ "=0,end\n";
			}

		}

		// if there is a token waiting on the port input, grab it
		expression.update();

		String script = inputs + "sprintf('----')\n"
				+ ((StringToken) expression.getToken()).stringValue() + "\n"
				+ "sprintf('----')\n" + outputs + "quit\n";

		// should probably do this inside the code rather than here, but here
		// makes it easier to change later on.
		script = script.replaceAll("\n", ",");

		return script;
	}

	/**
	 * parses the output from matlab to grab the values for the output ports
	 * 
	 * @param outputString
	 * @throws IllegalActionException
	 */
	private void parseOutput(String outputString) throws IllegalActionException {

		// this is a special token put in to make it easy to find the results
		// for ports
		final String scriptDivider = "ans =\n\n----";

		// process outputString
		// windows matlab writes '\r' characters along with the '\n' character.
		// remove them so the parsing script works properly.
		outputString = outputString.replaceAll("\r", "");

		// ensure indexof will work
		if (outputString.indexOf(scriptDivider) < 0) {
			throw new IllegalActionException(
					"Error parsing output: Matlab must not have fired");
		}

		// cut off the proceeding matlab hello message
		String outs = outputString.substring(outputString
				.indexOf(scriptDivider)
				+ scriptDivider.length(), outputString.length());

		String outputSendString = "";

		// send only the user entered results to the output port
		if (outs.indexOf(scriptDivider) >= 0) {
			outputSendString = outs.substring(0, outs.indexOf(scriptDivider));
		}
		output.send(0, new StringToken(outputSendString));

		// get all the results which are to be sent as tokens out of a specified
		// port
		String results = outs.substring(outs.indexOf(scriptDivider)
				+ scriptDivider.length(), outs.length());

		// string tokenizer doesn't seem to beable to use '\n's as seperators
		// so to make tokenization easy, replace the combinations of '\n's with
		// a unique character.
		results = results.replaceAll("\n\n\n", "*");
		results = results.replaceAll("\n\n", "");

		// break the string up into seperate sections for each port result
		StringTokenizer st = new StringTokenizer(results, "*");
		while (st.hasMoreTokens()) {

			// break each result up into tokens to make port name and value easy
			// to extract
			String ssst = st.nextToken();

			StringTokenizer ist = new StringTokenizer(ssst);
			if (ist.countTokens() > 2) {
				String portName = ist.nextToken();
				String fss = ist.nextToken();
				if (fss.equals("Undefined") || !fss.equals("=")) {
					// port is undefined, or something else has gone wrong
					// TODO: should a nil token be passed?
					// System.out.println("2nd token is \"" + fss + "\"");
				} else {
					// we are good to continue
					String[] value = new String[ist.countTokens()];
					int count = 0;
					while (ist.hasMoreTokens()) {
						value[count++] = ist.nextToken();
					}

					// set the value of the output port
					setOutputToken(portName, value);
				}
			}
		}
	}

	/**
	 * recursive function to build a value for assignment to a matlab variable
	 * from an input token.
	 * 
	 * recursively dives into arraytokens, surrounding each array with [ ]
	 * brackets to conform with matlab array inputs
	 * 
	 * TODO: extend for more input types, e.g. MatrixTokens
	 * 
	 * @param token
	 * 	 * @throws IllegalActionException
	 */
	private String getTokenValue(Token[] token) throws IllegalActionException {

		String returnval = "";

		for (int i = 0; i < token.length; i++) {
			if (token[i].getType()
					.isCompatible(new ArrayType(BaseType.UNKNOWN))) {
				returnval += " [ "
						+ getTokenValue(((ArrayToken) token[i]).arrayValue())
						+ " ] ";
			} else if (token[i].getType().equals(BaseType.STRING)) {
				returnval += " '" + ((StringToken) token[i]).stringValue()
						+ "' ";
			} else if (token[i].getType().equals(BaseType.INT)) {
				returnval += " " + ((IntToken) token[i]).intValue() + " ";
			} else if (token[i].getType().equals(BaseType.DOUBLE)) {
				returnval += " " + ((DoubleToken) token[i]).doubleValue() + " ";
			} else if (token[i].getType().equals(BaseType.BOOLEAN)) {
				returnval += " " + ((BooleanToken) token[i]).toString() + " ";
			} else if (token[i].getType().equals(BaseType.LONG)) {
				returnval += " " + ((LongToken) token[i]).longValue() + " ";
			} else if (token[i].getType().equals(BaseType.UNSIGNED_BYTE)) {
				returnval += " " + ((UnsignedByteToken) token[i]).byteValue()
						+ " ";
			} else {
				throw new IllegalActionException("invalid token type: "
						+ token[i].getType().toString());
			}
		}

		return returnval;
	}

	/**
	 * sends a value out a specified port.
	 * 
	 * tries to figure out what data type the value is.
	 * 
	 * 
	 * @param portName
	 * @param value
	 * @throws IllegalActionException
	 */
	private void setOutputToken(String portName, String[] value)
			throws IllegalActionException {
		List opList = outputPortList();
		Iterator opListIt = opList.iterator();

		// make sure the portName isn't output
		if (portName.equals(output.getName())) {
			throw new IllegalActionException(
					"sending a custom token out of port " + output.getName()
							+ " is bad!");
		}

		// iterate through the list of ports
		while (opListIt.hasNext()) {
			TypedIOPort tiop = (TypedIOPort) opListIt.next();
			String thisPortName = tiop.getName();
			// check if the name is the same as the one we want to set
			if (thisPortName.equals(portName)) {

				// check the type of the array
				// type 2 = string > 1 = double > 0 = int > -1 = no value
				int type = -1;
				for (int i = 0; i < value.length; i++) {
					try {
						if (value[i].indexOf(".") > -1) {
							// check if it can be converted to a double
							Double.valueOf(value[i]);
							// if the current type is an int then we can change
							// the whole
							// array type to double, but if the current type is
							// a string
							// then we have to keep it as a string
							type = type > 1 ? type : 1;
						} else {
							// check if it can be converted to an int
							Integer.valueOf(value[i]);
							type = type > 0 ? type : 0;
						}
					} catch (NumberFormatException e) {
						// it has to be a string
						type = type > 2 ? type : 2;
					}
				}

				// build the array using the specified type
				Token[] token;
				if (type == 2) {
					token = new StringToken[value.length];
					for (int i = 0; i < token.length; i++) {
						token[i] = new StringToken(value[i]);
					}
					if (value.length > 1) {
						tiop.setTypeEquals(new ArrayType(BaseType.STRING));
					} else {
						tiop.setTypeEquals(BaseType.STRING);
					}
				} else if (type == 1) {
					token = new DoubleToken[value.length];
					for (int i = 0; i < token.length; i++) {
						token[i] = new DoubleToken(Double.valueOf(value[i])
								.doubleValue());
					}
					if (value.length > 1) {
						tiop.setTypeEquals(new ArrayType(BaseType.DOUBLE));
					} else {
						tiop.setTypeEquals(BaseType.DOUBLE);
					}
				} else if (type == 0) {
					token = new IntToken[value.length];
					for (int i = 0; i < token.length; i++) {
						token[i] = new IntToken(Integer.valueOf(value[i])
								.intValue());
					}
					if (value.length > 1) {
						tiop.setTypeEquals(new ArrayType(BaseType.INT));
					} else {
						tiop.setTypeEquals(BaseType.INT);
					}
				} else {
					// throw an error if something went wrong
					throw new IllegalActionException(
							"invalid value passed for token");
				}

				// send the array over the output port
				if (token.length > 1) {
					tiop.send(0, new ArrayToken(token));
				} else {
					tiop.send(0, token, token.length);
				}

				break;
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	private final static String tempFilename = "matlab_results";
	private Process _p;
}