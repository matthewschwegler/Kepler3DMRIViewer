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
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * The purpose of this actor is to take a set of (x, y) points and return the
 * points that define the convex hull around the input. The convex hull can be
 * thought of as the region defined by a 'rubber band' placed around the
 * original data set. It is a sort of smallest polygon surrounding the input.
 * The ConvexHull routine from grass is called via JNI for this calculation.
 * 
 * 'pointFileName' is a tab delimited text file with x,y input points
 * 'numSitePoint' is the number of x,y pairs in the pointFileName file
 * 'hullFileName' is the name to be given to the hull point list file
 * 'numHullPoint' is the number of x,y pairs in the hull file 'hullFileResult'
 * is the output hull file name (same value as the 'hullFileName' but used as a
 * trigger for output
 */
public class GISHullActor extends TypedAtomicActor {
	// input ports
	public TypedIOPort pointFileName = new TypedIOPort(this, "pointFileName",
			true, false);
	public TypedIOPort hullFileName = new TypedIOPort(this, "hullFileName",
			true, false);
	public TypedIOPort numHullPoint = new TypedIOPort(this, "numHullPoint",
			false, true);
	public TypedIOPort hullFileResult = new TypedIOPort(this, "hullFileResult",
			false, true);

	public GISHullActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
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
		// System.out.println("firing GISHullActor");
		super.fire();

		StringToken inputFiletToken = (StringToken) pointFileName.get(0);
		String inputFiletNameStr = inputFiletToken.stringValue();

		StringToken outputFileToken = (StringToken) hullFileName.get(0);
		String outFileStr = outputFileToken.stringValue();

		// System.out.println("calling GISHull JNI code in actorcvs");
		HullJniGlue g = new HullJniGlue();
		int num_HullPoint = g.GISHull(inputFiletNameStr, outFileStr);

		numHullPoint.broadcast(new IntToken(num_HullPoint));
		hullFileResult.broadcast(new StringToken(outFileStr));
	}
}