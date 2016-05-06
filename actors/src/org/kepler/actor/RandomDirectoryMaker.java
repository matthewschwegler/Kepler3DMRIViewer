/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-05-28 16:35:43 -0700 (Tue, 28 May 2013) $' 
 * '$Revision: 32092 $'
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

package org.kepler.actor;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// RandomDirectoryMaker

/**
 * This actor creates a new directory. The corresponding path is given out.
 * 
 * @author Jianwu Wang
 * @version $Id: RandomDirectoryMaker.java 32092 2013-05-28 23:35:43Z jianwu $
 */
public class RandomDirectoryMaker extends TypedAtomicActor {

	/**
	 * Construct a RandomDirectoryMaker with the given container and name.
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
	public RandomDirectoryMaker(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setTypeEquals(BaseType.UNKNOWN);
		trigger.setMultiport(true);

		path = new TypedIOPort(this, "path", false, true);
		path.setTypeEquals(BaseType.STRING);

		parentDir = new PortParameter(this, "parent directory name");
		parentDir.setStringMode(true);
		parentDir.getPort().setTypeEquals(BaseType.STRING);

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
	public PortParameter parentDir = null;

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
		parentDir.update();
		_dirName = ((StringToken)parentDir.getToken()).stringValue();
		Random ran = new Random();
		if (_dirName.length() > 0) {
			_dir = new File(_dirName);
			if (_dir.exists() && !_dir.isDirectory()) {
				throw new IllegalActionException(this, _dir
						+ " exists and is not a directory.");
			}
			if(_dir.exists() && !_dir.canWrite()) {
				throw new IllegalActionException(this, _dir
						+ " exists but doesn't have write perssion.");
			}
			else
			{
				synchronized (this) {
					_mkdirsSuccess = false;
					do
					{
						randomInt = ran.nextInt();
						if (randomInt < 0)
							randomInt = 0 - randomInt;
						_childDir = new File(_dir, new Integer (randomInt).toString());
						if (!_childDir.exists())
							_mkdirsSuccess = _childDir.mkdirs();
					}	
					while (!_mkdirsSuccess);
				}
			}
			try {
				_childDirName = _childDir.getCanonicalPath();
			} catch (IOException ex) {
				_debug("Cannot get directory path.");
				throw new IllegalActionException(this, "Cannot get directory path of " + _childDir);
			}
		}
		path.send(0, new StringToken(_childDirName));
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private File _dir;
	private File _childDir;
	private boolean _mkdirsSuccess;
	private String _dirName;
	private String _childDirName;
	private int randomInt;
}