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
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// BooleanAccumulator

/**
 * This actor reads a sequence of boolean values and writes one boolean value
 * from their combination.
 * 
 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: BooleanAccumulator.java 24234 2010-05-06 05:21:26Z welker $
 */
public class BooleanAccumulator extends TypedAtomicActor {

	/**
	 * Construct a BooleanAccumulator with the given container and name.
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
	public BooleanAccumulator(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		booleans = new TypedIOPort(this, "booleans", true, false);
		booleans.setTypeEquals(BaseType.BOOLEAN);

		combination = new TypedIOPort(this, "combination", false, true);
		combination.setTypeEquals(BaseType.BOOLEAN);

		operation = new StringParameter(this, "Logical operator");
		operation.addChoice("and");
		operation.addChoice("or");
		operation.setExpression("and");

		number = new PortParameter(this, "number");
		number.setExpression("1");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The input port, which contains the booleans.
	 */
	public TypedIOPort booleans = null;
	/**
	 * The output port, which contains their combination.
	 */
	public TypedIOPort combination = null;
	/**
	 * The parameter, which specifies the logical operator.
	 */
	public StringParameter operation = null;
	/**
	 * The port parameter, which specifies the number of booleans in the
	 * sequence and defaults to 1.
	 */
	public PortParameter number = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Check the validity of the <i>number</i> parameter value, set the
	 * consumption rate of the input port, and, if necessary, invalidate the
	 * current schedule of the director.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException
	 *                If the parameter is out of range.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == number) {
			_length = ((IntToken) number.getToken()).intValue();
			if (_length < 0) {
				throw new IllegalActionException(this, "Invalid number: "
						+ _length);
			}
		} else {
			super.attributeChanged(attribute);
		}
	}

	/**
	 * Take the booleans and combine them.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director or if the operation is wrong.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		number.update();
		_length = ((IntToken) number.getToken()).intValue();
		_array = booleans.get(0, _length);
		_logic = operation.stringValue();
		_sum = (BooleanToken) _array[0];
		if (_logic.equalsIgnoreCase("and")) {
			for (int i = 1; i < _length; i++) {
				_sum = _sum.and((BooleanToken) _array[i]);
			}
		} else if (_logic.equalsIgnoreCase("or")) {
			for (int i = 1; i < _length; i++) {
				_sum = _sum.or((BooleanToken) _array[i]);
			}
		} else {
			_sum = null;
			throw new IllegalActionException(this, "Invalid operation: "
					+ _logic);
		}
		combination.send(0, _sum);
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return false;
	}

	/**
	 * Prefire the actor. Return true if the input port has enough tokens for
	 * this actor to fire. The number of tokens required is determined by the
	 * value of the <i>number</i> parameter.
	 * 
	 * @return boolean True if there are enough tokens at the input port for
	 *         this actor to fire.
	 * @exception IllegalActionException
	 *                If the hasToken() query to the input port throws it.
	 */
	public boolean prefire() throws IllegalActionException {
		_length = ((IntToken) number.getToken()).intValue();
		if (!booleans.hasToken(0, _length)) {
			if (_debugging) {
				_debug("Called prefire(), which returns false.");
			}
			return false;
		} else {
			return super.prefire();
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private int _length;
	private Token[] _array;
	private String _logic;
	private BooleanToken _sum;
}