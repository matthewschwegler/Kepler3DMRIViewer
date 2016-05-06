/*
 * Copyright (c) 2010 The Regents of the University of California.
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

package org.ROADnet;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.brtt.antelope.OrbWaveformPacket;

/**
 * This actor receives OrbPacket objects ensconced in ObjectTokens and extracts
 * any OrbPacketChannels from the OrbPackets and outputs them as
 * OrbPacketChannel objects ensconced in ObjectTokens.
 * 
 * @author Tobin Fricke, University of California
 * @version $Id: OrbPacketChannelExtractor.java 11161 2005-11-01 20:39:16Z
 *          ruland $
 */

public class OrbPacketChannelExtractor extends TypedAtomicActor {

	public OrbPacketChannelExtractor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		output = new TypedIOPort(this, "output", false, true);
		output.setMultiport(true);
		output.setTypeEquals(BaseType.OBJECT);

		input = new TypedIOPort(this, "input", true, false);
		input.setMultiport(true);
		input.setTypeEquals(BaseType.OBJECT);
	}

	/** Connect to the ORB */

	public void initialize() throws IllegalActionException {
		super.initialize();
	}

	/** Process a token */

	public void fire() throws IllegalActionException {
		super.fire();
		try {

			// first take care of any inputs

			for (int c = 0; c < input.getWidth(); c++) {
				while (input.hasToken(c)) {
					ObjectToken token = (ObjectToken) (input.get(c));

					// fixme: we should actually check the following

					OrbWaveformPacket packet = (OrbWaveformPacket) (token
							.getValue());

					for (int i = 0; i < packet.channels.size(); i++) {
						Token result = new ObjectToken(packet.channels.get(i));
						output.broadcast(result);
					}
				}
			}

		} catch (Exception e) {
			// ...
		}

	}

	/**
	 * The name of the Antelope ORB to connect to, in the format
	 * "hostname:port".
	 */

	public Parameter orbname;

	/** The sourcename to request from the ORB. */

	public Parameter srcname;

	/**
	 * Packets reaped from the ORB will appear on this port as Ptolemy
	 * ObjectTokens.
	 */

	public TypedIOPort output;
	public TypedIOPort input;

}
