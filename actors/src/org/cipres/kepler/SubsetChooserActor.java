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

import java.io.File;

import org.cipres.CipresIDL.api1.DataMatrix;
import org.cipres.datatypes.PhyloDataset;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////SubsetChooserActor
/**
 * This actor reads the input file, generates the entities list, facilitates the
 * user to choose a subset of entities, and stores the selected entities into an
 * output file.
 * 
 * @author Alex Borchers, Zhijie Guan
 * @version $Id: SubsetChooserActor.java 24234 2010-05-06 05:21:26Z welker $
 */

public class SubsetChooserActor extends TypedAtomicActor {

	/**
	 * Construct SubsetChooserActor source with the given container and name.
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

	public SubsetChooserActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		inputFileName = new TypedIOPort(this, "Subset Chooser Input File",
				true, false);
		inputFileName.setTypeEquals(BaseType.STRING);

		outputFileName = new TypedIOPort(this, "Selected entities file", false,
				true);
		// Set the type constraint.
		outputFileName.setTypeEquals(BaseType.STRING);

		outputFileDefaultName = new Parameter(this,
				"Output File Path and Name", new StringToken(""));

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The input file name is received from this port.
	 */
	public TypedIOPort inputFileName = null;

	/**
	 * The output file name is sent out through this port.
	 */
	public TypedIOPort outputFileName = null;

	/**
	 * The file name parameter, which defines the default output file name.
	 */
	public Parameter outputFileDefaultName;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Choose a subset from the displayed objects.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		if (inputFileName.hasToken(0)) {
			try {

				PhyloDataset pdIn = new PhyloDataset();
				File inputNexusFile = new File(((StringToken) inputFileName
						.get(0)).stringValue());
				pdIn.initialize(inputNexusFile);

				if (pdIn.getTaxaInfo() == null) {
					throw new IllegalActionException(this,
							"There are no taxa to subset");
				}

				DisplayObject[] displayObjects = new DisplayObject[pdIn
						.getTaxaInfo().length];
				for (int i = 0; i < pdIn.getTaxaInfo().length; i++) {
					displayObjects[i] = new DisplayObject(new Integer(i), pdIn
							.getTaxaInfo()[i]);
				}

				SubsetChooser sc = new SubsetChooser(displayObjects);
				DisplayObject[] displayObjectsSubset = sc
						.showSubsetChooserAsDialog();

				// create new PhyloDataset comprised of the subset and write it
				// to nexus file
				PhyloDataset pdOut;
				DataMatrix dmIn = pdIn.getDataMatrix();
				DataMatrix dmOut = new DataMatrix();
				String[] taxa = new String[displayObjectsSubset.length];
				short[][] matrix = new short[displayObjectsSubset.length][pdIn
						.getDataMatrix().m_numCharacters];
				Integer jInt;
				int j = 0;
				for (int i = 0; i < displayObjectsSubset.length; i++) {
					jInt = (Integer) displayObjectsSubset[i].getObject();
					j = jInt.intValue();
					taxa[i] = pdIn.getTaxaInfo()[j];
					for (int k = 0; k < dmIn.m_matrix[0].length; k++) {
						matrix[i][k] = dmIn.m_matrix[j][k];
					}
				}

				dmOut.m_matrix = matrix;
				dmOut.m_symbols = dmIn.m_symbols;
				dmOut.m_charStateLookup = dmIn.m_charStateLookup;
				dmOut.m_datatype = dmIn.m_datatype;
				dmOut.m_numCharacters = dmIn.m_numCharacters;
				dmOut.m_numStates = dmIn.m_numStates;
				dmOut.m_charStateLookup = dmIn.m_charStateLookup;

				pdOut = new PhyloDataset(dmOut, pdIn.getTrees(), taxa);

				String outFileName = ((StringToken) outputFileDefaultName
						.getToken()).stringValue();
				File outputNexusFile = new File(outFileName);

				pdOut.writeToNexus(outputNexusFile);
				outputFileName.send(0, new StringToken(outFileName));
			} catch (Exception e) {
				System.out.println("Exception on Subset Chooser: ");
				e.printStackTrace();
			}
		}
	}

}