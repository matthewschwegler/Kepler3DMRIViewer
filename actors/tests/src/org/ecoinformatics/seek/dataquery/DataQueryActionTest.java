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

/**
 *  '$RCSfile$'
 *  Copyright: 2000 Regents of the University of California and the
 *              National Center for Ecological Analysis and Synthesis
 *    Authors: Jing Tao
 *    Release: @release@
 *
 *   '$Author: welker $'
 *     '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $'
 * '$Revision: 24234 $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.ecoinformatics.seek.dataquery;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Hashtable;
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ecoinformatics.seek.datasource.eml.eml2.Eml200Parser;
import org.kepler.build.project.ProjectLocator;
import org.kepler.objectmanager.data.db.Entity;
import org.kepler.util.DotKeplerManager;
import org.xml.sax.InputSource;


/**
 * A test class for query action.
 * Notes: 1) we need to modified eml-sample.xml in <url> element from @ptolemydir@/sample.dat
 *          file:///home/tao/project/ptII4.0.1/sample.dat.
 *        2) we need set up "textdb.allow_full_path=true" in hsql proertie file
 */

public class DataQueryActionTest extends TestCase {
  //constant
  private final static String DBPATH = DotKeplerManager.getInstance()
                                       .getCacheDirString() + "cachedata";
  private final static String EMLFILEPATH = ProjectLocator.getProjectDir() + "/actors/lib/testdata/eml/eml-sample.xml";
  private final static String DATAFILEPATH = "sample.dat";
  private final static String ORIGINALDATA = ProjectLocator.getProjectDir() + "/actors/lib/testdata/eml/sample.dat";
  private final static String COMPLEXEML =
    ProjectLocator.getProjectDir() + "/actors/lib/testdata/eml/eml-complex-dataformat.xml";
  private final static String COMPLEXDATA = ProjectLocator.getProjectDir() + "/actors/lib/testdata/eml/complex.dat";
  private Eml200Parser parser = new Eml200Parser();
  private Entity entity = null;
  public DataQueryActionTest(String name) {
    super(name);
  }


  /**
   * Establish a testing framework by initializing appropriate objects
   */
  public void setUp() {

  }


  /**
   * Release any objects after tests are complete
   */
  public void tearDown() {
  }


  /**
   * Create a suite of tests to be run together
   *
   * @return Test
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new DataQueryActionTest("initialize"));
    suite.addTest(new DataQueryActionTest("actionPerformedNontextTableTest"));
    suite.addTest(new DataQueryActionTest(
      "actionPerformedRefreshNontextTableTest"));
    suite.addTest(new DataQueryActionTest("actionPerformedTextTableTest"));
    suite.addTest(new DataQueryActionTest("actionPerformedRefreshTextTableTest"));
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
   * A test for main method actionPerformed base on non-textTablein DataQueryAction
   */
  public void actionPerformedNontextTableTest() {
    queryActionToNontextTable(false);
  } //actionPerformedNontextTableTest


  /**
   * A test for main method actionPerformed base on non-textTablein DataQueryAction
   */
  public void actionPerformedRefreshNontextTableTest() {
    queryActionToNontextTable(true);
  } //actionPerformedNontextTableTest


  /*
   * method to query a not text table (by refresh or non=refresh)
   */
  private void queryActionToNontextTable(boolean refresh) {
    // create a hsql table and insert data
    boolean success = false;
    try {
      // get entity
      System.out.println("reading " + COMPLEXEML);
      Reader reader = new InputStreamReader(new FileInputStream(COMPLEXEML));
      InputSource source = new InputSource(reader);
      parser.parse(source);
      Hashtable entityhash = parser.getEntityHash();
      entity = (Entity)entityhash.get(entityhash.keys()
                                      .nextElement());
      InputStream dataStream = getComplexData();
      //figure out the table name
      DBTableNameResolver nameResolver = new DBTableNameResolver();
      entity = nameResolver.resolveTableName(entity);
      // generate table in another thread
      DBTablesGenerator tableGenerator = null;
      if (refresh == false) {
        tableGenerator = new DBTablesGenerator(entity, dataStream);
      } else {
        tableGenerator = new DBTablesGenerator(entity, dataStream, refresh);
      }
      Thread generateTableThread = new Thread(tableGenerator);
      generateTableThread.start();
      while (!tableGenerator.getIsDone()) {
        System.out.println("Waiting generate table is done");
      }
      success = tableGenerator.getSuccessStatus();
      String tableName = entity.getDBTableName();
      String sql = "SELECT * FROM " + tableName;
      System.out.println("The sql will be run is " + sql);
      HsqlDataQueryAction query = new HsqlDataQueryAction();
      query.setSQL(sql);
      query.setPrintResultset(true);
      query.actionPerformed(null);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Eror to create tables in db " + e.getMessage());
    }
    if (!success) {
      fail("tableGenerator.getSuccessStatus() returned FALSE (but no exception was generated) ");
    }
  }


  private InputStream getComplexData() throws Exception {
    return DataQueryActionTest.class.getResourceAsStream(COMPLEXDATA);
  }


  /**
   * A test for main method actionPerformed base on non-textTablein DataQueryAction
   * without refresh data
   */
  public void actionPerformedTextTableTest() {
    queryActionOnTextTable(false);

  } //actionPerformedNontextTableTest


  /**
   * A test for main method actionPerformed base on non-textTablein DataQueryAction
   * with refresh data
   */
  public void actionPerformedRefreshTextTableTest() {
    queryActionOnTextTable(true);

  } //actionPerformedNontextTableTest


  private void queryActionOnTextTable(boolean refresh) {

    boolean success = false;
// parse the eml document and get entity info
    Reader reader = null;

    // create a hsql table and insert data
    try {
      System.out.println("reading file " + EMLFILEPATH);
      reader = new InputStreamReader(new FileInputStream(EMLFILEPATH));
      InputSource source = new InputSource(reader);
      parser.parse(source);
      prepareData();
      Hashtable entityhash = parser.getEntityHash();
      entity = (Entity)entityhash.get(entityhash.keys()
                                      .nextElement());
      DBTableNameResolver nameResolver = new DBTableNameResolver();
      entity = nameResolver.resolveTableName(entity);
      // generate table in another thread
      DBTablesGenerator tableGenerator = null;
      if (refresh == false) {
        tableGenerator = new DBTablesGenerator(entity, DATAFILEPATH);
      } else {
        tableGenerator = new DBTablesGenerator(entity, DATAFILEPATH, refresh);
      }
      Thread generateTableThread = new Thread(tableGenerator);
      generateTableThread.start();
      //waiting table is done
      while (!tableGenerator.getIsDone()) {
        System.out.println("Waiting generate table is done");
      }
      success = tableGenerator.getSuccessStatus();
      String tableName = entity.getDBTableName();
      String sql = "SELECT * FROM " + tableName;
      System.out.println("The sql will be run is " + sql);
      HsqlDataQueryAction query = new HsqlDataQueryAction();
      query.setPrintResultset(true);
      query.setSQL(sql);
      query.actionPerformed(null);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Eror creating tables in db " + e.getMessage());
    }
    if (!success) {
      fail("Error creating tables.");
    }

  }


  /**
   * Creates the directories for storing the cache (if necessary)
   * @return the path to the cache
   */
  private void prepareData() throws Exception {
    //create cache dir if necessary
    StringTokenizer st = new StringTokenizer(DBPATH, File.separator);
    StringBuffer dirPath = new StringBuffer();
    while (st.hasMoreTokens()) {
      String dirName = st.nextToken();
      if (dirName.length() > 0) {
        if (dirPath.length() >= 0) {
          dirPath.append(File.separator);
        }
        dirPath.append(dirName);
        File file = new File(dirPath.toString());
        if (!file.exists()) {
          file.mkdir();
        }
      }
    }
    // copy the original data file to cache dir
    Reader reader = new InputStreamReader(new FileInputStream(ORIGINALDATA));
    FileWriter writer = new FileWriter(new File(DBPATH + File.separator
                                                + DATAFILEPATH));
    char[] buffer = new char[1000];
    int charNumber = 0;
    charNumber = reader.read(buffer);
    while (charNumber != -1) {
      //System.out.println("The read number is "+charNumber);
      writer.write(buffer, 0, charNumber);
      charNumber = reader.read(buffer);
      //buffer = new char[1000];
    }
    reader.close();
    writer.close();
  }

} //DataQueryActionTest

