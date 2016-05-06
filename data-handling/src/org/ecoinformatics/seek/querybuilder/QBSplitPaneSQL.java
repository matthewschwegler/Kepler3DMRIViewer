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

package org.ecoinformatics.seek.querybuilder;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Simple pane containing a text control for displaying the SQL of the query
 */
public class QBSplitPaneSQL extends JPanel {
	protected JEditorPane mEditor;

	/**
	 * QBSplitPane Constructor
	 */
	public QBSplitPaneSQL() {
		setLayout(new BorderLayout());

		mEditor = new JEditorPane();
		mEditor.setEditable(false);
		mEditor.setBackground(Color.LIGHT_GRAY);
		JScrollPane scrollpane = new JScrollPane(mEditor);
		add(scrollpane, BorderLayout.CENTER);
	}

	/**
	 * Set the text in the text control for displaying the SQL
	 * 
	 * @param aText
	 *            the string to be displayed
	 */
	public void setText(String aText) {
		if (aText != null) {
			if (aText.length() == 0) {
				aText = "Sorry, no valid SQL.";
			}
			mEditor.setEditable(true);
			mEditor.setText(aText);
			mEditor.setEditable(false);
		}
	}

}