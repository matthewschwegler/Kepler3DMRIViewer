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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleUtil;
import org.kepler.build.modules.ModulesTask;
import org.kepler.build.modules.ModulesTxt;
import org.kepler.build.modules.Suite;
import org.kepler.build.project.PrintError;
import org.kepler.build.util.CommandLine;
import org.kepler.build.util.MiscUtil;

/**
 * class to update all modules
 *
 * @author berkley
 */
public class UpdateModules extends ModulesTask
{
    protected SimpleDateFormat svnDateFormat = new SimpleDateFormat("{yyyy-MM-dd'T'HH:mm}");

    protected String revision = "head";
    protected Date date = null;
    protected String dateString = null;
    protected String buildAreaRev = "head";
    protected boolean buildAreaRevDifferent = false;
    protected boolean validDate = true;
    protected boolean updateBuildArea = true;

    protected List<String> exclude = new ArrayList<String>();
    protected List<String> include = new ArrayList<String>();

    /**
     * set the revision
     */
    public void setRevision(String revision)
    {
        this.revision = revision;
    }

    /**
     * set the date
     */
    public void setDate(String date)
    {
        dateString = date;
        if (date.equals("undefined"))
        {
            return;
        }
        try
        {
            setDateHelper(date);
        }
        catch (ParseException e)
        {
            validDate = false;
        }
    }

    /**
     * set the date helpler
     */
    private void setDateHelper(String date) throws ParseException
    {
        this.date = MiscUtil.parseDateTime(date);
    }

    /**
     * set an include string
     */
    public void setInclude(String include)
    {
        if (include.equals("undefined"))
        {
            return;
        }
        String[] includeArray = include.split(",");
        for (String moduleToInclude : includeArray)
        {
            this.include.add(moduleToInclude);
        }
    }

    /**
     * set an exclude string
     */
    public void setExclude(String exclude)
    {
        if (exclude.equals("undefined"))
        {
            return;
        }
        String[] excludeArray = exclude.split(",");
        for (String moduleToExclude : excludeArray)
        {
            this.exclude.add(moduleToExclude);
        }
    }

    /**
     * set a module to skip
     */
    public void setSkip(String skip)
    {
        setExclude(skip);
    }

    /**
     * set a flag to update or not update the build-area
     */
    public void setUpdateBuildArea(String updateBuildArea)
    {
        if (updateBuildArea.trim().equals("false"))
        {
            this.updateBuildArea = false;
        }
    }

    /**
     * set the build area revision to update to
     */
    public void setBuildAreaRev(String rev)
    {
        this.buildAreaRev = rev;
        buildAreaRevDifferent = true;
    }

    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        if (!validDate)
        {
            PrintError.message(dateString + " is not a valid date!");
            return;
        }

        String currentSuiteName = ModuleUtil.getCurrentSuiteName();
        Suite currentSuite = null;
        List<Module> subtractions = new ArrayList<Module>();
        if (!currentSuiteName.equals("unknown"))
        {
            currentSuite = Suite.make(currentSuiteName);
            if (include.size() == 0 || include.contains(currentSuite.getName()))
            {
                if (!exclude.contains(currentSuite.getName()))
                {
                    updateModule(currentSuite);
                }
            }
            ModulesTxt suiteModulesTxt = currentSuite.getModulesTxt();
            suiteModulesTxt.read();

            if (!modulesTxt.equals(suiteModulesTxt))
            {
                List<Module> additions = modulesTxt.getAdditions(suiteModulesTxt);
                subtractions = modulesTxt.getSubtractions(suiteModulesTxt);

                ChangeTo changeTo = new ChangeTo();
                changeTo.bindToOwner(this);
                changeTo.init();
                changeTo.setSuite(currentSuiteName);
                changeTo.execute();
                System.out.println();
                for (Module m : additions)
                {
                    updateModule(m);
                }
            }

        }

        for (Module module : moduleTree)
        {
            if (!module.equals(currentSuite) && !subtractions.contains(module))
            {
                if (include.size() == 0 || include.contains(module.getName()))
                {
                    if (!exclude.contains(module.getName()))
                    {
                        updateModule(module);
                    }
                }
            }
        }

        if( buildAreaRevDifferent )
        {
            updateBuildArea(buildAreaRev);
        }
        else
        {
            updateBuildArea(revision);
        }

        Ivy ivy = new Ivy();
        ivy.bindToOwner(this);
        ivy.init();
        ivy.execute();
        
        Maven maven = new Maven();
        maven.bindToOwner(this);
        maven.init();
        maven.execute();
    }

    /**
     * detect any modifications to the module
     *
     * @param module
     * @return
     */
    public boolean detectModifications(Module module)
    {
        String svnStatus = "svn status " + module.getSrc();
        //System.out.println(svnStatus);
        List<String> modifiedClasses = new ArrayList<String>();
        try
        {
            Process p = Runtime.getRuntime().exec(svnStatus);
            BufferedReader br = new BufferedReader(new InputStreamReader(p
                    .getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                if (line.startsWith("M"))
                {
                    modifiedClasses.add(line.split("\\s+")[1]);
                }
            }
            br.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (modifiedClasses.size() > 0)
        {
            System.out.println("The following classes have conflicts in the "
                    + module + " module:");
        }
        for (String modifiedClass : modifiedClasses)
        {
            System.out.println("  " + modifiedClass);
        }
        return modifiedClasses.size() > 0;
    }

    /**
     * update the module to the given rev
     *
     * @param module
     * @param rev
     * @throws IOException
     */
    public void updateModule(Module module, String rev) throws IOException
    {
        if (module.isReleased())
        {
            return;
        }

        /*if (detectModifications(module))
        {
          System.out.println("WARNING: the " + module
              + " module cannot be updated because of modifications.");
          System.out.println();
          return;
        }*/

        File moduleDir = module.getDir();
        if (moduleDir.isDirectory())
        {
            if (date != null)
            {
                rev = svnDateFormat.format(date);
            }
            System.out.println("Updating " + module + "...");
            if (module.isPtolemy())
            {
                String[] updatePtolemy = {"svn", "-r", "head", "update", "--accept", "postpone", moduleDir.getAbsolutePath()};
                CommandLine.exec(updatePtolemy);
                if (ptolemyHead.exists())
                {
                    rev = "head";
                    System.out.println("Using the head of ptolemy. Use 'ant update-ptolemy -Drev=stable' to work from the stable version.");
                }
                else
                {
                    System.out.println("Using the stable version of ptolemy. Use 'ant update-ptolemy -Drev=head' to work from the head.");
                    rev = ModuleUtil.readPtolemyRevision(module);
                    ptolemyCompiled.delete();
                }
                File ptolemySrc = module.getSrc();
                if (ptolemySrc.isDirectory())
                {
                    String[] updateCommand = {"svn", "-r", rev, "update", "--accept", "postpone", ptolemySrc.getAbsolutePath()};
                    CommandLine.exec(updateCommand);
                }
                else
                {
                    // If ptolemy/src is missing, it must not have been retrieved correctly.
                    // Try to download it again.
                    String[] checkoutCommand = new String[]{"svn", "co", "-r", rev, module.getLocation(), ptolemySrc.getAbsolutePath()};
                    CommandLine.exec(checkoutCommand);
                }
            }
            else
            {
                String[] updateCommand = {"svn", "-r", rev, "update", "--accept", "postpone", moduleDir.getAbsolutePath()};
                CommandLine.exec(updateCommand);
            }
        }
        else
        {
            Get get = new Get();
            get.bindToOwner(this);
            get.init();
            get.setModule(module.getName());
            get.execute();
        }

    }

    /**
     * update a module
     */
    public void updateModule(Module module) throws IOException
    {
        updateModule(module, revision);
    }

    /**
     * update the build area
     */
    public void updateBuildArea(String rev) throws IOException
    {
        updateModule(Module.make("build-area"), rev);
    }
}
