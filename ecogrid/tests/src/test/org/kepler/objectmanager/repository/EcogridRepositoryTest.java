/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2011-01-25 15:30:42 -0800 (Tue, 25 Jan 2011) $' 
 * '$Revision: 26836 $'
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

package test.org.kepler.objectmanager.repository;

import java.io.InputStream;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.objectmanager.repository.EcogridRepositoryResults;
import org.kepler.objectmanager.repository.Repository;
import org.kepler.objectmanager.repository.RepositoryManager;

/**
 * a test to check RepositoryManager and the EcogridRepository
 */
public class EcogridRepositoryTest  extends TestCase
{

  public EcogridRepositoryTest(String name)
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
    suite.addTest(new EcogridRepositoryTest("initialize"));
    suite.addTest(new EcogridRepositoryTest("testSearch"));
    suite.addTest(new EcogridRepositoryTest("testGet"));
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
   * test the search capability of the repository
   */
  public void testSearch()
  {
    try
    {
      Repository repo = RepositoryManager.getInstance().getRepository("keplerRepository");
      Iterator results = repo.search("a", false);
      if(results == null)
      {
        System.out.println("results is null");
        fail("result is null");
      }
      
      while(results.hasNext())
      {
        EcogridRepositoryResults result = (EcogridRepositoryResults)results.next();
        //System.out.println("result: " + result.toString());
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("Error: " + e.getMessage());
    }
  }
  
  /**
   * test the get methods
   */
  public void testGet()
  {
    try
    {
      Repository repo = RepositoryManager.getInstance().getRepository("keplerRepository");
      InputStream is = repo.get("actor.200.1", false);
      StringBuffer sb = new StringBuffer();
      byte[] b = new byte[1024];
      int numread = is.read(b, 0, 1024);
      while(numread != -1)
      {
        sb.append(new String(b, 0, numread));
        numread = is.read(b, 0, 1024);
      }
      
      String s = sb.toString();
      System.out.println(s);
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("Error in get: " + e.getMessage());
    }
  }
}