/**
 *
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY
 * OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.kepler.modulemanager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.Project;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.util.HttpsSvnReader;

//import ptolemy.backtrack.util.java.util.Vector;

/**
 * Class to download modules when they're needed
 * 
 * @author berkley
 */
public class ModuleDownloader
{
  private List<String> releasedModules = new ArrayList<String>();
  private Vector listeners = new Vector();

  /**
   *  constructor 
   */
  public ModuleDownloader()
  {
    calculateReleasedModules();
    listeners = new Vector();
  }
  
  /**
   * add a listener for ModuleManagerEvents 
   * @param listener
   */
  public void addListener(ModuleManagerEventListener listener)
  {
    if(listeners == null)
    {
      listeners = new Vector();
    }
    
    listeners.add(listener);
  }
  
  /**
   * remove the given listener
   * @param listener
   */
  public void removeListener(ModuleManagerEventListener listener)
  {
    listeners.remove(listener);
  }
  
  /**
   * download a set of modules as defined in a modules.txt file
   * @param modulesTxt
   */
  public void downloadFromModulesTxt(ModulesTxt modulesTxt)
    throws Exception
  {
    ModuleTree tree = new ModuleTree(modulesTxt);
    downloadModulesFromModuleList(tree.getModuleList());
  }
  
  private void downloadModulesFromModuleList(List<Module> moduleList)
    throws Exception
  {
    Vector v = new Vector();
    Iterator it = moduleList.iterator();
    while(it.hasNext())
    {
      Module m = (Module)it.next();
      //System.out.println("adding module " + m.getName() + " to the list.");
      v.add(m.getName());
    }
    downloadModules((List)v);
  }

  /**
   * download any modules in the list
   * 
   * @param moduleNames the list of modules to download
   */
  public void downloadModules(List<String> moduleNames)
    throws Exception
  {
    Project project = new Project();
    for (String moduleName : moduleNames)
    {
      //split to throw away any trailing string, eg ptII repo
      moduleName = moduleName.trim().split("\\s")[0];
      boolean isSuite = false;
      if (moduleName.startsWith("*"))
      {
        isSuite = true;
        moduleName = moduleName.substring(1, moduleName.length());
      }
      if (moduleName.matches("[a-zA-Z-]+\\d+\\.\\d+"))
      {
        moduleName = moduleName + "." + getHighestPatch(moduleName);
      }
      else if (moduleName.matches("[a-zA-Z-]+\\d+\\.\\d+\\.\\^"))
      {
        moduleName = moduleName.substring(0, moduleName.length() - 2);
        moduleName = moduleName + "." + getHighestPatch(moduleName);
      }
      System.out.println("Downloading " + moduleName + "...");

      Module module = Module.make(moduleName);
      download(module);
      unzip(module, project);

      if (isSuite)
      {
        String url = RepositoryLocations.getReleaseLocation() + "/" + moduleName
            + "/module-info/modules.txt";
        downloadModules(HttpsSvnReader.readURL(url));
      }
    }
  }
  
  /**
   * find the released modules
   * 
   */
  public void calculateReleasedModules()
  {
    releasedModules = HttpsSvnReader.read(RepositoryLocations.getReleaseLocation());
	// XXX Update released.txt and Module.released here too to be in sync.
	// TODO check about refactoring to get rid of releasedModules variable.
	Module.updateReleasedModuleList();
  }

  /**
   * get the highest patch
   * 
   * @param base
   * @return
   */
  private String getHighestPatch(String base)
  {
    try
    {
      if (base.startsWith("*"))
      {
        base = base.substring(1, base.length());
      }
      int highestPatch = 0;
      for (String branch : releasedModules)
      {
        if (branch.matches(base + "\\.\\d+"))
        {
          String[] parts = branch.split("\\.");
          String patchString = parts[parts.length - 1];
          int patch = Integer.parseInt(patchString);
          if (patch > highestPatch)
          {
            highestPatch = patch;
          }
        }
      }
      return "" + highestPatch;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * download a single module
   * 
   * @param module
   */
  private void download(Module module)
    throws Exception
  {
    int BUFFER = 1024;
    File parentDir = module.getDir().getParentFile();
    File zip = new File(parentDir, module.getName() + ".zip");
    File target = new File(parentDir, module.getName());
    if (zip.exists() || target.isDirectory())
    {
      return;
    }
	String moduleLocation = RepositoryLocations.getReleaseLocation() + module.getName();
	String urlName = moduleLocation + "/" + zip.getName();
	System.out.println("urlName:"+urlName);
	URL url = new URL(urlName);
	HttpURLConnection uc = (HttpURLConnection) url.openConnection();
	
	// check whether the url exists or not.
	uc.setRequestMethod("HEAD");
	//System.out.println("uc.getResponseCode():" + uc.getResponseCode());
	if (uc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
		throw new FileNotFoundException (url + " was not found. \nPlease check the correctness of the suite/module and try again.");
	}	    	
	//if exists, download it.
	uc = (HttpURLConnection) url.openConnection();
	updateListenersDownloadBegin(uc.getContentLength(), module.getName());
	InputStream is = new BufferedInputStream(uc.getInputStream());
	OutputStream os = new BufferedOutputStream(new FileOutputStream(zip));
	byte data[] = new byte[BUFFER];
	int count = 0;
	int numread = is.read(data, 0, BUFFER);
	while (numread >= 0)
	{
	  updateListenersProgress(uc.getContentLength(), BUFFER, count++, is);
	  os.write(data, 0, numread);
	  numread = is.read(data, 0, BUFFER);
	}
	os.close();
	is.close();
	
	// this may be a little expensive to call here every time, but it's safer.
	Module.updatePresentModuleList();
	
	updateListenersDownloadEnd();

  }
  
  /**
   * update the listeners on the status of the download
   * @param totalSize the total size of the transfer.  -1 if not known
   * @param bufferSize the size of each buffered transfer.  -1 if this is not a 
   *        buffered transfer
   * @param readCount the number of times bufferSize has been read.
   * @param is the inputStream being read
   */
  private void updateListenersProgress(int totalSize, int bufferSize, int readCount, InputStream is)
  {
    for(int i=0; i<listeners.size(); i++)
    {
      ModuleManagerEventListener listener = (ModuleManagerEventListener)listeners.get(i);
      listener.updateProgress(totalSize, bufferSize, readCount, is);
    }
  }
  
  /**
   * update the listeners when a download begins
   * @param totalSize the total size of the download.  -1 if unknown.
   */
  private void updateListenersDownloadBegin(int totalSize, String moduleName)
  {
    for(int i=0; i<listeners.size(); i++)
    {
      ModuleManagerEventListener listener = (ModuleManagerEventListener)listeners.get(i);
      listener.downloadBegin(totalSize, moduleName);
    }
  }
  
  /**
   * update the listeners when a download ends
   */
  private void updateListenersDownloadEnd()
  {
    for(int i=0; i<listeners.size(); i++)
    {
      ModuleManagerEventListener listener = (ModuleManagerEventListener)listeners.get(i);
      listener.downloadEnd();
    }
  }
  
  /**
   * update the listeners on the status of the unzip
   * @param totalSize the total size of the transfer.  -1 if not known
   * @param bufferSize the size of each buffered transfer.  -1 if this is not a 
   *        buffered transfer
   * @param readCount the number of times bufferSize has been read.
   * @param is the inputStream being read
   */
  private void updateUnzipListenersProgress(long totalSize, int bufferSize, int readCount)
  {
    for(int i=0; i<listeners.size(); i++)
    {
      ModuleManagerEventListener listener = (ModuleManagerEventListener)listeners.get(i);
      listener.unzipUpdateProgress(totalSize, bufferSize, readCount);
    }
  }
  
  /**
   * update the listeners when an unzip begins
   * @param totalSize the total size of the download.  -1 if unknown.
   */
  private void updateListenersUnzipBegin(long totalSize, String moduleName)
  {
    for(int i=0; i<listeners.size(); i++)
    {
      ModuleManagerEventListener listener = (ModuleManagerEventListener)listeners.get(i);
      listener.unzipBegin(totalSize, moduleName);
    }
  }
  
  /**
   * update the listeners when an unzip ends
   */
  private void updateListenersUnzipEnd()
  {
    for(int i=0; i<listeners.size(); i++)
    {
      ModuleManagerEventListener listener = (ModuleManagerEventListener)listeners.get(i);
      listener.unzipEnd();
    }
  }

  /**
   * unzip a module for a project
   * 
   * @param module
   * @param project
   */
  private void unzip(Module module, Project project)
    throws Exception
  {
    int BUFFER = 2048;
    File parentDir = module.getDir().getParentFile();
    File zip = new File(parentDir, module.getName() + ".zip");
    updateListenersUnzipBegin(zip.length(), module.getName());
    File target = new File(parentDir, module.getName());
    if (target.isDirectory())
      return;
    FileInputStream fis = new FileInputStream(zip);
    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
    ZipEntry entry;
    int progress = 0;
    while ((entry = zis.getNextEntry()) != null)
    {
      File moduleDir = new File(parentDir, module.getName());

      File outputFile = new File(moduleDir, entry.getName());
      if (entry.isDirectory())
      {
        outputFile.mkdirs();
        continue;
      }
      int count = 0;
      byte[] data = new byte[BUFFER];
      FileOutputStream fos = new FileOutputStream(outputFile);
      BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
      int c = 0;
      while ((count = zis.read(data, 0, BUFFER)) != -1)
      {
        updateUnzipListenersProgress(zip.length(), BUFFER, c++);
        dest.write(data, 0, count);
        progress += count;
      }
      dest.flush();
      dest.close();
    }
    zis.close();
    updateListenersUnzipEnd();
  }
}
