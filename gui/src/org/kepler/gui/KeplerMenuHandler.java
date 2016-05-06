/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2011-03-18 19:24:12 -0700 (Fri, 18 Mar 2011) $' 
 * '$Revision: 27324 $'
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
import ptolemy.actor.gui.TopPack;
import ptolemy.gui.Top;

/**
 * adds the kepler menus to vergil
 */
public class KeplerMenuHandler implements TopPack {
	public MenuMapper menuMapper;

	/**
	 * pack the gui for Top t. alreadyCalled is true if pack has already been
	 * called at least once.
	 */
	public void pack(Top t, boolean alreadyCalled) {
		// super.pack() creates ptolemy JMenuBar, but won't
		// add it, since we will call hideMenuBar()
		// System.out.println("Using Kepler Menu Handler");
		menuMapper = new MenuMapper((TableauFrame) t);
		// Do this only once per instance, since there
		// may be multiple calls to pack()
		//if (!alreadyCalled && StaticResources.getBoolean("KEPLER_MENUS", false)) {
			// proceed only if we are using the Kepler-specific UI, and this
			// hasn't
			// been called before...

			// menuMapper is a Runnable - run it:
			//t.deferIfNecessary(menuMapper);
		//}
	}

	/**
	 * pass the menuMapper back to the calling object
	 */
	public Object getObject(Object identifier) {
		return menuMapper;
	}
}