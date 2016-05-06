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

import java.io.File;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import util.PersistentVector;

/**
 * <p>
 * <b>Name:</b> MergeGrids.java<br/>
 * </p>
 * <p>
 * <b>Purpose:</b> The purpose of this actor is to 'merge' two ASC grids. The
 * precise meaning of 'merge' will depend on the 'merge' operator. One example
 * is the combination of 2 grids into a new grid whose extent is a rectangle
 * that includes both input bounding box retangles, averageing values from both
 * inputs. Simple math operations (add, subtract) are other examples.<br/>
 * 
 * Order of the input grids may be significant( e.g for subtraction). Extent of
 * the output will always include the combined extent of the inputs, but the
 * cell size will match that of the first grid.
 * </p>
 * 
 * @author : Dan Higgins NCEAS UC Santa Barbara
 * 
 */

public class MergeGrids extends TypedAtomicActor {
	/**
	 * This parameter describes the type of merge to be executed. Choices
	 * include" AVERAGE, ADD, SUBTRACT, MASK, NOT_MASK<br/>
	 * MASK - grid2 missing values will mask correponding points in grid1<br/>
	 * NOT_MASK - grid2 NOT-missing values will mask correponding points in
	 * grid1
	 */
	public StringParameter mergeOperation;
	int mergeOp = 0;
	// input ports
	/**
	 * The first grid file (*.asc format) to be merged
	 */
	public TypedIOPort grid1FileName = new TypedIOPort(this, "grid1FileName",
			true, false);
	/**
	 * The second grid file (*.asc format) to be merged
	 */
	public TypedIOPort grid2FileName = new TypedIOPort(this, "grid2FileName",
			true, false);
	/**
	 * The file name to be given to the result
	 */
	public TypedIOPort mergedGridFileName = new TypedIOPort(this,
			"mergedGridFileName", true, false);

	/**
	 * The resulting merged grid filename.
	 */
	public TypedIOPort mergedGridFileResult = new TypedIOPort(this,
			"mergedGridFileResult", false, true);

	/**
	 * Boolean setting to determine whether or not to use disk for storing grid
	 * data rather than putting all data in RAM arrays
	 */
	public Parameter useDisk;

	private Grid grid1;
	private Grid grid2;
	private Grid mergedGrid;

	private boolean useDiskValue = true;

	private static final int NEAREST_NEIGHBOR = 0;
	private static final int INVERSE_DISTANCE = 1;

	// merge operations
	private static final int AVERAGE = 0;
	private static final int ADD = 1;
	private static final int SUBTRACT = 2;
	private static final int MASK = 3; // grid2 missing values will mask
										// correponding points in grid1
	private static final int NOT_MASK = 4; // grid2 NOT-missing values will mask
											// correponding points in grid1

	public MergeGrids(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		mergeOperation = new StringParameter(this, "mergeOperation");
		mergeOperation.setExpression("Average");
		mergeOperation.addChoice("Average");
		mergeOperation.addChoice("Add");
		mergeOperation.addChoice("Subtract");
		mergeOperation.addChoice("Mask");
		mergeOperation.addChoice("(NOT)Mask");

		grid1FileName.setTypeEquals(BaseType.STRING);
		grid2FileName.setTypeEquals(BaseType.STRING);
		mergedGridFileResult.setTypeEquals(BaseType.STRING);
		mergedGridFileResult.setTypeEquals(BaseType.STRING);

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
		useDiskValue = ((BooleanToken) useDisk.getToken()).booleanValue();

		String temp = mergeOperation.stringValue();
		if (temp.equals("Average")) {
			mergeOp = AVERAGE;
		} else if (temp.equals("Add")) {
			mergeOp = ADD;
		} else if (temp.equals("Subtract")) {
			mergeOp = SUBTRACT;
		} else if (temp.equals("Mask")) {
			mergeOp = MASK;
		} else if (temp.equals("(NOT)Mask")) {
			mergeOp = NOT_MASK;
		}

		StringToken grid1FileToken = (StringToken) grid1FileName.get(0);
		String grid1FileNameStr = grid1FileToken.stringValue();
		StringToken grid2FileToken = (StringToken) grid2FileName.get(0);
		String grid2FileNameStr = grid2FileToken.stringValue();

		File grid1File = new File(grid1FileNameStr);
		File grid2File = new File(grid2FileNameStr);
		grid1 = new Grid(grid1File, !useDiskValue);
		grid2 = new Grid(grid2File, !useDiskValue);

		double minx = grid1.xllcorner;
		if (grid2.xllcorner < minx)
			minx = grid2.xllcorner;
		double miny = grid1.yllcorner;
		if (grid2.yllcorner < miny)
			miny = grid2.yllcorner;
		double maxx = grid1.xllcorner + grid1.ncols * grid1.delx;
		if ((grid2.xllcorner + grid2.ncols * grid2.delx) > maxx)
			maxx = grid2.xllcorner + grid2.ncols * grid2.delx;
		double maxy = grid1.yllcorner + grid1.nrows * grid1.dely;
		if ((grid2.yllcorner + grid2.nrows * grid2.dely) > maxy)
			maxy = grid2.yllcorner + grid2.nrows * grid2.dely;
		;
		double new_cs = grid1.delx; // remember, delx and dely are equal!
		int new_ncols = (int) ((maxx - minx) / new_cs);
		int new_nrows = (int) ((maxy - miny) / new_cs);

		mergedGrid = new Grid(new_ncols, new_nrows, new_cs, new_cs, minx, miny);
		// new merged grid has now been created but no storage or values alloted
		merge(NEAREST_NEIGHBOR, mergeOp);

		StringToken outputFileToken = (StringToken) mergedGridFileName.get(0);
		String outFileStr = outputFileToken.stringValue();
		mergedGrid.createAsc(outFileStr);
		mergedGridFileResult.broadcast(new StringToken(outFileStr));
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 * 
	 *	 */
	public boolean postfire() throws IllegalActionException {
		grid1.delete(); // remove potentially large data storage associated with
						// grid1
		grid2.delete(); // remove potentially large data storage associated with
						// grid2
		mergedGrid.delete(); // remove potentially large data storage associated
								// with mergedGrid
		return super.postfire();
	}

	private void merge(int scalingAlgorithm, int mergeOperation) {
		int nr = mergedGrid.nrows;
		int nc = mergedGrid.ncols;
		double ymin = mergedGrid.yllcorner;
		double xmin = mergedGrid.xllcorner;
		double dx = mergedGrid.delx;
		double dy = mergedGrid.dely;
		if (!useDiskValue) {
			double[][] newDataArray = new double[nc][nr];
			mergedGrid.dataArray = newDataArray;
			for (int j = 0; j < nr; j++) {
				double yloc = ymin + nr * dy - j * dy;
				for (int i = 0; i < nc; i++) {
					double xloc = xmin + i * dx;
					double val1 = grid1.interpValue(xloc, yloc,
							scalingAlgorithm);
					double val2 = grid2.interpValue(xloc, yloc,
							scalingAlgorithm);
					double val = getMergedValue(val1, val2, mergeOperation);
					newDataArray[i][j] = val;
				}
			}
		} else { // using PersistentVector for data storage
			mergedGrid.pv = new PersistentVector();
			mergedGrid.pv.setFirstRow(6);
			mergedGrid.pv.setFieldDelimiter("#x20");
			String[] rowvals = new String[nc];
			for (int j = 0; j < nr; j++) {
				double yloc = ymin + nr * dy - j * dy;
				for (int i = 0; i < nc; i++) {
					double xloc = xmin + i * dx;
					double val1 = grid1.interpValue(xloc, yloc,
							scalingAlgorithm);
					double val2 = grid2.interpValue(xloc, yloc,
							scalingAlgorithm);
					double val = getMergedValue(val1, val2, mergeOperation);

					String valStr;
					if (val > 1.0E100) {
						valStr = Grid.NODATA_value_String;
					} else {
						valStr = (new Double(val)).toString();
					}

					rowvals[i] = valStr;
				}
				mergedGrid.pv.addElement(rowvals);
				rowvals = new String[nc]; // needed to make sure new object
											// added to pv
			}
		}
	}

	private double getMergedValue(double val1, double val2, int mergeOperation) {

		if (mergeOperation == ADD) {
			// if either grid point is a missing value, return a missing value
			if ((val1 > 1.0e100) || (val2 > 1.0e100)) {
				return 1.0e101;
			} else {
				return (val1 + val2);
			}
		} else if (mergeOperation == SUBTRACT) {
			// if either grid point is a missing value, return a missing value
			if ((val1 > 1.0e100) || (val2 > 1.0e100)) {
				return 1.0e101;
			} else {
				return (val1 - val2);
			}
		} else if (mergeOperation == MASK) {
			if (val2 > 1.0e100) {
				return 1.0e101;
			} else {
				return val1;
			}
		} else if (mergeOperation == NOT_MASK) {
			if (val2 < 1.0e100) {
				return 1.0e101;
			} else {
				return val1;
			}
		} else { // 'AVERAGE' operation
			if ((val1 > 1.0e100) && (val2 > 1.0e100)) {
				return 1.0e101;
			} else if (val1 > 1.0e100) {
				return val2;
			} else if (val2 > 1.0e100) {
				return val1;
			} else {
				return ((val1 + val2) / 2.0);
			}
		}
	}
}