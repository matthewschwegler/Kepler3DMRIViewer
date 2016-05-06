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

// FOR SOAPLAB API

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Vector;

import org.embl.ebi.SoaplabShare.AnalysisWS;

import ptolemy.gui.GraphicalMessageHandler;
import embl.ebi.soap.axis.AxisCall;
import embl.ebi.utils.GException;

///////////////////////////////////////////////////////////////
////SoaplabServiceClient
/**
 * The following client is used by other web service soaplab related actors
 * while executing the web service as well as transmitting the above client
 * through various actors in the workflow. The gist of the following client lies
 * in the doCall method which establishes contact with the WSDL and executes the
 * desired enetered operation.
 * 
 * @author Nandita Mangal
 * @version $Id: SoaplabServiceClient.java, v 1.0 2005/19/07
 * @category.name web
 * @category.name external execution
 */

public class SoaplabServiceClient {

	/**
	 * Construct a SoaplabServiceClient with given wsdl.
	 * 
	 * @param wsdl_URL
	 *            The wsdl of the derived web service to be executed
	 * @exception MalformedURLException
	 *                If the url is not valid
	 * @exception GException
	 *                Error with the given WSDL analysis interface
	 */

	public SoaplabServiceClient(String wsdl_URL) throws MalformedURLException,
			GException {
		call = new AxisCall(new URL(wsdl_URL));
		InputMethods = new Vector(); // input set_<name> operations of
										// webservice
		OutputMethods = new Vector(); // output get_<name> operations of
										// webservice

	}

	// ////////////////////////////////////////////////////////////////////
	// // Public Methods ////

	/**
	 * To create a new job in the client. Call the standard operation
	 * "createEmtpyJob", via the doCall method
	 */
	public void setJobId() {
		jobId = (String) (doCall("createEmptyJob", new Object[] {}));
	}

	/**
	 * To get the client's job Id which was created in setJobID via
	 * "createEmptyJob".
	 */
	public String getJobId() {
		return jobId;
	}

	/**
	 * To get a list of all the InputMethods (set_<name>) methods in the given
	 * WSDL of the client.Inputmethods were set via caliing the
	 * generateInputMethods().
	 */
	public Vector getInputMethods() {
		return InputMethods;

	}

	/**
	 * To get a list of all the OutputMethods (get_<name>) methods in the given
	 * WSDL of the client.Outputmethods were set via caliing the
	 * generateOutputMethods().
	 */
	public Vector getOutputMethods() {
		return OutputMethods;
	}

	/**
	 * To generate all the output get_<name> operations belonging to the derived
	 * web service.INPUT_NAME gives the actual operation name of get_<name>
	 */
	public void generateOutputMethods() {
		try {

			Map[] attrs = (Map[]) doCall("getResultSpec", new Object[] {});
			if (attrs != null) {
				Arrays.sort(attrs, new Comparator() {
					public int compare(Object a, Object b) {
						String name1 = (String) ((Map) a)
								.get(AnalysisWS.INPUT_NAME);
						String name2 = (String) ((Map) b)
								.get(AnalysisWS.INPUT_NAME);
						return name1.compareTo(name2);
					}
				});

				for (int i = 0; i < attrs.length; i++) {
					OutputMethods.add(attrs[i].get(AnalysisWS.INPUT_NAME));

				}

			}
		} catch (Exception e) {
		}

	}

	/**
	 * To generate all the input set_<name> operations belonging to the derived
	 * web service.INPUT_NAME gives the actual operation name of set_<name>
	 */

	public void generateInputMethods() {

		try {

			Map[] attrs = (Map[]) doCall("getInputSpec", new Object[] {});
			if (attrs != null) {
				Arrays.sort(attrs, new Comparator() {
					public int compare(Object a, Object b) {
						String name1 = (String) ((Map) a)
								.get(AnalysisWS.INPUT_NAME);
						String name2 = (String) ((Map) b)
								.get(AnalysisWS.INPUT_NAME);
						return name1.compareTo(name2);
					}
				});

				for (int i = 0; i < attrs.length; i++) {
					InputMethods.add(attrs[i].get(AnalysisWS.INPUT_NAME));

				}

			}

		} catch (Exception e) {
		}

	}

	// ////////////////////////////////////////////////////////////////////
	// // protected methods ////

	/**
	 * Performs the actual web service execution via calling the AxisCall's
	 * doCall method which executes the given operation with input values.
	 * 
	 * @param method
	 *            The name of soaplab operation to be executed
	 * @param parameters
	 *            InputValues to be given to the above soaplab operation while
	 *            executing.One of the required parameters is the client's job
	 *            id.
	 */
	protected Object doCall(String method, Object[] parameters) {

		try {
			return call.doCall(method, parameters);
		} catch (Exception ex) {
			_confErrorStr += "\n"
					+ ex.getMessage()
					+ "There was an error while executing the web service. Kindly make sure the WSDL is valid. ";

		}
		if (!(_confErrorStr.equals(""))) {
			GraphicalMessageHandler.message(_confErrorStr);
		}
		return null;
	}

	// /////////////////////////////////////////////////////////////////////////////////
	// // private variables ////

	private AxisCall call;
	private String jobId;
	private Vector InputMethods;
	private Vector OutputMethods;
	private String _confErrorStr = "";

} // end of SoaplabServiceClient