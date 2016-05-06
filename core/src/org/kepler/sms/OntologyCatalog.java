/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: riddle $'
 * '$Date: 2010-09-27 11:23:32 -0700 (Mon, 27 Sep 2010) $' 
 * '$Revision: 25854 $'
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.modules.ModuleTree;
import org.kepler.sms.util.OntologyConfiguration;
import org.kepler.util.DotKeplerManager;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLAnnotation;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyChangeException;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyStorageException;
import org.semanticweb.owl.util.OWLEntityRemover;
import org.semanticweb.owl.vocab.OWLRDFVocabulary;

/**
 * Created by IntelliJ IDEA. User: sean Date: May 20, 2009 Time: 4:55:34 PM
 */

public class OntologyCatalog {

	static {
		manager = OWLManager.createOWLOntologyManager();
	}

	private Vector<NamedOntModel> _namedOntModels = new Vector<NamedOntModel>();
	private Vector<NamedOntModel> _libraryModels = new Vector<NamedOntModel>();
	private Vector<NamedOntModel> _tagBarModels = new Vector<NamedOntModel>();

	private static OntologyCatalog _catalog = null;
	// singleton instance
	// private String KEPLER = System.getProperty("KEPLER");
	// private String LOCAL_PATH = KEPLER + "/configs/ptolemy/configs/kepler/";
	// private String ONTO_FILE = LOCAL_PATH + "ontology2.owl";
	private String DEFAULT_NSPREFIX = "http://seek.ecoinformatics.org/ontology#";
	// this should be obtained from the onto

	// list of OntModel objects
	private Vector<NamedOntModel> _ontologyModels = new Vector<NamedOntModel>();

	protected OntologyCatalog(boolean initialize) {
		if (initialize) {
			initialize();
		} else {
			initialize(false);
		}
	}

	/**
	 * Constructor for the OntologyCatalog object
	 */
	protected OntologyCatalog() {
		initialize();
	}

	public static OntologyCatalog standaloneInstance() {
		OntologyConfiguration oc = OntologyConfiguration.instance();
		File indexFile = new File(System.getProperty("user.home")
				+ "/.kepler/ontology_catalog.xml");
		oc.setIndexFile(indexFile);
		oc.initialize();
		return instance();
	}

	/**
	 * @return The unique instance of this class This must be called to
	 *         create/obtain an instance of the catalog
	 */
	public static OntologyCatalog instance() {
		if (_catalog == null) {
			_catalog = new OntologyCatalog();
		}
		return _catalog;
	}

	public static OntologyCatalog instanceNoInitialize() {
		if (_catalog == null) {
			_catalog = new OntologyCatalog(false);
		}
		return _catalog;
	}

	private static boolean TEST_MODE = false;

	private OWLOntology createTestOntology()
			throws OWLOntologyCreationException, OWLOntologyChangeException {
		OWLDataFactory factory = manager.getOWLDataFactory();
		URI uri = URI.create("http://www.example.com/test.owl");
		OWLOntology ontology = manager.createOntology(uri);
		OWLClass human = factory.getOWLClass(URI.create(uri + "#human"));
		OWLClass man = factory.getOWLClass(URI.create(uri + "#man"));
		OWLClass woman = factory.getOWLClass(URI.create(uri + "#woman"));
		OWLClass socrates = factory.getOWLClass(URI.create(uri + "#socrates"));
		OWLClass plato = factory.getOWLClass(URI.create(uri + "#plato"));
		OWLClass hildegard = factory
				.getOWLClass(URI.create(uri + "#hildegard"));
		OWLAxiom a1 = factory.getOWLSubClassAxiom(man, human);
		OWLAxiom a2 = factory.getOWLSubClassAxiom(woman, human);
		OWLAxiom a3 = factory.getOWLSubClassAxiom(socrates, man);
		OWLAxiom a4 = factory.getOWLSubClassAxiom(plato, man);
		OWLAxiom a5 = factory.getOWLSubClassAxiom(hildegard, woman);
		manager.applyChange(new AddAxiom(ontology, a1));
		manager.applyChange(new AddAxiom(ontology, a2));
		manager.applyChange(new AddAxiom(ontology, a3));
		manager.applyChange(new AddAxiom(ontology, a4));
		manager.applyChange(new AddAxiom(ontology, a5));
		OWLAxiom b1 = factory.getOWLEntityAnnotationAxiom(human, factory
				.getOWLLabelAnnotation("Human"));
		OWLAxiom b2 = factory.getOWLEntityAnnotationAxiom(man, factory
				.getOWLLabelAnnotation("Man"));
		OWLAxiom b3 = factory.getOWLEntityAnnotationAxiom(woman, factory
				.getOWLLabelAnnotation("Woman"));
		OWLAxiom b4 = factory.getOWLEntityAnnotationAxiom(socrates, factory
				.getOWLLabelAnnotation("Socrates"));
		OWLAxiom b5 = factory.getOWLEntityAnnotationAxiom(plato, factory
				.getOWLLabelAnnotation("Plato"));
		OWLAxiom b6 = factory.getOWLEntityAnnotationAxiom(hildegard, factory
				.getOWLLabelAnnotation("Hildegard"));
		manager.applyChange(new AddAxiom(ontology, b1));
		manager.applyChange(new AddAxiom(ontology, b2));
		manager.applyChange(new AddAxiom(ontology, b3));
		manager.applyChange(new AddAxiom(ontology, b4));
		manager.applyChange(new AddAxiom(ontology, b5));
		manager.applyChange(new AddAxiom(ontology, b6));
		return ontology;
	}

	/**
	 * Returns the default ontology model for the actor library.
	 * 
	 * @return The defaultOntology value
	 */
	public OWLOntology getDefaultOntology() {
		if (TEST_MODE) {
			try {
				return createTestOntology();
			} catch (OWLOntologyCreationException ex) {
				log.error("Problem 1", ex);
			} catch (OWLOntologyChangeException ex) {
				log.error("Problem 2", ex);
			}
		}
		Iterator<NamedOntModel> models = getNamedOntModels();
		while (models.hasNext()) {
			NamedOntModel model = models.next();
			if (Constants.defaultOntologyName.equals(model.getName())) {
				return model.getOntology();
			}
		}
		return this.createDefaultOntology();
		// return (OWLOntology) getOntModels().elementAt(0);
	}

	public OWLOntology createDefaultOntology() {
		OntologyCatalog catalog = OntologyCatalog.instance();
		OWLOntology ontology = catalog.addModel(true,
				Constants.defaultOntologyName, true).getOntology();
		catalog.saveCatalog();
		try {
			// LibraryManager.getInstance().addOntology(OntologyCatalog.instance().getNamedOntModel(Constants.defaultOntologyName));
		} catch (Exception ex) {
			log.error("Error adding default ontology", ex);
		}
		return ontology;

	}

	/**
	 * Gets the ontModels attribute of the OntologyCatalog object
	 * 
	 * @return The ontModels value
	 */
	public Vector<NamedOntModel> getOntModels() {
		return _ontologyModels;
	}

	/**
	 * Get the named ontology model with the given name
	 * 
	 * @param name
	 *            the ontology name
	 * @return the NamedOntModel if it exists
	 */
	public NamedOntModel getNamedOntModel(String name) {
		Iterator<NamedOntModel> models = getNamedOntModels();
		while (models.hasNext()) {
			NamedOntModel model = models.next();
			if (model.getName().equals(name))
				return model;
		}
		return null;
	}

	/**
	 * Returns the first concept in the default onto with the given label
	 * 
	 * @param label
	 *            Description of the Parameter
	 * @return The conceptNameWithLabel value
	 */
	public String getConceptNameWithLabel(String label) {
		try {
			return getConceptWithLabel(label).toString();
		} catch (NullPointerException ex) {
			return null;
		}
	}

	public String getConceptName(OWLClass cls) {
		return cls.toString();
	}

	public String getLabelNameWithConcept(String conceptName) {
		OWLOntology defaultOnt = getDefaultOntology();
		if (defaultOnt == null || conceptName == null) {
			return null;
		}
		Set<OWLClass> classes = defaultOnt.getReferencedClasses();
		for (OWLClass c : classes) {
			if (conceptName.equals(c.toString())) {
				return getLabelNameWithConcept(c);
			}
		}
		return null;
	}

	public String getLabelNameWithConcept(NamedOntClass cls) {
		OWLOntology ontology = cls.getOntology();
		OWLClass owlClass = cls.ontClass();
		String label = getLabelNameWithConcept(ontology, owlClass);
		if (label != null) {
			// Not an orphan
			return label;
		}

		// Orphan!
		String[] parts = cls.getManualUrl().split("#");
		return parts[parts.length - 1];

	}

	public String getLabelNameWithConcept(OWLOntology ontology, OWLClass cls) {
		if (ontology == null || cls == null) {
			return null;
		}
		Set<OWLAnnotation> annotations = cls.getAnnotations(ontology,
				OWLRDFVocabulary.RDFS_LABEL.getURI());
		try {
			return Util.parseOWLSerializedString(annotations
					.toArray(new OWLAnnotation[1])[0].getAnnotationValue()
					.toString());
		} catch (NullPointerException ex) {
			String[] parts = cls.getURI().toString().split("#");
			return parts[parts.length - 1];
		}
	}

	public String getLabelNameWithConcept(OWLClass cls) {
		return getLabelNameWithConcept(getDefaultOntology(), cls);
	}

	/**
	 * Add a class with the given name to the ontology
	 * 
	 * @param model
	 *            the NamedOntModel to create the class in
	 * @param name
	 *            the name of the class
	 * @return the NamedOntModel
	 */
	public NamedOntClass createClass(NamedOntModel model, String name) {
		String id = name.trim().replaceAll("\\s", "");
		OWLDataFactory factory = manager.getOWLDataFactory();
		String fullId = model.getNameSpace() + "#" + id;
		OWLClass owlClass = factory.getOWLClass(URI.create(fullId));
		OWLAxiom axiom1 = factory.getOWLDeclarationAxiom(owlClass);
		OWLAnnotation annot = factory.getOWLLabelAnnotation(name);
		OWLAxiom axiom2 = factory.getOWLEntityAnnotationAxiom(owlClass, annot);
		try {
			manager.applyChange(new AddAxiom(model.getOntology(), axiom1));
			manager.applyChange(new AddAxiom(model.getOntology(), axiom2));
		} catch (OWLOntologyChangeException ex) {
			log.error("Error adding class declaration axiom to ontology");
		}
		return new NamedOntClass(owlClass, model.getOntology());
	}

	public NamedOntClass createClass(NamedOntModel model, String className,
			NamedOntClass superClass) {
		NamedOntClass ontClass = createClass(model, className);
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLAxiom axiom = factory.getOWLSubClassAxiom(ontClass.ontClass(),
				superClass.ontClass());
		try {
			manager.applyChange(new AddAxiom(model.getOntology(), axiom));
		} catch (OWLOntologyChangeException ex) {
			log.error("Error adding subclass axiom");
		}
		return ontClass;
	}

	/**
	 * Adds a concept to the default ontology
	 * 
	 * @param conceptName
	 *            The feature to be added to the Concept attribute
	 */
	public void addConcept(String conceptName) {
		addConcept(conceptName, conceptName);
	}

	/**
	 * Adds a concept to the default ontology
	 * 
	 * @param conceptName
	 *            The feature to be added to the Concept attribute
	 * @param conceptLabel
	 *            The feature to be added to the Concept attribute
	 */
	public void addConcept(String conceptName, String conceptLabel) {
		OWLOntology defaultOnt = getDefaultOntology();
		if (defaultOnt == null) {
			return;
		}
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass c = factory.getOWLClass(URI.create(DEFAULT_NSPREFIX
				+ conceptName));

		OWLAxiom axiom = factory.getOWLDeclarationAxiom(c);
		OWLAxiom axiom2 = factory.getOWLEntityAnnotationAxiom(c, factory
				.getOWLLabelAnnotation(conceptLabel));
		try {
			manager.applyChange(new AddAxiom(defaultOnt, axiom));
			manager.applyChange(new AddAxiom(defaultOnt, axiom2));
		} catch (OWLOntologyChangeException ex) {
			log.error("Error adding class declaration axiom to ontology", ex);
		}
		writeDefaultModel();
	}

	public void removeConcept(NamedOntClass ontClass) {
		OWLOntology ontology = ontClass.getOntology();
		OWLEntityRemover remover = new OWLEntityRemover(manager, Collections
				.singleton(ontology));
		ontClass.ontClass().accept(remover);
		try {
			manager.applyChanges(remover.getChanges());
		} catch (OWLOntologyChangeException ex) {
			log.error("Error removing concept", ex);
		}
	}

	/**
	 * Assigns a concept as a subconcept to a superconcept
	 * 
	 * @param subConceptName
	 *            Description of the Parameter
	 * @param superConceptName
	 *            Description of the Parameter
	 */
	public void assignSuperConcept(String subConceptName,
			String superConceptName) {
		OWLOntology defaultOnt = getDefaultOntology();
		if (defaultOnt == null) {
			return;
		}
		// check if sub and sup have been created already
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass sub = factory.getOWLClass(URI.create(DEFAULT_NSPREFIX
				+ subConceptName));
		OWLClass sup = factory.getOWLClass(URI.create(DEFAULT_NSPREFIX
				+ superConceptName));
		OWLAxiom axiom = factory.getOWLSubClassAxiom(sub, sup);
		try {
			manager.applyChange(new AddAxiom(defaultOnt, axiom));
		} catch (OWLOntologyChangeException ex) {
			log.error("Error adding subclass axiom to ontology", ex);
		}
		writeDefaultModel();
	}

	public void write() {
		writeDefaultModel();
	}

	/**
	 * Description of the Method
	 */
	protected void writeDefaultModel() {
		OWLOntology defaultOnt = getDefaultOntology();
		if (defaultOnt == null) {
			return;
		}
		try {
			manager.saveOntology(defaultOnt);
		} catch (OWLOntologyStorageException ex) {
			log.error("Error saving default model", ex);
		}
	}

	protected void initialize() {
		initialize(true);
	}

	/**
	 * Description of the Method
	 */
	protected void initialize(boolean init) {
		_namedOntModels.clear();
		_libraryModels.clear();
		_tagBarModels.clear();
		if (init) {
			OntologyConfiguration.instance().initialize();
		}
		// the default ontology is now "ontology.owl"
		// manager = OWLManager.createOWLOntologyManager();
		OntologyConfiguration config = OntologyConfiguration.instance();

		// for each ontology create a NamedOntModel from the
		for (Iterator iter = config.getFilePathNames(); iter.hasNext();) {
			String ontoFilePath = (String) iter.next();
			if (isDebugging)
				log.debug(ontoFilePath);
			NamedOntModel mdl;
			try {
				mdl = new NamedOntModel(ontoFilePath);
				boolean success = mdl.initialize();
				if (!success) {
					System.out.println("Could not instantiate model: '" + ontoFilePath + "'");
					return;
				}
				mdl.setColor(config.getLibraryColor(ontoFilePath));
				mdl.setLocal(config.isLibraryLocal(ontoFilePath));
				if (isDebugging)
					log.debug(mdl.getNameSpace());

				_namedOntModels.add(mdl);
				if (isDebugging)
					log.debug("_namedOntModels.add(" + mdl.getName() + ")");

				if (config.isLibraryOntology(ontoFilePath)) {
					_libraryModels.add(mdl);
					if (isDebugging)
						log.debug("_libraryModels.add(" + mdl.getName() + ")");
				}
				// TODO: This can be made more efficient. We don't need to
				// double
				// parse it.
				if (config.isTagBarOntology(ontoFilePath)) {
					_tagBarModels.add(mdl);
					if (isDebugging)
						log.debug("_tagBarModels.add(" + mdl.getName() + ")");
				}
			} catch (Exception e) {
				JFrame f = new JFrame();
				String errmsg = "Cannot locate ontology: " + ontoFilePath;
				JOptionPane.showMessageDialog(f, errmsg, "Configuration Error",
						JOptionPane.WARNING_MESSAGE);
				f.dispose();
			}
		}
	}

	/**
	 * The main program for the OntologyCatalog class
	 * 
	 * @param args
	 *            The command line arguments
	 */
	public static void main(String[] args) {
		OntologyCatalog catalog = OntologyCatalog.instance();
		catalog.addConcept("MyClass", "My Class Label");
		catalog.assignSuperConcept("MyClass", "WorkflowComponent");
	}

	public void setInLibrary(NamedOntModel model, boolean isInLibrary) {
		if (isInLibrary && !_libraryModels.contains(model)) {
			_libraryModels.add(model);
		} else if (!isInLibrary && _libraryModels.contains(model)) {
			_libraryModels.remove(model);
		}
	}

	public void setInTagBar(NamedOntModel model, boolean isInTagBar) {
		if (isInTagBar && !_tagBarModels.contains(model)) {
			_tagBarModels.add(model);
		} else if (!isInTagBar && _tagBarModels.contains(model)) {
			_tagBarModels.remove(model);
		}
	}

	/**
	 * Save out catalog to catalog filename
	 */
	public void saveCatalog() {
		Vector<OntologyRow> rows = new Vector<OntologyRow>();
		OntologyConfiguration config = OntologyConfiguration.instance();
		for (NamedOntModel model : _namedOntModels) {
			String path = model.getFile().getPath();
			if (isDebugging)
				log.debug("Processing ontology: '" + model.getName() + "'");
			boolean local = Constants.defaultOntologyName.equals(model
					.getName())
					|| model.isLocal();
			if (isDebugging)
				log.debug("Looking for the color of: " + path);
			Color color = config.getLibraryColor(path);
			rows.add(new OntologyRow(model, _libraryModels.contains(model),
					_tagBarModels.contains(model), color, local));
		}
		saveCatalog(rows);
	}

	public void saveCatalog(Vector<OntologyRow> rows) {
		// check paths
		for (OntologyRow row : rows) {
			File file = row.getModel().getFile();
			if (!file.exists()) {
				System.out.println("File path corruption detected. Aborting write.");
				return;
			}
		}
		OntologyConfiguration config = OntologyConfiguration.instance();
		File indexFile = config.getIndexFile();
		File directory = ModuleTree.instance().getModuleByStemName("common").getDir();
		if (indexFile.getAbsolutePath().startsWith(directory.getAbsolutePath())) {
			System.out.println("Preparing to write common ontology_catalog.xml file. Aborting.");
			return;
		}

		try {
			BufferedWriter writer = new BufferedWriter(
					new FileWriter(indexFile));
			writer.write("<?xml version=\"1.0\"?>\n<ontologies>\n");
			for (OntologyRow row : rows) {
				writer.write("<ontology filename=\""
						+ row.getModel().getFile().getAbsolutePath()
						+ "\" library=\"");
				if (row.isInLibrary()) {
					writer.write("true");
				} else {
					writer.write("false");
				}

				writer.write("\" tagbar=\"");
				if (row.isInTagBar()) {
					writer.write("true");
				} else {
					writer.write("false");
				}
				writer.write("\" color=\"" + row.getColor());

				writer.write("\" local=\"");
				if (row.isLocal()) {
					writer.write("true");
				} else {
					writer.write("false");
				}

				writer.write("\"/>\n");
			}
			writer.write("</ontologies>\n");
			writer.close();
		} catch (IOException ex) {
			log.error("IO error", ex);
		}
		// OntologyConfiguration.instance().reset();
		initialize();
	}

	private Set<String> getTopLevelLabels() {
		Set<String> labels = new HashSet<String>();
		for (NamedOntModel model : _namedOntModels) {
			labels.add(model.getTopLevelLabel());
		}
		return labels;
	}

	private String getNewOntologyName() {
		Set<String> labels = getTopLevelLabels();
		int i = 1;
		while (true) {
			String label = "Untitled " + i;

			// Is it taken?
			if (!labels.contains(label)) {
				// Nope, grab it.
				return label;
			}
			// Yes
			++i;
		}
	}

	public NamedOntModel addModel(boolean isInLibrary, String label,
			boolean editable) {
		File coreModuleDir = DotKeplerManager.getInstance()
				.getTransientModuleDirectory("core");
		NamedOntModel model = null;
		try {
			File temporaryFile = File.createTempFile("onto", ".owl",
					coreModuleDir);
			try {
				model = new NamedOntModel(temporaryFile.getAbsolutePath());
				model.setLocal(editable);
				if (label == null) {
					model.initializeNew(getNewOntologyName());
				} else {
					model.initializeNew(label);
				}
				model.write();
				addModel(model, isInLibrary);
			} catch (Exception ex) {
				log.error("Error", ex);
			}
		} catch (IOException ex) {
			log.error("Error", ex);
		}
		return model;
	}

	public void addModel(NamedOntModel model, boolean isInLibrary) {
		_namedOntModels.add(model);
		if (isInLibrary) {
			_libraryModels.add(model);
		}
	}

	public Iterator<NamedOntModel> getNamedOntModels() {
		return _namedOntModels.iterator();
	}

	/**
	 * Provides the set of named ontology models
	 * 
	 *@param libraryOnly
	 *            if true, return only the library models
	 *@return the set of named ontology models in the catalog
	 */
	public Iterator<NamedOntModel> getNamedOntModels(boolean libraryOnly) {
		if (libraryOnly) {
			return getLibraryNamedOntModels();
		} else {
			return getNamedOntModels();
		}
	}

	/**
	 * Provides the set of named ontology models used in the actor library
	 * 
	 *@return the unique instance of this class
	 */
	public Iterator<NamedOntModel> getLibraryNamedOntModels() {
		return _libraryModels.iterator();
	}

	public Iterator<NamedOntModel> getTagBarOntModels() {
		return _tagBarModels.iterator();
	}

	/**
	 * The set of ontology names in the catalog
	 * 
	 *@return The set of names for ontologies
	 */
	public Iterator<String> getOntologyNames() {
		Vector<String> results = new Vector<String>();
		for (Iterator<NamedOntModel> iter = getNamedOntModels(); iter.hasNext();) {
			NamedOntModel m = iter.next();
			results.add(m.getName());
		}
		return results.iterator();
	}

	/**
	 * The set of ontology names for use in the catalog
	 * 
	 *@return The libraryOntologyNames value
	 */
	public Iterator<String> getLibraryOntologyNames() {
		Vector<String> results = new Vector<String>();
		for (Iterator<NamedOntModel> iter = getLibraryNamedOntModels(); iter
				.hasNext();) {
			NamedOntModel m = iter.next();
			results.add(m.getName());
		}
		return results.iterator();
	}

	/**
	 * Maps the namespace of an ontology to the given ontology name
	 * 
	 *@param namespace
	 *            the namespace of the desired ontology
	 *@return the name of the ontology for the given namespace. The namespace
	 *         can optionally end in #
	 */
	public String getOntologyName(String namespace) {
		for (Iterator<NamedOntModel> iter = getNamedOntModels(); iter.hasNext();) {
			NamedOntModel m = iter.next();
			String nspace = m.getNameSpace() + "#";
			if (m.getNameSpace().equals(namespace) || nspace.equals(namespace)) {
				return m.getName();
			}
		}
		return null;
	}

	/**
	 * Maps semantic types to their NamedOntClass objects. Tries to optimize the
	 * search assuming '#' is used to distinguish namespaces from local names.
	 * 
	 *@param st
	 *            the semantic type to search for
	 *@return the named ontology class for the given semantic type or null if
	 *         none exists.
	 */
	public NamedOntClass getNamedOntClass(SemanticType st) {
		String conceptId = st.getConceptId();
		String[] parts = conceptId.split("#");
		// make sure we have a valid semantic type
		if (parts.length < 2) {
			return null;
		}
		// search for a matching model and class
		for (Iterator iter = getNamedOntModels(); iter.hasNext();) {
			NamedOntModel m = (NamedOntModel) iter.next();
			if (m.getNameSpace().equals(parts[0])) {
				for (Iterator iter2 = m.getNamedClasses(); iter2.hasNext();) {
					NamedOntClass c = (NamedOntClass) iter2.next();
					if (c.getLocalName().equals(parts[1])) {
						return c;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Maps namespaces and local names to NamedOntClass objects
	 * 
	 *@param namespace
	 *            the namespace of the class
	 *@param localName
	 *            the local name (id) of the class within the namespace
	 *@return the named ontology class for the given semantic type or null if
	 *         none exists.
	 */
	public NamedOntClass getNamedOntClass(String namespace, String localName) {
		if (namespace == null || localName == null) {
			return null;
		}
		// search for a matching model and class
		for (Iterator iter = getNamedOntModels(); iter.hasNext();) {
			NamedOntModel m = (NamedOntModel) iter.next();
			String m_nspace = m.getNameSpace() + "#";
			if (m.getNameSpace().equals(namespace)
					|| m_nspace.equals(namespace)) {
				for (Iterator<NamedOntClass> iter2 = m.getNamedClasses(); iter2
						.hasNext();) {
					NamedOntClass c = iter2.next();
					if (c.getLocalName().equals(localName)) {
						return c;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns class name with the given concept id in the ontologies managed by
	 * the catalog.
	 * 
	 *@param conceptId
	 *            complete name of class (with namespace)
	 *@param libraryClassesOnly
	 *            if true, search only those classes in one of the library
	 *            ontologies
	 *@return The named ontology class for the given concept id (local name
	 *         plus namespace), or null if no such class exist
	 */
	public NamedOntClass getNamedOntClass(String conceptId,
			boolean libraryClassesOnly) {
		if (conceptId == null) {
			return null;
		}
		for (Iterator<NamedOntModel> ontos = getNamedOntModels(libraryClassesOnly); ontos
				.hasNext();) {
			NamedOntModel m = ontos.next();
			for (Iterator<NamedOntClass> classes = m.getNamedClasses(); classes
					.hasNext();) {
				NamedOntClass c = classes.next();
				if (conceptId.equals(c.getConceptId())) {
					return c;
				}
			}
		}
		return null;
	}

	/**
	 * Returns class name with the given concept id in the ontologies managed by
	 * the catalog.
	 * 
	 *@param conceptId
	 *            complete name of class (with namespace)
	 *@return The named ontology class for the given concept id (local name
	 *         plus namespace), or null if no such class exist
	 */
	public NamedOntClass getNamedOntClass(String conceptId) {
		return getNamedOntClass(conceptId, false);
	}

	private static OWLOntologyManager manager;

	public static OWLOntologyManager getManager() {
		return manager;
	}

	private static final Log log = LogFactory.getLog(OntologyCatalog.class);
	private static final boolean isDebugging = log.isDebugEnabled();

	public OWLClass getConceptWithLabel(String label) {
		OWLOntology defaultOnt = getDefaultOntology();
		if (defaultOnt == null || label == null) {
			return null;
		}
		Set<OWLClass> classes = defaultOnt.getReferencedClasses();
		for (OWLClass c : classes) {
			String thisLabel = c.getAnnotations(defaultOnt,
					OWLRDFVocabulary.RDFS_LABEL.getURI()).toArray(
					new OWLAnnotation[1])[0].getAnnotationValue().toString();
			if (label.equals(thisLabel)) {
				return c;
			}
		}
		return null;
	}
}
