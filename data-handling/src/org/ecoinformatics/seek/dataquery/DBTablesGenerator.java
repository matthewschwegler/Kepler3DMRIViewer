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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.data.UnresolvableTypeException;
import org.kepler.objectmanager.data.db.Attribute;
import org.kepler.objectmanager.data.db.Entity;
import org.kepler.objectmanager.data.text.TextComplexFormatDataReader;
import org.kepler.util.DelimitedReader;
import org.kepler.util.sql.DatabaseFactory;

/**
 * The class to generate db tables base table entity
 * 
 * @author Jing Tao
 * 
 */

public class DBTablesGenerator implements Runnable {

	// Constant
	private static final String CREATETEXTABLEPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/SQLDictionary/textTable/createTextTable";
	private static final String CREATETABLEPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/SQLDictionary/createTable";
	private static final String IFEXISTSPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/SQLDictionary/dropSuffix";
	private static final String SEMICOLONPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/SQLDictionary/semicolon";
	private static final String FIELDSPEPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/SQLDictionary/textTable/fieldSeperator";
	private static final String SETTABLEPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/SQLDictionary/textTable/setTable";
	private static final String SOURCEPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/SQLDictionary/textTable/source";
	private static final String IGNOREFIRSTPATH = "//sqlEngine[sqlEngineName=\"hsql\"]/SQLDictionary/textTable/ignoreFirst";
	private static final String VECTORDATATYPE = "vectorDataType";
	private static final String TEXTFILETYPE = "textFileType";

	public static final String CREATETEXTTABLE; 
	public static String CREATETABLE;
	public static final String DROPTABLE = "DROP TABLE";
	public static String IFEXISTS; 
	public static final String LEFTPARENTH = "(";
	public static final String RIGHTPARENTH = ")";
	public static String SEMICOLON = ";"; 
	public static final String SPACE = " ";
	public static final String COMMA = ",";
	public static final String QUOTE = "\"";
	public static String FIELDSEPATATOR; 
	public static final String SELECT = "SELECT";
	public static final String INSERT = "INSERT INTO";
	public static final String DELETE = "DELETE";
	public static final String WHERE = "WHERE";
	public static final String FROM = "FROM";
	public static final String LIKE = "LIKE";
	public static final String VALUES = "VALUES";
	public static final String AND = "AND";
	public static final String QUESTION = "?";
	public static String SETTABLE; 
	public static String SOURCE; 
	public static String IGNOREFIRST; 
	public static final String STRING = "String";
	public static final String INTEGER = "Integer";
	public static final String LONG = "Long";
	public static final String DOUBLE = "Double";
	public static final String FLOAT = "Float";
	public static final String DATETIME = "Timestamp";
	public static final String BOOLEAN = "Boolean";

	private Vector dbJavaDataTypeList = new Vector();
	private Entity tableEntity;
	private InputStream givenData;
	private String textFileLocation;
	private String type;
	private boolean isDone = false;
	private boolean successStatus = false;
	private boolean isRefresh = false;

	private static Log log;
	private static boolean isDebugging;

	static {
		log = LogFactory.getLog("org.ecoinformatics.seek.dataquery");
		isDebugging = log.isDebugEnabled();
    
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty commonProperty = confMan.getProperty(ConfigurationManager.getModule("common"));
    ConfigurationProperty sqlEngineProperty = (ConfigurationProperty)commonProperty
      .findProperties("sqlEngineName", "hsql", true).get(0);
      
    CREATETEXTTABLE = sqlEngineProperty.getProperty("SQLDictionary.textTable.createTextTable").getValue();
    CREATETABLE = sqlEngineProperty.getProperty("SQLDictionary.createTable").getValue();
    IFEXISTS = sqlEngineProperty.getProperty("SQLDictionary.dropSuffix").getValue();
    SEMICOLON = sqlEngineProperty.getProperty("SQLDictionary.semicolon").getValue();
    FIELDSEPATATOR = sqlEngineProperty.getProperty("SQLDictionary.textTable.fieldSeperator").getValue();
    SETTABLE = sqlEngineProperty.getProperty("SQLDictionary.textTable.setTable").getValue();
    SOURCE = sqlEngineProperty.getProperty("SQLDictionary.textTable.source").getValue();
    IGNOREFIRST = sqlEngineProperty.getProperty("SQLDictionary.textTable.ignoreFirst").getValue();
	}

	/**
	 * This constructor is for non-text file table. Before create the
	 * consturctor user should run DBTableNameResolver first.
	 * 
	 * @param tableEntity
	 *            TableEntity table will generated base on the object
	 * @param givenData
	 *            InputStream data will be load to table. The input stream which
	 *            from data file. It should be text format
	 */
	public DBTablesGenerator(Entity tableEntity, InputStream givenData) {
		// default set refresh is false
		this(tableEntity, givenData, false);
	}// DBTablesGenerator

	/**
	 * This constructor is for text file table. Before create the consturctor
	 * user should run DBTableNameResolver first.
	 * 
	 * @param tableEntity
	 *            TableEntity
	 * @param textFileLocation
	 *            String
	 */
	public DBTablesGenerator(Entity tableEntity, String textFileLocation) {
		// default set refresh is false
		this(tableEntity, textFileLocation, false);
	}// DBTablesGenerator

	/**
	 * This constructor is for non-text file table. Before create the
	 * consturctor user should run DBTableNameResolver first.
	 * 
	 * @param tableEntity
	 *            TableEntity table will generated base on the object
	 * @param givenData
	 *            Vector[] data will be load to table. This a vector array, each
	 *            vector is one row of data. The element in vector is a string
	 * @param isRefresh
	 *            boolean force to re-generate table again
	 */
	public DBTablesGenerator(Entity tableEntity, InputStream givenData,
			boolean isRefresh) {
		this.tableEntity = tableEntity;
		this.givenData = givenData;
		this.type = VECTORDATATYPE;
		this.isRefresh = isRefresh;
	}// DBTablesGenerator

	/**
	 * This constructor is for text file table. Before create the consturctor
	 * user should run DBTableNameResolver first.
	 * 
	 * @param tableEntity
	 *            TableEntity
	 * @param textFileLocation
	 *            String
	 * @param isRefresh
	 *            boolean force to re-generate table again
	 */
	public DBTablesGenerator(Entity tableEntity, String textFileLocation,
			boolean isRefresh) {
		this.tableEntity = tableEntity;
		this.textFileLocation = textFileLocation;
		this.type = TEXTFILETYPE;
		this.isRefresh = isRefresh;
	}// DBTablesGenerator

	/**
	 * A thread to call some private method to generate table.
	 */
	public void run() {
		generateTables(isRefresh);
	}// run

	/**
	 * Method to get success status of generating table
	 * 
	 * @return boolean
	 */
	public synchronized boolean getSuccessStatus() {
		return this.successStatus;
	}

	/**
	 * Method to get isDone status of generationg table
	 * 
	 * @return boolean
	 */
	public synchronized boolean getIsDone() {
		return this.isDone;
	}

	/*
	 * This method will generate tables base on given type
	 */
	private void generateTables(boolean refresh) {
		if (type == null) {
			successStatus = false;
			isDone = true;
		} else if (type.equals(VECTORDATATYPE)) {
			// generate vector table
			successStatus = generateDBTableForGivenData(refresh);
			isDone = true;
		} else if (type.equals(TEXTFILETYPE)) {
			// gernate text table
			successStatus = generateDBTextTable(refresh);
			isDone = true;
		} else {
			successStatus = false;
			isDone = true;
		}

	}

	/*
	 * This is for non-text file tables. And will load data into db. If it is
	 * refresh, it will delete the record in system table and drop the old
	 * table. Then generate new table and create an record in system table
	 * 
	 * @param givenEntityList Hashtable
	 * 
	 * @param givenData Vector[] This a vector array, each vector is one row of
	 * data. The element in vector is a string
	 */
	private synchronized boolean generateDBTableForGivenData(boolean refresh) {
		boolean success = false;
		if (tableEntity == null) {
			log.debug("The entity is null and couldn't create table for it");
			return success;
		}
		// get table name
		String tableName = null;
		String url = null;
		DBTableExistenceChecker checker = null;
		try {
			checker = new DBTableExistenceChecker();
			url = tableEntity.getURL();
			if (url == null) {
				url = tableEntity.getName();
			}
			if (isDebugging) {
				log.debug("url is " + url);
			}
			// if the table already existed, we don't need
			// generate again(url is the key)
			if (checker.isURLExisted(url)) {
				// to do get the table name and set to table entity
				tableName = checker.getTableName(url);
				tableEntity.setDBTableName(tableName);
				if (isDebugging) {
					log.debug("Table " + tableName + " is existed for url "
							+ url);
				}
				if (!refresh) {
					// the table already existed, if not refresh, we need to
					// stop here
					if (isDebugging) {
						log.debug("refesh setting is " + refresh
								+ " and we don't need generate table again");
					}
					success = true;
					return success;
				} else {
					// table already existed. But we need refresh it - delete
					// record in
					// System and drop the table
					if (isDebugging) {
						log
								.debug("refesh setting is "
										+ refresh
										+ " and we need drop table and generate table again");
					}
					success = cleanUpRecord(tableName, url, checker);
					// if couldn't drop or delete record, return false
					if (success == false) {
						return success;
					}
				}
			}
			// if doesn't exited, we need to get the table name for TableEntity.
			// so before run this method, we need run DBTableNameResovler first.
			tableName = tableEntity.getDBTableName();
			if (isDebugging) {
				log.debug("The table name " + tableName + " will be generated");
			}
			if (tableName == null || tableName.trim().equals("")) {
				log
						.debug("The DB table name for given TableEntity object is null and couldn't generate table");
				success = false;
				return success;
			}

		} catch (Exception ee) {
			log.debug("The error in generate table is ", ee);
			success = false;
			return success;
		}

		try {
			// this is for non-text type table
			String generateTalbeSql = generateDDLForOneEntity(CREATETABLE,
					tableName, tableEntity);
			excuteSQLCommand(generateTalbeSql);
			loadDataIntoTable(tableName, tableEntity, givenData);
			success = true;
		} catch (Exception e) {
			log.debug("The error in generate table is ", e);
			success = false;
		}
		// if success, we need store tablename and url
		if (success) {
			try {
				checker.storeTableRecord(tableName, url);
				tableEntity.setDBTableName(tableName);
			} catch (Exception ee) {
				log.debug("The error in generate table is ", ee);
				success = false;
			}
		}

		// if not success, we need drop the generate table
		if (!success) {
			cleanUpRecord(tableName, url, checker);
		}
		return success;

	}// generatetable

	/*
	 * This is for text file tables. And will load data into db.
	 * 
	 * @param givenEntityList Hashtable
	 * 
	 * @param givenData Vector[] This a vector array, each vector is one row of
	 * data. The element in vector is a string
	 */
	private synchronized boolean generateDBTextTable(boolean refresh) {
		boolean success = false;
		if (tableEntity == null) {
			log.debug("The entity is null and couldn't create table for it");
			return success;
		}

		// get table name
		String tableName = null;
		DBTableExistenceChecker checker = null;
		String url = null;
		try {
			checker = new DBTableExistenceChecker();
			url = tableEntity.getURL();
			if (url == null) {
				url = tableEntity.getName();
			}
			if (isDebugging) {
				log.debug("The url in entity is " + url);
			}
			// if the table already existed, we don't need
			// generate again(url is the key)
			if (checker.isURLExisted(url)) {
				// to do get the table name and set to table entity
				tableName = checker.getTableName(url);
				tableEntity.setDBTableName(tableName);
				if (isDebugging) {
					log.debug("Table " + tableName + " is existed for url "
							+ url);
				}
				if (!refresh) {
					// the table already existed, if not refresh, we need to
					// stop here
					if (isDebugging) {
						log.debug("refesh setting is " + refresh
								+ " and we don't need generate table again");
					}
					success = true;
					return success;
				} else {
					// table already existed. But we need refresh it - delete
					// record in
					// System and drop the table
					if (isDebugging) {
						log
								.debug("refesh setting is "
										+ refresh
										+ " and we need drop table and generate table again");
					}
					success = cleanUpRecord(tableName, url, checker);
					// if couldn't drop or delete record, return false
					if (success == false) {
						return success;
					}
				}

			}

			// if doesn't exited, we need to get the table name for TableEntity.
			// so before run this method, we need run DBTableNameResovler first.
			tableName = tableEntity.getDBTableName();
			if (isDebugging) {
				log.debug("The table name " + tableName + " will be generated");
			}
			if (tableName == null || tableName.trim().equals("")) {
				log
						.debug("The DB table name for given TableEntity object is null and couldn't generate table");
				success = false;
				return success;
			}

		} catch (Exception ee) {
			log.debug("The error in generateDBTable is ", ee);
			success = false;
			return success;
		}

		try {

			int numOfHeadLines = tableEntity.getNumHeaderLines();
			// hsql only handle two scenaro no head line or one head line
			boolean ignoreHeadLines = false;
			if (numOfHeadLines == 0) {
				ignoreHeadLines = false;
			} else if (numOfHeadLines == 1) {
				ignoreHeadLines = true;
			} else {
				if (isDebugging) {
					log.debug("HSQL text table only handle one line header"
							+ " and this entity has " + numOfHeadLines
							+ " headlines");
				}
				success = false;
				return success;
			}

			// if this is attribute row oriented, hsql can't handle it
			String orientation = tableEntity.getOrientation();
			if (orientation != null && orientation.equals(Entity.ROWMAJOR)) {
				log
						.debug("DB doesn't handle a text table which attribute is row oriented");
				success = false;
				return success;
			}

			// this is for text type table
			String generateTableSql = generateDDLForOneEntity(CREATETEXTTABLE,
					tableName, tableEntity);
			excuteSQLCommand(generateTableSql);

			// bind text source to table
			String delimiterStr = tableEntity.getDelimiter();
			// need to figure out the delimiter str in db. The format is
			// different
			DelimiterResolver resolver = new DelimiterResolver();
			String dbDelimiter = resolver.resolve(delimiterStr);
			String bindTextFileToTalbeSql = generateBindTextFileToTableSQL(
					tableName, textFileLocation, dbDelimiter, ignoreHeadLines);

			excuteSQLCommand(bindTextFileToTalbeSql);
			success = true;
		} catch (Exception sql) {
			if (log.isDebugEnabled()) {
				sql.printStackTrace();
			}
			log.error("The error in generateDBTable is " + sql.getMessage());
			success = false;
		}

		// if success, we need store tablename and url
		if (success) {
			try {
				checker.storeTableRecord(tableName, url);
				tableEntity.setDBTableName(tableName);
			} catch (Exception ee) {
				success = false;
			}
		}

		// if not success, we need drop the generate table
		if (!success) {
			cleanUpRecord(tableName, url, checker);
		}

		return success;

	}// generateTable

	/*
	 * roll back method. This method will delete the record generate in system
	 * table and also drop the generated table
	 */
	private boolean cleanUpRecord(String tableName, String url,
			DBTableExistenceChecker checker) {
		boolean success = true;
		// drop the table
		String drop = generateDropSqlCommand(tableName);
		try {
			// drop the existed table
			excuteSQLCommand(drop);
			// delete the record from system table
			checker.deleteRecord(tableName, url);
		} catch (Exception ee) {
			success = false;
		}
		return success;
	}

	/*
	 * Method to generate drop sql command
	 */
	private synchronized String generateDropSqlCommand(String tableName) {
		String sql = DROPTABLE + SPACE + tableName + SPACE + IFEXISTS
				+ SEMICOLON;
		return sql;
	}

	/*
	 * Create a table base one given DDL
	 */
	private synchronized void excuteSQLCommand(String sql) throws SQLException,
			ClassNotFoundException {
		Connection conn = null;
		Statement st = null;
		if (isDebugging) {
			log.debug("The sql command to run is " + sql);
		}
		try {
			conn = DatabaseFactory.getDBConnection();
			st = conn.createStatement();
			st.execute(sql);
		} finally {
			st.close();
			conn.close();
		}
	}// generateTable

	/*
	 * Method to load data into table. If error happend, it will roll back.
	 * Vector is String vector in vector array data.
	 */
	private synchronized void loadDataIntoTable(String tableName, Entity table,
			InputStream dataStream) throws SQLException,
			ClassNotFoundException, IllegalArgumentException, Exception {
		if (dataStream == null) {
			return;
		}

		PreparedStatement pStatement = null;
		Connection conn = DatabaseFactory.getDBConnection();
		conn.setAutoCommit(false);
		try {
			String insertCommand = generateInsertCommand(tableName, table);
			pStatement = conn.prepareStatement(insertCommand);
			// int length = data.length;

			if (!table.getIsImageEntity() && table.isSimpleDelimited()) {
				// create SimpleDelimiter reader
				int numCols = table.getAttributes().length;
				String delimiter = table.getDelimiter();
				int numHeaderLines = table.getNumHeaderLines();
				String lineEnding = table.getPhysicalLineDelimiter();
				if (lineEnding == null || lineEnding.trim().equals("")) {
					lineEnding = table.getRecordDelimiter();
				}
				int numRecords = table.getNumRecords();
				boolean stripHeader = true;
				DelimitedReader simpleReader = new DelimitedReader(dataStream,
						numCols, delimiter, numHeaderLines, lineEnding,
						numRecords, stripHeader);
				Vector row = simpleReader.getRowDataVectorFromStream();
				while (!row.isEmpty()) {
					// insert one row data into table
					int sizeOfRow = row.size();
					for (int j = 0; j < sizeOfRow; j++) {
						String dataElement = (String) row.elementAt(j);
						// get data type for the vector which already has the
						// cloumn java
						// type info after parsing attribute in private method
						// parseAttributeList
						String javaType = (String) dbJavaDataTypeList
								.elementAt(j);
						// this method will binding data into preparedstatement
						// base on
						// java data type
						// The index of pstatement start 1 (Not 0), so it should
						// j+1.
						pStatement = setupPreparedStatmentParameter(j + 1,
								pStatement, dataElement, javaType);

					}
					pStatement.execute();
					row = simpleReader.getRowDataVectorFromStream();
				}
			} else if (!table.getIsImageEntity() && !table.isSimpleDelimited()) {
				TextComplexFormatDataReader complexReader = new TextComplexFormatDataReader(
						dataStream, table);
				Vector row = complexReader.getRowDataVectorFromStream();
				while (!row.isEmpty()) {
					// insert one row data into table
					int sizeOfRow = row.size();
					for (int j = 0; j < sizeOfRow; j++) {
						String dataElement = (String) row.elementAt(j);
						dataElement = dataElement.trim();
						// System.out.println("The data is "+ dataElement);
						// get data type for the vector which already has the
						// cloumn java
						// type info after parsing attribute in private method
						// parseAttributeList
						String javaType = (String) dbJavaDataTypeList
								.elementAt(j);
						// this method will binding data into preparedstatement
						// base on
						// java data type
						// The index of pstatement start 1 (Not 0), so it should
						// j+1.
						pStatement = setupPreparedStatmentParameter(j + 1,
								pStatement, dataElement, javaType);

					}
					pStatement.execute();
					row = complexReader.getRowDataVectorFromStream();
				}
			}

		} catch (SQLException sqle) {
			conn.rollback();
			pStatement.close();
			conn.close();
			throw sqle;
		} catch (IllegalArgumentException le) {
			conn.rollback();
			pStatement.close();
			conn.close();
			throw le;
		} catch (Exception ue) {
			conn.rollback();
			pStatement.close();
			conn.close();
			throw ue;
		}
		conn.commit();
		pStatement.close();
		conn.close();
	}// loadDataIntoTable

	/*
	 * Method for generate sql command to create table
	 */
	private synchronized String generateDDLForOneEntity(String tableType,
			String tableName, Entity table) throws SQLException,
			UnresolvableTypeException {
		StringBuffer sql = new StringBuffer();
		String textFileName = table.getFileName();
		int headLineNumber = table.getNumHeaderLines();
		String orientation = table.getOrientation();
		String delimiter = table.getDelimiter();
		sql.append(tableType);
		sql.append(SPACE);
		sql.append(tableName);
		sql.append(LEFTPARENTH);
		Attribute[] attributeList = table.getAttributes();
		String attributeSql = parseAttributeList(attributeList);
		sql.append(attributeSql);
		sql.append(RIGHTPARENTH);
		sql.append(SEMICOLON);
		String sqlStr = sql.toString();
		if (isDebugging) {
			log.debug("The command to create tables is " + sqlStr);
		}
		return sqlStr;
	}// generateDDLForOneEntity

	/*
	 * Add attribute defination in create table command. If one attribute is
	 * null or has same error an exception will be throw
	 */
	private synchronized String parseAttributeList(Attribute[] list)
			throws SQLException, UnresolvableTypeException {
		StringBuffer attributeSql = new StringBuffer();
		if (list == null || list.length == 0) {
			log.debug("There is no attribute defination in entity");
			throw new SQLException("There is no attribute defination in entity");
		}
		int size = list.length;
		DBDataTypeResolver dataTypeResolver = new DBDataTypeResolver();
		boolean firstAttribute = true;
		for (int i = 0; i < size; i++) {
			Attribute attribute = list[i];
			if (attribute == null) {
				log.debug("One attribute defination is null attribute list");
				throw new SQLException(
						"One attribute defination is null attribute list");
			}
			String name = attribute.getName();
			String dataType = attribute.getDataType();
			String dbDataType = dataTypeResolver.resolveDBType(dataType);
			String javaDataType = dataTypeResolver.resolveJavaType(dataType);
			dbJavaDataTypeList.add(javaDataType);
			if (!firstAttribute) {
				attributeSql.append(COMMA);
			}
			attributeSql.append(QUOTE);
			attributeSql.append(name);
			attributeSql.append(QUOTE);
			attributeSql.append(SPACE);
			attributeSql.append(dbDataType);
			firstAttribute = false;

		}// for
		return attributeSql.toString();
	}// parseAttributeList

	/*
	 * Generate a sql command to for insert data into talbe. Here we use
	 * PreparedStatement.
	 */
	private synchronized String generateInsertCommand(String tableName,
			Entity table) throws SQLException {
		StringBuffer sql = new StringBuffer();
		sql.append(INSERT);
		sql.append(SPACE);
		sql.append(tableName);
		sql.append(LEFTPARENTH);
		Attribute[] list = table.getAttributes();
		if (list == null || list.length == 0) {
			log.debug("There is no attribute defination in entity");
			throw new SQLException("There is no attribute defination in entity");
		}
		int size = list.length;
		// cloumna name part
		boolean firstAttribute = true;
		for (int i = 0; i < size; i++) {
			Attribute attribute = list[i];
			if (attribute == null) {
				log.debug("One attribute defination is null attribute list");
				throw new SQLException(
						"One attribute defination is null attribute list");
			}
			String name = attribute.getName();
			if (!firstAttribute) {
				sql.append(COMMA);
			}
			sql.append(name);
			firstAttribute = false;
		}
		sql.append(RIGHTPARENTH);
		sql.append(SPACE);
		sql.append(VALUES);
		sql.append(SPACE);
		sql.append(LEFTPARENTH);
		// value part, use ? replace
		firstAttribute = true;
		for (int i = 0; i < size; i++) {
			if (!firstAttribute) {
				sql.append(COMMA);
			}
			sql.append(QUESTION);
			firstAttribute = false;
		}
		sql.append(RIGHTPARENTH);
		sql.append(SEMICOLON);
		if (isDebugging) {
			log.debug("The insert command is " + sql.toString());
		}
		return sql.toString();
	}

	/*
	 * Method to setup data for prepare statment
	 */
	private synchronized PreparedStatement setupPreparedStatmentParameter(
			int index, PreparedStatement pStatement, String data,
			String javaDataType) throws SQLException,
			UnresolvableTypeException, IllegalArgumentException {
		if (pStatement == null) {
			return pStatement;
		}

		// get rid of white space
		if (data != null) {
			data = data.trim();
		}

		// set default type as string
		if (javaDataType == null) {
			pStatement.setString(index, data);
		} else {

			if (javaDataType.equals(STRING)) {
				pStatement.setString(index, data);
			} else if (javaDataType.equals(INTEGER)) {
				pStatement.setInt(index, (new Integer(data)).intValue());
			} else if (javaDataType.equals(DOUBLE)) {
				pStatement.setDouble(index, (new Double(data)).doubleValue());
			} else if (javaDataType.equals(FLOAT)) {
				pStatement.setFloat(index, (new Float(data)).floatValue());
			} else if (javaDataType.equals(BOOLEAN)) {
				pStatement
						.setBoolean(index, (new Boolean(data)).booleanValue());
			} else if (javaDataType.equals(LONG)) {
				pStatement.setLong(index, (new Long(data)).longValue());
			} else if (javaDataType.equals(DATETIME)) {
				pStatement.setTimestamp(index, Timestamp.valueOf(data));
			} else {
				throw new UnresolvableTypeException(
						"This java type "
								+ javaDataType
								+ " has NOT implement in "
								+ "DBTablesGenerator.setupPreparedStatmentParameter method");
			}
		}
		return pStatement;
	}// setupPreparedStatmentParameter

	private synchronized String generateBindTextFileToTableSQL(
			String tableName, String textFilePath, String delimiter,
			boolean ignoreFirstLine) throws SQLException {
		if (textFilePath == null || textFilePath.trim().equals("")) {
			log.debug("No file location specify for this text table");
			throw new SQLException(
					"No file location specify for this text table");
		}
		if (delimiter == null) {
			throw new SQLException("No delimiter be specified in metadata");
		}
		StringBuffer sql = new StringBuffer();
		sql.append(SETTABLE);
		sql.append(SPACE);
		sql.append(tableName);
		sql.append(SPACE);
		sql.append(SOURCE);
		sql.append(SPACE);
		sql.append(QUOTE);
		sql.append(textFilePath);
		sql.append(SEMICOLON);
		// delimiter part
		sql.append(FIELDSEPATATOR);
		sql.append(delimiter);

		if (ignoreFirstLine) {
			sql.append(SEMICOLON);
			sql.append(IGNOREFIRST);
		}
		sql.append(QUOTE);
		if (isDebugging) {
			log.debug("The set source command is " + sql.toString());
		}
		return sql.toString();
	}

}// DBTablesGenerator