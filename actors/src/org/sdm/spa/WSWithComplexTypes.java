/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-01-17 14:55:09 -0800 (Thu, 17 Jan 2013) $' 
 * '$Revision: 31346 $'
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

import java.io.IOException;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.wsdl.BindingOperation;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.ServiceException;
import javax.xml.soap.SOAPException;

import org.apache.axis.Constants;
import org.apache.axis.constants.Use;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.ElementDecl;
import org.apache.axis.wsdl.symbolTable.Parameter;
import org.apache.axis.wsdl.symbolTable.Parameters;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.apache.axis.wsdl.symbolTable.TypeEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.icon.ComponentEntityConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ptolemy.actor.IOPort;
import ptolemy.actor.IORelation;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.TypedIORelation;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.XMLToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.domains.sdf.lib.ArrayToSequence;
import ptolemy.domains.sdf.lib.SequenceToArray;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Location;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.XSLTUtilities;

//////////////////////////////////////////////////////////////////////////
//// WSWithComplexTypes
/**
 * <p>
 * 
 * This actor executes SOAP web services defined by WSDLs. Given a web service's URL
 * of a WSDL and an operation name, this actor specializes its input and output
 * ports to reflect the input and output parameters of the operation. For simple
 * web service types, e.g. string, int, or double, this actor's ports are
 * configured to the matching ptolemy type. Otherwise, the ports are set to
 * XMLTOKEN. When this actor fires, it reads each input port, invokes the web
 * service operation and sends the input data, and outputs the response to the
 * output ports.
 * 
 * </p>
 * <p>
 * 
 * The <i>inputMechanism</i> and <i>outputMechanism</i> parameters control the
 * creation of helper actors for complex/nested web service types. (These
 * parameters have no effect for simple web service types). By setting either to
 * 'composite', a composite actor is created for each parameter that is
 * complex/nested. Each composite actor is populated with necessary XML
 * Assembler or XML Disassembler actors needed to build the nested web service
 * type, and the composite actor ports are all simple ptolemy types. Changing
 * the mechanism back to 'simple' <b>deletes</b> the connected helper actors. If
 * you have made changes to the composite actors and don't want them lost,
 * disconnect them from this actor before changing the mechanism to 'simple'.
 * 
 * </p>
 * <p>
 * 
 * <b>Limitations:</b>
 * <ul>
 * <li>Unused input ports on composite actors must have the corresponding
 * internal links to XML Assembler actors removed. This is because the XML
 * Assembler actors will read a token from each input port whose width is
 * greater than 0.
 * <li>The layout of the composite actors on the canvas is not perfect;
 * sometimes a composite actor is placed over an existing actor.
 * <li>If the input to the web service contains an array of a nested type, the
 * generated composite actors will use SequenceToArray with <i>arrayLength</i>
 * defaulting to 1. For longer arrays, you must manually create them and send to
 * the appropriate XML Assembler actor.
 * <li>Web service responses containing multi-reference values (elements
 * refering to other elements for their content) are not handled.
 * <li>If the WSDL doesn't fully define the operation response, then the
 * corresponding output port is set to XMLTOKEN. You can access the values by
 * using XML Disassembler actor(s).
 * <li>A web service parameter with the WSDL type "any" sets the corresponding
 * actor port type to XMLTOKEN.
 * <li>Multidimensional arrays not handled and the corresponding port is set to
 * XMLTOKEN. You can get or set the values by using XML Assembler or
 * Disassembler actor(s).
 * </ul>
 * 
 * </p>
 * 
 * TODO: handle commit vs cancel? multiple created array conversion actors not
 * moved vertically
 * 
 * @author Daniel Crawl
 * @version $Id: WSWithComplexTypes.java 31346 2013-01-17 22:55:09Z crawl $
 */

public class WSWithComplexTypes extends TypedAtomicActor {
	/**
	 * Construct a WSWithComplexTypes source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public WSWithComplexTypes(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		wsdl = new PortParameter(this, "wsdl");
		wsdl.setStringMode(true);
		wsdl.setTypeEquals(BaseType.STRING);
		wsdl.getPort().setTypeEquals(BaseType.STRING);
		new Attribute(wsdl, "_showName");
		
		method = new StringParameter(this, "method");

		inputMechanism = new StringParameter(this, "inputMechanism");
		inputMechanism.setExpression(_ioTypes[0]);

		outputMechanism = new StringParameter(this, "outputMechanism");
		outputMechanism.setExpression(_ioTypes[0]);

		for (int i = 0; i < _ioTypes.length; i++) {
			inputMechanism.addChoice(_ioTypes[i]);
			outputMechanism.addChoice(_ioTypes[i]);
		}

		outputNil = new ptolemy.data.expr.Parameter(this, "outputNil",
				new BooleanToken(_outputNilVal));
		outputNil.setTypeEquals(BaseType.BOOLEAN);
		outputNil.setExpression("false");

		// params for the call
		username = new StringParameter(this, "username");
		password = new StringParameter(this, "password");
		timeout = new StringParameter(this, "timeout");
		timeout.setExpression("" + 600000);
		
		ignoreInvokeErrors = new ptolemy.data.expr.Parameter(this, "ignoreInvokeErrors");
		ignoreInvokeErrors.setTypeEquals(BaseType.BOOLEAN);
		ignoreInvokeErrors.setToken(BooleanToken.FALSE);
		
		hadError = new TypedIOPort(this, "hadError", false, true);
		hadError.setTypeEquals(BaseType.BOOLEAN);
		new Attribute(hadError, "_showName");

		_helper = new XMLHelper(this);
		_helper.setOutputNil(false);
		_helper.setArraysWrapped(true);

		// comparators to sort wsdl parameters and elementdecl names
		_paramComp = new ParameterComparator();
		_elDeclComp = new ElementDeclComparator();

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The web service WSDL address.
	 */
	public PortParameter wsdl = null;

	/**
	 * The web service method name to run.
	 */
	public StringParameter method = null;

	/**
	 * Setting to composite creates XML assembler and disassembler actors for
	 * complex (nested) parameters.
	 */
	public StringParameter inputMechanism = null;

	/**
	 * Setting to composite creates XML assembler and disassembler actors for
	 * complex (nested) parameters.
	 */
	public StringParameter outputMechanism = null;

	/**
	 * If true, then each output port whose name is not a child element of the
	 * incoming XML document outputs a nil token. (See XMLDisassembler).
	 */
	public ptolemy.data.expr.Parameter outputNil = null;

	/**
	 * The user name for authentication.
	 */
	public StringParameter username = null;

	/**
	 * The password for authentication.
	 */
	public StringParameter password = null;

	/**
	 * The timeout in milliseconds used by transport sender.
	 */
	public StringParameter timeout = null;
	
	/** If true, will not throw exception if error occurs invoking method. */
	public ptolemy.data.expr.Parameter ignoreInvokeErrors;

	/** Outputs true if error was ignored invoking method. */
	public TypedIOPort hadError;
	
	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * React to a change in an attribute.
	 * 
	 * @param attribute
	 *            The changed parameter.
	 * @exception IllegalActionException
	 *                If the parameter set is not valid.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		try {
			if (attribute == wsdl) {
				String str = ((StringToken)wsdl.getToken()).stringValue();
				// System.out.println("wsdl is now " + str);

				if (!str.equals(_wsdlStr)) {
					_wsdlStr = str;
					_initWSDL();
				}
			} else if (attribute == method) {
				String str = ((StringToken)method.getToken()).stringValue();
				//System.out.println(getName() + "method is now " + str);

				if (!str.equals(_methodStr)) {
					_methodStr = str;
					_initMethod();
				}
			} else if (attribute == inputMechanism) {
				String str = ((StringToken)inputMechanism.getToken()).stringValue();

				_inputIO = -1;
				for (int i = 0; i < _ioTypes.length; i++) {
					if (_ioTypes[i].equals(str)) {
						_inputIO = i;
						break;
					}
				}
				if (_inputIO == -1) {
					throw new IllegalActionException(this,
							"Invalid input mechanism: " + str);
				}

				_genInputPortsAndActors();
			} else if (attribute == outputMechanism) {
				String str = ((StringToken)outputMechanism.getToken()).stringValue();

				_outputIO = -1;
				for (int i = 0; i < _ioTypes.length; i++) {
					if (_ioTypes[i].equals(str)) {
						_outputIO = i;
						break;
					}
				}
				if (_outputIO == -1) {
					throw new IllegalActionException(this,
							"Invalid input mechanism: " + str);
				}

				_genOutputPortsAndActors();
			} else if (attribute == outputNil) {
				_outputNilVal = ((BooleanToken) outputNil.getToken())
						.booleanValue();
				_helper.setOutputNil(_outputNilVal);
			}
		} catch (NameDuplicationException e) {
			throw new IllegalActionException(this, "NameDuplicationException: "
					+ e.getMessage());
		}

		super.attributeChanged(attribute);
	}

	/**
	 * Initialize the actor by getting the input and output parameters from the
	 * soap service.
	 */
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();

		try {
			_initWSDL();
			_initMethod();
		} catch (NameDuplicationException e) {
			throw new IllegalActionException(this, "NameDuplicationException: "
					+ e.getMessage());
		}
	}

	/**
	 * Create and send the request, and send the response to the appropriate
	 * output ports.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		
		wsdl.update();
		//System.out.println(getName());

		SOAPBodyElement sbe;
		try {
		     sbe = _buildSOAPBodyRoot();
        } catch (SOAPException e) {
            throw new IllegalActionException(this, e, "Error build SOAP request.");
        }

        List<?> response = null;
        boolean ignoringError = false;
        
        try {
            //System.out.println(getFullName() + " invoking");
			response = _invokeMethod(sbe);
            //System.out.println(getFullName() + " done");
		} catch (Exception e) {
		    boolean ignoreInvokeErrorsVal = ((BooleanToken)ignoreInvokeErrors.getToken()).booleanValue();
		    if(!ignoreInvokeErrorsVal) {
		        throw new IllegalActionException(this, e, "Error invoking service.");
		    } else {
		        System.out.println("WARNING: Ignoring invocation error: " + e.getMessage());
		        ignoringError = true;
		    }
		}
		
	    if(!ignoringError) {
	        _parseResponse(response);
	        hadError.broadcast(BooleanToken.FALSE);
	    } else {
	        hadError.broadcast(BooleanToken.TRUE);
	    }

	}
	
	/** Returns false unless there are connected input ports. */
	public boolean postfire() throws IllegalActionException {
	 
	    final List<?> inputs = inputPortList();
	    if(inputs.size() > 0) {
	        for(Object object : inputs) {
	            final TypedIOPort port = (TypedIOPort) object;
	            if(port.numberOfSources() > 0) {
	                return super.postfire();
	            }
	        }
	    }
	    return false;
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/** Initialize the WSDL Parser and find the SOAP endpoint. */
	private void _initWSDL() throws IllegalActionException,
			NameDuplicationException {
		int i;

		// parse the WSDL
		try {
			_wsdlParser = new Parser();
			_wsdlParser.run(_wsdlStr);
		} catch (Exception e) {
			throw new IllegalActionException(this, "Could not parse WSDL: "
					+ e.getMessage());
		}

		// now find the entry for the ServiceEntry class in the symbol table.
		_wsdlService = null;

		HashMap<?,?> map = _wsdlParser.getSymbolTable().getHashMap();
		Iterator<?> entrySetIter = map.entrySet().iterator();
		while (entrySetIter.hasNext()) {
			Map.Entry<?,?> currentEntry = (Map.Entry<?,?>) entrySetIter.next();
			Vector<?> valueVector = (Vector<?>) currentEntry.getValue();
			for (i = 0; i < valueVector.size(); i++) {
				SymTabEntry symTabEntryObj = (SymTabEntry) valueVector.get(i);
				if ((ServiceEntry.class).isInstance(symTabEntryObj)) {
					_wsdlService = ((ServiceEntry) symTabEntryObj).getService();
					break;
				}
			}
			if (_wsdlService != null) {
				break;
			}
		}

		if (_wsdlService == null) {
			throw new IllegalActionException(this, "Could not find service");
		}

		// find a port with a SOAPAddress extensibility element.
		_wsdlPort = null;
		Map<?,?> ports = _wsdlService.getPorts();
		Iterator<?> nameIter = ports.keySet().iterator();
		while (nameIter.hasNext()) {
			String portName = (String) nameIter.next();
			Port port = (Port) ports.get(portName);
			List<?> extElemList = port.getExtensibilityElements();
			for (i = 0; extElemList != null && i < extElemList.size(); i++) {
				Object extEl = extElemList.get(i);
				if (extEl instanceof SOAPAddress) {
					_wsdlPort = port;
					break;
				}
			}
			if (_wsdlPort != null) {
				break;
			}
		}

		if (_wsdlPort == null) {
			throw new IllegalActionException(this, "Could not find port");
		}

		// now add the valid choices to the method parameter
		method.removeAllChoices();

		BindingEntry be = _wsdlParser.getSymbolTable().getBindingEntry(
				_wsdlPort.getBinding().getQName());
		Iterator<?> iter = be.getParameters().keySet().iterator();
		while (iter.hasNext()) {
			Operation oper = (Operation) iter.next();
			method.addChoice(oper.getName());
		}
	}

	/** Get the SOAP parameters for a method. */
	private void _initMethod() throws IllegalActionException,
			NameDuplicationException {
		_wsdlParams = null;

        if(_wsdlParser == null)
        {
            throw new IllegalActionException(this, "wsdl parser is null");    
        }
        else if(_wsdlPort == null)
        {
            throw new IllegalActionException(this, "wsdl port is null");
        }

		BindingEntry be = _wsdlParser.getSymbolTable().getBindingEntry(
				_wsdlPort.getBinding().getQName());
		Iterator<?> iter = be.getParameters().keySet().iterator();
		while (iter.hasNext()) {
			Operation oper = (Operation) iter.next();
			if (oper.getName().equals(_methodStr)) {
				_wsdlParams = (Parameters) be.getParameters().get(oper);
				break;
			}
		}

		if (_wsdlParams == null) {
			throw new IllegalActionException(this,
					"Could not get parameters for method " + _methodStr);
		}

		// determine the use
		_methodUse = null;
		iter = _wsdlPort.getBinding().getBindingOperations().iterator();
		while (iter.hasNext()) {
			BindingOperation bo = (BindingOperation) iter.next();
			if (bo.getName().equals(_methodStr)) {
				List<?> ext = null;
				if ((ext = bo.getBindingInput().getExtensibilityElements()) != null
						&& ext.size() > 0) {
					SOAPBody body = (SOAPBody) ext.get(0);
					_methodUse = body.getUse();
				} else if ((ext = bo.getBindingOutput()
						.getExtensibilityElements()) != null
						&& ext.size() > 0) {
					SOAPBody body = (SOAPBody) ext.get(0);
					_methodUse = body.getUse();
				}
				break;
			}
		}

		if (_methodUse == null) {
			throw new IllegalActionException(this,
					"Could not find binding operation for " + _methodStr);
		}

		// create the mapping between namespaces and types
		_nsTypeMap = createNSMapping(_wsdlParams.list);

		// NOTE: we remove unused ports here for both input and
		// output before creating the new ones since the old
		// method could have had a input parameter with the same
		// name as an output parameter in the current method
		// (or vice-versa).
		_removeUnusedInputPortsAndActors();
		_removeUnusedOutputPortsAndActors();

		_genInputPortsAndActors();
		_genOutputPortsAndActors();
	}

	/** Create input ports and actors. */
	private void _genInputPortsAndActors() throws IllegalActionException,
			NameDuplicationException {
		if (_wsdlParams != null) {
			_removeUnusedInputPortsAndActors();

			_linkedPortsMap = new LinkMap(this, true);

			// create the input ports
			Object[] paramArray = _wsdlParams.list.toArray();
			Arrays.sort(paramArray, _paramComp);
			for (int i = 0; i < paramArray.length; i++) {
				Parameter p = (Parameter) paramArray[i];
				_createOnePort(this, null, p.getName(), p.getType(), true);
			}
		}
	}

	/**
	 * Remove input ports and connected actors that don't match the input
	 * parameters of the current web service operation.
	 */
	private void _removeUnusedInputPortsAndActors()
			throws IllegalActionException, NameDuplicationException {
		int i;
		HashMap<String, String> inputNamesMap = new HashMap<String, String>();

		if (_wsdlParams != null) {
			Object[] paramArray = _wsdlParams.list.toArray();
			for (i = 0; paramArray != null && i < paramArray.length; i++) {
				Parameter p = (Parameter) paramArray[i];
				inputNamesMap.put(p.getName(), "used");
			}
		}

		// remove input ports not used by this method
		Object[] inputArray = inputPortList().toArray();
		for (i = 0; i < inputArray.length; i++) {
			IOPort p = (IOPort) inputArray[i];
			if (p != wsdl.getPort() && inputNamesMap.get(p.getName()) == null) {
				_findLinkedComposite(p, true);
				p.setContainer(null);
			}
		}
	}

	/** Create output ports and actors. */
	private void _genOutputPortsAndActors() throws IllegalActionException,
			NameDuplicationException {
		if (_wsdlParams != null) {
			_removeUnusedOutputPortsAndActors();

			// create the output port if service returns data
			if (_wsdlParams.returnParam != null) {
				/*
				 * System.out.println("return param name is " +
				 * _wsdlParams.returnParam.getName());
				 */

				_linkedPortsMap = new LinkMap(this, false);

				//try 
				//{
				_createOnePort(this, null, _wsdlParams.returnParam.getName(),
						_wsdlParams.returnParam.getType(), false);
				//}
				//catch(Throwable t) 
				//{
				//    t.printStackTrace();
				//}
			}
		}
	}

	/**
	 * Remove output ports and connected actors that don't match the output
	 * parameters of the current web service operation.
	 */
	private void _removeUnusedOutputPortsAndActors()
			throws IllegalActionException, NameDuplicationException {
		String outName = null;
		// see if there is a return parameter.
		if (_wsdlParams != null && _wsdlParams.returnParam != null) {
			outName = _wsdlParams.returnParam.getName();
		}

		Object[] outputArray = outputPortList().toArray();
		for (int i = 0; i < outputArray.length; i++) {
			IOPort p = (IOPort) outputArray[i];
			// if there is no return parameter, or the
			// current output port name isn't the same,
			// remove the port and actor.
            if (p != hadError
                    && (outName == null || !p.getName().equals(
                            _outputNamePrepend + outName))) {
                _findLinkedComposite(p, true);

				_debugMessage("removing output port " + p.getName());

				p.setContainer(null);
			}
		}
	}

	/**
	 * Convert a type string from a WSDL into the closest ptolemy type.
	 * 
	 * For XML data types see:
	 * http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/#built-in-datatypes
	 * 
	 * @param typeStr
	 *            the type string
	 * @param name
	 *            the parameter name
	 * @return the ptolemy type
	 */
	private static Type _getTypeFromWSDLStr(String typeStr, String name) {
		Type retval = null;

		if (typeStr.equals("string")) {
			retval = BaseType.STRING;
		} else if (typeStr.equals("boolean")) {
			retval = BaseType.BOOLEAN;
		} else if (typeStr.equals("double") || typeStr.equals("float")) {
			retval = BaseType.DOUBLE;
		} else if (typeStr.equals("long")
				|| typeStr.equals("unsignedLong")
				// according to the specification, these don't have
				// a max or min range so set them to be long instead
				// of int. maybe basetype.scalar would be more appropriate?
				|| typeStr.equals("integer")
				|| typeStr.equals("nonPositiveInteger")
				|| typeStr.equals("negativeInteger")
				|| typeStr.equals("nonNegativeInteger")
				|| typeStr.equals("positiveInteger")) {
			retval = BaseType.LONG;
		} else if (typeStr.equals("int") || typeStr.equals("short")
				|| typeStr.equals("byte") || typeStr.equals("unsignedInt")
				|| typeStr.equals("unsignedShort")
				|| typeStr.equals("unsignedByte")) {
			retval = BaseType.INT;
		} else if (typeStr.equals("unsignedByte")) {
			retval = BaseType.UNSIGNED_BYTE;
		} else {
			// throw new IllegalActionException(this, "Unknown type: "
			// + typeStr);
			_log.warn("WARNING: " + name + " has unknown WSDL type: " + typeStr);
		}
		return retval;
	}

	/** Convenience routine to get the input or output mechanism setting. */
	private int _getIOSetting(boolean isInput) {
		if (isInput) {
			return _inputIO;
		} else {
			return _outputIO;
		}
	}

	/**
	 * Create an input or output port for an actor, set its type, and
	 * (optionally) recursively create and link to XML assembler or disassembler
	 * helper actors.
	 * 
	 * @param beginActor
	 *            the actor on which to create the port
	 * @param myContainer
	 *            the (possibly null) container in which to link this port and
	 *            actors.
	 * @param name
	 *            the name of the port
	 * @param te
	 *            the WSDL TypeEntry
	 * @param isInput
	 *            whether this is an input port
	 * @return the number of newly created and linked actors to this port.
	 */
	private void _createOnePort(TypedAtomicActor beginActor,
			TypedCompositeActor myContainer, String name, TypeEntry te,
			boolean isInput) throws IllegalActionException,
			NameDuplicationException {
		int i;
		TypedIOPort port = null;
		String localName = _getNestedLeafName(name);
		boolean alreadyConnected = false;
		boolean isMultiArray = false;

		 _debugMessage("createOnePort for " + beginActor.getName() + " is input " +
            isInput + " port name " + name);

		// check to see if the port already exists
		if (beginActor == this) {
			// NOTE: we prepend a "> " to the output port name to prevent a
			// NameDuplicationException if an input parameter has the
			// same name.
			if (!isInput) {
				localName = _outputNamePrepend + localName;
			}

			Object[] portArray = null;
			if (isInput) {
				portArray = inputPortList().toArray();
			} else {
				portArray = outputPortList().toArray();
			}

			for (i = 0; portArray != null && i < portArray.length; i++) {
				TypedIOPort myPort = (TypedIOPort) portArray[i];

				// NOTE: if it's the output port, we need to prepend
				// outputNamePrepend.

				if (myPort.getName().equals(localName)) {
					port = myPort;
					alreadyConnected = _findLinkedComposite(port, false);
					break;
				}
			}
		}

		// create the port if we're not operating on the web service
		// actor, or we didn't find it.
		if (port == null) {
			port = _createPort(beginActor, localName, isInput, null);
			new Attribute(port, "_showName");
		}

		String wsdlTypeStr = null;
		boolean isArray = false;
		Type type = null;

		if (te.getDimensions().equals("[]")) {

			TypeEntry newTe = null;

			// determine the underlying type of the array.

			// see if single dimension
			QName qname = null;

			if ((qname = te.getComponentType()) != null) {
				wsdlTypeStr = qname.getLocalPart();
				newTe = _wsdlParser.getSymbolTable().getTypeEntry(qname, false);
				
				// if the type entry is null, get the reference type
				// (e.g. http://www.ncbi.nlm.nih.gov/entrez/eutils/soap/v2.0/eutils.wsdl,
				// run_eSearch, output type IdListType).
				if(newTe == null)
				{
				    newTe = te.getRefType();
				}
				
				_debugMessage(name + " is 1d array, wsdlTypeStr: " + wsdlTypeStr);
			} else {
				// multidimensional
				newTe = te.getRefType();
				wsdlTypeStr = newTe.getQName().getLocalPart();
				isMultiArray = true;
				_debugMessage(name + " is multi-dim array, wsdlTypeStr: " +
                    wsdlTypeStr);

			}

			wsdlTypeStr = _getNestedLeafName(wsdlTypeStr);

			te = newTe;
			isArray = true;
		} else {
			wsdlTypeStr = te.getQName().getLocalPart();
		}

		// see if it's a primitive type
		if (te.isSimpleType() || te.isBaseType()) {
			if (te.isSimpleType()) {
				ElementDecl sub = (ElementDecl) te.getContainedElements()
						.get(0);
				wsdlTypeStr = sub.getType().getQName().getLocalPart();
			}

			type = _getTypeFromWSDLStr(wsdlTypeStr, localName);

			// see if type was unknown
			if (type == null) {
				type = BaseType.XMLTOKEN;
			}

			_debugMessage("noncomplex port type " + te.getQName().getLocalPart());

			if (isArray) {
				port.setTypeEquals(new ArrayType(type));
			} else {
				port.setTypeEquals(type);
			}

			// see if we are creating a port inside a composite actor
			// for nested types.
			if (myContainer != null) {
				_addPortForComposite(myContainer, name, port, true, myContainer);
			}

			// set the port type in the moml
			_updateVergilType(beginActor, localName, port.getType());
		} 
		else // is a nested type
		{
		    _debugMessage("has nested port type");

			if (isArray) {
			    //_debugMessage("create " + name + " -> " + typeStr);
				type = new ArrayType(BaseType.XMLTOKEN);
			} else {
				type = BaseType.XMLTOKEN;
			}

			port.setTypeEquals(type);
			_updateVergilType(beginActor, localName, type);

			if (!alreadyConnected && _getIOSetting(isInput) == IO_COMPOSITE) {
				if (beginActor == this) {
					CompositeEntity top = (CompositeEntity) getContainer();
					String compNameStr = _genNestedName(getName(), name);

					_debugMessage("creating composite " + compNameStr);

					// create the composite actor
					myContainer = new TypedCompositeActor(top, compNameStr);
					_updateVergilLocation(beginActor, myContainer, isInput);

					// create the connection between the web service actor
					// and the composite actor
					port = _addPortForComposite(myContainer, name, port, false,
							top);
				}

				// bail out since we can't handle these
				if (isMultiArray) {

				    _debugMessage("is multiArray");

					// add the xml port to the composite actor
					/*_addPortForComposite(myContainer, name, port, true,
							myContainer);
					return;
                    */
				}

				_debugMessage("creating actor for port " + name);

				if (isArray) {
					// create a new relation from beginActor to convActor
					TypedIORelation convRel = new TypedIORelation(myContainer,
							myContainer.uniqueName(localName));
					port.link(convRel);

					// create the conversion actor
					TypedAtomicActor convActor = null;
					if (isInput) {
						convActor = new SequenceToArray(myContainer,
								myContainer.uniqueName("SequenceToArray"));

						// link the conversion actor
						convActor.getPort("output").link(convRel);
						// shift the port to the input of the conversion actor
						port = (TypedIOPort) convActor.getPort("input");

					} else {
						convActor = new ArrayToSequence(myContainer,
								myContainer.uniqueName("ArrayToSequence"));

						((ArrayToSequence) convActor).enforceArrayLength
								.setToken(new BooleanToken(false));
						// link the conversion actor
						convActor.getPort("input").link(convRel);
						// shift the port to the output of the conversion actor
						port = (TypedIOPort) convActor.getPort("output");
					}

					_updateVergilLocation(beginActor, convActor, isInput);

					// shift the pointers down so that we operate on the
					// conversion actor.
					beginActor = convActor;
					type = BaseType.XMLTOKEN;
					localName = wsdlTypeStr;
				}

				TypedAtomicActor xmlActor = null;
				if (isInput) {
				    _debugMessage("creating assembler for " + name);
					xmlActor = new XMLAssembler(myContainer, name);
				} else {
				    _debugMessage("creating disassembler for " + name);

					XMLDisassembler dis = new XMLDisassembler(myContainer, name);

					// set the disassembler's output nil value based on ours.
					dis.outputNil.setToken(new BooleanToken(_outputNilVal));
					dis.arraysWrapped.setToken(new BooleanToken(true));

					xmlActor = dis;
				}

				_debugMessage("createOnePort for " + xmlActor.getName() + " is input " + !isInput + " port name " + name);

				// remove the prepended output name.
				if (beginActor == this && !isInput) {
					localName = localName
							.substring(_outputNamePrepend.length());
				}

				TypedIOPort xmlPort = _createPort(xmlActor, localName,
						!isInput, type);

				_updateVergilLocation(beginActor, xmlActor, isInput);

				_debugMessage(beginActor.getName() + " -> " + xmlActor.getName());

				String relName = myContainer.uniqueName("rel "
						+ xmlActor.getName());
				//_debugMessage("creating relation " + relName);

				TypedIORelation relation = new TypedIORelation(myContainer,
						relName);

				// System.out.println("linking to "
				// + beginActor.getName() + " : " + port.getName());
				// System.out.println("and to "
				// + xmlActor.getName() + " : " + xmlPort.getName());

				port.link(relation);
				xmlPort.link(relation);

				_debugMessage("going to recurse down nested types for " + name);

				// recursive on nested types
				List<?> parts = te.getContainedElements();
                
                // see if there are any parts
                if(parts == null)
                {
                    // check for reference type
                    te = te.getRefType();
                    if(te != null)
                    {
                        // get parts of reference type
                        parts = te.getContainedElements();
                    }
                }

				if (parts != null) {
					Object[] partsArray = parts.toArray();
					Arrays.sort(partsArray, _elDeclComp);

					_debugMessage("has " + partsArray.length + " contained elements");

					for (i = 0; i < partsArray.length; i++) {
						ElementDecl decl = (ElementDecl) partsArray[i];

						String nestedName = _genNestedName(name, decl
								.getQName().getLocalPart());

						_debugMessage("nested name " + nestedName);

						_createOnePort(xmlActor, myContainer, nestedName, decl
								.getType(), isInput);
					}
				}
                else
                {
                    _debugMessage("WARNING: no contained elements!");
                    _debugMessage("    name  = " + name);
                    _debugMessage("    type  = " + te);
                }
			}
		}
		 
		_debugMessage("createOnePort end for " + beginActor.getName() + " is input " +
            isInput + " port name " + name);

	}

	/**
	 * See if a port is connected to a helper composite actor. A match is found
	 * if the actor is a TypedCompositeActor, and the name of the actor is a
	 * nested name of the web service actor name plus the port name. If a match
	 * is found, portWillBeDeleted is true, and the IO setting is simple, the
	 * matching composite actor, and all its contents, is deleted.
	 * 
	 * @param port
	 *            the port
	 * @param portWillBeDeleted
	 *            if the port will soon be deleted
	 * @return if a composite actor found
	 */
	private boolean _findLinkedComposite(IOPort port, boolean portWillBeDeleted)
			throws IllegalActionException, NameDuplicationException {
		boolean retval = false;
		TypedCompositeActor composite = null;
		String portName = port.getName();

		if (port.isOutput() && portName.startsWith(_outputNamePrepend)) {
			portName = portName.substring(_outputNamePrepend.length());
		}
		String nestedName = _genNestedName(getName(), portName);

		List<?> list = port.connectedPortList();
		if (list != null) {
			Iterator<?> conPorts = list.iterator();
			while (conPorts.hasNext()) {
				IOPort p = (IOPort) conPorts.next();
				NamedObj owner = p.getContainer();
				if (owner instanceof TypedCompositeActor
						&& owner.getName().equals(nestedName)) {
					composite = (TypedCompositeActor) owner;
					retval = true;
				}
			}
		}

		// if the composite actor was found and the port will be
		// deleted soon or the input/output mechanism is simple, delete it.
		if (retval
				&& (portWillBeDeleted || _getIOSetting(port.isInput()) == IO_SIMPLE)) {
			composite.removeAllRelations();
			composite.removeAllEntities();
			composite.removeAllPorts();
			composite.setContainer(null);
		}
		
		// if the composite actor was not found and the i/o mechanism
		// is composite, unlink any relations
		if(!retval && _getIOSetting(port.isInput()) == IO_COMPOSITE &&
		    ((TypedIOPort)port).getType() == BaseType.XMLTOKEN) {
		    List<?> relations = port.linkedRelationList();
		    for(Object object : relations)
		    {
		        if(object != null)
		        {
		            ((IORelation)object).setContainer(null);
		        }
		    }
		    //port.unlinkAll();
		}

		return retval;
	}

	/**
	 * Create a port for a composite actor and link it to an existing port. The
	 * parameter compEnt determines whether the link is internal or external.
	 * 
	 * @param compActor
	 *            the composite actor
	 * @param fullName
	 *            port name
	 * @param port
	 *            the port to link to
	 * @param sameDirection
	 *            if the new port is same direction as port
	 * @param compEnt
	 *            the container in which to create the relation
	 * @return the create port
	 */
	private TypedIOPort _addPortForComposite(TypedCompositeActor compActor,
			String fullName, TypedIOPort port, boolean sameDirection,
			CompositeEntity compEnt) throws IllegalActionException,
			NameDuplicationException {
		boolean isInput = port.isInput();

		// see if we need to reverse direction
		// XXX is always opposite direction if external link,
		// so do we need sameDirection?
		if (!sameDirection) {
			isInput = !isInput;
		}

		String msg = "creating port " + fullName + " for composite '" +
		    compActor.getName() + "' input: " + isInput;
		_debugMessage(msg);
				
		// create a port for the container
		String portName = _getNestedChildName(fullName);
		
		// see if port already exists with same name
		List<IOPort> portList = compActor.portList();
		for(IOPort curPort : portList)
		{
		    if(curPort.getName().equals(portName))
		    {
		        portName = "_" + portName;
		        break;
		    }
		}
		
		TypedIOPort conPort = _createPort(compActor, portName, isInput, port
				.getType());

		// show the name unless connected to web service actor
		if (compActor == compEnt) {
			new Attribute(conPort, "_showName");
		}

		// link the inside port to the outside one
		TypedIORelation rel = new TypedIORelation(compEnt, compEnt
				.uniqueName(fullName));
		port.link(rel);
		conPort.link(rel);

		_updateVergilLocation(port.getContainer(), conPort, isInput);

		return conPort;
	}

	/**
	 * Convenience routine to create a port for an actor, set the direction, and
	 * optionally the type.
	 * 
	 * @param ent
	 *            the actor for which to create the port
	 * @param name
	 *            the name of the port
	 * @param isInput
	 *            if this is an input port
	 * @param type
	 *            an optional type to set the port
	 * @return the port
	 */
	private TypedIOPort _createPort(ComponentEntity ent, String name,
			boolean isInput, Type type) throws IllegalActionException,
			NameDuplicationException {
		 
	    _debugMessage("creating port " + name + " for " + ent.getFullName());

		TypedIOPort retval = (TypedIOPort) ent.newPort(name);

		// set direction
		retval.setInput(isInput);
		retval.setOutput(!isInput);

		// if type exists, set and save in the moml
		if (type != null) {
			retval.setTypeEquals(type);
			_updateVergilType(ent, name, type);
		}

		return retval;
	}

	/**
	 * Update vergil and the MoML with type information.
	 * 
	 * @param context
	 * @param portName
	 *            the name of the port
	 * @param portTypeStr
	 *            the type
	 */
	private void _updateVergilType(NamedObj context, String portName, Type type) {
		// System.out.println("updating vergil for port " + portName);

		String str = "<group>" + "<port name =\"" + portName + "\">"
				+ "<property name=\"_type\""
				+ "class = \"ptolemy.actor.TypeAttribute\" value = \""
				+ type.toString() + "\"/>" + "</port>" + "</group>";

		MoMLChangeRequest request = new MoMLChangeRequest(this, context, str);
		request.setPersistent(true);
		requestChange(request);
	}

	/**
	 * Change the location and draw the appropriate icon for NamedObj.
	 * 
	 * @param old
	 *            the reference NamedObj
	 * @param cur
	 *            the NamedObj being placed
	 * @param toLeft
	 *            whether the NamedObj being placed is to the left of the
	 *            reference NamedObj.
	 */
	private void _updateVergilLocation(NamedObj old, NamedObj cur,
			boolean toLeft) throws IllegalActionException,
			NameDuplicationException {

		/*
		 * System.out.println("setting location for " + cur.getName()); if(cur
		 * instanceof TypedIOPort) System.out.println("is port"); else if(cur
		 * instanceof TypedAtomicActor) System.out.println("is atomic actor");
		 * else if(cur instanceof TypedCompositeActor)
		 * System.out.println("is comp actor"); else
		 * System.out.println("ERROR: don't know type");
		 */

		Location loc = (Location) old.getAttribute("_location");
		double[] coords = loc.getLocation();
		double[] curCoords = new double[coords.length];

		if (toLeft) {
			curCoords[0] = coords[0] - X_INC;
		} else {
			curCoords[0] = coords[0] + X_INC;
		}

		int n = 0;
		// only get and update the next port number for old
		// if cur is a composite actor or old is not the web service
		// actor. i.e. do NOT update the port number when cur
		// is connected to the web service actor and is an atomic
		// actor or port.
		if ((cur instanceof TypedCompositeActor) || old != this) {
			n = _linkedPortsMap.getNextPortNumber(old, toLeft);
		}

		curCoords[1] = coords[1] + (n * Y_INC);

		// System.out.println("loc n for " + old.getName() + " is " + n);

		Location curLoc = new Location(cur, "_location");
		curLoc.setLocation(curCoords);

		// add draw the icon for the actor.
		try {
			ComponentEntityConfig.addSVGIconTo(cur);
		} catch (IOException e) {
			throw new IllegalActionException(this, "IOException: "
					+ e.getMessage());
		}
	}

	/**
	 * Build a SOAPBodyElement based on the WSDL and method by reading the data
	 * from input ports.
	 * 
	 * @return the SOAPBodyElement
	 */
	private SOAPBodyElement _buildSOAPBodyRoot() throws SOAPException,
			IllegalActionException {
		SOAPBodyElement retval = null;

		String targetNS = _wsdlParser.getSymbolTable().getDefinition()
				.getTargetNamespace();

		retval = new SOAPBodyElement(XMLUtils.StringToElement(targetNS,
				_methodStr, ""));

		// add all the namespace types to the body
		Iterator<String> iter = _nsTypeMap.keySet().iterator();
		while (iter.hasNext()) {
			String ns = iter.next();
			String nsType = _nsTypeMap.get(ns);
			if (!nsType.equals("xsd") && !nsType.equals("xsi")) {
				retval.addNamespaceDeclaration(nsType, ns);
			}
		}

		// add encoding style attribute if encoded
		// "soapenv:encodingStyle" ->
		// "http://schemas.xmlsoap.org/soap/encoding/"
		if (_methodUse.equals(Use.ENCODED_STR)) {
			retval.setAttribute(Constants.NS_PREFIX_SOAP_ENV + ":"
					+ Constants.ATTR_ENCODING_STYLE, Constants.URI_SOAP11_ENC);
		}

		Object[] inputs = inputPortList().toArray();

		// add an element for each of the method's input parameters
		for (int i = 0; i < _wsdlParams.list.size(); i++) {
			Parameter p = (Parameter) _wsdlParams.list.get(i);

			// find the input port with the same name
			TypedIOPort port = null;
			for (int j = 0; j < inputs.length; j++) {
				TypedIOPort tmp = (TypedIOPort) inputs[j];
				if (tmp.getName().equals(p.getName())) {
					// System.out.println("found input port match " +
					// p.getName());
					port = tmp;
					break;
				}
			}

			// make sure we found it
			if (port == null) {
				throw new IllegalActionException(this, "Missing input port "
						+ p.getName());
			}

			// if port is not connected, do not read from it.
			if(port.numberOfSources() == 0) {
			    continue;
			}
			
			// consume a token from the input port and add it to the
			// SOAPBodyElement
			if (port.getType() == BaseType.XMLTOKEN) {
				XMLToken token = (XMLToken) port.get(0);
				Element root = token.getDomTree().getDocumentElement();
				retval.addChildElement(_buildSOAPBody(root, p.getName(), p
						.getType(), p.isNillable(), p.getQName()
						.getNamespaceURI()));
			} else {
				String value = null;
				if (port.getType() == BaseType.STRING) {
					value = ((StringToken) port.get(0)).stringValue();
				} else {
					value = port.get(0).toString();
				}

				SOAPBodyElement child = new SOAPBodyElement(XMLUtils
						.StringToElement("", p.getName(), ""));
				child.setValue(value);

				_addTypeOrNamespace(child, p.getQName().getNamespaceURI(), p
						.getType().getQName().getLocalPart());

				retval.addChildElement(child);
			}
		}

		return retval;
	}

	/**
	 * Depending on document style, either add the namespace or type attribute.
	 * 
	 * @param el
	 *            the element to modify
	 * @param namespace
	 *            the namespace
	 * @param typeStr
	 *            the type string
	 */
	private void _addTypeOrNamespace(SOAPBodyElement el, String namespace,
			String typeStr) {
		if (_methodUse.equals(Use.LITERAL_STR)) {
			el.setNamespaceURI(namespace);
		} else if (_methodUse.equals(Use.ENCODED_STR)) {
			String nsForType = _nsTypeMap.get(namespace);
			if (nsForType != null) {
				el.setAttribute("xsi:type", nsForType + ":" + typeStr);
			}
		}
	}

	/**
	 * Recursively create a SOAPBodyElement for a namespace based on an XML
	 * Element.
	 * 
	 * @param root
	 *            the XML Element containing the data
	 * @param name
	 *            the name
	 * @param te
	 *            the SOAP TypeEntry
	 * @param nillable
	 *            whether it can be nil
	 * @param parentNS
	 *            the parent XML namespace
	 * @return the SOAPBodyElement
	 */
	private SOAPBodyElement _buildSOAPBody(Element root, String name,
			TypeEntry te, boolean nillable, String parentNS)
			throws SOAPException, IllegalActionException {
		SOAPBodyElement retval = new SOAPBodyElement(XMLUtils.StringToElement(
				parentNS, name, ""));

		if (root.hasAttribute("nil")) {
			// System.out.println("is nil");

			if (!nillable) {
				throw new IllegalActionException(this, "Must supply " + name);
			} else {
				retval.setAttribute("xsi:nil", "true");
			}
		} else if (!te.isSimpleType() && !te.isBaseType()) {
			// System.out.println("is complex");

			Vector<?> parts = te.getContainedElements();
			if(parts != null) {
    			for (int i = 0; i < parts.size(); i++) {
    				ElementDecl decl = (ElementDecl) parts.get(i);
    				String declName = _getDeclName(decl);
    
    				NodeList nl = root.getElementsByTagName(declName);
    
    				if (nl.getLength() == 0) {
    					System.out.println("WARNING: missing: " + name);
    				} else {
    					if (nl.getLength() > 1) {
    						// xxx array?
    					    for(int j = 0; j < nl.getLength(); j++) {
    	                       retval.addChildElement(_buildSOAPBody((Element) nl
    	                                .item(j), declName, decl.getType(), decl
    	                                .getNillable(), decl.getQName()
    	                                .getNamespaceURI()));
    					    }
    					} else {
    						// System.out.println("found " + nl.item(0));
    						retval.addChildElement(_buildSOAPBody((Element) nl
    								.item(0), declName, decl.getType(), decl
    								.getNillable(), decl.getQName()
    								.getNamespaceURI()));
    					}
    				}
    			}
			} else if(te.isReferenced()) {
			    TypeEntry newTe = te.getRefType();
			    return _buildSOAPBody(root, name, newTe, nillable, parentNS);   
			}
		} else {
			retval.setValue(root.getChildNodes().item(0).getNodeValue());
			_addTypeOrNamespace(retval, parentNS, te.getQName().getLocalPart());
		}
		return retval;
	}

	/**
	 * Create a mapping from namespaces to namespace numbers.
	 * 
	 * @param inputs
	 * @return the mapping
	 */
	private Map<String, String> createNSMapping(List<?> inputs) {
		Map<String, String> retval = new HashMap<String, String>();
		int curNS = 2;

		// add the defaults
		retval.put("http://www.w3.org/2001/XMLSchema", "xsd");
		retval.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");

		// add target name space
		retval.put(_wsdlParser.getSymbolTable().getDefinition()
				.getTargetNamespace(), "ns1");

		Iterator<?> iter = inputs.iterator();
		while (iter.hasNext()) {
			Parameter p = (Parameter) iter.next();
			TypeEntry te = p.getType();
			curNS = addNSForTypeEntry(te, curNS, retval);
		}

		return retval;
	}

	/**
	 * Recursively create namespace to namespace numbers mapping.
	 * 
	 * @param te
	 *            the type
	 * @param curNS
	 *            the last used namespace number
	 * @param mapping
	 *            the mapping
	 * @return the last used namespace number
	 */
	private int addNSForTypeEntry(TypeEntry te, int curNS, Map<String, String> mapping) {
		String ns = te.getQName().getNamespaceURI();
		if (!mapping.containsKey(ns)) {
			curNS++;
			mapping.put(ns, "ns" + curNS);
			// System.out.println("adding " + ns + " = ns" + curNS);
		}

		List<?> parts = te.getContainedElements();
		for (int i = 0; parts != null && i < parts.size(); i++) {
			ElementDecl ed = (ElementDecl) parts.get(i);
			curNS = addNSForTypeEntry(ed.getType(), curNS, mapping);
		}
		return curNS;
	}

	/** Convenience routine to get the name from an ElementDecl. */
	private String _getDeclName(ElementDecl decl) {
		return _getNestedLeafName(decl.getQName().getLocalPart());
	}

	/**
	 * Convenience routine to get a leaf name from a fully nested name. e.g.
	 * "foo>bar>blah" returns "blah".
	 */
	private String _getNestedLeafName(String name) {
		String retval = name;
		int index;
		if ((index = name.lastIndexOf(">")) != -1) {
			retval = name.substring(index + 1);
		}
		return retval;
	}

	/**
	 * Convenience routine to get a nested child name from a fully nested name.
	 * e.g. "foo>bar>blah" returns "bar>blah".
	 */
	private String _getNestedChildName(String name) {
		String retval = name;
		int index;
		if ((index = name.indexOf(">")) != -1) {
			retval = name.substring(index + 1);
		}
		return retval;
	}

	/** Convenience routine to generate a nested name. */
	private String _genNestedName(String parent, String child) {
		return parent + ">" + _getNestedLeafName(child);
	}

	/**
	 * Invoke the WSDL operation and return any results.
	 * 
	 * @param sbe
	 *            a SOAPBodyElement containing data for input parameters
	 * @return the result, if any
	 */
	private List<?> _invokeMethod(SOAPBodyElement sbe)
			throws IllegalActionException, ServiceException, RemoteException,
			SAXException {
		org.apache.axis.client.Service svcClient = new org.apache.axis.client.Service(
				_wsdlParser, _wsdlService.getQName());

		Call call = svcClient.createCall(QName.valueOf(_wsdlPort.getName()),
				QName.valueOf(_methodStr));

		// set username, password, timeout if not empty.

		String str = username.stringValue();
		if (!str.equals("")) {
			call.setProperty(Call.USERNAME_PROPERTY, str);
		}

		str = password.stringValue();
		if (!str.equals("")) {
			call.setProperty(Call.PASSWORD_PROPERTY, str);
		}

		str = timeout.stringValue();
		if (!str.equals("")) {
			// we can do this cast since we used an axis service
			// to create the call
			((org.apache.axis.client.Call) call).setTimeout(new Integer(str));
		}

        if(_isDebugging || _debugging)
        {
            // attempt to print the request xml
            _debugMessage("Web Service REQUEST:");
            StringWriter writer = new StringWriter();
            XMLUtils.PrettyElementToWriter(sbe, writer);
            _debugMessage("\n" + writer.toString());
        }

		// invoke the call and return the results
		return (List<?>) call.invoke(new Object[] { sbe });
	}

	/** Send the response from the web service to the output ports. */
	private void _parseResponse(List<?> response) throws IllegalActionException {
		if (response != null) {
			// is top level always $operation + "response" ?
			MessageElement msg = (MessageElement) response.get(0);

			Document doc = null;
			try {
				doc = msg.getAsDocument();

                if(_isDebugging)
                {
                    _debugMessage("response:");
                    _debugMessage(XSLTUtilities.toString(doc));
                }

			} catch (Exception e) {
				throw new IllegalActionException(this, "Exception: "
						+ e.getMessage());
			}

			
			// remove hadError output port from set of ports to generate XML output for
			List<?> outPorts = new LinkedList<Object>(outputPortList());
			outPorts.remove(hadError);
			
			_helper.splitOutXML(_methodStr + "Response", doc, outPorts,
					_outputNamePrepend);
		}
	}

	private void _debugMessage(String msg)
	{
	    _log.debug(msg);
	    _debug(msg);
	    //System.out.println(msg);
	}
	
	/**
	 * A Comparator class for org.apache.axis.wsdl.symbolTable.Parameter names.
	 */
	private class ParameterComparator implements Comparator {
		public ParameterComparator() {
		}

		public int compare(Object o1, Object o2) {
			int retval = 0;

			if (!(o1 instanceof Parameter)) {
				retval = -1;
			} else if (!(o2 instanceof Parameter)) {
				retval = 1;
			} else {
				Parameter p1 = (Parameter) o1;
				Parameter p2 = (Parameter) o2;
				retval = p1.getName().compareTo(p2.getName());
			}
			return retval;
		}
	}

	/**
	 * A Comparator class for org.apache.axis.wsdl.symbolTable.ElementDecl
	 * names.
	 */
	private class ElementDeclComparator implements Comparator {
		public ElementDeclComparator() {
		}

		public int compare(Object o1, Object o2) {
			int retval = 0;
			if (!(o1 instanceof ElementDecl)) {
				retval = -1;
			} else if (!(o2 instanceof ElementDecl)) {
				retval = 1;
			} else {
				String e1 = _getDeclName((ElementDecl) o1);
				String e2 = _getDeclName((ElementDecl) o2);
				retval = e1.compareTo(e2);
			}
			return retval;
		}
	}

	/**
	 * This class is used to aid in placing objects on the canvas. It keeps
	 * track of how many connections we've made to an actor.
	 */
	private class LinkMap {
		/**
		 * Count the number of connections our web service actor already
		 * contains.
		 */
		public LinkMap(TypedAtomicActor actor, boolean forInput) {
			_map = new HashMap<NamedObj, Integer>();

			Object[] portArray = null;
			if (forInput) {
				portArray = actor.inputPortList().toArray();
			} else {
				portArray = actor.outputPortList().toArray();
			}

			int total = -1;
			for (int i = 0; i < portArray.length; i++) {
				total += ((ptolemy.kernel.Port) portArray[i]).numLinks();
			}
			_map.put(actor, new Integer(total));
			// System.out.println("adding " + total + " for " +
			// actor.getName());
		}

		/**
		 * This is called when making another connection; get the number (if
		 * any) and increment.
		 */
		public int getNextPortNumber(NamedObj no, boolean isInput) {
			int retval = 0;
			Integer val;
			if ((val = _map.get(no)) != null) {
				retval = val.intValue() + 1;
			}
			_map.put(no, new Integer(retval));
			return retval;
		}

		private Map<NamedObj, Integer> _map;
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	// the current web service WSDL URL
	private String _wsdlStr = "";

	// the current web service operation name
	private String _methodStr = "";

	private Parser _wsdlParser = null;
	private Service _wsdlService = null;
	private Port _wsdlPort = null;
	private Parameters _wsdlParams = null;
	private String _methodUse = null;
	private Map<String, String> _nsTypeMap = null;

	// whether to output nil values
	private boolean _outputNilVal = false;

	// used to split the response to corresponding ports
	private XMLHelper _helper = null;

	// comparators to sort wsdl parameters and elementdecl names
	private ParameterComparator _paramComp = null;
	private ElementDeclComparator _elDeclComp = null;

	// coordinate increments that control the spacing on the canvas
	// of newly created actors.
	private static final int X_INC = 150;
	private static final int Y_INC = 90;

	// i/o mechanism types
	private static final String[] _ioTypes = { "simple", "composite" };
	private static final int IO_SIMPLE = 0;
	private static final int IO_COMPOSITE = 1;

	// the current i/o mechanism settings
	private int _inputIO = -1;
	private int _outputIO = -1;

	// string that is prepended to output port (if any) to avoid
	// having identically named input and output port (not allowed in ptolemy).
	private static final String _outputNamePrepend = "> ";

	private LinkMap _linkedPortsMap;

    private static final Log _log = LogFactory.getLog(WSWithComplexTypes.class.getName());
    private static final boolean _isDebugging = _log.isDebugEnabled();
}
