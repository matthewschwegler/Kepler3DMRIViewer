/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

package org.skidlkit;

import java.io.BufferedReader;
import java.io.FileReader;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.gui.MessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// TextFileReader
/**
 * This actor reads a text file and outputs an array of the evaluations of all
 * lines read.
 * 
 * @author Longjiang Ding
 * @version $Id: TextFileReader.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class TextFileReader extends TypedAtomicActor {
	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public TextFileReader(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		input = new TypedIOPort(this, "input", true, false);
		input.setTypeEquals(BaseType.STRING);
		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);
		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
				+ "style=\"fill:white\"/>\n"
				+ "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
				+ " 15,-10 15,10, -15,10\" " + "style=\"fill:orange\"/>\n"
				+ "</svg>\n");
	} // end of constructor

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////
	/**
	 * The local input & output file names
	 */
	public TypedIOPort input;
	public TypedIOPort output;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	/**
	 * Read the input file name string if it is other than null ...
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		StringToken inToken = null;
		if (input.getWidth() != 0) { // get input from input port
			try {
				if (input.hasToken(0)) {
					// If the inputToken is null set reFire to false.
					try {
						inToken = (StringToken) input.get(0);
					} catch (Exception ex) {
					}
					if (inToken != null) {
						// set up command
						BufferedReader br = new BufferedReader(new FileReader(
								inToken.stringValue()));
						StringBuffer buf = new StringBuffer();
						String currentline = br.readLine();
						while (currentline != null) {
							buf.append(currentline + "\n");
							currentline = br.readLine();
						}
						output.send(0, new StringToken(buf.toString()));
					} else {
						MessageHandler.error("NoTokenException");
					}
				}
			} catch (Exception e) {
				MessageHandler.error(
						"Error opening/updating one of the input parameters: ",
						e);
			}
		} else { // otherwise use FileAttribute filename's value
			System.out.println("File : " + inToken.stringValue()
					+ " not exist!");
		}
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return false;
	}
}