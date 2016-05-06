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

package org.kepler.sms.actors;

import java.util.Iterator;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * This is an attribute that gets attached to a configured merge actor instance.
 * The attribute denotes a simple mapping from a source port to a target port.
 * Also, a single conversion can be applied to the source port as part of the
 * mapping (in particular, a simple unit conversion)
 * 
 * @author Shawn Bowers
 * @created October 17, 2005
 */

public class SimpleMergeMapping extends Attribute {

	/**
	 * Constructor
	 */
	public SimpleMergeMapping(String sourceActor, String sourcePort,
			String targetPort) throws IllegalActionException,
			NameDuplicationException {
		super();
		setSourceActor(sourceActor);
		setSourceActorPort(sourcePort);
		setTargetPort(targetPort);
	}

	/**
	 * Constructor
	 */
	public SimpleMergeMapping(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * Constructor
	 */
	public SimpleMergeMapping(NamedObj container, String name,
			String sourceActor, String sourcePort, String targetPort)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		setSourceActor(sourceActor);
		setSourceActorPort(sourcePort);
		setTargetPort(targetPort);
	}

	/**
	 * Constructor
	 */
	public SimpleMergeMapping(NamedObj container, String name,
			String sourceActor, String sourcePort, String targetPort,
			String conversion) throws IllegalActionException,
			NameDuplicationException {
		super(container, name);
		setSourceActor(sourceActor);
		setSourceActorPort(sourcePort);
		setTargetPort(targetPort);
		setConversion(conversion);
	}

	/**
	 * @param container
	 *            The container for this attribute
	 */
	public void setContainer(NamedObj container) throws IllegalActionException,
			NameDuplicationException {
		if (container == null) {
			super.setContainer(container);
			return;
		}

		if (!(container instanceof MergeActor)) {
			String msg = "This attribute can only be applied to "
					+ "org.Kepler.sms.actor.MergeActor instances.";
			throw new IllegalActionException(this, msg);
		}
		super.setContainer(container);
	}

	/**
	 * @return The name of the source actor
	 */
	public String getSourceActor() {
		Iterator iter = attributeList(SimpleMergeSourceActor.class).iterator();
		if (!iter.hasNext())
			return null;
		SimpleMergeSourceActor a = (SimpleMergeSourceActor) iter.next();
		return a.getSourceActor();
	}

	/**
	 * @return The name of the source actor port
	 */
	public String getSourceActorPort() {
		Iterator iter = attributeList(SimpleMergeSourceActorPort.class)
				.iterator();
		if (!iter.hasNext())
			return null;
		SimpleMergeSourceActorPort p = (SimpleMergeSourceActorPort) iter.next();
		return p.getSourceActorPort();
	}

	/**
	 * @return The name of the target port
	 */
	public String getTargetPort() {
		Iterator iter = attributeList(SimpleMergeTargetPort.class).iterator();
		if (!iter.hasNext())
			return null;
		SimpleMergeTargetPort p = (SimpleMergeTargetPort) iter.next();
		return p.getTargetPort();
	}

	/**
	 * @return The name of the conversion
	 */
	public String getConversion() {
		Iterator iter = attributeList(SimpleMergeConversion.class).iterator();
		if (!iter.hasNext())
			return null;
		SimpleMergeConversion c = (SimpleMergeConversion) iter.next();
		return c.getConversion();
	}

	/**
	 * Set the name of the source actor
	 */
	public void setSourceActor(String name) throws IllegalActionException,
			NameDuplicationException {
		if (name == null)
			throw new IllegalActionException(
					"setting source actor name to null");
		Iterator iter = attributeList(SimpleMergeSourceActor.class).iterator();
		SimpleMergeSourceActor a = null;
		if (!iter.hasNext())
			a = new SimpleMergeSourceActor(this, "_sourceActor");
		else
			a = (SimpleMergeSourceActor) iter.next();
		a.setSourceActor(name);
	}

	/**
	 * Set the name of the source actor port
	 */
	public void setSourceActorPort(String name) throws IllegalActionException,
			NameDuplicationException {
		if (name == null)
			throw new IllegalActionException(
					"setting source actor port to null");
		Iterator iter = attributeList(SimpleMergeSourceActorPort.class)
				.iterator();
		SimpleMergeSourceActorPort p = null;
		if (!iter.hasNext())
			p = new SimpleMergeSourceActorPort(this, "_sourceActorPort");
		else
			p = (SimpleMergeSourceActorPort) iter.next();
		p.setSourceActorPort(name);
	}

	/**
	 * Set the name of the target port
	 */
	public void setTargetPort(String name) throws IllegalActionException,
			NameDuplicationException {
		if (name == null)
			throw new IllegalActionException("setting target port name to null");
		Iterator iter = attributeList(SimpleMergeTargetPort.class).iterator();
		SimpleMergeTargetPort p = null;
		if (!iter.hasNext())
			p = new SimpleMergeTargetPort(this, "_targetPort");
		else
			p = (SimpleMergeTargetPort) iter.next();
		p.setTargetPort(name);
	}

	/**
	 * Set the name of the conversion
	 */
	public void setConversion(String name) throws IllegalActionException,
			NameDuplicationException {
		if (name == null)
			throw new IllegalActionException(
					"setting conversion function name to null");
		Iterator iter = attributeList(SimpleMergeConversion.class).iterator();
		SimpleMergeConversion c = null;
		if (!iter.hasNext())
			c = new SimpleMergeConversion(this, "_conversion");
		else
			c = (SimpleMergeConversion) iter.next();
		c.setConversion(name);
	}

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof SimpleMergeMapping))
			return false;
		SimpleMergeMapping that = (SimpleMergeMapping) obj;

		String thatActor = that.getSourceActor();
		String thisActor = this.getSourceActor();
		String thatActorPort = that.getSourceActorPort();
		String thisActorPort = this.getSourceActorPort();
		String thatTarget = that.getTargetPort();
		String thisTarget = this.getTargetPort();
		String thatConv = that.getConversion();
		String thisConv = this.getConversion();

		if (thatActor == null || thatActorPort == null || thatTarget == null)
			return false;

		if ((thatConv == null && thisConv != null)
				|| (thisConv == null && thatConv != null))
			return false;

		if (!thatActor.equals(thisActor))
			return false;
		if (!thatActorPort.equals(thisActorPort))
			return false;
		if (!thatTarget.equals(thisTarget))
			return false;
		if (thatConv != null && !thatConv.equals(thisConv))
			return false;

		return true;
	}

	/**
	 * For testing
	 */
	public static void main(String[] args) {
		try {
			MergeActor a = new MergeActor(new CompositeEntity(),
					"Merge Actor 1");
			SimpleMergeMapping m1 = new SimpleMergeMapping(a, "merge1", "a1",
					"p1", "t1");
			SimpleMergeMapping m2 = new SimpleMergeMapping(a, "merge2", "a2",
					"p1", "t1", "c1");

			System.out.println("merge1: <" + m1.getSourceActor() + ", "
					+ m1.getSourceActorPort() + ", " + m1.getTargetPort()
					+ ", " + m1.getConversion() + ">");

			System.out.println("merge2: <" + m2.getSourceActor() + ", "
					+ m2.getSourceActorPort() + ", " + m2.getTargetPort()
					+ ", " + m2.getConversion() + ">");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

} // SimpleMergeMapping