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

import org.cipres.kepler.registry.ActorInfo;
import org.cipres.kepler.registry.CipresKeplerRegistry;
import org.cipres.kepler.registry.Globals;
import org.cipres.util.file.cipresFile;

import ptolemy.data.StringToken;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////ClustalWSeqAlign
/**
 * This actor calls ClustalW for Sequence Alignment. The Cipres-Kepler registry
 * is used to provide the application information for ClustalW. After setting
 * all the application related information, such as command name, GUIGen XML
 * file, and standard output/error files, this actor invokes ClustalW and
 * retrieves back the alignment result after the execution.
 * 
 * This actor inherits GUIRunCIPRes actor since basically it is a customized
 * GUIRunCIPRes actor to call external Cipres programs.
 * 
 * @author Zhijie Guan, Alex Borchers
 * @version $Id: ClustalWSeqAlign.java 24234 2010-05-06 05:21:26Z welker $
 */

public class ClustalWSeqAlign extends GUIRunCIPRes {

	/**
	 * Construct ClustalWSeqAlign source with the given container and name.
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
	public ClustalWSeqAlign(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// get the application information from the Cipers-Kepler registry
		CipresKeplerRegistry registry = Globals.getInstance().getRegistry();
		_clustalWActor = registry.getActor("SequenceAlign_Clustal");

		// set the application information into the parameters
		command.setToken(new StringToken(_clustalWActor.getAppPathForOS()));
		uiXMLFile.setToken(new StringToken(_clustalWActor.getGuiXmlFile()));
		outputFile.setToken(new StringToken(registry.getDefaultStdOutDir()
				+ "ClustalWOut.log"));
		errorFile.setToken(new StringToken(registry.getDefaultStdOutDir()
				+ "ClustalWError.log"));
		workingDirectory.setToken(new StringToken(_clustalWActor
				.getWorkingDirecotry()));
		parameterForOutput.setToken(new StringToken("outfile"));
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	// /////////////////////////////////////////////////////////////////
	// // functional variables ////

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Run ClustalW for Sequence Alignment.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
	}

	/**
	 * After the sequence alignment with ClustalW, the output file usually does
	 * not comply with the standard Nexus file format. Thus, a post execution
	 * process is need to fix the file format of the ClustalW output. This
	 * function reads in the ClustalW output file, finds the symbol lists that
	 * does not comply with the Nexus format, and removes the symbol list from
	 * the output file.
	 */
	public void postExecutionProcess(String outputFileName) {
		// Fix the ClustalW output
		File inFile = new File(outputFileName);
		try {
			cipresFile cfIn = new cipresFile(inFile.getAbsolutePath());
			// put the fixed output in a temp file
			cipresFile cfOut = new cipresFile(Globals.getInstance()
					.getTempDir()
					+ "ClustalWTempOutput.tmp");
			String content = cfIn.getContent();
			// find beginnning of symbols statement
			int pos1 = content.toLowerCase().indexOf("symbols");
			// find end of symbols statement (will be the 2nd quotation mark
			// after "symbols")
			int pos2 = content.indexOf("\"", pos1);
			pos2 = content.indexOf("\"", pos2 + 1);
			String symbolsString = content.substring(pos1, pos2);
			cfOut.fillFromString(content.substring(0, pos1)
					+ content.substring(pos1 + symbolsString.length() + 1,
							content.length()));
			// rename the fixed output file to the original ClustalW output file
			cfOut.renameTo(cfIn);
		} catch (Exception ex) {
			System.out
					.println("Exception on fixing ClustalW's output Nexus file: ");
			ex.printStackTrace();
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
	 * private variables _clustalWActor is an application information record
	 * that stores all the ClustalW-related information.
	 */
	private ActorInfo _clustalWActor;

}