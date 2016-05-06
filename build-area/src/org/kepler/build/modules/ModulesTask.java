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

import java.io.File;
import java.util.Hashtable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.RepositoryLocations;

/**
 * Class to make a module task
 * Created by David Welker. Date: Sep 22, 2008 Time: 3:30:16 PM
 */
public abstract class ModulesTask extends Task
{
    protected static File basedir;

    protected ModuleTree moduleTree;
    protected ModulesTxt modulesTxt;
    protected File ptolemyCompiled;
    protected File ptolemyHead;

    protected String releaseLocation = null;

    //If this is a test release, use the location for test releases.

    public void setTest(boolean isTest)
    {
        releaseLocation = isTest ? RepositoryLocations.TEST_RELEASES : RepositoryLocations.RELEASED;
        RepositoryLocations.setReleaseLocation(releaseLocation);
        Module.reset();
        ModuleTree.init();
        modulesTxt = ModulesTxt.instance();
        moduleTree = ModuleTree.instance();
    }


    //key = module-name, value = version
    protected static Hashtable<String, String> versions = new Hashtable<String, String>();

    /**
     * init the task
     */
    public void init() throws BuildException
    {
        basedir = getProject().getBaseDir();
        ptolemyCompiled = new File(basedir, ".ptolemy-compiled");
        ptolemyHead = new File(basedir, "ptolemy-head");
        ProjectLocator.setKeplerModulesDir(basedir);

        if (!ModulesTxt.instance().exists())
        {
            Copy copy = new Copy();
            copy.bindToOwner(this);
            copy.setFile(new File(ProjectLocator.getBuildDir(),
                    "resources/modules.default"));
            copy.setTofile(ModulesTxt.instance());
            copy.execute();
        }
        ModuleTree.init();

        modulesTxt = ModulesTxt.instance();
        moduleTree = ModuleTree.instance();

    }

    /**
     * execute the task
     */
    @Override
    public void execute() throws BuildException
    {
        try
        {
            run();
        }
        catch (Exception e)
        {
            throw new BuildException(e);
        }
    }

    /**
     * run the task
     *
     * @throws Exception
     */
    public abstract void run() throws Exception;

    /**
     * set the project
     */
    @Override
    public void setProject(Project project)
    {
        super.setProject(project);
        ProjectLocator.setAntProject(project);
    }

    /**
     * set the property
     *
     * @param name
     * @return
     */
    protected String getProperty(String name)
    {
        return getProject().getProperty(name);
    }
}