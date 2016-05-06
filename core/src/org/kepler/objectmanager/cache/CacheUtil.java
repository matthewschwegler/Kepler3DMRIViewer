/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:21:34 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31119 $'
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.kepler.util.sql.DatabaseFactory;

/**
 * Class that represents an object in the ObjectCache. This class should be
 * extended by each type of object that wants to control its own lifecycle
 * events and serialization events.
 */
public class CacheUtil {
	/**
	 * execute a SQL command against the hsql database
	 */
	public static synchronized void executeSQLCommand(String sql)
			throws SQLException, CacheException {
		Connection conn = null;
		Statement st = null;
		try {
			conn = DatabaseFactory.getDBConnection();
			st = conn.createStatement();
			st.execute(sql);
		} catch (ClassNotFoundException cnfe) {
			throw new CacheException("Could not get the database connection: "
					+ cnfe.getMessage());
		} catch (SQLException sqle) {
			// sqle.printStackTrace();
			throw sqle;
		} finally {
			st.close();
			conn.close();
		}
	}

	/**
	 * execute a SQL query against the hsql database
	 */
	//comment this method out since returning a ResultSet is not good way for invocation 
	// since it is hard to close connections. 
//	public static synchronized ResultSet executeSQLQuery(String sql)
//			throws SQLException, CacheException {
//		Connection conn = null;
//		Statement st = null;
//		try {
//			conn = DatabaseFactory.getDBConnection();
//			// set up the statement so that it's scrollable and not updateable
//			// by other resultsets
//			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
//					ResultSet.CONCUR_UPDATABLE);
//			ResultSet rs = st.executeQuery(sql);
//			return rs;
//		} catch (ClassNotFoundException cnfe) {
//			// cnfe.printStackTrace();
//			throw new CacheException("Could not get the database connection: "
//					+ cnfe.getMessage());
//		} catch (SQLException sqle) {
//			// sqle.printStackTrace();
//			throw sqle;
//		}
//	}

	/**
	 * recursively cleans up the work dir, deleting old files that are no longer
	 * needed.
	 */
	public static void cleanUpDir(File dir) {
		String[] dirList = dir.list();
		if (dirList != null)
			for (int i = 0; i < dirList.length; i++) {
				File f = new File(dir, dirList[i]);
				if (f.isDirectory()) {
					cleanUpDir(f);
					f.delete();
				} else {
					f.delete();
				}
			}
	}

	/**
	 * reads bytes from in InputStream and writes them to an OutputStream.
	 * 
	 *@param is
	 *@param os
	 *@throws IOException
	 */
	public static void writeInputStreamToOutputStream(InputStream is,
			OutputStream os) throws IOException {
		byte[] b = new byte[1024];
		int numread = is.read(b, 0, 1024);

		while (numread != -1) {
			os.write(b, 0, numread);
			numread = is.read(b, 0, 1024);
		}
		os.flush();
	}

	/**
	 * writes the bytes in the reader to the writer.
	 * 
	 * @param r
	 *            the reader
	 * @param w
	 *            the writer
	 */
	public static void writeReaderToWriter(Reader r, Writer w)
			throws IOException {
		char[] c = new char[1024];
		int numread = r.read(c, 0, 1024);
		while (numread != -1) {
			w.write(c, 0, numread);
			numread = r.read(c, 0, 1024);
		}
		w.flush();
	}
}