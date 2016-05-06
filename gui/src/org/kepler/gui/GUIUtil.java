/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

import java.awt.Component;
import java.awt.Window;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.util.MessageHandler;

/**
 * A class to hold useful GUI utility methods.
 */
public class GUIUtil {

	/**
	 * The getParentWindow method will return the Window that contains the
	 * supplied Component.
	 * 
	 * @param source
	 * 	
	 */
	public static Window getParentWindow(Component source) {
	    return (Window)_getParent(source, Window.class);
	}
	
	/** Get the parent TableauFrame, if any, for a Component. */
	public static TableauFrame getParentTableauFrame(Component source) {
	    return (TableauFrame)_getParent(source, TableauFrame.class);
	}
	
	/** Get the parent of a Component. */
	private static Component _getParent(Component source, Class<?> clazz) {
		int recursionCount = 0;
		while (!clazz.isInstance(source)) {
			source = (Component) source.getParent();
			recursionCount++;
			if (source == null || recursionCount > 20) {
				try {
					MessageHandler.error("Window containing Component ("
							+ source.toString() + ") not found");
				} catch (Exception ce) {
					// Do nothing
				}
				return null;
			}
		}
		return (Window) source;
	}

}
