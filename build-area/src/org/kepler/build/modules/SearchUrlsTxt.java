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
import java.util.List;

import org.kepler.build.project.ProjectLocator;

public class SearchUrlsTxt
{
    private static SearchUrlsTxt instance;

    public static SearchUrlsTxt instance()
    {
        if( instance == null )
        {
            instance = new SearchUrlsTxt();
        }
        return instance;
    }

    private File searchUrlsTxt;
    private List<String> urls = new ArrayList<String>();

    private SearchUrlsTxt()
    {
        searchUrlsTxt = new File(ProjectLocator.getBuildDir(), "search-urls.txt");
        read();
    }

    public void add(String url)
    {
        urls.add(url);
        write();
    }

    public void remove(int index)
    {
        urls.remove(index);
        write();
    }

    public String getUrl(int index)
    {
        return urls.get(index);
    }

    public List<String> getUrls()
    {
        return urls;
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
        if( !searchUrlsTxt.exists() )
        {
            return;
        }
        BufferedReader br = new BufferedReader( new FileReader(searchUrlsTxt) );
        String url = null;
        while( (url = br.readLine()) != null )
        {
            //TODO: Check if the line is a valid url.
            url = url.trim();
            if( url.length() > 0 )
            {
                urls.add( url.trim() );
            }
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
        if( !searchUrlsTxt.exists() )
        {
            searchUrlsTxt.createNewFile();
        }
        PrintWriter pw = new PrintWriter( new BufferedWriter( new FileWriter(searchUrlsTxt)));
        for( String url : urls )
        {
            pw.println(url);
        }
        pw.close();
    }

}
