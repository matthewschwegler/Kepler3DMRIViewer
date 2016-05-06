/*
 * Copyright (c) 2010 The Regents of the University of California.
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

package test.org.kepler.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.util.AuthNamespace;

public class AuthNamespaceTest extends TestCase {

	public AuthNamespace an;

	public AuthNamespaceTest(String name) {
		super(name);
		
		an = AuthNamespace.getInstance();
		an.initializeForTesting("kepler-test.org/auth","testing");
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() {
	}

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() {}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() {
		// use the user's home directory as a test directory
		System.setProperty("KEPLER", System.getProperty("user.dir"));
		System.out.println("using " + System.getProperty("KEPLER"));
		
		TestSuite suite = new TestSuite();
		suite.addTest(new AuthNamespaceTest("initialize"));
		suite.addTest(new AuthNamespaceTest("testHashValue"));
		
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
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void testHashValue() {
		
//		String theHash = an.getHashValue();
//		System.out.println(theHash);
//		assertTrue(theHash.equals("kepler-test.org.auth.testing"));
		
	}
}
