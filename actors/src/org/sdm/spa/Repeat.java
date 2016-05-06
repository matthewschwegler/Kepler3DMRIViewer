/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2012-09-19 11:03:50 -0700 (Wed, 19 Sep 2012) $' 
 * '$Revision: 30712 $'
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

package org.sdm.spa;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * @deprecated
 * use ptolemy.domains.sdf.lib.Repeat instead.
 * @see ptolemy.domains.sdf.lib.Repeat
 */
public class Repeat extends TypedAtomicActor {

	public Repeat(CompositeEntity aContainer, String aName)
			throws IllegalActionException, NameDuplicationException {

		super(aContainer, aName);

		portOutput = new TypedIOPort(this, "portOutput", false, true);
		portInput = new TypedIOPort(this, "portInput", true, false);
		portNum = new TypedIOPort(this, "portNum", true, false);
		portNum.setTypeEquals(BaseType.INT);
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public TypedIOPort portOutput;
	public TypedIOPort portInput;
	public TypedIOPort portNum;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Initialize the PN actor.
	 * 
	 * @exception IllegalActionException
	 *                If the parent class throws it.
	 */
	public void initialize() throws IllegalActionException {
		super.initialize();
		_returnValue = true;
	}

	/**
	 * Fire the actor.
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		int num = ((IntToken) portNum.get(0)).intValue();
		Token tokIn = portInput.get(0);
		_debug("Process:" + num);

		for (int i = 0; i < num; i++) {
			portOutput.broadcast(tokIn);
		}
	}

	/**
	 * Post fire the actor. Return false to indicated that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return true;
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	private boolean _returnValue = true;
}