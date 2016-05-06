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

package org.ecoinformatics.seek.gis.java_gis;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.File;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * <p>
 * This actor creates a spatial raster grid usng the ConvexHull polygon. Raster
 * points within the ConvexHull are set to a value of 1. Points outside have a
 * value of "NO_DATA". The raster is thus a 'mask'.
 * </p>
 * <p>
 *'hullFileName' is a text file with the (x,y) values of the convex hull (one
 * pair per line, space delimited)<br/>
 *'rasterFileName' is the name to be given to the raster output<br/>
 *'numrows' is the number of rows for the raster<br/>
 *'numcols' is the number of columns for the raster<br/>
 *'xllcorner' is the x value for the lower left corner (if empty, set to the
 * minimum x in the convexHull)<br/>
 *'yllcorner' is the x value for the lower left corner (if empty set to the
 * minimum x in the convexHull)<br/>
 *'cellsize' is the (square) cellsize (if empty, 50 cells in x direction are
 * assumed; and nuber of y-cells (ncols) is recalculated to y-extent of
 * convexHull<br/>
 * </p>
 * <p>
 * This is a pure java implementation equivalent to the GISRasterActor based on
 * grass JNI
 * </p>
 * 
 * @author Dan Higgins NCEAS UC Santa Barbara
 */
public class CVHull2Raster extends TypedAtomicActor {
	/**
	 * x-value of the lower left corner of the grid to be created. This is a
	 * double value.
	 */
	public Parameter xllcorner;
	/**
	 * y-value of the lower left corner of the grid to be created. This is a
	 * double value.
	 */
	public Parameter yllcorner;
	/**
	 * Cell size of the grid to be created (assumed square). This is a double
	 * value.
	 */
	public Parameter cellsize;
	/**
	 * Number of rows in the grid to be created. This is an integer
	 */
	public Parameter numrows;
	/**
	 * Number of columns in the grid to be created. This is an integer
	 */
	public Parameter numcols;

	/**
	 * Boolean setting to determine whether or not to use disk for storing grid
	 * data rather than putting all data in RAM arrays. This option in much
	 * slower but allows for very large grids
	 */
	public Parameter useDisk;

	// input ports
	/**
	 * The name of the Convex Hull file of data points. 'hullFileName' is a text
	 * file with the (x,y) values of the convex hull (one pair per line, space
	 * delimited)
	 */
	public TypedIOPort hullFileName = new TypedIOPort(this, "hullFileName",
			true, false);
	/**
	 * The name to be given to the resulting raster grid file
	 */
	public TypedIOPort rasterFileName = new TypedIOPort(this, "rasterFileName",
			true, false);

	/**
	 * The output raster file result (it is in *.asc format).
	 */
	public TypedIOPort rasterFileResult = new TypedIOPort(this,
			"rasterFileResult", false, true);

	private double interior_value = 0.0;
	private double exterior_value = 1.0E120;

	/**
   *
   */
	public CVHull2Raster(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		xllcorner = new Parameter(this, "xllcorner");
		yllcorner = new Parameter(this, "yllcorner");
		cellsize = new Parameter(this, "cellsize");
		numrows = new Parameter(this, "numrows");
		numcols = new Parameter(this, "numcols");

		xllcorner.setExpression("0.0");
		xllcorner.setTypeEquals(BaseType.DOUBLE);
		yllcorner.setExpression("0.0");
		yllcorner.setTypeEquals(BaseType.DOUBLE);
		cellsize.setExpression("0.5");
		cellsize.setTypeEquals(BaseType.DOUBLE);
		numrows.setExpression("50");
		numrows.setTypeEquals(BaseType.INT);
		numcols.setExpression("50");
		numcols.setTypeEquals(BaseType.INT);

		hullFileName.setTypeEquals(BaseType.STRING);
		rasterFileName.setTypeEquals(BaseType.STRING);
		rasterFileResult.setTypeEquals(BaseType.STRING);

		useDisk = new Parameter(this, "useDisk");
		useDisk.setDisplayName("Use disk storage (for large grids)");
		useDisk.setTypeEquals(BaseType.BOOLEAN);
		useDisk.setToken(BooleanToken.TRUE);
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
		super.fire();

		StringToken inputFileToken = (StringToken) hullFileName.get(0);
		String inputFileNameStr = inputFileToken.stringValue();
		File pointFile = new File(inputFileToken.stringValue());
		ConvexHull ch = new ConvexHull(pointFile);
		Shape cvshape = ch.createShape();
		Rectangle2D boundingBox = cvshape.getBounds2D();
		double bbMinX = boundingBox.getMinX();
		double bbMaxX = boundingBox.getMaxX();
		double bbMinY = boundingBox.getMinY();
		double bbMaxY = boundingBox.getMaxY();

		StringToken outputFileToken = (StringToken) rasterFileName.get(0);
		String outFileStr = outputFileToken.stringValue();

		boolean useDiskValue = ((BooleanToken) useDisk.getToken())
				.booleanValue();

		int nrows;
		try {
			nrows = ((IntToken) numrows.getToken()).intValue();
		} catch (Exception w) {
			nrows = 50;
		}

		int ncols;
		try {
			ncols = ((IntToken) numcols.getToken()).intValue();
		} catch (Exception w) {
			ncols = 50;
		}

		double xmin;
		try {
			xmin = ((DoubleToken) xllcorner.getToken()).doubleValue();
		} catch (Exception w) {
			xmin = bbMinX;
		}

		double ymin;
		try {
			ymin = ((DoubleToken) yllcorner.getToken()).doubleValue();
		} catch (Exception w) {
			ymin = bbMinY;
		}

		double cs;
		try {
			cs = ((DoubleToken) cellsize.getToken()).doubleValue();
		} catch (Exception w) {
			cs = (bbMaxX - bbMinX) / ncols;
			// set the cellsize based on x-values; then recalc nrows
			nrows = (int) ((bbMaxY - bbMinY) / cs) + 1;
		}

		Grid grid = new Grid(ncols, nrows, cs, cs, xmin, ymin);
		if (!useDiskValue) {
			grid.inMemFlag = true;
			grid.dataArray = new double[ncols][nrows];
		}
		grid.createShapeMask(cvshape, exterior_value, interior_value);
		grid.createAsc(outFileStr);

		rasterFileResult.broadcast(new StringToken(outFileStr));
	}
}