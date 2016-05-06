/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package util;

import java.util.Vector;

import ptolemy.actor.lib.Transformer;
import ptolemy.data.ArrayToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// ConcatenateArrays

/**
 * <p>
 * On each firing, this actor reads exactly one token from each channel of the
 * input port and assembles the tokens into an ArrayToken. The ArrayToken is
 * sent to the output port. If there is no input token at any channel of the
 * input port, the prefire() will return false.
 * </p>
 * <p>
 * Derived from the ElementToArray actor. it is assumed that all inputs are
 * arrays of the same type of elementary data.
 * </p>
 * 
 * @author Dan Higgins
 * @version $Id: ConcatenateArrays.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 4.0
 */
public class ConcatenateArrays extends Transformer {
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
	public ConcatenateArrays(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// set the output type to be an ArrayType.
		// output.setTypeAtLeast(ArrayType.arrayOf(input));
		// Set Type Constraints.
		input.setTypeAtLeast(ArrayType.ARRAY_BOTTOM);
		output.setTypeAtLeast(input);
		// FIXME: correct type constraint for length
		output.setTypeAtLeast(ArrayType.ARRAY_UNSIZED_BOTTOM);

		input.setMultiport(true);

		// Set the icon.
		_attachText("_iconDescription", "<svg>\n"
				+ "<polygon points=\"-15,-15 15,15 15,-15 -15,15\" "
				+ "style=\"fill:white\"/>\n" + "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Override the base class to set type constraints.
	 * 
	 * @param workspace
	 *            The workspace for the new object.
	 * @return A new instance of ArrayElement.
	 * @exception CloneNotSupportedException
	 *                If a derived class contains an attribute that cannot be
	 *                cloned.
	 */
	public Object clone(Workspace workspace) throws CloneNotSupportedException {
		ConcatenateArrays newObject = (ConcatenateArrays) super
				.clone(workspace);
		(newObject.output).setTypeAtLeast(ArrayType.ARRAY_UNSIZED_BOTTOM);
		return newObject;
	}

	/**
	 * Consume one token from each channel of the input port, assemble those
	 * tokens into an ArrayToken, and send the result to the output.
	 * 
	 * @exception IllegalActionException
	 *                If not enough tokens are available.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		int size = input.getWidth();
		Type type = input.getType();

		// if ((ArrayType.class).isInstance(type)) {
		// System.out.println("Input is an array!");
		// }
		// for now, simply assume inputs are arrays
		Vector v = new Vector();
		//Type datatype = null;
		for (int i = 0; i < size; i++) {
			ArrayToken at = (ArrayToken) input.get(i);
			// The type is not used anywhere but inquiring the type from the first token
			//  does not allow that array to be empty. So I commented out. Norbert Podhorszki
			//if (i == 0) {
			//	datatype = at.getElement(0).getType();
			//}
			for (int j = 0; j < at.length(); j++) {
				v.addElement(at.getElement(j));
			}
		}
		int vecLength = v.size();
		Token[] ta = new Token[vecLength];
		for (int k = 0; k < vecLength; k++) {
			ta[k] = (Token) v.elementAt(k);
		}
		if (vecLength > 0)
			output.send(0, new ArrayToken(ta));
		else // send empty array 
			output.send(0, new ArrayToken(BaseType.NIL)); 

	}

	/**
	 * Return true if all channels of the <i>input</i> port have tokens, false
	 * if any channel does not have a token.
	 * 
	 * @return boolean True if all channels of the input port have tokens.
	 * @exception IllegalActionException
	 *                If the hasToken() query to the input port throws it.
	 * @see ptolemy.actor.IOPort#hasToken(int)
	 */
	public boolean prefire() throws IllegalActionException {
		for (int i = 0; i < input.getWidth(); i++) {
			if (!input.hasToken(i)) {
				return false;
			}
		}

		return true;
	}
}