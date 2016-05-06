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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.DateToken;
import ptolemy.data.LongToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Calculate the (floor of the) difference between two dates in minutes, hours,
 * days, months, etc.
 * <p>
 * <b>NOTE:</b> this actor may take a long time to execute if the granularity is
 * small, e.g., milliseconds or seconds, and the amount of time is large.
 * </p>
 * 
 * @author Daniel Crawl
 * @version $Id: DateDifference.java 32837 2014-07-14 22:26:04Z crawl $
 */

public class DateDifference extends TypedAtomicActor {

	/**
	 * Construct a DateDifference with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public DateDifference(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		date1 = new TypedIOPort(this, "date1", true, false);
		date1.setTypeEquals(BaseType.DATE);
		new Attribute(date1, "_showName");

		date2 = new TypedIOPort(this, "date2", true, false);
		date2.setTypeEquals(BaseType.DATE);
		new Attribute(date2, "_showName");

		granularity = new PortParameter(this, "granularity");
		granularity.setStringMode(true);
		granularity.getPort().setTypeEquals(BaseType.STRING);
		new Attribute(granularity.getPort(), "_showName");
		for (String str : _granularityMap.keySet()) {
			granularity.addChoice(str);
		}

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.LONG);
		output.setMultiport(true);
	    output.setDefaultWidth(1);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** Input date. */
	public TypedIOPort date1;

	/** Input date. */
	public TypedIOPort date2;

	/**
	 * Specifies the granularity of difference between dates, e.g., hours, days,
	 * months, etc.
	 */
	public PortParameter granularity;

	/** The difference between the dates. */
	public TypedIOPort output;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Send the token in the value parameter to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	@Override
    public void fire() throws IllegalActionException {
		super.fire();

		// read the two input dates
		Date d1 = new Date(((DateToken) date1.get(0)).getValue());
		Date d2 = new Date(((DateToken) date2.get(0)).getValue());

		// determine the type of difference
		granularity.update();
		String str = ((StringToken) granularity.getToken()).stringValue();
		Integer granularityType = _granularityMap.get(str);
		if (str == null) {
			throw new IllegalActionException(this, "Unknown time granularity: "
					+ str);
		}

		// calculate and output the difference
		long diff = _calculateDiff(d1, d2, granularityType);
		output.broadcast(new LongToken(diff));
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/** Calculate the difference between two dates for a specific format. */
	private long _calculateDiff(Date d1, Date d2, Integer granularityType) {
		long retval = 0;
		GregorianCalendar cal1 = new GregorianCalendar();
		GregorianCalendar cal2 = new GregorianCalendar();

		cal1.setTime(d1);
		cal2.setTime(d2);

		GregorianCalendar lower, upper;

		// determine which date is earlier
		if (cal1.before(cal2)) {
			lower = cal1;
			upper = cal2;
		} else {
			lower = cal2;
			upper = cal1;
		}

		if (_debugging) {
			_debug("lower date: " + lower.getTime());
			_debug("upper date: " + upper.getTime());
		}

		// increment the lower calendar date until it's after the upper
		// calendar date.
		// NOTE: this could take many iterations if the granularity
		// of time is small, e.g., seconds or milliseconds, and the
		// difference between the dates is large.
		int field = granularityType.intValue();
		while (lower.before(upper)) {
			retval++;
			lower.add(field, 1);
		}

		// we want the floor of the difference, so if lower is now after
		// upper, subtract one unit.
		if (lower.after(upper)) {
			retval--;
		}

		return retval;
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	/**
	 * An mapping of strings to java.util.Calendar fields for different
	 * granularities of time.
	 */
	private static HashMap<String, Integer> _granularityMap;

	static {
		// initialize the granularity map
		_granularityMap = new HashMap<String, Integer>();
		_granularityMap.put("Milliseconds", Calendar.MILLISECOND);
		_granularityMap.put("Seconds", Calendar.SECOND);
		_granularityMap.put("Minutes", Calendar.MINUTE);
		_granularityMap.put("Hours", Calendar.HOUR_OF_DAY);
		_granularityMap.put("Days", Calendar.DAY_OF_YEAR);
		_granularityMap.put("Months", Calendar.MONTH);
		_granularityMap.put("Years", Calendar.YEAR);
	}
}