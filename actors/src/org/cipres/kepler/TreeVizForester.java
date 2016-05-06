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

// Ptolemy packages
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import forester.atv.ATVjframe;

//////////////////////////////////////////////////////////////////////////
//// TreeVizForester
/**
 * Given a tree expression (in newick format), the TreeVizForester actor
 * displays the phylogenetic tree in the Forester tree view window.
 * 
 * @author Zhijie Guan
 * @version $Id: TreeVizForester.java 24234 2010-05-06 05:21:26Z welker $
 */

public class TreeVizForester extends TypedAtomicActor {

	/**
	 * Construct a TreeVizForester actor with the given container and name.
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

	public TreeVizForester(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// construct the input port inputTreeString
		inputTreeString = new TypedIOPort(this, "inputTreeString", true, false);
		inputTreeString.setDisplayName("Tree Expression (Newick)");
		// inputTreeString.setDisplayName("Tree Expression (Newick)");
		inputTreeString.setTypeEquals(BaseType.GENERAL);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * Tree expression to pass to the Forester tree display package. This port
	 * is an input port of type GENERAL.
	 */
	public TypedIOPort inputTreeString = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Display the phylogenetic tree ported in from the input port with the
	 * Forester package.
	 * 
	 * @exception IllegalActionException
	 *                if it is thrown by the super.fire() method.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		if (inputTreeString.hasToken(0)) {
			// get the tree expression
			String treeDescription = ((StringToken) inputTreeString.get(0))
					.stringValue();
			try {
				// create the tree in Forester
				forester.tree.Tree displayedTree = new forester.tree.Tree(
						treeDescription);
				// create the Forester tree view window
				ATVjframe atvframe = new ATVjframe(displayedTree);
				// atvframe.setTitle(treeDescription);
				// display the tree
				atvframe.showWhole();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}