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

package org.kepler.sms.actors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.kepler.sms.KeplerVirtualIOPort;
import org.kepler.sms.SemanticType;
import org.kepler.sms.gui.SemanticTypeTable;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.TypedIORelation;
import ptolemy.kernel.util.NamedObj;

/*
 FIXME: 

 - the conversion table: 
 1). read in while loading mappings (both MergeEditor and MappingRefinement)
 2). store when changed in editor
 3). make sure stored as output

 - go back to _computeMerge ... 
 */

/**
 * 
 * @author Shawn Bowers
 */
public class MergeEditorDialog extends JDialog {

	/**
	 * Constructor
	 */
	public MergeEditorDialog(Frame owner, MergeActor mergeActor) {
		super(owner);
		_owner = owner;
		_mergeActor = mergeActor;

		_loadTargetPorts();
		_loadMappings();

		this.setTitle("Compute Merge Results");
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// add the tree part
		pane.add(_createPortTrees());
		pane.add(Box.createRigidArea(new Dimension(0, 10)));

		// add the conversion part
		pane.add(_createConversionTable());
		pane.add(Box.createRigidArea(new Dimension(0, 10)));

		// add the semantic type table
		pane.add(_createSemTypeTable(owner));
		// pane.setBorder(_createTitledBorder("Output Port Semantic Type"));
		pane.add(Box.createRigidArea(new Dimension(0, 10)));

		pane.add(_createButtonPane());

		// add the pane
		setContentPane(pane);
		// set size
		this.setSize(500, 725);
		this.setResizable(false);
	}

	/**
	 * Create and initialize the main buttons
	 */
	private JPanel _createButtonPane() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pane.add(Box.createHorizontalGlue());
		pane.add(_commitBtn);
		pane.add(Box.createRigidArea(new Dimension(10, 0)));
		pane.add(_closeBtn);

		// init buttons
		_commitBtn.setMnemonic(KeyEvent.VK_C);
		_commitBtn.setActionCommand("commit");
		_commitBtn
				.setToolTipText("Save changes to annotations and close editor");
		_commitBtn.addActionListener(_buttonListener);
		_closeBtn.setActionCommand("cancel");
		_closeBtn.setToolTipText("Close editor without saving");
		_closeBtn.addActionListener(_buttonListener);

		return pane;
	}

	/**
	 * Create and initialize the input and output port trees.
	 */
	private JPanel _createPortTrees() {
		// outer pane
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(_createTitledBorder("Input-Output Mapping"));

		// tree pane
		JPanel treePane = new JPanel();
		treePane.setLayout(new BoxLayout(treePane, BoxLayout.X_AXIS));

		// create the input tree
		_inputPortTree = new JTree(_createInputModel());
		_inputPortTree.setRootVisible(false);
		_inputPortTree.setEditable(false);
		_inputPortTree.setExpandsSelectedPaths(true);
		// listen for mouse clicks
		_inputPortTree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = _inputPortTree.getRowForLocation(e.getX(), e
						.getY());
				if (selRow == -1)
					return;
				TreePath selPath = _inputPortTree.getPathForLocation(e.getX(),
						e.getY());
				TreeNode node = (TreeNode) selPath.getLastPathComponent();
				if (node == null || e.getClickCount() != 1)
					return;
				TreePath[] paths = { selPath };
				_inputPortTree.setSelectionPaths(paths);
				_outputPortTree.setSelectionPaths(null);
				_conversionTbl.setModel(_emptyConversionModel);
				// set the model to an empty model instance
				if (node instanceof _PortTreeNode) {
					_inputSingleClick((_PortTreeNode) node);
					_semTypeTable.setEnabled(false);
					_semTypeTable
							.setAnnotationObjectVisible(_emptyAnnotationPort);
					_loadNewConversionTable((IOPort) ((_PortTreeNode) node)
							.getPort());
				}

			}
		});
		JScrollPane inputTreeView = new JScrollPane(_inputPortTree);
		JPanel inputTreePane = _labelComponent("merge input ports ",
				inputTreeView, 225, 175);

		// create the output tree
		_outputPortTree = new JTree(_createOutputModel());
		_outputPortTree.setRootVisible(false);
		_outputPortTree.setEditable(false);
		_inputPortTree.setExpandsSelectedPaths(true);
		// listen for mouse clicks
		_outputPortTree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = _outputPortTree.getRowForLocation(e.getX(), e
						.getY());
				if (selRow == -1)
					return;
				TreePath selPath = _outputPortTree.getPathForLocation(e.getX(),
						e.getY());
				TreeNode node = (TreeNode) selPath.getLastPathComponent();
				if (node == null || e.getClickCount() != 1)
					return;
				TreePath[] paths = { selPath };
				_outputPortTree.setSelectionPaths(paths);
				_inputPortTree.setSelectionPaths(null);
				_conversionTbl.setModel(_emptyConversionModel);
				if (node instanceof _PortTreeNode) {
					_outputSingleClick((_PortTreeNode) node);
					_semTypeTable.setEnabled(true);
					_semTypeTable
							.setAnnotationObjectVisible((IOPort) ((_PortTreeNode) node)
									.getPort());
				}
			}
		});
		JScrollPane outputTreeView = new JScrollPane(_outputPortTree);
		JPanel outputTreePane = _labelComponent("merge output ports ",
				outputTreeView, 225, 175);

		// add input and output tree to treepane
		treePane.add(inputTreePane);
		treePane.add(Box.createRigidArea(new Dimension(10, 0)));
		treePane.add(outputTreePane);

		// button pane
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// create the buttons
		JButton btnEdit = new JButton("Refine Mappings");
		btnEdit.setActionCommand("refine");
		btnEdit.addActionListener(_buttonListener);
		btnEdit.setToolTipText("Add or remove input-output port mappings");
		JButton btnPrune = new JButton("Prune Output Ports");
		btnPrune.setActionCommand("prune");
		btnPrune.addActionListener(_buttonListener);
		btnPrune.setToolTipText("Remove unused output ports");
		JButton btnAdd = new JButton("Add Output Port");
		btnAdd.setActionCommand("add");
		btnAdd.addActionListener(_buttonListener);
		btnAdd.setToolTipText("Add a new output port");

		// add buttons
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(btnEdit);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(btnPrune);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(btnAdd);

		// add intermediate panes to outer pane
		pane.add(treePane);
		pane.add(Box.createRigidArea(new Dimension(0, 5)));
		pane.add(buttonPane);
		// return it
		return pane;
	}

	/*
	 * Create and initialize the conversion function combo box
	 */
	private JPanel _createConversionTable() {
		// outer pane
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(_createTitledBorder("Conversion Functions"));

		// set up the table
		_conversionTbl = new JTable(_emptyConversionModel);
		_conversionTbl
				.setPreferredScrollableViewportSize(new Dimension(200, 80));
		// init
		_conversionTbl.setColumnSelectionAllowed(false);
		_conversionTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// init the combo box view pane
		JPanel view = new JPanel();
		view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
		view.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top,left,bottom,right
		view.add(new JScrollPane(_conversionTbl));
		// add to the pane
		pane.add(view);
		// return
		return pane;
	}

	/**
	 * Create and initialize the output semantic type table
	 */
	private JPanel _createSemTypeTable(Frame owner) {
		// outer pane
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(_createTitledBorder("Output Port Semantic Type"));
		// init the table
		_semTypeTable = new SemanticTypeTable(owner, false); // don't show the
																// label
		_semTypeTable.setEnabled(false);
		_initSemTypeTable();

		// add to the pane
		pane.add(_semTypeTable);
		// return
		return pane;
	}

	/**
     *
     */
	private void _setFunctionCombo() {
		TableColumn functionColumn = _conversionTbl.getColumnModel().getColumn(
				0);
		JComboBox comboBox = new JComboBox();
		comboBox.setEditable(false);
		comboBox.addItem("");
		comboBox.addItem("gramsToKilograms");
		comboBox.addItem("inchesToMeters");
		functionColumn.setCellEditor(new DefaultCellEditor(comboBox));
	}

	/**
     * 
     */
	private void _loadNewConversionTable(IOPort port) {
		_ConversionFunctionTableModel model = new _ConversionFunctionTableModel();
		_conversionTbl.setModel(model);

		// set up where the column draws functions from
		_setFunctionCombo();

		// iteration over mapping targets ...
		Iterator<SimpleMergeMapping> iter = _mergeActor.getInputPortMappings(port);
		while (iter.hasNext()) {
			SimpleMergeMapping m = iter.next();
			String function = m.getConversion();
			if (function == null)
				function = "";
			model.insertRow(function, m.getTargetPort());
		}
	}

	/**
     *
     */
	private void _initSemTypeTable() {
		Vector oldports = _semTypeTable.getAnnotationObjects();
		Vector<IOPort> currports = new Vector<IOPort>();
		Iterator<IOPort> iter = getTargetPorts().iterator();
		
		while (iter.hasNext()) {
			IOPort p = iter.next();
			currports.add(p);
			if (!oldports.contains(p))
				_semTypeTable.addAnnotationObject(p);
		}
		
		Iterator oldPortsIter = oldports.iterator();
		while (iter.hasNext()) {
			IOPort p = (IOPort) oldPortsIter.next();
			if (!currports.contains(p))
				_semTypeTable.removeAnnotationObject(p);
		}
		_semTypeTable.addAnnotationObject(_emptyAnnotationPort);
	}

	/**
	 * Create a titled border with a blue font
	 */
	private TitledBorder _createTitledBorder(String title) {
		TitledBorder border = BorderFactory.createTitledBorder(" " + title
				+ " ");
		border.setTitleColor(new Color(0, 10, 230));
		return border;
	}

	/**
	 * Call back for input port tree
	 */
	private void _inputSingleClick(_PortTreeNode portNode) {
		// get the output mappings for the port
		IOPort p = (IOPort) portNode.getPort();
		_selectPortNodes(_outputPortTree, getTargetPorts(p));
	}

	/**
	 * Call back for output port tree
	 */
	private void _outputSingleClick(_PortTreeNode portNode) {
		IOPort outputPort = (IOPort) portNode.getPort();
		Vector<IOPort> inputs = new Vector<IOPort>();
		Iterator iter = _mergeActor.connectedPortList().iterator();
		while (iter.hasNext()) {
			IOPort inPort = (IOPort) iter.next();
			Iterator<IOPort> targets = getTargetPorts(inPort).iterator();
			while (targets.hasNext()) {
				IOPort target = targets.next();
				if (target.equals(outputPort))
					inputs.add(inPort);
			}// end while
		}// end while
		_selectPortNodes(_inputPortTree, inputs);
	}

	/**
	 * Selects the given set of ports in the tree.
	 */
	private void _selectPortNodes(JTree tree, Vector<IOPort> ports) {
		Vector<TreePath> treePaths = new Vector<TreePath>();
		Iterator<IOPort> iter = ports.iterator();
		while (iter.hasNext()) {
			TreePath path = _getTreePath(iter.next(), tree);
			if (path != null && !treePaths.contains(path))
				treePaths.add(path);
		}
		TreePath[] t = new TreePath[treePaths.size()];
		for (int i = 0; i < treePaths.size(); i++)
			t[i] = treePaths.elementAt(i);
		tree.setSelectionPaths(t);
	}

	/**
	 * Gets the tree path for the given port in the given tree
	 */
	private TreePath _getTreePath(IOPort port, JTree tree) {
		TreeModel m = tree.getModel();
		TreeNode root = (TreeNode) m.getRoot();
		TreePath startpath = new TreePath(root);
		return _getTreePath(port, root, startpath);
	}

	/**
	 * Gets the tree path for the given port, a parent node, and a current path.
	 * The parent is the leaf node of the current path.
	 */
	private TreePath _getTreePath(IOPort port, TreeNode parent,
			TreePath currpath) {
		Enumeration children = parent.children();
		while (children.hasMoreElements()) {
			TreeNode t = (TreeNode) children.nextElement();
			TreePath path = currpath.pathByAddingChild(t);
			if (t instanceof _PortTreeNode
					&& port.equals(((_PortTreeNode) t).getPort()))
				return path;
			TreePath portpath = _getTreePath(port, t, path);
			if (portpath != null)
				return portpath;
		}// end while
		return null;
	}

	/**
	 * Creates and initializes the default tree model of the input tree
	 */
	protected DefaultTreeModel _createInputModel() {
		_ActorTreeNode root = new _ActorTreeNode(null);
		_inputTreeModel = new DefaultTreeModel(root);

		Iterator iter = _mergeActor.getActors().iterator();
		while (iter.hasNext()) {
			NamedObj actor = (NamedObj) iter.next();
			_ActorTreeNode anode = new _ActorTreeNode(actor);
			root.addChild(anode);
			Iterator ports = _mergeActor.getActorPorts(actor).iterator();
			while (ports.hasNext()) {
				IOPort port = (IOPort) ports.next();
				_PortTreeNode pnode = new _PortTreeNode(port);
				anode.addChild(pnode);
			}// end while
		}// end while

		return _inputTreeModel;
	}

	/**
	 * Creates and initializes the default tree model of the output tree
	 */
	protected DefaultTreeModel _createOutputModel() {
		_ActorTreeNode root = new _ActorTreeNode(null);
		_outputTreeModel = new DefaultTreeModel(root);

		Iterator<IOPort> iter = getTargetPorts().iterator();
		while (iter.hasNext()) {
			IOPort port = iter.next();
			_PortTreeNode pnode = new _PortTreeNode(port);
			root.addChild(pnode);
		}// end while
		return _outputTreeModel;
	}

	private void _resetGUI() {
		_outputPortTree.setSelectionPaths(null);
		_inputPortTree.setSelectionPaths(null);
		_semTypeTable.setAnnotationObjectVisible(_emptyAnnotationPort);
		_conversionTbl.setModel(_emptyConversionModel);
	}

	// //////////////////////////////////////////////////////////////////////////
	// LOCAL TARGET PORTS

	/**
	 * Load the target ports from the merge actor. This method should be called
	 * only once when the dialog is created.
	 */
	private void _loadTargetPorts() {
		Iterator iter = _mergeActor.outputPortList().iterator();
		while (iter.hasNext()) {
			try {
				IOPort p = (IOPort) ((IOPort) iter.next()).clone();
				p.setContainer(null);
				_targetPorts.add(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return the set of local target ports
	 */
	public Vector<IOPort> getTargetPorts() {
		return _targetPorts;
	}

	/**
	 * @return the set of local target ports for the given input port
	 */
	public Vector<IOPort> getTargetPorts(IOPort inputPort) {
		Vector<IOPort> results = new Vector<IOPort>();
		NamedObj actorObj = inputPort.getContainer();
		String actor = actorObj.getName();
		String port = inputPort.getName();
		Vector<String> targetNames = new Vector<String>();
		Iterator<SimpleMergeMapping> iter = getMappings().iterator();
		while (iter.hasNext()) {
			SimpleMergeMapping m = iter.next();
			if (actor.equals(m.getSourceActor())
					&& port.equals(m.getSourceActorPort()))
				targetNames.add(m.getTargetPort());
		}// end while
		
		Iterator<IOPort> ioPortIter = getTargetPorts().iterator();
		while (ioPortIter.hasNext()) {
			IOPort p = ioPortIter.next();
			if (targetNames.contains(p.getName()))
				results.add(p);
		}// end while
		return results;
	}

	/**
	 * removes the given target port
	 */
	public void removeTargetPort(IOPort port) {
		_targetPorts.remove(port);
	}

	/**
	 * adds the given target port
	 */
	public void addTargetPort(IOPort port) {
		if (!_targetPorts.contains(port))
			_targetPorts.add(port);
	}

	// //////////////////////////////////////////////////////////////////////////
	// LOCAL MAPPINGS

	private void _loadMappings() {
		Iterator iter = _mergeActor.getMappings().iterator();
		while (iter.hasNext()) {
			try {
				SimpleMergeMapping m = (SimpleMergeMapping) iter.next();
				SimpleMergeMapping mcopy = new SimpleMergeMapping(m
						.getSourceActor(), m.getSourceActorPort(), m
						.getTargetPort());
				if (m.getConversion() != null)
					mcopy.setConversion(m.getConversion());
				_mappings.add(mcopy);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}// end while
	}

	private void _printMappings() {
		Iterator<SimpleMergeMapping> iter = _mappings.iterator();
		while (iter.hasNext()) {
			SimpleMergeMapping m = iter.next();
			String actor = m.getSourceActor();
			String actorPort = m.getSourceActorPort();
			String target = m.getTargetPort();
			System.out.println("... mapping: " + actor + ", " + actorPort
					+ ", " + target);
		}
	}

	// for commit ...
	// m.setName(_mergeActor.uniqueName("_merge"));

	public Vector<SimpleMergeMapping> getMappings() {
		return _mappings;
	}

	public void removeMapping(SimpleMergeMapping m) {
		_mappings.remove(m);
	}

	/**
	 * replace current mappings with the new set
	 */
	public void addMapping(SimpleMergeMapping m) {
		if (!_mappings.contains(m))
			_mappings.add(m);
	}

	private void _doAdd() {
		String msg = "Please enter a port name";
		String portName = null;
		boolean success = true;

		_resetGUI();

		try {
			String title = "Create Merge Output Port";
			int type = JOptionPane.PLAIN_MESSAGE;
			portName = (String) JOptionPane.showInputDialog(this, msg, title,
					type);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (portName == null)
			return;
		if (portName.trim().length() < 1) {
			msg = "A port name must be provided";
			success = false;
		}
		if (success) {
			try {
				NamedObj o = new NamedObj();
				o.setName(portName);
			} catch (Exception e) {
				success = false;
				msg = e.getMessage();
			}
		}
		Iterator<IOPort> iter = getTargetPorts().iterator();
		while (success && iter.hasNext()) {
			IOPort p = iter.next();
			if (portName.equals(p.getName())) {
				success = false;
				msg = "A port named '" + portName + "' already exists.";
				break;
			}
		}// end while

		if (!success) {
			JOptionPane.showMessageDialog(this, msg, "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		// add the port
		TypedIOPort out = new TypedIOPort();
		try {
			out.setName(portName);
			out.setOutput(true);
			out.setInput(false);
			addTargetPort(out);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		// update the display
		_outputPortTree.setModel(_createOutputModel());
		// add to the sem port dialog
		_semTypeTable.addAnnotationObject(out);
	}

	private void _doPrune() {
		_resetGUI();

		Iterator iter = _getDanglingOutputPorts().iterator();
		Vector dangling = new Vector();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			dangling.add(p);
			removeTargetPort(p);
		}
		// refresh the dialog
		_outputPortTree.setModel(_createOutputModel());
		// remove from semantic type dialog
		iter = dangling.iterator();
		while (iter.hasNext())
			_semTypeTable.removeAnnotationObject((IOPort) iter.next());
	}

	private Vector<IOPort> _getDanglingOutputPorts() {
		Vector<IOPort> results = new Vector<IOPort>();
		Iterator<SimpleMergeMapping> iter = getMappings().iterator();
		Vector<String> targets = new Vector<String>();
		while (iter.hasNext()) {
			SimpleMergeMapping m = iter.next();
			targets.add(m.getTargetPort());
		}

		Iterator<IOPort> ioIter = getTargetPorts().iterator();
		while (iter.hasNext()) {
			IOPort p = ioIter.next();
			if (!targets.contains(p.getName()))
				results.add(p);
		}// end while
		return results;
	}

	/**
	 * Call back for the commit button
	 */
	private void _doCommit() {
		_resetGUI();

		String msg = null;
		int opt = JOptionPane.YES_NO_OPTION;
		int type = JOptionPane.WARNING_MESSAGE;
		// if any "dangling" ports output warning message
		if (_getDanglingOutputPorts().size() > 0) {
			msg = "Dangling output ports: Some output ports are not mapped to input ports \n"
					+ "Commit anyway?";
			if (1 == JOptionPane.showConfirmDialog(this, msg, "Warning", opt,
					type))
				return; // abort
		}
		// commit the semantic type changes
		if ((msg = _semTypeTable.wellFormedSemTypes()) != null) {
			JOptionPane.showMessageDialog(this, msg, "Message",
					JOptionPane.ERROR_MESSAGE);
			return; // abort
		}
		if (_semTypeTable.hasUnknownSemTypes()) {
			msg = "Unable to find a matching ontology class for at least one semantic type. \n"
					+ "Commit anyway?";
			if (1 == JOptionPane.showConfirmDialog(this, msg, "Message", opt,
					type))
				return; // abort
		}
		_semTypeTable.commitAnnotationObjects();

		// update the mergeActor mappings and output ports
		_saveChanges();

		// System.out.println(_mergeActor.exportMoML());

		_mergeActor.update();

		// close the dialog
		_doClose();
	}

	private void _saveChanges() {
		// remove existing output ports
		Iterator iter = _mergeActor.outputPortList().iterator();
		while (iter.hasNext()) {
			IOPort p = (IOPort) iter.next();
			try {
				p.setContainer(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}// end while

		// add the new ports
		iter = getTargetPorts().iterator();
		while (iter.hasNext()) {
			TypedIOPort p = (TypedIOPort) iter.next();
			try {
				TypedIOPort port = (TypedIOPort) p.clone(_mergeActor
						.workspace());
				port.setContainer(_mergeActor);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}// end while
		_mergeActor.setProductionRate();

		// remove existing mappings
		iter = _mergeActor.getMappings().iterator();
		while (iter.hasNext()) {
			try {
				SimpleMergeMapping m = (SimpleMergeMapping) iter.next();
				m.setContainer(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// for each mapping, set name and clone into workspace
		iter = getMappings().iterator();
		while (iter.hasNext()) {
			SimpleMergeMapping m = (SimpleMergeMapping) iter.next();
			try {
				m.setName(_mergeActor.uniqueName("_merge"));
				SimpleMergeMapping map = (SimpleMergeMapping) m
						.clone(_mergeActor.workspace());
				map.setContainer(_mergeActor);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Call back for the close button
	 */
	private void _doClose() {
		dispose();
	}

	/**
     * 
     */
	private void _doEdit() {
		_resetGUI();
		MappingRefinementDialog.showDialog(_owner, _mergeActor, this);
		_outputPortTree.setSelectionPaths(null);
		_inputPortTree.setSelectionPaths(null);
	}

	/**
	 * Given a label string, component, height, and width, creates a new panel
	 * with a label and the component of size height and width. The label is
	 * positioned above the component and is left justified.
	 */
	private JPanel _labelComponent(String str, Component component, int width,
			int height) {
		// output pane
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		// pane for label only
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new BoxLayout(labelPane, BoxLayout.X_AXIS));
		labelPane.add(new JLabel(str, SwingConstants.LEFT));
		labelPane.add(Box.createHorizontalGlue());
		// add label
		pane.add(labelPane);
		// add space
		pane.add(Box.createRigidArea(new Dimension(0, 5)));
		// add component
		pane.add(component);
		// set sizes
		pane.setMaximumSize(new Dimension(width, height));
		pane.setMinimumSize(new Dimension(width, height));
		pane.setPreferredSize(new Dimension(width, height));
		// return outer pane
		return pane;
	}

	/**
	 * Inner class for tree nodes
	 */
	private class _ActorTreeNode implements TreeNode {
		public _ActorTreeNode(NamedObj actor) {
			_actor = actor;
		}

		public NamedObj getActor() {
			return _actor;
		}

		public void addChild(_ActorTreeNode child) {
			if (!_children.contains(child)) {
				_children.add(child);
				child.removeFromParent();
				child.setParent(this);
			}
		}

		public void addChild(_PortTreeNode child) {
			if (!_children.contains(child)) {
				_children.add(child);
				child.removeFromParent();
				child.setParent(this);
			}
		}

		public void setParent(TreeNode parent) {
			_parent = parent;
		}

		public void removeFromParent() {
			_parent = null;
		}

		public Enumeration children() {
			return _children.elements();
		}

		public boolean getAllowsChildren() {
			return true;
		}

		public TreeNode getChildAt(int index) {
			if (index < _children.size() && index >= 0)
				return (TreeNode) _children.elementAt(index);
			return null;
		}

		public int getIndex(TreeNode node) {
			return _children.indexOf(node);
		}

		public int getChildCount() {
			return _children.size();
		}

		public TreeNode getParent() {
			return _parent;
		}

		public boolean isLeaf() {
			return _children.size() == 0;
		}

		public String toString() {
			if (_actor != null)
				return _actor.getName();
			return "";
		}

		private NamedObj _actor;
		private Vector _children = new Vector();
		private TreeNode _parent;
	};

	/**
	 * Inner class for port tree nodes
	 */
	private class _PortTreeNode implements TreeNode {
		public _PortTreeNode(Object port) {
			_port = port;
		}

		public Object getPort() {
			return _port;
		}

		public void addChild(_PortTreeNode child) {
			if (!_children.contains(child)) {
				_children.add(child);
				child.removeFromParent();
				child.setParent(this);
			}
		}

		public void setParent(TreeNode parent) {
			_parent = parent;
		}

		public void removeFromParent() {
			_parent = null;
		}

		public Enumeration children() {
			return _children.elements();
		}

		public boolean getAllowsChildren() {
			return true;
		}

		public TreeNode getChildAt(int index) {
			if (index < _children.size() && index >= 0)
				return (TreeNode) _children.elementAt(index);
			return null;
		}

		public int getIndex(TreeNode node) {
			return _children.indexOf(node);
		}

		public int getChildCount() {
			return _children.size();
		}

		public TreeNode getParent() {
			return _parent;
		}

		public boolean isLeaf() {
			return _children.size() == 0;
		}

		public String toString() {
			String str = "";
			if (_port instanceof IOPort)
				str += ((IOPort) _port).getName();
			else if (_port instanceof KeplerVirtualIOPort)
				str += ((KeplerVirtualIOPort) _port).getName();
			return str;
		}

		private Object _port;
		private Vector _children = new Vector();
		private TreeNode _parent;

	};

	/**
	 * anonymous class to handle button events
	 */
	private ActionListener _buttonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("commit"))
				_doCommit();
			else if (e.getActionCommand().equals("cancel"))
				_doClose();
			else if (e.getActionCommand().equals("refine")) {
				_doEdit(); // new dialog
			} else if (e.getActionCommand().equals("prune")) {
				_doPrune();
			} else if (e.getActionCommand().equals("add")) {
				_doAdd();
			}
		}
	};

	public static void main(String[] args) {
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
					.getSystemLookAndFeelClassName());

			ptolemy.actor.TypedCompositeActor cont = new ptolemy.actor.TypedCompositeActor();

			MergeActor m = new MergeActor(cont, "merge");

			ptolemy.actor.TypedCompositeActor a = new ptolemy.actor.TypedCompositeActor(
					cont, "A");
			ptolemy.actor.lib.Const a1_inner = new ptolemy.actor.lib.Const(a,
					"A1_inner");
			a1_inner.value.setToken(new ptolemy.data.IntToken(1));
			ptolemy.actor.lib.Const a2_inner = new ptolemy.actor.lib.Const(a,
					"A2_inner");
			a2_inner.value.setToken(new ptolemy.data.IntToken(4));
			TypedIOPort a_output1 = new TypedIOPort(a, "output1", false, true);
			TypedIOPort a_output2 = new TypedIOPort(a, "output2", false, true);
			TypedIORelation a1rel = new TypedIORelation(a, "a1rel");
			a1_inner.output.link(a1rel);
			a_output1.link(a1rel);
			TypedIORelation a2rel = new TypedIORelation(a, "a2rel");
			a2_inner.output.link(a2rel);
			a_output2.link(a1rel);

			ptolemy.actor.lib.Const b = new ptolemy.actor.lib.Const(cont, "B");
			b.value.setToken(new ptolemy.data.IntToken(2));

			ptolemy.actor.lib.Const c = new ptolemy.actor.lib.Const(cont, "C");
			c.value.setToken(new ptolemy.data.IntToken(3));

			TypedIORelation arel11 = new TypedIORelation(cont, "rel11");
			a_output1.link(arel11);
			m.mergeInputPort.link(arel11);

			TypedIORelation arel12 = new TypedIORelation(cont, "rel12");
			a_output2.link(arel12);
			m.mergeInputPort.link(arel12);

			TypedIORelation brel = new TypedIORelation(cont, "rel2");
			b.output.link(brel);
			m.mergeInputPort.link(brel);

			TypedIORelation crel = new TypedIORelation(cont, "rel3");
			c.output.link(crel);
			m.mergeInputPort.link(crel);

			// semantic port annotations
			SemanticType t1 = new SemanticType(b.output, b.output
					.uniqueName("_semanticType"));
			t1
					.setConceptId("urn:lsid:lsid.ecoinformatics.org:onto:4:1#BiomassMeasurement");
			SemanticType t2 = new SemanticType(c.output, c.output
					.uniqueName("_semanticType"));
			t2
					.setConceptId("urn:lsid:lsid.ecoinformatics.org:onto:4:1#BiomassMeasurement");

			m.computeMerge();
			m.editMerge();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// a table for the conversion functions: t(function, output port)
	/**
	 * inner class for managing the table data
	 */
	private class _ConversionFunctionTableModel extends AbstractTableModel {
		// private members
		private String[] tableHeader = { "Function", "Output Port" }; // two
																		// columns
		private Vector tableData = new Vector(); // vector of arrays

		public int getColumnCount() {
			return 2;
		}

		public int getRowCount() {
			return tableData.size();
		}

		public String getColumnName(int columnIndex) {
			return tableHeader[columnIndex];
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			Object[] row = (Object[]) tableData.elementAt(rowIndex);
			return row[columnIndex];
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (columnIndex == 0)
				return true;
			return false;
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (!validRowIndex(rowIndex))
				return;
			Object[] row = (Object[]) tableData.elementAt(rowIndex);
			row[columnIndex] = aValue;
			fireTableChanged(new TableModelEvent(this)); // change event
		}

		public void insertRow(String function, String portName) {
			Object[] row = new Object[2];
			row[0] = function;
			row[1] = portName;
			if (rowExists(row)) // if exists, return
				return;
			// make sure there are no empty rows (if so, to first one)
			int index = firstEmptyRow();
			if (index != -1)
				tableData.setElementAt(row, index);
			else
				tableData.add(row);
			fireTableChanged(new TableModelEvent(this));
		}

		public boolean containsRow(String function, String portName) {
			Object[] tmpRow = new Object[2];
			tmpRow[0] = function;
			tmpRow[1] = portName;
			return rowExists(tmpRow);
		}

		/**
		 * @return a list of rows of non-empty Object arrays of arity 2
		 *         (Object[2])
		 */
		public Iterator getRows() {
			Vector results = new Vector();
			for (Iterator iter = tableData.iterator(); iter.hasNext();) {
				Object[] row = (Object[]) iter.next();
				if (row[0] != null || row[1] != null)
					results.add(row);
			}
			return results.iterator();
		}

		public void insertEmptyRow() {
			tableData.addElement(new Object[2]);
			fireTableChanged(new TableModelEvent(this)); // change event
		}

		public int firstEmptyRow() {
			int index = 0;
			for (Iterator iter = tableData.iterator(); iter.hasNext(); index++) {
				Object[] row = (Object[]) iter.next();
				if (row[0] == null && row[1] == null)
					return index;
			}
			return -1;
		}

		private boolean rowExists(Object[] newrow) {
			for (Iterator iter = tableData.iterator(); iter.hasNext();) {
				Object[] row = (Object[]) iter.next();
				if (newrow[0].equals(row[0]) && newrow[1].equals(row[1]))
					return true;
			}
			return false;
		}

		public void removeRow(int rowIndex) {
			if (!validRowIndex(rowIndex))
				return;
			tableData.removeElementAt(rowIndex);
			fireTableChanged(new TableModelEvent(this)); // change event
		}

		private boolean validRowIndex(int rowIndex) {
			if (rowIndex >= 0 && rowIndex < getRowCount())
				return true;
			return false;
		}
	};

	private Frame _owner;
	private MergeActor _mergeActor;
	private JButton _commitBtn = new JButton("Commit");
	private JButton _closeBtn = new JButton("Cancel");
	private DefaultTreeModel _outputTreeModel;
	private DefaultTreeModel _inputTreeModel;
	private JTree _outputPortTree;
	private JTree _inputPortTree;
	private SemanticTypeTable _semTypeTable;
	private IOPort _emptyAnnotationPort = new IOPort();
	private JTable _conversionTbl;
	private _ConversionFunctionTableModel _emptyConversionModel = new _ConversionFunctionTableModel();
	 // a local copy of the merge actor mappings
	private Vector<SimpleMergeMapping> _mappings = new Vector<SimpleMergeMapping>();
	// a local copy of the target output ports
	private Vector<IOPort> _targetPorts = new Vector<IOPort>(); 

}// MergeEditorDialog