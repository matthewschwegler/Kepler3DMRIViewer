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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.ASTPtRootNode;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.ModelScope;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.ParseTreeEvaluator;
import ptolemy.data.expr.PtParser;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.data.type.TypeConstant;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// AddPointToSVG
/**
 * This actor accepts a point and an SVG file and adds the point to the SVG
 * file. The point is defined as and array of doubles ({X,Y}). The SVG file may
 * have conversion ratios for X and Y, specified by a conversion and xRatio,
 * yRatio tags. The X,Y values are converted using these factors (if available).
 * The point is added as a tag to the SVG file. The resulting SVG content is
 * saved to svgOutFile. An example SVG file is available at
 * lib/testdata/geon/whalen.svg.
 * 
 * @UserLevelDocumentation This actor accepts a point, represented as an X,Y
 *                         array and an SVG file and adds the point to the SVG
 *                         file. If the file contains, xRatio, yRatio conversion
 *                         tags, the X,Y values will be converted accordingly.
 *                         The resulting SVG content is saved to svgOutFile.
 *                         This actor is typically used for display purposes. An
 *                         example SVG file is available at
 *                         lib/testdata/geon/whalen.svg.
 * @author Efrat Jaeger
 * @version $Id: AddPointToSVG.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class AddPointToSVG extends TypedAtomicActor {

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
	public AddPointToSVG(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		point = new TypedIOPort(this, "point", true, false);
		point.setTypeEquals(new ArrayType(BaseType.DOUBLE));

		svgFile = new TypedIOPort(this, "svgFile", true, false);
		svgFile.setTypeEquals(BaseType.STRING);

		svgOutputFile = new TypedIOPort(this, "svgOutputFile", true, false);
		svgOutputFile.setTypeEquals(BaseType.STRING);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);

		trigger = new TypedIOPort(this, "trigger", true, false);
		trigger.setMultiport(true);

		svgFileParam = new FileParameter(this, "svgFileParam");
		svgFileParam.setDisplayName("SVG File");

		svgOutFile = new FileParameter(this, "svgOutFile");
		svgOutFile.setDisplayName("SVG Output File");

		confirmOverwrite = new Parameter(this, "confirmOverwrite");
		confirmOverwrite.setTypeEquals(BaseType.BOOLEAN);
		confirmOverwrite.setToken(BooleanToken.TRUE);

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
	 * Point to be added. An array of doubles, {X,Y}.
	 * 
	 * @UserLevelDocumentation The point to be added to the SVG file. An array
	 *                         of doubles of the following format: {X,Y}.
	 */
	public TypedIOPort point;

	/**
	 * An SVG inupt file port.
	 * 
	 * @UserLevelDocumentation The path to the SVG file to be updated.
	 */
	public TypedIOPort svgFile;

	/**
	 * An SVG output file port.
	 * 
	 * @UserLevelDocumentation The path to the output SVG file.
	 */
	public TypedIOPort svgOutputFile;

	/**
	 * Outputs the SVG output file path.
	 * 
	 * @UserLevelDocumentation The actor output the path to the modified SVG
	 *                         file.
	 */
	public TypedIOPort output;

	/**
	 * Triggering the actor execution.
	 * 
	 * @UserLevelDocumentation This port is used to trigger the actor.
	 */
	public TypedIOPort trigger;

	/**
	 * An SVG input file name or URL. This is a string with any form accepted by
	 * FileParameter.
	 * 
	 * @UserLevelDocumentation The path to the SVG file to be updated.
	 * @see FileParameter
	 */
	public FileParameter svgFileParam;

	/**
	 * An SVG output file name or URL. This is a string with any form accepted
	 * by FileParameter.
	 * 
	 * @UserLevelDocumentation The path to the output SVG file.
	 * @see FileParameter
	 */
	public FileParameter svgOutFile;

	/**
	 * If false, then overwrite the specified file if it exists without asking.
	 * If true (the default), then if the file exists, ask for confirmation
	 * before overwriting.
	 * 
	 * @UserLevelDocumentation If false, then overwrite the specified file if it
	 *                         exists without asking. If true (the default),
	 *                         then if the file exists, ask for confirmation
	 *                         before overwriting.
	 */
	public Parameter confirmOverwrite;

	/**
	 * Translate the point into the svg ratio parameters and add it to the file.
	 * Broadcast the URL to the svg output file.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {

		// trigger the actor.
		for (int i = 0; i < trigger.getWidth(); i++) {
			if (trigger.hasToken(i)) {
				trigger.get(i);
			}
		}

		_svgFile = null;
		_svgOutFile = null;
		String fileName = "";
		// get SVG file.
		if (svgFile.getWidth() > 0) {
			fileName = ((StringToken) svgFile.get(0)).stringValue();
			int lineEndInd = fileName.indexOf("\n");
			if (lineEndInd != -1) { // The string contains a CR.
				fileName = fileName.substring(0, lineEndInd);
			}
			svgFileParam.setExpression(fileName.trim());
		}
		_svgFile = svgFileParam.asFile();
		if (!_svgFile.exists()) {
			throw new IllegalActionException(this, "SVG file " + fileName
					+ " doesn't exist.");
		}

		fileName = "";
		// get SVG output file.
		if (svgOutputFile.getWidth() > 0) {
			fileName = ((StringToken) svgOutputFile.get(0)).stringValue();
			int lineEndInd = fileName.indexOf("\n");
			if (lineEndInd != -1) { // The string contains a CR.
				fileName = fileName.substring(0, lineEndInd);
			}
			svgOutFile.setExpression(fileName);
		}
		_svgOutFile = svgOutFile.asFile();

		boolean confirmOverwriteValue = ((BooleanToken) confirmOverwrite
				.getToken()).booleanValue();
		// Don't ask for confirmation in append mode, since there
		// will be no loss of data.
		if (_svgOutFile.exists() && confirmOverwriteValue) {
			// Query for overwrite.
			// FIXME: This should be called in the event thread!
			// There is a chance of deadlock since it is not.
			if (!GraphicalMessageHandler.yesNoQuestion("OK to overwrite "
					+ _svgOutFile + "?")) {
				throw new IllegalActionException(this,
						"Please select another file name.");
			}
		}

		// get point.
		ArrayToken arrayToken = (ArrayToken) point.get(0);
		xVal = ((DoubleToken) arrayToken.getElement(0)).doubleValue();
		yVal = ((DoubleToken) arrayToken.getElement(1)).doubleValue();

		// get conversion factors.
		xRatio = "";
		yRatio = ""; // reset factors first.
		_getConversionFactors();

		// calculate parse tree for ratios.
		PtParser parser = new PtParser();
		_parseTreeEvaluator = new ParseTreeEvaluator();
		_scope = new VariableScope();

		// translate point.
		ASTPtRootNode _parseTree = null;

		if (!xRatio.equals("")) {
			_parseTree = parser.generateParseTree(xRatio);
			param = xVal;
			xVal = _ratioConvert(_parseTree);
		}

		if (!yRatio.equals("")) {
			_parseTree = parser.generateParseTree(yRatio);
			param = yVal;
			yVal = _ratioConvert(_parseTree);
		}

		// add the point to the svg file.
		_addPointToSVG();

		// broadcast SVG output file URL.
		output.broadcast(new StringToken(_svgOutFile.getAbsolutePath()));
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Add the point to the SVG file.
	 */
	private void _addPointToSVG() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(_svgFile));
			StringBuffer svgContent = new StringBuffer();
			String line;
			// _svgContent = new StringBuffer();
			String extraLine = "<circle cx='" + xVal + "' cy='" + yVal
					+ "' r='2' fill='red' stroke='red'/>"; // TODO: ADD VARIABLE
															// RADIUS..
			// System.out.println("Extra line" + extraLine);
			while ((line = br.readLine()) != null) {
				int ind = line.toLowerCase().indexOf("</svg>");
				if (ind != -1) {
					// System.out.println("Inside extra line");
					svgContent.append(line.substring(0, ind) + "\n");
					svgContent.append(extraLine + "\n");
					svgContent.append(line.substring(ind) + "\n");
				} else
					svgContent.append(line + "\n");
			}
			br.close();
			BufferedWriter out = new BufferedWriter(new FileWriter(_svgOutFile));
			out.write(svgContent.toString());
			out.flush();
			out.close();
		} catch (IOException e) {
			GraphicalMessageHandler.error("Error opening file", e);
		}
	}

	/**
	 * Get the xRatio, yRatio conversion factors, if available, from the SVG
	 * file.
	 */
	private void _getConversionFactors() throws IllegalActionException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream is = new FileInputStream(_svgFile);
			Document doc = builder.parse(is);

			NodeList nodes = doc.getElementsByTagName("conversion");
			for (int i = 0; i < nodes.getLength(); i++) {
				String _id = ((Element) nodes.item(i)).getAttribute("id");
				String value = nodes.item(i).getFirstChild().getNodeValue();
				// String value = ( (Element) nodes.item(i)).getAttribute(_id);
				if (_id.equals("xRatio"))
					xRatio = value;
				else if (_id.equals("yRatio"))
					yRatio = value;
			}
		} catch (Exception ex) {
			throw new IllegalActionException(this, "Error parsing SVG file "
					+ _svgFile);
		}
	}

	/**
	 * Use the conversion ratio to calculate a coordinate value.
	 * 
	 * @param _parseTree
	 * 	 * @throws IllegalActionException
	 */
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
			return getAllScopedVariableNames(null, AddPointToSVG.this);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Previous value of fileOrURL parameter. */
	private String _previousFileOrURL;
	private File _svgFile, _svgOutFile;
	private String xRatio = "", yRatio = "";
	private double param, xVal = 0.0, yVal = 0.0;
	// private ASTPtRootNode _parseTree = null;
	private ParseTreeEvaluator _parseTreeEvaluator = null;
	private VariableScope _scope = null;
}