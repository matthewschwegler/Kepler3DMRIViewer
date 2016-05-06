/*
 * Copyright (c) 2010-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-07-21 13:32:19 -0700 (Mon, 21 Jul 2014) $'
 * '$Revision: 32849 $'
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

package org.kepler.modulemanager;

import org.kepler.build.modules.Module;
import org.kepler.build.project.PrintError;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

/**
 * Created by David Welker.
 * Date: Apr 16, 2010
 * Time: 1:46:58 AM
 */
public class RepositoryLocations
{
    private static String releaseLocation = null;

    public static String getReleaseLocation()
    {
        if( releaseLocation == null )
        {
            releaseLocation = initReleaseLocation();
        }
        return releaseLocation;
    }

    private static String initReleaseLocation()
    {
        Module mmModule = ConfigurationManager.getModule("module-manager");
        if(mmModule == null) {
            PrintError.message("Did not find module-manager in modules.txt");            
        } else {
            ConfigurationProperty mmProperty = ConfigurationManager.getInstance().getProperty(mmModule);
            if(mmProperty == null) {
                PrintError.message("Did not find module-manager configuration.");            
            } else {
                ConfigurationProperty releaseProperty = mmProperty.getProperty("releaseLocation");
                if(releaseProperty == null) {
                    PrintError.message("Missing property releaseLocation in module-manager configuration.xml");
                } else {
                    return releaseProperty.getValue();
                }
            }
        }
        
        return null;
        //return mmProperty.getProperty("releaseLocation").getValue();
    }

}
