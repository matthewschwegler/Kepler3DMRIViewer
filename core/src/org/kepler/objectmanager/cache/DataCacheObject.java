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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents an item in the cache
 */
public abstract class DataCacheObject extends CacheObject implements Runnable,
		Serializable {
	/**
	 * Description of the Field
	 */
	protected final static int CACHE_BUSY = 1;
	/**
	 * Description of the Field
	 */
	protected final static int CACHE_ERROR = 2;
	/**
	 * Description of the Field
	 */
	protected final static int CACHE_COMPLETE = 3;
	/**
	 * Description of the Field
	 */
	protected final static int CACHE_EMPTY = 4;

	/**
	 * Description of the Field
	 */
	private transient Thread mThread = null;

	/**
	 * Description of the Field
	 */
	private static Log log;
	private static boolean isDebugging;

	static {
		log = LogFactory
				.getLog("org.kepler.objectmanager.cache.DataCacheObject");
		isDebugging = log.isDebugEnabled();
	}

	private final static String cachedatapath = CacheManager.cachePath
			+ File.separator + "cachedata" + File.separator;

	/**
	 * Description of the Field
	 */
	private Date mCreatedDate = new Date();
	/**
	 * Description of the Field
	 */
	private String mResourceName = null;
	/**
	 * Absolute filename of data file.
	 */
	private String mLocalFileName = null;

	/**
	 * Description of the Field
	 */
	private transient Vector mListeners = new Vector();

	/**
	 * Description of the Field
	 */
	private int mStatus = CACHE_EMPTY;

	/**
	 * Constructor
	 */
	public DataCacheObject() {
	}

	/**
	 *@param createdDate
	 *            The mCreatedDate to set.
	 */
	public final void setCreatedDate(Date createdDate) {
		mCreatedDate = createdDate;
	}

	/**
	 *@param localFileName
	 *            The mLocalFileName to set.
	 */
	public final void setAbsoluteFileName(String localFileName) {
		mLocalFileName = localFileName;
	}

	/**
	 *@param localFileName
	 *            The mLocalFileName to set.
	 */
	public final void setBaseFileName(String localFileName) {
		mLocalFileName = cachedatapath + localFileName;
	}

	/**
	 *@param resourceName
	 *            The mResourceName to set.
	 */
	public final void setResourceName(String resourceName) {
		mResourceName = resourceName;
	}

	/**
	 * 
	 */
	public final Object getObject() {
		return new File(mLocalFileName);
	}

	public final File getFile() {
		return (File) getObject();
	}

	/**
	 * Return the data as an InputStream. The data is read from the cached file
	 * on disk, so this method assumes the cache item has already been
	 * retrieved. If not, or if there is an error, then the return value will be
	 * null.
	 * 
	 *@return InputStream representing the data
	 */
	public final InputStream getDataInputStream() {
		BufferedInputStream bis = null;

		if (mLocalFileName != null && mLocalFileName.length() > 0) {
			try {
				File file = getFile();

				if (file != null && file.exists()) {
					FileInputStream fis = new FileInputStream(file);

					if (fis != null) {
						bis = new BufferedInputStream(fis);
					}
				}
			} catch (Exception e) {
				System.err.println(e);
			}
		} else {
			log.debug("loadData - mLocalFileName was null: \n  " + super.getName()
					+ "  \n" + mResourceName + "  \n" + mLocalFileName);
		}
		return bis;
	}

	/**
	 * Returns whether it is in empty state
	 * 
	 *	 */
	public final boolean isEmpty() {
		return mStatus == CACHE_EMPTY;
	}

	/**
	 * Return the status of getting the data
	 * 
	 * 
	 *	 */
	public final boolean isReady() {
		return mStatus == CACHE_COMPLETE;
	}

	/**
	 * Returns whether it is busy getting the data
	 * 
	 *	 */
	public final boolean isBusy() {
		return mStatus == CACHE_BUSY;
	}

	/**
	 * Returns whether it is in error state
	 * 
	 *	 */
	public final boolean isError() {
		return mStatus == CACHE_ERROR;
	}

	/**
	 *@return Returns the mCreatedDate.
	 */
	public final Date getCreatedDate() {
		return mCreatedDate;
	}

	/**
	 *@return Returns the mLocalFileName.
	 */
	public final String getAbsoluteFileName() {
		return mLocalFileName;
	}

	/**
	 *@return Returns the mLocalFileName.
	 */
	public final String getBaseFileName() {
		int inx = mLocalFileName.lastIndexOf(File.separator);

		if (inx > -1) {
			return mLocalFileName.substring(inx + 1);
		}
		return mLocalFileName;
	}

	/**
	 *@return Returns the mResourceName.
	 */
	public final String getResourceName() {
		return mResourceName;
	}

	/**
	 *@return Returns the mStatus.
	 */
	protected final int getStatus() {
		return mStatus;
	}

	/**
	 * Add a listenero
	 * 
	 *@param aListener
	 *            the listener to add
	 */
	public final void addListener(DataCacheListener aListener) {
		// If the listener is null, don't do anything.
		if (aListener == null) {
			return;
		}
		// If the object is already finished, then just notify.
		if (isReady() || isError()) {
			notifyOne(aListener);
			return;
		}
		// Otherwise, we need to add the listener to the list
		synchronized (mListeners) {
			mListeners.addElement(aListener);
		}
	}

	/**
	 * Removes a listener
	 * 
	 *@param aListener
	 *            the listener to remove
	 */
	public final void removeListener(DataCacheListener aListener) {
		if (aListener == null) {
			return;
		}
		synchronized (mListeners) {
			mListeners.removeElement(aListener);
		}
	}

	/**
	 * Removes all the listeners
	 */
	public final void removeAllListeners() {
		synchronized (mListeners) {
			mListeners.clear();
		}
	}

	/**
	 * Notifies the listener that the "getting" of the data has completed
	 */
	public final void notifyListeners() {
		List localcopy;
		synchronized (mListeners) {
			localcopy = (List) mListeners.clone();
		}
		for (Iterator i = localcopy.iterator(); i.hasNext();) {
			DataCacheListener l = (DataCacheListener) i.next();
			notifyOne(l);
		}
	}

	private final void notifyOne(DataCacheListener l) {
		l.complete(this);
	}

	/**
	 * Refreshes the data from the original source
	 * 
	 *@param aListener
	 */
	public final void refresh(DataCacheListener aListener) {
		clear();
		// System.err.println("Refresh Name "+mName +
		// "has new file name"+mLocalFileName);
		addListener(aListener);
		mCreatedDate = new Date();
		start();
	}

	/**
	 * Description of the Method
	 */
	public final void reset() {
		clear();
		mStatus = CACHE_EMPTY;
		mLocalFileName = null;
	}

	/**
	 * Clear the dat from this cache item
	 */
	public final void clear() {
		if (mLocalFileName != null && mLocalFileName.length() > 0) {
			File file = new File(mLocalFileName);

			if (file != null && file.exists()) {
				file.delete();
			}
		}
		removeAllListeners();
	}

	/**
	 * Abstract method for actually getting the data while in the thread
	 * 
	 * @return return the new status.
	 */
	public abstract int doWork();

	/**
	 * return a string representation of this datacacheobject
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		sb.append("classname = " + getClass().getName() + "\n");
		sb.append("name = " + super._name + "\n");
		// sb.append("resname = " + (mCreatedDate != null ?
		// mCreatedDate.getTime() : 0) + "\n");
		sb.append("resname = " + mResourceName + "\n");
		sb.append("date = "
				+ (mCreatedDate != null ? mCreatedDate.getTime() : 0) + "\n");
		sb.append("fileName = " + mLocalFileName + "\n");
		sb.append("}");
		return sb.toString();
	}

	// ----------------------------------------------------------------
	// -- Runnable Interface
	// ----------------------------------------------------------------
	/**
	 * Description of the Method
	 */
	public void start() {
		if (mThread == null) {
			mThread = new Thread(this);
			mThread.setPriority(Thread.MIN_PRIORITY);
			mThread.setName(super._name);
			mThread.start();
		}
	}

	public void stop() {
		if (mThread != null) {
			mThread.interrupt();
		}
	}

	/**
	 */
	public void run() {
		if (!isReady()) {
			mStatus = CACHE_BUSY;

			if (mThread.isInterrupted()) {
				mStatus = CACHE_ERROR;
			} else {
				mStatus = doWork();
			}
		}
		if (mThread.isInterrupted()) {
			mStatus = CACHE_ERROR;
		}

		log.debug("run - Done With Work.");
		notifyListeners();

		try {
			CacheManager.getInstance().updateObject(this);
		} catch (CacheException e) {
			log.error("CacheException occurred during run", e);
		}
		mThread = null;
	}

	/**
	 * Custom deserialization method. Need to initialize mListeners to empty
	 * vector.
	 * 
	 * @param ois
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream ois) throws IOException,
			ClassNotFoundException {
		ois.defaultReadObject();
		mListeners = new Vector();
	}

}