/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-07-14 15:26:04 -0700 (Mon, 14 Jul 2014) $' 
 * '$Revision: 32837 $'
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

package org.kepler.date;

import java.util.Date;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.RandomSource;
import ptolemy.data.DateToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Generate a random date between upper and lower dates.
 * 
 * @author Daniel Crawl
 * @version $Id: RandomDate.java 32837 2014-07-14 22:26:04Z crawl $
 */

public class RandomDate extends RandomSource {

	/**
	 * Construct a RandomDate with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public RandomDate(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		lowerBound = new TypedIOPort(this, "lowerBound", true, false);
		lowerBound.setTypeEquals(BaseType.DATE);

		upperBound = new TypedIOPort(this, "upperBound", true, false);
		upperBound.setTypeEquals(BaseType.DATE);

		output.setTypeEquals(BaseType.DATE);
		output.setMultiport(true);
        output.setDefaultWidth(1);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** The lower bound of the date. */
	public TypedIOPort lowerBound;

	/** The upper bound of the date. */
	public TypedIOPort upperBound;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/** Output the most recently generated random date. */
	@Override
	public void fire() throws IllegalActionException {
		super.fire();
		output.broadcast(new DateToken(_currentDate.getTime()));
	}

	/** Generate a new random date. */
	@Override
	protected void _generateRandomNumber() throws IllegalActionException {
		long lower = ((DateToken) lowerBound.get(0)).getValue();
		long upper = ((DateToken) upperBound.get(0)).getValue();

		if (lower > upper) {
			throw new IllegalActionException(this,
					"Invalid bounds: lowerBound is later than upperBound.");
		}

		double rawNum = _random.nextDouble();
		double num = (rawNum * (upper - lower)) + lower;
		_currentDate = new Date(new Double(num).longValue());
	}

	/** The most recently generated random date. */
	private Date _currentDate;
}