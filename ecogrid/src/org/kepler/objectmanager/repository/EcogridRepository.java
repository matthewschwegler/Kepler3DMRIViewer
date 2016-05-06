/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2014-04-21 16:34:35 -0700 (Mon, 21 Apr 2014) $' 
 * '$Revision: 32688 $'
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

package org.kepler.objectmanager.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.EcogridObjType;
import org.ecoinformatics.ecogrid.authenticatedqueryservice.AuthenticatedQueryServiceClient;
import org.ecoinformatics.ecogrid.authenticatedqueryservice.AuthenticatedQueryServiceGetToStreamClient;
import org.ecoinformatics.ecogrid.client.AuthenticationServiceClient;
import org.ecoinformatics.ecogrid.client.IdentifierServiceClient;
import org.ecoinformatics.ecogrid.client.PutServiceClient;
import org.ecoinformatics.ecogrid.client.RegistryServiceClient;
import org.ecoinformatics.ecogrid.queryservice.QueryServiceClient;
import org.ecoinformatics.ecogrid.queryservice.query.QueryType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetType;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecord;
import org.ecoinformatics.ecogrid.queryservice.stub.QueryServiceStub;
import org.ecoinformatics.ecogrid.registry.stub.RegistryEntryType;
import org.kepler.authentication.AuthenticationException;
import org.kepler.authentication.AuthenticationManager;
import org.kepler.authentication.ProxyEntity;
import org.kepler.gui.CanvasDropTargetListener;
import org.kepler.kar.KARFile;
import org.kepler.kar.karxml.KarXml;
import org.kepler.objectmanager.lsid.KeplerLSID;

import util.StaticUtil;

/**
 * This class represents an ecogrid repository
 * 
 * @author Chad Berkley
 */
public class EcogridRepository extends Repository {
	private static final Log log = LogFactory.getLog(EcogridRepository.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
	private static final String WORKFLOWRUNTYPE = "org.kepler.util.WorkflowRun";

	private String ECOGRIDPUTSERVER;
	private String ECOGRIDLSIDSERVER;
	private String ECOGRIDQUERYSERVER;
	private String ECOGRIDREGAUTHSERVER;
	private String ECOGRIDAUTHORIZATIONSERVER;
	private String ECOGRIDAUTHENTICATEDQUERYSERVICE;
	private String regsessionid;

	public EcogridRepository(String name, String repository, String putPath,
			String authDomain, String lsidPath, String queryPath, String authenticatedQueryPath, 
			String authorizationPath, String registry, String registryauth,
			String authProtocol, String lsidAuthority) {
		super(name, repository, putPath, authDomain, lsidPath, queryPath, authenticatedQueryPath,
				authorizationPath, registry, registryauth, authProtocol, lsidAuthority);
		ECOGRIDPUTSERVER = authProtocol +"://"+ repository + putPath;
		ECOGRIDLSIDSERVER = authProtocol +"://" + repository + lsidPath;
		ECOGRIDQUERYSERVER = authProtocol +"://"+ repository + queryPath;
		ECOGRIDAUTHORIZATIONSERVER = authProtocol +"://"+ repository + authorizationPath;
		ECOGRIDREGAUTHSERVER = registryauth;
		ECOGRIDAUTHENTICATEDQUERYSERVICE = authProtocol +"://"+ repository + authenticatedQueryPath;

		if (isDebugging) {
			log.debug("repository initialized with put server: "
					+ ECOGRIDPUTSERVER);
			log.debug("repository initialized with lsid server: "
					+ ECOGRIDLSIDSERVER);
			log.debug("repository initialized with query server: "
					+ ECOGRIDQUERYSERVER);
			log.debug("repository initialized with registry: " + registry);
		}

		CanvasDropTargetListener cdtListener = CanvasDropTargetListener
				.getInstance();
		cdtListener
				.registerListener(new EcogridRepositoryKARDownloadListener());
	}

	/**
	 * return the url for the lsid service associated with this repository
	 */
	public String getLSIDServerURL() {
		return ECOGRIDLSIDSERVER;
	}
	
	/**
	 * Get the url for the authorization service associated with this repository
	 * @return
	 */
	public String getAuthorizationServerURL(){
	  return ECOGRIDAUTHORIZATIONSERVER;
	}

	/**
	 * Search the repository and return an iterator of EcogridRepositoryResults.
	 * 
	 * @see EcogridRepositoryResults
	 * @param queryString
	 *            a string to search for
	 * @param authenticate
	 * 			boolean
	 * @return null if there are no results for the query. An iterator of
	 *         EcogridRepositoryResults if there are results.
	 * @throws AuthenticationException 
	 */
	public Iterator<EcogridRepositoryResults> search(String queryString, boolean authenticate)
			throws RepositoryException, AuthenticationException {
		if (isDebugging) {
			log.debug("search(\"" + queryString + "\")");
		}
		
		Vector<EcogridRepositoryResults> resultsVector = new Vector<EcogridRepositoryResults>();
		
			//System.out.println("EcogridRepository search(queryString,"+authenticate+") querying with queryString:" + 
			//		queryString);
			ResultsetType rst = arbitrarySearch(buildQueryDoc(queryString), authenticate);
			if (rst == null) { // check to see if the resultsettype is null
				return null;
			}

			ResultsetTypeRecord[] records = rst.getRecord();
			if (records == null || records.length == 0) { 
				// check to see if there are records
				return null;
			}

			if (isDebugging) {
				log.debug("There are " + records.length + " records");
			}
			
			// create the EcogridRepositoryResult object and put it in the
			// vector
			for (int i = 0; i < records.length; i++) {
				try { // catch this here so one result can't hose the whole
					// resultset					
					List<EcogridRepositoryResults> results = 
						EcogridRepositoryResults.parseKarXml(
								records[i].getIdentifier(), name, i, authenticate);
					
					resultsVector.addAll(results);
				} catch (Exception e) {
					System.out.println("could not load result: "
							+ records[i].toString() + "  error: "
							+ e.getMessage());
				}
				// ResultsetTypeRecord currentRecord = records[i];
			}

			return resultsVector.iterator();
	}
	
	/**
	 * Search the repository and return an iterator of EcogridRepositoryResults.
	 * 
	 * @param queryDocument
	 *            - the query document to give the QueryServiceClient
	 * @return
	 * @throws RepositoryException
	 * @throws AuthenticationException
	 */
	public Iterator<EcogridRepositoryResults> advancedSearch(
			Reader queryDocument, boolean authenticate)
			throws RepositoryException, AuthenticationException {

		Vector<EcogridRepositoryResults> resultsVector = new Vector<EcogridRepositoryResults>();

		ResultsetType rst = arbitrarySearch(queryDocument, authenticate);
		if (rst == null) { // check to see if the resultsettype is null
			return null;
		}

		ResultsetTypeRecord[] records = rst.getRecord();
		if (records == null || records.length == 0) { // check to see if
			// there are records
			return null;
		}

		if (isDebugging) {
			log.debug("There are " + records.length + " records");
		}

		// create the EcogridRepositoryResult object and put it in the
		// vector
		for (int i = 0; i < records.length; i++) {
			try { // catch this here so one result can't hose the whole
					// resultset
				List<EcogridRepositoryResults> results = EcogridRepositoryResults
						.parseKarXml(records[i].getIdentifier(), name, i,
								authenticate);
				resultsVector.addAll(results);
			} catch (Exception e) {
				System.out.println("could not load result: "
						+ records[i].toString() + "  error: " + e.getMessage());
			}
			// ResultsetTypeRecord currentRecord = records[i];
		}

		return resultsVector.iterator();
	}
	
	/**
	 * Search the repository using queryDocument.
	 * @param queryDocument
	 * @return ResultsetType from QueryServiceClient
	 * @throws RepositoryException
	 */
	private ResultsetType arbitrarySearch(Reader queryDocument)
			throws RepositoryException {

		try {
			QueryServiceClient qclient = new QueryServiceClient(
					ECOGRIDQUERYSERVER);
			ResultsetType rst = qclient.query(queryDocument);
			return rst;
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RepositoryException(
					"Error searching ecogrid repository: " + e.getMessage());
		}
	}
	

	/**
	 * Search the repository using queryDocument.
	 * 
	 * @param queryDocument
	 * @param authenticate
	 * @return ResultsetType, or null
	 * @throws RepositoryException
	 * @throws AuthenticationException
	 */
	public ResultsetType arbitrarySearch(Reader queryDocument,
			boolean authenticate) throws RepositoryException,
			AuthenticationException {

		if (!authenticate) {
			return arbitrarySearch(queryDocument);
		}

		String sessionId = authenticate();
		if (sessionId == null || sessionId.isEmpty()) {
			return null;
		}

		QueryType queryType = AuthenticatedQueryServiceClient
				.reader2QueryType(queryDocument);
		try {
			AuthenticatedQueryServiceClient authQueryClient = new AuthenticatedQueryServiceClient(
					ECOGRIDAUTHENTICATEDQUERYSERVICE);
			ResultsetType rst = authQueryClient.query(queryType, sessionId);

			return rst;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RepositoryException(
					"Error searching ecogrid repository: " + e.getMessage());
		}
	}

	/**
	 *
	 * @return sessionId, or null
	 * @throws AuthenticationException 
	 */
	private String authenticate() throws AuthenticationException {
		AuthenticationManager authManager = AuthenticationManager.getManager();
		// first peek to see if already authenticated
		ProxyEntity proxy;
		
		proxy = authManager.peekProxy(authDomain);

		// authenticate if necessary
		if (proxy == null) {
			proxy = authManager.getProxy(authDomain);
		}
		return proxy.getCredential();
	}
	
	/**
	 * return an object using a ecogrid docid identifier
	 * @throws AuthenticationException 
	 */
	public InputStream get(String docid, boolean authenticate) throws RepositoryException, AuthenticationException {
		
			if (!authenticate){
				return get(docid);
			}
			
			String sessionId = authenticate();
			if (sessionId == null || sessionId.isEmpty()){
				return null;
			}
			try {
			URL url = new URL(ECOGRIDAUTHENTICATEDQUERYSERVICE);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			AuthenticatedQueryServiceGetToStreamClient aqsgtsc = 
				new AuthenticatedQueryServiceGetToStreamClient(url);
			aqsgtsc.get(docid, sessionId, outputStream);
			outputStream.close();

			return new ByteArrayInputStream(outputStream.toByteArray());
			
		} catch (Exception e) {
			throw new RepositoryException("Error getting docid " + docid + ": "
					+ e.getMessage());
		}
	}
	
	/**
	 * return an object using a ecogrid docid identifier
	 */
	private InputStream get(String docid) throws RepositoryException {
		try {
			QueryServiceStub client = new QueryServiceStub(new URL(
					ECOGRIDQUERYSERVER), null);
			byte[] b = client.get(docid);
			return new ByteArrayInputStream(b);
		} catch (Exception e) {
			throw new RepositoryException("Error getting docid " + docid + ": "
					+ e.getMessage());
		}
	}

	/**
	 * Return karXml for a docid from a repositoryName. Move this method
	 * to another class if you can think of a better home.
	 * 
	 * @param docid
	 * @param repositoryName
	 * @return
	 * @throws RepositoryException
	 */
	public static KarXml getKarXml(String docid, String repositoryName, boolean authenticate) 
		throws RepositoryException {

		try {
			RepositoryManager rm = RepositoryManager.getInstance();
			Repository rep = rm.getRepository(repositoryName);
			if (!(rep instanceof EcogridRepository)) {
				log.error("EcogridRepository getKarXml is trying to search "
						+ "a non-Ecogrid Repository");
				return null;
			}
			EcogridRepository erep = (EcogridRepository) rep;
			InputStream is = erep.get(docid, authenticate);

			return KarXml.of(is);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	/**
	 * return the object from the repository that has the given lsid
	 * @throws AuthenticationException 
	 */
	public InputStream get(KeplerLSID lsid, boolean authenticate) throws RepositoryException, AuthenticationException {
		/*
		 * NOTE: this is a bad way to do this. this should use the lsid
		 * authority to pull the object from the grid. For the sake of getting
		 * something working, i'm leaving this in here for now. -CB
		 */
		String docid = lsid.getNamespace() + "." + lsid.getObject() + "."
				+ lsid.getRevision();
		return get(docid, authenticate);
	}

	/**
	 * put a file with a predetermined sessionid
	 */
	public void put(Object o, KeplerLSID lsid, String sessionId)
			throws RepositoryException {
		try {
			String docid = lsid.getNamespace() + "." + lsid.getObject() + "."
					+ lsid.getRevision();
			if (o instanceof File) {
				uploadDataFile(((File) o), docid, sessionId);
			} else {
				uploadMetadata(o.toString(), docid, sessionId);
			}
		} catch (Exception e) {
			throw new RepositoryException(e.getMessage());
		}
	}

	/**
	 * returns the next object for the given lsid
	 * 
	 * @param lsid
	 *            the lsid to get the next object for
	 */
	public String getNextObject(KeplerLSID lsid) throws RepositoryException {
		try {
			IdentifierServiceClient lsidClient = new IdentifierServiceClient(
					ECOGRIDLSIDSERVER);
			return lsidClient.getNextObject(lsid.toString());
		} catch (Exception e) {
			throw new RepositoryException("Error getting next object: "
					+ e.getMessage());
		}
	}

	/**
	 * returns the next revision for the given lsid
	 * 
	 * @param lsid
	 *            the lsid to get the next revision for
	 */
	public String getNextRevision(KeplerLSID lsid) throws RepositoryException {
		try {
			IdentifierServiceClient lsidClient = new IdentifierServiceClient(
					ECOGRIDLSIDSERVER);
			return lsidClient.getNextRevision(lsid.toString());
		} catch (Exception e) {
			throw new RepositoryException("Error getting next revision: "
					+ e.getMessage());
		}
	}

	// ////////////////////////////////////////////////////
	// ///// ecogrid registry methods /////////

	/**
	 * add an ecogrid registry entry.
	 * 
	 * @param registryEntry
	 *            an xml file that conforms to
	 *            seek/project/ecogrid/src/xsd_reg/RegistryEntryType.xsd
	 * @param sessionid
	 *            the session id to use to authenticate
	 */
	public KeplerLSID addRegistryEntry(RegistryEntryType registryEntry, String sessionId)
			throws RepositoryException {
		try {
			RegistryServiceClient client = new RegistryServiceClient(registry);
			// String docid = client.add(sessionId, new
			// StringReader(registryEntry));
			String docid = client.add(sessionId, registryEntry);
			KeplerLSID lsid = new KeplerLSID(docid, "kepler-project.org");
			return lsid;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RepositoryException("Error adding registry entry: "
					+ e.getMessage());
		}
	}

	/**
	 * update an ecogrid registry entry.
	 * 
	 * @param registryEntry
	 *            an xml file that conforms to
	 *            seek/project/ecogrid/src/xsd_reg/RegistryEntryType.xsd
	 * @param lsid
	 *            the id of the entry
	 * @param sessionid
	 *            the session id to use to authenticate
	 */
	public void updateRegistryEntry(String registryEntry, KeplerLSID lsid)
			throws RepositoryException {
		throw new RepositoryException("Not yet implemented.");
	}

	/**
	 * remove an ecogrid registry entry.
	 * 
	 * @param lsid
	 *            the id of the entry
	 * @param sessionid
	 *            the session id to use to authenticate
	 */
	public void removeRegistryEntry(KeplerLSID lsid, String sessionId) throws RepositoryException {
		try {
			String newdocid = lsid.getNamespace() + "." + lsid.getObject()
					+ "." + lsid.getRevision();
			RegistryServiceClient client = new RegistryServiceClient(registry);
			String docid = client.remove(sessionId, newdocid);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RepositoryException(
					"Error removing ecogrid registry entry.");
		}
	}

	/**
	 * login to the registry
	 */
	public String loginRegEcoGrid(String userName, String password)
			throws Exception {
		AuthenticationServiceClient client = new AuthenticationServiceClient(
				ECOGRIDREGAUTHSERVER);
		regsessionid = client.login_action(userName, password);
		return regsessionid;
	}

	/*
	 * Method to upload data
	 */
	private void uploadDataFile(File localFile, String docid,
			String sessionId) throws Exception {
		String localFilePath = localFile.getAbsolutePath();
		String localFileName = localFile.getName();
		
		int type = EcogridObjType.DATA;
		PutServiceClient client = new PutServiceClient(ECOGRIDPUTSERVER);
		byte[] data = StaticUtil.getBytesArrayFromFile(localFilePath);
		client.put(data, docid, localFileName, type, sessionId);
	}

	/*
	 * Method to upload metadata
	 */
	private void uploadMetadata(String metadataContent, String docid,
			String sessionId) throws Exception {
		int type = EcogridObjType.METADATA;
		byte[] content = metadataContent.getBytes();
		PutServiceClient client = new PutServiceClient(ECOGRIDPUTSERVER);
		client.put(content, docid, type, sessionId);
	}

	/**
	 * builds an ecogrid query document with the user's query string in it.
	 */
	private Reader buildQueryDoc(String queryString) {
		
		// we query all KAR namespaces
		Iterator<String> namespaceItr = KARFile.getKARNamespaces().iterator();
		
		StringBuffer sb = new StringBuffer();
		sb
				.append("<egq:query queryId=\"test.1.1\" system=\"http://knb.ecoinformatics.org\" ");
		sb.append("xmlns:egq=\"http://ecoinformatics.org/query-1.0.1\" ");
		sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
		sb
				.append("xsi:schemaLocation=\"http://ecoinformatics.org/query-1.0.1 ../../src/xsd/query.xsd\">\n");
		//sb.append("<namespace prefix=\"kepler\">kar</namespace>\n");
		
		while (namespaceItr.hasNext()){
			sb.append("<namespace prefix=\"kepler\">");
			sb.append(namespaceItr.next());
			sb.append("</namespace>\n");
		}
		sb.append("<returnField>/entity/@name</returnField>\n");
		// sb.append("<returnField>/entity/property/@name</returnField>\n");
		// sb.append("<returnField>/entity/property/@value</returnField>\n");
		sb.append("<title>kepler query</title>\n");
		sb.append("<AND>\n");
		sb.append("   <condition operator=\"LIKE\" concept=\"/\">%"
				+ queryString + "%</condition>\n");
		/// filter out kars that contain WorkflowRun, likely enable this once it's user toggle-able:
		///sb.append("   <condition operator=\"NOT LIKE\" concept=\"karEntry/karEntryAttributes/type\">"
        ///+ WORKFLOWRUNTYPE + "</condition>\n");
		sb.append("</AND>\n");
		sb.append("</egq:query>\n");
		return new StringReader(sb.toString());
	}

	/**
	 * a class to hold a registry client and a sessionid
	 */
	public class RegistryClientContainer {
		public RegistryServiceClient client;
		public String sessionid;

		/**
		 * Constructor
		 */
		public RegistryClientContainer(RegistryServiceClient client,
				String sessionid) {
			this.client = client;
			this.sessionid = sessionid;
		}
	}
}
