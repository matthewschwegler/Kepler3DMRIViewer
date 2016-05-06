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

import org.cipres.guigen.ServiceCommandPanel;
import org.cipres.helpers.GUIRun;
import org.cipres.kepler.registry.ActorInfo;
import org.cipres.kepler.registry.ActorIterator;
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
////PAUPInfer
/**
 * This actor calls PAUP for tree inference using parsimony.
 * 
 * @author Zhijie Guan, Alex Borchers
 * @version $Id: PAUPInfer.java 24234 2010-05-06 05:21:26Z welker $
 */

public class PAUPInfer extends GUIRunCIPRes {

	/**
	 * Construct PAUPInfer source with the given container and name.
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

	public PAUPInfer(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// get program information
		CipresKeplerRegistry registry = Globals.getInstance().getRegistry();
		_paupActor = registry.getActor("InferTreeByParsimony_Paup");

		// set program information to parameters
		command.setToken(new StringToken(_paupActor.getAppPathForOS()));
		uiXMLFile.setToken(new StringToken(_paupActor.getGuiXmlFile()));
		outputFile.setToken(new StringToken(registry.getDefaultStdOutDir()
				+ "PAUPInferOut.log"));
		errorFile.setToken(new StringToken(registry.getDefaultStdOutDir()
				+ "PAUPInferError.log"));
		workingDirectory.setToken(new StringToken(_paupActor
				.getWorkingDirecotry()));
		parameterForOutput.setToken(new StringToken("outfile"));
		paupCmdFile = new Parameter(this, "paupCmdFile", new StringToken(
				registry.getDefaultStdOutDir() + "paup_infer_cmds.nex"));
		paupCmdFile.setDisplayName("PAUP Command File Name");

		// iterations information
		iterations = new Parameter(this, "iterations", new IntToken(1));
		iterations.setDisplayName("Iteration Times");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The parameter for PAUP command file. This parameter will be set to
	 * String.
	 */
	public Parameter paupCmdFile;

	/**
	 * The iteration times of the program execution is set in this parameter.
	 */
	public Parameter iterations;

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Run PAUP for tree inference.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {

		ActorIterator actorIterator = null;
		if (inputParameterValue.hasToken(0) && inputParameterName.hasToken(0)) {
			try {
				GUIRun grun = new GUIRun("PAUP Tree Inference");

				// get parameter values
				String commandFileName = ((StringToken) command.getToken())
						.stringValue();
				if (commandFileName.length() != 0) {
					grun.setCommand(commandFileName);
				} else {
					System.out.println("Command is not defined!");
					throw new IllegalActionException("Command is not defined");
				}

				String outFileName = ((StringToken) outputFile.getToken())
						.stringValue();
				if (outFileName.length() != 0) {
					grun.setOutputFileName(outFileName);
				}

				String errorFileName = ((StringToken) errorFile.getToken())
						.stringValue();
				if (errorFileName.length() != 0) {
					grun.setErrorFileName(errorFileName);
				}

				String workingDirName = ((StringToken) workingDirectory
						.getToken()).stringValue();
				if (workingDirName.length() != 0) {
					grun.setWorkingDirectory(workingDirName);
				}

				String uiXMLFileName = ((StringToken) uiXMLFile.getToken())
						.stringValue();
				if (uiXMLFileName.length() != 0) {
					actorIterator = new ActorIterator(new File(uiXMLFileName),
							((IntToken) iterations.getToken()).intValue());
				}

				String outputParameterName = ((StringToken) parameterForOutput
						.getToken()).stringValue();
				if (outputParameterName.length() == 0) {
					outputParameterName = "outfile";
				}

				String paupCmdFileName = ((StringToken) paupCmdFile.getToken())
						.stringValue();
				if (paupCmdFileName.length() == 0) {
					paupCmdFileName = registry.getDefaultStdOutDir()
							+ "paup_infer_cmds.nex";
				}

				// set the GUIGen interface for iterative execution
				actorIterator
						.setParameterValueInAll(
								((StringToken) inputParameterName.get(0))
										.stringValue(),
								((StringToken) inputParameterValue.get(0))
										.stringValue());

				// collect the execution configuration for this program
				actorIterator.setOutfileParamInAll(_paupActor.actorName);
				actorIterator.show();
				ServiceCommandPanel[] pnls = actorIterator
						.getServiceCommandPanels();

				if (pnls != null) { // null returned if user clicks 'Cancel'
					// run paup_infer on each set of commands and send to
					// forester to viz tree(s)
					for (int i = 0; i < pnls.length; i++) {
						// outfile is the file that user assigned for PAUP
						// output
						File outfile = new File(pnls[i]
								.getParameterValue(outputParameterName));
						// paupApp indicates where PAUP is
						File paupApp = new File(_paupActor.getAppPathForOS());
						// paupOut is the real output that is generated by PAUP.
						// It consists of the directory PAUP is in,
						// plus the dir seperator, plus the outfile name
						File paupOut = new File(paupApp.getParent()
								+ Globals.getInstance().getDirSep()
								+ outfile.getName());

						String commands = pnls[i].getCmdBlock()
								+ "\n EXPORT File=" + paupOut.getName()
								+ " FORMAT=NEXUS REPLACE=YES;\n"
								+ " SAVETREES File=" + paupOut.getName()
								+ " FORMAT=NEXUS BRLENS=YES MAXDECIMALS=2"
								+ " APPEND = YES;";

						cipresFile cmdFile = new cipresFile(paupCmdFileName);
						cmdFile.fillFromString("#NEXUS\nBEGIN PAUP;\n"
								+ commands + " QUIT;\nEND;");

						grun.setArguments(paupCmdFileName);

						grun.setWaitForExecution(true);

						grun.execute();

						// now move outfile to its proper location
						if (!outfile.getAbsolutePath().equalsIgnoreCase(
								paupOut.getAbsolutePath())) {
							paupOut.renameTo(outfile);
						}

						exitCode.send(0, new IntToken(grun.getExitCode()));

						standardOutput.send(0, new StringToken(grun
								.getStandardOutput()));
						standardError.send(0, new StringToken(grun
								.getStandardError()));

						outputParameterValue.send(0, new StringToken(outfile
								.getAbsolutePath()));
					}
				}

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
	 * The instance of Cipres-Kepler registry
	 */
	private CipresKeplerRegistry registry = Globals.getInstance().getRegistry();

	/**
	 * private variables _paupActor records all the information for program
	 * PAUP.
	 */
	private ActorInfo _paupActor;

}