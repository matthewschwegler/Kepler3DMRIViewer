/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.sdm.spa;

import ptolemy.actor.lib.Transformer;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// StringReverse
/**
 * Output the reverse of a string provided at the input.
 * 
 * @author xiaowen
 * @version $Id: StringReverse.java 24234 2010-05-06 05:21:26Z welker $
 */

public class StringReverse extends Transformer {

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
	public StringReverse(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Set the types of the ports.
		input.setTypeEquals(BaseType.STRING);
		output.setTypeEquals(BaseType.STRING);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Clone the actor into the specified workspace.
	 * 
	 * @param workspace
	 *            The workspace for the new object.
	 * @return A new actor.
	 * @exception CloneNotSupportedException
	 *                If a derived class contains an attribute that cannot be
	 *                cloned.
	 */
	/*
	 * public Object clone(Workspace workspace) throws
	 * CloneNotSupportedException { StringReverse newObject = (StringReverse)
	 * super.clone(workspace);
	 * 
	 * // Set the type constraints.
	 * newObject.input.setTypeEquals(BaseType.STRING);
	 * newObject.output.setTypeEquals(BaseType.STRING);
	 * 
	 * return newObject; }
	 */

	/**
	 * If there is an input string, reverse it and produce that at the output.
	 * If there is no input, do nothing.
	 * 
	 * @exception IllegalActionException
	 *                If the superclass throws it, or if it is thrown reading
	 *                the input port or writing to the output port.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if (input.hasToken(0)) {
			StringToken inputToken = (StringToken) input.get(0);
			String value = inputToken.stringValue();
			String strReverse = (new StringBuffer(value)).reverse().toString();
			output.send(0, new StringToken(strReverse));
		}
	}
}

// vim: sw=4 ts=4 et