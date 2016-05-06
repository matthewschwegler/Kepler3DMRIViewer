/*
 * Copyright (c) 1999-2010 The Regents of the University of California.
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

package org.kepler.gui;

import java.util.Map;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.basic.ContextMenuFactoryCreator;
import diva.canvas.Figure;
import diva.graph.GraphController;
import diva.gui.toolbox.MenuFactory;

//////////////////////////////////////////////////////////////////////////
//// KeplerSchematicContextMenuFactory

/**
 * A factory that creates popup context menus for Kepler actors, directors, etc.
 * 
 * @author Matthew Brooke
 * @version $Id: KeplerSchematicContextMenuFactory.java 12085 2006-02-25
 *          06:23:24Z brooke $
 * @since Ptolemy II 1.0
 * @Pt.ProposedRating Red
 * @Pt.AcceptedRating Red
 */
public class KeplerSchematicContextMenuFactory extends KeplerContextMenuFactory {

	/**
	 * Create a new context menu factory associated with the specified
	 * controller.
	 * 
	 * @param controller
	 *            The controller.
	 */
	public KeplerSchematicContextMenuFactory(GraphController controller) {
		super(controller);
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	/**
	 * OVERRIDES PARENT CLASS TO SET isWorkflow TO TRUE
	 * 
	 * get Map of name/value pairs containing menu paths of original PTII
	 * context- menu items, and their correspondign Action objects
	 * 
	 * @param object
	 *            NamedObj
	 * @param isWorkflow
	 *            boolean - @todo - FIXME - this is a gnarly hack because a
	 *            workflow is actually a TypedCompositeActor, so if we just rely
	 *            in the "instanceof" checks like we do for other context menus,
	 *            this code will assume the workflow is actually an actor, and
	 *            will display the actor context menu instead of the workflow
	 *            one
	 * @return Map
	 */
	protected Map getOriginalMenuItemsMap(NamedObj object, boolean isWorkflow) {
		return super.getOriginalMenuItemsMap(object, true);
	}

	/**
	 * @see ptolemy.vergil.basic.BasicGraphController$SchematicContextMenuFactory
	 * @param source
	 *            Figure
	 * @return NamedObj
	 */
	protected NamedObj _getObjectFromFigure(Figure source) {
		if (source != null) {
			Object object = source.getUserObject();
			return (NamedObj) getController().getGraphModel()
					.getSemanticObject(object);
		} else {
			return (NamedObj) getController().getGraphModel().getRoot();
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // inner classes ////

	/**
	 * A factory that creates the KeplerSchematicContextMenuFactory - used by
	 * the config
	 * 
	 * @author Matthew Brooke
	 */
	public static class Factory extends ContextMenuFactoryCreator {

		/**
		 * Create an factory with the given name and container.
		 * 
		 *@param container
		 *            The container.
		 *@param name
		 *            The name of the entity.
		 *@exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 *@exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		public MenuFactory createContextMenuFactory(GraphController controller) {
			return new KeplerSchematicContextMenuFactory(controller);
		}
	}
}