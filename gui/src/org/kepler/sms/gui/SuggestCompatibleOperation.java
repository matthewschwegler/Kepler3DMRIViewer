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

// LibraryIndex.item()
// LibraryIndexComponentItem.getLSID()
// CacheManager.getInstance().getObject(lsid)
// ActorCacheObject.getMetadata().getActorAsNamedObject()
// ActorCacheObject aco = (ActorCacheObject)cacheMan.getObject(new KeplerLSID(lsidString));
// ActorMetadata am = aco.getMetadata();
// NamedObj no = am.getActorAsNamedObj(null);

package org.kepler.sms.gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.kepler.gui.GraphicalActorMetadata;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.sms.SMSServices;
import org.kepler.sms.SemanticTypeManager;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.EntityLibrary;
import ptolemy.vergil.tree.EntityTreeModel;
import ptolemy.vergil.tree.PTree;
import ptolemy.vergil.tree.VisibleTreeModel;

/**
 */
public class SuggestCompatibleOperation {

	// //////////////////////////////////////////////////////////////////////
	// PUBLIC MEMBERS

	public static int OUTPUT = 1;
	public static int INPUT = 2;
	public static int COMPONENTS = 3;

	// //////////////////////////////////////////////////////////////////////
	// PUBLIC CONSTRUCTORS

	/**
	 * Constructor
	 */
	public SuggestCompatibleOperation(Frame owner, NamedObj entity, int op) {
		_owner = owner;
		_entity = entity;
		// check for op type
		if (op == OUTPUT)
			_suggestOutput();
		else if (op == INPUT)
			_suggestInput();
		else if (op == COMPONENTS)
			_suggestComponents();
	}

	// //////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS

	/**
	 * Private method for performing output operation
	 */
	private void _suggestOutput() {
		// make sure output types exist
		if (SMSServices.getAllOutputSemanticTypes(_entity).size() == 0) {
			String msg = "No output semantic type annotations defined"
					+ " for this component.";
			JOptionPane.showMessageDialog(_owner, msg,
					"Suggest Compatible Outputs", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// do the input search
		_searchForInput();
		if (_components.size() == 0) {
			String msg = "No compatible components found in actor library"
					+ " for output types.";
			JOptionPane.showMessageDialog(_owner, msg,
					"Suggest Compatible Outputs", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// display results (if any)
		_displayResults();
	}

	/**
	 * Private method for performing input operation
	 */
	private void _suggestInput() {
		// make sure output types exist
		if (SMSServices.getAllInputSemanticTypes(_entity).size() == 0) {
			String msg = "No input semantic type annotations defined for"
					+ " this component.";
			JOptionPane.showMessageDialog(_owner, msg,
					"Suggest Compatible Outputs", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// do the input search
		_searchForOutput();
		if (_components.size() == 0) {
			String msg = "No compatible components found in actor library"
					+ " for output types.";
			JOptionPane.showMessageDialog(_owner, msg,
					"Suggest Compatible Outputs", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// display results (if any)
		_displayResults();
	}

	/**
	 * Private method for performing component operation
	 */
	private void _suggestComponents() {
		// make sure the actor has at least a semantic type (category or port)
		if (SMSServices.getAllInputSemanticTypes(_entity).size() == 0
				&& SMSServices.getAllOutputSemanticTypes(_entity).size() == 0
				&& SMSServices.getActorSemanticTypes(_entity).size() == 0) {
			String msg = "No semantic type annotations are defined"
					+ " for this component.";
			JOptionPane.showMessageDialog(_owner, msg,
					"Suggest Compatible Outputs", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// do the input search
		_searchForComponents();
		if (_components.size() == 0) {
			String msg = "No compatible components found in actor library.";
			JOptionPane.showMessageDialog(_owner, msg,
					"Suggest Compatible Outputs", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// display results (if any)
		_displayResults();
	}

	/**
	 * Private method for performing search for matching input
	 * 
	 * @param semTypes
	 *            a set of semantic types to search for
	 * @return a set of matching components
	 */
	private void _searchForInput() {
		// build hashtable of port types
		_buildOutputPortMgr();
		// get the list of components
		_buildInputComponents();
		// match component types to those in port manager
		_filterInputComponents();
	}

	/**
	 * Private method for performing search for matching input
	 * 
	 * @param semTypes
	 *            a set of semantic types to search for
	 * @return a set of matching components
	 */
	private void _searchForOutput() {
		// build hashtable of port types
		_buildInputPortMgr();
		// get the list of components
		_buildOutputComponents();
		// match component types to those in port manager
		_filterOutputComponents();
	}

	private void _searchForComponents() {
		// find components with compatible semantic type categorizations
		_buildSimilarComponents();
		_filterSimilarComponents();
	}

	/**
	 * Private method to build a hash table of ports (keys) and their semantic
	 * types (as named ont classes)
	 */
	private void _buildOutputPortMgr() {
		Iterator portIter = SMSServices.getAllOutputPorts(_entity).iterator();
		while (portIter.hasNext()) {
			Object port = portIter.next();
			_portMgr.addObject(port);
			Iterator typeIter = SMSServices.getPortSemanticTypes(port)
					.iterator();
			while (typeIter.hasNext())
				_portMgr.addType(port, typeIter.next());
		}
		_portMgr.pruneUntypedObjects();
	}

	/**
	 * Private method to build a hash table of ports (keys) and their semantic
	 * types (as named ont classes)
	 */
	private void _buildInputPortMgr() {
		Iterator portIter = SMSServices.getAllInputPorts(_entity).iterator();
		while (portIter.hasNext()) {
			Object port = portIter.next();
			_portMgr.addObject(port);
			Iterator typeIter = SMSServices.getPortSemanticTypes(port)
					.iterator();
			while (typeIter.hasNext())
				_portMgr.addType(port, typeIter.next());
		}
		_portMgr.pruneUntypedObjects();
	}

	/**
	 * Private method to build a list of components to search over. Components
	 * are those within the actor library that are not entity libraries and that
	 * have at least one output semantic type. The result is stored in
	 * _components
	 */
	private void _buildOutputComponents() {
		Iterator<NamedObj> nodes = _getComponents().iterator();
		while (nodes.hasNext()) {
			NamedObj node = nodes.next();
			// check if it has any input semantic types
			if (SMSServices.getAllOutputSemanticTypes(node).size() != 0) {
				if (!_componentExists(node))
					_components.add(node);
			}
		}
	}

	/**
	 * Private method to build a list of components to search over. Components
	 * are those within the actor library that are not entity libraries and that
	 * have at least one input semantic type. The result is stored in
	 * _components
	 */
	private void _buildInputComponents() {
		Iterator<NamedObj> nodes = _getComponents().iterator();
		while (nodes.hasNext()) {
			NamedObj node = nodes.next();
			// check if it has any input semantic types
			if (SMSServices.getAllInputSemanticTypes(node).size() != 0) {
				if (!_componentExists(node))
					_components.add(node);
			}
		}
	}

	/**
	 * Private method to build a list of components to search over. Components
	 * are those within the actor library that are not entity libraries and that
	 * have at least one input, output, or category semantic type. The result is
	 * stored in _components
	 */
	private void _buildSimilarComponents() {
		try {
			Iterator<NamedObj> nodes = _getComponents().iterator();
			while (nodes.hasNext()) {
				NamedObj node = nodes.next();
				// check if it has any input semantic types
				if (SMSServices.getActorSemanticTypes(node).size() != 0
						|| SMSServices.getAllInputSemanticTypes(node).size() != 0
						|| SMSServices.getAllOutputSemanticTypes(node).size() != 0) {
					if (!_componentExists(node))
						_components.add(node);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Private method that retrieves the components in the library as
	 * NamedObj's, i.e., actual, instantiated actors.
	 */
	private Vector<NamedObj> _getComponents() {
		Vector<NamedObj> result = new Vector<NamedObj>();
		try {

			CacheManager manager = CacheManager.getInstance();
			Vector<KeplerLSID> cachedLsids = manager.getCachedLsids();
			for (KeplerLSID lsid : cachedLsids) {
				try{
					ActorCacheObject aco = (ActorCacheObject) CacheManager
						.getInstance().getObject(lsid);
					ActorMetadata am = aco.getMetadata();
					GraphicalActorMetadata gam = new GraphicalActorMetadata(am);
					NamedObj node = gam.getActorAsNamedObj(null);
					result.add(node);
				}
				catch(ClassCastException cce){
					// can no longer assume all items in library can be cast
					// to ActorCacheObject. just skip these.
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Checks for like named components
	 */
	private boolean _componentExists(NamedObj obj) {
		String str = obj.getName();
		Iterator<NamedObj> compIter = _components.iterator();
		while (compIter.hasNext()) {
			NamedObj comp = compIter.next();
			if (str.equals(comp.getName()))
				return true;
		}
		return false;
	}

	/**
	 * This is the main algorithm for checking. Looks at output ports
	 * individually, and checks if any input ports are supertypes. Note this
	 * could be further generalized to support additional types of checking,
	 * e.g., across output/input ports.
	 */
	private void _filterOutputComponents() {
		Vector<NamedObj> results = new Vector<NamedObj>();

		// nothing to filter
		if (_components.size() == 0)
			return;

		Iterator<NamedObj> compIter = _components.iterator();
		while (compIter.hasNext()) {
			boolean comp_is_match = false;
			NamedObj comp = compIter.next();
			// for each input port, check comp output ports
			Iterator inPortIter = _portMgr.getObjects().iterator();
			while (!comp_is_match && inPortIter.hasNext()) {
				// get the input sem types
				Vector inSemTypes = _portMgr.getTypes(inPortIter.next());
				// get the output ports
				Iterator outPortIter = SMSServices.getAllOutputPorts(
						comp).iterator();
				while (!comp_is_match && outPortIter.hasNext()) {
					// get the input sem types
					Vector outSemTypes = SMSServices
							.getPortSemanticTypes(outPortIter.next());
					if (SMSServices.compare(outSemTypes, inSemTypes) == SMSServices.COMPATIBLE) {
						comp_is_match = true;
						results.add(comp);
					}
				}
			}
		}
		_components = results;
	}

	/**
	 * This is the main algorithm for checking. Looks at output ports
	 * individually, and checks if any input ports are supertypes. Note this
	 * could be further generalized to support additional types of checking,
	 * e.g., across output/input ports.
	 */
	private void _filterInputComponents() {
		Vector<NamedObj> results = new Vector<NamedObj>();

		// nothing to filter
		if (_components.size() == 0)
			return;

		Iterator<NamedObj> compIter = _components.iterator();
		while (compIter.hasNext()) {
			boolean comp_is_match = false;
			NamedObj comp = compIter.next();
			// for each output port, check comp input ports
			Iterator outPortIter = _portMgr.getObjects().iterator();
			while (!comp_is_match && outPortIter.hasNext()) {
				// get the output sem types
				Vector outSemTypes = _portMgr.getTypes(outPortIter.next());
				// get the input ports
				Iterator inPortIter = SMSServices.getAllInputPorts(
						comp).iterator();
				while (!comp_is_match && inPortIter.hasNext()) {
					// get the input sem types
					Vector inSemTypes = SMSServices
							.getPortSemanticTypes(inPortIter.next());
					if (SMSServices.compare(outSemTypes, inSemTypes) == SMSServices.COMPATIBLE) {
						comp_is_match = true;
						results.add(comp);
					}
				}
			}
		}
		_components = results;
	}

	private void _filterSimilarComponents() {
		if (SMSServices.getActorSemanticTypes(_entity).size() != 0)
			_filterSimilarComponentType();
		if (SMSServices.getAllInputSemanticTypes(_entity).size() != 0)
			_filterSimilarComponentInput();
		if (SMSServices.getAllOutputSemanticTypes(_entity).size() != 0)
			_filterSimilarComponentOutput();
	}

	private void _filterSimilarComponentType() {
		Vector<NamedObj> results = new Vector<NamedObj>();
		// nothing to filter
		if (_components.size() == 0)
			return;
		Iterator<NamedObj> compIter = _components.iterator();
		while (compIter.hasNext()) {
			boolean comp_is_match = false;
			NamedObj comp = compIter.next();
			Vector subTypes = SMSServices.getActorSemanticTypes(comp);
			Iterator typeIter = SMSServices.getActorSemanticTypes(_entity)
					.iterator();
			while (!comp_is_match && typeIter.hasNext()) {
				Vector superTypes = new Vector();
				superTypes.add(typeIter.next());
				if (SMSServices.compare(subTypes, superTypes) == SMSServices.COMPATIBLE) {
					comp_is_match = true;
					results.add(comp);
				}
			}
		}
		_components = results;
	}

	private void _filterSimilarComponentInput() {
		Vector<NamedObj> results = new Vector<NamedObj>();
		// nothing to filter
		if (_components.size() == 0)
			return;
		SemanticTypeManager manager = _inputPortManager();
		Iterator<NamedObj> compIter = _components.iterator();
		while (compIter.hasNext()) {
			boolean comp_is_match = false;
			NamedObj comp = compIter.next();
			// for each output port, check comp input ports
			Iterator portIter = manager.getObjects().iterator();
			while (!comp_is_match && portIter.hasNext()) {
				// get the search item input sem types
				Vector superTypes = manager.getTypes(portIter.next());
				// get the component input ports
				Iterator inPortIter = SMSServices.getAllInputPorts(
						(NamedObj) comp).iterator();
				while (!comp_is_match && inPortIter.hasNext()) {
					// get the input sem types
					Vector subTypes = SMSServices
							.getPortSemanticTypes(inPortIter.next());
					if (SMSServices.compare(subTypes, superTypes) == SMSServices.COMPATIBLE) {
						comp_is_match = true;
						results.add(comp);
					}
				}
			}
		}
		_components = results;
	}

	private void _filterSimilarComponentOutput() {
		Vector<NamedObj> results = new Vector<NamedObj>();
		// nothing to filter
		if (_components.size() == 0)
			return;
		SemanticTypeManager manager = _outputPortManager();
		Iterator<NamedObj> compIter = _components.iterator();
		while (compIter.hasNext()) {
			boolean comp_is_match = false;
			NamedObj comp = compIter.next();
			// for each output port, check comp input ports
			Iterator portIter = manager.getObjects().iterator();
			while (!comp_is_match && portIter.hasNext()) {
				// get the search item input sem types
				Vector subTypes = manager.getTypes(portIter.next());
				// get the component output ports
				Iterator outPortIter = SMSServices.getAllOutputPorts(
						(NamedObj) comp).iterator();
				while (!comp_is_match && outPortIter.hasNext()) {
					// get the output sem types
					Vector superTypes = SMSServices
							.getPortSemanticTypes(outPortIter.next());
					if (SMSServices.compare(subTypes, superTypes) == SMSServices.COMPATIBLE) {
						comp_is_match = true;
						results.add(comp);
					}
				}
			}
		}
		_components = results;
	}

	private SemanticTypeManager _inputPortManager() {
		SemanticTypeManager manager = new SemanticTypeManager();
		Iterator portIter = SMSServices.getAllInputPorts(_entity).iterator();
		while (portIter.hasNext()) {
			Object port = portIter.next();
			manager.addObject(port);
			Iterator typeIter = SMSServices.getPortSemanticTypes(port)
					.iterator();
			while (typeIter.hasNext())
				manager.addType(port, typeIter.next());
		}
		manager.pruneUntypedObjects();
		return manager;
	}

	private SemanticTypeManager _outputPortManager() {
		SemanticTypeManager manager = new SemanticTypeManager();
		Iterator portIter = SMSServices.getAllOutputPorts(_entity).iterator();
		while (portIter.hasNext()) {
			Object port = portIter.next();
			manager.addObject(port);
			Iterator typeIter = SMSServices.getPortSemanticTypes(port)
					.iterator();
			while (typeIter.hasNext())
				manager.addType(port, typeIter.next());
		}
		manager.pruneUntypedObjects();
		return manager;
	}

	/**
	 * Private method to draw a simple dialog of search matches.
	 */
	private void _displayResults() {
		// the dialog for viewing results
		final JDialog dialog = new JDialog();
		dialog.setTitle("Search Results");

		// overall panel for the dialog
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// set up the button
		JButton closeBtn = new JButton("Close");
		closeBtn.setActionCommand("close");
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("close"))
					dialog.dispose();
			}
		});

		// label
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
		panel1.add(new JLabel("Suggeted Components (drag to canvas):"));
		panel1.add(Box.createHorizontalGlue());

		// close button
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
		panel2.add(Box.createHorizontalGlue());
		panel2.add(closeBtn);

		// add everything to the main panel
		panel.add(panel1);
		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(new JScrollPane(_buildResultTree()));
		panel.add(Box.createRigidArea(new Dimension(0, 10)));
		panel.add(panel2);

		// add main panel to the dialog and show it
		dialog.add(panel);
		dialog.pack();
		dialog.show();
	}

	/**
	 * Construct the visual tree widget showing a list of results.
	 */
	private PTree _buildResultTree() {
		EntityLibrary root = new EntityLibrary();
		Workspace workspace = root.workspace();
		EntityTreeModel model = new VisibleTreeModel(root);

		Iterator<NamedObj> compIter = _components.iterator();
		while (compIter.hasNext()) {
			try {
				NamedObj entity = compIter.next();
				// add to the tree
				NamedObj obj = (NamedObj) entity.clone(workspace);
				if (obj instanceof ComponentEntity)
					((ComponentEntity) obj).setContainer(root);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		PTree resultTree = new PTree(model);
		resultTree.setRootVisible(false);
		resultTree.setMinimumSize(new Dimension(120, 100));
		resultTree.setMaximumSize(new Dimension(120, 100));

		return resultTree;
	}

	// //////////////////////////////////////////////////////////////////////////////
	// PRIVATE MEMBERS

	private Frame _owner;
	private NamedObj _entity;
	private int _op;

	private Vector<NamedObj> _components = new Vector<NamedObj>();
	private SemanticTypeManager _portMgr = new SemanticTypeManager();

}