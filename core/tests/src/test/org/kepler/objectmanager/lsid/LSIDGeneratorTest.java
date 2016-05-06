/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-12-07 15:38:14 -0800 (Tue, 07 Dec 2010) $' 
 * '$Revision: 26434 $'
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

package test.org.kepler.objectmanager.lsid;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.objectmanager.lsid.LSIDGenerator;
import org.kepler.util.AuthNamespace;

public class LSIDGeneratorTest extends TestCase {
	
	public AuthNamespace an;

	public LSIDGeneratorTest(String name) {
		super(name);
		
		an = AuthNamespace.getInstance();
		an.initializeForTesting("localhost","testing");
		
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() {
	}

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() {
		
	}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() {
		// use the user's home directory as a test directory
		System.setProperty("KEPLER", System.getProperty("user.dir"));
		System.out.println("using " + System.getProperty("KEPLER"));
		
		TestSuite suite = new TestSuite();
		suite.addTest(new LSIDGeneratorTest("initialize"));
		suite.addTest(new LSIDGeneratorTest("getNewLsidTest"));
		
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
	 * test the newLsid method.
	 */
	public void getNewLsidTest() {
		try {
			LSIDGenerator generator = LSIDGenerator.getInstance();
			
			KeplerLSID lsid1 = generator.getNewLSID();
			System.out.println("lsid: " + lsid1.getObject());
			assertEquals(lsid1.getAuthority(), an.getAuthority());
			assertEquals(lsid1.getNamespace(), an.getNamespace());
			assertTrue(lsid1.getObject() != null);
			assertTrue(lsid1.getRevision() != null);

			KeplerLSID lsid2 = generator.getNewLSID();
			System.out.println(lsid2);
			assertEquals(lsid2.getAuthority(), an.getAuthority());
			assertEquals(lsid2.getNamespace(), an.getNamespace());
			assertTrue(lsid2.getObject() != null);
			assertTrue(lsid2.getRevision() != null);
		} catch (Exception e) {
			fail("unexpected exception: " + e.getMessage());
		}
	}
}