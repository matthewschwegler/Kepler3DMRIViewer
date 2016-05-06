/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-06-16 11:22:01 -0700 (Mon, 16 Jun 2014) $' 
 * '$Revision: 32771 $'
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.library.LibItem;
import org.kepler.objectmanager.library.LibraryManager;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;

/**
 * This class returns moml given an LSID. It is instantiated via reflection when
 * the moml of a class is needed.
 * 
 *@author Dan Higgins
 *@created July 13, 2007
 */
public class GetMomlFromLSID {

	private static final Log log = LogFactory.getLog(GetMomlFromLSID.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/** Constructor */
	public GetMomlFromLSID() {
	}

	/**
	 * Get the MoMl for an actor represented by the NameObj 'dropObj'
	 */
	public String getMoml(NamedObj dropObj) {
		if (isDebugging) log.debug("getMoml("+dropObj.getName()+")");

		/**
		 * This boolean will tell us if this is just a plain old NamedObj as
		 * used in the Outline tab rather than if it is a special
		 * CompositeEntity placeholder object as is used in the Component
		 * Library.
		 */
		boolean isPlainOldNamedObj = false;
		
		String actorString = "";
		if (dropObj instanceof ComponentEntity) {
			try {
				int liid = LibraryManager.getLiidFor((ComponentEntity<?>)dropObj);
				if (isDebugging) log.debug(liid);
				if (liid >= 0) {
					LibItem li = LibraryManager.getInstance().getTreeItemIndexInformation(liid);
					
					CacheManager cacheMan = CacheManager.getInstance();
				
					String name = li.getName();
					ActorCacheObject aco = (ActorCacheObject) cacheMan
							.getObject(li.getLsid());
					if (aco == null) {
						throw new Exception("Object not found: " + li.getLsid());
					}
	
					ActorMetadata am = aco.getMetadata();
					if(am == null) {
						MessageHandler.error("Error getting metadata for component.");
					} else {
						NamedObj no = am.getActorAsNamedObj(null);
						actorString = no.exportMoML(name);
						if (isDebugging) log.debug("***************actorString: " + actorString);
						// this string has an xml header and a DOCTYPE declaration
						// following removes those elements
						int pos1 = actorString.indexOf("<!DOCTYPE");
						int pos2 = actorString.indexOf(">", pos1);
						int pos3 = actorString.indexOf("<", pos2);
						if (pos1 > -1) {
							actorString = actorString.substring(pos3, actorString.length());
						}
					}
				} else {
					isPlainOldNamedObj = true;
					//throw new Exception("No Library Index ID was found");
				}
	
			} catch (Exception w) {
			    MessageHandler.error("Error getting MoML for dropped object.", w);
			}
		} else {
			isPlainOldNamedObj = true;
			//log.error("Only ComponentEntities are allowed in the Library");
		}
		
		if (isPlainOldNamedObj) {
			// just return the NamedObj as it is
			actorString = dropObj.exportMoML();
		}
		
		// System.out.println("actorString: " + actorString);
		return actorString;
	}

}