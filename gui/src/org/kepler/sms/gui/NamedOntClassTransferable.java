/*
 * Copyright (c) 2005-2010 The Regents of the University of California.
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

package org.kepler.sms.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.kepler.sms.NamedOntClass;

import ptolemy.vergil.kernel.VergilUtilities;

/**
 * A transferable object that contains a local JVM reference to a number of
 * named objects. To get a reference to an iterator on the objects, request data
 * with the data flavor given in the static namedObjFlavor variable.
 * 
 * Note that this class was copied/modified from
 * ptolemy.vergil.toolbox.PtolemyTransferable
 */

public class NamedOntClassTransferable implements Transferable, Serializable {

	/**
	 * Create a new transferable object that contains no objects.
	 */
	public NamedOntClassTransferable() {
		_objectList = new LinkedList();
	}

	/**
	 * The only flavor associated with this transferable
	 */
	public static DataFlavor getNamedOntClassFlavor() {
		return namedOntClassFlavor;
	}

	/**
	 * Add the given named object to the objects contained in this transferable.
	 * If the object already exists in this transferable, then do not add it
	 * again.
	 * 
	 * @param object
	 *            The object to be added to this transferable.
	 */
	public void addObject(NamedOntClass object) {
		if (!_objectList.contains(object))
			_objectList.add(object);
	}

	/**
	 * Return the data flavors that this transferable supports.
	 * 
	 * @return The data flavors.
	 */
	public synchronized DataFlavor[] getTransferDataFlavors() {
		return _flavors;
	}

	/**
	 * Return true if the given data flavor is supported.
	 * 
	 * @param flavor
	 *            The data flavor that is searched for.
	 * @return true if the given data flavor is supported.
	 */
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		int i;

		for (i = 0; i < _flavors.length; i++) {
			if (_flavors[i].equals(flavor)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Return an object that represents the data contained within this
	 * transferable with the given flavor. If the flavor is namedOntClassFlavor,
	 * return an iterator of the objects that this transferable refers to.
	 * Otherwise, an exception is thrown.
	 * 
	 * @param flavor
	 *            The data flavor.
	 * @return An object with the given flavor.
	 * @exception UnsupportedFlavorException
	 *                If the given flavor is not supported.
	 */
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (flavor.equals(namedOntClassFlavor)) {
			return _objectList.iterator();
		}

		throw new UnsupportedFlavorException(flavor);
	}

	/**
	 * Remove the given object from this transferable. If the object does not
	 * exist in the transferable, then do nothing.
	 * 
	 * @param object
	 *            The object to be removed.
	 */
	public void removeObject(NamedOntClass object) {
		if (_objectList.contains(object))
			_objectList.remove(object);
	}

	// Under MacOS X 10.2, Java 1.4.1_01 we get a stack trace
	// when ever we drag and drop. For details See
	// http://lists.apple.com/archives/java-dev/2003/Apr/16/classcastexceptionindrag.txt
	// FIXME: This change happened just before the release of 3.0.2,
	// so we only make the change under Mac OS.
	static {
		if (VergilUtilities.macOSLookAndFeel()) {
			namedOntClassFlavor = new DataFlavor(
					DataFlavor.javaJVMLocalObjectMimeType
							+ ";class=org.kepler.sms.NamedOntClass",
					"Named Ont Class");
		} else {
			namedOntClassFlavor = new DataFlavor(
					DataFlavor.javaJVMLocalObjectMimeType
							+ "org.kepler.sms.NamedOntClass", "Named Ont Class");
		}
	}

	private final DataFlavor[] _flavors = { namedOntClassFlavor };
	private List _objectList; // the object contained by this transferable.
	private static final DataFlavor namedOntClassFlavor;

}