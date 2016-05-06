/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
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

package org.geon;

import ptolemy.actor.lib.Transformer;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// TokenToSeparateChannels
/**
 * Transmit each received token to a different consecutive output channel.
 * 
 * @author Efrat Jaeger
 * @version $Id: TokenToSeparateChannels.java 12324 2006-04-04 17:23:50Z
 *          altintas $
 * @since Ptolemy II 3.0.2
 */

public class TokenToSeparateChannels extends Transformer {

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
	public TokenToSeparateChannels(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		output.setMultiport(true);
		output.setTypeAtLeast(input);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Read the input token, and transmits it each time to a different
	 * consecutive channel. May be used to parallel inputs to different
	 * processors.
	 * 
	 * @exception IllegalActionException
	 *                Not thrown in this base class
	 */
	public void fire() throws IllegalActionException {

		if (channelInd == outputWidth)
			channelInd = 0;

		Token inputToken = input.get(0);
		output.send(channelInd, inputToken);
		channelInd++;
	}

	/**
	 * Gets the output port width. Sets the current channel index.
	 * 
	 * @exception IllegalActionException
	 *                If the parent class throws it.
	 * @return Whatever the superclass returns (probably true).
	 */
	public void initialize() throws IllegalActionException {
		channelInd = 0;
		outputWidth = output.getWidth();

		super.initialize();
	}

	/**
	 * Specifies the width of the output port
	 */
	private int outputWidth;

	/**
	 * The index of the channel used in the current iteration
	 */
	private int channelInd;
}