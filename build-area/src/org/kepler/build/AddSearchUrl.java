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

import org.kepler.build.modules.ModulesTask;
import org.kepler.build.modules.SearchUrlsTxt;
import org.kepler.build.project.PrintError;
import org.kepler.build.project.PropertyDefaults;

public class AddSearchUrl extends ModulesTask
{
    private String url;

    public void setUrl(String url)
    {
        this.url = url;
    }


    @Override
    public void run() throws Exception
    {

        if( url.equals( PropertyDefaults.getDefaultValue("url")) )
        {
            PrintError.message("You need to specify a url, e.g. -Durl=<url>.");
            return;
        }
        System.out.println("Adding url \"" + url + "\" to list of urls to be searched.");
        SearchUrlsTxt.instance().add(url);
    }
}
