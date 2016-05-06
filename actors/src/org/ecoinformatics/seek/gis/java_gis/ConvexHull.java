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

/**
 *         <b>Name:</b> ConvexHull.java<br>
 *      <b>Purpose:</b> Given a set of x,y points, this code calculates those points
 *               which makeup the ConvexHull, a polygon which surrounds the
 *               original set like a 'rubberband'. In this case, the 'wrapping paper'
 *               algorithm' described at
 *               http://www.cse.unsw.edu.au/~lambert/java/3d/hull.html
 *               is used.<br>
 *
 *               Note that the ConvexHull points can be converted to a Java2D 'Shape'
 *               Point location within the Hull can then be tested using Java2D methods.
 *               There is also a method for 'scaling' the ConvexHull polygon about its center 
 *
 *       @author: Dan Higgins NCEAS UC Santa Barbara
 *         @date: November, 2004  
 *
 */

package org.ecoinformatics.seek.gis.java_gis;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.util.Vector;

public class ConvexHull {

	Vector pointList; // vector of points as Point2D.Double objects
	Vector cvList; // ConvexHull list
	Vector scvList; // scaled ConvexHull list

	public ConvexHull() {
	}

	public int getCHListSize() {
		if (cvList != null) {
			return cvList.size();
		}
		return 0;
	}

	public ConvexHull(File pointsFile) {
		double x1, x2, x3, y1, y2, y3;
		FileReader inReader = null;
		BufferedReader bufReader = null;
		try {
			inReader = new FileReader(pointsFile);
			bufReader = new BufferedReader(inReader);
			pointList = getPoints(bufReader);
		} catch (Exception ee) {
			System.out.println("Exception reading points!");
		}
		Point2D cvpt = findFirstPoint(pointList);
		Point2D cvpt1 = cvpt;
		boolean flag = true;
		int i, j;
		cvList = new Vector();
		cvList.addElement(cvpt);
		do {
			for (i = 0; i < pointList.size(); i++) {
				flag = true;
				x1 = cvpt.getX();
				y1 = cvpt.getY();
				x2 = ((Point2D) pointList.elementAt(i)).getX();
				y2 = ((Point2D) pointList.elementAt(i)).getY();
				if ((x1 == x2) && (y1 == y2))
					continue;
				for (j = 0; j < pointList.size(); j++) {
					x3 = ((Point2D) pointList.elementAt(j)).getX();
					y3 = ((Point2D) pointList.elementAt(j)).getY();
					if ((x3 == x2) && (y3 == y2))
						continue;
					if ((x3 == x1) && (y3 == y1))
						continue;
					double det = x1 * (y2 - y3) - y1 * (x2 - x3)
							+ (y3 * x2 - y2 * x3);
					if (det > 0.0) {
						flag = false;
						break;
					}
				} // end of j loop
				if (flag) {
					cvpt = (Point2D) pointList.elementAt(i);
					cvList.addElement(cvpt);
					break;
				}
			} // end of i loop
		} while (!((cvpt.getX() == cvpt1.getX()) && (cvpt.getY() == cvpt1
				.getY())));// end of do
		// last point is a repeat of first, so remove it
		cvList.removeElementAt(cvList.size() - 1);
	}

	public void listCVHullPts() {
		for (int i = 0; i < cvList.size(); i++) {
			Point2D cvpt = (Point2D) cvList.elementAt(i);
			// System.out.println("X: "+cvpt.getX()+"   Y: "+cvpt.getY());
		}
	}

	public void cvHullToFile(String outfileName) {
		PrintWriter out = null;
		DecimalFormat myFormatter = new DecimalFormat("##0.00000");

		try {
			out = new PrintWriter(new FileOutputStream(outfileName));
			for (int i = 0; i < cvList.size(); i++) {
				Point2D cvpt = (Point2D) cvList.elementAt(i);
				double xval = cvpt.getX();
				out.print(" " + myFormatter.format(xval) + "  ");
				double yval = cvpt.getY();
				out.print(myFormatter.format(yval));
				out.println();
			}
		} catch (Exception e) {
			System.out.println("Problem writing CHull output file!");
		}
		out.close();
	}

	public void cvScaledHullToFile(double scalefactor, String outfileName) {
		createScaledShape(scalefactor);
		PrintWriter out = null;
		DecimalFormat myFormatter = new DecimalFormat("##0.00000");

		try {
			out = new PrintWriter(new FileOutputStream(outfileName));
			for (int i = 0; i < scvList.size(); i++) {
				Point2D cvpt = (Point2D) scvList.elementAt(i);
				double xval = cvpt.getX();
				out.print(" " + myFormatter.format(xval) + "  ");
				double yval = cvpt.getY();
				out.print(myFormatter.format(yval));
				out.println();
			}
		} catch (Exception e) {
			System.out.println("Problem writing ScaledCHull output file!");
		}
		out.close();
	}

	public Shape createShape() {
		GeneralPath gp = new GeneralPath();
		Point2D cvpt = (Point2D) cvList.elementAt(0);
		gp.moveTo((float) (cvpt.getX()), (float) (cvpt.getY()));
		for (int i = 1; i < cvList.size(); i++) {
			cvpt = (Point2D) cvList.elementAt(i);
			gp.lineTo((float) (cvpt.getX()), (float) (cvpt.getY()));
		}
		gp.closePath();
		return gp;
	}

	public Shape createScaledShape(double scalefactor) {
		Shape initshp = createShape();
		// find center
		double xcen = initshp.getBounds2D().getCenterX();
		double ycen = initshp.getBounds2D().getCenterY();
		AffineTransform at = AffineTransform.getTranslateInstance(-1.0 * xcen,
				-1.0 * ycen);
		Shape sh1 = at.createTransformedShape(initshp);
		at = AffineTransform.getScaleInstance(scalefactor, scalefactor);
		Shape sh2 = at.createTransformedShape(sh1);
		at = AffineTransform.getTranslateInstance(xcen, ycen);
		Shape sh3 = at.createTransformedShape(sh2);

		// get the scaled points
		scvList = new Vector();
		PathIterator pi = sh3.getPathIterator(null);
		while (pi.isDone() == false) {
			double[] coords = new double[6];
			int type = pi.currentSegment(coords);
			// System.out.println("x: "+coords[0] + "  y: "+coords[1]);
			java.awt.geom.Point2D.Double pt = new java.awt.geom.Point2D.Double(
					coords[0], coords[1]);
			scvList.addElement(pt);
			pi.next();
		}
		scvList.removeElementAt(scvList.size() - 1);
		return sh3;
	}

	public static void main(String[] args) {
		File df = new File("dataPoints.txt");
		// System.out.println("File: "+ df);
		long start = System.currentTimeMillis();
		ConvexHull ch = new ConvexHull(df);
		ch.cvHullToFile("HullPoints.txt");
		long stop = System.currentTimeMillis();
		ch.listCVHullPts();
		Shape cvshape = ch.createShape();
		System.out.println("Bounding Box: " + cvshape.getBounds2D());
		System.out.println("Is (0, 0) inside? : " + cvshape.contains(0.0, 0.0));
		System.out.println("Is (-50, -10) inside? : "
				+ cvshape.contains(-50.0, -10.0));
		Shape shx2 = ch.createScaledShape(2.0);
		System.out.println("Scaled Bounding Box: " + shx2.getBounds2D());

		System.out.println("Time(ms): " + (stop - start));
	}

	private Vector getPoints(BufferedReader br) {
		String cachedLine = "";
		double xval = 0.0;
		double yval = 0.0;
		Vector pts = new Vector();
		// unsure exactly how many point lines there are
		// but each line should have only two tokens
		boolean eoh = false;
		while (!eoh) {
			try {
				cachedLine = br.readLine();
				// System.out.println("cachedLine: "+cachedLine);
			} catch (Exception w) {
				System.out.println("error reading next line in points file!");
				eoh = true;
			}
			if (cachedLine == null)
				return pts;
			if (cachedLine.trim().length() > 0) {
				StringTokenizer st = new StringTokenizer(cachedLine);
				int cnt = st.countTokens(); // should be only 2
				if (cnt != 2)
					eoh = true;
				String firstToken = st.nextToken().trim();
				String secondToken = st.nextToken().trim();
				try {
					xval = java.lang.Double.parseDouble(firstToken);
					yval = java.lang.Double.parseDouble(secondToken);
				} catch (Exception e) {
					eoh = true;
				}
				if (!eoh) {

					java.awt.geom.Point2D.Double pt2D = new java.awt.geom.Point2D.Double(
							xval, yval);
					pts.addElement(pt2D);
				}
			}
		}
		return pts;
	}

	private Point2D findFirstPoint(Vector pts) {
		double minx;
		Point2D fp = (Point2D) pts.elementAt(0);
		minx = fp.getX();
		for (int i = 1; i < pts.size(); i++) {
			double tempx = ((Point2D) pts.elementAt(i)).getX();
			// System.out.println("i: "+i+"    tempx: "+tempx+
			// "    minx: "+minx);
			if (tempx < minx) {
				fp = (Point2D) pts.elementAt(i);
				minx = tempx;
			}
		}
		// System.out.println("MinX: "+fp.getX()+"   MinY: "+fp.getY());
		return fp;
	}
}