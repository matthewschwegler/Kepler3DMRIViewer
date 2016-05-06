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
 * class to tag a module
 *
 * @author berkley
 */
public class Tag extends ModulesTask
{
    protected String module;
    protected String name;

    /**
     * set the module to tag
     *
     * @param module
     */
    public void setModule(String module)
    {
        this.module = module;
    }

    /**
     * set the tag name
     *
     * @param name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        if (module.equals("undefined"))
        {
            PrintError.moduleNotDefined();
            return;
        }

        String[] infoCommand = {"svn", "info", RepositoryLocations.MODULES};
        Process p = Runtime.getRuntime().exec(infoCommand);

        BufferedReader br = new BufferedReader(new InputStreamReader(p
                .getInputStream()));
        String line = null;

        int revision = 0;
        while ((line = br.readLine()) != null)
        {
            if (line.startsWith("Revision:"))
            {
                revision = Integer.parseInt((line.split("\\s+")[1])) + 1;
                break;
            }
        }

        String tagName = !name.equals("undefined") ? module + "-" + name + "-"
                + revision + "-tag" : module + "-" + revision + "-tag";

        String[] tagCommand = {"svn", "copy",
                RepositoryLocations.MODULES + "/" + module,
                RepositoryLocations.REPO + "/tags/" + tagName, "-m",
                "\"Tagging " + module + " as " + tagName + "\""};
        CommandLine.exec(tagCommand);

    }
}
