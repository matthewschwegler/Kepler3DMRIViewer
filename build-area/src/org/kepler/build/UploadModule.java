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

import org.apache.tools.ant.taskdefs.Delete;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.util.CommandLine;
import org.kepler.build.util.SVN;

/**
 * upload a module to SVN
 * Created by David Welker.
 * Date: Oct 1, 2008
 * Time: 4:38:22 PM
 */
public class UploadModule extends ModulesTask
{
    private String module = "undefined";
    private String suite = "undefined";
    private String as;
    private final String SVN_BASE_URL = "https://code.kepler-project.org/code/kepler/";

    /**
     * set the module
     *
     * @param module
     */
    public void setModule(String module)
    {
        this.module = module;
    }

    /**
     * set the suite
     *
     * @param suite
     */
    public void setSuite(String suite)
    {
        this.suite = suite;
    }

    /**
     * set as another module
     *
     * @param as
     */
    public void setAs(String as)
    {
        this.as = as;
    }

    /**
     * execute the task
     */
    public void run() throws Exception
    {
        String module = "";
        if (this.module.equals("undefined"))
        {
            if (suite.equals("undefined"))
            {
                PrintError.mustDefineSuiteOrModule();
            }
            else
            {
                module = suite;
            }
        }
        else
        {
            module = this.module;
        }

        //First, create a link to the module directory that will uploaded
        File moduleDir = new File(basedir, module);

        if (!moduleDir.exists() || !moduleDir.isDirectory())
        {
            System.out.println("Error: No module directory named " + module + " exists");
            return;
        }

        //Second, create the SVN URL where the module will be imported.
        String url;

        if (as == null)
        {
            as = module;
        }

        if (module.contains("-tag"))
        {
            url = SVN_BASE_URL + "tags/" + as;
        }
        else if (module.endsWith("branch"))
        {
            url = SVN_BASE_URL + "branches/" + as;
        }
        else
        {
            url = SVN_BASE_URL + "trunk/modules/" + as;
        }

        SVN.getUsernameAndPassword(this);

        String osName = System.getProperty("os.name").toLowerCase().trim();

        if (osName.startsWith("windows"))
        {
            System.out.println("Manually enter this command on Windows to import:");
            System.out.println("svn import -m \"Uploading the " + as + " module.\" " + moduleDir.getAbsolutePath() + " " + url + " --username " + SVN.username + " --password " + SVN.password);
            return;
        }


        String[] importCommand = {"svn", "import",
                "-m \"Uploading the " + as + " module.\"", moduleDir.getAbsolutePath(),
                url, "--username", SVN.username, "--password", SVN.password};

        //Fourth, execute the SVN import command.
        System.out.println("Uploading " + module + "...");
        CommandLine.exec(importCommand);

        //TODO: For added safety, change to temporary move and verify download before delete.
        Delete delete = new Delete();
        delete.bindToOwner(this);
        delete.setDir(moduleDir);
        delete.execute();

        Get get = new Get();
        get.bindToOwner(this);
        get.setModule(module);
        get.execute();

    }
}
