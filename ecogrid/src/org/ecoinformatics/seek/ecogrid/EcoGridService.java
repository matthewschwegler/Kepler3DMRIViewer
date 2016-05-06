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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.seek.ecogrid.exception.InvalidEcoGridServiceException;

/**
 * This class represents a ecogrid service metadata
 * 
 * @author Jing Tao
 * 
 */

public class EcoGridService implements Serializable {
	private String serviceGroup = null;
	private String description = null;

	private String serviceName = null;
	private String wsdlURL = null;
	private String serviceType = null;
	private String endPoint = null;
	private String serviceClassification = null;
	private DocumentType[] documentTypeList = null;

	private static Log log;
	private static boolean isDebugging;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.EcoGridService");
		isDebugging = log.isDebugEnabled();
	}

	/**
	 * Default constructor
	 */
	public EcoGridService() {
	}// EcoGridService

	/**
	 * Copy constructor
	 * 
	 * @param myService
	 *            EcoGridService
	 */
	public EcoGridService(EcoGridService myService) {
		this.serviceName = myService.getServiceName();
		this.serviceGroup = myService.getServiceGroup();
		this.description = myService.getDescription();
		this.wsdlURL = myService.getWsdlURL();
		this.serviceType = myService.getServiceType();
		this.endPoint = myService.getEndPoint();
		this.serviceClassification = myService.getServiceClassification();
		this.documentTypeList = myService.getDocumentTypeList();
	}

	/**
	 * Get service name
	 * 
	 * @return String
	 */
	public String getServiceName() {
		return this.serviceName;
	}// getServiceName

	/**
	 * Set service name
	 * 
	 * @param serviceName
	 *            String
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}// setServiceName

	/**
	 * Get wsdl url for this service
	 * 
	 * @return String
	 */
	public String getWsdlURL() {
		return this.wsdlURL;
	}// getWsdlURL

	/**
	 * Set wsdl url for this service
	 * 
	 * @param wsdlURL
	 *            String
	 */
	public void setWsdlURL(String wsdlURL) {
		this.wsdlURL = wsdlURL;
	}// setWsdlURL

	/**
	 * Get service type of this service
	 * 
	 * @return String
	 */
	public String getServiceType() {
		return this.serviceType;
	}// getServiceType

	/**
	 * Set service type for this service
	 * 
	 * @param serviceType
	 *            String
	 */
	public void setServiceType(String serviceType)
			throws InvalidEcoGridServiceException {
		if (serviceType == null || serviceType.trim().equals("")) {
			throw new InvalidEcoGridServiceException(
					"service type couldn't be null in ecogrid service");
		}

		this.serviceType = serviceType;
	}// setServiceType

	/**
	 * Get end point for this service
	 * 
	 * @return String
	 */
	public String getEndPoint() {
		return this.endPoint;
	}// getEndPoint

	/**
	 * Set end point for this service
	 * 
	 * @param endPoint
	 *            String
	 */
	public void setEndPoint(String endPoint)
			throws InvalidEcoGridServiceException {
		if (endPoint == null || endPoint.trim().equals("")) {
			throw new InvalidEcoGridServiceException(
					"End Point couldn't be null in ecogrid service");
		}
		this.endPoint = endPoint;
	}// setEndPoint

	/**
	 * Get service classification
	 * 
	 * @return String
	 */
	public String getServiceClassification() {
		return this.serviceClassification;
	}// getServiceClassification

	/**
	 * Set service classification
	 * 
	 * @param serviceClassification
	 *            String
	 */
	public void setServiceClassification(String serviceClassification) {
		this.serviceClassification = serviceClassification;
	}// setServiceLcassification

	/**
	 * Get document type list in this service
	 * 
	 * @return DocumentType[]
	 */
	public DocumentType[] getDocumentTypeList() {
		return documentTypeList;
	}// getDcoumentTypeList

	/**
	 * Set document type list in this service
	 * 
	 * @param documentTypeList
	 *            DocumentType[]
	 */
	public void setDocumentTypeList(DocumentType[] documentTypeList) {
		this.documentTypeList = documentTypeList;
	}// setDocumentTypeList

	/**
	 * This method will copy a ecogrid service to another one. The difference to
	 * the copy constructor is, it creates a new array for document type
	 * 
	 * @param oldService
	 *            EcoGridService
	 * @return EcoGridService
	 */
	public static EcoGridService copyEcoGridService(EcoGridService oldService)
			throws Exception {

		EcoGridService newService = new EcoGridService();
		String serviceName = oldService.getServiceName();
		newService.setServiceName(serviceName);
		String serviceType = oldService.getServiceType();
		newService.setServiceType(serviceType);
		String endpoint = oldService.getEndPoint();
		newService.setEndPoint(endpoint);
		newService.setServiceGroup(oldService.getServiceGroup());
		newService.setServiceClassification(oldService
				.getServiceClassification());
		newService.setWsdlURL(oldService.getWsdlURL());
		DocumentType[] oldArray = oldService.getDocumentTypeList();
		if (oldArray != null) {
			int length = oldArray.length;
			DocumentType[] newArray = new DocumentType[length];
			for (int i = 0; i < length; i++) {
				DocumentType oldDoc = oldArray[i];
				String namespace = oldDoc.getNamespace();
				String label = oldDoc.getLabel();
				DocumentType newDoc = new DocumentType(namespace, label);
				newArray[i] = newDoc;
			}// for
			newService.setDocumentTypeList(newArray);
		}// if
		return newService;
	}// copyEcoGridService

	/**
	 * Method to print out the service
	 */
	public void print() {
		if (!isDebugging) {
			return;
		}

		log.debug("Service Name: " + serviceName);
		log.debug("Service Group: " + serviceGroup);
		log.debug("Service WSDL: " + wsdlURL);
		log.debug("Service Type: " + serviceType);
		log.debug("End Point: " + endPoint);
		log.debug("Service Classification: " + serviceClassification);
		if (documentTypeList != null) {
			for (int i = 0; i < documentTypeList.length; i++) {
				DocumentType type = documentTypeList[i];
				if (type != null) {
					type.print();
				}// if
			}// for
		}// if
	}// print

	public String getServiceGroup() {
		return this.serviceGroup;
	}

	public void setServiceGroup(String name) {
		this.serviceGroup = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}// EcoGridService