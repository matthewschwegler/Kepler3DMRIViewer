/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-03-27 11:51:37 -0700 (Wed, 27 Mar 2013) $'
 * '$Revision: 31772 $'
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

package org.kepler.modulemanager.gui.patch;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.modulemanager.RepositoryLocations;

/**
 * Created by David Welker.
 * Date: Oct 26, 2009
 * Time: 1:59:02 AM
 */
public class PatchChecker
{
    public static void check(boolean batchMode)
    {
        check(false, batchMode);
    }

    //If no patches are found, return false.
    public static boolean check(boolean ignoreConfiguration, boolean batchMode)
    {
        if( !ignoreConfiguration )
        {
            ConfigurationProperty mm = ConfigurationManager.getInstance().getProperty(ConfigurationManager.getModule("module-manager"));
            ConfigurationProperty checkForPatches = mm.getProperty("check-for-patches");
            boolean shouldCheckForPatches = checkForPatches.getValue().trim().equals("true") ? true : false;
            if( !shouldCheckForPatches )
            {
                System.out.println("Not checking for patches.");
                return false;
            }
        }
        try
        {
            URL url = new URL( RepositoryLocations.getReleaseLocation() );
            if( url.openConnection().getContentType() == null )
    	 	    return false;
        }
        catch( IOException e )
        {
            System.out.println("Internet connection is down. Not checking for patches.");
            return false;
        }

        System.out.println("Checking for patches...");

        List<String> released = Module.readReleased();

        final List<ModulePair> upgradeList = new ArrayList<ModulePair>();
        StringBuffer patchCandidateList = new StringBuffer();
        for( Module m : ModuleTree.instance() )
        {
            if( m.hasUpArrow() )
            {
                String mostRecentPatch = m.transformNameWithList(m.getName(), released);
                Module patchCandidate = Module.make(mostRecentPatch);
                if( patchCandidate.getPatch() > m.getPatch() ) {
                    upgradeList.add(new ModulePair(m, patchCandidate));
                    patchCandidateList.append(patchCandidate.getName());
                    patchCandidateList.append(", ");
                }
            }
        }

        if( upgradeList.isEmpty() )
            return false;
        else if (batchMode) {
        	System.out.println("There are available patches:\n" +
        	        patchCandidateList.substring(0, patchCandidateList.lastIndexOf(", ")) + "\n" + 
        			"To apply the patches, start the Kepler GUI or Module Manager.");
        	return true;
        }
        	

        JDialog dialog = new JDialog();
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.add(new UpgradeDialogPanel(upgradeList));
        dialog.pack();
        dialog.setSize(800,500);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return true;

    }

    public static void main(String[] args)
    {
        ModuleTree.init();
        check(false);
    }

}
