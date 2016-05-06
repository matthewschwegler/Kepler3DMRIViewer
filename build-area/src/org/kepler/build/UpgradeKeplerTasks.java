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
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.ProjectLocator;

/**
 * task to upgrade the kepler-tasks module
 *
 * @author berkley
 */
public class UpgradeKeplerTasks extends ModulesTask
{
    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        File keplerTasksJar = new File(ProjectLocator.getBuildDir(),
                "target/kepler-tasks.jar");
        Module keplerTasks = Module.make("kepler-tasks");

        File target = new File(keplerTasks.getLibDir(), "jar");

        System.out.println("Copying file: " + keplerTasksJar);
        System.out.println("To: " + target);

        Copy copy = new Copy();
        copy.bindToOwner(this);
        copy.init();
        copy.setFile(keplerTasksJar);
        copy.setTodir(target);
        copy.execute();
    }

}