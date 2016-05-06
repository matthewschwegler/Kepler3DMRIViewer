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

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;

/**
 * This derived class is used for representing a table's schema in a UI "tab"
 */
public class DBSelectTableOverviewModel extends AbstractTableModel {
	protected static final String[] COLUMN_TITLES = { "Field Name", "Data Type" };

	protected DSTableIFace mTable = null;
	protected Vector mItems = null;
	protected JTable mTableView = null;

	/**
	 * Constructor for Table Model
	 * 
	 * @param aTable
	 *            the table
	 */
	public DBSelectTableOverviewModel(DSTableIFace aTable) {
		mTable = aTable;

		mItems = new Vector();
		DBSelectTableModelItem item = new DBSelectTableModelItem();
		item.setName(DBUIUtils.ALL_FIELDS);
		item.setTableName(aTable.getName());
		mItems.add(item);

		for (Enumeration et = mTable.getFields().elements(); et
				.hasMoreElements();) {
			DSTableFieldIFace field = (DSTableFieldIFace) et.nextElement();
			item = new DBSelectTableModelItem();
			item.setName(field.getName());
			item.setDataType(field.getDataType());
			item.setTableName(aTable.getName());
			mItems.add(item);
		}
	}

	/**
	 * The name of the table for this model
	 * 
	 * @return the table name
	 */
	public String getTableName() {
		return mTable.getName();
	}

	/**
	 * Set the table's view
	 * 
	 * @param aTableView
	 *            a JTable
	 */
	public void setTableView(JTable aTableView) {
		mTableView = aTableView;
	}

	/**
	 * The table's view
	 * 
	 * @return the JTable
	 */
	public JTable getTableView() {
		return mTableView;
	}

	/**
	 * Returns DBSelectTableModelItem by name
	 * 
	 * @param aName
	 *            name of item to be returned
	 * @return the item
	 */
	public DBSelectTableModelItem getItemByName(String aName) {
		for (Enumeration e = mItems.elements(); e.hasMoreElements();) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) e
					.nextElement();
			if (item.getName().equals(aName)) {
				return item;
			}
		}
		return null;
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
	 * Returns the number of rows
	 * 
	 * @return Number of rows
	 */
	public int getRowCount() {
		return mItems.size();
	}

	/**
	 * Returns the Class object for a column
	 * 
	 * @param aColumn
	 *            the column in question
	 * @return the Class of the column
	 */
	public Class getColumnClass(int aColumn) {
		return aColumn == 0 ? DBSelectTableModelItem.class : String.class;
	}

	/**
	 * Gets the current field for a row
	 * 
	 * @return DBSelectTableModelItem
	 */
	protected DBSelectTableModelItem getFieldForRow(int aRow) {
		DBSelectTableModelItem field = null;
		if (aRow < mItems.size()) {
			return (DBSelectTableModelItem) mItems.elementAt(aRow);
		}
		return null;
	}

	/**
	 * Always return false
	 */
	public boolean isCellEditable(int aRrow, int aCol) {
		return false;
	}

	/**
	 * Get the column name
	 * 
	 * @param aColumn
	 *            the column in question
	 * @return the column name
	 */
	public String getColumnName(int aColumn) {
		return COLUMN_TITLES[aColumn];
	}

	/**
	 * Gets the value of the row, column
	 * 
	 * @param aRow
	 *            the row in question
	 * @param aColumn
	 *            the column in question
	 * @return the object that that location
	 */
	public Object getValueAt(int aRow, int aColumn) {
		DBSelectTableModelItem field = getFieldForRow(aRow);

		// System.out.println("getValueAt ("+row + ","+ column+")  "+field);
		if (field != null) {
			switch (aColumn) {
			case 0:
				return field;
			case 1:
				return field.getDataType();
			default:
				return "N/A";
			}
		}
		return "";
	}

	/**
	 * Fills QueryDef from Model
	 * 
	 * @param aQueryDef
	 *            the query
	 */
	public void fillQueryDef(DBQueryDef aQueryDef) {
		for (Enumeration e = mItems.elements(); e.hasMoreElements();) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) e
					.nextElement();
			aQueryDef.addSelectItem(item);
		}
	}
}