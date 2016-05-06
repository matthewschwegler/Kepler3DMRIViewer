/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-06-14 09:42:05 -0700 (Thu, 14 Jun 2012) $' 
 * '$Revision: 29943 $'
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
import java.io.FileWriter;

import org.kepler.build.modules.Module;
import org.kepler.util.DotKeplerManager;

/**
 * A class that implements the ConfigurationWriter interface for the commons
 * framework.
 */
public class GeneralConfigurationWriter implements ConfigurationWriter
{      
  /**
   * constructor
   */
  public GeneralConfigurationWriter()
  {
     
  }
  
  /**
   * write (serialize) a given configuration property
   */
  public void writeConfiguration(RootConfigurationProperty property)
    throws ConfigurationManagerException
  {
    if(!property.isDirty())
    { //there is nothing to write here since nothing changed
      return;
    }
    
    String filename = property.getFile().getName();
    Module m = property.getModule();
    
    //first check to see if the configurations directory exists in .kepler
    
    File modConfDir = DotKeplerManager.getInstance().getModuleConfigurationDirectory(m.getName());
    ///File modConfDir = DotKeplerManager.getInstance().getModuleConfigurationDirectory(m.getStemName());
    if(!modConfDir.exists())
    { //create the dir
      if(!modConfDir.mkdirs())
      {
          throw new ConfigurationManagerException("Unable to create directory " + modConfDir);
      }
    }
    
    //now we can save the property to .kepler/configurations
    try
    {
      File configFile = new File(modConfDir, filename);
      //get the first child of the root because that's actually the config
      //info we want.
      ConfigurationProperty cp = property.getProperty(0);
      String xml = cp.getXML();
      //System.out.println("xml: " + xml);
      FileWriter fw = new FileWriter(configFile);
      fw.write(xml, 0, xml.length());
      fw.flush();
      fw.close();
    }
    catch(Exception e)
    {
      throw new ConfigurationManagerException("Error serializing configuration " + 
        filename + " to directory " + modConfDir.getAbsolutePath() +
        " : " + e.getMessage());
    }
  }
}