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
import java.io.File;

import javax.swing.JMenuItem;
import javax.swing.tree.TreePath;

import org.kepler.gui.kar.ImportModuleDependenciesAction;
import org.kepler.gui.kar.ViewManifestAction;
import org.kepler.moml.KARErrorEntityLibrary;
import org.kepler.objectmanager.library.LibIndex;
import org.kepler.objectmanager.library.LibItem;

/**
 * Subclass of LibraryPopup that handles the popup menu for KARs inside the
 * library that do not have their module dependencies satisfied by the 
 * current module configuration.
 * 
 * @author Aaron Schultz
 * 
 */
public class KARErrorPopup extends LibraryPopup {

	public KARErrorPopup(TreePath path, Component comp) {
		super(path, comp);

		Object obj = getSelectionPath().getLastPathComponent();

		if (obj instanceof KARErrorEntityLibrary) {
			File karFile = null;

			try {
				
				LibItem li = getInfo();
				String karFileStr = li.getAttributeValue(LibIndex.ATT_KARFILE);
				karFile = new File(karFileStr);

			} catch (Exception e) {
				e.printStackTrace();
			}
			
			ImportModuleDependenciesAction imda = new ImportModuleDependenciesAction(getParentFrame());
			if (karFile != null) {
				imda.setArchiveFile(karFile);
			}
			this.add(new JMenuItem(imda));

			ViewManifestAction vma = new ViewManifestAction(getParentFrame());
			if (karFile != null) {
				vma.setArchiveFileToOpen(karFile);
			}
			this.add(new JMenuItem(vma));

		}

	}
}
