/*
 * Copyright (c) 2001-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: brooks $'
 * '$Date: 2010-12-17 16:53:34 -0800 (Fri, 17 Dec 2010) $' 
 * '$Revision: 26558 $'
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

import java.awt.Image;
import java.io.File;

import javax.swing.ImageIcon;

import ptolemy.actor.lib.Source;
import ptolemy.data.AWTImageToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// KeplerImageReader
/**
 * This actor reads an Image from a FileParameter, and outputs it as an
 * AWTImageToken.
 * 
 * <p>
 * This actor is a minor variation on the Ptolemy ImageReader _image is
 * 'flush()' to avoid the caching behavior that keeps showing the same cached
 * image when a workflow is run several times. (The caching fails to show a
 * 'new' version of a graphics file created by the workflow when 'getImage' is
 * used and a file (but not the name of the file) is changed. Change only
 * appears after JVM is shut down an restarted.) Dan Higgins(DFH) -
 * higgins@nceas.ucsb.edu Sept 27, 2004 The original PT ImageReader used URLs
 * rather than local files. This creates a problem when the image file is
 * dynamically created and does not exist at the start of the workflow
 * execution. Modifications were thus made to check for file existence; if it
 * does not exist, an empty file is created in the attributeChanged method. This
 * change makes the use of remote URLs for the image source invalid - Dan
 * Higgins 2 Oct 2004
 * 
 * @see FileParameter
 * @see AWTImageToken
 * @author Dan Higgins NCEAS UCSB, based on ptolemy.actor.lib.gui.ImageReader by Christopher Hylands
 * @version $Id: KeplerImageReader.java 26558 2010-12-18 00:53:34Z brooks $
 * @since Ptolemy II 3.0
 * @Pt.ProposedRating Red (cxh)
 * @Pt.AcceptedRating Red (cxh)
 */
public class KeplerImageReader extends Source {
	// We don't extend ptolemy.actor.lib.Reader because we are not
	// reading in data by columns. Probably this class and
	// ptolemy.actor.lib.Reader should extend a common base class?

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
	public KeplerImageReader(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		// Set the type of the output port.
		// output.setMultiport(true);
		output.setTypeEquals(BaseType.OBJECT);

		fileOrURL = new FileParameter(this, "fileOrURL");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The file name or URL from which to read. This is a string with any form
	 * accepted by File Attribute.
	 * 
	 * @see FileParameter
	 */
	public FileParameter fileOrURL;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * If the specified attribute is <i>URL</i>, then close the current file (if
	 * there is one) and open the new one.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException
	 *                If the specified attribute is <i>URL</i> and the file
	 *                cannot be opened.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == fileOrURL) {
			// Would it be worth checking to see if the URL exists and
			// is readable?
			_file = fileOrURL.asFile();
			if (_file != null) {
				try {
					if (!_file.getParentFile().exists()) {
						if (!_file.getParentFile().mkdirs()) {
							// If the directory does not exist, create it.  See
							// http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4143
							throw new IllegalActionException(this,
									"Failed to make directory \""
											+ _file.getParentFile() + "\"");
						}
					}
					_file.createNewFile(); // creates a new empty file if one
					// does not exist
				} catch (Exception ex) {
					throw new IllegalActionException(this, ex,
							"Problem creating the output file \"" + _file
									+ "\"");
				}
			}
		}
		super.attributeChanged(attribute);
	}

	/**
	 * Clone the actor into the specified workspace. This calls the base class
	 * and then set the filename public member.
	 * 
	 * @param workspace
	 *            The workspace for the new object.
	 * @return A new actor.
	 * @exception CloneNotSupportedException
	 *                If a derived class contains an attribute that cannot be
	 *                cloned.
	 */
	public Object clone(Workspace workspace) throws CloneNotSupportedException {
		KeplerImageReader newObject = (KeplerImageReader) super
				.clone(workspace);
		// newObject.output.setMultiport(true);
		return newObject;
	}

	/**
	 * Output the data read in the prefire.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		output.broadcast(new AWTImageToken(_image));
	}

	/**
	 * Open the file at the URL, and set the width of the output.
	 */
	public void initialize() throws IllegalActionException {
		attributeChanged(fileOrURL);
	}

	/**
	 * Read in an image.
	 * 
	 * @exception IllegalActionException
	 *                If an IO error occurs.
	 */
	public boolean prefire() throws IllegalActionException {
		if (_file == null) {
			throw new IllegalActionException("sourceFile was null");
		}
		_fileRoot = _file.getPath();
		_image = new ImageIcon(_fileRoot).getImage();
		_image.flush(); // added to get rid of cached data - DFH
		if (_image.getWidth(null) == -1 && _image.getHeight(null) == -1) {
			throw new IllegalActionException(this,
					"Image size is -1 x -1.  Failed to open '" + _fileRoot
							+ "'");
		}
		return super.prefire();
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	// The URL as a string.
	private String _fileRoot;

	// Image that is read in.
	private Image _image;

	// The File
	private File _file;
}