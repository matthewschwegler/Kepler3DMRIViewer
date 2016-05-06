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
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * <p>
 * The purpose of this actor is to take a set of (x, y) points and return the
 * points that define the convex hull around the input. The convex hull can be
 * thought of as the region defined by a 'rubber band' placed around the
 * original data set. It is a sort of smallest polygon surrounding the input.
 * The ConvexHull routine is implemented in Java and is thus portable to any
 * java-enabled system. This actor is designed to have the same functionality as
 * the JNI basedGISHullActor
 * </p>
 * <p>
 *'pointFileName' is a tab delimited text file with x,y input points<br/>
 *'hullFileName' is the name to be given to the hull point list file<br/>
 *'numHullPoint' is the number of x,y pairs in the hull file<br/>
 *'hullFileResult' is the output hull file name (same value as the
 * 'hullFileName' but used as a trigger for output
 * </p>
 * <p>
 * There is also a 'scaleFactorParameter'. This is the scale factor for an
 * AffineTransformation of the shape created by the ConvexHull. The convexHull
 * shape is scaled by this factor (linearly), centered on the center of the
 * convexhull bounding rectangle. The scale by area, set the scalefactor to the
 * square root of the area scaling factor (i.e. to make a shape with twice the
 * area, set the scale factor to SQRT(2) )
 * </p>
 * <p>
 * Note: if the scaleFactorParameter is empty or not a number, no scaling will
 * be done.
 * </p>
 */

public class GISHullActorJ extends TypedAtomicActor {
	// input ports
	/**
	 * 'pointFileName' is a tab delimited text file with x,y input points
	 */
	public TypedIOPort pointFileName = new TypedIOPort(this, "pointFileName",
			true, false);
	/**
	 * 'hullFileName' is the name to be given to the hull point list file
	 */
	public TypedIOPort hullFileName = new TypedIOPort(this, "hullFileName",
			true, false);
	/**
	 * 'numHullPoint' is the number of x,y pairs in the hull file<br/>
	 */
	public TypedIOPort numHullPoint = new TypedIOPort(this, "numHullPoint",
			false, true);
	/**
	 * 'hullFileResult' is the output hull file name (same value as the
	 * 'hullFileName' but used as a trigger for output
	 */
	public TypedIOPort hullFileResult = new TypedIOPort(this, "hullFileResult",
			false, true);

	/**
	 * This is the scale factor for an AffineTransformation of the shape created
	 * by the ConvexHull. The convexHull shape is scaled by this factor
	 * (linearly), centered on the center of the convexhull bounding rectangle.
	 * The scale by area, set the scalefactor to the square root of the area
	 * scaling factor (i.e. to make a shape with twice the area, set the scale
	 * factor to SQRT(2) )
	 */
	public StringParameter scaleFactorParameter;

	public GISHullActorJ(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		scaleFactorParameter = new StringParameter(this, "scaleFactorParameter");
		scaleFactorParameter.setDisplayName("Scale Factor");

		pointFileName.setTypeEquals(BaseType.STRING);
		hullFileName.setTypeEquals(BaseType.STRING);
		numHullPoint.setTypeEquals(BaseType.INT);
		hullFileResult.setTypeEquals(BaseType.STRING);
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

		String scaleFactorString = "";
		double scaleFactor = 1.0;
		scaleFactorString = scaleFactorParameter.stringValue();
		try {
			scaleFactor = (new Double(scaleFactorString)).doubleValue();
			// System.out.println("Scale factor: "+scaleFactor);
		} catch (Exception w) {
			scaleFactorString = "";
			scaleFactor = 1.0;
		}
		StringToken inputFileToken = (StringToken) pointFileName.get(0);
		String inputFileNameStr = inputFileToken.stringValue();
		StringToken outputFileToken = (StringToken) hullFileName.get(0);
		String outFileStr = outputFileToken.stringValue();

		File pointFile = new File(inputFileToken.stringValue());
		ConvexHull ch = new ConvexHull(pointFile);
		if (scaleFactorString.equals("")) {
			ch.cvHullToFile(outFileStr);
		} else {
			ch.cvScaledHullToFile(scaleFactor, outFileStr);
		}

		int num_HullPoint = ch.getCHListSize();
		// note: scaled convex hull will have same number of points

		numHullPoint.broadcast(new IntToken(num_HullPoint));
		hullFileResult.broadcast(new StringToken(outFileStr));
	}

}