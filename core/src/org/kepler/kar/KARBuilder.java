/**
 *  '$RCSfile$'
 *  '$Author: barseghian $'
 *  '$Date: 2013-01-11 18:13:59 -0800 (Fri, 11 Jan 2013) $'
 *  '$Revision: 31321 $'
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.handlers.ActorMetadataKAREntryHandler;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.ObjectManager;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.objectmanager.lsid.LSIDGenerator;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;

/**
 * Class to create KAR files from within kepler
 */
/**
 * @author Aaron Schultz
 * 
 */
public class KARBuilder {

	public static final String KAR_LSID_ATTRIBUTE_NAME = "karLSID";

	private static final Log log = LogFactory
			.getLog(KARBuilder.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();


	/**
	 * The save initiator list is the set of ComponentEntity objects to be saved
	 * into the KAR.
	 */
	private Vector<NamedObj> _saveInitiatorList;

	/*
	 * true if you want new lsids registered with the cache. If you are going to
	 * eventually cache the created kar file, set registerLSID to false.
	 */
	private boolean _registerLSID;

	private boolean _revision;

	private KeplerLSID _karLSID;
	private File _karFile;
	
	private LinkedHashMap<KAREntry, InputStream> _karItems;
	private Vector<KeplerLSID> _karItemLSIDs;
	private Vector<String> _karItemNames;
	private KARManifest _manifest;

	/**
	 * Empty Constructor for building a KAR file.
	 */
	public KARBuilder() {

		// initialize the defaults for private variables
		_saveInitiatorList = new Vector<NamedObj>();
		
		_karItems = new LinkedHashMap<KAREntry, InputStream>();
		_karItemLSIDs = new Vector<KeplerLSID>();
		_karItemNames = new Vector<String>();
		_manifest = new KARManifest();

		_karLSID = null;
		_karFile = null;

		_revision = false;
		_registerLSID = false;
	}

	public Vector<NamedObj> getSaveInitiatorList() {
		return _saveInitiatorList;
	}

	/**
	 * Add the namedObj to the _saveInitiatorList as well as into the ObjectManager.
	 * @param namedObj
	 */
	public void addSaveInitiator(NamedObj namedObj) {
		_saveInitiatorList.add(namedObj);
		
		// XXX KAREntryHandlers may need to access these from the ObjectManager
		// e.g. ReportLayoutKAREntryHandler.save does.
		try {
			ObjectManager.getInstance().addNamedObj(namedObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public File getKarFile() {
		return _karFile;
	}

	public void setKarFile(File karFile) {
		_karFile = karFile;
	}

	public boolean isRevision() {
		return _revision;
	}

	public void setRevision(boolean revision) {
		_revision = revision;
	}

	public boolean isRegisterLSID() {
		return _registerLSID;
	}

	public void setRegisterLSID(boolean registerLSID) {
		_registerLSID = registerLSID;
	}

	public KeplerLSID getKarLSID() {
		return _karLSID;
	}

	public void setKarLSID(KeplerLSID karLSID) {
		_karLSID = karLSID;
	}

	public KARManifest getManifest() {
		return _manifest;
	}

	public void setManifest(KARManifest manifest) {
		_manifest = manifest;
	}

	/**
	 * Handle the creation of the KAREntry objects for the Save Initiator List.
	 * 
	 * @return Hashtable<KAREntry, InputStream>
	 */
	public Hashtable<KAREntry, InputStream> handleInitiatorList() {

		Hashtable<KAREntry, InputStream> items = new Hashtable<KAREntry, InputStream>();

		for (NamedObj namedObj : _saveInitiatorList) {

			try {

				String objType = namedObj.getClass().getName();
				KeplerLSID lsid = NamedObjId.getIdFor(namedObj);

				ActorMetadata aMet = new ActorMetadata(namedObj);

				aMet.setName(namedObj.getName());
				aMet.setId(lsid.toString());

				String actorFilename = namedObj.getName() + "."
						+ lsid.createFilename() + ".xml";
				if (isDebugging)
					log.debug(actorFilename);

				KAREntry entry = new KAREntry(actorFilename);
				entry.setLSID(lsid);
				entry.setType(objType);
				entry.setHandler(ActorMetadataKAREntryHandler.class.getName());

				String actorMetadataString = aMet.toString();
				byte[] actorMetadataBytes = actorMetadataString.getBytes();
				ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(
						actorMetadataBytes);

				items.put(entry, byteArrayIS);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return items;
	}

	/**
	 * 
	 * @param entries
	 * @return
	 */
	public Vector<KeplerLSID> getKAREntryLSIDs(
			Hashtable<KAREntry, InputStream> entries) {

		Vector<KeplerLSID> lsids = new Vector<KeplerLSID>();
		for (KAREntry entry : entries.keySet()) {
			KeplerLSID lsid = entry.getLSID();
			lsids.add(lsid);
		}
		return lsids;
	}

	private Hashtable<KAREntry, InputStream> queryKAREntryHandlers(
			Vector<KeplerLSID> lsidsOfEntriesReturnedFromPreviousIteration, TableauFrame tableauFrame) 
			throws Exception {

		Hashtable<KAREntry, InputStream> entriesForThisIteration = new Hashtable<KAREntry, InputStream>();

		Collection<KAREntryHandler> allHandlers = CacheManager.getInstance()
				.getKAREntryHandlers();

		// Loop through the KAREntryHandlers
		for (KAREntryHandler keh : allHandlers) {
			if (isDebugging)
				log.debug(keh.getClass().getName());

			// Get the KAREntries from each handler
			Hashtable<KAREntry, InputStream> entries = keh.save(
					lsidsOfEntriesReturnedFromPreviousIteration, _karLSID, tableauFrame);
			if (entries != null) {
				for (KAREntry entry : entries.keySet()) {
					entry.setHandler(keh.getClass().getName());
					entriesForThisIteration.put(entry, entries.get(entry));
				}
			}
		}

		return entriesForThisIteration;

	}
	
	private void removeDuplicateKAREntries() throws Exception {

		// now remove any "duplicate" karentries where duplicate
		// defined as same name and lsid.
		// see bug#4555. We may remove this in the future, where
		// such dupes might be allowed
		// (e.g. same name + lsid but in different subdirs in kar).
		Hashtable<String, String> nameMap = new Hashtable<String, String>();
		for (KAREntry ke : _karItems.keySet()) {
			String name = ke.getName();
			String itemLsid = ke.getAttributes().getValue(
					KAREntry.LSID);
			if (nameMap.containsKey(name)) {
				if (nameMap.get(name).equals(itemLsid)) {
					_karItemLSIDs.remove(ke.getLSID());
					_karItemNames.remove(ke.getName());
					_karItems.remove(ke);
				}
			}
			nameMap.put(name, itemLsid);
		}
	}
	
	private void addEntriesToPrivateItems(Hashtable<KAREntry, InputStream> entries) {
		if (isDebugging) log.debug("addEntriesToPrivateItems("+entries.size()+")");
		
		for (KAREntry karEntryKey : entries.keySet()) {
			_karItems.put(karEntryKey, entries
					.get(karEntryKey));
			_karItemLSIDs.add(karEntryKey.getLSID());
			_karItemNames.add(karEntryKey.getName());
		}
		
	}

	public void generateKAR(TableauFrame tableauFrame, String overrideModDeps) throws IllegalActionException {
		if (isDebugging) log.debug("generateKAR()");

		if (_karLSID == null) {
			try {
				_karLSID = LSIDGenerator.getInstance().getNewLSID();
			} catch (Exception e) {
				log.error("could not generate new LSID for KAR: "
						+ e.getMessage());
				e.printStackTrace();
			}
		}

		try {

			// Get KAREntries for the Save Initiator List
			Hashtable<KAREntry, InputStream> initiatorEntries = handleInitiatorList();
			addEntriesToPrivateItems(initiatorEntries);
			
			int pass = 1;

			// Loop through KAR Entry handlers until no more KAREntry objects
			// are returned
			Vector<KeplerLSID> previousPassEntryLSIDs = getKAREntryLSIDs(initiatorEntries);
			if (isDebugging) 
				log.debug("Pass " + pass + " entries: " + previousPassEntryLSIDs.toString());
			while (previousPassEntryLSIDs.size() > 0) {
				pass++;
				
				// Get the KAREntries from all of the handlers
				Hashtable<KAREntry, InputStream> entries = 
					queryKAREntryHandlers(previousPassEntryLSIDs, tableauFrame);
				if (entries != null) {

					previousPassEntryLSIDs.removeAllElements();
					if (isDebugging) 
						log.debug("Pass " + pass + " entries: ");
					Vector<KeplerLSID> repeats = new Vector<KeplerLSID>();
					for (KAREntry karEntryKey : entries.keySet()) {
						String entryName = karEntryKey.getName();
						String entryType = karEntryKey.getType();
						KeplerLSID entryLSID = karEntryKey.getLSID();
						if (isDebugging) 
							log.debug( entryName + "  " + entryLSID + "  " + entryType );
						if (_karItemLSIDs.contains(entryLSID)) {
							// TODO make sure existing Entry Handlers do not produce repeated LSIDs.
							// This should never happen.
							System.out.println("KARBuilder generateKAR() Trying to add "+ entryName + " with " +
									"type:"+entryType+" but an entry with lsid:" + entryLSID + " has already " +
									"been added to KAR. Will NOT add this entry.");
							repeats.add(entryLSID);
						} else if (_karItemNames.contains(entryName)) {
							// TODO make sure existing Entry Handlers do not produce repeated LSIDs.
							// This should never happen.
							System.out.println("KARBuilder generateKAR() An entry with entryName"+ entryName + 
									" has already been added to KAR. Will NOT add this entry with lsid:" 
									+ entryLSID);
							repeats.add(entryLSID);
						} else {
							previousPassEntryLSIDs.add(entryLSID);
						}
					}
					// A kludge to protect against entry handlers returning entries that have already been added
					for (KeplerLSID repeatedLSID : repeats) {
						for (KAREntry ke : entries.keySet()) {
							if (ke.getLSID().equals(repeatedLSID)) {
								entries.remove(ke);
								if (isDebugging) log.debug("Removed " + repeatedLSID + " from pass " + pass + " entries" );
								break;
							}
						}
					}
					
					addEntriesToPrivateItems(entries);
				}
			}

			prepareManifest(overrideModDeps);

			writeKARFile();
			
		} catch (Exception e) {
			throw new IllegalActionException("Error building the KAR file: "
					+ e.getMessage());
		}

	}

	/**
	 * Prepare the KAR Manifest based on the kar items.
	 *
	 * @param overrideModDeps - Optional override of kar's module dependencies, 
	 * set null for normal use.
	 * @throws Exception
	 */
	private void prepareManifest(String overrideModDeps) throws Exception {

		_manifest
				.addMainAttribute(KARFile.LSID.toString(), _karLSID.toString());
		if (overrideModDeps == null){
			_manifest.addMainAttribute(KARFile.MOD_DEPEND.toString(),
				ModuleDependencyUtil.buildModuleDependenciesString());
		}
		else{
			//System.out.println("KARBuilder prepareManifest using overrideModDeps:"+overrideModDeps);
			_manifest.addMainAttribute(KARFile.MOD_DEPEND.toString(),
					overrideModDeps);
		}

		// add all the KAREntry attributes to the KARManifest
		Vector<KAREntry> toRemove = new Vector<KAREntry>(1);
		for (KAREntry ke : _karItems.keySet()) {

			Attributes atts = ke.getAttributes();
			
			// Check required attributes
			String entryLSID = atts.getValue(KAREntry.LSID);
			String entryType = atts.getValue(KAREntry.TYPE);
			String entryHandler = atts.getValue(KAREntry.HANDLER);

			if (entryLSID == null || entryType == null || entryHandler == null) {
				log
						.warn(ke.getName()
								+ " KAREntry did not have an LSID, Type, or Handler attribute.  KAREntry removed.");
				toRemove.add(ke);

			} else {
				
				// add all attributes of the karentry to the manifest
				for (Object att : atts.keySet()) {
					if (att instanceof Name) {
						Name attName = (Name) att;
						String attValue = atts.getValue(attName);
						_manifest.addEntryAttribute(ke, attName.toString(),
								attValue);
					}
				}
			}
		}
		if (toRemove.size() > 0) {

			for (KAREntry ke : toRemove) {
				_karItemLSIDs.remove(ke.getLSID());
				_karItems.remove(ke);
			}

		}

	}

	/**
	 *
	 */
	private void writeKARFile() throws IOException {

		JarOutputStream jos = new JarOutputStream(
				new FileOutputStream(_karFile), _manifest);
		Iterator<KAREntry> li = _karItems.keySet().iterator();
		while (li.hasNext()) {
			KAREntry entry = (KAREntry) li.next();
			if (isDebugging) log.debug("Writing " + entry.getName());
			try {
				jos.putNextEntry(entry);
	
				if (_karItems.get(entry) instanceof InputStream) {
					// inputstream from a bin file
					byte[] b = new byte[1024];
					InputStream is = (InputStream) _karItems.get(entry);
					int numread = is.read(b, 0, 1024);
					while (numread != -1) {
						jos.write(b, 0, numread);
						numread = is.read(b, 0, 1024);
					}
					is.close();
					// jos.flush();
					jos.closeEntry();
				}
			} catch (IOException ioe) {
				log.error(" Tried to write Duplicate Entry to kar " + entry.getName() + " " + entry.getLSID());
				ioe.printStackTrace();
			}
		}
		jos.flush();
		jos.close();

		log.info("done writing KAR file to "
				+ _karFile.getAbsolutePath());
	}

}
