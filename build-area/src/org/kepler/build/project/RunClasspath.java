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
import java.util.Iterator;

import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.util.DotKeplerManager;

/**
 * The Run-time classpath.
 */
public class RunClasspath extends Classpath
{
    /**
     * Constructor.
     */
    public RunClasspath()
    {
        super(ProjectLocator.getAntProject());
        type = "RUN";
        createPathElement().setLocation(ModulesTxt.instance().getParentFile());
        for (Module module : ModuleTree.instance())
        {
            append(makeRunClasspathPart(module));
        }

        // add the files from the CLASSPATH environement variable
        String classpath = System.getenv("CLASSPATH");
        if(classpath != null && !classpath.trim().isEmpty()) {
            final Path path = new Path(ProjectLocator.getAntProject());
            for(String pathStr : classpath.split(File.pathSeparator)) {
                path.createPathElement().setPath(pathStr);
            }
            append(path);
            System.out.println("adding $CLASSPATH to RunClassPath: " + path);
        }
    }

    /**
     * Make part of the run classpath.
     */
    private Path makeRunClasspathPart(Module module)
    {
        Path classpathPart = new Path(ProjectLocator.getAntProject());
        File targetClassesDir = module.getTargetClasses();
        classpathPart.createPathElement().setLocation(targetClassesDir);
        classpathPart.createPathElement().setLocation(module.getSrc());
        //System.out.println("RunClasspath:" + m + " " + m.getSrc());
        classpathPart.createPathElement().setLocation(module.getResourcesDir());
        classpathPart.createPathElement().setLocation(module.getConfigsDir());

        // see if we should add the 64 bit library to the classpath
        // NOTE: we want the 64 bit library directory in the classpath
        // since ptolemy.data.expr.UtilityFunctions.loadLibrary() searches
        // the classpath for a JNI library if it is not found in
        // java.library.path.
        if (LibPath.use64BitLibs())
        {
            classpathPart.createPathElement().setLocation(module.getLib64Dir());
        }
        classpathPart.createPathElement().setLocation(module.getLibDir());
        classpathPart.createPathElement().setLocation(module.getLibImagesDir());

        // Add target jar if the target classes dir does not exist
        if (! targetClassesDir.exists())
        {
            if (module.getTargetJar().exists())
            {
                classpathPart.createPathElement().setLocation(module.getTargetJar());
            }
        }
        // End add target jar
        if (!module.getLibDir().exists())
        {
            return classpathPart;
        }

        //Get jars from the lib directory.
        if (module.getLibDir().isDirectory())
        {
            // use wildcards to reduce the size of the classpath.
            // this gets around the maximum path length on windows.
            DirSet jarDirs = new DirSet();
            jarDirs.setProject(ProjectLocator.getAntProject());
            jarDirs.setDir(module.getLibDir());
            jarDirs.setIncludes("**/*");
            Iterator<Resource> i = jarDirs.iterator();
            while (i.hasNext())
            {
                Resource resource = i.next();
                if(resource instanceof FileResource) {
                    File file = ((FileResource)resource).getFile();
                    //System.out.println(file);
                    if(file.isDirectory()) {
                        File wildcardFile = new File(file, "*");
                        classpathPart.createPathElement().setLocation(wildcardFile);
                        //System.out.println("adding to cp: " + wildcardFile);
                    }
                }
            }
            
        }
        // Add the jars in ptolemy, which are located in src/lib
        if (module.isPtolemy())
        {
            File srcFile = module.getSrc();
            File srcLibDir = new File(srcFile, "lib");
            if (srcLibDir.isDirectory())
            {
                File wildcardFile = new File(srcLibDir, "*");
                classpathPart.createPathElement().setLocation(wildcardFile);
            }
        }
        
        // add the workflow demos directory so that the demos may be accessed
        // by ptolemy.actor.gui.HTMLViewer when showing the Help documentation
        // see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5194        
        File userModuleWorkflowDir = new File(DotKeplerManager.getInstance()
                .getPersistentModuleWorkflowsDirString()
                + File.separator
                + module.getName()
                + File.separator
                + "demos");
        
        File systemModuleDemoDir = module.getDemosDir();
        // NOTE: the first time kepler starts with a new version of a module,
        // the demos directories have not been copied into KeplerData. if
        // the demo directory exists for the module in the location where
        // kepler is installed, add the demo directory for where it will be
        // copied once kepler starts.
        // see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5895
        if (systemModuleDemoDir.exists() && systemModuleDemoDir.isDirectory()) {

            // it appears that a directory needs to exist before it can be added
            // to the classpath
            if(!userModuleWorkflowDir.exists() && !userModuleWorkflowDir.mkdirs()) {
                PrintError.message("Could not create directory " + userModuleWorkflowDir);
            }
            classpathPart.createPathElement().setLocation(userModuleWorkflowDir);
        }
        return classpathPart;
    }
}
