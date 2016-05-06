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
import org.apache.tools.ant.types.FileSet;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;

/**
 * class to jar from a collapsed modules hierarchy
 */
public class Deploy extends ModulesTask
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

        System.out.println(new File(basedir,"build-area").getAbsolutePath() + " classes");
        FileSet fs = new FileSet();
        fs.setProject(ProjectLocator.getAntProject());
        fs.setDir(new File(basedir,"build-area"));
        fs.setExcludes("**/*.iml");
        fs.setExcludes("src/");
        fs.setExcludes("tests/");
        fs.setExcludes("module-info/");
        Copy copy = new Copy();
        copy.bindToOwner(this);
        copy.init();
        copy.setTodir(new File(basedir, name));
        copy.addFileset(fs);
        copy.execute();

        for (Module module : moduleTree.reverse()) {
            fs = new FileSet();
            fs.setProject(ProjectLocator.getAntProject());
            fs.setDir(new File(module.getDir().getAbsolutePath()));
            fs.setExcludes("**/*.iml");
            fs.setExcludes("src/");
            fs.setExcludes("tests/");
            fs.setExcludes("module-info/");
            System.out.println(module.getDir().getAbsolutePath() + " classes");
            copy = new Copy();
            copy.bindToOwner(this);
            copy.init();
            copy.setTodir(new File(basedir, name));
            copy.addFileset(fs);
            copy.execute();

            if (new File(module.getDir().getAbsolutePath()+"/src").exists()) {
                fs = new FileSet();
                fs.setProject(ProjectLocator.getAntProject());
                fs.setDir(new File(module.getDir().getAbsolutePath()+"/src"));
                fs.setIncludes("**/*.properties");
                System.out.println(module.getDir().getAbsolutePath() + " properties");
                copy = new Copy();
                copy.bindToOwner(this);
                copy.init();
                copy.setTodir(new File(basedir, name+"/target/classes"));
                copy.addFileset(fs);
                copy.execute();
            }
        }

/*
        FileSet fss = new FileSet();

        Manifest.Attribute a = new Manifest.Attribute();
        a.addValue();

        Manifest mf = new Manifest();
        mf.addConfiguredAttribute(a);

        Jar jr = new Jar();
        jr.bindToOwner(this);
        jr.init();
        jr.setBasedir(new File(basedir, name));
        jr.addConfiguredManifest(mf);
        jr.setDestFile(new File(basedir, name + ".jar"));
        jr.execute();
*/
    }

}
