/*
 * Copyright (c) 2010 The Regents of the University of California.
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

/* Input a sequence of N input tokens and then output a trigger
   after the Nth token
 */

package org.ecoinformatics.seek.util;

import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.domains.sdf.lib.SDFTransformer;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// SequenceToTrigger

/**
 * This actor bundles a specified number of input tokens into a single array.
 * The number of tokens to be bundled is specified by the <i>sequenceLength</i>
 * parameter.
 * <p>
 * This actor is polymorphic. It can accept inputs of any type, as long as the
 * type does not change, and will produce an array with elements of the
 * corresponding type.
 * <p>
 */
public class SequenceToTrigger extends SDFTransformer {
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
	public SequenceToTrigger(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input_tokenConsumptionRate.setExpression("sequenceLength");

		// set the output type to be an ArrayType.
		output.setTypeEquals(BaseType.STRING);

		// Set parameters.
		sequenceLength = new PortParameter(this, "sequenceLength");
		sequenceLength.setExpression("1");

		// Set the icon.
		_attachText("_iconDescription", "<svg>\n"
				+ "<polygon points=\"-15,-15 15,15 15,-15 -15,15\" "
				+ "style=\"fill:white\"/>\n" + "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // parameters ////

	/**
	 * The size of the output array. This is an integer that defaults to 1.
	 */
	public PortParameter sequenceLength;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Ensure that the sequenceLength parameter is not negative.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException
	 *                If the parameters are out of range.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == sequenceLength) {
			int rate = ((IntToken) sequenceLength.getToken()).intValue();

			if (rate < 0) {
				throw new IllegalActionException(this,
						"Invalid sequenceLength: " + rate);
			}
		} else {
			super.attributeChanged(attribute);
		}
	}

	/**
	 * Consume the inputs and produce the output ArrayToken.
	 * 
	 * @exception IllegalActionException
	 *                If not enough tokens are available.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		sequenceLength.update();

		int length = ((IntToken) sequenceLength.getToken()).intValue();

		output.send(0, new StringToken("fire"));
	}

	/**
	 * Return true if the input port has enough tokens for this actor to fire.
	 * The number of tokens required is determined by the value of the
	 * <i>arrayLength</i> parameter.
	 * 
	 * @return boolean True if there are enough tokens at the input port for
	 *         this actor to fire.
	 * @exception IllegalActionException
	 *                If the hasToken() query to the input port throws it.
	 * @see ptolemy.actor.IOPort#hasToken(int, int)
	 */
	public boolean prefire() throws IllegalActionException {
		int length = ((IntToken) sequenceLength.getToken()).intValue();

		if (!input.hasToken(0, length)) {
			if (_debugging) {
				_debug("Called prefire(), which returns false.");
			}

			return false;
		} else {
			return super.prefire();
		}
	}

}
