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

import javax.swing.JMenuItem;
import javax.swing.tree.TreePath;

import org.kepler.gui.ShowDocumentationAction;
import org.kepler.gui.component.OpenCompositeAction;
import org.kepler.gui.lsid.LibLsidViewerAction;
import org.kepler.kar.KARFile;
import org.kepler.objectmanager.library.LibIndex;
import org.kepler.objectmanager.library.LibItem;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * Subclass of LibraryPopup that handles Components that are contained within a
 * KAR in the library.
 * 
 * @author Aaron Schultz
 * 
 */
public class KARComponentPopup extends LibraryPopup {

	private static final long serialVersionUID = 1404026700541313696L;

	public KARComponentPopup(TreePath path, Component comp) {
		super(path, comp);

	}

	public void initialize() {

		Object obj = getSelectionPath().getLastPathComponent();
		LibItem li = getInfo();
		KeplerLSID lsid = li.getLsid();
		
		if (li.getType() == LibIndex.TYPE_COMPONENT) {

			// Open
			String className = li.getAttributeValue(LibIndex.ATT_CLASSNAME);
			String ceClassName = "ptolemy.kernel.CompositeEntity";
			boolean isComposite = false;
			try {
				isComposite = KARFile.isSubclass(ceClassName, className);

				if (isComposite) {
					OpenCompositeAction oca = new OpenCompositeAction(getParentFrame());
					oca.setLsidToOpen(lsid);
					this.add(new JMenuItem(oca));
				}
			} catch (ClassNotFoundException e) {
				// ignore
			}

			// View Documentation
			ShowDocumentationAction sda = new ShowDocumentationAction(
					getSelectionPath(), getParentFrame());
			sda.setPtolemyFrame(getParentFrame());
			sda.setLsidToView(lsid);
			this.add(new JMenuItem(sda));

			// View LSID
			LibLsidViewerAction lva = new LibLsidViewerAction(getParentFrame());
			lva.setLsidToView(lsid);
			this.add(new JMenuItem(lva));

		} else if (getInfo().getType() == LibIndex.TYPE_NAMED_OBJ) {

			// View LSID
			LibLsidViewerAction lva = new LibLsidViewerAction(getParentFrame());
			lva.setLsidToView(lsid);
			this.add(new JMenuItem(lva));

		} else if (getInfo().getType() == LibIndex.TYPE_OBJECT) {
			
			// Open Object in external editor
			// TODO
			
		}

	}
}
