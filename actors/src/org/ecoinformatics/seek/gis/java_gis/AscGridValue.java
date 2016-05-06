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

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.Transformer;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This actor finds the value of a given position in an asc grid. Inputs include
 * the position point (in gis coordinates) and an *.asc gis file. Output is the
 * interpreted value using the indicated interpolation algorithm.
 * 
 * @author Dan Higgins NCEAS UC Santa Barbara
 */
public class AscGridValue extends Transformer {

	/**
	 * 'xLocation' is the xLocation (longitude) where the value is to be
	 * calculated
	 */
	public TypedIOPort xLocation = new TypedIOPort(this, "xLocation", true,
			false);

	/**
	 * 'yLocation' is the yLocation (latitude) where the value is to be
	 * calculated
	 */
	public TypedIOPort yLocation = new TypedIOPort(this, "yLocation", true,
			false);

	/**
	 * Boolean setting to determine whether or not to use disk for storing grid
	 * data rather than putting all data in RAM arrays
	 */
	public Parameter useDisk;

	/**
	 * This is the algoritm to be used in calculating cell values in the output
	 * grid from the values in the input grid Currently there are two choices:
	 * 'Nearest Neighbor' or 'Inverse Distance Weighted'
	 */
	public StringParameter algorithm;

	private Grid inputGrid;

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
	public AscGridValue(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		algorithm = new StringParameter(this, "algorithm");
		algorithm.setExpression("Nearest Neighbor");
		algorithm.addChoice("Nearest Neighbor");
		algorithm.addChoice("Inverse Distance");

		useDisk = new Parameter(this, "useDisk");
		useDisk.setDisplayName("Use disk storage (for large grids)");
		useDisk.setTypeEquals(BaseType.BOOLEAN);
		useDisk.setToken(BooleanToken.TRUE);

		xLocation.setTypeEquals(BaseType.DOUBLE);
		yLocation.setTypeEquals(BaseType.DOUBLE);
		output.setTypeEquals(BaseType.DOUBLE);
		input.setTypeEquals(BaseType.STRING);
	}

	/**
	 * 
	 * 
	 *@exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {

		String temp = "";
		double xloc = 0.0;
		double yloc = 0.0;
		double asc_value = 1.0E101;

		super.fire();
		if (xLocation.getWidth() > 0) { // has a connection
			if (xLocation.hasToken(0)) { // has a token

				try {
					DoubleToken xtoken = (DoubleToken) xLocation.get(0);
					xloc = xtoken.doubleValue();
				} catch (Exception w) {
					xloc = 0.0;
				}
			}
		}

		if (yLocation.getWidth() > 0) { // has a connection
			if (yLocation.hasToken(0)) { // has a token

				try {
					DoubleToken ytoken = (DoubleToken) yLocation.get(0);
					yloc = ytoken.doubleValue();
				} catch (Exception w) {
					xloc = 0.0;
				}
			}
		}

		try {
			String ascfilename = null;
			String ascOutfilename = null;
			String ascfileshortname = null;
			int width = input.getWidth();

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
				// System.out.println("ascfileshortname: "+ascfileshortname);
			}

			File file = new File(ascfilename);

			if (file.exists()) {
				inputGrid = new Grid(file, !useDiskValue); // a 'false'
															// !useDiskValue
															// forces the use of
															// disk rather than
															// RAM
				// System.out.println("nrows: "+inputGrid.nrows);
				// System.out.println("ncols: "+inputGrid.ncols);
				// System.out.println("delx: "+inputGrid.delx);
				// System.out.println("dely: "+inputGrid.dely);

				temp = algorithm.stringValue();
				if (temp.equals("Inverse Distance")) {
					algorithmToUse = _INVERSE_DISTANCE;
				} else {
					algorithmToUse = _NEAREST_NEIGHBOR;
				}

				asc_value = inputGrid.interpValue(xloc, yloc, algorithmToUse);

				if (inputGrid != null) {
					inputGrid.delete();
				}

				if (asc_value < 1.0E100) {
					output.broadcast(new DoubleToken(asc_value));
				} else {
					output.broadcast(DoubleToken.NIL);
				}
			}

		} catch (Exception eee) {
			throw new IllegalActionException("Problem Reading File");
		}
	}

}