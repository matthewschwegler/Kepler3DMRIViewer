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

package org.sdm.spa;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
 * @version $Id: CreateExpressionFromQuery.java 12324 2006-04-04 17:23:50Z
 *          altintas $
 */

public class CreateExpressionFromQuery extends TypedAtomicActor {

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
	public CreateExpressionFromQuery(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		queryPlan = new TypedIOPort(this, "queryPlan", true, false);
		queryPlan.setTypeEquals(BaseType.STRING);

		queryExpr1 = new TypedIOPort(this, "queryExpr1", false, true);
		queryExpr1.setTypeEquals(BaseType.STRING);
		queryExpr2 = new TypedIOPort(this, "queryExpr2", false, true);
		queryExpr2.setTypeEquals(BaseType.STRING);
		queryExpr3 = new TypedIOPort(this, "queryExpr3", false, true);
		queryExpr3.setTypeEquals(BaseType.STRING);
		queryExpr4 = new TypedIOPort(this, "queryExpr4", false, true);
		queryExpr4.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"80\" height=\"40\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public TypedIOPort queryPlan;
	public TypedIOPort queryExpr1;
	public TypedIOPort queryExpr2;
	public TypedIOPort queryExpr3;
	public TypedIOPort queryExpr4;

	/**
	 * 
	 * @exception IllegalActionException
	 *                If there is no director.
	 */
	public void fire() throws IllegalActionException {
		super.fire();
		StringToken inputToken = (StringToken) queryPlan.get(0);
		String layerLegends = inputToken.stringValue();
		// System.out.println(layerLegends);

		// /ASHRAF'S CODE
		try {
			org.w3c.dom.NodeList nl;

			DocumentBuilderFactory dbf;
			DocumentBuilder db;
			Document document;
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			document = db
					.parse(new InputSource(new StringReader(layerLegends)));

			org.w3c.dom.Element element = document.getDocumentElement();
			String queryStrings[] = new String[4];
			org.w3c.dom.NodeList datasets = element
					.getElementsByTagName("Dataset");
			if (datasets != null) {
				for (int i = 0; i < datasets.getLength(); i++) {
					StringBuffer queryString = new StringBuffer();
					String field = ((Element) datasets.item(i)).getAttribute(
							"column").trim();
					String layer = ((Element) datasets.item(i)).getAttribute(
							"id").trim();
					nl = ((Element) datasets.item(i))
							.getElementsByTagName("Term");
					String value = "";
					if (nl != null)
						value = nl.item(0).getFirstChild().getNodeValue()
								.trim();
					queryString.append(field + "='" + value + "' ");
					for (int j = 1; j < nl.getLength(); j++) {
						queryString.append(" OR ");
						value = nl.item(j).getFirstChild().getNodeValue()
								.trim();
						queryString.append(field + "='" + value + "' ");
					}
					queryStrings[i] = queryString.toString();
				}
				String utahString = queryStrings[0];
				String arizonaString = queryStrings[1];
				String nevadaString = queryStrings[2];
				String idahoString = queryStrings[3];
				// /END OF ASHRAF's CODE

				queryExpr1.broadcast(new StringToken(utahString));
				queryExpr2.broadcast(new StringToken(arizonaString));
				queryExpr3.broadcast(new StringToken(nevadaString));
				queryExpr4.broadcast(new StringToken(idahoString));

			}
		} catch (Exception ex) {
			System.out.println(ex);
		}
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

} // end of actor