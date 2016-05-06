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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

//////////////////////////////////////////////////////////////////////////
////GeonXMLUtil
/**
 * @author Efrat Jaeger
 * @version $Id: GeonXMLUtil.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 3.0.2
 */
public class GeonXMLUtil {

	public GeonXMLUtil() {

	}

	public Vector getAttrValue(File input, String tag, String attribute) {
		Vector stringAttr = new Vector();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream is = new FileInputStream(input);
			Document doc = builder.parse(is);

			NodeList nodes = doc.getElementsByTagName(tag);
			for (int i = 0; i < nodes.getLength(); i++) {
				String attr = ((Element) nodes.item(i)).getAttribute(attribute);
				stringAttr.addElement(attr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stringAttr;
	}

	public Vector getElementsById(File input, String tag, String attribute) { // temporary
																				// function
																				// for
																				// text
																				// tags.
		Vector stringAttr = new Vector();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream is = new FileInputStream(input);
			Document doc = builder.parse(is);

			NodeList nodes = doc.getElementsByTagName(tag);
			for (int i = 0; i < nodes.getLength(); i++) {
				String attr = ((Element) nodes.item(i)).getAttribute(attribute);
				int beginInd = attr.indexOf('(');
				int endInd = attr.indexOf(')');
				attr = attr.substring(beginInd + 2, endInd - 1);
				// System.out.println("attr = " + attr);
				Element elem = doc.getElementById(attr);
				// System.out.println(elem.getFirstChild().getNodeValue());
				stringAttr.addElement(elem.getFirstChild().getNodeValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stringAttr;
	}

	public String getElementsByTagId(File input, String tag, String id) { // temporary
																			// function
																			// for
																			// text
																			// tags.
		Vector stringAttr = new Vector();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream is = new FileInputStream(input);
			Document doc = builder.parse(is);

			NodeList nodes = doc.getElementsByTagName(tag);
			for (int i = 0; i < nodes.getLength(); i++) {
				String _id = ((Element) nodes.item(i)).getAttribute("id");
				if (id.equals(_id)) {
					String value = ((Element) nodes.item(i)).getAttribute(id);
					return value;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

}