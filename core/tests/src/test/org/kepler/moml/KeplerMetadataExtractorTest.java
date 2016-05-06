/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

package test.org.kepler.moml;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.build.project.ProjectLocator;
import org.kepler.moml.KeplerActorMetadata;
import org.kepler.moml.KeplerMetadataExtractor;
import org.kepler.util.FileUtil;

public class KeplerMetadataExtractorTest extends TestCase {
	private boolean deb = false;

	public KeplerMetadataExtractorTest(String name) {
		super(name);

	}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() {

		TestSuite suite = new TestSuite();
		suite.addTest(new KeplerMetadataExtractorTest("initialize"));
		suite.addTest(new KeplerMetadataExtractorTest("testExtraction"));

		return suite;
	}

	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() {
		if (deb)
			System.out.println("initialize()");
		assertTrue(1 == 1);
	}

	/**
	 * Run through all the Actors in the actors module and make sure no
	 * exceptions get thrown when parsing KeplerMetadata.
	 */
	public void testExtraction() {
		if (deb)
			System.out.println("testExtraction()");

		int maxFilesToTest = 10000;

		try {
			// Hopefully this relative path works in the test environment
			// it works O.K. locally
			File f = new File(ProjectLocator.getProjectDir() + "/actors/resources/kar");
			String s = FileUtil.clean(f);
			File actorKarDir = new File(s);
			System.out.println(actorKarDir);

			String[] dirs = actorKarDir.list();
			
			for (int i = 0; i < dirs.length; i++) {
			  System.out.println("testing dir " + dirs[i]);
				// break if the maximum test file limit has been reached
				if (i > maxFilesToTest)
					break;

				String dir = dirs[i];
				if (dir.startsWith(".")) {
					// skip it
				} else {
					// get the kar directory
					File karDir = new File(actorKarDir, dir);
					if (karDir.isDirectory()) {
						// get the moml file
						String momlFileName = dir + ".xml";
						File momlFile = new File(karDir, momlFileName);
						String momlFileStr = FileUtil.clean(momlFile);
						if (deb)
							System.out.println("karDirStr: " + momlFileStr);
						
						try
						{
						  KeplerActorMetadata kam = KeplerMetadataExtractor
						  		.extractActorMetadata(momlFile);
						  if (deb)
	              System.out.println("\n" + kam.debugString());
						}
						catch(java.io.FileNotFoundException fnfe)
						{
						  //ignore this
						}
					} else {
						// skip it
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

	}

}
