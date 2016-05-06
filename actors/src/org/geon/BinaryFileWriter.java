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
import java.io.FileOutputStream;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.Sink;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;
import ptolemy.util.MessageHandler;

//////////////////////////////////////////////////////////////////////////
//// BinaryFileWriter
/**
 * This actor receives an array of bytes as an input and writes it to a file. At
 * each iteration a bytes array is read from the input port and written to a
 * specified file.
 * 
 * @UserLevelDocumentation This actor writes streams of bytes array to a file
 *                         specified by filename and outputs the filePath.
 * @author Efrat Jaeger
 * @version $Id: BinaryFileWriter.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class BinaryFileWriter extends Sink {

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
	public BinaryFileWriter(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		input.setTypeEquals(new ArrayType(BaseType.UNSIGNED_BYTE));

		filePath = new TypedIOPort(this, "filePath", false, true);
		filePath.setTypeEquals(BaseType.STRING);

		fileName = new FileParameter(this, "fileName");
		fileName.setExpression("System.out");

		append = new Parameter(this, "append");
		append.setTypeEquals(BaseType.BOOLEAN);
		append.setToken(BooleanToken.FALSE);

		confirmOverwrite = new Parameter(this, "confirmOverwrite");
		confirmOverwrite.setTypeEquals(BaseType.BOOLEAN);
		confirmOverwrite.setToken(BooleanToken.TRUE);

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
	 * output the file URL for display purposes.
	 * 
	 * @UserLevelDocumentation Outputs the path to the written file.
	 */
	public TypedIOPort filePath;

	/**
	 * If true, then append to the specified file. If false (the default), then
	 * overwrite any preexisting file after asking the user for permission.
	 * 
	 * @UserLevelDocumentation If true, then append to the specified file. If
	 *                         false (the default), then overwrite any
	 *                         preexisting file after asking the user for
	 *                         permission.
	 */
	public Parameter append;

	/**
	 * The file name to which to write. This is a string with any form accepted
	 * by FileParameter. The default value is "System.out".
	 * 
	 * @see FileParameter
	 * @UserLevelDocumentation Path to the file to be written.
	 */
	public FileParameter fileName;

	/**
	 * If false, then overwrite the specified file if it exists without asking.
	 * If true (the default), then if the file exists, ask for confirmation
	 * before overwriting.
	 * 
	 * @UserLevelDocumentation If false, then overwrite the specified file if it
	 *                         exists without asking. If true (the default),
	 *                         then if the file exists, ask for confirmation
	 *                         before overwriting.
	 */
	public Parameter confirmOverwrite;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * If the specified attribute is <i>fileName</i> and there is an open file
	 * being written, then close that file. The new file will be opened or
	 * created when it is next written to.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException
	 *                If the specified attribute is <i>fileName</i> and the
	 *                previously opened file cannot be closed.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == fileName) {
			// Do not close the file if it is the same file.
			String newFileName = ((StringToken) fileName.getToken())
					.stringValue();
			// calling getToken forces an evaluation of the expression
			// System.out.println("newFileName:"+newFileName);
			if (_previousFileName != null
					&& !newFileName.equals(_previousFileName)) {
				_previousFileName = newFileName;
				fileName.close();
				_writer = null;
			}
		} else {
			super.attributeChanged(attribute);
		}
	}

	/**
	 * Clone the actor into the specified workspace.
	 * 
	 * @return A new actor.
	 * @exception CloneNotSupportedException
	 *                If a derived class contains an attribute that cannot be
	 *                cloned.
	 */
	public Object clone(Workspace workspace) throws CloneNotSupportedException {
		BinaryFileWriter newObject = (BinaryFileWriter) super.clone(workspace);
		newObject._writer = null;
		return newObject;
	}

	/**
	 * Writes the input content to the file. At the first iteration opens the
	 * file for writing.
	 * 
	 */
	public boolean postfire() throws IllegalActionException {
		if (input.hasToken(0)) {
			Token token = input.get(0);
			if (_writer == null) {
				// Open the file.
				File file = fileName.asFile();
				boolean appendValue = ((BooleanToken) append.getToken())
						.booleanValue();
				boolean confirmOverwriteValue = ((BooleanToken) confirmOverwrite
						.getToken()).booleanValue();
				// Don't ask for confirmation in append mode, since there
				// will be no loss of data.
				if (file.exists() && !appendValue && confirmOverwriteValue) {
					// Query for overwrite.
					if (!MessageHandler.yesNoQuestion("OK to overwrite " + file
							+ "?")) {
						throw new IllegalActionException(this,
								"Please select another file name.");
					}
				}
				try {
					_writer = new FileOutputStream(file);
				} catch (Exception ex) {
					System.out.println("Error opening stream");
				}
			}
			_writeToken(token);
			// String _fileName =
			// java.net.URLDecoder.decode(fileName.getExpression());
			// above line replaced so that expressions used in fileName will be
			// evaluated
			filePath.broadcast(new StringToken(((StringToken) fileName
					.getToken()).stringValue()));
		}
		return super.postfire();
	}

	/**
	 * Close the writer if there is one.
	 * 
	 * @exception IllegalActionException
	 *                If an IO error occurs.
	 */
	public void wrapup() throws IllegalActionException {
		try {
			_writer.close();
		} catch (Exception ex) {
			System.out.println("Error closing stream");
		}

		_writer = null;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	private void _writeToken(Token token) {
		ArrayToken dataArrayToken = (ArrayToken) token;
		byte[] dataBytes = new byte[dataArrayToken.length()];
		for (int j = 0; j < dataArrayToken.length(); j++) {
			UnsignedByteToken dataToken = (UnsignedByteToken) dataArrayToken
					.getElement(j);
			dataBytes[j] = (byte) dataToken.byteValue();
		}
		try {
			_writer.write(dataBytes);
		} catch (Exception ex) {
			System.out.println("Error writing to stream");
		}

	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	/** The current writer. */
	private FileOutputStream _writer;

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Previous value of fileName parameter. */
	private String _previousFileName;

}