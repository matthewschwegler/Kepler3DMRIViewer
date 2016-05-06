/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2012-03-27 14:09:20 -0700 (Tue, 27 Mar 2012) $' 
 * '$Revision: 29643 $'
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.kepler.build.modules.Module;

/**
 * Class that represents a configuration property within kepler
 */
public class ConfigurationProperty
{
  private Module module;
  private String name;
  private ConfigurationNamespace namespace;
  private String value;
  private List<ConfigurationProperty> propertiesList;
  private ConfigurationProperty parent;
  private boolean dirty;
  private boolean mutable;
  private Module originModule;
  public static final ConfigurationNamespace namespaceDefault = new ConfigurationNamespace("configuration");
  
  /**
   * constructor to build a configuration property for a module with a name and
   * a list of properties to load
   */
  public ConfigurationProperty(Module module, String name, List<ConfigurationProperty> propertiesList)
  {
    this(module, name, namespaceDefault, propertiesList);
  }
  
  /**
   * constructor to build a configuration property for a module with a name, 
   * namespace and a list of properties to nest within it
   */
  public ConfigurationProperty(Module module, String name, ConfigurationNamespace namespace, 
		  List<ConfigurationProperty> propertiesList)
  {
	  this(module, name, namespace, propertiesList, true);
  }

/**
   * constructor to build a configuration property for a module with a name, 
   * namespace and a list of properties to nest within it
   */
  public ConfigurationProperty(Module module, String name, ConfigurationNamespace namespace, 
		  List<ConfigurationProperty> propertiesList, boolean notifyListeners)
  {
    init(module, name, namespace, null);
    this.propertiesList = propertiesList;
    Iterator<ConfigurationProperty>itr = propertiesList.iterator();
    while (itr.hasNext())
    {
    	ConfigurationProperty cp = itr.next();
    	cp.setParent(this);
    	cp.setNamespace(notifyListeners, this.getNamespace());
    	cp.setOriginModule(module);
    }
  }
  
  /**
   * constructor to build a configuration property for a module with a name
   * and a single value
   */
  public ConfigurationProperty(Module module, String name, String value)
  {
    this(module, name, namespaceDefault, value); 
  }
  
  /**
   * constructor to build a configuration property for a module with a name
   * and a single value
   */
  public ConfigurationProperty(Module module, String name, ConfigurationNamespace namespace, String value)
  {
    init(module, name, namespace, value); 
  }
  
  /**
   * constructor to build a configuration property for a module with a name
   * but with no value or nested property list.
   */
  public ConfigurationProperty(Module module, String name)
  {
    this(module, name, namespaceDefault, (String)null);
  }
  
  /**
   * create a configuration property with one nested property
   */
  public ConfigurationProperty(Module module, String name, ConfigurationProperty property)
    throws NamespaceException, ConfigurationManagerException
  {
    this(module, name);
    addProperty(property);
  }
  
  /**
   * add a nested property to this configuration property
   */
  public void addProperty(ConfigurationProperty property)
    throws NamespaceException, ConfigurationManagerException
  {
    addProperty(property, false, true);
  }
  
  /**
   * add a nested property to this configuration property
   */
  public void addProperty(ConfigurationProperty property, boolean notifyListeners)
    throws NamespaceException, ConfigurationManagerException
  {
    addProperty(property, false, notifyListeners);
  }
  
  /**
   * add a nested property to this configuration property.  if ignoreMutable
   * is false, don't throw an exception if a property is mutable.  this method
   * is used by configuration readers to build the configuration and should
   * not be used in normal api calls.
   */
  protected void addProperty(ConfigurationProperty property, boolean ignoreMutable,
		  boolean notifyListeners) 
    throws NamespaceException, ConfigurationManagerException
  {
    if(!ignoreMutable && !mutable)
    { //check to see if the property is not mutable.  if it is not, throw
      //an exception
      throw new ConfigurationManagerException("The property " + getName() + 
        " is mutable and cannot be changed at runtime.");
    }
    
    checkNamespace(this, property);  
    propertiesList.add(property);
    property.setParent(this);
    setDirty(true);
    if (notifyListeners){
    	notifyListeners(this);
    }
  }
  
  /**
   * add a new property for a given module with a given name that has the given 
   * property nested within it.
   */
  public void addProperty(Module module, String name, ConfigurationProperty property)
    throws NamespaceException, ConfigurationManagerException
  {
    addProperty(module, name, namespaceDefault, property);
  }
  
  /**
   * add a new property for a given module with a given name that has the given 
   * property nested within it.
   */
  public void addProperty(Module module, String name, ConfigurationNamespace namespace, 
    ConfigurationProperty property)
    throws NamespaceException, ConfigurationManagerException
  {
    ConfigurationProperty cp = new ConfigurationProperty(module, name, namespace, (String)null);
    checkNamespace(this, cp);
    checkNamespace(this, property);
    cp.addProperty(property);
    addProperty(cp);
    setDirty(true);
    notifyListeners(this);
  }
  
  /**
   * add propertyToAdd before the beforeProperty in the ordering of the list of 
   * properties for this property
   * @param index index in this property's property list to add propertyToAdd
   * @param propertyToAdd the property to add
   */
  public void addPropertyAtIndex(int index, ConfigurationProperty propertyToAdd)
    throws NamespaceException, ConfigurationManagerException
  {
    if(!mutable)
    { //check to see if the property is not mutable.  if it is not, throw
      //an exception
      throw new ConfigurationManagerException("The property " + getName() + 
        " is mutable and cannot be changed at runtime.");
    }
    checkNamespace(this, propertyToAdd);
    propertiesList.add(index, propertyToAdd);
    propertyToAdd.setParent(this);
    setDirty(true);
    notifyListeners(this);
  }
  
  /**
   * add a nested property if the contents are not already present,
   * ignoring: namespaces, modules, originmodules. NOTE: if the
   * contents are the same, but in a different order, the property is
   * added.
   */
  public void addPropertyIfNotThere(ConfigurationProperty propertyToAdd)
      throws NamespaceException, ConfigurationManagerException
  {
      if(!containsProperty(propertyToAdd))
      {
          addProperty(propertyToAdd);
      }
  }
  
  /**
   * add a nested property at index if the contents are not already present,
   * ignoring: namespaces, modules, originmodules. NOTE: if the
   * contents are the same, but in a different order, the property is
   * added.
   */
  public void addPropertyIfNotThereAtIndex(int index, ConfigurationProperty propertyToAdd)
      throws NamespaceException, ConfigurationManagerException
  {
      if(!containsProperty(propertyToAdd))
      {
          addPropertyAtIndex(index, propertyToAdd);
      }
  }

  /**
   * remove the given property
   * 
   * @param propertyToRemove the property to remove
   * @return true if propertiesList contained this property
   */
  public boolean removeProperty(ConfigurationProperty propertyToRemove)
  {
    boolean removed = propertiesList.remove(propertyToRemove);
    setDirty(true);
    notifyListeners(this);
    return removed;
  }
  
  /**
   * return a list of nested properties within this property
   */
  public List<ConfigurationProperty> getProperties()
  {
    return propertiesList;
  }
  
  /**
   * return a list of nested properties that have a specific name.  Use 
   * dots to separate path names.  For example "some.config.path.value"
   */
  public List<ConfigurationProperty> getProperties(String name)
  {
    return getProperties(this, name, false);
  }
  
  /**
   * do a recursive get through this ConfigurationProperty
   * 
   * @param name the name to look for
   * @param recursive
   */
  public List<ConfigurationProperty> getProperties(String name, boolean recursive)
  {
    return getProperties(this, name, recursive);
  }
  
  /**
   * get properties from a configurationProperty with a given name.  If 
   * recursive is true, look through the entire structure of cp and return
   * all properties with the given name.  
   *
   * @param cp the ConfigurationProperty to get from
   * @param name the name to look for
   * @param recursive if true, look through the tree recursively.
   */
  public static List<ConfigurationProperty> getProperties(
    ConfigurationProperty cp, String name, boolean recursive)
  {
    //System.out.println("searching for " + name + " in " + cp.getName());
    List<ConfigurationProperty> propList = cp.getProperties();
    String[] s = processName(name);
    if(s.length > 1)
    {
      return getPropertiesWithPath(cp, s, true);
    }
    
    Vector<ConfigurationProperty> props = new Vector<ConfigurationProperty>();
    
    for(ConfigurationProperty prop : propList)
    {
      if(prop.getName().equals(name))
      {
        props.add(prop);
      }
      
      if(recursive)
      {
        List<ConfigurationProperty> recursiveProps = getProperties(prop, name, recursive);
        if(recursiveProps.size() > 0)
        {
          for(int j=0; j<recursiveProps.size(); j++)
          {
            props.add(recursiveProps.get(j));
          }
        }
      }
    }
    return props;
  }

  /**
   * if there are more than one property with the same name, just return
   * the first one. Return null if the property is not found
   */
  public ConfigurationProperty getProperty(String name)
  {
    return getProperty(name, 0);
  }
  
  /**
   * return a property with a given name at the specified index.  
   * Example:
   * prop1
   *   prop2=x
   *   prop2=y
   * getProperty("prop2", 1).getValue() == y
   * getProperty("prop2", 0).getValue() == x
   * returns null if the property does not exist
   */
  public ConfigurationProperty getProperty(String name, int index)
  {
    List propList = getProperties(name);
    //prettyPrintList(propList);
    if(propList.size() > index)
    {
      return (ConfigurationProperty)propList.get(index);
    }
    return null;
  }
  
  /**
   * get the property with the given index in the current list of properties
   */
  public ConfigurationProperty getProperty(int index)
  {
    return propertiesList.get(index);
  }
  
  /**
   * override originalProperty with newProperty. 
   *
   * @param originalProperty the property to override
   * @param newProperty the property to take the place of originalProperty
   * @param overrideNamespace true if you want to automatically set the namespace
   * of newProperty (along with all of its subproperties) to that of 
   * originalProperty.
   */
  public boolean overrideProperty(ConfigurationProperty originalProperty, 
    ConfigurationProperty newProperty, boolean overrideNamespace)
  {
    if(overrideNamespace)
    {
      newProperty.setNamespace(originalProperty.getNamespace(), true);
    }
    
    boolean replaced = replaceProperty(originalProperty, newProperty);
    notifyListeners(this);
    return replaced;
  }
  
  /**
   * returns a list of properties within this property that have a specific
   * name and value.  This will return the parent property of any properties
   * found.  Example:
   * 
   * &lt;a&gt;
   *   &lt;name&gt;jim&lt;/name&gt;
   *   &lt;otherprop&gt;XXX&lt;/otherprop&gt;
   * &lt;/a&gt;
   * &lt;b&gt;
   *   &lt;name&gt;bob&lt;/name&gt;
   *   &lt;otherprop&gt;XXX&lt;/otherprop&gt;
   * &lt;/b&gt;
   * &lt;c&gt;
   *   &lt;name&gt;jim&lt;/name&gt;
   *   &lt;otherprop&gt;XXX&lt;/otherprop&gt;
   * &lt;/c&gt;
   *
   * a call to findProperties("name", "jim") would return elements &lt;a&gt; and &lt;c&gt; 
   * in the list.
   * Note: the name parameter can NOT use the shorthand "dot" notation.
   * 
   * @param name the name of the property to find
   * @param value the value of the property to find
   * @param recursive true if you want a recursive (deep) search
   */
  public List<ConfigurationProperty> findProperties(String name, String value, 
    boolean recursive)
  {
    return findProperties(this, name, value, recursive);
  }
  
  /**
   * returns a list of properties within this property that have a specific
   * name and value.  This will return the parent property of any properties
   * found.  Example:
   * 
   * &lt;a&gt;
   *   &lt;name&gt;jim&lt;/name&gt;
   *   &lt;otherprop&gt;XXX&lt;/otherprop&gt;
   * &lt;/a&gt;
   * &lt;b&gt;
   *   &lt;name&gt;bob&lt;/name&gt;
   *   &lt;otherprop&gt;XXX&lt;/otherprop&gt;
   * &lt;/b&gt;
   * &lt;c&gt;
   *   &lt;name&gt;jim&lt;/name&gt;
   *   &lt;otherprop&gt;XXX&lt;/otherprop&gt;
   * &lt;/c&gt;
   *
   * a call to findProperties("name", "jim") would return elements &lt;a&gt; and &lt;c&gt; 
   * in the list.
   * Note: the name parameter can NOT use the shorthand "dot" notation.
   * Note: this method is non-recursive.
   * 
   * @param name the name of the property to find
   * @param value the value of the property to find
   */
  public List<ConfigurationProperty> findProperties(String name, String value)
  {
    return findProperties(name, value, false);
  }
  
  /**
   * return the value of this property.  if the value is not set this returns null
   */
  public String getValue()
  {
    return value;
  }
  
  /**
   * set the value of this property
   */
  public void setValue(String value)
    throws ConfigurationManagerException
  {
    if(!mutable)
    { //check to see if the property is not mutable.  if it is not, throw
      //an exception
      throw new ConfigurationManagerException("The property " + getName() + 
        " is mutable and cannot be changed at runtime.");
    }
    setDirty(true);
    this.value = value;
    notifyListeners(this);
  }
  
  /**
   * return true if this property has a string value
   */
  public boolean hasLeafValue()
  {
    if(value == null)
    {
      return false;
    }
    return true;
  }
  
  /**
   * return true if this property has nested properties
   */
  public boolean hasNestedProperties()
  {
    if(propertiesList.size() > 0)
    {
      return true;
    }
    return false;
  }
  
  /**
   * return the name of this property
   */
  public String getName()
  {
    return name;
  }
  
  /**
   * return the module associated with this property
   */
  public Module getModule()
  {
    return module;
  }
  
  /** 
   * return the namepace
   */
  public ConfigurationNamespace getNamespace()
  {
    return this.namespace;
  }
  
  /**
   * set dirty. generally other classes shouldn't call this
   * but it's a good way to force serialization when nothing has
   * actually changed.
   */
  public void setDirty(boolean dirty){
	  this.dirty = dirty;
  }
  
  /**
   * set the namespace of this property
   */
  public void setNamespace(boolean notifyListeners, ConfigurationNamespace namespace)
  {
    this.namespace = namespace;
    if (notifyListeners){
    	notifyListeners(this);
    }
  }
  
  /**
   * set the namespace of this property
   */
  public void setNamespace(ConfigurationNamespace namespace)
  {
    this.namespace = namespace;
    notifyListeners(this);
  }
  
  /**
   * sets the namespace. If recursive is true, set all subproperties' namespaces
   * as well.
   */
  public void setNamespace(ConfigurationNamespace namespace, boolean recursive)
  {
    setNamespace(namespace, recursive, true);
  }
  
  /**
   * sets the namespace. If recursive is true, set all subproperties' namespaces
   * as well.
   */
  public void setNamespace(ConfigurationNamespace namespace, boolean recursive, boolean notifyListeners)
  {
    setNamespace(this, namespace, recursive, notifyListeners);
    if (notifyListeners){
    	notifyListeners(this);
    }
  }
  
  /**
   * returns a fully qualified name including the module and namespace
   */
  public String getFullName()
  {
    String modName = module.getName() + ".";
    String ns = "";
    if(namespace != null)
    {
      ns = namespace + ".";
    }
    
    return modName + ns + name;
  }
  
  /**
   * returns true if this propert is mutable.  A mutable property is one that
   * can be changed and realized without restarting the application.  Only mutable
   * properties can be overwritten at runtime.  A property is assumed to be mutable
   * unless it is set not to be.
   */
  public boolean isMutable()
  {
    return mutable;
  }
  
  /**
   * return true if this ConfigurationProperty contains a property with the given name..  
   * Set recursive to true if you want to search this property's nested structure.
   * @param property the property
   */
  public boolean containsProperty(String name, boolean recursive)
  {
    if(!recursive)
    {
      ConfigurationProperty cp = getProperty(name);
      if(cp == null)
      {
        return false;
      }
      return true;
    }
    else
    {
      for(ConfigurationProperty cp : propertiesList)
      {
        if(cp.getName().equals(name))
        {
          return true;
        }
        if(cp.hasNestedProperties())
        {
          if(cp.containsProperty(name, recursive))
          {
            return true;
          }
        }
      }
      return false;
    }
  }

  /**
   * returns true if this property contains a nested property,
   * ignoring: namespaces, modules, originmodules. NOTE: if the
   * contents are the same, but in a different order, returns false.
   */
  public boolean containsProperty(ConfigurationProperty nestedProperty)
  {
      for(ConfigurationProperty subProperty : getProperties())
      {
          //System.out.println("checking");
          //System.out.println(subProperty.toString(true, false, false, false));
          //System.out.println(nestedProperty.toString(true, false, false, false));
          if(subProperty.equalsContents(nestedProperty))
          {
              //System.out.println("found! not adding property.");
              return true;
          }
      }

      return false;
  }

  /**
   * returns true if the property contents are identical to this one,
   * ignoring: namespaces, modules, originmodules. NOTE: if the contents
   * are the same, but in a different order, returns false.
   */
  public boolean equalsContents(ConfigurationProperty property)
  {
    if(property == null)
    {
        return false;
    }
    else if(property == this)
    {
        return true;
    }
    else
    {
        return toStringContents().equals(property.toStringContents());
    }

  }

  /**
   * return the xml version of this property
   */
  public String getXML()
  {
    return getXML(this, "");
  }
  
  public String toString(boolean recursive)
  {
    StringBuilder s = new StringBuilder("{name=" +
        name + ", module=" + module.getName() + ", namespace=" +
        namespace + ", value=" + value);
    if(recursive && propertiesList.size() > 0)
    {
      s.append(", propertyList={");
      for(ConfigurationProperty cp : propertiesList)
      {
        s.append(cp.toString());
      }
      s.append("}");
    }
    
    s.append("}");
    return s.toString();
  }
  
  /**
   * return a string representation of this property
   */
  public String toString()
  {
    return toString(true);
  }
  
  /** Returns the string contents of this property without the namespace,
   *  module name, and origin module name (if present).
   */
  public String toStringContents()
  {
      return toString(true, false, false, false);
  }

  /**
   * returns a string representation of this property
   * @param recursive if true, string includes all sub-properties
   * @param includeNamespace if true, string includes the namespace
   * @param includeModule if true, string include module name
   * @param includeOriginModule if true, string includes originModule name
   */
  public String toString(boolean recursive, boolean includeNamespace, boolean includeModule, boolean includeOriginModule)
  {
    String s;
    s = "{name=" + name;

    if(includeModule)
    {
        s += ", module=" + module.getName();
    }

    if(includeNamespace)
    {
        s += ", namespace=" + namespace;
    }

    s += ", value=" + value;

    if(recursive && propertiesList.size() > 0)
    {
      s += ", propertyList={";
      for(ConfigurationProperty cp : propertiesList)
      {
        if(includeOriginModule || !cp.getName().equals("originModule"))
        {
          s += cp.toString(recursive, includeNamespace, includeModule, includeOriginModule);
        }
      }
      s +="}";
    }

    s += "}";
    return s;
  }

  /**
   * print this configurationProperty nicely showing the hierarchy
   */
  public void prettyPrint()
  {
    System.out.println(prettyPrint(this, ""));
  }
  
  /**
   * returns true if this property has changed since it was last saved
   * @param recursive true if the dirty flag should be searched for in any
   * contained configuration property
   */
  public boolean isDirty(boolean recursive)
  {
    if(!recursive)
    {
      return this.dirty;
    }
    
    return isDirty(this);
  }
  
  /**
   * returns this congifuration property's dirty flag
   */
  public boolean isDirty()
  {
    return isDirty(false);
  }
  
  /**
   * returns the parent propert of this property.  null if this property
   * does not have a parent.
   */
  public ConfigurationProperty getParent()
  {
    return this.parent;
  }
  
  /**
   * set the namespace on a configurationProperty
   */
  public static void setNamespace(ConfigurationProperty cp, ConfigurationNamespace namespace, 
		  boolean recursive)
  {
	  setNamespace(cp, namespace, recursive, true);
  }
  
  /**
   * set the namespace on a configurationProperty
   */
  public static void setNamespace(ConfigurationProperty cp, ConfigurationNamespace namespace, 
		  boolean recursive, boolean notifyListeners)
  {
    cp.setNamespace(notifyListeners, namespace);
    if(recursive)
    {
      List<ConfigurationProperty> propList = cp.getProperties();
      for(ConfigurationProperty prop : propList)
      {
        setNamespace(prop, namespace, recursive, notifyListeners);
      }
    }
  }
  
  /**
   * Search a list of properties for properties that contain a name and a value
   */
  public static List<ConfigurationProperty> findProperties(
    List<ConfigurationProperty> properties, String name, String value, boolean recursive)
  {
    Vector<ConfigurationProperty> results = new Vector<ConfigurationProperty>();
    for(ConfigurationProperty cp : properties)
    {
      //System.out.println("searching " + cp.getName());
      List<ConfigurationProperty> subresults = findProperties(cp, name, value, recursive);
      for(ConfigurationProperty p : subresults)
      {
        if(!checkForDuplicates(results, p))
        {
          results.add(p);
        }
      }
    }
    return results;
  }
  
  /**
   * return a list of properties that have a name that matches name and a value
   * that matches value within property.  If recursive is set to true, do a deep
   * search of the property
   */
  public static List<ConfigurationProperty> findProperties(ConfigurationProperty property, 
    String name, String value, boolean recursive)
  {
    Vector<ConfigurationProperty> found = new Vector<ConfigurationProperty>();
    List<ConfigurationProperty> propertiesList = property.getProperties();
    for(ConfigurationProperty cp : propertiesList)
    {
      //System.out.println("cp: " + cp.getName());
      if(cp.getName().equals(name) && cp.getValue().equals(value) && !checkForDuplicates(found, cp))
      {
        found.add(cp.getParent());
      }

      if(recursive)
      {
        List<ConfigurationProperty> l = findProperties(cp, name, value, recursive);
        //simplePrintList(l);
        for(int j=0; j<l.size(); j++)
        {
          if(!checkForDuplicates(found, l.get(j)))
          {
            found.add(l.get(j));
          }
        }
      }
    }
    
    return found;
  }
  
  /**
   * pretty print a list of configuration properties
   */
  public static void prettyPrintList(List<ConfigurationProperty> l)
  {
    System.out.println("[");
    for(int i=0; i<l.size(); i++)
    {
      System.out.print(i + " = {\n");
      ConfigurationProperty cp = l.get(i);
      System.out.println("module: " + cp.getModule().getName() + 
        " name: " + cp.getName() + 
        " namespace: " + cp.getNamespace().toString() +
        " value: " + cp.getValue());
      cp.prettyPrint();
      System.out.print("}");
      if(i != l.size() - 1)
      {
        System.out.print(",\n");
      }
    }
    System.out.println("]");
  }
  
  public static void simplePrintList(List<ConfigurationProperty> l)
  {
    System.out.print("\n[");
    for(int i=0; i<l.size(); i++)
    {
      System.out.print(i + " = {");
      ConfigurationProperty cp = l.get(i);
      System.out.print("module: " + cp.getModule().getName() + " namespace: " 
        + cp.getNamespace().toString() + " name: " + cp.getName());
      System.out.print("}");
      if(i != l.size() - 1)
      {
        System.out.print(",\n");
      }
    }
    System.out.println("]");
  }
  
  /**
   * return a list of strings of the values of a property with a given name in 
   * the list of properties
   */
  public static List<String> getValueList(List<ConfigurationProperty> propList, 
    String propertyName, boolean recursive)
  {
    Vector<String> v = new Vector<String>();
    for(ConfigurationProperty cp : propList)
    {
      if(cp.getName().equals(propertyName))
      {
        v.add(cp.getValue());
      }
      
      if(recursive)
      {
        List<String> l = getValueList(cp.getProperties(), propertyName, recursive);
        for(int j=0; j<l.size(); j++)
        {
          v.add(l.get(j));
        }
      }
    }
    return v;
  }
  
  /**
   *
   */
  public void setOriginModule(Module m)
  {
    this.originModule = m;
  }
  
  /**
   *
   */
  public Module getOriginModule()
  {
    return this.originModule;
  }
  
  /**
   * set the parent property of this property
   */
  public void setParent(ConfigurationProperty property)
  {
    this.parent = property;
  }
  
  /**
   * reset the dirty flag
   * @param recursive set to true if all properties that this property
   * contains should also be reset
   */
  protected void resetDirty(boolean recursive)
  {
    setDirty(false);
    
    if(!recursive)
    {
      return;
    }
    
    Iterator it = getProperties().iterator();
    while(it.hasNext())
    {
      ConfigurationProperty cp = (ConfigurationProperty)it.next();
      cp.resetDirty(true);
    }
  }
    
  /**
   * recursively return all properties with a path denoted in s.  If returnChildren
   * is true, return the found child nodes.  If it is false, return the 
   * found parent nodes.  For example, if you are getting a/b/c, if returnChildren
   * is true, return a list of the c's.  If returnChildren is false,
   * return a list of the a's.  
   */
  protected static List<ConfigurationProperty> getPropertiesWithPath(ConfigurationProperty property,
    String[] s, boolean returnChildren)
  {
    if(returnChildren)
    { //return the found children nodes
      List<ConfigurationProperty> properties = getProperties(property, s[0], true);
      for(int i=1; i<s.length; i++)
      {
        properties = getPropertiesFromList(properties, s[i]);
      }
      
      return properties;
    }
    
    //return the found parent nodes
    List<ConfigurationProperty> properties = getProperties(property, s[s.length - 1], true);
    for(int i=s.length - 2; i>=0; i--)
    {
      properties = getParentPropertiesFromList(properties, s[i]);
    }
    
    return properties;
  }
  
  /**
   * set whether this property is mutable. A mutable property is one that
   * can be changed and realized without restarting the application.  Only mutable
   * properties can be overwritten at runtime.  A property is assumed to be mutable
   * unless it is set not to be.
   */
  protected void setMutable(boolean mutable)
  {
    this.mutable = mutable;
  }
  
  /**
   * searches a list of properties for properties of a given name.  This is
   * not recursive.
   * If returnChild is true, return the child elements instead of the parents
   */
  private static List<ConfigurationProperty> getParentPropertiesFromList(
    List<ConfigurationProperty> propertyList, String name)
  {
    Vector<ConfigurationProperty> results = new Vector<ConfigurationProperty>();
    for(ConfigurationProperty prop : propertyList)
    {
      //System.out.println("prop.parent: " + prop.getParent().getName());
      if(prop.getParent().getName().equals(name))
      {
        results.add(prop.getParent());
      }
    }
    return results;
  }
  
  /**
   * Note: callers should notifyListeners.
   * 
   * replace a specific property within a property's subproperties and 
   * return the index.  This method is recursive.
   * 
   * @param findProperty the property to look for
   * @param replaceProperty the property to replace findProperty with
   * @return boolean true if the property was found and replaced
   */
  private boolean replaceProperty(ConfigurationProperty findProperty, 
    ConfigurationProperty replaceProperty)
  {
    boolean found = false;
    for(int i=0; i<this.propertiesList.size(); i++)
    {
      ConfigurationProperty cp = this.propertiesList.get(i);
      if(cp == findProperty)
      {
        this.propertiesList.remove(i);
        this.propertiesList.add(i, replaceProperty);
        return true;
      }
      
      List l = cp.getProperties();
      if(l.size() > 0)
      {
        found = cp.replaceProperty(findProperty, replaceProperty);
        if(found)
        {
          break;
        }
      }
    }
    
    return found;
  }
  
  /**
   * return true if v contains cp
   */
  private static boolean checkForDuplicates(Vector<ConfigurationProperty> v, ConfigurationProperty cp)
  {
    for(ConfigurationProperty p : v)
    {
      if(p == cp)
      {
        return true;
      }
    }
    return false;
  }
  
  /**
   * return true of the property or any of its sub properties are dirty
   */
  private boolean isDirty(ConfigurationProperty p)
  {
    if(p.isDirty())
    {
      return true;
    }
    
    List l = p.getProperties();
    Iterator it = l.iterator();
    while(it.hasNext())
    {
      ConfigurationProperty cp = (ConfigurationProperty)it.next();
      if(isDirty(cp))
      {
        return true;
      }
      else
      {
        continue;
      }
    }
        
    return false;
  }
  
  /**
   * return an xml representation of this property
   */
  private static String getXML(ConfigurationProperty cp, String spaces)
  {
    String s = "";
    String name = cp.getName();
    Iterator it = cp.getProperties().iterator();
    String value = cp.getValue();
    
    s += spaces;
    s += "<" + name + ">\n";
    
    if(!cp.isMutable())
    {
      s += spaces + "  <mutable>false</mutable>\n";
    }
    
    Module originModule = cp.getOriginModule();
    if (cp.getParent() == null)
    {
    	System.out.println("ERROR ConfigurationProperty getXML(cp, spaces) cp.getParent() == null for cp:");
    	cp.prettyPrint();
    }
    
    if(originModule != null && !originModule.getName().equals(cp.getParent().getModule().getName()))
    {
      s += spaces + "  <originModule>" + originModule.getName() + "</originModule>\n";
    }
    
    if(value != null && !value.equals(""))
    {
      s += spaces + "  " + value + "\n";
    }
    
    while(it.hasNext())
    {
      ConfigurationProperty c = (ConfigurationProperty)it.next();
      // don't add originModule property more than once per property
      if (!c.getName().equals("originModule")){
    	  s += getXML(c, spaces + "  ");
      }
    }
    
    s += spaces + "</" + name + ">\n";
    
    return s;
  }
  
  /**
   * format the cp nicely
   */
  private static String prettyPrint(ConfigurationProperty cp, String spaces)
  {
    String s = "";
    String name = cp.getName();
    Iterator it = cp.getProperties().iterator();
    String value = cp.getValue();
    String originModName = cp.getOriginModule().getName();
    
    boolean dirty = cp.isDirty();
    s += spaces;
    s += name + "(" + originModName + ")";
    if(dirty)
    {
      s += "(DIRTY)";
    }
    
    if(value != null && !value.equals(""))
    {
      s += " = " + value;
    }
    s += "\n";
    
    while(it.hasNext())
    {
      ConfigurationProperty c = (ConfigurationProperty)it.next();
      s += prettyPrint(c, spaces + "  ");
    }
    return s;
  }
  
  /**
   * process a name with . notations into a string array
   */
  protected static String[] processName(String name)
  {
    String[] s;
    if(name.indexOf(".") != -1)
    {
      StringTokenizer st = new StringTokenizer(name, ".");
      int num = st.countTokens();
      s = new String[num];
      int i = 0;
      while(st.hasMoreTokens())
      {
        String ss = st.nextToken();
        s[i] = ss;
        i++;
      }
      return s;
    }
    else
    {
      s = new String[1];
      s[0] = name;
      return s;
    }
  }
  
  /**
   * recursively return the properties in the list that match name
   */
  private static List<ConfigurationProperty> getPropertiesFromList(
    List<ConfigurationProperty> propertyList, String name)
  {
    if(propertyList == null)
    {
      return null;
    }
    Vector<ConfigurationProperty> results = new Vector<ConfigurationProperty>();
    for(ConfigurationProperty cp : propertyList)
    {
      List<ConfigurationProperty> cpPropList = cp.getProperties();
      for(ConfigurationProperty cpProp : cpPropList)
      {
        if(cpProp.getName().equals(name))
        {
          results.add(cpProp);
        }
      }
    }
    return results;
  }
  
  /**
   * initialize this property
   */
  private void init(Module module, String name, ConfigurationNamespace namespace, String value)
  {
    this.propertiesList = new ArrayList<ConfigurationProperty>();
    this.module = module;
    this.originModule = module;
    this.name = name;
    this.namespace = namespace;
    this.value = value;
    this.parent = null;
    this.mutable = true;
  }
  
  /**
   * check to see if prop1 and prop2 are in the same namespace.  If they aren't
   * throw a NamespaceException
   */
  private void checkNamespace(ConfigurationProperty prop1, ConfigurationProperty prop2)
    throws NamespaceException
  {
    ConfigurationNamespace p1ns = prop1.getNamespace();
    ConfigurationNamespace p2ns = prop2.getNamespace();

    if(p1ns == null && p2ns == null)
    {
      return;
    }
    else if(p1ns == null || p2ns == null)
    {
      throw new NamespaceException("The namespace of configuration property " +
        prop1.getFullName() + " is not in the same namespace as " + 
        prop2.getFullName() + ".  One namespace is null and the other is not.");
    }
    else if(!p1ns.equals(p2ns))
    {
      throw new NamespaceException("The namespace of configuration property " +
        prop1.getFullName() + " is not in the same namespace as " + 
        prop2.getFullName() + ".  Properties must be in the same namespace " +
        "to be nested.");
    }
  }
  
  /**
   * notify listeners that the property has changed.
   */
  private void notifyListeners(ConfigurationProperty property)
  {
    try
    {
      ConfigurationManager.getInstance(false).notifyListeners(property);
    }
    catch(Exception e)
    {
      throw new RuntimeException("Could not notify listeners: " + e.getMessage());
    }
  }  
}