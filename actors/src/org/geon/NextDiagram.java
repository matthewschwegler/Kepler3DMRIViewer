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

//tokens
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////NextDiagram
/**
 * This is a domain specific actor used within the GEON mineral classification
 * workflow for choosing the next iteration's diagram. (For now there are only
 * two digitized diagram, so there is no actual transitions table. The actor
 * will be extended once more diagrams are available).
 * 
 * @author Efrat Jaeger
 * @version $Id: NextDiagram.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class NextDiagram extends TypedAtomicActor {
	// input ports
	public TypedIOPort rowInfo = new TypedIOPort(this, "rowInfo", true, false);
	public TypedIOPort transitionTable = new TypedIOPort(this,
			"transitionTable", true, false);
	public TypedIOPort region = new TypedIOPort(this, "region", true, false);
	// output ports
	public TypedIOPort rockName = new TypedIOPort(this, "rockName", false, true);
	public TypedIOPort nextDiagram = new TypedIOPort(this, "nextDiagram",
			false, true);

	public NextDiagram(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		region.setTypeEquals(BaseType.STRING);
		transitionTable.setTypeEquals(BaseType.GENERAL);
		rowInfo.setTypeEquals(BaseType.GENERAL);
		nextDiagram.setTypeEquals(BaseType.INT); // index to the next diagram.
		rockName.setTypeEquals(BaseType.STRING); // index to the next diagram.

		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-30\" y=\"-20\" " + "width=\"60\" height=\"40\" "
				+ "style=\"fill:white\"/>\n" + "<text x=\"-18\" y=\"-5\" "
				+ "style=\"font-size:14\">\n" + "Next \n" + "</text>\n"
				+ "<text x=\"-27\" y=\"13\" " + "style=\"font-size:14\">\n"
				+ "diagram \n" + "</text>\n" + "</svg>\n");
	}

	public void fire() throws IllegalActionException {
		// FIX ME: needs to be updated according to a transition table.
		if (rowInfo.hasToken(0) && region.hasToken(0)
				&& transitionTable.hasToken(0)) {
			StringToken regionToken = (StringToken) region.get(0); // FIX ME!!!
																	// TAKE CARE
																	// OF EMPTY
																	// REGION!!!
			String _region = regionToken.stringValue();
			if (_region.trim().startsWith("diorite")) {
				nextDiagram.broadcast(new IntToken(2));
			} else {
				rockName.broadcast(regionToken);
			}
		}

	}
}