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

import javax.swing.tree.TreePath;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.tree.PTree;

/**
 * This class implements a simple result display where the results from the
 * search are simply highlighted in the tree. Any subtree where a result may be
 * located is also expanded.
 * 
 * @author Chad Berkley
 */
public class ResultHighlighter extends LibrarySearchResultPane {
	/**
	 * the constructor passes in the library to highlight the results in and the
	 * results to highlight. if results is null, the tree is built fully
	 * collapsed with no highlights.
	 * 
	 * @param library
	 *            the library to highlight the results in
	 * @param results
	 *            the results to highlight
	 */
	public ResultHighlighter(PTree library, LibrarySearchResults results)
			throws IllegalActionException {
		super(library, results);
	}

	/**
	 * this method allows the search results to be updated in the panel
	 * 
	 * @param results
	 *            the results to update to
	 */
	public void update(LibrarySearchResults results) {
		this.results = results;
		if (results != null) {
			TreePath[] tp = createTreePathArray();
			library.clearSelection(); // clear the current selections
			collapseAll();
			for (int i = 0; i < tp.length; i++) {
				// library.expandPath(tp[i]);
				library.addSelectionPath(tp[i]);
			}
		}
	}

	/**
	 * A factory that creates the searcher to search the library
	 */
	public static class Factory extends LibrarySearchResultPaneFactory {
		/**
		 * Create an factory with the given name and container.
		 * 
		 * @param container
		 *            The container.
		 * @param name
		 *            The name of the entity.
		 * @exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 * @exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * creates a ResultsHighlighter and returns it.
		 * 
		 * @param _libraryModel
		 *            the model containing the actor library
		 * @return A new LibraryPane that displays the library
		 */
		public LibrarySearchResultPane createLibrarySearchResultPane(
				PTree library, LibrarySearchResults results)
				throws IllegalActionException {
			return new ResultHighlighter(library, results);
		}
	}
}