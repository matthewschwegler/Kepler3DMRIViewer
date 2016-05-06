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

package org.ecoinformatics.seek.gis.java_gis;

import java.io.File;
import java.util.Hashtable;

import ptolemy.actor.lib.SequenceActor;
import ptolemy.actor.lib.Transformer;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * <p>
 * This actor converts an ascii raster grid file into another ascii grid file
 * with different extent and cell spacing A ASC grid file is read as input. (The
 * file name(s) are connected to the input port.)<br/>
 * The xllcorner, yllcorner, cellsize, numrows and numcols of the desired grid
 * are also input and a new ASC file is created. Algorithms are disk based
 * meaning that very large rasters can be handled.
 * </p>
 * 
 * <p>
 * The input is a multiport so multiple input file names can be attached to that
 * port. Multiple tokens are output in a sequence if multiple input filenames
 * are attached. Token consumption and production rates are automatically
 * calculated
 * </p>
 * 
 * <p>
 * A useExistingFile boolean parameter has been added. When true, the actor
 * checks to see if the output file already exists. If it does, it is just sent
 * to the output port without repeating the calculation.
 * </p>
 */
public class GridRescaler extends Transformer implements SequenceActor {

	/**
	 * The x-value of the lower left corner of the output grid Usually this is a
	 * longitude. This is input as a string and converted to a double.
	 */
	public Parameter xllcorner;

	/**
	 * The y-value of the lower left corner of the output grid Usually this is a
	 * latitude. This is input as a string and converted to a double.
	 */
	public Parameter yllcorner;

	/**
	 * The cell size of the output grid. The grid is assumed square so that x
	 * and y grid sizes are equal and converted to a double..
	 */
	public Parameter cellsize;

	/**
	 * The number of rows (cells in the y-direction) in the output grid. This
	 * determines the extent of y-values in the grid when combined with the cell
	 * size This is input as a string and converted to a integer.
	 */
	public Parameter numrows;

	/**
	 * The number of columns (cells in the x-direction) in the output grid. This
	 * determines the extent of x-values in the grid when combined with the cell
	 * size This is input as a string and converted to a integer.
	 */
	public Parameter numcols;

	/**
	 * This is the algoritm to be used in calculating cell values in the output
	 * grid from the values in the input grid Currently there are two choices:
	 * 'Nearest Neighbor' or 'Inverse Distance Weighted'
	 */
	public StringParameter algorithm;

	/**
	 * This is the name to be given to the output file. If left empty, the input
	 * file name plus a suffix (".out"+i) will be used for output file names
	 * Note that the input port is a multiport so multiple input can be
	 * converted in a single firing. If this parameter is a directory name,
	 * output file(s) will be placed in that directory
	 */
	public FileParameter outputFilename;

	/**
	 * Boolean setting to determine whether or not to use currently existing
	 * output file, if it exists
	 */
	public Parameter useExistingFile;

	/**
	 * Boolean setting to determine whether or not to use disk for storing grid
	 * data rather than putting all data in RAM arrays
	 */
	public Parameter useDisk;

	private Parameter output_tokenProductionRate;
	private Parameter input_tokenConsumptionRate;
	private Grid inputGrid;
	private Grid newg;

	private boolean finished;
	private String prevAscFilename = "";
	private String prevOutFilename = "";
	private String cachedLine = "";
	private Hashtable header = null;

	private int algorithmToUse = _NEAREST_NEIGHBOR;
	private static final int _NEAREST_NEIGHBOR = 0;
	private static final int _INVERSE_DISTANCE = 1;

	/**
	 * constructor
	 * 
	 *@param container
	 *            The container.
	 *@param name
	 *            The name of this actor.
	 *@exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 *@exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public GridRescaler(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		xllcorner = new Parameter(this, "xllcorner");
		yllcorner = new Parameter(this, "yllcorner");
		cellsize = new Parameter(this, "cellsize");
		numrows = new Parameter(this, "numrows");
		numcols = new Parameter(this, "numcols");

		algorithm = new StringParameter(this, "algorithm");
		algorithm.setExpression("Nearest Neighbor");
		algorithm.addChoice("Nearest Neighbor");
		algorithm.addChoice("Inverse Distance");

		input.setMultiport(true);
		output_tokenProductionRate = new Parameter(output,
				"tokenProductionRate");
		output_tokenProductionRate.setExpression("0");
		outputFilename = new FileParameter(this, "outputFileName");

		useExistingFile = new Parameter(this, "use Existing File");
		useExistingFile.setTypeEquals(BaseType.BOOLEAN);
		useExistingFile.setToken(BooleanToken.FALSE);

		useDisk = new Parameter(this, "use disk storage (for large grids)");
		useDisk.setTypeEquals(BaseType.BOOLEAN);
		useDisk.setToken(BooleanToken.TRUE);

	}

	/**
	 * Resets the output token production rate based on width of input port
	 */
	public void connectionsChanged(Port port) {
		super.connectionsChanged(port);
		if (port == input) {
			try {
				output_tokenProductionRate.setToken(new IntToken(input
						.getWidth()));
				// NOTE: schedule is invalidated automatically already
				// by the changed connections.
			} catch (IllegalActionException ex) {
				throw new InternalErrorException(this, ex,
						"Failed to set the token production rate of the "
								+ "output port");
			}
		}
	}

	/**
	 * Creates rescaled ascii grid for each input grid using the input
	 * parameters Uses the 'Grid' class that is disk-based, thus allowing very
	 * large grids.
	 * 
	 *@exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		boolean calculateFlag = true;
		super.fire();
		try {
			String ascfilename = null;
			String ascOutfilename = null;
			String ascfileshortname = null;
			String temp = "";
			int width = input.getWidth();

			boolean useExistingFileValue = ((BooleanToken) useExistingFile
					.getToken()).booleanValue();
			boolean useDiskValue = ((BooleanToken) useDisk.getToken())
					.booleanValue();

			for (int iii = 0; iii < width; iii++) {
				ascfilename = ((StringToken) input.get(iii)).stringValue();
				System.out.println("ascfilename: " + ascfilename);
				if (ascfilename.indexOf("/") > -1) {
					ascfileshortname = ascfilename.substring(ascfilename
							.lastIndexOf("/") + 1, ascfilename.length());
				} else if (ascfilename.indexOf("\\") > -1) {
					ascfileshortname = ascfilename.substring(ascfilename
							.lastIndexOf("\\") + 1, ascfilename.length());
				} else {
					ascfileshortname = ascfilename;
				}
				System.out.println("ascfileshortname: " + ascfileshortname);
				temp = outputFilename.stringValue();
				if (temp.trim().length() == 0) {
					temp = ascfilename;
					int dotloc = temp.lastIndexOf(".");
					temp = temp.substring(0, dotloc);
					temp = temp + ".out" + iii;
					outputFilename.setExpression(temp);
					ascOutfilename = outputFilename.asFile().getPath();
				} else { // check to see if the output file is a dir
					File of = outputFilename.asFile();
					if (of.isDirectory()) {
						temp = ascfileshortname;
						int dotloc = temp.lastIndexOf(".");
						temp = temp.substring(0, dotloc);
						temp = temp + ".out" + iii;
						ascOutfilename = of.getPath() + "/" + temp;
					} else {
						ascOutfilename = outputFilename.asFile().getPath();
					}
				}
				if (useExistingFileValue) {
					// check to see if output file
					File of1 = outputFilename.asFile();
					if (of1.exists()) {
						if (of1.isFile()) {
							calculateFlag = false;
						}
					}
				}

				System.out.println("ascOutfilename: " + ascOutfilename);

				File file = new File(ascfilename);

				if (file.exists()) {
					if (calculateFlag) {
						inputGrid = new Grid(file, !useDiskValue); // a 'false'
																	// !useDiskValue
																	// forces
																	// the use
																	// of disk
																	// rather
																	// than RAM
						System.out.println("nrows: " + inputGrid.nrows);
						System.out.println("ncols: " + inputGrid.ncols);
						System.out.println("delx: " + inputGrid.delx);
						System.out.println("dely: " + inputGrid.dely);
						//
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
							xmin = ((DoubleToken) xllcorner.getToken())
									.doubleValue();
						} catch (Exception w) {
							xmin = 0.0;
						}

						double ymin;
						try {
							ymin = ((DoubleToken) yllcorner.getToken())
									.doubleValue();
						} catch (Exception w) {
							ymin = 0.0;
						}

						double cs;
						try {
							cs = ((DoubleToken) cellsize.getToken())
									.doubleValue();
						} catch (Exception w) {
							cs = 1.0;
						}
						temp = algorithm.stringValue();
						if (temp.equals("Inverse Distance")) {
							algorithmToUse = _INVERSE_DISTANCE;
						} else {
							algorithmToUse = _NEAREST_NEIGHBOR;
						}
						newg = inputGrid.rescale(nrows, ncols, cs, cs, xmin,
								ymin, algorithmToUse);
						newg.createAsc(ascOutfilename);
						System.out.println("ready to send: " + ascOutfilename);

					}
					output.broadcast(new StringToken(ascOutfilename));
				} else {
					throw new IllegalActionException("Input file "
							+ ascfilename + " does not exist.");
				}
			}
		} catch (Exception eee) {
			throw new IllegalActionException("Problem Reading File");
		}
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 * 
	 *	 */
	public boolean postfire() throws IllegalActionException {
		if (inputGrid != null) {
			inputGrid.delete();
			newg.delete();
		}
		return super.postfire();
	}

	/**
	 * Pre fire the actor. Calls the super class's prefire in case something is
	 * set there.
	 * 
	 *	 *@exception IllegalActionException
	 */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

}