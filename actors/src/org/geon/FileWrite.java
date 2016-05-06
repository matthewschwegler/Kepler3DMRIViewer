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

package org.geon;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.io.LineWriter;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// FileWriter
/**
 * This actor extends LineWriter by outputing the filename. The actor writes the
 * value of string tokens to a file, and outputs the file path.
 * 
 * The file is specified by the fileName attribute using any form acceptable to
 * FileParameter.
 * 
 * If the append attribute has value true, then the file will be appended to. If
 * it has value false, then if the file exists, the user will be queried for
 * permission to overwrite, and if granted, the file will be overwritten.
 * 
 * If the confirmOverwrite parameter has value false, then this actor will
 * overwrite the specified file if it exists without asking. If true (the
 * default), then if the file exists, then this actor will ask for confirmation
 * before overwriting.
 * 
 * @see FileParameter
 * @version $Id: FileWrite.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 2.2
 */
public class FileWrite extends LineWriter {

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
	public FileWrite(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		url = new TypedIOPort(this, "url", false, true);
		url.setTypeEquals(BaseType.STRING);
		url.setMultiport(false);

		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
				+ "style=\"fill:white\"/>\n"
				+ "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
				+ " 15,-10 15,10, -15,10\" " + "style=\"fill:red\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * Written file URL
	 */
	public TypedIOPort url;

	/**
	 * Calls LineWriter's postfire and broadcasts the fileName.
	 */
	public boolean postfire() throws IllegalActionException {
		boolean ret = super.postfire();
		url.broadcast(new StringToken(fileName.asFile().getAbsolutePath()));
		return ret;
	}

}