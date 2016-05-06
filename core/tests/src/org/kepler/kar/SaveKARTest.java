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

package org.kepler.kar;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.build.project.ProjectLocator;

import ptolemy.kernel.ComponentEntity;

/**
 * Test for the ConfigurationManager
 * 
 * @author Chad Berkley
 */
public class SaveKARTest extends TestCase
{

  public SaveKARTest(String name)
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
    suite.addTest(new SaveKARTest("initialize"));
    suite.addTest(new SaveKARTest("testSaveToDisk"));
    suite.addTest(new SaveKARTest("testSaveToCache"));
    //    suite.addTest(new SaveKARTest("initialize"));
    //    suite.addTest(new SaveKARTest("initialize"));
    //    suite.addTest(new SaveKARTest("initialize"));
    return suite;
  }

  /**
   * test saving to the cache
   */
  public void testSaveToCache()
  {
    try
    {
      SaveKAR sk = new SaveKAR();
      File karFile = new File(ProjectLocator.getProjectDir()
          + "/core/target/tmp/test.kar");
      sk.setFile(karFile);

      ComponentEntity ce = new ComponentEntity();
      ce.setClassName("ptolemy.kernel.ComponentEntity");
      ce.setName("new comp enity");
      sk.addSaveInitiator(ce);
      sk.saveToCache();
      karFile.delete();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      fail("testSaveToDisk threw and unexpected exception: " + e.getMessage());
    }
  }

  /**
   * test saving to disk
   */
  public void testSaveToDisk()
  {
    try
    {
      SaveKAR sk = new SaveKAR();
      File karFile = new File(ProjectLocator.getProjectDir()
          + "/core/target/tmp/test.kar");
      sk.setFile(karFile);

      ComponentEntity ce = new ComponentEntity();
      ce.setClassName("ptolemy.kernel.ComponentEntity");
      ce.setName("new comp enity");
      sk.addSaveInitiator(ce);
      
      //WARNING - using null TableauFrame here
      sk.saveToDisk(null, null);
      assertTrue(karFile.exists());
      
      karFile.delete();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      fail("testSaveToDisk threw and unexpected exception: " + e.getMessage());
    }
  }

  /**
   * Run an initial test that always passes to check that the test harness is
   * working.
   */
  public void initialize()
  {
    assertTrue(1 == 1);
  }
}