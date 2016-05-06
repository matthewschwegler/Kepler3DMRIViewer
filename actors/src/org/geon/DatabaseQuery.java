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

package org.geon;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.ecoinformatics.seek.querybuilder.DBQueryDef;
import org.ecoinformatics.seek.querybuilder.DBQueryDefParserEmitter;
import org.ecoinformatics.seek.querybuilder.DBSchemaParserEmitter;
import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.QBTableauFactory;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DateToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.util.StringUtilities;

//////////////////////////////////////////////////////////////////////////
//// DatabaseQuery
/**
 * This actor performs database queries against a specific database. It accepts
 * a string query and a database connection reference as inputs. The actor
 * produces the output of the query in the user's selected output format,
 * specified by the outputType parameter, either as an XML, Record, string or a
 * in a relational form with no metadata. The user can also specify whether to
 * broadcast each row at a time or the whole result at once.
 * 
 * @author Efrat Jaeger
 * @version $Id: DatabaseQuery.java 32837 2014-07-14 22:26:04Z crawl $
 * @since Ptolemy II 3.0.2
 */
public class DatabaseQuery extends DatabaseAccessor {

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
	public DatabaseQuery(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Parameters
		outputType = new StringParameter(this, "outputType");
		outputType.setExpression("XML");
		_outputType = _XML;
		outputType.addChoice("XML");
		outputType.addChoice("record");
		outputType.addChoice("array");
		outputType.addChoice("string");
		outputType.addChoice("no metadata");
		outputType.addChoice("result set"); // send result set as is.

		// Ports
		query = new PortParameter(this, "query");
		query.setStringMode(true);

		result = new TypedIOPort(this, "result", false, true);

		_schemaAttr = new StringAttribute(this, "schemaDef");
		//TextStyle schemaDefTS = new TextStyle(_schemaAttr, "schemaDef");

		_sqlAttr = new StringAttribute(this, "sqlDef");
		_sqlAttr.setVisibility(Settable.NONE);
		//TextStyle sqlDefTS = new TextStyle(_sqlAttr, "sqlDef");

		outputEachRowSeparately = new Parameter(this,
				"outputEachRowSeparately", new BooleanToken(false));
		outputEachRowSeparately.setTypeEquals(BaseType.BOOLEAN);
		attributeChanged(outputEachRowSeparately);

		lowerColumnNames = new Parameter(this, "lowerColumnNames",
				new BooleanToken(false));
		lowerColumnNames.setTypeEquals(BaseType.BOOLEAN);

		// create tableau for editting the SQL String
		_qbTableauFactory = new QBTableauFactory(this, "_tableauFactory");

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
	 * The output format: XML, Record or String or a relational string with no
	 * metadata information.
	 */
	public StringParameter outputType;

	/**
	 * Specify whether to display the complete result at once or each row
	 * separately.
	 */
	public Parameter outputEachRowSeparately;

	/**
	 * An input query string.
	 */
	public PortParameter query;

	/**
	 * The query result.
	 */
	public TypedIOPort result;

	/**
	 * Hidden variable containing the xml representation of the query as
	 * returned by the query builder.
	 */
	public StringAttribute _sqlAttr = null;

	/**
	 * The schema of the database.
	 */
	public StringAttribute _schemaAttr = null;

	/** If true, column names are converted to lower-case. */
	public Parameter lowerColumnNames;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Determine the output format
	 * 
	 * @param attribute
	 *            The attribute that changed.
	 * @exception IllegalActionException
	 *                If the output type is not recognized.
	 */
	@Override
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		try {
			if (attribute == outputType) {
				String strOutputType = outputType.getExpression();
				if (strOutputType.equals("XML")) {
					_outputType = _XML;
				} else if (strOutputType.equals("record")) {
					_outputType = _RECORD;
				} else if (strOutputType.equals("array")) {
					_outputType = _ARR;
				} else if (strOutputType.equals("string")) {
					_outputType = _STR;
				} else if (strOutputType.startsWith("no")) {
					_outputType = _NOMD;
				} else if (strOutputType.startsWith("result")) {
					_outputType = _RS;
				} else {
					throw new IllegalActionException(this,
							"Unrecognized math function: " + strOutputType);
				}
			} else if (attribute == outputEachRowSeparately) {
				_separate = ((BooleanToken) outputEachRowSeparately.getToken())
						.booleanValue();
			} else if (attribute == _sqlAttr) {
				if (_sqlAttr != null && !_sqlAttr.equals("")) {
					String sqlXMLStr = ((Settable) _sqlAttr).getExpression();
					if (sqlXMLStr != null && !sqlXMLStr.equals("")){
						DBQueryDef queryDef = DBQueryDefParserEmitter
							.parseQueryDef(_schemaDef, sqlXMLStr);
						String sqlStr = DBQueryDefParserEmitter.createSQL(
							_schemaDef, queryDef);
						if (sqlStr != null) {
							query.setToken(new StringToken(sqlStr));
						}
					}
				}
			} else if (attribute == _schemaAttr) {
				String schemaDef = ((Settable) _schemaAttr).getExpression();
				if (schemaDef.length() > 0) {
					_schemaDef = DBSchemaParserEmitter
							.parseSchemaDef(schemaDef);
				}
			} else if (attribute == lowerColumnNames) {
				_lowerColumnNamesVal = ((BooleanToken) lowerColumnNames
						.getToken()).booleanValue();
			} else {
				super.attributeChanged(attribute);
			}
		} catch (Exception nameDuplication) {
			/*
			 * throw new InternalErrorException(this, nameDuplication,
			 * "Unexpected name duplication");
			 */
		}
	}

	/**
	 * Try to set the database schema once the database connection port has been
	 * connected.
	 */
	@Override
	public void connectionsChanged(Port port) {
		super.connectionsChanged(port);
		if (port == dbcon) {
			List conPortsList = dbcon.connectedPortList();
			Iterator conPorts = conPortsList.iterator();
			while (conPorts.hasNext()) {
				IOPort p = (IOPort) conPorts.next();
				if (p.isOutput() && p.getName().equals("dbcon")) {
					NamedObj container = p.getContainer();
					if (container instanceof OpenDBConnection) {
						String schema = "";
						try {
							schema = ((OpenDBConnection) container)
									.sendSchemaToConnected();
						} catch (IllegalActionException ex) {
							schema = "";
							System.out.println("IllegalActionException: "
									+ ex.getMessage());
						}
						if (!schema.equals("")) {
							try {
								_schemaAttr.setExpression(schema);
							} catch (IllegalActionException ex) {
								// unable to set schema attribute..
								System.out.println("IllegalActionException: "
										+ ex.getMessage());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Consume a query and a database connection reference. Compute the query
	 * result according to the specified output format.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
   @Override
   public void fire() throws IllegalActionException {

		super.fire();

		query.update();
		_query = ((StringToken) query.getToken()).stringValue();
		if (!_query.equals(_prevQuery) || query.getPort().getWidth() > 0) { // if
																			// this
																			// is
																			// a
																			// different
																			// query.
			_prevQuery = _query;
			try {
				Statement st = _db.createStatement();
				ResultSet rs;
				try {
					// System.out.println("going to exec query: " + _query);
					rs = st.executeQuery(_query);
				} catch (Exception e1) {
					throw new IllegalActionException(this, e1,
							"SQL executeQuery exception for query:" + _query);
				}

				switch (_outputType) {
				case _XML:
					_createXML(rs);
					break;
				case _RECORD:
					_createRecord(rs);
					break;
				case _ARR:
					_createArr(rs);
					break;
				case _STR:
					_createString(rs);
					break;
				case _NOMD:
					_createNoMetadata(rs);
					break;
				case _RS:
					_sendResultSet(rs);
					break;
				default:
					throw new InternalErrorException(
							"Invalid value for _outputType private variable. "
									+ "DatabaseQuery actor (" + getFullName()
									+ ")" + " on output type " + _outputType);
				}
				rs.close();
				st.close();
			} catch (SQLException e) {
				throw new IllegalActionException(this, "SQLException: "
						+ e.getMessage());
			}
		} else {
			// if the query comes only from the parameter and hasn't changed
			// don't refire.
			if (query.getPort().getWidth() == 0) {
				_refire = false;
			}
		}
	}

	/**
	 * Takes care of halting the execution in case the query is not updated from
	 * a port and hasn't changed.
	 */
	@Override
	public boolean postfire() throws IllegalActionException {
		if (!_refire)
			return false;

		return super.postfire();
	}

	/**
	 * Read the outputType parameter and set output type accordingly.
	 * 
	 * @exception IllegalActionException
	 *                If the file or URL cannot be opened, or if the first line
	 *                cannot be read.
	 */
	@Override
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();

		_prevQuery = "";
		_refire = true;

		// clear any existing constraints.
        result.typeConstraints().clear();
        result.setTypeEquals(BaseType.UNKNOWN);

		// Set the output type.
		switch (_outputType) {
		case _XML:
			result.setTypeEquals(BaseType.STRING);
			break;
		case _RECORD:
		    // set the type to GENERAL; downstream actors will need to cast
		    // into the appropriate record type
		    result.setTypeEquals(BaseType.GENERAL);
			break;
		case _ARR:
			result.setTypeEquals(new ArrayType(BaseType.STRING));
			break;
		case _STR:
			result.setTypeEquals(BaseType.STRING);
			break;
		case _NOMD:
			result.setTypeEquals(BaseType.STRING);
			break;
		case _RS:
			result.setTypeEquals(BaseType.GENERAL);
			break;
		default:
			throw new InternalErrorException(
					"Invalid value for _outputType private variable. "
							+ "DatabaseQuery actor (" + getFullName() + ")"
							+ " on output type " + _outputType);
		}
	}

	@Override
	public void wrapup() throws IllegalActionException {
		super.wrapup();
		_prevQuery = "";
		_refire = true;
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Send result set as is (separate is not applicable in this case).
	 */

	private void _sendResultSet(ResultSet rs) throws IllegalActionException {
		result.broadcast(new ObjectToken(rs));
	}

	/**
	 * Create a string result.
	 */
	private void _createString(ResultSet rs) throws IllegalActionException {
		try {
			ResultSetMetaData md = rs.getMetaData();
			String res = "";
			while (rs.next()) {
				for (int i = 1; i <= md.getColumnCount(); i++) {
					if (_lowerColumnNamesVal) {
						res += md.getColumnName(i).toLowerCase() + ": ";
					} else {
						res += md.getColumnName(i) + ": ";
					}
					String val = rs.getString(i);
					if (val == null)
						res += "";
					else
						res += val;
					res += " ;  ";
				}
				if (_separate) {
					result.broadcast(new StringToken(res));
					res = "";
				} else {
					res += "\n";
				}
			}
			if (!_separate) {
				result.broadcast(new StringToken(res));
			}
			rs.close();
		} catch (Exception ex) {
			throw new IllegalActionException(this, ex,
					"exception in create String result");
		}
	}

	/**
	 * Create result as an array of string. (due to problems with record array)
	 */
	private void _createArr(ResultSet rs) throws IllegalActionException {
		try {
		  boolean hasResult = false;
			Vector results = new Vector();
			Token resultTokens[] = null;
			ResultSetMetaData md = rs.getMetaData();
			while (rs.next()) {
			  hasResult = true;
				String res = "";
				for (int i = 1; i <= md.getColumnCount(); i++) {
					String val = rs.getString(i);
					if (val == null)
						res += ",";
					else {
					  val = val.replace(',', '%');
					  res += val + ",";
					}
					
				}
				
				// remove last comma.
				int lstCmaInd = res.lastIndexOf(",");
				if (lstCmaInd > -1) {
					res = res.substring(0, lstCmaInd);
				}
				if (_separate) {
					resultTokens = new Token[1];
					resultTokens[0] = new StringToken(res);
					result.broadcast(new ArrayToken(resultTokens));
				} else {
					results.add(new StringToken(res));
				}
			}
			
			if(!hasResult){
        //sent an empty array token if there is no result
        result.broadcast(new ArrayToken(BaseType.STRING));
        return;
      }
			
			if (!_separate) {
			
					resultTokens = new Token[results.size()];
					results.toArray(resultTokens);
					result.broadcast(new ArrayToken(resultTokens));
				
			}
			rs.close();
		} catch (Exception ex) {
			throw new IllegalActionException(this, ex,
					"exception in create String result");
		}
	}

	/**
	 * Create an XML stream result.
	 */
	private void _createXML(ResultSet rs) throws IllegalActionException {
		try {
			String tab = "    ";
			String finalResult = "<?xml version=\"1.0\"?> \n";
			finalResult += "<result> \n";
			ResultSetMetaData md = rs.getMetaData();

			int colNum = md.getColumnCount();
			String tag[] = new String[colNum]; // holds all the result tags.
			for (int i = 0; i < colNum; i++) {
				if (_lowerColumnNamesVal) {
					tag[i] = md.getColumnName(i + 1).toLowerCase();
				} else {
					tag[i] = md.getColumnName(i + 1);
				}
				tag[i] = tag[i].replace(' ', '_');
				if (tag[i].startsWith("#")) {
					tag[i] = tag[i].substring(1);
				}

				// when joining two or more tables that have the same columns
				// we'd like to distinguish between them.
				int count = 1;
				int j;
				while (true) { // if the same tag appears more then once add an
								// incremental index to it.
					for (j = 0; j < i; j++) {
						if (tag[i].equals(tag[j])) { // the new tag already
														// exist
							if (count == 1) { // first duplicate
								tag[i] = tag[i] + count;
							} else {
								int tmp = count - 1;
								String strCnt = "" + tmp;
								int index = tag[i].lastIndexOf(strCnt);
								tag[i] = tag[i].substring(0, index); // remove
																		// the
																		// prev
																		// index.
								tag[i] = tag[i] + count;
							}
							count++;
							break;
						}
					}
					if (j == i) {// the tag was not found in existing tags.
						count = 1;
						break;
					}
				}
			}

			while (rs.next()) {
				String res = tab + "<row> \n";

				for (int i = 0; i < colNum; i++) {
					String val = rs.getString(i + 1);
					res += tab + tab;
					if (val == null) {
						// res += "<" + tag[i] + "/>\n";
						res += "<" + StringUtilities.escapeForXML(tag[i])
								+ "/>\n";
					} else {
						res += "<" + StringUtilities.escapeForXML(tag[i]) + ">"
								+ StringUtilities.escapeForXML(val) + "</"
								+ StringUtilities.escapeForXML(tag[i]) + ">\n";
					}
				}
				res += tab + "</row> \n";

				if (_separate) {
					finalResult += res + "</result>";
					result.broadcast(new StringToken(finalResult));
					finalResult = "<?xml version=\"1.0\"?> \n";
					finalResult += "<result>\n";
				} else {
					finalResult += res;
				}
			}
			if (!_separate) {
				finalResult += "</result>";
				result.broadcast(new StringToken(finalResult));
			}
			rs.close();
		} catch (Exception ex) {
			throw new IllegalActionException(this, ex,
					"exception in create XML stream");
		}
	}

	/** Create a record result. */
	private void _createRecord(ResultSet rs) throws IllegalActionException,
			SQLException {

		LinkedList<Token> outList = null;

		if (!_separate) {
			outList = new LinkedList<Token>();
		}

		ResultSetMetaData md = rs.getMetaData();
		int colNum = md.getColumnCount();
		String labels[] = new String[colNum];
		Type types[] = new Type[colNum];
		for (int i = 1; i <= colNum; i++) {
			if (_lowerColumnNamesVal) {
				labels[i - 1] = md.getColumnName(i).toLowerCase();
			} else {
				labels[i - 1] = md.getColumnName(i);
			}
			types[i - 1] = _convertTypeFromSQLType(md.getColumnType(i));
		}

		Token values[] = new Token[colNum];
		while (rs.next()) {
			for (int i = 1; i <= colNum; i++) {
				values[i - 1] = _makeTokenFromResultSet(i, rs, md
						.getColumnType(i));
			}

			Token token = new RecordToken(labels, values);
			if (_separate) {
				result.broadcast(token);
			} else {
				outList.add(token);
			}
		}
		rs.close();

		if (!_separate) {
			ArrayToken arrayToken = null;

			Token[] array = outList.toArray(new Token[0]);
			if (array.length == 0) {
				arrayToken = new ArrayToken(new RecordType(labels, types));
			} else {
				arrayToken = new ArrayToken(array);
			}
			result.broadcast(arrayToken);
		}
	}

	private static Type _convertTypeFromSQLType(int sqlType) {
		Type retval = null;
		if (sqlType == Types.ARRAY) {

		} else {
			retval = _sqlTypeMap.get(sqlType);
		}

		if (retval == null) {
			System.out.println("WARNING: unhandled sql type: " + sqlType);
		}
		return retval;
	}

	/** Create a token from a single SQL row. */
	private static Token _makeTokenFromResultSet(int i, ResultSet rs,
			int sqlType) throws IllegalActionException, SQLException {
		Token retval = null;

		switch (sqlType) {
		case Types.INTEGER:
			retval = new IntToken(rs.getInt(i));
			break;
		case Types.DOUBLE:
			retval = new DoubleToken(rs.getDouble(i));
			break;
		case Types.TIMESTAMP:
			retval = new DateToken(rs.getTimestamp(i).getTime());
			break;
		case Types.VARCHAR:
			retval = new StringToken(rs.getString(i));
			break;
		case Types.ARRAY:
			retval = _makeArrayTokenFromSQLArray(rs.getArray(i));
			break;
		case Types.REAL:
			retval = new DoubleToken(rs.getDouble(i));
			break;
		case Types.FLOAT:
			retval = new DoubleToken(rs.getDouble(i));
			break;
		default:
			System.out.println("WARNING: unhandled sql type: " + sqlType);
			retval = new StringToken(rs.getString(i));
			break;
		}
		return retval;
	}

	/** Create an array token from an SQL array. */
	private static ArrayToken _makeArrayTokenFromSQLArray(Array array)
			throws IllegalActionException, SQLException {
		ResultSet rsArray = array.getResultSet();

		// count the number of elements
		int len = 0;
		if (rsArray.first()) {
			len = 1;
			while (rsArray.next()) {
				len++;
			}
		}

		// XXX check for len = 0

		rsArray.first();
		Token[] tokens = new Token[len];
		int i = 0;

		switch (array.getBaseType()) {
		case Types.INTEGER:
			do {
				tokens[i++] = new IntToken(rsArray.getInt(2));
			} while (rsArray.next());
			break;
		case Types.DOUBLE:
			do {
				tokens[i++] = new DoubleToken(rsArray.getDouble(2));
			} while (rsArray.next());
			break;
		default:
			System.out.println("WARNING: unhandle sql array type: "
					+ array.getBaseTypeName());
			do {
				tokens[i++] = new StringToken(rsArray.getString(2));
			} while (rsArray.next());
			break;
		}
		return new ArrayToken(tokens);
	}

	/** Create a tabular form result string with no metadata information. */
	private void _createNoMetadata(ResultSet rs) throws IllegalActionException {
		try {
			ResultSetMetaData md = rs.getMetaData();
			int colNum = md.getColumnCount();
			String res = "";
			while (rs.next()) {
				String currRow = "";
				for (int i = 1; i <= colNum; i++) {
					String currVal = rs.getString(i);
					if (currVal == null || currVal.equals("")) {
						int type = md.getColumnType(i);
						if (type == Types.CHAR || type == Types.VARCHAR) {
							currVal = "-";
						} else
							currVal = "-1";
					}
					currVal = currVal.replace(' ', '_');
					currRow += currVal;

					// for display purposes.
					int colWidth = md.getColumnDisplaySize(i);
					int numSpace = colWidth - currVal.length();
					for (int j = 0; j < numSpace; j++) {
						currRow += " ";
					}
				}
				if (_separate) {
					result.broadcast(new StringToken(currRow));
				} else {
					res += currRow + "\n";
				}
			}
			if (!_separate) {
				// remove the last carriage return.
				int lastCRInd = res.lastIndexOf("\n");
				if (lastCRInd > -1) {
					res = res.substring(0, lastCRInd);
				}
				result.broadcast(new StringToken(res));
			}
			rs.close();
		} catch (Exception ex) {
			throw new IllegalActionException(this, ex,
					"exception in create custom result");
		}
	}

	private static HashMap<Integer, Type> _sqlTypeMap;

	static {

		_sqlTypeMap = new HashMap();

		_sqlTypeMap.put(Types.DOUBLE, BaseType.DOUBLE);
		_sqlTypeMap.put(Types.INTEGER, BaseType.INT);
		_sqlTypeMap.put(Types.VARCHAR, BaseType.STRING);
		_sqlTypeMap.put(Types.TIMESTAMP, BaseType.DATE);
		_sqlTypeMap.put(Types.REAL, BaseType.DOUBLE);
		_sqlTypeMap.put(Types.FLOAT, BaseType.FLOAT);
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	/** Output indicator parameter. */
	private int _outputType;

	/** Output indicator parameter. */
	private boolean _separate;

	/** Query string. */
	private String _query;

	/** Previously queried query.. */
	private String _prevQuery = "";

	/** Refire flag. */
	private boolean _refire = true;

	/** Query builder tableau factory. */
	protected QBTableauFactory _qbTableauFactory = null;

	/** Schema definition interface, used by the query builder */
	protected DSSchemaIFace _schemaDef = null;

	// Constants used for more efficient execution.
	private static final int _XML = 0;
	private static final int _RECORD = 1;
	private static final int _STR = 2;
	private static final int _NOMD = 3;
	private static final int _ARR = 4;
	private static final int _RS = 5;

	/** If true, use lower-case column names. */
	private boolean _lowerColumnNamesVal = false;
}