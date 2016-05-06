/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2011-04-07 12:45:01 -0700 (Thu, 07 Apr 2011) $' 
 * '$Revision: 27456 $'
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

import java.util.Vector;

/**
 * ProxyRepository is used to store all of the proxies requested by the user.
 * 
 * @author Zhijie Guan guan@sdsc.edu
 * 
 */

public class ProxyRepository {
	Vector repository = new Vector();

	/**
	 * This function gets the first available proxy and return it to the
	 * invoker.
	 * 
	 * @return The requested proxy, null if no such proxy exists.
	 */
	synchronized ProxyEntity getDefaultProxy() {
		if (repository.size() > 0) {
			return (ProxyEntity) repository.elementAt(0);
		} else {
			return null;
		}
	}

	/**
	 * This function returns the proxy with the specific index in the
	 * proxyRepository.
	 * 
	 * @param index
	 *            Proxy index in the proxyRepository
	 * @return The specified proxy
	 */
	ProxyEntity getProxyAt(int index) {
		if (index < repository.size()) {
			return (ProxyEntity) repository.elementAt(index);
		} else {
			return null;
		}
	}

	/**
	 * This function is used by LoginGUI to insert the new proxy into the
	 * proxyRepository
	 * 
	 * @param proxy
	 *            The new proxy
	 */
	public synchronized void insertProxy(ProxyEntity proxy) {
		repository.add(proxy);
		notifyAll();
	}

	/**
	 * This function is used to search proxy with the specified user name and
	 * domain.
	 * 
	 * @param userName
	 *            The specified username
	 * @param domain
	 *            The specified domain
	 * @return Index of the proxy, -1 for not found
	 */
	public synchronized ProxyEntity searchProxyInRepository(String userName,
			Domain domain) {
		for (int i = 0; i < repository.size(); i++) {
			if ((((ProxyEntity) repository.elementAt(i)).getUserName() == userName)
					&& (((ProxyEntity) repository.elementAt(i)).getDomain()
							.equalTo(domain))) {
				return (ProxyEntity) repository.elementAt(i);
			}
		}
		return null;
	}

	/**
	 * This function is used to search proxy with the specified domain.
	 * 
	 * @param domain
	 *            The specified domain
	 * @return Index of the proxy, -1 for not found
	 */
	public synchronized ProxyEntity searchProxyInRepository(Domain domain) {
		for (int i = 0; i < repository.size(); i++) {
			if (((ProxyEntity) repository.elementAt(i)).getDomain().equalTo(
					domain)) {
				return (ProxyEntity) repository.elementAt(i);
			}
		}
		return null;
	}

	public synchronized void removeProxy(ProxyEntity proxy) {
		for (int i = 0; i < repository.size(); i++) {
			if (((ProxyEntity) repository.elementAt(i)).getDomain().equalTo(
					proxy.getDomain())) {
				repository.remove(i);
				break;
			}
		}
	}
	
	 /**
   * Check if a credential with the given domain exists in this repository 
   * @param proxyCredential the string of the credential
   * @param domaim the given domain
   * @return true if the credential with the given domain exists
   */
  public synchronized boolean proxyExists(String proxyCredential,
                                                                                 Domain domain) {
    boolean existing = false;
    if(proxyCredential != null && domain != null){
      for (int i = 0; i < repository.size(); i++) {
        ProxyEntity proxy = (ProxyEntity) repository.elementAt(i);
        if(proxy != null && proxy.getDomain() != null && 
            proxy.getDomain().equals(domain) && proxy.getCredential() != null && 
            proxy.getCredential().equals(proxyCredential)) {
          existing = true;
          break;
        }      
      }
    }
    return existing;
  }


}