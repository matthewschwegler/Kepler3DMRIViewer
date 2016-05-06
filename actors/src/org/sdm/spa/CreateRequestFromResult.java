/*
 * Copyright (c) 2000-2010 The Regents of the University of California.
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

package org.sdm.spa;

import java.io.StringReader;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// CreateExpressionFromQuery
/**
 * @author Ilkay Altintas, Ashraf Memonn
 * @version $Id: CreateRequestFromResult.java 11161 2005-11-01 20:39:16Z ruland
 *          $
 */

public class CreateRequestFromResult extends TypedAtomicActor {

	/**
	 * Construct a CreateExpressionFromQuery actor with the given container and
	 * name.
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
	public CreateRequestFromResult(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		resultArizona = new TypedIOPort(this, "resultArizona", true, false);
		resultArizona.setTypeEquals(BaseType.STRING);
		resultIdaho = new TypedIOPort(this, "resultIdaho", true, false);
		resultIdaho.setTypeEquals(BaseType.STRING);
		envelope = new TypedIOPort(this, "envelope", true, false);
		envelope.setTypeEquals(BaseType.STRING);

		request = new TypedIOPort(this, "request", false, true);
		request.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"80\" height=\"40\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public TypedIOPort resultArizona;
	public TypedIOPort resultIdaho;
	public TypedIOPort envelope;
	public TypedIOPort request;

	private String constructRenderRequest(String layerName, String output) {
		String str = new String();
		Element element = stringToElement(output);
		String type = getStringValueForXMLTag(element, "type");
		String url = getStringValueForXMLTag(element, "url");
		return ("<layer><name>" + layerName + "</name><type>" + type
				+ "</type><url>" + url + "</url></layer>");
	}

	public Element stringToElement(String arg) {
		try {
			DocumentBuilderFactory dbf;
			DocumentBuilder db;
			Document document;
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			document = db.parse(new InputSource(new StringReader(arg)));
			return document.getDocumentElement();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}
	}

	public String getStringValueForXMLTag(Element xmlElement, String key) {
		NodeList nl = xmlElement.getElementsByTagName(key);
		if (nl.getLength() > 0) {
			return (nl.item(0).getFirstChild().getNodeValue().trim());
		}
		return "";
	}

	/**
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		StringToken inputToken = (StringToken) resultArizona.get(0);
		String arizonaStr = inputToken.stringValue();
		System.out.println("arizona--->" + arizonaStr);
		inputToken = (StringToken) resultIdaho.get(0);
		String idahoStr = inputToken.stringValue();
		System.out.println("idaho--->" + idahoStr);
		inputToken = (StringToken) envelope.get(0);
		String envStr = inputToken.stringValue();
		System.out.println("envelope" + envStr);
		String requestStr = "";

		// ASHRAF'S CODE
		String dynamicService = new String(Long.toString(new Date().getTime()));
		requestStr = "<request name=\"RenderMap\"><client>Browser</client><servicename>D"
				+ dynamicService
				+ "</servicename><envelope>"
				+ envStr
				+ "</envelope><layers>"
				+ constructRenderRequest("Arizona", arizonaStr)
				+ constructRenderRequest("Idaho", idahoStr)
				+ "<layer><name>States</name><type>LocalURL"
				+ "</type><url>c:\\geontemp\\states_polygon_area.shp</url></layer></layers></request>";
		System.out.println("request--->" + requestStr);
		// ASHRAF's CODE ENDS

		request.broadcast(new StringToken(requestStr));

	} // end of fire

	/**
	 * Post fire the actor. Return false to indicate that the process has
	 * finished. If it returns true, the process will continue indefinitely.
	 */
	public boolean postfire() {
		return false;
	} // end of postfire

	/**
	 * Pre fire the actor. Calls the super class's prefire in case something is
	 * set there.
	 */
	public boolean prefire() throws IllegalActionException {
		return super.prefire();
	} // end of prefire

} // end of WebService