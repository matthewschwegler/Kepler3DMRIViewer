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

/**
 *         Name: Grid.java
 *      Purpose: Used to represent georeferenced grids of values; includes
 *               methods for manipulating those grids. especially regridding.
 *               The emphasis is on equally spaced grids (like those described in
 *               *.ASC files).
 *
 *      Author : Dan Higgins
 *
 */

package org.ecoinformatics.seek.gis.java_gis;

import java.awt.Shape;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.StringTokenizer;

import util.PersistentVector;

public class Grid {

	public int nrows;
	public int ncols;

	public double xllcorner;
	public double yllcorner;
	public double delx;
	public double dely;
	// ASC files have square cells; i.e. a cellsize = delx = dely
	// we make it slightly more general here
	public static String NODATA_value_String = "-9999";

	public boolean inMemFlag = false;

	// assume a cell is defined by row and column index of upper left corner
	// and there is a numeric value associated with each cell

	public double[][] dataArray = null;
	public PersistentVector pv = null;

	// last row string array;used to speed up access when PersistenVector is
	// used
	private String[] rowSA = null;
	// last row index
	private int last_y = -1;
	// next to last row string array;used to speed up access when
	// PersistenVector is used
	private String[] next2lastrowSA = null;
	// next to last row index
	private int next2last_y = -1;

	private String cachedLine = "";
	private Hashtable header = null;

	private double no_val_threshold = 1.0E100;

	static final int NEAREST_NEIGHBOR = 0;
	static final int INVERSE_DISTANCE = 1;

	public Grid() {
		// just generate a simple grid
		nrows = 10;
		ncols = 10;
		delx = 1.0;
		dely = 1.0;
		xllcorner = 0.0;
		yllcorner = 0.0;
		// note that no data storage for grid values has been created
	}

	public Grid(int nx, int ny, double dx, double dy, double xmin, double ymin) {
		this.ncols = nx;
		this.nrows = ny;
		this.delx = dx;
		this.dely = dy;
		this.xllcorner = xmin;
		this.yllcorner = ymin;
		// note that no data storage for grid values has been created
	}

	// create a Grid from an .ASC file
	public Grid(File ascfile) {
		FileReader inReader = null;
		BufferedReader bufReader = null;
		try {
			inReader = new FileReader(ascfile);
			bufReader = new BufferedReader(inReader);
			header = getHeaderInformation(bufReader);
		} catch (Exception ee) {
			System.out.println("Exception at main!");
		}
		// first non-header line should be in cachedLine string
		// and header values should be in header hash
		// test
		// System.out.println("header hash: "+header);
		if (header.containsKey("NODATA_value")) {
			NODATA_value_String = (String) header.get("NODATA_value");
		}
		nrows = (new Integer((String) header.get("nrows"))).intValue();
		ncols = (new Integer((String) header.get("ncols"))).intValue();
		delx = (new Double((String) header.get("cellsize"))).doubleValue();
		dely = (new Double((String) header.get("cellsize"))).doubleValue();
		xllcorner = (new Double((String) header.get("xllcorner")))
				.doubleValue();
		yllcorner = (new Double((String) header.get("yllcorner")))
				.doubleValue();

		if (inMemFlag) {
			// now create the actual data array
			dataArray = new double[ncols][nrows];
			int i = 0;
			int j = 0;
			while (cachedLine != null) {
				StringTokenizer st = new StringTokenizer(cachedLine);
				i = 0;
				while (st.hasMoreTokens()) {
					String nextToken = st.nextToken().trim();
					if (!nextToken.equals(NODATA_value_String)) {
						double val = Double.parseDouble(nextToken);
						dataArray[i][j] = val;
					} else {
						// missing value
						dataArray[i][j] = 1.0E101;
					}
					i++;
				}
				try {
					cachedLine = bufReader.readLine();
					j++;
				} catch (Exception eee) {
					cachedLine = null;
				}
			}
		} else {

			// an ObjectFile version

			pv = new PersistentVector();
			pv.setFirstRow(6);
			pv.setFieldDelimiter("#x20");
			System.out.println("filename: " + ascfile.getPath());
			pv.init(ascfile.getPath());
		}
		try {
			bufReader.close();
		} catch (Exception w) {
			System.out.println("Error in creating grid from asc file!");
		}

	}

	// used to create a Grid from ASC file and determine whether to save it in
	// memory (or use disk)
	public Grid(File ascfile, boolean inMemory) {
		inMemFlag = inMemory;
		FileReader inReader = null;
		BufferedReader bufReader = null;
		try {
			inReader = new FileReader(ascfile);
			bufReader = new BufferedReader(inReader);
			header = getHeaderInformation(bufReader);
		} catch (Exception ee) {
			System.out.println("Exception at main!");
		}
		// first non-header line should be in cachedLine string
		// and header values should be in header hash
		// test
		// System.out.println("header hash: "+header);
		if (header.containsKey("NODATA_value")) {
			NODATA_value_String = (String) header.get("NODATA_value");
		}
		nrows = (new Integer((String) header.get("nrows"))).intValue();
		ncols = (new Integer((String) header.get("ncols"))).intValue();
		delx = (new Double((String) header.get("cellsize"))).doubleValue();
		dely = (new Double((String) header.get("cellsize"))).doubleValue();
		xllcorner = (new Double((String) header.get("xllcorner")))
				.doubleValue();
		yllcorner = (new Double((String) header.get("yllcorner")))
				.doubleValue();

		if (inMemFlag) {
			// now create the actual data array
			dataArray = new double[ncols][nrows];
			int i = 0;
			int j = 0;
			while (cachedLine != null) {
				StringTokenizer st = new StringTokenizer(cachedLine);
				i = 0;
				while (st.hasMoreTokens()) {
					String nextToken = st.nextToken().trim();
					if (!nextToken.equals(NODATA_value_String)) {
						double val = Double.parseDouble(nextToken);
						dataArray[i][j] = val;
					} else {
						// missing value
						dataArray[i][j] = 1.0E101;
					}
					i++;
				}
				try {
					cachedLine = bufReader.readLine();
					j++;
				} catch (Exception eee) {
					cachedLine = null;
				}
			}
		} else {

			// an ObjectFile version

			pv = new PersistentVector();
			pv.setFirstRow(6);
			pv.setFieldDelimiter("#x20");
			System.out.println("filename: " + ascfile.getPath());
			pv.init(ascfile.getPath());
		}
		try {
			bufReader.close();
		} catch (Exception w) {
			System.out.println("Error in creating grid from asc file!");
		}
	}

	// Note that the data is indexed from the top left, not the lower left
	public double getValue(int x, int y) {
		String val;
		double ret = 1.0E101; // assume any number > than no_val_threshold is
								// missing data
		if ((x < 0) || (x > ncols - 1))
			return ret; // outside grid
		if ((y < 0) || (y > nrows - 1))
			return ret; // outside grid
		// assume for now that the array is in memory
		if (dataArray != null) {
			ret = dataArray[x][y];
		}
		if (pv != null) {
			if ((y != next2last_y) && (y != last_y)) {
				next2last_y = last_y;
				next2lastrowSA = rowSA;
				rowSA = (String[]) (pv.elementAt(y));
				last_y = y;
			}
			if (y == last_y) {
				val = rowSA[x];
			} else {
				val = next2lastrowSA[x];
			}
			if (val.equals(NODATA_value_String)) {
				ret = 1.0E101;
			} else {
				ret = new Double(val).doubleValue();
			}
		}
		return ret;
	}

	// Note that the data is indexed from the top left, not the lower left
	// but the location is described from the lowerleft corner (as doubles)
	public double interpValue(double x, double y, int scalingAlgorithm) {
		x = x + .01 * delx; // slight shift to avoid round off problems - DFH
		y = y - .01 * dely; // slight shift to avoid round off problems - DFH
		if (scalingAlgorithm == INVERSE_DISTANCE) {
			return interpValue_IDW(x, y);
		}
		double ret = 1.0E101;
		int xint = (int) ((x - xllcorner) / delx);
		int yint = nrows - 1 - (int) ((y - yllcorner) / dely);
		if (yint == -1)
			yint = 0;
		ret = getValue(xint, yint);
		return ret;
	}

	public double interpValue_IDW(double x, double y) {
		int delij = 1;
		int deliij = 1;
		int delijj = 1;
		int deliijj = 1;
		double ret = 1.0E101;
		int xint = (int) ((x - xllcorner) / delx);
		int yint = nrows - 1 - (int) ((y - yllcorner) / dely);
		if (yint == -1)
			yint = 0;
		if (xint >= ncols - 1)
			xint = ncols - 2;
		if (yint >= nrows - 1)
			yint = nrows - 2;
		double valij = getValue(xint, yint);
		double valiij = getValue(xint + 1, yint);
		double valijj = getValue(xint, yint + 1);
		double valiijj = getValue(xint + 1, yint + 1);
		if ((valij >= no_val_threshold) && (valiij >= no_val_threshold)
				&& (valijj >= no_val_threshold)
				&& (valiijj >= no_val_threshold)) {
			return ret;
		}
		// no_values are often set at boundaries; i.e. oceans
		// in order to interpolate near these boundaries, the values
		// of cells with no data must be ignored. The checks below implement
		// this.
		if (valij >= no_val_threshold)
			delij = 0;
		if (valiij >= no_val_threshold)
			deliij = 0;
		if (valijj >= no_val_threshold)
			delijj = 0;
		if (valiijj >= no_val_threshold)
			deliijj = 0;

		// calculate distances to enclosing grid points.
		// if the distance is very small, just return the value at the corner
		// since
		// it will dominate anyway
		double dist2center = (delx * delx + dely * dely) / 4.0;
		double distij2 = ((x - xllcorner) % delx) * ((x - xllcorner) % delx)
				+ ((dely - (y - yllcorner) % dely))
				* ((dely - (y - yllcorner) % dely));
		if (distij2 < 1E-99)
			return valij;
		if ((distij2 < dist2center) && (delij == 0))
			return valij;
		double distiij2 = ((delx - (x - xllcorner) % delx))
				* ((delx - (x - xllcorner) % delx))
				+ ((dely - (y - yllcorner) % dely))
				* ((dely - (y - yllcorner) % dely));
		if (distiij2 < 1E-99)
			return valiij;
		if ((distiij2 < dist2center) && (deliij == 0))
			return valiij;
		double distijj2 = ((x - xllcorner) % delx) * ((x - xllcorner) % delx)
				+ ((y - yllcorner) % dely) * ((y - yllcorner) % dely);
		if (distijj2 < 1E-99)
			return valijj;
		if ((distijj2 < dist2center) && (delijj == 0))
			return valijj;
		double distiijj2 = ((delx - (x - xllcorner) % delx))
				* ((delx - (x - xllcorner) % delx)) + ((y - yllcorner) % dely)
				* ((y - yllcorner) % dely);
		if (distiijj2 < 1E-99)
			return valiijj;
		if ((distiijj2 < dist2center) && (deliijj == 0))
			return valiijj;

		double weightij = delij / distij2;
		double weightiij = deliij / distiij2;
		double weightijj = delijj / distijj2;
		double weightiijj = deliijj / distiijj2;
		double sumw = weightij + weightiij + weightijj + weightiijj;

		ret = (valij * weightij + valiij * weightiij + valijj * weightijj + valiijj
				* weightiijj)
				/ sumw;

		return ret;
	}

	public void createAsc(String filename) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileOutputStream(filename));
			out.print("nrows  " + nrows);
			out.println();
			out.print("ncols  " + ncols);
			out.println();
			out.print("xllcorner  " + xllcorner);
			out.println();
			out.print("yllcorner  " + yllcorner);
			out.println();
			out.print("cellsize  " + delx);
			out.println();
			out.print("NODATA_value  " + NODATA_value_String);
			out.println();
			if (dataArray != null) {
				for (int r = 0; r < nrows; r++) {
					for (int c = 0; c < ncols; c++) {
						double val = dataArray[c][r];
						if (val > no_val_threshold) {
							out.print(NODATA_value_String + " ");
						} else {
							out.print(val + " ");
						}
					}
					out.println();
				}
			}
			if (pv != null) {
				for (int r = 0; r < nrows; r++) {
					String[] rs = (String[]) pv.elementAt(r);
					String temp = "";
					for (int c = 0; c < ncols; c++) {
						temp = temp + rs[c] + " ";
					}
					out.println(temp);
				}
			}
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("Problem creating Asc File!");
		}
	}

	// rescale this grid as indicated using nearest neighbor
	public Grid rescale(int nr, int nc, double dx, double dy, double xmin,
			double ymin, int scalingAlgorithm) {
		Grid newGrid = new Grid(nc, nr, dx, dy, xmin, ymin);
		if (inMemFlag && (dataArray != null)) {
			double[][] newDataArray = new double[nc][nr];
			newGrid.dataArray = newDataArray;
			for (int j = 0; j < nr; j++) {
				// System.out.println("Working on row # "+j);
				double yloc = ymin + nr * dy - j * dy;
				for (int i = 0; i < nc; i++) {
					double xloc = xmin + i * dx;
					double val = this.interpValue(xloc, yloc, scalingAlgorithm);
					newDataArray[i][j] = val;
				}
			}
		} else { // using PersistentVector for data storage
			newGrid.pv = new PersistentVector();
			newGrid.pv.setFirstRow(6);
			newGrid.pv.setFieldDelimiter("#x20");
			String[] rowvals = new String[nc];
			for (int j = 0; j < nr; j++) {
				// System.out.println("Working on row # "+j);
				double yloc = ymin + nr * dy - j * dy;
				for (int i = 0; i < nc; i++) {
					double xloc = xmin + i * dx;
					double val = this.interpValue(xloc, yloc, scalingAlgorithm);
					String valStr;
					if (val > no_val_threshold) {
						valStr = NODATA_value_String;
					} else {
						valStr = (new Double(val)).toString();
					}
					rowvals[i] = valStr;
				}
				newGrid.pv.addElement(rowvals);
				rowvals = new String[nc]; // needed to make sure new object
											// added to pv
			}
		}
		return newGrid;
	}

	// reset values in range (minval,maxval) to newvalue
	public Grid reset(double minval, double maxval, double newvalue) {
		// create a new Grid with same parameters as this one
		Grid newGrid = new Grid(ncols, nrows, delx, dely, xllcorner, yllcorner);
		if (inMemFlag && (dataArray != null)) {
			double[][] newDataArray = new double[ncols][nrows];
			newGrid.dataArray = newDataArray;
			for (int j = 0; j < nrows; j++) {
				for (int i = 0; i < ncols; i++) {
					double val = this.getValue(i, j);
					if ((val > minval) && (val < maxval))
						val = newvalue;
					newDataArray[i][j] = val;
				}
			}
		} else { // using PersistentVector for data storage
			newGrid.pv = new PersistentVector();
			newGrid.pv.setFirstRow(6);
			newGrid.pv.setFieldDelimiter("#x20");
			String[] rowvals = new String[ncols];
			for (int j = 0; j < nrows; j++) {
				for (int i = 0; i < ncols; i++) {
					double val = this.getValue(i, j);
					if ((val > minval) && (val < maxval))
						val = newvalue;
					String valStr;
					if (val > no_val_threshold) {
						valStr = NODATA_value_String;
					} else {
						valStr = (new Double(val)).toString();
					}
					rowvals[i] = valStr;
				}
				newGrid.pv.addElement(rowvals);
				rowvals = new String[ncols]; // needed to make sure new object
												// added to pv
			}
		}
		return newGrid;
	}

	// transform values in range (minval,maxval) to by a multiplication and/or
	// addition
	public Grid transform(double minval, double maxval, double multFactor,
			double addFactor) {
		// create a new Grid with same parameters as this one
		Grid newGrid = new Grid(ncols, nrows, delx, dely, xllcorner, yllcorner);
		if (inMemFlag && (dataArray != null)) {
			double[][] newDataArray = new double[ncols][nrows];
			newGrid.dataArray = newDataArray;
			for (int j = 0; j < nrows; j++) {
				for (int i = 0; i < ncols; i++) {
					double val = this.getValue(i, j);
					if ((val > minval) && (val < maxval)) {
						val = multFactor * val + addFactor;
					}
					newDataArray[i][j] = val;
				}
			}
		} else { // using PersistentVector for data storage
			newGrid.pv = new PersistentVector();
			newGrid.pv.setFirstRow(6);
			newGrid.pv.setFieldDelimiter("#x20");
			String[] rowvals = new String[ncols];
			for (int j = 0; j < nrows; j++) {
				for (int i = 0; i < ncols; i++) {
					double val = this.getValue(i, j);
					if ((val > minval) && (val < maxval)) {
						val = multFactor * val + addFactor;
					}
					String valStr;
					if (val > no_val_threshold) {
						valStr = NODATA_value_String;
					} else {
						valStr = (new Double(val)).toString();
					}
					rowvals[i] = valStr;
				}
				newGrid.pv.addElement(rowvals);
				rowvals = new String[ncols]; // needed to make sure new object
												// added to pv
			}
		}
		return newGrid;
	}

	public int getNumberMissingDataPixels() {
		int cnt = 0;
		if (dataArray != null) { // in memory
			for (int i = 0; i < ncols; i++) {
				for (int j = 0; j < nrows; j++) {
					double val = getValue(i, j);
					if (val > no_val_threshold)
						cnt++;
				}
			}
			return cnt;
		}
		if (pv != null) {
			for (int j = 0; j < nrows; j++) {
				String[] rowSA = (String[]) (pv.elementAt(j));
				for (int i = 0; i < ncols; i++) {
					String val = rowSA[i];
					if (val.equals(NODATA_value_String)) {
						cnt++;
					}
				}
			}
		}
		return cnt;
	}

	// remove this Grid object
	// in particular, delete the potentially large Object file associated with
	// the PersistentVector
	public void delete() {
		if (pv != null) {
			pv.delete();
			pv = null;
		}
		if (dataArray != null) {
			dataArray = null;
		}
	}

	public double getFractionMissingDataPixels() {
		double frac = ((double) getNumberMissingDataPixels()) / (ncols * nrows);
		return frac;
	}

	// count the pixels with value 'val' within the distance 'thresh'
	public int getNumberPixelsWithValue(double val, double thresh) {
		double tval;
		int cnt = 0;
		if (dataArray != null) { // in memory
			for (int i = 0; i < ncols; i++) {
				for (int j = 0; j < nrows; j++) {
					double temp = getValue(i, j);
					if (Math.abs(temp - val) < thresh)
						cnt++;
				}
			}
			return cnt;
		}
		if (pv != null) {
			for (int j = 0; j < nrows; j++) {
				String[] rowSA = (String[]) (pv.elementAt(j));
				for (int i = 0; i < ncols; i++) {
					String ts = rowSA[i];
					if (!(ts.equals(NODATA_value_String))) {
						tval = new Double(ts).doubleValue();
						if (Math.abs(tval - val) < thresh)
							cnt++;
					}
				}
			}
		}
		return cnt;
	}

	// get fraction of nonMissing data pixels with value
	public double getFractionPixelsWithValue(double val, double thresh) {
		double frac = ((double) getNumberPixelsWithValue(val, thresh))
				/ ((ncols * nrows) - getNumberMissingDataPixels());
		return frac;
	}

	// createRectangularMask takes this grid and sets alls cells outside the
	// specified rectangle to one value and all cells inside to another
	public void createRectangularMask(double xmin, double ymin, double xmax,
			double ymax, double outvalue, double invalue) {
		String invalueStr;
		if (invalue > no_val_threshold) {
			invalueStr = NODATA_value_String;
		} else {
			invalueStr = (new Double(invalue)).toString();
		}
		String outvalueStr;
		if (outvalue > no_val_threshold) {
			outvalueStr = NODATA_value_String;
		} else {
			outvalueStr = (new Double(outvalue)).toString();
		}

		if (inMemFlag && (dataArray != null)) {
			for (int j = 0; j < nrows; j++) {
				double yloc = yllcorner + nrows * dely - j * dely;
				for (int i = 0; i < ncols; i++) {
					double xloc = xllcorner + i * delx;
					if ((xloc > xmin) && (xloc < xmax)) {
						if ((yloc > ymin) && (yloc < ymax)) {
							// inside the rectangle
							dataArray[i][j] = invalue;
						}
					} else {
						dataArray[i][j] = outvalue;
					}
				}
			}
		} else { // using PersistentVector for data storage
			if (pv == null) {
				pv = new PersistentVector();
				pv.setFirstRow(6);
				pv.setFieldDelimiter("#x20");
			}
			String[] rowvals = new String[ncols];
			for (int j = 0; j < nrows; j++) {
				double yloc = yllcorner + nrows * dely - j * dely;
				for (int i = 0; i < ncols; i++) {
					double xloc = xllcorner + i * delx;
					if ((xloc > xmin) && (xloc < xmax)) {
						if ((yloc > ymin) && (yloc < ymax)) {
							// inside the rectangle
							rowvals[i] = (new Double(invalue)).toString();
						}
					} else {
						rowvals[i] = (new Double(outvalue)).toString();
					}
				}
				pv.addElement(rowvals);
				rowvals = new String[ncols]; // needed to make sure new object
												// added to pv
			}

		}
	}

	// createShapeMask takes this grid and sets alls cells outside the
	// specified Shape to one value and all cells inside to another
	public void createShapeMask(Shape shape, double outvalue, double invalue) {
		String invalueStr;
		if (invalue > no_val_threshold) {
			invalueStr = NODATA_value_String;
		} else {
			invalueStr = (new Double(invalue)).toString();
		}
		String outvalueStr;
		if (outvalue > no_val_threshold) {
			outvalueStr = NODATA_value_String;
		} else {
			outvalueStr = (new Double(outvalue)).toString();
		}

		if (inMemFlag && (dataArray != null)) {
			for (int j = 0; j < nrows; j++) {
				double yloc = yllcorner + nrows * dely - j * dely;
				for (int i = 0; i < ncols; i++) {
					double xloc = xllcorner + i * delx;
					if (shape.contains(xloc, yloc)) {
						// inside the rectangle
						dataArray[i][j] = invalue;
					} else {
						dataArray[i][j] = outvalue;
					}
				}
			}
		} else { // using PersistentVector for data storage
			if (pv == null) {
				pv = new PersistentVector();
				pv.setFirstRow(6);
				pv.setFieldDelimiter("#x20");
			}
			String[] rowvals = new String[ncols];
			for (int j = 0; j < nrows; j++) {
				double yloc = yllcorner + nrows * dely - j * dely;
				for (int i = 0; i < ncols; i++) {
					double xloc = xllcorner + i * delx;
					if (shape.contains(xloc, yloc)) {
						// inside the rectangle
						rowvals[i] = invalueStr;
					} else {
						rowvals[i] = outvalueStr;
					}
				}
				pv.addElement(rowvals);
				rowvals = new String[ncols]; // needed to make sure new object
												// added to pv
			}

		}
	}

	public static void main(String[] args) {
		File testFile = new File("./test.asc");
		Grid grid = new Grid(testFile);
		System.out.println("nrows: " + grid.nrows);
		System.out.println("ncols: " + grid.ncols);
		System.out.println("delx: " + grid.delx);
		System.out.println("dely: " + grid.dely);

		System.out.println("(0,0): " + grid.getValue(0, 0));
		System.out.println("(1,1): " + grid.getValue(1, 1));
		System.out.println("(2,2): " + grid.getValue(2, 2));
		System.out.println("(2.5,2.5): "
				+ grid.interpValue(2.5, 2.5, NEAREST_NEIGHBOR));
		System.out.println("(0.0,0.0): "
				+ grid.interpValue(0.0, 0.0, NEAREST_NEIGHBOR));

		System.out
				.println("Starting to create an ASC file! ------------------");

		grid.createAsc("outtest.asc");
		System.out.println("Finished creating an ASC file! ------------------");

		System.out.println("Starting to rescale! ------------------");
		Grid newg = grid
				.rescale(grid.nrows / 2, grid.ncols / 2, grid.delx * 2,
						grid.dely * 2, grid.xllcorner, grid.yllcorner,
						NEAREST_NEIGHBOR);
		System.out.println("Finished rescaling! ------------------");
		// newg.createAsc("outtest2.asc");

		// String[] arr = (String[])grid.pv.elementAt(5);

		// System.out.println("Str: "+arr[0]);
	}

	private Hashtable getHeaderInformation(BufferedReader br) {
		Hashtable headerVals = new Hashtable();
		// unsure exactly how many header lines may occur
		// but each line should have only two string tokens with the first being
		// a name
		// assume the 'name' is NOT a number
		boolean eoh = false; // eoh -> end of header
		while (!eoh) {
			try {
				cachedLine = br.readLine();
			} catch (Exception w) {
				System.out
						.println("error reading next line in getHeaderInformation!");
				eoh = true;
			}
			StringTokenizer st = new StringTokenizer(cachedLine);
			int cnt = st.countTokens(); // should be only 2
			if (cnt != 2)
				eoh = true;
			String firstToken = st.nextToken().trim();
			String secondToken = st.nextToken().trim();
			eoh = true;
			try {
				Double.parseDouble(firstToken);
			} catch (Exception e) {
				eoh = false;
			}
			if (!eoh) {
				headerVals.put(firstToken, secondToken);
			}
			if (!headerVals.containsKey("NODATA_value")) {
				headerVals.put("NODATA_value", "-9999"); // set a default
			}
		}
		return headerVals;
	}

}