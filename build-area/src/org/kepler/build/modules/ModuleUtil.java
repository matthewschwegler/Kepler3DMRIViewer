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
package org.kepler.build.modules;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.kepler.build.project.ProjectLocator;

/**
 * utility methods for modules
 *
 * @author berkley
 */
public class ModuleUtil
{
    /**
     * This method returns true if the name indicated EITHER a branch or released version.
     *
     * @param module
     * @return
     */
    public static boolean hasReleasedName(Module module)
    {
        return isReleasedName(module.getName());
    }

    /**
     * return true if this name is a released or branched module
     *
     * @param moduleName
     * @return
     */
    public static boolean isReleasedOrBranchedName(String moduleName)
    {
        return isReleasedName(moduleName) || isBranchedName(moduleName);
    }

    /**
     * return true if the name is released
     *
     * @param moduleName
     * @return
     */
    public static boolean isReleasedName(String moduleName)
    {
        return moduleName.matches("[a-zA-Z-]+-\\d+\\.\\d+\\.\\d+");
    }

    /**
     * return true if the name is a branch
     *
     * @param moduleName
     * @return
     */
    public static boolean isBranchedName(String moduleName)
    {
        return moduleName.matches("[a-zA-Z-]+-\\d+\\.\\d+");
    }

    /**
     * unzip a file
     *
     * @param zip
     */
    public static void unzip(File zip)
    {
        try
        {
            String destinationDirName = zip.getName().substring(0, zip.getName().length() - ".zip".length());
            File destinationDir = new File(zip.getParentFile(), destinationDirName);
            unzipHelper(zip, destinationDir);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * unzip a file to a dir
     *
     * @param zip
     * @param destinationDir
     */
    public static void unzip(File zip, File destinationDir)
    {
        try
        {
            unzipHelper(zip, destinationDir);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * unzip a file to a dir
     *
     * @param zip
     * @param destinationDir
     * @throws IOException
     */
    private static void unzipHelper(File zip, File destinationDir) throws IOException
    {
        System.out.println("Unzipping " + zip.getAbsolutePath());

        destinationDir.mkdirs();
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null)
        {
            File outputFile = new File(destinationDir, entry.getName());
            if (entry.isDirectory())
            {
                outputFile.mkdirs();
                continue;
            }
            int count = 0;
            final int BUFFER = 2048;
            byte[] data = new byte[2048];
            FileOutputStream fos = new FileOutputStream(outputFile);
            BufferedOutputStream destinationStream = new BufferedOutputStream(fos, BUFFER);
            while ((count = zis.read(data, 0, BUFFER)) != -1)
            {
                destinationStream.write(data, 0, count);
            }
            destinationStream.flush();
            destinationStream.close();
        }
        zis.close();
        System.out.println("Done unzipping");
    }

    /**
     * create a horizontal line
     *
     * @param size
     * @return
     */
    public static String horizontalLine(int size)
    {
        String line = "";
        for (int i = 0; i < size; i++)
        {
            line += "=";
        }
        return line;
    }

    /**
     * get the relative path of a file
     *
     * @param file
     * @return
     */
    public static String getRelativePath(File file)
    {
        String basepath = ProjectLocator.getProjectDir().getAbsolutePath();
        String filepath = file.getAbsolutePath();
        ;
        if (!filepath.startsWith(basepath))
        {
            return filepath;
        }
        return filepath.substring(basepath.length() + 1, filepath.length());
    }

    public static String getCurrentSuiteName()
    {
        if (ModulesTxt.instance() == null)
        {
            return "unknown";
        }
        String currentSuiteTxtName = CurrentSuiteTxt.getName();
        if (currentSuiteTxtName != null)
        {
            return currentSuiteTxtName;
        }
        return "unknown";
    }

    /**
     * get the ptolemy revision for the default ptolemy module. The ptolemyModule
     * parameter is necessary because of the possibility that we are working from
     * a module other thank the trunk where the name would be, for example, ptolemy-8.0
     * instead of ptolemy.
     */
    public static String readPtolemyRevision(Module ptolemyModule)
    {
        try
        {
            return readPtolemyRevisionHelper(ptolemyModule);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return "-1";
        }
    }

    /**
     * read the ptolemy revision
     *
     * @return
     * @throws IOException
     */
    private static String readPtolemyRevisionHelper(Module ptolemyModule) throws IOException
    {

        File ptolemyRevisionFile = new File(ptolemyModule.getModuleInfoDir(), "revision.txt");
        if (!ptolemyRevisionFile.exists())
        {
            return "head";
        }
        BufferedReader br = new BufferedReader(new FileReader(ptolemyRevisionFile));
        String revision = br.readLine().trim();
        br.close();
        return revision;
    }

}
