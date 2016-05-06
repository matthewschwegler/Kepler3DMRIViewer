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

package util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import ptolemy.vergil.basic.KeplerDocumentationAttribute;

/**
 * Class to process the documentation file into a format usable by kepler
 * 
 */
public class DocProcessor {
	private int cursor;
	private String s;

	/**
	 * constructor
	 */
	public DocProcessor(int step, File inputFile) throws Exception {
		System.out.println("Step: " + step);
		System.out.println("Input File: " + inputFile);
		if (step == 2) {
			processFiles(inputFile);
		} else if (step == 1) {
			breakUpFile(inputFile);
		} else {
			throw new Exception("Step " + step + " doesn't exist.");
		}
	}

	private String getFileAsString(File f) throws Exception {
		FileReader fr = new FileReader(f);
		StringBuffer sb = new StringBuffer();
		char[] c = new char[1024];
		int numread = fr.read(c, 0, 1024);
		while (numread != -1) {
			sb.append(new String(c, 0, numread));
			numread = fr.read(c, 0, 1024);
		}

		// now we have the file in a string
		s = sb.toString();
		return s;
	}

	/**
	 * breaks up a documentation file delimited by <begin> and <end> tags into
	 * individual files.
	 */
	private void breakUpFile(File f) throws Exception {
		s = getFileAsString(f);
		while (s.length() > 0) {
			int begin = s.indexOf("<begin>");
			int end = s.indexOf("<end>");
			// get the contents
			String fileContents = s.substring(begin + "<begin>".length(), end);
			String fileName = "";
			int cur = 0;
			// get the filename
			while (fileContents.charAt(cur) != ' ') {
				fileName += fileContents.charAt(cur);
				cur++;
			}
			File outputFile = new File(
					"/home/berkley/project/kepler-docs/docFiles/" + fileName);
			System.out.println("writing file: " + outputFile.getAbsolutePath());
			outputFile.createNewFile();
			FileWriter fw = new FileWriter(outputFile);
			fw.write(fileContents, 0, fileContents.length());
			fw.flush();
			fw.close();

			// now cut the fileContents out of s
			s = s.substring(end + "<end>".length(), s.length());
		}

	}

	/**
	 * run step one
	 */
	private void processFiles(File f) throws Exception {
		File dir = new File("/home/berkley/project/kepler-docs/docFiles/");
		File[] fileList = dir.listFiles();
		for (int i = 0; i < fileList.length; i++) {
			s = getFileAsString(fileList[i]);
			processFile(s, fileList[i]);
		}
	}

	/**
	 * process an individual actor doc file
	 */
	private void processFile(String s, File f) throws Exception {
		System.out.println("processing " + f.getAbsolutePath());
		int cur = 0;
		// get the name
		String name = "";
		while (s.charAt(cur) != ' ') {
			name += s.charAt(cur);
			cur++;
		}
		name = name.trim();

		// get the author
		String author = "";
		while (s.charAt(cur) != '\n') {
			author += s.charAt(cur);
			cur++;
		}
		cur++;
		author = author.trim();

		// user level doc
		String uld = "";
		int beg = s.indexOf("@UserLevelDescription", cur);
		int end = s.indexOf("PORTS @UserLevelDescription", cur);
		uld = s.substring(beg + "@UserLevelDescription".length(), end);
		uld = uld.trim();

		cur = end + "PORTS @UserLevelDescription".length();

		// Input ports
		Hashtable inputPortHash = new Hashtable();
		beg = s.indexOf("Input Ports", cur);
		end = s.indexOf("Output Ports", cur);
		String portString = s.substring(beg + "Input Ports".length(), end);
		processPorts(portString, inputPortHash);
		cur = end;

		// Output ports
		Hashtable outputPortHash = new Hashtable();
		beg = s.indexOf("Output Ports", cur);
		end = s.indexOf("Parameters @UserLevelDescription", cur);
		portString = s.substring(beg + "Output Ports".length(), end);
		processPorts(portString, outputPortHash);
		cur = end;

		// parameters
		Hashtable paramHash = new Hashtable();
		beg = s.indexOf("Parameters @UserLevelDescription", cur);
		end = s.indexOf("Sample Demos", cur);
		portString = s.substring(beg
				+ "Parameters @UserLevelDescription".length(), end);
		processPorts(portString, paramHash);
		cur = end;

		// System.out.println("name: " + name);
		// System.out.println("author: " + author);
		// System.out.println("uld: " + uld);
		// printHash(inputPortHash);
		// printHash(outputPortHash);
		// printHash(paramHash);
		// System.out.println("input ports: " + inputPortHash.toString());
		// System.out.println("output ports: " + outputPortHash.toString());
		// System.out.println("params: " + paramHash.toString());
		KeplerDocumentationAttribute da = toDocumentAttributeXML(name, author,
				uld, inputPortHash, outputPortHash, paramHash);
		// System.out.println(da.exportMoML());
		// System.out.println("cursor: " + cursor);
		// System.out.println("length: " + s.length());

		File outputFile = new File(
				"/home/berkley/project/kepler-docs/docFiles/" + name + ".xml");
		FileWriter fw = new FileWriter(outputFile);
		String das = da.exportMoML();
		fw.write(das, 0, das.length());
		fw.flush();
		fw.close();
		System.out.println("done processing " + f.getAbsolutePath());
	}

	/**
	 * prints a hashtable a bit nicer than the toString method
	 */
	private void printHash(Hashtable h) {
		for (Enumeration e = h.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			String val = (String) h.get(key);
			System.out.println(key + ": " + val);
		}
	}

	/**
	 * process the port that starts with begStr and ends with endStr
	 */
	private void processPorts(String portString, Hashtable portHash) {
		int portCur = 0;
		while (portCur < portString.length()) {
			boolean done = false;
			if (!portString.trim().equals("")) { // make sure there is at least
				// one port
				while (portString.charAt(portCur) == ' '
						|| portString.charAt(portCur) == '\n') { // move through
					// any white
					// space
					portCur++;
					if (portCur >= portString.length()) {
						done = true;
						break;
					}
				}
				if (done)
					break;

				String portName = portString.substring(portCur,
						portString.indexOf("\n", portCur)).trim();
				portCur = portString.indexOf("\n", portCur) + 1;
				String portDesc = portString.substring(portCur,
						portString.indexOf("\n", portCur)).trim();
				portHash.put(portName, portDesc);
				portCur = portString.indexOf("\n", portCur);
			} else {
				break;
			}
		}
	}

	/**
	 * creates a documentation attribute
	 */
	private KeplerDocumentationAttribute toDocumentAttributeXML(String name,
			String author, String uld, Hashtable inputPorts,
			Hashtable outputPorts, Hashtable params) throws Exception {
		KeplerDocumentationAttribute da = new KeplerDocumentationAttribute();
		da.setName(name);
		da.setAuthor(author);
		da.setUserLevelDocumentation(uld);
		// need to merge the ports since the DA doesn't make a distinction
		// between
		// input and output
		Hashtable ports = new Hashtable();
		Enumeration keys = inputPorts.keys();
		while (keys.hasMoreElements()) {
			String keyname = (String) keys.nextElement();
			String desc = (String) inputPorts.get(keyname);
			ports.put(keyname, desc);
		}

		keys = outputPorts.keys();
		while (keys.hasMoreElements()) {
			String keyname = (String) keys.nextElement();
			String desc = (String) outputPorts.get(keyname);
			ports.put(keyname, desc);
		}

		da.setPortHash(ports);
		da.setPropertyHash(params);

		return da;
	}

	/**
	 * main()
	 */
	public static void main(String[] args) {
		System.out.println("usage: util.DocProcessor <step> <filename>");
		int step = new Integer(args[0]).intValue();
		String filename = args[1];
		try {
			DocProcessor dp = new DocProcessor(step, new File(filename));
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}