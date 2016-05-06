/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-08-03 09:30:04 -0700 (Fri, 03 Aug 2012) $' 
 * '$Revision: 30343 $'
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;

/**
 * A place to keep some useful file processing static utility methods.
 * @author Aaron Schultz
 */
public class FileUtil {


	/**
	 * Return the highest ranked (i.e. top override) of the given
	 * relative file path.
	 * 
	 * @param relativePath
	 * @return
	 */
	public static File getHighestRankedFile(String relativePath) {
		for (Module module : ModuleTree.instance()) {
			File file = new File(module.getDir(), relativePath);
			if (file.exists()) {
				return file;
			}
		}

		// System.out.println("Could not find file: " + relativePath);
		return null;
	}
	
	/**
	 * Get a File object from a resource on the classpath using the 
	 * ClassLoader that loaded the given object.
	 * 
	 * @param relativeTo
	 * @param relativePath
	 * @return
	 */
	public static File getResourceAsFile(Object relativeTo, String relativePath) {
		ClassLoader cl = relativeTo.getClass().getClassLoader();
		URL url = cl.getResource(relativePath);
		URI uri;
		try {
			uri = url.toURI();
			File f = new File(uri);
			return f;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Read in a file as a string.
	 * 
	 * @param f
	 * @return
	 * @throws java.io.IOException
	 */
	public static String readFileAsString(File file) throws java.io.IOException {
		int fLength = (int) file.length();
	    byte[] buffer = new byte[fLength];
	    FileInputStream fis = new FileInputStream(file);
	    BufferedInputStream f = new BufferedInputStream(fis);
	    f.read(buffer);
	    f.close();
	    return new String(buffer);
	}
	
	/*
	 * Convert the InputStream to a String.
	 */
	public static String convertStreamToString(InputStream is) throws IOException {
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}
	
	public static String appendNewLine(String s) {
		s += System.getProperty("line.separator");
		return s;
	}
	
	/**
	 * @param base
	 * @param sub
	 * @return true if sub is a subdirectory of base
	 * @throws IOException 
	 */
	public static boolean isSubdirectory(File base, File sub) throws IOException {
		String[] basePaths = pathRep(base);
		String[] subPaths = pathRep(sub);
		if (basePaths.length >= subPaths.length) {
			return false;
		} else {
			boolean sameRoot = true;
			for (int i = 0; i < basePaths.length; i++) {
				if (!basePaths[i].equals(subPaths[i])) {
					sameRoot = false;
				}
			}
			return sameRoot;
		}
	}
	
	/**
	 * Return a string array representation of the file path.
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static String[] pathRep(File f) throws IOException {
		String a = clean(f);
		String[] paths = a.split("/");
		return paths;
	}
	
	/**
	 * Always return the true unique string representation of 
	 * a file with no trailing separators.
	 * 
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static String clean(File f) throws IOException {
		String s = f.getCanonicalFile().toURI().getPath();
		if (s.endsWith("/")) {
			s = s.substring(0,s.length()-1);
		}
		return s;
	}
	
	/**
	 * Return filename without extension.
	 * @param filename
	 * @return
	 */
	public static String getFileNameWithoutExtension(String filename) {
		// filename without the extension
		String choppedFilename;

		// extension without the dot
		//String ext;

		int dotPlace = filename.lastIndexOf('.');

		if (dotPlace >= 0) {
			// possibly empty
			choppedFilename = filename.substring(0, dotPlace);

			// possibly empty
			//ext = filename.substring(dotPlace + 1);
		} else {
			// was no extension
			choppedFilename = filename;
			//ext = "";
		}
		
		return choppedFilename;
	}

	
	/*
	 * Get the extension of a file.
	 */
	public static String getExtension(File f) {
		String ext = "";
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}
	
	/** Copy a file or directory to another. If the source is a directory,
	 *  the contents are recursively copied.
	 * @param sourceLocation the source
	 * @param targetLocation the destination
	 * @param overwrite If true, overwrite files in the targetLocation
	 * @throws IOException
	 */
	public static void copyDirectory(File sourceLocation , File targetLocation,
	        boolean overwrite) throws IOException {
	 
	    if (sourceLocation.isDirectory()) {
	        if (!targetLocation.exists()) {
	            boolean created = targetLocation.mkdirs();
	            if (!created){
	            	System.out.println("FileUtil copyDirectory ERROR couldn't mkdirs:" + targetLocation);
	            	// if we couldn't create the directories, stop copying
	            	return;
	            }
	        }
	 
	        String[] children = sourceLocation.list();
	        for (int i=0; i<children.length; i++) {
	            copyDirectory(new File(sourceLocation, children[i]),
	                    new File(targetLocation, children[i]), overwrite);
	        }
	    } else if(overwrite || !targetLocation.exists()) {
	        InputStream in = new FileInputStream(sourceLocation);
	        OutputStream out = new FileOutputStream(targetLocation);
	 
	        // Copy the bits from instream to outstream
	        byte[] buf = new byte[1024];
	        int len;
	        while ((len = in.read(buf)) > 0) {
	            out.write(buf, 0, len);
	        }
	        in.close();
	        out.close();
	        
	        //set executable property
	        if (sourceLocation.canExecute())
	        	targetLocation.setExecutable(true);
	    }
	}
	
}
