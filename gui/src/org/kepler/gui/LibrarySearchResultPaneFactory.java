/*
 * Copyright (c) 1997-2010 The Regents of the University of California.
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

import java.util.Iterator;

import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.tree.PTree;

/**
 * This class creates a pane that displays the search results. This is a
 * pluggable interface defined in the kepler configuration.
 * 
 *@author Chad Berkley
 *@since February 17, 2005
 *@version $Id: LibrarySearchResultPaneFactory.java,v 1.3 2004/08/25 20:50:46
 *          berkley Exp $
 *@since Kepler 1.0 alpha 2
 */
public class LibrarySearchResultPaneFactory extends Attribute {

	/**
	 * Create a factory with the given name and container.
	 * 
	 *@param container
	 *            The container.
	 *@param name
	 *            The name.
	 *@exception IllegalActionException
	 *                If the container is incompatible with this attribute.
	 *@exception NameDuplicationException
	 *                If the name coincides with an attribute already in the
	 *                container.
	 */
	public LibrarySearchResultPaneFactory(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * This method returns a LibrarySearchResultPane which is used in the
	 * pluggable interface for the search result pane
	 * 
	 *@param library
	 *            Description of the Parameter
	 *@param results
	 *            Description of the Parameter
	 *@return A tableau for the effigy, or null if one cannot be created.
	 *@exception IllegalActionException
	 *                Description of the Exception
	 */
	public LibrarySearchResultPane createLibrarySearchResultPane(PTree library,
			LibrarySearchResults results) throws IllegalActionException {
		LibrarySearchResultPane pane = null;
		Iterator factories = attributeList(LibrarySearchResultPaneFactory.class)
				.iterator();
		while (factories.hasNext() && pane == null) {
			LibrarySearchResultPaneFactory factory = (LibrarySearchResultPaneFactory) factories
					.next();
			pane = factory.createLibrarySearchResultPane(library, results);
		}
		return pane;
	}
}