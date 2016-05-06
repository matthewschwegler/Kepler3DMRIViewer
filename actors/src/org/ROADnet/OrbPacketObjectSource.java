/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-06-14 15:40:45 -0700 (Thu, 14 Jun 2012) $' 
 * '$Revision: 29949 $'
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
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.brtt.antelope.Orb;
import com.brtt.antelope.OrbPacket;
import com.brtt.antelope.OrbRawPacket;

/**
 * This actor connects to an Antelope ORB and provides a stream of
 * {@link OrbPacket} objects to Ptolemy, delivered as {@link ObjectToken}s. This
 * is part of an experiment to provide an interface to Antelope that closely
 * follows the traditional programming interface, as opposed to a more abstract
 * interface that would provide "waveforms" or "database tuples" without
 * explicitly dealing with the mechanics of orb packets.
 * 
 * @see OrbWaveformSource
 * @see OrbImageSource
 * @author Tobin Fricke, University of California
 * @version $Id: OrbPacketObjectSource.java 29949 2012-06-14 22:40:45Z crawl $
 */

public class OrbPacketObjectSource extends TypedAtomicActor {

	public OrbPacketObjectSource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		output = new TypedIOPort(this, "output", false, true);
		output.setMultiport(true);
		output.setTypeEquals(BaseType.OBJECT);

		input = new TypedIOPort(this, "input", true, false);
		input.setMultiport(true);
		input.setTypeEquals(BaseType.OBJECT);

		orbname = new Parameter(this, "orbname");
		srcname = new Parameter(this, "srcname");

		orbname.setTypeEquals(BaseType.STRING);
		srcname.setTypeEquals(BaseType.STRING);
	}

	/** Connect to the ORB */

	public void initialize() throws IllegalActionException {
		System.out.println("OrbSource: initialize");
		try {
			super.initialize();

			String orb = StringToken.convert(orbname.getToken()).stringValue();
			String src = StringToken.convert(srcname.getToken()).stringValue();

			// how should we handle permissions?

			_orb = new Orb(orb, "rw");
			_orb.select(src);
			_orb.after(0);
		} catch (Exception e) {
			// ...
		}

	}

	/**
	 * Reap one packet from the ORB, and broadcast it to the output port as an
	 * ObjectToken holding an OrbPacket object.
	 */

	public void fire() throws IllegalActionException {
		super.fire();
		try {

			// first take care of any inputs

			for (int c = 0; c < input.getWidth(); c++) {
				while (input.hasToken(c)) {
					ObjectToken token = (ObjectToken) (input.get(c));

					// fixme: we should actually check the following

					OrbPacket packet = (OrbPacket) (token.getValue());

					if (!(packet instanceof OrbRawPacket)) {
						packet = packet.stuff();
					}

					_orb.put((OrbRawPacket) packet);
				}
			}

			// now take care of the outputs

			if (output.numberOfSinks() > 0) {

				// we have to decide whether this should block or not --
				// depends on domain?

				OrbPacket pkt = _orb.reap(true);

				output.broadcast(new ObjectToken(pkt));
			}

		} catch (Exception e) {
			throw new IllegalActionException(this, e.getMessage());
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

	private Orb _orb;

}
