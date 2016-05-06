/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2012-06-07 13:57:40 -0700 (Thu, 07 Jun 2012) $' 
 * '$Revision: 29891 $'
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

package org.kepler.kar;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.client.IdentifierServiceClient;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.objectmanager.lsid.LSIDGenerator;
import org.kepler.objectmanager.repository.Repository;

/**
 * This action imports a kar file to the system
 * 
 *@author Chad Berkley, Aaron Schultz
 *@created 10/11/2006
 */
public class UploadToRepository {

	private static final Log log = LogFactory.getLog(UploadToRepository.class);
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * The repository that we are uploading to.
	 */
	private Repository _repository;

	/**
	 * The KARFile that we are uploading.
	 */
	private KARFile _karFile;

	/**
	 * Whether or not this KAR is public.
	 */
	private boolean publicFile = false;

	/**
	 * The session id to use with the webservice.
	 */
	private String _sessionId;

	// Accessor methods

	public String getSessionId() {
		return _sessionId;
	}

	public void setSessionId(String sessionId) {
		_sessionId = sessionId;
	}

	public boolean isPublicFile() {
		return publicFile;
	}

	public void setPublicFile(boolean isPublic) {
		this.publicFile = isPublic;
	}

	/**
	 * 
	 * @return Repository where we are uploading the file to
	 */
	public Repository getRepository() {
		return _repository;
	}

	/**
	 * Set the Repository to upload the KARFile to.
	 * 
	 * @param repository
	 */
	public void setRepository(Repository repository) {
		this._repository = repository;
	}

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public UploadToRepository(File karFile) {

		try {
			_karFile = new KARFile(karFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public boolean isAlreadyRegistered(KeplerLSID lsid) throws Exception {
		IdentifierServiceClient identificationClient = new IdentifierServiceClient(
				_repository.getLSIDServerURL());
		return identificationClient.isRegistered(lsid.toString());
	}

	/**
	 * Use the given NamedObj to generate the metadata for metacat.
	 * 
	 * @param object
	 * */
	public boolean uploadMetadata(String metadata) {

		try {

			LSIDGenerator lsidGen = LSIDGenerator.getInstance();
			KeplerLSID metadataLSID = lsidGen.getNewLSID();

			// check if it exists
			if (isAlreadyRegistered(metadataLSID)) {
				if (isDebugging)
					log.debug("metadata already exists for this lsid: "
							+ metadataLSID);
				return false;
			}

			if (isDebugging)
				log.debug("uploading actor metadata with id " + metadataLSID
						+ " using sessionid " + getSessionId());
			_repository.put(metadata, metadataLSID, getSessionId());
			if (isDebugging)
				log.debug("uploaded actor metadata with id " + metadataLSID);

			// create an access file so we can make this entity public on
			// the ecogrid
			// Why are we using a new LSID here? -aaron
			KeplerLSID accessLSID = LSIDGenerator.getInstance().getNewLSID();

			// put the access file for the metadata
			if (isDebugging)
				log.debug("uploading access file for AM with id " + accessLSID);
			String accessDoc = buildAccessDocument(metadataLSID, isPublicFile());
			_repository.put(accessDoc, accessLSID, getSessionId());
			if (isDebugging)
				log.debug("uploaded access file for AM with id " + accessLSID);

			// register an id for the access file for the kar
			KeplerLSID karAccessLSID = lsidGen.getNewLSID();

			// put the access file for the kar file
			if (isDebugging)
				log.debug("uploading access file for kar with access id "
						+ karAccessLSID);

			String karAccessDoc = buildAccessDocument(_karFile.getLSID(),
					isPublicFile());
			_repository.put(karAccessDoc, karAccessLSID, getSessionId());
			if (isDebugging)
				log.debug("uploaded access file for kar with id "
						+ karAccessLSID);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	/**
	 * uploads a single object to the repository
	 * 
	 * @return true if the upload was succesful
	 */
	public boolean uploadFile() throws Exception {
		if (isDebugging)
			log.debug("uploadFile()");

		KeplerLSID karLSID = _karFile.getLSID();

		if (isAlreadyRegistered(karLSID)) {
		  log.warn("The kar file has already been registered and can't be uploaded again");
			return false;
		}

		if (isDebugging)
			log.debug("uploading kar file with id " + karLSID);

		_repository.put(_karFile.getFileLocation(), karLSID, getSessionId());
		if (isDebugging)
			log.debug("uploaded kar file with id " + karLSID);

		return true;
	}

	/**
	 * uploads a document as the kepler user with public read permissions
	 */
	public static String buildAccessDocument(KeplerLSID lsid) {
		return buildAccessDocument(lsid, null, true);
	}

	public static String buildAccessDocument(KeplerLSID lsid, boolean publicDoc) {
		return buildAccessDocument(lsid, null, publicDoc);
	}

	/**
	 * build an access document that gives 'public' read access to the inserted
	 * documents.
	 */
	public static String buildAccessDocument(KeplerLSID lsid, String owner,
			boolean publicDoc) {
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\"?>\n");
		sb
				.append("<eml:eml packageId=\"\" system=\"knb\" "
						+ "xmlns:eml=\"eml://ecoinformatics.org/eml-2.0.1\" "
						+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
						+ "xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.1 eml.xsd\">\n");
		sb.append("<dataset>\n");
		sb.append("<title>access for " + lsid.toString() + "</title>\n");
		sb.append("<creator id=\"1\">\n");
		sb.append("  <individualName><surName>kepler</surName>\n");
		sb.append("  </individualName>\n");
		sb.append("</creator>\n");

		sb.append("<contact><references>1</references>\n");
		sb.append("</contact>\n");

		if (publicDoc) {
			sb.append("<access authSystem=\"knb\" order=\"allowFirst\">\n");
			sb.append("  <allow>\n");
			sb.append("    <principal>public</principal>\n");
			sb.append("    <permission>read</permission>\n");
			sb.append("  </allow>\n");
			sb.append("</access>\n");
		}

		if (owner != null) {
			sb.append("<access authSystem=\"knb\" order=\"allowFirst\">\n");
			sb.append("  <allow>\n");
			sb.append("    <principal>" + owner + "</principal>\n");
			sb.append("    <permission>read</permission>\n");
			sb.append("    <permission>write</permission>\n");
			sb.append("  </allow>\n");
			sb.append("</access>\n");
		}

		sb.append("<dataTable id=\"x\">\n");

		sb.append("<entityName>asdf</entityName>\n");

		sb.append("<physical>\n");
		sb.append("  <objectName>tmp</objectName>\n");
		sb.append("  <dataFormat> \n");
		sb.append("  <externallyDefinedFormat>\n");
		sb.append("    <formatName>application/vnd.ms-excel</formatName>\n");
		sb.append("  </externallyDefinedFormat>\n");
		sb.append("  </dataFormat>\n");

		sb.append("  <distribution>\n");
		sb.append("    <online>\n");
		sb.append("      <url>ecogrid://knb/" + lsid.getNamespace() + "."
				+ lsid.getObject() + "." + lsid.getRevision() + "</url>\n");
		sb.append("    </online>\n");
		sb.append("   </distribution>\n");
		sb.append("</physical>\n");

		sb.append("<attributeList>\n");
		sb.append("  <attribute id=\"2\">\n");
		sb.append("    <attributeName>0</attributeName>\n");
		sb.append("    <attributeDefinition>0</attributeDefinition>\n");
		sb.append("    <measurementScale>\n");
		sb.append("      <interval>\n");
		sb.append("        <unit>\n");
		sb
				.append("          <standardUnit>metersPerSecondSquared</standardUnit>\n");
		sb.append("        </unit>\n");
		sb.append("        <precision>.2</precision>\n");
		sb.append("        <numericDomain>\n");
		sb.append("          <numberType>natural</numberType>\n");
		sb.append("        </numericDomain>\n");
		sb.append("      </interval>\n");
		sb.append("    </measurementScale>\n");
		sb.append("  </attribute>\n");
		sb.append("</attributeList>\n");

		sb.append("</dataTable>\n");
		sb.append("</dataset>\n");
		sb.append("</eml:eml>\n");

		return sb.toString();
	}

}