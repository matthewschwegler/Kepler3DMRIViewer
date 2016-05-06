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

import org.apache.tools.ant.types.FileSet;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;

/**
 * Class to find a class
 * Created by David Welker.
 * Date: Nov 12, 2009
 * Time: 10:13:07 AM
 */
public class Find extends ModulesTask
{
    private String classToFind;

    /**
     * set the class to find
     *
     * @param classToFind
     */
    public void setClass(String classToFind)
    {
        this.classToFind = classToFind;
    }

    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        if (classToFind.equals("undefined"))
        {
            PrintError.message("You must specify a class with the package. i.e. -Dclass=org.kepler.foo.Bar");
            return;
        }

        String includesPattern = getIncludesPattern(classToFind);

        boolean matchFound = false;

        for (Module m : ProjectLocator.getProjectDir())
        {
            if (!m.getSrc().isDirectory())
                continue;
            FileSet fs = new FileSet();
            fs.setProject(getProject());
            fs.setDir(m.getSrc());
            fs.setIncludes(includesPattern);

            String[] includedFiles = fs.getDirectoryScanner().getIncludedFiles();
            if (includedFiles.length != 0)
            {
                if (!matchFound)
                {
                    System.out.println(classToFind + " is found in the following downloaded modules:");
                    matchFound = true;
                }
                System.out.println("  " + m);
            }
        }
        if (!matchFound)
            System.out.println(classToFind + " is not found in any downloaded module.");
    }

    /**
     * get an includes pattern for java classes
     *
     * @param javaClass
     * @return
     */
    private String getIncludesPattern(String javaClass)
    {
        if (javaClass.endsWith(".java"))
            javaClass = javaClass.substring(0, javaClass.length() - ".java".length());
        String[] parts = javaClass.split("\\.");
        String pattern = "";
        for (String part : parts)
        {
            pattern += part + "/";
        }
        pattern = pattern.substring(0, pattern.length() - 1);
        return pattern + ".java";
    }
}
