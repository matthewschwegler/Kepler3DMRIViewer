/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2010-10-13 12:22:12 -0700 (Wed, 13 Oct 2010) $' 
 * '$Revision: 26060 $'
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

package org.kepler.gui.kar;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;
import diva.gui.GUIUtilities;

/**
 * This action saves an actor as a kar file. It is called from right clicking on
 * an actor.
 * 
 *@author Aaron Schultz
 */
public class ExportActorArchiveAction extends ExportArchiveAction {

	private static final Log log = LogFactory
			.getLog(ExportActorArchiveAction.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private static String DISPLAY_NAME = StaticResources.getDisplayString(
			"actions.actor.displayName", "Save Archive (KAR)...");
	private static String TOOLTIP = StaticResources.getDisplayString(
			"actions.actor.tooltip", "Save a KAR file archive.");

	private static ImageIcon LARGE_ICON = null;
	private static KeyStroke ACCELERATOR_KEY = null;

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public ExportActorArchiveAction(TableauFrame parent) {
		super(parent);
		
		// note: initialize method is called in super class constructor
	}

	/**
	 * Override the initialize method to change the behavior of the constructor.
	 */
	protected void initialize() {

		this.putValue(Action.NAME, DISPLAY_NAME);
		this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
		this.putValue("tooltip", TOOLTIP);
		this.putValue(GUIUtilities.ACCELERATOR_KEY, ACCELERATOR_KEY);

		this.setSingleItemKAR(true);
		setRefreshFrameAfterSave(false);
		setMapKARToCurrentFrame(false);
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public boolean handleAction(ActionEvent e) {

		// Get the NamedObj
		NamedObj target = super.getTarget();
		
		if (target instanceof ComponentEntity) {

			// check it for LSID, Name, and SemanticTypes
			boolean continueExport = checkSingleObject(target, true);
			if (!continueExport) {
				return false;
			}
	
			_savekar.addSaveInitiator((ComponentEntity)target);

			return true;
		}
		return false;
	}
}