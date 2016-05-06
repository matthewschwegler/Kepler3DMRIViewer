/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

package org.kepler.objectmanager.library;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.sql.DatabaseFactory;

public class LibSearch {
	private static final Log log = LogFactory.getLog(LibSearch.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * the name of the table in the database
	 */
	public static final String LIB_SEARCH_TABLE_NAME = "LIBRARY_SEARCH";

	/**
	 * Map of integers for the different types of strings that are stored in the
	 * CACHE_SEARCH table.
	 */
	public static final int TYPE_NAME = 1;
	public static final int TYPE_CLASSNAME = 2;
	public static final int TYPE_ONTCLASSNAME = 3;
	public static final int TYPE_ONTOLOGY = 4;
	public static final int TYPE_FOLDERNAME = 5;
	public static final int TYPE_KARNAME = 6;
	public static final int TYPE_LOCALREPO = 7;

	private Connection _conn;
	private Statement _stmt;
	private PreparedStatement _insertPrepStmt;
	private PreparedStatement _deletePrepStmt;
	private PreparedStatement _deleteAllPrepStmt;

	/**
	 * A constructor that creates a new connection to the database.
	 * 
	 * @throws Exception
	 */
	public LibSearch() throws Exception {
		if (isDebugging)
			log.debug("new CacheSearch()");
		try {
			initialize(DatabaseFactory.getDBConnection());
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Error obtaining database connection: "
					+ e.getMessage());
		}

	}

	/**
	 * A constructor that is given a connection to the database.
	 * 
	 * @param conn
	 */
	public LibSearch(Connection conn) {
		initialize(conn);
	}

	/**
	 * Initialize the instance.
	 * 
	 * @param conn
	 */
	public void initialize(Connection conn) {
		if (isDebugging)
			log.debug("initialize(" + conn.toString() + ")");
		_conn = conn;

		try {
			// By creating the statement and keeping it around
			// make sure to close your resultsets to save memory
			_stmt = _conn.createStatement();
			_insertPrepStmt = _conn.prepareStatement("insert into "
					+ LIB_SEARCH_TABLE_NAME
					+ " (sid, type, liid, searchstring) values ( ?, ?, ?, ? )");
			_deletePrepStmt = _conn.prepareStatement("delete from "
					+ LIB_SEARCH_TABLE_NAME + " where liid = ?");
			_deleteAllPrepStmt = _conn.prepareStatement("delete from "
					+ LIB_SEARCH_TABLE_NAME);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Search all search string types in the index for a specific string.
	 * 
	 * @param value
	 * @return Vector<Integer> of Library Index IDs that match
	 */
	public Vector<Integer> search(String value) {
		// Read the configuration
		LibSearchConfiguration lsc = new LibSearchConfiguration();
		return search(value, lsc.getSearchTypes());
	}

	/**
	 * Search a specific search string type in the index for the given value.
	 * 
	 * @param value
	 * @param type
	 * @return Vector<Integer> of Library Index IDs that match
	 */
	public Vector<Integer> search(String value, int type) {
		Vector<Integer> types = new Vector<Integer>(1);
		types.add(new Integer(type));
		return search(value, types);
	}

	/**
	 * Return all LIID values that match the given string and search types.
	 * 
	 * @param value
	 * @param types
	 * @return Vector<Integer> that are the Library Index IDs that match
	 */
	public Vector<Integer> search(String value, Vector<Integer> types) {
		Vector<Integer> liids = new Vector<Integer>();
		try {
			String query = "SELECT liid FROM " + LIB_SEARCH_TABLE_NAME
					+ " WHERE searchstring like '%" + value.toLowerCase()
					+ "%'";
			if (types.size() > 0) {
				query += " and ( type = " + types.elementAt(0);
				for (int i = 1; i < types.size(); i++) {
					query += " or type = " + types.elementAt(i);
				}
				query += " ) ";
			}
			if (isDebugging)
				log.debug(query);

			ResultSet rs = _stmt.executeQuery(query);
			if (rs != null) {
				while (rs.next()) {
					int liid = rs.getInt(1);
					liids.add(new Integer(liid));
				}
				rs.close();
			}
		} catch (Exception sqle) {
			sqle.printStackTrace();
		}
		return liids;
	}

	/**
	 * Delete everything from the database table.
	 */
	public void clear() throws SQLException {
		_deleteAllPrepStmt.executeUpdate();

		String resetAutoInc = "ALTER TABLE " + LIB_SEARCH_TABLE_NAME
				+ " ALTER COLUMN SID RESTART WITH 1";
		_stmt.execute(resetAutoInc);
	}

	/**
	 * Remove all of the entries from the table that match the given KeplerLSID.
	 * 
	 * @param lsid
	 * @throws SQLException
	 */
	public void remove(int liid) throws SQLException {
		_deletePrepStmt.setInt(1, liid);
		_deletePrepStmt.executeUpdate();
		_deletePrepStmt.clearParameters();
	}

	/**
	 * Count how many rows there are in the LIBRARY_SEARCH table.
	 * 
	 * @return
	 */
	public int countItems() {
		int count = 0;
		try {
			String cntQuery = "SELECT count(SID) from " + LIB_SEARCH_TABLE_NAME;
			ResultSet rs = _stmt.executeQuery(cntQuery);
			if (rs == null)
				return count;
			if (rs.next()) {
				count = rs.getInt(1);
			}
			rs.close();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		return count;

	}

	/**
	 * Convenience method to reduce code redundancy.
	 * 
	 * @param type
	 * @param lsid
	 * @param searchString
	 * @throws Exception
	 */
	public void insertRow(int type, int liid, String searchString)
			throws SQLException {
		if (isDebugging)
			log.debug("insertRow(" + type + "," + liid + "," + searchString
					+ ")");

		_insertPrepStmt.setNull(1, java.sql.Types.INTEGER);
		_insertPrepStmt.setInt(2, type);
		_insertPrepStmt.setInt(3, liid);
		_insertPrepStmt.setString(4, searchString.toLowerCase());
		try {
			_insertPrepStmt.executeUpdate();
		} catch (SQLException sqle) {
			// if (isDebugging) log.debug(sqle.getMessage());
			if (sqle.getErrorCode() == -104) {
				// the insert violated the unique key (LIID,SEARCHSTRING)
				// so we're just going to ignore the error since this
				// search string already exists for this LIID
				// if (isDebugging) log.debug("Ignoring duplicate insert");
			} else {
				throw sqle;
			}
		} finally {
			_insertPrepStmt.clearParameters();
		}

	}
}
