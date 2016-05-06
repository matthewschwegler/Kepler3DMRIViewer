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

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

/**
 * The DomainList is used to store all of the domain info included in Kepler
 * authentication framework. Currently we only have two domains: GEON and SEEK.
 * In the future, with more domains added into the authentication framework, all
 * the domain info would be stored in a XML configuration file. This class needs
 * to parse that file and read the domain info into the system.
 * 
 * @author Zhijie Guan guan@sdsc.edu
 * 
 */

public class DomainList {
	private String AUTHSERVICESBUNDLE = "ptolemy/configs/kepler/authServicesBundle";
	private Vector domainVector = new Vector();
	private static DomainList dlist = null; // singleton member

	/**
	 * The constructor is used to set/read domain info
	 * 
	 */
	private DomainList() throws IOException 
  {
    List propList = ConfigurationManager.getInstance().getProperties(
      ConfigurationManager.getModule("authentication"), "config.service");
    for(int i=0; i<propList.size(); i++)
    {
      ConfigurationProperty serviceProp = (ConfigurationProperty)propList.get(i);
      Domain d = new Domain();
      d.setDomain(serviceProp.getProperty("domain").getValue());
      d.setServiceOperation(serviceProp.getProperty("serviceOperation").getValue());
      d.setServiceURL(serviceProp.getProperty("serviceURL").getValue());
      d.setServiceClass(serviceProp.getProperty("serviceClass").getValue());
      d.setUsername(serviceProp.getProperty("username").getValue());
      d.setPassword(serviceProp.getProperty("password").getValue());
      domainVector.add(d);
    }
	}

	public static DomainList getInstance() throws IOException {
		if (dlist == null) {
			dlist = new DomainList();
		}
		return dlist;
	}

	/**
	 * @return a Vector of Domains
	 */
	public Vector getDomainList() {
		return domainVector;
	}

	public void addDomain(Domain d) {
		Domain existingDomain = this.searchDomainList(d.getDomain());
		if (existingDomain == null) {
			this.domainVector.add(d);
		}
	}

	/**
	 * Search the domain list for a specific domain. return the domain if it's
	 * found, if not, return null;
	 */
	public Domain searchDomainList(String domainName) {
		for (int i = 0; i < domainVector.size(); i++) {
			Domain d = (Domain) domainVector.elementAt(i);
			if (d.getDomain().equals(domainName)) {
				return d;
			}
		}
		return null;
	}
}