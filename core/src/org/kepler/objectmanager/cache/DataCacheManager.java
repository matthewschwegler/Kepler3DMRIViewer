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

import java.util.zip.CRC32;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.ObjectManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.objectmanager.lsid.LSIDGenerator;

public class DataCacheManager {

	private static final Log log = LogFactory.getLog(ObjectManager.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private static DataCacheManager mDataCacheManager = new DataCacheManager();

	
	private CacheManager cm;

	private DataCacheManager() {
		try {
			cm = CacheManager.getInstance();
		} catch (CacheException e) {
			log.error("Cannot create CacheManager", e);
		}
	}

	/**
	 * Get reference to the singleton DataCacheManager object.
	 * 
	 * 	 */
	public static DataCacheManager getInstance() {
		return mDataCacheManager;
	}

	/**
	 * 
	 * @param dobj
	 */
	public synchronized static void removeItem(DataCacheObject dobj) {
		try {
			getInstance().cm.removeObject(dobj.getLSID());
		} catch (CacheException e) {
			log.error("Cannot remove DataCacheObject " + dobj.toString());
		}
	}

	/**
	 * 
	 * @param aPhysicalFileName
	 * @param aLogicalName
	 * @param aType
	 * @param aFileLocation
	 * 	 */
	public synchronized static DataCacheFileObject putFile(
			String aPhysicalFileName, String aLogicalName, String aType,
			int aFileLocation) {
		try {
			return mDataCacheManager.putFileInternal(aPhysicalFileName,
					aLogicalName, aType, aFileLocation);
		} catch (Exception e) {
			log.error("Cannot complete putFile", e);
		}
		return null;
	}

	private DataCacheFileObject putFileInternal(String aPhysicalFileName,
			String aLogicalName, String aType, int aFileLocation)
			throws Exception {

		String magicString = aLogicalName + "|" + aType;
		KeplerLSID lsid = getDataLSID(magicString);
		if (isDebugging) log.debug(lsid.toString());
		DataCacheFileObject dfo = new DataCacheFileObject();
		dfo.initializeWithFileName(aPhysicalFileName, aLogicalName, aType,
				aFileLocation);
		dfo.setLSID(lsid);
		cm.insertObject(dfo);

		return dfo;
	}

	/**
	 * 
	 * Returns NULL if the object is not in the cache.
	 * 
	 * @param aName
	 *            Name of file to retrieve from cache.
	 * @return The DataCacheFileObject or null if not found in cache.
	 */
	public synchronized static DataCacheFileObject getFile(String aLogicalName,
			String aType) {
		try {
			return mDataCacheManager.getFileInternal(aLogicalName, aType);
		} catch (Exception e) {
			log.error("Cannot complete getFile", e);
		}
		return null;
	}

	private DataCacheFileObject getFileInternal(String aName, String aResource)
			throws Exception {
		if (isDebugging) log.debug("getFileInternal("+aName+", "+aResource+")");

		//String magicString = aName + "|" + aResource;

		KeplerLSID lsid = LSIDGenerator.getInstance().getNewLSID();
		if (isDebugging) log.debug( lsid.toString() );
		DataCacheFileObject item = (DataCacheFileObject) cm.getObject(lsid);
		return item;
	}

	/**
	 * 
	 * @param aListener
	 * @param aName
	 * @param aResourceName
	 * @param aClassName
	 * 	 */
	public synchronized static DataCacheObject getCacheItem(
			DataCacheListener aListener, String aName, String aResourceName,
			String aClassName) {
		return getCacheItem(aListener, aName, null, aResourceName, aClassName);
	}
	
	/**
	 * 
	 * @param aListener
	 * @param aName
	 * @param aIdentifier
	 * @param aResourceName
	 * @param aClassName
	 * 	 */
	public synchronized static DataCacheObject getCacheItem(
			DataCacheListener aListener, String aName, String aIdentifier, String aResourceName,
			String aClassName) {

		try {
			return mDataCacheManager.getCacheItemInternal(aListener, aName, aIdentifier,
					aResourceName, aClassName);
		} catch (Exception e) {
			log.error("Error with Cache");
		}
		return null;
	}
	
	/**
	 * Return a temporary local LSID based on a string.
	 * @param magicstring
	 * 	 * @throws Exception
	 */
	private KeplerLSID getDataLSID(String magicstring) throws Exception {
		CRC32 c = new CRC32();
		c.update(magicstring.getBytes());
		String hexValue = Long.toHexString(c.getValue());
		KeplerLSID lsid = new KeplerLSID("localdata",hexValue,0L,0L);
		return lsid;
	}

	private DataCacheObject getCacheItemInternal(DataCacheListener aListener,
			String aName, String aIdentifier, String aResourceName, String aClassName)
			throws Exception {
		if (isDebugging) log.debug("getCacheItemInternal("+aListener+", "+aName+", "+aResourceName+", "+aClassName+")");

		String magicString = aName + "|" + aResourceName;
		if (aIdentifier != null) {
			magicString = aName + "|" + aIdentifier + "|" + aResourceName;
		}
		KeplerLSID lsid = getDataLSID(magicString);
		if (isDebugging) log.debug( lsid.toString() );

		DataCacheObject dobj = (DataCacheObject) cm.getObject(lsid);
		if (dobj != null) {
			dobj.addListener(aListener);
			return dobj;
		}

		// dboj == null means it was not in the cache. We create a new one and
		// register it.

		try {
			Class clazz = Class.forName(aClassName);
			dobj = (DataCacheObject) clazz.newInstance();
			dobj.setLSID(lsid);
			dobj.setName(aName);
			dobj.addListener(aListener);
			dobj.setName(aName);
			dobj.setResourceName(aResourceName);
			dobj.setBaseFileName(lsid.createFilename());
			cm.insertObject(dobj);

			return dobj;
		} catch (Exception e1) {
			log.error("Unable to create new object", e1);
		}

		return null;
	}

}