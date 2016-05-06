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
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.kepler.sms.NamedOntClass;
import org.kepler.sms.NamedOntModel;
import org.kepler.sms.OntologyCatalog;
import org.kepler.sms.SemanticType;

import ptolemy.kernel.util.NamedObj;

/**
 * 
 * @author Shawn Bowers
 */
public class SemanticLinkTable extends JPanel {

	/**
	 * This is the default constructor
	 * 
	 * @param owner
	 *            The frame that owns the dialog.
	 */
	public SemanticLinkTable(Frame owner) {
		super();
		_owner = owner;
		_catalog = OntologyCatalog.instance();
		// set up pane
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(_createTable());
		add(Box.createRigidArea(new Dimension(5, 0)));
		add(_createButtons());
		// set size
		setSize(450, 285);
	}

	/**
	 * Add an object to the set of objects being annotated within this panel.
	 * 
	 * @param obj
	 *            The object to add.
	 */
	public void addLinkDomainObject(NamedObj obj) {
		if (obj == null)
			return;

		for (Iterator iter = _linkDomainObjects.iterator(); iter.hasNext();) {
			Object[] entry = (Object[]) iter.next();
			if (entry[0].equals(obj))
				return;
		}
		Object[] entry = new Object[2];
		entry[0] = obj;
		entry[1] = _initializeNamedObj(obj);
		_linkDomainObjects.add(entry);
	}

	/**
	 * @return The set of linkDomain objects being annotated with the panel.
	 */
	public Vector getLinkDomainObjects() {
		Vector result = new Vector();
		for (Iterator iter = _linkDomainObjects.iterator(); iter.hasNext();) {
			Object[] entry = (Object[]) iter.next();
			result.add(entry[0]);
		}
		return result;
	}

	/**
	 * Causes the given object to become visible in the given display. The given
	 * linkDomain object is added if it is not currently one of the linkDomain
	 * objects of the panel.
	 * 
	 * @param obj
	 *            The linkDomain object to make visible.
	 */
	public void setLinkDomainObjectVisible(NamedObj obj) {
		// add it if it doesn't exist
		addLinkDomainObject(obj);
		_SemLinkTableModel model = null;
		for (Iterator iter = _linkDomainObjects.iterator(); iter.hasNext();) {
			Object[] entry = (Object[]) iter.next();
			if (entry[0].equals(obj)) {
				model = (_SemLinkTableModel) entry[1];
				break;
			}
		}
		if (model == null)
			return;
		_table.setModel(model);
	}

	/**
	 * FIXME: Fill in...
	 * 
	 * @return The set of linkDomain objects whose semantic types that have been
	 *         modified.
	 */
	public Vector getModifiedLinkDomainObjects() {
		return getLinkDomainObjects();
	}

	/**
	 * FIXME: Fill in...
	 */
	public boolean commitLinkDomainObjects() {
		return true;
	}

	/**
	 * Initialize the table
	 */
	private JPanel _createTable() {
		// text label
		JPanel txtPane = new JPanel();
		txtPane.setLayout(new BoxLayout(txtPane, BoxLayout.LINE_AXIS));
		txtPane.add(new JLabel("Semantic Links"));
		txtPane.add(Box.createHorizontalGlue());

		// set up the table
		_table = new JTable(new _SemLinkTableModel()); // 0 row and 3 columns
		_table.setPreferredScrollableViewportSize(new Dimension(325, 80));

		// set up where the column draws ontologies from
		TableColumn ontoColumn = _table.getColumnModel().getColumn(1);
		JComboBox comboBox = new JComboBox();
		comboBox.setEditable(true);
		for (Iterator iter = _catalog.getOntologyNames(); iter.hasNext();)
			comboBox.addItem("" + iter.next());
		ontoColumn.setCellEditor(new DefaultCellEditor(comboBox));

		// misc. config.
		_table.setColumnSelectionAllowed(false);
		_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// set up the overall pane
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pane.add(txtPane);
		pane.add(Box.createRigidArea(new Dimension(0, 5)));
		pane.add(new JScrollPane(_table));

		// return the pane
		return pane;
	}

	/**
	 * Initialize the bottom buttons (close)
	 */
	private JPanel _createButtons() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pane.add(Box.createHorizontalGlue());
		pane.add(_addBtn);
		pane.add(Box.createRigidArea(new Dimension(10, 0)));
		pane.add(_removeBtn);
		pane.add(Box.createRigidArea(new Dimension(10, 0)));

		// init buttons
		_addBtn.setActionCommand("add");
		_addBtn.setToolTipText("Add new row for semantic types");
		_addBtn.addActionListener(_buttonListener);
		_addBtn.setEnabled(false);
		_removeBtn.setActionCommand("remove");
		_removeBtn.setToolTipText("Remove selected semantic type");
		_removeBtn.addActionListener(_buttonListener);
		_removeBtn.setEnabled(false);

		return pane;
	}

	/**
	 * anonymous class to handle button events
	 */
	private ActionListener _buttonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("add")) {
				// add a new row to the table
				_SemLinkTableModel model = (_SemLinkTableModel) _table
						.getModel();
				model.insertEmptyRow();
			} else if (e.getActionCommand().equals("remove")) {
				// remove current row
				int rowIndex = _table.getSelectedRow();
				if (rowIndex == -1)
					return;
				_SemLinkTableModel model = (_SemLinkTableModel) _table
						.getModel();
				model.removeRow(rowIndex);
			}
		}
	};

	/**
	 * inner class for managing the table data
	 */
	private class _SemLinkTableModel extends AbstractTableModel {
		// private members
		private String[] tableHeader = { "Ontology", "Property", "Range",
				"Conditions" };
		private Vector tableData = new Vector(); // vector of arrays

		/**
		 * get the column count
		 */
		public int getColumnCount() {
			return tableHeader.length;
		}

		/**
		 * get the row cound
		 */
		public int getRowCount() {
			return tableData.size();
		}

		/**
		 * get the column name
		 */
		public String getColumnName(int columnIndex) {
			return tableHeader[columnIndex];
		}

		/**
		 * get the value of a cell
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object[] row = (Object[]) tableData.elementAt(rowIndex);
			return row[columnIndex];
		}

		/**
		 * everything is editable
		 */
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		/**
		 * sets the value in the column. checks that the term is valid?
		 */
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (!validRowIndex(rowIndex))
				return;
			Object[] row = (Object[]) tableData.elementAt(rowIndex);
			row[columnIndex] = aValue;
			fireTableChanged(new TableModelEvent(this)); // change event
		}

		/**
		 * Insert a new named ontology class into the model
		 */
		public void insertRow(NamedOntClass cls) {
			Object[] row = new Object[2];
			row[1] = cls.getOntologyName();
			row[2] = cls.getName();
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

		/**
		 * @return True if the table contains the given class, and false
		 *         otherwise.
		 */
		public boolean containsRow(NamedOntClass cls) {
			Object[] tmpRow = new Object[2];
			tmpRow[0] = cls.getOntologyName();
			tmpRow[1] = cls.getName();
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
				if (row[0] != null || row[1] != null || row[2] != null)
					results.add(row);
			}
			return results.iterator();
		}

		/**
		 * insert an empty row
		 */
		public void insertEmptyRow() {
			tableData.addElement(new Object[3]);
			fireTableChanged(new TableModelEvent(this)); // change event
		}

		/**
		 * @return the first empty row or -1 if no empties
		 */
		private int firstEmptyRow() {
			int index = 0;
			for (Iterator iter = tableData.iterator(); iter.hasNext(); index++) {
				Object[] row = (Object[]) iter.next();
				if (row[0] == null && row[1] == null && row[2] == null)
					return index;
			}
			return -1;
		}

		/**
		 * @return true if the row already exists
		 */
		private boolean rowExists(Object[] newrow) {
			for (Iterator iter = tableData.iterator(); iter.hasNext();) {
				Object[] row = (Object[]) iter.next();
				if (newrow[0].equals(row[0]) && newrow[1].equals(row[1])
						&& newrow[2].equals(row[2]))
					return true;
			}
			return false;
		}

		/**
		 * remove selected row
		 */
		public void removeRow(int rowIndex) {
			if (!validRowIndex(rowIndex))
				return;
			tableData.removeElementAt(rowIndex);
			fireTableChanged(new TableModelEvent(this)); // change event
		}

		/**
		 * check that we are at a valid row
		 */
		private boolean validRowIndex(int rowIndex) {
			if (rowIndex >= 0 && rowIndex < getRowCount())
				return true;
			return false;
		}
	};

	/**
	 * intializes the table with the named objects current semantic types
	 */
	private _SemLinkTableModel _initializeNamedObj(NamedObj obj) {
		// create a new model
		_SemLinkTableModel model = new _SemLinkTableModel();
		// add rows to it (named ontology classes of the item)
		// for(Iterator iter = _getObjectsNamedOntClasses(obj); iter.hasNext();)
		// {
		// NamedOntClass cls = (NamedOntClass)iter.next();
		// model.insertRow(cls);
		// }
		return model;
	}

	/**
	 * @return A list of all named ontology classes for the current named object
	 *         (if one exists)
	 */
	private Iterator _getObjectsNamedOntClasses(NamedObj obj) {
		Vector result = new Vector();
		if (obj == null)
			return result.iterator();
		for (Iterator iter = obj.attributeList(SemanticType.class).iterator(); iter
				.hasNext();) {
			SemanticType st = (SemanticType) iter.next();
			NamedOntClass cls = _catalog.getNamedOntClass(st);
			if (cls != null)
				result.add(cls);
		}
		return result.iterator();
	}

	/**
	 * Performs the close button operation.
	 */
	// protected boolean _doClose() {
	// for(Iterator iter = getLinkDomainObjects().iterator(); iter.hasNext();) {
	// NamedObj obj = (NamedObj)iter.next();
	// if(_semTypesChanged(obj)) {
	// Object[] options = {"Yes", "No", "Cancel"};
	// String msg = "Would you like to save the changes you made to '" +
	// obj.getName() + "'?";
	// int n = JOptionPane.showOptionDialog(this, msg, "Message",
	// JOptionPane.YES_NO_CANCEL_OPTION,
	// JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
	// if(n == 0 && _saveSemLinks()) {
	// _close();
	// return true;
	// }
	// else if(n ==1) {
	// _close();
	// return true;
	// }
	// return false;
	// }
	// _close();
	// return true;
	// }
	/**
	 * Performs the commit button operation.
	 */
	// protected void _doCommit() {
	// if(_semTypesChanged())
	// _saveSemLinks();
	// }

	/**
	 * Saves the semantic types in the table to the named object.
	 * 
	 * @return True if the semantic types are well-formed and the save
	 *         succeeded, and false otherwise.
	 */
	// private boolean _saveSemLinks() {
	// String msg;
	// if(!_wellFormedSemanticTypes())
	// return false;
	// if(!_accessibleSemanticTypes())
	// return false;
	// // remove current semtypes
	// _removeNamedObjSemtypes();
	// // add new ones (make sure to remove dups)
	// _addSemLinks();
	// // save if in actor library (has id) ... ?
	// return true;
	// }
	/**
	 * Adds the semantic types in the dialog to the entity.
	 */
	private void _addSemLinks(NamedObj obj, _SemLinkTableModel model) {
		Vector result = new Vector();

		for (Iterator iter = model.getRows(); iter.hasNext();) {
			String namespace = "";
			String classname = "";
			// get the row
			Object[] row = (Object[]) iter.next();
			boolean foundModel = false, foundClass = false;
			// search for namespace
			for (Iterator models = _catalog.getNamedOntModels(); models
					.hasNext();) {
				NamedOntModel m = (NamedOntModel) models.next();
				if (row[0].equals(m.getName())) {
					namespace = m.getNameSpace();
					foundModel = true;
					// search for classname
					for (Iterator classes = m.getNamedClasses(); classes
							.hasNext();) {
						NamedOntClass c = (NamedOntClass) classes.next();
						if (row[1].equals(c.getName())) {
							classname = c.getLocalName();
							foundClass = true;
							break;
						}
					}
					break;
				}
			}
			if (!foundModel) {
				namespace = row[0].toString();
				classname = row[1].toString();
			} else if (!foundClass) {
				classname = row[1].toString();
			}
			// create id
			if ((namespace + classname).split("#").length != 2) {
				String[] ns_array = namespace.split("#");
				namespace = "";
				for (int i = 0; i < ns_array.length; i++)
					namespace = namespace + ns_array[i];
				namespace = namespace + "#";
				String[] cl_array = classname.split("#");
				classname = "";
				for (int i = 0; i < cl_array.length; i++)
					classname = classname + cl_array[i];
			}
			String id = namespace + classname;
			if (!result.contains(id))
				result.add(id);
		}
		// add the semantic type
		int i = 0;
		for (Iterator iter = result.iterator(); iter.hasNext();) {
			try {
				SemanticType st = new SemanticType(obj, "semanticType" + i++);
				st.setExpression((String) iter.next());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return True if every semantic type in the table has an ontology value
	 *         and class value.
	 */
	private boolean _wellFormedSemanticTypes(_SemLinkTableModel model) {
		String msg;
		for (Iterator iter = model.getRows(); iter.hasNext();) {
			Object[] row = (Object[]) iter.next();
			if (row[0] == null) {
				msg = "Commit Error: Every semantic type must have an ontology value.";
				JOptionPane.showMessageDialog(this, msg, "Message",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (row[1] == null) {
				msg = "Commit Error: Every semantic type must have a class value.";
				JOptionPane.showMessageDialog(this, msg, "Message",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

	/**
	 * @return True if all the semantic types are "known" to the local version
	 *         of kepler.
	 */
	private boolean _accessibleSemanticTypes(_SemLinkTableModel model) {
		String msg;
		for (Iterator iter = model.getRows(); iter.hasNext();) {
			boolean classFound = false;
			Object[] row = (Object[]) iter.next();
			for (Iterator iter2 = _catalog.getNamedOntModels(); iter2.hasNext();) {
				NamedOntModel ontmodel = (NamedOntModel) iter2.next();
				if (row[0].equals(ontmodel.getName())
						|| row[0].equals(ontmodel.getNameSpace())) {
					for (Iterator iter3 = ontmodel.getNamedClasses(); iter3
							.hasNext();) {
						NamedOntClass cls = (NamedOntClass) iter3.next();
						if (row[1].equals(cls.getName())
								|| row[1].equals(cls.getLocalName()))
							classFound = true;
					}
				}
			}
			if (!classFound) {
				msg = "Unable to find a matching class for at least one semantic type. \n"
						+ "Do you wish to save anyway?";
				int n = JOptionPane.showConfirmDialog(this, msg, "Message",
						JOptionPane.YES_NO_OPTION);
				if (n == 1)
					return false;
				break;
			}
		}
		return true;
	}

	/**
	 * Removes the current semtypes on the named object
	 */
	private void _removeNamedObjSemtypes(NamedObj obj) {
		if (obj == null)
			return;

		Vector semtypes = new Vector();
		for (Iterator iter = obj.attributeList(SemanticType.class).iterator(); iter
				.hasNext();)
			semtypes.add((SemanticType) obj);

		for (Iterator iter = semtypes.iterator(); iter.hasNext();) {
			SemanticType st = (SemanticType) iter.next();
			try {
				st.setContainer(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Really shut the thing down.
	 */
	private void _close() {
	}

	/**
	 * @return True if the semantic types have changed since the last commit;
	 *         false otherwise
	 */
	private boolean _semLinksChanged(NamedObj obj, _SemLinkTableModel model) {
		// each namedObject semantic type should be in the dialog
		Vector types = new Vector();
		for (Iterator iter = _getObjectsNamedOntClasses(obj); iter.hasNext();) {
			NamedOntClass cls = (NamedOntClass) iter.next();
			if (!model.containsRow(cls))
				return true;
			types.add(cls);
		}

		// there shouldn't be more rows
		Vector rows = new Vector();
		for (Iterator iter = model.getRows(); iter.hasNext();)
			rows.add(iter.next());

		return rows.size() != types.size();
	}

	// private members
	private Vector _linkDomainObjects = new Vector();
	private JTable _table = null; // the ontology, class table
	private JButton _addBtn = new JButton("Add"); // button for adding rows to
													// table
	private JButton _removeBtn = new JButton("Remove"); // button removing items
	private Frame _owner; // the owner of this dialog
	private OntologyCatalog _catalog; // holds the ontologies
}