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

package org.kepler.build.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * class to read an https svn stream
 * <p/>
 * Created on Mar 27, 2009
 *
 * @author David Welker
 */
public class HttpsSvnReader
{
    /**
     * read a dir withing location
     *
     * @param location
     * @return
     */
    public static ArrayList<String> read(String location)
    {
        ArrayList<String> dirs = new ArrayList<String>();
        List<String> lines = readURL(location);
        if (lines == null)
        {
            return null;
        }
        for (String line : lines)
        {
            if (line.trim().startsWith("<dir name="))
            {
                dirs.add(line.split("\"")[1]);
            }
        }
        return dirs;
    }

    /**
     * Reads both directories and files within the location.
     *
     * @param location
     * @return
     */
    public static List<String> readAll(String location)
    {
        List<String> files = new ArrayList<String>();
        List<String> lines = readURL(location);
        if (lines == null)
        {
            return null;
        }
        for (String line : lines)
        {
            if (line.trim().startsWith("<dir name=")
                    || line.trim().startsWith("<file name="))
            {
                files.add(line.split("\"")[1]);
            }
        }
        return files;
    }

    /**
     * read a url
     *
     * @param location
     * @return
     */
    public static List<String> readURL(String location)
    {
        List<String> lines = null;
        try
        {
            lines = readURLHelper(location);
        }
        catch (Exception ex)
        {
        }

        return lines;
    }

    /**
     * read a location
     *
     * @param location
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private static List<String> readURLHelper(String location)
            throws MalformedURLException, IOException
    {
        List<String> lines = new ArrayList<String>();
        URL url = new URL(location);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url
                .openStream()));
            String line = null;
            while ((line = br.readLine()) != null)
            {
                lines.add(line);
            }
            return lines;
        } finally {
            if(br != null) {
                br.close();
            }
        }
    }
}
