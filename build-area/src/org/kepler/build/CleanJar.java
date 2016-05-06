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
 * class to clean any created jars from the modules
 * Created by David Welker.
 * Date: Sep 30, 2008
 * Time: 11:42:26 AM
 */
public class CleanJar extends ModulesTask
{
    /**
     * run the task
     */
    public void run() throws Exception
    {
        for (Module m : moduleTree)
        {
            cleanJar(m);
        }
    }

    /**
     * Delete the jar file associated with on particular module.
     *
     * @param module The module from which the jar is to be deleted.
     */
    protected void cleanJar(Module module)
    {
        if (module.isReleased())
        {
            return;
        }
        Delete delete = new Delete();
        delete.bindToOwner(this);
        delete.setFile(module.getTargetJar());
        delete.execute();
    }
}