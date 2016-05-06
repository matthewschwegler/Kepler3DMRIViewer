/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

package org.ROADnet;

//ptolemy classes
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.Variable;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

import com.brtt.antelope.Orb;
import com.brtt.antelope.OrbSource;

/**
 * This actor connects to an Antelope ORB and collects element values for
 * weather elements such as temperature,pressure,humidity,wind.
 * 
 * @see OrbWaveformSource, OrbWaveformSink, OrbPacketSource,
 * @author Nandita Mangal, University of California
 * @version $Id: OrbLogger.java 24234 2010-05-06 05:21:26Z welker $
 * @UserLevelDocumentation This actor connects to the Antelope ORB and logs the
 *                         required sensor data values.
 */

public class OrbLogger extends TypedAtomicActor {
	/**
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
	public OrbLogger(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// The orb source to connect to, (Samples: mercali.ucsd.edu:6770 or
		// rt.sdsc.edu:6770)
		orbname = new Parameter(this, "orbname");
		orbname.setTypeEquals(BaseType.STRING);

		// Sensor Locations File
		sitesFile = new TypedIOPort(this, "sitesFile", true, false);
		sitesFile.setTypeEquals(BaseType.STRING);

		// A File containing the weather elements to be recorded as well a
		// regular expression of
		// the keywords matching the sourcnames for the above weather elements.
		// e.g Pressure ((pres|Bar|BaroPr).*) [0-1500] specifies Pressure
		// element,with keywords
		// such as pres,Bar,BaroPr (like HPWREN_SDSC_Bar/GENC) and range of
		// values should be 0-1500.
		elementsExpressionFile = new TypedIOPort(this,
				"elementsExpressionFile", true, false);
		elementsExpressionFile.setTypeEquals(BaseType.STRING);

		// The path to save the recorded data files
		filePath = new TypedIOPort(this, "filePath", true, false);
		filePath.setTypeEquals(BaseType.STRING);

		// If the user wants to output the existing workflow Parameters
		paramsOutput = new Parameter(this, "paramsOutput", new BooleanToken(
				false));
		paramsOutput.setTypeEquals(BaseType.BOOLEAN);
		paramsOutput.setDisplayName("Output the Workflow Parameters");

		// If the user wants to output an End/Finished Signal from the current
		// actor
		// check this box in parameters dialog
		triggerOutput = new Parameter(this, "triggerOutput", new BooleanToken(
				false));
		triggerOutput.setTypeEquals(BaseType.BOOLEAN);
		triggerOutput.setDisplayName("Output Finished/End Signal ");

		// If the user wants to output an Error Log of the Exceptions/Errors
		// encountered
		// check this box in parameters dialog
		ErrorOutput = new Parameter(this, "ErrorOutput",
				new BooleanToken(false));
		ErrorOutput.setTypeEquals(BaseType.BOOLEAN);
		ErrorOutput.setDisplayName("Output ErrorsFile Handle ");

		_attachText(
				"_iconDescription",
				"<svg>"
						+ "  <rect x=\"-30\" y=\"-20\" width=\"60\" height=\"40\" "
						+ "        style=\"fill:white\"/> "
						+ "  <circle cx=\"0\" cy=\"0\" r=\"15\" style=\"stroke:black\"/> "
						+ "  <text x=\"-10\" y=\"-5\">Orb</text>" + "</svg>");
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * If the specified attribute is triggerOutput then create a "triggerReady"
	 * output port If the specified attribute is paramOutput then create ports
	 * for all the workflow String Parameters If the specified attribute is
	 * ErrorOutput then create a "ErrorFile" output port The newly created ports
	 * are removed if attribute is unchecked.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute at) throws IllegalActionException {

		try {

			if (at == triggerOutput) {
				_triggerFlag = ((BooleanToken) triggerOutput.getToken())
						.booleanValue();

				// If true create a "triggerReady" port which will output an
				// End/Finished Signal.
				if (_triggerFlag) {
					// check if trigger exists already?
					IOPort deleteTriggerCheck = getPort("triggerReady", 2);
					if (deleteTriggerCheck == null) {
						TypedIOPort p1 = new TypedIOPort(this, "triggerReady",
								false, true);
						new Attribute(p1, "_showName");
						p1.setTypeEquals(BaseType.BOOLEAN);

					}

				}
				// remove trigger port
				else {
					// iterate through each port till we get to triggerReady
					// port.
					List outPortList = this.outputPortList();
					Iterator ports = outPortList.iterator();
					while (ports.hasNext()) {
						IOPort p = (IOPort) ports.next();
						if (p.isOutput()) {
							try {
								if (p.getName().equals("triggerReady")) {
									p.setContainer(null);

								}
							} catch (Exception e) {
								System.err
										.println("Could not delete the trigger port--'"
												+ e.getMessage() + "'.");
								writeErrorLog("Couldn't delete port:trigger"
										+ e.getMessage());

							}// catch
						}// if
					}// while
				}// if triggerFlag
			}
			// Workflow Parameters (if they exist) can have their values send
			// out as
			// param ports with this actor
			// (Useful for scenarios when workflow is being executed as a web
			// application & parameters are set as inputs to a workflow from
			// WorkflowExecute)
			else if (at == paramsOutput) {
				_paramsFlag = ((BooleanToken) paramsOutput.getToken())
						.booleanValue();
				if (_paramsFlag)
					addParamsPorts();
				else
					removeParamsPorts();
			}
			// a log of all errors/exceptions can be output as a port
			else if (at == ErrorOutput) {
				_ErrorsFlag = ((BooleanToken) ErrorOutput.getToken())
						.booleanValue();
				if (_ErrorsFlag)
					addErrorHandle();
				else
					removeErrorHandle();
			}

		} catch (Exception e) {
			System.err.println(e.toString());
			writeErrorLog(e.toString());
		}

	}

	/**
	 * This method initializes the component and connects to the specified orb
	 * source.It also obtains all source names in the current orb and puts them
	 * in array _values.
	 * 
	 */
	public void initialize() throws IllegalActionException {

		super.initialize();
		try {

			// The Orb Source to connect to.
			_orb = new Orb(StringToken.convert(orbname.getToken())
					.stringValue(), "r");

			// Sources will contain all the current sourcenames in the above orb
			OrbSource sources[], srcs[];
			int r, c;

			// get all source names.
			sources = _orb.sources();
			_nSources = sources.length;
			srcs = new OrbSource[_nSources];
			java.lang.System.arraycopy(sources, 0, srcs, 0, _nSources);
			java.util.Arrays.sort(srcs);
			_values = new String[_nSources];
			_selectedValues = new Vector();

			// _values is the array holding all the source names in the current
			// orb.
			for (r = 0; r < _nSources; r++) {
				_values[r] = srcs[r].srcname;
			}

		} catch (Exception e) {

			writeErrorLog("Couldn't connect to Orb and/or get the required element Sources."
					+ e.toString());

		}

	}

	/**
	 * This method first determines how many threads are currently running in
	 * the workflow.For each of the weather elements such as
	 * (Temp,Press,Wind,Humidity) we get the appropriate filter (regular
	 * expression pattern) and narrow down the source names to unstuff for
	 * values. Once these sources have been determined for the current element,
	 * a separate process is started to unstuff each waveform packet. Reaping
	 * each packet's value in a separate process makes sure (a read time out for
	 * one packet/other exceptions) doesn't slow down unstuffing for all the
	 * other sourcenames as well. More of a concurrently reaping way of
	 * unstuffing the packets, due to high number of sensors. (about 1500 in
	 * total)
	 */

	public void fire() throws IllegalActionException {
		super.fire();

		// read all the regular Expression/keywords to be used for source names
		// search
		calculateElementSources();

		// read the path of sensor sites file
		_sitesFileRoot = ((StringToken) (sitesFile.get(0))).stringValue();

		// read the path where to save all the result files produced
		_filePath = ((StringToken) (filePath.get(0))).stringValue();

		// delete pre-existing files
		deleteOldLogFiles();

		// first determine how many threads are running
		determineThreadsFinished();
		int preWorkflowThreads = threadCounter;

		try {
			Vector currentSources = new Vector();
			boolean found = false;

			int j = 0;
			int i = 0;

			// loop for each of the elements
			// elementsCounter is the number of weather elements such as
			// Temperature,Pressure,Humidity,Wind
			for (i = 0; i < elementsCounter; i++) {
				// re-initialize current sources to include all the source names
				// in the ORB
				currentSources.removeAllElements();
				for (j = 0; j < _values.length; j++) {
					if (_values[j] != null)
						currentSources.add(_values[j]);
				}

				// for each filter get a bunch of refined sourcenames
				String filter = (String) regularExpVector.elementAt(i);
				System.err.println(filter);
				for (j = 0; j < currentSources.size(); j++) {

					pattern = Pattern.compile(filter);
					matcher = pattern.matcher((String) currentSources
							.elementAt(j));
					while (matcher.find()) {

						found = true;
					}
					if (!found) { // if filter couldn't find match for htat
									// source name
						// delete it from list of sources for hte element.
						currentSources.removeElementAt(j);
						j--; // update the counter of for loop accordingly
					}
					found = false; // reset boolean false

				}
				// get the narrowed down version of currentSources for the
				// current Element
				// after filter has filtered out some of the source names.

				j = 0;
				// iterate through the filtered sources
				for (j = 0; j < currentSources.size(); j++) {
					if (excludeVector.contains((String) currentSources
							.elementAt(j))) {
						// do nothing: dont add that source.
					} else {

						// add that source
						// Get the source packet value in a separate process

						// Get the required orbsource
						String orbName = StringToken
								.convert(orbname.getToken()).stringValue();

						// Get the current packet name
						String packetName = (String) currentSources
								.elementAt(j);

						// Get the current element being logged
						String elementName = (String) elementsVector
								.elementAt(i);

						// Get the sites file
						String sitesFile = _sitesFileRoot;

						// Get the range for the element
						String ranges = (String) rangeVector.elementAt(i);

						// Reap the Packet (unstuff the packet for the value)
						ReapPacketThread p = new ReapPacketThread(orbName,
								packetName, elementName, sitesFile, ranges,
								_filePath);
						// start the process
						p.start();

					}
				}

			}

		} catch (Exception e) {
			System.err.println(e.toString() + " in Fire");
			writeErrorLog("In Main Fire method:" + e.toString());

		}

		// here we check if all the started threads have finished or not
		// compare with starting number of threads
		boolean continueCheck = true;
		while (continueCheck) {
			determineThreadsFinished();
			if (threadCounter <= preWorkflowThreads) {
				// if all the threads have finished output the ready signal &
				// errorsFile
				// if the user has checked the boxes for them in
				// ParametersDialog.

				IOPort p = getPort("triggerReady", 2);
				if (p != null)
					p.broadcast(new BooleanToken(true));
				calculateParams();

				IOPort p2 = getPort("ErrorsFile", 2);
				if (p2 != null)
					p2.broadcast(new StringToken(_filePath + "/ErrorsLogFile"));
				continueCheck = false;

			}
		}

	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * This method updates threadCounter variable to indicate the number of
	 * current threads.A thread exists in a thread group and a thread group can
	 * contain other thread groups. This method visits all threads in all thread
	 * groups.
	 * 
	 * @author Java Developers Almanac,Addison-Wesley.
	 */

	private void determineThreadsFinished() {

		// Find the root thread group
		ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
		while (root.getParent() != null) {
			root = root.getParent();
		}
		threadCounter = 0;
		// Visit each thread group
		visit(root, 0);
	}

	// This method recursively visits all thread groups under `group'.
	public void visit(ThreadGroup group, int level) {
		// Get threads in `group'
		int numThreads = group.activeCount();
		Thread[] threads = new Thread[numThreads * 2];
		numThreads = group.enumerate(threads, false);

		// Enumerate each thread in `group'
		for (int i = 0; i < numThreads; i++) {
			// Get thread
			Thread thread = threads[i];

			threadCounter++;

		}

		// Get thread subgroups of `group'
		int numGroups = group.activeGroupCount();
		ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
		numGroups = group.enumerate(groups, false);

		// Recursively visit each subgroup
		for (int i = 0; i < numGroups; i++) {
			visit(groups[i], level + 1);
		}
	}

	/**
	 * This method first retrieves all the workflow parameters and creates
	 * appropriate output ports. NOTE: All workflow parameter are currently
	 * expected to be string type
	 */
	private void addParamsPorts() {
		try {
			NamedObj model = getContainer();
			String val = "";
			Iterator atts = model.attributeList().iterator();
			boolean continueLoop = true;
			while (atts.hasNext() && continueLoop == true) {
				Attribute att = (Attribute) atts.next();
				if (att instanceof Variable) {
					String attName = att.getName();

					if (getPort(attName, 2) == null) {

						TypedIOPort p = new TypedIOPort(this, attName, false,
								true);
						new Attribute(p, "_showName");
						Variable var = (Variable) att;
						p.setTypeEquals(var.getToken().getType());
						_paramsPorts.add(attName);
					}

				}
			}

		} catch (Exception e) {
			writeErrorLog("Adding Workflow Parameter Ports" + e.toString());
		}
	}

	/**
	 * This method removes all the workflow ports created.
	 */

	private void removeParamsPorts() {

		List outPortList = this.outputPortList();
		Iterator ports = outPortList.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (p.isOutput()) {
				try {
					if (_paramsPorts.contains(p.getName())) {
						p.setContainer(null);
					}
				} catch (Exception e) {
					writeErrorLog("Removing Workflow Parameter Ports"
							+ e.toString());
				}

			}
		}

		_paramsPorts.removeAllElements();

	}

	/**
	 * This method adds a Error log File port
	 */
	private void addErrorHandle() {
		try {
			TypedIOPort p = new TypedIOPort(this, "ErrorsFile", false, true);
			new Attribute(p, "_showName");
			p.setTypeEquals(BaseType.STRING);
		} catch (Exception e) {
			writeErrorLog("Adding ErrorsLogFile port" + e.toString());
		}
	}

	/**
	 * This method removes the Error log File port
	 */
	private void removeErrorHandle() {
		List outPortList = this.outputPortList();
		Iterator ports = outPortList.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (p.isOutput()) {
				try {
					if (p.getName().equals("ErrorsFile")) {
						p.setContainer(null);
					}
				} catch (Exception e) {
					writeErrorLog("Deleting ErrorsLogFile port" + e.toString());
				}
			}
		}

	}

	/**
	 * This method retrieves the port for the specified name. Mode=1 is input
	 * port & Mode=2 is output port.
	 */

	private IOPort getPort(String portName, int mode) {
		if (mode == 1) {
			List inPortList = this.inputPortList();
			Iterator ports = inPortList.iterator();
			while (ports.hasNext()) {
				IOPort p = (IOPort) ports.next();
				if (p.isInput()) {
					try {
						if (p.getName().equals(portName)) {
							return p;
						}
					} catch (Exception e) {
						writeErrorLog("Couldn't retrieve port" + portName
								+ e.toString());
					}
				}
			}

		} else if (mode == 2) {

			List outPortList = this.outputPortList();
			Iterator ports = outPortList.iterator();
			while (ports.hasNext()) {
				IOPort p = (IOPort) ports.next();
				if (p.isOutput()) {
					try {
						if (p.getName().equals(portName)) {
							return p;
						}
					} catch (Exception e) {
						writeErrorLog("Couldn't retrieve port" + portName
								+ e.toString());

					}
				}
			}

		}
		// null otherwise
		return null;

	}

	/**
	 * This method broadcasts the workflow parameter values to the appropriate
	 * parameter ports
	 */
	private void calculateParams() {

		List outPortList = this.outputPortList();
		Iterator ports = outPortList.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (p.isOutput() && !(p.getName().equals("triggerReady"))) {
				try {

					// retrieve workflow value
					Token t = getWorkflowParam(p.getName());
					if (t != null)
						p.broadcast(t);

				} catch (Exception e) {
					writeErrorLog("Couldn't broadcast workflow parameter values to ports"
							+ e.toString());

				}
			}
		}

	}

	/**
	 * This method simply outputs the following 6 workflow parameters values to
	 * be used by other actors.
	 * colorTemp,colorPress,colorWind,colorHumid,contourWidth,contourLevel.
	 */
	private Token getWorkflowParam(String portName) {

		try {
			NamedObj model = getContainer();
			String val = "";
			Iterator atts = model.attributeList().iterator();
			boolean continueLoop = true;
			while (atts.hasNext() && continueLoop == true) {
				Attribute att = (Attribute) atts.next();
				if (att instanceof Variable) {
					String attName = att.getName();
					if (attName.trim().equals(portName)) {
						Variable var = (Variable) att;
						return var.getToken();
					}

				}
			}

		} catch (Exception e) {
			System.err.println(e.toString());
			writeErrorLog("Couldn't retrieve workflow Parameters"
					+ e.toString());

		}

		return null;
	}

	/**
	 * This method first reads the given regular expressions file containing
	 * sourcenames keywords,ranges as well as weather element names.Accordingly
	 * creates an elementsVector,regularExpVector,rangeVector and an
	 * excludeVector(sources not to be included) for further use later in the
	 * actor.
	 */
	private void calculateElementSources() {

		int tokenCounter = 0;
		elementsCounter = 0;
		String str = "";

		try {

			_expressionsFileRoot = ((StringToken) (elementsExpressionFile
					.get(0))).stringValue();

			BufferedReader in = new BufferedReader(new FileReader(
					_expressionsFileRoot));

			while ((str = in.readLine()) != null && str != "\n") {
				// System.err.println(str);
				StringTokenizer st = new StringTokenizer(str);
				while (st.hasMoreTokens()) {

					if (tokenCounter == 0) // currently at an element
					{
						elementsVector.add(st.nextToken());
						// System.err.println("ELEMENTS " + elementsVector);
					} else if (tokenCounter == 1) // currently at the regular
													// expression
					{
						regularExpVector.add(st.nextToken());
						// System.err.println("REGEXP" + regularExpVector);
					} else if (tokenCounter == 2) // currently at the range
					{
						rangeVector.add(st.nextToken());

						// System.err.println("RANGE" + rangeVector);
					} else if (tokenCounter == 3) // EXCLUDE
					{
						String filter = st.nextToken();
						// System.err.println(filter);
						if (filter.indexOf("EXCLUDE") >= 0) {
							filter = filter.substring(8, filter.length() - 1);

							if (filter.indexOf(",") >= 0) {
								StringTokenizer st2 = new StringTokenizer(
										filter, ",");
								while (st2.hasMoreTokens()) {
									excludeVector.add(st2.nextToken());
								}// while
							}// if-","
							else
								excludeVector.add(filter);

						}// if-exclude
						// System.err.println("EXCLUDE" + excludeVector);
					}// if-token==3
					tokenCounter++;

				}
				tokenCounter = 0;
				elementsCounter++;

			}

		} catch (Exception e) {
			System.err.println(e.toString());
			writeErrorLog(e.toString());
		}

	}

	private void writeErrorLog(String message) {

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(_filePath
					+ "/ErrorsLogFile", true));
			bw.write("\n");
			bw.write(message);
			bw.close();

		} catch (Exception e) {
			System.err.println(e.toString());
		}

	}

	private void deleteOldLogFiles() {
		boolean success = false;

		for (int i = 0; i < elementsCounter; i++) {

			// Get the current element being logged
			String element = (String) elementsVector.elementAt(i);

			success = (new File(new String(_filePath + "/" + element
					+ "FileValues"))).delete();
			if (!success) {
				writeErrorLog("Failed to delete old log file: " + element
						+ "FileValues");
			}
			success = (new File(new String(_filePath + "/" + element
					+ "FileSensors"))).delete();
			if (!success) {
				writeErrorLog("Failed to delete old log file: " + element
						+ "FileSensors");
			}

		}
		success = (new File(new String(_filePath + "/ErrorsLogFile"))).delete();
		if (!success) {
			writeErrorLog("Failed to delete old log file: " + "ErrorsLogFile");
		}

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The name of the orb to connect to, in the format "hostname:port". Note
	 * that orbnames.pf-style names are not supported -- you have to use a valid
	 * IP address or resolvable DNS name, and you have to use a numeric port
	 * number.
	 * 
	 * @UserLevelDocumentation The orbname refers to the specific ORB source
	 *                         that you want to connect to via Antelope software
	 *                         in Kepler.
	 */
	public Parameter orbname;

	// the path where to save all the log files,element values files
	public TypedIOPort filePath;

	// the file path containing regular expressions for elements & source names
	// keywords
	public TypedIOPort elementsExpressionFile;

	// the file path containing sensor locations.
	public TypedIOPort sitesFile;

	// whether to include trigger port
	public Parameter triggerOutput;

	// whether to include workflow parameters ports
	public Parameter paramsOutput;

	// whether to include errorlog file port
	public Parameter ErrorOutput;

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	// contains all the source names for the specified orb.
	private String _values[];
	// the orb which we connected to.
	private Orb _orb;
	// number of sources in the orb
	private int _nSources;
	// current source counter, what index sourceName is currently being reaped.
	private int _sourceCounter = 0;
	// name of current source being reaped.
	private String _currentSource = "";
	// recording of all element values finished or not, indicates whether to
	// continueStream is true/false.
	private boolean _continueStream = true;
	// a vector to hold all the sourcenames for the specific element currently
	// being recorded.
	private Vector _selectedValues;

	// The string containing sensor locations file's path
	private String _sitesFileRoot;
	// The sensor locations File
	private File _sitesFile;
	// The regular expression File
	private File _expressionFile;
	// The path which contains all data files in the local server
	private String _filePath = "";
	// The path to file containing expressions.
	private String _expressionsFileRoot = "";

	private boolean _paramsFlag;
	private boolean _triggerFlag;
	private boolean _ErrorsFlag;

	// a list of all workflow parameter ports created
	private Vector _paramsPorts = new Vector();
	// contains the weather elements being recorded
	private Vector elementsVector = new Vector();

	// contains the regular expresion filters for the above elements
	private Vector regularExpVector = new Vector();
	// contains the ranges for the above elements
	private Vector rangeVector = new Vector();
	// contains the sources not to be excluded
	private Vector excludeVector = new Vector();
	// number of weather elements recorded
	private int elementsCounter = 0;

	private static Pattern pattern;
	private static Matcher matcher;

	// count of threads currently in the application.
	private int threadCounter = 0;
}