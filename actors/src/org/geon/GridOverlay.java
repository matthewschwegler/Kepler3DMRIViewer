/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-12-07 15:35:50 -0800 (Tue, 07 Dec 2010) $' 
 * '$Revision: 26427 $'
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

package org.geon;

//tokens
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * A template actor used in Gravity modeling design workflow.
 * 
 * @UserLevelDocumentation A template actor used in Gravity modeling design
 *                         workflow.
 * @author Efrat Jaeger
 * @version $Id: GridOverlay.java 26427 2010-12-07 23:35:50Z welker $
 */
public class GridOverlay extends TypedAtomicActor {
	public GridOverlay(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		input = new TypedIOPort(this, "input", true, false);
		input.setTypeEquals(BaseType.DOUBLE_MATRIX); // Push or Pull
		input.setMultiport(true);
		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.DOUBLE_MATRIX);

	}

	public TypedIOPort input;
	public TypedIOPort output;

	public void initialize() throws IllegalActionException {
	}

	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	}

	public void fire() throws IllegalActionException {
		super.fire();
	}
}
