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
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ice.tar.TarArchive;

/**
 * This class reprents a tar archive object. Thought tar file is not a realy a
 * comression file, we still consider it is because want to untar it like unzip
 * file.
 * 
 * @author Jing Tao
 * 
 */

public class EcogridTarArchivedDataCacheItem extends
		EcogridCompressedDataCacheItem {
	private static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.datasource.EcogridTarArchivedDataCacheItem");
	}

	/**
	 * Default constructor
	 */
	public EcogridTarArchivedDataCacheItem() {
		super();
	}

	/**
	 * Constructor if we need refresh it even the cache item already untared
	 * 
	 * @param refresh
	 *            boolean
	 */
	public EcogridTarArchivedDataCacheItem(boolean refresh) {
		super(refresh);
	}

	/**
	 * This method will specifically to untar a cache item. The un tared file
	 * location will be cachedata/unzip/cacheitemlocationfilename/
	 * 
	 * @throws Exception
	 */
	public void unCompressCacheItem() throws Exception {
		if (unCompressedCacheItemDir != null) {
			log
					.debug("At unCompressCacheItem method in EcogridTarArchive ojbect");
			extractTarFile(getAbsoluteFileName(), unCompressedCacheItemDir);
			unCompressedFileList = unCompressedCacheItemDir.list();
		}

	}

	/**
	 * This method will untar a given file to destination dir
	 * 
	 * @param tarFileName
	 *            String
	 * @param unTarDesDir
	 *            File
	 */
	public static void extractTarFile(String tarFileName, File unTarDesDir)
			throws Exception {
		TarArchive tar = null;
		if (tarFileName != null && unTarDesDir != null) {
			log.debug("In extractTarFile method ");
			log.debug("The source tar file is " + tarFileName);
			log.debug("The un tar destionation dir is " + unTarDesDir);
			try {
				unTarDesDir.mkdirs();
				tar = new TarArchive(new FileInputStream(tarFileName));
				tar.extractContents(unTarDesDir);
			} catch (Exception ex) {
				log.debug("The exception in extractTarFile 1", ex);
				throw ex;
			} finally {
				if (tar != null) {
					try {
						tar.closeArchive();
					} catch (IOException ioe) {
						// don't know what to do now
						log.debug("The exception in extractTarFile 2", ioe);
					}
				}
			}
		}
	}

}