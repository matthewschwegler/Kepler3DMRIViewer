/*
 * Copyright (c) 2014 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-06-28 09:53:53 -0700 (Sat, 28 Jun 2014) $'
 * '$Revision: 32790 $'
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
import java.io.IOException;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.util.CommandLine;

/** An ant task that calls Maven's dependency:copy-dependencies
 * 
 *  @author Daniel Crawl
 *  @version $Id: Maven.java 32790 2014-06-28 16:53:53Z crawl $
 * 
 */
public class Maven extends ModulesTask
{
    @Override
    public void run() throws Exception
    {
        if(_moduleName != null && !_moduleName.equals("undefined"))
        {
            // NOTE: if we're running change-to to change suites, the module may not
            // be in the current suite, so make the module instead
            Module module = Module.make(_moduleName);
            if(module == null)
            {
                PrintError.message("Error creating module " + _moduleName);
                return;
            }
            _runModule(module);
        }
        else
        {
            System.out.println("Checking for Maven dependencies.");
            for( Module module : moduleTree )
            {
                _runModule(module);
            }
        }
    }
    
    /** Set the module name to update. */
    public void setModule(String moduleName)
    {
        _moduleName = moduleName;
    }

    private void _runModule(Module module) throws IOException
    {
        File pomXML = new File(module.getModuleInfoDir(), "pom.xml");
        if(pomXML.exists()) {
            System.out.println("Retrieving Maven dependencies for " + module.getName() + "...");
                            
            String[] statusCommand = {"mvn",
                "dependency:copy-dependencies"};
            CommandLine.exec(statusCommand, module.getModuleInfoDir());
        }

    }
    
    /** The name of the module to update. */
    private String _moduleName;
    
}
