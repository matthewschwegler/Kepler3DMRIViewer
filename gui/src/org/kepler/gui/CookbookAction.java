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
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.util.FileUtilities;
import diva.gui.GUIUtilities;

/**
 * This action opens the Kelper Cookbook, which is an html file at the
 * classpath-relative location defined by COOKBOOK_URL_STR
 * 
 *@author Matthew Brooke
 *@since 27 January 2006
 */
public class CookbookAction extends AbstractAction {

	// //////////////////////////////////////////////////////////////////////////////
	// Note that these are defaults - Instantiating code will
	// probably override these in a localizable manner

	private final String DISPLAY_NAME = "Cookbook";
	private final String TOOLTIP = "Open Kepler Cookbook";
	private final ImageIcon LARGE_ICON = null;
	private final Integer MNEMONIC_KEY = new Integer(KeyEvent.VK_C);
	private final KeyStroke ACCELERATOR_KEY = null;
	// = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().
	// getMenuShortcutKeyMask());
	// //////////////////////////////////////////////////////////////////////////////

	// NOTE - $CLASSPATH part is needed by
	// ptolemy.util.FileUtilities.nameToURL()
	private final String COOKBOOK_URL_STR = "$CLASSPATH/ptolemy/configs/kepler/cookbook.html";

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public CookbookAction(TableauFrame parent) {
		super();
		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"CookbookAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}
		this.parent = parent;

		this.putValue(Action.NAME, DISPLAY_NAME);
		this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
		this.putValue(GUIUtilities.MNEMONIC_KEY, MNEMONIC_KEY);
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

		try {
			URL cookbookUrl = FileUtilities.nameToURL(COOKBOOK_URL_STR, null,
					getClass().getClassLoader());
			parent.getConfiguration().openModel(null, cookbookUrl,
					cookbookUrl.toExternalForm());
		} catch (Exception ex) {
			if (isDebugging) {
				log
						.error("exception trying to open the cookbook. \nPlease check "
								+ "the classpath-relative location: "
								+ COOKBOOK_URL_STR);
			}
		}
	}

	private TableauFrame parent;

	private static final Log log = LogFactory.getLog(
			CookbookAction.class.getName());

	private static final boolean isDebugging = log.isDebugEnabled();

}