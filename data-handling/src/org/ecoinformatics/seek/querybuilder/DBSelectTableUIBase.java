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

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * The JTable abstract base class for rendering portions (or all) of the select
 * statement.
 * 
 */
public abstract class DBSelectTableUIBase extends JTable implements
		DropTargetListener {
	protected DBSelectTableModelBase mModel = null;
	protected DataFlavor[] mDataFlavor = new DataFlavor[1];
	protected int mAceptableActions = DnDConstants.ACTION_COPY_OR_MOVE;
	protected DropTarget mDropTarget = new DropTarget(this, mAceptableActions,
			this);

	protected Vector mListeners = new Vector();

	protected JCheckBox mIsDisplayedCheckbox = null;

	protected JComboBox mTablesComboBox = null;
	protected JComboBox mFieldsComboBox = null;
	protected JComboBox mBoolOpers = null;

	protected String mDragTableName = "";

	protected boolean mCheckOkToDrop = true;
	protected boolean mOkToDrop = true;

	private static final int INCLUDECLOWIDTH = 100;

	/**
	 * Constructs a Table "View" which allows DBTable items to be dropped onto
	 * it
	 * 
	 */
	public DBSelectTableUIBase() {
	}

	/*
	 * sets tyhe listener to null
	 */
	public void finalize() {
		mListeners = null;
	}

	/**
	 * Perform the drop action for the sub-class of choice
	 * 
	 * @param e
	 *            the ebent
	 */
	protected abstract void doDrop(DropTargetDropEvent e);

	/**
	 * Notification that an model item was added.
	 * 
	 * @param aItem
	 *            the item to be added
	 */
	protected void itemWasAdded(DBSelectTableModelItem aItem) {
		if (mListeners != null) {
			for (Enumeration ef = mListeners.elements(); ef.hasMoreElements();) {
				((DBSelectTableFieldChangedListener) ef.nextElement())
						.notifyFieldChanged();
			}
		}
	}

	/**
	 * Adds listener
	 * 
	 * @param aListener
	 *            listener to be added
	 * 
	 */
	public void addFieldChangeListener(
			DBSelectTableFieldChangedListener aListener) {
		if (aListener != null)
			mListeners.add(aListener);
	}

	/**
	 * Sets a single listener for this object, remove it by setting it to null
	 * 
	 * @param aListener
	 *            listener to be added
	 * 
	 */
	public void removeFieldChangeListener(
			DBSelectTableFieldChangedListener aListener) {
		if (aListener != null)
			mListeners.remove(aListener);
	}

	/**
	 * Receices this notification before the editor is paced in the cell
	 * 
	 * @param aRow
	 *            row of cell
	 * @param aColumn
	 *            coloumn of cell
	 */
	public TableCellEditor getCellEditor(int aRow, int aColumn) {
		// fill the comboboxes that will be used for the cell
		if (aColumn == 0) {
			fillTableCombobox(aRow);

		} else if (aColumn == 1) {
			fillFieldCombobox(aRow);
		}
		return super.getCellEditor(aRow, aColumn);
	}

	/**
	 * Fill the table combobox, note that if all the fields have been already
	 * used the table won't show in the list unless they are editting a row
	 * containing that table name
	 * 
	 * @param aRow
	 *            the row of the item
	 */
	protected void fillTableCombobox(int aRow) {
		mTablesComboBox.removeAllItems();
		Vector tableNames = mModel.getAvailableTableNames(aRow);
		for (Enumeration ef = tableNames.elements(); ef.hasMoreElements();) {
			mTablesComboBox.addItem((String) ef.nextElement());
		}
	}

	/**
	 * Fill the fields combobox with any field names for that table that have
	 * not been used. BUT! Remember to include the name of the item that is
	 * currently being editted
	 * 
	 * @param aRow
	 *            the row of the item
	 * 
	 */
	protected void fillFieldCombobox(int aRow) {
		mFieldsComboBox.removeAllItems();
		String tableName = mModel.getFieldForRow(aRow).getTableName();
		Vector fieldNames = mModel.getAvailableFieldNames(tableName);
		for (Enumeration ef = fieldNames.elements(); ef.hasMoreElements();) {
			mFieldsComboBox.addItem((String) ef.nextElement());
		}
	}

	/**
	 * Set the data model into the table
	 * 
	 * @param aModel
	 *            the table data model
	 */
	public void setModel(DBSelectTableModelBase aModel) {
		if (aModel != null)
			super.setModel(aModel);
		mModel = aModel;
	}

	/**
	 * Creates and Installs cell editors, must be called AFTER setting the model
	 */
	public void installEditors() {
		mIsDisplayedCheckbox = new JCheckBox();

		if (mModel != null && mModel.getSchema() != null) {
			// Use the combo box as the editor in the "Favorite Color" column.
			mTablesComboBox = new JComboBox();
			mTablesComboBox.setBackground(Color.WHITE);
			mFieldsComboBox = new JComboBox();
			mFieldsComboBox.setBackground(Color.WHITE);
			mBoolOpers = new JComboBox();
			mBoolOpers.setBackground(Color.WHITE);

			mTablesComboBox.addItem("  ");
			DBUIUtils.fillTableCombobox(mModel.getSchema(), mTablesComboBox);
			setRowHeight(mTablesComboBox.getPreferredSize().height);

			getColumn("Table").setCellEditor(
					new DefaultCellEditor(mTablesComboBox));
			getColumn("Table").setCellRenderer(new JComboBoxCellRenderer());
			getColumn("Field").setCellEditor(
					new DefaultCellEditor(mFieldsComboBox));
			getColumn("Field").setCellRenderer(new JComboBoxCellRenderer());
			getColumn(DBSelectTableModelStd.INCLUDESELECTION)
					.setPreferredWidth(INCLUDECLOWIDTH);

		}
	}

	/**
	 * Indicates whether the items can be dropped
	 * 
	 * @param aTableName
	 *            the table name
	 * @param aFieldName
	 *            the field name
	 * @return true if is can be dropped, otherwise false
	 */
	private boolean okToDrop(String aTableName, String aFieldName) {

		Vector fieldNames = mModel.getAvailableFieldNames(aTableName);
		for (Enumeration et = fieldNames.elements(); et.hasMoreElements();) {
			String tblFieldName = (String) et.nextElement();
			if (tblFieldName.equals(aFieldName)) {
				return true;
			}
		}
		return false;
	}

	// --------------------------------------------------------------
	// ------------------ Drop Target Methods -----------------------
	// --------------------------------------------------------------

	/**
	 * Checks to see if it is the right type of object
	 */
	public void dragEnter(DropTargetDragEvent e) {
		if (!isDragOk(e)) {
			e.rejectDrag();
			return;
		}
	}

	/**
	 * Stubbed
	 */
	public void dragExit(DropTargetEvent dropTargetEvent) {
	}

	/**
	 * Checks to see if it is the right type of object
	 */
	public void dragOver(DropTargetDragEvent e) {

		if (!isDragOk(e)) {
			e.rejectDrag();
			return;
		}
	}

	/**
	 * Checks to make sure the transaferable is OK to be dropped
	 * 
	 * @param e
	 *            current D&D event
	 * @return whether it can be dropped
	 */
	private boolean isDragOk(DropTargetDragEvent e) {
		if (!e.isDataFlavorSupported(mDataFlavor[0])) {
			return false;
		}

		// the actions specified when the source
		// created the DragGestureRecognizer
		int sa = e.getSourceActions();

		// we're saying that these actions are necessary
		if ((sa & mAceptableActions) == 0)
			return false;

		// System.out.println("isDragOk: "+e.getSourceActions() +
		// " mAceptableActions "+mAceptableActions +"  "+(((e.getSourceActions()
		// & mAceptableActions) != 0)));
		return true;
	}

	/**
	 * Stubbed
	 */
	public void dropActionChanged(DropTargetDragEvent dropTargetDragEvent) {
	}

	/**
	 * Allows DBTable items to be droped and then creates a new "row" in the
	 * table representing the item
	 */
	public synchronized void drop(DropTargetDropEvent e) {
		doDrop(e);
	}

	/*
	 * This calss will render at table cell as JComboBox
	 */
	protected class JComboBoxCellRenderer implements TableCellRenderer {
		public Component getTableCellRendererComponent(JTable table,
				Object Value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			// System.out.println("value in ===================== is "+Value);
			String[] itemList = new String[1];
			if (Value == null) {
				itemList[0] = "";
			} else {
				itemList[0] = Value.toString();
			}
			JComboBox renderer = new JComboBox(itemList);
			renderer.setSelectedIndex(0);
			renderer.setBackground(Color.WHITE);
			return renderer;
		}
	}

	/*
	 * This class will add listener to pop up a right click menu - deleting this
	 * row
	 */
	protected class PopupListener extends MouseAdapter {
		// on the Mac, popups are triggered on mouse pressed, while
		// mouseReleased triggers them on the PC; use the trigger flag to
		// record a trigger, but do not show popup until the
		// mouse released event
		private boolean trigger = false;
		private DBSelectTableUIBase table;

		public PopupListener(DBSelectTableUIBase table) {
			this.table = table;
		}

		/**
		 * Description of the Method
		 * 
		 *@param e
		 *            Description of the Parameter
		 */
		public void mousePressed(MouseEvent e) {
			// maybeShowPopup(e);
			if (e.isPopupTrigger()) {
				trigger = true;
			}
		}

		/**
		 * Description of the Method
		 * 
		 *@param e
		 *            Description of the Parameter
		 */
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		/**
		 * Description of the Method
		 * 
		 *@param e
		 *            Description of the Parameter
		 */
		private void maybeShowPopup(MouseEvent e) {
			if ((e.isPopupTrigger()) || (trigger) && table != null
					&& mModel != null) {
				// System.out.println("start in myabe show popup");
				trigger = false;
				int selectedRow = table
						.rowAtPoint(new Point(e.getX(), e.getY()));
				// System.out.println("the selected row number is "+selectedRow);
				int rowLength = mModel.getRowCount();
				// System.out.println("the row number in the model is "+rowLength);
				// the last row wouldn't popup a deletion menu because
				// it is
				if ((selectedRow != rowLength - 1)) {
					DBSelectTableModelStd stdModel = (DBSelectTableModelStd) mModel;
					// System.out.println("in generate pop menu (pass selected != rowLength");
					DBSelectTableModelDeleteAction deleteAction = new DBSelectTableModelDeleteAction(
							"Delete", table, stdModel, selectedRow);
					JPopupMenu popup = new JPopupMenu();
					;
					JMenuItem deletionMenuItem = new JMenuItem(deleteAction);
					deletionMenuItem.setBackground(Color.LIGHT_GRAY);
					popup.add(deletionMenuItem);
					popup.show(e.getComponent(), e.getX(), e.getY());

				}
			}
		}
	}

}