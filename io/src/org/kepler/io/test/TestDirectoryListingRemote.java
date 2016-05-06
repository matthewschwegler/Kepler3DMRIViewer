/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.kepler.io.test;

import java.io.File;

import org.kepler.io.DirectoryListing;
import org.kepler.io.FileInfo;
import org.kepler.ssh.ExecException;
import org.kepler.ssh.SshExec;

/* Test DirectoryListing utility on a local directory */
public class TestDirectoryListingRemote {

	public static void main(String[] arg) throws ExecException {

		String target = arg.length > 0 ? arg[0] : "localhost";
		String dir = arg.length > 1 ? arg[1] : "";
		String pattern = arg.length > 2 ? arg[2] : null;
		System.out.println("Directory to be watched: " + target + ":" + dir
				+ "   with pattern: " + pattern);

		// use identity file if exist for ssh connections
		File id = new File(System.getProperty("user.home") + File.separator
				+ ".ssh" + File.separator + "id_rsa");
		if (id.isFile()) {
			SshExec ssh = new SshExec("dummy", "localhost"); // host@user will
																// be never used
																// in this
																// object...
			ssh.addIdentity(id.getAbsolutePath()); // Only this is what we need!
		} else
			System.out.println("You have to type password at connection\n");

		String patterns[] = new String[1];
		patterns[0] = pattern;
		DirectoryListing dl = new DirectoryListing(target, dir, patterns);

		dl.list(); // initial list
		FileInfo[] files = dl.getList();

		System.out.println("File list at first read (" + files.length
				+ " files):");
		System.out.println(print(files));

		System.out.println("Now sleep for 15 seconds, and read again. "
				+ "Meanwhile add new files to the directory");

		try {
			java.lang.Thread.sleep(15000);
		} catch (InterruptedException ex) {
		}
		;

		dl.list(); // second list
		files = dl.getList();

		System.out.println("File list after second read (" + files.length
				+ " files):");
		System.out.println(print(files));

		files = dl.getNewFiles(false);
		System.out.println("New files (" + files.length + " files):");
		System.out.println(print(files));

	}

	private static StringBuffer print(FileInfo[] files) {
		StringBuffer sb = new StringBuffer("");
		if (files != null && files.length > 0) {
			for (int i = 0; i < files.length; i++) {
				sb.append(files[i].getName() + "\t" + files[i].getSize() + "\t"
						+ files[i].getDate());
				if (i < files.length - 1)
					sb.append("\n");
			}
		}
		return sb;
	}

}