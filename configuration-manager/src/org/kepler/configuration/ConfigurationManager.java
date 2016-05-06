/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: koehler $'
 * '$Date: 2013-02-06 15:11:12 -0800 (Wed, 06 Feb 2013) $' 
 * '$Revision: 31410 $'
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.util.DotKeplerManager;

import ptolemy.util.MessageHandler;

/**
 * A class to manage configuration options in Kepler. For more information see
 * https://kepler-project.org/developers/teams/framework/kepler-configuration/
 * proposed-future-kepler-configuration-system
 * and
 * https://kepler-project.org/developers/teams/framework/configuration-system-
 * documentation
 * 
 * @author Chad Berkley
 * @created October 2009
 */
public class ConfigurationManager
{
  //classes to read and write the configuration
  protected ConfigurationWriter configWriter;
  protected ConfigurationReader configReader;

  public static final String dotKeplerConfigurationsDir = System
      .getProperty("user.home")
      + File.separator + ".kepler" + File.separator + "configurations";

  private static ConfigurationManager configurationManager;
  private List<ConfigurationProperty> propertyList;
  private Vector<ConfigurationEventListener> listeners;

  /**
   * private constructor
   */
  private ConfigurationManager()
  {
    propertyList = new ArrayList<ConfigurationProperty>();
    listeners = new Vector<ConfigurationEventListener>();
    //this if block will go away.  it should be replaced by a reflections
    //method to load the correct configurationreader/writer class.
    //its here for testing only.
    //use the general configuration writer which writes dirty properties
    //to the .kepler directory instead of back to the configuration directory
    configWriter = new GeneralConfigurationWriter();
    configReader = new CommonsConfigurationReader(configWriter);

    addConfigurationListener(new SaveListener());
    
  }

  /**
   * singleton accessor
   */
  public static ConfigurationManager getInstance()
  {
    return getInstance(true);
  }

  /**
   * singleton accessor. set loadConfiguration to true if you want to
   * load the configuration automatically. This singleton accessor should really
   * only be used for testing.
   */
  protected static ConfigurationManager getInstance(boolean loadConfiguration)
  {
    if (configurationManager == null)
    {
      configurationManager = new ConfigurationManager();
      if(loadConfiguration && ModulesTxt.buildAreaExists()) {
          try {
            configurationManager.loadConfiguration();
        } catch (ConfigurationManagerException e) {
            MessageHandler.error("Error loading configurations", e);
        }
      }
    }
    return configurationManager;
  }

  /**
   * add a property to the manager
   * 
   * @param property the property to add
   */
  public void addProperty(RootConfigurationProperty property)
      throws NamespaceException
  {
    ConfigurationNamespace namespace = property.getNamespace();
    List propList = getProperties(property.getModule());
    for (int i = 0; i < propList.size(); i++)
    {
      RootConfigurationProperty rcp = (RootConfigurationProperty) propList
          .get(i);
      if (rcp.getNamespace().equals(namespace))
      {
        throw new NamespaceException("Can't add a second namespace '"
            + namespace + "' to the configuration for module '"
            + property.getModule().getName() + "'");
      }
    }
    propertyList.add(property);
  }

  /**
   * set a list of properties all at once
   * 
   * @param propertyList the list of properties to add
   */
  public void addProperties(List<RootConfigurationProperty> propertyList)
      throws NamespaceException
  {
    for (int i = 0; i < propertyList.size(); i++)
    {
      RootConfigurationProperty rcp = propertyList.get(i);
      addProperty(rcp);
    }
  }

  /**
   * return all properties handled by the manager
   */
  public List<ConfigurationProperty> getProperties()
  {
    return propertyList;
  }

  /**
   * get a list of properties that belong to a module
   * 
   * @param module the module to get the property list for
   */
  public List<ConfigurationProperty> getProperties(Module module)
  {
    //System.out.println("getting properties for module " + module.getName());
    Vector<ConfigurationProperty> v = new Vector<ConfigurationProperty>();
    for (int i = 0; i < propertyList.size(); i++)
    {
      ConfigurationProperty prop = propertyList.get(i);
      if (prop.getModule().getName().equals(module.getName()))
      {
        v.add(prop);
      }
    }
    return v;
  }

  /**
   * get a list of properties that belong to a certain module within a specific
   * namespace
   * 
   * @param module
   * @param namespace namespace within the module to get a property list for
   */
  public List<ConfigurationProperty> getProperties(Module module,
      ConfigurationNamespace namespace)
  {
    Vector<ConfigurationProperty> v = new Vector<ConfigurationProperty>();
    List<ConfigurationProperty> l = getProperties(module);
    for (int i = 0; i < l.size(); i++)
    {
      ConfigurationProperty cp = l.get(i);
      if (cp.getNamespace().equals(namespace))
      {
        v.add(cp);
      }
    }
    return v;
  }

  /**
   * get a list of properties that belong to a certain module with a specific
   * name within a namespace
   * 
   * @param module
   * @param namespace namespace within the module
   * @param name name of the property to get
   */
  public List<ConfigurationProperty> getProperties(Module module,
      ConfigurationNamespace namespace, String name)
  {
    Vector<ConfigurationProperty> v = new Vector<ConfigurationProperty>();
    List<ConfigurationProperty> l = getProperties(module, namespace);
    for (int i = 0; i < l.size(); i++)
    {
      ConfigurationProperty cp = l.get(i);
      List<ConfigurationProperty> propList = cp.getProperties(name);
      for (int j = 0; j < propList.size(); j++)
      {
        v.add(propList.get(j));
      }
    }
    return v;
  }

  /**
   * get a list of properties that belong to a certain module with a specific
   * name.
   * this assumes the default namespace.
   * 
   * @param module
   * @param name the name of the property to get
   */
  public List<ConfigurationProperty> getProperties(Module module, String name)
  {
    return getProperties(module, ConfigurationProperty.namespaceDefault, name);
  }

  /**
   * Returns the root property of the default configuration for a module.
   * This should be stored in
   * a file named "configuration.xml" in the
   * &lt;module&gt;/resources/configurations
   * directory. If no such file exists, this will return null.
   * 
   * @param module the module to get the default configuration for.
   */
  public ConfigurationProperty getProperty(Module module)
  {
    return getProperty(module, ConfigurationProperty.namespaceDefault);
  }

  /**
   * return the root ConfigurationProperty of the namespace.
   * 
   * @param module the module to get the property from
   * @param namespace the namespace to get the property from
   */
  public ConfigurationProperty getProperty(Module module,
      ConfigurationNamespace namespace)
  {
    List l = getProperties(module);
    for (int i = 0; i < l.size(); i++)
    {
      RootConfigurationProperty rcp = (RootConfigurationProperty) l.get(i);

      if (rcp.getNamespace().equals(namespace))
      {
        return rcp.getRootProperty();
      }
    }
    return null;
  }

  /**
   * get a property from the module's default namespace configuration with
   * a given name
   * 
   * @param module the module to get the property from
   * @param name the name of the property to get.
   */
  public ConfigurationProperty getProperty(Module module, String name)
  {
    return getProperty(module, ConfigurationProperty.namespaceDefault, name);
  }

  /**
   * return a single property from a module with a specific name in a specific
   * namespace. If more than one property shares a name, the first is returned.
   * Return null if the property is not found.
   * 
   * @param module the module to get the property from
   * @param namespace the namespace to get the property from
   * @param name the name of the property to get
   */
  public ConfigurationProperty getProperty(Module module,
      ConfigurationNamespace namespace, String name)
  {
    ConfigurationProperty cp = getProperty(module, namespace);
    if (cp == null){
    	return null;
    }
    ConfigurationProperty prop = cp.getProperty(name);
    return prop;
  }

  /**
   * serialize the entire configuration
   */
  public void saveConfiguration() throws ConfigurationManagerException
  {
    for (int i = 0; i < propertyList.size(); i++)
    {
      RootConfigurationProperty prop = (RootConfigurationProperty) propertyList
          .get(i);
      configWriter.writeConfiguration(prop);
    }
  }

  /**
   * add a configuration listener
   * 
   * @param listener the listener to add
   */
  public void addConfigurationListener(ConfigurationEventListener listener)
  {
    listeners.add(listener);
  }

  /**
   * remove a listener
   * 
   * @param listener the listener to remove
   */
  public void removeConfigurationListener(ConfigurationEventListener listener)
  {
    for (int i = 0; i < listeners.size(); i++)
    {
      ConfigurationEventListener cel = listeners.get(i);
      if (cel == listener)
      {
        listeners.remove(i);
      }
    }
  }

  /**
   * returns the file from the .kepler directory if it exists, returns f if
   * it doesn't. This method should be used to get configuration files
   * because if the file has changed it will be written back to .kepler, not
   * back to the resources/configurations directory of the module.
   * 
   * @param m the module to get the overwrite file from
   * @param f the file to get
   */
  public static File getOverwriteFile(Module m, File f)
  {
    File dotKeplerConfDir = DotKeplerManager.getInstance()
        .getModuleConfigurationDirectory(m.getName());
    ///    	.getModuleConfigurationDirectory(m.getStemName());
    if (!dotKeplerConfDir.exists())
    {
      return f;
    }

    String[] files = dotKeplerConfDir.list();
    for (int j = 0; j < files.length; j++)
    {
      File confFile = new File(dotKeplerConfDir, files[j]);
      if (confFile.getName().equals(f.getName()))
      {
        //found it.  return it.
        return confFile;
      }
    }
    return f;
  }

  /**
   * get a module by name. return null if not found
   * 
   * @param name the name of the module to get
   */
  public static Module getModule(String name)
  {
    Module m = ModuleTree.instance().getModuleByStemName(name);
    return m;
  }

  /**
   * notify any listeners that a configuration event has occured
   * 
   * @param property the property that changed to send to the listeners
   */
  protected void notifyListeners(ConfigurationProperty property)
  {
    for (int i = 0; i < listeners.size(); i++)
    {
      ConfigurationEventListener listener = listeners.get(i);
      listener.eventPerformed(new ConfigurationEvent(property));
    }
  }

  /**
   * deserialize the configuration
   */
  protected void loadConfiguration() throws ConfigurationManagerException
  {
    ModuleTree tree = ModuleTree.instance();
    Iterator it = tree.iterator();
    while (it.hasNext())
    {
      Module m = (Module) it.next();
      //System.out.println("loading files from module " + m.getName());
      List<RootConfigurationProperty> v = configReader.loadConfigurations(m);
      
      if (v == null)
      {
        continue;
      }
      for (int i = 0; i < v.size(); i++)
      {
        ConfigurationProperty prop = v.get(i);
        prop.resetDirty(true);
        //System.out.println("adding prop to prop list: " + prop.toString(false));
        propertyList.add(prop);
      }
    }
    //check to see if any properties of a module have the same namespace and 
    //if so, merge them into one property
    resolveNamespaces();
  }

  /**
   * remove all configurations currently listed and reset the configuration
   * manager back to its un-initialized state. This should really only be used
   * for testing.
   */
  protected void clearConfigurations()
  {
    configurationManager = null;
    propertyList = null;
    listeners = null;
  }

  /**
   * utility method to remove the file extension from the filename
   * 
   * @param filename the filename to remove the extension from
   */
  protected static String removeFileExtension(String filename)
  {
    String s = filename.substring(0, filename.lastIndexOf("."));
    return s;
  }

  /**
   * remove the locale designator from a filename. this works with
   * or without a file extension (i.e. .xml). This method assumes
   * a locale with both a country and language code (i.e. _en_US).
   */
  protected static String removeLocaleDesignator(String filename)
  {
    String fn = filename;
    int i = filename.lastIndexOf("_");
    if (i != -1)
    {
      filename = filename.substring(0, i);
      i = filename.lastIndexOf("_");
      if (i != -1)
      {
        filename = filename.substring(0, i);
        return filename;
      }
    }

    return fn;
  }

  /**
   * check to see if any properties of a module have the same namespace and
   * if so, merge them into one property
   */
  private void resolveNamespaces()
  {
    //ConfigurationProperty.simplePrintList(getProperties());
    ModuleTree tree = ModuleTree.instance();
    Iterator it = tree.iterator();
    while (it.hasNext())
    {
      boolean done = false;
      Module m = (Module) it.next();
      List props = getProperties(m);
      for (int i = 0; i < props.size(); i++)
      { //go through the properties and check to see if any have the same namespace
        ConfigurationProperty cp = (ConfigurationProperty) props.get(i);
        for (int j = 0; j < props.size(); j++)
        {
          ConfigurationProperty cp2 = (ConfigurationProperty) props.get(j);
          if (i != j)
          {
            if (cp.getNamespace().equals(cp2.getNamespace()))
            { //same namespace is found.  merge them and get out of the loops
              RootConfigurationProperty rcp = new RootConfigurationProperty(m,
                  cp.getNamespace().toString(), cp.getNamespace());
              try
              {
                rcp.addProperty(((RootConfigurationProperty) cp)
                    .getRootProperty());
                rcp.addProperty(((RootConfigurationProperty) cp2)
                    .getRootProperty());
                propertyList.remove(cp);
                propertyList.remove(cp2);
                propertyList.add(rcp);
                done = true;
                break;
              }
              catch (Exception e)
              {
                System.out.println("Error merging namesapace properties.  "
                    + "This exception should not be happening: "
                    + e.getMessage());
              }
            }
          }
        }

        if (done)
        {
          break;
        }
      }
    }
  }
}