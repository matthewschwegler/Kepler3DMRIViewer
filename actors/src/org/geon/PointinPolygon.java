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

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import nl.skybound.awt.DoublePolygon;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.gui.MessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// PointInPolygon
/**
 * This actor receives an array of polygon points and populates them in a
 * polygon object it also receives a classification point. The actor outputs
 * whether the point is contained in the polygon along with the polygon
 * coordinates for display purposes.
 * 
 * @author Efrat Jaeger
 * @version $Id: PointinPolygon.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class PointinPolygon extends TypedAtomicActor {

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
	public PointinPolygon(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		// Set the type constraint.
		region = new TypedIOPort(this, "region", false, true);
		region.setTypeEquals(BaseType.STRING);
		toBrowser = new TypedIOPort(this, "toBrowser", false, true);
		toBrowser.setTypeEquals(BaseType.STRING);
		point = new TypedIOPort(this, "point", true, false); // input point
		point.setTypeEquals(BaseType.GENERAL);
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

	/**
	 * A URL for display the point in the regions.
	 */
	public TypedIOPort toBrowser;

	/**
	 * Receive a set of polygons and a classification point and returns the
	 * point region along with a URL for browser display of the point in the
	 * polygon.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		int layer = 1;
		while (true) {
			if (polygonRegions.hasToken(0) && point.hasToken(0)) {
				ObjectToken obj1 = (ObjectToken) polygonRegions.get(0);
				PolygonUtil polygons[] = (PolygonUtil[]) obj1.getValue();
				ObjectToken obj2 = (ObjectToken) point.get(0);
				Point p = (Point) obj2.getValue();
				x = p.getX();
				y = p.getY();
				// System.out.println("pointInPolygon point: {" + x + " , " + y
				// + "}");
				for (int j = 0; j < polygons.length; j++) {
					DoublePolygon P = polygons[j].getPolygon();
					// for (int i=0; i<P.npoints;i++)
					// System.out.println("{" + P.xpoints[i] + " , " +
					// P.ypoints[i] + "}");
					arrayLen = P.npoints;
					String _region = polygons[j].getRegion();
					// System.out.println("region = " + _region);
					if (_isPolygonVertex(P, x, y) || _onPolygonEdge(P, x, y)
							|| P.contains(x, y)) {
						result = _region; // FIX ME!!! TAKE CARE OF MULTIPLE
											// REGIONS AND NO REGION!!!
					}
				}

				// get the working directory.
				String _keplerPath = System.getProperty("user.dir");

				String displayFile = _keplerPath + "/lib/testdata/geon/layer"
						+ layer + ".svg";
				_addRegionToSVG(displayFile, result);
				region.broadcast(new StringToken(result));
				if (layer == 1 && !result.trim().startsWith("diorite")) // FIX
																		// ME!!
																		// for
																		// now
																		// only
																		// diorite
																		// has a
																		// second
																		// level.
					layer++;
				// System.out.println("file to display: " + displayFile);
				toBrowser.broadcast(new StringToken(displayFile)); // FIX ME!!!
																	// ADD THE
																	// FILE TO
																	// SHOW!!!
				layer++;
				if (layer > 2)
					break;
			}
		}
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return false;
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
	private void _addRegionToSVG(String svgFile, String region) {
		try {
			// System.out.println("SVG File: " + svgFile);
			File svg = new File(svgFile);
			BufferedReader br = new BufferedReader(new FileReader(svg));
			String line;
			String toFile = "";
			String extraLine;
			if (region.length() > 20) {
				int tmp = result.indexOf(" ", 15);
				extraLine = "<text x= '315' y='70' fill='blue' text-anchor='middle' style='font-size: 12pt; font-family: serif; ' >"
						+ region.substring(0, tmp) + "</text>";
				extraLine += "<text x= '315' y='90' fill='blue' text-anchor='middle' style='font-size: 12pt; font-family: serif; ' >"
						+ region.substring(tmp + 1) + "</text>";
			} else {
				extraLine = "<text x= '315' y='70' fill='blue' text-anchor='middle' style='font-size: 12pt; font-family: serif; ' >"
						+ region + "</text>";
			}
			// System.out.println("Extra line" + extraLine);
			while ((line = br.readLine()) != null) {
				int ind = line.toLowerCase().indexOf("<text");
				if (ind != -1) {
					// System.out.println("Inside extra line");
					toFile += line.substring(0, ind) + "\n";
					toFile += extraLine + "\n";
					toFile += line.substring(ind) + "\n";
				} else
					toFile += line + "\n";
			}
			br.close();

			// System.out.println(toFile);
			BufferedWriter out = new BufferedWriter(new FileWriter(svg));
			out.write(toFile);
			out.close();
		} catch (IOException e) {
			MessageHandler.error("Error opening file", e);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private double x, y;
	private String result, id;
	private int arrayLen;
}