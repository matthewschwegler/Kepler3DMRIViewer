/*
 *  Copyright (c) 2003-2010 The Regents of the University of California.
 *  All rights reserved.
 *  Permission is hereby granted, without written agreement and without
 *  license or royalty fees, to use, copy, modify, and distribute this
 *  software and its documentation for any purpose, provided that the above
 *  copyright notice and the following two paragraphs appear in all copies
 *  of this software.
 *  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 *  FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 *  THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 *  PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 *  PT_COPYRIGHT_VERSION_2
 *  COPYRIGHTENDKEY
 */
package org.kepler.kar;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.modules.Module;
import org.kepler.build.util.Version;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationManagerException;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.configuration.NamespaceException;
import org.kepler.objectmanager.cache.CacheException;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.gui.TableauFrame;

/**
 * A KAR file is just a jar file that requires a manifest to be present that has
 * "KAR-Version" and "lsid" main attributes. This KARFile is used for reading
 * KAR files. For Writing KARFiles see KARBuilder class.
 * 
 * @author Aaron Schultz
 */
public class KARFile extends JarFile implements Serializable {
	private static final long serialVersionUID = 1522142700198331341L;
	private static final Log log = LogFactory.getLog(KARFile.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * This is the current KAR version.
	 */
	public static final String VERSION_2_1 = "2.1";
	public static final String CURRENT_VERSION = VERSION_2_1;
	
	/**
	 * Old versions still supported.
	 */
	public static final String VERSION_2_0 = "2.0";
	public static final String VERSION_1_0 = "1.0";
	
	//TODO these variables might be better placed elsewhere
	/** "strict"*/
	public static final String KAR_COMPLIANCE_STRICT = "strict";
	/** "relaxed"*/
	public static final String KAR_COMPLIANCE_RELAXED = "relaxed";
	/** "relaxed" if you change this you should also change KARFILE_CONFIG_PROP_MODULE's configuration.xml
	 we need to keep both for users that started with an earlier version of configuration.xml
	 that lacks the KAR_COMPLIANCE_PROPERTY
	 */
	public static final String KAR_COMPLIANCE_DEFAULT = KAR_COMPLIANCE_RELAXED;
	/** "KARComplianceMode"*/
	public static final String KAR_COMPLIANCE_PROPERTY_NAME = "KARComplianceMode";
	/** "core" Module*/
	public static final Module KARFILE_CONFIG_PROP_MODULE = ConfigurationManager.getModule("core");

	/** "karVersions"*/
	public static final String KAR_VERSIONS_PROPERTY_NAME = "karVersions";
	/** "currentVersion" sub-element of karVersions*/
	public static final String KAR_CURRENT_VERSION_PROPERTY_NAME = "currentVersion";
	/** "karVersion" sub-element of karVersions*/
	public static final String KAR_VERSION_PROPERTY_NAME = "karVersion";
	/** "version" sub-element of karVersion*/
	public static final String KAR_VERSION_VERSION_PROPERTY_NAME = "version";
	/** "namespace" sub-element of karVersion*/
	public static final String KAR_VERSION_NAMESPACE_PROPERTY_NAME = "namespace";
	/** "schemaUrl" sub-element of karVersion*/
	public static final String KAR_VERSION_SCHEMAURL_PROPERTY_NAME = "schemaUrl";
	/** "resourceDir" sub-element of karVersion*/
	public static final String KAR_VERSION_RESOURCEDIR_PROPERTY_NAME = "resourceDir";
	/** "resourceFileName" sub-element of karVersion*/
	public static final String KAR_VERSION_RESOURCE_FILENAME_PROPERTY_NAME = "resourceFileName";
	
	//if you change any of these you must also change KARFILE_CONFIG_PROP_MODULE's configuration.xml
	public static final String KAR_CURRENT_VERSION_DEFAULT = "2.1";
	public static final String KAR_VERSION_200_RESOURCEDIR_DEFAULT = "kar-2.0.0";
	public static final String KAR_VERSION_200_RESOURCE_FILENAME_DEFAULT = "KARFile.xsd";
	public static final String KAR_VERSION_200_VERSION_DEFAULT = "2.0";
	public static final String KAR_VERSION_200_NAMESPACE_DEFAULT = "http://www.kepler-project.org/kar-2.0.0";
	public static final String KAR_VERSION_200_SCHEMAURL_DEFAULT = "https://code.kepler-project.org/code/kepler/branches/releases/release-branches/core-2.0/resources/KARFile.xsd";
	public static final String KAR_VERSION_210_RESOURCEDIR_DEFAULT = "kar-2.1.0";
	public static final String KAR_VERSION_210_RESOURCE_FILENAME_DEFAULT = "KARFile-2.1.0.xsd";
	public static final String KAR_VERSION_210_VERSION_DEFAULT = "2.1";
	public static final String KAR_VERSION_210_NAMESPACE_DEFAULT = "http://www.kepler-project.org/kar-2.1.0";
	public static final String KAR_VERSION_210_SCHEMAURL_DEFAULT = "https://code.kepler-project.org/code/kepler/branches/releases/release-branches/core-2.1/resources/KARFile-2.1.0.xsd";

	/**
	 * This is the File extension to be used for KAR files.
	 */
	public static final String EXTENSION = "kar";

	/**
	 * This is the list of all supported versions.
	 */
	private Vector<String> _supportedVersions = new Vector<String>();

	/**
	 * <code>Name</code> object for <code>lsid</code> manifest attribute used
	 * for globally identifying KAREntries as unique.
	 */
	public static final Name LSID = new Name("lsid");

	/**
	 * <code>Name</code> object for <code>KAR-Version</code> manifest attribute.
	 * This attribute indicates the version number of the manifest standard to
	 * which a KAR file's manifest conforms.
	 */
	public static final Name KAR_VERSION = new Name("KAR-Version");

	/**
	 * <code>Name</code> object for <code>MOD_DEPEND</code> manifest attribute.
	 * This attribute indicates modules that are needed in order to open this
	 * KAR file.
	 */
	public static final Name MOD_DEPEND = new Name("module-dependencies");
	
	/**
	 * <code>Name</code> object for <code>OPENABLE</code> manifest attribute.
	 * This attribute indicates whether or not this KAR file is openable.
	 */
	public static final Name OPENABLE = new Name("openable");

	/**
	 * Separator to be used for dependency lists.
	 */
	public static final String DEP_SEPARATOR = ";";

	/**
	 * If the KARFile object was created from a KARFile on disk this is the File
	 * path and name of that file.
	 */
	private File _fileOnDisk;

	/**
	 * A list of all the entry types contained in this KARFile indexed by LSID.
	 */
	private Hashtable<KeplerLSID, String> _lsidTypes = new Hashtable<KeplerLSID, String>();

	/**
	 * A list of all the entry names contained in this KARFile indexed by LSID.
	 */
	private Hashtable<KeplerLSID, String> _lsidNames = new Hashtable<KeplerLSID, String>();

	/**
	 * Constructor for creating a KARFile from an existing file.
	 * 
	 * @param f
	 *            the file to create the KAR from
	 * @throws IOException
	 */
	public KARFile(File f) throws IOException {
		super(f);
		if (isDebugging)
			log.debug("KARFile(" + f + ")");

		_fileOnDisk = f;

		initializeSupportedVersions();

		if (getManifest() == null) {
			throw new IOException("This KAR file does not contain a Manifest.");
		}

		String lsidStr = getManifest().getMainAttributes().getValue(LSID);
		if (isDebugging)
			log.debug(lsidStr);
		if (lsidStr == null) {
			throw new IOException(
					"The KAR file does not contain an lsid attribute in the manifest");
		}
		if (!KeplerLSID.isKeplerLSIDFormat(lsidStr)) {
			throw new IOException(
					"The LSID of the KAR file is not properly formatted.");
		}

		String version = getManifest().getMainAttributes()
				.getValue(KAR_VERSION);
		if (version == null) {
			throw new IOException(
					"The KAR file does not contain a KAR-Version attribute in the manifest.");
		}
		if (!_supportedVersions.contains(version)) {
			throw new IOException(version
					+ " is not a supported KAR version.\n"
					+ getSupportedVersionString());
		}

		Enumeration<JarEntry> karFileEnum = entries();
		while (karFileEnum.hasMoreElements()) {
			JarEntry entry = (JarEntry) karFileEnum.nextElement();
			KeplerLSID lsid = getEntryLSID(entry);
			String type = getEntryType(entry);
			if (lsid != null && type != null) {
				_lsidNames.put(lsid, entry.getName());
				_lsidTypes.put(lsid, type);
			}
		}

	}

	private void initializeSupportedVersions() {
		_supportedVersions.add(VERSION_1_0);
		_supportedVersions.add(VERSION_2_0);
		_supportedVersions.add(VERSION_2_1);
	}

	public KeplerLSID getLSID() {
		try {
			return new KeplerLSID(getManifest().getMainAttributes().getValue(
					LSID));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @param lsid
	 * @throws IOException
	 */
	public void setLSID(KeplerLSID lsid) throws IOException {
		Attributes atts = getManifest().getMainAttributes();
		if (atts.containsKey(LSID)) {
			atts.remove(LSID);
		}
		atts.put(LSID, lsid.toString());
	}

	public String getVersion() {
		try {
			Attributes atts = getManifest().getMainAttributes();
			String karVersion = atts.getValue(KAR_VERSION);
			return karVersion;
		} catch (IOException e) {
			return null;
		}
	}
	
	public boolean isOpenable() {
		try {
			Attributes atts = getManifest().getMainAttributes();
			String openable = atts.getValue(OPENABLE);
			if (openable != null) {
				openable = openable.trim();
				if (openable.equalsIgnoreCase("false")) {
					return false;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Vector<String>moduleDependencies = getModuleDependencies();
		if (ModuleDependencyUtil.checkIfModuleDependenciesSatisfied(moduleDependencies)){
			return true;
		}
		return false;
	}

	/**
	 * Set the version of the KARFile.
	 * 
	 * @param version
	 * @throws IOException
	 *             if the given version is not supported
	 */
	public void setVersion(String version) throws IOException {
		if (_supportedVersions.contains(version)) {
			Attributes atts = getManifest().getMainAttributes();
			if (atts.containsKey(KAR_VERSION)) {
				atts.remove(KAR_VERSION);
			}
			atts.put(KAR_VERSION, version);
		} else {
			throw new IOException(version
					+ " is not a supported KAR version.\n"
					+ getSupportedVersionString());
		}
	}

	public Vector<String> getModuleDependencies() {
		Vector<String> dependencies = new Vector<String>();
		try {
			String depStr = getManifest().getMainAttributes().getValue(
					MOD_DEPEND);
			if (depStr == null) {
				return dependencies;
			}
			dependencies = ModuleDependencyUtil.parseDependencyString(depStr);
		} catch (Exception e) {
		}
		return dependencies;
	}

	/**
	 * Return the LSIDs of all of the entries contained by this KARFile.
	 * 
	 * */
	public Set<KeplerLSID> getContainedLSIDs() {
		return _lsidTypes.keySet();
	}

	public String getEntryType(KeplerLSID lsid) {
		return _lsidTypes.get(lsid);
	}

	public String getEntryName(KeplerLSID lsid) {
		return _lsidNames.get(lsid);
	}

	/**
	 * Return the name of the local repository that this KAR is stored in.
	 * 
	 * @return null if no name is found
	 */
	public String getLocalRepoName() {
		LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
		LocalRepositoryManager.LocalRepository repository = lrm.getContainingLocalRepository(getFileLocation());
		if(repository != null) {
		    return lrm.getLocalRepositories().get(repository);
		}
		return null;
	}

	/**
	 * Return the path where this kar file is found on disk.
	 */
	public String getPath() {
		String filePath = getFileLocation().getPath();
		String fileName = getFileLocation().getName();
		if (isDebugging)
			log.debug(filePath + "   " + fileName);

		if (filePath.endsWith(fileName)) {
			// remove the filename
			int i = filePath.length() - fileName.length();
			if (i > 0) {
				filePath = filePath.substring(0, i);
			}
			if (isDebugging)
				log.debug("New filePath: " + filePath);
		}
		return filePath;
	}

	public File getFileLocation() {
		return _fileOnDisk;
	}

	/**
	 * returns a KAREntry for the given lsid. If there is not KAREntry then
	 * return null.
	 * 
	 * @param name
	 *            the name of the entry to return
	 */
	public ZipEntry getKAREntry(KeplerLSID lsid) {
		String path = _lsidNames.get(lsid);
		ZipEntry ze = super.getEntry(path);
		if (ze != null) {
			return ze;
		}
		return null;
	}

	/**
	 * Returns only valid KAREntries contained in this KARFile. To get all of
	 * the JarEntries use the entries() method.
	 * 
	 * @return List<KAREntry> of all valid KAREntries in this KARFile.
	 */
	public List<KAREntry> karEntries() {
		if (isDebugging)
			log.debug("karEntries()");
		Enumeration<JarEntry> entries = entries();
		Vector<KAREntry> karEntries = new Vector<KAREntry>();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (isDebugging)
				log.debug(entry.getName());
			if (entry.getName().equals("MANIFEST.MF")
					|| entry.getName().equals("META-INF/")
					|| entry.getName().equals(JarFile.MANIFEST_NAME)) {
				// skip manifest entries
			} else {
				KAREntry karEntry = new KAREntry(entry);
				if (karEntry.isValid()) {
					if (isDebugging)
						log.debug("karEntry.isValid() == true");
					karEntries.add(karEntry);
				} else {
					if (isDebugging)
						log.debug("karEntry.isValid() == false");
				}
			}
		}
		return karEntries;
	}

	/**
	 * Return an array of lsids of a certain type from within this kar. if the
	 * type is null return all of the lsids
	 * 
	 * @param getType
	 *            the type of the content to get
	 */
	public KeplerLSID[] getContentOfType(String getType) throws Exception {

		Vector<KeplerLSID> returnVec = new Vector<KeplerLSID>();
		for (KeplerLSID lsid : this.getContainedLSIDs()) {
			String type = this.getEntryType(lsid);
			if (getType == null || getType.equals(type)) {
				returnVec.addElement(lsid);
			} else if (isSubclass(getType, type)) {
				returnVec.addElement(lsid);
			}
		}

		KeplerLSID[] lsids = new KeplerLSID[returnVec.size()];
		for (int i = 0; i < returnVec.size(); i++) {
			lsids[i] = (KeplerLSID) returnVec.elementAt(i);
		}
		return lsids;
	}

	/**
	 * Determine if type is a subclass of getType. This belongs in a UTIL class
	 * somewhere?
	 * 
	 * @param getType
	 * @param type
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static boolean isSubclass(String getType, String type)
			throws ClassNotFoundException {
		
		// protect against nulls
		if (getType == null || type == null)
			return false;

		// the superclass test
		Class getClazz = Class.forName(getType);

		// handle superclasses
		Class theClazz = Class.forName(type);
		Class superClazz = theClazz.getSuperclass();
		while (superClazz != null) {
			if (superClazz.getName().equals(getClazz.getName())) {
				return true;
			}
			superClazz = superClazz.getSuperclass();
		}
		return false;
	}

	/**
	 * @return String of the supported KAR versions.
	 */
	private String getSupportedVersionString() {
		String s = "Supported KAR versions:\n";
		for (String v : _supportedVersions) {
			s += "      " + v + "\n";
		}
		return s;
	}

	/**
	 * Read the LSID from the attributes of the given entry and return it as a
	 * KeplerLSID.
	 * 
	 * @param entry
	 * @return
	 * @throws IOException
	 */
	private KeplerLSID getEntryLSID(JarEntry entry) throws IOException {
		Attributes atts = entry.getAttributes();
		if (atts == null) {
			return null;
		}

		String lsidStr = atts.getValue(KAREntry.LSID);
		if (lsidStr == null) {
			log.error("KAREntries must have an LSID and a type.");
			return null;
		}

		KeplerLSID lsid = null;
		try {
			lsid = new KeplerLSID(lsidStr);
		} catch (Exception e) {
			log.warn(lsidStr + " is not a properly formatted KeplerLSID");
			return null;
		}
		return lsid;
	}

	/**
	 * Read the type attribute from the given entry and return it as a string.
	 * 
	 * @param entry
	 * @return
	 * @throws IOException
	 */
	private String getEntryType(JarEntry entry) throws IOException {
		Attributes atts = entry.getAttributes();
		if (atts == null) {
			return null;
		}
		String type = atts.getValue(KAREntry.TYPE);
		return type;
	}

	/**
	 * This method makes sure that all of the entries of this KARFile are in the
	 * Cache. It caches the entries in the order that their dependencies
	 * dictate.
	 * 
	 * @throws Exception
	 */
	public void cacheKARContents() throws Exception {
		if (isDebugging) {
			log.debug("openKAR: " + this.toString());
		}
		try {
			// get references to all the managers we'll be using
			LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
			KARCacheManager kcm = KARCacheManager.getInstance();
			CacheManager cm = CacheManager.getInstance();

			// Make sure the file is in a local repository
			if (!lrm.isInLocalRepository(getFileLocation())) {
				log
						.warn("KAR should be in a Local Repository Folder to be inserted in the cache: "
								+ getFileLocation());
				// return;
			}

			// Add a row to the KARS_CACHED table
			boolean inserted = kcm.insertIntoCache(this);
			if (!inserted) {
				// This KAR has already been cached, don't do it again
				return;
			}

			// keep two lists while traversing the dependencies, start with all
			// of the entries (we don't know yet if they are cached or not)
			// and move them into the cached entries as they are cached (or if
			// they are already cached)
			Vector<KAREntry> entries = (Vector<KAREntry>) karEntries();
			Hashtable<KeplerLSID, KAREntry> cachedEntries = new Hashtable<KeplerLSID, KAREntry>();

			// do one pass through the entries to see if any of them are already
			// in the cache
			for (KAREntry entry : entries) {
				KeplerLSID lsid = entry.getLSID();

				// See if this entry is already in the Cache
				boolean alreadyCached = cm.isContained(lsid);
				if (alreadyCached) {

					// add this entry into the cachedEntries list
					cachedEntries.put(entry.getLSID(), entry);

					// Insert a row into the KAR_CONTENTS table for this entry
					File karFile = getFileLocation();
					KeplerLSID entryLsid = entry.getLSID();
					String entryName = entry.getName();
					String entryType = entry.getType();
					kcm.insertEntryIntoCache(karFile, entryLsid, entryName,
							entryType);
				}
			}

			// remove entries that were already cached
			for (KAREntry entry : cachedEntries.values()) {
				entries.remove(entry);
			}

			// keep cycling through the uncached entries until the list is empty
			while (entries.size() > 0) {

				// keep track of the entries cached during this pass
				Vector<KAREntry> cachedThisPass = new Vector<KAREntry>(entries
						.size());

				// cycle through all of the remaining, uncached entries
				for (KAREntry entry : entries) {
					if (isDebugging)
						log.debug(entry.getName());

					// get the dependency list for this entry
					List<KeplerLSID> depList = entry.getLsidDependencies();

					if (depList.size() == 0) {
						// if there are no dependencies we just cache it
						boolean success = cache(entry);
						if (success) {
							cachedEntries.put(entry.getLSID(), entry);
							cachedThisPass.add(entry);
							break;
						}
						if (isDebugging)
							log.debug(success);
					} else {
						// if there are dependencies then we check to make sure
						// that all of the dependencies have already been cached
						boolean allDependenciesHaveBeenCached = true;
						for (KeplerLSID lsid : depList) {
							// if any of the dependencies have not been cached,
							// set false
							if (!cm.isContained(lsid)) {
								allDependenciesHaveBeenCached = false;
							}
						}
						if (allDependenciesHaveBeenCached) {
							// all dependencies have been cached so it is
							// OK to cache this entry
							boolean success = cache(entry);
							if (success) {
								cachedEntries.put(entry.getLSID(), entry);
								cachedThisPass.add(entry);
								break;
							}
							if (isDebugging)
								log.debug(success);
						}
					}
				}
				if (cachedThisPass.size() == 0) {
					// Bad news, nothing is getting cached
					// This means that there are uncached entries that
					// have unsatisfied dependencies
					// break out to avoid infinite loop
					// Vector<KAREntry> entriesWithBrokenDependencies = entries;
					break;
				}

				// remove any entries that got cached this pass
				for (KAREntry entry : cachedThisPass) {
					entries.remove(entry);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Here we go through all the KAREntries and call the open method of the
	 * appropriate KAREntryHandlers. It is assumed that cacheKARContents() has
	 * been called at some point before openKAR() In other words everything in
	 * the kar is already cached when calling the open() method of the
	 * KAREntryHandlers.
	 * 
	 * Note: There is some issue with having this method here, since it is
	 * really a gui specific function it probably does not belong here in the
	 * core module.
	 * 
	 * @param tableauFrame
	 * @param forceOpen
	 * @throws Exception
	 * @returns true if at least one of the entries in the KAR was opened
	 */
	public boolean openKARContents(TableauFrame tableauFrame, boolean forceOpen) throws Exception {
		if (isDebugging)
			log.debug("openKAR: " + this.toString());
		
		if (!forceOpen && !isOpenable()){
			return false;
		}
		
		try {

			/**
			 * Loop through the kar entries and call the open method of the
			 * appropriate KAREntryHandler
			 */
			Vector<KAREntry> unopenedEntries = (Vector<KAREntry>) karEntries();
			Hashtable<KeplerLSID, KAREntry> openedEntries = new Hashtable<KeplerLSID, KAREntry>();

			// keep cycling through the unopened entries until the list is empty
			while (unopenedEntries.size() > 0) {

				// keep track of the entries that were opened during this pass
				Vector<KAREntry> openedThisPass = new Vector<KAREntry>(
						unopenedEntries.size());

				// cycle through all of the remaining, unopened entries
				for (KAREntry entry : unopenedEntries) {
					if (isDebugging) {
						log.debug(entry.getName());
					}

					// get the dependency list for this entry
					List<KeplerLSID> depList = entry.getLsidDependencies();

					if (depList.size() == 0) {
						// if there are no dependencies we just open it up
						boolean success = open(entry, tableauFrame);
						if (success) {
							openedEntries.put(entry.getLSID(), entry);
							openedThisPass.add(entry);
							break;
						}
						if (isDebugging)
							log.debug(success);
					} else {
						// if there are dependencies then we check to make sure
						// that all of the dependencies have already been opened
						boolean allDependenciesHaveBeenOpened = true;
						for (KeplerLSID lsid : depList) {
							// if any of the dependencies have not been opened,
							// set false
							if (!openedEntries.containsKey(lsid)) {
								allDependenciesHaveBeenOpened = false;
							}
						}
						if (allDependenciesHaveBeenOpened) {
							// dependencies have been opened so OK to open this
							// one
							boolean success = open(entry, tableauFrame);
							if (success) {
								openedEntries.put(entry.getLSID(), entry);
								openedThisPass.add(entry);
								break;
							}
							if (isDebugging)
								log.debug(success);
						}
					}
				}

				if (openedThisPass.size() == 0) {
					// Bad news, nothing is getting opened
					// break out to avoid infinite loop
					break;
				}

				// remove the entries that were opened during this pass
				for (KAREntry entry : openedThisPass) {
					unopenedEntries.remove(entry);
				}
			}
			

			if (openedEntries.size() == 0) {
				return false;
			}
		} catch (Exception e) {
			throw new Exception("Error on Open: " + e.getMessage());
		}
		return true;
	}

	/**
	 * Get all the KAREntryHandlers that can handle the given entry.
	 * 
	 * @param entry
	 * @return KAREntryHandlers that can handle the given entry.
	 */
	private Vector<KAREntryHandler> getHandlersForEntry(KAREntry entry) {
		Vector<KAREntryHandler> handlers = new Vector<KAREntryHandler>(2);

		// First we make sure that the EntryHandler that wrote the entry
		// is present in the system
		try {
			// check for it in the classpath
			Class.forName(entry.getHandler());
		} catch (Exception e) {
			log.warn("KAREntryHandler not in classpath: " + entry.getHandler());
			return handlers;
		}

		try {
			// check to see if it is registered with the KAREntryHandler
			// extension point
			boolean writeHandlerPresent = false;
			for (KAREntryHandler k : CacheManager.getInstance()
					.getKAREntryHandlers()) 
			{
				if (k.getClass().getName().equals(entry.getHandler())) {
					writeHandlerPresent = true;
					break;
				}
			}
			if (writeHandlerPresent) {
				// continue looking for EntryHandlers that handle this type
			} else {
				log.warn("KAREntryHandler is not registered with suite: "
						+ entry.getHandler());
				return handlers;
			}
		} catch (CacheException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
		}

		KeplerLSID lsid = entry.getLSID();
		String type = entry.getType();
		if (isDebugging)
			log.debug(lsid + " " + type);
		if (lsid == null || type == null) {
			return null;
		}
		try {
			if (getVersion().equals(VERSION_1_0)) {
				// In version 1.0 KARs the type is a one word string
				// that identifies what type of file it is
				for (KAREntryHandler k : CacheManager.getInstance()
						.getKAREntryHandlers()) {
					if (k.getTypeName().equals(type)) {
						handlers.add(k);
					}
				}
			} else if (getVersion().equals(VERSION_2_0) || 
					getVersion().equals(VERSION_2_1)) {
				// In version 2.0 and 2.1 KARs the type is the binary class
				// name for the type of file that the KAREntry is.
				// A KAREntryHandler may be able to handle more than
				// one type in version 2.0 and 2.1
				for (KAREntryHandler k : CacheManager.getInstance()
						.getKAREntryHandlers()) {
					if (k.handlesType(type)) {
						handlers.add(k);
					}
				}
			}
		} catch (CacheException e) {
			e.printStackTrace();
		}
		return handlers;
	}

	/**
	 * Insert the object into the cache and record it in the kar contents table.
	 * 
	 * @param entry
	 * @return true if the entry could be cached
	 */
	private boolean cache(KAREntry entry) {
		if (entry.getAttributes() == null) {
			return false;
		}

		CacheManager cm;
		KARCacheManager kcm;
		try {
			cm = CacheManager.getInstance();
			kcm = KARCacheManager.getInstance();

			if (cm.isContained(entry.getLSID())) {
				// This entry is already cached
				// make sure there is a row for it in the KAR_CONTENTS table
				File karFile = getFileLocation();
				KeplerLSID entryLsid = entry.getLSID();
				String entryName = entry.getName();
				String entryType = entry.getType();
				kcm.insertEntryIntoCache(karFile, entryLsid, entryName,
						entryType);
				return true;
			}
		} catch (CacheException e1) {
			e1.printStackTrace();
			return false;
		}

		// get all KAREntryHandlers for this KAREntry
		Vector<KAREntryHandler> handlers = getHandlersForEntry(entry);
		if (handlers == null)
			return false;

		if (handlers.size() <= 0) {
			// this KAREntry type is not recognized ignore it
			log.warn("No KAREntryHandlers found for KAREntry: " + entry.getName() +" ignoring this item.");
			return false;
		}

		// loop through all of the handlers and pass the entry to their cache
		// method
		// if they return a cache object then insert it into the cache.
		for (KAREntryHandler keh : handlers) {
			try {
				CacheObject co = keh.cache(this, entry);
				if (co != null) {

					// The handler returned a CacheObject, insert it into the
					// cache
					cm.insertObject(co);

					// Insert a row into the KAR_CONTENTS table for this entry
					File karFile = getFileLocation();
					KeplerLSID entryLsid = entry.getLSID();
					String entryName = entry.getName();
					String entryType = entry.getType();
					kcm.insertEntryIntoCache(karFile, entryLsid, entryName,
							entryType);

				}
			} catch (CacheException ce) {
				log.warn("KAREntry was not cached: " + entry.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	/**
	 * Call the open methods of all KAREntryHandlers that support the type of
	 * the given entry.
	 * 
	 * @param entry
	 * @param tableauFrame
	 * @return
	 */
	public boolean open(KAREntry entry, TableauFrame tableauFrame) {
		Vector<KAREntryHandler> handlers = getHandlersForEntry(entry);
		if (handlers == null)
			return false;

		if (handlers.size() <= 0) {
			// this KAREntry type is not recognized ignore it
			log.warn("No KAREntryHandlers found for KAREntry: " + entry.getName() +" ignoring this item.");
			return false;
		}

		boolean allsuccess = true;
		for (KAREntryHandler keh : handlers) {
			try {
				boolean success = keh.open(this, entry, tableauFrame);
				if (!success) {
					allsuccess = false;
				}
				if (isDebugging)
					log.debug(success);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return allsuccess;

	}
	
	public boolean areAllModuleDependenciesSatisfied(){
		return ModuleDependencyUtil.checkIfModuleDependenciesSatisfied(this.getModuleDependencies());
	}

	/** Get a map of missing module dependencies. */
	public Map<String, Version> getMissingDependencies() {
		
		List<String> dependencies = getModuleDependencies();
		return ModuleDependencyUtil.getUnsatisfiedDependencies(dependencies);

	}
	
	/**
	 * @return karVersions configuration property, adding it to config file
	 * if it doesn't exist.
	 */
	public static ConfigurationProperty getKARVersionsConfigProperty() {
		
		ConfigurationManager cman = ConfigurationManager.getInstance();
		ConfigurationProperty coreProperty = cman
				.getProperty(KARFILE_CONFIG_PROP_MODULE);
		ConfigurationProperty KARVersionsProp = coreProperty
			.getProperty(KAR_VERSIONS_PROPERTY_NAME);
		
		try {
		
			// add karVersions configuration property (and all sub props) if
			// necessary
			if (KARVersionsProp == null) {
				KARVersionsProp = new ConfigurationProperty(
						KARFILE_CONFIG_PROP_MODULE, KAR_VERSIONS_PROPERTY_NAME);
				coreProperty.addProperty(KARVersionsProp);

				ConfigurationProperty KARCurrentVersionProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_CURRENT_VERSION_PROPERTY_NAME,
						KARFile.KAR_CURRENT_VERSION_DEFAULT);

				ConfigurationProperty KARVersion200Prop = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_PROPERTY_NAME);
				ConfigurationProperty KARVersion200VersionProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_VERSION_PROPERTY_NAME,
						KARFile.KAR_VERSION_200_VERSION_DEFAULT);
				ConfigurationProperty KARVersion200SchemaUrlProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_SCHEMAURL_PROPERTY_NAME,
						KARFile.KAR_VERSION_200_SCHEMAURL_DEFAULT);
				ConfigurationProperty KARVersion200NamespaceProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_NAMESPACE_PROPERTY_NAME,
						KARFile.KAR_VERSION_200_NAMESPACE_DEFAULT);
				ConfigurationProperty KARVersion200ResourceDirProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_RESOURCEDIR_PROPERTY_NAME,
						KARFile.KAR_VERSION_200_RESOURCEDIR_DEFAULT);
				ConfigurationProperty KARVersion200ResourceFileNameProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_RESOURCE_FILENAME_PROPERTY_NAME,
						KARFile.KAR_VERSION_200_RESOURCE_FILENAME_DEFAULT);
				
				ConfigurationProperty KARVersion210Prop = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_PROPERTY_NAME);
				ConfigurationProperty KARVersion210VersionProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_VERSION_PROPERTY_NAME,
						KARFile.KAR_VERSION_210_VERSION_DEFAULT);
				ConfigurationProperty KARVersion210SchemaUrlProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_SCHEMAURL_PROPERTY_NAME,
						KARFile.KAR_VERSION_210_SCHEMAURL_DEFAULT);
				ConfigurationProperty KARVersion210NamespaceProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_NAMESPACE_PROPERTY_NAME,
						KARFile.KAR_VERSION_210_NAMESPACE_DEFAULT);
				ConfigurationProperty KARVersion210ResourceDirProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_RESOURCEDIR_PROPERTY_NAME,
						KARFile.KAR_VERSION_210_RESOURCEDIR_DEFAULT);
				ConfigurationProperty KARVersion210ResourceFileNameProp = new ConfigurationProperty(
						KARFile.KARFILE_CONFIG_PROP_MODULE,
						KARFile.KAR_VERSION_RESOURCE_FILENAME_PROPERTY_NAME,
						KARFile.KAR_VERSION_210_RESOURCE_FILENAME_DEFAULT);

				KARVersion200Prop.addProperty(KARVersion200VersionProp);
				KARVersion200Prop.addProperty(KARVersion200SchemaUrlProp);
				KARVersion200Prop.addProperty(KARVersion200NamespaceProp);
				KARVersion200Prop.addProperty(KARVersion200ResourceDirProp);
				KARVersion200Prop.addProperty(KARVersion200ResourceFileNameProp);

				KARVersion210Prop.addProperty(KARVersion210VersionProp);
				KARVersion210Prop.addProperty(KARVersion210SchemaUrlProp);
				KARVersion210Prop.addProperty(KARVersion210NamespaceProp);
				KARVersion210Prop.addProperty(KARVersion210ResourceDirProp);
				KARVersion210Prop.addProperty(KARVersion210ResourceFileNameProp);

				KARVersionsProp.addProperty(KARCurrentVersionProp);
				KARVersionsProp.addProperty(KARVersion200Prop);
				KARVersionsProp.addProperty(KARVersion210Prop);
			}

		} catch (NamespaceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConfigurationManagerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return KARVersionsProp;
		
	}
	
	/**
	 * 
	 * @return all KAR namespaces found in karVersions config property
	 */
	public static List<String> getKARNamespaces() {

		List<String> namespaces = new ArrayList<String>();
		ConfigurationProperty KARVersionsProp = KARFile.getKARVersionsConfigProperty();

		Iterator<ConfigurationProperty> karVersionsItr = KARVersionsProp
			.getProperties().iterator();
		
		while (karVersionsItr.hasNext()) {
			ConfigurationProperty prop = karVersionsItr.next();
			if (prop.getName().equals(KAR_VERSION_PROPERTY_NAME)) {
				ConfigurationProperty namespaceConfigProp = prop
					.getProperty(KAR_VERSION_NAMESPACE_PROPERTY_NAME);
				namespaces.add(namespaceConfigProp.getValue());
			}
		}

		return namespaces;
	}

	/**
	 * Return the local resource dir associated with given namespace, 
	 * null if can't be found.
	 * 
	 * @param namespace
	 * @return
	 */
	public static String getResourceDir(String namespace) {
		ConfigurationProperty KARVersionsProp = KARFile
				.getKARVersionsConfigProperty();

		Iterator<ConfigurationProperty> karVersionsItr = KARVersionsProp
				.getProperties().iterator();

		while (karVersionsItr.hasNext()) {
			ConfigurationProperty prop = karVersionsItr.next();
			if (prop.getName().equals(KAR_VERSION_PROPERTY_NAME)) {
				String namespaceFromConfig = prop
						.getProperty(KAR_VERSION_NAMESPACE_PROPERTY_NAME).getValue();
				if (namespace.equals(namespaceFromConfig)){
					return prop.getProperty(KAR_VERSION_RESOURCEDIR_PROPERTY_NAME).getValue();
				}
			}
		}
		if (isDebugging)
			log.error("Couldn't find resourceDir associated with namespace:"+namespace + " return null");
		return null;
	}
	
	/**
	 * Return the local resource filename associated with given namespace, 
	 * null if can't be found.
	 * 
	 * @param namespace
	 * @return
	 */
	public static String getResourceFileName(String namespace) {
		ConfigurationProperty KARVersionsProp = KARFile
				.getKARVersionsConfigProperty();

		Iterator<ConfigurationProperty> karVersionsItr = KARVersionsProp
				.getProperties().iterator();

		while (karVersionsItr.hasNext()) {
			ConfigurationProperty prop = karVersionsItr.next();
			if (prop.getName().equals(KAR_VERSION_PROPERTY_NAME)) {
				String namespaceFromConfig = prop
						.getProperty(KAR_VERSION_NAMESPACE_PROPERTY_NAME).getValue();
				if (namespace.equals(namespaceFromConfig)){
					return prop.getProperty(KAR_VERSION_RESOURCE_FILENAME_PROPERTY_NAME).getValue();
				}
			}
		}
		if (isDebugging)
			log.error("Couldn't find resourceFileName associated with namespace:"+namespace + " return null");
		return null;
	}

}
