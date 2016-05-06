/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:19:36 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31113 $'
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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.Token;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.BaseType.BooleanType;
import ptolemy.data.type.BaseType.DoubleType;
import ptolemy.data.type.BaseType.IntType;
import ptolemy.data.type.BaseType.LongType;
import ptolemy.data.type.BaseType.StringType;
import ptolemy.data.type.BaseType.UnsignedByteType;
import ptolemy.data.type.Type;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

///////////////////////////////////////////////////////////////
////SoaplabChooseOperation
/**
 * <p>
 * The following actor is for choosing set_&lt;name&gt; operations for executing
 * any derived web service registered at EBI. The following actor takes in an
 * input values to serve as parameters for the set_&lt;name&gt; web service
 * operation. The actor asks the user to choose the desired set_&lt;name&gt;
 * operation. The inputValues and set_&lt;name&gt; operation name are further
 * forwarded to the next actor SoaplabServiceStarter.
 * </p>
 * 
 * @author Nandita Mangal
 * @version $Id: SoaplabChooseOperation.java, v 1.0 2005/19/07
 * @category.name web
 * @category.name external execution
 */

public class SoaplabChooseOperation extends TypedAtomicActor {

	/**
	 * Construct a SoaplabChooseOperation actor with given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name
	 */

	public SoaplabChooseOperation(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		wsdlUrl = new StringParameter(this, "wsdlUrl");
		selectInputSetMethods = new StringParameter(this, "inputSetMethods");

		input = new TypedIOPort(this, "input", true, false);
		input.setTypeEquals(BaseType.UNKNOWN); // type of input value expected
												// is not known yet

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.OBJECT);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// //////////////////////////////////////////////////////////////////////////////
	// // Ports and Parameters ////

	/**
	 * The web service URL which is registered at EBI
	 */
	public StringParameter wsdlUrl = null;

	/**
	 * The standard "set_&lt;name&gt;" method choices for SoaplabServices
	 */
	public StringParameter selectInputSetMethods = null;

	/**
	 * Outputs the username and input values to next actor namely
	 * "SoaplabServiceStarter"
	 */
	public TypedIOPort output;

	/**
	 * The parameters for the "set_&lt;name&gt;" operations
	 */
	public TypedIOPort input;

	// //////////////////////////////////////////////////////////////////////////////
	// // public Methods ////

	/**
	 * Callback for changes in attribute values Get the WSDL from the given URL.
	 * 
	 * @param at
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute at) throws IllegalActionException {

		if (at == wsdlUrl) {

			if (wsdlUrl.getExpression().equals("")) {
				selectInputSetMethods.removeAllChoices();
			} else {
				selectInputSetMethods.removeAllChoices();
				try {

					// NOTE:- The following client is just created for geting
					// the WSDL input methods
					// in the very first stage of the workflow beforehand.The
					// client created at this
					// stage is not utilized after getting the required
					// inputMethods.

					SoaplabServiceClient client = new SoaplabServiceClient(
							wsdlUrl.getExpression());
					client.setJobId(); // create Job in order to be able to
										// generate input method names
					client.generateInputMethods(); // get the set_&lt;name&gt;
													// operations
					Vector inputMethods = client.getInputMethods();
					for (int i = 0; i < inputMethods.size(); i++) {
						selectInputSetMethods.addChoice("set_"
								+ (String) (inputMethods.elementAt(i)));
					}

				} catch (Exception ex) {
					_debug("<EXCEPTION> There was an error while parsing the WSDL. "
							+ ex + ". </EXCEPTION>");
					// GraphicalMessageHandler.message(
					_confErrorStr += "\n"
							+ ex.getMessage()
							+ "There was an error while parsing the WSDL in the actor: "
							+ this.getName();// );
				}

				if (!(_confErrorStr.equals(""))) {
					GraphicalMessageHandler.message(_confErrorStr);

				}
			}

		}

	} // end of attributeChanged

	/**
	 * Gets a list of input ports configured with the actor and finally outputs
	 * an object containing all the inputValues for the set_&lt;name&gt;
	 * operation as well as name of operation itself.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */

	public void fire() throws IllegalActionException {

		super.fire();

		// a vector to contain all user input data such as operation
		// name,operation input values
		Vector userInputs = new Vector();

		List inPortList = this.inputPortList();
		Iterator ports = inPortList.iterator();

		while (ports.hasNext()) {

			IOPort p = (IOPort) ports.next();
			List connections = p.connectedPortList();
			if (!(connections.isEmpty())) {

				if (wsdlUrl.getExpression().equals(""))
					GraphicalMessageHandler.message("\nWSDL is empty in actor:"
							+ this.getName());
				else if (selectInputSetMethods.getExpression().equals(""))
					GraphicalMessageHandler
							.message("\nSet Method is empty in actor:"
									+ this.getName());

				Token originalToken = p.get(0); // getInputVaulue
				Object tokenObjectForm = _getTokenConvertedObject(originalToken); // get
																					// the
																					// Java
																					// object
																					// form
																					// of
																					// input
																					// Token
				userInputs.add(tokenObjectForm);

			}

		}
		userInputs.add(selectInputSetMethods.getExpression()); // add the
																// set_&lt;name&gt;
																// operation
																// name
		output.broadcast(new ObjectToken(userInputs)); // braodcast the object
														// consisting of input
														// values + operation
														// name.

	}// end of fire

	/**
	 * Before executing the actor, delete any unconnected input ports to the
	 * actor done in order to prevent possible SDF disconnected graphs problem
	 * for users.
	 */
	public boolean prefire() throws IllegalActionException {

		List inPortList = this.inputPortList();
		Iterator ports = inPortList.iterator();

		while (ports.hasNext()) {

			IOPort p = (IOPort) ports.next();
			List connections = p.connectedPortList();
			if (connections.isEmpty()) {
				// delete the empty/unconnected port
				try {
					p.setContainer(null);
				} catch (Exception ex) {
					// Exceptions: IllegalAction or NameDuplicationException
					_debug("<EXCEPTION> There was an error while attempting to delete unused ports "
							+ ex + ". </EXCEPTION>");
					_confErrorStr += "\n"
							+ ex.getMessage()
							+ "There was an error while deleting the unused input ports of the actor."
							+ this.getName();// );
				}
			}
		}

		return super.prefire();

	} // end of prefire

	// //////////////////////////////////////////////////////////////////
	// // private Methods ///

	/**
	 * Given a token recieved from the input ports, the following attempts to
	 * get the token Value and convert it to a java Object form.
	 * 
	 * @param portToken
	 *            The input Token retrieved from the input port
	 */

	private Object _getTokenConvertedObject(Token portToken) {

		if (portToken.getType() instanceof BooleanType) {
			return new Boolean(portToken.toString());
		} else if (portToken.getType() instanceof IntType) {
			return new Integer(portToken.toString());
		} else if (portToken.getType() instanceof LongType) {
			return new Long(portToken.toString());
		} else if (portToken.getType() instanceof StringType) {
			String returnValueStr = portToken.toString();
			String suffix = "\"";
			// extra step to delete the "" quotation prefixes around the string
			if (returnValueStr.endsWith(suffix)
					&& returnValueStr.startsWith(suffix)) {
				return returnValueStr.substring(1,
						(returnValueStr.length()) - 1);
			}
		} else if (portToken.getType() instanceof DoubleType) {
			return new Double(portToken.toString());
		} else if (portToken.getType() instanceof UnsignedByteType) {
			// ->There is no byte in Ptolemy type sys. cast the byte to INT.
			return new Integer(portToken.toString());
		} else if (portToken.getType() instanceof ArrayType) {

			// get the type of elements inside the ArrayToken
			Type tokenType = ((ArrayToken) portToken).getElementType();

			if (tokenType instanceof BooleanType) {

				Boolean arrayBool[] = new Boolean[((ArrayToken) portToken)
						.length()];
				for (int i = 0; i < ((ArrayToken) portToken).length(); i++) {
					arrayBool[i] = new Boolean((((ArrayToken) portToken)
							.getElement(i)).toString());
				}
				return arrayBool;
			} else if (tokenType instanceof IntType) {

				Integer arrayInt[] = new Integer[((ArrayToken) portToken)
						.length()];
				for (int i = 0; i < ((ArrayToken) portToken).length(); i++) {
					arrayInt[i] = new Integer((((ArrayToken) portToken)
							.getElement(i)).toString());
				}
				return arrayInt;

			} else if (tokenType instanceof LongType) {

				Long arrayLong[] = new Long[((ArrayToken) portToken).length()];
				for (int i = 0; i < ((ArrayToken) portToken).length(); i++) {
					arrayLong[i] = new Long((((ArrayToken) portToken)
							.getElement(i)).toString());
				}
				return arrayLong;

			} else if (tokenType instanceof StringType) {

				String arrayString[] = new String[((ArrayToken) portToken)
						.length()];
				for (int i = 0; i < ((ArrayToken) portToken).length(); i++) {
					// extra step to delete the "" quotation prefixes around the
					// string
					String returnValueStr = (((ArrayToken) portToken)
							.getElement(i)).toString();
					String suffix = "\"";
					if (returnValueStr.endsWith(suffix)
							&& returnValueStr.startsWith(suffix)) {
						arrayString[i] = returnValueStr.substring(1,
								(returnValueStr.length()) - 1);
					}
				}
				return arrayString;

			} else if (tokenType instanceof DoubleType) {

				Double arrayDouble[] = new Double[((ArrayToken) portToken)
						.length()];
				for (int i = 0; i < ((ArrayToken) portToken).length(); i++) {
					arrayDouble[i] = new Double((((ArrayToken) portToken)
							.getElement(i)).toString());
				}
				return arrayDouble;

			} else if (tokenType instanceof UnsignedByteType) {

				// ->There is no byte in Ptolemy type sys. cast the byte to INT.
				Integer arrayInt[] = new Integer[((ArrayToken) portToken)
						.length()];
				for (int i = 0; i < ((ArrayToken) portToken).length(); i++) {
					arrayInt[i] = new Integer((((ArrayToken) portToken)
							.getElement(i)).toString());
				}
				return arrayInt;

			}
		} else {
			_debug("<WARNING>Could not convert the token to appropriate java Object.Setting it to string. </WARNING>");
		}

		return new String(portToken.toString());
	} // end of _getTokenConvertedObject

	// ////////////////////////////////////////////////////////////////////////////
	// // private variables ////

	protected String _confErrorStr = "";

}// end of SoaplabChooseOperation