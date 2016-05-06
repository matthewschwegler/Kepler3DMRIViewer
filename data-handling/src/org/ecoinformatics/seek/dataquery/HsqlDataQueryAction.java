/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.dataquery;

import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.sql.DatabaseFactory;

/**
 * This class will handle data query action base on Hsql sql engine
 * 
 * @author Jing Tao
 * 
 */
public class HsqlDataQueryAction extends DataQueryAction {
	private Connection conn = null;
	private Statement stmt = null;
	private String sql = null;
	private ResultSet rs = null;
	private boolean printResultset = false;

	private static Log log;
	private static boolean isDebugging;

	static {
		log = LogFactory.getLog("org.ecoinformatics.seek.dataquery");
		isDebugging = log.isDebugEnabled();
	}

	/**
	 * Default constructor. In this conctructor, it will set up connection to db
	 */
	public HsqlDataQueryAction() throws SQLException, ClassNotFoundException {
		conn = DatabaseFactory.getDBConnection();
	}

	/**
	 * Get sql command
	 * 
	 * @return String
	 */
	public String getSQL() {
		return sql;
	}

	/**
	 * Set sql command
	 * 
	 * @param mysql
	 *            String
	 */
	public void setSQL(String mysql) {
		sql = mysql;
	}

	/**
	 * Method to get result set
	 * 
	 * @return ResultSet
	 */
	public ResultSet getResultSet() {
		return rs;
	}

	/**
	 * Close the underlying database resources and invalidate object.
	 * 
	 */
	public void close() {
		try {
			rs.close();
			stmt.close();
			conn.close();
		} catch (SQLException e) {
			log.debug("Unable to close objects ", e);
		}
		rs = null;
	}

	/**
	 * Method to print out the result set. This is for debug.
	 * 
	 * @param printResultset
	 *            boolean
	 */
	public void setPrintResultset(boolean printResultset) {
		this.printResultset = printResultset;
	}

	/**
	 * This method will handle such staff: Load data into db(create text tables
	 * and set source file location to table) Run sql query against the db
	 * 
	 * @param event
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent event) {
		if (sql == null) {
			log.warn("Sql query is null and user should specify it");
			return;
		}
		try {
			stmt = conn.createStatement();
		} catch (SQLException sqle) {
			log.warn("Couldn't create a statement", sqle);
			return;
		}
		// Run the sql query and get resultset
		try {
			rs = stmt.executeQuery(sql);
			// this part is only for debug
			if (printResultset) {
				handleResultSet(rs);
			}
		} catch (SQLException e) {
			if (isDebugging) {
				log.debug("The error to run sql command " + sql + " is ", e);
			}
			return;
		}

	}// actionPerformed

	/*
	 * This method will transfer resultset to some output We need to discuss it
	 */
	private void handleResultSet(ResultSet result) throws SQLException {
		if (result == null) {
			log.debug("result is null");
			return;
		}
		ResultSetMetaData meta = result.getMetaData();
		int colmax = meta.getColumnCount();
		Object obj = null;

		// the result set is a cursor into the data. You can only
		// point to one row at a time
		// assume we are pointing to BEFORE the first row
		// rs.next() points to next row and returns true
		// or false if there is no next row, which breaks the loop
		while (result.next()) {
			for (int i = 0; i < colmax; i++) {
				obj = result.getObject(i + 1); // Is SQL the first column is
												// indexed
				// with 1 not 0
				if (isDebugging) {
					log.debug(obj.toString());
				}
			}// for
		}// while

	}// handleResultSet
}