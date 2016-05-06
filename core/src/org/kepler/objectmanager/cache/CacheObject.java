/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: riddle $'
 * '$Date: 2010-06-28 12:16:46 -0700 (Mon, 28 Jun 2010) $' 
 * '$Revision: 25036 $'
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

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * Class that represents an object in the ObjectCache. This class should be
 * extended by each type of object that wants to control its own lifecycle
 * events and serialization events.
 */
public abstract class CacheObject implements CacheObjectInterface, Serializable {

	private static final long serialVersionUID = -3774217914823785296L;
	private static final Log log = LogFactory.getLog(CacheObject.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	protected transient String _name;
	protected transient KeplerLSID _lsid;
	protected Vector<String> _semanticTypes =  new Vector<String>();
	protected Hashtable<String,String> _attributes = new Hashtable<String,String>();

	/**
	 * default constructor. if you use this constrctor, you'll need to set the
	 * name and lsid through the setLSID and setName methods.
	 */
	protected CacheObject() {
		// default constructor;
		_lsid = null;
		_name = null;
	}

	/**
	 * construct a new CacheObject
	 */
	public CacheObject(String name, KeplerLSID lsid) {
		_name = name;
		_lsid = lsid;
	}

	/**
	 * get the name of this object
	 */
	public String getName() {
		return _name;
	}

	/**
	 * get the lsid for this object
	 */
	public KeplerLSID getLSID() {
		return _lsid;
	}

	/**
	 * set the lsid
	 */
	public void setLSID(KeplerLSID lsid) {
		_lsid = lsid;
	}

	/**
	 * set the name
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * this returns the semantic types vector
	 * 
	 *@return The semanticTypes value
	 */
	public void setSemanticTypes(Vector<String> semTypes) {
		_semanticTypes = semTypes;
	}

	/**
	 * this returns the semantic types vector
	 * 
	 *@return The semanticTypes value
	 */
	public Vector<String> getSemanticTypes() {
		return _semanticTypes;
	}

	/**
	 * set a user configured attribute on the CacheObject
	 */
	public void addAttribute(String name, String value) {
		if (isDebugging) log.debug("addAttribute("+name+","+value+")");
		_attributes.put(name, value);
	}

	/**
	 * get the attribute with the specified name
	 */
	public String getAttribute(String name) {
		if (isDebugging) log.debug("getAttribute("+name+")");
		return (String) _attributes.get(name);
	}
	
	public Set<String> getAttributeNames() {
		return _attributes.keySet();
	}

	/**
	 * remove the attribute with the given name and return it.
	 */
	public void removeAttribute(String name) {
		if (isDebugging) log.debug("removeAttribute("+name+")");
		_attributes.remove(name);
	}

	/**
	 * return the java object associated with this CacheObject
	 */
	public abstract Object getObject();

	/**
	 * call back for when this object is added to the cache. This method should
	 * be overridden by derviced classes if needed. Default action is to do
	 * nothing.
	 */
	public void objectAdded() {
	}

	/**
	 * call back for when this object is removed by the user. This method should
	 * be overridden by derived classes if needed. Default action is to do
	 * nothing.
	 */
	public void objectRemoved() {
	}

	/**
	 * call back for when this object is purged by ObjectCache. This method
	 * should be overridden by derived classes if needed. Default action is to
	 * do nothing.
	 */
	public void objectPurged() {
	}

}