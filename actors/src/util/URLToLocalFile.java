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

package util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.Source;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// URLToLocalFile
/**
 * This actor is a modification of the BinaryFileReader. It is designed to read
 * a URL and copy it to the local file system. (It can also be used to read a
 * local file and then write it to another location.)
 * 
 * @author Dan Higgins
 * @version $Id: URLToLocalFile.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class URLToLocalFile extends Source {

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
	public URLToLocalFile(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		output.setTypeEquals(BaseType.BOOLEAN);

		// endOfFile = new TypedIOPort(this, "endOfFile", false, true);
		// endOfFile.setTypeEquals(BaseType.BOOLEAN);

		fileOrURL = new FileParameter(this, "fileOrURL");
		outputFile = new FileParameter(this, "outputFile");

		fileOrURLPort = new TypedIOPort(this, "fileOrURLPort", true, false);
		fileOrURLPort.setTypeEquals(BaseType.STRING);

		outputFilePort = new TypedIOPort(this, "outputFilePort", true, false);
		outputFilePort.setTypeEquals(BaseType.STRING);

		overwrite = new Parameter(this, "overwrite");
		overwrite.setTypeEquals(BaseType.BOOLEAN);
		overwrite.setToken(BooleanToken.TRUE);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"30\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"3\" y=\"20\" "
				+ "style=\"font-size:12; fill:blue; font-family:SansSerif\">"
				+ "URL2File</text>\n" + "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * An output port that produces <i>false</i> until the end of file is
	 * reached, at which point it produces <i>true</i>. The type is boolean.
	 */
	public TypedIOPort endOfFile;

	/**
	 * The file name or URL from which to read. This is a string with any form
	 * accepted by FileParameter.
	 * 
	 * @see FileParameter
	 */
	public FileParameter fileOrURL;

	/**
	 * An input for optionally providing an input file name.
	 * 
	 * @see FileParameter
	 */
	public TypedIOPort fileOrURLPort;

	/**
	 * The file name to which to write. This is a string with any form accepted
	 * by FileParameter.
	 * 
	 * @see FileParameter
	 */
	public FileParameter outputFile;

	/**
	 * An output for optionally providing an output file name.
	 * 
	 * @see FileParameter
	 */
	public TypedIOPort outputFilePort;

	public Parameter overwrite;

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
		URLToLocalFile newObject = (URLToLocalFile) super.clone(workspace);
		newObject._reachedEOF = false;
		newObject._reader = null;
		return newObject;
	}

	/**
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		boolean overwriteValue = ((BooleanToken) overwrite.getToken())
				.booleanValue();

		if (_reachedEOF == false) {
			_openAndReadBytes(overwriteValue);
		}
		output.send(0, new BooleanToken(true));
		_reachedEOF = false;
	}

	/**
	 * @exception IllegalActionException
	 *                If the file or URL cannot be opened or read.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_reader = null;
		_writer = null;
		// _openAndReadFirstBytes();
	}

	/**
	 * Close the reader if there is one.
	 * 
	 * @exception IllegalActionException
	 *                If an IO error occurs.
	 */
	public void wrapup() throws IllegalActionException {
		fileOrURL.close();
		outputFile.close();
		_reader = null;
		_writer = null;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	/** The current reader for the input file. */
	protected InputStream _reader;

	/** The current writer for the output file. */
	protected FileOutputStream _writer;

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Open the file and read the first bytes.
	 */
	private void _openAndReadBytes(boolean overwrite)
			throws IllegalActionException {
		if (fileOrURLPort.getWidth() > 0) {
			if (fileOrURLPort.hasToken(0)) {
				String name = ((StringToken) fileOrURLPort.get(0))
						.stringValue();
				fileOrURL.setExpression(name);
			}
		}
		if (outputFilePort.getWidth() > 0) {
			if (outputFilePort.hasToken(0)) {
				String name = ((StringToken) outputFilePort.get(0))
						.stringValue();
				outputFile.setExpression(name);
			}
		}
		String name = fileOrURL.stringValue();
		String outname = outputFile.stringValue();
		// file or url.
		URL url = fileOrURL.asURL();
		File file = outputFile.asFile();
		if (file.exists() && !overwrite)
			return;
		if (url == null) {
			throw new IllegalActionException(this,
					"No input url/file name has been specified.");
		}

		// jan2706: fix the "http:/www..." problem:
		try {
			String fixedUrlAsString = url.toString().replaceFirst(
					"(https?:)//?", "$1//");
			url = new URL(fixedUrlAsString);
		} catch (Exception e) {
			System.out.println("Badly formed url exception: " + e);
		}

		// System.out.println( "URLToLocalFile2: url.toString()="+url.toString()
		// );
		try {
			_reader = url.openStream();
			_writer = new FileOutputStream(file);
		} catch (IOException ex) {
			throw new IllegalActionException(this, ex,
					"Cannot open file or URL");
		}
		BufferedInputStream _breader = new BufferedInputStream(_reader);
		BufferedOutputStream _bwriter = new BufferedOutputStream(_writer);
		_reachedEOF = false;
		try {
			int c;
			while ((c = _breader.read()) != -1) {
				_bwriter.write(c);
			}
			_bwriter.flush();
			_writer.close();
			_reader.close();
			_reachedEOF = true;
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

}