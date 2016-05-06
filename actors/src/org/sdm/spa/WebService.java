/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-05-09 11:05:40 -0700 (Wed, 09 May 2012) $' 
 * '$Revision: 29823 $'
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.ServiceException;

import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.Parameters;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.apache.axis.wsdl.symbolTable.SymbolTable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.ParameterPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Relation;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////WebService
/**
 * <p>
 * The WebService actor provides the user with a plug-in interface to execute
 * any WSDL-defined web service. Given a URL for the WSDL of a web service and
 * an operation name that is included in the WSDL, this actor customizes itself
 * to execute this web service operation.
 * </p>
 * <p>
 * WSDL is an XML format for describing network services as a set of endpoints
 * operating on messages containing either document-oriented or procedure-
 * oriented information. The operations and messages are described abstractly,
 * and then bound to a concrete network protocol and message format to define an
 * endpoint. Related concrete endpoints are combined into abstract endpoints
 * (services). WSDL is extensible to allow description of endpoints and their
 * messages regardless of what message formats or network protocols are used to
 * communicate. More information on WSDL and realted standard can be found at:
 * http://www.w3.org/TR/wsdl
 * </p>
 * <p>
 * The user can instantiate the generic web service actor by providing the WSDL
 * URL and choosing the desired web service operation. The actor then a
 * utomatically specializes itself and adds ports with the inputs and outputs as
 * described by the WSDL. The so instantiated actor acts as a proxy for the web
 * service being executed and links to the other actors through its ports.
 * </p>
 * <p>
 * The WSDL is parsed to get the input, output and binding information. It
 * dynamically generates ports for each input and output of the operation. This
 * customization happens at the configuration time of a model. When the actor is
 * fired at run time, it gets the binding information and creates a call object
 * to run the model. Using this call object, it invokes the web service and
 * broadcasts the response to the output ports.
 * </p>
 * <p>
 * <i><b>Notices to users:</b></i>
 * <ul>
 * <li>Please double-click on the actor to start customization.</li>
 * <li>To enter a WSDL URL which is not in the given list of WSDL URLs, click on
 * the "Preferences" button on the configuration interface and change the type
 * of the parameter to "Text". Then you can type in the WSDL you would like to
 * use.</li>
 * <li>After you select the WSDL, "Commit" and double-click on the actor again.
 * This will reconfigure the list of available operations. Please do this
 * everytime you change the WSDL URL.</li>
 * </ul>
 * </i>
 * </p>
 * 
 * @author Ilkay Altintas, Updates by: Karen L. Schuchardt, Zhiming Zhao,
 *         Daniel Crawl
 * @version $Id: WebService.java 29823 2012-05-09 18:05:40Z crawl $
 */

public class WebService extends TypedAtomicActor {

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
	 *                If the container already has an actor with this name.
	 */
	public WebService(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		wsdlUrl = new StringParameter(this, "wsdlUrl");
		// wsdlUrl.setExpression("http://xml.nig.ac.jp/wsdl/DDBJ.wsdl");
		methodName = new StringParameter(this, "methodName");
		userName = new StringParameter(this, "userName");
		password = new StringParameter(this, "password");
		timeout = new StringParameter(this, "timeout");
		timeout.setExpression("" + 600000);

		startTrigger = new TypedIOPort(this, "startTrigger", true, false);
		// new Attribute(startTrigger, "_showName");
		// startTrigger.setContainer(null);
		hide = new SingletonParameter(startTrigger, "_hide");
		hide.setToken(BooleanToken.TRUE);
		// Set the trigger Flag.
		hasTrigger = new Parameter(this, "hasTrigger", new BooleanToken(false));
		hasTrigger.setTypeEquals(BaseType.BOOLEAN);

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
	 * The timeout duration in web service call.
	 */
	public StringParameter timeout;
	/**
	 * This is an parameter to activate the optional startTrigger port. Please
	 * activate it <i>ONLY</i> when the actor has no input and it is required
	 * for scheduling of the actor.
	 */
	public Parameter hasTrigger;
	/**
	 * This is an optional input port that can be used to help the scheduling of
	 * the actor.
	 * 
	 * <p>
	 * This port is activated by the hasTrigger parameter. Double-click on the
	 * actor to enable. Please enable it <i>ONLY</i> when the actor has no input
	 * and it is required for scheduling of the actor.
	 * </p>
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
       
        if(_isDebugging) {
            _log.debug("attribute changed: " + at);
        }

		if (at == hasTrigger) {
			_triggerFlag = ((BooleanToken) hasTrigger.getToken())
					.booleanValue();
			_debug("<TRIGGER_FLAG>" + _triggerFlag + "</TRIGGER_FLAG>");
			if (_triggerFlag) {
				hide.setToken(BooleanToken.FALSE);
			} else {
				List<?> inPortList = inputPortList();
				Iterator<?> ports = inPortList.iterator();
				while (ports.hasNext()) {
					IOPort p = (IOPort) ports.next();
					if (p.isInput()) {
						if (p.getName().equals("startTrigger")) {
							// p.setContainer(null);
							hide.setToken(BooleanToken.TRUE);
						}
					}
				}
			}
		} else if (at == wsdlUrl) {
		    
		    String newUrlStr = ((StringToken)wsdlUrl.getToken()).stringValue();
		    
		    if(!_urlStr.equals(newUrlStr))
		    {
		        _urlStr = newUrlStr;
				
		        methodName.removeAllChoices();
				//_deletePorts();
				_wsdlParser = null;
				
				if(_urlStr.length() > 0)
				{
    				_wsdlParser = new Parser();
    				// Parse the wsdl for the web service.
    				try
    				{
    				    _wsdlParser.run(_urlStr);
    				}
                    catch(Exception e)
                    {
                        throw new IllegalActionException(this, e,
                                "Could not run WSDL parser.");
                    }
                    
    				_configureOperationNames();		
    				
                    if(_methodNameStr.length() > 0)
                    {
                        _configureActor();
                    }
				}
		    }
			
		} else if (at == methodName) {
	    		
			String newMethodStr = ((StringToken)methodName.getToken()).stringValue();
			
			if(!_methodNameStr.equals(newMethodStr))
			{
			    //_deletePorts();
                _methodNameStr = newMethodStr;                
                _debug("<METHOD_NAME>" + _methodNameStr + "</METHOD_NAME>");


				if(_urlStr.length() > 0)
				{
    				int slashIndex = _urlStr.lastIndexOf('/');
    				String wsName = _urlStr.substring(slashIndex + 1,
    						_urlStr.length() - 5);
    
    				_attachText(
    						"_iconDescription",
    						"<svg>\n"
    								+ "<rect x=\"0\" y=\"0\" "
    								+ "width=\"160\" height=\"50\" "
    								+ "style=\"fill:white\"/>\n"
    								+ "<text x=\"20\" y=\"25\" "
    								+ "style=\"font-size:11; fill:red; font-family:SansSerif\">"
    								+ wsName + "_" + _methodNameStr + "</text>\n"
    								+ "</svg>\n");
    
    
    				if(_methodNameStr.length() > 0 && _urlStr.length() > 0)
    				{
    				    _configureActor();
    				}
				}
            }
		}
	} // end of attributeChanged

	/** Configure the actor for the entered operation of the given web service. */
	private void _configureOperationNames() throws IllegalActionException {
		
	    _service = null;

		// Find the entry for the ServiceEntry class in the symbol table.
		HashMap map = _wsdlParser.getSymbolTable().getHashMap();
		Iterator<?> entrySetIter = map.entrySet().iterator();
		while (entrySetIter.hasNext()) {
			final Map.Entry currentEntry = (Map.Entry) entrySetIter.next();
			final Vector valueVector = (Vector) currentEntry.getValue();
			int vecSize = valueVector.size();
			for (int index = 0; index < vecSize; ++index) {
				SymTabEntry symTabEntryObj = (SymTabEntry) valueVector
						.get(index);
				if ((ServiceEntry.class).isInstance(symTabEntryObj)) {
					_service = ((ServiceEntry) symTabEntryObj).getService();
				}
			}
		}

		if(_service == null)
		{
		    String str = _urlStr;
		    _urlStr = "";
		    throw new IllegalActionException(this, "Unable to find service entry for " + str);
		}
		
		Port port = _getSOAPAddress(_service.getPorts());
		if (port == null) {
			_log.debug("<ERROR> No port was returned by the _getSOAPAddress. </ERROR>");

		}
		_portName = "";
		_portName = port.getName();
		_binding = port.getBinding();
		SymbolTable symbolTable = _wsdlParser.getSymbolTable();
		BindingEntry bEntry = symbolTable.getBindingEntry(_binding
				.getQName());

		Iterator<?> iter = bEntry.getParameters().keySet().iterator();
		for (; iter.hasNext();) {
			final Operation oper = (Operation) iter.next();
			methodName.addChoice(oper.getName());
		}
	} // end-of-configureOperationNames

	/** Configure the actor for the entered operation of the given web service. */
	private void _configureActor() throws IllegalActionException {
	    
	    if(_service == null)
	    {
	        throw new IllegalActionException(this, "No service entry.");
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

		boolean foundOperation = false;
		Iterator<?> iter = bEntry.getParameters().keySet().iterator();
		for (; iter.hasNext();) {
			Operation operation = (Operation) iter.next();
			if (operation.getName().equals(_methodNameStr)) {
				foundOperation = true;
				final Parameters parameters = (Parameters) bEntry.getParameters().get(operation);
				
				_inputNameSet.clear();
				_inputNameSet.add("startTrigger");
				
				_outputNameSet.clear();
				_outputNameSet.add("clientExecErrors");
				
				//System.out.println("cleared name sets.");
				
				try
				{
				    _createPorts(parameters);
				}
				catch(NameDuplicationException e)
				{
				    throw new IllegalActionException(this, e,
				            "Error creating ports.");
				}

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

						/*
						 * Original code String baseTypeStr =
						 * _getArrayBaseType(arrTypeNode);
						 */
						/* This code updated by Zhiming Zhao */
						String baseTypeStr = (String) ((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
								.getType().getRefType().getQName()
								.getLocalPart()
								+ "[]";
						QName nodeQname = ((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
								.getType().getRefType().getQName();

						/* End */

						_debug("ARRAY PARAM BASE TYPE: " + baseTypeStr);

						try {
						    _createPort(
								((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
										.getMode(), baseTypeStr,
								(String) returnQName.getLocalPart());
						} catch(NameDuplicationException e) {
		                    throw new IllegalActionException(this, e,
		                            "Error creating ports.");
		                }
					} else {
					    try {
					        _createPort(
								((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
										.getMode(),
								((org.apache.axis.wsdl.symbolTable.Parameter) parameters.returnParam)
										.getType().getQName()
										.getLocalPart(),
								(String) returnQName.getLocalPart());
					    } catch(NameDuplicationException e) {
                            throw new IllegalActionException(this, e,
                                    "Error creating ports.");
                        }
					}
					_debug("<RETURN_QNAME>" + returnQName.getLocalPart()
							+ "</RETURN_QNAME>");
				}
				// Break out of the loop
				break;
			}
		}
		
		if(!foundOperation)
		{
		    String str = _methodNameStr;
		    _methodNameStr = "";
		    throw new IllegalActionException(this, "Could not find operation " + str);
		}
		
		// remove unused ports
		for(Object obj: inputPortList())
		{
		    IOPort curPort = (IOPort)obj;
		    
		    if(!_inputNameSet.contains(curPort.getName()))
		    {
		        //System.out.println("old input: " + curPort.getName());
		        try
		        {
    		        if(curPort instanceof ParameterPort)
    		        {
    		            ((ParameterPort)curPort).getParameter().setContainer(null);
    		        }
    		        
    		        curPort.setContainer(null);
		        }
		        catch(NameDuplicationException e)
		        {
		            throw new IllegalActionException(this, e, "Error removing " + curPort.getName());
		        }
		    }
		}
		
		for(Object obj: outputPortList())
		{
		    IOPort curPort = (IOPort)obj;
		    if(!_outputNameSet.contains(curPort.getName()))
		    {
		        //System.out.println("old output: " + curPort.getName());
		        try
		        {
		            curPort.setContainer(null);
		        }
                catch(NameDuplicationException e)
                {
                    throw new IllegalActionException(this, e, "Error removing " + curPort.getName());
                }
		    }
		}
		
		
	} // end-of-configureActor

	
	/** Creates ports for the web service operation */
	private void _createPorts(Parameters params) 
	    throws IllegalActionException, NameDuplicationException {
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
				/*
				 * String baseTypeStr =
				 * arrTypeNode.getFirstChild().getFirstChild().
				 * getFirstChild
				 * ().getAttributes().getNamedItem("wsdl:arrayType").
				 * getNodeValue(); String[] result = baseTypeStr.split(":");
				 * baseTypeStr = result[1];
				 */
				/* Updated by Zhiming */
				String baseTypeStr = param.getType().getRefType()
						.getQName().getLocalPart()
						+ "[]";
				/* Updated by Zhiming End */

				_debug("ARRAY PARAM BASE TYPE: " + baseTypeStr);
				_createPort(param.getMode(), baseTypeStr, (String) param
						.getQName().getLocalPart());
			} else { // if (param.getType().getDimension() != null) {
				_createPort(param.getMode(), param.getType().getQName()
						.getLocalPart(), (String) param.getQName()
						.getLocalPart());
			}
		}
	}

	/** Create a port if it does not already exist, and set its type. */
	private void _createPort(int mode, String portTypeStr, String portNameStr) 
	    throws IllegalActionException, NameDuplicationException {
	    
	    TypedIOPort port = null;
	    
	    // see if port already exists.
	    boolean exists = false;
	    port = (TypedIOPort) getPort(portNameStr);
	    if(port != null)
	    {
	        if(mode == 3 || (port.isInput() && mode == 1) ||
	            (port.isOutput() && mode == 2))
	        {
	            exists = true;
	        }
	    }
	   
	    // create port if necessary
		if (mode == 1) { // input
		    if(!exists) {
		        PortParameter parameter = new PortParameter(this, portNameStr);
		        port = parameter.getPort();
		        _debug("<INPUT>" + portNameStr + "</INPUT>");   
		    }
		} else if (mode == 2) { // output
		    if(!exists) {
		        port = new TypedIOPort(this, portNameStr, false, true);
		        _debug("<OUTPUT>" + portNameStr + "</OUTPUT>");
		    }
		} else if (mode == 3) { // input/output
		    // XXX is this possible? can't have two ports w/ same name!
		    if(!exists) {
		        TypedIOPort pin = new TypedIOPort(this, portNameStr, true, false);
		        new Attribute(pin, "_showName");
		        _setPortType(pin, portTypeStr);
		        _debug("<INPUT>" + portNameStr + "</INPUT>");
		        port = new TypedIOPort(this, portNameStr, false, true);
		        _debug("<OUTPUT>" + portNameStr + "</OUTPUT>");
		    }
		}	
		
		if(!exists) {
		    // show port name
		    new Attribute(port, "_showName");
		}
		
        // set type.
        _setPortType(port, portTypeStr);
        
        if(mode == 1 || mode == 3) {
            _inputNameSet.add(portNameStr);
        } 
        
        if(mode == 2 || mode == 3) {
            _outputNameSet.add(portNameStr);
        }

	} // end-of-createPort

	/** Deletes all the ports of this actor. */
	/*
	protected void _deletePorts() throws IllegalActionException {
		List<?> inPortList = inputPortList();
		Iterator<?> ports = inPortList.iterator();
		while (ports.hasNext()) {
			IOPort port = (IOPort) ports.next();
			try {
				if (!(port.getName().equals("startTrigger"))) {
				    if(port instanceof ParameterPort) {
				        ((ParameterPort)port).getParameter().setContainer(null);
				    }
				    
					port.setContainer(null);
				}
			} catch (NameDuplicationException e) {
			    throw new IllegalActionException(this, e, 
			            "Could not delete the input port: " + port.getName());
			}
		}

		List<?> outPortList = outputPortList();
		Iterator<?> oports = outPortList.iterator();
		while (oports.hasNext()) {
			IOPort port = (IOPort) oports.next();
			try {
				if (!(port.getName().equals("clientExecErrors"))) {
				    port.setContainer(null);
				}
			} catch (NameDuplicationException e) {
				throw new IllegalActionException(this, e, 
				        "Could not delete the output port: " + port.getName());
			}
		}
	} // end of deletePorts
	*/

	/**
	 * Get the URL address of the the location of the web service defined by the
	 * given WSDL. Get the the namespace and binding information of the web
	 * service using the given WSDL and methodName. Add a parameter to the call
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

		_urlStr = ((StringToken)wsdlUrl.getToken()).stringValue();
		_methodNameStr = ((StringToken)methodName.getToken()).stringValue();

		_wsdlParser = new Parser();

		try {
			_wsdlParser.run(new String(_urlStr));
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error while parsing the WSDL in fire for URL: "
					+ _urlStr + " .\n" + ex + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_errorsStr += "\n"
					+ ex.getMessage()
					+ "Error in fire: There was an error while parsing the WSDL in the actor: "
					+ this.getName();// );

			/*
			 * The following exception is thrown and for the case when the web
			 * service's server is down. The director SDF4WS catches the
			 * exception thrown below and re-tries to get web service
			 * access.After three re-trials the director finally switches over
			 * to the same service but at a different server (if second server
			 * is available)
			 */

			GraphicalMessageHandler.message("\nWebService WSDL:" + _urlStr
					+ " Not Responding");
			throw new IllegalActionException(
					"\nWebService WSDL Not Responding.");

		}
		_getServiceBinding();
		try {
			SymbolTable symbolTable = _wsdlParser.getSymbolTable();
			BindingEntry bEntry = symbolTable.getBindingEntry(_binding
					.getQName());
			Parameters parameters = null;
			Iterator<?> iter = bEntry.getParameters().keySet().iterator();
			for (; iter.hasNext();) {
				final Operation operation = (Operation) iter.next();
				if (operation.getName().equals(_methodNameStr)) {
					parameters = (Parameters) bEntry.getParameters().get(operation);
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
			org.apache.axis.client.Service myServiceClient = new org.apache.axis.client.Service(
					_wsdlParser, _service.getQName());
			_call = myServiceClient.createCall(QName.valueOf(_portName), QName
					.valueOf(_methodNameStr));
			_debug(_call.getClass().getName() + "Call implementation");

			// KLS((org.apache.axis.client.Call) _call).setTimeout(new
			// Integer(600000));
			((org.apache.axis.client.Call) _call).setTimeout(new Integer(
					timeout.stringValue()));

			// Add a parameter to the call object for each input part.
			List<?> inPortList = inputPortList();
			int numInPorts = inPortList.size();
			// _debug("<TRIGGER_FLAG_BEFORE_OBJECT_ARR>" + _triggerFlag +
			// "</TRIGGER_FLAG_BEFORE_OBJECT_ARR>");
			// if (_triggerFlag) {
			_objArr = new Object[numInPorts - 1];
			// }
			// else {
			// _objArr = new Object[numInPorts];
			// }
			Iterator<?> ports = inPortList.iterator();
			int i = 0;
			while (ports.hasNext()) {
				// Fill these parameters with the input values on channels on
				// the
				// ports that correspond to them.
				TypedIOPort p = (TypedIOPort) ports.next();
				_debug("<INPUT_PORT>" + p.getName() + "</INPUT_PORT>");
				if (p.getName().equals("startTrigger")) {
					_debug("Skipped the value of the trigger port in fire.");
				} else {
				    
				    PortParameter input = ((ParameterPort)p).getParameter();
				    input.update();
				    _objArr[i] = _getObjectFromToken(input.getToken());
				    i++;
				}
			}
			for (int j = 0; j < i; j++) {
				_debug("_objArr[" + j + "]=" + _objArr[j]);
			}

			_debug("<OBJ_ARR_LENGTH> "
					+ (new Integer(_objArr.length)).toString()
					+ " </OBJ_ARR_LENGTH>");
			_debug("<USERNAME> " + userName.stringValue() + " </USERNAME>");
			_call.setProperty(Call.USERNAME_PROPERTY, userName.stringValue());
			_debug("<PASSWORD> " + password.stringValue() + " </PASSWORD>");
			_call.setProperty(Call.PASSWORD_PROPERTY, password.stringValue());

			_debug("Starting the invoke!");
			// Element invokeResult = (Element) call.invoke(objArr);
			Object invokeResult = _call.invoke(_objArr);
			_debug("Got results from the invoke...");
			List<?> outPortList = this.outputPortList();
			Iterator<?> oports = outPortList.iterator();
			_debug("<RETURN_MODE> " + new Integer(_returnMode).toString()
					+ " </RETURN_MODE>");
			if (_returnMode == 2) {
				while (oports.hasNext()) {
					TypedIOPort po = (TypedIOPort) oports.next();
					if (!(po.getName().equals("clientExecErrors"))) {
						_sendOutput(po, invokeResult);
					}
				}
			} else if (_returnMode == 1) {
				Map outParams = _call.getOutputParams();
				while (oports.hasNext()) {
					// Fill these parameters with the input values on
					// channels on the ports that correspond to them.
					TypedIOPort po = (TypedIOPort) oports.next();
					_debug("<OUTPUT_PORT>" + po.getName() + ", "
							+ po.getType().toString() + "</OUTPUT_PORT>");
					try {
						if (!(po.getName().equals("clientExecErrors"))) {
							_sendOutput(po, outParams.get(new QName("", po
									.getName())));
						}
					} catch (Exception ex) {
						_debug("<EXCEPTION> There was an exception when sending the outputs."
								+ " OutValue: "
								+ (String) org.apache.axis.utils.JavaUtils
										.convert(outParams.get(new QName("", po
												.getName())),
												java.lang.String.class)
								+ ". </EXCEPTION>");
						// GraphicalMessageHandler.message(
						_errorsStr += "\n"
								+ ex.getMessage()
								+ "\nThe error occured in the actor: "
								+ this.getName()
								+ "\n Please look at the debugging details for this actor for "
								+ "more information.";// );
					}
				}
			}
		} catch (ServiceException se) {
		    throw new IllegalActionException(this, se, "Service error.");
		} catch (java.rmi.RemoteException rex) {
		    throw new IllegalActionException(this, rex, "Web service error.");
		}

		System.out.println(_errorsStr);
		if (!(_errorsStr.equals(""))) {
			clientExecErrors.broadcast(new StringToken(_errorsStr));
		} else {
			clientExecErrors.broadcast(new StringToken("NO ERRORS."));
		}
	} // end of fire

	/** Configure the service, port and binding info. */
	private void _getServiceBinding() throws IllegalActionException {
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
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error when configuring the actor: "
					+ ex + ". </EXCEPTION>");
			throw new IllegalActionException(this, ex, "There was an error configuring the actor.");
		}
	} // end-of-getServiceBinding

	
	public void preinitialize() throws IllegalActionException
	{
	    // convert existing input ports that are not port parameters
	    // to port parameters.
	    
	    Set<IOPort> portsToChange = new HashSet<IOPort>();
	    
	    List<?> inputList = inputPortList();
	    for(Object obj: inputList)
	    {
	        IOPort port = (IOPort)obj;
	        if(! port.getName().equals("startTrigger") &&
	            !(port instanceof ParameterPort))
	        {
	            portsToChange.add(port);
	        }
	    }
	    
	    for(IOPort port : portsToChange)
	    {
	        // get any connections to this port
	        List<?> relationList = port.linkedRelationList();
	        String name = port.getName();
	        try 
	        {
                port.setContainer(null);
            }
	        catch (NameDuplicationException e)
            {
	            throw new IllegalActionException(this, e, "Error removing old port " + name);
            }
	        
	        PortParameter parameter;
            try 
            {
                parameter = new PortParameter(this, name);
            }
            catch (NameDuplicationException e)
            {
                throw new IllegalActionException(this, e, "Error creating port parameter " + name);
            }
	        
            port = parameter.getPort();
	        for(Object obj : relationList)
	        {
	            port.link((Relation)obj);
	        }
	        
	    }
	    
	    
	    // NOTE: call parent's preinitialize last since the ports may have
	    // changed.
	    super.preinitialize();
	}

	// ////////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Returns a port with a SOAPAddress extensibility element.
	 */
	private Port _getSOAPAddress(Map ports) {
		Iterator<?> nameIter = ports.keySet().iterator();
		while (nameIter.hasNext()) {
			String portName = (String) nameIter.next();
			Port port = (Port) ports.get(portName);
			List<?> extElemList = port.getExtensibilityElements();
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
							+ ") and the res type(Integer) do not match in _sendOutput(). <ERROR>");
				}
			} else if (res instanceof Double) {
				if (outPort.getType().toString().equals("double")) {
					outPort.broadcast(new DoubleToken(((Double) res)
							.doubleValue()));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(Double) do not match in _sendOutput(). <ERROR>");
				}
			} else if (res instanceof Long) {
				if (outPort.getType().toString().equals("long")) {
					outPort.broadcast(new LongToken(((Long) res).longValue()));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(Long) do not match in _sendOutput(). <ERROR>");
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

					/*
					 * Original code String resultArrStr = "{"; for (int
					 * resCount = 0; resCount < xxx - 1; resCount++) {
					 * _debug("resultArr[" + resCount + "] = " +
					 * resultArr[resCount]); resultArrStr += resultArr[resCount]
					 * + ", ";
					 * 
					 * } resultArrStr += resultArr[xxx - 1] + "}";
					 */
					/* code updated by zhiming */

					String resultArrStr = "{";
					for (int resCount = 0; resCount < xxx - 1; resCount++) {
						_debug("resultArr[" + resCount + "] = "
								+ resultArr[resCount]);

						/*
						 * Check if '"' is included in the string array.
						 */
						if (((resultArr[resCount].getBytes())[0] == (resultArr[resCount]
								.getBytes())[resultArr[resCount].length() - 1])
								&& (resultArr[resCount].getBytes()[0] == 34)) {
							resultArrStr += resultArr[resCount] + ",";
							// System.out.println("Without adding \"");;

						} else {
							resultArrStr += "\"" + resultArr[resCount] + "\",";
							// System.out.println("add \"");;
						}
					}
					if (((resultArr[xxx - 1].getBytes())[0] == (resultArr[xxx - 1]
							.getBytes())[resultArr[xxx - 1].length() - 1])
							&& ((resultArr[xxx - 1].getBytes())[0] == 34)) {
						resultArrStr += resultArr[xxx - 1] + "}";
						// System.out.println("Without adding \"");;
					} else {
						resultArrStr += "\"" + resultArr[xxx - 1] + "\"}";
						// System.out.println("add \"");;
					}
					// System.out.println("stringArrayB: "+resultArrStr);;
					/* End Zhiming */

					outPort.broadcast(new ArrayToken(resultArrStr));
				} else {
					_debug("<ERROR> The outPort type ("
							+ outPort.getType().toString()
							+ ") and the res type(String[]) do not match in _sendOutput(). <ERROR>");
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
							+ ") and the res type(String[]) do not match in _sendOutput(). <ERROR>");
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
							+ ") and the res type(String[]) do not match in _sendOutput(). <ERROR>");
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
							+ ") and the res type(Double[]) do not match in _sendOutput(). <ERROR>");
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
							+ ") and the res type(double[]) do not match in _sendOutput(). <ERROR>");
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

			else if (res instanceof byte[]) {
				_debug("IN byte[]");
				if (outPort.getType().toString().equals("{unsignedByte}")) {
					byte[] resultArr = (byte[]) res;
					int bytesAvailable = resultArr.length;
					Token[] dataTokens = new Token[bytesAvailable];

					for (int j = 0; j < bytesAvailable; j++) {
						dataTokens[j] = new UnsignedByteToken(resultArr[j]);
					}

					outPort.broadcast(new ArrayToken(dataTokens));
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

	/** Get the native object from a Token. */
	private Object _getObjectFromToken(Token token) {
			if(token instanceof DoubleToken) {
			    return ((DoubleToken)token).doubleValue();
			} else if(token instanceof IntToken) {
			    return ((IntToken)token).intValue();
			} else if (token instanceof StringToken) {
			    return ((StringToken)token).stringValue();
			} else if (token instanceof LongToken) {
			    return ((LongToken)token).longValue();
			} else if (token instanceof BooleanToken) {
			    return ((BooleanToken)token).booleanValue();
			} else if(token instanceof ArrayToken) {
			    ArrayToken arrayToken = (ArrayToken)token;
			    Object[] arrayObj = new Object[arrayToken.length()];
			    for(int i = 0; i < arrayToken.length(); i++) {
			        arrayObj[i] = _getObjectFromToken(arrayToken.getElement(i));
			    }
			    return arrayObj;
			} else {
			    System.out.println("WARNING: unknown type of token: " + token.getType());
			    return token.toString();
			}
		}

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
		/*
		 * NOTE TO SELF: I used:
		 * http://www-106.ibm.com/developerworks/webservices
		 * /library/ws-soapmap1/ as reference for the Apache->SOAP type mapping.
		 * Haven't got to the special "object encoding" and "Sending blobs"
		 * parts of the doc. Need to consider them seperately if we wanna do it.
		 */
		if (typeStr.equals("int")) {
			port.setTypeEquals(BaseType.INT);
		} else if (typeStr.equals("boolean")) {
			port.setTypeEquals(BaseType.BOOLEAN);
		} else if (typeStr.equals("long")) {
			port.setTypeEquals(BaseType.LONG);
		} else if (typeStr.equals("double")) {
			port.setTypeEquals(BaseType.DOUBLE);
		} else if (typeStr.equals("float")) { // There is no float in Ptolemy
												// type sys.
			port.setTypeEquals(BaseType.DOUBLE);
		} else if (typeStr.equals("byte")) {
			// ->There is no byte in Ptolemy type sys. So I cast the byte to
			// INT.
			port.setTypeEquals(BaseType.INT);
		} else if (typeStr.equals("short")) {
			// ->There is no short in Ptolemy type sys. So again cast it to INT
			port.setTypeEquals(BaseType.INT);
		} else if (typeStr.equals("string")) {
			port.setTypeEquals(BaseType.STRING);
		} else if (typeStr.equals("string[]")) {
			port.setTypeEquals(new ArrayType(BaseType.STRING));
		} else if (typeStr.equals("byte[]")) {
			port.setTypeEquals(new ArrayType(BaseType.INT));
		} else if (typeStr.equals("short[]")) {
			port.setTypeEquals(new ArrayType(BaseType.INT));
		} else if (typeStr.equals("int[]")) {
			port.setTypeEquals(new ArrayType(BaseType.INT));
		} else if (typeStr.equals("long[]")) {
			port.setTypeEquals(new ArrayType(BaseType.LONG));
		} else if (typeStr.equals("double[]")) {
			port.setTypeEquals(new ArrayType(BaseType.DOUBLE));
		} else if (typeStr.equals("float[]")) {
			port.setTypeEquals(new ArrayType(BaseType.DOUBLE));
		} else if (typeStr.equals("boolean[]")) {
			port.setTypeEquals(new ArrayType(BaseType.BOOLEAN));
		} else {
			_debug("<WARNING>Could not specify the type. Setting it to string. </WARNING>");
			port.setTypeEquals(BaseType.STRING);
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////

	private Binding _binding = null;
	// The main service call object
	private Call _call = null;
	// The name of the method that this web service actor binds to
	// static private String _methodNameStr = "";
	private String _methodNameStr = "";
	// The input values to be sent when invoking the web service call.
	private Object[] _objArr;
	// The name of the port...
	private String _portName = null;
	private int _returnMode = 0; // 1--multiple output 2--single output param
	private Service _service = null;
	// The URL of the WSDL that describes the web service
	// static private String _urlStr = new String();
	private String _urlStr = new String();
	// The parser for the WSDL. Will be initiated by the _urlStr.
	private Parser _wsdlParser = null;
	private String _errorsStr = "";
	private boolean _triggerFlag = false;

    private static final Log _log = LogFactory.getLog(WebService.class.getName());
    private static final boolean _isDebugging = _log.isDebugEnabled();
    
    private Set<String> _inputNameSet = new HashSet<String>();
    private Set<String> _outputNameSet = new HashSet<String>();
    

} // end of WebService