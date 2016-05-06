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

package org.cipres.kepler;

// ptolemy packages
import java.util.Observable;
import java.util.Observer;

import javax.swing.JOptionPane;

import org.cipres.helpers.GUIRun;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.Manager;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;

//////////////////////////////////////////////////////////////////////////
//// GUIRunCIPRes
/**
 * This actor calls external Cipres applications with GUIGen interfaces.
 * 
 * The GUIRunCIPRes actor provides a template to invoke external Cipres
 * applications. A set of parameters is configured in the actor to collect
 * application related information. The value of these parameters will be fed to
 * the GUIRun object when the actor fires. Thus the GUIRun object will invoke
 * the corresponding application with those values, like the input/output file
 * names and the working directory. After the execution, if the external program
 * returns correctly, the standard output of the execution will be sent to the
 * standard output file, the standard error will be sent to the standard error
 * file, and the program output file name will be sent to the
 * outputParameterValue port.
 * 
 * The user can also set some words to be monitored in the standard output
 * stream. The observer/observable design pattern is used here. The GUIRunCIPRes
 * object is an observer. The GUIRun object is an observable. Whenever the
 * GUIRun object find any of these monitored words in the standard output steam
 * of the program execution, it will notify the GUIRunCIPRes objects to promote
 * the user handling the issue. Usually the GUIRunCIPRes actor will stop the
 * execution of the program.
 * 
 * @author Zhijie Guan
 * @version $Id: GUIRunCIPRes.java 24234 2010-05-06 05:21:26Z welker $
 */

public class GUIRunCIPRes extends TypedAtomicActor implements Observer {

	/**
	 * Construct GUIRunCIPRes source with the given container and name.
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

	public GUIRunCIPRes(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// initialize the standard output port
		standardOutput = new TypedIOPort(this, "standardOutput", false, true);
		standardOutput.setDisplayName("Standard Output");
		standardOutput.setTypeEquals(BaseType.STRING);

		// initialize the standard error port
		standardError = new TypedIOPort(this, "standardError", false, true);
		standardError.setDisplayName("Standard Error");
		standardError.setTypeEquals(BaseType.STRING);

		// initialize the exit code port
		exitCode = new TypedIOPort(this, "exitCode", false, true);
		exitCode.setDisplayName("Exit Code");
		exitCode.setTypeEquals(BaseType.STRING);

		// parameters initialization
		// output parameter value
		outputParameterValue = new TypedIOPort(this, "outputParameterValue",
				false, true);
		outputParameterValue.setDisplayName("Parameter Value (Output)");
		outputParameterValue.setTypeEquals(BaseType.STRING);

		// input parameter name
		inputParameterName = new TypedIOPort(this, "inputParameterName", true,
				false);
		inputParameterName.setDisplayName("Input Parameter Name");
		inputParameterName.setTypeEquals(BaseType.STRING);

		// input parameter value
		inputParameterValue = new TypedIOPort(this, "inputParameterValue",
				true, false);
		inputParameterValue.setDisplayName("Input Parameter Value");
		inputParameterValue.setTypeEquals(BaseType.STRING);

		// command name
		command = new FileParameter(this, "command");
		command.setDisplayName("External Command");

		// GUIGen xml file name
		uiXMLFile = new FileParameter(this, "uiXMLFile");
		uiXMLFile.setDisplayName("GUIGen XML File");

		// Standard output file
		outputFile = new FileParameter(this, "outputFile");
		outputFile.setDisplayName("Standard Output File");

		// Standard error file
		errorFile = new FileParameter(this, "errorFile");
		errorFile.setDisplayName("Standard Error File");

		// Working directory
		workingDirectory = new Parameter(this, "workingDirectory",
				new StringToken(""));
		workingDirectory.setDisplayName("Working Direcotry");

		// parameter's name in the GUIRun. Usually it is "output"
		parameterForOutput = new Parameter(this, "parameterForOutput",
				new StringToken(""));
		parameterForOutput.setDisplayName("Parameter to Output");

		// words to be monitored
		monitoredErrorWords = new Parameter(this, "monitoredErrorWords",
				new StringToken(""));
		monitoredErrorWords.setDisplayName("Error Words to be Monitored");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The command path and name are defined in this parameter.
	 */
	public FileParameter command;

	/**
	 * The GUIGen xml file's path and name are defined in this parameter.
	 */
	public FileParameter uiXMLFile;

	/**
	 * The standard output file's path and name are defined in this parameter.
	 */
	public FileParameter outputFile;

	/**
	 * The standard error file's path and name are defined in this parameter.
	 */
	public FileParameter errorFile;

	/**
	 * The workfing directory of the external program is defined in this
	 * parameter.
	 */
	public Parameter workingDirectory;

	/**
	 * The GUIRun parameter to be send out is defined in this parameter. Usually
	 * this GUIRun parameter is "outfile".
	 */
	public Parameter parameterForOutput;

	/**
	 * The monitored words are defined in this parameter.
	 */
	public Parameter monitoredErrorWords;

	/**
	 * The standard output stream of the execution is sent out through this
	 * port.
	 */
	public TypedIOPort standardOutput = null;

	/**
	 * The standard error stream of the execution is sent out through this port.
	 */
	public TypedIOPort standardError = null;

	/**
	 * The exit code of the execution is sent out through this port.
	 */
	public TypedIOPort exitCode = null;

	/**
	 * The output parameter value, usually the name of the output file of the
	 * execution, is sent out throught this port.
	 */
	public TypedIOPort outputParameterValue = null;

	/**
	 * The input parameter name (usually "infile") is sent through this port.
	 */
	public TypedIOPort inputParameterName = null;

	/**
	 * The input parameter value (usually the name of the input file of the
	 * execution) is sent through this port.
	 */
	public TypedIOPort inputParameterValue = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Invoke the external program by setting the parameters of the execution
	 * using GUIGen. The monitored words are being monitored during the
	 * execution.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		if (inputParameterValue.hasToken(0) && inputParameterName.hasToken(0)) {
			super.fire();
			try {

				// get command
				String commandFileName = ((StringToken) command.getToken())
						.stringValue();
				if (commandFileName.length() != 0) {
					_grun.setCommand(commandFileName);
				} else {
					System.out.println("Command is not defined!");
					throw new IllegalActionException("Command is not defined");
				}

				// get GUIGen xml file
				String uiXMLFileName = ((StringToken) uiXMLFile.getToken())
						.stringValue();
				if (uiXMLFileName.length() != 0) {
					_grun.setUIXMLFile(uiXMLFileName);
				}

				// get standard output file name
				String outFileName = ((StringToken) outputFile.getToken())
						.stringValue();
				if (outFileName.length() != 0) {
					_grun.setOutputFileName(outFileName);
				}

				// get standard error file name
				String errorFileName = ((StringToken) errorFile.getToken())
						.stringValue();
				if (errorFileName.length() != 0) {
					_grun.setErrorFileName(errorFileName);
				}

				// get the working directory
				String workingDirName = ((StringToken) workingDirectory
						.getToken()).stringValue();
				if (workingDirName.length() != 0) {
					_grun.setWorkingDirectory(workingDirName);
				}

				// set the input file to the GUIRun object
				_grun
						.setArgumentValue(((StringToken) inputParameterName
								.get(0)).stringValue(),
								((StringToken) inputParameterValue.get(0))
										.stringValue());

				// get arguments information with GUIGen interface
				_grun.setArgumentsWithGUIGen();

				// Here we assume all the programs executed by GUIRunCIPRes must
				// be finished to get the final results
				// So the GUIRun is set to wait until the execution is finished
				_grun.setWaitForExecution(true);

				// check if the user set the monitored error words
				String errorWords = ((StringToken) monitoredErrorWords
						.getToken()).stringValue();
				if (errorWords.length() != 0) {
					_grun.setMonitoredErrorWords(errorWords);
					_grun.addObserver(this); // add this GUIRunCIPRes as an
												// observer
				}

				_grun.execute(); // invoke the external program

				String outputFileName = _grun
						.getArgumentValue(((StringToken) parameterForOutput
								.getToken()).stringValue());

				// call postExecutionProcess to process the output data after
				// the execution
				postExecutionProcess(outputFileName);

				exitCode.send(0, new IntToken(_grun.getExitCode())); // send out
																		// the
																		// exit
																		// code
																		// token

				if (_grun.getExitCode() != 0) {
					_terminateWorkflow = true; // set the flag to terminate the
												// workflow since errors are
												// reported
				} else {
					// send out standard output/error and output file
					standardOutput.send(0, new StringToken(_grun
							.getStandardOutput()));
					standardError.send(0, new StringToken(_grun
							.getStandardError()));

					outputParameterValue.send(0,
							new StringToken(outputFileName));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This function processes the output data after the execution. Usually
	 * subclasses of the GUIRunCIPRes class will implement this function.
	 * 
	 * @param outputFileName
	 *            is the output file name of the execution
	 */
	public void postExecutionProcess(String outputFileName) {
	}

	/**
	 * Observer function update guiRunObj is the GUIRun object that runs the
	 * program obj is the String message that GUIRun object send back. This
	 * string message is the standard output/error line that contains the
	 * monitored words
	 */
	public void update(Observable guiRunObj, Object obj) {
		String outputMessage = (String) obj;
		int stop = 0; // For default, we assume the execution should be stopped
						// if any error appears

		try {
			// ask the user if we should abort the program execution
			stop = JOptionPane.showConfirmDialog(null, // parent component
					"The program execution reported the following error message.\n"
							+ // Message
							"Would you like to stop the execution?\n" + // Message
							outputMessage + "\n", // Error message for Standard
													// output/error
					"Error Reported", // Title
					JOptionPane.YES_NO_OPTION, // Option type
					JOptionPane.ERROR_MESSAGE); // Message type
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (stop == JOptionPane.YES_OPTION) {
			((GUIRun) guiRunObj).stopExecution(); // stop the execution
		}

	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() throws IllegalActionException {
		if (_terminateWorkflow) { // try to kill the execution
			Nameable container = getContainer();
			if (container instanceof CompositeActor) {
				Manager manager = ((CompositeActor) container).getManager();
				if (manager != null) {
					manager.finish(); // stop the workflow
				} else {
					throw new IllegalActionException(this,
							"Cannot terminate the workflow without a Manager");
				}
			} else {
				throw new IllegalActionException(this,
						"Cannot terminate the workflow without a container that is a CompositeActor");
			}
		}
		return !_terminateWorkflow;
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////
	/**
	 * GUIRun object to invoke external programs.
	 */
	protected GUIRun _grun = new GUIRun("External Program");

	/**
	 * a flag to terminate the whole workflow if the execution of program got
	 * error messages
	 */
	private boolean _terminateWorkflow = false;
}