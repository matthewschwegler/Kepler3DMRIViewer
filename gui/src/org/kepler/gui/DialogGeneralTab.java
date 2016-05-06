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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.moml.NamedObjId;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.CancelException;
import ptolemy.util.MessageHandler;

/**
 * A JTabbedPane tab to be added to a TabbedDialog object. This particular pane
 * shows the "General" settings for the object being configured
 * 
 * @author Matthew Brooke
 * @since 27 February 2006
 */
public class DialogGeneralTab extends AbstractDialogTab {

	public DialogGeneralTab(NamedObj target, String targetType,
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
		if (nameTxtFld.getText() == null
				|| nameTxtFld.getText().trim().length() < 1) {
			nameTxtFld.requestFocus();
			return false;
		}
		return true;
	}

	/**
	 * Save the user-editable values associated with this tab. The container
	 * should probably call validateInput() on each tab before saving
	 */
	public void save() {

		if (_target == null) {
			log.warn("Cannot save - target is NULL");
			return;
		}
		try {
			_target.setName(nameTxtFld.getText());
		} catch (NameDuplicationException ex) {
			try {
				MessageHandler.warning(StaticResources.getDisplayString(
						"general.errors.NameDuplication", "Error"), ex);
			} catch (CancelException exception) {
				// Ignore the cancel.
			}
		} catch (IllegalActionException ex) {
			try {
				MessageHandler.warning(StaticResources.getDisplayString(
						"general.errors.UnknownCannotDo", "Error"), ex);
			} catch (CancelException exception) {
				// Ignore the cancel.
			}
		}
		/** @todo - FIXME - need to save these values - how? */
		noteTxtArea.getText();

		/** @todo - FIXME - need to save these values - how? */
		cbName.isSelected();
		cbNotes.isSelected();
		cbPorts.isSelected();
	}

	/**
	 * getTopPanel
	 * 
	 * @return Component
	 */
	protected Component getTopPanel() {

		Box topPanel = Box.createHorizontalBox();
		JLabel nameLbl = WidgetFactory.makeJLabel(StaticResources
				.getDisplayString("dialogs." + _targetType + ".general.name",
						""), TabbedDialog.jLabelDims);
		topPanel.add(nameLbl);

		nameTxtFld = WidgetFactory.makeJTextField((_target != null ? _target
				.getName() : ""), TabbedDialog.textFieldDims);
		topPanel.add(nameTxtFld);

		JLabel idLbl = WidgetFactory.makeJLabel(
				StaticResources.getDisplayString("dialogs." + _targetType
						+ ".general.id", ""), TabbedDialog.idLabelDims);
		idLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		topPanel.add(idLbl);

		topPanel.add(WidgetFactory.getDefaultSpacer());

		JLabel idFieldLbl = WidgetFactory.makeJLabel(getLSIDString(_target),
				TabbedDialog.idValueDims);
		topPanel.add(idFieldLbl);

		topPanel.add(Box.createHorizontalGlue());

		return topPanel;
	}

	/**
	 * getCenterPanel
	 * 
	 * @return Component
	 */
	protected Component getCenterPanel() {
		Box centerPanel = Box.createHorizontalBox();

		final Border middlePanelPaddingBorder = BorderFactory
				.createEmptyBorder( // top, left, bottom, right
						StaticResources.getSize(
								"dialogs.tabPanels.padding.top", 0), 0, 0, 0);

		centerPanel.setBorder(middlePanelPaddingBorder);

		JLabel noteLbl = WidgetFactory.makeJLabel(StaticResources
				.getDisplayString("dialogs." + _targetType + ".general.note",
						"Note"), TabbedDialog.jLabelDims);
		centerPanel.add(noteLbl);

		noteTxtArea = WidgetFactory.makeJTextArea(_target != null ? _target
				.getName() : "");
		JScrollPane scrollPane = new JScrollPane(noteTxtArea);
		scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane
				.setVerticalScrollBarPolicy(scrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setWheelScrollingEnabled(true);
		centerPanel.add(scrollPane);

		return centerPanel;
	}

	/**
	 * getBottomPanel
	 * 
	 * @return Component
	 */
	protected Component getBottomPanel() {
		final Box bottomPanel = Box.createVerticalBox();
		final Border bottomPanelTitledBorder = BorderFactory
				.createTitledBorder(StaticResources.getDisplayString("dialogs."
						+ _targetType + ".general.NamesNotesBorderTitle", ""));

		bottomPanel.setBorder(bottomPanelTitledBorder);

		cbName = new JCheckBox(StaticResources.getDisplayString("dialogs."
				+ _targetType + ".general.showNameCheckbox", ""));
		cbNotes = new JCheckBox(StaticResources.getDisplayString("dialogs."
				+ _targetType + ".general.showNoteCheckbox", ""));
		cbPorts = new JCheckBox(StaticResources.getDisplayString("dialogs."
				+ _targetType + ".general.showPortNamesCheckbox", ""));

		bottomPanel.add(cbName);
		bottomPanel.add(cbNotes);
		bottomPanel.add(cbPorts);
		return bottomPanel;
	}

	private String getLSIDString(NamedObj target) {

		// never knowingly returns null...
		if (target == null) {
			return "";
		}
		NamedObjId lsid = (NamedObjId) target
				.getAttribute(NamedObjId.NAME);
		String lsidStr = "";
		if (lsid != null) {
			lsidStr = lsid.getExpression();
			if (isDebugging) {
				log.debug("\n\n*** FOUND LSID (" + lsidStr + ") for: "
						+ target.getClassName());
			}
		}
		return lsidStr;
	}

	private JCheckBox cbName;
	private JCheckBox cbNotes;
	private JCheckBox cbPorts;
	private JTextArea noteTxtArea;
	private JTextField nameTxtFld;

	private static final Log log = LogFactory.getLog(
			DialogGeneralTab.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
}