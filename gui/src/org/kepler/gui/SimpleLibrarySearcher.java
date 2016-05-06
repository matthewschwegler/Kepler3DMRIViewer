/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2013-01-16 16:52:56 -0800 (Wed, 16 Jan 2013) $' 
 * '$Revision: 31343 $'
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.authentication.AuthenticationException;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.objectmanager.repository.Repository;
import org.kepler.objectmanager.repository.RepositoryException;
import org.kepler.objectmanager.repository.RepositoryManager;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.EntityLibrary;

/**
 * class to search the library. This class uses a simple name comparator to
 * determine results. It uses a depth first traversal of the tree.
 * 
 *@author berkley
 *@since February 17, 2005
 */
public class SimpleLibrarySearcher extends LibrarySearcher {
	private static final Log log = LogFactory
			.getLog(SimpleLibrarySearcher.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	// the stack to create the return paths with
	private Stack<Object> _pathStack;
	private boolean skipRepositoryOntology = false;

	/**
	 * constructor
	 * 
	 *@param library
	 *            Description of the Parameter
	 *@param searchPane
	 *            Description of the Parameter
	 */
	public SimpleLibrarySearcher(JTree library, LibrarySearchPane searchPane) {
		super(library, searchPane);
	}
	
	/**
	 * Set true if want not to display remote ontology
	 */
	public void setSkipReposiotryOntology(boolean skipRepositoryOntology){
	  this.skipRepositoryOntology = skipRepositoryOntology;
	}

	/**
	 * search for val in the library
	 * 
	 *@param val
	 *            Description of the Parameter
	 *@param authenticate
	 *			  if true prompt user to login if necessary
	 *@return Description of the Return Value
	 * @throws AuthenticationException 
	 * @throws RepositoryException 
	 */
	public LibrarySearchResults search(String value, boolean authenticate)
			throws IllegalActionException, RepositoryException, AuthenticationException {
		if (isDebugging) {
			log.debug("search(" + value + ")");
		}

		Long searchTime = System.currentTimeMillis();

		value = value.trim();

		// need to reset the results before doing the search
		_results = new LibrarySearchResults();
		if (value.trim().equals("")) {
			return _results;
		}

		// We'll need the tree model for the search
		TreeModel model = _library.getModel();

		// check if the search string is a KeplerLSID
		if (KeplerLSID.isKeplerLSIDFormat(value)) {
			if (isDebugging)
				log.debug("User is searching for an LSID");

			try {
				KeplerLSID lsid = new KeplerLSID(value);
				Vector<KeplerLSID> lsids = new Vector<KeplerLSID>(1);
				lsids.add(lsid);
				if (isDebugging)
					log.debug("Searching for " + lsid);

				Vector<Integer> liids = LibraryManager.getInstance()
						.getLiidsFor(lsids);
				findLiids(liids, model);
			} catch (Exception e) {
				Vector<Integer> liids = LibraryManager.getInstance().getIndex()
						.getSearcher().search(value);
				findLiids(liids, model);
			}

		} else {
			Vector<Integer> liids = LibraryManager.getInstance().getIndex()
					.getSearcher().search(value);
			if (isDebugging)
				log.debug("Index search generated " + liids.size()
						+ " results in "
						+ (System.currentTimeMillis() - searchTime) + "ms");
			searchTime = System.currentTimeMillis();
			findLiids(liids, model);
			if (isDebugging)
				log.debug("Model search generated " + _results.size()
						+ " results in "
						+ (System.currentTimeMillis() - searchTime) + "ms");

		}
		searchTime = System.currentTimeMillis();
		if (isDebugging)
			log.debug("Start Repository Search");
		try {
			RepositoryManager rm = null;
			try {
				rm = RepositoryManager.getInstance();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (Repository r : rm.getRepositories()) {
				if (r.includeInSearch()) {
					if (isDebugging) {
						log.debug("Searching remote repository " + r.getName());
					}

					// use EcogridRepositoryLibrarySearcher as the default
					String className = "org.kepler.objectmanager.repository.EcogridRepositoryLibrarySearcher";
					List propList = ConfigurationManager.getInstance()
							.getProperties(
									ConfigurationManager.getModule("gui"));
					List reposSearcherList = ConfigurationProperty
							.findProperties(propList, "name",
									"repositorySearcher", true);
					if (reposSearcherList != null
							&& reposSearcherList.size() > 0) {
						className = ((ConfigurationProperty) reposSearcherList
								.get(0)).getProperty("value").getValue();
					}

					// Use reflection to instantiate the class
					Class searchClass = Class.forName(className);

					Class[] args = new Class[] { JTree.class,
							LibrarySearchPane.class, String.class };

					// create a constructor
					Constructor constructor = searchClass.getConstructor(args);

					// set the args
					Object[] argImp = new Object[] { _library, _searchPane,
							r.getName() };

					RepositorySearcher reposSearcher = (RepositorySearcher) constructor
							.newInstance(argImp);
					reposSearcher.setSkipOntology(skipRepositoryOntology);
					// search the ecogrid repository
					// you must call this or the
					// default repository name of 'defaultRepository' is used.
					log.debug("calling search using RepositorySearcher of class:"+className);
					Iterator itr = null;
					try{
						itr = reposSearcher.search(value, authenticate).iterator();
						while (itr.hasNext()) {
							TreePath p = (TreePath) itr.next();
							if (!_results.contains(p)) {
								_results.add(p);
							}
						}
					}catch(AuthenticationException ae){
						if (ae.getType() != AuthenticationException.USER_CANCEL) {
							throw ae;
						}
						log.debug("caught AuthenticationException of type USER_CANCEL, taking no action, proceed...");
					}
				}
			}
			if (isDebugging)
				log.debug("End Repository Search: "
						+ (System.currentTimeMillis() - searchTime) + "ms");

		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return _results;
	}

	/**
	 * pre-reads the library so searches will be faster. When the momlparser
	 * hits an error in the moml, it rebuilds the model which in turn collapses
	 * the entire library JTree. This presearches the tree which causes the
	 * parse errors to get thrown before the first search so the tree won't be
	 * collapsed the first time you search.
	 */
	protected void init() {
		_pathStack = new Stack<Object>();
	}

	/**
	 * Search the tree model and add any components that match any of the
	 * supplied KeplerLSIDs to the results. This method does not match
	 * EntityLibraries but only checks leaf nodes that are ComponentEntities.
	 * 
	 * @param lsids
	 * @param model
	 */
	private void findLiids(Vector<Integer> liids, TreeModel model) {
		if (isDebugging)
			log.debug("findLsids(" + liids + " " + model.getRoot() + "");

		Object o = model.getRoot();

		// start from the root
		_pathStack = new Stack<Object>();

		_pathStack.push(o);

		for (int i = 0; i < model.getChildCount(o); i++) {
			Object child = model.getChild(o, i);
			if (child instanceof NamedObj) {
				NamedObj nobjChild = (NamedObj) child;
				String name = nobjChild.getName();
				if (name.startsWith("_"))
					continue;

				_pathStack.push(child);

				if (nobjChild instanceof EntityLibrary) {
					findLiids(liids, (EntityLibrary) nobjChild);
				} else if (nobjChild instanceof ComponentEntity) {
					checkLiid((ComponentEntity) nobjChild, liids);
				}
			}
		}
	}

	/**
	 * Recursive helper function for findLsids(Vector<KeplerLSID> lsids,
	 * TreeModel model)
	 * 
	 * @param lsids
	 * @param entity
	 */
	private void findLiids(Vector<Integer> liids, EntityLibrary entity) {
		if (isDebugging)
			log.debug("findLsids(" + liids + " " + entity.getName() + ")");

		// loop through the children of the entity
		for (Iterator i = entity.containedObjectsIterator(); i.hasNext();) {
			NamedObj e = (NamedObj) i.next();
			String name = e.getName();
			if (name.startsWith("_"))
				continue;

			_pathStack.push(e);

			if (e instanceof EntityLibrary) {
				// recurse
				findLiids(liids, (EntityLibrary) e);
			} else if (e instanceof ComponentEntity) {
				// leaf node
				checkLiid((ComponentEntity) e, liids);
				popstack();
			} else {
				popstack();
			}
		}
		popstack();

	}

	/**
	 * Check to see if the supplied NamedObj matches any of the LIIDs in the
	 * supplied list.
	 * 
	 * @param nobj
	 * @param lsids
	 */
	private boolean checkLiid(ComponentEntity e, Vector<Integer> liids) {
		if (isDebugging)
			log.debug("checkLsid(" + e.getName() + " " + liids + ")");
		int thisLiid = LibraryManager.getLiidFor(e);
		if (thisLiid != -1) {
			Integer iLiid = new Integer(thisLiid);
			if (isDebugging)
				log.debug(iLiid);
			if (liids.contains(iLiid)) {
				if (isDebugging)
					log.debug("MATCH");
				addStackTopToResult();
				return true;
			}
		}
		return false;
	}

	/**
	 * pop the top off the stackeroo
	 */
	private void popstack() {
		if (!_pathStack.empty()) {
			_pathStack.pop();
		}
	}

	/**
	 * adds the path at the top of the stack to the result
	 */
	private void addStackTopToResult() {
		_results.add(createPathFromStack());
	}

	/**
	 * return the path as a TreePath
	 * 
	 *@return Description of the Return Value
	 */
	private TreePath createPathFromStack() {
		return new TreePath(_pathStack.toArray());
	}
}