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
import java.util.Hashtable;
import java.util.Vector;

import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;

/**
 * This class extends DBSelectTableModelBase and is used to enabled the user to
 * indicate which items will be displayed, the boolean operator for the criteria
 * and the criteria. It's JTable (view) is displayed in the lower panel of the
 * "Standard" tab.
 */
public class DBSelectTableModelStd extends DBSelectTableModelBase {
	static final String INCLUDESELECTION = "Include in Selection";

	protected Hashtable mModelHash = null;

	protected String[] COLUMN_TITLES = { "Table", "Field", "Data Type",
			INCLUDESELECTION, "Operator", "Criteria" };

	/**
	 * Constructor for Table Model
	 * 
	 * @param aSchema
	 *            the schema object
	 * @param aModelHash
	 *            a hashtable containing allthe schema's of all tables
	 */
	public DBSelectTableModelStd(DSSchemaIFace aSchema, Hashtable aModelHash) {
		super(aSchema);
		mModelHash = aModelHash;
	}

	/**
	 * This takes the table name and field name and looks up the
	 * DBSelectTableModelItem from the cahced Model, so both UIs are working
	 * from the same data model item.
	 * 
	 * @param aField
	 *            filed
	 * @param aValue
	 *            new field name
	 */
	protected void setFieldName(DBSelectTableModelItem aField, Object aValue) {
		// If "*" is selected then it will be of type DBTableField
		if (!(aValue instanceof String)) {
			aValue = aValue.toString();
		}

		// Find the current Model item (aField) and then replace it the proper
		// one
		// from the hashtable of models
		int inx = mItems.indexOf(aField);
		if (inx != -1) {
			// The rows in this model contain DBSelectTableModelItem items
			// so when the name changes we need to "look up" the new item
			// and replace the existing one.
			// NOTE: We set the diaply to false for the one we are removing.
			// if it is in the list twice (and it shouldn't be) then that is the
			// way it goes.
			DBDisplayItemIFace displayItem = getDisplayItem(aField
					.getTableName(), (String) aValue);
			if (displayItem != null
					&& displayItem instanceof DBSelectTableModelItem) {
				// aField.setDisplayed(false);
				setDisplayListCell(aField, false);
				DBSelectTableOverviewModel model = (DBSelectTableOverviewModel) mModelHash
						.get(aField.getTableName());
				model.fireTableDataChanged();
				mItems.remove(inx);
				mItems.insertElementAt(displayItem, inx);
			}
		}
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
	 * Sets Display attr in UI List cell
	 * 
	 * @param aFieldCell
	 *            the field
	 * @param aFlag
	 *            whether it is displayed
	 */
	protected void setDisplayListCell(DBSelectTableModelItem aFieldCell,
			boolean aFlag) {
		DBSelectTableOverviewModel model = (DBSelectTableOverviewModel) mModelHash
				.get(aFieldCell.getTableName());
		if (model != null) {
			// For the Intermediate Pane the Upper model is different than when
			// it is a Standard Pane.
			// For a Standard pane the upper and lower panes use the same model
			// For an Intermediate Pane the the Upper model is built from the
			// Desktop GUI
			// and the lower model is from the original schema
			//
			// So here we such the Hashtable which are from the upper model and
			// get the appropriate fild and make sure it is updated as well
			DBTableField field = getFieldFor(aFieldCell);
			if (field != null) {
				field.setDisplayed(aFlag);
				if (mSchema instanceof DBTableDesktopPane) {
					((DBTableDesktopPane) mSchema).makeDirty();
				}
			}
			aFieldCell.setDisplayed(aFlag);
			model.fireTableDataChanged();
			if (model.getTableView() != null) {
				model.getTableView().repaint();
			}
			fireTableDataChanged();
		}
	}

	/**
	 * Returns the Class object for a column
	 * 
	 * @param aCol
	 *            index of column
	 * @return the Class of the column
	 */
	public Class getColumnClass(int aCol) {
		return getValueAt(0, aCol).getClass();
	}

	/**
	 * Get the column name
	 */
	public String getColumnName(int column) {
		return COLUMN_TITLES[column];
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
	 * Return the model hashtable
	 * 
	 * 	 */
	public Hashtable getModelHashtable() {
		return mModelHash;
	}

	/**
	 * Indicates if col and row is editable
	 * 
	 * @param aRow
	 *            index of row
	 * @param aCol
	 *            index of column
	 * @return true if edittable, otherwise false
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
		case 4:
			return tableNameOK;
		case 5: {
			DBSelectTableModelItem field = getFieldForRow(aRow);
			return tableNameOK && field != null
					&& !field.getName().equals(DBUIUtils.ALL_FIELDS)
					&& !field.getOperator().equals(DBUIUtils.NO_NAME)
					&& field.getOperator().length() > 0;
		}
		}
		return false;
	}

	/**
	 * Gets the value of the row, col
	 * 
	 * @param aRow
	 *            index of row
	 * @param aCol
	 *            index of column
	 * @return the object
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
				return field.getOperator();
			case 5:
				return field.getCriteria();
			default:
				return "N/A";
			}
		}
		return "";
	}

	/**
	 * Retrieves the Model that represents this table, then looks up the Field
	 * (DBSelectTableModelItem) and returns the item
	 * 
	 * @param aTableName
	 *            name of table model
	 * @param aFieldName
	 *            name of feild to be looked up
	 * @return the display item
	 */
	protected DBSelectTableModelItem getDisplayItem(String aTableName,
			String aFieldName) {
		DBSelectTableOverviewModel model = (DBSelectTableOverviewModel) mModelHash
				.get(aTableName);
		if (model != null) {
			return model.getItemByName(aFieldName);
		}
		return null;
	}

	/**
	 * Sets a new value into the Model
	 * 
	 * @param aValue
	 *            value
	 * @param aRow
	 *            index of row
	 * @param aCol
	 *            index of column
	 */
	public void setValueAt(Object aValue, int aRow, int aCol) {
		if (aValue == null)
			return;

		DBSelectTableModelItem fieldCell = null;
		if (aRow < mItems.size()) {
			fieldCell = (DBSelectTableModelItem) mItems.elementAt(aRow);
			switch (aCol) {
			case 0: {
				String tableName = (String) aValue;
				if (!tableName.equals(DBUIUtils.NO_NAME)) {
					String fieldName = (String) mAvailFieldNames.elementAt(0);
					fieldCell.setDisplayItem(getDisplayItem(tableName,
							fieldName));
					fieldCell.setName((String) mAvailFieldNames.elementAt(0));
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
				if (aValue instanceof String) {
					fieldCell.setOperator((String) aValue);
					if (aValue.equals(DBUIUtils.NO_NAME)) {
						setCriteria(fieldCell, "");
					}
				}
				break;

			case 5:
				setCriteria(fieldCell, aValue);
				break;
			}
			this.fireTableDataChanged();
		}
	}

	/**
	 * Adds a condition
	 * 
	 * @param aSelectHash
	 *            the hashtable of table/field names
	 * @param aCond
	 *            the condition of class DBSelectTableModelItem
	 */
	private void addCondition(Hashtable aSelectHash, Object aCond,
			Hashtable aItemsToDelete) {
		if (aCond instanceof DBSelectTableModelItem) {
			DBSelectTableModelItem modelItem = (DBSelectTableModelItem) aCond;
			mItems.add(modelItem);

			int newInx = mItems.size() - 1;
			modelItem.setDisplayItem(getDisplayItem(modelItem.getTableName(),
					modelItem.getName()));

			boolean isDisplayed = false;
			if (aSelectHash != null) {
				String key = DBUIUtils.getFullFieldName(modelItem
						.getTableName(), modelItem.getName());
				if (aSelectHash.get(key) != null) {
					isDisplayed = true;
					aItemsToDelete.put(key, key);
				}
			} else {
				isDisplayed = true;
			}

			DBDisplayItemIFace item = modelItem.getDisplayItem();
			if (item instanceof DBSelectTableModelItem) {
				setDisplayListCell((DBSelectTableModelItem) item, isDisplayed);
			}
		}
	}

	/**
	 * Used to recurse the where object tree, to determine it's complexity
	 * 
	 * @param aSelectHash
	 *            the hashtable of table/field names
	 * @param aOper
	 *            a where object
	 * @param aCurrOper
	 *            a string representing the operator
	 */
	private int recurseOperator(Hashtable aSelectHash, DBWhereOperator aOper,
			String aCurrOper, Hashtable aItemsToDelete) {
		for (Enumeration e = aOper.getEnumeration(); e.hasMoreElements();) {
			DBWhereIFace item = (DBWhereIFace) e.nextElement();
			if (item instanceof DBWhereOperator) {
				DBWhereOperator oper = (DBWhereOperator) item;
				if (oper.getOperator().equals(aCurrOper)) {
					int status = recurseOperator(aSelectHash, oper, aCurrOper,
							aItemsToDelete);
					if (status != DBQueryDef.BUILD_OK) {
						return DBQueryDef.BUILD_TOO_COMPLEX_WHERE;
					}
				} else {
					return DBQueryDef.BUILD_TOO_COMPLEX_WHERE;
				}
			} else {
				addCondition(aSelectHash, item, aItemsToDelete);
			}
		}

		return DBQueryDef.BUILD_OK;
	}

	/**
	 * Build UI from the Query Definition Object
	 * 
	 * @param aQueryDef
	 *            the QueryDef to build the UI from
	 * @param aStrBuf
	 *            the out put string buffer
	 * @return returns the type of operator it is "AND"/"OR"
	 */
	public int buildFromQueryDef(DBQueryDef aQueryDef, StringBuffer aStrBuf,
			boolean aSkipJoins) {
		if (aQueryDef == null)
			return DBQueryDef.BUILD_ERROR;

		int status = DBQueryDef.BUILD_OK;
		mItems.clear();

		Hashtable selectHash = new Hashtable();
		Vector selects = aQueryDef.getSelects();
		String operStr = null;

		// Build hash of item in the "display" portion
		for (Enumeration e = selects.elements(); e.hasMoreElements();) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) e
					.nextElement();
			selectHash.put(DBUIUtils.getFullFieldName(item.getTableName(), item
					.getName()), item);
		}

		// Recurse the where clause adding them to the model
		// each one will check the has to determine whether the "display" bool
		// should be flipped
		// if so, then it removes it from the hash
		//
		// NOTE: The selectHash contains all the table.fieldNames for those
		// fields
		// in the select statement. Since this is the "Standard" tab and all the
		// conditions end up
		// in the lower panel with all the select fields we need to make sure we
		// don't put in select fields
		// when they can be "covered" by a condition field.
		//
		// Sooooo, the itemsToDelete hashtable contains all the select field
		// names that were used by the
		// condition fields and no longer need to be added on their own. The
		// addCondition and recurseOperator
		// methods will fill the itemsToDelete hashtable as it adds the
		// conditional fields to the bottom panel table.
		Hashtable itemsToDelete = new Hashtable();
		DBWhereIFace whereObj = aQueryDef.getWhere();
		if (whereObj != null) {
			if (whereObj instanceof DBWhereCondition) {
				addCondition(selectHash, (DBWhereCondition) whereObj,
						itemsToDelete);
			} else {
				DBWhereOperator operator = (DBWhereOperator) whereObj;
				operStr = operator.getOperator();
				status = recurseOperator(selectHash, operator, operStr,
						itemsToDelete);
			}
		}

		if (status != DBQueryDef.BUILD_OK)
			return status;

		// Now delete all the select fields from the hashtable because they have
		// all ready
		// been added via the conditions. If it is still in the selectHash
		// hastable then
		// it will need to be added with the "display" attribute checked
		for (Enumeration e = itemsToDelete.elements(); e.hasMoreElements();) {
			String fullName = (String) e.nextElement();
			if (selectHash.get(fullName) != null) {
				selectHash.remove(fullName);
			}
		}

		// Any items left in the hash should be added with the display set
		// pass in null for the hash
		for (Enumeration e = selectHash.elements(); e.hasMoreElements();) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) e
					.nextElement();
			addCondition(null, item, itemsToDelete);
		}

		// make sure this gets set in case the where clause is empty and
		// there are no joins
		if (operStr == null || operStr.length() == 0) {
			operStr = DBWhereOperator.AND_OPER;
		}

		if (!aSkipJoins && aQueryDef.getJoins() != null
				&& aQueryDef.getJoins().size() > 0) {
			if (operStr.equals(DBWhereOperator.AND_OPER)) {
				// Build hash of item in the "display" portion
				for (Enumeration e = aQueryDef.getJoins().elements(); e
						.hasMoreElements();) {
					DBSelectTableModelItem leftItem = (DBSelectTableModelItem) e
							.nextElement();
					DBSelectTableModelItem rightItem = (DBSelectTableModelItem) e
							.nextElement();
					// if (leftItem == null ||leftItem.getTableName() == null)
					// {
					// int x = 0;
					// }
					DBSelectTableOverviewModel model = (DBSelectTableOverviewModel) mModelHash
							.get(leftItem.getTableName());
					if (model != null) {
						DBSelectTableModelItem modelItem = model
								.getItemByName(leftItem.getName());
						if (modelItem != null) {
							DBSelectTableModelItem item = new DBSelectTableModelItem(
									leftItem.getTableName(),
									leftItem.getName(),
									modelItem.getDataType(),
									false,
									DBUIUtils.getFullFieldName(rightItem
											.getTableName(), rightItem
											.getName()),
									DBSelectTableUIStd.OPERS_TXT[DBSelectTableUIStd.EQUALS_INX],
									leftItem.getMissingValueCode());
							item.setDisplayItem(modelItem);
							mItems.add(item);
						}
					}
				}
			} else {
				status = DBQueryDef.BUILD_TOO_COMPLEX_JOINS;
			}
		}

		// make sure there is always an empty "row" at the bottom
		mItems.add(new DBSelectTableModelItem());

		// tell the UI what kind of operator we have
		aStrBuf.setLength(0);
		aStrBuf.append(operStr);

		return status;
	}

}