/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: brooks $'
 * '$Date: 2010-06-10 13:14:30 -0700 (Thu, 10 Jun 2010) $' 
 * '$Revision: 24798 $'
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
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// StringSplitter

/**
 * Read a string and write an array filled with the parts of the string. The
 * regular expression separating the entries can be specified as a parameter.
 * 
 * <p>Note that this class uses java.util.String.split(), which means that
 * trailing empty strings are not included in the results.</p>

 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: StringSplitter.java 24798 2010-06-10 20:14:30Z brooks $
 */
public class StringSplitter extends TypedAtomicActor {

        // FIXME: we need a StringTokenizer actor

	/**
	 * Construct a StringSplitter with the given container and name.
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
	public StringSplitter(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		string = new TypedIOPort(this, "string", true, false);
		string.setTypeEquals(BaseType.STRING);

		array = new TypedIOPort(this, "array", false, true);
		array.setTypeEquals(new ArrayType(BaseType.STRING));

		separator = new StringParameter(this, "Regular expression");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The input port, which contains the string.
	 */
	public TypedIOPort string = null;

	/**
	 * The output port, which contains the array.
	 */
	public TypedIOPort array = null;

        // FIXME: this should be a PortParameter.
	/**
	 * The parameter, which specifies the regular expression.
	 */
	public StringParameter separator = null;

	///////////////////////////////////////////////////////////////////
	////               public methods                              ////

	/**
	 * Take the string and split it into an array.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		_text = string.get(0);
		_spacer = separator.stringValue();
		_parts = (((StringToken) _text).stringValue()).split(_spacer);
		_size = _parts.length;
		_tokens = new StringToken[_size];
		for (int i = 0; i < _size; i++) {
			_tokens[i] = new StringToken(_parts[i]);
		}
		_all = new ArrayToken(_tokens);
		array.send(0, _all);
	}

	///////////////////////////////////////////////////////////////////
	////                private members                            ////

	private Token _text;
	private String _spacer;
	private String[] _parts;
	private int _size;
	private StringToken[] _tokens;
	private ArrayToken _all;
}