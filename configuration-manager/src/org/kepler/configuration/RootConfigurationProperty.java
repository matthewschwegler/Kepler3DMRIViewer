/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

import org.kepler.build.modules.Module;

/**
 * Class that represents a root configuration property within kepler
 */
public class RootConfigurationProperty extends ConfigurationProperty
{
  private File file;
  
  /**
   * construct a root configuration property within a module with a specific file
   */
  public RootConfigurationProperty(Module m, File configurationFile)
  {
    super(m, ConfigurationManager.removeFileExtension(configurationFile.getName()));
    this.file = configurationFile;
  }
  
  protected RootConfigurationProperty(Module m, String name)
  {
    super(m, name);
    this.file = null;
  }
  
  protected RootConfigurationProperty(Module m, String name, ConfigurationProperty property)
    throws NamespaceException, ConfigurationManagerException
  {
    super(m, name, property);
    this.file = null;
  }
  
  protected RootConfigurationProperty(Module m, String name, ConfigurationNamespace namespace)
  {
    super(m, name, namespace, (String)null);
    this.file = null;
  }
  
  protected RootConfigurationProperty(Module m, String name, ConfigurationNamespace namespace, ConfigurationProperty property)
    throws NamespaceException, ConfigurationManagerException
  {
    this(m, name, namespace);
    addProperty(property);
  }
  
  /**
   * return the file that this configuration property represents
   */
  public File getFile()
  {
    return this.file;
  }
  
  /**
   * returns true if any of the properties in this RootConfigurationProperty have
   * been changed.  This is the opposite functionality of 
   * ConfigurationProperty.isDirty() which is not recursive by default
   */
  public boolean isDirty()
  {
    if(getProperties().size() > 0)
    {
      return getProperty(0).isDirty(true);
    }
    return false;
  }
  
  /**
   * return the root property of this configuration
   */
  public ConfigurationProperty getRootProperty()
  {
    return getProperties().get(0);
  }
}