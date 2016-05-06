/*
 * Copyright (c) 2010-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-05-09 11:05:40 -0700 (Wed, 09 May 2012) $' 
 * '$Revision: 29823 $'
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

package org.kepler.plotting;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jul 7, 2010
 * Time: 12:34:55 PM
 */

public enum PointType {
	SQUARE (new Rectangle2D.Double(-3, -3, 6, 6), "square"),
	CIRCLE (new Ellipse2D.Double(-3, -3, 6, 6), "circle"),
	TRIANGLE_UP (new Polygon(new int[] {0, 3, -3}, new int[] {-3, 3, 3}, 3), "triangle (up)"),
	DIAMOND (new Polygon(new int[] {0, 3, 0, -3}, new int[] {-3, 0, 3, 0}, 4), "diamond"),
	BAR_HORIZONTAL (new Rectangle2D.Double(-3, -1.5, 6, 3), "bar (horiz)"),
	TRIANGLE_DOWN (new Polygon(new int[] {-3, 3, 0}, new int[] {-3, -3, 3}, 3), "triangle (down)"),
	OVAL (new Ellipse2D.Double(-3, -1.5, 6, 3), "oval"),
	TRIANGLE_RIGHT (new Polygon(new int[] {-3, 3, -3}, new int[] {-3, 0, 3}, 3), "triangle (right)"),
	BAR_VERTICAL (new Rectangle2D.Double(-1.5, -3, 3, 6), "bar (vert)"),
	TRIANGLE_LEFT (new Polygon(new int[] {-3, 3, 3}, new int[] {0, -3, 3}, 3), "triangle (left)");
	
	PointType(Shape shape, String description) {
		this.shape = shape;
		this.description = description;
	}
	
	public Shape getShape() {
		return shape;
	}
	
	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString() {
		return getDescription();
	}
	
	private Shape shape;
	private String description;
}
