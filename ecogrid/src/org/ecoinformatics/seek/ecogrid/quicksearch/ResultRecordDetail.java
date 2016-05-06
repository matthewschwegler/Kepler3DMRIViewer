/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.ecogrid.quicksearch;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;

/**
 * Detail information about a result record. This information is recorded for
 * each record and stored in the result record.
 * 
 * @see ResultRecord
 * 
 * @author Matt Jones and Jing Tao
 */
public class ResultRecordDetail extends StringAttribute {

	/**
	 * Construct a new detail record with the given name and containing record.
	 * 
	 * @param container
	 *            the record about which this detail applies
	 * @param name
	 *            the detail information
	 * @throws IllegalActionException
	 *             when the detail can not be created
	 * @throws NameDuplicationException
	 *             when the detail information already exists
	 */
	public ResultRecordDetail(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		// super(container, name);
		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"30\" height=\"40\" " + "style=\"fill:white\"/>\n"
				+ "<text x=\"3\" y=\"18\""
				+ "style=\"font-size:18; fill:green; font-family:SansSerif\">"
				+ "1 0" + "</text>\n" + "<text x=\"3\" y=\"35\""
				+ "style=\"font-size:18; fill:green; font-family:SansSerif\">"
				+ "0 1" + "</text>\n" + "</svg>\n");
	}
}