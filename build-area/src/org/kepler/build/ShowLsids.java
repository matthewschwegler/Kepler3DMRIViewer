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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.kepler.build.modules.Kar;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModulesTask;

public class ShowLsids extends ModulesTask
{
    boolean deb = false;

    private Vector<String> errors = new Vector<String>();
    private Vector<String> modulesWithKarDirs = new Vector<String>();
    private Vector<Kar> kars = new Vector<Kar>();
    private Hashtable<String, String> lsids = new Hashtable<String, String>(500);
    private Vector<String> skipDirNames = new Vector<String>();

    private void findKars()
    {
        if (deb)
            System.out.println("Finding Kar Directories...");

        /**
         * TODO update this to retrieve the manifests stored in the module kar
         * resource directories on the SVN server, instead of only looking at
         * local modules.
         */
        for (Module module : moduleTree)
        {
            File dir = module.getKarResourcesDir();
            if (!dir.isDirectory() || !dir.exists())
            {
                continue;
            }
            int karDirsFound = 0;
            File[] karDirs = dir.listFiles();
            for (File karDir : karDirs)
            {
                if (deb)
                    System.out.println("    " + karDir);
                String dirName = karDir.getName();
                boolean skip = skipDirNames.contains(dirName);
                if (!karDir.isDirectory() || skip)
                {
                    continue;
                }
                File manifest = new File(karDir, "MANIFEST.MF");
                if (!manifest.exists())
                {
                    String err = "Skipping " + karDir + " NO MANIFEST.MF";
                    errors.add(err);
                    continue;
                }
                Kar kar = new Kar(karDir, manifest, module);
                kars.add(kar);
                karDirsFound++;
            }
            if (karDirsFound > 0)
            {
                modulesWithKarDirs.add(module.getName());
            }
        }
        System.out.println("");
    }

    public void processKarLsids()
    {
        if (deb)
            System.out.println("Processing Kar Directories...");
        for (Kar kar : kars)
        {
            if (deb)
                System.out.println("**** " + kar.getName());

            File manifestFile = kar.getManifest();
            FileInputStream fis;

            try
            {

                fis = new FileInputStream(manifestFile);
                Manifest manifest = new Manifest(fis);

                Set<String> lsidsFoundSoFar = lsids.keySet();

                Attributes mains = manifest.getMainAttributes();
                String karlsid = mains.getValue("lsid");
                if (deb)
                    System.out.println("**** " + karlsid);
                if (karlsid == null)
                {
                    String err = kar.getName() + " has no LSID";
                    errors.add(err);
                }
                else
                {
                    if (lsidsFoundSoFar.contains(karlsid))
                    {
                        String nameOfExisting = lsids.get(karlsid);
                        String err = "DUPLICATE LSID: " + karlsid
                                + " found in " + kar.getName()
                                + " already exists in " + nameOfExisting;
                        errors.add(err);
                    }
                    else
                    {
                        lsids.put(karlsid, kar.getName());
                    }
                }

                Map<String, Attributes> entries = manifest.getEntries();
                for (String entryName : entries.keySet())
                {
                    if (deb)
                        System.out.println("entry: " + entryName);
                    Attributes atts = entries.get(entryName);
                    String lsid = atts.getValue("lsid");
                    if (deb)
                        System.out.println("lsid: " + lsid);
                    if (lsidsFoundSoFar.contains(lsid))
                    {
                        String nameOfExisting = lsids.get(lsid);
                        String err = "DUPLICATE LSID: " + lsid + " found in "
                                + entryName + " already exists in "
                                + nameOfExisting;
                        errors.add(err);
                    }
                    else
                    {
                        lsids.put(lsid, entryName);
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
        }
    }

    private void listLsids()
    {
        Set<String> l = lsids.keySet();
        List<String> lList = new Vector<String>();
        lList.addAll(l);
        Collections.sort(lList);
        for (String lsid : lList)
        {
            String entryName = lsids.get(lsid);
            System.out.println(lsid + " " + entryName);
        }
    }

    private void listModulesWithKarDirs()
    {
        System.out.println(modulesWithKarDirs.size() + " of "
                + moduleTree.getModuleList().size()
                + " modules have Kar Resource Directories: ");
        System.out.print("    ");
        for (String s : modulesWithKarDirs)
        {
            System.out.print(s + " ");
        }
        System.out.println();
        System.out.print("    ");
        for (Module module : moduleTree)
        {
            System.out.print(module.getName() + " ");
        }
        System.out.println();
    }

    private void initSkipDirs()
    {
        skipDirNames.add(".svn");
    }

    public void run() throws Exception
    {
        initSkipDirs();
        findKars();
        processKarLsids();
        listLsids();
        System.out.println();
        listModulesWithKarDirs();
        System.out.println(lsids.size() + " lsids found");
        System.out.println(errors.size() + " errors");
        for (String err : errors)
        {
            System.out.println(err);
        }
    }

}

