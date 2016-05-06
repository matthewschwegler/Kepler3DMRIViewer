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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.FileSet;
import org.kepler.build.modules.Kar;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;
import org.kepler.util.DotKeplerManager;

/**
 * class to make the kars in kepler Created by David Welker. Date: Aug 21, 2008
 * Time: 5:00:44 PM
 */
public class MakeKars extends ModulesTask
{

    private Set<Kar> kars = new HashSet<Kar>();
    String module = "undefined";

    /**
     * A list of resources/kar directory names for making kars.
     */
    Vector<String> devKarList = new Vector<String>();

    /**
     * set the module
     *
     * @param module
     */
    public void setModule(String module)
    {
        this.module = module;
    }

    /**
     * run the task
     *
     * @Override ModulesTask.run()
     */
    public void run() throws Exception
    {
        // If the module is not undefined, then no kars should be made.
        if (module.equals("undefined"))
        {
            karSuite();
        }
        else
        {
            System.out.println("Not making any kars");
        }
    }

    /**
     * create all kars defined in a suite
     */
    public void karSuite()
    {
        File devKars = new File(getProject().getBaseDir(), "devKars");
        if (devKars.exists())
        {
            System.out.println("Building Development Kars...");
            readDevKars(devKars);
            for (Module module : moduleTree)
            {
                constructDevelopmentKarsForModule(module);
            }
        }
        else
        {
            System.out.println("Building Kars...");
            for (Module module : moduleTree)
            {
                constructKarsForModule(module);
            }
        }
        makeKars();
    }

    /**
     * Read in a list of kar directories that should get made on startup. One
     * kar directory name per line.
     * <p>
     * Example File:<br/>
     * <br/>
     * Display<br/>
     * Constant<br/>
     * SDFDirector<br/>
     * RExpression<br/>
     * </p>
     *
     * @param devKars
     */
    private void readDevKars(File devKars)
    {
        BufferedReader in = null;
        try
        {
            in = new BufferedReader(new FileReader(devKars));
            String line = new String();
            while ((line = in.readLine()) != null)
            {
                line = line.trim();
                if (line != "")
                {
                    devKarList.add(line);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(in != null)
            {
                try {
                    in.close();
                } catch (IOException e) {
                    System.err.println("Error closing " + devKars + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * construct all kars in a module
     */
    public void constructKarsForModule(Module module)
    {
        File dir = module.getKarResourcesDir();
        if (!dir.isDirectory() || !dir.exists())
        {
            return;
        }
        File[] karDirs = dir.listFiles();
        for (File karDir : karDirs)
        {
            addKar(karDir, module);
        }
    }

    /**
     * add a kar
     *
     * @param karDir
     * @param module
     */
    private void addKar(File karDir, Module module)
    {
        if (!karDir.isDirectory())
        {
            return;
        }
        File manifest = new File(karDir, "MANIFEST.MF");
        if (!manifest.exists())
        {
            return;
        }
        Kar kar = new Kar(karDir, manifest, module);
        kars.add(kar);
    }

    /**
     * Call this method if the devKars file exists. It will only MakeKars for
     * directory names in the file.
     *
     * @param module
     */
    public void constructDevelopmentKarsForModule(Module module)
    {
        File dir = module.getKarResourcesDir();
        if (!dir.isDirectory() || !dir.exists())
        {
            return;
        }
        File[] karDirs = dir.listFiles();
        for (File karDir : karDirs)
        {
            if (devKarList.contains(karDir.getName()))
            {
                addKar(karDir, module);
            }
        }
    }

    /**
     * make the kars
     */
    public void makeKars()
    {

        DotKeplerManager dkm = DotKeplerManager.getInstance();
        for (Kar kar : kars)
        {
            FileSet filesToKar = new FileSet();
            filesToKar.setProject(getProject());
            filesToKar.setDir(kar.getDir());

            Module mod = kar.getModule();
            //String modName = mod.getName();
            String modName = mod.getStemName();
            File modPersistentDir = dkm.getPersistentModuleDirectory(modName);
            File destDir = new File(modPersistentDir, "kar");
            destDir.mkdirs();
            File destFile = new File(destDir, kar.getName() + ".kar");

            Jar jar = new Jar();
            jar.bindToOwner(this);
            jar.init();
            jar.setDestFile(destFile);
            jar.setManifest(kar.getManifest());
            jar.addFileset(filesToKar);

            jar.execute();
        }
    }

}
