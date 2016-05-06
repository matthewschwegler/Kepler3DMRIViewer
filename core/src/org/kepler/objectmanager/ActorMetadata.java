/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-06-16 11:21:25 -0700 (Mon, 16 Jun 2014) $' 
 * '$Revision: 32770 $'
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

package org.kepler.objectmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.moml.CompositeClassEntity;
import org.kepler.moml.NamedObjId;
import org.kepler.moml.PortAttribute;
import org.kepler.moml.PropertyAttribute;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.sms.SemanticType;
import org.kepler.util.DotKeplerManager;
import org.kepler.util.FileUtil;
import org.kepler.util.TransientStringAttribute;

import ptolemy.actor.Director;
import ptolemy.actor.IOPort;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.expr.Variable;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.ComponentRelation;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.Entity;
import ptolemy.kernel.InstantiableNamedObj;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.SingletonConfigurableAttribute;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.MoMLParser;
import ptolemy.moml.unit.UnitAttribute;
import ptolemy.util.StringUtilities;
import ptolemy.vergil.basic.KeplerDocumentationAttribute;

/**
 * This class parses and contains metadata for an actor. this creates an entity
 * of the form:
 * 
 * <pre>
 * &lt;entity name=&quot;Constant&quot;&gt;
 *     &lt;property name=&quot;documentation&quot; class=&quot;ptolemy.vergil.basic.KeplerDocumentationAttribute&quot;&gt;
 *     actor to provide constant input
 *     &lt;/property&gt;
 *     &lt;property name=&quot;entityId&quot; class=&quot;org.kepler.moml.NamedObjId&quot;
 *                     value=&quot;urn:lsid:lsid.ecoinformatics.org:actor:101:1&quot;/&gt;
 *     &lt;property name=&quot;class&quot; value=&quot;org.kepler.actor.TestActor&quot;
 *               class=&quot;ptolemy.kernel.util.StringAttribute&quot;&gt;
 *       &lt;property name=&quot;id&quot; value=&quot;urn:lsid:lsid.ecoinformatics.org:actor:1001:1&quot;
 *                 class=&quot;ptolemy.kernel.util.StringAttribute&quot;/&gt;
 *     &lt;/property&gt;
 *     &lt;property name=&quot;output&quot; class=&quot;org.kepler.moml.PortProperty&gt;
 *       &lt;property name=&quot;direction&quot; value=&quot;output&quot; class=&quot;ptolemy.kernel.util.StringAttribute&quot;/&gt;
 *       &lt;property name=&quot;dataType&quot; value=&quot;unknown&quot; class=&quot;ptolemy.kernel.util.StringAttribute&quot;/&gt;
 *       &lt;property name=&quot;isMultiport&quot; value=&quot;false&quot; class=&quot;ptolemy.kernel.util.StringAttribute&quot;/&gt;
 *     &lt;/property&gt;
 *     &lt;property name=&quot;trigger&quot; class=&quot;org.kepler.moml.PortProperty&gt;
 *       &lt;property name=&quot;direction&quot; value=&quot;input&quot; class=&quot;ptolemy.kernel.util.StringAttribute&quot;/&gt;
 *       &lt;property name=&quot;dataType&quot; value=&quot;unknown&quot; class=&quot;ptolemy.kernel.util.StringAttribute&quot;/&gt;
 *       &lt;property name=&quot;isMultiport&quot; value=&quot;true&quot; class=&quot;ptolemy.kernel.util.StringAttribute&quot;/&gt;
 *     &lt;/property&gt;
 *     &lt;property class=&quot;org.kepler.sms.SemanticType&quot; name=&quot;semanticType&quot; value=&quot;urn:lsid:lsid.ecoinformatics.org:onto:1:1#ConstantActor&quot;/&gt;
 *     &lt;property class=&quot;org.kepler.moml.Dependency&quot; name=&quot;dependency&quot; value=&quot;urn:lsid:lsid.ecoinformatics.org:nativeLib:1:1&quot;/&gt;
 *     &lt;/entity&gt;
 * </pre>
 * 
 * Note: As of 8/18/09, this class no longer includes graphical ammentities,
 * such as icons in the moml. If you want graphical widgets for your actor, you
 * must use org.kepler.gui.GraphicalActorMetadata.
 * 
 * @author Chad Berkley
 *@created April 07, 2005
 */
public class ActorMetadata implements Serializable {

	private static final Log log = LogFactory.getLog(ActorMetadata.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * The id of the class that implements this actor.
	 */
	private String _classId;

	/**
	 * The java class name. For composites, this is the name of the xml file
	 * like org.kepler.actor.MyCompositeActor
	 */
	private String _className;

	/**
	 * The class name as represented in ptII. For composites, this is always
	 * ptolemy.actor.TypedCompositeActor. For java classes, this is the name of
	 * the java class.
	 */
	private String _internalClassName;

	/**
	 * The id of the actor.
	 */
	private String _actorId;

	/**
	 * The name of the actor
	 */
	private String _actorName;

	/**
	 * The instantiation of the actor. TODO describe more about how/when this is
	 * populated, etc.
	 */
	private NamedObj _actor;
	
	/**
	 * True if the actor described by this ActorMetadata is a class description.
	 */
	private boolean _isClass;

	/**
	 * dependency ids
	 */
	private Vector<ClassedProperty> _dependencyVector;

	/**
	 * semantic types
	 */
	private Vector<ClassedProperty> _semanticTypeVector;

	/**
	 * ports
	 */
	private Vector<PortMetadata> _portVector;

	/**
	 * changed flag
	 */
	private boolean _changed = false;

	/**
	 * attribute vector for generic attributes
	 */
	private Vector<Attribute> _attributeVector;

	/**
	 * vector for relations in a composite
	 */
	private Vector<ComponentRelation> _relationVector;

	/**
	 * 
	 */
	private String _links = null;

	/**
	 * 
	 */
	private Vector<MetadataHandler> _metadataHandlerVector;
	
	/**
	 * default constructor. this is for serialization only. do not use this.
	 */
	public ActorMetadata() {
		initialize();
		// nothing here.
	}

	private void initialize() {
		_dependencyVector = new Vector<ClassedProperty>();
		_semanticTypeVector = new Vector<ClassedProperty>();
		_portVector = new Vector<PortMetadata>();
		_attributeVector = new Vector<Attribute>();
		_relationVector = new Vector<ComponentRelation>();
		_metadataHandlerVector = new Vector<MetadataHandler>();
		_isClass = false;
	}

	/**
	 * Constructor. Takes in xml metadata. This should be a moml entity with the
	 * kepler additional metadata properties. The entity is parsed and an
	 * ActorMetadata object is created with appropriate fields.
	 * 
	 * @param moml
	 *            the xml metadata
	 */
	public ActorMetadata(InputStream moml) throws InvalidMetadataException {
		if (isDebugging && moml != null)
			log.debug("ActorMetadata(" + moml.toString() + ")");

		initialize();

		try {
			String momlStr = FileUtil.convertStreamToString(moml);

			if (isDebugging) {
				// log.debug("\n********************************");
				// log.debug(momlStr);
				// log.debug("********************************\n");
				log.debug("**** MoMLParser ****");
			}

			/**
			 * The MoMLParser cannot be the first thing to be called on Kepler
			 * generated MoML files. Because TypedIOPorts are converted to
			 * PortAttributes, an error is thrown when trying to parse Kepler
			 * generated MoMLs that have any added ports. TODO: How to fix this?
			 */
			MoMLParser parser = new MoMLParser(new Workspace());
			parser.reset();

			if (isDebugging)
				log.debug("got moml parser outputing moml");

			NamedObj obj = null;
			try {
				obj = parser.parse(null, momlStr);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			if (obj == null)
				return;

			if (isDebugging) {
				String filename = "parsed-actor_" + obj.getName() + ".moml";
				writeDebugMomlFor(obj, filename);
			}

			if (obj instanceof TypedCompositeActor) {
				_links = ((TypedCompositeActor) obj).exportLinks(1, null);
			}
			_actorName = obj.getName();
			
			StringAttribute classAttribute = (StringAttribute) obj.getAttribute("class");
			
			if(classAttribute == null) {
				throw new InvalidMetadataException("Missing 'class' attribute for " +
						obj.getFullName());
			}
			
			Attribute idAtt = classAttribute.getAttribute("id");
			
			if(idAtt == null) {
				throw new InvalidMetadataException("Missing 'id' attribute for " +
						obj.getFullName());
			}
			
			_classId = ((StringAttribute) idAtt).getExpression();
			
			_className = classAttribute.getExpression();
			_internalClassName = obj.getClassName();
			
			Attribute actIdAtt = obj.getAttribute(NamedObjId.NAME);
			
			if(actIdAtt == null) {
				throw new InvalidMetadataException("Missing '" +
						NamedObjId.NAME + "' attribute for " + obj.getFullName());
			}
			
			_actorId = ((NamedObjId) actIdAtt).getExpression();
			
			NamedObj actor = getActorClass(_className, _actorName, obj);
			
			// Handle class definitions
			if (actor instanceof InstantiableNamedObj) {
				InstantiableNamedObj ino = (InstantiableNamedObj) actor;
				if (ino.isClassDefinition()) {
					_isClass = true;
					//_internalClassName = _className;
				}
			}
			this.setActor(actor);

			if (isDebugging) {
				String filename = "instantiated-actor-before_"
						+ getActor().getName() + ".moml";
				writeDebugMomlFor(obj, filename);
			}

			// get the semantic type and dependency lsids and any general
			// properties
			for (Object o : obj.attributeList()) {

				Attribute a = null;
				if (o instanceof Attribute) {
					a = (Attribute) o;
				} else {
					log.error("Object is not an Attribute");
					continue;
				}

				getSemanticTypesAndDependencyLsids(a);

			}

			// get the ports

			// NOTE: we parse obj instead of actor since actor does not have
			// the PortAttributes
			parseNamedObj(obj);
			addAllRelations();
			
			if (isDebugging) {
				String filename = "instantiated-actor-after_"
						+ getActor().getName() + ".moml";
				writeDebugMomlFor(obj, filename);
			}

		} catch (IOException ioe) {
			throw new InvalidMetadataException("Error reading data from lsid "
					+ _actorId + ": " + ioe.getMessage(), ioe);
		} catch (Exception e) {
			if (isDebugging)
				log.debug(e.getStackTrace());
			throw new InvalidMetadataException(
					"Error in parsing actor metadata: " + e.getMessage(), e);
		}
	}
	
	/**
	 * TODO: What is this doing?
	 * 
	 * @param a
	 * @return
	 */
	private boolean checkAtt(Attribute a) {
		String attName = a.getName();
		String acn = a.getClassName();
		if (!attName.equals(NamedObjId.NAME) && !attName.equals("class")
				&& !acn.equals("org.kepler.moml.PropertyEntity")
				&& !acn.equals("org.kepler.moml.PropertyAttribute")
				&& !acn.equals("org.kepler.moml.CompositeClassEntity")
				&& !acn.equals("org.kepler.moml.PortAttribute")) {
			return true;
		}
		return false;
	}

	/**
	 * TODO: What is this doing?
	 * 
	 * @param a
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 * @throws CloneNotSupportedException
	 */
	private void getSemanticTypesAndDependencyLsids(Attribute a)
			throws IllegalActionException, NameDuplicationException,
			CloneNotSupportedException {

		String attName = a.getName();
		String attClassName = a.getClassName();
		String aClassName = a.getClass().getName();
		String kd = "ptolemy.vergil.basic.KeplerDocumentationAttribute";

		if (attClassName.equals(kd)) {
			// parse the documentation

			setDocumentationAttribute((KeplerDocumentationAttribute) a);
		} else if (checkAtt(a)) {

			String attValue = null;
			if (a instanceof StringAttribute) {
				attValue = ((StringAttribute) a).getExpression();

			} else if (a instanceof UnitAttribute) {
				attValue = ((UnitAttribute) a).getExpression();

			} else if (a instanceof Variable) {
				attValue = ((Variable) a).getExpression();
			}

			if (attName.indexOf("semanticType") != -1) {
				addSemanticType(a.getName(), attValue);

			} else if (attName.indexOf("dependency") != -1) {
				ClassedProperty prop = new ClassedProperty(attName, attValue,
						aClassName);
				_dependencyVector.add(prop);

			} else if (attName.indexOf("_iconDescription") != -1) {
				NamedObj act = getActor();
				Attribute iconAtt = act.getAttribute("_iconDescription");

				if (act != null && iconAtt != null) {
					handleIconDescription(a);

				} else {
					addAttribute(a);
				}
			} else {
				// add generic attributes
				addAttribute(a);
			}
		}
	}

	/**
	 * TODO: What is this doing?
	 * 
	 * @param a
	 */
	private void handleIconDescription(Attribute a) {
		boolean objIsDefault = false;
		boolean actorIsDefault = false;

		String objIconStr = ((SingletonConfigurableAttribute) a)
				.getExpression();
		String actorIconStr = ((SingletonConfigurableAttribute) getActor()
				.getAttribute("_iconDescription")).getExpression();

		// Should this come from somewhere else?
		String pp = "<polygon" + " points=\"-20,-10 20,0 -20,10\""
				+ " style=\"fill:blue\"/>";

		if (objIconStr.indexOf(pp) != -1) {
			objIsDefault = true;
		}

		if (actorIconStr.indexOf(pp) != -1) {
			actorIsDefault = true;
		}

		if ((objIsDefault && actorIsDefault)
				|| (objIsDefault && actorIsDefault)
				|| (!objIsDefault && !actorIsDefault)) {
			addAttribute(a);
		} else if (objIsDefault && !actorIsDefault) {
			// do nothing. leave the attribute in the actor
		}
	}

	/**
	 * builds a new ActorMetadata object from an existing NamedObj
	 * 
	 * @param am
	 *            the ActorMetadata to build this object from.
	 */
	public ActorMetadata(NamedObj obj) {
		
		//System.out.println(obj.exportMoML());
		
		initialize();
		
		if (obj instanceof InstantiableNamedObj) {
			InstantiableNamedObj ino = (InstantiableNamedObj) obj;
			if (ino.isClassDefinition()) {
				_isClass = true;
			}
		}
		
		this.setActor(obj);

		if (obj.getAttribute(NamedObjId.NAME) != null) {
			_actorId = ((NamedObjId) obj.getAttribute(NamedObjId.NAME))
					.getExpression();
		}
		_actorName = obj.getName();

		if (_isClass) {
			
			Attribute classAtt = obj.getAttribute("tempClassName");
			if (classAtt != null) {
				TransientStringAttribute classAttribute = (TransientStringAttribute) classAtt;
				String className = classAttribute.getExpression();
				_className = className;
				_internalClassName = className;
			}
			
			_links = null;

			addAllAttributes();
			addAllRelations();
			removeLinks();

		} else {
			_className = obj.getClassName();
			_internalClassName = obj.getClassName();
			try {
				if (obj instanceof TypedCompositeActor) {
					_links = ((TypedCompositeActor) obj).exportLinks(1, null);
				}
			} catch (Exception e) {
				System.out.println("Error looking at links: " + e.getMessage());
			}
	
			// set the internalClassname
			if (obj instanceof TypedAtomicActor) {
				_internalClassName = "ptolemy.kernel.ComponentEntity";
			} else if (obj instanceof Attribute) {
				_internalClassName = "org.kepler.moml.PropertyEntity";
			} else if (obj instanceof TypedCompositeActor
					|| obj instanceof CompositeEntity) {
				_internalClassName = "org.kepler.moml.CompositeClassEntity";
			}

			addAllAttributes();
			addAllRelations();
		}
		parseActor();		
	}

	/**
	 * return the actor this object was built from.
	 */
	public NamedObj getActor() {
		return _actor;
	}

	public void setActor(NamedObj actor) {
		_actor = actor;
	}

	/**
	 * return the name of the actor
	 */
	public String getName() {
		return _actorName;
	}

	/**
	 * set the name
	 */
	public void setName(String name) {
		_actorName = name;
	}

	/**
	 * return the lsid of the actor class object
	 */
	public String getId() {
		return _actorId;
	}

	/**
	 * returns the id of this object as a KeplerLSID
	 */
	public KeplerLSID getLSID() throws Exception {
		return new KeplerLSID(_actorId);
	}

	/**
	 *
	 */
	public KeplerDocumentationAttribute getDocumentationAttribute() {
		KeplerDocumentationAttribute da = (KeplerDocumentationAttribute) getActor()
				.getAttribute("KeplerDocumentation");
		return da;
	}

	/**
	 *
	 */
	public void setDocumentationAttribute(KeplerDocumentationAttribute newda)
			throws IllegalActionException, NameDuplicationException,
			CloneNotSupportedException {
		
		NamedObj a = getActor();
		if (a == null) 
			return;
		
		Attribute da = a.getAttribute("documentation");

		// if the actor already has a da, then get rid of it and set this one
		// instead
		if (da != null)
			da.setContainer(null);

		da = a.getAttribute("KeplerDocumentation");
		if (da != null)
			da.setContainer(null);

		Workspace w = a.workspace();
		KeplerDocumentationAttribute n = (KeplerDocumentationAttribute) newda
				.clone(w);
		n.setContainer(a);
	}

	/**
	 * set the id
	 */
	public void setId(String id) {
		_actorId = id;
	}

	/**
	 * return the classname of the actor object. this will be the java type
	 * classname no matter what kind of actor we are describing. for instance if
	 * it is a java class, it will be a.b.c.ClassName where the file is stored
	 * in a/b/c/ClassName.class. If it is a MoML class, it will be
	 * a.b.c.MomlClassName where the file is a/b/c/MomlClassName.xml
	 */
	public String getClassName() {
		return _className;
	}

	/**
	 * return the internal class name of the actor object. for composites this
	 * will be ptolemy.actor.TypedCompositeActor. For atomics, it will be the
	 * full java class name.
	 */
	public String getInternalClassName() {
		return _internalClassName;
	}

	/**
	 * return the id of the class
	 */
	public String getClassId() {
		return _classId;
	}

	public void setClassId(String id) {
		_classId = id;
	}

	/**
	 * add the id of a dependency to the metadata
	 * 
	 * @param id
	 *            the id of the dependency to add
	 */
	public void addDependency(String id) {
		ClassedProperty cp = new ClassedProperty("dependency", id,
				"org.kepler.moml.Dependency");
		_dependencyVector.addElement(cp);
	}

	/**
	 * add the id of a semantic type to the metadata
	 * 
	 * @param id
	 *            the id of the semantic type to add
	 */
	public void addSemanticType(String name, String id) {
		ClassedProperty cp = new ClassedProperty(name, id,
				"org.kepler.sms.SemanticType");
		_semanticTypeVector.addElement(cp);
	}

	/**
	 * return a vector of the ids of the semantic type reference
	 */
	public Vector<String> getSemanticTypes() {
		Vector<String> v = new Vector<String>();
		for (int i = 0; i < _semanticTypeVector.size(); i++) {
			v
					.addElement(((ClassedProperty) _semanticTypeVector
							.elementAt(i)).value);
		}
		return v;
	}

	public Hashtable<String, String> getSemanticTypeHash() {
		Hashtable<String, String> h = new Hashtable<String, String>();
		for (int i = 0; i < _semanticTypeVector.size(); i++) {
			ClassedProperty cp = (ClassedProperty) _semanticTypeVector
					.elementAt(i);
			h.put(cp.name, cp.value);
		}
		return h;
	}

	public void removeSemanticType(String id) {
		for (int i = 0; i < _semanticTypeVector.size(); i++) {
			ClassedProperty cp = (ClassedProperty) _semanticTypeVector
					.elementAt(i);
			if (cp.name.equals(id)) {
				_semanticTypeVector.remove(i);
				break;
			}
		}
	}

	/**
	 * return a vector of the ids of the semantic type reference
	 */
	public Vector<String> getDependencies() {
		Vector<String> v = new Vector<String>();
		for (int i = 0; i < _dependencyVector.size(); i++) {
			v
					.addElement(((ClassedProperty) _dependencyVector
							.elementAt(i)).value);
		}
		return v;
	}

	/**
	 * get the changed flag. this is useful for keeping track of the state of
	 * the actor metadata. this flag does not affect this class in any internal
	 * way except for setting the flag.
	 */
	public boolean getChanged() {
		return _changed;
	}

	/**
	 * set the changed flag. this is useful for keeping track of the state of
	 * the actor metadata. this flag does not affect this class in any internal
	 * way except for setting the flag.
	 * 
	 * @param b
	 */
	public void setChanged(boolean b) {
		_changed = b;
	}

	/**
	 * add a generic attribute to this actorMetadata object.
	 * 
	 * @param a
	 *            the attribute to add
	 */
	public void addAttribute(Attribute a) {
		// why did we intentionally skip PortParameters?
		// if
		// (a.getClassName().equals("ptolemy.actor.parameters.PortParameter")) {
		// return;
		// }
		_attributeVector.add(a);
	}

	/**
	 * add a relation
	 */
	public void addRelation(ComponentRelation r) {
		_relationVector.add(r);
	}

	/**
	 * return this actor as a ComponentEntity
	 * 
	 * @param container
	 *            the new ComponentEntity's container
	 */
	public NamedObj getActorAsNamedObj(CompositeEntity container)
			throws Exception {
		
		NamedObj obj = null;

		// Throw a NullPointerException here with a good message.
		// If actor is null, clone() will fail anyway.
		if (getActor() == null) {
			throw new NullPointerException(
					"Could not clone '"
							+ _actorName
							+ "' from the '"
							+ _className
							+ "' class; the object is null, possibly meaning it was not "
							+ "found. Perhaps there is a classpath problem and/or the "
							+ "karlib needs to be flushed?");

		} else if (getActor() instanceof TypedCompositeActor) {
			
			/*
			 * if we are dealing with a composite entity, do stuff a bit
			 * differently we need to instantiate the composite or else it will
			 * show up in the library as a class instead of an entity. this
			 * causes kepler to think that the user wants to drag a class to the
			 * canvas and it requires the user to instantiate the actor before
			 * using it. by calling instantiate here, we bypass that.
			 */
			if (_internalClassName
					.equals("org.kepler.moml.CompositeClassEntity")) {
				obj = getActor();
				obj = addPorts(obj, _portVector);

				if (container == null) {
					try {
						NamedObj newobj = (NamedObj) getActor().clone(
								new Workspace());
						// this kinda works
						return newobj;
					} catch (java.lang.CloneNotSupportedException cnse) {
						System.out
								.println("trying to clone "
										+ getActor().getName()
										+ " but "
										+ "you can't clone this object for some reason: "
										+ cnse.getMessage());
					}
				} else {
					return (NamedObj) getActor().clone(container.workspace());
					// this kinda works
				}
				// return
				// (NamedObj)((TypedCompositeActor)actor).instantiate(container,
				// actor.getName());
				// obj =
				// (NamedObj)((TypedCompositeActor)actor).instantiate(container,
				// actor.getName());
				// obj.setClassName(className);
				// return obj;
			} else {
			    
			    // see if the internal class name is a subclass of composite actor
	            Class<?> clazz = Class.forName(_internalClassName);
	            Class<?> compositeActorClass = Class.forName("ptolemy.actor.TypedCompositeActor");
	            if(compositeActorClass.isAssignableFrom(clazz)) {
	                // clone it
	                if(container == null) { 
	                    return (NamedObj) getActor().clone(new Workspace());
	                } else {
	                    return (NamedObj) getActor().clone(container.workspace());
	                }
	            } else {
    				obj = (NamedObj) ((TypedCompositeActor) getActor())
    						.instantiate(container, getActor().getName());
    				obj.setClassName(_className);
	            }
			}

		} else if (getActor() instanceof Director
				|| getActor() instanceof Attribute) {
			// this is a director or other Attribute derived class

			// obj = new Director(container, actorName);
			// obj.setClassName(className);
			if (container == null) {
				obj = (NamedObj) getActor().clone(new Workspace());
			} else {
				obj = (NamedObj) getActor().clone(container.workspace());
			}

		} else {
			// this is an atomic actor
			if (container != null) {
				obj = (NamedObj) getActor().clone(container.workspace());
				((TypedAtomicActor) obj).setContainer(container);
			} else {
				obj = (NamedObj) getActor().clone(null);
			}
		}

		// call the metadata handlers
		for (int i = 0; i < _metadataHandlerVector.size(); i++) {
			MetadataHandler handler = (MetadataHandler) _metadataHandlerVector
					.elementAt(i);
			handler.handleMetadata(obj);
		}

		NamedObjId objId;
		StringAttribute classObj;
		StringAttribute classIdObj;

		try {
			objId = new NamedObjId(obj, NamedObjId.NAME);
		} catch (ptolemy.kernel.util.IllegalActionException iee) {
			objId = (NamedObjId) obj.getAttribute(NamedObjId.NAME);
		} catch (ptolemy.kernel.util.NameDuplicationException nde) {
			objId = (NamedObjId) obj.getAttribute(NamedObjId.NAME);
		}

		try {
			classObj = new StringAttribute(obj, "class");
			classIdObj = new StringAttribute(classObj, "id");
		} catch (ptolemy.kernel.util.InternalErrorException iee) {
			classObj = (StringAttribute) obj.getAttribute("class");
			classIdObj = (StringAttribute) classObj.getAttribute("id");
		} catch (ptolemy.kernel.util.NameDuplicationException nde) {
			classObj = (StringAttribute) obj.getAttribute("class");
			classIdObj = (StringAttribute) classObj.getAttribute("id");
		}

		if (objId != null) {
			objId.setExpression(_actorId);
		}

		classObj.setExpression(_className);
		classIdObj.setExpression(_classId);
		for (int i = 0; i < _semanticTypeVector.size(); i++) {
			ClassedProperty cp = (ClassedProperty) _semanticTypeVector
					.elementAt(i);
			Attribute attribute = obj.getAttribute(cp.name);
			if (attribute == null) {
				SemanticType semType = new SemanticType(obj, cp.name);
				semType.setExpression(cp.value);
			}
			else if (!(attribute instanceof SemanticType)) {
				log.warn("Attribute is not a SemanticType as expected");
			}
		}
		/*
		 * FIXME: TODO: add dependencies and other info to the NamedObj
		 */

		// add the general attributes to the object
		for (int i = 0; i < _attributeVector.size(); i++) {
			Attribute a = (Attribute) _attributeVector.elementAt(i);
			Attribute aClone = (Attribute) a.clone(obj.workspace());
			try {
				aClone.setContainer(obj);
			} catch (NameDuplicationException nde) {
				// System.out.println("obj already has attribute " +
				// a.getName());
				// ignore this, it shouldn't matter.
				// Specialized versions of some actors (e.g. RExpression actor
				// require that parameters be reset to new values
				// without the following code, these will not be reset
				// Dan Higgins - 1/20/2006
				String attValue;
				String attName;
				attName = aClone.getName();
				if (aClone instanceof StringAttribute) {
					attValue = ((StringAttribute) aClone).getExpression();
					StringAttribute sa = (StringAttribute) obj
							.getAttribute(attName);
					sa.setExpression(attValue);
				} else if (aClone instanceof StringParameter) {
					attValue = ((StringParameter) aClone).getExpression();
					StringParameter sp = (StringParameter) obj
							.getAttribute(attName);
					sp.setExpression(attValue);
				} else if (aClone instanceof Parameter) {
					attValue = ((Parameter) aClone).getExpression();
					Parameter pp = (Parameter) obj.getAttribute(attName);
					pp.setExpression(attValue);
				}

			}
		}

		// copy any extra moml ports over
		if (!(getActor() instanceof Director)) {
			obj = addPorts(obj, _portVector);
		}

		return obj;
	}

	/**
	 * return the moml xml representation of the actor.
	 */
	public String toString() {
        if (isDebugging) {
            log.debug("toString()");
        }
		return toString(true, true, true);
	}
	
	/**
	 * return the moml xml representation of the actor.
	 * @param incSemanticTypeNames if true, add a suffix to property names
	 * for semantic types, e.g. semanticType00 -> semanticType000 
	 * @param includeAttributes if true, add the attributes and parameters
	 * to the xml representation. this flag does not affect semantic types
	 * or documentation attributes.
	 * @param includePorts if true, add the ports to the xml representation.
	 */
	public String toString(boolean addSuffixToSemanticTypeNames,
	        boolean includeAttributes, boolean includePorts) {

		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");

		sb.append("<!DOCTYPE entity PUBLIC "
				+ "\"-//UC Berkeley//DTD MoML 1//EN\"\n"
				+ "    \"http://ptolemy.eecs.berkeley.edu"
				+ "/xml/dtd/MoML_1.dtd\">\n");
		if (getActor() instanceof PropertyAttribute) {

			sb.append("<property name=\"" + _actorName
					+ "\" class=\"org.kepler.moml.PropertyAttribute\">\n");

		} else if (getActor() instanceof Attribute) {

			sb.append("<property name=\"" + _actorName
					+ "\" class=\"org.kepler.moml.PropertyEntity\">\n");

		} else if (_internalClassName
				.equals("org.kepler.moml.CompositeClassEntity")) {

			sb.append("<entity name=\"" + _actorName
					+ "\" class=\"org.kepler.moml.CompositeClassEntity\">\n");

		} else {

			if (_isClass) {
				//System.out.println("isClass: " + _internalClassName);
				sb.append("<entity name=\"" + _actorName
						+ "\" class=\"" + _internalClassName + "\">\n");
			} else {
				sb.append("<entity name=\"" + _actorName
						+ "\" class=\"ptolemy.kernel.ComponentEntity\">\n");
			}

		}

		sb.append("<property name=\"" + NamedObjId.NAME + "\"  value=\""
				+ _actorId + "\" class=\"org.kepler.moml.NamedObjId\"/>\n");

		if (_internalClassName.equals("ptolemy.actor.Director")
				|| _internalClassName
						.equals("ptolemy.actor.TypedCompositeActor")
				|| _internalClassName.equals("ptolemy.kernel.ComponentEntity")
				|| _internalClassName.equals("org.kepler.moml.PropertyEntity")
				|| _internalClassName
						.equals("org.kepler.moml.CompositeClassEntity")) {
			sb.append("<property name=\"class\" value=\"" + _className
					+ "\" class=\"ptolemy.kernel.util.StringAttribute\">\n");
		} else {
			sb.append("<property name=\"class\" value=\"" + _internalClassName
					+ "\" class=\"ptolemy.kernel.util.StringAttribute\">\n");
		}

		// add another property to tell what the moml class is
		if (_internalClassName.equals("ptolemy.actor.TypedCompositeActor")) {
			sb.append("  <property name=\"momlClass\" value=\"" + _className
					+ "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
		}
		sb.append("  <property name=\"id\" value=\"" + _classId
				+ "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
		sb.append("</property>\n");

		// print the dependencies
		if (_dependencyVector.size() != 0) {
			for (int i = 0; i < _dependencyVector.size(); i++) {
				ClassedProperty cp = (ClassedProperty) _dependencyVector.get(i);
				cp.addNameIterator(i);
				sb.append(cp.toString() + "\n");
			}
		}

		// print the semantic types
		if (_semanticTypeVector.size() != 0) {
			for (int i = 0; i < _semanticTypeVector.size(); i++) {
				ClassedProperty cp = (ClassedProperty) _semanticTypeVector
						.get(i);
				if(addSuffixToSemanticTypeNames) {
					cp.addNameIterator(i);
				}
				sb.append(cp.toString() + "\n");
			}
		}

		// add general attributes
		if (_attributeVector.size() != 0) {
			for (int i = 0; i < _attributeVector.size(); i++) {
				Attribute a = (Attribute) _attributeVector.elementAt(i);
				if(includeAttributes || (a instanceof SemanticType)) {
				    sb.append(a.exportMoML() + "\n");
				}
			}
		}

		// print the ports
		// NOTE: the ports must come after attributes since ParameterPorts
		// must come after the PortParameters. otherwise parsing the xml
		// results in a NameDuplicatonException.
		// see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=4580

		if (includePorts && _portVector.size() != 0) {
			for (int i = 0; i < _portVector.size(); i++) {
				if (getActor() instanceof TypedCompositeActor
						&& getActor() instanceof InstantiableNamedObj
						&& !((InstantiableNamedObj) getActor())
								.isClassDefinition()) {
					sb.append(((PortMetadata) _portVector.get(i))
							.toMoMLString());
				} else {
					sb.append(((PortMetadata) _portVector.get(i)).toString());
				}
			}
		}

		// add relations
		if (_relationVector.size() != 0) {
			for (int i = 0; i < _relationVector.size(); i++) {
				ComponentRelation r = (ComponentRelation) _relationVector
						.elementAt(i);
				sb.append(r.exportMoML() + "\n");
			}

		}

		// add documentation
		KeplerDocumentationAttribute da = getDocumentationAttribute();
		if (da != null) {
			sb.append(da.exportMoML());
		}

		String cce = "org.kepler.moml.CompositeClassEntity";
		if (_internalClassName.equals(cce)) {
			CompositeEntity ce = (CompositeEntity) getActor();

			// add any class definitions 
			// NOTE: these must be added before the entities since the
			// MoMLParser needs to read them first.
            List<?> classDefinitions = ce.classDefinitionList();
            for(Object obj : classDefinitions) {
                String classDefinitionMoml = ((NamedObj)obj).exportMoML();
                sb.append(classDefinitionMoml);
            }

			List<?> entList = ce.entityList();
			Iterator<?> entityItt = entList.iterator();

			while (entityItt.hasNext()) {
				Entity ent = (Entity) entityItt.next();
				String entMoml = ent.exportMoML();
				sb.append(entMoml);
			}
			
		}

		if (_links != null) {
			sb.append(_links);
		}

		if (getActor() instanceof Attribute
				|| getActor() instanceof PropertyAttribute) {
			sb.append("</property>");
		} else {
			sb.append("</entity>\n");
		}

		return sb.toString();
	}

	/**
	 * this method allows other classes to add handlers to further process the
	 * metadata produced by this class. The handlers will be called in the
	 * getActorAsNamedObj() and getActorClass() methods.
	 * 
	 * @param handler
	 *            the handler to add
	 */
	public void addMetadataHandler(MetadataHandler handler) {
		_metadataHandlerVector.add(handler);
	}

	/**
	 * remove a metadata handler
	 * 
	 * @param handler
	 *            the handler to remove
	 */
	public void removeMetadataHandler(MetadataHandler handler) {
		_metadataHandlerVector.remove(handler);
	}

	private boolean addPort(TypedIOPort p, PortMetadata pm)
			throws IllegalActionException {
		if (isDebugging)
			log.debug("*************p: " + p.exportMoML());
		boolean found = false;

		String pName = p.getName();
		String cpName = cleansePortName(pm.name);
		if (pName.equals(cpName)) {

			if (isDebugging)
				log.debug("adding port " + pm.name);

			Vector<NamedObj> pAtts = pm.attributes;
			for (int j = 0; j < pAtts.size(); j++) {
				try {
					if (isDebugging)
						log.debug("cloning port " + pm.name);

					NamedObj noAtt = pAtts.elementAt(j);

					Attribute pm_att = (Attribute) noAtt;
					if (isDebugging)
						log.debug("processing attribute: " + pm_att.getName());
					if (pm_att instanceof SemanticType) {
						((Attribute) ((Attribute) pAtts.elementAt(j)).clone(p
								.workspace())).setContainer(p);
						// System.out.println("added attribute " +
						// ((Attribute)pm.attributes.elementAt(j)).getName()
						// + " to port " + p.getName());
					}
					// System.out.println("cloned port " + pm.name);
					// System.out.println("<port> is now: " +
					// p.exportMoML());
				} catch (NameDuplicationException nde) {
					// System.out.println("tried to duplicate " +
					// pm.attributes.elementAt(j).toString());
				} catch (java.lang.CloneNotSupportedException cnse) {
				}
			}
			found = true;
		}
		return found;
	}

	/**
	 * adds any kepler ports to the actor
	 */
	private NamedObj addPorts(NamedObj obj, Vector<PortMetadata> portVector)
			throws IllegalActionException, NameDuplicationException {
		boolean found = false;

		for (int i = 0; i < portVector.size(); i++) {

			// if there are any ports in the port vector that are not in the
			// port iterator, add them

			Entity ent = (Entity) obj;
			List<?> pList = ent.portList();
			Iterator<?> portIterator = pList.iterator();
			PortMetadata pm = (PortMetadata) portVector.elementAt(i);
			Vector<NamedObj> pmAtts = pm.attributes;

			if (isDebugging)
				log.debug("**********pm: " + pm.toString());

			while (portIterator.hasNext()) {

				TypedIOPort p = (TypedIOPort) portIterator.next();
				found = addPort(p, pm);
				if (found)
					break;

			}

			if (!found) {
				TypedIOPort port = null;

				if (obj instanceof TypedAtomicActor) {
					TypedAtomicActor taa = (TypedAtomicActor) obj;
					List<?> taaPL = taa.portList();
					Iterator<?> portList = taaPL.iterator();

					boolean flag = true;
					while (portList.hasNext()) {

						TypedIOPort oldport = (TypedIOPort) portList.next();
						String oldportName = oldport.getName();

						if (oldportName.equals(pm.name))
							flag = false;

						List<?> opAttList = oldport.attributeList();

						if (isDebugging) {
							log.debug("old port atts: " + opAttList.size());
							log.debug("pm att size: " + pmAtts.size());
						}

						if (opAttList.size() != pmAtts.size())
							flag = true;
					}

					if (flag) {

						if (isDebugging)
							log.debug("adding port " + pm.name + " to obj");

						String cpName = cleansePortName(pm.name);
						port = new TypedIOPort(taa, cpName);

					} else {

						if (isDebugging) {
							log.debug("not adding port " + pm.name + " to obj");
						}

					}
				} else {

					TypedCompositeActor tca = (TypedCompositeActor) obj;
					List<?> tcaPList = tca.portList();
					Iterator<?> portList = tcaPList.iterator();

					boolean flag = true;
					while (portList.hasNext()) {

						TypedIOPort oldport = (TypedIOPort) portList.next();
						String oldportName = oldport.getName();

						if (oldportName.equals(pm.name))
							flag = false;
					}

					if (flag) {
						String cpName = cleansePortName(pm.name);
						port = new TypedIOPort(tca, cpName);
					}
				}

				if (port == null) {
					continue;
				}

				if (pm.direction.equals("input")) {
					port.setInput(true);
				} else if (pm.direction.equals("output")) {
					port.setOutput(true);
				} else if (pm.direction.equals("inputoutput")) {
					port.setInput(true);
					port.setOutput(true);
				}

				if (pm.multiport) {
					port.setMultiport(true);
				}

				Iterator<?> attItt = pmAtts.iterator();
				while (attItt.hasNext()) {

					Attribute a = (Attribute) attItt.next();
					try {
						Workspace pws = port.workspace();
						Attribute attClone = (Attribute) a.clone(pws);
						attClone.setContainer(port);

					} catch (CloneNotSupportedException cnse) {
						System.out.println("Cloning the attribute "
								+ a.getName() + " is not supported: "
								+ cnse.getMessage());
					}
				}
			} else {
				found = false;
			}
		}

		return obj;
	}

	/**
	 * removes the 'kepler:' from the port names. this tag is used to keep ptii
	 * from throwing name duplication exceptions becuase our port metadata
	 * Parameter has the same name as the port it describes
	 */
	private String cleansePortName(String portName) {
		if (portName.indexOf("kepler:") != -1) {
			// strip the kepler: off of the name. kepler: is there to
			// prevent namedup exceptions in the moml parser
			return portName.substring(7, portName.length());
		}
		return portName;
	}

	/**
	 * parse an actor to create the actorMetadata object
	 * 
	 *@param actor
	 */
	private void parseActor() {
		NamedObj actor = getActor();
		if (actor instanceof ComponentEntity /*
											 * && !(actor instanceof
											 * TypedCompositeActor)
											 */) {
			List<?> list = ((ComponentEntity) actor).portList();
			Iterator<?> portIterator = list.iterator();
			while (portIterator.hasNext()) {
				Port p = (Port) portIterator.next();
				PortMetadata pm = new PortMetadata();

				pm.name = p.getName();
				
				if(p instanceof TypedIOPort)
				{
				    pm.type = ((TypedIOPort)p).getType().toString();
				}
				
				if(p instanceof IOPort)
				{
				    IOPort iop = (IOPort)p;
				    
				    pm.multiport = iop.isMultiport();

    				if (iop.isInput() && iop.isOutput()) {
    					pm.direction = "inputoutput";
    
    				} else if (iop.isInput()) {
    					pm.direction = "input";
    
    				} else if (iop.isOutput()) {
    					pm.direction = "output";
    				}
				}

				pm.portClassName = p.getClassName();

				// get any other port attributes and copy them
				List<?> portAttList = p.attributeList();
				Iterator<?> portAttsIt = portAttList.iterator();

				while (portAttsIt.hasNext()) {

					NamedObj att = (NamedObj) portAttsIt.next();
					pm.addAttribute(att);

					if (isDebugging)
						log.debug("adding att: " + att.exportMoML());
				}

				_portVector.addElement(pm);
			}
		}
	}

	/**
	 * parse a named obj and get it's port info
	 */
	private void parseNamedObj(NamedObj obj) throws InvalidMetadataException {

		if (obj instanceof CompositeClassEntity) {

			// if it's a composite, look for the <port> objects instead of the
			// PortProperties
			Entity ent = (Entity) obj;
			List<?> pList = ent.portList();
			Iterator<?> portIterator = pList.iterator();

			while (portIterator.hasNext()) {

				TypedIOPort port = (TypedIOPort) portIterator.next();
				PortMetadata pm = new PortMetadata();

				pm.name = port.getName();
				pm.type = port.getType().toString();
				pm.multiport = port.isMultiport();

				if (port.isInput()) {
					pm.direction = "input";
				}

				if (port.isOutput()) {
					pm.direction = "output";
				}

				if (port.isOutput() && port.isInput()) {
					pm.direction = "inputoutput";
				}

				pm.portClassName = port.getClassName();

				List<?> attList = port.attributeList();
				Iterator<?> listIt = attList.iterator();

				while (listIt.hasNext()) {
					NamedObj att = (NamedObj) listIt.next();
					pm.addAttribute(att);
				}

				_portVector.addElement(pm);

			}

		} else if (obj instanceof ComponentEntity) {

			List<?> list = null;
			try {

				Class<?> cls = Class.forName("org.kepler.moml.PortAttribute");
				ComponentEntity ce = (ComponentEntity) obj;
				list = ce.attributeList(cls);

			} catch (ClassNotFoundException cnfe) {
			}

			Iterator<?> portIterator = list.iterator();

			while (portIterator.hasNext()) {
				PortAttribute pa = (PortAttribute) portIterator.next();
				PortMetadata pm = new PortMetadata();

				pm.name = pa.getName();
				if(pm.name == null) {
					throw new InvalidMetadataException("Port is missing name.");
				}
				
				StringAttribute attribute = (StringAttribute) pa.getAttribute("dataType");
				if(attribute == null) {
					throw new InvalidMetadataException("Port " + pm.name + " is missing dataType attribute.");
				}
				pm.type = attribute.getExpression();
				
				attribute = (StringAttribute) pa.getAttribute("isMultiport");
				if(attribute == null) {
					throw new InvalidMetadataException("Port " + pm.name + " is missing isMultiport attribute.");
				}
				
				// try parsing the boolean string
				try {
					pm.multiport = Boolean.valueOf(attribute.getExpression()).booleanValue();
				} catch(Throwable t) {
					throw new InvalidMetadataException("Invalid isMultiport value for port "
							+ pm.name + ": " + attribute.getExpression());
				}
				
				attribute = (StringAttribute) pa.getAttribute("direction");
				if(attribute == null) {
					throw new InvalidMetadataException("Port " + pm.name + " is missing direction attribute.");
				}
				pm.direction = attribute.getExpression();

				List<?> attList = pa.attributeList();
				Iterator<?> listIt = attList.iterator();

				while (listIt.hasNext()) {
					NamedObj att = (NamedObj) listIt.next();

					if (!att.getName().equals("dataType")
							&& !att.getName().equals("isMultiport")
							&& !att.getName().equals("direction")) {

						if (isDebugging)
							log.debug("adding att: " + att.exportMoML());
						pm.addAttribute(att);
					}
				}

				_portVector.addElement(pm);
			}
		}
	}

	/**
	 * look for a moml class and try to instantiate it
	 * 
	 * @param actorName
	 * @param className
	 * @param actorMetadataMoml
	 * @return
	 */
	private NamedObj lookForMoml(String actorName, String className,
			NamedObj actorMetadataMoml) throws Exception {
		if (isDebugging) log.debug("lookForMoml");

		if (_internalClassName.equals("org.kepler.moml.CompositeClassEntity")) {
			// This is a total hack, but I cannot get the
			// CompositeActors to clone correctly, so this try statement
			// basically clones them manually. It works so i'm leaving
			// it for now.
			try {
				StringBuffer sb = new StringBuffer();
				// sb.append("<class name=\"" + actorName +
				// "\" extends=\"" + className + "\">");
				sb.append("<entity name=\"" + actorName + "\" class=\""
						+ className + "\">");

				// get attributes and clone them over to the new obj
				CompositeClassEntity cce = (CompositeClassEntity) actorMetadataMoml;
				Iterator<?> attItt = cce.attributeList().iterator();

				while (attItt.hasNext()) {
					Attribute a = (Attribute) attItt.next();
					sb.append(a.exportMoML());
				}

				// get entities and clone them over to the new obj

				Iterator<?> entItt = cce.entityList().iterator();
				while (entItt.hasNext()) {
					ComponentEntity ent = (ComponentEntity) entItt.next();
					sb.append(ent.exportMoML());
				}

				// get ports and clone them over to the new obj
				Iterator<?> portItt = cce.portList().iterator();
				while (portItt.hasNext()) {
					Port p = (Port) portItt.next();
					sb.append(p.exportMoML());
				}

				// get relations and clone them over to the new obj
				TypedCompositeActor tca = (TypedCompositeActor) actorMetadataMoml;
				Iterator<?> relationItt = tca.relationList().iterator();
				while (relationItt.hasNext()) {
					ComponentRelation r = (ComponentRelation) relationItt
							.next();
					sb.append(r.exportMoML());
				}

				sb.append(_links);
				// sb.append("</class>");
				sb.append("</entity>");

				Workspace w = actorMetadataMoml.workspace();

				if (isDebugging)
					log.debug("**** MoMLParser ****");

				MoMLParser parser = new MoMLParser(w);
				parser.reset();
				NamedObj obj = parser.parse(sb.toString());
				return obj;
			} catch (Exception exc) {
				System.out.println("error creating compositeClassEntity: "
						+ exc.getMessage());
				exc.printStackTrace();
				throw exc;
			}
		} else {
		    
		    // see if the internal class name is a subclass of composite actor
		    Class<?> clazz = Class.forName(_internalClassName);
		    Class<?> compositeActorClass = Class.forName("ptolemy.actor.TypedCompositeActor");
		    if(compositeActorClass.isAssignableFrom(clazz)) {
		        
		        // parse the moml in the actor metadata
                MoMLParser parser = new MoMLParser(actorMetadataMoml.workspace());
                parser.reset();
                NamedObj obj = parser.parse(actorMetadataMoml.exportMoML());
                return obj;		        
		        
		    } else {
    			// ptolemy.kernel.ComponentEntity
    			ComponentEntity actor = parseMoMLFile(className);
    			if (isDebugging) {
    				log.debug("returning " + actor.getName());
    			}
    			return (TypedCompositeActor) actor;
		    }
		}

	}

	/**
	 * get the actor class and instantiate it to a NamedObj and return it
	 */
	private NamedObj getActorClass(String className, String actorName,
			NamedObj actorMetadataMoml) {

		if (isDebugging && actorMetadataMoml != null) {
			log.debug("getActorClass(" + className + "," + actorName + ","
					+ actorMetadataMoml.getName() + ")");
		}

		// try to get the actor class and instantiate it
		try {
			
			TypedAtomicActor actor;
			Class<?> actorClass = Class.forName(className);
			CompositeEntity ce = new CompositeEntity();
			Object[] args = { ce, actorName };
			actor = (TypedAtomicActor) createInstance(actorClass, args);

			// call the metadata handlers
			for (int i = 0; i < _metadataHandlerVector.size(); i++) {

				Object o = _metadataHandlerVector.elementAt(i);
				MetadataHandler handler = (MetadataHandler) o;
				handler.handleMetadata(actor);
			}

			return actor;
			
		} catch (Exception e) {
			if (isDebugging) {
				log.debug(e.toString());
				log.debug(_internalClassName);
			}
			
			NamedObj secTry;
			try {
				
				secTry = lookForMoml(actorName, className, actorMetadataMoml);
				return secTry;

			} catch (Exception ee) {
				if (isDebugging)
					log.debug(ee.toString());
				
				NamedObj thirdTry;
				try {

					thirdTry = handleNonEntityDirectors(actorName, className, 
							actorMetadataMoml);
					return thirdTry;

				} catch (Exception eee) {
					
					if (isDebugging)
						log.debug(eee.toString());

					NamedObj fourthTry;
					try {

						fourthTry = handleMiscAtt(actorName, className, 
								actorMetadataMoml);
						return fourthTry;

					} catch (Exception eeee) {
						
						printFinalException(eeee,className);
						return null;
					}
				}
			}
		}
	}
	
	/**
	 * Helper method.
	 * @param eeee
	 * @param className
	 */
	private void printFinalException(Exception eeee,String className) {
		System.out.println("The class name you entered was either not "
				+ "found in classpath or could not be instantiated:");
		System.out.println(className);
		System.out.println("Note that this class must be in the "
				+ "classpath from which you launched this program.");
		String msg = eeee.getMessage();
		if (msg != null) {
			System.out.println(msg);
		}

	}

	/**
	 * Todo: What is this doing?
	 * @param className
	 * @param actorName
	 * @param actorMetadataMoml
	 * @return
	 * @throws Exception
	 */
	private NamedObj handleMiscAtt(String actorName, String className, 
			NamedObj actorMetadataMoml) throws Exception {

		Attribute att;
		Class<?> attClass = Class.forName(className);
		Object[] args = { new CompositeEntity(), actorName };
		att = (Attribute) createInstance(attClass, args);
		return att;
	}

	/**
	 * This handles directors which are not entities, but attributes
	 * 
	 * @param className
	 * @param actorName
	 * @param actorMetadataMoml
	 * @return
	 * @throws Exception
	 */
	private NamedObj handleNonEntityDirectors(String actorName,
			String className, NamedObj actorMetadataMoml) throws Exception {

		Director director;
		Class<?> directorClass = Class.forName(className);
		Object[] args = { new CompositeEntity(), actorName };
		director = (Director) createInstance(directorClass, args);
		return director;
	}

	/**
	 * add all of the attributes with the exception of the kepler attributes
	 * (which should already be in the metadata) to the AM object.
	 * 
	 * @param obj
	 *            the NamedObj to get the attributes from.
	 */
	private void addAllAttributes() {
		NamedObj obj = getActor();
		if (obj != null && obj.attributeList() != null) {

			List<?> attList = obj.attributeList();
			Iterator<?> i = attList.iterator();

			while (i.hasNext()) {
				Attribute a = (Attribute) i.next();
				String name = a.getName();
				if (!name.equals(NamedObjId.NAME) && !name.equals("class")
						&& !name.equals("KeplerDocumentation")
						&& name.indexOf("dependency") == -1) {
					addAttribute(a);
				}

			}
		}
	}
	
	private void removeLinks() {
		NamedObj obj = getActor();
		if (obj == null) return;
	}

	/**
	 * add all of the relations
	 * 
	 * @param obj
	 *            the NamedObj to get the attributes from.
	 */
	private void addAllRelations() {
		NamedObj obj = getActor();
		if (obj == null) return;
		
		if (obj instanceof TypedCompositeActor) {
			TypedCompositeActor tca = (TypedCompositeActor) obj;
			List<?> relList = tca.relationList();
			if (relList != null) {
			
				Iterator<?> i = relList.iterator();
				while (i.hasNext()) {
					ComponentRelation r = (ComponentRelation) i.next();
					addRelation(r);
				}
			}
		}
	}

	/**
	 * search the classpath for a specific class and return the file. In the
	 * tradition of ptolemy, this will also search for moml files with a class
	 * definition. this is required for composite actors.
	 * 
	 * @param className
	 *            the name of the class to search for
	 * @param workDir
	 *            the directory where temp files can be created
	 */
	protected File searchClasspath(String className)
			throws FileNotFoundException {
		
		// separate the class name from the package path
		String actualClassName = className.substring(
				className.lastIndexOf(".") + 1, className.length());
		String packagePath = className.substring(0, className.lastIndexOf("."));
		String[] packagePathStruct = packagePath.split("\\.");

		// get the classpath so we can search for the class files
		String classpath = System.getProperty("java.class.path");
		String sep = System.getProperty("path.separator");
		StringTokenizer st = new StringTokenizer(classpath, sep);
		
		while (st.hasMoreTokens()) {
			String path = st.nextToken();
			File pathDir = new File(path);

			if (pathDir.isDirectory()) {
				// search the directory for the file

				if (matchDirectoryPath(pathDir, packagePathStruct, 0)) {
					// now we found a candidate...see if the class is in there
					File classDir = new File(pathDir.getAbsolutePath()
							+ File.separator
							+ packagePath.replace('.', File.separator
									.toCharArray()[0]));
					File[] classDirFiles = classDir.listFiles();
					for (int i = 0; i < classDirFiles.length; i++) {

						String dirFileName = classDirFiles[i].getName();

						if (dirFileName.indexOf(".") != -1) {
							String extension = dirFileName.substring(
									dirFileName.lastIndexOf("."), dirFileName
											.length());
							String prefix = dirFileName.substring(0,
									dirFileName.lastIndexOf("."));
							if (actualClassName.equals(prefix)
									&& (extension.equals(".class") || extension
											.equals(".xml"))) {
								// search for xml or class files
								return classDirFiles[i];
							}
						}
					}
				}
			} else if (pathDir.isFile()) {
				// search a jar file for the file. if it's not a jar file,
				// ignore it
				try {
					String entryName = className.replace('.', '/') + ".class";
					JarFile jarFile = null;
					try {
					    jarFile = new JarFile(pathDir);
    					// this looks for a class file
    					JarEntry entry = jarFile.getJarEntry(entryName);
    					if (entry != null) {
    						// get the class file from the jar and return it
    						return pathDir;
    					} else {
    						// look for the xml file instead
    						entryName = className.replace('.', '/') + ".xml";
    						entry = jarFile.getJarEntry(entryName);
    						if (entry != null) {
    							return pathDir;
    						}
    					}
					} finally {
					    if(jarFile != null) {
					        jarFile.close();
					    }
					}
				} catch (Exception e) {
					// keep going if this isn't a jar file
					continue;
				}
			}
		}
		throw new FileNotFoundException("Cannot find the specified class "
				+ "file in the classpath.");
	}

	/**
	 * locates a directory based on an array of paths to look for. for instance,
	 * if you are searching for org/kepler/kar searchForMe would be {org,
	 * kepler, kar}. dir is where the search starts. Index should start out as
	 * 0.
	 */
	private boolean matchDirectoryPath(File dir, String[] searchForMe, int index) {
		File[] dirContents = dir.listFiles();
		for (int i = 0; i < dirContents.length; i++) {
			if (dirContents[i].getName().equals(searchForMe[index])) {
				if (index < searchForMe.length - 1) {
					if (index < searchForMe.length) {
						return matchDirectoryPath(dirContents[i], searchForMe,
								++index);
					} else {
						return false;
					}
				} else {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * try to locate and parse a moml file as a class
	 */
	protected ComponentEntity parseMoMLFile(String className) throws Exception {
		if (isDebugging)
			log.debug("parseMoMLFile(" + className + ")");

        JarFile jarFile = null;
        InputStream xmlStream = null;
		try {
    		// first we need to find the file and read it
    		File classFile = searchClasspath(className);
    		StringWriter sw = new StringWriter();
    		if (classFile.getName().endsWith(".jar")) {
    			jarFile = new JarFile(classFile);
    			ZipEntry entry = jarFile.getEntry(className.replace('.', '/')
    					+ ".xml");
    			xmlStream = jarFile.getInputStream(entry);
    		} else {
    			xmlStream = new FileInputStream(classFile);
    		}
    
    		byte[] b = new byte[1024];
    		int numread = xmlStream.read(b, 0, 1024);
    		while (numread != -1) {
    			String s = new String(b, 0, numread);
    			sw.write(s);
    			numread = xmlStream.read(b, 0, 1024);
    		}
    		sw.flush();
    		// get the moml document
    		String xmlDoc = sw.toString();
    		sw.close();

    		if (isDebugging)
    			log.debug("**** MoMLParser ****");
    
    		// use the moml parser to parse the doc
    		MoMLParser parser = new MoMLParser();
    		parser.reset();
    
    		// System.out.println("processing " + className);
    		NamedObj obj = parser.parse(xmlDoc);
    		return (ComponentEntity) obj;
		} finally {
            if(jarFile != null) {
                jarFile.close();
            }
            if(xmlStream != null) {
                xmlStream.close();
            }
		}
	}

	/**
	 * createInstance. creates an instance of a class object. taken from
	 * ptolemy's MomlParser class. modified for this application.
	 * 
	 * @param newClass
	 *@param arguments
	 *@return TypedAtomicActor
	 *@exception Exception
	 */
	public static NamedObj createInstance(Class<?> newClass, Object[] arguments)
			throws Exception {
		if (isDebugging)
			log.debug("createInstance(" + newClass + "," + arguments + ")");
		Constructor<?>[] constructors = newClass.getConstructors();
		for (int i = 0; i < constructors.length; i++) {
			Constructor<?> constructor = constructors[i];
			Class<?>[] parameterTypes = constructor.getParameterTypes();

			for (int j = 0; j < parameterTypes.length; j++) {
				Class<?> c = parameterTypes[j];
				if (isDebugging) 
					log.debug(c.getName());
			}

			if (parameterTypes.length != arguments.length) {
				continue;
			}

			boolean match = true;

			for (int j = 0; j < parameterTypes.length; j++) {
				if (!(parameterTypes[j].isInstance(arguments[j]))) {
					match = false;
					break;
				}
			}

			if (match) {
				NamedObj newEntity = (NamedObj) constructor
						.newInstance(arguments);
				return newEntity;
			}
		}

		// If we get here, then there is no matching constructor.
		// Generate a StringBuffer containing what we were looking for.
		StringBuffer argumentBuffer = new StringBuffer();

		for (int i = 0; i < arguments.length; i++) {
			argumentBuffer.append(arguments[i].getClass() + " = \""
					+ arguments[i].toString() + "\"");

			if (i < (arguments.length - 1)) {
				argumentBuffer.append(", ");
			}
		}

		throw new Exception("Cannot find a suitable constructor ("
				+ arguments.length + " args) (" + argumentBuffer + ") for '"
				+ newClass.getName() + "'");
	}

	/**
	 * This method is just a helper debugging method to write out the object
	 * moml to a file in a specified location on disk, the cache dir.
	 * 
	 * @param obj
	 * @param filename
	 */
	private void writeDebugMomlFor(NamedObj obj, String filename) {
		String momlExport = obj.exportMoML();

		DotKeplerManager dkm = DotKeplerManager.getInstance();
		File f = new File(dkm.getCacheDir(), filename);
		FileWriter fw;
		try {
			fw = new FileWriter(f);
			fw.write(momlExport, 0, momlExport.length());
			fw.flush();
			fw.close();
			System.out.println("done exporting debug moml: " + f.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * metadata object for a classed property
	 */
	private static class ClassedProperty implements Serializable {
		public String name = "";
		public String value = "";
		public String className = "";
		public Integer iterator = null;

		/**
		 * constructor
		 */
		public ClassedProperty(String name, String value, String className) {
			this.name = name;
			this.value = value;
			this.className = className;
		}

		/**
		 * adds an integer to the end of the name of the property if there is
		 * more than one of these properties. for instance: semanticType0,
		 * semanticType1, etc.
		 * 
		 * @param int i the number to add to the name.
		 */
		public void addNameIterator(int i) {
			iterator = Integer.valueOf(i);
		}

		/**
		 * create the xml rep of the ClassedProperty
		 */
		public String toString() {
			if (iterator == null) {
				return "<property name=\"" + name + "\" value=\"" + value
						+ "\" class=\"" + className + "\"/>";
			} else {
				int itt = iterator.intValue();
				return "<property name=\"" + name + itt + "\" value=\"" + value
						+ "\" class=\"" + className + "\"/>";
			}
		}
	}

	/**
	 * metadata object that represents a port
	 */
	private static class PortMetadata implements Serializable {
		public String name = "";
		public String direction = "";
		public String type = "";
		public boolean multiport = false;
		public Vector<NamedObj> attributes = new Vector<NamedObj>();
		
		/** The class name of the port, e.g., ptolemy.actor.TypedIOPort */
		public String portClassName;

		public void addAttribute(NamedObj att) {
			attributes.addElement(att);
		}

		/**
		 * return the moml xml rep of the port
		 * 
		 * */
		public String toString() {
			StringBuffer sb = new StringBuffer();

			if (name.indexOf("kepler:") != -1) {
				sb.append("<property name=\"" + name
						+ "\" class=\"org.kepler.moml.PortAttribute\">\n");
			} else {
				sb.append("<property name=\"kepler:" + name
						+ "\" class=\"org.kepler.moml.PortAttribute\">\n");
			}
			sb.append("  <property name=\"direction\" value=\"" + direction
					+ "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
			sb.append("  <property name=\"dataType\" value=\"" + type
					+ "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
			sb.append("  <property name=\"isMultiport\" value=\"" + multiport
					+ "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
			for (int i = 0; i < attributes.size(); i++) {
				// System.out.println("appending port attribute: " +
				// ((NamedObj)attributes.elementAt(i)).exportMoML());
				sb.append("  "
						+ ((NamedObj) attributes.elementAt(i)).exportMoML());
			}
			sb.append("</property>\n");
			/*
			 * sb.append("<port name=\"" + name +
			 * "\" class=\"ptolemy.actor.IOPort\">\n");
			 * sb.append("  <property name=\"direction\" value=\"" + direction +
			 * "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
			 * sb.append("  <property name=\"dataType\" value=\"" + type +
			 * "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
			 * sb.append("  <property name=\"isMultiport\" value=\"" + multiport
			 * + "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
			 * sb.append("</port>\n");
			 */
			return sb.toString();
		}

		public String toMoMLString() {
			StringBuffer sb = new StringBuffer();
			String dispName = name;
			if (name.indexOf("kepler:") != -1) {
				dispName = name.substring(8, name.length());
			}

	        String escapedDispName = StringUtilities.escapeForXML(dispName);
				
            sb.append("<port name=\"" + escapedDispName + "\" class=\""
                    + portClassName + "\">\n");
				
			if (direction.equals("input")) {
				sb.append("  <property name=\"input\"/>\n");
			}
			if (direction.equals("output")) {
				sb.append("  <property name=\"output\"/>\n");
			}
			if (direction.equals("inputoutput")) {
				sb.append("  <property name=\"output\"/>\n");
				sb.append("  <property name=\"input\"/>\n");
			}
			// added by Dan Higgins April 2007
			sb.append("  <property name=\"dataType\" value=\"" + type
					+ "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
			sb.append("  <property name=\"isMultiport\" value=\"" + multiport
					+ "\" class=\"ptolemy.kernel.util.StringAttribute\"/>\n");
			for (int i = 0; i < attributes.size(); i++) {
				// System.out.println("appending port attribute: " +
				// ((NamedObj)attributes.elementAt(i)).exportMoML());
				sb.append("  "
						+ ((NamedObj) attributes.elementAt(i)).exportMoML());
			}
			// end addition by Dan Higgins April 2007
			sb.append("</port>\n");
			return sb.toString();
		}
	}
}