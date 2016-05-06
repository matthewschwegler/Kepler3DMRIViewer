/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-06-15 14:28:15 -0700 (Fri, 15 Jun 2012) $' 
 * '$Revision: 29958 $'
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

package org.geon;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import util.DBUtil;

//////////////////////////////////////////////////////////////////////////
//// OpenDBConnection
/**
 * This actor opens a database connection using the database format, database
 * URL, username and password, and sends a reference to it.
 * 
 * @author Efrat Jaeger
 * @version $Id: OpenDBConnection.java 29958 2012-06-15 21:28:15Z crawl $
 * @since Ptolemy II 3.0.2
 */
public class OpenDBConnection extends TypedAtomicActor {

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */

	public OpenDBConnection(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setMultiport(true);

		dbcon = new TypedIOPort(this, "dbcon", false, true);
		// Set the type constraint.
		dbcon.setTypeEquals(DBConnectionToken.DBCONNECTION);

		// catalog = new StringAttribute(this, "catalog");
		// catalog.setExpression("");
		// _catalog = _none;

		databaseFormat = new StringParameter(this, "databaseFormat");
		databaseFormat.setDisplayName("database format");
		databaseFormat.setExpression("Oracle");
		databaseFormat.addChoice("Oracle");
		databaseFormat.addChoice("DB2");
		databaseFormat.addChoice("Local MS Access");
		databaseFormat.addChoice("MS SQL Server");
		databaseFormat.addChoice("PostgreSQL");
		databaseFormat.addChoice("MySQL");
		databaseFormat.addChoice("HSQL");
		databaseFormat.addChoice("SQLite");
		_dbFormat = _DBType._ORCL;

		// driverName = new StringAttribute(this, "driverName");
		databaseURL = new StringParameter(this, "databaseURL");

		username = new StringParameter(this, "username");
		password = new StringParameter(this, "password");

		dbParams = new TypedIOPort(this, "dbParams", false, true);
		dbParams.setTypeEquals(getDBParamsType());

		_attachText("_iconDescription", "<svg>\n"
				+ "<ellipse cx=\"0\" cy=\"-30\" " + "rx=\"20\" ry=\"10\"/>\n"
				+ "<line x1=\"20\" y1=\"0\" " + "x2=\"20\" y2=\"-30\"/>\n"
				+ "<line x1=\"-20\" y1=\"0\" " + "x2=\"-20\" y2=\"-30\"/>\n"
				+ "<line x1=\"-20\" y1=\"0\" " + "x2=\"20\" y2=\"0\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * Input trigger: if connected, actor will only run once token is available.
	 */
	public TypedIOPort trigger;

	/** A reference to a db connection */
	public TypedIOPort dbcon;

	/** A record containing parameters to create a db connection. */
	public TypedIOPort dbParams;

	// public StringAttribute catalog;
	public StringParameter databaseFormat;
	// public StringAttribute driverName;
	public StringParameter databaseURL;
	public StringParameter username;
	public StringParameter password;

	public String strFileOrURL;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Callback for changes in attribute values. If the dbFormat has changed,
	 * points to the new driver path. At any change in the connection
	 * attributes, tries to connect to the referenced database to get the schema
	 * and send it to connected actors.
	 * 
	 * @param at
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 *                If the offsets array is not nondecreasing and nonnegative.
	 */
	public void attributeChanged(Attribute at) throws IllegalActionException {
		if (at == databaseFormat) {
			String dbFormat = databaseFormat.stringValue();
			_driverName = DBUtil.get(dbFormat.trim().toLowerCase());

			if (dbFormat.equals("Oracle")) {
				_dbFormat = _DBType._ORCL;
			} else if (dbFormat.equals("DB2")) {
				_dbFormat = _DBType._DB2;
			} else if (dbFormat.equals("Local MS Access")) {
				_dbFormat = _DBType._LACCS;
			} else if (dbFormat.equals("MS SQL Server")) {
				_dbFormat = _DBType._MSSQL;
			} else if (dbFormat.equals("PostgreSQL")) {
				_dbFormat = _DBType._PGSQL;
			} else if (dbFormat.equals("MySQL")) {
				_dbFormat = _DBType._MYSQL;
			} else if (dbFormat.equals("HSQL")) {
				_dbFormat = _DBType._HSQL;
			}  else if (dbFormat.equals("SQLite")){
				_dbFormat = _DBType._SQLite;
			}else {
				throw new IllegalActionException(this,
						"No jdbc driver within the system for " + dbFormat);
			}
			if (!_driverName.equals(this._prevDriver)) {
				_setDBURL();
				_prevDriver = _driverName;
				_getAndSendSchema();
			}
		} else if (at == databaseURL) {
			_setDBURL();
			if (!_databaseURL.equals(_prevDBURL)) {
				_prevDBURL = _databaseURL;
				_getAndSendSchema();
			}
		} else if (at == username) {
			_username = username.stringValue();
			if (!_username.equals(_prevUser)) {
				_prevUser = _username;
				_getAndSendSchema();
			}
		} else if (at == password) {
			_password = password.stringValue();
			if (!_password.equals(_prevPasswd)) {
				_prevPasswd = _password;
				_getAndSendSchema();
			}

		} else {
			super.attributeChanged(at);
		}
	}

	/**
	 * When connecting the dbcon port, trigger the connectionsChanged of the
	 * connected actor.
	 */
	public void connectionsChanged(Port port) {
		super.connectionsChanged(port);
		if (port == dbcon) {
			List<?> conPortsList = dbcon.connectedPortList();
			Iterator<?> conPorts = conPortsList.iterator();
			while (conPorts.hasNext()) {
				IOPort p = (IOPort) conPorts.next();
				if (p.isInput()) {
					Entity container = (Entity) p.getContainer();
					container.connectionsChanged(p);
				}
			}
		}
	}

	/**
	 * Connect to a database outputs a reference to the DB connection.
	 */

	public void fire() throws IllegalActionException {

		// consume tokens on trigger port if connected.
		for (int i = 0; i < trigger.getWidth(); i++) {
			if (trigger.hasToken(i)) {
				trigger.get(i);
			}
		}

		try {

			// String _username = username.stringValue();
			// String _password = password.stringValue();

			// _setDBURL();

			Connection con = _connect("fire");
			dbcon.broadcast(new DBConnectionToken(con));

			RecordToken token = _createParamsToken(_driverName, _databaseURL,
					_username, _password);
			dbParams.broadcast(token);

		} catch (Exception ex) {
			throw new IllegalActionException(this, ex,
					"fire exception DB connection");
		}
	}

	/**
	 * postfiring the actor.
	 * 
	 */
	public boolean postfire() {
		return false;
	}

	/**
	 * Returns the database schema. This function is called from the dbcon port
	 * connected actors.
	 */
	public String sendSchemaToConnected() throws IllegalActionException {
		if (_schema.equals("")) {
			_schema = _getSchema();
		}
		return _schema;
	}

	/** Get the type of database parameter token. */
	public static Type getDBParamsType() {
		return new RecordType(_paramsLabels, new Type[] { BaseType.STRING,
				BaseType.STRING, BaseType.STRING, BaseType.STRING });
	}

	/** Get a JDBC Connection from a database parameter token. */
	public static Connection getConnection(RecordToken params)
			throws IllegalActionException {
	    
		try {

			Connection retval = null;
			String driver = ((StringToken) params.get("driver")).stringValue();

            try {
                Class.forName(driver).newInstance();
            } catch (ClassNotFoundException e) {
				throw new IllegalActionException("The JDBC class " + driver
						+ " was not found on the classpath. One "
						+ "possible reason for this error is "
						+ "the JDBC jar is missing. Several JDBC "
						+ "jars are released under licenses "
						+ "incompatible with Kepler's license such "
						+ "as the Oracle JDBC jar. In "
						+ "these cases, the jar must be downloaded "
						+ "manually and placed in core/lib/jar/dbdrivers.");
			}
	        
			String user = _getParamField(params, "user");
			String passwd = _getParamField(params, "password");
			String url = _getParamField(params, "url");

			if (user.length() == 0 && passwd.length() == 0) {
				retval = DriverManager.getConnection(url);
			} else {
				retval = DriverManager.getConnection(url, user, passwd);
			}

			return retval;

		} catch (InstantiationException e) {
			throw new IllegalActionException(e.getClass().getName() + ": "
					+ e.getMessage());
		} catch (IllegalAccessException e) {
			throw new IllegalActionException(e.getClass().getName() + ": "
					+ e.getMessage());
		} catch (SQLException e) {
			throw new IllegalActionException(e.getClass().getName() + ": "
					+ e.getMessage());
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Connecting to the database and returning a database connection reference.
	 * The context is either "fire" or "other", if unable to connect from a
	 * "fire" context throws an exception (for connecting from other context it
	 * might be the case that not all the connection attributes have already
	 * been set).
	 * 
	 * @param context
	 * 	 * @throws IllegalActionException
	 */
	private Connection _connect(String context) throws IllegalActionException {
		Connection retval = null;
		try {
			RecordToken token = _createParamsToken(_driverName, _databaseURL,
					_username, _password);
			retval = getConnection(token);
		} catch (IllegalActionException e) {
			if (context.equals("fire"))
				throw new IllegalActionException(this, e.getCause(), e.getMessage());
		}

		return retval;
	}

	/**
	 * Called from attributeChanged. Once a connection attribute has been
	 * changed tries to connect to the database, get the schema and forward it
	 * to dbcon port connected actors.
	 * 
	 * @throws IllegalActionException
	 */
	private void _getAndSendSchema() throws IllegalActionException {
		_getSchema();

		// send schema to connected
		if (!_schema.equals(""))
			connectionsChanged(dbcon);
	}

	/**
	 * Gets the database schema in an XML format readable by the query builder.
	 * 
	 * @param con
	 *            - the database connection object.
	 */
	private void _getDBSchema(Connection con) {
		StringBuffer schema = new StringBuffer();
		schema.append("<schema>\n");
		StringBuffer table = new StringBuffer();
		String prevTable = "";
		String prevSchema = "";
		int numTables = 0;
		int numFields = 0;

		try {
			DatabaseMetaData dmd = con.getMetaData();
			ResultSet schemas;

			String subname = null;

			if (_dbFormat == _DBType._MYSQL) {
				schemas = dmd.getCatalogs();
				subname = _databaseURL
						.substring(_databaseURL.lastIndexOf("/") + 1);
			} else
				schemas = dmd.getSchemas();

			while (schemas.next()) {
				String schemaName = schemas.getString(1);

				// ignore certain schemas
				if (_dbFormat == _DBType._ORCL) {
					// system schema will be the same as username.
					if (!schemaName.toLowerCase().equals(_username))
						continue;
				} else if (_dbFormat == _DBType._PGSQL) {
					// system schemas
					if (schemaName.startsWith("pg_catalog")
							|| schemaName.startsWith("information_schema"))
						continue;
				} else if (_dbFormat == _DBType._MYSQL) {
					// skip schemas with a different subname
					if (!schemaName.equals(subname))
						continue;
				} else if (schemaName.toLowerCase().startsWith("sys")) {
					// system schemas
					continue;
				}

				ResultSet rs = dmd.getColumns(null, schemaName, "%", "%");

				// ResultSetMetaData md = rs.getMetaData();
				while (rs.next()) {

					// String schemaName = rs.getString(2);
					String tableName = rs.getString(3);
					String columnName = rs.getString(4);
					String columnType = rs.getString(6);

					if (tableName.equals(""))
						continue;
					if (!tableName.equals(prevTable)
							|| !schemaName.equals(prevSchema)) {
						// new table, closing a previous one if exists
						if (numFields > 0) {
							table.append("  </table>\n");
							numTables++;
							schema.append(table.toString());
						}

						table = new StringBuffer();
						table.append("  <table name=\"");
						if (!schemaName.toLowerCase().equals("null")) {
							table.append(schemaName + ".");
						}
						table.append(tableName + "\">\n");
						numFields = 0;
						prevTable = tableName;
						prevSchema = schemaName;
					}
					if (columnName.equals(""))
						continue;
					else {
						table.append("    <field name=\"" + columnName
								+ "\" dataType=\"" + columnType + "\"/>\n");
						numFields++;
					}
				}
			}
			table.append("  </table>\n");
			if (numFields > 0) {
				numTables++;
				schema.append(table.toString());
			}
			schema.append("</schema>");
			if (numTables > 0) {
				_schema = schema.toString();
			}
		} catch (SQLException ex) {
			_schema = "";
			System.out.println("SQLException: " + ex.getMessage());
		}
	}

	/**
	 * Connects to the database and get the schema.
	 * 
	 * @return database schema string
	 * @throws IllegalActionException
	 */
	private String _getSchema() throws IllegalActionException {
		Connection con = _connect("other");
		if (con != null) {
			_getDBSchema(con);
			try {
				con.close();
			} catch (SQLException e) {
				con = null;
				System.out.println("SQLException closing connection: "
						+ e.getMessage());
			}
			if (!_schema.equals(""))
				return _schema;
		}
		return "";
	}

	/**
	 * Set the absolute database URL depending on the database driver.
	 * 
	 * @throws IllegalActionException
	 */
	private void _setDBURL() throws IllegalActionException {
		_databaseURL = databaseURL.stringValue();
		switch (_dbFormat) {
		case _ORCL:
			if (!_databaseURL.trim().startsWith("jdbc:oracle:thin:@")) {
				if (_databaseURL.trim().startsWith("jdbc:oracle:")) {// a
																		// different
																		// driver
																		// type
																		// is
																		// spcified.
					int ind = _databaseURL.indexOf("@");
					if (ind > -1) {
						_databaseURL = "jdbc:oracle:thin:@"
								+ _databaseURL.substring(ind);
					} else
						throw new IllegalActionException(this,
								"Illegal database URL: " + _databaseURL);
				} else {
					_databaseURL = "jdbc:oracle:thin:@" + _databaseURL;
				}
			}
			break;
		case _DB2:
			if (!_databaseURL.trim().startsWith("jdbc:db2:")) {
				_databaseURL = "jdbc:db2:" + _databaseURL;
			}
			break;
		case _LACCS:
			if (!_databaseURL.trim().startsWith("jdbc:odbc:")) {
				_databaseURL = "jdbc:odbc:" + _databaseURL;
			}
			break;
		case _MSSQL:
			if (!_databaseURL.trim().startsWith("jdbc:sqlserver:")) {
				_databaseURL = "jdbc:sqlserver:" + _databaseURL;
			}
			_databaseURL = _databaseURL + ";User=" + _username + ";Password="
					+ _password;
			// _username = null; _password = null;
			break;
		case _PGSQL:
			if (!_databaseURL.trim().startsWith("jdbc:postgresql:")) {
				_databaseURL = "jdbc:postgresql:" + _databaseURL;
			}
			break;
		case _MYSQL:
			if (!_databaseURL.trim().startsWith("jdbc:mysql:")) {
				_databaseURL = "jdbc:mysql:" + _databaseURL;
			}
			break;
		case _HSQL:
			if (!_databaseURL.trim().startsWith("jdbc:hsqldb:")) {
				_databaseURL = "jdbc:hsqldb:" + _databaseURL;
			}
			break;
		case _SQLite:
			if (!_databaseURL.trim().startsWith("jdbc:sqlite:"))
			{
				_databaseURL = "jdbc:sqlite:" + _databaseURL;
			}
			break;
		default:
			System.out.println(databaseFormat.getExpression()
					+ " is not supported");
		}
	}

	/** Get the value of a field in a database parameter token. */
	private static String _getParamField(RecordToken record, String name) {
		StringToken token = (StringToken) record.get(name);
		if (token == null) {
			return "";
		} else {
			return token.stringValue();
		}
	}

	/** Create a database parameter token. */
	private static RecordToken _createParamsToken(String driver, String url,
			String user, String passwd) throws IllegalActionException {

		return new RecordToken(_paramsLabels, new Token[] {
				new StringToken(driver), new StringToken(url),
				new StringToken(user), new StringToken(passwd) });
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	// An indicator for the db format.
	private _DBType _dbFormat;
	private String _databaseURL = "";
	private String _driverName = "";
	private String _username = "";
	private String _password = "";

	// Saving previous values to track changes.
	private String _prevDriver = "";
	private String _prevDBURL = "";
	private String _prevUser = "";
	private String _prevPasswd = "";

	// the database schema
	private String _schema = "";

	// database types
	private enum _DBType {
		_ORCL, _DB2, _LACCS, _MSSQL, _PGSQL, _MYSQL, _HSQL, _SQLite
	};

	/** The database parameter token labels. */
	private static String[] _paramsLabels = new String[] { "driver", "url",
			"user", "password" };
}
