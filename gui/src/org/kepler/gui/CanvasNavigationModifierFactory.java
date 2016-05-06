/*
 * Copyright (c) 1997-2010 The Regents of the University of California.
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

import java.util.Iterator;

import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * This class allows the configuration option canvasNavigationModifier to be
 * passed via the moml configuration. The ScrollBarModifier class contains a
 * flag which determines if the scrollbars should be displayed or not. Other
 * such configuration options could also be added via this class.
 */
public class CanvasNavigationModifierFactory extends Attribute {

	/**
	 * Create a factory with the given name and container.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name.
	 * @exception IllegalActionException
	 *                If the container is incompatible with this attribute.
	 * @exception NameDuplicationException
	 *                If the name coincides with an attribute already in the
	 *                container.
	 */
	public CanvasNavigationModifierFactory(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * creates the ScrollBarModifier class and returns it
	 */
	public ScrollBarModifier createScrollBarModifier() {
		ScrollBarModifier modifier = null;
		Iterator factories = attributeList(
				CanvasNavigationModifierFactory.class).iterator();
		while (factories.hasNext() && modifier == null) {
			CanvasNavigationModifierFactory factory = (CanvasNavigationModifierFactory) factories
					.next();
			modifier = factory.createScrollBarModifier();
		}
		return modifier;
	}
}