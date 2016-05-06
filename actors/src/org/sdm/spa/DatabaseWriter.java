/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-07-14 15:26:04 -0700 (Mon, 14 Jul 2014) $' 
 * '$Revision: 32837 $'
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

package org.sdm.spa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;

import org.geon.DatabaseAccessor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.DateToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.MatrixToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.XMLToken;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// DatabaseWriter

/**
 * This actor performs database updates and returns the number of updated rows.
 * If <i>input</i> is an SQL string, the SQL is run on the database.
 * <p>
 * If the input is a record token, the name-value pairs are inserted into the
 * table specfied by <i>table</i>. Optionally, auto-increments for a column will
 * be done when <i>autoIncColumnName</i> is specified; the incremented value is
 * output in <i>autoIncValue</i>.
 * </p>
 * 
 * @author Yang Zhao, Daniel Crawl
 * @version $Id: DatabaseWriter.java 32837 2014-07-14 22:26:04Z crawl $
 */

public class DatabaseWriter extends DatabaseAccessor {

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
	public DatabaseWriter(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		input = new PortParameter(this, "input");

		table = new PortParameter(this, "table");
		table.setStringMode(true);
		table.getPort().setTypeEquals(BaseType.STRING);

		autoIncColumnName = new PortParameter(this, "autoIncColumnName");
		autoIncColumnName.setStringMode(true);
		autoIncColumnName.getPort().setTypeEquals(BaseType.STRING);

		autoIncValue = new TypedIOPort(this, "autoIncValue", false, true);
		autoIncValue.setTypeEquals(BaseType.INT);

		// the output
		result = new TypedIOPort(this, "result", false, true);
		result.setTypeEquals(BaseType.INT);

		_domBuilder = new DOMBuilder();
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** The input for the update. */
	public PortParameter input;

	/** The number of rows successfully updated. */
	public TypedIOPort result;

	/** Name of table. */
	public PortParameter table;

	/** Name of column to auto-increment. */
	public PortParameter autoIncColumnName;

	/** Auto-increment value. */
	public TypedIOPort autoIncValue;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Read a string token for the query from the input port, execute it on the
	 * database and output the query result.
	 * 
	 * @exception IllegalActionException
	 *                If there is error to execute the query or if the base
	 *                class throw it.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		String cmd = "";
		int incVal = -1;

		try {
			// read the input
			input.update();
			Token token = input.getToken();
			Type type = token.getType();

			if (_debugging) {
				_debug("input: " + token);
			}

			// see what kind of input it is
			if (type == BaseType.STRING) {
				cmd = ((StringToken) token).stringValue();
			} else if (type == BaseType.XMLTOKEN
					|| token instanceof RecordToken) {
				LinkedHashMap<String, String> map;
				String tableName;

				if (type == BaseType.XMLTOKEN) {
					org.w3c.dom.Document d = ((XMLToken) token).getDomTree();
					Document doc = _domBuilder.build(d);
					Element root = doc.getRootElement();
					tableName = root.getName();
					map = _parseXMLCmd(doc);
				} else // if(token instanceof RecordToken)
				{
					table.update();
					tableName = ((StringToken) table.getToken()).stringValue();
					map = _parseRecordCmd((RecordToken) token);
				}

				if (tableName.equals("")) {
					throw new IllegalActionException(this,
							"No value for required table name");
				}

				// check for auto increment column name
				incVal = _checkAutoIncColumn(tableName, map);

				cmd = _convertMapToSQLInsert(tableName, map);

			} else {
				throw new IllegalActionException(this,
						"Unknown type of token in updateSQL: " + type);
			}

			if (_debugging) {
				_debug("sql command: " + cmd);
			}

			Statement st = _db.createStatement();
			int nresult = st.executeUpdate(cmd);
			st.close();
			result.broadcast(new IntToken(nresult));

			if (incVal > -1) {
				autoIncValue.broadcast(new IntToken(incVal));
			}
		} catch (SQLException e) {
			throw new IllegalActionException(this, e,
					"failed to execute the command: " + cmd);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/** Convert an XML document into a map. */
	private LinkedHashMap<String, String> _parseXMLCmd(Document doc) {
		LinkedHashMap<String, String> retval = new LinkedHashMap();
		Element root = doc.getRootElement();
		List l = root.getChildren();
		for (int i = 0; i < l.size(); i++) {
			Element child = (Element) l.get(i);
			retval.put(child.getName(), child.getText());
		}
		return retval;
	}

	/** Convert a record token into a map. */
	private LinkedHashMap<String, String> _parseRecordCmd(RecordToken token) {
		LinkedHashMap<String, String> retval = new LinkedHashMap();
		Object labels[] = token.labelSet().toArray();

		for (int i = 0; i < labels.length; i++) {
			String str = (String) labels[i];
			Token val = token.get(str);
			String value = _convertTokenToSQL(val);
			if (value != null){
				retval.put(str, value);
			}
			else{
				retval.put(str, null);
			}
		}

		return retval;
	}

	/** Convert a token's value to an SQL string. */
	private String _convertTokenToSQL(Token token) {
		String retval = null;

		if (token.isNil()){
			return null;
		}
		else if (token instanceof StringToken) {
			retval = ((StringToken) token).stringValue();
		} else if ((token instanceof IntToken)
				|| (token instanceof DoubleToken)) {
			retval = token.toString();
		} else if (token instanceof ptolemy.data.DateToken) {
			long ms = ((DateToken) token).getValue();
			java.sql.Timestamp ts = new java.sql.Timestamp(ms);
			retval = ts.toString();
		} else if (token instanceof ArrayToken) {
			ArrayToken array = (ArrayToken) token;
			StringBuffer buf = new StringBuffer("{");
			for (int i = 0; i < array.length(); i++) {
				String str = _convertTokenToSQL(array.getElement(i));
				buf.append(str + ", ");
			}
			// remove the last comma and space
			buf = buf.delete(buf.length() - 2, buf.length());

			buf.append("}");
			retval = buf.toString();
			if (retval.equals("{}")){
				return null;
			}
		} else if (token instanceof MatrixToken) {
			ArrayToken array = MatrixToken.matrixToArray((MatrixToken) token);
			retval = _convertTokenToSQL(array);
		} else {
			System.out.println("WARNING: unhandled token type "
					+ "converted to sql: " + token.getType());
			retval = token.toString();
		}

		return retval;
	}

	/**
	 * Convert a map of key-values into a SQL insert string.
	 * 
	 * @param tableName
	 *            the name of the table
	 * @param map
	 *            the map of column names and values
	 * @return an SQL insert string
	 */
	private String _convertMapToSQLInsert(String tableName,
			LinkedHashMap<String, String> map) {
		StringBuffer retval = new StringBuffer();

		retval.append("INSERT INTO " + tableName + " (");

		for (String name : map.keySet()) {
			retval.append(name + ", ");
		}

		// remove the last comma and space
		retval = retval.delete(retval.length() - 2, retval.length());

		retval.append(") VALUES (");

		for (String name : map.keySet()) {
			if (map.get(name) == null){
				retval.append(null + ", ");
			}
			else{
				retval.append("'" + map.get(name) + "', ");
			}
		}

		// remove the last comma and space
		retval = retval.delete(retval.length() - 2, retval.length());

		retval.append(")");

		return retval.toString();
	}

	/**
	 * Perform an auto-increment. Find the largest value in the column
	 * <i>colName</i>, return this value incremented by one and plce it into the
	 * map.
	 */
	private int _checkAutoIncColumn(String tableName,
			LinkedHashMap<String, String> map) throws IllegalActionException,
			SQLException {
		int retval = -1;
		autoIncColumnName.update();
		String colName = ((StringToken) autoIncColumnName.getToken())
				.stringValue();

		// see if auto-inc column name was given
		if (colName.length() > 0) {
			String cmd = "SELECT max(" + colName + ") from " + tableName;
			Statement st = _db.createStatement();
			ResultSet rs = st.executeQuery(cmd);
			int val = 0;

			// see if it exists
			if (rs.next()) {
				val = rs.getInt(1) + 1;
			}

			rs.close();
			st.close();

			retval = val;

			map.put(colName, String.valueOf(val));
		}

		return retval;
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Helper object to convert from w3c Document to jdom Document */
	private DOMBuilder _domBuilder = null;
}