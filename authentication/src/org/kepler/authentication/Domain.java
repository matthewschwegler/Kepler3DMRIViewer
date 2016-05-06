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
 * Domain is used to represent the organization which the user belongs to. Users
 * can setup their domain and subdomain, which represent the organization and
 * department/group they belong to, respectively.
 * 
 * @author Zhijie Guan guan@sdsc.edu
 * 
 */

public class Domain {
	private String domain;
	private String subdomain;
	private String serviceURL;
	private String serviceOperation;
	private String serviceClass;
	private String username;
	private String password;

	/**
	 * @return Returns the domain.
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @param domain
	 *            The domain to set.
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * @return Returns the subdomain.
	 */
	public String getSubdomain() {
		return subdomain;
	}

	/**
	 * @param subdomain
	 *            The subdomain to set.
	 */
	public void setSubdomain(String subdomain) {
		this.subdomain = subdomain;
	}

	/**
	 * @param fullDomain
	 *            The fullDomain to set.
	 */
	public void setFullDomain(String fullDomain) {
		int slashPosition = fullDomain.indexOf('/');

		if (slashPosition == -1) {
			// Only domain exists
			this.domain = fullDomain;
		} else {
			// Subdomain exists
			this.domain = fullDomain.substring(0, slashPosition);
			this.subdomain = fullDomain.substring(slashPosition + 1);
		}
	}

	/**
	 * @return Returns the fullDomain
	 */
	public String getFullDomain() {
		if (this.subdomain != null) {
			return this.domain + '/' + this.subdomain;
		} else {
			return this.domain;
		}
	}

	/**
	 * Compare to another domain to see if they are equal
	 * 
	 * @param d
	 *            Another domain
	 * @return True if equal, False otherwise
	 */
	public boolean equalTo(Domain d) {
		if ((this.domain == d.getDomain())
				&& (this.subdomain == d.getSubdomain())) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return Returns the serviceOperation.
	 */
	public String getServiceOperation() {
		return serviceOperation;
	}

	/**
	 * @param serviceOperation
	 *            The serviceOperation to set.
	 */
	public void setServiceOperation(String serviceOperation) {
		this.serviceOperation = serviceOperation;
	}

	/**
	 * @return Returns the serviceURL.
	 */
	public String getServiceURL() {
		return serviceURL;
	}

	/**
	 * @param serviceURL
	 *            The serviceURL to set.
	 */
	public void setServiceURL(String serviceURL) {
		this.serviceURL = serviceURL;
	}

	/**
	 * @return Returns the serviceURL.
	 */
	public String getServiceClass() {
		return serviceClass;
	}

	/**
	 * @param serviceURL
	 *            The serviceURL to set.
	 */
	public void setServiceClass(String serviceClass) {
		this.serviceClass = serviceClass;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String toString() {
		return "[name: " + domain + ", serviceURL: " + serviceURL
				+ ", serviceOperation: " + serviceOperation + "]";
	}
}