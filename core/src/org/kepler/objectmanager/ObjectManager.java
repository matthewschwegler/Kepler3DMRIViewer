/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2011-03-02 11:59:11 -0800 (Wed, 02 Mar 2011) $' 
 * '$Revision: 27233 $'
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

package org.kepler.objectmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.cache.CacheException;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.objectmanager.cache.CacheObjectInterface;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.NamedObj;

/**
 * The ObjectManager searches through all of the NamedObj Objects in the
 * workspace to see if any of them have a KeplerLSID associated with them.
 * 
 * @author Aaron Schultz
 * @version $Id: ObjectManager.java 27233 2011-03-02 19:59:11Z aschultz $
 * 
 */
public class ObjectManager {

	private static final Log log = LogFactory.getLog(ObjectManager.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	// WeakHashMap will discard items required during KAR save process
	// (refactored so this is probably only the workflow now).
	// see: http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5200
	//private WeakHashMap<KeplerLSID, NamedObj> _namedObjs;
	private Map<KeplerLSID, NamedObj> _namedObjs;

	/**
	 * Empty constructor
	 */
	public ObjectManager() {
		//_namedObjs = new WeakHashMap<KeplerLSID, NamedObj>(1);
		HashMap<KeplerLSID, NamedObj> hashMap = new HashMap<KeplerLSID, NamedObj>(1);
		_namedObjs = Collections.synchronizedMap(hashMap);
	}

	/**
	 * Add a NamedObject to the ObjectManager. These are searched when getting
	 * an object by LSID.
	 * 
	 * @param namedObj
	 * @throws Exception
	 */
	public void addNamedObj(NamedObj namedObj) throws Exception {
		KeplerLSID lsid = NamedObjId.getIdFor(namedObj);
		//Set<KeplerLSID> keys = _namedObjs.keySet();
		//Collection<NamedObj> values = _namedObjs.values();
		if (_namedObjs.containsKey(lsid)) {
			_namedObjs.remove(lsid);
		}
		_namedObjs.put(lsid, namedObj);
	}


	/**
	 * Attempt to remove NamedObj from ObjectManager: 
	 * if getIdFor(namedObj) returns an LSID for an object 
	 * in the ObjectManager, it will be removed.
	 * 
	 * @param namedObj
	 * @return removed NamedObj, or null if none.
	 */
	public NamedObj removeNamedObj(NamedObj namedObj) {
		KeplerLSID lsid = NamedObjId.getIdFor(namedObj);
		return _namedObjs.remove(lsid);
	}
	
	/**
	 * Attempt to remove NamedObj from ObjectManager: 
	 * if getIdFor(namedObj) returns an LSID for an object 
	 * in the ObjectManager, it will be removed.
	 * 
	 * @param namedObj
	 * @return removed NamedObj, or null if none.
	 */
	public ArrayList<NamedObj> removeNamedObjs(NamedObj namedObj) {
		KeplerLSID lsid = NamedObjId.getIdFor(namedObj);
		//it is modified because of bug // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5095#c14
                // the reason is because the lsid of an updated namedObj is changed, the old method won't remove the old versions of namedObj.
		//return _namedObjs.remove(lsid);
		return removeObjectsForLSIDWithoutRevision(lsid.toStringWithoutRevision());
	}

	/**
	 * Attempts to remove object from ObjectManager: 
	 * if getObjectRevision(lsid) returns a NamedObj, calls removeNamedObj on
	 * this NamedObj.
	 * 
	 * NOTE: Chances are good this won't remove the NamedObj you want.
	 * 
	 * @param lsid
	 * @return removed NamedObj, or null if none.
	 * @throws Exception
	 */
	public NamedObj removeObject(KeplerLSID lsid) throws Exception {
		if (isDebugging)
			log.debug("removeObject(" + lsid + ")");
		NamedObj no = getObjectRevision(lsid);
		if (no != null) {
			return removeNamedObj(no);
		}
		return null;
	}

	/**
	 * @param lsid for the NamedObj to remove from ObjectManager
	 * @return the NamedObj removed, or null if none.
	 */
	public NamedObj removeObjectForLSID(KeplerLSID lsid){
		return _namedObjs.remove(lsid);
	}
	
	/**
	 * Remove all NamedObjects from ObjectManager that match this lsid without
	 * revision.
	 * @param lsidWithoutRevision
	 * @return ArrayList of NamedObjs that were removed.
	 */
	public ArrayList<NamedObj> removeObjectsForLSIDWithoutRevision(String lsidWithoutRevision){
		ArrayList<NamedObj> removedNamedObjects = new ArrayList<NamedObj>();
		Iterator<KeplerLSID> lsidItr = _namedObjs.keySet().iterator();
		ArrayList<KeplerLSID> lsidsToRemove = new ArrayList<KeplerLSID>();
		while (lsidItr.hasNext()){
			KeplerLSID lsid = lsidItr.next();
			if (lsid.toStringWithoutRevision().equals(lsidWithoutRevision)){
				lsidsToRemove.add(lsid);
			}
		}
		
		lsidItr = lsidsToRemove.iterator();
		while(lsidItr.hasNext()){
			KeplerLSID lsid = lsidItr.next();
			NamedObj removedNamedObj = _namedObjs.remove(lsid);
			removedNamedObjects.add(removedNamedObj);
		}
		
		return removedNamedObjects;
	}

	/**
	 * 
	 * @param lsid
	 * @return
	 * @throws Exception
	 */
	public String getObjectType(KeplerLSID lsid) throws Exception {
		if (isDebugging)
			log.debug("getObjectType(" + lsid + ")");
		NamedObj no = getObjectRevision(lsid);
		if (no == null) {
			return null;
		}
		return no.getClass().getName();
	}

	/**
	 * 
	 * @param lsid
	 * @return
	 * @throws Exception
	 */
	public NamedObj getObjectRevision(KeplerLSID lsid) throws Exception {
		if (isDebugging) {
			log.debug("getObjectRevision(" + lsid + ")");
			printDebugInfo();
		}

		boolean matchRevision = true;

		NamedObj obj = getObjectFromManager(lsid, matchRevision);
		if (obj == null) {
			if (isDebugging)
				log.debug(lsid + " is not registered with ObjectManager");
			obj = getObjectFromCache(lsid);
			if (obj == null) {
				if (isDebugging)
					log.debug(lsid + " is not registered with CacheManager");
			}
		}
		
		if (obj != null) {
			if (isDebugging)
				log.debug(obj.getName() + " was found");
			// Make sure it has the NamedObjId Attribute
			NamedObjId noi = NamedObjId.getIdAttributeFor(obj);
			if (noi == null) {
				NamedObjId lsidSA = new NamedObjId(obj, NamedObjId.NAME);
				lsidSA.setExpression(lsid.toString());
			} else {
				KeplerLSID lsidCheck = noi.getId();
				if (!lsidCheck.equals(lsid)) {
					log.error("lsids don't match: " + lsid.toString() + " != " + lsidCheck);
					noi.setExpression(lsid.toString());
				}
			}
		}
		
		return obj;
	}

	/**
	 * Return the NamedObj that has the highest revision number for a given LSID
	 * after searching through all the NamedObjs accessible by the
	 * ObjectManager. If no NamedObj is found at all then try to get the highest
	 * revision from the cache. If that doesn't find anything then return null.
	 * This method will replace the existing getObject(KeplerLSID) method
	 * 
	 * @param lsid
	 * @return
	 * @throws Exception
	 */
	public NamedObj getHighestObjectRevision(KeplerLSID lsid) throws Exception {
		boolean matchRevision = false;

		Vector<NamedObj> allObjs = new Vector<NamedObj>();

		for (NamedObj namedObj : _namedObjs.values()) {
			if (namedObj instanceof CompositeEntity) {
				Vector<NamedObj> theNOS = findAll(lsid,
						(CompositeEntity) namedObj, matchRevision);

				if (theNOS != null) {
					for (NamedObj no : theNOS) {
						allObjs.add(no);
					}
				}
			} else {
				if (NamedObjId.idMatches(lsid, namedObj, matchRevision)) {
					allObjs.add(namedObj);
				}
			}
		}

		NamedObj noWithHighestLSID = null;
		for (NamedObj no : allObjs) {
			if (noWithHighestLSID == null) {
				noWithHighestLSID = no;
			} else {
				NamedObjId idAtt = NamedObjId.getIdAttributeFor(no);
				if (idAtt != null) {
					KeplerLSID objId = idAtt.getId();
					
					NamedObjId noWithHighestIdAtt = NamedObjId.getIdAttributeFor(noWithHighestLSID);
					KeplerLSID noWithHighestObjId = noWithHighestIdAtt.getId();
					
					if (objId.getRevision() > noWithHighestObjId.getRevision()) {
						noWithHighestLSID = no;
					}
				}
			}
		}

		if (noWithHighestLSID == null) {
			// get the highest revision from the CacheManager
			try {
				CacheObject co = CacheManager.getInstance().getHighestCacheObjectRevision(lsid);
				if (co != null){
					Object o = co.getObject();
					if (o instanceof NamedObj) {
						noWithHighestLSID = (NamedObj) o;
					}
				}
			} catch (CacheException ce) {
				ce.printStackTrace();
			}

		}

		return noWithHighestLSID;
	}

	/**
	 * Return all of the NamedObjs contained by the given CompositeEntity that
	 * match the given lsid either with or without matching the revision as
	 * specified.
	 * 
	 * @param lsid
	 * @param composite
	 * @param matchRevision
	 * @return
	 */
	private Vector<NamedObj> findAll(KeplerLSID lsid,
			CompositeEntity composite, boolean matchRevision) {
		Vector<NamedObj> objs = new Vector<NamedObj>(3);

		if (NamedObjId.idMatches(lsid, (NamedObj) composite, matchRevision)) {
			objs.add((NamedObj) composite);
		}

		for (Object obj : composite.entityList()) {
			if (obj instanceof NamedObj) {
				if (NamedObjId.idMatches(lsid, (NamedObj) obj, matchRevision)) {
					objs.add((NamedObj) obj);
				}
				if (obj instanceof CompositeEntity) {
					Vector<NamedObj> childObjs = findAll(lsid,
							(CompositeEntity) obj, matchRevision);
					if (childObjs != null && childObjs.size() > 0) {
						for (NamedObj o : childObjs) {
							objs.add(o);
						}
					}
				}
			}
		}

		if (objs.size() > 0) {
			return objs;
		}
		return null;
	}

	/**
	 * Return a NamedObj that has been added to the ObjectManager.
	 * 
	 * @param lsid
	 * @return
	 * @throws Exception
	 */
	private NamedObj getObjectFromManager(KeplerLSID lsid, boolean matchRevision)
			throws Exception {
		if (isDebugging)
			log.debug("getObjectFromManager(" + lsid + "," + matchRevision
					+ ")");
		for (NamedObj namedObj : _namedObjs.values()) {
			if (namedObj instanceof CompositeEntity) {
				NamedObj theNO = checkComposite(lsid,
						(CompositeEntity) namedObj, matchRevision);

				if (theNO != null) {
					if (isDebugging)
						log.debug("Found NamedObj: " + theNO.getName() + " "
								+ NamedObjId.getIdFor(theNO));
					return theNO;
				}
			} else {
				if (NamedObjId.idMatches(lsid, namedObj, matchRevision)) {
					return namedObj;
				}
			}
		}
		return null;
	}

	/**
	 * Return a NamedObj that has been instantiated from the CacheManager.
	 * 
	 * @param lsid
	 * @return
	 * @throws Exception
	 */
	private NamedObj getObjectFromCache(KeplerLSID lsid) throws Exception {
		NamedObj obj = null;

		CacheObjectInterface coi = CacheManager.getInstance().getObject(lsid);
		if (coi != null) {
			if (isDebugging)
				log.debug("CacheObjectInterface found in CacheManager");
			Object o = coi.getObject();
			if (o != null) {
				if (isDebugging) {
					log.debug(o.getClass().getName());
				}
				if (o instanceof NamedObj) {
					obj = (NamedObj) o;
				} else if (o instanceof ActorMetadata) {
					ActorMetadata am = (ActorMetadata) o;
					obj = am.getActor();
				}
			}
		}
		return obj;

	}

	/**
	 * Deeply recurse all entities of the given composite entity and return the
	 * NamedObj that matches the search LSID
	 */
	private NamedObj checkComposite(KeplerLSID lsid, CompositeEntity composite,
			boolean matchRevision) {
		if (isDebugging)
			log.debug("checkComposite(" + composite.getName() + ")");

		if (NamedObjId.idMatches(lsid, (NamedObj) composite, matchRevision)) {
			return (NamedObj) composite;
		}

		for (Object obj : composite.entityList()) {
			if (obj instanceof NamedObj) {
				if (isDebugging)
					log.debug(((NamedObj) obj).getName());
				if (NamedObjId.idMatches(lsid, (NamedObj) obj, matchRevision)) {
					return (NamedObj) obj;
				}
				if (obj instanceof CompositeEntity) {
					NamedObj theObj = checkComposite(lsid,
							(CompositeEntity) obj, matchRevision);
					if (theObj != null) {
						return theObj;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Print the objects that are currently registered with the ObjectManager.
	 */
	public void printDebugInfo() {
		System.out.println("*************"
				+ " NamedObjs registered with ObjectManager:");
		Iterator<KeplerLSID> lsidItr = _namedObjs.keySet().iterator();
		while (lsidItr.hasNext()){
			KeplerLSID lsid = lsidItr.next();
			System.out.println(_namedObjs.get(lsid).getName()+"=>"+lsid);
		}
		
		System.out.println("** getIdFor these NamedObj returns:");
		for (NamedObj no : _namedObjs.values()) {
			System.out.println(no.getName() + " getIdFor("+no.getName()+"):" + NamedObjId.getIdFor(no));
		}

		System.out.println("*************\n");

	}

	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static ObjectManager getInstance() {
		return ObjectManagerHolder.INSTANCE;
	}

	private static class ObjectManagerHolder {
		private static final ObjectManager INSTANCE = new ObjectManager();
	}
}
