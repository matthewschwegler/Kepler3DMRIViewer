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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.yaml.snakeyaml.Yaml;


/**
 * Test for the configuration manager
 * 
 * @author Chad Berkley
 */
public class SerializationComparisonTest extends TestCase {
	ConfigurationManager config;
  ModuleTree moduleTree;
  
	public SerializationComparisonTest(String name) 
  {
		super(name);
    try
    {
		  config = ConfigurationManager.getInstance();
    }
    catch(Exception e)
    {
      fail("Could not instantiate ConfigurationManager");
    }
    ModuleTree.init();
    moduleTree = ModuleTree.instance();
	}

	/**
	 * Establish a testing framework by initializing appropriate objects
	 */
	public void setUp() 
  {
    File f = org.kepler.util.DotKeplerManager.getInstance().getModuleConfigurationDirectory("configuration-manager");
    File files[] = f.listFiles();
    for(int i=0; i<files.length; i++)
    { //make sure there are no saved files because it will mess up these tests.
      files[i].delete();
    }
	}

	/**
	 * Release any objects after tests are complete
	 */
	public void tearDown() 
  {
    File f = org.kepler.util.DotKeplerManager.getInstance().getModuleConfigurationDirectory("configuration-manager");
    File files[] = f.listFiles();
    for(int i=0; i<files.length; i++)
    { //make sure there are no saved files because it will mess up these tests.
      files[i].delete();
    }
	}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() 
  {
		TestSuite suite = new TestSuite();
		suite.addTest(new SerializationComparisonTest("initialize"));
    suite.addTest(new SerializationComparisonTest("testYamlRead"));
    suite.addTest(new SerializationComparisonTest("testCommonsRead"));
    suite.addTest(new SerializationComparisonTest("testCommonsLoadConfiguration"));
    suite.addTest(new SerializationComparisonTest("testCommonsSaveConfiguration"));
    suite.addTest(new SerializationComparisonTest("testCommonsMutableSerialization"));
		suite.addTest(new SerializationComparisonTest("testCommonsLocaleSerialization"));
    suite.addTest(new SerializationComparisonTest("testFullConfigFile"));
    suite.addTest(new SerializationComparisonTest("testVirtualNamespaces"));
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
   * test the yaml interface for reading properties
   */
  public void testYamlRead()
  {
    try
    {
      InputStream input = new FileInputStream(new File("configuration-manager/resources/configurations/config.yaml"));
      Yaml yaml = new Yaml();
      LinkedHashMap config = (LinkedHashMap)yaml.load(input);
      //System.out.println("class: " + config.getClass().getName());
      //System.out.println("data: " + config.toString());
      /*Iterator keys = data.keySet().iterator();
      while(keys.hasNext())
      {
        String key = (String)keys.next();
        System.out.println(key);
      }*/
      
      //try to read viewPaneTabPanes/viewPane/viewPaneLocation/location
      LinkedList viewPaneTabPanes = (LinkedList)config.get("viewPaneTabPanes");
      //System.out.println("viewPaneTabPanes: " + viewPaneTabPanes.toString());
      
      LinkedHashMap viewPaneLHM = (LinkedHashMap)viewPaneTabPanes.get(0);
      //System.out.println("viewPaneLHM: " + viewPaneLHM.toString());
      
      LinkedList viewPane = (LinkedList)viewPaneLHM.get("viewPane");
      //System.out.println("viewPaneLocation: " + viewPane.toString());
      
      LinkedHashMap viewPaneLocationLHM = (LinkedHashMap)viewPane.get(1);
      //System.out.println("viewPaneLocation: " + viewPaneLocationLHM.toString());
      
      LinkedList viewPaneLocation = (LinkedList)viewPaneLocationLHM.get("viewPaneLocation");
      //System.out.println("location: " + viewPaneLocation.toString());
      
      LinkedHashMap locationLHM = (LinkedHashMap)viewPaneLocation.get(0);
      //System.out.println("location: " + locationLHM.toString());
      
      String location = (String)locationLHM.get("location");
      //System.out.println("location: " + location.toString());
      assertTrue(location.equals("W"));
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("Threw yaml error: " + e.getMessage());
    }
    System.out.println();
    System.out.println();
  }
  
  /**
   * test the commons interface for reading properties
   */
  public void testCommonsRead()
  {
    try
    {
      File f = new File("configuration-manager/resources/configurations/configuration.xml");
      assertTrue(f.exists());
      XMLConfiguration config = new XMLConfiguration(f);
      
      //get the conditionals of query and read some properties
      ArrayList al = (ArrayList)config.getProperty("ecogridService.queryList.query.AND.OR.condition.concept");
      assertTrue(config.getString("ecogridService.queryList.query.AND.OR.condition(1).concept").equals("keyword"));
      assertTrue(config.getString("ecogridService.queryList.query.AND.OR.condition(3).operator").equals("LIKE"));
      
      //break the config down into a smaller subset of just the queryList
      HierarchicalConfiguration sub = config.configurationAt("ecogridService.queryList");
      al = (ArrayList)sub.getProperty("query.returnField");
      al = (ArrayList)sub.getProperty("query(0).returnField");
      //get the 2nd returnfield of the first query
      assertTrue(((String)al.get(1)).equals("entityName"));

    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("Commons error: " + e.getMessage());
    }
    
    System.out.println();
    System.out.println();
  }
  
  /**
   * test the loading of the configuration files
   */
  public void testCommonsLoadConfiguration()
  {
    try
    {
      ConfigurationReader cr = new CommonsConfigurationReader();
      ModuleTree tree = ModuleTree.instance();
      Iterator it = tree.iterator();
      while(it.hasNext())
      {
        Module m = (Module)it.next();
        if(m.getName().equals("configuration-manager"))
        {
          Vector v = (Vector)cr.loadConfigurations(m);
          ConfigurationProperty configxmlProp = (ConfigurationProperty)v.elementAt(0);
          String value = configxmlProp.getProperty("config").getProperty("splash-image").getValue();
          assertTrue(value.equals("images/kepler-splash.png"));
          ConfigurationProperty tmp = configxmlProp.getProperty("config")
            .getProperty("viewPaneTabPanes").getProperty("viewPane");
          //System.out.println("tmp: " + tmp.toString());
          //tmp.prettyPrint();
          value = configxmlProp.getProperty("config").getProperty("viewPaneTabPanes")
            .getProperty("viewPane").getProperty("viewPaneLocation", 1).getProperty("name").getValue();
          assertTrue(value.equals("E"));
        }
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("No exception should have been thrown loading configurations: " + e.getMessage());
    }
  }
  
  /**
   * test serialization using commons
   */
  public void testCommonsSaveConfiguration()
  {
    try
    {
      //save the config file so we can re-write it after this test
      File configFile = new File("configuration-manager/resources/configurations/test.xml");
      
      //read in a file, change a property, then write it out and see if we
      //get the correct output
      CommonsConfigurationReader reader = new CommonsConfigurationReader();
      RootConfigurationProperty cp = reader.loadConfiguration(
        getModule("configuration-manager"), 
        configFile);
      cp.resetDirty(true);
      
      //change value 1
      ConfigurationProperty p = cp.getProperty("config.viewPaneTabPanes.viewPane.name");
      assertTrue(p.getValue().equals("Kepler Classic"));
      p.setValue("New and Improved Kepler");
      assertTrue(p.getValue().equals("New and Improved Kepler"));
      assertTrue(cp.getProperty("config.viewPaneTabPanes.viewPane.name").getValue().equals("New and Improved Kepler"));
      assertTrue(p.isDirty());
      //change value 2
      p = cp.getProperty("config.viewPaneTabPanes.viewPane.viewPaneLocation");
      p.getProperty("tabPane", 1).getProperty("name").setValue("Some New Data");
      assertTrue(p.getProperty("tabPane", 1).getProperty("name").getValue().equals("Some New Data"));
      assertTrue(cp.getProperty("config.viewPaneTabPanes.viewPane.viewPaneLocation")
        .getProperty("tabPane", 1).getProperty("name").getValue().equals("Some New Data"));
      assertTrue(p.getProperty("tabPane", 1).getProperty("name").isDirty());
      
      p = cp.getProperty("config.viewPaneTabPanes.viewPane.viewPaneLocation");
      ConfigurationProperty p2 = new ConfigurationProperty(p.getModule(), "anothernewprop", "some value");
      p2.setNamespace(cp.getNamespace());
      p.addProperty(p.getModule(), "newprop", cp.getNamespace(), p2);
      assertTrue(p.isDirty());
      
      //serialize the property with the new value
      GeneralConfigurationWriter writer = new GeneralConfigurationWriter();
      //cp.prettyPrint();
      writer.writeConfiguration(cp);
      
      //deserialize it and check that the value is the new one
      reader = new CommonsConfigurationReader();
      cp = reader.loadConfiguration(
        getModule("configuration-manager"), 
        new File("configuration-manager/resources/configurations/test.xml"));
      //cp.prettyPrint();
      p = cp.getProperty("config.viewPaneTabPanes.viewPane.viewPaneLocation");
      //p.prettyPrint();
      assertTrue(p.getProperty("tabPane", 1).getProperty("name").getValue().equals("Some New Data"));
      assertTrue(cp.getProperty("config.viewPaneTabPanes.viewPane.viewPaneLocation")
        .getProperty("tabPane", 1).getProperty("name").getValue().equals("Some New Data"));
        
      File f = new File(ConfigurationManager.dotKeplerConfigurationsDir + "/configuration-manager", "test.xml");
      f.delete();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("No exception should have been thrown in testCommonsSaveConfiguration(): " +
        e.getMessage());
    }
  }
  
  /**
   * test (de)serialization of mutable entities
   */
  public void testCommonsMutableSerialization()
  {
    try
    {
      //save the file so we can re-write it after this test
      File configFile = new File("configuration-manager/resources/configurations/test.xml");
      
      CommonsConfigurationReader reader = new CommonsConfigurationReader();
      RootConfigurationProperty cp = reader.loadConfiguration(
        getModule("configuration-manager"), configFile);
      
      ConfigurationProperty p = cp.getProperty("config.viewPaneTabPanes.viewPane");
      p.setMutable(false);
      
      //try to change p.  this should throw an exception
      try
      {
        p.setValue("XXX");
        fail("An exception should have been thrown since p is not mutable.");
      }
      catch(ConfigurationManagerException cme)
      {
        
      }
      
      try
      {
        p.addProperty(
          new ConfigurationProperty(
            getModule("configuration-manager"), "newname", 
            p.getNamespace(), "newval"));
        fail("An exception should have been thrown since p is not mutable.");
      }
      catch(ConfigurationManagerException cme)
      {
        
      }
      
      //now serialize p and make sure its still not mutable when deserialized
      GeneralConfigurationWriter writer = new GeneralConfigurationWriter();
      writer.writeConfiguration(cp);
      
      //deserialize it and check that the value is the new one
      reader = new CommonsConfigurationReader();
      cp = reader.loadConfiguration(
        getModule("configuration-manager"), 
        new File("configuration-manager/resources/configurations/test.xml"));
      
      p = cp.getProperty("config.viewPaneTabPanes.viewPane");
      
      
      assertFalse(p.isMutable());
      File f = new File(ConfigurationManager.dotKeplerConfigurationsDir + "/configuration-manager", "test.xml");
      f.delete();
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("No exception should have been thrown in testCommonsMutableSerialization: " + e.getMessage());
    }
  }
  
  /**
   * test locale support
   */
  public void testCommonsLocaleSerialization()
  {
    try
    {
      Module m = getModule("configuration-manager");
      File f = new File(m.getConfigurationsDir(), "test.xml");
      Locale l = Locale.GERMANY;
      
      //we ask to load test.xml with a german locale.  there's a german test.xml
      //(test_de_DE.xml) so we should not load test.xml
      CommonsConfigurationReader ccr = new CommonsConfigurationReader();
      ConfigurationProperty cp = ccr.loadConfiguration(m, f, l);
      assertTrue(cp == null);
      
      //we should load test_de_DE.xml when using the locale de_DE
      f = new File(m.getConfigurationsDir(), "test_de_DE.xml");
      cp = ccr.loadConfiguration(m, f, l);
      assertTrue(cp != null);
      
      //we should not load this file because we want a chinese locale, but we're 
      //trying to load a german file when another default file exists
      l = Locale.CHINA;
      cp = ccr.loadConfiguration(m, f, l);
      assertTrue(cp == null);
      
      //this file should load because we're asking for chinese and there is
      //no chinese file and we're asking to load a file with no locale
      //designator.
      f = new File(m.getConfigurationsDir(), "test.xml");
      cp = ccr.loadConfiguration(m, f, l);
      assertTrue(cp != null);
    }
    catch(Exception e)
    {
      fail("No exception should have been thrown in testCommonsLocaleSerialization: " + e.getMessage());
    }
  }
  
  /**
   * test to handle the full configuration.xml file (instead of test.xml which is only
   * a subset).
   */
  public void testFullConfigFile()
  {
    try
    {
      ConfigurationManager.getInstance().clearConfigurations();
      Module m = getModule("configuration-manager");
      List l = ConfigurationManager.getInstance().getProperties(m);
      /*for(int i=0; i<l.size(); i++)
      {
        String name = ((ConfigurationProperty)l.get(i)).getName();
        System.out.println("name: " + name);
      }*/
      ConfigurationProperty cp = ConfigurationManager.getInstance()
        .getProperty(m, new ConfigurationNamespace("configuration"));
      //cp.prettyPrint();
      assertTrue(cp != null);
      assertTrue(cp.getProperty("splash-image").getValue().equals("images/kepler-splash.png"));
      
    }
    catch(Exception e)
    {
      e.printStackTrace();
      fail("No exception should have been thrown in testFullConfigFile: " + e.getMessage());
    }
  }
  
  /**
   * test allowing multiple config files to write to the same namespace with the
   * &lt;namespace&gt; element
   */
  public void testVirtualNamespaces()
  {
    ConfigurationManager.getInstance().clearConfigurations();
    Module m = getModule("configuration-manager");
    List l = ConfigurationManager.getInstance()
        .getProperties(m, new ConfigurationNamespace("test-ns"));
    //ConfigurationProperty.prettyPrintList(l);
    
    assertTrue(l.size() == 1);
    ConfigurationProperty cp = (ConfigurationProperty)l.get(0);
    List props = cp.getProperties("config.pair.name");
    assertTrue(((ConfigurationProperty)props.get(0)).getValue().equals("x"));
    assertTrue(((ConfigurationProperty)props.get(1)).getValue().equals("y"));
  }
  
  /**
   * get a module
   */
  private Module getModule(String name)
  {
    return ConfigurationManager.getModule(name);
  }
}
