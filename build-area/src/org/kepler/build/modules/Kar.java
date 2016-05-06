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

import java.io.File;

/**
 * class to represent a kar file
 * Created by David Welker.
 * Date: Aug 21, 2008
 * Time: 6:21:06 PM
 */
public class Kar
{
    private String name;
    private File dir;
    private Module module;
    private File manifest;

    /**
     * constructor
     *
     * @param dir
     * @param manifest
     * @param module
     */
    public Kar(File dir, File manifest, Module module)
    {
        this.dir = dir;
        this.manifest = manifest;
        this.module = module;
        name = dir.getName();
    }

    /**
     * get the name
     *
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * get the dir to create it in
     *
     * @return
     */
    public File getDir()
    {
        return dir;
    }

    /**
     * get the module to create the kar for
     *
     * @return
     */
    public Module getModule()
    {
        return module;
    }

    /**
     * get the manifest
     *
     * @return
     */
    public File getManifest()
    {
        return manifest;
    }

    /**
     * returns true if 'this' == o
     */
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        Kar kar = (Kar) o;

        if (name != null ? !name.equals(kar.name) : kar.name != null)
        {
            return false;
        }

        return true;
    }

    /**
     * return a hash code for the manifest
     */
    public int hashCode()
    {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (dir != null ? dir.hashCode() : 0);
        result = 31 * result + (manifest != null ? manifest.hashCode() : 0);
        return result;
    }
}
