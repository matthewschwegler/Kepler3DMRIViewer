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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.ecoinformatics.ecogrid.registry.stub.RegistryEntryType;
import org.ecoinformatics.ecogrid.registry.stub.RegistryEntryTypeDocumentType;
import org.ecoinformatics.seek.ecogrid.exception.InvalidEcoGridServiceException;
import org.kepler.authentication.AuthenticationException;
import org.kepler.authentication.AuthenticationManager;
import org.kepler.authentication.Domain;
import org.kepler.authentication.DomainList;
import org.kepler.authentication.ProxyEntity;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.util.DotKeplerManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ptolemy.util.MessageHandler;

/**
 * This class will control the current service user want to search: user can
 * add, remove and update the service list
 * 
 * @author leinfelder (modified from jing tao's version)
 * 
 */

public class EcoGridServicesController {

	protected static Log log;
	protected static boolean isDebugging;
	protected static final String TRUE = "true";
	protected static final String FALSE = "false";

	private String _saveFileName;
	
	public static final String AUTHENTICATEDQUERYSERVICETYPE = "http://ecoinformatics.org/authenticatedqueryservice-1.0.0";
	public static final String QUERYSERVICETYPE = "http://ecoinformatics.org/queryservice-1.0.0";
	public static final String AUTHSERVICETYPE = "http://ecoinformatics.org/authenticationservice-1.0.0";
	public static final String PUTSERVICETYPE = "http://ecoinformatics.org/putservice-1.0.0";
	public static final String IDENTIFIERSERVICETYPE = "http://ecoinformatics.org/identifierservice-1.0.0";

	static {
		log = LogFactory.getLog(EcoGridServicesController.class);
		isDebugging = log.isDebugEnabled();
	}

	// list user selected
	private Vector selectedSearchingServicesList = new Vector();

	// the list in configure file
	private Vector<EcoGridService> currentServicesList = new Vector();

	// the types to include in the listing
	private Vector serviceTypes = new Vector();

	// maximum number of times we retry authentication
	private int maxLoginAttempt = 3;

	private static EcoGridServicesController controller = null;

	// restrict the use of this class to singleton
	private EcoGridServicesController() {
		// Set up file name for storing default local save directory
		File modDir = DotKeplerManager.getInstance()
				.getTransientModuleDirectory("ecogrid");
		if (modDir != null) {
			_saveFileName = modDir.toString();
		} else {
			_saveFileName = System.getProperty("KEPLER");
		}
		if (!_saveFileName.endsWith(File.separator)) {
			_saveFileName += File.separator;
		}
		_saveFileName += "EcoGridServices";
	}

	/**
	 * Method to get an object from this singleton class
	 * 
	 */
	public static EcoGridServicesController getInstance() {
		if (controller == null) {
			controller = new EcoGridServicesController();
			controller.init();
		}
		return controller;
	}

	private static String lookupAuthenticationDomainMapping(String serviceType) {
		// the get the key=value map for type=class
		String parentXPath = "//" + MetadataSpecificationInterface.ECOGRIDPATH
				+ "/" + MetadataSpecificationInterface.AUTHENTICATION_MAPPING;
		
    
    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty authMapping = ecogridProperty.findProperties("serviceType", serviceType, true).get(0);
    return authMapping.getProperty("serviceClass").getValue();
	}

	protected void init() {
		// include all service types for the generic controller
		serviceTypes.add(QUERYSERVICETYPE);
		serviceTypes.add(AUTHENTICATEDQUERYSERVICETYPE);
		serviceTypes.add(AUTHSERVICETYPE);
		serviceTypes.add(PUTSERVICETYPE);
		serviceTypes.add(IDENTIFIERSERVICETYPE);
		//readServicesFromConfig();
		readServices();
	}

	/**
	 * Remove a given service from current query service list. The key for
	 * removing is endpoint. If endpoint is same, we think they are same service
	 * 
	 * @param service
	 *            EcoGridService
	 */
	public void removeService(EcoGridService service) {
		if (service == null) {
			// if service is null, we don't need do anything
			return;
		}// if
		else {
			String endPoint = service.getEndPoint();
			if (endPoint == null || endPoint.trim().equals("")) {
				return;
			}// if
			else {
				// go through the current query service list and find a servcie
				// which has same endpoint to the given servcie. The remove it.
				int size = currentServicesList.size();
				for (int i = 0; i < size; i++) {
					EcoGridService currentService = (EcoGridService) currentServicesList
							.elementAt(i);
					if (currentService != null) {
						String currentEndPoint = currentService.getEndPoint();
						if (currentEndPoint != null
								&& currentEndPoint.equals(endPoint)) {
							currentServicesList.remove(i);
							if (isDebugging) {
								log.debug("Delete service "
										+ service.getServiceName()
										+ " from list");
							}
							return;
						}// if
					}// if
				}// for
			}// else
		}// else
	}// removeService

	/**
	 * Add a service to service list. If new service has same endpoints to
	 * service already in current. Then we will compare document type array and
	 * add new document type into service If the serviceType is not a in our
	 * list, it will throw an exception
	 * 
	 * @param service
	 *            EcoGridService
	 * @throws InvalidEcoGridServiceException
	 */
	public void addService(EcoGridService service)
			throws InvalidEcoGridServiceException {
		String serviceType = service.getServiceType();
		if (serviceType == null || !serviceTypes.contains(serviceType)) {
			throw new InvalidEcoGridServiceException(
					"The service type is invalid or null: " + serviceType
							+ ".  Couldn't be added to list");
		}// if

		// isDuplicateService
		int index = isDuplicateService(service, currentServicesList);
		if (index == -1) {
			// it is new end point
			if (isDebugging) {
				log.debug("Add service " + service.getServiceName()
						+ " into list");
			}
			currentServicesList.add(service);
		} else {
			// compare document type list if new service has same endpoint
			EcoGridService currentService = (EcoGridService) currentServicesList
					.elementAt(index);
			DocumentType[] currentDocTypeList = currentService
					.getDocumentTypeList();
			DocumentType[] newDocTypeList = service.getDocumentTypeList();
			if (currentDocTypeList == null || currentDocTypeList.length == 0) {
				// if current service doesn't have any document type, just set
				// the new one
				currentService.setDocumentTypeList(newDocTypeList);
			} else if (newDocTypeList != null) {
				int sizeOfnew = newDocTypeList.length;
				int sizeofCurrent = currentDocTypeList.length;
				Vector newDocTypeVector = new Vector();
				// go through new document type
				for (int j = 0; j < sizeOfnew; j++) {
					boolean existed = false;
					DocumentType newType = newDocTypeList[j];
					if (newType == null) {
						continue;
					}
					String newNamespace = newType.getNamespace();
					if (newNamespace == null || newNamespace.trim().equals("")) {
						continue;
					}
					for (int i = 0; i < sizeofCurrent; i++) {
						DocumentType currentType = currentDocTypeList[i];
						if (currentType == null) {
							continue;
						} else {
							String currentNamespace = currentType
									.getNamespace();
							if (currentNamespace == null
									|| currentNamespace.trim().equals("")) {
								continue;
							} else if (currentNamespace.equals(newNamespace)) {
								existed = true;
							}//
						}// else

					}// for
					// if the namespace is a new space, add this document type
					// into the array
					if (!existed) {
						newDocTypeVector.add(newType);
					}
				}// for
				// if we do get some new document type(newDocTypeVector is not
				// empty)
				// we should a new doctype into list
				if (!newDocTypeVector.isEmpty()) {
					DocumentType[] updatedDocTypeList = addNewDocType(
							currentDocTypeList, newDocTypeVector);
					currentService.setDocumentTypeList(updatedDocTypeList);
				}// if

			}// else if
		}// else
	}// addService

	public void clearServicesList() {
		this.currentServicesList = new Vector();
		this.selectedSearchingServicesList = new Vector();
	}

	/*
	 * Method to add a vector document type to array
	 */
	protected DocumentType[] addNewDocType(DocumentType[] currentArray,
			Vector newTypeVector) {
		int arraySize = currentArray.length;
		int vectorSize = newTypeVector.size();
		int newSize = arraySize + vectorSize;
		DocumentType[] newArray = new DocumentType[newSize];
		// copy the array
		for (int i = 0; i < arraySize; i++) {
			newArray[i] = currentArray[i];
		}// for
		// copy the vector
		for (int j = 0; j < vectorSize; j++) {
			newArray[arraySize + j] = (DocumentType) newTypeVector.elementAt(j);
		}// for
		return newArray;
	}// addNewDocType

	/**
	 * Method to update a existed EcoGridService
	 * 
	 * @param service
	 *            EcoGridService
	 * @throws InvalidEcoGridServiceException
	 */
	public void updateService(EcoGridService service)
			throws InvalidEcoGridServiceException {
		if (service == null) {
			throw new InvalidEcoGridServiceException(
					"Couldn't use a null service" + " to update a list");
		}
		// make sure the service is a valid type
		String serviceType = service.getServiceType();
		String endPoint = service.getEndPoint();
		if (serviceType == null || !serviceTypes.contains(serviceType)) {
			throw new InvalidEcoGridServiceException(
					"The service type is invalid or null "
							+ "and couldn't be updated into list");
		}// if
		if (endPoint == null || endPoint.trim().equals("")) {
			throw new InvalidEcoGridServiceException(
					"End point cannot be null in "
							+ "the new service which will update the list");
		}
		boolean success = false;
		// go through the vector
		int size = currentServicesList.size();
		for (int i = 0; i < size; i++) {
			EcoGridService currentService = (EcoGridService) currentServicesList
					.elementAt(i);
			String currentEndPoint = currentService.getEndPoint();
			if (currentEndPoint != null && currentEndPoint.equals(endPoint)) {
				// replace the old one, but respect the selection status
				Object oldService = currentServicesList.remove(i);
				if (oldService instanceof SelectableEcoGridService) {
					if (service instanceof SelectableEcoGridService) {
						// set the selection status for the overall service
						((SelectableEcoGridService) service)
								.getSelectableServiceName().setIsSelected(
										((SelectableEcoGridService) oldService)
												.getSelectableServiceName()
												.getIsSelected());
						SelectableDocumentType[] documentTypeList = ((SelectableEcoGridService) service)
								.getSelectableDocumentTypeList();
						SelectableDocumentType[] oldDocumentTypeList = ((SelectableEcoGridService) oldService)
								.getSelectableDocumentTypeList();
						if (documentTypeList != null) {
							// go through the new documents and set the
							// selection status on each of them
							for (int j = 0; j < documentTypeList.length; j++) {
								// avoid arrayindexoutofbounds exceptions
								if (oldDocumentTypeList != null
										&& j >= oldDocumentTypeList.length) {
									break;
								}
								// set the selection status
								if (oldDocumentTypeList[j].getNamespace()
										.equals(
												documentTypeList[j]
														.getNamespace())) {
									documentTypeList[j]
											.setIsSelected(oldDocumentTypeList[j]
													.getIsSelected());
								}
							}
						}
					}
				}
				currentServicesList.add(i, service);
				if (isDebugging) {
					log.debug("Update the service " + service.getServiceName());
				}
				success = true;
			}
		}// for
		// couldn't find a service to update and will throw a exception
		if (!success) {
			throw new InvalidEcoGridServiceException(
					"Couldn't find a target service " + "to update in the list");
		}
	}// updateService

	/**
	 * Method to get the full service list vector
	 * 
	 * @return Vector all services in the controller
	 */
	public Vector getServicesList() {
		return this.currentServicesList;
	}

	public void setServicesList(Vector serviceList) {
		this.currentServicesList = serviceList;
	}

	/**
	 * Rather than completely overwriting the existing services, this method
	 * allows new services to be merged with the existing ones. Services with
	 * overlapping endpoints are updated to use the other attributes of the new
	 * service. If a service (endpoint) is not found in the current list, the
	 * new service is added
	 * 
	 * @param newServices
	 *            list of new EcoGridServices to merge with the existing
	 *            services
	 * @throws InvalidEcoGridServiceException
	 *             cascades up from the add() and update() methods used by this
	 *             method
	 */
	public void mergeServicesList(Vector newServices) {
		for (int i = 0; i < newServices.size(); i++) {
			// get a new service
			EcoGridService newService = (EcoGridService) newServices.get(i);
			// look it up in the list
			EcoGridService existingService = this.getService(newService
					.getEndPoint());
			if (existingService != null) {
				try {
					this.updateService(newService);
				} catch (InvalidEcoGridServiceException e) {
					// warn, but continue processing the list
					log.info("could not update service:" + e.getMessage());
					// e.printStackTrace();
				}
			} else {
				try {
					this.addService(newService);
				} catch (InvalidEcoGridServiceException e) {
					// warn, but continue processing the list
					log.info("could not add service:" + e.getMessage());
					// e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Provides an authentication credential for the given service object (if
	 * possible) Note that there may not be adequate information in the
	 * EcoGridServicesController to support authentication for every service.
	 * Additionally, authentication is not applicable for all EcoGridService
	 * types
	 * 
	 * @param service
	 *            an any given EcoGridService that might need authentication
	 *            credentials to use This service must be part of a service
	 *            group that has registered a valid authentication service that
	 *            will provide the credential for use by other service types
	 * @return an authentication credential (string) to be used by the service
	 *         given in parameter
	 */
	public synchronized String authenticateForService(EcoGridService service) {
		// authenticate for any given service
		String sessionId = "bogus!";
		String domainName = null;
		try {
			if (service != null) {
				// get the authentication domain for the service
				Domain domain = EcoGridServicesController.getInstance()
						.getServiceDomain(service);
				if (domain != null) {
					domainName = domain.getDomain();
					log.debug("domainName=" + domainName);
				}
			}
			if (domainName != null) {
				for (int i = 0; i < maxLoginAttempt; i++) {
					try {
						ProxyEntity proxy = AuthenticationManager.getManager()
								.getProxy(domainName);
						sessionId = proxy.getCredential();
						log.debug("authentication credential=" + sessionId);
						break;
					} catch (AuthenticationException ae) {
						if (ae.getType() == AuthenticationException.USER_CANCEL) {
							log.info("user cancelled the authentication");
							break;
						} else {
							MessageHandler.error("Error authenticating", ae);
							log.error("The authentication exception is ", ae);
							ae.printStackTrace();
							// continue to prompt until we've done it enough
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("There was a non-authentication exception: ", e);
			e.printStackTrace();
		}
		return sessionId;
	}

	/**
	 * 
	 * @param service
	 * 	 */
	public Domain getServiceDomain(EcoGridService service) {
		Domain domain = null;

		try {
			// first check if we have the Domain for this service group already
			domain = DomainList.getInstance().searchDomainList(
					service.getServiceGroup());
			if (domain != null) {
				return domain;
			}
		} catch (Exception e) {
			// suppose there is no pre-existing domain for this service
		}

		// look up authentication information, if it exists
		EcoGridService authService = this.getService(service.getServiceGroup(),
				AUTHSERVICETYPE);
		if (authService != null) {
			domain = new Domain();
			domain.setDomain(authService.getServiceGroup());
			domain.setServiceURL(authService.getEndPoint());
			// map the serviceType to the serviceClass for domain
			domain
					.setServiceClass(lookupAuthenticationDomainMapping(authService
							.getServiceType()));
			
			// look up default user for new/unknown services
			ConfigurationProperty ecogridProperty = 
				ConfigurationManager.getInstance().getProperty(ConfigurationManager.getModule("ecogrid"));
		    ConfigurationProperty defaultUser = ecogridProperty.getProperty("defaultUser");
		    
			String username = defaultUser.getProperty("username").getValue();
			String password = defaultUser.getProperty("password").getValue();
			domain.setUsername(username);
			domain.setPassword(password);

			// add the new domain to the list for future reference
			try {
				DomainList.getInstance().addDomain(domain);
			} catch (Exception e) {
				log.error("problem adding new Domain to DomainList: "
						+ domain.getDomain());
				e.printStackTrace();
			}

		}

		return domain;
	}

	/**
	 * Used for tying together related services Finds the EcoGridService for a
	 * given endpoint
	 * 
	 * @param endPoint
	 *            the URL of the service
	 * @return service object (EcoGridService) that shares the same endPoint
	 */
	public EcoGridService getService(String endPoint) {
		EcoGridService service = null;
		for (int i = 0; i < currentServicesList.size(); i++) {
			service = (EcoGridService) currentServicesList.get(i);
			if (service.getEndPoint().equals(endPoint)) {
				return service;
			}
		}
		return null;
	}

	/**
	 * Finds the EcoGridService for a given serviceGroup/serviceType pairing
	 * 
	 * @param serviceGroup
	 *            the name of the group of services to search within
	 * @param serviceType
	 *            the type of service to locate
	 * @return service that meets the input parameters
	 */
	public EcoGridService getService(String serviceGroup, String serviceType) {
		EcoGridService service = null;
		for (int i = 0; i < currentServicesList.size(); i++) {
			service = (EcoGridService) currentServicesList.get(i);
			if (service.getServiceGroup().equals(serviceGroup)) {
				if (service.getServiceType().equals(serviceType)) {
					return service;
				}
			}
		}
		return null;
	}

	/**
	 * Finds a list of services for a given serviceGroup name
	 * 
	 * @param serviceGroup
	 *            the name of the group of services to search within
	 * @return list of EcoGridService objects for the given serviceGroup
	 */
	public Vector getServiceGroupList(String serviceGroup) {
		Vector retList = new Vector();
		for (int i = 0; i < currentServicesList.size(); i++) {
			EcoGridService service = (EcoGridService) currentServicesList
					.get(i);
			if (service.getServiceGroup().equals(serviceGroup)) {
				retList.add(service);
			}
		}
		return retList;
	}

	/**
	 * Finds a list of services for a given serviceType name this is a
	 * convenience method for the more general form that takes a full list of
	 * types
	 * 
	 * @param serviceType
	 *            the name of the serviceType of services to search within
	 * @return list of EcoGridService objects for the given serviceType
	 */
	public Vector getServicesList(String serviceType) {
		Vector types = new Vector();
		types.add(serviceType);
		return getServicesList(types);
	}

	/**
	 * Finds a list of services with serviceTypes in the given type list
	 * 
	 * @param serviceTypeList
	 *            list of serviceTypes to match on
	 * @return list of EcoGridService objects for the given serviceTypes
	 */
	public Vector getServicesList(Vector serviceTypeList) {
		Vector retList = new Vector();
		for (int i = 0; i < currentServicesList.size(); i++) {
			EcoGridService service = (EcoGridService) currentServicesList
					.get(i);
			if (serviceTypeList.contains(service.getServiceType())) {
				retList.add(service);
			}
		}
		return retList;
	}

	/**
	 * Finds a list of query services this is a convenience method for the more
	 * general form that takes a full list of types
	 * 
	 * @return list of EcoGridService objects for querying
	 */
	public Vector getQueryServicesList() {
		Vector types = new Vector();
		types.add(AUTHENTICATEDQUERYSERVICETYPE);
		types.add(QUERYSERVICETYPE);
		return getServicesList(types);
	}

	/*
	 * Given a service node in configure file, this method will transfer it to
	 * [Selectable]EcoGridService java object
	 */
	private SelectableEcoGridService generateServiceFromServiceNode(
			ConfigurationProperty cp) {
		SelectableEcoGridService service = null;
		if (cp != null) {
			/*String serviceName = getKidNodeValue(serviceNode,
					MetadataSpecificationInterface.SERVICENAME);
			String serviceType = getKidNodeValue(serviceNode,
					MetadataSpecificationInterface.SERVICETYPE);
			String endPoint = getKidNodeValue(serviceNode,
					MetadataSpecificationInterface.ENDPOINT);
			String selection = getKidNodeValue(serviceNode,
					MetadataSpecificationInterface.SELECTION);
      String serviceGroup = getKidNodeValue(serviceNode,
					MetadataSpecificationInterface.SERVICEGROUP);
      */
      String serviceName = cp.getProperty("serviceName").getValue();
      String serviceType = cp.getProperty("serviceType").getValue();
      String endPoint = cp.getProperty("endPoint").getValue();
      String selection = "false";
      if(cp.getProperty("selected") != null)
      {
        selection = cp.getProperty("selected").getValue();
      }
      String serviceGroup = cp.getProperty("serviceGroup").getValue();
			
			// String serviceClassification = getKidNodeValue(serviceNode,
			// MetadataSpecificationInterface.SERVICECLASSIFICATION);
			// String description = getKidNodeValue(serviceNode,
			// MetadataSpecificationInterface.DESCRIPTION);

			// System.out.println("the selection for service is "+selection);
			boolean selected = false;
			if (selection != null && selection.equals(TRUE)) {
				selected = true;
			}
			SelectableDocumentType[] documentTypeList = getDocumentList(cp);

			// generate a service java object
			try {
				service = new SelectableEcoGridService();
				SelectableServiceName name = new SelectableServiceName();
				name.setServiceName(serviceName);
				name.setIsSelected(selected);
				service.setSelectableServiceName(name);
				service.setServiceName(serviceName);
				service.setServiceType(serviceType);
				service.setEndPoint(endPoint);
				service.setSelectableDocumentTypeList(documentTypeList);
				service.setDocumentTypeList(documentTypeList);
				service.setServiceGroup(serviceGroup);
				// service.setServiceClassification(serviceClassification);
				// service.setDescription(description);

			}// try
			catch (Exception e) {
				service = null;
				log.debug("couldn't get a service from configure file ", e);
			}// catch
		}// if
		return service;
	}// handleSericeNode

	/*
	 * Give a parentnode and kid node name, it will return the kid node value
	 */
	private String getKidNodeValue(Node parentNode, String kidName) {
		String nodeValue = null;
		try {
			NodeList nodeList = XPathAPI.selectNodeList(parentNode, kidName);
			if (nodeList != null && nodeList.getLength() > 0) {
				Node node = nodeList.item(0);
				if (node != null && node.getFirstChild() != null) {
					nodeValue = node.getFirstChild().getNodeValue();
				}// if
			}// if
		}// try
		catch (Exception e) {
			if (isDebugging) {
				log.debug("Couldn't find " + kidName, e);
			}
		} // catch
		if (isDebugging) {
			log.debug("The value of " + kidName + " is " + nodeValue);
		}
		return nodeValue;
	}// getKideNodeValue

	/*
	 * Given a service node and return a documentType list array
	 */
	private SelectableDocumentType[] getDocumentList(ConfigurationProperty cp) 
  {
		SelectableDocumentType[] documentList = null;
		Vector documentVector = new Vector();
		//NodeList documentTypeNodeList = null;
    List documentTypeList = cp.getProperties("documentType");
		//try {
			// get documenttype node list
			//documentTypeNodeList = XPathAPI.selectNodeList(serviceNode,
			//		MetadataSpecificationInterface.DOCUMENTTYPE);
      
		//} catch (Exception e) {
		//	log.debug("Couldn't find document list in config", e);
		//	return documentList;
		//}// catch
    if(documentTypeList == null || documentTypeList.size() == 0)
    {
      return documentList;
    }

		if (documentTypeList != null && documentTypeList.size() > 0) 
    {
			int size = documentTypeList.size();
			for (int i = 0; i < size; i++) 
      {
				boolean selected = true;
				/*Node documentTypeNode = documentTypeNodeList.item(i);
				String namespace = getKidNodeValue(documentTypeNode,
						MetadataSpecificationInterface.NAMESPACE);
				String label = getKidNodeValue(documentTypeNode,
						MetadataSpecificationInterface.LABEL);
				String selectedStr = getKidNodeValue(documentTypeNode,
						MetadataSpecificationInterface.SELECTION);
        */
        ConfigurationProperty documentTypeProp = (ConfigurationProperty)documentTypeList.get(i);
        String namespace = documentTypeProp.getProperty("namespace").getValue();
        String label = documentTypeProp.getProperty("label").getValue();
        String selectedStr = documentTypeProp.getProperty("selected").getValue();
        
				// System.out.println("the selection string for namespace
				// "+namespace+" is "+selectedStr);
				if (selectedStr != null && selectedStr.equals(FALSE)) {
					selected = false;
				}

				SelectableDocumentType type = null;
				try {
					type = new SelectableDocumentType(namespace, label,
							selected);
				} catch (Exception e) {
					log.debug("Couldn't generate a document type", e);
					continue;
				}
				// the vector will stored the document type
				documentVector.add(type);
			} // for
			// transfer a vector to array.
			int length = documentVector.size();
			documentList = new SelectableDocumentType[length];
			for (int i = 0; i < length; i++) 
      {
				documentList[i] = (SelectableDocumentType) documentVector
						.elementAt(i);
			} // for

		} // if

		return documentList;
	}// getDocumentList

	/*
	 * Judge if a given service already existed in a service list. The key is
	 * endpoint. If it is duplicate service, index in vector will be returned
	 * Otherwise, -1 will be returned.
	 */
	private static int isDuplicateService(EcoGridService service,
			Vector serviceList) throws InvalidEcoGridServiceException {
		int duplicateIndex = -1;
		if (service == null || serviceList == null) {
			throw new InvalidEcoGridServiceException(
					"The service or service list is"
							+ " null and couldn't judge if the given service is duplicate in list");
		}
		String givenEndPoint = service.getEndPoint();
		if (givenEndPoint == null || givenEndPoint.trim().equals("")) {
			throw new InvalidEcoGridServiceException(
					"The given service doesn't have "
							+ "endpoint and couldn't judge if the given service is duplicate in list");
		}
		int size = serviceList.size();
		for (int i = 0; i < size; i++) {
			EcoGridService currentService = (EcoGridService) serviceList
					.elementAt(i);
			String currentEndPoint = currentService.getEndPoint();
			if (currentEndPoint != null
					&& currentEndPoint.equals(givenEndPoint)) {
				duplicateIndex = i;
				break;
			}// if
		}// for
		return duplicateIndex;
	}// isDuplicateService

	/**
	 * Method to get selected service with selected document types from service
	 * list vector. If a service selected, but without any selected documents,
	 * this service couldn't be in this vector
	 */
	public Vector getSelectedServicesList() {
		selectedSearchingServicesList = new Vector();
		for (int i = 0; i < currentServicesList.size(); i++) {
			EcoGridService service = (EcoGridService) currentServicesList
					.elementAt(i);
			if (service != null) {
				SelectableEcoGridService selectableService = (SelectableEcoGridService) service;
				if (selectableService.getSelectableServiceName()
						.getIsSelected() == true) {
					SelectableDocumentType[] docList = selectableService
							.getSelectedDocumentTypeList();
					if (docList != null && docList.length != 0) {
						// System.out.println("Add service !!!!!!!!!!!!!!!!! " +
						// selectableService.getEndPoint() +"into selected
						// service list");
						selectedSearchingServicesList.add(selectableService);
					}
				}
			}
		}
		return this.selectedSearchingServicesList;
	}
	
	public void readServices() {

		File saveFile = new File(_saveFileName);

		if (saveFile.exists()) {
			try {
				InputStream is = new FileInputStream(saveFile);
				ObjectInput oi = new ObjectInputStream(is);
				Object newObj = oi.readObject();
				oi.close();

				Vector<EcoGridService> servs = (Vector<EcoGridService>) newObj;
				for (EcoGridService egs : servs) {
					addService(egs);
				}

				return;
			} catch (Exception e1) {
				// problem reading file, try to delete it
				log.warn("Exception while reading EcoGridServices file: "
						+ e1.getMessage());
				try {
					saveFile.delete();
				} catch (Exception e2) {
					log.warn("Unable to delete EcoGridServices file: "
							+ e2.getMessage());
				}
			}
		} else {
			// initialize default services from the Config.xml file
			readServicesFromConfig();
		}
	}

	/*
	 * Read query service from configure file and put the service into a vector
	 * 
	 * If any updates are done on Config, be sure to call this method again to
	 * refresh with the newest values
	 */
	private void readServicesFromConfig() {
		// reset the vector
		currentServicesList = new Vector();

    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("ecogrid"));
    List servicesList = ecogridProperty.getProperties("servicesList.service");
    
		// find service type is ecogrid query
		if (servicesList != null && servicesList.size() > 0) 
    {
			int length = servicesList.size();
			for (int i = 0; i < length; i++) 
      {
        ConfigurationProperty cp = (ConfigurationProperty)servicesList.get(i);
				//Node serviceTypeNode = typeList.item(i);
				//if (serviceTypeNode != null
				//		&& serviceTypeNode.getFirstChild() != null) {
				//	String serviceTypeString = serviceTypeNode.getFirstChild()
				//			.getNodeValue();
        String serviceTypeString = cp.getProperty("serviceType").getValue();
					if (isDebugging) {
						log.debug("The service type from configure file is "
								+ serviceTypeString);
					}
					// if this a valid service type, transfer the service
					// node(servicetype's parent
					// node) into java object and put it into service list
					if (serviceTypeString != null
							&& serviceTypes.contains(serviceTypeString)) 
          {
						//Node queryServiceNode = serviceTypeNode.getParentNode();
						//SelectableEcoGridService service = generateServiceFromServiceNode(queryServiceNode);
            SelectableEcoGridService service = generateServiceFromServiceNode(cp);
						if (service != null) 
            {
							// check duplicate
							int duplicateIndex = -2;
							try {
								duplicateIndex = isDuplicateService(service,
										currentServicesList);
							} catch (InvalidEcoGridServiceException e) {
								log
										.debug(
												"The error for checking duplicate ecogrid service in readQueryServiceFromConfig is ",
												e);
							}
							// if it is not duplicate, the index should be -1
							if (duplicateIndex == -1) 
              {
								if (isDebugging) 
                {
									log.debug("read service "
											+ service.getServiceName()
											+ " from config file to vector");
								}
								currentServicesList.add(service);
							}
						}
					}// if
				}// if
			}// for
  }	
  
	public void deleteServicesFile() {
		File saveFile = new File(_saveFileName);
		if (saveFile.exists()) {
			if (isDebugging)
				log.debug("delete " + saveFile);
			saveFile.delete();
		}
	}
	
	public void writeServices() throws Exception {
		File saveFile = new File(_saveFileName);
		if (saveFile.exists()) {
			if (isDebugging)
				log.debug("delete " + saveFile);
			saveFile.delete();
		}
		try {
			OutputStream os = new FileOutputStream(saveFile);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(currentServicesList);
			oos.flush();
			if (isDebugging) {
				log.debug("wrote " + saveFile);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Print out every service in the list
	 */
	public void print() {
		if (currentServicesList != null) {
			int size = currentServicesList.size();
			for (int i = 0; i < size; i++) {
				EcoGridService service = (EcoGridService) currentServicesList
						.elementAt(i);
				service.print();
				log.debug("----------------");
			}// for
		}// if
	}// print

	/**
	 * Filters out the non-query services given a list of EcoGridServices
	 * 
	 * @param list
	 *            of many different kinds of services
	 * @return list of EcoGridService objects for querying
	 */
	public static Vector filterQueryServicesList(Vector inputServices) {
		Vector types = new Vector();
		types.add(AUTHENTICATEDQUERYSERVICETYPE);
		types.add(QUERYSERVICETYPE);

		Vector retList = new Vector();
		for (int i = 0; i < inputServices.size(); i++) {
			EcoGridService service = (EcoGridService) inputServices.get(i);
			if (types.contains(service.getServiceType())) {
				retList.add(service);
			}
		}
		return retList;
	}

	public static Vector registryEntries2EcogridServices(
			RegistryEntryType[] list, boolean selectable) {
		Vector serviceVector = new Vector();
		if (list == null) {
			log.debug("The registry entry  list is null");
			return serviceVector;
		}
		int length = list.length;
		for (int i = 0; i < length; i++) {
			RegistryEntryType registryType = list[i];
			String serviceName = registryType.getServiceName();
			log.debug("The service name from registry is " + serviceName);
			String serviceType = registryType.getServiceType();
			log.debug("The service type from registry is " + serviceType);
			String endpoint = registryType.getEndPoint();
			log.debug("Find the service " + endpoint + " in registry");
			String serviceGroup = registryType.getServiceGroup();
			log.debug("the service group is " + serviceGroup + " in registry");
			RegistryEntryTypeDocumentType[] typeList = registryType
					.getDocumentType();

			DocumentType[] docTypeList = null;
			EcoGridService service = new EcoGridService();
			try {
				service.setServiceName(serviceName);
				service.setServiceType(serviceType);
				service.setEndPoint(endpoint);
				service.setServiceGroup(serviceGroup);
				docTypeList = transferRegDocTypeArray(typeList);
				service.setDocumentTypeList(docTypeList);
				if (docTypeList == null || docTypeList.length == 0) {
					log.debug("Document type list is null or empty");
					// continue;
				}
			} catch (Exception e) {
				log.debug("Could not set service  " + e.getMessage());
				continue;
			}
			log.debug("Adding service " + endpoint + " into the vector");
			if (selectable) {
				// make it selectable
				service = new SelectableEcoGridService(service);
			}
			serviceVector.add(service);
		}//
		return serviceVector;
	}// transferRegEntriesArrayToVector

	private static DocumentType[] transferRegDocTypeArray(
			RegistryEntryTypeDocumentType[] regTypeList) throws Exception {
		DocumentType[] docTypeList = null;
		if (regTypeList == null) {
			return docTypeList;
		}// if
		int length = regTypeList.length;
		Vector tmp = new Vector();
		for (int i = 0; i < length; i++) {
			RegistryEntryTypeDocumentType regDocType = regTypeList[i];
			String namespace = regDocType.getNamespace();
			String label = regDocType.getLabel();
			try {
				DocumentType type = new DocumentType(namespace, label);
				tmp.add(type);
			} catch (Exception e) {
				log.debug("Could not create document type ", e);
				throw e;
			}
		}// for
		// transfer the tmp vector to an array
		int size = tmp.size();
		docTypeList = new DocumentType[size];
		for (int i = 0; i < size; i++) {
			docTypeList[i] = (DocumentType) tmp.elementAt(i);
		}// for
		return docTypeList;
	}// transferRegDocTypeArray

}// EcoGridServiceController