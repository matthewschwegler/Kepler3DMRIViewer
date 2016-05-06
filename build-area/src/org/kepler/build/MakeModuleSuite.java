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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.kepler.build.modules.ModulesTask;

/**
 * Class to create a new module suite.
 *
 * @author davidwelker
 */
public class MakeModuleSuite extends ModulesTask
{
    String name;

    /**
     * set the name of the new suite
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
        //Sanity check: make sure that the user has defined a name for the module.
        if (name.equals("undefined"))
        {
            System.out.println("You must define a name for the module.");
            System.out.println("e.g., ant make-module-suite -Dname=the-module");
            return;
        }

        System.out.println("Making a hybrid module suite named " + name + ".");

        //Make the directories associated with a source module.
        //System.out.println("Making a new module named " + name);
        File moduleDir = new File(basedir, name);
        File srcDir = new File(moduleDir, "src");
        File systemPropDir = new File(moduleDir, "resources/system.properties");
        File libExeDir = new File(moduleDir, "lib/exe");
        srcDir.mkdirs();
        systemPropDir.mkdirs();
        libExeDir.mkdirs();
        File moudleInfoDir = new File(moduleDir, "module-info");
        moudleInfoDir.mkdirs();
        File modulesTxt = new File(moudleInfoDir, "modules.txt");
        writeDefaultModulesTxt(modulesTxt);
    }

    /**
     * write the new modules.txt file
     *
     * @param modulesTxt
     * @throws IOException
     */
    protected void writeDefaultModulesTxt(File modulesTxt) throws IOException
    {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(modulesTxt)));
        pw.println(name);
        pw.println("*kepler");
        pw.close();
    }

}
