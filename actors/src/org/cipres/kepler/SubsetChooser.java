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

package org.cipres.kepler;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

public class SubsetChooser extends JFrame {

	private static final long serialVersionUID = -4694408842783273897L;

	private static Logger logger = Logger.getLogger(SubsetChooser.class
			.getName());

	private JPanel pnlSubsetChooser = new JPanel();
	private JPanel pnlMain;
	private DisplayObject[] displayObjects;
	private JCheckBox[] checkboxes;
	private JDialog dialog;
	private String headerText;

	/*************************************************************
	 * CONSTRUCTORS
	 **************************************************************/
	public SubsetChooser(DisplayObject[] displayObjects) throws Exception {
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.displayObjects = displayObjects;
	}

	public SubsetChooser(DisplayObject[] displayObjects, String headerText)
			throws Exception {

		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		this.displayObjects = displayObjects;
		this.headerText = headerText;

	}

	public JPanel getSubsetChooserPanel() throws Exception {
		buildPanel(false);
		return pnlMain;
	}

	public DisplayObject[] showSubsetChooserAsDialog() throws Exception {
		buildPanel(true);
		dialog = new JDialog();
		dialog.getContentPane().add(this.pnlMain);
		dialog.setModal(true);
		dialog.pack();

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closePanelAsDialog();
			}
		});

		// make sure dialog size is not > 80% of screen size
		Dimension dialogSize = dialog.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		dialogSize.height = Math.min(dialogSize.height,
				(int) (screenSize.height * .8));
		dialogSize.width = Math.min(dialogSize.width,
				(int) (screenSize.width * .8));
		dialog.setSize(dialogSize);

		// Center the dialog
		dialog.setLocation((screenSize.width - dialogSize.width) / 2,
				(screenSize.height - dialogSize.height) / 2);

		dialog.show();
		dialog.dispose();

		return getSubset();
	}

	private void closePanelAsDialog() {
		dialog.dispose();
	}

	public DisplayObject[] getSubset() {
		ArrayList aL = new ArrayList();
		for (int i = 0; i < checkboxes.length; i++) {
			if (checkboxes[i].isSelected()) {
				aL.add(this.displayObjects[i]);
				;
			}
		}
		aL.trimToSize();
		return (DisplayObject[]) aL.toArray(new DisplayObject[aL.size()]);
	}

	/*************************************************************
	 * PRIVATE METHODS
	 **************************************************************/

	private void buildPanel(boolean bShowOkButton) throws Exception {

		// ui elements
		GridBagLayout gridBagLayout1 = new GridBagLayout();
		JLabel lblItem = new JLabel("Item");
		JButton btnSelectAll2 = new JButton("Select All");
		JButton btnDeselectAll1 = new JButton("Deselect All");
		JCheckBox checkBox = new JCheckBox("Checkbox1");
		JButton btnSelectAll1 = new JButton("Select All");
		JButton btnDeselectAll2 = new JButton("Deselect All");
		JScrollPane jScrollPane = new JScrollPane();

		// build content panes
		pnlMain = new JPanel();
		BorderLayout borderLayout = new BorderLayout();
		pnlMain.setLayout(borderLayout);

		pnlSubsetChooser.setLayout(gridBagLayout1);
		this.getContentPane().add(jScrollPane, java.awt.BorderLayout.WEST);
		jScrollPane.getViewport().add(pnlSubsetChooser);
		pnlMain.add(jScrollPane, BorderLayout.WEST);
		jScrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		jScrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		int row = 0;

		// add header text if set in ctor
		if (headerText != null) {
			JLabel lblHeader = new JLabel(headerText);
			pnlSubsetChooser.add(lblHeader, new GridBagConstraints(1, row, 2,
					1, 0.0, 0.0, GridBagConstraints.NORTH,
					GridBagConstraints.NONE, new Insets(15, 15, 15, 0), 0, 0));
			row++;
		}
		// add top-row buttons
		pnlSubsetChooser.add(btnSelectAll1, new GridBagConstraints(1, row, 1,
				1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.NONE,
				new Insets(15, 15, 15, 0), 0, 0));

		pnlSubsetChooser.add(btnDeselectAll1, new GridBagConstraints(2, row, 1,
				1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(15, 5, 15, 15), 0, 0));
		row++;

		// add item header
		lblItem.setFont(new java.awt.Font("Dialog", 1, 14));
		pnlSubsetChooser.add(lblItem, new GridBagConstraints(1, row, 2, 1, 0.0,
				0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 15, 5, 0), 0, 0));

		// add list of selectable items
		checkboxes = new JCheckBox[displayObjects.length];
		for (int i = 0; i < displayObjects.length; i++) {
			row++;
			checkBox = new JCheckBox(displayObjects[i].getName(), true);
			checkboxes[i] = checkBox;
			pnlSubsetChooser.add(checkBox, new GridBagConstraints(1, row, 2, 1,
					0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 15, 0, 0), 0, 0));
		}

		// add bottom-row buttons
		row++;
		pnlSubsetChooser.add(btnSelectAll2, new GridBagConstraints(1, row, 1,
				1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(15, 15, 15, 0), 0, 0));

		pnlSubsetChooser.add(btnDeselectAll2, new GridBagConstraints(2, row, 1,
				1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(15, 5, 15, 15), 0, 0));

		if (bShowOkButton) {
			JButton btnOk = new JButton("Ok");
			row++;
			pnlSubsetChooser.add(btnOk, new GridBagConstraints(1, row, 1, 1,
					0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 15, 15, 0), 0, 0));
			btnOk.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Component c = (Component) e.getSource();
					if (c.hasFocus()) {
						closePanelAsDialog();
					}
				}
			});
		}

		// add action listeners
		btnSelectAll1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Component c = (Component) e.getSource();
				if (c.hasFocus()) {
					selectAll(true);
				}
			}
		});

		btnSelectAll2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Component c = (Component) e.getSource();
				if (c.hasFocus()) {
					selectAll(true);
				}
			}
		});

		btnDeselectAll1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Component c = (Component) e.getSource();
				if (c.hasFocus()) {
					selectAll(false);
				}
			}
		});

		btnDeselectAll2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Component c = (Component) e.getSource();
				if (c.hasFocus()) {
					selectAll(false);
				}
			}
		});

	} // end buildPanel

	private void selectAll(boolean bSelect) {
		for (int i = 0; i < checkboxes.length; i++) {
			checkboxes[i].setSelected(bSelect);
		}
	}

}
