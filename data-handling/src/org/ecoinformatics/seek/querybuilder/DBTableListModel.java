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

import java.util.Vector;

import javax.swing.AbstractListModel;

import org.kepler.objectmanager.data.db.DSTableFieldIFace;

/**
 * THis class is the model for each Table Frame List
 */
public class DBTableListModel extends AbstractListModel {
	protected DBTableFrame mTableFrame = null;
	protected Vector mItems = new Vector();

	/**
	 * Constructor - KNows who the table frame is
	 * 
	 * @param aTableFrame
	 */
	public DBTableListModel(DBTableFrame aTableFrame) {
		mTableFrame = aTableFrame;
	}

	/**
	 * Add a field to the list
	 * 
	 * @param aField
	 *            the field
	 */
	public void add(DSTableFieldIFace aField) {
		mItems.add(new DBTableField(aField, mTableFrame));
	}

	/**
	 * Add a field to the list
	 * 
	 * @param aField
	 *            the field
	 */
	public void add(DBTableField aField) {
		mItems.add(aField);
	}

	/**
	 * Get the number of fields
	 * 
	 * @return the number of fields
	 */
	public int getSize() {
		return mItems.size();
	}

	/**
	 * Get a single field (data item)
	 * 
	 * @return the object at the index (no checks made on the index)
	 */
	public Object getElementAt(int index) {
		return mItems.get(index);
	}

	/**
	 * Returns the Vector of field object (data)
	 * 
	 * @return vector the items
	 */
	public Vector getFields() {
		return mItems;
	}
}