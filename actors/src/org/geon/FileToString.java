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

import java.io.BufferedReader;
import java.io.IOException;

import ptolemy.actor.lib.Source;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// FileToString
/**
 * This actor reads a file or URL and outputs its content in a single string.
 * The file or URL is specified using any form acceptable to FileParameter.
 * <p>
 * This actor can skip some lines at the beginning of the file or URL, with the
 * number specified by the <i>numberOfLinesToSkip</i> parameter. The default
 * value of this parameter is 0.
 * 
 * @author Efrat Jaeger
 * @version $Id: FileToString.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class FileToString extends Source {

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
	public FileToString(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		output.setTypeEquals(BaseType.STRING);

		fileOrURL = new FileParameter(this, "fileOrURL");

		numberOfLinesToSkip = new Parameter(this, "numberOfLinesToSkip",
				new IntToken(0));
		numberOfLinesToSkip.setTypeEquals(BaseType.INT);

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
	 * The file name or URL from which to read. This is a string with any form
	 * accepted by FileParameter.
	 * 
	 * @see FileParameter
	 */
	public FileParameter fileOrURL;

	/**
	 * The number of lines to skip at the beginning of the file or URL. This
	 * parameter contains an IntToken, initially with value 0. The value of this
	 * parameter must be non-negative.
	 */
	public Parameter numberOfLinesToSkip;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * If the specified attribute is <i>fileOrURL</i> and there is an open file
	 * being read, then close that file and open the new one; if the attribute
	 * is <i>numberOfLinesToSkip</i> and its value is negative, then throw an
	 * exception. In the case of <i>fileOrURL</i>, do nothing if the file name
	 * is the same as the previous value of this attribute.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException
	 *                If the specified attribute is <i>fileOrURL</i> and the
	 *                file cannot be opened, or the previously opened file
	 *                cannot be closed; or if the attribute is
	 *                <i>numberOfLinesToSkip</i> and its value is negative.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == fileOrURL) {
			// NOTE: We do not want to close the file if the file
			// has not in fact changed. We check this by just comparing
			// name, which is not perfect...
			if (_previousFileOrURL != null
					&& !fileOrURL.getExpression().equals(_previousFileOrURL)) {
				_previousFileOrURL = fileOrURL.getExpression();
				fileOrURL.close();
				// Ignore if the fileOrUL is blank.
				if (fileOrURL.getExpression().trim().equals("")) {
					_reader = null;
				} else {
					_reader = fileOrURL.openForReading();
				}
			}
		} else if (attribute == numberOfLinesToSkip) {
			int linesToSkip = ((IntToken) numberOfLinesToSkip.getToken())
					.intValue();
			if (linesToSkip < 0) {
				throw new IllegalActionException(this, "The number of lines "
						+ "to skip cannot be negative.");
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
		FileToString newObject = (FileToString) super.clone(workspace);
		newObject._currentLine = null;
		newObject._reader = null;
		return newObject;
	}

	/**
	 * Output the data lines into a string.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		_openAndReadFirstLine();
		while (_currentLine != null) {
			_result += _currentLine;
			_result += "\n";
			try {
				_currentLine = _reader.readLine();
			} catch (IOException ex) {
				throw new IllegalActionException(this, ex,
						"fire failed reading line");
			}
		}
		output.broadcast(new StringToken(_result));
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		_result = "";
		return false;
	}

	/**
	 * Open the file or URL and read the first line, and use the first line to
	 * set the type of the output.
	 * 
	 * @exception IllegalActionException
	 *                If the file or URL cannot be opened, or if the first line
	 *                cannot be read.
	 */
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();
		// _openAndReadFirstLine();
	}

	/**
	 * Close the reader if there is one.
	 * 
	 * @exception IllegalActionException
	 *                If an IO error occurs.
	 */
	public void wrapup() throws IllegalActionException {
		fileOrURL.close();
		_reader = null;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	/** Cache of most recently read data. */
	protected String _currentLine;

	/** The current reader for the input file. */
	protected BufferedReader _reader;

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Open the file and read the first line.
	 */
	private void _openAndReadFirstLine() throws IllegalActionException {
		_reader = fileOrURL.openForReading();
		try {
			// Read (numberOfLinesToSkip + 1) lines
			int numberOfLines = ((IntToken) numberOfLinesToSkip.getToken())
					.intValue();
			for (int i = 0; i <= numberOfLines; i++) {
				_currentLine = _reader.readLine();
				if (_currentLine == null) {
					throw new IllegalActionException(this, "The file does not "
							+ "have enough lines.");
				}
			}
		} catch (IOException ex) {
			throw new IllegalActionException(this, ex, "Preinitialize failed.");
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Previous value of fileOrURL parameter. */
	private String _previousFileOrURL;

	/** Result string Variable. */
	private String _result = new String("");

}