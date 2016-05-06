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

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.AbstractAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.client.RegistryServiceClient;
import org.ecoinformatics.ecogrid.registry.stub.RegistryEntryType;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

import ptolemy.util.MessageHandler;

/**
 * Class will search the registry
 * 
 * @author Jing Tao
 * 
 */

public class SearchRegistryAction extends AbstractAction {
	private RegistrySearchDialog searchDialog = null;
	private EcogridPreferencesTab parent = null;
	private String registryEndPoint = null;
	private RegistryServiceClient client = null;

	private static final String ENDPOINTPATH = "//ecogridService/registry/endPoint";
	private static final String PERCENTAGE = "%";
	private String sessionId = "foo"; // this value can be replaced by something
										// else

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.SearchRegistryAction");
	}

	/**
	 * Constructor
	 * 
	 * @param name
	 *            String
	 * @param searchDialog
	 *            JDialog
	 * @param parent
	 *            ServicesDisplayFrame
	 */
	public SearchRegistryAction(String name, RegistrySearchDialog searchDialog,
			EcogridPreferencesTab parent, Point location) {
		super(name);
		this.searchDialog = searchDialog;
		this.parent = parent;
    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty endpointProperty = ecogridProperty.getProperty("registry.endPoint");
    registryEndPoint = endpointProperty.getValue();

	}// SearchRegistryAction

	/**
	 * Method do search
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		String xpath = searchDialog.getXPath();
		String value = searchDialog.getSearchValue();
		try {
			client = new RegistryServiceClient(registryEndPoint);
		} catch (Exception ee) {
			log.debug("The error to generate registry search client ", ee);
		}
		if (client != null && xpath != null && !xpath.trim().equals("")) {
			Vector searchResult = searchRegistry(xpath, value);
			Vector onlyQueryServices = EcoGridServicesController
					.filterQueryServicesList(searchResult);
			Vector selectedServicesLists = SelectableEcoGridService
					.transferServiceVectToDefaultSelectedServiceVect(onlyQueryServices);
			// destroy the search dialog
			searchDialog.setVisible(false);
			searchDialog.dispose();
			searchDialog = null;
			// create a search result frame base on search result
			EcoGridServicesController controller = parent
					.getEcoGridServicesController();
			//SearchRegistryResultFrame resultFrame = new SearchRegistryResultFrame(
			//		"Search Result", parent);

		}// if

	}// actionPerformed

	/*
	 * This methd will search registry
	 */
	private Vector searchRegistry(String optionLabel, String value) {
		Vector newServiceList = new Vector();

		// This is for all service
		if (optionLabel != null
				&& optionLabel.equals(RegistrySearchDialog.ALLSERVICES)) {
			log.debug("The option label is " + optionLabel);
			RegistryEntryType[] serviceList = null;
			try {
				serviceList = client.list(sessionId);
				newServiceList = EcoGridServicesController
						.registryEntries2EcogridServices(serviceList, true // selectable
						);
			} catch (Exception e) {
				log.debug("couldn't search the ecogrid registry because ", e);
			}

		}// if
		else if (optionLabel != null && value != null) {
			log.debug("The option label is " + optionLabel + " and value is "
					+ value);
			String xpath = searchDialog.getXPath(optionLabel);
			value = decorateSearchValue(value);
			log.debug("The xpath value is:" + xpath + " for option label:"
					+ optionLabel + " sessionId:" + sessionId);
			if (xpath != null) {
				RegistryEntryType[] serviceList = null;
				try {
					serviceList = client.query(sessionId, xpath, value);
					newServiceList = EcoGridServicesController
							.registryEntries2EcogridServices(serviceList, true // selectable
							);
				} catch (Exception e) {
					log.debug("couldn't search the ecogrid registry because ",
							e);
				}
			}// if

		}// elseif

		return newServiceList;
	}// searchResigry

	/**
	 * Query the registry and persist to config file
	 */
	public static void queryRegistryRewriteConfig() {

		RegistryEntryType[] registryEntries = null;
		Vector ecogridServicesList = new Vector();
		String sessionId = "fakesessionid";
    
    ConfigurationProperty ecogridProperty = ConfigurationManager.getInstance()
      .getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty registryEndPointProp = ecogridProperty.getProperty("registry.endPoint");
    String registryEndPoint = registryEndPointProp.getValue();

		RegistryServiceClient client = null;

		try {
			client = new RegistryServiceClient(registryEndPoint);
		} catch (Exception ee) {
			String msg = "Problem creating registry search service client using endpoint: \n"
					+ registryEndPoint;
			// MessageHandler.error(msg, ee);
			log.warn(msg);
			return;
		}
		try {
			// get all the entries
			registryEntries = client.list(sessionId);

		} catch (Exception e) {
			String msg = "Problem looking up registry entries using endpoint: \n"
					+ registryEndPoint;
			// MessageHandler.error(msg, e);
			log.warn(msg);
			return;
		}

		try {
			// convert them to EcogridServices, optionally selectable
			ecogridServicesList = EcoGridServicesController
					.registryEntries2EcogridServices(registryEntries, true // selectable
					);
			// set the new services from registry as the services in the
			// controller
			// EcoGridServicesController.getInstance().setServicesList(ecogridServicesList);
			// merge the new list with the existing
			EcoGridServicesController.getInstance().mergeServicesList(
					ecogridServicesList);
		} catch (Exception e) {
			String msg = "Problem merging new services with existing services";
			MessageHandler.error(msg, e);
			log.error(msg);
			return;
		}

		// write them to the config
		try {
			//EcoGridServicesController.getInstance().writeServicesToConfig();
			EcoGridServicesController.getInstance().writeServices();
		} catch (Exception e) {
			String msg = "Problem writing new services to config file";
			MessageHandler.error(msg, e);
			log.error(msg);
			return;
		}
	}

	/*
	 * Method to add % symbol to value
	 */
	private String decorateSearchValue(String value) {
		String newValue = PERCENTAGE + value + PERCENTAGE;
		return newValue;
	}

	/*
	 * This method is hard code to go some serivce list without searching
	 */
	private Vector hardCodeReturnVector() {
		Vector newServiceList = new Vector();
		try {
			DocumentType type1 = new DocumentType(
					"eml://ecoinformatics.org/eml-2.0.0",
					"Ecological Metadata Language 2.0.0");
			DocumentType type2 = new DocumentType(
					"http://digir.net/schema/conceptual/darwin/full/2001/1.0",
					"Darwin Core 1.0");
			DocumentType[] docTypes = new DocumentType[2];
			docTypes[0] = type1;
			docTypes[1] = type2;
			EcoGridService service1 = new EcoGridService();
			service1.setServiceName("KNB EcoGrid");
			service1.setServiceType("EcoGridQueryInterface");
			service1
					.setEndPoint("http://ecogrid.ecoinformatics.org/ogsa/services/org/ecoinformatics/ecogrid/EcoGridQueryInterfaceLevelOneService");
			service1.setDocumentTypeList(docTypes);

			DocumentType type3 = new DocumentType(
					"eml://ecoinformatics.org/eml-2.0.0",
					"Ecological Metadata Language 2.0.0");
			DocumentType type4 = new DocumentType(
					"http://digir.net/schema/conceptual/darwin/full/2001/1.0",
					"Darwin Core 1.0");
			DocumentType[] docTypes1 = new DocumentType[2];
			docTypes1[0] = type3;
			docTypes1[1] = type4;
			EcoGridService service2 = new EcoGridService();
			service2.setServiceName("KU Digir EcoGrid QueryInterface");
			service2.setServiceType("EcoGridQueryInterface");
			// service2.setEndPoint("http://129.237.201.166:8080/ogsa/services/org/ecoinformatics/ecogrid/EcoGridQueryInterfaceLevelOneServiceFactory");
			service2
					.setEndPoint("http://129.237.127.19:8080/ogsa/services/org/ecoinformatics/ecogrid/EcoGridQueryInterfaceLevelOneService");
			service2.setDocumentTypeList(docTypes1);

			DocumentType type5 = new DocumentType(
					"eml://ecoinformatics.org/eml-2.0.0",
					"Ecological Metadata Language 2.0.0");
			DocumentType type6 = new DocumentType(
					"http://digir.net/schema/conceptual/darwin/full/2001/1.0",
					"Darwin Core 1.0");
			DocumentType[] docTypes2 = new DocumentType[2];
			docTypes2[0] = type5;
			docTypes2[1] = type6;
			EcoGridService service3 = new EcoGridService();
			service3.setServiceName("Pine EcoGrid QueryInterface");
			service3.setServiceType("EcoGridQueryInterface");
			service3
					.setEndPoint("http://pine.nceas.ucsb.edu:8090/ogsa/services/org/ecoinformatics/ecogrid/EcoGridQueryInterfaceLevelOneService");
			service3.setDocumentTypeList(docTypes2);

			newServiceList.add(service1);
			newServiceList.add(service2);
			newServiceList.add(service3);

		} catch (Exception e) {

		}
		return newServiceList;
	}

}