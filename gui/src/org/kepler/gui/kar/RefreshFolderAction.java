/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-06-11 17:46:51 -0700 (Mon, 11 Jun 2012) $' 
 * '$Revision: 29920 $'
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.cache.LocalRepositoryManager;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.gui.JFileChooserBugFix;
import ptolemy.gui.PtFileChooser;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

public class RefreshFolderAction extends FigureAction {

	private static String DISPLAY_NAME = "Refresh";
	private static String TOOLTIP = "Refresh the folder contents.";
	private static ImageIcon LARGE_ICON = null;
	private static KeyStroke ACCELERATOR_KEY = null;

	// //////////////////////////////////////////////////////////////////////////////

	private TableauFrame parent;

	private final static Log log = LogFactory.getLog(RefreshFolderAction.class);
	private static final boolean isDebugging = log.isDebugEnabled();

	private File _folder = null;

	/**
	 * Constructor
	 * 
	 *@param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public RefreshFolderAction(TableauFrame parent) {
		super("Refresh");
		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"RefreshFolderAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}
		this.parent = parent;

		this.putValue(Action.NAME, DISPLAY_NAME);
		this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
		this.putValue("tooltip", TOOLTIP);
		this.putValue(GUIUtilities.ACCELERATOR_KEY, ACCELERATOR_KEY);
	}

	/**
	 * Explicitly set the folder that the action will open. If no file is set a
	 * File chooser dialog is displayed to the user.
	 * 
	 * @param archiveFile
	 */
	public void setFolderToRefresh(File folder) {
		_folder = folder;
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		File folder = null;
		if (_folder != null) {

			folder = _folder;

		} else {
                        // Avoid white boxes in file chooser, see
                        // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3801
                        JFileChooserBugFix jFileChooserBugFix = new JFileChooserBugFix();
                        Color background = null;
                        PtFileChooser chooser = null;

                        try {
                            background = jFileChooserBugFix.saveBackground();
                            // ask the user what file to refresh
                            chooser = new PtFileChooser(parent, "Choose folder", JFileChooser.OPEN_DIALOG);
                            chooser.setCurrentDirectory(LocalRepositoryManager.getInstance()
					.getSaveRepository());
                            int returnVal = chooser.showDialog(parent, "Refresh");
                            if (returnVal == JFileChooser.APPROVE_OPTION) {
				// process the given kar file
				folder = chooser.getSelectedFile();
                            }
                        } finally {
                            jFileChooserBugFix.restoreBackground(background);
                        }
		}
		if (folder != null) {
			System.out.println("Under Construction: "+folder.toString()+" was not refreshed.");
		}
		LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
		//lrm.refreshFolderModelForFolder(folder);
	}
}
