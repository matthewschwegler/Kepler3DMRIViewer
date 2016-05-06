/*
 *  The node controller for actor instances.
 *  Copyright (c) 2010 The Regents of the University of California.
 *  All rights reserved.
 *  Permission is hereby granted, without written agreement and without
 *  license or royalty fees, to use, copy, modify, and distribute this
 *  software and its documentation for any purpose, provided that the above
 *  copyright notice and the following two paragraphs appear in all copies
 *  of this software.
 *  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 *  FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 *  THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 *  PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 *  PT_COPYRIGHT_VERSION_2
 *  COPYRIGHTENDKEY
 */

package org.kepler.kar.handlers;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.KAREntry;
import org.kepler.kar.KAREntryHandler;
import org.kepler.kar.KAREntryHandlerFactory;
import org.kepler.kar.KARFile;
import org.kepler.modulemanager.ModuleManagerEventListener;
import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * a kar handler to handle downloading modules if a kar file needs them
 * 
 * @author chad berkley
 */
public class ModuleTxtKAREntryHandler implements KAREntryHandler
{
  private String type = "moduleTxt";
  private static final Log log = LogFactory
      .getLog(ActorMetadataKAREntryHandler.class.getName());
  private static final boolean isDebugging = log.isDebugEnabled();

  /**
   * constructor
   */
  public ModuleTxtKAREntryHandler()
  {

  }

  /**
   * The getTypeName() method must return the type of object that this
   * KAREntryHandler saves. This method should return the KAR version 1.0
   * type name or null if version 1.0 is not supported by this handler.
   **/
  public String getTypeName()
  {
    return type;
  }

  /**
   * This method should return true if this KAREntryHandler can handle the
   * specified type. The type passed in is the binary class name of the file.
   * 
   * @param typeName
   **/
  public boolean handlesType(String typeName)
  {
    if (typeName.equals(type))
    {
      return true;
    }
    return false;
  }

  /**
   * The initialize method is called directly after instantiating this
   * KAREntryHandler.
   */
  public void initialize()
  {
  }

  /**
   * This method should return a CacheObject that will be put into the cache.
   * Once all the contents of the KAR exist in the cache then the open method
   * is called. In this way each entry in the kar can have access to all the
   * other entries through the cache when they get opened.
   * 
   * @param karFile
   * @param entry
   * @return the CacheObject that will be put into the CacheManager
   * @throws Exception
   */
  public CacheObject cache(KARFile karFile, KAREntry entry) throws Exception
  { 
    System.out.println("caching from ModuleTxtKAREntryHandler " + karFile.getFileLocation().getName() + ","
        + entry.toString());
    /*TextFileCacheObject cobj = new TextFileCacheObject(karFile
        .getInputStream(entry), entry.getLSID(), entry.toString());
    String modulesTxtStr = (String)cobj.getObject();
    File f = new File(DotKeplerManager.getInstance().getTransientDir(), "modules.txt");
    //write this to a temp file
    FileWriter fw = new FileWriter(f);
    fw.write(modulesTxtStr, 0, modulesTxtStr.length());
    fw.flush();
    fw.close();
    
    //merge the modules.txt file in the kar with the currently loaded modules.txt file
    ModulesTxt newModulesTxt = new ModulesTxt(f.toURI());
    System.out.println("downloading modules in modules.txt from kar " + karFile.getFileLocation().getName());
    System.out.println("modules.txt: " + ((String)cobj.getObject()));
    ModuleDownloader downloader = ModuleDownloader.getInstance();
    downloader.addListener(new ModuleStartupDownloadListener(karFile.getFileLocation().getName()));
    downloader.downloadFromModulesTxt(newModulesTxt);
    //now that the module(s) are downloaded, we need to process them into
    //the current ModuleTree so that they are available on the classpath
    //to the kar that needs them.
    //This could be tricky, because a kar could introduce conflicts between
    //modules.
    ModuleTree moduleTree = ModuleTree.instance();
    ModulesTxt mainModulesTxt = moduleTree.getModulesTxt();
    Iterator newModulesIt = newModulesTxt.modules.iterator();
    while(newModulesIt.hasNext())
    {
      //add the new module
      Module newModule = (Module)newModulesIt.next();
      mainModulesTxt.add(newModule, 0);
    }
    
    mainModulesTxt.write();
    //TODO: kepler should restart at this point to load the new classpath, 
    //but that might cause problems so I'm omitting that for now.
    
    return cobj;*/
    return null;
  }

  /**
   * When a KAR file is opened, any entries in the kar file that have the same
   * type as this KAREntryHandler will be passed to this open method. This
   * method will always be called after the cache method.
   * 
   * @param karFile
   * @param entry
   * @param tableauFrame
   * @return boolean true if the entry was opened successfully.
   * @throws Exception
   */
  public boolean open(KARFile karFile, KAREntry entry, TableauFrame tableauFrame) throws Exception
  {
    System.out.println("MTKEH opening " + karFile.toString() + ","
        + entry.toString());
    //tell the module-manager to download the module here
    System.out
        .println("kar entry handler is telling moduleTxtKAREntryHandler to open "
            + entry.getName());
    return true;
  }

  /**
   * Return an array of KAREntry objects that are to be saved for the given
   * lsid. All KAREntries must be of the same type of object that is returned
   * by the getTypeName() method.
   * 
   * @param lsid
   * @param karLsid the lsid of the containing KAR
   * @param tableauFrame
   * @return an array of KAREntries to be saved with this LSID object.
   * @throws Exception
   */
  public Hashtable<KAREntry, InputStream> save(Vector<KeplerLSID> lsids,
      KeplerLSID karLsid, TableauFrame tableauFrame) throws Exception
  {
    return null;
  }

  /**
   * A factory that creates a KAREntryHandler object.
   * 
   *@author Aaron Schultz
   */
  public static class Factory extends KAREntryHandlerFactory
  {
    /**
     * Create a factory with the given name and container.
     * 
     *@param container
     *          The container.
     *@param name
     *          The name of the entity.
     *@exception IllegalActionException
     *              If the container is incompatible with this attribute.
     *@exception NameDuplicationException
     *              If the name coincides with an attribute already in the
     *              container.
     */
    public Factory(NamedObj container, String name)
        throws IllegalActionException, NameDuplicationException
    {
      super(container, name);
    }

    /**
     * Create a library pane that displays the given library of actors.
     * 
     * @return A new LibraryPaneTab that displays the library
     */
    public KAREntryHandler createKAREntryHandler()
    {
      if (isDebugging)
        log.debug("moduleTxtKAREntryHandler()");
      return new ModuleTxtKAREntryHandler();
    }
  }
  
  /**
   * listener for module download events
   * @author berkley
   *
   */
  private class ModuleStartupDownloadListener implements ModuleManagerEventListener
  {
    private String karFileName;
    
    public ModuleStartupDownloadListener(String karFileName)
    {
      this.karFileName = karFileName;
    }
    
    /**
     * update progress for the progress monitor
     */
    public void updateProgress(int totalSize, int bufferSize, int readCount, InputStream is)
    {
      System.out.println("downloading " + (bufferSize * readCount) + " of " + totalSize + " bytes.");
    }
    
    /**
     * close the progress monitor at the end of the DL.
     */
    public void downloadEnd()
    {
      System.out.println("Done downloading module.");
    }
    
    /**
     * start up a new progress monitor at the beginning of a download.
     */
    public void downloadBegin(int totalSize, String moduleName)
    {
      System.out.println("Downloading module " + moduleName + " from kar file " + karFileName);
    }
    
    /**
     * create a new progress bar when an unzip begins
     */
    public void unzipBegin(long totalSize, String moduleName)
    {
      System.out.println("Unzipping module " + moduleName + " from kar file " + karFileName);
    }
    
    /**
     * get rid of the unzip progress bar
     */
    public void unzipEnd()
    {
      System.out.println("Done unzipping.");
    }
    
    /**
     * update the progress of an unzip
     */
    public void unzipUpdateProgress(long totalSize, int bufferSize, int readCount)
    {

    }
  }
}
