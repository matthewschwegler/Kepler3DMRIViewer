/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-12-07 15:36:16 -0800 (Tue, 07 Dec 2010) $' 
 * '$Revision: 26428 $'
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

import java.io.ByteArrayOutputStream;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ecoinformatics.ecogrid.queryservice.QueryServiceGetToStreamClient;




/**
 * This class will test QueryServiceGetToStreamClient object
 */

public class QueryServiceGetToStreamClientTest extends TestCase
{
  //constant
  private static String endpoint = "http://knb.ecoinformatics.org/knb/services/QueryService";
  private static final String docid = "tao.1.1";

  public QueryServiceGetToStreamClientTest(String name)
  {
    super(name);
  }


  /**
   * Establish a testing framework by initializing appropriate objects
   */
  public void setUp()
  {
   
  }

  /**
   * Release any objects after tests are complete
   */
  public void tearDown()
  {
  }

  /**
   * Create a suite of tests to be run together
   */
  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTest(new QueryServiceGetToStreamClientTest("initialize"));
    suite.addTest(new QueryServiceGetToStreamClientTest("get"));
    return suite;
  }

  /**
   * Run an initial test that always passes to check that the test
   * harness is working.
   */
  public void initialize()
  {
    assertTrue(1 == 1);
  }

  /**
   * A test for method reslove for ";"
   *
   * Note - as of 12/9/2005 the contents of this try block was commented out.  This is the only
   * place which used Util.DBPATH or LOCALFILENAME.
   * Since this test was nonfunctional and was also broken by future revisions to org.ecoinformatics.util.Util
   * I commented out the references to Util.DBPATH to enable compilation.
   */
  public void get()
  {
    try
    {
      QueryServiceGetToStreamClient client = 
    	  new QueryServiceGetToStreamClient(new URL(endpoint));
      client.get(docid, System.out);
      assertEquals(true, true);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      client.get(docid, bytes);
      assertNotNull(bytes);
      assertTrue(bytes.size() > 0);
    }
    catch(Exception e)
    {
    	e.printStackTrace();
      System.out.println("error is "+ e.getMessage());
      assertTrue(1==2);
    }
  }

 

}


