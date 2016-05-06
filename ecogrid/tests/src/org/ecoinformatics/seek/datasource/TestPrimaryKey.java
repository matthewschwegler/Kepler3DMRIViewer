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

package org.ecoinformatics.seek.datasource;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.objectmanager.data.db.Constraint;
import org.kepler.objectmanager.data.db.PrimaryKey;
import org.kepler.objectmanager.data.db.UnWellFormedConstraintException;

/**
 * @author Jing Tao
 *
 */

public class TestPrimaryKey extends TestCase
{
  private PrimaryKey primaryKey = null;
  private String CONSTRAINTNAME = "primary_key";
  private String KEY1           = "id1";
  private String KEY2           = "id2";

  public TestPrimaryKey(String name)
  {
    super(name);
  }

  protected void setUp() throws Exception
  {
    super.setUp();
    primaryKey = new PrimaryKey();
    String constraintName = CONSTRAINTNAME;
    primaryKey.setName(constraintName);
  }

  protected void tearDown() throws Exception
  {
    primaryKey = null;
    super.tearDown();
  }

  public void testPrintStringHaveOneKey() throws UnWellFormedConstraintException
  {
    String[] keys = new String[1];
    keys[0] = KEY1;
    primaryKey.setKeys(keys);
    String expectedReturn = " "+ Constraint.CONSTRAINT + " "+CONSTRAINTNAME + " " +
                            Constraint.PRIMARYKEYSTRING + " "+"("+KEY1+")" +" ";
    String actualReturn = primaryKey.printString();
    assertEquals("return value", expectedReturn, actualReturn);

  }

  public void testPrintStringHaveTwoKeys() throws UnWellFormedConstraintException
  {
    String[] keys = new String[2];
    keys[0] = KEY1;
    keys[1] = KEY2;
    primaryKey.setKeys(keys);
    String expectedReturn = " "+ Constraint.CONSTRAINT + " "+CONSTRAINTNAME + " " +
                            Constraint.PRIMARYKEYSTRING + " "+"("+
                            KEY1 + "," + KEY2 + ")"+" ";
    String actualReturn = primaryKey.printString();
    assertEquals("return value", expectedReturn, actualReturn);

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
  * Create a suite of tests to be run together
  */
  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTest(new TestPrimaryKey("initialize"));
    suite.addTest(new TestPrimaryKey("testPrintStringHaveOneKey"));
    suite.addTest(new TestPrimaryKey("testPrintStringHaveTwoKeys"));
    return suite;
  }


}
