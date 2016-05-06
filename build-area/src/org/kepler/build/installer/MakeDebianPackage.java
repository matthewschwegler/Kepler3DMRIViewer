/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-10-18 09:09:18 -0700 (Fri, 18 Oct 2013) $'
 * '$Revision: 32500 $'
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.kepler.build.util.CommandLine;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/** Task to create Debian package files.
 * 
 *  @author Daniel Crawl
 *  @version $Id
 *  
 * 
 */
public class MakeDebianPackage extends MakeInstallerBase {

    @Override
    public void run() throws Exception {
            	
    	_initializeInstaller("Debian .deb");

        // create directory in installer directory
    	File targetDir = _getInstallerDirectory("debian");

    	File rootDir = new File(targetDir, "root");
    	
        // create layout in installer directory
    	_createDirectoryLayout(rootDir);
        
        // create control file
        _makeControlFile();
        
        // create .desktop file
        // TODO
        
        // copy files to installer directory
        _copyFilesToInstallerTree(_installedKeplerDir);
        
        _copyPtolemyLibs(_installedKeplerDir);
        
        _setPermissionsInInstallerTree(_installedKeplerDir);

        // build package
        _buildDebianPackage(rootDir);
    }
    
    /** Set the homepage url for the control file. */
    public void setHomepage(String homepage) {
    	_homepage = homepage;
    }
    
    /** Set the directory to install the application. */
    public void setInstalldir(String installdir)
    {
    	_installDirStr = installdir;
    }

    /** Build the debian package file. */
    private void _buildDebianPackage(File rootDir) throws IOException {
    	
        final String[] command = {"dpkg", "--build", rootDir.getAbsolutePath(),
        		rootDir.getParent() + "/" + _appname.toLowerCase() + "-" + _version + ".deb"};
        CommandLine.exec(command);
    }
    
    /** Create the installer tree directories. */
    private void _createDirectoryLayout(File targetDir) throws Exception {
    	
    	_debianDir = new File(targetDir, "DEBIAN");
    	_createDirectory(_debianDir);
    	
    	if(_installDirStr == null || _installDirStr.trim().isEmpty() ||
    			_installDirStr.equals("undefined")) {
    		_installDirStr = _DEFAULT_INSTALL_DIR;
    	} else if(_installDirStr.startsWith("/")) {
    		_installDirStr = _installDirStr.substring(1);
    	}
    	
    	if(!_installDirStr.endsWith("/")) {
    		_installDirStr += "/";
    	}
    	
    	String keplerInstallDirStr = _installDirStr + _appname.toLowerCase() + "-" + _version;
    	System.out.println("Kepler will be installed in /" + keplerInstallDirStr);
    	
    	_installedKeplerDir = new File(targetDir, keplerInstallDirStr);
    	_createDirectory(_installedKeplerDir);
    }
    
    /** Create the control file for the debian package. */
    private void _makeControlFile() throws IOException {
        
    	File controlFile = new File(_debianDir, "control");
    	
        Configuration configuration = new Configuration();
        configuration.setDirectoryForTemplateLoading(basedir);
        Template template = configuration.getTemplate("build-area/resources/installer/debian/control.ftl");

        Map<String,String> stringMap = new HashMap<String,String>();
        stringMap.put("appname", _appname);
        stringMap.put("version", _version);
        stringMap.put("homepage", _homepage);
        
        Writer writer = new FileWriter(controlFile);
        try {
			template.process(stringMap, writer);
		} catch (TemplateException e) {
			throw new IOException("Error processing template", e);
		}
        writer.close();
        
        System.out.println("created control file " + controlFile);
    }
    
    /** The directory to install the application. */
    private String _installDirStr;
    
    /** The homepage url for the control file. */
    private String _homepage;

    /** The DEBIAN directory in the installer tree. */
    private File _debianDir;
    
    /** The location to install kepler within the installer tree. */
    private File _installedKeplerDir;
    
    /** The default location to install kepler. */
    private final static String _DEFAULT_INSTALL_DIR = "/usr/local";

}
