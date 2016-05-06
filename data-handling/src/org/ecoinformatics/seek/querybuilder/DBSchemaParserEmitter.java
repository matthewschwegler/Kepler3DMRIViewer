/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.querybuilder;

import java.util.Enumeration;
import java.util.Vector;

import org.kepler.objectmanager.data.db.DSSchemaDef;
import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableDef;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;
import org.kepler.objectmanager.data.db.DSTableKeyIFace;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class can generate XML from a DSSchemaIFace object or create a schema
 * from and XML dcoument
 */
public class DBSchemaParserEmitter {
	private static final String[] KEY_DESC = { "", "PRIMARYKEY", "SECONDARYKEY" };
	private static String MISSINGVALUELIST = "missingValueCodeList";
	private static String MISSINGVALUECODE = "missingValueCode";

	/**
	 * Create a DSSchemaIFace object from an XML DOM
	 * 
	 * @param aDoc
	 *            the DOM
	 * @return the schema object
	 */
	public static DSSchemaIFace processDOM(Document aDoc) {
		DSSchemaDef schemaDef = new DSSchemaDef();
		Node schemaNode = DBUIUtils.findNode(aDoc, "schema");
		if (schemaNode == null) {
			System.out.println("*** Error DOM is missing its schema node!");
			schemaDef = null;
			return null;
		}

		NodeList tableList = schemaNode.getChildNodes();
		if (tableList != null) {
			for (int i = 0; i < tableList.getLength(); i++) {
				Node tableNode = tableList.item(i);
				if (tableNode.getNodeType() != Node.TEXT_NODE) {
					String nodeName = tableNode.getNodeName();
					if (nodeName != null && nodeName.equals("table")) {
						String name = DBUIUtils
								.findAttrValue(tableNode, "name");
						if (name != null && name.length() > 0) {
							DSTableDef tableDef = new DSTableDef(name);
							schemaDef.addTable(tableDef);
							NodeList fieldList = tableNode.getChildNodes();
							if (fieldList != null) {
								for (int j = 0; j < fieldList.getLength(); j++) {
									Node field = fieldList.item(j);
									if (field != null
											&& field.getNodeType() != Node.TEXT_NODE) {
										String fldName = DBUIUtils
												.findAttrValue(field, "name");
										if (fldName == null
												|| fldName.length() == 0) {
											System.out
													.println("*** Error field DOM node is missing its name!");
											return null;
										}
										String dataType = DBUIUtils
												.findAttrValue(field,
														"dataType");
										if (dataType == null
												|| dataType.length() == 0) {
											System.out
													.println("*** Error field DOM node is missing its data type!");
											return null;
										}

										// handle missing value
										Node missingValueList = DBUIUtils
												.findNode(field,
														MISSINGVALUELIST);
										Vector missingValueCodeVector = new Vector();
										// System.out.println("before parsing missing value in schema parser");
										if (missingValueList != null) {
											// System.out.println("after missingValue list is not null1");
											NodeList missingValue = missingValueList
													.getChildNodes();
											// System.out.println("after get missing value list kids2");
											if (missingValue != null) {
												// System.out.println("after missingvalue list is not null3");
												for (int k = 0; k < missingValue
														.getLength(); k++) {
													// System.out.println("in missing value element4");
													Node missingcodeNode = missingValue
															.item(k);
													String missingCode = DBUIUtils
															.findNodeValue(
																	missingcodeNode,
																	MISSINGVALUECODE);
													// System.out.println("the missing value code add to vector "+missingCode);
													missingValueCodeVector
															.add(missingCode);
												}
											}
										}

										String keyType = DBUIUtils
												.findAttrValue(field, "keyType");
										if (keyType != null) {
											if (keyType
													.equals(KEY_DESC[DSTableKeyIFace.PRIMARYKEY])) {
												tableDef.addPrimaryKey(fldName,
														dataType,
														missingValueCodeVector);
											} else if (keyType
													.equals(KEY_DESC[DSTableKeyIFace.SECONDARYKEY])) {
												tableDef.addSecondaryKey(
														fldName, dataType,
														missingValueCodeVector);
											} else {
												tableDef.addField(fldName,
														dataType,
														missingValueCodeVector);
											}
										} else {
											tableDef.addField(fldName,
													dataType,
													missingValueCodeVector);
										}
									}
								}
							}
						} else {
							System.out
									.println("*** Error table DOM node is missing its name!");
							return null;
						}
					} else {
						System.out
								.println("*** Error chiuld of schema is not \"table\"!");
						return null;
					}
				}
			}
		}
		return schemaDef;
	}

	/**
	 * Reads in a Schema Definition and returns a DSSchemaIFace object
	 * 
	 * @param aFileName
	 *            Name of XML file representing a XML document
	 * @return the DSSchemaIFace object
	 */
	public static DSSchemaIFace readSchemaDef(String aFileName) {

		DSSchemaIFace schemaDef = null;
		try {
			schemaDef = processDOM(DBUIUtils.readXMLFile2DOM(aFileName));
			// Debug
			/*
			 * if (schemaDef != null) {
			 * System.out.println("[\n"+emitXML(schemaDef)+"\n]\n"); }
			 */
		} catch (Exception e) {
			System.err.println(e);
		}

		return schemaDef;
	}

	/**
	 * Parses an XML String representing a Schema Def and returns a
	 * DSSchemaIFace object
	 * 
	 * @param aXMLSchemaStr
	 *            String representing a XML document
	 * @return the DSSchemaIFace object
	 */
	public static DSSchemaIFace parseSchemaDef(String aXMLSchemaStr) {

		DSSchemaIFace schemaDef = null;
		try {
			// System.out.println("the schema string is "+aXMLSchemaStr);
			schemaDef = processDOM(DBUIUtils.convertXMLStr2DOM(aXMLSchemaStr));
			// Debug
			/*
			 * if (schemaDef != null) {
			 * System.out.println("[\n"+emitXML(schemaDef)+"\n]\n"); }
			 */
		} catch (Exception e) {
			System.err.println(e);
		}

		return schemaDef;
	}

	/**
	 * Generate XML for a field schema
	 * 
	 * @param aStrBuf
	 *            the buffer to append to
	 * @param aField
	 *            the objec to be emitted
	 */
	protected static void emit(StringBuffer aStrBuf, DSTableFieldIFace aField) {
		if (aField != null) {
			aStrBuf.append("    <field name=\"" + aField.getName() + "\"");
			aStrBuf.append(" dataType=\"" + aField.getDataType() + "\"");
			if (aField instanceof DSTableKeyIFace
					&& ((DSTableKeyIFace) aField).getKeyType() != DSTableKeyIFace.UNDEFINEDKEY) {
				aStrBuf.append(" keyType=\""
						+ KEY_DESC[((DSTableKeyIFace) aField).getKeyType()]
						+ "\"");
			}
			Vector missingValueVector = aField.getMissingValueCode();
			if (missingValueVector != null && !missingValueVector.isEmpty()) {
				aStrBuf.append(">\n");
				aStrBuf.append("       <" + MISSINGVALUELIST + ">\n");
				for (int i = 0; i < missingValueVector.size(); i++) {
					String code = (String) missingValueVector.elementAt(i);
					aStrBuf.append("           <" + MISSINGVALUECODE + ">");
					aStrBuf.append(code);
					aStrBuf.append("</" + MISSINGVALUECODE + ">\n");
				}
				aStrBuf.append("       </" + MISSINGVALUELIST + ">\n");
				aStrBuf.append("    </field>\n");
			} else {
				aStrBuf.append("/>\n");
			}
		}
	}

	/**
	 * Generate XML for the schema
	 * 
	 * @param aSchema
	 *            the schema
	 * @return string of the schema's xml representation
	 */
	public static String emitXML(DSSchemaIFace aSchema) {
		if (aSchema == null)
			return "";

		StringBuffer strBuf = new StringBuffer("<schema>\n");

		Vector tables = aSchema.getTables();
		if (tables != null && tables.size() > 0) {
			for (Enumeration et = tables.elements(); et.hasMoreElements();) {
				DSTableIFace table = (DSTableIFace) et.nextElement();
				strBuf.append("  <table name=\"" + table.getName() + "\">\n");
				Vector fields = table.getFields();
				for (Enumeration ef = fields.elements(); ef.hasMoreElements();) {
					DSTableFieldIFace field = (DSTableFieldIFace) ef
							.nextElement();
					emit(strBuf, field);
				}
				strBuf.append("  </table>\n");
			}
		}
		strBuf.append("</schema>\n");

		return strBuf.toString();
	}

}