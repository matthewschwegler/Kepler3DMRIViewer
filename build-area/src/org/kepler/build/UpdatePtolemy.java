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

import java.io.File;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleUtil;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.PrintError;
import org.kepler.build.util.CommandLine;

/**
 * task to update ptolemy
 *
 * @author berkley
 */
public class UpdatePtolemy extends ModulesTask
{
    private String revision = "undefined";

    public void setRevision(String revision)
    {
        this.revision = revision;
    }

    @Override
    public void run() throws Exception
    {
        if (!revision.equals("head") && !revision.equals("stable"))
        {
            PrintError.message("You must type either: 'ant update-ptolemy -Drev=head' or 'ant update-ptolemy -Drev=stable'.");
            return;
        }

        if (revision.equals("head"))
        {
            ptolemyHead.createNewFile();
            ptolemyCompiled.delete();
        }
        else
        {
            ptolemyHead.delete();
        }

        Module ptolemy = null;
        for (Module m : moduleTree)
        {
            if (m.getStemName().equals(Module.PTOLEMY))
            {
                ptolemy = m;
                break;
            }
        }

        if (ptolemy == null)
        {
            PrintError.message("There is no ptolemy module in the suite.");
            return;
        }


        if (!ptolemy.getDir().isDirectory())
        {
            PrintError.message(ptolemy.getDir().getAbsolutePath() + " does not exist.");
            return;
        }

        String rev = revision.equals("head") ? "head" : ModuleUtil.readPtolemyRevision(ptolemy);

        File ptolemySrc = ptolemy.getSrc();
        if (ptolemySrc.isDirectory())
        {
            String[] updateCommand = {"svn", "update", "-r", rev, ptolemySrc.getAbsolutePath()};
            CommandLine.exec(updateCommand);
        }
        else
        {
            // If ptolemy/src is missing, it must not have been retrieved correctly.
            // Try to download it again.
            String[] checkoutCommand = new String[]{"svn", "co", "-r", rev, ptolemy.getLocation(), ptolemySrc.getAbsolutePath()};
            CommandLine.exec(checkoutCommand);
        }

    }

}
