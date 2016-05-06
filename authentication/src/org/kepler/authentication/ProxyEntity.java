/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.authentication;

/**
 * ProxyEntity is used to store the user's proxy
 * 
 * Note: 1. Since now the GAMA server does not provide lifetime info for the
 * proxy, we decide not to implement the lifetime setting and checking for
 * ProxyEntity.
 * 
 * @author Zhijie Guan guan@sdsc.edu
 * 
 */

public class ProxyEntity {
	private String userName; // User Name
	private Domain domain; // User Domain
	private String credential; // User Proxy

	/**
	 * constructor
	 */
	public ProxyEntity() {
		super();
	}

	/**
	 * @return Returns the domain.
	 */
	public Domain getDomain() {
		return domain;
	}

	/**
	 * @param domain
	 *            The domain to set.
	 */
	public void setDomain(Domain domain) {
		this.domain = domain;
	}

	/**
	 * @return Returns the proxy.
	 */
	public String getCredential() {
		return credential;
	}

	/**
	 * @param proxy
	 *            The proxy to set.
	 */
	public void setCredential(String cred) {
		this.credential = cred;
	}

	/**
	 * @return Returns the userName.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName
	 *            The userName to set.
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * Check if the proxy is expired or not
	 * 
	 */
	public boolean isExpired() {
		return false;
	}
}