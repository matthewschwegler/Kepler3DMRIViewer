/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-06-14 09:45:38 -0700 (Thu, 14 Jun 2012) $' 
 * '$Revision: 29944 $'
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.util.DotKeplerManager;

/**
 * An interface to the deserialization methods required by the configuration
 * manager to deserialize properties
 */
public class CommonsConfigurationReader implements ConfigurationReader
{
  private ConfigurationWriter configurationWriter;
  
  /** Logging. */
  private final static Log _log = LogFactory.getLog(CommonsConfigurationReader.class);
  
  /** True if log level is set to DEBUG. */
  private final static boolean _isDebugging = _log.isDebugEnabled();

  /**
   * constructor
   */
  public CommonsConfigurationReader()
  {
  }

  /**
   * constructor
   */
  public CommonsConfigurationReader(ConfigurationWriter configurationWriter)
  {
      this.configurationWriter = configurationWriter;
  }
  
  /**
   * load all configurations for a given module
   */
  public List<RootConfigurationProperty> loadConfigurations(Module m)
    throws ConfigurationManagerException
  {
    // NOTE: the following are for testing. Oracle JDK appears to
    // ignore the value of $LANG for Locale.getDefault().
    //Locale l = new Locale("en" ,"US");
    //Locale l = new Locale("fi" ,"FI");
    //Locale l = new Locale("zh", "TW");
    //return loadConfigurations(m, l); //Locale.getDefault());
    return loadConfigurations(m, Locale.getDefault());
  }
  
  /**
   * load all configurations for a given module
   */
  public List<RootConfigurationProperty> loadConfigurations(Module m, Locale l)
    throws ConfigurationManagerException
  {
    Vector<RootConfigurationProperty> configs = new Vector<RootConfigurationProperty>();
    File configDir = m.getConfigurationsDir();
    if(configDir == null || !configDir.exists())
    {
      return null;
    }
    
    try
    {
      File[] configFiles = configDir.listFiles();
      //set the name of this config prop as the name of the config file
      for(File f : configFiles)
      {
        RootConfigurationProperty cp = null;
        if(!f.isDirectory())
        { //try to open each file with the commons reader
          if(f.getName().endsWith(".xml"))
          { //use the xml reader
            //System.out.println("loading config file " + f.getName() + " for module " + m.getName());
            cp = loadConfiguration(m, f, l);
	    //if(cp == null) { System.out.println("is NULL"); }
          }
        }
        
        if(cp != null)
        {
          configs.add(cp);
        }
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
      throw new ConfigurationManagerException(
        "Error loading configuration: " + e.getMessage());
    }
        
    return configs;
  }
  
  /**
   * load the configuration for a single file in a module
   */
  public RootConfigurationProperty loadConfiguration(Module m, File f)
    throws ConfigurationManagerException
  {
    return loadConfiguration(m, f, Locale.getDefault());
  }
  
  /**
   * load the configuration for a single file in a module
   */
  public RootConfigurationProperty loadConfiguration(Module m, File f, Locale l)
    throws ConfigurationManagerException
  {
    try
    {
      boolean loadfile = loadThisFile(f, l);
      //System.out.println("Should we load " + f.getName() +
        //" for locale " + l.toString() + " : " + loadfile);
      if(!loadfile)
      { //don't load this file if it's not of the correct locale
        return null;
      }


      f = ConfigurationManager.getOverwriteFile(m, f);
      
      XMLConfiguration xmlconfig = new XMLConfiguration();
      xmlconfig.setDelimiterParsingDisabled(true);
      
      FileInputStream stream = null;
      try
      {
          stream = new FileInputStream(f);
          xmlconfig.load(stream);
      }
      finally
      {
          if(stream != null)
          {
              stream.close();
          }
      }

      //David Welker: Adding configuration directives
      boolean mustSave = applyConfigurationDirectives(m, f, xmlconfig);

      RootConfigurationProperty cp = new RootConfigurationProperty(m, f);
      //set the default namespace
      ConfigurationNamespace namespace = new ConfigurationNamespace(
        ConfigurationManager.removeLocaleDesignator(
          ConfigurationManager.removeFileExtension(f.getName())));
      cp.setNamespace(namespace);
      
      ConfigurationNode rootCN = xmlconfig.getRootNode();
      
      //check to see if there is a namsepace element and use that if there is
      if(rootCN.getChild(0).getName().equals("namespace"))
      { 
        String ns = (String)rootCN.getChild(0).getValue();
        namespace = new ConfigurationNamespace(ns);
        cp.setNamespace(namespace);
      }
      //create the config property
      ConfigurationProperty deserializedProp = getConfiguration(rootCN, 
        m, namespace);
      
      cp.addProperty(deserializedProp);

      if( mustSave && configurationWriter != null)
        configurationWriter.writeConfiguration(cp);

      return cp;
    }
    catch(Exception e)
    {
      e.printStackTrace();
      throw new ConfigurationManagerException("Error loading configuration file " +
        f.getPath() + ": " + e.getMessage());
    }
  }

    /**
     * Author: David Welker
     *
     * This method will apply any previously unrun configuration directives to the configuration.
     * Note: Remove configuration and change configuration do not do anything yet, pending further discussion.
     * @param xmlconfig
     * @return Returns true if a configuration directive is applied.
     */
    private boolean applyConfigurationDirectives(Module module, File configurationFile, XMLConfiguration xmlconfig) throws ConfigurationException
    {
        boolean directiveApplied = false;
        File configurationDirectivesDir = module.getConfigurationDirectivesDir();
        if( !configurationDirectivesDir.isDirectory() )
            return directiveApplied;
        String configurationFilename = configurationFile.getName();
        boolean useDefaultNames = configurationFilename.equals("configuration.xml");
        String addDirectivesFilename = useDefaultNames ? "add.xml" : configurationFilename + "-add.xml";
        String changeDirectivesFilename = useDefaultNames ? "change.xml" : configurationFilename + "-change.xml";
        String removeDirecivesFilename = useDefaultNames ? "remove.xml" : configurationFilename + "-remove.xml";
        File addDirectivesFile = new File(configurationDirectivesDir, addDirectivesFilename);
        if( addDirectivesFile.exists() )
            directiveApplied = applyAddDirectives(module, addDirectivesFile, xmlconfig);
        File changeDirectivesFile = new File(configurationDirectivesDir, changeDirectivesFilename);
        if( changeDirectivesFile.exists() )
            directiveApplied = applyChangeDirectives(module, changeDirectivesFile, xmlconfig);
        File removeDirectivesFile = new File(configurationDirectivesDir, removeDirecivesFilename);
        if( removeDirectivesFile.exists() )
            directiveApplied = applyRemoveDirectives(module, removeDirectivesFile, xmlconfig);
        return directiveApplied;
    }

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


    /**
     * Author: David Welker
     *
     * This method applies unrun add directives to the configuration.
     * @param addXml
     * @param xmlconfig
     */
    private boolean applyAddDirectives(Module module, File addXml, XMLConfiguration xmlconfig) throws ConfigurationException
    {
        String addXmlFilename = addXml.getName();
        String addedXmlFilename = addXmlFilename.substring(0,addXmlFilename.length()-4) + "ed.xml";

        File addedXml = new File(DotKeplerManager.getInstance().getModuleConfigurationDirectory(module.getName()), addedXmlFilename);

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
            return false;

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
        return true;

    }

    //David Welker - Not Implemented - Further Discussion Needed
    private boolean applyChangeDirectives(Module module, File changeXml, XMLConfiguration xmlconfig) throws ConfigurationException
    {
        return false;
    }

    //David Welker - Not Implemented - Further Discussion Needed
    private boolean applyRemoveDirectives(Module module, File removeXml, XMLConfiguration xmlconfig) throws ConfigurationException
    {
        return false;
    }

    /**
   * return true if this file should be loaded.  if it has a locale designation
   * that matches the given locale, return true.  If it does not contain a 
   * locale designation and the locale is en_US return true.  If the locale is 
   * not en_US, but there are no other config files with the proper designator,
   * then return true.
   */
  private static boolean loadThisFile(File f, Locale l)
  {
    //System.out.println("looking for file " + f.getName() + " with locale " + l.toString());
    
    // see if the locale matches the locale in the file,
    // or the locale is en_US and file has no locale
    if(checkLocaleDesignator(f, l))
    {
      return true;
    }
    else
    { //see if there is a file to load that isn't this one
      File dir = new File(f.getParent());
      String s[] = dir.list();
      boolean filefound = false;
	  String baseName = getBaseName(f);
      for(int i=0; i<s.length; i++)
      {
          //if(f.getName().startsWith("uiMenuMap")) System.out.println("    checking possibility " + s[i]); 

    	  File possibleFile = new File(dir, s[i]);
    	  // basename must match
    	  String possibleBaseName = getBaseName(possibleFile);
    	  if (!possibleBaseName.equals(baseName)) {
		  //if(f.getName().startsWith("uiMenuMap")) System.out.println("    base name does not match for " + s[i]);
    		  continue;
    	  }
    	  // see if the other possibility has a matching locale,
    	  // or no locale and our locale is en_US. in this case,
    	  // this file should be loaded instead
    	  if(checkLocaleDesignator(possibleFile, l)) {
    		  filefound = true;
		  //System.out.println("  " + s[i] + " should be loaded instead");
    		  break;
    	  }
		  //else if(f.getName().startsWith("uiMenuMap")) System.out.println("  checkLocaleDesignator fails");

      }
      
      if(filefound)
      {
        //there is another config file that should get loaded for this locale
        //so don't load this one.
        return false;
      }
    }

    /*
	if(f.getName().startsWith("uiMenuMap")) {
		System.out.println("  no exact match.");
		System.out.println("    gld = " + getLocaleDesignator(f));
		System.out.println("    nocm = " + noOtherConfigurationMatch(f, l));
   	}
   	*/

    //there is no exact match for what we want
    if(getLocaleDesignator(f) == null || noOtherConfigurationMatch(f, l))
    { //if this is a default file, use it
      //System.out.println(" no exact match; using this since it's default.");
      return true;
    }
    else
    { //if not, don't
      return false;
    }
  }
  
  /**
   * return true if this is the only configuration file with its stem name
   * or if this configuration file is for en_US and there are no other
   * configuration files with the same base name with a matching locale.
   */
  private static boolean noOtherConfigurationMatch(File f, Locale l)
  {
    File dir = f.getParentFile();
    String[] list = dir.list();
    String fBasename = getBaseName(f);
    
    // NOTE: if this file is en_US and l is not en_US, return true
    // since we should use this file. this method is only called
    // after we have checked all other possibilities.
    String localeStr = getLocaleDesignator(f);
    if(localeStr != null && localeStr.equals("en_US") && !l.toString().equals("en_US"))
    {
        return true;
    }
    
    
    for(int i=0; i<list.length; i++)
    {
      File dirF = new File(dir, list[i]);
      if(dirF.getAbsolutePath().equals(f.getAbsolutePath()))
      { //don't look at the actual file, just its siblings
        continue;
      }
      String basename = getBaseName(dirF);
      if(basename.equals(fBasename))
      {
          return false;
      }
    }
    
    
    return true;
  }
  
  /**
   * return true if the designator on the file matches the locale exactly
   */
  private static boolean checkLocaleDesignator(File f, Locale l)
  {
    String designator = getLocaleDesignator(f);
    
    //System.out.println("designator: " + designator);
    //System.out.println("locale: " + l.toString());
    //System.out.println("File: " + f.getName());
    
    if(designator == null && l.toString().equals("en_US"))
    { //if there is no designator and the locale is en_US, use the file
      return true;
    }
    
    //the designator is not null, see if this file matches the designator
    if(designator != null && designator.equals(l.toString()))
    { //load this file!
      return true;
    }
    
    return false;
  }
  
  /**
   * return the locale designator from a filename, or null if there isn't one
   */
  private static String getLocaleDesignator(File f)
  {
    String basename = ConfigurationManager.removeFileExtension(f.getName());
    String designator = null;
    if(basename.length() > 7)
    {
      designator = basename.substring(basename.length() - 6, basename.length());
      if(designator.charAt(0) != '_' || designator.charAt(3) != '_')
      { //these characters do not represnet a locale 
        designator = null;
      }
      else
      {
        designator = designator.substring(1, designator.length());
      }
    }
    return designator;
  }
  
  /**
   * return the base name of a file without an extension or locale designator
   * @param f
   */
  private static String getBaseName(File f)
  {
    String basename = ConfigurationManager.removeFileExtension(f.getName());
    String designator = null;
    if(basename.length() > 7)
    {
      designator = basename.substring(basename.length() - 6, basename.length());
      if(designator.charAt(0) != '_' || designator.charAt(3) != '_')
      { //these characters do not represnet a locale 
        return basename;
      }
      else
      {
        return basename.substring(0, basename.length() - 6);
      }
    }
    return basename;
  }
  
  /**
   * add the structure of root to prop
   */
  private ConfigurationProperty getConfiguration(ConfigurationNode root, Module m, ConfigurationNamespace namespace)
    throws Exception
  {
    String originMod;
    boolean originOk = true;
    ConfigurationProperty cp = new ConfigurationProperty(m, root.getName());
    cp.setNamespace(namespace);
    String value = (String)root.getValue();
    boolean mutable = true;
    if(value != null && !value.equals(""))
    {
      cp.setValue(value);
    }
    
    if(root.getChildrenCount() != 0)
    {
      Iterator it = root.getChildren().iterator();
      while(it.hasNext())
      {
        ConfigurationNode child = (ConfigurationNode)it.next();
        ConfigurationProperty nextProp = getConfiguration(child, m, namespace);
        if(nextProp == null)
        {
            if(_isDebugging)
            {
              _log.debug("not loading property " + child.getName() +
                " because it was added from an inactive module.");
            }
          continue;
        }
        
        if(nextProp.getName().equals("mutable"))
        {
          if(nextProp.getValue() != null && 
             nextProp.getValue().equals("false"))
          {
            cp.setMutable(false);
          }
        }
        else if(nextProp.getName().equals("originModule"))
        {
          if(nextProp.getValue() != null &&
             !nextProp.getValue().equals(""))
          {
            originMod = nextProp.getValue();
            if(!ModuleTree.instance().contains(originMod))
            {
              originOk = false;
            }
            else
            {
              cp.setOriginModule(ConfigurationManager.getModule(originMod));
            }
          }
        }
        
        cp.addProperty(nextProp, true);
      }
    }
    
    if(originOk)
    {
      return cp;
    }
    else
    {
      return null;
    }
  }
}
