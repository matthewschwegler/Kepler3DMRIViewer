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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import ptolemy.vergil.tree.PTree;

/**
 * This class extends the PTree class of Ptolemy. It will display the search
 * result in ResultPanel. This adds two new features - 1) a double click of
 * resultrecord will get metadata 2) add a right click button to get a new menu
 * - get metadata. Metadata and
 * 
 *@author Jing Tao
 *@created February 17, 2005
 */
public class DataSearchResultTree extends PTree {
	private JPopupMenu popup;
	private JMenuItem getMetadataMenuItem;
	private ResultPanel panel;

	/**
	 * Consturctor of Tree.
	 * 
	 *@param model
	 *            TreeModel
	 *@param panel
	 *            ResultPanel pass this to GetMetadataAction in order to get
	 *            Configuration
	 */
	public DataSearchResultTree(TreeModel model, ResultPanel panel) {
		super(model);
		this.panel = panel;
		MouseListener popupListener = new PopupListener();
		this.addMouseListener(popupListener);
	}

	/*
	 * This class will add a right click menu for get metadata info
	 */
	/**
	 * Description of the Class
	 * 
	 *@author berkley
	 *@created February 17, 2005
	 */
	class PopupListener extends MouseAdapter {
		// on the Mac, popups are triggered on mouse pressed, while
		// mouseReleased triggers them on the PC; use the trigger flag to
		// record a trigger, but do not show popup until the
		// mouse released event
		boolean trigger = false;

		/**
		 * Description of the Method
		 * 
		 *@param e
		 *            Description of the Parameter
		 */
		public void mousePressed(MouseEvent e) {
			// maybeShowPopup(e);
			if (e.isPopupTrigger()) {
				trigger = true;
			}
		}

		/**
		 * Description of the Method
		 * 
		 *@param e
		 *            Description of the Parameter
		 */
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		/**
		 * Description of the Method
		 * 
		 *@param e
		 *            Description of the Parameter
		 */
		private void maybeShowPopup(MouseEvent e) {
			if ((e.isPopupTrigger()) || (trigger)) {

				trigger = false;
				TreePath selPath = getPathForLocation(e.getX(), e.getY());
				if ((selPath != null)) {
					Object ob = selPath.getLastPathComponent();
					// only show resultrecord(not show menu in resultdetails)
					if (ob != null && ob instanceof ResultRecord) {
						ResultRecord resultItem = (ResultRecord) ob;
						// String namespace =
						// "eml://ecoinformatics.org/eml-2.0.0";
						// resultItem.setNamespace(namespace);
						GetMetadataAction getMetadataAction = new GetMetadataAction(
								resultItem);
						getMetadataMenuItem = new JMenuItem(getMetadataAction);
						popup = new JPopupMenu();
						popup.add(getMetadataMenuItem);

						setSelectionPath(selPath);
						popup.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}
		}
	}

}