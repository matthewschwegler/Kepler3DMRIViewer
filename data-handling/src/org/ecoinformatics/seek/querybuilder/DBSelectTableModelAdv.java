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

import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;

/**
 * This class extends DBSelectTableModelBase and is used to enabled the user to
 * indicate which items will be displayed. It's JTable (view) is displayed in
 * the "Select" table in the lower panel.
 */
public class DBSelectTableModelAdv extends DBSelectTableModelBase {
	protected String[] COLUMN_TITLES = { "Table", "Field", "Data Type",
			"Display" };// , "Criteria"};

	/**
	 * Constructor for Table Model
	 * 
	 * @param aSchema
	 *            the schema object
	 */
	public DBSelectTableModelAdv(DSSchemaIFace aSchema) {
		super(aSchema);
	}

	/**
	 * Look up in the Schema for the field in it's table Then checks to see if
	 * it is an instanceof DBTableField and returns that
	 * 
	 * @param aItemCell
	 *            TableMode cell item
	 * @return List Cell Item (hopefully), or null if it isn't an instance of
	 *         DBTableField
	 */
	protected DBTableField getFieldFor(DBSelectTableModelItem aItemCell) {
		DSTableFieldIFace fieldIFace = DBUIUtils.getFieldByName(mSchema,
				aItemCell.getTableName(), aItemCell.getName());
		if (fieldIFace instanceof DBTableField) {
			return (DBTableField) fieldIFace;
		}
		return null;
	}

	/**
	 * Sets Display attr in UI List cell, and causes a refresh to occur
	 * 
	 * @param aFieldCell
	 *            field
	 * @param aFlag
	 *            whether it is displayed or not
	 */
	protected void setDisplayListCell(DBSelectTableModelItem aFieldCell,
			boolean aFlag) {
		DBTableField field = getFieldFor(aFieldCell);
		if (field != null) {
			field.setDisplayed(aFlag);
			if (mSchema instanceof DBTableDesktopPane) {
				((DBTableDesktopPane) mSchema).makeDirty();
			}
		}
	}

	/**
	 * Returns the number of columns
	 * 
	 * @return Number of columns
	 */
	public int getColumnCount() {
		return COLUMN_TITLES.length;
	}

	/**
	 * Returns the Class object for a column
	 * 
	 * @param aCol
	 *            the column in question
	 * @return the Class of the column
	 */
	public Class getColumnClass(int aCol) {
		if (aCol == 3)
			return Boolean.class;
		else
			return getValueAt(0, aCol).getClass();
	}

	/**
	 * Indicates if col and row is editable
	 * 
	 * @param aRow
	 *            index of row
	 * @param aCol
	 *            index of column
	 * @return true if editable, false if not
	 */
	public boolean isCellEditable(int aRow, int aCol) {
		boolean tableNameOK = isTableNameOK(aRow);
		switch (aCol) {
		case 0:
			return true;
		case 1:
			return tableNameOK;
		case 2:
			return false;
		case 3:
			return tableNameOK;
		case 4: {
			DBSelectTableModelItem field = getFieldForRow(aRow);
			return field != null && tableNameOK
					&& !field.getName().equals(DBUIUtils.ALL_FIELDS);
		}
		}

		return true;
	}

	/**
	 * Get the column name
	 * 
	 * @param aCol
	 *            index of column
	 * @return the column name
	 */
	public String getColumnName(int aCol) {
		return COLUMN_TITLES[aCol];
	}

	/**
	 * Gets the value of the row col
	 * 
	 * @param aRow
	 *            index of row
	 * @param aCol
	 *            index of column
	 */
	public Object getValueAt(int aRow, int aCol) {
		DBSelectTableModelItem field = getFieldForRow(aRow);
		if (field != null) {
			switch (aCol) {
			case 0:
				return field.getTableName();
			case 1:
				return field.getName();
			case 2:
				return field.getDataType();
			case 3:
				return new Boolean(field.isDisplayed());
			case 4:
				return field.getCriteria();
			default:
				return "N/A";
			}
		}
		return "";
	}

	/**
	 * Sets a new value into the Model
	 * 
	 * @param aValue
	 *            new value
	 * @param aRow
	 *            index of row
	 * @param aCol
	 *            index of column
	 */
	public void setValueAt(Object aValue, int aRow, int aCol) {
		DBSelectTableModelItem fieldCell = null;
		if (aRow < mItems.size()) {
			fieldCell = (DBSelectTableModelItem) mItems.elementAt(aRow);
			switch (aCol) {
			case 0: {
				String tableName = (String) aValue;
				if (!tableName.equals(DBUIUtils.NO_NAME)) {
					// Get the avilable names that are not yet used
					getAvailableFieldNames(tableName);
					String fieldName = (String) mAvailFieldNames.elementAt(0);
					fieldCell.setTableName(tableName);
					fieldCell.setName(fieldName);
					DSTableFieldIFace fieldIFace = DBUIUtils.getFieldByName(
							mSchema, tableName, fieldName);
					fieldCell.setDataType(fieldIFace.getDataType());
				}
				setTableName(fieldCell, aRow, aValue);
			}
				break;

			case 1:
				setFieldName(fieldCell, aValue);
				break;

			case 2:
				// shouldn't happen
				break;

			case 3:
				setDisplay(fieldCell, aValue);
				break;

			case 4:
				setCriteria(fieldCell, aValue);
				break;

			}
			fireTableModelChanged();
		}
	}

	/**
	 * Fills QueryDef from Model with "select" items. NOTE: it makes copies of
	 * the items.
	 * 
	 * @param aQueryDef
	 *            item to be copied
	 */
	public void fillQueryDef(DBQueryDef aQueryDef) {
		for (Enumeration e = mItems.elements(); e.hasMoreElements();) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) e
					.nextElement();
			if (item.getTableName().length() > 0
					&& !item.getTableName().equals(DBUIUtils.NO_NAME)
					&& item.getName().length() > 0) {
				aQueryDef.addSelectItem(new DBSelectTableModelItem(item));
			}
		}
	}

	/**
	 * Builds UI from the Query Definition Object The Advance Model just tracks
	 * what is being displayed
	 * 
	 * @param aQueryDef
	 *            the query
	 */
	public void buildFromQueryDef(DBQueryDef aQueryDef) {
		if (aQueryDef == null)
			return;

		mItems.clear();
		for (Enumeration e = aQueryDef.getSelects().elements(); e
				.hasMoreElements();) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) e
					.nextElement();
			if (item.getTableName().length() > 0
					&& !item.getTableName().equals(DBUIUtils.NO_NAME)
					&& item.getName().length() > 0) {
				mItems.add(new DBSelectTableModelItem(item));
				// XXX not using setDisplayListCell,
				// for some reason the last item does get updated correctly
				DBTableField field = getFieldFor(item);
				item.setDisplayed(true);
				if (field != null) {
					field.setDisplayed(true);
				}
				// XXX setDisplayListCell(item, true);
			}
		}

		// Make sure everything gets correctly update
		if (mSchema instanceof DBTableDesktopPane) {
			((DBTableDesktopPane) mSchema).makeDirty();
		}

		add((DBTableField) null, true);
		fireTableModelChanged();
	}

}