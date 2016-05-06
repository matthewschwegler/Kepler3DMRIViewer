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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.Suite;

/**
 * class to represent the project dir
 *
 * @author welker
 */
public class KeplerModulesDir extends File implements Iterable<Module>
{
    /**
     * constructor
     *
     * @param pathname
     */
    public KeplerModulesDir(String pathname)
    {
        super(pathname);
    }

    /**
     * constructor
     *
     * @param uri
     */
    public KeplerModulesDir(URI uri)
    {
        super(uri);
    }

    /**
     * constructor
     *
     * @param parent
     * @param child
     */
    public KeplerModulesDir(String parent, String child)
    {
        super(parent, child);
    }

    /**
     * constructor
     *
     * @param parent
     * @param child
     */
    public KeplerModulesDir(File parent, String child)
    {
        super(parent, child);
    }

    /**
     * constructor
     *
     * @param file
     */
    public KeplerModulesDir(File file)
    {
        super(file.getAbsolutePath());
    }

    private static final long serialVersionUID = 8742117853204981308L;

    private boolean initialized = false;
    private List<Module> modules = new ArrayList<Module>();

    /**
     * init this class
     */
    private void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;
        if (!isDirectory())
        {
            return;
        }
        for (File moduleDir : listFiles())
        {
            if (!moduleDir.isDirectory() || moduleDir.getName().startsWith(".svn") || moduleDir.getName().equals(".settings"))
            {
                continue;
            }
            Module module = isSuite(moduleDir) ? Suite.make(moduleDir.getName()) : Module.make(moduleDir.getName());
            modules.add(module);
        }
    }

    /**
     * return true if the module in moduleDir is a suite
     *
     * @param moduleDir
     * @return
     */
    private boolean isSuite(File moduleDir)
    {
        return (new File(moduleDir, "module-info/modules.txt")).exists();
    }

    /**
     * get an iterator of modules in the proejct dir
     */
    public Iterator<Module> iterator()
    {
        init();
        return modules.iterator();
    }
}
