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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.skybound.awt.DoublePolygon;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ptolemy.actor.lib.Source;
import ptolemy.data.DoubleToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.ASTPtRootNode;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.ModelScope;
import ptolemy.data.expr.ParseTreeEvaluator;
import ptolemy.data.expr.PtParser;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.data.type.TypeConstant;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// SVGToPolygon
/**
 * This actor converts an SVG file into polygon objects. The polygon coordinates
 * are read using the polygon tag.
 * 
 * @author Efrat Jaeger
 * @version $Id: SVGToPolygon.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class SVGToPolygon extends Source {

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
	public SVGToPolygon(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		fileOrURL = new FileParameter(this, "fileOrURL");

		output.setTypeEquals(BaseType.GENERAL);
		trigger.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n"
				+ "<rect x=\"-25\" y=\"-20\" " + "width=\"50\" height=\"40\" "
				+ "style=\"fill:white\"/>\n"
				+ "<polygon points=\"-15,-10 -12,-10 -8,-14 -1,-14 3,-10"
				+ " 15,-10 15,10, -15,10\" " + "style=\"fill:red\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The file name or URL from which to read. This is a string with any form
	 * accepted by FileParameter.
	 * 
	 * @see FileParameter
	 */
	public FileParameter fileOrURL;

	/**
	 * Output the SVG file into a Polygons and their region names.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {
		if (trigger.getWidth() > 0) {
			String fileName = ((StringToken) trigger.get(0)).stringValue();
			int lineEndInd = fileName.indexOf("\n");
			if (lineEndInd != -1) { // The string contains a CR.
				fileName = fileName.substring(0, lineEndInd);
			}
			fileOrURL.setExpression(fileName.trim());
		}
		input = fileOrURL.asFile();

		GeonXMLUtil parseXML = new GeonXMLUtil();

		Vector polyPoints = parseXML.getAttrValue(input, "polygon", "points");
		Vector regionsStr = parseXML.getElementsById(input, "polygon",
				"onmouseover");

		// get conversion factors.
		xRatio = "";
		yRatio = ""; // reset factors first.
		_getConversionFactors(input);

		// calculate parse tree for ratios.
		PtParser parser = new PtParser();
		_parseTreeEvaluator = new ParseTreeEvaluator();
		_scope = new VariableScope();

		ASTPtRootNode _parseTreeX = null, _parseTreeY = null;

		if (!xRatio.equals(""))
			_parseTreeX = parser.generateParseTree(xRatio);

		if (!yRatio.equals(""))
			_parseTreeY = parser.generateParseTree(yRatio);

		PolygonUtil polygonRegions[] = new PolygonUtil[polyPoints.size()];
		int beginInd, endInd;
		for (int i = 0; i < polyPoints.size(); i++) {

			// extract the polygon points.
			String points = ((String) polyPoints.get(i));
			int iter = 0, ind = -1, comma = -1;
			polygonRegions[i] = new PolygonUtil();
			polygonRegions[i].Poly = new DoublePolygon();
			while (iter < points.length()) {
				ind = points.indexOf(' ', iter);
				String point = points.substring(iter, ind);
				iter = ind + 1;
				comma = point.indexOf(',');

				// processing the point by the conversion ratios.
				double Px = Double.parseDouble(point.substring(0, comma));
				if (!xRatio.equals("")) {
					param = Px;
					Px = _ratioConvert(_parseTreeX);
				}
				double Py = Double.parseDouble(point.substring(comma + 1));
				if (!yRatio.equals("")) {
					param = Py;
					Py = _ratioConvert(_parseTreeY);
				}
				// System.out.println(Px+","+Py);
				polygonRegions[i].Poly.addPoint(Px, Py);
				if (ind == points.lastIndexOf(' ')) {
					point = points.substring(ind + 1);
					comma = point.indexOf(',');
					Px = Double.parseDouble(point.substring(0, comma));
					if (!xRatio.equals("")) {
						param = Px;
						Px = _ratioConvert(_parseTreeX);
					}
					Py = Double.parseDouble(point.substring(comma + 1));
					if (!yRatio.equals("")) {
						param = Py;
						Py = _ratioConvert(_parseTreeY);
					}
					// System.out.println(Px+","+Py);
					polygonRegions[i].Poly.addPoint(Px, Py);
					iter = points.length();
				}
			}

			// extract the region.
			String reg = ((String) regionsStr.get(i));
			/*
			 * beginInd = reg.indexOf('('); endInd = reg.indexOf(')'); reg =
			 * reg.substring(beginInd + 2, endInd - 1);
			 */
			polygonRegions[i].Region = reg;
		}
		output.broadcast(new ObjectToken(polygonRegions));
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return true;
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	private void _getConversionFactors(File input)
			throws IllegalActionException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream is = new FileInputStream(input);
			Document doc = builder.parse(is);

			NodeList nodes = doc.getElementsByTagName("conversion");
			for (int i = 0; i < nodes.getLength(); i++) {
				String _id = ((Element) nodes.item(i)).getAttribute("id");
				String value = nodes.item(i).getFirstChild().getNodeValue();
				// String value = ( (Element) nodes.item(i)).getAttribute(_id);
				if (_id.equals("toOriginalX"))
					xRatio = value;
				else if (_id.equals("toOriginalY"))
					yRatio = value;
			}
		} catch (Exception ex) {
			throw new IllegalActionException(this, "Error parsing SVG file "
					+ input);
		}
	}

	private double _ratioConvert(ASTPtRootNode _parseTree)
			throws IllegalActionException {
		DoubleToken ratioToken = (DoubleToken) _parseTreeEvaluator
				.evaluateParseTree(_parseTree, _scope);
		if (ratioToken == null) {
			throw new IllegalActionException(this,
					"Expression yields a null result.");
		}
		return ratioToken.doubleValue();
	}

	private class VariableScope extends ModelScope {

		/**
		 * Look up and return the attribute with the specified name in the
		 * scope. Return null if such an attribute does not exist.
		 * 
		 * @return The attribute with the specified name in the scope.
		 */
		public Token get(String name) throws IllegalActionException {
			if (name.equals("val")) {
				return new DoubleToken(param);
			} else
				return null;
		}

		/**
		 * Look up and return the type of the attribute with the specified name
		 * in the scope. Return null if such an attribute does not exist.
		 * 
		 * @return The attribute with the specified name in the scope.
		 */
		public Type getType(String name) throws IllegalActionException {
			if (name.equals("val")) {
				return BaseType.DOUBLE;
			} else
				return null;
		}

		/**
		 * Look up and return the type term for the specified name in the scope.
		 * Return null if the name is not defined in this scope, or is a
		 * constant type.
		 * 
		 * @return The InequalityTerm associated with the given name in the
		 *         scope.
		 * @exception IllegalActionException
		 *                If a value in the scope exists with the given name,
		 *                but cannot be evaluated.
		 */
		public ptolemy.graph.InequalityTerm getTypeTerm(String name)
				throws IllegalActionException {
			if (name.equals("val")) {
				return new TypeConstant(BaseType.DOUBLE);
			} else
				return null;
		}

		/**
		 * Return the list of identifiers within the scope.
		 * 
		 * @return The list of identifiers within the scope.
		 */
		public Set identifierSet() {
			return getAllScopedVariableNames(null, SVGToPolygon.this);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Previous value of fileOrURL parameter. */
	private String _previousFileOrURL;
	private File input;
	private String xRatio = "", yRatio = "";
	private double param;
	// private ASTPtRootNode _parseTree = null;
	private ParseTreeEvaluator _parseTreeEvaluator = null;
	private VariableScope _scope = null;

}