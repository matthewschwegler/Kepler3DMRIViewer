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

package org.kepler.sms.actors;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;

/**
 * 
 * @author Shawn Bowers
 * @created October 17, 2005
 */

public class SimpleMergeSourceActorPort extends StringAttribute {

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
	public SimpleMergeSourceActorPort(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * @return The source actor names.
	 */
	public String getSourceActorPort() {
		return getExpression();
	}

	/**
	 * Sets the source actor port name
	 */
	public void setSourceActorPort(String name) throws IllegalActionException {
		setExpression(name);
	}

	/**
	 * Set the container of the virtual port to the given container. This method
	 * fails, in addition to the normal ptolemy constraints for setting
	 * containers, if the given container is not a ptolemy.kernel.Entity object.
	 * 
	 * @param container
	 *            The container for this virtual port.
	 */
	public void setContainer(NamedObj container) throws IllegalActionException,
			NameDuplicationException {
		if (!(container instanceof SimpleMergeMapping)) {
			String msg = "This attribute can only be applied to "
					+ "org.Kepler.sms.actor.SimpleMergeMapping instances.";
			throw new IllegalActionException(this, msg);
		}
		super.setContainer(container);
	}

} // SimpleMergeSourceActorPort