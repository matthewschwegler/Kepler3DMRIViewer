/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-12-07 15:37:09 -0800 (Tue, 07 Dec 2010) $' 
 * '$Revision: 26431 $'
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

package test.org.kepler.authentication;

import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.authentication.Domain;
import org.kepler.authentication.DomainList;


public class DomainListTest  extends TestCase
{
  
  public DomainListTest(String name)
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
    suite.addTest(new DomainListTest("initialize"));
    suite.addTest(new DomainListTest("getDomainListTest"));
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
   * test the newLsid method.
   */
  public void getDomainListTest()
  {
    try
    {
      DomainList list = DomainList.getInstance();
      Vector v = list.getDomainList();
      Domain d = (Domain)v.elementAt(0);
      Domain d2 = (Domain)v.elementAt(1);
      if(!d.getDomain().equals("GEON"))
      {
        fail("First domain should be geon");
      }
      else if(!d.getServiceOperation().equals("loginUserMyProxy"))
      {
        fail("First service op should be loginUserMyProxy");
      }
      else if(!d2.getDomain().equals("SEEK"))
      {
        fail("2nd domain should be SEEK");
      }
      else if(d2.getServiceClass().indexOf("org.kepler.authentication.LDAPAuthenticationService") == -1)
      {
        fail("the 2nd service class should be LDAPAuthenticationService");
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("unexpected exception: " + e.getMessage());
    }
  }
}