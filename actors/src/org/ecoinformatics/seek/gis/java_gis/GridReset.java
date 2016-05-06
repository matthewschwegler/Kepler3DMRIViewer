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
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This actor is for transforming some range of values in a grid to some other
 * value. If the current value of a grid cell is within the given range (minval,
 * maxval) and a 'newval' is given, the values in the range are set to 'newval'.
 * If 'newval' is empty or the input string is an invalid representation of a
 * double, then the current value is multiplied by the multiplicationFactor and
 * the additonParameter is added and the new value is the result.
 * 
 * (Actually, a new grid is returned, rather than revising the current grid.)
 * 
 * @author Dan Higgins NCEAS UC Santa Barbara
 */
public class GridReset extends Transformer implements SequenceActor {

	/**
	 * The minumum value in a range where the grid pixel value is to be reset.
	 */
	public Parameter minvalParameter;
	/**
	 * The maximum value in a range where the grid pixel value is to be reset.
	 */
	public Parameter maxvalParameter;
	/**
	 * The new value of pixels in the (minval,maxval) range. If absent,
	 * transform the existing value
	 */
	public Parameter newvalParameter;
	/**
	 * if newvalParameter is empty, multiply the existing value by this factor
	 */
	public Parameter multiplicationFactor;
	/**
	 * if newvalParameter is empty, add this to the existing value
	 */
	public Parameter additionParameter;

	/**
	 * Boolean setting to determine whether or not to use disk for storing grid
	 * data rather than putting all data in RAM arrays
	 */
	public Parameter useDisk;

	/**
	 * The name to be given to the output File
	 */
	private FileParameter outputFilename;

	private Grid inputGrid;
	private Grid newg;

	private boolean finished;
	private String prevAscFilename = "";
	private String prevOutFilename = "";
	private String cachedLine = "";
	private Hashtable header = null;

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
	public GridReset(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		minvalParameter = new Parameter(this, "minvalParameter");
		minvalParameter.setDisplayName("Minumum Value");
		maxvalParameter = new Parameter(this, "maxvalParameter");
		maxvalParameter.setDisplayName("Maximum Value");
		newvalParameter = new Parameter(this, "newvalParameter");
		newvalParameter.setDisplayName("New Value");
		multiplicationFactor = new Parameter(this, "multiplicationFactor");
		multiplicationFactor.setDisplayName("Multiplication Factor");
		additionParameter = new Parameter(this, "additionParameter");
		additionParameter.setDisplayName("Addition Parameter");

		outputFilename = new FileParameter(this, "outputFileName");

		useDisk = new Parameter(this, "useDisk");
		useDisk.setDisplayName("Use disk storage (for large grids)");
		useDisk.setTypeEquals(BaseType.BOOLEAN);
		useDisk.setToken(BooleanToken.TRUE);
	}

	public void connectionsChanged(Port port) {
		super.connectionsChanged(port);
	}

	/**
	 * 
	 *@exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		try {
			String ascfilename = null;
			String ascOutfilename = null;
			String ascfileshortname = null;
			String temp = "";
			ascfilename = ((StringToken) input.get(0)).stringValue();
			boolean useDiskValue = ((BooleanToken) useDisk.getToken())
					.booleanValue();
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
				temp = temp + ".out";
				outputFilename.setExpression(temp);
				ascOutfilename = outputFilename.asFile().getPath();
			} else { // check to see if the output file is a dir
				File of = outputFilename.asFile();
				if (of.isDirectory()) {
					temp = ascfileshortname;
					int dotloc = temp.lastIndexOf(".");
					temp = temp.substring(0, dotloc);
					temp = temp + ".out";
					ascOutfilename = of.getPath() + "/" + temp;
				} else {
					ascOutfilename = outputFilename.asFile().getPath();
				}
			}

			System.out.println("ascOutfilename: " + ascOutfilename);

			File file = new File(ascfilename);

			if (file.exists()) {
				inputGrid = new Grid(file, !useDiskValue); // a 'false'
															// !useDiskValue
															// forces the use of
															// disk rather than
															// RAM
				double minval;
				try {
					minval = ((DoubleToken) minvalParameter.getToken())
							.doubleValue();
				} catch (Exception w) {
					minval = -1.0e-9;
				}

				double maxval;
				try {
					maxval = ((DoubleToken) maxvalParameter.getToken())
							.doubleValue();
				} catch (Exception w) {
					maxval = 1.0e+9;
				}
				boolean newvalFlag = true;
				double newval = 0.0;
				try {
					newval = ((DoubleToken) newvalParameter.getToken())
							.doubleValue();
				} catch (Exception w) {
					// if newval is not a number (e.g. empty) set flag
					// this will often be the case when we want to scale or
					// shift values
					newvalFlag = false;
				}

				double multFactor;
				try {
					multFactor = ((DoubleToken) multiplicationFactor.getToken())
							.doubleValue();
				} catch (Exception w) {
					multFactor = 1.0;
				}
				double addFactor;
				try {
					addFactor = ((DoubleToken) additionParameter.getToken())
							.doubleValue();
				} catch (Exception w) {
					addFactor = 0.0;
				}
				if (newvalFlag) {
					newg = inputGrid.reset(minval, maxval, newval);
				} else {
					newg = inputGrid.transform(minval, maxval, multFactor,
							addFactor);
				}
				newg.createAsc(ascOutfilename);
				System.out.println("ready to send: " + ascOutfilename);

				output.broadcast(new StringToken(ascOutfilename));
			} else {
				throw new IllegalActionException("Input file " + ascfilename
						+ " does not exist.");
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
		inputGrid.delete();
		newg.delete();
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