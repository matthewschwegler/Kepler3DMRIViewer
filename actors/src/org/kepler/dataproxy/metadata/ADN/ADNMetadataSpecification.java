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

package org.kepler.dataproxy.metadata.ADN;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecord;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecordReturnField;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadata;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructure;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructureReturnField;
import org.ecoinformatics.seek.ecogrid.MetadataSpecificationInterface;
import org.ecoinformatics.seek.ecogrid.exception.EcoGridException;
import org.ecoinformatics.seek.ecogrid.exception.InvalidEcogridQueryException;
import org.ecoinformatics.seek.ecogrid.quicksearch.SearchQueryGenerator;
import org.ecoinformatics.seek.ecogrid.quicksearch.SortableResultRecord;
import org.ecoinformatics.seek.ecogrid.quicksearch.SortableResultRecordComparator;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.dataproxy.datasource.geon.GEONDatabaseResource;
import org.kepler.dataproxy.datasource.geon.GEONShpResource;
import org.xml.sax.SAXException;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This class is a plugin for adn namespace to transfer query group to ecogrid
 * query type. It also provide a method to create quick search query
 * 
 * @author Efrat Jaeger-Frank
 */

public class ADNMetadataSpecification extends MetadataSpecificationInterface {
	public static final String ADNNAMESPACE = "http://www.sdsc.geongrid.org/services/search";
	private static final int NAMESPACEARRAYLENGTH = 1;
	private static final String OPERATOR = "LIKE";
	private static final String FILERFIELD = "filerField";
	private static final String UNKNOWNTITLE = "unknownTitle";
	private static final String REPLACE = "#value#";
	private static final String QUERYID = "geon-quick-search-query";
	private static final int GAP = 106;

	private int _numResults = 0;
	protected String namespace = null;
	protected String queryId = null;

	private static final String DOCURL = "//documentation[namespace=\""
			+ ADNNAMESPACE + "\"]/url";
	private static final String DOCUSERNAME = "//documentation[namespace=\""
			+ ADNNAMESPACE + "\"]/username";
	private Map fieldIdtoNameMap = new HashMap();

	// private String recordTitle = null;
	// private Vector entityName = null;

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.kepler.dataproxy.metadata.ADN.ADNMetadataSpecification");
	}

	/**
	 * Default constructor
	 */
	public ADNMetadataSpecification() {
		namespace = ADNNAMESPACE; // TODO: REPLACE
		queryId = QUERYID;
	}// Eml200EcoGridQueryTransfer

	/**
	 * Returns a URL to the ADN documentation
	 * 
	 * @throws MalformedURLException
	 */

	public static URL getDocumentation(String recordId)
			throws MalformedURLException {
        
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    ConfigurationProperty documentationProperty = confMan.getProperty(
      ConfigurationManager.getModule("common"));
    String docURL =  documentationProperty.getProperty("documentation.url").getValue();
    String userName = documentationProperty.getProperty("documentation.username").getValue();
    
		docURL += "?id=" + recordId + "&username=" + userName + "#in_browser";
		return new URL(docURL);
	}

	/**
	 * Method to create a quick query search
	 * 
	 * @param value
	 *            String
	 * @return QueryType
	 */
	public QueryType getQuickSearchEcoGridQuery(String value)
			throws InvalidEcogridQueryException {
		Hashtable replaceMapping = new Hashtable();
		replaceMapping.put(REPLACE, value);
		SearchQueryGenerator eml200QueryGenerator = new SearchQueryGenerator(
				queryId, replaceMapping);
		QueryType ecogridQuery = eml200QueryGenerator.getQuery();
		return ecogridQuery;

	}// getQuickSearchEcoGridQuery

	/**
	 * Method to create a ecogrid query
	 * 
	 * @return QueryType
	 */
	public QueryType getEcoGridQuery() {
		QueryType ecogridQuery = null;
		return ecogridQuery;
	}// getEcoGridQuery

	/**
	 * This method will transfer ResultsetType java object to array of
	 * ResultRecord java object. The ResultRecord object can be shown in kepler.
	 * If the results is null or there is no record in the result, null will be
	 * return
	 * 
	 * @param ResultsetType
	 *            results the result need to be transform
	 * @param String
	 *            endpoints the search end point
	 * @return ResultRecord[] the resultrecord need be returned.
	 */
	public boolean transformResultset(ResultsetType results, String endpoint,
			CompositeEntity container, Vector aResultList) throws SAXException,
			IOException, EcoGridException, NameDuplicationException,
			IllegalActionException {
		if (results == null) {
			return false; // ???
		}

		ResultsetTypeResultsetMetadata metaData = results
				.getResultsetMetadata();
		if (metaData != null) {
			populateFieldMap(metaData);
		}

		// transfer ResultType to a vector of sorted titles containing
		// (title,ids,returnFieldsVector)
		Vector resultsetItemList = transformResultsetType(results);

		// transfer the sored vector (contains eml2resultsetitem object to an
		// array
		// of ResultRecord
		int numResults = resultsetItemList.size();

		aResultList = new Vector();

		Hashtable titleList = new Hashtable();// This hashtable is for keeping
												// track
		// if there is a duplicate title
		for (int i = 0; i < numResults; i++) {
			try {
				SortableResultRecord source = (SortableResultRecord) resultsetItemList
						.elementAt(i);
				String title = source.getTitle();
				log.debug("The title is " + title);
				String id = source.getId();
				log.debug("The id is " + id);
				Vector returnFieldList = source.getEntityList();
				// if couldn't find id, skip this record
				if (id == null || id.trim().equals("")) {
					continue;
				}

				// if couldn't find title, assign a one to it -- <j>
				if (title == null || title.trim().equals("")) {
					title = "<" + i + ">";
				}
				if (titleList.containsKey(title)) {
					title = title + " " + i;
				}
				titleList.put(title, title);

				String format = null;
				String description = "";

				for (int j = 0; j < returnFieldList.size(); j++) {
					ResultsetTypeRecordReturnField returnField = (ResultsetTypeRecordReturnField) returnFieldList
							.elementAt(j);
					if (returnField == null) {
						continue;
					}
					String returnFieldId = returnField.getId();
					String returnFieldValue = returnField.get_value();
					String returnFieldName = (String) fieldIdtoNameMap
							.get(returnFieldId);
					if (returnFieldName != null
							&& !returnFieldName.trim().equals("")) {
						if (returnFieldName.equals("description")) {
							log.debug("The description after parsing is  "
									+ returnFieldValue);
							description = returnFieldValue;
						} else if (returnFieldName.equals("format")) {
							format = returnFieldValue;
						}
					}
				}

				TypedAtomicActor newRecord = null;
				if (format.trim().toLowerCase().indexOf("database") > -1) { // VERIFY!!!!
					log.debug("The entiy is a database resource");
					newRecord = new GEONDatabaseResource(container, title);
					((GEONDatabaseResource) newRecord)._idAtt.setExpression(id);
					((GEONDatabaseResource) newRecord)._endpointAtt
							.setExpression(endpoint);
					((GEONDatabaseResource) newRecord)._namespaceAtt
							.setExpression(namespace);
					((GEONDatabaseResource) newRecord)._descriptionAtt
							.setExpression(restyleDescription(description));
				} else if (format.equals("shapefile")) {
					log.debug("The entiy is a shapefile resource");
					newRecord = new GEONShpResource(container, title);
					((GEONShpResource) newRecord)._idAtt.setExpression(id);
					((GEONShpResource) newRecord)._endpointAtt
							.setExpression(endpoint);
					((GEONShpResource) newRecord)._namespaceAtt
							.setExpression(namespace);
					((GEONShpResource) newRecord)._descriptionAtt
							.setExpression(restyleDescription(description));

				} else
					continue;

				aResultList.add(newRecord);

			} catch (Exception e) {
				continue;
			}
		}// for

		_numResults = aResultList.size();
		return _numResults > 0;
	}

	/*
	 * Method to transform array of AnyRecordType to array of EML2ResultsetItem
	 */
	private Vector transformResultsetType(ResultsetType result) {
		Vector itemList = new Vector();
		if (result == null) {
			return itemList;
		}
		ResultsetTypeRecord[] records = result.getRecord();
		if (records == null) {
			return itemList;
		}

		int numRecords = records.length;
		// transfer every records from source to a dest - EML2ResultsetItem obj
		for (int i = 0; i < numRecords; i++) {
			ResultsetTypeRecord currentRecord = records[i];
			if (currentRecord == null) {
				continue;
			}
			String docid = currentRecord.getIdentifier();
			log.debug("The doc id after parsing resultset is " + docid);
			// if couldn't find identifier, we don't need it
			if (docid == null || docid.trim().equals("")) {
				continue;
			}
			ResultsetTypeRecordReturnField[] recordReturnFieldList = currentRecord
					.getReturnField();
			if (recordReturnFieldList == null) {
				continue;
			}

			String recordTitle = null;

			Vector returnFieldList = new Vector();

			int length = recordReturnFieldList.length;
			for (int j = 0; j < length; j++) {
				ResultsetTypeRecordReturnField currentReturnField = recordReturnFieldList[j];
				if (currentReturnField == null) {
					continue;
				}
				String returnFieldId = currentReturnField.getId();
				String returnFieldValue = currentReturnField.get_value();
				String returnFieldName = (String) fieldIdtoNameMap
						.get(returnFieldId);
				if (returnFieldName != null
						&& !returnFieldName.trim().equals("")) {
					if (returnFieldName.equals("title")) {
						log.debug("The title after parsing is  "
								+ returnFieldValue);
						recordTitle = returnFieldValue;
						recordTitle = replaceDotByDash(recordTitle);
					} else {
						returnFieldList.add(currentReturnField); // the other
																	// field
																	// will be
																	// used
																	// later.
					}
				}

			}// for
			if (recordTitle == null) {
				recordTitle = UNKNOWNTITLE + i;
			}
			SortableResultRecord newItem = new SortableResultRecord(
					recordTitle, docid, returnFieldList);
			itemList.add(newItem);

		}// for
		Collections.sort(itemList, new SortableResultRecordComparator());
		return itemList;
	}

	/**
	 * Initialize the mapping between return fields ids and names
	 * 
	 * @param metaData
	 */
	private void populateFieldMap(ResultsetTypeResultsetMetadata metaData) {
		ResultsetTypeResultsetMetadataRecordStructure recordStructure = metaData
				.getRecordStructure();
		ResultsetTypeResultsetMetadataRecordStructureReturnField[] fields = recordStructure
				.getReturnField();
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				String value = fields[i].getName();
				String id = fields[i].getId();
				fieldIdtoNameMap.put(id, value);
				// fieldNametoIdHash.put( value, id );
			}
		}
	}

	/**
	 * Add new lines to provider a nicer display of the description.
	 * 
	 * @param detail
	 *            the additional information about the record
	 */
	private String restyleDescription(String description) {
		int fromInd = 0;
		int prevSpaceInd = 0;
		String newDescription = "";
		int descLen = description.length();
		while (fromInd + GAP < descLen) {
			int nextSpaceInd = description.indexOf(' ', fromInd + GAP);
			prevSpaceInd = description.lastIndexOf(' ', nextSpaceInd - 1); // make
																			// it
																			// backword.
			newDescription += description.substring(fromInd, prevSpaceInd)
					+ "\n";
			fromInd = prevSpaceInd + 1;
		}
		newDescription += description.substring(fromInd);
		return newDescription;
	}

	/**
	 * Creates GEONDataResource items (derived from Source) and adds them into
	 * the container
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
	public boolean addResultsetRecordsToContainer(ResultsetType results,
			String endpoint, CompositeEntity container, Vector aResultList)
			throws SAXException, IOException, EcoGridException,
			NameDuplicationException, IllegalActionException {
		// parse the resultset into ResultRecord array and stored it into
		// a vector
		try {
			return transformResultset(results, endpoint, container, aResultList);
		} catch (Exception ee) {
			log
					.debug(
							"The error to transform from resultset to GEONDataResource ",
							ee);
		}
		return false;
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
		return "Geology";
	}

	/**
	 * 
	 * @return returns the number of results that for this data.
	 */
	public int getNumResults() {
		return _numResults;
	}

}// Eml200EcoGridQueryTransfer