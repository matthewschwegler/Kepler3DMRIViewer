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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;

import org.kepler.objectmanager.data.DataType;
import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;

/**
 * This class is a JPanel that contains the list of Where OPerator and Condition
 * objects. NOTE: The list display the operator and its children in reverse
 * polish notation. It also contains<br>
 * 1) a sub-panel with some UI for adding and removing items.<br>
 * 2) A subpanel inspector for the editting of the COndition or Operator items<br>
 * 3) A text control for displaying the results of the where conidtion
 */
public class DBWherePanel extends JPanel implements ListSelectionListener,
		ListDataListener {
	protected DBWhereList mList = null;
	protected DBWhereModel mModel = null;
	protected DSSchemaIFace mSchema = null;

	protected JButton mAddCondBtn = null;
	protected JButton mAddAndOperBtn = null;
	protected JButton mAddOROperBtn = null;
	protected JButton mRemoveBtn = null;
	protected JTextField mNameField = new JTextField();
	protected JTextField mCriteriaField = new JTextField();
	protected JEditorPane mEditorPane = null;
	protected JScrollPane mTextScrollPane = null;

	protected JComboBox mTablesCombobox = new JComboBox();
	protected JComboBox mFieldsCombobox = new JComboBox();
	protected JComboBox mCondCombobox = new JComboBox();
	protected JComboBox mOperCombobox = new JComboBox();

	protected JPanel mCondInspPanel = null;
	protected JPanel mOperInspPanel = null;
	protected JPanel mCurrInspPanel = null;
	protected JPanel mInspContainer = null;

	protected boolean mRejectChanges = false;
	protected TableModelListener mModelListener = null;

	/**
	 * Constructor
	 * 
	 * @param aSchema
	 *            the schema
	 * 
	 */
	public DBWherePanel(DSSchemaIFace aSchema) {
		super(new BorderLayout());
		mSchema = aSchema;

		mModel = new DBWhereModel();
		mList = new DBWhereList(aSchema, mModel);

		DBUIUtils.fillTableCombobox(mSchema, mTablesCombobox);
		mTablesCombobox.setSelectedIndex(0);
		DBUIUtils.fillFieldCombobox(mSchema, (String) mTablesCombobox
				.getSelectedItem(), mFieldsCombobox, false);

		mList.getSelectionModel().addListSelectionListener(this);
		mModel.addListDataListener(this);

		mOperInspPanel = createOperInspector();
		mCondInspPanel = createCondInspector();

		mInspContainer = new JPanel(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().setView(mList);
		mInspContainer.add(scrollPane, BorderLayout.CENTER);

		mEditorPane = new JEditorPane("text/text", "\n\n\n\n\n");
		mEditorPane.setMinimumSize(new Dimension(200, 200));
		mEditorPane.setFocusable(false);
		mEditorPane.setEditable(false);

		mTextScrollPane = new JScrollPane();
		mTextScrollPane.getViewport().setView(mEditorPane);
		mTextScrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		mTextScrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		add(createControlPanel(), BorderLayout.EAST);
		add(mInspContainer, BorderLayout.CENTER);
		add(mTextScrollPane, BorderLayout.SOUTH);

	}

	/**
	 * Set a single model listener
	 * 
	 * @param aTblModelListener
	 *            the listener
	 */
	public void setModelListener(TableModelListener aTblModelListener) {
		mModelListener = aTblModelListener;
	}

	/**
	 * Returns the model
	 * 
	 * @return the model
	 */
	public DBWhereModel getModel() {
		return mModel;
	}

	/**
	 * Creates the inspector panel for a condition
	 * 
	 * @return the panel
	 */
	protected JPanel createCondInspector() {
		JPanel panel = new JPanel(new GridLayout(2, 4));
		panel.add(new JLabel("Table", SwingConstants.CENTER));
		panel.add(new JLabel("Field", SwingConstants.CENTER));
		panel.add(new JLabel("Compartor", SwingConstants.CENTER));
		panel.add(new JLabel("Value", SwingConstants.CENTER));

		panel.add(mTablesCombobox);
		panel.add(mFieldsCombobox);

		panel.add(mCondCombobox);
		for (int i = 1; i < DBSelectTableUIStd.OPERS_TXT.length; i++) { // skip
																		// first
																		// item
			mCondCombobox.addItem(DBSelectTableUIStd.OPERS_TXT[i]);
		}
		panel.add(mCriteriaField);

		mTablesCombobox.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// populateFieldComboboxFromTableName((String)mTablesCombobox.getSelectedItem());
				DBUIUtils.fillFieldCombobox(mSchema, (String) mTablesCombobox
						.getSelectedItem(), mFieldsCombobox, false);
				doUpdate(false);
				generateAndSetWhereText();
			}
		});

		mFieldsCombobox.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				doUpdate(false);
				generateAndSetWhereText();
			}
		});

		mCondCombobox.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				doUpdate(false);
				generateAndSetWhereText();
			}
		});

		KeyListener keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				doUpdate(true);
				generateAndSetWhereText();
			}

			public void keyReleased(KeyEvent e) {
				keyPressed(e);
			}

			public void keyTyped(KeyEvent e) {
				keyPressed(e);
			}
		};
		mCriteriaField.addKeyListener(keyListener);
		// mCriteriaField.addKeyListener(this);

		return panel;
	}

	/**
	 * Creates the inspector panel for the Operator
	 * 
	 * @return the panel
	 */
	protected JPanel createOperInspector() {
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel panel = new JPanel(new BorderLayout());
		mainPanel.add(panel, BorderLayout.WEST);
		panel.add(new JLabel("Operator: ", SwingConstants.CENTER),
				BorderLayout.WEST);

		panel.add(mOperCombobox, BorderLayout.CENTER);
		mOperCombobox.addItem(DBWhereOperator.AND_OPER);
		mOperCombobox.addItem(DBWhereOperator.OR_OPER);

		mOperCombobox.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				doUpdate(false);
				generateAndSetWhereText();
			}
		});

		return mainPanel;
	}

	/**
	 * Creates and adds a button to the control panel used for editting the list
	 * of operators and conditions
	 * 
	 * @param aPanel
	 *            the parent
	 * @param name
	 *            the name/text on the button
	 * @param gridbag
	 *            gridbag
	 * @param c
	 *            constraint
	 * @return the new button
	 */
	protected JButton makeButton(JPanel aPanel, String name,
			GridBagLayout gridbag, GridBagConstraints c) {
		JButton button = new JButton(name);
		gridbag.setConstraints(button, c);
		aPanel.add(button);
		return button;
	}

	/**
	 * Helper class to add new item to main list
	 * 
	 * @param aItem
	 *            item to be added
	 */
	protected void addNewItem(DBWhereIFace aParent, DBWhereIFace aItem) {
		DBWhereIFace afterItem = null;
		int newInx = mList.getSelectedIndex();
		if (newInx != -1) {
			afterItem = (DBWhereIFace) mModel.getElementAt(newInx);
		}

		newInx++;
		mModel.add(aItem, newInx);

		if (aParent != null) {
			((DBWhereOperator) aParent).addAfter(aItem, afterItem);
		}

		mModel.fireContentsChanged();
		mList.setSelectedIndex(newInx);

	}

	/**
	 * Return the appropriate DBWhereIFace object that the new item will be
	 * parented to
	 * 
	 * @return the parent
	 */
	protected DBWhereOperator getParentForInsert() {
		DBWhereIFace parent = null;
		int inx = mList.getSelectedIndex();
		if (inx != -1) {
			parent = (DBWhereIFace) mModel.getElementAt(inx);
			if (!parent.isOperator() || ((DBWhereOperator) parent).isClosure()) {
				parent = parent.getParent();
			}
		}
		return (DBWhereOperator) parent;
	}

	/**
	 * Create a new operator and two children
	 * 
	 * @param aOperName
	 */
	protected void addOperator(String aOperName) {
		DBWhereOperator parent = getParentForInsert();
		if (parent != null || mModel.getSize() == 0) {
			DBWhereOperator operObj = new DBWhereOperator(parent, false);
			operObj.setName(aOperName);
			addNewItem(parent, operObj);
			String tableName = (String) mTablesCombobox.getItemAt(0);
			String fieldName = (String) mFieldsCombobox.getItemAt(0);
			DSTableFieldIFace fieldIFace = DBUIUtils.getFieldByName(mSchema,
					tableName, fieldName);
			addNewItem(operObj, new DBWhereCondition(operObj, tableName,
					fieldName, fieldIFace != null ? fieldIFace.getDataType()
							: ""));
			addNewItem(operObj, new DBWhereCondition(operObj, tableName,
					fieldName, fieldIFace != null ? fieldIFace.getDataType()
							: ""));
			generateAndSetWhereText();
		}
	}

	/**
	 * Creates the control panel of button for adding and removing items in the
	 * "where clause"
	 * 
	 * 	 */
	protected JPanel createControlPanel() {
		JPanel mainPanel = new JPanel(new BorderLayout());

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER; // end row
		c.fill = GridBagConstraints.HORIZONTAL;

		JPanel panel = new JPanel(gridbag);
		panel.setBorder(new TitledBorder("Control"));

		mAddAndOperBtn = makeButton(panel, "Add AND", gridbag, c);
		mAddOROperBtn = makeButton(panel, "Add OR", gridbag, c);
		mAddCondBtn = makeButton(panel, "Add Condition", gridbag, c);
		mRemoveBtn = makeButton(panel, "Remove", gridbag, c);
		mainPanel.add(panel, BorderLayout.NORTH);

		mAddCondBtn.setEnabled(true);
		mAddAndOperBtn.setEnabled(true);
		mAddOROperBtn.setEnabled(true);
		mRemoveBtn.setEnabled(false);

		mAddCondBtn.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				DBWhereOperator parent = getParentForInsert();
				// if (parent != null || mModel.getSize() == 0) {
				if (mModel.getSize() == 0) {
					String tableName = (String) mTablesCombobox.getItemAt(0);
					String fieldName = (String) mFieldsCombobox.getItemAt(0);
					DBWhereCondition cond = new DBWhereCondition(parent,
							tableName, fieldName, DataType.STR);
					DSTableFieldIFace fieldIFace = DBUIUtils.getFieldByName(
							mSchema, tableName, fieldName);
					if (fieldIFace != null) {
						cond.setDataType(fieldIFace.getDataType());
					}
					addNewItem(parent, cond);
					generateAndSetWhereText();
				}
			}
		});

		mAddAndOperBtn.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				addOperator(DBWhereOperator.AND_OPER);
			}
		});

		mAddOROperBtn.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				addOperator(DBWhereOperator.OR_OPER);
			}
		});

		mRemoveBtn.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int inx = mList.getSelectedIndex();
				if (inx != -1) {
					DBWhereIFace item = (DBWhereIFace) mModel.getElementAt(inx);

					// First remove the item from its parent
					DBWhereIFace parent = item.getParent();
					if (parent != null) {
						if (parent.isOperator()
								&& parent instanceof DBWhereOperator) // safety
																		// checks
																		// (should
																		// NEVER
																		// fail)
						{
							((DBWhereOperator) parent).remove(item);
							if (item.isOperator()
									&& item instanceof DBWhereOperator) {
								((DBWhereOperator) parent)
										.remove(((DBWhereOperator) item)
												.getClosure());
							}
						}
					}

					// Now remove it from the List Model
					mModel.remove(item);
					mList.clearSelection();
					generateAndSetWhereText();

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							// valueChanged(null);
						}
					});

				}
			}
		});

		return mainPanel;
	}

	/**
	 * Updates the inspector UI depending on the type of object being editted
	 * 
	 * @param aDoTextOnly
	 */
	protected void doUpdate(boolean aDoTextOnly) {
		if (mRejectChanges)
			return;

		int inx = mList.getSelectedIndex();
		if (inx != -1) {
			DBWhereIFace item = (DBWhereIFace) mModel.getElementAt(inx);
			if (!item.isOperator()) {
				if (item instanceof DBWhereCondition) {
					DBWhereCondition cond = (DBWhereCondition) item;
					if (!aDoTextOnly) {
						String tableName = (String) mTablesCombobox
								.getSelectedItem();
						String fieldName = (String) mFieldsCombobox
								.getSelectedItem();
						cond.setTableName(tableName);
						cond.setName(fieldName);
						cond.setOperator((String) mCondCombobox
								.getSelectedItem());
						DSTableFieldIFace fieldIFace = DBUIUtils
								.getFieldByName(mSchema, tableName, fieldName);
						if (fieldIFace != null) {
							cond.setDataType(fieldIFace.getDataType());
						}

					}
					cond.setCriteria(mCriteriaField.getText());
				}
			} else if (item instanceof DBWhereOperator) {
				((DBWhereOperator) item).setName((String) mOperCombobox
						.getSelectedItem());
			}
			mModel.fireContentsChanged();
		}
	}

	/**
	 * Recurses through the "tree" of operators and creates a textual rendering
	 * of the operators and conditions
	 * 
	 * @param aOper
	 * @param aStrBuf
	 * @param aLevel
	 * 	 */
	protected int recurseList(DBWhereOperator aOper, StringBuffer aStrBuf,
			int aLevel, boolean aUseSymbols) {
		int retDepth = aLevel;
		// Check number of Children
		if (aOper.getNumChildern() < 2)
			return 0;

		int numChildren = 0;
		for (Enumeration e = aOper.getEnumeration(); e.hasMoreElements();) {
			DBWhereIFace item = (DBWhereIFace) e.nextElement();
			if (item instanceof DBWhereOperator) {
				DBWhereOperator oper = (DBWhereOperator) item;
				if (!oper.isClosure() && oper.getNumChildern() > 1) {
					numChildren++;
				}
			} else {
				numChildren++;
			}
		}
		if (numChildren < 2)
			return 0;

		if (aLevel > 0) {
			aStrBuf.append("\n");
			for (int i = 0; i < aLevel; i++) {
				aStrBuf.append("  ");
			}
		}

		int cnt = 0;
		aStrBuf.append("(");
		for (Enumeration e = aOper.getEnumeration(); e.hasMoreElements();) {
			DBWhereIFace item = (DBWhereIFace) e.nextElement();
			if (item instanceof DBWhereOperator) {
				DBWhereOperator oper = (DBWhereOperator) item;
				if (!oper.isClosure() && oper.getNumChildern() > 1) {
					if (cnt > 0) {
						aStrBuf.append(" "
								+ DBSelectTableUIStd.getBoolOperSymbol(aOper
										.getName()) + " ");
					}
					int depth = recurseList(oper, aStrBuf, aLevel + 1,
							aUseSymbols);
					if (depth > retDepth)
						retDepth = depth;
				}
			} else {
				if (cnt > 0) {
					aStrBuf.append(" "
							+ DBSelectTableUIStd.getBoolOperSymbol(aOper
									.getName()) + " ");
				}
				aStrBuf.append(item.toString(aUseSymbols));
				cnt++;
			}
		}
		aStrBuf.append(")");

		return retDepth;
	}

	/**
	 * Generates a textual representation of the "where" clause
	 * 
	 */
	public String generateWhereSQL(boolean aUseSymbols) {
		StringBuffer strBuf = new StringBuffer("");
		int depth = 0;
		if (mModel.getSize() > 0) {
			if (mModel.getElementAt(0) instanceof DBWhereOperator) {
				DBWhereOperator oper = (DBWhereOperator) mModel.getElementAt(0);
				depth = recurseList(oper, strBuf, 0, aUseSymbols);
			} else {
				strBuf.append(((DBWhereCondition) mModel.getElementAt(0))
						.toString(aUseSymbols));
			}
			for (int i = 0; i < (5 - depth); i++) {
				strBuf.append("\n");
			}
		}
		return strBuf.toString();

	}

	/**
	 * Recurses through the "tree" of operators and creates a textual rendering
	 * of the operators and conditions
	 * 
	 * @param aOper
	 * @param aStrBuf
	 * @param aLevel
	 * 	 */
	protected boolean isComplexRecurse(DBWhereOperator aParent, String aName) {
		if (!aParent.getName().equals(aName)) {
			return true;
		}

		// Check number of Children
		if (aParent.getNumChildern() < 2)
			return false;

		for (Enumeration e = aParent.getEnumeration(); e.hasMoreElements();) {
			DBWhereIFace item = (DBWhereIFace) e.nextElement();
			if (item instanceof DBWhereOperator) {
				DBWhereOperator oper = (DBWhereOperator) item;
				if (!oper.isClosure() && oper.getNumChildern() > 1) {
					boolean isComplex = isComplexRecurse(oper, aName);
					if (isComplex) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Generates a textual representation of the "where" clause
	 * 
	 */
	public boolean isComplex() {
		if (mModel.getSize() > 0) {
			if (mModel.getElementAt(0) instanceof DBWhereOperator) {
				DBWhereOperator oper = (DBWhereOperator) mModel.getElementAt(0);
				return isComplexRecurse(oper, oper.getName());
			}
		}
		return false;

	}

	/**
	 * Generates a textual representation of the "where" clause and displays it
	 * 
	 */
	protected void generateAndSetWhereText() {
		mEditorPane.setText(generateWhereSQL(false));
	}

	/**
	 * Populates the table combobox with the table names from the schema
	 * 
	 */
	/*
	 * protected void populateTableCombobox() {
	 * mTablesCombobox.removeAllItems(); Vector tables = mSchema.getTables(); if
	 * (tables != null && tables.size() > 0) { for (Enumeration et =
	 * tables.elements(); et.hasMoreElements();) { DSTableIFace table =
	 * (DSTableIFace)et.nextElement(); mTablesCombobox.addItem(table.getName());
	 * } } }
	 */

	/**
	 * Add a filed name to the combo for each field in the table
	 * 
	 * @param aTableName
	 *            The table to use to fill the field combo
	 */
	/*
	 * protected void populateFieldComboboxFromTableName(String aTableName) {
	 * Vector tables = mSchema.getTables(); if (tables != null && tables.size()
	 * > 0) { for (Enumeration et = tables.elements(); et.hasMoreElements();) {
	 * DSTableIFace table = (DSTableIFace)et.nextElement(); if
	 * (table.getName().equals(aTableName)) { mFieldsCombobox.removeAllItems();
	 * Vector fields = table.getFields(); for (Enumeration ef =
	 * fields.elements(); ef.hasMoreElements();) { DSTableFieldIFace field =
	 * (DSTableFieldIFace)ef.nextElement(); if (!field.getName().equals("*")) {
	 * mFieldsCombobox.addItem(field.getName()); } } } } } }
	 */

	/**
	 * Given a selection in the "main" list it populates the Field Combobox from
	 * the table
	 * 
	 */
	protected void populateFieldComboboxFromMainList() {
		int inx = mList.getSelectedIndex();
		if (inx != -1) {
			DBWhereIFace item = (DBWhereIFace) mModel.getElementAt(inx);
			if (!item.isOperator()) {
				if (item instanceof DBWhereCondition) {
					DBWhereCondition cond = (DBWhereCondition) item;
					String selectedTableName = (String) mTablesCombobox
							.getSelectedItem();
					String tableName = cond.getTableName();
					if (!tableName.equals(selectedTableName)) {
						// populateFieldComboboxFromTableName(tableName);
						DBUIUtils.fillFieldCombobox(mSchema, tableName,
								mFieldsCombobox, false);

					}
				}
			}
		}
	}

	/**
	 * Returns the index of a String item in a combobox
	 * 
	 * @param aCBX
	 *            the combobox
	 * @param aName
	 *            the string name of the item
	 * @return the index in the combobox
	 */
	protected int getIndexForName(JComboBox aCBX, String aName) {
		for (int i = 0; i < aCBX.getItemCount(); i++) {
			String name = (String) aCBX.getItemAt(i);
			if (name.equals(aName)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Fills QueryDef from Model
	 */
	public void fillQueryDef(DBQueryDef aQueryDef) {
		mModel.fillQueryDef(aQueryDef);
	}

	// --------------------------------------------
	// -------- ListSelectionListener -------------
	// --------------------------------------------

	/**
	 * Upates the "inspector" ui when an item in the list is clicked on
	 */
	public void valueChanged(ListSelectionEvent e) {
		mRejectChanges = true;
		if (e == null || !e.getValueIsAdjusting()) {
			if (mCurrInspPanel != null) {
				mInspContainer.remove(mCurrInspPanel);
			}
			boolean enable = false;
			int selectedInx = mList.getSelectedIndex();
			if (selectedInx != -1 && mModel.getSize() > 0) {
				DBWhereIFace item = (DBWhereIFace) mModel
						.getElementAt(selectedInx);
				if (!item.isOperator()) {
					mCriteriaField.setText("");
					mCurrInspPanel = mCondInspPanel;

					if (item instanceof DBWhereCondition) {
						DBWhereCondition whereItem = (DBWhereCondition) item;
						mTablesCombobox.setSelectedIndex(getIndexForName(
								mTablesCombobox, whereItem.getTableName()));
						populateFieldComboboxFromMainList();
						mFieldsCombobox.setSelectedIndex(getIndexForName(
								mFieldsCombobox, item.getName()));
						mCondCombobox.setSelectedIndex(getIndexForName(
								mCondCombobox, whereItem.getOperator()));
						mCriteriaField.setText(whereItem.getCriteria());
						if (mCriteriaField.getText().length() == 0) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									mCriteriaField.requestFocus();
									mTablesCombobox.repaint();
									mFieldsCombobox.repaint();
									mCondCombobox.repaint();
								}
							});
						}
					}
				} else {
					mCurrInspPanel = mOperInspPanel;
					mOperCombobox.setSelectedIndex(getIndexForName(
							mOperCombobox, item.getName()));
				}
				mInspContainer.add(mCurrInspPanel, BorderLayout.SOUTH);
				enable = selectedInx != mModel.getSize() - 1
						|| selectedInx == 0 && mModel.getSize() == 1;
			}
			this.validate();
			this.repaint();
			mRemoveBtn.setEnabled(enable);

			enable = (selectedInx != (mModel.getSize() - 1) && selectedInx != -1)
					|| mModel.getSize() == 0;
			mAddCondBtn.setEnabled(enable);
			mAddAndOperBtn.setEnabled(enable);
			mAddOROperBtn.setEnabled(enable);
		}
		mRejectChanges = false;
	}

	// --------------------------------------------
	// ------------ ListDataListener --------------
	// --------------------------------------------
	public void contentsChanged(ListDataEvent e) {
		if (mModelListener != null) {
			mModelListener.tableChanged(null);
		}
	}

	public void intervalAdded(ListDataEvent e) {
	}

	public void intervalRemoved(ListDataEvent e) {
	}
}