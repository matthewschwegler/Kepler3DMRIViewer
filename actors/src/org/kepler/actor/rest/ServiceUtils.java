/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
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

package org.kepler.actor.rest;

//////////////////////////////////////////////////////////////////////////
////ServiceUtils
/**
 * <p>
 * ServiceUtils is a utility class.
 * </p>
 * 
 * @author Madhu, SDSC
 * @version $Id: ServiceUtils.java 24234 2010-05-06 05:21:26Z welker $
 */
public class ServiceUtils {

	public static final String LINESEP = System.getProperty("line.separator");
	public static final String FILESEP = System.getProperty("file.separator");
	public static final String USERHOME = System.getProperty("user.home");
	public static final String TAB = "\t";

	public static final String ANEMPTYSPACE = " ";
	public static final String NOSPACE = "";
	public static final String PARAMDELIMITER = ",";
	public static final String EQUALDELIMITER = "=";
	public static final String GT = ">";

	/**
	 * 
	 * Checks if supplied string is empty or null
	 * 
	 * @param String
	 * @return boolean
	 */
	public static boolean checkEmptyString(String value) {

		if (value == null || value.trim().equals(NOSPACE)) {
			return true;
		}

		return false;
	}

	/**
	 * @param val
	 * 	 */
	public static String trimString(String val) {
		return val.trim();
	}
}