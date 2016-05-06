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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.taskdefs.optional.depend.ClassFile;
import org.apache.tools.ant.taskdefs.optional.depend.JarFileIterator;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;

/**
 * task to report the overrids for a module
 *
 * @author berkley
 */
public class ReportOverrides extends ModulesTask
{
    protected Hashtable<String, List<String>> classModuleTable = new Hashtable<String, List<String>>();
    public int numberOfOverrides = 0;

    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        for (Module module : moduleTree)
        {
            if (module.getName().equals("kepler-1.0-jar-tag"))
            {
                handleKepler10JarTag(module);
                continue;
            }
            List<String> classes = getClasses(module);
            addToClassModuleTable(classes, module.getName());
        }
        reportOverrides();
    }

    /**
     * @param module
     */
    protected void handleKepler10JarTag(Module module)
    {
        handleJarFile("kepler-r7920.jar", module);
        handleJarFile("ptolemy-r49472.jar", module);
    }

    /**
     * handle a jar file
     *
     * @param jarFilename
     * @param module
     */
    protected void handleJarFile(String jarFilename, Module module)
    {
        File jarDir = new File(module.getLibDir(), "jar");
        File jarFile = new File(jarDir, jarFilename);
        if (!jarFile.exists())
        {
            System.out.println("ERROR: " + jarFilename + " not found!");
            return;
        }
        handleJarFile(jarFile, jarFilename);
    }

    /**
     * handle a jar file
     *
     * @param jar
     * @param jarFilename
     */
    protected void handleJarFile(File jar, String jarFilename)
    {
        List<String> classes = getClasses(jar);
        addToClassModuleTable(classes, jarFilename);
    }

    /**
     * get the classes
     *
     * @param module
     * @return
     */
    protected List<String> getClasses(Module module)
    {
        List<String> moduleClassesList = new ArrayList<String>();
        getClasses(moduleClassesList, "", module.getSrc());
        return moduleClassesList;
    }

    /**
     * get the classes
     *
     * @param jarFile
     * @return
     */
    protected List<String> getClasses(File jarFile)
    {
        List<String> moduleClassList = new ArrayList<String>();
        JarFileIterator i = null;
        try
        {
            i = new JarFileIterator(new FileInputStream(jarFile));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        ClassFile classFile = null;
        while ((classFile = i.getNextClassFile()) != null)
        {
            String className = classFile.getFullClassName();
            if (!className.contains("$"))
            {
                moduleClassList.add(className);
            }
        }
        return moduleClassList;
    }

    /**
     * get classes
     *
     * @param moduleClassesList
     * @param pkg
     * @param dir
     */
    protected void getClasses(List<String> moduleClassesList, String pkg, File dir)
    {
        if (!dir.exists())
        {
            return;
        }
        File[] files = dir.listFiles();
        for (File f : files)
        {
            if (f.isDirectory())
            {
                String newPkg = pkg.equals("") ? f.getName() : pkg + "." + f.getName();
                getClasses(moduleClassesList, newPkg, f);
                continue;
            }
            if (f.getName().endsWith(".java"))
            {
                moduleClassesList
                        .add(pkg
                                + "."
                                + f.getName().substring(0,
                                f.getName().length() - ".java".length()));
            }

        }
    }

    /**
     * add a class to the module table
     *
     * @param classes
     * @param moduleName
     */
    protected void addToClassModuleTable(List<String> classes, String moduleName)
    {
        for (String key : classes)
        {
            if (classModuleTable.containsKey(key))
            {
                List<String> modules = classModuleTable.get(key);
                modules.add(moduleName);
            }
            else
            {
                List<String> modules = new ArrayList<String>();
                modules.add(moduleName);
                classModuleTable.put(key, modules);
            }
        }
    }

    /**
     * report overrides
     */
    protected void reportOverrides()
    {
        numberOfOverrides = 0;
        Set<String> keys = classModuleTable.keySet();
        if (keys.size() > 0)
        {
            System.out.println("Overrides:\n");
        }
        for (String key : keys)
        {
            List<String> modules = classModuleTable.get(key);
            if (modules.size() <= 1)
            {
                continue;
            }
            System.out.println(key);
            String overrides = "  ";
            for (String m : modules)
            {
                if (!overrides.equals("  "))
                {
                    overrides += " --> ";
                }
                overrides += m;
            }
            System.out.println(overrides + "\n");
            numberOfOverrides++;
        }
        System.out.println("\nNumber of overrides: " + numberOfOverrides);
    }

}
