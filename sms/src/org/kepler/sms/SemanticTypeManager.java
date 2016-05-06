/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.kepler.sms;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 */

public class SemanticTypeManager {
	private static final Log log = LogFactory.getLog(SemanticTypeManager.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	// //////////////////////////////////////////////////////////////////////////////
	// PUBLIC CONSTRUCTOR

	public SemanticTypeManager() {
	}

	// //////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS

	/**
	 * Check if the given object is being managed by this manager
	 * 
	 * @param obj
	 *            the object to check
	 * @return true if the object is being managed by the manager, false
	 *         otherwise
	 */
	public boolean isObject(Object obj) {
		if (obj != null && _objectTable.containsKey(obj))
			return true;
		return false;
	}

	/**
	 * Add and object to this manager
	 * 
	 * @param obj
	 *            the object being added
	 */
	public void addObject(Object obj) {
		if (isObject(obj))
			return;
		_objectTable.put(obj, new Vector<Object>());
	}

	/**
	 * Remove an object from this manager
	 * 
	 * @param obj
	 *            the object to remove
	 */
	public void removeObject(Object obj) {
		if (isObject(obj))
			_objectTable.remove(obj);
	}

	/**
	 * Obtain all objects in this manager.
	 * 
	 * @return the set of objects being managed by this manger.
	 */
	public Vector<Object> getObjects() {
		return new Vector<Object>(_objectTable.keySet());
	}

	/**
	 * Assign a concept id to the given object.
	 * 
	 * @param obj
	 *            the object to assign the concept to
	 * @param concept
	 *            the concept (class) to assign to the object
	 */
	public void addType(Object obj, Object concept) {
		if (!isObject(obj) || concept == null)
			return;
		Vector<Object> types = getTypes(obj);
		if (!types.contains(concept))
			types.add(concept);
	}

	/**
	 * Remove a concept id from the given object.
	 * 
	 * @param obj
	 *            the object to remove the concept from
	 * @param concept
	 *            the concept to remove
	 */
	public void removeType(Object obj, String concept) {
		if (!isObject(obj) || concept == null)
			return;
		Vector<Object> types = getTypes(obj);
		if (types.contains(concept))
			types.remove(concept);
	}

	public void removeTypes(Object obj) {
		if (!isObject(obj))
			return;

		_objectTable.remove(obj);
		_objectTable.put(obj, new Vector<Object>());
	}

	/**
	 * Check if a concept id is assigned to an object in this manager.
	 * 
	 * @param obj
	 *            the object to check
	 * @param concept
	 *            the type to check for
	 * @return true if concept is assigned to obj, false otherwise
	 */
	public boolean isType(Object obj, String concept) {
		if (!isObject(obj) || concept == null)
			return false;
		Iterator<Object> types = getTypes(obj).iterator();
		while (types.hasNext())
			if (concept.equals(types.next()))
				return true;
		return false;
	}

	/**
	 * Obtain the set of types assigned to the given object
	 * 
	 * @param obj
	 *            the object to return the types for
	 * @return the types assigned to the object, or an empty list if the object
	 *         is not managed by this manager.
	 */
	public Vector<Object> getTypes(Object obj) {
		if (!isObject(obj))
			return new Vector<Object>();
		return (Vector<Object>) _objectTable.get(obj);
	}
	
	
	public Vector<NamedOntClass> getTypesAsNamedOntClasses(Object obj) {
		Vector<NamedOntClass> results = new Vector<NamedOntClass>();

		if (!isObject(obj)){
			return results;
		}
		
		Vector semTypes = getTypes(obj);
		Iterator semTypesItr = semTypes.iterator();
		while (semTypesItr.hasNext()){
			Object semType = semTypesItr.next();
			if (semType instanceof NamedOntClass){
				NamedOntClass noc = (NamedOntClass)semType;
				results.add(noc);
			}
		}
		
		return results;
	}
	

	/**
	 * Removes all objects from this manager that have no associated types
	 */
	public void pruneUntypedObjects() {
		Vector<Object> removeList = new Vector<Object>();
		Iterator<Object> objsIter = getObjects().iterator();
		while (objsIter.hasNext()) {
			Object obj = objsIter.next();
			Vector<Object> types = getTypes(obj);
			if (types.size() == 0)
				removeList.add(obj);
		}
		Iterator<Object> iter = removeList.iterator();
		while (iter.hasNext())
			removeObject(iter.next());
	}

	/**
	 * Create a saved state (memento) of this manager.
	 * 
	 * @return the created memento
	 */
	public SemanticTypeManagerMemento createMemento() {
		return new SemanticTypeManagerMemento(this);
	}

	/**
	 * Set this managers state to the state saved by the memento.
	 * 
	 * @param memento
	 *            the memento whose state is being loaded
	 */
	public void setMemento(SemanticTypeManagerMemento memento) {
		SemanticTypeManager manager = memento.getState();
		_objectTable = new Hashtable<Object, Vector<Object>>();
		if (manager == null)
			return;
		Iterator<Object> objIter = manager.getObjects().iterator();
		while (objIter.hasNext()) {
			Object obj = objIter.next();
			addObject(obj);
			Vector<Object> types = manager.getTypes(obj);
			Iterator<Object> typeIter = types.iterator();
			while (typeIter.hasNext())
				addType(obj, (String) typeIter.next());
		}
	}

	/**
	 * Check if the manager of the given memento is different than this manager.
	 * 
	 * @param memento
	 *            the memento to check against
	 * @return true if this manager is different than the memento's, false
	 *         otherwise
	 */
	public boolean isModified(SemanticTypeManagerMemento memento) {
		return !this.equals(memento.getState());
	}

	/**
	 * Create a clone of this manager
	 * 
	 * @return the clone
	 */
	public Object clone() {
		SemanticTypeManager m = new SemanticTypeManager();
		m._objectTable = (Hashtable<Object, Vector<Object>>) _objectTable
				.clone();
		return m;
	}

	/**
	 * Check if the given manager is equivalent to this manager. Two managers
	 * are equal if they contain the same objects, and the objects have the same
	 * types.
	 * 
	 * @param obj the object to test for equivalence
	 * @return true if the given object is equivalent to this manager
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof SemanticTypeManager))
			return false;

		SemanticTypeManager manager = (SemanticTypeManager) obj;
		Vector<Object> managerObjects = manager.getObjects();

		if (getObjects().size() != managerObjects.size())
			return false;

		Iterator<Object> objs = getObjects().iterator();
		while (objs.hasNext()) {
			Object o = objs.next();
			if (!managerObjects.contains(o))
				return false;
			Vector<Object> managerTypes = manager.getTypes(o);
			if (getTypes(o).size() != managerTypes.size())
				return false;
			Iterator<Object> types = getTypes(o).iterator();
			while (types.hasNext())
				if (!managerTypes.contains(types.next()))
					return false;
		}

		return true;
	}

	/**
	 * Constructs a string representation of this manager
	 * 
	 * @return a string representation of this manager
	 */
	public String toString() {
		String str = "";
		Iterator<Object> objs = getObjects().iterator();
		while (objs.hasNext()) {
			Object obj = objs.next();
			str += "object '" + obj + "' has types [";
			Iterator<Object> types = getTypes(obj).iterator();
			while (types.hasNext()) {
				Object type = types.next();
				str += type;
				if (types.hasNext())
					str += ", ";
			}
			str += "]";
			if (objs.hasNext())
				str += "\n";
		}
		return str;
	}

	// //////////////////////////////////////////////////////////////////////////////
	// PRIVATE MEMBERS

	private Hashtable<Object, Vector<Object>> _objectTable = new Hashtable<Object, Vector<Object>>();

	// //////////////////////////////////////////////////////////////////////////////
	// TESTING

	public static void main(String[] args) {
		SemanticTypeManager m1 = new SemanticTypeManager();
		String o1_1 = new String("p1");
		String o2_1 = new String("p2");
		String o3_1 = new String("p2/a");
		String c1_1 = new String("#t1");
		String c2_1 = new String("#t2");
		String c3_1 = new String("#t3");
		m1.addObject(o3_1);
		m1.addObject(o2_1);
		m1.addObject(o1_1);
		m1.addType(o1_1, c3_1);
		m1.addType(o1_1, c2_1);
		m1.addType(o1_1, c1_1);
		m1.removeType(o1_1, c3_1);
		m1.addType(o2_1, c2_1);
		m1.addType(o3_1, c3_1);
		m1.removeType(o3_1, c1_1);

		SemanticTypeManager m2 = new SemanticTypeManager();
		String o1_2 = new String("p1");
		String o2_2 = new String("p2");
		String o3_2 = new String("p2/a");
		String c1_2 = new String("#t1");
		String c2_2 = new String("#t2");
		String c3_2 = new String("#t3");
		m2.addObject(o1_2);
		m2.addObject(o2_2);
		m2.addObject(o3_2);
		m2.addType(o1_2, c1_2);
		m2.addType(o1_2, c2_2);
		m2.addType(o1_2, c3_2);
		// m2.removeType(o1_2, c3_2);
		m2.addType(o2_2, c2_2);
		m2.addType(o3_2, c3_2);

		System.out.println("\n m1 := \n" + m1.clone());
		System.out.println("\n m2 := \n" + m2.clone());

		System.out.println("\n m1.equals(m2) = " + m1.equals(m2));

		SemanticTypeManagerMemento mem = m1.createMemento();
		System.out.println("\n m1.isModified(mem) = " + m1.isModified(mem));

		String o4 = new String("p4");
		m1.addObject(o4);

		System.out.println("\n m1.isModified(mem) = " + m1.isModified(mem));

		m1.setMemento(mem);

		System.out.println(m1);

	}

}