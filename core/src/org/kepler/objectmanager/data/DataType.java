/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.data;

public class DataType {
	/**
	 * the following constants are definitions for the dataType type as seen in
	 * the config.xml file
	 */
	/** represents numbers that are not necessarily whole */
	public static final int REAL = 0;
	/** represents an integer {...,-3,-2,-1,0,1,2,3,...} */
	public static final int INTEGER = 1;
	/** text type. In the config.xml file, this is represented as 'nan' */
	public static final int STRING = 2;

	// number type definitionation
	public static final String REALTYPE = "real";
	public static final String INTEGERTYPE = "integer";
	public static final String STRINGTYPE = "nan";
	public static final String NATURALTYPE = "natural";
	public static final String WHOLETYPE = "whole";
	public static final String DATETIMETYPE = "datetime";
	private static final String[] NUMBERTYPEARRAY = { REALTYPE, INTEGERTYPE,
			STRINGTYPE, NATURALTYPE, WHOLETYPE, DATETIMETYPE };

	/**
	 * The following definitions are definitions of the types that should be
	 * used in the class Attribute for defining what kind of object an attribute
	 * uses.(DataType name)
	 */
	public static final String INT = "INTEGER";
	public static final String LONG = "LONG";
	public static final String FLOAT = "FLOAT";
	public static final String DOUBLE = "DOUBLE";
	public static final String STR = "STRING";
	public static final String DATETIME = "DATETIME";

	// private variables
	private double min;
	private double max;
	private String numberType; // this should be one of the predefined
								// numberType in configure.xml
	private String[] aliases;
	private String name;

	/**
	 * Constructor. Note that the type should be taken from one of the public
	 * static variables in this class.
	 * 
	 * @param min
	 *            the minimum value for this data type
	 * @param max
	 *            the maximum value for this data type
	 * @param precision
	 *            the precision for this data type
	 * @param type
	 *            the type of number represented by this data type. This value
	 *            should be taken from the public static variables defined in
	 *            this class.
	 * @param aliases
	 *            one or more names given to this type.
	 * @param name
	 *            the proper monarch name of the DataType. This should equate to
	 *            one of (INT, LONG, FLOAT, DOUBLE, STRING) from the above
	 *            static definitions.
	 */
	public DataType(String name, double min, double max, String type,
			String[] aliases) throws Exception {
		this.min = min;
		this.max = max;
		setupNumberType(type);
		this.aliases = aliases;
		this.name = name;
	}

	/**
	 * return the minimum in the range for the type
	 */
	public String getName() {
		return name;
	}

	/**
	 * return the minimum in the range for the type
	 */
	public double getMin() {
		return min;
	}

	/**
	 * return the maximum in the range for the type
	 */
	public double getMax() {
		return max;
	}

	/**
	 * return the type of the data type. This shoud be one of the constants
	 * (REAL, INTEGER, STRING) defined above.
	 */
	public String getNumberType() {
		return numberType;
	}

	public String[] getNames() {
		return aliases;
	}

	/**
	 * returns a string representation of this DataType
	 */
	public String toString() {
		StringBuilder buf = new StringBuilder("name: ");
        buf.append(name);
		buf.append("\naliases: ");
		for (int i = 0; i < aliases.length; i++) {
			buf.append(aliases[i]);
			if (i != aliases.length - 1) {
				buf.append(", ");
			}
		}
		buf.append("\nmin: ");
        buf.append(min);
		buf.append("\nmax: ");
        buf.append(max);
		buf.append("\ntype: ");
        buf.append(numberType);

		return buf.toString();
	}

	/*
	 * This method will lookup the array and found the pre-defined number type
	 * If couldn't find it, throw a exception
	 */
	private void setupNumberType(String numType)
			throws UnresolvableTypeException {
		if (numType == null || numType.equals("")) {
			throw new UnresolvableTypeException("Couldn't find numberType for "
					+ numType);
		}
		boolean findIt = false;
		int length = NUMBERTYPEARRAY.length;
		for (int i = 0; i < length; i++) {
			String valueInArray = NUMBERTYPEARRAY[i];
			if (valueInArray != null && valueInArray.equals(numType)) {
				this.numberType = numType;
				findIt = true;
				break;
			}
		}
		// if findIt is false, there is no numbertype for the given numType,
		// throw a exception
		if (!findIt) {
			throw new UnresolvableTypeException("Couldn't find numberType for "
					+ numType);
		}
	}
}