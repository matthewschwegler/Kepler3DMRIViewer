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
import org.kepler.build.modules.Suite;

/**
 * class to clean the target directories of a suite
 *
 * @author berkley
 */
public class CleanTarget extends ModulesTask
{
    private Suite suite = null;

    /**
     * set the suite
     *
     * @param suite
     */
    public void setSuite(Suite suite)
    {
        this.suite = suite;
    }

    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        if (suite == null)
        {
            for (Module module : moduleTree)
            {
                cleanTarget(module);
            }
        }
        else
        {
            cleanTarget(suite);
        }

    }

    /**
     * clean the directories
     *
     * @param module
     */
    protected void cleanTarget(Module module)
    {
        Delete delete = new Delete();
        delete.bindToOwner(this);
        delete.init();
        delete.setDir(module.getTargetDir());
        delete.execute();
    }

}
