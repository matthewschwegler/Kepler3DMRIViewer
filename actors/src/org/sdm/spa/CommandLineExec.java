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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.FilePortParameter;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;

// ////////////////////////////////////////////////////////////////////////
// // CommandLineExec
/**
 * Execute a command string.
 * <p>
 * Given a command string, the <I>CommandLineExec</I> actor executes it using
 * the java Runtime class.
 * </p>
 * <p>
 * <b>SUPPORTED COMMAND TYPES:</b>
 * <ul>
 * <li>command</li>
 * <li>command &lt; infile &gt; outfile</li>
 * <li>command &gt; outfile</li>
 * <li>command &lt; infile</li>
 * <li>command [arg1..argn] &gt; outfile</li>
 * <li>command [arg1..argn]</li>
 * <li>command [arg1..argn] &lt; infile &gt; outfile</li>
 * <li>command1 | command 2 <i>(<B>Warning</B>: This type of commands doesn't
 * give the output of all the commands. Instead it outputs only the result of
 * the last one.)</i></li>
 * </ul>
 * </p>
 * <p>
 * <b>Example commands:</b>
 * <ul>
 * <li>C:/Program Files/Internet Explorer/IEXPLORE.EXE <br/>
 * To generate this command, just double click on the actor and type this in the
 * <i>command</i> parameter field.</li>
 * <li>C:/cygwin/bin/perl.exe c:/project/kepler/test/workflows/example.pl &gt;
 * c:/project/kepler/test/workflows/example.out</li>
 * <li>C:/cygwin/bin/dir.exe &gt; dirTemp.txt</li>
 * </ul>
 * </p>
 * 
 * @author Ilkay Altintas, Christopher Hylands Brooks, Bilsay Yildirim,
 *         Contributor: Edward A. Lee
 * @version $Id: CommandLineExec.java 24234 2010-05-06 05:21:26Z welker $
 */

public class CommandLineExec extends TypedAtomicActor {

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
	public CommandLineExec(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		// Uncomment the next line to see debugging statements
		// addDebugListener(new ptolemy.kernel.util.StreamListener());

		stdString = errString = null;

		// Construct parameter
		command = new PortParameter(this, "command");
		new Attribute(command, "_showName");

		outputFile = new FilePortParameter(this, "outputFile");
		outputFile.setTypeEquals(BaseType.STRING);
		new Attribute(outputFile, "_showName");

		// Array with an empty name and value means
		// default environment of the calling process.
		environment = new Parameter(this, "environment");
		environment.setExpression("{{name = \"\", value = \"\"}}");

		directory = new FileParameter(this, "directory");
		directory.setExpression("$CWD");

		// Construct input ports.
		arguments = new TypedIOPort(this, "arguments", true, false);
		arguments.setMultiport(true);
		new Attribute(arguments, "_showName");

		inputStream = new TypedIOPort(this, "inputStream", true, false);
		new Attribute(inputStream, "_showName");

		infileHandle = new TypedIOPort(this, "infileHandle", true, false);
		infileHandle.setTypeEquals(BaseType.STRING);
		new Attribute(infileHandle, "_showName");

		trigger = new TypedIOPort(this, "trigger", true, false);

		hide = new SingletonParameter(trigger, "_hide");
		hide.setToken(BooleanToken.TRUE);

		// Construct output ports.
		outfileHandle = new TypedIOPort(this, "outfileHandle", false, true);
		outfileHandle.setTypeEquals(BaseType.STRING);
		new Attribute(outfileHandle, "_showName");

		exitCode = new TypedIOPort(this, "exitCode", false, true);
		exitCode.setTypeEquals(BaseType.STRING);
		new Attribute(exitCode, "_showName");

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);
		new Attribute(output, "_showName");

		// Set Flags.
		outputLineByLine = new Parameter(this, "outputLineByLine",
				new BooleanToken(false));
		outputLineByLine.setTypeEquals(BaseType.BOOLEAN);

		waitForProcess = new Parameter(this, "waitForProcess",
				new BooleanToken(false));
		waitForProcess.setTypeEquals(BaseType.BOOLEAN);

		hasTrigger = new Parameter(this, "hasTrigger", new BooleanToken(false));
		hasTrigger.setTypeEquals(BaseType.BOOLEAN);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"20\" y=\"25\" "
				+ "style=\"font-size:30; fill:blue; font-family:SansSerif\">"
				+ "$</text>\n" + "</svg>\n");

	} // constructor

	// //////////////// Public ports and parameters ///////////////////////

	/**
	 * The attribute to hide or show the trigger port without deleting it.
	 */
	public SingletonParameter hide;

	/**
	 * The command to execute.
	 */
	public PortParameter command;

	/**
	 * Needs to be filled in if the user wants the command to output to a file.
	 */
	public FilePortParameter outputFile;

	/**
	 * The arguments to the command. Implemented as a multi/input port to
	 * support more than one argument. It concatanates the inputs in all the
	 * channels. <br/>
	 * <I>If there is an input file, this port can be empty.</I>
	 */
	public TypedIOPort arguments;

	/**
	 * Strings to pass to the standard input of the subprocess. This port is an
	 * input port of type String.
	 */
	public TypedIOPort inputStream;

	/**
	 * This is an optional port. Used if the file accepts an input file instead
	 * of a list of arguments.
	 */
	public TypedIOPort infileHandle;

	/**
	 * The trigger port. The type of this port is undeclared, meaning that it
	 * will resolve to any data type.
	 */
	public TypedIOPort trigger;

	/**
	 * A string that forwards the outputFile parameter if it exists.
	 */
	public TypedIOPort outfileHandle;
	/**
	 * The result stream of the command.
	 */
	public TypedIOPort output;

	/**
	 * Exit code will be 1 if the command executes successfully.
	 */
	public TypedIOPort exitCode;

	/**
	 * If selected, broadcasts the output of the command line by line.
	 * 
	 */
	public Parameter outputLineByLine;

	/**
	 * Unhide the trigger port when this parameter is true. This Parameter is
	 * type of boolean.
	 * 
	 * @UserLevelDocumentation <br/>
	 *                         <b>NOTE: </b>in fact, user can use the port
	 *                         configuration window to hide or unhide a port.
	 *                         This paremeter is here to provide a more
	 *                         intuitive interface for this actor.
	 */
	public Parameter hasTrigger;

	/*
	 * The environment to execute the command.
	 */
	public Parameter environment;

	/*
	 * The directory to execute the command
	 */
	public FileParameter directory;

	/*
	 * This allows parameter to allow the process to block the threads until it
	 * exits.
	 */
	public Parameter waitForProcess;

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
	}// attributeChanged

	/**
	 * Send the exitCode and outputFileHandle(optional) to the result port.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		// update the values in the PortParameters
		command.update();
		outputFile.update();

		stdString = null;
		errString = "1";
		_lineFlag = ((BooleanToken) outputLineByLine.getToken()).booleanValue();
		_debug("<TRIGGER_FLAG>" + _lineFlag + "</TRIGGER_FLAG>");

		_waitForProcessFlag = ((BooleanToken) waitForProcess.getToken())
				.booleanValue();

		// Get Directory
		directoryAsFile = directory.asFile();
		if (_debugging) {
			_debug("About to exec \""
					+ ((StringToken) command.getToken()).stringValue() + "\""
					+ "\n in \"" + directoryAsFile + "\"\n with environment:");
		}
		// Process the environment parameter.
		ArrayToken environmentTokens = (ArrayToken) environment.getToken();

		if (_debugging) {
			_debug("environmentTokens: " + environmentTokens);
		}

		environmentArray = null;

		if (environmentTokens.length() >= 1) {
			environmentArray = new String[environmentTokens.length()];

			for (int i = 0; i < environmentTokens.length(); i++) {
				StringToken nameToken = (StringToken) (((RecordToken) environmentTokens
						.getElement(i)).get("name"));
				StringToken valueToken = (StringToken) (((RecordToken) environmentTokens
						.getElement(i)).get("value"));
				environmentArray[i] = nameToken.stringValue() + "="
						+ valueToken.stringValue();

				if (_debugging) {
					_debug("  " + i + ". \"" + environmentArray[i] + "\"");
				}

				if ((i == 0) && (environmentTokens.length() == 1)
						&& environmentArray[0].equals("=")) {
					if (_debugging) {
						_debug("There is only one element, "
								+ "it is a string of length 0,\n so we "
								+ "pass Runtime.exec() an null "
								+ "environment so that we use\n "
								+ "the default environment");
					}
					environmentArray = null;
				}
			}
		}

		if (((StringToken) command.getToken()).stringValue() != null) {
			String _commandStr = ((StringToken) command.getToken())
					.stringValue();

			// simply consume the trigger token if there is some.
			if (_triggerFlag) {
				if (trigger.getWidth() > 0) {
					// if (!((BooleanToken) trigger.get(0)).equals(null)) {
					if (trigger.hasToken(0)) {
						trigger.get(0);
						_debug("consume the token at the trigger port.");
					}
				}
			}

			// Consume the input file token if there's one.

			try {
				if ((infileHandle.numberOfSources() > 0)) {
					String value = ((StringToken) infileHandle.get(0))
							.stringValue();
					if (value.length() > 0) {
						_debug("infileHandle(i) = " + value);
						if (value.startsWith("file:/"))
							value = value.substring(7);
						_commandStr += " < " + value;
					}
				}
			} catch (IllegalActionException ex) {
				_debug("Input file is null.");
			}

			// Create the argument string. Consume data in all the channels and
			// combine them
			String argString = "";
			int i = 0;
			int width = arguments.getWidth();

			for (i = 0; i < width; i++) {
				String value = ((StringToken) arguments.get(i)).stringValue();
				if (value.length() > 0) {
					_debug("arguments(i) = " + value);
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

			int commandCount = getSystemProps();

			// Get the output file path if there's one and add it to the command
			// string.
			if (((StringToken) outputFile.getToken()).stringValue() == "") {
				_debug("Output file is null.");
				outfileHandle.broadcast(new StringToken(""));
			} else {
				String outFilePath = outputFile.stringValue();
				if (outFilePath.startsWith("file:///")) {
					if (_charsToSkip == 6) {
						_charsToSkip = 8;
					} else if (_charsToSkip == 5) {
						_charsToSkip = 7;
					}
				}
				if (outFilePath.startsWith("file:/")) {
					outFilePath = outputFile.stringValue().substring(
							_charsToSkip);
				}
				_commandStr += " > " + outFilePath;

				if (!outFilePath.equals(""))
					outfileHandle.broadcast(new StringToken(outFilePath));
			}

			_commandArr[commandCount] = _commandStr;
			_debug("<COMMAND>" + _commandArr[commandCount] + "</COMMAND>");
			// EXECUTION OF THE GENERATED COMMAND.
			_debug("Executing the command...");

			try {
				_stopFireRequested = false;

				// Execute the command
				Runtime rt = Runtime.getRuntime();
				proc = rt.exec(_commandArr, environmentArray, directoryAsFile);

				InputStream inStream = proc.getInputStream();
				_outputGobbler = new _StreamReaderThread(inStream,
						"Exec Stdout Gobbler-" + _streamReaderThreadCount++,
						this, 1);

				InputStream errStream = proc.getErrorStream();
				_errorGobbler = new _StreamReaderThread(errStream,
						"Exec Stderr Gobbler-" + _streamReaderThreadCount++,
						this, 2);

				if (_streamReaderThreadCount > 1000) {
					// Avoid overflow in the thread count.
					_streamReaderThreadCount = 0;
				}

				_errorGobbler.start();
				_outputGobbler.start();

				// We could have a parameter that if it was set
				// we would throw an exception if there was any error data.
				if (!procDone(proc)) {
					String line = null;
					if ((inputStream.numberOfSources() > 0)) {
						line = ((StringToken) inputStream.get(0)).stringValue();
						if (line.length() > 0) {
							if (_debugging) {
								_debug("CommandLine: Input: '" + line + "'");
							}
							OutputStreamWriter inputStreamWriter = new OutputStreamWriter(
									proc.getOutputStream());
							_inputBufferedWriter = new BufferedWriter(
									inputStreamWriter);
							if (_inputBufferedWriter != null) {
								try {
									_inputBufferedWriter.write(line);
									_inputBufferedWriter.flush();
								} catch (IOException ex) {
									throw new IllegalActionException(this, ex,
											"Problem writing input '" + line
													+ "'");
								}
							}
						}
					}
				}

				// close the child proc's stdin
				// (some programs reading stdin expect it to be
				// closed before excuting, e.g. cat).
				proc.getOutputStream().close();

				// Wait for the Process to finish if it is indicated
				if (_waitForProcessFlag) {
					try {
						// The next line waits for the subprocess to finish.
						int processReturnCode = proc.waitFor();
						// wait for stream gobbler threads to finish
						while (_errorGobbler.isAlive()
								|| _outputGobbler.isAlive()) {
							Thread.yield();
						}
						if (processReturnCode != 0) {
							// We could have a parameter that would enable or
							// disable this.
							throw new IllegalActionException(
									this,
									"Executing command \""
											+ ((StringToken) command.getToken())
													.stringValue()
											+ "\" returned a non-zero return value of "
											+ processReturnCode);
						}
					} catch (InterruptedException interrupted) {
						throw new InternalErrorException(this, interrupted,
								"_process.waitFor() was interrupted");
					} catch (IllegalActionException e) {
						throw new InternalErrorException(this, e,
								"exec'd process exited with non zero exit code");
					}
				}

				if (_debugging) {
					_debug("Exec: Error: '" + errString + "'");
					_debug("Exec: Output: '" + stdString + "'");
				}
			} catch (Exception ex) {
				_debug("<EXCEPTION> An exception occured when executing the command. "
						+ " \n\t Exception: " + ex + "\n</EXCEPTION>");
			}
			// Send error and output to ports
			output.send(0, new StringToken(stdString));
			exitCode.send(0, new StringToken(errString));
		}
	} // end-of-fire

	// Check if the process is done or not
	private boolean procDone(Process p) {
		try {
			p.exitValue();
			return true;
		} catch (IllegalThreadStateException e) {
			return false;
		}
	}

	// Get system properties and set the command array according to it
	public int getSystemProps() throws IllegalActionException {
		// Get OS name
		String osName = System.getProperty("os.name");
		// System.out.println(((StringToken)
		// shell.getToken()).stringValue().toString());
		// String sh="sh";
		_debug("<OS>" + osName + "</OS>");

		if (osName.equals("Windows NT") || osName.equals("Windows XP")
				|| osName.equals("Windows 2000")) {

			_commandArr[0] = "cmd.exe";
			_commandArr[1] = "/C";
			_charsToSkip = 6;
			/*
			 * } else{
			 * 
			 * _commandArr[0] ="C:'\''cygwin'\''cywgin.bat"; _commandArr[1] =
			 * "-c"; _charsToSkip = 6; }
			 */
			return 2;
		} else if (osName.equals("Windows 95")) {
			_commandArr[0] = "command.com";
			_commandArr[1] = "/C";
			_charsToSkip = 6;
			return 2;
		} else {
			_commandArr[0] = "/bin/sh";
			_commandArr[1] = "-c";
			_charsToSkip = 5;
			return 2;
			/* return 0; */
		}
	} // end-of-getSystemProps

	/**
	 * Override the base class to stop waiting for input data.
	 */
	public void stopFire() {
		// NOTE: This method used to be synchronized, as
		// was the fire() method, but this caused deadlocks. EAL
		super.stopFire();
		_stopFireRequested = true;

		try {
			_terminateProcess();
		} catch (IllegalActionException ex) {
			throw new InternalErrorException(ex);
		}
	}

	private void _terminateProcess() throws IllegalActionException {
		if (proc != null) {
			proc.destroy();
			proc = null;
		}
	}

	/**
	 * Terminate the subprocess. This method is invoked exactly once per
	 * execution of an application. None of the other action methods should be
	 * be invoked after it.
	 * 
	 * @exception IllegalActionException
	 *                Not thrown in this base class.
	 */
	public void wrapup() throws IllegalActionException {
		_terminateProcess();
	}

	// /////////////////////////////////////////////////////////////////
	// // inner classes ////
	// Private class that reads a stream in a thread and updates the
	// stringBuffer.
	private class _StreamReaderThread extends Thread {
		/**
		 * Create a _StreamReaderThread.
		 * 
		 * @param inputStream
		 *            The stream to read from.
		 * @param name
		 *            The name of this StreamReaderThread, which is useful for
		 *            debugging.
		 * @param actor
		 *            The parent actor of this thread, which is used in error
		 *            messages.
		 */
		_StreamReaderThread(InputStream inputStream, String name,
				Nameable actor, int ID) {
			super(name);
			_inputStream = inputStream;
			_inputStreamReader = new InputStreamReader(_inputStream);
			_actor = actor;
			_stringBuffer = new StringBuffer();
			myID = ID;
		}

		/**
		 * Read lines from the inputStream and append them to the stringBuffer.
		 */
		public void run() {
			if (!_inputStreamReaderClosed) {
				_read();
			}
		}

		private void _read() {
			// We read the data as a char[] instead of using readline()
			// so that we can get strings that do not end in end of
			// line chars.

			try {
				br = new BufferedReader(_inputStreamReader);
				String line = null;

				while ((line = br.readLine()) != null && !_stopFireRequested) {
					if (line.length() != 0) {
						if (_debugging) {
							// Note that ready might be false here since
							// we already read the data.
							_debug("_read(): Gobbler '" + getName()
									+ "' Ready: " + line + " Value: '" + line
									+ "'");
						}
						_stringBuffer.append(line + "\n");
					}
				}
			} catch (IOException ex) {
				throw new InternalErrorException(_actor, ex, getName()
						+ ": Failed while reading from " + _inputStream);
			}
			// if this thread is reading in stdout, then give this new string to
			// the actor's stdString
			if (myID == 1) {
				stdString = _stringBuffer.toString();
			}
			// if this thread is reading in stderr, then give this new string to
			// the actor's errString
			if (myID == 2) {
				errString = _stringBuffer.toString();
				if ((errString == null) || (errString.length() == 0))
					errString = "1";
			}
		}

		// Last parameter entered on the constructor.
		// 1 indicates storing the standard output, 2 indicates coying to the
		// standard error.
		private int myID;

		// The actor associated with this stream reader.
		private Nameable _actor;

		// Stream from which to read.
		private InputStreamReader _inputStreamReader;

		// Indicator that the stream has been closed.
		private boolean _inputStreamReaderClosed = false;

		// StringBuffer to update.
		private StringBuffer _stringBuffer;

		// BufferReader
		private BufferedReader br;
	}

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////

	// StreamReader with which we read stderr.
	private _StreamReaderThread _errorGobbler;

	// Stream from which to read.
	private InputStream _inputStream;
	// StreamReader with which we read stdout.
	private _StreamReaderThread _outputGobbler;
	// The subprocess gets its input from this BufferedWriter.
	private BufferedWriter _inputBufferedWriter;
	// The combined command to execute.
	private String _commandArr[] = new String[3];
	private boolean _lineFlag = false;
	private boolean _waitForProcessFlag = false;
	private String[] environmentArray;
	private File directoryAsFile;
	private boolean _triggerFlag = false;
	// Indicator that stopFire() has been called.
	private boolean _stopFireRequested = false;
	private Process proc;
	private int _charsToSkip = 6;
	// Instance count of output and error threads, used for debugging.
	// When the value is greater than 1000, we reset it to 0.
	private static int _streamReaderThreadCount = 0;

	private String stdString, errString;
} // end-of-class-CommandLine