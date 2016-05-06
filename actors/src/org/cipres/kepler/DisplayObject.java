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

package org.cipres.kepler;

//////////////////////////////////////////////////////////////////////////
////DisplayObject
/**
 * A DisplayObject object stores an object and its name for display. It is used
 * by SubsetChooser for displaying the name of different type objects.
 * 
 * @author Alex Borchers, Zhijie Guan
 * @version $Id: DisplayObject.java 24234 2010-05-06 05:21:26Z welker $
 */

public class DisplayObject {

	private Object object;
	private String name;

	public DisplayObject() {
	}

	/**
	 * Create a DisplayObject object with the object and its name.
	 * 
	 * @param object
	 * @param name
	 */
	public DisplayObject(Object object, String name) {
		super();
		// TODO Auto-generated constructor stub
		this.object = object;
		this.name = name;
	}

	/**
	 * Set the object for the DisplayObject object.
	 * 
	 * @param object
	 *            : object to be set
	 */
	public void setObject(Object object) {
		this.object = object;
	}

	/**
	 * Set the name for the DisplayObject object
	 * 
	 * @param name
	 *            : name to be set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the object
	 * 
	 * @return the object
	 */
	public Object getObject() {
		return object;
	}

	/**
	 * Get the object name
	 * 
	 * @return the object name as a string
	 */
	public String getName() {
		return name;
	}

}