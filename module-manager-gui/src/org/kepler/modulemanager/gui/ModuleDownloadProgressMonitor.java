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

package org.kepler.modulemanager.gui;

import java.awt.Container;
import java.io.InputStream;

import javax.swing.ProgressMonitor;

import org.kepler.modulemanager.ModuleManagerEventListener;

public class ModuleDownloadProgressMonitor implements ModuleManagerEventListener
{
  private Container parent = null;
  private ProgressMonitor progressMonitor;
  
  /**
   * constructor
   * @param parent the parent gui object
   */
  public ModuleDownloadProgressMonitor(Container parent)
  {
    this.parent = parent;
  }
  
  /**
   * update progress for the progress monitor
   */
  public void updateProgress(int totalSize, int bufferSize, int readCount, InputStream is)
  {
    progressMonitor.setProgress(readCount*bufferSize);
    //System.out.println("downloaded " + (readCount * bufferSize) + " of " + totalSize);
  }
  
  /**
   * close the progress monitor at the end of the DL.
   */
  public void downloadEnd()
  {
    progressMonitor.close(); 
  }
  
  /**
   * start up a new progress monitor at the beginning of a download.
   */
  public void downloadBegin(int totalSize, String moduleName)
  {
    progressMonitor = new ProgressMonitor(parent, "Downloading module " + moduleName, "", 0, totalSize);
    progressMonitor.setMillisToDecideToPopup(500);
    progressMonitor.setMillisToPopup(500);
  }
  
  /**
   * create a new progress bar when an unzip begins
   */
  public void unzipBegin(long totalSize, String moduleName)
  {
    progressMonitor = new ProgressMonitor(parent, "Unzipping module " + moduleName, "", 0, (int)totalSize);
    progressMonitor.setMillisToDecideToPopup(500);
    progressMonitor.setMillisToPopup(500);
  }
  
  /**
   * get rid of the unzip progress bar
   */
  public void unzipEnd()
  {
    progressMonitor.close(); 
  }
  
  /**
   * update the progress of an unzip
   */
  public void unzipUpdateProgress(long totalSize, int bufferSize, int readCount)
  {
    progressMonitor.setProgress(readCount*bufferSize);
    //System.out.println("unzipped " + (readCount * bufferSize) + " of " + totalSize);
  }
}
