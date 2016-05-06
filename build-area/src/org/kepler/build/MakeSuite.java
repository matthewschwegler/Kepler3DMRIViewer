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

import org.kepler.build.modules.ModulesTask;

/**
 * Class to create a new suite.
 * Created by David Welker.
 * Date: Sep 19, 2008
 * Time: 4:15:28 PM
 */
public class MakeSuite extends ModulesTask
{
    private String name;

    /**
     * set the name
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
            System.out.println("e.g. ant make-suite -Dname=suite.name");
            return;
        }

        System.out.println("Making a suite named " + name + ".");

        //Make the files and directories associated with a suite.
        //System.out.println("Making a new suite named " + name);
        File moduleDir = new File(basedir, name);
        File moudleInfoDir = new File(moduleDir, "module-info");
        moudleInfoDir.mkdirs();
        File modulesTxt = new File(moudleInfoDir, "modules.txt");
        modulesTxt.createNewFile();
    }
}