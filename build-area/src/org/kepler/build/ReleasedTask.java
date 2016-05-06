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

import java.util.ArrayList;
import java.util.List;

import org.kepler.build.modules.ModulesTask;

/**
 * Created by David Welker.
 * Date: Dec 10, 2009
 * Time: 11:22:15 PM
 */
public abstract class ReleasedTask extends ModulesTask
{
    /**
     * @param names
     * @return
     */
    protected List<String> trimOlderNames(List<String> names)
    {
        if (names == null)
        {
            return new ArrayList<String>();
        }
        if (names.isEmpty())
        {
            return names;
        }
        List<String> removeList = new ArrayList<String>();
        for (String name : names)
        {
            if (nameIsOld(name, names))
            {
                removeList.add(name);
            }
        }
        names.removeAll(removeList);
        return names;
    }

    /**
     * @param name
     * @param names
     * @return
     */
    private boolean nameIsOld(String name, List<String> names)
    {
        for (String otherName : names)
        {
            if (nameIsOld(name, otherName))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @param name
     * @param otherName
     * @return
     */
    private boolean nameIsOld(String name, String otherName)
    {
        if (!getPattern(name).equals(getPattern(otherName)))
        {
            return false;
        }
        int patch = extractPatch(name);
        int otherPatch = extractPatch(otherName);
        return patch < otherPatch;
    }

    /**
     * @param name
     * @return
     */
    private String getPattern(String name)
    {
        String[] parts = name.split("\\.");
        String patch = parts[parts.length - 1];
        return name.substring(0, name.length() - patch.length() - 1);
    }

    /**
     * @param name
     * @return
     */
    private int extractPatch(String name)
    {
        String[] parts = name.split("\\.");
        return Integer.parseInt(parts[parts.length - 1]);
    }
}
