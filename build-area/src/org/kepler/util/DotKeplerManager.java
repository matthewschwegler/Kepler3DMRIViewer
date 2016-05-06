/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-11 14:57:25 -0700 (Tue, 11 Sep 2012) $' 
 * '$Revision: 30631 $'
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

package org.kepler.util;

import java.io.File;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.util.Version;

/**
 * @author Aaron
 * 
 */
public class DotKeplerManager {

	public static final String onePointZeroPointZeroVersion = "1.0.0";
	/** constant for the 2.0.0 version of the .kepler directory */
	public static final String twoPointZeroPointZeroVersion = "2.0.0";
	/** constant for the 2.1 version of the .kepler directory */
	public static final String twoPointOneVersion = "2.1";
    /** constant for the 2.4 version of the .kepler directory */
    public static final String twoPointFourVersion = "2.4";
	/** The current version of the .kepler directory */
	public static final String currentDotKeplerVersion = twoPointFourVersion;


	// previously this was Config.KEPLER_USER_DIR
	private final static String _dotKeplerPath = System.getProperty("user.home")
			+ File.separator + ".kepler" + File.separator;

	//private static String versionMarkerFilename = "versionMarker.txt";

	private final static String _cacheDirName = "cache-" + currentDotKeplerVersion;
	private final static String _cacheDirectory = _dotKeplerPath + _cacheDirName
			+ File.separator;
	private final static File _cacheDir = new File(_cacheDirectory);

	private final static String _persistentDirName = "KeplerData";
	private final static String _persistentDirectory = System
			.getProperty("user.home")
			+ File.separator + _persistentDirName + File.separator;
	private final static File _persistentDir = new File(_persistentDirectory);
	
	private final static String _persistentUserDataDirName = "MyData";
	private final static String _persistentUserDataDirectory = 
	  _persistentDirectory + _persistentUserDataDirName + File.separator;
	private final static File _persistentUserDataDir = new File(_persistentUserDataDirectory);
	
	private final static String _persistentWorkflowsDirName = "workflows";
	private final static String _persistentWorkflowsDirectory = 
		_persistentDirectory + _persistentWorkflowsDirName + File.separator;
	private final static File _persistentWorkflowsDir = new File(_persistentWorkflowsDirectory);
	
	private final static String _persistentUserWorkflowsDirName = "MyWorkflows";
	private final static String _persistentUserWorkflowsDirectory = 
		_persistentWorkflowsDirectory + _persistentUserWorkflowsDirName + File.separator;
	private final static File  _persistentUserWorkflowsDir = new File(_persistentUserWorkflowsDirectory);
	
	private final static String _persistentModuleWorkflowsDirName = "module";
	private final static String _persistentModuleWorkflowsDirectory = 
		_persistentWorkflowsDirectory + _persistentModuleWorkflowsDirName + File.separator;
	private final static File _persistentModuleWorkflowsDir = new File(_persistentModuleWorkflowsDirectory);
	
	private final static String _persistentDocsDirName = "docs";
	private final static String _persistentDocsDirectory = 
		_persistentDirectory + _persistentDocsDirName + File.separator;
	private final static File _persistentDocsDir = new File(_persistentDocsDirectory);
	
	private final static String _configurationDirName = "configuration";

	private final static String _modulesDirName = "modules";
	
	/** Name of directory for log files. */
	private final static String _logDirName = "log";

	// singleton instance
	private static DotKeplerManager instance = null;

	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static DotKeplerManager getInstance() {

		if (instance == null) {
			instance = new DotKeplerManager();
			instance.initialize();
		}
		return instance;
	}

	/**
	 * Initialize method for Singleton class.
	 */
	public void initialize() {

		try {
			
			// NO LONGER DOING THIS:
			// check to see if we are using an older .kepler directory (from a
			// previous
			// version of kepler. If so, change the cache dir name
			//performDotKeplerVersionCheck();
			
			// We create the transient and persistent module directories here
			// so they all show up regardless of whether they're used
			// This just makes it more obvious for module developers that
			// these are here for them to use.
			for (Module m : ModuleTree.instance()) {
				if (m == null) {
					System.out
							.println("DotKeplerManager WARNING: module was null");
				}

				// --- Create a transient directory for each module.
				getTransientModuleDirectory(m.getStemName());
				// --- Create a persistent directory for each module
				getPersistentModuleDirectory(m.getStemName());
				// --- Create a persistent user data directory
				createUserDataDirectory();
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Module Directory could not be created");
			System.exit(0);
		}
	}

	/**
	 * Returns the path of the Kepler hidden working directory terminated with
	 * File.seperator. This directory is currently coded to be
	 * ${user.home}/.kepler. This directory is transient and nothing should be
	 * kept in it that can't be deleted and rebuilt.
	 * 
	 * @return String representation of the transient .kepler directory location
	 */
	public static String getDotKeplerPath() {
		return _dotKeplerPath;
	}

	/**
	 * Return the transient directory.
	 * 
	 * @return File representation of the transient directory, e.g. ~/.kepler
	 */
	public File getTransientDir() {
		return new File(getDotKeplerPath());
	}

	/**
	 * Return a File representing the cache directory.
	 * 
	 * @return File representation of the transient cache directory, e.g.
	 *         ~/.kepler/cache
	 */
	public File getCacheDir() {
		return _cacheDir;
	}

	/**
	 * Returns the path to a subdirectory of the cache directory. The pathname
	 * is normalized to use the system style File.separator.
	 * 
	 * @param String
	 *            a subdirectory of the working directory
	 * @return String normalized pathname of the cache subdirectory.
	 */
	public String getCacheDir(String path) {
		String pdir = _cacheDirectory + path;
		if (pdir.endsWith(File.separator)) {
			return pdir;
		} else {
			return pdir + File.separator;
		}
	}

	/**
	 * Return the Cache directory as a string making sure there is a
	 * File.separator character at the end.
	 * 
	 * @return String representation of the transient Cache Directory
	 */
	public String getCacheDirString() {
		String s = getCacheDir().toString();
		if (!s.endsWith(File.separator)) {
			s += File.separator;
		}
		return s;
	}

	/**
	 * Return a File representation of the persistent Kepler data directory.
	 * 
	 * @return File representation of the persistent directory, e.g.
	 *         ~/KeplerData
	 */
	public File getPersistentDir() {
		return _persistentDir;
	}

	/**
	 * Return a String representation of the Persistent Directory Location.
	 * 
	 * @return String representation of Persistent Directory Location
	 */
	public String getPersistentDirString() {
		String s = getPersistentDir().toString();
		if (!s.endsWith(File.separator)) {
			s += File.separator;
		}
		return s;
	}
	
	/**
	 * Return a File representation of the persistent workflows directory.
	 * 
	 * @return File representation of the persistent workflows directory, e.g.
	 *         ~/KeplerData/workflows
	 */
	public File getPersistentWorkflowsDir() {
		return _persistentWorkflowsDir;
	}
	
	/**
	 * Return a String representation of the persistent workflows Location.
	 * 
	 * @return String representation of persistent workflows Location
	 */
	public String getPersistentWorkflowsDirString() {
		String s = getPersistentWorkflowsDir().toString();
		if (!s.endsWith(File.separator)) {
			s += File.separator;
		}
		return s;
	}
	
	/**
	 * Return a File representation of the persistent user workflows directory.
	 * 
	 * @return File representation of the persistent user workflows directory, e.g.
	 *         ~/KeplerData/workflows/MyWorkflows
	 */
	public File getPersistentUserWorkflowsDir() {
		return _persistentUserWorkflowsDir;
	}
	
	/**
	 * Return the name of the default persistent user workflows directory.
	 * @return
	 */
	public String getPersistentUserWorkflowsDirName(){
		return _persistentUserWorkflowsDirName;
	}
	
	/**
	 * Return a String representation of the persistent user workflows Location.
	 * 
	 * @return String representation of persistent user workflows Location
	 */
	public String getPersistentUserWorkflowsDirString() {
		String s = getPersistentUserWorkflowsDir().toString();
		if (!s.endsWith(File.separator)) {
			s += File.separator;
		}
		return s;
	}
	
	 /**
   * Return the name of the default persistent user data directory.
   * @return
   */
  public String getPersistentUserDataDirName(){
    return _persistentUserDataDirName; 
  }
  
  /**
   * Return a String representation of the persistent user data Location.
   * 
   * @return String representation of persistent user data Location
   */
  public String getPersistentUserDataDirString() {
    String s = getPersistentUserDataDir().toString();
    if (!s.endsWith(File.separator)) {
      s += File.separator;
    }
    return s;
  }
  
  
  /**
   * Return a File representation of the persistent user workflows directory.
   * 
   * @return File representation of the persistent user workflows directory, e.g.
   *         ~/KeplerData/workflows/MyWorkflows
   */
  public File getPersistentUserDataDir() {
    return _persistentUserDataDir;
  }
	
	/**
	 * Return a File representation of the persistent module workflows directory.
	 * 
	 * @return File representation of the persistent module workflows directory, e.g.
	 *         ~/KeplerData/workflows/module
	 */
	public File getPersistentModuleWorkflowsDir() {
		return _persistentModuleWorkflowsDir;
	}
	
	/**
     * Return the directory that is designated for this module to read and write
     * persistent workflow files into. null is returned if a directory
     * could not be found.
     * 
     * @param moduleName
     * @return the directory where the module should write files
     */
    public File getPersistentModuleWorkflowsDir(String moduleName) {
        File persistentWorkflowDir = new File(getPersistentModuleWorkflowsDir(), moduleName);
        return persistentWorkflowDir;
    }

	/**
	 * Return a String representation of the persistent module workflows Location.
	 * 
	 * @return String representation of persistent module workflows Location
	 */
	public String getPersistentModuleWorkflowsDirString() {
		String s = getPersistentModuleWorkflowsDir().toString();
		if (!s.endsWith(File.separator)) {
			s += File.separator;
		}
		return s;
	}

	/**
	 * Return a File representation of the persistent documentation directory.
	 * 
	 * @return File representation of the persistent documentation directory, e.g.
	 *         ~/KeplerData/docs
	 */
	public File getPersistentDocumentationDir() {
		return _persistentDocsDir;
	}
	
	/**
	 * Return a String representation of the persistent documentation Location.
	 * 
	 * @return String representation of persistent documentation Location
	 */
	public String getPersistentDocumentationDirString() {
		String s = getPersistentDocumentationDir().toString();
		if (!s.endsWith(File.separator)) {
			s += File.separator;
		}
		return s;
	}
	
	
	/**
	 * Return the directory that is designated for this module to read and write
	 * *TEMPORARY* data to. null is returned if a directory could not be found.
	 * 
	 * @param moduleName
	 * @return File the directory where the module should write transient files
	 */
	public File getTransientModuleDirectory(String moduleName) {

		File transientModuleDir = new File(getCacheDir(), _modulesDirName
				+ File.separator + moduleName);
		if (!transientModuleDir.exists()) {
			boolean created = transientModuleDir.mkdirs();
			if (created) {

			}
		}
		return transientModuleDir;
	}

	/**
	 * Return the directory that is designated for this module to read and write
	 * persistent configuration files into. null is returned if a directory
	 * could not be found.
	 * 
	 * @param moduleName
	 * @return the directory where the module should write files
	 */
	public File getPersistentModuleDirectory(String moduleName) {

		File persistentModDir = new File(getPersistentDir(), _modulesDirName
				+ File.separator + moduleName);

		if (!persistentModDir.exists()) {
			boolean created = persistentModDir.mkdirs();
			if (created) {

			}
		}
		return persistentModDir;
	}

	/**
	 * Return the configuration directory that is inside the
	 * persistentModuleDirectory.
	 * 
	 * @param moduleName
	 * @return
	 */
	public File getModuleConfigurationDirectory(String moduleName) {

		File modConfigDir = null;
		//if moduleName includes version info, return module-name/configuration-x.y[.z]/
		if (Version.isVersioned(moduleName)){
			Version v = Version.fromVersionString(moduleName);
			String version = v.getVersionString();
			String baseName = v.getBasename();

			modConfigDir = new File(getPersistentModuleDirectory(baseName),
					_configurationDirName + Version.nameVersionSeparator + version + File.separator);
		}
		else{
			modConfigDir = new File(getPersistentModuleDirectory(moduleName),
					_configurationDirName + File.separator);
		}

		if (!modConfigDir.exists()) {
			boolean created = modConfigDir.mkdirs();
			if (created) {
				
			}
		}
		return modConfigDir;

	}
	
	/** Get the log directory. */
	public String getLogDirString() {
	    String dirStr = getCacheDir(_logDirName);
	    File dir = new File(dirStr);
	    if(!dir.exists() && !dir.mkdirs()) {
	        System.err.println("ERROR: could not create directory " + dirStr);
	    }
	    return dirStr;
	}
	
	/**
	 * Create the user data directory if it doesn't exist.
	 * User data directory will be ~/KeplerData/MyData
	 */
	private void createUserDataDirectory() {
		if (!_persistentUserDataDir.exists()) {
			_persistentUserDataDir.mkdirs();
		}
	}

	/**
	 * check for the signature of an older version of .kepler and changes
	 * directory names appropriately.
	 */
	/*
	private void performDotKeplerVersionCheck() throws Exception {
		String version = getVersionFromVersionMarker();
		if (version == null || !version.trim().equals(currentDotKeplerVersion)) {
			// TODO: add a new checkForVersionXXX method here for future version
			// conversions
			boolean version100 = checkForVersion100();
			if (version100) { // upgrade a 1.0.0 version to the current version
				System.out.println("Adapting to use a 1.0 .kepler directory.");
				_cacheDirName = _cacheDirName + "-"
						+ twoPointZeroPointZeroVersion;
			}
			// add a marker file so we know this .kepler version on the
			// filesystem
			addCurrentVersionMarker();
		} else {
			parseVersionMarker();
		}
	}
	*/

	/**
	 * parse the version marker and change the values of any variables set
	 * within it.
	 */
	/*
	private void parseVersionMarker() throws Exception {
		String marker = getMarkerText();
		StringTokenizer st = new StringTokenizer(marker, "\n");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			StringTokenizer st2 = new StringTokenizer(token, "=");
			String key = st2.nextToken();
			String val = st2.nextToken();
			if (key.equals("version")) {
				continue;
			} else if (key.equals("_cacheDirName")) {
				_cacheDirName = val;
				_cacheDir = new File(_cacheDirectory);
			} // add other keys here if they become necessary in future versions
		}
	}
	*/

	/**
	 * check for the existence of a kepler version 1.0.0 .kepler directory this
	 * is a special case because no version marker exists for 1.0.0
	 * installations
	 */
	/*
	private boolean checkForVersion100() throws Exception {
		File actorLibraryFile = new File(_dotKeplerPath, "actorLibrary");
		File cacheFile = new File(_dotKeplerPath, "cache");
		File karFile = new File(_dotKeplerPath, "kar");
		File libraryIndexFile = new File(_dotKeplerPath, "libraryIndex");
		File tmpFile = new File(_dotKeplerPath, "tmp");

		if (actorLibraryFile.exists() && cacheFile.exists() && karFile.exists()
				&& libraryIndexFile.exists() && tmpFile.exists()) {
			return true;
		}
		return false;
	}
	*/

	/**
	 * add a marker file to tell us what version of the .kepler directory we are
	 * currently using for future upgrades
	 */
	/*
	private void addCurrentVersionMarker() throws Exception {
		setVersionMarker(currentDotKeplerVersion);
	}
	*/

	/**
	 * write the version and class variables to the .kepler version marker file
	 */
	/*
	private void setVersionMarker(String version) throws Exception {
		File markerFile = new File(_dotKeplerPath, versionMarkerFilename);

		if (!markerFile.exists()) {
			File path = new File(_dotKeplerPath);
			path.mkdirs();
			markerFile.createNewFile();
		}
		FileWriter fw = new FileWriter(markerFile);
		version = "version=" + version + "\n";
		version += "_cacheDirName=" + _cacheDirName + "\n";
		// other keys can be added here in the future if they are needed
		fw.write(version, 0, version.length());
		fw.flush();
		fw.close();
	}
	*/

	/**
	 * get the current version from the .kepler version marker file. return null
	 * if there is no marker file
	 * 
	 * @return
	 */
	/*
	private String getVersionFromVersionMarker() throws Exception {
		String marker = getMarkerText();
		if (marker == null) {
			return null;
		}
		int startIndex = marker.indexOf("version=") + "version-".length();
		int stopIndex = marker.indexOf("\n", startIndex);
		return marker.substring(startIndex, stopIndex);
	}
	*/

	/**
	 * return the contents of the marker file
	 * 
	 * @return
	 */
	/*
	private String getMarkerText() throws Exception {
		File markerFile = new File(_dotKeplerPath, versionMarkerFilename);
		if (!markerFile.exists()) {
			return null;
		}
		FileReader fr = new FileReader(markerFile);
		char[] c = new char[1024];
		int numread = fr.read(c, 0, 1024);
		StringBuffer sb = new StringBuffer();
		while (numread != -1) {
			sb.append(new String(c, 0, numread));
			numread = fr.read(c, 0, 1024);
		}
		return sb.toString();
	}
	*/
}