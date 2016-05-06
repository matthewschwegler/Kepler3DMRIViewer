/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jones $'
 * '$Date: 2011-09-09 01:42:06 -0700 (Fri, 09 Sep 2011) $' 
 * '$Revision: 28480 $'
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
import java.net.URL;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

/**
 * This action imports a kar file to the system
 * 
 * @author Chad Berkley
 * @created 10/4/2006
 */
public class AboutAction extends FigureAction {

	// ////////////////////////////////////////////////////////////////////////////
	// LOCALIZABLE RESOURCES - NOTE that these default values are later
	// overridden by values from the uiDisplayText resourcebundle file
	// ////////////////////////////////////////////////////////////////////////////

	private static String DISPLAY_NAME = StaticResources.getDisplayString(
			"actions.actor.displayName", "About");

	private TableauFrame parent;

	private final static Log log = LogFactory.getLog(ActorDialogAction.class
			.getName());
	private final static boolean isDebugging = log.isDebugEnabled();
	private static ImageIcon LARGE_ICON = null;
	private static KeyStroke ACCELERATOR_KEY = null;
	private static String TOOLTIP = StaticResources.getDisplayString(
			"actions.actor.tooltip", "About Kepler");

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public AboutAction(TableauFrame parent) {
		super("");
		this.parent = parent;

		this.putValue(Action.NAME, DISPLAY_NAME);
		this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
		this.putValue("tooltip", TOOLTIP);
		this.putValue(GUIUtilities.ACCELERATOR_KEY, ACCELERATOR_KEY);
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {

		// must call this first...
		super.actionPerformed(e);
		ConfigurationProperty commonProperty = ConfigurationManager.getInstance().getProperty(
						ConfigurationManager.getModule("common"));
		String splashname = commonProperty.getProperty("splash.image").getValue();
		final URL splashURL = ClassLoader.getSystemClassLoader().getResource(splashname);
		SplashWindow.splash(splashURL, true);
	}
}