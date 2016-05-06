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

package org.kepler.actor.io;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;

/**
 * This actor will replicate a token for each time fire() is called. this is
 * useful if the director is iterating and you want the same token to be sent
 * over a port for each iteration.
 */
public class ParameterSetter extends TypedAtomicActor {
	Token t = null;
	TypedIOPort input;
	TypedIOPort output;
	CompositeEntity container;
	StringAttribute paramName;

	/**
	 * const.
	 */
	public ParameterSetter() {
		super();
		init();
	}

	/**
	 * const.
	 */
	public ParameterSetter(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		init();
	}

	/**
	 * const
	 */
	public ParameterSetter(Workspace w) {
		super(w);
		init();
	}

	/**
	 * init the ports
	 */
	private void init() {
		try {
			input = new TypedIOPort(this, "Input", true, false);
			output = new TypedIOPort(this, "Output", false, true);
			container = (CompositeEntity) getContainer();
			paramName = new StringAttribute(this, "Parameter Name");
		} catch (Exception e) {
			input = null;
			output = null;
			System.out.println("Error adding ports to TokenIterator: "
					+ e.getMessage());
		}
	}

	/**
	 * fire
	 */
	public void fire() throws IllegalActionException {
		// System.out.println("firing");
		if (t == null) {
			// System.out.println("getting token from input");
			t = input.get(0);
			String paramNameStr = paramName.getExpression();
			if (paramNameStr == null || paramNameStr.equals("")) {
				throw new IllegalActionException(
						"You must set the parameter name in "
								+ "the config for ParameterSetter.");
			}
			Parameter p = (Parameter) container.getAttribute(paramNameStr);
			if (p == null) {
				throw new IllegalActionException("The parameter "
						+ paramNameStr
						+ " does not exist.  Please create it and try again.");
			}
			p.setToken(t);
		}
		// System.out.println("sending token to output");
		output.send(0, t);
	}

}