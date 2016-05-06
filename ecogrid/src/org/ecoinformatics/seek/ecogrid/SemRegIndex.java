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

package org.ecoinformatics.seek.ecogrid;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Description of the Class
 * 
 *@author berkley
 *@created February 17, 2005
 */
public class SemRegIndex {

	private String LOCAL_PATH = "tests/test/org/ecoinformatics/seek/sms/";
	private String ONTO_FILE = LOCAL_PATH + "test_onto.owl";
	private String INDEX_FILE = LOCAL_PATH + "semreg_index.xml";
	private Document index;

	/**
	 * Constructor
	 */
	public SemRegIndex() {
		System.out.println(">>> onto_file = " + ONTO_FILE);
		System.out.println(">>> index_file = " + INDEX_FILE);

	}

	/**
	 * getActiveClassNames Finds the active domain of class names of the index
	 * 
	 *@return The activeClassNames value
	 */
	public Vector getActiveClassNames() {
		// run through the index checking for instances ...
		return new Vector();
	}

	/**
	 * search default case.
	 * 
	 *@param classname
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	public Vector search(String classname) {
		return search(classname, false);
	}

	/**
	 * search A simple search implementation that finds actors in the index
	 * (identified via their paths) that instantiate the classname. For example,
	 * search("RichnessIndex") finds all actors that instantiate either the
	 * class named "RichnessIndex" or one of its subclasses.
	 * 
	 *@param classname
	 *            Description of the Parameter
	 *@param approx
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	public Vector search(String classname, boolean approx) {
		// load the index into memory for now
		loadIndex();

		// check if valid concept name
		if (!isValidClass(classname, approx)) {
			System.out.println(">>> didn't find classname");
			return new Vector();
		}

		// iterate through the index, searching for a matching item
		return traverseIndex(classname, approx);
	}

	/**
	 * traverseIndex
	 * 
	 *@param classname
	 *            Description of the Parameter
	 *@param approx
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	private Vector traverseIndex(String classname, boolean approx) {
		Vector result = new Vector();
		// get the entry tags
		NodeList entries = index.getElementsByTagName("entry");
		// iterate through each tag, check if there is a match
		for (int i = 0; i < entries.getLength(); i++) {
			Element entry = (Element) entries.item(i);
			checkCondition(classname, entry, result, approx);
		}
		return result;
	}

	/**
	 * checkCondition Determines whether the entry's annotation instantiates
	 * classname, and if so, adds the actor path to result.
	 * 
	 *@param classname
	 *            Description of the Parameter
	 *@param entry
	 *            Description of the Parameter
	 *@param result
	 *            Description of the Parameter
	 *@param approx
	 *            Description of the Parameter
	 */
	private void checkCondition(String classname, Element entry, Vector result,
			boolean approx) {
		// get the annotation node
		Element annotation = (Element) (entry
				.getElementsByTagName("annotation").item(0));
		// get the 'ref' attribute
		String fname = annotation.getAttribute("ref");
		System.out.println(">>> CHECKING CONDITION (" + fname + ")");

		// load the ontology
		OntModel onto = ModelFactory.createOntologyModel(
				OntModelSpec.OWL_MEM_RDFS_INF, null);
		onto.setDynamicImports(true);

		// read in the annotation
		onto.read("file:" + LOCAL_PATH + fname);

		boolean foundMatch = false;
		// get the individuals defined by the annotation
		ExtendedIterator inds = onto.listIndividuals();
		while (inds.hasNext() && !foundMatch) {
			Individual i = (Individual) inds.next();
			// get the direct types of this individual
			ExtendedIterator types = i.listRDFTypes(false);
			while (types.hasNext() && !foundMatch) {
				Resource r = (Resource) types.next();
				OntClass c = (OntClass) r.as(OntClass.class);
				// check if c is a matching class
				System.out.println(">>>> checking " + c);
				if (!approx && c.getLocalName().equals(classname)) {
					foundMatch = true;
				}
				if (approx && approxMatch(c.getLocalName(), classname)) {
					foundMatch = true;
				}
			}
		}
		if (foundMatch) {
			System.out.println(">>> FOUND MATCH!");
			// found a match: build the path, add it to result, and return
			Element item = (Element) (entry.getElementsByTagName("item")
					.item(0));
			result.add(buildResult(item));
		}
	}

	/**
	 * buildResult Note that we assume at least one folder and leafs have at
	 * most one actor here.
	 * 
	 *@param item
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	private Vector buildResult(Element item) {
		Vector result = new Vector();
		// get the folders under item
		NodeList folders = item.getElementsByTagName("folder");
		// get each folder and add to result
		Element folder = item;
		for (int i = 0; i < folders.getLength(); i++) {
			folder = (Element) folders.item(i);
			result.add(folder.getAttribute("name"));
		}
		// get the actor and add to result
		Element actor = (Element) folder.getElementsByTagName("actor").item(0);
		result.add(actor.getAttribute("name"));
		// return the path
		return result;
	}

	/**
	 * isValidClass Note that we are assuming uniformity in that there is only a
	 * single namespace for the ontology, and so below, we only check the
	 * fragments of the URIs of the classes in the ontology. This operation is
	 * just a simple optimization pre-step.
	 * 
	 *@param classname
	 *            Description of the Parameter
	 *@param approx
	 *            Description of the Parameter
	 *@return The validClass value
	 */
	private boolean isValidClass(String classname, boolean approx) {
		// load the ontology
		OntModel onto = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,
				null);
		// read in the ontology
		onto.read("file:" + ONTO_FILE);
		// get the classes in the onto
		Iterator iter = onto.listClasses();
		while (iter.hasNext()) {
			OntClass c = (OntClass) iter.next();
			if (!approx && c.getLocalName().equals(classname)) {
				return true;
			}
			if (approx && approxMatch(c.getLocalName(), classname)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Description of the Method
	 * 
	 *@param val1
	 *            Description of the Parameter
	 *@param val2
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	private boolean approxMatch(String val1, String val2) {
		val1 = val1.toLowerCase();
		val2 = val2.toLowerCase();
		if (val1.indexOf(val2) != -1 || val2.indexOf(val1) != -1) {
			return true;
		}
		return false;
	}

	/**
	 * loadIndex
	 */
	private void loadIndex() {
		// build the factory
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// set validating false; no dtd
		factory.setValidating(false);
		// construct doc builder and parse it
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
			index = builder.parse(new File(INDEX_FILE));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// for testing
	/**
	 * The main program for the SemRegIndex class
	 * 
	 *@param args
	 *            The command line arguments
	 */
	public static void main(String[] args) {
		SemRegIndex index = new SemRegIndex();
		String query = "RichnessIndex";
		Vector result = index.search("RichnessIndex");
		System.out.println("Query for '" + query + "' returned " + result);
	}

}