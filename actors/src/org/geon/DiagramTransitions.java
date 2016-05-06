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

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// DiagramTransitions
/**
 * This is a domain specific actor that holds all the information about rock
 * naming diagrams and the transtions between them. It transfers the data along
 * with initial information to a process that loops over the rock data. (As
 * currently there are only two digitized diagram, there is no actual
 * transitions table yet. The actor will be extended once more diagrams are
 * available).
 * 
 * @author Efrat Jaeger
 * @version $Id: DiagramTransitions.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class DiagramTransitions extends TypedAtomicActor {

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
	public DiagramTransitions(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		diagramsAndTransitions = new TypedIOPort(this,
				"diagramsAndTransitions", false, true);
		diagramsAndTransitions.setTypeEquals(BaseType.STRING);
		index = new TypedIOPort(this, "index", false, true);
		index.setTypeEquals(BaseType.INT);

		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
				+ "style=\"fill:yellow\"/>\n"
				+ "<polygon points=\"-15,-2 0,-15 15,-2 11,15 -11,15\" "
				+ "style=\"fill:white\"/>\n" + "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * All the diagrams and transitions information.
	 */
	public TypedIOPort diagramsAndTransitions;

	/**
	 * A reference to the initial diagram.
	 */
	public TypedIOPort index;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Provide the diagrams and transitions between them along with a refernce
	 * to first diagram.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {

		// FIX ME: need to implement the transitions table as soon as we have
		// more diagrams digitized.
		diagramsAndTransitions.broadcast(new StringToken(
				"Diagrams and Transitions"));
		index.broadcast(new IntToken(1));
	}

	/**
	 * Return false to indicate that the process has finished.
	 * 
	 * @exception IllegalActionException
	 *                If thrown by the super class.
	 */
	public boolean postfire() throws IllegalActionException {
		return false;
	}
}