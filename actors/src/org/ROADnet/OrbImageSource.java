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
import ptolemy.data.AWTImageToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.brtt.antelope.Orb;
import com.brtt.antelope.OrbImagePacket;
import com.brtt.antelope.OrbRawPacket;

/**
 * This actor connects to an Antelope ORB and collects packets matching the
 * given sourcename. Packets are reaped from the Orb and unpacked assuming that
 * they're OrbImagePackets. The actor outputs ImageTokens to Ptolemy.
 * 
 * @author Tobin Fricke (tobin@splorg.org), University of California
 * @version $Id: OrbImageSource.java 24234 2010-05-06 05:21:26Z welker $
 * @Pt.ProposedRating Red (tobin)
 */

public class OrbImageSource extends TypedAtomicActor {
	/**
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
	public OrbImageSource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		output = new TypedIOPort(this, "output", false, true);
		output.setMultiport(false);
		output.setTypeEquals(BaseType.OBJECT); // FIXME -- it's an Image

		orbname = new Parameter(this, "orbname");
		srcname = new Parameter(this, "srcname");

		orbname.setTypeEquals(BaseType.STRING);
		srcname.setTypeEquals(BaseType.STRING);

	}

	/** Initialize the component and connect to the ORB. */

	public void initialize() throws IllegalActionException {
		try {
			super.initialize();
			_orb = new Orb(StringToken.convert(orbname.getToken())
					.stringValue(), "r");
			_orb.select(StringToken.convert(srcname.getToken()).stringValue());
			_orb.after(0);
		} catch (Exception e) {
			throw new IllegalActionException(this, "Couldn't connect to Orb."
					+ " (" + e.getMessage() + ")");
		}

	}

	/**
	 * Reap one packet from the ORB, unstuff it as an OrbImagePacket, and output
	 * the resulting ImageToken (containing a java.awt.Image object). Note that
	 * this whole actor can be implemented as a composite actor utilizing
	 * ObjectToRecord, RecordDisassembler, and something that forms ImageTokens
	 * from java.awt.Image.
	 */

	public void fire() throws IllegalActionException {
		super.fire();
		try {
			OrbRawPacket pkt = (OrbRawPacket) (_orb.reap(false));
			OrbImagePacket imgPkt = (OrbImagePacket) (OrbImagePacket
					.unstuff(pkt));
			output.broadcast(new AWTImageToken(imgPkt.image));

		} catch (Exception e) {
			throw new IllegalActionException(this, e.getMessage());
		}
	}

	/**
	 * The name of the orb to connect to, in the format "hostname:port". Note
	 * that orbnames.pf-style names are not supported -- you have to use a valid
	 * IP address or resolvable DNS name, and you have to use a numeric port
	 * number.
	 */

	public Parameter orbname;

	/**
	 * The source name to request from the Orb. When this actor is initialized,
	 * orb.select() is called with the value of this parameter.
	 */

	public Parameter srcname;

	/** Samples from incoming waveform packets appear on this port. */

	public TypedIOPort output;

	/**
	 * This is our orb handle. Maybe one day it will also come on an input
	 * channel.
	 */

	private Orb _orb;

}
