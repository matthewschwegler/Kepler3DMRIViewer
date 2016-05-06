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

/**
 * 
 */
package org.kepler.kar.karxml;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.kar.KARFile;
import org.kepler.util.FileUtil;
import org.xml.sax.SAXException;

/**
 * @author Aaron Schultz
 */
public class KarXmlGeneratorTest extends TestCase {

	public KarXmlGeneratorTest(String name) {
		super(name);
	}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(new KarXmlGeneratorTest("initialize"));
		suite.addTest(new KarXmlGeneratorTest("testProcessXmlEntryContents"));
		suite.addTest(new KarXmlGeneratorTest("testGetKarXml"));
		return suite;
	}

	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() {
		assertTrue(1 == 1);
	}

	/**
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	public void tearDown() throws Exception {
	}

	public void testProcessXmlEntryContents() {
		KarXmlGenerator kxg = new KarXmlGenerator();

		StringBuffer sbTest = new StringBuffer();
		sbTest.append("<kar>\n <karEntry> <?xml version=\"1.0\"?></karEntry>");
		sbTest.append("<karEntry><?xml?> </karEntry></kar>");

		StringBuffer sbExpected = new StringBuffer();
		sbExpected.append("<kar>\n <karEntry> </karEntry>");
		sbExpected.append("<karEntry> </karEntry></kar>");

		String result = kxg.processXmlEntryContents(sbTest.toString());
		// System.out.println(result);

		String expectedResult = sbExpected.toString();
		boolean success = result.equals(expectedResult);
		assertTrue(success);

	}

	/**
	 * Test method for {@link org.kepler.kar.karxml.KarXmlGenerator#getKarXml()}
	 * .
	 * 
	 * @throws IOException
	 */
	public void testGetKarXml() {

		try {

			// Get the test KAR file
			String karFileStr = "org/kepler/kar/karxml/KarXmlTest.kar";
			File karFile = FileUtil.getResourceAsFile(this, karFileStr);
			KARFile kf = new KARFile(karFile);

			// Get the prebuilt test karxml output file and read it in
			String karXmlFileStr = "org/kepler/kar/karxml/KarXmlTest.xml";
			File karXmlFile = FileUtil.getResourceAsFile(this, karXmlFileStr);
			String prebuiltKarXml = FileUtil.readFileAsString(karXmlFile);

			// process the kar into a karxml file
			KarXmlGenerator kxg = new KarXmlGenerator();
			kxg.setKarFile(kf);
			String generatedKarXml = kxg.getKarXml();

			// compare the processed karxml file to the prebuilt test case
			// first remove all formatting characters
			String trimPrebuiltKarXml = prebuiltKarXml.replaceAll(
					"[\\n\\r\\f\\t]", "");
			String trimGeneratedKarXml = generatedKarXml.replaceAll(
					"[\\n\\r\\f\\t]", "");

			// System.out.println(trimPrebuiltKarXml);
			// System.out.println("\n**********************\n**********************\n");
			// System.out.println(trimGeneratedKarXml);

			// The two string should be equal
			boolean success = trimPrebuiltKarXml.equals(trimGeneratedKarXml);
			// System.out.println("Success: " + success);
			assertTrue(success);

		} catch (IOException e) {
			fail(e.getMessage());
		}
	  catch (SAXException ee) {
    fail(ee.getMessage());
    }
	}

}
