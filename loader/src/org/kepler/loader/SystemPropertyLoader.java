/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 13:34:53 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31479 $'
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

package org.kepler.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Properties;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.build.project.ProjectLocator;

/**
 * Created by David Welker. Date: Jul 31, 2008 Time: 4:18:10 PM
 */
public class SystemPropertyLoader
{
	public static void load() throws URISyntaxException, IOException
	{
		for (Module module : ModuleTree.instance())
		{
			loadSystemPropertiesFromModule(module);
		}
		makeSubstitutes();
	}

	private static void loadSystemPropertiesFromModule(Module module) throws IOException
	{
		
		File systemPropertiesDir = module.getSystemPropertiesDir();
		if (!systemPropertiesDir.exists()) return;
		File[] children = systemPropertiesDir.listFiles(new FilenameFilter()
		{
			public boolean accept(File dir, String filename)
			{
				return filename.endsWith(".properties");
			}
		});
		for (File systemPropertiesFile : children)
			loadSystemProperties(systemPropertiesFile);
	}

	private static void makeSubstitutes()
	{
		Enumeration propertyNames = System.getProperties().propertyNames();
		while (propertyNames.hasMoreElements())
		{
			String name = (String) propertyNames.nextElement();
			String value = System.getProperty(name);
			//File projectDir = ProjectLocator.getProjectDir();
			// if( name.equals("KEPLER") && !value.endsWith("core") )
			// value = "${project.path}/kepler-1.0-jar-tag";
			if (value.contains("${project.path}"))
			{
				value = value.replace("${project.path}", ProjectLocator.getProjectDir().getAbsolutePath()).trim();
				System.setProperty(name, value);
			}
			if (value.contains("${user.home}"))
			{
				value = value.replace("${user.home}", System.getProperty("user.home")).trim();
				System.setProperty(name, value);
			}
		}
	}

	private static void loadSystemProperties(File systemPropertiesFile) throws IOException
	{
		Properties systemProperties = new Properties(System.getProperties());
		InputStream stream = null;
		try {
			stream = new FileInputStream(systemPropertiesFile);
			systemProperties.load(stream);
		} finally {
			if(stream != null) {
				stream.close();
			}
		}
		System.setProperties(systemProperties);
	}

	public static void main(String[] args) throws URISyntaxException, IOException
	{
		SystemPropertyLoader.load();
	}

}
