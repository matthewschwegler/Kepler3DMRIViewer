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
import java.io.PrintWriter;
import java.util.List;

import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.RepositoryLocations;
import org.kepler.build.util.HttpsSvnReader;

/**
 * Update the released modules.txt
 * Created by David Welker.
 * Date: Dec 10, 2009
 * Time: 7:14:33 PM
 */
public class UpdateReleasedTxt extends ReleasedTask
{
    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        List<String> released = HttpsSvnReader.read(RepositoryLocations.RELEASED);
        released = trimOlderNames(released);
        File releasedTxt = ProjectLocator.getCacheFile("released.txt");
        releasedTxt.getParentFile().mkdirs();
        releasedTxt.createNewFile();

        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(releasedTxt)));
        for (String line : released)
        {
            pw.println(line);
        }
        pw.close();
    }

}
