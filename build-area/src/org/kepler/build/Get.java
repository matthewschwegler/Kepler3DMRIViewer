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
import java.io.IOException;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleUtil;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.modules.OSRegistryTxt;
import org.kepler.build.modules.Suite;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.RepositoryLocations;
import org.kepler.build.util.CommandLine;

/**
 * Class to get a module or suite
 * Created by David Welker. Date: Aug 13, 2008 Time: 12:17:19 PM
 */
public class Get extends ModulesTask
{
    private String suiteName = "undefined";
    private String moduleName = "undefined";
    private String ptBranch = null;
    private boolean innerGet = false;

    /**
     * set the suite to get
     *
     * @param suite
     */
    public void setSuite(String suite)
    {
        this.suiteName = suite;
    }

    /**
     * set the module to get
     *
     * @param module
     */
    public void setModule(String module)
    {
        this.moduleName = module;
    }

    /**
     * set an inner get (gets modules within a suite)
     *
     * @param innerGet
     */
    public void setInnerGet(boolean innerGet)
    {
        this.innerGet = innerGet;
    }

    /**
     * set a ptolem branch to use
     *
     * @param ptBranch
     */
    public void setPtolemyBranch(String ptBranch)
    {
        this.ptBranch = ptBranch;
    }

    /**
     * Run the task.
     */
    @Override
    public void run() throws Exception
    {
        if (suiteName.equals("undefined") && moduleName.equals("undefined"))
        {
            PrintError.mustDefineSuiteOrModule();
            return;
        }

        if (!suiteName.equals("undefined"))
        {
            getSuite();
        }
        else
        {
            getModule(Module.make(moduleName));
        }
    }

    /**
     * get the suite
     *
     * @throws IOException
     */
    public void getSuite() throws IOException
    {
        if (!innerGet)
        {
            System.out.println("Retrieving modules....\n");
        }

        Suite s = Suite.make(suiteName);
        getModule(s);
        ModulesTxt modulesTxt = s.getModulesTxt();
        if (!modulesTxt.exists())
        {
            PrintError.notASuite(s);
            return;
        }
        modulesTxt.read();
        for (Module module : s.getModulesTxt())
        {
            if (!OSRegistryTxt.isCompatibleWithCurrentOS(module))
                continue;
            if (module.isSuite())
            {
                Get get = new Get();
                get.bindToOwner(this);
                get.init();
                get.setInnerGet(true);
                get.setSuite(module.getName());
                if (ptBranch != null)
                {
                    get.setPtolemyBranch(ptBranch);
                }
                get.execute();
            }
            else
            {
                getModule(module);
            }
        }

    }

    /**
     * The purpose of this function is to ensure that the modules.txt that is
     * visible in the repository without unzipping dominates in any
     * released suite.
     *
     * @param file
     * @param module
     */
    private void unzip(File file, Module module)
    {
        File modulesTxt = module.getModulesTxt();
        File modulesTxtTemp = new File(module.getModuleInfoDir(), "modules.txt.temp");
        modulesTxt.renameTo(modulesTxtTemp);
        ModuleUtil.unzip(file, module.getDir());
        modulesTxtTemp.renameTo(modulesTxt);
        System.out.println();
    }

    /**
     * get a module
     *
     * @param module
     * @throws IOException
     */
    public void getModule(Module module) throws IOException
    {
        File moduleDir = module.getDir();

        if (moduleDir.isDirectory() && moduleDir.list().length != 0)
        {
            if (moduleDir.list().length == 1)
            {
                File loneFile = moduleDir.listFiles()[0];
                if (loneFile.getName().equals(".svn"))
                {
                    String[] updateCommand = {"svn", "update",
                            moduleDir.getAbsolutePath()};
                    System.out.println("Updating " + module + "...");
                    CommandLine.exec(updateCommand);
                    return;
                }
                if (loneFile.getName().endsWith(".zip"))
                {
                    ModuleUtil.unzip(loneFile, moduleDir);
                    return;
                }
            }

            if (moduleDir.list().length == 2)
            {
                for (File f : moduleDir.listFiles())
                {
                    if (f.getName().endsWith(".zip"))
                    {
                        unzip(f, module);
                    }
                }
            }

            //if( !innerGet )
            System.out.println("The module " + module + " already exists.");
            return;
        }

        System.out.println(module + ":");

        // FIXME http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5489
        // Rework this code to only print a downloading message when downloading.
        System.out.println("Downloading (if necessary) " + module + "...");
        //System.out.println("Downloading " + module + "...");        
        String rev = "head";
        String[] checkoutCommand;

        //FIXME hardcodes. see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5440
        // ptolemy-kepler-2.2 contains the ptolemy src
        if (!module.getName().equalsIgnoreCase(Module.PTOLEMY_KEPLER_2_2) &&
        		module.isPtolemy())
        {
            checkoutCommand = new String[]{"svn", "co", "-r", rev, RepositoryLocations.getLocation(module.getName()), moduleDir.getAbsolutePath()};
        }
        else
        {
            checkoutCommand = new String[]{"svn", "co", "-r", rev, module.getLocation(), moduleDir.getAbsolutePath()};
        }

        CommandLine.exec(checkoutCommand);
        if (CommandLine.errorExecutingLastProcess())
        {
            module.delete();
            return;
        }

        OSRegistryTxt.add(module);

        // get the ptolemy source from ptolemy svn if this is not a released
        // version of the ptolemy module or ptolemy-kepler-2.2, which includes the source
        
        //FIXME hardcodes. see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5440
        if (!module.getName().equalsIgnoreCase(Module.PTOLEMY_KEPLER_2_2) &&
        		module.isPtolemy() && !module.isReleased())
        {

            if (ptBranch != null)
            {
                checkoutCommand = new String[]{"svn", "co", ptBranch, module.getSrc().getAbsolutePath()};
            }
            else
            {
                rev = ModuleUtil.readPtolemyRevision(module);
                System.out.println("ptolemy revision: " + rev);
                checkoutCommand = new String[]{"svn", "co", "-r", rev, module.getLocation(), module.getSrc().getAbsolutePath()};
            }

            CommandLine.exec(checkoutCommand);
            if (CommandLine.errorExecutingLastProcess())
            {
                module.delete();
                return;
            }
        }

        if (module.getName().equals("kepler-1.0-jar-tag"))
        {
            reorganizeKepler();
        }

        //HERE
        if (moduleDir.isDirectory()
                && (moduleDir.list().length == 2 || moduleDir.list().length == 3))
        {
            for (File f : moduleDir.listFiles())
            {
                if (f.getName().endsWith(".zip"))
                {
                    unzip(f, module);
                }
            }
        }
        
        // finally get any maven dependencies
        Maven maven = new Maven();
        maven.bindToOwner(this);
        maven.init();
        maven.setModule(module.getName());
        maven.execute();
        return;
    }

    /**
     * reorganize after downloading a versioned jar
     */
    private void reorganizeKepler()
    {
        System.out.println("Unjarring kepler-1.0-jar-tag/kepler-1.0.jar...");
        Module kepler10JarTag = Module.make("kepler-1.0-jar-tag");
        File keplerDir = kepler10JarTag.getDir();
        ModuleUtil.unzip(new File(keplerDir, "kepler-1.0.jar"), keplerDir);
    }
}
