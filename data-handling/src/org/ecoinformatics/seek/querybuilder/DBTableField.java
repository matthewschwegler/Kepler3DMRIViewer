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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Vector;

import org.kepler.objectmanager.data.db.DSTableFieldDef;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.DSTableKeyIFace;

/**
 * A field in the table frame. This is an item that can be dragged and dropped
 */
public class DBTableField implements Transferable, DBDisplayItemIFace {
	protected DSTableFieldIFace mTableField = null;
	protected DBTableFrame mTableFrame = null;
	protected Rectangle mRect = new Rectangle(-1, -1, -1, -1);
	protected boolean mIsLinked = false;
	protected boolean mIsDragOver = false;
	protected boolean mIsDisplayed = false;
	protected Dimension mPreferredDim = null;
	protected Vector mMissingValueCode = new Vector();

	protected DataFlavor flavors[] = new DataFlavor[1];

	/**
	 * Default Constructor
	 */
	public DBTableField() {
	}

	/**
	 * Constructor
	 * 
	 * @param aField
	 *            the database field
	 * @param aOwner
	 *            the table owning the field
	 */
	public DBTableField(DSTableFieldIFace aField, DBTableFrame aOwner) {
		super();
		mTableField = aField;
		mTableFrame = aOwner;
		if (aField != null)
			flavors[0] = new DataFlavor(DBTableField.class, "DBTableField");
		else
			flavors[0] = new DataFlavor(DSTableFieldDef.class,
					"DSTableFieldDef");
	}

	/**
	 * Get the iface obj representing the key
	 * 
	 * @return return obj if it is a key, otherwise false
	 */
	public DSTableKeyIFace getTableKeyIFace() {
		return mTableField instanceof DSTableKeyIFace ? (DSTableKeyIFace) mTableField
				: null;
	}

	/**
	 * Returns the owner object
	 * 
	 * @return the owner object
	 */
	public DBTableFrame getTable() {
		return mTableFrame;
	}

	/**
	 * Set whether it is a link or not
	 * 
	 * @param aVal
	 *            whether is it a link
	 */
	public void setLinked(boolean aVal) {
		mIsLinked = aVal;
	}

	/**
	 * Return whether it is a link
	 * 
	 * @return true if linked
	 */
	public boolean getLinked() {
		return mIsLinked;
	}

	/**
	 * Set whether it is displayed or not
	 * 
	 * @param aVal
	 *            true if the display attr is to be set
	 */
	public void setDisplayed(boolean aVal) {
		mIsDisplayed = aVal;
	}

	/**
	 * Return whether it is being displayed
	 * 
	 * @return true if the display attr is set
	 */
	public boolean isDisplayed() {
		return mIsDisplayed;
	}

	/**
	 * Return the name
	 * 
	 * @return name as a string
	 */
	public String getName() {
		return mTableField == null ? DBUIUtils.ALL_FIELDS : mTableField
				.getName();
	}

	/**
	 * Return the table name (returns the owner's name)
	 * 
	 * @return name as a string
	 */
	public String getTableName() {
		return mTableFrame != null ? mTableFrame.getName() : "";
	}

	/**
	 * Return the datatype
	 * 
	 * @return data type per DataDescription class
	 */
	public String getDataType() {
		return mTableField == null ? "" : mTableField.getDataType();
	}

	/**
	 * Sets whether it is currently being dragged over
	 * 
	 * @param aVal
	 *            true if dragged over otherwise false
	 */
	public void setDragOver(boolean aVal) {
		mIsDragOver = aVal;
	}

	/**
	 * Returns if it is currently be dragged over
	 * 
	 * @return true if it is currently be dragged over
	 */
	public boolean isDragOver() {
		return mIsDragOver;
	}

	/**
	 * Returns the actual name or DBUIUtils.ALL_FIELDS if no name
	 */
	public String toString() {
		return mTableField == null ? DBUIUtils.ALL_FIELDS : mTableField
				.getName();// + (mIsDisplayed ? DBUIUtils.ALL_FIELDS : "");
	}

	/**
	 * Returns the bounds of the item
	 * 
	 * @return the bounds
	 */
	public Rectangle getBounds() {
		return new Rectangle(mRect);
	}

	/**
	 * Sets the rectangle if the item
	 * 
	 * @param aRect
	 */
	public void setRect(Rectangle aRect) {
		mRect.setBounds(aRect);
	}

	/**
	 * Sets the rectangle
	 * 
	 * @param x
	 *            X Do the same as dragOver
	 * @param y
	 *            Y coord
	 * @param w
	 *            Width
	 * @param h
	 *            Height
	 */
	public void setRect(int x, int y, int w, int h) {
		mRect.setBounds(x, y, w, h);
	}

	/**
	 * Translates its position via the point
	 * 
	 * @param aPnt
	 *            the delta point
	 */
	public void add(Point aPnt) {
		mRect.translate(aPnt.x, aPnt.y);
	}

	/**
	 * Sets the preferred dimension
	 * 
	 * @param aDim
	 */
	public void setPreferredDim(Dimension aDim) {
		mPreferredDim = aDim;
	}

	/**
	 * Gets the preferred dimension
	 * 
	 * @return the dimension
	 */
	public Dimension getPreferredDim() {
		return mPreferredDim;
	}

	// ---------------------------------------------------------------
	// -- Transferable Interface
	// ---------------------------------------------------------------

	/**
	 * Returns the array of flavors in which it can provide the data.
	 */
	public synchronized DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	/**
	 * Returns whether the requested flavor is supported by this object.
	 */
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(flavors[0]);
	}

	/**
	 * If the data was requested in the "java.lang.String" flavor, return the
	 * String representing the selection.
	 */
	public synchronized Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (flavor.equals(flavors[0])) {
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