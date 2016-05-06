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

import java.util.ArrayList;
import java.util.List;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.ProjectLocator;

/**
 * class to report overrides in the current suite
 *
 * @author welker
 */
public class ReportAllOverrides extends ModulesTask
{
    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        int maxNumberOfOverrides = 0;
        List<String> highOverrideSuites = new ArrayList<String>();
        for (Module suite : ProjectLocator.getProjectDir())
        {
            if (!suite.isSuite())
                return;
            System.out.println("============================================================");
            System.out.println("Module: " + suite.getName());
            System.out.println("============================================================");
            ReportOverrides ro = new ReportOverrides();
            ro.bindToOwner(this);
            ro.execute();

            if (maxNumberOfOverrides == ro.numberOfOverrides)
                highOverrideSuites.add(suite.getName());
            if (maxNumberOfOverrides < ro.numberOfOverrides)
            {
                maxNumberOfOverrides = ro.numberOfOverrides;
                highOverrideSuites.clear();
                highOverrideSuites.add(suite.getName());
            }
            System.out.println();
        }
        System.out.println("============================================================");
        System.out.println("                        Summary\n");
        System.out.println("The suite(s) with the most overrides: " + getSuiteListForPrinting(highOverrideSuites));
        System.out.println("Number of overrides: " + maxNumberOfOverrides);
        System.out.println("============================================================");

    }

    /**
     * get the suite list
     *
     * @param suites
     * @return
     */
    protected String getSuiteListForPrinting(List<String> suites)
    {
        String list = suites.get(0);
        for (int i = 1; i < suites.size(); i++)
            list += ", " + suites.get(i);
        return list;
    }


}
