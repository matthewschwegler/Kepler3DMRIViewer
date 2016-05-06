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

import org.apache.tools.ant.taskdefs.Delete;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;

/**
 * class to clean the build
 * Created by David Welker.
 * Date: Aug 22, 2008
 * Time: 2:56:01 PM
 */
public class Clean extends ModulesTask
{
    protected boolean tests = false;
    protected String moduleName = "all";

    /**
     * set the module to clean
     *
     * @param moduleName
     */
    public void setModule(String moduleName)
    {
        this.moduleName = moduleName.equals("undefined") ? "all" : moduleName;
    }
    
    /**
     * run the task
     */
    public void run() throws Exception
    {
        // If the module "all" is specified, clean all the modules in modules.txt.
        if (moduleName.equals("all"))
        {
            for (Module module : moduleTree)
            {
                if(!_cleanOnlyTests)
                {
                    clean(module);
                }
                cleanTests(module);
            }
        }
        // Clean only the specified module.
        else
        {
            Module module = Module.make(moduleName);
            if(!_cleanOnlyTests)
            {
                clean(module);
            }
            cleanTests(module);
        }
    }
    
    /** Set the TestsOnly attribute. If value is true, only clean test
     *  classes, otherwise clean both test and non-test classes.
     */
    public void setTestsOnly(String value)
    {
        if(value.equals("true"))
        {
            _cleanOnlyTests = true;
        }
    }

    /**
     * Delete the target folder of the specified module.
     *
     * @param module The module to be deleted.
     */
    protected void clean(Module module)
    {
        if (module.isReleased())
        {
            return;
        }

        // Delete the module in question.
        Delete delete = new Delete();
        delete.bindToOwner(this);
        delete.init();
        delete.setDir(module.getTargetClasses());
        delete.execute();

        if (module.getName().equals(Module.PTOLEMY))
        {
            ptolemyCompiled.delete();
        }
    }

    /**
     * delete the compiled classes in the tests dir
     */
    protected void cleanTests(Module module)
    {
        if (module.isReleased() || !module.getTestsClassesDir().isDirectory())
        {
            return;
        }

        Delete delete = new Delete();
        delete.bindToOwner(this);
        delete.init();
        delete.setDir(module.getTestsClassesDir());
        delete.execute();
    }
    
    /** If true, only clean classes for tests, otherwise remove all classes. */
    private boolean _cleanOnlyTests = false;
}
