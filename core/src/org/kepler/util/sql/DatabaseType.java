/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-04-15 15:23:41 -0700 (Mon, 15 Apr 2013) $' 
 * '$Revision: 31920 $'
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

package org.kepler.util.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import util.StringUtil;

/**
 *
 * This class provides utility routines for accessing SQL databases. It
 * cannot be directly instantiated: it has several abstract methods that
 * must be implemented for a specific database engine such as MySQL.
 *
 * @author Daniel Crawl
 * @version $Id: DatabaseType.java 31920 2013-04-15 22:23:41Z crawl $
 *
 */
    
public abstract class DatabaseType
{
    protected DatabaseType()
    {
        _fkCounter = 0; 
        _tablePrefix = null;
    }
        
    /** Add the minor column to the version table with a specific value. */
    public void addMinorVersion(int minorVersion) throws SQLException
    {
    	Statement statement = getStatement();
    	try
    	{
    		Schema schema = new Schema(0);
            Table versionTable = schema.createTable(DatabaseType.VERSION_TABLE_NAME);
            versionTable.putColumn("minor", Column.INTEGER, String.valueOf(minorVersion));
            createColumn(versionTable.getColumn("minor"), statement);
    	}
    	finally
    	{
    		if(statement != null)
    		{
    			statement.close();
    		}
    	}
    }
    
    /** Change the type of a column. */
    public void changeColumnType(Column newColumn, Column oldColumn) throws SQLException
    {
        String tableName = newColumn.getTable().getName();
                
        String columnName = newColumn.getName();
        String tmpColumnName = columnName + "_new_TMP1234";
        
        // create a new column with a temporary name
        newColumn.setName(tmpColumnName);
        // do not enable null constraint since column will initially be empty
        createColumn(newColumn, false);
        
        // copy the data
        copyColumn(tmpColumnName, columnName, tableName);
        
        // drop the old column
        deleteColumn(columnName, tableName);
        
        // rename the new column back to original name
        newColumn.setName(columnName);
        renameColumn(tmpColumnName, newColumn, tableName);
        
        // enable nullable if column has that constraint
        if(!newColumn.isNullAllowed())
        {
            setColumnNotNull(newColumn, tableName);
        }
    }
    
    /** Commit the current transaction. */
    public void commit() throws SQLException
    {
        _connection.commit();
    }

    /** Connect to a database. */
    public void connect(Map<String,String> parameters) throws SQLException
    {	
        _connectInitialize(parameters.get(DatabaseFactory.Parameter.TABLEPREFIX.getName()));

        String hostName = parameters.get(DatabaseFactory.Parameter.HOST.getName());
        String port = parameters.get(DatabaseFactory.Parameter.PORT.getName());
        String dbName = parameters.get(DatabaseFactory.Parameter.NAME.getName());
        String userName = parameters.get(DatabaseFactory.Parameter.USER.getName());
        String passwd = parameters.get(DatabaseFactory.Parameter.PASSWD.getName());
        String url = parameters.get(DatabaseFactory.Parameter.JDBC_URL.getName());
        
        // use the jdbc url if not empty
        if(url != null && url.length() > 0)
        {
            _connection = _getConnection(url);
        }
        else
        {
            try
            {
                port = _getPort(port, dbName, hostName);
            }
            catch (IOException e)
            {
                throw new SQLException("Error chosing random port.", e);
            }
            
            url = _getJDBCUrl(hostName, port, dbName);
            _connection = _getConnection(url, port, dbName, userName, passwd);
        }
    
        String createIndexes = parameters.get(DatabaseFactory.Parameter.CREATE_INDEXES.getName());
        if(createIndexes == null || createIndexes.trim().isEmpty() || createIndexes.equals("true"))
        {
            _createIndexes = true;
        }
    }

    /** Copy the contents between columns in the same table. */
    public void copyColumn(String destName, String srcName, String tableName) throws SQLException
    {
        String sqlStr = "UPDATE " + getTableName(tableName) +
            " SET " + getColumnName(destName) + " = " + getColumnName(srcName);
        _executeSQL(sqlStr);
    }

    /** Adds a column to an existing table in the database. 
     *  @param column the column to create 
     *  @param allowNullable If true and column can be nullable, column is
     *  created with nullable constraint.
     */
    public void createColumn(Column column, boolean allowNullable) throws SQLException
    {
        Statement statement = null;
        try
        {
            statement = _connection.createStatement();
            createColumn(column, statement, allowNullable);
        }
        finally
        {
            if(statement != null)
            {
                statement.close();
            }
        }
    }
    /** Adds a column to an existing table in the database.
     *  @param column the column to create
     *  @param statement the Statement to use to execute the SQL
     */
    public void createColumn(Column column, Statement statement) throws SQLException
    {
        createColumn(column, statement, true);
    }
    
    /** Adds a column to an existing table in the database.
     *  @param column the column to create
     *  @param statement the Statement to use to execute the SQL
     *  @param allowNullable If true and column can be nullable, column is
     *  created with nullable constraint. 
     */
    public void createColumn(Column column, Statement statement, boolean allowNullable) throws SQLException
    { 
        String tableName = getTableName(column.getTable().getName());
        StringBuilder str = new StringBuilder("ALTER TABLE ");
        str.append(tableName);
        str.append(" ADD "); //COLUMN ");
        str.append(getColumnDefinition(column, allowNullable));
        //System.out.println("WARNING: Adding missing column " + tableName +
        //    "." + column.getName());
        //System.out.println(str);
        statement.execute(str.toString());
    }

    /** Create tables in the database based on a Schema. Automatically
     *  adds the version table to the database and schema definition.
     *  @param schema the schema definition.
     */
    public void createTables(Schema schema) throws SQLException
    {
        createTables(schema, true);
    }
        
    /** Create tables in the database based on a Schema. 
     *  @param schema the schema definition
     *  @param createVersion if true, add the version table into the
     *  schema definition and insert the the version information into the
     *  table.
     */
    public void createTables(Schema schema, boolean createVersion) throws SQLException
    {
        Statement statement = null;
        try
        {
            statement = _connection.createStatement();

            // create each table
            for(Table table : schema.tables())
            {
                _createTable(table, statement);
            }
      
            // add constraints
            // we do this after adding all the tables since a constraint
            // may refer to another table, e.g., a foreign key.
            for(Table table : schema.tables())
            {
                _createTableConstraints(table, statement);
            }
            
            if(createVersion)
            {
                createVersionTable(schema.getMajorVersion(), schema.getMinorVersion());
            }
        }
        finally
        {
            if(statement != null)
            {
                statement.close();
            }
        }
    }

    /** Create a version table in the database. */
    public void createVersionTable(int majorVersion, int minorVersion) throws SQLException
    {
        Schema schema = new Schema(majorVersion);
        schema.setMinorVersion(minorVersion);
        
        // add the version table.
        Table versionTable = schema.createTable(DatabaseType.VERSION_TABLE_NAME);
        versionTable.putColumn("version", Column.PK_INTEGER);
        versionTable.putColumn("minor", Column.INTEGER);
        //versionTable.putConstraint("version_constraint", "version", " = " + _version);
        
        createTables(schema, false);

        Statement statement = null;
        try
        {
            statement = _connection.createStatement();
            // insert the version values into the version table
            int result = statement.executeUpdate("INSERT INTO " +
                getTableName(VERSION_TABLE_NAME) + 
                "(version, minor) VALUES (" +
                majorVersion + ", " + minorVersion + ")");
        
            if(result != 1)
            {
                throw new SQLException("Unable to insert into version table.");
            }
        }
        finally
        {
            if(statement != null)
            {
                statement.close();
            }
        }
    }
    
    /** Delete a column from a table. */
    public void deleteColumn(String columnName, String tableName) throws SQLException
    {
        String sqlStr = "ALTER TABLE " + getTableName(tableName) +
            " DROP COLUMN " + getColumnName(columnName);
        _executeSQL(sqlStr);
    }

    /** Close a JDBC connection. */
    public void disconnect() throws SQLException
    {
        for(PreparedStatement statement : _preparedStatements)
        {
        	statement.close();
        }
        _preparedStatements.clear();
        
        // NOTE: we do not close these since it is in _preparedStatements
        // and closed above.
        _psGetMajorVersion = null;
        _psGetMinorVersion = null;
        _psUpdateMinorVersion = null;


        if(_connection != null)
        {
            _connection.close();
            _connection = null;
        }
    }

    /** Get the string used in SQL statements to modify the
     *  data type of a column.
     */
    public String getColumnAlterStr()
    {
        return "MODIFY";
    }
    
    /** Adjust the name of a column. */
    public String getColumnName(String columnName)
    {
        if(_needCapitalColumnNames())
        {
            return columnName.toUpperCase();
        }
        else
        {
            return columnName;
        }
    }

    /** Adjust the name of multiple (comma-separated) columns. */
    public String getColumnNames(String columnNames)
    {
        String[] names = columnNames.split(",\\s*");
        for(int i = 0; i < names.length; i++)
        {
            names[i] = getColumnName(names[i]);
        }
        return StringUtil.join(names, ", ");
    }

    /** Get the size of a column in a table. */
    public int getColumnSize(String tableName, String columnName)
        throws SQLException
    {
        int retval = -1;
       
        DatabaseMetaData metadata = _connection.getMetaData();
            
        String catalogName = null;
        if(_hasCatalog())
        {
            catalogName = _connection.getCatalog();
        }

        ResultSet result = metadata.getColumns(catalogName, null, 
            getTableName(tableName), null);

        String adjustedColumnName = getColumnName(columnName);

        while(result.next())
        {
            String curName = result.getString("COLUMN_NAME");
            if(curName.equalsIgnoreCase(adjustedColumnName))
            {
                retval = result.getInt("COLUMN_SIZE");
                break;
            }
        }
        result.close();

        if(retval == -1)
        {
            throw new SQLException("Could not find size of column " + 
                columnName + " in table " + tableName);
        }

        return retval;
    }

    /** Get the JDBC Connection object. */
    public Connection getConnection()
    {
    	return _connection;
    }
    
    /** Get a string representation of the false value for a boolean column. */
    public String getFalseValue()
    {
        return "0";
    }
    
    /** Get a string representing the default time. */
    public String getDefaultTimeStr()
    {
        return "0";
    }

    /** Get the name of the database type. */
    abstract public String getName();

    /** Get a PreparedStatement from an SQL string. All PreparedStatements
     *  returned by this method are closed when disconnect() is called.
     *  
     *  NOTE: this method returns different PreparedStatements when
     *  called with the same SQL statement so that they may be executed
     *  in parallel.
     *  
     */
    public PreparedStatement getPrepStatement(String str) throws SQLException
    {
        PreparedStatement retval = null;

        //System.out.println("creating PS: " + str);

        // see if database type supports auto-incremented keys
        if(str.toUpperCase().startsWith("INSERT") && _hasGeneratedKeys())
        {
            retval = _connection.prepareStatement(str,
                Statement.RETURN_GENERATED_KEYS);
        }
        else
        {
            retval = _connection.prepareStatement(str);
        }
        
        _preparedStatements.add(retval);

        return retval;
    }
    
    /** Get the primary file extension of the database. If the database
     *  is not file-based, this returns null.
     */
    public String getPrimaryFileExtension()
    {
    	return null;
    }

    /** Create a PreparedStatement for an SQL INSERT. */
    public PreparedStatement getSQLInsert(String tableName, String columnNames,
        String valueNames) throws SQLException
    {
        return getSQLInsert(tableName, null, columnNames, valueNames);
    }

    /** Create a PreparedStatement for an SQL INSERT that uses an
     *  auto-increment column.
     */
    public PreparedStatement getSQLInsert(String tableName, String incName,
        String columnNames, String valueNames) throws SQLException
    {
        StringBuilder str = new StringBuilder("INSERT INTO ");
        str.append(getTableName(tableName));
        str.append(" (");

        if(incName != null && _needSequencesForAutoInc())
        {
            str.append(incName);
            str.append(", ");
        }

        str.append(getColumnNames(columnNames));
        str.append(") VALUES (");

        if(incName != null && _needSequencesForAutoInc())
        {
            str.append(_getSequenceName(tableName, incName));
            str.append(".nextval, ");
        }

        str.append(valueNames);
        str.append(")");
        
        return getPrepStatement(str.toString());
    }

    /** Create a PreparedStatement for an SQL SELECT with no conditions.
     *  @param tableName the name of the table to query
     *  @param columnNames a comma-separated list of columns to query
     */
    public PreparedStatement getSQLSelect(String tableName, String columnNames)
        throws SQLException
    {
        return getSQLSelect(tableName, columnNames, null);
    }

    /** Create a PreparedStatement for an SQL SELECT.
     *  @param tableName the name of the table to query
     *  @param columnNames a comma-separated list of columns to query
     *  @param conditions a comma-separated list of conditions. This is
     *  optional and may be null.
     */
    public PreparedStatement getSQLSelect(String tableName, String columnNames,
        String conditions) throws SQLException
    {
        StringBuilder str = new StringBuilder("SELECT ");
        str.append(getColumnNames(columnNames));
        str.append(" FROM ");
        str.append(getTableName(tableName));
        if(conditions != null)
        {
            str.append(" WHERE ");
            str.append(conditions);
        }
        return getPrepStatement(str.toString());
    }

    /** Create a PrepareStatement for an SQL UPDATE. */ 
    public PreparedStatement getSQLUpdate(String tableName, String columnNames,
        String conditions) throws SQLException
    {
        StringBuilder str = new StringBuilder("UPDATE ");
        str.append(getTableName(tableName));
        str.append(" SET ");
        str.append(getColumnNames(columnNames));
        if(conditions != null)
        {
        	str.append(" WHERE " );
        	str.append(conditions);
        }
        return getPrepStatement(str.toString());
    }
    
    /** Create a PrepareStatement for an SQL DELETE. */ 
    public PreparedStatement getSQLDelete(String tableName, String conditions) 
    	throws SQLException
    {
        StringBuilder str = new StringBuilder("DELETE ");
        str.append(" FROM ");
        str.append(getTableName(tableName));
        str.append(" WHERE " );
        str.append(conditions);
        return getPrepStatement(str.toString());
    }
 
    /** Get a Statement. */
    public Statement getStatement() throws SQLException
    {
        return _connection.createStatement();
    }

    /** Get the name for a table, possibly adding a suffix. */
    public String getTableName(String name)
    {
        String retval = null;
        if(_tablePrefix != null)
        {
            retval = _tablePrefix + name;
        }
        else
        {
            retval = name;
        }

        if(_needCapitalTableNames())
        {
            return retval.toUpperCase();
        }
        else
        {
            return retval;
        }
    }
    
    /** Get the major version of the schema from the version table. If the
     *  version table is not found, returns null.
     */    
    public Integer getMajorVersion() throws SQLException
    {
        return _getVersion(true);
    }

    /** Get the minor version of the schema from the version table. If the
     *  version table is not found, returns null.
     */    
    public Integer getMinorVersion() throws SQLException
    {
        return _getVersion(false);
    }
    
    /** Get a string of the version from the version table. */
    public String getVersionString() throws SQLException
    {
    	Integer major = getMajorVersion();
    	Integer minor = getMinorVersion();
    	
    	String retval;
    	
    	if(major == null)
    	{
    		retval = "unknown.";
    	}
    	else
    	{
    		retval = major + ".";
    	}
    	
    	if(minor == null)
    	{
    		retval += "unknown";
    	}
    	else
    	{
    		retval += minor;
    	}
    	
    	return retval;
    }
    
    /** Get the major or minor version of the schema from the version table.
     *  Returns null if either the version table is not found, or if getting
     *  the minor version and the minor column does not exist.
     *  @param major If true, returns the major version, otherwise returns the
     *  minor version.
     */
    private Integer _getVersion(boolean major) throws SQLException
    {
    	PreparedStatement query = null;
    	
        // see if version table exists
        if(tableExists(VERSION_TABLE_NAME))
        {
            // if we're getting the minor version, see if minor column exists
            if(!major)
            {
            	if(_psGetMinorVersion == null)
            	{
	                Set<String> columns = _getExistingColumnNames(VERSION_TABLE_NAME);
	                if(!columns.contains(getColumnName("minor")))
	                {
	                    return null;
	                }
	                else
	                {
	                	_psGetMinorVersion = getSQLSelect(VERSION_TABLE_NAME, "minor");
	                }
            	}
            	
            	query = _psGetMinorVersion;
            }
            else
            {
            	if(_psGetMajorVersion == null)
	            {
	            	_psGetMajorVersion = getSQLSelect(VERSION_TABLE_NAME, "version");
	            }
            	
            	query = _psGetMajorVersion;
            }
        }
        
        if(query != null)
        {
            ResultSet result = null;
            try
            {
                result = query.executeQuery();
                
                if(result.next())
                {
                    if(major)
                    {    
                        return result.getInt(1);
                    }
                    else
                    {
                        return result.getInt(1);
                    }
                }
            }
            finally
            {
                if(result != null)
                {
                    result.close();
                }
            }
        }
        
        // version was not found
        return null;
    }
    
    /** Returns true of the database schema is newer than the schema definition.
     *  @param schema the schema definition.
     */
    public boolean hasNewerSchema(Schema schema) throws SQLException
    {
        int schemaMajorVersion = schema.getMajorVersion();
        int schemaMinorVersion = schema.getMinorVersion();
        
        Integer dbMajorVersion = getMajorVersion();
        Integer dbMinorVersion = getMinorVersion();
        
        if(dbMajorVersion == null)
        {
            throw new SQLException("Database schema major version not found.");
        }
        else if(dbMinorVersion == null)
        {
            throw new SQLException("Database schema minor version not found.");            
        }

        return (schemaMajorVersion < dbMajorVersion ||
                (schemaMajorVersion == dbMajorVersion &&
                schemaMinorVersion < dbMinorVersion));
    }
    
    /** Returns true if the database schema is older than the schema definition.
     *  @param schema the schema definition
     */
    public boolean hasOlderSchema(Schema schema) throws SQLException
    {
        int schemaMajorVersion = schema.getMajorVersion();
        int schemaMinorVersion = schema.getMinorVersion();
        
        Integer dbMajorVersion = getMajorVersion();
        Integer dbMinorVersion = getMinorVersion();
        
        if(dbMajorVersion == null)
        {
            throw new SQLException("Database schema major version not found.");
        }
        else if(dbMinorVersion == null)
        {
        	addMinorVersion(0);
        	dbMinorVersion = 0;
        }

        return (schemaMajorVersion > dbMajorVersion ||
                (schemaMajorVersion == dbMajorVersion &&
                schemaMinorVersion > dbMinorVersion));
    }
    
    /** Execute a prepared statement that returns an auto-incremented key.
     *  @param prepStmt PreparedStatement to be executed
     *  @param tableName the table name
     *  @param incName the name of the auto-incremented column
     */
    public synchronized int insert(PreparedStatement prepStmt, String tableName,
        String incName) throws SQLException
    {
        Statement statement = null;
        ResultSet result = null;
     
        int rowCount = prepStmt.executeUpdate();
        if(rowCount != 1)
        {
            throw new SQLException("SQL insert did not appear to take place " +
                " since row count is : " + rowCount);
        }

        try
        {
            // see if database supports auto-incremented columns
            if(_hasGeneratedKeys())
            {
                result = prepStmt.getGeneratedKeys();
            }
            else if(_needIdentityForAutoInc())
            {
                //XXX cache this as a prepared statement
                statement = _connection.createStatement();
                statement.execute("CALL IDENTITY()");
                result = statement.getResultSet();
            }
            else if(_needSequencesForAutoInc())
            {
                //XXX cache this
                statement = _connection.createStatement();
                String sequenceName = _getSequenceName(tableName, incName);
                //XXX dual is oracle specific
                statement.execute("SELECT " + sequenceName + ".currval from dual");
                result = statement.getResultSet();
            }

            if(result == null || !result.next())
            {
                throw new SQLException("No returned auto-increment");
            }
            
            if(_isAutoIncResultReferencedByName())
            {
                return result.getInt(incName);
            }
            else
            {
                return result.getInt(1);
            }
        }
        finally
        {
            if(result != null)
            {
                result.close();
            }

            if(statement != null)
            {
                statement.close();
            }
        }
    }

    /** Returns true if database name should be an absolute path in the
     *  file system.
     */
    public boolean needAbsolutePathForName()
    {
        return false;
    }
    
    /** Returns true if need host name for connect. */
    public boolean needHostForConnect()
    {
        return true;
    }
    
    /** Returns true if need password for connect. */
    public boolean needPasswordForConnect()
    {
        return true;
    }

    /** Returns true if need user name for connect. */
    public boolean needUserForConnect()
    {
        return true;
    }

    /** See if a stored procedure name exists in the database. */
    public boolean procedureExists(String procedureName) throws SQLException
    {
        boolean retval = false;

        DatabaseMetaData metadata = _connection.getMetaData();

        String catalogName = null;
        if(_hasCatalog())
        {
            catalogName = _connection.getCatalog();
        }
        ResultSet result = metadata.getProcedures(catalogName, null, null);

    	System.out.println("checking if exists " + procedureName);
        
        while(result.next())
        {
        	System.out.println("PROCEDURE = " + result.getString("PROCEDURE_NAME"));
            if(procedureName.equalsIgnoreCase(result.getString("PROCEDURE_NAME")))
            {
                retval = true;
                break;
            }
        }

        result.close();

        return retval;
    }

    /** Rename a column. */
    abstract public void renameColumn(String oldName, Column newColumn, String tableName) throws SQLException;

    /** Set not null constraint to a column. */
    abstract public void setColumnNotNull(Column column, String tableName) throws SQLException;

    /** See if a table name exists in the database. */
    public boolean tableExists(String tableName) throws SQLException
    {
        boolean retval = false;
       
        DatabaseMetaData metadata = _connection.getMetaData();

        String catalogName = null;
        if(_hasCatalog())
        {
            catalogName = _connection.getCatalog();
        }
        //System.out.println("going to get tables for catalog = " + catalogName);
        ResultSet result = metadata.getTables(catalogName, null, null, null);
        //System.out.println("got tables");

        String realTableName = getTableName(tableName);

        // check the results
        while(result.next())
        {
            //System.out.println("checking table:");
            //System.out.println("  table_cat = " + result.getString("TABLE_CAT"));
            //System.out.println("  table_name = " + result.getString("TABLE_NAME"));
            if(realTableName.equalsIgnoreCase(result.getString("TABLE_NAME")))
            {
                retval = true;
                break;
            }
        }
        result.close();

        return retval;
    }
    
    /** See if a table exists in the database. */
    public boolean tableExists(Table table) throws SQLException
    {
        return tableExists(table.getName());
    }
    
    /** Update the minor version in version table. */
    public void updateMinorVersion(int minor) throws SQLException
    {    	
    	if(_psUpdateMinorVersion == null)
    	{
    		_psUpdateMinorVersion = getSQLUpdate(VERSION_TABLE_NAME, "minor = ?", null);
    	}
    	
    	_psUpdateMinorVersion.setInt(1, minor);
    	
    	if(_psUpdateMinorVersion.executeUpdate() != 1)
    	{
    		throw new SQLException("Unable to update minor version.");
    	}
    }
  
    /** The name of the version table. */
    public static final String VERSION_TABLE_NAME = "version_table";

    ///////////////////////////////////////////////////////////////////
    // protected methods
    
    /** Returns true if foreign keys are automatically indexed. */
    protected boolean _areForeignKeysIndexed()
    {
        return true;
    }

    /** Returns true if primary keys are automatically indexed. */
    protected boolean _arePrimaryKeysIndexed()
    {
        return true;
    }
    
    /** Execute an SQL statement. */
    protected void _executeSQL(String sqlStr) throws SQLException 
    {
        Statement statement = null;
        try
        {
            statement = getStatement();
            statement.execute(sqlStr);
        }
        finally
        {
            if(statement != null)
            {
                statement.close();
            }
        }
    }

    /** Get a connection to the database. */
    protected Connection _getConnection(String jdbcURL) throws SQLException
    {
        return DriverManager.getConnection(jdbcURL);
    }
    
    /** Get a connection to the database. */
    protected Connection _getConnection(String url, String dbPort, 
        String databaseName, String userName, String passwd) throws SQLException
    {
        return DriverManager.getConnection(url, userName, passwd);
    }
    
    /** Get any suffix used when creating tables. */
    protected String _getCreateTableSuffix()
    {
        return null;
    }
    
    protected String _combineHostAndPort(String host, String port)
    {
        if(port != null && port.length() > 0)
        {
            return host + ":" + port;
        }
        return host;
    }

    /** Get a JDBC URL. */
    abstract protected String _getJDBCUrl(String hostName, String port,
        String databaseName) throws SQLException;

    /** Get the driver class name. */
    abstract protected String _getDriverName();

    /** Get a port number. In the base class, returns the dbPort argument. */
    protected synchronized String _getPort(String dbPort, String dbName, String hostName) throws IOException
    {
        return dbPort;
    }

    /** Get the SQL string of a column type. */
    abstract protected String _getTypeString(Column column);

    /** Returns true if database uses a catalog. */
    protected boolean _hasCatalog()
    {
        return true;
    }

    /** Returns true if database supports auto-generated keys in its prepared
     *  statements.
     */
    abstract protected boolean _hasGeneratedKeys();

    /** Initialize data structures and load driver before connecting. */
    protected void _connectInitialize(String tablePrefix) throws SQLException
    {
        // disconnect if already connected
        disconnect();

        _tablePrefix = tablePrefix;

        String driverName = _getDriverName();
        try
        {
            Class.forName(driverName).newInstance();
        }
        catch(ClassNotFoundException e)
        {
			throw new SQLException("The JDBC class " + driverName
					+ " was not found on the classpath. One "
					+ "possible reason for this error is "
					+ "the JDBC jar is missing. Several JDBC "
					+ "jars are released under licenses "
					+ "incompatible with Kepler's license such "
					+ "as the Oracle JDBC jar. In "
					+ "these cases, the jar must be downloaded "
					+ "manually and placed in core/lib/jar/dbdrivers.");

        }
        catch(InstantiationException e)
        {
            throw new SQLException("Could not instantiate " + driverName +
                ": " + e.getMessage());
        }
        catch(IllegalAccessException e)
        {
            throw new SQLException("IllegalAccessException: " + e.getMessage());
        }
    }
    
    /** Returns true if identifier is too long. */
    protected boolean _isIdentifierTooLong(String identifier)
    {
        return false;
    }
   
    /** Returns true if an auto-incremented column value
     *  in a ResultSet must be referenced with the name
     *  instead of position.
     */
    protected boolean _isAutoIncResultReferencedByName()
    {
        return false;
    }
    
    /** Returns true if table is cached. */
    protected boolean _isTableCached()
    {
        return false;
    }

    /** Returns true if column names should be capitalized. */
    protected boolean _needCapitalColumnNames()
    {
        return false;
    }

    /** Returns true if table names should be capitalized. */
    protected boolean _needCapitalTableNames()
    {
        return false;
    }
    
    /** Returns true if need to call IDENTITY() after INSERTs with
     *  autoincrement columns.
     */
    protected boolean _needIdentityForAutoInc()
    {
        return false;
    }

    /** Returns true if need to use sequences for autoincrement columns. */
    protected boolean _needSequencesForAutoInc()
    {
        return false;
    }

    ///////////////////////////////////////////////////////////////////
    // protected variables
    
    /** The JDBC connection object. */
    protected Connection _connection;
    
    /** If false, do not create indexes. */
    protected boolean _createIndexes = false;

    ///////////////////////////////////////////////////////////////////
    // private methods

    /** Create an index on a table and column(s). */
    public void createIndex(String indexName, String tableName,
        String columnName) throws SQLException
    {
        createIndex(indexName, tableName, columnName, Table.IndexType.Default);
    }
    
    /** Create a specific type of index on a table and column(s).
     * NOTE: currently only the default type of index is created. 
     */
    public void createIndex(String indexName, String tableName,
            String columnName, Table.IndexType type) throws SQLException
    {        
        StringBuilder str = new StringBuilder("CREATE INDEX ");
        if(indexName != null)
        {
            str.append(indexName);
        }
        else
        {
            str.append(getTableName(tableName) + "_" + columnName.replaceAll("[,\\s]", "_") + "_idx");
        }
        str.append(" ON ");
        str.append(getTableName(tableName));
        str.append(" (");
        str.append(getColumnName(columnName));
        str.append(")");

        //System.out.println(str);
        _executeSQL(str.toString());
    }
    
    /** Get the SQL definition of a column. */
    public StringBuilder getColumnDefinition(Column column) throws SQLException
    {
        return getColumnDefinition(column, true);
    }
    
    /** Get the SQL definition of a column.
     *  @param column the column 
     *  @param allowNullable If true and the column can be null, then the returned 
     *  definition contains the nullable constraint.
     */
    public StringBuilder getColumnDefinition(Column column, boolean allowNullable) throws SQLException
    {
        String columnName = column.getName();
        StringBuilder retval = new StringBuilder(getColumnName(columnName));

        String typeStr = _getTypeString(column);
        if(typeStr == null)
        {
            throw new SQLException("Column " + columnName +
                " has unsupported type.");
        }

        retval.append(" " + typeStr);
   
        // see if column type has length
        int length = column.getLength();
        if(length > 0)
        {
            retval.append("(" + length + ")");
        }

        // see if there's a default value
        String defaultValue = column.getDefaultValue();
        if(defaultValue != null)
        {
            retval.append(" DEFAULT '" + defaultValue + "'");
        }

        // see if null is allowed
        if(allowNullable && !column.isNullAllowed())
        {
                retval.append(" NOT NULL");
        }
        
        return retval;
    }

    /** Create a table in the database including all of its columns,
     *  primary keys, and indices for foreign keys. Foreign keys are
     *  <b>not</b> created since the reference table may not exist yet.
     */
    private void _createTable(Table table, Statement statement) throws SQLException
    {
        String tableName = table.getName();
        Set<String> primaryKeys = new HashSet<String>();
        Set<String> foreignKeys = new HashSet<String>();
    
        StringBuilder str = new StringBuilder("CREATE");

        if(_isTableCached())
        {
            str.append(" CACHED");
        }

        str.append(" TABLE " + getTableName(tableName) + "(");

        // add each column
        for(String columnName : table.columnNames())
        {
            Column column = table.getColumn(columnName);

            if(column.isPrimaryKey())
            {
                primaryKeys.add(columnName);
            }

            if(table.isForeignKey(columnName))
            {
                foreignKeys.add(columnName);
            }

            // create sequences if necessary
            if(_needSequencesForAutoInc() && column.isAutoIncrement())
            {
                String seqStr = "CREATE SEQUENCE " +
                    _getSequenceName(tableName, columnName);
                //System.out.println(seqStr);
                statement.execute(seqStr);
            }
        
            str.append(getColumnDefinition(column));

            str.append(", ");
        }

        boolean addPrimaryKey = _createIndexes;

        // if indexes are turned off, do not create primary keys.
        // however, if the primary key is an autoincrement column, create the
        // primary key since it is required for auto increment columns.
        if(primaryKeys.size() == 1)
        {
            final String primaryKeyName = primaryKeys.toArray(new String[0])[0];
            final Column primaryKeyColumn = table.getColumn(primaryKeyName);
            if(primaryKeyColumn.isAutoIncrement())
            {
                addPrimaryKey = true;
            }
        }

        // add any primary keys
        if(primaryKeys.size() > 0 && addPrimaryKey)
        {
            str.append("PRIMARY KEY (");
            for(String columnName : primaryKeys)
            {
                str.append(getColumnName(columnName));
                str.append(", "); 
            }
            // remove the last comma and space
            str.delete(str.length() - 2, str.length());

            str.append(")");
        }
        else
        {
            // remove the last comma and space
            str.delete(str.length() - 2, str.length());
        }

        str.append(") ");

        // append any database-specific suffix
        String tableSuffix = _getCreateTableSuffix();
        if(tableSuffix != null)
        {
            str.append(tableSuffix);
        }

        // create the table
        //System.out.println(str);
        statement.execute(str.toString());

        // see if we should create indexes
        if(_createIndexes)
        {
            // create indices for primary keys
            if(! _arePrimaryKeysIndexed() && primaryKeys.size() > 0)
            {
                for(String columnName : primaryKeys)
                {
                    createIndex(null, tableName, columnName);
                }
            }
    
            // create indices for foreign keys
            if(! _areForeignKeysIndexed())
            {
                for(String columnName : foreignKeys)    
                {
                    // if column is primary key, there already is an index.
                    if(! primaryKeys.contains(columnName))
                    {
                        createIndex(null, tableName, columnName);
                    }
                }
            }
            
            // create other indexes
            for(String indexName : table.indexes())
            {
                Table.IndexType type = table.getIndexType(indexName);
                String columnName = table.getIndexColumns(indexName);
                createIndex(indexName, tableName, columnName, type);
            }
        }
    }
    
    private String _getForeignKeyName(String name)
    {
        String retval = name;
        if(_isIdentifierTooLong(retval))
        {
            retval= "fk_" + _fkCounter;
            _fkCounter++;
        }
        
        if(_tablePrefix != null)
        {
            retval = _tablePrefix + retval;
        }
        
        return retval;
    }
    
    /** Add the foreign keys to a table. */
    private void _createTableConstraints(Table table, Statement statement) throws SQLException
    {
        String tableName = table.getName();
        
        for(String fkName : table.foreignKeyNames())
        {
            Table.ForeignKey fk = table.getForeignKey(fkName);

            StringBuilder str = new StringBuilder("ALTER TABLE ");
            str.append(getTableName(tableName));
            str.append(" ADD CONSTRAINT ");
            str.append(_getForeignKeyName(fkName));
            str.append(" FOREIGN KEY (");
            str.append(fk.getColumn());
            str.append(") REFERENCES ");
            str.append(getTableName(fk.getRefTable()));
            str.append(" (");
            str.append(fk.getRefColumn());
            str.append(")");
            str.append(" ON DELETE CASCADE");
            //System.out.println(str);
            statement.execute(str.toString());

        }

        /* FIXME make sure this works for all db types
        for(String constraintName : table.constraintNames())
        {
            Table.ColumnConstraint constraint = table.getConstraint(constraintName);
    
            str = new StringBuilder("ALTER TABLE ");
            str.append(getTableName(tableName));
            str.append(" ADD CONSTRAINT ");
            str.append(constraintName);
            str.append(" ");
            str.append(constraint.getColumnName());
            str.append(" ");
            str.append(constraint.getConstraintValue());
            //System.out.println(str);
            statement.execute(str.toString());
        }
        */
    }

    /** Get a list of columns for a table in the database. */
    private Set<String> _getExistingColumnNames(String tableName) throws SQLException
    {
        Set<String> retval = new HashSet<String>();
        
        DatabaseMetaData metadata = _connection.getMetaData();

        String catalogName = null;
        if(_hasCatalog())
        {
            catalogName = _connection.getCatalog();
        }
        ResultSet result = null;
        
        try
        {
            result = metadata.getColumns(catalogName, null, getTableName(tableName), null);
            
            // check the results
            while(result.next())
            {
                retval.add(result.getString("COLUMN_NAME"));
            }
            return retval;
        }
        finally
        {
            if(result != null)
            {
                result.close();
            }
        }
    }
    
    /** Get a list of tables in the database. */
    /* unused
    private Set<String> _getExistingTableNames() throws SQLException
    {
        Set<String> retval = new HashSet<String>();
        
        DatabaseMetaData metadata = _connection.getMetaData();

        String catalogName = null;
        if(_hasCatalog())
        {
            catalogName = _connection.getCatalog();
        }
        ResultSet result = null;
        
        try
        {
            result = metadata.getTables(catalogName, null, null, null);
            while(result.next())
            {
                retval.add(result.getString("TABLE_NAME"));
            }
            return retval;
        }
        finally
        {
            if(result != null)
            {
                result.close();
            }
        }
    }
    */
    
    /** Generate a name for a sequence given a table and column. */
    private String _getSequenceName(String tableName, String columnName)
    {
        return getTableName(tableName) + "_" + columnName + "_seq";
    }
    
    ///////////////////////////////////////////////////////////////////
    // private variables

    /** A counter for foreign key names. */
    private int _fkCounter;

    /** A set to hold open PreparedStatements objects. */
    private final Set<PreparedStatement> _preparedStatements = 
    	Collections.synchronizedSet(new HashSet<PreparedStatement>());

    /** Prepared statement to query the major version. */
    private PreparedStatement _psGetMajorVersion;

    /** Prepared statement to query the minor version. */
    private PreparedStatement _psGetMinorVersion;
    
    /** Prepared statement to update the minor version. */
    private PreparedStatement _psUpdateMinorVersion;

    /** Prefix for table names. */
    private String _tablePrefix;
}
