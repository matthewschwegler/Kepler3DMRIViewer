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

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.ProjectLocator;
import org.kepler.build.project.RepositoryLocations;

/**
 * get all modules in a suite
 *
 * @author welker
 */
public class GetAllModules extends ModulesTask
{
    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        Process p = Runtime.getRuntime().exec("svn ls " + RepositoryLocations.MODULES);
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;
        while ((line = br.readLine()) != null)
        {
            String module = line.substring(0, line.length() - 1);
            Get get = new Get();
            get.bindToOwner(this);
            get.init();
            get.setModule(module);
            get.execute();
        }

        for (Module m : ProjectLocator.getProjectDir())
        {
            if (!m.isSuite())
                continue;
            Get get = new Get();
            get.bindToOwner(this);
            get.init();
            get.setSuite(m.getName());
            get.setInnerGet(true);
            get.execute();
        }

    }

}
