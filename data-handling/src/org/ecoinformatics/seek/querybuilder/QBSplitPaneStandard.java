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
import java.awt.Color;
import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;

import org.kepler.objectmanager.data.DataType;
import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;

//import com.sun.jdi.connect.Connector.SelectedArgument;

//import Formulate.DBSchemaFieldNode;

/**
 * This class shows a split view with a set of table schemas showing via tabs in
 * the upper pane. And a two tab control in the lower pane. The Select pane
 * enables the user to indicate which fields will be displayed and what the
 * conditions will be for each of the fields.
 * 
 * The Where pane enables
 * 
 * @author Rod Spears
 */
public class QBSplitPaneStandard extends JPanel implements
		ListSelectionListener, QBBuilderInterface {
	private static final int NUM_TABLE_THRESOLD = 3;

	protected JSplitPane mSplitPane = null;
	protected DSSchemaIFace mSchema = null;

	protected Hashtable mModelHash;
	protected Hashtable mTablesViewHash;

	protected DBSelectTableUIStd mTableView;
	protected DBSelectTableModelStd mTableModel;
	protected DBSelectTableOverviewModel mModel;
	protected DBSelectTableOverviewTable mSubTableView;
	protected JScrollPane mTableSchemaPane;
	protected JRadioButton mAllRadio;
	protected JRadioButton mAnyRadio;
	protected JList mTableList;

	protected JSplitPane mUpperSplitPane;
	protected JPanel mUpperPanel;
	protected JPanel mSimulatedListPanel;

	protected JPanel mSimulatedListList;
	protected Action mSimulatedListAction;
	protected Vector mCheckboxes;

	protected TableModelListener mTableModelListener = null;

	/**
	 * QBSplitPaneBase Constructor
	 */
	public QBSplitPaneStandard(DSSchemaIFace aSchema) {
		mSchema = aSchema;

		setLayout(new BorderLayout());

		mSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				createUpperPanel(aSchema), createLowerPanel(aSchema));
		mSplitPane.setContinuousLayout(true);
		// mSplitPane.setOneTouchExpandable(true);
		mSplitPane.setDividerLocation(230);

		add(mSplitPane, BorderLayout.CENTER);
	}

	/**
	 * Return Schema
	 */
	public DSSchemaIFace getSchema() {
		return mSchema;
	}

	/**
	 * Method to get selected table view
	 * 
	 * 	 */
	public DBSelectTableUIBase getSelectedTableView() {
		return mTableView;
	}

	/**
	 * Method to get selected table view's model
	 * 
	 * 	 */
	public DBSelectTableOverviewModel getSelectedOverTableViewModel() {
		return mModel;
	}

	/**
	 * Method to set selected table Views's model
	 */
	public void setSelectedOverViewModel(DBSelectTableOverviewModel mModel) {
		this.mModel = mModel;
	}

	/**
	 * Method to get model hash
	 */
	public Hashtable getModelHashtable() {
		return mModelHash;
	}

	/**
	 * Sets the Model Listener appropriately
	 * 
	 * @param aTblModelListener
	 */
	public void setTableModelListener(TableModelListener aTblModelListener) {
		mTableModelListener = aTblModelListener;
		mTableModel.addTableModelListener(aTblModelListener);
	}

	/**
	 * Create the upper panel
	 * 
	 * @param aSchema
	 *            the schema
	 * @return the UI component
	 */
	public JComponent createUpperPanel(DSSchemaIFace aSchema) {
		mModelHash = new Hashtable();
		mTablesViewHash = new Hashtable();

		mTableView = new DBSelectTableUIStd(); // referenced in createLowerPanel
		// and createUpperPanel

		mUpperPanel = new JPanel(new BorderLayout());
		Vector tables = aSchema.getTables();
		Object[] tblArray = null;
		String[] tableNameArray = null;
		if (tables != null && tables.size() > 0) {
			tblArray = tables.toArray();
			tableNameArray = new String[tblArray.length];
			QuickSort.doSort(tblArray, 0, tblArray.length - 1);
			for (int i = 0; i < tblArray.length; i++) {
				DSTableIFace table = (DSTableIFace) tblArray[i];
				mModel = new DBSelectTableOverviewModel(table);
				tableNameArray[i] = table.getName();
				mModelHash.put(table.getName(), mModel);
				if (i == 0) {
					mSubTableView = new DBSelectTableOverviewTable(mModel);
					setOverTableView();

				}

			}
		} else {
			tableNameArray = new String[1];
			tableNameArray[0] = "";
		}

		JPanel schemaSelectionPanel = new JPanel();
		schemaSelectionPanel.setLayout(new BoxLayout(schemaSelectionPanel,
				BoxLayout.X_AXIS));
		JLabel label = new JLabel(" Available Table Schemas: ");
		JComboBox schemaSelection = new JComboBox(tableNameArray);
		TableSchemaSelectionListener selectionListener = new TableSchemaSelectionListener(
				this);
		schemaSelection.addActionListener(selectionListener);
		schemaSelection.setBackground(Color.WHITE);
		schemaSelectionPanel.add(label);
		schemaSelectionPanel.add(schemaSelection);
		schemaSelectionPanel.add(Box.createHorizontalGlue());
		JPanel seperationPane = new JPanel();
		seperationPane
				.setLayout(new BoxLayout(seperationPane, BoxLayout.Y_AXIS));
		seperationPane.add(schemaSelectionPanel);
		seperationPane.add(Box.createVerticalStrut(20));
		mUpperPanel.add(seperationPane, BorderLayout.NORTH);

		if (mSubTableView != null) {
			mTableSchemaPane = new JScrollPane(mSubTableView);
			mTableView.addFieldChangeListener(mSubTableView);
			mUpperPanel.add(mTableSchemaPane, BorderLayout.CENTER);
		}

		valueChanged(null); // enables/disables the add button

		return mUpperPanel;
	}

	/**
	 * This method will set tableview and its model
	 * 
	 */
	public void setOverTableView() {
		if (mSubTableView != null) {
			mSubTableView.setModel(mModel);
			mModel.setTableView(mSubTableView);
			mSubTableView.setColumnSelectionAllowed(false);
			mSubTableView.setRowSelectionAllowed(false);
			mSubTableView.setCellSelectionEnabled(false);
			mSubTableView.getColumn("Field Name").setCellRenderer(
					new DBSelectTableOverviewCellRenderer());
			// mModel.fireTableDataChanged();
		}
	}

	/**
	 * Creates a horizontal panel
	 * 
	 * @param threeD
	 *            whether it should have a 3D border
	 * @return the panel
	 */
	public static JPanel createHorizontalPanel(boolean aThreeD) {
		Border loweredBorder = new CompoundBorder(new SoftBevelBorder(
				SoftBevelBorder.LOWERED), new EmptyBorder(5, 5, 5, 5));
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.setAlignmentY(TOP_ALIGNMENT);
		p.setAlignmentX(LEFT_ALIGNMENT);
		if (aThreeD) {
			p.setBorder(loweredBorder);
		}
		return p;
	}

	/**
	 * Creates the radio buttons
	 * 
	 * @return the container (panel)
	 */
	protected JPanel createRadioButtons() {
		ButtonGroup group = new ButtonGroup();

		// Text Radio Buttons
		JPanel p2 = createHorizontalPanel(true);

		mAllRadio = (JRadioButton) p2.add(new JRadioButton(
				"Meets ALL included conditions listed below  "));
		group.add(mAllRadio);
		mAllRadio.setSelected(true);

		mAnyRadio = (JRadioButton) p2.add(new JRadioButton(
				"Meets ANY included conditions list below"));
		group.add(mAnyRadio);

		return p2;
	}

	/**
	 * Create the lower panel
	 * 
	 * @param aSchema
	 *            the schema
	 * @return the component
	 */
	public JComponent createLowerPanel(DSSchemaIFace aSchema) {
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
	 * impl of abstract method
	 * 
	 */
	protected void refresh() {
	}

	/**
	 * Adds or removes a table tab from the Tabbed Pane
	 * 
	 * @param aTabName
	 *            name of tab to be changed
	 * @param isSelected
	 *            indicates whether it will be added or removed
	 */
	/*
	 * public void adjustTab(String aTabName, boolean isSelected) { int inx =
	 * mTabbedpane.indexOfTab(aTabName); if (inx > -1) { if (!isSelected) {
	 * mTabbedpane.removeTabAt(inx); } } else if (isSelected) {
	 * mTabbedpane.add(aTabName, (Component) mTablesViewHash.get(aTabName)); } }
	 */

	/**
	 * Reset the UI to nothing
	 * 
	 */
	public void reset() {

	}

	/**
   * 
   */
	/*
	 * class UpdateSimulatedListListAction extends AbstractAction { public void
	 * actionPerformed(ActionEvent e) { JCheckBox cb = (JCheckBox)
	 * e.getSource(); adjustTab(cb.getText(), cb.isSelected()); } }
	 */

	// ---------------------------------------------------
	// -- ListSelectionListener
	// ---------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {
		// mAddBtn.setEnabled(mTableList.getSelectedIndex() != -1);
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
		return QBBuilderInterface.STANDARD;
	}

	/**
	 * A textual name for this builder
	 * 
	 * @return string of the name
	 */
	public String getName() {
		return "Standard";
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
		return true;
	}

	/**
	 * Create SQL string
	 */
	public String createSQL() {
		Hashtable tableNames = new Hashtable();
		StringBuffer strBuf = new StringBuffer("SELECT ");
		DBSelectTableModelStd model = (DBSelectTableModelStd) mTableView
				.getModel();

		int criteriaCnt = 0;
		int displayCnt = 0;
		for (int i = 0; i < model.getRowCount(); i++) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) model
					.getFieldForRow(i);
			if (item.isDisplayed()) {
				tableNames.put(item.getTableName(), item.getTableName());
				displayCnt++;
			}

			if (item.getCriteria().length() > 0) {
				tableNames.put(item.getTableName(), item.getTableName());
				criteriaCnt++;
			}
		}
		if (displayCnt == 0)
			return "";

		/*
		 * Hashtable displayedHash = new Hashtable(); displayCnt = 0; for (int i
		 * = 0; i < model.getRowCount(); i++) { DBSelectTableModelItem item =
		 * (DBSelectTableModelItem) model.getFieldForRow(i); if
		 * (item.isDisplayed()) { String fullName =
		 * DBUIUtils.getFullFieldName(item.getTableName(), item.getName()); if
		 * (displayedHash.get(fullName) == null) { displayedHash.put(fullName,
		 * "X"); if (displayCnt > 0) { strBuf.append(", "); } displayCnt++; if
		 * (tableNames.size() > 1) { strBuf.append(fullName); } else {
		 * strBuf.append(DBUIUtils.fixNameWithSpaces(item.getName())); }
		 * tableNames.put(item.getTableName(), item.getTableName()); } } }
		 */

		Hashtable selectFields = proneDuplicateFieldAndWildCard(model);
		// System.out.println("the select field hash is "+selectFields);
		if (selectFields != null) {
			Enumeration em = selectFields.keys();
			while (em.hasMoreElements()) {
				String tableNameStr = (String) em.nextElement();
				// System.out.println("The table name is "+tableNameStr);
				Vector fieldVector = (Vector) selectFields.get(tableNameStr);
				// System.out.println("the fieldVector is "+fieldVector);
				if (fieldVector != null) {
					// System.out.println("the fieldVector is not null");
					displayCnt = 0;
					for (int i = 0; i < fieldVector.size(); i++) {
						String fieldNameStr = (String) fieldVector.elementAt(i);
						// System.out.println("the field name is "+fieldNameStr);
						String fullName = DBUIUtils.getFullFieldName(
								tableNameStr, fieldNameStr);
						if (displayCnt > 0) {
							strBuf.append(", ");
						}

						if (tableNames.size() > 1) {
							strBuf.append(fullName);
						} else {
							strBuf.append(DBUIUtils
									.fixNameWithSpaces(fieldNameStr));
						}
						displayCnt++;
					}
				}

			}
		}

		strBuf.append(" FROM ");

		displayCnt = 0;
		for (Enumeration et = tableNames.elements(); et.hasMoreElements();) {
			String tableName = (String) et.nextElement();
			if (displayCnt > 0) {
				strBuf.append(", ");
			}
			displayCnt++;
			strBuf.append(tableName);
		}

		if (criteriaCnt > 0) {
			criteriaCnt = 0;
			strBuf.append(" WHERE ");
			for (int i = 0; i < model.getRowCount(); i++) {
				DBSelectTableModelItem item = (DBSelectTableModelItem) model
						.getFieldForRow(i);
				if (item.getCriteria().length() > 0) {
					if (criteriaCnt > 0) {
						strBuf
								.append(mAllRadio.isSelected() ? " AND "
										: " OR ");
					}
					criteriaCnt++;
					if (tableNames.size() > 1) {
						strBuf.append(DBUIUtils.fixNameWithSpaces(item
								.getTableName())
								+ ".");
					}
					String name = DBUIUtils.fixNameWithSpaces(item.getName());
					String criteria = item.getDataType().equals(DataType.STR) ? "'"
							+ item.getCriteria() + "'"
							: item.getCriteria();
					strBuf.append(name
							+ DBSelectTableUIStd.getBoolOperSymbol(item
									.getOperator()) + criteria);
				}
			}
		}
		return strBuf.toString();
	}

	/*
	 * If selected Field has, the other field will be proned. It will return a
	 * Hashtable Key is table name and value is a vector containing fields
	 * names. The duplicate field will be prone too
	 */
	private Hashtable proneDuplicateFieldAndWildCard(DBSelectTableModelStd model) {
		// if set up this vairable to true, the
		// sql command select *, day from tableX will be select * from tableX
		boolean proneWildCard = false;
		Hashtable tableFields = new Hashtable();
		if (model != null) {
			// System.out.println("model is not null");
			for (int i = 0; i < model.getRowCount(); i++) {
				DBSelectTableModelItem item = (DBSelectTableModelItem) model
						.getFieldForRow(i);

				if (item.isDisplayed()) {
					String tableName = item.getTableName();
					String fieldName = item.getName();
					// System.out.println("The tableName from model "+tableName);
					// System.out.println("The fieldName from model "+fieldName);
					Vector fieldNameVector = (Vector) tableFields
							.get(tableName);
					if (fieldNameVector == null) {
						// System.out.println("generate new vector if the vector from hash is null");
						fieldNameVector = new Vector();
					}

					if (proneWildCard) {
						// System.out.println("in prone wild card branch");
						if (fieldName != null
								&& fieldName
										.equals(DBQueryDefParserEmitter.WILDCARD)) {
							// removed all other fields
							// System.out.println("in handel wild card part");
							fieldNameVector = new Vector();
							fieldNameVector
									.add(DBQueryDefParserEmitter.WILDCARD);
							// System.out.println("add wild card into vector");
							tableFields.put(tableName, fieldNameVector);
							// System.out.println("add table name and vector into hash");
						} else if (fieldName != null) {
							// test if the vector's first value if is wild card
							// - * (wild card will always in first value)
							// if it is, we would add this field value
							String firstField = null;
							if (!fieldNameVector.isEmpty()) {
								firstField = (String) fieldNameVector
										.elementAt(0);
								// System.out.println("get first element in vector "+firstField);
							}

							if (firstField == null
									|| !firstField
											.equals(DBQueryDefParserEmitter.WILDCARD)
									&& !fieldNameVector.contains(fieldName)) {
								// System.out.println("add file name "+fieldName
								// +" into vector");
								fieldNameVector.add(fieldName);
							}
						}
						tableFields.put(tableName, fieldNameVector);
						// System.out.println("add table name and vector into hash");
					} else {
						if (!fieldNameVector.contains(fieldName)) {
							// System.out.println("add file name "+fieldName
							// +" into vector");
							fieldNameVector.add(fieldName);
						}
						tableFields.put(tableName, fieldNameVector);
						// System.out.println("add table name and vector into hash");
					}
				}
			}
		}
		return tableFields;
		//
	}

	/**
	 * Build UI from the Query Definition Object
	 */
	public int buildFromQueryDef(DBQueryDef aQueryDef) {
		if (aQueryDef == null)
			return DBQueryDef.BUILD_ERROR;

		StringBuffer operStr = new StringBuffer();
		int status = mTableModel.buildFromQueryDef(aQueryDef, operStr, false);
		if (operStr.toString().equals(DBWhereOperator.AND_OPER)) {
			mAllRadio.setSelected(true);
		} else {
			mAnyRadio.setSelected(true);
		}
		mTableModel.fireTableModelChanged();
		repaint();
		return status;
	}

	/**
	 * Fills QueryDef from Model
	 */
	public void fillQueryDef(DBQueryDef aQueryDef) {
		if (aQueryDef == null)
			return;

		aQueryDef.setIsAdv(false);

		// Collect which items are being displayed and which ones have criteria
		Hashtable tableNameHash = new Hashtable();
		Hashtable itemsDisplayedHash = new Hashtable();
		Vector itemsDisplayed = new Vector();
		Vector itemsWithCriteria = new Vector();

		for (int i = 0; i < mTableModel.getRowCount(); i++) {
			DBSelectTableModelItem item = mTableModel.getFieldForRow(i);
			if (item.getTableName().length() > 0 && item.getName().length() > 0) {
				DBSelectTableModelItem newItem = null;

				String fullName = DBUIUtils.getFullFieldName(item
						.getTableName(), item.getName());
				if (itemsDisplayedHash.get(fullName) == null) {
					if (item.isDisplayed()) {
						newItem = new DBSelectTableModelItem(item);
						itemsDisplayedHash.put(fullName, fullName);
						tableNameHash.put(item.getTableName(), item
								.getTableName());
						itemsDisplayed.add(newItem);
					}
				}

				if (item.getCriteria().length() > 0) {
					tableNameHash.put(item.getTableName(), item.getTableName());
					itemsWithCriteria
							.add(newItem == null ? new DBSelectTableModelItem(
									item) : newItem);
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
									leftFld.getDataType(), false, "", "", item
											.getMissingValueCode());
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