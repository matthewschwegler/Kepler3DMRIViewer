/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-01-19 14:41:30 -0800 (Thu, 19 Jan 2012) $' 
 * '$Revision: 29260 $'
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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * An implementation of DatabaseType for MySQL.
 *
 * @author Daniel Crawl
 * @version $Id: MySQL.java 29260 2012-01-19 22:41:30Z crawl $
 *
 */
    
public class MySQL extends DatabaseType
{
    /** Only this package (DatabaseFactory) can instantiate. */
    protected MySQL()
    {
        super();
    }
    
    /** Connect to a database. */
    public void connect(Map<String,String> parameters) throws SQLException
    {   
        _connectInitialize(parameters.get(DatabaseFactory.Parameter.TABLEPREFIX.getName()));

        String dbName = parameters.get(DatabaseFactory.Parameter.NAME.getName());
        
        // see if the jdbc url parameter is used
        String url = parameters.get(DatabaseFactory.Parameter.JDBC_URL.getName());
        if(url != null && url.length() > 0)
        {
            // get the database name from the url
            Matcher matcher = _databaseNamePattern.matcher(url);
            if(!matcher.find())
            {
                throw new SQLException("Could not determine database name in " +
                    "JDBC URL " + url);
            }
            
            // save the database name
            dbName = matcher.group(1);
            
            // make the connection without the database name since the catalog
            // may not yet exist
            String urlNoDB = matcher.replaceFirst("/\\?");
            _connection = _getConnection(urlNoDB);
        }
        else
        {
            // no jdbc url, so build one from the other parameters.
            String hostName = parameters.get(DatabaseFactory.Parameter.HOST.getName());
            String port = parameters.get(DatabaseFactory.Parameter.PORT.getName());
            String userName = parameters.get(DatabaseFactory.Parameter.USER.getName());
            String passwd = parameters.get(DatabaseFactory.Parameter.PASSWD.getName());

            url = _getJDBCUrl(hostName, port, "");
            _connection = _getConnection(url, port, dbName, userName, passwd);            
        }
        
        // create the catalog if it does not exist.
        _createCatalog(dbName);
        
        String createIndexes = parameters.get(DatabaseFactory.Parameter.CREATE_INDEXES.getName());
        if(createIndexes == null || createIndexes.trim().isEmpty() || createIndexes.equals("true"))
        {
            _createIndexes = true;
        }

    }
    
    /** Get the name of the type. */
    public String getName()
    {
        return "MySQL";
    }

    /** Rename a column. */
    public void renameColumn(String oldName, Column newColumn, String tableName) throws SQLException
    {        
        String sqlStr = "ALTER TABLE " + getTableName(tableName) +
        " CHANGE COLUMN " + getColumnName(oldName) + " " + getColumnDefinition(newColumn);
        _executeSQL(sqlStr);
    }

    /** Set not null constraint to a column. */
    public void setColumnNotNull(Column column, String tableName) throws SQLException
    {
        String sqlStr = "ALTER TABLE " + getTableName(tableName) +
        " CHANGE COLUMN " + getColumnName(column.getName()) + " " + getColumnDefinition(column);
        _executeSQL(sqlStr);
    }

    ///////////////////////////////////////////////////////////////////
    // protected methods
    
    /** Get any suffix used when creating tables. */
    protected String _getCreateTableSuffix()
    {
        return "ENGINE=InnoDB";
    }

    /** Get the driver class name. */
    protected String _getDriverName()
    {
        return "com.mysql.jdbc.Driver";
    }
    
    /** Get a JDBC URL. */
    protected String _getJDBCUrl(String hostName, String port,
            String databaseName) throws SQLException
    {
        // NOTE: we set zeroDateTimeBehavior so that if a date
        // time is set to all zeros, we want to return null
        // instead of throwing an exception.
        // (see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4308)
        
        String hostAndPort = _combineHostAndPort(hostName, port);
        return "jdbc:mysql://" + hostAndPort + "/" + databaseName +
            "?zeroDateTimeBehavior=convertToNull&rewriteBatchedStatements=true&cachePrepStmts=true";
    }

    /** Get the SQL string of a column type. */
    protected String _getTypeString(Column column)
    {
        String retval = null;

        switch(column.getType())
        {
            case Boolean:
                retval = "tinyint(1)";
                break;
            case Binary:
                retval = "binary";
                break;
           case Blob:
                retval = "longblob";
                break;
            case TextBlob:
                retval = "longtext";
                break;
            case Integer:
                retval = "int";
                break;
            case Timestamp:
                retval = "timestamp";
                break;
            case VarBinary:
                retval = "varbinary";
                break;
            case Varchar:
                retval = "varchar";
                break;
        }

        if(retval != null && column.isAutoIncrement())
        {
            retval += " auto_increment";
        }

        return retval;
    }

    /** Returns true if database supports auto-generated keys in its prepared
     *  statements.
     */
    protected boolean _hasGeneratedKeys()
    {
        return true;
    }
    
    ///////////////////////////////////////////////////////////////////
    // private methods

    /** Create the database catalog if it does not exist. */
    private void _createCatalog(String dbName) throws SQLException
    {
        boolean found = false;
        DatabaseMetaData metadata = _connection.getMetaData();
        ResultSet results = null;
        
        try
        {
            results = metadata.getCatalogs();
            // see if database exists
            while(results.next())
            {
                if(dbName.equals(results.getString("TABLE_CAT")))
                {
                    found = true;
                    break;
                }
            }
        }
        finally
        {
            if(results != null)
            {
                results.close();
            }
        }

        // create if not found
        if(!found)
        {
            Statement statement = null;
            try
            {
                statement = _connection.createStatement();
                statement.execute("CREATE DATABASE " + dbName); 
            }
            finally
            {
                if(statement != null)
                {
                    statement.close();
                }
            }
        }

        _connection.setCatalog(dbName); 
    }
    
    /** A regex pattern to find the database name in a mysql jdbc url. */
    private static final Pattern _databaseNamePattern = Pattern.compile("/(\\w+)\\?");

}