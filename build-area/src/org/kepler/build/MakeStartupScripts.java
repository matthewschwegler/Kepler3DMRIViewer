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
package org.kepler.build;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.ant.taskdefs.Copy;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.ProjectLocator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Created by Chad Berkley
 * Date: April 13, 2009
 * <p/>
 * This class creates the classpath for tasks such as Run to use
 */
public class MakeStartupScripts extends ModulesTask
{

    private File scriptLocation;


    /**
     * execute the task
     */
    public void run() throws Exception
    {
        scriptLocation = ProjectLocator.getKeplerModulesDir();
        makeKeplerSh();
        makeModuleManagerSh();
        makeKeplerBat();
        makeModuleManagerBat();
        copyKeplerJar();
    }

    private void makeKeplerSh() throws IOException, TemplateException
    {
        makeSh("kepler.sh", "");
    }

    private void makeModuleManagerSh() throws IOException, TemplateException
    {
        makeSh("module-manager.sh", "UseModuleManager");
    }

    private void makeSh(String filename, String args) throws IOException, TemplateException
    {
        File keplerSh = new File(scriptLocation, filename);

        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(basedir);
        Template template = cfg.getTemplate("build-area/resources/startup-script/sh.ftl");

        Map root = new HashMap();
        root.put("args", args);

        Writer writer = new FileWriter(keplerSh);
        template.process(root, writer);

        Chmod chmod = new Chmod();
        chmod.bindToOwner(this);
        chmod.init();
        chmod.setFile(keplerSh);
        chmod.setPerm("755");
        chmod.execute();
    }

    private void makeKeplerBat() throws IOException, TemplateException
    {
    	makeBat("kepler.bat", "");
    }
    
    private void makeModuleManagerBat() throws IOException, TemplateException
    {
    	makeBat("module-manager.bat", "UseModuleManager");
    }
    
    private void makeBat(String filename, String args) throws IOException, TemplateException
    {
        File keplerBat = new File(scriptLocation, filename);

        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(basedir);
        Template template = cfg.getTemplate("build-area/resources/startup-script/bat.ftl");

        Map root = new HashMap();
        root.put("args", args);

        Writer writer = new FileWriter(keplerBat);
        template.process(root, writer);
    }

    private void copyKeplerJar()
    {
        File keplerTasksJar = new File(ProjectLocator.getBuildDir(), "target/kepler-tasks.jar");
        File keplerJar = new File(ProjectLocator.getKeplerModulesDir(), "kepler.jar");
        Copy copy = new Copy();
        copy.bindToOwner(this);
        copy.setFile(keplerTasksJar);
        copy.setTofile(keplerJar);
        copy.execute();
    }

}
