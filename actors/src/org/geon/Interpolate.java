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

package org.geon;

//tokens
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.numericsolutions.geomodeltools.invdist_power_isosearch2d;

/**
 * A grid interpolation actor. Currently only inverse distance is supported.
 * 
 * @author Efrat Jaeger, John Harris
 * @version $Id: Interpolate.java 24234 2010-05-06 05:21:26Z welker $
 */
public class Interpolate extends TypedAtomicActor {
	public Interpolate(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		input = new TypedIOPort(this, "input", true, false);
		input.setTypeEquals(BaseType.STRING); // Push or Pull
		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);
		interpolationAlg = new StringParameter(this, "interpolationAlg");
		interpolationAlg.setExpression("inverse distance");
		interpolationAlg.addChoice("inverse distance");
		interpolationAlg.addChoice("minimum curvature");
		interpolationAlg.addChoice("nearest neighbor");
		interpolationAlg.addChoice("surface");
		outputFormat = new StringParameter(this, "outputFormat");
		outputFormat.setExpression("ESRI ascii grid");
		outputFormat.addChoice("ESRI ascii grid");
		outFile = new FileParameter(this, "outFile");
		outFile.setDisplayName("output File");
		latMin = new Parameter(this, "latMin");
		latMin.setTypeEquals(BaseType.DOUBLE);
		latMax = new Parameter(this, "latMax");
		latMax.setTypeEquals(BaseType.DOUBLE);
		longMin = new Parameter(this, "longMin");
		longMin.setTypeEquals(BaseType.DOUBLE);
		longMax = new Parameter(this, "longMax");
		longMax.setTypeEquals(BaseType.DOUBLE);
		xSpace = new Parameter(this, "xSpace");
		xSpace.setDisplayName("x grid spacing");
		xSpace.setTypeEquals(BaseType.DOUBLE);
		ySpace = new Parameter(this, "ySpace");
		ySpace.setDisplayName("y grid spacing");
		ySpace.setTypeEquals(BaseType.DOUBLE);
		coefficient = new Parameter(this, "coefficient");
		coefficient.setTypeEquals(BaseType.DOUBLE);
		nullVal = new Parameter(this, "nullVal");
		nullVal.setDisplayName("null Value representation");
		nullVal.setTypeEquals(BaseType.DOUBLE);
		searchRadius = new Parameter(this, "searchRadius");
		searchRadius.setDisplayName("set radius");
		searchRadius.setTypeEquals(BaseType.DOUBLE);

	}

	/** A string representation of the dataset */
	public TypedIOPort input;

	/** A string representation of the gridded output */
	public TypedIOPort output;

	// FIXME: currently supports only IDW.
	/** The selected algorithm */
	public StringParameter interpolationAlg;

	// NOTE: so far only esri ascii grid is supported.
	/** The format of the interpolated result */
	public StringParameter outputFormat;

	public FileParameter outFile;
	/** Minimum latitude */
	public Parameter latMin;

	/** Maximum latitude */
	public Parameter latMax;

	/** Minimum longitude */
	public Parameter longMin;

	/** Maximum longitude */
	public Parameter longMax;

	/** Spacing between the grid cells */
	public Parameter xSpace;

	/** Spacing between the grid cells */
	public Parameter ySpace;

	/** Weight coefficient */
	public Parameter coefficient;

	/** Representation of null values */
	public Parameter nullVal;

	/** The search space */
	public Parameter searchRadius;

	public void initialize() throws IllegalActionException {
	}

	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	public void fire() throws IllegalActionException {
		try {
			// TODO: take care of other algorithm and output format selection.
			float xmin = (float) ((DoubleToken) latMin.getToken())
					.doubleValue();
			float xmax = (float) ((DoubleToken) latMax.getToken())
					.doubleValue();
			float ymin = (float) ((DoubleToken) longMin.getToken())
					.doubleValue();
			float ymax = (float) ((DoubleToken) longMax.getToken())
					.doubleValue();

			float dx = (float) ((DoubleToken) xSpace.getToken()).doubleValue();
			float dy = (float) ((DoubleToken) ySpace.getToken()).doubleValue();
			float nullval = (float) ((DoubleToken) nullVal.getToken())
					.doubleValue();

			float weight = (float) ((DoubleToken) coefficient.getToken())
					.doubleValue();
			float searchradius = (float) ((DoubleToken) searchRadius.getToken())
					.doubleValue();

			String dataFile = ((StringToken) input.get(0)).stringValue();
			dataFile = dataFile.trim();
			String gridFile = ((StringToken) outFile.getToken()).stringValue();

			invdist_power_isosearch2d gridder = new invdist_power_isosearch2d();

			gridder.execute(dataFile, gridFile, xmin, xmax, ymin, ymax, dx, dy,
					nullval, weight, searchradius);

			output.broadcast(new StringToken(gridFile));

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}