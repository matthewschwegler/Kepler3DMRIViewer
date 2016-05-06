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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kepler.build.modules.Module;

/** A collection of utilities to manage configuration properties.
 * 
 * @author Daniel Crawl
 * @version $Id: ConfigurationUtilities.java 29643 2012-03-27 21:09:20Z barseghian $
 */
public class ConfigurationUtilities 
{
    
    /** Return a map from a list of name value pairs contained in a property.
     */
    public static Map<String,String> getPairsMap(ConfigurationProperty property)
    {
        Map<String,String> retval = new HashMap<String,String>();
        
        List<ConfigurationProperty> nameList =
            property.getProperties("pair.name");
        for(ConfigurationProperty nameProperty : nameList)
        {
            String value = 
                nameProperty.getParent().getProperty("value").getValue();
            retval.put(nameProperty.getValue(), value);
        }
        return retval;
    }

    /** Copy all the properties in a namespace from one module to another. */
    public static void copyAllProperties(String namespaceStr, String destModuleName, String srcModuleName)
    {
        copyProperties(namespaceStr, destModuleName, srcModuleName, null, null);
    }

    /** Copy a property or set of properties from one module to another using the default namespace.
     *  @param destModuleName name of destination module
     *  @param srcModuleName name of source module
     *  @param destPropertyName name of destination property
     *  @param srcPropertyName name of property or properties in source module to copy. Can use "a.b.c"
     *  format to denote a nested name.
     */
    public static void copyProperties(String destModuleName, String srcModuleName,
            String destPropertyName, String srcPropertyNames)
    {
        copyProperties(ConfigurationProperty.namespaceDefault.getNamespace(), destModuleName, srcModuleName,
                destPropertyName, srcPropertyNames);
    }
         
    /** Copy a property or set of properties from one module to another for a specific namespace.
     *  @param namespaceStr the namespace 
     *  @param destModuleName name of destination module
     *  @param srcModuleName name of source module
     *  @param destPropertyName name of destination property
     *  @param srcPropertyName name of property or properties in source module to copy. Can use "a.b.c"
     *  format to denote a nested name.
     */
    public static void copyProperties(String namespaceStr, String destModuleName, String srcModuleName,
        String destPropertyName, String srcPropertyNames)
    {
        final ConfigurationNamespace namespace = new ConfigurationNamespace(namespaceStr);

        final ConfigurationManager manager = ConfigurationManager.getInstance();
        
        final Module destModule = ConfigurationManager.getModule(destModuleName);
        final ConfigurationProperty destRoot = manager.getProperty(destModule, namespace);
        final ConfigurationProperty destProperty;
        if(destPropertyName != null)
        {
            destProperty = destRoot.getProperty(destPropertyName);
        }
        else
        {
            destProperty = destRoot;
        }

        final Module srcModule = ConfigurationManager.getModule(srcModuleName);
        final ConfigurationProperty srcRoot = manager.getProperty(srcModule, namespace);
        final List<ConfigurationProperty> srcProperties;
        if(srcPropertyNames != null)
        {
            srcProperties = srcRoot.getProperties(srcPropertyNames);
        }
        else
        {
            srcProperties = srcRoot.getProperties();
        }

        for(ConfigurationProperty property : srcProperties)
        {
            try 
            {
                // only add the property if it is not already there
                destProperty.addPropertyIfNotThere(property);
            }
            catch (Exception e)
            {
                System.out.println("Could not add property: " + e.getMessage());
            }
        }        
    }
    
    /** Copy a property or set of properties from one module to specific top level indices of another 
     *  for a specific namespace.
     *  @param namespaceStr the namespace 
     *  @param destModuleName name of destination module
     *  @param srcModuleName name of source module
     *  @param destPropertyName name of destination property
     *  @param srcPropertyName name of property or properties in source module to copy. Can use "a.b.c"
     *  format to denote a nested name.
     *  @param indices top level destination indices where properties will be placed
     * @throws Exception 
     */
    public static void copyPropertiesToIndices(String namespaceStr, String destModuleName, String srcModuleName,
        String destPropertyName, String srcPropertyNames, ArrayList<Integer> indices) throws Exception
    {
    	
        final ConfigurationNamespace namespace = new ConfigurationNamespace(namespaceStr);

        final ConfigurationManager manager = ConfigurationManager.getInstance();
        
        final Module destModule = ConfigurationManager.getModule(destModuleName);
        final ConfigurationProperty destRoot = manager.getProperty(destModule, namespace);
        final ConfigurationProperty destProperty;
        if(destPropertyName != null)
        {
            destProperty = destRoot.getProperty(destPropertyName);
        }
        else
        {
            destProperty = destRoot;
        }

        final Module srcModule = ConfigurationManager.getModule(srcModuleName);
        final ConfigurationProperty srcRoot = manager.getProperty(srcModule, namespace);
        final List<ConfigurationProperty> srcProperties;
        if(srcPropertyNames != null)
        {
            srcProperties = srcRoot.getProperties(srcPropertyNames);
        }
        else
        {
            srcProperties = srcRoot.getProperties();
        }

        if (srcProperties.size() != indices.size()){
			throw new Exception("ConfigurationUtilities.copyPropertiesToIndices could not add " +
					"properties because srcProperties.size():"+ srcProperties.size() + " != " +
					"indices.size()" + indices.size());
        }
        
        int index = 0;
        for(ConfigurationProperty property : srcProperties)
        {
            try 
            {
                // only add the property if it is not already there
                destProperty.addPropertyIfNotThereAtIndex(indices.get(index), property);
                index++;
            }
            catch (Exception e)
            {
                System.out.println("Could not add property: " + e.getMessage());
            }
        }        
    }
    
}