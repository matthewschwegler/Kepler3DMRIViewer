/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author$'
 * '$Date$'
 * '$Revision$'
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
package org.kepler.build.modules;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.taskdefs.Copy;
import org.kepler.build.project.ProjectLocator;

public class OSRegistryTxt extends File
{
    private static OSRegistryTxt instance;

    private Map<String, ArrayList<String>> registryEntries = new HashMap<String, ArrayList<String>>();

    private static OSRegistryTxt instance()
    {
        if (instance == null)
        {
            File buildDir = ProjectLocator.shouldUtilizeUserKeplerModules() ?
                    ProjectLocator.getUserBuildDir() :
                    ProjectLocator.getBuildDir();
            if (!buildDir.isDirectory())
            {
                buildDir.mkdirs();
            }
            File osRegistryTxt = new File(buildDir, "os-registry.txt");
            File localOSRegistryTxt = new File(ProjectLocator.getBuildDir(), "os-registry.txt");
            if (localOSRegistryTxt.exists() && !osRegistryTxt.exists())
            {
                Copy copy = new Copy();
                copy.setProject(ProjectLocator.getAntProject());
                copy.init();
                copy.setOverwrite(true);
                copy.setFile(localOSRegistryTxt);
                copy.setTofile(osRegistryTxt);
                copy.execute();
            }
            instance = new OSRegistryTxt(osRegistryTxt.getParentFile(), osRegistryTxt.getName());
        }
        return instance;
    }

    private OSRegistryTxt(File parent, String filename)
    {
        super(parent, filename);
        read();
    }

    private void read()
    {
        try
        {
            readHelper();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void readHelper() throws IOException
    {
        registryEntries.clear();
        if (!exists())
        {
            createNewFile();
            return;
        }
        BufferedReader br = new BufferedReader(new FileReader(this));
        String line = null;
        while ((line = br.readLine()) != null)
        {
            String[] parts = line.split("\\s+");
            String module = parts[0];
            ArrayList<String> compatibleOSes = new ArrayList<String>();
            for (int i = 1; i < parts.length; i++)
            {
                compatibleOSes.add(parts[i]);
            }
            registryEntries.put(module, compatibleOSes);
        }
        br.close();
    }

    private void write()
    {
        try
        {
            writeHelper();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void writeHelper() throws IOException
    {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(this)));
        for (String key : registryEntries.keySet())
        {
            String line = key;
            for (String compatibleOS : registryEntries.get(key))
            {
                line += " " + compatibleOS;
            }
            pw.println(line);
        }
        pw.close();
    }

    private void addEntry(Module module)
    {
        File osTxt = module.getOsTxt();
        if (!osTxt.exists())
        {
            return;
        }
        registryEntries.put(module.getStemName(), getCompatibleOSes(osTxt));
        write();
    }

    private ArrayList<String> getCompatibleOSes(File osTxt)
    {
        try
        {
            return getCompatibleOSesHelper(osTxt);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<String> getCompatibleOSesHelper(File osTxt) throws IOException
    {
        ArrayList<String> compatibleOSes = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(osTxt));
        String line = null;
        while ((line = br.readLine()) != null)
        {
            line = line.trim();
            String parts[] = line.split("\\s+");
            for (String compatibleOS : parts)
            {
                compatibleOSes.add(compatibleOS);
            }
        }
        br.close();
        return compatibleOSes;
    }

    public static void add(Module module)
    {
        OSRegistryTxt instance = instance();
        instance.addEntry(module);
    }

    public static boolean isCompatibleWithCurrentOS(Module module)
    {
        OSRegistryTxt instance = instance();
        if (!instance.registryEntries.containsKey(module.getStemName()))
        {
            return true;
        }
        String osName = System.getProperty("os.name").toLowerCase().trim();
        for (String compatibleOS : instance.registryEntries.get(module.getStemName()))
        {
            if (osName.startsWith(compatibleOS))
            {
                return true;
            }
        }
        return false;
    }

}
