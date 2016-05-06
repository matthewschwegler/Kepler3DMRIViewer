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

/*
 * @author xiaowen
 */

// To compile and test:
// 1. Set your classpath to include all Ptolemy and SPA jars
// 2. javac ActorInspector.java
// 3. java org.sdm.spa.util.ActorInspector
package org.sdm.spa.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ptolemy.actor.Manager;
import ptolemy.actor.TypeConflictException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedCompositeActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.lib.Recorder;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.Type;
import ptolemy.domains.pn.kernel.PNDirector;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ConfigurableAttribute;
import ptolemy.kernel.util.KernelException;

public class ActorInspector {

	TypedAtomicActor actor;

	public static void main(String[] args) {
		ActorInspector m;
		try {
			m = new ActorInspector("org.sdm.spa.StringConst");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		List inports = m.getInputPorts();
		List outports = m.getOutputPorts();
		List attrs = m.getAttributeList();

		System.out.println("Input ports: " + inports);
		System.out.println("Output ports: " + outports);
		System.out.println("Attributes: " + attrs);
	}

	/**
	 * @param fullname
	 *            Name of the class to instantiate.
	 * @exception ClassCastException
	 *                If the class is not a TypedAtomicActor.
	 * @exception ClassNotFoundException
	 *                If the class cannot be found.
	 * @exception InstantiationException
	 *                If the class cannot be instantiated.
	 * @exception IllegalAccessException
	 *                If the class is not accessible.
	 * @exception InvocationTargetException
	 *                If the underlying constructor throws an exception.
	 * @exception KernelException
	 *                If the manager initializing a workflow containing this
	 *                actor throws it.
	 * @exception NoSuchMethodException
	 *                If expected constructor not found.
	 */
	public ActorInspector(String fullname) throws ClassCastException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException, KernelException,
			NoSuchMethodException {

		TypedCompositeActor container;
		Recorder rec;
		PNDirector dir;
		Manager manager;

		try {
			container = new TypedCompositeActor();
			rec = new Recorder(container, "rec");
			dir = new PNDirector(container, "dir");
			manager = new Manager("manager");

			container.setManager(manager);
		} catch (ptolemy.kernel.util.NameDuplicationException e) {
			e.printStackTrace();
			throw new RuntimeException("Internal Error!");
		} catch (ptolemy.kernel.util.IllegalActionException e) {
			e.printStackTrace();
			throw new RuntimeException("Internal Error!");
		}

		Class actorclass = Class.forName(fullname);

		// Check this is a TypedAtomicActor.
		if (!TypedAtomicActor.class.isAssignableFrom(actorclass)) {
			throw new ClassCastException(fullname
					+ " is not a subclass of TypedAtomicActor.");
		}

		Constructor constructor = actorclass.getConstructor(new Class[] {
				CompositeEntity.class, String.class });
		actor = (TypedAtomicActor) constructor.newInstance(new Object[] {
				container, "actor" });

		try {
			manager.initialize();
		} catch (TypeConflictException e) {
			System.err.println("TypeConflictException while processing "
					+ fullname + ": " + e);
			// The port is probably configured based on what it's linked to.
		}
	}

	// ***** Attribute Class and Methods *****

	public List getAttributeList() {
		Vector vecAttributes = new java.util.Vector();

		Iterator it = actor.attributeList().iterator();

		while (it.hasNext()) {
			Attribute at = (Attribute) it.next();

			String name = at.getName();

			String value;
			if (at instanceof Parameter) {
				value = at.toString();
			} else if (at instanceof ConfigurableAttribute) {
				value = ((ConfigurableAttribute) at).getExpression();
			} else {
				value = "";
			}

			vecAttributes.add(new AttributeInfo(name, value));
		}

		return vecAttributes;
	}

	public class AttributeInfo {
		public String name;
		public String value;

		public AttributeInfo(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String toString() {
			return "name: " + name + " value: " + value;
		}
	}

	// ***** Port Class and Methods *****

	public List getInputPorts() {
		return getPorts(actor.inputPortList());
	}

	public List getOutputPorts() {
		return getPorts(actor.outputPortList());
	}

	private List getPorts(List list) {
		Vector vecPorts = new java.util.Vector();

		Iterator it = list.iterator();

		while (it.hasNext()) {
			TypedIOPort port = (TypedIOPort) it.next();

			// get properties
			String name = port.getName();
			Type type = port.getType();
			boolean isMultiport = port.isMultiport();

			// append to vector
			PortInfo pi = new PortInfo(name, type, isMultiport);
			vecPorts.addElement(pi);
		}

		return vecPorts;
	}

	public class PortInfo {
		public String name;
		public Type type;
		public boolean isMultiport;

		public PortInfo(String name, Type type, boolean isMultiport) {
			this.name = name;
			this.type = type;
			this.isMultiport = isMultiport;
		}

		public String toString() {
			return "name: " + name + " type: " + type + " isMultiport: "
					+ isMultiport;
		}
	}
}