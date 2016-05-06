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

package org.ecoinformatics.seek.datasource.darwincore;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecord;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecordReturnField;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadata;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructure;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructureReturnField;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataSystem;
import org.ecoinformatics.seek.ecogrid.MetadataSpecificationInterface;
import org.ecoinformatics.seek.ecogrid.exception.EcoGridException;
import org.ecoinformatics.seek.ecogrid.exception.InvalidEcogridQueryException;
import org.ecoinformatics.seek.ecogrid.quicksearch.SearchQueryGenerator;
import org.xml.sax.SAXException;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This class is a plugin for DarwinCore namespace to transfer query group to
 * ecogrid query type. It also provide a method to create quick search query
 * 
 * @author Rod Spears (adpated from EML2MetadataSpecification.java)
 */

public class DarwinCoreMetaDataSpecification extends
		MetadataSpecificationInterface {
	private static final String REPLACE = "#value#";
	private static final String QUERYID = "darwincore-quick-search-query";

	private int _numResults = 0;
	private Hashtable systemProviderHash = new Hashtable();
	private Hashtable fieldNametoIdHash = new Hashtable();

	/**
	 * Default constructor
	 */
	public DarwinCoreMetaDataSpecification() {

	} // DarwinCoreMetaDataSpecification

	/**
	 * Method to create a quick query search
	 * 
	 * @param value
	 *            String
	 * @return QueryType
	 */
	public QueryType getQuickSearchEcoGridQuery(String aValue)
			throws InvalidEcogridQueryException {
		Hashtable replaceMapping = new Hashtable();
		replaceMapping.put(REPLACE, aValue);
		SearchQueryGenerator darwinCoreQueryGenerator = new SearchQueryGenerator(
				QUERYID, replaceMapping);
		QueryType ecogridQuery = darwinCoreQueryGenerator.getQuery();
		return ecogridQuery;

	} // getQuickSearchEcoGridQuery

	/**
	 * Method to create a ecogrid query
	 * 
	 * @return QueryType
	 */
	public QueryType getEcoGridQuery() {
		QueryType ecogridQuery = null;
		return ecogridQuery;
	} // getEcoGridQuery

	/**
	 * Helper class to return a named field's value (content)
	 * 
	 * @param aRec
	 *            the record
	 * @param aFieldName
	 *            the name of the field
	 * @return the contents
	 */
	private static String getReturnFieldValue(ResultsetTypeRecord aRec,
			String aFieldName) {
		ResultsetTypeRecordReturnField[] fields = aRec.getReturnField();
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				if (fields[i].getId().equals(aFieldName)) {
					return fields[i].get_value();
				}
			}
		}
		return null;
	}

	private void populateSystemHash(ResultsetTypeResultsetMetadata metaData) {
		ResultsetTypeResultsetMetadataSystem[] system = metaData.getSystem();
		if (system != null) {
			for (int i = 0; i < system.length; i++) {
				systemProviderHash.put(system[i].getId(), system[i].get_value()
						.toString());
			}
		}
	}

	private void populateFieldHash(ResultsetTypeResultsetMetadata metaData) {
		ResultsetTypeResultsetMetadataRecordStructure rs = metaData
				.getRecordStructure();
		ResultsetTypeResultsetMetadataRecordStructureReturnField[] fields = rs
				.getReturnField();
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				String value = fields[i].getName();
				String id = fields[i].getId();
				fieldNametoIdHash.put(value, id);
			}
		}
	}

	private String findFieldId(String fieldName,
			ResultsetTypeResultsetMetadata metaData) {
		ResultsetTypeResultsetMetadataRecordStructure rs = metaData
				.getRecordStructure();
		ResultsetTypeResultsetMetadataRecordStructureReturnField[] fields = rs
				.getReturnField();
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				String value = fields[i].getName();
				if (value.endsWith(fieldName)) {
					return fields[i].getId();
				}
			}
		}

		return null;
	}

	/**
	 * Creates a DarwinCore DataSource which extends Source
	 * 
	 * @param aResults
	 * @param aEndPointURLStr
	 * @param aContainer
	 * @param aResultList
	 * @throws SAXException
	 * @throws IOException
	 * @throws EcoGridException
	 * @throws NameDuplicationException
	 * @throws IllegalActionException
	 */
	public boolean addResultsetRecordsToContainer(ResultsetType aResults,
			String aEndPointURLStr, CompositeEntity aContainer,
			Vector aResultList) throws NameDuplicationException,
			IllegalActionException {
		_numResults = 0;
		if (aResults == null) {
			return false;
		}

		Hashtable speciesHash = new Hashtable();
		String fieldId = null;

		// collect all the providers into a hash table
		ResultsetTypeResultsetMetadata metaData = aResults
				.getResultsetMetadata();
		if (metaData != null) {
			populateSystemHash(metaData);
			populateFieldHash(metaData);
			fieldId = findFieldId("ScientificName", metaData);
		}

		ResultsetTypeRecord[] records = aResults.getRecord();
		if (records == null) {
			return false;
		}

		if (records.length == 0) {
			return false;
		}
		for (int i = 0; i < records.length; i++) {
			ResultsetTypeRecord rec = records[i];
			String scientificName = getReturnFieldValue(rec, fieldId);
			// Drop records
			if (scientificName == null || scientificName.trim().length() == 0) {
				continue;
			}
			scientificName = MetadataSpecificationInterface
					.replaceDotByDash(scientificName);
			// convert the scientific name to lowercase -- effictively making
			// the hash table case insensitive.
			scientificName = scientificName.toLowerCase();
			Hashtable providers = (Hashtable) speciesHash.get(scientificName);
			if (providers == null) {
				providers = new Hashtable();
				speciesHash.put(scientificName, providers);
				// System.out.println("*** Putting ScientificName["+scientificName+"]");
			}
			providers.put(rec.getSystem(), rec.getSystem());
			// System .out.println("  System["+rec.getSystem()+"]");
		}

		// Sort the keys alphabetically for presentation.
		Vector sortedKeys = new Vector(speciesHash.keySet());
		Collections.sort(sortedKeys);
		Iterator it = sortedKeys.iterator();
		while (it.hasNext()) {
			String scientificName = (String) it.next();
			Hashtable providers = (Hashtable) speciesHash.get(scientificName);
			StringBuffer providerList = new StringBuffer();
			for (Enumeration ep = providers.keys(); ep.hasMoreElements();) {
				String providerId = (String) ep.nextElement();
				String provider = (String) systemProviderHash.get(providerId);
				if (providerList.length() > 0) {
					providerList.append(",");
				}
				providerList.append(provider);
			}

			// System.out.println("*** ProviderList["+scientificName+"]["+providerList+"]");
			DarwinCoreDataSource darwinDS = new DarwinCoreDataSource(
					aContainer, scientificName);
			darwinDS.setSearchData(scientificName);
			darwinDS.setEndpoint(aEndPointURLStr);
			DarwinCoreDataSource.generateDocumentationForInstance(darwinDS);
			aResultList.addElement(darwinDS);
			_numResults++;

		}

		return _numResults > 0;
	}

	/**
	 * 
	 * @return Returns a unique name that descrobes this class, often it is the
	 *         name of the class that implments the interface
	 */
	public String getName() {
		return getClass().getName();
	}

	/**
     * 
     */
	public String getBriefName() {
		return "Museum";
	}

	/**
	 * 
	 * @return returns the number of results that for this data.
	 */
	public int getNumResults() {
		return _numResults;
	}

}