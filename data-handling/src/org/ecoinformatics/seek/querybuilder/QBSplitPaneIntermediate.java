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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;

import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
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
public class QBSplitPaneIntermediate extends JPanel implements
		ListSelectionListener, QBBuilderInterface {
	protected JSplitPane mSplitPane = null;
	protected DSSchemaIFace mSchema = null;
	protected DBTableDesktopPane mDesktop = null;
	protected DBTableJoin mTableJoins = null;
	protected JButton mAddBtn = null;
	protected JList mTableList = null;
	protected TableModelListener mTableModelListener = null;

	protected Hashtable mModelHash = null;
	protected Hashtable mTablesViewHash = null;

	protected DBSelectTableUIStd mTableView = null;
	protected DBSelectTableModelStd mTableModel = null;

	protected JRadioButton mAllRadio = null;
	protected JRadioButton mAnyRadio = null;

	/**
	 * QBSplitPaneIntermediate Constructor
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aListener
	 *            a listener of changes to the overall model
	 */
	public QBSplitPaneIntermediate(DSSchemaIFace aSchema,
			TableModelListener aListener) {
		mSchema = aSchema;

		setLayout(new BorderLayout());

		JComponent upperPanel = createUpperPanel(aSchema, aListener);
		JComponent lowerPanel = createLowerPanel(mDesktop);

		mSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upperPanel,
				lowerPanel);
		mSplitPane.setContinuousLayout(true);
		mSplitPane.setOneTouchExpandable(true);
		mSplitPane.setDividerLocation(230);

		add(mSplitPane, BorderLayout.CENTER);

		// Now that the Top and Bottom Panes have been created
		// It is time to hook up the listeners so the tables in the top pane
		// can listen for changes in the bottom pane
		Vector tables = mDesktop.getTables();
		for (int i = 0; i < tables.size(); i++) {
			mTableModel.addTableModelListener((TableModelListener) tables
					.elementAt(i));
			mTableView.addFieldChangeListener(mDesktop);
		}

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
	}

	/**
	 * Do Clean up
	 */
	public void shutdown() {
		mTableList.removeListSelectionListener(this);
		mTableView.removeFieldChangeListener(mDesktop);
		mTableModel.removeTableModelListener(mTableModelListener);
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
	 * @param aSchema
	 *            the schema
	 * @param aListener
	 *            the listener for the overall model changes
	 * @return the component representing the upper pane
	 */
	public JComponent createUpperPanel(DSSchemaIFace aSchema,
			TableModelListener aListener) {
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
	/*
	 * public JPanel createSelectForDisplayPanel(DSSchemaIFace aSchema) { JPanel
	 * panel = new JPanel(new BorderLayout());
	 * 
	 * // Create table mode and view for the DB Tables mTableView = new
	 * DBSelectTableUIAdv(); mTableModel = new DBSelectTableModelAdv(aSchema);
	 * mTableView.setColumnSelectionAllowed(false);
	 * mTableView.setRowSelectionAllowed(true);
	 * mTableView.setCellSelectionEnabled(true);
	 * 
	 * Vector tables = mDesktop.getTables(); mTableView.setModel(mTableModel);
	 * for (int i=0;i<tables.size();i++) {
	 * mTableModel.addTableModelListener((TableModelListener
	 * )tables.elementAt(i)); } mTableView.installEditors();
	 * 
	 * JScrollPane scrollpane = new JScrollPane(mTableView);
	 * 
	 * panel.add(scrollpane, BorderLayout.CENTER); return panel; }
	 */

	/**
	 * Creates the radio buttons
	 * 
	 * @return the container (panel)
	 */
	protected JPanel createRadioButtons() {
		ButtonGroup group = new ButtonGroup();

		// Text Radio Buttons
		JPanel p2 = QBSplitPaneStandard.createHorizontalPanel(true);

		mAllRadio = (JRadioButton) p2.add(new JRadioButton(
				"Meets All the Conditions Below"));
		group.add(mAllRadio);
		mAllRadio.setSelected(true);

		mAnyRadio = (JRadioButton) p2.add(new JRadioButton(
				"Meets Any of the Conditions Below"));
		group.add(mAnyRadio);

		return p2;
	}

	/**
	 * Creates the lower panel that contains the tabs "Select" and "Where"
	 * 
	 * @param aSchema
	 *            the schema
	 * @return the component representing the lower pane
	 */
	public JComponent createLowerPanel(DSSchemaIFace aSchema) {
		mModelHash = new Hashtable();
		Vector tables = aSchema.getTables();
		if (tables != null && tables.size() > 0) {
			Object[] tblArray = tables.toArray();
			QuickSort.doSort(tblArray, 0, tblArray.length - 1);

			for (int i = 0; i < tblArray.length; i++) {
				DSTableIFace table = (DSTableIFace) tblArray[i];
				DBSelectTableOverviewModel model = new DBSelectTableOverviewModel(
						table);
				mModelHash.put(table.getName(), model);
			}
		}

		mTableView = new DBSelectTableUIStd(); // referenced in createLowerPanel
												// and createUpperPanel
		// Create table mode and view for the DB Tables
		mTableModel = new DBSelectTableModelStd(aSchema, mModelHash);
		mTableView.setColumnSelectionAllowed(false);
		mTableView.setRowSelectionAllowed(true);
		mTableView.setCellSelectionEnabled(true);

		mTableView.setModel(mTableModel);
		mTableView.installEditors();

		JPanel noviceMainPanel = new JPanel(new BorderLayout());
		noviceMainPanel.add(createRadioButtons(), BorderLayout.NORTH);

		JScrollPane scrollpane = new JScrollPane(mTableView);
		scrollpane.setPreferredSize(new Dimension(700, 300));
		noviceMainPanel.add(scrollpane, BorderLayout.CENTER);

		return noviceMainPanel;
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
	 * Makes the entire Desktop object repaint itself
	 * 
	 */
	protected void refresh() {
		// mDesktop.refresh();
		if (mDesktop != null)
			mDesktop.makeDirty();
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

		return mTableModel.getRowCount() > 1 && !atLeastOneForDisplay;
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
		return QBBuilderInterface.INTERMEDIATE;
	}

	/**
	 * A textual name for this builder
	 * 
	 * @return string of the name
	 */
	public String getName() {
		return "Intermediate";
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
			return mTableJoins.mJoinItems.size() == 0;

		case QBBuilderInterface.INTERMEDIATE:
			return true;

		case QBBuilderInterface.ADVANCED:
			return true;
		}

		return false;
	}

	/**
	 * Create SQL string
	 */
	public String createSQL() {
		Hashtable tableNames = new Hashtable();
		StringBuffer strBuf = new StringBuffer("SELECT ");
		DBSelectTableModelStd model = (DBSelectTableModelStd) mTableView
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

		/*
		 * strBuf.append(" WHERE "); strBuf.append(whereStr); String
		 * wherePanelStr = mWherePanel.generateWhereSQL(true); String noSpaces =
		 * wherePanelStr.trim(); if (noSpaces.length() > 0) {
		 * strBuf.append(" AND "); strBuf.append(wherePanelStr); }
		 */

		return strBuf.toString();

	}

	/**
	 * Build UI from the Query Definition Object
	 * 
	 * @param aQueryDef
	 *            the query
	 */
	public int buildFromQueryDef(DBQueryDef aQueryDef) {
		if (aQueryDef == null)
			return DBQueryDef.BUILD_ERROR;

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

		StringBuffer operStr = new StringBuffer();
		int status = mTableModel.buildFromQueryDef(aQueryDef, operStr, true);
		if (operStr.toString().equals(DBWhereOperator.AND_OPER)) {
			mAllRadio.setSelected(true);
		} else {
			mAnyRadio.setSelected(true);
		}
		mTableModel.fireTableModelChanged();
		repaint();
		return status;

		/*
		 * if (aQueryDef != null) {
		 * 
		 * mTableModel.buildFromQueryDef(aQueryDef);
		 * 
		 * mDesktop.clearTables(); for (Enumeration e =
		 * aQueryDef.getTables().elements(); e.hasMoreElements();) {
		 * mDesktop.addTableToWorkspace((DBQueryDefTable)e.nextElement()); }
		 * 
		 * // clear and build joins mTableJoins.clear(); if
		 * (aQueryDef.getJoins() != null) { for (Enumeration e =
		 * aQueryDef.getJoins().elements(); e.hasMoreElements();) {
		 * DBSelectTableModelItem left =
		 * (DBSelectTableModelItem)e.nextElement(); DBSelectTableModelItem right
		 * = (DBSelectTableModelItem)e.nextElement(); mTableJoins.addJoin(left,
		 * right); } }
		 * 
		 * refresh(); return DBQueryDef.BUILD_OK; } return
		 * DBQueryDef.BUILD_ERROR;
		 */
	}

	/**
	 * Fill the QueryDef from the Model
	 * 
	 * @param aQueryDef
	 *            the query
	 */
	public void fillQueryDef(DBQueryDef aQueryDef) {
		if (aQueryDef == null)
			return;

		Vector tables = new Vector();

		// Collect which items are being displayed and which ones have criteria
		Hashtable tableNameHash = new Hashtable();

		Component tableFrames[] = mDesktop
				.getComponentsInLayer(JDesktopPane.DEFAULT_LAYER.intValue());
		for (int i = 0; i < tableFrames.length; i++) {
			if (tableFrames[i] instanceof DBTableFrame) {
				DBTableFrame tbl = (DBTableFrame) tableFrames[i];
				aQueryDef.addTable(tbl.getId(), tbl.getName(), tbl
						.getLocation().x, tbl.getLocation().y);
				// remove a duplicate table name
				tableNameHash.remove(tbl.getName());
			}
		}

		// Add any table names that where used in a where clause but not in the
		// joins
		// for (Enumeration e = tableNameHash.elements(); e.hasMoreElements();)
		// {
		// aQueryDef.addTable((String) e.nextElement());
		// }

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
			aQueryDef.setIsAdv(true);
		} else {
			aQueryDef.setIsAdv(false);
		}

		Vector itemsDisplayed = new Vector();
		Vector itemsWithCriteria = new Vector();
		for (int i = 0; i < mTableModel.getRowCount(); i++) {
			DBSelectTableModelItem item = mTableModel.getFieldForRow(i);
			if (item.getTableName().length() > 0 && item.getName().length() > 0) {
				tableNameHash.put(item.getTableName(), item.getTableName());
				if (item.isDisplayed()) {
					itemsDisplayed.add(new DBSelectTableModelItem(item));
				}
				if (item.getCriteria().length() > 0) {
					itemsWithCriteria.add(new DBSelectTableModelItem(item));
				}
			}
		}

		// Add each item in the display vector to the QueryDef selects
		for (Enumeration et = itemsDisplayed.elements(); et.hasMoreElements();) {
			aQueryDef.addSelectItem((DBSelectTableModelItem) et.nextElement());
		}

		// Now create the where object from the items with criteria
		boolean isSingle = itemsWithCriteria.size() == 1;
		if (itemsWithCriteria.size() > 0) {
			if (mAllRadio.isSelected()) {
				Vector joins = new Vector();
				// loop thru all the items and see if any are joins
				for (Enumeration et = itemsWithCriteria.elements(); et
						.hasMoreElements();) {
					DBSelectTableModelItem item = (DBSelectTableModelItem) et
							.nextElement();
					String oper = item.getOperator();
					if (oper != null
							&& oper
									.equals(DBSelectTableUIStd.OPERS_TXT[DBSelectTableUIStd.EQUALS_INX])) {
						String criteria = item.getCriteria();
						StringBuffer tableName = new StringBuffer();
						DSTableFieldIFace rightFld = DBUIUtils
								.isTableFieldName(mSchema, criteria, tableName);
						DSTableFieldIFace leftFld = DBUIUtils.getFieldByName(
								mSchema, item.getTableName(), item.getName()); // not
																				// really
																				// needed

						if (rightFld != null) {
							DBSelectTableModelItem leftItem = new DBSelectTableModelItem(
									item.getTableName(), item.getName(),
									leftFld.getDataType(), false, "", "",
									leftFld.getMissingValueCode());
							DBSelectTableModelItem rightItem = new DBSelectTableModelItem(
									tableName.toString(), rightFld.getName(),
									rightFld.getDataType(), false, "", "",
									rightFld.getMissingValueCode());
							joins.add(leftItem);
							joins.add(rightItem);
							item.setName(null);
							tableNameHash.put(tableName.toString(), tableName
									.toString());
						}
					}
				}
				if (joins.size() > 0) {
					aQueryDef.setJoins(joins);
				}
			}

			// Add each table name that used
			for (Enumeration et = tableNameHash.elements(); et
					.hasMoreElements();) {
				aQueryDef.addTable((String) et.nextElement());
			}
			DBWhereOperator oper = null;
			if (!isSingle) {
				oper = new DBWhereOperator(null, false);
				oper
						.setOperator(mAllRadio.isSelected() ? DBWhereOperator.AND_OPER
								: DBWhereOperator.OR_OPER);
			}

			boolean itemWasAdded = false;
			DBWhereCondition cond = null;
			DBWhereIFace after = null;
			for (Enumeration et = itemsWithCriteria.elements(); et
					.hasMoreElements();) {
				DBSelectTableModelItem item = (DBSelectTableModelItem) et
						.nextElement();
				// skip any item with a null name because it was already used
				if (item.getName() != null) {
					cond = new DBWhereCondition(oper, item.getTableName(), item
							.getName(), item.getDataType());
					cond.setOperator(item.getOperator());
					cond.setCriteria(item.getCriteria());
					if (oper != null) {
						oper.addAfter(cond, after);
						itemWasAdded = true;
					}
					after = cond;
				}
			}
			if (isSingle) {
				if (cond != null) {
					aQueryDef.setWhere((DBWhereIFace) cond);
				}
			} else if (oper != null && itemWasAdded) {
				aQueryDef.setWhere((DBWhereIFace) oper);
			}
		}
	}
}