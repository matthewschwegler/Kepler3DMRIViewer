/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-01-19 14:39:48 -0800 (Thu, 19 Jan 2012) $' 
 * '$Revision: 29258 $'
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

/**
 *
 * A generic representation of an SQL column.
 *
 * @author Daniel Crawl
 * @version $Id: Column.java 29258 2012-01-19 22:39:48Z crawl $
 *
 */
    
public class Column
{
    /** Construct a Column with a type. */
    public Column(Type type)
    {
        _initialize(type, false, false, -1, null, false);
    }

    /** Construct a Column with a type and specify auto-increment. */
    public Column(Type type, boolean autoIncrement)
    {
        _initialize(type, autoIncrement, false, -1, null, false); 
    }

    /** Construct a Column with a type and specify auto-increment and
     *  primary key.
     */
    public Column(Type type, boolean autoIncrement, boolean primary)
    {
        _initialize(type, autoIncrement, primary, -1, null, false);
    }

    /** Construct a Column with a type and specify auto-increment,
     *  primary key, and if null values are allowed.
     */
    public Column(Type type, boolean autoIncrement, boolean primary, 
        boolean nullAllowed)
    {
        _initialize(type, autoIncrement, primary, -1, null, nullAllowed);
    }

    /** Construct a Column with a type and type length. */
    public Column(Type type, int length)
    {
        _initialize(type, false, false, length, null, false); 
    }

    /** Construct a Column with a type and type length, and specifying if
     *  can be null.
     */
    public Column(Type type, int length, boolean nullAllowed)
    {
        _initialize(type, false, false, length, null, nullAllowed); 
    }

    /** Construct a Column with a type and type length, and specifying if
     *  can be null or a primary key.
     */
    public Column(Type type, int length, boolean nullAllowed, boolean primary)
    {
        _initialize(type, false, primary, length, null, nullAllowed); 
    }

    /** Construct a Column with a type and a default value. */
    public Column(Type type, String defaultValue)
    {
        _initialize(type, false, false, -1, defaultValue, false); 
    }
    
    /** Get the default value. Returns null if none exists. */
    public String getDefaultValue()
    {
        return _default;
    }

    /** Get the length of the column's type. Returns -1 if not specified. */
    public int getLength()
    {
        return _length;
    }

    /** Get the name. If the column has not been added to a table,
     *  returns null.
     */
    public String getName()
    {
        return _name;
    }
    
    /** Get the containing table. If the column has not been added to a,
     *  then return null.
     */
    public Table getTable() 
    {
        return _table;
    }

    /** Get the data type. */
    public Type getType()
    {
        return _type;
    }

    /** Returns true if column value is auto-incremented. */
    public boolean isAutoIncrement()
    {
        return _autoIncrement;
    }

    /** Returns true if column can have null values. */
    public boolean isNullAllowed()
    {
        return _nullAllowed;
    }

    /** Returns true if column is primary key. */
    public boolean isPrimaryKey()
    {
        return _primary;
    }
    
    /** Set the name. */
    public void setName(String name)
    {
        _name = name;
    }

    /** Get a string representation of the column. */
    public String toString()
    {
        StringBuilder retval = new StringBuilder("column ");
        retval.append(_name);
        retval.append(" ");
        retval.append("type=");
        retval.append(_type.toString());
        retval.append(" length=");
        retval.append(_length);
        retval.append(" nullable=");
        retval.append(_nullAllowed);
        retval.append(" primary=");
        retval.append(_primary);
        retval.append(" autoinc=");
        retval.append(_autoIncrement);
        retval.append(" default=");
        if(_default != null)
        {
            retval.append(_default);
        }
        else
        {
            retval.append("null");
        }
        retval.append(" table=");
        if(_table != null)
        {
            retval.append(_table.getName());
        }
        else
        {
            retval.append("null");
        }
        return retval.toString();
    }

    ////////////////////////////////////////////////////////////////////////
    //// public variables

    /** Data types. */
    public enum Type
    {
        Boolean,
        Binary,
        Blob,
        TextBlob,
        Integer,
        Timestamp,
        VarBinary,
        Varchar,
    }

    /** An auto-incrementing integer column. */
    public static final Column AUTOINCID = 
        new Column(Column.Type.Integer, true, true);

    /** A boolean column. */
    public static final Column BOOLEAN = new Column(Column.Type.Boolean);
    
    /** A BLOB column. */
    public static final Column BLOB = new Column(Column.Type.Blob);

    /** An integer column. */
    public static final Column INTEGER = new Column(Column.Type.Integer);

    /** An MD5 text column. */
    public static final Column MD5_TEXT = new Column(Column.Type.Varchar, 32);
    
    /** An MD5 binary column. */
    public static final Column MD5_BINARY = new Column(Column.Type.VarBinary, 16, true);

    /** An integer column that can be null. */
    public static final Column NULLABLE_INTEGER = 
        new Column(Column.Type.Integer, false, false, true);

    /** An MD5 text column that can be null. */
    public static final Column NULLABLE_MD5_TEXT =
        new Column(Column.Type.Varchar, 32, true);

    /** A text column that can be null. Maximum length is 255 characters. */
    public static final Column NULLABLE_TEXT = 
        new Column(Column.Type.Varchar, 255, true);

    /** A primary key integer column. */
    public static final Column PK_INTEGER =
        new Column(Column.Type.Integer, false, true);

    /** A primary key MD5 text column. */
    public static final Column PK_MD5_TEXT = new Column(Column.Type.Varchar, 32,
        false, true);

    public static final Column PK_MD5_BINARY = new Column(Column.Type.VarBinary, 16,
            false, true);

    /** A primary key text column. Maximum length is 255 characters. */
    public static final Column PK_TEXT = new Column(Column.Type.Varchar, 255,
        false, true);

    /** A text column. Maximum length is 255 characters. */
    public static final Column TEXT = new Column(Column.Type.Varchar, 255);
    
    /** A text column with no maximum length. */
    public static final Column TEXT_UNLIMITED = new Column(Column.Type.TextBlob);
    
    /** A timestamp column. */
    public static final Column TIMESTAMP = new Column(Column.Type.Timestamp);

    /** A column for text UUIDs that are primary keys. */
    public static final Column PK_UUID_TEXT = new Column(Column.Type.Varchar, 36, false, true);
    
    /** A column for text UUIDs. */
    public static final Column UUID_TEXT = new Column(Column.Type.Varchar, 36);

    ////////////////////////////////////////////////////////////////////////
    //// package methods

    Column(Column column)
    {
        _autoIncrement = column._autoIncrement;
        _default = column._default;
        _length = column._length;
        _name = column._name;
        _nullAllowed = column._nullAllowed;
        _primary = column._primary;
        _table = column._table;
        _type = column._type;
    }
    
    /** Set the default value. */
    void _setDefaultValue(String value)
    {
        _default = value;
    }
        
    /** Set the table. */
    void _setTable(Table table)
    {
        _table = table;
    }
    
    ////////////////////////////////////////////////////////////////////////
    //// private methods

    /** Initialize a column. */
    private void _initialize(Type type, boolean autoIncrement, boolean primary,
        int length, String defaultValue, boolean nullAllowed)
    {
        _type = type;
        _autoIncrement = autoIncrement;
        _primary = primary;
        _length = length;
        _default = defaultValue;
        _nullAllowed = nullAllowed;
    }

    ////////////////////////////////////////////////////////////////////////
    //// private variables

    /** The column data type. */
    private Type _type;

    /** If true, column value is auto-incremented during an insert. */
    private boolean _autoIncrement;

    /** If true, column is a primary key. */
    private boolean _primary;

    /** The length of column type. */
    private int _length;

    /** If true, column value can be null. */
    private boolean _nullAllowed;

    /** The default value. */
    private String _default;
    
    /** The parent table. This is null until column added to a table. */
    private Table _table;
    
    /** The column name. This is null until column added to a table. */
    private String _name;
}