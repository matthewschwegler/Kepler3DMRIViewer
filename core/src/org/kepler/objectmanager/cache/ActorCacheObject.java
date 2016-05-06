/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-11-14 20:18:41 -0800 (Thu, 14 Nov 2013) $' 
 * '$Revision: 32540 $'
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

package org.kepler.objectmanager.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.moml.KeplerActorMetadata;
import org.kepler.moml.KeplerMetadataExtractor;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ptolemy.util.MessageHandler;

import ptolemy.moml.MoMLParser;

/**
 *  Class that represents an object in the ObjectCache. This class should be
 *  extended by each type of object that wants to control its own lifecycle
 *  events and serialization events.
 *
 *@author     berkley
 *@created    September 7, 2007
 */

/**
 * Modified by Dan Higgins, Aug 9, 2007 to avoid instantiating all the actors
 * when the ActorCacheObject is created. This is done be avoiding creating the
 * ActorMetadata object and using the MoML parser. Instead a DOM parser is used
 * to pull out just a few of parameters in the kar files MoML description of the
 * actor, and the xml saved with the ActorCacheObject is exactly that in the kar
 * file.
 * 
 *@author berkley
 *@created September 7, 2007
 */
public class ActorCacheObject extends CacheObject implements Externalizable {

	private static final Log log = LogFactory.getLog(ActorCacheObject.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * 
	 */
	private ActorMetadata _actor = null;

	/**
	 * 
	 */
	private String _actorString;

	/**
	 * 
	 */
	private String _className;

	/**
	 * 
	 */
	private String _rootname;

	/**
	 * Constructor for the ActorCacheObject object
	 */
	public ActorCacheObject() {
		super();
		_actor = null;
	}

	/**
	 * construct a new CacheObject
	 * 
	 *@param name
	 *            Description of the Parameter
	 *@param lsid
	 *            Description of the Parameter
	 */
	public ActorCacheObject(String name, KeplerLSID lsid) {
		super(name, lsid);
		if (isDebugging)
			log.debug("ActorCacheObject(" + name + "," + lsid + ")");
	}

	/**
	 * create an ActorCacheObject from a stream
	 * 
	 * @param actorStream
	 *            the stream of the actor moml
	 * @exception CacheException
	 *                Description of the Exception
	 */
	public ActorCacheObject(InputStream actorStream) throws CacheException {
		super();
		if (isDebugging)
			log.debug("ActorCacheObject(" + actorStream.getClass().getName()
					+ ")");
		
		KeplerActorMetadata kam = null;
		try {
			kam = KeplerMetadataExtractor
				.extractActorMetadata(actorStream);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		if (kam != null) {
			_actorString = kam.getActorString();
			this.setName( kam.getName() );
			this.setLSID( kam.getLsid() );
			_className = kam.getClassName();
			_rootname = kam.getRootName();
			setSemanticTypes( kam.getSemanticTypes() );
			
			Map<String,String> attributes = kam.getAttributes();
			if (attributes != null) {
				for (String attributeName : attributes.keySet()) {
					String attributeValue = attributes.get(attributeName);
					this.addAttribute(attributeName, attributeValue);

				}
			}
		}
	}

	/**
	 * this returns an ActorMetadata object. It will need to be casted to be
	 * used
	 * 
	 *@return The object value
	 */
	public Object getObject() {
		if (isDebugging)
			log.debug("getObject()");
		return getMetadata();
	}

	/**
	 * serialize this class
	 * 
	 *@param out
	 *            Description of the Parameter
	 *@exception IOException
	 *                Description of the Exception
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		if (isDebugging)
			log.debug("writeExternal(" + out.getClass().getName() + ")");
		byte[] b = _actorString.getBytes();
		out.write(b, 0, b.length);
		out.flush();
	}

	/**
	 * deserialize this class
	 * 
	 *@param in
	 *            Description of the Parameter
	 *@exception IOException
	 *                Description of the Exception
	 *@exception ClassNotFoundException
	 *                Description of the Exception
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		if (isDebugging)
			log.debug("readExternal(" + in.getClass().getName() + ")");

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] b = new byte[1024];
		int numread = in.read(b, 0, 1024);
		while (numread != -1) {
			bos.write(b, 0, numread);
			numread = in.read(b, 0, 1024);
		}
		bos.flush();
		_actorString = bos.toString();
		try {
			StringReader strR = new StringReader(_actorString);
			InputSource xmlIn = new InputSource(strR);
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					if (MOML_PUBLIC_ID_1.equals(publicId)) {
						return new InputSource(MoMLParser.class.getResourceAsStream("MoML_1.dtd"));
					}
					else {
						return null;
					}
				}
			});
			Document doc = builder.parse(xmlIn);
			Node rootNode = doc.getDocumentElement();
			_rootname = rootNode.getNodeName();
			NamedNodeMap nnm = rootNode.getAttributes();
			Node namenode = nnm.getNamedItem("name");
			String nameStr = namenode.getNodeValue();
			this._name = nameStr;
			NodeList probNodes = rootNode.getChildNodes();
			for (int i = 0; i < probNodes.getLength(); i++) {
				Node child = probNodes.item(i);
				if (child.hasAttributes()) {
					NamedNodeMap childAttrs = child.getAttributes();
					Node idNode = childAttrs.getNamedItem("name");
					if (idNode != null) {
						String nameval = idNode.getNodeValue();
						if (nameval.equals(NamedObjId.NAME)) {
							Node idNode1 = childAttrs.getNamedItem("value");
							String idval = idNode1.getNodeValue();
							this._lsid = new KeplerLSID(idval);
						}
						if (nameval.equals("class")) {
							Node idNode3 = childAttrs.getNamedItem("value");
							String classname = idNode3.getNodeValue();
							this._className = classname;
						}
						if (nameval.startsWith("semanticType")) {
							Node idNode2 = childAttrs.getNamedItem("value");
							String semtype = idNode2.getNodeValue();
							_semanticTypes.add(semtype);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException("Error in ActorCacheObject(ReadExternal): "
					+ e.getMessage());
		}
	}

	/**
	 * Return the ActorMetadata for this actor.
	 * 
	 *@return The metadata value
	 */
	public ActorMetadata getMetadata() {
		if (isDebugging)
			log.debug("getMetadata()");
		if (_actor == null) {
			// added if actor has never been created - DFH
			try {
				byte[] bb = _actorString.getBytes();
				ByteArrayInputStream bytein = new ByteArrayInputStream(bb);
				_actor = new ActorMetadata(bytein);
			} catch (Exception e) {
				MessageHandler.error("Error parsing actor metadata.", e);
			}
		}
		return _actor;
	}

	/**
	 * Gets the className attribute of the ActorCacheObject object.
	 * 
	 *@return The className value
	 */
	public String getClassName() {
		return _className;
	}

	/**
	 * Gets the actorString attribute of the ActorCacheObject object.
	 * 
	 *@return The actorString value
	 */
	public String getActorString() {
		return _actorString;
	}

	/**
	 * Gets the rootName attribute of the ActorCacheObject object.
	 * 
	 *@return The rootName value
	 */
	public String getRootName() {
		return _rootname;
	}

	public static String MOML_PUBLIC_ID_1 = "-//UC Berkeley//DTD MoML 1//EN";	
}