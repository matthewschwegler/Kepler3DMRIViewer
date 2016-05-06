/**
 *
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 * FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY
 * OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.kepler.modulemanager;

import java.io.InputStream;

/**
 * This interface handles event passing from the ModuleDownloader to 
 * its clients.
 * @author berkley
 *
 */
public interface ModuleManagerEventListener {
	/**
	 * update the listener on the status of the download
	 * @param totalSize the total size of the transfer.  -1 if not known
	 * @param bufferSize the size of each buffered transfer.  -1 if this is not a 
	 *        buffered transfer
	 * @param readCount the number of times bufferSize has been read.
	 * @param is the inputStream being read
	 */
	public void updateProgress(int totalSize, int bufferSize, int readCount,
			InputStream is);

	/**
	 * update the listener when a download ends
	 */
	public void downloadEnd();

	/**
	 * update the listener when a download begins
	 * @param totalSize the total size of the download.  -1 if unknown.
	 * @param moduleName the name of the module beginning to download
	 */
	public void downloadBegin(int totalSize, String moduleName);

	/**
	 * begin an unzip procedure
	 * @param totalSize total size of the zip file
	 * @param moduleName the name of the module unzipping
	 */
	public void unzipBegin(long totalSize, String moduleName);

	/**
	 * end an unzip procedure
	 */
	public void unzipEnd();

	/**
	 * update the progress on an unzip procedure
	 * @param totalSize total size of the zip file
	 * @param bufferSize size of each buffer in the zip entry
	 * @param readCount number of bufferSize'd buffers that have been read
	 */
	public void unzipUpdateProgress(long totalSize, int bufferSize,
			int readCount);
}
