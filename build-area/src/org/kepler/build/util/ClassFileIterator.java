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
import java.io.IOException;
import java.util.Iterator;

import org.apache.tools.ant.taskdefs.optional.depend.ClassFile;
import org.apache.tools.ant.taskdefs.optional.depend.DirectoryIterator;

/**
 * class to iterate of classes
 *
 * @author welker
 */
public class ClassFileIterator extends DirectoryIterator implements
        Iterable<ClassFile>
{

    /**
     * constructor
     *
     * @param rootDirectory
     * @throws IOException
     */
    public ClassFileIterator(File rootDirectory) throws IOException
    {
        super(rootDirectory, true);
    }

    /**
     * get the iterator
     */
    public Iterator<ClassFile> iterator()
    {
        return new Iterator<ClassFile>()
        {

            ClassFile next = null;

            public boolean hasNext()
            {
                next = getNextClassFile();
                return next != null;
            }

            public ClassFile next()
            {
                return next;
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }

        };
    }

}
