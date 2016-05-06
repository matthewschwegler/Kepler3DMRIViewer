/**
 *  '$RCSfile$'
 *  '$Author: crawl $'
 *  '$Date: 2013-04-22 15:59:20 -0700 (Mon, 22 Apr 2013) $'
 *  '$Revision: 31954 $'
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
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.cache.CacheContent;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.sql.DatabaseFactory;

import ptolemy.util.MessageHandler;

/**
 * This is a singleton class to help manage KARs as they are stored in the
 * cache.
 * 
 * @author Aaron Schultz
 * 
 */
public class KARCacheManager {
	private static final Log log = LogFactory.getLog(KARCacheManager.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/** Table Names **/
	public static final String KARS_CACHED_TABLE_NAME = "KARS_CACHED";
	public static final String KAR_ERRORS_TABLE_NAME = "KAR_ERRORS";
	public static final String KAR_CONTENTS_TABLE_NAME = "KAR_CONTENTS";

	private Connection _conn;
	private Statement _stmt;
	private PreparedStatement _insertPrepStmt;
	private PreparedStatement _insContentsPrepStmt;
	private PreparedStatement _insErrorsPrepStmt;
	private PreparedStatement _karsLastModifiedPrepStmt;
	private PreparedStatement _fileForFilePrepStmt;
	private PreparedStatement _allKarsInCache;
	
	public KARCacheManager() {

		try {
			_conn = DatabaseFactory.getDBConnection();
			if (isDebugging) {
				log.debug(_conn.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// By creating the statement and keeping it around
			// make sure to close your resultsets to save memory
			_stmt = _conn.createStatement();
			_insertPrepStmt = _conn.prepareStatement("insert into "
					+ KARS_CACHED_TABLE_NAME
					+ " (file, lsid, version, reponame, lastmodified)"
					+ " values ( ?, ?, ?, ?, ? )");
			_insContentsPrepStmt = _conn.prepareStatement("insert into "
					+ KAR_CONTENTS_TABLE_NAME + " (file, lsid, name, type)"
					+ " values ( ?, ?, ?, ? ) ");
			_insErrorsPrepStmt = _conn.prepareStatement("insert into "
					+ KAR_ERRORS_TABLE_NAME + " (file, lsid, version, reponame, dependencies, lastmodified)"
					+ " values ( ?, ?, ?, ?, ?, ? ) ");
			_karsLastModifiedPrepStmt = _conn.prepareStatement("SELECT LASTMODIFIED FROM "
                    + KARS_CACHED_TABLE_NAME + " WHERE FILE = ? "
                    + "UNION SELECT LASTMODIFIED FROM "+ KAR_ERRORS_TABLE_NAME + " WHERE FILE = ?");
			_fileForFilePrepStmt = _conn.prepareStatement("SELECT FILE FROM "
                    + KARS_CACHED_TABLE_NAME + " WHERE FILE = ? "
                    + " UNION SELECT FILE FROM " + KAR_ERRORS_TABLE_NAME + " WHERE FILE = ?");
			_allKarsInCache = _conn.prepareStatement("SELECT FILE FROM " + KARS_CACHED_TABLE_NAME
	                + " UNION SELECT FILE FROM " + KAR_ERRORS_TABLE_NAME);



		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Vector<KARCacheError> getKARCacheErrors() {

		Vector<KARCacheError> errors = new Vector<KARCacheError>();

		String query = "SELECT "
				+ "FILE,LSID,VERSION,REPONAME,DEPENDENCIES " + " FROM "
				+ KAR_ERRORS_TABLE_NAME;
		ResultSet rs;
		try {
			if (isDebugging)
				log.debug(query);
			rs = _stmt.executeQuery(query);
			if (rs == null)
				throw new SQLException("Query Failed: " + query);
			while (rs.next()) {
				KARCacheError kce = new KARCacheError();
				try {
					String fileStr = rs.getString(1);
					File file = new File(fileStr);
					String lsidStr = rs.getString(2);
					KeplerLSID lsid = new KeplerLSID(lsidStr);
					String version = rs.getString(3);
					String reponame = rs.getString(4);
					String depStr = rs.getString(5);
					Vector<String> deps = ModuleDependencyUtil
						.parseDependencyString(depStr);
					
					kce.setFile(file);
					kce.setLsid(lsid);
					kce.setVersion(version);
					kce.setRepoName(reponame);
					kce.setDependencies(deps);
					
					errors.add(kce);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			errors = new Vector<KARCacheError>();
		}

		return errors;

	}

	/**
	 * Return all of the KARCacheContents.
	 * 
	 * @return
	 */
	public Vector<KARCacheContent> getKARCacheContents() {
		return getKarCacheContents("");
	}

	/**
	 * Return all of the KARCacheContents matching the specified type.
	 * 
	 * @param type
	 * @return
	 */
	public Vector<KARCacheContent> getKARCacheContents(String type) {
		String whereClause = " WHERE " + KAR_CONTENTS_TABLE_NAME + ".TYPE = '"
				+ type + "'";
		return getKarCacheContents(whereClause);
	}

	/**
	 * Return all of the KARCacheContents matching the specified lsid.
	 * 
	 * @param type
	 * @return
	 */
	public Vector<KARCacheContent> getKARCacheContents(KeplerLSID lsid) {
		String whereClause = " WHERE " + KAR_CONTENTS_TABLE_NAME + ".LSID = '"
				+ lsid.toString() + "'";
		return getKarCacheContents(whereClause);
	}

	/**
	 * Return all of the KARCacheContents in the specified KAR file.
	 * 
	 * @param type
	 * @return
	 */
	public Vector<KARCacheContent> getKARCacheContents(File karFile) {
		String whereClause = " WHERE " + KAR_CONTENTS_TABLE_NAME + ".FILE = '"
				+ karFile.toString() + "'";
		return getKarCacheContents(whereClause);
	}

	/**
	 * Return the contents of the KAR_CONTENTS table that match the given type
	 * or every row if the type given is the empty string.
	 * 
	 * @param type
	 * @return
	 */
	private Vector<KARCacheContent> getKarCacheContents(String whereClause) {

		Vector<KARCacheContent> contents = new Vector<KARCacheContent>();

		String query = "SELECT ";
		query += KAR_CONTENTS_TABLE_NAME + ".NAME, ";
		query += KAR_CONTENTS_TABLE_NAME + ".TYPE, ";
		query += KARS_CACHED_TABLE_NAME + ".FILE, ";
		query += KARS_CACHED_TABLE_NAME + ".LSID, ";
		query += KARS_CACHED_TABLE_NAME + ".VERSION, ";
		query += KARS_CACHED_TABLE_NAME + ".REPONAME, ";
		query += CacheManager.CACHETABLENAME + ".NAME, ";
		query += CacheManager.CACHETABLENAME + ".LSID, ";
		query += CacheManager.CACHETABLENAME + ".DATE, ";
		query += CacheManager.CACHETABLENAME + ".FILE, ";
		query += CacheManager.CACHETABLENAME + ".TYPE, ";
		query += CacheManager.CACHETABLENAME + ".CLASSNAME";
		query += " FROM " + KAR_CONTENTS_TABLE_NAME;
		query += " INNER JOIN " + KARS_CACHED_TABLE_NAME;
		query += " ON " + KAR_CONTENTS_TABLE_NAME + ".FILE = "
				+ KARS_CACHED_TABLE_NAME + ".FILE";
		query += " INNER JOIN " + CacheManager.CACHETABLENAME;
		query += " ON " + KAR_CONTENTS_TABLE_NAME + ".LSID = "
				+ CacheManager.CACHETABLENAME + ".LSID";

		if (!whereClause.trim().equals("")) {
			query += " " + whereClause;
		}
		ResultSet rs;
		try {
			if (isDebugging)
				log.debug(query);
			rs = _stmt.executeQuery(query);
			if (rs == null)
				throw new SQLException("Query Failed: " + query);
			while (rs.next()) {

				// Instantiate the KARCacheContent object that we'll return
				KARCacheContent kcc = new KARCacheContent();
				try {
					kcc.setName(rs.getString(1));
					kcc.setType(rs.getString(2));

					// Populate the Foreign Key data from the KARS_CACHED table
					KARCached kc = new KARCached();
					File karFile = new File(rs.getString(3));
					kc.setFile(karFile);
					KeplerLSID karLsid = new KeplerLSID(rs.getString(4));
					kc.setLsid(karLsid);
					kc.setVersion(rs.getString(5));
					kc.setRepoName(rs.getString(6));
					kcc.setKarCached(kc);

					// Populate the Foreign Key data from the CacheContent table
					CacheContent cc = new CacheContent();
					cc.setName(rs.getString(7));
					KeplerLSID ccLsid = new KeplerLSID(rs.getString(8));
					cc.setLsid(ccLsid);
					Date changed = new Date(rs.getLong(9));
					cc.setDateChanged(changed);
					File file = new File(rs.getString(10));
					cc.setFile(file);
					cc.setType(rs.getString(11));
					cc.setClassName(rs.getString(12));
					kcc.setCacheContent(cc);

					// Populate the Semantic types
					kcc.populateSemanticTypes(_stmt);

					contents.add(kcc);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			contents = new Vector<KARCacheContent>();
		}

		return contents;
	}

	public void clearKARCache() {

		try {
			String deleteAll = "delete from " + KARS_CACHED_TABLE_NAME;
			_stmt.executeUpdate(deleteAll);
			deleteAll = "delete from " + KAR_ERRORS_TABLE_NAME;
			_stmt.executeUpdate(deleteAll);
		} catch (SQLException sqle) {
			log.error(sqle.getMessage());
			sqle.printStackTrace();
		}

	}

	/**
	 * Update the KAR cache tables to reflect the kar files on disk.
	 * 
	 * @return boolean true if the cache was changed because of the
	 *         synchronization.
	 */
	public boolean synchronizeKARCacheWithLocalRepositories() {
		boolean contentsOnDiskHaveChanged = false;

		LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
		lrm.scanReposForKarFiles();
		Vector<File> karFiles = lrm.getKarFiles();

		if (isDebugging)
			log
					.debug("loop through all the kar files and make sure they are in the cache");
		for (int k = 0; k < karFiles.size(); k++) {
			
			try {
				File f = karFiles.elementAt(k);
												
				final long time = getLastModifiedTimeOfFileInCache(f.toString());
				
				// see if the last modified time is different than the time
				// when added to the cache
				if(time != f.lastModified()) {
				
				    // they are different, so update the cache.
				    
				    // see if the file was previously cached
				    if(time > 0) {
				        log.debug(f + " has a different modified time than in cache; will update cache.");
				        // remove previous version
				        removeKARFromCache(f);
				    } else {
				        log.debug(f + " is not in cache.");
				    }
				
				    KARFile kFile = new KARFile(f);
					// This KAR is not cached, go ahead and cache it
					boolean allDependenciesSatisfied = kFile.areAllModuleDependenciesSatisfied();
					if (allDependenciesSatisfied) {
						kFile.cacheKARContents();
					} else {
						insertKARError(kFile);
					}
					contentsOnDiskHaveChanged = true;
				}				
			} catch (Exception e) {
				log.warn("Unable to process kar file \""
						+ karFiles.elementAt(k).toString() + "\".", e);
			}
		}

		if (isDebugging)
			log
					.debug("loop through the cache and make sure there are matching KAR files");
		try {

			ResultSet rs = _allKarsInCache.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					String cachedFileStr = rs.getString(1);
					File cachedKar = new File(cachedFileStr);
					if (!karFiles.contains(cachedKar)) {
						removeKARFromCache(cachedKar);
						contentsOnDiskHaveChanged = true;
					}
				}
				rs.close();
			}
		} catch (Exception sqle) {
			sqle.printStackTrace();
		}

		// free up the kar files
		karFiles = null;

		if (contentsOnDiskHaveChanged) {
			log
					.info("The Cache was out of sync with KAR files in Local Repositories.");
			log
					.info("The Cache has been synchronized with KAR files in Local Repositories.");
		} else {
			log
					.info("The Cache is in sync with KAR files in Local Repositories.");
		}
		return contentsOnDiskHaveChanged;
	}

	/**
	 * Remove a KAR file from the cache. The contents of the KAR are also
	 * removed only if the objects don't exist in another KAR.
	 * 
	 * @param karLSID
	 *            the LSID of the kar to remove
	 * @return boolean true if and only if the kar file was removed
	 */
	public boolean removeKARFromCache(File karFile) {
		boolean success = false;
		try {

			// Store all of the LSIDs that this KAR contained
			Vector<KARCacheContent> karContents = getKARCacheContents(karFile);

			// Remove the KAR, which will cascade removal of the contents
			String delQuery = "DELETE FROM " + KARS_CACHED_TABLE_NAME
					+ " WHERE file = '" + karFile.toString() + "'";
			if (isDebugging)
				log.debug(delQuery);
			int rowsDel = _stmt.executeUpdate(delQuery);
			if (rowsDel >= 1) {
				success = true;
			}

			// Remove the KAR, which will cascade removal of the contents
			String delErrQuery = "DELETE FROM " + KAR_ERRORS_TABLE_NAME
					+ " WHERE file = '" + karFile.toString() + "'";
			if (isDebugging)
				log.debug(delErrQuery);
			int rowsErrDel = _stmt.executeUpdate(delErrQuery);
			if (rowsErrDel >= 1) {
				success = true;
			}

			if (success) {
				// Check to see if the LSIDs are in any other KARS
				// If they aren't then remove them from the Cache completely
				for (KARCacheContent entry : karContents) {
					Vector<KARCacheContent> otherEntries = getKARCacheContents(entry
							.getLsid());
					if (otherEntries.size() == 0) {
						CacheManager.getInstance()
								.removeObject(entry.getLsid());
					}
				}
			}
		} catch (Exception sqle) {
			sqle.printStackTrace();
		}
		return success;
	}

	/**
	 * 
	 * @param karFile
	 * @return boolean
	 */
	public boolean insertKARError(KARFile karFile) {
		if (isDebugging)
			log.debug("insertKARError(" + karFile.toString() + ")");

		File karFileLocation = karFile.getFileLocation();
		KeplerLSID lsid = karFile.getLSID();
		String version = karFile.getVersion();
		String reponame = karFile.getLocalRepoName();
		Vector<String> modDeps = karFile.getModuleDependencies();
		String depStrs = ModuleDependencyUtil.generateDependencyString(modDeps);
		
		try {
			_insErrorsPrepStmt.setString(1, karFileLocation.toString());
			_insErrorsPrepStmt.setString(2, lsid.toString());
			_insErrorsPrepStmt.setString(3, version);
			if (reponame == null) {
				_insErrorsPrepStmt.setNull(4, java.sql.Types.VARCHAR);
			} else {
				_insErrorsPrepStmt.setString(4, reponame);
			}
			_insErrorsPrepStmt.setString(5, depStrs);
			_insErrorsPrepStmt.setLong(6, karFileLocation.lastModified());
			int rows = _insErrorsPrepStmt.executeUpdate();
			if (isDebugging)
				log.debug(rows + " rows affected on insert");

			_conn.commit();
			_insErrorsPrepStmt.clearParameters();
			if (isDebugging)
				log.debug("insert succeeded");
			return true;
		} catch (Exception sqle) {
			try {
				_conn.rollback();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (isDebugging)
				log.debug(sqle.getMessage());
			// sqle.printStackTrace();
		}

		return false;
	}

	/**
	 * Insert a KARFile into the KARS_CACHED table. Called from KARFile class.
	 * This method DOES NOT insert any of the entries into the cache. Use
	 * insertEntryIntoCache and CacheManager.insertObject
	 * 
	 * @param karFile
	 * @return true if and only if the kar was inserted false if already exists
	 *         or error
	 */
	public boolean insertIntoCache(KARFile karFile) {
		if (isDebugging)
			log.debug("insertIntoCache(" + karFile.toString() + ")");

		File karFileLocation = karFile.getFileLocation();
		KeplerLSID lsid = karFile.getLSID();
		String version = karFile.getVersion();
		String reponame = karFile.getLocalRepoName();

		try {
			_insertPrepStmt.setString(1, karFileLocation.toString());
			_insertPrepStmt.setString(2, lsid.toString());
			_insertPrepStmt.setString(3, version);
			if (reponame == null) {
				_insertPrepStmt.setNull(4, java.sql.Types.VARCHAR);
			} else {
				_insertPrepStmt.setString(4, reponame);
			}
			_insertPrepStmt.setLong(5, karFile.getFileLocation().lastModified());    
			int rows = _insertPrepStmt.executeUpdate();
			if (isDebugging)
				log.debug(rows + " rows affected on insert");

			_conn.commit();
			_insertPrepStmt.clearParameters();
			if (isDebugging)
				log.debug("insert succeeded");
			return true;
		} catch (Exception sqle) {
			log.error("Failed to insert KAR " + karFile.getPath() + " into database: " + sqle.getMessage());
			try {
				_conn.rollback();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (isDebugging)
				log.debug(sqle.getMessage());
			// sqle.printStackTrace();
		}

		return false;
	}

	public void insertEntryIntoCache(File karFile, KeplerLSID entryLSID,
			String entryName, String entryType) {
		if (isDebugging)
			log.debug("insertEntryIntoCache(" + karFile.toString() + ","
					+ entryLSID + "," + entryName + "," + entryType + ")");

		// we log the fact that this lsid is found in this KAR
		try {
			_insContentsPrepStmt.setString(1, karFile.toString());
			_insContentsPrepStmt.setString(2, entryLSID.toString());
			_insContentsPrepStmt.setString(3, entryName);
			_insContentsPrepStmt.setString(4, entryType);
			_insContentsPrepStmt.executeUpdate();
			_conn.commit();
			_insContentsPrepStmt.clearParameters();
		} catch (SQLException sqle) {
			// if this entry is not found in the CACHECONTENTTABLE
			// then we can not add it to the KAR_CONTENTS table
			// because it violates the foreign key. So just ignore it
			if (isDebugging) {
				log.debug(sqle.getMessage());
				sqle.printStackTrace();
			}
		}
	}

	/**
	 * Determine if the KAR represented by the supplied LSID has already been
	 * cached.
	 * 
	 * @param karLSID
	 * @deprecated Cached Kars now use the File as the primary key Use
	 *             isCached(File) instead
	 * */
	public boolean isCached(KeplerLSID karLSID) {
		if (isDebugging)
			log.debug("isCached(" + karLSID.toString() + ")");
		if (karLSID == null)
			return false;
		try {
			ResultSet rs = _stmt.executeQuery("SELECT count(lsid) FROM "
					+ KARS_CACHED_TABLE_NAME + " where lsid = '"
					+ karLSID.toString() + "'");
			if (rs == null) {
				return false;
			}
			if (rs.next()) {
				Integer cnt = rs.getInt(1);
				if (isDebugging)
					log.debug(cnt);
				if (cnt >= 1) {
					rs.close();
					return true;
				}
			}
			rs.close();
		} catch (Exception sqle) {
			sqle.printStackTrace();
		}
		return false;
	}

	/**
	 * Determine if the KAR represented by the supplied File has already been
	 * cached. NOTE: this method does not check if the File's contents have
	 * changed since cached.
	 * 
	 * @param File karFile
	 * 
	 */
	public boolean isCached(File karFile) {
		if (karFile != null){
			if (isDebugging)
				log.debug("isCached(" + karFile.toString() + ")");
		}
		if (karFile == null){
			return false;
		}
		try {
		    String name = karFile.toString();
		    _fileForFilePrepStmt.setString(1, name);
		    _fileForFilePrepStmt.setString(2, name);
			ResultSet rs = _fileForFilePrepStmt.executeQuery();
			if (rs == null) {
				return false;
			}
			if (rs.next()) {
				rs.close();
				return true;
			}
			rs.close();
		} catch (Exception sqle) {
			sqle.printStackTrace();
		}
		return false;
	}
	
	/** Get the last modified time of a file in the cache. Returns
	 *  < 0 if the file is not in the cache.
	 */
	public long getLastModifiedTimeOfFileInCache(String filename) {

	    try {
    	    ResultSet result = null;
    	    try {
    	        _karsLastModifiedPrepStmt.setString(1, filename);
                _karsLastModifiedPrepStmt.setString(2, filename);
                result = _karsLastModifiedPrepStmt.executeQuery();
                if(result.next()) {
                    return result.getLong(1);
                }

    	    } finally {
                if(result != null) {
                    result.close();
                }
            }
        } catch (Exception e) {
            MessageHandler.error("Error checking if " + filename + " is in cache.", e);
        }

        return -1;
	}

	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static KARCacheManager getInstance() {
		return KARCacheManagerHolder.INSTANCE;
	}

	private static class KARCacheManagerHolder {
		private static final KARCacheManager INSTANCE = new KARCacheManager();
	}

}
