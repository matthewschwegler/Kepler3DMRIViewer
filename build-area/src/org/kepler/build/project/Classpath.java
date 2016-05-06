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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

/**
 * A class that represents a classpath.
 *
 * @author welker
 */
public abstract class Classpath extends Path
{
    protected String type = "UNKNOWN";

    /**
     * constructor
     *
     * @param project
     */
    public Classpath(Project project)
    {
        super(project);
        _addDefaultPaths();
    }

    /**
     * constructor
     *
     * @param project
     * @param path
     */
    public Classpath(Project project, String path)
    {
        super(project, path);
        _addDefaultPaths();
    }

    /**
     * print the classpath
     */
    public void print()
    {
        System.out.println(type + " CLASSPATH");
        System.out.println("==========================");
        String[] parts = toString().split(File.pathSeparator);
        for (String part : parts)
        {
            System.out.println(part);
        }
        System.out.println();
    }
    
    /** Get the path to the JavaFX jar.
     *  @returns If the path is found and Java is 1.7, returns the path. Otherwise, null.
     */
    public static String getJavaFXJarPath() {
        // add JavaFX jar if compiling on Java 1.7
        if(System.getProperty("java.version").startsWith("1.7")) {
            File jfxjarFile = new File(System.getProperty("java.home"),
                    "lib" + File.separator + "jfxrt.jar");
            if(jfxjarFile.exists()) {
                return jfxjarFile.getAbsolutePath();
            }
        }
        return null;
    }

    /** Add default paths to the classpath. */
    private void _addDefaultPaths() {

        // add JavaFX jar if compiling on Java 1.7
        if(System.getProperty("java.version").startsWith("1.7")) {
            String jfxJarPath = getJavaFXJarPath();
            if(jfxJarPath == null && !_printedJFXWarning) {
                System.out.println("WARNING: could not find JavaFX jar file. " + 
                        "(Expected location: " + jfxJarPath + ").");
                _printedJFXWarning = true;
            } else {
                Path jfxPath = new Path(ProjectLocator.getAntProject());
                jfxPath.createPathElement().setPath(jfxJarPath);
                append(jfxPath);
            }
        }
    }
    
    
    /** If true, have printed warning about missing java fx jar. */
    private static boolean _printedJFXWarning = false;

}
