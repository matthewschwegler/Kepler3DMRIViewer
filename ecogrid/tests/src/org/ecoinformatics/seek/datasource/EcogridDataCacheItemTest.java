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

package org.ecoinformatics.seek.datasource;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ecoinformatics.seek.ecogrid.EcoGridService;
import org.ecoinformatics.seek.ecogrid.EcoGridServicesController;
import org.kepler.authentication.AuthenticationManager;
import org.kepler.authentication.AuthenticationService;
import org.kepler.authentication.Domain;
import org.kepler.authentication.LDAPAuthenticationService;
import org.kepler.authentication.ProxyEntity;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.DataCacheListener;
import org.kepler.objectmanager.cache.DataCacheManager;
import org.kepler.objectmanager.cache.DataCacheObject;

/**
 * A JUnit test class for EcogridDataCacheItem class
 * @author tao
 *
 */
public class EcogridDataCacheItemTest  extends TestCase
{
	 /**Constants*/
	 private static final String DATAURL = "ecogrid://knb/datacache.1.1";
	 private static final String ENDPOINT = "http://library.kepler-project.org/kepler/services/AuthenticatedQueryService";
	 private static final String SERVICENAME = "Kpler EcoGrid QueryInterface";
	 private static final String SERVICETYPE =  "http://ecoinformatics.org/authenticatedqueryservice-1.0.0";
	 private static final String SERVCIEGROUP = "SEEK";
	 private static final String USERNAME = "uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org";
	 private static final String PASSWORD  = "kepler";
	 private CacheManager cache = null;
	
	 /*
	   * @see TestCase#setUp()
	   */
	  protected void setUp() throws Exception {
	    super.setUp();
	   cache = CacheManager.getInstance();
		
	  }


	  /*
	   * @see TestCase#tearDown()
	   */
	  protected void tearDown() throws Exception {
	    super.tearDown();
	  }


	  /**
	   * Constructor for TextComplexFormatDataReaderTest.
	   * @param arg0
	   */
	  public EcogridDataCacheItemTest(String arg0) {
	    super(arg0);
	  }


	  /**
	   * Create a suite of tests to be run together
	   */
	  public static Test suite() {
	    TestSuite suite = new TestSuite();
	    suite.addTest(new EcogridDataCacheItemTest("initialize"));
	    suite.addTest(new EcogridDataCacheItemTest("recoverDownloadTest"));
	    return suite;
	  }


	  /**
	   * Run an initial test that always passes to check that the test
	   * harness is working.
	   */
	  public void initialize() {
	    assertTrue(1 == 1);
	  }

      /**
       * Tests if an error in download will be cached - User failed download a data file at first time since, 
       * for example, a network glitch. After the network glitch was fixed, the use should download the
       * data file successfully. 
       * Here is the idea - there is data file in server which is not public readable.
       * User PUBLIC will try to download file first. The download will fail.  But the second time 
       * the download will succeed as it is done by a logined user . 
       */
	  public void recoverDownloadTest() throws Exception
	  {
		  // clear up cache
		  cache.clearCache();
		  
		  // download data file as public user and it will fail
		  DummyDataCacheListener dummy = new DummyDataCacheListener();
		  EcogridDataCacheItem cachedDataItem = (EcogridDataCacheItem) DataCacheManager.getCacheItem(dummy, "Data " + DATAURL, 
				  DATAURL, EcogridDataCacheItem.class.getName());
		  cachedDataItem.setEndPoint(ENDPOINT);
		  //cachedDataItem.setShowAlterWindow(false);
		  cachedDataItem.start();
		  Thread.sleep(5000);
		  //check if cache has the data file (It should NOT have it)
		  assertFalse(cache.isContained(cachedDataItem.getLSID()));
		  
		  //login first
		  EcoGridService queryService = createEcoGridServiceObject();
		  EcoGridServicesController.getInstance().addService(queryService);
		  AuthenticationManager authMan = AuthenticationManager.getManager();
		  AuthenticationService service = authMan.getAuthenticationService("SEEK");
		  Domain d = AuthenticationManager.getDomain("SEEK");
		  ProxyEntity pentity = ((LDAPAuthenticationService)service).authenticate(d, USERNAME, PASSWORD);
		  authMan.addProxyEntity(pentity);
		
		  // download the data file again as user kepler and it should succeed
		  dummy = new DummyDataCacheListener();
		  cachedDataItem = (EcogridDataCacheItem) DataCacheManager.getCacheItem(dummy, "Data " + 
				  DATAURL, DATAURL, EcogridDataCacheItem.class.getName());
		  cachedDataItem.setEndPoint(ENDPOINT);
		  cachedDataItem.start();
		  Thread.sleep(5000);
		  //check if cache has the data file (It should have it)
		  assertTrue(cache.isContained(cachedDataItem.getLSID()));
		  
	  }
	  
	  private EcoGridService createEcoGridServiceObject() throws Exception
	  {
		   EcoGridService service = new EcoGridService();
		   service.setServiceName(SERVICENAME);
		   service.setEndPoint(ENDPOINT);
		   service.setServiceType(SERVICETYPE);
		   service.setServiceGroup(SERVCIEGROUP);
		   return service;
	  }
	  
	  /*
	   * A dummy DataCacheListener class which will be used in EcogridDataCacheItem constructor
	   */
	  private class DummyDataCacheListener implements DataCacheListener
	  {
	  	    public void complete(DataCacheObject aItem)
	  	    {
	  	    	System.out.println("the action is complete");
	  	    }
	  }
}