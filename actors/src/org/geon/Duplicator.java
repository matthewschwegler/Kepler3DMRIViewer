/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
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

package org.geon;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// Duplicator
/**
 * This actor takes as inputs a token and an integer number n and duplicates the
 * token n times.
 * 
 * @author Efrat Jaeger
 * @version $Id: Duplicator.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 4.0.1
 */

public class Duplicator extends TypedAtomicActor {

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
	public Duplicator(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input = new TypedIOPort(this, "input", true, false);

		numCopies = new TypedIOPort(this, "numCopies", true, false);
		numCopies.setTypeEquals(BaseType.INT);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeAtLeast(ArrayType.arrayOf(input));
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** Input token. */
	public TypedIOPort input;

	/**
	 * Number of copies.
	 */
	public TypedIOPort numCopies;

	/**
	 * Output token.
	 */
	public TypedIOPort output;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * broadcast the input token numCopies times.
	 */
	public void fire() throws IllegalActionException {
		int _numCopies = ((IntToken) numCopies.get(0)).intValue();
		Token copies[] = new Token[_numCopies];
		Token value = input.get(0);

		for (int i = 0; i < _numCopies; i++) {
			copies[i] = value;
		}
		output.send(0, new ArrayToken(copies));
	}
}