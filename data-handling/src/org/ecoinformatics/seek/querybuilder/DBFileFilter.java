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

package org.ecoinformatics.seek.querybuilder;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.filechooser.FileFilter;

/**
 * Adapted from the Swing JFileChooser demo and the ExampleFileFilter file
 */
public class DBFileFilter extends FileFilter {
	private static String TYPE_UNKNOWN = "Type Unknown";
	private static String HIDDEN_FILE = "Hidden File";

	private Hashtable mFilters = null;
	private String mDescription = null;
	private String mFullDescription = null;
	private boolean mUseExtensionsInDescription = true;

	/**
	 * Creates a file filter. If no mFilters are added, then all files are
	 * accepted.
	 */
	public DBFileFilter() {
		this.mFilters = new Hashtable();
	}

	/**
	 * Creates a file filter that accepts files with the given extension.
	 * Example: new ExampleFileFilter("jpg");
	 */
	public DBFileFilter(String extension) {
		this(extension, null);
	}

	/**
	 * Creates a file filter that accepts the given file type. Example: new
	 * ExampleFileFilter("jpg", "JPEG Image Images");
	 * 
	 * Note that the "." before the extension is not needed. If provided, it
	 * will be ignored.
	 */
	public DBFileFilter(String extension, String mDescription) {
		this();
		if (extension != null)
			addExtension(extension);

		if (mDescription != null)
			setDescription(mDescription);
	}

	/**
	 * Creates a file filter from the given string array. Example: new
	 * ExampleFileFilter(String {"gif", "jpg"});
	 * 
	 * Note that the "." before the extension is not needed adn will be ignored.
	 */
	public DBFileFilter(String[] mFilters) {
		this(mFilters, null);
	}

	/**
	 * Creates a file filter from the given string array and mDescription.
	 * Example: new DBFileFilter(String {"gif", "jpg"}, "Gif and JPG Images");
	 * 
	 * Note that the "." before the extension is not needed and will be ignored.
	 */
	public DBFileFilter(String[] mFilters, String mDescription) {
		this();
		for (int i = 0; i < mFilters.length; i++) {
			// add mFilters one by one
			addExtension(mFilters[i]);
		}
		if (mDescription != null)
			setDescription(mDescription);
	}

	/**
	 * Return true if this file should be shown in the directory pane, false if
	 * it shouldn't.
	 * 
	 * Files that begin with "." are ignored.
	 * 
	 */
	public boolean accept(File f) {
		if (f != null) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = getExtension(f);
			if (extension != null && mFilters.get(getExtension(f)) != null) {
				return true;
			}
			;
		}
		return false;
	}

	/**
	 * Return the extension portion of the file's name .
	 */
	public String getExtension(File f) {
		if (f != null) {
			String filename = f.getName();
			int i = filename.lastIndexOf('.');
			if (i > 0 && i < filename.length() - 1) {
				return filename.substring(i + 1).toLowerCase();
			}
			;
		}
		return null;
	}

	/**
	 * Adds a filetype "dot" extension to filter against.
	 * 
	 * For example: the following code will create a filter that mFilters out
	 * all files except those that end in ".jpg" and ".tif":
	 * 
	 * ExampleFileFilter filter = new ExampleFileFilter();
	 * filter.addExtension("jpg"); filter.addExtension("tif");
	 * 
	 * Note that the "." before the extension is not needed and will be ignored.
	 */
	public void addExtension(String extension) {
		if (mFilters == null) {
			mFilters = new Hashtable(5);
		}
		mFilters.put(extension.toLowerCase(), this);
		mFullDescription = null;
	}

	/**
	 * Returns the human readable mDescription of this filter. For example:
	 * "JPEG and GIF Image Files (*.jpg, *.gif)"
	 */
	public String getDescription() {
		if (mFullDescription == null) {
			if (mDescription == null || isExtensionListInDescription()) {
				mFullDescription = mDescription == null ? "(" : mDescription
						+ " (";
				// build the mDescription from the extension list
				Enumeration extensions = mFilters.keys();
				if (extensions != null) {
					mFullDescription += "." + (String) extensions.nextElement();
					while (extensions.hasMoreElements()) {
						mFullDescription += ", ."
								+ (String) extensions.nextElement();
					}
				}
				mFullDescription += ")";
			} else {
				mFullDescription = mDescription;
			}
		}
		return mFullDescription;
	}

	/**
	 * Sets the human readable mDescription of this filter. For example:
	 * filter.setDescription("Gif and JPG Images");
	 */
	public void setDescription(String mDescription) {
		this.mDescription = mDescription;
		mFullDescription = null;
	}

	/**
	 * Determines whether the extension list (.jpg, .gif, etc) should show up in
	 * the human readable mDescription.
	 * 
	 * Only relevent if a mDescription was provided in the constructor or using
	 * setDescription();
	 */
	public void setExtensionListInDescription(boolean b) {
		mUseExtensionsInDescription = b;
		mFullDescription = null;
	}

	/**
	 * Returns whether the extension list (.jpg, .gif, etc) should show up in
	 * the human readable mDescription.
	 * 
	 * Only relevent if a description was provided in the constructor or using
	 * setDescription();
	 */
	public boolean isExtensionListInDescription() {
		return mUseExtensionsInDescription;
	}
}