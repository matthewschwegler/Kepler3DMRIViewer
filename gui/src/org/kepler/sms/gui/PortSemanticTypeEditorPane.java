/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2010-11-09 17:36:37 -0800 (Tue, 09 Nov 2010) $' 
 * '$Revision: 26278 $'
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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.kepler.moml.NamedObjId;
import org.kepler.sms.KeplerCompositeIOPort;
import org.kepler.sms.KeplerIOPortReference;
import org.kepler.sms.KeplerRefinementIOPort;
import org.kepler.sms.KeplerVirtualIOPort;
import org.kepler.sms.NamedOntClass;
import org.kepler.sms.NamedOntModel;
import org.kepler.sms.OntologyCatalog;
import org.kepler.sms.SMSServices;
import org.kepler.sms.SemanticType;
import org.kepler.sms.SemanticTypeManager;
import org.kepler.sms.SemanticTypeManagerMemento;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.NamedObj;

/*

 ALGORITHM: 

 - First build the port model (without type manager)
 - Go through the port model, and create the manager (with types)
 - Get and store a momento

 */

/**
 * @author Shawn Bowers
 * 
 *         TODO: 1. Fix composite port support a. display using different color
 *         b. implement removal
 */
public class PortSemanticTypeEditorPane extends JPanel {

	/**
	 * Constructor
	 */
	public PortSemanticTypeEditorPane(Frame owner, NamedObj namedObj,
			int direction) {
		super();
		_owner = owner;
		_namedObj = namedObj;
		_direction = direction;

		// add the semantic type
		_semTypeTable = new SemanticTypeTable(owner);
		_classSelector = new OntoClassSelectionJPanel(false, 500, 370);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		add(Box.createRigidArea(new Dimension(0, 5)));
		add(_createPortTree());
		add(Box.createRigidArea(new Dimension(0, 5)));
		add(_classSelector);

		// initialize the semantic type manager
		_initSemanticTypeManager();

		// get a memento for checking for changes
		_memento = _manager.createMemento();

		// select the first port here ...
		_defaultSelection();
	}

	// //////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS

	/**
	 * Performs the commit button operation.
	 */
	public void doCommit() {
		// save the state of the old selection
		Object port = null;
		if (_selectedNode != null) {
			port = _selectedNode.getPort();
		}

		// remove old state
		_manager.removeTypes(port);

		Iterator<NamedOntClass> typeIter = _classSelector.getNamedOntClasses().iterator();
		while (typeIter.hasNext())
			_manager.addType(port, typeIter.next());

		// do the commit itself
		Iterator objIter = _manager.getObjects().iterator();
		while (objIter.hasNext()) {
			NamedObj obj = (NamedObj) objIter.next();
			
			Vector<NamedOntClass> namedOntClasses = _manager.getTypesAsNamedOntClasses(obj);
			Iterator<NamedOntClass> nocItr = namedOntClasses.iterator();
			Vector<NamedOntClass> nocToAdd = _manager.getTypesAsNamedOntClasses(obj);
			
			//remove from existing those that are not in namedOntClasses
			Iterator<SemanticType> existingSemTypeItr = obj.attributeList(SemanticType.class).iterator();
			while (existingSemTypeItr.hasNext()){
				SemanticType existingSemType = existingSemTypeItr.next();
				boolean remove = true;
				nocItr = namedOntClasses.iterator();
				while (nocItr.hasNext()){
					NamedOntClass noc = nocItr.next();
					if (noc.getConceptId().equals(existingSemType.getConceptId())){
						
						// remove it from the to-add list, it's already there
						nocToAdd.remove(noc);
						
						// and don't remove it from existing.
						remove = false;
						break;
					}
				}
				if (remove){
					try {
						// REMOVE
						existingSemType.setContainer(null);
						// update LSID revision
						NamedObj container = obj.getContainer();
						NamedObjId.getIdAttributeFor(container).updateRevision();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			// now add those that aren't already there
			_addSemTypes(obj, nocToAdd);
		}
	}

	/**
	 * Adds the semantic types in the dialog to the entity.
	 */
	private void _addSemTypes(NamedObj obj, Vector semTypes) {
		Iterator typeIter = semTypes.iterator();
		while (typeIter.hasNext()) {
			try {
				SemanticType st = new SemanticType(obj, obj
						.uniqueName("semanticType"));
				st.setExpression(((NamedOntClass) typeIter.next())
						.getConceptId());
				
				// update LSID revision
				NamedObj container = obj.getContainer();
				NamedObjId.getIdAttributeFor(container).updateRevision();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Removes the current semtypes on the named object
	 */
	private void _removeNamedObjSemtypes(NamedObj obj) {
		if (obj == null)
			return;

		Vector<SemanticType> semtypes = new Vector<SemanticType>();
		Iterator<SemanticType> iter = obj.attributeList(SemanticType.class).iterator();
		while (iter.hasNext())
			semtypes.add(iter.next());

		iter = semtypes.iterator();
		while (iter.hasNext()) {
			SemanticType st = iter.next();
			try {
				st.setContainer(null);
				// update LSID revision
				NamedObj container = obj.getContainer();
				NamedObjId.getIdAttributeFor(container).updateRevision();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// not needed anymore (?)
	public void doClose() {
	}

	/**
	 * @return true if any semantic type annotations have been modified.
	 */
	public boolean hasModifiedSemTypes() {
		// update the current selection
		// save the state of the old selection
		Object obj = null;
		if (_selectedNode != null) {
			obj = _selectedNode.getPort();
		}

		// remove old state
		_manager.removeTypes(obj);

		Iterator<NamedOntClass> typeIter = _classSelector.getNamedOntClasses().iterator();
		while (typeIter.hasNext())
			_manager.addType(obj, typeIter.next());

		// check if the manager is different than the memento
		if (_manager.isModified(_memento))
			return true;

		return false;
	}

	/**
	 * TODO: Determine checks for well-formed annotations
	 * 
	 * @return The first error if semantic type annotations are not well formed,
	 *         and null otherwise.
	 */
	public String wellFormedSemTypes() {
		// return _semTypeTable.wellFormedSemTypes();
		return null;
	}

	// //////////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS

	/**
     * 
     */
	private JPanel _createPortTree() {
		// outer pane
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

		// tree pane
		JPanel treePane = new JPanel();
		treePane.setLayout(new BoxLayout(treePane, BoxLayout.X_AXIS));

		// create the input tree
		_portTree = new JTree(_createModel());
		_portTree.setRootVisible(false);
		_portTree.setEditable(false);
		_portTree.setCellRenderer(new MyRenderer());
		_portTree.setShowsRootHandles(true);

		// Listen for when the selection changes.
		_portTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				_PortTreeNode node = (_PortTreeNode) _portTree
						.getLastSelectedPathComponent();
				if (node == null)
					return;
				_singleClick((_PortTreeNode) node);
			}
		});

		TreeSelectionModel m = _portTree.getSelectionModel();
		m.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// select the first port here ...
		// _defaultSelection();

		JScrollPane treeView = new JScrollPane(_portTree,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		treeView.setMaximumSize(new Dimension(500, 115));
		treeView.setMinimumSize(new Dimension(500, 115));
		treeView.setPreferredSize(new Dimension(500, 115));

		// button pane
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// create the buttons
		// _generalizeBtn.setEnabled(false);
		_generalizeBtn.setActionCommand("generalize");
		_generalizeBtn.addActionListener(_buttonListener);
		_generalizeBtn.updateUI();
		// _generalizeBtn.setToolTipText("Create a group of ports");
		_generalizeBtn.setText("Bundle Ports");

		_portRemoveBtn.setEnabled(false);
		_portRemoveBtn.setActionCommand("remove");
		_portRemoveBtn.addActionListener(_buttonListener);
		// _portRemoveBtn.setToolTipText("Remove a virtual port");
		_portRemoveBtn.setText("Unbundle Ports");

		// add buttons
		// _compositeBox.setEnabled(false);
		buttonPane.add(_compositeBox);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(_generalizeBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(_portRemoveBtn);

		// label pane
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new BoxLayout(labelPane, BoxLayout.X_AXIS));
		labelPane.add(new JLabel("Select port for annotation:",
				SwingConstants.LEFT));
		labelPane.add(Box.createHorizontalGlue());

		// add intermediate panes to outer pane
		pane.add(labelPane);
		pane.add(Box.createRigidArea(new Dimension(0, 2)));
		pane.add(treeView);
		pane.add(Box.createRigidArea(new Dimension(0, 5)));
		pane.add(buttonPane);

		return pane;
	}

	/**
	 * This is called in the composite dialog Need to figure out how this should
	 * look ... TODO: 1. icons for: input/output ports 2. icons for:
	 * input/output refinements 3. icons for: input/output bundles Ports within
	 * bundles are simply the reg. ports
	 */
	protected DefaultTreeModel _createModel() {
		_PortTreeNode root = new _PortTreeNode("");
		_treeModel = new DefaultTreeModel(root);

		// get all the ports (real and virtual)
		Vector ioPorts = SMSServices.getIOPorts(_namedObj);
		Vector compositePorts = SMSServices.getPortBundles(_namedObj);
		Vector refinePortNames = new Vector();

		// get the refinemet port names
		Iterator refIter = SMSServices.getRefinementPorts(_namedObj).iterator();
		while (refIter.hasNext())
			refinePortNames.add(((KeplerRefinementIOPort) refIter.next())
					.getPointer());

		// add the objects semantic types
		// Iterator typeIter = _getSemTypes(obj).iterator();
		// while(typeIter.hasNext())
		// _manager.addType(obj, (String)typeIter.next());

		// build up the regular ports first
		Iterator portIter = ioPorts.iterator();
		while (portIter.hasNext()) {
			TypedIOPort port = (TypedIOPort) portIter.next();
			if (validPort(port)) {
				_PortTreeNode node = new _PortTreeNode(port);
				root.addChild(node);
				Type t = port.getType();
				if (t instanceof RecordType)
					_addRefinementPorts(node, (RecordType) t, refinePortNames);
				// else if(t instanceof ArrayType)
				// _addRefinementPorts(node, (ArrayType)t, refinePortNames);
			}
		}

		// build up the composite ports if selected
		if (!_compositeBox.isSelected())
			return _treeModel;

		Iterator compPortIter = compositePorts.iterator();
		while (compPortIter.hasNext()) {
			KeplerCompositeIOPort port = (KeplerCompositeIOPort) compPortIter
					.next();
			if (validPort(port)) {
				_PortTreeNode node = new _CompositePortTreeNode(port);
				root.addChild(node);
				// get the "children" and add ...
				Iterator refs = port.getEncapsulatedPortReferences().iterator();
				while (refs.hasNext()) {
					KeplerIOPortReference ref = (KeplerIOPortReference) refs
							.next();
					_PortTreeNode ref_node = new _ReferencePortTreeNode(ref
							.getPort());
					node.addChild(ref_node);
				}
			}
		}

		return _treeModel;
	}

	/**
	 * Traverse the tree tree model, storing any annotations.
	 */
	private void _initSemanticTypeManager() {
		_PortTreeNode root = (_PortTreeNode) _treeModel.getRoot();
		for (int i = 0; i < root.getChildCount(); i++)
			_initSemanticTypeManager((_PortTreeNode) root.getChildAt(i));
	}

	private void _initSemanticTypeManager(_PortTreeNode node) {
		Object obj = node.getPort();
		_manager.addObject(obj);

		Iterator typeIter = _getSemTypes(obj).iterator();
		while (typeIter.hasNext()) {
			SemanticType st = (SemanticType) typeIter.next();
			NamedOntClass cls = OntologyCatalog.instance().getNamedOntClass(st);
			_manager.addType(obj, cls);
		}

		for (int i = 0; i < node.getChildCount(); i++)
			_initSemanticTypeManager((_PortTreeNode) node.getChildAt(i));
	}

	private Vector _getSemTypes(Object obj) {
		if (obj instanceof IOPort)
			return SMSServices.getPortSemanticTypes((IOPort) obj);
		else if (obj instanceof KeplerRefinementIOPort)
			return SMSServices
					.getPortSemanticTypes((KeplerRefinementIOPort) obj);
		else if (obj instanceof KeplerCompositeIOPort)
			return SMSServices
					.getPortSemanticTypes((KeplerCompositeIOPort) obj);
		return new Vector();
	}

	/**
	 * @return True if the port is of the right direction; false otherwise.
	 * @param p
	 *            The IOPort of interest
	 */
	public boolean validPort(IOPort p) {
		if (_direction == OUTPUT && p.isOutput())
			return true;
		if (_direction == INPUT && p.isInput())
			return true;
		return false;
	}

	/**
	 * @return True if the port is of the right direction; false otherwise.
	 * @param p
	 *            The KeplerCompositeIOPort of interest
	 */
	public boolean validPort(KeplerCompositeIOPort p) {
		if (_direction == OUTPUT && p.isOutput())
			return true;
		if (_direction == INPUT && p.isInput())
			return true;
		return false;
	}

	/**
     *
     */
	private void _addRefinementPorts(_PortTreeNode parent, RecordType t,
			Vector refinePortNames) {
		Iterator iter = t.labelSet().iterator(); // get record component labels
		while (iter.hasNext()) {
			String label = (String) iter.next();
			Object obj = parent.getPort();
			String portname = null;
			if (obj instanceof IOPort)
				portname = ((IOPort) obj).getName();
			else if (obj instanceof KeplerVirtualIOPort)
				portname = ((KeplerVirtualIOPort) obj).getName();
			String pointer = portname + "/" + label;
			// check if exists already as a port
			if (!refinePortNames.contains(pointer)) {
				try {
					KeplerRefinementIOPort p = new KeplerRefinementIOPort(
							_namedObj, pointer);
					_PortTreeNode node = new _PortTreeNode(p);
					parent.addChild(node);
					if (p.getType() instanceof RecordType) {
						_addRefinementPorts(node, (RecordType) p.getType(),
								refinePortNames);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
     * 
     */
	private void _doGeneralize() {
		KeplerCompositeIOPort newPort;
		newPort = CompositePortDialog.showDialog(_owner, (Entity) _namedObj,
				_direction);
		if (newPort == null)
			return;
		_portTree.setModel(_createModel());
	}

	/**
     * 
     */
	private void _doRemove() {
	}

	/**
     *
     */
	private void _singleClick(_PortTreeNode node) {
		if (_selectedNode == null) {
			_selectedNode = node;
			return;
		}

		// save the state of the old selection
		Object obj = null;
		if (_selectedNode != null) {
			obj = _selectedNode.getPort();
		}

		// remove old state
		_manager.removeTypes(obj);

		Iterator<NamedOntClass> typeIter = _classSelector.getNamedOntClasses().iterator();
		while (typeIter.hasNext())
			_manager.addType(obj, typeIter.next());

		// load in the new state
		_selectedNode = node;

		_updateGUI();
	}

	private void _defaultSelection() {
		TreeModel model = _portTree.getModel();
		Object root = model.getRoot();
		if (model != null && model.getChildCount(root) > 0) {
			Object child = model.getChild(root, 0);
			int index = model.getIndexOfChild(root, child);
			_portTree.setSelectionInterval(index, index);
		} else
			_portTree.setSelectionRow(-1);
		_updateGUI();
	}

	/**
     *
     */
	private void _updateGUI() {
		if (_selectedNode == null)
			return;

		// get the port of the selected node
		Object obj = null;
		if (_selectedNode != null) {
			obj = _selectedNode.getPort();
		}

		// clear the class selector
		_classSelector.clear();
		// add the types
		Iterator typeIter = _manager.getTypes(obj).iterator();
		while (typeIter.hasNext())
			_classSelector.addNamedOntClass((NamedOntClass) typeIter.next());
	}

	/**
	 * Inner class for tree nodes
	 */
	private class _PortTreeNode extends DefaultMutableTreeNode {
		/** constructor */
		public _PortTreeNode(Object port) {
			_port = port;
		}

		/** @return The port encapsulated in the node */
		public Object getPort() {
			return _port;
		}

		/** Add given node as child */
		public void addChild(_PortTreeNode child) {
			if (!_children.contains(child)) {
				_children.add(child);
				child.removeFromParent();
				child.setParent(this);
			}
		}

		/** Set the parent of this node */
		public void setParent(_PortTreeNode parent) {
			_parent = parent;
		}

		/** Remove the parent of this node */
		public void removeFromParent() {
			_parent = null;
		}

		/** @return The children of this node */
		public Enumeration children() {
			return _children.elements();
		}

		/** @return Always true. */
		public boolean getAllowsChildren() {
			return true;
		}

		/** @return The ith tree node */
		public TreeNode getChildAt(int index) {
			if (index < _children.size() && index >= 0)
				return (TreeNode) _children.elementAt(index);
			return null;
		}

		/** @return the index of the child node */
		public int getIndex(TreeNode node) {
			return _children.indexOf(node);
		}

		/** @return The number of children */
		public int getChildCount() {
			return _children.size();
		}

		/** @return The parent of this node */
		public TreeNode getParent() {
			return _parent;
		}

		/** @return True if no children */
		public boolean isLeaf() {
			return _children.size() == 0;
		}

		public String toString() {
			String str = "";
			if (_port instanceof IOPort)
				str += ((IOPort) _port).getName() + "  (";
			else if (_port instanceof KeplerCompositeIOPort)
				str += ((KeplerCompositeIOPort) _port).getName() + "  (";
			else if (_port instanceof KeplerRefinementIOPort)
				str += ((KeplerRefinementIOPort) _port).getName() + "  (";

			if (_port instanceof TypedIOPort) {
				Type type = ((TypedIOPort) _port).getType();
				if (type instanceof BaseType)
					str += type;
				else if (type instanceof RecordType)
					str += "record";
				else if (type instanceof ArrayType)
					str += "array";
				else
					str += "other";
			} else if (_port instanceof KeplerVirtualIOPort) {
				Type type = ((KeplerVirtualIOPort) _port).getType();
				if (type instanceof BaseType)
					str += type;
				else if (type instanceof RecordType)
					str += "record";
				else if (type instanceof ArrayType)
					str += "array";
				else
					str += "other";
			} else
				str += "unknown";
			return str + ")";
		}

		/* Private members */
		private Object _port;
		private Vector _children = new Vector();
		private _PortTreeNode _parent;

	};

	private class _CompositePortTreeNode extends _PortTreeNode {
		/** constructor */
		public _CompositePortTreeNode(Object port) {
			super(port);
		}
	};

	private class _RefinementPortTreeNode extends _PortTreeNode {
		/** constructor */
		public _RefinementPortTreeNode(Object port) {
			super(port);
		}
	};

	private class _ReferencePortTreeNode extends _PortTreeNode {
		/** constructor */
		public _ReferencePortTreeNode(Object port) {
			super(port);
		}
	};

	/**
	 * private class for rendering the ontology tree. Uses different icons for
	 * ontology nodes and class nodes.
	 */
	private class MyRenderer extends DefaultTreeCellRenderer {
		private ImageIcon _portIcon = new ImageIcon(IOPORT_ICON);
		private ImageIcon _virtPortIcon = new ImageIcon(VIRT_IOPORT_ICON);

		/** initializes the renderer */
		public MyRenderer() {
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded,
					leaf, row, hasFocus);
			if (isIOPort(value))
				setIcon(_portIcon);
			else
				setIcon(_virtPortIcon);
			return this;
		}

		/** @return True if the given node object is a ... */
		protected boolean isIOPort(Object value) {
			_PortTreeNode node = (_PortTreeNode) value;
			Object port = node.getPort();
			if (port instanceof IOPort)
				return true;
			return false;
		}

		/** @return True if the given node object is a ... */
		protected boolean isOntoNode(Object value) {
			_PortTreeNode node = (_PortTreeNode) value;
			Object port = node.getPort();
			if (port instanceof NamedOntModel)
				return true;
			return false;
		}

	}; // inner class

	/**
	 * anonymous class to handle button events
	 */
	private ActionListener _buttonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("generalize"))
				_doGeneralize();
			else if (e.getActionCommand().equals("remove"))
				_doRemove();
		}
	};

	/**
	 * Add an object to the set of objects being annotated within this panel.
	 * 
	 * @param obj
	 *            The object to add.
	 */
	private void _addAnnotationObject(NamedObj obj) {
		if (obj == null)
			return;

		for (Iterator iter = _annotationObjects.iterator(); iter.hasNext();) {
			Object[] entry = (Object[]) iter.next();
			if (entry[0].equals(obj))
				return;
		}
		Object[] entry = new Object[2];
		entry[0] = obj;
		// entry[1] = _initializeNamedObj(obj);
		_annotationObjects.add(entry);
	}

	/**
	 * Remove the given annotation object from the editor
	 */
	private void _removeAnnotationObject(NamedObj obj) {
		if (obj == null)
			return;
		Object[] objentry = null;
		for (Iterator iter = _annotationObjects.iterator(); iter.hasNext();) {
			Object[] entry = (Object[]) iter.next();
			if (entry[0].equals(obj)) {
				objentry = entry;
				break;
			}
		}
		if (objentry == null)
			return;
		_annotationObjects.remove(objentry);
	}

	/**
	 * @return The set of annotation objects being annotated with the panel.
	 */
	private Vector _getAnnotationObjects() {
		Vector result = new Vector();
		for (Iterator iter = _annotationObjects.iterator(); iter.hasNext();) {
			Object[] entry = (Object[]) iter.next();
			result.add(entry[0]);
		}
		return result;
	}

	/* Private Members */

	private JTree _portTree;
	private JCheckBox _compositeBox = new JCheckBox("Show bundled ports");
	private OntoClassSelectionJPanel _classSelector; // annotate to classes
	private JButton _generalizeBtn = new JButton();
	private JButton _portRemoveBtn = new JButton();

	private SemanticTypeManager _manager = new SemanticTypeManager();
	private SemanticTypeManagerMemento _memento;

	private DefaultTreeModel _treeModel;
	private _PortTreeNode _selectedNode; // currently selected tree node
	private SemanticTypeTable _semTypeTable; // going away
	private Vector _annotationObjects = new Vector();
	private Frame _owner; // the frame owner
	private NamedObj _namedObj; // the actor/entity being types
	private int _direction; // direction of the ports

	public static int INPUT = 1; // input ports
	public static int OUTPUT = 2; // output ports
	public static int PARAMETER = 3; // not used currently

	private String KEPLER = System.getProperty("KEPLER");
	private String IOPORT_ICON = KEPLER
			+ "/configs/ptolemy/configs/kepler/sms/ioport.png";
	private String VIRT_IOPORT_ICON = KEPLER
			+ "/configs/ptolemy/configs/kepler/sms/ioportv.png";
	private String COMP_IOPORT_ICON = KEPLER
			+ "/configs/ptolemy/configs/kepler/sms/ioportc.png";

}