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
package org.kepler.build.project;

import java.io.File;
import java.net.URI;

/**
 * Created by David Welker.
 * Date: Sep 1, 2010
 * Time: 1:53:01 AM
 *
 * @deprecated
 */
public class ProjectDir extends KeplerModulesDir
{
    /**
     * constructor
     *
     * @param pathname
     */
    public ProjectDir(String pathname)
    {
        super(pathname);
    }

    /**
     * constructor
     *
     * @param uri
     */
    public ProjectDir(URI uri)
    {
        super(uri);
    }

    /**
     * constructor
     *
     * @param parent
     * @param child
     */
    public ProjectDir(String parent, String child)
    {
        super(parent, child);
    }

    /**
     * constructor
     *
     * @param parent
     * @param child
     */
    public ProjectDir(File parent, String child)
    {
        super(parent, child);
    }

    /**
     * constructor
     *
     * @param file
     */
    public ProjectDir(File file)
    {
        super(file.getAbsolutePath());
    }
}
