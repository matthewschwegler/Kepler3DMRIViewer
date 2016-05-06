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

import java.util.Iterator;

import org.kepler.build.project.RepositoryLocations;

/**
 * class that represents a suite
 *
 * @author berkley
 */
public class Suite extends Module implements Iterable<Module>
{
    protected String releasedSuiteLocation;
    protected ModuleTree modulesTree;

    /**
     * factory
     *
     * @param name
     * @param location
     * @return
     */
    public static Suite make(String name, String location)
    {
        return new Suite(name, location);
    }

    /**
     * factory
     *
     * @param name
     * @return
     */
    public static Suite make(String name)
    {
        return new Suite(name);
    }

    /**
     * constructor
     *
     * @param name
     */
    protected Suite(String name)
    {
        super(name);
        isSuite = true;
    }

    /**
     * constructor
     *
     * @param name
     * @param location
     */
    protected Suite(String name, String location)
    {
        super(name, location);
        isSuite = true;
    }

    /**
     * write a string
     */
    public String writeString()
    {
        return writeStringHelper("*" + name);
    }

    /**
     * return an iterator over the modules in this suite.
     */
    public Iterator<Module> iterator()
    {
        return modulesTxt.iterator();
    }

    /**
     * set the modulesTxt for this suite
     *
     * @param modulesTxt
     */
    public void setModulesTxt(ModulesTxt modulesTxt)
    {
        this.modulesTxt = modulesTxt;
    }

    /**
     * get the location of the released suite
     *
     * @return
     */
    public String getReleasedSuiteLocation()
    {
        return RepositoryLocations.getReleasedOrBranchedLocation(name);
    }

    /**
     * return a moduleTree for this suite
     *
     * @return
     */
    public ModuleTree getModulesTree()
    {
        if (modulesTree == null)
        {
            modulesTree = new ModuleTree(modulesTxt);
        }
        return modulesTree;
    }

}
