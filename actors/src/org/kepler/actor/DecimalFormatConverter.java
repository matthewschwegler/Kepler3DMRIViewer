/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

import java.text.DecimalFormat;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// DecimalFormatConverter

/**
 * This actor will generate a number based on the specified format.
 * 
 * @author Jianwu Wang
 * @version $Id: DecimalFormatConverter.java 24000 2010-04-28 00:12:36Z berkley $
 */
public class DecimalFormatConverter extends TypedAtomicActor {

	/**
	 * Construct a DecimalFormatConverter with the given container and name.
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
	public DecimalFormatConverter(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input = new TypedIOPort(this, "input", true, false);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);

		myformat = new PortParameter(this, "decimal format");
		myformat.setStringMode(true);
		myformat.setExpression("" + 0);
		myformat.getPort().setTypeEquals(BaseType.STRING);

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
	public TypedIOPort input = null;
	/**
	 * The output port, which contains the new directory path.
	 */
	public TypedIOPort output = null;
	/**
	 * The parameter, which is a string for the designed format.
	 */
	public PortParameter myformat = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * output the number according to designed format.
	 * 
	 * @exception IllegalActionException
	 *                
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		
		String _inputString = input.get(0).toString();
		_input = (new Double(_inputString)).doubleValue();
		
		myformat.update();
		_myformat = ((StringToken)myformat.getToken()).stringValue();
		DecimalFormat df = new DecimalFormat(_myformat);
		
		
		_output = df.format(_input);
	
		output.send(0, new StringToken(_output));
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private String _myformat;
	private double _input;
	private String _output;
}