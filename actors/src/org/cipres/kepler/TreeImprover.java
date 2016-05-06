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
import org.cipres.CipresIDL.api1.DataMatrix;
import org.cipres.CipresIDL.api1.Tree;
import org.cipres.CipresIDL.api1.TreeImprove;
import org.cipres.CipresIDL.api1.TreeScore;
import org.cipres.helpers.CipresRegistry;
import org.cipres.helpers.CipresServiceDialog;
import org.cipres.helpers.RegistryEntryWrapper;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// TreeImprover
/**
 * The TreeImprover actor improves a phylogenetic tree according to the settings
 * configured by the user through the GUIGen interface. This actor uses the
 * TreeImprove CORBA service provided by the CIPRes registry.
 * 
 * @author Zhijie Guan
 * @version $Id: TreeImprover.java 24234 2010-05-06 05:21:26Z welker $
 */

public class TreeImprover extends TypedAtomicActor {

	/**
	 * Construct a TreeImprover actor with the given container and name.
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

	public TreeImprover(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// construct the input port inputMatrix
		inputMatrix = new TypedIOPort(this, "inputMatrix", true, false);
		inputMatrix.setDisplayName("Original Data Matrix");
		inputMatrix.setTypeEquals(BaseType.GENERAL);

		// construct the input port inputTree
		inputTree = new TypedIOPort(this, "inputTree", true, false);
		inputTree.setDisplayName("Original Tree");
		inputTree.setTypeEquals(BaseType.GENERAL);

		// construct the output port outputTree
		outputTree = new TypedIOPort(this, "outputTree", false, true);
		outputTree.setDisplayName("Improved Tree");
		// Set the type constraint.
		outputTree.setTypeEquals(BaseType.GENERAL);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////
	/**
	 * A tree in CIPRes tree data structure is passed to the TreeImprover actor
	 * through this input port. This port is an input port of type GENERAL.
	 */
	public TypedIOPort inputTree = null;

	/**
	 * A matrix containing the characters information of the analyzed taxa is
	 * passed to the TreeImprover actor through this input port. This port is an
	 * input port of type GENERAL.
	 */
	public TypedIOPort inputMatrix = null;

	/**
	 * The improved tree is sent through this output port. This port is an
	 * output port of type GENERAL.
	 */
	public TypedIOPort outputTree = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////
	// Cipres tree data structure
	private org.cipres.CipresIDL.api1.Tree _finalTree = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Improve a tree and send the final tree to the output port.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if ((inputTree.hasToken(0)) && (inputMatrix.hasToken(0))) {

			RegistryEntryWrapper treeImproverWrapper = null;
			try {
				// get the TreeImprove CORBA service
				treeImproverWrapper = CipresRegistry.getCipresServiceWrapper(
						TreeImprove.class, null, null);
				// with GUI version
				CipresServiceDialog dialog = treeImproverWrapper
						.getServiceDialog(null);
				int status = dialog.showAndInitialize();
				if (status == CipresServiceDialog.OK) {
					TreeImprove service = (TreeImprove) treeImproverWrapper
							.getService();
					// set the tree and matrix for the TreeImprove service
					service.setTree((Tree) ((ObjectToken) inputTree.get(0))
							.getValue());
					service.setMatrix((DataMatrix) ((ObjectToken) inputMatrix
							.get(0)).getValue());
					_finalTree = service.improveTree(null);

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

				} else if (status == CipresServiceDialog.ERROR) {
					throw new IllegalActionException(this,
							"error initializing service");
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (treeImproverWrapper != null) {
					// release TreeImprove service
					treeImproverWrapper.releaseService();
				}
			}

			// send out the improved tree
			outputTree.send(0, new ObjectToken(_finalTree));
		}
	}
}