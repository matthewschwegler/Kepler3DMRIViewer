/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-01-23 13:50:06 -0800 (Wed, 23 Jan 2013) $' 
 * '$Revision: 31361 $'
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

import org.kepler.gui.kar.DeleteArchiveAction;
import org.kepler.gui.kar.KarUploaderAction;
import org.kepler.gui.kar.OpenArchiveAction;
import org.kepler.gui.kar.ViewManifestAction;
import org.kepler.moml.KAREntityLibrary;
import org.kepler.objectmanager.library.LibIndex;

/**
 * Subclass of LibraryPopup that handles the popup menu for KARs inside the
 * library.
 * 
 * @author Aaron Schultz
 * 
 */
public class KARPopup extends LibraryPopup {

	private static final long serialVersionUID = 4797725289324539443L;

	public KARPopup(TreePath path, Component comp) {
		super(path, comp);

		Object obj = getSelectionPath().getLastPathComponent();

		if (obj instanceof KAREntityLibrary) {
			
			String karFileStr = getInfo().getAttributeValue(LibIndex.ATT_KARFILE);
			
			// make sure the file string is not null. the string can be null when
			// the kar is deleted but not removed from the library tree.			
			if(karFileStr != null) {
    			File karFile = new File(karFileStr);
    
    			// Open
    			OpenArchiveAction oaa = new OpenArchiveAction(getParentFrame());
    			if (karFile != null) {
    				oaa.setArchiveFileToOpen(karFile);
    			}
    			this.add(new JMenuItem(oaa));
    
    			ViewManifestAction vma = new ViewManifestAction(getParentFrame());
    			if (karFile != null) {
    				vma.setArchiveFileToOpen(karFile);
    			}
    			this.add(new JMenuItem(vma));
    			
    			// Upload To Repository
    			KarUploaderAction kua = new KarUploaderAction(getParentFrame());
    			if (karFile != null) {
    				kua.setArchiveFileToUpload(karFile);
    			}
    			this.add(new JMenuItem(kua));
    
    			// Delete
    			DeleteArchiveAction daa = new DeleteArchiveAction(getParentFrame());
    			daa.setArchiveFileToDelete(karFile);
    			this.add(new JMenuItem(daa));
			}
		}

		// this.add(new JMenuItem("Under Construction: KARPopup"));

		// Remove KAR from library here

	}
}
