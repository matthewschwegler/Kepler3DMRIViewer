/*
 * Copyright (c) 2010 The Regents of the University of California.
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

package org.kepler.sms;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jul 16, 2009
 * Time: 4:37:50 PM
 */
public class Color {
	public Color() {}
	public Color(java.awt.Color color) {
		this.color = color;
		this.colorString = color.getRed() + "," + color.getGreen() + "," + color.getBlue();
	}
	
	private static java.awt.Color DEFAULT_COLOR = java.awt.Color.WHITE;
	
	public Color(String colorString) {
		initializeAsStaticField(colorString);
		if (color == null) {
			initializeAsRGB(colorString);
		}
		if (color == null) {
			initializeAsDefault();
		}
	}
	
	public static Color getDefaultColor() {
		return new Color(DEFAULT_COLOR);
	}

	private void initializeAsDefault() {
		this.color = DEFAULT_COLOR;
		setColorString();
	}

	public void initializeAsStaticField(String string) {
		Class awtColorClass = java.awt.Color.class;
		try {
			Field field = awtColorClass.getField(string.toUpperCase());
			this.color = (java.awt.Color) field.get(null);
			setColorString();
		}
		catch(NoSuchFieldException ignored) {}
		catch(IllegalAccessException ignored) {}
	}
	
	public void initializeAsRGB(String string) {
		Pattern pattern = Pattern.compile("([0-9]+),([0-9]+),([0-9]+)");
		Matcher matcher = pattern.matcher(string);
		if (matcher.matches()) {
			int r = Integer.valueOf(matcher.group(1));
			int g = Integer.valueOf(matcher.group(2));
			int b = Integer.valueOf(matcher.group(3));
			this.color = new java.awt.Color(r, g, b);
			setColorString();
		}
	}
	
	public void setColor(java.awt.Color color) {
		this.color = color;
		this.setColorString();
	}

	private void setColorString() {
		colorString = color.getRed() + "," + color.getGreen() + "," + color.getBlue();
	}

	@Override
	public String toString() {
		return colorString;
	}
	
	public java.awt.Color getAwtColor() {
		return this.color;
	}
	
	private java.awt.Color color;
	private String colorString;
	
	public static Color RED = new Color(java.awt.Color.RED);
}
