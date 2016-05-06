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
 * Used to render the table of items (rows) that make up the entire query.
 */
public class DBSelectTableUIStd extends DBSelectTableUIBase {
	public static final String[] OPERS = { DBUIUtils.NO_NAME, " = ", " != ",
			" > ", " < " };
	public static final String[] OPERS_TXT = { DBUIUtils.NO_NAME, "EQUALS",
			"NOT EQUALS", "GREATER THAN", "LESS THAN" };
	public static final int NONAME_INX = 0;
	public static final int EQUALS_INX = 1;
	public static final int NOTEQUALS_INX = 2;
	public static final int GT_INX = 3;
	public static final int LT_INX = 4;

	/**
	 * Default Constructor
	 * 
	 */
	public DBSelectTableUIStd() {
		mDataFlavor[0] = new DataFlavor(DBSelectTableModelItem.class,
				"DBSelectTableModelItem");
	}

	/**
	 * Notification that an model item was added
	 * 
	 * @param aItem
	 */
	protected void itemWasAdded(DBSelectTableModelItem aItem) {
		aItem.setDisplayed(true);
		super.itemWasAdded(aItem); // notify listener
	}

	/**
	 * Creates and Installs cell editors, must be called AFTER setting the model
	 */
	public void installEditors() {
		super.installEditors();

		for (int i = 0; i < OPERS_TXT.length; i++) {
			mBoolOpers.addItem(OPERS_TXT[i]);
		}
		getColumn("Operator").setCellEditor(new DefaultCellEditor(mBoolOpers));
		getColumn("Operator").setCellRenderer(new JComboBoxCellRenderer());
		// add right click menu
		PopupListener rightMenuListener = new PopupListener(this);
		this.addMouseListener(rightMenuListener);
	}

	/**
	 * Return the DBTableField from the transferable
	 * 
	 * @param e
	 *            the event
	 * @return the field item for this transferable
	 */
	private DBSelectTableModelItem getTransferableAsItem(DropTargetDropEvent e) {
		DBSelectTableModelItem item = null;
		try {
			Transferable tr = e.getTransferable();
			if (tr != null) {
				if (tr.isDataFlavorSupported(mDataFlavor[0])) {
					item = (DBSelectTableModelItem) tr
							.getTransferData(mDataFlavor[0]);
				}
			}
		} catch (IOException io) {
			io.printStackTrace();
		} catch (UnsupportedFlavorException ufe) {
			ufe.printStackTrace();
		}
		return item;
	}

	/**
	 * Does the drop,
	 */
	protected void doDrop(DropTargetDropEvent e) {

		DBSelectTableModelItem item = getTransferableAsItem(e);
		if (item != null) {
			e.acceptDrop(mAceptableActions);
			e.getDropTargetContext().dropComplete(true);

			String tableName = item.getTableName();
			String fieldName = item.getName();

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
				itemWasAdded(mModel.add(item, false));
			}

			dragExit((DropTargetEvent) null);
			return;
		}

		e.rejectDrop();
		dragExit((DropTargetEvent) null);
	}

	/**
	 * Returns the symbol for the text descript
	 * 
	 * @param aBoolStr
	 *            the string name of boolean operator
	 * @return symbol string
	 */
	public static String getBoolOperSymbol(String aBoolStr) {
		for (int i = 0; i < OPERS_TXT.length; i++) {
			if (aBoolStr.equals(OPERS_TXT[i])) {
				return OPERS[i];
			}
		}
		return aBoolStr;
	}

}