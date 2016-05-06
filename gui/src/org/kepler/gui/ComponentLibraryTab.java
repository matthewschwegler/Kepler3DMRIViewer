/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:22:25 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31122 $'
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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URL;
import java.util.Iterator;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.authentication.AuthenticationException;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.gui.popups.LibraryPopupListener;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.objectmanager.repository.RepositoryException;
import org.kepler.sms.OntologyCatalog;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.moml.EntityLibrary;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.tree.EntityTreeModel;

public class ComponentLibraryTab extends JPanel implements TabPane {
	private static final Log log = LogFactory.getLog(ComponentLibraryTab.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private String _name;
	protected EntityTreeModel _libraryModel;
	// protected LibraryManager _lman = LibraryManager.getInstance();
	/**
	 * The configuration for this tableau.
	 */
	private TableauFrame _frame;

	protected AnnotatedPTree _library;
	protected ResultTreeRebuilder _libraryPanel;
	protected SimpleLibrarySearcher _librarySearcher; // SimpleLibrarySearcher
	private SimpleSearchUIPane _librarySearchPane; // SimpleSearchUIPane
	private SearchButtonActionHandler _searchButtonHandler = new SearchButtonActionHandler();

	private JComboBox _ontSelectBox;
	private OntSelectActionHandler _ontSelectHandler = new OntSelectActionHandler();

	private ImageIcon _ontOpenIcon;
	private ImageIcon _ontClosedIcon;
	private ImageIcon _folOpenIcon;
	private ImageIcon _folClosedIcon;

	private MouseListener _mouseListener;
	private LibraryActionHandler _libraryActionHandler;

	// XXX currently always doing authenticated queries and gets.
	// The main ramification here is this means the user will be
	// prompted on their first search to authenticate. Previously
	// we only ever did non-authenticated search on Components tab.
	public static final boolean AUTHENTICATE = true;

	public final String allOntologiesName = "All Ontologies and Folders";

	private LocalRepositoryManager _localRepoManager;

	public ComponentLibraryTab(TableauFrame parent) {
		super();
		this.setBackground(TabManager.BGCOLOR);
		_frame = parent;

		URL ontOpenURL = ComponentLibraryTab.class.getResource(StaticResources
				.getSettingsString("ONTOLOGY_TREEICON_OPEN_PATH", ""));
		if (ontOpenURL != null) {
			_ontOpenIcon = new ImageIcon(ontOpenURL);
		}

		URL ontClosedURL = ComponentLibraryTab.class
				.getResource(StaticResources.getSettingsString(
						"ONTOLOGY_TREEICON_CLOSED_PATH", ""));
		if (ontClosedURL != null) {
			_ontClosedIcon = new ImageIcon(ontClosedURL);
		}

		URL folOpenURL = ComponentLibraryTab.class.getResource(StaticResources
				.getSettingsString("FOLDER_TREEICON_OPEN_PATH", ""));
		if (folOpenURL != null) {
			_folOpenIcon = new ImageIcon(folOpenURL);
		}

		URL folClosedURL = ComponentLibraryTab.class
				.getResource(StaticResources.getSettingsString(
						"FOLDER_TREEICON_CLOSED_PATH", ""));
		if (folClosedURL != null) {
			_folClosedIcon = new ImageIcon(folClosedURL);
		}

		_localRepoManager = LocalRepositoryManager.getInstance();

	}

	/**
	 * Implementation of TabPane getName()
	 */
	public String getTabName() {
		return _name;
	}

	public void setTabName(String name) {
		_name = name;
	}

	/**
	 * Implementation of TabPane setParentFrame(TableauFrame)
	 */
	public void setParentFrame(TableauFrame parent) {
		if (parent == null) {
			//System.out.println("setParentFrame(null)");
			closing();
		} else {
			_frame = parent;
		}
	}

	private void closing() {
		// System.out.println("ComponentLibraryTab.closing()");
		//_mouseListener.closing();
		_library.removeMouseListener(_mouseListener);
		_mouseListener = null;
		//_library.removeListener(_libraryActionHandler);
		_libraryActionHandler = null;
		if (_librarySearchPane instanceof SimpleSearchUIPane) {
			SimpleSearchUIPane pane = (SimpleSearchUIPane) _librarySearchPane;
			pane.closing();
		}
		_frame = null;

	}

	/**
	 * Implementation of getParentFrame getName()
	 */
	public TableauFrame getParentFrame() {
		return _frame;
	}

	/**
	 * Set true if want not to display remote ontology
	 */
	public void setSkipReposiotryOntology(boolean skipRepositoryOntology) {
		_librarySearcher.setSkipReposiotryOntology(skipRepositoryOntology);
	}

	/**
	 * Implementation of TabPane getName()
	 */
	public void initializeTab() {

		initLibraryTree();
		this.setBackground(TabManager.BGCOLOR);

		/*
		 * Getting rid of pluggable Areas. Can get similar results by extending
		 * this class and using the TabPane extension point.
		 */
		initLibrarySearchPane();
		initLibrarySearcher();
		initOntSelectBox();
		initLibraryPanel();

		JPanel kludgePane = new JPanel();
		kludgePane.setLayout(new BorderLayout());

		// create and add the search panel to the library
		this.setLayout(new BorderLayout());
		this.add(_librarySearchPane, BorderLayout.NORTH);
		kludgePane.add(_ontSelectBox, BorderLayout.NORTH);
		kludgePane.add(_libraryPanel, BorderLayout.CENTER);
		this.add(kludgePane, BorderLayout.CENTER);

	}

	/*
	 * Init the library tree for library panel.
	 */
	protected void initLibraryTree() {
		// get the tree model from the LibraryIndex
		LibraryManager libraryManager = LibraryManager.getInstance();

		_libraryModel = libraryManager.getTreeModel(getFilterFile());
		_library = new AnnotatedPTree(_libraryModel, this);

		_mouseListener = new LibraryPopupListener(_library);
		_library.setMouseListener(_mouseListener);

		_library.initAnotatedPTree();

		libraryManager.addLibraryJTree((JTree) _library);

		_libraryActionHandler = new LibraryActionHandler();
		_library.addListener(_libraryActionHandler);
		_library.setRootVisible(false);
	}

	/**
	 * Set the selection of the JTree (result) in this panel to single
	 * selection, and add a selection listener to it.
	 * 
	 * @param listener
	 */
	public void addTreeSingleSelectionListener(TreeSelectionListener listener) {
		if (_library != null) {
			_library.getSelectionModel().setSelectionMode(
					TreeSelectionModel.SINGLE_TREE_SELECTION);
			_library.addTreeSelectionListener(listener);
		}
		if (_libraryPanel != null) {
			_libraryPanel.setSingleTreeSelectionListener(listener);
		}
	}

	/**
	 * Get the result tree after searching.
	 * 
	 * @return null if _libraryPanel is null
	 */
	public JTree getResultTree() {
		if (_libraryPanel != null) {
			JTree tree = _libraryPanel.getResultTree();
			if (tree != null) {
				return tree;
			} else {
				return _library;
			}
		} else if (_library != null) {
			return _library;
		} else {
			return null;
		}
	}

	protected File getFilterFile() {
		TableauFrame tableauFrame = this._frame;
		Attribute moduleAttribute = tableauFrame.getTableau().getAttribute(
				"_filterModule");
		Attribute relativePathAttribute = tableauFrame.getTableau()
				.getAttribute("_filterRelativePath");
		if (moduleAttribute == null || relativePathAttribute == null) {
			return null;
		} else {
			try {
				String moduleName = ((StringAttribute) moduleAttribute)
						.getExpression();
				String relativePath = ((StringAttribute) relativePathAttribute)
						.getExpression();
				// NOTE: Should this be getModuleByStemName? Be sure to test on
				// installation. Won't matter at the moment because it's only
				// used in sensor-view module.
				Module module = ModuleTree.instance().getModule(moduleName);
				File filterFile = new File(module.getDir(), relativePath);
				if (filterFile.exists()) {
					return filterFile;
				} else {
					return null;
				}
			} catch (ClassCastException ex) {
				System.err
						.println("Unable to cast attributes _filterModule and _filterRelativePath to StringAttribute");
				return null;
			}
		}
	}

	public void reinitializeTab() {
		this.removeAll();
		this.initializeTab();
		this.updateUI();
		this.repaint();
	}

	/*
	 * org.ecoinformatics.seek.ecogrid.SimpleLibrarySearchGUIPane
	 * org.kepler.gui.SimpleSearchUIPane
	 */
	protected void initLibrarySearchPane() {

		_librarySearchPane = new SimpleSearchUIPane(_searchButtonHandler);
		if (_librarySearchPane == null) {
			System.out.println("error: _searchPane is null.  " + "This "
					+ "problem can be fixed by adding a librarySearchGUIPane "
					+ "property in the configuration.xml file.");
		}

	}

	/*
	 * org.ecoinformatics.seek.ecogrid.SimpleLibrarySearcher
	 * org.kepler.gui.SimpleChoiceLibrarySearcher
	 * org.kepler.gui.SimpleLibrarySearcher Other possible search techniques can
	 * be substituted here. this would include the ontological lookup scheme
	 * that requires sparrow support
	 */
	protected void initLibrarySearcher() {

		_librarySearcher = new SimpleLibrarySearcher(_library,
				_librarySearchPane);
		if (_librarySearcher == null) {
			System.out
					.println("error: librarySearcher is null.  This "
							+ "problem can be fixed by adding a librarySearcherFactory "
							+ "property in the configuration.xml file.");
		}

	}

	protected void initOntSelectBox() {

		OntologyCatalog oc = OntologyCatalog.instance();
		_ontSelectBox = new JComboBox();
		_ontSelectBox.addItem(allOntologiesName);
		Iterator<String> onts = oc.getLibraryOntologyNames();
		while (onts.hasNext()) {
			_ontSelectBox.addItem(onts.next());
		}
		_ontSelectBox.addActionListener(_ontSelectHandler);
		_ontSelectBox.setRenderer(new IconListRenderer());

		for (String l : _localRepoManager.getLocalRepositories().values()) {
			_ontSelectBox.addItem(l);
		}

		this.add(_ontSelectBox, BorderLayout.NORTH);

	}

	/*
	 * org.kepler.gui.ResultTreeRebuilder
	 * org.ecoinformatics.seek.ecogrid.ResultHighlighter
	 * org.ecoinformatics.seek.ecogrid.ResultsLister
	 */
	protected void initLibraryPanel() {

		try {
			_libraryPanel = new ResultTreeRebuilder(_library, null);
			if (_libraryPanel == null) {
				System.out.println("error: _libraryPanel is null.  ");
			}
		} catch (IllegalActionException iae) {
			System.out.println("Error displaying search results: " + iae);
		}

	}

	public JTree getJTree() {
		return _library;
	}

	public class IconListRenderer extends DefaultListCellRenderer {

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			JLabel label = (JLabel) super.getListCellRendererComponent(list,
					value, index, isSelected, cellHasFocus);

			if (label.getText().equals(allOntologiesName)) {
				// No Icon
			} else if (_localRepoManager.isLocalRepositoryName(label.getText())) {
				label.setIcon(_folClosedIcon);
			} else {
				label.setIcon(_ontClosedIcon);
			}
			return label;

		}
	}

	/**
	 * class to handle the search button
	 * 
	 * @author berkley
	 * @since February 17, 2005
	 */
	public class SearchButtonActionHandler implements ActionListener {
		/**
		 * Description of the Method
		 * 
		 * @param e
		 *            Description of the Parameter
		 */
		public void actionPerformed(ActionEvent e) {
			
			if (isDebugging) {
				log.debug("SearchButtonActionHandler.actionPerformed(ActionEvent)");
			}
			
			Component component = null;
			
			try {
				/*
				 * Wait cursor is not set when hitting enter in text field.
				 * http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5581
				 * 
				 * However this doesn't help:
				 * 
				if (e.getSource() != null){
					component = ((Component)e.getSource()).getParent();
					while (component != null){
						component = component.getParent();
						if (component instanceof ComponentLibraryTab){
							component.requestFocus();
							component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							break;
						}
					}
				}
				*/
				
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				// need to disable the action listener here so update() isn't
				// called twice
				_ontSelectBox.removeActionListener(_ontSelectHandler);
				_ontSelectBox.setSelectedItem(allOntologiesName);
				_ontSelectBox.addActionListener(_ontSelectHandler);

				String searchTerm = _librarySearchPane.getSearchTerm();
				if (isDebugging)
					log.debug("Search term is '" + searchTerm + "'");
				Long totalSearchTime = System.currentTimeMillis();
				Long searchTime = System.currentTimeMillis();
				LibrarySearchResults searchResults = _librarySearcher.search(
						searchTerm, AUTHENTICATE);
				if (isDebugging)
					log.debug("Library search completed in "
							+ (System.currentTimeMillis() - searchTime) + "ms");
				searchTime = System.currentTimeMillis();
				_libraryPanel.update(searchResults);
				if (isDebugging)
					log.debug("Library updated in "
							+ (System.currentTimeMillis() - searchTime) + "ms");
				if (isDebugging)
					log.debug("Total search time was "
							+ (System.currentTimeMillis() - totalSearchTime)
							+ "ms");

			} catch (RepositoryException re) {
				re.printStackTrace();
				MessageHandler
						.error("There was a problem searching the Repository. Please check your network connection.\n",
								re);
			} catch (AuthenticationException ae) {
				if (ae.getType() != AuthenticationException.USER_CANCEL) {
					MessageHandler
							.error("Authentication error. Please ensure your username and password were spelled correctly,\nand verify your network connection\n",
									ae);
					ae.printStackTrace();
				}
			} catch (Exception exception) {
				exception.printStackTrace();
				MessageHandler.error("Error.\n", exception);
			} finally {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				/*
				 * Wait cursor is not set when hitting enter in text field.
				 * http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5581
				 * 
				 * However this doesn't help:
				 * 
				if (component != null){
					component.requestFocus();
					component.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
				*/
			}
		}

	}

	/**
	 * Listener used to changes from the NewFolderFrame
	 */
	public class LibraryActionHandler implements ActionListener {
		/**
		 * Description of the Method
		 * 
		 * @param event
		 *            Description of Parameter
		 */
		public void actionPerformed(ActionEvent event) {
			if (isDebugging) {
				log.debug("LibraryActionHandler.actionPerformed(ActionEvent)");
			}
			String command = event.getActionCommand();
			if (command.equals("new_folder_created")
					|| command.equals("new_actor_created")) {
				// _library.clearSelection(); //clear the current selections
				// LibrarySearchResultPane.collapseAll(_library);
				/*
				 * TreeAction action = (TreeAction)event.getSource(); TreePath
				 * path = action.getPath(); _library.addSelectionPath(path);
				 * System.out.println("path expanded.");
				 * //LibrarySearchResultPane.expandAll(_library);
				 * _library.expandPath(path);
				 */
			}
		}
	}

	/**
	 * Inner class to handle changes to the Ontology Selection box.
	 * 
	 * @author Aaron Schultz
	 */
	public class OntSelectActionHandler implements ActionListener {
		private boolean _actionDisabled = false;

		public void disableAction(boolean actionDisabled) {
			_actionDisabled = actionDisabled;
		}

		public void actionPerformed(ActionEvent e) {
			if (!_actionDisabled) {

				if (e.getSource() == _ontSelectBox) {

					String selectedOntology = (String) _ontSelectBox
							.getSelectedItem();

					try {

						if (selectedOntology.equals(allOntologiesName)) {
							_libraryPanel.update(new LibrarySearchResults());
						} else {
							EntityLibrary selOntLibrary = null;
							int cnt = _libraryModel.getChildCount(_libraryModel
									.getRoot());
							for (int i = 0; i < cnt; i++) {
								NamedObj child = (NamedObj) _libraryModel
										.getChild(_libraryModel.getRoot(), i);
								if (child.getName().equals(selectedOntology)) {
									if (child instanceof EntityLibrary) {
										selOntLibrary = (EntityLibrary) child;
									}
									break;
								}
							}
							if (selOntLibrary == null) {
								String message = "No Components found in "
										+ selectedOntology;
								JOptionPane.showMessageDialog(getParentFrame(),
										message);
								_libraryPanel
										.update(new LibrarySearchResults());
								_ontSelectBox
										.setSelectedItem(allOntologiesName);
							} else {
								((ResultTreeRebuilder) _libraryPanel)
										.update(selOntLibrary);
								_librarySearchPane.clearSearch();
							}
						}
					} catch (IllegalActionException iae) {
						// FIXME: handle this exception correctly
						System.out.println("Error searching: "
								+ iae.getMessage());
						// iae.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * A factory that creates the library panel for the editors.
	 * 
	 * @author Aaron Schultz
	 */
	public static class Factory extends TabPaneFactory {
		/**
		 * Create a factory with the given name and container.
		 * 
		 * @param container
		 *            The container.
		 * @param name
		 *            The name of the entity.
		 * @exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 * @exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a library pane that displays the given library of actors.
		 * 
		 * @return A new LibraryPaneTab that displays the library
		 */
		public TabPane createTabPane(TableauFrame parent) {
			ComponentLibraryTab clt = new ComponentLibraryTab(parent);
			clt.setTabName(this.getName());
			return clt;
		}
	}
}
