/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author$'
 * '$Date$'
 * '$Revision$'
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
package org.kepler.build.modules;

import java.io.File;

import org.apache.tools.ant.taskdefs.Copy;
import org.kepler.build.project.ProjectLocator;

/**
 * Created by David Welker.
 * Date: Aug 30, 2010
 * Time: 4:50:55 PM
 */
public class ModuleLocationRegistryTxt
{
    private static File moduleLocationRegistryTxt = null;

    public static File instance()
    {
        if (moduleLocationRegistryTxt == null)
        {
            File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ?
                    ProjectLocator.getUserBuildDir() :
                    ProjectLocator.getBuildDir();
            if (!buildDir.isDirectory())
            {
                buildDir.mkdirs();
            }
            moduleLocationRegistryTxt = new File(buildDir, "module-location-registry.txt");
            File localModuleLocationRegistryTxt = new File(ProjectLocator.getBuildDir(), "module-location-registry.txt");
            if (!moduleLocationRegistryTxt.exists() && localModuleLocationRegistryTxt.exists())
            {
                Copy copy = new Copy();
                copy.setProject(ProjectLocator.getAntProject());
                copy.init();
                copy.setOverwrite(true);
                copy.setFile(localModuleLocationRegistryTxt);
                copy.setTofile(moduleLocationRegistryTxt);
                copy.execute();
            }

            File registryTxt = new File(buildDir, "registry.txt");
            File localRegistryTxt = new File(ProjectLocator.getBuildDir(), "registry.txt");
            if( !registryTxt.exists() && localRegistryTxt.exists() )
            {
                Copy copy = new Copy();
                copy.setProject(ProjectLocator.getAntProject());
                copy.init();
                copy.setOverwrite(true);
                copy.setFile(localRegistryTxt);
                copy.setTofile(registryTxt);
                copy.execute();
            }


        }
        return moduleLocationRegistryTxt;
    }
}
