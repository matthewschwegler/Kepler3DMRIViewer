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
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.brtt.antelope.Orb;
import com.brtt.antelope.OrbErrorException;
import com.brtt.antelope.OrbPacketChannel;
import com.brtt.antelope.OrbWaveformPacket;
import com.brtt.antelope.SourceName;

/**
 * Ptolemy actor to send waveform data to an Antelope ORB
 * 
 * @author Tobin Fricke, University of California
 * @version $Id: OrbWaveformSink.java 24234 2010-05-06 05:21:26Z welker $
 * @Pt.ProposedRating Red (tobin)
 */

public class OrbWaveformSink extends TypedAtomicActor {

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

	public OrbWaveformSink(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input = new TypedIOPort(this, "input", true, false);
		times = new TypedIOPort(this, "times", true, false);

		input.setMultiport(false);
		input.setTypeEquals(BaseType.INT);

		times.setMultiport(false);
		times.setTypeEquals(BaseType.DOUBLE);

		orbname = new Parameter(this, "orbname");
		srcname = new Parameter(this, "srcname");
		nsamp = new Parameter(this, "nsamp");
		samprate = new Parameter(this, "samprate");

		orbname.setTypeEquals(BaseType.STRING);
		srcname.setTypeEquals(BaseType.STRING);
	}

	/* The initialization will connect to the ORB */

	public void initialize() throws IllegalActionException {
		System.out.println("OrbWaveformSink: initialize");
		try {
			super.initialize();
			_orb = new Orb(StringToken.convert(orbname.getToken())
					.stringValue(), "w");
			_orb.select(StringToken.convert(srcname.getToken()).stringValue());
			_orb.after(0);
		} catch (Exception e) {
			System.out.println("Couldn't connect to ORB! (" + e + ")");
		}
	}

	public boolean prefire() throws IllegalActionException {
		return (input.hasToken(0) && super.prefire());
	}

	public void fire() throws IllegalActionException {
		super.fire();
		try {

			int nsamp = ((IntToken) (this.nsamp.getToken())).intValue();

			/*
			 * We expect timestamps to arrive synchronously with samples. We
			 * only keep the timestamps that correspond to the beginning of a
			 * packet, however.
			 */

			double sampleTime = ((DoubleToken) (times.get(0))).doubleValue();

			/*
			 * Manufacture a new array for each packet, since OrbPacket doesn't
			 * (currently) make a copy. Not sure if this is necessary.
			 */

			if (samplesSoFar == 0) {
				samplesBuffer = new int[nsamp];
				packetTime = sampleTime;
			}

			samplesBuffer[samplesSoFar] = ((IntToken) (input.get(0)))
					.intValue();
			samplesSoFar++;

			if (samplesSoFar == nsamp) {

				samplesSoFar = 0;

				String srcname = ((StringToken) (this.srcname.getToken()))
						.stringValue();

				double samprate = ((DoubleToken) (this.samprate.getToken()))
						.doubleValue();

				OrbWaveformPacket pkt = new OrbWaveformPacket(packetTime, 0,
						new SourceName(srcname));

				double calib = 1.0; // FixMe
				double calper = -1;
				String segtype = "c";

				OrbPacketChannel channel = new OrbPacketChannel(samplesBuffer,
						pkt.srcname, calib, calper, segtype, packetTime,
						samprate);

				pkt.addChannel(channel);
				// pkt.stuffGEN();

				System.out.println("Pushing this channel:\"" + channel + "\"");
				System.out.println("In this packet: \"" + pkt + "\"");
				_orb.put(pkt.stuff());

			}

		} catch (java.io.IOException e) {
			// hmm -- what is the best thing to do here?
			System.out
					.println("OrbWaveformSink: fire() experienced IOexception: "
							+ e);
		} catch (OrbErrorException e) {
			System.out
					.println("OrbWaveformSink: fire() experienced OrbErrorException: "
							+ e);
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
	/** The number of samples per packet. */
	public Parameter nsamp; // number of samples per packet
	/** The rate at which samples are being updated/entering in the packet */
	public Parameter samprate;
	/** Data values to be written to the Antelope ORB */
	public TypedIOPort input;
	/** The packet time of the ORB stream being written to */
	public TypedIOPort times;

	private Orb _orb = null;
	private int samplesBuffer[] = null;
	private int samplesSoFar = 0;
	private double packetTime = 0;
}
