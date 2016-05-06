/*
 * Copyright (c) 2011-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-05-09 11:05:40 -0700 (Wed, 09 May 2012) $' 
 * '$Revision: 29823 $'
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

package org.kepler.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.kernel.util.NamedObj;

/**
 * This singleton class holds a map of all KeplerGraphFrame objects and their
 * associated NamedObj models.
 * 
 * @author Aaron Schultz
 */
public class ModelToFrameManager {

    // FIXME: This seems to be a duplicate of the Configuration? -cxh

	private static final Log log = LogFactory.getLog(ModelToFrameManager.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private Map<NamedObj, KeplerGraphFrame> modelToFrameMap;

	/**
	 * Empty constructor
	 */
	public ModelToFrameManager() {
		HashMap<NamedObj, KeplerGraphFrame> hashMap = new HashMap<NamedObj, KeplerGraphFrame>();
		modelToFrameMap = Collections.synchronizedMap(hashMap);
		if (isDebugging) {
			log.debug("Constructing ModelToFrameManager");
		}
	}
	
	/** Get the frame for a model. */
	public KeplerGraphFrame getFrame(NamedObj model) {
	    return modelToFrameMap.get(model);
	}

	/**
	 * Map a NamedObj to a KeplerGraphFrame.
	 * 
	 * @param obj
	 * @param frame
	 */
	public void add(NamedObj obj, KeplerGraphFrame frame) {
		if (obj != null && frame != null) {
			modelToFrameMap.put(obj, frame);
			if (isDebugging) {
				log.debug("mapping NamedObj: " + obj.getName()
						+ " to KeplerGraphFrame: " + frame.getTitle());
			}
		}
	}

	/**
	 * Remove all mappings to the given KeplerGraphFrame.
	 * 
	 * @param frame
	 */
	public void remove(KeplerGraphFrame frame) {

		List<NamedObj> removeList = new ArrayList<NamedObj>();

		for (NamedObj obj : modelToFrameMap.keySet()) {
			KeplerGraphFrame kgf = modelToFrameMap.get(obj);
			if (kgf == frame) {
				removeList.add(obj);
			}
		}
		for (NamedObj removeObj : removeList) {
			modelToFrameMap.remove(removeObj);
			if (isDebugging) {
				log.debug("removing KeplerGraphFrame mapping of obj:"
						+ removeObj.getName());
			}
		}
	}
	
	/** Remove a model from the mapping. */
	public void removeModel(NamedObj model) {
	    modelToFrameMap.remove(model);
	}

	/** Return the number of open frames. 
	 *  XXX this is incorrect since multiple models may be
	 *  contained in the same frame.
	 */
	public int numberOfOpenFrames() {
	    return modelToFrameMap.size();
	}
	
	/**
	 * Print the map for debugging purposes.
	 */
	public void printDebugInfo() {
		System.out.println("ModelToFrameManager.printDebugInfo()");
		for (NamedObj key : modelToFrameMap.keySet()) {
			KeplerGraphFrame kgf = modelToFrameMap.get(key);
			System.out.println(key.getName() + " : " + kgf.getTitle());
		}
	}

	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static ModelToFrameManager getInstance() {
		return ModelToFrameManagerHolder.INSTANCE;
	}

	private static class ModelToFrameManagerHolder {
		private static final ModelToFrameManager INSTANCE = new ModelToFrameManager();
	}
}
