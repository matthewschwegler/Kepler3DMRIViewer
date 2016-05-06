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

package org.ecoinformatics.seek.datasource;

import java.io.File;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.DotKeplerManager;

/**
 * This class will handle compressed data object(for example a zip file)
 * download from server in Chache system. This class will encapsulate unzip the
 * zip file into a directory and give a api to user to get a local file path by
 * given a file entension. For example: a zip file will be download as
 * 1209.dat(it has xyz.bil, xyz.blw, xyz.hdr, xyz.stx)then the zip file will be
 * unzziped to /cachedir/uncompress/1029.dat. If you give a file extension -
 * bil, it will return /cachedir/uncompress/l029.dat/xyz.bil
 * 
 * @author Jing Tao
 * 
 */

public abstract class EcogridCompressedDataCacheItem extends
		EcogridDataCacheItem {
	private static final String UNZIPDIR = "unzip";
	private static final String FILEEXTENSIONSEP = ".";
	protected String unCompressedFilePath = null;
	protected File unCompressedCacheItemDir = null;
	protected String[] unCompressedFileList = null;
	protected boolean refresh = false;

	private static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.datasource.EcogridCompressedDataCacheItem");
	}

	/**
	 * Default constructor
	 */
	public EcogridCompressedDataCacheItem() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param refresh
	 *            if the compressed file need be uncompressed again even already
	 *            has umpressed before
	 */
	public EcogridCompressedDataCacheItem(boolean refresh) {
		super();
		this.refresh = refresh;
	}

	/**
	 * This method will load data from local file first. If there is no loca
	 * file It will download from server. Then it will unzip the file.
	 */
	public int doWork() {
		log.debug("EcogridCompressedDataCacheItem - doing Work mStatus "
				+ getStatus());

		int superStatus = super.doWork();

		// Was there a problem with the download?
		if (superStatus != CACHE_COMPLETE)
			return CACHE_ERROR;

		try {
			String fname = getBaseFileName();
			if (fname != null) {
				log.debug("The local file name is " + fname);
				// DBPATH already has File.separetor as end
				unCompressedFilePath = DotKeplerManager.getInstance().getCacheDirString()
						+ UNZIPDIR + File.separator + fname;
				log.debug("The unzip file path is " + unCompressedFilePath);
				unCompressedCacheItemDir = new File(unCompressedFilePath);
				// if this dir alreay exist we don't want uncompressed again
				if (unCompressedCacheItemDir.exists() && !refresh) {
					log
							.debug("The uncompress cache item dir already exist and refresh is "
									+ refresh + "we don't uncomressed it again");
					unCompressedFileList = unCompressedCacheItemDir.list();
					return CACHE_COMPLETE;
				} else {
					log
							.debug("The uncomress cache item dir doesn't exist or refresh is true"
									+ ", we need to unCompressedCacheItemDir again");
					unCompressedCacheItemDir.mkdirs();
					unCompressCacheItem();
				}

			}

		} catch (Exception e) {
			log.error("Error  unzipping data item " + getName() + " -- Error msg: " + e.getMessage() );
			// we need to delete the uncompress dir for this data item
			unCompressedCacheItemDir.delete();
			return CACHE_ERROR;
		}
		return CACHE_COMPLETE;
	}

	/**
	 * This is an abracted method for uncompress cache item. It will be
	 * overwritten by subclass - EcogridZippedDataCacheItem,
	 * EcogridGZippedDataCacheItem
	 */
	public abstract void unCompressCacheItem() throws Exception;

	/**
	 * This method will get a file path which matches the given file extension
	 * in unzipped file list. If no match, null will be returned
	 * 
	 * @param fileExtension
	 *            String
	 * @return String[]
	 */
	public String[] getUnzippedFilePath(String fileExtension) {
		String[] result = null;
		if (unCompressedFileList == null || fileExtension == null) {
			return result;
		}
		int length = unCompressedFileList.length;
		Vector tmp = new Vector();
		for (int i = 0; i < length; i++) {
			String fileName = (String) unCompressedFileList[i];
			if (fileName != null) {
				log.debug("file name in file list is " + fileName);
				int dotPosition = fileName.lastIndexOf(FILEEXTENSIONSEP);
				String extension = fileName.substring(dotPosition + 1, fileName
						.length());
				log.debug("The file extension for file name " + fileName
						+ " in file list is " + extension);
				if (extension.equals(fileExtension)) {
					// store the file path into a tmp array
					tmp.add(unCompressedFilePath + File.separator + fileName);
				}
			}
		}
		// transfer vector in array
		if (!tmp.isEmpty()) {
			int len = tmp.size();
			result = new String[len];
			for (int j = 0; j < len; j++) {
				result[j] = (String) tmp.elementAt(j);
				log.debug("The file path which math file extension "
						+ fileExtension + " is " + result[j]);
			}
		}
		return result;
	}

	/**
	 * Method to set up refresh variable
	 * 
	 * @param refresh
	 *            boolean
	 */
	public void setRefresh(boolean refresh) {
		this.refresh = refresh;
	}

	/**
	 * Method to get refresh variable
	 * 
	 * @return boolean
	 */
	public boolean getRefresh() {
		return this.refresh;
	}
}