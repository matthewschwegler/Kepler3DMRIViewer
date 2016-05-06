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

import java.util.StringTokenizer;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 *
 */
public class GISBufferActor extends TypedAtomicActor {
	public TypedIOPort rasterFileName = new TypedIOPort(this, "rasterFileName",
			true, false);
	public TypedIOPort numRasterRows = new TypedIOPort(this, "numRasterRows",
			true, false);
	public TypedIOPort numRasterCols = new TypedIOPort(this, "numRasterCols",
			true, false);
	public TypedIOPort bufferFileName = new TypedIOPort(this, "bufferFileName",
			true, false);
	public TypedIOPort numDistances = new TypedIOPort(this, "numDistances",
			true, false);
	public TypedIOPort valDistances = new TypedIOPort(this, "valDistances",
			true, false);
	public TypedIOPort bufferFileResult = new TypedIOPort(this,
			"bufferFileResult", false, true);

	/**
	 * his actor takes a raster grid and assigns a 'buffed regoin around the
	 * cells with values of 1. This allows one to take a grid created using a
	 * convexHull and expand the masked region about its outer boundary.
	 * 
	 * inputs describe the grid and set the number of buffered regions to add
	 * and the number of cells to add for each buffered region.
	 */
	public GISBufferActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		rasterFileName.setTypeEquals(BaseType.STRING);
		numRasterRows.setTypeEquals(BaseType.INT);
		numRasterCols.setTypeEquals(BaseType.INT);
		bufferFileName.setTypeEquals(BaseType.STRING);
		numDistances.setTypeEquals(BaseType.INT);
		valDistances.setTypeEquals(BaseType.STRING);
		bufferFileResult.setTypeEquals(BaseType.STRING);
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
		// System.out.println("firing GISBufferActor");
		super.fire();

		StringToken inputFiletToken = (StringToken) rasterFileName.get(0);
		String inputFiletNameStr = inputFiletToken.stringValue();

		IntToken numRasterRowsToken = (IntToken) numRasterRows.get(0);
		int num_RasterRows = numRasterRowsToken.intValue();

		IntToken numRasterColsToken = (IntToken) numRasterCols.get(0);
		int num_RasterCols = numRasterColsToken.intValue();

		StringToken outputFileToken = (StringToken) bufferFileName.get(0);
		String outFileStr = outputFileToken.stringValue();

		IntToken numDistancesToken = (IntToken) numDistances.get(0);
		int num_distances = numDistancesToken.intValue();

		StringToken valDistancesToken = (StringToken) valDistances.get(0);
		StringTokenizer st = new StringTokenizer(valDistancesToken
				.stringValue());
		int count = st.countTokens();
		float[] distances = new float[count];
		int seq = 0;
		while (st.hasMoreElements()) {
			distances[seq++] = (new Float(st.nextToken())).floatValue();
		}

		// System.out.println("Running GISBuffer JNI code");
		BufferJniGlue g = new BufferJniGlue();
		int ret = g.GISBuffer(inputFiletNameStr, num_RasterRows,
				num_RasterCols, outFileStr, num_distances, distances);
		bufferFileResult.broadcast(new StringToken(outFileStr));
	}
}