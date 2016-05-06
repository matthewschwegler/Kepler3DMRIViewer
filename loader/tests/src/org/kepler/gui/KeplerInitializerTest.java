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

package org.kepler.gui;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Simple JUnit test which executes the org.kepler.moml.StartupInitialization routines.
 * It will execute all the steps defined in the config.xml file -- creating directories
 * and initializing db schema.
 * 
 * This test is rather unusual in that other tests (any test which uses the db)
 * requires this test to be executed and succeed first.  This is controlled externally
 * by the build.xml test task.
 * 
 * @author Kevin Ruland
 *
 */
public class KeplerInitializerTest extends TestCase {

	  public KeplerInitializerTest(String name)
	  {
	    super(name);
	  }

	  public void initialize()
	  {
		  try {
			  new KeplerInitializer();
		  }
		  catch (Exception e) {
			  System.err.println("Exception occurred during KeplerInitializer");
			  e.printStackTrace();
			  fail();
		  }
	  }

	  /**
	   * Create a suite of tests to be run together
	   */
	  public static Test suite()
	  {
	    TestSuite suite = new TestSuite();
	    suite.addTest(new KeplerInitializerTest("initialize"));
	    return suite;
	  }
}
