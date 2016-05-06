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

package org.ecoinformatics.seek.querybuilder;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class represents a single query. It is made up of three different parts:<br>
 * 1) The "select" part that describes all the fields that will be "displayed"
 * by the query<br>
 * 2) The "joins" part that is basically table/field name pairs<br>
 * 3) The "where" part which describes a heirarchical (tree) of operators and
 * conditions <br>
 */
public class DBQueryDef {
	/** The query was built ok with no errors **/
	public static final int BUILD_OK = 0;
	/** A generic error during parsing and building **/
	public static final int BUILD_ERROR = 1;
	/** More than likely the where clause had a mix of ANDs and ORs **/
	public static final int BUILD_TOO_COMPLEX_WHERE = 2;
	/** Was unable to convert a set of complex ORs to a standard format **/
	public static final int BUILD_TOO_COMPLEX_JOINS = 3;

	protected boolean mIsAdv = false;
	protected Vector mSelects = new Vector();
	protected Vector mTables = new Vector();
	protected Vector mJoins = null;
	protected DBWhereIFace mWhere = null;

	/**
	 * Default Constructor
	 * 
	 */
	public DBQueryDef() {
	}

	/**
	 * Returns whether it considers the query to be "advanced"<br>
	 * An advanced query is one that was built with the advanced tab and then
	 * saved out. It is more of a hint to the tool when reading queries back in.
	 * 
	 * @return true if it is advanced, false if standard
	 */
	public boolean isAdv() {
		return mIsAdv;
	}

	/**
	 * Sets the hint as to whether it is an advanced query
	 * 
	 * @param aIsAdv
	 *            The mIsAdv to set.
	 */
	public void setIsAdv(boolean aIsAdv) {
		mIsAdv = aIsAdv;
	}

	/**
	 * Adds an item to the selects vector
	 * 
	 * @param aItem
	 */
	public void addSelectItem(DBSelectTableModelItem aItem) {
		mSelects.add(aItem);
	}

	/**
	 * Sets the where object
	 * 
	 * @param aItem
	 */
	public void setWhere(DBWhereIFace aItem) {
		mWhere = aItem;
	}

	/**
	 * A copy of the where object and all of its children. It traverses and
	 * builds a duplicate tree of the where object
	 * 
	 * @return a copy of the where object and all of its children
	 */
	public DBWhereIFace getWhere() {
		DBWhereIFace whereTree = null;
		if (mWhere != null) {
			if (mWhere instanceof DBWhereOperator) {
				whereTree = new DBWhereOperator(null, (DBWhereOperator) mWhere);
			} else {
				whereTree = new DBWhereCondition(null,
						(DBWhereCondition) mWhere);
			}
		}

		return whereTree;
	}

	/**
	 * The vector of items representing the the items to be disdplayed<br>
	 * The items are object of class DBSelectTableModelItem
	 * 
	 * @return the Vector of selects, (the display elements)
	 */
	public Vector getSelects() {
		return mSelects;
	}

	/**
	 * Returns a vector of pairs of items, meaning this vector MUST have an even
	 * number of items. The objects are of class DBSelectTableModelItem
	 * 
	 * @return the vector of join pairs
	 */
	public Vector getJoins() {
		return mJoins;
	}

	/**
	 * Sets a vector of DBSelectTableModelItem "pairs", assumes that the vector
	 * always holds an even number of elements, one left and right in pairs
	 * 
	 * @param aJoins
	 *            the join vector
	 */
	public void setJoins(Vector aJoins) {
		mJoins = aJoins;
	}

	/**
	 * Loads the hashtable with the table names from the DBSelectTableModelItem
	 * objects in the vector
	 * 
	 * @param aVector
	 *            the vector of DBSelectTableModelItem items
	 * @param aHashtable
	 *            the hashtable
	 */
	private void addTableNamesToHash(Vector aVector, Hashtable aHashtable) {
		if (aVector != null) {
			for (Enumeration e = mSelects.elements(); e.hasMoreElements();) {
				String tblName = ((DBSelectTableModelItem) e.nextElement())
						.getTableName();
				if (tblName.length() > 0) {
					aHashtable.put(tblName, tblName);
				}
			}
		}
	}

	/**
	 * Adds table item to "table" list, it will create a new DBQueryDefTable
	 * object
	 * 
	 * @param aId
	 *            id of table
	 * @param aName
	 *            name of table
	 * @param aX
	 *            x coord
	 * @param aY
	 *            y coord
	 */
	public void addTable(int aId, String aName, int aX, int aY) {
		mTables.add(new DBQueryDefTable(aId, aName, aX, aY));
	}

	/**
	 * Adds table item to "from" list
	 * 
	 * @param aName
	 *            name of table
	 */
	public void addTable(String aName) {
		mTables.add(new DBQueryDefTable(-1, aName, -1, -1));
	}

	/**
	 * Adds table DBQueryDefTable item to "table" list
	 * 
	 * @param aItem
	 *            DBQueryDefTable item
	 */
	public void addTable(DBQueryDefTable aItem) {
		mTables.add(aItem);
	}

	/**
	 * Returns a vector of table names that would represent the "table" portion
	 * 
	 * @return vector
	 */
	public Vector getTables() {
		return mTables;
	}

	/**
	 * Clears all the vectors and the where object
	 * 
	 */
	public void clear() {
		mSelects.clear();
		mTables.clear();
		mJoins.clear();
		mWhere = null;
	}

}