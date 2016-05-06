/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2011-11-10 17:31:50 -0800 (Thu, 10 Nov 2011) $' 
 * '$Revision: 28928 $'
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

package org.ecoinformatics.seek.datasource.eml.eml2;

import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecord;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecordReturnField;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadata;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataNamespace;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructure;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeResultsetMetadataRecordStructureReturnField;
import org.ecoinformatics.seek.ecogrid.MetadataSpecificationInterface;
import org.ecoinformatics.seek.ecogrid.exception.EcoGridException;
import org.ecoinformatics.seek.ecogrid.exception.InvalidEcogridQueryException;
import org.ecoinformatics.seek.ecogrid.quicksearch.ResultRecord;
import org.ecoinformatics.seek.ecogrid.quicksearch.SearchQueryGenerator;
import org.ecoinformatics.seek.ecogrid.quicksearch.SortableResultRecord;
import org.ecoinformatics.seek.ecogrid.quicksearch.SortableResultRecordComparator;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.kar.KARCacheContent;
import org.kepler.kar.KARCacheManager;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.xml.sax.SAXException;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * This class is a plugin for eml200 namespace to transfer query group to
 * ecogrid query type. It also provide a method to create quick search query
 * 
 * @author Jing Tao
 */

public class EML2MetadataSpecification extends MetadataSpecificationInterface {
	public static final String EML200NAMESPACE = "eml://ecoinformatics.org/eml-2.0.0";
	private static final int NAMESPACEARRAYLENGTH = 1;
	private static final String OPERATOR = "LIKE";
	private static final String FILERFIELD = "filerField";
	private static final String UNKNOWNTITLE = "unknownTitle";
	private static final String REPLACE = "#value#";
	private static final String QUERYID = "eml200-quick-search-query";

	private int _numResults = 0;
	protected String namespace = null;
	protected String queryId = null;

	protected final static Log log;
	protected final static boolean isDebugging;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.EML2MetadataSpecification");
		isDebugging = log.isDebugEnabled();
	}

	// private String recordTitle = null;
	// private Vector entityName = null;

	/**
	 * Default constructor
	 */
	public EML2MetadataSpecification() {
		namespace = EML200NAMESPACE;
		queryId = QUERYID;
	}// Eml200EcoGridQueryTransfer

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
	public ResultRecord[] transformResultset(ResultsetType results,
			String endpoint, CompositeEntity container) throws SAXException,
			IOException, EcoGridException, NameDuplicationException,
			IllegalActionException {
		Eml200DataSource[] resultRecordArray = null;
		if (results == null) {
			return resultRecordArray;
		}

		// get titlstring and entity name string from configure file
		String titleReturnFieldString = null;
		String entityReturnFieldString = null;
		// xpath for config file
		String titlePath = "//" + ECOGRIDPATH + "/" + RETURNFIELDTYPELIST + "/"
				+ RETURNFIELD + "[@" + NAMESPACE + "='" + namespace + "' and @"
				+ RETURNFIELDTYPE + "='" + RETURNFIELDTITLE + "']";
        
		String entityPath = "//" + ECOGRIDPATH + "/" + RETURNFIELDTYPELIST
				+ "/" + RETURNFIELD + "[@" + NAMESPACE + "='" + namespace
				+ "' and @" + RETURNFIELDTYPE + "='" + RETURNFIELDENTITY + "']";
        
		List titlePathList = null;
		List entityPathList = null;
    
    //get the specific configuration we want
    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance().getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty returnFieldTypeList = ecogridProperty.getProperty("returnFieldTypeList");
    //findthe namespace properties
    List returnFieldTypeNamespaceList = returnFieldTypeList.findProperties(NAMESPACE, namespace, true  );
    //find the properties out of the correct namespace properties that also have the correct returnfieldtype
    titlePathList = ConfigurationProperty.findProperties(returnFieldTypeNamespaceList, RETURNFIELDTYPE, RETURNFIELDTITLE, false);
    //get the value list for the properties
    titlePathList = ConfigurationProperty.getValueList(titlePathList, "value", true);
        
		if (titlePathList.isEmpty()) {
			log
					.debug("Couldn't get title from config Eml200EcoGridQueryTransfer.transformResultset");
			throw new EcoGridException(
					"Couldn't get title returnfield from config");
		}

    entityPathList = ConfigurationProperty.findProperties(returnFieldTypeNamespaceList, RETURNFIELDTYPE, RETURNFIELDENTITY, false);
    //get the value list for the properties
    entityPathList = ConfigurationProperty.getValueList(entityPathList, "value", true);
    
		if (entityPathList.isEmpty()) {
			log
					.debug("Couldn't get entity returnfield from config Eml200EcoGridQueryTransfer.transformResultset");
			throw new EcoGridException(
					"Couldn't get entity returnfield from config");
		}
		// only choose the first one in vector as title returnfied or
		// entityreturn
		// field
		titleReturnFieldString = (String) titlePathList.get(0);
		entityReturnFieldString = (String) entityPathList.get(0);

		// transfer ResultType to a vector of eml2resultsetItem and
		// sorted the vector
		Vector resultsetItemList = transformResultsetType(results,
				titleReturnFieldString, entityReturnFieldString);
		// transfer the sored vector (contains eml2resultsetitem object to an
		// array
		// of ResultRecord
		int arraySize = resultsetItemList.size();
		_numResults = arraySize;

		resultRecordArray = new Eml200DataSource[arraySize];
		Hashtable titleList = new Hashtable();// This hashtable is for keeping
												// track
		// if there is a duplicate title
		
		KeplerLSID EML200ActorLSID = null;
		for (int i = 0; i < arraySize; i++) {
			try {
				SortableResultRecord source = (SortableResultRecord) resultsetItemList
						.elementAt(i);
				String title = source.getTitle();
				log.debug("The title is " + title);
				String id = source.getId();
				log.debug("The id is " + id);
				Vector entityList = source.getEntityList();
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
				//make it unique in the result tree
				if (container.getEntity(title) != null) {
					String hint = "another service";
					if (endpoint.lastIndexOf("/") > -1) {
						hint = endpoint.substring(endpoint.lastIndexOf("/") + 1);
					}	
					title = title + " (" + hint  + ")";
				}
				titleList.put(title, title);
				Eml200DataSource newRecord = new Eml200DataSource(container, title);
				newRecord.setRecordId(id);
				newRecord.setEndpoint(endpoint);
				newRecord.setNamespace(namespace);
				for (int j = 0; j < entityList.size(); j++) {
					String entityNameString = (String) entityList.elementAt(j);
					log.debug("The entiy name will be " + entityNameString);
					newRecord.addRecordDetail(entityNameString);

				}
				Eml200DataSource.generateDocumentationForInstance(newRecord);
				
				// look up EML200DataSource class LSID and create and add
				// NamedObjID attribute to instance
				if (EML200ActorLSID == null){
					KARCacheManager kcm = KARCacheManager.getInstance();
					Vector<KARCacheContent> list = kcm.getKARCacheContents();
					for(KARCacheContent content : list) {
						if(content.getCacheContent().getClassName().equals(Eml200DataSource.class.getName())) {
							EML200ActorLSID = content.getLsid();
						}
					}
				}
				NamedObjId lsidSA = new NamedObjId(newRecord, NamedObjId.NAME);
				lsidSA.setExpression(EML200ActorLSID);

				resultRecordArray[i] = newRecord;
			} catch (Exception e) {
				continue;
			}
		}// for
		return resultRecordArray;
	}

	/*
	 * Method to transform array of AnyRecordType to array of EML2ResultsetItem
	 */
	private Vector transformResultsetType(ResultsetType result,
			String titleReturnFieldString, String entityReturnFieldString) {
		Vector itemList = new Vector();
		if (result == null) {
			return itemList;
		}
		ResultsetTypeRecord[] records = result.getRecord();
		if (records == null) {
			return itemList;
		}

		ResultsetTypeResultsetMetadata metadata = result.getResultsetMetadata();
		String titleId = getIdForGivenReturnField(titleReturnFieldString,
				namespace, metadata);
		log.debug("The title id for eml2 is " + titleId);
		String entityId = getIdForGivenReturnField(entityReturnFieldString,
				namespace, metadata);
		log.debug("The entity id for eml2 is " + entityId);
		int arraySize = records.length;
		// transfer every records from source to a dest - EML2ResultsetItem obj
		for (int i = 0; i < arraySize; i++) {
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
			String recordTitle = null;
			Vector entityNameList = new Vector();
			ResultsetTypeRecordReturnField[] recordReturnFieldList = currentRecord
					.getReturnField();
			if (recordReturnFieldList == null) {
				continue;
			}
			int length = recordReturnFieldList.length;
			for (int j = 0; j < length; j++) {
				ResultsetTypeRecordReturnField currentReturnField = recordReturnFieldList[j];
				if (currentReturnField == null) {
					continue;
				}
				String returnFieldId = currentReturnField.getId();
				String returnFieldValue = currentReturnField.get_value();
				if (returnFieldId != null && !returnFieldId.trim().equals("")) {
					if (titleId != null && returnFieldId.equals(titleId)) {
						log.debug("The title after parsing is  "
								+ returnFieldValue);
						recordTitle = returnFieldValue;
						recordTitle = replaceDotByDash(recordTitle);
					}
					if (entityId != null && returnFieldId.equals(entityId)) {
						log.debug("The original entity is " + returnFieldValue);
						returnFieldValue = replaceDotByDash(returnFieldValue);
						entityNameList.add(returnFieldValue);
					}
				}

			}// for
			if (recordTitle == null) {
				recordTitle = UNKNOWNTITLE + i;
			}
			SortableResultRecord newItem = new SortableResultRecord(
					recordTitle, docid, entityNameList);
			itemList.add(newItem);

		}// for
		Collections.sort(itemList, new SortableResultRecordComparator());
		return itemList;
	}

	/*
	 * A method to get an id for given returnfield and namespace in a metadata. if
	 * more than one can be found, we only chose the first one
	 */
	private String getIdForGivenReturnField(String givenReturnField,
			String givenNameSpace, ResultsetTypeResultsetMetadata metadata) {
		String id = null;
		if (givenReturnField == null || givenNameSpace == null
				|| metadata == null) {
			return id;
		}

		ResultsetTypeResultsetMetadataRecordStructure structure = metadata
				.getRecordStructure();
		if (structure != null) {
			
			ResultsetTypeResultsetMetadataNamespace[] rtrmn = metadata.getNamespace();
			for (int i=0; i< rtrmn.length; i++){
				//String nameSpace = metadata.getNamespace().get_value().toString();
				String nameSpace = rtrmn[i].get_value().toString();
				if (nameSpace != null && nameSpace.equals(givenNameSpace)) {
					log.debug("The target namespace " + nameSpace + " is found");
					ResultsetTypeResultsetMetadataRecordStructureReturnField[] returnFieldList = structure
						.getReturnField();
					if (returnFieldList != null) {
						int length = returnFieldList.length;
						for (int j = 0; j < length; j++) {
							ResultsetTypeResultsetMetadataRecordStructureReturnField currentReturnField = returnFieldList[j];
							if (currentReturnField != null) {
								String returnFieldName = currentReturnField
									.getName();
								String returnFieldId = currentReturnField.getId();
								if (returnFieldName != null
									&& returnFieldName.equals(givenReturnField)) {
									id = returnFieldId;
									if (id != null) {
										log.debug("find the id " + id);
										break;
									}
								}
							}// if currentReturnField != null)
						}// for
					}// if returnFieldList !=null
				}// if namespace !=null
			}// for i < rtrmn.length
		} // if structure != null
		log.debug("the final id  for given namespace" + givenNameSpace
				+ " and given return field " + givenReturnField + " is " + id);
		return id;
	}

	/**
	 * Creates a the ResultRecord items (derived from Source) and adds them into
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

			ResultRecord[] recordArray = transformResultset(results, endpoint,
					container);
			if (recordArray != null) {
				// put the array into vector which will store the ResultRecord
				// from one
				// search scope
				ResultRecord.transformResultRecordArrayToVector(recordArray,
						aResultList);
			}

			return true;
		} catch (Exception ee) {
			log.debug("The error to transform from resultset to ResultRecord ",
					ee);
		}
		return false;
	}

	/**
	 * 
	 * @return Returns a unique name that describes this class, often it is the
	 *         name of the class that implements the interface
	 */
	public String getName() {
		return getClass().getName();
	}

	/**
     * 
     */
	public String getBriefName() {
		return "Ecological";
	}

	/**
	 * 
	 * @return returns the number of results that for this data.
	 */
	public int getNumResults() {
		return _numResults;
	}

}// Eml200EcoGridQueryTransfer