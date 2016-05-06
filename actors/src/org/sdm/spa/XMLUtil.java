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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author zcheng
 * 
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public class XMLUtil {

	/**
	 * Constructor for XMLUtil.
	 */
	public XMLUtil() {

	}

	private String getItemValue(String in, String path) {

		Vector items = getItems(in, path);
		if (items.size() > 0) {
			return (String) items.elementAt(0);
		} else {
			return null;
		}

	}

	public Vector getItems(String input, String xpath) {
		Vector result = new Vector();
		String res = "";
		try {

			// Since we are not able to handle DTD types,
			// We will delete the head, any line that starts with <! will be
			// discarded.
			StringReader sr = new StringReader(input);
			BufferedReader br = new BufferedReader(sr);
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.trim().startsWith("<!"))
					sb.append(line + "\n");
			}
			input = sb.toString();

			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();

			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			ByteArrayInputStream bis = new ByteArrayInputStream(input
					.getBytes());

			Document document = builder.parse(bis);

			Element docElement;
			docElement = document.getDocumentElement();
			Element el = docElement;

			// here we will loop through the xpath
			StringTokenizer st = new StringTokenizer(xpath, "/");
			List items = null;
			String restpath = "";
			// skip the root
			if (st.hasMoreTokens())
				st.nextToken();

			while (st.hasMoreTokens()) {
				String val = st.nextToken();
				// We need to test whether currrent tag has
				// only one item or not.
				items = childElementList(el, val);
				if (items != null && items.size() > 1) {
					// here we need to extract from the rest path
					// Now we need to extract info from all items
					while (st.hasMoreTokens()) {
						restpath = restpath + "/" + st.nextToken();
					}

				} else {
					// only one item in the element
					el = firstChildElement(el, val);
				}

			}
			// extract information from the rest paths
			if (items != null && items.size() > 0) {
				Iterator iter = items.iterator();
				while (iter.hasNext()) {
					Element item = (Element) iter.next();
					String value = extractItem(item, restpath);
					if (value != null) {
						res = res + value + ":";
						// System.out.println(value);
						result.add(value);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Method extractItem. Extract info according to the xpath from the element.
	 * 
	 * @param el
	 * @param xpath
	 * @return String
	 */
	private String extractItem(Element el, String xpath) {
		StringTokenizer st = new StringTokenizer(xpath, "/");
		String result = null;
		while (st.hasMoreTokens()) {
			String val = st.nextToken();
			el = firstChildElement(el, val);
		}
		result = elementValue(el);

		return result;

	}

	/**
	 * Return the text (node value) of the first node under this, works best if
	 * normalized.
	 */
	public static String elementValue(Element element) {
		if (element == null)
			return null;
		// make sure we get all the text there...
		element.normalize();
		Node textNode = element.getFirstChild();

		if (textNode == null)
			return null;
		// should be of type text
		return textNode.getNodeValue();
	}

	/**
	 * Return a List of Element objects that have the given name and are
	 * immediate children of the given element; if name is null, all child
	 * elements will be included.
	 */
	private List childElementList(Element element, String childElementName) {
		if (element == null)
			return null;

		List elements = new LinkedList();
		Node node = element.getFirstChild();

		if (node != null) {
			do {
				if (node.getNodeType() == Node.ELEMENT_NODE
						&& (childElementName == null || childElementName
								.equals(node.getNodeName()))) {
					Element childElement = (Element) node;

					elements.add(childElement);
				}
			} while ((node = node.getNextSibling()) != null);
		}
		return elements;
	}

	private Element firstChildElement(Element element, String childElementName) {
		if (element == null)
			return null;
		// get the first element with the given name
		Node node = element.getFirstChild();

		if (node != null) {
			do {
				if (node.getNodeType() == Node.ELEMENT_NODE
						&& (childElementName == null || childElementName
								.equals(node.getNodeName()))) {
					Element childElement = (Element) node;

					return childElement;
				}
			} while ((node = node.getNextSibling()) != null);
		}
		return null;
	}

}