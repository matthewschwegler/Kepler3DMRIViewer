/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:22:25 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31122 $'
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

package org.kepler.gui.popups;

import java.awt.Component;

import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.gui.PtolemyFrame;

/**
 * A class for organizing popup menus in the Library. By extending this class
 * you can add actions to the different types of items that show up in the
 * library. See examples such as OntologyComponentPopup. See the AnnotatedPTree
 * method "maybeShowPopup" for where to add the subclass in order for it to show
 * up.
 * 
 * @author Aaron Schultz
 * 
 */
public class NoLiidLibraryPopup extends JPopupMenu {

	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(LibraryPopup.class);
	private static final boolean isDebugging = log.isDebugEnabled();
	
	private TreePath _selectionPath;
	private PtolemyFrame _parentFrame;
	private int _liid;

	public NoLiidLibraryPopup() {
		_selectionPath = null;
		_parentFrame = null;
	}

	/**
	 * @param path
	 * @param comp
	 */
	public NoLiidLibraryPopup(TreePath path, Component comp) {

		while (comp != null && !(comp instanceof PtolemyFrame)) {
			comp = comp.getParent();
		}

		if (comp == null) {
			System.out.println("Cannot find the PtolemyFrame.");
			return;
		}

		setParentFrame((PtolemyFrame) comp);
		setSelectionPath(path);
		
	}

	/**
	 * Override this method with the good stuff.
	 */
	public void initialize() {
	}

	public NoLiidLibraryPopup(TreePath path, PtolemyFrame parentFrame) {
		_selectionPath = path;
		_parentFrame = parentFrame;
	}

//	public LibItem getInfo() {
//		LibItem li = LibraryManager.getInstance().getTreeItemIndexInformation(
//				getLiid());
//		return li;
//	}

//	public int getLiid() {
//		return _liid;
//	}

//	private void setLiid(int liid) {
//		_liid = liid;
//	}
//
	public TreePath getSelectionPath() {
		return _selectionPath;
	}

	public void setSelectionPath(TreePath selectionPath) {
		_selectionPath = selectionPath;
	}

	public PtolemyFrame getParentFrame() {
		return _parentFrame;
	}

	public void setParentFrame(PtolemyFrame parentFrame) {
		_parentFrame = parentFrame;
	}

}
