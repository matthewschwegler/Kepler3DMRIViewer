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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.resources.FileResource;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;

/**
 * class to represent the path containing shared libraries
 *
 * @author berkley
 */
public class LibPath extends Path
{

    /**
     * Construct a LibPath for all modules.
     */
    public LibPath()
    {
        this(null);
    }

    /**
     * Construct a new LibPath for a specific module.
     *
     * @param module the module. If null, construct a LibPath for all modules.
     */
    public LibPath(Module module)
    {
        super(ProjectLocator.getAntProject());
        StringBuilder parts = new StringBuilder();
        Iterable<Module> moduleList;

        if (module == null)
        {
            moduleList = ModuleTree.instance();
        }
        else
        {
            LinkedList<Module> list = new LinkedList<Module>();
            list.add(module);
            moduleList = list;
        }

        for (Module curModule : moduleList)
        {
            boolean is32bit = false;
            boolean isMac = (_osNameStr.indexOf("Mac") != -1);

            // see should add the 64 bit lib directories
            if (use64BitLibs())
            {
                File lib64Dir = curModule.getLib64Dir();
                if (lib64Dir.exists())
                {
                    _appendSubDirectories(lib64Dir, parts);
                }
            }
            else
            {
                is32bit = true;
            }

            // add 32 bit directories on for Macs regardless of 32/64 bit
            // since 32 and 64 bit libraries can be placed in a single
            // shared library.
            if (is32bit || isMac)
            {
                File libDir = curModule.getLibDir();
                if (libDir.exists())
                {
                    _appendSubDirectories(libDir, parts);
                }
            }
        }

        setPath(parts.toString());
    }

    public static boolean use64BitLibs()
    {
        return _load64BitLibs;
    }

    /**
     * Append a list of directories containing shared libraries for the
     * current operating system.
     *
     * @param path    the directory in which to search
     * @param builder the string to append to
     */
    private static void _appendSubDirectories(File path, StringBuilder builder)
    {
        // set the library extension based on operating system
        String libExtension = "**/*.so";

        if (_osNameStr.indexOf("Windows") != -1)
        {
            libExtension = "**/*.dll";
        }
        else if (_osNameStr.indexOf("Mac") != -1)
        {
            libExtension = "**/*.dylib **/*.jnilib";
        }

        //System.out.println("searching for libs: " + path + File.separator + libExtension);

        Set<String> directories = new HashSet<String>();

        // find all the libraries with a matching extension
        // and add their containing directories into a set.
        FileSet fileSet = new FileSet();
        fileSet.setProject(ProjectLocator.getAntProject());
        fileSet.setDir(path);
        fileSet.setIncludes(libExtension);
        Iterator<?> iterator = fileSet.iterator();
        while (iterator.hasNext())
        {
            FileResource resource = (FileResource) iterator.next();
            directories.add(resource.getFile().getParentFile().getAbsolutePath());
            //System.out.println("found lib " + resource + " dir = " + resource.getFile().getParentFile().getAbsolutePath());
        }

        for (String dir : directories)
        {
            //System.out.println("adding lib dir " + dir);
            builder.append(dir);
            builder.append(File.pathSeparator);
        }
    }

    private final static String _osNameStr = System.getProperty("os.name");
    private final static String _osArchStr = System.getProperty("os.arch");
    private final static boolean _load64BitLibs = (_osArchStr.equals("x86_64") || _osArchStr.equals("amd64"));
}
