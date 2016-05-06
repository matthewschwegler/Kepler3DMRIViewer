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
import java.io.FilenameFilter;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// FileArrayPrinter

/**
 * This actor reads a directory and writes a string array of the non-hidden,
 * readable files in it, including their path names. The files can be filtered
 * by their extension.
 * 
 * @author Wibke Sudholt, University and ETH Zurich, October 2004
 * @version $Id: FileArrayPrinter.java 24234 2010-05-06 05:21:26Z welker $
 */
public class FileArrayPrinter extends TypedAtomicActor {

	/**
	 * Construct a FileArrayPrinter with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public FileArrayPrinter(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setTypeEquals(BaseType.UNKNOWN);
		trigger.setMultiport(true);

		files = new TypedIOPort(this, "files", false, true);
		files.setTypeEquals(new ArrayType(BaseType.STRING));

		directory = new PortParameter(this, "directory");
		directory.setStringMode(true);

		filter = new StringParameter(this, "File extension filter");

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
	 * The input port, which is a trigger.
	 */
	public TypedIOPort trigger = null;
	/**
	 * The output port, which is an array with the file paths and names.
	 */
	public TypedIOPort files = null;
	/**
	 * The port or parameter, which is a string with the directory name.
	 */
	public PortParameter directory = null;
	/**
	 * The parameter, which is a string with the file extension filter.
	 */
	public StringParameter filter = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Take the directory and print out the file array.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		// Consume the trigger tokens.
		for (int i = 0; i < trigger.getWidth(); i++) {
			if (trigger.hasToken(i)) {
				trigger.get(i);
			}
		}
		// Get the directory name.
		directory.update();
		_content = ((StringToken) directory.getToken()).stringValue();
		// Get the filter and put the files into an array.
		_dirPath = new File(_content);
		if ((_dirPath.isDirectory()) && (_dirPath.canRead())) {
			_selection = filter.stringValue();
			_filePaths = _dirPath.listFiles(new _ExtFilter(_selection));
			_oldSize = _filePaths.length;
			_newSize = 0;
			for (int i = 0; i < _oldSize; i++) {
				if ((_filePaths[i].isFile()) && (_filePaths[i].canRead())
						&& (!_filePaths[i].isHidden())) {
					_newSize++;
				}
			}
			_fileArray = new StringToken[_newSize];
			int j = 0;
			for (int i = 0; i < _oldSize; i++) {
				if ((_filePaths[i].isFile()) && (_filePaths[i].canRead())
						&& (!_filePaths[i].isHidden())) {
					try {
						_fileArray[j] = new StringToken(_filePaths[i]
								.getCanonicalPath());
					} catch (Exception ex) {
						_debug("Cannot get the file path.");
					}
					j++;
				}
			}
			files.send(0, new ArrayToken(_fileArray));
		}
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return false;
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Define the file filter by extension.
	 */
	private class _ExtFilter implements FilenameFilter {
		private String _extension = null;

		private _ExtFilter(String _extension) {
			this._extension = _extension;
		}

		public boolean accept(File dir, String name) {
			if (_extension.length() == 0) {
				return true;
			} else {
				return name.endsWith("." + _extension);
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private String _content;
	private File _dirPath;
	private String _selection;
	private File[] _filePaths;
	private int _oldSize;
	private int _newSize;
	private StringToken[] _fileArray;
}