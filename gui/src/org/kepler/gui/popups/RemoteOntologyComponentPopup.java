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
import java.util.Collections;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.tree.TreePath;

import org.kepler.gui.component.ToggleLsidAction;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * Subclass of LibraryPopup that handles library items that are components and
 * not inside of a KAR (i.e. components inside ontologies).
 * 
 * @author Aaron Schultz
 * 
 */
public class RemoteOntologyComponentPopup extends NoLiidLibraryPopup {

	public RemoteOntologyComponentPopup(TreePath path, Component comp) {
		super(path, comp);
	}

	public void initialize() {
		Object obj = getSelectionPath().getLastPathComponent();
//		KeplerLSID lsid = getInfo().getLsid();

		// LSID Toggle Menu
		addLsidToggleMenu();
		
		// View Documentation
//		ShowDocumentationAction sda = new ShowDocumentationAction(
//				getSelectionPath(), getParentFrame());
//		sda.setPtolemyFrame(getParentFrame());
//		sda.setLsidToView(lsid);
//		this.add(new JMenuItem(sda));

		// View LSID
//		LibLsidViewerAction lva = new LibLsidViewerAction(getParentFrame());
//		lva.setLsidToView(lsid);
//		this.add(new JMenuItem(lva));
	}

	/**
	 * It is possible that there is more than one LSID associated with this
	 * Library Item. Add a menu item that allows the user to toggle which LSID
	 * is currently the default LSID. If there is only one lsid associated with
	 * this item then we don't add the menu.
	 */
	private void addLsidToggleMenu() {

		JMenu jm = new JMenu("");
//		jm.add(new JMenuItem("Blah blah blah"));
//		Vector<KeplerLSID> lsids = getInfo().getLsids();
		List<KeplerLSID> lsids = Collections.emptyList();
		if (lsids.size() > 1) {
			for (KeplerLSID altLSID : lsids) {
				JMenuItem jmi = new JMenuItem(altLSID.toString());
				ToggleLsidAction tla = new ToggleLsidAction();
//				tla.setLiid(this.getLiid());
				jmi.addActionListener(tla);
				jm.add(jmi);
			}
			this.add(jm);
			this.add(new JSeparator());
		}
		this.add(jm);
	}
}
