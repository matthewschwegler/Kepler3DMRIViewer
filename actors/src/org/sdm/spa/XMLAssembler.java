/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
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

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.XMLToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// XMLAssembler
/**
 * 
 * On each firing, read one token from each input port and assemble them into an
 * XML document where the root element name is specified by the output port
 * name.
 * 
 * @author Daniel Crawl
 * @version $Id: XMLAssembler.java 24234 2010-05-06 05:21:26Z welker $
 */
public class XMLAssembler extends TypedAtomicActor {

	/**
	 * Construct a XMLAssembler source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public XMLAssembler(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		inputNil = new Parameter(this, "inputNil");
		inputNil.setTypeEquals(BaseType.BOOLEAN);
		inputNil.setExpression(String.valueOf(_inputNilVal));

		encloseInputPortName = new Parameter(this, "encloseInputPortName");
		encloseInputPortName.setTypeEquals(BaseType.BOOLEAN);
		encloseInputPortName.setExpression(String
				.valueOf(_encloseInputPortNameVal));

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * If true, then for each unconnected input port an element is created in
	 * the output document with an attribute nil whose value is "true".
	 */
	public Parameter inputNil = null;

	/**
	 * If true, then each token received will be added to an element with the
	 * name of the input port. By setting it false, input XML documents can be
	 * merged into a single document.
	 */
	public Parameter encloseInputPortName = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * React to a change in an attribute.
	 * 
	 * @param attribute
	 *            The changed parameter.
	 * @exception IllegalActionException
	 *                If the parameter set is not valid.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == inputNil) {
			Token token = inputNil.getToken();
			_inputNilVal = ((BooleanToken) token).booleanValue();
		} else if (attribute == encloseInputPortName) {
			Token token = encloseInputPortName.getToken();
			_encloseInputPortNameVal = ((BooleanToken) token).booleanValue();
		}
		super.attributeChanged(attribute);
	}

	/**
	 * Set all output ports whose types have not been set to XMLToken.
	 * 
	 * @exception IllegalActionException
	 */
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();

		Object[] portArray = outputPortList().toArray();
		for (int i = 0; i < portArray.length; i++) {
			TypedIOPort port = (TypedIOPort) portArray[0];
			if (port.getType() == BaseType.UNKNOWN) {
				port.setTypeEquals(BaseType.XMLTOKEN);
			}
		}
	}

	/**
	 * Read each input port, assemble the XML document, and send it to all
	 * output ports.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		StringBuffer xmlStr = new StringBuffer();

		// read a value from each port into an xml string
		Object[] inPortArray = inputPortList().toArray();
		for (int i = 0; i < inPortArray.length; i++) {
			TypedIOPort port = (TypedIOPort) inPortArray[i];
			String name = port.getName();

			// see if there's nothing connected and we allow nils
			if (_inputNilVal && port.getWidth() == 0) {
				xmlStr.append("<" + name + " nil=\"true\"/>");
			} else {
				_addTokenStr(xmlStr, port);
			}
		}

		// create documents and send for each output port
		Object[] outPortArray = outputPortList().toArray();
		for (int i = 0; i < outPortArray.length; i++) {
			TypedIOPort port = (TypedIOPort) outPortArray[i];
			String name = port.getName();

			String outputStr = "<" + name + ">" + xmlStr.toString() + "</"
					+ name + ">";

			Token token = null;

			try {
				token = new XMLToken(outputStr);
			} catch (Exception e) {
				throw new IllegalActionException(this,
						"XMLAssembler: unable to create XMLToken with string: "
								+ xmlStr);
			}

			port.broadcast(token);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	/**
	 * Consume and add the token to a string buffer.
	 * 
	 * @param buf
	 *            the buffer
	 * @param port
	 *            input port that supplies the token
	 */
	private void _addTokenStr(StringBuffer buf, TypedIOPort port)
			throws IllegalActionException {
		Token token = port.get(0);

		if (port.getType() instanceof ArrayType) {
			Token[] array = ((ArrayToken) token).arrayValue();
			for (int i = 0; i < array.length; i++) {
				_addOneTokenStr(buf, port.getName(), array[i]);
			}
		} else {
			_addOneTokenStr(buf, port.getName(), token);
		}
	}

	/**
	 * Add a single value from a token, optionally enclosing it in the port's
	 * name, to a StringBuffer.
	 * 
	 * @param buf
	 *            the buffer
	 * @param portName
	 *            the name of the input port
	 * @param token
	 *            the token
	 */
	private void _addOneTokenStr(StringBuffer buf, String portName, Token token) {
		if (_encloseInputPortNameVal) {
			buf.append("<" + portName + ">");
		}

		// if it's a string token, do a stringValue so we don't get
		// the quotes
		Type tokenType = token.getType();
		if (tokenType == BaseType.STRING) {
			buf.append(((StringToken) token).stringValue());
		} else {
			String tokenStr = token.toString();

			// see if xml token with beginning <?xml
			if (tokenType == BaseType.XMLTOKEN
					&& tokenStr.indexOf("<?xml") == 0) {
				tokenStr = tokenStr.substring(tokenStr.indexOf(">") + 1);
			}

			buf.append(tokenStr);
		}

		if (_encloseInputPortNameVal) {
			buf.append("</" + portName + ">");
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	// whether to add element with nil="true" attribute for unconnected
	// input ports. (see inputNil)
	private boolean _inputNilVal = true;

	// whether to put each token value read into an element with the same
	// name as input port. (see encloseInputPortName)
	private boolean _encloseInputPortNameVal = true;
}