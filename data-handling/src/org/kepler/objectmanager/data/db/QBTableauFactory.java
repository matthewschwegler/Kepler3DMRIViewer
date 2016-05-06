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

import java.util.Iterator;

import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFactory;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.StringAttribute;

//////////////////////////////////////////////////////////////////////////
//// QBTableauFactory
/**
 * This class is an attribute that creates a Query Builder to edit a specified
 * string attribute in the container of this attribute.
 */
public class QBTableauFactory extends TableauFactory {

	/**
	 * Create a factory with the given name and container.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name.
	 * @exception IllegalActionException
	 *                If the container is incompatible with this attribute.
	 * @exception NameDuplicationException
	 *                If the name coincides with an attribute already in the
	 *                container.
	 */
	public QBTableauFactory(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);

		_sqlAttrName = new StringAttribute(this, "sqlName");
		((Settable) _sqlAttrName).setExpression("sqlDef");
		_schemaAttrName = new StringAttribute(this, "schemaName");
		((Settable) _schemaAttrName).setExpression("schemaDef");
	}

	// /////////////////////////////////////////////////////////////////
	// // parameters ////

	/** The name of the sql definition string attribute that is to be edited. */
	public StringAttribute _sqlAttrName;

	/** The name of the schema definition string attribute (not editted) */
	public StringAttribute _schemaAttrName;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Remove any editor that may have been associated with this object by a
	 * previous call to createEditor().
	 */
	public void clear() {
		_editor = null;
	}

	/**
	 * Create a tableau for the specified effigy. The tableau will be created
	 * with a new unique name with the specified effigy as its container. If
	 * this factory cannot create a tableau for the given effigy (it is not an
	 * instance of PtolemyEffigy), then return null.
	 * 
	 * @param effigy
	 *            The component effigy.
	 * @return A tableau for the effigy, or null if one cannot be created.
	 * @exception Exception
	 *                If the factory should be able to create a Tableau for the
	 *                effigy, but something goes wrong.
	 */
	public Tableau createTableau(Effigy effigy) throws Exception {
		if (!(effigy instanceof PtolemyEffigy)) {
			return null;
		}
		NamedObj object = ((PtolemyEffigy) effigy).getModel();
		Attribute sqlAttrName = object.getAttribute(_sqlAttrName
				.getExpression());
		if (!(sqlAttrName instanceof StringAttribute)) {
			throw new IllegalActionException(object, "Expected "
					+ object.getFullName()
					+ " to contain a StringAttribute named "
					+ _sqlAttrName.getExpression() + ", but it does not.");
		}

		Attribute schemaAttrName = object.getAttribute(_schemaAttrName
				.getExpression());
		if (!(schemaAttrName instanceof StringAttribute)) {
			throw new IllegalActionException(object, "Expected "
					+ object.getFullName()
					+ " to contain a StringAttribute named "
					+ _schemaAttrName.getExpression() + ", but it does not.");
		}

		// effigy may already contain a texteffigy.
		QBEffigy qbEffigy = null;
		Iterator subEffigies = effigy.entityList(QBEffigy.class).iterator();
		while (subEffigies.hasNext()) {
			qbEffigy = (QBEffigy) subEffigies.next();
		}
		if (qbEffigy == null) {
			qbEffigy = QBEffigy.newQBEffigy(effigy);
		}

		// qbEffigy may already have a tableau.
		Iterator tableaux = qbEffigy.entityList(QBTableau.class).iterator();
		if (tableaux.hasNext()) {
			return (QBTableau) tableaux.next();
		}

		// Need a new tableau, so create an editor for it.
		if (_editor == null) {
			_editor = new QBEditor(this, (StringAttribute) sqlAttrName,
					(StringAttribute) schemaAttrName, "Query Builder");
		}

		return new QBTableau(qbEffigy, "_tableau", _editor);
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	// Keep track of an open editor so that it isn't opened more than
	// once.
	private QBEditor _editor;
}