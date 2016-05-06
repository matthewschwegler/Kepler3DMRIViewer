/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-10-09 17:44:40 -0700 (Tue, 09 Oct 2012) $' 
 * '$Revision: 30846 $'
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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.popups.LibraryPopupListener;

public class AnnotatedPTreePopupListener extends MouseAdapter {

	private static final Log log = LogFactory.getLog(LibraryPopupListener.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
	// on the Mac, popups are triggered on mouse pressed, while
	// mouseReleased triggers them on the PC; use the trigger flag to
	// record a trigger, but do not show popup until the
	// mouse released event
	protected boolean _trigger = false;

	protected AnnotatedPTree _aptree;

	public AnnotatedPTreePopupListener(AnnotatedPTree aptree) {
		_aptree = aptree;
	}

	/**
	 * Description of the Method
	 * 
	 *@param e
	 *            Description of the Parameter
	 */
	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger()) {
			_trigger = true;
		}
	}

	/**
	 * Description of the Method
	 * 
	 *@param e
	 *            Description of the Parameter
	 */
	public void mouseReleased(MouseEvent e) {
		maybeShowPopup(e);
	}
	private void maybeShowPopup(MouseEvent e) {}
}
