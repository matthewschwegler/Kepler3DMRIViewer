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

package org.kepler.data.conversion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ptolemy.actor.lib.Transformer;
import ptolemy.data.ArrayToken;
import ptolemy.data.DateToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * Convert a string or an array of strings to the type of the <i>output</i>
 * port. (The output port type must be explicity set). If input is an array
 * of strings, output must also be an array.
 * 
 * @author Daniel Crawl
 * @version $Id: StringToN.java 32837 2014-07-14 22:26:04Z crawl $
 */

public class StringToN extends Transformer {
	/**
	 * Construct a StringToN with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public StringToN(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		output.setMultiport(true);
		output.setDefaultWidth(1);

		_dateFormat = new SimpleDateFormat();

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	@Override
    public void fire() throws IllegalActionException {
		super.fire();

		Token outToken = null;
		Token inToken = input.get(0);
		Type outType = output.getType();

		if (inToken.getType() instanceof ArrayType) {
			Type outElementType;

			if (!(outType instanceof ArrayType)) {
				throw new IllegalActionException(this,
						"Cannot convert array of strings to non-array.");
			} else {
				outElementType = ((ArrayType) outType).getElementType();
			}

			Token[] array = ((ArrayToken) inToken).arrayValue();
			Token[] out = new Token[array.length];
			for (int i = 0; i < array.length; i++) {
				String str = ((StringToken) array[i]).stringValue();
				out[i] = _makeToken(str, outElementType);
			}
			outToken = new ArrayToken(out);
		} else {
			String str = ((StringToken) inToken).stringValue();
			outToken = _makeToken(str, outType);
		}

		output.broadcast(outToken);
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/** Convert a string to a specific type. */
    private Token _makeToken(String str, Type type)
			throws IllegalActionException {
	    try {
    		if (type == BaseType.DOUBLE) {
    			return new DoubleToken(Double.valueOf(str).doubleValue());
    		} else if (type == BaseType.INT) {
    			return new IntToken(Integer.valueOf(str).intValue());
    		} else if (type == BaseType.LONG) {
    			return new LongToken(Long.valueOf(str).longValue());
    		} else if (type == BaseType.STRING) {
    			return new StringToken(str);
    		} else if (type == BaseType.DATE) {
    			try {
    				Date date = _dateFormat.parse(str);
    				return new DateToken(date.getTime());
    			} catch (ParseException e) {
    				throw new IllegalActionException(this, e, "Could not parse "
    						+ " date string " + str);
    			}
    		} else if(type instanceof RecordType) { 
    		    return new RecordToken(str);
    	    } else {
    			throw new IllegalActionException(this, "Conversion from string to "
    					+ type + " not implemented.");
    		}
	    } catch(NumberFormatException e) {
	        throw new IllegalActionException(this, e, "Error converting string to number.");
	    }
	}

	/** Used to convert strings to date objects. */
	private SimpleDateFormat _dateFormat;
}