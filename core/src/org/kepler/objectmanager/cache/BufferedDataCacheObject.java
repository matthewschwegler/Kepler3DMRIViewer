/*
 * Copyright (c) 2010 The Regents of the University of California.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class BufferedDataCacheObject extends DataCacheObject {

	private static Log log;

	static {
		log = LogFactory
				.getLog("org.kepler.objectmanager.cache.BufferedDataCacheObject");
	}

	/**
	 * Sets the data into the cache item
	 * 
	 *@param aData
	 *            the new data byte array
	 */
	public void setData(byte[] aData) {
		saveData(aData);
	}

	/**
	 * Returns the data as a byte array
	 * 
	 *	 */
	public byte[] getData() throws CacheException {
		return loadData();
	}

	/**
	 * Loads the data from the cache (a file)
	 * 
	 *@return whether it was able to load the data
	 */
	private byte[] loadData() throws CacheException {
		log.debug("getName() = " + getName());
		log.debug("getResourceName() = " + getResourceName());
		log.debug("getLocalFileName() = " + getAbsoluteFileName());
		File file = getFile();

		if (file == null || !file.exists()) {
			log.debug("File is null or does not exist");
			throw new CacheException("File for " + getName()
					+ " is null or does not exist");
		}

		long size = file.length();

		if (size > Integer.MAX_VALUE) {
			// XXX this could be bad for large datasets
			log.debug("loadData - Too Much Data for byte array:");
			throw new CacheException("Too much data for byte array");
		}
		if (size <= 0) {
			log.debug("loadData - File empty:");
			return new byte[0];
		}

		int iSize = (int) size;

		try {
			byte[] mData = new byte[iSize];

			FileInputStream fis = new FileInputStream(file);

			BufferedInputStream bis = new BufferedInputStream(fis);

			int retSize = bis.read(mData, 0, iSize);

			if (retSize != iSize) {
				log.debug("loadData - Wrong amount of data read:");
				throw new CacheException("Wrong amount of data read");
			}

			bis.close();
			log.debug("DataCacheObject - Data was loaded:");
			return mData;
		} catch (FileNotFoundException e) {
			throw new CacheException("File for " + getName()
					+ " does not exist");
		} catch (IOException e) {
			throw new CacheException("File for " + getName() + " io exception");
		}
	}

	/**
	 * Save the data out to a file
	 * 
	 *	 */
	private boolean saveData(byte[] mData) {
		if (mData != null) {
			try {
				if (mData.length > 0) {
					File file = getFile();
					FileOutputStream fos = new FileOutputStream(file);
					BufferedOutputStream bos = new BufferedOutputStream(fos);

					bos.write(mData);
					bos.close();
				}

			} catch (Exception e) {
				System.err.println(e);
				return false;
			}
		}
		return true;
	}

}
