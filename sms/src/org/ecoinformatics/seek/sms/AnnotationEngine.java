/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2011-01-27 17:55:42 -0800 (Thu, 27 Jan 2011) $' 
 * '$Revision: 26857 $'
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.util.DotKeplerManager;
import org.kepler.util.StaticResources;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.EntityLibrary;
import ptolemy.moml.MoMLChangeRequest;
import ptolemy.vergil.tree.EntityTreeModel;
import ptolemy.vergil.tree.VisibleTreeModel;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.tidy.Checker;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdql.Query;
import com.hp.hpl.jena.rdql.QueryEngine;
import com.hp.hpl.jena.rdql.QueryResults;
import com.hp.hpl.jena.rdql.ResultBinding;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * AnnotationEngine Interface { AnnotationEngine instance() Vector
 * getDefaultConceptNames() void addActorAnnotation(String lsid, String
 * conceptName) EntityTreeModel buildDefaultActorLibrary() Vector search(String
 * classname) Vector search(String classname, boolean approx) }
 * 
 *@author berkley
 *@created February 17, 2005
 */
public class AnnotationEngine {
	private static final Log log = LogFactory.getLog(AnnotationEngine.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private boolean _debug = false;
	// set to false to suppress debug messages
	private static AnnotationEngine _engine = null;
	// singleton instance
	private KeplerLocalLSIDService _libService = KeplerLocalLSIDService
			.instance();

	// should be a listener interface
	private EntityTreeModel _currentTreeModel = null;

	// paths to necessary files
	// TODO: make these more robust ...
	private String KEPLER = System.getProperty("KEPLER");
	//TODO FIXME hardcoded path:
	private String LOCAL_PATH = KEPLER + "/common/configs/" + StaticResources.RESOURCEBUNDLE_DIR + "/";
	private String ONTO_FILE = LOCAL_PATH + "ontology.owl";
	private String SCHEMA_FILE = LOCAL_PATH + "annotation-schema.owl";
	// private String ANNOTATION_FILE = LOCAL_PATH + "annotations.owl";
	private static final String ANNOTATION_FILE = new File(DotKeplerManager.getInstance()
			.getTransientModuleDirectory("sms"),"annotations").toString();

	// constants to hold namespaces
	private String SCHEMA_NS = "http://seek.ecoinformatics.org/annotation-schema#";
	private String ONTO_NS = "http://seek.ecoinformatics.org/ontology#";
	// private String ONTO_NS = "urn:lsid:localhost:ontology:1:1#";

	// the annotation model
	private OntModel _ontModelAnnotations;
	// the annotation model
	private OntModel _ontModelAnnotationSchema;

	/**
	 * Constructor
	 */
	protected AnnotationEngine() {
		initialize();
	}

	/**
	 *@return The unique instance of this class This must be called to
	 *         create/obtain an instance of the engine.
	 */
	public static AnnotationEngine instance() {
		if (_engine == null) {
			_engine = new AnnotationEngine();
		}
		return _engine;
	}

	/**
	 * For testing... dumps out the annotations using n3 syntax.
	 */
	public void print() {
		// N3 also works
		_ontModelAnnotations.write(System.out, "N-TRIPLE");
	}

	/**
	 * Reload the ontology and annotation information. Eventually, should check
	 * if changes were made prior to loading.
	 */
	protected void initialize() {
		try {

			Checker validate = new Checker(false);
			debug("loading ontologies and annotations ... ", false);
			// create and read the annotation schema and annotations
			_ontModelAnnotationSchema = ModelFactory.createOntologyModel(
					OntModelSpec.OWL_MEM_RDFS_INF, null);
			_ontModelAnnotationSchema.read("file:" + SCHEMA_FILE);
			// _ontModelAnnotations.read("file:" + ANNOTATION_FILE);
			_ontModelAnnotations = ModelFactory.createOntologyModel(
					OntModelSpec.OWL_MEM_RDFS_INF, null);
			// _ontModelAnnotations.read("File:" + ANNOTATION_FILE);
			debug("OK", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Not Implemented.
	 * 
	 *@return The defaultConceptNames value
	 */
	public Vector<String> getDefaultConceptNames() {
		Iterator iter = ontModelOntology().listClasses();
		Vector<String> result = new Vector<String>();
		while (iter.hasNext()) {
			OntClass c = (OntClass) iter.next();
			result.add(c.getLocalName());
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Annotate an actor (via the lsid) with a given ontology concept id
	 * 
	 *@param lsid
	 *            The feature to be added to the ActorAnnotation attribute
	 *@param conceptName
	 *            The feature to be added to the ActorAnnotation attribute
	 *@exception Exception
	 *                Description of the Exception
	 */
	public void addActorAnnotation(String lsid, String conceptName)
			throws Exception {
		// TODO: this should change with new schema !!!

		// check if the lsid/actor exists
		if (!_libService.isAssignedLSID(lsid)) {
			throw new Exception("Id not registered: " + lsid);
		}

		// get the class with the conceptName
		OntClass conceptClass = ontModelOntology().getOntClass(
				ONTO_NS + conceptName);
		if (conceptClass == null) {
			throw new Exception("Not a valid ontology concept: " + conceptName);
		}

		// make sure we don't already have the same annotation; if so,
		// clean-return
		if (annotationExists(lsid, conceptClass)) {
			return;
		}

		// construct an anonymous Actor instance, and assign an lsid property to
		// it
		OntClass actorClass = _ontModelAnnotationSchema.getOntClass(SCHEMA_NS
				+ "Actor");
		Individual actorInst = _ontModelAnnotations
				.createIndividual(actorClass);
		Property lsidProp = _ontModelAnnotationSchema.getOntProperty(SCHEMA_NS
				+ "lsid");
		actorInst.addProperty(lsidProp, lsid);

		// construct an anonymous ItemTag instance, and assign taggedItem ->
		// Actor and the concept
		OntClass itemTagClass = _ontModelAnnotationSchema.getOntClass(SCHEMA_NS
				+ "ItemTag");
		Individual itemTagInst = _ontModelAnnotations
				.createIndividual(conceptClass);
		itemTagInst.addRDFType(conceptClass);
		Property taggedItemProp = _ontModelAnnotationSchema
				.getOntProperty(SCHEMA_NS + "taggedItem");
		itemTagInst.addProperty(taggedItemProp, actorInst);

		// construct the Annotation and assign annotates -> ItemTag
		OntClass annotClass = _ontModelAnnotationSchema.getOntClass(SCHEMA_NS
				+ "Annotation");
		Individual annotInst = _ontModelAnnotations
				.createIndividual(annotClass);
		Property annotatesProp = _ontModelAnnotationSchema
				.getProperty(SCHEMA_NS + "annotates");
		annotInst.addProperty(annotatesProp, itemTagInst);

		// output addition
		debug("Saving Annotation...", false);
		File file = new File(ANNOTATION_FILE);
		// first, write the xml header:
		FileWriter writer = new FileWriter(file);
		writer.write("<?xml version=\"1.0\"?>\n");
		writer.close();
		// now open the stream for append
		OutputStream output = new FileOutputStream(file, true);
		_ontModelAnnotations.write(output, "RDF/XML-ABBREV");
		output.close();
		// need to update the library
		debug("OK", true);

		// rebuild the library
		NamedObj obj = _libService.getData(lsid);
		rebuildActorLibrary(obj, conceptClass.getLabel(null));
	}

	/**
	 * helper function
	 * 
	 *@param obj
	 *            Description of the Parameter
	 *@param conceptLabel
	 *            Description of the Parameter
	 */
	private void rebuildActorLibrary(NamedObj obj, String conceptLabel) {
		// iterate through the tree to the node with the name of the
		// conceptClass

		if (conceptLabel == null) {
			return;
		}

		addToCurrentTreeModel((NamedObj) _currentTreeModel.getRoot(), obj,
				conceptLabel);
	}

	/**
	 * recursive helper function
	 * 
	 *@param parent
	 *            The feature to be added to the ToCurrentTreeModel attribute
	 *@param obj
	 *            The feature to be added to the ToCurrentTreeModel attribute
	 *@param conceptLabel
	 *            The feature to be added to the ToCurrentTreeModel attribute
	 */
	private void addToCurrentTreeModel(NamedObj parent, NamedObj obj,
			String conceptLabel) {
		// return if parent is null or not a composite entity
		if (parent == null || !(parent instanceof CompositeEntity)) {
			return;
		}
		CompositeEntity folder = (CompositeEntity) parent;
		// see if parent matches concept label and add, otherwise iterate
		if (folder.getName().equals(conceptLabel)) {
			// add and do issue change request
			try {
				NamedObj obj2 = (NamedObj) obj.clone(folder.workspace());
				if (obj2 instanceof ComponentEntity) {
					((ComponentEntity) obj2).setContainer(folder);
				} else if (obj2 instanceof Attribute) {
					((Attribute) obj2).setContainer(folder);
				} else {
					return;
				}
				ChangeRequest request = new MoMLChangeRequest(obj2,
						"adding object to actor library");
				obj2.requestChange(request);
				obj2.executeChangeRequests();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// get children and iterate
			Iterator iter = folder.entityList().iterator();
			while (iter.hasNext()) {
				addToCurrentTreeModel((NamedObj) iter.next(), obj, conceptLabel);
			}
		}
	}

	/**
	 * helper function. returns true if an annotation of actor with lsid and of
	 * type conceptname exists
	 * 
	 *@param lsid
	 *            Description of the Parameter
	 *@param conceptClass
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	private boolean annotationExists(String lsid, OntClass conceptClass) {
		try {
			Vector resultIds = new Vector();
			String str = "";
			str += "select ?item \n";
			str += "where  (?res, " + "<" + RDF.type + ">, " + "<"
					+ conceptClass.getURI() + ">), \n";
			str += "       (?res, " + "<" + SCHEMA_NS
					+ "taggedItem>, ?item), \n";
			str += "       (?item, " + "<" + SCHEMA_NS + "lsid>, '" + lsid
					+ "')";
			Query q = new Query(str);
			q.setSource(_ontModelAnnotations);
			QueryResults results = new QueryEngine(q).exec();
			return results.hasNext();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Constructs the default actor library that is loaded, e.g., when Kepler is
	 * started in graph mode.
	 * 
	 *@param _libPane
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	public EntityTreeModel buildDefaultActorLibrary() {
		EntityTreeModel libraryModel = _libService.getBasicActorLibrary();
		try {
			debug("building tree model ... ", false);
			EntityLibrary root = new EntityLibrary();
			OntClass thing = ontModelOntology().getOntClass(OWL.Thing.getURI());
			Iterator iter = thing.listSubClasses(true);
			// true means direct subclasses
			while (iter.hasNext()) {
				buildTreeModel(root, (OntClass) iter.next());
			}
			debug("OK", true);
			_currentTreeModel = new VisibleTreeModel(root);
			return _currentTreeModel;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * recursive helper function to build the new tree model
	 * 
	 *@param parent
	 *            Description of the Parameter
	 *@param currClass
	 *            Description of the Parameter
	 */
	private void buildTreeModel(EntityLibrary parent, OntClass currClass) {
		try {
			EntityLibrary folder = new EntityLibrary();
			Workspace workspace = folder.workspace();

			// check to make sure it isn't some non-ontology class TODO:
			// change this to hold a list of target namespaces obtained
			// from loading the ontology
			if (!currClass.getNameSpace().equals(ONTO_NS)) {
				return;
			}

			folder.setName(currClass.getLabel(null));
			folder.setContainer(parent);

			// get all annotated item instances of the class
			Iterator iter = getMatchingAnnotatedItemIds(currClass).iterator();

			// for each id add the item with the same id (nested loop; ugh!)
			while (iter.hasNext()) {
				Object id = iter.next();
				NamedObj obj = _libService.getData(id.toString());
				if (obj != null) {
					// clone into current workspace
					NamedObj obj2 = (NamedObj) obj.clone(workspace);
					if (obj2 instanceof ComponentEntity) {
						((ComponentEntity) obj2).setContainer(folder);
					} else if (obj2 instanceof Attribute) {
						((Attribute) obj2).setContainer(folder);
					}
				}
			}
			// get direct subclasses
			iter = currClass.listSubClasses(true);
			while (iter.hasNext()) {
				OntClass c = (OntClass) iter.next();
				buildTreeModel(folder, c);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * helper function. returns a list of id strings that are instances of the
	 * currClass uses a rdql query.
	 * 
	 *@param currClass
	 *            Description of the Parameter
	 *@return The matchingAnnotatedItemIds value
	 */
	private Vector<Object> getMatchingAnnotatedItemIds(OntClass currClass) {
		try {
			Vector<Object> resultIds = new Vector<Object>();
			String str = "";
			str += "select ?lsid \n";
			str += "where  (?res, " + "<" + RDF.type + ">, " + "<"
					+ currClass.getURI() + ">), \n";
			str += "       (?res, " + "<" + SCHEMA_NS
					+ "taggedItem>, ?item), \n";
			str += "       (?item, " + "<" + SCHEMA_NS + "lsid>, ?lsid)";
			Query q = new Query(str);
			q.setSource(_ontModelAnnotations);
			QueryResults results = new QueryEngine(q).exec();
			while (results.hasNext()) {
				ResultBinding res = (ResultBinding) results.next();
				// Return from get is null if not found
				Object obj = res.get("lsid");
				// obj will be a Jena object: resource, property or RDFNode.
				if (obj != null) {
					resultIds.add(obj);
				}
			}
			results.close();
			return resultIds;
		} catch (Exception e) {
			e.printStackTrace();
			return new Vector<Object>();
		}
	}

	/**
	 * for testing
	 * 
	 *@param str
	 *            Description of the Parameter
	 *@param newline
	 *            Description of the Parameter
	 */
	private void debug(String str, boolean newline) {
		if (!_debug) {
			return;
		}
		if (newline) {
			System.out.println(str);
		} else {
			System.out.print("ANNOTATION ENGINE: " + str);
		}
	}

	/**
	 * search default case
	 * 
	 *@param classname
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 */
	public Vector<NamedObj> search(String classname) {
		return search(classname, true);
	}

	/**
	 *@param classname
	 *            the classname (as a term/keyword) to search for
	 *@param approx
	 *            if true, find approximate term matches, and if false, do exact
	 *            matches Searches the ontology matching the term and for
	 *            matches, finds annotated items. For example,
	 *            search("RichnessIndex") finds all actors that instantiate
	 *            either the class named "RichnessIndex" or one of its
	 *            subclasses.
	 *@return Description of the Return Value
	 */
	public Vector<NamedObj> search(String classname, boolean approx) {
		if (isDebugging) log.debug("search("+classname+", "+approx+")");
		// check if valid concept name
		if (!isValidClass(classname, approx)) {
			debug("didn't find classname", true);
			return new Vector<NamedObj>();
		}

		Vector<NamedObj> result = new Vector<NamedObj>();
		// find all the matching class names and their subclasses
		Vector<OntClass> classes = getMatchingClassNames(classname, approx);
		Iterator<OntClass> iter = classes.iterator();
		while (iter.hasNext()) {
			// find the matching lsids for the class name
			OntClass cls = iter.next();
			Vector<Object> ids = getMatchingAnnotatedItemIds(cls);
			Iterator<Object> idIter = ids.iterator();
			while (idIter.hasNext()) {
				// get the associated objects
				NamedObj obj = _libService.getData(idIter.next().toString());
				if (obj != null && !result.contains(obj)) {
					result.add(obj);
				}
				// add the results
			}
		}
		return result;
	}

	/**
	 * helper function to find all class names
	 * 
	 *@param classname
	 *            Description of the Parameter
	 *@param approx
	 *            Description of the Parameter
	 *@return The matchingClassNames value
	 */
	private Vector<OntClass> getMatchingClassNames(String classname, boolean approx) {
		Vector<OntClass> initResult = new Vector<OntClass>();
		// get all classes in ontology
		Iterator iter = ontModelOntology().listClasses();
		while (iter.hasNext()) {
			// find classes that have a similar name
			OntClass cls = (OntClass) iter.next();
			if (approx && approxMatch(cls.getLocalName(), classname)) {
				initResult.add(cls);
			} else if (!approx && classname.equals(cls.getLocalName())) {
				initResult.add(cls);
			}
		}
		Vector<OntClass> result = (Vector) initResult.clone();
		iter = initResult.iterator();
		while (iter.hasNext()) {
			// find all subclasses of direct classes
			OntClass cls = (OntClass) iter.next();
			Iterator clsIter = cls.listSubClasses(false);
			// direct = false
			while (clsIter.hasNext()) {
				OntClass subCls = (OntClass) clsIter.next();
				if (!result.contains(subCls)) {
					result.add(subCls);
				}
			}
		}
		return result;
	}

	/**
	 * helper function for search. Note that we are assuming uniformity in that
	 * there is only a single namespace for the ontology, and so below, we only
	 * check the fragments of the URIs of the classes in the ontology.
	 * 
	 * This operation is just a simple optimization pre-step.
	 * 
	 *@param classname
	 *            Description of the Parameter
	 *@param approx
	 *            Description of the Parameter
	 *@return The validClass value
	 */
	private boolean isValidClass(String classname, boolean approx) {
		// get the classes in the onto
		Iterator iter = ontModelOntology().listClasses();
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
	 * helper function for search
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

	private OntModel ontModelOntology() {
		OntologyCatalog catalog = OntologyCatalog.instance();
		return catalog.getDefaultOntology();
	}

	/**
	 * The main program for the AnnotationEngine class for testing: prints out
	 * the annotations.
	 * 
	 *@param args
	 *            The command line arguments
	 */
	public static void main(String[] args) {
		AnnotationEngine eng = AnnotationEngine.instance();
		eng.print();
	}

}