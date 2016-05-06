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
package org.kepler.build;

import java.util.List;

import org.kepler.build.modules.ModulesTask;
import org.kepler.build.modules.SearchUrlsTxt;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.PropertyDefaults;

public class RemoveSearchUrl extends ModulesTask
{
    private String number;

    public void setNumber(String number)
    {
        this.number = number;
    }

    @Override
    public void run() throws Exception
    {
        if( number.equals(PropertyDefaults.getDefaultValue("number")) )
        {
            PrintError.message("You must specify a url number to remove, e.g. -Dnumber=<number>");
            return;
        }

        int index = -1;

        try
        {
            index = Integer.parseInt(number);
        }
        catch( NumberFormatException e)
        {
            PrintError.message(number + " is not a positive integer.");
            return;
        }

        List<String> urls = SearchUrlsTxt.instance().getUrls();

        if( index < 1 || index > urls.size() )
        {
            PrintError.message("The number you choose must between 1 and " + urls.size() );
            System.out.println("The urls you can delete are as follows:");
            int i = 0;
            for( String url : urls )
            {
                i++;
                System.out.println(i + ": " + url);
            }
            return;
        }

        index--;
        String urlToRemove = SearchUrlsTxt.instance().getUrl(index);
        SearchUrlsTxt.instance().remove(index);

        System.out.println("You have successfully removed \"" + urlToRemove + "\" from this list of urls to search.");
        System.out.println("The remaining urls are:");
        int i = 0;
        for( String url : urls )
        {
            i++;
            System.out.println(i + ": " + url);
        }
    }
}
