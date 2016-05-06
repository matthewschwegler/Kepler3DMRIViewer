/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
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

import java.awt.Frame;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JDialog;

/**
 * This class encapsulates the simple browser for browsing and selecting
 * ontology classes.
 * 
 * @author Shawn Bowers
 */
public class SemanticTypeBrowserDialog extends JDialog {

	/**
	 * This is the default constructor
	 */
	public SemanticTypeBrowserDialog(Frame owner, boolean classView) {
		super(owner);
		_owner = owner;
		initialize(classView);
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize(boolean classView) {
		SemanticTypeBrowserPane pane = new SemanticTypeBrowserPane(this,
				classView);
		getContentPane().add(pane);
		setSize(SemanticTypeBrowserPane.PREFERRED_WIDTH,
				SemanticTypeBrowserPane.PREFERRED_HEIGHT);
		setTitle("Semantic Type Browser");
		setResizable(false);
		pane.addSelectionListener(new _SelectionListener());
	}

	/**
	 * This is the default constructor
	 */
	public SemanticTypeBrowserDialog(Frame owner) {
		super(owner);
		_owner = owner;
		initialize();
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		SemanticTypeBrowserPane pane = new SemanticTypeBrowserPane(this);
		getContentPane().add(pane);
		setSize(SemanticTypeBrowserPane.PREFERRED_WIDTH,
				SemanticTypeBrowserPane.PREFERRED_HEIGHT);
		setTitle("Semantic Type Browser");
		setResizable(false);
		pane.addSelectionListener(new _SelectionListener());
	}

	/**
	 * This method permits listeners to sign up for selection events from the
	 * dialog. That is, an event is broadcast whenever a user hits the select
	 * button
	 */
	public void addSelectionListener(SemanticTypeBrowserSelectionListener l) {
		if (!_listeners.contains(l))
			_listeners.add(l);
	}

	/**
	 * A private class for signing up to panel events.
	 */
	private class _SelectionListener implements
			SemanticTypeBrowserSelectionListener {
		public void valueChanged(SemanticTypeBrowserSelectionEvent e) {
			for (Iterator iter = _listeners.iterator(); iter.hasNext();) {
				SemanticTypeBrowserSelectionListener l = (SemanticTypeBrowserSelectionListener) iter
						.next();
				l.valueChanged(e);
			}
		}
	}

	public static void main(String[] args) {
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
					.getSystemLookAndFeelClassName());
			// SemanticTypeBrowserDialog d = new SemanticTypeBrowserDialog(null,
			// falss);
			SemanticTypeBrowserDialog d = new SemanticTypeBrowserDialog(null);
			d.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* PRIVATE MEMBERS */

	private Vector _listeners = new Vector(); // listeners
	private Frame _owner; // the frame that called this one
}