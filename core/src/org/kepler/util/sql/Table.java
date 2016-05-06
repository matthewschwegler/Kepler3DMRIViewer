/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-01-19 14:42:22 -0800 (Thu, 19 Jan 2012) $' 
 * '$Revision: 29261 $'
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 
 *
 * A generic representation of an SQL table. A Table can contain
 * columns, constraints, and foreign keys.
 *
 * @author Daniel Crawl
 * @version $Id: Table.java 29261 2012-01-19 22:42:22Z crawl $
 *
 */
    
public class Table
{
    /** Construct a new Table. */
    Table(String name)
    {
        _tableName = name;

        _columnMap = new LinkedHashMap<String,Column>();
        _constraintMap = new LinkedHashMap<String,ColumnConstraint>();
        _fkMap = new LinkedHashMap<String,ForeignKey>();

        _columnConstraintsMap = new HashMap<String,Set<String>>();
        _columnFKsMap = new HashMap<String,Set<String>>();
    }
    
    /** Get the columns. */
    public Set<Column> columns()
    {
        return new HashSet<Column>(_columnMap.values());
    }

    /** Get the column names. */
    public Set<String> columnNames()
    {
        return new HashSet<String>(_columnMap.keySet()); 
    }

    /** Get the constraint names. */
    public Set<String> constraintNames()
    {
        return _constraintMap.keySet();
    }

    /** Get the foreign key names. */
    public Set<String> foreignKeyNames()
    {
        return _fkMap.keySet();
    }

    /** Get a specific column. */
    public Column getColumn(String name)
    {
        return _columnMap.get(name);
    }

    /** Get a specific constraint. */
    public ColumnConstraint getConstraint(String name)
    {
        return _constraintMap.get(name);
    }

    /** Get a specific foreign key. */
    public ForeignKey getForeignKey(String name)
    {
        return _fkMap.get(name);
    }
    
    /** Get the column(s) for an index. */
    public String getIndexColumns(String indexName)
    {
        return _indexNameColumnsMap.get(indexName);
    }
    
    /** Get the index type. */
    public IndexType getIndexType(String name)
    {
        return _indexNameTypeMap.get(name);
    }

    /** Get the table name. */
    public String getName()
    {
        return _tableName;
    }
    
    /** Returns true if column references another table. */
    public boolean isForeignKey(String columnName)
    {
        return _columnFKsMap.containsKey(columnName);
    }
    
    /** Get the indexes. */
    public Set<String> indexes()
    {
        return new HashSet<String>(_indexNameTypeMap.keySet());
    }

    /** Add a new or replace an existing column with no foreign key or index. */
    public void putColumn(String name, Column column)
    {
        putColumn(name, column, null, null);
    }
    
    /** Add a new or replace an existing column with a default value. */
    public void putColumn(String name, Column column, String defaultValue)
    {
        putColumn(name, column, null, null, defaultValue);
    }
    
    /** Add a new or replace an existing column.
     *  @param name the column name
     *  @param column the column type
     *  @param fkTable the referenced table in foreign key
     *  @param fkColumn the referenced column in foreign key
     */
    public void putColumn(String columnName, Column column, String fkTable, 
        String fkColumn)
    {
        putColumn(columnName, column, fkTable, fkColumn, null);
    }
    
    /** Add a new or replace an existing column.
     *  @param name the column name
     *  @param column the column type
     *  @param fkTable the referenced table in foreign key
     *  @param fkColumn the referenced column in foreign key
     *  @param defaultValue the default value
     */
    public void putColumn(String columnName, Column column, String fkTable, 
        String fkColumn, String defaultValue)
    {
        // NOTE: we make a copy of the column since we set the
        // name and table, and don't want to overwrite these values
        // in one of the static columns.
        Column columnClone = new Column(column);
        _columnMap.put(columnName, columnClone);
        columnClone._setTable(this);
        columnClone.setName(columnName);
        if(defaultValue != null)
        {
            columnClone._setDefaultValue(defaultValue);
        }

        if(fkTable != null && fkColumn != null)
        {
            String fkName = _tableName + "_" + columnName + "_fk";
            putForeignKey(fkName, columnName, fkTable, fkColumn);
        }
    }

    /** Add a new or replace an existing constraint. */
    public void putConstraint(String constraintName, String columnName,
        String constraintValue)
    {
        ColumnConstraint constraint = new ColumnConstraint(columnName, constraintValue);
        _constraintMap.put(constraintName, constraint);
        _addColumnConstraint(columnName, constraintName);
    }
    
    /** Add an index on one or more columns. */
    public void putIndex(String columnName)
    {
    	putIndex(columnName.replaceAll("[,\\s]", "_") + "_idx", columnName);
    }
    
    /** Add a new or replace an existing index. */
    public void putIndex(String indexName, String columnName)
    {
        putIndex(indexName, columnName, IndexType.Default);
    }
    
    /** Add a new or replace an index index specifying the type of index. */
    public void putIndex(String indexName, String columnName, IndexType type)
    {
        _indexNameTypeMap.put(indexName, type);
        _indexNameColumnsMap.put(indexName, columnName);
    }

    /** Add a new or replace an existing foreign key. */
    public void putForeignKey(String fkName, String columnName, String fkTable,
        String fkColumn)
    {
        ForeignKey fk = new ForeignKey(columnName, fkTable, fkColumn);
        _fkMap.put(fkName, fk);

        _addColumnForeignKey(columnName, fkName);
    }

    /** Remove a column. */
    public void removeColumn(String columnName)
    {
        _columnMap.remove(columnName);

        // remove any constraints associated with this column
        Set<String> constraintSet = _columnConstraintsMap.remove(columnName);
        if(constraintSet != null)
        {
            for(String constraintName : constraintSet)
            {
                removeConstraint(constraintName);
            }
        }

        // remove any foriegn keys associated with this column
        Set<String> fkSet = _columnFKsMap.remove(columnName);
        if(fkSet != null)
        {
            for(String fkName : fkSet)
            {
                _fkMap.remove(fkName);
            }
        }
    }

    /** Remove a constraint. */
    public void removeConstraint(String name)
    {
        _constraintMap.remove(name);

        //XXX update columnConstraintsMap?
    }

    /** Remove a foreign key. */
    public void removeForeignKey(String name)
    {
        _fkMap.remove(name);

        //XXX update columnFKsMap?
    }
    
    /** Get a string representation of the table. */
    public String toString()
    {
        StringBuilder retval = new StringBuilder("table ");
        retval.append(_tableName);
        retval.append("\n");
        for(Column column : columns())
        {
            retval.append("{");
            retval.append(column.toString());
            retval.append("}\n");
        }
        return retval.toString();
    }

    ////////////////////////////////////////////////////////////////////////
    //// public fields

    /** Index types. */
    public enum IndexType
    {
        Bitmap,
        Default,
    }

    ////////////////////////////////////////////////////////////////////////
    //// protected classes

    protected static class ColumnConstraint
    {
        private ColumnConstraint(String columnName, String constraintValue)
        {
            _columnName = columnName;
            _value = constraintValue;
        }
        
        public String getColumnName()
        {
            return _columnName;
        }
        
        public String getConstraintValue()
        {
            return _value;
        }
        
        private String _columnName;
        private String _value;
    }
    
    /** A class for foreign keys. */
    protected static class ForeignKey
    {
        /** Construct a new ForeignKey with a column name, and referent
         *  table and column names. This is private since only Table
         *  may create new ForeignKeys.
         */
        private ForeignKey(String columnName, String refTable, String refColumn)
        {
            _column = columnName;
            _refColumn = refColumn;
            _refTable = refTable;
        }

        /** Get the column name. */
        protected String getColumn()
        {
            return _column;
        }
       
        /** Get the referenced column name. */
        protected String getRefColumn()
        {
            return _refColumn;
        }
       
        /** Get the referenced table name. */
        protected String getRefTable()
        {
            return _refTable;
        }

        /** The column name. */
        private String _column;

        /** The referenced column name. */
        private String _refColumn;

        /** The referenced table name. */
        private String _refTable;
    }

    ////////////////////////////////////////////////////////////////////////
    //// private methods

    /** Add a constraint associated with a column. */
    private void _addColumnConstraint(String columnName, String constraintName)
    {
        _updateMap(_columnConstraintsMap, columnName, constraintName);
    }

    /** Add a foreign key associated with a column. */
    private void _addColumnForeignKey(String columnName, String fkName)
    {
        _updateMap(_columnFKsMap, columnName, fkName);
    }

    /** Update a map of string to hash set. */
    private void _updateMap(Map<String,Set<String>> map, String key,
        String value)
    {
        Set<String> valueSet = map.get(key);
        if(valueSet == null)
        {
            valueSet = new HashSet<String>();
            map.put(key, valueSet);
        }
        valueSet.add(value);
    }

    ////////////////////////////////////////////////////////////////////////
    //// private variables

    /** A mapping of column name to a set of its constraints. */
    // XXX this is not used.
    private Map<String,Set<String>> _columnConstraintsMap;

    /** A mapping of columns name to a set of its foreign keys. */
    private Map<String,Set<String>> _columnFKsMap;

    /** A mapping of name to column. */
    private Map<String,Column> _columnMap;
    
    /** A mapping of name to constraint. */
    private Map<String,ColumnConstraint> _constraintMap;

    /** A mapping of name to foreign keys. */
    private Map<String,ForeignKey> _fkMap;

    /** The name of this table. */
    private String _tableName;
    
    /** A mapping of index name to index type. */
    private Map<String,IndexType> _indexNameTypeMap = new HashMap<String,IndexType>();
    
    /** A mapping of index name to column(s). */
    private Map<String,String> _indexNameColumnsMap = new HashMap<String,String>();    
}