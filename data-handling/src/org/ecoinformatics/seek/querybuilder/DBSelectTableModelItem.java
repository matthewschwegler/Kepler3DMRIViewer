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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Vector;

/**
 * This class represents each row in the JTable that enables the user to select
 * a specific field in table and any additional information that can be changed.
 * The mDisplayItem data member will hold a valid pointer to an object in the
 * schema in the Advanced version, but not in the Standard.
 */
public class DBSelectTableModelItem implements Transferable, DBDisplayItemIFace {
	protected DBDisplayItemIFace mDisplayItem = null;
	protected int mTableId = -1;
	protected String mTableName = "";
	protected String mName = "";
	protected String mDataType = "";
	protected boolean mIsDisplayed = false; // Uses mDisplayItem's display attr
											// first if possible
	protected String mCriteria = "";
	protected String mOper = "";

	protected DataFlavor mFlavors[] = new DataFlavor[1];

	private Vector mMissingValueCode = new Vector();

	/**
	 * Default Constructor
	 */
	public DBSelectTableModelItem() {
		mFlavors[0] = new DataFlavor(DBSelectTableModelItem.class,
				"DBSelectTableModelItem");
	}

	/**
	 * Constructor with args
	 * 
	 * @param aTableName
	 *            table name
	 * @param aFieldName
	 *            field name
	 * @param aDataType
	 *            data type
	 * @param aIsDisplayed
	 *            whether it is displayed
	 * @param aCriteria
	 *            criteria
	 * @param aOperator
	 *            operator
	 */
	public DBSelectTableModelItem(String aTableName, String aFieldName,
			String aDataType, boolean aIsDisplayed, String aCriteria,
			String aOperator, Vector missingValue) {
		mTableName = aTableName;
		mName = aFieldName;
		mDataType = aDataType;
		mIsDisplayed = aIsDisplayed;
		mCriteria = aCriteria;
		mOper = aOperator;
		mMissingValueCode = missingValue;
	}

	/**
	 * Copy Constructor
	 * 
	 * @param aItem
	 *            item to be copied
	 */
	public DBSelectTableModelItem(DBSelectTableModelItem aItem) {
		mTableId = aItem.mTableId;
		mDisplayItem = aItem.mDisplayItem;
		mTableName = aItem.mTableName;
		mName = aItem.mName;
		mDataType = aItem.mDataType;
		mIsDisplayed = aItem.mIsDisplayed;
		mCriteria = aItem.mCriteria;
		mOper = aItem.mOper;
		mMissingValueCode = aItem.getMissingValueCode();
	}

	/**
	 * Constructor from DBTableField
	 */
	public DBSelectTableModelItem(DBDisplayItemIFace aDisplayItem) {
		if (aDisplayItem != null) {
			mDisplayItem = aDisplayItem;
			mTableName = aDisplayItem.getTableName();
			mName = aDisplayItem.getName();
			mDataType = aDisplayItem.getDataType();
			mIsDisplayed = aDisplayItem.isDisplayed();
		}
	}

	/**
	 * Sets the DBDisplayItemIFace object
	 * 
	 * @param aDisplayItem
	 *            object to set
	 */
	public void setDisplayItem(DBDisplayItemIFace aDisplayItem) {
		mDisplayItem = aDisplayItem;
	}

	/**
	 * Gets the DBDisplayItemIFace object
	 * 
	 * @return DisplayItem object
	 */
	public DBDisplayItemIFace getDisplayItem() {
		return mDisplayItem;
	}

	/**
	 * Gets the dataType
	 * 
	 * @return Returns the dataType.
	 */
	public String getDataType() {
		return mDataType;
	}

	/**
	 * Sets the dataType
	 * 
	 * @param aDataType
	 *            The dataType to set.
	 */
	public void setDataType(String aDataType) {
		mDataType = aDataType;
	}

	/**
	 * Returns whether the item is to be "displayed" in the select statment
	 * 
	 * @return Returns the isDisplayed.
	 */
	public boolean isDisplayed() {
		return mDisplayItem != null ? mDisplayItem.isDisplayed() : mIsDisplayed;
	}

	/**
	 * Sets the displayed attr, if it has a mDisplayItem it will use that
	 * instead for caching the value and making sure the UI is properly updated
	 * 
	 * @param isDisplayed
	 *            The isDisplayed to set.
	 */
	public void setDisplayed(boolean isDisplayed) {
		if (mDisplayItem != null) {
			mDisplayItem.setDisplayed(isDisplayed);
		} else {
			mIsDisplayed = isDisplayed;
		}
	}

	/**
	 * Return the name
	 * 
	 * @return Returns the name.
	 */
	public String getName() {
		return mName;
	}

	/**
	 * @param aName
	 *            The name to set.
	 */
	public void setName(String aName) {
		mName = aName;
	}

	/**
	 * Return the table name
	 * 
	 * @return Returns the tableName.
	 */
	public String getTableName() {
		return mTableName;
	}

	/**
	 * Sets the table name
	 * 
	 * @param aTableName
	 *            The tableName to set.
	 */
	public void setTableName(String aTableName) {
		mTableName = aTableName;
	}

	/**
	 * Return the table Id
	 * 
	 * @return Returns the tableId.
	 */
	public int getTableId() {
		return mTableId;
	}

	/**
	 * Sets the table Id
	 * 
	 * @param aTableId
	 *            The table Id to set.
	 */
	public void setTableId(int aTableId) {
		mTableId = aTableId;
	}

	/**
	 * Return the criteria
	 * 
	 * @return Returns the criteria.
	 */
	public String getCriteria() {
		return mCriteria;
	}

	/**
	 * @param aCriteria
	 *            The criteria to set.
	 */
	public void setCriteria(String aCriteria) {
		mCriteria = aCriteria;
	}

	/**
	 * Returns the operator name
	 * 
	 * @return Returns the operator.
	 */
	public String getOperator() {
		return mOper;
	}

	/**
	 * set the operator by name (string)
	 * 
	 * @param aOper
	 *            the operator
	 */
	public void setOperator(String aOper) {
		mOper = aOper;
	}

	/**
	 * Returns the name as the textual rendering (the name)
	 */
	public String toString() {
		return mName;
	}

	// ---------------------------------------------------------------
	// -- Transferable Interface
	// ---------------------------------------------------------------

	/**
	 * Returns the array of mFlavors in which it can provide the data.
	 */
	public synchronized DataFlavor[] getTransferDataFlavors() {
		return mFlavors;
	}

	/**
	 * Returns whether the requested flavor is supported by this object.
	 */
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(mFlavors[0]);
	}

	/**
	 * If the data was requested in the "java.lang.String" flavor, return the
	 * String representing the selection.
	 */
	public synchronized Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (flavor.equals(mFlavors[0])) {
			return this;
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
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