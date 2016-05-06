/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 11:13:44 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31472 $'
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
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * This class represents one row of the CACHECONTENTTABLE.
 * 
 * @author Aaron Schultz
 */
public class CacheContent {

	private static final Log log = LogFactory.getLog(CacheContent.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private String _name;
	private KeplerLSID _lsid;
	private Date _dateChanged;
	private File _file;
	private String _type;
	private String _className;

	private Vector<KeplerLSID> _semanticTypes;

	public CacheContent() {
		_semanticTypes = new Vector<KeplerLSID>();
	}

	public void populate(KeplerLSID lsid, Statement stmt) throws Exception {

		String query = "SELECT NAME,LSID,DATE,FILE,TYPE,CLASSNAME FROM "
				+ CacheManager.CACHETABLENAME + " WHERE LSID = '"
				+ lsid.toString() + "'";
		if (isDebugging) log.debug(query);
		ResultSet rs = stmt.executeQuery(query);
		if (rs == null)
			throw new SQLException("Query Failed: " + query);
		if (rs.next()) {
			String name = rs.getString(1);
			String lsidStr = rs.getString(2);
			Long d = rs.getLong(3);
			String file = rs.getString(4);
			String type = rs.getString(5);
			String className = rs.getString(6);

			setName(name);
			setLsid(new KeplerLSID(lsidStr));
			setDateChanged(new Date(d));
			setFile(new File(file));
			setType(type);
			setClassName(className);
		} else {
			throw new SQLException(lsid + " was not found in "
					+ CacheManager.CACHETABLENAME);
		}
		rs.close();

	}

	/**
	 * @return the name
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * @return the lsid
	 */
	public KeplerLSID getLsid() {
		return _lsid;
	}

	/**
	 * @param lsid
	 *            the lsid to set
	 */
	public void setLsid(KeplerLSID lsid) {
		_lsid = lsid;
	}

	/**
	 * @return the dateChanged
	 */
	public Date getDateChanged() {
		return _dateChanged;
	}

	/**
	 * @param dateChanged
	 *            the dateChanged to set
	 */
	public void setDateChanged(Date dateChanged) {
		_dateChanged = dateChanged;
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		return _file;
	}

	/**
	 * @param file
	 *            the file to set
	 */
	public void setFile(File file) {
		_file = file;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return _type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		_type = type;
	}
	
	public String getClassName() {
		return _className;
	}
	
	public void setClassName(String className) {
		_className = className;
	}

	/**
	 * @return the semanticTypes
	 */
	public Vector<KeplerLSID> getSemanticTypes() {
		return _semanticTypes;
	}

	/**
	 * Populate the semantic types from the CACHE_SEMTYPES table.
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	public void populateSemanticTypes(Statement stmt) throws SQLException {
		String query = "SELECT SEMTYPE FROM "
				+ CacheManager.CACHE_SEMTYPES_TABLE_NAME + " WHERE LSID = '"
				+ getLsid().toString() + "'";
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(query);
			if (rs == null)
				throw new SQLException("Query Failed: " + query);
			while (rs.next()) {
				try {
					KeplerLSID semType = new KeplerLSID(rs.getString(1));
					_semanticTypes.add(semType);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			if(rs != null) {
				rs.close();
			}
		}
	}

}
