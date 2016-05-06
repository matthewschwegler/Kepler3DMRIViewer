/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2011-01-27 17:55:07 -0800 (Thu, 27 Jan 2011) $' 
 * '$Revision: 26856 $'
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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.seek.sms.AnnotationEngine;
import org.kepler.moml.NamedObjId;

import ptolemy.kernel.util.NamedObj;

/**
 * A very simple, first draft ontology-based library search engine
 * 
 *@author berkley
 *@since October 30, 2006
 *
 *@deprecated This class is unused, so deprecating. Consider 
 *for deletion? -01/27/11 derik
 */
public class OntLibrarySearcher extends LibrarySearcher {
	private static final Log log = LogFactory.getLog(OntLibrarySearcher.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private AnnotationEngine _engine = null;
	// for searching
	private Stack<NamedObj> _pathStack;
	// the stack to create the return paths with
	private Hashtable<String, Vector<TreePath>> _hashTable;

	// key is lsid of leaf, object is vector of treepaths to leaf

	/**
	 * Constructor
	 * 
	 *@param library
	 *            Description of the Parameter
	 *@param searchPane
	 *            Description of the Parameter
	 */
	public OntLibrarySearcher(JTree library, LibrarySearchPane searchPane) {
		super(library, searchPane);
		_engine = AnnotationEngine.instance();
	}

	/**
	 * Search for ontology-keyword in the library
	 * 
	 *@param value
	 *            the keyword in the ontology to search for
	 *@param authenticate
	 *			  if true prompt user to login if necessary
	 *@return Description of the Return Value
	 */
	public LibrarySearchResults search(String value, boolean authenticate) {
		// assign the new results of the search
		_results = new LibrarySearchResults();
		// this is a protected class member
		if (value.trim().equals("")) {
			return _results;
		}

		// build the hash table
		NamedObj root = (NamedObj) _library.getModel().getRoot();
		_pathStack = new Stack<NamedObj>();
		_pathStack.push(root);
		_hashTable = new Hashtable<String, Vector<TreePath>>();
		buildHashTable(root, _library.getModel());

		// get the ontology search results
		Vector<NamedObj> ont_results = _engine.search(value, true);
		// find the appropriate results in the library
		Iterator<NamedObj> iter = ont_results.iterator();
		while (iter.hasNext()) {
			NamedObj obj = iter.next();
			Iterator<String> idIter = getIds(obj).iterator();
			// for each id, add the tree path to the result
			while (idIter.hasNext()) {
				String id = idIter.next();
				Iterator<TreePath> pathIter = hashTableGet(id);
				while (pathIter.hasNext()) {
					TreePath path = pathIter.next();
					if (!_results.contains(path)) {
						_results.add(path);
					}
				}
			}
		}
		return _results;
	}

	//
	// helper function to build the hash table of paths leading to NamedObj's
	// with ids
	private void buildHashTable(NamedObj parent, TreeModel model) {
		for (int i = 0; i < model.getChildCount(parent); i++) {
			NamedObj child = (NamedObj) model.getChild(parent, i);
			_pathStack.push(child);
			if (model.isLeaf(child)) {
				Iterator<String> iter = getIds(child).iterator();
				while (iter.hasNext()) {
					TreePath path = new TreePath(_pathStack.toArray());
					hashTablePut(iter.next(), path);
				}
			} else {
				buildHashTable(child, model);
			}

			_pathStack.pop();
		}

	}

	//
	// helper function: given named object returns it's ids
	//
	/**
	 * Gets the ids attribute of the OntLibrarySearcher object
	 * 
	 *@param obj
	 *            Description of the Parameter
	 *@return The ids value
	 */
	private Vector<String> getIds(NamedObj obj) {
		Vector<String> ids = new Vector<String>();
		List<NamedObjId> idAtts = obj.attributeList(NamedObjId.class);
		Iterator<NamedObjId> iter = idAtts.iterator();
		while (iter.hasNext()) {
			NamedObjId id = (NamedObjId) iter.next();
			ids.add(id.getExpression());
		}
		return ids;
	}

	//
	// helper function: add an (id, path) to _hashtable
	//
	private void hashTablePut(String id, TreePath path) {
		if (!_hashTable.containsKey(id)) {
			Vector<TreePath> paths = new Vector<TreePath>();
			paths.add(path);
			_hashTable.put(id, paths);
		} else {
			Vector<TreePath> paths = _hashTable.get(id);
			if (!paths.contains(path)) {
				paths.add(path);
			}
		}
	}

	//
	// helper function: given an id, returns an iterator of its paths
	//
	private Iterator<TreePath> hashTableGet(String id) {
		Vector<TreePath> ids = _hashTable.get(id);
		if (ids == null) {
			return new Vector<TreePath>().iterator();
		}
		return ids.iterator();
	}

	/**
	 * provides any initialization needed prior to searching. It is called when
	 * the class is constructed. Note that any local variables of extending
	 * classes should be initialized in init since it is called be the
	 * constructor of the super class (LibrarySearcher).
	 */
	protected void init() {
	}

}
// OntLibrarySearcher