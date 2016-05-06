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

package org.kepler.sms.gui;

/**
 * 
 */

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.kepler.sms.NamedOntProperty;

public class OntoPropSearchDialog extends JDialog {

	private NamedOntProperty _namedProp = null;
	private JList _choiceList = new JList();

	protected NamedOntProperty getChoice() {
		return _namedProp;
	}

	public static NamedOntProperty showDialog(Frame aFrame, Vector choices) {
		OntoPropSearchDialog d = new OntoPropSearchDialog(aFrame, choices);
		return d.getChoice();
	}

	protected OntoPropSearchDialog(Frame aFrame, Vector choices) {
		super(aFrame, true);
		setTitle("Search Results");

		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// add the label
		JPanel msgPanel = new JPanel();
		msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.X_AXIS));
		msgPanel.add(new JLabel("Select a matching property:",
				SwingConstants.LEFT));
		msgPanel.add(Box.createHorizontalGlue());
		pane.add(msgPanel);

		// add the choice list
		JScrollPane choiceView = new JScrollPane(_choiceList);
		_choiceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_choiceList.setListData(choices);
		_choiceList.setSelectedIndex(0);
		choiceView.setMinimumSize(new Dimension(250, 200));
		choiceView.setMaximumSize(new Dimension(Short.MAX_VALUE, 200));
		choiceView.setPreferredSize(new Dimension(250, 200));
		pane.add(choiceView);

		// add space between namespace and property
		pane.add(Box.createRigidArea(new Dimension(0, 5)));

		// add the button pane

		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(Box.createRigidArea(new Dimension(15, 0)));
		btnPanel.add(cancelBtn);
		pane.add(btnPanel);

		// add listeners to the buttons
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				_namedProp = (NamedOntProperty) _choiceList.getSelectedValue();
				// exit the window
				dispose();
			}
		});

		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				_namedProp = null;
				// exit the window
				dispose();
			}
		});

		// set up the dialog
		this.setContentPane(pane);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.pack();
		this.show();
	}
}