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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import org.kepler.gui.GraphicalActorMetadata;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.sms.SMSServices;
import org.kepler.sms.SemanticType;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.EntityLibrary;
import ptolemy.vergil.tree.EntityTreeModel;
import ptolemy.vergil.tree.PTree;
import ptolemy.vergil.tree.VisibleTreeModel;



/**
 * 
 * @author Shawn Bowers
 */
public class SemanticSearchDialog extends JDialog {
    /**
     * This is the default constructor
     */
    public SemanticSearchDialog(Frame owner, NamedObj namedObj) {
        super(owner);

        _namedObj = namedObj;
        _owner = owner;

        // set title and close behavior
        this.setTitle("Semantic Search");
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // create the frame's pane
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // create the ssearch and result pane
        JPanel bodyPane = new JPanel();
        bodyPane.setLayout(new BoxLayout(bodyPane, BoxLayout.X_AXIS));
        bodyPane.add(_createSearchPane());
        bodyPane.add(Box.createRigidArea(new Dimension(10, 0)));
        bodyPane.add(_createResultPane());

        // add components to pane
        pane.add(bodyPane);
        pane.add(Box.createRigidArea(new Dimension(0, 5)));
        pane.add(_createButtons());

        // pane.add(Box.createRigidArea(new Dimension(0, 5)));
        pane.add(_createStatus());

        // add the pane
        setContentPane(pane);
        // set size
        this.setSize(675, 575);
        // setResizable(false);

    }

    /**
     * Initialize the bottom buttons (close)
     */
    private JPanel _createSearchPane() {
        // initialize "dummy" search objects
        try {
            _searchActor = new TypedAtomicActor();
            _inSearchPort = new TypedIOPort(_searchActor, "input", true, false);
            _outSearchPort = new TypedIOPort(_searchActor, "output", false,
                                             true);
            _actorTable = new SemanticTypeTable(_getFrame(), false);
            _actorTable.addAnnotationObject(_searchActor);
            _actorTable.setAnnotationObjectVisible(_searchActor);
            _actorTable.setEnabled(false);
            _inPortTable = new SemanticTypeTable(_getFrame(), false);
            _inPortTable.addAnnotationObject(_inSearchPort);
            _inPortTable.setAnnotationObjectVisible(_inSearchPort);
            _inPortTable.setEnabled(false);
            _outPortTable = new SemanticTypeTable(_getFrame(), false);
            _outPortTable.addAnnotationObject(_outSearchPort);
            _outPortTable.setAnnotationObjectVisible(_outSearchPort);
            _outPortTable.setEnabled(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // initialize the checkboxes
        _chkActorSearch = new JCheckBox("Actor Semantic Types");
        _chkActorSearch.setSelected(false);
        _chkActorSearch.addItemListener(_checkBoxListener);
        _chkInPortSearch = new JCheckBox("Input Semantic Types");
        _chkInPortSearch.setSelected(false);
        _chkInPortSearch.addItemListener(_checkBoxListener);
        _chkOutPortSearch = new JCheckBox("Output Semantic Types");
        _chkOutPortSearch.setSelected(false);
        _chkOutPortSearch.addItemListener(_checkBoxListener);

        // initialize the panel
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        pane.setBorder(_createTitledBorder("Search Criteria"));

        pane.add(_createSearchComponent(_chkActorSearch, _actorTable));
        pane.add(Box.createRigidArea(new Dimension(0, 5)));
        pane.add(_createSearchComponent(_chkInPortSearch, _inPortTable));
        pane.add(Box.createRigidArea(new Dimension(0, 5)));
        pane.add(_createSearchComponent(_chkOutPortSearch, _outPortTable));

        return pane;
    }

    /**
     * 
     */
    private JPanel _createResultPane() {
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setBorder(_createTitledBorder("Search Results"));

        _resultTree = new PTree(new EntityTreeModel(null));
        _resultTree.setPreferredSize(new Dimension(280, 100));
        JPanel treePane = new JPanel();
        treePane.setLayout(new BoxLayout(treePane, BoxLayout.X_AXIS));
        treePane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        treePane.add(new JScrollPane(_resultTree));

        pane.add(treePane);

        // return
        return pane;
    }

    /**
     * Combines a check box and semantic type table into a single pane.
     */
    private JPanel _createSearchComponent(JCheckBox chk, SemanticTypeTable tbl) {
        // set up the main panel
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        // construct the check box
        JPanel chkPane = new JPanel();
        chkPane.add(Box.createRigidArea(new Dimension(10, 0)));
        chkPane.setLayout(new BoxLayout(chkPane, BoxLayout.X_AXIS));
        chkPane.add(chk);
        chkPane.add(Box.createHorizontalGlue());
        // add the check box
        pane.add(chkPane);
        // add the table
        pane.add(tbl);
        // return the pane
        return pane;
    }

    /**
     * Initialize the bottom buttons (close)
     */
    private JPanel _createButtons() {
        // init buttons
        _btnSearch = new JButton("Search");
        _btnSearch.setActionCommand("search");
        _btnSearch.setToolTipText("Search for matching components");
        _btnSearch.addActionListener(_buttonListener);
        _btnClose = new JButton("Close");
        _btnClose.setActionCommand("close");
        _btnClose.setToolTipText("Close dialog");
        _btnClose.addActionListener(_buttonListener);
        // create the pane
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.LINE_AXIS));
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pane.add(Box.createHorizontalGlue());
        pane.add(_btnSearch);
        pane.add(Box.createRigidArea(new Dimension(10, 0)));
        pane.add(_btnClose);

        return pane;
    }

    /**
     *
     */
    private JPanel _createStatus() {
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));

        _lblQueryStatus = new JLabel("status: ");

        pane.add(Box.createRigidArea(new Dimension(5, 0)));
        pane.add(_lblQueryStatus);
        // statusPane.setPreferredSize(new Dimension(100, 25));
        pane.add(Box.createHorizontalGlue());

        return pane;
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
     * @return The frame of the dialog
     */
    private Frame _getFrame() {
        java.awt.Container c = getParent();
        while (!(c instanceof Frame) && c != null)
            c = c.getParent();
        if (c != null)
            return (Frame) c;
        return null;
    }

    /**
     * Anonymous class to handle check box state changes
     */
    private ItemListener _checkBoxListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Object source = e.getItemSelectable();
                if (source == _chkActorSearch)
                    _actorSearchStateChanged();
                else if (source == _chkInPortSearch)
                    _inPortSearchStateChanged();
                else if (source == _chkOutPortSearch)
                    _outPortSearchStateChanged();
            }
	};

    /**
     * anonymous class to handle button events
     */
    private ActionListener _buttonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("search"))
                    _doSearch();
                else if (e.getActionCommand().equals("close"))
                    _doClose();
            }
	};

    /**
     * Toggles the actor search pane
     */
    private void _actorSearchStateChanged() {
        if (_chkActorSearch.isSelected())
            _actorTable.setEnabled(true);
        else
            _actorTable.setEnabled(false);
    }

    /**
     * Toggles the input port search pane
     */
    private void _inPortSearchStateChanged() {
        if (_chkInPortSearch.isSelected())
            _inPortTable.setEnabled(true);
        else
            _inPortTable.setEnabled(false);
    }

    /**
     * Toggles the out port search pane
     */
    private void _outPortSearchStateChanged() {
        if (_chkOutPortSearch.isSelected())
            _outPortTable.setEnabled(true);
        else
            _outPortTable.setEnabled(false);
    }

    /**
     * Performs the search button operation.
     */
    private void _doSearch() {

        Vector<SemanticType> actorTypes = new Vector<SemanticType>();
        Vector<SemanticType> inTypes = new Vector<SemanticType>();
        Vector<SemanticType> outTypes = new Vector<SemanticType>();

        String msg;
        // check for some search terms; if none then warn
        // if(_chkOutPortSearch.isSelected())
        outTypes = _outPortTable.getSemanticTypes(_outSearchPort);
        // if(_chkInPortSearch.isSelected())
        inTypes = _inPortTable.getSemanticTypes(_inSearchPort);
        // if(_chkActorSearch.isSelected())
        actorTypes = _actorTable.getSemanticTypes(_searchActor);
        if (outTypes.size() == 0 && inTypes.size() == 0
            && actorTypes.size() == 0) {
            msg = "No search criteria specified.";
            JOptionPane.showMessageDialog(this, msg, "Message",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        // make sure the sem types are well formed first!
        if ((msg = _outPortTable.wellFormedSemTypes()) != null) {
            JOptionPane.showMessageDialog(this, msg, "Message",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        } else if ((msg = _inPortTable.wellFormedSemTypes()) != null) {
            JOptionPane.showMessageDialog(this, msg, "Message",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        } else if ((msg = _actorTable.wellFormedSemTypes()) != null) {
            JOptionPane.showMessageDialog(this, msg, "Message",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        // warn about unknown semantic types
        if (_outPortTable.hasUnknownSemTypes()
            || _inPortTable.hasUnknownSemTypes()
            || _actorTable.hasUnknownSemTypes()) {
            msg = "Unable to find a matching ontology class for at least one semantic type. \n"
                + "Do you wish to search anyway?";
            int n = JOptionPane.showConfirmDialog(this, msg, "Message",
                                                  JOptionPane.YES_NO_OPTION);
            if (n == 1)
                return;
        }

        // give notice of executing search, and disable search button
        _lblQueryStatus.setText("status: executing search ");
        _btnSearch.setEnabled(false);

        // create a search task, add listener and run it
        _SearchTask task = new _SearchTask(actorTypes, inTypes, outTypes);
        task.addListener(this);
        Thread thread = new Thread(task);
        thread.start();
    }

    /**
     * Callback for thread
     */
    private void _searchCompletedAction(_SearchTask task) {
        _btnSearch.setEnabled(true);

        Vector<Entity> result = task.getResults();
        _lblQueryStatus.setText("status: found " + result.size() + " results");

        EntityLibrary root = new EntityLibrary();
        Workspace workspace = root.workspace();
        EntityTreeModel model = new VisibleTreeModel(root);

        for (Iterator<Entity> iter = result.iterator(); iter.hasNext();) {
            try {
                NamedObj entity = (NamedObj) iter.next();
                // add to the tree
                NamedObj obj = _clone(entity, workspace);
                if (obj instanceof ComponentEntity) {
                    ((ComponentEntity) obj).setContainer(root);
                    // ChangeRequest request = new MoMLChangeRequest(obj,
                    // "adding object to search result");
                    // obj.requestChange(request);
                    // obj.executeChangeRequests();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        _resultTree.setModel(model);
        _resultTree.setRootVisible(false);

    }

    private NamedObj _clone(NamedObj obj, Workspace workspace) {
        NamedObj result = null;
        try {
            result = (NamedObj) obj.clone(workspace);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * A private inner class to execute query as a separate thread.
     */
    private class _SearchTask implements Runnable {
        private Vector<SemanticType> _actorTypes;
        private Vector<SemanticType> _inputTypes;
        private Vector<SemanticType> _outputTypes;
        private Vector<Entity> _results;
        private Vector<SemanticSearchDialog> _listeners = new Vector<SemanticSearchDialog>();

        /** constructor */
        public _SearchTask(Vector<SemanticType> actorTypes, Vector<SemanticType> inputTypes,
                           Vector<SemanticType> outputTypes) {
            _actorTypes = actorTypes;
            _inputTypes = inputTypes;
            _outputTypes = outputTypes;
        }

        /** to notify dialog when results are obtained */
        public void addListener(SemanticSearchDialog obj) {
            _listeners.add(obj);
        }

        /** executes the query */
        public void run() {
            _results = _doSearch(_actorTypes, _inputTypes, _outputTypes);
            for (Iterator<SemanticSearchDialog> iter = _listeners.iterator(); iter.hasNext();) {
                SemanticSearchDialog dialog = (SemanticSearchDialog) iter
                    .next();
                dialog._searchCompletedAction(this);
            }
        }

        /** retrieve the results */
        public Vector<Entity> getResults() {
            return _results;
        }

    }; // _SearchTask

    /**
     * 
     */
    private synchronized Vector<Entity> _doSearch(Vector<SemanticType> searchActorTypes,
                                          Vector<SemanticType> searchInTypes, 
                                          Vector<SemanticType> searchOutTypes) {
        Vector<Entity> result = new Vector<Entity>();

        Vector<Entity> objects = _getObjectsToSearch();

        // check if there is a match; we know there is at least one
        // semantic type, which is checked in _doSearch() above
        for (Iterator<Entity> iter = objects.iterator(); iter.hasNext();) {
            boolean compatible = true;
            Entity entity = iter.next();

            if (searchActorTypes.size() > 0 && compatible) {
                Vector entityActorTypes = SMSServices
                    .getActorSemanticTypes(entity);
                if (SMSServices.compare(entityActorTypes, searchActorTypes) != SMSServices.COMPATIBLE)
                    compatible = false;
            }

            if (searchInTypes.size() > 0 && compatible) {
                // iterate through the semantic types while still compatible
                for (Iterator<SemanticType> types = searchInTypes.iterator(); types.hasNext()
                         && compatible;) {
                    boolean found = false;
                    Vector<SemanticType> searchInType = new Vector<SemanticType>();
                    searchInType.add(types.next());
                    // iterator through ports until we find a match
                    for (Iterator ports = entity.portList().iterator(); ports
                             .hasNext()
                             && !found;) {
                        IOPort port = (IOPort) ports.next();
                        Vector entityInTypes = SMSServices
                            .getPortSemanticTypes(port);
                        if (SMSServices.compare(searchInType, entityInTypes) == SMSServices.COMPATIBLE)
                            found = true;
                    }
                    // if we didn't find a match for a semtype then we're not
                    // compatible
                    if (!found)
                        compatible = false;
                }
            }

            if (searchOutTypes.size() > 0 && compatible) {
                // iterate through the semantic types while still compatible
                for (Iterator<SemanticType> types = searchOutTypes.iterator(); types
                         .hasNext()
                         && compatible;) {
                    boolean found = false;
                    Vector<SemanticType> searchOutType = new Vector<SemanticType>();
                    searchOutType.add(types.next());
                    // iterator through ports until we find a match
                    for (Iterator ports = entity.portList().iterator(); ports
                             .hasNext()
                             && !found;) {
                        IOPort port = (IOPort) ports.next();
                        Vector entityOutTypes = SMSServices
                            .getPortSemanticTypes(port);
                        if (SMSServices.compare(entityOutTypes, searchOutType) == SMSServices.COMPATIBLE)
                            found = true;
                    }
                    // if we didn't find a match for a semtype then we're not
                    // compatible
                    if (!found)
                        compatible = false;
                }
            }

            if (compatible)
                result.add(entity);
        }

        return result;
    }

	/**
     * 
     */
	private Vector<Entity> _getObjectsToSearch() {
		Vector<Entity> result = new Vector<Entity>();
		try {
			CacheManager manager = CacheManager.getInstance();
			Vector<KeplerLSID> cachedLsids = manager.getCachedLsids();
			for (KeplerLSID lsid : cachedLsids) {
				if (manager.getObject(lsid) instanceof ActorCacheObject){
					ActorCacheObject aco = (ActorCacheObject) manager
						.getObject(lsid);
					ActorMetadata am = aco.getMetadata();
					GraphicalActorMetadata gam = new GraphicalActorMetadata(am);
					NamedObj obj = gam.getActorAsNamedObj(null);
					// relax to just named obj here?
					if (obj instanceof Entity){
						result.add((Entity)obj);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

    /**
     * Performs the cancel button operation.
     */
    private void _doClose() {
        _actorTable.dispose();
        _inPortTable.dispose();
        _outPortTable.dispose();
        dispose();
    }

    /**
     * Really shut the thing down.
     */
    private void _close() {
        dispose();
    }

    private class _ResultTableModel extends AbstractTableModel {
        // private members
        private String[] tableHeader = { "Object Name", "Object Type" }; // two
        // columns
        private Vector tableData = new Vector(); // vector of NamedObj

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
            if (!validRowIndex(rowIndex))
                return null;
            NamedObj obj = (NamedObj) tableData.elementAt(rowIndex);
            if (rowIndex == 0)
                return obj.getName();
            if (rowIndex == 1)
                return _getSimpleName(obj.getClass());
            return null;
        }

        /**
         * results not editable
         */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        /**
         * sets the value in the column. checks that the term is valid?
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            // if(!validRowIndex(rowIndex))
            // return;
            // Object[] row = (Object []) tableData.elementAt(rowIndex);
            // row[columnIndex] = aValue;
            // fireTableChanged(new TableModelEvent(this)); // change event
        }

        /**
         * Insert a new named ontology class into the model
         */
        public void insertRow(NamedObj obj) {
            if (rowExists(obj))
                return;
            tableData.add(obj);
        }

        /**
         * @return True if the table contains the given class, and false
         *         otherwise.
         */
        public boolean containsRow(NamedObj obj) {
            return rowExists(obj);
        }

        /**
         * @return a list of rows of non-empty Object arrays of arity 2
         *         (Object[2])
         */
        public Iterator getRows() {
            return tableData.iterator();
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
        private boolean rowExists(NamedObj obj) {
            return tableData.contains(obj);
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

        /**
         * returns the "simple" name of the class
         */
        private String _getSimpleName(Class c) {
            StringTokenizer st = new StringTokenizer(c.getName(), ".", false);
            while (st.hasMoreTokens()) {
                String str = st.nextToken();
                if (!st.hasMoreTokens())
                    return str;
            }
            return null;
        }

    };

    /**
     * Main method for testing the dialog.
     * 
     * @param args
     *            the arguments to the program
     */
    public static void main(String[] args) {
        try {
            // a composite "wrapper"
            TypedCompositeActor swf = new TypedCompositeActor();

            // windows look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SemanticSearchDialog dialog = new SemanticSearchDialog(null, swf);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Private members */

    private SemanticTypeTable _actorTable; // the sem types for actor search
    // criteria
    private SemanticTypeTable _inPortTable; // the sem types for input
    private SemanticTypeTable _outPortTable; // the sem types for output
    private TypedAtomicActor _searchActor; // dummy actor for attaching search
    // criteria
    private TypedIOPort _inSearchPort; // dummy port for attaching search
    // criteria
    private TypedIOPort _outSearchPort; // dummy port for attaching search
    // criteria
    private JCheckBox _chkActorSearch; // check box for using actor search
    private JCheckBox _chkInPortSearch; // check box for using input search
    private JCheckBox _chkOutPortSearch; // check box for using output search
    private PTree _resultTree; // stores results in the tree
    private JButton _btnSearch; // search btton
    private JButton _btnClose; // close dialog button
    private JLabel _lblQueryStatus; // status of query
    private Frame _owner; // the owner of this dialog
    private NamedObj _namedObj; // the canvas
}