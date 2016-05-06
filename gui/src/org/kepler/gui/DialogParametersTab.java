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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.InsetsUIResource;

import org.kepler.util.StaticResources;

import ptolemy.actor.gui.Configurer;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.gui.ComponentDialog;
import ptolemy.gui.Query;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.util.StringUtilities;

/**
 * A JTabbedPane tab to be added to a TabbedDialog object. This particular pane
 * shows the "Parameters" settings for the object being configured
 * 
 * @author Matthew Brooke
 * @since 27 February 2006
 */
public class DialogParametersTab extends AbstractDialogTab {

	public DialogParametersTab(NamedObj target, String targetType,
			TableauFrame frame) {
		super(target, targetType, frame);
	}

	/**
	 * check the user input for errors/omissions. Return true if everything is
	 * OK and we can proceed with a save(). Return false if there are problems
	 * that need to be corrected, and preferably request focus for the "problem"
	 * UI component
	 * 
	 * @return boolean true if everything is OK and we can proceed with a
	 *         save(). Return false if there are problems that need to be
	 *         corrected, and preferably request focus for the "problem" UI
	 *         component
	 */
	public boolean validateInput() {
		/** @todo - FIXME - needs to be implemented */
		return true;
	}

	/**
	 * Save the user-editable values associated with this tab. The container
	 * should probably call validateInput() on each tab before saving
	 */
	public void save() {
		/** @todo - FIXME - needs to be implemented */
	}

	/**
	 * getTopPanel
	 * 
	 * @return Component
	 */
	protected Component getTopPanel() {
		Box topPanel = Box.createHorizontalBox();
		cbExpert = new JCheckBox(StaticResources.getDisplayString(
				"dialogs.actor.parameters.expertCheckbox", ""));
		topPanel.add(cbExpert);
		topPanel.add(WidgetFactory.getDefaultSpacer());
		return topPanel;
	}

	/**
	 * getCenterPanel
	 * 
	 * @return Component
	 */
	protected Component getCenterPanel() {
		JPanel centerPanel = new JPanel(new BorderLayout());

		final Box paramsBox = Box.createVerticalBox();
		final Border titledBorder = BorderFactory.createTitledBorder("");

		paramsBox.setBorder(titledBorder);
		centerPanel.add(paramsBox, BorderLayout.CENTER);

		Box buttonBox = Box.createVerticalBox();

		initButtons();
		buttonBox.add(addButton);
		buttonBox.add(WidgetFactory.getDefaultSpacer());
		buttonBox.add(delButton);
		buttonBox.add(WidgetFactory.getDefaultSpacer());
		buttonBox.add(fmtButton);
		buttonBox.add(WidgetFactory.getDefaultSpacer());
		buttonBox.add(rstButton);
		buttonBox.add(Box.createVerticalGlue());
		centerPanel.add(buttonBox, BorderLayout.EAST);

		paramsBox.add(new Configurer(_target));

		return centerPanel;
	}

	/**
	 * getBottomPanel
	 * 
	 * @return Component
	 */
	protected Component getBottomPanel() {

		Dimension dim = new Dimension(StaticResources.getSize(
				"dialogs.tabPanels.padding.top", 0), 20); // y-component is
															// ignored
		return Box.createRigidArea(dim);
	}

	private void initButtons() {

		// remember default button margins
		final InsetsUIResource defaultUIMgrButtonMargin = (InsetsUIResource) UIManager
				.get("Button.margin");
		// now set our custom ones
		UIManager.put("Button.margin", BUTTON_INSIDE_PADDING);

		final int BUTTON_FONT_SIZE = StaticResources.getSize(
				"button.limitedSpace.maxFontSize", 11);
		// remember default button font
		final Font defaultUIMgrButtonFont = (Font) UIManager.get("Button.font");

		// now set our custom size, provided it's smaller than the default:
		int buttonFontSize = (defaultUIMgrButtonFont.getSize() < BUTTON_FONT_SIZE) ? defaultUIMgrButtonFont
				.getSize()
				: BUTTON_FONT_SIZE;

		addButton = new JButton();
		delButton = new JButton();
		fmtButton = new JButton();
		rstButton = new JButton();

		// text
		addButton.setText(StaticResources.getDisplayString(
				"dialogs.actor.parameters.addButton", ""));
		delButton.setText(StaticResources.getDisplayString(
				"dialogs.actor.parameters.deleteButton", ""));
		fmtButton.setText(StaticResources.getDisplayString(
				"dialogs.actor.parameters.formatButton", ""));
		rstButton.setText(StaticResources.getDisplayString(
				"dialogs.actor.parameters.resetButton", ""));
		// Dims
		WidgetFactory.setPrefMinMaxSizes(addButton, BUTTON_DIMS);
		WidgetFactory.setPrefMinMaxSizes(delButton, BUTTON_DIMS);
		WidgetFactory.setPrefMinMaxSizes(fmtButton, BUTTON_DIMS);
		WidgetFactory.setPrefMinMaxSizes(rstButton, BUTTON_DIMS);

		// actionListeners
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_addAction();
			}
		});
		delButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_deleteAction();
			}
		});
		fmtButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_formatAction();
			}
		});
		rstButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_resetAction();
			}
		});

		// restore default button margins
		if (defaultUIMgrButtonMargin != null) {
			UIManager.put("Button.margin", defaultUIMgrButtonMargin);
		}
		// restore default button font
		if (defaultUIMgrButtonFont != null) {
			UIManager.put("Button.font", defaultUIMgrButtonFont);
		}

	}

	/**
	 * _addAction
	 */
	private void _addAction() {

		// * * * SEE ptolemy.actor.gui.EditParametersDialog for how to do this *
		// * *

		/**
		 * @todo - note this changes params immediately on the target object - so
		 *       if user hits cancel, we need to roll them back
		 *       (configurer.restore()???)
		 */

		// Create a new dialog to add a parameter, then open a new
		// EditParametersDialog.
		Query _query = new Query();

		_query.addLine("name", "Name", "");
		_query.addLine("default", "Default value", "");
		_query.addLine("class", "Class", "ptolemy.data.expr.Parameter");

		ComponentDialog dialog = new ComponentDialog(null,
				"Add a new parameter to " + _target.getFullName(), _query, null);

		// If the OK button was pressed, then queue a mutation
		// to create the parameter.
		// A blank property name is interpreted as a cancel.
		String newName = _query.getStringValue("name");

		// Need to escape quotes in default value.
		String newDefValue = StringUtilities.escapeForXML(_query
				.getStringValue("default"));

		if (dialog.buttonPressed().equals("OK") && !newName.equals("")) {
			String moml = "<property name=\"" + newName + "\" value=\""
					+ newDefValue + "\" class=\""
					+ _query.getStringValue("class") + "\"/>";
			// _target.addChangeListener(this);

			MoMLChangeRequest request = new MoMLChangeRequest(this, _target,
					moml);
			request.setUndoable(true);
			_target.requestChange(request);
		}
		dialog.setVisible(true);
	}

	/**
	 * _addAction
	 */
	private void _deleteAction() {
		/** @todo - FIXME - implement this */
		// * * * SEE ptolemy.actor.gui.EditParametersDialog for how to do this *
		// * *
	}

	/**
	 * _addAction
	 */
	private void _formatAction() {
		/** @todo - FIXME - implement this. What does it do??? */
	}

	/**
	 * _addAction
	 */
	private void _resetAction() {
		/** @todo - FIXME - implement this */
		// * * * SEE ptolemy.actor.gui.EditParametersDialog for how to do this *
		// * *
		// maybe use configurer.restore()??
		//
		// ALSO - should global cancel call this method to reset params to
		// original??
	}

	private JButton addButton;
	private JButton delButton;
	private JButton fmtButton;
	private JButton rstButton;
	private final InsetsUIResource BUTTON_INSIDE_PADDING = new InsetsUIResource(
			2, 0, 2, 0); // (top, left, bottom, right)
	private JCheckBox cbExpert;
	private static final Dimension BUTTON_DIMS = StaticGUIResources.getDimension(
			"dialogs.params.buttons.width", "dialogs.params.buttons.height",
			94, 22);
}