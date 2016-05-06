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

package org.kepler.sms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owl.model.OWLAnnotation;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLProperty;
import org.semanticweb.owl.vocab.OWLRDFVocabulary;

/**
 * 
 */
public class NamedOntProperty implements Comparable {

	private OWLProperty _ontProperty;
	private String _name;
	private OWLOntology _ontology;
	private NamedOntModel _model;

	public NamedOntProperty(OWLProperty ontProperty, OWLOntology ontology, NamedOntModel model) {
		_ontProperty = ontProperty;
		_ontology = ontology;
		_model = model;
		String str = ontProperty.getAnnotations(ontology, OWLRDFVocabulary.RDFS_LABEL.getURI()).toArray(new OWLAnnotation[1])[0].getAnnotationValue().toString();
//		String str = ontProperty.getLabel(null);
		if (str != null)
			_name = str;
		else
			_name = ontProperty.toString();
	}

	public String toString() {
		return getName();
	}

	/**
	 * @return The domain of the property (a set of NamedOntClasses)
	 */
	public Iterator getDomain(boolean sorted) {
		Set<OWLDescription> domains = _ontProperty.getDomains(_ontology);
		List<NamedOntClass> classes = new ArrayList<NamedOntClass>();
		for (OWLDescription domain : domains) {
			classes.add(new NamedOntClass(domain.asOWLClass(), _ontology));
		}
		if (sorted) {
			Collections.sort(classes);
		}
		return classes.iterator();
	}

	/**
	 * FIXME: Need to support non class ranges
	 */
	public Iterator getRange(boolean sorted) {
		List<NamedOntClass> classes = new ArrayList<NamedOntClass>();
		Set<OWLDescription> ranges = _ontProperty.getRanges(_ontology);
		for (OWLDescription range : ranges) {
			classes.add(new NamedOntClass(range.asOWLClass(), _ontology));
		}

		if (sorted) {
			Collections.sort(classes);
		}
		return classes.iterator();
	}

//	/**
//	 * Given a resource, returns its ontology class if it has one. and null
//	 * otherwise.
//	 */
//	// NOTE: Not used
//	private NamedOntClass getOntClass(OntResource res) {
//		String lname = res.getLocalName();
//		String nspace = res.getNameSpace();
//		OntologyCatalog catalog = OntologyCatalog.instance();
//		return catalog.getNamedOntClass(nspace, lname);
//	}

//	// NOTE: Not used
//	private NamedOntClass getOntClass(OWLDescription res) {
//		res.
//		String lname = res.getLocalName();
//		String nspace = res.getNameSpace();
//		OntologyCatalog catalog = OntologyCatalog.instance();
//		return catalog.getNamedOntClass(nspace, lname);
//	}
	
	public String getName() {
		return _name;
	}

	/**
     */
	public Iterator getNamedSuperProperties(boolean sorted) {
		Vector<NamedOntProperty> result = new Vector<NamedOntProperty>();
		Set<OWLProperty> properties = _ontProperty.getSuperProperties(_ontology);
		for (OWLProperty property : properties) {
			result.add(new NamedOntProperty(property, _ontology, _model));
		}
		if (sorted) {
			Collections.sort(result);
		}
		return result.iterator();
	}

	/**
     */
	public Iterator getNamedSubProperties(boolean sorted) {
		Vector<NamedOntProperty> result = new Vector<NamedOntProperty>();
		Set<OWLProperty> properties = _ontProperty.getSubProperties(_ontology);
		for (OWLProperty property : properties) {
			result.add(new NamedOntProperty(property, _ontology, _model));
		}
		if (sorted) {
			Collections.sort(result);
		}
		return result.iterator();
	}

	public int compareTo(Object obj) {
		String str1 = toString();
		String str2 = obj.toString();
		return str1.compareTo(str2);
	}

	/**
	 * @return The (first) comment associated with the property
	 */
	public String getComment() {
		OWLAnnotation[] comments = _ontProperty.getAnnotations(_ontology, OWLRDFVocabulary.RDFS_COMMENT.getURI()).toArray(new OWLAnnotation[1]);
		if (comments.length == 0) {
			return null;
		}
		return NamedOntModel.parseLabel(comments[0]);
	}

	protected OWLProperty ontProperty() {
		return _ontProperty;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof NamedOntProperty))
			return false;
		NamedOntProperty tmp = (NamedOntProperty) obj;
		if (!tmp.ontProperty().equals(this.ontProperty()))
			return false;
		return true;
	}

} // NamedOntProperty