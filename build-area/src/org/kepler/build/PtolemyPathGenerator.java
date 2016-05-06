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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.types.FileSet;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.project.ProjectLocator;

/**
 * Created by Chad Berkley
 * Date: July 15, 2009
 * Build classpaths and buildpaths for the ptolemy module
 */
public class PtolemyPathGenerator
{
    private static PtolemyPathGenerator ppg = null;
    private Module ptolemyModule;
    private Vector paths;

    /**
     * Private constructor. Use getInstance() to get an instance of this class
     */
    private PtolemyPathGenerator()
    {
        paths = new Vector();
        Iterator<Module> modulesIt = ModuleTree.instance().iterator();
        while (modulesIt.hasNext())
        {
            Module m = modulesIt.next();
            if (m.getName().equals(Module.PTOLEMY) ||
            		m.getName().matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+(\\.\\d+)?") ||
                    m.getName().matches(Module.PTOLEMY+"-\\d+\\.\\d+(\\.\\d+)?"))
            {
                ptolemyModule = m;
            }
        }
        buildPaths();
    }

    /**
     * singleton accessor.
     */
    public static PtolemyPathGenerator getInstance()
    {
        if (ppg == null)
        {
            ppg = new PtolemyPathGenerator();
        }
        return ppg;
    }

    //we can return the paths in various ways.  As a single path object, as a path
    //array, however we need to get it.

    /**
     * return a fileset with the ptolemy paths
     */
    public FileSet getFileset()
    {
        FileSet fs = new FileSet();
        fs.setProject(ProjectLocator.getAntProject());
        fs.setDir(ProjectLocator.getAntProject().getBaseDir());
        //System.out.println("fs dir is " + ProjectLocator.getAntProject().getBaseDir());
        //add the paths
        fs.setIncludes(createIncludesList());
        //System.out.println("ptolemy fileset: " + fs.toString());
        return fs;
    }

    /**
     * build the path objects for all ptolemy classpath objects
     */
    private void buildPaths()
    {
        File f = new File(ptolemyModule.getSrc(), ".classpath.default");
        System.out.println("Getting Ptolemy paths from " + f.getAbsolutePath());
        try
        {
            String pathContents = parseDotClasspath(f);
            StringTokenizer st = new StringTokenizer(pathContents, "\n");
            while (st.hasMoreTokens())
            {
                String path = st.nextToken();
                paths.addElement(ptolemyModule.getName() + "/" + path);
            }
        }
        catch (FileNotFoundException fnfe)
        {
            System.out.println("ERROR: could not find the ptolemy paths.txt file at "
                    + f.getAbsolutePath()
                    + ".  No ptolemy classpath elements have been added.");
        }
        catch (IOException ioe)
        {
            System.out.println("ERROR: could not read the ptolemy paths.txt file at "
                    + f.getAbsolutePath()
                    + ".  No ptolemy classpath elements have been added.");
        }
    }

    /**
     * parse the .classpath.default file in ptolemy to get the paths
     */
    private String parseDotClasspath(File classpathFile)
            throws FileNotFoundException, IOException
    {
        StringBuffer sb = new StringBuffer();
        FileReader fr = null;
        try {
            fr = new FileReader(classpathFile);
            char[] c = new char[1024];
            int numread = fr.read(c, 0, 1024);
            while (numread != -1)
            {
                sb.append(c, 0, numread);
                numread = fr.read(c, 0, 1024);
            }
        } finally {
            if(fr != null) {
                fr.close();
            }
        }

        StringBuffer classpathTxt = new StringBuffer();
        String classpathStr = sb.toString();
        String searchStr = "<classpathentry kind=\"lib\" path=\"";
        int startIndex = 0;
        ;
        int endIndex = 0;
        int currentIndex = classpathStr.indexOf(searchStr, startIndex);
        while (currentIndex != -1)
        {
            endIndex = classpathStr.indexOf("\"/>", currentIndex);
            String path = classpathStr.substring(currentIndex + searchStr.length(),
                    endIndex);
            classpathTxt.append(path + "\n");
            startIndex = endIndex;
            currentIndex = classpathStr.indexOf(searchStr, startIndex);
        }

        return classpathTxt.toString();
    }

    /**
     * create a string list of the includes
     */
    private String createIncludesList()
    {
        String includes = new String();
        for (int i = 0; i < paths.size(); i++)
        {
            includes += (String) paths.elementAt(i);
            if (i != paths.size() - 1)
            {
                includes += ", ";
            }
        }
        //System.out.println("includes: " + includes);
        return includes;
    }
}
