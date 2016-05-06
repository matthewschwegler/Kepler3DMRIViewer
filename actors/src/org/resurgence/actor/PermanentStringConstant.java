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

import ptolemy.actor.lib.Source;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.vergil.icon.BoxedValueIcon;

//////////////////////////////////////////////////////////////////////////
//// PermanentStringConstant

/**
 * <p>
 * This actor generates a sequence of the input string.
 * </p>
 * <p>
 * It is based on the SDM SPA StringConst actor.
 * </p>
 * 
 * @author Wibke Sudholt, University and ETH Zurich, November 2004
 * @version $Id: PermanentStringConstant.java 13429 2007-02-01 20:18:02Z berkley
 *          $
 */

public class PermanentStringConstant extends Source {

	/**
	 * Construct a PermanentStringConstant with the given container and name.
	 * Create the <i>value</i> parameter.
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
	public PermanentStringConstant(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// Use a file attribute to allow the output to be either a string
		// typed by the user or a file URL selected using a file chooser.
		value = new FileParameter(this, "value");

		// Set the type constraint.
		output.setTypeEquals(BaseType.STRING);

		BoxedValueIcon icon = new BoxedValueIcon(this, "_icon");
		icon.displayWidth.setExpression("25");
		icon.attributeName.setExpression("value");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The value produced by this constant source.
	 */
	public FileParameter value;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Send the token in the <i>value</i> parameter to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		output.send(0, new StringToken(value.stringValue()));
	}

	// /////////////////////////////////////////////////////////////////
	// // protected members ////

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	// /////////////////////////////////////////////////////////////////
	// // private members ////

}