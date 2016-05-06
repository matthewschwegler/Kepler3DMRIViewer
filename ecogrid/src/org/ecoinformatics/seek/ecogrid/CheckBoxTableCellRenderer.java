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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class will render a table cell as panel - label + a check box
 * 
 * @author Jing Tao
 * 
 */

public class CheckBoxTableCellRenderer implements TableCellRenderer {

	private static final int FONTSIZE = 12;
	private static final String FONTNAME = "TableCellText";
	private static final char SEPERATOR = '.';

	public static final int DEFAUTTOPROW = -1;

	private JTable jTable = null;
	private JTable topTable = null;
	private Vector selectedServiceList = null;
	private int topRowNum = DEFAUTTOPROW;
	private static Log log;
	private static boolean isDebugging;

	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.CheckBoxTableCellRenderer");
		isDebugging = log.isDebugEnabled();
	}

	/**
	 * Default Constructor
	 */
	public CheckBoxTableCellRenderer(JTable topTable,
			Vector selectedServiceList, int topRowNum) {
		this.topTable = topTable;
		this.selectedServiceList = selectedServiceList;
		this.topRowNum = topRowNum;

	}// CheckBoxTableCellRenderer

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		jTable = table;

		JPanel cellPanel = new JPanel();
		cellPanel.setBorder(new LineBorder(Color.lightGray, 1));
		cellPanel.setBackground(Color.WHITE);
		cellPanel.setPreferredSize(new Dimension(
				ServicesDisplayPanel.CELLPREFERREDWIDTH,
				ServicesDisplayPanel.HEIGHT));

		SelectableDocumentType selectedDocumentType = null;
		boolean isChecked = false;
		boolean isEnable = true;
		String text = null;
		if (value != null && value instanceof SelectableObjectInterface) {
			SelectableObjectInterface selectedObj = (SelectableObjectInterface) value;
			text = selectedObj.getSelectableObjectLabel();
			isChecked = selectedObj.getIsSelected();
			isEnable = selectedObj.getEnabled();
		}

		/*
		 * label = (JLabel)renderer.getTableCellRendererComponent(table, text,
		 * isSelected, hasFocus, row, column);
		 */
		JLabel label = new JLabel(text);
		label.setFont(new Font(FONTNAME, Font.PLAIN, FONTSIZE));
		label.setPreferredSize(new Dimension(
				ServicesDisplayPanel.LABELPREFERWIDTH,
				ServicesDisplayPanel.HEIGHT));
		// set a check box name
		String checkBoxName = "" + topRowNum + SEPERATOR + row;
		JCheckBox checkBox = new JCheckBox();
		checkBox.setName(checkBoxName);
		checkBox.setBackground(Color.WHITE);
		checkBox.setSelected(isChecked);
		CheckBoxListener listener = new CheckBoxListener();
		checkBox.addItemListener(listener);
		// checkBox.setEnabled(false);

		/*
		 * if (topRowNum != DEFAUTTOPROW ) { // for sub table we need to set up
		 * check box enable status checkBox.setEnabled(isEnable); }//if
		 */

		// add the label and checkbox to jpanel which has a border layout
		// manager
		BorderLayout layoutManager = new BorderLayout();
		cellPanel.setLayout(layoutManager);
		cellPanel.add(label, BorderLayout.CENTER);
		cellPanel.add(checkBox, BorderLayout.WEST);
		return cellPanel;
	}// getTableCellRender

	/*
	 * This method will parse a checkbox name string "topRowNumber.rowNumber"
	 * for top table, topNumber should be -1 and rowNumber is the service
	 * vector's element number. For the subtable(documentType), the topRowNumber
	 * is service'vector element number and row number is array number in
	 * document type array
	 */
	private RowNumberObject parseCheckBoxName(String checkBoxName) {
		RowNumberObject rowNumberObj = null;
		try {
			if (checkBoxName != null) {
				checkBoxName = checkBoxName.trim();
				int seperatorPosition = checkBoxName.indexOf(SEPERATOR);
				String topRowStr = checkBoxName.substring(0, seperatorPosition);
				String rowStr = checkBoxName.substring(seperatorPosition + 1,
						checkBoxName.length());
				if (isDebugging) {
					log.debug("The top row number is " + topRowStr);
					log.debug("The row number is " + rowStr);
				}
				int topRow = (new Integer(topRowStr)).intValue();
				int row = (new Integer(rowStr)).intValue();
				rowNumberObj = new RowNumberObject(topRow, row);
			}
		} catch (Exception e) {
			log.debug("Couldn't parse check box name ", e);
		}
		return rowNumberObj;
	}

	/*
	 * When use checked serviceName checkbox, every document type which belong
	 * to thsi serviceName should checked and checkbox will be disalbed
	 */
	private void checkAllDocumentTypes(int rowNumber) {
		boolean checked = true;
		handleAllDocumentTypes(rowNumber, checked);
	}// checkedAllDocumentTypes

	/*
	 * When user unchecked serviceName checkbox, every document type which
	 * belong to this serviceName should be unchecked and checkbox will be
	 * enable
	 */
	private void uncheckAllDocumentTypes(int rowNumber) {
		boolean checked = false;
		handleAllDocumentTypes(rowNumber, checked);
	}// uncheckedAllDocumentTypes

	/*
	 * This method will handle check and unchecked, enable and disable document
	 * types checkbox
	 */
	private void handleAllDocumentTypes(int rowNumber, boolean checked) {
		SelectableEcoGridService service = (SelectableEcoGridService) selectedServiceList
				.elementAt(rowNumber);
		// set boolean value for every document list
		SelectableDocumentType[] typeList = service
				.getSelectableDocumentTypeList();
		if (typeList != null) {
			int length = typeList.length;
			// change the tableModel value for documenttype list
			for (int i = 0; i < length; i++) {
				SelectableDocumentType type = typeList[i];
				if (type != null) {
					type.setIsSelected(checked);
					// type.setEnabled(checked);
				}// if
			}// for
			// repaint table
			if (jTable != null) {
				jTable.repaint();
			}
		}// if

	}// handleAllDocumentTypes

	/* Class to listen for ItemEvents */
	private class CheckBoxListener implements ItemListener {
		public void itemStateChanged(ItemEvent event) {
			Object object = event.getItemSelectable();
			JCheckBox checkBox = (JCheckBox) object;
			String checkBoxName = checkBox.getName();
			RowNumberObject rowNumberObj = parseCheckBoxName(checkBoxName);
			int topRowNumber = -1;
			int rowNumber = 0;
			if (rowNumberObj != null) {
				topRowNumber = rowNumberObj.getTopRowNumber();
				rowNumber = rowNumberObj.getRowNumber();
			}

			SelectableEcoGridService service = null;
			if (topRowNumber == DEFAUTTOPROW) {
				// this is for top table cell(serive Name)
				service = (SelectableEcoGridService) selectedServiceList
						.elementAt(rowNumber);
				if (service != null) {
					SelectableServiceName serviceName = service
							.getSelectableServiceName();
					if (serviceName != null) {
						if (serviceName.getIsSelected()) {
							serviceName.setIsSelected(false);
							uncheckAllDocumentTypes(rowNumber);
						} else {
							serviceName.setIsSelected(true);
							checkAllDocumentTypes(rowNumber);
						}
						service.setSelectableServiceName(serviceName);
					}// if
				}// if
			}// if
			else {
				// this is for sub table cell(document type array
				service = (SelectableEcoGridService) selectedServiceList
						.elementAt(topRowNumber);
				SelectableDocumentType[] docTypeList = service
						.getSelectableDocumentTypeList();
				SelectableDocumentType docType = null;
				if (docTypeList != null) {
					docType = docTypeList[rowNumber];
					if (docType != null) {
						if (docType.getIsSelected()) {
							docType.setIsSelected(false);
						} else {
							docType.setIsSelected(true);
						}
					}
					// if all document type are unselected then we will
					// unselected
					// service name and make document type selectiong box
					// disable
					if (isAllDocTypeUnSelected(docTypeList)) {

						SelectableServiceName serviceName = service
								.getSelectableServiceName();
						serviceName.setIsSelected(false);
						service.setSelectableServiceName(serviceName);
						SelectableDocumentType[] typeLists = service
								.getSelectableDocumentTypeList();
						int length = typeLists.length;
						for (int i = 0; i < length; i++) {
							SelectableDocumentType type = typeLists[i];
							// type.setEnabled(false);
						}

						if (topTable != null) {
							topTable.repaint();
						}
					} else if (!(service.getSelectableServiceName()
							.getIsSelected())) {
						// if we selected a doctype, but service name is not
						// selected
						// we should set up service selected
						service.getSelectableServiceName().setIsSelected(true);
						if (topTable != null) {
							topTable.repaint();
						}
					}
				}// if
			}// else
		}// itemStateChanged

	}// CheckBoxListner

	/*
	 * This method will to make sure if all document type in a service are
	 * unselected
	 */
	private boolean isAllDocTypeUnSelected(SelectableDocumentType[] docTypeList) {
		boolean allUnSelected = true;
		if (docTypeList == null) {
			return allUnSelected;
		}

		int size = docTypeList.length;
		for (int i = 0; i < size; i++) {
			SelectableDocumentType type = docTypeList[i];
			if (type.getIsSelected()) {
				allUnSelected = false;
				break;
			}
		}
		return allUnSelected;
	}

	/*
	 * This class represents a object which inclue both topRowNumber and
	 * rowNumber If topRowNumber is -1, this means it is top table cell and row
	 * number is service vector's element number. If the topRowNumber is not -1,
	 * then the topRowNumber will be service vector's element number, the
	 * rowNumber now will be the array number in documentType in one service
	 */
	private class RowNumberObject {
		private int topRowNumber = -1;
		private int rowNumber = 0;

		/**
		 * Contructor of RowNumberObject
		 * 
		 * @param topRowNumber
		 *            int
		 * @param rowNumber
		 *            int
		 */
		public RowNumberObject(int topRowNumber, int rowNumber) {
			this.topRowNumber = topRowNumber;
			this.rowNumber = rowNumber;
		}// RowNumberObject

		/**
		 * Method to get top row number
		 * 
		 * @return int
		 */
		public int getTopRowNumber() {
			return topRowNum;
		}// getTopRowNumber

		/**
		 * Method to get row number
		 * 
		 * @return int
		 */
		public int getRowNumber() {
			return rowNumber;
		}// getRowNumber
	}// RowNumberObject

}// CheckBoxForSubTableListner