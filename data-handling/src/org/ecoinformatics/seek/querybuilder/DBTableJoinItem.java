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

import java.awt.Polygon;

/**
 * @author Rod Spears
 * 
 *         This class represents a single relationship between two fields in
 *         different tables
 */
public class DBTableJoinItem {
	protected Polygon mPolygons[] = new Polygon[3];
	protected boolean mIsChanged = true;

	protected boolean mIsSelected = false;

	protected DBTableField mItemLeft;
	protected DBTableField mItemRight;

	/**
	 * Constructor with DBTableFields
	 * 
	 * @param aItemLeft
	 * @param aItemRight
	 */
	public DBTableJoinItem(DBTableField aItemLeft, DBTableField aItemRight) {
		for (int i = 0; i < mPolygons.length; i++)
			mPolygons[i] = new Polygon();

		mItemLeft = aItemLeft;
		mItemRight = aItemRight;
	}

	/**
	 * Return Left side item of Join
	 * 
	 * @return the object on the left side of the join
	 */
	public DBTableField getItemLeft() {
		return mItemLeft;
	}

	/**
	 * Return Right side item of Join
	 * 
	 * @return the object on the left side of the join
	 */
	public DBTableField getItemRight() {
		return mItemRight;
	}

	/**
	 * Returns the array of X coord locations
	 * 
	 * @return array
	 */
	public Polygon[] getPolygons() {
		return mPolygons;
	}

	/**
	 * 
	 * @param aX1
	 * @param aY1
	 * @param aX2
	 * @param aY2
	 */
	private void adjustPolygon(Polygon aPoly, int aX1, int aY1, int aX2, int aY2) {
		boolean firstLeft = aX1 < aX2;
		boolean firstHigher = aY1 < aY2;
		boolean xEqual = aX1 == aX2;
		boolean yEqual = aY1 == aY2;

		aPoly.reset();

		if (aX1 == aX2) {
			aPoly.addPoint(aX1 - 2, aY1);
			aPoly.addPoint(aX1 + 2, aY1);
			aPoly.addPoint(aX1 + 2, aY2);
			aPoly.addPoint(aX1 - 2, aY2);
			aPoly.addPoint(aX1 - 2, aY1);

		} else if (aY1 == aY2) {
			aPoly.addPoint(aX1, aY1 - 2);
			aPoly.addPoint(aX2, aY1 - 2);
			aPoly.addPoint(aX2, aY1 + 2);
			aPoly.addPoint(aX1, aY1 + 2);
			aPoly.addPoint(aX1, aY1 - 2);

		} else {
			aPoly.addPoint(aX1, aY1 - 3);
			aPoly.addPoint(aX2, aY2 - 3);
			aPoly.addPoint(aX2, aY2 + 3);
			aPoly.addPoint(aX1, aY1 + 3);
			aPoly.addPoint(aX1, aY1 - 3);
			/*
			 * if (aX1 < aY1) { if (aY1 < aY2) { aPoly.addPoint(aX1, aY1-2);
			 * aPoly.addPoint(aX2, aY2-2); aPoly.addPoint(aX2, aY2+2);
			 * aPoly.addPoint(aX1, aY1+2); aPoly.addPoint(aX1, aY1-2); } else {
			 * aPoly.addPoint(aX1, aY1-2); aPoly.addPoint(aX2, aY2-2);
			 * aPoly.addPoint(aX2, aY2+2); aPoly.addPoint(aX1, aY1+2);
			 * aPoly.addPoint(aX1, aY1-2); } } else { if (aY1 < aY2) {
			 * 
			 * } else { aPoly.addPoint(aX1, aY1-2); aPoly.addPoint(aX2, aY2-2);
			 * aPoly.addPoint(aX2, aY2+2); aPoly.addPoint(aX1, aY1+2);
			 * aPoly.addPoint(aX1, aY1-2);
			 * 
			 * } }
			 */
		}
	}

	/**
	 * Change contents of one of the polygons
	 * 
	 * @param aInx
	 *            the index of the polygon
	 * @param aX1
	 *            X point
	 * @param aY1
	 *            Y point
	 * @param aX2
	 *            X point
	 * @param aY2
	 *            Y point
	 */
	public void changePolygon(int aInx, int aX1, int aY1, int aX2, int aY2) {
		adjustPolygon(mPolygons[aInx], aX1, aY1, aX2, aY2);
		mIsChanged = true;
	}

	/**
	 * Sets that the polygons have changed
	 * 
	 * @param aIsChanged
	 */
	public void setChanged(boolean aIsChanged) {
		mIsChanged = aIsChanged;
	}

	/**
	 * @return Returns the mIsSelected.
	 */
	public boolean isSelected() {
		return mIsSelected;
	}

	/**
	 * @param isSelected
	 *            The mIsSelected to set.
	 */
	public void setIsSelected(boolean isSelected) {
		mIsSelected = isSelected;
	}
}