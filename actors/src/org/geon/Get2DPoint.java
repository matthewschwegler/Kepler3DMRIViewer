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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.ASTPtRootNode;
import ptolemy.data.expr.ModelScope;
import ptolemy.data.expr.ParseTreeEvaluator;
import ptolemy.data.expr.PtParser;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.data.type.TypeConstant;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// Expression
/**
 * Get 2D classification point for rock classification.
 * 
 * The actor Accepts coordinates expressions and an xml composition string. It
 * evaluates the expression using the expression evaluator (PtParser as in the
 * Expression actor) and the composition. Returns an array of doubles of length
 * two representing the x and y values.
 * 
 * see Expression // is ambiguous
 * 
 * @author Efrat Jaeger
 * @version $Id: Get2DPoint.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 4.0.1
 */

public class Get2DPoint extends TypedAtomicActor {

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
	public Get2DPoint(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		rowInfo = new TypedIOPort(this, "rowInfo", true, false);
		rowInfo.setTypeEquals(BaseType.STRING);

		coordinates = new TypedIOPort(this, "coordinates", true, false);
		coordinates.setTypeEquals(new ArrayType(BaseType.STRING));

		point = new TypedIOPort(this, "point", false, true);
		point.setTypeEquals(new ArrayType(BaseType.DOUBLE));
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** Composition info. */
	public TypedIOPort rowInfo;

	/**
	 * An array of the coordinates to be evaluated.
	 */
	public TypedIOPort coordinates;

	/**
	 * The classification point.
	 */
	public TypedIOPort point;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Accepts coordinates expressions and an xml composition string. Evaluates
	 * the expression using the expression evaluator and the composition.
	 * Returns an array of doubles of length two representing the x and y
	 * values.
	 * 
	 * @exception IllegalActionException
	 *                If the evaluation of the expression triggers it, or the
	 *                evaluation yields a null result, or the evaluation yields
	 *                an incompatible type, or if there is no director, or if a
	 *                connected input has no tokens.
	 */
	public void fire() throws IllegalActionException {

		try {
			Token[] coordinatesTokens = ((ArrayToken) coordinates.get(0))
					.arrayValue();

			String compositionXML = ((StringToken) rowInfo.get(0))
					.stringValue();
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				factory.setValidating(false);
				DocumentBuilder builder = factory.newDocumentBuilder();
				byte[] xmlBytes = compositionXML.getBytes();
				InputStream is = new ByteArrayInputStream(xmlBytes);
				_composition = builder.parse(is);
			} catch (Exception ex) {
				throw new IllegalActionException(this, ex
						+ " xml processing exception.");
			}

			String xCoor = ((StringToken) coordinatesTokens[1]).stringValue();
			String yCoor = ((StringToken) coordinatesTokens[2]).stringValue();

			// Token inputToken = port.get(0);
			// _tokenMap.put(port.getName(), inputToken);

			PtParser parser = new PtParser();
			_parseTree = parser.generateParseTree(xCoor);
			_parseTreeEvaluator = new ParseTreeEvaluator();
			_scope = new VariableScope();
			DoubleToken xToken = (DoubleToken) _parseTreeEvaluator
					.evaluateParseTree(_parseTree, _scope);
			_parseTree = parser.generateParseTree(yCoor);
			DoubleToken yToken = (DoubleToken) _parseTreeEvaluator
					.evaluateParseTree(_parseTree, _scope);
			if (xToken == null || yToken == null) {
				throw new IllegalActionException(this,
						"One of the coordinates yields a null result: "
								+ " x : " + xCoor + " , y : " + yCoor);
			}

			double xVal = xToken.doubleValue();
			double yVal = yToken.doubleValue();
			Token arrayToken[] = new Token[2];
			arrayToken[0] = new DoubleToken(xVal);
			arrayToken[1] = new DoubleToken(yVal);
			// Point2D.Double _point = new Point2D.Double(xVal, yVal);
			point.broadcast(new ArrayToken(arrayToken));
		} catch (IllegalActionException ex) {
			// Chain exceptions to get the actor that threw the exception.
			throw new IllegalActionException(this, ex,
					"Invalid coordintes expression.");
		}

	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	private class VariableScope extends ModelScope {

		/**
		 * Look up and return the attribute with the specified name in the
		 * scope. Return 0 if such an attribute does not exist.
		 * 
		 * @return The attribute with the specified name in the scope.
		 */
		public Token get(String name) throws IllegalActionException {

			String strVal = "";
			double value = 0;
			NodeList minValues = _composition.getElementsByTagName(name);
			for (int i = 0; i < minValues.getLength(); i++) {
				Element mineral = (Element) minValues.item(i);
				if (mineral.getChildNodes().getLength() > 0) {
					strVal = mineral.getFirstChild().getNodeValue().trim();
				}
				if (!strVal.equals("")) {
					Double DVal = new Double(Double.parseDouble(strVal));
					value = DVal.doubleValue();
				}
			}
			return new DoubleToken(value);
		}

		/**
		 * Look up and return the type of the attribute with the specified name
		 * in the scope. Return null if such an attribute does not exist.
		 * 
		 * @return The attribute with the specified name in the scope.
		 */
		public Type getType(String name) throws IllegalActionException {
			return BaseType.DOUBLE;
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
			return new TypeConstant(BaseType.DOUBLE);
		}

		/**
		 * Return the list of identifiers within the scope.
		 * 
		 * @return The list of identifiers within the scope.
		 */
		public Set identifierSet() {
			return getAllScopedVariableNames(null, Get2DPoint.this);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	private Document _composition = null;
	private int _iterationCount = 1;
	private ASTPtRootNode _parseTree = null;
	private ParseTreeEvaluator _parseTreeEvaluator = null;
	private VariableScope _scope = null;
	private Map _tokenMap;
}