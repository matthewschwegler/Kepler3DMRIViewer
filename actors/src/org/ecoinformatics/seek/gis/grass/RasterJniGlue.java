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

/**
 *
 */
class RasterJniGlue {
	public native int ReadInput(
			String pointDataFileName /* charpointDataFile */, int numSitePoints /*
																				 * int
																				 * numSitePoints
																				 */
	);

	public native void SetGridSize(int nrow /* int nrow */, int ncol /* int ncol */
	);

	public native double[] GetBoundary();

	public native int SetBoundary(double xmin, double ymin, double xmax,
			double ymax);

	public native int PerformRasterization();

	public native int WriteOutput(String raster_map);

	/**
	 * load the library
	 */
	static {
		System.loadLibrary("gisraster");
	}

	/**
	 * test the jni glue
	 */
	public static void main(String[] args) {
		// System.setProperty("java.library.path","c:\\kepler");
		System.out.println(System.getProperty("java.library.path"));
		// System.loadLibrary("GISRaster");
		RasterJniGlue g = new RasterJniGlue();
		System.out.println("calling GISRaster");
		g.ReadInput("HullPoint.txt", 12);
		g.SetGridSize(20, 20);
		double[] boundary = g.GetBoundary();
		System.out.println(boundary[0] + " " + boundary[1] + " " + boundary[2]
				+ " " + boundary[3]);
		double w = boundary[2] - boundary[0];
		double h = boundary[3] - boundary[1];
		double xmin = boundary[0] - w * 0.5;
		double ymin = boundary[1] - h * 0.5;
		double xmax = boundary[2] + w * 0.5;
		double ymax = boundary[3] + h * 0.5;
		System.out.println(xmin + " " + ymin + " " + xmax + " " + ymax);
		g.SetBoundary(xmin, ymin, xmax, ymax);
		g.PerformRasterization();
		g.WriteOutput("HullRaster0.txt");
	}
}