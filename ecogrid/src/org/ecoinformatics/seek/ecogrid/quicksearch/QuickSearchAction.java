/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.seek.ecogrid.DocumentType;
import org.ecoinformatics.seek.ecogrid.EcoGridServicesController;
import org.ecoinformatics.seek.ecogrid.MetadataSpecificationInterface;
import org.ecoinformatics.seek.ecogrid.SelectableEcoGridService;
import org.ecoinformatics.seek.ecogrid.exception.InvalidEcogridQueryException;
import org.kepler.objectmanager.cache.CacheException;
import org.kepler.objectmanager.cache.CacheManager;

import ptolemy.kernel.CompositeEntity;
import ptolemy.vergil.tree.EntityTreeModel;
import ptolemy.vergil.tree.VisibleTreeModel;
import EDU.oswego.cs.dl.util.concurrent.CountDown;

/**
 * This class will handle quick search action
 */

class QuickSearchAction extends AbstractAction {
	// attributes
	private Vector searchServicesVector = null;

	private CompositeEntity resultRoot = null;

	// This vector will store a list of vector. One sub-vector is a list of
	// resultrecord got from one search scope after excuting query (probably
	// search
	// mutiple endpoints)
	private DatasetPanel datasetPanel = null;

	private EntityTreeModel treeModel = null;

	private Vector actionList = new Vector();
	private CountDown completedRequests = null;

	private EcoGridServicesController controller = null;

	private int totalResulsetItem = 0;

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.QuickSearchAction");
	}

	/**
	 * Construct of this class
	 * 
	 * @param controller
	 *            the search info includes endspoints, namespace etc
	 * @param name
	 *            the name of the action
	 * @param datasetPane
	 *            the datasetpane to fire the action
	 */
	public QuickSearchAction(EcoGridServicesController controller, String name,
			DatasetPanel datasetPane) {
		super(name);
		this.controller = controller;
		this.datasetPanel = datasetPane;

	}

	/**
	 * Set up search scope vector
	 * 
	 * @param searchVector
	 *            Vector
	 */
	/*
	 * public void setSearchSerivcesVector(Vector searchVector) {
	 * searchServicesVector = searchVector; }
	 */

	/**
	 * The todo Implementation of abstract method. It will search ecogrid site
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public synchronized void actionPerformed(ActionEvent e) {
		datasetPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		CacheManager cm;
		try {
			cm = CacheManager.getInstance();
			//cm.showDB();
		} catch (CacheException e1) {
			e1.printStackTrace();
		}
		String searchValue = null;
		if (datasetPanel != null) {
			searchValue = datasetPanel.getSearchTextFieldValue();
			// searchType = datasetPanel.getSearchDataSrcType();
			resultRoot = datasetPanel.getResultRoot();
		}

		//
		// If no search term is entered, return immediately.
		if (searchValue == null || searchValue.trim().equals("")) {
			return;
		}

		System.out.println("searching..");

		searchServicesVector = controller.getSelectedServicesList();
		actionList = new Vector();

		// transfer endpoint based EcoGridService to namespace based Search
		// Scope
		Vector searchScopeVector = transformEcoGridServiceToSearchScope();
		if (!searchScopeVector.isEmpty() && resultRoot != null) {
			resultRoot.removeAllEntities();
			// go through every namespace in search scope
			for (int i = 0; i < searchScopeVector.size(); i++) {
				// vecotr to store the ResultRecord for one search scope
				SearchScope searchScope = (SearchScope) searchScopeVector
						.elementAt(i);
				// String namespace = searchScope.getNamespace();
				// get quick search query from metadata specification class
				MetadataSpecificationInterface metadataSpecClass = searchScope
						.getMetadataSpecification();

				// *** Temporary Code
				String namespace = searchScope.getNamespace();

				QueryType quickSearchQuery = null;
				try {
					quickSearchQuery = metadataSpecClass
							.getQuickSearchEcoGridQuery(searchValue);
				} catch (InvalidEcogridQueryException inE) {
					log.debug("The error to generate quick search query ", inE);
					return;
				}
				Vector searchEndPoints = searchScope.getEndPoints();
				if (searchEndPoints == null) {
					log.debug("No search end points can be found");
					return;
				}

				// go through the end points vector and create query action

				searchEndPointsVector(searchEndPoints, quickSearchQuery,
						searchValue, metadataSpecClass, namespace);

			} // for
			log.debug("Initial query action ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ "
					+ actionList.size());
			completedRequests = new CountDown(actionList.size());
			// start query action
			datasetPanel.resetResultsPanel();

			boolean forRegistryQuery = false;
			datasetPanel.startSearchProgressBar(forRegistryQuery);
			for (int i = 0; i < actionList.size(); i++) {
				QueryAction queryAction = (QueryAction) actionList.elementAt(i);
				queryAction.actionPerformed(null);
			}
			
			
		} // if
		datasetPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	} // actionPerformed

	/*
	 * cd Go through the search end vector
	 */
	private void searchEndPointsVector(Vector searchEndPoints,
			QueryType quickSearchQuery, String searchValue,
			MetadataSpecificationInterface metadataSpecClass, String namespace) {

		// go through each search end points and get result set
		for (int j = 0; j < searchEndPoints.size(); j++) {

			String url = (String) searchEndPoints.elementAt(j);
			log.debug("============search " + url + " for namespace "
					+ namespace);
			QueryAction doQuery = new QueryAction(this, quickSearchQuery, url,
					searchValue, metadataSpecClass, namespace);
			actionList.addElement(doQuery);

		} // for
		// actionList = new Vector();

	}

	/**
	 * Method transfer EcoGridService(base on endpoint) vector to SearchScope
	 * (base on namespace) vector
	 * 
	 * @return vector - may be empty.
	 */
	private Vector transformEcoGridServiceToSearchScope() {
		Vector searchScopeVector = new Vector();

		int size = searchServicesVector.size();
		// this hashtable will stored the namespace and its searchscope
		Hashtable namespaceAndSearhScopeMap = new Hashtable();
		for (int i = 0; i < size; i++) {
			SelectableEcoGridService service = (SelectableEcoGridService) searchServicesVector
					.elementAt(i);
			if (service == null) {
				continue;
			}
			String endPoint = service.getEndPoint();
			if (endPoint == null || endPoint.trim().equals("")) {
				continue;
			}
			DocumentType[] typeList = service.getSelectedDocumentTypeList();
			if (typeList == null) {
				continue;
			}
			// go through every type
			for (int j = 0; j < typeList.length; j++) {
				DocumentType type = typeList[j];
				String namespaceStr = type.getNamespace();
				String metadataClassName = type
						.getMetadataSpecificationClassName();
				if (namespaceStr != null && !namespaceStr.trim().equals("")
						&& metadataClassName != null
						&& !metadataClassName.trim().equals("")) {
					if (!namespaceAndSearhScopeMap.containsKey(namespaceStr)) {
						// if this is first namespace we found, create a new
						// searchscope
						Vector endPointsVector = new Vector();
						endPointsVector.add(endPoint);
						try {
							SearchScope newScope = new SearchScope(
									namespaceStr, metadataClassName,
									endPointsVector);
							namespaceAndSearhScopeMap.put(namespaceStr,
									newScope);
							log.debug("add " + namespaceStr + " "
									+ metadataClassName + " " + endPoint
									+ " into SearchScope");
						} catch (Exception e) {
							log.debug("error create search scope ", e);
							continue;
						}
					} else {
						// if we already has a SearchScope for this namespace,
						// add this endpoints to endpoints vector
						SearchScope existedScope = (SearchScope) namespaceAndSearhScopeMap
								.get(namespaceStr);
						log.debug("Add endpoint " + endPoint
								+ " to a existed searchscope object");
						existedScope.addSearchEndPoint(endPoint);
						// overwrite the old value
						namespaceAndSearhScopeMap.put(namespaceStr,
								existedScope);
					}
				} // if
			} // for
		} // for

		// transfer the hashtable to a vector
		Enumeration en = namespaceAndSearhScopeMap.keys();
		while (en.hasMoreElements()) {
			String key = (String) en.nextElement();
			SearchScope scope = (SearchScope) namespaceAndSearhScopeMap
					.get(key);
			if (scope != null) {
				searchScopeVector.add(scope);
			}
		}

		return searchScopeVector;
	}

	/**
	 * Method to stop quick search.
	 * 
	 */
	public synchronized void stop() {

		// then will should stop the queryaction(they are in another thread)
		int size = actionList.size();
		log
				.debug("The size of original query action in stop method is "
						+ size);
		for (int i = 0; i < size; i++) {
			QueryAction queryAct = (QueryAction) actionList.elementAt(i);
			queryAct.stopAction();
		}
		datasetPanel.resetResultsPanel();
		datasetPanel.setProgressLabel("search canceled.");
		// reset total result set item
		totalResulsetItem = 0;
		actionList = new Vector();
		completedRequests = null;
		datasetPanel.resetSearchPanel();
	}

	public synchronized void notifyQueryActionComplete(QueryAction queryAction) {
		// old canceled action wouldn't add to recycle vector
		if (!queryAction.isCanceled() && actionList.contains(queryAction)) {
			log.debug("finished query action !!!!!!!!!!!!!"
					+ queryAction.getNameSpace() + " "
					+ queryAction.getSearchValue());
			completedRequests.release();
		} else {
			log.debug("descard a canceled query action !!!!!!!!!!!!!"
					+ queryAction.getNameSpace() + " "
					+ queryAction.getSearchValue());
			return;
		}
		// after recycled every query action, display the panel
		int sizeOfOrigin = actionList.size();
		log.debug("The original action size = " + sizeOfOrigin);
		int numToCompleted = completedRequests.currentCount();
		log.debug("Actions not completed= " + numToCompleted);
		if (numToCompleted == 0) {
			log.debug("finalize the resultset.........");
			for (int i = 0; i < sizeOfOrigin; i++) {
				QueryAction action = (QueryAction) actionList.elementAt(i);
				resultsetComplete(action);
			}
			log.debug("update dataset panel");
			// sort the resultvector from one scope and add it to the
			// recordVector
			// Collections.sort(resultFromOneScope, new
			// ResultRecordComparator());
			treeModel = new VisibleTreeModel(resultRoot);
			datasetPanel.update(treeModel);
			int realCount = treeModel.getChildCount(resultRoot);
			datasetPanel.setProgressLabel(realCount + " results returned.");
			datasetPanel.resetSearchPanel();
			// reset total result set item
			totalResulsetItem = 0;
		}
	}

	/**
	 * Processes the resultset and fills the panel
	 * 
	 * @param aQA
	 *            QueryAction
	 */
	private void resultsetComplete(QueryAction aQA) {
		// parse the resultset into and stored it into a vector
		Vector resultVector = new Vector();
		int numResults = 0;
		try {

			MetadataSpecificationInterface mdi = aQA.getMetaDataIFace();
			ResultsetType results = aQA.getResultSet();

			// _queryAction = aQA;
			if (results != null) {
				mdi.addResultsetRecordsToContainer(results, aQA.getURL(),
						resultRoot, resultVector);
				int newHits = mdi.getNumResults();
				// Note: this must be done after calling
				// addResultsetRecordsToContainer
				// addResultsetRecordsToContainer does the processing
				totalResulsetItem = totalResulsetItem + newHits;
			}
			/*
			 * else { // using SwingUtilities lets the msg dialog (model) show
			 * on it's own thread instead of // blocking here and keeping the
			 * progress meter from ending SwingUtilities.invokeLater(new
			 * Runnable() { public void run() {
			 * JOptionPane.showMessageDialog(datasetPanel,
			 * _queryAction.getError()); } });
			 * datasetPanel.setProgressLabel("Error getting results."); }
			 */

		} catch (Exception ee) {
			log.debug("Error adding resultset to container " + ee.getMessage());
		}

	}
}