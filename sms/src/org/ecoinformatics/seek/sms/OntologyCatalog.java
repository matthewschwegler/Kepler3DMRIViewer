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

package org.ecoinformatics.seek.sms;

/**
 *@author    Shawn Bowers
 *
 *  Documentation coming soon
 */

import java.io.FileOutputStream;
import java.util.Vector;

import org.kepler.util.StaticResources;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.tidy.Checker;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Description of the Class
 * 
 *@author berkley
 *@created February 17, 2005
 */
public class OntologyCatalog {

	private static OntologyCatalog _catalog = null;
	// singleton instance
	private String KEPLER = System.getProperty("KEPLER");
	//TODO FIXME hardcoded path:
	private String LOCAL_PATH = KEPLER + "/common/configs/" + StaticResources.RESOURCEBUNDLE_DIR + "/";
	private String DEFAULT_NSPREFIX = "http://seek.ecoinformatics.org/ontology#";
	// this should be obtained from the onto

	// list of OntModel objects
	private Vector<OntModel> _ontologyModels = new Vector<OntModel>();

	/** Constructor for the OntologyCatalog object */
	protected OntologyCatalog() {
		initialize();
	}

	/**
	 *@return The unique instance of this class This must be called to
	 *         create/obtain an instance of the catalog
	 */
	public static OntologyCatalog instance() {
		if (_catalog == null) {
			_catalog = new OntologyCatalog();
		}
		return _catalog;
	}

	/**
	 * Returns the default ontology model for the actor library.
	 * 
	 *@return The defaultOntology value
	 */
	public OntModel getDefaultOntology() {
		try {
			return (OntModel) getOntModels().elementAt(0);
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Gets the ontModels attribute of the OntologyCatalog object
	 * 
	 *@return The ontModels value
	 */
	public Vector<OntModel> getOntModels() {
		return _ontologyModels;
	}

	/**
	 * Returns the first concept in the default onto with the given label
	 * 
	 *@param label
	 *            Description of the Parameter
	 *@return The conceptNameWithLabel value
	 */
	public String getConceptNameWithLabel(String label) {
		OntModel defaultOnt = getDefaultOntology();
		if (defaultOnt == null || label == null) {
			return null;
		}
		ExtendedIterator iter = defaultOnt.listClasses();
		while (iter.hasNext()) {
			OntClass c = (OntClass) iter.next();
			if (label.equals(c.getLabel(null))) {
				return c.getLocalName();
			}
		}
		return null;
	}

	/**
	 * Adds a concept to the default ontology
	 * 
	 *@param conceptName
	 *            The feature to be added to the Concept attribute
	 */
	public void addConcept(String conceptName) {
		addConcept(conceptName, conceptName);
	}

	/**
	 * Adds a concept to the default ontology
	 * 
	 *@param conceptName
	 *            The feature to be added to the Concept attribute
	 *@param conceptLabel
	 *            The feature to be added to the Concept attribute
	 */
	public void addConcept(String conceptName, String conceptLabel) {
		OntModel defaultOnt = getDefaultOntology();
		if (defaultOnt == null) {
			return;
		}
		OntClass c = defaultOnt.createClass(DEFAULT_NSPREFIX + conceptName);
		c.addLabel(conceptLabel, null);
		// no "language"
		writeDefaultModel();
	}

	/**
	 * Assigns a concept as a subconcept to a superconcept
	 * 
	 *@param subConceptName
	 *            Description of the Parameter
	 *@param superConceptName
	 *            Description of the Parameter
	 */
	public void assignSuperConcept(String subConceptName,
			String superConceptName) {
		OntModel defaultOnt = getDefaultOntology();
		if (defaultOnt == null) {
			return;
		}
		// check if sub and sup have been created already
		OntClass sub = defaultOnt.getOntClass(subConceptName);
		if (sub == null) {
			sub = defaultOnt.createClass(DEFAULT_NSPREFIX + subConceptName);
		}
		OntClass sup = defaultOnt.getOntClass(superConceptName);
		if (sup == null) {
			sup = defaultOnt.createClass(DEFAULT_NSPREFIX + superConceptName);
		}
		sub.addSuperClass(sup);
		writeDefaultModel();
	}

	/** Description of the Method */
	protected void writeDefaultModel() {
		OntModel defaultOnt = getDefaultOntology();
		if (defaultOnt == null) {
			return;
		}
		try {
			FileOutputStream hndl = new FileOutputStream(LOCAL_PATH
					+ "ontology.owl");
			defaultOnt.write(hndl);
			hndl.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Description of the Method */
	protected void initialize() {
		// the default ontology is now "ontology.owl"
		String ONTO_FILE = LOCAL_PATH + "ontology.owl";
		try {
			Checker validate = new Checker(false);
			// create and read the ontology model
			OntModel _ontModel = ModelFactory.createOntologyModel(
					OntModelSpec.OWL_MEM_RDFS_INF, null);
			_ontModel.read("file:" + ONTO_FILE);
			_ontologyModels.add(_ontModel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * The main program for the OntologyCatalog class
	 * 
	 *@param args
	 *            The command line arguments
	 */
	public static void main(String[] args) {
		OntologyCatalog catalog = OntologyCatalog.instance();
		catalog.addConcept("MyClass", "My Class Label");
		catalog.assignSuperConcept("MyClass", "WorkflowComponent");
	}

}