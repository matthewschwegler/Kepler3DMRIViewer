/*
 *  Copyright (c) 2003-2010 The Regents of the University of California.
 *  All rights reserved.
 *  Permission is hereby granted, without written agreement and without
 *  license or royalty fees, to use, copy, modify, and distribute this
 *  software and its documentation for any purpose, provided that the above
 *  copyright notice and the following two paragraphs appear in all copies
 *  of this software.
 *  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 *  FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 *  THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 *  PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 *  PT_COPYRIGHT_VERSION_2
 *  COPYRIGHTENDKEY
 */
package org.kepler.kar;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * KAR manifest file. This extends java.util.jar.Manifest with utility methods.
 */
public class KARManifest extends Manifest {
	
	private final static String MANIFEST_VERSION = "1.4.2";
	
	/**
	 * Constructor.
	 */
	public KARManifest() {
		super();
		init();
	}

	/**
	 * Constructor. Create the manifest from the inputstream
	 * 
	 * @param is
	 */
	public KARManifest(InputStream is) throws IOException {
		super(is);
		init();
	}

	/**
	 * Constructor. Create the manifest as a copy of the passed manifest
	 * 
	 * @param man
	 */
	public KARManifest(Manifest man) {
		super(man);
		init();
	}

	/**
	 * initialize the attributes
	 */
	private void init() {
		Attributes mainAtts = getMainAttributes();
		if (mainAtts == null) {
			mainAtts = new Attributes();
		}
		mainAtts.put(Attributes.Name.MANIFEST_VERSION, MANIFEST_VERSION);
		// mainAtts.put(new Attributes.Name("KAR-Version"), "1.0");
		mainAtts.put(KARFile.KAR_VERSION, KARFile.CURRENT_VERSION);
	}

	/**
	 * add a main manifest attribute
	 * 
	 * @param name
	 * @param value
	 */
	public void addMainAttribute(String name, String value) {
		Attributes atts = getMainAttributes();
		atts.putValue(name, value);
	}

	/**
	 * get a main manifest attribute
	 * 
	 * @param name
	 */
	public String getMainAttribute(String name) {
		Attributes atts = getMainAttributes();
		return atts.getValue(name);
	}

	/**
	 * add an entry specific attribute
	 * 
	 * @param entry
	 *            the JarEntry that you are adding an attribute for
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 */
	public void addEntryAttribute(KAREntry entry, String name, String value) {
		addEntryAttribute(entry.getName(), name, value);
	}

	/**
	 * add an entry specific attribute
	 * 
	 * @param entryName
	 *            the name of the JarEntry that you are adding an attribute for
	 * @param name
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 */
	public void addEntryAttribute(String entryName, String name, String value) {
		Map entries = getEntries();
		if (entries.get(entryName) == null) {
			entries.put(entryName, new Attributes());
		}

		Attributes atts = getAttributes(entryName);
		if (atts == null) {
			atts = new Attributes();
		}

		atts.put(new Attributes.Name(name), value);
	}

	/**
	 * get a JarEntry specific attribute
	 * 
	 * @param entry
	 *            the entry you're getting the attribute for
	 * @param name
	 *            the name of the attribute
	 */
	public String getEntryAttribute(KAREntry entry, String name) {
		Attributes atts = getAttributes(entry.getName());
		return atts.getValue(name);
	}

	/**
	 * get a JarEntry specific attribute
	 * 
	 * @param entryName
	 *            the name of the JarEntry you're getting the attribute for
	 * @param name
	 *            the name of the attribute
	 */
	public String getEntryAttribute(String entryName, String name) {
		Attributes atts = getAttributes(entryName);
		return atts.getValue(name);
	}
}
