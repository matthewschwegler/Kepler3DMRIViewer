/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 13:35:22 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31480 $'
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

/**
 * 
 */
package org.kepler.objectmanager.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.DotKeplerManager;

/**
 * @author Aaron Schultz
 */
public class LibSearchConfiguration implements Serializable {
	private static final Log log = LogFactory.getLog(LibSearchConfiguration.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
	private Vector<Integer> _searchTypes;

	private String _saveFileName;
	
	public LibSearchConfiguration () {
		
		// Set up file name for serializing to disk
		File modDir = DotKeplerManager.getInstance()
				.getTransientModuleDirectory("core");
		if (modDir != null) {
			_saveFileName = modDir.toString();
		} else {
			_saveFileName = DotKeplerManager.getInstance().getCacheDirString();
		}
		if (!_saveFileName.endsWith(File.separator)) {
			_saveFileName += File.separator;
		}
		_saveFileName += "ComponentSearchConfig";

		init();
	}
	
	private void init() {
		File saveFile = new File(_saveFileName);

		if (saveFile.exists()) {
			if (isDebugging) {
				log.debug("Save file exists: " + saveFile.toString());
			}

			try {
				InputStream is = null;
				ObjectInput oi = null;
				try {
					is = new FileInputStream(saveFile);
				    oi = new ObjectInputStream(is);
				    Object newObj = oi.readObject();
	                _searchTypes = (Vector<Integer>) newObj;
	                return;
				} finally {
				    if(oi != null) {
				        oi.close();
				    }
				    if(is != null) {
				    	is.close();
				    }
				}

			} catch (Exception e1) {
				// problem reading file, try to delete it
				log.warn("Exception while reading save file: "
						+ e1.getMessage());
				try {
					saveFile.delete();
				} catch (Exception e2) {
					log.warn("Unable to delete save file: "
							+ e2.getMessage());
				}
			}
		} else {
			setDefaults();
		}
		
	}
	
	public void addSearchType(int type) {
		_searchTypes.add(new Integer(type));
	}
	
	public void removeSearchType(int type) {
		_searchTypes.remove(new Integer(type));
	}
	
	public boolean contains(int type) {
		if (_searchTypes.contains(new Integer(type))) {
			return true;
		}
		return false;
	}
	
	public Vector<Integer> getSearchTypes() {
		return _searchTypes;
	}
	
	private void setDefaults() {
		_searchTypes = new Vector<Integer>(2);
		_searchTypes.add(new Integer(LibSearch.TYPE_NAME));
		_searchTypes.add(new Integer(LibSearch.TYPE_ONTCLASSNAME));
	}

	public void serializeToDisk() {
		if (isDebugging)
			log.debug("serializeToDisk()");

		File saveFile = new File(_saveFileName);
		if (saveFile.exists()) {
			if (isDebugging)
				log.debug("delete " + saveFile);
			saveFile.delete();
		}
		try {
		    ObjectOutputStream oos = null;
		    try {
    			OutputStream os = new FileOutputStream(saveFile);
    			oos = new ObjectOutputStream(os);
    			oos.writeObject(_searchTypes);
    			oos.flush();
    			if (isDebugging) {
    				log.debug("wrote " + saveFile);
    			}
		    } finally {
		        if(oos != null) {
		            oos.close();
		        }
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
