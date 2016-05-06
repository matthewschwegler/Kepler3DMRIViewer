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

package org.kepler.objectmanager.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * This class is not as generic as the name may indicate. It is designed to get
 * the metadata to determine where the data is stored and then does a Ecogrid
 * "get" to the data.
 */
public class DataCacheFileObject extends DataCacheObject {
	// File Type
	public static final int UNKNOWN = 0; // file stored internally
	public static final int INTERNAL = 1; // file stored internally
	public static final int EXTERNAL = 2; // file is external to cache
	public static final int COPY_FILE_TO_CACHE = 3; // file is moved, then Type
													// is set to INTERNAL

	// Protected
	protected int mFileLoc = UNKNOWN;

	/**
	 * A DataCacheFileObject object that can point to an itnernal file or
	 * external file object
	 * 
	 */
	public DataCacheFileObject() {
		super();
	}

	/**
	 * @return Returns whether the file is internal or external, if nothing has
	 *         happened then it return UNKNOWN
	 */
	public int getFileLocation() {
		return mFileLoc;
	}

	/**
	 * @return Returns the string describing the files type
	 */
	public String getType() {
		return getResourceName();
	}

	/**
	 * Set the status of the file item
	 * 
	 * @param aFileLoc
	 *            the location type
	 */
	public void setFileLocation(int aFileLoc) {
		mFileLoc = aFileLoc;
	}

	/**
	 * This copies the external file into the cache and saves it
	 * 
	 * @param aPhysicalFileName
	 *            the physical file name to be copied
	 */
	private void copyFileToCache(String aPhysicalFileName) {
		File currentFile = new File(aPhysicalFileName);
		if (currentFile == null || !currentFile.exists()) {
			return;
		}
		try {
			File mFile = getFile();
			if (mFile != null) {
				FileOutputStream fos = new FileOutputStream(mFile);
				if (fos != null) {
					BufferedOutputStream bos = new BufferedOutputStream(fos);
					if (bos != null) {
						FileInputStream fis = new FileInputStream(currentFile);
						if (fis != null) {
							BufferedInputStream bis = new BufferedInputStream(
									fis);
							if (bis != null) {
								long filesize = currentFile.length();
								int size = 20500;
								byte[] data = new byte[size]; // 20K
								while (filesize > 0) {
									int retSize = bis.read(data, 0, size);
									bos.write(data, 0, retSize);
									filesize -= retSize;
								}
								mFileLoc = INTERNAL;
								bis.close();
							}
							fis.close();
						}
						bos.close();
					}
					fos.close();
				}
			}
		} catch (Exception e) {
			System.err.println(e);
			mFileLoc = UNKNOWN;
		}

	}

	/**
	 * This is one of the hardest working methods in the class. It determines
	 * what to do the aFileName argument. If the file type is: INTENRAL then it
	 * creates a new "empty" file, and the aFileName is just text EXTERNAL then
	 * it uses aFileName as the mLocalFileName data member in order to point at
	 * the extneral file. NOTE: An extneral file is NEVER deleted by the DCM
	 * COPY_FILE_TO_CACHE then it copies the file from the extneral location
	 * identifed by the aFileName argument to an intneral cache file. Afterward
	 * it sets the mFileLoc to INTERNAL
	 * 
	 * @param aPhysicalFileName
	 *            the "physical" name of a file (this name will not be stored if
	 *            aFileLocation is COPY_FILE_TO_CACHE)
	 * @param aLogicalName
	 *            the "logical" name of a file unrelated to the physical name
	 * @param aType
	 *            a description of the file's type (optional and can be null)
	 * @param aFileLocation
	 *            the file location type (see DataCacheFileObject for values)
	 */
	public void initializeWithFileName(String aPhysicalName,
			String aLogicalName, String aType, int aFileLocation) {
		if (mFileLoc == INTERNAL) {
			File mFile = getFile();
			if (mFile != null) {
				mFile.delete();
			}
		}

		mFileLoc = aFileLocation;
		setAbsoluteFileName(null);
		setName(aLogicalName);
		setResourceName(aType);

		switch (mFileLoc) {
		case INTERNAL:
			setBaseFileName(aLogicalName);
			break;

		case EXTERNAL:
			setAbsoluteFileName(aPhysicalName);
			break;

		case COPY_FILE_TO_CACHE:
			copyFileToCache(aPhysicalName);
			break;
		}
	}

	// ----------------------------------------------------------------
	// -- Overrides
	// ----------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ecoinformatics.seek.datasource.DataCacheObject#doWork()
	 */
	public int doWork() {
		return CACHE_COMPLETE;
	}

}