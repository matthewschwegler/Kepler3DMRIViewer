/*
 * Copyright (c) 1999-2010 The Regents of the University of California.
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

package org.sdm.spa.actors.piw.viz;

import javax.swing.SwingUtilities;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.IntToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// PIWVisualizationActor
/**
 * This actor is used to visualize data from various sources in SPA's promoter
 * identification workflow. It takes the input given to it from the workflow and
 * passes it to PIWVisualizationFrame, where the real work of creating the
 * visualization is started. In other words, this acts as a liason between the
 * workflow and the code for creating the visualization. Only when all of the
 * information that is needed exists, should the PIWVisualizationFrame be
 * created.
 * 
 * @author Beth Yost
 * @author xiaowen
 * @version $Id: VizActor.java 24234 2010-05-06 05:21:26Z welker $
 */

public class VizActor extends TypedAtomicActor {

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                contained.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public VizActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		// create the port
		String[] arrNames = { "TransfacNames", // names of transcription factor
												// binding sites
				"TransfacSites", // indices corresponding to the names
				"OrigSeq", // original sequence input to BLAST
				"AccessionNumber", // original accession number input by user
				"GeneID", // Gene ID returned by BLAST
				"ClustalWSeq" // sequence returned by ClustalW
		};

		Type[] arrTypes = { new ArrayType(BaseType.STRING),
				new ArrayType(BaseType.INT), BaseType.STRING, BaseType.STRING,
				BaseType.STRING, BaseType.STRING };

		portSequences = new TypedIOPort(this, "Sequences", true, false);
		portSequences.setTypeEquals(new ArrayType(new RecordType(arrNames,
				arrTypes)));

		_createIcon();
	}

	// /////////////////////////////////////////////////////////////////
	// // public variables ////

	public TypedIOPort portSequences;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Also for testing, if called, create the visualization frame then stop
	 * calling this actor This is just set up this way for testing and needs to
	 * be changed.
	 * 
	 * @exception IllegalActionException
	 *                If the parent class throws it.
	 */
	public void fire() throws IllegalActionException {
		// Get the input array token.
		ArrayToken arrSeqs = (ArrayToken) portSequences.get(0);

		// Sequences to be displayed.
		final Sequence[] sequences = new Sequence[arrSeqs.length()];

		// Loop through each element of the array.
		for (int i = 0; i < arrSeqs.length(); i++) {
			// Get the record token.
			RecordToken rec = (RecordToken) arrSeqs.getElement(i);

			// Extract the fields of the record token.
			ArrayToken arrTokTransfacNames = (ArrayToken) rec
					.get("TransfacNames");
			ArrayToken arrTokTransfacSites = (ArrayToken) rec
					.get("TransfacSites");
			StringToken strTokOrigSeq = (StringToken) rec.get("OrigSeq");
			StringToken strTokAccessionNumber = (StringToken) rec
					.get("AccessionNumber");
			StringToken strTokGeneID = (StringToken) rec.get("GeneID");
			StringToken strTokClustalWSeq = (StringToken) rec
					.get("ClustalWSeq");

			// Make sure there are equal numbers of transcription factor names
			// and sites
			if (arrTokTransfacNames.length() != arrTokTransfacSites.length()) {
				throw new IllegalActionException("Error detected by "
						+ this.getFullName() + "\n" + "There were "
						+ arrTokTransfacNames.length()
						+ " transcription factor names, and "
						+ arrTokTransfacSites
						+ " transcription factor sites input"
						+ " for accession number '"
						+ strTokAccessionNumber.stringValue()
						+ "' and gene ID '" + strTokGeneID.stringValue());
			}

			// Get an array of transcription factor binding sites.
			TranscriptionFactorBindingSite[] arrTFBS = new TranscriptionFactorBindingSite[arrTokTransfacNames
					.length()];

			for (int j = 0; j < arrTokTransfacNames.length(); j++) {
				arrTFBS[j] = new TranscriptionFactorBindingSite(
						((StringToken) arrTokTransfacNames.getElement(j))
								.stringValue(), ((IntToken) arrTokTransfacSites
								.getElement(j)).intValue());
			}

			// Create the sequence.
			sequences[i] = new Sequence(strTokAccessionNumber.stringValue(),
					strTokGeneID.stringValue(),
					strTokClustalWSeq.stringValue(), arrTFBS);
		}

		// Put this in the Swing thread in case there are race conditions.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				VizApplication app = new VizApplication(false);
				app.show(sequences);
			}
		});
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/** Create and set the svg icon (a DNA strand) to be displayed in SPA. */
	private void _createIcon() {
		// Creating a green and red double helix.
		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-20\" y=\"-15\" " + "width=\"40\" height=\"30\" "
				+ "style=\"fill:lightGrey\"/>\n" + "<rect x=\"-15\" y=\"-10\" "
				+ "width=\"30\" height=\"20\" " + "style=\"fill:white\"/>\n"

				+ "<line x1=\"-3\" y1=\"6\" x2=\"3\" y2=\"6\" "
				+ "style=\"stroke:green\"/>\n"
				+ "<line x1=\"-4\" y1=\"4\" x2=\"4\" y2=\"4\" "
				+ "style=\"stroke:green\"/>\n"
				+ "<line x1=\"-3\" y1=\"2\" x2=\"3\" y2=\"2\" "
				+ "style=\"stroke:red\"/>\n"

				+ "<line x1=\"-3\" y1=\"-2\" x2=\"3\" y2=\"-2\" "
				+ "style=\"stroke:green\"/>\n"
				+ "<line x1=\"-4\" y1=\"-4\" x2=\"4\" y2=\"-4\" "
				+ "style=\"stroke:green\"/>\n"
				+ "<line x1=\"-3\" y1=\"-6\" x2=\"3\" y2=\"-6\" "
				+ "style=\"stroke:red\"/>\n"

				+ "<line x1=\"-4\" y1=\"-4\" x2=\"4\" y2=\"4\" "
				+ "style=\"stroke:black\"/>\n"
				+ "<line x1=\"-4\" y1=\"4\" x2=\"4\" y2=\"-4\" "
				+ "style=\"stroke:black\"/>\n"

				+ "<line x1=\"-4\" y1=\"4\" x2=\"4\" y2=\"10\" "
				+ "style=\"stroke:black\"/>\n"
				+ "<line x1=\"-4\" y1=\"10\" x2=\"4\" y2=\"4\" "
				+ "style=\"stroke:black\"/>\n"

				+ "<line x1=\"-4\" y1=\"-4\" x2=\"4\" y2=\"-10\" "
				+ "style=\"stroke:black\"/>\n"
				+ "<line x1=\"-4\" y1=\"-10\" x2=\"4\" y2=\"-4\" "
				+ "style=\"stroke:black\"/>\n"

				+ "</svg>\n");

	}
}

// vim: ts=4 sw=4 et