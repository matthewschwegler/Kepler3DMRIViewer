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
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;

/**
 * class to create a package
 * Created by David Welker.
 * Date: Oct 24, 2008
 * Time: 4:25:48 PM
 */
public class Package extends ModulesTask
{
    private String suite;
    private String name;

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
     * set the name of the package
     *
     * @param name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        if (suite.equals("undefined") && name.equals("undefined"))
        {
            PrintError
                    .message("You must specify a name. (i.e. 'ant package -Dname=<name>");
            return;
        }
        else if (name.equals("undefined"))
        {
            name = suite;
        }

        if (!suite.equals("undefined"))
        {
            ChangeTo c = new ChangeTo();
            c.bindToOwner(this);
            c.init();
            c.setSuite(suite);
            c.execute();
        }

        CompileModules c = new CompileModules();
        c.bindToOwner(this);
        c.init();
        c.execute();

        CleanKar ck = new CleanKar();
        ck.bindToOwner(this);
        ck.init();
        ck.execute();

        MakeKars mk = new MakeKars();
        mk.bindToOwner(this);
        mk.init();
        mk.execute();

        File from = new File(basedir, "build-area/target/kepler-tasks.jar");
        File to = new File(basedir, name + ".jar");

        Copy copy = new Copy();
        copy.bindToOwner(this);
        copy.init();
        copy.setFile(from);
        copy.setTofile(to);
        copy.execute();

        Zip zip = new Zip();
        zip.bindToOwner(this);
        zip.init();
        zip.setBasedir(basedir);
        String includes = "";

        for (Module module : moduleTree)
        {
            includes += module + "/**,";
        }
        includes += "build-area/**," + name + ".jar," + suite + "/**";
        zip.setIncludes(includes);
        zip.setExcludes("**/.svn/**");
        zip.setDestFile(new File(basedir, name + ".zip"));
        zip.execute();

    }

}
