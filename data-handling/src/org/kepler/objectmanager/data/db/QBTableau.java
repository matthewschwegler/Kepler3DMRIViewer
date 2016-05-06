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

import ptolemy.actor.gui.Tableau;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
//// QBTableau
/**
 * A tableau representing Query Builder. The constructor of this class creates
 * the window. The QB window itself is an instance of QBEditor, and can be
 * accessed using the getFrame() method. As with other tableaux, this is an
 * entity that is contained by an effigy of a model. There can be any number of
 * instances of this class in an effigy.
 */
public class QBTableau extends Tableau {

	/**
	 * Construct a new tableau for the model represented by the given effigy.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name.
	 * @exception IllegalActionException
	 *                If the container does not accept this entity (this should
	 *                not occur).
	 * @exception NameDuplicationException
	 *                If the name coincides with an attribute already in the
	 *                container.
	 */
	public QBTableau(QBEffigy container, String name)
			throws IllegalActionException, NameDuplicationException {

		this(container, name, null);
	}

	/**
	 * Construct a new tableau for the model represented by the given effigy.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name.
	 * @param editor
	 *            The text editor to use, or null to use the default.
	 * @exception IllegalActionException
	 *                If the container does not accept this entity (this should
	 *                not occur).
	 * @exception NameDuplicationException
	 *                If the name coincides with an attribute already in the
	 *                container.
	 */
	public QBTableau(QBEffigy container, String name, QBEditor editor)
			throws IllegalActionException, NameDuplicationException {

		super(container, name);
		String title = "Unnamed";
		QBEditor frame = editor;
		setFrame(frame);
		frame.setTableau(this);
	}

}