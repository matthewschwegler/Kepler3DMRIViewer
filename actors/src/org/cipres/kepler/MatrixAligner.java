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
import java.io.File;

import org.cipres.CipresIDL.api1.MatrixAlign;
import org.cipres.CipresIDL.api1.Tree;
import org.cipres.datatypes.PhyloDataset;
import org.cipres.guigen.ServiceCommandPanel;
import org.cipres.helpers.CipresRegistry;
import org.cipres.helpers.RegistryEntryWrapper;
import org.cipres.util.Config;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// MatrixAligner
/**
 * This MatrixAligner actor wraps the MatrixAligner CORBA service provided by
 * the CIPRes software package. The MatrixAligner service implements the
 * multipler sequence alignment directed by a guide tree.
 * 
 * @author Zhijie Guan, Alex Borchers
 * @version $Id: MatrixAligner.java 24234 2010-05-06 05:21:26Z welker $
 */

public class MatrixAligner extends TypedAtomicActor {

	/**
	 * Construct a MatrixAligner actor with the given container and name.
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

	public MatrixAligner(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// construct the input port inputDataMatrix
		inputDataMatrix = new TypedIOPort(this, "inputDataMatrix", true, false);
		inputDataMatrix.setDisplayName("Data Matrix");
		inputTree = new TypedIOPort(this, "inputTree", true, false);
		inputTree.setDisplayName("Tree");

		// construct the output port outputTree
		outputTree = new TypedIOPort(this, "outputTree", false, true);
		outputTree.setDisplayName("Tree after RecIDCM3");
		// Set the type constraint.
		outputTree.setTypeEquals(BaseType.GENERAL);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////
	/**
	 * A matrix containing the characters information of the analyzed taxa is
	 * passed to the RecIDCM3 actor through this input port. This port is an
	 * input port of type GENERAL.
	 */
	public TypedIOPort inputDataMatrix = null;

	/**
	 * A tree in CIPRes tree data structure is passed to the RecIDCM3 actor
	 * through this input port. This tree is treated as an initial tree for the
	 * RecIDCM3 algorithm. This port is an input port of type GENERAL.
	 */
	public TypedIOPort inputTree = null;

	/**
	 * The inferred tree is sent through this output port. This port is an
	 * output port of type GENERAL.
	 */
	public TypedIOPort outputTree = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////
	// a tree data structure in CIPRes IDL format

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Improve the tree using RecIDCM3 CORBA service, and send the result tree
	 * to the output port.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		RegistryEntryWrapper matrixAlignWrapper = null;
		try {

			matrixAlignWrapper = CipresRegistry.getCipresServiceWrapper(
					MatrixAlign.class, null, null);
			MatrixAlign service = (MatrixAlign) matrixAlignWrapper.getService();
			org.omg.CORBA.StringHolder reply = new org.omg.CORBA.StringHolder();
			boolean executeSucceeded = false;

			// if args[0]="0" uses guigen
			if (option == 0) {
				File guiXmlFile = new File(Config.getInstance().getXmlGuiDir()
						+ Config.getInstance().getSlash() + "clustalw.xml");
				ServiceCommandPanel pnl = new ServiceCommandPanel(guiXmlFile);

				// TODO implement Cancel functionality
				pnl.setHideCancelButton(true);

				// for deugging/testing - automatically sets infile and outfile
				// params
				// --------------------------------------------------------
				String outFilePath = Config.getInstance().getUniqueTmpFilename(
						"clustalOut_", ".nex");
				String inFilePath = Config.getInstance().getSampleFilesDir()
						+ Config.getInstance().getSlash()
						+ "zfish_nt_12T.fasta";
				pnl.setParameterValue("infile", inFilePath);
				pnl.setParameterValue("outfile", outFilePath);
				// --------------------------------------------------------
				// end for deugging/testing

				pnl
						.setNotifyButtonDisplayMode(ServiceCommandPanel.NOTIFY_BUTTON_DISPLAY_MODE_LAUNCH);
				pnl.showServicePanelAsDialog();

				executeSucceeded = service.execute(pnl.getCmdBlock(), reply);
			}

			if (option == 1) {
				File nexFile = new File(Config.getInstance()
						.getSampleFilesDir()
						+ Config.getInstance().getSlash() + "TestNexusFile.nex");
				PhyloDataset pdIn = new PhyloDataset();
				pdIn.initialize(nexFile);
				service.setTaxa(pdIn.getTaxaInfo());
				service.setInputMatrix(pdIn.getDataMatrix());
				service.setGuideTree(pdIn.getFirstTree());
				executeSucceeded = service.execute("", reply);
				// now get output tree and matrix
				PhyloDataset pdOut = new PhyloDataset();
				pdOut.setDataMatrix(service.getAlignedMatrix());
				pdOut.setTrees(new Tree[] { service.getTree(null) });
				pdOut.setTaxaInfo(service.getTaxa());
				pdOut.writeToNexus(new File(Config.getInstance()
						.getUniqueTmpFilename("ClustalExOut_", ".nex")));
				System.out.println("nexus written");
			}

			if (executeSucceeded) {
				System.out.println("Clustal executed successfully");
			} else {
				System.out.println(reply.value);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (matrixAlignWrapper != null) {
				matrixAlignWrapper.releaseService();
			}
			// System.exit(0);
		}
		/*
		 * if ( (inputTree.hasToken(0)) && (inputDataMatrix.hasToken(0)) ) {
		 * 
		 * RegistryEntryWrapper rid3Wrapper = null; try { // get RecIDCM3
		 * algorithm wrapper rid3Wrapper =
		 * CipresRegistry.getCipresServiceWrapper(Rid3TreeImprove.class, null,
		 * null); // use GuiGen to get users' settings CipresServiceDialog
		 * dialog = rid3Wrapper.getServiceDialog(null); int status =
		 * dialog.showAndInitialize(); if (status == CipresServiceDialog.OK) {
		 * // get the RecIDCM3 algorithm wrapper Rid3TreeImprove service =
		 * (Rid3TreeImprove)rid3Wrapper.getService(); // set the tree and matrix
		 * into the CORBA service service.setTree(
		 * (Tree)((ObjectToken)inputTree.get(0)).getValue() );
		 * service.setMatrix(
		 * (DataMatrix)((ObjectToken)inputDataMatrix.get(0)).getValue() ); //
		 * infer the result tree _finalTree = service.improveTree(null); } else
		 * if (status == CipresServiceDialog.ERROR) { throw new
		 * IllegalActionException(this, "error initializing CIPRES service, "
		 * " service dialog returned error"); }
		 * 
		 * } catch (Exception e) { e.printStackTrace(); } finally { if
		 * (rid3Wrapper != null) { // release the RecIDCM3 service
		 * rid3Wrapper.releaseService(); } }
		 * 
		 * // send out the inferred tree to the output port outputTree.send(0,
		 * new ObjectToken( _finalTree )); }
		 */}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	/*
	 * public boolean postfire() { return false; }
	 */
	private int option = 0;
}