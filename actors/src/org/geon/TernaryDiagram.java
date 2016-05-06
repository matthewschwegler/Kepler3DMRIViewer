/*
 * Copyright (c) 2002-2010 The Regents of the University of California.
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

import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.PrintWriter;

//////////////////////////////////////////////////////////////////////////
////TernaryDiagram
/**
 * This actor reads a string and outputs an array of coordinates and a string of
 * region.
 * 
 * @author Efrat Jaeger
 * @version $Id: TernaryDiagram.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class TernaryDiagram {

	// parameters for the diagram size
	final public static double width = 420;
	final public static double height = 700;
	final public static double leftPadding = 40;
	final public static double rightPadding = 40;
	final public static double diagramWidth = width - leftPadding
			- rightPadding;
	final public static double triangleDiagramHeight = (diagramWidth * Math
			.sin(Math.PI / 3));
	final public static double diagramHeight = triangleDiagramHeight * 2;
	final public static double bottomPadding = 80;
	final public static double topPadding = height - diagramHeight
			- bottomPadding;

	public static boolean fourVertices = false;

	private static String getSVGHeader(String topVertex, String leftVertex,
			String rightVertex, String bottomVertex) {

		// get header and scripts
		String svg = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"no\"?>\n";
		svg += "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 20010904//EN\"\n";
		svg += "      \"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">\n";

		svg += "<svg xmlns=\"http://www.w3.org/2000/svg\"\n"
				+ "     xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n"
				+ "     width='" + width + "' height='" + height + "'"
				+ ">\n\n";// not sure about the height..

		svg += "<script type=\"text/ecmascript\">\n\n";

		svg += "<![CDATA[\n";

		svg += "     function show(tid) {\n";
		svg += "         if (lastMsg != null) {\n";
		svg += "            lastMsg.setAttribute(\"visibility\", \"hidden\");\n";
		svg += "         }\n";
		svg += "         obj = svgDocument.getElementById(tid);\n";
		svg += "         obj.setAttribute(\"visibility\", \"visible\");\n";
		svg += "         obj.setAttribute(\"fill\", \"navy\");\n";
		svg += "         obj = svgDocument.getElementById(tid+\"Polygon\");\n";
		svg += "         obj.setAttribute(\"fill\", \"#5f7f8f\");\n";
		svg += "     }\n";

		svg += "     function unshow(tid) {\n";
		svg += "         obj = svgDocument.getElementById(tid);\n";
		svg += "         obj.setAttribute(\"visibility\", \"hidden\");\n";
		svg += "         obj = svgDocument.getElementById(tid+\"Polygon\");\n";
		svg += "         obj.setAttribute(\"fill\", \"white\");\n";
		svg += "         if (lastMsg != null) {\n";
		svg += "            lastMsg.setAttribute(\"fill\", \"brown\");\n";
		svg += "            lastMsg.setAttribute(\"visibility\", \"visible\");\n";
		svg += "         }\n";
		svg += "     }\n";

		svg += "     var lastCircle;\n";
		svg += "     var lastMsg; \n";

		svg += "// ]]>\n\n";
		svg += "</script>\n\n";

		String top = (topVertex != null) ? topVertex : "NULL";
		String left = (leftVertex != null) ? leftVertex : "NULL";
		String right = (rightVertex != null) ? rightVertex : "NULL";
		String bot = (bottomVertex != null) ? bottomVertex : "NULL";

		svg += "<text id='properties' visibility = 'hidden' ";
		svg += "properties='top " + top + " left " + left + " right " + right
				+ " bot " + bot + " ";
		svg += "leftPadding " + leftPadding + " topPadding " + topPadding
				+ " diagramWidth " + diagramWidth + " ";
		svg += "triangleDiagramHeight " + triangleDiagramHeight + "' />\n";

		svg += "<rect x='0' y='0' rx='5' ry='5' width='" + width + "' height='"
				+ height + "' fill='#f4f4f4' />\n";

		// draw labels
		if (topVertex != null) {
			svg += "<text x='"
					+ (width / 2 - 15)
					+ "' y='"
					+ (topPadding - 10)
					+ "' style='font-weight:bold; font-size: 12pt; font-family: serif; ' >"
					+ topVertex + "</text>\n";
		}

		if (leftVertex != null) {
			svg += "<text x='"
					+ (leftPadding - 30)
					+ "' y='"
					+ (height - (bottomPadding + triangleDiagramHeight))
					+ "' style='font-weight:bold; font-size: 12pt; font-family: serif; ' >"
					+ leftVertex + "</text>\n";
		}

		if (rightVertex != null) {
			svg += "<text x='"
					+ (width - rightPadding + 5)
					+ "' y='"
					+ (height - (bottomPadding + triangleDiagramHeight))
					+ "' style='font-weight:bold; font-size: 12pt; font-family: serif; ' >"
					+ rightVertex + "</text>\n";
		}

		if (bottomVertex != null) {
			svg += "<text x='"
					+ (width / 2 - 15)
					+ "' y='"
					+ (height - bottomPadding + 15)
					+ "' style='font-weight:bold; font-size: 12pt; font-family: serif; ' >"
					+ bottomVertex + "</text>\n";
			fourVertices = true;
		}

		return svg;
	}

	private static String getSVGFooter() {
		String svg = "</svg>\n";
		return svg;
	}

	private static String getLabelInSVG(String label, String msg) {
		double textY = (fourVertices) ? (height - bottomPadding + 20) : (height
				- bottomPadding + 20 - triangleDiagramHeight);
		String svg = "<text id='"
				+ label
				+ "' x='"
				+ (width / 2)
				+ "' y='"
				+ textY
				+ "' fill='brown' text-anchor='middle' visibility='hidden' "
				+ "style='font-weight:bold; font-size: 12pt; font-family: serif; ' >\n";
		svg += msg;
		svg += "</text>\n";
		return svg;
	}

	public static String getPlagPxOlDiagraminSVG() {

		String svg = getSVGHeader("Plag", "Px", "Ol", null);

		// draw top triangle
		double y1 = topPadding + triangleDiagramHeight / 10;
		double x1 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		double x2 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		svg += "<polygon id='anorthositePolygon' points='" + x1 + "," + y1
				+ " " + x2 + "," + y1 + " " + (width / 2) + "," + topPadding
				+ "'";
		svg += " onmouseover='show(\"anorthosite\")' onmouseout='unshow(\"anorthosite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("anorthosite", "anorthosite");

		// draw bottom polygon
		double y2 = topPadding + triangleDiagramHeight * 9 / 10;
		double x3 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);
		double x4 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);
		double x5 = (x1 + x2) / 2;
		svg += "<polygon id='ultramaficPolygon' points='" + (leftPadding) + ","
				+ (topPadding + triangleDiagramHeight) + " " + x3 + "," + y2
				+ " " + x4 + "," + y2 + " " + (width - rightPadding) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"ultramafic\")' onmouseout='unshow(\"ultramafic\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("ultramafic", "ultramafic");

		// draw the left polygon
		svg += "<polygon id='noritePolygon' points='" + x1 + "," + y1 + " "
				+ x5 + "," + y1 + " " + (x3 + x5 - x1) + "," + y2 + " " + x3
				+ "," + y2 + "'";
		svg += " onmouseover='show(\"norite\")' onmouseout='unshow(\"norite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("norite", "norite");

		// draw the right polygon
		svg += "<polygon id='troctolitePolygon' points='" + x5 + "," + y1 + " "
				+ x2 + "," + y1 + " " + x4 + "," + y2 + " " + (x4 - x5 + x1)
				+ "," + y2 + "'";
		svg += " onmouseover='show(\"troctolite\")' onmouseout='unshow(\"troctolite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("troctolite", "troctolite");

		// draw the center polygon
		svg += "<polygon id='gabbroPolygon' points='" + x5 + "," + y1 + " "
				+ (x3 + x5 - x1) + "," + y2 + " " + (x4 - x5 + x1) + "," + y2
				+ "'";
		svg += " onmouseover='show(\"gabbro\")' onmouseout='unshow(\"gabbro\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("gabbro", "gabbro");

		svg += getSVGFooter();
		return svg;

	}

	public static String getPlagPxHblDiagraminSVG() {

		String svg = getSVGHeader("Plag", "Px", "Hbl", null);

		// draw top triangle
		double y1 = topPadding + triangleDiagramHeight / 10;
		double x1 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		double x2 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		svg += "<polygon id='anorthositePolygon' points='" + x1 + "," + y1
				+ " " + x2 + "," + y1 + " " + (width / 2) + "," + topPadding
				+ "'";
		svg += " onmouseover='show(\"anorthosite\")' onmouseout='unshow(\"anorthosite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("anorthosite", "anorthosite");

		// draw the left polygon
		double y2 = topPadding + triangleDiagramHeight * 9 / 10;
		double x3 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);
		double x4 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);
		double x5 = (x1 + x2) / 2;
		svg += "<polygon id='noritePolygon' points='" + x1 + "," + y1 + " "
				+ x5 + "," + y1 + " " + (x3 + x5 - x1) + "," + y2 + " " + x3
				+ "," + y2 + "'";
		svg += " onmouseover='show(\"norite\")' onmouseout='unshow(\"norite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("norite", "norite");

		// draw the right polygon
		svg += "<polygon id='hornblendePolygon' points='" + x5 + "," + y1 + " "
				+ x2 + "," + y1 + " " + x4 + "," + y2 + " " + (x4 - x5 + x1)
				+ "," + y2 + "'";
		svg += " onmouseover='show(\"hornblende\")' onmouseout='unshow(\"hornblende\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("hornblende", "hornblendite gabbro");

		// draw the center polygon
		svg += "<polygon id='gabbroPolygon' points='" + x5 + "," + y1 + " "
				+ (x3 + x5 - x1) + "," + y2 + " " + (x4 - x5 + x1) + "," + y2
				+ "'";
		svg += " onmouseover='show(\"gabbro\")' onmouseout='unshow(\"gabbro\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("gabbro", "gabbro");

		// draw bottom1 polygon
		svg += "<polygon id='pyroxenitePolygon' points='" + x3 + "," + y2 + " "
				+ leftPadding + "," + (topPadding + triangleDiagramHeight)
				+ " " + (leftPadding + (x2 - x1)) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"pyroxenite\")' onmouseout='unshow(\"pyroxenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("pyroxenite", "pyroxenite");

		// bottom2
		svg += "<polygon id='hblpyroxenitePolygon' points='" + x3 + "," + y2
				+ " " + (leftPadding + (x2 - x1)) + ","
				+ (topPadding + triangleDiagramHeight) + " "
				+ (leftPadding + diagramWidth / 2) + ","
				+ (topPadding + triangleDiagramHeight) + " "
				+ (leftPadding + diagramWidth / 2) + "," + y2 + "'";
		svg += " onmouseover='show(\"hblpyroxenite\")' onmouseout='unshow(\"hblpyroxenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("hblpyroxenite", "hornblende pyroxenite");

		// pyroxenite-hornblendite
		svg += "<polygon id='pxhblPolygon' points='"
				+ (leftPadding + diagramWidth / 2) + ","
				+ (topPadding + triangleDiagramHeight) + " "
				+ (leftPadding + diagramWidth / 2) + "," + y2 + " " + x4 + ","
				+ y2 + " " + (width - rightPadding - (x2 - x1)) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"pxhbl\")' onmouseout='unshow(\"pxhbl\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("pxhbl", "pyroxenite hornblendite");

		// hornblendite
		svg += "<polygon id='hornblenditePolygon' points='"
				+ (width - rightPadding - (x2 - x1)) + ","
				+ (topPadding + triangleDiagramHeight) + " " + x4 + "," + y2
				+ " " + (width - rightPadding) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"hornblendite\")' onmouseout='unshow(\"hornblendite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("hornblendite", "hornblendite");

		svg += getSVGFooter();
		return svg;

	}

	public static String getPlagOpxCpxDiagraminSVG() {

		String svg = getSVGHeader("Plag", "Opx", "Cpx", null);

		// draw top triangle
		double y1 = topPadding + triangleDiagramHeight / 10;
		double x1 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		double x2 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		svg += "<polygon id='anorthositePolygon' points='" + x1 + "," + y1
				+ " " + x2 + "," + y1 + " " + (width / 2) + "," + topPadding
				+ "'";
		svg += " onmouseover='show(\"anorthosite\")' onmouseout='unshow(\"anorthosite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("anorthosite", "anorthosite");

		// draw bottom polygon
		double y2 = topPadding + triangleDiagramHeight * 9 / 10;
		double x3 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);
		double x4 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);
		double x5 = (x1 + x2) / 2;
		svg += "<polygon id='ultramaficPolygon' points='" + (leftPadding) + ","
				+ (topPadding + triangleDiagramHeight) + " " + x3 + "," + y2
				+ " " + x4 + "," + y2 + " " + (width - rightPadding) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"ultramafic\")' onmouseout='unshow(\"ultramafic\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("ultramafic", "pyroxenite");

		// draw the left polygon
		svg += "<polygon id='noritePolygon' points='" + x1 + "," + y1 + " "
				+ x5 + "," + y1 + " " + (x3 + x5 - x1) + "," + y2 + " " + x3
				+ "," + y2 + "'";
		svg += " onmouseover='show(\"norite\")' onmouseout='unshow(\"norite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("norite", "norite");

		// draw the right polygon
		svg += "<polygon id='troctolitePolygon' points='" + x5 + "," + y1 + " "
				+ x2 + "," + y1 + " " + x4 + "," + y2 + " " + (x4 - x5 + x1)
				+ "," + y2 + "'";
		svg += " onmouseover='show(\"troctolite\")' onmouseout='unshow(\"troctolite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("troctolite", "gabbro");

		// draw the center1 polygon
		svg += "<polygon id='clinopyroxenePolygon' points='" + x5 + "," + y1
				+ " " + (x3 + x5 - x1) + "," + y2 + " " + (width / 2) + ","
				+ y2 + "'";
		svg += " onmouseover='show(\"clinopyroxene\")' onmouseout='unshow(\"clinopyroxene\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("clinopyroxene", "clinopyroxene norite");

		// draw the center2 polygon
		svg += "<polygon id='orthopyroxenePolygon' points='" + x5 + "," + y1
				+ " " + (width / 2) + "," + y2 + " " + (x4 - x5 + x1) + ","
				+ y2 + "'";
		svg += " onmouseover='show(\"orthopyroxene\")' onmouseout='unshow(\"orthopyroxene\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("orthopyroxene", "orthopyroxene gabbro");

		svg += getSVGFooter();
		return svg;

	}

	public static String getQAPFDiagraminSVG() {

		String svg = getSVGHeader("Q", "A", "P", "F");
		// top half
		// draw top triangle
		double y1 = topPadding + triangleDiagramHeight / 10;
		double x1 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		double x2 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		svg += "<polygon id='quartzolitePolygon' points='" + x1 + "," + y1
				+ " " + x2 + "," + y1 + " " + (width / 2) + "," + topPadding
				+ "'";
		svg += " onmouseover='show(\"quartzolite\")' onmouseout='unshow(\"quartzolite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("quartzolite", "quartzolite");

		// draw second top polygon
		double y2 = topPadding + triangleDiagramHeight * 4 / 10;
		double x3 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight * 4 / 10);
		double x4 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight * 4 / 10);
		svg += "<polygon id='quartzrichPolygon' points='" + x1 + "," + y1 + " "
				+ x2 + "," + y1 + " " + x4 + "," + y2 + " " + x3 + "," + y2
				+ "'";
		svg += " onmouseover='show(\"quartzrich\")' onmouseout='unshow(\"quartzrich\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("quartzrich", "quartz rich granitic");

		// draw third level polygons
		double y3 = topPadding + triangleDiagramHeight * 8 / 10;
		double level2length = 2 * (Math.tan(Math.PI / 6)
				* triangleDiagramHeight * 4 / 10);
		double x5 = x3 + level2length / 10;
		double x6 = x5 + level2length / 4;
		double x8 = x4 - level2length / 10;
		double x7 = x8 - level2length / 4;
		double level3length = (Math.tan(Math.PI / 6) * triangleDiagramHeight
				* 8 / 10) * 2;
		double x9 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight * 8 / 10);
		double x10 = x9 + level3length / 10;
		double x11 = x10 + level3length / 4;
		double x14 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight * 8 / 10);
		double x13 = x14 - level3length / 10;
		double x12 = x13 - level3length / 4;

		// leftmost polygon
		svg += "<polygon id='alkfeldgranitePolygon' points='" + x3 + "," + y2
				+ " " + x5 + "," + y2 + " " + x10 + "," + y3 + " " + x9 + ","
				+ y3 + "'";
		svg += " onmouseover='show(\"alkfeldgranite\")' onmouseout='unshow(\"alkfeldgranite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("alkfeldgranite", "alkali feldspar granite");

		// second leftmost polygon
		svg += "<polygon id='syenogranitePolygon' points='" + x5 + "," + y2
				+ " " + x6 + "," + y2 + " " + x11 + "," + y3 + " " + x10 + ","
				+ y3 + "'";
		svg += " onmouseover='show(\"syenogranite\")' onmouseout='unshow(\"syenogranite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("syenogranite", "syeno granite");

		// center polygon
		svg += "<polygon id='monzogranitePolygon' points='" + x6 + "," + y2
				+ " " + x7 + "," + y2 + " " + x12 + "," + y3 + " " + x11 + ","
				+ y3 + "'";
		svg += " onmouseover='show(\"monzogranite\")' onmouseout='unshow(\"monzogranite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("monzogranite", "monzo granite");

		// second rightmost polygon
		svg += "<polygon id='granodioritePolygon' points='" + x7 + "," + y2
				+ " " + x8 + "," + y2 + " " + x13 + "," + y3 + " " + x12 + ","
				+ y3 + "'";
		svg += " onmouseover='show(\"granodiorite\")' onmouseout='unshow(\"granodiorite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("granodiorite", "granodiorite");

		// rightmost polygon
		svg += "<polygon id='tonalitePolygon' points='" + x8 + "," + y2 + " "
				+ x4 + "," + y2 + " " + x14 + "," + y3 + " " + x13 + "," + y3
				+ "'";
		svg += " onmouseover='show(\"tonalite\")' onmouseout='unshow(\"tonalite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("tonalite", "tonalite");

		// draw fourth level polygons
		double y4 = topPadding + triangleDiagramHeight * 19 / 20;
		double level4length = 2 * (Math.tan(Math.PI / 6)
				* triangleDiagramHeight * 19 / 20);
		double x15 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight * 19 / 20);
		double x16 = x15 + level4length / 10;
		double x17 = x16 + level4length / 4;
		double x20 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight * 19 / 20);
		double x19 = x20 - level4length / 10;
		double x18 = x19 - level4length / 4;

		// leftmost polygon
		svg += "<polygon id='qrzalkfeldsyenitePolygon' points='" + x9 + ","
				+ y3 + " " + x10 + "," + y3 + " " + x16 + "," + y4 + " " + x15
				+ "," + y4 + "'";
		svg += " onmouseover='show(\"qrzalkfeldsyenite\")' onmouseout='unshow(\"qrzalkfeldsyenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("qrzalkfeldsyenite",
				"quartz alkali feldspar syenite");

		// second leftmost polygon
		svg += "<polygon id='quartzsyenitePolygon' points='" + x10 + "," + y3
				+ " " + x11 + "," + y3 + " " + x17 + "," + y4 + " " + x16 + ","
				+ y4 + "'";
		svg += " onmouseover='show(\"quartzsyenite\")' onmouseout='unshow(\"quartzsyenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("quartzsyenite", "quartz syenite");

		// center polygon
		svg += "<polygon id='quartzmonzonitePolygon' points='" + x11 + "," + y3
				+ " " + x12 + "," + y3 + " " + x18 + "," + y4 + " " + x17 + ","
				+ y4 + "'";
		svg += " onmouseover='show(\"quartzmonzonite\")' onmouseout='unshow(\"quartzmonzonite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("quartzmonzonite", "quartz monzonite");

		// second rightmost polygon
		svg += "<polygon id='quartzmonzoPolygon' points='" + x12 + "," + y3
				+ " " + x13 + "," + y3 + " " + x19 + "," + y4 + " " + x18 + ","
				+ y4 + "'";
		svg += " onmouseover='show(\"quartzmonzo\")' onmouseout='unshow(\"quartzmonzo\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("quartzmonzo",
				"quartz monzodiorite quartz monzogabbro");

		// rightmost polygon
		svg += "<polygon id='quartzPolygon' points='" + x13 + "," + y3 + " "
				+ x14 + "," + y3 + " " + x20 + "," + y4 + " " + x19 + "," + y4
				+ "'";
		svg += " onmouseover='show(\"quartz\")' onmouseout='unshow(\"quartz\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("quartz",
				"quartz diorite quartz gabbro quartz anorthosite");

		// draw fifth level polygons
		double y5 = topPadding + triangleDiagramHeight;
		double level5length = 2 * (Math.tan(Math.PI / 6) * triangleDiagramHeight);
		double b1 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight);
		double b2 = b1 + level5length / 10;
		double b3 = b2 + level5length / 4;
		double b6 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight);
		double b5 = b6 - level5length / 10;
		double b4 = b5 - level5length / 4;

		// leftmost polygon
		svg += "<polygon id='alkfeldsyenitePolygon' points='" + x15 + "," + y4
				+ " " + x16 + "," + y4 + " " + b2 + "," + y5 + " " + b1 + ","
				+ y5 + "'";
		svg += " onmouseover='show(\"alkfeldsyenite\")' onmouseout='unshow(\"alkfeldsyenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("alkfeldsyenite", "alkali feldspar syenite");

		// second leftmost polygon
		svg += "<polygon id='syenitePolygon' points='" + x16 + "," + y4 + " "
				+ x17 + "," + y4 + " " + b3 + "," + y5 + " " + b2 + "," + y5
				+ "'";
		svg += " onmouseover='show(\"syenite\")' onmouseout='unshow(\"syenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("syenite", "syenite");

		// center polygon
		svg += "<polygon id='monzonitePolygon' points='" + x17 + "," + y4 + " "
				+ x18 + "," + y4 + " " + b4 + "," + y5 + " " + b3 + "," + y5
				+ "'";
		svg += " onmouseover='show(\"monzonite\")' onmouseout='unshow(\"monzonite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("monzonite", "monzonite");

		// second rightmost polygon
		svg += "<polygon id='monzoPolygon' points='" + x18 + "," + y4 + " "
				+ x19 + "," + y4 + " " + b5 + "," + y5 + " " + b4 + "," + y5
				+ "'";
		svg += " onmouseover='show(\"monzo\")' onmouseout='unshow(\"monzo\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("monzo", "monzodiorite monzogabbro");

		// rightmost polygon
		svg += "<polygon id='diogabanorthPolygon' points='" + x19 + "," + y4
				+ " " + x20 + "," + y4 + " " + b6 + "," + y5 + " " + b5 + ","
				+ y5 + "'";
		svg += " onmouseover='show(\"diogabanorth\")' onmouseout='unshow(\"diogabanorth\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("diogabanorth", "diorite gabbro anorthosite");

		// bottom half
		// draw third level polygons
		double y6 = topPadding + triangleDiagramHeight * 11 / 10;
		double level6length = 2 * (Math.tan(Math.PI / 6)
				* triangleDiagramHeight * 9 / 10);
		double b7 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);
		double b8 = b7 + level5length / 10;
		double b9 = b8 + level5length / 4;
		double b12 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);
		double b11 = b12 - level5length / 10;
		double b10 = b11 - level5length / 4;

		// leftmost polygon
		svg += "<polygon id='foidbearalkfeldsyenitePolygon' points='" + b1
				+ "," + y5 + " " + b2 + "," + y5 + " " + b8 + "," + y6 + " "
				+ b7 + "," + y6 + "'";
		svg += " onmouseover='show(\"foidbearalkfeldsyenite\")' onmouseout='unshow(\"foidbearalkfeldsyenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foidbearalkfeldsyenite",
				"foid bearing alkali feldspar syenite");

		// second leftmost polygon
		svg += "<polygon id='foidbearsyenitePolygon' points='" + b2 + "," + y5
				+ " " + b3 + "," + y5 + " " + b9 + "," + y6 + " " + b8 + ","
				+ y6 + "'";
		svg += " onmouseover='show(\"foidbearsyenite\")' onmouseout='unshow(\"foidbearsyenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foidbearsyenite", "foid bearing syenite");

		// center polygon
		svg += "<polygon id='foidbearmonzonitePolygon' points='" + b3 + ","
				+ y5 + " " + b4 + "," + y5 + " " + b10 + "," + y6 + " " + b9
				+ "," + y6 + "'";
		svg += " onmouseover='show(\"foidbearmonzonite\")' onmouseout='unshow(\"foidbearmonzonite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foidbearmonzonite", "foid bearing monzonite");

		// second rightmost polygon
		svg += "<polygon id='foidbearmonzoPolygon' points='" + b4 + "," + y5
				+ " " + b5 + "," + y5 + " " + b11 + "," + y6 + " " + b10 + ","
				+ y6 + "'";
		svg += " onmouseover='show(\"foidbearmonzo\")' onmouseout='unshow(\"foidbearmonzo\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foidbearmonzo",
				"foid bearing monzodiorite foid bearing monzogabbro");

		// rightmost polygon
		svg += "<polygon id='foidbeardiogabanorthPolygon' points='" + b5 + ","
				+ y5 + " " + b6 + "," + y5 + " " + b12 + "," + y6 + " " + b11
				+ "," + y6 + "'";
		svg += " onmouseover='show(\"foidbeardiogabanorth\")' onmouseout='unshow(\"foidbeardiogabanorth\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foidbeardiogabanorth",
				"foid bearing diorite foid bearing gabbro foid bearing anorthosite");

		// draw third level polygons
		double y7 = topPadding + triangleDiagramHeight * 16 / 10;

		// leftmost polygon
		svg += "<polygon id='foidsyenitePolygon' points='" + b7 + "," + y6
				+ " " + b8 + "," + y6 + " " + x5 + "," + y7 + " " + x3 + ","
				+ y7 + "'";
		svg += " onmouseover='show(\"foidsyenite\")' onmouseout='unshow(\"foidsyenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foidsyenite", "foid syenite");

		// second leftmost polygon
		svg += "<polygon id='foidmonzosyenitePolygon' points='" + b8 + "," + y6
				+ " " + (width / 2) + "," + y6 + " " + (width / 2) + "," + y7
				+ " " + x5 + "," + y7 + "'";
		svg += " onmouseover='show(\"foidmonzosyenite\")' onmouseout='unshow(\"foidmonzosyenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foidmonzosyenite", "foidmonzo syenite");

		// second rightmost polygon
		svg += "<polygon id='foidmonzoPolygon' points='" + (width / 2) + ","
				+ y6 + " " + b11 + "," + y6 + " " + x8 + "," + y7 + " "
				+ (width / 2) + "," + y7 + "'";
		svg += " onmouseover='show(\"foidmonzo\")' onmouseout='unshow(\"foidmonzo\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foidmonzo", "foid monzosyenite foid gabbro");

		// rightmost polygon
		svg += "<polygon id='foiddiogabPolygon' points='" + b11 + "," + y6
				+ " " + b12 + "," + y6 + " " + x4 + "," + y7 + " " + x8 + ","
				+ y7 + "'";
		svg += " onmouseover='show(\"foiddiogab\")' onmouseout='unshow(\"foiddiogab\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foiddiogab", "foid diorite foid gabbro");

		// draw bottom triangle.
		double ybot = topPadding + diagramHeight;

		svg += "<polygon id='foidolitePolygon' points='" + x3 + "," + y7 + " "
				+ x4 + "," + y7 + " " + (width / 2) + "," + ybot + "'";
		svg += " onmouseover='show(\"foidolite\")' onmouseout='unshow(\"foidolite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("foidolite", "foidolite");

		svg += getSVGFooter();
		return svg;

	}

	public static String getOlOpxCpxDiagraminSVG() {

		String svg = getSVGHeader("Ol", "Opx", "Cpx", null);

		// top half
		// draw top triangle
		double y1 = topPadding + triangleDiagramHeight / 10;
		double x1 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		double x2 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight / 10);
		svg += "<polygon id='dunitePolygon' points='" + x1 + "," + y1 + " "
				+ x2 + "," + y1 + " " + (width / 2) + "," + topPadding + "'";
		svg += " onmouseover='show(\"dunite\")' onmouseout='unshow(\"dunite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("dunite", "dunite");

		// draw the left polygon
		double y2 = topPadding + triangleDiagramHeight * 6 / 10;
		double x3 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight * 6 / 10);
		double x4 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight * 6 / 10);
		double x5 = (x1 + x2) / 2;
		svg += "<polygon id='harzburitePolygon' points='" + x1 + "," + y1 + " "
				+ x5 + "," + y1 + " " + (x3 + x5 - x1) + "," + y2 + " " + x3
				+ "," + y2 + "'";
		svg += " onmouseover='show(\"harzburite\")' onmouseout='unshow(\"harzburite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("harzburite", "harzburite");

		// draw the right polygon
		svg += "<polygon id='wehrlitePolygon' points='" + x5 + "," + y1 + " "
				+ x2 + "," + y1 + " " + x4 + "," + y2 + " " + (x4 - x5 + x1)
				+ "," + y2 + "'";
		svg += " onmouseover='show(\"wehrlite\")' onmouseout='unshow(\"wehrlite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("wehrlite", "wehrlite");

		// draw the center polygon
		svg += "<polygon id='lherzolitePolygon' points='" + x5 + "," + y1 + " "
				+ (x3 + x5 - x1) + "," + y2 + " " + (x4 - x5 + x1) + "," + y2
				+ "'";
		svg += " onmouseover='show(\"lherzolite\")' onmouseout='unshow(\"lherzolite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("lherzolite", "lherzolite");

		// bottom half
		// draw bottom left polygon
		double y3 = topPadding + triangleDiagramHeight * 9 / 10;
		double x6 = (width / 2)
				- (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);
		double x7 = (width / 2)
				+ (Math.tan(Math.PI / 6) * triangleDiagramHeight * 9 / 10);

		svg += "<polygon id='pyroxenitePolygon' points='" + x3 + "," + y2 + " "
				+ leftPadding + "," + (topPadding + triangleDiagramHeight)
				+ " " + (leftPadding + (x2 - x1)) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"pyroxenite\")' onmouseout='unshow(\"pyroxenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("pyroxenite", "pyroxenite");

		// bottom right polygon
		svg += "<polygon id='hblpyroxenitePolygon' points='" + x3 + "," + y2
				+ " " + (leftPadding + (x2 - x1)) + ","
				+ (topPadding + triangleDiagramHeight) + " "
				+ (leftPadding + diagramWidth / 2) + ","
				+ (topPadding + triangleDiagramHeight) + " "
				+ (leftPadding + diagramWidth / 2) + "," + y2 + "'";
		svg += " onmouseover='show(\"hblpyroxenite\")' onmouseout='unshow(\"hblpyroxenite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("hblpyroxenite", "hornblende pyroxenite");

		// pyroxenite-hornblendite
		svg += "<polygon id='pxhblPolygon' points='"
				+ (leftPadding + diagramWidth / 2) + ","
				+ (topPadding + triangleDiagramHeight) + " "
				+ (leftPadding + diagramWidth / 2) + "," + y2 + " " + x4 + ","
				+ y2 + " " + (width - rightPadding - (x2 - x1)) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"pxhbl\")' onmouseout='unshow(\"pxhbl\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("pxhbl", "pyroxenite hornblendite");

		// hornblendite
		svg += "<polygon id='hornblenditePolygon' points='"
				+ (width - rightPadding - (x2 - x1)) + ","
				+ (topPadding + triangleDiagramHeight) + " " + x4 + "," + y2
				+ " " + (width - rightPadding) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"hornblendite\")' onmouseout='unshow(\"hornblendite\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("hornblendite", "hornblendite");

		svg += getSVGFooter();
		return svg;

	}

	public static String getZrTid100Srd2DiagraminSVG() {
		Point2D.Double p;
		String svg = getSVGHeader("Ti/100", "Zr", "Sr/2", null);

		svg += "<polygon id='Zr-Ti/100-Sr/2' points='"
				+ (leftPadding + diagramWidth / 2) + "," + topPadding + " "
				+ leftPadding + "," + (topPadding + triangleDiagramHeight)
				+ " " + (leftPadding + diagramWidth) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"Zr-Ti/100-Sr/2 \")' onmouseout='unshow(\"Zr-Ti/100-Sr/2 \")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 33, 45, 22);
		double y1 = p.getY();
		double x1 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 46, 39, 15);
		double y2 = p.getY();
		double x2 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 54, 32, 14);
		double y3 = p.getY();
		double x3 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 52, 25, 23);
		double y4 = p.getY();
		double x4 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 35, 25, 40);
		double y5 = p.getY();
		double x5 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 27, 32, 41);
		double y6 = p.getY();
		double x6 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 26, 41, 33);
		double y7 = p.getY();
		double x7 = p.getX();

		// right polygon
		svg += "<polygon id='Ocean-floorCPolygon' points='" + x1 + "," + y1
				+ " " + x2 + "," + y2 + " " + x3 + "," + y3 + " " + x4 + ","
				+ y4 + " " + x5 + "," + y5 + " " + x6 + "," + y6 + " " + x7
				+ "," + y7 + "'";
		svg += " onmouseover='show(\"Ocean-floor C\")' onmouseout='unshow(\"Ocean-floor C\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("Ocean-floor C", "Ocean-floor C");

		// draw left polygon

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 20, 18, 62);
		double y8 = p.getY();
		double x8 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 16, 5, 79);
		double y9 = p.getY();
		double x9 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 11, 22, 67);
		double y10 = p.getY();
		double x10 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 15, 35, 50);
		double y11 = p.getY();
		double x11 = p.getX();

		svg += "<polygon id='Island-arcAPolygon' points='" + x7 + "," + y7
				+ " " + x6 + "," + y6 + " " + x5 + "," + y5 + " " + x8 + ","
				+ y8 + " " + x9 + "," + y9 + " " + x10 + "," + y10 + " " + x11
				+ "," + y11 + "'";
		svg += " onmouseover='show(\"Island-arc A\")' onmouseout='unshow(\"Island-arc A\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("Island-arc A", "Island-arc A");

		// draw bottom polygon

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 52, 11, 37);
		double y12 = p.getY();
		double x12 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 32, 6, 62);
		double y13 = p.getY();
		double x13 = p.getX();

		svg += "<polygon id='Calc-alkaliBPolygon' points='" + x5 + "," + y5
				+ " " + x4 + "," + y4 + " " + x12 + "," + y12 + " " + x13 + ","
				+ y13 + " " + x9 + "," + y9 + " " + x8 + "," + y8 + "'";
		svg += " onmouseover='show(\"Calc-alkali B\")' onmouseout='unshow(\"Calc-alkali B\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("Calc-alkali B", "Calc-alkali B");

		svg += getSVGFooter();
		return svg;

	}

	public static String getZrTid100Y3DiagraminSVG() {
		Point2D.Double p;
		String svg = getSVGHeader("Ti/100", "Zr", "Y*3", null);

		// The main Triangle
		svg += "<polygon id='Zr-Ti/100-Y*3' points='"
				+ (leftPadding + diagramWidth / 2) + "," + topPadding + " "
				+ leftPadding + "," + (topPadding + triangleDiagramHeight)
				+ " " + (leftPadding + diagramWidth) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"Zr-Ti/100-Y*3 \")' onmouseout='unshow(\"Zr-Ti/100-Y*3 \")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";

		// Top left polygon
		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 23, 48, 29);
		double y1 = p.getY();
		double x1 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 29, 42, 29);
		double y2 = p.getY();
		double x2 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 44, 30, 26);
		double y3 = p.getY();
		double x3 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 55, 24, 21);
		double y4 = p.getY();
		double x4 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 58, 29, 13);
		double y5 = p.getY();
		double x5 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 37, 50, 13);
		double y6 = p.getY();
		double x6 = p.getX();

		svg += "<polygon id='Within-plateDpolygon ' points ='" + x1 + "," + y1
				+ " " + x2 + "," + y2 + " " + x3 + "," + y3 + " " + x4 + ","
				+ y4 + " " + x5 + "," + y5 + " " + x6 + "," + y6 + "'";
		svg += " onmouseover='show(\"Within-plate DD\")' onmouseout='unshow(\"Within-plate DD\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("Within-plate DD", "Within-plate DD");

		// Top right polygon
		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 23, 48, 29);
		double y17 = p.getY();
		double x17 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 17, 42, 41);
		double y18 = p.getY();
		double x18 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 27, 28, 45);
		double y19 = p.getY();
		double x19 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 29, 42, 29);
		double y20 = p.getY();
		double x20 = p.getX();

		svg += "<polygon id='Island-arcA' points ='" + x17 + "," + y17 + " "
				+ x18 + "," + y18 + " " + x19 + "," + y19 + " " + x20 + ","
				+ y20 + "'";

		svg += " onmouseover='show(\"Islandic-arc A\")' onmouseout='unshow(\"Islandic-arc A\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("Islandic-arc A", "Islandic-arc A");

		// central polygon
		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 27, 28, 45);
		double y13 = p.getY();
		double x13 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 29, 42, 29);
		double y14 = p.getY();
		double x14 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 44, 30, 26);
		double y15 = p.getY();
		double x15 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 39, 20, 41);
		double y16 = p.getY();
		double x16 = p.getX();

		svg += "<polygon id='Ocean-floorB' points ='" + x13 + "," + y13 + " "
				+ x14 + "," + y14 + " " + x15 + "," + y15 + " " + x16 + ","
				+ y16 + "'";

		svg += " onmouseover='show(\"Ocean-floor B\")' onmouseout='unshow(\"Ocean-floor B\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("Ocean-floor B", "Ocean-floor B");

		// bottom right polygon
		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 44, 30, 26);
		double y7 = p.getY();
		double x7 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 39, 20, 41);
		double y8 = p.getY();
		double x8 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 54, 11, 35);
		double y9 = p.getY();
		double x9 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 60, 12, 28);
		double y10 = p.getY();
		double x10 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 61, 17, 22);
		double y11 = p.getY();
		double x11 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 55, 24, 21);
		double y12 = p.getY();
		double x12 = p.getX();

		svg += "<polygon id='Calc-alkaliBC' points ='" + x7 + "," + y7 + " "
				+ x8 + "," + y8 + " " + x9 + "," + y9 + " " + x10 + "," + y10
				+ " " + x11 + "," + y11 + " " + x12 + "," + y12 + "'";

		svg += " onmouseover='show(\"Calc-alkali B,C\")' onmouseout='unshow(\"Calc-alkali B,C\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("Calc-alkali B,C", "Calc-alkali B,C");

		svg += getSVGFooter();
		return svg;

	}

	public static String getThHfd3TaDiagraminSVG() {
		Point2D.Double p;
		String svg = getSVGHeader("Hf/3", "Th", "Ta", null);

		// The main Triangle
		svg += "<polygon id='Th-Hf/3-Ta' points='"
				+ (leftPadding + diagramWidth / 2) + "," + topPadding + " "
				+ leftPadding + "," + (topPadding + triangleDiagramHeight)
				+ " " + (leftPadding + diagramWidth) + ","
				+ (topPadding + triangleDiagramHeight) + "'";
		svg += " onmouseover='show(\"Th-Hf/3-Ta \")' onmouseout='unshow(\"Th-Hf/3-Ta \")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";

		System.out.println("Hf = {" + (leftPadding + diagramWidth / 2) + " , "
				+ topPadding + "}");
		System.out.println("Th = {" + leftPadding + " , "
				+ (topPadding + triangleDiagramHeight) + "}");

		// Top right polygon
		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 3, 88, 9);
		double y1 = p.getY();
		double x1 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 15, 77, 8);
		double y2 = p.getY();
		double x2 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 35, 46, 19);
		double y3 = p.getY();
		double x3 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 7, 65, 28);
		double y4 = p.getY();
		double x4 = p.getX();

		svg += "<polygon id='N-MORBpolygon' points ='" + x1 + "," + y1 + " "
				+ x2 + "," + y2 + " " + x3 + "," + y3 + " " + x4 + "," + y4
				+ "'";
		svg += " onmouseover='show(\"N-MORB\")' onmouseout='unshow(\"N-MORB\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("N-MORB", "N-MORB");

		// Top right polygon
		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 7, 65, 28);
		double y5 = p.getY();
		double x5 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 35, 46, 19);
		double y6 = p.getY();
		double x6 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 54, 25, 21);
		double y7 = p.getY();
		double x7 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 57, 20, 23);
		double y8 = p.getY();
		double x8 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 26, 34, 40);
		double y9 = p.getY();
		double x9 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 13, 42, 45);
		double y10 = p.getY();
		double x10 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 5, 55, 40);
		double y11 = p.getY();
		double x11 = p.getX();

		svg += "<polygon id='E-MORB' points ='" + x5 + "," + y5 + " " + x6
				+ "," + y6 + " " + x7 + "," + y7 + " " + x8 + "," + y8 + " "
				+ x9 + "," + y9 + " " + x10 + "," + y10 + " " + x11 + "," + y11
				+ "'";

		svg += " onmouseover='show(\"E-MORB\")' onmouseout='unshow(\"E-MORB\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("E-MORB", "E-MORB");

		// bottom polygon
		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 26, 34, 40);
		y11 = p.getY();
		x11 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 57, 20, 23);
		double y12 = p.getY();
		double x12 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 63, 8, 29);
		double y13 = p.getY();
		double x13 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 40, 12, 48);
		double y14 = p.getY();
		double x14 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 32, 20, 48);
		double y15 = p.getY();
		double x15 = p.getX();

		svg += "<polygon id='OIB-Rift' points ='" + x11 + "," + y11 + " " + x12
				+ "," + y12 + " " + x13 + "," + y13 + " " + x14 + "," + y14
				+ " " + x15 + "," + y15 + "'";

		svg += " onmouseover='show(\"OIB(Rift)\")' onmouseout='unshow(\"OIB(Rift)\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("OIB(Rift)", "OIB(Rift)");

		// bottom right polygon
		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 15, 85, 0);
		double y16 = p.getY();
		double x16 = p.getX();

		System.out.println("problemPoint = {" + x16 + " , " + y16 + "}");

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 15, 77, 8);
		double y17 = p.getY();
		double x17 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 23, 64, 13);
		double y18 = p.getY();
		double x18 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 55, 25, 20);
		double y19 = p.getY();
		double x19 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 95, 0, 5);
		double y20 = p.getY();
		double x20 = p.getX();

		p = getPoint(leftPadding, topPadding, diagramWidth,
				triangleDiagramHeight, 100, 0, 0);
		double y21 = p.getY();
		double x21 = p.getX();

		System.out.println("Th = {" + x21 + " , " + y21 + "}");

		svg += "<polygon id='Arc-basalts' points ='" + x16 + "," + y16 + " "
				+ x17 + "," + y17 + " " + x18 + "," + y18 + " " + x19 + ","
				+ y19 + " " + x20 + "," + y20 + " " + x21 + "," + y21 + "'";

		svg += " onmouseover='show(\"Arc-basalts\")' onmouseout='unshow(\"Arc-basalts\")' ";
		svg += " fill='white' style='stroke: black; stroke-width: 0.4; ' />\n";
		svg += getLabelInSVG("Arc-basalts", "Arc-basalts");

		svg += getSVGFooter();
		return svg;

	}

	public static Point2D.Double getPoint(double leftPadding,
			double topPadding, double diagramWidth, double diagramHeight,
			double zr, double ti, double sr) {
		double X = 0, Y = 0;

		double sum = zr + ti + sr;

		X = sr / sum;
		Y = ti / sum;

		double x1 = leftPadding + (X * diagramWidth);
		double y1 = topPadding + diagramHeight;

		double y0 = topPadding + ((1 - Y) * diagramHeight);
		double x0 = x1 + (y1 - y0) / (Math.tan(Math.PI / 6)); // divide by
																// tan(30)
																// instead of
																// sin(30)?

		// System.out.println("x1 = " + x1 + " , y1 = " + y1 + " , x0 = " + x0 +
		// " , y0 = " + y0);

		return new Point2D.Double(x0, y0);

	}

	static public void main(String args[]) throws Exception {
		// fourVertices = false;

		fourVertices = true;
		String svg1 = getQAPFDiagraminSVG();
		PrintWriter pw1 = new PrintWriter(new FileWriter(
				"C:/projects/kepler/lib/testdata/geon/QAPF.svg"));
		pw1.println(svg1);
		pw1.close();
		/*
		 * fourVertices = false; String svg2 = getThHfd3TaDiagraminSVG();
		 * PrintWriter pw2 = new PrintWriter(new
		 * FileWriter("ternarydiagrams/ThHfd3Ta.svg")); pw2.println(svg2);
		 * pw2.close();
		 */
	}

}