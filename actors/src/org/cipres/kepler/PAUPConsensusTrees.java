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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import org.cipres.helpers.GUIRun;
import org.cipres.kepler.registry.ActorInfo;
import org.cipres.kepler.registry.CipresKeplerRegistry;
import org.cipres.kepler.registry.Globals;
import org.cipres.util.file.cipresFile;

import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////PAUPConsensusTrees
/**
 * This actor calls PAUP to find consensus trees for a group of trees.
 * 
 * @author Zhijie Guan, Alex Borchers
 * @version $Id: PAUPConsensusTrees.java 24234 2010-05-06 05:21:26Z welker $
 */

public class PAUPConsensusTrees extends GUIRunCIPRes {

	/**
	 * Construct PAUPConsensusTrees source with the given container and name.
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

	public PAUPConsensusTrees(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// get program information
		CipresKeplerRegistry registry = Globals.getInstance().getRegistry();
		_paupActor = registry.getActor("ConsensusTree_Paup");

		// set program related information
		command.setToken(new StringToken(_paupActor.getAppPathForOS()));
		uiXMLFile.setToken(new StringToken(_paupActor.getGuiXmlFile()));
		outputFile.setToken(new StringToken(registry.getDefaultStdOutDir()
				+ "PAUPConsensusOut.log"));
		errorFile.setToken(new StringToken(registry.getDefaultStdOutDir()
				+ "PAUPConsensusError.log"));
		workingDirectory.setToken(new StringToken(_paupActor
				.getWorkingDirecotry()));
		parameterForOutput.setToken(new StringToken("outfile"));
		paupCmdFile = new Parameter(this, "paupCmdFile", new StringToken(
				registry.getDefaultStdOutDir() + "paup_consensus_cmds.nex"));
		paupCmdFile.setDisplayName("PAUP Command File Name");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The parameter for PAUP command file. This parameter will be set to
	 * String.
	 */
	public Parameter paupCmdFile;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Run PAUP for finding consensus trees.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		if (inputParameterValue.hasToken(0) && inputParameterName.hasToken(0)) {
			try {
				GUIRun grun = new GUIRun("PAUP Consensus Trees");

				// if (command.hasToken(0)) {
				grun.setCommand(((StringToken) command.getToken())
						.stringValue());
				// }

				// if (uiXMLFile.hasToken(0)) {
				grun.setUIXMLFile(((StringToken) uiXMLFile.getToken())
						.stringValue());
				// }

				// if (outputFile.hasToken(0)) {
				grun.setOutputFileName(((StringToken) outputFile.getToken())
						.stringValue());
				// }

				// if (errorFile.hasToken(0)) {
				grun.setErrorFileName(((StringToken) errorFile.getToken())
						.stringValue());
				// }

				// if (workingDirectory.hasToken(0)) {
				grun.setWorkingDirectory(((StringToken) workingDirectory
						.getToken()).stringValue());
				// }

				grun.setArgumentValue(((StringToken) inputParameterName.get(0))
						.stringValue(), ((StringToken) inputParameterValue
						.get(0)).stringValue());

				grun.setArgumentsWithGUIGen();

				// if path to output file is > 64 chars paup will throw an error
				// so, we have give paup only the filename, which causes it to
				// write to
				// its own dir, then rename the file to the location specified
				// by the
				// outFilePath
				// Here is how we handle the file rename issue
				// outfile is the file that user assigned for PAUP output
				// infile is the file that user assigned for PAUP input
				File outfile = new File(grun.getArgumentValue("outfile"));
				File infile = new File(grun.getArgumentValue("infile"));
				// paupApp indicates where PAUP is
				File paupApp = new File(_paupActor.getAppPathForOS());
				// paupOut is the real output that is generated by PAUP.
				// It consists of the directory PAUP is in,
				// plus the dir seperator, plus the outfile name
				File paupOut = new File(paupApp.getParent()
						+ Globals.getInstance().getDirSep() + outfile.getName());
				// paupIn is the real input that is fed to PAUP.
				// It consists of the directory PAUP is in,
				// plus the dir seperator, plus the infile name
				File paupIn = new File(paupApp.getParent()
						+ Globals.getInstance().getDirSep() + infile.getName());

				// Now we are going to copy the input file to its new location
				// in PAUP directory
				FileChannel in = null, out = null;
				try {
					in = new FileInputStream(infile).getChannel();
					out = new FileOutputStream(paupIn).getChannel();

					in.transferTo(0, in.size(), out); // copy from infile to
														// paupIn
				} catch (Exception e) {
					System.out
							.println("Error(s) reported during file copy from "
									+ infile.getAbsolutePath() + " to "
									+ paupIn.getAbsolutePath() + ".");
					e.printStackTrace();
				} finally {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				}

				// this command has paup:
				// 1. import input and write to output
				// 2. execute contree and append trees to output
				String commands = "EXECUTE " + paupIn.getName() + ";"
						+ " EXPORT File=" + paupOut.getName()
						+ " FORMAT = NEXUS TREES=NO REPLACE=YES;"
						+ " CLEARTREES NOWARN=YES;" + " SET WARNRESET=NO; "
						+ grun.getArguments() + " TreeFile ="
						+ paupOut.getName() + " APPEND=YES;";

				String paupCmdFileName = ((StringToken) paupCmdFile.getToken())
						.stringValue();
				cipresFile cmdFile = new cipresFile(paupCmdFileName);
				cmdFile.fillFromString("#NEXUS\nBEGIN PAUP;\n" + commands
						+ " QUIT;\nEND;");

				grun.setArguments(paupCmdFileName);

				grun.setWaitForExecution(true);

				grun.execute();

				// now move outfile to its proper location
				if (!outfile.getAbsolutePath().equalsIgnoreCase(
						paupOut.getAbsolutePath())) {
					paupOut.renameTo(outfile);
				}

				exitCode.send(0, new IntToken(grun.getExitCode()));

				standardOutput.send(0,
						new StringToken(grun.getStandardOutput()));
				standardError.send(0, new StringToken(grun.getStandardError()));

				outputParameterValue.send(0, new StringToken(grun
						.getArgumentValue(((StringToken) parameterForOutput
								.getToken()).stringValue())));
			} catch (Exception e) {
				e.printStackTrace();
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

	/**
	 * private variables _paupActor records program information for PAUP.
	 */
	private ActorInfo _paupActor;

}