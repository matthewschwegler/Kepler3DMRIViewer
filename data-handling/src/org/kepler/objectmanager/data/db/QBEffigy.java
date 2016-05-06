/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.data.db;

import java.lang.reflect.Method;
import java.net.URL;

import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.EffigyFactory;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// QBEffigy
/**
 * An effigy for a SQL XML definition.
 */
public class QBEffigy extends Effigy {

	/**
	 * Create a new effigy in the specified workspace with an empty string for
	 * its name.
	 * 
	 * @param workspace
	 *            The workspace for this effigy.
	 */
	public QBEffigy(Workspace workspace) {
		super(workspace);
	}

	/**
	 * Create a new effigy in the given directory with the given name.
	 * 
	 * @param container
	 *            The directory that contains this effigy.
	 * @param name
	 *            The name of this effigy.
	 */
	public QBEffigy(CompositeEntity container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Create a new effigy in the given container containing the specified text.
	 * The new effigy will have a new instance of DefaultStyledDocument
	 * associated with it.
	 * 
	 * @param container
	 *            The container for the effigy.
	 * @return A new instance of QBEffigy.
	 * @exception Exception
	 *                If the text effigy cannot be contained by the specified
	 *                container, or if the specified text cannot be inserted
	 *                into the document.
	 */
	public static QBEffigy newQBEffigy(CompositeEntity container)
			throws Exception {
		// Create a new effigy.
		QBEffigy effigy = new QBEffigy(container, container
				.uniqueName("effigy"));
		return effigy;
	}

	// /////////////////////////////////////////////////////////////////
	// // inner classes ////

	/**
	 * A factory for creating new effigies.
	 */
	public static class Factory extends EffigyFactory {

		/**
		 * Create a factory with the given name and container.
		 * 
		 * @param container
		 *            The container.
		 * @param name
		 *            The name.
		 * @exception IllegalActionException
		 *                If the container is incompatible with this entity.
		 * @exception NameDuplicationException
		 *                If the name coincides with an entity already in the
		 *                container.
		 */
		public Factory(CompositeEntity container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
			try {
				Class effigyClass = Class
						.forName("org.kepler.objectmanager.data.db.QBEffigy");
				_newQBEffigyURL = effigyClass.getMethod("newQBEffigy",
						new Class[] { CompositeEntity.class, URL.class,
								URL.class });
			} catch (ClassNotFoundException ex) {
				throw new IllegalActionException(ex.toString());
			} catch (NoSuchMethodException ex) {
				throw new IllegalActionException(ex.toString());
			}
		}

		// /////////////////////////////////////////////////////////////
		// // public methods ////

		/**
		 * Return true, indicating that this effigy factory is capable of
		 * creating an effigy without a URL being specified.
		 * 
		 * @return True.
		 */
		public boolean canCreateBlankEffigy() {
			return true;
		}

		/**
		 * Create a new effigy in the given container by reading the specified
		 * URL. If the specified URL is null, then create a blank effigy. The
		 * extension of the URL is not checked, so this will open any file.
		 * Thus, this factory should be last on the list of effigy factories in
		 * the configuration. The new effigy will have a new instance of
		 * DefaultStyledDocument associated with it.
		 * 
		 * @param container
		 *            The container for the effigy.
		 * @param base
		 *            The base for relative file references, or null if there
		 *            are no relative file references. This is ignored in this
		 *            class.
		 * @param in
		 *            The input URL.
		 * @return A new instance of QBEffigy.
		 * @exception Exception
		 *                If the URL cannot be read, or if the data is malformed
		 *                in some way.
		 */
		public Effigy createEffigy(CompositeEntity container, URL base, URL in)
				throws Exception {
			// Create a new effigy.
			try {
				return (Effigy) _newQBEffigyURL.invoke(null, new Object[] {
						container, base, in });
			} catch (java.lang.reflect.InvocationTargetException ex) {
				if (ex instanceof Exception) {
					// Rethrow the initial cause
					throw (Exception) (ex.getCause());
				} else {
					throw new Exception(ex.getCause());
				}
				// Uncomment this for debugging
				// throw new java.lang.reflect.InvocationTargetException(ex,
				// " Invocation of method failed!. Method was: "
				// + _newQBEffigyURL
				// + "\nwith arguments( container = " + container
				// + " base = " + base + " in = " + in + ")");
			}
		}

		private Method _newQBEffigyURL;
	}
}