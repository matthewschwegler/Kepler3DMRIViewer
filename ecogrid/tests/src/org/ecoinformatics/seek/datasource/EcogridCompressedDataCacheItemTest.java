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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ecoinformatics.seek.dataquery.DelimiterResolver;




/**
 * This class will test EcogridCompressedDataCacheItem object
 * Note: make sure there is a zip file name 1009.dat in the cache dir and 1009.dat
 * has three zip entries: query1.txt, query.txt and query.data
 */

public class EcogridCompressedDataCacheItemTest extends TestCase
{
  //constant
  private DelimiterResolver resolver = null;
  private static final String fileName = "1009.dat";

  public EcogridCompressedDataCacheItemTest(String name)
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
    suite.addTest(new EcogridCompressedDataCacheItemTest("initialize"));
    suite.addTest(new EcogridCompressedDataCacheItemTest("getUnzippedFilePathTest"));
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
  public void getUnzippedFilePathTest()
  {
      //String path =Util.DBPATH+"unzip"+File.separator+fileName+File.separator;
      //String LOCALFILENAME = Util.DBPATH +fileName;
    try
    {
      /*EcogridCompressedDataCacheItem item = new EcogridCompressedDataCacheItem(false);
      item.setLocalFileName(LOCALFILENAME);
      item.unzipCacheItem();
      String[] txtList  = item.getUnzippedFilePath("txt");
      System.out.println("The txt file in list is "+txtList[0]);
      System.out.println("The txt file in list is "+txtList[1]);
      assertTrue(txtList[0].equals(path+"query.txt")||txtList[0].equals(path+"query1.txt"));
      assertTrue(txtList[1].equals(path+"query.txt")||txtList[1].equals(path+"query1.txt"));
      String[] dataList = item.getUnzippedFilePath("data");
      System.out.println("The data file in list is "+dataList[0]);
      assertTrue(dataList[0].equals(path+"query.data"));*/
    }
    catch(Exception e)
    {
      System.out.println("error is "+ e.getMessage());
      assertTrue(1==2);
    }
  }

 

}//DataQueryActionTest


