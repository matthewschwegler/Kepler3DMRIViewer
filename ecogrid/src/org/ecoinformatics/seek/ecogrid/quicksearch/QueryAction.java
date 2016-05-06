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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.seek.datasource.EcogridQueryDataCacheItem;
import org.ecoinformatics.seek.ecogrid.MetadataSpecificationInterface;
import org.kepler.objectmanager.cache.DataCacheListener;
import org.kepler.objectmanager.cache.DataCacheManager;
import org.kepler.objectmanager.cache.DataCacheObject;

/**
 * This class will access ecogrid nodes: send the query and get the resultset
 * 
 * @author Tao
 */

class QueryAction extends AbstractAction implements DataCacheListener {

	private static final Log log = LogFactory.getLog(QueryAction.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
	private QueryType query = null;
	private String url = null;
	private ResultsetType results = null;
	private String searchValue = null;
	// private EcogridQueryClient ecogridClient = null;
	private String error = null;
	private volatile boolean canceled = false;

	private MetadataSpecificationInterface _metadataSpecClass = null;
	private QuickSearchAction _searchAction = null;
	private EcogridQueryDataCacheItem _dataCacheItem = null;
	private String _namespace = null;

	/**
	 * Contructor of QueryAction
	 * 
	 * @param myQuery
	 *            QueryType
	 * @param url
	 *            Vector
	 */
	public QueryAction(QuickSearchAction mySearchAction, QueryType myQuery,
			String myUrl, String mySearchValue,
			MetadataSpecificationInterface myMetadataSpecClass, String namespace) {
		_searchAction = mySearchAction;
		query = myQuery;
		url = myUrl;
		searchValue = mySearchValue;
		_metadataSpecClass = myMetadataSpecClass;
		_namespace = namespace;
	}

	/**
	 * 
	 * 	 */
	public MetadataSpecificationInterface getMetaDataIFace() {
		return _metadataSpecClass;
	}

	/**
	 * 
	 * 	 */
	public String getURL() {
		return url;
	}

	public String getNameSpace() {
		return _namespace;
	}

	public String getSearchValue() {
		return searchValue;
	}

	/**
	 * Get status of canceled
	 * 
	 * 	 */
	public boolean isCanceled() {
		return canceled;
	}

	/**
	 * The todo Implementation of abstract method. It will search ecogrid site
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		if (query == null || url == null) {
			error = "Query is null or search url is null";
			log.debug(error);
			return;
		}
		try {
			String dataCacheName = "Ecogrid Query Search for " + _namespace
					+ " " + searchValue;
			log.debug(dataCacheName);
			_dataCacheItem = (EcogridQueryDataCacheItem) DataCacheManager
					.getCacheItem(this, dataCacheName, url,
							EcogridQueryDataCacheItem.class.getName());
			if (_dataCacheItem.isEmpty()) {
				_dataCacheItem.setQuery(query);
				log.debug("The query cache item is +++++++++++++"
						+ _dataCacheItem.toString());
				_dataCacheItem.start();
			}
		} catch (Exception ee) {
			error = ee.getMessage();
			log.error(error, ee);
		}
	}// actionPerformed

	/**
	 * Method to get the resultset after query
	 * 
	 * @return ResultsetType
	 */
	public ResultsetType getResultSet() {
		return this.results;
	}// getResultSet

	/**
	 * Method to return the error message when execute query action
	 * 
	 * @return String
	 */
	public String getError() {
		return error;
	}

	/**
	 * Method to stop query action by stopping the cache item thread
	 * 
	 */
	public void stopAction() {
		canceled = true;
		// If the dataCacheItem hasn't finished yet, stop it.
		if (!_dataCacheItem.isReady()) {
			_dataCacheItem.stop();
			DataCacheManager.removeItem(_dataCacheItem);
		}
	}

	// ------------------------------------------------------------------------
	// -- DataCacheListener
	// ------------------------------------------------------------------------

	public void complete(DataCacheObject aItem) {

		aItem.removeListener(this);

		if (aItem.isReady()) {
			results = ((EcogridQueryDataCacheItem) aItem).getResultset();
		} else {
			results = null;
			// error =
			// "There was error querying for `"+searchValue+"`\nPlease try again.";
		}
		_searchAction.notifyQueryActionComplete(this);
	}
}// QueryAction