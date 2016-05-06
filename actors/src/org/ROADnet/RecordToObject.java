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

import ptolemy.actor.lib.Transformer;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Ptolemy actor to use a RecordToken to fill in the fields of a newly
 * instantiated Object and then output this object encapsulated within an
 * ObjectToken. This operation is, of course, rather hairy, but it seemed like a
 * logical thing to do after implementing ObjectToRecord. <b>NOT IMPLEMENTED
 * YET.</b>
 * 
 * @author Tobin Fricke, University of California
 * @version $Id: RecordToObject.java 24234 2010-05-06 05:21:26Z welker $
 * @Pt.ProposedRating Red (tobin)
 */

public class RecordToObject extends Transformer {

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

	public RecordToObject(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input.setMultiport(false);
		input.setTypeEquals(BaseType.OBJECT);
		output.setTypeEquals(BaseType.OBJECT);

		classname = new Parameter(this, "orbname");
		classname.setTypeEquals(BaseType.STRING);
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
		/*
		 * Class c = Class.forName(---, false, null); Object o =
		 * c.newInstance();
		 * 
		 * // Now we will make lists of the fields in the class and the //
		 * fields in the record, so that we can verify their bijection.
		 * 
		 * Field[] classFields = c.getFields(); int nClassFields =
		 * Array.getLength(fields);
		 * 
		 * if (nClassFields != nRecordFields) { _debug("Class has " +
		 * nClassFields + " fields while record has " + nRecordFields +
		 * "fields."); }
		 * 
		 * String[] labels = new String[nFields]; ObjectToken[] values = new
		 * ObjectToken[nFields];
		 * 
		 * for (int i = 0; i < nFields; i++) { Field f =
		 * (Field)(Array.get(fields, i)); labels[i] = f.getName();
		 * 
		 * // use class.isPrimitive if necessary }
		 * 
		 * Token r = new RecordToken(labels, values); output.broadcast(r);
		 */
	}

	public Parameter classname;
}
