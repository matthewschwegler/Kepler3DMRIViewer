/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2011-06-09 15:45:21 -0700 (Thu, 09 Jun 2011) $' 
 * '$Revision: 27699 $'
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

import java.sql.SQLException;

/**
 *
 * An implementation of DatabaseType for Oracle.
 *
 * @author Daniel Crawl
 * @version $Id: Oracle.java 27699 2011-06-09 22:45:21Z crawl $
 *
 */
    
public class Oracle extends DatabaseType
{
    /** Only this package (DatabaseFactory) can instantiate. */
    protected Oracle()
    {
        super();
    }
    
    /** Adjust the name of a column. */
    public String getColumnName(String columnName)
    {
        // user is reserved
        if(columnName.equals("user"))
        {
            return "\"user\"";
        }
        return super.getColumnName(columnName);
    }
    
    /** Get a string representing the default time. */
    public String getDefaultTimeStr()
    {
        return "'01-Jan-00 1:0:0 am'";
    }

    /** Get the name of the type. */
    public String getName()
    {
        return "Oracle";
    }

    /** Rename a column. */
    public void renameColumn(String oldName, Column newColumn, String tableName) throws SQLException
    {
        String newName = getColumnName(newColumn.getName());
        String sqlStr = "ALTER TABLE " + getTableName(tableName) +
        " RENAME COLUMN " + getColumnName(oldName) + " TO " + newName;
        _executeSQL(sqlStr);
    }

    /** Set not null constraint to a column. */
    public void setColumnNotNull(Column column, String tableName) throws SQLException
    {
        String sqlStr = "ALTER TABLE " + getTableName(tableName) +
        " MODIFY (" + getColumnName(column.getName()) + " NOT NULL)";
        _executeSQL(sqlStr);
    }

    ///////////////////////////////////////////////////////////////////
    // protected methods
    
    /** Get the driver class name. */
    protected String _getDriverName()
    {
        return "oracle.jdbc.driver.OracleDriver";
    }
    
    /** Get a JDBC URL. */
    protected String _getJDBCUrl(String hostName, String port,
            String databaseName) throws SQLException
    {
        String hostAndPort = _combineHostAndPort(hostName, port);
        return "jdbc:oracle:thin:@" + hostAndPort + ":" + databaseName;
    }

    /** Get the SQL string of a column type. */
    protected String _getTypeString(Column column)
    {
        String retval = null;

        switch(column.getType())
        {
            case Boolean:
                retval = "number(1)";
                break;
            case Blob:
                retval = "blob";
                break;
            case TextBlob:
                retval = "clob";
                break;
            case Integer:
                retval = "int";
                break;
            case Timestamp:
                retval = "timestamp";
                break;
            case Varchar:
                retval = "varchar";
                break;
        }

        return retval;
    }

    /** Returns true if database supports auto-generated keys in its prepared
     *  statements.
     */
    protected boolean _hasGeneratedKeys()
    {
        return false;
    }
    
    /** Returns true if identifier is too long. */
    protected boolean _isIdentifierTooLong(String identifier)
    {
        return identifier.length() > 30;
    }

    /** Returns true if column names should be capitalized. */
    protected boolean _needCapitalColumnNames()
    {
        return true;
    }

    /** Returns true if table names should be capitalized. */
    protected boolean _needCapitalTableNames()
    {
        return true;
    }

    /** Returns true if need to use sequences for autoincrement columns. */
    protected boolean _needSequencesForAutoInc()
    {
        return true;
    }
}