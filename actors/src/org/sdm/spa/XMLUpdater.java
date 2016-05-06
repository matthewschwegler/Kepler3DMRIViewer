/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-07-11 15:13:03 -0700 (Wed, 11 Jul 2012) $' 
 * '$Revision: 30175 $'
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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.jdom.output.XMLOutputter;

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.XMLToken;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// XMLUpdater class name
/**
 * On each firing, read an XML token from port <i>input</i>, one token from each
 * additional input port, and output the original XML updated with new values.
 * This actor is similar to RecordUpdater.
 * 
 * @author Daniel Crawl
 * @version $Id: XMLUpdater.java 30175 2012-07-11 22:13:03Z crawl $
 */

public class XMLUpdater extends TypedAtomicActor {

	/**
	 * Construct an XMLUpdater source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public XMLUpdater(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		input = new TypedIOPort(this, XML_IN_PORT_NAME, true, false);
		input.setTypeEquals(BaseType.XMLTOKEN);

		output = new TypedIOPort(this, "output", false, true);
		output.setTypeEquals(BaseType.XMLTOKEN);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

		_domBuilder = new DOMBuilder();
		_xo = new XMLOutputter();
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////
	
	/** The input port that reads the XML document. */
	public TypedIOPort input = null;

	/** The output port that writes the XML document. */
	public TypedIOPort output = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Send the token in the value parameter to the output.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		XMLToken token = (XMLToken) input.get(0);
		org.w3c.dom.Document d = token.getDomTree();
		Document doc = _domBuilder.build(d);
		Element root = doc.getRootElement();

		Object[] inPortArray = inputPortList().toArray();
		for (int i = 0; i < inPortArray.length; i++) {
			TypedIOPort port = (TypedIOPort) inPortArray[i];
			String name = port.getName();

			if (!name.equals(XML_IN_PORT_NAME) && port.getWidth() > 0) {
				_appendToDoc(root, port);
			}
		}

		XMLToken outToken = null;

		try {
			outToken = new XMLToken(_xo.outputString(doc));
		} catch (Exception e) {
			throw new IllegalActionException(this,
					"Exception creating XMLToken: " + e.getMessage());
		}

		output.broadcast(outToken);
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	private void _appendToDoc(Element root, TypedIOPort port)
			throws IllegalActionException {
		Token token = port.get(0);

		if (port.getType() instanceof ArrayType) {
			throw new IllegalActionException(this, "Arrays not yet supported");
		} else {
			_appendOneToken(root, port.getName(), token);
		}
	}

	private void _appendOneToken(Element root, String name, Token token)
			throws IllegalActionException {
		Type type = token.getType();
		String str = null;

		if (type == BaseType.XMLTOKEN) {
			throw new IllegalActionException(this, "xml not supported");
		} else if (type == BaseType.STRING) {
			str = ((StringToken) token).stringValue();
		} else {
			str = token.toString();
		}

		Element child = new Element(name);
		child.setText(str);
		root.addContent(child);
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	private final static String XML_IN_PORT_NAME = "input";

	private DOMBuilder _domBuilder;
	private XMLOutputter _xo;
}