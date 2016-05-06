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

package test.workflows;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import junit.framework.TestCase;
import ptolemy.actor.gui.MoMLSimpleApplication;


/**
 * <p>basic test that executes MOML workflows using MoMLSimpleApplication</p>
 *
 * @author Matthew Brooke
 * @version 1.0
 */
public class WorkflowExecutionTest extends TestCase {

  private String wfName;

  private final String X11Msg
    = "This workflow is attempting to create visual elements."
      + "This workflow may be executing correctly, but momlexecute "
      + "cannot find an X Server to use.  DISPLAY is "
      + System.getProperty("DISPLAY");

  /**
   * Constructor
   *
   * @param methodName - the name of the test method to run.
   * Should be "testWorkflow" unless you have a very good reason to change it.
   */
  public WorkflowExecutionTest(String methodName) {
    super(methodName);
    assertTrue(
      "INCORRECT METHOD NAME RECEIVED - SHOULD BE \"testWorkflow\", but was: "
      + methodName,
      "testWorkflow".equals(methodName));
  }


  protected void setUp() throws Exception {
    super.setUp();
  }


  protected void tearDown() throws Exception {
    super.tearDown();
  }


  /**
   * set full path to test workflow
   *
   * @param workflowFullPath - the filename String of the workflow to run.
   *                       Full path is required; example:
   *          /Users/brooke/dev/KEPLER/kepler/workflows/test/AddAscGridsTest.xml
   */
  public void setWorkflowName(String workflowFullPath) {

    wfName = workflowFullPath;
  }


  public void testWorkflow() throws IOException {

    if(wfName == null)
    { //we are executing in the test suite and not doing a workflow test
      return;
    }
    assertNotNull("wfName is NULL ", wfName);
    assertTrue("wfName is EMPTY ", !wfName.trim().equals(""));

    File wfFile = new File(wfName);
    assertTrue("Workflow file does not exist: " + wfName, wfFile.exists());
    assertTrue("Workflow file exists but cannot be read: " + wfName,
               wfFile.canRead());
    wfFile = null;

    System.out.println("WorkflowExecutionTest - Testing Workflow: " + wfName);

    ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
    PrintStream psOut = new PrintStream(baosOut);
    PrintStream origOut = System.out;
    PrintStream origErr = System.err;
    System.setOut(psOut);
    System.setErr(psOut); //redirect err to out
    try {
      new MoMLSimpleApplication(wfName);
    } catch (Throwable ex) {
      ex.printStackTrace();
    } finally {
      psOut.flush();
      psOut.close();
      baosOut.flush();
      baosOut.close();
      System.setOut(origOut);
      System.setErr(origErr);
    }

    String outputStr = baosOut.toString();

    System.out.println("** WorkflowExecutionTest - Test Output for Workflow: "
                       + wfName
                       + "\n-------------\n"
                       + outputStr
                       + "\n-------------\n");

    boolean hasExc = ( (outputStr.indexOf("Exception") >= 0)
                      && (outputStr.indexOf("ptII.properties") < 0));

   System.out.println(
     (hasExc) ? "*** EXCEPTION FOUND! workflow: " + wfName
     + " ***" : "--- no exceptions in workflow: " + wfName + " ---");

    assertTrue(X11Msg + "\n Workflow: " + wfName + "\n Output: \n" + outputStr,
               outputStr.indexOf("InternalError") < 0);

    assertTrue("Output contains an exception for workflow: " + wfName
               + "\nOutput was: \n" + outputStr, !hasExc);

  }
}