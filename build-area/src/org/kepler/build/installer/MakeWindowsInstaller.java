/*
 * Copyright (c) 2009 The Regents of the University of California.
 * All rights reserved.
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.launch4j.ant.Launch4jTask;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.project.ProjectLocator;

import com.izforge.izpack.ant.IzPackTask;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Created by David Welker.  Support for creating .exe files added by Christopher Brooks.
 * Date: Jul 14, 2010
 * Time: 10:52:18 PM
 */
public class MakeWindowsInstaller extends MakeInstallerBase
{
    private File tempDir;

    //Temp files needed to create installer.
    private File installXml;
    private File l4jXml;
    private File readmeHtml;
    private File shortcutSpecXml;

    @Override
    public void run() throws Exception
    {
    	_initializeInstaller("Windows Installer");
        
        tempDir = _getInstallerDirectory("temp");
        
        makeInstallXML();
        makeL4JXML();
        makeShortcutSpecXML();
        makeReadmeHtml();


        File installTargetDir = _getInstallerDirectory("windows");
        File windowsInstallerJar = new File(installTargetDir, _appname + "-" + _version + "-win.jar");
        IzPackTask izPack = new IzPackTask();
        izPack.bindToOwner(this);
        izPack.setInput(installXml.getAbsolutePath());
        izPack.setOutput(windowsInstallerJar.getAbsolutePath());
        izPack.setBasedir(".");
        izPack.execute();

        // Create a .exe that wraps the jar because double clicking on a .jar does
        // not always work.
        Launch4jTask launch4j = new Launch4jTask();
        launch4j.setConfigFile(l4jXml.getAbsoluteFile());
        launch4j.setJarPath(windowsInstallerJar.getAbsolutePath());
        File windowsInstallerExe = new File(installTargetDir, _appname + "-" + _version + "-win.exe");
        launch4j.setOutfile(windowsInstallerExe.getAbsoluteFile());
        //for Launch4jTask, version number should be 'x.x.x.x', so we need to append missing '.0'. 
        int dotNumber = _version.split("\\.").length;
        String versionStr = _version;
        System.out.println("versionStr:" + versionStr);
        for (int i=dotNumber; i < 4; i++){
        	versionStr = versionStr + ".0";
        }
        
        // versions strings for launch4j can only have digits and periods.
        // NOTE: this version string only appears to be used internally?
        String launch4jVersion = versionStr.replaceAll("[^\\d\\.]", "");
        launch4j.setFileVersion(launch4jVersion);
        launch4j.setProductVersion(launch4jVersion);
        launch4j.execute();

        cleanupTempFiles();
    }

    private void makeInstallXML() throws IOException, TemplateException
    {
        installXml = new File(tempDir, "install.xml");

        Configuration cfg = new Configuration();
        //File templatesDir = new File(ProjectLocator.getBuildDir(), "resources/templates");
        cfg.setDirectoryForTemplateLoading(basedir);
        Template template = cfg.getTemplate("build-area/resources/installer/windows/izpack.ftl");

        Map root = new HashMap();
        root.put("appname", _appname);
        root.put("appversion", _version);
        root.put("basedir", basedir.getAbsolutePath());

        List standardModules = new ArrayList();
        List moduleDependentPacks = new ArrayList();

        for (Module module : ModuleTree.instance())
        {
            if (module.getStemName().equals("r"))
                root.put("rDir", module.getName());
            File moduleInstallXml = new File(module.getDir(), "module-info/install.xml");
            if (moduleInstallXml.exists())
            {
                Map packMap = new HashMap();
                packMap.put("installXml", "/" + module.getName() + "/module-info/install.xml");
                moduleDependentPacks.add(packMap);
            }
            else
            {
                Map moduleMap = new HashMap();
                moduleMap.put("name", module.getName());
                standardModules.add(moduleMap);
            }
        }
        root.put("standardModules", standardModules);
        root.put("moduleDependentsPacks", moduleDependentPacks);

        Writer writer = new FileWriter(installXml);
        template.process(root, writer);
    }


    private void makeL4JXML() throws IOException, TemplateException
    {
        l4jXml = new File(tempDir, "l4j.xml");

        Configuration cfg = new Configuration();
        //File templatesDir = new File(ProjectLocator.getBuildDir(), "resources/templates");
        cfg.setDirectoryForTemplateLoading(basedir);
        Template template = cfg.getTemplate("build-area/resources/installer/windows/l4j.ftl");

        // The directory that contains the launch4j windres and ld binaries used under Mac OS X.
	String osName = System.getProperty("os.name");
	String binDirectory = "";
	if (osName.equals("Linux")) {
	    binDirectory = "bin-linux";
	} else if (osName.equals("Mac OS X")) {
	    binDirectory = "bin-darwin";
	} else if (osName.startsWith("Win")) {
	    binDirectory = "bin-win32";
	} else {
	    throw new IOException("The os.name property was " + osName + ", which is not Linux, Mac OS X or Win.");
	}
        File launch4jBindir = new File(ProjectLocator.getKeplerModulesDir() + "/build-area/resources/installer/launch4j/" + binDirectory);
        if (!launch4jBindir.isDirectory() 
	    || ((!new File(launch4jBindir, "windres").isFile())
		&& (!new File(launch4jBindir, "windres.exe").isFile()))) {
            throw new IOException("Could not find Launch4j/windres.  Launch4jBindir was: "
                    + launch4jBindir);
        }

        System.setProperty("launch4j.bindir", launch4jBindir.getAbsolutePath());

        Map root = new HashMap();
        root.put("appname", _appname);

        root.put("appversion", _version);

        root.put("basedir", basedir.getAbsolutePath());

        File installTargetDir = new File(ProjectLocator.getKeplerModulesDir().getParentFile(), "finished-kepler-installers/windows");
        File windowsInstallerJar = new File(installTargetDir, _appname + "-" + _version + "-win.jar");
        root.put("jarname", windowsInstallerJar.getAbsolutePath());

        ModuleTree tree = ModuleTree.instance();
        File iconFile = new File(ProjectLocator.getKeplerModulesDir() + "/" + tree.getModuleByStemName("common").getName() + "/resources/icons/kepler.ico");
        if (!iconFile.isFile()) {
            throw new IOException("Launch4j Icon file " + iconFile + " does not exist.");
        }
        root.put("icon", iconFile.getAbsolutePath());

        File windowsInstallerExe = new File(installTargetDir, _appname + "-" + _version + "-win.exe");
        root.put("exefile", windowsInstallerExe.getAbsolutePath());

        List standardModules = new ArrayList();
        List moduleDependentPacks = new ArrayList();

        Writer writer = new FileWriter(l4jXml);
        template.process(root, writer);
        System.out.println("Launch4j file: " + l4jXml);
    }

    private void makeReadmeHtml() throws IOException, TemplateException
    {
        readmeHtml = new File(tempDir, "README.html");

        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(basedir);
        Template template = cfg.getTemplate("build-area/resources/installer/windows/README.ftl");

        Map root = new HashMap();
        root.put("appversion", _version);

        DateFormat dateFormat = new SimpleDateFormat("MMMMM dd, yyyy");
        Calendar cal = Calendar.getInstance();
        String date = dateFormat.format(cal.getTime());
        root.put("date", date);

        Writer writer = new FileWriter(readmeHtml);
        template.process(root, writer);
    }

    private void makeShortcutSpecXML() throws IOException, TemplateException
    {
        shortcutSpecXml = new File(tempDir, "shortcutSpec.xml");

        Configuration cfg = new Configuration();
        //File templatesDir = new File(ProjectLocator.getBuildDir(), "resources/templates");
        cfg.setDirectoryForTemplateLoading(basedir);
        Template template = cfg.getTemplate("build-area/resources/installer/windows/shortcut.ftl");

        Map root = new HashMap();
        root.put("appversion", _version);
        
        // use the common module name for the icon location
        ModuleTree moduleTree = ModuleTree.instance();
        Module commonModule = moduleTree.getModuleByStemName("common");
        root.put("commonmodule", commonModule.getName());
                
        Writer writer = new FileWriter(shortcutSpecXml);
        template.process(root, writer);
    }

    private void cleanupTempFiles()
    {
        installXml.deleteOnExit();
        //l4jXml.deleteOnExit();
        readmeHtml.deleteOnExit();
        shortcutSpecXml.deleteOnExit();
    }

}

