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

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;

/**
 * This classes is the abstract "base" class implementation of the model that is
 * used in the JTables. Classes are derived from this class so they can show
 * more or less information for the items that are being displayed.
 */
public abstract class DBSelectTableModelBase extends AbstractTableModel {
	protected Vector mItems;
	protected DSSchemaIFace mSchema;
	protected Vector mAvailTableNames = new Vector();
	protected Vector mAvailFieldNames = new Vector();

	/**
	 * Constructor for Table Model
	 * 
	 * @param aSchema
	 *            the schema
	 */
	public DBSelectTableModelBase(DSSchemaIFace aSchema) {
		mSchema = aSchema;
		mItems = new Vector();
		add((DBTableField) null, true);
	}

	/**
	 * Returns the number of columns
	 * 
	 * @return Number of columns
	 */
	public abstract int getColumnCount();

	/**
	 * Returns the Class object for a column
	 * 
	 * @param aColumn
	 *            the column in question
	 * @return the Class of the column
	 */
	public abstract Class getColumnClass(int aColumn);

	/**
	 * Indicates if col and row is editable
	 * 
	 * @param aRow
	 *            the row of the cell
	 * @param aColumn
	 *            the column of the cell
	 */
	public abstract boolean isCellEditable(int aRow, int aColumn);

	/**
	 * Get the column name
	 * 
	 * @param aColumn
	 *            the column of the cell to be gotten
	 */
	public abstract String getColumnName(int aColumn);

	/**
	 * Gets the value of the row col
	 * 
	 * @param aRow
	 *            the row of the cell to be gotten
	 * @param aColumn
	 *            the column of the cell to be gotten
	 */
	public abstract Object getValueAt(int aRow, int aColumn);

	/**
	 * Sets a new value into the Model
	 * 
	 * @param aValue
	 *            the value to be set
	 * @param aRow
	 *            the row of the cell to be set
	 * @param aColumn
	 *            the column of the cell to be set
	 */
	public abstract void setValueAt(Object aValue, int aRow, int aColumn);

	/**
	 * Sets Display attr in UI List cell
	 * 
	 * @param aFieldCell
	 *            the cell item to be changed
	 * @param aFlag
	 *            whether it is to be displayed
	 */
	protected abstract void setDisplayListCell(
			DBSelectTableModelItem aFieldCell, boolean aFlag);

	/**
	 * Returns the Schema object
	 * 
	 * @return the schema
	 */
	public DSSchemaIFace getSchema() {
		return mSchema;
	}

	/**
	 * Returns the number of rows
	 * 
	 * @return Number of rows
	 */
	public int getRowCount() {
		return mItems.size();
	}

	/**
	 * Resturns the items vector
	 * 
	 * 	 */
	public Vector getItemVector() {
		return mItems;
	}

	/**
	 * Gets the current field for a row
	 * 
	 * @param the
	 *            row of the field to be gotten
	 * @return the row's item, or null if the index is wrong
	 */
	protected DBSelectTableModelItem getFieldForRow(int aRow) {
		DBSelectTableModelItem field = null;
		if (aRow > -1 && aRow < mItems.size()) {
			return (DBSelectTableModelItem) mItems.elementAt(aRow);
		}
		return null;
	}

	/**
	 * Checks to see if there is a valid table name in the cell/row
	 * 
	 * @param the
	 *            row of the field to be checked
	 * @return false if the name is ok
	 */
	protected boolean isTableNameOK(int aRow) {
		boolean tableNameOK = false;
		DBSelectTableModelItem field = getFieldForRow(aRow);
		if (field != null) {
			tableNameOK = field != null && field.getTableName().length() > 0
					&& !field.getTableName().equals(DBUIUtils.NO_NAME);
		}
		return tableNameOK;
	}

	/**
	 * Sets the Display cell for a row
	 * 
	 * @param aRow
	 *            the row of the field to be updated
	 * @param aFlag
	 *            indicates whether it is displayed or not
	 */
	public void setIsDisplayed(int aRow, boolean aFlag) {
		DBTableField fieldCell = null;
		this.fireTableRowsUpdated(aRow, aRow);
	}

	/**
	 * Adds a field (Row) to the model
	 * 
	 * @param aField
	 * @param aDoAppend
	 *            indicates whether to append it at the end or one index before
	 *            the end
	 * @return the new item that was added
	 */
	public DBSelectTableModelItem add(DBTableField aField, boolean aDoAppend) {
		DBSelectTableModelItem item = new DBSelectTableModelItem(aField);
		mItems.add(aDoAppend ? mItems.size() : Math.max(mItems.size() - 1, 0),
				item);
		int newRow = mItems.size() - 1;
		fireTableRowsUpdated(0, newRow);
		this.fireTableDataChanged();
		return item;
	}

	/**
	 * Adds a item (Row) to the model
	 * 
	 * @param aItem
	 *            the item to add
	 * @param aDoAppend
	 *            indicates whether to append it at the end or one index before
	 *            the end
	 * @return the new item that was added
	 */
	public DBSelectTableModelItem add(DBSelectTableModelItem aItem,
			boolean aDoAppend) {
		DBSelectTableModelItem newItem = new DBSelectTableModelItem(
				(DBDisplayItemIFace) aItem);
		mItems.add(aDoAppend ? mItems.size() : Math.max(mItems.size() - 1, 0),
				newItem);
		int newRow = mItems.size() - 1;
		fireTableRowsUpdated(0, newRow);
		this.fireTableDataChanged();
		return aItem;
	}

	/**
	 * Sets the table name, if the name is NO_NAME then it deletes the row in
	 * the model
	 * 
	 * @param aField
	 *            object to be updated
	 * @param aRow
	 *            the row to be deleted if necessary
	 * @param aValue
	 *            the new name or NO_NAME
	 */
	protected void setTableName(DBSelectTableModelItem aField, int aRow,
			Object aValue) {
		if (aValue instanceof String) {
			String valStr = (String) aValue;
			setDisplayListCell(aField, false);
			if (valStr.equals(DBUIUtils.NO_NAME)) {
				if (mItems.size() > 1) {
					if (mItems.size() == 2) {
						if (aRow == 0) {
							aField = (DBSelectTableModelItem) mItems
									.elementAt(1);
							if (aField.getTableName().equals(DBUIUtils.NO_NAME)
									|| aField.getTableName().equals("")) {
								mItems.removeElementAt(aRow);
							}
						}
					} else if (aRow < mItems.size() - 1) {
						mItems.removeElementAt(aRow);
					}
				}

			} else {
				aField.setTableName(valStr);
				setDisplayListCell(aField, aField.isDisplayed());

				if (aRow == mItems.size() - 1) {
					add((DBTableField) null, true);
				}
			}
			this.fireTableDataChanged();
		}
	}

	/**
	 * Checks to see if the Table Name/Field Name is already in the model
	 * 
	 * @param aTableName
	 *            table name
	 * @param aFieldName
	 *            field name
	 * @return true if in model, false if not.
	 */
	private boolean isInModelAlready(String aTableName, String aFieldName) {
		for (Enumeration e = mItems.elements(); e.hasMoreElements();) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) e
					.nextElement();
			if (aTableName.equals(item.getTableName())
					&& aFieldName.equals(item.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets a field name, and notifies the model of data change
	 * 
	 * @param aField
	 *            a model item to be changed
	 * @param aValue
	 *            the new name
	 */
	protected void setFieldName(DBSelectTableModelItem aField, Object aValue) {
		if (aValue instanceof String) {

			if (!isInModelAlready(aField.getTableName(), aField.getName())) {
				setDisplayListCell(aField, false);
			}

			String fieldName = (String) aValue;
			aField.setName(fieldName);
			if (fieldName.equals("*")) {
				aField.setDataType("");
				this.fireTableDataChanged();
			} else {
				DSTableFieldIFace fieldIFace = DBUIUtils.getFieldByName(
						mSchema, aField.getTableName(), fieldName);
				if (fieldIFace != null) {
					aField.setDataType(fieldIFace.getDataType());
					this.fireTableDataChanged();
				}

				setDisplayListCell(aField, aField.isDisplayed());
			}
		}
	}

	/**
	 * Sets the field to be marked as "displayed" and updates the UI
	 * 
	 * @param aField
	 *            the field
	 * @param aValue
	 *            the Boolean value to be set
	 */
	protected void setDisplay(DBSelectTableModelItem aField, Object aValue) {
		if (aValue instanceof Boolean) {
			boolean val = ((Boolean) aValue).booleanValue();
			aField.setDisplayed(val);
			setDisplayListCell(aField, val);
		}
	}

	/**
	 * Sets the crieria field if it is a string
	 * 
	 * @param aField
	 *            the field object
	 * @param aValue
	 *            the new string value
	 */
	protected void setCriteria(DBSelectTableModelItem aField, Object aValue) {
		if (aValue instanceof String)
			aField.setCriteria((String) aValue);
	}

	/**
	 * Returns the available field names for a table
	 * 
	 * @param aTableName
	 *            table name
	 * @return vector of names
	 */
	public Vector getAvailableFieldNames(String aTableName) {
		mAvailFieldNames.removeAllElements();
		mAvailFieldNames.add(DBUIUtils.ALL_FIELDS);

		Vector tables = mSchema.getTables();
		if (tables != null && tables.size() > 0) {
			for (Enumeration et = tables.elements(); et.hasMoreElements();) {
				DSTableIFace table = (DSTableIFace) et.nextElement();
				if (table.getName().equals(aTableName)) {
					Vector fields = table.getFields();
					for (Enumeration ef = fields.elements(); ef
							.hasMoreElements();) {
						DSTableFieldIFace field = (DSTableFieldIFace) ef
								.nextElement();
						if (!field.getName().equals(DBUIUtils.ALL_FIELDS))
							mAvailFieldNames.add(field.getName());
					}
					break;
				}
			}
		}
		return mAvailFieldNames;
	}

	/**
	 * Returns a vector of table names and includes the name of the table
	 * identified by the aRow parameter
	 * 
	 * @param aRow
	 *            the row to be included or -1
	 * @return vector of table names
	 */
	public Vector getAvailableTableNames(int aRow) {
		mAvailTableNames.removeAllElements();
		mAvailTableNames.add(DBUIUtils.NO_NAME);

		Vector tables = mSchema.getTables();
		if (tables != null && tables.size() > 0) {
			for (Enumeration et = tables.elements(); et.hasMoreElements();) {
				DSTableIFace table = (DSTableIFace) et.nextElement();
				getAvailableFieldNames(table.getName());
				if (mAvailFieldNames.size() > 0) {
					mAvailTableNames.add(table.getName());
				}
			}
		}

		return mAvailTableNames;
	}

	/**
	 * Notifies the table that it has been completely updated.
	 * 
	 */
	public void fireTableModelChanged() {
		fireTableRowsUpdated(0, mItems.size() - 1);
		fireTableDataChanged();
	}

}