/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-06-11 17:46:03 -0700 (Mon, 11 Jun 2012) $' 
 * '$Revision: 29919 $'
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
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SingleSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;

import org.kepler.objectmanager.data.DataType;
import org.kepler.objectmanager.data.db.Attribute;
import org.kepler.objectmanager.data.db.DSSchemaDef;
import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableDef;
import org.kepler.objectmanager.data.db.Entity;
import org.kepler.objectmanager.data.db.QBEditor;

import ptolemy.gui.PtFileChooser;
import ptolemy.gui.PtFilenameFilter;

/**
 * The Query Builder app which extends a JPanel. This enables it to be embedded
 * in an applet or an application.
 */
public class QBApp extends JPanel implements ChangeListener, TableModelListener {

	// The preferred size of the demo
	private static final int PREFERRED_WIDTH = 720;
	private static final int PREFERRED_HEIGHT = 640;
	private static final String ADD_MSG = "Add an item by clicking in the empty \"table\" cell, or drag a \"field\" item from the top panel to the bottom.";
	private static final String CHANGED_MSG = "Query has Changed.";

	private static final int STD_TAB = 0;
	// private static final int INTER_TAB = 1;
	// private static final int ADV_TAB = 2;
	private static final int SQL_TAB = 1;

	private boolean mDoingDemo = false;
	private boolean mDoingDemoTests = false;

	private JPanel mDemoPanel = null;

	private boolean mIsChanged = false;
	private boolean mNoOuterUI = false;

	// Status Bar
	private JTextField mStatusField = null;
	private JMenuBar mMenuBar = null;
	private GenericMenuAction mSaveQueryAction = null;
	private GenericMenuAction mSaveSchemaAction = null;
	private JFrame mFrame = null;
	private QBApplet mApplet = null;

	private JTabbedPane mTabbedPane = null;
	private int mTabInx = 0;
	private int mLastTabInx = 0;

	private DSSchemaIFace mSchema = null;
	private DBQueryDef mQueryDef = null;

	private QBSplitPaneStandard mStdSP = null;
	// private QBSplitPaneIntermediate mInterSP = null;
	// private QBSplitPaneAdvanced mAdvSP = null;
	private QBSplitPaneSQL mSQLSP = null;
	private QBBuilderInterface mBuilderTab = null;

	private TableModelListener mExternalTMListener = null;
	private QBEditor qbEditor = null;

	private static final int BUTTONXSIZE = 90;
	private static final int BUTTONYSIZE = 30;

	/**
	 * Constructor with Applet
	 * 
	 * @param aApplet
	 */
	public QBApp(QBEditor editor) {
		qbEditor = editor;
		mNoOuterUI = true;
		initialize();
	}

	/**
	 * Constructor with Applet
	 * 
	 * @param aApplet
	 */
	public QBApp(QBApplet aApplet) {
		this(aApplet, null);
	}

	/**
	 * QBApp Constructor
	 */
	public QBApp(QBApplet aApplet, GraphicsConfiguration gc) {

		// Note that the applet may null if this is started as an application
		mApplet = aApplet;

		// Create Frame here for app-mode
		if (!isApplet() && gc != null) {
			mFrame = new JFrame(gc);
		}
		initialize();
	}

	/**
	 * General Method for initializing the class
	 * 
	 */
	private void initialize() {
		setLayout(new BorderLayout());

		// set the preferred size of the demo
		setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));

		initializeUI();

		JEditorPane editorPane = new JEditorPane("text/text",
				"Generated SQL will go here!");
		editorPane.setEditable(false);

		// create tab
		mTabbedPane = new JTabbedPane();

		DBQueryDef queryDef = null;
		if (mDoingDemo) {
			queryDef = createDemoQuery();
			loadSchemaIntoUI(createDemoSchema());
		}

		add(mTabbedPane, BorderLayout.CENTER);
		JPanel buttonPane = createButtonPanel();
		add(buttonPane, BorderLayout.SOUTH);

		setStatus(ADD_MSG);

		if (mDoingDemo) {
			loadQuery(queryDef);
			// mIsChanged = false;
			// mLastTabInx = mQueryDef.isAdv() ? 1 : 0;
			// mTabbedPane.setSelectedIndex(mLastTabInx);
		}

		// Note that
		// we again must do this on the GUI thread using invokeLater.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				showQBApp();
			}
		});
	}

	private JPanel createButtonPanel() {
		JPanel wholePanel = new JPanel();
		wholePanel.setLayout(new BoxLayout(wholePanel, BoxLayout.Y_AXIS));
		wholePanel.add(Box.createVerticalStrut(10));
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(Box.createHorizontalGlue());
		JButton cancelButton = new JButton(new CancelAction(qbEditor));
		cancelButton.setPreferredSize(new Dimension(BUTTONXSIZE, BUTTONYSIZE));
		JButton OKButton = new JButton(new OKAction(qbEditor));
		OKButton.setPreferredSize(new Dimension(BUTTONXSIZE, BUTTONYSIZE));
		buttonPanel.add(OKButton);
		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(cancelButton);
		buttonPanel.add(Box.createHorizontalStrut(10));
		wholePanel.add(buttonPanel);
		wholePanel.add(Box.createVerticalStrut(10));
		return wholePanel;
	}

	/**
	 * Sets a new Schema into the UI and recreates the UI for it
	 * 
	 * @param aSchema
	 *            the new schema
	 */
	public void loadSchemaIntoUI(DSSchemaIFace aSchema) {
		if (aSchema == null)
			return;

		mSchema = aSchema;

		// if (mAdvSP != null)
		// mAdvSP.shutdown();

		try {
			mTabbedPane.removeAll();
		} catch (Exception e) {
			System.err.println(e);
		}

		mStdSP = null;
		// mInterSP = null;
		// mAdvSP = null;
		mSQLSP = null;

		/*
		 * if (!isApplet()) { java.lang.System.gc();
		 * java.lang.System.runFinalization();
		 * java.lang.System.runFinalizersOnExit(true); }
		 */

		// if (!firstTime) return;
		mStdSP = new QBSplitPaneStandard(mSchema);
		// mInterSP = new QBSplitPaneIntermediate(mSchema, this);
		// mAdvSP = new QBSplitPaneAdvanced(mSchema, this);
		mSQLSP = new QBSplitPaneSQL();

		// Initialize
		mStdSP.setTableModelListener(this);
		// mInterSP.setTableModelListener(this);
		// mAdvSP.setTableModelListener(this);

		mTabbedPane.add("General", mStdSP);
		// mTabbedPane.add("Intermediate", mInterSP);
		// mTabbedPane.add("Advanced", mAdvSP);
		mTabbedPane.add("SQL", mSQLSP);

		mTabbedPane.getModel().addChangeListener(this);
	}

	/**
	 * 
	 * @return returns a test/demo schema
	 */
	private DSSchemaDef createDemoSchema() {
		DSSchemaDef schema = null;
		if (mDoingDemo) {
			schema = new DSSchemaDef("Test Schema");
			boolean useTableEntity = true;
			if (useTableEntity) {
				Entity table1 = new Entity("", "Employees", "", new Boolean(
						true), Entity.ROWMAJOR, 0);
				table1.add(new Attribute("", "EmpNo", DataType.INT));
				table1.add(new Attribute("", "First Name", DataType.STR));
				table1.add(new Attribute("", "Last Name", DataType.STR));
				table1.add(new Attribute("", "Addr 1", DataType.STR));
				table1.add(new Attribute("", "Addr 2", DataType.STR));
				table1.add(new Attribute("", "City", DataType.STR));
				table1.add(new Attribute("", "State", DataType.STR));
				table1.add(new Attribute("", "Zip", DataType.STR));
				schema.addTable(table1);

				table1 = new Entity("", "EmpDept", "", new Boolean(true),
						Entity.ROWMAJOR, 0);
				table1.add(new Attribute("", "EmpNo", DataType.INT));
				table1.add(new Attribute("", "DeptNo", DataType.INT));
				schema.addTable(table1);

				table1 = new Entity("", "Department", "", new Boolean(true),
						Entity.ROWMAJOR, 0);
				table1.add(new Attribute("", "DeptNo", DataType.INT));
				table1.add(new Attribute("", "Name", DataType.STR));
				for (int i = 0; i < 15; i++) {
					table1.add(new Attribute("", "User Field " + i,
							DataType.STR));
				}
				schema.addTable(table1);

				table1 = new Entity("", "EmpDoc", "", new Boolean(true),
						Entity.ROWMAJOR, 0);
				table1.add(new Attribute("", "EmpNo", DataType.INT));
				table1.add(new Attribute("", "DocNo", DataType.INT));
				schema.addTable(table1);

				table1 = new Entity("", "Document", "", new Boolean(true),
						Entity.ROWMAJOR, 0);
				table1.add(new Attribute("", "DocNo", DataType.INT));
				table1.add(new Attribute("", "Title", DataType.STR));
				table1.add(new Attribute("", "Desc", DataType.STR));
				table1.add(new Attribute("", "Biblo", DataType.STR));
				schema.addTable(table1);

			} else {
				DSTableDef table1 = new DSTableDef("Employees");
				table1.addPrimaryKey("EmpNo", DataType.INT, null);
				table1.addField("First Name", DataType.STR, null);
				table1.addField("Last Name", DataType.STR, null);
				table1.addField("Addr 1", DataType.STR, null);
				table1.addField("Addr 2", DataType.STR, null);
				table1.addField("City", DataType.STR, null);
				table1.addField("State", DataType.STR, null);
				table1.addField("Zip", DataType.STR, null);
				schema.addTable(table1);

				table1 = new DSTableDef("EmpDept");
				table1.addPrimaryKey("EmpNo", DataType.INT, null);
				table1.addSecondaryKey("DeptNo", DataType.INT, null);
				schema.addTable(table1);

				table1 = new DSTableDef("Department");
				table1.addPrimaryKey("DeptNo", DataType.INT, null);
				table1.addField("Name", DataType.STR, null);
				for (int i = 0; i < 15; i++) {
					table1.addField("User Field " + i, DataType.STR, null);
				}
				schema.addTable(table1);

				table1 = new DSTableDef("EmpDoc");
				table1.addPrimaryKey("EmpNo", DataType.INT, null);
				table1.addSecondaryKey("DocNo", DataType.INT, null);
				schema.addTable(table1);

				table1 = new DSTableDef("Document");
				table1.addPrimaryKey("DocNo", DataType.INT, null);
				table1.addField("Title", DataType.STR, null);
				table1.addField("Desc", DataType.STR, null);
				table1.addField("Biblo", DataType.STR, null);
				schema.addTable(table1);
			}
		}
		return schema;
	}

	/**
	 * 
	 * @return returns a demo query object
	 */
	private DBQueryDef createDemoQuery() {
		DBQueryDef queryDef = new DBQueryDef();
		Vector joins = new Vector();
		String tableNames[] = { "Employees", "EmpDept", "EmpDept",
				"Department", "Employees", "EmpDoc", "EmpDoc", "Document" };
		String fieldNames[] = { "EmpNo", "EmpNo", "DeptNo", "DeptNo", "EmpNo",
				"EmpNo", "DocNo", "DocNo" };
		int tableIds[] = { 0, 1, 1, 2, 0, 3, 3, 4 };
		for (int i = 0; i < tableNames.length; i++) {
			Vector missingValue = null;
			DBSelectTableModelItem item = new DBSelectTableModelItem(
					tableNames[i], fieldNames[i], "", false, "", "",
					missingValue);
			item.setTableId(tableIds[i]);
			joins.add(item);
		}
		queryDef.setJoins(joins);

		int tableFrameIds[] = { 0, 1, 2, 3, 4 };
		String tableFrameNames[] = { "Employees", "EmpDept", "Department",
				"EmpDoc", "Document" };
		for (int i = 0; i < tableFrameIds.length; i++) {
			queryDef.addTable(tableFrameIds[i], tableFrameNames[i], -1, -1);
		}
		queryDef.setIsAdv(true);
		return queryDef;
	}

	// *******************************************************
	// *************** Load UI ******************
	// *******************************************************

	public void initializeUI() {

		JPanel top = new JPanel();
		top.setLayout(new BorderLayout());
		add(top, BorderLayout.NORTH);

		mMenuBar = createMenus();
		if (mMenuBar != null)
			top.add(mMenuBar, BorderLayout.NORTH);

		mStatusField = new JTextField("");
		mStatusField.setEditable(false);
		add(mStatusField, BorderLayout.SOUTH);

		mDemoPanel = new JPanel();
		mDemoPanel.setLayout(new BorderLayout());
		mDemoPanel.setBorder(new EtchedBorder());
		mDemoPanel.setBackground(Color.green);
		add(mDemoPanel, BorderLayout.CENTER);

	}

	/**
	 * Set qb tableau
	 * 
	 * @param tableau
	 */
	public void setQBEditor(QBEditor editor) {
		this.qbEditor = editor;
	}

	/**
	 * @param b
	 * 	 */
	protected PropertyChangeListener createActionChangeListener(JMenuItem b) {
		return new ActionChangedListener(b);
	}

	/**
	 * 
	 * @author globus
	 * 
	 *         TODO To change the template for this generated type comment go to
	 *         Window - Preferences - Java - Code Generation - Code and Comments
	 */
	private class ActionChangedListener implements PropertyChangeListener {
		JMenuItem menuItem;

		ActionChangedListener(JMenuItem mi) {
			super();
			this.menuItem = mi;
		}

		public void propertyChange(PropertyChangeEvent e) {
			String propertyName = e.getPropertyName();
			if (e.getPropertyName().equals(Action.NAME)) {
				String text = (String) e.getNewValue();
				menuItem.setText(text);
			} else if (propertyName.equals("enabled")) {
				Boolean enabledState = (Boolean) e.getNewValue();
				menuItem.setEnabled(enabledState.booleanValue());
			}
		}
	}

	/**
	 * Create menus
	 */
	public JMenuBar createMenus() {
		JMenuBar menuBar = null;

		if (!isApplet() && !mNoOuterUI) {
			menuBar = new JMenuBar();
			JMenu fileMenu = (JMenu) menuBar.add(new JMenu("File"));
			fileMenu.setMnemonic('F');
			createMenuItem(fileMenu, "Open Query", "O", "Open Query File",
					new GenericMenuAction(this,
							GenericMenuAction.OPEN_QUERY_ACTION), true);
			createMenuItem(fileMenu, "Save Query", "S", "Save Query",
					mSaveQueryAction = new GenericMenuAction(this,
							GenericMenuAction.SAVE_QUERY_ACTION), false);
			fileMenu.addSeparator();
			createMenuItem(fileMenu, "Open Schema", "O", "Open Schema File",
					new GenericMenuAction(this,
							GenericMenuAction.OPEN_SCHEMA_ACTION), true);
			createMenuItem(fileMenu, "Save Schema", "S", "Save Schema",
					mSaveSchemaAction = new GenericMenuAction(this,
							GenericMenuAction.SAVE_SCHEMA_ACTION), false);
			fileMenu.addSeparator();
			createMenuItem(fileMenu, "Exit", "x", "Exit Appication",
					new GenericMenuAction(this, GenericMenuAction.EXIT_ACTION),
					true);

			if (mDoingDemoTests) {
				fileMenu = (JMenu) menuBar.add(new JMenu("Tests"));
				fileMenu.setMnemonic('T');
				createMenuItem(fileMenu, "Test Queries", "e", "Test Queries",
						new GenericMenuAction(this,
								GenericMenuAction.QUERY_TESTS_ACTION), true);
			}
		}

		return menuBar;
	}

	/**
	 * Creates a generic menu item
	 */
	public JMenuItem createMenuItem(JMenu menu, String label, String mnemonic,
			String accessibleDescription, Action action, boolean enabled) {
		JMenuItem mi = (JMenuItem) menu.add(new JMenuItem(label));
		mi.setMnemonic(mnemonic.charAt(0));
		mi.getAccessibleContext().setAccessibleDescription(
				accessibleDescription);
		mi.addActionListener(action);
		if (action == null) {
			mi.setEnabled(false);
		} else {
			action.addPropertyChangeListener(createActionChangeListener(mi));
			action.setEnabled(enabled);
		}
		return mi;
	}

	/**
	 * Bring up the QBApp demo by showing the frame (only applicable if coming
	 * up as an application, not an applet);
	 */
	public void showQBApp() {
		if (!isApplet() && getFrame() != null) {
			// put QBApp in a frame and show it
			JFrame f = getFrame();
			f.setTitle("Query Builder");
			f.getContentPane().add(this, BorderLayout.CENTER);
			f.pack();

			Rectangle screenRect = f.getGraphicsConfiguration().getBounds();
			Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
					f.getGraphicsConfiguration());

			// Make sure we don't place the demo off the screen.
			int centerWidth = screenRect.width < f.getSize().width ? screenRect.x
					: screenRect.x + screenRect.width / 2 - f.getSize().width
							/ 2;
			int centerHeight = screenRect.height < f.getSize().height ? screenRect.y
					: screenRect.y + screenRect.height / 2 - f.getSize().height
							/ 2;

			centerHeight = centerHeight < screenInsets.top ? screenInsets.top
					: centerHeight;

			f.setLocation(centerWidth, centerHeight);
			f.show();
		}
	}

	/**
	 * Determines if this is an applet or application
	 */
	public boolean isApplet() {
		return (mApplet != null);
	}

	/**
	 * Returns the applet instance
	 */
	public QBApplet getApplet() {
		return mApplet;
	}

	/**
	 * Returns the frame instance
	 */
	public JFrame getFrame() {
		return mFrame;
	}

	/**
	 * Returns the menubar
	 */
	public JMenuBar getMenuBar() {
		return mMenuBar;
	}

	/**
	 * Set the status
	 */
	public void setStatus(String s) {
		// do the following on the gui thread
		SwingUtilities.invokeLater(new QBAppRunnable(this, s) {
			public void run() {
				mQBApp.mStatusField.setText((String) obj);
			}
		});
	}

	public void setIsChanged(boolean aIsChanged) {
		mIsChanged = aIsChanged;
		if (mSaveQueryAction != null) {
			mSaveQueryAction.setEnabled(aIsChanged);
		}
	}

	public void setExternalTMListener(TableModelListener aL) {
		mExternalTMListener = aL;
	}

	/**
	 * Saves Query to a file
	 */
	public Writer saveFile(String aTitle, String aFilterStr, String aFilterTitle) {
		PtFileChooser chooser = new PtFileChooser(mFrame, "Save " + aTitle, JFileChooser.SAVE_DIALOG);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setSelectedFile(null);
		final DBFileFilter dbFileFilter = new DBFileFilter(aFilterStr, aFilterTitle);
		chooser.addChoosableFileFilter(new PtFilenameFilter() {
			@Override
		    public boolean accept(File file) {
		    	return dbFileFilter.accept(file);
		    }

			@Override
		    public boolean accept(File directory, String name) {
		    	return dbFileFilter.accept(new File(directory, name));
		    }

			@Override
		    public String getDescription() {
		    	return dbFileFilter.getDescription();
		    }
		});
		int retval = chooser.showDialog(this, "Save");
		if (retval == JFileChooser.APPROVE_OPTION) {
			// declared here only to make visible to finally clause; generic
			// reference
			Writer output = null;
			try {
				// use buffering
				return new BufferedWriter(new FileWriter(chooser
						.getSelectedFile()));
			} catch (FileNotFoundException ex) {
				JOptionPane.showMessageDialog(this, ex.toString());
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, ex.toString());
			} finally {
				// flush and close both "output" and its underlying FileWriter
				try {
					if (output != null)
						output.close();
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(this, ex.toString());
				}
			}
		}
		return null;
	}

	/**
	 * Saves Query to a file
	 */
	public void saveQueryFile() {
		Writer output = saveFile("Query", "query", "Query Files");
		if (output != null) {
			DBQueryDef queryDef = new DBQueryDef();
			mBuilderTab.fillQueryDef(queryDef);

			try {
				output.write(DBQueryDefParserEmitter.emitXML(queryDef));
				output.close();
				setIsChanged(false);
				setStatus(ADD_MSG);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, ex.toString());
			}
		}
	}

	/**
	 * Saves Query to a file
	 */
	public void saveSchemaFile() {
		Writer output = saveFile("Schema", "schema", "QueSchemary Files");
		if (output != null) {
			try {
				output.write(DBSchemaParserEmitter.emitXML(mSchema));
				output.close();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, ex.toString());
			}
		}
	}

	/**
	 * Loads the QueryDef object into the UI
	 * 
	 * @param aQueryDef
	 *            the query def
	 */
	protected void loadQuery(DBQueryDef aQueryDef) {
		if (aQueryDef != null) {
			mQueryDef = aQueryDef;

			QBBuilderInterface builder = null;
			int tabInx = SQL_TAB;

			boolean debug = false;
			/*
			 * if (debug) { int status = mInterSP.buildFromQueryDef(aQueryDef);
			 * if (status == DBQueryDef.BUILD_TOO_COMPLEX_WHERE || status ==
			 * DBQueryDef.BUILD_TOO_COMPLEX_JOINS) {
			 * JOptionPane.showMessageDialog(this,
			 * "Switching to the Advanced View,\nthe query is too complex for the Standard Tab."
			 * ); tabInx = ADV_TAB; builder = mAdvSP;
			 * builder.buildFromQueryDef(mQueryDef); } else { tabInx =
			 * INTER_TAB; builder = mInterSP; } } else { if (aQueryDef.isAdv())
			 * { int status = mInterSP.buildFromQueryDef(aQueryDef); if (status
			 * == DBQueryDef.BUILD_TOO_COMPLEX_WHERE || status ==
			 * DBQueryDef.BUILD_TOO_COMPLEX_JOINS) {
			 * JOptionPane.showMessageDialog(this,
			 * "Switching to the Advanced View,\nthe query is too complex for the Intermediate Tab."
			 * ); tabInx = ADV_TAB; builder = mAdvSP;
			 * builder.buildFromQueryDef(mQueryDef); } else { tabInx =
			 * INTER_TAB; builder = mInterSP; } } else { int status =
			 * mStdSP.buildFromQueryDef(aQueryDef); if (status ==
			 * DBQueryDef.BUILD_TOO_COMPLEX_WHERE || status ==
			 * DBQueryDef.BUILD_TOO_COMPLEX_JOINS) {
			 * mTabbedPane.setSelectedIndex(INTER_TAB);
			 * JOptionPane.showMessageDialog(this,
			 * "Switching to the Intermediate View,\nthe query is too complex for the Standard Tab."
			 * ); tabInx = INTER_TAB; builder = mInterSP;
			 * builder.buildFromQueryDef(mQueryDef); } else { tabInx = STD_TAB;
			 * builder = mStdSP; } }
			 * 
			 * }
			 */

			// only handle stand panel
			int status = mStdSP.buildFromQueryDef(aQueryDef);
			if (status == DBQueryDef.BUILD_TOO_COMPLEX_WHERE
					|| status == DBQueryDef.BUILD_TOO_COMPLEX_JOINS) {
				JOptionPane.showMessageDialog(this,
						"The query is too complex for the Standard Tab.");
			} else {
				tabInx = STD_TAB;
				builder = mStdSP;
			}

			if (mBuilderTab == null) {
				mBuilderTab = builder;
				mLastTabInx = tabInx;
				mTabInx = tabInx;
			}
			mTabbedPane.setSelectedIndex(tabInx);

		} else {
			JOptionPane
					.showMessageDialog(this,
							"There was an error parsing the query.\nIt could not be loaded.");
		}
		setIsChanged(false);
		setStatus(ADD_MSG);
	}

	/**
	 * Fills QueryDef from Model
	 */
	public void fillQueryDef(DBQueryDef aQueryDef) {
		if (aQueryDef == null)
			return;

		mBuilderTab.fillQueryDef(aQueryDef);

	}

	/**
	 * Sets which builder tab should be used if the query is null
	 * 
	 */
	public void initializeBuilder() {
		if (mBuilderTab == null) {
			mBuilderTab = mStdSP;
			mTabInx = STD_TAB;
			mLastTabInx = STD_TAB;
			mTabbedPane.setSelectedIndex(mLastTabInx);
		}

	}

	/**
	 * Loads a Schema and a Query into the Builder
	 * 
	 * @param aSchema
	 *            The schema to be used
	 * @param aQueryDef
	 *            the query def to be loaded (can be null)
	 */
	public void set(DSSchemaIFace aSchema, DBQueryDef aQueryDef) {
		mQueryDef = aQueryDef;
		loadSchemaIntoUI(aSchema);
		if (mQueryDef != null) {
			loadQuery(mQueryDef);
		} else {
			initializeBuilder();
		}
	}

	/**
	 * 
	 * Opens a Query and loads into the builder
	 */
	public String openFile(String aTitle, String aFilterStr, String aFilterTitle) {
		PtFileChooser chooser = new PtFileChooser(mFrame, "Open " + aTitle, JFileChooser.OPEN_DIALOG);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setSelectedFile(null);
		final DBFileFilter dbFileFilter = new DBFileFilter(aFilterStr, aFilterTitle);
		chooser.addChoosableFileFilter(new PtFilenameFilter() {
			@Override
		    public boolean accept(File file) {
		    	return dbFileFilter.accept(file);
		    }

			@Override
		    public boolean accept(File directory, String name) {
		    	return dbFileFilter.accept(new File(directory, name));
		    }

			@Override
		    public String getDescription() {
		    	return dbFileFilter.getDescription();
		    }
		});
		JComponent frame = this;

		// clear the preview from the previous display of the chooser
		int retval = chooser.showDialog(frame, "Open");
		if (retval == JFileChooser.APPROVE_OPTION) {
			File theFile = chooser.getSelectedFile();
			if (theFile != null) {
				if (theFile.isDirectory()) {
					// JOptionPane.showMessageDialog(frame,
					// "You chose this directory: " + theFile.getPath());
				} else {
					return theFile.getPath();
				}
			}

		} else if (retval == JFileChooser.CANCEL_OPTION) {
			// JOptionPane.showMessageDialog(frame,
			// "User cancelled operation. No file was chosen.");
		} else if (retval == JFileChooser.ERROR_OPTION) {
			JOptionPane.showMessageDialog(frame,
					"An error occured. No file was chosen.");
		} else {
			JOptionPane.showMessageDialog(frame, "Unknown operation occured.");
		}
		return null;
	}

	/**
	 * 
	 * Opens a Query and loads into the builder
	 */
	public void openQueryFile() {
		String path = openFile("Query", "query", "Query Files");
		if (path != null) {
			mQueryDef = DBQueryDefParserEmitter.readQueryDef(mSchema, path);
			if (mQueryDef == null) {
				JOptionPane.showMessageDialog(this, DBQueryDefParserEmitter
						.getErrorCodeText(), "Query Error",
						JOptionPane.ERROR_MESSAGE);
			} else {
				mBuilderTab = null; // must be done here
				loadQuery(mQueryDef);
			}
		}
	}

	/**
	 * 
	 * Opens a Query and loads into the builder
	 */
	public void openSchemaFile() {
		String path = openFile("Schema", "schema", "Schema Files");
		if (path != null) {

			DSSchemaIFace schema = DBSchemaParserEmitter.readSchemaDef(path);
			if (schema != null) {
				mQueryDef = null;
				loadSchemaIntoUI(schema);
				initializeBuilder();
			}
		}
	}

	/**
	 * Helper method
	 * 
	 * @param aFileName
	 */
	private void runQueryTest(String aFileName) {
		DBQueryDef queryDef = DBQueryDefParserEmitter.readQueryDef(mSchema,
				aFileName);
		System.err.print(aFileName + " - ");
		System.err.println(queryDef == null ? DBQueryDefParserEmitter
				.getErrorCodeText() : "OK");

	}

	/**
	 * 
	 * Loads various queries for testing the parser
	 */
	public void testQueries() {
		String dirStr = "/home/globus/";

		runQueryTest(dirStr + "good.query");
		runQueryTest(dirStr + "join_fieldname_bad.query");
		// runQueryTest(dirStr + "join_tableid_bad.query");
		runQueryTest(dirStr + "join_tablename_bad.query");
		runQueryTest(dirStr + "select_fieldname_bad.query");
		runQueryTest(dirStr + "select_tablename_bad.query");
		runQueryTest(dirStr + "table_tablename_bad.query");
		runQueryTest(dirStr + "where_fieldname_bad.query");
		runQueryTest(dirStr + "where_tablename_bad.query");
	}

	/**
	 * Returns the SQL in a TAB of the builder and sets it into the SQL Tab's
	 * readonly text editor
	 * 
	 * @param aLastInx
	 *            the last builder tab to be used
	 * @return the SQL string of that tab
	 */
	public String processTabForSQL(int aLastInx) {
		switch (aLastInx) {
		case 0:
			return mStdSP.createSQL();

			// case 1 :
			// return mInterSP.createSQL();

			// case 2 :
			// return mAdvSP.createSQL();

		case SQL_TAB:
			// should never get here
			break;
		}
		return null;
	}

	/**
	 * Return the appropriate builder for a given tab index
	 * 
	 * @param aTabIndex
	 *            the tabe index
	 * @return the QBBuilderInterface object for the tab index
	 */
	private QBBuilderInterface getBuilderFromTabIndex(int aTabIndex) {
		switch (aTabIndex) {
		case 0:
			return mStdSP;
			// case 1 : return mInterSP;
			// case 2 : return mAdvSP;
		}
		return null;
	}

	/**
	 * Process when TAB changes
	 */
	public void processTabChange() {
		// Bail if there is no valid Query Obj
		/*
		 * if (mQueryDef == null) { System.out.println("the mQueryDef= null");
		 * return; }
		 */

		if (mStdSP != null) {
			// System.out.println("clear the selection");
			DBSelectTableUIBase table = mStdSP.getSelectedTableView();
			if (table != null) {
				TableCellEditor editor = table.getCellEditor();
				if (editor != null) {
					editor.stopCellEditing();
				}
			}
			/*
			 * int column = table.getSelectedColumn();
			 * System.out.println("selected column "+column); int row =
			 * table.getSelectedRow(); System.out.println("selected row "+row);
			 * Object obj = table.getModel().getValueAt(row, column);
			 * System.out.println("!!!!!!!!!the selected value is "+obj); column
			 * = table.getSelectedColumn();
			 * System.out.println("selected column "+column); row =
			 * table.getSelectedRow(); table.getModel().setValueAt(obj, row,
			 * column);
			 * 
			 * obj = table.getModel().getValueAt(row, column);
			 * System.out.println("!!!!!!!!!the selected value is "+obj);
			 * table.transferFocus();
			 */

		}

		// This can happen if the user clicks to the SQL tab and
		// then back to the same tab as before
		//
		// So we do not try to process (or convert) the Query from one tab to
		// the next
		if (mLastTabInx == mTabInx) {
			return;
		}

		if (mTabInx == SQL_TAB) {
			mSQLSP.setText(processTabForSQL(mLastTabInx));
			return;
		}

		// System.out.println("mTableInx is "+mTabInx);
		QBBuilderInterface newBldr = getBuilderFromTabIndex(mTabInx);
		// System.out.println("newbldr is "+newBldr);

		boolean cachedIsChanged = false;
		boolean doSwitch = true;
		if (!mBuilderTab.canConvertTo(newBldr)) {
			String msg = "Data Loss will occur for this query when switching to from "
					+ mBuilderTab.getName() + " to " + newBldr.getName();
			Object[] options = { "Continue", "Do Not Switch" };
			int option = JOptionPane.showOptionDialog(null, msg,
					"Switching Error", JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE, null, options, options[0]);

			if (option == JOptionPane.NO_OPTION) {
				mTabInx = mBuilderTab.getType();
				mTabbedPane.setSelectedIndex(mTabInx);
				doSwitch = false;
			}

		}

		if (doSwitch) {
			mTabInx = newBldr.getType();
			cachedIsChanged = true;
			mQueryDef = new DBQueryDef();

			mBuilderTab.fillQueryDef(mQueryDef);
			newBldr.buildFromQueryDef(mQueryDef);

			mBuilderTab = newBldr;

			mTabbedPane.setSelectedIndex(mTabInx);
		}

		setIsChanged(cachedIsChanged);
		// setStatus(mIsChanged? CHANGED_MSG : msgStr);

	}

	/**
	 * @return whether the SQL tab is the current Tab
	 */
	public boolean isSQLTab() {
		return mTabInx == SQL_TAB;
	}

	/**
	 * 
	 * @return the SQL string for the "current" or more recent builder tab
	 */
	public String getSQLString() {
		return "";
	}

	// *******************************************************
	// ****************** Runnables ***********************
	// *******************************************************

	/**
	 * Generic QBApp runnable. This is intended to run on the AWT gui event
	 * thread so as not to muck things up by doing gui work off the gui thread.
	 * Accepts a QBApp and an Object as arguments, which gives subtypes of this
	 * class the two "must haves" needed in most runnables for this demo.
	 */
	class QBAppRunnable implements Runnable {

		protected QBApp mQBApp;

		protected Object obj;

		public QBAppRunnable(QBApp aQBApp, Object obj) {
			this.mQBApp = aQBApp;
			this.obj = obj;
		}

		public void run() {
		}
	}

	// *******************************************************
	// ******************** Actions ***********************
	// *******************************************************

	class GenericMenuAction extends AbstractAction {
		public static final int OPEN_QUERY_ACTION = 0;
		public static final int SAVE_QUERY_ACTION = 1;
		public static final int OPEN_SCHEMA_ACTION = 2;
		public static final int SAVE_SCHEMA_ACTION = 3;
		public static final int EXIT_ACTION = 4;
		public static final int QUERY_TESTS_ACTION = 5;

		QBApp mQueryBuilder;
		int mActionType;

		protected GenericMenuAction(QBApp aQBApp, int aActionType) {
			super("Action");
			mQueryBuilder = aQBApp;
			mActionType = aActionType;
		}

		public void actionPerformed(ActionEvent e) {
			switch (mActionType) {
			case OPEN_QUERY_ACTION:
				mQueryBuilder.openQueryFile();
				break;
			case SAVE_QUERY_ACTION:
				mQueryBuilder.saveQueryFile();
				break;
			case OPEN_SCHEMA_ACTION:
				mQueryBuilder.openSchemaFile();
				break;
			case SAVE_SCHEMA_ACTION:
				mQueryBuilder.saveSchemaFile();
				break;
			case EXIT_ACTION: {
				System.out.println("here!!!!!!!!!!!11");
				if (mIsChanged) {
					int x0 = JOptionPane.CLOSED_OPTION;
					int x1 = JOptionPane.YES_OPTION;
					int x2 = JOptionPane.NO_OPTION;
					int x3 = JOptionPane.OK_OPTION;
					int x4 = JOptionPane.CLOSED_OPTION;
					Object[] options = { "Discard and Exit", "Save and Exit",
							"Cancel" };
					String msg = "You have changed the query, do you wish to save it before exiting?";
					int option = JOptionPane.showOptionDialog(null, msg,
							"Exiting", JOptionPane.DEFAULT_OPTION,
							JOptionPane.WARNING_MESSAGE, null, options,
							options[1]);
					System.out.println("the option chose is " + option);
					if (option == JOptionPane.NO_OPTION) {
						mQueryBuilder.saveQueryFile();
					} else if (option == 2) {
						return;
					}
				}
				System.exit(0);
			}
				break;
			case QUERY_TESTS_ACTION:
				mQueryBuilder.testQueries();
				break;
			}

		}
	}

	class CancelAction extends AbstractAction {
		QBEditor frame;

		protected CancelAction(QBEditor frame) {
			super("Cancel");
			this.frame = frame;
		}

		public void actionPerformed(ActionEvent e) {
			if (frame != null) {
				frame.closeWindows();
			}

			// System.exit(0);

		}
	}

	class OKAction extends AbstractAction {

		QBEditor frame;

		protected OKAction(QBEditor frame) {
			super("OK");
			this.frame = frame;

		}

		public void actionPerformed(ActionEvent e) {
			if (frame != null) {
				if (mStdSP != null) {

					DBSelectTableUIBase table = mStdSP.getSelectedTableView();
					if (table != null) {
						TableCellEditor editor = table.getCellEditor();
						if (editor != null) {
							editor.stopCellEditing();
						}
					}
				}

				frame.save();
			}

		}
	}

	// -----------------------------------------------------------------------------
	// -- ChangeListener Interface
	// -----------------------------------------------------------------------------

	/**
	 * Notify when TAB changes
	 */
	public void stateChanged(ChangeEvent e) {
		if (mTabInx != SQL_TAB) {
			mLastTabInx = mTabInx;
		}

		SingleSelectionModel model = (SingleSelectionModel) e.getSource();
		mTabInx = model.getSelectedIndex();

		processTabChange();
	}

	// -----------------------------------------------------------------------------
	// -- TableModelListener Interface
	// -----------------------------------------------------------------------------

	public void tableChanged(TableModelEvent e) {
		setIsChanged(true);
		setStatus(CHANGED_MSG);
		if (mExternalTMListener != null) {
			mExternalTMListener.tableChanged(e);
		}
	}

	// -----------------------------------------------------------------------------
	// -- Application MAIN
	// -----------------------------------------------------------------------------

	/**
	 * QBApp Main. Called only if we're an application, not an applet.
	 */
	public static void main(String[] args) {
		// Create QBApp on the default monitor
		QBApp qbApp = new QBApp(null, GraphicsEnvironment
				.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration());
	}

}