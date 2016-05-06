/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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
import ptolemy.data.StringToken;
import ptolemy.data.expr.Variable;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// StringToPolygon
/**
 * This actor reads a string and outputs an array of coordinates and a string of
 * region. This is a domain specific actor used within the GEON mineral
 * classifier. The string is of the following format:
 * {{{x1,y1},{x2,y2},...},region}.
 * 
 * @author Efrat Jaeger
 * @version $Id: StringToPolygon.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class StringToPolygon extends TypedAtomicActor {

	public TypedIOPort input = new TypedIOPort(this, "input", true, false);
	public TypedIOPort region = new TypedIOPort(this, "region", false, true);
	public TypedIOPort coordinates = new TypedIOPort(this, "coordinates",
			false, true);

	public StringToPolygon(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		input.setTypeEquals(BaseType.STRING);
		coordinates.setTypeEquals(new ArrayType(new ArrayType(BaseType.INT)));
		region.setTypeEquals(BaseType.STRING);

		_expressionEvaluator = new Variable(this, "_expressionEvaluator");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Clone the actor into the specified workspace.
	 * 
	 * @return A new actor.
	 * @exception CloneNotSupportedException
	 *                If a derived class contains an attribute that cannot be
	 *                cloned.
	 */
	/*
	 * public Object clone(Workspace workspace) throws
	 * CloneNotSupportedException { LineReader newObject =
	 * (LineReader)super.clone(workspace); newObject._currentLine = null;
	 * newObject._reachedEOF = false; newObject._reader = null; return
	 * newObject; }
	 */

	/**
	 * Output the data lines into an array.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */

	public void fire() throws IllegalActionException {
		int i = 0;
		int numBrackets = 1;
		inputValue = ((StringToken) input.get(0)).stringValue();
		if (inputValue.charAt(i++) == '{') {
			while (numBrackets != 0) {
				if (inputValue.charAt(i) == '{')
					numBrackets++;
				if (inputValue.charAt(i) == '}')
					numBrackets--;
				i++;
			}
			_expressionEvaluator.setExpression(inputValue.substring(0, i));
			coordinates.broadcast(_expressionEvaluator.getToken());

			region.broadcast(new StringToken(inputValue.substring(i, inputValue
					.length())));

		} else
			throw new IllegalActionException("Illegal String!");

	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */

	public boolean postfire() {
		return true;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	/** Cache of most recently read data. */
	protected String inputValue;

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Variable used to evaluate expressions. */
	private Variable _expressionEvaluator;

}