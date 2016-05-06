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
package org.kepler.build.installer;

import java.io.File;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.GZip;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.TarFileSet;
import org.apache.tools.ant.types.ZipFileSet;

/** Create a .tar.gz file of the current suite.
 *  
 * Created by David Welker.
 * Date: Jul 14, 2010
 * Time: 10:52:31 PM
 */
public class MakeLinuxInstaller extends MakeInstallerBase
{
    @Override
    public void run() throws Exception
    {
        
        String installerName = "Linux";
        if(_zip) {
            installerName = " .zip";
        } else {
            installerName = " .tar.gz";
        }
    	_initializeInstaller(installerName);

    	ArchiveFileSet fileSet = null;

    	Task task = null;
    	
    	if(_zip) {
    	    task = new Zip();
    	    fileSet = new ZipFileSet();
    	    
    	} else {
            task = new Tar();
            Tar.TarLongFileMode gnu = new Tar.TarLongFileMode();
            gnu.setValue(Tar.TarLongFileMode.GNU);
            ((Tar) task).setLongfile(gnu);
            fileSet = new TarFileSet();
    	}    	    

        task.bindToOwner(this);
        task.init();

    	fileSet.setPrefix(_appname + "-" + _version);
        _getFilesToCopy(fileSet);
        fileSet.setFileMode("755");
        
        if(_zip) {
            ((Zip) task).add(fileSet);
        } else {
            ((Tar) task).add(fileSet);
        }
        
        File tempDir = _getInstallerDirectory("temp");

        String outputFileName =  _appname + "-" + _version ;
        if(_zip) {
            outputFileName += ".zip";
        } else {
            outputFileName += "-linux.tar";
        }
        
        File installTargetDir = _getInstallerDirectory("linux");
        File outputFile = new File(tempDir, outputFileName);

        if(_zip) {
            outputFile = new File(installTargetDir, outputFileName);
            ((Zip) task).setDestFile(outputFile);
        } else {
            outputFile = new File(tempDir, outputFileName);
            ((Tar) task).setDestFile(outputFile);
            // NOTE: delete the output file on exit when creating a tar file
            // since we create a separate .tar.gz below.
            outputFile.deleteOnExit();
        }

        task.execute();
        
        if(!_zip) {

            File gzipFile = new File(installTargetDir, _appname + "-" + _version + "-linux.tar.gz");
            gzipFile.getParentFile().mkdirs();
            GZip gzip = new GZip();
            gzip.bindToOwner(this);
            gzip.init();
            gzip.setSrc(outputFile);
            gzip.setDestfile(gzipFile);
            gzip.execute();
        }
    }
    
    /** Set if this task should create a zip file. */
    public void setZip(String zip) {
        if(zip != null && zip.equals("true")) {
            _zip = true;
        }
    }
    
    /** If true, build a zip file. Otherwise, build a .tar.gz file. */
    private boolean _zip = false;
    
}
