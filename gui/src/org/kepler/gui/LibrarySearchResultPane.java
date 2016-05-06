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

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.vergil.tree.PTree;

/**
 * LibrarySearchResultPane provides an interface that should be used by all
 * classes that can display the Kepler search results panel.
 * 
 *@author Chad Berkley
 *@since February 17, 2005
 */
public abstract class LibrarySearchResultPane extends JPanel {
	/**
	 * the library for the result pane
	 */
	protected PTree library;

	/**
	 * the search results to display in the pane
	 */
	protected LibrarySearchResults results;

	/**
	 * constructor. this initializes the library and results variables as well
	 * as sets the layout for the panel. update is automatically called from
	 * here as well.
	 * 
	 *@param library
	 *            the library to search
	 *@param results
	 *            the results of a search
	 *@exception IllegalActionException
	 *                Description of the Exception
	 *@throws IllegalActionException
	 *             if the search fails
	 */
	public LibrarySearchResultPane(PTree library, LibrarySearchResults results)
			throws IllegalActionException {
		this.library = library;
		this.results = results;
		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(library), BorderLayout.CENTER);
		update(results);
	}

	/**
	 * this method allows the search results to be updated in the panel
	 * 
	 *@param results
	 *            the results to update to
	 *@throws IllegalActionException
	 *             if the update fails
	 */
	public abstract void update(LibrarySearchResults results)
			throws IllegalActionException;

	/**
	 * collapses a given library completely
	 * 
	 *@param library
	 *            the JTree to collapse
	 */
	public static void collapseAll(JTree library) {
		int count = library.getRowCount();
		for (int i = count - 1; i >= 0; i--) {
			// collapse in reverse order or else the outer nodes get collapsed
			// before the inner ones.
			if (!library.isCollapsed(i)) {
				library.collapseRow(i);
			}
		}
	}

	/**
	 * expand a given tree
	 * 
	 *@param library
	 *            the JTree to expand
	 */
	public static void expandAll(JTree library) {
		int rowCount = library.getRowCount();
		boolean doneFlag = false;
		int previousRowCount = 0;

		while (true) {
			for (int i = rowCount - 1; i >= 0; i--) {
				// expand all the currently visible rows
				library.expandRow(i);
			}

			previousRowCount = rowCount;
			rowCount = library.getRowCount();
			// update the new rowcount

			if (previousRowCount == rowCount) {
				// HACK because the tree doesn't expand (and thus returns true
				// on it's
				// isCollapsed() call) on CompositeEntity nodes
				// with no children. this code should just run the following for
				// loop until it makes it to the end, signalling that entire
				// tree
				// is collapsed.
				break;
			}

			for (int i = rowCount - 1; i >= 0; i--) {
				// check to see if all of the visible rows are expanded
				if (library.isCollapsed(i)) {
					break;
				}
			}
		}
	}

	/**
	 * collapses the default library tree completely
	 */
	protected void collapseAll() {
		collapseAll(this.library);
	}

	/**
	 * expand the entire default library tree
	 */
	protected void expandAll() {
		expandAll(this.library);
	}

	/**
	 * create an array of treepaths from the results
	 * 
	 *@return Description of the Return Value
	 */
	protected TreePath[] createTreePathArray() {
		TreePath[] tm = new TreePath[results.size()];
		for (int i = 0; i < results.size(); i++) {
			tm[i] = results.getTreePath(i);
		}
		return tm;
	}
}