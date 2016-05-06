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
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

///////////////////////////////////////////////////////////////
////SoaplabServiceStarter
/**
 * The following actor is for creating the soaplab client as well as setting the
 * client's set_&lt;name&gt; operations with appropriate values.The output is
 * the soaplab service client itself, forwarded to other others such as
 * SoaplabAnalysis
 * 
 * @author Nandita Mangal
 * @version $Id: SoaplabServiceStarter.java, v 1.0 2005/19/07
 * @category.name web
 * @category.name external execution
 */

public class SoaplabServiceStarter extends TypedAtomicActor {

	/**
	 * Construct a SoaplabServiceStarter actor with given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name
	 */
	public SoaplabServiceStarter(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		wsdlUrl = new StringParameter(this, "wsdlUrl");

		setOperation1 = new TypedIOPort(this, "setOperation1", true, false);
		setOperation1.setTypeEquals(BaseType.OBJECT);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.OBJECT);

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

	private StringParameter inputMethods = null;

	/**
	 * Errors encountered while parsing WSDL/setting set_&lt;name&gt; operations
	 */
	public TypedIOPort clientExecErrors;

	/**
	 * The output is the soaplab service client itself, forwarded to other
	 * others such as SoaplabAnalysis
	 */
	public TypedIOPort output;

	/**
	 * Setting set_&lt;name&gt; operations with appropriate input values.
	 */
	public TypedIOPort setOperation1;

	// /////////////////////////////////////////////////////////////////////
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
				// do nothing
			} else {
				try {

					// Create the actual soaplab service client to be used by
					// this
					// as well as forwarded to other soaplab actors in later
					// stages
					// of the workflow
					client = new SoaplabServiceClient(wsdlUrl.getExpression());
					client.setJobId();
				} catch (Exception ex) {
					_debug("<EXCEPTION> There was an error while parsing the WSDL. "
							+ ex + ". </EXCEPTION>");
					// GraphicalMessageHandler.message(
					_confErrorStr += "\n"
							+ ex.getMessage()
							+ "There was an error while parsing the WSDL in the actor: "
							+ this.getName();// );
				}

			}

			if (!(_confErrorStr.equals(""))) {
				GraphicalMessageHandler.message(_confErrorStr);

			}

		}

	} // end of attributeChanged

	/**
	 * For each of the connected input port, retrieve the input Values from the
	 * port ( an object containing the set_&lt;name&gt; operation as well its
	 * respective input Values). Perform the call for setting the above
	 * set_&lt;name&gt; operations with the client.
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
					if (wsdlUrl.getExpression().equals("")) {
						GraphicalMessageHandler
								.message("WebService WSDL is empty in actor:"
										+ this.getName());
					}
					Vector input = (Vector) (((ObjectToken) (p.get(0)))
							.getValue());
					String setOperationName = (String) (input.lastElement());
					input.removeElementAt(input.size() - 1); // remove
																// set_&lt;name&gt;
																// from the
																// inputValues
					if (input.size() == 1)
						client.doCall(new String(setOperationName),
								new Object[] { client.getJobId(),
										input.elementAt(0) });

					else // more than one input value to the set operation
					{
						Object[] params = new Object[input.size() + 1];
						params[0] = (Object) (client.getJobId());
						for (int i = 0; i < input.size(); i++) {
							params[i + 1] = (Object) (input.elementAt(i));
						}
						client.doCall(new String(setOperationName), params);
					}
				}

			}

			// forward the client to other states in the workflow
			ObjectToken aboveClient = new ObjectToken(client);
			output.broadcast(aboveClient);

		} catch (Exception ex) {
			_debug("<EXCEPTION> There was an error executing the web service operation "
					+ ex + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_confErrorStr += "\n" + ex.getMessage()
					+ "There was an error executing the web service operation"
					+ this.getName();// );
		}

		if (!(_confErrorStr.equals(""))) {
			GraphicalMessageHandler.message(_confErrorStr);

		}

	} // end of fire

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

	// ////////////////////////////////////////////////////////////////////
	// // private methods ////

	private SoaplabServiceClient client;
	protected String _confErrorStr = "";

} // end of SoaplabServiceStarter