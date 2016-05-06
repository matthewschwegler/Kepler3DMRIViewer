/*
 * Copyright (c) 2010 The Regents of the University of California.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.kepler.build.modules.ModulesTask;
import org.kepler.build.project.RepositoryLocations;

/**
 * Created by David Welker.
 * Date: Sep 3, 2009
 * Time: 5:43:51 PM
 */
public abstract class Brancher extends ModulesTask
{
    protected String moduleName = "undefined";
    protected boolean isBranch;
    protected List<String> existingBranches = new ArrayList<String>();

    /**
     * set the module
     *
     * @param moduleName
     */
    public void setModule(String moduleName)
    {
        //This ensures that the moduleName is only set once.
        if (moduleName.equals("undefined"))
        {
            return;
        }
        this.moduleName = moduleName;
        isBranch = isBranch(moduleName);
    }

    /**
     * return true if the given module is a branch
     *
     * @param moduleName
     * @return
     */
    public boolean isBranch(String moduleName)
    {
        String[] parts = moduleName.split("[-/]");
        String lastPart = parts[parts.length - 1];
        return lastPart.matches("\\d+\\.\\d+");
    }

    /**
     * get the highest version of a published module
     * base is either the module i.e. foo or the module with the major version foo-2
     *
     * @param base
     * @return
     * @throws Exception
     */
    public VersionNumber getHighestVersion(String base) throws Exception
    {
        VersionNumber highestVersion = new VersionNumber(0, 0);
        for (String branch : existingBranches)
        {
            if (branch.matches(base + "-\\d+\\.\\d+"))
            {
                VersionNumber releasedVersion = new VersionNumber(branch);
                if (!highestVersion.isHigherThan(releasedVersion))
                {
                    highestVersion = releasedVersion;
                }
            }
        }
        return highestVersion;
    }

    /**
     * find the existing branches of a module
     *
     * @throws Exception
     */
    public void calculateExistingBranches() throws Exception
    {
        String[] svnLsCommand = {"svn", "ls", RepositoryLocations.BRANCHES};
        Process p = Runtime.getRuntime().exec(svnLsCommand);
        BufferedReader br = new BufferedReader(new InputStreamReader(p
                .getInputStream()));
        String line = null;
        while ((line = br.readLine()) != null)
        {
            line = line.trim();
            if (line.endsWith("/"))
            {
                existingBranches.add(line.substring(0, line.length() - 1));
            }
        }
    }

    /**
     * class that represents a version number for a released module
     *
     * @author welker
     */
    protected class VersionNumber
    {
        int major;
        int minor;

        /**
         * constructor
         *
         * @param major
         * @param minor
         */
        public VersionNumber(int major, int minor)
        {
            this.major = major;
            this.minor = minor;
        }

        /**
         * constructor
         * moduleListing example: foo-1.5/
         *
         * @param moduleListing
         */
        public VersionNumber(String moduleListing)
        {
            String versionNumber = extractNumber(moduleListing);
            major = Integer.parseInt(versionNumber.split("\\.")[0]);
            minor = Integer.parseInt(versionNumber.split("\\.")[1]);
        }

        /**
         * increment the minor version
         *
         * @return
         */
        public VersionNumber incrementMinor()
        {
            return new VersionNumber(major, minor + 1);
        }

        /**
         * increment the major version
         *
         * @return
         */
        public VersionNumber incrementMajor()
        {
            return new VersionNumber(major + 1, 0);
        }

        /**
         * return true if the current version number is higher than the given
         * version number
         *
         * @param other
         * @return
         */
        public boolean isHigherThan(VersionNumber other)
        {
            if (major > other.major)
            {
                return true;
            }
            if (major < other.major)
            {
                return false;
            }
            if (minor > other.minor)
            {
                return true;
            }
            return false;
        }

        /**
         * extract the version number from a module listing
         * moduleListing = foo-1.5/
         * return 1.5
         *
         * @param moduleListing
         * @return
         */
        private String extractNumber(String moduleListing)
        {
            String[] parts = moduleListing.split("[-/]");
            return parts[parts.length - 1];
        }

        /**
         * return the major version
         *
         * @return
         */
        public String majorString()
        {
            return "" + major;
        }

        /**
         * return the minor version
         *
         * @return
         */
        public String minorString()
        {
            return "" + minor;
        }

        /**
         * return a string rep of this version
         */
        public String toString()
        {
            return major + "." + minor;
        }
    }
}
