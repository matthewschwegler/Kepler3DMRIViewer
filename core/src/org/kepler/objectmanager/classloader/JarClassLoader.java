/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

package org.kepler.objectmanager.classloader;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;

/**
 * @author tao This class will load the given jar files(specified by URL) into
 *         JVM. This class will suport to Object manager to dynamic load jar
 *         files into memory
 */
public class JarClassLoader extends URLClassLoader {
	private URL[] url;

	/**
	 * Constructor (currently we only support url points to jar file rather than
	 * the directory
	 * 
	 * @param url
	 *            The url array which point jar files
	 */
	public JarClassLoader(URL[] url) {
		super(url);
		this.url = url;
	}

	/**
	 * This method will load classes in the jar files into jvm
	 * 
	 */
	public void loadJarFiles() throws IOException, ClassNotFoundException {
		if (url != null) {
			int size = url.length;
			for (int i = 0; i < size; i++) {
				URL u = url[i];
				loadOneJarFile(u);
			}
		}
	}

	private void loadOneJarFile(URL u) throws IOException,
			ClassNotFoundException {
		URL jarURL = new URL("jar", "", u + "!/");
		String mainClassName = null;
		// get main class name in the jar
		JarURLConnection connection = (JarURLConnection) jarURL
				.openConnection();
		Attributes attribute = connection.getMainAttributes();
		if (attribute != null) {
			mainClassName = attribute.getValue(Attributes.Name.MAIN_CLASS);
			loadClass(mainClassName);
		}

	}
}