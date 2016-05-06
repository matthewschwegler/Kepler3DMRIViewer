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

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;

/**
 * A data structure to hold a semantic property in a MoML file. The container of
 * a semantic property is currently limited to KeplerIOPortSemanticLinks.
 * 
 * @author Shawn Bowers
 * @created June 20, 2005
 * @see org.kepler.sms.KeplerIOPortSemanticLink
 */
public class SemanticProperty extends StringAttribute {

	/** Constructor */
	public SemanticProperty() {
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
	public SemanticProperty(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * Constructor
	 * 
	 * @param workspace
	 *            Description of the Parameter
	 */
	public SemanticProperty(Workspace workspace) {
		super(workspace);
	}

	/**
	 * Set the container of the semantic property to the given container. This
	 * method fails, in addition to the normal ptolemy constraints for setting
	 * containers, if the given container is not a
	 * org.kepler.sms.KeplerIOPortSemanticLink object.
	 * 
	 * @param container
	 *            The container for this virtual port.
	 */
	public void setContainer(NamedObj container) throws IllegalActionException,
			NameDuplicationException {
		if (!(container instanceof KeplerIOPortSemanticLink)) {
			String msg = "Container not an instance of KeplerIOPortSemanticLink for semantic property '"
					+ getName() + "'";
			throw new IllegalActionException(this, msg);
		}
		super.setContainer(container);
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
	 * set the semantic property id value
	 * 
	 * @param expression
	 *            The new semantic type concept id
	 */
	public void setPropertyId(String expr)
			throws ptolemy.kernel.util.IllegalActionException {
		setExpression(expr);
	}

	/**
	 * return the value of the semantic property (the id)
	 * 
	 * @return The concept id
	 */
	public String getPropertyId() {
		return getExpression();
	}

	/**
	 * SemanticTypes should be invisible to the user
	 * 
	 * @return The visibility value
	 */
	public Settable.Visibility getVisibility() {
		// return NONE; // this should be changed?
		// return ptolemy.kernel.util.Settable.FULL;
		return ptolemy.kernel.util.Settable.NOT_EDITABLE;
	}

	/**
	 * this method does not change the visibility. SemanticType should only be
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
		if (!(obj instanceof SemanticProperty))
			return false;
		SemanticProperty sempropId = (SemanticProperty) obj;
		String str = sempropId.getExpression();
		if (this.getExpression() == null) {
			if (str != null)
				return false;
			return true;
		}
		return this.getExpression().equals(sempropId.getExpression());
	}

} // SemanticType