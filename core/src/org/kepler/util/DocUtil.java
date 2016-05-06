/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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

package org.kepler.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * a class to do some documentation tranformations
 */
public class DocUtil {
	/**
	 * the constructor
	 */
	public DocUtil(File dir) throws IOException {
		File index = new File(dir, "index.html");
		String html = "<html><head><title>Documentation Index</title></head><body><ul>";
		FileWriter fw = new FileWriter(index);
		fw.write(html);
		fw.flush();
		createIndex(dir, fw);
		fw.write("</ul></body></html>");
		fw.flush();
		fw.close();
	}

	/**
	 * this method will index all of the documentation files it finds in the
	 * provided directory. the index will be placed in the dir with the other
	 * files
	 */
	public File[] createIndex(File dir, FileWriter fw) throws IOException {
		File[] dirlist = dir.listFiles();
		System.out.println("dirlist size: " + dirlist.length);
		for (int i = 0; i < dirlist.length; i++) {
			System.out.println("dirlist: " + dirlist[i].getName());
			if (dirlist[i].isDirectory()) {
				System.out.println("recursing");
				createIndex(dirlist[i], fw);
			} else {
				if (!dirlist[i].getName().equals("index.html")) {
					System.out.println("writing");
					fw.write("<li><a href=\"" + dirlist[i].getPath() + "\">"
							+ dirlist[i].getName() + "</a></li>");
				}
			}
		}
		System.out.println("returning");
		return null;
	}

	/**
	 * main
	 */
	public static void main(String[] args) {
		String dir = args[0];
		File f = new File(dir);
		if (!f.isDirectory()) {
			System.err.println("The first argument must be a directory.");
			System.exit(1);
		}
		try {
			System.out.println("using directory: " + f.getAbsolutePath());
			new DocUtil(f);
		} catch (Exception e) {
			System.err.println("Could not complete the documentation: "
					+ e.getMessage());
		}
	}

}