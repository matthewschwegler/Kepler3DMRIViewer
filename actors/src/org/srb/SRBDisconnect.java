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

package org.srb;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import edu.sdsc.grid.io.srb.SRBFileSystem;

//////////////////////////////////////////////////////////////////////////
//// SRBDisconnect
/**
 * This actor reads an SRB file.
 * 
 * @author Bing Zhu & Efrat Jaeger
 * @version $Id: SRBDisconnect.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class SRBDisconnect extends TypedAtomicActor {

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
	public SRBDisconnect(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		SRBFileSystem = new TypedIOPort(this, "SRBFileSystem", true, false);
		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setMultiport(true);

		// Set the type constraint.
		SRBFileSystem.setTypeEquals(BaseType.GENERAL);
		trigger.setTypeEquals(BaseType.GENERAL);

		_attachText(
				"_iconDescription",
				"<svg>\n"
						+ "<text x=\"0\" y=\"30\""
						+ "style=\"font-size:40; fill:blue; font-family:Verdana font-style:italic; font-weight:bold\">"
						+ "SRB</text>\n" + "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * Connection reference
	 */
	public TypedIOPort SRBFileSystem;

	public TypedIOPort trigger;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Closes the SRB connection.
	 */
	public void fire() throws IllegalActionException {
		for (int i = 0; i < trigger.getWidth(); i++) { // make sure all the
														// processes have
														// terminated before
														// closing the
														// connection.
			if (trigger.hasToken(i)) {
				trigger.get(i);
			}
		}
		SRBFileSystem srbFileSystem = (SRBFileSystem) ((ObjectToken) SRBFileSystem
				.get(0)).getValue();
		srbFileSystem = SRBUtil.closeConnection(srbFileSystem);
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished.
	 */
	public boolean postfire() {
		return false; // FIX ME
	}
}