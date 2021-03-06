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
import java.util.Iterator;

import ptolemy.kernel.Entity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.Workspace;

/**
 * A KeplerVirtualPortReference refers to a kepler virtual port within its
 * containing entity. The virutal port is referenced by its name within the
 * context of the entity (actor).
 * 
 * @author Shawn Bowers
 * @created June 14, 2005
 * @see ptolemy.kernel.Port
 */
public class KeplerVirtualIOPortReference extends KeplerIOPortReference {

	/** Constructor */
	public KeplerVirtualIOPortReference() {
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
	public KeplerVirtualIOPortReference(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * Constructor
	 * 
	 * @param workspace
	 *            Description of the Parameter
	 */
	public KeplerVirtualIOPortReference(Workspace workspace) {
		super(workspace);
	}

	/**
	 * Return the default expression which is null
	 * 
	 * @return The defaultExpression value. Currently if not given, then
	 *         unknown.
	 */
	public String getDefaultExpression() {
		return null;
	}

	/**
	 * @return The virtual port being referenced.
	 */
	public Object getPort() {
		// get the container, etc.
		String portName = getExpression();

		// iterate to first entity container
		NamedObj container = getContainer();
		while (container != null && !(container instanceof Entity))
			container = container.getContainer();

		// couldn't find a container
		if (container == null)
			return null;

		Entity actor = (Entity) container;
		for (Iterator ports = actor.attributeList().iterator(); ports.hasNext();) {
			Object obj = ports.next();
			if (obj instanceof KeplerVirtualIOPort) {
				KeplerVirtualIOPort port = (KeplerVirtualIOPort) obj;
				if (portName == null && port.getName() == null)
					return port;
				else if (portName.equals(port.getName()))
					return port;
			}
		}
		return null;
	}

	/**
	 * Sets the reference to the given virtual port.
	 * 
	 * @param vport
	 *            The virtual port that this reference will refer to. The
	 *            virtual port must be within the same container.
	 */
	public void setPort(KeplerVirtualIOPort port) throws IllegalActionException {
		this.setExpression(port.getName());
	}

	/**
	 * Set visibility to NOT_EDITABLE
	 * 
	 * @return The visibility value
	 */
	public Settable.Visibility getVisibility() {
		// return NONE; // this should be changed?
		// return ptolemy.kernel.util.Settable.FULL;
		return ptolemy.kernel.util.Settable.NOT_EDITABLE;
	}

	/**
	 * The visibility cannot be changed. This method does nothing.
	 * 
	 * @param visibility
	 *            The new visibility value
	 */
	public void setVisibility(Settable.Visibility visibility) {
		// do nothing....we don't want the visibility getting changed
	}

	/**
	 * Validate the expression. The expression is valid if it has a
	 * corresponding virtual port.
	 */
	public Collection validate() throws IllegalActionException {
		// want to make sure the port exists ...
		if (getPort() == null) {
			String msg = "Could not find virtual port '" + getExpression()
					+ "'";
			throw new IllegalActionException(this, msg);
		}
		return null;
	}

	/**
	 * References are equal when they refer to the same port.
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof KeplerVirtualIOPortReference))
			return false;
		KeplerVirtualIOPortReference ref = (KeplerVirtualIOPortReference) obj;
		if (getPort() == null)
			return false;
		return getPort().equals(ref.getPort());
	}

} // KeplerVirtualIOPortReference