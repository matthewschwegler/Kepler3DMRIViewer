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
 * <p>
 * GARP is a computer program for predicting species locations based on various
 * spatial data sets of environment variables and known species locations. GARP
 * is an acronym for Genetic Algorithm for Rule Set Production. GARP was
 * originally ceated by David Stockwell. The version in Kepler is based on
 * 'Desktop GARP', http://www.lifemapper.org/desktopgarp/. The GarpPrediction
 * actor predicts presence/absence data on a spatial grid based on the input
 * RuleSet (calculated by the GarpAlgorithm actor) and the input set of
 * environmental layers. The input layers are described in a summary xml file
 * (*.dxl). The outputs are either an *.asc grid file or a *.bmp file. Either
 * can be displayed as a bitmapped image with predicted presence/absence
 * indicated by pixel values (e.g. color mapped when displayed).
 * </p>
 * <p>
 * This is a JNI-based actor. It requires the following: linux: libgarp.so
 * windows: garp.dll, libexpat.dll MacOSX - currently not available for the Mac
 * (3/16/2006)
 * </p>
 * 
 * @author Chad Berkeley, Dan Higgins, NCEAS, UC Santa Barbara
 */
public class GarpPrediction extends TypedAtomicActor {
	// FileParameters
	/**
	 * This is the file name of the file containing the RuleSet data. It is
	 * usually the output of a GarpAlgorithm actor.
	 */
	public FileParameter ruleSetFilenameParameter = new FileParameter(this,
			"ruleSetFilenameParameter");
	/**
	 * This is the file name of the *.dxl file used to summarize the set of
	 * spatial data files with environmental data for each pixel.
	 */
	public FileParameter layersetFilenameParameter = new FileParameter(this,
			"layersetFilenameParameter");
	/**
	 * This is the file name to be used for the output ASCII grid file.
	 */
	public FileParameter outputASCIIParameter = new FileParameter(this,
			"outputASCIIParameter");
	/**
	 * This is the file name to be used for the output BMP raster file.
	 */
	public FileParameter outputBMPParameter = new FileParameter(this,
			"outputBMPParameter");

	// input ports
	/**
	 * This is the file name of the file containing the RuleSet data. It is
	 * usually the output of a GarpAlgorithm actor.
	 */
	public TypedIOPort ruleSetFilename = new TypedIOPort(this,
			"ruleSetFilename", true, false);

	/**
	 * This is the file name of the *.dxl file used to summarize the set of
	 * spatial data files with environmental data for each pixel.
	 */
	public TypedIOPort layersetFilename = new TypedIOPort(this,
			"layersetFilename", true, false);

	/**
	 * This is the file name to be used for the output ASCII grid file.
	 */
	public TypedIOPort outputASCII = new TypedIOPort(this, "outputASCII", true,
			false);

	/**
	 * This is the file name to be used for the output BMP raster file.
	 */
	public TypedIOPort outputBMP = new TypedIOPort(this, "outputBMP", true,
			false);
	// output ports
	/**
	 * This is the file name of the output ASCII grid file. This port fires when
	 * the output predicted distribution grid has been created
	 */
	public TypedIOPort outputASCIIFileName = new TypedIOPort(this,
			"outputASCIIFileName", false, true);
	/**
	 * This is the file name of the output BMP raster file. This port fires when
	 * the output predicted distribution grid has been created. It contains the
	 * same information as the ASCII output port but in a more easily displayed
	 * raster format.
	 */
	public TypedIOPort outputBMPFileName = new TypedIOPort(this,
			"outputBMPFileName", false, true);

	/**
	 * GarpPrediction Actor
	 */
	public GarpPrediction(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		ruleSetFilename.setTypeEquals(BaseType.STRING);
		layersetFilename.setTypeEquals(BaseType.STRING);
		outputASCII.setTypeEquals(BaseType.STRING);
		outputBMP.setTypeEquals(BaseType.STRING);
		outputASCIIFileName.setTypeEquals(BaseType.STRING);
		outputBMPFileName.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"66\" height=\"42\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"12\" y=\"16\" "
				+ "style=\"font-size:14; fill:blue; font-family:SansSerif\">"
				+ "GARP</text>\n" + "<text x=\"4\" y=\"34\" "
				+ "style=\"font-size:12; fill:blue; font-family:SansSerif\">"
				+ "Prediction</text>\n" + "</svg>\n");

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
		System.out.println("firing GarpPrediction");
		super.fire();
		String ruleSetFilenameStr = "";
		String layersetFilenameStr = "";
		String outputASCIIStr = "";
		String outputBMPStr = "";

		if (ruleSetFilename.numberOfSources() > 0) {
			if (!ruleSetFilename.hasToken(0))
				return;
			StringToken ruleSetFilenameToken = (StringToken) ruleSetFilename
					.get(0);
			ruleSetFilenameStr = ruleSetFilenameToken.stringValue();
			ruleSetFilenameParameter.setExpression(ruleSetFilenameStr);
		} else {
			ruleSetFilenameStr = ruleSetFilenameParameter.asFile().getPath();
		}

		if (layersetFilename.numberOfSources() > 0) {
			if (!layersetFilename.hasToken(0))
				return;
			StringToken layersetFilenameToken = (StringToken) layersetFilename
					.get(0);
			layersetFilenameStr = layersetFilenameToken.stringValue();
			layersetFilenameParameter.setExpression(layersetFilenameStr);
		} else {
			layersetFilenameStr = layersetFilenameParameter.asFile().getPath();
		}

		if (outputASCII.numberOfSources() > 0) {
			if (!outputASCII.hasToken(0))
				return;
			StringToken outputASCIIToken = (StringToken) outputASCII.get(0);
			outputASCIIStr = outputASCIIToken.stringValue();
			outputASCIIParameter.setExpression(outputASCIIStr);
		} else {
			outputASCIIStr = outputASCIIParameter.asFile().getPath();
		}

		if (outputBMP.numberOfSources() > 0) {
			if (!outputBMP.hasToken(0))
				return;
			StringToken outputBMPToken = (StringToken) outputBMP.get(0);
			outputBMPStr = outputBMPToken.stringValue();
			outputBMPParameter.setExpression(outputBMPStr);
		} else {
			outputBMPStr = outputBMPParameter.asFile().getPath();
		}

		System.out.println("Starting GarpPrediction JNI Code");
		new GarpJniGlue().DoGarpPrediction(ruleSetFilenameStr,
				layersetFilenameStr, outputASCIIStr, outputBMPStr);

		outputASCIIFileName.broadcast(new StringToken(outputASCIIStr));
		outputBMPFileName.broadcast(new StringToken(outputBMPStr));

		System.out.println("Finished with GarpPrediction JNI Code");
	}
}