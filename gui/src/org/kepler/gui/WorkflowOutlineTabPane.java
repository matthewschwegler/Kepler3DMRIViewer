/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-14 16:58:50 -0700 (Fri, 14 Sep 2012) $' 
 * '$Revision: 30684 $'
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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.gui.PtolemyFrame;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

public class WorkflowOutlineTabPane extends JPanel implements TabPane {
	private static final Log log = LogFactory
			.getLog(WorkflowOutlineTabPane.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private TableauFrame _frame;
	private String _tabName;

	/** The outline panel object. */
	private WorkflowOutlinePanel _outlinePanel;
	
	/** The search panel. */
	private SearchUIJPanel _searchUIJPanel;
	
	public WorkflowOutlineTabPane() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.TabPane#initializeTab()
	 */
	public void initializeTab() throws Exception {
		if (isDebugging) log.debug("initializeTab()");
		this.setLayout(new BorderLayout());
		
		_initializeSearchPanel();
		add(_searchUIJPanel, BorderLayout.NORTH);
		
		CompositeEntity entity = (CompositeEntity) ((PtolemyFrame) _frame).getModel();
		_outlinePanel = new WorkflowOutlinePanel(entity, true, true, true);
		add(_outlinePanel, BorderLayout.CENTER);
	}	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.TabPane#getParentFrame()
	 */
	public TableauFrame getParentFrame() {
		return _frame;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.TabPane#getTabName()
	 */
	public String getTabName() {
		return _tabName;
	}

	public void setTabName(String tabName) {
		_tabName = tabName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kepler.gui.TabPane#setParentFrame(ptolemy.actor.gui.TableauFrame)
	 */
	public void setParentFrame(TableauFrame parent) {
		_frame = parent;
	}
	
	/** Change the root of the workflow displayed in the tab. */
	public void setWorkflow(CompositeEntity composite) {
	    _outlinePanel.initialize(composite);
	}

	   /** Initialize the search panel. */
    private void _initializeSearchPanel() {
        
        _searchUIJPanel = new SearchUIJPanel();
        _searchUIJPanel.setBorderTitle("Search Workflow");
        
        // add action for search button
        _searchUIJPanel.setSearchAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                _searchUIJPanel.setCancelButtonEnabled(true);
                String term = _searchUIJPanel.getSearchTerm();
                _outlinePanel.search(term.trim());
            }
        });

        // add action for cancel button, and on mac when X
        // is pressed in search field
        _searchUIJPanel.setCancelAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                _searchUIJPanel.setCancelButtonEnabled(false);
                _outlinePanel.search("");
            }
        });

        _searchUIJPanel.init();
    }

	/**
	 * A factory that creates the library panel for the editors.
	 * 
	 *@author Aaron Schultz
	 */
	public static class Factory extends TabPaneFactory {
		/**
		 * Create a factory with the given name and container.
		 * 
		 *@param container
		 *            The container.
		 *@param name
		 *            The name of the entity.
		 *@exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 *@exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a library pane that displays the given library of actors.
		 * 
		 * @return A new LibraryPaneTab that displays the library
		 */
		public TabPane createTabPane(TableauFrame parent) {
			WorkflowOutlineTabPane wotp = new WorkflowOutlineTabPane();
			wotp.setTabName(this.getName());
			return wotp;
		}
	}
}