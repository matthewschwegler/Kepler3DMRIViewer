/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-09-17 21:38:03 -0700 (Mon, 17 Sep 2012) $' 
 * '$Revision: 30699 $'
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
import ptolemy.data.ArrayToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;


/**
 * This actor reads an array and writes a string with all the elements. The
 * characters separating the entries can be specified as a parameter.
 * 
 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: ArrayToString.java 30699 2012-09-18 04:38:03Z barseghian $
 */
public class ArrayToString extends TypedAtomicActor {

	/**
	 * Construct an ArrayToString with the given container and name.
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
	public ArrayToString(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		array = new TypedIOPort(this, "array", true, false);
		array.setTypeAtLeast(ArrayType.ARRAY_BOTTOM);

		string = new TypedIOPort(this, "string", false, true);
		string.setTypeEquals(BaseType.STRING);

		separator = new Parameter(this, "Element separator",
				new StringToken(""));
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The input port, which contains the array.
	 */
	public TypedIOPort array = null;
	/**
	 * The output port, which contains the string.
	 */
	public TypedIOPort string = null;
	/**
	 * The parameter, which specifies the element separator.
	 */
	public Parameter separator = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Take the array and print out the elements.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		_token = ((ArrayToken) array.get(0)).arrayValue();
		_elements = (_entry.convert(_token[0])).stringValue();
		_spacer = ((StringToken) separator.getToken()).stringValue();
		for (int i = 1; i < _token.length; i++) {
			_elements = _elements + _spacer
					+ (_entry.convert(_token[i])).stringValue();
		}
		string.send(0, new StringToken(_elements));
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private Token[] _token;
	private String _elements;
	private StringToken _entry;
	private String _spacer;
}