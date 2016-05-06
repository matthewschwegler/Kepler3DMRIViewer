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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.tools.ant.taskdefs.Copy;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.util.CommandLine;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;


/** A task that creates a Mac .dmg file of the current suite.
 *  
 * Created by David Welker.
 * Date: Jul 14, 2010
 * Time: 10:36:41 PM
 */
public class MakeMacOSXInstaller extends MakeInstallerBase
{
    private File readmeHtml;

    @Override
    public void run() throws Exception
    {
    	_initializeInstaller("Mac .dmg");

        File volumeDir = new File(ProjectLocator.getKeplerModulesDir().getParentFile(), "finished-kepler-installers/macosx/Kepler-" + _version);
        volumeDir.getParentFile().mkdirs();
        File appTargetDir = new File(volumeDir, _appname + "-" + _version);
        appTargetDir.mkdirs();
        
        //create readme file
        makeReadmeHtml(appTargetDir.getParentFile());
        //copy license file
        System.out.println("Copying license.txt to " + appTargetDir.getParentFile().getAbsolutePath());
        Copy copy = new Copy();
        copy.bindToOwner(this);
        copy.setFile(new File(ProjectLocator.getBuildDir(), "resources/release/license.txt"));
        copy.setTodir(appTargetDir.getParentFile());
        copy.execute();

        makeKeplerApp(appTargetDir);
        makeModuleManagerApp(appTargetDir);

        CommandLine.exec(new String[]{"ln", "-s", "/Applications", volumeDir.getAbsolutePath() + "/To install, drag the Kepler folder here."});
        CommandLine.exec(new String[]{"hdiutil", "create", "-srcfolder", volumeDir.getAbsolutePath(), volumeDir.getParentFile() + "/" + _appname + "-" + _version + ".dmg"});
    }

    /*
     * Returns the Java directory which will vary based on the app.
     */

    private File makeApp(File appTargetDir, String dotAppName, String mainClass, String classpath, String arguments, String iconFileName) throws Exception
    {
        System.out.println("Making " + dotAppName + ".app");
        File installerResources = new File(ProjectLocator.getBuildDir(), "resources/installer/macosx");
        File appFolder = new File(appTargetDir, dotAppName + ".app");
        appFolder.mkdir();

        //Copy Info.plist and PkgInfo to Kepler.app/Contents
        File contentsDir = new File(appFolder, "Contents");
        contentsDir.mkdir();

        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(installerResources);
        Template template = cfg.getTemplate("Info-plist.ftl");

        Map<String,String> map = new HashMap<String,String>();
        map.put("appname", dotAppName);
        map.put("mainClass", mainClass);
        map.put("classpath", classpath);
        map.put("arguments", arguments);


        File infoPlist = new File(contentsDir, "Info.plist");
        System.out.println(contentsDir.getAbsolutePath());
        if (contentsDir.isDirectory())
            System.out.println("contentsDir is a directory.");
        else
            System.out.println("contentsDir does not exist.");
        infoPlist.createNewFile();
        Writer writer = new FileWriter(infoPlist);
        template.process(map, writer);

        File pkgInfo = new File(installerResources, "PkgInfo");
        Copy copy = new Copy();
        copy.bindToOwner(this);
        copy.setFile(pkgInfo);
        copy.setTodir(contentsDir);
        copy.execute();

        //The JavaApplicationStub, the executable that gets everything started, is stored at Kepler.app/Contents/MacOS
        //Copy JavaApplicationStub to ${appname}.app/Contents/MacOS.
        File macOS = new File(contentsDir, "MacOS");
        macOS.mkdir();

        File javaApplicationStub = new File(installerResources, "JavaApplicationStub");
        copy = new Copy();
        copy.bindToOwner(this);
        copy.setFile(javaApplicationStub);
        copy.setTodir(macOS);
        copy.execute();

        //Make the javaApplicationStubExecutable
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.US).contains("windows");
        if (!isWindows)
        {
            CommandLine.exec(new String[]{"chmod", "-R", "777", macOS.getPath()});
        }


        //The resources dir contains the Kepler application icon as well as the Java folder where all the modules are stored.
        File resourcesDir = new File(contentsDir, "Resources");
        resourcesDir.mkdir();
        //Copy Kepler icon to Kepler.app/Contents/Resources.
        File keplerIcns = new File(installerResources, "kepler.icns");
        copy = new Copy();
        copy.bindToOwner(this);
        copy.setFile(keplerIcns);
        copy.setTodir(resourcesDir);
        copy.execute();

        File javaDir = new File(resourcesDir, "Java");
        javaDir.mkdir();

        return javaDir;

    }
    
    private void makeKeplerApp(File appTargetDir) throws Exception
    {
        File javaDir = makeApp(appTargetDir, "Kepler", 
        		"org.kepler.build.runner.Kepler", 
        		"$JAVAROOT/kepler.jar", "",
        		"kepler.icns");

        _copyFilesToInstallerTree(javaDir);
    }
    
    private void makeModuleManagerApp(File appTargetDir) throws Exception
    {
        makeApp(appTargetDir, "Module Manager", "org.kepler.build.runner.Kepler", "$JAVAROOT/../../../../Kepler.app/Contents/Resources/Java/kepler.jar", "UseModuleManager", "kepler.icns");
    }

    private void makeReadmeHtml(File tempDir) throws IOException, TemplateException
    {
    	
    	System.out.println("Copying README.html to " + tempDir.getAbsolutePath());
        readmeHtml = new File(tempDir, "README.html");

        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(basedir);
        Template template = cfg.getTemplate("build-area/resources/installer/windows/README.ftl");

        Map<String,String> root = new HashMap<String,String>();
        root.put("appversion", _version);

        DateFormat dateFormat = new SimpleDateFormat("MMMMM dd, yyyy");
        Calendar cal = Calendar.getInstance();
        String date = dateFormat.format(cal.getTime());
        root.put("date", date);

        Writer writer = new FileWriter(readmeHtml);
        template.process(root, writer);
    }
}
