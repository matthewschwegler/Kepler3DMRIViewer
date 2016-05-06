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

package org.ecoinformatics.seek.util;

import java.util.Hashtable;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class FieldChooser extends TypedAtomicActor {
	public TypedIOPort input = new TypedIOPort(this, "input", true, false); // the
																			// record
																			// input
	public TypedIOPort inputChoice = new TypedIOPort(this, "inputChoice", true,
			false); // the field in the record to choose
	public TypedIOPort output = new TypedIOPort(this, "output", false, true); // output
																				// the
																				// users
																				// choice

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
	public FieldChooser(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		input.setTypeEquals(BaseType.GENERAL);
		inputChoice.setTypeEquals(BaseType.STRING);
		output.setTypeEquals(BaseType.GENERAL);
	}

	/**
	 * Send a random number with a uniform distribution to the output. This
	 * number is only changed in the prefire() method, so it will remain
	 * constant throughout an iteration.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		int inputChoiceWidth = inputChoice.getWidth();
		if (inputChoiceWidth > 1) {
			throw new IllegalActionException(
					"The inputChoice port can only accept "
							+ "one channel of data.");
		}

		Token inputChoiceToken = inputChoice.get(0);
		String inputChoiceVal = inputChoiceToken.toString();
		// take the quotes off the string
		inputChoiceVal = inputChoiceVal.substring(1,
				inputChoiceVal.length() - 1);

		int width = input.getWidth();
		for (int i = 0; i < width; i++) { // get the records and output the
											// correct field
			if (input.hasToken(i)) {
				Token token = input.get(i);
				String value = token.toString();
				Hashtable fields = new Hashtable();
				value = value.substring(1, value.length() - 1); // chop the
																// braces
				while (value.length() != 0) {
					String val;
					if (value.indexOf(",") == -1) { // at the end of the string
						val = value.substring(0, value.length());
						value = "";
					} else {
						val = value.substring(0, value.indexOf(","));
					}
					String hashname = val.substring(0, val.indexOf("="));
					String hashval = val.substring(val.indexOf("=") + 1, val
							.length());
					fields.put(hashname.trim(), hashval.trim());
					if (val.length() + 1 < value.length()) {
						value = value.substring(val.length() + 1, value
								.length());
					}
				}

				if (!fields.containsKey(inputChoiceVal)) {
					throw new IllegalActionException("No such key '"
							+ inputChoiceVal + "'...fields is "
							+ fields.toString());
				}

				try {
					sendToken(fields.get(inputChoiceVal));
				} catch (ClassCastException cce) {
					throw new IllegalActionException(
							"Error casting the output token '"
									+ fields.get(inputChoiceVal).toString()
									+ "' to the appropriate output port type: "
									+ cce.getMessage());
				}
			}
		}
	}

	/**
	 * sends the token. this can be overwritten to send different types of
	 * tokens besides strings.
	 */
	protected void sendToken(Object o) throws ClassCastException,
			IllegalActionException {
		output.send(0, new StringToken((String) o));
		// getToken will always return a stringToken here.
	}

	/**
   *
   */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}
}