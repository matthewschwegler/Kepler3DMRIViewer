/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 15:15:22 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31481 $'
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
package org.kepler.gui.component;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.DotKeplerManager;

/**
 * @author Aaron Schultz
 * 
 */
public class ShowFolders {
	private static final Log log = LogFactory.getLog(ShowFolders.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * location of the persistent file on disk
	 */
	private String _saveFileName;

	/**
	 * whether or not to show folders in the component tab
	 */
	private Boolean _showFolders = null;

	/**
	 * Constructor
	 */
	public ShowFolders() {

		// Set up file name for storing boolean
		File modDir = DotKeplerManager.getInstance()
				.getTransientModuleDirectory("gui");
		if (modDir != null) {
			_saveFileName = modDir.toString();
		} else {
			_saveFileName = DotKeplerManager.getDotKeplerPath();
		}
		if (!_saveFileName.endsWith(File.separator)) {
			_saveFileName += File.separator;
		}
		_saveFileName += "ShowFolders";

		init();

	}

	/**
	 * Load saved state
	 */
	private void init() {
		File saveFile = new File(_saveFileName);

		if (saveFile.exists()) {
			if (isDebugging) {
				log.debug("ShowFolders exists: " + saveFile.toString());
			}

			try {
				InputStream is = null;
			    ObjectInput oi = null;
    			try {
    				is = new FileInputStream(saveFile);
    				oi = new ObjectInputStream(is);
    				Object newObj = oi.readObject();    
    				_showFolders = (Boolean) newObj;
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
				log.warn("Exception while reading localSaveRepoFile: "
						+ e1.getMessage());
				try {
					saveFile.delete();
				} catch (Exception e2) {
					log.warn("Unable to delete localSaveRepoFile: "
							+ e2.getMessage());
				}
			}
		} else {
			// default to true
			set(true);
		}

	}

	/**
	 * Set whether or not folders should be shown in the Component Tab.
	 * 
	 * @param showFolders
	 *            true if the folders should show up in the component tab
	 */
	public void set(Boolean showFolders) {
		if (_showFolders != null && showFolders.equals(_showFolders)) {
			// do nothing
		} else {
			_showFolders = showFolders;
			serializeToDisk();
		}
	}

	/**
	 * Set whether or not folders should be shown in the Component Tab.
	 * 
	 * @param showFolders
	 *            true if the folders should show up in the component tab
	 */
	public void set(boolean showFolders) {
		set(new Boolean(showFolders));
	}

	/**
	 * 
	 * @return true if the folders should show up in the component tab
	 */
	public boolean show() {
		return _showFolders.booleanValue();
	}

	/**
	 * Save state to the module read/write area as a Java serialized object
	 */
	private void serializeToDisk() {
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
    			oos.writeObject(_showFolders);
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
