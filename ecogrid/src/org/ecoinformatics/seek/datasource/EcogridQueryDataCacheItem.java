/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.datasource;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.authenticatedqueryservice.AuthenticatedQueryServiceClient;
import org.ecoinformatics.ecogrid.queryservice.QueryServiceClient;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecord;
import org.ecoinformatics.ecogrid.queryservice.util.EcogridResultsetParser;
import org.ecoinformatics.ecogrid.queryservice.util.EcogridResultsetTransformer;
import org.ecoinformatics.seek.ecogrid.EcoGridService;
import org.ecoinformatics.seek.ecogrid.EcoGridServicesController;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.DataCacheObject;

/**
 * This class is not as generic as the name may indicate. It is designed to get
 * the metadata to determine where the data is stored and then does a Ecogrid
 * "get" to the data.
 */
public class EcogridQueryDataCacheItem extends DataCacheObject {
	private static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.datasource.EcogridQueryDataCacheItem");
	}

	private QueryType _query = null;
	private ResultsetType _resultset = null;

	/**
     * 
     *
     */
	public EcogridQueryDataCacheItem() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ecoinformatics.seek.datasource.DataCacheItem#doWork()
	 */
	public int doWork() {
		log.debug("EcogridQueryDataCacheItem - doing Work mStatus "
				+ getStatus());
		try {

			// check the serviceType based on endpoint
			EcoGridService service = EcoGridServicesController.getInstance()
					.getService(getResourceName());
			// authenticate if needed
			if (service != null
					&& service
							.getServiceType()
							.equals(
									EcoGridServicesController.AUTHENTICATEDQUERYSERVICETYPE)) {
				String sessionId = EcoGridServicesController.getInstance()
						.authenticateForService(service);
				AuthenticatedQueryServiceClient authQueryClient = new AuthenticatedQueryServiceClient(
						getResourceName());
				_resultset = authQueryClient.query(_query, sessionId);
			} else {
				// just use the normal, non-authenticated service
				QueryServiceClient ecogridClient = new QueryServiceClient(
						getResourceName());
				_resultset = ecogridClient.query(_query);
			}

		} catch (InterruptedException ie) {
			log.debug("Interrupted");
			_resultset = null;
			cleanUpCache();
			return CACHE_EMPTY;
		} catch (Exception ee) {
			log.error("The exception in query is ", ee);
			_resultset = null;
			cleanUpCache();
			return CACHE_ERROR;
		}
		if (_resultset == null) {
			log.debug("*** Resultset was NULL!");
			return CACHE_COMPLETE;
		}
		ResultsetTypeRecord[] records = _resultset.getRecord();
		if (records == null) {
			log.debug("*** Resultset records array was NULL!");
			return CACHE_COMPLETE;
		}
		String rsStr = null;
		try {
			rsStr = EcogridResultsetTransformer.toXMLString(_resultset);
		} catch (java.io.IOException ioe) {
			rsStr = new String();
		}
		if (rsStr.length() == 0) {
			log.debug("*** Resultset string was empty.");
			return CACHE_COMPLETE;
		}
		try {
			BufferedWriter bos = new BufferedWriter(new FileWriter(getFile()));
			bos.write(rsStr);
			bos.flush();
			bos.close();
			return CACHE_COMPLETE;
		} catch (IOException ioe) {
			log.error("*** Unable to write file.");
			cleanUpCache();
			return CACHE_ERROR;
		}

	}

	/**
	 * @param aQuery
	 *            the UqeryType object
	 */
	public void setQuery(QueryType aQuery) {
		_query = aQuery;
	}

	/**
	 * Returns the data cached as a resultset object
	 * 
	 * 	 */
	public ResultsetType getResultset() {
		if (isReady() && _resultset == null) {
			try {
				_resultset = EcogridResultsetParser
						.parseXMLFile(getAbsoluteFileName());
			} catch (org.xml.sax.SAXException e) {
				_resultset = null;
			}
		}
		return _resultset;
	}

	/*
	 * Cleanup the cache object if some error happens during the search.
	 */
	private void cleanUpCache() {

		// clean up the cache object
		try {
			// log.warn("close the input stream and delete existed file");
			// bos.close();
			// getFile().delete();
			if (this.getLSID() != null) {
				CacheManager.getInstance().removeObject(this.getLSID());
			}

		} catch (Exception e) {
			log.error("Couldn't close the output stream to cache file "
					+ e.getMessage());
		}

	}
}