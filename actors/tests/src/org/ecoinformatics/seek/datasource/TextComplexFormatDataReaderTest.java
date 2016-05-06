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

package org.ecoinformatics.seek.datasource;


import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.build.project.ProjectLocator;
import org.kepler.objectmanager.data.db.Attribute;
import org.kepler.objectmanager.data.db.AttributeList;
import org.kepler.objectmanager.data.db.Entity;
import org.kepler.objectmanager.data.text.TextComplexDataFormat;
import org.kepler.objectmanager.data.text.TextComplexFormatDataReader;
import org.kepler.objectmanager.data.text.TextDelimitedDataFormat;
import org.kepler.objectmanager.data.text.TextWidthFixedDataFormat;


/**
 * @author tao
 * JUnit test case for TextComplexFormatDataReader
 */
public class TextComplexFormatDataReaderTest extends TestCase {
  private static String DATAFILEPATH = ProjectLocator.getProjectDir() + "/actors/lib/testdata/eml/complex.dat";

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
  }


  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }


  /**
   * Constructor for TextComplexFormatDataReaderTest.
   * @param arg0
   */
  public TextComplexFormatDataReaderTest(String arg0) {
    super(arg0);
  }


  /**
   * Create a suite of tests to be run together
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new TextComplexFormatDataReaderTest("initialize"));
    suite.addTest(new TextComplexFormatDataReaderTest(
      "testGetRowDataVectorFromStream"));
    return suite;
  }


  /**
   * Run an initial test that always passes to check that the test
   * harness is working.
   */
  public void initialize() {
    assertTrue(1 == 1);
  }


  /**
   * To test get row data vector method
   *
   */
  public final void testGetRowDataVectorFromStream() throws Exception {
    String id = "100";
    String name = "complexDataFormatTable";
    String description = "For testing";
    Vector attributeListVector = new Vector();
    for (int i = 0; i < 7; i++) {
      String idAtt = "" + i;
      String nameAtt = "name " + i;
      String type = "String";
      Attribute att = new Attribute(idAtt, nameAtt, type);
      attributeListVector.add(att);

    }
    AttributeList attributeList = new AttributeList();
    attributeList.setAttributes(attributeListVector);
    Entity entity = new Entity(id, name, description, attributeList);
    String physcialLineDelimiter = "\n";
    entity.setPhysicalLineDelimiter(physcialLineDelimiter);
    TextComplexDataFormat[] formatList = new TextComplexDataFormat[7];
    TextWidthFixedDataFormat format0 = new TextWidthFixedDataFormat(1);
    format0.setFieldStartColumn(2);
    formatList[0] = format0;
    TextDelimitedDataFormat format1 = new TextDelimitedDataFormat(",");
    formatList[1] = format1;
    TextWidthFixedDataFormat format2 = new TextWidthFixedDataFormat(2);
    formatList[2] = format2;
    TextWidthFixedDataFormat format3 = new TextWidthFixedDataFormat(2);
    formatList[3] = format3;
    TextDelimitedDataFormat format4 = new TextDelimitedDataFormat("#");
    formatList[4] = format4;
    TextDelimitedDataFormat format5 = new TextDelimitedDataFormat(";");
    formatList[5] = format5;
    TextWidthFixedDataFormat format6 = new TextWidthFixedDataFormat(2);
    formatList[6] = format6;
    entity.setDataFormatArray(formatList);
    entity.setNumHeaderLines(1);
    InputStream dataStream = getDataStream();
    TextComplexFormatDataReader reader =
      new TextComplexFormatDataReader(dataStream, entity);
    Vector rowVector = reader.getRowDataVectorFromStream();
    int index = 0;
    String value = (String)rowVector.elementAt(index);
    assertTrue(value.equals("2"));
    index = 5;
    value = (String)rowVector.elementAt(index);
    assertTrue(value.equals("34"));
    index = 6;
    value = (String)rowVector.elementAt(index);
    assertTrue(value.equals("23"));

    rowVector = reader.getRowDataVectorFromStream();
    index = 0;
    value = (String)rowVector.elementAt(index);
    assertTrue(value.equals("4"));
    index = 5;
    value = (String)rowVector.elementAt(index);
    assertTrue(value.equals("56"));
    index = 6;
    value = (String)rowVector.elementAt(index);
    assertTrue(value.equals("34"));

    rowVector = reader.getRowDataVectorFromStream();
    index = 0;
    value = (String)rowVector.elementAt(index);
    assertTrue(value.equals("3"));
    index = 5;
    value = (String)rowVector.elementAt(index);
    assertTrue(value.equals(""));
    index = 6;
    value = (String)rowVector.elementAt(index);
    assertTrue(value.equals(""));
  }


  /*
   * Method to get input stream from data file lib/testdata/eml/complex.dat
   */
  private InputStream getDataStream() throws Exception {
    return new FileInputStream(DATAFILEPATH);
  }
}