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

import java.awt.Component;

import javax.swing.Box;
import javax.swing.JScrollPane;

import ptolemy.actor.gui.DialogTableau;
import ptolemy.actor.gui.PortConfigurerDialog;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.NamedObj;

/**
 * A JTabbedPane tab to be added to a TabbedDialog object. This particular pane
 * shows the "Ports" settings for the object being configured
 * 
 * @author Matthew Brooke
 * @since 27 February 2006
 */
public class DialogPortsTab extends AbstractDialogTab {

	public DialogPortsTab(NamedObj target, String targetType, TableauFrame frame) {
		super(target, targetType, frame);
	}

	/**
	 * Validate the user-editable values associated with this tab
	 * 
	 * @return boolean - true if user values validate correctly; false otherwise
	 */
	public boolean validateInput() {
		/** @todo - FIXME - needs to be implemented */
		return true;
	}

	/**
	 * Save the user-editable values associated with this tab
	 */
	public void save() {
		/** @todo - FIXME - needs to be implemented */
	}

	/**
	 * get the Component that will be displayed in the NORTH section of the
	 * BorderLayout. Note that if the dialog is resizable, this Component will
	 * need to stretch along the x axis, while retaining its aesthetic qualities
	 * 
	 * @return Component
	 */
	protected Component getTopPanel() {
		Box topPanel = Box.createHorizontalBox();
		/** @todo - FIXME - needs to be implemented */
		return topPanel;
	}

	/**
	 * get the Component that will be displayed in the CENTER section of the
	 * BorderLayout. Note that if the dialog is resizable, this Component will
	 * need to stretch along both the x <em>and</em> y axes, while retaining its
	 * aesthetic qualities
	 * 
	 * @return Component
	 */
	protected Component getCenterPanel() {
		Box centerPanel = Box.createHorizontalBox();

		/** @todo - FIXME - needs to be implemented */
		//
		// SEE ptolemy.actor.gui.PortConfigurerDialog
		// and ptolemy.vergil.kernel.PortDialogFactory
		//
		DialogTableau dialogTableau = DialogTableau.createDialog(_frame,
				TabbedDialog.getConfiguration(), _frame.getEffigy(),
				PortConfigurerDialog.class, (Entity) _target);

		dialogTableau.getFrame().setVisible(false);
		Component[] panels = dialogTableau.getFrame().getContentPane()
				.getComponents();

		for (int i = 0; i < panels.length; i++) {
			if (panels[i] instanceof JScrollPane) {
				centerPanel.add(panels[i]);
				break;
			}
		}

		return centerPanel;

	}

	/**
	 * get the Component that will be displayed in the SOUTH section of the
	 * BorderLayout. Note that if the dialog is resizable, this Component will
	 * need to stretch along the x axis, while retaining its aesthetic qualities
	 * 
	 * @return Component
	 */
	protected Component getBottomPanel() {
		Box bottomPanel = Box.createHorizontalBox();
		/** @todo - FIXME - needs to be implemented */
		return bottomPanel;
	}
}