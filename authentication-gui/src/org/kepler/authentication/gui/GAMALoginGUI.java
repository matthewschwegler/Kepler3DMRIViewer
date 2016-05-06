/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2013-01-16 15:42:20 -0800 (Wed, 16 Jan 2013) $' 
 * '$Revision: 31342 $'
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
import java.io.IOException;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.kepler.authentication.DomainList;
import org.kepler.authentication.ProxyRepository;

/**
 * LoginGUI pops up the login dialog to let user login, retrieves credential
 * from GAMA server, and stores the credential into ProxyRepository.
 * 
 * @author Zhijie Guan guan@sdsc.edu
 * 
 */

public class GAMALoginGUI extends JPanel implements ActionListener {
	private static ProxyRepository proxyRepository; // the proxyrepository got
													// from
													// AuthenticationManager
	private static JFrame controllingFrame; // The Frame of login dialog

	// Command button constants
	private String OK = "ok";
	private String RESET = "reset";

	// Input fields
	private JTextField userNameField;
	private JPasswordField passwordField;
	private JComboBox domainField;
	private static Vector domainList;

	private static String username = null;
	private static String password = null;
	
	// XXX magic string, get rid of this
	public static String USERNAME_BREAK = "BREAK";

	/**
	 * The constructor is used to build all the display components
	 * 
	 */
	public GAMALoginGUI() {
		// Create everything.
		// User Name field, label, and pane
		try {
			domainList = DomainList.getInstance().getDomainList();
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, "There was an error "
					+ "reading the domain list: " + ioe.getMessage());
		}
		userNameField = new JTextField(20);
		JLabel userNameLabel = new JLabel("Enter your user name: ");
		userNameLabel.setLabelFor(userNameField);
		JPanel userNamePane = new JPanel(new FlowLayout());
		userNamePane.add(userNameLabel);
		userNamePane.add(userNameField);

		// Password field, label, and pane
		passwordField = new JPasswordField(20);
		passwordField.setEchoChar('#');
		passwordField.setActionCommand(OK);
		passwordField.addActionListener(this);
		JLabel passwordLabel = new JLabel("Enter the password: ");
		passwordLabel.setLabelFor(passwordField);
		JPanel passwordPane = new JPanel(new FlowLayout());
		passwordPane.add(passwordLabel);
		passwordPane.add(passwordField);

		// Domain field, label, and pane

		domainField = new JComboBox(domainList);
		domainField.setSelectedIndex(0);
		ComboBoxRenderer renderer = new ComboBoxRenderer();
		domainField.setRenderer(renderer);
		JLabel domainLabel = new JLabel("Select the domain/subdomain: ");
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
		JButton resetButton = new JButton("Cancel");
		okButton.setActionCommand(OK);
		okButton.addActionListener(this);
		resetButton.setActionCommand(RESET);
		resetButton.addActionListener(this);
		JPanel buttonPane = new JPanel(new FlowLayout());
		buttonPane.add(okButton);
		buttonPane.add(resetButton);

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
			char[] passwd = passwordField.getPassword();
			password = new String(passwd);

			// Zero out the possible password, for security.
			for (int i = 0; i < passwd.length; i++) {
				passwd[i] = 0;
			}

			username = userNameField.getText();
		} else if (RESET.equals(cmd)) { // CANCEL button is clicked.
			controllingFrame.dispose();
			username = USERNAME_BREAK;
		}
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
	private static void createAndShowGUI() {
		// Make sure we have nice window decorations.
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Create and set up the window.
		controllingFrame = new JFrame("GAMA Login");
		//controllingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		controllingFrame.addWindowListener(new WindowAdapter(){
      public void windowClosing(WindowEvent e) {
        username = USERNAME_BREAK;
        controllingFrame.dispose();
      }
    }
    );

		// Create and set up the content pane.
		final GAMALoginGUI contentPane = new GAMALoginGUI();
		contentPane.setOpaque(true); // content panes must be opaque
		controllingFrame.setContentPane(contentPane);

		// Make sure the focus goes to the right component
		// whenever the frame is initially given the focus.
		controllingFrame.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				contentPane.resetFocus();
			}
		});

		// Display the window.
		controllingFrame.pack();
		controllingFrame.setLocationRelativeTo(null); // stay in the center
		controllingFrame.setVisible(true);
	}

	/**
	 * Function to show the login dialog It adds the login dialog into the
	 * event-dispatching thread
	 * 
	 * @param p
	 *            The ProxyRepository got from the AuthenticationManager
	 */
	public static void fire() {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	/**
	 * get the username from the gui
	 */
	public static String getUsername() {
		return username;
	}

	/**
	 * get the password from the gui
	 */
	public static String getPassword() {
		return password;
	}
}