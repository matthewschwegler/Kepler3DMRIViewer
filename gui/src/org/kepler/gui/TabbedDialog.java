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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.HTMLViewer;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.data.expr.FileParameter;
import ptolemy.util.CancelException;
import ptolemy.util.MessageHandler;

/**
 * Base class for Actor, Director and Workflow dialogs, containing a JTabbedPane
 * to which any number of tabs may be added at runtime. Also has OK, Apply,
 * Cancel and Help buttons at the bottom of the dialog
 * 
 * @author Matthew Brooke
 * @since 27 February 2006
 */
public abstract class TabbedDialog extends JDialog {

	// ////////////////////////////////////////////////////////////////////////////
	// LOCALIZABLE RESOURCES - NOTE that these default values are later
	// overridden by values from the uiDisplayText resourcebundle file
	// ////////////////////////////////////////////////////////////////////////////
	private static final String OK_BUTTON_TEXT = StaticResources
			.getDisplayString("general.OK", "OK");
	private static String APPLY_BUTTON_TEXT = StaticResources.getDisplayString(
			"general.APPLY", "Apply");
	private static String CANCEL_BUTTON_TEXT = StaticResources
			.getDisplayString("general.CANCEL", "Cancel");
	private static String HELP_BUTTON_TEXT = StaticResources.getDisplayString(
			"general.HELP", "Help");
	// ////////////////////////////////////////////////////////////////////////////

	private static Dimension BUTTON_DIMS = StaticGUIResources.getDimension(
			"dialogs.buttons.width", "dialogs.buttons.height", 70, 20);

	private static final boolean IS_RESIZABLE = true;

	private static final Dimension TAB_PANEL_DIMS = StaticGUIResources
			.getDimension("dialogs.tabPanels.width",
					"dialogs.tabPanels.height", 630, 200);

	// Note that only the second number (the y value) is observed.
	// X-axis is stretched to fit the container
	private static final Dimension BUTTON_PANEL_DIMS = StaticGUIResources
			.getDimension("dialogs.buttonPanel.width",
					"dialogs.buttonPanel.height", 40, 40);

	// blank margin width (pixels) between jTabbedPane and dialog container at
	// sides & top. Bottom is set to 0, because button panel sets space there
	private static final int JTABPANE_MARGINS = StaticResources.getSize(
			"dialogs.tabPanels.margins", 15);

	private JPanel mainPanel = new JPanel();
	private JPanel buttonPanel = new JPanel();
	private JTabbedPane tabPane = new JTabbedPane();
	private JButton okButton = new JButton();
	private JButton applyButton = new JButton();
	private JButton cancelButton = new JButton();
	private JButton helpButton = new JButton();

	private static Log log = LogFactory.getLog(
			TabbedDialog.class.getName());
	private static boolean isDebugging = log.isDebugEnabled();

	/**
	 * Construct an instance of this TabbedDialog, with the specified owner
	 * Frame and title string. Optionally make dialog modal.
	 * 
	 * @param frame
	 *            Frame
	 * @param title
	 *            String
	 * @param modal
	 *            boolean
	 */
	public TabbedDialog(TableauFrame frame, String title, boolean modal) {

		super(frame, title, modal);
		this._frame = frame;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		init();
	}

	/**
	 * Add a new tab to the TabbedDialog, which will act as a container for the
	 * passed JPanel, and which will have the title text specified in tabText.
	 * Tab will be added at the next available position
	 * 
	 * @param panel
	 *            JPanel
	 * @param tabText
	 *            String
	 */
	protected void _addTab(JPanel panel, String tabText) {

		WidgetFactory.setPrefMinSizes(panel, TAB_PANEL_DIMS);
		tabPane.add(panel, tabText);
	}

	/**
	 * bring the requested tab to the front in the TabbedDialog.
	 * 
	 * @param dialogTab
	 *            AbstractDialogTab
	 */
	protected void _selectTab(AbstractDialogTab dialogTab) {
		tabPane.setSelectedComponent(dialogTab);
	}

	/**
	 * method that is called when user clicks the "OK" button. May be overridden
	 * by derived classes
	 * 
	 */
	protected void _okAction() {

		boolean savedOK = _applyAction();
		if (savedOK) {
			closeNoSave();
		} else {
			if (isDebugging) {
				log
						.info("Validation problem with user input; call to _applyAction() "
								+ "in implementing class returned FALSE");
			}
			// implementing _applyAction class should check for _applyAction
			// returning false, and if so, should display instructions to the
			// user
		}
	}

	/**
	 * method that is called when user clicks the "Apply" button. May be
	 * overridden by derived classes. this method contains a call to
	 * _validateInput() on ach of the contained AbstractDialogTabs; if
	 * _validateInput() returns false for any of these tab-panels, then this
	 * method, in turn, will return false
	 * 
	 * @return boolean true if apply worked, false if not (eg if there were
	 *         validation errors etc.)
	 */
	protected boolean _applyAction() {

		boolean allValid = true;
		AbstractDialogTab nextTab = null;
		int totTabs = tabPane.getTabCount();

		// first, call validateInput() on all tabs to
		// make sure we can proceed with save
		for (int i = 0; i < totTabs; i++) {
			nextTab = (AbstractDialogTab) tabPane.getComponent(i);
			if (nextTab == null) {
				continue;
			}
			if (!nextTab.validateInput()) {
				_selectTab(nextTab);
				allValid = false;
			}
		}

		// if everything is valid, call save() on all
		// tabs; if not, show an error message
		if (allValid) {
			for (int i = 0; i < totTabs; i++) {
				nextTab = (AbstractDialogTab) tabPane.getComponent(i);
				if (nextTab == null) {
					continue;
				}
				nextTab.save();
			}
			return true;
		} else {
			/** @todo - FIXME - need a dialog to tell user somethign's not valid */
			return false;
		}
	}

	/**
	 * method that is called when user clicks the "Cancel" button. May be
	 * overridden by derived classes
	 */
	protected void _cancelAction() {
		closeNoSave();
	}

	/**
	 * method that is called when user clicks the "Help" button. Shows top-level
	 * help pages, but may be overridden by derived classes to provide
	 * context-sensitive help
	 */
	protected void _helpAction() {

		try {
			URL doc = null;
			FileParameter helpAttribute = null;
			Configuration config = getConfiguration();

			if (config != null) {
				helpAttribute = (FileParameter) config.getAttribute("_help",
						FileParameter.class);
			}

			if (helpAttribute != null) {
				doc = helpAttribute.asURL();
				config.openModel(null, doc, doc.toExternalForm());
			} else {
				HTMLViewer viewer = new HTMLViewer();
				doc = getClass().getClassLoader().getResource(
						StaticResources.getSettingsString(
								"dialogs.defaultHelpURL",
								"ptolemy/configs/doc/basicHelp.htm"));
				viewer.setPage(doc);
				viewer.pack();
				viewer.setVisible(true);
			}

		} catch (Exception ex) {

			try {
				MessageHandler.warning(StaticResources.getDisplayString(
						"general.errors.noHelpAvailable",
						"Cannot open help page"), ex);
			} catch (CancelException exception) {
				// Ignore the cancel.
			}
		}
	}

	// ////////////////////////////////////////////////////////////////////////////
	// protected variables //
	// ////////////////////////////////////////////////////////////////////////////

	/**
	 * Dimensions of the JLabels to left of text input fields:
	 */
	protected static final Dimension jLabelDims = StaticGUIResources.getDimension(
			"dialogs.labels.width", "dialogs.labels.height", 10, 10);

	/**
	 * dims of dialog text input fields
	 */
	protected static final Dimension textFieldDims = StaticGUIResources
			.getDimension("dialogs.textfields.width",
					"dialogs.textfields.height", 10, 10);

	/**
	 * dims of "ID" label on dialog "general" tab
	 */
	protected static final Dimension idLabelDims = StaticGUIResources
			.getDimension("dialogs.labels.id.width",
					"dialogs.labels.id.height", 10, 10);

	/**
	 * dims of "ID" value on dialog "general" tab
	 */
	protected static final Dimension idValueDims = StaticGUIResources
			.getDimension("dialogs.labels.idvalue.width",
					"dialogs.labels.idvalue.height", 10, 10);

	/**
	 * dims of paddign inside tab panes
	 */
	protected static final Border tabPanePadding = BorderFactory
			.createEmptyBorder(
					// top, left, bottom, right
					StaticResources.getSize("dialogs.tabPanels.padding.top", 0),
					StaticResources
							.getSize("dialogs.tabPanels.padding.left", 0),
					StaticResources.getSize("dialogs.tabPanels.padding.bottom",
							0), StaticResources.getSize(
							"dialogs.tabPanels.padding.right", 0));

	protected TableauFrame _frame;

	// ////////////////////////////////////////////////////////////////////////////
	// private methods //
	// ////////////////////////////////////////////////////////////////////////////

	private void init() {

		this.setResizable(IS_RESIZABLE);
		mainPanel.setLayout(new BorderLayout());

		getContentPane().add(mainPanel);

		tabPane.setBorder(BorderFactory.createEmptyBorder(JTABPANE_MARGINS,
				JTABPANE_MARGINS, 0, JTABPANE_MARGINS));
		mainPanel.add(tabPane, java.awt.BorderLayout.CENTER);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0,
				JTABPANE_MARGINS, 0, JTABPANE_MARGINS));
		WidgetFactory.setPrefMinSizes(buttonPanel, BUTTON_PANEL_DIMS);
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		mainPanel.add(buttonPanel, java.awt.BorderLayout.SOUTH);

		Color bgColor = StaticGUIResources.getColor("dialogs.mainPanel.bgcolor.r",
				"dialogs.mainPanel.bgcolor.g", "dialogs.mainPanel.bgcolor.b");
		if (bgColor != null) {
			mainPanel.setOpaque(true);
			mainPanel.setBackground(bgColor);
			buttonPanel.setOpaque(true);
			buttonPanel.setBackground(bgColor);
		}

		initButtons();
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okButton);
		buttonPanel.add(WidgetFactory.getDefaultSpacer());
		buttonPanel.add(applyButton);
		buttonPanel.add(WidgetFactory.getDefaultSpacer());
		buttonPanel.add(cancelButton);
		buttonPanel.add(WidgetFactory.getDefaultSpacer());
		buttonPanel.add(helpButton);

	}

	protected static Configuration getConfiguration() {

		List configsList = Configuration.configurations();
		Configuration config = null;
		for (Iterator it = configsList.iterator(); it.hasNext();) {
			config = (Configuration) it.next();
			if (config != null)
				break;
		}
		return config;
	}

	private void initButtons() {

		// text
		okButton.setText(OK_BUTTON_TEXT);
		applyButton.setText(APPLY_BUTTON_TEXT);
		cancelButton.setText(CANCEL_BUTTON_TEXT);
		helpButton.setText(HELP_BUTTON_TEXT);

		// Dims
		WidgetFactory.setPrefMinSizes(okButton, BUTTON_DIMS);
		WidgetFactory.setPrefMinSizes(applyButton, BUTTON_DIMS);
		WidgetFactory.setPrefMinSizes(cancelButton, BUTTON_DIMS);
		WidgetFactory.setPrefMinSizes(helpButton, BUTTON_DIMS);

		// actionListeners
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_okAction();
			}
		});
		applyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_applyAction();
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_cancelAction();
			}
		});
		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_helpAction();
			}
		});
	}

	private void closeNoSave() {
		TabbedDialog.this.setVisible(false);
		TabbedDialog.this.dispose();
	}
}