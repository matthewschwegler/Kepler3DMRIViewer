/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2010-08-13 12:10:37 -0700 (Fri, 13 Aug 2010) $' 
 * '$Revision: 25362 $'
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

package org.ecoinformatics.seek.datasource.darwincore;

import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecord;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecordReturnField;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructure;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructureReturnField;
import org.ecoinformatics.ecogrid.queryservice.util.EcogridQueryParser;
import org.ecoinformatics.seek.dataquery.DBTableNameResolver;
import org.ecoinformatics.seek.dataquery.DBTablesGenerator;
import org.ecoinformatics.seek.dataquery.HsqlDataQueryAction;
import org.ecoinformatics.seek.datasource.DataSourceIcon;
import org.ecoinformatics.seek.datasource.EcogridDataCacheItem;
import org.ecoinformatics.seek.datasource.EcogridQueryDataCacheItem;
import org.ecoinformatics.seek.datasource.eml.eml2.Eml200DataSource;
import org.ecoinformatics.seek.ecogrid.quicksearch.ResultTreeRoot;
import org.ecoinformatics.seek.querybuilder.DBQueryDef;
import org.ecoinformatics.seek.querybuilder.DBQueryDefParserEmitter;
import org.ecoinformatics.seek.querybuilder.DBSchemaParserEmitter;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.DataCacheListener;
import org.kepler.objectmanager.cache.DataCacheManager;
import org.kepler.objectmanager.cache.DataCacheObject;
import org.kepler.objectmanager.data.DataType;
import org.kepler.objectmanager.data.db.DSSchemaDef;
import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableDef;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;
import org.kepler.objectmanager.data.db.Entity;
import org.kepler.objectmanager.data.db.QBTableauFactory;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.gui.style.TextStyle;
import ptolemy.actor.lib.Source;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.vergil.basic.KeplerDocumentationAttribute;
import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * A ResultRecord that is in EML 2.0.0 format.
 */
public class DarwinCoreDataSource extends Source implements DataCacheListener {
	static final String DATATYPES[] = { DataType.INT, DataType.FLOAT,
			DataType.DOUBLE, DataType.LONG, DataType.STR };
	static final BaseType BASETYPES[] = { BaseType.INT, BaseType.DOUBLE,
			BaseType.DOUBLE, BaseType.LONG, BaseType.STRING };
	static private Hashtable mTypeHash = new Hashtable();
	static private Hashtable mType2IntHash = new Hashtable();

	static final Hashtable asTablePorts = new Hashtable();
	static final Hashtable asRowPorts = new Hashtable();

	private static DSSchemaIFace _darwinCoreSchema = null;

	// Constants used for more efficient execution.
	private static final int _ASTABLE = 1;
	private static final int _ASROW = 2;
	private static final int _ASFIELD = 3;

	protected static final String DATATABLE = "DataTable";
	protected static final String DELIMITER = "Delimiter";
	protected static final String NUMCOLUMNS = "numColumns";
	protected static final String DATAROW = "DataRow";
	protected static final String ROWDELIM = "\r\n";
	protected static final String COLDELIM = "\t";

	protected static final String ENDPOINT_ATTR = "endPoint";
	protected static final String SEARCHDATA_ATTR = "searchData";
	protected static final String OUTPUTTYPE_ATTR = "outputType";

	private static Log log;
	private static boolean isDebugging;

	// for looking up the default documentation from the actor lib
	private static KeplerDocumentationAttribute defaultDocumentation = null;

	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.datasource.darwincore.DarwinCoreDataSource");
		isDebugging = log.isDebugEnabled();

		asTablePorts.put(DATATABLE, BaseType.STRING);
		asTablePorts.put(DELIMITER, BaseType.STRING);
		asTablePorts.put(NUMCOLUMNS, BaseType.INT);

		asRowPorts.put(DATAROW, BaseType.STRING);
		asRowPorts.put(DELIMITER, BaseType.STRING);
		asRowPorts.put(NUMCOLUMNS, BaseType.INT);

	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	public QBTableauFactory _qbTableauFactory = null;

	/**
	 * Output indicator parameter.
	 */
	private int _outputType = _ASFIELD;
	private Entity _tableEntity = null;
	private int _recordCount = 0;

	private Vector _columns = null;
	private Vector _colTypes = new Vector();

	private DBQueryDef _queryDef = null;
	private DSSchemaIFace _schemaDef = null;
	private DSSchemaIFace _resultsetSchemaDef = null;

	private boolean _ignoreSchemaChange = false;
	private boolean _ignoreSearchChange = true;
	private boolean _ignoreSqlChange = false;

	private DBTablesGenerator _tableGenerator = null;

	/**
	 * _cacheDataItem is the DataCacheObject which contains the raw resultset
	 * returned from the query.
	 */
	private EcogridQueryDataCacheItem _cachedDataItem = null;

	/**
	 * _downloadCompleted is a Latch object (from oswego) which is used to
	 * synchronize the start of the workflow. The Latch is released when the
	 * _cacheDataItem has been completed. It is also used to notify a thread
	 * blocked in initialized() when the stop method is executed.
	 */
	private Latch _downloadCompleted = new Latch();

	/**
	 * _tableDataCache is the DataCacheObject which contains the TEXT Table
	 * data.
	 */
	private EcogridDataCacheItem _tableDataCache = null;

	private Vector[] _dataVectors = null;

	private DataSourceIcon _icon;

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////
	public TypedIOPort speciesName = new TypedIOPort(this, "speciesName", true,
			false);

	/**
	 * The SQL command to operate in data file
	 */
	public StringAttribute _sqlAttr = null;
	/**
	 * Schema defination of data file
	 */
	public StringAttribute _schemaAttr = null;
	/**
	 * The type of output for data entity. It includes: As Field: This is
	 * default value. The output port will be the fields in eml package and
	 * output port type will be decided by metadata. User also can use SQL
	 * command to select data column. As Table: The selected entity will be send
	 * out as a string which contains whole entity data. It has tree output
	 * ports: DataTable - data itself, Delimiter - delimiter to seperate fields
	 * and NumColumns - the number of fields in the table. As Row: In this
	 * output type, one row of selected column data will be sent out as string.
	 * It has tree output ports: DataRow - data itself, Delimiter - delimiter to
	 * seperate fields and NumColumns - the number of fields in the table.
	 */
	public StringParameter _outputTypeAttr = null;
	/**
	 * The ecogrid query services url
	 */
	public StringAttribute _endPoint = null;
	/**
	 * The value which were used for search
	 */
	// public StringAttribute _searchData = null;
	public StringParameter _searchData = null;

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
	 * @since
	 */
	public DarwinCoreDataSource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		_icon = new DataSourceIcon(this);

		// _attachText("_iconDescription", getReadyIcon());

		_searchData = new StringParameter(this, SEARCHDATA_ATTR);
		_endPoint = new StringAttribute(this, ENDPOINT_ATTR);

		_outputTypeAttr = new StringParameter(this, "outputType");
		_outputTypeAttr.setExpression("As Table");
		_outputTypeAttr.addChoice("As Field");
		_outputTypeAttr.addChoice("As Table");
		_outputTypeAttr.addChoice("As Row");
		_outputType = _ASTABLE;

		_schemaAttr = new StringAttribute(this, "schemaDef");
		TextStyle schemaDefTS = new TextStyle(_schemaAttr, "schemaDef");
		_schemaAttr.setVisibility(Eml200DataSource.DEFAULTFORSCHEMA);

		_sqlAttr = new StringAttribute(this, "sqlDef");
		TextStyle sqlDefTS = new TextStyle(_sqlAttr, "sqlDef");
		_sqlAttr.setVisibility(Eml200DataSource.DEFAULTFORSQL);

		_searchData.setPersistent(true);
		_endPoint.setPersistent(true);
		_outputTypeAttr.setPersistent(true);
		_schemaAttr.setPersistent(true);
		_sqlAttr.setPersistent(true);

		// create tableau for editting the SQL String
		_qbTableauFactory = new QBTableauFactory(this, "_tableauFactory");

		if (mTypeHash.size() == 0) {
			for (int i = 0; i < DATATYPES.length; i++) {
				mTypeHash.put(DATATYPES[i], BASETYPES[i]);
				mType2IntHash.put(DATATYPES[i], new Integer(i));
			}
		}

		// Get the Schema for All DarwinCoreDataSources
		if (_darwinCoreSchema == null) {
			_darwinCoreSchema = DarwinCoreSchema.getInstance().getSchema();
		}

	}

	/**
	 * Generic StringAttribute Setter
	 * 
	 * @param aStrAttr
	 *            the StringAttribute object being set
	 * @param aVal
	 *            the value being set
	 * @param aName
	 *            name of attr being set
	 */
	protected void setStrAttr(StringAttribute aStrAttr, String aVal,
			String aName) {
		try {
			aStrAttr.setExpression(aVal);
		} catch (IllegalActionException iae) {
			System.err.println("Could not set " + aName + " with value[" + aVal
					+ "]");
		}
	}

	/**
	 * Generic StringAttribute Getter
	 * 
	 * @param aStrAttr
	 *            the StringAttribute object being gotten
	 * @param aName
	 *            name of attr being gotten
	 * 
	 * @return value of attr
	 */
	private String getStrAttr(StringAttribute aStrAttr, String aName) {
		String value = null;
		try {
			value = aStrAttr.getExpression();
		} catch (Exception e) {
			System.err.println("Error getting " + aName + " attribute.");
		}
		return value;
	}

	/**
	 * Set the endpoint of this record. The endpoint indicates where the service
	 * generating the record can be accessed.
	 * 
	 * @param aVal
	 *            the URL of the service that contains the record
	 */
	public void setEndpoint(String aVal) {
		setStrAttr(_endPoint, aVal, ENDPOINT_ATTR);
	}

	/**
	 * Get the endpoint of this record. The endpoint indicates where the service
	 * generating the record can be accessed.
	 * 
	 * @return endpoint the URL of the service that contains the record
	 */
	public String getEndpoint() {

		return getStrAttr(_endPoint, ENDPOINT_ATTR);
	}

	/**
	 * Set the searchData of this record.
	 * 
	 * @param aVal
	 *            the URL of the service that contains the record
	 */
	public void setSearchData(String aVal) {
		_searchData.setExpression(aVal);
	}

	/**
	 * Get the searchData of this record.
	 * 
	 * @return endpoint the URL of the service that contains the record
	 */
	public String getSearchData() {
		String value = null;
		try {
			value = _searchData.stringValue();
		} catch (Exception e) {
			System.err.println("Error getting searchData attribute.");
		}
		return value;
	}

	/**
	 * Initialize the actor prior to running in the workflow. This reads the
	 * metadata and configures the ports.
	 * 
	 * @throws IllegalActionException
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_recordCount = 0;
	}

	private void initial_setup() throws IllegalActionException {
		try {
			_downloadCompleted.acquire();
			log.debug("Is stop requested? " + getDirector().isStopRequested());
		} catch (InterruptedException e) {
			log.debug("Is stop requested? " + getDirector().isStopRequested());
			if (getDirector().isStopRequested()) {
				throw new IllegalActionException("Execution interrupted");
			}
		}
		String sqlStr = "";

		String sqlXMLStr = ((Settable) _sqlAttr).getExpression();
		if (sqlXMLStr == null || sqlXMLStr.length() == 0) {
			sqlStr = "SELECT * FROM "
					+ (_tableEntity.getMappedName() != null ? _tableEntity
							.getMappedName() : _tableEntity.getName());

		} else {
			Hashtable mappedNameHash = new Hashtable();
			if (_tableEntity.getMappedName() != null) {
				mappedNameHash.put(_tableEntity.getName(), _tableEntity
						.getMappedName());
			}
			DBQueryDef queryDef = DBQueryDefParserEmitter.parseQueryDef(
					_schemaDef, sqlXMLStr, mappedNameHash);
			_columns = queryDef.getSelects();
			sqlStr = DBQueryDefParserEmitter.createSQL(_schemaDef, queryDef);
		}

		// excuted query
		if (sqlStr != null && !sqlStr.trim().equals("")) {
			// if table gnerated successfully, we will run query
			if (_tableGenerator != null && _tableGenerator.getSuccessStatus()) {
				try {
					_icon.setBusy();
					HsqlDataQueryAction queryAction = new HsqlDataQueryAction();
					queryAction.setSQL(sqlStr);
					queryAction.actionPerformed(null);
					ResultSet result = queryAction.getResultSet();
					_dataVectors = transformResultSet(result);
					_icon.setReady();
				} catch (Exception e) {
					log.debug("Error to run query is ", e);
					throw new IllegalActionException(e.getMessage());
				}

			}
		}
	}

	/*
	 * Method to transform resultset to a vector array data. A vector is a row
	 * and data type is string.
	 */
	private Vector[] transformResultSet(ResultSet aResultset)
			throws SQLException {
		if (aResultset == null) {
			System.err.println("Resultset is NULL in transformResultSet.");
			return null;
		}
		ResultSetMetaData metadata = aResultset.getMetaData();
		if (metadata == null) {
			System.err.println("Metadata is NULL for resultset.");
			return null;
		}
		int columnSize = metadata.getColumnCount();

		Vector data = new Vector();
		while (aResultset.next()) {
			Vector rowData = new Vector();
			for (int i = 0; i < columnSize; i++) {
				String str = aResultset.getString(i + 1);
				rowData.add(str);
			}
			data.add(rowData);
		}
		Vector[] dataArray = null;
		dataArray = transformVectorToArray(data);
		return dataArray;
	}

	/**
	 * 
	 * @param vector
	 * 	 */
	private Vector[] transformVectorToArray(Vector vector) {
		if (vector == null) {
			return null;
		}
		int size = vector.size();
		Vector[] array = new Vector[size];
		for (int i = 0; i < size; i++) {
			array[i] = (Vector) vector.elementAt(i);
		}
		return array;
	}

	/**
	 * Checks for a valid string for a numerical type, empty is NOT valid if not
	 * valid then return the MAX_VALUE for the number
	 * 
	 * @param aValue
	 *            the value to be checked
	 * @param aType
	 *            the type of number it is
	 * @return the new value
	 */
	private String getValidStringValueForType(String aValue, String aType) {
		String valueStr = aValue;
		Integer iVal = (Integer) mType2IntHash.get(aType);
		switch (iVal.intValue()) {
		case 0:
			try {
				Integer.parseInt(aValue);
			} catch (Exception e) {
				valueStr = Integer.toString(Integer.MAX_VALUE);
			}
			break;

		case 1:
			try {
				Float.parseFloat(aValue);
			} catch (Exception e) {
				valueStr = Float.toString(Float.MAX_VALUE);
			}
			break;

		case 2:
			try {
				Double.parseDouble(aValue);
			} catch (Exception e) {
				valueStr = Double.toString(Double.MAX_VALUE);
			}
			break;

		case 3:
			try {
				Long.parseLong(aValue);
			} catch (Exception e) {
				valueStr = Long.toString(Long.MAX_VALUE);
			}
			break;

		case 4:
			// no-op
			break;
		}
		return valueStr;
	}

	/**
	 * Append a header row describing the columns
	 * 
	 * @param aStrBuf
	 * @param aColumns
	 * @param aDelim
	 */
	private void appendHeaderRow(StringBuffer aStrBuf, Vector aColumns,
			String aColDelim, String aRowDelim) {
		if (aStrBuf != null && aColumns != null) {
			StringBuffer headerRow = new StringBuffer();
			Enumeration colEnum = aColumns.elements();
			while (colEnum.hasMoreElements()) {
				DSTableFieldIFace colDef = (DSTableFieldIFace) colEnum
						.nextElement();
				if (headerRow.length() > 0) {
					headerRow.append(aColDelim);
				}
				headerRow.append(colDef.getName());
			}
			aStrBuf.append(headerRow);
			aStrBuf.append(aRowDelim);
		}
	}

	/**
	 * Creates a string table from the resultset records
	 * 
	 * @param aRS
	 * 	 */
	private String createTableFromResultset(String delim,
			ResultsetTypeRecord[] records) {
		StringBuffer tableStr = new StringBuffer();
		StringBuffer rowOfData = new StringBuffer();
		DSTableIFace tableSchema = (DSTableIFace) _resultsetSchemaDef
				.getTables().elementAt(0);
		Vector columns = tableSchema.getFields();

		appendHeaderRow(tableStr, columns, delim, ROWDELIM);

		for (int i = 0; i < records.length; i++) {
			rowOfData.setLength(0);

			//
			// Note: this code assumes all the columns are returned for every
			// record.
			// It also assumes they are in the same order as in the metadata.
			Enumeration colEnum = columns.elements();
			int colInx = 0;
			while (colEnum.hasMoreElements()) {
				DSTableFieldIFace colDef = (DSTableFieldIFace) colEnum
						.nextElement();
				ResultsetTypeRecordReturnField field = records[i]
						.getReturnField(colInx);
				String eleStr = field.get_value();
				if (colInx > 0) {
					rowOfData.append(delim);
				}
				rowOfData.append(getValidStringValueForType(eleStr, colDef
						.getDataType()));
				colInx++;
			}
			tableStr.append(rowOfData);
			tableStr.append(ROWDELIM);
		}
		return tableStr.toString();
	}

	/**
	 * Creates a string table from the resultset records
	 * 
	 * @param aRS
	 * 	 */
	private String createTableFromSQLResults(boolean aIncludeHeader,
			String aDelim) {
		StringBuffer tableStr = new StringBuffer();
		StringBuffer rowOfData = new StringBuffer();
		String colDelim = "\t";

		if (aIncludeHeader) {
			appendHeaderRow(tableStr, _columns, colDelim, ROWDELIM);
		}

		for (int i = 0; i < _dataVectors.length; i++) {
			rowOfData.setLength(0);

			Enumeration colEnum = _columns.elements();
			Enumeration dataEnum = _dataVectors[i].elements();
			int colInx = 0;
			while (colEnum.hasMoreElements()) {
				DSTableFieldIFace colDef = (DSTableFieldIFace) colEnum
						.nextElement();
				String eleStr = (String) dataEnum.nextElement();
				if (colInx > 0) {
					rowOfData.append(colDelim);
				}

				rowOfData.append(getValidStringValueForType(eleStr, colDef
						.getDataType()));
				colInx++;
			}
			tableStr.append(rowOfData);
			tableStr.append(ROWDELIM);
		}
		return tableStr.toString();
	}

	public boolean prefire() throws IllegalActionException {
		boolean check = true;
		if (_dataVectors != null) {
			check = (_recordCount < _dataVectors.length);
			// if (!check) _recordCount = 0;
			// System.out.println("in prefire "+"_recordCount = "+_recordCount+" check = "+check);
			// System.out.println("in prefire "+"_dataVectors.length = "+_dataVectors.length);
		}
		return check;
	}

	/**
	 * Send an entire tableas a single token over the port on each fire event.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	private void fireAsTable() throws IllegalActionException {
		if (_tableEntity == null) {
			System.err.println("_tableEntity is null in fireAsTable");
			return;
		}

		TypedIOPort pp = (TypedIOPort) this.getPort(DATATABLE);
		pp.send(0, new StringToken(createTableFromSQLResults(false, "\t")));

		pp = (TypedIOPort) this.getPort(DELIMITER);
		String delim = _tableEntity.getDelimiter();
		pp.send(0, new StringToken(delim));

		pp = (TypedIOPort) this.getPort(NUMCOLUMNS);
		pp.send(0, new IntToken(_tableEntity.getAttributes().length));

	}

	/**
	 * Send a row as a single token over the port on each fire event.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	private void fireAsRow() throws IllegalActionException {
		if (_tableEntity == null) {
			System.err.println("_tableEntity is null in fireAsRow");
			return;
		}

		StringBuffer rowOfData = new StringBuffer();
		String delim = _tableEntity.getDelimiter();

		TypedIOPort port = (TypedIOPort) this.getPort(DATAROW);
		if (port != null && _dataVectors != null) {
			Enumeration dataEnum = _dataVectors[_recordCount].elements();
			int fieldCnt = 0;
			while (dataEnum.hasMoreElements()) {
				String eleStr = (String) dataEnum.nextElement();

				if (fieldCnt > 0) {
					rowOfData.append(delim);
				}
				rowOfData.append(eleStr);
				fieldCnt++;
			}

			port.send(0, new StringToken(rowOfData.toString()));

			port = (TypedIOPort) this.getPort(DELIMITER);
			port.send(0, new StringToken(delim));

			port = (TypedIOPort) this.getPort(NUMCOLUMNS);
			port.send(0, new IntToken(fieldCnt));

			_recordCount++;
		} else {
			System.err.println("Port is null.");
		}

	}

	/**
	 * Send a record's tokens over the ports on each fire event.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	private void fireAsField() throws IllegalActionException {
		if (_columns == null) {
			System.err.println("_columns is null in fireAsField");
			return;
		}

		if (_colTypes == null) {
			System.err.println("_colTypes is null in fireAsField");
			return;
		}
		// System.out.println("the columns is "+_columns);
		// System.out.println("the columns type is "+_colTypes);
		// System.out.println("the data is "+_dataVectors[_recordCount]);
		// dbg.print("Processing record: " + i, 2);
		Enumeration colEnum = _columns.elements();
		Enumeration dataEnum = _dataVectors[_recordCount].elements();
		Enumeration typeEnum = _colTypes.elements();
		while (colEnum.hasMoreElements() && dataEnum.hasMoreElements()
				&& typeEnum.hasMoreElements()) {
			DSTableFieldIFace colDef = (DSTableFieldIFace) colEnum
					.nextElement();
			// System.out.println("the colDef "+colDef.getName());
			String eleStr = (String) dataEnum.nextElement();
			// System.out.println("the eleStr is "+eleStr);
			Type type = (Type) typeEnum.nextElement();
			// System.out.println("the type is "+type.toString());
			TypedIOPort port = (TypedIOPort) this.getPort(colDef.getName()
					.trim());

			if (port != null) {
				Token val = null;
				// System.out.println("the port is not null and will send it out");
				// find the data type for each att
				if (type == BaseType.INT) {
					try {
						if (eleStr != null) {
							val = new IntToken(new Integer(eleStr).intValue());
						} else {
							// System.out.println("in nil");
							val = IntToken.NIL;
						}
					} catch (Exception e) {
						val = new IntToken(Integer.MAX_VALUE);
					}
				} else if (type == BaseType.DOUBLE) {
					try {
						if (eleStr != null) {
							val = new DoubleToken(new Double(eleStr)
									.doubleValue());
						} else {
							// System.out.println("in nil");
							val = DoubleToken.NIL;
						}
					} catch (Exception e) {
						val = new DoubleToken(Double.MAX_VALUE);
					}

				} else if (type == BaseType.LONG) {
					try {
						if (eleStr != null) {
							val = new LongToken(new Long(eleStr).longValue());
						} else {
							// System.out.println("in nil");
							val = LongToken.NIL;
						}
					} catch (Exception e) {
						val = new DoubleToken(Long.MAX_VALUE);
					}

				} else if (type == BaseType.STRING) {
					if (eleStr != null) {
						val = new StringToken(eleStr);
					} else {
						// System.out.println("in nil");
						val = StringToken.NIL;
					}

				} else {
					val = new StringToken("");
				}
				// send the data on the port
				port.send(0, val);
			}
		}
		_recordCount++;
	}

	/**
	 * Send a record's tokens over the ports on each fire event.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		super.fire();

		if (speciesName.getWidth() > 0) {
			if (speciesName.hasToken(0)) {
				String speciesNameStr = ((StringToken) (speciesName.get(0)))
						.stringValue();
				_ignoreSearchChange = false;
				_searchData.setExpression(speciesNameStr);
				// _searchData is a stringParameter; need to 'getToken' to
				// forcer
				// evaluation of expression and trigger attribute change
				_searchData.getToken();
				// attributeChanged(_searchData);
				_ignoreSearchChange = true;
			}
		} else {
			// need to call attributechanged to get data frome ecogrid if not in
			// the
			// cache and to trigger the latch from a callback from 'complete'
			_ignoreSearchChange = false;
			attributeChanged(_searchData);
			_ignoreSearchChange = false;
		}
		initial_setup();
		_downloadCompleted = new Latch();
		if (_dataVectors == null) {
			System.err
					.println("_dataVectors is null in createTableFromSQLResults");
			return;
		}

		if (_recordCount >= _dataVectors.length) {
			System.err.println("_recordCount [" + _recordCount
					+ "] is greater than _dataVectors.length "
					+ _dataVectors.length);
			return;
		}
		switch (_outputType) {
		case _ASTABLE:
			fireAsTable();
			break;

		case _ASROW:
			fireAsRow();
			break;

		case _ASFIELD:
			fireAsField();
			break;

		default:
			throw new IllegalActionException(this, "Unrecognized outputType: "
					+ _outputType);

		}
	}

	/**
	 * Remove all ports by setting their container to null. As a side effect,
	 * the ports will be unlinked from all relations. This method is
	 * write-synchronized on the workspace, and increments its version number.
	 */
	private void removeAllOutputPortsExcept(Set l)
			throws IllegalActionException {
		Vector portNames = getExistingPortNames();
		for (Enumeration e = portNames.elements(); e.hasMoreElements();) {
			String currPortName = (String) e.nextElement();
			TypedIOPort port = (TypedIOPort) this.getPort(currPortName);
			if (port != null && !port.isInput() && !l.contains(currPortName)) {
				try {
					log.debug("Removing port named: " + port.getName());
					port.setContainer(null);
				} catch (Exception ex) {
					throw new IllegalActionException(this,
							"Error removing port: " + currPortName);
				}

			}
		}

	}

	private void createOutputPorts(Map m) throws IllegalActionException {

		Iterator i = m.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry e = (Map.Entry) i.next();
			String portname = (String) e.getKey();
			BaseType b = (BaseType) e.getValue();
			try {
				portConfigurer(portname, b);
			} catch (NameDuplicationException ex) {
				log.error("Unexpected Port Name duplication for port name: "
						+ portname, ex);
				throw new IllegalActionException(this, ex, "Port name "
						+ portname + " duplicated");
			}
		}

	}

	/**
	 * It reconfigures all the Ports to represent passing data back a row at a
	 * time
	 * 
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	private void reconfigurePortsAsTable() throws IllegalActionException {
		log.debug("ReconfigurePortsAsTable");
		removeAllOutputPortsExcept(asTablePorts.keySet());
		createOutputPorts(asTablePorts);
	}

	/**
	 * Helper method that checks to see if a port is there before creating one
	 * 
	 * @param aName
	 * @param aType
	 * @throws ptolemy.kernel.util.NameDuplicationException
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	private void portConfigurer(String aName, Type aType)
			throws NameDuplicationException, IllegalActionException {
		TypedIOPort pp = (TypedIOPort) this.getPort(aName);
		if (pp == null) {
			log.debug("Adding port named: " + aName);
			pp = new TypedIOPort(this, aName, false, true);
			pp.setTypeEquals(aType);
		} else {
			log.debug("Adding port named (set type): " + pp.getName());
			pp.setTypeEquals(aType);
		}
	}

	/**
	 * It reconfigures all the Ports to represent passing data back a row at a
	 * time
	 * 
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	private void reconfigurePortsAsRow() throws IllegalActionException {
		removeAllOutputPortsExcept(asRowPorts.keySet());
		createOutputPorts(asRowPorts);
	}

	/**
	 * It reconfigures all the Ports from the new "columns" list Meaning it
	 * removes port not in the list and adds new port that are in the list but
	 * not yet there
	 * 
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	private void reconfigurePortsAsField() throws IllegalActionException {
		if (_columns != null) {
			// throw new
			// IllegalActionException(this,"Unable to Configure output ports as Field until the download is complete");

			// Get List of new column names
			_colTypes.clear();

			HashMap newColPorts = new HashMap();
			for (Enumeration e = _columns.elements(); e.hasMoreElements();) {
				DSTableFieldIFace col = (DSTableFieldIFace) e.nextElement();
				String colName = col.getName();
				Type colType = getBaseType(col.getDataType());
				_colTypes.add(colType);
				newColPorts.put(colName, colType);
			}

			removeAllOutputPortsExcept(newColPorts.keySet());
			createOutputPorts(newColPorts);
		}

	}

	/**
	 * 
	 * @param aNewColumns
	 * @param aColTypes
	 * @throws ptolemy.kernel.util.IllegalActionException
	 */
	private void reconfigurePorts(String why) {
		log.debug("Creating reconfigure ports change request " + why);
		this.requestChange(new ChangeRequest(this, why) {
			public void _execute() throws Exception {
				log.debug("Executing reconfigure ports change request "
						+ this.getDescription());
				log.debug("Current _outputType = " + _outputType);
				switch (_outputType) {
				case _ASTABLE:
					reconfigurePortsAsTable();
					break;

				case _ASROW:
					reconfigurePortsAsRow();
					break;

				case _ASFIELD:
					reconfigurePortsAsField();
					break;

				default:
					throw new IllegalActionException(DarwinCoreDataSource.this,
							"Unrecognized outputType: " + _outputType);

				}

			}
		});
	}

	/**
	 * Callback for changes in attribute values.
	 */
	public void attributeChanged(ptolemy.kernel.util.Attribute attribute)
			throws IllegalActionException {
		// System.out.println("Changed: "+attribute.getName()+"  "+((Settable)attribute).getExpression());
		if (attribute == _sqlAttr && !_ignoreSqlChange) {
			String sqlDef = ((Settable) attribute).getDefaultExpression();
			sqlDef = ((Settable) attribute).getExpression();
			if (sqlDef.length() > 0) {
				_queryDef = DBQueryDefParserEmitter.parseQueryDef(_schemaDef,
						sqlDef);
				_columns = _queryDef.getSelects();
				log
						.debug("Calling reconfigurePorts because of sqlAttr change.");
				reconfigurePorts("sqlAttr change");
			}

		} else if (attribute == _schemaAttr && !_ignoreSchemaChange) // NOTE: We
																		// may
																		// skip
																		// setting
																		// it
																		// here
																		// because
																		// _ignoreSchemaChange
																		// may
																		// be
																		// true
		{
			String schemaDef = ((Settable) _schemaAttr).getExpression();

			// MOML may have a blank definition
			if (schemaDef.length() > 0) {
				if (isDebugging) {
					log.debug("schemaDef = " + schemaDef);
				}

				_schemaDef = DBSchemaParserEmitter.parseSchemaDef(schemaDef);
			}

		} else if (attribute == _outputTypeAttr) {
			String strOutputType = _outputTypeAttr.stringValue();
			log.debug("Attribute Changed: " + strOutputType);
			if (strOutputType.equals("As Table") && _outputType != _ASTABLE) {
				_outputType = _ASTABLE;
			} else if (strOutputType.equals("As Row") && _outputType != _ASROW) {
				_outputType = _ASROW;
			} else if (strOutputType.equals("As Field")
					&& _outputType != _ASFIELD) {
				_outputType = _ASFIELD;
			} else {
				// throw new IllegalActionException(this,
				// "Unrecognized outputType: " + strOutputType);
			}
			log
					.debug("Calling reconfigurePorts because of outputTypeAttr change.");
			reconfigurePorts("outputTypeAttr");

		} else if ((attribute.getName().equals(ENDPOINT_ATTR) || attribute
				.getName().equals(SEARCHDATA_ATTR))
				&& // !_ignoreSearchChange &&
				// removed the ignoreSearchFlag so that actor will initialize
				// when 1st dragged
				// to the work area. Without this, the SQL database cannot be
				// viewed to set
				// values. - Dan Higgins Jan 2008
				hasConnectionValues()
				&& !(attribute.getContainer().getContainer() instanceof ResultTreeRoot)) {
			_icon.setBusy();
			_ignoreSearchChange = true;
			String endPointStr = getEndpoint();
			String searchStr = getSearchData();
			if ((searchStr == null) || (searchStr.length() < 1))
				return;

			// System.out.println("endPointStr: "+endPointStr);
			// System.out.println("searchStr:   "+searchStr);

			StringBuffer queryBuf = new StringBuffer();
			queryBuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			queryBuf
					.append("<egq:query queryId=\"query-digir.1.1\" system=\"http://knb.ecoinformatics.org\" ");
			queryBuf
					.append("  xmlns:egq=\"http://ecoinformatics.org/query-1.0.1\" ");
			queryBuf
					.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
			queryBuf
					.append("  xsi:schemaLocation=\"http://ecoinformatics.org/query-1.0.1 ../../src/xsd/query.xsd\">");
			queryBuf
					.append("  <namespace prefix=\"darwin\">http://digir.net/schema/conceptual/darwin/2003/1.0</namespace>");
			queryBuf.append("  <title>" + searchStr + " Query</title>");

			boolean useAllField = false;
			if (useAllField) {
				DSTableIFace table = (DSTableIFace) _darwinCoreSchema
						.getTables().elementAt(0); // there is only one table
				for (Enumeration e = table.getFields().elements(); e
						.hasMoreElements();) {
					DSTableFieldIFace field = (DSTableFieldIFace) e
							.nextElement();
					queryBuf.append("  <returnField>/" + field.getName()
							+ "</returnField>");
				}
			} else {
				queryBuf.append("  <returnField>/Species</returnField>");
				queryBuf.append("  <returnField>/ScientificName</returnField>");
				queryBuf.append("  <returnField>/Collector</returnField>");
				queryBuf.append("  <returnField>/YearCollected</returnField>");
				queryBuf
						.append("  <returnField>/InstitutionCode</returnField>");
				queryBuf.append("  <returnField>/CollectionCode</returnField>");
				queryBuf.append("  <returnField>/CatalogNumber</returnField>");
				queryBuf
						.append("  <returnField>/CatalogNumberText</returnField>");
				queryBuf
						.append("  <returnField>/DecimalLatitude</returnField>");
				queryBuf
						.append("  <returnField>/DecimalLongitude</returnField>");
			}
			queryBuf
					.append("  <condition operator=\"LIKE\" concept=\"ScientificName\">"
							+ searchStr + "</condition>");
			queryBuf.append("</egq:query>");

			try {
				QueryType query = null;

				StringReader strReader = new StringReader(queryBuf.toString());
				EcogridQueryParser parser = new EcogridQueryParser(strReader);
				parser.parseXML();
				query = parser.getEcogridQuery();

				_icon.setBusy();
				String resName = "DigirQuery: " + searchStr;
				_cachedDataItem = (EcogridQueryDataCacheItem) DataCacheManager
						.getCacheItem(this, resName, endPointStr,
								EcogridQueryDataCacheItem.class.getName());
				if (_cachedDataItem.isEmpty()) {
					_cachedDataItem.setQuery(query);
					_cachedDataItem.start();
				}
				// need to update the _sqlAttr expression to include the new
				// table name
				// use a regex ('replaceAll') - DFH March 2007
				String temp = _sqlAttr.getExpression();
				temp = temp.replaceAll("DigirTable: .*?\"", "DigirTable: "
						+ getSearchData() + "\"");
				_ignoreSqlChange = true;
				_sqlAttr.setExpression(temp);
				_ignoreSqlChange = false;
			} catch (Exception e) {
				System.err.println(e);
				e.printStackTrace();
			}
		}
	}

	/**
	 * returns the names of the current ports
	 */
	private Vector getExistingPortNames() {
		List l = this.portList();
		Vector v = new Vector();
		for (int i = 0; i < l.size(); i++) {
			TypedIOPort p = (TypedIOPort) l.get(i);
			v.add(p.getName().trim());
		}
		return v;
	}

	/**
	 * Returns a Ptolemly type for a given Kepler DataSource Type
	 * 
	 * @param aType
	 *            DataSource type
	 * @return Ptolemy type
	 */
	private static BaseType getBaseType(String aType) {
		BaseType type = (BaseType) mTypeHash.get(aType);
		if (type == null) {
			return BaseType.UNKNOWN;
		}
		return type;
	}

	private void generateTable(ResultsetTypeRecord[] records) {
		_icon.setBusy();
		try {

			// Create a name for this text table which is unique.
			String resName = "DigirTable: " + getSearchData();

			// Ok, now create the table as a "string" table where each row is
			// ROWDELIM
			// and load it into the cache.
			_tableDataCache = (EcogridDataCacheItem) DataCacheManager
					.getCacheItem(null, resName, DATATABLE,
							EcogridDataCacheItem.class.getName());

			// now create the table entity
			_tableEntity = new Entity(_tableDataCache.getLSID().toString(),
					resName, "", new Boolean(true), Entity.COLUMNMAJOR,
					records.length);
			_tableEntity.setNumHeaderLines(1);
			_tableEntity.setDelimiter(COLDELIM);
			_tableEntity.setRecordDelimiter(ROWDELIM);

			DBTableNameResolver nameResolver = new DBTableNameResolver();
			try {
				_tableEntity = nameResolver.resolveTableName(_tableEntity);
			} catch (Exception e) {
			}
			String tableName = _tableEntity.getMappedName() + "";

			String tableStr = createTableFromResultset(COLDELIM, records);
			// System.err.println("*** Table[\n"+tableStr+"\n]");
			_tableDataCache.setData(tableStr.getBytes());

			DSTableIFace tableSchema = (DSTableIFace) _resultsetSchemaDef
					.getTables().elementAt(0);
			Vector columns = tableSchema.getFields();
			Enumeration colEnum = columns.elements();
			while (colEnum.hasMoreElements()) {
				DSTableFieldIFace colDef = (DSTableFieldIFace) colEnum
						.nextElement();
				org.kepler.objectmanager.data.db.Attribute attr = new org.kepler.objectmanager.data.db.Attribute(
						colDef.getName(), colDef.getName(), colDef
								.getDataType());
				_tableEntity.add(attr);
			}

			_tableGenerator = new DBTablesGenerator(_tableEntity,
					_tableDataCache.getBaseFileName());
			_tableGenerator.run(); // don't do thread

			DSSchemaDef schema = new DSSchemaDef();
			schema.addTable(_tableEntity);
			_schemaDef = schema;
			// right here it is really hard to know if the table has already
			// been generated
			// and whether we need to set the schema for the first time or not
			// So if it is empty, then set it, if not then ignore it
			// if (_schemaAttr.getExpression().length() == 0)
			{
				String schemaDefXML = DBSchemaParserEmitter.emitXML(_schemaDef);
				_ignoreSchemaChange = true;
				_schemaAttr.setExpression(schemaDefXML);
				_ignoreSchemaChange = false;
			}

		} catch (Exception e) {
		}
		_icon.setReady();
	}

	// ------------------------------------------------------------------------
	// -- DataCacheListener
	// ------------------------------------------------------------------------

	public void complete(DataCacheObject aItem) {
		try {
			_cachedDataItem = (EcogridQueryDataCacheItem) aItem;
			if (!_cachedDataItem.isReady()) {
				_icon.setError();
				return;
			}
			// System.out.println("Inside 'complete' of DarwinCoreDataSource!!!!");
			ResultsetType _resultsetType = _cachedDataItem.getResultset();
			if (_resultsetType == null) {
				_icon.setError();
				// System.out.println("Resultset was NULL!");
				return;
			}

			//
			// Using the metadata names construct
			// the table schema definition
			//
			// Note: The columns are defined in the same order as they appear in
			// the document.
			// This is not the same order they are sent in the query. However,
			// this code does leverage the fact that the Ecogrid Digir
			// implementation
			// returns all the fields in all records in the same order as they
			// appear in
			// the metadata.
			ResultsetTypeResultsetMetadataRecordStructure rs = _resultsetType
					.getResultsetMetadata().getRecordStructure();
			ResultsetTypeResultsetMetadataRecordStructureReturnField[] fields = rs
					.getReturnField();
			if (fields != null) {
				DSTableDef table = new DSTableDef("DarwinCore");
				for (int i = 0; i < fields.length; i++) {
					String fieldname = fields[i].getName();
					// System.out.println("the field name from return structure "+fieldname);
					//
					// Test for a leading "/" in the fieldname.
					// Strip it if it's there.
					// This Fixes the problem that Ecogrid needs to have leading
					// '/' right now. But yet this part of the system cannot
					// handle them.
					if (fieldname.startsWith("/")) {
						fieldname = fieldname.substring(1);
					}
					String fieldtype = DarwinCoreSchema.getInstance()
							.lookupTypeFromName(fieldname);
					// System.out.println(fieldname +" -> "+ fieldtype);
					table.addField(fieldname, fieldtype, null);
				}
				// create a schemaDef that was used by the results
				DSSchemaDef schema = new DSSchemaDef();
				schema.addTable(table);
				_resultsetSchemaDef = schema;
				// DSTableIFace table =
				// (DSTableIFace)_darwinCoreSchema.getTables().elementAt(0);
				String sqlDef = _sqlAttr.getExpression();
				log.debug("Calling reconfigurePorts in Complete.");
				if (sqlDef.length() == 0 || _queryDef == null
						|| _columns == null || _colTypes == null) {
					_columns = table.getFields();
					// reconfigurePorts("Complete 1");
				} else {
					_columns = _queryDef.getSelects();
					// reconfigurePorts("Complete 2");
				}
			}

			ResultsetTypeRecord[] _records = null;
			_records = _cachedDataItem.getResultset().getRecord();

			//
			// Here we need to make populate the _schemaDef attribute in order
			// to enable the
			// query builder (Ie, Look Inside or Open Actor).
			if (_records != null) {
				generateTable(_records);
			}

			_icon.setReady();
		} finally {
			_downloadCompleted.release();
		}
	}

	/**
	 * stop is used by the Director to indicate to the actor that execution is
	 * to be stopped. We use this opportunity to notify the execution thread
	 * which may be blocked in the initialize() method waiting for the
	 * _downlaodCompleted Latch. We don't release the Latch because that will
	 * only be done when the download completes.
	 */
	public void stop() {
		log.debug("Stopping");
		synchronized (_downloadCompleted) {
			_downloadCompleted.notifyAll();
		}
		super.stop();
	}

	/**
	 * Determine if the recordId and endpoint attributes have valid values for
	 * use in retrieving the record for parsing.
	 * 
	 * @return boolean true if both recordid and endpoint are not null and are
	 *         not the empty string
	 */
	private boolean hasConnectionValues() {
		boolean hasValues = false;
		String SearchData = this.getSearchData();
		String endpoint = this.getEndpoint();

		if (SearchData != null && !SearchData.equals("") && endpoint != null
				&& !endpoint.equals("")) {
			hasValues = true;
		}
		return hasValues;
	}

	/**
	 * This method allows default documentation to be added to the actor
	 * specified in the parameter. The KeplerDocumentation is retrieved from the
	 * actor that exists in the actor library. The default documentation is only
	 * loaded once since it will likely remain quite static. If the given actor
	 * instance already contains the KeplerDocumentation attribute, the existing
	 * attribute is preserved (nothing is changed).
	 * 
	 * @param emlActor
	 *            the instance to which documentation will be added
	 */
	public static void generateDocumentationForInstance(
			DarwinCoreDataSource actor) {
		try {
			// only look up the documentation only once
			if (defaultDocumentation == null) {
				Iterator cacheIter = CacheManager.getInstance()
						.getCacheObjectIterator();
				while (cacheIter.hasNext()) {
					Object co = cacheIter.next();
					// is this an actor cache object?
					if (co instanceof ActorCacheObject) {
						ActorCacheObject aco = (ActorCacheObject) co;
						// for this class?
						if (aco.getClassName().equals(actor.getClassName())) {
							// get the metadata
							ActorMetadata am = aco.getMetadata();
							// get the default documentation from the metadata
							defaultDocumentation = am
									.getDocumentationAttribute();
							log
									.debug("looked up default KeplerDocumentation contained in "
											+ am.getName());
							break;
						}
					}
				}
			}

			// add the documentation for this actor if it is not there already
			if (actor.getAttribute("KeplerDocumentation") == null) {
				// make an instance of the documentation attribute for the input
				// actor
				KeplerDocumentationAttribute keplerDocumentation = new KeplerDocumentationAttribute(
						actor, "KeplerDocumentation");

				// copy the default and set it for this one
				keplerDocumentation
						.createInstanceFromExisting(defaultDocumentation);
				keplerDocumentation.setContainer(actor);
				log.debug("set the KeplerDocumentation for actor instance: "
						+ actor.getName());

			}
		} catch (Exception e) {
			log
					.error("error encountered whilst generating default documentation for actor instance: "
							+ e.getMessage());
			e.printStackTrace();
		}
	}

}