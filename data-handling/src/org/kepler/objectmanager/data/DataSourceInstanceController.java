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

package org.kepler.objectmanager.data;

import java.awt.event.ActionEvent;
import java.net.URL;

import org.kepler.dataproxy.datasource.DataSourceInterface;

import ptolemy.actor.gui.BrowserEffigy;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.actor.ActorInstanceController;
import ptolemy.vergil.toolbox.FigureAction;
import ptolemy.vergil.toolbox.MenuActionFactory;
import diva.graph.GraphController;

//////////////////////////////////////////////////////////////////////////
//// DataSourceInstanceController
/**
 * This class provides a controller that overrides the default GetDocumentation
 * actions for data sources and can transform data source documentation to HTML
 * format before displaying it.
 * <p>
 * NOTE: There should be only one instance of this class associated with a given
 * GraphController. This is because this controller listens for changes to the
 * graph and re-renders the ports of any actor instance in the graph when the
 * graph changes. If there is more than one instance, this rendering will be
 * done twice, which can result in bugs like port labels appearing twice.
 * 
 * @author Matthew Jones
 * @version $Id: DataSourceInstanceController.java,v 1.1 2004/10/25 23:31:46
 *          jones Exp $
 * @since Ptolemy II 4.0
 * @Pt.ProposedRating Red (mbj)
 * @Pt.AcceptedRating Red (mbj)
 */
public class DataSourceInstanceController extends ActorInstanceController {

	/**
	 * Create an actor instance controller associated with the specified graph
	 * controller with full access.
	 * 
	 * @param controller
	 *            The associated graph controller.
	 */
	public DataSourceInstanceController(GraphController controller) {
		this(controller, FULL);
	}

	/**
	 * Create an entity controller associated with the specified graph
	 * controller with the specified access.
	 * 
	 * @param controller
	 *            The associated graph controller.
	 */
	public DataSourceInstanceController(GraphController controller,
			Access access) {
		super(controller, access);
		_menuFactory.addMenuItemFactory(new MenuActionFactory(
				new GetDSDocumentationAction()));
	}

	// /////////////////////////////////////////////////////////////////
	// // protected variables ////

	// /////////////////////////////////////////////////////////////////
	// // inner classes ////

	/**
	 * This is an action that accesses the documentation for a Ptolemy object
	 * associated with a figure. Note that this base class does not put this
	 * action in a menu, since some derived classes will not want it. But by
	 * having it here, it is available to all derived classes.
	 */
	protected class GetDSDocumentationAction extends FigureAction {

		public GetDSDocumentationAction() {
			super("Get Metadata");
		}

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			NamedObj target = getTarget();
			String className = target.getClass().getName();
			String docName = "doc.codeDoc." + className;
			try {
				URL toRead = null;
				if (target instanceof DataSourceInterface) {
					toRead = ((DataSourceInterface) target).getDocumentation();
				} else {
					toRead = getClass().getClassLoader().getResource(
							docName.replace('.', '/') + ".html");
				}
				if (toRead != null) {
					if (_configuration != null) {
						boolean useBrowser = false;
						String ref = toRead.getRef();
						if (ref != null) {
							useBrowser = ref.equals("in_browser");
						}
						if (useBrowser && (BrowserEffigy.staticFactory != null)) {
							_configuration.openModel(toRead, toRead, toRead
									.toExternalForm(),
									BrowserEffigy.staticFactory);
						} else {
							_configuration.openModel(toRead, toRead, toRead
									.toExternalForm());
						}
						// HyperlinkEvent hle = new HyperlinkEvent(target,
						// HyperlinkEvent.EventType.ACTIVATED, toRead,
						// toRead.toExternalForm());
						// HTMLViewer htmlViewer = new HTMLViewer();
						// htmlViewer.hyperlinkUpdate(hle);
						// _configuration.openModel(null, toRead, toRead
						// .toExternalForm());
					} else {
						MessageHandler.error("Cannot open documentation for "
								+ "data source " + className
								+ " without a configuration.");
					}
				} else {
					MessageHandler.error("Cannot find documentation for "
							+ className + "\nSorry.");
				}
			} catch (Exception ex) {
				MessageHandler.error("Cannot find documentation for "
						+ className + "\nSorry.", ex);
			}
		}
	}
}