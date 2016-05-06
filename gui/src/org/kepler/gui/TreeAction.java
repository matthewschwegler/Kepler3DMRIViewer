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
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.tree.TreePath;

import ptolemy.vergil.toolbox.FigureAction;

/**
 * This action adds annotations to the tree
 * 
 *@author Chad Berkley
 *@since February 17, 2005
 */
public abstract class TreeAction extends FigureAction {
	protected TreePath path;
	protected Component parent;
	private Vector listeners = new Vector();

	/**
	 * Constructor
	 * 
	 *@param path
	 *            Description of the Parameter
	 *@param parent
	 *            Description of the Parameter
	 */
	public TreeAction(TreePath path, Component parent, String label) {
		super(label);
		this.path = path;
		this.parent = parent;
	}

	/**
	 * Adds a listener for the new_folder_created event
	 * 
	 *@param listener
	 */
	public void addListener(ActionListener listener) {
		listeners.addElement(listener);
	}

	/**
	 * return the path where the folder was added
	 */
	public TreePath getPath() {
		return path;
	}

	/**
	 * notify any listeners that a new folder has been created.
	 */
	protected void notifyListeners(String eventName) {
		for (int i = 0; i < listeners.size(); i++) {
			ActionListener listener = (ActionListener) listeners.elementAt(i);
			listener.actionPerformed(new ActionEvent(this, 1, eventName));
		}
	}
}