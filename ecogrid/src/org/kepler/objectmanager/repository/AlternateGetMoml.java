/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:22:04 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31121 $'
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

package org.kepler.objectmanager.repository;

import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheNamedObj;
import org.kepler.objectmanager.library.LibItem;
import org.kepler.objectmanager.library.LibraryManager;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Mar 10, 2010
 * Time: 10:20:42 AM
 */

public class AlternateGetMoml {
	
	public AlternateGetMoml() {}
	
	public String getMoml(NamedObj dropObj) {

		String actorString = "";
		
		if (dropObj instanceof CacheNamedObj) {
			CacheNamedObj cno = (CacheNamedObj) dropObj;
			Object object = cno.get().getObject();
			ActorMetadata am = (ActorMetadata) object;
			String name = am.getName();
			NamedObj no;
			try {
				no = am.getActorAsNamedObj(null);
			}
			catch(Exception ex) {
				ex.printStackTrace();
				return "";
			}
			actorString = no.exportMoML(name);
			int pos1 = actorString.indexOf("<!DOCTYPE");
			int pos2 = actorString.indexOf(">", pos1);
			int pos3 = actorString.indexOf("<", pos2);
			if (pos1 > -1) {
				actorString = actorString.substring(pos3, actorString.length());
			}
		}
		else if (dropObj instanceof ComponentEntity) {
			try {
				int liid = LibraryManager.getLiidFor((ComponentEntity)dropObj);
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
					NamedObj no = am.getActorAsNamedObj(null);
					actorString = no.exportMoML(name);
					// this string has an xml header and a DOCTYPE declaration
					// following removes those elements
					int pos1 = actorString.indexOf("<!DOCTYPE");
					int pos2 = actorString.indexOf(">", pos1);
					int pos3 = actorString.indexOf("<", pos2);
					if (pos1 > -1) {
						actorString = actorString.substring(pos3, actorString.length());
					}
				} else {
					throw new Exception("No Library Index ID was found");
				}
	
			} catch (Exception w) {
				String msg = w.getMessage();
				if (msg == null) {
					msg = "empty error message";
				}
				System.out
						.println("Error inside GetMomlFromLSID.getMoml(): " + msg);
				try {
					Thread.sleep(500);
				}
				catch(InterruptedException ignored) {}
				try {
					throw new Exception();
				}
				catch(Exception ex) {
					ex.printStackTrace();
				}
				try {
					Thread.sleep(500);
				}
				catch(InterruptedException ignored) {}
			}
		}
		else {
			System.out.println("I don't know what to do about this");
		}
		// System.out.println("actorString: " + actorString);
		return actorString;
	}
	
}
