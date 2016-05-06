/*
 * Copyright (c) 2010 The Regents of the University of California.
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

/*
 * Created on Jul 12, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.ecoinformatics.seek.querybuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A set of uttiliy methods (all static)
 */
public class DBUIUtils {
	public static final String ALL_FIELDS = "*";
	public static final String NO_NAME = "  ";

	/**
	 * Helper method to fill combobox with the list of field names from a
	 * "named" table schema
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aTableName
	 *            the table name in the schema
	 * @param aCBX
	 *            the comboxbox
	 * @param aInclAstrick
	 *            if the table schema includes a field with an astrick "*" then
	 *            include it also
	 */
	public static void fillFieldCombobox(DSSchemaIFace aSchema,
			String aTableName, JComboBox aCBX, boolean aInclAstrick) {
		if (!aTableName.equals("  ")) {
			aCBX.removeAllItems();
			if (aInclAstrick) {
				aCBX.addItem("*");
			}

			Vector tables = aSchema.getTables();
			if (tables != null && tables.size() > 0) {
				for (Enumeration et = tables.elements(); et.hasMoreElements();) {
					DSTableIFace table = (DSTableIFace) et.nextElement();
					if (table.getName().equals(aTableName)) {
						Vector fields = table.getFields();
						for (Enumeration ef = fields.elements(); ef
								.hasMoreElements();) {
							DSTableFieldIFace field = (DSTableFieldIFace) ef
									.nextElement();
							if (aInclAstrick || !field.getName().equals("*")) {
								aCBX.addItem(field.getName());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Helper method to fill combobox with the list of table names from the
	 * schema
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aCBX
	 *            the comboxbox
	 */
	public static void fillTableCombobox(DSSchemaIFace aSchema, JComboBox aCBX) {
		Vector tables = aSchema.getTables();
		if (tables != null && tables.size() > 0) {
			for (Enumeration et = tables.elements(); et.hasMoreElements();) {
				DSTableIFace table = (DSTableIFace) et.nextElement();
				aCBX.addItem(table.getName());
			}
		}
	}

	/**
	 * Add brackets around names with spaces
	 * 
	 * @param aName
	 *            the name to be changed
	 * @return a name with brackets, but only if necessary
	 */
	public static String fixNameWithSpaces(String aName) {
		if (aName.indexOf(' ') != -1)
			return "[" + aName + "]";
		else
			return aName;
	}

	/**
	 * Returns the full table name/field name concatenated
	 * 
	 * @param aField
	 *            object for which to get the names
	 * @return the full name
	 */
	public static String getFullFieldName(DBTableField aField) {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append(fixNameWithSpaces(aField.getTable().getName()));
		strBuf.append(".");
		strBuf.append(fixNameWithSpaces(aField.getName()));
		return strBuf.toString();
	}

	/**
	 * Returns the full table name/field name concatenated
	 * 
	 * @param aTblName
	 *            the table name
	 * @param aFieldName
	 *            the field name
	 * @return the concatenated name
	 */
	public static String getFullFieldName(String aTblName, String aFieldName) {
		StringBuffer strBuf = new StringBuffer();
		strBuf.append(fixNameWithSpaces(aTblName));
		strBuf.append(".");
		strBuf.append(fixNameWithSpaces(aFieldName));
		return strBuf.toString();
	}

	/**
	 * Returns a table by name
	 * 
	 * @param aName
	 *            name of table to be found
	 * 	 */
	protected static DSTableIFace getTableByName(DSSchemaIFace aSchema,
			String aName) {
		if (aSchema != null) {
			Vector tables = aSchema.getTables();
			for (int i = 0; i < tables.size(); i++) {
				DSTableIFace table = (DSTableIFace) tables.elementAt(i);
				if (table.getName().equals(aName))
					return table;
			}
		}
		return null;
	}

	/**
	 * Returns a DBTableField object by name
	 * 
	 * @param aTable
	 *            table to look into
	 * @param aName
	 *            name of field to be found
	 * @return field object
	 */
	public static DSTableFieldIFace getFieldByName(DSTableIFace aTable,
			String aName) {
		if (aTable != null) {
			Vector fields = aTable.getFields();
			for (int i = 0; i < fields.size(); i++) {
				DSTableFieldIFace field = (DSTableFieldIFace) fields
						.elementAt(i);
				// System.out.println("["+field.getName()+"]["+aName+"]");
				if (field.getName().equals(aName))
					return field;
			}
		}
		return null;
	}

	/**
	 * Returns a DBTableField object by name
	 * 
	 * @param aSchema
	 *            schema
	 * @param aTableName
	 *            table name to look into
	 * @param aFieldName
	 *            name of field to be found
	 * @return field object
	 */
	public static DSTableFieldIFace getFieldByName(DSSchemaIFace aSchema,
			String aTableName, String aFieldName) {
		Vector tables = aSchema.getTables();
		if (tables != null && tables.size() > 0) {
			for (Enumeration et = tables.elements(); et.hasMoreElements();) {
				DSTableIFace table = (DSTableIFace) et.nextElement();
				if (table.getName().equals(aTableName)) {
					Vector fields = table.getFields();
					for (Enumeration ef = fields.elements(); ef
							.hasMoreElements();) {
						DSTableFieldIFace field = (DSTableFieldIFace) ef
								.nextElement();
						if (field.getName().equals(aFieldName)) {
							return field;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Removes the brackets from a name that has one of spaces in it
	 * 
	 * @param aName
	 *            the name
	 * @return the new name w/o the brackts
	 */
	private static String removeBrackets(String aName) {
		if (aName == null || aName.length() == 0)
			return "";

		int startInx = aName.indexOf("[");
		int endInx = aName.length();
		if (startInx != -1) {
			endInx = aName.indexOf("]");
			if (endInx == -1) {
				return "";
			}
			endInx--;
		} else {
			startInx = 0;
		}
		return aName.substring(startInx, endInx);
	}

	/**
	 * Returns true if the name is of format "table name"."field name" and the
	 * table name and field name are in the schema
	 * 
	 * @param aSchema
	 *            schema
	 * @param aName
	 *            the full name
	 * @param aTableName
	 *            can be null, or a stringbuffer that will return the name of
	 *            the table
	 * @return Returns true if the name is of format "table name"."field name"
	 *         and the table name and field name are in the schema
	 */
	public static DSTableFieldIFace isTableFieldName(DSSchemaIFace aSchema,
			String aName, StringBuffer aTableName) {
		int sepInx = aName.indexOf(".");
		if (sepInx == -1)
			return null;

		String tableName = removeBrackets(aName.substring(0, sepInx));
		String fieldName = removeBrackets(aName.substring(sepInx + 1, aName
				.length()));
		if (aTableName != null) {
			aTableName.setLength(0);
			aTableName.append(tableName);
		}
		return getFieldByName(aSchema, tableName, fieldName);
	}

	/**
	 * Returns the desired number of spaces for the depth (usually *2)
	 * 
	 * @param aDepth
	 *            the depth
	 * @return spaces
	 */
	public static String getSpaces(int aDepth) {
		StringBuffer strBuf = new StringBuffer();
		for (int i = 0; i < aDepth; i++) {
			strBuf.append("  ");
		}
		return strBuf.toString();
	}

	// ------------------------------------------------------------
	// ------------------------------------------------------------
	// -- Common DOM and XML Utilities
	// ------------------------------------------------------------
	// ------------------------------------------------------------

	/**
	 * ------------------------------------------------------------ Gets the
	 * String value of a node. First checks it's value and if that is null then
	 * it checks to see if it has a child node and gets the value of the first
	 * child. Assumption: That the first child is a #text node, delibertly NOT
	 * checking the first node's type
	 * 
	 * @param aNode
	 *            Parent to search (should be the document root)
	 * @return Returns the value of the node
	 *         --------------------------------------------------------------
	 */
	public static String getNodeValue(Node aNode) {
		String value = null;
		if (aNode.getNodeValue() != null) {
			value = aNode.getNodeValue() != null ? aNode.getNodeValue().trim()
					: null;
		} else {
			NodeList list = aNode.getChildNodes();
			if (list.getLength() == 1) {
				Node child = list.item(0);
				if (child != null) {
					value = child.getNodeValue() != null ? child.getNodeValue()
							.trim() : null;
				}
			}
		}
		return value;
	}

	/**
	 * ------------------------------------------------------------ Finds the
	 * first node of a given type
	 * 
	 * @param aNode
	 *            Parent to search (should be the document root)
	 * @param aName
	 *            Name of node to find
	 * @return Returns the node of that name or null
	 *         --------------------------------------------------------------
	 */
	public static Node findNode(Node aNode, String aName) {
		String name = aNode.getNodeName() != null ? aNode.getNodeName().trim()
				: "";
		if (aName.equalsIgnoreCase(name)) {
			return aNode;
		}

		NodeList list = aNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child != null) {
				Node node = findNode(child, aName);
				if (node != null) {
					return node;
				}
			}
		}
		return null;
	}

	/**
	 * ------------------------------------------------------------ Gets the
	 * value of the named node
	 * 
	 * @param aNode
	 *            Parent to search (should be the document root)
	 * @param aName
	 *            Name of node to find
	 * @return Returns the node's value as a string
	 *         --------------------------------------------------------------
	 */
	public static String findNodeValue(Node aNode, String aName) {
		String value = null;
		Node node = findNode(aNode, aName);
		if (node != null) {
			value = getNodeValue(node);
		}
		return value;
	}

	/**
	 * ------------------------------------------------------------ Gets an
	 * attribute value for the named node.
	 * 
	 * @param aNode
	 *            Parent to search (should be the document root)
	 * @param aName
	 *            Name of node to find
	 * @param aAttr
	 *            Name of attribute to return
	 * @return Returns the attribute's value as a string
	 *         --------------------------------------------------------------
	 */
	public static String findAttrValueForNode(Node aNode, String aName,
			String aAttr) {
		String value = null;
		Node node = findNode(aNode, aName);
		if (node != null) {
			NamedNodeMap map = node.getAttributes();
			if (map != null) {
				Node attrNode = map.getNamedItem(aAttr);
				if (attrNode != null) {
					value = getNodeValue(attrNode);
				}
			}
		}
		return value;
	}

	/**
	 * ------------------------------------------------------------ Gets an
	 * attribute value for the node.
	 * 
	 * @param aNode
	 *            Parent to search (should be the document root)
	 * @param aAttr
	 *            Name of attribute to return
	 * @return Returns the attribute's value as a string
	 *         --------------------------------------------------------------
	 */
	public static String findAttrValue(Node aNode, String aAttr) {
		String value = null;
		if (aNode != null) {
			NamedNodeMap map = aNode.getAttributes();
			if (map != null) {
				Node attrNode = map.getNamedItem(aAttr);
				if (attrNode != null) {
					value = getNodeValue(attrNode);
				}
			}
		}
		return value;
	}

	/**
	 * Looks up the attr and returns the int of it or returns -1
	 * 
	 * @param aNode
	 *            the node with the attr
	 * @return the int or returns -1
	 */
	public static int getIntAttrId(Node aNode, String aAttrName) {
		String idStr = findAttrValue(aNode, aAttrName);
		if (idStr != null && idStr.length() > 0) {
			try {
				int id = Integer.parseInt(idStr);
				if (id > -1 && id < Integer.MAX_VALUE) {
					return id;
				}
			} catch (NumberFormatException e) {
			}
		}
		return -1;
	}

	/**
	 * Prints a DOM Tree (recursive)
	 * 
	 * @param aNode
	 *            parent node of tree to be printed
	 * @param aLevel
	 *            indicates the current indentation level
	 */
	public static void printNode(Node aNode, int aLevel) {
		if (aNode == null) {
			return;
		}

		String spaces = "";
		for (int i = 0; i < aLevel; i++) {
			spaces += "..";
		}

		System.out.println(spaces + "Name:  " + aNode.getNodeName());
		System.out.println(spaces + "Type:  " + aNode.getNodeType());
		System.out.println(spaces + "Value: " + aNode.getNodeValue());
		NodeList list = aNode.getChildNodes();
		if (list != null) {
			for (int i = 0; i < list.getLength(); i++) {
				Node child = list.item(i);
				printNode(child, aLevel + 1);
			}
		}
	}

	/**
	 * Reads in an XML document and returns a String of the file's contents
	 * 
	 * @param aFileName
	 *            file name of XML file to be read
	 */
	public static String readXMLFile2Str(String aFileName) {
		try {
			FileReader fileReader = new FileReader(aFileName);
			BufferedReader bufReader = new BufferedReader(fileReader);
			StringBuffer strBuf = new StringBuffer();

			String line = bufReader.readLine();
			while (line != null) {
				strBuf.append(line);
				strBuf.append("\n");
				line = bufReader.readLine();
			}
			return strBuf.toString();

		} // try
		catch (Exception e) {
			System.err.println("readXMLFile2Str - Exception: " + e);
		}
		return null;
	}

	/**
	 * Reads in an XML document and returns the Document node of the DOM tree
	 * 
	 * @param aFileName
	 *            file name of XML file to be read
	 */
	public static Document readXMLFile2DOM(String aFileName) {
		File file = new File(aFileName);
		if (!file.exists())
			return null;
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();

			if (tFactory.getFeature(DOMSource.FEATURE)
					&& tFactory.getFeature(DOMResult.FEATURE)) {
				// Instantiate a DocumentBuilderFactory.
				DocumentBuilderFactory dFactory = DocumentBuilderFactory
						.newInstance();

				// And setNamespaceAware, which is required when parsing xsl
				// files
				dFactory.setNamespaceAware(true);

				// Use the DocumentBuilderFactory to create a DocumentBuilder.
				DocumentBuilder dBuilder = dFactory.newDocumentBuilder();

				// Use the DocumentBuilder to parse the XML input.
				Document xmlDoc = dBuilder.parse(aFileName);

				// printNode(xmlDoc, 0);

				return xmlDoc;

			}
		} // try
		catch (org.xml.sax.SAXParseException e) {
			System.err.println("Tried Reading[" + aFileName + "]");
			System.err.println("readXMLFile2DOM - Exception: " + e);
			// String xmlString = readXMLFile2Str(aFileName);
			// System.out.println("XML Dump " + aFileName);
			// System.out.println("------------------------------------------");
			// System.out.println(xmlString);
			// System.out.println("------------------------------------------");
		} catch (Exception e) {
			System.err.println("Tried Reading[" + aFileName + "]");
			System.err.println("readXMLFile2DOM - Exception: " + e);
		}
		return null;
	}

	/**
	 * Convert/Parses an XML string into a DOM tree
	 * 
	 * @param aXMLStr
	 *            XML string (document)
	 */
	public static Document convertXMLStr2DOM(String aXMLStr) {
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();

			if (tFactory.getFeature(DOMSource.FEATURE)
					&& tFactory.getFeature(DOMResult.FEATURE)) {
				// Instantiate a DocumentBuilderFactory.
				DocumentBuilderFactory dFactory = DocumentBuilderFactory
						.newInstance();

				// And setNamespaceAware, which is required when parsing xsl
				// files
				dFactory.setNamespaceAware(true);

				// Use the DocumentBuilderFactory to create a DocumentBuilder.
				DocumentBuilder dBuilder = dFactory.newDocumentBuilder();

				StringReader strReader = new StringReader(aXMLStr);
				InputSource inpSrc = new InputSource(strReader);

				// Use the DocumentBuilder to parse the XML input.
				Document xmlDoc = dBuilder.parse(inpSrc);

				return xmlDoc;

			}
		} // try
		catch (Exception e) {
			System.err.println("convertXMLStr2DOM - Exception: " + e);
			System.err.println("XML Dump");
			System.err.println("------------------------------------------");
			System.err.println(aXMLStr);
			System.err.println("------------------------------------------");
			e.printStackTrace();
		}
		return null;
	}

}
