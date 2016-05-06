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

package org.sdm.spa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Zhengang Cheng
 * 
 *         To hold misc utility functions.
 * 
 */
public class Util {
	/**
	 * Method remove. Remove specified char from the string. Added because
	 * RepaceAll is not available for jdk before 1.4.
	 * 
	 * @param str
	 * @param rechar
	 * @return String
	 */
	public static String remove(String str, char rechar) {
		int len = str.length();
		char buf[] = new char[len];

		int i = 0;
		int j = 0;
		char c;
		while (i < len) {
			c = str.charAt(i);
			if (c != rechar) {
				buf[j] = c;
				j++;
			}
			i++;
		}
		return new String(buf, 0, j);
	}

	/**
	 * Get Caching result It will be stored at file p1_p2;
	 */
	public static String getFromCache(String md, String in) {
		String p1, p2;
		String fname = md.hashCode() + "_" + in.hashCode();
		File file = new File(fname);
		String res = null;
		if (file.exists()) {
			res = getFile(file);
		}
		return res;

	}

	/**
	 * Save into Caching File It will be stored at file p1_p2;
	 */
	public static void saveToCache(String md, String in, String res) {
		String p1, p2;
		String fname = md.hashCode() + "_" + in.hashCode();
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					fname)));
			bw.write(res);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static String getFile(File file) {
		String content = null;

		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			int size = fis.available();

			char[] buf = new char[size];
			int i = br.read(buf, 0, size);
			content = String.valueOf(buf, 0, size);
			// Debug.logInfo("Processing: size" + size + " read:" + i, module);
			fis.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return content;
	}

	public static boolean toCache = false;

}