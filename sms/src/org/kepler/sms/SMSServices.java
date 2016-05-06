/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:23:53 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31128 $'
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.kepler.moml.NamedObjId;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedIOPort;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

//////////////////////////////////////////////////////////////////////////
//// SMSServices
/**
 * This class provides a set of operations, or services for interacting with the
 * Semantic Mediation System. These services are currently only partially
 * defined.
 * 
 * @author Shawn Bowers
 * @version $Id: SMSServices.java 31128 2012-11-26 22:23:53Z crawl $
 * @since Kepler alpha
 */

public class SMSServices {

	public static int COMPATIBLE = 1;
	public static int UNKNOWN = 0;
	public static int INCOMPATIBLE = -1;
	
	private static List<SMSTagChangeListener> listeners = new ArrayList<SMSTagChangeListener>();
	
	public static void addListener(SMSTagChangeListener listener) {
		listeners.add(listener);
	}
	
	public static void removeListener(SMSTagChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * For use within an extended SCIA tool
	 */
	// public String getSemanticCorrespondences(Actor source, Actor target)

	/**
	 * Compare the compatibility of the two sets of semantic types. Both
	 * arguements represent conjoined sets of semantic types. The method returns
	 * three possible values: compatible, unknown, or incompatible. If either
	 * type is empty, unknown is returned. The types are considered compatible
	 * if they are non-empty and if the conjunction of the first set implies the
	 * conjunction of the second set. The types are considered incompatible if
	 * they are not compatible and not unkown.
	 * 
	 * @param semSubtypes
	 *            The semantic types that when conjoined form a sub-class of the
	 *            super type (semSupertypes).
	 * @param semSupertypes
	 *            The semantic types that when conjoined form a super-class of
	 *            the sub type (subSemTypes)
	 * @return Answers {@link #COMPATIBLE} if the inputs are compatible,
	 *         {@link #UNKNOWN} if the inputs are unkown, and
	 *         {@link INCOMPATIBLE} if the types are incompatible.
	 * 
	 *         FIXME: Need to somehow handle the case when the semtype is not
	 *         available locally: Can we assume they are known here? Do we need
	 *         to call the object manager? Do we throw an exception? For now, we
	 *         just ignore them!
	 */
	public static int compare(Vector semSubtypes, Vector semSupertypes) {
		OntologyCatalog catalog = OntologyCatalog.instance();
		Vector<NamedOntClass> subClasses = new Vector<NamedOntClass>();
		Vector<NamedOntClass> superClasses = new Vector<NamedOntClass>();

		// first check if either is empty; and if so return unknown
		if (semSubtypes.size() == 0 || semSupertypes.size() == 0)
			return UNKNOWN;

		// convert to ont classes; if we don't have knowledge of the
		// class, then don't add it ...
		for (Iterator iter = semSubtypes.iterator(); iter.hasNext();) {
			NamedOntClass c = catalog.getNamedOntClass((SemanticType) iter
					.next());
			if (c != null && !subClasses.contains(c))
				subClasses.add(c); // ignore unknown types
		}
		for (Iterator iter = semSupertypes.iterator(); iter.hasNext();) {
			NamedOntClass c = catalog.getNamedOntClass((SemanticType) iter
					.next());
			if (c != null && !superClasses.contains(c))
				superClasses.add(c); // ignore unkown types
		}

		// if we don't have any classes, return unknown
		if (subClasses.size() == 0 || superClasses.size() == 0)
			return UNKNOWN;

		// if the sem-subtypes contain a contradiction, then return
		// compatible (i.e., false implies anything)
		for (Iterator<NamedOntClass> iter = subClasses.iterator(); iter.hasNext();) {
			NamedOntClass cls = iter.next();
			for (Iterator<NamedOntClass> iter2 = subClasses.iterator(); iter.hasNext();) {
				NamedOntClass tstCls = iter.next();
				if (cls.isDisjointWith(tstCls))
					return COMPATIBLE;
			}
		}

		// if the sem-supertypes contain a contradiction, then return
		// incompatible, (i.e., we have true implies false)
		for (Iterator<NamedOntClass> iter = superClasses.iterator(); iter.hasNext();) {
			NamedOntClass cls = iter.next();
			for (Iterator<NamedOntClass> iter2 = superClasses.iterator(); iter.hasNext();) {
				NamedOntClass tstCls = iter.next();
				if (cls.isDisjointWith(tstCls))
					return INCOMPATIBLE;
			}
		}

		// check that every supertype has a corresponding subtype
		for (Iterator<NamedOntClass> iter = superClasses.iterator(); iter.hasNext();) {
			NamedOntClass superClass = iter.next();
			boolean found = false;
			for (Iterator<NamedOntClass> iter2 = subClasses.iterator(); iter2.hasNext();) {
				NamedOntClass subClass = iter2.next();
				if (superClass.isEquivalent(subClass)
						|| superClass.isSubClass(subClass) || superClass.equals(subClass))
					found = true;
			}
			if (!found)
				return INCOMPATIBLE;
		}

		return COMPATIBLE;
	}

	/**
     *
     */
	public static boolean compatible(Vector semSubtypes, Vector semSupertypes) {
		return compare(semSubtypes, semSupertypes) == COMPATIBLE;
	}

	/**
     * 
     */
	public static NamedOntClass getNamedOntClassFor(SemanticType semtype) {
		OntologyCatalog catalog = OntologyCatalog.instance();
		return catalog.getNamedOntClass(semtype);
	}

	/**
     * 
     */
	public static NamedOntClass getNamedOntClassFor(String conceptId) {
		OntologyCatalog catalog = OntologyCatalog.instance();
		return catalog.getNamedOntClass(conceptId);
	}

	/**
     *
     */
	public static Vector<SemanticType> getActorSemanticTypes(NamedObj obj) {
		Vector<SemanticType> result = new Vector<SemanticType>();
		List<SemanticType> semTypes = obj.attributeList(SemanticType.class);
		Iterator<SemanticType> iter = semTypes.iterator();
		while (iter.hasNext())
			result.add(iter.next());
		return result;
	}

	/**
	 * Set given NamedObj's SemanticTypes to namedOntClasses
	 * 
	 * @param obj
	 *            The named object (actor) to set the types on
	 * @param namedOntClasses
	 *            Set named object's SemanticTypes to these
	 */
	public static void setActorSemanticTypes(NamedObj obj, Vector<NamedOntClass> namedOntClasses) {
		
		
		final List<NamedOntClass> removedClasses = new ArrayList<NamedOntClass>();
		final List<NamedOntClass> addedClasses = new ArrayList<NamedOntClass>();

		// make sure obj is not null
		if (obj == null){
			return;
		}
		// make sure namedOntClasses is not null; is so create an empty vector
		if (namedOntClasses == null){
			namedOntClasses = new Vector<NamedOntClass>();
		}

		Iterator<SemanticType> existingItr = getActorSemanticTypes(obj).iterator();
		Iterator<NamedOntClass> namedOntItr = namedOntClasses.iterator();
		
		// ADD semTypes that aren't already there (those in namedOntClasses and not in existingSemTypes)
		while (namedOntItr.hasNext()) {
			boolean semTypeAlreadyExists = false;
			NamedOntClass cls = namedOntItr.next();			
			SemanticType s = null;
			while (existingItr.hasNext()){
				s = existingItr.next();
				if (s.getConceptId().equals(cls.getConceptId())){
					semTypeAlreadyExists = true;
					break;
				}
			}
			
			if (!semTypeAlreadyExists){
				try {
					SemanticType st = new SemanticType(obj, obj
							.uniqueName("semanticType"));
					if (cls != null) {
						st.setConceptId(cls.getConceptIdWithLabel());
						///12.21.10 Would this be more appropriate?:
						///st.setConceptId(cls.getConceptId());
						///st.setLabel(cls.getName());
						
						// update LSID revision
						NamedObjId noi = NamedObjId.getIdAttributeFor(obj);
						if (noi != null) {
							noi.updateRevision();
						}
						
						addedClasses.add(cls);
						
					} else
						System.out
								.println(">>> [SMSServices, 238] Cannot find class: "
										+ cls);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		

		// REMOVE semantic types if necessary (those in existing, but not in namedOntClasses)
		existingItr = getActorSemanticTypes(obj).iterator();
		while (existingItr.hasNext()){
			SemanticType s = existingItr.next();
			namedOntItr = namedOntClasses.iterator();

			boolean needToRemove = true;
			while (namedOntItr.hasNext()) {
				NamedOntClass cls = namedOntItr.next();
				/// 12.21.10 Instead, should the label be utilized? ala:
				///if (s.getConceptId().equals(cls.getConceptIdWithLabel())){
				if (s.getConceptUri().equals(cls.getConceptId())){
					needToRemove = false;
				}
			}
			if (needToRemove){
				try {
					s.setContainer(null);
					// update LSID revision
					NamedObjId noi = NamedObjId.getIdAttributeFor(obj);
					if (noi != null) {
						noi.updateRevision();
					}
					removedClasses.add(NamedOntClass.createNamedOntClassFromURI(s.getConceptId()));
				} catch (IllegalActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NameDuplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// Right before we leave this method, let's notify the listeners
		for (SMSTagChangeListener listener : listeners) {
			listener.tagsChanged(obj, addedClasses, removedClasses);
		}
	}

	/**
	 * Get all the ports, both "real" and "virtual" from a given named object.
	 * 
	 * @param obj
	 *            the named object
	 * @return the set of ports for the object TODO: Return as IOPortWrapper
	 *          objects
	 */
	public static Vector<Object> getAllPorts(NamedObj obj) {
		Vector<Object> result = new Vector<Object>();
		Iterator objIter = obj.containedObjectsIterator();
		while (objIter.hasNext()) {
			Object item = objIter.next();
			if (item instanceof KeplerRefinementIOPort)
				result.add(item);
			else if (item instanceof TypedIOPort)
				result.add(item);
			else if (item instanceof KeplerCompositeIOPort)
				result.add(item);
		}
		return result;
	}

	public static Vector<Object> getAllOutputPorts(NamedObj obj) {
		Vector<Object> results = new Vector<Object>();
		Iterator<Object> portIter = getAllPorts(obj).iterator();
		while (portIter.hasNext()) {
			Object p = portIter.next();
			if (p instanceof KeplerCompositeIOPort) {
				if (((KeplerCompositeIOPort) p).isOutput())
					results.add(p);
			} else if (p instanceof KeplerRefinementIOPort) {
				if (((KeplerRefinementIOPort) p).isOutput())
					results.add(p);
			} else if (p instanceof IOPort) {
				if (((IOPort) p).isOutput())
					results.add(p);
			}
		}
		return results;
	}

	public static Vector<Object> getAllInputPorts(NamedObj obj) {
		Vector<Object> results = new Vector<Object>();
		Iterator<Object> portIter = getAllPorts(obj).iterator();
		while (portIter.hasNext()) {
			Object p = portIter.next();
			if (p instanceof KeplerCompositeIOPort) {
				if (((KeplerCompositeIOPort) p).isInput())
					results.add(p);
			} else if (p instanceof KeplerRefinementIOPort) {
				if (((KeplerRefinementIOPort) p).isInput())
					results.add(p);
			} else if (p instanceof IOPort) {
				if (((IOPort) p).isInput())
					results.add(p);
			}
		}
		return results;
	}

	/**
	 * Get all the io ports
	 * 
	 * @param obj
	 *            the named object
	 * @return the set of ports for the object TODO: Return as IOPortWrapper
	 *          objects
	 */
	public static Vector<Object> getIOPorts(NamedObj obj) {
		Vector<Object> result = new Vector<Object>();
		Iterator objIter = obj.containedObjectsIterator();
		while (objIter.hasNext()) {
			Object item = objIter.next();
			if (item instanceof IOPort)
				result.add(item);
		}
		return result;
	}

	/**
	 * Get all the existing refinement ports defiend for a given named object.
	 * 
	 * @param obj
	 *            the named object
	 * @return the set of ports for the object TODO: Return as IOPortWrapper
	 *          objects
	 */
	public static Vector<Object> getRefinementPorts(NamedObj obj) {
		Vector<Object> result = new Vector<Object>();
		Iterator objIter = obj.containedObjectsIterator();
		while (objIter.hasNext()) {
			Object item = objIter.next();
			if (item instanceof KeplerRefinementIOPort)
				result.add(item);
		}
		return result;
	}

	/**
	 * Get all the port bundles define for a given named object.
	 * 
	 * @param obj
	 *            the named object
	 * @return the set of ports for the object TODO: Return as IOPortWrapper
	 *          objects
	 */
	public static Vector<Object> getPortBundles(NamedObj obj) {
		Vector<Object> result = new Vector<Object>();
		Iterator objIter = obj.containedObjectsIterator();
		while (objIter.hasNext()) {
			Object item = objIter.next();
			if (item instanceof KeplerCompositeIOPort)
				result.add(item);
		}
		return result;
	}

	/**
	 * Given a port object (TODO: convert to IOPortWrapper), returns the
	 * semantic types assigned to the port.
	 * 
	 * @param port
	 *            the port object
	 * @return a set of semantic type objects
	 */
	public static Vector<SemanticType> getPortSemanticTypes(Object port) {
		Vector<SemanticType> result = new Vector<SemanticType>();
		Iterator<SemanticType> portIter = null;
		if (port instanceof IOPort)
			portIter = ((IOPort) port).attributeList(SemanticType.class)
					.iterator();
		else if (port instanceof KeplerRefinementIOPort)
			portIter = ((KeplerRefinementIOPort) port).attributeList(
					SemanticType.class).iterator();
		else if (port instanceof KeplerCompositeIOPort)
			portIter = ((KeplerCompositeIOPort) port).attributeList(
					SemanticType.class).iterator();

		if (portIter == null)
			return result;

		while (portIter.hasNext())
			result.add(portIter.next());

		return result;
	}

	/**
	 * Return all the input semantic types for the object.
	 */
	public static Vector<SemanticType> getAllOutputSemanticTypes(NamedObj obj) {
		Vector<SemanticType> result = new Vector<SemanticType>();
		Iterator<Object> portIter = getAllOutputPorts(obj).iterator();
		while (portIter.hasNext()) {
			Object port = portIter.next();
			Iterator<SemanticType> typeIter = null;
			if (port instanceof KeplerRefinementIOPort)
				typeIter = SMSServices.getPortSemanticTypes(
						(KeplerRefinementIOPort) port).iterator();
			else if (port instanceof KeplerCompositeIOPort)
				typeIter = SMSServices.getPortSemanticTypes(
						(KeplerCompositeIOPort) port).iterator();
			else if (port instanceof IOPort)
				typeIter = SMSServices.getPortSemanticTypes((IOPort) port)
						.iterator();
			while (typeIter.hasNext()) {
				SemanticType t = typeIter.next();
				if (!result.contains(t))
					result.add(t);
			}
		}
		return result;
	}

	/**
	 * Return all the input semantic types for the object.
	 */
	public static Vector<SemanticType> getAllInputSemanticTypes(NamedObj obj) {
		Vector<SemanticType> result = new Vector<SemanticType>();
		Iterator<Object> portIter = getAllInputPorts(obj).iterator();
		while (portIter.hasNext()) {
			Object port = portIter.next();
			Iterator<SemanticType> typeIter = null;
			if (port instanceof KeplerRefinementIOPort)
				typeIter = SMSServices.getPortSemanticTypes(
						(KeplerRefinementIOPort) port).iterator();
			else if (port instanceof KeplerCompositeIOPort)
				typeIter = SMSServices.getPortSemanticTypes(
						(KeplerCompositeIOPort) port).iterator();
			else if (port instanceof IOPort)
				typeIter = SMSServices.getPortSemanticTypes((IOPort) port)
						.iterator();
			while (typeIter.hasNext()) {
				SemanticType t = typeIter.next();
				if (!result.contains(t))
					result.add(t);
			}
		}
		return result;
	}

	/**
     * 
     */
	public static String exportSemanticAnnotation(Entity entity) {
		// We assume that annotation rules always have the same
		// parents. For example, given a port p1 :: {a = int, b =
		// int} we have the following:
		//
		// Annotating p1 with #m gives
		// val: $1 p1 => inst: $1 #m
		//
		// Annotating p1/a with #b gives
		// val: $1 p1, val: $2 $1/a => inst: $2 #b
		//
		// Annotation p1 linked to p1/2 via #p gives
		// val: $1 p1, val: $2 $1/a => prop: $1 #p $2
		//
		// Annotating generalization g1(p1/a, p1/b) with #d gives
		// val: $1 p1, val: $2 $1/a, val: $3 $1/b =>
		// inst: g1($2, $3) #d
		//
		// where a and b are assumed to be values within the same
		// port value
		//
		// Thus, we can't do "cross-product-style" annotation, e.g.,
		// given p1 with structural type {{a=int, b=int}} (a list of
		// records), we can't say things like:
		//
		// val: $1 p1, val: $2 $1/elem, val: $3 $1/elem,
		// val: $4 $2/a, val: $5 $3/b =>
		// inst: g1($4, $5) #d
		//
		// In particular, we would have instead:
		//
		// val: $1 p1, val: $2 $1/elem, val: $3 $2/a,
		// val: $4 $2/b =>
		// inst: g1($3, $4) #d
		//
		// That is, we assume that the components always have
		// the same parent
		//
		return null;
	}

	/**
     *
     */
	public static void importSemanticAnnotation(String annotation, Entity entity) {
	}

	// searching ...

} // SMSServices