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

import java.awt.Point;

/**
 * Represents a table and it's X,Y position on the "drawing canvas"<br>
 * NOTE: the x,y attributes are "hints" for UI layout, but are not required.
 */
public class DBQueryDefTable {
	protected Point mPnt = new Point(-1, -1);
	protected int mId;
	protected String mName;

	/**
	 * Constructor
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
	public DBQueryDefTable(int aId, String aName, int aX, int aY) {
		mId = aId;
		mName = aName;
		mPnt.x = aX;
		mPnt.y = aY;
	}

	/**
	 * Returns the Id
	 * 
	 * @return Returns the mId.
	 */
	public int getId() {
		return mId;
	}

	/**
	 * Sets the Id
	 * 
	 * @param aId
	 *            The mId to set.
	 */
	public void setId(int aId) {
		mId = aId;
	}

	/**
	 * @return Returns the mName.
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Returns the name
	 * 
	 * @param aName
	 *            The mName to set.
	 */
	public void setName(String aName) {
		mName = aName;
	}

	/**
	 * Returns the location as a point
	 * 
	 * @return Returns the mPnt (the reference)
	 */
	public Point getPnt() {
		return mPnt;
	}

	/**
	 * Sets the location Point
	 * 
	 * @param aPnt
	 *            The mPnt to set. (sets the contents not the pointer)
	 */
	public void setPnt(Point aPnt) {
		mPnt.setLocation(aPnt);
	}
}