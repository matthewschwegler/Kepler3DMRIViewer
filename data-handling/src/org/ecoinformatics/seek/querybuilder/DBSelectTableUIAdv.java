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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.DefaultCellEditor;

/**
 * This is the is derived from the abstract base class DBSelectTableUIBase, that
 * displays all the items to be displayed as part of the query. The advanced
 * class only allows the display attribute to be set
 */
public class DBSelectTableUIAdv extends DBSelectTableUIBase {
	/**
	 * Default Constructor
	 */
	public DBSelectTableUIAdv() {
		mDataFlavor[0] = new DataFlavor(DBTableField.class, "DBTableField");
		// mDataFlavor[1] = new DataFlavor(DSTableFieldDef.class,
		// "DSTableFieldDef"); // XXX Is this still needed???

	}

	/**
	 * Notification that an model item was added
	 * 
	 * @param aItem
	 */
	protected void itemWasAdded(DBSelectTableModelItem aItem) {
		// For Advanced Model
		if (mModel.getSchema() instanceof DBTableDesktopPane) {
			aItem.setDisplayed(true);

			super.itemWasAdded(aItem); // notify listener
		}

	}

	/**
	 * Creates and Installs cell editors, must be called AFTER setting the model
	 * 
	 */
	public void installEditors() {
		super.installEditors();
		getColumnModel().getColumn(3).setCellEditor(
				new DefaultCellEditor(mIsDisplayedCheckbox));
	}

	/**
	 * Return the DBTableField from the transferable
	 * 
	 * @param e
	 *            the event
	 * @return the field item for this transferable
	 */
	private DBTableField getTransferableAsDBField(DropTargetDropEvent e) {
		DBTableField dbTableField = null;
		try {
			Transferable tr = e.getTransferable();
			if (tr != null) {
				if (tr.isDataFlavorSupported(mDataFlavor[0])) {
					dbTableField = (DBTableField) tr
							.getTransferData(mDataFlavor[0]);
				}
			}
		} catch (IOException io) {
			io.printStackTrace();
		} catch (UnsupportedFlavorException ufe) {
			ufe.printStackTrace();
		}
		return dbTableField;
	}

	/**
	 * Does the drop
	 * 
	 * @param e
	 *            the event
	 */
	protected void doDrop(DropTargetDropEvent e) {

		DBTableField dbTableField = getTransferableAsDBField(e);
		if (dbTableField != null) {
			e.acceptDrop(mAceptableActions);
			e.getDropTargetContext().dropComplete(true);

			String tableName = dbTableField.getTable().getName();
			String fieldName = dbTableField.getName();

			boolean found = false;
			Vector fieldNames = mModel.getAvailableFieldNames(tableName);
			for (Enumeration et = fieldNames.elements(); et.hasMoreElements();) {
				String tblFieldName = (String) et.nextElement();
				if (tblFieldName.equals(fieldName)) {
					found = true;
					break;
				}
			}

			if (found) {
				itemWasAdded(mModel.add(dbTableField, false));
			}

			dragExit((DropTargetEvent) null);
			return;
		}

		e.rejectDrop();
		dragExit((DropTargetEvent) null);
	}
}