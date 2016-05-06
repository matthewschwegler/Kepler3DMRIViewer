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

package org.ecoinformatics.seek.datasource.darwincore;

import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.EcogridUtils;
import org.ecoinformatics.ecogrid.EcogridUtilsNamedNodeIterator;
import org.ecoinformatics.seek.datasource.EcogridDataCacheItem;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.cache.DataCacheListener;
import org.kepler.objectmanager.cache.DataCacheManager;
import org.kepler.objectmanager.cache.DataCacheObject;
import org.kepler.objectmanager.data.DataType;
import org.kepler.objectmanager.data.db.DSSchemaDef;
import org.kepler.objectmanager.data.db.DSSchemaIFace;
import org.kepler.objectmanager.data.db.DSTableDef;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * The Darwin Core Schema instance.
 */
public class DarwinCoreSchema implements DataCacheListener {
	static final String XSD_TYPES[] = { "string", "nonNegativeInteger",
			"decimal", "gYear", "dateTime" };
	static final String KEPLER_TYPES[] = { DataType.STR, DataType.INT,
			DataType.FLOAT, DataType.STR, DataType.STR };

	private Hashtable mFieldNameToType = null;
	private Hashtable mXSD2KeplerHashtable = null;
	private DSSchemaDef mSchemaDef = null;

	/*
	 * The xpath in Config used to extract url for Darwin Core schema.
	 */
	private static final String schemaXpath = "//ecogridService/digir/schema";

	/*
	 * Synchronization latch. It's released when the data has been downloaded.
	 */
	private Latch mFinished;

	/*
	 * Singleton instance.
	 */
	private static DarwinCoreSchema mGlobalDarwinCoreSchema = null;

	/*
	 * Logger.
	 */
	private static Log log;

	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.datasource.darwincore.DarwinCoreSchema");
	}

	/**
	 * Returns the singleton
	 * 
	 * 	 */
	public synchronized static DarwinCoreSchema getInstance() {
		if (mGlobalDarwinCoreSchema == null) {
			mGlobalDarwinCoreSchema = new DarwinCoreSchema();
		}
		return mGlobalDarwinCoreSchema;
	}

	/**
	 * 
	 * @return the schema object for the DarwinCore
	 */
	public DSSchemaIFace getSchema() {
		// TODO - implement timeout here and raise exception.
		try {
			mFinished.acquire();
		} catch (InterruptedException e) {
			log.error("Wait for getSchema interrupted", e);
			throw new RuntimeException("DarwinCoreSchema acquire interrupted",
					e);
		}
		return mSchemaDef;
	}

	public String lookupTypeFromName(String aName) {
		// TODO - implement timeout here and raise exception.
		try {
			mFinished.acquire();
		} catch (InterruptedException e) {
			log.error("Wait for getSchema interrupted", e);
			throw new RuntimeException("DarwinCoreSchema acquire interrupted",
					e);
		}
		String type = (String) mFieldNameToType.get(aName);
		if (type == null) {
			return "STRING";
		}
		return type;
	}

	/**
	 * Construct s an object to produce a DarwinCoreSchema
	 */
	protected DarwinCoreSchema() {
		mFinished = new Latch();

		mXSD2KeplerHashtable = new Hashtable();
		mFieldNameToType = new Hashtable();
		for (int i = 0; i < XSD_TYPES.length; i++) {
			mXSD2KeplerHashtable.put(XSD_TYPES[i], KEPLER_TYPES[i]);
		}

    //schemaXpath: //ecogridService/digir/schema
    String urlStr;
    //get the configuration for this module
    ConfigurationManager confMan = ConfigurationManager.getInstance();
    //get the specific configuration we want
    ConfigurationProperty ecogridProperty = confMan.getProperty(ConfigurationManager.getModule("ecogrid"));
    ConfigurationProperty schemaProperty = ecogridProperty.getProperty("digir.schema");
    urlStr = schemaProperty.getValue();
    
		EcogridDataCacheItem darwinCoreDataItem = (EcogridDataCacheItem) DataCacheManager
				.getCacheItem(this, "DarwinCoreSchema", urlStr,
						EcogridDataCacheItem.class.getName());
		darwinCoreDataItem.start();
	}

	public void complete(DataCacheObject item) {
		try {
			if (!item.isReady()) {
				// TODO : proper error handling.
			}

			Document dom = null;
			try {
				dom = EcogridUtils.readXMLFile2DOM(item.getAbsoluteFileName());
				if (dom == null) {
					// TODO: proper error handling
					return;
				}

			} catch (Exception e) {
				log.error("Exception occurred while parsing schema", e);
				return;
			}

			// EcogridUtils.printNode(dom, 0);
			Node schemaNode = null;
			StringBuffer nameStrBuf = new StringBuffer();
			StringBuffer nameSpaceStrBuf = new StringBuffer();
			NodeList list = dom.getChildNodes();
			if (list != null) {
				for (int i = 0; i < list.getLength(); i++) {
					Node child = list.item(i);
					if (child != null) {
						EcogridUtils.parseForNameAndNamespace(child
								.getNodeName(), nameStrBuf, nameSpaceStrBuf);
						if (nameStrBuf.toString().equals("schema")) {
							schemaNode = child;
							break;
						}

					}
				}
			}
			if (schemaNode == null)
				return;

			mSchemaDef = new DSSchemaDef();
			DSTableDef tableDef = new DSTableDef("DarwinCode");
			mSchemaDef.addTable(tableDef);

			// XXX should substitute this later with XSLT transform for easier
			// processing
			String elementName = nameSpaceStrBuf.length() > 0 ? nameSpaceStrBuf
					.toString()
					+ ":element" : "element";
			EcogridUtilsNamedNodeIterator iter = new EcogridUtilsNamedNodeIterator(
					schemaNode, elementName);
			while (iter.hasMoreNodes()) {
				Node elementNode = iter.nextNode();

				String name = EcogridUtils.findAttrValue(elementNode, "name");
				StringBuffer nameSB = new StringBuffer();
				StringBuffer nsSB = new StringBuffer();
				EcogridUtils.parseForNameAndNamespace(name, nameSB, nsSB);
				// System.out.println("nameSB: "+nameSB);

				StringBuffer typeSB = new StringBuffer();
				StringBuffer typeNSSB = new StringBuffer();

				String type = EcogridUtils.findAttrValue(elementNode, "type");
				if (type == null) {
					String restrictionName = nameSpaceStrBuf.length() > 0 ? nameSpaceStrBuf
							.toString()
							+ ":restriction"
							: "restriction";
					Node restriction = EcogridUtils.findNode(elementNode,
							restrictionName);
					if (restriction != null) {
						type = EcogridUtils.findAttrValue(restriction, "base");
					}
				}
				EcogridUtils.parseForNameAndNamespace(type, typeSB, typeNSSB);
				// System.out.println("typeSB: "+typeSB);
				if (nameSB.length() > 0 && typeSB.length() > 0) {
					String keplerType = (String) mXSD2KeplerHashtable
							.get(typeSB.toString());
					if (keplerType == null) {
						keplerType = typeSB.toString();
						log.error("Missing a Mapping from XSD ["
								+ typeSB.toString() + "] to Kepler type.");
					}
					tableDef.addField(nameSB.toString(), keplerType, null);
					mFieldNameToType.put(nameSB.toString(), keplerType);
				} else {
					// XXX
					// System.err.println("["+nameSB.toString()+"]["+typeSB.toString()+"]");
				}
			}
			// XXX
			// System.out.println(DBSchemaParserEmitter.emitXML(mSchemaDef));

		} finally {
			mFinished.release();
		}
	}
}