/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.backprop;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/*<p></p>*/

public class BackProp extends TypedAtomicActor {
	// input ports
	public TypedIOPort inputFilename = new TypedIOPort(this, "inputFilename",
			true, false);
	public TypedIOPort outputFilename = new TypedIOPort(this, "outputFilename",
			true, false);

	/*
	 * public TypedIOPort outputASCII = new TypedIOPort(this, "outputASCII",
	 * true, false); public TypedIOPort outputBMP = new TypedIOPort(this,
	 * "outputBMP", true, false); //output ports public TypedIOPort
	 * outputASCIIFileName = new TypedIOPort(this, "outputASCIIFileName", false,
	 * true); public TypedIOPort outputBMPFileName = new TypedIOPort(this,
	 * "outputBMPFileName", false, true);
	 */

	/**
	 * BackProp Actor
	 */
	public BackProp(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		inputFilename.setTypeEquals(BaseType.STRING);
		outputFilename.setTypeEquals(BaseType.STRING);
	}

	/**
   *
   */
	public void initialize() throws IllegalActionException {
	}

	/**
   *
   */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	/**
   *
   */
	public void fire() throws IllegalActionException {
		System.out.println("firing BackProp");
		super.fire();

		StringToken inputFilenameToken = (StringToken) inputFilename.get(0);
		String inputFilenameStr = inputFilenameToken.stringValue();

		StringToken outputFilenameToken = (StringToken) outputFilename.get(0);
		String outputFilenameStr = outputFilenameToken.stringValue();

		System.out.println("Calling BackProp?JNI Code");
		new BackpropJniGlue().doBackProp(inputFilenameStr, outputFilenameStr);

		// outputASCIIFileName.broadcast(new StringToken(outputASCIIStr));
		// outputBMPFileName.broadcast(new StringToken(outputBMPStr));

		System.out.println("done");
	}
}