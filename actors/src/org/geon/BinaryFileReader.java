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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.Source;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// BinaryFileReader
/**
 * This actor reads a file or URL and outputs its content as a bytes array. At
 * each iteration a chunk of bytes is read from the file and outputed as a bytes
 * array.
 * 
 * @UserLevelDocumentation This actor reads binary files and streams its content
 *                         as a bytes array.
 * @author Efrat Jaeger
 * @version $Id: BinaryFileReader.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class BinaryFileReader extends Source {

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
	public BinaryFileReader(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		output.setTypeEquals(new ArrayType(BaseType.UNSIGNED_BYTE));

		endOfFile = new TypedIOPort(this, "endOfFile", false, true);
		endOfFile.setTypeEquals(BaseType.BOOLEAN);

		fileOrURL = new FileParameter(this, "fileOrURL");

		fileOrURLPort = new TypedIOPort(this, "fileOrURLPort", true, false);
		fileOrURLPort.setTypeEquals(BaseType.STRING);

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
	 * An output port that produces false until the end of file is reached, at
	 * which point it produces true. The type is boolean.
	 * 
	 * @UserLevelDocumentation An output port that produces false until the end
	 *                         of file is reached, at which point it produces
	 *                         true.
	 */
	public TypedIOPort endOfFile;

	/**
	 * The file name or URL from which to read. This is a string with any form
	 * accepted by FileParameter.
	 * 
	 * @UserLevelDocumentation The path to the file to be read.
	 * @see FileParameter
	 */
	public FileParameter fileOrURL;

	/**
	 * An input for optionally providing a file name.
	 * 
	 * @UserLevelDocumentation An input port for passing a file path sent from a
	 *                         previous step in the workflow.
	 * @see FileParameter
	 */
	public TypedIOPort fileOrURLPort;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Clone the actor into the specified workspace.
	 * 
	 * @return A new actor.
	 * @exception CloneNotSupportedException
	 *                If a derived class contains an attribute that cannot be
	 *                cloned.
	 */
	public Object clone(Workspace workspace) throws CloneNotSupportedException {
		BinaryFileReader newObject = (BinaryFileReader) super.clone(workspace);
		newObject.nBytesRead = 0;
		newObject.bytesRead = null;
		newObject._reachedEOF = false;
		newObject._reader = null;
		return newObject;
	}

	/**
	 * Output the data read initially in the prefire() than in each invocation
	 * of postfire(), if there is any.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		if (nBytesRead > 0) {
			Token _bytes[] = new Token[nBytesRead];
			for (int i = 0; i < nBytesRead; i++) {
				_bytes[i] = new UnsignedByteToken(bytesRead[i]);
			}
			output.send(0, new ArrayToken(_bytes));
		}
	}

	/**
	 * If this is called after prefire() has been called but before wrapup() has
	 * been called, then close any open file.
	 * 
	 * @exception IllegalActionException
	 *                If the file or URL cannot be opened or read.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_reader = null;
		fileOrURL.close();
	}

	/**
	 * Read the next bytes from the file. If there reached EOF, return false.
	 * Otherwise, return whatever the superclass returns.
	 * 
	 * @exception IllegalActionException
	 *                If there is a problem reading the file.
	 */
	public boolean postfire() throws IllegalActionException {
		if (_reader == null) {
			return false;
		}
		try {
			nBytesRead = _reader.read(bytesRead);
			if (nBytesRead <= 0) {
				// In case the return value gets ignored by the domain:
				_reachedEOF = true;
				_newFile = true;
				endOfFile.broadcast(BooleanToken.TRUE);
				String fileNameStr = ((StringToken) fileOrURL.getToken())
						.stringValue();
				// If the fileName is read from a parameter and hasn't changed -
				// meaning there is no next file to read.
				if (fileOrURLPort.getWidth() == 0
						&& fileNameStr.equals(_previousFileOrURL)) {
					return false;
				}
			} else {
				endOfFile.broadcast(BooleanToken.FALSE);
			}
			return super.postfire();
		} catch (IOException ex) {
			throw new IllegalActionException(this, ex, "Postfire failed");
		}
	}

	/**
	 * If this method is called after wrapup() has been called, then open the
	 * file, and read the first chunk of bytes Return false if there is no more
	 * data available in the file. Otherwise, return whatever the superclass
	 * returns.
	 * 
	 * @exception IllegalActionException
	 *                If the superclass throws it.
	 */
	public boolean prefire() throws IllegalActionException {
		if (_newFile) {
			if (fileOrURLPort.getWidth() > 0) {
				if (fileOrURLPort.hasToken(0)) {
					String name = ((StringToken) fileOrURLPort.get(0))
							.stringValue();
					int lineEndInd = name.indexOf("\n");
					if (lineEndInd != -1) // if the string contains a CR.
						name = name.substring(0, lineEndInd);
					fileOrURL.setExpression(name);
				}
			}
			String fileNameStr = ((StringToken) fileOrURL.getToken())
					.stringValue();
			/*
			 * if (fileNameStr.equals(_previousFileOrURL)) { _reader = null;
			 * return false; } else
			 */
			_previousFileOrURL = fileNameStr;

			_openAndReadFirstBytes();
			_reachedEOF = false;
			_newFile = false;
		}

		// if (_reachedEOF) return false;
		// else
		return super.prefire();
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
		_newFile = true;
		_previousFileOrURL = null;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	/** number of bytes read. */
	protected int nBytesRead;

	/** The current bytes read. */
	protected byte[] bytesRead = new byte[20000];

	/** The current reader for the input file. */
	protected InputStream _reader;

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Open the file and read the first bytes.
	 */
	private void _openAndReadFirstBytes() throws IllegalActionException {
		URL url = fileOrURL.asURL();
		if (url == null) {
			throw new IllegalActionException(this,
					"No file name has been specified.");
		}
		try {
			_reader = url.openStream();
		} catch (IOException ex) {
			throw new IllegalActionException(this, ex,
					"Cannot open file or URL");
		}

		_reachedEOF = false;
		try {
			nBytesRead = _reader.read(bytesRead);
		} catch (IOException ex) {
			throw new IllegalActionException(this, ex, "Preinitialize failed.");
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Previous value of fileOrURL parameter. */
	private String _previousFileOrURL;

	/** Indicator that we have reached the end of file. */
	private boolean _reachedEOF = false;

	/** Indicator to open a new file in the prefire */
	private boolean _newFile = true;
}