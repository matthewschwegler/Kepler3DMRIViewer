/*
 * Copyright (c) 2000-2010 The Regents of the University of California.
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

import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.Parameters;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.apache.axis.wsdl.symbolTable.SymbolTable;

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
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;

//////////////////////////////////////////////////////////////////////////
//// WebServiceStub
/**
 * This is a stub actor that executes a web service, given a wsdl url and
 * operation name. This actor will act as the java stub for the specific
 * operation of a web service.
 * 
 * @author Ilkay Altintas
 * @version $Id: WebServiceStub.java 30702 2012-09-18 18:40:51Z crawl $
 * @category.name web service
 * @category.name distributed
 * @category.name remote
 * @deprecated Use org.sdm.spa.WebService or org.sdm.spa.WSWithComplexTypes instead
 */

public class WebServiceStub extends TypedAtomicActor {

	/**
	 * Construct a WebServiceStub actor with the given container and name.
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
	public WebServiceStub(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		wsdlUrl = new StringParameter(this, "wsdlUrl");
		methodName = new StringParameter(this, "methodName");
		userName = new StringParameter(this, "userName");
		password = new StringParameter(this, "password");

		startTrigger = new TypedIOPort(this, "startTrigger", true, false);
		new Attribute(startTrigger, "_showName");
		startTrigger.setContainer(null);
		// Set the trigger Flag.
		hasTrigger = new Parameter(this, "hasTrigger", new BooleanToken(false));
		hasTrigger.setTypeEquals(BaseType.BOOLEAN);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	} // end-of-constructor

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	// The parameter for the URL of the web service WSDL.
	public StringParameter wsdlUrl;
	// The parameter for the method name.
	// FIX ME: I want to make the methodName parameter style as "choice style".
	// The choices will be automatically updated when the wsdl changes.
	// Right now it works but doesn't load them until you open and close the
	// parameter configuration interface.
	public StringParameter methodName;
	// The userName and password to invoke the web service if necessary.
	public StringParameter userName;
	public StringParameter password;

	public Parameter hasTrigger;
	public TypedIOPort startTrigger;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Callback for changes in attribute values. This one is just used for the
	 * trigger port.
	 * 
	 * @param a
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute at) throws IllegalActionException {
		if (at == hasTrigger) {
			_triggerFlag = ((BooleanToken) hasTrigger.getToken())
					.booleanValue();
			_debug("<TRIGGER_FLAG>" + _triggerFlag + "</TRIGGER_FLAG>");
			if (_triggerFlag) {
				try {
					startTrigger.setContainer(this);
				} catch (NameDuplicationException ndex) {
					_debug("111: " + ndex.getMessage());
					GraphicalMessageHandler.message(ndex.getMessage()
							+ "Could not create the trigger port in actor:"
							+ this.getName());
				}
			} else {
				List inPortList = this.inputPortList();
				Iterator ports = inPortList.iterator();
				while (ports.hasNext()) {
					IOPort p = (IOPort) ports.next();
					if (p.isInput()) {
						try {
							if (p.getName().equals("startTrigger")) {
								p.setContainer(null);
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
		}
	} // end-of-attributeChanged

	/**
	 * Given a WebServiceStub, outputs if that actor is same with the current
	 * one.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof WebServiceStub))
			return false;
		try {
			WebServiceStub in = (WebServiceStub) obj;
			if ((this.wsdlUrl.stringValue().equals(in.wsdlUrl.stringValue()))
					&& (this.methodName.stringValue().equals(in.methodName
							.stringValue()))) {
				return true;
			}
		} catch (Exception ex) {
			// Do nothing. It will go out if this.
			// PRINT THIS LATER.
		}

		// System.out.println("IN OBJ WSDLURL: " + in.wsdlUrl.stringValue());
		// System.out.println("THIS WSDLURL: " + this.wsdlUrl.stringValue());

		// System.out.println("IN OBJ METHODNAME: " +
		// in.methodName.stringValue());
		// System.out.println("THIS METHODNAME: " +
		// this.methodName.stringValue());
		return false;
	}

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
		_urlStr = wsdlUrl.getExpression();
		_methodNameStr = methodName.getExpression();
		try {
			_wsdlParser.run(_urlStr);
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error while parsing the WSDL in fire. "
					+ ex + ". </EXCEPTION>");
			GraphicalMessageHandler
					.message(ex.getMessage()
							+ "Error in fire: There was an error while parsing the WSDL in the actor: "
							+ this.getName());
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
			GraphicalMessageHandler
					.message(ex.getMessage()
							+ "There was an error when setting up the return mode of the web "
							+ "service at: " + this.getName());
		}

		try {
			org.apache.axis.client.Service myServiceClient = new org.apache.axis.client.Service(
					_wsdlParser, _service.getQName());
			_debug("Method name in fire: " + _methodNameStr);
			_call = myServiceClient.createCall(QName.valueOf(_portName), QName
					.valueOf(_methodNameStr));
			_debug(_call.getClass().getName() + "Call implementation");

			((org.apache.axis.client.Call) _call)
					.setTimeout(new Integer(600000));

			// Add a parameter to the call object for each input part.
			List inPortList = this.inputPortList();
			int numInPorts = inPortList.size();
			if (_triggerFlag) {
				_objArr = new Object[numInPorts - 1];
			} else {
				_objArr = new Object[numInPorts];
			}
			Iterator ports = inPortList.iterator();
			int i = 0;
			while (ports.hasNext()) {
				// Fill these parameters with the input values on channels on
				// the
				// ports that correspond to them.
				TypedIOPort p = (TypedIOPort) ports.next();
				_debug("<INPUT_PORT>" + p.getName() + "</INPUT_PORT>");
				if (p.getName().equals("startTrigger")) {
					_debug("Skipped the value of the trigger port in fire.");
				} else if (p.hasToken(0)) {
					_setObjectArray(p, i);
					i++;
				}
			}
			for (int j = 0; j < i; j++) {
				_debug("_objArr[" + j + "]=" + _objArr[j]);
			}

			_debug("<USERNAME> " + userName.stringValue() + " </USERNAME>");
			_call.setProperty(Call.USERNAME_PROPERTY, userName.stringValue());
			_debug("<PASSWORD> " + password.stringValue() + " </PASSWORD>");
			_call.setProperty(Call.PASSWORD_PROPERTY, password.stringValue());

			_debug("Starting the invoke!");
			// Element invokeResult = (Element) call.invoke(objArr);
			Object invokeResult = _call.invoke(_objArr);
			_debug("Got results from the invoke...");
			List outPortList = this.outputPortList();
			Iterator oports = outPortList.iterator();
			_debug("<RETURN_MODE> " + new Integer(_returnMode).toString()
					+ " </RETURN_MODE>");
			if (_returnMode == 2) {
				TypedIOPort po = (TypedIOPort) oports.next();
				_sendOutput(po, invokeResult);
			} else if (_returnMode == 1) {
				Map outParams = _call.getOutputParams();
				while (oports.hasNext()) {
					// Fill these parameters with the input values on
					// channels on the ports that correspond to them.
					TypedIOPort po = (TypedIOPort) oports.next();
					_debug("<OUTPUT_PORT>" + po.getName() + ", "
							+ po.getType().toString() + "</OUTPUT_PORT>");
					try {
						_sendOutput(po, outParams.get(new QName("", po
								.getName())));
					} catch (Exception ex) {
						_debug("<EXCEPTION> There was an exception when sending the outputs."
								+ " OutValue: "
								+ (String) org.apache.axis.utils.JavaUtils
										.convert(outParams.get(new QName("", po
												.getName())),
												java.lang.String.class)
								+ ". </EXCEPTION>");
						GraphicalMessageHandler
								.message(ex.getMessage()
										+ "\nThe error occured in the actor: "
										+ this.getName()
										+ "\n Please look at the debugging details for this actor for "
										+ "more information.");
					}
				}
			}
		} catch (ServiceException se) {
			_debug("<EXCEPTION> Service exception in fire() method: "
					+ se.getMessage() + ". </EXCEPTION>");
			GraphicalMessageHandler.message(se.getMessage()
					+ "\nThe service exception error occured in the actor: "
					+ this.getName());
		} catch (java.rmi.RemoteException rex) {
			_debug("<EXCEPTION> Remote exception in fire() method: "
					+ rex.getMessage() + ". </EXCEPTION>");
			// get rmi.getCause() and print it here.
			GraphicalMessageHandler.message(rex.getMessage()
					+ "\nThe remote exception error occured in the actor: "
					+ this.getName());

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
				_debug("<ERROR> No port was returned by the _getSOAPAddress. </ERROR>");
			}
			_portName = "";
			_portName = port.getName();
			_binding = port.getBinding();
		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error when configuring the actor: "
					+ ex + ". </EXCEPTION>");
			GraphicalMessageHandler.message(ex.getMessage()
					+ "There was an error when configuring the actor:"
					+ this.getName());
		}
	} // end-of-getServiceBinding

	// ////////////////////////////////////////////////////////////////////
	// //////////// private methods ////////////////

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

			else
				outPort.broadcast(new StringToken(
						"Cannot identify the type instance of the result!"));
		} catch (IllegalActionException iae) {
			_debug("<EXCEPTION> There was an exception in _sendOutput(): "
					+ iae.toString() + ". </EXCEPTION>");
			GraphicalMessageHandler
					.message(iae.getMessage()
							+ "There was an exception when sending the outputs in actor: "
							+ this.getName() + iae.toString());
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
			GraphicalMessageHandler.message(ex.getMessage()
					+ "\nThe error occured in the actor: " + this.getName());

		}
	} // end-of-setObjectArray

	// ////////////////////////////////////////////////////////////////////
	// // private variables ////

	private Binding _binding = null;
	// The main service call object
	private Call _call = null;
	// The name of the method that this web service actor binds to
	private String _methodNameStr = "";
	// The input values to be sent when invoking the web service call.
	private Object[] _objArr;
	// The name of the port...
	private String _portName = null;
	private int _returnMode = 0; // 1--multiple output 2--single output param
	private Service _service = null;
	// The URL of the WSDL that describes the web service
	private String _urlStr = "";
	private String _wsName = "";
	// The parser for the WSDL. Will be initiated by the _urlStr.
	private Parser _wsdlParser = new Parser();

	private boolean _triggerFlag = false;

} // end of WebServiceStub