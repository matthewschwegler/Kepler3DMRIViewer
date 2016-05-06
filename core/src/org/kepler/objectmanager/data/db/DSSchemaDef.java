/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.data.db;

import java.util.Vector;

/**
 * Simple Implementation of the DSSchemaIFace interface
 */
public class DSSchemaDef implements DSSchemaIFace {
	String mName = null;
	Vector mTables = new Vector();

	/**
	 * Default Constructor
	 */
	public DSSchemaDef() {
		mName = "";
	}

	/**
	 * Constructor with a Name for the Schema
	 * 
	 * @param aName
	 *            name of schema
	 */
	public DSSchemaDef(String aName) {
		mName = aName;
	}

	/**
	 * Adds a DSTableDef Object to the Schema
	 * 
	 * @param aTable
	 *            object to be added
	 */
	public void addTable(DSTableIFace aTable) {
		mTables.add(aTable);
	}

	// --------------- DSSchmeaIFace INterface -------------

	/**
	 * Returns the vector of tables
	 * 
	 * @return vector
	 */
	public Vector getTables() {
		return mTables;
	}

	/**
	 * Returns the name
	 * 
	 * @return name
	 */
	public String getName() {
		return mName;
	}

}