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
import org.cipres.datatypes.PhyloDataset;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// PhyloDataReader
/**
 * The PhyloDataReader actor reads and parses the data stored in Nexus format.
 * This actor uses the data structure defined in the CIPRes software package.
 * 
 * @author Zhijie Guan
 * @version $Id: PhyloDataReader.java 24234 2010-05-06 05:21:26Z welker $
 */

public class PhyloDataReader extends TypedAtomicActor {

	/**
	 * Construct a PhyloDataReader actor with the given container and name.
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

	public PhyloDataReader(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// construct the input port inputNexusData
		inputNexusData = new TypedIOPort(this, "inputNexusData", true, false);
		inputNexusData.setDisplayName("Nexus File Content");
		inputNexusData.setTypeEquals(BaseType.GENERAL);

		// construct the output port outputDataMatrix
		outputDataMatrix = new TypedIOPort(this, "outputDataMatrix", false,
				true);
		outputDataMatrix.setDisplayName("Data Matrix");
		// Set the type constraint.
		outputDataMatrix.setTypeEquals(BaseType.GENERAL);

		// construct the output port outputTree
		outputTree = new TypedIOPort(this, "outputTree", false, true);
		outputTree.setDisplayName("Tree");
		// Set the type constraint.
		outputTree.setTypeEquals(BaseType.GENERAL);

		// construct the output port outputTaxaInfo
		outputTaxaInfo = new TypedIOPort(this, "outputTaxaInfo", false, true);
		outputTaxaInfo.setDisplayName("Taxa Info");
		// Set the type constraint.
		outputTaxaInfo.setTypeEquals(BaseType.GENERAL);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The phylogenetic data in Nexus format is passed to the PhyloDataReader
	 * actor through this input port. The data could contain information about
	 * the phylogenetic tree, data matrix, and/or Taxa. Generally this data is
	 * read from a Nexus input file, or retrieved from a phylogenetic info
	 * database, like TreeBase. This port is an input port of type GENERAL.
	 */
	public TypedIOPort inputNexusData = null;

	/**
	 * The tree information contained in the input data is parsed and sent out
	 * through this output port. This port is an output port of type GENERAL.
	 */
	public TypedIOPort outputTree = null;

	/**
	 * The data matrix information contained in the input data is parsed and
	 * sent out through this output port. This port is an output port of type
	 * GENERAL.
	 */
	public TypedIOPort outputDataMatrix = null;

	/**
	 * The taxa information contained in the input data is parsed and sent out
	 * through this output port. This port is an output port of type GENERAL.
	 */
	public TypedIOPort outputTaxaInfo = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////
	// tree structure
	private org.cipres.CipresIDL.api1.Tree _tree = null;
	// data matrix
	private org.cipres.CipresIDL.api1.DataMatrix _dataMatrix = null;
	// taxa info
	private String[] _taxaInfo = null;
	// phylodataset used to store and parse the input data
	private org.cipres.datatypes.PhyloDataset _phyloDataset;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Read and parse the Nexus data.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		if (inputNexusData.hasToken(0)) {

			try {
				_phyloDataset = new PhyloDataset();
				// initialize the phylodataset with the input data
				_phyloDataset.initialize(((StringToken) inputNexusData.get(0))
						.stringValue());

				// parse the phylodataset to get the tree, data matrix, and taxa
				// info
				_tree = _phyloDataset.getFirstTree();
				_dataMatrix = _phyloDataset.getDataMatrix();
				_taxaInfo = _phyloDataset.getTaxaInfo();
			} catch (Exception ex) {
				System.out.println("Exception on read the phylo data set: ");
				ex.printStackTrace();
			}

			// send out the object tokens
			ObjectToken treeToken = new ObjectToken(_tree);
			outputTree.send(0, treeToken);

			ObjectToken dataMatrixToken = new ObjectToken(_dataMatrix);
			outputDataMatrix.send(0, dataMatrixToken);

			ObjectToken taxaInfoToken = new ObjectToken(_taxaInfo);
			outputTaxaInfo.send(0, taxaInfoToken);
		}

	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return false;
	}
}