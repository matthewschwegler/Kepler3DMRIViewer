/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-02-11 09:34:36 -0800 (Tue, 11 Feb 2014) $' 
 * '$Revision: 32585 $'
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

/**
 * 
 */
package org.kepler.objectmanager.library;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.util.Version;
import org.kepler.kar.KARCacheContent;
import org.kepler.kar.KARCacheError;
import org.kepler.kar.KARCacheManager;
import org.kepler.kar.KARCached;
import org.kepler.kar.KAREntry;
import org.kepler.kar.KARFile;
import org.kepler.kar.handlers.ActorMetadataKAREntryHandler;
import org.kepler.moml.KeplerActorMetadata;
import org.kepler.moml.KeplerMetadataExtractor;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheContent;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.cache.LocalRepositoryManager.LocalRepository;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.sms.NamedOntClass;
import org.kepler.sms.NamedOntModel;
import org.kepler.sms.OntologyCatalog;
import org.kepler.util.DotKeplerManager;


/**
 * A library index for keeping track of the tree structure in the component
 * library. This class uses preorder tree traversal method in an SQL table to
 * maintain the indexed structure of the component library instead of using XML
 * (as was previously done). This allows for easier control over the ordering of
 * the contents of the tree, faster and more targeted access to the contents of
 * the library using SQL queries (and some simple math), and it also introduces
 * a very useful new Identifier for the contents of the tree, the LIID.
 * 
 * Two companion tables, LIBRARY_LSIDS and LIBRARY_ATTRIBUTES, are used to store
 * additional metadata about the library contents. The LIBRARY_LSIDS table
 * serves to track the existence of multiple LSIDs for one single Component in
 * the library. This is necessary for handling revision management and the user
 * will be allowed to toggle which LSID the component is currently associated
 * with, without destroying the look of the component library tree. The
 * LIBRARY_ATTRIBUTES table is used for storing single string values that are
 * associated with component library items. For example, a KAR item can have the
 * location of that KAR on disk stored as an attribute of the library index
 * item.
 * 
 * @author Aaron Schultz
 */
public class LibIndex {
	private static final Log log = LogFactory.getLog(LibIndex.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * the name of the table in the database
	 */
	public static final String LIBRARY_INDEX_TABLE_NAME = "LIBRARY_INDEX";

	/**
     * the name of the table in the database used to store attributes
     */
    public static final String LIBRARY_LSIDS_TABLE_NAME = "LIBRARY_LSIDS";

    /**
     * the name of the table in the database used to store attributes
     */
    public static final String LIBRARY_ATTRIBUTES_TABLE_NAME = "LIBRARY_ATTRIBUTES";
    
    /**
	 * Map of integers for the different types of objects that are stored in the
	 * LIBRARY_INDEX table. To add a new type to the tree you must do a few
	 * things. Add a new static integer here. Modify LibraryManager.getTreeItem
	 * to handle the new type. Add methods to this class for recognizing the
	 * type in KAR files.
	 */
	public static final int TYPE_COMPONENT = 1;
	public static final int TYPE_NAMED_OBJ = 2;
	public static final int TYPE_ONTOLOGY = 3;
	public static final int TYPE_CONCEPT = 4;
	public static final int TYPE_FOLDER = 5;
	public static final int TYPE_LOCALREPO = 6;
	public static final int TYPE_KAR = 7;
	public static final int TYPE_KAR_ERROR = 8;
	public static final int TYPE_KARFOLDER = 9;
	public static final int TYPE_OBJECT = 10;

	/**
	 * Attribute names that are used to keep information about Library nodes.
	 */
	public static final String ATT_REPOPATH = "REPOPATH";
	public static final String ATT_FOLDER = "FOLDER";
	public static final String ATT_KARENTRYPATH = "KARENTRYPATH";
	public static final String ATT_KARFILE = "KARFILE";
	public static final String ATT_XMLFILE = "XMLFILE";
	public static final String ATT_CLASSNAME = "CLASSNAME";

	private LibSearch _searcher;
	private Hashtable<Integer, Integer> _searchTypeMap;

	// Convenience references
	private Connection _conn;
	private Statement _stmt;

	// Prepared Statements
	private PreparedStatement _updateOrderPrepStmt;
	private PreparedStatement _updateLsidPrepStmt;
	private PreparedStatement _getLIIDForKarPrepStmt;
	private PreparedStatement _getLIIDForXMLPrepStmt;
	private PreparedStatement _getLIIDForFolderPrepStmt;
	private PreparedStatement _getLIIDForKarEntryPrepStmt;
	private PreparedStatement _getLIIDForKarErrorPrepStmt;
	private PreparedStatement _getLIIDForRepositoryPrepStmt;
	private PreparedStatement _getLIIDForOntologyNamePrepStmt;
	private PreparedStatement _getLIIDForOntologyClassPrepStmt;
	private PreparedStatement _getNumRowsInLibraryIndexPrepStmt;
	private PreparedStatement _getLIIDOfParentsPrepStmt;
	private PreparedStatement _getLIIDForParentAndNamePrepStmt;
	private PreparedStatement _getLIIDForNullParentAndNamePrepStmt;
	private PreparedStatement _getRangeForLIIDPrepStmt;
	private PreparedStatement _getLIIDForParentPrepStmt;
	private PreparedStatement _getLftRgtSumPrepStmt;
	private PreparedStatement _getLIIDRootsPrepStmt;
	private PreparedStatement _deleteLIIDFromLibraryAttributesPrepStmt;
	private PreparedStatement _insertIntoLibraryAttributesPrepStmt;
	private PreparedStatement _getNumLIIDForLIIDAndLSIDPrepStmt;
	private PreparedStatement _insertIntoLibraryLSIDsPrepStmt;

	private LibraryManager _libraryManager;
	
	/** Level for root objects in the tree. */
	private static final int ROOT_LEVEL = 1;

	/** Root item for Demos */
	private LibItem _demosFolderItem;
	
	/** Pattern for the modules' persistent workflow directory. */
    private Pattern _demosFolderPattern = Pattern.compile(Pattern
            .quote(DotKeplerManager.getInstance()
                    .getPersistentModuleWorkflowsDirString())
            + "([^" + Pattern.quote(File.separator) + "]+)");

	/**
	 * There are two ways to insert rows into the table. One was is to have the
	 * insert statement handle the ordering of the hierarchy at insert time.
	 * This is very convenient for inserting just one row. But very time
	 * consuming for inserting many rows. For many rows, one may toggle the
	 * _orderedInsert to Off and once the rows have been inserted call the
	 * refreshPreorderValues method to order the rows based on parent
	 * relationships.
	 */
	private boolean _orderedInsert;

	/**
	 * A constructor that is given a connection to the database.
	 * 
	 * @param conn
	 */
	public LibIndex(Connection conn) {
		initialize(conn);
	}

	/**
	 * Return the current value of orderedInsert. A value of false means that
	 * items inserted will not be automatically ordered during insert.
	 * 
	 * @return
	 */
	public boolean isOrderedInsert() {
		return _orderedInsert;
	}

	/**
	 * In general orderedInsert should always be true. When inserting many rows
	 * however it is faster to not order the entire table dynamically on every
	 * insert but to wait until all the rows have been inserted and then update
	 * the ordering based on parent relationships using the
	 * refreshPreorderValues method. This method allows you to toggle ordering
	 * during insert to on (true) or off (false).
	 * 
	 * @param orderedInsert
	 */
	public void setOrderedInsert(boolean orderedInsert) {
		_orderedInsert = orderedInsert;
	}

	/**
	 * Initialize the instance.
	 * 
	 * @param conn
	 */
	private void initialize(Connection conn) {
		if (isDebugging)
			log.debug("initialize(" + conn.toString() + ")");
		_conn = conn;

		_libraryManager = LibraryManager.getInstance();
		
		try {
			// By creating the statement and keeping it around
			// make sure to close your resultsets to save memory
			_stmt = _conn.createStatement();

			_updateOrderPrepStmt = _conn.prepareStatement("update "
					+ LIBRARY_INDEX_TABLE_NAME
					+ " SET LFT=?, RGT=? WHERE LIID=?");
			_updateLsidPrepStmt = _conn.prepareStatement("update "
					+ LIBRARY_INDEX_TABLE_NAME + " SET LSID=? WHERE LIID=?");
			
			_getLIIDForKarPrepStmt = _conn.prepareStatement("SELECT LIID FROM "
		                + LIBRARY_ATTRIBUTES_TABLE_NAME + " WHERE NAME = '"
		                + ATT_KARFILE + "' AND VALUE = ?");

			_getLIIDForXMLPrepStmt = _conn.prepareStatement("SELECT LIID FROM "
                    + LIBRARY_ATTRIBUTES_TABLE_NAME + " WHERE NAME = '"
                    + ATT_XMLFILE + "' AND VALUE = ?");
			
			_getLIIDForFolderPrepStmt = _conn.prepareStatement("SELECT LIID FROM "
		                + LIBRARY_ATTRIBUTES_TABLE_NAME + " WHERE NAME = '"
		                + ATT_FOLDER + "' AND VALUE = ?");

			
		    _getLIIDForKarEntryPrepStmt = _conn.prepareStatement("SELECT LIID FROM "
		                + LIBRARY_ATTRIBUTES_TABLE_NAME + " WHERE NAME = '"
		                + ATT_KARENTRYPATH + "' AND VALUE = ?");

		    _getLIIDForKarErrorPrepStmt = _conn.prepareStatement("SELECT LIID FROM "
		                + LIBRARY_ATTRIBUTES_TABLE_NAME + " WHERE NAME = '"
		                + ATT_KARFILE + "' AND VALUE = ?");

		    _getLIIDForRepositoryPrepStmt = _conn.prepareStatement("SELECT LIID FROM "
	                + LIBRARY_ATTRIBUTES_TABLE_NAME + " WHERE NAME = '"
	                + ATT_REPOPATH + "' AND VALUE = ?");

		    _getLIIDForOntologyNamePrepStmt = _conn.prepareStatement("SELECT LIID FROM "
		            + LIBRARY_INDEX_TABLE_NAME + " WHERE NAME = ? AND LEVEL = 1");

		    _getLIIDForOntologyClassPrepStmt = _conn.prepareStatement("SELECT LIID FROM "
		            + LIBRARY_INDEX_TABLE_NAME + " WHERE LSID = ?");

		    _getNumRowsInLibraryIndexPrepStmt = _conn.prepareStatement("SELECT count(LIID) from "
	                + LIBRARY_INDEX_TABLE_NAME);

		    _getLIIDOfParentsPrepStmt = _conn.prepareStatement("SELECT LIID from "
		            + LIBRARY_INDEX_TABLE_NAME + " WHERE LFT < ? AND RGT > ? "
		            + "ORDER BY LEVEL");

		    _getLIIDForParentAndNamePrepStmt = _conn.prepareStatement("select liid from "
		            + LIBRARY_INDEX_TABLE_NAME + " where parent = ? and name = ?");

		    _getLIIDForNullParentAndNamePrepStmt = _conn.prepareStatement("select LIID from "
		            + LIBRARY_INDEX_TABLE_NAME + " where PARENT IS NULL AND NAME = ?");

		    _getRangeForLIIDPrepStmt = _conn.prepareStatement("SELECT LFT,RGT from "
		            + LIBRARY_INDEX_TABLE_NAME + " WHERE LIID = ?");
		        
		    _getLIIDForParentPrepStmt = _conn.prepareStatement("SELECT liid FROM "
		            + LIBRARY_INDEX_TABLE_NAME + " WHERE parent = ? ORDER BY TYPE,NAME");

		    _getLftRgtSumPrepStmt = _conn.prepareStatement("select (sum(LFT) + sum(RGT)) FROM "
		            + LIBRARY_INDEX_TABLE_NAME);
		    
		    _getLIIDRootsPrepStmt = _conn.prepareStatement("SELECT LIID FROM "
		            + LIBRARY_INDEX_TABLE_NAME + " WHERE LEVEL = 1 ORDER BY TYPE,NAME");
		    
		    _deleteLIIDFromLibraryAttributesPrepStmt = _conn.prepareStatement("DELETE FROM "
		            + LIBRARY_ATTRIBUTES_TABLE_NAME + " WHERE LIID = ?");

            _insertIntoLibraryAttributesPrepStmt = _conn.prepareStatement("INSERT INTO "
                    + LIBRARY_ATTRIBUTES_TABLE_NAME + " (LIID,NAME,VALUE) values (?, ?, ?)"); 
                        
            _getNumLIIDForLIIDAndLSIDPrepStmt = _conn.prepareStatement("SELECT count(LIID) FROM "
                    + LIBRARY_LSIDS_TABLE_NAME + " WHERE LIID = ? AND LSID = ?");
            
            _insertIntoLibraryLSIDsPrepStmt = _conn.prepareStatement("insert into "
                    + LIBRARY_LSIDS_TABLE_NAME + " (LIID,LSID) values (?,?)");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
		
		// Share the connection with the LibSearch
		_searcher = new LibSearch(_conn);
		initSearchMap();

		setOrderedInsert(true);
	}

	/**
	 * Delete all rows in the LIBRARY_INDEX table. This will also delete all
	 * rows in the LIBRARY_ATTRIBUTES table by cascading foreign key deletes.
	 */
	public void clear() {
		String clear = "delete from " + LIBRARY_INDEX_TABLE_NAME;
		if (isDebugging)
			log.debug(clear);
		String resetAutoInc = "ALTER TABLE " + LIBRARY_INDEX_TABLE_NAME
				+ " ALTER COLUMN LIID RESTART WITH 1";
		if (isDebugging)
			log.debug(resetAutoInc);
		try {
			_stmt.executeUpdate(clear);
			_stmt.execute(resetAutoInc);

			getSearcher().clear();
			_stmt.getConnection().commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		_demosFolderItem = null;
	}

	/**
	 * Completely delete the library index and recreate it from the contents of
	 * cached KARs.
	 */
	public void rebuild() {
		log.info("Building LibIndex...");
		clear();

		setOrderedInsert(false);
		try {

			KARCacheManager kcm = KARCacheManager.getInstance();

			// Add all Kar Contents to the Library Index table
			Vector<KARCacheContent> contents = kcm.getKARCacheContents();
			for (KARCacheContent content : contents) {

				assureOntologyComponent(content);
				assureKarEntry(content);

			}

			// Add all Kar Errors to the Library
			Vector<KARCacheError> errors = kcm.getKARCacheErrors();
			for (KARCacheError error : errors) {
				assureKarError(error);
			}
			
			for(File xmlFile : LocalRepositoryManager.getInstance().getXMLFiles()) {
			    assureXML(xmlFile);
			}

			// Refresh the ordering of the library (this is much faster than
			// updating the order every time we insert)
			refreshPreorderValues();

			// because we insert with no order here, we can't
			// easily determine the path to any given LibItem
			// so we wait until all individual LibItems have been
			// created and ordered to finish up indexing the items
			// for the search
			finishSearchIndexing();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		setOrderedInsert(true);
	}

	/**
	 * Select all of the existing LibItems from the Library Index and insert all
	 * of their parent items into the Search Index.
	 * 
	 * @throws SQLException
	 */
	private void finishSearchIndexing() throws SQLException {
		Vector<LibItem> items = getItems();
		for (LibItem item : items) {
			addAllParentsToSearchIndex(item);
		}
	}

	/**
	 * Update the default LSID for the given LIID to the given LSID.
	 * 
	 * @param liid
	 * @param lsid
	 * @throws SQLException
	 */
	public void updateDefaultLsid(int liid, KeplerLSID lsid)
			throws SQLException {
		if (isDebugging)
			log.debug("updateDefaultLsid(" + liid + "," + lsid + ")");

		try {
			_updateLsidPrepStmt.setString(1, lsid.toString());
			_updateLsidPrepStmt.setInt(2, liid);

			_updateLsidPrepStmt.executeUpdate();
			_updateLsidPrepStmt.clearParameters();
			_conn.commit();
		} catch (SQLException sqle) {
			throw sqle;
		}
	}

	/**
	 * Remove all data from the index database that is associated with the given
	 * Library Index ID and all of the data of it's children.
	 * 
	 * @param liid
	 * @return
	 */
	public boolean removeItem(int liid) {
		boolean success = false;
		LibItem li = null;
        try {
            li = _libraryManager.getPopulatedLibItem(liid);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		if (li != null) {
			success = removeItem(li);
		}
		return success;
	}

	/**
	 * Remove all data from the index database that is associated with the given
	 * Library Item (using the Library Index ID) and all of the data of it's
	 * children. This method is the same as removeItem(int liid). It is here for
	 * convenience.
	 * 
	 * @param li
	 * @return
	 */
	public boolean removeItem(LibItem li) {
		String delete = "DELETE FROM " + LibIndex.LIBRARY_INDEX_TABLE_NAME
				+ " WHERE LIID = " + li.getLiid();
		// NOTE: the Foreign Key to the parent LIID prevents us from doing this:
		// + " WHERE LFT >= " + li.getLeft() + " AND RGT <= " + li.getRight();
		// which is faster performance wise. Instead we'll trust the ON DELETE
		// CASCADE for the parent foreign key to properly remove all the
		// children of the LIID we're removing which is a little slower
		// performance wise but still should work fine

		String updateLFT = "UPDATE " + LibIndex.LIBRARY_INDEX_TABLE_NAME
				+ " SET LFT = LFT - " + (li.getRight() - li.getLeft() + 1)
				+ " WHERE LFT > " + li.getLeft();
		String updateRGT = "UPDATE " + LibIndex.LIBRARY_INDEX_TABLE_NAME
				+ " SET RGT = RGT - " + (li.getRight() - li.getLeft() + 1)
				+ " WHERE RGT > " + li.getLeft();
		try {
			_stmt.executeUpdate(delete);
			_stmt.executeUpdate(updateLFT);
			_stmt.executeUpdate(updateRGT);
			_stmt.getConnection().commit();
		} catch (Exception e) {
			try {
				_stmt.getConnection().rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return false;
		}
		return true;
	}

	/**
	 * This method will remove the given LSID from being associated with any of
	 * the Library Items. If there is another LSID associated with the given
	 * item then it will be assigned as the default. If the Library Item is only
	 * associated with the given LSID then the Item itself will be removed.
	 * 
	 * @param lsid
	 * @return Vector<Integer> all LIIDs that got removed
	 * @throws SQLException
	 */
	public Vector<Integer> removeItemsByLsid(KeplerLSID lsidToRemove)
			throws SQLException {
		Vector<Integer> liidsThatGotRemoved = new Vector<Integer>();

		// First keep the history of all LIIDs this LSID was associated with
		Vector<KeplerLSID> kludge = new Vector<KeplerLSID>(1);
		kludge.add(lsidToRemove);
		Vector<Integer> liidAssociations = LibraryManager.getInstance()
				.getLiidsFor(kludge);

		// Now remove all the LIID -> LSID associations for this LSID
		String remove = "DELETE FROM " + LIBRARY_LSIDS_TABLE_NAME
				+ " WHERE lsid = '" + lsidToRemove + "'";
		_stmt.executeUpdate(remove);

		// Now go through all the liids
		for (Integer liid : liidAssociations) {
			// check if this LSID is the default LSID
			String defaultQuery = "SELECT LSID FROM "
					+ LibIndex.LIBRARY_INDEX_TABLE_NAME + " WHERE LIID = "
					+ liid.intValue();
			if (isDebugging)
				log.debug(defaultQuery);
			ResultSet rs1 = null;
			ResultSet rs2 = null;
			try {
				rs1 = _stmt.executeQuery(defaultQuery);
				if (rs1 == null)
					throw new SQLException("Query Failed: " + defaultQuery);
				if (rs1.next()) {
					String currentDefaultLsidStr = rs1.getString(1);
					KeplerLSID currentDefaultLsid = null;
					try {
						currentDefaultLsid = new KeplerLSID(currentDefaultLsidStr);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (currentDefaultLsid.equals(lsidToRemove)) {
						// This is the default LSID
						// check if there are any LSIDs that can become the new
						// default
						String newDefaultQuery = "SELECT LSID FROM "
								+ LIBRARY_LSIDS_TABLE_NAME
								+ " WHERE LIID = " + liid.intValue()
								+ " order by LSID DESC";
						if (isDebugging)
							log.debug(newDefaultQuery);
						rs2 = _stmt.executeQuery(newDefaultQuery);
						if (rs2 == null)
							throw new SQLException("Query Failed: "
									+ newDefaultQuery);
						if (rs2.next()) {
							// Yes there are other LSIDs associated with this LIID,
							// set the first one we get back as the new default
							// (hopefully the "order by DESC" will give us the
							// highest revision)
							String updateDefaultLSID = "UPDATE "
									+ LibIndex.LIBRARY_INDEX_TABLE_NAME
									+ " SET LSID = '" + rs2.getString(1)
									+ " WHERE LIID = " + liid.intValue();
							if (isDebugging)
								log.debug(updateDefaultLSID);
							_stmt.executeUpdate(updateDefaultLSID);
						} else {
							// No there are no more LSIDs associated with this LIID
							// so let's remove this LIID if it is a leaf
							LibItem li = _libraryManager.getPopulatedLibItem(liid);
							// double check that this is a leaf node
							if ((li.getRight() - li.getLeft()) == 1) {
								li.delete(_stmt);
								liidsThatGotRemoved.add(li.getLiid());
							} else {
								// theoretically we never run into this BUT
								// if we do let's go ahead and null out the
								// default lsid
								String updateDefaultLSID = "UPDATE "
										+ LibIndex.LIBRARY_INDEX_TABLE_NAME
										+ " SET LSID = NULL" + " WHERE LIID = "
										+ liid.intValue();
								_stmt.executeUpdate(updateDefaultLSID);
								log
										.warn("Nulled default LSID because the node is not a leaf");
							}
						}
					} else {
						// This is not the default LSID so we're done with this one
					}
				}
			} finally {
				if(rs1 != null) {
					rs1.close();
				}
				if(rs2 != null) {
					rs2.close();
				}
			}
		}
		_stmt.getConnection().commit();
		return liidsThatGotRemoved;
	}

	/**
	 * Assures that the LibIndex row under the folder hierarchy exists for the
	 * given KARCacheContent. This method shouldn't really be used. It is only
	 * public so the LibraryManager.addKar() method can use it.
	 * 
	 * @param content
	 * @throws SQLException
	 */
	public LibItem assureKarEntry(KARCacheContent content) throws SQLException {
		if (isDebugging)
			log.debug("assureKarEntry(" + content.getName() + ")");

		// add the object to the folders hierarchy
		File karFile = content.getKarFile();
		String entry = content.getName();

		LibItem li = findKarEntry(karFile, entry);
		if (li == null) {
			LibItem liParent = null;
			String[] entryPath = entry.split("/");
			if (entryPath.length > 1) {
				// entry is in a subfolder
				String folderPath = new String();
				for (int i = 0; i < (entryPath.length - 1); i++) {
					folderPath = entryPath[i] + "/";
				}
				folderPath = folderPath.substring(0, folderPath.length() - 1);
				liParent = assureKarFolder(content.getKarCached(), folderPath);
			} else {
				liParent = assureKar(content.getKarCached());
			}
			
			CacheContent cc = content.getCacheContent();
			String actorName = cc.getName();

			li = new LibItem();
			li.setName(actorName);

			int t = determineLibIndexType(content);
			if (t > 0) {
				li.setType(t);
			} else {
				log
						.error("Could not determine the LibIndex type for KARCacheContent");
				return null;
			}

			li.setParent(liParent.getLiid());
			li.setLevel(liParent.getLevel() + 1);
			li.setLsid(content.getLsid());
			li.addAttribute(ATT_KARENTRYPATH, getKarEntryPath(karFile, entry));
			String className = content.getCacheContent().getClassName();
			if (className != null) {
				li.addAttribute(ATT_CLASSNAME, className);
				transferAttributes(li, content);
			}
			insertNoOrder(li);

		} else {
			// it is already there
			// this is probably an unnecessary check
		}
		return li;
	}
	
	private static Map<KeplerLSID, Map<String, String>> cachedItemAttributes = new HashMap<KeplerLSID, Map<String, String>>();

	private void transferAttributes(LibItem li, KARCacheContent content) {

		if (cachedItemAttributes.containsKey(li.getLsid())) {
			Map<String, String> attributes = cachedItemAttributes.get(li.getLsid());
			for (String name : attributes.keySet()) {
				li.addAttribute(name, attributes.get(name));
			}
			return;
		}

		KARFile karFile = null;
		try {
			karFile = new KARFile(content.getKarFile());
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		
		KAREntry karEntry = new KAREntry(content.getName());
		ActorMetadataKAREntryHandler keh = new ActorMetadataKAREntryHandler();
		ActorCacheObject aco;
		try {
			aco = (ActorCacheObject) keh.cache(karFile, karEntry);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			return;
		}

		Map<String, String> attributes = new HashMap<String, String>();
		for (String attributeName : aco.getAttributeNames()) {
			String attributeValue = aco.getAttribute(attributeName);
			attributes.put(attributeName, attributeValue);
			li.addAttribute(attributeName, attributeValue);
		}

		cachedItemAttributes.put(li.getLsid(), attributes);
	}

	/**
	 * Determine the appropriate LibIndex type of the given KARCacheContent
	 * 
	 * @param kcc
	 * @return
	 */
	public int determineLibIndexType(KARCacheContent content) {
		int theType = -1;
		String cacheObjectType = content.getCacheContent().getType();
		boolean isActorCacheObject = false;
		try {
			String actorCacheObjectType = "org.kepler.objectmanager.cache.ActorCacheObject";
			if (cacheObjectType.equals(actorCacheObjectType)) {
				isActorCacheObject = true;
			} else if(cacheObjectType.equals("org.kepler.objectmanager.cache.TextFileCacheObject")) {
			  isActorCacheObject = false;
			  return LibIndex.TYPE_OBJECT;
			} else {
				isActorCacheObject = KARFile.isSubclass(actorCacheObjectType,
						cacheObjectType);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if (isActorCacheObject) {
			theType = LibIndex.TYPE_COMPONENT;
		} else {
			String entryObjectType = content.getType();
			boolean isGenericNamedObjType = false;
			try {
				String namedObjectType = "ptolemy.kernel.util.NamedObj";
				if (entryObjectType.equals(namedObjectType)) {
					isGenericNamedObjType = true;
				} else {
					isGenericNamedObjType = KARFile.isSubclass(namedObjectType,
							entryObjectType);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			if (isGenericNamedObjType) {
				theType = LibIndex.TYPE_NAMED_OBJ;
			} else {
				theType = LibIndex.TYPE_OBJECT;
			}
		}
		return theType;
	}

	/**
	 * Assures that the LibIndex rows for the KARCacheContent object exist under
	 * the Ontology tree. This method shouldn't really be used. It is only
	 * public so the LibraryManager.addKar() method can use it.
	 * 
	 * @param content
	 */
	public Vector<LibItem> assureOntologyComponent(KARCacheContent content) {
		Vector<LibItem> items = new Vector<LibItem>();

		OntologyCatalog oc = OntologyCatalog.instance();

		// add the object in the ontology class hierarchy
		for (KeplerLSID st : content.getSemanticTypes()) {
					    
			NamedOntClass noc = oc.getNamedOntClass(st.toString());
			if (noc != null) {

				// First determine if the NamedOntClass belongs to
				// an Ontology that is supposed to be showing up
				// in the library.
				boolean includeInOntologies = false;
				Iterator<NamedOntModel> libraryModels = oc
						.getLibraryNamedOntModels();
				while (libraryModels.hasNext()) {
					if (libraryModels.next().equals(noc.getModel())) {
						includeInOntologies = true;
						break;
					}
				}
				// If this NamedOntClass belongs to an Ontology Model
				// that is configured to be included in the library
				// then add it, otherwise just skip it
				if (includeInOntologies) {

					// Recurse up the tree
					Vector<LibItem> parents = assureOntClass(noc);
					for (LibItem parent : parents) {

						CacheContent cc = content.getCacheContent();
						String actorName = cc.getName();
						
						LibItem li = new LibItem();
						li.setName(actorName);
						li.setParent(parent.getLiid());
						KeplerLSID lsid = content.getLsid();
						if (lsid != null) {
							li.setLsid(lsid);
						}

						// Figure out what LibIndex type this KARCacheContent is
						int t = determineLibIndexType(content);
						if (t > 0) {
							li.setType(t);
						} else {
							log
									.error("Could not determine the LibIndex type for KARCacheContent");
							continue;
						}

						li.setLevel(parent.getLevel() + 1);

						String className = content.getCacheContent()
								.getClassName();
						if (className != null) {
							li.addAttribute(ATT_CLASSNAME, className);
							transferAttributes(li, content);
						}
						try {
							
							// See if this LibItem will collide with an existing LibItem
							int liidOfExistingChild = childExists(li.getParent(),li.getName());
							if (liidOfExistingChild > -1) {
								// A LIID with this name and parent already exists
								// just add the LSID to the existing LIID item
								insertLiidLsid(liidOfExistingChild, li.getLsid());
							} else {
								// create a new item in the library
								insertNoOrder(li);
								items.add(li);
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return items;
	}

	/**
	 * Count how many rows there are in the LIBRARY_INDEX table.
	 * 
	 * @return
	 */
	public int countItems() {
		int count = 0;
		try {
			ResultSet rs = null;
			try {
				rs = _getNumRowsInLibraryIndexPrepStmt.executeQuery();
				if (rs == null)
					return count;
				if (rs.next()) {
					count = rs.getInt(1);
					if (rs.next()) {
						// should never happen
						throw new SQLException(
								"Multiple rows found in countItems()");
					}
				}
			} finally {
				if(rs != null) {
					rs.close();
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		return count;

	}

	/**
	 * Returns a Vector of LibItems that are the parents of the given LibItem.
	 * 
	 * @param li
	 * @return Vector<LibItem> representing all parents of the given LibItem
	 * @throws SQLException
	 */
	public Vector<LibItem> getPath(LibItem li) throws SQLException {
		Vector<LibItem> pathItems = new Vector<LibItem>();

		_getLIIDOfParentsPrepStmt.setInt(1, li.getLeft());
		_getLIIDOfParentsPrepStmt.setInt(2, li.getRight());
		if (isDebugging)
			log.debug(_getLIIDOfParentsPrepStmt);
		ResultSet rs = null;
		try {
			rs = _getLIIDOfParentsPrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getLIIDOfParentsPrepStmt);
			while (rs.next()) {
				int liid = rs.getInt(1);
				LibItem parentItem = _libraryManager.getPopulatedLibItem(liid);
				pathItems.add(parentItem);
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		pathItems.add(li);
		return pathItems;
	}

	/**
	 * Get all of the items.
	 * 
	 * @return
	 */
	public Vector<LibItem> getItems() {
		return getItemsOfType(-1, -1);
	}

	/**
	 * Get all items of the given type.
	 * 
	 * @param type
	 * @return
	 */
	public Vector<LibItem> getItemsOfType(int type) {
		return getItemsOfType(type, -1);
	}

	/**
	 * Return items of the given type that are somewhere under the given root.
	 * 
	 * @param type
	 * @param root
	 * @return
	 */
	public Vector<LibItem> getItemsOfType(int type, int root) {
		Vector<LibItem> items = new Vector<LibItem>(countItems());

		// TODO: actually select all the values instead of just the liid
		String query = "SELECT LIID " + " FROM " + LIBRARY_INDEX_TABLE_NAME;

		if (type <= 0 && root <= 0) {
			// get everything
		} else {
			if (root > 0) {
				// get items under the specified root node
				try {
					query += " WHERE " + getRange(root);
				} catch (SQLException sqle) {
					log.warn(root + " library item not found");
					return items;
				}
				if (type > 0) {
					query += " AND TYPE = " + type;
				}
			} else {
				// root <= 0
				if (type > 0) {
					// get everything with the specified type
					query += " WHERE TYPE = " + type;
				}
			}
		}

		query += " ORDER BY LFT";

		try {
			if (isDebugging)
				log.debug(query);
			ResultSet rs = null;
			try {
				rs = _stmt.executeQuery(query);
				if (rs == null)
					throw new SQLException("Query Failed: " + query);
				while (rs.next()) {
					int liid = rs.getInt(1);
					// Doing it this way is kinda slow, but works for now
	                LibItem li = _libraryManager.getPopulatedLibItem(liid);
					items.add(li);
				}
			} finally {
				if(rs != null) {
					rs.close();
				}
			}
		} catch (SQLException sqle) {
			log.warn(root + " library item not found");
			return items;
		}
		if (isDebugging)
			log.debug(items.size() + " library items found");
		return items;
	}

	/**
	 * Convenience method for building the SQL string for queries needing the
	 * range between an LIIDs LFT and RGT preorder values.
	 * 
	 * @param liid
	 * @return
	 * @throws SQLException
	 */
	private String getRange(int liid) throws SQLException {
		String rangeString = new String();
		_getRangeForLIIDPrepStmt.setInt(1, liid);
		ResultSet rs = null;
		try {
			rs = _getRangeForLIIDPrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getRangeForLIIDPrepStmt);
			if (rs.next()) {
				int l = rs.getInt(1);
				int r = rs.getInt(2);
				rangeString += " LFT >= " + l + " AND ";
				rangeString += " RGT <= " + r;
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		return rangeString;

	}
	
	/**
	 * Take a LibItem and check to see if there is already an entry
	 * in the sql table for it depending on the type,name,and parent.
	 * If there is already an item in the table, return the LIID for
	 * the existing item.
	 * 
	 * @param li
	 * @return long LIID of existing item or -1 if no duplicate is found
	 */
	public int checkIfDuplicate(LibItem li) throws SQLException {
		int liid = -1;

		_getLIIDForParentAndNamePrepStmt.setInt(1, li.getParent());
		_getLIIDForParentAndNamePrepStmt.setString(2, li.getName());
		ResultSet rs = null;
		try {
			rs = _getLIIDForParentAndNamePrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query failed: " + _getLIIDForParentAndNamePrepStmt);
			if (rs.next()) {
				liid = rs.getInt(1);
				if (rs.wasNull()) {
					// bad news.
					liid = -1;
				}
				if (rs.next()) {
					// also bad news
				}
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		return liid;
	}
	
	/**
	 * Insert this LibItem into the table, make sure it doesn't already
	 * exist.  If it does try to add the LSID to the LIBRARY_LSIDS table.
	 * 
	 * @param li
	 * @throws SQLException
	 */
	private void insert(LibItem li) throws SQLException {
		if (isDebugging)
			log.debug("insert(" + li.getName() + ")");
		
		// Make sure there isn't an existing entry for this item
		int liidOfExistingChild = childExists(li.getParent(), li.getName());
		if (liidOfExistingChild > -1) {
			try {
				// Add the lsid to the Library_lsids table if the item already exists
				insertLiidLsid(liidOfExistingChild, li.getLsid());
			} catch (SQLException sqle) {
				throw new SQLException("Unable to insert LIID for existing child.");
			}
		} else {
		    /**
		     * Insert a new row for this LibItem under the parent liid or at level 1 if
		     * parent is null.
		     *
		     */
	        String insert = "INSERT INTO " + LibIndex.LIBRARY_INDEX_TABLE_NAME
	                + " (PARENT,LFT,RGT,LEVEL,LSID,TYPE,NAME) values (";

	        int index = getAlphabeticInsertIndex(li);
	        if (index < 0)
	            return;

	        li.setLeft(index);
	        li.setRight(index + 1);

	        if (li.getParent() == null) {
	            insert += "NULL"; // parent
	        } else {
	            insert += "" + li.getParent(); // parent
	        }

	        insert += "," + li.getLeft(); // left
	        insert += "," + li.getRight(); // right
	        insert += "," + li.getLevel(); // level

	        if (li.getLsid() != null) {
	            insert += ",'" + li.getLsid().toString() + "'"; // lsid
	        } else {
	            insert += ",NULL";
	        }
	        insert += "," + li.getType(); // type
	        insert += ",'" + li.getName() + "'"; // name
	        insert += ")";

	        String updateLeft = "UPDATE " + LibIndex.LIBRARY_INDEX_TABLE_NAME
	                + " SET LFT = LFT + 2 WHERE LFT >= " + li.getLeft();
	        String updateRight = "UPDATE " + LibIndex.LIBRARY_INDEX_TABLE_NAME
	                + " SET RGT = RGT + 2 WHERE RGT >= " + li.getLeft();

	        if (isDebugging) {
	            log.debug("\n" + updateLeft + "\n" + updateRight + "\n" + insert);
	        }

	        _stmt.executeUpdate(updateLeft);
	        _stmt.executeUpdate(updateRight);
	        _stmt.executeUpdate(insert);

	        // find out the auto assigned liid
	        String queryNewLiid = "CALL IDENTITY();";
	        ResultSet rs = null;
	        try {
	        	rs = _stmt.executeQuery(queryNewLiid);
		        if (rs != null && rs.next()) {
		            int newLiid = rs.getInt(1);
		            if (!rs.wasNull()) {
		                li.setLiid(newLiid);
		                if (isDebugging)
		                    log.debug("IDENTITY: " + li.getLiid());
		            } else {
		                log.error("Failed to retrieve auto assigned identity");
		            }
		        }
	        } finally {
	        	if(rs != null) {
	        		rs.close();
	        	}
	        }

	        // add the lsid to the LiidLsid table
	        insertLiidLsid(li.getLiid(), li.getLsid());
	        insertAttributes(li);
	        
	        LibraryManager.getInstance().getIndex().addToSearchIndex(li);

	        _stmt.getConnection().commit();
		}
	}
	
	private void insertAttributes(LibItem li) throws SQLException {
        
	    final int liid = li.getLiid();
	    
	    _deleteLIIDFromLibraryAttributesPrepStmt.setInt(1, liid);
	    
        if (isDebugging) log.debug(_deleteLIIDFromLibraryAttributesPrepStmt);
        _deleteLIIDFromLibraryAttributesPrepStmt.executeUpdate();
        _stmt.getConnection().commit();

        for(Entry<String, String> entry : li.getAttributes().entrySet()) {
            final String attName = entry.getKey();
            final String attValue = entry.getValue();
            _insertIntoLibraryAttributesPrepStmt.setInt(1,liid);
            _insertIntoLibraryAttributesPrepStmt.setString(2, attName);
            _insertIntoLibraryAttributesPrepStmt.setString(3, attValue);
            if (isDebugging) log.debug(_insertIntoLibraryAttributesPrepStmt);
            _insertIntoLibraryAttributesPrepStmt.executeUpdate();
        }
        _stmt.getConnection().commit();
    }
	
	/**
     * Figure out what the LEFT integer should be for a new row that is to be
     * inserted in alphabetical order for the current getParent() value. Or for
     * Level 1 if getParent() == null
     * 
     * @param stmt
     * @return
     * @throws SQLException
     */
    private int getAlphabeticInsertIndex(LibItem li) throws SQLException {
        int insertIndex = -1;

        int parentLevel = 0;
        String query = "select LIID,RGT,LEVEL,LSID,NAME from "
                + LibIndex.LIBRARY_INDEX_TABLE_NAME;
        if (li.getParent() == null) {
            query += " where LEVEL = 1";
        } else {
            query += " where PARENT = " + li.getParent();
        }
        query += " order by NAME ";
        if (isDebugging) {
            log.debug(query);
        }
        ResultSet rs = null;
        int cnt = 0;
        try {
        	rs = _stmt.executeQuery(query);
	        if (rs == null)
	            log.error("Query Failed: " + query);
	        int prevRight = -1;
	        while (rs.next()) {
	            int liid = rs.getInt(1); // LIID
	            int r = rs.getInt(2); // RGT
	            parentLevel = rs.getInt(3) - 1; // LEVEL
	            String lsid = rs.getString(4); // LSID
	            String n = rs.getString(5); // NAME
	
	            int comparison = n.compareToIgnoreCase(li.getName());
	            if (comparison == 0) {
	                log.debug(lsid);
	                log.debug(li.getLsid());
	                if (lsid.equals(li.getLsid())) {
	                    log.debug("LSID matches");
	                    throw new SQLException(li.getName()
	                            + " already exists as child of parent "
	                            + li.getParent());
	                } else {
	                    log.debug("LSID does not match");
	                    try {
	                        // Add the lsid to the Library_lsids table
	                        insertLiidLsid(liid, li.getLsid());
	                        return -1;
	                    } catch (SQLException sqle) {
	                        throw new SQLException("bummer");
	                    }
	                }
	            }
	            if (comparison > 0) {
	                // we want to insert before this item
	                if (cnt == 0) {
	                    // this is the first child
	                    // use the parents' left index
	                    break;
	                } else {
	                    // use the right index of the previous row plus 1
	                    insertIndex = prevRight + 1;
	                }
	            }
	            if (comparison < 0) {
	                // Go on to the next child, unless there is no next child
	                // for that case we'll set this to be the current right plus 1
	                // every pass through the loop
	                insertIndex = r + 1;
	            }
	            prevRight = r;
	            cnt++;
	        }
        } finally {
        	if(rs != null) {
                rs.close();
        	}
        }
        
        if (cnt == 0) {
            if (isDebugging)
                log.debug("No children found for parent " + li.getParent());
            // Or we're inserting at the top of the list
            if (li.getParent() == null) {
                // we're inserting at the very beginning
                insertIndex = 0;
            } else {
                String parentQuery = "SELECT LFT,LEVEL from "
                        + LibIndex.LIBRARY_INDEX_TABLE_NAME + " WHERE LIID = "
                        + li.getParent();
                if (isDebugging) {
                    log.debug(parentQuery);
                }
                ResultSet parentResult = null;
                try {
                	parentResult = _stmt.executeQuery(parentQuery);
	                if (parentResult == null)
	                    log.error("Query Failed: " + parentQuery);
	                if (parentResult.next()) {
	                    insertIndex = parentResult.getInt(1) + 1;
	                    parentLevel = parentResult.getInt(2);
	                }
                } finally {
                	if(parentResult != null) {
                		parentResult.close();
                	}
                }
            }
        }
        if (isDebugging)
            log.debug("return: " + insertIndex);
        li.setLevel(parentLevel + 1);
        return insertIndex;
    }


	/**
	 * Insert a LibItem row but do not update the lft and rgt ordering columns.
	 * This method is useful only for doing bulk inserts of many rows at a time.
	 * Then after a bunch of inserts the ordering must be updated by calling the
	 * refreshPreorderValues() method. For only inserting one row and having the
	 * ordering updated use the LibItem.insert() method.
	 * 
	 * You can setOrderedInsert(true) to force insert by insert ordering in this
	 * method.
	 * 
	 * @param li
	 */
	private void insertNoOrder(LibItem li) throws SQLException {
		if (isDebugging)
			log.debug("insertNoOrder(" + li.getName() + ")");

		if (isOrderedInsert()) {
			if (isDebugging)
				log.debug("Do Ordered Insert");
			insert(li);
			return;
		}

		// else we insert without updating the preorder values
		if (isDebugging)
			log.debug("Do Unordered Insert");

		String insert = "INSERT INTO " + LibIndex.LIBRARY_INDEX_TABLE_NAME
				+ " (PARENT,LFT,RGT,LEVEL,LSID,TYPE,NAME) values (";

		if (li.getParent() == null) {
			insert += "NULL"; // parent
		} else {
			insert += "" + li.getParent(); // parent
		}

		insert += "," + li.getLeft(); // left
		insert += "," + li.getRight(); // right
		insert += "," + li.getLevel(); // level

		if (li.getLsid() != null) {
			insert += ",'" + li.getLsid().toString() + "'"; // lsid
		} else {
			insert += ",NULL";
		}
		insert += "," + li.getType(); // type
		insert += ",'" + li.getName() + "'"; // name
		insert += ")";

		if (isDebugging)
			log.debug(insert);

		boolean itemInserted = false;
		try {
			int rows = _stmt.executeUpdate(insert);
			if (rows == 1) {
				itemInserted = true;
			} else {
				log.error("item was not inserted");
			}
		} catch (SQLException sqle) {
			if (isDebugging) {
				log.debug("ERROR CODE: " + sqle.getErrorCode());
				log.debug("ERROR MESSAGE: " + sqle.getMessage());
			}
			if (sqle.getErrorCode() == -104) {
				// This Name already exists under this parent
				if (li.getType() == LibIndex.TYPE_COMPONENT
						|| li.getType() == LibIndex.TYPE_NAMED_OBJ) {

					// Add it to the Library_LSIDS table
					KeplerLSID lsid = li.getLsid();
					if (lsid != null) {
						
						int liid = checkIfDuplicate(li);
						if (liid < 0) {
							// not a duplicate
						} else {
							insertLiidLsid(liid, li.getLsid());
						}

						return;

					} else {
						if (isDebugging)
							log.debug("lsid is null");
						throw sqle;
					}
				} else {
					if (isDebugging)
						log.debug("type is " + li.getType());
					throw sqle;
				}
			} else {
				if (isDebugging)
					log.debug("different error code");
				throw sqle;
			}
		}

		if (itemInserted) {
			// find out the auto assigned liid
			String queryNewLiid = "CALL IDENTITY();";
			ResultSet rs = null;
			try {
				rs = _stmt.executeQuery(queryNewLiid);
				if (rs == null)
					throw new SQLException("Query Failed: " + queryNewLiid);
				if (rs.next()) {
					int newLiid = rs.getInt(1);
					if (!rs.wasNull()) {
						li.setLiid(newLiid);
						if (isDebugging)
							log.debug("IDENTITY: " + li.getLiid());
					} else {
						log.error("Failed to retrieve auto assigned identity");
					}
				}
			} finally {
				if(rs != null) {
					rs.close();
				}
			}

			insertLiidLsid(li.getLiid(), li.getLsid());
			insertAttributes(li);

			addToSearchIndex(li);
		}

		_stmt.getConnection().commit();
	}

	   /**
     * Add a new LSID to the LIBRARY_LSIDS table for the specified LIID.
     * 
     * @param liid
     * @param lsid
     * @throws SQLException
     */
    private void insertLiidLsid(int liid, KeplerLSID lsid)
            throws SQLException {
        if (lsid == null) {
            if (isDebugging)
                log.debug("lsid is null, skip insert into LIBRARY_LSIDS");
            return;
        }

        _getNumLIIDForLIIDAndLSIDPrepStmt.setInt(1, liid);
        _getNumLIIDForLIIDAndLSIDPrepStmt.setString(2, lsid.toString());        
        if (isDebugging) log.debug(_getNumLIIDForLIIDAndLSIDPrepStmt);
        ResultSet rs = null;
        try {
        	rs = _getNumLIIDForLIIDAndLSIDPrepStmt.executeQuery();
	        if (rs.next()) {
	            int cnt = rs.getInt(1);
	            if (cnt <= 0) {
	                _insertIntoLibraryLSIDsPrepStmt.setInt(1, liid);
	                _insertIntoLibraryLSIDsPrepStmt.setString(2, lsid.toString());
	                if (isDebugging) log.debug(_insertIntoLibraryLSIDsPrepStmt);
	                _insertIntoLibraryLSIDsPrepStmt.executeUpdate();
	            } else {
	                // Already in there. ignore
	            }
	        }
        } finally {
        	if(rs != null) {
        		rs.close();
        	}
        }
        _stmt.getConnection().commit();
    }

	/**
	 * Add the given LibItem to the search index. If the OrderedInsert flag is
	 * set to true then all of the parents of this LibItem will be add to the
	 * search index for this item.
	 * 
	 * @param li
	 * @throws SQLException
	 */
	public void addToSearchIndex(LibItem li) throws SQLException {

		// Add a row for this LibItem
		Integer searchType = _searchTypeMap.get(li.getType());
		if (searchType != null) {
			_searcher.insertRow(searchType.intValue(), li.getLiid(), li
					.getName());
		}

		// We can only get the path to this LibItem if it has been inserted AND
		// ordered
		if (isOrderedInsert()) {
			addAllParentsToSearchIndex(li);
		}
	}

	/**
	 * Add all of the parent LibItems to the search index for the given LibItem.
	 * 
	 * @param li
	 * @throws SQLException
	 */
	private void addAllParentsToSearchIndex(LibItem li) throws SQLException {
		// Add a row for every LibItem in the path to this LibItem
		Vector<LibItem> parents = getPath(li);
		for (LibItem parent : parents) {
			Integer searchType = _searchTypeMap.get(parent.getType());
			if (searchType != null) {
				_searcher.insertRow(searchType.intValue(), li.getLiid(), parent
						.getName());
			}
		}

	}

	/**
	 * Here we map the LibItem types to the different Search types.
	 */
	private void initSearchMap() {
		_searchTypeMap = new Hashtable<Integer, Integer>();
		_searchTypeMap.put(TYPE_COMPONENT, LibSearch.TYPE_NAME);
		_searchTypeMap.put(TYPE_CONCEPT, LibSearch.TYPE_ONTCLASSNAME);
		_searchTypeMap.put(TYPE_NAMED_OBJ, LibSearch.TYPE_NAME);
		_searchTypeMap.put(TYPE_ONTOLOGY, LibSearch.TYPE_ONTOLOGY);
		_searchTypeMap.put(TYPE_FOLDER, LibSearch.TYPE_FOLDERNAME);
		_searchTypeMap.put(TYPE_KARFOLDER, LibSearch.TYPE_FOLDERNAME);
		_searchTypeMap.put(TYPE_KAR, LibSearch.TYPE_KARNAME);
		_searchTypeMap.put(TYPE_LOCALREPO, LibSearch.TYPE_LOCALREPO);
	}

	public LibSearch getSearcher() {
		return _searcher;
	}

	/**
	 * Sum the LFT and RGT columns and verify they are continuous integers from
	 * 1 to (countItems()*2). This function should always return true after
	 * finishing sql transactions for insert, update, or delete.
	 * 
	 * @return true if the LFT and RGT columns of the table add up
	 * @throws SQLException
	 */
	public boolean verifyPreorderValues() throws SQLException {
		if (isDebugging)
			log.debug(_getLftRgtSumPrepStmt);
		ResultSet rs = null;
		try {
			rs = _getLftRgtSumPrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getLftRgtSumPrepStmt);
			if (rs.next()) {
				int sumLR = rs.getInt(1);
				int cnt = countItems() * 2;
				int sumCnt = (cnt * (cnt + 1)) / 2;
				if (isDebugging)
					log.debug(sumLR + " " + sumCnt);
				if (sumLR == sumCnt) {
					return true;
				}
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		return false;
	}

	/**
	 * Refresh the lft and rgt PTT values based on the parent information and
	 * ordering by name.
	 */
	private void refreshPreorderValues() {
		try {
			int left = 1;
			ResultSet rs = null;
			try {
					rs = _getLIIDRootsPrepStmt.executeQuery();
				if (rs == null)
					throw new SQLException("Query Failed: " + _getLIIDRootsPrepStmt);
				while (rs.next()) {
					int liid = rs.getInt(1);
					left = refreshPreorderValues(liid, left);
				}
			} finally {
				if(rs != null) {
					rs.close();
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
	}

	/**
	 * Recursive function for refreshing the lft and rgt PTT values of a given
	 * parent LIID starting at the given LEFT value.
	 * 
	 * @param parent
	 * @param left
	 * @return
	 * @throws SQLException
	 */
	private int refreshPreorderValues(int parent, int left) throws SQLException {
		int right = left + 1;

		// get all children of this node
		_getLIIDForParentPrepStmt.setInt(1, parent);
		ResultSet rs = null;
		try {
			rs = _getLIIDForParentPrepStmt.executeQuery();
			while (rs.next()) {
				int p = rs.getInt(1);
				right = refreshPreorderValues(p, right);
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}

		// UPDATE LIBRARY_INDEX SET LFT=?, RGT=? WHERE LIID=?
		_updateOrderPrepStmt.clearParameters();
		_updateOrderPrepStmt.setInt(1, left);
		_updateOrderPrepStmt.setInt(2, right);
		_updateOrderPrepStmt.setInt(3, parent);
		_updateOrderPrepStmt.executeUpdate();

		// return the right value of this node + 1
		return right + 1;
	}

	/**
	 * Returns a string that uniquely identifies a KAR entry using the full path
	 * to the KAR file and the name of the entry as it appears in the KAR
	 * manifest.
	 * 
	 * @param karFile
	 * @param entry
	 * @return
	 */
	private String getKarEntryPath(File karFile, String entry) {
		String fullPath = karFile.toString();
		if (!fullPath.endsWith(File.separator)
				&& !entry.startsWith(File.separator)) {
			fullPath += File.separator + entry;
		} else {
			fullPath += entry;
		}
		return fullPath;
	}

	/**
	 * This will find any folder or file kar entry.
	 * 
	 * @param karFile
	 * @param path
	 * @return
	 */
	private LibItem findKarEntry(File karFile, String path) throws SQLException {
		LibItem li = null;

		_getLIIDForKarEntryPrepStmt.setString(1, getKarEntryPath(karFile, path));
		if (isDebugging)
			log.debug(_getLIIDForKarEntryPrepStmt);
		ResultSet rs = null;
		try {
			rs = _getLIIDForKarEntryPrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getLIIDForKarEntryPrepStmt);
			if (rs.next()) {
				int liid = rs.getInt(1);
				if (!rs.wasNull()) {
	                li = _libraryManager.getPopulatedLibItem(liid);
				}
				if (rs.next()) {
					log.error("LIBRARY_INDEX table is corrupt: "
							+ "Multiples of KAREntry " + path + " found");
				}
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		return li;
	}

	/**
	 * Given the KARCached object and the
	 * 
	 * @param kc
	 * @param path
	 * @return
	 */
	private LibItem assureKarFolder(KARCached kc, String path) {
		File karFile = kc.getFile();
		LibItem li = null;
		try {
			li = findKarEntry(karFile, path);
			if (li == null) {
				LibItem parent = null;
				String[] pathRep = path.split("/");
				if (pathRep.length > 1) {
					String parentPath = new String();
					for (int i = 0; i < pathRep.length - 1; i++) {
						parentPath += pathRep[i] + "/";
					}
					parent = assureKarFolder(kc, parentPath);
					if (parent == null) {
						throw new Exception("Unable to find or create "
								+ getKarEntryPath(karFile, parentPath));
					}
				} else {
					parent = assureKar(kc);
					if (parent == null) {
						throw new Exception("Unable to find or create "
								+ karFile);
					}
				}
				li = new LibItem();
				li.setName(pathRep[pathRep.length - 1]);
				li.setType(LibIndex.TYPE_KARFOLDER);
				li.setParent(parent.getLiid());
				li.setLevel(parent.getLevel() + 1);
				li.setLsid(null); // folders don't have LSIDs!
				li.addAttribute(ATT_KARENTRYPATH,
						getKarEntryPath(karFile, path));
				insertNoOrder(li);
			}
		} catch (Exception e) {
			log.error("Unable to assureKarFolder(" + karFile.toString() + ","
					+ path + ")");
			e.printStackTrace();
		}
		return li;
	}

	/**
	 * 
	 * @param kce
	 * @return
	 * @throws SQLException
	 */
	private LibItem findKarError(KARCacheError kce) throws SQLException {
		LibItem li = null;
		
		File karFile = kce.getFile();
		_getLIIDForKarErrorPrepStmt.setString(1, karFile.toString());
		if (isDebugging)
			log.debug(_getLIIDForKarErrorPrepStmt);
		ResultSet rs = null;
		try {
			rs = _getLIIDForKarErrorPrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getLIIDForKarErrorPrepStmt);
			if (rs.next()) {
				int liid = rs.getInt(1);
				if (rs.wasNull()) {
	                li = _libraryManager.getPopulatedLibItem(liid);
				}
				if (rs.next()) {
					log.error("LIBRARY_INDEX table is corrupt: "
							+ "multiples of KAR Error " + karFile + " found");
				}
				return li;
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		
		return li;
	}

	/**
	 * 
	 * @param kce
	 * @return
	 */
	private LibItem assureKarError(KARCacheError kce) {
		File karFile = kce.getFile();
		LibItem li = null;
		try {
			li = findKarError(kce);
			
			if (li == null) {
				LibItem parent = null;
				File parentFile = karFile.getParentFile();
				LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
				LocalRepository repo = lrm.getRepositoryForFile(parentFile);
				if (repo != null) {
					parent = assureLocalRepository(repo);
				} else {
					parent = assureFolder(parentFile);
				}
				if (parent == null)
					throw new Exception();
	
				li = new LibItem();
				li.setName(kce.getName());
				li.setType(LibIndex.TYPE_KAR_ERROR);
				li.setParent(parent.getLiid());
				li.setLevel(parent.getLevel() + 1);
				li.setLsid(kce.getLsid());
				li.addAttribute(ATT_KARFILE, karFile.toString());
				insertNoOrder(li);
			}

		} catch (Exception e) {
			log.error("Unable to assureKarError(" + kce.getFile().toString()
					+ ")");
			e.printStackTrace();
		}
		return li;
	}

	/**
	 * @param karFile
	 * @return LibItem that represents the given KAR File or null if the given
	 *         KAR File does not correspond to an entry in the Library Index.
	 * @throws SQLException
	 */
	public LibItem findKar(File karFile) throws SQLException {
		LibItem li = null;
	    _getLIIDForKarPrepStmt.setString(1, karFile.toString());
		if (isDebugging)
			log.debug(_getLIIDForKarPrepStmt);
		ResultSet rs = null;
		try {
			rs = _getLIIDForKarPrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getLIIDForKarPrepStmt);
			if (rs.next()) {
				int liid = rs.getInt(1);
				if (!rs.wasNull()) {
	                li = _libraryManager.getPopulatedLibItem(liid);
				}
				if (rs.next()) {
					log.error("LIBRARY_INDEX table is corrupt: "
							+ " multiples of KAR " + karFile + " found");
				}
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		return li;
	}

	/**
	 * 
	 * @param kc
	 * @return
	 */
	private LibItem assureKar(KARCached kc) {
		File karFile = kc.getFile();
		LibItem li = null;		
		try {
			li = findKar(karFile);
			if (li != null) {
				return li;
			}

			LibItem parent = null;
			File parentFile = karFile.getParentFile();
			LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
			LocalRepository repo = lrm.getRepositoryForFile(parentFile);
			if (repo != null) {
				parent = assureLocalRepository(repo);
			} else {
				parent = assureFolder(parentFile);
			}
			if (parent == null)
				throw new Exception();

			li = new LibItem();
			li.setName(kc.getName());
			li.setType(LibIndex.TYPE_KAR);
			li.setParent(parent.getLiid());
			li.setLevel(parent.getLevel() + 1);
			li.setLsid(kc.getLsid());
			li.addAttribute(ATT_KARFILE, karFile.toString());
			insertNoOrder(li);
			
			// see if this kar belongs to a demo
			ModuleTree tree = ModuleTree.instance();
			DotKeplerManager dkm = DotKeplerManager.getInstance();
			for(Module module : tree.getModuleList()) {
			    String moduleName = module.getName();
			    String moduleWorkflowsDir = dkm.getPersistentModuleWorkflowsDir(moduleName).getAbsolutePath();
			    String moduleDemosDir = moduleWorkflowsDir + File.separator + "demos";
			    
			    if(karFile.getAbsolutePath().startsWith(moduleDemosDir)) {
			        			        
		            parent = assureDemoFolder(parentFile);		            
		            li = new LibItem();
		            li.setName(kc.getName());
		            li.setType(TYPE_KAR);
		            li.setParent(parent.getLiid());
		            li.setLevel(parent.getLevel() + 1);
		            li.setLsid(kc.getLsid());
		            li.addAttribute(ATT_KARFILE, karFile.toString());
		            insertNoOrder(li);
			    }
			}

		} catch (Exception e) {
			log.error("Unable to assureKar(" + kc.getFile().toString() + ")");
			e.printStackTrace();
		}
		return li;
	}
	
	/**
     * @param file
     * @return LibItem that represents the given XML File or null if the given
     *         XML File does not correspond to an entry in the Library Index.
     * @throws SQLException
     */
    public LibItem findXML(File file) throws SQLException {
        LibItem li = null;
        _getLIIDForXMLPrepStmt.setString(1, file.toString());
        if (isDebugging)
            log.debug(_getLIIDForXMLPrepStmt);
        ResultSet rs = null;
        try {
        	rs = _getLIIDForXMLPrepStmt.executeQuery();
	        if (rs == null)
	            throw new SQLException("Query Failed: " + _getLIIDForXMLPrepStmt);
	        if (rs.next()) {
	            int liid = rs.getInt(1);
	            if (!rs.wasNull()) {
	                li = _libraryManager.getPopulatedLibItem(liid);
	            }
	            if (rs.next()) {
	                log.error("LIBRARY_INDEX table is corrupt: "
	                        + " multiples of XML " + file + " found");
	            }
	        }
        } finally {
        	if(rs != null) {
        		rs.close();
        	}
        }
        return li;
    }

	/**
     * 
     * @param kc
     * @return
     */
    public LibItem assureXML(File file) {
        LibItem li = null;      
        try {
            li = findXML(file);
            if (li != null) {
                return li;
            }

            LibItem parent = null;
            File parentFile = file.getParentFile();
            LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
            LocalRepository repo = lrm.getRepositoryForFile(parentFile);
            if (repo != null) {
                parent = assureLocalRepository(repo);
            } else {
                parent = assureFolder(parentFile);
            }
            if (parent == null)
                throw new Exception();
            
            KeplerActorMetadata metadata = null;
            KeplerLSID lsid = null;
            try {
                 metadata = KeplerMetadataExtractor.extractActorMetadata(file, false);
            } catch(Exception e) {
                System.err.println("Error parsing " + file + ": " + e.getMessage());
                return null;                
            }
            if(metadata == null) {
                return null;
            }
            
            String className = metadata.getClassName();
            
            li = new LibItem();
            li.setName(file.getName());
            li.setType(TYPE_COMPONENT);
            li.setParent(parent.getLiid());
            li.setLevel(parent.getLevel() + 1);
            li.setLsid(lsid);
            li.addAttribute(ATT_XMLFILE, file.toString());
            if(className != null) {
                li.addAttribute(ATT_CLASSNAME, className);
            }
            insertNoOrder(li);
            
            // see if this kar belongs to a demo
            ModuleTree tree = ModuleTree.instance();
            DotKeplerManager dkm = DotKeplerManager.getInstance();
            for(Module module : tree.getModuleList()) {
                String moduleName = module.getName();
                String moduleWorkflowsDir = dkm.getPersistentModuleWorkflowsDir(moduleName).getAbsolutePath();
                String moduleDemosDir = moduleWorkflowsDir + File.separator + "demos";
                
                if(file.getAbsolutePath().startsWith(moduleDemosDir)) {
                                        
                    parent = assureDemoFolder(parentFile);                  
                    li = new LibItem();
                    li.setName(file.getName());
                    li.setType(TYPE_COMPONENT);
                    li.setParent(parent.getLiid());
                    li.setLevel(parent.getLevel() + 1);
                    li.setLsid(lsid);
                    li.addAttribute(ATT_XMLFILE, file.toString());
                    if(className != null) {
                        li.addAttribute(ATT_CLASSNAME, className);
                    }
                    insertNoOrder(li);
                }
            }

        } catch (Exception e) {
            log.error("Unable to assureXML(" + file.toString() + ")");
            e.printStackTrace();
        }
        return li;
    }
    
	private LibItem findFolder(String folder) throws SQLException {
		LibItem li = null;
		_getLIIDForFolderPrepStmt.setString(1, folder);
		if (isDebugging)
			log.debug(_getLIIDForFolderPrepStmt);
		ResultSet rs = null;
		try {
			rs = _getLIIDForFolderPrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getLIIDForFolderPrepStmt);
			if (rs.next()) {
				int liid = rs.getInt(1);
				if (!rs.wasNull()) {
					li = _libraryManager.getPopulatedLibItem(liid);
				}
				if (rs.next()) {
					log.error("LIBRARY_INDEX table is corrupt: "
							+ "multiples of folder " + folder + " found");
				}
				return li;
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		return li;
	}
	
	private LibItem assureDemoFolder(File folder) {
	    LibItem li = null;
	    try {
	        
	        li = findFolder(_demosFolderItem + ":" + folder.toString());
	        if(li == null) {
	            
                if(_demosFolderItem == null) {
                    _demosFolderItem = new LibItem();
                    _demosFolderItem.setName("Demos");
                    _demosFolderItem.setType(TYPE_FOLDER);
                    _demosFolderItem.setParent(null);
                    _demosFolderItem.setLevel(ROOT_LEVEL);
                    _demosFolderItem.setLsid(null);
                    insertNoOrder(_demosFolderItem);					
                }

    	        LibItem parent = null;
    	        
    	        String path = folder.getAbsolutePath();
    	        Matcher matcher = _demosFolderPattern.matcher(path);
    	        
    	        if(path.equals(DotKeplerManager.getInstance()
                            .getPersistentModuleWorkflowsDir().getPath())) {
                    return _demosFolderItem;
    	        } else if (matcher.find() && path.endsWith("demos")) {
                    if(matcher.group(1).startsWith("outreach")) {
                        return _demosFolderItem;
                    } else {
                        return assureDemoFolder(folder.getParentFile());
                    }
    	        } else {
    	            parent = assureDemoFolder(folder.getParentFile());
    	        }
    	        
    	        // remove the version from the name, if present.
    	        final String unversionedName = Version.stem(folder.getName());
    	        final String name = LocalRepositoryManager.getLocalRepositoryName(unversionedName);
    	        
                li = new LibItem();
                li.setName(name);
                li.setType(LibIndex.TYPE_FOLDER);
                li.setParent(parent.getLiid());
                li.setLevel(parent.getLevel() + 1);
                li.setLsid(null); // folders don't have LSIDs!
                li.addAttribute(ATT_FOLDER, _demosFolderItem + ":" + folder.toString());
                insertNoOrder(li);
	        }	        
	    } catch (Exception e) {
            log.error("Unable to assureFolder(" + folder.toString() + ") rooted at " + _demosFolderItem.getName());
            e.printStackTrace();
	    }
	    return li;
	}
	
	/**
	 * 
	 * @param folder
	 * @return
	 */
	private LibItem assureFolder(File folder) {
		LibItem li = null;
		try {
			li = findFolder(folder.toString());

			if (li == null) {
				// determine the parent LibItem
				LibItem parent = null;

				LocalRepositoryManager lrm = LocalRepositoryManager
						.getInstance();
				LocalRepository repo = lrm.getRepositoryForFile(folder);
				if (repo != null) {
					if (isDebugging)
						log.debug(folder + " is a repository");
					parent = assureLocalRepository(repo);
				} else {
				    repo = lrm.getContainingLocalRepository(folder);
					if (repo == null) {
						throw new Exception(folder
								+ " is not in a local repository");
					}
					File parentFile = folder.getParentFile();
					if (isDebugging)
						log.debug(repo + " - " + parentFile);
					if (repo.isFileRepoDirectory(parentFile)) {
						parent = assureLocalRepository(repo);
					} else {
						parent = assureFolder(parentFile);
					}

					// insert a new LibItem for this folder
					li = new LibItem();
					li.setName(folder.getName());
					li.setType(LibIndex.TYPE_FOLDER);
					li.setParent(parent.getLiid());
					li.setLevel(parent.getLevel() + 1);
					li.setLsid(null); // folders don't have LSIDs!
					li.addAttribute(ATT_FOLDER, folder.toString());
					insertNoOrder(li);
				}
			}
		} catch (Exception e) {
			log.error("Unable to assureFolder(" + folder.toString() + ")");
			e.printStackTrace();
		}
		return li;
	}

	private LibItem findLocalRepository(LocalRepository repo) throws SQLException {
		LibItem li = null;
		_getLIIDForRepositoryPrepStmt.setString(1, repo.toString());
		if (isDebugging)
			log.debug(_getLIIDForRepositoryPrepStmt);
		ResultSet rs = null;
		try {
			rs = _getLIIDForRepositoryPrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getLIIDForRepositoryPrepStmt);
			if (rs.next()) {
				int liid = rs.getInt(1);
				if (!rs.wasNull()) {
	                li = _libraryManager.getPopulatedLibItem(liid);
				}
				if (rs.next()) {
					log.error("LIBRARY_INDEX table is corrupt: "
							+ " multiples of local repository " + repo + " found");
				}
				return li;
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		return li;
	}

	/**
	 * 
	 * @param repo
	 * @return
	 */
	private LibItem assureLocalRepository(LocalRepository repo) {
		LibItem li = null;

		try {
			li = findLocalRepository(repo);

			if (li == null) {
				// insert a new LibItem row for this Local Repository
				LocalRepositoryManager lrm = LocalRepositoryManager
						.getInstance();
				String repoName = lrm.getLocalRepositories().get(repo);

				li = new LibItem();
				li.setName(repoName);
				li.setType(LibIndex.TYPE_LOCALREPO);
				li.setParent(null);
				li.setLevel(ROOT_LEVEL);
				li.setLsid(null); // repos don't have LSIDs!
				li.addAttribute(ATT_REPOPATH, repo.toString());
				insertNoOrder(li);
			}
		} catch (Exception e) {
			log.error("Unable to assureLocalRepository(" + repo.toString()
					+ ")");
			e.printStackTrace();
		}
		return li;
	}

	/**
	 * 
	 * @param ontologyName
	 * @return
	 * @throws SQLException
	 */
	private LibItem findOntology(String ontologyName) throws SQLException {
		LibItem li = null;
		_getLIIDForOntologyNamePrepStmt.setString(1, ontologyName);
		if (isDebugging)
			log.debug(_getLIIDForOntologyNamePrepStmt);
		ResultSet rs = null;
		try {
			rs = _getLIIDForOntologyNamePrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getLIIDForOntologyNamePrepStmt);
			if (rs.next()) {
				int liid = rs.getInt(1);
				if (isDebugging)
					log.debug(liid + " is already in the index table");
				if (!rs.wasNull()) {
	                li = _libraryManager.getPopulatedLibItem(liid);
				}
				if (rs.next()) {
					log.error("LIBRARY_INDEX table is corrupt: "
							+ "multiples of ontology " + ontologyName + " found");
				}
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		return li;
	}

	/**
	 * Return the LibItem for the given Ontology Name. If no LibItem exists in
	 * the LIBRARY_INDEX then insert a new row for it and return a LibItem
	 * representation of the row.
	 * 
	 * @param ontologyName
	 * @return
	 */
	private LibItem assureOntology(String ontologyName) {
		LibItem li = null;

		try {
			li = findOntology(ontologyName);

			if (li == null) {
				if (isDebugging)
					log.debug(ontologyName + " is not in the index table");
				li = new LibItem();
				li.setName(ontologyName);
				li.setType(LibIndex.TYPE_ONTOLOGY);
				li.setParent(null);
				li.setLevel(1);
				insertNoOrder(li);
			}
		} catch (Exception e) {
			log.error("Unable to assureOntology(" + ontologyName + ")");
			e.printStackTrace();
		}
		return li;
	}

	/**
	 * 
	 * @param noc
	 * @return
	 * @throws SQLException
	 */
	private Vector<LibItem> findOntClass(NamedOntClass noc) throws SQLException {
		Vector<LibItem> items = new Vector<LibItem>();
		_getLIIDForOntologyClassPrepStmt.setString(1, noc.getConceptId());
		if (isDebugging)
			log.debug(_getLIIDForOntologyClassPrepStmt);
		ResultSet rs = null;
		try { 
			rs = _getLIIDForOntologyClassPrepStmt.executeQuery();
			if (rs == null)
				log.error("Query Failed: " + _getLIIDForOntologyClassPrepStmt);
			while (rs.next()) {
				int liid = rs.getInt(1);
				if (isDebugging)
					log.debug(liid + " is already in the index table");
				if (!rs.wasNull()) {
	                LibItem li = _libraryManager.getPopulatedLibItem(liid);
					items.add(li);
				}
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
		return items;
	}

	/**
	 * Return a Vector of LibItem objects from the Library_Index that correlate
	 * to the given NamedOntClass. If none exist then insert.
	 * 
	 * TODO: there is likely a serious flaw here, check into it...
	 * 
	 * @param noc
	 * @return
	 */
	private Vector<LibItem> assureOntClass(NamedOntClass noc) {
		if (isDebugging)
			log.debug(noc.getName());
		Vector<LibItem> items = null;
		try {

			items = findOntClass(noc);

			if (items.size() <= 0) {
				if (isDebugging)
					log.debug("needs to be added to the index table");
				Iterator<NamedOntClass> parents = noc
						.getNamedSuperClasses(false);
				while (parents.hasNext()) {
					NamedOntClass parent = parents.next();
					Vector<LibItem> parentItems = assureOntClass(parent);
					for (LibItem parentItem : parentItems) {
						LibItem newli = new LibItem();
						newli.setName(noc.getName());
						newli.setLsid(new KeplerLSID(noc.getConceptId()));
						newli.setParent(parentItem.getLiid());
						newli.setType(LibIndex.TYPE_CONCEPT);
						newli.setLevel(parentItem.getLevel() + 1);
						insertNoOrder(newli);
						items.add(newli);
					}
				}
				if (items.size() <= 0) {
					if (isDebugging)
						log.debug("no super classes, add the ontology");
					LibItem parentOnt = assureOntology(noc.getOntologyName());

					LibItem newli = new LibItem();
					newli.setName(noc.getName());
					newli.setLsid(new KeplerLSID(noc.getConceptId()));
					newli.setParent(parentOnt.getLiid());
					newli.setType(LibIndex.TYPE_CONCEPT);
					newli.setLevel(parentOnt.getLevel() + 1);
					insertNoOrder(newli);
					items.add(newli);
				}
			}
		} catch (Exception e) {
			log.error("Unable to assureOntClass(" + noc.toString() + ")");
			e.printStackTrace();
		}
		return items;
	}
	
	/**
	 * Return true if there is a child of the parent in the index with the given
	 * name.
	 * 
	 * @param parentLiid
	 * @param childName
	 * @return
	 */
	public int childExists(Integer parentLiid, String childName) {
		int liidOfExistingChild = -1;
		try {
		    PreparedStatement query;
			if (parentLiid == null) {
				query = _getLIIDForNullParentAndNamePrepStmt;
				query.setString(1, childName);
			} else {
			    query = _getLIIDForParentAndNamePrepStmt;
			    query.setInt(1, parentLiid.intValue());
			    query.setString(2, childName);
			}
			
			if (isDebugging)
				log.debug(query);
			ResultSet rs = null;
			try {
				rs = query.executeQuery();
				if (rs == null)
					throw new SQLException("Query Failed: " + query);
				if (rs.next()) {
					liidOfExistingChild = rs.getInt(1);
				}
			} finally {
				if(rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return liidOfExistingChild;
	}

}
