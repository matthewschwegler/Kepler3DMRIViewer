/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-04-17 14:39:59 -0700 (Tue, 17 Apr 2012) $' 
 * '$Revision: 29739 $'
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

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.jdom.output.XMLOutputter;

import ptolemy.actor.AtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.LongToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.UnsignedByteToken;
import ptolemy.data.XMLToken;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;

//////////////////////////////////////////////////////////////////////////
//// XMLHelper

/**
 * This splits an XML document into its child elements and each is sent to the
 * output port with the same name. It is used by both XMLDisassembler and the
 * new web service actor. (The latter uses this class so that when the web
 * service does not have complex return types, it can output them directly [like
 * the original WebServiceActor]).
 * 
 * @author Daniel Crawl
 * @version $Id: XMLHelper.java 29739 2012-04-17 21:39:59Z crawl $
 */

public class XMLHelper {
	/**
	 * Initialize helper classes.
	 * 
	 * @param actor
	 *            the using this class
	 */
	public XMLHelper(AtomicActor actor) {
		_actor = actor;
		_outputter = new XMLOutputter();
		_builder = new DOMBuilder();
	}

	/** Set the value of _outputNilVal. */
	public void setOutputNil(boolean val) {
		_outputNilVal = val;
	}

	/** Set the value of _arraysWrappedVal. */
	public void setArraysWrapped(boolean val) {
		_arraysWrappedVal = val;
	}

	/**
	 * Peel off the root element from an XML document, and output the content in
	 * each child element to the output port with same name.
	 * 
	 * @param name
	 *            the root element name in the XML document to peel off
	 * @param doc
	 *            the XML document
	 * @param outputPorts
	 *            the output ports
	 * @param portPrepend
	 *            optional string to remove from beginning of port name
	 */
	public void splitOutXML(String name, org.w3c.dom.Document doc,
			List outputPorts, String portPrepend) throws IllegalActionException {
		Document jdomDoc = _builder.build(doc);
		Element rootEl = jdomDoc.getRootElement();

		/*
		 * System.out.println("peeling for " + name);
		 * System.out.println("rootel name " + rootEl.getName());
		 * System.out.println("xml = " + _outputter.outputString(jdomDoc));
		 */

		// remove all the name spaces so the getChildren() call below
		// retrieves all the children
		List k = rootEl.getChildren();
		for (int z = 0; z < k.size(); z++) {
			Element e = (Element) k.get(z);
			e.setNamespace(null);
			// System.out.println("rm ns for " + e.getName());
			// System.out.println("z " + z + " name |" + e.getName() + "|");
			// System.out.println("z " + z + " vat " + e.getText());
		}

		// make sure document root element name matches port name
		if (!rootEl.getName().equals(name)) {
			throw new IllegalActionException(_actor, "No xml input with name "
					+ name);
		}

		// output each part to the corresponding port
		Object[] portArray = outputPorts.toArray();
		for (int i = 0; i < portArray.length; i++) {
			TypedIOPort port = (TypedIOPort) portArray[i];
			String portName = port.getName();
			Type portType = port.getType();

			Token token = null;

			// see if the port's name begins with the port prepend string
			if (portPrepend != null && portName.startsWith(portPrepend)) {
				// remove port prepend string from port name
				portName = portName.substring(portPrepend.length());
			}

			// System.out.println("looking for child " + portName);

			// get the children matching this port name
			List kids = rootEl.getChildren(portName);

			if (kids.size() == 0) {
				System.out.println("WARNING: no xml child for " + portName);
			} else if (((Element) kids.get(0)).getAttribute("href") != null) {
				System.out
						.println("WARNING: multi-reference values not supported.");
			} else if (portType instanceof ArrayType) {
				Type subType = ((ArrayType) portType).getElementType();

				// see if wrapped
				if (_arraysWrappedVal) {
					// remove the additional element
					kids = ((Element) kids.get(0)).getChildren();
				}

				Token[] array = new Token[kids.size()];
				if(array.length == 0) {
				    token = new ArrayToken(subType);
				} else {
    				for (int j = 0; j < kids.size(); j++) {
    					array[j] = _makeToken(subType, (Element) kids.get(j));
    				}
    				token = new ArrayToken(array);
				}
			} else if (kids.size() > 1) {
				throw new IllegalActionException(_actor, "XML part " + portName
						+ " contained array, but port is not arrayType");
			} else {
				token = _makeToken(portType, (Element) kids.get(0));
			}

			if (token == null) {
				System.out.println("null token");
			} else {
				port.broadcast(token);
			}
		}
	}

	/**
	 * Create a token from an Element matching the type of output port.
	 * 
	 * @param portType
	 *            the type
	 * @param child
	 *            the data
	 * @return the token
	 */
	private Token _makeToken(Type portType, Element child)
			throws IllegalActionException {
		Token retval = null;

		// see if it is nil
		String nilStr = child.getAttributeValue("nil");
		if (nilStr != null && nilStr.equals("true")) {
			if (!_outputNilVal) {
				throw new IllegalActionException(_actor,
						"Got nil value but not configured to output them.");
			} else {
				retval = Token.NIL;
			}
		} else {
			String valStr = null;

			// get a string of the data
			if (portType == BaseType.XMLTOKEN) {
				valStr = "<?xml version=\"1.0\"?>"
						+ _outputter.outputString(child);
			} else {
				valStr = child.getText();
			}

			// System.out.println("make token " + portType + " for "
			// + child.getName() + " : " + valStr);

			// create a token based on the type using the data
			if (portType == BaseType.STRING) {
				retval = new StringToken(valStr);
			} else if (portType == BaseType.BOOLEAN) {
				retval = new BooleanToken(valStr);
			} else if (portType == BaseType.INT) {
				retval = new IntToken(valStr);
			} else if (portType == BaseType.DOUBLE) {
				retval = new DoubleToken(valStr);
			} else if (portType == BaseType.LONG) {
				retval = new LongToken(valStr);
			} else if (portType == BaseType.UNSIGNED_BYTE) {
				retval = new UnsignedByteToken(valStr);
			} else if (portType == BaseType.XMLTOKEN) {
				try {
					retval = new XMLToken(valStr);
				} catch (Exception e) {
					// e.printStackTrace();
					throw new IllegalActionException(_actor,
							"XMLToken constructor exception: " + e.getMessage());
				}
			}
		}
		return retval;
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	// whether we output nil tokens for nil="true" attributes
	private boolean _outputNilVal = false;

	// whether array elements are wrapped in an additional element
	private boolean _arraysWrappedVal = false;

	// used to create jdom documents from dom documents.
	private DOMBuilder _builder = null;

	// used to get the string value of an Element
	private XMLOutputter _outputter = null;

	// the actor that's using this class
	private AtomicActor _actor = null;
}