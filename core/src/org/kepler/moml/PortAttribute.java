/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.moml;

import java.util.Collection;
import java.util.Vector;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.ValueListener;
import ptolemy.kernel.util.Workspace;

/**
 * This implements a DependencyAttribute for moml properties.
 * 
 *@author berkley
 *@created Aug. 19, 2005
 */
public class PortAttribute extends StringAttribute {
	/** container for the value */
	private String value = null;
	/** container for valueListeners assigned to this attribute */
	private Vector valueListeners = new Vector();

	/** Constructor */
	public PortAttribute() {
		super();
	}

	/**
	 * Constructor
	 * 
	 *@param container
	 *            Description of the Parameter
	 *@param name
	 *            Description of the Parameter
	 *@exception IllegalActionException
	 *                Description of the Exception
	 *@exception NameDuplicationException
	 *                Description of the Exception
	 */
	public PortAttribute(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * Constructor
	 * 
	 *@param workspace
	 *            Description of the Parameter
	 */
	public PortAttribute(Workspace workspace) {
		super(workspace);
	}

	/**
	 * returns the default expression which is null
	 * 
	 *@return The defaultExpression value
	 */
	public String getDefaultExpression() {
		return null;
	}

	/**
	 * set the value of this id
	 * 
	 *@param expression
	 *            The new expression value
	 */
	public void setExpression(String expression) {
		// System.out.println("setting NamedObjId to " + expression);
		value = expression;
		// set the value

		for (int i = 0; i < valueListeners.size(); i++) {
			// notify any listeners of the change
			ValueListener listener = (ValueListener) valueListeners
					.elementAt(i);
			listener.valueChanged(this);
		}
	}

	/**
	 * return the value of the id
	 * 
	 *@return The expression value
	 */
	public String getExpression() {
		return value;
	}

	/**
	 * add a valueListener
	 * 
	 *@param listener
	 *            The feature to be added to the ValueListener attribute
	 */
	public void addValueListener(ValueListener listener) {
		valueListeners.add(listener);
	}

	/**
	 * NamedObjIds should be invisible to the user
	 * 
	 *@return The visibility value
	 */
	public Settable.Visibility getVisibility() {
		return NONE;
	}

	/**
	 * this method does not change the visibility. NamedObjId should only be
	 * invisible
	 * 
	 *@param visibility
	 *            The new visibility value
	 */
	public void setVisibility(Settable.Visibility visibility) {
		// do nothing....we don't want the visibility getting changed
	}

	/**
	 * remove the indicated listener
	 * 
	 *@param listener
	 *            Description of the Parameter
	 */
	public void removeValueListener(ValueListener listener) {
		valueListeners.remove(listener);
	}

	/** validate the expression. */
	public Collection validate() {
		// don't need to do anything here
		return null;
	}

	/**
	 * Description of the Method
	 * 
	 *@param obj
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof NamedObjId)) {
			return false;
		}
		NamedObjId objId = (NamedObjId) obj;
		String str = objId.getExpression();
		if (this.getExpression() == null) {
			if (str != null) {
				return false;
			}
			return true;
		}
		return this.getExpression().equals(objId.getExpression());
	}
}