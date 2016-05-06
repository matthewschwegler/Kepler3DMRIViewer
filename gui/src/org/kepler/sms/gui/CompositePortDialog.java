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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.kepler.sms.KeplerCompositeIOPort;
import org.kepler.sms.KeplerVirtualIOPort;
import org.kepler.sms.SMSServices;

import ptolemy.actor.IOPort;
import ptolemy.kernel.Entity;

public class CompositePortDialog extends JDialog {

	private KeplerCompositeIOPort _result = null;

	/**
     * 
     */
	protected KeplerCompositeIOPort getChoice() {
		// System.out.println(">>> RESULT :=");
		// System.out.println(_result.exportMoML());
		return _result;
	}

	/**
	 * Note: Only entities may have ports
	 */
	public static KeplerCompositeIOPort showDialog(Frame aFrame, Entity entity,
			int direction) {
		CompositePortDialog d = new CompositePortDialog(aFrame, entity,
				direction);
		return d.getChoice();
	}

	/**
     *
     */
	protected CompositePortDialog(Frame aFrame, Entity entity, int direction) {
		super(aFrame, true);
		_direction = direction;

		if (_direction == PortSemanticTypeEditorPane.INPUT)
			setTitle("Create New Input Composite Port");
		else if (_direction == PortSemanticTypeEditorPane.OUTPUT)
			setTitle("Create New Output Composite Port");

		_entity = entity;

		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		pane.add(_createPortName());
		pane.add(Box.createRigidArea(new Dimension(0, 5)));
		pane.add(_createPortList());
		pane.add(Box.createRigidArea(new Dimension(0, 5)));
		pane.add(_createButtons());

		_loadPortList();

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
	private JPanel _createPortName() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

		// name panel
		JPanel namePane = new JPanel();
		namePane.setLayout(new BoxLayout(namePane, BoxLayout.X_AXIS));
		pane.add(Box.createRigidArea(new Dimension(10, 0)));
		if (_direction == PortSemanticTypeEditorPane.INPUT)
			namePane.add(new JLabel("Composite Input Port Name:",
					SwingConstants.LEFT));
		else if (_direction == PortSemanticTypeEditorPane.OUTPUT)
			namePane.add(new JLabel("Composite Output Port Name:",
					SwingConstants.LEFT));
		namePane.add(Box.createHorizontalGlue());
		pane.add(namePane);

		// add the port name box
		_txtPortName = new JTextField();
		_txtPortName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_portNameChanged();
			}
		});
		pane.add(_txtPortName);

		return pane;
	}

	/**
     *
     */
	private JPanel _createPortList() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

		// name panel
		JPanel namePane = new JPanel();
		namePane.setLayout(new BoxLayout(namePane, BoxLayout.X_AXIS));
		pane.add(Box.createRigidArea(new Dimension(10, 0)));
		if (_direction == PortSemanticTypeEditorPane.INPUT)
			namePane.add(new JLabel("Component Input Ports: ",
					SwingConstants.LEFT));
		if (_direction == PortSemanticTypeEditorPane.OUTPUT)
			namePane.add(new JLabel("Component Output Ports: ",
					SwingConstants.LEFT));
		namePane.add(Box.createHorizontalGlue());
		pane.add(namePane);

		// add the port box
		Vector columns = new Vector();
		if (_direction == PortSemanticTypeEditorPane.INPUT)
			columns.add("Input Port Name");
		if (_direction == PortSemanticTypeEditorPane.OUTPUT)
			columns.add("Output Port Name");

		columns.add("Include in Composite");
		_tblPortList = new JTable(new _PortListTableModel());
		_tblPortList.setCellSelectionEnabled(false);
		_tblPortList.setColumnSelectionAllowed(false);
		_tblPortList.setRowSelectionAllowed(false);
		_tblPortList.getModel().addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				_portListChanged();
			}
		});

		JScrollPane scrollPane = new JScrollPane(_tblPortList);
		scrollPane.setPreferredSize(new Dimension(250, 200));
		pane.add(scrollPane);

		return pane;
	}

	private void _loadPortList() {
		Vector ports = new Vector();
		Iterator iter;
		if (_direction == PortSemanticTypeEditorPane.INPUT) {
			// get all the input ports of the entity and a
			iter = SMSServices.getAllInputPorts(_entity).iterator();
			while (iter.hasNext())
				ports.add(iter.next());
		} else if (_direction == PortSemanticTypeEditorPane.OUTPUT) {
			iter = SMSServices.getAllOutputPorts(_entity).iterator();
			while (iter.hasNext())
				ports.add(iter.next());
		}
		iter = ports.iterator();
		while (iter.hasNext())
			_addRow(iter.next());

	}

	/**
     *
     */
	private JPanel _createButtons() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		_btnCreate = new JButton("Create");
		_btnCancel = new JButton("Cancel");

		_btnCreate.setEnabled(false);

		pane.add(_btnCreate);
		pane.add(Box.createRigidArea(new Dimension(15, 0)));
		pane.add(_btnCancel);

		// add listeners to the buttons
		_btnCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				_doCreate();
			}
		});

		_btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				dispose();
			}
		});

		return pane;
	}

	/**
     *
     */
	private void _portNameChanged() {
		_checkEnableCreate();
	}

	/**
     *
     */
	private void _portListChanged() {
		_checkEnableCreate();
	}

	/**
     * 
     */
	private void _addRow(Object obj) {
		_PortListTableModel m = (_PortListTableModel) _tblPortList.getModel();
		if (obj instanceof IOPort) {
			Object[] row = { new _IOPortWrapper((IOPort) obj), Boolean.FALSE };
			m.addRow(row);
		} else {
			Object[] row = { obj, Boolean.FALSE };
			m.addRow(row);
		}
	}

	/**
     * 
     */
	private void _clearPortList() {
		_PortListTableModel m = (_PortListTableModel) _tblPortList.getModel();
		m.clearRows();
	}

	/**
     *
     */
	private void _checkEnableCreate() {
		if (!_validPortName() || !_validSelection()) {
			_btnCreate.setEnabled(false);
			return;
		}
		_btnCreate.setEnabled(true);
	}

	/**
     *
     */
	private boolean _validPortName() {
		String str = _txtPortName.getText();
		if (str == null || str.trim().equals(""))
			return false;
		return true;
	}

	/**
     *
     */
	private boolean _validSelection() {
		_PortListTableModel m = (_PortListTableModel) _tblPortList.getModel();
		boolean sel = false;
		for (int i = 0; i < m.getRowCount(); i++) {
			if (m.rowSelected(i))
				sel = true;
		}
		if (!sel)
			return false;
		return true;
	}

	/**
     *
     */
	private void _doCreate() {
		try {
			_result = new KeplerCompositeIOPort(_entity, _txtPortName.getText());
			_PortListTableModel m = (_PortListTableModel) _tblPortList
					.getModel();
			for (int i = 0; i < m.getRowCount(); i++) {
				if (m.rowSelected(i)) {
					Object obj = m.getValueAt(i, 0);
					if (obj instanceof _IOPortWrapper) {
						_IOPortWrapper port = (_IOPortWrapper) obj;
						_result.addEncapsulatedPort(port.getPort());
					} else if (obj instanceof KeplerVirtualIOPort)
						_result.addEncapsulatedPort((KeplerVirtualIOPort) obj);
				}
			}
			dispose();
		} catch (Exception e) {
			e.printStackTrace();
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
				return "''";
		}

	};

	private class _PortListTableModel extends AbstractTableModel {
		private String[] columnNames = { "Port Name", "Include in Group" };
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
			if (getColumnName(c).equals("Port Name"))
				return Object.class;
			else if (getColumnName(c).equals("Include in Group"))
				return Boolean.class;
			else
				return Object.class;
		}

		public void setValueAt(Object value, int row, int col) {
			Object[] obj = (Object[]) data.elementAt(row);
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
			if (getColumnName(col).equals("Include in Group"))
				return true;
			return false;
		}

	};

	/** Private members */
	private int _direction; // either INPUT or OUTPUT
	private JTextField _txtPortName;
	private JTable _tblPortList;
	private JButton _btnCreate;
	private JButton _btnCancel;
	private Entity _entity;
}