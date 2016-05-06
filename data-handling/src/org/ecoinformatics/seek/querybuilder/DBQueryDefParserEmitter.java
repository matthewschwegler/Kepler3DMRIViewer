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

import java.awt.Point;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableFieldIFace;
import org.kepler.objectmanager.data.db.DSTableIFace;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is class is used to parse XML and create a DBQueryDef object and/or emit
 * XML from a DBQuery object<br>
 * Call "readQueryDef" for reading in a query<br>
 * Call "emitXML" to generate the XML <br>
 */
public class DBQueryDefParserEmitter {
	private static final int SELECT_TYPE = 0;
	private static final int TABLE_TYPE = 1;
	private static final int WHERE_TYPE = 2;

	/** query was built OK **/
	public static final int QUERY_OK = 0;
	/** error building query **/
	public static final int QUERY_ERROR = 1;
	/** the XML was missing a "query" element **/
	public static final int NO_QUERY_NODE = 2;
	/** a table name was used that could not be found in the schema **/
	public static final int BAD_TABLENAME = 3;
	/** a field name was used that could not be found in the schema **/
	public static final int BAD_FIELDNAME = 4;

	/** description of the error codes **/
	private static final String[] ERROR_DESC = { "OK", "Query in invalid",
			"No \"query\" element", "Table name is not in schema.",
			"Field name is not in tables's schema." };
	public static final String WILDCARD = "*";

	/** the error code **/
	private static int mErrorCode = QUERY_OK;

	private static Hashtable mMappedNameHash = null;

	/**
	 * Return the text for the error code
	 * 
	 * @return a string representing the error
	 */
	public static String getErrorCodeText() {
		return ERROR_DESC[mErrorCode];
	}

	// --------------------------------------------------------------
	// -- XML Consumption
	// --------------------------------------------------------------

	/**
	 * Processes the where node
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aNode
	 *            the node
	 * @param aParent
	 *            the node's parent
	 * @return a where object (tree)
	 */
	protected static DBWhereIFace processWhereNode(DSSchemaIFace aSchema,
			Node aNode, DBWhereOperator aParent) {
		DBWhereIFace whereObj = null;
		String nodeName = aNode.getNodeName();
		if (nodeName.equals(DBWhereOperator.AND_OPER)
				|| nodeName.equals(DBWhereOperator.OR_OPER)) {
			DBWhereOperator oper = new DBWhereOperator(aParent, false);
			if (aParent != null) {
				aParent.append(oper);
			}

			oper.setName(nodeName);
			whereObj = oper;
			NodeList list = aNode.getChildNodes();
			if (list != null) {
				for (int i = 0; i < list.getLength(); i++) {
					if (list.item(i).getNodeType() != Node.TEXT_NODE) {
						if (processWhereNode(aSchema, list.item(i), oper) == null) {
							return null;
						}
					}
				}
			}
		} else if (nodeName.equals("field")) {
			String tblName = DBUIUtils.findAttrValue(aNode, "tableName");
			DSTableIFace tblIFace = DBUIUtils.getTableByName(aSchema, tblName);
			if (tblIFace == null) {
				mErrorCode = BAD_TABLENAME;
				return null;
			}

			String fldName = DBUIUtils.findAttrValue(aNode, "fieldName");
			DSTableFieldIFace field = DBUIUtils.getFieldByName(tblIFace,
					fldName);
			if (field != null) {
				DBWhereCondition cond = new DBWhereCondition(aParent,
						getMappedTableName(tblName), fldName, field
								.getDataType());
				String operStr = DBUIUtils.findAttrValue(aNode, "oper");
				if (operStr != null)
					cond.setOperator(operStr);

				String criteriaStr = DBUIUtils.findAttrValue(aNode, "criteria");
				if (criteriaStr != null)
					cond.setCriteria(criteriaStr);

				// If the parent null then this "should" be the only item in the
				// where clause
				if (aParent != null) {
					aParent.append(cond);
				}

				whereObj = cond; // must set the return var or it will have been
									// considered that it failed.

			} else {
				mErrorCode = BAD_FIELDNAME;
				return null;
			}

		}
		return whereObj;
	}

	/**
	 * Creates a new DBTableField item from a DOM Node
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aNode
	 *            creates a model item
	 * @return the DBSelectTableModelItem item
	 */
	private static DBTableField createFieldFromNode(DSSchemaIFace aSchema,
			Node aNode) {
		String tblName = DBUIUtils.findAttrValue(aNode, "tableName");
		DSTableIFace tblIFace = DBUIUtils.getTableByName(aSchema, tblName);
		if (tblIFace == null) {
			mErrorCode = BAD_TABLENAME;
			return null;
		}
		String fldName = DBUIUtils.findAttrValue(aNode, "fieldName");
		DSTableFieldIFace fldIFace = DBUIUtils
				.getFieldByName(tblIFace, fldName);
		if (fldIFace == null) {
			mErrorCode = BAD_TABLENAME;
			return null;
		}
		DBTableFrame tableFrame = new DBTableFrame(tblIFace, -1);
		DBTableField field = new DBTableField(fldIFace, tableFrame);
		return field;
	}

	/**
	 * Creates a new DBSelectTableModelItem item from a DOM Node
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aNode
	 *            creates a model item
	 * @return the DBSelectTableModelItem item
	 */
	private static DBSelectTableModelItem createItemFromNode(
			DSSchemaIFace aSchema, Node aNode) {
		String tblName = DBUIUtils.findAttrValue(aNode, "tableName");
		DSTableIFace tblIFace = DBUIUtils.getTableByName(aSchema, tblName);
		if (tblIFace == null) {
			mErrorCode = BAD_TABLENAME;
			return null;
		}
		String fldName = DBUIUtils.findAttrValue(aNode, "fieldName");
		DSTableFieldIFace fldIFace = DBUIUtils
				.getFieldByName(tblIFace, fldName);
		if (fldIFace == null) {
			mErrorCode = BAD_TABLENAME;
			return null;
		}
		DBSelectTableModelItem item = new DBSelectTableModelItem();
		item.setTableName(getMappedTableName(tblName));
		item.setName(fldName);
		item.setTableId(DBUIUtils.getIntAttrId(aNode, "tableId")); // default
																	// value is
																	// -1 which
																	// is ok to
																	// set
		return item;
	}

	/**
	 * Creates a new DBSelectTableModelItem item from a DOM Node
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aNode
	 *            creates a model item
	 * @return a DBQueryDefTable
	 */
	private static DBQueryDefTable createTableItem(DSSchemaIFace aSchema,
			Node aNode) {
		String name = DBUIUtils.findAttrValue(aNode, "name");
		DSTableIFace tblIFace = DBUIUtils.getTableByName(aSchema, name);
		if (tblIFace == null) {
			mErrorCode = BAD_TABLENAME;
			return null;
		}

		DBQueryDefTable item = new DBQueryDefTable(DBUIUtils.getIntAttrId(
				aNode, "id"), name == null ? "" : getMappedTableName(name),
				DBUIUtils.getIntAttrId(aNode, "x"), DBUIUtils.getIntAttrId(
						aNode, "y"));
		return item;
	}

	/**
	 * Reads in a XML document that is a query definition
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aDoc
	 *            the document DOM
	 * @return the query def object
	 */
	public static DBQueryDef processDOM(DSSchemaIFace aSchema, Document aDoc) {
		DBQueryDef queryDef = new DBQueryDef();
		Node queryNode = DBUIUtils.findNode(aDoc, "query");
		if (queryNode != null) {
			queryDef.setIsAdv(DBUIUtils.findAttrValue(queryNode, "advanced")
					.equalsIgnoreCase("true"));
		} else {
			mErrorCode = NO_QUERY_NODE;
			queryDef = null;
			return null;
		}

		// process the "select" portion
		Node selectNode = DBUIUtils.findNode(aDoc, "select");
		if (selectNode != null) {
			NodeList list = selectNode.getChildNodes();
			if (list != null) {
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					if (child.getNodeType() != Node.TEXT_NODE) {
						String tblName = DBUIUtils.findAttrValue(child,
								"tableName");
						DSTableIFace tblIFace = DBUIUtils.getTableByName(
								aSchema, tblName);
						if (tblIFace == null) {
							mErrorCode = BAD_TABLENAME;
							return null;
						}
						String fldName = DBUIUtils.findAttrValue(child,
								"fieldName");

						try {
							if (fldName != null && fldName.equals(WILDCARD)) {
								// handle wild card *
								Vector fields = tblIFace.getFields();
								if (fields != null) {
									for (int j = 0; j < fields.size(); j++) {
										DSTableFieldIFace field = (DSTableFieldIFace) fields
												.elementAt(j);
										String fieldName = field.getName();
										addSelectedItemIntoQueryDef(fieldName,
												tblIFace, queryDef);
									}
								} else {
									throw new Exception(
											"no fields in the table " + tblName);
								}
							} else {
								// non wild card - just regular field name
								addSelectedItemIntoQueryDef(fldName, tblIFace,
										queryDef);
							}
						} catch (Exception e) {
							e.printStackTrace();
							mErrorCode = BAD_FIELDNAME;
							// System.out.println("The bad file name");
							return null;
						}

					}
				}
			}
		}

		// process the "tables" portion
		Node tablesNode = DBUIUtils.findNode(aDoc, "tables");
		if (tablesNode != null) {
			NodeList list = tablesNode.getChildNodes();
			if (list != null) {
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					String nodeName = child.getNodeName();
					if (nodeName.equals(DBWhereOperator.AND_OPER)
							|| nodeName.equals(DBWhereOperator.OR_OPER)
							|| nodeName.equals("table")) {
						DBQueryDefTable tableItem = createTableItem(aSchema,
								child);
						if (tableItem == null) {
							return null;
						}
						queryDef.addTable(tableItem);
					}
				}
			}
		}

		// process the "where" portion
		Node whereNode = DBUIUtils.findNode(aDoc, "where");
		if (whereNode != null) {
			NodeList list = whereNode.getChildNodes();
			if (list != null) {
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					String nodeName = child.getNodeName();
					if (nodeName.equals(DBWhereOperator.AND_OPER)
							|| nodeName.equals(DBWhereOperator.OR_OPER)
							|| nodeName.equals("field")) {
						DBWhereIFace whereObj = processWhereNode(aSchema,
								child, null);
						if (whereObj == null && mErrorCode != QUERY_OK) {
							return null;
						}
						queryDef.setWhere(whereObj);
					}
				}
			}
		}

		// process the "joins" portion
		Node joinNode = DBUIUtils.findNode(aDoc, "joins");
		if (joinNode != null) {
			Vector joins = new Vector();
			NodeList joinNodeList = joinNode.getChildNodes();
			for (int i = 0; i < joinNodeList.getLength(); i++) {
				Node child = joinNodeList.item(i);
				String nodeName = child.getNodeName();
				if (nodeName.equals("join")) {
					DBTableField left = null;
					DBTableField right = null;
					NodeList childList = child.getChildNodes();
					for (int j = 0; j < childList.getLength(); j++) {
						Node joinChild = childList.item(j);
						nodeName = joinChild.getNodeName();
						if (nodeName.equals("left")) {
							left = createFieldFromNode(aSchema, joinChild);
							if (left == null) {
								return null;
							}
						} else if (nodeName.equals("right")) {
							right = createFieldFromNode(aSchema, joinChild);
							if (right == null) {
								return null;
							}
						}
					}
					if (left != null && right != null) {
						DBTableJoinItem join = new DBTableJoinItem(left, right);
						joins.add(join);
					}
				}
			}
			if (joins.size() > 0) {
				queryDef.setJoins(joins);
			}
		}

		return queryDef;
	}

	/*
	 * This method will added a selected field into queryDef object
	 */
	private static void addSelectedItemIntoQueryDef(String fldName,
			DSTableIFace tblIFace, DBQueryDef queryDef) throws Exception {
		DSTableFieldIFace field = DBUIUtils.getFieldByName(tblIFace, fldName);
		String tblName = tblIFace.getName();
		if (field != null) {
			// System.out.println("field " +fldName +
			// " has the missing value in generating field is "+field.getMissingValueCode());
			DBSelectTableModelItem item = new DBSelectTableModelItem(
					getMappedTableName(tblName), fldName, field.getDataType(),
					true, "", "", field.getMissingValueCode());
			queryDef.addSelectItem(item);

		} else {
			throw new Exception("Couldn't find the selected field " + fldName
					+ " in table " + tblName);
		}

	}

	/**
	 * Reads in a Query and returns a Query object
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aFileName
	 *            Name of XML file representing a XML document
	 * @return DBQueryDef object
	 */
	public static DBQueryDef readQueryDef(DSSchemaIFace aSchema,
			String aFileName) {

		DBQueryDef queryDef = null;
		try {
			queryDef = processDOM(aSchema, DBUIUtils.readXMLFile2DOM(aFileName));
			// debug code
			// if (queryDef != null)
			// {
			// System.out.println("[\n"+emitXML(queryDef)+"\n]\n");
			// }
		} catch (Exception e) {
			System.err.println(e);
		}

		return queryDef;
	}

	/**
	 * Parses and creates a DBQueryDef object from an XML String
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aXMLQueryStr
	 *            XML string representing a XML document
	 * @return DBQueryDef object
	 */
	public static DBQueryDef parseQueryDef(DSSchemaIFace aSchema,
			String aXMLQueryStr, Hashtable aMappedNameHash) {
		mMappedNameHash = aMappedNameHash;

		DBQueryDef queryDef = null;
		try {
			queryDef = processDOM(aSchema, DBUIUtils
					.convertXMLStr2DOM(aXMLQueryStr));
		} catch (Exception e) {
			System.err.println(e);
		}

		return queryDef;

	}

	/**
	 * Parses and creates a DBQueryDef object from an XML String
	 * 
	 * @param aSchema
	 *            the schema
	 * @param aXMLQueryStr
	 *            XML string representing a XML document
	 * @return DBQueryDef object
	 */
	public static DBQueryDef parseQueryDef(DSSchemaIFace aSchema,
			String aXMLQueryStr) {
		return parseQueryDef(aSchema, aXMLQueryStr, null);
	}

	// --------------------------------------------------------------
	// -- XML Generation
	// --------------------------------------------------------------

	/**
	 * Returns a mapped name if one exists
	 */
	private static String getMappedTableName(String aName) {
		if (mMappedNameHash != null) {
			String mappedName = (String) mMappedNameHash.get(aName);
			if (mappedName != null && mappedName.length() > 0) {
				return mappedName;
			}
		}
		return aName;
	}

	/**
	 * Appends the XML generated from a DBWhereCondition
	 * 
	 * @param aStrBuf
	 *            the output buffer
	 * @param aCond
	 *            the condition
	 */
	private static void appendCondXML(StringBuffer aStrBuf,
			DBWhereCondition aCond) {
		aStrBuf.append("  <field tableName=\"" + aCond.getTableName()
				+ "\"  fieldName=\"" + aCond.getName() + "\" oper=\""
				+ aCond.getOperator() + "\" criteria=\"" + aCond.getCriteria()
				+ "\"/>\n");
	}

	/**
	 * Recurses through the "tree" of operators and creates a textual rendering
	 * of the operators and conditions
	 * 
	 * @param aStrBuf
	 *            the output buffer
	 * @param aWhereObj
	 *            the where object tree
	 * @param aLevel
	 *            the level within the tree
	 * @return true if done, false if not
	 */
	protected static boolean recurseWhere(StringBuffer aStrBuf,
			DBWhereIFace aWhereObj, int aLevel) {
		if (aWhereObj == null)
			return true;

		if (aWhereObj instanceof DBWhereCondition) {
			aStrBuf.append(DBUIUtils.getSpaces(aLevel));
			appendCondXML(aStrBuf, (DBWhereCondition) aWhereObj);
			return true;
		}

		DBWhereOperator whereOper = (DBWhereOperator) aWhereObj;

		// Check number of Children
		if (whereOper.getNumChildern() < 2)
			return false;

		int numChildren = 0;

		for (Enumeration e = whereOper.getEnumeration(); e.hasMoreElements();) {
			DBWhereIFace item = (DBWhereIFace) e.nextElement();
			if (item instanceof DBWhereOperator) {
				DBWhereOperator oper = (DBWhereOperator) item;
				if (!oper.isClosure() && oper.getNumChildern() > 1) {
					numChildren++;
				}
			} else {
				numChildren++;
			}
		}

		if (numChildren < 2)
			return false;

		aStrBuf.append(DBUIUtils.getSpaces(aLevel));
		aStrBuf.append("<" + whereOper.getName() + ">\n");

		for (Enumeration e = whereOper.getEnumeration(); e.hasMoreElements();) {
			DBWhereIFace item = (DBWhereIFace) e.nextElement();
			if (item instanceof DBWhereOperator) {
				DBWhereOperator oper = (DBWhereOperator) item;
				if (!oper.isClosure() && oper.getNumChildern() > 1) {
					boolean status = recurseWhere(aStrBuf, oper, aLevel + 1);
					if (!status)
						return false;
				}
			} else {
				DBWhereCondition cond = (DBWhereCondition) item;
				aStrBuf.append(DBUIUtils.getSpaces(aLevel + 1));
				appendCondXML(aStrBuf, (DBWhereCondition) item);
			}
		}
		aStrBuf.append(DBUIUtils.getSpaces(aLevel));
		aStrBuf.append("</" + whereOper.getName() + ">\n");
		return true;
	}

	/**
	 * Appends the generation of the XML for DBSelectTableModelItem item
	 * 
	 * @param aStrBuf
	 *            output buffer
	 * @param aItem
	 *            the item to be ouputted
	 * @param aDepth
	 *            the depth in the tree
	 * @param aType
	 *            the type of item we are working on
	 */
	private static void generateXMLFor(StringBuffer aStrBuf,
			DBSelectTableModelItem aItem, int aDepth, int aType) {
		String tableName = aItem.getTableName();

		aStrBuf.append(DBUIUtils.getSpaces(aDepth));

		aStrBuf.append("<field");

		if (aItem.getTableId() != -1) {
			aStrBuf.append(" tableId=\"" + aItem.getTableId() + "\"");
		}

		aStrBuf.append(" tableName=\"" + tableName + "\"");
		aStrBuf.append(" fieldName=\"" + aItem.getName() + "\"");

		if (aType == SELECT_TYPE) {
			// aStrBuf.append(" displayed=\""+(isDisplayed?"true":"false")+"\"");
		} else {
			aStrBuf.append(" datatype=\"" + aItem.getDataType() + "\"");
			aStrBuf.append(" criteria=\"" + aItem.getCriteria() + "\"");
			aStrBuf.append(" operator=\"" + aItem.getOperator() + "\"");
		}
		aStrBuf.append("/>\n");
	}

	/**
	 * Somewhat generic method for outputing either the select or the tables
	 * portion of the query
	 * 
	 * @param aList
	 *            the vector of objects
	 * @param aNodeName
	 *            the node name
	 * @param aDepth
	 *            the depth of the tree (for indentation)
	 * @param aType
	 *            the type of objects being processed
	 * @return a string of XML
	 */
	private static String enumerateObjs(Vector aList, String aNodeName,
			int aDepth, int aType) {
		if (aList.size() == 0)
			return "";

		StringBuffer strBuf = new StringBuffer();
		strBuf.append(DBUIUtils.getSpaces(aDepth));
		strBuf.append("<" + aNodeName + ">\n");
		for (Enumeration et = aList.elements(); et.hasMoreElements();) {
			Object obj = et.nextElement();
			if (aType == TABLE_TYPE && obj instanceof DBQueryDefTable) {
				strBuf.append(DBUIUtils.getSpaces(aDepth + 1));
				DBQueryDefTable table = (DBQueryDefTable) obj;

				strBuf.append("<table name=\"" + table.getName() + "\"");

				if (table.getId() > -1)
					strBuf.append(" id=\"" + table.getId() + "\"");

				Point pnt = table.getPnt();
				if (pnt.x > -1)
					strBuf.append(" x=\"" + pnt.x + "\"");

				if (pnt.y > -1)
					strBuf.append(" y=\"" + pnt.y + "\"");

				strBuf.append("/>\n");

			} else if (aType == SELECT_TYPE
					&& obj instanceof DBSelectTableModelItem) {
				generateXMLFor(strBuf, (DBSelectTableModelItem) obj, aDepth,
						aType);
			}
		}
		strBuf.append(DBUIUtils.getSpaces(aDepth));
		strBuf.append("</" + aNodeName + ">\n");
		return strBuf.toString();
	}

	/**
	 * Appends generated XML for a join item (left or right)
	 * 
	 * @param aStrBuf
	 *            the output string
	 * @param aName
	 *            the name of XML element
	 * @param aItem
	 *            to be generated as XML
	 */
	private static void processJoinItem(StringBuffer aStrBuf, String aName,
			DBSelectTableModelItem aItem) {
		if (aItem == null)
			return;

		aStrBuf.append("      <" + aName + " ");
		if (aItem.getTableId() > -1) {
			aStrBuf.append("tableId=\"" + aItem.getTableId() + "\" ");
		}
		aStrBuf.append("tableName=\"" + aItem.getTableName()
				+ "\" fieldName=\"" + aItem.getName() + "\"/>\n");
	}

	/**
	 * Appends generated XML for all of the joins
	 * 
	 * @param aStrBuf
	 *            the output string
	 * @param aJoins
	 *            vector of pairs of items representing the joins
	 */
	private static void processJoins(StringBuffer aStrBuf, Vector aJoins) {
		// make sure there is an even number
		if (aJoins != null && aJoins.size() > 0 && aJoins.size() % 2 == 0) {
			StringBuffer strBuf = new StringBuffer("  <joins>\n");
			for (Enumeration et = aJoins.elements(); et.hasMoreElements();) {
				strBuf.append("    <join>\n");
				processJoinItem(strBuf, "left", (DBSelectTableModelItem) et
						.nextElement());
				processJoinItem(strBuf, "right", (DBSelectTableModelItem) et
						.nextElement());
				strBuf.append("    </join>\n");
			}
			aStrBuf.append(strBuf.toString());
			aStrBuf.append("  </joins>\n");
		}
	}

	/**
	 * Creates the XML that represents a query
	 * 
	 * @return the XML document
	 */
	public static String emitXML(DBQueryDef aQueryDef) {
		if (aQueryDef == null)
			return "";

		Vector selects = aQueryDef.getSelects();
		Vector tables = aQueryDef.getTables();
		DBWhereIFace whereObj = aQueryDef.getWhere();

		// if this is empty query, return ""
		if ((selects == null || (selects != null && selects.isEmpty()))
				&& (tables == null || (tables != null && tables.isEmpty()))) {
			return "";
		}

		StringBuffer strBuf = new StringBuffer("<query advanced=\""
				+ aQueryDef.isAdv() + "\">\n");

		strBuf.append(enumerateObjs(selects, "select", 1, SELECT_TYPE));

		strBuf.append(enumerateObjs(tables, "tables", 1, TABLE_TYPE));

		processJoins(strBuf, aQueryDef.getJoins());

		// DBWhereIFace whereObj = aQueryDef.getWhere();
		if (whereObj != null) {
			strBuf.append("  <where>\n");
			recurseWhere(strBuf, whereObj, 2);
			strBuf.append("  </where>\n");
		}

		strBuf.append("</query>\n");

		return strBuf.toString();
	}

	/**
	 * Create SQL string
	 */
	public static String createSQL(DSSchemaIFace aSchemaDef,
			DBQueryDef aQueryDef) {
		if (aQueryDef == null)
			return null;

		Hashtable tableNames = new Hashtable();
		StringBuffer strBuf = new StringBuffer("SELECT ");

		int displayCnt = 0;
		for (Enumeration et = aQueryDef.getSelects().elements(); et
				.hasMoreElements();) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) et
					.nextElement();
			if (item.isDisplayed()) {
				tableNames.put(item.getTableName(), item.getTableName());
				displayCnt++;
			}
		}
		if (displayCnt == 0)
			return null;

		displayCnt = 0;
		for (Enumeration et = aQueryDef.getSelects().elements(); et
				.hasMoreElements();) {
			DBSelectTableModelItem item = (DBSelectTableModelItem) et
					.nextElement();
			if (item.isDisplayed()) {
				if (displayCnt > 0) {
					strBuf.append(", ");
				}
				displayCnt++;
				strBuf.append(DBUIUtils.getFullFieldName(item.getTableName(),
						item.getName()));
				tableNames.put(item.getTableName(), item.getTableName());
			}
		}
		strBuf.append(" FROM ");

		StringBuffer whereStr = new StringBuffer();
		if (aQueryDef.getJoins() != null) {
			int cnt = 0;
			for (Enumeration et = aQueryDef.getJoins().elements(); et
					.hasMoreElements();) {
				if (cnt > 0) {
					whereStr.append(" AND ");
				}
				cnt++;
				DBTableJoinItem joinItem = (DBTableJoinItem) et.nextElement();
				whereStr.append(DBUIUtils.getFullFieldName(joinItem
						.getItemLeft()));
				whereStr.append(" = ");
				whereStr.append(DBUIUtils.getFullFieldName(joinItem
						.getItemRight()));
				String tblName = joinItem.getItemLeft().getTable().getName();
				tableNames.put(tblName, tblName);
				tblName = joinItem.getItemRight().getTable().getName();
				tableNames.put(tblName, tblName);
			}
		}

		displayCnt = 0;
		for (Enumeration et = tableNames.elements(); et.hasMoreElements();) {
			String tableName = (String) et.nextElement();
			if (tableName.indexOf(' ') != -1) {
				tableName = "[" + tableName + "]";
			}
			if (displayCnt > 0) {
				strBuf.append(", ");
			}
			displayCnt++;
			strBuf.append(tableName);
		}

		// Super cheesey, but we will do this for now
		DBWherePanel wherePanel = new DBWherePanel(aSchemaDef);
		wherePanel.getModel().initialize(aQueryDef.getWhere());
		wherePanel.fillQueryDef(aQueryDef);
		boolean addedWhere = false;
		if (aQueryDef.getJoins() != null) {
			addedWhere = true;
			strBuf.append(" WHERE ");
			strBuf.append(whereStr);
		}
		String wherePanelStr = wherePanel.generateWhereSQL(true);
		String noSpaces = wherePanelStr.trim();
		if (noSpaces.length() > 0) {

			strBuf.append(addedWhere ? " AND " : " WHERE ");
			strBuf.append(wherePanelStr);
		}

		return strBuf.toString();

	}

}