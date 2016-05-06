/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2010-12-23 11:01:04 -0800 (Thu, 23 Dec 2010) $' 
 * '$Revision: 26600 $'
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.PreferencesTab;
import org.kepler.gui.PreferencesTabFactory;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * Class reprents a frame which will display a service panel
 * 
 *@author not attributable
 *@created February 17, 2005
 */

public class EcogridPreferencesTab extends JPanel implements PreferencesTab {

	private String _tabName;

	private JPanel mainPanel = new JPanel();
	private JPanel textPanel = new JPanel();
	private JLabel textLabel = new JLabel();
	private JPanel buttonPanel = new JPanel();
	private ServicesDisplayPanel servicesDisplayPanel = null;
	private String displayText = "";
	private Vector<String> selectedServiceList = null;

	private static int BUTTON_FONT_SIZE = StaticResources.getSize(
			"button.limitedSpace.maxFontSize", 11);
	private JButton refreshButton = null;
	private JCheckBox keepExistingCheckbox = null;
	private JPanel newButtonPanel = null;

	/**
	 * Description of the Field
	 */
	protected EcoGridServicesController controller = null;
	private Vector<SelectableEcoGridService> originalServiceList = new Vector<SelectableEcoGridService>();

	protected TableauFrame parentFrame;

	/**
	 * Description of the Field
	 */
	public final static int MARGINGSIZE = 20;
	/**
	 * Description of the Field
	 */
	public final static int GAP = 30;
	/**
	 * Description of the Field
	 */
	public final static int HEIGHT = 400;
	/**
	 * Description of the Field
	 */
	public final static Dimension BUTTONDIMENSION = new Dimension(83, 25);
	private final static int ROWNUMBER = 5;

	protected final static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.ecogrid.ServicesDisplayFrame");
	}

	/**
	 * Constructor of the frame
	 * 
	 *@param frameTitle
	 *            Description of the Parameter
	 *@param selectedServiceList
	 *            Description of the Parameter
	 *@param controller
	 *            Description of the Parameter
	 *@param location
	 *            Description of the Parameter
	 *@exception HeadlessException
	 *                Description of the Exception
	 *@throws HeadlessException
	 */
	public EcogridPreferencesTab() throws HeadlessException {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.PreferencesTab#getTabName()
	 */
	public String getTabName() {
		return _tabName;
	}

	public void setTabName(String tabName) {
		_tabName = tabName;
	}

	public void initializeTab() throws Exception {
		this.controller = EcoGridServicesController.getInstance();
		this.selectedServiceList = this.controller.getQueryServicesList();
		initOriginalServiceList();
		initMainPanel();
		initButtonPanel();
		this.setLayout(new BorderLayout());

		// GridBagLayout gridbag = new GridBagLayout();

		/*
		 * int gridx, int gridy, int gridwidth, int gridheight, double weightx,
		 * double weighty, int anchor, int fill, Insets insets, int ipadx, int
		 * ipady
		 */
		// this.setLayout(gridbag);
		this.add(mainPanel, BorderLayout.CENTER);
		initPanelSize();
	}

	/*
	 * Method to initial frame size
	 */
	/**
	 * Description of the Method
	 */
	private void initPanelSize() {
		Insets insets = getInsets();
		int width = 2 * MARGINGSIZE + insets.left + insets.right
				+ ServicesDisplayPanel.HEADNAME.length
				* ServicesDisplayPanel.CELLPREFERREDWIDTH;
		int height = HEIGHT;
		this.setMinimumSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kepler.gui.PreferencesTab#setParent(ptolemy.actor.gui.TableauFrame)
	 */
	public void setParent(TableauFrame frame) {
		this.parentFrame = frame;
	}

	/*
	 * This method will init button panel
	 */
	private void initButtonPanel() {

		// remember default button font
		final Font defaultUIMgrButtonFont = (Font) UIManager.get("Button.font");

		// now set our custom size, provided it's smaller than the default:
		int buttonFontSize = (defaultUIMgrButtonFont.getSize() < BUTTON_FONT_SIZE) ? defaultUIMgrButtonFont
				.getSize(): BUTTON_FONT_SIZE;

		final Font BUTTON_FONT = new Font(defaultUIMgrButtonFont.getFontName(),
				defaultUIMgrButtonFont.getStyle(), buttonFontSize);

		UIManager.put("Button.font", BUTTON_FONT);

		refreshButton = new JButton(
				new ServicesRefreshAction(
						StaticResources.getDisplayString(
								"preferences.data.refresh", 
								"Refresh"), 
						this));
		refreshButton.setMinimumSize(EcogridPreferencesTab.BUTTONDIMENSION);
		refreshButton.setPreferredSize(EcogridPreferencesTab.BUTTONDIMENSION);
		refreshButton.setSize(EcogridPreferencesTab.BUTTONDIMENSION);
		// setMaximumSize was truncating label on osX. I don't know why. 
		// It seems about the same as how SearchUIJPanel does things. -derik
		//refreshButton.setMaximumSize(EcogridPreferencesTab.BUTTONDIMENSION);

		keepExistingCheckbox = new JCheckBox(
				StaticResources.getDisplayString(
						"preferences.data.keepExistingSources",
						"Keep existing sources"));
		keepExistingCheckbox.setSelected(false);

		JPanel bottomPanel = new JPanel();

		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(refreshButton);
		bottomPanel.add(keepExistingCheckbox);


		newButtonPanel = new JPanel();
		newButtonPanel.setLayout(new BorderLayout());
		newButtonPanel.add(Box
				.createVerticalStrut(EcogridPreferencesTab.MARGINGSIZE),
				BorderLayout.NORTH);
		newButtonPanel.add(bottomPanel, BorderLayout.SOUTH);
		setButtonPanel(newButtonPanel);
		
		// restore default button font
		if (defaultUIMgrButtonFont != null) {
			UIManager.put("Button.font", defaultUIMgrButtonFont);
		}
	}

	/**
	 * This method will re-add buttons to frame
	 */
	public void updateButtonPanel() {
		this.remove(newButtonPanel);
		initButtonPanel();
	}

	public boolean isKeepExisting() {
		return this.keepExistingCheckbox.isSelected();
	}

	// ServicesDisplayFrame

	/*
	 * Method to initPanles (box layout)
	 */
	/**
	 * Description of the Method
	 */
	private void initMainPanel() {
		mainPanel.setLayout(new BorderLayout());

		// init text panel and add it to main panel
		initTextPanel();
		mainPanel.add(textPanel, BorderLayout.NORTH);
		// add gap between text and servicepanel

		// init service dispaly panel and add it to main panel
		initServiceDisplayPanel();
		mainPanel.add(servicesDisplayPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

	}

	// initMainPanel

	/*
	 * Init text panel (border Layout and box layout)
	 */
	/**
	 * Description of the Method
	 */
	private void initTextPanel() {
		// text part of text panel
		JPanel textTopPanel = new JPanel();
		textTopPanel.setLayout(new BoxLayout(textTopPanel, BoxLayout.X_AXIS));
		textTopPanel.add(textLabel);
		textTopPanel.add(Box.createHorizontalGlue());
		// y box layout
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		textPanel.add(textTopPanel);
		textPanel.add(Box.createVerticalStrut(GAP));
	}

	// initTextPanel

	/*
	 * Init service display panel(border layout)
	 */
	/**
	 * Description of the Method
	 */
	private void initServiceDisplayPanel() {
		servicesDisplayPanel = new ServicesDisplayPanel(selectedServiceList);
	}

	// initServiceDisplayPanel

	/*
	 * Method to initialize the orginal service, it will copy the service list
	 * in controller to another vector(it will create a new EcoGridService
	 * object, not a pointer)
	 */
	/**
	 * Description of the Method
	 */
	private void initOriginalServiceList() {
		Vector serviceList = controller.getServicesList();
		if (serviceList != null) {

			int size = serviceList.size();
			for (int i = 0; i < size; i++) {
				SelectableEcoGridService service = (SelectableEcoGridService) serviceList
						.elementAt(i);
				try {
					SelectableEcoGridService newServcie = SelectableEcoGridService
							.copySelectableEcoGridService(service);
					originalServiceList.add(newServcie);

				} catch (Exception e) {
					log
							.debug(
									"Error for copy a servie in initOrignialServiceList ",
									e);
				}

			}
		}
	}

	//

	/**
	 * Method to get the main panel.
	 * 
	 *@return JPanel
	 */
	public JPanel getMainPanel() {
		return this.mainPanel;
	}

	// getMainPanel

	/**
	 * Method to get service display panel
	 * 
	 *@return JPanel
	 */
	public ServicesDisplayPanel getServicesDisplayPanel() {
		return this.servicesDisplayPanel;
	}

	// getServiceDisplayPanel()

	/**
	 * Method to set up a new service dipaly panel panel
	 * 
	 *@param servicesDisplayPanel
	 *            ServicesDisplayPanel
	 */
	public void setServiceDisplayPanel(ServicesDisplayPanel servicesDisplayPanel) {
		mainPanel.remove(this.servicesDisplayPanel);
		this.servicesDisplayPanel = servicesDisplayPanel;
		mainPanel.add(this.servicesDisplayPanel, BorderLayout.CENTER);
		mainPanel.validate();
	}

	// setServiceDisplayPanel

	/**
	 * Method to get button panel
	 * 
	 *@return JPanel
	 */
	public JPanel getButtonPanel() {
		return this.buttonPanel;
	}

	// get button panel

	/**
	 * Method to reset buttion panel
	 * 
	 *@param buttonPanel
	 *            JPanel
	 */
	public void setButtonPanel(JPanel buttonPanel) {
		mainPanel.remove(this.buttonPanel);
		this.buttonPanel = buttonPanel;
		mainPanel.add(this.buttonPanel, BorderLayout.SOUTH);
		mainPanel.validate();
	}

	// setButtonPanel

	/**
	 * Method to get display text
	 * 
	 *@return String
	 */
	public String getDisplayText() {
		return this.displayText;
	}

	// getDisplayText

	/**
	 * Method to set the display string
	 * 
	 *@param displayText
	 *            String
	 */
	public void setDisplayText(String displayText) {
		this.displayText = displayText;
		textLabel.setText(this.displayText);
	}

	// setDislayText

	/**
	 * Method to get controller
	 * 
	 *@return EcoGridServicesController
	 */
	public EcoGridServicesController getEcoGridServicesController() {
		return this.controller;
	}

	// getecogridcontroller

	/**
	 * Method to set controller
	 * 
	 *@param controller
	 *            EcoGridServicesController
	 */
	public void setEcoGridServicesController(
			EcoGridServicesController controller) {

		this.controller = controller;
	}

	// setEcoGridServicesController

	/**
	 * Method to get original service list(this list is for cancel button)
	 * 
	 *@return Vector
	 */
	public Vector getOriginalServiceList() {
		return this.originalServiceList;
	}

	// getOriginalServiceList

	/**
	 * Set original service list
	 * 
	 *@param originalServiceList
	 *            Vector
	 */
	public void setOriginalServiceList(Vector originalServiceList) {
		this.originalServiceList = originalServiceList;
	}

	// setOriginalServiceList

	/**
	 * Method to update service in controller base on the user selection in the
	 * panel
	 */
	public void updateController() {
		// currently we just set new service into controller(memory), later will
		// need to
		// save to jar configure file
		if (servicesDisplayPanel != null && controller != null) {
			// removed the all unselected serivce
			Vector allUnSelectedList = servicesDisplayPanel
					.getAllUnSelectedServicesList();
			if (allUnSelectedList != null) {
				int size = allUnSelectedList.size();
				for (int i = 0; i < size; i++) {
					SelectableEcoGridService service = (SelectableEcoGridService) allUnSelectedList
							.elementAt(i);
					controller.removeService(service);
				}
			}
			// updated partial selected services
			// this vector is a service list with the selected document type
			Vector partialSelectedList = servicesDisplayPanel
					.getPartialSelectedServicesList();
			if (partialSelectedList != null) {
				int length = partialSelectedList.size();
				for (int j = 0; j < length; j++) {
					SelectableEcoGridService service = (SelectableEcoGridService) partialSelectedList
							.elementAt(j);
					try {
						controller.updateService(service);
					} catch (Exception ee) {
						log.debug("Could not update a service "
								+ service.getServiceName(), ee);
					}
				}// for
			}// if
		}
	}

	/**
	 * A factory that creates the ServicesListModification panel for the
	 * PreferencesFrame.
	 * 
	 *@author Aaron Schultz
	 */
	public static class Factory extends PreferencesTabFactory {
		/**
		 * Create a factory with the given name and container.
		 * 
		 *@param container
		 *            The container.
		 *@param name
		 *            The name of the entity.
		 *@exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 *@exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a PreferencesTab that displays the selected Ecogrid Services.
		 * 
		 * @return A new LibraryPaneTab that displays the library
		 */
		public PreferencesTab createPreferencesTab() {
			EcogridPreferencesTab ept = new EcogridPreferencesTab();
			ept.setTabName(this.getName());
			return ept;
		}
	}

	public void onClose() {
		EcoGridServicesController esc = EcoGridServicesController.getInstance();
		try {
			esc.writeServices();
		} catch (Exception e) {
			esc.deleteServicesFile();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.PreferencesTab#onCancel()
	 */
	public void onCancel() {
		controller.setServicesList(originalServiceList);
	}

}