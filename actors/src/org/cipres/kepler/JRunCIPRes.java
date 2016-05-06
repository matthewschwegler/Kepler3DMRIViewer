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

import java.util.Observable;
import java.util.Observer;

import javax.swing.JOptionPane;

import org.cipres.helpers.JRun;

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
//// JRunCIPRes
/**
 * This actor calls the external applications.
 * 
 * The JRunCIPRes actor provides a template to invoke external Cipres
 * applications. A set of parameters is configured in the actor to collect
 * application related information. The value of these parameters will be fed to
 * the JRun object when the actor fires. Thus the GUIRun object will invoke the
 * corresponding application with those values, like the input/output file names
 * and the working directory. After the execution, if the external program
 * returns correctly, the standard output of the execution will be sent to the
 * standard output file, and the standard error will be sent to the standard
 * error file.
 * 
 * The user can also set some words to be monitored in the standard output
 * stream. The observer/observable design pattern is used here. The JRunCIPRes
 * object is an observer. The JRun object is an observable. Whenever the JRun
 * object find any of these monitored words in the standard output steam of the
 * program execution, it will notify the JRunCIPRes objects to promote the user
 * handling the issue. Usually the JRunCIPRes actor will stop the execution of
 * the program.
 * 
 * @author Zhijie Guan
 * @version $Id: JRunCIPRes.java 24234 2010-05-06 05:21:26Z welker $
 */

public class JRunCIPRes extends TypedAtomicActor implements Observer {

	/**
	 * Construct JRunCIPRes source with the given container and name.
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

	public JRunCIPRes(CompositeEntity container, String name)
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

		// initialize the input trigger port
		inputTrigger = new TypedIOPort(this, "inputTrigger", true, false);
		inputTrigger.setDisplayName("Input Trigger");
		inputTrigger.setTypeEquals(BaseType.STRING);

		// command name
		command = new FileParameter(this, "command");
		command.setDisplayName("External Command");

		// standard output file name
		outputFile = new FileParameter(this, "outputFile");
		outputFile.setDisplayName("Standard Output File");

		// standard error file name
		errorFile = new FileParameter(this, "errorFile");
		errorFile.setDisplayName("Standard Error File");

		// arguments
		arguments = new Parameter(this, "arguments", new StringToken(""));
		arguments.setDisplayName("Arguments");

		// working directory
		workingDirectory = new Parameter(this, "workingDirectory",
				new StringToken(""));
		workingDirectory.setDisplayName("Working Direcotry");

		// monitored error words
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
	 * The standard output file's path and name are defined in this parameter.
	 */
	public FileParameter outputFile;

	/**
	 * The standard error file's path and name are defined in this parameter.
	 */
	public FileParameter errorFile;

	/**
	 * The arguments of the program that will be executed are defined in this
	 * parameter.
	 */
	public Parameter arguments;

	/**
	 * The workfing directory of the external program is defined in this
	 * parameter.
	 */
	public Parameter workingDirectory;

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
	 * The trigger that will enable the execution of this actor is received
	 * through this port.
	 */
	public TypedIOPort inputTrigger = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Invoke the external program. The monitored words are being monitored
	 * during the execution.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		if (inputTrigger.hasToken(0)) {
			super.fire();
			try {
				// get command file name
				String commandFileName = ((StringToken) command.getToken())
						.stringValue();
				if (commandFileName.length() != 0) {
					_jrun.setCommand(commandFileName);
				} else {
					System.out.println("Command is not defined!");
					throw new IllegalActionException("Command is not defined");
				}

				// get standard output file name
				String outFileName = ((StringToken) outputFile.getToken())
						.stringValue();
				if (outFileName.length() != 0) {
					_jrun.setOutputFileName(outFileName);
				}

				// get standard error file name
				String errorFileName = ((StringToken) errorFile.getToken())
						.stringValue();
				if (errorFileName.length() != 0) {
					_jrun.setErrorFileName(errorFileName);
				}

				// get arguments string
				String argumentsString = ((StringToken) arguments.getToken())
						.stringValue();
				if (argumentsString.length() != 0) {
					_jrun.setArguments(argumentsString);
				}

				// get working directory
				String workingDirName = ((StringToken) workingDirectory
						.getToken()).stringValue();
				if (workingDirName.length() != 0) {
					_jrun.setWorkingDirectory(workingDirName);
				}

				// Here we assume all the programs executed by JRunCIPRes must
				// be finished to get the final results
				// So the JRun is set to wait until the execution is finished
				_jrun.setWaitForExecution(true);

				// check if the user set the monitored error words
				String errorWords = ((StringToken) monitoredErrorWords
						.getToken()).stringValue();
				if (errorWords.length() != 0) {
					_jrun.setMonitoredErrorWords(errorWords);
					_jrun.addObserver(this); // add this GUIRunCIPRes as an
												// observer
				}

				_jrun.execute(); // invoke the external program

				exitCode.send(0, new IntToken(_jrun.getExitCode())); // send out
																		// the
																		// exit
																		// code
																		// token

				if (_jrun.getExitCode() != 0) {
					_terminateWorkflow = true; // set the flag to terminate the
												// workflow since errors are
												// reported
				} else {
					standardOutput.send(0, new StringToken(_jrun
							.getStandardOutput()));
					standardError.send(0, new StringToken(_jrun
							.getStandardError()));

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Observer function update jRunObj is the jRun object that runs the program
	 * obj is the String message that jRun object send back. This string message
	 * is the standard output/error line that contains the monitored words
	 */
	public void update(Observable jRunObj, Object obj) {
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
			((JRun) jRunObj).stopExecution(); // stop the execution
		}

	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() throws IllegalActionException {
		if (_terminateWorkflow) {
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
	 * JRun object to invoke external programs
	 */
	private JRun _jrun = new JRun();

	/**
	 * a flag to terminate the whole workflow since the execution of program got
	 * error messages
	 */
	private boolean _terminateWorkflow = false;
}