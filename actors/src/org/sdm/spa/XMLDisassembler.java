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

import org.w3c.dom.Document;

import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.data.BooleanToken;
import ptolemy.data.Token;
import ptolemy.data.XMLToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// XMLDisassembler
/**
 * This actor disassembles an XML document into its child elements. The input
 * port name must match the document's root element name and is peeled off. Each
 * child element is sent to the output port with the same name.
 * 
 * @author Daniel Crawl
 * @version $Id: XMLDisassembler.java 24234 2010-05-06 05:21:26Z welker $
 */

public class XMLDisassembler extends TypedAtomicActor {

	/**
	 * Construct a XMLDisassembler source with the given container and name.
	 * 
	 * @param name
	 *            The name of this actor.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public XMLDisassembler(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		outputNil = new Parameter(this, "outputNil");
		outputNil.setTypeEquals(BaseType.BOOLEAN);
		outputNil.setExpression("false");

		arraysWrapped = new Parameter(this, "arraysWrapped");
		arraysWrapped.setTypeEquals(BaseType.BOOLEAN);
		arraysWrapped.setExpression("false");

		_helper = new XMLHelper(this);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");
	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/**
	 * If true, then each output port whose name is not a child element of the
	 * incoming XML document outputs a nil token.
	 */
	public Parameter outputNil = null;

	/**
	 * If true, then each element of an array is wrapped in an additional
	 * element.
	 */
	public Parameter arraysWrapped = null;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * React to a change in an attribute. Update the value if one of the
	 * parameters changed.
	 * 
	 * @param attribute
	 *            The changed parameter.
	 * @exception IllegalActionException
	 *                If the parameter set is not valid.
	 */
	public void attributeChanged(Attribute attribute)
			throws IllegalActionException {
		if (attribute == outputNil) {
			Token token = outputNil.getToken();
			_helper.setOutputNil(((BooleanToken) token).booleanValue());
		} else if (attribute == arraysWrapped) {
			Token token = arraysWrapped.getToken();
			_helper.setArraysWrapped(((BooleanToken) token).booleanValue());
		}
		super.attributeChanged(attribute);
	}

	/**
	 * Read the XML input and send pieces to the correct output ports.
	 * 
	 * @exception IllegalActionException
	 *                If it is thrown by the send() method sending out the
	 *                token.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		Object[] inPortArray = inputPortList().toArray();
		for (int i = 0; i < inPortArray.length; i++) {
			IOPort port = (IOPort) inPortArray[i];
			Document doc = ((XMLToken) port.get(0)).getDomTree();
			_helper.splitOutXML(port.getName(), doc, outputPortList(), null);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	private XMLHelper _helper = null;
}