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
import org.cipres.CipresIDL.api1.TreeDecompose;
import org.cipres.CipresIDL.api1.TreeScore;
import org.cipres.helpers.CipresRegistry;
import org.cipres.helpers.RegistryEntryWrapper;
import org.cipres.util.tree.TreePruner;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// TreeDecomposer
/**
 * The TreeDecomposer actor decomposes a tree into subtrees. This actor uses the
 * TreeDecompose service provided by CIPRes CORBA registry.
 * 
 * @author Zhijie Guan
 * @version $Id: TreeDecomposer.java 24234 2010-05-06 05:21:26Z welker $
 */

public class TreeDecomposer extends TypedAtomicActor {

	/**
	 * Construct a TreeDecomposer actor with the given container and name.
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

	public TreeDecomposer(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// construct the input port inputTree
		inputTree = new TypedIOPort(this, "inputTree", true, false);
		inputTree.setDisplayName("Tree");
		inputTree.setTypeEquals(BaseType.GENERAL);

		// construct the output port outputTrees
		outputTrees = new TypedIOPort(this, "outputTrees", false, true);
		outputTrees.setDisplayName("Decomposed Subtrees");
		// Set the type constraint.
		outputTrees.setTypeEquals(BaseType.GENERAL);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * A tree in CIPRes tree data structure is passed to the TreeDecomposer
	 * actor through this input port. This port is an input port of type
	 * GENERAL.
	 */
	public TypedIOPort inputTree = null;

	/**
	 * The decomposed trees are sent out through this output port. All the trees
	 * are wrapped in a single token to facilitate the transportation. This port
	 * is an output port of type GENERAL.
	 */
	public TypedIOPort outputTrees = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Decompose the input tree and send the subtrees to the output port.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if (inputTree.hasToken(0)) {

			RegistryEntryWrapper treeDecomposerWrapper = null;
			// subtrees after the decomposition
			Tree[] resultTrees = null;
			// tokens to wrap each subtree
			ObjectToken[] treeTokens = null;

			try {
				TreePruner treePruner = new TreePruner(); // tree pruner

				// get the TreeDecompose service wrapper
				treeDecomposerWrapper = CipresRegistry.getCipresServiceWrapper(
						TreeDecompose.class, null, null);
				// get the TreeDecompose service
				TreeDecompose decomposer = (TreeDecompose) treeDecomposerWrapper
						.getService();
				// get the input tree
				Tree wholeTree = (Tree) ((ObjectToken) inputTree.get(0))
						.getValue();
				treePruner.setTree(wholeTree.m_newick); // set the treePruner

				// decompose process
				int[][] decompositions = decomposer.leafSetDecompose(wholeTree);
				resultTrees = new Tree[decompositions.length];
				treeTokens = new ObjectToken[decompositions.length];
				for (int i = 0; i < decompositions.length; i++) {
					// create each subtree based on the leaf set decomposition
					Tree tempTree = new Tree();
					tempTree.m_newick = treePruner.pruneTree(decompositions[i]);
					tempTree.m_leafSet = decompositions[i];

					// fix_tree service, currently it is not available for
					// public access
					// resultTrees[i] = RecIDcm3Impl.fix_tree( tempTree );

					// fix the tempTree. This section should be replaced once
					// the fix_tree is open to public access
					tempTree.m_newick = tempTree.m_newick.trim();
					if (tempTree.m_newick.lastIndexOf(';') == -1) {
						tempTree.m_newick += ";";
					}
					if (tempTree.m_score == null) {
						tempTree.m_score = new TreeScore();
						tempTree.m_score.noScore(0);
					}

					resultTrees[i] = tempTree;
					resultTrees[i].m_name = "subset " + i;

					// wrap the subtree as a token
					treeTokens[i] = new ObjectToken(resultTrees[i]);

					// for debug
					// System.out.println("TreeDecomposer: resultTrees[" + i +
					// "] = " + resultTrees[i].m_newick);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (treeDecomposerWrapper != null) {
					// release the TreeDecompose service
					treeDecomposerWrapper.releaseService();
				}
			}

			outputTrees.send(0, new ObjectToken(resultTrees));
		}
	}
}