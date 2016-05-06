/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2010-12-09 15:11:45 -0800 (Thu, 09 Dec 2010) $' 
 * '$Revision: 26470 $'
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

package org.ecoinformatics.seek.dataquery;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.sql.DatabaseFactory;

/**
 * This class will check if a given table name already existed in db. This class
 * has tigh relationship to hsql. If we use different sql engine This class need
 * to re-implemented.
 * 
 * @author Jing Tao
 * 
 */

public class DBTableExistenceChecker {

	// this table name should NOT be easy to be duplidate
	// Make this table name weild to make sure it is hard to duplicate
	private static final String TABLENAME = "KEPLERTEXTTABLES";
	private static final String TABLENAMEFIELD = "TABLENAME";
	private static final String URLFIELD = "URL";

	private static Log log;
	private static boolean isDebugging;

	static {
		log = LogFactory.getLog("org.ecoinformatics.seek.dataquery");
		isDebugging = log.isDebugEnabled();
	}

	/**
	 * Check if a given url existed. Url is a key for table
	 * 
	 * @param url
	 *            String
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @return boolean
	 */
	public boolean isURLExisted(String url) throws SQLException,
			ClassNotFoundException {
		return isGivenStringExisted(URLFIELD, url);
	}

	/**
	 * Check if a given table name existed
	 * 
	 * @param tableName
	 *            String
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @return boolean
	 */
	public boolean isTableNameExisted(String tableName) throws SQLException,
			ClassNotFoundException {
		return isGivenStringExisted(TABLENAMEFIELD, tableName);
	}

	/*
	 * A method will check if a given string existed in the table
	 */
	private boolean isGivenStringExisted(String fieldName, String givenString)
			throws SQLException, ClassNotFoundException {
		boolean existed = true;
		String sql = DBTablesGenerator.SELECT + DBTablesGenerator.SPACE
				+ fieldName + DBTablesGenerator.SPACE + DBTablesGenerator.FROM
				+ DBTablesGenerator.SPACE + TABLENAME + DBTablesGenerator.SPACE
				+ DBTablesGenerator.WHERE + DBTablesGenerator.SPACE + fieldName
				+ DBTablesGenerator.SPACE + DBTablesGenerator.LIKE
				+ DBTablesGenerator.SPACE + "'" + givenString + "'"
				+ DBTablesGenerator.SEMICOLON;
		if (isDebugging) {
			log.debug("The sql for checking table if it existed is " + sql);
		}
		Connection conn = DatabaseFactory.getDBConnection();
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery(sql);
		if (rs.next()) {
			existed = true;
		} else {
			existed = false;
		}
		rs.close();
		st.close();
		conn.close();
		return existed;
	}// isTalbeExisted

	/**
	 * Method will store a gnerated table info (table name and url) into a
	 * persistant table
	 * 
	 * @param tableName
	 *            String
	 * @param url
	 *            String
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public void storeTableRecord(String tableName, String url)
			throws SQLException, ClassNotFoundException {
		String sql = DBTablesGenerator.INSERT + DBTablesGenerator.SPACE
				+ TABLENAME + DBTablesGenerator.SPACE
				+ DBTablesGenerator.LEFTPARENTH + TABLENAMEFIELD
				+ DBTablesGenerator.COMMA + URLFIELD
				+ DBTablesGenerator.RIGHTPARENTH + DBTablesGenerator.SPACE
				+ DBTablesGenerator.VALUES + DBTablesGenerator.SPACE
				+ DBTablesGenerator.LEFTPARENTH + "'" + tableName + "'"
				+ DBTablesGenerator.COMMA + "'" + url + "'"
				+ DBTablesGenerator.RIGHTPARENTH + DBTablesGenerator.SEMICOLON;
		if (isDebugging) {
			log.debug("The sql to insert table record into storing table is "
					+ sql);
		}
		Connection conn = DatabaseFactory.getDBConnection();
		Statement st = conn.createStatement();
		st.execute(sql);
		st.close();
		conn.close();
	}// storeTableRecord

	public void deleteRecord(String tableName, String url) throws SQLException,
			ClassNotFoundException {
		String sql = DBTablesGenerator.DELETE + DBTablesGenerator.SPACE
				+ DBTablesGenerator.FROM + DBTablesGenerator.SPACE + TABLENAME
				+ DBTablesGenerator.SPACE + DBTablesGenerator.WHERE
				+ DBTablesGenerator.SPACE + DBTablesGenerator.LEFTPARENTH
				+ TABLENAMEFIELD + DBTablesGenerator.SPACE
				+ DBTablesGenerator.LIKE + DBTablesGenerator.SPACE + "'"
				+ tableName + "'" + DBTablesGenerator.SPACE
				+ DBTablesGenerator.AND + DBTablesGenerator.SPACE + URLFIELD
				+ DBTablesGenerator.SPACE + DBTablesGenerator.LIKE
				+ DBTablesGenerator.SPACE + "'" + url + "'"
				+ DBTablesGenerator.RIGHTPARENTH + DBTablesGenerator.SEMICOLON;
		if (isDebugging) {
			log.debug("delete record from table is " + sql);
		}
		Connection conn = DatabaseFactory.getDBConnection();
		Statement st = conn.createStatement();
		st.execute(sql);
		st.close();
		conn.close();
	}

	/**
	 * This method will return a tableName for a given URL in the storing table
	 * 
	 * @param url
	 *            String
	 * @return String
	 */
	public String getTableName(String url) throws SQLException,
			ClassNotFoundException {
		String tableName = null;
		if (url == null) {
			if (isDebugging) {
				log
						.debug("The table for given url " + url + " is "
								+ tableName);
			}
			return tableName;
		}
		String sql = DBTablesGenerator.SELECT + DBTablesGenerator.SPACE
				+ TABLENAMEFIELD + DBTablesGenerator.SPACE
				+ DBTablesGenerator.FROM + DBTablesGenerator.SPACE + TABLENAME
				+ DBTablesGenerator.SPACE + DBTablesGenerator.WHERE
				+ DBTablesGenerator.SPACE + URLFIELD + DBTablesGenerator.SPACE
				+ DBTablesGenerator.LIKE + DBTablesGenerator.SPACE + "'" + url
				+ "'" + DBTablesGenerator.SEMICOLON;
		if (isDebugging) {
			log.debug("The sql to get table name from url is " + sql);
		}
		Connection conn = DatabaseFactory.getDBConnection();
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery(sql);
		if (rs.next()) {
			tableName = rs.getString(1);
		}
		if (isDebugging) {
			log.debug("The table for given url " + url + " is " + tableName);
		}
		rs.close();
		conn.close();
		return tableName;
	}

}// DBTableExistenceChecker