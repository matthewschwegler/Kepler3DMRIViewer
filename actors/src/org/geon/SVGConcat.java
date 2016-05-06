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
import java.net.URI;

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
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.gui.GraphicalMessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.attributes.URIAttribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// AddPointToSVG
/**
 * Concatanate several SVG files to a single file. Typical Usage is to create
 * output displays. The actor accepts the input SVG paths as an array of
 * strings, and x,y shift values for positioning each of the files and returns
 * the path of the concatenated output file.
 * 
 * @author Efrat Jaeger
 * @version $Id: SVGConcat.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class SVGConcat extends TypedAtomicActor {

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
	public SVGConcat(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		svgFiles = new TypedIOPort(this, "svgFiles", true, false);
		svgFiles.setTypeEquals(new ArrayType(BaseType.STRING));

		outputPathPort = new TypedIOPort(this, "outputPath", true, false);
		outputPathPort.setTypeEquals(BaseType.STRING);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.STRING);

		outputPathParam = new FileParameter(this, "outputPathParam");
		outputPathParam.setDisplayName("SVG output file");

		shiftX = new Parameter(this, "shiftX");
		shiftY = new Parameter(this, "shiftY");
		shiftX.setExpression("0.0");
		shiftY.setExpression("0.0");

		// trigger = new TypedIOPort(this, "trigger", true, false);
		// trigger.setMultiport(true);

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
	 * Array of SVG input files.
	 */
	public TypedIOPort svgFiles;

	/**
	 * An SVG output file URL port.
	 */
	public TypedIOPort outputPathPort;

	/**
	 * Output SVG output file URL.
	 */
	public TypedIOPort output;

	/**
	 * Trigger actor execution.
	 */
	// public TypedIOPort trigger;
	/**
	 * An SVG output file name or URL. This is a string with any form accepted
	 * by FileParameter.
	 * 
	 * @see FileParameter
	 */
	public FileParameter outputPathParam;

	/**
	 * Relative shift over the X coordinate during concatenation.
	 */
	public Parameter shiftX;

	/**
	 * Relative shift over the Y coordinate during concatenation.
	 */
	public Parameter shiftY;

	/**
	 * If <i>false</i>, then overwrite the specified file if it exists without
	 * asking. If <i>true</i> (the default), then if the file exists, ask for
	 * confirmation before overwriting.
	 */
	public Parameter confirmOverwrite;

	/**
	 * Translate the point into the svg ratio parameters and add it to the file.
	 * Broadcasts the URL to the svg output file.
	 * 
	 * @exception IllegalActionException
	 *                If there's no director.
	 */
	public void fire() throws IllegalActionException {

		// trigger the actor.
		/*
		 * for (int i = 0; i < trigger.getWidth(); i++) { if
		 * (trigger.hasToken(i)) { trigger.get(i); } }
		 */

		_svgFile = null;
		_svgOutFile = null;
		String fileName = "";
		// get SVG output file.
		if (outputPathPort.getWidth() > 0) {
			fileName = ((StringToken) outputPathPort.get(0)).stringValue();
			int lineEndInd = fileName.indexOf("\n");
			if (lineEndInd != -1) { // The string contains a CR.
				fileName = fileName.substring(0, lineEndInd);
			}
			outputPathParam.setExpression(fileName);
		}
		_svgOutFile = outputPathParam.asFile();

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
		double _shiftX = 0.0, _shiftY = 0.0;
		try {
			_shiftX = ((DoubleToken) shiftX.getToken()).doubleValue();
		} catch (Exception ex) {
			_shiftX = 0.0;
		}
		try {
			_shiftY = ((DoubleToken) shiftY.getToken()).doubleValue();
		} catch (Exception ex) {
			_shiftY = 0.0;
		}

		// FIXME: Currently assuming all files have the same header
		// (except for width and height) and the same scripts
		// and that tags begin with a new line.
		String line;
		_maxWidth = 0;
		_maxHeight = 0;
		StringBuffer _sbheader = new StringBuffer();
		StringBuffer _sbmicro = new StringBuffer();
		StringBuffer _sbcontent = new StringBuffer();
		// Get SVG input array.
		ArrayToken arrayToken = (ArrayToken) svgFiles.get(0);
		for (int i = 0; i < arrayToken.length(); i++) {
			String svgFilePath = ((StringToken) arrayToken.getElement(i))
					.stringValue();
			_svgFile = new File(svgFilePath);
			if (!_svgFile.isAbsolute()) {
				// Try to resolve the base directory.
				URI modelURI = URIAttribute.getModelURI(this);
				if (modelURI != null) {
					URI newURI = modelURI.resolve(svgFilePath);
					_svgFile = new File(newURI);
				}
			}

			try {
				BufferedReader br = new BufferedReader(new FileReader(_svgFile));
				if (i == 0) {// get header.
					while ((line = br.readLine()) != null) {
						int ind = line.toLowerCase().indexOf("width");
						if (ind != -1) {
							break;
						} else {
							_sbheader.append(line + "\n");
						}
					}
					// _widthHeightLine = line;

					while ((line = br.readLine()) != null) {
						int ind = line.toLowerCase().indexOf("</script>");
						if (ind != -1) {
							_sbmicro.append(line + "\n");
							break;
						} else {
							_sbmicro.append(line + "\n");
						}
					}
				} else { // skip to after script.
					while ((line = br.readLine()) != null) {
						int ind = line.toLowerCase().indexOf("</script>");
						if (ind != -1) {
							break;
						}
					}
				}

				String transform = "<g transform=\"translate(";
				if (_shiftX > 0)
					transform += _maxWidth;
				else
					transform += "0.0";
				transform += ",";
				if (_shiftY > 0)
					transform += _maxHeight;
				else
					transform += "0.0";
				transform += ")\">";
				_sbcontent.append(transform);

				while ((line = br.readLine()) != null) {
					int ind = line.toLowerCase().indexOf("</svg>");
					if (ind != -1) {
						// System.out.println("Inside extra line");
						_sbcontent.append(line.substring(0, ind) + "\n");
						_sbcontent.append("</g>\n");
						break;
					} else
						_sbcontent.append(line + "\n");
				}
				br.close();

				_getWidthHeight();
				if (_shiftX > 0)
					_maxWidth = _maxWidth + _tempWidth + _shiftX;
				else
					_maxWidth = (_maxWidth > _tempWidth) ? _maxWidth
							: _tempWidth;
				if (_shiftY > 0)
					_maxHeight = _maxHeight + _tempHeight + _shiftY;
				else
					_maxHeight = (_maxHeight > _tempHeight) ? _maxHeight
							: _tempHeight;

			} catch (Exception ex) {
				throw new IllegalActionException(this, ex, "Error parsing "
						+ _svgFile + " in actor " + this.getName());
			}
		}
		_sbcontent.append("</svg>");
		_sbheader.append("     width='" + _maxWidth);
		_sbheader.append("' height='" + _maxHeight + "'>\n");

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(_svgOutFile));
			out.write(_sbheader.toString());
			out.write(_sbmicro.toString());
			out.write(_sbcontent.toString());
			out.flush();
			out.close();
		} catch (IOException ex) {
			throw new IllegalActionException(this, ex, "Error writing to file "
					+ _svgOutFile);
		}

		// broadcast SVG output file URL.
		output.broadcast(new StringToken(_svgOutFile.getAbsolutePath()));
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	private void _getWidthHeight() throws IllegalActionException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream is = new FileInputStream(_svgFile);
			Document doc = builder.parse(is);

			NodeList nodes = doc.getElementsByTagName("svg");
			for (int i = 0; i < nodes.getLength(); i++) {
				String attr = ((Element) nodes.item(i)).getAttribute("width");
				_tempWidth = Double.parseDouble(attr);
				attr = ((Element) nodes.item(i)).getAttribute("height");
				_tempHeight = Double.parseDouble(attr);
			}
		} catch (Exception ex) {
			throw new IllegalActionException(this, "Error parsing SVG file "
					+ _svgFile);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** Previous value of fileOrURL parameter. */
	private File _svgFile, _svgOutFile;
	private double param, xVal = 0.0, yVal = 0.0;
	private double _tempWidth, _tempHeight, _maxWidth, _maxHeight;
	private String _widthHeightLine;
}