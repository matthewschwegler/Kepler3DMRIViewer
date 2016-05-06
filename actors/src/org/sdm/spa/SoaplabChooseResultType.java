/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

///////////////////////////////////////////////////////////////
////SoaplabChooseResultType
/**
 * The following actor is for choosing get_&lt;name&gt; operations for
 * displaying web service execution results as desired. The input is a soaplab
 * service client on which the call is made after the user selects the
 * get_&lt;name&gt; operation.
 * 
 * @author Nandita Mangal
 * @version $Id: SoaplabChooseResultType.java, v 1.0 2005/19/07
 * @category.name web
 * @category.name external execution
 */

public class SoaplabChooseResultType extends TypedAtomicActor {

	/**
	 * Construct a SoaplabChooseResultType actor with given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name
	 */

	public SoaplabChooseResultType(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		wsdlUrl = new StringParameter(this, "wsdlUrl");
		selectOutputGetMethod = new StringParameter(this, "outputGetMethods");

		input = new TypedIOPort(this, "input", true, false);
		input.setTypeEquals(BaseType.OBJECT);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// ///////////////////////////////////////////////////////////////////
	// // Ports and Parameters ////
	/**
	 * The web service URL which is registered at EBI
	 */
	public StringParameter wsdlUrl = null;

	/**
	 * The "get_&lt;name&gt;" method for the soaplab webservice
	 */
	public StringParameter selectOutputGetMethod = null;

	/**
	 * output string value from the get_&lt;name&gt; operation
	 */
	public TypedIOPort output;

	/**
	 * The input is a soaplab service client on which the call is made after the
	 * user selects the get_&lt;name&gt; operation.
	 */
	public TypedIOPort input;

	// //////////////////////////////////////////////////////////////////////
	// // public methods ////

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

				selectOutputGetMethod.removeAllChoices();

			} else {

				selectOutputGetMethod.removeAllChoices();
				try {

					// NOTE:- The following client is just created for geting
					// the WSDL output methods
					// beforehand i.e before actual running of the workflow.
					client = new SoaplabServiceClient(wsdlUrl.getExpression());
					client.setJobId();
					client.generateOutputMethods();

					Vector outputMethods = client.getOutputMethods();

					for (int i = 0; i < outputMethods.size(); i++) {
						selectOutputGetMethod.addChoice("get_"
								+ (String) (outputMethods.elementAt(i)));
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
	 * Calls the appropriate get_&lt;name&gt; operation on the client and
	 * outputs the result obtained.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		super.fire();

		try {
			List inPortList = this.inputPortList();
			Iterator ports = inPortList.iterator();

			while (ports.hasNext()) {

				IOPort p = (IOPort) ports.next();
				List connections = p.connectedPortList();
				if (!(connections.isEmpty())) {
					ObjectToken inputToken = (ObjectToken) (p.get(0));
					client = (SoaplabServiceClient) (inputToken.getValue());
					output.broadcast(new StringToken(selectOutputGetMethod
							.getExpression()
							+ ":\n"
							+ client.doCall(selectOutputGetMethod
									.getExpression(), new Object[] { client
									.getJobId() })));
				}

			}
		} catch (Exception ex) {

			_debug("<EXCEPTION> There was an error while getting the web service results. "
					+ ex + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_confErrorStr += "\n"
					+ ex.getMessage()
					+ "\nThere was an error while getting the web service results in the actor: "
					+ this.getName();// );

		}

	}// end of fire

	// //////////////////////////////////////////////////////////////////////////////
	// // private variables ////

	private SoaplabServiceClient client;
	protected String _confErrorStr = "";
}