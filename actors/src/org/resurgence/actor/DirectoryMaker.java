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

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// DirectoryMaker

/**
 * This actor creates a new directory. The corresponding path is given out.
 * 
 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: DirectoryMaker.java 24234 2010-05-06 05:21:26Z welker $
 */
public class DirectoryMaker extends TypedAtomicActor {

	/**
	 * Construct a DirectoryMaker with the given container and name.
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
	public DirectoryMaker(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setTypeEquals(BaseType.UNKNOWN);
		trigger.setMultiport(true);

		path = new TypedIOPort(this, "path", false, true);
		path.setTypeEquals(BaseType.STRING);

		directory = new PortParameter(this, "Directory name");
		directory.setStringMode(true);
		directory.getPort().setTypeEquals(BaseType.STRING);

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
	 * The output port, which contains the new directory path.
	 */
	public TypedIOPort path = null;
	/**
	 * The parameter, which is a string with the directory name.
	 */
	public PortParameter directory = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Create the new directory.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director or if directory making does not
	 *                work.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		for (int i = 0; i < trigger.getWidth(); i++) {
			if (trigger.hasToken(i)) {
				trigger.get(i);
			}
		}
		directory.update();
		_dirName = ((StringToken)directory.getToken()).stringValue();
		if (_dirName.length() > 0) {
			_dir = new File(_dirName);
			if (!_dir.exists()) {
				_mkdirsSuccess = _dir.mkdirs();
				if (!_mkdirsSuccess) {
					throw new IllegalActionException(this, "Directory " + _dir
							+ " was not successfully made.");
				}
			} else {
				if (!_dir.isDirectory()) {
					throw new IllegalActionException(this, _dir
							+ " exists and is not a directory.");
				}
			}
			try {
				_dirName = _dir.getCanonicalPath();
			} catch (Exception ex) {
				_debug("Cannot get directory path.");
			}
		}
		path.send(0, new StringToken(_dirName));
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private File _dir;
	private boolean _mkdirsSuccess;
	private String _dirName;
}