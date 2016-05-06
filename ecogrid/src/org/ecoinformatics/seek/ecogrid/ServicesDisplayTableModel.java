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

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class will represent a table model for displaying service list
 * 
 * @author Jing Tao
 * 
 */

public class ServicesDisplayTableModel extends AbstractTableModel {
	private Vector selectedServicesList = null;
	private String[] headerLabel = null;
	private Vector rowHeightFactor = new Vector();

	private static final int DEFAUTFACTOR = 1;

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.ServicesDisplayTableModel");
	}

	/**
	 * Constructor for the table model
	 * 
	 * @param selectedServiceList
	 *            Vector real data
	 * @param headerLabel
	 *            String[] label will be shown in header
	 */
	public ServicesDisplayTableModel(Vector selectedServiceList,
			String[] headerLabel) {
		this.selectedServicesList = selectedServiceList;
		this.headerLabel = headerLabel;
		generateRowHeightFactor();
	}// ServiceDisplayTableModel

	/*
	 * Method to get height factor for a row(the number of doctype in one serive
	 */
	private void generateRowHeightFactor() {
		if (selectedServicesList != null) {
			int size = selectedServicesList.size();
			for (int i = 0; i < size; i++) {
				SelectableEcoGridService service = (SelectableEcoGridService) selectedServicesList
						.elementAt(i);
				SelectableDocumentType[] typeList = service
						.getSelectableDocumentTypeList();
				if (typeList != null && typeList.length > 0) {
					rowHeightFactor.add(i, new Integer(typeList.length));
				} else {
					rowHeightFactor.add(i, new Integer(DEFAUTFACTOR));
				}
			}// for
		}// if
	}// generateRowHeightFactor

	/**
	 * Method to get selected service list
	 * 
	 * @return Vector
	 */
	public Vector getSelectedServicesList() {
		return this.selectedServicesList;
	}

	/**
	 * Method to get row height factor
	 * 
	 * @return Vector
	 */
	public Vector getRowHeightFactor() {
		return rowHeightFactor;
	}

	/**
	 * Get the number of row
	 * 
	 * @return int
	 */
	public int getRowCount() {
		if (selectedServicesList != null) {
			return selectedServicesList.size();
		} else {
			return 0;
		}
	}// getRowCount

	/**
	 * Get the number of column
	 * 
	 * @return int
	 */
	public int getColumnCount() {
		if (headerLabel != null) {
			return headerLabel.length;
		} else {
			return 0;
		}
	}// getColumnCount

	/**
	 * Get the header for given column number
	 * 
	 * @param column
	 *            int
	 * @return String
	 */
	public String getColumnName(int column) {
		if (headerLabel != null) {
			return headerLabel[column];
		} else {
			return null;
		}
	}// getCoumnName

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
			SelectableEcoGridService service = (SelectableEcoGridService) selectedServicesList
					.elementAt(rowIndex);
			String columnName = headerLabel[columnIndex];
			if (columnName != null) {
				if (columnName.equals(ServicesDisplayPanel.SERVICENAMECOL)) {
					value = service.getSelectableServiceName();
				} else if (columnName.equals(ServicesDisplayPanel.LOCATIONCOL)) {
					value = service.getEndPoint();
				} else if (columnName
						.equals(ServicesDisplayPanel.DOCUMENTTYPECOL)) {
					value = service.getSelectableDocumentTypeList();
				}
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

	}// getValueAt

	/**
	 * Return the Class for each column so that they can be rendered correctly.
	 */
	public Class getColumnClass(int c) {
		Class currentClass = null;
		try {
			currentClass = this.getValueAt(0, c).getClass();
		} catch (NullPointerException npe) {
			try {
				currentClass = Class.forName("java.lang.String");
			} catch (ClassNotFoundException cnfe) {
				log.debug("Error in getColumnClass ", cnfe);
			}
		}
		return currentClass;
	}// getColumnClass

	/**
	 * Method for table editable
	 * 
	 * @param row
	 *            int
	 * @param column
	 *            int
	 * @return boolean
	 */
	public boolean isCellEditable(int row, int column) {
		return true;
	}
}// ServiceDisplayTableModel