/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.ecogrid;

/**
 * This interface represents a selected object
 * 
 * @author Jing Tao
 */
public interface SelectableObjectInterface {
	public static final boolean DEFAULTSELECTIONSTATUS = true;
	public static final boolean ENABLE = true;
	public static final boolean DISABLE = false;

	/**
	 * Method to get selectable object label(for example name)
	 * 
	 * @return String
	 */
	public String getSelectableObjectLabel();

	/**
	 * Method to get selected status
	 * 
	 * @return boolean
	 */
	public boolean getIsSelected();

	/**
	 * Method to set selected status
	 * 
	 * @param isSelected
	 *            boolean
	 */
	public void setIsSelected(boolean isSelected);

	/**
	 * Method to get icon enable or not
	 * 
	 * @return boolean
	 */
	public boolean getEnabled();

	/**
	 * Method to set icon enable or not
	 * 
	 * @param isEnable
	 *            boolean
	 */
	public void setEnabled(boolean isEnable);

}// SelectedObjectInterface