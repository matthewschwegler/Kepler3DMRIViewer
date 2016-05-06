/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-10-22 14:29:41 -0700 (Tue, 22 Oct 2013) $' 
 * '$Revision: 32507 $'
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.BrowserLauncher;
import ptolemy.actor.gui.style.TextStyle;
import ptolemy.data.BooleanToken;
import ptolemy.data.LongToken;
import ptolemy.data.StringToken;
import ptolemy.data.XMLToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import util.UUIDGen;

//////////////////////////////////////////////////////////////////////////
////BrowserUI
/*
 * <p>BrowserUI actor is an actor that:
 * <ul>
 * <li> displays a text/HTML output of an actor including different
 * graphical output, and
 * <li> interacts with the user during workflow execution.
 * </ul>
 * </p>
 * <p>Given a <i>file path or URL</i> including a CGI-based form, or
 * <i>text/HTML content</i>, this actor can be used for injecting user
 * control and input. It can also be used for efficient output of legacy
 * applications anywhere in a workflow via the user's local web browser.
 * The BrowserUI actor uses the default browser in the user's computer.
 * execution, the actor just outputs the (name, value) pairs in XML format and
 * as separate arrays.</p>
 *
 * <p>The actor can be configured using the configuration interface to allow
 * for automatic CGI form generation. The configuration is made through a text
 * box that simply specifies the name and type of the output ports that the
 * user wants to configure the actor for. Please refer to
 * BrowserUIConfigureTest.xml under workflows/test in your directory for
 * more information on the configuration of this actor.</p>
 *
 * @author Ilkay Altintas, Efrat Jaeger and Kai Lin
 * @version $Id: BrowserUI.java 32507 2013-10-22 21:29:41Z jianwu $
 *
 */

public class BrowserUI extends TypedAtomicActor {

	/**
	 * Construct a BrowserUI actor with the given container and name.
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
	public BrowserUI(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		fileOrURLPort = new TypedIOPort(this, "fileOrURL", true, false);
		fileOrURLPort.setTypeEquals(BaseType.STRING);
		new Attribute(fileOrURLPort, "_showName");

		fileOrURLParameter = new FileParameter(this, "fileOrURL");

		fileFormat = new StringParameter(this, "file extension");
		fileFormat.setExpression("");
		fileFormat.addChoice(".html");
		fileFormat.addChoice(".xml");
		fileFormat.addChoice(".txt");
		fileFormat.addChoice(".xsl");
		fileFormat.addChoice(".svg");

		fileContent = new TypedIOPort(this, "fileContent", true, false);
		fileContent.setTypeEquals(BaseType.STRING);
		new Attribute(fileContent, "_showName");

		portConfiguration = new StringParameter(this, "portConfiguration");
		TextStyle portConfigStyle = new TextStyle(portConfiguration,
				"portConfiguration");

		maxWaitTime = new Parameter(this, "max wait time",
				new LongToken("300"));
		maxWaitTime.setTypeEquals(BaseType.LONG);
		
		useForDisplay = new Parameter(this, "use for display",
				new BooleanToken(false));
		useForDisplay.setTypeEquals(BaseType.BOOLEAN);

		hasTrigger = new Parameter(this, "hasTrigger", new BooleanToken(false));
		hasTrigger.setTypeEquals(BaseType.BOOLEAN);

		xmlOutput = new TypedIOPort(this, "xmlOutput", false, true);
		xmlOutput.setTypeEquals(BaseType.XMLTOKEN);
		new Attribute(xmlOutput, "_showName");

		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setTypeEquals(BaseType.GENERAL);
		// trigger.setContainer(null);
		hide = new SingletonParameter(trigger, "_hide"); // DFH
		hide.setToken(BooleanToken.TRUE); // DFH

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"100\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"10\" y=\"25\" "
				+ "style=\"font-size:16; fill:blue; font-family:SansSerif\">"
				+ "BROWSER</text>\n" + "</svg>\n");

	} // end of constructor

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public SingletonParameter hide;

	/**
	 * A parameter indicating how many seconds this actor will wait before
	 * throwing time out exception.
	 */
	public Parameter maxWaitTime;
	
	/**
	 * A boolean parameter indicating whether this actor is used only for
	 * display only or will it be used for user interactions.
	 */
	public Parameter useForDisplay;

	/**
	 * A boolean parameter indicating whether trigger port has to be activated
	 * in order to schedule the actor.
	 */
	public Parameter hasTrigger;

	/**
	 * Output ports configuration.
	 * <p>
	 * The description of the ports should be represented as
	 * <ul>
	 * <li>portName portType
	 * </ul>
	 * one line per each output port.
	 */
	public StringParameter portConfiguration;

	/**
	 * Input file or URL parameter.
	 */
	public TypedIOPort fileOrURLPort;

	/**
	 * Input file or URL parameter.
	 */
	public FileParameter fileOrURLParameter;

	/** Content of the file. */
	public TypedIOPort fileContent;

	/**
	 * An xml name, value pair user response output.
	 */
	public TypedIOPort xmlOutput;

	/**
	 * The format of the data (used for content input).
	 */
	public StringParameter fileFormat;

	/**
	 * The port to trigger the actor in case there are no input ports connected.
	 * This port is used only for scheduling the actor.
	 * <p>
	 * <i>Please activate the hasTrigger parameter to display this port, and
	 * uncheck it to hide/disable it.</i>
	 * </p>
	 */
	public TypedIOPort trigger;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Callback for changes in attribute values.
	 * 
	 * @param at
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 *                If the offsets array is not nondecreasing and nonnegative.
	 */
	public void attributeChanged(Attribute at) throws IllegalActionException {
		if (at == hasTrigger) {
			_triggerFlag = ((BooleanToken) hasTrigger.getToken())
					.booleanValue();
			_debug("<TRIGGER_FLAG>" + _triggerFlag + "</TRIGGER_FLAG>");
			if (_triggerFlag) {
				// check if the trigger input port exists.
				boolean triggerExists = false;
				List inPortList = this.inputPortList();
				Iterator ports = inPortList.iterator();
				while (ports.hasNext()) {
					IOPort p = (IOPort) ports.next();
					if (p.isInput()) {
						if (p.getName().equals("trigger")) {
							triggerExists = true;
						}
					}
				}
				if (!triggerExists) { // if there is no trigger port.
					try {
						trigger.setContainer(this);
						hide.setToken(BooleanToken.FALSE); // DFH
					} catch (NameDuplicationException ndex) {
						_debug(ndex.getMessage());
						GraphicalMessageHandler.message(ndex.getMessage()
								+ "Could not create the trigger port in actor:"
								+ this.getName());
					}
				}
			} else {
				List inPortList = this.inputPortList();
				Iterator ports = inPortList.iterator();
				while (ports.hasNext()) {
					IOPort p = (IOPort) ports.next();
					if (p.isInput()) {
						try {
							if (p.getName().equals("trigger")) {
								// p.setContainer(null); //DFH
								hide.setToken(BooleanToken.TRUE); // DFH
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
		} else if (at == portConfiguration) {
			try {
				String configuration = ((StringToken) portConfiguration
						.getToken()).stringValue();
				BufferedReader br = new BufferedReader(new StringReader(
						configuration));
				String line = "";
				List ports = new ArrayList();
				while ((line = br.readLine()) != null) {
					if (!line.trim().equals("")) {
						String portName = line.trim();
						if (portName.equals("xmlOutput")) {
							throw new IllegalActionException(
									"xmlOutput port is already in use "
											+ "in actor " + this.getName());
						}
						ports.add(portName);
						// Type _inferType(portType);//FIXME: ADD TYPES
					}
				}
				// if there are any named ports add them and turn
				// _namedPortsExist on.
				if (ports.size() > 0) {
					_setOutputPorts(ports);
					_namedPortsExist = true;
				}
			} catch (IOException ioex) {
				throw new IllegalActionException(
						"Could not process configuration in actor "
								+ this.getName() + ": " + ioex.getMessage());
			}
		} else if (at == useForDisplay) {
			forDisplay = ((BooleanToken) useForDisplay.getToken())
					.booleanValue();
			// if true remove all output ports.
			if (forDisplay) {
				try {
					List outputPorts = this.outputPortList();
					Iterator it = outputPorts.iterator();
					while (it.hasNext()) {
						IOPort iop = (IOPort) it.next();
						iop.setContainer(null);
					}
				} catch (NameDuplicationException nex) {
					throw new IllegalActionException(this,
							"Could not remove xmlOutput port in actor "
									+ this.getName() + ": " + nex.getMessage());
				}
			} else {
				// else add xml output attribute
				try {
					xmlOutput.setContainer(this);
				} catch (NameDuplicationException nex) {
					throw new IllegalActionException(this,
							"Could not add xmlOutput port in actor "
									+ this.getName() + ": " + nex.getMessage());
				}
				attributeChanged(portConfiguration);
			}
		} else
			super.attributeChanged(at);

	} // end of attributeChanged

	/**
	 * Given a ...., BrowserUI actor opens a broser ...
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		Reader in = null;
		StringToken inputToken = null;
		// String ext = "";
		boolean fileContentFlag = false;
		boolean hasForm = false;
		// UUIDGen uuidgen = new UUIDGen();
		String strFileOrURL = "";

		// TODO: read trgier
		try {
			for (int i = 0; i < trigger.getWidth(); i++) {
				trigger.get(i);
			}

			// if content port is connected then the input is a file content.
			if (fileContent.getWidth() > 0) {
				inputToken = (StringToken) fileContent.get(0);

				if (inputToken != null) {
					String content = inputToken.stringValue();
					_ext = fileFormat.getExpression();
					// ADDED if forDisplay or known display content extension.
					// display and return.					
					if (forDisplay || _extensions.contains(_ext)) {
						strFileOrURL = _writeContentToFile(content);
						BrowserLauncher.openURL(strFileOrURL);
						//sleep for 1000 ms because the BrowserLauncher may not be able to open url before the workflow execution finishes.
						Thread.sleep(1000);
						return;
					} else {
						in = new StringReader(content);
						fileContentFlag = true;
					}

				} else {
					// If the inputToken is null set _reFire to false.
					System.out.println("No Token Exception!");
					_reFire = false;
					return;
				}
			}

			// the input is a file or a url.
			else {
				if (fileOrURLPort.getWidth() > 0) {
					try {
						inputToken = (StringToken) fileOrURLPort.get(0);
					} catch (Exception ex) {
					}

					if (inputToken != null) {
						// set the fileOrURL parameter to be the input token.
						fileOrURLParameter.setExpression(inputToken
								.stringValue());
					} else {
						// If the inputToken is null set _reFire to false.
						System.out.println("No Token Exception!");
						_reFire = false;
						return;
					}
				}
				// Remove an ending \n (before converting to URL).
				strFileOrURL = ((StringToken) fileOrURLParameter.getToken())
						.stringValue();
				int lineEndInd = strFileOrURL.indexOf("\n");
				if (lineEndInd > -1) {
					strFileOrURL = strFileOrURL.substring(0, lineEndInd);
					fileOrURLParameter.setExpression(strFileOrURL);
				}

				// Read the fileOrURL parameter.
				URL fileOrURL = fileOrURLParameter.asURL();
				strFileOrURL = fileOrURL.toExternalForm();

				_debug("<fileOrURL>" + strFileOrURL + "</fileOrURL>");
				if (strFileOrURL.trim().equals("")) {
					throw new IllegalActionException(this,
							"Input string is empty. ");
				}
				// Immediately display contents of non interactive extensions
				// (e.g., jpeg,,,).
				if (forDisplay || _isDisplayExtension(fileOrURL)) {
					BrowserLauncher.openURL(strFileOrURL);
					// TODO: if used only for display remove ports.
					// TODO: if there is no connected output ports use only for
					// display.
					return;
				}

				// Get the input stream.
				URL url = new URL(strFileOrURL);
				in = new InputStreamReader(url.openStream());

				// in = _getURLReader(strFileOrURL);
			}
			// THIS CODE ASSUMES THAT THERE WILL BE JUST ONE FORM IN THE HTML!!!

			BufferedReader br = new BufferedReader(in);
			String line;

			// TODO: perhaps a better approach is necessary here
			// (not to save the whole file content to ram).
			StringBuffer sbContent = new StringBuffer();

			while ((line = br.readLine()) != null) {
				int beginFormTagInd = line.toLowerCase().indexOf("<form ");
				if (beginFormTagInd != -1) {
					int endFormTagInd = line.toLowerCase().indexOf(">",
							beginFormTagInd + 5);
					String formTag = "";
					if (endFormTagInd > -1) {
						formTag = line.substring(beginFormTagInd,
								endFormTagInd + 1);
					} else
						formTag = line.substring(beginFormTagInd);

					String formLine = line;
					while (endFormTagInd == -1) {
						formLine = br.readLine();
						endFormTagInd = formLine.toLowerCase().indexOf(">");
						if (endFormTagInd > -1) {
							formTag += formLine.substring(0, endFormTagInd + 1);
						} else
							formTag = formLine;

					}
					formTag = _processFormTag(formTag);
					line = line.substring(0, beginFormTagInd) + formTag
							+ formLine.substring(endFormTagInd + 1);
				}
				int closeFormInd = line.toLowerCase().indexOf("</form>");
				if (closeFormInd != -1) {
					// the file has a form.
					hasForm = true;
					sbContent.append(line.substring(0, closeFormInd) + "\n");
					// Generate a universal id for the request
					_procID = _uuidgen.generateUUID();

					// Append the form in the html source with "extraLine" that
					// has the id.
					String extraLine = "<input type=\"hidden\" name=\"ptid\" value=\""
							+ _procID + "\"/>";

					sbContent.append(extraLine + "\n");
					sbContent.append(line.substring(closeFormInd) + "\n");
				} else {
					sbContent.append(line + "\n");
				}
			}
			br.close();
			String contentFileToDisplay = "";
			if (!hasForm) {
				// if this is a file content, write it to a temp file and
				// display.
				if (fileContentFlag) {
					strFileOrURL = _writeContentToFile(sbContent.toString());
				}
				BrowserLauncher.openURL(strFileOrURL);
				return;
			} else { // if there is a form.
				if (!fileContentFlag) {
					// TODO: if this is a file or a url. replace relative paths
					// (A6). --- Efrat: Replace relative paths in general
				}
				if (_ext.equals("")) {
					_ext = ".html";
				}
				strFileOrURL = _writeContentToFile(sbContent.toString());
				BrowserLauncher.openURL(strFileOrURL);

				// FIXME: change to kepler server.
				String fetchURL = serverPath + "?ptid="
						+ _procID;
				System.out.println("fetchURL:  " + fetchURL + "\n");
				URL url = new URL(fetchURL);

				String xml = _getUserResponse(url);
				xmlOutput.broadcast(new XMLToken(xml));

				// create name value pairs and broadcast response if
				// _namedPortsExists.
				if (_namedPortsExist) {
					_sendValuesToNamedPorts(xml);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			if (contentDisplayFile != null) {
				if (contentDisplayFile.exists()) {
					contentDisplayFile.delete();
				}
			}
			throw new IllegalActionException(this,
			"An exception occured in actor " + this.getName() + ":"
					+ ex.getMessage());
//			try {
//				if (server != null)
//				{
//					server.stopServer();
//					server = null;
//				}
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				throw new IllegalActionException("exception in actor " + this.getName() + ":" + e.getMessage());
//			}			
//			throw new IllegalActionException(this,
//					"An exception occured in actor " + this.getName() + ":"
//							+ ex.getMessage());
		}
	} // end of fire

	public void initialize() throws IllegalActionException {

		// create the unique id generator.
		_uuidgen = new UUIDGen();

		// Get the useForDisplay parameter or verify whether to just display the
		// url.
		forDisplay = ((BooleanToken) useForDisplay.getToken()).booleanValue();
		
		

		// Set the path to the backend server.
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    //get the specific configuration we want
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty serversProperty = commonProperty.getProperty("servers.server");
    ConfigurationProperty geonProperty = serversProperty.findProperties("name", "geon").get(0);
    serverUrl = geonProperty.getProperty("url").getValue();
	serverPort = new Integer (geonProperty.getProperty("port").getValue()).intValue();
	serverContext = geonProperty.getProperty("context").getValue();
	if (maxWaitTime.getToken() == null)
		serverMaxWaitTime = new Long (geonProperty.getProperty("maxWaitTime").getValue()).longValue();
	else
		serverMaxWaitTime = ((LongToken) maxWaitTime.getToken()).longValue();
	serverPath = serverUrl + ":" + serverPort + "/" + serverContext;
    
		// If there are no connected output ports - set forDisplay to be true.
		if (!forDisplay) {
			List outputPorts = outputPortList();
			Iterator outputPortsIterator = outputPorts.iterator();
			// check whether this actor is a sink.
			boolean isSink = true;
			while (outputPortsIterator.hasNext()) {
				IOPort p = (IOPort) outputPortsIterator.next();
				if (p.getWidth() > 0) {
					// the actor is not a sink.
					isSink = false;
					break;
				}
			}
			if (isSink) {
				forDisplay = true;
			}
		}

		try {
			server = new BrowserUIServer();
			server.startServer(serverContext, serverPort);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalActionException("exception in actor " + this.getName() + ":" + e.getMessage());
		}
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() throws IllegalActionException {

		if (contentDisplayFile != null) {
			if (contentDisplayFile.exists()) {
				contentDisplayFile.delete();
			}
		}
		contentDisplayFile = null;

		if (_reFire)
			return super.postfire();
		else
			return _reFire;
			
	} // end of postfire

	/**
	 * Wrapup the actor. Set _reFire to true.
	 */
	public void wrapup() throws IllegalActionException {
//		try {
//			if (server != null)
//			{
//				server.stopServer();
//				server = null;
//			}			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			throw new IllegalActionException("exception in actor " + this.getName() + ":" + e.getMessage());
//		}

		if (contentDisplayFile != null) {
			if (contentDisplayFile.exists()) {
				contentDisplayFile.delete();
			}
		}
		contentDisplayFile = null;
		_reFire = true;
	}

	private Map _createNameValuePairs(String xml) {

		Map output = new HashMap();
		while (!xml.trim().equals("</xmp>")) {

			int indStart = xml.toLowerCase().indexOf("<name>");
			if (indStart == -1) { // no name tag - return output hashmap.
				return output;
			}
			int indEnd = xml.toLowerCase().indexOf("</name>");
			String currentParamName = xml.substring(indStart + 6, indEnd);
			xml = xml.substring(indEnd + 7);
			indStart = xml.toLowerCase().indexOf("<value>");
			indEnd = xml.toLowerCase().indexOf("</value>");
			String currentParamValue = xml.substring(indStart + 7, indEnd);
			xml = xml.substring(indEnd + 8);

			output.put(currentParamName, currentParamValue);
		}
		return output;
	}

	private String _getUserResponse(URL url) throws IllegalActionException {
		long startTime  = System.currentTimeMillis();
		long currentTime  = System.currentTimeMillis();

		while (serverMaxWaitTime < 0 || currentTime < (startTime + (serverMaxWaitTime * 1000))) {
			HttpURLConnection urlconn;
			try {
				urlconn = (HttpURLConnection) url.openConnection();
				urlconn.connect();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(
						urlconn.getInputStream()));
				String line = br.readLine();
				String resp = "";
				while (line != null) {
					resp += line + "\n";
					line = br.readLine();
				}
				br.close();
				urlconn.disconnect();

				int xmpInd = resp.toLowerCase().indexOf("<xmp>");
				if (xmpInd != -1) {
					return resp;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			currentTime  = System.currentTimeMillis();
		}
		throw new IllegalActionException("Operation timed out in actor "
				+ this.getName() + ".");
	}

	private void _sendValuesToNamedPorts(String xml)
			throws IllegalActionException {
		Map pairs = _createNameValuePairs(xml);
		List outputPorts = this.outputPortList();
		Iterator outputs = outputPorts.iterator();
		while (outputs.hasNext()) {
			IOPort p = (IOPort) outputs.next();
			String portName = p.getName();
			if (!portName.equals("xmlOutput")) {
				String value = (String) pairs.get(portName);
				// TODO: by port configuration convert the string to the desired
				// type.

				p.broadcast(new StringToken(value));
			}
		}
	}

	/**
	 * if this is a file with no extension, just display it. if this is a url
	 * with no extension, it can have a form.
	 */
	private boolean _isDisplayExtension(URL pfileOrURL) {
		String path = pfileOrURL.getPath();
		String _fileExtension = "";
		int lastDotInd = path.lastIndexOf(".");
		// there is an extension.
		if (lastDotInd > -1) {
			_fileExtension = path.substring(lastDotInd);
			_fileExtension = _fileExtension.trim();
			String ext = _fileExtension.toLowerCase();
			if (_extensions.contains(ext))
				return true;
			else
				return false;
		} else { // there is no extension.
			return false;
		}
	}

	/**
	 * This function accepts the form tab and sets the post action.
	 * 
	 * @param formTag
	 * 	 */
	private String _processFormTag(String formTag) {
		int actionInd = formTag.toLowerCase().indexOf("action");
		actionLine = "action=\"" + serverPath + "\"";
		// if there is already an action - overide it.
		if (actionInd > -1) {
			int firstquoteInd = formTag.indexOf("\"", actionInd + 1);
			int endActionInd = -1;
			if (firstquoteInd == -1) {
				firstquoteInd = formTag.indexOf("'", actionInd + 1);
				endActionInd = formTag.indexOf("'", firstquoteInd + 1);
			} else {
				endActionInd = formTag.indexOf("\"", firstquoteInd + 1);
			}
			if (firstquoteInd == -1) { // no action tag value
				formTag = formTag.substring(0, actionInd) + actionLine
						+ formTag.substring(actionInd + 6);
			} else {
				formTag = formTag.substring(0, actionInd) + actionLine
						+ formTag.substring(endActionInd + 1);
			}
		}
		// else add action
		else
			formTag = formTag.substring(0, 6) + actionLine
					+ formTag.substring(6);

		return formTag;
	}

	private void _setOutputPorts(List ports) throws IllegalActionException {
		// remove unnecessary ports
		List outputPorts = this.outputPortList();
		Iterator it = outputPorts.iterator();
		while (it.hasNext()) {
			IOPort iop = (IOPort) it.next();
			if (!ports.contains(iop.getName())
					&& !iop.getName().equals("xmlOutput"))
				try {
					iop.setContainer(null);
				} catch (NameDuplicationException nex) {
					throw new IllegalActionException(this, nex.getMessage()
							+ " Could not delete port " + iop.getName()
							+ " to actor " + this.getName());
				}

		}
		// create desired ports and set types for existing ones.
		outputPorts = this.outputPortList();
		for (int i = 0; i < ports.size(); i++) {
			it = outputPorts.iterator();
			boolean portExists = false; // checking whether the output port
										// exists.
			while (it.hasNext()) {
				TypedIOPort p = (TypedIOPort) it.next();
				if (p.getName().equals((String) ports.get(i))) {
					p.setTypeEquals(BaseType.STRING);
					portExists = true;
					break;
				}
			}
			if (!portExists) {
				try {
					TypedIOPort iop = new TypedIOPort(this, (String) ports
							.get(i), false, true);
					iop.setTypeEquals(BaseType.STRING);
					new Attribute(iop, "_showName");
				} catch (NameDuplicationException ex) {
					throw new IllegalActionException(ex.getMessage()
							+ " Actor " + this.getName()
							+ " already contains a port " + "named "
							+ (String) ports.get(i));
				}
			}
		}
	}

	/**
	 * Writes the content input to a uniquely identified file.
	 * 
	 * @param content
	 * 	 * @throws IOException
	 */
	private String _writeContentToFile(String content) throws IOException {
		String contentFileToDisplay = _uuidgen.generateUUID() + _ext;
		contentDisplayFile = new File(System.getProperty("user.home") + File.separator + contentFileToDisplay);
		String strFileOrURL = contentDisplayFile.getAbsolutePath();
		BufferedWriter out = new BufferedWriter(new FileWriter(strFileOrURL,
				false));
		out.write(content);
		out.close();
		return strFileOrURL;

	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	// a flag to verify whether the html contains a form.
	private boolean _reFire = true;

	private static Set _extensions = new TreeSet();

	static {

		_extensions.add(".xml");
		_extensions.add(".xsl");
		_extensions.add(".gif");
		_extensions.add(".jpg");
		_extensions.add(".jpeg");
		_extensions.add(".svg");
		_extensions.add(".pdf");
		_extensions.add(".ps");
		_extensions.add(".ppt");
		_extensions.add(".doc");
		_extensions.add(".pps");
		_extensions.add(".txt");
		_extensions.add(".zip");
		// _extensions.add(""); - would cause an unentered extension to be a
		// display extension..

	}

	private boolean _triggerFlag = false;
	private File contentDisplayFile = null;
	// private String _fileExtension = "";
	private String _absoluteFilePathToDisplay = "";
	private String _procID = "";
	private boolean forDisplay;
	private String _ext = "";

	/**
	 * The unique id generator.
	 */
	private UUIDGen _uuidgen;

	/**
	 * Specifies whether to broadcast response to name ports set by the
	 * configuration parameter.
	 */
	private boolean _namedPortsExist = false;

	/**
	 * Represents the server post action URL.
	 */
	private String actionLine = "";

	// private static final String ACTIONLINE =
	// "action=\"http://geon01.sdsc.edu:8080/pt2/jsp/pts.jsp\"";

	/**
	 * Path to the geon server url in the config file.
	 */
	private static final String SERVERPATH = "//servers/server[@name=\"geon\"]/url";

	/**
	 * URL to backend server
	 */
	private String serverPath = "";

    private String serverUrl = "";

	private int serverPort= 0;

	private long serverMaxWaitTime = 0;

    private String serverContext = "";

	private BrowserUIServer server = null;

}
