/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.kepler.authentication.Domain;
import org.kepler.authentication.DomainList;

/**
 * DomainSelectionGUI pops up the pre-login dialog to let user select the
 * authentication domain
 * 
 * @author Ben Leinfelder
 * 
 */

public class DomainSelectionGUI extends JPanel implements ActionListener {
	private static JFrame controllingFrame; // The Frame of login dialog

	// Command button constants
	private String OK = "ok";
	private String CANCEL = "cancel";

	// Input fields
	private JComboBox domainField;
	private static Vector domainList;
	private static String domain;
	
	// XXX magic string, get rid of this
	public static String DOMAIN_BREAK = "BREAK";

	/**
	 * The constructor is used to build all the display components
	 * 
	 */
	public DomainSelectionGUI() {
		// set up the list
		try {
			domainList = DomainList.getInstance().getDomainList();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Domain field, label, and pane
		domainField = new JComboBox();
		Dimension dim = new Dimension(200, 20);
		domainField.setMinimumSize(dim);
		domainField.setPreferredSize(dim);

		// iterate through the list, add each domain name as option
		Iterator dIter = domainList.iterator();
		while (dIter.hasNext()) {
			Domain d = (Domain) dIter.next();
			String domainName = d.getDomain();
			domainField.addItem(domainName);
		}

		JLabel domainLabel = new JLabel("Authentication Domain: ");
		domainLabel.setLabelFor(domainField);
		JPanel domainPane = new JPanel(new FlowLayout());
		domainPane.add(domainLabel);
		domainPane.add(domainField);

		// Lay out input fields and labels on inputPane
		JPanel inputPane = new JPanel();
		inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.Y_AXIS));
		inputPane.add(domainPane);

		// Buttons and their buttonPane
		JButton okButton = new JButton("OK");
		JButton resetButton = new JButton("Cancel");
		okButton.setActionCommand(OK);
		okButton.addActionListener(this);
		resetButton.setActionCommand(CANCEL);
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

		if (OK.equals(cmd)) {
			// User selects domain and clicks OK button
			domain = (String) domainField.getSelectedItem();
			System.out.println("OK - disposing");
			controllingFrame.dispose();
		} else if (CANCEL.equals(cmd)) {
			domain = DOMAIN_BREAK;
			System.out.println("user cancelled action - disposing");
			controllingFrame.dispose();
		}
	}

	// Method to reset the focus of the login dialog
	// Must be called from the event-dispatching thread.
	protected void resetFocus() {
		domainField.requestFocusInWindow();
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	public static void createAndShowGUI() {
		resetFields();
		// Make sure we have nice window decorations.
		System.out.println("creating gui");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Create and set up the window.
		controllingFrame = new JFrame("Domain Selection");
		//controllingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		controllingFrame.addWindowListener(new WindowAdapter(){
	    public void windowClosing(WindowEvent e) {
	      domain = DOMAIN_BREAK;
	      controllingFrame.dispose();
	    }
		}
		);

		// Create and set up the content pane.
		final DomainSelectionGUI contentPane = new DomainSelectionGUI();
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
	 */
	public static void fire() {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		System.out.println("Firing new thread");
		resetFields();
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				System.out.println("Running in new thread");
				createAndShowGUI();
				System.out.println("exiting new thread");
			}
		});
	}

	/**
	 * return the domain
	 */
	public static String getDomain() {
		return domain;
	}

	/**
	 * resets the static fields to null
	 */
	public static void resetFields() {
		domain = null;
	}
}