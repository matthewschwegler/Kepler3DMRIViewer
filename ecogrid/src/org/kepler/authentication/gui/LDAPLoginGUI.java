/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2013-01-16 16:52:56 -0800 (Wed, 16 Jan 2013) $' 
 * '$Revision: 31343 $'
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

package org.kepler.authentication.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.ecoinformatics.seek.ecogrid.MetadataSpecificationInterface;
import org.kepler.authentication.ProxyRepository;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.ModelDirectory;
import ptolemy.actor.gui.Tableau;

/**
 * LoginGUI pops up the login dialog to let user login, retrieves credential
 * from GAMA server, and stores the credential into ProxyRepository.
 * 
 * @author Zhijie Guan guan@sdsc.edu
 * 
 */

public class LDAPLoginGUI extends JPanel implements ActionListener {
	private static ProxyRepository proxyRepository; // the proxyrepository got
													// from
													// AuthenticationManager
	private JDialog controllingDialog; // The Frame of login dialog

	private static String organizationListXPath = "//"
			+ MetadataSpecificationInterface.ECOGRIDPATH + "/"
			+ "ldapOrganizations/organization";

	// Command button constants
	private String OK = "ok";
	private String CANCEL = "cancel";
	private String ANON = "anon";

	// Input fields
	private JTextField userNameField;
	private JPasswordField passwordField;
	private JComboBox domainField;
	private static Vector domainList;

	private String username;
	private String password;
	private String organization;

	private String domainName;

	/**
	 * The constructor is used to build all the display components
	 * 
	 */
	public LDAPLoginGUI() {
		// Create everything.
		// User Name field, label, and pane
		userNameField = new JTextField(20);
		JLabel userNameLabel = new JLabel("Username: ");
		userNameLabel.setLabelFor(userNameField);
		JPanel userNamePane = new JPanel(new FlowLayout());
		userNamePane.add(userNameLabel);
		userNamePane.add(userNameField);

		// Password field, label, and pane
		passwordField = new JPasswordField(20);
		passwordField.setEchoChar('*');
		passwordField.setActionCommand(OK);
		passwordField.addActionListener(this);
		JLabel passwordLabel = new JLabel("Password: ");
		passwordLabel.setLabelFor(passwordField);
		JPanel passwordPane = new JPanel(new FlowLayout());
		passwordPane.add(passwordLabel);
		passwordPane.add(passwordField);

		// Domain field, label, and pane
		domainField = new JComboBox();

		// get the organization names from the config
		ConfigurationProperty ecogridProperty = ConfigurationManager
				.getInstance().getProperty(
						ConfigurationManager.getModule("ecogrid"));
		ConfigurationProperty ldapOrgProp = ecogridProperty
				.getProperty("ldapOrganizations");
		Iterator orgs = ConfigurationProperty.getValueList(
				ldapOrgProp.getProperties(), "organization", false).iterator();

		while (orgs.hasNext()) {
			domainField.addItem(orgs.next());
		}
		JLabel domainLabel = new JLabel("Organization: ");
		domainLabel.setLabelFor(domainField);
		JPanel domainPane = new JPanel(new FlowLayout());
		domainPane.add(domainLabel);
		domainPane.add(domainField);

		// Lay out input fields and labels on inputPane
		JPanel inputPane = new JPanel();
		inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.Y_AXIS));
		inputPane.add(userNamePane);
		inputPane.add(passwordPane);
		inputPane.add(domainPane);

		// Buttons and their buttonPane
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		JButton anonLoginButton = new JButton("Login Anonymously");
		okButton.setActionCommand(OK);
		okButton.addActionListener(this);
		cancelButton.setActionCommand(CANCEL);
		cancelButton.addActionListener(this);
		anonLoginButton.setActionCommand(ANON);
		anonLoginButton.addActionListener(this);
		JPanel buttonPane = new JPanel(new FlowLayout());
		buttonPane.add(okButton);
		buttonPane.add(cancelButton);
		buttonPane.add(anonLoginButton);

		// Pane for the whole window
		setLayout(new BorderLayout());
		add(inputPane, BorderLayout.CENTER);
		add(buttonPane, BorderLayout.PAGE_END);
	}

	/**
	 * Event listener
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		if (OK.equals(cmd)) { // User login and click OK button
			username = userNameField.getText();

			char[] passwd = passwordField.getPassword();
			password = new String(passwd);

			// Zero out the possible password, for security.
			for (int i = 0; i < passwd.length; i++) {
				passwd[i] = 0;
			}

			organization = (String) domainField.getSelectedItem();
		} else if (CANCEL.equals(cmd)) {
			organization = DomainSelectionGUI.DOMAIN_BREAK;
		} else if (ANON.equals(cmd)) {
			username = "anon";
			password = "anon";
			organization = "anon";
		}
		//System.out.println("LDAPLoginGUI actionPerformed calling controllingDialog.dispose()");
		controllingDialog.dispose();
		// controllingDialog.setVisible(false);
		//System.out.println("LDAPLoginGUI actionPerformed done disposing of controllingDialog");
	}

	// Method to reset the focus of the login dialog
	// Must be called from the event-dispatching thread.
	protected void resetFocus() {
		userNameField.requestFocusInWindow();
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	public void createAndShowGUI() {
		// Make sure we have nice window decorations.
		System.out.println("LDAPLoginGUI createAndShowGUI creating gui");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Create and set up the window.
		Configuration config = (Configuration) Configuration.configurations()
				.iterator().next();
		// get the directory from the config
		ModelDirectory directory = (ModelDirectory) config
				.getEntity("directory");

		// find the parent tableau to use as the parent for this dialog
		Iterator effigy = directory.entityList(Effigy.class).iterator();
		Tableau t = null;
		while (effigy.hasNext()) {
			Effigy e = (Effigy) effigy.next();
			t = (Tableau) e.getEntity("graphTableau");
			if (t != null) {
				break;
			}
		}

		controllingDialog = new JDialog(t.getFrame());
		controllingDialog.setTitle("Authenticating for: " + domainName);
		controllingDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		controllingDialog.setModal(true);

		// Create and set up the content pane.
		final LDAPLoginGUI contentPane = this;

		this.setOpaque(true); // content panes must be opaque
		controllingDialog.setContentPane(this);

		// Make sure the focus goes to the right component
		// whenever the frame is initially given the focus.
		controllingDialog.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				contentPane.resetFocus();
			}

			// XXX the only thing that seems to cause windowClosing is the X 
			// close button so we can behave like Cancel button here and set
			// organization to magic string BREAK
			public void windowClosing(WindowEvent e) {
				organization = DomainSelectionGUI.DOMAIN_BREAK;
			}

		});

		// Display the window.
		controllingDialog.pack();
		controllingDialog.setLocationRelativeTo(null); // stay in the center
		controllingDialog.setVisible(true);
	}

	/**
	 * Function to show the login dialog It adds the login dialog into the
	 * event-dispatching thread
	 */
	public void fire() {
		// creating and showing this application's GUI.
		resetFields();
		createAndShowGUI();
	}

	/**
	 * return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * return the org
	 */
	public String getOrganization() {
		return organization;
	}

	public void setDomainName(String name) {
		domainName = name;
	}

	/**
	 * resets the static fields to null
	 */
	public void resetFields() {
		username = null;
		password = null;
		organization = null;

		// and the input fields
		this.userNameField.setText(username);
		this.passwordField.setText(password);
		this.domainField.setSelectedIndex(0);

	}
}