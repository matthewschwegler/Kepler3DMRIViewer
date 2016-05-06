/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
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

package org.kepler.objectmanager.data;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.basic.NamedObjController;
import ptolemy.vergil.basic.NodeControllerFactory;
import diva.graph.GraphController;

//////////////////////////////////////////////////////////////////////////
//// DataSourceControllerFactory
/**
 * This is attribute that produces a custom node controller for Data Sources.
 * This class produces a node controller that is customized in order to add the
 * "Get metadata" menu. To use this class, just insert it as an attribute inside
 * any Ptolemy II object, and then right clicking on the icon for that object
 * will result in the use of the controller specified here. The instance by
 * convention will be named "_controllerFactory", but the only reason to enforce
 * this is that only the first such controller factory found as an attribute
 * will be used. It is a singleton, so placing it any container will replace any
 * previous controller factory with the same name.
 * 
 * @author Matthew Jones
 * @version $Id: DataSourceControllerFactory.java 15645 2008-11-07 19:39:48Z
 *          berkley $
 * @since Ptolemy II 4.0
 * @Pt.ProposedRating Red (mbj)
 * @Pt.AcceptedRating Red (mbj)
 */
public class DataSourceControllerFactory extends NodeControllerFactory {

	/**
	 * Construct a new factory with the given container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name.
	 * @exception IllegalActionException
	 *                If the factory cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an attribute with this name.
	 */
	public DataSourceControllerFactory(NamedObj container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Return a new node controller. This class returns an instance of
	 * DataSourceInstanceController which customizes the "Get metadata" menu to
	 * support documentation for the data sources.
	 * 
	 * @param controller
	 *            The associated graph controller.
	 * @return A new node controller.
	 */
	public NamedObjController create(GraphController controller) {
		return new DataSourceInstanceController(controller);
	}
}