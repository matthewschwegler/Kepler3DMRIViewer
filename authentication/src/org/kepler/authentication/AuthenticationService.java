/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2011-03-31 11:37:56 -0700 (Thu, 31 Mar 2011) $' 
 * '$Revision: 27403 $'
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

/**
 * AuthenticationService is responsible for contact the GAMA service and get
 * back the credential for the user
 * 
 * @author Zhijie Guan guan@sdsc.edu
 * 
 */

public abstract class AuthenticationService {
	protected String serviceURL; // The service URL
	protected String operationName; // The service operation name
	protected String userName; // The username
	protected String password; // The user password
	protected String credential; // The user's credential if this service
	protected AuthenticationListener authListener = null;
	// if this service supports credentials

	/**
	 * @param operationName
	 *            The operationName to set.
	 */
	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	/**
	 * @param password
	 *            The password to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @param serviceURL
	 *            The serviceURL to set.
	 */
	public void setServiceURL(String serviceURL) {
		this.serviceURL = serviceURL;
	}

	/**
	 * @param userName
	 *            The userName to set.
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * 
	 * Note, currently unused, and authenticate(null) will cause NPE.
	 * 
	 * Function: to check if the user can get authentication correctly. Note:
	 * need to talk with GAMA group to see what will be returned if something is
	 * wrong
	 * 
	 * @return True/False to state if the user get authenticated or not
	 */
	public boolean isAuthenticated() throws AuthenticationException {
		//FIXME using null here causes NPE:
		authenticate(null); // Try to get authentication
		if (credential == null) {
			return false;
		}
		return true;
	}

	/**
	 * @return Credential to the user
	 */
	public String getCredential() {
		return credential;
	}
	
	/**
   * Set the authentication listener
   * @param listener
   */
  public void setAuthenticationListener(AuthenticationListener listener) {
    authListener = listener;
  }

	/**
	 * Authenticate a user
	 */
	public abstract ProxyEntity authenticate(Domain domain)
			throws AuthenticationException;

	/**
	 * Unauthenticate a user
	 */
	public abstract void unauthenticate(ProxyEntity proxy)
			throws AuthenticationException;

}