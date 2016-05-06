/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2011-01-26 17:54:56 -0800 (Wed, 26 Jan 2011) $' 
 * '$Revision: 26850 $'
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

import javax.swing.JTree;

import org.kepler.authentication.AuthenticationException;
import org.kepler.objectmanager.repository.RepositoryException;

import ptolemy.kernel.util.IllegalActionException;

/**
 * this abstract class is should be extended by all classes that provide a
 * search engine for the actory library. Any local variables in the extending
 * classes should be initialized in the init method because it is called from
 * the constructor of this class.
 * 
 *@author berkley
 *@since February 17, 2005
 */
public abstract class LibrarySearcher {
	/**
	 * the library to search
	 */
	protected JTree _library;
	/**
	 * Description of the Field
	 */
	protected LibrarySearchResults _results;
	/**
	 * Description of the Field
	 */
	protected LibrarySearchPane _searchPane;
	/**
	 * a user setable properties hash
	 */
	protected Hashtable<String,Object> _properties;

	/**
	 * constructor
	 * 
	 *@param library
	 *            Description of the Parameter
	 *@param searchPane
	 *            Description of the Parameter
	 */
	public LibrarySearcher(JTree library, LibrarySearchPane searchPane) {
		_library = library;
		_searchPane = searchPane;
		_results = new LibrarySearchResults();
		_properties = new Hashtable<String,Object>();
		init();
	}

	/**
	 * set a user setable property
	 */
	public void setProperty(String name, Object value) {
		_properties.put(name, value);
	}

	/**
	 * returns a user set property
	 */
	public Object getProperty(String name) {
		return _properties.get(name);
	}

	/**
	 * search for value in the library
	 * 
	 *@param value
	 *            the value to search for
	 *@param searchRemotely
	 *            true if a remote search should take place.
	 *@param authenticate
	 *			  true will prompt user to login if necessary.
	 *@return LibrarySearchResults the results of the search
	 * @throws RepositoryException 
	 * @throws AuthenticationException 
	 */
	public abstract LibrarySearchResults search(String value, 
			boolean authenticate) throws IllegalActionException, RepositoryException, AuthenticationException;

	/**
	 * provides any initialization needed prior to searching. It is called when
	 * the class is constructed. Note that any local variables of extending
	 * classes should be initialized in init since it is called be the
	 * constructor of the super class (LibrarySearcher).
	 */
	protected abstract void init();
}