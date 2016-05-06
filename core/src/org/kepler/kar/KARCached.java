/**
 *  '$RCSfile$'
 *  '$Author: crawl $'
 *  '$Date: 2012-06-13 09:44:25 -0700 (Wed, 13 Jun 2012) $'
 *  '$Revision: 29932 $'
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * This class holds the information for one row of the KARS_CACHED table.
 * 
 * @author Aaron Schultz
 */
public class KARCached {
	private static final Log log = LogFactory.getLog(KARCached.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private File _file;
	private KeplerLSID _lsid;
	private String _version;
	private String _repoName;

	public KARCached() {
	}

	/**
	 * Populate this object from the database. setLsid() must be called before
	 * running this method.
	 * 
	 * @param stmt
	 * @throws Exception
	 */
	public void populate(File karFile, Statement stmt) throws Exception {

		String query = "SELECT LSID,VERSION,REPONAME FROM "
				+ KARCacheManager.KARS_CACHED_TABLE_NAME + " WHERE FILE = '"
				+ karFile + "'";
		if (isDebugging)
			log.debug(query);
		ResultSet rs = stmt.executeQuery(query);
		if (rs == null)
			throw new SQLException("Query Failed: " + query);
		if (rs.next()) {
			String lsidStr = rs.getString(2);
			String version = rs.getString(3);
			String reponame = rs.getString(4);
			if (rs.wasNull()) {
				reponame = null;
			}

			if (!karFile.isFile()) {
				throw new Exception(
						"KARS_CACHED table data in database is corrupt: "
						+ "KAR file is not a file");
			}
			KeplerLSID lsid = new KeplerLSID(lsidStr);
			if (lsid == null) {
				throw new Exception(
						"KARS_CACHED table data in database is corrupt: "
						+ "KAR lsid is null");
			}
			setFile(karFile);
			setLsid(lsid);
			setVersion(version);
			setRepoName(reponame);
		} else {
			throw new SQLException(getFile() + " was not found in "
					+ KARCacheManager.KARS_CACHED_TABLE_NAME);
		}
		rs.close();
	}

	public File getFile() {
		return _file;
	}

	public void setFile(File file) {
		_file = file;
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
		this._lsid = lsid;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return _file.getName();
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return _version;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(String version) {
		this._version = version;
	}

	/**
	 * @return the repoName
	 */
	public String getRepoName() {
		return _repoName;
	}

	/**
	 * @param repoName
	 *            the repoName to set
	 */
	public void setRepoName(String repoName) {
		this._repoName = repoName;
	}

	/**
	 * @return the path
	 */
	public File getPath() {
		return getFile().getParentFile();
	}

}
