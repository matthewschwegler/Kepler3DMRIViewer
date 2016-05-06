/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-07-10 16:34:53 -0700 (Tue, 10 Jul 2012) $' 
 * '$Revision: 30149 $'
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

package org.kepler.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.KARCacheManager;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.library.LibIndex;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.objectmanager.repository.Repository;
import org.kepler.objectmanager.repository.RepositoryManager;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.gui.JFileChooserBugFix;
import ptolemy.gui.PtFileChooser;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * @author Aaron Schultz
 * 
 */
public class ComponentLibraryPreferencesTab extends JPanel implements
		PreferencesTab {
	private static final long serialVersionUID = -5778742096181302681L;
	private static final Log log = LogFactory
			.getLog(ComponentLibraryPreferencesTab.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private TableauFrame _frame;
	private String _tabName;

	private JButton _newSource;
	private JButton _removeSource;
	private JButton _defaultSources;
	private JButton _buildLibrary;
	private JTable _sourceList;

	/**
	 * Keep a reference to the instance of LocalRepositoryManager for ease of
	 * use.
	 */
	private LocalRepositoryManager _localRepoManager;
	/**
	 * Keep a reference to the instance of RepositoryManager for ease of use.
	 */
	private RepositoryManager _repositoryManager;

	/**
	 * 
	 */
	public ComponentLibraryPreferencesTab() {
		_localRepoManager = LocalRepositoryManager.getInstance();
		_localRepoManager.setCheckpoint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.PreferencesTab#getTabName()
	 */
	public String getTabName() {
		return _tabName;
	}

	public void setTabName(String name) {
		_tabName = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.PreferencesTab#initializeTab()
	 */
	public void initializeTab() throws Exception {

		_repositoryManager = RepositoryManager.getInstance();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		initDoc();
		initTopPanel();
		initSourceList();

		this.setMinimumSize(new Dimension(300, 300));
		this.setPreferredSize(new Dimension(300, 300));
		this.setBackground(TabManager.BGCOLOR);
	}

	/**
	 * Initialize the top panel that contains controls for the table.
	 */
	private void initTopPanel() {

		_newSource = new JButton(
				StaticResources.getDisplayString(
						"general.ADD",
						"Add"));
		_newSource.setPreferredSize(new Dimension(100, 50));
		_newSource.addActionListener(new AddSourceListener());

		_removeSource = new JButton(
				StaticResources.getDisplayString(
						"general.REMOVE",
						"Remove"));
		_removeSource.setPreferredSize(new Dimension(100, 50));
		_removeSource.addActionListener(new RemoveSourceListener());

		_defaultSources = new JButton(
				StaticResources.getDisplayString(
						"preferences.useDefaults",
						"Use Defaults"));
		_defaultSources.setPreferredSize(new Dimension(100, 50));
		_defaultSources.addActionListener(new DefaultSourcesListener());

		_buildLibrary = new JButton(
				StaticResources.getDisplayString(
						"preferences.build",
						"Build"));
		_buildLibrary.setPreferredSize(new Dimension(100, 50));
		_buildLibrary.addActionListener(new BuildLibraryListener());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.setBackground(TabManager.BGCOLOR);
		buttonPanel.setAlignmentX(RIGHT_ALIGNMENT);
		buttonPanel.add(_newSource);
		buttonPanel.add(_removeSource);
		buttonPanel.add(_defaultSources);
		buttonPanel.add(_buildLibrary);

		LibraryManager lm = LibraryManager.getInstance();

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(buttonPanel, BorderLayout.CENTER);

		add(topPanel);

	}

	/**
	 * Initialize the source list table.
	 */
	private void initSourceList() {

		try {

			ComponentSourceTableModel cstm = new ComponentSourceTableModel();
			_sourceList = new JTable(cstm);
			_sourceList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			_sourceList.setShowGrid(true);
			_sourceList.setShowHorizontalLines(true);
			_sourceList.setShowVerticalLines(true);
			_sourceList.setGridColor(Color.lightGray);
			_sourceList.setIntercellSpacing(new Dimension(5, 5));
			_sourceList.setRowHeight(_sourceList.getRowHeight() + 10);
			_sourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			if (isDebugging) {
				log.debug("intercellspacing: "
						+ _sourceList.getIntercellSpacing().toString());
				log.debug("getRowHeight(): " + _sourceList.getRowHeight());
			}

			// Search column
			TableColumn c0 = _sourceList.getColumnModel().getColumn(0);
			c0.setMinWidth(50);
			c0.setPreferredWidth(60);
			c0.setMaxWidth(100);
			c0.setResizable(true);

			// Save column
			TableColumn c1 = _sourceList.getColumnModel().getColumn(1);
			c1.setMinWidth(50);
			c1.setPreferredWidth(60);
			c1.setMaxWidth(100);
			c1.setResizable(true);

			// Type column
			TableColumn c2 = _sourceList.getColumnModel().getColumn(2);
			c2.setMinWidth(50);
			c2.setPreferredWidth(60);
			c2.setMaxWidth(100);
			c2.setResizable(true);

			// Name column
			TableColumn c3 = _sourceList.getColumnModel().getColumn(3);
			c3.setMinWidth(50);
			c3.setPreferredWidth(100);
			c3.setMaxWidth(200);
			c3.setResizable(true);

			// Source column
			TableColumn c4 = _sourceList.getColumnModel().getColumn(4);
			c4.setMinWidth(200);
			c4.setPreferredWidth(600);
			c4.setMaxWidth(2000);
			c4.setResizable(true);

			JScrollPane sourceListSP = new JScrollPane(_sourceList);
			sourceListSP
					.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			sourceListSP
					.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			sourceListSP.setBackground(TabManager.BGCOLOR);
			add(sourceListSP);
		} catch (Exception e) {
			System.out.println(e.toString());
		}

	}

	/**
	 * Initialize the top panel that contains controls for the table.
	 */
	private void initDoc() {

		String header = 
			StaticResources.getDisplayString(
					"preferences.description1",
					"The Component Library is built using KAR files found"
					+ " in the following local directories.  Adding or removing local directories will rebuild"
					+ " the component library.")
			+ "\n\n"
			+ StaticResources.getDisplayString(
					"preferences.description2",					
					"By selecting the search box next to remote"
					+ " repositories, components from the remote repositories will be included"
					+ " when searching components.")
			+ "\n\n"		
			+ StaticResources.getDisplayString(
					"preferences.description3",
					"By selecting the save box next to a local repository, KAR files will be saved"
					+ " to that directory by default.")
			+ "\n\n"		
			+ StaticResources.getDisplayString(
					"preferences.description4",
					"By selecting the save box next to a remote repository, you will be asked "
					+ " if you want to upload the KAR to that repository when it is saved.");

		JTextArea headerTextArea = new JTextArea(header);
		headerTextArea.setEditable(false);
		headerTextArea.setLineWrap(true);
		headerTextArea.setWrapStyleWord(true);
		headerTextArea.setPreferredSize(new Dimension(300, 400));
		headerTextArea.setBackground(TabManager.BGCOLOR);
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(TabManager.BGCOLOR);
		headerPanel.add(headerTextArea, BorderLayout.CENTER);
		JScrollPane headerPane = new JScrollPane(headerPanel);
		headerPane.setPreferredSize(new Dimension(300, 150));

		add(headerPane);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kepler.gui.PreferencesTab#setParent(ptolemy.actor.gui.TableauFrame)
	 */
	public void setParent(TableauFrame frame) {
		_frame = frame;
	}

	/**
	 * Called when the preference tab is closed.
	 * 
	 * @see org.kepler.gui.PreferencesTab#onClose()
	 */
	public void onClose() {
		if (isDebugging) {
			log.debug("onClose()");
		}

		// If the local directories have changed, rebuild the
		// library.
		if (_localRepoManager.changedSinceCheckpoint()) {
			_localRepoManager.synchronizeDB();
			_buildLibrary.doClick();
		}
		
		try {
			// look up the tab by the tab class
			TabPane tp = 
				TabManager.getInstance().getTab(_frame, ComponentLibraryTab.class);
			if (tp instanceof ComponentLibraryTab) {
				((ComponentLibraryTab)tp).reinitializeTab();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Called when the user cancels
	 * 
	 * @see org.kepler.gui.PreferencesTab#onCancel()
	 */
	public void onCancel() {
		if (_localRepoManager.changedSinceCheckpoint()) {
			_localRepoManager.restoreCheckpoint();
		}
	}

	/**
	 * Rebuild the library.
	 */
	public void rebuildLibrary() {

		try {
			
			KARCacheManager kcm = KARCacheManager.getInstance();
			kcm.clearKARCache();
			
			LibraryManager lm = LibraryManager.getInstance();
			LibIndex index = lm.getIndex();
			index.clear();
			lm.buildLibrary();
			lm.refreshJTrees();
			_localRepoManager.setCheckpoint();

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public class ComponentSourceTableModel extends AbstractTableModel {

		private String[] columnNames = 
			{ 
				StaticResources.getDisplayString("preferences.search", "Search"), 
				StaticResources.getDisplayString("preferences.save", "Save"), 
				StaticResources.getDisplayString("preferences.type", "Type"), 
				StaticResources.getDisplayString("preferences.name", "Name"),
				StaticResources.getDisplayString("preferences.source", "Source") 
				};

		private Vector<Boolean> searchSources;
		private Vector<Boolean> saveSources;
		private Vector<String> sourceTypes;
		private Vector<String> sourceNames;
		private Vector<String> sourceValues;

		public ComponentSourceTableModel() {
			refreshData();
		}

		public void refreshData() {
			searchSources = new Vector<Boolean>();
			saveSources = new Vector<Boolean>();
			sourceTypes = new Vector<String>();
			sourceNames = new Vector<String>();
			sourceValues = new Vector<String>();

			/** Local Repositories **/
			final File saveDir = _localRepoManager.getSaveRepository();
			for (LocalRepositoryManager.LocalRepository localRepo : _localRepoManager.getLocalRepositories()
					.keySet()) {
				searchSources.add(new Boolean(true));
				if (localRepo.isFileRepoDirectory(saveDir)) {
					saveSources.add(new Boolean(true));
				} else {
					saveSources.add(new Boolean(false));
				}
				sourceTypes.add("local");
				sourceNames.add(_localRepoManager.getLocalRepositories().get(
						localRepo));
				sourceValues.add(localRepo.getDefaultDirectory().toString());
			}

			/** Remote Repositories **/
			for (Repository r : _repositoryManager.getRepositories()) {
				searchSources.add(new Boolean(r.includeInSearch()));
				sourceTypes.add("remote");
				sourceNames.add(r.getName());
				sourceValues.add(r.getRepository());

				// set the save repository
				Repository saveRepo = _repositoryManager.getSaveRepository();
				if (r == saveRepo) {
					saveSources.add(new Boolean(true));
				} else {
					saveSources.add(new Boolean(false));
				}
			}

			fireTableDataChanged();
		}

		public int getColumnCount() {
			return 5;
		}

		public String getColumnName(int col) {
			return columnNames[col];
		}

		public int getRowCount() {
			return searchSources.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return searchSources.elementAt(rowIndex);
			case 1:
				return saveSources.elementAt(rowIndex);
			case 2:
				return sourceTypes.elementAt(rowIndex);
			case 3:
				return sourceNames.elementAt(rowIndex);
			case 4:
				return sourceValues.elementAt(rowIndex);
			default:
				return null;
			}
		}

		public Class getColumnClass(int col) {
			switch (col) {
			case 0:
				return Boolean.class;
			case 1:
				return Boolean.class;
			case 2:
				return String.class;
			case 3:
				return String.class;
			case 4:
				return String.class;
			default:
				return null;
			}
		}

		/*
		 * Don't need to implement this method unless your table's editable.
		 */
		public boolean isCellEditable(int row, int col) {
			if (col == 0) {
				if (sourceTypes.elementAt(row).equals("remote")) {
					return true;
				}
			}
			if (col == 1) {
				if (sourceTypes.elementAt(row).equals("local")
						|| sourceTypes.elementAt(row).equals("remote")) {
					return true;
				}
			}
			if (col == 3) {
				if (sourceTypes.elementAt(row).equals("local")) {
					return true;
				}
			}
			return false;
		}

		public void setValueAt(Object value, int row, int col) {
			if (col == 0) {
				if (sourceTypes.elementAt(row).equals("remote")) {
					if (value instanceof Boolean) {
						Repository r = _repositoryManager
								.getRepository(sourceNames.elementAt(row));
						r.setIncludeInSearch(((Boolean) value).booleanValue());
						searchSources.setElementAt((Boolean) value, row);
						fireTableCellUpdated(row, col);
					}
				}
			}
			if (col == 1) {
				if (sourceTypes.elementAt(row).equals("local")) {
					if (value instanceof Boolean) {
						for (int i = 0; i < saveSources.size(); i++) {
							if (sourceTypes.elementAt(i).equals("local")) {
								if (i == row) {
									saveSources.setElementAt(new Boolean(true),
											i);
									LocalRepositoryManager.getInstance()
											.setLocalSaveRepo(
													new File(sourceValues
															.elementAt(row)));
									fireTableCellUpdated(i, col);
								} else {
									saveSources.setElementAt(
											new Boolean(false), i);
									fireTableCellUpdated(i, col);
								}
							}
						}
					}
				} else if (sourceTypes.elementAt(row).equals("remote")) {
					if (value instanceof Boolean) {
						boolean checked = ((Boolean) value).booleanValue();
						for (int i = 0; i < saveSources.size(); i++) {
							if (sourceTypes.elementAt(i).equals("remote")) {
								if (checked) {
									if (i == row) {
										// tell the repositoryManager to save to
										// this
										// repository
										_repositoryManager
												.setSaveRepository(_repositoryManager
														.getRepository(sourceNames
																.elementAt(row)));
										saveSources.setElementAt(new Boolean(
												true), i);
										fireTableCellUpdated(i, col);
									} else {
										saveSources.setElementAt(new Boolean(
												false), i);
										fireTableCellUpdated(i, col);
									}
								} else {
									// Set no save repository
									saveSources.setElementAt(
											new Boolean(false), i);
									if (i == row) {
										_repositoryManager
												.setSaveRepository(null);
									}
									fireTableCellUpdated(i, col);
								}
							}
						}
					}
				}
			}
			if (col == 3) {
				if (sourceTypes.elementAt(row).equals("local")) {
					if (isDebugging)
						log.debug(value.getClass().getName());
					if (value instanceof String) {
						try {
							LocalRepositoryManager.getInstance()
									.setLocalRepoName(
											new File(sourceValues
													.elementAt(row)),
											(String) value);
							sourceNames.setElementAt((String) value, row);
						} catch (Exception e) {
							JOptionPane.showMessageDialog(GUIUtil
									.getParentWindow(_sourceList), e
									.getMessage());
							fireTableCellUpdated(row, col);
						}
					}
				}
			}
		}

	}

	/**
	 * A listener for the Add source button.
	 * 
	 * @author Aaron Schultz
	 */
	private class AddSourceListener implements ActionListener {

		/** Action for changing the view. */
		public void actionPerformed(ActionEvent e) {

			Object c = e.getSource();
			if (c instanceof JButton) {

				JButton jc = (JButton) c;

				File saveRepo = _localRepoManager.getSaveRepository();
                                // Avoid white boxes in file chooser, see
                                // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3801
                                JFileChooserBugFix jFileChooserBugFix = new JFileChooserBugFix();
                                Color background = null;
                                PtFileChooser chooser = null;
                                try {
                                    background = jFileChooserBugFix.saveBackground();
                                    chooser = new PtFileChooser(_frame, "Add Repository", JFileChooser.OPEN_DIALOG);
                                    chooser.setCurrentDirectory(saveRepo);
                                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
                                    int returnVal = chooser.showDialog(_frame, "Choose");

                                    if (returnVal == JFileChooser.APPROVE_OPTION) {

					File selectedFile = chooser.getSelectedFile();
					try {
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						_localRepoManager.addLocalRepoRootDir(selectedFile);

						TableModel tm = _sourceList.getModel();
						ComponentSourceTableModel cstm = (ComponentSourceTableModel) tm;
						cstm.refreshData();

					} catch (Exception e1) {
						Window parentWindow = GUIUtil.getParentWindow(jc);
						JOptionPane.showMessageDialog(parentWindow, e1.getMessage());
					} finally {
						setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
                                    } else {
					if (isDebugging)
                                            log.debug("Cancel Add source operation");
                                    }
                                } finally {
                                    jFileChooserBugFix.restoreBackground(background);
                                }
			}
		}

	}

	/**
	 * A listener for the set default sources button.
	 * 
	 * @author Aaron Schultz
	 * 
	 */
	private class DefaultSourcesListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			Object c = e.getSource();
			if (c instanceof JButton) {
				JButton jc = (JButton) c;
				int choice = JOptionPane.showConfirmDialog(GUIUtil
						.getParentWindow(jc), "Reset to default directories?");
				if (choice == JOptionPane.YES_OPTION) {
					_localRepoManager.setDefaultLocalRepos();
					_localRepoManager.setDefaultSaveRepo();
					_repositoryManager.setSearchNone();
					((ComponentSourceTableModel) _sourceList.getModel())
							.refreshData();
				}

			}
		}
	}

	/**
	 * A listener for the set default sources button.
	 * 
	 * @author Aaron Schultz
	 * 
	 */
	private class BuildLibraryListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			Object c = e.getSource();
			if (c instanceof JButton) {
				rebuildLibrary();
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	/**
	 * A listener for the Add source button.
	 * 
	 * @author Aaron Schultz
	 */
	private class RemoveSourceListener implements ActionListener {

		/** Action for changing the view. */
		public void actionPerformed(ActionEvent e) {

			Object c = e.getSource();
			if (c instanceof JButton) {
				JButton jc = (JButton) c;
				int currentRow = _sourceList.getSelectedRow();

				if (currentRow < 0) {
					Window parentWindow = GUIUtil.getParentWindow(jc);
					JOptionPane.showMessageDialog(parentWindow,
							"Please highlight a source for removal.");
					return;
				}

				TableModel tm = _sourceList.getModel();
				ComponentSourceTableModel cstm = (ComponentSourceTableModel) tm;

				String sourceType = (String) cstm.getValueAt(currentRow, 2);
				String removeDir = (String) cstm.getValueAt(currentRow, 4);
				if (sourceType.equals("remote")) {
					JOptionPane.showMessageDialog(GUIUtil.getParentWindow(jc),
							"Cannot remove remote sources: \n" + removeDir
									+ "\n is a remote source.");
					return;
				}
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					File removeDirFile = new File(removeDir);
					_localRepoManager.removeLocalRepoRootDir(removeDirFile);
					cstm.refreshData();
				} catch (Exception e2) {
					JOptionPane.showMessageDialog(GUIUtil.getParentWindow(jc),
							"Unable to remove repository root directory.\n"
									+ e2.getMessage());
				} finally {
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		}

	}

	/**
	 * A factory that creates the ServicesListModification panel for the
	 * PreferencesFrame.
	 * 
	 *@author Aaron Schultz
	 */
	public static class Factory extends PreferencesTabFactory {
		/**
		 * Create a factory with the given name and container.
		 * 
		 *@param container
		 *            The container.
		 *@param name
		 *            The name of the entity.
		 *@exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 *@exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a PreferencesTab that displays the selected Ecogrid Services.
		 * 
		 * @return A new LibraryPaneTab that displays the library
		 */
		public PreferencesTab createPreferencesTab() {
			ComponentLibraryPreferencesTab clpt = new ComponentLibraryPreferencesTab();
			clpt.setTabName(this.getName());
			return clpt;
		}
	}

}