/*
 * Copyright (c) 2006-2010 The Regents of the University of California.
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
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;

/**
 * This class will express an action which will delete a selected row from
 * DBselectTableModel
 * 
 * @author tao
 * 
 */
public class DBSelectTableModelDeleteAction extends AbstractAction {
	private DBSelectTableModelStd model;
	private int selectedRow;
	private DBSelectTableUIBase table;
	private static final boolean FALSE = false;

	/**
	 * Conscturct
	 * 
	 * @param model
	 * @param selectedRow
	 */
	public DBSelectTableModelDeleteAction(String name,
			DBSelectTableUIBase table, DBSelectTableModelStd model,
			int selectedRow) {
		super(name);
		this.table = table;
		this.model = model;
		this.selectedRow = selectedRow;
	}

	/**
	 * Invoked when an action occurs. It will delete selected row from model
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		if (model != null && selectedRow >= 0) {
			Vector itemList = model.getItemVector();
			Hashtable modelHash = model.getModelHashtable();
			// System.out.println("the old list is "+itemList);
			if (itemList != null) {
				// System.out.println("remove the selected row "+selectedRow);
				DBSelectTableModelItem selectedItem = (DBSelectTableModelItem) itemList
						.elementAt(selectedRow);
				String tableName = selectedItem.getTableName();
				String fieldName = selectedItem.getName();
				// System.out.println("the talbe name is "+tableName);
				// System.out.println("the file name is "+fieldName);
				// make sure the upper pane unselected the deleted field
				DBSelectTableOverviewModel overViewModel = (DBSelectTableOverviewModel) modelHash
						.get(tableName);
				DBSelectTableModelItem overViewItem = overViewModel
						.getItemByName(fieldName);
				overViewItem.setDisplayed(FALSE);
				// delete the selected row.
				itemList.remove(selectedRow);
				// Vector newList = model.getItemVector();
				// System.out.println("the new list is "+newList);
				// update the lower panel
				table.setModel(model);
				model.fireTableDataChanged();
				// update upper panel
				overViewModel.fireTableDataChanged();
				if (overViewModel.getTableView() != null) {
					overViewModel.getTableView().repaint();
				}

			}
		}

	}

}