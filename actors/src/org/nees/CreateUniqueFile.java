/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
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

package org.nees;

import java.io.File;
import java.io.IOException;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// CreateUniqueFile
/**
 * This actor creates a uniquely-named file in given a directory <i>dir</i>.
 * 
 * @author Daniel Crawl
 * @version $Id: CreateUniqueFile.java 24234 2010-05-06 05:21:26Z welker $
 */

public class CreateUniqueFile extends TypedAtomicActor {

	/**
	 * Construct a CreateUniqueFile source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public CreateUniqueFile(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		prefix = new PortParameter(this, "prefix", new StringToken("pre"));
		prefix.setTypeEquals(BaseType.STRING);
		new Attribute(prefix, "_showName");

		suffix = new PortParameter(this, "suffix", new StringToken(".tmp"));
		suffix.setTypeEquals(BaseType.STRING);
		new Attribute(suffix, "_showName");

		dir = new PortParameter(this, "dir", new StringToken(System
				.getProperty("java.io.tmpdir")));
		dir.setTypeEquals(BaseType.STRING);
		new Attribute(dir, "_showName");

		absFilename = new Parameter(this, "Absolute Path", new BooleanToken(
				true));
		absFilename.setTypeEquals(BaseType.BOOLEAN);

		filename = new TypedIOPort(this, "output", false, true);
		filename.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The character prefix to use in generating the file name.
	 */
	public PortParameter prefix = null;

	/**
	 * The character suffix to use in generating the file name.
	 */
	public PortParameter suffix = null;

	/**
	 * The directory in which to create the file.
	 */
	public PortParameter dir = null;

	/**
	 * If true, the output a full path instead of just the file name.
	 */
	public Parameter absFilename = null;

	/**
	 * The name of the created file.
	 */
	public TypedIOPort filename = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Create the file using java.lang.File.createTempFile().
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		StringToken token = null;

		prefix.update();
		String prefixStr = "";
		if ((token = (StringToken) prefix.getToken()) != null) {
			prefixStr = token.stringValue();
		}

		suffix.update();
		String suffixStr = "";
		if ((token = (StringToken) suffix.getToken()) != null) {
			suffixStr = token.stringValue();
		}

		dir.update();
		String dirStr = "";
		if ((token = (StringToken) dir.getToken()) == null) {
			throw new IllegalActionException(this, "Must supply directory.");
		} else {
			dirStr = token.stringValue();
		}

		File dirFile = new File(dirStr);

		try {
			File tempFile = File.createTempFile(prefixStr, suffixStr, dirFile);

			boolean abs = ((BooleanToken) absFilename.getToken())
					.booleanValue();

			String outStr = null;

			// see if we should output the entire path or just
			// the file name.
			if (abs) {
				outStr = tempFile.getAbsolutePath();
			} else {
				outStr = tempFile.getName();
			}

			filename.broadcast(new StringToken(outStr));
		} catch (IOException e) {
			throw new IllegalActionException(this, "IOException: "
					+ e.getMessage());
		}
	}
}