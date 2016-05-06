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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.NamedObj;

/**
 * Actor tabbed dialog
 * 
 * @author Matthew Brooke
 * @since 27 February 2006
 */
public class ActorDialog extends TabbedDialog {

	private static final boolean isModal = StaticResources.getBoolean(
			"diaogs.all.isModal", true);

	private static String TITLE = StaticResources.getDisplayString(
			"dialogs.actor.titleBar", "");
	private static String GENERAL = StaticResources.getDisplayString(
			"dialogs.actor.generalTab", "");
	private static String PARAMS = StaticResources.getDisplayString(
			"dialogs.actor.parametersTab", "");
	private static String PORTS = StaticResources.getDisplayString(
			"dialogs.actor.portsTab", "");
	private static String ANNOT = StaticResources.getDisplayString(
			"dialogs.actor.annotationsTab", "");
	private static String UNITS = StaticResources.getDisplayString(
			"dialogs.actor.unitsTab", "");

	public ActorDialog(TableauFrame frame, NamedObj actor) {

		super(frame, TITLE, isModal);
		this._actor = actor;
		init();
		pack();
	}

	private void init() {
		generalTab = new DialogGeneralTab(_actor,
				AbstractDialogTab.ACTOR_TARGET_TYPE, _frame);
		_addTab(generalTab, GENERAL);
		paramsTab = new DialogParametersTab(_actor,
				AbstractDialogTab.ACTOR_TARGET_TYPE, _frame);
		_addTab(paramsTab, PARAMS);
		portsTab = new DialogPortsTab(_actor,
				AbstractDialogTab.ACTOR_TARGET_TYPE, _frame);
		_addTab(portsTab, PORTS);
		annotTab = new DialogAnnotationsTab(_actor,
				AbstractDialogTab.ACTOR_TARGET_TYPE, _frame);
		_addTab(annotTab, ANNOT);
		unitsTab = new DialogUnitsTab(_actor,
				AbstractDialogTab.ACTOR_TARGET_TYPE, _frame);
		_addTab(unitsTab, UNITS);
	}

	private static final Log log = LogFactory.getLog(
			ActorDialog.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	private DialogGeneralTab generalTab;
	private DialogParametersTab paramsTab;
	private DialogPortsTab portsTab;
	private DialogAnnotationsTab annotTab;
	private DialogUnitsTab unitsTab;
	private final NamedObj _actor;

	// //////////////////////////////////////////////////////////////////////////////

	/**
	 * main for testing
	 * 
	 * @param param
	 *            String[]
	 */
	public static void main(String[] param) {
		StaticGUIResources.setLookAndFeel();

		ActorDialog actorDialog = new ActorDialog(null, null);
		actorDialog.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
		});
		actorDialog.setLocation(200, 200);
		actorDialog.setVisible(true);
	}
}