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

import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.AbstractAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This action will add selected service in registry search reslut frame to
 * current service search frame.
 * 
 * @author Jing Tao
 * 
 */

public class AddServicesFromRegistrySearchAction extends AbstractAction {
	private EcogridPreferencesTab current = null;
	private EcogridPreferencesTab parent = null;
	private static Log log;

	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.AddServicesFromRegistrySearchAction");
	}

	/**
	 * Constructor
	 * 
	 * @param name
	 *            String
	 * @param current
	 *            ServicesDisplayFrame
	 * @param parent
	 *            ServicesDisplayFrame
	 */
	public AddServicesFromRegistrySearchAction(String name,
			EcogridPreferencesTab current, EcogridPreferencesTab parent) {
		super(name);
		this.current = current;
		this.parent = parent;
	}// AddServicesFromRegistrySearchAction

	public void actionPerformed(ActionEvent e) {
		if (current != null) {
			ServicesDisplayPanel displayPanel = current
					.getServicesDisplayPanel();
			EcoGridServicesController controller = parent
					.getEcoGridServicesController();
			// get the selected service list
			Vector allSelectedService = displayPanel
					.getAllSelectedServicesList();
			Vector partialSelectedService = displayPanel
					.getPartialSelectedServicesList();
			// add service list to controller
			addSerivcesVectorToController(controller, allSelectedService);
			addSerivcesVectorToController(controller, partialSelectedService);

			// create a another current service list display frame
			if (controller != null) {
				Vector currentServiceList = controller.getServicesList();
				/*
				 * Vector defaultSelectedServiceList =SelectableEcoGridService.
				 * transferServiceVectToDefaultSelectedServiceVect
				 * (currentServiceList);
				 */
				//current.setVisible(false);
				//current.dispose();
				//current = null;

				EcogridPreferencesTab frame = new EcogridPreferencesTab();
				Vector original = parent.getOriginalServiceList();

				frame.setOriginalServiceList(original);
				frame.updateButtonPanel();
				//parent.setVisible(false);
				//parent.dispose();
				//parent = null;

			}// if
		}// if
	}// actionPerformed

	/*
	 * Method to add vector a service to a controller
	 */
	private void addSerivcesVectorToController(
			EcoGridServicesController controller, Vector serviceList) {
		if (controller != null) {
			if (serviceList != null) {
				int size = serviceList.size();
				for (int i = 0; i < size; i++) {
					EcoGridService service = (EcoGridService) serviceList
							.elementAt(i);
					try {
						controller.addService(service);
					} catch (Exception ee) {
						log.debug("The error adding service is ", ee);
					}
				}// for
			}// if
		}// if
	}// addSerivcesVectorToController
}// AddServicesFromRegistrySearchAction