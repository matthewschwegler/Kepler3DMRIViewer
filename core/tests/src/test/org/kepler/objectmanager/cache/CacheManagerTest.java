/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:21:34 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31119 $'
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

package test.org.kepler.objectmanager.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.objectmanager.cache.RawDataCacheObject;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * <p>
 * Title: SearchScopeTest
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */

public class CacheManagerTest extends TestCase {
	CacheManager cache;

	public CacheManagerTest(String name) {
		super(name);
		try {
			cache = CacheManager.getInstance();
		} catch (Exception e) {
			fail("could not get instance of cache: " + e.getMessage());
		}
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
		// note that the order of these tests DOES matter. don't change the
		// order!
		suite.addTest(new CacheManagerTest("initialize"));
		suite.addTest(new CacheManagerTest("testDBCreation"));
		suite.addTest(new CacheManagerTest("testInsertCacheObject"));
		suite.addTest(new CacheManagerTest("testGetCacheObject"));
		suite.addTest(new CacheManagerTest("testRemoveCacheObject"));
		suite.addTest(new CacheManagerTest("testClearCache"));
		suite.addTest(new CacheManagerTest("testGetCacheObjectIterator"));
		suite.addTest(new CacheManagerTest("testIsContained"));
		// suite.addTest(new CacheManagerTest("testSerialization"));
		suite.addTest(new CacheManagerTest("testKARHandling"));
		return suite;
	}

	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() {
		assertTrue(1 == 1);
	}

	//comment it out since the CacheUtil.executeSQLQuery() is comment out. 
//	public void testDBCreation() {
//		try {
//			ResultSet rs = CacheUtil
//					.executeSQLQuery("select name from cacheContentTable");
//			if (rs == null) {
//				fail("table cacheContentTable not created");
//			}
//			// System.out.println("ResultSet: " + rs);
//
//			// cache.clearCache();
//		} catch (Exception e) {
//			fail("Error creating db: " + e.getMessage());
//		}
//	}

	//comment it out since the CacheUtil.executeSQLQuery() is comment out.
//	public void testInsertCacheObject() {
//		try {
//			cache.clearCache();
//			RawDataCacheObject co = new RawDataCacheObject("testObj",
//					new KeplerLSID("urn:lsid:localhost:test:1:1"));
//			cache.insertObject(co);
//			ResultSet rs = CacheUtil
//					.executeSQLQuery("select name from cachecontenttable;");
//			rs.first();
//			String name = rs.getString("name");
//			assertTrue(name.equals("testObj"));
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail("error inserting object: " + e.getMessage());
//		}
//	}

	public void testGetCacheObject() {
		try {
			RawDataCacheObject co = (RawDataCacheObject) cache
					.getObject(new KeplerLSID("urn:lsid:localhost:test:1:1"));
			assertTrue(co.getName().equals("testObj"));
		} catch (Exception e) {
			fail("Error getting cache object: " + e.getMessage());
		}
	}

	public void testRemoveCacheObject() {
		try {
			KeplerLSID lsid = new KeplerLSID("urn:lsid:localhost:test:1:1");
			cache.removeObject(lsid);
			if (cache.getObject(lsid) == null) {
				assertTrue(true);
			} else {
				fail("the object should have been removed.");
			}
		} catch (Exception e) {
			fail("Error removing CacheObject: " + e.getMessage());
		}
	}

	public void testClearCache() {
		try {
			cache.clearCache();
			Iterator i = cache.getCacheObjectIterator();
			if (i.hasNext()) {
				fail("Cache is not empty");
			}
			assertTrue(true);
		} catch (Exception e) {
			fail("Error getting cache object: " + e.getMessage());
		}

	}

	public void testGetCacheObjectIterator() {
		try {
			cache.clearCache();
			RawDataCacheObject co1 = new RawDataCacheObject("test1",
					new KeplerLSID("urn:lsid:localhost:test:1:1"));
			RawDataCacheObject co2 = new RawDataCacheObject("test2",
					new KeplerLSID("urn:lsid:localhost:test:2:1"));
			RawDataCacheObject co3 = new RawDataCacheObject("test3",
					new KeplerLSID("urn:lsid:localhost:test:3:1"));

			Map expected = new HashMap();
			expected.put("test1", null);
			expected.put("test2", null);
			expected.put("test3", null);

			cache.insertObject(co1);
			cache.insertObject(co2);
			cache.insertObject(co3);
			Iterator i = cache.getCacheObjectIterator();
			while (i.hasNext()) {
				CacheObject co = (CacheObject) i.next();
				String name = co.getName();
				if (!expected.containsKey(name)) {
					fail("Item " + name + " in table but not expected");
				} else if (expected.get(name) != null) {
					fail("Item " + name + " found twice in iterator");
				} else {
					expected.put(name, Boolean.TRUE);
				}
			}
		} catch (Exception e) {
			fail("Error creating iterator: " + e.getMessage());
		}
	}

	public void testIsContained() {
		try {
			assertTrue(cache.isContained(new KeplerLSID(
					"urn:lsid:localhost:test:1:1")));
			assertFalse(cache.isContained(new KeplerLSID(
					"urn:lsid:localhost:test:4:1")));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error in testIsContained: " + e.getMessage());
		}
	}
	
	/*
	public void testSerialization() {
		try {
			// to work, this test requires that the getNewInstance() method in
			// CacheManager
			// be uncommented
			CacheManager cache2 = CacheManager.getNewInstance();
			assertTrue(cache2.isContained(new KeplerLSID(
					"urn:lsid:localhost:test:1:1")));
			CacheObjectInterface co = cache2.getObject(new KeplerLSID(
					"urn:lsid:localhost:test:1:1"));
			assertTrue(co.getName().equals("test1"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("serialization test failed: " + e.getMessage());
		}
	}
	*/

	public void testKARHandling() {

	}
}
