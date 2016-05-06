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

package org.ecoinformatics.seek.ecogrid;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * This class represents a frame for service list modification
 * 
 * @author Jing Tao
 */

public class SearchRegistryResultFrame extends EcogridPreferencesTab {
	private JButton okButton = null;
	private JButton cancelButton = null;
	private EcogridPreferencesTab parent = null;

	private static final String TEXT = "Data Source(s) in Registry:";

	/**
	 * Constructor for the frame
	 * 
	 * @param frameTitle
	 *            String
	 * @param selectedServiceList
	 *            Vector
	 */
	public SearchRegistryResultFrame(EcogridPreferencesTab parent) {
		super();
		setDisplayText(TEXT);
		this.parent = parent;
		initButtonPanel();
	}// ServicesListModificationFrame

	/*
	 * This method will init button panel
	 */
	private void initButtonPanel() {

		okButton = new JButton(new AddServicesFromRegistrySearchAction("Add",
				this, parent));
		okButton.setPreferredSize(EcogridPreferencesTab.BUTTONDIMENSION);
		okButton.setMaximumSize(EcogridPreferencesTab.BUTTONDIMENSION);
		cancelButton = new JButton(new CancelSearchAction("Cancel", this.parentFrame,
				parent));
		cancelButton.setPreferredSize(EcogridPreferencesTab.BUTTONDIMENSION);
		cancelButton.setMaximumSize(EcogridPreferencesTab.BUTTONDIMENSION);
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		JPanel leftPart = new JPanel();
		leftPart.setLayout(new BoxLayout(leftPart, BoxLayout.X_AXIS));
		leftPart.add(okButton);
		leftPart.add(Box
				.createHorizontalStrut(EcogridPreferencesTab.MARGINGSIZE));
		leftPart.add(cancelButton);
		bottomPanel.add(Box.createHorizontalGlue(), BorderLayout.WEST);
		bottomPanel.add(leftPart, BorderLayout.EAST);

		JPanel newButtonPanel = new JPanel();
		newButtonPanel.setLayout(new BorderLayout());
		newButtonPanel.add(Box
				.createVerticalStrut(EcogridPreferencesTab.MARGINGSIZE),
				BorderLayout.NORTH);
		newButtonPanel.add(bottomPanel, BorderLayout.SOUTH);
		setButtonPanel(newButtonPanel);
	}// initButton

	public static void main(String[] args) {

		EcoGridServicesController controller = EcoGridServicesController
				.getInstance();
		Vector unSelectedserviceList = controller.getServicesList();
		// transfer to selectedSerive list(object is SelectedEcoGridService now)
		Vector selectedServicesLists = SelectableEcoGridService
				.transferServiceVectToDefaultSelectedServiceVect(unSelectedserviceList);

		/*
		 * SearchRegistryResultFrame frame = new
		 * SearchRegistryResultFrame("SwingApplication", null
		 * selectedServicesLists);
		 */

	}// main

}// ServicesListModificationFrame