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

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.axis.types.URI;
import org.ecoinformatics.ecogrid.queryservice.query.ANDType;
import org.ecoinformatics.ecogrid.queryservice.query.ConditionType;
import org.ecoinformatics.ecogrid.queryservice.query.ORType;
import org.ecoinformatics.ecogrid.queryservice.query.OperatorType;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.query.QueryTypeNamespace;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;


/**
 * <p>Title: SearchScopeTest</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class SearchQueryGeneratorTest  extends TestCase
{
  private static final String EML200QUERYID = "eml200-quick-search-query";
  
  public SearchQueryGeneratorTest(String name)
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
    suite.addTest(new SearchQueryGeneratorTest("initialize"));
    suite.addTest(new SearchQueryGeneratorTest("testSearchQuery"));
    suite.addTest(new SearchQueryGeneratorTest("getEML200Query"));
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
   * test the SearchQuery class
   */
  public void testSearchQuery()
  {
    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty queryPathProp = ecogridProperty.getProperty("queryList");
    List queryList = queryPathProp.getProperties("query");
    ConfigurationProperty queryProp = (ConfigurationProperty)queryList.get(2);
    //queryProp.prettyPrint();
    SearchQuery sq = new SearchQuery(queryProp);
    
    assertTrue(sq.queryId.equals("eml210-quick-search-query"));
    assertTrue(sq.system.equals("http://knb.ecoinformatics.org"));
    assertTrue(sq.title.equals("eml210-quick-search-query"));
    String returnfield1 = (String)sq.returnField.get(1);
    assertTrue(returnfield1.equals("entityName"));
    assertTrue(sq.condition == null);
    
    //System.out.println("sq: " + sq.toString());
    Hashtable valueMap = new Hashtable();
    valueMap.put("#value#", "newValue");
    sq.replaceValues(valueMap);
    //System.out.println("sq: " + sq.toString());
    
  }

  /**
   * test the query generation for eml200query
   */
  public void getEML200Query()
  {
    String eml200QueryId = EML200QUERYID;
    String original      = "#value#";
    String replacement   = "soil";
    Hashtable map  = new Hashtable();
    map.put(original, replacement);
    try
    {
      SearchQueryGenerator eml200Query = new SearchQueryGenerator(eml200QueryId, map);
      QueryType query  = eml200Query.getQuery();
      System.out.println("query before the transform: " + query.getQueryId());


      String str = org.ecoinformatics.ecogrid.queryservice.util.EcogridQueryTransformer.toXMLString( query );

      System.err.println( "Query xml is: " );
      System.err.println( str );
      

      String id = query.getQueryId();
      System.err.println("The query id (after gnerate query) is "+ id);
      assertTrue(id.equals(EML200QUERYID));
      URI system = query.getSystem();
      System.err.println("The system (after gnerate query) is "+ system.toString());
      assertTrue(system.toString().equals("http://knb.ecoinformatics.org"));
      //QueryTypeNamespace namespace = query.getNamespace();
      QueryTypeNamespace[] namespaces = query.getNamespace();
      System.err.print("The namespaces (after gnerate query) are:");
      for (int i=0; i<namespaces.length; i++){
    	  QueryTypeNamespace namespace = namespaces[i];
    	  System.out.print(" "+namespace.toString());
    	  assertTrue(namespace.toString().equals("eml://ecoinformatics.org/eml-2.0.0"));
      }
      System.out.println();
      String[] titles = query.getTitle();
      String title  = titles[0];
      System.err.println("The title (after gnerate query) is "+ title);
      assertTrue(title.equals(EML200QUERYID));
      String[] returnFields = query.getReturnField();
      int length = returnFields.length;
      for (int i=0; i<length; i++)
      {
        System.err.println("The return fieled is " + returnFields[i]);
      }
      ANDType topAnd = query.getAND();
      
      ORType[] ANDConditionList = topAnd.getOR();

      ORType firstOR = ANDConditionList[0];
      
      ConditionType[] titleConditionList = firstOR.getCondition();

      ConditionType titleCondition       = titleConditionList[0];
      String titlePathStr = titleCondition.getConcept();
      assertTrue(titlePathStr.equals("dataset/title"));
      OperatorType opertor = titleCondition.getOperator();
      assertTrue(opertor.getValue().equals("LIKE"));
      assertTrue(titleCondition.get_value().equals(replacement));
      
      ORType[] orTypeList = topAnd.getOR();
      ORType or  = orTypeList[0];
      ConditionType[] conditionList = or.getCondition();
      int size = conditionList.length;
      for (int i=0; i<size; i++)
      {
        ConditionType cond = conditionList[0];
        String path = cond.getConcept();
        System.err.println(""+ path);
        OperatorType oper = cond.getOperator();
        System.err.println(""+oper);
        System.err.println(""+cond.get_value());
      }
      
      
    }
    catch(Exception e)
    {
      System.err.println("Error in getEML200Query " + e.getMessage());
      e.printStackTrace();
      assertTrue(1==2);
    }
  }


}//SearchQueryGeneratorTest

