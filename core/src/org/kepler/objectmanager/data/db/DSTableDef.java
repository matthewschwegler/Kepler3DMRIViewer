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
 * Simple Implementation of the DSTableIFace<br>
 * A Table schema has a list of field schemas. It also has a notion of a primary
 * and secondary key. When the keys are added they are also added as fields,
 * meaning if the user calls getPrimaryKey, the object that is returned will
 * also be in the Vector of fields.
 */
public class DSTableDef implements DSTableIFace {
	protected String mName = "";
	protected Vector mFields = new Vector();

	protected DSTableKeyDef mPrimaryKey = new DSTableKeyDef();
	protected Vector mSecondaryKeys = new Vector();

	/**
	 * Construct a DSTableDef
	 * 
	 * @param name
	 *            name of the table
	 */
	public DSTableDef(String aName) {
		mName = aName;
		mPrimaryKey.setTable(this);
	}

	/**
	 * Returns a Vector of all the fields in a table
	 * 
	 * @return Returns the mFields.
	 */
	public Vector getFields() {
		return mFields;
	}

	/**
	 * Returns a DSTableKeyDef Object representing the Primary Key
	 * 
	 * @return Returns a Vector of DSTableFieldDef objects
	 */
	public DSTableKeyIFace getPrimaryKey() {
		return mPrimaryKey;
	}

	/**
	 * Returns a Vector of DSTableFieldDef Objects where each object is a
	 * secondary Key
	 * 
	 * @return Returns a Vector of DSTableFieldDef objects
	 */
	public Vector getSecondaryKeys() {
		return mSecondaryKeys;
	}

	/**
	 * Gets the name of table
	 * 
	 * @return Returns the mName.
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Gets the name of mapped table
	 * 
	 * @return Returns the mName.
	 */
	public String getMappedName() {
		return mName;
	}

	/**
	 * Sets the nameof the table
	 * 
	 * @param aName
	 *            The name to set.
	 */
	public void setName(String aName) {
		mName = aName;
	}

	/**
	 * Adds a field to the table
	 * 
	 * @param aName
	 *            name of field
	 * @param aDataType
	 *            data type of field as defined by the DataType class
	 * @return returns the new field object
	 */
	public DSTableFieldDef addField(String aName, String aDataType,
			Vector aMissingValue) {
		DSTableFieldDef fieldDef = new DSTableFieldDef(Integer.toString(mFields
				.size()), aName, aDataType, aMissingValue);
		mFields.add(fieldDef);
		fieldDef.setTable(this);
		return fieldDef;
	}

	/**
	 * Adds a Primary Key to the table
	 * 
	 * @param aName
	 *            name of key
	 * @param aDataType
	 *            data type of field as defined by the DataType class
	 * @return the new primary key object (as a field)
	 */
	public DSTableFieldDef addPrimaryKey(String aName, String aDataType,
			Vector aMissingValue) {
		DSTableFieldDef fieldDef = new DSTableFieldDef(Integer.toString(mFields
				.size()), aName, aDataType, DSTableKeyIFace.PRIMARYKEY,
				aMissingValue);
		mPrimaryKey.add(fieldDef);
		mFields.add(fieldDef);
		fieldDef.setTable(this);
		return fieldDef;
	}

	/**
	 * Adds a secondary key to the table
	 * 
	 * @param aName
	 *            name of key
	 * @param aDataType
	 *            data type of field as defined by the DataType class
	 * @return the new secondary key object (as a field)
	 */
	public DSTableFieldDef addSecondaryKey(String aName, String aDataType,
			Vector aMissingValue) {
		DSTableFieldDef fieldDef = new DSTableFieldDef(Integer.toString(mFields
				.size()), aName, aDataType, DSTableKeyIFace.SECONDARYKEY,
				aMissingValue);
		mSecondaryKeys.add(fieldDef);
		mFields.add(fieldDef);
		fieldDef.setTable(this);
		return fieldDef;
	}

	/**
	 * Returns name of table as a string
	 */
	public String toString() {
		return mName;
	}
}