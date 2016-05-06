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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.tools.ant.taskdefs.Copy;
import org.kepler.build.project.ProjectLocator;

/**
 * Created by David Welker.
 * Date: Aug 19, 2010
 * Time: 9:22:55 PM
 */
public class CurrentSuiteTxt
{
    private static String currentSuiteName;

    public static void init()
    {
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ? ProjectLocator.getUserBuildDir() : ProjectLocator.getBuildDir();
        File currentSuiteTxt = new File(buildDir, CURRENT_SUITE_FILE_NAME);
        if (!buildDir.isDirectory())
        {
                buildDir.mkdirs();
        }
        File localCurrentSuiteTxt = new File(ProjectLocator.getBuildDir(), CURRENT_SUITE_FILE_NAME);
        if (!currentSuiteTxt.exists() && localCurrentSuiteTxt.exists())
        {
            Copy copy = new Copy();
            copy.setProject(ProjectLocator.getAntProject());
            copy.init();
            copy.setOverwrite(true);
            copy.setFile(localCurrentSuiteTxt);
            copy.setTofile(currentSuiteTxt);
            copy.execute();
        }

        if( !currentSuiteTxt.exists() )
        {
            currentSuiteName = "unknown";
            return;
        }


        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(currentSuiteTxt));
        }
        catch (FileNotFoundException e)
        {
            return;
        }

        try
        {
            currentSuiteName = br.readLine().trim();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        try
        {
            br.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static String getName()
    {
        if (currentSuiteName == null)
        {
            init();
        }
        return currentSuiteName != null ? currentSuiteName : "unknown";
    }

    public static void setName(String name)
    {
        currentSuiteName = name;
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ? ProjectLocator.getUserBuildDir() : ProjectLocator.getBuildDir();
        File currentSuiteTxt = new File(buildDir, CURRENT_SUITE_FILE_NAME);
        PrintWriter pw = null;
        try
        {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(currentSuiteTxt)));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        pw.println(name);
        pw.close();
    }

    public static void delete()
    {
        currentSuiteName = null;
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ? ProjectLocator.getUserBuildDir() : ProjectLocator.getBuildDir();
        File currentSuiteTxt = new File(buildDir, CURRENT_SUITE_FILE_NAME);
        currentSuiteTxt.delete();
    }

    /** The name of the file containing the current suite. */
    public final static String CURRENT_SUITE_FILE_NAME = "current-suite.txt";

}
