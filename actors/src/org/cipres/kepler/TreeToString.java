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

package org.cipres.kepler;

// ptolemy package
import org.cipres.CipresIDL.api1.Tree;
import org.cipres.datatypes.TreeWrapper;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// TreeToString
/**
 * The TreeToString actor gets the whole description of a tree (including tree
 * name, tree score, leaf set, and Newick) and transforms it into a single
 * string.
 * 
 * @author Zhijie Guan
 * @version $Id: TreeToString.java 24234 2010-05-06 05:21:26Z welker $
 */

public class TreeToString extends TypedAtomicActor {

	/**
	 * Construct a TreeToString actor with the given container and name.
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

	public TreeToString(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// construct input port inputTree
		inputTree = new TypedIOPort(this, "inputTree", true, false);
		inputTree.setDisplayName("Tree");
		inputTree.setTypeEquals(BaseType.GENERAL);

		// construct output put outputString
		outputString = new TypedIOPort(this, "outputString", false, true);
		outputString.setDisplayName("Tree Expressed in a String");
		// Set the type constraint.
		outputString.setTypeEquals(BaseType.GENERAL);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The whole tree expression to pass to the TreeToString actor. This
	 * expression may include the tree name, tree score, leaf set, and/or
	 * Newick. This port is an input port of type GENERAL.
	 */
	public TypedIOPort inputTree = null;

	/**
	 * The single tree string expression to represent a tree. This port is an
	 * output port of type GENERAL.
	 */
	public TypedIOPort outputString = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Send the single string description of a tree to the output port.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		if (inputTree.hasToken(0)) {
			// translate the tree expression using the TreeWrapper class
			outputString.send(0, new StringToken(TreeWrapper
					.asString((Tree) ((ObjectToken) inputTree.get(0))
							.getValue())));
		}

	}
}