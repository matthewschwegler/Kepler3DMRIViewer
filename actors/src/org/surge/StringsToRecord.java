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

import java.util.HashMap;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// StrigsToRecord class name
/**
 * This actor converts a set of labels and values to a record. Similar to
 * RecordAssembler except that the label names are read from an input port
 * instead of a parameter.
 * 
 * @author Daniel Crawl
 * @version $Id: StringsToRecord.java 24234 2010-05-06 05:21:26Z welker $
 */

public class StringsToRecord extends TypedAtomicActor {

	/**
	 * Construct a StringsToRecord source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public StringsToRecord(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		labels = new TypedIOPort(this, "labels", true, false);
		labels.setTypeEquals(BaseType.STRING);

		values = new TypedIOPort(this, "values", true, false);
		values.setTypeEquals(BaseType.STRING);

		output = new TypedIOPort(this, "output", false, true);
		values.setTypeEquals(BaseType.GENERAL);

		delim = new StringParameter(this, "Field delimeter");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public TypedIOPort labels = null;
	public TypedIOPort values = null;

	public TypedIOPort output = null;

	// the delimeter
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

		String l = ((StringToken) labels.get(0)).stringValue();
		String v = ((StringToken) values.get(0)).stringValue();

		String delimStr = delim.stringValue();

		String[] labelsStr = l.split(delimStr);
		String[] valuesStr = v.split(delimStr);

		// sanity check
		if (labelsStr.length != valuesStr.length) {
			String msg = "number of labels (" + l + ")";
			msg += " and values (" + v + ") do not equal.";
			throw new IllegalActionException(msg);
		}

		HashMap map = new HashMap();
		for (int i = 0; i < labelsStr.length; i++) {
			map.put(labelsStr[i].trim(), new StringToken(valuesStr[i].trim()));
		}

		RecordToken out = new RecordToken(map);
		output.broadcast(out);
	}
}