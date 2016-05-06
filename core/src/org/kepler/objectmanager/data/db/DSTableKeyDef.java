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
 * Simple Implementation of the DSTableKeyIFace<br>
 * A key can be made up of more than one field
 */
public class DSTableKeyDef implements DSTableKeyIFace {
	protected Vector mFields = new Vector();
	protected DSTableDef mTable = null;
	protected Vector mMissingValueCode = new Vector();

	/**
	 * Default Constructor
	 * 
	 */
	public DSTableKeyDef() {
	}

	/**
	 * Adds a DSTableFieldDef object to the Key
	 * 
	 * @param aFieldDef
	 *            Object to be added
	 */
	public void add(DSTableFieldDef aFieldDef) {
		// aFieldDef.setIsKey(true);
		mFields.add(aFieldDef);
	}

	/**
	 * Get Vector of DSTableFieldDef objects
	 * 
	 * @return vector
	 */
	public Vector getFields() {
		return mFields;
	}

	/**
	 * Returns one or more names. A single name for a single field or key.
	 * Multiple names for a key with multiple fields
	 * 
	 * @return String of type in format "XXXX, XXXX, XXXX"
	 */
	public String getName() {
		StringBuffer name = new StringBuffer();
		for (int i = 0; i < mFields.size(); i++) {
			if (i > 0)
				name.append(", ");
			name.append(((DSTableFieldDef) mFields.elementAt(i)).getName());
		}
		return name.toString();
	}

	/**
	 * Passes back one or more types in human readable string may want to change
	 * this to an array of type or a vector
	 * 
	 * @return String of type in format "XXXX, XXXX, XXXX"
	 */
	public String getDataType() {
		StringBuffer name = new StringBuffer();
		for (int i = 0; i < mFields.size(); i++) {
			if (i > 0)
				name.append(", ");
			name.append(((DSTableFieldDef) mFields.elementAt(i)).getDataType());
		}
		return name.toString();
	}

	/**
	 * @return Returns the mKeyType.
	 */
	public int getKeyType() {
		return DSTableKeyIFace.PRIMARYKEY;
	}

	/**
	 * Sets the Parent DSTableDef object
	 * 
	 * @param aTable
	 *            DSTableDef object
	 */
	public void setTable(DSTableDef aTable) {
		mTable = aTable;
	}

	/**
	 * Returns the table for the field
	 * 
	 * @return DSTableDef Object
	 */
	public DSTableIFace getTable() {
		return mTable;
	}

	/**
	 * Method to return the vector which store the missing value code. If this
	 * attribute doesn't has the missing value code, empty vector will be
	 * returned.
	 * 
	 * 	 */
	public Vector getMissingValueCode() {
		return mMissingValueCode;
	}

	/**
	 * Method to add missing value code into a vector. This method will be used
	 * to store the missing value code in metadata
	 * 
	 * @param code
	 */
	public void addMissingValueCode(String code) {
		if (code != null) {
			mMissingValueCode.add(code);
		}
	}

	/**
	 * Method to return the vector which store the missing value code. If this
	 * attribute doesn't has the missing value code, empty vector will be
	 * returned.
	 * 
	 * 	 */
	public void setMissingValueCode(Vector missingValueVector) {
		mMissingValueCode = missingValueVector;
	}
}