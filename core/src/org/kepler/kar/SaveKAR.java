/**
 *  '$RCSfile$'
 *  '$Author: barseghian $'
 *  '$Date: 2010-11-09 17:36:37 -0800 (Tue, 09 Nov 2010) $'
 *  '$Revision: 26278 $'
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.FileUtil;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;

/**
 * This is a gui independent class to be used for saving a KAR file to disk and
 * to remote repositories.
 * 
 * @author Aaron Schultz
 * 
 */
public class SaveKAR {
	private static final Log log = LogFactory.getLog(SaveKAR.class);
	private static final boolean isDebugging = log.isDebugEnabled();

	private File _karFile;
	private ArrayList<ComponentEntity> _saveInitiatorObjs;
	private KeplerLSID _specificLSID = null;

	public SaveKAR() {
		_saveInitiatorObjs = new ArrayList<ComponentEntity>();
	}

	public File getFile() {
		return _karFile;
	}

	public void addSaveInitiator(ComponentEntity ce) {
		if (_saveInitiatorObjs.contains(ce)) {
			if (isDebugging)
				log.debug(ce.getName()
						+ " is already in the save initiator list");
		} else {
			_saveInitiatorObjs.add(ce);
		}
	}

	public ArrayList<ComponentEntity> getSaveInitiatorList() {
		return _saveInitiatorObjs;
	}

	/**
	 * Set the LSID to be used for the KAR. If a specific LSID is not set then a
	 * new LSID will be generated.
	 * 
	 * @param lsid
	 */
	public void specifyLSID(KeplerLSID lsid) {
		_specificLSID = lsid;
	}

	public void setFile(File karFile) {
		// Force the kar extension
		if (!FileUtil.getExtension(karFile).equals(KARFile.EXTENSION)) {
			karFile = new File(karFile.getAbsolutePath() + "."
					+ KARFile.EXTENSION);
		}
		_karFile = karFile;
	}

	/**
	 * Every NamedObj should have an LSID.
	 * 
	 * @param no
	 */
	public KeplerLSID checkNamedObjLSID(NamedObj no) {

		// Make sure the object has an LSID
		// assign one if it doesn't
		KeplerLSID lsid = NamedObjId.getIdFor(no);
		
		if (isDebugging)
			log.debug(lsid);
		
		return lsid;
	}

	/**
	 * Check a namedObj's name. If null or just whitespace or 
	 * startsWith "Unnamed" throw an exception.
	 * 
	 * @param no
	 * @throws Exception
	 */
	public void checkNamedObjName(NamedObj no) throws Exception {

		// Make sure the object has a name
		if (no.getName() == null || no.getName().trim().equals("")) {
			throw new Exception("No Name");
		}

		if (no.getName().startsWith("Unnamed")) {
			throw new Exception("Unnamed");
		}

	}

	/**
	 * Save this KAR file to disk.
	 */
	public KeplerLSID saveToDisk(TableauFrame tableauFrame, String overrideModDeps) {
		if (isDebugging)
			log.debug("saveToDisk()");

		KARBuilder karBuilder = new KARBuilder();
		karBuilder.setKarFile(getFile());
		try {
			for (ComponentEntity initiator : _saveInitiatorObjs) {
				karBuilder.addSaveInitiator(initiator);
			}
			karBuilder.setRegisterLSID(false);
			karBuilder.setRevision(false);
			if (_specificLSID != null) {
				karBuilder.setKarLSID(_specificLSID);
			}
			karBuilder.generateKAR(tableauFrame, overrideModDeps);

			// Update the File in case KARBuilder changes it for some reason
			_karFile = karBuilder.getKarFile();

			// return the new lsid
			return karBuilder.getKarLSID();

		} catch (Exception ex) {
			log.error("Failed to create kar file: " + ex.getMessage());
			ex.printStackTrace();
		}

		return null;

	}

	/**
	 * Save this KAR file to the cache only if it is being saved to disk in a
	 * local repository.
	 */
	public void saveToCache() {
		if (isDebugging)
			log.debug("saveToCache()");

		LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
		if (lrm.isInLocalRepository(getFile())) {

			KARFile thekar;
			try {
				thekar = new KARFile(getFile());
				thekar.cacheKARContents();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
		}
	}


}
