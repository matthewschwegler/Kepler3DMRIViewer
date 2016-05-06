/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-18 11:40:51 -0700 (Tue, 18 Sep 2012) $' 
 * '$Revision: 30702 $'
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

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.ServiceException;

import org.apache.axis.Message;
import org.apache.axis.constants.Style;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.Parameters;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.apache.axis.wsdl.symbolTable.SymbolTable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.IOPort;
import ptolemy.actor.Manager;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.XMLToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;

//////////////////////////////////////////////////////////////////////////
////MessageBasedWebService
/**
 * The MessageWebService actor, provides the user with a plug-in interface to
 * execute any WSDL-defined web service. Given a URL for the WSDL of a web
 * service and an operation name that is included in the WSDL, this actor
 * customizes itself to execute this web service operation.
 * <P>
 * WSDL is an XML format for describing network services as a set of endpoints
 * operating on messages containing either document-oriented or procedure-
 * oriented information. The operations and messages are described abstractly,
 * and then bound to a concrete network protocol and message format to define an
 * endpoint. Related concrete endpoints are combined into abstract endpoints
 * (services). WSDL is extensible to allow description of endpoints and their
 * messages regardless of what message formats or network protocols are used to
 * communicate. More information on WSDL and realted standard can be found at:
 * http://www.w3.org/TR/wsdl
 * <P>
 * The user can instantiate the generic web service actor by providing the WSDL
 * URL and choosing the desired web service operation. The actor then a
 * utomatically specializes itself and adds ports with the inputs and outputs as
 * described by the WSDL. The so instantiated actor acts as a proxy for the web
 * service being executed and links to the other actors through its ports.
 * <P>
 * The WSDL is parsed to get the input, output and binding information. It
 * dynamically generates ports for each input and output of the operation. This
 * customization happens at the configuration time of a model. When the actor is
 * fired at run time, it gets the binding information and creates a call object
 * to run the model. Using this call object, it invokes the web service and
 * broadcasts the response to the output ports.
 * 
 * The above MessageBasedWebService actor expects XMLTokens as input and
 * broadcasts XMLTokens as well.
 * 
 * <P>
 * <I><B >Notices to users:</B>
 * <ul>
 * <li>Please double-click on the actor to start customization.
 * <li>To enter a WSDL URL which is not in the given list of WSDL URLs, click on
 * the "Preferences" button on the configuration interface and change the type
 * of the parameter to "Text". Then you can type in the WSDL you would like to
 * use.
 * <li>After you select the WSDL, "Commit" and double-click on the actor again.
 * This will reconfigure the list of available operations. Please do this
 * everytime you change the WSDL URL.
 * </ul>
 * </i>
 * 
 * @author Ilkay Altintas, Nandita Mangal
 * @version $Id: MessageBasedWebService.java 30702 2012-09-18 18:40:51Z crawl $
 * @deprecated Use org.sdm.spa.WebService or org.sdm.spa.WSWithComplexTypes instead.
 */

public class MessageBasedWebService extends TypedAtomicActor {

	/**
	 * Construct a WebService actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has actor with this name.
	 */
	public MessageBasedWebService(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		// Parameters for the MessageBasedWebService Actor as
		// wsdlURL/ws_endpoint and ws_methodName
		// Username,Password for protected web services.

		wsdlUrl = new StringParameter(this, "wsdlUrl");
		methodName = new StringParameter(this, "methodName");
		userName = new StringParameter(this, "userName");
		password = new StringParameter(this, "password");

		startTrigger = new TypedIOPort(this, "startTrigger", true, false);
		hide = new SingletonParameter(startTrigger, "_hide");
		hide.setToken(BooleanToken.TRUE);

		// Set the trigger Flag.
		hasTrigger = new Parameter(this, "hasTrigger", new BooleanToken(false));
		hasTrigger.setTypeEquals(BaseType.BOOLEAN);

		// this port is used for displaying the errors during execution of the
		// actor
		clientExecErrors = new TypedIOPort(this, "clientExecErrors", false,
				true);
		clientExecErrors.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The parameter for the URL of the web service WSDL.
	 */
	public StringParameter wsdlUrl;
	/**
	 * The parameter for the method name.
	 */
	public StringParameter methodName;
	/**
	 * The userName to invoke the web service if necessary.
	 */
	public StringParameter userName;
	/**
	 * The password to invoke the web service if necessary.
	 */
	public StringParameter password;
	/**
	 * This is an parameter to activate the optional startTrigger port.
	 * <I>Please activate it <i>ONLY</I> when the actor has no input and it is
	 * required for scheduling of the actor.
	 */
	public Parameter hasTrigger;
	/**
	 * This is an optional input port that can be used to help the scheduling of
	 * the actor.
	 * 
	 * <P>
	 * This port is activated by the hasTrigger parameter. Double-click on the
	 * actor to enable. <I>Please enable it <i>ONLY</I> when the actor has no
	 * input and it is required for scheduling of the actor.
	 */
	public TypedIOPort startTrigger;
	/**
	 * It outputs the errors if any occured when actor is executing. It outputs
	 * "NO ERRORS." if there are no exceptional cases.
	 * 
	 */
	public TypedIOPort clientExecErrors;

	public SingletonParameter hide;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Callback for changes in attribute values Get the WSDL from the given URL.
	 * 
	 * @param at
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute at) throws IllegalActionException {

		System.err.println("ATTRIBUTE CHANGED CALLED");
		if (at == hasTrigger) {
			_triggerFlag = ((BooleanToken) hasTrigger.getToken())
					.booleanValue();
			_debug("<TRIGGER_FLAG>" + _triggerFlag + "</TRIGGER_FLAG>");
			if (_triggerFlag) {
				try {
					// startTrigger.setContainer(this);
					hide.setToken(BooleanToken.FALSE);
				} catch (Exception ex) {
					_debug("111: " + ex.getMessage());
					GraphicalMessageHandler.message(this.getFullName()
							+ ": Could not create the trigger port--'"
							+ ex.getMessage() + "'.");
				}
			} else {
				List inPortList = this.inputPortList();
				Iterator ports = inPortList.iterator();
				while (ports.hasNext()) {
					IOPort p = (IOPort) ports.next();
					if (p.isInput()) {
						try {
							if (p.getName().equals("startTrigger")) {
								// p.setContainer(null);
								hide.setToken(BooleanToken.TRUE);
							}
						} catch (Exception e) {
							GraphicalMessageHandler.message(this.getFullName()
									+ ": Could not delete the trigger port--'"
									+ e.getMessage() + "'.");
						}
					}
				}
			}
		} else if (at == wsdlUrl) {

			List inPortList = this.inputPortList();
			int numInPorts = inPortList.size();
			List outPortList = this.outputPortList();
			int numOutPorts = outPortList.size();

			if (wsdlUrl.getExpression().equals("")) {
				// System.out.println("WSD url is empty string");
				// do nothing
			} else if ((_urlStr.equals(""))
					&& (methodName.getExpression().equals(""))
					&& (numInPorts == 0) && (numOutPorts == 0)) {
				// System.out.println("WSDL url before getExpression: " +
				// _urlStr);
				_urlStr = wsdlUrl.getExpression();
				// System.out.println("Method name before removeAllChoice: " +
				// _methodNameStr);
				methodName.removeAllChoices();
				// System.out.println("Method name after removeAllChoice: " +
				// _methodNameStr);
				// System.out.println("WSDL url after getExpression: " +
				// _urlStr);
				_wsdlParser = new Parser();
				_confErrorStr = "";
				// Parse the wsdl for the web service.
				try {
					_wsdlParser.run(_urlStr);
					configureOperationNames();
				} catch (Exception ex) {
					_debug("<EXCEPTION> There was an error while parsing the WSDL. "
							+ ex + ". </EXCEPTION>");
					_confErrorStr += "\n" + this.getFullName()
							+ ": Could not parse WSDL--'" + ex.getMessage()
							+ "'.";// );
				}
				if (!(_confErrorStr.equals(""))) {
					GraphicalMessageHandler.message(_confErrorStr);
				}
			} else if ((_urlStr.equals(""))
					&& (!(methodName.getExpression().equals("")))
					&& ((numInPorts != 0) && (numOutPorts != 0))) // The actor
																	// has at
																	// least one
																	// port
			{
				// System.out.println("WSDL url before getExpression: " +
				// _urlStr);
				_urlStr = wsdlUrl.getExpression();
				// System.out.println("Method name before removeAllChoice: "
				// +_methodNameStr);
				// System.out.println("Method name after removeAllChoice: " +
				// _methodNameStr);
				// System.out.println("WSDL url after getExpression: " +
				// _urlStr);
			} else if (!(this._urlStr.equals(wsdlUrl.getExpression()))) { // If
																			// the
																			// value
																			// has
																			// really
																			// changed.
				_confErrorStr = "";
				// System.out.println("WSDL url before getExpression: " +
				// _urlStr);
				_urlStr = wsdlUrl.getExpression();
				// System.out.println("Method name before removeAllChoice: " +
				// _methodNameStr);
				methodName.removeAllChoices();
				// System.out.println("Method name after removeAllChoice: "
				// +_methodNameStr);
				// System.out.println("WSDL url after getExpression: " +
				// _urlStr);
				deletePorts();
				_wsdlParser = new Parser();
				// Parse the wsdl for the web service.
				try {
					_wsdlParser.run(_urlStr);
					configureOperationNames();
				} catch (Exception ex) {
					_debug("<EXCEPTION> There was an error while parsing the WSDL. "
							+ ex + ". </EXCEPTION>");
					// GraphicalMessageHandler.message(
					_confErrorStr += "\n"
							+ ex.getMessage()
							+ "There was an error while parsing the WSDL in the actor: "
							+ this.getName();// );
				}
			} else {
				_debug("The " + _urlStr
						+ " was the same. Ports left unchanged.");
			}
			if (!(_confErrorStr.equals(""))) {
				GraphicalMessageHandler.message(_confErrorStr);
			}
		} else if (at == methodName) {

			List inPortList = this.inputPortList();
			int numInPorts = inPortList.size();
			List outPortList = this.outputPortList();
			int numOutPorts = outPortList.size();

			if (methodName.getExpression().equals("")) {
				// System.out.println("Method name is empty string.");
				// do nothing
			} else if ((_methodNameStr.equals("")) && (numInPorts == 0)
					&& (numOutPorts == 0)) {
				_confErrorStr = "";
				// System.out.println("Method name before get expression: "
				// +_methodNameStr);
				_methodNameStr = methodName.getExpression();
				_debug("<METHOD_NAME>" + _methodNameStr + "</METHOD_NAME>");
				// System.out.println("Method name after get expression: " +
				// _methodNameStr);
				int slashIndex = _urlStr.lastIndexOf('/');
				_wsName = _urlStr.substring(slashIndex + 1,
						_urlStr.length() - 5);

				_attachText(
						"_iconDescription",
						"<svg>\n"
								+ "<rect x=\"0\" y=\"0\" "
								+ "width=\"160\" height=\"50\" "
								+ "style=\"fill:white\"/>\n"
								+ "<text x=\"20\" y=\"25\""
								+ "style=\"font-size:11; fill:red; font-family:SansSerif\">"
								+ _wsName + "_" + _methodNameStr + "</text>\n"
								+ "</svg>\n");
				configureActor();
				if (!(_confErrorStr.equals(""))) {
					GraphicalMessageHandler.message(_confErrorStr);
				}
			} else if ((_methodNameStr.equals(""))
					&& ((numInPorts != 0) && (numOutPorts != 0))) // The actor
																	// has at
																	// least one
																	// port
			{
				// System.out.println("Method name before get expression: "
				// +_methodNameStr);
				_methodNameStr = methodName.getExpression();
				_debug("<METHOD_NAME>" + _methodNameStr + "</METHOD_NAME>");
				// System.out.println("Method name after get expression: " +
				// _methodNameStr);
				int slashIndex = _urlStr.lastIndexOf('/');
				_wsName = _urlStr.substring(slashIndex + 1,
						_urlStr.length() - 5);
				_attachText(
						"_iconDescription",
						"<svg>\n"
								+ "<rect x=\"0\" y=\"0\" "
								+ "width=\"160\" height=\"50\" "
								+ "style=\"fill:white\"/>\n"
								+ "<text x=\"20\" y=\"25\""
								+ "style=\"font-size:11; fill:red; font-family:SansSerif\">"
								+ _wsName + "_" + _methodNameStr + "</text>\n"
								+ "</svg>\n");
			} else if (!(this._methodNameStr.equals(methodName.getExpression()))) { // if
																					// the
																					// methodName
																					// really
																					// changed.

				_confErrorStr = "";
				// System.out.println("Method name before get expression: "
				// +_methodNameStr);
				_methodNameStr = methodName.getExpression();
				_debug("<METHOD_NAME>" + _methodNameStr + "</METHOD_NAME>");
				// System.out.println("Method name after get expression: " +
				// _methodNameStr);
				int slashIndex = _urlStr.lastIndexOf('/');
				_wsName = _urlStr.substring(slashIndex + 1,
						_urlStr.length() - 5);

				_attachText(
						"_iconDescription",
						"<svg>\n"
								+ "<rect x=\"0\" y=\"0\" "
								+ "width=\"160\" height=\"50\" "
								+ "style=\"fill:white\"/>\n"
								+ "<text x=\"20\" y=\"25\""
								+ "style=\"font-size:11; fill:red; font-family:SansSerif\">"
								+ _wsName + "_" + _methodNameStr + "</text>\n"
								+ "</svg>\n");

				// Delete all the ports the actor has when the method name
				// changes.
				deletePorts();
				configureActor();
				if (!(_confErrorStr.equals(""))) {
					GraphicalMessageHandler.message(_confErrorStr);
				}
			} else {
				_debug("The " + _methodNameStr + " was the same. "
						+ "Ports left unchanged.");
			}
		}
	} // end of attributeChanged

	/**
	 * Configure the actor for the entered operation of the given web service.
	 */
	public void configureOperationNames() {

		System.err.println("CONFIGURE OPERATION NAMES callled");

		try {
			_service = null;

			// Find the entry for the ServiceEntry class in the symbol table.
			HashMap map = _wsdlParser.getSymbolTable().getHashMap();
			Iterator entrySetIter = map.entrySet().iterator();
			while (entrySetIter.hasNext()) {
				Map.Entry currentEntry = (Map.Entry) entrySetIter.next();
				Vector valueVector = (Vector) currentEntry.getValue();
				int vecSize = valueVector.size();
				for (int index = 0; index < vecSize; ++index) {
					SymTabEntry symTabEntryObj = (SymTabEntry) valueVector
							.get(index);
					if ((ServiceEntry.class).isInstance(symTabEntryObj)) {
						_service = ((ServiceEntry) symTabEntryObj).getService();
					}
				}
			}

			Port port = _getSOAPAddress(_service.getPorts());
			if (port == null) {
				_debug("<ERROR> No port was returned by the _getSOAPAddress. </ERROR>");

			}
			_portName = "";
			_portName = port.getName();
			_binding = port.getBinding();
			SymbolTable symbolTable = _wsdlParser.getSymbolTable();
			BindingEntry bEntry = symbolTable.getBindingEntry(_binding
					.getQName());

			Operation operation = null;
			Parameters parameters = null;
			Iterator iter = bEntry.getParameters().keySet().iterator();
			for (; iter.hasNext();) {
				Operation oper = (Operation) iter.next();
				methodName.addChoice(oper.getName());
			}
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error when configuring the actor: "
					+ ex + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_confErrorStr += "\n" + this.getFullName()
					+ ": Could not configure actor--'" + ex.getMessage() + "'.";// );
		}
	} // end-of-configureOperationNames

	/**
	 * Configure the actor for the entered operation of the given web service.
	 */
	public void configureActor() {

		System.err.println("CONFIGURE ACTOR called:");
		try {
			_service = null;

			// Find the entry for the ServiceEntry class in the symbol table.
			HashMap map = _wsdlParser.getSymbolTable().getHashMap();
			Iterator entrySetIter = map.entrySet().iterator();
			while (entrySetIter.hasNext()) {
				Map.Entry currentEntry = (Map.Entry) entrySetIter.next();
				Vector valueVector = (Vector) currentEntry.getValue();
				int vecSize = valueVector.size();
				for (int index = 0; index < vecSize; ++index) {
					SymTabEntry symTabEntryObj = (SymTabEntry) valueVector
							.get(index);
					if ((ServiceEntry.class).isInstance(symTabEntryObj)) {
						_service = ((ServiceEntry) symTabEntryObj).getService();
					}
				}
			}

			Port port = _getSOAPAddress(_service.getPorts());
			if (port == null) {
				_debug("<ERROR> No port was returned by the _getSOAPAddress. </ERROR>");
			}
			_portName = "";
			_portName = port.getName();
			_binding = port.getBinding();
			SymbolTable symbolTable = _wsdlParser.getSymbolTable();
			BindingEntry bEntry = symbolTable.getBindingEntry(_binding
					.getQName());

			Operation operation = null;
			Parameters parameters = null;
			Iterator iter = bEntry.getParameters().keySet().iterator();
			for (; iter.hasNext();) {
				Operation oper = (Operation) iter.next();
				if (oper.getName().equals(_methodNameStr)) {
					operation = oper;
					parameters = (Parameters) bEntry.getParameters().get(oper);
					createPorts(parameters);

					// Set output type
					if (parameters.returnParam == null) {
						_returnMode = 1; // Get outputs into a map object!
					} else if (parameters.returnParam != null) {
						_returnMode = 2; // Get the invoke result value as a
											// single value.
						// Get the QName for the return Type
						QName returnQName = parameters.returnParam.getQName();
						if (((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
								.getType().getDimensions().equals("[]")) {
							Node arrTypeNode = ((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
									.getType().getNode();
							String baseTypeStr = _getArrayBaseType(arrTypeNode);
							_debug("ARRAY PARAM BASE TYPE: " + baseTypeStr);
							_createPort(
									((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
											.getMode(), baseTypeStr,
									(String) returnQName.getLocalPart());
						} else {
							_createPort(
									((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
											.getMode(),
									((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
											.getType().getQName()
											.getLocalPart(),
									(String) returnQName.getLocalPart());
						}
						_debug("<RETURN_QNAME>" + returnQName.getLocalPart()
								+ "</RETURN_QNAME>");
					}
					// Break out of the loop
					break;
				}
			}
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error when configuring the actor: "
					+ ex + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_confErrorStr += "\n" + ex.getMessage()
					+ "There was an error when configuring the actor:"
					+ this.getName();// );
		}
	} // end-of-configureActor

	/**
	 * Query the base type of the array type specified in the given dom node.
	 * 
	 * @param arrayTypeNode
	 * 	 */
	private String _getArrayBaseType(Node arrayTypeNode) {
		_debug("TYPE NAME: "
				+ arrayTypeNode.getAttributes().getNamedItem("name"));
		String baseTStr = arrayTypeNode.getFirstChild().getFirstChild()
				.getFirstChild().getAttributes().getNamedItem("wsdl:arrayType")
				.getNodeValue();
		String[] result = baseTStr.split(":");
		baseTStr = result[1];
		return baseTStr;
	} // end-of-getArrayBaseType

	/** Creates ports for the web service operation */
	public void createPorts(Parameters params) {
		try {
			// Create ports using the input and output part descriptions
			for (int j = 0; j < params.list.size(); j++) {
				org.apache.axis.wsdl.symbolTable.Parameter param = (org.apache.axis.wsdl.symbolTable.Parameter) params.list
						.get(j);
				_debug("PARAM DIMENSION: " + param.getType().getDimensions());
				_debug("PARAM TYPE: "
						+ param.getType().getQName().getLocalPart());
				if (param.getType().getDimensions().equals("[]")) {

					Node arrTypeNode = param.getType().getNode();
					_debug("TYPE NAME: "
							+ arrTypeNode.getAttributes().getNamedItem("name"));
					String baseTypeStr = arrTypeNode.getFirstChild()
							.getFirstChild().getFirstChild().getAttributes()
							.getNamedItem("wsdl:arrayType").getNodeValue();
					String[] result = baseTypeStr.split(":");
					baseTypeStr = result[1];
					_debug("ARRAY PARAM BASE TYPE: " + baseTypeStr);
					_createPort(param.getMode(), baseTypeStr, (String) param
							.getQName().getLocalPart());
				} else { // if (param.getType().getDimension() != null) {
					_createPort(param.getMode(), param.getType().getQName()
							.getLocalPart(), (String) param.getQName()
							.getLocalPart());
				}
			}
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error when creating the TypedIOPorts: "
					+ ex + "</EXCEPTION>");
			// GraphicalMessageHandler.message(
			_errorsStr += "\n" + this.getFullName()
					+ ": Could not create ports--'" + ex.getMessage() + "'.";// );
		}
	}

	private void _createPort(int mode, String portTypeStr, String portNameStr) {

		System.err.println("CREATE PORT CALLED");
		try {
			if (mode == 1) { // input
				TypedIOPort pin = new TypedIOPort(this, portNameStr, true,
						false);
				new Attribute(pin, "_showName");
				_setPortType(pin, portTypeStr);
				_debug("<INPUT>" + portNameStr + "</INPUT>");
			} else if (mode == 2) { // output
				TypedIOPort pout = new TypedIOPort(this, portNameStr, false,
						true);
				new Attribute(pout, "_showName");
				_setPortType(pout, portTypeStr);
				_debug("<OUTPUT>" + portNameStr + "</OUTPUT>");
			} else if (mode == 3) { // input/output
				TypedIOPort pin = new TypedIOPort(this, portNameStr, true,
						false);
				new Attribute(pin, "_showName");
				_setPortType(pin, portTypeStr);
				_debug("<INPUT>" + portNameStr + "</INPUT>");
				TypedIOPort pout = new TypedIOPort(this, portNameStr, false,
						true);
				new Attribute(pout, "_showName");
				_setPortType(pout, portTypeStr);
				_debug("<OUTPUT>" + portNameStr + "</OUTPUT>");
			}
		} catch (ptolemy.kernel.util.IllegalActionException iae) {
			_debug("<EXCEPTION> There was an IllegalActionException when creating the TypedIOPorts in _createPort: "
					+ iae + "</EXCEPTION>");
			// GraphicalMessageHandler.message(
			_errorsStr += "\n"
					+ iae.getMessage()
					+ "There was an error when creating the TypedIOPorts in actor: "
					+ this.getName();// );
		} catch (ptolemy.kernel.util.NameDuplicationException nde) {
			// GraphicalMessageHandler.message(
			_errorsStr += "\n"
					+ nde.getMessage()
					+ "\nThere was a NameDuplicationException when creating the "
					+ "TypedIOPorts in the actor: " + this.getName();// );
			_debug("<EXCEPTION> There was a NameDuplicationException when creating the TypedIOPorts in _createPort: "
					+ nde + "</EXCEPTION>");
		}
	} // end-of-createPort

	/** Deletes all the ports of this actor. */
	public void deletePorts() {
		List inPortList = this.inputPortList();
		Iterator ports = inPortList.iterator();
		while (ports.hasNext()) {
			IOPort p = (IOPort) ports.next();
			if (p.isInput()) {
				try {
					if (!(p.getName().equals("startTrigger"))) {
						p.setContainer(null);
					}
				} catch (Exception e) {
					// GraphicalMessageHandler.message(
					_confErrorStr += "\n" + e.getMessage()
							+ "Could not delete the input port: " + p.getName()
							+ " in the actor: " + this.getName();// );
				}
			}
		}

		List outPortList = this.outputPortList();
		int numOutPorts = outPortList.size();
		Iterator oports = outPortList.iterator();
		while (oports.hasNext()) {
			IOPort outp = (IOPort) oports.next();
			if (outp.isOutput()) {
				try {
					if (!(outp.getName().equals("clientExecErrors"))) {
						outp.setContainer(null);
					}
				} catch (Exception e) {
					// GraphicalMessageHandler.message(
					_confErrorStr += "\n" + e.getMessage()
							+ "Could not delete the output port:"
							+ outp.getName() + " in the actor: "
							+ this.getName();// );
				}
			}
		}
	} // end of deletePorts

	/**
	 * Get the URL address of the the location of the web service defined by the
	 * given WSDL. Get the the namespace and binding information ofthe web
	 * service using the given WSDL and methodName. Add a parameterto the call
	 * object for each input part.Fill these parameters with the input values on
	 * channels on the ports that correspond to them. Invoke the web service
	 * using all the gathered information. Send the response of the call to the
	 * result port.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		super.fire();

		// triggerring the actor..
		if (startTrigger.getWidth() > 0) {
			for (int i = 0; i < startTrigger.getWidth(); i++)
				startTrigger.get(i);
		}

		_urlStr = wsdlUrl.getExpression();
		_methodNameStr = methodName.getExpression();

		_wsdlParser = new Parser();

		try {
			_wsdlParser.run(_urlStr);
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error while parsing the WSDL in fire for URL: "
					+ _urlStr + " .\n" + ex + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_errorsStr += "\n"
					+ ex.getMessage()
					+ "Error in fire: There was an error while parsing the WSDL in the actor: "
					+ this.getName();// );
		}
		getServiceBinding();
		try {
			SymbolTable symbolTable = _wsdlParser.getSymbolTable();
			BindingEntry bEntry = symbolTable.getBindingEntry(_binding
					.getQName());
			Operation operation = null;
			Parameters parameters = null;
			Iterator iter = bEntry.getParameters().keySet().iterator();
			for (; iter.hasNext();) {
				Operation oper = (Operation) iter.next();
				if (oper.getName().equals(_methodNameStr)) {
					operation = oper;
					parameters = (Parameters) bEntry.getParameters().get(oper);
					// Set output type
					if (parameters.returnParam == null) {
						_returnMode = 1; // Get outputs into a map object!
					} else if (parameters.returnParam != null) {
						_returnMode = 2; // Get the invoke result value as a
											// single value.
					}
					// Break out of the loop
					break;
				}
			}
		} catch (Exception ex) {
			_debug("<EXCEPTION>In fire when setting the return mode: " + ex
					+ ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_errorsStr += "\n"
					+ ex.getMessage()
					+ "There was an error when setting up the return mode of the web "
					+ "service at: " + this.getName();// );
		}

		try {

			// prepare and make call

			org.apache.axis.client.Service myServiceClient = new org.apache.axis.client.Service();
			_call = (org.apache.axis.client.Call) myServiceClient.createCall();

			System.out.println("URL:" + _urlStr);
			_call.setTargetEndpointAddress(_urlStr);
			_call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS,
					Boolean.FALSE);
			_call.setProperty(org.apache.axis.AxisEngine.PROP_SEND_XSI,
					Boolean.FALSE);
			_call.setOperationName(new QName(_urlStr, _methodNameStr));
			// _call.addParameter("body", XMLType.SOAP_DOCUMENT,
			// ParameterMode.IN);
			// _call.setReturnType(XMLType.ESOAP_DOCUMENT);
			_call.setOperationStyle(Style.DOCUMENT);

			// _debug("<OBJ_ARR_LENGTH> " + (new
			// Integer(_objArr.length)).toString() + " </OBJ_ARR_LENGTH>");
			// _debug("<USERNAME> " + userName.stringValue() + "</USERNAME>");
			// _call.setProperty(Call.USERNAME_PROPERTY,userName.stringValue());
			_debug("<PASSWORD> " + password.stringValue() + " </PASSWORD>");
			_call.setProperty(Call.PASSWORD_PROPERTY, password.stringValue());

			Document request_doc = null;

			List inPortList = this.inputPortList();
			Iterator ports = inPortList.iterator();
			while (ports.hasNext()) {
				IOPort p = (IOPort) ports.next();
				System.out.println("Input = " + p.getName());
				if (p.isInput() && !p.getName().equals("startTrigger")) {
					Object obj = p.get(0);
					System.out.println("Input = " + obj);
					if (obj instanceof XMLToken) {
						XMLToken token = (XMLToken) obj;
						System.out.println("INPUT is " + token);
						try {
							ByteArrayInputStream bais = new ByteArrayInputStream(
									token.toString().getBytes());
							request_doc = XMLUtils.newDocument(bais);
						} catch (Exception ex) {
							ex.printStackTrace();
							_debug("<EXCEPTION>" + ex + "</EXCEPTION>");
						}
					}
				}
			}

			if (request_doc != null) {
				SOAPBodyElement inputEle = new SOAPBodyElement(request_doc
						.getDocumentElement());
				// System.out.println(xml);
				System.out.println("Starting the invoke!");

				_call.invoke(new Object[] { inputEle });

				System.out.println("Got results from the invoke...");

				Message response = _call.getResponseMessage();
				SOAPBodyElement s1body = null;
				String tempOutput = null;
				try {
					s1body = response.getSOAPEnvelope().getFirstBody();
					tempOutput = XMLUtils.DocumentToString(s1body
							.getAsDocument());
					System.out.println(tempOutput);
					_debug("<TEMPOUTPUT>" + tempOutput + "</TEMPOUTPUT>");
				} catch (Exception ex) {
					ex.printStackTrace();
					_debug("<EXCEPTION>" + ex + "</EXCEPTION>");
				}
				// _sendOutput(output, tempOutput);

				// We would send the temp out to all the output ports
				List outPortList = this.outputPortList();
				Iterator oports = outPortList.iterator();
				// Now we are sending the Document (in String form) namely
				// tempOutput to the
				// respective port via the method _sendOutput.

				while (oports.hasNext()) {
					TypedIOPort po = (TypedIOPort) oports.next();
					System.out.println("Output port = " + po.getName());
					if (!(po.getName().equals("clientExecErrors"))) {
						po.broadcast(new XMLToken(tempOutput));
						System.out.println("Output is" + tempOutput);
					}
				}
			} else {
				System.out.println("No input found");

			}
		} catch (ServiceException se) {
			se.printStackTrace();
			_debug("<EXCEPTION> Service exception in fire() method: "
					+ se.getMessage() + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_errorsStr += "\n" + se.getMessage()
					+ "\nThe service exception error occured in the actor: "
					+ this.getName();// );
		} catch (Exception rex) {
			rex.printStackTrace();
			_debug("<EXCEPTION> Remote exception in fire() method: "
					+ rex.getMessage() + ". </EXCEPTION>");
			// get rmi.getCause() and print it here.
			// GraphicalMessageHandler.message(
			_errorsStr += "\n" + rex.getMessage()
					+ "\nThe remote exception error occured in the actor: "
					+ this.getName();// );

			// FIX ME: Don't stop the model here but pause and let the user
			// refine it!
			/*
			 * // NOTE: We need to consume data on all channels that have data.
			 * // If we don't then DE will go into an infinite loop. for (int i
			 * = 0; i < input.getWidth(); i++) { if (input.hasToken(i)) { if
			 * (((BooleanToken)input.get(i)).booleanValue()) { result = true; }
			 * } }
			 */
			Nameable container = getContainer();
			if (container instanceof CompositeActor) {
				Manager manager = ((CompositeActor) container).getManager();
				if (manager != null) {
					manager.finish();
				} else {
					throw new IllegalActionException(this,
							"Cannot stop without a Manager.");
				}
			} else {
				throw new IllegalActionException(this,
						"Cannot stop without a container that is a CompositeActor.");
			}
		}

		System.out.println(_errorsStr);
		if (!(_errorsStr.equals(""))) {
			clientExecErrors.broadcast(new StringToken(_errorsStr));
		} else {
			clientExecErrors.broadcast(new StringToken("NO ERRORS."));
		}
	} // end of fire

	/** Configure the service, port and binding info. */
	public void getServiceBinding() {
		try {
			_service = null;

			// Find the entry for the ServiceEntry class in the symbol table.
			HashMap map = _wsdlParser.getSymbolTable().getHashMap();
			Iterator entrySetIter = map.entrySet().iterator();
			while (entrySetIter.hasNext()) {
				Map.Entry currentEntry = (Map.Entry) entrySetIter.next();
				Vector valueVector = (Vector) currentEntry.getValue();
				int vecSize = valueVector.size();
				for (int index = 0; index < vecSize; ++index) {
					SymTabEntry symTabEntryObj = (SymTabEntry) valueVector
							.get(index);
					if ((ServiceEntry.class).isInstance(symTabEntryObj)) {
						_service = ((ServiceEntry) symTabEntryObj).getService();
					}
				}
			}

			Port port = _getSOAPAddress(_service.getPorts());
			if (port == null) {
				_debug("<ERROR> No port was returned by the _getSOAPAddress.</ERROR>");
			}
			_portName = "";
			_portName = port.getName();
			_binding = port.getBinding();
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error when configuring the actor: "
					+ ex + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_errorsStr += "\n" + ex.getMessage()
					+ "There was an error when configuring the actor:"
					+ this.getName();// );
		}
	} // end-of-getServiceBinding

	/**
	 * Pre fire the actor. Calls the super class's prefire in case something is
	 * set there.
	 */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	} // end of prefire

	// ////////////////////////////////////////////////////////////////////
	// // private methods
	// //

	/**
	 * Returns a port with a SOAPAddress extensibility element.
	 */
	private Port _getSOAPAddress(Map ports) {
		Iterator nameIter = ports.keySet().iterator();
		while (nameIter.hasNext()) {
			String portName = (String) nameIter.next();
			Port port = (Port) ports.get(portName);
			List extElemList = port.getExtensibilityElements();
			for (int i = 0; (extElemList != null) && (i < extElemList.size()); i++) {
				Object extEl = extElemList.get(i);
				if (extEl instanceof SOAPAddress) {
					return port;
				}
			}
		}
		return null;
	} // end-of-getSOAPAddress

	/**
	 * _sendOutput Send the output ports from the given port with the right type
	 * casting.
	 * 
	 * @param outPort
	 * @param res
	 */
	private void _sendOutput(TypedIOPort outPort, Object res) {
		_debug("<RES_CLASS_NAME>" + res.getClass().getName()
				+ "</RES_CLASS_NAME>");
		try {
			if (res instanceof String) {
				if (outPort.getType().toString().equals("string")) {
					outPort.broadcast(new StringToken((String) res));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(String) do not match in _sendOutput(). <ERROR>");
				}
			} else if (res instanceof Integer) {
				if (outPort.getType().toString().equals("int")) {
					outPort.broadcast(new IntToken(((Integer) res).intValue()));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(Integer) do not match in_sendOutput(). <ERROR>");
				}
			} else if (res instanceof Double) {
				if (outPort.getType().toString().equals("double")) {
					outPort.broadcast(new DoubleToken(((Double) res)
							.doubleValue()));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(Double) do not match in sendOutput(). <ERROR>");
				}
			} else if (res instanceof Long) {
				if (outPort.getType().toString().equals("long")) {
					outPort.broadcast(new LongToken(((Long) res).longValue()));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(Long) do not match in _sendOutput().<ERROR>");
				}
			} else if (res instanceof Boolean) {
				if (outPort.getType().toString().equals("boolean")) {
					outPort.broadcast(new BooleanToken(((Boolean) res)
							.booleanValue()));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(Boolean) do not match in _sendOutput(). <ERROR>");
				}
			} else if (res instanceof String[]) {
				if (outPort.getType().toString().equals("{string}")) {
					String[] resultArr = (String[]) res;
					int xxx = resultArr.length;
					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);
						resultArrStr += resultArr[resCount] + ", ";
					}
					resultArrStr += resultArr[xxx - 1] + "}";
					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(String[]) do not match in_sendOutput(). <ERROR>");
				}
			} else if ((res instanceof Integer[])) {
				_debug("IN Integer[]");
				if (outPort.getType().toString().equals("{int}")) {
					Integer[] resultArr = (Integer[]) res;
					int xxx = resultArr.length;
					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);
						resultArrStr += resultArr[resCount] + ", ";
					}
					resultArrStr += resultArr[xxx - 1] + "}";
					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(String[]) do not match in_sendOutput(). <ERROR>");
				}
			} else if (res instanceof int[]) {
				_debug("IN int[]");
				if (outPort.getType().toString().equals("{int}")) {
					int[] resultArr = (int[]) res;
					int xxx = resultArr.length;
					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);
						resultArrStr += resultArr[resCount] + ", ";
					}
					resultArrStr += resultArr[xxx - 1] + "}";
					_debug(resultArrStr);
					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(String[]) do not match in_sendOutput(). <ERROR>");
				}
			} else if (res instanceof Double[]) {
				_debug("IN Double[]");
				if (outPort.getType().toString().equals("{double}")) {
					Double[] resultArr = (Double[]) res;
					int xxx = resultArr.length;
					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);
						resultArrStr += resultArr[resCount] + ", ";
					}
					resultArrStr += resultArr[xxx - 1] + "}";
					_debug(resultArrStr);
					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(Double[]) do not match in_sendOutput(). <ERROR>");
				}
			} else if (res instanceof double[]) {
				_debug("IN double[]");
				if (outPort.getType().toString().equals("{double}")) {
					double[] resultArr = (double[]) res;
					int xxx = resultArr.length;
					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);
						resultArrStr += resultArr[resCount] + ", ";
					}
					resultArrStr += resultArr[xxx - 1] + "}";
					_debug(resultArrStr);
					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(double[]) do not match in_sendOutput(). <ERROR>");
				}
			} else if (res instanceof Float[]) {
				_debug("IN Float[]");
				if (outPort.getType().toString().equals("{double}")) {
					Float[] resultArr = (Float[]) res;
					int xxx = resultArr.length;
					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);
						resultArrStr += resultArr[resCount] + ", ";
					}
					resultArrStr += resultArr[xxx - 1] + "}";
					_debug(resultArrStr);
					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(Float[]) do not match in _sendOutput(). <ERROR>");
				}
			}

			else if (res instanceof float[]) {
				_debug("IN float[]");
				if (outPort.getType().toString().equals("{double}")) {
					float[] resultArr = (float[]) res;
					int xxx = resultArr.length;
					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);
						resultArrStr += resultArr[resCount] + ", ";
					}
					resultArrStr += resultArr[xxx - 1] + "}";
					_debug(resultArrStr);
					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(float[]) do not match in _sendOutput(). <ERROR>");
				}
			} else if (res instanceof Boolean[]) {
				_debug("IN Boolean[]");
				if (outPort.getType().toString().equals("{boolean}")) {
					Boolean[] resultArr = (Boolean[]) res;
					int xxx = resultArr.length;
					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);
						resultArrStr += resultArr[resCount] + ", ";
					}
					resultArrStr += resultArr[xxx - 1] + "}";
					_debug(resultArrStr);
					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(Boolean[]) do not match in _sendOutput(). <ERROR>");
				}
			}

			else if (res instanceof boolean[]) {
				_debug("IN boolean[]");
				if (outPort.getType().toString().equals("{boolean}")) {
					boolean[] resultArr = (boolean[]) res;
					int xxx = resultArr.length;
					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);
						resultArrStr += resultArr[resCount] + ", ";
					}
					resultArrStr += resultArr[xxx - 1] + "}";
					_debug(resultArrStr);
					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(boolean[]) do not match in _sendOutput(). <ERROR>");
				}
			}

			else
				outPort.broadcast(new StringToken(
						"Cannot identify the type instance of the result!"));
		} catch (IllegalActionException iae) {
			_debug("<EXCEPTION> There was an exception in _sendOutput(): "
					+ iae.toString() + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_errorsStr += "\n"
					+ iae.getMessage()
					+ "There was an exception when sending the outputs in actor: "
					+ this.getName() + iae.toString();// );
		}
	} // end-of-sendOutput

	/**
	 * _setObjectArray Set the values of the arguments (_objArr) to pass to
	 * call.
	 * 
	 * @param portPtr
	 * @param index
	 */
	private void _setObjectArray(TypedIOPort portPtr, int index) {
		try {
			_debug("PORT TYPE in PTOLEMY ACTOR: "
					+ portPtr.getType().toString());
			if (portPtr.getType().toString().equals("int")) {
				_objArr[index] = new Integer(((IntToken) (portPtr.get(0)))
						.intValue());
			} else if (portPtr.getType().toString().equals("double")) {
				_objArr[index] = new Double(((DoubleToken) (portPtr.get(0)))
						.doubleValue());
			} else if (portPtr.getType().toString().equals("string")) {
				_objArr[index] = new String(((StringToken) portPtr.get(0))
						.stringValue());
			} else if (portPtr.getType().toString().equals("long")) {
				_objArr[index] = new Long(((LongToken) (portPtr.get(0)))
						.longValue());
			} else if (portPtr.getType().toString().equals("boolean")) {
				_objArr[index] = new Boolean(((BooleanToken) (portPtr.get(0)))
						.booleanValue());
			} else if (portPtr.getType().toString().equals("{int}")) {
				Token[] tempTokenArr = ((ArrayToken) (portPtr.get(0)))
						.arrayValue();
				int numTokens = tempTokenArr.length;
				Integer[] tempStrArr = new Integer[numTokens];
				for (int ind = 0; ind < numTokens; ind++)
					tempStrArr[ind] = new Integer(tempTokenArr[ind].toString());
				_objArr[index] = tempStrArr;
			} else if (portPtr.getType().toString().equals("{string}")) {
				Token[] tempTokenArr = ((ArrayToken) (portPtr.get(0)))
						.arrayValue();
				int numTokens = tempTokenArr.length;
				String[] tempStrArr = new String[numTokens];
				for (int ind = 0; ind < numTokens; ind++)
					tempStrArr[ind] = tempTokenArr[ind].toString();
				_objArr[index] = tempStrArr;
			} else if (portPtr.getType().toString().equals("{long}")) {
				Token[] tempTokenArr = ((ArrayToken) (portPtr.get(0)))
						.arrayValue();
				int numTokens = tempTokenArr.length;
				Long[] tempStrArr = new Long[numTokens];
				for (int ind = 0; ind < numTokens; ind++)
					tempStrArr[ind] = new Long(tempTokenArr[ind].toString());
				_objArr[index] = tempStrArr;
			} else if (portPtr.getType().toString().equals("{boolean}")) {
				Token[] tempTokenArr = ((ArrayToken) (portPtr.get(0)))
						.arrayValue();
				int numTokens = tempTokenArr.length;
				Boolean[] tempStrArr = new Boolean[numTokens];
				for (int ind = 0; ind < numTokens; ind++)
					tempStrArr[ind] = new Boolean(tempTokenArr[ind].toString());
				_objArr[index] = tempStrArr;
			} else if (portPtr.getType().toString().equals("{double}")) {
				Token[] tempTokenArr = ((ArrayToken) (portPtr.get(0)))
						.arrayValue();
				int numTokens = tempTokenArr.length;
				Double[] tempStrArr = new Double[numTokens];
				for (int ind = 0; ind < numTokens; ind++)
					tempStrArr[ind] = new Double(tempTokenArr[ind].toString());
				_objArr[index] = tempStrArr;
			} else {
				_debug("Could not: specify the type of the port and set object arr.");
			}
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an exception in setObjectArray method: "
					+ ex + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_errorsStr += "\n" + ex.getMessage()
					+ "\nThe error occured in the actor: " + this.getName();// );

		}
	} // end-of-setObjectArray

	/**
	 * Set the type of a port based on a string representation of that type that
	 * was extracted from the WSDL description.
	 * 
	 * @param arrayTypes
	 *            a hash of defined array types by name
	 * @param port
	 *            the port whose type is to be set
	 * @param typeStr
	 *            the string representation of the type to be set
	 */
	private void _setPortType(TypedIOPort port, String typeStr) {
		port.setTypeEquals(BaseType.XMLTOKEN);
	}

	// ////////////////////////////////////////////////////////////////////
	// // private variables
	// //

	private Binding _binding = null;
	// The main service call object
	private org.apache.axis.client.Call _call = null;
	// The name of the method that this web service actor binds to
	// static private String _methodNameStr = "";
	private String _methodNameStr = "";
	// The input values to be sent when invoking the web service call.
	private Object[] _objArr;
	// The name of the port...
	private String _portName = null;
	private int _returnMode = 0; // 1--multiple output 2--single outputparam
	private Service _service = null;
	// The URL of the WSDL that describes the web service
	// static private String _urlStr = new String();
	private String _urlStr = new String();
	private String _wsName = "";
	// The parser for the WSDL. Will be initiated by the _urlStr.
	private Parser _wsdlParser = null;
	private String _errorsStr = "";
	private String _confErrorStr = "";
	private boolean _triggerFlag = false;

	private String _WSEndpoint = null;

	private String _WSMethodName = null;

	private Document _RequestDoc = null;

} // end of WebService