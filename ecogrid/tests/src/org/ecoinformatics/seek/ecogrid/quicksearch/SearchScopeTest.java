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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ecoinformatics.seek.ecogrid.exception.NULLSearchNamespaceException;
import org.ecoinformatics.seek.ecogrid.exception.NoMetadataSpecificationClassException;
import org.ecoinformatics.seek.ecogrid.exception.NoSearchEndPointException;

/**
 * <p>Title: SearchScopeTest</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class SearchScopeTest  extends TestCase
{
  private static final String ENDPOINT  = "http://dev.nceas.ucsb.edu";
  private static final String NAMESPACE = "eml://ecoinformatics.org/eml-2.0.0";
  private static final String CLASSNAME =
                   "org.ecoinformatics.seek.datasource.eml.eml2.EML2MetadataSpecification";
  String namespace = null;
  Vector endpoints = null;
  SearchScope searchScope = null;

  public SearchScopeTest(String name)
  {
    super(name);
    namespace = NAMESPACE;
    endpoints = new Vector();
    endpoints.addElement(ENDPOINT);

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
    suite.addTest(new SearchScopeTest("initialize"));
    suite.addTest(new SearchScopeTest("constructorThrowNULLSearchNamespace"));
    suite.addTest(new SearchScopeTest("constructorThrowNoMetadataClass"));
    suite.addTest(new SearchScopeTest("constructorThrowNoEndPoint"));
    suite.addTest(new SearchScopeTest("getNamespaceTest"));
    suite.addTest(new SearchScopeTest("getMetadataSpecificationTest"));
    suite.addTest(new SearchScopeTest("getEndPointsTest"));
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
   * Method to testing construct to throw a null search name space exception
   */
  public void constructorThrowNULLSearchNamespace()
  {
    boolean flag = false;
    namespace = null;
    try
    {
      searchScope = new SearchScope(namespace, CLASSNAME, endpoints);
    }
    catch (Exception e)
    {
      if (e instanceof NULLSearchNamespaceException)
      {
        flag = true;
      }
    }
    assertTrue(flag);
  }

 

  /**
  * Method to testing construct to throw a null metadata specification
  */
 public void constructorThrowNoMetadataClass()
 {
   boolean flag = false;
   namespace = "hello";
   String className = "helloClass";
   try
   {
     searchScope = new SearchScope(namespace, className, endpoints);
   }
   catch (Exception e)
   {

     if (e instanceof NoMetadataSpecificationClassException)
     {
       flag = true;
     }
   }
   assertTrue(flag);
 }

 /**
 * Method to testing construct to throw a no end points exception
 */
public void constructorThrowNoEndPoint()
{
  boolean flag = false;
  endpoints = null;
  try
  {
    searchScope = new SearchScope(namespace, CLASSNAME, endpoints);
  }
  catch (Exception e)
  {
    if (e instanceof NoSearchEndPointException)
    {
      flag = true;
    }
  }
  assertTrue(flag);
}


 /**
  * Method to testing getNamespace method
  */
 public void getNamespaceTest()
 {
   boolean flag = false;
   try
   {
     initInstance();
   }
   catch (Exception e)
   {
     flag = false;
   }
   if ( searchScope.getNamespace().equals(NAMESPACE))
   {
     flag = true;
   }
   assertTrue(flag);
 }

 /**
  * Test the getMetadataSpecification method
  */
 public void getMetadataSpecificationTest()
 {
   boolean flag = false;
   try
  {
    initInstance();
  }
  catch (Exception e)
  {
    flag = false;

  }
  if ( searchScope.getMetadataSpecification().
       getClass().getName().equals(CLASSNAME))
  {
    flag = true;
  }
  assertTrue(flag);
 }

 /**
 * Test the getEndPonts method
 */
public void getEndPointsTest()
{
  boolean flag = false;
  try
 {
   initInstance();
 }
 catch (Exception e)
 {
   flag = false;
 }
 Vector url = searchScope.getEndPoints();
 String urlString = (String)url.elementAt(0);
 if (urlString.equals(ENDPOINT))
 {
   flag = true;
 }
 assertTrue(flag);
}


 private void initInstance()throws Exception
 {
   try
  {
   searchScope = new SearchScope(namespace, CLASSNAME, endpoints);
  }
  catch (Exception e)
  {
    throw e;
  }
 }


}
