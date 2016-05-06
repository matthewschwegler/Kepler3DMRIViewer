/*
 * Copyright (c) 2013 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 16:13:10 -0800 (Thu, 21 Feb 2013) $'
 * '$Revision: 31488 $'
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that represents a version
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Nov 30, 2009
 * Time: 4:23:07 PM
 */
public class Version
{
	
    private final String basename;
    private final Integer major;
    private final Integer minor;
    private final Integer micro;
    private String releaseLevel;
    private final Integer revision;
    
    public static String nameVersionSeparator = "-";

    /**
     * inner class for a builder
     *
     * @author berkley
     */
    public static class Builder
    {
        // Required
        private final String basename;
        private final int major;

        // Optional
        private Integer minor = null;
        private Integer micro = null;
        private String releaseLevel = null;
        private Integer revision = null;

        /**
         * constructor
         *
         * @param basename
         * @param major
         */
        public Builder(String basename, int major)
        {
            this.basename = basename;
            this.major = major;
        }

        /**
         * minor version
         *
         * @param val
         * @return
         */
        public Builder minor(int val)
        {
            this.minor = val;
            return this;
        }

        /**
         * micro version
         *
         * @param val
         * @return
         */
        public Builder micro(int val)
        {
            this.micro = val;
            return this;
        }

        /**
         * release level
         *
         * @param val
         * @return
         */
        public Builder releaseLevel(String val)
        {
            this.releaseLevel = val;
            return this;
        }

        /**
         * revision
         *
         * @param val
         * @return
         */
        public Builder revision(int val)
        {
            this.revision = val;
            return this;
        }

        /**
         * build
         *
         * @return
         */
        public Version build()
        {
            return new Version(this);
        }
    }

    /**
     * constructor
     *
     * @param basename
     */
    private Version(String basename)
    {
        this.basename = basename;
        this.major = null;
        this.minor = null;
        this.micro = null;
        this.releaseLevel = null;
        this.revision = null;
    }

    /**
     * constructor
     *
     * @param builder
     */
    private Version(Builder builder)
    {
        this.basename = builder.basename;
        this.major = builder.major;
        this.minor = builder.minor;
        this.micro = builder.micro;
        this.releaseLevel = builder.releaseLevel;
        this.revision = builder.revision;
    }

    private static Pattern versionStringPattern = Pattern.compile("(.*?)" + // base name
    		nameVersionSeparator + // a real and actual dash
            "([0-9]+)" + // major version number

            "(?:" + // The following is optional:
            "\\.([0-9]+)" + // a period and a minor version number
            "(?:\\.([0-9]+))?" + // a period and a micro version number
            // NOTE: You can only have a micro version number if you also have a minor version number
            ")?" + "(?:" + // The following is optional:
            "(.*?)" + // The release level
            "([0-9]+)?" + // The patch number
            // NOTE: You can only have a patch number if you also have a release level string
            ")?$");

    /**
     * get a version from a string
     *
     * @param name
     * @return
     */
    public static Version fromUnversionedString(String name)
    {
        return new Version(name);
    }

    /**
     * get a version from a string
     *
     * @param name
     * @return
     */
    public static Version fromVersionString(String name)
    {
        Matcher matcher = versionStringPattern.matcher(name);
        if (!matcher.matches())
        {
            // Could not match string. Could be an unversioned module.
            throw new IllegalArgumentException("Module name " + name + " does not " +
                    "appear to contain a version string.");
        }

        Builder builder = new Builder(matcher.group(1), Integer.valueOf(matcher
                .group(2)));

        String minorString = matcher.group(3);
        String microString = matcher.group(4);
        String releaseLevelString = matcher.group(5);
        String revisionString = matcher.group(6);

        try
        {
            builder.minor(Integer.valueOf(minorString));
        }
        catch (NumberFormatException ignored)
        {
        } // All numbers default to null at first

        try
        {
            builder.micro(Integer.valueOf(microString));
        }
        catch (NumberFormatException ignored)
        {
        }

        try
        {
            builder.revision(Integer.valueOf(revisionString));
        }
        catch (NumberFormatException ignored)
        {
        }

        builder.releaseLevel(("".equals(releaseLevelString)) ? null
                : releaseLevelString);

        return builder.build();
    }

    /**
     * See if a string is versioned.
     */
    public static boolean isVersioned(String name)
    {
        Matcher matcher = versionStringPattern.matcher(name);
        return matcher.matches();
    }

    /**
     * get a basename
     *
     * @return
     */
    public String getBasename()
    {
        return basename;
    }
    
    /**
     * Return version as String if available, null if not.
     * @return major.minor[.micro] or null
     */
    public String getVersionString(){
		String version = null;
		if (!hasMajor()){
			return version;
		}
		version =""+ getMajor();
		if (!hasMinor()){
			return version;
		}
		version = version.concat("."+ getMinor());
		if (!hasMicro()){
			return version;
		}
		version = version.concat("."+ getMicro());

		return version;
    }

    /**
     * get a major version number
     *
     * @return
     */
    public int getMajor()
    {
        return major;
    }

    /**
     * get a minor version number
     *
     * @return
     */
    public int getMinor()
    {
        return minor;
    }

    /**
     * get a micro version number
     */
    public int getMicro()
    {
        return micro;
    }

    /**
     * get a release version number
     *
     * @return
     */
    public String getReleaseLevel()
    {
        return releaseLevel;
    }

    /**
     * get the revision
     *
     * @return
     */
    public int getRevision()
    {
        return revision;
    }

    /**
     * return true if there is a major release version
     *
     * @return
     */
    public boolean hasMajor()
    {
        return major != null;
    }

    /**
     * return true if there is a minor version
     *
     * @return
     */
    public boolean hasMinor()
    {
        return minor != null;
    }

    /**
     * return true if there is a micro version number
     *
     * @return
     */
    public boolean hasMicro()
    {
        return micro != null;
    }

    /**
     * return true if there is a release version
     *
     * @return
     */
    public boolean hasReleaseLevel()
    {
        return releaseLevel != null;
    }

    /**
     * get the stem name
     *
     * @param name
     * @return
     */
    public static String stem(String name)
    {
        try
        {
            return fromVersionString(name).getBasename();
        }
        catch (IllegalArgumentException ex)
        {
            return fromUnversionedString(name).getBasename();
        }
    }



}
