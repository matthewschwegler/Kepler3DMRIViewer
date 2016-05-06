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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kepler.build.project.ProjectLocator;

/**
 * Update present.txt, a list of modules present on the system.
 * Created by David Welker.
 * Date: Dec 10, 2009
 * Time: 11:14:23 PM
 */
public class UpdatePresentTxt extends ReleasedTask
{
	
	private static String RELEASED_MODULE_NAME_PATTERN = "[a-zA-Z-]+-\\d+\\.\\d+\\.\\d+";

	
    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
		// XXX since ProjectLocator.getProjectDir() isn't necessarily the 
    	// installed application dir, use ProjectLocator.getApplicationDir 
    	// as necessary
        //KeplerModulesDir projectDir = ProjectLocator.getProjectDir();
    	
		File appDir = ProjectLocator.getKeplerModulesDir();
		
		String userKeplerModulesDir = ProjectLocator.getUserKeplerModulesDir().toString();
		if (!userKeplerModulesDir.endsWith(File.separator))
		{
			userKeplerModulesDir = userKeplerModulesDir.concat(File.separator);
		}
		
		//only need to muck with classpath if they're the same
		if (appDir.toString().equals(userKeplerModulesDir))
		{
			appDir = ProjectLocator.getApplicationModulesDir();
			if (appDir == null)
			{
				System.out.println("UpdatePresentTxt ERROR, couldn't find application dir");
				appDir = ProjectLocator.getKeplerModulesDir();
			}
		}
    	
    	//String[] present = projectDir.list(new FilenameFilter()
        String[] present = appDir.list(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.matches(RELEASED_MODULE_NAME_PATTERN);
            }
        });
        List<String> presentList = new ArrayList<String>(Arrays.asList(present));
        
        //Look in KeplerData/kepler.modules/for more modules if use.keplerdata is present.
        if( ProjectLocator.shouldUtilizeUserKeplerModules() )
        {
        	File moreModulesDir = ProjectLocator.getUserKeplerModulesDir();
        	String[] morePresent = moreModulesDir.list(new FilenameFilter()
        	{
            	public boolean accept(File dir, String name)
            	{
                	return name.matches(RELEASED_MODULE_NAME_PATTERN);
            	}
        	});
        	for( String replacementCandidate : morePresent )
        	{
        		presentList.add(replacementCandidate);
        	}
        }
        
        presentList = trimOlderNames(presentList);
        File presentTxt = ProjectLocator.getCacheFile("present.txt");
        presentTxt.getParentFile().mkdirs();
        presentTxt.createNewFile();

        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(presentTxt)));
        for (String line : presentList)
        {
            pw.println(line);
        }
        pw.close();

    }
}
