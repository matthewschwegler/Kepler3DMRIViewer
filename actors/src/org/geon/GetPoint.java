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

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.IntToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.gui.MessageHandler;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// GetPoint
/**
 * This is a domain specific actor used within the GEON mineral classifier for
 * calculating a calssification point given mineral composition and coordinate
 * names.
 * 
 * @author Efrat Jaeger
 * @version $Id: GetPoint.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class GetPoint extends TypedAtomicActor {

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
	public GetPoint(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		// Set the type constraint.
		rowInfo = new TypedIOPort(this, "rowInfo", true, false);
		// rowInfo.setTypeEquals(BaseType.GENERAL);
		rowInfo.setTypeEquals(BaseType.STRING);
		coordinateNames = new TypedIOPort(this, "coordinateNames", true, false);
		coordinateNames.setTypeEquals(BaseType.INT); // FIX ME! for demo
														// purposes!!!
		point = new TypedIOPort(this, "point", false, true);
		point.setTypeEquals(BaseType.GENERAL);
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * Hold the mineral composition info.
	 */
	public TypedIOPort rowInfo;

	/**
	 * The coordinates/minerals of the classification diagram.
	 */
	public TypedIOPort coordinateNames;

	/**
	 * The classification point.
	 */
	public TypedIOPort point;

	/**
	 * Retrieve the mineral composition for the coordinate names and calculate
	 * the classification point.
	 */
	public void fire() throws IllegalActionException {

		// get the working directory.
		String _keplerPath = System.getProperty("user.dir");
		System.out.println("kepler abs path ==> " + _keplerPath);

		// FIX ME: for demo purposes. needs to be generalized.
		// ObjectToken obj1 = (ObjectToken) rowInfo.get(0);
		// RockSample Rock = (RockSample) obj1.getValue();

		RockSample Rock = new RockSample();

		String xmlStr = ((StringToken) rowInfo.get(0)).stringValue();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			byte[] xmlBytes = xmlStr.getBytes();
			InputStream is = new ByteArrayInputStream(xmlBytes);
			Document doc = builder.parse(is);

			NodeList nodes = doc.getElementsByTagName("row");
			for (int i = 0; i < nodes.getLength(); i++) {
				NodeList childs = nodes.item(i).getChildNodes();
				for (int j = 0; j < childs.getLength(); j++) {
					Node child = childs.item(j);
					if (child instanceof Element) {
						Element elem = (Element) child;
						String tag = elem.getTagName();
						String val = "";
						if (elem.getChildNodes().getLength() > 0) {
							val = elem.getFirstChild().getNodeValue().trim();
						}
						Rock.add(tag, val);
					}
				}
			}
		} catch (Exception ex) {
			throw new IllegalActionException(this, ex
					+ " xml processing exception.");
		}

		while (true) {
			int layer = ((IntToken) coordinateNames.get(0)).intValue();
			if (layer == 1) {
				_point = Rock.getPointForGabbroOlivine(35, 32, 340, 294, layer);
				// FIX ME: Change according to diagram coordinates.
				_addPointToSVG(_keplerPath + "/lib/testdata/geon/QAPF.svg",
						_keplerPath + "/lib/testdata/geon/layer1.svg"); // FIX
																		// ME!!!
																		// relative
																		// files
			} else if (layer == 2) {
				_point = Rock.getPointForGabbroOlivine(40, 32, 420, 345, layer);
				// FIX ME: Change according to diagram coordinates.
				_addPointToSVG(_keplerPath + "/lib/testdata/geon/PlagPxOl.svg",
						_keplerPath + "/lib/testdata/geon/layer2.svg"); // FIX
																		// ME!!!
																		// relative
																		// files
			}
			// System.out.println("point: Px = " + _point.getX() + " , Py = " +
			// _point.getY());
			point.broadcast(new ObjectToken(_point));
		}
	}

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return true;
	}

	/**
	 * Add the classification point to the SVG file for display purposes.
	 */
	private void _addPointToSVG(String inpFile, String outFile) {
		try {
			System.out.println("input : " + inpFile + " , output : " + outFile);
			File input = new File(inpFile);
			BufferedReader br = new BufferedReader(new FileReader(input));
			String line;
			File output = new File(outFile);
			// System.out.println("File name" + file.getName());
			BufferedWriter out = new BufferedWriter(new FileWriter(output));
			// System.out.println("path" + file.getPath());
			String extraLine = "<circle cx='" + _point.getX() + "' cy='"
					+ _point.getY() + "' r='3' fill='red' stroke='red'/>";
			// System.out.println("Extra line" + extraLine);
			while ((line = br.readLine()) != null) {
				int ind = line.toLowerCase().indexOf("</svg>");
				if (ind != -1) {
					// System.out.println("Inside extra line");
					out.write(line.substring(0, ind) + "\n");
					out.write(extraLine + "\n");
					out.write(line.substring(ind) + "\n");
				} else
					out.write(line + "\n");
			}
			out.close();
			br.close();
		} catch (IOException e) {
			MessageHandler.error("Error opening file", e);
		}
	}

	/**
	 * The classification point.
	 */
	private Point _point;
}