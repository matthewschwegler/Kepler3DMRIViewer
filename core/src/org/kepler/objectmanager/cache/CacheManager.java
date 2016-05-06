/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-03-27 11:58:38 -0700 (Wed, 27 Mar 2013) $' 
 * '$Revision: 31776 $'
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.modules.ModuleTree;
import org.kepler.kar.KAREntryHandler;
import org.kepler.kar.KAREntryHandlerFactory;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.DotKeplerManager;
import org.kepler.util.sql.DatabaseFactory;

import ptolemy.actor.gui.Configuration;
import ptolemy.util.MessageHandler;

/**
 * This class represents a disk cache of CacheObjects. The cache manages cache
 * objects by calling their lifecycle event handlers and serializing every time
 * a change is made. Objects in the cache each have a unique lsid. Once an
 * object is in the cache, it will not be updated unless the lsid changes.
 * 
 * this class uses hsql to keep track of cache entries.
 */
public class CacheManager {

	private static final Log log = LogFactory.getLog(CacheManager.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private static CacheManager cache = null;

	private Hashtable<String, CacheObjectInterface> objectHash = new Hashtable<String, CacheObjectInterface>();

	private Vector<CacheListener> listeners = new Vector<CacheListener>();
	private Vector<KAREntryHandler> karEntryHandlers = new Vector<KAREntryHandler>();

	protected static final String cachePath = DotKeplerManager.getInstance()
			.getCacheDirString();
	/** The directory containing serialized java objects in the cache. */
    private static final String objectPath = cachePath + "objects"
            + File.separator + getDatabaseSchemaName();
	public static final String tmpPath = cachePath + "tmp";
	public static final String CACHETABLENAME = "cacheContentTable";
	public static final String CACHE_SEMTYPES_TABLE_NAME = "CACHE_SEMTYPES";

	private Connection _conn;
	private Statement _stmt;

	private PreparedStatement _cacheInsertStatement;
	private PreparedStatement _cacheSemTypesInsertStmt;
	private PreparedStatement _cacheUpdateStatement;
	private PreparedStatement _getLsidForLsidPrepStmt;
	private PreparedStatement _getSemTypeForLsidPrepStmt;
	private PreparedStatement _getLsidsForClassPrepStmt;

	/**
	 * Construct a new CacheManager
	 */
	protected CacheManager() throws CacheException {
		if (isDebugging) 
      log.debug("new CacheManager()");

		try {
			_conn = DatabaseFactory.getDBConnection();
			_stmt = _conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {			
		    if (isDebugging) log.debug(countDB() + " Items Cached.");
		    
			_cacheInsertStatement = _conn
					.prepareStatement("insert into "
							+ CACHETABLENAME
							+ " (name, lsid, date, file, type, classname) values ( ?, ?, ?, ?, ?, ? )");
			_cacheSemTypesInsertStmt = _conn.prepareStatement("insert into "
					+ CACHE_SEMTYPES_TABLE_NAME
					+ " (LSID,SEMTYPE) values ( ?, ?)");
			_cacheUpdateStatement = _conn.prepareStatement("update "
					+ CACHETABLENAME + " set name = ?," // 1
					+ " date = ?," // 2
					+ " file = ?," // 3
					+ " type = ? " // 4
					+ " where lsid = ?"); // 5
			
			_getLsidForLsidPrepStmt = _conn.prepareStatement("select LSID from "
			        + CACHETABLENAME + " where lsid = ?");

			_getSemTypeForLsidPrepStmt = _conn.prepareStatement("SELECT SEMTYPE FROM "
			        + CACHE_SEMTYPES_TABLE_NAME + " WHERE LSID = ?");

			_getLsidsForClassPrepStmt = _conn.prepareStatement("select LSID from "
			        + CACHETABLENAME + " where classname = ?");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// create the directory for the serialized java objects if it
		// does not exist
		File objectDir = new File(objectPath);
		if(!objectDir.exists() && !objectDir.mkdirs()) {
		    MessageHandler.error("Could not create directories " + objectDir);
		}

	}

	/**
	 * create a new singleton instance of CacheManager
	 */
	public static synchronized CacheManager getInstance() throws CacheException {
		if (cache == null) {
			cache = new CacheManager();
			cache.initKAREntryHandlers();
		}
		return cache;
	}
	
	/** Shutdown the cache manager. */
	public static void shutdown() {
	    
	    if(cache != null) {
	        
	        cache.objectHash.clear();
	        cache.listeners.clear();
	        cache.karEntryHandlers.clear();

	        try {
	            cache._stmt.close();
	            cache._stmt = null;
	            cache._conn.close();
	            cache._conn = null;
	        } catch(SQLException e) {
	            MessageHandler.error("Error closing database connection.", e);
	        }
	        
	        cache = null;
	    }
	}

	/**
	 * returns a temp file that is guaranteed to be around for one kepler
	 * session but could be deleted if the cache gets too full.
	 */
	public synchronized File getTempFile() {
		File f = new File(tmpPath);
		f.mkdirs();
		f = new File(tmpPath, "tmp" + System.currentTimeMillis());
		// TODO: register this file somewhere so it can get deleted later
		return f;
	}

	/**
	 * @param keh
	 */
	public void addKAREntryHandler(KAREntryHandler keh) {
		if (isDebugging)
			log.debug("addKAREntryHandler(" + keh.getClass().getName() + ")");
		karEntryHandlers.add(keh);
	}

	public Collection<KAREntryHandler> getKAREntryHandlers() {
		return karEntryHandlers;
	}

	/**
	 * Initialize all KAREntryHandlers that have been specified in
	 * configuration.
	 */
	private void initKAREntryHandlers() {
		if (isDebugging)
			log.debug("initKAREntryHandlers");

		List configsList = Configuration.configurations();
		Configuration config = null;
		for (Iterator it = configsList.iterator(); it.hasNext();) {
			config = (Configuration) it.next();
			if (config != null) {
				break;
			}
		}
		if (config != null) {
			// KAREntryHandlerFactory KEHFactory = (KAREntryHandlerFactory)
			// config
			// .getAttribute("karEntryHandlerFactory");
			try {
				KAREntryHandlerFactory KEHFactory = new KAREntryHandlerFactory(
						config, "KarEntryHandlerFactory");

				if (KEHFactory != null) {
					boolean success = KEHFactory.registerKAREntryHandlers();
					if (!success) {
						System.out
								.println("error: karEntryHandlerFactory is null.  "
										+ "This "
										+ "problem can be fixed by adding a karEntryHandlerFactory "
										+ "property in the configuration.xml file.");
					}
				} else {
					System.out
							.println("error: KAREntryHandlerFactory is null.  This "
									+ "problem can be fixed by adding a karEntryHandlerFactory "
									+ "property in the configuration.xml file.");
				}
			} catch (ptolemy.kernel.util.NameDuplicationException nde) {
				// do nothing. the property is already there.
				System.out.println("#######################");
			} catch (Exception e) {
				System.out.println("Could not create KarEntryHandlerFactory: "
						+ e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * insert a new CacheObjectInterface into the cache.
	 * 
	 * @param co
	 *            the cache object to insert
	 */
	public synchronized void insertObject(CacheObjectInterface co)
			throws CacheException {
		if (co == null)
			return;
		// get the critical info
		String name = co.getName();
		KeplerLSID klsid = co.getLSID();
		if (klsid == null) {
			log.warn("KAREntry has no lsid: " + name);
			return;
		}
		String lsid = klsid.toString();
		String date = String.valueOf(System.currentTimeMillis());
		String filename = co.getLSID().createFilename();
		if (isDebugging)
			log.debug(name + " " + lsid + " " + filename);
		if (isDebugging)
			log.debug(co.getClass().getName());

		// save the entry to the DB
		try {
			_cacheInsertStatement.clearParameters();
			_cacheInsertStatement.setString(1, name);
			_cacheInsertStatement.setString(2, lsid.toString());
			_cacheInsertStatement.setString(3, date);
			_cacheInsertStatement.setString(4, filename);
			_cacheInsertStatement.setString(5, co.getClass().getName());
			if (co instanceof ActorCacheObject) {
				String className = ((ActorCacheObject) co).getClassName();
				_cacheInsertStatement.setString(6, className);
			} else {
				_cacheInsertStatement.setNull(6, java.sql.Types.VARCHAR);
			}
			objectHash.put(lsid, co);
			serializeObjectInFile(co, filename);
			_cacheInsertStatement.executeUpdate();

			// insert the semantic types for this object
			if (co instanceof CacheObject) {
				Vector<String> semTypes = ((CacheObject) co).getSemanticTypes();
				for (String semType : semTypes) {
					_cacheSemTypesInsertStmt.clearParameters();
					_cacheSemTypesInsertStmt.setString(1, lsid.toString());
					_cacheSemTypesInsertStmt.setString(2, semType);
					_cacheSemTypesInsertStmt.executeUpdate();
				}
			}

			_conn.commit();
		} catch (Exception sqle) {
			log.error(sqle.getMessage());
			try {
				_conn.rollback();
			} catch (Exception e) {
				throw new CacheException(
						"Could not roll back the database after error "
								+ sqle.getMessage(), e);
			}
			// sqle.printStackTrace();
			log
					.error("Could not insert entry into cache: " + name + " "
							+ lsid);
			throw new CacheException(
					"Could not create hsql entry for new CacheObjectInterface: ",
					sqle);
		}
		notifyListeners(co, "add");
	}

	/**
	 * update a CacheObjectInterface in the cache.
	 */
	public synchronized void updateObject(CacheObjectInterface co)
			throws CacheException {
		// get the critical info
		String name = co.getName();
		String lsid = co.getLSID().toString();
		String date = String.valueOf(System.currentTimeMillis());
		String filename = co.getLSID().createFilename();
		String coType = co.getClass().getName();
		// save the entry to the DB
		try {
			_cacheUpdateStatement.setString(1, name);
			_cacheUpdateStatement.setString(2, date);
			_cacheUpdateStatement.setString(3, filename);
			_cacheUpdateStatement.setString(4, coType);
			_cacheUpdateStatement.setString(5, lsid.toString());
			_cacheUpdateStatement.executeUpdate();
			_cacheUpdateStatement.clearParameters();
			serializeObjectInFile(co, filename);
			_conn.commit();
		} catch (Exception sqle) {
			try {
				_conn.rollback();
			} catch (Exception e) {
				throw new CacheException(
						"Could not roll back the database after error "
								+ sqle.getMessage(), e);
			}
			throw new CacheException(
					"Could not create hsql entry for new CacheObjectInterface: ",
					sqle);
		}
		notifyListeners(co, "update");
	}

	/**
	 * remove the CacheObjectInterface with the specified lsid.
	 * 
	 * @param lsid
	 */
	public void removeObject(KeplerLSID lsid) throws CacheException {
		// grab a copy from the hash to use for calling listeners. There will be
		// no listeners on an object returned from the db.
		CacheObjectInterface co = (CacheObjectInterface) objectHash.get(lsid
				.toString());
		try {
			String sql = "SELECT file FROM " + CACHETABLENAME + " WHERE lsid='"
					+ lsid.toString() + "'";
			if (isDebugging)
				log.debug(sql);
			ResultSet rs = _stmt.executeQuery(sql);
			if (rs == null)
				throw new SQLException("Query Failed: " + sql);
			if (rs.next()) {
				File f = new File(objectPath, rs.getString("file"));
				if (isDebugging)
					log.debug(f.toString());
				if (f.exists()) {
					f.delete();
				}
			}
			rs.close();

			try {
				sql = "DELETE FROM " + CACHETABLENAME + " WHERE lsid='"
						+ lsid.toString() + "'";
				if (isDebugging) {
					// log.debug(showDB());
					log.debug(sql);
				}
				_stmt.execute(sql);
			} catch (Exception e) {
				log.error(lsid.toString() + " did not exist in "
						+ CACHETABLENAME);
				log.error(e.getMessage());
			}
			_conn.commit();
			objectHash.remove(lsid.toString());
		} catch (Exception e) {
			try {
				_conn.rollback();
			} catch (Exception e1) {
				throw new CacheException(
						"Could not roll back the database after error "
								+ e.getMessage(), e1);
			}
			throw new CacheException("Error removing object " + lsid
					+ " from the cache", e);
		}
		if (co != null) {
			notifyListeners(co, "remove");
		}
	}

	/**
	 * debug method to show the contents of the table
	 * 
	 * @throws CacheException, SQLException
	 */
	public String showDB() throws CacheException, SQLException {
		StringBuilder buf = new StringBuilder();
		PreparedStatement selectAll;
		// Create the prepared statement.
		try {
			// FIXME reuse this prepared statement
			selectAll = _conn
					.prepareStatement("select name, lsid, file, type from "
							+ CACHETABLENAME);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CacheException("Unable to create prepared statement.");
		}
		try {
			ResultSet rs = selectAll.executeQuery();
			while (rs.next()) {
				// name, lsid, file, type
				String name = rs.getString("name");
				String lsid = rs.getString("lsid");
				String file = rs.getString("file");
				String coType = rs.getString("type");
				buf.append("name: ");
				buf.append(name);
				buf.append("  lsid: ");
				buf.append(lsid);
				buf.append("  file: ");
				buf.append(file);
				buf.append("  type: ");
				buf.append(coType);
				buf.append("\n");
			}
		} catch (Exception e) {
			System.out.println("error showing db: " + e.getMessage());
		} finally{
			selectAll.close();
		}
		return buf.toString();
	}

	/**
	 * debug method to count the contents of the table * @throws CacheException
	 */
	public int countDB() throws CacheException, SQLException {
		PreparedStatement selectAll = null;
		// Create the prepared statements.
		try {
			selectAll = _conn.prepareStatement("select lsid from "
					+ CACHETABLENAME);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CacheException("Unable to create prepared statements.");
		}
		int cnt = 0;
		try {
			ResultSet rs = selectAll.executeQuery();
			while (rs.next()) {
				cnt++;
			}
		} catch (Exception e) {
			System.out.println("error counting db: " + e.getMessage());
		} finally{
			selectAll.close();
		}
		return cnt;

	}

	/**
	 * return the CacheObjectInterface with the specified lsid. Returns null if
	 * the object is not in the cache and cannot be resolved.
	 * 
	 * @param lsid
	 * @throws CacheException
	 *             when there is an issue with the cache.
	 */
	public CacheObjectInterface getObject(KeplerLSID lsid)
			throws CacheException {
		if (isDebugging)
			log.debug(lsid.toString());
		// first look in the hash table
		CacheObjectInterface co = (CacheObjectInterface) objectHash.get(lsid
				.toString());

		// Found it in the hash - return it.
		if (co != null) {
			/*
			 * using if (isDebugging) here is bad since it cause getObject to be called
			 * when it is not supposed to, thus invoking the momlparser
				log.debug("    was found in the hash");
				if (co.getObject() != null) {
					if (co.getObject() instanceof NamedObj) {
						log.debug("NamedObj: "
								+ ((NamedObj) co.getObject()).getName());
					} else {
						log.debug("Object: "
								+ co.getObject().getClass().getName());
					}
				} else {
					log
							.debug("the object contained by this cache object is NULL");
				}
			*/
			return co;
		}

		// Now look in the database:
		try {
			String query = "select name, lsid, file from " + CACHETABLENAME
					+ " where lsid='" + lsid.toString() + "'";
			ResultSet rs = null;
			try {
				rs = _stmt.executeQuery(query);
				if (rs == null)
					throw new SQLException("Query Failed: " + query);
				if (rs.next()) {
					// found it in the database.
					String name = rs.getString("name");
					String file = rs.getString("file");
					if (isDebugging)
						log.debug(name + " " + file);
					File theObjectFile = new File(objectPath, file);
					if (isDebugging)
						log.debug(theObjectFile.toString());
					FileInputStream fileInputStream = null;
					ObjectInputStream ois = null;
					try {
						fileInputStream = new FileInputStream(theObjectFile);
					    ois = new ObjectInputStream(fileInputStream);
	    				// deserialize the CacheObjectInterface
	    				co = (CacheObjectInterface) ois.readObject();
	    				co.setName(name);
	    				co.setLSID(lsid);
	    				// add the CacheObjectInterface to the hashtable for easier
	    				// access next time
	    				objectHash.put(lsid.toString(), co);
	    				return co;
					} finally {
					    if(ois != null) {
					        ois.close();
					    }
					    if(fileInputStream != null) {
					    	fileInputStream.close();
					    }
					}
				}
			} finally {
				if(rs != null) {
					rs.close();
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			throw new CacheException("SQL exception when getting object", sqle);
		} catch (Exception e) {
			throw new CacheException(
					"Exception occurred while deserializing object", e);
		}
		return co;
	}

	public Vector<CacheContent> getCachedContents() {
		return getCachedContents("");
	}

	/**
	 * @param type
	 * @return
	 */
	public Vector<CacheContent> getCachedContents(String type) {

		Vector<CacheContent> contents = new Vector<CacheContent>();

		String query = "SELECT NAME,LSID,DATE,FILE,TYPE,CLASSNAME FROM "
				+ CACHETABLENAME;
		if (!type.trim().equals("")) {
			query += " WHERE TYPE = '" + type + "'";
		}
		try {
			ResultSet rs = null;
			try {
				rs = _stmt.executeQuery(query);
				if (rs == null)
					throw new SQLException("Query Failed: " + query);
				while (rs.next()) {
					CacheContent cc = new CacheContent();
					cc.setName(rs.getString(1));
					try {
						KeplerLSID lsid = new KeplerLSID(rs.getString(2));
						cc.setLsid(lsid);
						Long l = Long.parseLong(rs.getString(3));
						Date d = new Date(l);
						cc.setDateChanged(d);
						File f = new File(rs.getString(4));
						cc.setFile(f);
						cc.setType(rs.getString(5));
						cc.setClassName(rs.getString(6));
						contents.add(cc);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} finally {
				if(rs != null) {
					rs.close();
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
			contents = new Vector<CacheContent>();
		}

		return contents;

	}

	/**
	 * Return a complete list of KeplerLSIDs for everything that is in the
	 * cache.
	 * 
	 * @return
	 */
	public Vector<KeplerLSID> getCachedLsids() {
		return getCachedLsids(null);
	}
	
	/**
	 * @param lsid
	 * @return
	 */
	public Vector<KeplerLSID> getCachedLsids(KeplerLSID lsid) {
		Vector<KeplerLSID> lsids = new Vector<KeplerLSID>();

		String query = "SELECT LSID FROM " + CACHETABLENAME;
		if (lsid != null) {
			query += " WHERE LSID like '" + lsid.toStringWithoutRevision() + "%'";
		}
		if (isDebugging) log.debug(query);
		ResultSet rs;
		try {
			rs = _stmt.executeQuery(query);
			if (rs == null)
				throw new SQLException("Query Failed: " + query);
			while (rs.next()) {
				String lsidStr = rs.getString(1);
				try {
					KeplerLSID cachedLsid = new KeplerLSID(lsidStr);
					lsids.add(cachedLsid);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			lsids = new Vector<KeplerLSID>();
		}
		
		return lsids;
	}
	
	/** Get a list of LSIDs for a class name. */
	public List<KeplerLSID> getCachedLsidsForClass(String className) throws Exception {
	    List<KeplerLSID> retval = new LinkedList<KeplerLSID>();
	    ResultSet result = null;
        try {
            _getLsidsForClassPrepStmt.setString(1, className);
            result = _getLsidsForClassPrepStmt.executeQuery();
            while (result.next()) {
                retval.add(new KeplerLSID(result.getString(1)));
            }
        } finally {
            if (result != null) {
                result.close();
            }
        }
	    return retval;
	}

	/**
	 * @param lsid
	 * @return
	 */
	public Long getHighestCachedLsidRevision(KeplerLSID lsid) {
		Long highestRev = 0L;
		Vector<KeplerLSID> cachedLsids = getCachedLsids(lsid);
		for (KeplerLSID cachedLsid : cachedLsids) {
			Long thisRev = cachedLsid.getRevision();
			if (thisRev > highestRev) {
				highestRev = thisRev;
			}
		}
		return highestRev;
	}

	/**
	 * Return a list of Semantic Types for the given LSID.
	 * 
	 * @param lsid
	 * @return
	 */
	public Vector<KeplerLSID> getSemanticTypesFor(KeplerLSID lsid) {

		Vector<KeplerLSID> semTypes = new Vector<KeplerLSID>();

		ResultSet rs;
		try {
	        _getSemTypeForLsidPrepStmt.setString(1, lsid.toString());
			rs = _getSemTypeForLsidPrepStmt.executeQuery();
			if (rs == null)
				throw new SQLException("Query Failed: " + _getSemTypeForLsidPrepStmt);
			while (rs.next()) {
				String lsidStr = rs.getString(1);
				try {
					KeplerLSID semType = new KeplerLSID(lsidStr);
					semTypes.add(semType);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			semTypes = new Vector<KeplerLSID>();
		}

		return semTypes;

	}

	/**
	 * return an iterator of all of the CacheObjectInterfaces in the cache
	 */
	public Iterator<CacheObjectInterface> getCacheObjectIterator()
			throws CacheException {
		return getCacheObjectIterator("");
	}

	/**
	 * Return the CacheObject that has the highest revision number and matches
	 * the given LSID, return null if not found.
	 * 
	 * @param anLsid
	 * @return
	 * @throws CacheException
	 */
	public CacheObject getHighestCacheObjectRevision(KeplerLSID anLsid)
			throws CacheException {

		CacheObject coWithHighestLSID = null;
		try {
			long highestRev = 0;
			Vector<KeplerLSID> cachedLsids = getCachedLsids();
			for (KeplerLSID lsid : cachedLsids) {
				if (lsid.equalsWithoutRevision(anLsid)) {
					if (lsid.getRevision() > highestRev) {
						highestRev = lsid.getRevision();
					}
				}
			}

			String highestRevLsidStr = anLsid.toStringWithoutRevision() + ":"
					+ highestRev;
			KeplerLSID highestRevLsid = new KeplerLSID(highestRevLsidStr);

			coWithHighestLSID = (CacheObject) getObject(highestRevLsid);
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return coWithHighestLSID;
	}

	/**
	 * Read the Java Object Serialized in the given file and add it to the
	 * objectHash.
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private CacheObjectInterface deserializeCacheObject(String file)
			throws Exception {
		// deserialize the cache object and put it in the hash
		File f = new File(objectPath, file);
		try {
			FileInputStream fis = new FileInputStream(f);
			ObjectInputStream ois = null;
			try {
			    ois = new ObjectInputStream(fis);
    			CacheObjectInterface coi = null;
    			coi = (CacheObjectInterface) ois.readObject();
    			if (coi != null) {
    				// TODO do all items need to be added to objectHash here?
    				// for most cacheObjects getLSID() returns null, which was
    				// giving an NPE here.
    				// ActorCacheObject seems to work because it implements
    				// readExternal which
    				// sets its lsid.
    				if (coi.getLSID() != null) {
    					objectHash.put(coi.getLSID().toString(), coi);
    				}
    				return coi;
    			}
			} finally {
			    if(ois != null) {
			        ois.close();
			    }
			}
		} catch (FileNotFoundException fnfe) {
			log.warn(file + " could not be found:" + fnfe);
		} catch (ClassNotFoundException cnfe) {
			log.warn(file + " could not be instantiated:");
			log.warn("  No class definition found for " + cnfe.getMessage());
		}
		return null;
	}

	/**
	 * @param type
	 * @return all cache objects that are of the specified type
	 * @throws CacheException
	 */
	public Iterator<CacheObjectInterface> getCacheObjectIterator(String type)
			throws CacheException {
		if (isDebugging)
			log.debug("getCacheObjectIterator(" + type + ")");
		try {
			Vector<CacheObjectInterface> items = new Vector<CacheObjectInterface>();
			String sql = "select name, lsid, file, type from " + CACHETABLENAME;
			if (!type.trim().equals("")) {
				sql += " WHERE type = '" + type + "'";
			}
			if (isDebugging)
				log.debug(sql);
			ResultSet rs = _stmt.executeQuery(sql);
			while (rs.next()) {
				String name = rs.getString("name");
				String file = rs.getString("file");
				String lsidString = rs.getString("lsid");
				String coType = rs.getString("type");
				if (isDebugging) {
					log.debug("name: " + name);
					log.debug("file: " + file);
					log.debug("lsidString: " + lsidString);
					log.debug("type: " + coType);
				}

				CacheObjectInterface coi = (CacheObjectInterface) objectHash
						.get(lsidString);
				if (coi == null) {
					deserializeCacheObject(file);
				}
				items.add(coi);
			}
			rs.close();
			return items.iterator();
		} catch (Exception e) {
			e.printStackTrace();
			throw new CacheException(
					"Error creating CacheObjectInterface iterator. "
							+ "Try removing the ~/.kepler directory?: "
							+ e.getMessage());
		}
	}

	/**
	 * return true of the given lsid has an associated object in the cache
	 * 
	 * @param lsid
	 */
	public boolean isContained(KeplerLSID lsid) throws CacheException {
		boolean foundIt = false;
		try {
		    _getLsidForLsidPrepStmt.setString(1, lsid.toString());
			ResultSet rs = _getLsidForLsidPrepStmt.executeQuery();
			if (rs.next()) {
				foundIt = true;
			}
			rs.close();
		} catch (Exception e) {
			throw new CacheException("Error determining contents of cache: "
					+ e.getMessage());
		}
		
		/*
		if(foundIt && !_isObjectSerializedInFile(lsid)) {
		    System.out.println("WARNING: in cache database, but not on file system: " + lsid);
		}
		*/
		
		return foundIt;
	}

	/**
	 * clear the cache of all contents
	 */
	public void clearCache() throws SQLException, CacheException {
		// Remove the data files.
		CacheUtil.cleanUpDir(new File(objectPath));
		// Clear our objectHash.
		objectHash.clear();

		String sql = "delete from " + CACHETABLENAME;
		_stmt.execute(sql);
		_conn.commit();

	}

	/**
	 * add a CacheListener to listen for cache events
	 */
	public void addCacheListener(CacheListener listener) {
		listeners.add(listener);
	}

	/**
	 * notifies any listeners as to an action taking place.
	 */
	private void notifyListeners(CacheObjectInterface co, String op) {
		CacheEvent ce = new CacheEvent(co);

		if (op.equals("add")) {
			for (int i = 0; i < listeners.size(); i++) {
				CacheListener cl = (CacheListener) listeners.elementAt(i);
				cl.objectAdded(ce);
			}
			co.objectAdded();
		} else if (op.equals("remove")) {
			for (int i = 0; i < listeners.size(); i++) {
				CacheListener cl = (CacheListener) listeners.elementAt(i);
				cl.objectRemoved(ce);
			}
			co.objectRemoved();
		} else if (op.equals("purge")) {
			for (int i = 0; i < listeners.size(); i++) {
				CacheListener cl = (CacheListener) listeners.elementAt(i);
				cl.objectPurged(ce);
			}
			co.objectPurged();
		}
	}

	private void serializeObjectInFile(CacheObjectInterface co, String filename)
			throws CacheException {
		File outputFile = new File("");
		try {
			outputFile = new File(CacheManager.objectPath, filename);
			if (isDebugging)
				log.debug(outputFile.toString());
			FileOutputStream fos = new FileOutputStream(outputFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(co); // serialize the CacheObjectInterface itself
			oos.flush();
			oos.close();
		} catch (IOException e) {
			log.error("Serializing object to cache has failed: "+outputFile.toString());
			e.printStackTrace();
			throw new CacheException("Unable to serialize " + co.getName(), e);
		}

	}
	
	/** Returns true if the serialized object for an LSID exists
	 *  in cache objects directory.
	 */
	private boolean _isObjectSerializedInFile(KeplerLSID lsid) {
	    File file = new File(CacheManager.objectPath, lsid.toString());
	    return file.exists() && file.isFile();
	}
	
	/** Get the name of the cache database schema. */
	public static String getDatabaseSchemaName() {
        // set the schema based on the the contents of the suite.
	    try {
            // put "S" in front of the schema name: HSQL will not accept
            // schema names beginning with a digit.
	        return "S" + ModuleTree.instance().getModuleConfigurationMD5();
	    } catch(Exception e) {
	        MessageHandler.error("Error getting cache database schema name; using PUBLIC instead.", e);
	        return "PUBLIC";
	    }
	}
}