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
package org.kepler.build.ide;

import java.io.File;

import org.apache.tools.ant.taskdefs.Delete;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;

/**
 * Clean all generated IDE Files
 * Created by David Welker.
 * Date: Sep 18, 2008
 * Time: 12:18:21 PM
 */
public abstract class CleanIDE extends ModulesTask
{
    /**
     * run the task
     */
    public void run() throws Exception
    {
        generalClean();

        for (Module m : moduleTree)
        {
            cleanModule(m);
        }
    }

    public abstract void generalClean();

    public abstract void cleanModule(Module module);

    /**
     * delete a file in basedir
     *
     * @param relative
     */
    public void delete(String relative)
    {
        delete(new File(basedir, relative));
    }

    /**
     * delete the files out of a module
     *
     * @param module
     * @param relative
     */
    public void delete(Module module, String relative)
    {
        delete(module.getDir(), relative);
    }

    /**
     * delete a file in a dir
     *
     * @param dir
     * @param relative
     */
    public void delete(File dir, String relative)
    {
        delete(new File(dir, relative));
    }

    /**
     * delete a file
     *
     * @param file
     */
    public void delete(File file)
    {
        if (!file.exists())
        {
            return;
        }
        Delete delete = new Delete();
        delete.bindToOwner(this);
        delete.init();
        if (file.isDirectory())
        {
            delete.setDir(file);
        }
        else
        {
            delete.setFile(file);
        }
        delete.execute();
    }
}
