/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:19:36 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31113 $'
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

package org.cipres.kepler;

// Ptolemy package
import java.awt.Color;
import java.io.File;

import javax.swing.JFileChooser;

import org.cipres.util.Config;
import org.cipres.util.file.cipresFile;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.type.BaseType;
import ptolemy.gui.JFileChooserBugFix;
import ptolemy.gui.PtFileChooser;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// NexusFileReader
/**
 * The NexusFileReader actor reads a Nexus file from the local file system and
 * sends the file content as a string token.
 * 
 * @author Zhijie Guan
 * @version $Id: NexusFileReader.java 31113 2012-11-26 22:19:36Z crawl $
 */

public class NexusFileReader extends TypedAtomicActor {

	/**
	 * Construct a NexusFileReader actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor. by the proposed container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */

	public NexusFileReader(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// construct the input port inputTrigger
		inputTrigger = new TypedIOPort(this, "inputTrigger", true, false);
		inputTrigger.setDisplayName("Trigger");
		inputTrigger.setTypeEquals(BaseType.GENERAL);

		// construct the output port outputFileContent
		outputFileContent = new TypedIOPort(this, "outputFileContent", false,
				true);
		outputFileContent.setDisplayName("File Content");
		// Set the type constraint.
		outputFileContent.setTypeEquals(BaseType.GENERAL);

		// construct the parmeter fileNamePar
		// set the default value of this parameter as an empty string ("")
		// since this parameter should only accept a string value
		fileNamePar = new FileParameter(this, "fileNamePar");
		fileNamePar.setDisplayName("Nexus File Name");

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * A trigger signal that enables the actor is passed to the NexusFileReader
	 * actor through this input port. This port is an input port of type
	 * GENERAL.
	 */
	public TypedIOPort inputTrigger = null;

	/**
	 * The Nexus file content is sent out through this output port. This port is
	 * an output port of type GENERAL.
	 */
	public TypedIOPort outputFileContent = null;

	/**
	 * The Nexus file name is set in this parameter. The NexusFileReader actor
	 * opens and reads the file specified by this parameter and sends out the
	 * content through the outputFileContent port. If the user leaves this
	 * parameter empty, the NexusFileReader will pop up a file chooser dialog to
	 * let the user specify the file in the local file system.
	 */
	public FileParameter fileNamePar = null;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Send the content of the Nexus file to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		if (inputTrigger.hasToken(0)) {
			inputTrigger.get(0);
			// get the nexus file name
			String fileNameStr = ((StringToken) fileNamePar.getToken())
					.stringValue();
			if (fileNameStr.length() == 0) {
                            // Avoid white boxes in file chooser, see
                        // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3801
                            JFileChooserBugFix jFileChooserBugFix = new JFileChooserBugFix();
                            Color background = null;
                            try {
                                background = jFileChooserBugFix.saveBackground();

				// if the file name is empty, the actor pops up a file chooser
				// dialog
				PtFileChooser fileChooser = new PtFileChooser(null, "Choose Nexus File", JFileChooser.OPEN_DIALOG);
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				Config config = Config.getInstance();
				if(config != null) {
					fileChooser.setCurrentDirectory(new File(config.getDefaultNexusFileDir()));					
				}
				if (fileChooser.showDialog(null, "Open") == JFileChooser.APPROVE_OPTION) {
					fileNameStr = fileChooser.getSelectedFile()
							.getAbsolutePath();
				}
                            } finally {
                                jFileChooserBugFix.restoreBackground(background);
                            }
                        }
			try {
				// This block is for sending file content
				if(fileNameStr.isEmpty()) {
					throw new IllegalActionException(this, "Must specify input file name.");
				}
				cipresFile cf = new cipresFile(fileNameStr);
				StringToken fileContentToken = new StringToken(cf.getContent());
				outputFileContent.send(0, fileContentToken);

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return false;
	}
}