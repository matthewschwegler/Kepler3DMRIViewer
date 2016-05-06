/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-08-30 14:33:16 -0700 (Thu, 30 Aug 2012) $' 
 * '$Revision: 30578 $'
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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.popups.OutlinePopupListener;
import org.kepler.util.StaticResources;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.MoMLChangeRequest;

public class WorkflowOutlinePanel extends JPanel {
	private static final Log log = LogFactory
			.getLog(WorkflowOutlinePanel.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
	private SubsetWorkflowOutlineTreeModel wotm;

	private JCheckBox togglePorts;
	private JCheckBox toggleRelations;
	private JCheckBox toggleAttributes;
	
	private boolean includeAttributeCheckBox = false;
	private boolean includePortsCheckBox = false;
	private boolean includeRelationsCheckBox = false;
    private MouseListener _mouseListener;

    private AnnotatedPTree _ptree;

    private String _searchTerm;
    
	public WorkflowOutlinePanel(CompositeEntity entity, 
			boolean includeAttributeCheckBox, boolean includePortsCheckBox, 
			boolean includeRelationsCheckBox) {
		
		this.includeAttributeCheckBox = includeAttributeCheckBox;
		this.includePortsCheckBox = includePortsCheckBox;
		this.includeRelationsCheckBox = includeRelationsCheckBox;
		
		initialize(entity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.TabPane#initializeTab()
	 */
	public void initialize(CompositeEntity entity) {
		if (isDebugging) log.debug("initialize()");
		this.setLayout(new BorderLayout());
		this.setBackground(TabManager.BGCOLOR);
		
		wotm = new SubsetWorkflowOutlineTreeModel(entity);

		refreshOutlineTree();
	}
	
	public void refreshOutlineTree() {
		if (isDebugging) {
			log.debug("refreshOutlineTree()");
		}

		this.removeAll();

		_ptree = new AnnotatedPTree(wotm, this);

		_mouseListener = new OutlinePopupListener(_ptree);
		_ptree.setMouseListener(_mouseListener);

		_ptree.setRootVisible(false);
		_ptree.initAnotatedPTree();
		JScrollPane jSP = new JScrollPane(_ptree,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jSP.setPreferredSize(new Dimension(200, 200));

		this.add(jSP, BorderLayout.CENTER);
		
		setControls();

		this.repaint();
		this.validate();

		// if panel was showing the results of a search, redo the search
		if(_searchTerm != null && !_searchTerm.isEmpty()) {
		    search(_searchTerm);
		}
	}

	/** Display the subset of the workflow for entities containing the given term.
	 *  To display everything set the term to the empty string.
	 */
	public void search(String searchTerm) {

	    // reset the display
        wotm.showAll();

        // do the search if search term is not empty
	    if(!searchTerm.isEmpty()) {
	        Object root = wotm.getRoot();
	        _searchTree(root, searchTerm);
	        // show only the matches
	        wotm.showSubset();

	    }
	    
	    // issue an empty change request to repaint the tree
        MoMLChangeRequest change = 
                new MoMLChangeRequest(wotm.getRoot(), (NamedObj) wotm.getRoot(), "<group></group>");
        change.setPersistent(false);
        ((NamedObj) wotm.getRoot()).requestChange(change);

        // if the search term was not empty, expand the tree to show
        // the matching nodes
        if(!searchTerm.isEmpty()) {
            // do this in the swing thread so that it occurs after the tree is repainted
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    LibrarySearchResultPane.expandAll(_ptree);
                }
            });
        }
        
        _searchTerm = searchTerm;
        
	}
	
	/** Recursively check if a node and its children contain the search term. */
	private void _searchTree(Object node, String searchTerm) {	   
	    
	    NamedObj namedObj = (NamedObj)node;
	    //System.out.println("searching " + namedObj.getFullName());
	    // see if the name contains the search term; ignore case
	    if(namedObj.getName().toLowerCase().contains(searchTerm.toLowerCase())) {	        
	        //System.out.println(namedObj);
	        wotm.addToSubset(namedObj);
	    }
	    
	    if(!wotm.isLeaf(node)) {
	        for(int i = 0; i < wotm.getChildCount(node); i++) {
	            _searchTree(wotm.getChild(node, i), searchTerm);
	        }
	    }
	}
	
	private void setControls() {

		JPanel controlPanel = new JPanel(new BorderLayout());
		
		if (includePortsCheckBox) {
			togglePorts = new JCheckBox(StaticResources.getDisplayString(
					"outline.showPorts", "Show Ports"));
			togglePorts.setSelected(wotm.includePorts);
			togglePorts.addActionListener(new ToggleListener());
			togglePorts.setBackground(TabManager.BGCOLOR);
			controlPanel.add(togglePorts, BorderLayout.NORTH);
		}

		if (includeAttributeCheckBox) {
			toggleAttributes = new JCheckBox(StaticResources.getDisplayString(
					"outline.showAttributes", "Show Attributes"));
			toggleAttributes.setSelected(wotm.includeAttributes);
			toggleAttributes.addActionListener(new ToggleListener());
			toggleAttributes.setBackground(TabManager.BGCOLOR);
			controlPanel.add(toggleAttributes, BorderLayout.CENTER);

		}

		if (includeRelationsCheckBox) {
			toggleRelations = new JCheckBox(StaticResources.getDisplayString(
					"outline.showRelations", "Show Relations"));
			toggleRelations.setSelected(wotm.includeRelations);
			toggleRelations.addActionListener(new ToggleListener());
			toggleRelations.setBackground(TabManager.BGCOLOR);
			controlPanel.add(toggleRelations, BorderLayout.SOUTH);
		}
		
		controlPanel.setBackground(TabManager.BGCOLOR);
		this.add(controlPanel,BorderLayout.SOUTH);		
	}

	
	private class ToggleListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			if (isDebugging) log.debug("actionPerformed("+e+")");
			if (e.getSource() == togglePorts) {
				if (isDebugging) log.debug("togglePorts");
				if (togglePorts.isSelected()) {
					wotm.includePorts = true;
				} else {
					wotm.includePorts = false;
				}
			} else if (e.getSource() == toggleAttributes) {
				if (isDebugging) log.debug("toggleAttributes");
				if (toggleAttributes.isSelected()) {
					wotm.includeAttributes = true;
				} else {
					wotm.includeAttributes = false;
				}
			} else if (e.getSource() == toggleRelations) {
				if (isDebugging) log.debug("toggleRelations");
				if (toggleRelations.isSelected()) {
					wotm.includeRelations = true;
				} else {
					wotm.includeRelations = false;
				}
			}
			refreshOutlineTree();
		}
	}

}
