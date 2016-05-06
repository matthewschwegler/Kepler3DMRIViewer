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

import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.RepositoryLocations;
import org.kepler.build.util.CommandLine;
import org.kepler.build.util.SVN;

/**
 * Publish the ptolemy module
 * Created by David Welker.
 * Date: Oct 8, 2009
 * Time: 11:07:16 PM
 */
public class PublishPtolemy extends ModulesTask
{
    private String version;

    /**
     * set the version to publish
     *
     * @param version
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        if (!version.matches("\\d+\\.\\d+\\.\\d+"))
        {
            PrintError
                    .message("When you publish the ptolemy module, you must specify a version X.Y.Z");
            return;
        }

        File modulesTxtCopy = new File(basedir, ".modulesTxtCopy");
        Copy copy = new Copy();
        copy.bindToOwner(this);
        copy.init();
        copy.setFile(ModulesTxt.instance());
        copy.setTofile(modulesTxtCopy);
        copy.execute();

        ChangeTo changeTo = new ChangeTo();
        changeTo.bindToOwner(this);
        changeTo.init();
        changeTo.setSuite(Module.PTOLEMY);
        changeTo.execute();

        JarModules jar = new JarModules();
        jar.bindToOwner(this);
        jar.init();
        jar.execute();

        Module module = Module.make(Module.PTOLEMY);
        String moduleName = Module.PTOLEMY;

        Zip zip = new Zip();
        zip.bindToOwner(this);
        zip.setBasedir(module.getDir());
        zip.setExcludes("src/**");
        zip.setExcludes("target/classes/**");
        zip.setExcludes("target/eclipse/**");
        zip.setExcludes("target/idea/**");
        zip.setExcludes("target/netbeans/**");
        File publishDir = new File(basedir, ".publish");
        File publishModuleDir = new File(publishDir, moduleName + "." + version);
        publishModuleDir.mkdirs();
        String destfilename = moduleName + "." + version + ".zip";
        zip.setDestFile(new File(publishModuleDir, destfilename));
        zip.execute();

        //If a module-info dir exists in this module, copy it so that it is available.
        File moduleInfoDir = module.getModuleInfoDir();
        if (moduleInfoDir.exists())
        {
            copy = new Copy();
            copy.bindToOwner(this);
            copy.setTodir(publishModuleDir);
            FileSet dirToCopy = new FileSet();
            dirToCopy.setDir(module.getDir());
            dirToCopy.setIncludes("module-info/**");
            copy.add(dirToCopy);
            copy.execute();
        }

        SVN.getUsernameAndPassword(this);
        String modulePublishingLocation = RepositoryLocations.RELEASED + "/"
                + moduleName + "." + version;
        String[] importCommand = {"svn", "import",
                "-m \"[build-system] " + destfilename + "...\"",
                publishModuleDir.getAbsolutePath(), modulePublishingLocation,
                "--username", SVN.username, "--password", SVN.password};
        CommandLine.exec(importCommand);

        copy = new Copy();
        copy.bindToOwner(this);
        copy.init();
        copy.setOverwrite(true);
        copy.setFile(modulesTxtCopy);
        copy.setTofile(ModulesTxt.instance());
        copy.execute();

        publishDir.deleteOnExit();
        modulesTxtCopy.deleteOnExit();
    }
}
