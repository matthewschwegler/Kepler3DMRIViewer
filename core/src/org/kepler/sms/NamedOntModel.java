/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2011-04-08 16:06:46 -0700 (Fri, 08 Apr 2011) $' 
 * '$Revision: 27484 $'
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

package org.kepler.sms;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLAnnotation;
import org.semanticweb.owl.model.OWLAnnotationAxiom;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLLabelAnnotation;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyStorageException;
import org.semanticweb.owl.model.OWLProperty;
import org.semanticweb.owl.model.RemoveAxiom;
import org.semanticweb.owl.util.SimpleURIMapper;

/**
 * This class encapsulates/wraps an ontology model. TODO: still developing the
 * interface for this class.
 */
public class NamedOntModel implements Comparable {

    private OWLOntology _ontology; // the ontology in the model
    private String _name; // the name of the ontology, or the namespace
    private OWLOntologyManager _manager;
	private Color color;
	private boolean local;
	
    private String _filePath; // the name of the OWL file

    /**
     * Creates a named ont model for the ontology given in filePath
     * 
     * @param filePath
     *            The path to the file to load. We assume the ontology is
     *            already classified.
     */
    public NamedOntModel(String filePath) throws Exception {
	_filePath = filePath;
    }
	
    public static String parseLabel(OWLAnnotationAxiom axiom) {
	return parseLabel(axiom.getAnnotation());
    }
	
    public static String parseLabel(OWLAnnotation annot) {
	Pattern pattern = Pattern.compile("\"(.*)\"\\^\\^string");
	if(annot == null || annot.getAnnotationValue() == null)
	    return null;
	String a = annot.getAnnotationValue().toString();
	Matcher matcher = pattern.matcher(a);
	if (matcher.matches()) {
	    return matcher.group(1);
	}
	return a;
    }
	
    public String getTopLevelLabel() {
	Set<OWLAnnotationAxiom> axioms = _ontology.getAnnotationAxioms();
	URI labelURI = URI.create("http://www.w3.org/2000/01/rdf-schema#label");
	for (OWLAnnotationAxiom axiom : axioms) {
	    if (axiom.getAnnotation().getAnnotationURI().equals(labelURI) && axiom.getSubject() instanceof OWLOntology) {
		_name = parseLabel(axiom);
		return _name;
	    }
	}
	_name = _ontology.toString();
	return _name;
    }
	
    public void initializeNew(String label) {
	Random random = new Random();
	int i;
	do {
	    i = random.nextInt();
	}
	while(i < 0);
		
	_manager = OntologyCatalog.getManager();
	URI logicalUri = URI.create("urn:lsid:localhost:onto:" + random.nextInt() + ":1");
	URI physicalUri = new File(_filePath).toURI();
	SimpleURIMapper mapper = new SimpleURIMapper(logicalUri, physicalUri);
	_manager.addURIMapper(mapper);

	try {
	    _ontology = _manager.createOntology(logicalUri);
	}
	catch(OWLOntologyCreationException e) {
	    e.printStackTrace();
	}
		
	setTopLevelLabel(label);
	_name = label;		
    }
	
    public void removeTopLevelLabel() {
	if (_ontology == null) {
	    return;
	}
		
	Set<OWLAnnotationAxiom> axioms = _ontology.getAnnotationAxioms();
	URI labelURI = URI.create("http://www.w3.org/2000/01/rdf-schema#label");
	for (OWLAnnotationAxiom axiom : axioms) {
	    if (axiom.getAnnotation().getAnnotationURI().equals(labelURI) && axiom.getSubject() instanceof OWLOntology) {
		try {
		    _manager.applyChange(new RemoveAxiom(_ontology, axiom));
		}
		catch(OWLOntologyChangeException ex) {
		    ex.printStackTrace();
		}
		return;
	    }
	}
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NamedOntModel that = (NamedOntModel) o;

		if (_filePath != null ? !_filePath.equals(that._filePath) : that._filePath != null) return false;
		if (_name != null ? !_name.equals(that._name) : that._name != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = _name != null ? _name.hashCode() : 0;
		result = 31 * result + (_filePath != null ? _filePath.hashCode() : 0);
		return result;
	}

	
    public void setTopLevelLabel(String label) {
	if (_ontology == null) {
	    return;
	}
		
	OWLDataFactory factory = _manager.getOWLDataFactory();
	OWLLabelAnnotation annotation = factory.getOWLLabelAnnotation(label);
	OWLAxiom axiom = factory.getOWLOntologyAnnotationAxiom(_ontology, annotation);
	try {
	    _manager.applyChange(new AddAxiom(_ontology, axiom));
	}
	catch(OWLOntologyChangeException ex) {
	    log.error("Error adding ontology label", ex);
	}
    }
	
    public File getFile() {
	return new File(_filePath);
    }
	
    /**
     * Initializes a named ont model wrapper for the ontology.
     */
    public boolean initialize() {
	_manager = OntologyCatalog.getManager();
	try {
	    _ontology = _manager.loadOntologyFromPhysicalURI(new File(_filePath).toURI());
		SimpleURIMapper mapper = new SimpleURIMapper(_ontology.getURI(), new File(_filePath).toURI());
		_manager.addURIMapper(mapper);
	}
	catch(OWLOntologyCreationException ex) {
	    return false;
	}
	_name = getTopLevelLabel();
	
		// garbage collect to keep memory usage from spiking
		System.gc();
		
		return true;
    }

    /**
     * @return The namespace for the ontology
     */
    public String getNameSpace() {
	return _ontology.getURI().toString();
    }
	
    public OWLOntology getOntology() {
	return this._ontology;
    }

    /**
     * @return The root classes, i.e., without a named superclass, for this
     *         ontology.
     */
    public Iterator<NamedOntClass> getRootClasses(boolean sorted) {
	// get all the classesg
	Vector<OWLClass> classes = new Vector(_ontology.getReferencedClasses());
	Vector<NamedOntClass> results = new Vector();
	boolean foundResult = true;
	// check each class to see if a root
	for (OWLClass c : _ontology.getReferencedClasses()) {		// TODO: Is this an appropriate replacement for listNamedClasses()?
	    for (OWLDescription superClassAxiom : c.getSuperClasses(_ontology)) {
			    
		if(!superClassAxiom.isAnonymous()) {
		    OWLClass s = superClassAxiom.asOWLClass();
		    if (classes.contains(s)) {
			foundResult = false;
			break;
		    }
		}
	    }

	    if (foundResult) {
		results.add(new NamedOntClass(c, _ontology));
	    }
	    foundResult = true;
	}
	if (sorted)
	    Collections.sort(results);
	return results.iterator();
    }
    
    public void write() {
	try {
	    _manager.saveOntology(_ontology);
	}
	catch(OWLOntologyStorageException ex) {
	    ex.printStackTrace();
	}
    }

    /**
     * gets a list of the named classes in the ontology
     * 
     * @param sorted
     *            Return sorted list if true
     * @return A sorted list of named ontology classes
     */
    public Iterator<NamedOntClass> getNamedClasses() {

    	//use TreeSet instead of Vector so contains() is fast. TreeSet also
    	//gives ascending natural ordering sorting for free. fix for bug #4539
    	//Vector<NamedOntClass> results = new Vector<NamedOntClass>();
    	TreeSet<NamedOntClass> results = new TreeSet<NamedOntClass>();
    	NamedOntClass noc = null;
    	for (OWLClass c : _ontology.getReferencedClasses()) {
    		noc = new NamedOntClass(c, _ontology);
    		
    		if (!results.contains(noc))
    			results.add(noc);
    	}
		
    	return results.iterator();
    }

    public NamedOntClass getNamedClass(String name) {
	for(OWLClass c : _ontology.getReferencedClasses()) {
	    String uri = c.getURI().toString();
	    String [] parts = uri.split("#");
	    if(parts.length != 2)
		return null;
	    if(parts[1].equals(name))
		return new NamedOntClass(c, _ontology);
	}
	return null;
    }

    	/**
    	 * gets a list of the named properties in the ontology
    	 *
    	 * @param sorted
    	 *            Return sorted list if true
    	 * @return A sorted list of named ontology classes
    	 */
    	public Iterator<NamedOntProperty> getNamedProperties(boolean sorted) {
			Set<OWLProperty> owlProperties = new HashSet<OWLProperty>();
			owlProperties.addAll(_ontology.getReferencedDataProperties());
			owlProperties.addAll(_ontology.getReferencedObjectProperties());

			List<NamedOntProperty> results = new ArrayList<NamedOntProperty>();
			for (OWLProperty owlProperty : owlProperties) {
				results.add(new NamedOntProperty(owlProperty, _ontology, this));
			}

    		if (sorted) {
    			Collections.sort(results);
			}
    		return results.iterator();
    	}

    /**
     *
     */
    public String toString() {
	return getName();
    }

    /**
     * @return The name of the model
     */
    public String getName() {
	return _name;
    }

    public int compareTo(Object obj) {
	String str1 = toString();
	String str2 = obj.toString();
	return str1.compareTo(str2);
    }

    private static final Log log = LogFactory.getLog(NamedOntModel.class);

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}
} // NamedOntModel