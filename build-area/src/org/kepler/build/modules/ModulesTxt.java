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
package org.kepler.build.modules;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.taskdefs.Copy;
import org.kepler.build.project.ProjectLocator;

/**
 * class that represents a modules.txt file
 *
 * @author berkley
 */
public class ModulesTxt extends File implements Iterable<Module>
{
    private static ModulesTxt instance;

    /**
     * init the modules.txt
     * This may be called only after Locations.setProjectDir() is called.
     */
    public static void init()
    {
        instance = instance();
        instance.read();
    }

    public static boolean buildAreaExists()
    {
        File userBuildDir = ProjectLocator.getUserBuildDir();
        File buildDir = ProjectLocator.getBuildDir();
        if (!buildDir.isDirectory()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * get a singleton instance
     *
     * @return
     */
    public static ModulesTxt instance()
    {
        if (instance == null)
        {
            File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ?
                    ProjectLocator.getUserBuildDir() :
                    ProjectLocator.getBuildDir();
            if (!buildDir.isDirectory())
            {
                buildDir.mkdirs();
            }

            instance = new ModulesTxt(buildDir, "modules.txt");
            File localModulesTxt = new File(ProjectLocator.getBuildDir(), "modules.txt");
            if (!instance.exists() && localModulesTxt.exists())
            {
                Copy copy = new Copy();
                copy.setProject(ProjectLocator.getAntProject());
                copy.init();
                copy.setOverwrite(true);
                copy.setFile(localModulesTxt);
                copy.setTofile(instance);
                copy.execute();
            }
        }
        return instance;
    }

    public List<Module> modules = new ArrayList<Module>();

    /**
     * constructor
     *
     * @param pathname
     */
    public ModulesTxt(String pathname)
    {
        super(pathname);
    }

    /**
     * constructor
     *
     * @param uri
     */
    public ModulesTxt(URI uri)
    {
        super(uri);
    }

    /**
     * constructor
     *
     * @param parent
     * @param child
     */
    public ModulesTxt(String parent, String child)
    {
        super(parent, child);
    }

    /**
     * constructor
     *
     * @param parent
     * @param child
     */
    public ModulesTxt(File parent, String child)
    {
        super(parent, child);
    }

    /**
     * read the modules.txt file
     */
    public void read()
    {
        try
        {
            readHelper();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * read the modules.txt file
     *
     * @throws IOException
     */
    private void readHelper() throws IOException
    {
        modules.clear();
        BufferedReader br = new BufferedReader(new FileReader(this));
        String line = null;
        while ((line = br.readLine()) != null)
        {
            line = line.trim();
            //Ignore empty lines or comments that start with #.
            if (line.equals("") || line.startsWith("#"))
            {
                continue;
            }
            String[] parts = line.split("\\s+");
            String name = parts[0];
            String location = parts.length > 1 ? parts[1] : null;
            Module module = line.startsWith("*") ? new Suite(name.substring(1),
                    location) : new Module(name, location);
            modules.add(module);
        }
        br.close();
    }

    /**
     * write the file
     */
    public void write()
    {
        write(this);
    }

    /**
     * write a file
     *
     * @param location
     */
    public void write(File location)
    {
        try
        {
            writeHelper(location);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * write a file
     *
     * @param location
     * @throws IOException
     */
    private void writeHelper(File location) throws IOException
    {
        PrintWriter pw = new PrintWriter(new BufferedWriter(
                new FileWriter(location)));
        for (Module module : modules)
        {
            pw.println(module.writeString());
        }
        pw.close();
    }

    /**
     * add a module with a name
     *
     * @param name
     */
    public void add(String name)
    {
        modules.add(Module.make(name));
    }

    /**
     * add a module
     *
     * @param module
     */
    public void add(Module module)
    {
        modules.add(module);
    }

    /**
     * add a module to a specific location in the module list
     */
    public void add(Module module, int index)
    {
        modules.add(index, module);
    }

    /**
     * replace a module
     *
     * @param target
     * @param with
     */
    public void replace(Module target, Module with)
    {
        int index = modules.indexOf(target);
        modules.remove(target);
        modules.add(index, with);
    }

    /**
     * clear all modules
     */
    public void clear()
    {
        modules.clear();
    }

    /**
     * get an iterator of modules
     */
    public Iterator<Module> iterator()
    {
        return modules.iterator();
    }

    /**
     * get any additions to the modules.txt
     *
     * @param modulesTxt
     * @return
     */
    public List<Module> getAdditions(ModulesTxt modulesTxt)
    {
        List<Module> additions = new ArrayList<Module>();
        if (this.equals(modulesTxt))
        {
            return additions;
        }
        for (Module m : modulesTxt.modules)
        {
            if (!modules.contains(m))
            {
                additions.add(m);
            }
        }
        return additions;
    }

    /**
     * get any subtractions from modules.txt
     *
     * @param modulesTxt
     * @return
     */
    public List<Module> getSubtractions(ModulesTxt modulesTxt)
    {
        List<Module> subtractions = new ArrayList<Module>();
        if (this.equals(modulesTxt))
        {
            return subtractions;
        }
        for (Module m : modules)
        {
            if (!modulesTxt.modules.contains(m))
            {
                subtractions.add(m);
            }
        }
        return subtractions;
    }

    /**
     * returns true if this == obj
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        ModulesTxt other = (ModulesTxt) obj;
        if (modules == null)
        {
            if (other.modules != null)
            {
                return false;
            }
        }
        else
        {
            if (modules.size() != other.modules.size())
            {
                return false;
            }
            for (int i = 0; i < modules.size(); i++)
            {
                if (!modules.get(i).equals(other.modules.get(i)))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * return a string rep of this
     */
    public String toString()
    {
        String result = "";
        for (Module module : modules)
        {
            result += module.writeString();
        }
        return result;
    }

    private static final long serialVersionUID = -1487063011887178186L;
}
