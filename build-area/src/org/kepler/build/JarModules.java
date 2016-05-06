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
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.FileSet;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;

/**
 * class to jar modules
 * Created by David Welker.
 * Date: Aug 22, 2008
 * Time: 5:28:46 PM
 * $Id: JarModules.java 32742 2014-06-03 00:35:09Z crawl $
 */
public class JarModules extends ModulesTask
{
    private boolean useVersions;
    private String moduleName = "undefined";
    
    /**
     * set the module to compile
     *
     * @param moduleName
     */
    public void setModule(String moduleName)
    {
        this.moduleName = moduleName;
    }

    /**
     * Set the versions to use.
     */
    public void setUseVersions(boolean useVersions)
    {
        this.useVersions = useVersions;
    }

    /**
     * Run the task.
     */
    @Override
    public void run() throws Exception
    {
        if(moduleName.equals("undefined"))
        {
            for (Module module : moduleTree)
            {
                jar(module);
            }
        }
        else
        {
            Module module = moduleTree.getModuleByStemName(moduleName);
            if(module == null)
            {
                PrintError.message("Module " + moduleName + " is not in the current suite.");
            }
            else
            {
                jar(module);
            }
        }
    }

    /**
     * Create a file set.
     */
    private FileSet createFileSet(File dir, Module module)
    {
        FileSet fs = new FileSet();
        fs.setProject(getProject());
        fs.setDir(dir);
        fs.setExcludes("**/*.jar");

        if(module.isPtolemy())
        {
            fs.setExcludesfile(new File(basedir, "build-area/settings/ptolemy-excludes"));
        }

        return fs;
    }

    /**
     * Add a fileset.
     *
     * @param jar
     * @param dir
     * @return
     */
    private int addFileset(Jar jar, File dir, Module module)
    {
        FileSet fs = createFileSet(dir, module);
        jar.addFileset(fs);
        return fs.size();
    }

    /**
     * Jar a module.
     *
     * @param module
     * @throws Exception
     */
    private void jar(Module module) throws Exception
    {
        String jarName = module.getName();
        if (useVersions)
        {
            jarName += "-" + versions.get(jarName);
        }
        File dest = new File(module.getTargetDir(), jarName + ".jar");
        dest.getParentFile().mkdirs();

        Jar jar = new Jar();
        jar.bindToOwner(this);
        jar.init();
        jar.setDestFile(dest);

        List<File> dirs = new ArrayList<File>();

        dirs.add(module.getTargetClasses());
        // include the source directory for all modules including ptolemy.
        // addFileset() uses the ptolemy-excludes file to excludes directories
        // not used by kepler. the source directory for ptolemy is included
        // since we need ptolemy/actor/ActorModule.properties for graphical
        // actors.
        dirs.add(module.getSrc());
        dirs.add(module.getResourcesDir());
        // include the configs directory for all modules, not just common
        // since other modules may override files in this directory.
        dirs.add(module.getConfigsDir());


        int numFilesToJar = 0;
        for (File dir : dirs)
        {
            if (dir.exists() && dir.isDirectory())
            {
                numFilesToJar += addFileset(jar, dir, module);
            }
        }
        if (numFilesToJar > 0)
        {
            jar.execute();
        }
    }
}
