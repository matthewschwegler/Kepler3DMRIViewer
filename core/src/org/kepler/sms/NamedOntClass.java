/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2010-12-21 17:17:53 -0800 (Tue, 21 Dec 2010) $' 
 * '$Revision: 26584 $'
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

/**
 * 
 */

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.io.RDFXMLOntologyFormat;
import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLAnnotation;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLLabelAnnotation;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyStorageException;
import org.semanticweb.owl.model.OWLRuntimeException;
import org.semanticweb.owl.vocab.OWLRDFVocabulary;

public class NamedOntClass implements Comparable {

	protected OWLClass _ontClass;
	protected OWLOntology _ontology;
	protected String _name;
	protected NamedOntModel _model = null;
	protected boolean _orphaned;
	private boolean _removable = true;

	public String getManualUrl() {
		return _manualUrl;
	}

	public void setManualUrl(String _manualUrl) {
		this._manualUrl = _manualUrl;
	}

	protected String _manualUrl;

	public boolean isOrphaned() {
		return _orphaned;
	}

	public void setOrphaned(boolean orphaned) {
		this._orphaned = orphaned;
	}

	public NamedOntClass(String url, String label) {
		this.setOrphaned(true);
		this.setManualUrl(url);
		_name = label;
	}
	
	public NamedOntClass(String text) {
		OntologyCatalog catalog = OntologyCatalog.instance();
		_ontology = catalog.getDefaultOntology();
		_name = text;
		_ontClass = catalog.getConceptWithLabel(text);
		_orphaned = false;
		if (_ontClass == null) {
			// This isn't already in the default ontology, so we have to deal with that.
			OWLOntologyManager manager = OntologyCatalog.getManager();
			OWLDataFactory factory = manager.getOWLDataFactory();
			OWLClass cls = factory.getOWLClass(URI.create(_ontology.getURI() + makeNameIntoOWLClass(text)));
			OWLLabelAnnotation label = factory.getOWLLabelAnnotation(text);
			OWLAxiom axiom = factory.getOWLEntityAnnotationAxiom(cls, label);
			try {
				manager.applyChange(new AddAxiom(_ontology, axiom));
			}
			catch(OWLOntologyChangeException ex) {
				log.error("Error adding new class and label: " + text, ex);
			}
			try {
				// NOTE: Must specify a separate physical URI or it doesn't
				// NOTE: know where it is. The logical URI is no help.
				Iterator<NamedOntModel> iterator = catalog.getNamedOntModels();
				while (iterator.hasNext()) {
					NamedOntModel model = iterator.next();
					if (isDefaultModel(model)) {
						manager.saveOntology(_ontology, new RDFXMLOntologyFormat(), model.getFile().toURI());
						log.info("Saved ontology");
						break;
					}
				}
			}
			catch(OWLOntologyStorageException ex) {
				log.error("Error saving ontology", ex);
			}

			_ontClass = cls;
		}
	}

	private void setModel() {
		OntologyCatalog catalog = OntologyCatalog.instance();
		Iterator<NamedOntModel> iterator = catalog.getNamedOntModels();
		while(iterator.hasNext()) {
			NamedOntModel model = iterator.next();
			if (model.getOntology().equals(_ontology)) {
				_model = model;
			}
		}
		
	}

	public NamedOntClass getThisClass() {
		return this;
	}

	private boolean isDefaultModel(NamedOntModel model) {
		return Constants.defaultOntologyName.equals(model.getName());
	}

	private String makeNameIntoOWLClass(String text) {
		return makeNameIntoOWLClassByAddingUnderscores(text);
	}
	
	private String makeNameIntoOWLClassByAddingUnderscores(String text) {
		StringBuilder buffer = new StringBuilder("#");
		for (String chunk : text.split("\\s+")) {
			buffer.append(chunk).append("_");
		}
		String string = buffer.toString();
		return string.substring(0, string.length() - 1);
	}
	
	// This has been deprecated in favor of the whitespace replacement
	// approach (see: makeNameIntoOWLClassByAddingUnderscores(String).
/*
	private String makeNameIntoOWLClassByRemovingWhitespace(String text) {
		StringBuilder buffer = new StringBuilder("#");
		for (String chunk : text.split("\\s+")) {
			buffer.append(chunk);
		}
		return buffer.toString();
	}
*/

	public NamedOntClass(OWLClass ontClass, OWLOntology ontology) {
		_ontClass = ontClass;
		_ontology = ontology;
		try {
			String str = NamedOntModel.parseLabel(ontClass.getAnnotations(ontology, OWLRDFVocabulary.RDFS_LABEL.getURI()).toArray(new OWLAnnotation[1])[0]);		
			if (str != null)
				_name = str;
			else
				_name = ontClass.toString();
		}
		catch(NullPointerException ex) {
			_name = ontClass.toString();
		}
	}
	
	public NamedOntClass(NamedOntClass noc) {
		this._ontClass = noc.ontClass();
		this._ontology = noc.getOntology();
		this._name = noc.getName();
	}

	public boolean isRemovable() {
		return _removable;
	}
	
	public void setRemovable(boolean removable) {
		_removable = removable;
	}

	public static class IteratorIterable<T> implements Iterable<T> {
		public IteratorIterable(Iterator<T> iterator) {
			this.iterator = iterator;
		}

		public Iterator<T> iterator() {
			return this.iterator;
		}

		private Iterator<T> iterator;
	}
	
	public static NamedOntClass createNamedOntClassFromURI(String tagURIString) {
		String ontologyURIString;
		String[] parts = tagURIString.split("#");
		if (parts.length < 2) {
			return null;
		}
		else {
			ontologyURIString = parts[0];
		}
		return createNamedOntClassFromURIs(tagURIString, ontologyURIString);
	}

	public static NamedOntClass createNamedOntClassFromURIs(String tagURIString, String ontologyURIString) {
		//String originalTagURIString = tagURIString;

		String[] parts = tagURIString.split("#");
		
		String label;
		if (parts.length > 2) {
			label = parts[2];
			tagURIString = parts[0] + "#" + parts[1];
		}
		else {
			label = parts[1];
		}
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLClass cls = manager.getOWLDataFactory().getOWLClass(URI.create(tagURIString));
		
		NamedOntModel theModel = null;
		for (NamedOntModel model : new IteratorIterable<NamedOntModel>(OntologyCatalog.instance().getNamedOntModels())) {
			if (ontologyURIString.equals(model.getOntology().getURI().toString())) {
				theModel = model;
			}
		}
		if (theModel == null) {
			// Model not found
			/// changed 12.21.10
			///return new NamedOntClass(originalTagURIString, label);
			/// sometimes the originalTagURIString looks like (dont want extra label):
			/// urn:lsid:localhost:onto:797050988:1#testtag#testtag
			return new NamedOntClass(tagURIString, label);
		}
		
		OWLOntology ontology = theModel.getOntology();
		return new NamedOntClass(cls, ontology);
	}

	public String toString() {
		return getName();
	}

	public OWLClass ontClass() {
		return _ontClass;
	}
	
	public OWLOntology getOntology() {
		return _ontology;
	}

	public String getName() {
		return _name;
	}
	
	private static Pattern parseAnnotation = Pattern.compile("\"(.*)\"\\^\\^string");
	
	public String getComment() {
		try {
			OWLAnnotation annotation = _ontClass.getAnnotations(_ontology, OWLRDFVocabulary.RDFS_COMMENT.getURI()).iterator().next();
			String rawAnnotation = annotation.getAnnotationValue().toString();
			Matcher matcher = parseAnnotation.matcher(rawAnnotation);
			if (matcher.matches()) {
				return matcher.group(1);
			}
			return rawAnnotation;
		}
		catch(NoSuchElementException ex) {
			return "";
		}
	}
	
	public NamedOntModel getModel() {
		if (_model == null) {
			setModel();
		}
		return _model;
	}

	public String getOntologyName() {
		String nspace = getNameSpace();
		OntologyCatalog _catalog = OntologyCatalog.instance();
		return _catalog.getOntologyName(nspace);
	}

	public String getLocalName() {
		String uriString = getConceptUri();
		return uriString.split("#")[1];
//		return _ontClass.toString();
	}
	
	public boolean hasNameSpace() {
		String uriString = getConceptUri();
//		String uriString = _ontClass.getURI().toString();
		String localName = getLocalName();
		assert(uriString.endsWith(localName));
		return uriString.length() - localName.length() > 1;
	}
	
	private String getConceptUri() {
		if (isOrphaned()) {
			return getManualUrl();
		}
		else {
			return _ontClass.getURI().toString();
		}		
	}
	
	public String getNameSpace() {
		if (hasNameSpace()) {
			return _getNameSpace();
		}
		else {
			return _ontology.getURI().toString() + "#";
		}
	}

	public String _getNameSpace() {
		String uriString = getConceptUri();
		String localName = getLocalName();
		assert(uriString.endsWith(localName));
		return uriString.substring(0, uriString.length() - localName.length());
	}

	public String getConceptId() {
		return getNameSpace() + getLocalName();
	}
	
	public String getConceptIdWithLabel() {
		return getConceptId() + "#" + getName();
	}

	/**
	 * @param sorted
	 *            Return sorted list if true.
	 */
	public Iterator<NamedOntClass> getNamedSubClasses(boolean sorted) {
		Vector<NamedOntClass> result = new Vector<NamedOntClass>();
		for (OWLDescription desc : _ontClass.getSubClasses(_ontology)) {
			OWLClass subcls = desc.asOWLClass();
			boolean hasLabel = !subcls.getAnnotations(_ontology, OWLRDFVocabulary.RDFS_LABEL.getURI()).isEmpty();
			if (hasLabel || subcls.toString() != null) {
				// NOTE: Does subcls.toString == null happen?
				result.add(new NamedOntClass(subcls, _ontology));
			}
		}
		if (sorted) {
			Collections.sort(result);
		}
		return result.iterator();
	}

	/**
	 * @param sorted
	 *            Return sorted list if true.
	 */
	public Iterator<NamedOntClass> getNamedSuperClasses(boolean sorted) {
		Vector<NamedOntClass> result = new Vector<NamedOntClass>();
		for (OWLDescription desc : _ontClass.getSuperClasses(_ontology)) {
			OWLClass supcls;
			try {
				supcls = desc.asOWLClass();
			}
			catch(OWLRuntimeException ex) {
				log.warn("Failed to parse: " + desc + " (" + desc.getClass().getName() + ")");
				continue;
			}
			boolean hasLabel = !supcls.getAnnotations(_ontology, OWLRDFVocabulary.RDFS_LABEL.getURI()).isEmpty();
			if (hasLabel || supcls.toString() != null) {
				// NOTE: Does supcls.toString == null happen?
				result.add(new NamedOntClass(supcls, _ontology));
			}
		}
		if (sorted) {
			Collections.sort(result);
		}
		return result.iterator();
	}

	public Iterator<NamedOntProperty> getNamedProperties(boolean sorted) {
		return Collections.<NamedOntProperty>emptyList().iterator();
//		Vector<NamedOntProperty> result = new Vector<NamedOntProperty>();
//
//		Set<OWLAnnotation> annotations = _ontClass.getAnnotations(_ontology);
////		for (OWLAnnotation annotation : annotations) {
////			annotation.getAnno
////		}
//		for (Iterator<OWLProperty> iter = _ontClass.listDeclaredProperties(true); iter
//				.hasNext();) {
//			OWLProperty prop = (OWLProperty) iter.next();
//			result.add(new NamedOntProperty(prop));
//		}
//		if (sorted)
//			Collections.sort(result);
//		return result.iterator();
	}

//	/**
//	 * @return The (first) comment associated with the class
//	 */
//	// NOTE: Not used
//	public String getComment() {
//		return _ontClass.getComment(null);
//	}

//	/**
//	 * @return Answer true if the given class is a "direct" sub-class of this
//	 *         class.
//	 */
//	// NOTE: Not used - but would be easy to convert
//	public boolean isDirectSubClass(NamedOntClass cls) {
//		for (Iterator<NamedOntClass> iter = getNamedSubClasses(false); iter.hasNext();) {
//			NamedOntClass tmpCls = (NamedOntClass) iter.next();
//			if (tmpCls.equals(cls))
//				return true;
//		}
//		return false;
//	}

	/**
	 * @return Answer true if the given class is a sub-class of this class.
	 */
	public boolean isSubClass(NamedOntClass cls) {
		// return _ontClass.hasSubClass(cls.ontClass(), false);
		Set<OWLDescription> descriptions = this.ontClass().getSubClasses(this.getOntology());
		for (OWLDescription description : descriptions) {
			NamedOntClass newClass = new NamedOntClass(description.asOWLClass(), this.getOntology());
			if (cls.equals(newClass)) {
				return true;
			}
		}
		return false;
	}

//	/**
//	 * @return Answer true if the given class is a "direct" super-class of this
//	 *         class.
//	 */
//	// NOTE: Not used - but would be easy to convert
//	public boolean isDirectSuperClass(NamedOntClass cls) {
//		// return _ontClass.hasSuperClass(cls.ontClass(), true);
//		for (Iterator<NamedOntClass> iter = getNamedSuperClasses(false); iter.hasNext();) {
//			NamedOntClass tmpCls = (NamedOntClass) iter.next();
//			if (tmpCls.equals(cls))
//				return true;
//		}
//		return false;
//	}

//	/**
//	 * @return Answer true if the given class is a super-class of this class.
//	 */
//	// NOTE: Not used - but would be easy to convert
//	public boolean isSuperClass(NamedOntClass cls) {
//		// return _ontClass.hasSuperClass(cls.ontClass(), false);
//		for (Iterator<NamedOntClass> iter = getNamedSuperClasses(false); iter.hasNext();) {
//			NamedOntClass tmpCls = (NamedOntClass) iter.next();
//			if (tmpCls.equals(cls))
//				return true;
//			if (tmpCls.isSuperClass(cls))
//				return true;
//		}
//		return false;
//	}

	/**
	 * @return Answer true if the given class is disjoint with this class.
	 */
	// NOTE: Not used
	public boolean isDisjointWith(NamedOntClass cls) {
		Set<OWLDescription> descriptions = ontClass().getDisjointClasses(this.getOntology());
		for (OWLDescription description : descriptions) {
			NamedOntClass newClass = new NamedOntClass(description.asOWLClass(), this.getOntology());
			if (cls.equals(newClass)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return Answer true if the given class is equivalent to this class.
	 */
	// NOTE: Not used
	public boolean isEquivalent(NamedOntClass cls) {
		Set<OWLDescription> descriptions = ontClass().getEquivalentClasses(this.getOntology());
		for (OWLDescription description : descriptions) {
			NamedOntClass newClass = new NamedOntClass(description.asOWLClass(), this.getOntology());
			if (cls.equals(newClass)) {
				return true;
			}
		}
		return false;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof NamedOntClass))
			return false;
		NamedOntClass tmp = (NamedOntClass) obj;
		String myUri, yourUri;
		myUri = this.isOrphaned() ? this.getManualUrl() : this.getAbsoluteURI().toString();
		yourUri = tmp.isOrphaned() ? tmp.getManualUrl() : tmp.getAbsoluteURI().toString();
		return myUri.equals(yourUri);
	}

	@Override
	public int hashCode() {
		if (isOrphaned()) {
			return getManualUrl().hashCode();
		}
		return getAbsoluteURI().hashCode();
	}

	private URI getAbsoluteURI() {
		// Make OWL class absolute
		URI absoluteOntClassURI;
		if (_ontClass.getURI().toString().startsWith("#")) {
			absoluteOntClassURI = URI.create(_ontology.getURI().toString() + _ontClass.getURI().toString());
		}
		else {
			absoluteOntClassURI = _ontClass.getURI();
		}
		
		return absoluteOntClassURI;
	}

	public int compareTo(Object obj) {
		String str1 = toString();
		String str2 = obj.toString();
		return str1.compareTo(str2);
	}
	
	private static final Log log = LogFactory.getLog(NamedOntClass.class);	

}