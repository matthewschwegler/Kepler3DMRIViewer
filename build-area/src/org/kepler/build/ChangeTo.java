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

import org.kepler.build.modules.CurrentSuiteTxt;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.modules.Suite;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.RepositoryLocations;

/**
 * Class to change to a different suite of modules
 * Created by David Welker.
 * Date: Oct 9, 2008
 * Time: 4:09:01 PM
 */
public class ChangeTo extends ModulesTask
{
    protected String suiteName;
    protected String under = "undefined";
    protected String ptBranch = null;

    /**
     * set the suite to change to
     *
     * @param suiteName
     */
    public void setSuite(String suiteName)
    {
        this.suiteName = suiteName;
    }

    /**
     * set a suite to to change to "under" another in the modules.txt.  This
     * allows for ad hoc suite dependencies.
     *
     * @param under
     */
    public void setUnder(String under)
    {
        this.under = under;
    }

    /**
     * set the branch to use
     *
     * @param branch
     */
    public void setBranch(String branch)
    {
        RepositoryLocations.setDefaultRepository(branch);
    }

    /**
     * set the ptolemy branch to use
     *
     * @param ptBranch
     */
    public void setPtolemyBranch(String ptBranch)
    {
        this.ptBranch = ptBranch;
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        //Sanity Check: -Dsuite=<suite.name> must be set.
        if (suiteName.equals("undefined"))
        {
            PrintError.suiteNotDefined();
            // FIXME: we should list more possible modules
            System.out.println("  For example: \"ant change-to -Dsuite=kepler\" or \"ant change-to -Dsuite=kepler-1.0\"");
            return;
        }

        //Get any modules in the suite that are not already present.
        Get get = new Get();
        get.bindToOwner(this);
        get.init();
        get.setSuite(suiteName);
        get.setModule("undefined");
        if (ptBranch != null && !ptBranch.startsWith("${"))
        {
            get.setPtolemyBranch(ptBranch);
        }
        get.execute();

        if (!under.equals("undefined"))
        {
            Get getUnder = new Get();
            getUnder.bindToOwner(this);
            getUnder.init();
            getUnder.setSuite("undefined");
            getUnder.setModule(under);
            getUnder.execute();
        }

        Suite suite = Suite.make(suiteName);
        ModulesTxt suiteModulesTxt = suite.getModulesTxt();
        if (!suiteModulesTxt.exists())
        {
            PrintError.notASuite(suite);
            return;
        }
        suiteModulesTxt.read();
        if (!under.equals("undefined"))
        {
            suiteModulesTxt.clear();
            suiteModulesTxt.add(under);
            suiteModulesTxt.add("*" + suiteName);
        }
        System.out.println();
        System.out.println("Changing the value of modules.txt");
        suiteModulesTxt.write(modulesTxt);
        if (under.equals("undefined") )
        {
            CurrentSuiteTxt.setName(suiteName);
        }
        else
        {
            CurrentSuiteTxt.setName("unknown");
        }
    }
}
