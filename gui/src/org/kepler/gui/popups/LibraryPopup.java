/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2010-12-23 11:01:04 -0800 (Thu, 23 Dec 2010) $' 
 * '$Revision: 26600 $'
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
import org.kepler.objectmanager.library.LibItem;
import org.kepler.objectmanager.library.LibraryManager;

import ptolemy.actor.gui.PtolemyFrame;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;

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
public class LibraryPopup extends JPopupMenu {

	private static final long serialVersionUID = -5556895608334936467L;
	private static final Log log = LogFactory.getLog(LibraryPopup.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
	private TreePath _selectionPath;
	private PtolemyFrame _parentFrame;
	private int _liid;

	public LibraryPopup() {
		_selectionPath = null;
		_parentFrame = null;
	}

	/**
	 * @param path
	 * @param comp
	 */
	public LibraryPopup(TreePath path, Component comp) {

		while (comp != null && !(comp instanceof PtolemyFrame)) {
			comp = comp.getParent();
		}

		if (comp == null) {
			System.out.println("Cannot find the PtolemyFrame.");
			return;
		}

		setParentFrame((PtolemyFrame) comp);
		setSelectionPath(path);
		
		try {
			Object obj = getSelectionPath().getLastPathComponent();
			
			if (obj instanceof ComponentEntity) {
				int liid = LibraryManager.getLiidFor((ComponentEntity) obj);
				if (isDebugging) log.debug(liid);
				if (liid != -1) {
					setLiid(liid);
				} else {
					throw new Exception("LIID not found for "
							+ ((NamedObj) obj).getName() + " "
							+ obj.getClass().getName());
				}
			} else {
				throw new Exception("Object must be a NamedObj");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Override this method with the good stuff.
	 */
	public void initialize() {
	}

	public LibraryPopup(TreePath path, PtolemyFrame parentFrame) {
		_selectionPath = path;
		_parentFrame = parentFrame;
	}

	public LibItem getInfo() {
		LibItem li = LibraryManager.getInstance().getTreeItemIndexInformation(
				getLiid());
		return li;
	}

	public int getLiid() {
		return _liid;
	}

	private void setLiid(int liid) {
		_liid = liid;
	}

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
