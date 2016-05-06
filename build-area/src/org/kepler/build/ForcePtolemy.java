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

package org.kepler.build;

import org.kepler.build.modules.ModulesTask;

/**
 * A class to force ptolemy to build
 * Created by David Welker.
 * Date: Jun 22, 2009
 * Time: 2:34:50 PM
 */
public class ForcePtolemy extends ModulesTask
{
    protected String moduleName = "undefined";
    protected String compilerArgs = "undefined";
    protected boolean debug = false;
    protected String include = "undefined";
    protected String exclude = "undefined";

    /**
     * set the module
     *
     * @param moduleName
     */
    public void setModule(String moduleName)
    {
        this.moduleName = moduleName;
    }

    /**
     * set the compiler arguments
     *
     * @param compilerArgs
     */
    public void setCompilerArgs(String compilerArgs)
    {
        this.compilerArgs = compilerArgs;
    }

    /**
     * set compiler debugging
     *
     * @param debug
     */
    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    /**
     * set any includes
     *
     * @param include
     */
    public void setInclude(String include)
    {
        this.include = include;
    }

    /**
     * set any excludes
     *
     * @param exclude
     */
    public void setExclude(String exclude)
    {
        this.exclude = exclude;
    }

    /**
     * set a skip.  If a module is set to skip, it will not be compiled
     *
     * @param skip
     */
    public void setSkip(String skip)
    {
        setExclude(skip);
    }

    /**
     * run the task
     */
    public void run() throws Exception
    {
        ptolemyCompiled.delete();
        /*
        * CompileModules compile = new CompileModules();
        * compile.bindToOwner(this);
        * compile.init();
        * compile.setModule(moduleName);
        * compile.setCompilerArgs(compilerArgs);
        * compile.setDebug(debug);
        * compile.setInclude(include);
        * compile.setExclude(exclude);
        * compile.execute();
        */
    }
}
