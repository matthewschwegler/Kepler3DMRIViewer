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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.DateToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Create a date token. If a date string is not given on <i>input</i>, the
 * current date and time is used.
 * 
 * @author Daniel Crawl
 * @version $Id: CreateDate.java 32837 2014-07-14 22:26:04Z crawl $
 */

public class CreateDate extends LimitedFiringSource {

	/**
	 * Construct a CreateDate with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public CreateDate(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		inputFormat = new PortParameter(this, "format");
		inputFormat.setStringMode(true);
		inputFormat.getPort().setTypeEquals(BaseType.STRING);
		new Attribute(inputFormat.getPort(), "_showName");

		for (int i = 0; i < dateFormats.length; i++) {
			inputFormat.addChoice(dateFormats[i]);
		}

		input = new PortParameter(this, "input");
		input.setStringMode(true);
		input.getPort().setTypeEquals(BaseType.STRING);
		new Attribute(input.getPort(), "_showName");

		output.setTypeEquals(BaseType.DATE);
		output.setMultiport(true);
		output.setDefaultWidth(1);

		defaultTimezone = new StringParameter(this, "defaultTimezone");
		String[] timezones = TimeZone.getAvailableIDs();
		Arrays.sort(timezones);
		for(String timeZoneID : timezones) {
		    defaultTimezone.addChoice(timeZoneID);
		}
		// set the default
		defaultTimezone.setExpression(TimeZone.getDefault().getID());
		
		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * A string date and time. If not used, the current date and time is used.
	 */
	public PortParameter input;

	/**
	 * The format of the input date and time. See java.text.SimpleDateFormat for
	 * the syntax.
	 */
	public PortParameter inputFormat;
	
	/** The time zone to use when <i>inputFormat</i> is specified, but no time
	 *  zone is found in the format (specified with 'z' or 'Z'). This value is
	 *  not used when <i>inputFormat</i> is the milliseconds or seconds since
	 *  the epoch.
	 */
	public StringParameter defaultTimezone;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/** React to an attribute change. */
	@Override
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
	
	    if(attribute == defaultTimezone) {
	        String str = defaultTimezone.stringValue();
	        if(str.trim().isEmpty()) {
	            _defaultTimeZone = TimeZone.getDefault();
	        } else {
	            // make sure it's valid
	            boolean found = false;
	            final String[] timezones = TimeZone.getAvailableIDs();
	            for(String timezone : timezones) {
	                if(timezone.equals(str)) {
	                    found = true;
	                    break;
	                }
	            }
	            if(!found) {
	                throw new IllegalActionException(this, "Unknown time zone: " + str);
	            }
	            _defaultTimeZone = TimeZone.getTimeZone(str);
	        }
	    } else {
	        super.attributeChanged(attribute);
	    }
	    
	}
	
	/** Create a date token. */
	@Override
	public void fire() throws IllegalActionException {
		super.fire();

		inputFormat.update();
		input.update();

		Date outDate;

		// see if there's input
		String dateStr = ((StringToken) input.getToken()).stringValue();
		if (dateStr.equals("")) {
			// output the current date/time.
			outDate = new Date();
		} else {
			// see if there's a format
			String formatStr = ((StringToken) inputFormat.getToken())
					.stringValue();
			if (formatStr.equals("")) {
			    // no format specified, so use default
			    SimpleDateFormat sdf = new SimpleDateFormat();
			    sdf.setTimeZone(_defaultTimeZone);
				try {
                    outDate = sdf.parse(dateStr);
                } catch (ParseException e) {
                    throw new IllegalActionException(this, e, "Error parsing date string.");
                }
			} else if (formatStr.equals(MS_SINCE_EPOCH)) {
				// first parse as a double since the parser
				// for longs cannot handle scientific notation.
				long millisec = new Double(dateStr).longValue();
				outDate = new Date(millisec);
			} else if (formatStr.equals(S_SINCE_EPOCH)) {
			    long sec = new Double(dateStr).longValue();
			    outDate = new Date(sec * 1000);
			} else {
				try {
					SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
					
					// see if the format contains the time zone
					if(!formatStr.contains("z") && !formatStr.contains("Z")) {
					    sdf.setTimeZone(_defaultTimeZone);
					}

					outDate = sdf.parse(dateStr);

				} catch (IllegalArgumentException e) {
					throw new IllegalActionException(this, e,
							"Error in input format.");
				} catch (ParseException e) {
					throw new IllegalActionException(this, e,
							"Error parsing input date.");
				}
			}
		}
		
		output.broadcast(new DateToken(outDate.getTime()));
	}

	/** Constant string for special case of milliseconds since epoch.
	 *  There does not appear to be formatting symbols for this.
	 */
	public final static String MS_SINCE_EPOCH = "milliseconds since epoch";
	
	/** Constant string for seconds since epoch. */
	public final static String S_SINCE_EPOCH = "seconds since epoch";

	/** Common date formats.
	 *  @see java.text.SimpleDateFormat
	 */
	final static String[] dateFormats = { "MM-dd-yyyy", "MM-dd-yy",
			"yyyyMMdd", "yyyy.MM.dd G 'at' HH:mm:ss z", "EEE, MMM d, ''yy",
			"h:mm a", "hh 'o''clock' a, zzzz", "K:mm a, z",
			"yyyyy.MMMMM.dd GGG hh:mm aaa", "EEE, d MMM yyyy HH:mm:ss Z",
			"yyMMddHHmmssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", MS_SINCE_EPOCH,
			S_SINCE_EPOCH };
	
	/** The time zone to use when a format is specified without a timezone. TODO */
	private TimeZone _defaultTimeZone = TimeZone.getDefault();
}