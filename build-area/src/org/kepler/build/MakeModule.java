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
import org.kepler.build.project.PrintError;

/**
 * Class to create a new module
 * Created by David Welker.
 * Date: Sep 19, 2008
 * Time: 4:15:58 PM
 */
public class MakeModule extends ModulesTask
{
    String name;

    /** Set the name of the new module. */
    public void setName(String name)
    {
        this.name = name;
    }

    /** Create a module. */
    public void run() throws Exception
    {
        //Sanity check: make sure that the user has defined a name for the module.
        if (name.equals("undefined"))
        {
            PrintError.message("You must define a name for the module.\n" +
                    "e.g., ant make-module -Dname=the-module");
            return;
        }

        System.out.println("Making a module named " + name + ".");

        //Make the directories associated with a source module.
        File moduleDir = new File(basedir, name);
        
        File srcDir = new File(moduleDir, "src");
        _mkdirIfDoesNotExist(srcDir);
        
        // FIXME these locations are duplicated in Module.java
        
        File systemPropDir = new File(moduleDir, "resources/system.properties");
        _mkdirIfDoesNotExist(systemPropDir);
        
        File libExeDir = new File(moduleDir, "lib/exe");
        _mkdirIfDoesNotExist(libExeDir);
        
        File libJarDir = new File(moduleDir, "lib/jar");
        _mkdirIfDoesNotExist(libJarDir);
        
        File lib64Dir = new File(moduleDir, "lib64");
        _mkdirIfDoesNotExist(lib64Dir);

        File moduleInfoDir = new File(moduleDir, "module-info");
        _mkdirIfDoesNotExist(moduleInfoDir);
        
    }
        
    /** Create a directory if it does not exist. */
    private void _mkdirIfDoesNotExist(File dir)
    {
        if(!dir.exists() && !dir.mkdirs())
        {
            PrintError.message("WARNING: could not create directory " + dir);
        }
    }

}
