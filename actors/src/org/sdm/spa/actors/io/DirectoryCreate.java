/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.sdm.spa.actors.io;

import java.io.File;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Creates the directory specified by the input port if it doesn't exist plus
 * parent directories.
 * 
 * If the directory could not be created, perhaps because a file of the same
 * name already exists, then a non-empty error message will be output at the
 * errors port. Otherwise, an empty string will be output in that port after
 * creation. No error will be reported if a directory of this name already
 * exists.
 * 
 * @author xiaowen
 * @version $Id: DirectoryCreate.java 24234 2010-05-06 05:21:26Z welker $
 */
public class DirectoryCreate extends TypedAtomicActor {
	/**
	 * Create an instance of this actor.
	 * 
	 * @param container
	 *            The entity to contain this actor
	 * @param name
	 *            Name of the actor
	 * @throws IllegalActionException
	 *             If superclass throws it.
	 * @throws NameDuplicationException
	 *             If superclass throws it.
	 */
	public DirectoryCreate(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		// Initialize both ports.
		this.name = new PortParameter(this, "name");
		this.errors = new TypedIOPort(this, "errors", false, true);

		// Set the type constraints.
		this.name.setTypeEquals(BaseType.STRING);
		this.errors.setTypeEquals(BaseType.STRING);
	}

	/**
	 * Create the directory with the name taken from the input port and output
	 * either the error or an empty string along the errors port.
	 * 
	 * @throws IllegalActionException
	 *             If superclass throws it.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		// Get the input.
		name.update();
		String strName = ((StringToken) name.getToken()).stringValue();

		// File object representing the target directory
		File fileDir = new File(strName);

		// If the directory already exists, then stop.
		if (fileDir.isDirectory()) {
			errors.send(0, new StringToken());
			return;
		}

		boolean done = false;

		try {
			done = fileDir.mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
			errors.send(0, new StringToken(
					"Exception thrown while trying to create directory '"
							+ strName + "' in actor '" + this.getFullName()
							+ "': " + e.toString()));
			return;
		}

		if (!done) {
			errors.send(0, new StringToken("Could not create directory '"
					+ strName + "' in actor '" + this.getFullName() + "'."));
		} else {
			errors.send(0, new StringToken());
		}
	}

	/** Input port representing the name of the file to create. */
	protected PortParameter name;

	/** Port that outputs errors if there are any. */
	protected TypedIOPort errors;
}