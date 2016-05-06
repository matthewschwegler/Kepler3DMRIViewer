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

package org.kepler.objectmanager.cache;

import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * Interface that represents an object in the ObjectCache.
 */
public interface CacheObjectInterface {
	/**
	 * get the name of this object
	 */
	public String getName();

	/**
	 * get the lsid for this object
	 */
	public KeplerLSID getLSID();

	/**
	 * set the lsid
	 */
	public void setLSID(KeplerLSID lsid);

	/**
	 * set the name
	 */
	public void setName(String name);

	/**
	 * set a user configured attribute on the CacheObject
	 */
	public void addAttribute(String name, String value);

	/**
	 * get the attribute with the specified name
	 */
	public String getAttribute(String name);

	/**
	 * remove the attribute with the given name and return it.
	 */
	public void removeAttribute(String name);

	/**
	 * return the java object associated with this CacheObject
	 */
	public Object getObject();

	/**
	 * call back for when this object is added to the cache
	 */
	public void objectAdded();

	/**
	 * call back for when this object is removed by CacheManager
	 */
	public void objectRemoved();

	/**
	 * call back for when this object is purged by CacheManager
	 */
	public void objectPurged();

	/**
	 * deserialize this cache object
	 */
	// public void deserialize(InputStream in) throws CacheException;
	/**
	 * serialize this cache object
	 */
	// public void serialize(OutputStream out) throws CacheException;
}