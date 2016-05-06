/*
 * Copyright (c) 2010 The Regents of the University of California.
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

import ptolemy.actor.gui.TableauFrame;

/**
 * This interface is to be implemented by anyone wanting to add a tab to the
 * PreferencesFrame. The PreferencesTabFactory generates instances of
 * PreferencesTab.
 * 
 * @author Aaron Schultz
 * 
 */
public interface PreferencesTab {

	/**
	 * PreferencesTab usually will need access to the configuration of the
	 * Tableau that is opened in the BasicGraphFrame. This method will be called
	 * before calling initializeTab.
	 * 
	 * @param config
	 */
	public abstract void setParent(TableauFrame frame);

	/**
	 * Initialization of the tab should be done at some point after the
	 * constructor is called. The setConfiguration method is called before
	 * calling initializeTab() and this allows for easily adding new functions
	 * to this interface later on.
	 * 
	 * @throws Exception
	 */
	public abstract void initializeTab() throws Exception;

	/**
	 * This method should return the name of the tab which is used to label the
	 * tab in the PreferencesFrame.
	 */
	public abstract String getTabName();

	/**
	 * This method is called as the preferences frame is closed legitimately
	 * meaning that all changes to the preferences should be saved.
	 */
	public abstract void onClose();
	
	/**
	 * This method is called if the user presses the cancel button on the preferences frame.
	 * In this case all changes to the preferences should be ignored.
	 */
	public abstract void onCancel();

}
