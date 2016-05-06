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

import org.kepler.objectmanager.data.DataType;

/**
 * THis class is a model class for a condition of a where clause
 */
public class DBWhereCondition extends DBWhereListCellBase {
	/**
	 * Constructor
	 * 
	 * @param aParent
	 *            parent to be added to
	 * @param aTableName
	 *            name of item
	 * @param aFieldName
	 *            field name of item
	 * @param aDataType
	 *            the datatype
	 */
	public DBWhereCondition(DBWhereOperator aParent, String aTableName,
			String aFieldName, String aDataType) {
		super(aParent);
		mParent = aParent;
		mName = aFieldName;
		mTableName = aTableName;
		mDataType = aDataType;
		mOper = DBSelectTableUIStd.OPERS_TXT[DBSelectTableUIStd.EQUALS_INX];
	}

	/**
	 * Copy Constructor (does not copy parent), sets it to the new parent
	 * 
	 * @param aParent
	 *            parent to be added to
	 * @param aCond
	 *            object to be copied
	 */
	public DBWhereCondition(DBWhereOperator aParent, DBWhereCondition aCond) {
		super(aParent);
		mParent = aParent;
		mName = aCond.mName;
		mTableName = aCond.mTableName;
		mDataType = aCond.mDataType;
		mOper = aCond.mOper;
		mDepth = aCond.mDepth;
		mCriteria = aCond.mCriteria;
		mIsDisplayed = aCond.mIsDisplayed;
		mTableId = aCond.mTableId;
	}

	/**
	 * Indicates whether it is an operator
	 * 
	 * @return whether it is an operator or not
	 */
	public boolean isOperator() {
		return false;
	}

	/**
	 * Converts the entire condition to a string
	 * 
	 * @param aUseSymbols
	 *            indicates whether to return a string as a symbol or as text
	 * @return its representation as a string
	 */
	public String toString(boolean aUseSymbols) {

		StringBuffer strBuf = new StringBuffer(DBUIUtils.getFullFieldName(
				mTableName, mName)
				+ " "
				+ (aUseSymbols ? DBSelectTableUIStd.getBoolOperSymbol(mOper)
						: mOper));
		if (getCriteria().length() > 0) {
			if (mDataType.equals(DataType.STR))
				strBuf.append(" '" + getCriteria() + "'");
			else
				strBuf.append(" " + getCriteria());
		} else {
			strBuf.append("  <no criteria>");
		}
		return strBuf.toString();
	}

	/**
	 * Converts the entire condition to a string
	 * 
	 * @return its representation as a string
	 */
	public String toString() {
		return toString(false);
	}

}