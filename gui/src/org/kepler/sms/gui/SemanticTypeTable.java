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
 * SemanticTypeEditor is a dialog for adding and removing semantic types to
 * particular workflow components.
 * 
 * @author Shawn Bowers
 */
public class SemanticTypeTable extends JPanel {

    /**
     * This is the default constructor
     * 
     * @param owner The frame that owns the dialog.
     */
    public SemanticTypeTable(Frame owner, boolean showLabel) {
        super();
        _owner = owner;
        _catalog = OntologyCatalog.instance();
        // set up pane
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(_createTable(showLabel));
        add(Box.createRigidArea(new Dimension(5, 0)));
        add(_createButtons());
        // create a browser
        initialize_browser();
        // set size
        setSize(450, 285);
    }

    /**
     * This is the default constructor
     * 
     * @param owner The frame that owns the dialog.
     */
    public SemanticTypeTable(Frame owner) {
        super();
        _owner = owner;
        _catalog = OntologyCatalog.instance();
        // set up pane
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(_createTable(true));
        add(Box.createRigidArea(new Dimension(5, 0)));
        add(_createButtons());
        // create a browser
        initialize_browser();
        // set size
        setSize(450, 285);
    }

    /**
     * Add an object to the set of objects being annotated within this panel.
     * 
     * @param obj The object to add.
     */
    public void addAnnotationObject(NamedObj obj) {
        if (obj == null)
            return;

        for (Iterator iter = _annotationObjects.iterator(); iter.hasNext();) {
            Object[] entry = (Object[]) iter.next();
            if (entry[0].equals(obj))
                return;
        }
        Object[] entry = new Object[2];
        entry[0] = obj;
        entry[1] = _initializeNamedObj(obj);
        _annotationObjects.add(entry);
    }

    /**
     * Remove the given annotation object from the editor
     */
    public void removeAnnotationObject(NamedObj obj) {
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
    public Vector getAnnotationObjects() {
        Vector result = new Vector();
        for (Iterator iter = _annotationObjects.iterator(); iter.hasNext();) {
            Object[] entry = (Object[]) iter.next();
            result.add(entry[0]);
        }
        return result;
    }

    /**
     * Causes the given object to become visible in the given
     * display. The given annotation object is added if it is not
     * currently one of the annotation objects of the panel.
     * 
     * @param obj The annotation object to make visible.
     */
    public void setAnnotationObjectVisible(NamedObj obj) {
        // add it if it doesn't exist
        addAnnotationObject(obj);
        _SemTypeTableModel model = null;
        for (Iterator iter = _annotationObjects.iterator(); iter.hasNext();) {
            Object[] entry = (Object[]) iter.next();
            if (entry[0].equals(obj)) {
                model = (_SemTypeTableModel) entry[1];
                break;
            }
        }
        if (model == null)
            return;
        _table.setModel(model);
        _setOntologyCombo();
    }

    /**
     * @return True if any annotation objects have been modified.
     */
    public boolean hasModifiedAnnotationObjects() {
        for (Iterator entries = _annotationObjects.iterator(); entries
                 .hasNext();) {
            Object[] entry = (Object[]) entries.next();
            NamedObj obj = (NamedObj) entry[0];
            _SemTypeTableModel model = (_SemTypeTableModel) entry[1];

            // each obj's semantic type should be in the dialog
            Vector types = new Vector();
            for (Iterator iter = obj.attributeList(SemanticType.class)
                     .iterator(); iter.hasNext();) {
                SemanticType semtype = (SemanticType) iter.next();
                NamedOntClass cls = _catalog.getNamedOntClass(semtype);
                if (cls != null && !model.containsRow(cls))
                    return true;
                if (cls == null
                    && !model.containsRow(semtype.getNamespace(), semtype
                                          .getConceptName()))
                    return true;
                types.add(semtype);
            }

            // there shouldn't be more rows
            Vector rows = new Vector();
            for (Iterator iter = model.getRows(); iter.hasNext();)
                rows.add(iter.next());
            if (rows.size() != types.size())
                return true;
        }
        return false;
    }

    /**
     * @return The current types for the object in the table. Each type is
     *         returned as a semantic type.
     */
    public Vector<SemanticType> getSemanticTypes(NamedObj obj) {
        for (Iterator iter = _annotationObjects.iterator(); iter.hasNext();) {
            Object[] entry = (Object[]) iter.next();
            if (entry[0].equals(obj)) {
                _SemTypeTableModel model = (_SemTypeTableModel) entry[1];
                return _getSemanticTypes(model);
            }
        }
        return new Vector<SemanticType>();
    }

    /**
     * Saves the current annotations.
     */
    public void commitAnnotationObjects() {
        for (Iterator entries = _annotationObjects.iterator(); entries
                 .hasNext();) {
            Object[] entry = (Object[]) entries.next();
            NamedObj obj = (NamedObj) entry[0];
            _SemTypeTableModel model = (_SemTypeTableModel) entry[1];
            _removeNamedObjSemtypes(obj);
            // add new ones (make sure to remove dups)
            _addSemTypes(obj, model);
        }
    }

    /**
     * @return The first error if semantic type annotations are not well formed,
     *         and null otherwise.
     */
    public String wellFormedSemTypes() {
        for (Iterator entries = _annotationObjects.iterator(); entries
                 .hasNext();) {
            Object[] entry = (Object[]) entries.next();
            NamedObj obj = (NamedObj) entry[0];
            _SemTypeTableModel model = (_SemTypeTableModel) entry[1];

            for (Iterator iter = model.getRows(); iter.hasNext();) {
                Object[] row = (Object[]) iter.next();
                if (row[0] == null)
                    return "Every semantic type must have an ontology value.";
                if (row[1] == null)
                    return "Every semantic type must have a class value.";
            }
        }
        return null;
    }

    /**
     * @return True if there are any semantic types that are not in the catalog.
     */
    public boolean hasUnknownSemTypes() {
        for (Iterator entities = _annotationObjects.iterator(); entities
                 .hasNext();) {
            Object[] entity = (Object[]) entities.next();
            _SemTypeTableModel model = (_SemTypeTableModel) entity[1];
            for (Iterator rows = model.getRows(); rows.hasNext();) {
                boolean classFound = false;
                Object[] row = (Object[]) rows.next();
                for (Iterator ontModels = _catalog.getNamedOntModels(); ontModels
                         .hasNext();) {
                    NamedOntModel ontModel = (NamedOntModel) ontModels.next();
                    if (row[0].equals(ontModel.getName())
                        || row[0].equals(ontModel.getNameSpace())) {
                        for (Iterator classes = ontModel.getNamedClasses(); classes
                                 .hasNext();) {
                            NamedOntClass cls = (NamedOntClass) classes.next();
                            if (row[1].equals(cls.getName())
                                || row[1].equals(cls.getLocalName()))
                                classFound = true;
                        }
                    }
                }
                if (!classFound)
                    return true;
            } // for
        } // for
        return false;
    }

    /**
     * A hack to make the table turn "on" and "off" by enabling and disabling
     * the buttons. The buttons start off enabled.
     */
    public void setEnabled(boolean isEnabled) {
        _addBtn.setEnabled(isEnabled);
        _removeBtn.setEnabled(isEnabled);
        _browseBtn.setEnabled(isEnabled);
    }

    /**
     * Release the browser.
     */
    public void dispose() {
        _browser.dispose();
    }

    /**
     * Initialize the table
     */
    private JPanel _createTable(boolean showLabel) {
        // text label
        JPanel txtPane = new JPanel();
        txtPane.setLayout(new BoxLayout(txtPane, BoxLayout.LINE_AXIS));
        txtPane.add(new JLabel("Semantic Types"));
        txtPane.add(Box.createHorizontalGlue());

        // set up the table
        _table = new JTable(new _SemTypeTableModel()); // 0 row and 2 columns
        _table.setPreferredScrollableViewportSize(new Dimension(325, 80));

        // set up where the column draws ontologies from
        for (Iterator iter = _catalog.getOntologyNames(); iter.hasNext();)
            _ontoNames.add("" + iter.next());
        _setOntologyCombo();

        // misc. config.
        _table.setColumnSelectionAllowed(false);
        _table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // set up the overall pane
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        if (showLabel)
            pane.add(txtPane);
        pane.add(Box.createRigidArea(new Dimension(0, 5)));
        pane.add(new JScrollPane(_table));

        // return the pane
        return pane;
    }

    /**
     * Initialize and set the ontology combo box to known ontology names.
     */
    private void _setOntologyCombo() {
        // set up where the column draws ontologies from
        TableColumn ontoColumn = _table.getColumnModel().getColumn(0);
        JComboBox comboBox = new JComboBox();
        comboBox.setEditable(true);
        for (Iterator iter = _ontoNames.iterator(); iter.hasNext();)
            comboBox.addItem("" + iter.next());
        ontoColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }

    /**
     * Initialize the bottom buttons (close)
     */
    private JPanel _createButtons() {
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pane.add(Box.createHorizontalGlue());
        pane.add(_browseBtn);
        pane.add(Box.createRigidArea(new Dimension(10, 0)));
        pane.add(_addBtn);
        pane.add(Box.createRigidArea(new Dimension(10, 0)));
        pane.add(_removeBtn);
        pane.add(Box.createRigidArea(new Dimension(10, 0)));

        // init buttons
        // _browseBtn.setMnemonic(KeyEvent.VK_B);
        _browseBtn.setActionCommand("browse");
        _browseBtn.setToolTipText("Start semantic type browser");
        _browseBtn.addActionListener(_buttonListener);
        // _addBtn.setMnemonic(KeyEvent.VK_A);
        _addBtn.setActionCommand("add");
        _addBtn.setToolTipText("Add new row for semantic types");
        _addBtn.addActionListener(_buttonListener);
        // _removeBtn.setMnemonic(KeyEvent.VK_R);
        _removeBtn.setActionCommand("remove");
        _removeBtn.setToolTipText("Remove selected semantic type");
        _removeBtn.addActionListener(_buttonListener);

        return pane;
    }

    /**
     * 
     */
    private void initialize_browser() {
        _browser = new SemanticTypeBrowserDialog(_owner, true); // for classes
        // only
        _browser.setTitle("Semantic Type Browser");
        _browser.addSelectionListener(new _BrowserListener());
    }

    /**
     * Inner class to handle browser selection events
     */
    private class _BrowserListener implements
                                       SemanticTypeBrowserSelectionListener {
        public void valueChanged(SemanticTypeBrowserSelectionEvent e) {
            _SemTypeTableModel model = (_SemTypeTableModel) _table.getModel();
            model.insertRow(e.getOntClass());
        }
    };

    /**
     * anonymous class to handle button events
     */
    private ActionListener _buttonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("browse")) {
                    _browser.setVisible(true);
                } else if (e.getActionCommand().equals("add")) {
                    // add a new row to the table
                    _SemTypeTableModel model = (_SemTypeTableModel) _table
                        .getModel();
                    model.insertEmptyRow();
                } else if (e.getActionCommand().equals("remove")) {
                    // remove current row
                    int rowIndex = _table.getSelectedRow();
                    if (rowIndex == -1)
                        return;
                    _SemTypeTableModel model = (_SemTypeTableModel) _table
                        .getModel();
                    model.removeRow(rowIndex);
                }
            }
	};

    /**
     * inner class for managing the table data
     */
    private class _SemTypeTableModel extends AbstractTableModel {
        // private members
        private String[] tableHeader = { "Ontology", "Class" }; // two columns
        private Vector tableData = new Vector(); // vector of arrays

        /**
         * get the column count
         */
        public int getColumnCount() {
            return 2;
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
            row[0] = cls.getOntologyName();
            row[1] = cls.getName();
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
         * @return True if the table contains the given namespace and concept,
         *         and false otherwise.
         */
        public boolean containsRow(String namespace, String concept) {
            Object[] tmpRow = new Object[2];
            tmpRow[0] = namespace;
            tmpRow[1] = concept;
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

        /**
         * insert an empty row
         */
        public void insertEmptyRow() {
            tableData.addElement(new Object[2]);
            fireTableChanged(new TableModelEvent(this)); // change event
        }

        /**
         * @return the first empty row or -1 if no empties
         */
        public int firstEmptyRow() {
            int index = 0;
            for (Iterator iter = tableData.iterator(); iter.hasNext(); index++) {
                Object[] row = (Object[]) iter.next();
                if (row[0] == null && row[1] == null)
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
                if (newrow[0].equals(row[0]) && newrow[1].equals(row[1]))
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
    private _SemTypeTableModel _initializeNamedObj(NamedObj obj) {
        // create a new model
        _SemTypeTableModel model = new _SemTypeTableModel();

        // get semantic type properties
        for (Iterator iter = obj.attributeList(SemanticType.class).iterator(); iter
                 .hasNext();) {
            SemanticType semType = (SemanticType) iter.next();
            NamedOntClass c = _catalog.getNamedOntClass(semType);
            if (c != null)
                model.insertRow(c);
            else {
                model.insertEmptyRow();
                int row = model.firstEmptyRow();
                model.setValueAt(semType.getNamespace(), row, 0);
                model.setValueAt(semType.getConceptName(), row, 1);
            }
        }
        return model;
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
            semtypes.add((SemanticType) iter.next());

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
     * @return The set of semantic types defined in the model as a set of
     *         concept id strings.
     */
    private Vector<SemanticType> _getSemanticTypes(_SemTypeTableModel model) {
        Vector<SemanticType> result = new Vector<SemanticType>();
        for (Iterator iter = model.getRows(); iter.hasNext();) {
            String namespace = "";
            String classname = "";
            // get the row
            Object[] row = (Object[]) iter.next();
            boolean foundModel = false, foundClass = false;
            // search for namespace
            for (Iterator ontModels = _catalog.getNamedOntModels(); ontModels
                     .hasNext();) {
                NamedOntModel ontModel = (NamedOntModel) ontModels.next();
                if (row[0] != null && row[0].equals(ontModel.getName())) {
                    namespace = ontModel.getNameSpace();
                    foundModel = true;
                    // search for classname
                    for (Iterator classes = ontModel.getNamedClasses(); classes
                             .hasNext();) {
                        NamedOntClass cls = (NamedOntClass) classes.next();
                        if (row[1] != null && row[1].equals(cls.getName())) {
                            classname = cls.getLocalName();
                            foundClass = true;
                            break;
                        }
                    }
                    break;
                }
            }
            if (!foundModel) {
                if (row[0] != null)
                    namespace = row[0].toString();
                if (row[1] != null)
                    classname = row[1].toString();
            } else if (!foundClass) {
                if (row[1] != null)
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
            try {
                String id = namespace + classname;
                SemanticType type = new SemanticType();
                type.setConceptId(id);
                if (!result.contains(type))
                    result.add(type);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Adds the semantic types in the dialog to the entity.
     */
    private void _addSemTypes(NamedObj obj, _SemTypeTableModel model) {
        Vector<SemanticType> result = _getSemanticTypes(model);
        // add the semantic type
        int i = 0;
        for (Iterator<SemanticType> iter = result.iterator(); iter.hasNext();) {
            try {
                SemanticType st = new SemanticType(obj, obj
                                                   .uniqueName("semanticType"));
                st.setExpression((iter.next()).getConceptId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // private members
    private Vector _annotationObjects = new Vector();
    private JTable _table = null; // the ontology, class table
    private JButton _browseBtn = new JButton("Browse"); // button for browsing
    private JButton _addBtn = new JButton("Add"); // button for adding rows to
    // table
    private JButton _removeBtn = new JButton("Remove"); // button removing items
    private Frame _owner; // the owner of this dialog
    private SemanticTypeBrowserDialog _browser; // the browser
    private OntologyCatalog _catalog; // holds the ontologies
    private Vector _ontoNames = new Vector();
}