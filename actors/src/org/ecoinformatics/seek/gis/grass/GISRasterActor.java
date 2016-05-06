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

package org.ecoinformatics.seek.gis.grass;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This actor crease a spatial raster grid usng the ConvexHull polygon. Add
 * raster points within the ConvexHull are set to a value of 1. Points outside
 * have a value of 0. The raster is thus a 'mask'.
 * 
 * 'hullFileName' is a text file with the (x,y) values of the convex hull (one
 * pair per line, space delimited) 'numHullPoint' is the number of Hull points
 * 'rasterFileName' is the name to be given to the raster output 'numRasterRows'
 * is the number of rows for the raster 'numRasterCols' is the number of columns
 * for the raster 'xmin' is the minumum x value (if -1 set to the minimum x in
 * the convexHull) 'xmax' is the minumum x value (if -1 set to the minimum x in
 * the convexHull) 'ymin' is the minumum y value (if -1 set to the minimum y in
 * the convexHull) 'xmax' is the minumum y value (if -1 set to the minimum y in
 * the convexHull)
 * 
 */
public class GISRasterActor extends TypedAtomicActor {
	// input ports
	public TypedIOPort hullFileName = new TypedIOPort(this, "hullFileName",
			true, false);
	public TypedIOPort numHullPoint = new TypedIOPort(this, "numHullPoint",
			true, false);
	public TypedIOPort rasterFileName = new TypedIOPort(this, "rasterFileName",
			true, false);
	public TypedIOPort numRasterRows = new TypedIOPort(this, "numRasterRows",
			true, false);
	public TypedIOPort numRasterCols = new TypedIOPort(this, "numRasterCols",
			true, false);

	public TypedIOPort xmin = new TypedIOPort(this, "xmin", true, false);
	public TypedIOPort ymin = new TypedIOPort(this, "ymin", true, false);
	public TypedIOPort xmax = new TypedIOPort(this, "xmax", true, false);
	public TypedIOPort ymax = new TypedIOPort(this, "ymax", true, false);

	public TypedIOPort rasterFileResult = new TypedIOPort(this,
			"rasterFileResult", false, true);

	/**
   *
   */
	public GISRasterActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		hullFileName.setTypeEquals(BaseType.STRING);
		numHullPoint.setTypeEquals(BaseType.INT);
		rasterFileName.setTypeEquals(BaseType.STRING);
		numRasterRows.setTypeEquals(BaseType.INT);
		numRasterCols.setTypeEquals(BaseType.INT);
		xmin.setTypeEquals(BaseType.DOUBLE);
		ymin.setTypeEquals(BaseType.DOUBLE);
		xmax.setTypeEquals(BaseType.DOUBLE);
		ymax.setTypeEquals(BaseType.DOUBLE);
		rasterFileResult.setTypeEquals(BaseType.STRING);
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
		// System.out.println("firing GISRasterActor");
		super.fire();

		StringToken inputFiletToken = (StringToken) hullFileName.get(0);
		String inputFiletNameStr = inputFiletToken.stringValue();

		IntToken numHullPointToken = (IntToken) numHullPoint.get(0);
		int num_HullPoint = numHullPointToken.intValue();

		StringToken outputFileToken = (StringToken) rasterFileName.get(0);
		String outFileStr = outputFileToken.stringValue();

		IntToken numRasterRowsToken = (IntToken) numRasterRows.get(0);
		int num_RasterRows = numRasterRowsToken.intValue();

		IntToken numRasterColsToken = (IntToken) numRasterCols.get(0);
		int num_RasterCols = numRasterColsToken.intValue();

		double x_min = ((DoubleToken) (xmin.get(0))).doubleValue();
		double y_min = ((DoubleToken) (ymin.get(0))).doubleValue();
		double x_max = ((DoubleToken) (xmax.get(0))).doubleValue();
		double y_max = ((DoubleToken) (ymax.get(0))).doubleValue();

		// System.out.println("Running GISRaster JNI code");
		RasterJniGlue g = new RasterJniGlue();

		g.ReadInput(inputFiletNameStr, num_HullPoint);
		g.SetGridSize(num_RasterRows, num_RasterCols);
		double[] boundary = g.GetBoundary();
		System.out.println(boundary[0] + " " + boundary[1] + " " + boundary[2]
				+ " " + boundary[3]);
		if (x_min == -1)
			x_min = boundary[0];
		if (y_min == -1)
			y_min = boundary[1];
		if (x_max == -1)
			x_max = boundary[2];
		if (y_max == -1)
			y_max = boundary[3];

		g.SetBoundary(x_min, y_min, x_max, y_max);
		g.PerformRasterization();
		g.WriteOutput(outFileStr);

		rasterFileResult.broadcast(new StringToken(outFileStr));
	}
}