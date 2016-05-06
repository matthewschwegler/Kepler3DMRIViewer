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
package org.kepler.build.project;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;

/**
 * class that represents the compile classpath
 *
 * @author berkley
 */
public class CompileClasspath extends Classpath
{
    /**
     * constructor. create the compile classpath
     */
    public CompileClasspath(Module module)
    {
        super(ProjectLocator.getAntProject());
        type = "COMPILE";
        List<Module> lowerPriorityModules = ModuleTree.instance()
                .getLowerPriorityModules(module);
        for (Module m : lowerPriorityModules)
        {
            this.createPathElement().setLocation(m.getTargetClasses());
        }

        //Second, add all of the relevant jar files.
        Path jarClasspath = getJarsPath(module);
        for (Module m : lowerPriorityModules)
        {
            jarClasspath.append(getJarsPath(m));
        }

        append(jarClasspath);
    }

    /**
     * get the jar path for each module
     */
    private Path getJarsPath(Module module)
    {
        Path jarPath = new Path(ProjectLocator.getAntProject());
        if (module.isReleased())
        {
            if (module.getTargetJar().exists())
            {
                FileSet targetJarFileset = new FileSet();
                targetJarFileset.setProject(ProjectLocator.getAntProject());
                targetJarFileset.setDir(module.getTargetDir());
                targetJarFileset.setIncludes(module.getTargetJar().getName());
                jarPath.addFileset(targetJarFileset);
            }
        }

        if (!module.getLibDir().isDirectory())
        {
            return jarPath;
        }

        //Get any jars in the lib directory.
        FileSet jarFileset = new FileSet();
        jarFileset.setProject(ProjectLocator.getAntProject());
        jarFileset.setDir(module.getLibDir());
        jarFileset.setIncludes("**/*.jar");
        jarPath.addFileset(jarFileset);

        // Add the jars in ptolemy, which are located in src/lib
        if (module.getStemName().startsWith(Module.PTOLEMY))
        {
            File srcFile = module.getSrc();
            File srcLibFile = new File(srcFile, "lib");
            if (srcLibFile.isDirectory())
            {
                FileSet srcJarFileset = new FileSet();
                srcJarFileset.setProject(ProjectLocator.getAntProject());
                srcJarFileset.setDir(srcLibFile);
                srcJarFileset.setIncludes("**/*.jar");
                jarPath.addFileset(srcJarFileset);
            }
        }

        return jarPath;
    }
}
