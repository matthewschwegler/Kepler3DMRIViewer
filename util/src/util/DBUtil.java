/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

package util;

import java.util.Map;
import java.util.TreeMap;

//////////////////////////////////////////////////////////////////////////
//// DBUtil
/**
 * @author Efrat Jaeger
 * @version $Id: DBUtil.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 4.0.1
 */
public class DBUtil {

	/** This class cannot be instantiated. */
	private DBUtil() {
	}

	private static Map<String, String> _drivers = new TreeMap<String, String>();

	static {
		_drivers.put("oracle", "oracle.jdbc.driver.OracleDriver");
		_drivers.put("db2", "com.ibm.db2.jcc.DB2Driver");
		_drivers.put("local ms access", "sun.jdbc.odbc.JdbcOdbcDriver");
		_drivers.put("remote ms access", "org.objectweb.rmijdbc.Driver");
		_drivers.put("ms sql server",
				"com.microsoft.sqlserver.jdbc.SQLServerDriver");
		_drivers.put("postgresql", "org.postgresql.Driver");
		_drivers.put("mysql", "com.mysql.jdbc.Driver");
		_drivers.put("hsql", "org.hsqldb.jdbcDriver");
		_drivers.put("sqlite", "org.sqlite.JDBC");
	}

	public static String get(String dbFormat) {
		if (_drivers.containsKey(dbFormat)) {
			return _drivers.get(dbFormat);
		} else {
			return "";
		}
	}
}