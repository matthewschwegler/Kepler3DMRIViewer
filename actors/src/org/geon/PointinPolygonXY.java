/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
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

package org.geon;

import java.util.Vector;

import nl.skybound.awt.DoublePolygon;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// PointInPolygonXY
/**
 * This actor receives an array of polygons and regions and a classification
 * point. It returns an array of all the regions the point falls in.
 * 
 * @author Efrat Jaeger
 * @version $Id: PointinPolygonXY.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class PointinPolygonXY extends TypedAtomicActor {

	/**
	 * Construct an actor with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the actor cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public PointinPolygonXY(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		// Set the type constraint.
		region = new TypedIOPort(this, "region", false, true);
		region.setTypeEquals(new ArrayType(BaseType.STRING));
		point = new TypedIOPort(this, "point", true, false); // input point
		point.setTypeEquals(new ArrayType(BaseType.DOUBLE));
		polygonRegions = new TypedIOPort(this, "polygonRegions", true, false); // Polygon
																				// coordinates
																				// and
																				// regions
		polygonRegions.setTypeEquals(BaseType.GENERAL);

		_attachText("_iconDescription", "<svg>\n"
				+ "<polygon points=\"-15,-2 0,-15 15,-2 11,15 -11,15\" "
				+ "style=\"fill:white\"/>\n"
				+ "<circle cx=\"3\" cy=\"4\" r=\"1\""
				+ "style=\"fill:black\"/>\n" + "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * A classification point.
	 */
	public TypedIOPort point;

	/**
	 * A set of polygons and their region names.
	 */
	public TypedIOPort polygonRegions;

	/**
	 * The point's region.
	 */
	public TypedIOPort region;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Receive a set of polygons and a classification point and returns the
	 * point region along with a URL for browser display of the point in the
	 * polygon.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		if (polygonRegions.hasToken(0) && point.hasToken(0)) {
			ObjectToken obj1 = (ObjectToken) polygonRegions.get(0);
			PolygonUtil polygons[] = (PolygonUtil[]) obj1.getValue();
			ArrayToken arrayToken = (ArrayToken) point.get(0);
			double x = ((DoubleToken) arrayToken.getElement(0)).doubleValue();
			double y = ((DoubleToken) arrayToken.getElement(1)).doubleValue();
			// Point2D.Double p = (Point2D.Double) obj2.getValue();
			// double x = p.getX();
			// double y = p.getY();
			Vector result = new Vector();
			for (int j = 0; j < polygons.length; j++) {
				DoublePolygon P = polygons[j].getPolygon();
				arrayLen = P.npoints;
				String _region = polygons[j].getRegion();
				if (_isPolygonVertex(P, x, y) || _onPolygonEdge(P, x, y)
						|| P.contains(x, y)) {
					result.add(new StringToken(_region)); // FIX ME!!! TAKE CARE
															// OF MULTIPLE
															// REGIONS AND NO
															// REGION!!!
				}
			}
			Token[] regions = null;
			if (result.size() > 0) {
				regions = new Token[result.size()];
				result.toArray(regions);
			} else {
				regions = new Token[1];
				regions[0] = new StringToken("");
			}
			region.broadcast(new ArrayToken(regions));
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/*
	 * Checking whether the given point is one of the polygon's vertices.
	 */
	private boolean _isPolygonVertex(DoublePolygon P, double x, double y) {
		for (int i = 0; i < arrayLen; i++) {
			if (P.xpoints[i] == x && P.ypoints[i] == y)
				return true;
		}
		return false;
	}

	/*
	 * Checking whether the given point is one of the polygon's edges.
	 */
	private boolean _onPolygonEdge(DoublePolygon P, double x, double y) {
		double x1, x2, y1, y2;
		for (int i = 0; i < arrayLen; i++) {
			x1 = P.xpoints[i];
			x2 = P.xpoints[(i + 1) % arrayLen];
			y1 = P.ypoints[i];
			y2 = P.ypoints[(i + 1) % arrayLen];
			if (((x1 <= x && x <= x2) || (x2 <= x && x <= x1)) && // x and y are
																	// between
																	// x1,x2 and
																	// y1,y2
					((y1 <= y && y <= y2) || (y2 <= y && y <= y1))) {
				if (((y - y1) / (x - x1)) == ((y - y2) / (x - x2)))
					return true;
			}
		}
		return false;
	}

	/*
	 * Adding the classification point to the SVG file for display purposes.
	 */
	/*
	 * private void _addRegionToSVG(String svgFile, String region) { try { //
	 * System.out.println("SVG File: " + svgFile); File svg = new File(svgFile);
	 * BufferedReader br = new BufferedReader(new FileReader(svg)); String line;
	 * String toFile = ""; String extraLine; if (region.length() > 20) { int tmp
	 * = result.indexOf(" ",15); extraLine =
	 * "<text x= '315' y='70' fill='blue' text-anchor='middle' style='font-size: 12pt; font-family: serif; ' >"
	 * + region.substring(0,tmp) + "</text>"; extraLine +=
	 * "<text x= '315' y='90' fill='blue' text-anchor='middle' style='font-size: 12pt; font-family: serif; ' >"
	 * + region.substring(tmp+1) + "</text>"; } else { extraLine =
	 * "<text x= '315' y='70' fill='blue' text-anchor='middle' style='font-size: 12pt; font-family: serif; ' >"
	 * + region + "</text>"; } // System.out.println("Extra line" + extraLine);
	 * while ( (line = br.readLine()) != null) { int ind =
	 * line.toLowerCase().indexOf("<text"); if (ind != -1) { //
	 * System.out.println("Inside extra line"); toFile += line.substring(0, ind)
	 * + "\n"; toFile += extraLine + "\n"; toFile += line.substring(ind) + "\n";
	 * } else toFile += line + "\n"; } br.close();
	 * 
	 * // System.out.println(toFile); BufferedWriter out = new
	 * BufferedWriter(new FileWriter(svg)); out.write(toFile); out.close(); }
	 * catch (IOException e) { MessageHandler.error("Error opening file", e); }
	 * }
	 */
	// /////////////////////////////////////////////////////////////////
	// // private members ////
	// private double x, y;
	// private String id;
	private int arrayLen;
}