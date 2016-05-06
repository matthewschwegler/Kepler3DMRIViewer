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

import java.text.SimpleDateFormat;
import java.util.Date;

import ptolemy.actor.lib.Transformer;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.DateToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Convert a date token to a string using a specific format. See
 * java.text.SimpleDateFormat for formatting syntax.
 * 
 * @author Daniel Crawl
 * @version $Id: DateToString.java 32837 2014-07-14 22:26:04Z crawl $
 */

public class DateToString extends Transformer {

	/**
	 * Construct a DateToString with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public DateToString(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input.setTypeEquals(BaseType.DATE);
		new Attribute(input, "_showName");

		outputFormat = new PortParameter(this, "format");
		outputFormat.setStringMode(true);
		outputFormat.getPort().setTypeEquals(BaseType.STRING);
		new Attribute(outputFormat.getPort(), "_showName");
		for (int i = 0; i < CreateDate.dateFormats.length; i++) {
			outputFormat.addChoice(CreateDate.dateFormats[i]);
		}

		output.setTypeEquals(BaseType.STRING);
		output.setMultiport(true);
        output.setDefaultWidth(1);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The format of the string. See java.text.SimpleDateFormat for formatting
	 * syntax.
	 */
	public PortParameter outputFormat;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Convert the input date to a formatted output string.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	@Override
    public void fire() throws IllegalActionException {
		super.fire();

		// read the input
		Date date = new Date(((DateToken) input.get(0)).getValue());

		// read the format
		outputFormat.update();
		String formatStr = ((StringToken) outputFormat.getToken())
				.stringValue();

		
		String outStr;
		
		if (formatStr.equals(CreateDate.MS_SINCE_EPOCH)) {
	        outStr = String.valueOf(date.getTime());
		} else if (formatStr.equals(CreateDate.S_SINCE_EPOCH)) {
		    outStr = String.valueOf(date.getTime() / 1000);
		} else {
		    SimpleDateFormat sdf;

		    if (formatStr.equals("")) {
		        sdf = new SimpleDateFormat();
		    } else {
		        sdf = new SimpleDateFormat(formatStr);
		    }
		    outStr = sdf.format(date);
		}

		output.broadcast(new StringToken(outStr));
	}
}
