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
package org.kepler.build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.util.CommandLine;

/**
 * Created by David Welker.
 * Date: Feb 26, 2010
 * Time: 12:15:56 AM
 */
public class RegisterModuleLocation extends ModulesTask
{
    private String moduleName;
    private String location;

    public void setName(String moduleName)
    {
        this.moduleName = moduleName;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    @Override
    public void run() throws Exception
    {
        if (moduleName.equals("undefined") || location.equals("undefined"))
        {
            PrintError.message("You must define both -Dmodule and -Dlocation...");
            return;
        }


        File registryTxt = new File(ProjectLocator.getBuildDir(), "module-location-registry.txt");
        List<String> registryLines = new ArrayList<String>();
        if( registryTxt.exists() )
        {
            BufferedReader br = new BufferedReader(new FileReader(registryTxt));
            String line = null;
            while ((line = br.readLine()) != null)
            {
                registryLines.add(line);
            }
            br.close();
        }

        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(registryTxt)));
        for (String registryLine : registryLines)
            pw.println(registryLine);
        pw.println(moduleName + "\t" + location);
        pw.close();

        String[] checkIn = {"svn", "ci", registryTxt.getAbsolutePath(),
                "-m [build-system] Registering " + moduleName + " at location: " + location + "..."};
        CommandLine.exec(checkIn);
    }
}
