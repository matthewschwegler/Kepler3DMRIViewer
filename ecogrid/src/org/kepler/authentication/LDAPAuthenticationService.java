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

import org.ecoinformatics.ecogrid.client.AuthenticationServiceClient;
import org.kepler.authentication.gui.DomainSelectionGUI;
import org.kepler.authentication.gui.LDAPLoginGUI;

/**
 * Authenticates a user against LDAP
 * 
 */

public class LDAPAuthenticationService extends AuthenticationService {

	/**
	 * Use LDAP to authenticate the user
	 */
	public synchronized ProxyEntity authenticate(Domain d)
			throws AuthenticationException {
		/*
		 * STEPS: 1) open the gui to get user info 2) authenticate the user
		 * based on the info 3) if user is authenticated, create ProxyEntity and
		 * return it
		 * 
		 * MAJOR NOTE: This class gets its service url from the repository
		 * config file, not the authServicesBundle. this is done so that there
		 * are not two different defined authentication urls for ldap. i'm not
		 * sure if this is the right way to do this.
		 */

		// check if we even need to ask for the user/pass
		if (this.userName != null && this.password != null) {
			ProxyEntity pentity = authenticate(d, this.userName, this.password);
			this.credential = pentity.getCredential();
			return pentity;
		}

		System.out.println("LDAPAuthenticationService authenticate("+d.getDomain()+")");
		// help out with a hint as to where they are authenticating
		LDAPLoginGUI loginGUI = new LDAPLoginGUI();
		loginGUI.setDomainName(d.getDomain());
		loginGUI.fire();

		// user canceled the action
		if (loginGUI.getOrganization().equals(DomainSelectionGUI.DOMAIN_BREAK)) {
			return null;
		}

		// ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(
		// "Authenticating...");
		// worker.start();

		String username = loginGUI.getUsername();
		String password = loginGUI.getPassword();
		String org = loginGUI.getOrganization();
		loginGUI.resetFields();

		ProxyEntity pentity;
		System.out.println("d: " + d.getDomain() + " username: " + username
				+ " password: ****" + " org: " + org);
		pentity = authenticate(d, username, password, org);

		// worker.destroy(); // kill the window
		// worker.interrupt(); // stop the thread
		return pentity;
	}

	/**
	 * this method authenticates using a full dn instead of breaking it into
	 * username and org
	 */
	public ProxyEntity authenticate(Domain d, String dn, String password)
			throws AuthenticationException {

		String ldapURL = d.getServiceURL();
		String sessionid;
		ProxyEntity pentity;

		try {
			// System.out.println("==============authenticating with url: " +
			// ldapURL);
			AuthenticationServiceClient client = new AuthenticationServiceClient(
					ldapURL);
			String ldapUserStr;
			if (dn.equals("anon")) { 
				// get the generic kepler username from the
				// properties file
				ldapUserStr = d.getUsername();
				password = d.getPassword();
			} else {
				ldapUserStr = dn;
			}

			System.out.println("Authenticating with user: " + ldapUserStr
					+ " and " + "password: ******");
			sessionid = client.login_action(ldapUserStr, password);
			pentity = new ProxyEntity();
			pentity.setDomain(d);
			pentity.setCredential(sessionid);
			pentity.setUserName(ldapUserStr);
		} catch (Exception e) {
			throw new AuthenticationException("Error authenticating: "
					+ e.getMessage());
		}

		return pentity;
	}

	/**
	 * this method authenticates without creating a gui popup window for the
	 * user. The username/password/org must be provided. this method assumes
	 * dc=ecoinformatics,dc=org.
	 */
	public ProxyEntity authenticate(Domain d, String username, String password,
			String org) throws AuthenticationException {
		String dn;

		if (username.equals("anon")) {
			dn = "anon";
		} else {
			dn = "uid=" + username + ",o=" + org + ",dc=ecoinformatics,dc=org";
		}
		return authenticate(d, dn, password);
	}

	public void unauthenticate(ProxyEntity pentity)
			throws AuthenticationException {

		// check for credential first
		String credential = pentity.getCredential();
		if (credential == null) {
			throw new AuthenticationException(
					"Cannot unauthenticate with no credential given: credential="
							+ credential);
		}

		String ldapURL = pentity.getDomain().getServiceURL();

		try {
			AuthenticationServiceClient client = new AuthenticationServiceClient(
					ldapURL);
			System.out
					.println("unauthenticating for credential: " + credential);
			client.logout_action(credential);
			this.credential = null;
		} catch (Exception e) {
			throw new AuthenticationException("Error unauthenticating: "
					+ e.getMessage());
		}

	}
}