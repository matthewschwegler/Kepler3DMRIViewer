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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.CompileClasspath;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.util.StreamSingleCommandExec;

/**
 * Create the Javadocs for Kepler.
 * Created by David Welker.
 * Date: Nov 7, 2008
 * Time: 4:12:57 PM
 * @version $Id: CreateJavadoc.java 31111 2012-11-26 22:17:14Z crawl $
 */
public class CreateJavadoc extends ModulesTask
{
    protected String moduleName;
    protected List<String> command = new ArrayList<String>();
    protected String sourcepath = "";
    protected Path classpath = new Path(ProjectLocator.getAntProject());

    /**
     * set the module to create javadocs for
     *
     * @param moduleName
     */
    public void setModule(String moduleName)
    {
        this.moduleName = moduleName;
    }

    /**
     * Delete the javadoc in the destination directory.
     *
     * @param destDir The directory containing javadoc to be deleted.
     */
    protected void cleanJavadoc(File destDir)
    {
        if (!destDir.isDirectory())
        {
            return;
        }
        Delete delete = new Delete();
        delete.bindToOwner(this);
        delete.setDir(destDir);
        delete.execute();
    }

    /**
     * Specifies the packages for which javadoc should be produced.
     *
     * @param packages One or more packages.
     */
    protected void specifyPackages(String... packages)
    {
        String subpackages = null;
        for (String p : packages)
        {
            subpackages = subpackages == null ? p : subpackages + ":" + p;
        }
        addToCommand("-subpackages " + subpackages);
    }

    /**
     * add any additional command strings
     *
     * @param s
     */
    private void addToCommand(String s)
    {
        String[] parts = s.split("\\s+");
        for (String part : parts)
        {
            command.add(part);
        }
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        //First, make sure that any existing javadoc is removed.
        File destDir = new File(basedir, "javadoc");
        cleanJavadoc(destDir);

        // If a particular module is not defined, then produce javadoc for all modules in modules.txt.
        if (moduleName.equals("undefined"))
        {
            addModuleSourcepaths();
            addModuleCompilepaths();
        }
        // Produce javadoc for only one module.
        else
        {
            Module m = ModuleTree.instance().getModule(moduleName);
            if(m == null) {
                PrintError.message("ERROR: module " + moduleName + " is not in the current suite.");
                return;
            }
            addSourcepath(m);
            addCompilepath(m);
        }

        command.add("javadoc");
        // Add the necessary argument for the javadoc command.
        addToCommand("-d " + destDir.getAbsolutePath());
        addToCommand("-sourcepath " + sourcepath);

        addDefaultCompilePath();
 
        addToCommand("-classpath " + classpath.toString());

        // Needed for RatingTaglet
        addToCommand("-tagletpath " + Module.make("ptolemy").getTargetDir() + "/classes");

        // FIXME: what about os dependent excludes
        String ptolemyExcludes = _ptolemyExcludes();
        if (ptolemyExcludes.length() > 0)
        {
            addToCommand("-exclude " + ptolemyExcludes);
        }

        specifyPackages("org", "com", "util", "ptolemy");
        addToCommand("-link http://download.oracle.com/javase/6/docs/api/");
        command.add("-linksource");
        command.add("-author");
        command.add("-breakiterator");
        command.add("-use");
        command.add("-quiet");
        addToCommand("-tag Pt.AcceptedRating -tag Pt.ProposedRating ");
        addToCommand("-tag category.name -tag UserLevelDocumentation ");
        addToCommand("-tag created -tag entity.description -tag status");
        addToCommand("-taglet doc.doclets.RatingTaglet");
        command.add("-J-Xmx1024m");

        final StreamSingleCommandExec exec = new StreamSingleCommandExec();
        exec.setCommands(command);
        exec.start();
    }

    /**
     * Include default classpath for all javadoc builds.
     */
    protected void addDefaultCompilePath()
    {
	// Find tools.jar or classes.jar.  See similar code in $PTII/configure.in.
	File toolsJar = new File(System.getProperty("java.home").replace('\\', '/'),
				 "../lib/tools.jar");
	if (!toolsJar.exists()) {
	    // Might be Mac OS X.  Why Apple changed the JVM layout is a mystery.
	    File classesJar = new File(System.getProperty("java.home").replace('\\', '/'),
				"../Classes/classes.jar");
	    if (classesJar.exists()) {
		toolsJar = classesJar;
	    }
	}

        classpath.addFileset(_getPtolemyFileSet());
        classpath.append(new Path(ProjectLocator.getAntProject(), toolsJar.getAbsolutePath()));

        addCompilepath(Module.make("build-area"));
    }

    /**
     * Add source paths from which javadoc should be produced for all the modules.
     */
    protected void addModuleSourcepaths()
    {
        for (Module module : moduleTree)
        {
            addSourcepath(module);
        }
    }

    /**
     * Add the source path from which the javadoc should be produced for one
     * module.
     *
     * @param srcDir The directory that contains Java code from which javadoc
     *               shall be produced.
     */
    protected void addSourcepath(File srcDir)
    {
        sourcepath += sourcepath.equals("") ? "" : File.pathSeparator;
        sourcepath += srcDir.getAbsolutePath();
    }

    /**
     * Add the compile path from which javadoc should be produced for one module.
     *
     * @param module The module for which javadoc should be produced.
     */
    protected void addSourcepath(Module module)
    {
        if (!module.getSrc().isDirectory())
        {
            return;
        }
        addSourcepath(module.getSrc());
    }

    /**
     * Add compile paths from which javadoc should be produced for all the modules.
     */
    protected void addModuleCompilepaths()
    {
        for (Module module : moduleTree)
        {
            addCompilepath(module);
        }
    }

    /**
     * Add the source path from which javadoc should be produced for one module.
     *
     * @param module The module for which javadoc should be produced.
     */
    protected void addCompilepath(Module module)
    {
        if (!module.getSrc().isDirectory())
        {
            return;
        }
        CompileClasspath path = new CompileClasspath(module);
        classpath.append(path);
    }

    /**
     * Return a File.pathSeparator separated list of Ptolemy jar files.
     *
     * @return a File.pathSeparator separated list of Ptolemy jar files.
     */
    private FileSet _getPtolemyFileSet()
    {
        PtolemyPathGenerator ptolemyPathGenerator = PtolemyPathGenerator
                .getInstance();
        FileSet fileSet = ptolemyPathGenerator.getFileset();
        return fileSet;
    }

    /**
     * Return a colon separated list of Ptolemy packages to exclude.
     *
     * @return a colon separated list of Ptolemy packages to exclude.
     * @throws IOException If the ptolemy-excludes file cannot be found or
     *                     read.
     */
    private String _ptolemyExcludes() throws IOException
    {
        StringBuffer results = new StringBuffer();
        String fileName = Module.make("build-area").getSrc()
                + "/../settings/ptolemy-excludes";
        File ptolemyExcludes = new File(fileName);

        if (ptolemyExcludes.exists())
        {
            FileInputStream stream = null;
            DataInputStream in = null;
            try
            {
                stream = new FileInputStream(ptolemyExcludes);
                in = new DataInputStream(stream);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null)
                {
                    if (results.length() > 0)
                    {
                        results.append(":");
                    }
                    results.append(line.replace("/", ".").replace(".**", ""));
                }
            }
            finally
            {
                if (in != null)
                {
                    try
                    {
                        in.close();
                    }
                    catch (IOException ex)
                    {
                        PrintError.message("Failed to close " + ptolemyExcludes, ex);
                    }
                }
            }
        }
        return results.toString();
    }
}
