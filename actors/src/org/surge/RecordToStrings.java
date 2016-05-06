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

package org.surge;

import java.util.Iterator;
import java.util.Set;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import util.StringUtil;

//////////////////////////////////////////////////////////////////////////
//// RecordToStrings class name
/**
 * This actor converts a record into a string of labels and a string of values.
 * Similar to RecordDisassembler except that the label names are output as a
 * string instead of a parameter.
 * 
 * @author Daniel Crawl
 * @version $Id: RecordToStrings.java 24234 2010-05-06 05:21:26Z welker $
 */

public class RecordToStrings extends TypedAtomicActor {

	/**
	 * Construct a RecordToStrings source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public RecordToStrings(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input = new TypedIOPort(this, "input", true, false);

		labels = new TypedIOPort(this, "labels", false, true);
		labels.setTypeEquals(BaseType.STRING);

		values = new TypedIOPort(this, "values", false, true);
		values.setTypeEquals(BaseType.STRING);

		delim = new StringParameter(this, "Field separator");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public TypedIOPort input = null;

	public TypedIOPort labels = null;
	public TypedIOPort values = null;

	public StringParameter delim = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Send the token in the value parameter to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		String delimStr = delim.stringValue();

		for (int i = 0; i < input.getWidth(); i++) {
			RecordToken rec = (RecordToken) input.get(i);
			Set ls = rec.labelSet();
			Iterator iter = ls.iterator();

			String[] labelsStr = new String[ls.size()];
			String[] valsStr = new String[ls.size()];
			int j = 0;

			while (iter.hasNext()) {
				labelsStr[j] = (String) iter.next();
				valsStr[j] = ((StringToken) rec.get(labelsStr[j]))
						.stringValue();
				j++;
			}

			labels.broadcast(new StringToken(StringUtil.join(labelsStr,
					delimStr)));
			values
					.broadcast(new StringToken(StringUtil.join(valsStr,
							delimStr)));
		}
	}

	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		System.out.println("attr changed: " + attribute);

		super.attributeChanged(attribute);
	}
}