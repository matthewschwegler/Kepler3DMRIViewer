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

package org.ecoinformatics.seek.querybuilder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JComboBox;

/**
 * In QueryBuilder GUI, there is a JComboBox for selecting table schema. This
 * listener will reponse when the selection event happens. It will recontruct
 * the schema display base on which table user selected.
 * 
 * @author tao
 * 
 */
public class TableSchemaSelectionListener implements ActionListener {

	private QBSplitPaneStandard pane;

	/**
	 * Constructor with dataModel and table view
	 * 
	 * @param pane
	 */
	public TableSchemaSelectionListener(QBSplitPaneStandard pane) {
		this.pane = pane;

	}

	/**
	 * Action reponse for the event: if seletion of ComboBox, change the
	 * tableView (JTable)'s model should be updated
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		JComboBox cb = (JComboBox) e.getSource();
		String selectedTableName = (String) cb.getSelectedItem();
		// System.out.println("The selected table name is "+ selectedTableName);
		if (pane != null) {
			Hashtable dataModelHash = pane.getModelHashtable();
			if (dataModelHash != null && !dataModelHash.isEmpty()) {
				// System.out.println("==========================get the model and restet it");
				DBSelectTableOverviewModel model = (DBSelectTableOverviewModel) dataModelHash
						.get(selectedTableName);
				// System.out.println("+++++++++++++++++new model is"+model);
				pane.setSelectedOverViewModel(model);
				pane.setOverTableView();
				// tableView.setModel(model);
			}
		}
		// tableView.
	}

}