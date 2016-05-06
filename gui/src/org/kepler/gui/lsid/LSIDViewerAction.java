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

/**
 * 
 */
package org.kepler.gui.lsid;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * An action for viewing the LSID and LSID Referral List (aka Derived From list)
 * in a popup dialog box.
 * 
 * @author Aaron Schultz
 * 
 */
public class LSIDViewerAction extends FigureAction {

	private static final Log log = LogFactory.getLog(LSIDViewerAction.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private final static String LABEL = "View LSID";

	private TableauFrame _parent;
	private NamedObj _obj;

	public LSIDViewerAction(TableauFrame parent) {
		super(LABEL);

		_parent = parent;

		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"ExportArchiveAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}

		this.putValue("tooltip", "Open the LSID Viewer Dialog.");
	}
	
	public void setObject(NamedObj no) {
		_obj = no;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		if (isDebugging)
			log.debug("LSIDViewerAction.actionPerformed()");

		NamedObj object = null;
		if (_obj == null) {
			object = getTarget();
		} else {
			object = _obj;
		}
		
		if (isDebugging)
			log.debug( object.getClass().getName() );

		if (object instanceof Entity) {
			
		} else if (object instanceof Attribute) {
			
		} else {

			// get the entity from parent
			Tableau tableau = _parent.getTableau();
			Effigy effigy = (Effigy) tableau.getContainer();
			Entity entity = null;
			if (effigy instanceof PtolemyEffigy) {
				entity = (Entity) ((PtolemyEffigy) effigy).getModel();
			}

			if (entity == null)
				return;
			object = entity;
		}

		LSIDViewer lv = new LSIDViewer();
		lv.setEditingEnabled(false);
		lv.initialize((NamedObj) object);
		lv.setSize(new Dimension(400, 300));
		lv.setLocation(_parent.getLocation());
		lv.setVisible(true);

	}

}
