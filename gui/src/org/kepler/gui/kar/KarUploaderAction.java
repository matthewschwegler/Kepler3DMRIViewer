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
package org.kepler.gui.kar;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.KARFile;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

/**
 * This action can be used for uploading a single Item KAR to a remote
 * repository.
 * 
 * 
 * @author Aaron Schultz
 * 
 */
public class KarUploaderAction extends FigureAction {

	private static final Log log = LogFactory.getLog(KarUploaderAction.class);
	private static final boolean isDebugging = log.isDebugEnabled();

	private File archiveFileToUpload = null;

	private TableauFrame _parent;

	private static String DISPLAY_NAME = "Upload";
	private static String TOOLTIP = "Upload a KAR file archive.";
	private static ImageIcon LARGE_ICON = null;
	private static KeyStroke ACCELERATOR_KEY = null;

	/**
	 * @param name
	 */
	public KarUploaderAction(TableauFrame parent) {
		super("Upload");

		if (parent == null) {
			IllegalArgumentException iae = new IllegalArgumentException(
					"ActorUploaderAction constructor received NULL argument for TableauFrame");
			iae.fillInStackTrace();
			throw iae;
		}
		_parent = parent;

		initialize();
	}

	protected void initialize() {

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
	public void setArchiveFileToUpload(File archiveFile) {
		archiveFileToUpload = archiveFile;
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		ComponentUploader uploader = new ComponentUploader(_parent);

		KARFile kf2upload;
		try {
			kf2upload = new KARFile(archiveFileToUpload);
			uploader.upload(kf2upload);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

}
