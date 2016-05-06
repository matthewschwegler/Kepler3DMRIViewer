/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 16:13:10 -0800 (Thu, 21 Feb 2013) $'
 * '$Revision: 31488 $'
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
import java.util.ArrayList;
import java.util.List;

import org.kepler.build.project.PrintError;

/**
 * class to patch a released module or suite
 * Created by David Welker.
 * Date: Sep 14, 2009
 * Time: 4:15:01 PM
 */
public class Patch extends Release
{
    protected List<String> existingReleaseded = new ArrayList<String>();

    /**
     * calculate the existing released suites
     *
     * @throws Exception
     */
    public void calculateExistingReleased() throws Exception
    {
        String[] svnLsCommand = {"svn", "ls", releaseLocation};
        Process p = Runtime.getRuntime().exec(svnLsCommand);
        BufferedReader br = new BufferedReader(new InputStreamReader(p
                .getInputStream()));
        String line = null;
        while ((line = br.readLine()) != null)
        {
            line = line.trim();
            if (line.endsWith("/"))
            {
                existingReleaseded.add(line.substring(0, line.length() - 1));
            }
        }
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        if (moduleName.equals("undefined"))
        {
            PrintError.moduleNotDefined();
            return;
        }
        if (!isBranch)
        {
            PrintError.message("You module you specified is not a branch.");
            return;
        }
        calculateExistingReleased();

        int patch = 0;
        boolean alreadyReleased = false;
        for (String released : existingReleaseded)
        {

            if (released.startsWith(moduleName))
            {
                alreadyReleased = true;
                String[] parts = released.split("\\.");
                String releasedPatch = parts[parts.length - 1];
                int releasedPatchInt = Integer.parseInt(releasedPatch);
                if (patch <= releasedPatchInt)
                {
                    patch = releasedPatchInt + 1;
                }
            }
        }
        if (!alreadyReleased)
        {
            PrintError.message(moduleName
                    + " must be released before you can patch.");
            return;
        }
        release(patch);
    }
}
