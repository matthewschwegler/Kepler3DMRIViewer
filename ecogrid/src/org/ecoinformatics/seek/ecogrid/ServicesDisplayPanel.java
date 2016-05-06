/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: leinfelder $'
 * '$Date: 2010-07-29 10:05:50 -0700 (Thu, 29 Jul 2010) $' 
 * '$Revision: 25179 $'
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
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.StaticResources;

/**
 * This class will represents a panel which will display services
 * 
 * @author Jing Tao
 * 
 */

public class ServicesDisplayPanel extends JScrollPane {
	public static final int LABELPREFERWIDTH = 180;
	public static final int CELLHEIGHT = 30;
	public static final int CELLPREFERREDWIDTH = 300;
	public static final int CELLMINIWIDTH = 200;
	public static final int CELLMAXWIDTH = 300;
	public static final String SERVICENAMECOL = 
		StaticResources.getDisplayString("preferences.data.serviceName", "Service Name");
	public static final String LOCATIONCOL = 
		StaticResources.getDisplayString("preferences.data.location", "Location");
	public static final String DOCUMENTTYPECOL = 
		StaticResources.getDisplayString("preferences.data.documentType", "Document Type");

	private static final boolean ROWSELECTION = false;
	private static final boolean CELLSELECTION = false;
	private static final boolean COLUMNSELECTION = false;

	// private EcoGridServicesController serviceController = null;
	private Vector selectedServiceList = null;
	private ServicesDisplayTableModel tableModel = null;
	private JTable table = null;
	private CheckBoxTableCellRenderer checkBoxRenderer = null;
	private TableTableCellRenderer tableRenderer = null;
	// private JButton responseButton = null;

	// If we need modify columns in table, we need modify this array
	// and getValueAt method in ServicesDisplayTableModel class
	public static final String[] HEADNAME = { SERVICENAMECOL, DOCUMENTTYPECOL };
	private static final int ROWNUMBER = 4;

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.ServicesDisplayPanel");
	}

	/**
	 * Constructor of ServicesDisplayPane
	 * 
	 * @param Vector
	 *            selectedServiceList
	 */
	public ServicesDisplayPanel(Vector selectedServiceList) {
		this.selectedServiceList = selectedServiceList;
		init();
	}// ServiceDispalyPane

	/*
	 * Method to init the pane
	 */
	private void init() {
		this.setPreferredSize(new Dimension(HEADNAME.length
				* CELLPREFERREDWIDTH, CELLHEIGHT));
		this.getViewport().setBackground(Color.WHITE);

		tableModel = new ServicesDisplayTableModel(selectedServiceList,
				HEADNAME);

		table = new JTable(tableModel);
		table.setRowSelectionAllowed(ROWSELECTION);
		table.setColumnSelectionAllowed(COLUMNSELECTION);
		table.setCellSelectionEnabled(CELLSELECTION);

		Vector rowHeightFactorList = tableModel.getRowHeightFactor();
		initRowHeight(rowHeightFactorList);
		initColumnWidth();

		// first col(service name render use checkbox renderer
		// second col(location) use default rendered
		// third col(doctype) use table rendered
		checkBoxRenderer = new CheckBoxTableCellRenderer(table,
				selectedServiceList, CheckBoxTableCellRenderer.DEFAUTTOPROW);
		tableRenderer = new TableTableCellRenderer(table, selectedServiceList);

		TableColumn serviceNameCol = table.getColumn(SERVICENAMECOL);
		serviceNameCol.setCellRenderer(checkBoxRenderer);
		serviceNameCol.setCellEditor(new CheckBoxTableCellEditor(table,
				new JCheckBox(), selectedServiceList,
				CheckBoxTableCellRenderer.DEFAUTTOPROW));

		TableColumn docTypeCol = table.getColumn(DOCUMENTTYPECOL);
		docTypeCol.setCellRenderer(tableRenderer);
		docTypeCol.setCellEditor(new TableTableCellEditor(table,
				new JCheckBox(), selectedServiceList));

		this.getViewport().add(table);
	}// init

	/*
	 * This method picks good column sizes.
	 */
	private void initColumnWidth() {
		TableColumn column = null;
		for (int i = 0; i < tableModel.getColumnCount(); i++) {
			column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(CELLPREFERREDWIDTH);
			// column.setMaxWidth(CELLMAXWIDTH);
			// column.setMinWidth(CELLMINIWIDTH);
		}// for
	}// initColumnSizes

	/*
	 * Initial the row height base on given row height factor. If the factor is
	 * null, then table will have unique height
	 */
	private void initRowHeight(Vector factorList) {
		if (factorList != null) {
			int rowNumber = table.getRowCount();
			int listSize = factorList.size();
			if (rowNumber <= 0) {
				table.setRowHeight(CELLHEIGHT);
			}// if
			else {
				for (int i = 0; i < rowNumber; i++) {
					if (i < listSize) {
						int factor = ((Integer) factorList.elementAt(i))
								.intValue();
						log.debug("The factor for row " + i + " is " + factor);
						table.setRowHeight(i, factor * CELLHEIGHT);
					} else {
						table.setRowHeight(i, CELLHEIGHT);
					}
				}// for
			}// else
		}// if
		else {
			table.setRowHeight(CELLHEIGHT);
		}//
	}// initRowHeight

	/**
	 * This method will return a vector which will a whole service was selected
	 * We consider the whole services was seleted: The service name was selected
	 * and all document types in the service were selected.
	 * 
	 * @return Vector
	 */
	public Vector getAllSelectedServicesList() {
		Vector list = new Vector();
		Vector selectedList = tableModel.getSelectedServicesList();
		if (selectedList != null) {
			int length = selectedList.size();
			for (int i = 0; i < length; i++) {
				SelectableEcoGridService service = (SelectableEcoGridService) selectedList
						.elementAt(i);
				SelectableServiceName name = service.getSelectableServiceName();
				if (name != null && !name.getIsSelected()) {
					SelectableDocumentType[] types = service
							.getSelectableDocumentTypeList();
					if (isDocuementTypeAllSelected(types)) {
						list.add(service);
					}// fi
				} else if (name != null && name.getIsSelected()) {
					list.add(service);
				}
			}// for
		}// if
		return list;
	}// getSelectedServicesList

	/**
	 * This method will return a vector which will a whole service was
	 * unselected We consider the whole services was seleted: 1. both service
	 * name and all document types in the service were unselected. 2. service
	 * name is selected, but the all documents types are unselected
	 * 
	 * @return Vector
	 */
	public Vector getAllUnSelectedServicesList() {
		Vector list = new Vector();
		Vector selectedList = tableModel.getSelectedServicesList();
		if (selectedList != null) {
			int length = selectedList.size();
			for (int i = 0; i < length; i++) {
				SelectableEcoGridService service = (SelectableEcoGridService) selectedList
						.elementAt(i);
				SelectableServiceName name = service.getSelectableServiceName();
				// if (name != null && !name.getIsSelected())
				// {
				SelectableDocumentType[] types = service
						.getSelectableDocumentTypeList();
				if (isDocuementTypeAllUnSelected(types)) {
					list.add(service);
				}// fi
				// }//else if
			}// for
		}// if
		return list;
	}// getSelectedServicesList

	/*
	 * Method to judge if document types all selected
	 */
	private boolean isDocuementTypeAllSelected(
			SelectableDocumentType[] documentTypes) {
		boolean allSelected = true;
		if (documentTypes == null) {
			allSelected = false;
			return allSelected;
		}// if
		else {
			int length = documentTypes.length;
			for (int i = 0; i < length; i++) {
				SelectableDocumentType type = documentTypes[i];
				if (type != null && !type.getIsSelected()) {
					allSelected = false;
					break;
				}
			}//
			return allSelected;
		}// else
	}// isDocumentTypeAllSelected

	/*
	 * Method to judge if document types all unselected
	 */
	private boolean isDocuementTypeAllUnSelected(
			SelectableDocumentType[] documentTypes) {
		boolean allUnSelected = true;
		if (documentTypes == null) {
			allUnSelected = false;
			return allUnSelected;
		}// if
		else {
			int length = documentTypes.length;
			for (int i = 0; i < length; i++) {
				SelectableDocumentType type = documentTypes[i];
				if (type != null && type.getIsSelected()) {
					allUnSelected = false;
					break;
				}
			}//
			return allUnSelected;
		}// else
	}// isDocumentTypeAllSelected

	/**
	 * Method to return a partial selection serivce list. The service in this
	 * list will only have the selected document type.(We consider the following
	 * case as partial selection: service was selected and only part of document
	 * types was selected)
	 * 
	 * @return Vector
	 */
	public Vector getPartialSelectedServicesList() {
		Vector list = getPartilServiceList(true);
		return list;
	}// getPartialSelectedServiceList

	/**
	 * This method will return a partial selected service list. The service only
	 * have the unselected document type
	 * 
	 * @return Vector
	 */
	public Vector getPartialUnselectedServiceList() {
		Vector list = getPartilServiceList(false);
		return list;
	}// getPartialUnSelectedServcieList

	private Vector getPartilServiceList(boolean selected) {
		Vector list = new Vector();
		Vector selectedList = tableModel.getSelectedServicesList();
		if (selectedList != null) {
			int length = selectedList.size();
			for (int i = 0; i < length; i++) {
				SelectableEcoGridService service = (SelectableEcoGridService) selectedList
						.elementAt(i);
				SelectableServiceName name = service.getSelectableServiceName();
				if (name != null && name.getIsSelected()) {
					SelectableDocumentType[] typeList = service
							.getSelectableDocumentTypeList();
					Vector newTypeList = new Vector();
					if (typeList != null) {
						int size = typeList.length;
						int count = 0;
						for (int j = 0; j < size; j++) {
							SelectableDocumentType type = typeList[j];
							if (selected) {
								if (type != null && type.getIsSelected()) {
									count++;
									newTypeList.add(type);
								} // if
							} else {
								if (type != null && !type.getIsSelected()) {
									count++;
									newTypeList.add(type);
								}
							}
						}// for
						if (count > 0 && count < size) {
							DocumentType[] newTypes = DocumentType
									.tranformVectorToArray(newTypeList);
							service.setDocumentTypeList(newTypes);
							list.add(service);
						}

					}// if
				}// if
			}// for
		}// if
		return list;
	}

	public static void main(String[] args) {
		int width = 600;
		int height = 300;
		EcoGridServicesController controller = EcoGridServicesController
				.getInstance();
		Vector unSelectedserviceList = controller.getServicesList();
		// transfer to selectedSerive list(object is SelectedEcoGridService now)
		Vector selectedServicesList = SelectableEcoGridService
				.transferServiceVectToDefaultSelectedServiceVect(unSelectedserviceList);

		ServicesDisplayPanel serviceDisplayPane = new ServicesDisplayPanel(
				selectedServicesList);
		// set up a frame
		JFrame frame = new JFrame("SwingApplication");
		frame.setSize(width, height);
		frame.getContentPane().add(serviceDisplayPane, BorderLayout.CENTER);
		// Finish setting up the frame, and show it.
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setVisible(true);

	}// main

}// ServicesDisplayPane