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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * Class that represents an object in the CacheManager. This class should be
 * extended by each type of object that wants to control its own lifecycle
 * events and serialization events.
 */
public class RawDataCacheObject extends CacheObject {
	private File datafile;

	public RawDataCacheObject() {
		super();
		datafile = null;
	}

	/**
	 * construct a new CacheObject
	 */
	public RawDataCacheObject(String name, KeplerLSID lsid) {
		super(name, lsid);
		datafile = new File(
				CacheManager.cachePath + File.separator + "RawData", lsid
						.createFilename());
	}

	/**
	 * construct a new CacheObject with a given filename.
	 */
	RawDataCacheObject(String name, KeplerLSID lsid, String filename) {
		super(name, lsid);
		datafile = new File(filename);
	}

	/**
	 * This returns a file object pointing to the data file. You'll need to cast
	 * the object to a File to use it.
	 */
	public Object getObject() {
		return datafile;
	}

	/**
	 * set the data file that is associated with this RawDataCacheObject
	 */
	public void setData(InputStream is) throws IOException {
		FileOutputStream fos = new FileOutputStream(datafile);
		CacheUtil.writeInputStreamToOutputStream(is, fos);
	}

	/**
	 * return the data as a stream
	 */
	public InputStream getDataAsStream() throws CacheException {
		try {
			FileInputStream fis = new FileInputStream(datafile);
			return fis;
		} catch (Exception e) {
			throw new CacheException("Could not get the data stream for "
					+ "RawDataCacheObject " + getLSID().toString() + " : "
					+ e.getMessage());
		}
	}

	/**
	 * call back for when this object is added to the cache
	 */
	public void objectAdded() {
		// System.out.println("object " + lsid.toString() + " added");
	}

	/**
	 * call back for when this object is removed by the user
	 */
	public void objectRemoved() {
		// System.out.println("object " + lsid.toString() + " removed");
	}

	/**
	 * call back for when this object is purged by CacheManager
	 */
	public void objectPurged() {
		// System.out.println("object " + lsid.toString() + " purged");
	}

	/**
	 * deserialize this object
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		StringBuffer sb = new StringBuffer();
		byte[] b = new byte[1024];
		int numread = in.read(b, 0, 1024);
		while (numread != -1) {
			sb.append(new String(b, 0, numread));
			numread = in.read(b, 0, 1024);
		}
		String serialStr = sb.toString();
		_name = serialStr.substring(serialStr.indexOf("<name>") + 6,
				serialStr.indexOf("</name>"));
		try {
			_lsid = new KeplerLSID(serialStr.substring(serialStr
					.indexOf("<lsid>") + 6, serialStr.indexOf("</lsid>")));
		} catch (Exception e) {
			throw new IOException("Could not create kepler lsid: "
					+ e.getMessage());
		}
		this.datafile = new File(serialStr.substring(serialStr
				.indexOf("<filename>") + 10, serialStr.indexOf("</filename>")));
	}

	/**
	 * serialize this object
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		String serialStr = "<name>" + getName() + "</name><lsid>" + _lsid.toString()
				+ "</lsid><filename>" + datafile.getAbsolutePath()
				+ "</filename>";
		byte[] b = serialStr.getBytes();
		out.write(b, 0, b.length);
		out.flush();
	}
}