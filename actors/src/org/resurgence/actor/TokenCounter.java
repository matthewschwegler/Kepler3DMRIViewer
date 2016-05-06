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
import ptolemy.data.IntToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// TokenCounter

/**
 * This actor consumes and counts all tokens from the input port and prints out
 * their overall number. The relations connected to the input port are scanned
 * one after the other.
 * 
 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: TokenCounter.java 24234 2010-05-06 05:21:26Z welker $
 */
public class TokenCounter extends TypedAtomicActor {

	/**
	 * Construct a TokenCounter with the given container and name.
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
	public TokenCounter(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		token = new TypedIOPort(this, "token", true, false);
		token.setTypeEquals(BaseType.UNKNOWN);
		token.setMultiport(true);

		number = new TypedIOPort(this, "number", false, true);
		number.setTypeEquals(BaseType.INT);
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The input port, which contains all tokens.
	 */
	public TypedIOPort token = null;
	/**
	 * The output port, which contains the number of tokens.
	 */
	public TypedIOPort number = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Consume and count all tokens and print out their number.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		_counter = 0;
		// Consume and count the input tokens.
		for (int i = 0; i < token.getWidth(); i++) {
			// FIXME: This does not work for the PN Director.
			while (token.hasToken(i)) {
				token.get(i);
				_counter++;
			}
		}
		// Print out the number of tokens.
		number.send(0, new IntToken(_counter));
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return false;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private int _counter;
}