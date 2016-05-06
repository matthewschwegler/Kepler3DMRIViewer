/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2013-01-16 15:35:49 -0800 (Wed, 16 Jan 2013) $' 
 * '$Revision: 31341 $'
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

package org.kepler.authentication;

/**
 * This is an exception class for the authentication package to throw when
 * there's an error.
 */

public class AuthenticationException extends Exception {
	/**
	 * set this type if the exception is thrown because the user canceled the
	 * action
	 */
	public static int USER_CANCEL = 1;

	/**
	 * set this type if the exception is normal. this is the default.
	 */
	public static int NORMAL = 2;

	/**
	 * set this type if the auth failed due to bad pw or username
	 */
	public static int BAD_USER = 3;

	// set the default to normal
	private int type = NORMAL;

	/**
	 * constructor
	 */
	public AuthenticationException() {
		super();
	}

	/**
	 * constructor
	 */
	public AuthenticationException(String msg) {
		super(msg);
	}

	/**
	 * constructor
	 */
	public AuthenticationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * constructor
	 */
	public AuthenticationException(Throwable cause) {
		super(cause);
	}

	/**
	 * set the type of this exception
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * get the type of this exception
	 */
	public int getType() {
		return type;
	}
}