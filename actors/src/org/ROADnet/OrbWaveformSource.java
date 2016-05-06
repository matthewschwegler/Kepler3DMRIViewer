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
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.brtt.antelope.Orb;
import com.brtt.antelope.OrbPacketChannel;
import com.brtt.antelope.OrbWaveformPacket;

/**
 * This actor connects to an Antelope ORB and collects packets matching the
 * given sourcename, supplying samples from waveform packets to the channels of
 * the output port. Waveforms encapsulated as GEN and GENC will always go to
 * channel 0 of the output port. MGENC packets may contain multiple channels,
 * and the samples are sent to channels 0, 1, 2, ... up to the number of
 * channels contained. Make sure that the relation connected to the output port
 * has its "Width" parameter set properly!
 * 
 * @see OrbPacketSource
 * @see OrbWaveformSink
 * @see Orb
 * @author Tobin Fricke, University of California
 * @version $Id: OrbWaveformSource.java 29949 2012-06-14 22:40:45Z crawl $
 * @Pt.ProposedRating Red (tobin)
 */

public class OrbWaveformSource extends TypedAtomicActor {
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
	public OrbWaveformSource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		output = new TypedIOPort(this, "output", false, true);
		output.setMultiport(true);
		output.setTypeEquals(BaseType.INT);

		times = new TypedIOPort(this, "times", false, true);
		times.setMultiport(false);
		times.setTypeEquals(BaseType.DOUBLE);

		orbname = new Parameter(this, "orbname");
		srcname = new Parameter(this, "srcname");

		orbname.setTypeEquals(BaseType.STRING);
		srcname.setTypeEquals(BaseType.STRING);

		_attachText(
				"_iconDescription",
				"<svg>"
						+ "  <rect x=\"-30\" y=\"-20\" width=\"60\" height=\"40\" "
						+ "        style=\"fill:white\"/> "
						+ "  <circle cx=\"0\" cy=\"0\" r=\"15\" style=\"stroke:black\"/> "
						+ "  <text x=\"-10\" y=\"-5\">Orb</text>" + "</svg>");
	}

	/** Initialize the component and connect to the ORB. */

	public void initialize() throws IllegalActionException {
		System.out.println("OrbWaveformSource: initialize");
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
	 * Reap one packet from the ORB and distribute the samples contained in
	 * channels to the output port.
	 */

	public void fire() throws IllegalActionException {
		super.fire();
		try {

			OrbWaveformPacket pkt = (OrbWaveformPacket) (_orb.reap(true));
			/* Is there a better way to warn the user of situations like this? */

			if (pkt.channels.size() != output.getWidth()) {
				_debug("Packet received from ORB contains "
						+ pkt.channels.size()
						+ "channels, but output port contains "
						+ output.getWidth() + " channels.");
			}

			for (int c = 0; c < pkt.channels.size(); c++) {
				OrbPacketChannel channel = (OrbPacketChannel) (pkt.channels
						.get(c));
				for (int n = 0; n < channel.nsamp; n++) {
					output.send(c, new IntToken(channel.data[n]));

					/*
					 * We only broadcast the timestamps corresponding to channel
					 * zero. I'm not sure whether this is the best plan.
					 */

					if (c == 0)
						times.broadcast(new DoubleToken(channel.time + n
								/ channel.samprate));
				}
			}
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

	/**
	 * Samples from incoming waveform packets appear on this port. For
	 * multiplexed packets (eg, MGENC format), the channels in incoming packets
	 * are mapped to the channels of this output port. In that case you should
	 * verify that the "width" property of the relation connected to this port
	 * is set to the expected number of channels. Also, rather than connecting
	 * multiple relations directly to this port, you should probably use a
	 * single "relation" object (black diamond), so that all of the channels are
	 * carried on the same relation. Then you can use the "Select" actor to
	 * access specific channels.
	 */
	public TypedIOPort output;

	/**
	 * The timestamps for individual samples are output on this port. It's up to
	 * you to ensure that samples and their associated time stamps are consumed
	 * synchronously. At present, this is a single port and the times correspond
	 * to samples on channel zero of packets reaped from the Orb. Another
	 * possibility would be to demultiplex the times in a way analagous to the
	 * way samples themselves are distributed. Sample times are computed from
	 * the start time given in each packet and the sample rate.
	 */
	public TypedIOPort times;

	private Orb _orb;

}
