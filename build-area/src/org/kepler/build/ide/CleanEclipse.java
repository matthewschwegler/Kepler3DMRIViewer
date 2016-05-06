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
package org.kepler.build.ide;

import org.kepler.build.modules.Module;
import org.kepler.build.project.ProjectLocator;

/**
 * Clean the eclipse workspace files
 * Created by David Welker.
 * Date: Sep 18, 2008
 * Time: 12:04:22 PM
 */
public class CleanEclipse extends CleanIDE
{
    /**
     * clean the .project and .classpath files in the build dir
     */
    public void generalClean()
    {
        delete(ProjectLocator.getBuildDir(), ".project");
        delete(ProjectLocator.getBuildDir(), ".classpath");
    }

    /**
     * clean the .project and .classpath files in the module dir
     */
    public void cleanModule(Module module)
    {
        delete(module, ".project");
        delete(module, ".classpath");
    }
}
