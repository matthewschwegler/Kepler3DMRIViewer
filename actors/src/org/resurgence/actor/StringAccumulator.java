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

package org.resurgence.actor;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// StringAccumulator

/**
 * This actor reads several strings and writes them into one string. The
 * characters separating the entries can be specified as parameter.
 * 
 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: StringAccumulator.java 24234 2010-05-06 05:21:26Z welker $
 */
public class StringAccumulator extends TypedAtomicActor {

	/**
	 * Construct a StringAccumulator with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public StringAccumulator(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		parts = new TypedIOPort(this, "parts", true, false);
		parts.setTypeEquals(BaseType.STRING);
		parts.setMultiport(true);

		whole = new TypedIOPort(this, "whole", false, true);
		whole.setTypeEquals(BaseType.STRING);

		separator = new Parameter(this, "Substring separator", new StringToken(
				""));
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The input port, which contains the substrings.
	 */
	public TypedIOPort parts = null;
	/**
	 * The output port, which contains the full string.
	 */
	public TypedIOPort whole = null;
	/**
	 * The parameter, which specifies the substring separator.
	 */
	public Parameter separator = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Take the partial strings and print out the whole string.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		_string = "";
		_spacer = ((StringToken) separator.getToken()).stringValue();
		for (int i = 0; i < parts.getWidth(); i++) {
			if (parts.hasToken(i)) {
				if (_string.length() == 0) {
					_string = ((StringToken) parts.get(i)).stringValue();
				} else {
					_string = _string + _spacer
							+ ((StringToken) parts.get(i)).stringValue();
				}
			}
		}
		whole.send(0, new StringToken(_string));
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private String _string;
	private String _spacer;
}