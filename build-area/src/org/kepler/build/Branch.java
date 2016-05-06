package org.kepler.build;

import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.PropertyDefaults;
import org.kepler.build.project.RepositoryLocations;
import org.kepler.build.util.CommandLine;

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
public class Branch extends ModulesTask
{
    private String moduleName;
    private String version;

    public void setModule(String moduleName)
    {
        this.moduleName = moduleName;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }


    @Override
    public void run() throws Exception
    {
        if( moduleName.equals( PropertyDefaults.getDefaultValue("module") ))
        {
            PrintError.moduleNotDefined();
            return;
        }
        if( version.equals( PropertyDefaults.getDefaultValue("version") ) )
        {
            PrintError.message("Must specify a version number. e.g. -Dversion=2.5");
            return;
        }
        if( !version.matches("\\d+\\.\\d+") )
        {
            PrintError.message(version + " is an invalid version. The version must be in the form (digit)+.(digit)+. eg. 2.4, 3.8, 1.2, etc...");
            return;
        }

        String branchName = moduleName + "-" + version;

        String from = RepositoryLocations.MODULES + "/" + moduleName;
        String to = RepositoryLocations.BRANCHES + "/" + branchName;

        String message = "\"Branching " + moduleName + " as " + branchName + "\"";
        String[] branchCommand = {"svn", "copy", from, to, "-m", message};
        CommandLine.exec(branchCommand);

    }
}
