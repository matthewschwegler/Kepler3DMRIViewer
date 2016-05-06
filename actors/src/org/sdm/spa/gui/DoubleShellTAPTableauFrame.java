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

package org.sdm.spa.gui;

import java.awt.BorderLayout;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// DoubleShellTAPTableauFrame
/**
 * Based on the Ptolemy II v.3.0.2 code for ExpressionShellFrame.
 * 
 * @author Ilkay Altintas
 * @version $Id: DoubleShellTAPTableauFrame.java 7475 2003-10-15 01:21:06Z
 *          altintas $
 */
public class DoubleShellTAPTableauFrame extends TableauFrame {

	/**
	 * Construct a frame to display the ExpressionShell window. After
	 * constructing this, it is necessary to call setVisible(true) to make the
	 * frame appear. This is typically accomplished by calling show() on
	 * enclosing tableau.
	 * 
	 * @param tableau
	 *            The tableau responsible for this frame.
	 * @exception IllegalActionException
	 *                If the model rejects the configuration attribute.
	 * @exception NameDuplicationException
	 *                If a name collision occurs.
	 */
	public DoubleShellTAPTableauFrame(DoubleShellTAPTableau tableau)
			throws IllegalActionException, NameDuplicationException {
		super(tableau);

		JPanel component = new JPanel();
		component.setLayout(new BoxLayout(component, BoxLayout.Y_AXIS));

		tableau.shellPanel = new DoubleShellTextAreaPanel();
		tableau.shellPanel.setInterpreter(tableau);
		component.add(tableau.shellPanel);
		getContentPane().add(component, BorderLayout.CENTER);
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	protected void _help() {
		try {
			URL doc = getClass().getClassLoader().getResource(
					"doc/expressions.htm");
			getConfiguration().openModel(null, doc, doc.toExternalForm());
		} catch (Exception ex) {
			System.out.println("ExpressionShellTableau._help(): " + ex);
			_about();
		}
	}
}