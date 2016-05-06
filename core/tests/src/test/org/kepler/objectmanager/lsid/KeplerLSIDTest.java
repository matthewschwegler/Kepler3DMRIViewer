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

package test.org.kepler.objectmanager.lsid;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.objectmanager.lsid.KeplerLSID;

public class KeplerLSIDTest extends TestCase {

	private boolean deb = true;

	public KeplerLSIDTest(String name) {
		super(name);

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

		TestSuite suite = new TestSuite();
		suite.addTest(new KeplerLSIDTest("initialize"));
		suite.addTest(new KeplerLSIDTest("validateLsidTest"));
		suite.addTest(new KeplerLSIDTest("hashCodeTest"));

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
	 * test the KeplerLSID validation
	 */
	public void validateLsidTest() {
		if (deb)
			System.out.println("validateLsidTest()");
		String str = "";

		// This should not fail
		str = "urn:lsid:somestring:12:42:1";
		try {
			KeplerLSID lsid = new KeplerLSID(str);
			if (deb)
				System.out.println("    String: " + str);
			if (deb)
				System.out.println("KeplerLSID: " + lsid.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		// This should fail
		str = "urn:lsid:somestring:12:42";
		try {
			KeplerLSID lsid = new KeplerLSID(str);
			if (deb)
				System.out.println("    String: " + str);
			if (deb)
				System.out.println("KeplerLSID: " + lsid.toString());
			fail();
		} catch (Exception e) {
			// do nothing
		}

		// This should not fail
		str = "urn:lsid:somestring:12:42:1#lovely";
		try {
			KeplerLSID lsid = new KeplerLSID(str);
			if (deb)
				System.out.println("    String: " + str);
			if (deb)
				System.out.println("KeplerLSID: " + lsid.toString());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		// TODO add a bunch more validation tests here
	}

	public void hashCodeTest() {
		if (deb)
			System.out.println("hashCodeTest()");
		Hashtable<String, KeplerLSID> lsids = new Hashtable<String, KeplerLSID>();
		String str = "";
		try {
			str = "urn:lsid:somestring:12:42:1";
			lsids.put(str, new KeplerLSID(str));
			str = "urn:lsid:somestring:12:42:5";
			lsids.put(str, new KeplerLSID(str));
			str = "urn:lsid:somestring:16:42:5";
			lsids.put(str, new KeplerLSID(str));
			str = "urn:lsid:somestring:16:46:5#yo";
			lsids.put(str, new KeplerLSID(str));
		} catch (Exception e) {
			fail();
		}

		Hashtable<KeplerLSID, String> lsidStrings = new Hashtable<KeplerLSID, String>();
		for (String lsidStr : lsids.keySet()) {
			if (deb)
				System.out.println("    String: " + lsidStr);
			KeplerLSID lsid = lsids.get(lsidStr);
			if (deb)
				System.out.println("KeplerLSID: " + lsid.toString());
			assertTrue(lsids.containsKey(lsidStr));
			assertTrue(lsids.contains(lsid));
			assertTrue(lsids.containsValue(lsid));
			lsidStrings.put(lsid, lsidStr);
		}

		for (KeplerLSID lsid : lsidStrings.keySet()) {
			String lsidStr = lsidStrings.get(lsid);
			assertTrue(lsidStrings.containsKey(lsid));
			assertTrue(lsidStrings.contains(lsidStr));
			assertTrue(lsidStrings.containsValue(lsidStr));
		}

	}

}
