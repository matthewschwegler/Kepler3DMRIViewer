/*
 * Copyright (c) 2010 The Regents of the University of California.
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

package org.kepler.moml;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * A Class to hold the Kepler Actor Metadata for an Actor described in MoML.
 * 
 * @author Aaron Schultz
 */
public class KeplerActorMetadata {

	/**
	 * The Name of the Actor.
	 */
	private String _name;
	
	/**
	 * The LSID of the Actor as found in the NamedObjId attribute.
	 */
	private KeplerLSID _lsid;

	/**
	 * Semantic Types assigned to this actor.
	 */
	private Vector<String> _semanticTypes;

	/**
	 * The Class Name of this actor.
	 */
	private String _className;

	/**
	 * The root name (i.e. the name of the root xml node) for this actor.
	 */
	private String _rootName;
	
	/**
	 * The full String representation of the MoML for this actor.
	 */
	private String _actorString;

	
	public KeplerActorMetadata() {
		_semanticTypes = new Vector<String>();
	}
	
	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public KeplerLSID getLsid() {
		return _lsid;
	}

	public void setLsid(KeplerLSID lsid) {
		_lsid = lsid;
	}

	public Vector<String> getSemanticTypes() {
		return _semanticTypes;
	}
	
	public void addSemanticType(String semanticType) {
		_semanticTypes.add(semanticType);
	}
	
	public void removeSemanticType(String semanticType) {
		_semanticTypes.remove(semanticType);
	}

	public void setSemanticTypes(Vector<String> semanticTypes) {
		_semanticTypes = semanticTypes;
	}

	public String getClassName() {
		return _className;
	}

	public void setClassName(String className) {
		_className = className;
	}

	public String getRootName() {
		return _rootName;
	}

	public void setRootName(String rootName) {
		_rootName = rootName;
	}

	public String getActorString() {
		return _actorString;
	}

	public void setActorString(String actorString) {
		_actorString = actorString;
	}
	
	public void addAttribute(String name, String value) {
		if (_attributes == null) {
			_attributes = new HashMap<String, String>();
		}
		_attributes.put(name, value);
	}
	
	public String getAttribute(String name) {
		return _attributes.get(name);
	}
	
	public Map<String, String> getAttributes() {
		return _attributes;
	}

	public String debugString() {
		String d = "";
		d += "Name: " + getName() + "\n";
		d += "ClassName: " + getClassName() + "\n";
		d += "LSID: " + getLsid() + "\n";
		d += "RootName: " + getRootName() + "\n";
		for (String st : getSemanticTypes()) {
			d += "SemanticType: " + st + "\n";
		}
		//d += "ACTORSTRING: " + getActorString() + "\n";
		return d;
	}

	private Map<String, String> _attributes = null;
}
