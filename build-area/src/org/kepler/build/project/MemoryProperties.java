/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author$'
 * '$Date$'
 * '$Revision$'
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
package org.kepler.build.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by David Welker.
 * Date: 12/18/10
 * Time: 2:11 PM
 */
public class MemoryProperties
{
    private static MemoryProperties instance;

    private Properties memoryProperties = new Properties();
    private File memoryPropertiesFile;
    
    // if memory.xml doesn't already exist, these are used in creating it:
    private static final String MIN_MEMORY = "256m"; //Xms, the initial heap size
    private static final String MAX_MEMORY = "1000m";
    private static final String DEFAULT_STACK_SIZE = "1m";

    private MemoryProperties()
    {
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ? ProjectLocator.getUserBuildDir() : ProjectLocator.getBuildDir();
        memoryPropertiesFile = new File(buildDir, "settings/memory.xml");
        if( memoryPropertiesFile.exists() )
        {
            try
            {
            	FileInputStream stream = null;
            	try {
            		stream = new FileInputStream(memoryPropertiesFile);
            		memoryProperties.loadFromXML(stream);
            	} finally {
            		if(stream != null) {
            			stream.close();
            		}
            	}
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            memoryProperties.put("min", MIN_MEMORY);
            memoryProperties.put("max", MAX_MEMORY);
            memoryProperties.put("stack", DEFAULT_STACK_SIZE);
            writeMemoryProperties("Default Memory Properties");
        }
    }

    private void writeMemoryProperties(String comment)
    {

        try
        {
            memoryPropertiesFile.getParentFile().mkdirs();
            memoryPropertiesFile.createNewFile();
            FileOutputStream stream = null;
            try {
            	stream = new FileOutputStream(memoryPropertiesFile);
            	memoryProperties.storeToXML(stream, comment);
            } finally {
            	if(stream != null) {
            		stream.close();
            	}
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private static MemoryProperties instance()
    {
        if( instance == null )
            instance = new MemoryProperties();
        return instance;
    }

    public static String getMinMemory()
    {
        return instance().memoryProperties.getProperty("min");
    }

    public static void setMinMemory(String min)
    {
        instance().memoryProperties.setProperty("min", min);
        instance().writeMemoryProperties("Modified Memory Properties");
    }

    public static String getMaxMemory()
    {
        return instance().memoryProperties.getProperty("max");
    }

    public static void setMaxMemory(String max)
    {
        instance().memoryProperties.setProperty("max", max);
        instance().writeMemoryProperties("Modified Memory Properties");
    }
    
    public static String getStackSize()
    {
        String stackSize = instance().memoryProperties.getProperty("stack");
        if(stackSize == null) {
            setStackSize(DEFAULT_STACK_SIZE);
            stackSize = DEFAULT_STACK_SIZE;
        }
        return stackSize;
    }
    
    public static void setStackSize(String size)
    {
        instance().memoryProperties.setProperty("stack", size);
        instance().writeMemoryProperties("Modified Memory Properties");
    }

    public static void main(String[] args)
    {
        System.out.println(getMinMemory());
        System.out.println(getMaxMemory());
        setMinMemory("256m");
        setMaxMemory("1000m");
        setStackSize(DEFAULT_STACK_SIZE);
    }


}
