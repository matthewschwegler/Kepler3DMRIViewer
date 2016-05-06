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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;

import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;

/**
 * This class shows a split view with a set of table schemas as draggable tables
 * in the upper pane. The the "from" clause of the select can be created by
 * dragging fields from one table to the next to form the "join" relationships.
 * And a two tab control in the lower pane. The Select pane enables the user to
 * indicate which fields will be displayed and what the conditions will be for
 * each of the fields.
 * 
 * The Where pane enables the user to specify a complex conditional.
 */
public class QBSplitPaneAdvanced extends JPanel implements
		ListSelectionListener, QBBuilderInterface {
	protected JSplitPane mSplitPane = null;
	protected DSSchemaIFace mSchema = null;
	protected DBTableDesktopPane mDesktop = null;
	protected DBTableJoin mTableJoins = null;
	protected DBWherePanel mWherePanel = null;
	protected JPanel mSelectPanel = null;
	protected DBSelectTableUIAdv mTableView = null;
	protected DBSelectTableModelAdv mTableModel = null;
	protected JButton mAddBtn = null;
	protected JList mTableList = null;
	protected TableModelListener mTableModelListener = null;

	/**
	 * QBSplitPaneAdvanced Constructor
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aListener
	 *            a listener of changes to the overall model
	 */
	public QBSplitPaneAdvanced(DSSchemaIFace aSchema,
			TableModelListener aListener) {
		mSchema = aSchema;

		setLayout(new BorderLayout());

		mSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				createUpperPanel(aListener), createLowerPanel());
		mSplitPane.setContinuousLayout(true);
		mSplitPane.setOneTouchExpandable(true);
		mSplitPane.setDividerLocation(230);

		add(mSplitPane, BorderLayout.CENTER);
	}

	/**
	 * Sets the Model Listener appropriately
	 * 
	 * @param aTblModelListener
	 *            a listener
	 */
	public void setTableModelListener(TableModelListener aTblModelListener) {
		mTableModelListener = aTblModelListener;
		mTableModel.addTableModelListener(aTblModelListener);
		mWherePanel.setModelListener(aTblModelListener);
	}

	/**
	 * Do Clean up
	 */
	public void shutdown() {
		mTableList.removeListSelectionListener(this);
		mTableView.removeFieldChangeListener(mDesktop);
		mTableModel.removeTableModelListener(mTableModelListener);
		mWherePanel.setModelListener(null);
		mDesktop.setTableModelListener(null);

		mTableView.setModel(null);
		Vector tables = mDesktop.getTables();
		if (tables != null && tables.size() > 0) {
			for (Enumeration et = tables.elements(); et.hasMoreElements();) {
				mTableModel.removeTableModelListener((TableModelListener) et
						.nextElement());
			}
		}
		// Clean up
		mSplitPane = null;
		mSchema = null;
		mDesktop = null;
		mTableJoins = null;
		mWherePanel = null;
		mSelectPanel = null;
		mTableView = null;
		mTableModel = null;
		mAddBtn = null;
		mTableList = null;
		mTableModelListener = null;
	}

	/**
	 * Return Schema
	 * 
	 * @return the schema
	 */
	public DSSchemaIFace getSchema() {
		return mDesktop;
	}

	/**
	 * Creates the DesktopPane that contains all the tables with "links" or
	 * joins.
	 * 
	 * @param aListener
	 *            the listener for the overall model changes
	 * @return the component representing the upper pane
	 */
	public JComponent createUpperPanel(TableModelListener aListener) {
		mDesktop = new DBTableDesktopPane();
		mDesktop.setSchema(mSchema);
		mDesktop.setTableModelListener(aListener);

		// Create the mDesktop pane
		mTableJoins = mDesktop.getTableJoins();

		JScrollPane desktopScroller = new JScrollPane();
		desktopScroller.getViewport().add(mDesktop);

		DefaultListModel listModel = new DefaultListModel();
		Vector tables = mSchema.getTables();
		if (tables != null && tables.size() > 0) {
			for (Enumeration et = tables.elements(); et.hasMoreElements();) {
				DSTableIFace table = (DSTableIFace) et.nextElement();
				listModel.addElement(table.getName());
			}
		}

		JPanel rightSidePanel = new JPanel(new BorderLayout());
		mTableList = new JList(listModel);
		mTableList.addListSelectionListener(this);
		mTableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		MouseListener mouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					mDesktop.addTableToWorkspace((String) mTableList
							.getSelectedValue());
				}
			}
		};
		mTableList.addMouseListener(mouseListener);

		JScrollPane scroller = new JScrollPane();
		scroller.getViewport().add(mTableList);

		rightSidePanel.add(scroller, BorderLayout.CENTER);
		rightSidePanel.add(new JLabel("Available Tables:"), BorderLayout.NORTH);

		mAddBtn = new JButton("<- Add");
		rightSidePanel.add(mAddBtn, BorderLayout.SOUTH);

		valueChanged(null); // enables/disables the add button

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				desktopScroller, rightSidePanel);
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(true);

		mAddBtn.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				mDesktop.addTableToWorkspace((String) mTableList
						.getSelectedValue());
			}
		});

		return splitPane;
	}

	/**
	 * Creates the "Select" panel for discribing what fields will have their
	 * values displayed
	 * 
	 * @param aSchema
	 *            the schema
	 * @return the component representing the select pane
	 */
	public JPanel createSelectForDisplayPanel(DSSchemaIFace aSchema) {
		JPanel panel = new JPanel(new BorderLayout());

		// Create table mode and view for the DB Tables
		mTableView = new DBSelectTableUIAdv();
		mTableModel = new DBSelectTableModelAdv(aSchema);
		mTableView.setColumnSelectionAllowed(false);
		mTableView.setRowSelectionAllowed(true);
		mTableView.setCellSelectionEnabled(true);

		Vector tables = mDesktop.getTables();
		mTableView.setModel(mTableModel);
		for (int i = 0; i < tables.size(); i++) {
			mTableModel.addTableModelListener((TableModelListener) tables
					.elementAt(i));
		}
		mTableView.installEditors();

		JScrollPane scrollpane = new JScrollPane(mTableView);

		panel.add(scrollpane, BorderLayout.CENTER);
		return panel;
	}

	/**
	 * Creates the lower panel that contains the tabs "Select" and "Where"
	 * 
	 * @param aSchema
	 *            the schema
	 * @return the component representing the lower pane
	 */
	public JComponent createLowerPanel() {
		// create tab
		JTabbedPane tabbedpane = new JTabbedPane();
		mWherePanel = new DBWherePanel(mDesktop);
		mSelectPanel = createSelectForDisplayPanel(mDesktop);
		tabbedpane.add("Select", mSelectPanel);
		tabbedpane.add("Where", mWherePanel);
		return tabbedpane;
	}

	/**
	 * (future work)
	 * 
	 * @param aList
	 * @param aTableName
	 * 	 */
	/*
	 * private DBJoinPrcTable getTable(Vector aList, String aTableName) { for
	 * (Enumeration et = aList.elements(); et.hasMoreElements();) {
	 * DBJoinPrcTable item = (DBJoinPrcTable)et.nextElement(); if
	 * (item.getTable().getName().equals(aTableName)) { return item; } } return
	 * null; }
	 */

	/**
	 * (future work) Adds a "join" condition
	 * 
	 * @param aList
	 * @param aField
	 * @param aJoinItem
	 */
	/*
	 * private void addJoinToTables(Vector aList, DBTableField aField,
	 * DBTableJoinItem aJoinItem) { DBJoinPrcTable item = getTable(aList,
	 * aField.getTable().getName()); if (item == null) { item = new
	 * DBJoinPrcTable(aField.getTable()); aList.add(item); }
	 * item.add(aJoinItem); }
	 */

	/**
	 * Create SQL string
	 */
	public String createSQL() {
		Hashtable tableNames = new Hashtable();
		StringBuffer strBuf = new StringBuffer("SELECT ");
		DBSelectTableModelAdv model = (DBSelectTableModelAdv) mTableView
				.getModel();
		int displayCnt = 0;
		for (int i = 0; i < model.getRowCount(); i++) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) model
					.getFieldForRow(i);
			if (item.isDisplayed()) {
				tableNames.put(item.getTableName(), item.getTableName());
				displayCnt++;
			}
		}
		if (displayCnt == 0)
			return "No valid SQL to generate";

		displayCnt = 0;
		for (int i = 0; i < model.getRowCount(); i++) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) model
					.getFieldForRow(i);
			if (item.isDisplayed()) {
				if (displayCnt > 0) {
					strBuf.append(", ");
				}
				displayCnt++;
				strBuf.append(DBUIUtils.getFullFieldName(item.getTableName(),
						item.getName()));
				tableNames.put(item.getTableName(), item.getTableName());
			}
		}
		strBuf.append(" FROM ");

		StringBuffer whereStr = new StringBuffer();
		Vector joins = mTableJoins.getJoins();
		int cnt = 0;
		for (Enumeration et = joins.elements(); et.hasMoreElements();) {
			if (cnt > 0) {
				whereStr.append(" AND ");
			}
			cnt++;
			DBTableJoinItem joinItem = (DBTableJoinItem) et.nextElement();
			whereStr.append(DBUIUtils.getFullFieldName(joinItem.getItemLeft()));
			whereStr.append(" = ");
			whereStr
					.append(DBUIUtils.getFullFieldName(joinItem.getItemRight()));
			String tblName = joinItem.getItemLeft().getTable().getName();
			tableNames.put(tblName, tblName);
			tblName = joinItem.getItemRight().getTable().getName();
			tableNames.put(tblName, tblName);
		}

		displayCnt = 0;
		for (Enumeration et = tableNames.elements(); et.hasMoreElements();) {
			String tableName = (String) et.nextElement();
			if (tableName.indexOf(' ') != -1) {
				tableName = "[" + tableName + "]";
			}
			if (displayCnt > 0) {
				strBuf.append(", ");
			}
			displayCnt++;
			strBuf.append(tableName);
		}
		strBuf.append(" WHERE ");
		strBuf.append(whereStr);
		String wherePanelStr = mWherePanel.generateWhereSQL(true);
		String noSpaces = wherePanelStr.trim();
		if (noSpaces.length() > 0) {
			strBuf.append(" AND ");
			strBuf.append(wherePanelStr);
		}

		return strBuf.toString();

	}

	/**
	 * Makes the entire Desktop object repaint itself
	 * 
	 */
	protected void refresh() {
		// mDesktop.refresh();
		if (mDesktop != null)
			mDesktop.makeDirty();
	}

	/**
	 * Fill the hastable with the table names
	 * 
	 * @param aWhereObj
	 *            the where object
	 * @param aHashTable
	 *            the hastable
	 */
	protected void fillHashWithTableNamesForWhere(DBWhereIFace aWhereObj,
			Hashtable aHashTable) {
		if (aWhereObj == null)
			return;

		if (aWhereObj instanceof DBWhereCondition) {
			String tblName = ((DBWhereCondition) aWhereObj).getTableName();
			if (tblName.length() > 0)
				aHashTable.put(tblName, tblName);

		} else {
			DBWhereOperator whereOper = (DBWhereOperator) aWhereObj;
			for (Enumeration e = whereOper.getEnumeration(); e
					.hasMoreElements();) {
				fillHashWithTableNamesForWhere((DBWhereIFace) e.nextElement(),
						aHashTable);
			}
		}
	}

	// ---------------------------------------------------
	// -- ListSelectionListener
	// ---------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {
		mAddBtn.setEnabled(mTableList.getSelectedIndex() != -1);
	}

	// ---------------------------------------------------
	// -- QBBuilderInterface
	// ---------------------------------------------------

	/**
	 * 
	 * @return returns the "type" of builder it is as defined by the constants
	 *         in this interface
	 */
	public int getType() {
		return QBBuilderInterface.ADVANCED;
	}

	/**
	 * A textual name for this builder
	 * 
	 * @return string of the name
	 */
	public String getName() {
		return "Advanced";
	}

	/**
	 * Returns whether their will be data loss if this query is converted to a
	 * "standard" query meaning we have defined some "where" items, but have not
	 * created any "display" items
	 * 
	 * @return true if possible data loss
	 */
	public boolean possibleDataLoss() {
		boolean atLeastOneForDisplay = false;
		for (int i = 0; i < mTableModel.getRowCount(); i++) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) mTableModel
					.getFieldForRow(i);
			if (item.isDisplayed()) {
				atLeastOneForDisplay = true;
			}
		}

		return mTableModel.getRowCount() > 1 && !atLeastOneForDisplay
				&& mWherePanel.getModel().getSize() > 0;
	}

	/**
	 * This checks to see if this type of builder can convert the internal SQL
	 * to a more complex or less complex form.
	 * 
	 * This is typically called when switching from a more complex builder to a
	 * less complex builder
	 * 
	 * @param aBldr
	 *            The "receiving" builder, in other words can this builder
	 *            convert the SQL to the new builder
	 * @return true if it can convert it, false if it can not
	 */
	public boolean canConvertTo(QBBuilderInterface aBldr) {
		switch (aBldr.getType()) {
		case QBBuilderInterface.STANDARD:
			return mTableJoins.mJoinItems.size() == 0
					&& !mWherePanel.isComplex();

		case QBBuilderInterface.INTERMEDIATE:
			return !mWherePanel.isComplex();

		case QBBuilderInterface.ADVANCED:
			return true;
		}

		return false;
	}

	/**
	 * Build UI from the Query Definition Object
	 * 
	 * @param aQueryDef
	 *            the query
	 */
	public int buildFromQueryDef(DBQueryDef aQueryDef) {
		if (aQueryDef != null) {
			mWherePanel.getModel().initialize(aQueryDef.getWhere());
			mWherePanel.generateAndSetWhereText();
			mWherePanel.valueChanged(null);
			mTableModel.buildFromQueryDef(aQueryDef);

			mDesktop.clearTables();
			for (Enumeration e = aQueryDef.getTables().elements(); e
					.hasMoreElements();) {
				mDesktop.addTableToWorkspace((DBQueryDefTable) e.nextElement());
			}

			// clear and build joins
			mTableJoins.clear();
			if (aQueryDef.getJoins() != null) {
				for (Enumeration e = aQueryDef.getJoins().elements(); e
						.hasMoreElements();) {
					DBSelectTableModelItem left = (DBSelectTableModelItem) e
							.nextElement();
					DBSelectTableModelItem right = (DBSelectTableModelItem) e
							.nextElement();
					mTableJoins.addJoin(left, right);
				}
			}

			refresh();
			return DBQueryDef.BUILD_OK;
		}
		return DBQueryDef.BUILD_ERROR;
	}

	/**
	 * Fill the QueryDef from the Model
	 * 
	 * @param aQueryDef
	 *            the query
	 */
	public void fillQueryDef(DBQueryDef aQueryDef) {
		if (aQueryDef == null) {
			return;
		}
		aQueryDef.setIsAdv(true);
		mTableModel.fillQueryDef(aQueryDef);
		mWherePanel.fillQueryDef(aQueryDef);
		Hashtable tableNamesHash = new Hashtable();
		fillHashWithTableNamesForWhere(aQueryDef.getWhere(), tableNamesHash);
		Vector tables = new Vector();

		Component tableFrames[] = mDesktop
				.getComponentsInLayer(JDesktopPane.DEFAULT_LAYER.intValue());
		for (int i = 0; i < tableFrames.length; i++) {
			if (tableFrames[i] instanceof DBTableFrame) {
				DBTableFrame tbl = (DBTableFrame) tableFrames[i];
				aQueryDef.addTable(tbl.getId(), tbl.getName(), tbl
						.getLocation().x, tbl.getLocation().y);
				// remove a duplicate table name
				tableNamesHash.remove(tbl.getName());
			}
		}

		// add any table names that where used in a where clause but not in the
		// joins
		for (Enumeration e = tableNamesHash.elements(); e.hasMoreElements();) {
			aQueryDef.addTable((String) e.nextElement());
		}

		if (mTableJoins.getJoins().size() > 0) {
			Vector joins = new Vector();
			for (Enumeration e = mTableJoins.getJoins().elements(); e
					.hasMoreElements();) {
				DBTableJoinItem joinItem = (DBTableJoinItem) e.nextElement();
				DBSelectTableModelItem item = new DBSelectTableModelItem();
				item.setTableName(joinItem.getItemLeft().getTable().getName());
				item.setName(joinItem.getItemLeft().getName());
				item.setTableId(joinItem.getItemLeft().getTable().getId());
				joins.add(item);

				item = new DBSelectTableModelItem();
				item.setTableName(joinItem.getItemRight().getTable().getName());
				item.setName(joinItem.getItemRight().getName());
				item.setTableId(joinItem.getItemRight().getTable().getId());
				joins.add(item);
			}
			if (joins.size() > 0) {
				aQueryDef.setJoins(joins);
			}
		}
	}

}