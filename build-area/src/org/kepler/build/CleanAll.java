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
 * class to clean all targets in the build
 *
 * @author welker
 */
public class CleanAll extends ModulesTask
{
    /**
     * run the task
     */
    @Override
    public void run() throws Exception
    {
        //clean, clean-cache, clean-jar, clean-kar, clean-tests, clean-installer
        Clean clean = new Clean();
        clean.bindToOwner(this);
        clean.init();
        clean.execute();

        CleanJar cleanJar = new CleanJar();
        cleanJar.bindToOwner(this);
        cleanJar.init();
        cleanJar.execute();

        CleanCache cleanCache = new CleanCache();
        cleanCache.bindToOwner(this);
        cleanCache.init();
        cleanCache.execute();

        CleanKar cleanKar = new CleanKar();
        cleanKar.bindToOwner(this);
        cleanKar.init();
        cleanKar.execute();

        CleanDotKepler cleanDotKepler = new CleanDotKepler();
        cleanDotKepler.bindToOwner(this);
        cleanDotKepler.init();
        cleanDotKepler.execute();
    }

}
