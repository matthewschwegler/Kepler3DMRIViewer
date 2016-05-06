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

import java.awt.Color;
import java.awt.Component;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * This class will render a table cell as another table. The cell of subtable
 * will be rendered as checkbox render
 * 
 * @author Jing Tao
 * 
 */

public class TableTableCellRenderer implements TableCellRenderer {
	protected static final String DOCUMENTTYPECOL = "documenType";
	private Vector selectedServiceList = null;
	private JTable topTable = null;
	private int topRowNum = CheckBoxTableCellRenderer.DEFAUTTOPROW;

	/**
	 * Constructor with a vector(tableModel)
	 * 
	 * @param selectedServiceList
	 *            Vector
	 */
	public TableTableCellRenderer(JTable topTable, Vector selectedServiceList) {
		this.topTable = topTable;
		this.selectedServiceList = selectedServiceList;

	}// TableTableCellRenderer

	/**
	 * Method to render a cell with another table
	 * 
	 * @param table
	 *            JTable
	 * @param value
	 *            Object
	 * @param isSelected
	 *            boolean
	 * @param hasFocus
	 *            boolean
	 * @param row
	 *            int
	 * @param column
	 *            int
	 * @return Component
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		topRowNum = row;// top row number for store this table
		SelectableDocumentType[] selectedTypeList = (SelectableDocumentType[]) value;
		SelectableDocumentTypeTableModel tableModel = new SelectableDocumentTypeTableModel(
				selectedTypeList);
		JTable cellTable = new JTable(tableModel);
		cellTable.setRowHeight(ServicesDisplayPanel.CELLHEIGHT);
		// make the border is white for the cell
		cellTable.setGridColor(Color.WHITE);
		TableColumn columnOne = cellTable.getColumn(DOCUMENTTYPECOL);
		columnOne.setPreferredWidth(ServicesDisplayPanel.CELLPREFERREDWIDTH);
		CheckBoxTableCellRenderer checkboxRenderer = new CheckBoxTableCellRenderer(
				topTable, selectedServiceList, topRowNum);
		columnOne.setCellRenderer(checkboxRenderer);
		CheckBoxTableCellEditor editor = new CheckBoxTableCellEditor(topTable,
				new JCheckBox(), selectedServiceList, topRowNum);
		columnOne.setCellEditor(editor);
		TableTableCellEditor tableEditor = new TableTableCellEditor(topTable,
				new JCheckBox(), selectedServiceList);
		cellTable.setCellEditor(tableEditor);
		return cellTable;
	}// getTableCellRendererCompenent

}// TableTableCellRenderer

class SelectableDocumentTypeTableModel extends AbstractTableModel {
	private SelectableDocumentType[] list = null;

	/**
	 * Consturctor
	 * 
	 * @param list
	 *            DocumenType[]
	 */
	public SelectableDocumentTypeTableModel(SelectableDocumentType[] list) {
		this.list = list;
	}

	/**
	 * Get the number of row
	 * 
	 * @return int
	 */
	public int getRowCount() {
		if (list != null) {
			return list.length;
		} else {
			return 0;
		}
	} // getRowCount

	/**
	 * Get the number of column
	 * 
	 * @return int
	 */
	public int getColumnCount() {
		return 1;
	} // getColumnCount

	/**
	 * Get the header for given column number
	 * 
	 * @param column
	 *            int
	 * @return String
	 */
	public String getColumnName(int column) {
		return TableTableCellRenderer.DOCUMENTTYPECOL;
	} // getCoumnName

	/**
	 * Method to get value
	 * 
	 * @param rowIndex
	 *            int
	 * @param columnIndex
	 *            int
	 * @return Object
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object value = null;
		try {
			if (list != null) {
				SelectableDocumentType type = list[rowIndex];
				value = type;
			}// if

		}// try
		catch (ArrayIndexOutOfBoundsException aioobe) {
			value = null;
		} catch (NullPointerException npe) {
			value = null;
		} catch (Exception e) {
			value = null;
		}

		return value;

	} // getValueAt

	/**
	 * Make table editable
	 * 
	 * @param row
	 *            int
	 * @param column
	 *            int
	 * @return boolean
	 */
	public boolean isCellEditable(int row, int column) {
		return true;
	}// isCellEditable

}// SelectedDocumentTypeTableModel