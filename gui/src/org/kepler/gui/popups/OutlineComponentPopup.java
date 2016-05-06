/*
 * Copyright (c) 2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-08-30 16:13:08 -0700 (Thu, 30 Aug 2012) $' 
 * '$Revision: 30583 $'
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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.tree.TreePath;

import org.kepler.gui.ShowDocumentationAction;
import org.kepler.gui.component.OpenCompositeAction;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.Relation;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.basic.BasicGraphFrame;

/**
 * Subclass of NoLiidLibraryPopup that handles Components that are contained within
 * the workflow Outline view.
 * 
 * @author Philippe Huynh
 * 
 */
public class OutlineComponentPopup extends NoLiidLibraryPopup {

	public OutlineComponentPopup(TreePath path, Component comp) {
		super(path, comp);

	}

	public void initialize() {

		final Object obj = getSelectionPath().getLastPathComponent();
        KeplerLSID lsid = null;
        NamedObjId namedObjId = org.kepler.moml.NamedObjId.getIdAttributeFor((NamedObj) obj);
        if(namedObjId != null) {
        	lsid = namedObjId.getId();
        }
   
        // if it's a composite, add Open menu item
        if (obj instanceof ptolemy.kernel.CompositeEntity) {
           OpenCompositeAction oca = new OpenCompositeAction(getParentFrame());
           oca.setLsidToOpen(lsid);
           oca.setNamedObjToOpen((NamedObj) obj);
           this.add(new JMenuItem(oca));
        }
		
		// View Documentation
        // do not show documentation for attributes, ports, relations, etc.
        if (obj instanceof ComponentEntity) {
			ShowDocumentationAction sda = new ShowDocumentationAction(
					getSelectionPath(), getParentFrame());
			sda.setPtolemyFrame(getParentFrame());
			this.add(new JMenuItem(sda));
        }

		// Show On Canvas centers the canvas on the target
        // do not show for relations
        if(!(obj instanceof Relation)) {
            AbstractAction action = new AbstractAction("Show On Canvas") {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    BasicGraphFrame.openComposite(getParentFrame(), (NamedObj) obj);
                }
            };
			add(new JMenuItem(action));
        }
	}
}
