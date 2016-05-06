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

package org.ecoinformatics.seek.backprop;

class BackpropJniGlue {
	/**
	 * native footprint for PresampleLayers
	 */
	public native void doBackProp(String inputFile /* charlayerSetFileName */,
			String outputFile /* chardataPointFileName */);

	/**
	 * load the library
	 */
	static {
		System.loadLibrary("backprop");
	}

	/**
	 * test the jni glue
	 */
	public static void main(String[] args) {
		System.out.println("running backprop jni test");
		System.out.println("creating BackPropJniGlue object");
		BackpropJniGlue b = new BackpropJniGlue();
		System.out.println("calling doBackProp");
		b
				.doBackProp(
						"/Users/berkley/project/kepler/lib/testdata/backprop/datafile.txt",
						"/Users/berkley/project/kepler/lib/testdata/backprop/output.txt");
		System.out.println("done with backprop jni test");
	}
}