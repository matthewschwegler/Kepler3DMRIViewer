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

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

/**
 * This action opens the Actor Tabbed Dialog.
 * 
 *@author Matthew Brooke
 *@since 26 February 2006
 */
public class ActorDialogAction extends FigureAction {

	// ////////////////////////////////////////////////////////////////////////////
	// LOCALIZABLE RESOURCES - NOTE that these default values are later
	// overridden by values from the uiDisplayText resourcebundle file
	// ////////////////////////////////////////////////////////////////////////////

	private static String DISPLAY_NAME = StaticResources.getDisplayString(
			"actions.actor.displayName", "Configure");
	private static String TOOLTIP = StaticResources.getDisplayString(
			"actions.actor.tooltip", "Change Settings for Actor");
	private static ImageIcon LARGE_ICON = null;
	private static KeyStroke ACCELERATOR_KEY = null;

	// = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().
	// getMenuShortcutKeyMask());
	// //////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public ActorDialogAction(TableauFrame parent) {
		super("");
		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"ActorDialogAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}
		this.parent = parent;

		this.putValue(Action.NAME, DISPLAY_NAME);
		this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
		this.putValue("tooltip", TOOLTIP);
		this.putValue(GUIUtilities.ACCELERATOR_KEY, ACCELERATOR_KEY);
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {

		// must call this first...
		super.actionPerformed(e);
		// ...before calling this:
		NamedObj target = super.getTarget();

		actorDialog = new ActorDialog(parent, target);
		actorDialog.setVisible(true);
	}

	private TableauFrame parent;
	private TabbedDialog actorDialog;

	private static final Log log = LogFactory.getLog(
			ActorDialogAction.class.getName());

	private static final boolean isDebugging = log.isDebugEnabled();
}