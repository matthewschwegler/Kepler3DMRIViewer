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
package org.kepler.build.ide;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

/**
 * class to merge files
 * Created by David Welker.
 * Date: Aug 25, 2008
 * Time: 10:37:20 AM
 */
public class FileMerger
{
    /**
     * merge a file to a printwriter
     *
     * @param from
     * @param to
     */
    public static void merge(File from, PrintWriter to)
    {
        FileReader reader = null;
        try
        {
            reader = new FileReader(from);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        merge(reader, to);
    }

    /**
     * merge to streams
     *
     * @param from
     * @param to
     */
    public static void merge(InputStream from, PrintWriter to)
    {
        InputStreamReader reader = new InputStreamReader(from);
        merge(reader, to);
    }

    /**
     * mrege a reader and a printwriter
     *
     * @param from
     * @param to
     */
    public static void merge(Reader from, PrintWriter to)
    {
        BufferedReader br = new BufferedReader(from);
        String line = null;
        try
        {
            while ((line = br.readLine()) != null)
            {
                to.println(line);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
    }

    /**
     * merge a dir/file to a printwriter
     *
     * @param fromDir
     * @param fromName
     * @param to
     */
    public static void merge(File fromDir, String fromName, PrintWriter to)
    {
        merge(new File(fromDir, fromName), to);
    }

    /**
     * get a printwriter for a file
     *
     * @param file
     * @return
     */
    public static PrintWriter getPrintWriter(File file)
    {
        file.getParentFile().mkdirs();
        PrintWriter pw = null;
        try
        {
            pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        return pw;
    }

    /**
     * make a file
     *
     * @param fromDir
     * @param fromName
     * @param toDir
     * @param toName
     */
    public static void makeFile(File fromDir, String fromName, File toDir,
                                String toName)
    {
        File to = new File(toDir, toName);
        PrintWriter pw = FileMerger.getPrintWriter(to);
        File from = new File(fromDir, fromName);
        FileMerger.merge(from, pw);
        pw.close();
    }
}
