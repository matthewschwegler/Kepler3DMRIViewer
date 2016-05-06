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
import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

import ptolemy.vergil.basic.BasicGraphFrame;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * This action removes actors from the tree
 * 
 *@author Chad Berkley
 *@since February 17, 2005
 */
public class RemoveActorAction extends FigureAction {
	private final static String LABEL = "Remove Actor";
	private TreePath path;
	private Component parent;
	private NewActorFrame naFrame;

	/**
	 * Constructor
	 * 
	 *@param path
	 *            the TreePath where the actor is being removed.
	 */
	public RemoveActorAction(TreePath path, Component parent) {
		super(LABEL);
		this.path = path;
		this.parent = parent;
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Component current = parent;
		while (parent != null && !(parent instanceof BasicGraphFrame)) {
			parent = current.getParent();
			current = parent;
		}

		int userChoice = JOptionPane
				.showConfirmDialog(
						null,
						"Are you sure you "
								+ "want to remove this actor?  It will no longer be accessible from the "
								+ "actor library.  Click \"Yes\" to remove the actor.",
						"Are you sure?", JOptionPane.YES_NO_OPTION);
		if (userChoice == JOptionPane.YES_OPTION) {
			System.out.println("removing actor...");
			JOptionPane
					.showMessageDialog(
							null,
							"This functionality isn't implemented yet...check back in alpha 6.",
							"alert", JOptionPane.ERROR_MESSAGE);
		}
	}
}