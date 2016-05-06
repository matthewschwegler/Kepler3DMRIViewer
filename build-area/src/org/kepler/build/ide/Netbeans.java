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
package org.kepler.build.ide;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.kepler.build.modules.Module;

/**
 * create files to use kepler with netbeans
 * Created by David Welker.
 * Date: Sep 12, 2008
 * Time: 11:42:46 AM
 */
public class Netbeans extends Ide
{
    private File netbeansDir;

    /**
     * init the task
     */
    public void init()
    {
        super.init();
        netbeansDir = new File(getProject().getBaseDir(),
                "build-area/resources/netbeans");
    }

    /**
     * make a file
     */
    private void makeFile(String from, String to)
    {
        FileMerger.makeFile(netbeansDir, from, basedir, to);
    }

    /**
     * merge a file
     */
    private void merge(String from, PrintWriter to)
    {
        FileMerger.merge(netbeansDir, from, to);
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        for (Module module : moduleTree)
        {
            File nbProjectDir = new File(module.getDir(), "nbproject");
            if (nbProjectDir.exists())
            {
                continue;
            }
            createClassesDir(module);
            makeProjectXmlFile(module);
            makeProjectProperties(module);
            makeEmptyFile(module + "/nbproject/genfiles.properties");
        }
        makeMainModule();
    }

    /**
     * create the classes dir
     */
    private void createClassesDir(Module module)
    {
        module.getTargetClasses().mkdirs();
    }

    /**
     * make a main module
     */
    private void makeMainModule()
    {
        //createTargetClassesDir("build-area");
        makeFile("MainProjectXml", "build-area/nbproject/project.xml");
        makeFile("MainProjectProperties", "build-area/nbproject/project.properties");
        makeEmptyFile("build-area/nbproject/genfiles.properties");
    }

    /**
     * make an empty file
     */
    private void makeEmptyFile(String name)
    {
        File file = new File(basedir, name);
        try
        {
            file.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * make a project xml file
     */
    private void makeProjectXmlFile(Module module)
    {
        File projectXmlFile = new File(basedir, module + "/nbproject/project.xml");
        PrintWriter pw = FileMerger.getPrintWriter(projectXmlFile);
        merge("ProjectXmlIntro", pw);
        pw.println("            <name>" + module + "</name>");
        merge("ProjectXmlMiddle", pw);
        printProjectXMLDependencies(pw, module);
        merge("ProjectXmlEnd", pw);
        pw.close();
        System.out.println("Writing " + projectXmlFile);

    }

    /**
     * make a project prop file
     */
    private void makeProjectProperties(Module module)
    {
        File projectPropertiesFile = new File(basedir, module
                + "/nbproject/project.properties");
        PrintWriter pw = FileMerger.getPrintWriter(projectPropertiesFile);
        merge("ProjectPropertiesIntro", pw);
        pw.println("# This directory is removed when the project is cleaned:");
        pw.println("dist.dir=target");
        pw.println("dist.jar=${dist.dir}/" + module + ".jar");
        pw.println("dist.javadoc.dir=${dist.dir}/javadoc");
        pw.println("excludes=");
        if (module.getSrc().isDirectory())
            pw.println("file.reference.main-java=src");
        else
            pw.println("file.reference.main-java=module-info");
        pw.println("file.reference.target-classes=target/classes");
        printAllResourceDefinitions(pw, module);

        printAllJarReferenceDefinitions(pw, module);
        merge("ProjectPropertiesMiddle1", pw);
        printAllResourceReferences(pw, module);
        printAllJarReferences(pw, module);

        printModuleReferences(pw, module);
        merge("ProjectPropertiesMiddle2", pw);
        printModuleDependencies(pw, module);
        merge("ProjectPropertiesEnd", pw);
        pw.close();
        System.out.println("Writing " + projectPropertiesFile);
    }

    private void printAllJarReferenceDefinitions(PrintWriter pw, Module currentModule)
    {
        List<Module> lowerPriorityModules = moduleTree.getLowerPriorityModules(currentModule);
        printJarReferenceDefinitions(pw, currentModule);
        for (Module module : lowerPriorityModules)
        {
            printJarReferenceDefinitions(pw, module);
        }
    }

    private void printJarReferenceDefinitions(PrintWriter pw, Module module)
    {
        List<File> jars = getJarFiles(module);
        for (File jar : jars)
        {
            printJarReferenceDefinition(pw, module, jar);
        }
    }

    private void printAllJarReferences(PrintWriter pw, Module currentModule)
    {
        List<Module> lowerPriorityModules = moduleTree.getLowerPriorityModules(currentModule);
        printJarReferences(pw, currentModule, false);
        if (lowerPriorityModules.isEmpty())
            return;
        Module lastModule = lowerPriorityModules.remove(lowerPriorityModules.size() - 1);
        for (Module module : lowerPriorityModules)
        {
            printJarReferences(pw, module, false);
        }
        printJarReferences(pw, lastModule, true);
    }

    private void printJarReferences(PrintWriter pw, Module module, boolean last)
    {
        List<File> jars = getJarFiles(module);
        printJarReferences(pw, module, jars, last);
    }


    /**
     * get jar files
     */
    private List<File> getJarFiles(Module module)
    {
        List<File> result = new ArrayList<File>();

        File libDir = module.getLibDir();
        if (!libDir.exists())
        {
            return result;
        }

        FileSet jars = new FileSet();
        jars.setProject(getProject());
        jars.setDir(libDir);
        jars.setIncludes("**/*.jar");

        Iterator<Resource> i = jars.iterator();
        while (i.hasNext())
        {
            Resource resource = i.next();
            if(resource instanceof FileResource) {
                result.add(((FileResource)resource).getFile());
            }
        }

        return result;
    }

    /**
     * print a jar reference dev
     */
    private void printJarReferenceDefinition(PrintWriter pw, Module module, File jar)
    {
        String referencePath = getReferencePath(module, jar);
        pw.println(getJarReference(module, jar) + "=" + referencePath);
    }

    /**
     * get a reference path
     */
    private String getReferencePath(Module module, File jar)
    {
        return getReferencePath(module, getJarPath(module, jar));
    }

    /**
     * get a reference path
     */
    private String getReferencePath(Module module, String jarPath)
    {
        return "../" + module.getDir().getName() + "/" + jarPath;
    }

    /**
     * get a jar path
     */
    private String getJarPath(Module module, File jar)
    {
        String jarPath = jar.getAbsolutePath();
        String modulePath = module.getDir().getAbsolutePath();
        jarPath = jarPath.substring(modulePath.length() + 1);
        return jarPath;
    }

    /**
     * get a jar . path
     */
    private String getJarDotPath(String jarPath)
    {
        return jarPath.replace('/', '.');
    }

    /**
     * get a jar reference
     */
    private String getJarReference(Module module, File jar)
    {
        String jarPath = getJarPath(module, jar);
        String dotPath = getJarDotPath(jarPath);
        return "file.reference." + dotPath;
    }

    /**
     * print a jar reference
     */
    private void printJarReferences(PrintWriter pw, Module module, List<File> jars, boolean last)
    {
        int i = 0;
        for (File jar : jars)
        {
            pw.print("    ${" + getJarReference(module, jar) + "}");
            if (++i < jars.size() || !last)
            {
                pw.println(":\\");
            }
        }
    }

    /**
     * print a ln if it exists
     */
    private void printlnIfExists(PrintWriter pw, File f, String s)
    {
        if (f.exists())
        {
            pw.println(s);
        }
    }

    /**
     * print all resource definitions
     */
    private void printAllResourceDefinitions(PrintWriter pw, Module currentModule)
    {
        List<Module> lowerPriorityModules = moduleTree.getLowerPriorityModules(currentModule);
        printResourceDefinitions(pw, currentModule);
        for (Module module : lowerPriorityModules)
        {
            printResourceDefinitions(pw, module);
        }
    }

    /**
     * print resources definitions
     *
     * @param pw
     * @param module
     */
    private void printResourceDefinitions(PrintWriter pw, Module module)
    {
        File configs = module.getConfigsDir();
        File lib = module.getLibDir();
        File libImages = module.getLibImagesDir();
        File resources = module.getResourcesDir();
        printlnIfExists(pw, configs, "file.reference." + module + ".configs=../"
                + module + "/configs");
        printlnIfExists(pw, lib, "file.reference." + module + ".lib=../" + module
                + "/lib");
        printlnIfExists(pw, libImages, "file.reference." + module
                + ".lib.images=../" + module + "/lib/images");
        printlnIfExists(pw, resources, "file.reference." + module
                + ".resources=../" + module + "/resources");
    }

    /**
     * print all resource references
     */
    private void printAllResourceReferences(PrintWriter pw, Module currentModule)
    {
        List<Module> lowerPriorityModules = moduleTree
                .getLowerPriorityModules(currentModule);
        printResourceReferences(pw, currentModule);
        for (Module module : lowerPriorityModules)
        {
            printResourceReferences(pw, module);
        }
    }

    /**
     * print resource references
     */
    private void printResourceReferences(PrintWriter pw, Module module)
    {
        File configs = module.getConfigsDir();
        File lib = module.getLibDir();
        File libImages = module.getLibImagesDir();
        File resources = module.getResourcesDir();
        printlnIfExists(pw, configs, "    ${file.reference." + module
                + ".configs}:\\");
        printlnIfExists(pw, lib, "    ${file.reference." + module + ".lib}:\\");
        printlnIfExists(pw, libImages, "    ${file.reference." + module
                + ".lib.images}:\\");
        printlnIfExists(pw, resources, "    ${file.reference." + module
                + ".resources}:\\");
    }

    /**
     * print module references
     */
    private void printModuleReferences(PrintWriter pw, Module currentModule)
    {
        List<Module> lowerPriorityModules = moduleTree
                .getLowerPriorityModules(currentModule);
        if (lowerPriorityModules.isEmpty())
        {
            pw.println();
            return;
        }
        pw.println(":\\");
        Module lowestPriorityModule = lowerPriorityModules.get(lowerPriorityModules
                .size() - 1);
        for (Module module : lowerPriorityModules)
        {
            pw.print("    ${reference." + module + ".jar}");
            if (!module.equals(lowestPriorityModule))
            {
                pw.println(":\\");
            }
            else
            {
                pw.println();
            }
        }
    }

    /**
     * print module depends
     */
    private void printModuleDependencies(PrintWriter pw, Module currentModule)
    {
        for (Module module : moduleTree.getLowerPriorityModules(currentModule))
        {
            pw.println("project." + module + "=../" + module);
            pw.println("reference." + module + ".jar=${project." + module
                    + "}/target/" + module + ".jar");
        }
    }

    /**
     * print project xml depends
     */
    private void printProjectXMLDependencies(PrintWriter pw, Module currentModule)
    {
        List<Module> lowerPriorityModules = moduleTree
                .getLowerPriorityModules(currentModule);
        if (lowerPriorityModules.isEmpty())
        {
            return;
        }
        pw
                .println("        <references xmlns=\"http://www.netbeans.org/ns/ant-project-references/1\">");
        for (Module module : lowerPriorityModules)
        {
            pw.println("            <reference>");
            pw.println("                <foreign-project>" + module
                    + "</foreign-project>");
            pw.println("                <artifact-type>jar</artifact-type>");
            pw.println("                <script>nbbuild.xml</script>");
            pw.println("                <target>jar</target>");
            pw.println("                <clean-target>clean</clean-target>");
            pw.println("                <id>jar</id>");
            pw.println("            </reference>");
        }
        pw.println("        </references>");
    }
}
