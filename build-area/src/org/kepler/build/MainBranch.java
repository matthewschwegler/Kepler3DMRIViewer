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

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.RepositoryLocations;
import org.kepler.build.util.CommandLine;

/**
 * Task to deal with branches in the svn tree
 *
 * @author welker
 */
public class MainBranch extends ModulesTask
{

    protected String moduleName;
    protected String name;
    private boolean useRevision = true;

    /**
     * set the module to branch
     *
     * @param module
     */
    public void setModule(String module)
    {
        this.moduleName = module;
    }

    /**
     * set the name
     *
     * @param name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    public void useRevision(boolean useRevision)
    {
        this.useRevision = useRevision;
    }


    /**
     * Run the task
     */
    @Override
    public void run() throws Exception
    {
        if (moduleName.equals("undefined"))
        {
            PrintError.moduleNotDefined();
            return;
        }

        String[] infoCommand = {"svn", "info", RepositoryLocations.MODULES};
        Process p = Runtime.getRuntime().exec(infoCommand);

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;

        int rev = 0;
        if (useRevision)
        {
            while ((line = br.readLine()) != null)
            {
                if (line.startsWith("Revision:"))
                {
                    rev = Integer.parseInt((line.split("\\s+")[1])) + 1;
                    break;
                }
            }
        }
        String revisionBranch = useRevision ? rev + "-branch" : "branch";

        String branchName = !name.equals("undefined")
                ? moduleName + "-" + name + "-" + revisionBranch
                : moduleName + "-" + revisionBranch;

        String[] branchCommand = {"svn", "copy",
                RepositoryLocations.MODULES + "/" + moduleName,
                RepositoryLocations.REPO + "/branches/" + branchName, "-m",
                "\"Branching " + moduleName + " as " + branchName + "\""};
        CommandLine.exec(branchCommand);
    }

}
