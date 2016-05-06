/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2010-12-23 11:01:04 -0800 (Thu, 23 Dec 2010) $' 
 * '$Revision: 26600 $'
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

/**
 * A singleton class to keep track of whether or not the PreferencesFrame is
 * currently open or not.
 * 
 * @author Aaron Schultz
 */
public class PreferencesFrameTracker {
	private static class PreferencesFrameTrackerHolder {
		private static final PreferencesFrameTracker INSTANCE = new PreferencesFrameTracker();
	}

	/**
	 * A boolean that is true if the PreferencesFrame is open, and false if it
	 * is not.
	 */
	private boolean open = false;
	private PreferencesFrame pFrame = null;

	public PreferencesFrameTracker() {
	}

	public PreferencesFrame getPreferencesFrame() {
		return pFrame;
	}

	public boolean isOpen() {
		if (open && pFrame != null) {
			return true;
		} else {
			open = false;
			return false;
		}
	}

	public void setOpen(PreferencesFrame prefFrame) {
		this.pFrame = prefFrame;
		this.open = true;
	}

	public void setClosed() {
		open = false;
		pFrame = null;
	}

	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static PreferencesFrameTracker getInstance() {
		return PreferencesFrameTrackerHolder.INSTANCE;
	}

}