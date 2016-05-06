/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.ecogrid;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class represents a dialog box for searching registry
 * 
 * @author Jing Tao
 */

public class RegistrySearchDialog extends JDialog {
	private static final int WIDTH = 500;
	private static final int HEIGHT = 200;
	private static final String CONTAINS = "contains";

	private JPanel mainPanel = new JPanel();
	private JButton searchButton = null;
	private JButton cancelButton = null;
	private JComboBox optionList = null;
	private JTextField inputField = new JTextField();
	private Vector options = new Vector();
	private EcogridPreferencesTab parent = null;
	private Hashtable xpathMap = new Hashtable();
	private Vector originalServiceList = null;

	public static final String SERVICENAME = "Service Name";
	public static final String LOCATION = "Location";
	public static final String ALLSERVICES = "All Services";
	public static final String SERVICENAMEXPATH = "serviceName";
	public static final String LOCATIONXPATH = "endPoint";

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.GetMetadataAction");
	}

	/**
	 * Construct of this dialog
	 * 
	 * @param parent
	 *            Frame
	 * @param title
	 *            String
	 */
	public RegistrySearchDialog(EcogridPreferencesTab parent, String title,
			Vector originalServiceList) {
		this.parent = parent;
		this.originalServiceList = originalServiceList;
		this.setLocation(parent.getLocation());
		setSize(new Dimension(WIDTH, HEIGHT));
		initMainPanel();
		getContentPane().add(
				Box.createVerticalStrut(EcogridPreferencesTab.MARGINGSIZE),
				BorderLayout.NORTH);
		getContentPane().add(
				Box.createHorizontalStrut(EcogridPreferencesTab.MARGINGSIZE),
				BorderLayout.EAST);
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		getContentPane().add(
				Box.createVerticalStrut(EcogridPreferencesTab.MARGINGSIZE),
				BorderLayout.SOUTH);
		getContentPane().add(
				Box.createHorizontalStrut(EcogridPreferencesTab.MARGINGSIZE),
				BorderLayout.WEST);
		setVisible(true);
	}// RegistrySearchDialog

	/*
	 * Method to init panels
	 */
	private void initMainPanel() {
		JPanel selectionPanel = new JPanel();
		selectionPanel
				.setLayout(new BoxLayout(selectionPanel, BoxLayout.X_AXIS));
		initOptions();
		optionList = new JComboBox(options);
		optionList.setEditable(false);
		optionList.addItemListener(new TextFieldEnableController());
		selectionPanel.add(optionList);
		selectionPanel.add(Box.createHorizontalStrut(EcogridPreferencesTab.GAP));
		JLabel label = new JLabel(CONTAINS);
		selectionPanel.add(label);
		selectionPanel.add(Box.createHorizontalStrut(EcogridPreferencesTab.GAP));
		inputField.setEnabled(false);
		selectionPanel.add(inputField);
		selectionPanel.add(Box.createHorizontalGlue());

		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(selectionPanel, BorderLayout.NORTH);
		mainPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JPanel rightButtonPanel = new JPanel();
		rightButtonPanel.setLayout(new BoxLayout(rightButtonPanel,
				BoxLayout.X_AXIS));
		/*searchButton = new JButton(new SearchRegistryAction("Search", this,
				parent, parent.getLocation()));
		searchButton.setPreferredSize(ServicesDisplayFrame.BUTTONDIMENSION);
		searchButton.setMaximumSize(ServicesDisplayFrame.BUTTONDIMENSION);
		rightButtonPanel.add(searchButton);*/
		rightButtonPanel.add(Box
				.createHorizontalStrut(EcogridPreferencesTab.MARGINGSIZE));
		cancelButton = new JButton(new CancelSearchAction("Cancel", this,
				parent));
		cancelButton.setPreferredSize(EcogridPreferencesTab.BUTTONDIMENSION);
		cancelButton.setMaximumSize(EcogridPreferencesTab.BUTTONDIMENSION);
		rightButtonPanel.add(cancelButton);
		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
		buttonPanel.add(rightButtonPanel, BorderLayout.EAST);

		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
	}// initMainPanel

	/*
	 * Method to initialize option list
	 */
	private void initOptions() {
		options.add(ALLSERVICES);
		options.add(SERVICENAME);
		options.add(LOCATION);
		xpathMap.put(SERVICENAME, SERVICENAMEXPATH);
		xpathMap.put(LOCATION, LOCATIONXPATH);
	}// initOptions

	/**
	 * Method to get the selected xpath in combobox.
	 * 
	 * @return String
	 */
	public String getXPath() {
		Object selectedObj = optionList.getSelectedItem();
		String selectedString = (String) selectedObj;
		log.debug("The selcted xpath is " + selectedString);
		return selectedString;
	}// getXPath

	/**
	 * Get the value from input text field
	 * 
	 * @return String
	 */
	public String getSearchValue() {
		String value = inputField.getText();
		log.debug("The input value is " + value);
		return value;
	}// getValue

	/**
	 * This method will retrun a xpath for optionLabel. For example, optionLabel
	 * is "Location" and xpath is "endPoint".
	 * 
	 * @param optionLabel
	 *            String
	 * @return String
	 */
	public String getXPath(String optionLabel) {
		String xpath = null;
		xpath = (String) xpathMap.get(optionLabel);
		return xpath;
	}// getXPath

	/*
	 * When option list selected is all service, the text field should be
	 * disable(Because no value needed). If option is not at all service the
	 * text box should be enable
	 */
	private class TextFieldEnableController implements ItemListener {

		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				// clear text in text box
				inputField.setText("");
				String selectedString = (String) optionList.getSelectedItem();
				log.debug("Selected item is " + selectedString);
				if (selectedString != null
						&& !selectedString.equals(ALLSERVICES)) {
					inputField.setEnabled(true);
				}// if
				else {
					inputField.setEnabled(false);
				}// else
			}// if
		}// itemStateChanged
	}// TextFieldEnableController

	public static void main(String[] arg) {
		// Frame parent = new Frame("hello");
		// RegistrySearchDialog dialog = new RegistrySearchDialog(null,
		// "RegistrySearch");
	}

}// RegistrySearchDialog