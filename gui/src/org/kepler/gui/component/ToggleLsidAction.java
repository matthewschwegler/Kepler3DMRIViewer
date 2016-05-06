/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

package org.kepler.gui.component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.GUIUtil;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * This action toggles the LSID of a Component Item in the Library.
 */
public class ToggleLsidAction implements ActionListener {

	private Integer _liid;

	public void setLiid(Integer liid) {
		_liid = liid;
	}

	public Integer getLiid() {
		return _liid;
	}

	private final static Log log = LogFactory.getLog(ToggleLsidAction.class);
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * Constructor
	 * 
	 *@param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public ToggleLsidAction() {}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {

		try {
			
			Object o = e.getSource();
			if (o instanceof JMenuItem) {
				JMenuItem jmi = (JMenuItem)o;
				String lsidStr = jmi.getText();
				if (isDebugging) log.debug( getLiid() + " " + lsidStr );
				LibraryManager lm = LibraryManager.getInstance();
				try {
					KeplerLSID newDefaultLSID = new KeplerLSID(lsidStr);
					lm.getIndex().updateDefaultLsid(getLiid(), newDefaultLSID);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(GUIUtil
							.getParentWindow(jmi), ex
							.getMessage());
				}
			}
			
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
}