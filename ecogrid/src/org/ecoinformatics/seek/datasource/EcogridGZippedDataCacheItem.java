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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class reprents a gzipped cache item, it will run gunzip to uncompressed
 * this data object
 * 
 * @author Jing Tao
 * 
 */
public class EcogridGZippedDataCacheItem extends EcogridCompressedDataCacheItem {
	private static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.datasource.EcogridGZippedDataCacheItem");
	}

	/**
	 * Default constructor for this object
	 */
	public EcogridGZippedDataCacheItem() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param refresh
	 *            if the compressed file need be uncompressed again even already
	 *            has umpressed before
	 */
	public EcogridGZippedDataCacheItem(boolean refresh) {
		super(refresh);
	}

	/**
	 * This method overwrite the super class - EcogridCompressedDataCacheItem.
	 * It specifys the gunzip method to uncompress the file. The new ungzip file
	 * will be the the cacheitem file name without the file extension. An file
	 * location will be the cachedata/unzip/cacheitemname/ If this is tar file
	 * too, we will untar it after ungzip it.
	 * 
	 * @throws Exception
	 */
	public void unCompressCacheItem() throws Exception {
		if (unCompressedFilePath != null) {
			log.debug("At unCompressCacheItem method in Zip ojbect");
			// read the gzip file and ungzip it
			GZIPInputStream gZipFileReader = new GZIPInputStream(
					new FileInputStream(getFile()));
			String unGZipFileName = removeFileExtension(getAbsoluteFileName());
			String unGZipFilePath = unCompressedFilePath + File.separator
					+ unGZipFileName;
			log.debug("The unGzip aboslute file path is " + unGZipFilePath);
			File unGzipFile = new File(unGZipFilePath);
			FileOutputStream fileWriter = new FileOutputStream(unGzipFile);
			byte[] array = new byte[3000 * 1024];
			int len;
			while ((len = gZipFileReader.read(array)) >= 0) {
				fileWriter.write(array, 0, len);
			}
			gZipFileReader.close();
			fileWriter.close();
			// if this is a tar file too, will untar it.
			if (getIsTarFile()) {
				if (unCompressedFilePath != null) {
					log.debug("untar the file after ungzip");
					EcogridTarArchivedDataCacheItem.extractTarFile(
							unGZipFilePath, unCompressedCacheItemDir);
				}
			}
			unCompressedFileList = unCompressedCacheItemDir.list();
		}
	}

	/*
	 * Given a file name, this method will remove file extension. If no file
	 * extension the given string will be returned.
	 */
	private String removeFileExtension(String fileName) {
		String newFileName = null;
		if (fileName != null) {
			int lastDotIndex = fileName.lastIndexOf(".");
			if (lastDotIndex != -1) {
				newFileName = fileName.substring(0, lastDotIndex);
			} else {
				newFileName = fileName;
			}
		}
		log.debug("The ungzip file name will be " + newFileName);
		return newFileName;
	}

}