/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-07-25 13:29:43 -0700 (Wed, 25 Jul 2012) $' 
 * '$Revision: 30284 $'
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

package org.kepler.moml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ptolemy.moml.MoMLParser;

/**
 * This class will contain methods for parsing out the Kepler Metadata from a
 * MOML file.
 * 
 * @author Aaron Schultz
 */
public class KeplerMetadataExtractor {

	private static final Log log = LogFactory
			.getLog(KeplerMetadataExtractor.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	//private static final boolean isTracing = log.isTraceEnabled();

	public static KeplerActorMetadata extractActorMetadata(File momlFile)
	        throws Exception {
	    return extractActorMetadata(momlFile, true);
	}
	   
	public static KeplerActorMetadata extractActorMetadata(File momlFile, boolean printError)
			throws Exception {
		FileInputStream fis = new FileInputStream(momlFile);
		return extractActorMetadata(fis, printError);
	}

    /** Get the metadata from an input stream. 
     *  @param actorStream the input stream
     */
    public static KeplerActorMetadata extractActorMetadata(
            InputStream actorStream) throws Exception {
        return extractActorMetadata(actorStream, true);
    }
	
	/** Get the metadata from an input stream optionally printing output if a parsing error occurs.
	 *  @param actorStream the input stream
	 *  @param printError if true, print a stack trace and error message if a parsing error occurs.
	 */
	public static KeplerActorMetadata extractActorMetadata(
			InputStream actorStream, boolean printError) throws Exception {
		//if (isTracing)
			//log.trace("ActorCacheObject(" + actorStream.getClass().getName()
					//+ ")");

		KeplerActorMetadata kam = new KeplerActorMetadata();

		ByteArrayOutputStream byteout;
		try {
			// Copy 1024 bytes from actorStream to byteout
			byteout = new ByteArrayOutputStream();
			byte[] b = new byte[1024];
			int numread = actorStream.read(b, 0, 1024);
			while (numread != -1) {
				byteout.write(b, 0, numread);
				numread = actorStream.read(b, 0, 1024);
			}
			kam.setActorString(byteout.toString());

			// need to get actor name and id from the string
			// thus build a DOM representation
			String nameStr = null;
			try {
				//if (isTracing) log.trace(kam.getActorString());
				StringReader strR = new StringReader(kam.getActorString());
				
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
				
				// TODO 
				// this causes http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4671
				// when File => Save Archive w/ wf w/ actor w/ < in the name:
				Document doc = builder.parse(xmlIn);
				//if (isTracing) log.trace(doc.toString());

				Element rootNode = doc.getDocumentElement();
				kam.setRootName(rootNode.getNodeName());

				// Get the value of the name attribute of the root node
				NamedNodeMap nnm = rootNode.getAttributes();
				Node namenode = nnm.getNamedItem("name");
				nameStr = namenode.getNodeValue();
				kam.setName(nameStr);

				boolean emptyKeplerDocumentation = true;
				boolean foundKeplerDocumentation = false;
                boolean foundUserLevelDocumentation = false;
                boolean foundAuthor = false;

				// Cycle through the children of the root node
				NodeList probNodes = rootNode.getChildNodes();
				for (int i = 0; i < probNodes.getLength(); i++) {
					Node child = probNodes.item(i);

					if (child.hasAttributes()) {

						NamedNodeMap childAttrs = child.getAttributes();
						Node idNode = childAttrs.getNamedItem("name");
						if (idNode != null) {

							// the entityId
							String nameval = idNode.getNodeValue();
							if (nameval.equals(NamedObjId.NAME)) {
								Node idNode1 = childAttrs.getNamedItem("value");
								String idval = idNode1.getNodeValue();
								kam.setLsid(new KeplerLSID(idval));
							}

							// the class name
							if (nameval.equals("class")) {
								Node idNode3 = childAttrs.getNamedItem("value");
								String classname = idNode3.getNodeValue();
								kam.setClassName(classname);
							}

							// the semantic types
							if (nameval.startsWith("semanticType")) {
								Node idNode2 = childAttrs.getNamedItem("value");
								String semtype = idNode2.getNodeValue();
								kam.addSemanticType(semtype);
							}
							
							// userLevelDocumentation must be contained in KeplerDocumentation 
							if (nameval.equals("userLevelDocumentation")) {
							    log.warn(nameStr + " userLevelDocumentation property should be contained in a KeplerDocumentation property.");
							} else if(nameval.equals("KeplerDocumentation")) {
							    
							    foundKeplerDocumentation = true;
							    
							    final NodeList keplerDocNodeList = child.getChildNodes();
							    if(keplerDocNodeList.getLength() > 0) {

							        emptyKeplerDocumentation = false;

							        for (int j = 0; j < keplerDocNodeList.getLength(); j++) {
					                    final Node docChildNode = keplerDocNodeList.item(j);
					                    final NamedNodeMap docChildNamedNodeMap = docChildNode.getAttributes();
					                    
					                    if(docChildNamedNodeMap != null) {
					                        
					                        final Node docChildChildName = docChildNamedNodeMap.getNamedItem("name");
					                        
					                        if(docChildChildName != null) {
					                        
					                            final String docChildChildNameValue = docChildChildName.getNodeValue();
					                            
					                            if(docChildChildNameValue.equals("userLevelDocumentation")) {
					                                
					                                foundUserLevelDocumentation = true;
					                                final String text = docChildNode.getTextContent();
					                                if(text == null || text.trim().isEmpty()) {
					                                    log.debug(nameStr + " has empty userLevelDocumentation.");
					                                }
					                                
					                            } else if(docChildChildNameValue.equals("author")) {
					                                foundAuthor = true;

					                                final String text = docChildNode.getTextContent();
                                                    if(text == null || text.trim().isEmpty()) {
                                                        log.debug(nameStr + " has empty author documentation.");
                                                    }
					                                
					                            }
					                        }
					                    }					                    
							        }   
							    }
							}
														
							if (nameval.startsWith(COPY_ATTRIBUTE_PREFIX)) {
								String value = childAttrs.getNamedItem("value").getNodeValue();
								kam.addAttribute(nameval, value);
							}
						}
					}
				}
				
				// check documentation
				if(!foundKeplerDocumentation) {
				    log.debug(nameStr + " is missing KeplerDocumentation.");
				} else if(emptyKeplerDocumentation) {
				    log.debug(nameStr + " KeplerDocumentation is empty.");
				} else if(!foundUserLevelDocumentation && !foundAuthor) {
				    log.debug(nameStr + " is missing userLevelDocumentation and author documentation.");
				}
				else if(!foundUserLevelDocumentation) {
				    log.debug(nameStr + " is missing userLevelDocumentation.");
				}
				else if(!foundAuthor) {
				    log.debug(nameStr + " is missing author documentation.");
				}
				
				
			} catch (Exception e) {
			    if(printError) {
    				e.printStackTrace();
    				System.out.println("Error parsing Actor KAR DOM \""
    						+ ((nameStr == null) ? byteout.toString().substring(0,
    								300)
    								+ "..." : nameStr) + "\": " + e.getMessage());
			    }
				kam = null;
			}
			finally {
			    actorStream.close();
			    byteout.close();
			}
		} catch (Exception e) {
			kam = null;
			throw new Exception("Error extracting Actor Metadata: "
					+ e.getMessage());
		}

		return kam;
	}
	
	public static final String COPY_ATTRIBUTE_PREFIX = "_wrapper";	
	public static final String MOML_PUBLIC_ID_1 = "-//UC Berkeley//DTD MoML 1//EN";	
}