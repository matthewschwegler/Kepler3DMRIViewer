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
package org.kepler.build.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.kepler.build.modules.ModuleLocationRegistryTxt;
import org.kepler.build.modules.ModuleUtil;

/**
 * class to handle repository locations
 *
 * @author berkley
 */
public class RepositoryLocations
{
    // Repository Locations

    // Use https:, not http: for the Kepler repository base so as to
    // make it easier for people behind firewalls to access the Kepler
    // tree.
    // See http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5502
    public final static String REPO = "https://code.kepler-project.org/code/kepler";
    public final static String BRANCHES = REPO + "/releases/release-branches";
    public static String RELEASED = REPO + "/releases/released";
    public final static String TEST_RELEASES = REPO + "/releases/test-releases";
    public static String MODULES = REPO + "/trunk/modules";

    public static HashMap<String, String> moduleRegistry = null;

    public static void setReleaseLocation(String location)
    {
        RELEASED = location;
    }

    private static HashMap<String, String> readModuleRegistry()
    {
        try
        {
            return readModuleRegistryHelper();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static HashMap<String, String> readModuleRegistryHelper() throws IOException
    {
        HashMap<String, String> registry = new HashMap<String, String>();
        File registryTxt = ModuleLocationRegistryTxt.instance();
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(registryTxt));
            String line = null;
            while ((line = br.readLine()) != null)
            {
                String[] parts = line.split("\\s+");
                String module = parts[0];
                String location = parts[1];
                registry.put(module, location);
            }
            return registry;
        }
        finally
        {
            if(br != null)
            {
                br.close();
            }
        }
    }

    /**
     * get the location
     * name is the name of the module to locate.
     *
     * @param name
     * @return
     */
    public static String getLocation(String name)
    {
        if (moduleRegistry == null)
            moduleRegistry = readModuleRegistry();

        if (moduleRegistry.containsKey(name))
        {
            return moduleRegistry.get(name) + "/" + name;
        }
        if (name.contains("-tag"))
        {
            return REPO + "/tags/" + name;
        }
        if (name.endsWith("branch"))
        {
            return REPO + "/branches/" + name;
        }
        else if (ModuleUtil.isReleasedOrBranchedName(name))
        {
            return getReleasedOrBranchedLocation(name);
        }
        return MODULES + "/" + name;
    }

    /**
     * get the publised or branch location for the module with given name
     *
     * @param name
     * @return
     */
    public static String getReleasedOrBranchedLocation(String name)
    {
        if (name.matches("[a-zA-Z-]+-\\d+\\.\\d+"))
        {
            return BRANCHES + "/" + name;
        }
        return RELEASED + "/" + name;
    }

    /**
     * set the default repository
     *
     * @param repository
     */
    public static void setDefaultRepository(String repository)
    {
        if (repository.equals("null"))
        {
            return;
        }
        MODULES = repository + "/modules";
    }
}
