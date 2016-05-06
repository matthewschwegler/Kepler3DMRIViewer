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
import java.util.ArrayList;
import java.util.List;

import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;

/**
 * Created by David Welker.
 * Date: Sep 8, 2010
 * Time: 10:06:26 PM
 */
public class RollbackTxt
{
    /**
     * Saves the value of rollback.txt with all the modules from the ModuleTree
     */
    public static void save() throws IOException
    {
        // save modules.txt
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ? ProjectLocator.getUserBuildDir() : ProjectLocator.getBuildDir();
        File rollbackTxt = new File(buildDir, MODULES_ROLLBACK);
        PrintWriter pw = null;
        try
        {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(rollbackTxt)));
            for (Module m : ModuleTree.instance())
            {
                pw.println(m);
            }
        } finally {
            if(pw != null) {
                pw.close();
            }
        }
            
        // save current-suite.txt
        File rollbackSuiteTxt = new File(buildDir, SUITE_ROLLBACK);
        String currentSuiteName = CurrentSuiteTxt.getName();
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(rollbackSuiteTxt);
            fileWriter.write(currentSuiteName);
        } finally {
            if(fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    /**
     * Renames rollback.txt to modules.txt. After this, rollback.txt will not exist.
     */
    public static void load()
    {
        // restore modules.txt
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ? ProjectLocator.getUserBuildDir() : ProjectLocator.getBuildDir();
        File rollbackTxt = new File(buildDir, MODULES_ROLLBACK);
        if(!rollbackTxt.renameTo(ModulesTxt.instance())) {
            PrintError.message("Unable to rename " + rollbackTxt + " to " + ModulesTxt.instance());
        }
        
        // restore current-suite.txt
        File currentSuiteRollbackTxt = new File(buildDir, SUITE_ROLLBACK);
        File currentSuiteFile = new File(buildDir, CurrentSuiteTxt.CURRENT_SUITE_FILE_NAME);
        if(!currentSuiteRollbackTxt.renameTo(currentSuiteFile)) {
            PrintError.message("Unable to rename " + currentSuiteRollbackTxt + " to " +
                    currentSuiteFile);
        }
    }

    /** Get the list of module names from the module rollback file. */
    public static List<String> read()
    {
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ? ProjectLocator.getUserBuildDir() : ProjectLocator.getBuildDir();
        File rollbackTxt = new File(buildDir, MODULES_ROLLBACK);

        List<String> modules = new ArrayList<String>();
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(rollbackTxt));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        String module = null;
        try
        {
            while ((module = br.readLine()) != null)
                modules.add(module);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return modules;
    }

    /**
     * Returns whether rollback.txt exists, which indicates whether a rollback is possible.
     */
    public static boolean exists()
    {
        File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ? ProjectLocator.getUserBuildDir() : ProjectLocator.getBuildDir();
        File rollbackTxt = new File(buildDir, MODULES_ROLLBACK);
        return rollbackTxt.exists();
    }
    
    private static final String MODULES_ROLLBACK = "rollback.txt";
    private static final String SUITE_ROLLBACK = "rollback-current-suite.txt";
    
}
