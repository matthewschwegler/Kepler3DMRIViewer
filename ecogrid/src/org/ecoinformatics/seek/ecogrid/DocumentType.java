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

package org.ecoinformatics.seek.ecogrid;

import java.io.Serializable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.seek.ecogrid.exception.UnrecognizedDocumentTypeException;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

/**
 * This class express a DocumentType object. It has three filed. The namespace -
 * the document type The label - readable name for namespace The
 * metadataSepecificationClass - a java class implemented for this namespace
 * 
 * @author Jing Tao
 * 
 */

public class DocumentType implements Serializable {
	private String label = null;
	private String namespace = null;
	private String metadataSpecificationClassName = null;
	private static Log log;
	private static boolean isDebugging;

	static {
		log = LogFactory.getLog("org.ecoinformatics.seek.ecogrid.DocumentType");
		isDebugging = log.isDebugEnabled();
	}

	/**
	 * In this class we only need two parameters namepace and readable label. If
	 * readable label is null, namespace itself will be label. The name of
	 * metacataspaecifiaction class will be read from config file.
	 * 
	 * @param namespace
	 *            String the namespace of the document
	 * @param label
	 *            String the readable name of the document
	 * @throws UnrecognizedDocumentTypeException
	 */
	public DocumentType(String namespace, String label)
			throws UnrecognizedDocumentTypeException {
		if (namespace == null || namespace.trim().equals("")) {
			throw new UnrecognizedDocumentTypeException(
					"Namespace couldn't be null" + " for a document type");
		}
		this.namespace = namespace;
		if (label != null && !label.trim().equals("")) {
			this.label = label;
		} else {
			label = namespace;
		}
		if (isDebugging) {
			log.debug("The namespace for this document type is "
					+ namespace.toString());
			log.debug("The label for this doucment type is " + label);
		}
		readMetadataSpecificationClassName();
	}// DocumentType

	/**
	 * Copy constructor
	 * 
	 * @param myDocumentType
	 *            DocumentType
	 */
	public DocumentType(DocumentType myDocumentType) {
		this.namespace = myDocumentType.getNamespace();
		this.label = myDocumentType.getLabel();
		this.metadataSpecificationClassName = myDocumentType
				.getMetadataSpecificationClassName();
	}

	/*
	 * Get a metadata specification class name from config file
	 */
	private void readMetadataSpecificationClassName()
			throws UnrecognizedDocumentTypeException {
		String xpath = "//" + MetadataSpecificationInterface.ECOGRIDPATH + "/"
				+ MetadataSpecificationInterface.METADATASPECIFICATIONCLASSLIST
				+ "/" + MetadataSpecificationInterface.METADATACLASS + "[@"
				+ MetadataSpecificationInterface.NAMESPACE + "='" + namespace
				+ "']";
		String error = "Could NOT find the class name for metadata specification "
				+ "in ecogrid configure file ";
    ConfigurationProperty ecogridProperty = ConfigurationManager
        .getInstance().getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty cp  = ecogridProperty.getProperty(
      MetadataSpecificationInterface.METADATASPECIFICATIONCLASSLIST);
    ConfigurationProperty specProp = (ConfigurationProperty)
      cp.findProperties(MetadataSpecificationInterface.NAMESPACE, namespace, true).get(0);
    
		// check if the return vector is null or not
		if (specProp == null) {

			throw new UnrecognizedDocumentTypeException(error);
		}

    metadataSpecificationClassName = specProp.getProperty("value").getValue();
		// make sure metadataspecification class name is not null
		if (metadataSpecificationClassName == null
				|| metadataSpecificationClassName.trim().equals("")) {
			throw new UnrecognizedDocumentTypeException(error);
		}
		if (isDebugging) {
			log.debug("The metadata specification class name is "
					+ metadataSpecificationClassName);
		}

	}// readMetadataSpecificationClassName

	/**
	 * Method to get namespace of the document type
	 * 
	 * @return String
	 */
	public String getNamespace() {
		return this.namespace;
	}// getNamespace

	/**
	 * Method to set namespace for this doucmment type
	 * 
	 * @param namespace
	 *            String
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}// setNamespace

	/**
	 * Method to get the readable name of this document type
	 * 
	 * @return String
	 */
	public String getLabel() {
		return this.label;
	}// getLabel

	/**
	 * Method to set a readable name for this document type
	 * 
	 * @param label
	 *            String
	 */
	public void setLabel(String label) {
		this.label = label;
	}// setLabel

	/**
	 * Method to get metadata specification class name for this document type
	 * 
	 * @return String
	 */
	public String getMetadataSpecificationClassName() {
		return this.metadataSpecificationClassName;
	}// getMetadataSpecificationClassName

	/**
	 * Method to set metadata specification class name
	 */
	public void setMetadataSepcificationClassName(
			String metadataSpecificationClassName) {
		this.metadataSpecificationClassName = metadataSpecificationClassName;
	}

	public void print() {
		if (isDebugging) {
			log.debug("Namespace of document type: " + namespace);
			log.debug("Label of document type: " + label);
			log.debug("Metadata specification class name "
					+ metadataSpecificationClassName);
		}
	}// print

	/**
	 * Static method to transform a vector to document type array
	 * 
	 * @param list
	 *            Vector
	 * @return DocumentType[]
	 */
	public static DocumentType[] tranformVectorToArray(Vector list) {
		if (list == null) {
			return null;
		}
		int size = list.size();
		DocumentType[] array = new DocumentType[size];
		for (int i = 0; i < size; i++) {
			array[i] = (DocumentType) list.elementAt(i);
		}
		return array;
	}

}// DocumentType