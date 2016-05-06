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

import java.io.File;
import java.io.FileWriter;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// TextFileWriter

/**
 * <p>
 * This actor reads a string-valued input token and writes it to the
 * corresponding file without any extra line breaks. It does not include any
 * enclosing quotation marks in the output. The path and name of the file are
 * given via an input port. The user can decide if the text is appended to the
 * file if it exists, if an existing file is overwritten, or left as it is.
 * </p>
 * <p>
 * This actor is based on the Ptolemy II LineWriter actor.
 * </p>
 * 
 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: TextFileWriter.java 24234 2010-05-06 05:21:26Z welker $
 */
public class TextFileWriter extends TypedAtomicActor {

	/**
	 * Construct a TextFileWriter with the given container and name.
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
	public TextFileWriter(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		string = new TypedIOPort(this, "string", true, false);
		string.setTypeEquals(BaseType.STRING);

		fileToWrite = new TypedIOPort(this, "fileToWrite", true, false);
		fileToWrite.setTypeEquals(BaseType.STRING);

		fileWritten = new TypedIOPort(this, "fileWritten", false, true);
		fileWritten.setTypeEquals(BaseType.STRING);

		change = new StringParameter(this, "Change existing");
		change.setTypeEquals(BaseType.STRING);
		change.addChoice("No");
		change.addChoice("Append");
		change.addChoice("Overwrite");
		change.setToken(new StringToken("No"));

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
	 * The first input port, which contains the text to be written.
	 */
	public TypedIOPort string = null;
	/**
	 * The second input port, which contains the file path and name to which to
	 * write.
	 */
	public TypedIOPort fileToWrite = null;
	/**
	 * The output port, which contains the name and path of the written file.
	 */
	public TypedIOPort fileWritten = null;
	/**
	 * The parameter, which specifies what should happen to existing files.
	 */
	public StringParameter change = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Read an input string and write it to the corresponding file.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if (string.hasToken(0) && fileToWrite.hasToken(0)) {
			_text = ((StringToken) string.get(0)).stringValue();
			_path = ((StringToken) fileToWrite.get(0)).stringValue();
			_changeValue = change.stringValue();
			_handle = new File(_path);
			_needNew = !_handle.exists();
			if (_changeValue.equalsIgnoreCase("Append")) {
				_doChange = true;
				_append = true;
			} else if (_changeValue.equalsIgnoreCase("Overwrite")) {
				_doChange = true;
				_append = false;
			} else {
				_doChange = _needNew;
				_append = false;
			}
			_writer = null;
			if (_doChange) {
				if (_needNew) {
					try {
						_parentDir = _handle.getParentFile();
						if (!_parentDir.exists()) {
							_mkdirsSuccess = _parentDir.mkdirs();
							if (!_mkdirsSuccess) {
								throw new IllegalActionException(this,
										"Parent directory " + _parentDir
												+ " was not successfully made.");
							}
						}
						_handle.createNewFile();
					} catch (Exception ex) {
						_debug("File cannot be created.");
					}
				}
				try {
					_writer = new FileWriter(_handle, _append);
					_writer.write(_text);
					_writer.close();
				} catch (Exception ex) {
					_debug("File cannot be written.");
				}
				try {
					_changedFile = _handle.getCanonicalPath();
				} catch (Exception ex) {
					_debug("Path cannot be determined.");
				}
				fileWritten.send(0, new StringToken(_changedFile));
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private String _path;
	private String _text;
	private String _changeValue;
	private File _handle;
	private boolean _needNew;
	private boolean _doChange;
	private boolean _append;
	private File _parentDir;
	private boolean _mkdirsSuccess;
	private FileWriter _writer;
	private String _changedFile;
}