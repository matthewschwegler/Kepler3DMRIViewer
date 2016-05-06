/*
 * Copyright (c) 2010 The Regents of the University of California.
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

package edu.sdsc.nbcr.opal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.axis.encoding.Base64;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Implementation of a Kepler actor that contructs an input for the Opal-based
 * Mast service
 * 
 * @author: Sriram Krishnan [mailto:sriram@sdsc.edu]
 */
public class MastOpalInput extends TypedAtomicActor {

	// result string variable
	private String _result = "";
	private boolean done = false;

	// list of parameters
	public FileParameter memeOutFile, sequenceFile;

	// list of ports;
	public TypedIOPort output, trigger;

	public MastOpalInput(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// add parameters
		memeOutFile = new FileParameter(this, "memeOutFile");
		sequenceFile = new FileParameter(this, "sequenceFile");

		// add the trigger
		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setTypeEquals(BaseType.BOOLEAN);

		// add an output port of type String
		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
				+ "style=\"fill:white\"/>\n"
				+ "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
				+ " 15,-10 15,10, -15,10\" " + "style=\"fill:orange\"/>\n"
				+ "</svg>\n");
	}

	// the method that is invoked when this actor is triggered
	public void fire() throws IllegalActionException {
		super.fire();

		// check if action is triggered
		if (trigger.getWidth() == 0)
			return;

		boolean triggerValue = ((BooleanToken) trigger.get(0)).booleanValue();
		if (!triggerValue)
			return;

		StringBuffer buff = new StringBuffer();
		buff
				.append("<launchJobInput xmlns=\"http://nbcr.sdsc.edu/opal/types\">\n");

		// create the list of arguments
		buff.append("   <argList>" + memeOutFile.asFile().getName() + " "
				+ sequenceFile.asFile().getName() + " </argList>\n");

		// create the base64 encoded input files
		try {
			File f = sequenceFile.asFile();
			buff.append("   <inputFile>\n");
			buff.append("      <name>" + f.getName() + "</name>\n");
			buff.append("      <contents>");
			byte[] data = new byte[(int) f.length()];
			FileInputStream fIn = new FileInputStream(f);
			fIn.read(data);
			fIn.close();
			buff.append(Base64.encode(data, 0, data.length));
			buff.append("</contents>\n");
			buff.append("   </inputFile>\n");
		} catch (IOException ioe) {
			throw new IllegalActionException(ioe.getMessage());
		}

		try {
			File f = memeOutFile.asFile();
			buff.append("   <inputFile>\n");
			buff.append("      <name>" + f.getName() + "</name>\n");
			buff.append("      <contents>");
			byte[] data = new byte[(int) f.length()];
			FileInputStream fIn = new FileInputStream(f);
			fIn.read(data);
			fIn.close();
			buff.append(Base64.encode(data, 0, data.length));
			buff.append("</contents>\n");
			buff.append("   </inputFile>\n");
		} catch (IOException ioe) {
			throw new IllegalActionException(ioe.getMessage());
		}

		buff.append("</launchJobInput>\n");

		// copy the xml to the result, and signify done
		_result = buff.toString();
		done = true;

		// create the input for the Opal-based Mast service
		output.send(0, new StringToken(_result));
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		if (done) {
			_result = "";
			return false;
		} else
			return true;
	}
}
