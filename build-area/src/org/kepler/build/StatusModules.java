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

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.util.CommandLine;

/**
 * A class to run svn status in each module directory.
 *
 * @author Daniel Crawl
 * @version $Id: StatusModules.java 31789 2013-03-27 22:03:48Z crawl $
 */
public class StatusModules extends ModulesTask
{
    /**
     * run the task
     */
    public void run() throws Exception
    {
        for (Module module : moduleTree)
        {
            String[] statusCommand = {"svn", "stat",
                    module.getDir().getAbsolutePath()};
            CommandLine.exec(statusCommand);
            
            if (module.isPtolemy())
            {
            	String[] ptolemyStatusCommand = {"svn", "stat",
                        module.getSrc().getAbsolutePath()};
                CommandLine.exec(ptolemyStatusCommand);
            }
        }
        
        // build-area isn't in modules.txt
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules()
        		? ProjectLocator.getUserBuildDir()
        		: ProjectLocator.getBuildDir();
        String[] statusCommand = {"svn", "stat",
        		buildDir.getAbsolutePath()};
        CommandLine.exec(statusCommand);

    }

}
