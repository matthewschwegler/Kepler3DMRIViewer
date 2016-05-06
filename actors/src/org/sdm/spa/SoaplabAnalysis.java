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

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

///////////////////////////////////////////////////////////////
////SoaplabAnalysis
/**
 * The following actor is for exectuing the standard soaplab operations after
 * creating soaplab clients and setting input operations in the former stages of
 * the workflow.
 * 
 * @author Nandita Mangal
 * @version $Id: SoaplabAnalysis.java, v 1.0 2005/19/07
 * @category.name web
 * @category.name external execution
 */

public class SoaplabAnalysis extends TypedAtomicActor {

	/**
	 * Construct a SoaplabAnalysis actor with given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name
	 */

	public SoaplabAnalysis(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		soaplabMethod = new StringParameter(this, "soaplabMethodName");
		_addMethodChoices(); // add the standard method choices for soaplab
								// services
		soaplabEditMethod = new StringParameter(this,
				"OR Enter another Soaplab Method");

		// NOTE inputClient = outputClient , can use multiport if director is
		// not SDF
		inputClient = new TypedIOPort(this, "clientInput", true, false);
		inputClient.setTypeEquals(BaseType.OBJECT);

		outputClient = new TypedIOPort(this, "clientOutput", false, true);
		outputClient.setTypeEquals(BaseType.OBJECT);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// //////////////////////////////////////////////////////////////////////////
	// // Ports and Parameters ////

	/**
	 * The standard method choices for SoaplabServices
	 */
	public StringParameter soaplabMethod = null;

	/**
	 * Enter your own Soaplab Method.
	 */
	public StringParameter soaplabEditMethod = null;

	/**
	 * SoaplabServiceClient input from pervious soaplab actor
	 * operation.ServiceClient created in SoaplabService Starter Actor.
	 */
	public TypedIOPort inputClient;

	/**
	 * Modified SoaplabServiceClient after performing a Call with the client's
	 * jobID.
	 */
	public TypedIOPort outputClient;

	// //////////////////////////////////////////////////////////////////////////
	// // public Methods ////

	/**
	 * Gets the client as input and given the standard soaplab operation, calls
	 * that specific operation on the client.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */

	public void fire() throws IllegalActionException {

		super.fire();

		String method = "";
		if (soaplabEditMethod.getExpression().equals(""))
			method = soaplabMethod.getExpression();
		else
			method = soaplabEditMethod.getExpression();

		try {
			ObjectToken clientToken = (ObjectToken) (inputClient.get(0));
			SoaplabServiceClient client = (SoaplabServiceClient) (clientToken
					.getValue());

			client.doCall(method, new Object[] { client.getJobId() });
			ObjectToken tokenClient = new ObjectToken(client);
			outputClient.broadcast(tokenClient);

		} catch (Exception ex) {

			if (soaplabEditMethod.getExpression().equals("")
					&& soaplabMethod.getExpression().equals(""))
				GraphicalMessageHandler
						.message("\nSoaplab Method not set in actor Soaplab Analysis!");

			_debug("<EXCEPTION> There was an error while executing the web service operation "
					+ ex + ". </EXCEPTION>");
			// GraphicalMessageHandler.message(
			_confErrorStr += "\n"
					+ ex.getMessage()
					+ "\nThere was an error while executing the web service operation in the actor: "
					+ this.getName();// );

		}

		if (!(_confErrorStr.equals(""))) {
			GraphicalMessageHandler.message(_confErrorStr);

		}

	}// end of fire

	// //////////////////////////////////////////////////////////////////////////
	// // private Methods ////

	/**
	 * Simply add the known standard soaplab operations to the parameter
	 * soaplabMethod
	 */

	private void _addMethodChoices() {

		soaplabMethod.addChoice("run");
		soaplabMethod.addChoice("destory");
		soaplabMethod.addChoice("describe");
		soaplabMethod.addChoice("getStatus");
		soaplabMethod.addChoice("getInputSpec");
		soaplabMethod.addChoice("getResultSpec");
		soaplabMethod.addChoice("getAnalysisType");
		soaplabMethod.addChoice("waitFor");
		soaplabMethod.addChoice("runAndWaitFor");
		soaplabMethod.addChoice("getResults");
		soaplabMethod.addChoice("terminate");
		soaplabMethod.addChoice("getLastEvent");
		soaplabMethod.addChoice("getNotificationDescriptor");
		soaplabMethod.addChoice("getCreted");
		soaplabMethod.addChoice("getStarted");
		soaplabMethod.addChoice("getEnded");
		soaplabMethod.addChoice("getCharacteristics");
		soaplabMethod.addChoice("getElapsed");
		soaplabMethod.addChoice("getSomeResults");

	}// end of __addMethodChoices

	protected String _confErrorStr = "";
}