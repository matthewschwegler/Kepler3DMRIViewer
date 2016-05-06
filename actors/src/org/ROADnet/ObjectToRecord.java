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

package org.ROADnet;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import ptolemy.actor.lib.Transformer;
import ptolemy.data.ObjectToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Ptolemy actor to produce from an ObjectToken a RecordToken, with record
 * entries corresponding to fields within the object carried by the ObjectToken.
 * This actor was originally written to eliminate the need for special
 * field-accessor actors for each class transported with ObjectToken; it seems
 * like a logical enough thing to do, as an Object is pretty much a record where
 * names can map to other Objects (fields) or to function closures (methods).
 * This actor ignores methods completely.
 * 
 * <p>
 * <img src="http://mercali.ucsd.edu/~tobin/OrbObjectToRecordDemo.png"/>
 * </p>
 * 
 * @author Tobin Fricke (tobin@splorg.org), University of California
 * @version $Id: ObjectToRecord.java 24234 2010-05-06 05:21:26Z welker $
 * @see RecordToObject
 * @Pt.ProposedRating Red (tobin)
 */

public class ObjectToRecord extends Transformer {

	/**
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

	public ObjectToRecord(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input.setTypeEquals(BaseType.OBJECT);
		// output.setTypeAtLeast(new RecordType(new String[0], new Type[0])); //
		// FIXME
	}

	public boolean prefire() throws IllegalActionException {
		return (input.hasToken(0) && super.prefire());
	}

	/**
	 * Accept an ObjectToken from the input and produce a corresponding
	 * RecordToken on the output.
	 */

	public void fire() throws IllegalActionException {
		super.fire();

		Object o = ((ObjectToken) (input.get(0))).getValue();
		Class c = o.getClass();
		Field[] fields = c.getFields();
		int nFields = Array.getLength(fields);

		String[] labels = new String[nFields];
		ObjectToken[] values = new ObjectToken[nFields];

		for (int i = 0; i < nFields; i++) {
			Field f = (Field) (Array.get(fields, i));
			labels[i] = f.getName();
			try {
				/*
				 * Here there is room to do something smart, like convert
				 * java.lang.String objects into StringTokens.
				 */
				values[i] = new ObjectToken(f.get(o));
			} catch (IllegalAccessException e) {
				values[i] = new ObjectToken(null); // not the best approach
			}
		}

		/*
		 * It might be useful to either include the classname as a field in the
		 * record (problematic due to possible name collission), as a separate
		 * token, or even in a higher level of recordToken wrapping.
		 */

		Token r = new RecordToken(labels, values);
		output.broadcast(r);
	}

}
