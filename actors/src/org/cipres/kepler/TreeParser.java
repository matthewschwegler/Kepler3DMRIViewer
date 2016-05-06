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

// Ptolemy package
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
//// TreeParser
/**
 * The TreeParser actor parses tree data structure into the tree name, tree
 * score, leaf set, and Newick, and sends them to different ouput ports.
 * 
 * @author Zhijie Guan
 * @version $Id: TreeParser.java 24234 2010-05-06 05:21:26Z welker $
 */

public class TreeParser extends TypedAtomicActor {

	/**
	 * Construct a TreeParser actor with the given container and name.
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

	public TreeParser(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// construct the input port inputTree
		inputTree = new TypedIOPort(this, "inputTree", true, false);
		inputTree.setDisplayName("Tree");

		// construct the output ports outputName, outputScore, outputLeafSet,
		// and outputNewick
		outputName = new TypedIOPort(this, "outputName", false, true);
		outputName.setDisplayName("Tree Name");
		outputScore = new TypedIOPort(this, "outputScore", false, true);
		outputScore.setDisplayName("Tree Score");
		outputLeafSet = new TypedIOPort(this, "outputLeafSet", false, true);
		outputLeafSet.setDisplayName("Tree Leaf Set");
		outputNewick = new TypedIOPort(this, "outputNewick", false, true);
		outputNewick.setDisplayName("Tree Expression (Newick)");
		// Set the type constraint.
		outputName.setTypeEquals(BaseType.GENERAL);
		outputScore.setTypeEquals(BaseType.GENERAL);
		outputLeafSet.setTypeEquals(BaseType.GENERAL);
		outputNewick.setTypeEquals(BaseType.GENERAL);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The tree description in CIPRes tree data structure is passed to the
	 * TreeParser actor through this input port. This port is an input port of
	 * type GENERAL.
	 */
	public TypedIOPort inputTree = null;

	/**
	 * The detailed information of a tree is parsed and sent to different output
	 * ports. The tree name is sent to the outputName port. The type of this
	 * port will be set to GENERAL.
	 */
	public TypedIOPort outputName = null;

	/**
	 * The tree score is sent to the outputScore port. The type of this port
	 * will be set to GENERAL.
	 */
	public TypedIOPort outputScore = null;

	/**
	 * The tree's leaf set is sent to the outputLeafSet port. The type of this
	 * port will be set to GENERAL.
	 */
	public TypedIOPort outputLeafSet = null;

	/**
	 * The tree's newick expression is sent to the outputNewick port. The type
	 * of this port will be set to GENERAL.
	 */
	public TypedIOPort outputNewick = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////
	// CIPRes IDL tree data structure
	private org.cipres.CipresIDL.api1.Tree _aTree;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Parse the tree and send various sub data structure into corresponding
	 * ports.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		if (inputTree.hasToken(0)) {
			// get the tree in CIPRes IDL data structure
			_aTree = (Tree) ((ObjectToken) inputTree.get(0)).getValue();
			// send the sub data structure to different ports
			outputName.send(0, new StringToken(_aTree.m_name));
			outputScore.send(0, new StringToken(TreeWrapper
					.scoreToString(_aTree.m_score)));
			String leafSet = "";
			for (int i = 0; i < _aTree.m_leafSet.length; i++) {
				leafSet += _aTree.m_leafSet[i] + " ";
			}
			outputLeafSet.send(0, new StringToken(leafSet));
			outputNewick.send(0, new StringToken(_aTree.m_newick));
		}
	}
}