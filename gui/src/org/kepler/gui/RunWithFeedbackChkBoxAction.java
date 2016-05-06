/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2011-11-04 10:27:30 -0700 (Fri, 04 Nov 2011) $' 
 * '$Revision: 28870 $'
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
import java.util.Map;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

/**
 * This class provides an action to turn on or off animating a workflow
 * during execution. When animation is turned on, a checkbox appears
 * next to the text in the menu item.
 * 
 *@author Matthew Brooke
 *@since 27 January 2006
 */
public class RunWithFeedbackChkBoxAction extends FigureAction implements InitializableAction {

	// //////////////////////////////////////////////////////////////////////////////
	// Note that these are defaults - Instantiating code will
	// probably override these in a localizable manner

	private final String DISPLAY_NAME = "Run with Feedback";
	private final String TOOLTIP = "Show feedback when running workflow";
	private final ImageIcon LARGE_ICON = null;
	private final Integer MNEMONIC_KEY = new Integer(KeyEvent.VK_F);
	private final KeyStroke ACCELERATOR_KEY = null;
	// = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().
	// getMenuShortcutKeyMask());
	// //////////////////////////////////////////////////////////////////////////////

	// old PTII display names used for actionCommand
	// text, for backward compatibility
	private final String FEEDBACK_ON_PTII_DISPLAY_NAME = "Animate Execution";
	private final String FEEDBACK_OFF_PTII_DISPLAY_NAME = "Stop Animating";

	// NOTE - Keys in PTIIMenuActionsMap are all UPPERCASE
	private final String FEEDBACK_ON_PTIIMENUPATH = ("Debug->" + FEEDBACK_ON_PTII_DISPLAY_NAME)
			.toUpperCase();

	// NOTE - Keys in PTIIMenuActionsMap are all UPPERCASE
	private final String FEEDBACK_OFF_PTIIMENUPATH = ("Debug->" + FEEDBACK_OFF_PTII_DISPLAY_NAME)
			.toUpperCase();

	private JCheckBoxMenuItem jcbMenuItem;
	private Action feedbackOnAction;
	private Action feedbackOffAction;

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public RunWithFeedbackChkBoxAction(TableauFrame parent) {
		super("");
		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"RunWithFeedbackChkBoxAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}
		this.putValue(Action.NAME, DISPLAY_NAME);
		this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
		this.putValue(GUIUtilities.MNEMONIC_KEY, MNEMONIC_KEY);
		this.putValue("tooltip", TOOLTIP);
		this.putValue(GUIUtilities.ACCELERATOR_KEY, ACCELERATOR_KEY);
	}
	
	public boolean initialize(Map<String, Action> menuActionsMap) {
	    
	    // set the menu type to a checkbox so that MenuMapper will use
	    // a JCheckBoxMenuItem for our menu item.
		this.putValue(MenuMapper.MENUITEM_TYPE,
				MenuMapper.CHECKBOX_MENUITEM_TYPE);

		feedbackOnAction = (Action) menuActionsMap.get(FEEDBACK_ON_PTIIMENUPATH);
		feedbackOffAction = (Action) menuActionsMap.get(FEEDBACK_OFF_PTIIMENUPATH);

		/* This method gets called when MenuMapper is constructing the menu bar.
		 * The actions Debug->Animate Execution and Debug->Stop Animating may not be
		 * present for the type of window that is being constructed, e.g.,
		 * a documentation window, so do not check if they are null.
		 */
		/*
		if (feedbackOnAction == null || feedbackOffAction == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"RunWithFeedbackChkBoxAction constructor could not get actions for "
							+ FEEDBACK_ON_PTIIMENUPATH + " or "
							+ FEEDBACK_OFF_PTIIMENUPATH);
			iae.fillInStackTrace();
			throw iae;
		}
		*/
		
		// Both actions need to be present in order to use this action
		if(feedbackOnAction == null || feedbackOffAction == null) {
		    return false;
		}
		return true;
		
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {

	    /* Our menu item was executed, so make sure the corresponding Ptolemy
	     * actions were found.
	     */
		if (feedbackOnAction == null || feedbackOffAction == null) {
			if (isDebugging) {
				log
						.debug("RunWithFeedbackChkBoxAction.actionPerformed() - null Action(s):"
								+ "\n feedbackOnAction = "
								+ feedbackOnAction
								+ "\n feedbackOffAction = " + feedbackOffAction);
			}
			return;
		}

		// get the menu item so that we can see if animation is currently turned on
		if (jcbMenuItem == null) {
			jcbMenuItem = (JCheckBoxMenuItem) this
					.getValue(MenuMapper.NEW_JMENUITEM_KEY);
		}
		
		if (jcbMenuItem != null) {

			boolean isChecked = jcbMenuItem.getState();

			if (isChecked) {
			    
			    log.debug("FEEDBACK_ON");

				// NOTE - PTII uses ActionCommands for some listeners;
				// need to set this to PTII display name so it works...
				jcbMenuItem.setActionCommand(FEEDBACK_ON_PTII_DISPLAY_NAME);

				feedbackOnAction.actionPerformed(e);
			} else {
				log.debug("FEEDBACK_OFF");

				// NOTE - PTII uses ActionCommands for some listeners;
				// need to set this to PTII display name so it works...
				jcbMenuItem.setActionCommand(FEEDBACK_OFF_PTII_DISPLAY_NAME);

				feedbackOffAction.actionPerformed(e);
			}
		}
	}

	private static final Log log = LogFactory.getLog(
			RunWithFeedbackChkBoxAction.class.getName());

	private static final boolean isDebugging = log.isDebugEnabled();

}