/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2010-12-23 11:01:04 -0800 (Thu, 23 Dec 2010) $' 
 * '$Revision: 26600 $'
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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.ecoinformatics.seek.ecogrid.EcoGridServicesController;
import org.kepler.gui.PreferencesAction;
import org.kepler.gui.SearchUIJPanel;
import org.kepler.gui.TabPane;
import org.kepler.gui.TabPaneFactory;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.tree.EntityTreeModel;
import ptolemy.vergil.tree.VisibleTreeModel;

/**
 * This class will display a panel for search and search result from ecogrid
 * service.
 * 
 *@author berkley
 *@created February 17, 2005
 */
public class DatasetPanel extends JPanel implements TabPane {

	// declare controls
	// search part panel (above)
	private SearchUIJPanel _searchUIJPanel;
	private QuickSearchAction quickSearchAction;
	private QuickSearchCancelAction cancelQuickSearchAction;
	private JProgressBar _progressBar;
	private JLabel _progressLabel;

	// result part panel
	private CompositeEntity resultsRoot;
	private ResultPanel resultPanel;
	private EcoGridServicesController searchController = null;

	private TableauFrame _frame;

	private String name = "Data";

	/**
	 * Set up the pane with preference size and configuration for search
	 * 
	 *@param controller
	 *            Description of the Parameter
	 *@exception Exception
	 *                Description of the Exception
	 */
	public DatasetPanel(TableauFrame parent) {
		super();
		_frame = parent;
		searchController = EcoGridServicesController.getInstance();
		
		quickSearchAction = new QuickSearchAction(searchController,
				SearchUIJPanel.SEARCH_BUTTON_CAPTION, this);
	}

	/**
	 * Implementation of TabPane getName()
	 */
	public String getTabName() {
		return name;
	}

	/**
	 * Implementation of TabPane setParentFrame(TableauFrame)
	 */
	public void setParentFrame(TableauFrame parent) {
		_frame = parent;
	}

	/**
	 * Implementation of TabPane getParentFrame()
	 */
	public TableauFrame getParentFrame() {
		return _frame;
	}

	/**
	 * Get the search text field value
	 * 
	 *@return The searchTextFieldValue value
	 */
	public String getSearchTextFieldValue() {
		return _searchUIJPanel.getSearchTerm();
	}

	/**
	 * Gets the resultRoot attribute of the DatasetPanel object
	 * 
	 *@return The resultRoot value
	 */
	public CompositeEntity getResultRoot() {
		return resultsRoot;
	}

	/**
	 * Initialize the tab panel
	 * 
	 *@throws Exception
	 */
	public void initializeTab() throws Exception {

		this.setLayout(new BorderLayout());

		// init searchPartPanel
		initSearchPartPanel();

		// add searchPartPanel into this panel top(North)
		this.add(_searchUIJPanel, BorderLayout.NORTH);

		// add empty result scroll panel into this panel bottotm(South)
		resultPanel = new ResultPanel(createResultModel());
		this.add(resultPanel, BorderLayout.CENTER);

		_progressBar = new JProgressBar();
		_progressBar.setVisible(false);

		_progressLabel = new JLabel();
		_progressLabel.setVisible(true);

		JPanel progressPanel = new JPanel(new BorderLayout());
		progressPanel.add(_progressLabel, BorderLayout.NORTH);
		progressPanel.add(_progressBar, BorderLayout.SOUTH);
		this.add(progressPanel, BorderLayout.SOUTH);
		

	}

	/**
	 * Initialize the search field and buttons
	 * 
	 * @throws Exception
	 */
	private void initSearchPartPanel() throws Exception {

		_searchUIJPanel = new SearchUIJPanel();

		_searchUIJPanel.setBorderTitle(
				StaticResources.getDisplayString("data.search", "Search Data"));

		_searchUIJPanel.setSearchAction(quickSearchAction);
		
		//_searchUIJPanel.setSearchAction(new QuickSearchAction(searchController,
		//		SearchUIJPanel.SEARCH_BUTTON_CAPTION, this));

		/*
		_searchUIJPanel.setResetAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				resetResultsPanel();
				_searchUIJPanel.setSearchTerm("");
			}
		});
		*/

		//_searchUIJPanel.setCancelAction(new QuickSearchCancelAction(
		//		quickSearchAction, SearchUIJPanel.CANCEL_BUTTON_CAPTION));
		_searchUIJPanel.setCancelAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				resetResultsPanel();
				_searchUIJPanel.setSearchTerm("");
				quickSearchAction.stop(); //TODO get errors from CacheManager when doing this call. 
										  //Check: can it work both during and post-search?
				_searchUIJPanel.setCancelButtonEnabled(false);
			}
		});

		_searchUIJPanel.setSourceAction(new PreferencesAction("Data"));

		_searchUIJPanel.init();

	}

	/**
	 * Create the tree model used to contain the result set. This is displayed
	 * in the JTree on the left of the window.
	 * 
	 *@return the EntityTreeModel that can contain the results
	 */
	private EntityTreeModel createResultModel() {
		try {
			resultsRoot = new ResultTreeRoot("resultset");
			Attribute libraryMarker = new Attribute(resultsRoot,
					"_libraryMarker");
		} catch (IllegalActionException iae) {
			System.err.println("Could not create entity.");
		} catch (NameDuplicationException nde) {
			System.err.println("An entity with that name already exists.");
		}
		EntityTreeModel resultTreeModel = new VisibleTreeModel(resultsRoot);
		return resultTreeModel;
	}

	/**
	 * Method to get EcoGridServicesController
	 * 
	 *@return EcoGridServicesController
	 */
	public EcoGridServicesController getSearchScope() {
		return this.searchController;
	}

	/**
	 * Set searchController for this panel
	 * 
	 *@param newTreeData
	 *            Description of the Parameter
	 */
	/*
	 * public void setEcoGridServiceController(EcoGridServicesController
	 * controller) { this.searchController = controller; if (quickSearchAction
	 * != null) { Vector servicesVector = controller.getServicesList();
	 * quickSearchAction.setSearchSerivcesVector(servicesVector); } }
	 */

	/**
	 * Update the result panel after searching
	 * 
	 *@param newTreeData
	 *            CompositeEntity, the search result
	 */
	public void update(EntityTreeModel newTreeData) {
		resultPanel.setTreeModel(newTreeData);
	}

	/**
	 * Sets the progressLabel attribute of the DatasetPanel object
	 * 
	 *@param aMsg
	 *            The new progressLabel value
	 */
	public void setProgressLabel(String aMsg) {
		_progressLabel.setText(aMsg);
	}

	/**
	 * Method to starting search progress bar
	 * 
	 *@param all
	 *            If true, disable _all_ buttons (for auto-fetching data
	 *            sources)
	 */
	public void startSearchProgressBar(boolean all) {

		if (all) {
			_searchUIJPanel.setAllSearchEnabled(false);
		} else {
			_searchUIJPanel.setSearchEnabled(false);
		}
		_progressLabel.setText("");
		_progressBar.setIndeterminate(true);
		_progressBar.setVisible(true);
		_progressLabel.setVisible(false);
	}

	/**
	 * Reset the search panel - enabling/disabling buttons as appropriate and
	 * hiding progress bar
	 */
	public void resetSearchPanel() {

		_searchUIJPanel.setSearchEnabled(true);
		_progressBar.setIndeterminate(false);
		_progressBar.setVisible(false);
		_progressLabel.setVisible(true);
		_searchUIJPanel.setCancelButtonEnabled(true);

	}

	/**
	 * Reset the results panel to ist blank state
	 */
	public void resetResultsPanel() {

		this.getResultRoot().removeAllEntities();
		this.update(new VisibleTreeModel(this.getResultRoot()));
		
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
			DatasetPanel dsp = new DatasetPanel(parent);
			dsp.name = this.getName();
			return dsp;
		}
	}
}