/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-01-17 10:45:04 -0800 (Thu, 17 Jan 2013) $' 
 * '$Revision: 31345 $'
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

package org.kepler.sms.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.sms.Color;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class reads in ontology_catalog.xml and returns the set of ontologies.
 * It also gives those that are suitable as library categorizations.
 */
public class OntologyConfiguration {

	/**
	 * This is a singleton class, so the constructor is "hidden"
	 */
	protected OntologyConfiguration() {
		defaultColors = new ArrayList<Color>();
		Color[] colors = new Color[] {new Color("red"), new Color("blue"), new Color("pink"), new Color("orange"), new Color("green"), new Color("magenta"), new Color("cyan"), new Color("gray")};
		defaultColors.addAll(Arrays.asList(colors));
	}
	
	public void setIndexFile(File indexFile) {
		_indexFile = indexFile;
	}
	
	public File getIndexFile() {
		return _indexFile;
	}

	/**
	 * Initialize the catalog TODO: read from config file
	 */
	public void initialize() {
		// load the index file
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			initializePaths();
			_document = builder.parse(INDEX_FILE);
			// System.out.println("Reading ontology file: " + new
			// File(INDEX_FILE).getAbsolutePath());
		} catch (SAXException sxe) {
			Exception x = sxe;
			if (sxe.getException() != null)
				x = sxe.getException();
			x.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void initializePaths() {
		/*
		File indexFile = ProjectLocator
				.getHighestRankedFile("configs/ptolemy/configs/kepler/ontologies/ontology_catalog.xml");
		*/
		// System.out.println("**** " + indexFile.getAbsolutePath());

		try {
			INDEX_FILE = _indexFile.getAbsolutePath();
			URI uri = getClass().getClassLoader().getResource("ptolemy/configs/kepler/ontologies").toURI();
			
			// make sure the directory is on the file system. 
			// FIXME allow the directory to be in other locations, e.g., in a jar.
			if(!uri.getScheme().equals("file")) {
			    log.error("Ontology directory is not on file system: " + uri);
			} else {
			    ONTO_PATH = new File(uri).getAbsolutePath() + "/";
			}
		} catch(URISyntaxException ex) {
			log.error("Error finding ontology directory.", ex);
		} catch(IllegalArgumentException ex2) {
            log.error("Error finding ontology directory.");
		}
	}

	/**
	 * This method must be called to create/obtain an instance of the catalog
	 * 
	 * @return The unique instance of this class
	 */
	public static OntologyConfiguration instance() {
		if (_config == null)
			_config = new OntologyConfiguration();
		return _config;
	}
	
	public void reset() {
		_document = null;
	}

	/**
	 * Get the file path names for the ontologies
	 */
	public Iterator getFilePathNames() {
		Vector result = new Vector();
		if (_document == null)
			return result.iterator();

		// get the root
		Element root = _document.getDocumentElement();
		if (root == null)
			return result.iterator();

		// iterate through root, get each ontology element and its
		// filename
		NodeList lst = root.getElementsByTagName("ontology");
		for (int i = 0; i < lst.getLength(); i++) {
			Element elem = (Element) lst.item(i);
			Attr att = elem.getAttributeNode("filename");
			if (att != null) {
				String filename = att.getValue();
				if (filename != null)
					result.addElement(getAbsoluteOntologyPath(filename));
			}
		}

		return result.iterator();
	}
	
	
	/**
	 * FIXME
	 * TODO create test cases for this method
	 * 
	 * @param filename
	 * @return
	 */
	private String getAbsoluteOntologyPath(String filename) {
		if (isDebugging) log.debug("getAbsoluteOntologyPath(\"" + filename + "\")");
		if (new File(filename).isFile()) {
			if (isDebugging) log.debug("Path is absolute");
			return filename;
		}
		if (isDebugging) log.debug("Path is relative: " + new File(ONTO_PATH + filename).getAbsolutePath());
		return ONTO_PATH + filename;
	}
	
	private List<Color> defaultColors;
	private Map<String, Color> defaultSetColors = new HashMap<String, Color>();
	
	public Color getLibraryColor(String filepath) {
		if (_document == null || filepath == null)
			return null;

		// get the root
		Element root = _document.getDocumentElement();
		if (root == null)
			return null;
    
        filepath = new File(filepath).getAbsolutePath();

		// iterate to find the ontology with filepath
		NodeList lst = root.getElementsByTagName("ontology");
		for (int i = 0; i < lst.getLength(); i++) {
			Element elem = (Element) lst.item(i);
			Attr att = elem.getAttributeNode("filename");
			if (att != null) {
				String filename = att.getValue();
				if (filepath.equals(getAbsoluteOntologyPath(filename))) {
					Attr libatt = elem.getAttributeNode("color");
					if (libatt != null) {
						String colorString = libatt.getValue();
						return new Color(colorString);
					}
                }
			}
		}

        if (!defaultSetColors.containsKey(filepath)) {
            if (defaultColors.isEmpty()) {
                return Color.getDefaultColor();
            }
            defaultSetColors.put(filepath, defaultColors.remove(defaultColors.size() - 1));
        }
        return defaultSetColors.get(filepath);
	}
	
	public Boolean isLibraryLocal(String filepath) {
		if (_document == null || filepath == null)
			return null;

		// get the root
		Element root = _document.getDocumentElement();
		if (root == null)
			return null;
		// iterate to find the ontology with filepath
		NodeList lst = root.getElementsByTagName("ontology");
		for (int i = 0; i < lst.getLength(); i++) {
			Element elem = (Element) lst.item(i);
			Attr att = elem.getAttributeNode("filename");
			if (att != null) {
				String filename = att.getValue();
				if (filepath.equals(getAbsoluteOntologyPath(filename))) {
					Attr libatt = elem.getAttributeNode("local");
					if (libatt != null) {
						String localityString = libatt.getValue();
						return "true".equals(localityString);
					}
					else {
						// Default to not editable
						return false;
					}
				}
			}
		}
		return false;	
	}

	/**
	 * @return True if the filepath is suitable as a library categorization.
	 */
	public boolean isLibraryOntology(String filepath) {
		if (_document == null || filepath == null)
			return false;

		// get the root
		Element root = _document.getDocumentElement();
		if (root == null)
			return false;
		// iterate to find the ontology with filepath
		NodeList lst = root.getElementsByTagName("ontology");
		for (int i = 0; i < lst.getLength(); i++) {
			Element elem = (Element) lst.item(i);
			Attr att = elem.getAttributeNode("filename");
			if (att != null) {
				String filename = att.getValue();
				if (filepath.equals(getAbsoluteOntologyPath(filename))) {
					Attr libatt = elem.getAttributeNode("library");
					if (libatt != null) {
						String library = libatt.getValue();
						if (library != null && library.equals("true"))
							return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean isTagBarOntology(String filepath) {
		if (_document == null || filepath == null)
			return false;

		// get the root
		Element root = _document.getDocumentElement();
		if (root == null)
			return false;
		// iterate to find the ontology with filepath
		NodeList lst = root.getElementsByTagName("ontology");
		for (int i = 0; i < lst.getLength(); i++) {
			Element elem = (Element) lst.item(i);
			Attr att = elem.getAttributeNode("filename");
			if (att != null) {
				String filename = att.getValue();
				if (filepath.equals(getAbsoluteOntologyPath(filename))) {
					Attr libatt = elem.getAttributeNode("tagbar");
					if (libatt != null) {
						String library = libatt.getValue();
						if (library != null && library.equals("false")) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	

	/* PRIVATE MEMBERS */

	private static OntologyConfiguration _config = null; // singleton instance
	// private String KEPLER = System.getProperty("KEPLER");
	// private String KEPLER =
	private String ONTO_PATH;
	private String INDEX_FILE;
	private Document _document; // the ontology document
	private File _indexFile;

	private static final Log log = LogFactory.getLog(OntologyConfiguration.class);
	private static final boolean isDebugging = log.isDebugEnabled();
	
}