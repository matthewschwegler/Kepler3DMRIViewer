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


import java.io.File;

import org.apache.ivy.ant.IvyConfigure;
import org.apache.ivy.ant.IvyRetrieve;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;

public class Ivy extends ModulesTask
{
    @Override
    public void run() throws Exception
    {
        System.out.println("Checking for Ivy dependencies.");
        for( Module module : moduleTree )
        {
            File ivyXml = new File(module.getModuleInfoDir(), "ivy.xml");
            File ivySettingsXml = new File(module.getModuleInfoDir(), "ivysettings.xml");
            if( ivyXml.exists() && ivySettingsXml.exists() )
            {
                System.out.println("Retrieving Ivy dependencies for " + module.getName() + "...");
                
                IvyConfigure ivyConfigure = new IvyConfigure();
                ivyConfigure.bindToOwner(this);
                ivyConfigure.setFile(ivySettingsXml);
                ivyConfigure.execute();
    
                IvyRetrieve ivyRetrieve = new IvyRetrieve();
                ivyRetrieve.bindToOwner(this);
                ivyRetrieve.setFile(ivyXml);
                // download the dependencies into lib/jar/ivy/
                String pattern = module.getLibDir().getAbsolutePath() +
                        File.separator +
                        "jar" +
                        File.separator +
                        "ivy" +
                        File.separator +
                        "[artifact]-[revision].[ext]";
                ivyRetrieve.setPattern(pattern);
                ivyRetrieve.execute();
            }
        }


    }
}
