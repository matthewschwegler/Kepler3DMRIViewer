/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2011-09-29 11:34:35 -0700 (Thu, 29 Sep 2011) $' 
 * '$Revision: 28670 $'
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
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.build.modules.ModuleTree;

/**
 * Test for the ConfigurationProperty
 * 
 * @author Chad Berkley
 */
public class ConfigurationPropertyTest extends TestCase {
	ConfigurationManager config;
  ModuleTree moduleTree;
  
	public ConfigurationPropertyTest(String name) 
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
		// note that the order of these tests DOES matter. don't change the
		// order!
		suite.addTest(new ConfigurationPropertyTest("initialize"));
    suite.addTest(new ConfigurationPropertyTest("testAddProperty"));
    suite.addTest(new ConfigurationPropertyTest("testAddProperties"));
    suite.addTest(new ConfigurationPropertyTest("testGetProperty"));
    suite.addTest(new ConfigurationPropertyTest("testGetProperties"));
    suite.addTest(new ConfigurationPropertyTest("testGetPropertiesWithPath"));
    suite.addTest(new ConfigurationPropertyTest("testOverrideProperty"));
    suite.addTest(new ConfigurationPropertyTest("testFindProperties"));
    suite.addTest(new ConfigurationPropertyTest("testAddPropertyAtIndex"));
    suite.addTest(new ConfigurationPropertyTest("testContainsProperty"));
    suite.addTest(new ConfigurationPropertyTest("testRemoveProperty"));
    //suite.addTest(new ConfigurationPropertyTest(""));
    //suite.addTest(new ConfigurationPropertyTest(""));
    //suite.addTest(new ConfigurationPropertyTest(""));
    //suite.addTest(new ConfigurationPropertyTest(""));
		return suite;
	}
	
	/**
	 *   public void removeProperty(ConfigurationProperty propertyToRemove)
	 */
	public void testRemoveProperty()
	{
	  ConfigurationProperty cp = ConfigurationManager.getInstance().getProperty(
      ConfigurationManager.getModule("configuration-manager"), 
      new ConfigurationNamespace("test"));
    ConfigurationProperty cp2 = cp.getProperty("viewPaneTabPanes");
    assertTrue(cp.containsProperty("viewPaneTabPanes", false));
    cp.removeProperty(cp2);
    assertFalse(cp.containsProperty("viewPaneTabPanes", false));
    
    // NOTE: add the property back otherwise the test configuration file gets
    // written to disk with no sub-nodes, which causes errors when the
    // configuration manager starts next. (i.e., Kepler won't start)
    
    try {
        cp.addProperty(cp2);
    } catch (NamespaceException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } catch (ConfigurationManagerException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    
    ConfigurationManager.getInstance().clearConfigurations();
	}
  
  /**
   *    public boolean containsProperty(String name, boolean recursive)
   */
  public void testContainsProperty()
  {
    ConfigurationManager.getInstance().clearConfigurations();
    ConfigurationProperty cp = ConfigurationManager.getInstance().getProperty(
      ConfigurationManager.getModule("configuration-manager"), 
      new ConfigurationNamespace("test"));
      
    assertTrue(cp.containsProperty("viewPaneTabPanes", false));
    assertTrue(cp.containsProperty("viewPaneLocation", true));
    assertFalse(cp.containsProperty("viewPaneLocation", false));
    assertFalse(cp.containsProperty("some random property", true));
  }
 
  /**
   * public void addPropertyAtIndex(int index, ConfigurationProperty propertyToAdd)
   */
  public void testAddPropertyAtIndex()
  {
    try
    {
      ConfigurationManager.getInstance().clearConfigurations();
      ConfigurationProperty cp = ConfigurationManager.getInstance().getProperty(
        ConfigurationManager.getModule("configuration-manager"), 
        new ConfigurationNamespace("test"));
      ConfigurationProperty prop = cp.getProperty("viewPaneTabPanes.viewPane");
      ConfigurationProperty newprop = new ConfigurationProperty(
        ConfigurationManager.getModule("configuration-manager"), "NewProp", 
        prop.getNamespace(), "new value");
      assertTrue(prop.getProperty(5).getName().equals("viewPaneLocation"));
      prop.addPropertyAtIndex(5, newprop);
      assertTrue(prop.getProperty(5).getName().equals("NewProp"));
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("Should not have thrown exception in testAddPropertyAtIndex: " + e.getMessage());
    }
  }
  
  /**
   *   public List<ConfigurationProperty> findProperties(String name, String value, 
   *     boolean recursive)
   *   public List<ConfigurationProperty> findProperties(String name, String value)
   *   
   */
  public void testFindProperties()
  {
    ConfigurationManager.getInstance().clearConfigurations();
    ConfigurationProperty cp = ConfigurationManager.getInstance().getProperty(
      ConfigurationManager.getModule("configuration-manager"), 
      new ConfigurationNamespace("test"));
    List l = cp.findProperties("name", "W", true);
    //ConfigurationProperty.prettyPrintList(l);
    assertTrue(l.size() == 3);
  }
  
  /**
   * public boolean overrideProperty(ConfigurationProperty originalProperty, 
   *   ConfigurationProperty newProperty, boolean overrideNamespace)
   */
  public void testOverrideProperty()
  {
    ConfigurationNamespace cn = new ConfigurationNamespace("test");
    ConfigurationProperty cp = ConfigurationManager.getInstance().getProperty(
      ConfigurationManager.getModule("configuration-manager"), cn);
    ConfigurationProperty overrideProperty = new ConfigurationProperty(
      ConfigurationManager.getModule("configuration-manager"), "testprop", cn, "testval");
    ConfigurationProperty propToOverride = cp.getProperty("viewPaneTabPanes.viewPane");
    //cp.prettyPrint();
    cp.overrideProperty(propToOverride, overrideProperty, false);
    //cp.prettyPrint();
    assertTrue(cp.getProperty("viewPaneTabPanes.viewPane") == null);
    assertTrue(cp.getProperty("viewPaneTabPanes.testprop") != null);
    assertTrue(cp.getProperty("viewPaneTabPanes.testprop").getValue().equals("testval"));
  }
  
  /**
   *  protected static List<ConfigurationProperty> getPropertiesWithPath(ConfigurationProperty property,
   *    String[] s, boolean returnChildren)
   */
  public void testGetPropertiesWithPath()
  {
    ConfigurationProperty cp = ConfigurationManager.getInstance().getProperty(
      ConfigurationManager.getModule("configuration-manager"), new ConfigurationNamespace("test"));
    //cp.prettyPrint();
    String[] s = {"viewPane", "viewPaneLocation", "tabPane", "name"};
    List l = ConfigurationProperty.getPropertiesWithPath(cp, s, false);
    //ConfigurationProperty.prettyPrintList(l);
    //System.out.println("list size: " + l.size());
    assertTrue(l.size() == 3);
    
    String[] s2 = {"viewPaneLocation"};
    l = ConfigurationProperty.getPropertiesWithPath(cp, s2, false);
    assertTrue(l.size() == 5);
    
    //test getting the children props
    String[] s3 = {"viewPane", "viewPaneLocation", "tabPane", "name"};
    l = ConfigurationProperty.getPropertiesWithPath(cp, s3, true);
    //ConfigurationProperty.prettyPrintList(l);
    assertTrue(l.size() == 3);
    assertTrue(((ConfigurationProperty)l.get(1)).getValue().equals("Data") ||
      ((ConfigurationProperty)l.get(1)).getValue().equals("Some New Data"));
  }
  
  /**
   * public List<ConfigurationProperty> getProperties()
   * public List<ConfigurationProperty> getProperties(String name)
   * public List<ConfigurationProperty> getProperties(String name, boolean recursive)
   */
  public void testGetProperties()
  {
    ConfigurationProperty cp = ConfigurationManager.getInstance().getProperty(
      ConfigurationManager.getModule("configuration-manager"));
    //get all
    List l = cp.getProperties();
    assertTrue(((ConfigurationProperty)l.get(5)).getName().equals("stylesheet"));
    //get by name
    l = cp.getProperties("stylesheet");
    assertTrue(((ConfigurationProperty)l.get(0)).getProperty("namespace").getValue().equals("eml://ecoinformatics.org/eml-2.0.0"));
    assertTrue(((ConfigurationProperty)l.get(1)).getProperty("namespace").getValue().equals("eml://ecoinformatics.org/eml-2.0.1"));
    //recursive get by name
    l = cp.getProperties("systemid", true);
    assertTrue(l.size() == 3);
    assertTrue(((ConfigurationProperty)l.get(0)).getValue().equals("style/eml-2.0.1/eml.xsl"));
    assertTrue(((ConfigurationProperty)l.get(1)).getValue().equals("style/eml-2.0.1/eml2.xsl"));
    assertTrue(((ConfigurationProperty)l.get(2)).getValue().equals("ts"));
    
    l = cp.getProperties("test.stylesheet.systemid");
    //ConfigurationProperty.prettyPrintList(l);
    assertTrue(l.size() == 1);
    assertTrue(((ConfigurationProperty)l.get(0)).getValue().equals("ts"));
    
  }
  
  /**
   * public ConfigurationProperty getProperty(String name)
   * public ConfigurationProperty getProperty(String name, int index)
   * public ConfigurationProperty getProperty(int index)
   */
  public void testGetProperty()
  {
    
    ConfigurationProperty cp = ConfigurationManager.getInstance().getProperty(
      ConfigurationManager.getModule("configuration-manager"));
    assertTrue(cp.getProperty("splash-image").getValue().equals("images/kepler-splash.png"));
    //assertTrue(cp.getProperty("startup.mkdir.dir", 2).getValue().equals("cache/objects"));
    assertTrue(cp.getProperty(1).getValue().equals("actors/lib/jar/"));
  }
  
  /**
   * public void addProperties(List<RootConfigurationProperty> propertyList)
   */
  public void testAddProperties()
  {
    try
    {
      try
      { //this tries to add to the default namespace, it should fail because
        //there is a configuration.xml file that already loads into this 
        //namespace automatically.
        RootConfigurationProperty rcp = new RootConfigurationProperty(
          ConfigurationManager.getModule("configuration-manager"),
          "testProperty1",
          new ConfigurationProperty(ConfigurationManager.getModule("configuration-manager"),
            "test1", "test-val1"));
        RootConfigurationProperty rcp2 = new RootConfigurationProperty(
          ConfigurationManager.getModule("configuration-manager"),
          "testProperty2",
          new ConfigurationProperty(ConfigurationManager.getModule("configuration-manager"),
            "test2", "test-val2"));
        Vector v = new Vector();
        v.add(rcp);
        v.add(rcp2);
        ConfigurationManager.getInstance().addProperties(v);
        fail("Should have thrown namespace exception");
      }
      catch(NamespaceException ne)
      {
        
      }
      
      //this should work.
      ConfigurationNamespace newNamespace1 = new ConfigurationNamespace("newNamespace1");
      ConfigurationNamespace newNamespace2 = new ConfigurationNamespace("newNamespace2");
      RootConfigurationProperty rcp = new RootConfigurationProperty(
        ConfigurationManager.getModule("configuration-manager"),
        "testProperty1", newNamespace1,
        new ConfigurationProperty(ConfigurationManager.getModule("configuration-manager"),
          "test1", newNamespace1, "test-val1"));
      RootConfigurationProperty rcp2 = new RootConfigurationProperty(
        ConfigurationManager.getModule("configuration-manager"),
        "testProperty2", newNamespace2, 
        new ConfigurationProperty(ConfigurationManager.getModule("configuration-manager"),
          "test2", newNamespace2, "test-val2"));
      Vector v = new Vector();
      v.add(rcp);
      v.add(rcp2);
      ConfigurationManager.getInstance().addProperties(v);
      
      ConfigurationProperty newProp = 
        ConfigurationManager.getInstance()
          .getProperty(
            ConfigurationManager.getModule("configuration-manager"), newNamespace1);
      //newProp.prettyPrint();
      assertTrue(newProp.getValue().equals("test-val1"));
    }
    catch(Exception e)
    {
      fail("should not have thrown an exception: " + e.getMessage());
    }
  }
  
  /**
   * public void addProperty(RootConfigurationProperty property)
   */
  public void testAddProperty()
  {
    try
    {
      try
      { //this tries to add to the default namespace, it should fail because
        //there is a configuration.xml file that already loads into this 
        //namespace automatically.
        RootConfigurationProperty rcp = new RootConfigurationProperty(
          ConfigurationManager.getModule("configuration-manager"),
          "testProperty",
          new ConfigurationProperty(ConfigurationManager.getModule("configuration-manager"),
            "test", "test-val"));
        ConfigurationManager.getInstance().addProperty(rcp);
        fail("Should have thrown namespace exception");
      }
      catch(NamespaceException ne)
      {
        
      }
      
      //use a new namespace and this should work
      ConfigurationNamespace newNamespace = new ConfigurationNamespace("newNamespace");
      RootConfigurationProperty rcp = new RootConfigurationProperty(
          ConfigurationManager.getModule("configuration-manager"),
          "testProperty", newNamespace,
          new ConfigurationProperty(ConfigurationManager.getModule("configuration-manager"),
            "test", newNamespace, "test-val"));
      ConfigurationManager.getInstance().addProperty(rcp);
      
      //ConfigurationProperty.simplePrintList(ConfigurationManager.getInstance().getProperties());
      
      ConfigurationProperty newProp = 
        ConfigurationManager.getInstance()
          .getProperty(
            ConfigurationManager.getModule("configuration-manager"), newNamespace);
      //newProp.prettyPrint();
      assertTrue(newProp.getValue().equals("test-val"));      
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("Should not have thrown an exception in testConfigurationManagerInstance: " + e.getMessage());
    }
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
   * private class to test the event listeners
   */
  private class ConfigListener implements ConfigurationEventListener
  {
    private int event;
    
    public ConfigListener(int event)
    {
      this.event = event;
    }
    
    public void eventPerformed(ConfigurationEvent ce)
    {
      if(event == 0)
      {
        //System.out.println("ce: " + ce.getProperty().getName());
        //ce.getProperty().prettyPrint();
        assertTrue(ce.getProperty().getName().equals("viewPaneTabPanes"));
      }
      else if(event == 1)
      {
        //System.out.println("ce: " + ce.getProperty().getName());
        //ce.getProperty().prettyPrint();
        assertTrue(ce.getProperty().getName().equals("name"));
      }
      else
      {
        fail("Error, the event " + event + " was never performed.");
      }
    }
  }
}
