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
 * Interface class that enables the manipulation of either operators or
 * conditions
 */
public interface DBWhereIFace {
	/**
	 * The objects name
	 * 
	 * @return the name
	 */
	public String getName();

	/**
	 * Return its parent object
	 * 
	 * @return the parent
	 */
	public DBWhereOperator getParent();

	/**
	 * Return whether it is an operator or not
	 * 
	 * @return true if operator, otherwise false
	 */
	public boolean isOperator();

	// ------- Needed fr UI support -------------
	/**
	 * Return its depth in the tree
	 * 
	 * @return the depth
	 */
	public int getDepth();

	/**
	 * Sets the depth in the tree
	 * 
	 * @param aDepth
	 *            the depth
	 */
	public void setDepth(int aDepth);

	/**
	 * Sets it bounds of where it is being rendered
	 * 
	 * @param aRect
	 *            the bounds
	 */
	public void setBounds(Rectangle aRect);

	/**
	 * Its bounds of where it is being rendered
	 * 
	 * @return the bounds
	 */
	public Rectangle getBounds();

	/**
	 * Sets whether it is being dragged over
	 * 
	 * @param aIsOver
	 *            true if the mouse is over the object
	 */
	public void setDragOver(boolean aIsOver);

	/**
	 * Returns if it is being dragged over.
	 * 
	 * @return true if the mouse is over it.
	 */
	public boolean isDragOver();

	/**
	 * Returns string
	 * 
	 * @param aUseSymbols
	 *            Generate string using symbols instead of text for conditions
	 *            etc.
	 * @return string representation
	 */
	public String toString(boolean aUseSymbols);

}