/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2010-10-13 12:22:12 -0700 (Wed, 13 Oct 2010) $' 
 * '$Revision: 26060 $'
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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * A generic representation of an SQL schema.
 *
 * @author Daniel Crawl
 * @version $Id: Schema.java 26060 2010-10-13 19:22:12Z barseghian $
 *
 */
    
public class Schema
{
    /** Construct a new schema.
     *  @param version the version of the schema 
     */
    public Schema(int version)
    {
        _tableMap = new LinkedHashMap<String,Table>(); 
        _majorVersion = version;
    }

    /** Returns true if schema contains a specific table. */
    public boolean containsTable(String name)
    {
        return _tableMap.containsKey(name);
    }

    /** Create a new table. If a table already exists with the same
     *  name, it is overwritten.
     */
    public Table createTable(String name)
    {
        Table retval = new Table(name);
        putTable(name, retval);
        return retval;
    }

    /** Get an existing table. If table does not exist, returns null. */
    public Table getTable(String name)
    {
        return _tableMap.get(name);
    }

    /** Get the major version. */
    public int getMajorVersion()
    {
        return _majorVersion;
    }
    
    /** Get the minor version. */
    public int getMinorVersion()
    {
        return _minorVersion;
    }
    
    /** Get the version string that includes the major and minor versions. */
    public String getVersionString()
    {
        return _majorVersion + "." + _minorVersion;
    }
    
    /** Add a new or change an existing table. */
    public void putTable(String name, Table table)
    {
        _tableMap.put(name, table);
    }

    /** Remove a table. */
    public void removeTable(String name)
    {
        _tableMap.remove(name);
    }

    /** Set the major version. */
    public void setMajorVersion(int version)
    {
        _majorVersion = version;
    }

    /** Set the minor version. */
    public void setMinorVersion(int version)
    {
        _minorVersion = version;
    }

    /** Get a list of tables. */
    public Set<Table> tables()
    {
        return new HashSet<Table>(_tableMap.values());
    }    

    /** Get the names of all tables. */
    public Set<String> tableNames()
    {
        return new HashSet<String>(_tableMap.keySet());
    }

    ////////////////////////////////////////////////////////////////////////
    //// private variables

    /** A mapping of table name to table object. */
    private Map<String,Table> _tableMap;
    
    /** The major version of the schema. */
    private int _majorVersion = -1;
    
    /** The minor version of the schema. */
    private int _minorVersion = 0;

}