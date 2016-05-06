/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-12-07 15:35:50 -0800 (Tue, 07 Dec 2010) $' 
 * '$Revision: 26427 $'
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

package org.ecoinformatics.seek.ecogrid;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.axis.types.URI;
import org.ecoinformatics.ecogrid.queryservice.query.ANDType;
import org.ecoinformatics.ecogrid.queryservice.query.ConditionType;
import org.ecoinformatics.ecogrid.queryservice.query.OperatorType;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.query.QueryTypeNamespace;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecord;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecordReturnField;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadata;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataNamespace;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructure;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructureReturnField;
import org.ecoinformatics.seek.datasource.eml.eml2.EML2MetadataSpecification;
import org.ecoinformatics.seek.ecogrid.quicksearch.ResultRecord;
import org.ecoinformatics.seek.ecogrid.quicksearch.ResultRecordDetail;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.StringAttribute;

/**
 * A JUnit test for testing Step class processing
 */
public class EML2MetadataSpecificationTest extends TestCase
{
  EML2MetadataSpecification eml2Metadata;

  private static final String SEARCHVALUE = "value";
  private static final String ID1         = "foo.1.1";
  private static final String ID2         = "foo.2.1";
  private static final String TITLE1      = "soil1";
  private static final String TITLE2      = "soil2";
  private static final String ENTITY11    = "entity11";
  private static final String ENTITY12    = "entity12";
  private static final String ENTITY21    = "entity21";
  private static final String ENTITY22    = "entity22";
  private static final String ENDPOINT    = "http://dev.nceas.ucsb.edu";

  /**
   * Constructor to build the test
   *
   * @param name the name of the test method
   */
  public EML2MetadataSpecificationTest(String name)
  {
    super(name);
    eml2Metadata = new EML2MetadataSpecification();

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
    suite.addTest(new EML2MetadataSpecificationTest("initialize"));
    suite.addTest(
         new EML2MetadataSpecificationTest("getQuickSearchEcoGridQueryTest"));
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
   * The test for method getQuickSearchEcoGridQuery. This method will generate
   * a ecogrid query for quick search
   */
  public void getQuickSearchEcoGridQueryTest()
  {
    try
    {
       QueryType query = eml2Metadata.getQuickSearchEcoGridQuery(SEARCHVALUE);

       String str = org.ecoinformatics.ecogrid.queryservice.util.EcogridQueryTransformer.toXMLString( query );
       System.err.println("Query is: ");
       System.err.println( str );

       String[] tiles = query.getTitle();
       assertTrue(tiles[0].equals("eml200-quick-search-query"));
       System.out.print("The namespaces are:");
       QueryTypeNamespace[] namespaces = query.getNamespace();
       for (int i=0; i<namespaces.length; i++){
    	   QueryTypeNamespace namespace = namespaces[i];
    	   System.out.print(" "+namespace.get_value());
    	   assertTrue(
    			   (namespace.toString()).trim().equals("eml://ecoinformatics.org/eml-2.0.0"));
       }
       System.out.println();
       String [] returnFields  = query.getReturnField();
       assertTrue(returnFields[0].equals("dataset/title"));
       assertTrue(returnFields[1].equals("entityName"));
       ANDType and = query.getAND();

       ConditionType[] conditionList = and.getOR()[0].getCondition();
       ConditionType condition = conditionList[0];
       OperatorType operator = condition.getOperator();
       String operatorStr = operator.getValue();
       assertTrue(operatorStr.equals("LIKE"));
       String expressionString = condition.getConcept();
       assertTrue(expressionString.equals("dataset/title"));
       String searchValue = condition.get_value();
       assertTrue(searchValue.equals(SEARCHVALUE));


    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("Error getQuickSearchEcoGridQueryTest: " + e.getMessage());
    }
  }

  /**
   *  The method to test transformResultset method. The original method will
   *  transform ecogrid resultset to ResultRecord object array which will be
   *  used in kepler
   */
   public void  transformResultsetTest()
   {
     try
     {
       ResultsetType resultType = generateResultSet();
       CompositeEntity resultsRoot = new CompositeEntity();
       resultsRoot.setName("resultset");
       ResultRecord[] resultRecord = eml2Metadata.transformResultset(
           resultType, ENDPOINT, resultsRoot);
       // compare record 1
       ResultRecord record1 = resultRecord[0];
       String name1 = record1.getName();
       assertTrue(name1.equals(TITLE1));
       StringAttribute idAttr1 = (StringAttribute) record1.getAttribute(
           "recordId");
       String id1 = idAttr1.getExpression();
       assertTrue(id1.equals(ID1));
       ResultRecordDetail tetail11 = (ResultRecordDetail)
           record1.getAttribute(ENTITY11);
       assertTrue(tetail11.getName().equals(ENTITY11));
       

       
     }
     catch (Exception e)
     {
       e.printStackTrace();
       fail("Error getQuickSearchEcoGridQueryTest: " + e.getMessage());
     }

   }

   /*
    * Method to generate a resultset object
    */
   private ResultsetType generateResultSet() throws Exception
   {
      ResultsetType result = new ResultsetType();
      // set system
      result.setSystem(new URI("http://knb.ecoinformatics.org")); 
      // set resultsetId
      result.setResultsetId("eml.001");
      // set meta data
      ResultsetTypeResultsetMetadata metadata = 
                                         new ResultsetTypeResultsetMetadata();
      //get the size of metacat hashtable
      int size = 2;
      // if size is zero, it start 0
      metadata.setStartRecord(1);
      metadata.setEndRecord(size);
      metadata.setRecordCount(size);
      ResultsetTypeResultsetMetadataNamespace[] namespaces = new ResultsetTypeResultsetMetadataNamespace[1];
      ResultsetTypeResultsetMetadataNamespace namespace = new ResultsetTypeResultsetMetadataNamespace();
      namespace.set_value("eml://ecoinformatics.org/eml-2.0.0");
      namespaces[0] = namespace;
      //metadata.setNamespace(namespace);
      metadata.setNamespace(namespaces);
      
      ResultsetTypeResultsetMetadataRecordStructureReturnField[] array = 
              new ResultsetTypeResultsetMetadataRecordStructureReturnField[2];
      ResultsetTypeResultsetMetadataRecordStructureReturnField field1 = new ResultsetTypeResultsetMetadataRecordStructureReturnField();
      field1.setId("f1");
      field1.setName("dataset/title");
      ResultsetTypeResultsetMetadataRecordStructureReturnField field2 = new ResultsetTypeResultsetMetadataRecordStructureReturnField();
      field2.setId("f2");
      field1.setName("entityName");
      array[0]=field1;
      array[1]=field2;
      ResultsetTypeResultsetMetadataRecordStructure structure = new ResultsetTypeResultsetMetadataRecordStructure();
      structure.setReturnField(array);
      metadata.setRecordStructure(structure);
      result.setResultsetMetadata(metadata);
      
      // ---------handle record 
      // create a AnyRecordArray
      ResultsetTypeRecord[] recordList = new ResultsetTypeRecord[1];
      ResultsetTypeRecord record1 = new ResultsetTypeRecord();
      ResultsetTypeRecordReturnField[] array2= new ResultsetTypeRecordReturnField[2];
      ResultsetTypeRecordReturnField return1 = new ResultsetTypeRecordReturnField();
      return1.setId("f1");
      return1.set_value(TITLE1);
      ResultsetTypeRecordReturnField return2 = new ResultsetTypeRecordReturnField();
      return1.setId("f2");
      return1.set_value(ENTITY11);
      array2[0]=return1;
      array2[1]=return2;
      record1.setReturnField(array2);
      recordList[0]=record1;
     
      // set record array to ecogrid result objec
      result.setRecord(recordList);
     
     return result;
   }


}
