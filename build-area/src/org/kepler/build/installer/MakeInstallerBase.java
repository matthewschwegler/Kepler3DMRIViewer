/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-07-09 15:27:00 -0700 (Wed, 09 Jul 2014) $'
 * '$Revision: 32827 $'
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.kepler.build.MakeStartupScripts;
import org.kepler.build.modules.CurrentSuiteTxt;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.PropertyDefaults;
import org.kepler.build.util.Version;

/** Base class for installers containing common tasks.
 * 
 *  @author Daniel Crawl
 *  @version $Id 
 */
public abstract class MakeInstallerBase extends ModulesTask {

    /** Set the name of the installer. */
    public void setAppname(String appname)
    {
        _appname = appname;
    }

    /** Set the version of the installer. */
    public void setVersion(String version)
    {
        _version = version;
    }

    /** Print a warning if the modules are not released. */
    protected void _warnIfNotRunningWithReleasedModules() {
        
        for(Module module : ModuleTree.instance()) {
            if(!module.isReleased()) {
                System.out.println("WARNING: the installer is being created with " +
                    "non-released modules (either from a branch or the trunk); " +
                    "this has not been thoroughly tested for all platforms and " +
                    "the resulting installer probably will not work.");
                return;
            }
        }

    }

    /** Copy files to the installer tree. This method also changes the permissions
     *  on scripts to be executable.
     */
    protected void _copyFilesToInstallerTree(File installerTreeDir) throws Exception {
        
        // copy the modules
       
        final FileSet fileset = new FileSet();
        _getFilesToCopy(fileset);               
        Copy copy = new Copy();
        copy.bindToOwner(this);
        copy.addFileset(fileset);
        copy.setTodir(installerTreeDir);
        copy.setIncludeEmptyDirs(false);
        copy.execute();
   
   }

    /** Set the permissions on the files and directories in the installer
     *  tree (the copy of kepler in finished-kepler-installer/{mac,linux,etc})
     *  to be world readable and executable.
     */
    protected void _setPermissionsInInstallerTree(File installerTreeDir) throws Exception {

        // make everything world readable and executable
        System.out.println("Setting permissions on files in " + installerTreeDir);
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(installerTreeDir);
        scanner.setIncludes(new String[] {"**/*"});
        scanner.scan();
        for(String fileStr : scanner.getIncludedFiles()) {
            File includedFile = new File(installerTreeDir, fileStr);
            includedFile.setExecutable(true, false);
            includedFile.setReadable(true, false);
        }

        for(String dirStr : scanner.getIncludedDirectories()) {
            File includedDir = new File(installerTreeDir, dirStr);
            includedDir.setExecutable(true, false);
            includedDir.setReadable(true, false);
        }
    }

    /** Copy the files in the ptolemy module src/lib to the lib/ptolemy-lib 
     *  since the src/ directories are not included in the installer.
     *  NOTE: running 'ant release' for the ptolemy module already performs
     *  this copy; this copy is only necessary when building an installer
     *  from a non-released (i.e., branch or trunk) version of Kepler.
     */
    protected void _copyPtolemyLibs(File installerTreeDir) throws Exception {

        // find the ptolemy module
        Module ptolemyModule = null;
        for(Module module : ModuleTree.instance()) {
            if(module.isPtolemy()) {
                ptolemyModule = module;
            }
        }

        if(ptolemyModule == null) {
            throw new Exception("Could not find ptolemy module in suite.");
        }

        // if ptolemy-lib does not already exist, do the copy
        File srcLibDir = new File(ptolemyModule.getSrc(), "lib");
        File destLibDir = new File(installerTreeDir, 
            ptolemyModule.getName() + File.separator + "lib" +
            File.separator + Module.PTOLEMY_LIB);
   
        // copy unless ptolemy-lib already exists. 
        if(!destLibDir.exists()) {
            FileSet srcFileSet = new FileSet();
            srcFileSet.setDir(srcLibDir);
            Copy copy = new Copy();
            copy.bindToOwner(this);
            copy.addFileset(srcFileSet);
            copy.setTodir(destLibDir);
            copy.execute();
        }
    }

    /** Create a directory and any containing directories that do not exist. */
    protected void _createDirectory(File directory) throws Exception {
    	//System.out.println("mkdir -p " + directory);
        if(!directory.exists() && !directory.mkdirs()) {
        	throw new Exception("Unable to create directory " + directory);
        }

        // make the directory world readable and executable
        // dpkg (used to create the debian installer) requires the directory
        // containing the control file to be world readable
        // and executable.
        directory.setReadable(true, false);
        directory.setExecutable(true, false);
    }
    
    /** Perform tasks common to all installers. */
    protected void _initializeInstaller(String name) throws Exception {
        
        System.out.println("Making " + name);

        _warnIfNotRunningWithReleasedModules();

        if (_version.equals(PropertyDefaults.getDefaultValue("version"))) {
        	
        	System.out.println("Version not specified, so guessing.");
        	
        	// try to get the version from the suite
        	String suiteName = CurrentSuiteTxt.getName();
        	
        	if(Version.isVersioned(suiteName)) {
        		Version version = Version.fromVersionString(suiteName);
        		_version = version.getVersionString();
        		
        		System.out.println("Version set to suite version: " + _version);
        	} else {
        		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd.HHmmss");
        		_version = format.format(new Date());
        		System.out.println("No version in suite, using timestamp: " + _version);
        	}
        }   
        
        // create the startup scripts (e.g., kepler.sh, kepler.bat, etc.)
        MakeStartupScripts makeSS = new MakeStartupScripts();
        makeSS.bindToOwner(this);
        makeSS.run();

        // create build-area/install-id.txt
        InstallationIdTxt.write(_version);
        
        // create build-area/use.keplerdata
        // this file will be deleted when the program exits
        File useKeplerdataKeplerModules = new File(ProjectLocator.getBuildDir(), "use.keplerdata");
        useKeplerdataKeplerModules.createNewFile();
        useKeplerdataKeplerModules.deleteOnExit();

    }

    /** Get the set of files to place in the installer.
     *
     *  @param fileset A FileSet object to add includes and excludes of
     *  files for the installer.
     */
    protected void _getFilesToCopy(FileSet fileset) throws Exception {
        
        fileset.setProject(ProjectLocator.getAntProject());
        fileset.setDir(basedir);
        
        fileset.setIncludes("kepler.jar");
        fileset.setIncludes("kepler.sh");
        fileset.setIncludes("kepler.bat");
        fileset.setIncludes("module-manager.sh");
        fileset.setIncludes("module-manager.bat");
        fileset.setIncludes("build-area/install-id.txt");
        fileset.setIncludes("build-area/modules.txt");
        fileset.setIncludes("build-area/current-suite.txt");
        fileset.setIncludes("build-area/os-registry.txt");
        fileset.setIncludes("build-area/module-manager-gui.txt");
        fileset.setIncludes("build-area/module-location-registry.txt");
        fileset.setIncludes("build-area/registry.txt");
        fileset.setIncludes("build-area/use.keplerdata");
        fileset.setIncludes("build-area/lib/ant.jar");
        fileset.setIncludes("build-area/settings/build-properties.xml");
        
        for(Module module : moduleTree) {
         
            final String moduleName = module.getName();
            
            fileset.setIncludes(moduleName + "/");
            
            fileset.setExcludes(moduleName + "/.classpath");
            fileset.setExcludes(moduleName + "/.project");
            fileset.setExcludes(moduleName + "/src/");
            //fileset.setExcludes(moduleName + "/src/**/*.java");
            //fileset.setExcludes(moduleName + "/src/**/*.class");
            fileset.setExcludes(moduleName + "/target/classes/");
            fileset.setExcludes(moduleName + "/target/eclipse/");
            fileset.setExcludes(moduleName + File.separator + module.getName() + ".zip");
            
            // see if the module is ptolemy. if so, use the ptolemy excludes
            if(module.isPtolemy()) {
            	
            	// the ptolemy-excludes file is relative to ptolemy module src directory,
            	// so read each line and add the module name and src
                BufferedReader reader = new BufferedReader(
                		new FileReader(new File(basedir, "build-area/settings/ptolemy-excludes")));
                String line = reader.readLine();
                while(line != null) {
                    fileset.setExcludes(moduleName + "/src/" + line);
                    line = reader.readLine();
                }
                reader.close();
            }            
        }        
    }
    
    /** Get the directory where the installer will be built. The directory
     *  will be created if it does not exist.
     *
     *  @param name the name of the installer
     */
    protected File _getInstallerDirectory(String name) throws Exception {
        
        File installTargetDir = new File(
        		ProjectLocator.getKeplerModulesDir().getParentFile(),
        		"finished-kepler-installers" + File.separator + name);

        // if the directory already exists, rename it.
        // this make sure that no old files that were previously copied
        // into the installer tree are put in the new installer.
        if(installTargetDir.exists()) {
            int i = 0;
            File oldInstallTargetDir = new File(installTargetDir.getParentFile(),
                name + "-old." + i);
            while(oldInstallTargetDir.exists()) {
                i++;
                oldInstallTargetDir = new File(installTargetDir.getParentFile(),
                    name + "-old." + i);
            }

            System.out.println("Renaming old " + name + " installer to " +
                oldInstallTargetDir);
            if(!installTargetDir.renameTo(oldInstallTargetDir)) {
                throw new Exception("Could not rename " + installTargetDir + " to " + oldInstallTargetDir);
            }
        
            installTargetDir = new File(
        		ProjectLocator.getKeplerModulesDir().getParentFile(),
        		"finished-kepler-installers" + File.separator + name);
        }

        _createDirectory(installTargetDir);
        return installTargetDir;
    }
    
    /** The installer name. */
    protected String _appname;
    
    /** The installer version. */
    protected String _version;

}
