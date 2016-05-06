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

package org.kepler.gui.kar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.KARFile;
import org.kepler.objectmanager.cache.LocalRepositoryManager;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.gui.ExtensionFilenameFilter;
import ptolemy.gui.JFileChooserBugFix;
import ptolemy.gui.PtFileChooser;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

/**
 * This action opens a kar file to the system. It is called from the O
 */
public class ViewManifestAction extends FigureAction {

	private static final long serialVersionUID = 4476849345160344931L;
	private static String DISPLAY_NAME = "View Manifest";
	private static String TOOLTIP = "View the manifest of this Kepler ARchive.";
	private static ImageIcon LARGE_ICON = null;
	private static KeyStroke ACCELERATOR_KEY = null;

	// //////////////////////////////////////////////////////////////////////////////

	private TableauFrame parent;

	private final static Log log = LogFactory.getLog(ViewManifestAction.class);
	private static final boolean isDebugging = log.isDebugEnabled();

	private File archiveFileToOpen = null;

	/**
	 * Constructor
	 * 
	 *@param parent
	 *            the "frame" (derived from ptolemy.gui.Top) where the menu is
	 *            being added.
	 */
	public ViewManifestAction(TableauFrame parent) {
		super(DISPLAY_NAME);
		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"ViewManifestAction constructor received NULL argument for TableauFrame");
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
	 * Explicitly set the Archive file that the action will open. If not file is
	 * set a File chooser dialog is displayed to the user.
	 * 
	 * @param archiveFile
	 */
	public void setArchiveFileToOpen(File archiveFile) {
		archiveFileToOpen = archiveFile;
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		File karFile = null;
		if (archiveFileToOpen != null) {

			karFile = archiveFileToOpen;

		} else {
			// ask the user what file to open
			// Create a file filter that accepts .kar files.
			ExtensionFilenameFilter filter = new ExtensionFilenameFilter(".kar", "kar");

                        // Avoid white boxes in file chooser, see
                        // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3801
                        JFileChooserBugFix jFileChooserBugFix = new JFileChooserBugFix();
                        Color background = null;
                        PtFileChooser chooser = null;
                        try {
                            background = jFileChooserBugFix.saveBackground();
                            chooser = new PtFileChooser(parent, "Choose archive", JFileChooser.OPEN_DIALOG);
                            chooser.setCurrentDirectory(LocalRepositoryManager.getInstance()
                                    .getSaveRepository());
                            chooser.addChoosableFileFilter(filter);

                            int returnVal = chooser.showDialog(parent, "Open");
                            if (returnVal == JFileChooser.APPROVE_OPTION) {
				// process the given kar file
				karFile = chooser.getSelectedFile();
                            }
                        } finally {
                            jFileChooserBugFix.restoreBackground(background);
                        }

		}
		if (karFile != null) {
			try {
				KarManifestViewer kmv = new KarManifestViewer();
				kmv.initialize(new KARFile(karFile));
				kmv.setSize(new Dimension(800, 400));
				kmv.setLocation(parent.getLocation());
				kmv.setVisible(true);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
	}
}
