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
import java.util.Vector;

import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * This class holds the information for one row of the KARS_CACHED table.
 * 
 * @author Aaron Schultz
 */
public class KARCacheError {

	private File _file;
	private KeplerLSID _lsid;
	private String _version;
	private String _repoName;
	private Vector<String> _dependencies;

	public KARCacheError() {
	}

	/**
	 * Populate this object from the database. setLsid() must be called before
	 * running this method.
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	public void populate(File karFile, Statement stmt) throws Exception {

		String query = "SELECT LSID,VERSION,REPONAME,DEPENDENCIES FROM "
				+ KARCacheManager.KAR_ERRORS_TABLE_NAME + " WHERE FILE = '"
				+ karFile + "'";
		ResultSet rs = stmt.executeQuery(query);
		if (rs == null)
			throw new SQLException("Query Failed: " + query);
		if (rs.next()) {
			String lsidStr = rs.getString(1);
			KeplerLSID lsid = new KeplerLSID(lsidStr);
			String version = rs.getString(2);
			String reponame = rs.getString(3);
			String depStr = rs.getString(4);
			Vector<String> deps = ModuleDependencyUtil.parseDependencyString(depStr);

			setFile(karFile);
			setLsid(lsid);
			setVersion(version);
			setRepoName(reponame);
			setDependencies(deps);
		} else {
			throw new SQLException(getFile().toString() + " was not found in "
					+ KARCacheManager.KAR_ERRORS_TABLE_NAME);
		}
		rs.close();
	}

	public File getFile() {
		return _file;
	}

	public void setFile(File karFile) {
		_file = karFile;
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
		return getFile().getName();
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

	/**
	 * @return the dependencies
	 */
	public Vector<String> getDependencies() {
		return _dependencies;
	}

	/**
	 * @param dependencies
	 *            the dependencies to set
	 */
	public void setDependencies(Vector<String> dependencies) {
		this._dependencies = dependencies;
	}

	public String debugString() {
		String s = "KARCacheError:\n";
		s += "  lsid: " + getLsid() + "\n";
		s += "  name: " + getName() + "\n";
		s += "  version: " + getVersion() + "\n";
		s += "  repoName: " + getRepoName() + "\n";
		s += "  path: " + getPath() + "\n";
		s += "  dependencies: " + getDependencies() + "\n";
		return s;
	}

}
