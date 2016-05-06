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
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// Diagrams
/**
 * This is a domain specific actor for processing Rock naming SVG diagrams.
 * Receive all the diagrams information and the transitions between them from
 * its predecessor, the DiagramsTransitions actor and a refernce to this level's
 * diagram and extract the transitions table, the referenced diagram and its
 * coordinates (For now there are only two digitized diagram, so there is no
 * actual transitions table. The actor will be extended once more diagrams are
 * available).
 * 
 * @author Efrat Jaeger
 * @version $Id: Diagrams.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class Diagrams extends TypedAtomicActor {

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
	public Diagrams(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// set the type constraint.
		diagramTransitions = new TypedIOPort(this, "diagramTransitions", true,
				false);
		diagramTransitions.setTypeEquals(BaseType.STRING);
		nextDiagram = new TypedIOPort(this, "nextDiagram", true, false);
		nextDiagram.setTypeEquals(BaseType.INT);
		coordinateNames = new TypedIOPort(this, "coordinateNames", false, true);
		coordinateNames.setTypeEquals(BaseType.INT); // FIX ME: layer for demo
														// purposes!
		transitionTable = new TypedIOPort(this, "transitionTable", false, true);
		transitionTable.setTypeEquals(BaseType.OBJECT);
		diagram = new TypedIOPort(this, "diagram", false, true);
		diagram.setTypeEquals(BaseType.STRING);

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
	public TypedIOPort diagramTransitions;

	/**
	 * A reference to the diagram to be processed.
	 */
	public TypedIOPort nextDiagram;

	/**
	 * The coordinates of the referenced diagram.
	 */
	public TypedIOPort coordinateNames;

	/**
	 * Specifies the transitions between diagrams.
	 */
	public TypedIOPort transitionTable; // FIX ME: needs to be implemented as
										// soon as there are more diagrams.

	/**
	 * An SVG diagram.
	 */
	public TypedIOPort diagram;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Receive diagrams information and a reference to the current diagram.
	 * Output the current diagram, its coordinates names and the transition
	 * table.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		while (true) {

			// get the working directory.
			String _keplerPath = System.getProperty("user.dir");

			// FIX ME: for demo purposes since there are only two diagrams.
			IntToken layer = (IntToken) nextDiagram.get(0);
			coordinateNames.broadcast(layer); // FIX ME: layer would eventually
												// be the coordinates names.
			if (layer.intValue() == 1)
				diagram.broadcast(new StringToken(_keplerPath
						+ "/lib/testdata/geon/QAPF.svg"));
			else if (layer.intValue() == 2)
				diagram.broadcast(new StringToken(_keplerPath
						+ "/lib/testdata/geon/PlagPxOl.svg"));
			transitionTable.broadcast(new ObjectToken("transitions table"));

		}
	}
}