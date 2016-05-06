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
import java.util.Vector;

import javax.swing.AbstractListModel;

/**
 * This class represents the model for the JList class. The complexity here is
 * that the model want to view everything as a a vector, but the items are
 * actually an object tree. So the options where to:<br>
 * 1) Use a vecotr as the model and add the complexity to the add and remove
 * methods (this is what was done) 2) Use the "real" data model and then when an
 * item was asked for by index we traverse the tree to figure out the index each
 * time.<br>
 * <br>
 * Assuming painting occurs more often than model updates I went with approach
 * number one
 */
public class DBWhereModel extends AbstractListModel {
	protected Vector mItems = new Vector();
	protected DBWhereIFace mRootObj = null;

	/**
	 * Default Constructor
	 */
	public DBWhereModel() {
	}

	/**
	 * Initialize it by clearing everything and building it from the root where
	 * object
	 * 
	 * @param aWhereObj
	 */
	public void initialize(DBWhereIFace aWhereObj) {
		mItems.clear();
		mRootObj = aWhereObj;
		buildFromWhereObj(mRootObj);
		fireContentsChanged();
	}

	/**
	 * Build it from the where object
	 * 
	 * @param aWhereObj
	 *            the new object
	 */
	protected void buildFromWhereObj(DBWhereIFace aWhereObj) {
		if (aWhereObj != null) {
			mItems.add(aWhereObj);
			if (aWhereObj.isOperator() && aWhereObj instanceof DBWhereOperator) {
				DBWhereOperator whereObj = (DBWhereOperator) aWhereObj;
				for (Enumeration et = whereObj.getEnumeration(); et
						.hasMoreElements();) {
					buildFromWhereObj((DBWhereIFace) et.nextElement());
				}
				mItems.add(whereObj.getClosure());
			}
		}
	}

	/**
	 * Returns the size of the model
	 */
	public int getSize() {
		return mItems.size();
	}

	/**
	 * Gets an element in the model at a specified index
	 */
	public Object getElementAt(int index) {
		return mItems.elementAt(index);
	}

	/**
	 * Returns the current insertion index depending on the current drag over
	 * item
	 * 
	 * @return the index
	 */
	protected int getInsertionIndexFromDrag() {
		int inx = 0;

		for (Enumeration e = mItems.elements(); e.hasMoreElements();) {
			DBWhereIFace item = (DBWhereIFace) e.nextElement();
			if (item.isDragOver()) {
				return inx + 1;
			}
			inx++;
		}
		return -1;
	}

	/**
	 * Adds an item to the model, the parent attr MUST be set before adding item
	 * 
	 * @param aItem
	 *            item to be added, it finds the insert point via the lasted
	 *            dragged object if that can't be found then it adds it as the
	 *            last child of the parent.
	 * @return the index it was added at
	 */
	public int add(DBWhereIFace aItem) {
		int inx;
		if (mItems.size() == 0) {
			inx = 0;
		} else {
			// check to make the parent is set
			DBWhereOperator parent = aItem.getParent();
			if (parent == null)
				return -1;

			inx = getInsertionIndexFromDrag();
			if (inx == -1) {
				inx = mItems.indexOf(parent.getClosure());
				if (inx == -1)
					return -1;
				inx--; // back up one slot from the closure
			}
		}
		add(aItem, inx);
		return inx;
	}

	/**
	 * Adds an item to the model at a specified index, the parent attr MUST be
	 * set before adding item
	 * 
	 * @param aItem
	 *            item to be added, it finds the insert point via the lasted
	 *            dragged object if that can't be found then it adds it as the
	 *            last child of the parent.
	 * @param aInx
	 *            index as to where it will be added
	 */
	public void add(DBWhereIFace aItem, int aInx) {
		mItems.add(aInx, aItem);
		if (aItem instanceof DBWhereOperator) {
			mItems.add(aInx + 1, ((DBWhereOperator) aItem).getClosure());
		}
		if (mRootObj == null && aItem.getParent() == null) {
			mRootObj = aItem;
		}
		fireContentsChanged(this, 0, mItems.size() - 1);
	}

	/**
	 * Remove item from list, if operator, remove its closure and all the
	 * children too
	 * 
	 * @param aObj
	 *            item to be removed
	 */
	public void remove(DBWhereIFace aObj) {
		int startInx = mItems.indexOf(aObj);
		if (aObj instanceof DBWhereOperator) {
			DBWhereIFace closureItem = ((DBWhereOperator) aObj).getClosure();
			DBWhereIFace delItem = null;
			while (delItem != closureItem) {
				delItem = (DBWhereIFace) mItems.elementAt(startInx);
				mItems.remove(delItem);
			}
		} else {
			mItems.remove(aObj);
		}

		if (mItems.size() == 0) {
			mRootObj = null;
		}

		fireContentsChanged();
	}

	/**
	 * Expose the ability to send the UI a notification the model has been
	 * updated
	 * 
	 */
	public void fireContentsChanged() {
		fireContentsChanged(this, 0, mItems.size() - 1);
	}

	/**
	 * Fills QueryDef from Model
	 */
	public void fillQueryDef(DBQueryDef aQueryDef) {
		DBWhereIFace whereObj = null;
		if (mRootObj != null) {
			if (mRootObj instanceof DBWhereOperator) {
				if (((DBWhereOperator) mRootObj).getNumChildern() > 0) {
					whereObj = mRootObj;
				}
			} else {
				whereObj = mRootObj;
			}
		}
		aQueryDef.setWhere(whereObj);
	}

}