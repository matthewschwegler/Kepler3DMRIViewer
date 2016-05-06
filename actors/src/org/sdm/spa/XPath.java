/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-05-04 18:42:47 -0700 (Fri, 04 May 2012) $' 
 * '$Revision: 29810 $'
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

import java.io.StringWriter;
import java.io.Writer;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.SerializerFactory;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.Transformer;
import ptolemy.actor.parameters.PortParameter;
import ptolemy.data.ArrayToken;
import ptolemy.data.StringToken;
import ptolemy.data.XMLToken;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// XPath
/**
 * XPath selects XML nodes based on the XPath syntax.
 * 
 * This actor takes in an XPath string and an XMLToken, and it returns an array
 * of XMLTokens.
 * 
 * @author xiaowen
 * @version $Id: XPath.java 29810 2012-05-05 01:42:47Z crawl $
 */

public class XPath extends Transformer {

	/**
	 * Construct an XPath actor with the given container and name.
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
	public XPath(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {

		super(container, name);

		portXPath = new PortParameter(this, "xpath");

		all = new TypedIOPort(this, "all", false, true);

		input.setTypeEquals(BaseType.XMLTOKEN);
		output.setTypeEquals(new ArrayType(BaseType.XMLTOKEN));
		all.setTypeEquals(new ArrayType(BaseType.STRING));
		portXPath.setTypeEquals(BaseType.STRING);

		_attachText("_iconDescription", "<svg>\n" + "<rect x=\"0\" y=\"0\" "
				+ "width=\"60\" height=\"20\" " + "style=\"fill:white\"/>\n"
				+ "</svg>\n");

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	/** The XPath expression. */
	public PortParameter portXPath;

	/** The text resulting from applying the XPath expression. */
	public TypedIOPort all;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////


	/**
	 * Take in an XMLToken and the XPath expression, and return an ArrayToken
	 * containing XMLTokens representing the result of selecting nodes using the
	 * XPath expression.
	 * 
	 * @exception IllegalActionException
	 *                If it can't select nodes using the XPath expression or if
	 *                it's unable to create the resulting XMLTokens.
	 */
	public void fire() throws IllegalActionException {
		super.fire();

		// get inputs
		XMLToken tokenXml = (XMLToken) input.get(0);
		portXPath.update();
		String strXPath = ((StringToken) portXPath.getToken()).stringValue();
		_debug("The XPath expression is: " + strXPath);

        XMLToken xmlTokens[] = null;
        StringToken stringTokens[] = null;

		// run XPath on it
		Document document = tokenXml.getDomTree();
		if(document == null) {
		    throw new IllegalActionException(this, "Input has no XML: " + tokenXml);
		}
		
		synchronized(document) {
		
    		Element element = document.getDocumentElement();
    		if(element == null) {
    		    throw new IllegalActionException(this, "No document element in: " + tokenXml);
    		}
    		Node nodeRoot = element.getFirstChild();
    		NodeList nodeHits = null;
    
    		try {
    			nodeHits = XPathAPI.selectNodeList(nodeRoot, strXPath);
    		} catch (javax.xml.transform.TransformerException e) {
    			throw new IllegalActionException(this, "XPath: could not select nodes.");
    		} catch(NullPointerException e2) {
    		    throw new IllegalActionException(this, "Error selecting node list for: " + tokenXml);
    		}
    
    		// format the results
    		int numXMLTokens = 0;
    		xmlTokens = new XMLToken[nodeHits.getLength()];
    		stringTokens = new StringToken[nodeHits.getLength()];
    
    		SerializerFactory serializerFactory = SerializerFactory
    				.getSerializerFactory("xml");
    		OutputFormat outputFormat = new OutputFormat();
    		outputFormat.setOmitXMLDeclaration(true);
    		XMLSerializer xmlSerializer = (XMLSerializer) serializerFactory
    				.makeSerializer(outputFormat);
    
    		for (int i = 0; i < nodeHits.getLength(); i++) {
    			Writer stringWriter = new StringWriter();
    			xmlSerializer.setOutputCharStream(stringWriter);
    
    			Node node = nodeHits.item(i);
    
    			if (node.getNodeType() == Node.ELEMENT_NODE) {
    				numXMLTokens++;
    				try {
    					xmlSerializer.serialize((Element) node);
    				} catch (java.io.IOException e) {
    					throw new IllegalActionException(this,
    							"XPath: java.io.IOException ...");
    				}
    
    				String xmlStr = stringWriter.toString();
    
    				try {
    					xmlTokens[i] = new XMLToken(xmlStr);
    				} catch (java.lang.Exception e) {
    					throw new IllegalActionException(this,
    							"XPath: unable to create XMLToken with string: "
    									+ stringWriter.toString());
    				}
    
    				stringTokens[i] = new StringToken(xmlStr);
    			} else if (node.getNodeType() == Node.ATTRIBUTE_NODE
    					|| node.getNodeType() == Node.TEXT_NODE) {
    				stringTokens[i] = new StringToken(node.getNodeValue());
    			}
    		}
    
    		// If there were no results, then send out a dummy xml token.
    		// This is really a hack. The ideal solution for this actor would be
    		// able to send out an empty ArrayToken.
    		if (numXMLTokens < xmlTokens.length) {
    			xmlTokens = new XMLToken[1];
    			try {
    				xmlTokens[0] = new XMLToken("<dummy/>");
    			} catch (java.lang.Exception e) {
    				// well this really shouldn't happen.
    				throw new IllegalActionException(this,
    						"XPath: '<dummy/>' apparently isn't valid XML");
    			}
    		}

		}
		
		// send out the results
		if(xmlTokens.length == 0) {
		    output.send(0, new ArrayToken(BaseType.XMLTOKEN));
	    } else {
	        output.send(0, new ArrayToken(xmlTokens));
	    }

		// if no results, send empty array
		if (stringTokens.length == 0) {
			all.broadcast(new ArrayToken(BaseType.STRING));
		} else {
			all.broadcast(new ArrayToken(stringTokens));
		}
	}

}