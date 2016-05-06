/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2013-01-24 16:59:27 -0800 (Thu, 24 Jan 2013) $' 
 * '$Revision: 31370 $'
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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.gui.Configuration;
import ptolemy.kernel.util.StringAttribute;

/**
 * AuthenticationManager manages all the authentication issue for the user. It
 * helps the user to get the proxy from the GAMA server. For details:
 * http://kepler-project.org/Wiki.jsp?page=KeplerAuthenticationFramework Note:
 * all of the get Proxy methods are synchronized to avoid multiple entries
 * (which may cause multiple user login dialogs appear) from the actors.
 * 
 * @author Zhijie Guan guan@sdsc.edu
 * 
 */

public class AuthenticationManager {
  private static final Log log = LogFactory.getLog(AuthenticationManager.class
      .getName());
	// ProxyRepository is used to store all of the proxies
	private ProxyRepository proxyRepository = new ProxyRepository();

	// Singleton template is used here to make sure there is only one
	// AuthenticationManager in the system
	private static AuthenticationManager authenticationManager = null;
	private static AuthenticationListener authListener = null;

	/**
	 * singleton constructor
	 */
	private AuthenticationManager() {
		try {
			preAuthenticate();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void preAuthenticate() throws Exception {
		
		// authenticate using configured values
		String userName = null;
		String password = null;
		String[] domainNames = null;
		
		// look up the configured values
		Configuration _configuration = (Configuration) Configuration.configurations().iterator().next();
		StringAttribute domainAttr = (StringAttribute)_configuration.getAttribute("_domain");
		if (domainAttr != null) {
			String domainName = domainAttr.getExpression();
			domainNames = domainName.split(",");
		}
		StringAttribute userAttr = (StringAttribute)_configuration.getAttribute("_username");
		if (userAttr != null) {
			userName = userAttr.getExpression();
		}
		StringAttribute passwordAttr = (StringAttribute)_configuration.getAttribute("_password");
		if (passwordAttr != null) {
			password = passwordAttr.getExpression();
		}
		
		if (domainNames != null && userName != null && password != null) {
			for (String domainName: domainNames) {
				// find the domain
				Domain domain = AuthenticationManager.getDomain(domainName);
				
				// do the authentication
				AuthenticationService authService = AuthenticationManager.getAuthenticationService(domain);
				authService.setUserName(userName);
				authService.setPassword(password);
				ProxyEntity proxy = authService.authenticate(domain);
				String sessionId = proxy.getCredential();
				addProxyEntity(proxy);
			}
		}
	}

	/**
	 * Part of singleton template, the only way to get an instance of
	 * AuthenticationManager
	 * 
	 * @return The unique AuthenticationManager instance
	 */
	public static AuthenticationManager getManager() {
		if (authenticationManager == null) {
			authenticationManager = new AuthenticationManager();
		}
		return authenticationManager;
	}

	/**
	 * This function retrieves back the default proxy for the user
	 * 
	 * @return The default proxy
	 */
	/*
	 * public synchronized ProxyEntity getProxy() { while
	 * (proxyRepository.getDefaultProxy() == null) { // No proxy exists
	 * LoginGUI.fire(proxyRepository); // Launch the login dialog
	 * proxyRepository.waitForUserLogin(); // Wait in ProxyRepository for user
	 * login } return proxyRepository.getDefaultProxy(); }
	 */

	/**
	 * This function retrieves back the user proxy within specified domain,
	 * adding ProxyEntity to AuthenticationManager's proxyRepository in the 
	 * process.
	 * 
	 * @param domain
	 *            Specified domain
	 * @return The requested proxy
	 */
	public synchronized ProxyEntity getProxy(String domainName)
			throws AuthenticationException {
		try {
			Domain d = getDomain(domainName);
			ProxyEntity entity = proxyRepository.searchProxyInRepository(d);
			// check to see if we're already authenticated against this service
			if (entity != null) {
				return entity;
			}
			// get the service
			AuthenticationService service = getAuthenticationService(d);
			// auth against the service
			entity = service.authenticate(d);
			addProxyEntity(entity);
			return entity;
		} catch (Exception e) {
			if (e instanceof AuthenticationException){
				throw (AuthenticationException)e;
			}
			throw new AuthenticationException(e.getMessage());
		}
	}

	public synchronized void revokeProxy(ProxyEntity entity)
			throws AuthenticationException {
		try {
			// Domain d = getDomain(domainName);
			// ProxyEntity entity = proxyRepository.searchProxyInRepository(d);

			if (entity != null) { // check to see if we're already authenticated
									// against this service.
				AuthenticationService service = getAuthenticationService(entity
						.getDomain()); // get the service
				service.unauthenticate(entity); // unauth against the service
				proxyRepository.removeProxy(entity);
			}
			return;
		} catch (AuthenticationException ae) {
			throw ae;
		} catch (Exception e) {
			throw new AuthenticationException(e.getMessage());
		}
	}

	public synchronized ProxyEntity peekProxy(String domainName)
			throws AuthenticationException {
		try {
			Domain d = getDomain(domainName);
			ProxyEntity entity = proxyRepository.searchProxyInRepository(d);

			if (entity != null) { // do we have some sort of proxy?
				return entity;
			}
			return null;
		} catch (AuthenticationException ae) {
			throw ae;
		} catch (Exception e) {
			throw new AuthenticationException(e.getMessage());
		}
	}

	/**
	 * adds a proxyEntity to the entity repository if it was authenticated
	 * outside of the AuthManager.
	 */
	public synchronized void addProxyEntity(ProxyEntity pentity)
			throws AuthenticationException {
		if (pentity != null) {
			// save the entity for the next time
			proxyRepository.insertProxy(pentity);
		} else {
			AuthenticationException ex = new AuthenticationException(
				"Error authenticating.  The "
				+ "authentication service returned null.");
			ex.setType(AuthenticationException.USER_CANCEL);
			throw ex;
		}
	}

	/**
	 * return an AuthenticationService for the given domain name
	 */
	public synchronized AuthenticationService getAuthenticationService(
			String domainName) throws AuthenticationException {
		try {
			DomainList dlist = DomainList.getInstance();
			Domain d = dlist.searchDomainList(domainName);
			return getAuthenticationService(d);
		} catch (Exception e) {
			throw new AuthenticationException(
					"Error getting authentication service: " + e.getMessage());
		}
	}

	/**
	 * returns a Domain for a given domainName
	 */
	public static Domain getDomain(String domainName)
			throws AuthenticationException {
		try {
			DomainList dlist = DomainList.getInstance();
			Domain d = dlist.searchDomainList(domainName);
			return d;
		} catch (Exception e) {
			throw new AuthenticationException("Error getting domain: "
					+ e.getMessage());
		}
	}

	/**
	 * returns an instantiation of the correct AuthenticationService based on
	 * the serviceClass listed for the domain. This is set in the
	 * authServicesBundle.properties file
	 */
	public static synchronized AuthenticationService getAuthenticationService(
			Domain domain) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		String className = domain.getServiceClass();
		Class authClass = Class.forName(className);
		AuthenticationService service = (AuthenticationService) authClass
				.newInstance();
		service.setAuthenticationListener(authListener);
		service.setOperationName(domain.getDomain());
		service.setServiceURL(domain.getServiceURL());
		service.setOperationName(domain.getServiceOperation());
		return service;
	}
	
  /**
   * Check if a credential with the given domain exists in this manager 
   * @param proxyCredential the string of the credential
   * @param domaim the given domain
   * @return true if the credential with the given domain exists
   */
	public synchronized boolean proxyExists(String proxyCredential,
			String domainName) {
		Domain domain = null;
		try {
			domain = DomainList.getInstance().searchDomainList(domainName);
		} catch (IOException e) {
			log.warn("Couldn't get the autherntication doman named "
					+ domainName + " - " + e.getMessage());
			return false;
		}

		return proxyRepository.proxyExists(proxyCredential, domain);
	}

	
	/**
	 * Set the authentication listener
	 * @param listener
	 */
	public void setAuthenticationListener(AuthenticationListener listener) {
		authListener = listener;
	}
}