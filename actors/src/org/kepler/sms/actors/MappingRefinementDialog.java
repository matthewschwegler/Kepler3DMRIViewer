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

import java.awt.Component;
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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import ptolemy.actor.IOPort;
import ptolemy.kernel.util.NamedObj;

public class MappingRefinementDialog extends JDialog {

	/**
	 * Note: Only entities may have ports
	 */
	public static void showDialog(Frame aFrame, MergeActor mergeActor,
			MergeEditorDialog parent) {
		MappingRefinementDialog d = new MappingRefinementDialog(aFrame,
				mergeActor, parent);
	}

	/**
     *
     */
	protected MappingRefinementDialog(Frame aFrame, MergeActor mergeActor,
			MergeEditorDialog parent) {
		super(aFrame, true);
		setTitle("Refine Input-Output Mappings");

		_mergeActor = mergeActor;
		_parent = parent;

		// load all the given mappings for the merge actor
		_loadMappings();

		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		pane.add(_createInputPortList());
		pane.add(Box.createRigidArea(new Dimension(0, 10)));
		pane.add(_createOutputPortList());
		pane.add(Box.createRigidArea(new Dimension(0, 10)));
		pane.add(_createButtons());

		// set up the dialog
		this.setContentPane(pane);
		this.setResizable(false);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.pack();
		this.show();
	}

	/**
     *
     */
	private JPanel _createInputPortList() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

		// get the list of actors
		Vector actors = new Vector();
		Iterator iter = _mergeActor.getActors().iterator();
		while (iter.hasNext()) {
			NamedObj obj = (NamedObj) iter.next();
			actors.add(new _ActorWrapper(obj));
		}

		_cbActors = new JComboBox(actors);
		_cbActors.setEditable(false);
		_cbActors.setSelectedIndex(-1);
		_cbActors.setPreferredSize(new Dimension(250, 20));
		_cbActors.setMaximumSize(new Dimension(250, 20));
		_cbActors.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_actorChoiceChanged();
			}
		});
		pane.add(_labelComponent("Source Actor", _cbActors, 250, 50));
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		_cbPorts = new JComboBox();
		_cbPorts.setEditable(false);
		_cbPorts.setSelectedIndex(-1);
		_cbPorts.setPreferredSize(new Dimension(250, 20));
		_cbPorts.setMaximumSize(new Dimension(250, 20));
		_cbPorts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_portChoiceChanged();
			}
		});
		pane.add(_labelComponent("Source Actor Port", _cbPorts, 250, 50));

		return pane;
	}

	/**
     *
     */
	private JPanel _createOutputPortList() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

		_tblPortList = new JTable(_model);
		_tblPortList.setCellSelectionEnabled(false);
		_tblPortList.setColumnSelectionAllowed(false);
		_tblPortList.setRowSelectionAllowed(false);
		_tblPortList.setShowGrid(false);
		Iterator<IOPort> iter = _parent.getTargetPorts().iterator();
		while (iter.hasNext()) {
			IOPort p = iter.next();
			_model.insertRow(new _IOPortWrapper(p), new Boolean(false));
		}
		JScrollPane view = new JScrollPane(_tblPortList,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		view.setPreferredSize(new Dimension(260, 150));
		view.setMaximumSize(new Dimension(260, 150));
		pane.add(view);

		return pane;
	}

	/**
     *
     */
	private JPanel _createButtons() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		_btnOK = new JButton("OK");
		_btnCancel = new JButton("Cancel");

		pane.add(Box.createHorizontalGlue());
		pane.add(_btnOK);
		pane.add(Box.createRigidArea(new Dimension(15, 0)));
		pane.add(_btnCancel);

		// add listeners to the buttons
		_btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				_doOK();
			}
		});

		_btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				dispose();
			}
		});

		return pane;
	}

	public void _actorChoiceChanged() {
		_ActorWrapper w = (_ActorWrapper) _cbActors.getSelectedItem();
		if (w == null)
			return;
		NamedObj actor = w.getActor();
		// clear the old ports
		_cbPorts.removeAllItems();
		// populate the port combo box
		Iterator<IOPort> ports = _mergeActor.getActorPorts(actor).iterator();
		while (ports.hasNext()) {
			IOPort p = ports.next();
			_cbPorts.addItem(new _IOPortWrapper(p));
		}// end while
	}

	public void _portChoiceChanged() {
		// get the actor
		_ActorWrapper aw = (_ActorWrapper) _cbActors.getSelectedItem();
		// get the new port
		_IOPortWrapper pw = (_IOPortWrapper) _cbPorts.getSelectedItem();
		if (aw == null || pw == null)
			return;
		//NamedObj actor = aw.getActor();
		IOPort port = pw.getPort();
		Vector<IOPort> targets = _getCurrentTargetPorts(port);
		for (int i = 0; i < _model.getRowCount(); i++) {
			_IOPortWrapper tw = (_IOPortWrapper) _model.getValueAt(i, 0);
			IOPort target = tw.getPort();
			if (targets.contains(target))
				_model.setValueAt(new Boolean(true), i, 1);
			else
				_model.setValueAt(new Boolean(false), i, 1);
		}// end for
	}

	private Vector<IOPort> _getCurrentTargetPorts(IOPort port) {
		Vector<IOPort> results = new Vector<IOPort>();
		String actorName = port.getContainer().getName();
		String portName = port.getName();
		Iterator<SimpleMergeMapping> maps = _mappings.iterator();
		while (maps.hasNext()) {
			SimpleMergeMapping m = maps.next();
			if (actorName.equals(m.getSourceActor())
					&& portName.equals(m.getSourceActorPort())) {
				String outName = m.getTargetPort();
				Iterator<IOPort> ports = _parent.getTargetPorts().iterator();
				while (ports.hasNext()) {
					IOPort p = ports.next();
					if (outName.equals(p.getName()))
						results.add(p);
				}// end while
			}
		}// end while
		return results;
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

	private void _loadMappings() {
		Iterator<SimpleMergeMapping> iter = _parent.getMappings().iterator();
		while (iter.hasNext()) {
			SimpleMergeMapping mapping = iter.next();
			String actor = mapping.getSourceActor();
			String actorPort = mapping.getSourceActorPort();
			String target = mapping.getTargetPort();
			try {
				SimpleMergeMapping m = new SimpleMergeMapping(actor, actorPort,
						target);
				_mappings.add(m);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}// end while
	}

	private void _addMapping(IOPort target) {
		_ActorWrapper aw = (_ActorWrapper) _cbActors.getSelectedItem();
		_IOPortWrapper pw = (_IOPortWrapper) _cbPorts.getSelectedItem();
		if (aw == null || pw == null)
			return;
		NamedObj actor = aw.getActor();
		IOPort port = pw.getPort();
		try {
			SimpleMergeMapping m = new SimpleMergeMapping(actor.getName(), port
					.getName(), target.getName());
			if (!_mappings.contains(m))
				_mappings.add(m);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void _removeMapping(IOPort target) {
		_ActorWrapper aw = (_ActorWrapper) _cbActors.getSelectedItem();
		_IOPortWrapper pw = (_IOPortWrapper) _cbPorts.getSelectedItem();
		if (aw == null || pw == null)
			return;
		NamedObj actor = aw.getActor();
		IOPort port = pw.getPort();
		try {
			SimpleMergeMapping m = new SimpleMergeMapping(actor.getName(), port
					.getName(), target.getName());
			_mappings.remove(m);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
     *
     */
	private void _doOK() {
		// we don't care about the current one ...
		// we want to look at all and check that they each have targets
		Iterator actors = _mergeActor.getActors().iterator();
		while (actors.hasNext()) {
			NamedObj actor = (NamedObj) actors.next();
			String actorName = actor.getName();
			Iterator<IOPort> ports = _mergeActor.getActorPorts(actor).iterator();
			while (ports.hasNext()) {
				IOPort port = ports.next();
				String portName = port.getName();
				// check if well formed
				if (!_hasTarget(actorName, portName)) {
					String msg = "Actor '" + actorName
							+ "' is missing an output port target for "
							+ "port '" + portName + "'";
					JOptionPane.showMessageDialog(this, msg, "Message",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			}// end while
			if (!_hasAllowableMappings(actor)) {
				String msg = "Multiple ports for Actor '" + actorName
						+ "' are mapped " + "to the same output port target";
				JOptionPane.showMessageDialog(this, msg, "Message",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		// commit the mappings (prune unused ports, etc.)
		_doCommit();
	}

	private void _doCommit() {
		// copy the parent mappings
		Vector parentMappings = new Vector();
		Iterator iter = _parent.getMappings().iterator();
		while (iter.hasNext())
			parentMappings.add(iter.next());

		// keep the same mappings in mergeActor, delete the "old ones"
		iter = parentMappings.iterator();
		while (iter.hasNext()) {
			try {
				SimpleMergeMapping m = (SimpleMergeMapping) iter.next();
				SimpleMergeMapping mcurr = _getMapping(m.getSourceActor(), m
						.getSourceActorPort(), m.getTargetPort());
				if (mcurr == null)
					_parent.removeMapping(m); // this mapping was removed in the
												// refinement
				else
					_mappings.remove(mcurr); // this mapping existed, so remove
												// from current
			} catch (Exception e) {
				e.printStackTrace();
			}
		}// end while

		// add the rest of the current mappings to mergeActor
		iter = _mappings.iterator();
		while (iter.hasNext()) {
			SimpleMergeMapping m = (SimpleMergeMapping) iter.next();
			_parent.addMapping(m);
		}
		dispose();
	}

	/**
	 * @return A mapping from the local mappings that matches the input
	 */
	private SimpleMergeMapping _getMapping(String actor, String port,
			String target) {
		Iterator iter = _mappings.iterator();
		while (iter.hasNext()) {
			SimpleMergeMapping m = (SimpleMergeMapping) iter.next();
			if (actor.equals(m.getSourceActor())
					&& port.equals(m.getSourceActorPort())
					&& target.equals(m.getTargetPort()))
				return m;
		}// end while
		return null;
	}

	private boolean _hasTarget(String actorName, String portName) {
		Iterator mappings = _mappings.iterator();
		boolean found = false;
		while (mappings.hasNext()) {
			SimpleMergeMapping m = (SimpleMergeMapping) mappings.next();
			if (actorName.equals(m.getSourceActor())
					&& portName.equals(m.getSourceActorPort())) {
				found = true;
				break;
			}
		}// end while
		return found;
	}

	/**
	 * Given one input and one output port, determines whether they can be an
	 * input-output mapping according to the existing mappings. Currently, the
	 * only mapping not allowed is when the an actor has multiple output ports
	 * mapped to the same target port. Note that a mapping is different than a
	 * "match", which is between input ports (e.g., of different source actors).
	 */
	private boolean _hasAllowableMappings(NamedObj actor) {
		// get all the output port names for the actor
		Vector<String> ports = new Vector<String>();
		Iterator<IOPort> iter = _mergeActor.getActorPorts(actor).iterator();
		while (iter.hasNext()) {
			IOPort p = iter.next();
			ports.add(p.getName());
		}// end while

		// iterator through the target ports, obtaining their mappings
		iter = _parent.getTargetPorts().iterator();
		while (iter.hasNext()) {
			IOPort target = iter.next();
			// store the actor input ports for the target
			Vector<String> targetInputs = new Vector<String>();
			Iterator<SimpleMergeMapping> mappings = _mappings.iterator();
			while (mappings.hasNext()) {
				SimpleMergeMapping m = mappings.next();
				if (m.getTargetPort().equals(target.getName())
						&& m.getSourceActor().equals(actor.getName()))
					targetInputs.add(m.getSourceActorPort());
			}
			// check if multiple target inputs
			if (targetInputs.size() > 1)
				return false;
		}// end while

		return true;
	}

	private void _printMappings() {
		Iterator iter = _mappings.iterator();
		while (iter.hasNext()) {
			SimpleMergeMapping m = (SimpleMergeMapping) iter.next();
			String actor = m.getSourceActor();
			String actorPort = m.getSourceActorPort();
			String target = m.getTargetPort();
			System.out.println("... mapping: " + actor + ", " + actorPort
					+ ", " + target);
		}
	}

	/**
     *
     */
	private class _IOPortWrapper {
		private IOPort _port;

		public _IOPortWrapper(IOPort port) {
			_port = port;
		}

		public IOPort getPort() {
			return _port;
		}

		public String toString() {
			if (_port != null)
				return _port.getName();
			else
				return "";
		}
	};

	/**
     *
     */
	private class _ActorWrapper {
		private NamedObj _actor;

		public _ActorWrapper(NamedObj actor) {
			_actor = actor;
		}

		public NamedObj getActor() {
			return _actor;
		}

		public String toString() {
			if (_actor != null)
				return _actor.getName();
			else
				return "";
		}
	};

	private class _PortListTableModel extends AbstractTableModel {
		private String[] columnNames = { "Output Port", "Target" };
		private Vector data = new Vector();

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return data.size();
		}

		public String getColumnName(int col) {
			return columnNames[col];
		}

		public void insertRow(_IOPortWrapper p, Boolean checked) {
			Object[] row = new Object[2];
			row[0] = p;
			row[1] = checked;
			addRow(row);
		}

		public void addRow(Object[] row) {
			data.add(row);
			fireTableRowsInserted(getRowCount(), getRowCount());
		}

		public void clearRows() {
			data = new Vector();
			fireTableDataChanged();
		}

		public Object getValueAt(int row, int col) {
			Object[] obj = (Object[]) data.elementAt(row);
			return obj[col];
		}

		/*
		 * JTable uses this method to determine the default renderer/ editor for
		 * each cell. If we didn't implement this method, then the last column
		 * would contain text ("true"/"false"), rather than a check box.
		 */
		public Class getColumnClass(int c) {
			if (getColumnName(c).equals(columnNames[0]))
				return _IOPortWrapper.class;
			else if (getColumnName(c).equals(columnNames[1]))
				return Boolean.class;
			else
				return Object.class;
		}

		public void setValueAt(Object value, int row, int col) {
			Object[] obj = (Object[]) data.elementAt(row);
			// update the mappings
			if (col == 1) {
				_IOPortWrapper w = (_IOPortWrapper) obj[0];
				IOPort p = (IOPort) w.getPort();
				if (((Boolean) value).booleanValue() == true)
					_addMapping(p);
				else if (((Boolean) value).booleanValue() == false)
					_removeMapping(p);
			}
			// change the value
			obj[col] = value;
			fireTableCellUpdated(row, col);
		}

		public boolean rowSelected(int row) {
			Object[] obj = (Object[]) data.elementAt(row);
			Boolean sel = (Boolean) obj[1];
			if (sel.equals(Boolean.TRUE))
				return true;
			return false;
		}

		public boolean isCellEditable(int row, int col) {
			if (getColumnName(col).equals(columnNames[1]))
				return true;
			return false;
		}

	};

	/** Private members */
	private MergeActor _mergeActor;
	private JButton _btnOK;
	private JButton _btnCancel;
	private JComboBox _cbActors;
	private JComboBox _cbPorts;
	private JTable _tblPortList;
	private _PortListTableModel _model = new _PortListTableModel();
	private Vector<SimpleMergeMapping> _mappings = new Vector<SimpleMergeMapping>();
	private MergeEditorDialog _parent;

}