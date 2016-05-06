/*
 * Copyright (c) 2001-2010 The Regents of the University of California.
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

package util;

import ij.ImageJ;
import ij.macro.MacroRunner;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.StringTokenizer;
import java.util.Vector;

import ptolemy.actor.gui.style.TextStyle;
import ptolemy.actor.lib.Sink;
import ptolemy.data.StringToken;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// ShowLocations
/**
 * <p>
 * This actor takes a set of points and plots them on a map. It uses the ImageJ
 * macro system (see http://rsb.info.nih.gov/ij/ for information on the ImageJ
 * system for image processing).
 * </p>
 * <p>
 * The 'map' is any image raster file that ImageJ can display. Point coordinates
 * are mapped to image file raster pixels by specifying the x and y values of
 * the upper left corner of the image and a multiplicative scale factor.
 * </p>
 * <p>
 * The points are given in the input text file (specified by a file name). A
 * single point location is given per line with the x and y values separated by
 * any a space or a tab.
 * </p>
 * 
 * @author Dan Higgins - NCEAS
 */
public class ShowLocations extends Sink {

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
	public ShowLocations(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		macroString = new StringParameter(this, "macroString");
		TextStyle macroStringTS = new TextStyle(macroString, "macroString");
		macroString.setExpression(_initMacro);

		fileOrURL = new FileParameter(this, "fileOrURL");
		fileOrURL.setExpression("world_720x360.jpg");
		X_upperleft = new StringParameter(this, "X_upperleft");
		X_upperleft.setExpression("-180.0");
		Y_upperleft = new StringParameter(this, "Y_upperleft");
		Y_upperleft.setExpression("90.0");
		scale_factor = new StringParameter(this, "scale_factor");
		scale_factor.setExpression("2.0");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * The file name or URL of the background map/image. x,y locations will be
	 * plotted on top of this image This is a string with any form accepted by
	 * File Attribute.
	 * 
	 */
	public FileParameter fileOrURL;

	/**
	 * The ImageJ macro to execute. Note that if the expression "_FILE_" is
	 * included in this string, it is replaced by the fileOrUrl parameter
	 * string, enabling the insertion of the input image file. <br/>
	 * x and y data in the input file will replace "_XPOINTS_" and "_YPOINTS_"
	 */
	public StringParameter macroString;

	/**
	 * The x-value of upper left corner of image (for scaling)
	 */
	public StringParameter X_upperleft;

	/**
	 * The y-value of upper left corner of image (for scaling)
	 */
	public StringParameter Y_upperleft;

	/**
	 * The scale factor. If (x,y) is the input location of a point, it is
	 * plotted at the pixel nearest to (x-pos, y-pos) where<br/>
	 * x-pos = ((x-X_upperleft)*scale_factor<br/>
	 * y-pos = ((x-Y_upperleft)*scale_factor
	 */
	public StringParameter scale_factor;

	static ImageJ ij;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * If the specified attribute is <i>URL</i>, then close the current file (if
	 * there is one) and open the new one.
	 * 
	 * @param attribute
	 *            The attribute that has changed.
	 * @exception IllegalActionException
	 *                If the specified attribute is <i>URL</i> and the file
	 *                cannot be opened.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		super.attributeChanged(attribute);
	}

	/**
	 * Clone the actor into the specified workspace. This calls the base class
	 * and then set the filename public member.
	 * 
	 * @param workspace
	 *            The workspace for the new object.
	 * @return A new actor.
	 * @exception CloneNotSupportedException
	 *                If a derived class contains an attribute that cannot be
	 *                cloned.
	 */
	public Object clone(Workspace workspace) throws CloneNotSupportedException {
		ShowLocations newObject = (ShowLocations) super.clone(workspace);
		return newObject;
	}

	/**
	 * Open the file at the URL, and set the width of the output.
	 */
	public void initialize() throws IllegalActionException {
		attributeChanged(fileOrURL);
	}

	/**
	 * Read in an image.
	 * 
	 * @exception IllegalActionException
	 *                If an IO error occurs.
	 */
	public boolean prefire() throws IllegalActionException {
		if (_url == null) {
			_fileRoot = null;
		} else {
			try {
				_fileRoot = _url.getFile();
			} catch (Exception e) {
				_fileRoot = null;
			}
		}
		return super.prefire();
	}

	/**
   *
   */
	public synchronized void fire() throws IllegalActionException {
		super.fire();
		if (!input.hasToken(0))
			return;

		String name = "";
		// If the fileOrURL input port is connected and has data, then
		// get the file name from there.
		if (input.getWidth() > 0) {
			if (input.hasToken(0)) {
				name = ((StringToken) input.get(0)).stringValue();
				_url = fileOrURL.asURL();
				_fileRoot = _url.getFile();
			}
		} else {
			name = ((StringToken) input.get(0)).stringValue();
		}

		System.out.println("firing ShowLocations");
		System.out.println("name: " + name);
		getPoints(name);
		scalePoints();

		if (ij == null) {
			if (IJMacro.ij != null) {// IJMacro may already have a static
										// instance of an ImageJ class; if so,
										// use it
				ij = IJMacro.ij;
			} else if (ShowLocations.ij != null) {
				ij = ShowLocations.ij;
			} else {
				ij = new ImageJ();
			}
		}
		if (ij == null || (ij != null && !ij.isShowing())) {
			new ImageJ();
		}
		String xulS = X_upperleft.stringValue();
		String yulS = Y_upperleft.stringValue();
		String scaleS = scale_factor.stringValue();
		try {
			xul = (new java.lang.Double(xulS)).doubleValue();
		} catch (Exception w) {
			xul = -180.0;
		}
		try {
			yul = (new java.lang.Double(yulS)).doubleValue();
		} catch (Exception w) {
			yul = 90.0;
		}
		try {
			mult_factor = (new java.lang.Double(scaleS)).doubleValue();
		} catch (Exception w) {
			mult_factor = 2.0;
		}
		String macro = macroString.getExpression();
		System.out.println("macro: " + macro);
		_fileRoot = _fileRoot.replace('\\', '/');
		macro = macro.replaceAll("_FILE_", _fileRoot);
		macro = macro.replaceAll("_XPOINTS_", createIJArray(xpts_scaled));
		macro = macro.replaceAll("_YPOINTS_", createIJArray(ypts_scaled));
		System.out.println("macro: " + macro);
		MacroRunner mr = new MacroRunner(macro);
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private String _initMacro = "run(\"Open...\", \"open=_FILE_\");"
			+ "\nmakeSelection(\"polygon\", newArray(_XPOINTS_), newArray(_YPOINTS_));";

	// The URL as a string.
	private String _fileRoot;

	// The File
	private File _file;

	// The URL of the file.
	private URL _url;

	private Vector pts = null;

	private double xul = -180.0;
	private double yul = 90.0;
	private double mult_factor = 2.0;

	private Vector xpts_scaled = null;
	private Vector ypts_scaled = null;

	private double scale_x(double x) {
		return ((x - xul) * mult_factor);
	}

	private double scale_y(double y) {
		return ((yul - y) * mult_factor);
	}

	// assume that pts are now in Vector pts
	// (due to call of getPoints)
	private void scalePoints() {
		xpts_scaled = new Vector();
		ypts_scaled = new Vector();
		DecimalFormat myFormatter = new DecimalFormat("##0.00");
		for (int i = 0; i < pts.size(); i++) {
			Point2D pt = (Point2D) pts.elementAt(i);
			double x = scale_x(pt.getX());
			double y = scale_y(pt.getY());
			String xS = myFormatter.format(x);
			// System.out.println("xS: "+xS);
			xpts_scaled.add(xS);
			String yS = myFormatter.format(y);
			// System.out.println("yS: "+yS);
			ypts_scaled.add(yS);
		}
	}

	private String createIJArray(Vector vec) {
		String res = (String) vec.elementAt(0);
		for (int i = 1; i < vec.size(); i++) {
			res = res + " , " + (String) vec.elementAt(i);
		}
		return res;
	}

	private void getPoints(String file) {
		File pointsFile = new File(file);
		FileReader inReader = null;
		BufferedReader bufReader = null;
		try {
			inReader = new FileReader(pointsFile);
			bufReader = new BufferedReader(inReader);

			String cachedLine = "";
			double xval = 0.0;
			double yval = 0.0;
			pts = new Vector();
			// unsure exactly how many point lines there are
			// but each line should have only two tokens
			boolean eoh = false;
			while (!eoh) {
				try {
					cachedLine = bufReader.readLine();
					// System.out.println("cachedLine: "+cachedLine);
				} catch (Exception w) {
					System.out
							.println("error reading next line in points file!");
					eoh = true;
				}
				if (cachedLine == null)
					return;
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
		} catch (Exception w) {
		}
	}

}