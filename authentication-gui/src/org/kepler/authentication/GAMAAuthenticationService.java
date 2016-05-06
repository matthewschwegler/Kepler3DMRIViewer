/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2013-01-16 15:42:20 -0800 (Wed, 16 Jan 2013) $' 
 * '$Revision: 31342 $'
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

package org.kepler.authentication;

import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.kepler.authentication.gui.GAMALoginGUI;
import org.kepler.gui.ProgressMonitorSwingWorker;

/**
 * AuthenticationService is responsible for contact the GAMA service and get
 * back the credential for the user
 * 
 * @author Zhijie Guan guan@sdsc.edu
 * 
 */

public class GAMAAuthenticationService extends AuthenticationService {

	/**
	 * Use the GAMA authentication service to get credential returns null if the
	 * user cancels the action.
	 */
	public synchronized ProxyEntity authenticate(Domain d)
			throws AuthenticationException {
		/*
		 * STEPS: 1) open the gui to get user info 2) authenticate the user
		 * based on the info 3) if user is authenticated, create ProxyEntity and
		 * return it
		 */

		GAMALoginGUI.fire();
		while (GAMALoginGUI.getUsername() == null) {
		} // wait for the input to be made and the 'ok' button to be pressed

		// user canceled the action
		if (GAMALoginGUI.getUsername().equals(GAMALoginGUI.USERNAME_BREAK)) {
			return null;
		}

		String userName = GAMALoginGUI.getUsername();
		String password = GAMALoginGUI.getPassword();

		ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(
				"Authenticating...");
		worker.execute();

		ProxyEntity pentity = new ProxyEntity();
		try {
			System.out.println("1");
			Service service = new Service(); // GAMA web service
			System.out.println("2");
			Call call = (Call) service.createCall(); // Service call
			System.out.println("3");

			System.out.println("serviceURL: " + serviceURL);
			System.out.println("4");
			call.setTargetEndpointAddress(new java.net.URL(serviceURL));
			System.out.println("5");
			call.setOperationName(operationName);
			System.out.println("7");

			// call parameters
			call.addParameter("username", XMLType.XSD_STRING, ParameterMode.IN);
			System.out.println("8");
			call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
			System.out.println("9");
			call.setReturnType(XMLType.XSD_STRING);
			System.out.println("10: " + userName + "/" + password);
			Object[] o = new Object[] { userName, password };
			System.out.println("o: " + o[0] + "/" + o[1]);

			Object returnObject = call.invoke(o);
			if (returnObject == null) {
				System.out.println("11");
				System.out.println("returnobj is null");
				credential = null;
				System.out.println("12");
			} else {
				System.out.println("returnobj is " + (String) returnObject);
				credential = (String) returnObject;
			}

			pentity = new ProxyEntity();
			pentity.setDomain(d);
			pentity.setCredential(credential);
			pentity.setUserName(userName);

			worker.done();
			worker.cancel(true);
			return pentity;
		} catch (Exception e) {
			worker.done();
			worker.cancel(true);
			throw new AuthenticationException(
					"GAMA Authentication Service error: " + e.toString());
		}
	}

	public void unauthenticate(ProxyEntity pentity)
			throws AuthenticationException {
		// TODO: implement the unauth method BRL20071129
		throw new AuthenticationException(
				"GAMA unauthenticate method is not implemented");
	}
}