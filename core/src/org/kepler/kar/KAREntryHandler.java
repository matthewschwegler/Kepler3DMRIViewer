/**
 *  '$RCSfile$'
 *  '$Author: barseghian $'
 *  '$Date: 2010-06-23 17:25:10 -0700 (Wed, 23 Jun 2010) $'
 *  '$Revision: 24970 $'
 *
 *  For Details:
 *  http://www.kepler-project.org
 *
 *  Copyright (c) 2010 The Regents of the
 *  University of California. All rights reserved. Permission is hereby granted,
 *  without written agreement and without license or royalty fees, to use, copy,
 *  modify, and distribute this software and its documentation for any purpose,
 *  provided that the above copyright notice and the following two paragraphs
 *  appear in all copies of this software. IN NO EVENT SHALL THE UNIVERSITY OF
 *  CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 *  OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
 *  DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY
 *  DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE
 *  SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.kepler.kar;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.gui.TableauFrame;

public interface KAREntryHandler {

	/**
	 * The getTypeName() method must return the type of object that this
	 * KAREntryHandler saves.  This method should return the KAR version 1.0
	 * type name or null if version 1.0 is not supported by this handler.
	 * 
	 * 	 */
	public String getTypeName();
	
	/**
	 * This method should return true if this KAREntryHandler can handle the
	 * specified type.  The type passed in is the binary class name of the file.
	 * 
	 * @param typeName
	 * 	 */
	public boolean handlesType(String typeName);
	
	/**
	 * The initialize method is called directly after instantiating this
	 * KAREntryHandler.
	 */
	public void initialize();
	
	/**
	 * This method should return a CacheObject that will be put into the cache.
	 * Once all the contents of the KAR exist in the cache then the open method
	 * is called.  In this way each entry in the kar can have access to all the
	 * other entries through the cache when they get opened.
	 * 
	 * @param karFile
	 * @param entry
	 * @return the CacheObject that will be put into the CacheManager
	 * @throws Exception
	 */
	public CacheObject cache(KARFile karFile, KAREntry entry) throws Exception;

	/**
	 * When a KAR file is opened, any entries in the KAR file that have the same
	 * type as this KAREntryHandler will be passed to this open method.  This
	 * method will always be called after the cache method.
	 * 
	 * @param karFile
	 * @param entry
	 * @param tableauFrame
	 * @return boolean true if the entry was opened successfully.
	 * @throws Exception
	 */
	public boolean open(KARFile karFile, KAREntry entry, TableauFrame tableauFrame) throws Exception;

	/**
	 * Return an array of KAREntry objects that are to be saved for the given
	 * lsids. 
	 * 
	 * @param lsid
	 * @param karLsid the lsid of the containing KAR
	 * @param tableauFrame
	 * @return an array of KAREntries to be saved with this LSID object.
	 * @throws Exception
	 */
	public Hashtable<KAREntry,InputStream> save(Vector<KeplerLSID> lsids, KeplerLSID karLsid, 
			TableauFrame tableauFrame) throws Exception;
}
