/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

import org.cipres.kepler.registry.ActorInfo;
import org.cipres.kepler.registry.CipresKeplerRegistry;
import org.cipres.kepler.registry.Globals;

import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////AlignmentEditor_Seaview
/**
 * Given an alignment, the AlignmentEditor_Seaview actor displays the alignment
 * in the seaview window and facititates the user to edit the alignment. Note1:
 * since Seaview is an application running as another process with GUI, the
 * actor cannot monitor the execution of Seaview. Thus the actor cannot know
 * when the user finishes the work and closes the Seaview window. Note2: Here we
 * assume the user will save the output file with the same name as the input
 * file. We will change it to let the user input the output file name in the
 * future.
 * 
 * @author Zhijie Guan, Alex Borchers
 * @version $Id: AlignmentEditor_Seaview.java 24234 2010-05-06 05:21:26Z welker $
 */

public class AlignmentEditor_Seaview extends JRunCIPRes {

	/**
	 * Construct a AlignmentEditor_Seaview actor with the given container and
	 * name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */

	public AlignmentEditor_Seaview(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// get the information of Seaview application
		CipresKeplerRegistry registry = Globals.getInstance().getRegistry();
		_seaviewActor = registry.getActor("AlignmentEditor_Seaview");

		// SeaView has different invocation methods in different OSs
		// In Mac, SeaView runs by "open -a Seaview.app input_file_name"
		// In all the other OS, SeaView runs by "Seaview input_file_name"
		if (System.getProperty("os.name").startsWith("Mac")) {
			command.setToken(new StringToken("open -a "
					+ _seaviewActor.getAppPathForOS()));
		} else {
			command.setToken(new StringToken(_seaviewActor.getAppPathForOS()));
		}

		// set the standard output file
		outputFile.setToken(new StringToken(registry.getDefaultStdOutDir()
				+ "SeaviewOut.log"));

		// set the stardard error file
		errorFile.setToken(new StringToken(registry.getDefaultStdOutDir()
				+ "SeaviewError.log"));

		// set the working directory
		workingDirectory.setToken(new StringToken(_seaviewActor
				.getWorkingDirecotry()));

		// initialize the port for getting in the input file name
		inputFileName = new TypedIOPort(this, "inputFileName", true, false);
		inputFileName.setDisplayName("Input File Name");
		inputFileName.setTypeEquals(BaseType.STRING);

		// initialize the port for sending out the output file name
		outputFileName = new TypedIOPort(this, "outputFileName", false, true);
		outputFileName.setDisplayName("Output File Name");
		outputFileName.setTypeEquals(BaseType.STRING);

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The input file name is transferred into the actor through this port. The
	 * input file contains the alignment for reviewing and editing.
	 */
	public TypedIOPort inputFileName;

	/**
	 * The output file name is sent out throught this port. The output file
	 * contains the edited alignment.
	 */
	public TypedIOPort outputFileName;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Run seaview for alignment reviw and manipulation.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {

		// get the input file name and send it to JRun object as the command
		// argument
		StringToken inputFileNameToken = (StringToken) inputFileName.get(0);
		arguments.setToken(inputFileNameToken);

		super.fire();

		// the output file name must be the same as the input file name
		// currently we don't support the function that an user can save the
		// result to another file
		outputFileName.send(0, inputFileNameToken);
	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	// Note here we want to let this actor be able to run multiple times to view
	// and edit
	// multiple different alignments.
	/*
	 * public boolean postfire() { return false; }
	 */

	/**
	 * private variables This is the ActorInfo object which comes from the
	 * Cipres-kepler registry and records all the appliation information for
	 * Seaview.
	 */
	private ActorInfo _seaviewActor;

}