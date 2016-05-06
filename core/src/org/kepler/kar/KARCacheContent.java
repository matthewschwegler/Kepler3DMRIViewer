/**
 *  '$RCSfile$'
 *  '$Author: crawl $'
 *  '$Date: 2013-02-21 11:13:44 -0800 (Thu, 21 Feb 2013) $'
 *  '$Revision: 31472 $'
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.cache.CacheContent;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.sms.SemanticType;

/**
 * This class holds the information for one row of the KAR_CACHE_CONTENT table.
 * 
 * @author Aaron Schultz
 */
public class KARCacheContent {
	private static final Log log = LogFactory.getLog(KARCacheContent.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private String _name;
	private String _type;

	private KARCached _karCached;
	private CacheContent _cacheContent;

	private Vector<KeplerLSID> _semanticTypes;

	public KARCacheContent() {
		_semanticTypes = new Vector<KeplerLSID>();
	}

	/**
	 * Populate this object from the database. setKarLsid() and setLsid() must
	 * be called before running this method.
	 * 
	 * @param stmt
	 * @throws Exception
	 */
	public void populate(File karFile, KeplerLSID lsid, Statement stmt) throws Exception {

		String query = "SELECT NAME,TYPE FROM "
				+ KARCacheManager.KAR_CONTENTS_TABLE_NAME
				+ " WHERE FILE = '" + karFile + "' AND LSID = '"
				+ lsid.toString() + "'";
		if (isDebugging) log.debug(query);
		ResultSet rs = stmt.executeQuery(query);
		if (rs == null)
			throw new SQLException("Query Failed: " + query);
		if (rs.next()) {
			String name = rs.getString(1);
			String type = rs.getString(2);

			setName(name);
			setType(type);

			// Populate the foreign key object
			KARCached kc = new KARCached();
			kc.populate(karFile,stmt);
			setKarCached(kc);
			
			CacheContent cc = new CacheContent();
			cc.populate(getLsid(),stmt);
			setCacheContent(cc);
			
			// Populate the semantic types
			populateSemanticTypes(stmt);
		} else {
			throw new SQLException(karFile + " - " + lsid.toString()
					+ " was not found in "
					+ KARCacheManager.KAR_CONTENTS_TABLE_NAME);
		}
		rs.close();

	}
	
	/**
	 * @return File that points to the kar file
	 */
	public File getKarFile() {
		return getKarCached().getFile();
	}

	/**
	 * @return the lsid of the cache content
	 */
	public KeplerLSID getLsid() {
		return getCacheContent().getLsid();
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
		this._name = name;
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
		this._type = type;
	}

	/**
	 * @return the karCached
	 */
	public KARCached getKarCached() {
		return _karCached;
	}

	/**
	 * @param karCached
	 *            the karCached to set
	 */
	public void setKarCached(KARCached karCached) {
		this._karCached = karCached;
	}
	
	public CacheContent getCacheContent() {
		return _cacheContent;
	}
	
	public void setCacheContent(CacheContent cacheContent) {
		_cacheContent = cacheContent;
	}
	
	/**
	 * @return the _semanticTypes
	 */
	public Vector<KeplerLSID> getSemanticTypes() {
		return _semanticTypes;
	}

	/**
	 * @param semanticTypes the _semanticTypes to set
	 */
	public void setSemanticTypes(Vector<KeplerLSID> semanticTypes) {
		_semanticTypes = semanticTypes;
	}
	
	/**
	 * Populate the semantic types from the CACHE_SEMTYPES table.
	 * @param stmt
	 * @throws SQLException
	 */
	public void populateSemanticTypes(Statement stmt) throws SQLException {
		String query = "SELECT SEMTYPE FROM " + CacheManager.CACHE_SEMTYPES_TABLE_NAME
			+ " WHERE LSID = '" + getLsid().toString() + "'";
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(query);
			if (rs == null) throw new SQLException("Query Failed: " + query);
			while (rs.next()) {
				try {
					String rawEntry = rs.getString(1);
					SemanticType st = new SemanticType();
					st.setExpression(rawEntry);
					KeplerLSID semType = new KeplerLSID(st.getConceptUri());
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
