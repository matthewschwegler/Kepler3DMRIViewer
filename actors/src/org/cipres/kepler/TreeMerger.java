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
import org.cipres.CipresIDL.api1.TreeMerge;
import org.cipres.CipresIDL.api1.TreeScore;
import org.cipres.helpers.CipresRegistry;
import org.cipres.helpers.RegistryEntryWrapper;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// TreeMerger
/**
 * The TreeMerger actor merges a set of trees got from the input port and sends
 * the whole tree to the output port.This actor uses the "tree merge" services
 * provided by the CIPRes CORBA registry.
 * 
 * @author Zhijie Guan
 * @version $Id: TreeMerger.java 24234 2010-05-06 05:21:26Z welker $
 */

public class TreeMerger extends TypedAtomicActor {

	/**
	 * Construct a TreeMerger actor with the given container and name.
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

	public TreeMerger(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// construct the input port inputTrees
		inputTrees = new TypedIOPort(this, "inputTrees", true, false);
		inputTrees.setDisplayName("Trees");
		inputTrees.setTypeEquals(BaseType.GENERAL);

		// construct the output port outputTree
		outputTree = new TypedIOPort(this, "outputTree", false, true);
		outputTree.setDisplayName("Merged Tree");
		// Set the type constraint.
		outputTree.setTypeEquals(BaseType.GENERAL);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////
	/**
	 * A set of trees in CIPRes tree data structure is passed to the TreeMerger
	 * actor through this input port. This port is an input port of type
	 * GENERAL.
	 */
	public TypedIOPort inputTrees = null;

	/**
	 * The merged whole tree is sent through this output port. This port is an
	 * output port of type GENERAL.
	 */
	public TypedIOPort outputTree = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////
	private org.cipres.CipresIDL.api1.Tree _finalTree = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Merge a set of trees and send the whole tree to the output port.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if (inputTrees.hasToken(0)) {

			RegistryEntryWrapper treeMergerWrapper = null;
			try {
				// get the TreeMerge CORBA service from CIPRes registry
				treeMergerWrapper = CipresRegistry.getCipresServiceWrapper(
						TreeMerge.class, null, null);
				TreeMerge service = (TreeMerge) treeMergerWrapper.getService();

				// merge trees using TreeMerge service
				_finalTree = service
						.mergeTrees((Tree[]) (((ObjectToken) inputTrees.get(0))
								.getValue()));

				// fix _finalTree. This section should be replaced once the
				// fix_tree is open to public access
				_finalTree.m_newick = _finalTree.m_newick.trim();
				if (_finalTree.m_newick.lastIndexOf(';') == -1) {
					_finalTree.m_newick += ";";
				}
				if (_finalTree.m_score == null) {
					_finalTree.m_score = new TreeScore();
					_finalTree.m_score.noScore(0);
				}

				// This is the code to fix the whole tree with the fix_tree
				// service.
				// Unfortunately this fix_tree service is not available for
				// public access yet.
				// finalTree = RecIDcm3Impl.fix_tree( service.mergeTrees(
				// (Tree[])((ObjectToken)inputTrees.get(0)).getValue() ) );

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (treeMergerWrapper != null) {
					// release the TreeMerge service
					treeMergerWrapper.releaseService();
				}
			}

			// send out the whole tree to the ouput tree port
			outputTree.send(0, new ObjectToken(_finalTree));
		}
	}
}