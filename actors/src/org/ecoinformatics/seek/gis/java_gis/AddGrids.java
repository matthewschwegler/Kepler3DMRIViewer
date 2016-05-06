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
import java.util.Vector;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import util.PersistentVector;

/**
 * <b>Name:</b> AddGrids.java<br/>
 * <b>Purpose:</b> The purpose of this actor is to 'merge' multiple ASC grids.
 * This differs from MergeGrid in that more than 2 grids can be merged.
 * <p>
 * Extent and cell size will match that of the first grid. The primary purpose
 * is to combine stochastic grids to give a spatial distribution where more
 * probable cells have larger values. Thus, cell values are added for all pixels
 * in the input grid file list.
 * </p>
 * 
 * @author : Dan Higgins NCEAS UC Santa Barbara
 * 
 */

public class AddGrids extends TypedAtomicActor {
	int mergeOp = 1; // ADD
	// input ports
	/**
	 * A string array of filenames of grid files to be added
	 */
	public TypedIOPort gridFilenameArrayPort = new TypedIOPort(this,
			"gridFilenameArrayPort", true, false);

	/**
	 * The name to be given to the resulting output file
	 */
	public TypedIOPort mergedGridFileName = new TypedIOPort(this,
			"mergedGridFileName", true, false);

	/**
	 * The file name of the resulting grid (acts as a trigger when addition is
	 * complete)
	 */
	public TypedIOPort mergedGridFileResult = new TypedIOPort(this,
			"mergedGridFileResult", false, true);

	private Grid grid1;
	private Grid grid2;
	private Grid mergedGrid;

	private static final int NEAREST_NEIGHBOR = 0;
	private static final int INVERSE_DISTANCE = 1;

	private Vector gridsVector;

	public AddGrids(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		gridFilenameArrayPort.setTypeEquals(new ArrayType(BaseType.STRING));
		mergedGridFileName.setTypeEquals(BaseType.STRING);
		mergedGridFileResult.setTypeEquals(BaseType.STRING);

		gridsVector = new Vector();
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

		gridsVector = new Vector();
		if (gridFilenameArrayPort.getWidth() > 0) {
			ArrayToken token = (ArrayToken) gridFilenameArrayPort.get(0);
			// now iterate over all the filenames in the array
			for (int i = 0; i < token.length(); i++) {
				StringToken s_token = (StringToken) token.getElement(i);
				String ascfilename = s_token.stringValue();
				gridsVector.addElement(ascfilename);
			}
		}
		System.out.println("Array size: " + gridsVector.size());

		// assume at least 2 grids in the Vector
		File grid1File = new File((String) gridsVector.elementAt(0));
		File grid2File = new File((String) gridsVector.elementAt(1));
		grid1 = new Grid(grid1File);
		grid2 = new Grid(grid2File);

		double minx = grid1.xllcorner;
		double miny = grid1.yllcorner;
		double maxx = grid1.xllcorner + grid1.ncols * grid1.delx;
		double maxy = grid1.yllcorner + grid1.nrows * grid1.dely;
		double new_cs = grid1.delx; // remember, delx and dely are equal!
		int new_ncols = (int) ((maxx - minx) / new_cs);
		int new_nrows = (int) ((maxy - miny) / new_cs);

		mergedGrid = new Grid(new_ncols, new_nrows, new_cs, new_cs, minx, miny);
		// new merged grid has now been created but no storage or values alloted
		merge(NEAREST_NEIGHBOR, mergeOp);
		// we have now added the first two grids
		grid1.delete(); // get rid of unneeded data
		grid2.delete();
		for (int k = 2; k < gridsVector.size(); k++) {
			grid1 = mergedGrid;
			mergedGrid = new Grid(new_ncols, new_nrows, new_cs, new_cs, minx,
					miny);
			grid2File = new File((String) gridsVector.elementAt(k));
			grid2 = new Grid(grid2File);
			merge(NEAREST_NEIGHBOR, mergeOp);
			grid1.delete(); // get rid of unneeded data
			grid2.delete();
		}

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
		if (mergedGrid.inMemFlag && (mergedGrid.dataArray != null)) {
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

		// if either grid point is a missing value, return a missing value
		if ((val1 > 1.0e100) || (val2 > 1.0e100)) {
			return 1.0e101;
		} else {
			return (val1 + val2);
		}
	}
}