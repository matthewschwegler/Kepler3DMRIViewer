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
import org.kepler.objectmanager.ObjectManager;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * An action for viewing the LSID and LSID Referral List (aka Derived From list)
 * in a popup dialog box.
 * 
 * @author Aaron Schultz
 * 
 */
public class LibLsidViewerAction extends FigureAction {

	private static final Log log = LogFactory.getLog(LibLsidViewerAction.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private final static String LABEL = "View LSID";

	private TableauFrame _parent;
	private KeplerLSID _lsidToView;
	
	public void setLsidToView(KeplerLSID lsid) {
		_lsidToView = lsid;
	}
	public KeplerLSID getLsidToView() {
		return _lsidToView;
	}

	public LibLsidViewerAction(TableauFrame parent) {
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

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		if (isDebugging)
			log.debug("LSIDViewerAction.actionPerformed()");

		/*
		 * So this class shouldn't really be needed. The LSID should show up in
		 * the popup menu itself, hopefully that'll happen one of these days.
		 * For now this kludge will let us see what LSID is the default LSID for
		 * a component in the library at least.
		 */
		try {
			NamedObj obj = ObjectManager.getInstance().getObjectRevision(getLsidToView());

			if (obj == null) {
				MessageHandler.message("Non-NamedObject LSID: " + getLsidToView().toString());
				return;
			}
			
			LSIDViewer lv = new LSIDViewer();
			lv.setEditingEnabled(false);
			lv.initialize((NamedObj) obj);
			lv.setSize(new Dimension(400, 300));
			lv.setLocation(_parent.getLocation());
			lv.setVisible(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
