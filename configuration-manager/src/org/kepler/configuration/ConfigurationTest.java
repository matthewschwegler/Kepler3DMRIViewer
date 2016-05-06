/*
 * Copyright (c) 2011 The Regents of the University of California.
 * All rights reserved.
 *
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.XMLConfiguration;
import org.kepler.build.modules.Module;

/**
 * Created by David Welker.
 */
public class ConfigurationTest
{
    public static String trimmedKey(String key)
    {
        if( !key.contains(".") )
            return key;
        return key.substring(key.indexOf('.') + 1, key.length());
    }

    public static boolean addMatch(List<String> addKeys, List<String> addedKeys, XMLConfiguration addXmlConfig, XMLConfiguration addedXmlConfig)
    {
        if( addKeys.size() != addedKeys.size() )
            return false;

        HashMap<String, String> matchedKeys = new HashMap<String,String>();
        for( String addKey : addKeys )
        {
            boolean keyMatchFound = false;
            for( String addedKey : addedKeys )
            {
                if( trimmedKey(addKey).equals(trimmedKey(addedKey)) )
                {
                    keyMatchFound = true;
                    matchedKeys.put(addKey, addedKey);
                }
            }
            if( !keyMatchFound )
                return false;
        }

        for( Map.Entry<String,String> entry : matchedKeys.entrySet() )
        {
            Object addProperty = addXmlConfig.getProperty(entry.getKey());
            Object addedProperty = addedXmlConfig.getProperty(entry.getValue());

            if( !addProperty.equals(addedProperty) )
                return false;
        }

        return true;
    }

    public static void addTest() throws Exception
    {
        System.out.println("AddTest");
        File configDir = Module.make("configuration-manager").getConfigurationsDir();
        File configDirectivesDir = new File( configDir.getParentFile(), "config-directives" );
        System.out.println(configDir);
        System.out.println(configDirectivesDir);
        System.out.println();

        File originalXml = new File(configDir, "original.xml");
        File addXml = new File(configDirectivesDir, "add.xml");
        File addedXml = new File(configDirectivesDir, "added.xml");
        File newXml = new File(configDirectivesDir, "new.xml");

        XMLConfiguration xmlconfig = new XMLConfiguration();
        xmlconfig.setDelimiterParsingDisabled(true);
        xmlconfig.load(originalXml);

        Iterator i;

        XMLConfiguration addXmlConfig = new XMLConfiguration();
        addXmlConfig.setDelimiterParsingDisabled(true);
        addXmlConfig.load(addXml);

        XMLConfiguration addedXmlConfig = new XMLConfiguration();
        addedXmlConfig.setDelimiterParsingDisabled(true);
        if( addedXml.exists() )
            addedXmlConfig.load(addedXml);

        i = addXmlConfig.getKeys();
        if( !i.hasNext() )
            return;

        List<String> firstParts = new ArrayList<String>();
        while( i.hasNext() )
        {
            String key = (String)i.next();
            if( key.contains(".") )
            {
                String candidate = key.substring(0, key.indexOf('.'));
                if( !firstParts.contains(candidate))
                    firstParts.add(candidate);
            }
        }

        for( String firstPart : firstParts )
        {
            System.out.println("firstPart = " + firstPart);
            int maxAddIndex = addXmlConfig.getMaxIndex(firstPart);
            int maxAddedIndex = addedXmlConfig.getMaxIndex(firstPart);
            int addIndex = xmlconfig.getMaxIndex(firstPart) + 1;

            List<String> removeKeys = new ArrayList<String>();
            for( int j = 0; j <= maxAddIndex; j++ )
            {
                List<String> addKeys = new ArrayList<String>();
                Iterator x1 = addXmlConfig.getKeys(firstPart+"("+j+")");
                while( x1.hasNext() )
                {
                    String key = (String)x1.next();
                    addKeys.add(key);
                }
                for( int k = 0; k <= maxAddedIndex; k++ )
                {
                    List<String> addedKeys = new ArrayList<String>();
                    Iterator x2 = addedXmlConfig.getKeys(firstPart+"("+k+")");
                    while( x2.hasNext() )
                    {
                        String key = (String)x2.next();
                        addedKeys.add(key);
                    }

                    if( addMatch(addKeys, addedKeys, addXmlConfig, addedXmlConfig) )
                    {
                        for( String addKey : addKeys )
                            removeKeys.add(addKey);
                    }
                }
            }
            for( int j = removeKeys.size() - 1; j >= 0; j-- )
            {
                String removeKey = removeKeys.get(j);
                addXmlConfig.clearProperty(removeKey);
            }

            System.out.println("Adding config...");
            for( int j = 0; j <= maxAddIndex; j++ )
            {
                String addXMLKey = firstPart + "("+j+")";
                i = addXmlConfig.getKeys(addXMLKey);
                while( i.hasNext() )
                {
                    String addXmlConfigKey = (String)i.next();
                    String lastPart = addXmlConfigKey.substring(addXmlConfigKey.indexOf('.')+1,addXmlConfigKey.length());
                    String originalXmlConfigKey = firstPart + "("+(addIndex+j)+")."+lastPart;
                    String addedXmlConfigKey = firstPart + "("+(maxAddedIndex+1+j)+")."+lastPart;
                    xmlconfig.addProperty(originalXmlConfigKey, addXmlConfig.getProperty(addXmlConfigKey));
                    addedXmlConfig.addProperty(addedXmlConfigKey, addXmlConfig.getProperty(addXmlConfigKey));
                }
            }
        }

        System.out.println("Simple adds...");
        List<String> addedKeys = new ArrayList<String>();
        i = addedXmlConfig.getKeys();
        while( i.hasNext() )
            addedKeys.add((String)i.next());

        i = addXmlConfig.getKeys();
        while( i.hasNext() )
        {
            String addKey = (String)i.next();
            if( addKey.contains(".") )
                continue;
            Object value = addXmlConfig.getProperty(addKey);
            if( addedKeys.contains(addKey) )
            {
                if( addedXmlConfig.getProperty(addKey).equals(value) )
                    continue;
            }

            xmlconfig.addProperty(addKey, value);
            addedXmlConfig.addProperty(addKey, value);
        }


        addedXmlConfig.save(addedXml);
        xmlconfig.save(newXml);
    }

    public static void changeTest() throws Exception
    {

        System.out.println("ChangeTest");
        File configDir = Module.make("configuration-manager").getConfigurationsDir();
        File configDirectivesDir = new File( configDir.getParentFile(), "config-directives" );
        System.out.println(configDir);
        System.out.println(configDirectivesDir);
        System.out.println();

        File originalXml = new File(configDir, "original.xml");
        File changeXml = new File(configDirectivesDir, "change.xml");
        File changedXml = new File(configDirectivesDir, "changed.xml");

        XMLConfiguration originalXmlConfig = new XMLConfiguration();
        originalXmlConfig.setDelimiterParsingDisabled(true);
        originalXmlConfig.load(originalXml);

        Iterator i;

        i = originalXmlConfig.getKeys();

        System.out.println("Original.xml:");
        while( i.hasNext() )
        {
            String key = (String)i.next();
            System.out.println(key + " = " + originalXmlConfig.getProperty(key));
        }
        System.out.println();

        XMLConfiguration changeXmlConfig = new XMLConfiguration();
        changeXmlConfig.setDelimiterParsingDisabled(true);
        changeXmlConfig.load(changeXml);

        XMLConfiguration changedXmlConfig = new XMLConfiguration();
        changedXmlConfig.setDelimiterParsingDisabled(true);
        if( changedXml.exists() )
            changedXmlConfig.load(changedXml);

        i = changeXmlConfig.getKeys();

        System.out.println("Change.xml:");
        while( i.hasNext() )
        {
            String key = (String)i.next();
            System.out.println(key + " = " + changeXmlConfig.getProperty(key));
        }
        System.out.println();

        i = changeXmlConfig.getKeys();
        while( i.hasNext() )
        {
            String key = (String)i.next();
            Object value = changeXmlConfig.getProperty(key);
            Object changed = changedXmlConfig.getProperty(key);
            if( changed == null || !value.equals(changed) )
            {
                originalXmlConfig.setProperty(key,value);
                changedXmlConfig.setProperty(key,value);
            }
        }

        i = originalXmlConfig.getKeys();

        System.out.println("New Config:");
        while( i.hasNext() )
        {
            String key = (String)i.next();
            System.out.println(key + " = " + originalXmlConfig.getProperty(key));
        }
        System.out.println();

        i = changedXmlConfig.getKeys();

        System.out.println("Changed.xml");
        while( i.hasNext() )
        {
            String key = (String)i.next();
            System.out.println(key + " = " + changedXmlConfig.getProperty(key));
        }
        System.out.println();

        changedXmlConfig.save(changedXml);
    }

    public static void removeTest() throws Exception
    {

        System.out.println("RemoveTest");
        File configDir = Module.make("configuration-manager").getConfigurationsDir();
        File configDirectivesDir = new File( configDir.getParentFile(), "config-directives" );
        System.out.println(configDir);
        System.out.println(configDirectivesDir);
        System.out.println();

        File originalXml = new File(configDir, "original.xml");
        File removeXml = new File(configDirectivesDir, "remove.xml");
        File removedXml = new File(configDirectivesDir, "removed.xml");
        File newXml = new File(configDirectivesDir, "new.xml");

        XMLConfiguration originalXmlConfig = new XMLConfiguration();
        originalXmlConfig.setDelimiterParsingDisabled(true);
        originalXmlConfig.load(originalXml);

        Iterator i;

        XMLConfiguration removeXmlConfig = new XMLConfiguration();
        removeXmlConfig.setDelimiterParsingDisabled(true);
        removeXmlConfig.load(removeXml);

        XMLConfiguration removedXmlConfig = new XMLConfiguration();
        removedXmlConfig.setDelimiterParsingDisabled(true);
        if( removedXml.exists() )
            removedXmlConfig.load(removedXml);

        i = removeXmlConfig.getKeys();
        while( i.hasNext() )
        {
            String key = (String)i.next();
            Object value = removeXmlConfig.getProperty(key);
            Object removed = removedXmlConfig.getProperty(key);
            if( removed == null || !value.equals(removed) )
            {
                originalXmlConfig.clearProperty(key);
                removedXmlConfig.setProperty(key,value);
            }
        }

        removedXmlConfig.save(removedXml);
        originalXmlConfig.save(newXml);
    }


    public static void main(String[] args) throws Exception
    {
        addTest();
        //changeTest();
        //removeTest();
    }

}
