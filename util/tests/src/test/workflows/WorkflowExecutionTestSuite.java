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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * <p>Test Suite that creates a test for each of the MOML workflows named in
 * the comma-delimited list contained in the environment variable "-DWF_LIST=".
 * This list is typically created and passed by ant
 *
 * @author Matthew Brooke
 * @version 1.0
 */
public class WorkflowExecutionTestSuite extends TestCase {

  private static String wfList;

  public WorkflowExecutionTestSuite(String s) {
    super(s);
  }


  public static Test suite() {


    wfList = System.getProperty("WF_LIST");

    if(wfList == null)
    { 
      TestSuite suite = new TestSuite();
      return suite;
    }
    
    assertNotNull("workflow list (from JVM arg \"-DWF_LIST=\") is NULL ",
                  wfList);

    assertTrue("workflow list (from JVM arg \"-DWF_LIST=\") is EMPTY ",
               !wfList.trim().equals(""));

    // open the workflows list file, parse it and get the list of workflows
    HashSet wfSet = new HashSet();
    FileReader r = null;
    try {
	System.out.println("Trying to read file: " + wfList);
        r = new FileReader(wfList);
    } catch (FileNotFoundException fnfe) {
        fail("Actor list file not found while trying to read workflow tests.");
    }

    try {
        BufferedReader br = new BufferedReader(r);
        String line = "";
        while (line != null) {
            line = br.readLine();
	    if (line != null && !line.startsWith("#")) {
	        String[] actorInfo = line.split(",");
		for (int i = 4; i < actorInfo.length; i++) {
			wfSet.add(actorInfo[i]);
		}
            }
        }
    } catch (IOException ioe) {
        fail("Error while trying to read workflow tests." + ioe.getMessage());
    }
    

    assertNotNull("workflows set (read from workflows list) is NULL ",
                  wfSet);
    assertTrue("workflows set (read from workflows list) is EMPTY ",
               !wfSet.isEmpty());

    TestSuite suite = new TestSuite();

    Iterator it = wfSet.iterator();
    while (it.hasNext()) {
      String wf = (String)it.next();
      if (!wf.equals("NA") && !wf.equals("none")) {
          System.out.println("Adding Test to Suite for Workflow: " + wf);
          WorkflowExecutionTest nextTest = new WorkflowExecutionTest("testWorkflow");
          nextTest.setWorkflowName(wf);
          suite.addTest(nextTest);
      }
    }
    return suite;
  }
}