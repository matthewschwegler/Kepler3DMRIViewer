/* Implementation of DatabaseType for PostgreSQL.

Copyright (c) 2012 The Regents of the University of California.
All rights reserved.
Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the above
copyright notice and the following two paragraphs appear in all copies
of this software.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
ENHANCEMENTS, OR MODIFICATIONS.

*/

package org.kepler.util.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * An implementation of DatabaseType for PostgreSQL.
 *
 * @author Daniel Crawl
 * @version $Id: PostgreSQL.java 30214 2012-07-18 00:59:57Z crawl $
 *
 */
    
public class PostgreSQL extends DatabaseType
{
    /** Only this package (DatabaseFactory) can instantiate. */
    protected PostgreSQL()
    {
        super();
    }
    
    /** Connect to a database. */
    @Override
    public void connect(Map<String,String> parameters) throws SQLException
    {
        try
        {
            super.connect(parameters);
        }
        catch(SQLException e)
        {
            // see if the reason we could not connect is that the
            // database does not exist.
            String message = e.getMessage();
            Matcher matcher = _databaseDoesNotExistPattern.matcher(message);
            if(matcher.find())
            {
                // create the database
                String dbName = matcher.group(1);
                Connection connection = null;
                try
                {
                    // see if the full url was specified
                    String jdbcUrl = parameters.get(DatabaseFactory.Parameter.JDBC_URL.getName());
                    if(jdbcUrl != null && !jdbcUrl.isEmpty())
                    {
                        throw new SQLException("JDBC URL specified, but database does not exist. " +
                                " This case is not supported; the database must be created manually.");
                    }
                    else
                    {
                        // connect to the database named "postgres"
                        String url = _getJDBCUrl(
                            parameters.get(DatabaseFactory.Parameter.HOST.getName()),
                            parameters.get(DatabaseFactory.Parameter.PORT.getName()),
                            "postgres");
                        connection = DriverManager.getConnection(url, 
                            parameters.get(DatabaseFactory.Parameter.USER.getName()), 
                            parameters.get(DatabaseFactory.Parameter.PASSWD.getName()));
                    }
                    
                    Statement statement = null;
                    try
                    {
                        statement = connection.createStatement();
                        // postgres database names are case-insensitive, so
                        // put quotes around the name to get upper-case letters.
                        statement.execute("CREATE DATABASE \"" + dbName + "\"");
                    }
                    finally
                    {
                        if(statement != null)
                        {
                            statement.close();
                        }
                    }
                }
                finally
                {
                    if(connection != null)
                    {
                        connection.close();
                    }
                }
                
                // try connecting again
                super.connect(parameters);
            }
            else
            {
                throw e;
            }
        }
    
    }

    /** Adjust the name of a column. */
    public String getColumnName(String columnName)
    {
        // user is reserved
        if(columnName.equals("user"))
        {
            return "\"user\"";
        }
        return columnName;
    }
    
    /** Get a string representing the default time. */
    public String getDefaultTimeStr()
    {
        return "'01-Jan-00 1:0:0 am'";
    }
    
    /** Get a string representation of the false value for boolean column. */
    public String getFalseValue()
    {
        return "'0'";
    }

    /** Get the name of the type. */
    public String getName()
    {
        return "PostgreSQL";
    }

    /** Rename a column. */
    @Override
    public void renameColumn(String oldName, Column newColumn, String tableName)
            throws SQLException {
        
        // XXX this looks the same as oracle.
        String sqlStr = "ALTER TABLE " + getTableName(tableName) + " RENAME COLUMN " +
            getColumnName(oldName) + " TO " + getColumnDefinition(newColumn);
        _executeSQL(sqlStr);
    }

    /** Set not null constraint to a column. */
    @Override
    public void setColumnNotNull(Column column, String tableName)
            throws SQLException {
        // XX this looks the same as hsql.
        String sqlStr = "ALTER TABLE " + getTableName(tableName) + " ALTER COLUMN " +
                getColumnName(column.getName()) + " SET NOT NULL";
        _executeSQL(sqlStr);                
    }

    ///////////////////////////////////////////////////////////////////
    // protected methods

    /** Get the driver class name. */
    protected String _getDriverName()
    {
        return "org.postgresql.Driver";
    }
    
    /** Get a JDBC URL. */
    @Override
    protected String _getJDBCUrl(String hostName, String port,
            String databaseName) throws SQLException {
        String hostAndPort = _combineHostAndPort(hostName, port);
        return "jdbc:postgresql://" + hostAndPort + "/" + databaseName;
    }

    /** Get the SQL string of a column type. */
    protected String _getTypeString(Column column)
    {
        String retval = null;

        // auto-incrementing columns are type serial
        if(column.isAutoIncrement())
        {
            retval = "serial";
        }
        else
        {
            switch(column.getType())
            {
            case Binary:
                retval = "bytea";
                break;
            case Boolean:
                retval = "boolean";
                break;
            case Blob:
                retval = "bytea";
                break;
            case Integer:
                retval = "integer";
                break;
            case TextBlob:
                retval = "text";
                break;
            case Timestamp:
                retval = "timestamp";
                break;
            case Varchar:
                retval = "varchar";
                break;
            case VarBinary:
                retval = "bytea";
                break;
            default:
                break;
            }
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
    
    /** Returns true if an auto-incremented column value
     *  in a ResultSet must be referenced with the name
     *  instead of position.
     */
    protected boolean _isAutoIncResultReferencedByName()
    {
        return true;
    }

    /** Regex to match error when connecting and database not exist. */
    private final static Pattern _databaseDoesNotExistPattern =
            Pattern.compile("FATAL: database \"([^\\s]+)\" does not exist");

}
