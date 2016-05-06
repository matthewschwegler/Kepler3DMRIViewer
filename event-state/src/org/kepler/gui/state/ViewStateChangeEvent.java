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

package org.kepler.gui.state;

import java.awt.Component;

import ptolemy.kernel.util.NamedObj;

/**
 * An event that represents a state change in the application, showing both the
 * state that changed and the source of the state change.
 */
public class ViewStateChangeEvent extends StateChangeEvent {

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	// PUBLIC STATIC STATE CHANGE EVENT TYPE CONSTANTS
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	public static String SHOW_VIEW = "SHOW_VIEW";

	// TODO: add more here

	private String viewName;

	/**
	 * Construct a new StateChange event.
	 * @param source
	 * @param changedState
	 * @param reference
	 * @param viewName
	 */
	public ViewStateChangeEvent(Component source, String changedState,
			NamedObj reference, String viewName) {
		super(source, changedState, reference);
		this.viewName = viewName;
	}

	/**
	 * Get the value of the viewName for this event.
	 * 
	 * @return the String value for the viewName
	 */
	public String getViewName() {
		return viewName;
	}

}