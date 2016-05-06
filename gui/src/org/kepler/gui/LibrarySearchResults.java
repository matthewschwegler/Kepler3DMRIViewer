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

package org.kepler.gui;

import java.util.Vector;

import javax.swing.tree.TreePath;

/**
 * A class to contain the search results from the library. Conceivably, this
 * class could hold any result of a tree search.
 * 
 *@author berkley
 *@since February 17, 2005
 */
public class LibrarySearchResults extends Vector {
	/**
	 * constructor
	 */
	public LibrarySearchResults() {
		super();
	}

	/**
	 * adds a TreePath to the results
	 * 
	 *@param path
	 *            the TreePath to add to the results
	 */
	public void add(TreePath path) {
		// System.out.println("path: " + path.toString());
		super.addElement(path);
	}

	/**
	 * returns the result at the specified index
	 * 
	 *@param index
	 *            the index of the result to return
	 *@return The treePath value
	 */
	public TreePath getTreePath(int index) {
		return (TreePath) super.elementAt(index);
	}

	/**
	 * return a formatted string representation of this object
	 * 
	 *@return Description of the Return Value
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\n");
		for (int i = 0; i < this.size(); i++) {
			// We need to call toString() on TreePath so as to
			// avoid "The method append(Object) is ambiguous"
			// under Eclipse 3.2 with Java 1.5
			sb.append(this.getTreePath(i).toString()).append("\n");
		}
		sb.append("\n}");
		return sb.toString();
	}
}