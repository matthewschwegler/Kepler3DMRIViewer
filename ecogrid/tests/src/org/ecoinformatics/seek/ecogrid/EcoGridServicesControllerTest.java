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

package org.ecoinformatics.seek.ecogrid;

import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.authentication.Domain;


/**
 * <p>Title: EcoGridServicesControllerTest</p>
 * <p>Description: Tests the EcoGridServicesController methods </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class EcoGridServicesControllerTest  extends TestCase
{
  private EcoGridServicesController controller = EcoGridServicesController.getInstance();
  
  private static Log log = LogFactory.getLog(EcoGridServicesControllerTest.class);
  
  public EcoGridServicesControllerTest(String name)
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
    suite.addTest(new EcoGridServicesControllerTest("initialize"));
    suite.addTest(new EcoGridServicesControllerTest("getDomain"));
    //suite.addTest(new EcoGridServicesControllerTest("addServiceTest"));
    //suite.addTest(new EcoGridServicesControllerTest("updateServiceTest"));
    //suite.addTest(new EcoGridServicesControllerTest("removeServiceTest"));
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
  
  
  public void getQueryServices() {
	  try {
		  Vector services = controller.getQueryServicesList();///(EcoGridServicesController.QUERYSERVICETYPE);
		  //Vector services = controller.getServicesList(EcoGridServicesController.QUERYSERVICETYPE);
		  assertNotNull(services);
		  assertTrue(services.size() > 0);
	  }
	  catch (Exception e) {
		  e.printStackTrace();
		  fail("problem getting services");
	}
  }
  
  public void getDomain() {
	  EcoGridService service = null;
	  try {
		  service = (EcoGridService) controller.getServicesList().elementAt(1);
		  System.out.println("service: " + service.getEndPoint());
		  Domain domain = controller.getServiceDomain(service);
		  assertNotNull(domain);
		  assertNotNull(domain.getDomain());
		  assertTrue(domain.getDomain().length() > 0);
		  log.debug("domain: " + domain.getDomain());
	  }
	  catch (Exception e) {
		  e.printStackTrace();
		  fail("problem getting domain for service: " + service.getServiceName());
	}
  }
  
  /**
   * Test adding a service into controller
   */
  public void addServiceTest()
  {
    try
    {
      System.out.println("Old service in list before add ");
      controller.print();
      EcoGridService newService = new EcoGridService(); 
      newService.setServiceName("New Query Service");
      newService.setServiceGroup("KNB");
      newService.setServiceType(EcoGridServicesController.QUERYSERVICETYPE);
      newService.setEndPoint("http://BOGUSENDPOINT");
      DocumentType type1 = new DocumentType("eml://ecoinformatics.org/eml-2.0.0", "Ecological Metadata Language 2.0.0");
      DocumentType type2 = new DocumentType("http://digir.net/schema/conceptual/darwin/full/2001/1.0", "Darwin Core 1.0");
      DocumentType[] typeList = new DocumentType[2];
      typeList[0] = type1;
      typeList[1] = type2;
      newService.setDocumentTypeList(typeList);
      controller.addService(newService);
      System.out.println("new service in list after add");
      controller.print();    
    }
    catch(Exception e)
    {
      System.out.println("error in add service test "+e.getMessage() );
      e.printStackTrace();
      assertTrue(1==2);
    }
  }//addServiceTest
  
  /**
   * A method to test update controller list
   */
  public void updateServiceTest()
  {
    try
    {
      System.out.println("Old service in list before update");
      controller.print();
      EcoGridService newService = new EcoGridService();
      newService.setServiceGroup("DIGIR");
      newService.setServiceName("New Query Serive");
      newService.setServiceType(EcoGridServicesController.QUERYSERVICETYPE);
      newService.setEndPoint("http://BOGUSENDPOINT");
      DocumentType type2 = new DocumentType(
                        "http://digir.net/schema/conceptual/darwin/full/2001/1.0", 
                        "Darwin Core 1.0");
      DocumentType[] typeList = new DocumentType[1];
      typeList[0] = type2;
      newService.setDocumentTypeList(typeList);
      controller.updateService(newService);
      System.out.println("new service in list after update");
      controller.print();  
    }
    catch(Exception e)
    {
        System.err.println("error in update service test "+e.getMessage());
        e.printStackTrace();
      assertTrue(1==2);
    }
  }//updateServiceTest
  
  /**
  * A method to test remove a service from controller list
  */
 public void removeServiceTest()
 {
   try
   {
     System.err.println("Old service in list before remove ");
     controller.print();
     Vector serviceList = controller.getServicesList();
     EcoGridService service =(EcoGridService)serviceList.elementAt(0); 
     controller.removeService(service);
     System.err.println("new service in list after remove ");
     controller.print();  
   }
   catch(Exception e)
   {
     System.err.println("error in remove service test "+e.getMessage());
     e.printStackTrace();
     assertTrue(1==2);
   }
 }//updateServiceTest


}//EcoGridServicesControllerTest
