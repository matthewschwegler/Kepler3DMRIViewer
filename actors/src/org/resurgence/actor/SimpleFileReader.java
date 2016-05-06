/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
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

package org.resurgence.actor;

import java.io.BufferedReader;
import java.io.FileReader;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// SimpleFileReader

/**
 * <p>
 * This actor reads a file and outputs its contents as a single string.
 * </p>
 * <p>
 * It is based on the Ptolemy II FileReader actor.
 * </p>
 * 
 * @author Wibke Sudholt, University of Zurich, April 2005
 * @version $Id: SimpleFileReader.java 24234 2010-05-06 05:21:26Z welker $
 */
public class SimpleFileReader extends TypedAtomicActor {

	public SimpleFileReader(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		file = new TypedIOPort(this, "file", true, false);
		file.setTypeEquals(BaseType.STRING);

		content = new TypedIOPort(this, "content", false, true);
		content.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
				+ "style=\"fill:white\"/>\n"
				+ "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
				+ " 15,-10 15,10, -15,10\" " + "style=\"fill:red\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // public variables ////

	/**
	 * The input port, which gives the file name from which to read.
	 */
	public TypedIOPort file;
	/**
	 * The output port, which provides the file contents.
	 */
	public TypedIOPort content;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Output the data read from the file as a string.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director or if reading the file triggers an
	 *                exception.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		if (file.hasToken(0)) {
			_path = ((StringToken) file.get(0)).stringValue();
		}

		try {
			BufferedReader reader = new BufferedReader(new FileReader(_path));
			StringBuffer lineBuffer = new StringBuffer();
			String newline = System.getProperty("line.separator");
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				lineBuffer = lineBuffer.append(line);
				lineBuffer = lineBuffer.append(newline);
			}
			content.send(0, new StringToken(lineBuffer.toString()));
		} catch (Exception ex) {
			throw new IllegalActionException(this, ex.getMessage());
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private String _path;
}