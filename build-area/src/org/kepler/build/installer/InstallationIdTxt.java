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
package org.kepler.build.installer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.tools.ant.taskdefs.Copy;
import org.kepler.build.modules.CurrentSuiteTxt;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.project.ProjectLocator;

/**
 * Created by David Welker.
 * Date: Nov 21, 2010
 * Time: 5:58:40 PM
 */
public class InstallationIdTxt
{
    public static void check() throws IOException
    {
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ?
                ProjectLocator.getUserBuildDir() :
                ProjectLocator.getBuildDir();
    	
        if (isNewInstall())
        {

            //Copy install-id.txt
            File installationIdTxt = new File(buildDir, "install-id.txt");
            File localInstallationIdTxt = new File(ProjectLocator.getBuildDir(), "install-id.txt");

            Copy copy = new Copy();
            copy.setProject(ProjectLocator.getAntProject());
            copy.init();
            copy.setOverwrite(true);
            copy.setFile(localInstallationIdTxt);
            copy.setTofile(installationIdTxt);
            copy.execute();

            //Copy modules.txt
            File modulesTxt = new File(buildDir, "modules.txt");
            File localModulesTxt = new File(ProjectLocator.getBuildDir(), "modules.txt");

            copy = new Copy();
            copy.setProject(ProjectLocator.getAntProject());
            copy.init();
            copy.setOverwrite(true);
            copy.setFile(localModulesTxt);
            copy.setTofile(modulesTxt);
            copy.execute();
            
            //Copy current-suite.txt
            File currentSuiteTxt = new File(buildDir, CurrentSuiteTxt.CURRENT_SUITE_FILE_NAME);
            File localCurrentSuiteTxt = new File(ProjectLocator.getBuildDir(), CurrentSuiteTxt.CURRENT_SUITE_FILE_NAME);

            copy = new Copy();
            copy.setProject(ProjectLocator.getAntProject());
            copy.init();
            copy.setOverwrite(true);
            copy.setFile(localCurrentSuiteTxt);
            copy.setTofile(currentSuiteTxt);
            copy.execute();
            
            createInstallPath(buildDir);
            
            
            CurrentSuiteTxt.init();

            ModuleTree.init();
            
        } else if (!installPathMatch(buildDir)){

        	createInstallPath(buildDir);
        }
    }

    public static void write(String appversion)
    {
        File installationIdTxt = new File(ProjectLocator.getBuildDir(), "install-id.txt");
        PrintWriter pw = null;
        try
        {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(installationIdTxt)));
            pw.println(appversion.trim());
            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static boolean isNewInstall()
    {
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ?
                ProjectLocator.getUserBuildDir() :
                ProjectLocator.getBuildDir();

        File installationIdTxt = new File(buildDir, "install-id.txt");
        File localInstallationIdTxt = new File(ProjectLocator.getBuildDir(), "install-id.txt");

        return !versionsMatch(installationIdTxt, localInstallationIdTxt);

    }

    private static boolean installPathMatch(File buildDir)
    {
        return read(new File(buildDir, "install-path.txt")).equals(ProjectLocator.getBuildDir().getParentFile().getAbsolutePath());
    }
    
    private static void createInstallPath(File buildDir) throws IOException
    {    
	    //create installPath.txt at KeplerData directory
	    File installPath = new File(buildDir, "install-path.txt");
	    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(installPath)));
	    pw.println(ProjectLocator.getBuildDir().getParentFile().getAbsolutePath());
	    pw.flush();
	    pw.close();
    }
    
    private static boolean versionsMatch(File installationIdTxt, File localInstallationIdTxt)
    {
        return read(installationIdTxt).equals(read(localInstallationIdTxt));
    }

    private static String read(File installationIdTxt)
    {
        if (!installationIdTxt.exists())
        {
            return "";
        }
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(installationIdTxt));
            return br.readLine().trim();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return "";
        }
    }

}
