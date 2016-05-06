/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
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

package test.org.kepler;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.ExecutionEngine;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.ExecutionListener;
import ptolemy.actor.Manager;

public class ExecutionEngineTest extends TestCase {
	
  private ExecutionEngine engine;
  private boolean listenerFlag = false;

  /**
   * constructor
   */
	public ExecutionEngineTest(String name) {
		super(name);
		try {
			engine = ExecutionEngine.getInstance();
		} catch (Exception e) {
			fail("could not get instance of ExecutionEngine: " + e.getMessage());
		}
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() {
    cleanup();
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
		suite.addTest(new ExecutionEngineTest("initialize"));
		suite.addTest(new ExecutionEngineTest("testRunModel"));
    suite.addTest(new ExecutionEngineTest("testRunModelMultiThread"));
		suite.addTest(new ExecutionEngineTest("testListeners"));
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
   * test single thread running of models
   */
	public void testRunModel() {
    cleanup();
		try 
    {
      engine.runModel(
        (CompositeActor)ExecutionEngine.parseMoML(
          ExecutionEngine.readFile("loader/resources/test/test-model-1.xml")));
      String testString = ExecutionEngine.readFile("loader/target/test.txt");
      if(!testString.equals("test file"))
      {
        fail("Test file not created from workflow.");
      }
		} 
    catch (Exception e) 
    {
			fail("Error running model or finding output file: " + e.getMessage());
		}
	}
  
  /**
   * test multi-thread running of models.
   */
  public void testRunModelMultiThread() {
    cleanup();
		try 
    {
      Manager m1 = engine.runModelInThread(
        (CompositeActor)ExecutionEngine.parseMoML(
          ExecutionEngine.readFile("loader/resources/test/test-model-1.xml")));
      
      Manager m2 = engine.runModelInThread(
        (CompositeActor)ExecutionEngine.parseMoML(
          ExecutionEngine.readFile("loader/resources/test/test-model-2.xml")));
      
      m1.waitForCompletion();
      String testString = ExecutionEngine.readFile("loader/target/test.txt");
      if(!testString.equals("test file"))
      {
        fail("Test file not created from workflow.");
      }
      
      m2.waitForCompletion();
      testString = ExecutionEngine.readFile("loader/target/test2.txt");
      if(!testString.equals("test file 2"))
      {
        fail("The second test file was not created.  Multi-threaded run failed.");
      }
		} 
    catch (Exception e) 
    {
			fail("Error running model or finding output file: " + e.getMessage());
		}
	}
  
  /**
   * test adding and removing execution listeners
   */
  public void testListeners() {
    ExeListener exeListener = new ExeListener();
    cleanup();
		try 
    {
      engine.addExecutionListener(exeListener);
      Manager m1 = engine.runModelInThread(
        (CompositeActor)ExecutionEngine.parseMoML(
          ExecutionEngine.readFile("loader/resources/test/test-model-1.xml")));
      
      m1.waitForCompletion();
      String testString = ExecutionEngine.readFile("loader/target/test.txt");
      if(!testString.equals("test file"))
      {
        fail("Test file not created from workflow.");
      }
      if(!listenerFlag)
      {
        fail("The listener did not function correctly.");
      }
		} 
    catch (Exception e) 
    {
			fail("Error running model or finding output file: " + e.getMessage());
		}
  }
  
  /**
   * remove temp output files
   */
  private void cleanup()
  {
    File f = new File("loader/target/test.txt");
    if(f.exists())
    {
      System.out.println("Deleting old output file: " + f.getAbsolutePath());
      f.delete();
    }
    
    f = new File("loader/target/test2.txt");
    if(f.exists())
    {
      System.out.println("Deleting old output file: " + f.getAbsolutePath());
      f.delete();
    }
  }
  
  /**
   * private listener class
   */
  private class ExeListener implements ExecutionListener
  {
    /**
     * implements executionError in ExecutionListener
     */
    public void executionError(Manager manager, Throwable throwable)
    {
      
    }
    
    /**
     * implements executionFinished in ExecutionListener
     */
    public void executionFinished(Manager manager)
    {
      listenerFlag = true;
    }
    
    /**
     * implements managerStateChanged in ExecutionListener
     */
    public void managerStateChanged(Manager manager)
    {
      
    }
  }
}