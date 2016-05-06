/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-01-23 14:31:43 -0800 (Wed, 23 Jan 2013) $' 
 * '$Revision: 31365 $'
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

package org.geon;

import ptolemy.actor.lib.Const;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import ptolemy.vergil.icon.BoxedValueIcon;

//////////////////////////////////////////////////////////////////////////
//// ConstOnce
/**
 * Produce a constant output once. The value of the output is that of the token
 * contained by the value parameter, which by default is an IntToken with value
 * 1. The type of the output is that of value parameter. The actor emits the
 * parameter value on the ouput port during a single fire event.
 * 
 * @UserLevelDocumentation This actor produces a constant value once. The value
 *                         is set by the user as a parameter of the actor, or
 *                         defaults to an integer value of 1 if unset. The actor
 *                         emits the parameter value on the ouput port once. A
 *                         typical usage of this actor is used to parameterize
 *                         other models that take constant values as inputs.
 * @author Efrat Jaeger
 * @version $Id: ConstOnce.java 31365 2013-01-23 22:31:43Z jianwu $
 * @since Ptolemy II 3.0.2
 * @deprecated Use ptolemy.actor.lib.Const instead.
 */

@Deprecated
public class ConstOnce extends Const {

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public ConstOnce(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		BoxedValueIcon icon = new BoxedValueIcon(this, "_icon");
		icon.displayWidth.setExpression("25");
		icon.attributeName.setExpression("value");
		//disable this parameter since it is not used in this actor.
		firingCountLimit.setVisibility(Settable.NONE);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Set postfire to false.
	 * 
	 * @exception IllegalActionException
	 *                If thrown by the super class.
	 */
	public boolean postfire() throws IllegalActionException {
		return false;
	}
}