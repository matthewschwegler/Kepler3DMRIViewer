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

import java.awt.Rectangle;

/**
 * This class is derived from DBSelectTableModelItem so we can pick up some
 * basic funcationality for getting and setting of the name and criteria etc.
 */
public class DBWhereListCellBase extends DBSelectTableModelItem implements
		DBWhereIFace {
	protected DBWhereOperator mParent = null;
	protected Rectangle mRect = new Rectangle();
	protected int mDepth = 1;
	protected boolean mIsDragOver = false;

	/**
	 * Constructor
	 * 
	 * @param aParent
	 *            the parent object
	 */
	public DBWhereListCellBase(DBWhereOperator aParent) {
		super();
		mParent = aParent;
	}

	/**
	 * Returns the parent
	 * 
	 * @return parent object
	 */
	public DBWhereOperator getParent() {
		return mParent;
	}

	/**
	 * Sets the depth
	 * 
	 * @param aDepth
	 *            the new depth
	 */
	public void setDepth(int aDepth) {
		mDepth = aDepth;
	}

	/**
	 * Returns the depth
	 * 
	 * @return depth
	 */
	public int getDepth() {
		return mDepth;
	}

	/**
	 * Sets the bounds of the item
	 */
	public void setBounds(Rectangle aRect) {
		mRect.setBounds(aRect);
	}

	/**
	 * Returns the bounds
	 * 
	 * @return rectange of the bounds
	 */
	public Rectangle getBounds() {
		return mRect;
	}

	/**
	 * Return if it is an operator
	 * 
	 * @return boolean indicating if it is an operator
	 */
	public boolean isOperator() {
		return false;
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

	/*
	 * Returns the name
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return mName;
	}

	/*
	 * Returns the name
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString(boolean aUseSymbols) {
		return toString();
	}

}