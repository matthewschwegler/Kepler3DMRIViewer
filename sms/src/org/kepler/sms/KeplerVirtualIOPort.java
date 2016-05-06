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

import ptolemy.data.type.Type;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * A virtual IO port is a basic (property) attribute. The name of the property
 * is the name of the (virtual) IO port.
 * 
 * @author Shawn Bowers
 * @created June 15, 2005
 */

public abstract class KeplerVirtualIOPort extends Attribute {

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
	public KeplerVirtualIOPort(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * @return True if the port is considered an output.
	 */
	public abstract boolean isOutput();

	/**
	 * @return True if the port is considered an input.
	 */
	public abstract boolean isInput();

	/**
	 * @return The data type of the virtual port. If no type exists, then null.
	 */
	public abstract Type getType();

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
		if (!(container instanceof Entity)) {
			String msg = "Container not an instance of ptolemy.kernel.Entity for virtual port '"
					+ getName() + "'";
			throw new IllegalActionException(this, msg);
		}
		super.setContainer(container);
	}

} // KeplerVirtualIOPort