/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
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

package org.kepler.sms.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.kepler.sms.NamedOntClass;
import org.kepler.sms.NamedOntModel;
import org.kepler.sms.NamedOntProperty;
import org.kepler.sms.OntologyCatalog;
import org.kepler.util.StaticResources;

/**
 * This class implements a simple panel for selecting classes from a tree widget
 * and adding them to a target list. The panel uses drag and drop events,
 * buttons, and right-click menu items for adding and removing classes.
 * Right-click events also are used to navigate properties. This panel also
 * provides a simple term search mechanism.
 * 
 * @author Shawn Bowers
 */
public class OntoClassSelectionJPanel extends JPanel {

	//private JPopupMenu popup;
	private OntoClassSelectionJTree _ontoTree;
	private OntoClassSelectionJList _classList;
	private JTextField _searchTxt;
	private JTextArea _commentTxt;
	private JButton _searchBtn;
	private JButton _addBtn;
	private JButton _removeBtn;
	
	/**
	 * Default constructor that initializes the panel, accepting all ontologies
	 * and having a default width and height.
	 */
	public OntoClassSelectionJPanel() {
		super();
		init(false, 525, 350);
	}

	/**
	 * Initializes the panel with default width and height.
	 * 
	 * @param libraryOnly
	 *            if true, only loads the library indexed ontologies
	 */
	public OntoClassSelectionJPanel(boolean libraryOnly) {
		super();
		init(libraryOnly, 525, 350);
	}

	/**
	 * Initializes the panel accepting all ontologies.
	 * 
	 * @param width
	 *            the width of the component
	 * @param height
	 *            the height of the component
	 */
	public OntoClassSelectionJPanel(int width, int height) {
		super();
		init(false, width, height);
	}

	/**
	 * Initializes the panel with the appropriate ontologies, width, and height
	 * 
	 * @param libraryOnly
	 *            if true, only loads the library indexed ontologies
	 * @param width
	 *            the width of the component
	 * @param height
	 *            the height of the component
	 */
	public OntoClassSelectionJPanel(boolean libraryOnly, int width, int height) {
		super();
		init(libraryOnly, width, height);
	}

	/**
	 * Provides access to the selected ontology classes
	 * 
	 * @return the current set of selections in the panel
	 */
	public Vector<NamedOntClass> getNamedOntClasses() {
		return getListAsVector();
	}

	/**
	 * adds the given NamedOntClass objects to the panel.
	 * 
	 * @param ontClass
	 *            the ontology class to add
	 */
	public void addNamedOntClass(NamedOntClass ontClass) {
		if (!getNamedOntClasses().contains(ontClass))
			addToList(ontClass);
	}

	/**
	 * Remove all selected classes.
	 */
	public void clear() {
		clearList();
	}

	// PRIVATE METHODS

	/**
	 * Private method for initializing the panel
	 */
	private void init(boolean libraryOnly, int length, int height) {

		_searchTxt = new JTextField(14);
		_searchBtn = new JButton("Search");
		_addBtn = new JButton(">>");
		_removeBtn = new JButton("<<");
		
		JScrollPane treeView = createTreeView(libraryOnly);
		_ontoTree.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

		JPanel listView = createListView();
		_classList.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

		// set up search button
		_searchBtn.addActionListener(new ClassSearchButtonListener());
		_searchTxt.addActionListener(new ClassSearchButtonListener());

		// initialize the buttons
		_addBtn.addActionListener(new AddButtonListener());
		_addBtn.setEnabled(false);
		_removeBtn.addActionListener(new RemoveButtonListener());
		_removeBtn.setEnabled(false);

		// the description text area
		_commentTxt = new JTextArea();
		_commentTxt.setEditable(false);
		_commentTxt.setLineWrap(true);
		_commentTxt.setWrapStyleWord(true);
		_commentTxt.setEnabled(false);
		JScrollPane commentView = new JScrollPane(_commentTxt,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		commentView.setMaximumSize(new Dimension(length, 150));
		commentView.setPreferredSize(new Dimension(length, 150));

		// onto tree label
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
		panel1.add(new JLabel("All Categories:", SwingConstants.LEFT));
		panel1.add(Box.createHorizontalGlue());

		// search
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
		panel2.add(_searchTxt);
		panel2.add(Box.createRigidArea(new Dimension(5, 0)));
		panel2.add(_searchBtn);

		// onto tree
		JPanel panel3 = new JPanel();
		panel3.setLayout(new BoxLayout(panel3, BoxLayout.Y_AXIS));
		panel3.add(panel1);
		panel3.add(Box.createRigidArea(new Dimension(0, 2)));
		panel3.add(treeView);
		panel3.add(Box.createRigidArea(new Dimension(0, 5)));
		panel3.add(panel2);

		// add and remove button panel
		JPanel panel4 = new JPanel();
		panel4.setLayout(new BoxLayout(panel4, BoxLayout.Y_AXIS));
		panel4.add(_addBtn);
		panel4.add(Box.createRigidArea(new Dimension(0, 5)));
		panel4.add(_removeBtn);

		// class list label
		JPanel panel5 = new JPanel();
		panel5.setLayout(new BoxLayout(panel5, BoxLayout.X_AXIS));
		panel5.add(new JLabel("Selected Categories:", SwingConstants.LEFT));
		panel5.add(Box.createHorizontalGlue());

		// class list panel
		JPanel panel6 = new JPanel();
		panel6.setLayout(new BoxLayout(panel6, BoxLayout.Y_AXIS));
		panel6.add(panel5);
		panel6.add(Box.createRigidArea(new Dimension(0, 2)));
		panel6.add(listView);

		// top portion
		JPanel panel7 = new JPanel();
		panel7.setLayout(new BoxLayout(panel7, BoxLayout.X_AXIS));
		panel7.add(panel3);
		panel7.add(Box.createRigidArea(new Dimension(5, 0)));
		panel7.add(panel4);
		panel7.add(Box.createRigidArea(new Dimension(5, 0)));
		panel7.add(panel6);

		// comment/description label
		JPanel panel8 = new JPanel();
		panel8.setLayout(new BoxLayout(panel8, BoxLayout.X_AXIS));
		panel8.add(new JLabel("Category Description:", SwingConstants.LEFT));
		panel8.add(Box.createHorizontalGlue());

		// top-level panel
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(panel7);
		panel.add(Box.createRigidArea(new Dimension(0, 7)));
		panel.add(panel8);
		panel.add(Box.createRigidArea(new Dimension(0, 2)));
		panel.add(commentView);

		panel.setMaximumSize(new Dimension(length, height));
		panel.setPreferredSize(new Dimension(length, height));

		add(panel);
	}

	/**
	 * Private method that initiliazes and creates the tree view sub-panel
	 * 
	 * @param libraryOnly
	 *            true if only the library ontologies are selected
	 * @return a scroll pane containing the ontologies
	 */
	private JScrollPane createTreeView(boolean libraryOnly) {
		// create the default root node
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("");

		// get each ont model for the library
		Iterator ontModels = OntologyCatalog.instance().getNamedOntModels(
				libraryOnly);

		while (ontModels.hasNext()) {
			// add ontologies to root
			NamedOntModel m = (NamedOntModel) ontModels.next();
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(m);
			rootNode.add(childNode);
			// get each root class of the model
			Iterator rootClasses = m.getRootClasses(true);
			while (rootClasses.hasNext()) {
				// build tree from the root
				NamedOntClass root = (NamedOntClass) rootClasses.next();
				buildTree(root, childNode);
			}
		}

		// assign root to tree
		_ontoTree = new OntoClassSelectionJTree(rootNode);
		// configure tree
		_ontoTree.setRootVisible(false);
		_ontoTree.setCellRenderer(new OntoClassSelectionJTreeRenderer());
		_ontoTree.setDragEnabled(false); // if true, causes problems on linux
		_ontoTree
				.addTreeSelectionListener(new OntoClassTreeSelectionListener());
		_ontoTree.setShowsRootHandles(true);

		// wrap tree in scroll pane
		return new JScrollPane(_ontoTree,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	}

	/**
	 * Private method that recursively initializes the tree
	 * 
	 * @param c
	 *            the named ont class parent
	 * @param parentNode
	 *            the parent tree node
	 */
	private void buildTree(NamedOntClass c, DefaultMutableTreeNode parentNode) {
		// add the class to the parent
		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(c);
		parentNode.add(childNode);
		// get subclasses
		Iterator subclasses = c.getNamedSubClasses(true);
		while (subclasses.hasNext()) {
			NamedOntClass subclass = (NamedOntClass) subclasses.next();
			buildTree(subclass, childNode);
		}
	}

	/**
	 * Private method that initiliazes and creates the list sub-panel
	 * 
	 * @return a pane containing the selected class list
	 */
	private JPanel createListView() {
		// initialize the class list
		_classList = new OntoClassSelectionJList(new DefaultListModel());
		_classList.setFixedCellWidth(175);
		_classList.addListSelectionListener(new ClassListSelectionListener());

		// add to drop target
		OntoClassSelectionJListDropTarget t = new OntoClassSelectionJListDropTarget(
				_classList);

		// overall pane
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JScrollPane(_classList,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS));
		return panel;
	}

	/**
	 * Private method to add ont classes to the list
	 * 
	 * @param ontClass
	 *            the NamedOntClass to add to the list
	 */
	private void addToList(NamedOntClass ontClass) {
		DefaultListModel mdl = (DefaultListModel) _classList.getModel();
		if (!mdl.contains(ontClass)) {
			// String txt = ontClass.getOntologyName();
			mdl.addElement(ontClass);
		}
	}

	/**
	 * Private method to clear the list
	 */
	private void clearList() {
		DefaultListModel mdl = (DefaultListModel) _classList.getModel();
		mdl.removeAllElements();
	}

	/**
	 * Private method to return all items in the class list
	 * 
	 * @return a list of the selected NamedOntClasses
	 */
	private Vector<NamedOntClass> getListAsVector() {
		DefaultListModel mdl = (DefaultListModel) _classList.getModel();
		Vector<NamedOntClass> result = new Vector<NamedOntClass>();
		for (int i = 0; i < mdl.getSize(); i++)
			result.add((NamedOntClass)mdl.getElementAt(i));
		return result;
	}

	/**
	 * Private method to return all items in the class list
	 * 
	 * @param ontClass
	 *            the NamedOntClass to remove from the list
	 */
	private void removeFromList(NamedOntClass ontClass) {
		DefaultListModel mdl = (DefaultListModel) _classList.getModel();
		mdl.removeElement(ontClass);
	}

	/**
	 * Private method that returns tree nodes containing OntTreeNodes
	 * 
	 * @param str
	 *            search string
	 * @return set of paths in the ontology tree
	 */
	private Vector findMatchingClasses(String str) {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) _ontoTree
				.getModel().getRoot();
		return findMatchingClasses(root, str);
	}

	/**
	 * Private method that returns tree nodes containing OntTreeNodes
	 * 
	 * @param root
	 *            The root of the tree to search
	 * @param str
	 *            The search string
	 */
	private Vector findMatchingClasses(DefaultMutableTreeNode root, String str) {
		Vector result = new Vector();
		Object obj = root.getUserObject();
		if (obj instanceof NamedOntClass) {
			NamedOntClass cls = (NamedOntClass) obj;
			if (approxMatch(cls.getName(), str)) {
				result.add(root);
				return result;
			}
		}
		Enumeration children = root.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children
					.nextElement();
			Iterator ancestors = findMatchingClasses(child, str).iterator();
			while (ancestors.hasNext())
				result.add(ancestors.next());
		}
		return result;
	}

	/**
	 * Private method for checking whether two strings match
	 * 
	 * @param val1
	 *            first string to compare
	 * @param val2
	 *            second string to compare
	 * @return true if strings approximately match
	 */
	private boolean approxMatch(String val1, String val2) {
		val1 = val1.toLowerCase();
		val2 = val2.toLowerCase();
		if (val1.indexOf(val2) != -1 || val2.indexOf(val1) != -1)
			return true;
		return false;
	}

	/**
	 * Private method for collapsing the ontology tree to just the ontology
	 * nodes
	 */
	private void collapseTree() {
		int row = _ontoTree.getRowCount() - 1;
		while (row >= 0) {
			_ontoTree.collapseRow(row);
			row--;
		}
	}

	/**
	 * Private method for executing a search
	 * 
	 * @param searchStr
	 *            the string to search for
	 */
	private void doSearch(String searchStr) {
		// reset the selections
		_ontoTree.clearSelection();
		// collapse the tree
		collapseTree();
		// if empty return
		if (searchStr.trim().equals(""))
			return;
		// get all the matches
		Iterator results = findMatchingClasses(searchStr).iterator();
		while (results.hasNext()) {
			// add selection for each match
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) results
					.next();
			_ontoTree.addSelectionPath(new TreePath(node.getPath()));
		}
		_commentTxt.setText("");
	}

	/**
	 * Private method to select (highlight) a NamedOntClass in the onto tree
	 * 
	 * @param cls
	 *            the NamedOntClass to select
	 */
	private void doSelect(NamedOntClass cls) {
		if (cls == null)
			return;
		// clear current selection
		_ontoTree.clearSelection();
		// collapse the tree
		collapseTree();
		// get the matches
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) _ontoTree
				.getModel().getRoot();
		Iterator paths = findPaths(cls, root).iterator();
		while (paths.hasNext()) {
			TreePath path = (TreePath) paths.next();
			_ontoTree.addSelectionPath(path);
		}
	}

	/**
	 * Private method for finding all paths in onto tree to given NamedOntClass
	 * 
	 * @param cls
	 *            the NamedOntClass to find
	 * @param root
	 *            the root of the ontology tree
	 * @return a set of paths that lead to the NamedOntClass
	 */
	private Vector findPaths(NamedOntClass cls, DefaultMutableTreeNode root) {
		Vector paths = new Vector();
		Object obj = root.getUserObject();
		if (obj instanceof NamedOntClass) {
			if (cls.equals((NamedOntClass) obj)) {
				// add path to paths
				TreeNode[] ps = root.getPath();
				paths.add(new TreePath(root.getPath()));
			}
		}
		Enumeration children = root.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children
					.nextElement();
			Iterator descendants = findPaths(cls, child).iterator();
			while (descendants.hasNext())
				paths.add(descendants.next());
		}
		return paths;

	}

	// PRIVATE CLASSES

	/**
	 * Private class for rendering the ontology tree. Uses different icons for
	 * ontology nodes and class nodes.
	 */
	private class OntoClassSelectionJTreeRenderer extends
			DefaultTreeCellRenderer {

		private ImageIcon _classIcon, _ontoIcon;
		
		private final String CLASS_ICON = StaticResources.getSettingsString(
				"ONTOL_CLASS_TREEICON_PATH", "");
		private final String ONTO_ICON = StaticResources.getSettingsString(
				"ONTOLOGY_TREEICON_CLOSED_PATH", "");
	
		/** initializes the renderer */
		public OntoClassSelectionJTreeRenderer() {

			URL ontoImgURL = OntoClassSelectionJTreeRenderer.class
					.getResource(ONTO_ICON);
			if (ontoImgURL != null) {
				_ontoIcon = new ImageIcon(ontoImgURL);
			}
			URL classImgURL = null;
			//OntoClassSelectionJTreeRenderer.class.getResource(CLASS_ICON);
			if (classImgURL != null) {
				_classIcon = new ImageIcon(classImgURL);
			}

			// _classIcon = new ImageIcon(CLASS_ICON);
			// _ontoIcon = new ImageIcon(ONTO_ICON);
		}

		/** returns tree cell renderer */
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded,
					leaf, row, hasFocus);
			if (isClassNode(value)) {
				setIcon(_classIcon);
				setToolTipText("This is a class ... ");
			} else if (isOntoNode(value)) {
				setIcon(_ontoIcon);
				setToolTipText("This is an ontology ... ");
			}
			return this;
		}

		/**
		 * Determines if a given value is a class node
		 * 
		 * @return true if the given node object is a NamedOntClass
		 */
		protected boolean isClassNode(Object value) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object obj = node.getUserObject();
			if (obj instanceof NamedOntClass)
				return true;
			return false;
		}

		/**
		 * Determines if a given value is an ontology node
		 * 
		 * @return true if the given node object is a NamedOntModel
		 */
		protected boolean isOntoNode(Object value) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object obj = node.getUserObject();
			if (obj instanceof NamedOntModel)
				return true;
			return false;
		}

	};

	/**
	 * Private class implementing a listener for search button
	 */
	private class ClassSearchButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			doSearch(_searchTxt.getText());
		}
	};

	/**
	 * Private class for implementing a listener for add button
	 */
	private class AddButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			TreePath[] paths = _ontoTree.getSelectionPaths();
			if (paths.length < 1)
				return;
			for (int i = 0; i < paths.length; i++) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[i]
						.getLastPathComponent();
				if (node.getUserObject() instanceof NamedOntClass) {
					NamedOntClass cls = (NamedOntClass) node.getUserObject();
					addToList(cls);
				}
			}
		}
	};

	/**
	 * Private class for implementing a listener for remove button
	 */
	private class RemoveButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {
			Object[] vals = _classList.getSelectedValues();
			for (int i = 0; i < vals.length; i++) {
				NamedOntClass cls = (NamedOntClass) vals[i];
				removeFromList(cls);
			}
		}
	};

	/**
	 * Private class that extends JTree for icons and drag and drop
	 */
	private class OntoClassSelectionJTree extends JTree implements
			ActionListener {
		private JPopupMenu popup;
		private JMenuItem desc;
		private JMenuItem select;

		public OntoClassSelectionJTree(DefaultMutableTreeNode node) {
			super(node);
			popup = new JPopupMenu();
			popup.setOpaque(true);
			popup.setLightWeightPopupEnabled(true);
			initPopup();
			MouseAdapter mouseAdapter = new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger())
						doPopup(e);
				}

				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger())
						doPopup(e);
				}
			};
			addMouseListener(mouseAdapter);
			DragSource ds = DragSource.getDefaultDragSource();
			ds.createDefaultDragGestureRecognizer(this,
					DnDConstants.ACTION_COPY_OR_MOVE,
					new _OntoClassSelectionTreeGestureListener());
		}

		private void initPopup() {
			popup.removeAll();

			select = new JMenuItem("Add as Selection", KeyEvent.VK_A);
			select.addActionListener(this);
			select.setActionCommand("__SELECT");
			popup.add(select);
			popup.addSeparator();
		}

		private void doPopup(MouseEvent e) {

			// unhighlight nodes
			clearSelection();

			// highlight selected node
			int row = getRowForLocation(e.getX(), e.getY());
			if (row == -1)
				return;
			TreePath path = getPathForRow(row);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
					.getLastPathComponent();
			if (!(node.getUserObject() instanceof NamedOntClass))
				return;

			setSelectionRow(row);

			// configure popup
			initPopup();
			NamedOntClass cls = (NamedOntClass) node.getUserObject();
			Iterator props = cls.getNamedProperties(true);
			while (props.hasNext()) {
				NamedOntProperty p = (NamedOntProperty) props.next();
				JMenu submenu = new JMenu(p.getName());
				popup.add(submenu);
				Iterator ranges = p.getRange(true);
				while (ranges.hasNext()) {
					NamedOntClass range = (NamedOntClass) ranges.next();
					JMenuItem rangeitem = new JMenuItem(range.getName());
					rangeitem.addActionListener(this);
					rangeitem.setActionCommand(range.getNameSpace()
							+ range.getName());
					submenu.add(rangeitem);
				}
			}

			// show popup
			popup.show((JComponent) e.getSource(), e.getX(), e.getY());
		}

		public void actionPerformed(ActionEvent ae) {
			TreePath path = getSelectionPath();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
					.getLastPathComponent();
			NamedOntClass cls = (NamedOntClass) node.getUserObject();
			if (ae.getActionCommand().equals("__SELECT")) {
				addToList(cls);
			} else {
				String searchStr = ae.getActionCommand();
				NamedOntClass searchCls = OntologyCatalog.instance()
						.getNamedOntClass(searchStr);
				if (searchCls != null)
					doSelect(searchCls);
			}
		}
	};

	/**
	 * Private class for handling drag and drop initiation from the JTree
	 */
	private class _OntoClassSelectionTreeGestureListener implements
			DragGestureListener {
		public void dragGestureRecognized(DragGestureEvent e) {
			DragSourceListener dsl = new DragSourceListener() {
				public void dragDropEnd(DragSourceDropEvent dsde) {
				}

				public void dragEnter(DragSourceDragEvent dsde) {
					DragSourceContext context = dsde.getDragSourceContext();
					// Intersection of the users selected action,
					// and the source and target actions
					int myaction = dsde.getDropAction();
					// if((myaction & DnDConstants.ACTION_COPY_OR_MOVE) != 0)
					if ((myaction & DnDConstants.ACTION_COPY_OR_MOVE) != 0)
						context.setCursor(DragSource.DefaultCopyDrop);
					else
						context.setCursor(DragSource.DefaultCopyNoDrop);
				}

				public void dragExit(DragSourceEvent dse) {
				}

				public void dragOver(DragSourceDragEvent dsde) {
				}

				public void dropActionChanged(DragSourceDragEvent dsde) {
				}

			};
			Component source = e.getComponent();
			if (source instanceof OntoClassSelectionJTree) {
				OntoClassSelectionJTree tree = (OntoClassSelectionJTree) source;
				Point sourcePoint = e.getDragOrigin();
				TreePath path = tree.getPathForLocation(sourcePoint.x,
						sourcePoint.y);
				// If we didn't select anything.. then don't drag.
				if (path == null)
					return;
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
						.getLastPathComponent();
				if (node == null)
					return;
				if (node.getUserObject() instanceof NamedOntClass) {
					NamedOntClassTransferable transferable = new NamedOntClassTransferable();
					transferable
							.addObject((NamedOntClass) node.getUserObject());
					e
							.startDrag(DragSource.DefaultCopyNoDrop,
									transferable, dsl);
				}
			}
		}
	};

	/**
	 * Private class that extends JList for right click menus
	 */
	private class OntoClassSelectionJList extends JList implements
			ActionListener {
		private JPopupMenu popup;
		private JMenuItem navigate;
		private JMenuItem remove;

		public OntoClassSelectionJList(ListModel model) {
			super(model);
			popup = new JPopupMenu();
			popup.setOpaque(true);
			popup.setLightWeightPopupEnabled(true);
			initPopup();
			MouseAdapter mouseAdapter = new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger())
						doPopup(e);
				}

				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger())
						doPopup(e);
				}

				public void mouseClicked(MouseEvent ev) {
					// double click
					if (ev.getClickCount() == 2) {
						Object[] items = _classList.getSelectedValues();
						NamedOntClass cls = (NamedOntClass) items[0];
						doSelect(cls);
					}
				}
			};
			addMouseListener(mouseAdapter);
		}

		private void initPopup() {
			navigate = new JMenuItem("Show in Class Browser", KeyEvent.VK_S);
			navigate.addActionListener(this);
			navigate.setActionCommand("__NAVIGATE");
			popup.add(navigate);
			remove = new JMenuItem("Remove Selection", KeyEvent.VK_R);
			remove.addActionListener(this);
			remove.setActionCommand("__REMOVE");
			popup.add(remove);
		}

		private void doPopup(MouseEvent e) {
			// clear selection
			_classList.clearSelection();
			// figure out which item
			int index = _classList
					.locationToIndex(new Point(e.getX(), e.getY()));
			if (index == -1
					|| !_classList.getCellBounds(index, index).contains(
							e.getX(), e.getY()))
				return;
			// select the item
			_classList.setSelectedIndex(index);
			// show popup
			popup.show((JComponent) e.getSource(), e.getX(), e.getY());
		}

		public void actionPerformed(ActionEvent ae) {
			Object[] items = _classList.getSelectedValues();
			if (items == null || items.length < 1)
				return;
			NamedOntClass cls = (NamedOntClass) items[0];

			if (ae.getActionCommand().equals("__REMOVE"))
				removeFromList(cls);
			else if (ae.getActionCommand().equals("__NAVIGATE"))
				doSelect(cls);
		}
	};

	/**
	 * Private class for drop targets; copied/modified from ptolemy code base
	 */
	private class OntoClassSelectionJListDropTarget extends DropTarget {
		public OntoClassSelectionJListDropTarget(OntoClassSelectionJList list) {
			setComponent(list);
			try {
				addDropTargetListener(new OntoClassSelectionDropTargetListener(
						list));
			} catch (java.util.TooManyListenersException wow) {
			}
		}
	};

	/**
	 * Private class implementing a drop target listener for the list;
	 * copied/modified from ptolemy code base
	 */
	private class OntoClassSelectionDropTargetListener implements
			DropTargetListener {

		public OntoClassSelectionDropTargetListener(OntoClassSelectionJList list) {
			_list = list;
		}

		/**
		 * This is called while a drag operation is ongoing, when the mouse
		 * pointer enters the operable part of the drop site for the DropTarget
		 * registered with this listener.
		 * 
		 * @param dtde
		 *            The drop event.
		 */
		public void dragEnter(DropTargetDragEvent dtde) {
			DataFlavor df = NamedOntClassTransferable.getNamedOntClassFlavor();
			if (dtde.isDataFlavorSupported(df))
				dtde.acceptDrag(DnDConstants.ACTION_MOVE);
			else
				dtde.rejectDrag();
		}

		/**
		 * Remove any highlighting that might be active. This is called while a
		 * drag operation is ongoing, when the mouse pointer has exited the
		 * operable part of the drop site for the DropTarget registered with
		 * this listener.
		 * 
		 * @param dtde
		 *            The drop event.
		 */
		public void dragExit(DropTargetEvent dtde) {
			// _list.clearSelection();
		}

		/**
		 * This is called when a drag operation is ongoing, while the mouse
		 * pointer is still over the operable part of the drop site for the
		 * DropTarget registered with this listener.
		 * 
		 * @param dtde
		 *            The drop event.
		 */
		public void dragOver(DropTargetDragEvent dtde) {
			_list.clearSelection();
		}

		/**
		 * This is called when the drag operation has terminated with a drop on
		 * the operable part of the drop site for the DropTarget registered with
		 * this listener.
		 * 
		 * @param dtde
		 *            The drop event.
		 */
		public void drop(DropTargetDropEvent dtde) {
			// Unhighlight the target.
			_list.clearSelection();

			Iterator iterator = null;
			DataFlavor df = NamedOntClassTransferable.getNamedOntClassFlavor();
			if (dtde.isDataFlavorSupported(df)) {
				try {
					dtde.acceptDrop(DnDConstants.ACTION_MOVE);
					iterator = (Iterator) dtde.getTransferable()
							.getTransferData(df);
				} catch (Exception e) {
				}
			} else
				dtde.rejectDrop();

			// nothing to drop!
			if (iterator == null)
				return;

			// add the object
			while (iterator.hasNext()) {
				NamedOntClass obj = (NamedOntClass) iterator.next();
				addNamedOntClass(obj);
			}

			// notify drag source that the action is complete and successful
			dtde.dropComplete(true);
		}

		/**
		 * This is called if the user has modified the current drop gesture.
		 * 
		 * @param dtde
		 *            The drop event.
		 */
		public void dropActionChanged(DropTargetDragEvent dtde) {
		}

		private OntoClassSelectionJList _list;

	};

	/**
	 * Private class for handling tree selections. On selection, displays the
	 * comment for the selected class (if a comment exists).
	 */
	private class OntoClassTreeSelectionListener implements
			TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent ev) {
			TreePath[] paths = _ontoTree.getSelectionPaths();

			if (paths == null || paths.length < 1) {
				_commentTxt.setText("");
				return;
			}

			if (paths.length > 1) {
				_commentTxt.setText("");
			} else {
				TreePath path = paths[0];
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
						.getLastPathComponent();
				Object obj = node.getUserObject();

				if (!(obj instanceof NamedOntClass))
					_commentTxt.setText("");
				else {
					NamedOntClass cls = (NamedOntClass) obj;
					_commentTxt.setText(cls.getComment());
				}
			}

			// check if the only path is NamedOntModel
			_addBtn.setEnabled(false);
			for (int i = 0; i < paths.length; i++) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[i]
						.getLastPathComponent();
				if (node.getUserObject() instanceof NamedOntClass)
					_addBtn.setEnabled(true);
			}
		}
	};

	/**
	 * Private class for handling class list selections. On selection, enables
	 * remove button.
	 */
	private class ClassListSelectionListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent ev) {
			if (!ev.getValueIsAdjusting()) {
				Object[] selections = _classList.getSelectedValues();
				if (selections.length < 1)
					_removeBtn.setEnabled(false);
				else
					_removeBtn.setEnabled(true);
			}

		}
	};

	// TESTING

	/**
	 * Main method for testing
	 *
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.getContentPane().add(new OntoClassSelectionJPanel());
		frame.setTitle("Test Frame");
		frame.pack();
		frame.show();
	}*/

}