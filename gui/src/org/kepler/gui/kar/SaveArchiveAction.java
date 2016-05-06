/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-01-15 14:34:02 -0800 (Tue, 15 Jan 2013) $' 
 * '$Revision: 31330 $'
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

package org.kepler.gui.kar;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.KeyStroke;

import org.kepler.kar.KARFile;
import org.kepler.kar.KARManager;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * This action attempts to save a KAR in place without user interaction using
 * ExportArchiveAction. If this cannot be done, a Save As... is attempted using
 * ExportArchiveAction.
 */
public class SaveArchiveAction extends FigureAction {

	protected TableauFrame _parent;
	private static KeyStroke ACCELERATOR_KEYSTROKE = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, 
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

	/**
	 * Constructor
	 * 
	 * @param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public SaveArchiveAction(TableauFrame parent) {
		super("");
		
		putValue(ACCELERATOR_KEY, ACCELERATOR_KEYSTROKE);
		
		_parent = parent;

		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"SaveArchiveAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}

	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		KARFile karFile = KARManager.getInstance().get(_parent);
		ExportArchiveAction eaa = new ExportArchiveAction(_parent);

		// canWrite() checks if exists and writeable. We do in fact want
		// the existence check to occur, e.g. when we create tmp kar files
		// we sometimes delete them right after opening, and we want 
		// the user to get a Save As... prompt when they attempt to Save it.
		if (karFile == null || !karFile.getFileLocation().canWrite()) {
			eaa.actionPerformed(e);
		} else {
			eaa.setRefreshFrameAfterSave(false); 
			eaa.setSaveFile(karFile.getFileLocation());
			eaa.actionPerformed(e);
		}

		if (eaa.saveSucceeded()) {
			_parent.setModified(false);
		}
	}
}