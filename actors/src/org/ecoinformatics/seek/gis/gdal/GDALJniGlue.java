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

package org.ecoinformatics.seek.gis.gdal;

/**
 *
 */
class GDALJniGlue {
	/**
	 * GDALTranslation glue
	 */
	public native String GDALTranslate(String outputType, String outputFormat,
			String inputFilename, String outputFilename);

	/**
	 * GDALWarp glue
	 */
	public native String GDALWarp(String inputParams, String outputParams,
			String outputType, String inputFilename, String outputFilename);

	/**
	 * load the library
	 */
	static {
		System.loadLibrary("gdalactor");
	}

	/**
	 * test the jni glue
	 */
	public static void main(String[] args) {
		// System.out.println(System.getProperty("java.library.path"));
		// System.loadLibrary("GISBuffer");
		GDALJniGlue g = new GDALJniGlue();
		System.out.println("calling GDALJniGlue");
		g.GDALTranslate("Byte", "AAIGrid", "/home/berkley/hydro1k/na_dem.bil",
				"/home/berkley/hydro1k/na_dem.ascii");
	}
}