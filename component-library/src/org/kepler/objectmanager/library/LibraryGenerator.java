/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2011-04-08 16:06:46 -0700 (Fri, 08 Apr 2011) $' 
 * '$Revision: 27484 $'
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

package org.kepler.objectmanager.library;

import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.EntityLibrary;

/**
 * This class is used for generating the Component Library from the prebuilt
 * LibIndex. It's purpose is to separate out the functionality of generating an
 * entire library using the index from the function of dynamically changing the
 * library contents one item or node at a time, as the functions of the
 * LibraryManager do. This will keep confusion about the API to a minimum
 * (before they were all in the same class).
 * 
 * @author Aaron Schultz
 */
public class LibraryGenerator {

	private static final Log log = LogFactory.getLog(LibraryGenerator.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * Convenience reference.
	 */
	private LibraryManager _libMan;

	public LibraryGenerator() {
		_libMan = LibraryManager.getInstance();
	}

	public CompositeEntity generate(Workspace workspace, LibIndex libIndex) {
		CompositeEntity _root = null;
		try {
			_root = (CompositeEntity) new EntityLibrary(workspace);
			_root.setName("kepler actor library");
		} catch (ptolemy.kernel.util.NameDuplicationException nde) {
			// do nothing, just leave the name blank
			System.out.println("the name 'kepler actor library' is already in use");
		} catch (IllegalActionException e) {
			e.printStackTrace();
		}
		if (_root == null) return null;

		Stack<LibItem> s = new Stack<LibItem>();
		Stack<NamedObj> treeObjs = new Stack<NamedObj>();
		treeObjs.push(_root);

		Vector<LibItem> items = libIndex.getItems();
		System.gc();
		for (LibItem item : items) {
			// if (isDebugging) log.debug(item.debugString());
			try {
				// if (isDebugging) log.debug("s.size(): " + s.size());
				// if (isDebugging) log.debug("treeObjs.size(): " +
				// treeObjs.size());
				while (s.size() > 0) {
					int lastRight = s.elementAt(s.size() - 1).getRight();
					int thisRight = item.getRight();
					if (lastRight < thisRight) {
						// if (isDebugging)
						// log.debug(s.elementAt(s.size()-1).getRight() + " < "
						// + item.getRight());
						s.pop();
						treeObjs.pop();
					} else {
						break;
					}
				}
				if (isDebugging) log.debug( s.toString() );
				if (isDebugging) log.debug( treeObjs.toString() );
				NamedObj parent = treeObjs.elementAt(treeObjs.size() - 1);
				if (isDebugging) log.debug(parent.getName() + " " + parent.getClass().getName());
				if (parent instanceof CompositeEntity) {
					ComponentEntity current = _libMan.createAndAddTreeItem(
							(CompositeEntity) parent, item);
					if (isDebugging) {
						String indent = " ";
						for (int i = 0; i < treeObjs.size(); i++) {
							indent += "+-";
						}
						log.debug(indent + " " + current.getName() + "     "
								+ current.getClass().getName());
					}
					s.push(item);
					treeObjs.push(current);
				} else {
					log
							.error("Parent is something other than CompositeEntity!  Should never happen.");
					// Try refreshing the order
					
					// Try complete reubild
					return _root;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.gc();
		return _root;
	}

}
