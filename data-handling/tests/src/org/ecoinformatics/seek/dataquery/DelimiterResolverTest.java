/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-12-07 15:37:25 -0800 (Tue, 07 Dec 2010) $' 
 * '$Revision: 26432 $'
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

package org.ecoinformatics.seek.dataquery;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;




/**
 *
 */

public class DelimiterResolverTest extends TestCase
{
  //constant
  private DelimiterResolver resolver = null;

  public DelimiterResolverTest(String name)
  {
    super(name);
  }


  /**
   * Establish a testing framework by initializing appropriate objects
   */
  public void setUp()
  {
    resolver = new DelimiterResolver();
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
    suite.addTest(new DelimiterResolverTest("initialize"));
    suite.addTest(new DelimiterResolverTest("semicolonResolveTest"));
    suite.addTest(new DelimiterResolverTest("tabResolveTest"));
    suite.addTest(new DelimiterResolverTest("hexadecimalResolveTest"));
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
   */
  public void semicolonResolveTest()
  {
    String dbDelimiter = resolver.resolve(";");
    assertTrue( dbDelimiter.equals("\\semi"));
  }

  public void tabResolveTest()
  {
    String dbDelmiter = resolver.resolve("\\t");
    assertTrue(dbDelmiter.equals("\\t"));
  }

  public void hexadecimalResolveTest()
  {
    String dbDelimiter = resolver.resolve("0x20");
    System.out.println("dbDelimiter: " + dbDelimiter);
    assertTrue(dbDelimiter.equals("0x20"));
  }

}//DataQueryActionTest


