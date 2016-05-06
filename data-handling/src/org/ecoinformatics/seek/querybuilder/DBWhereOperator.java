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

/**
 * A boolean operator class that can "contain" one or more conditions
 * (DBWhereCondition). <i>cond</i> AND <i>cond</i> AND <i>cond</i><br>
 * An operator contains a "closure" object that used for the sole purpose of
 * showing where the operator containment ends in the UI.
 */
public class DBWhereOperator extends DBWhereListCellBase {
	public static final String OR_OPER = "OR";
	public static final String AND_OPER = "AND";

	protected Vector mItems = new Vector();
	protected boolean mIsClosure = false;
	protected DBWhereOperator mClosure = null;

	/**
	 * Constructor - Creates a "closure" object, Must be manually add and
	 * removed from parent the closure object must also be manually added and
	 * removed
	 * 
	 * @param aParent
	 *            the parent of the object
	 * @param aIsClosure
	 *            indicates whether it is a closure object
	 */
	public DBWhereOperator(DBWhereOperator aParent, boolean aIsClosure) {
		super(aParent);
		setName(AND_OPER); // default to an AND (for no real reason)

		if (!aIsClosure) {
			mClosure = new DBWhereOperator(aParent, true);
		}
	}

	/**
	 * Copy Constructor - Copy all fields, but creates a new "closure" object,
	 * Must be manually add and removed from parent the closure object must also
	 * be manually added and removed
	 * 
	 * @param aParent
	 *            the parent of the object
	 * @param aObj
	 *            the object to be copied
	 */
	public DBWhereOperator(DBWhereOperator aParent, DBWhereOperator aObj) {
		super(aParent);
		setName(AND_OPER); // default to an AND (for no real reason)
		mClosure = new DBWhereOperator(aParent, true);

		for (Enumeration et = aObj.mItems.elements(); et.hasMoreElements();) {
			Object obj = et.nextElement();
			if (obj instanceof DBWhereOperator) {
				mItems.add(new DBWhereOperator(this, (DBWhereOperator) obj));
			} else {
				mItems.add(new DBWhereCondition(this, (DBWhereCondition) obj));
			}
		}
	}

	/**
   *
   */
	public void finalize() {

	}

	/**
	 * Sets the depth
	 * 
	 * @param aDepth
	 *            the depth in the object tree
	 */
	public void setDepth(int aDepth) {
		super.setDepth(aDepth);
		if (mClosure != null) {
			mClosure.setDepth(aDepth);
		}
	}

	/**
	 * Return the close object
	 * 
	 * @return the closure object
	 */
	public DBWhereOperator getClosure() {
		return mClosure;
	}

	/**
	 * Indicates whether it is a closure object
	 * 
	 * @return true is it is a "closure" item
	 */
	public boolean isClosure() {
		return mIsClosure;
	}

	/**
	 * Sets whether it is a closure object
	 * 
	 * @param aVal
	 *            whether it is a closure item
	 */
	public void SetClosure(boolean aVal) {
		mIsClosure = aVal;
	}

	/**
	 * Add new item "after" the item identified
	 * 
	 * @param aNewObj
	 *            the new item
	 * @param aAfterObject
	 *            the item the new item will be placed after
	 */
	public void addAfter(DBWhereIFace aNewObj, Object aAfterObject) {
		aNewObj.setDepth(getDepth() + 1);
		int newInx = aAfterObject == null ? 0
				: mItems.indexOf(aAfterObject) + 1;
		mItems.add(newInx, aNewObj);
	}

	/**
	 * Adds new item "at the end of the list
	 * 
	 * @param aNewObj
	 *            the new item
	 */
	public void append(DBWhereIFace aNewObj) {
		aNewObj.setDepth(getDepth() + 1);
		mItems.add(aNewObj);
	}

	/**
	 * Removes an item
	 * 
	 * @param aObj
	 */
	public void remove(DBWhereIFace aObj) {
		mItems.remove(aObj);
	}

	/**
	 * Returns an enumeration for the child of the operator
	 * 
	 * @return the enumeration
	 */
	public Enumeration getEnumeration() {
		return mItems.elements();
	}

	/**
	 * Returns the number of children
	 * 
	 * @return number of children
	 */
	public int getNumChildern() {
		return mItems.size();
	}

	/**
	 * Sets the name
	 * 
	 * @param aName
	 *            the name as a string
	 */
	public void setName(String aName) {
		super.setName(aName);
		if (mClosure != null) {
			mClosure.setName(aName);
		}
	}

	/**
	 * Sets the operator name (calls setName)
	 * 
	 * @param operator
	 *            The operator to set.
	 */
	public void setOperator(String operator) {
		setName(operator);
	}

	/**
	 * Indicates whether it is an operator
	 * 
	 * @return Returns true if an operator
	 */
	public boolean isOperator() {
		return true;
	}

	/**
	 * Returns name
	 */
	public String toString() {
		return mIsClosure ? "/" + mName : mName;
	}

}