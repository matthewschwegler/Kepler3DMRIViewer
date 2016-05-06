/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-12-07 15:39:30 -0800 (Tue, 07 Dec 2010) $' 
 * '$Revision: 26437 $'
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

package org.kepler.configuration;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.build.modules.ModuleTree;

/**
 * Test for the ConfigurationManager
 * 
 * @author Chad Berkley
 */
public class ConfigurationManagerTest extends TestCase {
	ConfigurationManager config;
  ModuleTree moduleTree;
  
	public ConfigurationManagerTest(String name) 
  {
		super(name);
    try
    {
		  config = ConfigurationManager.getInstance();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("could not instantiate the configurationManager: " + e.getMessage());
    }
    ModuleTree.init();
    moduleTree = ModuleTree.instance();
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
		suite.addTest(new ConfigurationManagerTest("initialize"));
    suite.addTest(new ConfigurationManagerTest("testGetPropertyNoArguments"));
    suite.addTest(new ConfigurationManagerTest("testGetPropertyWithNamespace"));
    suite.addTest(new ConfigurationManagerTest("testGetPropertyWithNamespaceAndName"));
    suite.addTest(new ConfigurationManagerTest("testGetPropertiesWithModule"));
    suite.addTest(new ConfigurationManagerTest("testGetPropertiesWithModuleAndNamespace"));
    suite.addTest(new ConfigurationManagerTest("testGetPropertiesWithModuleNamespaceAndName"));
    suite.addTest(new ConfigurationManagerTest("testGetPropertiesWithModuleAndName"));
    suite.addTest(new ConfigurationManagerTest("testModuleNameAliasing"));
		return suite;
	}

	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() 
  {
		assertTrue(1 == 1);
	}
  
  /**
   * getProperty(Module module)
   */
  public void testGetPropertyNoArguments()
  {
    ConfigurationProperty cp = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("configuration-manager"));
    //cp.prettyPrint();
    assertTrue(cp.getName().equals("config"));
  }
  
  /**
   * getProperty(Module module, ConfigurationNamespace namespace)
   */
  public void testGetPropertyWithNamespace()
  { 
    List l = ConfigurationManager.getInstance().getProperties(
      ConfigurationManager.getModule("configuration-manager"));
    ConfigurationProperty cp = ConfigurationManager.getInstance().getProperty(
      ConfigurationManager.getModule("configuration-manager"),
      new ConfigurationNamespace("test"));
   
    assertTrue(cp.getName().equals("config"));
  }
  
  /**
   * getProperty(Module module, String name)
   */
  public void testGetPropertyWithNamespaceAndName()
  {
    //this also tests the dot name notation for ConfigurationManager.
    ConfigurationProperty cp = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("configuration-manager"), 
        "autoDataSourcesUpdate.delay");
    assertTrue(cp.getValue().equals("0"));
    //cp.prettyPrint();
  }
  
  /**
   * getProperties(Module module)
   */
  public void testGetPropertiesWithModule()
  {
    List l = ConfigurationManager.getInstance()
      .getProperties(ConfigurationManager.getModule("configuration-manager"));
    //ConfigurationProperty.prettyPrintList(l);
    ConfigurationProperty l2 = (ConfigurationProperty)l.get(3);
    //l2.prettyPrint();
    //System.out.println("name: " + l2.getName());
    assertTrue(l2.getName().equals("test-ns"));
    assertTrue(l2.getNamespace().toString().equals("test-ns"));
  }
  
  /**
   * getProperties(Module module, ConfigurationNamespace namespace)
   */
  public void testGetPropertiesWithModuleAndNamespace()
  {
    List l = ConfigurationManager.getInstance()
      .getProperties(ConfigurationManager.getModule("configuration-manager"),
        new ConfigurationNamespace("test2"));
    ConfigurationProperty l0 = (ConfigurationProperty)l.get(0);
    assertTrue(l0.getName().equals("test2"));
    assertTrue(l0.getNamespace().toString().equals("test2"));
  }
  
  /**
   * getProperties(Module module, ConfigurationNamespace namespace, String name)
   */
  public void testGetPropertiesWithModuleNamespaceAndName()
  {
    List l = ConfigurationManager.getInstance()
      .getProperties(ConfigurationManager.getModule("configuration-manager"),
        new ConfigurationNamespace("test2"), "startup.mkdir");
    ConfigurationProperty l2 = (ConfigurationProperty)l.get(2);
    //ConfigurationProperty.prettyPrintList(l);
    assertTrue(l2.getValue().equals("cache/objects"));
    
    //test deeper dot notation
    l = ConfigurationManager.getInstance()
      .getProperties(ConfigurationManager.getModule("configuration-manager"),
        new ConfigurationNamespace("test"), "config.viewPaneTabPanes.viewPane.viewPaneLocation");
    //ConfigurationProperty.prettyPrintList(l);
    ConfigurationProperty l3 = (ConfigurationProperty)l.get(3);
    assertTrue(l3.getProperty("name").getValue().equals("W"));
  }
  
  /**
   * getProperties(Module module, String name)
   */
  public void testGetPropertiesWithModuleAndName()
  {
    List l = ConfigurationManager.getInstance()
      .getProperties(ConfigurationManager.getModule("configuration-manager"), 
        "config.startup.mkdir.dir");
    //ConfigurationProperty.prettyPrintList(l);
    ConfigurationProperty l2 = (ConfigurationProperty)l.get(2);
    //System.out.println("l2: " + l2.getValue());
    assertTrue(l2.getValue().equals("RawData"));
  }
  
  /**
   * test the aliasing 
   */
  public void testModuleNameAliasing()
  {
    //hold off on this test until there is an easier way to test it.  
  }
}