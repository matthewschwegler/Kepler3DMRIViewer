/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.garp;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * GARP is a computer program for predicting species locations based on various
 * spatial data sets of environment variables and known species locations. GARP
 * is an acronym for Genetic Algorithm for Rule Set Production. GARP was
 * originally ceated by David Stockwell. The version in Kepler is based on
 * 'Desktop GARP', http://www.lifemapper.org/desktopgarp/. The
 * GarpPresampleLayers actor carries out the first step in a GARP calculation to
 * be executes as a Kepler workflow. It samples the environment data and known
 * locations to create a 'CellSet' of information to be used in later steps. As
 * input, it takes several file names. One is a summary of spatial grids
 * containing things like environmental data (e.g. temperature, precipitation,
 * etc.) A number of these spatial grids are summarized in an xml file (named
 * *.dxl). The second major input is locations file that contains a list of
 * known spatial locations where the species of interest has been found. This
 * information is 'presampled' to create a CellSet to be used in subsequent
 * pieces of the GARP calculation. There is also an input that should give the
 * file name to be used to store this CellSet data. The output port provides the
 * file name when the file has been actually written. (The output is thus a
 * 'trigger'.)
 * 
 * This is a JNI-based actor. It requires the following: linux: libgarp.so
 * windows: garp.dll, libexpat.dll MacOSX - currently not available for the Mac
 * (3/16/2006)
 * 
 * @author Chad Berkeley, Dan Higgins, NCEAS, UC Santa Barbara
 */
public class GarpPresampleLayers extends TypedAtomicActor {
	/**
	 * This parameter is the file name of the xml file that summarizes all the
	 * spatial layers to be used in the GARP calculation. The file name is of
	 * the *.dxl format. Examine samples to see the content.
	 */
	public FileParameter layersetFilenameParameter = new FileParameter(this,
			"layersetFilenameParameter");
	/**
	 * This parameter is the file name of the file that contains the known
	 * locations to be used to determine a ruleset for predicting other
	 * locations from the spatial environment layers. The file is a text file
	 * with one location per line. Each line contains a point location (x,y)
	 * with the numeric values of x and y separated by a 'tab' character.
	 * Typically the (x,y) is (longitude, latitude).
	 */
	public FileParameter dataPointFileNameParameter = new FileParameter(this,
			"dataPointFileNameParameter");
	/**
	 * This parameter is the file name to be used in saving the cell set
	 * information. This information is a sample to be used by GARP in creating
	 * the rule set for predicting occurrences.
	 */
	public FileParameter cellSetFileNameParameter = new FileParameter(this,
			"cellSetFileNameParameter");
	// input ports
	/**
	 * This input port can be used to supply the file name of the xml file that
	 * summarizes all the spatial layers to be used in the GARP calculation. The
	 * file name is of the *.dxl format. Examine samples to see the content.
	 */
	public TypedIOPort layersetFilename = new TypedIOPort(this,
			"layersetFilename", true, false);

	/**
	 * This input port can be used to supply the file name of the file that
	 * contains the known locations to be used to determine a ruleset for
	 * predicting other locations from the spatial environment layers. The file
	 * is a text file with one location per line. Each line contains a point
	 * location (x,y) with the numeric values of x and separated by a 'tab'
	 * character. Typically the (x,y) is (longitude, latitude).
	 */
	public TypedIOPort dataPointFileName = new TypedIOPort(this,
			"dataPointFileName", true, false);

	/**
	 * input port can be used to supply the file name to be used in saving the
	 * cell set information. This information is a sample to be used by GARP in
	 * creating the rule set for predicting occurrences.
	 */
	public TypedIOPort cellSetFileName = new TypedIOPort(this,
			"cellSetFileName", true, false);
	// output ports
	/**
	 * This output port supplies the file name of the cellSetFile. It has the
	 * same name as supplied in the input port, but it is only fired after the
	 * data has been calculated. It is thus used as a 'trigger' for the next
	 * step in the GARP calculation.
	 */
	public TypedIOPort cellSetFileNameOutput = new TypedIOPort(this,
			"cellSetFileNameOutput", false, true);

	/**
	 * Garp presample layer actor
	 */
	public GarpPresampleLayers(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		layersetFilename.setTypeEquals(BaseType.STRING);
		dataPointFileName.setTypeEquals(BaseType.STRING);
		cellSetFileName.setTypeEquals(BaseType.STRING);
		cellSetFileNameOutput.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"66\" height=\"42\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"12\" y=\"16\" "
				+ "style=\"font-size:14; fill:blue; font-family:SansSerif\">"
				+ "GARP</text>\n" + "<text x=\"4\" y=\"34\" "
				+ "style=\"font-size:12; fill:blue; font-family:SansSerif\">"
				+ "Presample</text>\n" + "</svg>\n");

	}

	/**
   *
   */
	public void initialize() throws IllegalActionException {
	}

	/**
   *
   */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	/**
   *
   */
	public void fire() throws IllegalActionException {
		System.out.println("Firing GarpPresampleLayers");
		super.fire();
		String layersetFilenameStr = "";
		String dataPointFileNameStr = "";
		String cellSetFileNameStr = "";
		if (layersetFilename.numberOfSources() > 0) {
			if (!layersetFilename.hasToken(0))
				return;
			StringToken layerSetFileNameToken = (StringToken) layersetFilename
					.get(0);
			layersetFilenameStr = layerSetFileNameToken.stringValue();
			layersetFilenameParameter.setExpression(layersetFilenameStr);
		} else {
			layersetFilenameStr = layersetFilenameParameter.asFile().getPath();
		}

		if (dataPointFileName.numberOfSources() > 0) {
			if (!dataPointFileName.hasToken(0))
				return;
			StringToken dataPointFileNameToken = (StringToken) dataPointFileName
					.get(0);
			dataPointFileNameStr = dataPointFileNameToken.stringValue();
			dataPointFileNameParameter.setExpression(dataPointFileNameStr);
		} else {
			dataPointFileNameStr = dataPointFileNameParameter.asFile()
					.getPath();
		}

		if (cellSetFileName.numberOfSources() > 0) {
			if (!cellSetFileName.hasToken(0))
				return;
			StringToken cellSetFileNameToken = (StringToken) cellSetFileName
					.get(0);
			cellSetFileNameStr = cellSetFileNameToken.stringValue();
			cellSetFileNameParameter.setExpression(cellSetFileNameStr);
		} else {
			cellSetFileNameStr = cellSetFileNameParameter.asFile().getPath();
		}

		System.out.println("Starting GarpPresampleLayers JNI Code");
		new GarpJniGlue().PresampleLayers(layersetFilenameStr,
				dataPointFileNameStr, cellSetFileNameStr);

		cellSetFileNameOutput.broadcast(new StringToken(cellSetFileNameStr));

		System.out.println("Finished with GarpPresampleLayers JNI Code");
	}
}