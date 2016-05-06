package org.ecoinformatics.seek.querybuilder;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.objectmanager.data.db.DSSchemaIFace;


/**
 * <p>Title: SearchScopeTest</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class DBSchemaParserEmitterTest  extends TestCase
{
 
  public DBSchemaParserEmitterTest(String name)
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
    suite.addTest(new DBSchemaParserEmitterTest("initialize"));
    suite.addTest(new DBSchemaParserEmitterTest("parserWithoutMissingValueTest"));
    suite.addTest(new DBSchemaParserEmitterTest("parserWithMissingValueTest"));
    return suite;
  }

  /**
   * Run an initial test that always passes to check that the test
   * harness is working.
   */
  public void initialize()
  {
    assertTrue(1 == 1);
  }//initialize
  
  /**
   * Test adding a service into controller
   */
  public void parserWithoutMissingValueTest()
  {
	StringBuffer buffer = new StringBuffer();
	buffer.append("<schema>");
	buffer.append("<table name=\"test\">\n");
	buffer.append("  <field name=\"col1\" dataType=\"int\"/>");
	buffer.append("  <field name=\"col2\" dataType=\"double\"/>");
	buffer.append("</table>");
	buffer.append("</schema>");
	String schemaStr = buffer.toString();
	System.out.println("the schema is\n"+schemaStr);
	DSSchemaIFace schema = DBSchemaParserEmitter.parseSchemaDef(schemaStr);
	String result = DBSchemaParserEmitter.emitXML(schema);
	System.out.println("the reuslt is \n"+result);
	assertTrue(1==1);
  }//addServiceTest
  
  /**
   * Test adding a service into controller
   */
  public void parserWithMissingValueTest()
  {
	StringBuffer buffer = new StringBuffer();
	buffer.append("<schema>");
	buffer.append("<table name=\"test\">\n");
	buffer.append("  <field name=\"col1\" dataType=\"int\">");
	buffer.append("<missingValueCodeList>");
	buffer.append("<missingValueCode>");
	buffer.append("9999");
	buffer.append("</missingValueCode>");
	buffer.append("</missingValueCodeList>");
	buffer.append("</field>");
	buffer.append("  <field name=\"col2\" dataType=\"double\">");
	buffer.append("<missingValueCodeList>");
	buffer.append("<missingValueCode>");
	buffer.append("9999");
	buffer.append("</missingValueCode>");
	buffer.append("<missingValueCode>");
	buffer.append("-");
	buffer.append("</missingValueCode>");
	buffer.append("</missingValueCodeList>");
	buffer.append("</field>");
	buffer.append("</table>");
	buffer.append("</schema>");
	String schemaStr = buffer.toString();
	System.out.println("the schema is\n"+schemaStr);
	DSSchemaIFace schema = DBSchemaParserEmitter.parseSchemaDef(schemaStr);
	String result = DBSchemaParserEmitter.emitXML(schema);
	System.out.println("the reuslt is \n"+result);
	assertTrue(1==1);
  }//addServiceTest

}//EcoGridQueryServicesControllerTest<
