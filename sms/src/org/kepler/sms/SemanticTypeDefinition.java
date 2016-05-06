/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
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

package org.kepler.sms;

import java.util.Collection;
import java.util.Vector;

import ptolemy.kernel.util.ConfigurableAttribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.Workspace;

/**
 * A data structure to hold a semantic type given in a kar file. The property
 * can contain a configuration (configure element), which gives the concept
 * definition in OWL-DL. One can also give a source file, however, this
 * functionality is not yet supported.
 * 
 *@author bowers
 *@created May 9, 2005
 */
public class SemanticTypeDefinition extends ConfigurableAttribute {

	/** container for the value */
	private String _conceptId; // the main value of the property (the class
								// name)
	private String _conceptDef; // the concept definition ... if one exists
	private String _conceptSrc; // the source file ... if one exists

	/** container for valueListeners assigned to this attribute */
	private Vector _valueListeners = new Vector();

	/** Constructor */
	public SemanticTypeDefinition() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param container
	 *            Description of the Parameter
	 * @param name
	 *            The value of the property
	 * @exception IllegalActionException
	 *                Description of the Exception
	 * @exception NameDuplicationException
	 *                Description of the Exception
	 */
	public SemanticTypeDefinition(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * Constructor
	 * 
	 * @param workspace
	 *            Description of the Parameter
	 */
	public SemanticTypeDefinition(Workspace workspace) {
		super(workspace);
	}

	/**
	 * returns the default expression which is null
	 * 
	 * @return The defaultExpression value. Currently if not given, then
	 *         unknown.
	 */
	public String getDefaultExpression() {
		return null;
	}

	/**
	 * return the value of the semantic type
	 * 
	 * @return The concept id
	 */
	public String getConceptId() {
		return null; // returns the top-level class name used (on-the-fly)
	}

	/**
     * 
     */
	public void setConceptDef(String text) throws Exception {
		configure(null, null, text);
	}

	/**
     * 
     */
	public String getConceptDef() throws java.io.IOException {
		return value(); // returns the entire definition
	}

	/**
	 * @return The visibility value
	 */
	public Settable.Visibility getVisibility() {
		// return ptolemy.kernel.util.Settable.FULL;
		return ptolemy.kernel.util.Settable.NOT_EDITABLE;
	}

	/**
	 * this method does not change the visibility. SemanticType should always be
	 * invisible
	 * 
	 * @param visibility
	 *            The new visibility value
	 */
	public void setVisibility(Settable.Visibility visibility) {
		// do nothing....we don't want the visibility getting changed
	}

	/** validate the expression. */
	public Collection validate() throws IllegalActionException {
		// ... may want to change this in the future
		return null;
	}

	/**
	 * Description of the Method
	 * 
	 * @param obj
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	public boolean equals(Object obj) {
		return false; // needs work
	}

	// other methods: isSubConcept(); isSuperConcept(); isEquivalentConcept();

} // SemanticTypeDefinition