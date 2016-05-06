/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.kepler.gui;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * class to tell vergil whether scrollbars should be included in the canvas
 * pane. The property canvasNavigationModifier in the configuration can be used
 * to turn on the scrollbars.
 */
public class ScrollBarModifier {
	/** flag telling whether to show the scrollbars or not. */
	private boolean scrollbars = true;

	/**
	 * returns true if the scrollbars should be enabled
	 */
	public boolean getScrollBarModifier() {
		return scrollbars;
	}

	/**
	 * A factory that creates the library panel for the editors.
	 */
	public static class Factory extends CanvasNavigationModifierFactory {
		/**
		 * Create an factory with the given name and container.
		 * 
		 * @param container
		 *            The container.
		 * @param name
		 *            The name of the entity.
		 * @exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 * @exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * creates a ScrollBarModifier and returns it.
		 * 
		 * @param _libraryModel
		 *            the model containing the actor library
		 * @return A new LibraryPane that displays the library
		 */
		public ScrollBarModifier createScrollBarModifier() {
			return new ScrollBarModifier();
		}
	}
}