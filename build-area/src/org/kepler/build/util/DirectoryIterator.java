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
package org.kepler.build.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * iterate over directories
 * Created by IntelliJ IDEA. User: sean Date: Feb 17, 2009 Time: 4:47:16 PM
 */
public class DirectoryIterator implements Iterator<File>
{

    /**
     * constructor
     *
     * @param directory
     * @param exclusions
     */
    public DirectoryIterator(File directory, String[] exclusions)
    {
        this.directory = directory;
        this.exclusions = new HashSet<String>();
        this.exclusions.addAll(Arrays.asList(exclusions));

        directories = new ArrayList<File>();

        if (directory.isDirectory())
        {
            getAllDirectories();
        }
        iterator = directories.iterator();
    }

    /**
     * get all directories
     */
    private void getAllDirectories()
    {
        File[] dirs = directory.listFiles(new FileFilter()
        {
            public boolean accept(File file)
            {
                return file.isDirectory();
            }
        });
        directories.addAll(Arrays.asList(dirs));
        for (File dir : dirs)
        {
            if (exclusions.contains(dir.getName()))
            {
                directories.remove(dir);
            }
        }
    }

    /**
     * return true if the iterator has more dirs
     */
    public boolean hasNext()
    {
        return iterator.hasNext();
    }

    /**
     * get the next file
     */
    public File next()
    {
        return iterator.next();
    }

    /**
     * remove the last object
     */
    public void remove()
    {
        iterator.remove();
    }

    private List<File> directories;
    private Iterator<File> iterator;
    private File directory;
    private Set<String> exclusions;
}
