/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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

package org.kepler.gui;

import java.awt.event.ActionListener;

import javax.swing.JPanel;

/**
 * LibrarySearchPane provides and interface for the search panel in the actor
 * library
 * 
 * @author Chad Berkley
 */
public abstract class LibrarySearchPane extends JPanel {
	ActionListener searchButtonHandler;

	/**
	 * constructor
	 * 
	 * @param searchButtonListener
	 *            a listener to handle the search event
	 */
	public LibrarySearchPane(ActionListener searchButtonHandler) {
		this.searchButtonHandler = searchButtonHandler;
	}

	/**
	 * get the preferred/minimum width of this panel - calculated to allow
	 * enough space for all buttons and spacers etc
	 * 
	 * @return the minimum allowable width of this panel
	 */
	public abstract int getMinimumWidth();

	/**
	 * return the search string
	 */
	public abstract String getSearchTerm();
}