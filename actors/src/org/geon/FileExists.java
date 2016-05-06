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

import java.io.File;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// FileExists
/**
 * @see FileParameter
 * @author Efrat Jaeger
 * @version $Id: FileExists.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 4.0.1
 */

public class FileExists extends TypedAtomicActor {

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
	public FileExists(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		filePath = new TypedIOPort(this, "filePath", true, false);
		filePath.setTypeEquals(BaseType.STRING);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.BOOLEAN);

		filePathParam = new FileParameter(this, "filePathParam");
		filePathParam.setDisplayName("filePath");

		new Parameter(filePathParam, "allowDirectories", BooleanToken.TRUE);

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * File path.
	 */
	public TypedIOPort filePath;

	/**
	 * Output.
	 */
	public TypedIOPort output;

	/**
	 * File path name or URL. This is a string with any form accepted by
	 * FileParameter.
	 * 
	 * @see FileParameter
	 */
	public FileParameter filePathParam;

	/**
	 * Verify whether the file exists.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		File _filePath = null;
		String fileName = "";

		// get source file.
		if (filePath.getWidth() > 0) {
			fileName = ((StringToken) filePath.get(0)).stringValue();
			int lineEndInd = fileName.indexOf("\n");
			if (lineEndInd != -1) { // The string contains a CR.
				fileName = fileName.substring(0, lineEndInd);
			}
			filePathParam.setExpression(fileName);
		}
		_filePath = filePathParam.asFile();

		if (_filePath.exists()) {
			output.broadcast(new BooleanToken(true));
		} else {
			output.broadcast(new BooleanToken(false));
		}
	}
}