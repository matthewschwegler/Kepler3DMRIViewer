/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A component for editing Step information.
 */
public class NewActorFrame extends JDialog {
	// CONSTANTS
	private final static int PAD = 5;
	private final static int HORIZONTAL = 200;
	private final static int SPACE = 3;
	private final static int WIDE_SPACE = 10;
	private final static int COLUMNS = 25;
	private final static int ROWS = 4;

	private Component parent;
	private JButton cancelButton;
	private JButton okButton;
	private JLabel actorNameLabel;
	private JLabel classNameLabel;
	private JLabel introLabel1;
	private JLabel introLabel2;
	private JLabel introLabel3;
	private JLabel introLabel4;
	private JTextField actorNameTextField;
	private JTextField classNameTextField;

	private Vector listeners = new Vector();

	/**
	 * Construct the editor dialog with the given Step.
	 * 
	 * @param parent
	 *            the parent frame for this dialog
	 */
	public NewActorFrame(Component parent) {
		super((java.awt.Frame) parent, "New Actor", true);
		this.parent = parent;
		init();
		setVisible(false);
	}

	private void init() {
		this.setName("New Actor");
		actorNameLabel = new JLabel("Actor name");
		classNameLabel = new JLabel("Class name");
		introLabel1 = new JLabel(
				"<html><body><p>Enter the name of the actor that you "
						+ "would like to add.  Then enter the Java "
						+ "class name of the actor.  This class must "
						+ "be in the classpath before adding the actor.</p></body></html>");
		okButton = new JButton("OK");
		cancelButton = new JButton("Cancel");
		actorNameTextField = new JTextField();
		classNameTextField = new JTextField();

		ActionHandler ahandler = new ActionHandler();
		cancelButton.addActionListener(ahandler);
		okButton.addActionListener(ahandler);

		Container c = getContentPane();
		c.setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		c.add(Box.createRigidArea(new Dimension(WIDE_SPACE, PAD)));

		JPanel horizPanel = new JPanel();
		horizPanel.setLayout(new BoxLayout(horizPanel, BoxLayout.Y_AXIS));

		JPanel labelPanel = new JPanel();
		// labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
		// labelPanel.add(Box.createRigidArea(new Dimension(HORIZONTAL, PAD)));
		// labelPanel.setPreferredSize(new Dimension(100, 20));
		// labelPanel.add(Box.createRigidArea(new Dimension(HORIZONTAL, PAD)));
		introLabel1.setPreferredSize(new Dimension(300, 80));
		// labelPanel.add(Box.createRigidArea(new Dimension(HORIZONTAL, PAD)));
		labelPanel.add(introLabel1);
		// labelPanel.add(introLabel2);
		// labelPanel.add(introLabel3);
		// labelPanel.add(introLabel4);
		// labelPanel.add(Box.createRigidArea(new Dimension(HORIZONTAL, PAD)));

		JPanel actorNamePanel = new JPanel();
		actorNameTextField.setColumns(15);
		actorNamePanel
				.setLayout(new BoxLayout(actorNamePanel, BoxLayout.X_AXIS));
		actorNamePanel.add(Box.createRigidArea(new Dimension(10, SPACE)));
		actorNamePanel.add(actorNameLabel);
		actorNamePanel.add(Box.createRigidArea(new Dimension(10, SPACE)));
		actorNamePanel.add(actorNameTextField);
		actorNamePanel.add(Box.createRigidArea(new Dimension(10, SPACE)));

		JPanel classNamePanel = new JPanel();
		classNameTextField.setColumns(15);
		classNamePanel
				.setLayout(new BoxLayout(classNamePanel, BoxLayout.X_AXIS));
		classNamePanel.add(Box.createRigidArea(new Dimension(10, SPACE)));
		classNamePanel.add(classNameLabel);
		classNamePanel.add(Box.createRigidArea(new Dimension(10, SPACE)));
		classNamePanel.add(classNameTextField);
		classNamePanel.add(Box.createRigidArea(new Dimension(10, SPACE)));

		// horizPanel.add(labelPanel);
		horizPanel.add(Box.createRigidArea(new Dimension(10, SPACE)));
		horizPanel.add(actorNamePanel);
		horizPanel.add(Box.createRigidArea(new Dimension(10, SPACE)));
		horizPanel.add(classNamePanel);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		c.add(labelPanel);
		c.add(horizPanel);
		c.add(Box.createRigidArea(new Dimension(20, SPACE)));
		c.add(buttonPanel);

		// put the window in the middle of its parent
		if (parent != null) {
			int x = parent.getX();
			int y = parent.getY();
			int width = parent.getWidth();
			int height = parent.getHeight();
			setLocation(x + ((width / 2) - (this.getWidth() / 2)), y
					+ ((height / 2) - (this.getHeight() / 2)));
		}

		pack();
	}

	/**
	 * add an action listener that will be notified when the ok or cancel button
	 * is clicked.
	 */
	public void addActionListener(ActionListener listener) {
		listeners.addElement(listener);
	}

	/**
	 * get the folder name from the user input. don't call this until the
	 * actionlistener has been fired.
	 */
	public String getActorName() {
		return actorNameTextField.getText();
	}

	/**
	 * get the class name from the user input. don't call this until the
	 * actionlistener has been fired.
	 */
	public String getClassName() {
		return classNameTextField.getText();
	}

	/**
	 * get the concept name from the user input. don't call this until the
	 * actionlistener has been fired.
	 */
	public String getConceptName() {
		return normalizeConceptName();
	}

	/**
	 * handle ok button events
	 */
	private void okButtonHandler(ActionEvent event) {
		for (int i = 0; i < listeners.size(); i++) {
			ActionListener listener = (ActionListener) listeners.elementAt(i);
			listener.actionPerformed(new ActionEvent(this, 1,
					"okbutton_clicked"));
		}
	}

	/**
	 * handle cancle button events
	 */
	private void cancelButtonHandler(ActionEvent event) {
		for (int i = 0; i < listeners.size(); i++) {
			ActionListener listener = (ActionListener) listeners.elementAt(i);
			listener.actionPerformed(new ActionEvent(this, 2,
					"cancelbutton_clicked"));
		}
	}

	/**
	 * normalize the foldername for the concept name
	 */
	private String normalizeConceptName() {
		// take the spaces out of the folder name and put it as the
		// concept name
		return actorNameTextField.getText().replaceAll("\\s", "");
	}

	/**
	 * Listener used to detect button presses
	 */
	private class ActionHandler implements ActionListener {
		/**
		 * Description of the Method
		 * 
		 * @param event
		 *            Description of Parameter
		 */
		public void actionPerformed(ActionEvent event) {
			Object object = event.getSource();
			if (object == okButton) {
				okButtonHandler(event);
			} else if (object == cancelButton) {
				cancelButtonHandler(event);
			}
		}
	}
}