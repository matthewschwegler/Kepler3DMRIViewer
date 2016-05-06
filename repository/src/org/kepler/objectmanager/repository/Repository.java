/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:27:07 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31133 $'
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

package org.kepler.objectmanager.repository;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.authentication.AuthenticationException;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * This interface represents a repository on the ecogrid
 * 
 * @author Chad Berkley
 */
public abstract class Repository {
	private static final Log log = LogFactory.getLog(Repository.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
	protected String name;
	protected String repository;
	protected String putPath;
	protected String authDomain;
	protected String queryPath;
	protected String authenticatedQueryPath;
	protected String authorizationPath;
	protected String lsidPath;
	protected String registry;
	protected String registryauth;
	protected String authProtocol;
	protected String lsidAuthority;
	
	protected boolean includeInSearch;
	protected boolean includeInSave;

	/**
	 * constructor
	 * 
	 * @param repository
	 *            the address of the repository
	 * @param username
	 *            the username of the repository
	 * @param password
	 *            the password for the repository
	 * @param authProtocol
	 *            protocol (http or https for the repository)
	 */
	public Repository(String name, String repository, String putPath,
			String authDomain, String lsidPath, String queryPath, String authenticatedQueryPath, 
			String authorizationPath, String registry, String registryauth, String authProtocol, 
			String lsidAuthority) {
		this.name = name;
		this.repository = repository;
		this.putPath = putPath;
		this.authDomain = authDomain;
		this.queryPath = queryPath;
		this.authenticatedQueryPath = authenticatedQueryPath;
		this.authorizationPath = authorizationPath;
		this.lsidPath = lsidPath;
		this.registry = registry;
		this.registryauth = registryauth;
		this.authProtocol = authProtocol;
		this.lsidAuthority = lsidAuthority;
		
		this.includeInSearch = false;
		this.includeInSave = false;
	}

	/**
	 * Search the repository and return an iterator of results (the Iterator
	 * contains KeplerLSIDs of matched items).
	 * @throws AuthenticationException 
	 */
	public abstract Iterator search(String queryString, boolean authenticate)
			throws RepositoryException, AuthenticationException;

	/**
	 * return the object from the repository that has the given lsid
	 * @throws AuthenticationException 
	 */
	public abstract InputStream get(KeplerLSID lsid, boolean authenticate) throws RepositoryException, AuthenticationException;

	/**
	 * get method with a string id
	 * @throws AuthenticationException 
	 */
	public abstract InputStream get(String id, boolean authenticate) throws RepositoryException, AuthenticationException;

	/**
	 * put an object into the repository with a predetermined sessionid
	 */
	public abstract void put(Object o, KeplerLSID lsid, String sessionid)
			throws RepositoryException;

	/**
	 * get the url for the lsid service associated with this repository
	 */
	public abstract String getLSIDServerURL();

	/**
	 * get the repository url
	 */
	public String getRepository() {
		return repository;
	}

	/**
	 * get the name of the repository
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * get the registry of the repository
	 */
	public String getRegistry() {
		return registry;
	}

	/**
	 * return the authentication service path
	 */
	public String getAuthDomain() {
		return authDomain;
	}

	/**
	 * return the put service path
	 */
	public String getPutPath() {
		return putPath;
	}

	/**
	 * return the query service path
	 */
	public String getQueryPath() {
		return queryPath;
	}
	
	/**
	 * return the authenticated query service path
	 */
	public String getAuthenticatedQueryPath() {
		return authenticatedQueryPath;
	}

	/**
	 * return the lsid service path
	 */
	public String getLSIDPath() {
		return lsidPath;
	}
	
	/**
	 * 
	 * return the authorization service path
	 */
	public String getAuthorizationPath(){
	  return authorizationPath;
	}

	/**
	 * return the protocol of the authentication
	 */
	public String getAuthProtocol() {
		return authProtocol;
	}

	public String getLsidAuthority() {
		return lsidAuthority;
	}

	public boolean includeInSearch() {
		return includeInSearch;
	}

	public void setIncludeInSearch(boolean includeInSearch) {
		this.includeInSearch = includeInSearch;
		
		//write the value to the config file
		String val = "false";
		if(includeInSearch)
		{
		  val = "true";
		}
				
		try
		{
      ConfigurationProperty prop = ConfigurationManager.getInstance()
          .getProperty(ConfigurationManager.getModule("repository"));
      List reposList = prop.getProperties("repository");
      for (int i = 0; i < reposList.size(); i++) 
      {
        ConfigurationProperty cp = (ConfigurationProperty) reposList.get(i);
        if(!cp.getProperty("name").getValue().equals(this.name))
        {
          continue;
        }
        
        ConfigurationProperty srProp = cp.getProperty("searchRepository");
        if(srProp != null)
        {
          srProp.setValue(val);
        }
        else
        {
          srProp = new ConfigurationProperty(cp.getModule(), "searchRepository");
          srProp.setValue(val);
          cp.addProperty(srProp);
        }
      }
      ConfigurationManager.getInstance().saveConfiguration();
    }
    catch(Exception e)
    {
      System.out.println("Error: could not write search repository to the config file: " + e.getMessage());
      e.printStackTrace();
    }
	}
	
	/**
	 * If the repository is included in saving process
	 * @return true if it is included in saving process
	 */
	public boolean includeInSave() {
    return includeInSave;
  }

	/**
	 * Set the repository to be included in saving process or not
	 * @param includeInSave true if the repository should be included
	 */
  public void setIncludeInSave(boolean includeInSave) {
    this.includeInSave = includeInSave;
  }

	/**
	 * return a string rep of this object for debug purposes
	 */
	public String toString() {
		String s = "name=" + name + ", repository=" + repository;
		return s;
		
	}
}