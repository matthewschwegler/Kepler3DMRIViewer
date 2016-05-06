/*
 * Copyright (c) 2010 The Regents of the University of California.
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

import javax.swing.AbstractAction;

/**
 * This method will look up the services from the registry and save the service
 * list to configuration file
 * 
 * @author Ben Leinfelder
 * 
 */

public class ServicesRefreshAction extends AbstractAction {
	private EcogridPreferencesTab prefTab = null;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            String
	 * @param frame
	 *            ServicesDisplayFrame
	 */
	public ServicesRefreshAction(String name,
			EcogridPreferencesTab prefTab) {
		super(name);
		this.prefTab = prefTab;
	}

	/**
	 * Method to dispose the frame and save service list to configure file
	 * 
	 * @param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		// reset the list
		if (!this.prefTab.isKeepExisting()) {
			EcoGridServicesController.getInstance().clearServicesList();
		}

		// look up the services and rewrite the config
		SearchRegistryAction.queryRegistryRewriteConfig();

		// refresh the frame, panel, whatever
		prefTab
				.setServiceDisplayPanel(new ServicesDisplayPanel(
						EcoGridServicesController.getInstance()
								.getQueryServicesList()));
		prefTab.validate();

		// update service in controller base on user selection
		prefTab.updateController();
		// frame.setVisible(false);
		// frame.dispose();
		// frame = null;
	}

}