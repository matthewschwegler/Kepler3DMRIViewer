/*
 * Copyright (c) 2000-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-12-11 14:43:48 -0800 (Tue, 11 Dec 2012) $' 
 * '$Revision: 31224 $'
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

import java.awt.Component;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JMenuItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationNamespace;
import org.kepler.configuration.ConfigurationProperty;

import ptolemy.actor.Director;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.Port;
import ptolemy.kernel.Relation;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.basic.BasicGraphController;
import ptolemy.vergil.basic.BasicGraphFrame;
import ptolemy.vergil.basic.ContextMenuFactoryCreator;
import ptolemy.vergil.toolbox.MenuItemFactory;
import ptolemy.vergil.toolbox.PtolemyMenuFactory;
import diva.canvas.CanvasLayer;
import diva.canvas.Figure;
import diva.graph.GraphController;
import diva.graph.GraphPane;
import diva.gui.toolbox.JContextMenu;
import diva.gui.toolbox.MenuFactory;

//////////////////////////////////////////////////////////////////////////
//// KeplerContextMenuFactory

/**
 * A factory that creates popup context menus for Kepler actors, directors, etc.
 * 
 * @author Matthew Brooke
 * @version $Id: KeplerContextMenuFactory.java 12101 2006-02-28 00:50:34Z brooke
 *          $
 * @since Ptolemy II 1.0
 * @Pt.ProposedRating Red
 * @Pt.AcceptedRating Red
 */
public class KeplerContextMenuFactory extends PtolemyMenuFactory implements
		MenuFactory {

	/**
	 * Create a new menu factory that contains no menu item factories.
	 * 
	 * @param controller
	 *            GraphController
	 */
	public KeplerContextMenuFactory(GraphController controller) {
		super(controller);
		this.controller = controller;
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Create an instance of the menu associated with this factory.
	 * 
	 * @param figure
	 *            The figure for which to create a context menu.
	 * @return JContextMenu
	 */
	@Override
	public JContextMenu create(Figure figure) {

		/**
		 * @todo - FIXME - wanted to do this only once, then cache the menu -
		 *       however, the menu actions in PTII "stick" at the value of the
		 *       actor first clicked on, unless we redo this each time - MB
		 */

		NamedObj object = _getObjectFromFigure(figure);
		if (object == null) {
			return null;
		}

		menuItemHolder = new JContextMenu(object, object.getFullName());

		Component parent = getParent(figure);
		menuItemHolder.setInvoker(parent);

		// 1) Get all PTII menu items and put them in a Map for easier
		// access later...
		Map<String, Action> origMenuItemsMap = getOriginalMenuItemsMap(object, false);

		// 2) Now we have all the PTII menu items, get the
		// Kepler-specific menu mappings from the preferences file,
		// then go thru the Kepler menu mappings and
		// populate the new popup menu with Kepler menus,
		// creating any new menu items that don't exist yet

		// this is a Map that will be used to keep track of
		// what we have added to the menus, and in what order
		menu = createKeplerContextMenu(origMenuItemsMap, object,
				getTableauFrame(figure));

		if (menu == null) {
			log.error("Problem creating Kepler context menus - using PTII defaults");
			return super.create(figure);
		}
		return menu;
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	private JContextMenu createKeplerContextMenu(Map<String, Action> ptiiMenuActionsMap,
			NamedObj object, TableauFrame tFrameInstance) {

		if (ptiiMenuActionsMap == null) {
			return null;
		}

		final LinkedHashMap<String, JMenuItem> keplerCtxtMenuMap = new LinkedHashMap<String, JMenuItem>();
		final JContextMenu contextMenu = new JContextMenu(object,
				object.getFullName());

		log.debug("***************\nKEPLER CONTEXT MENUS:\n***************\n");

		Iterator<ConfigurationProperty> it = null;
		try {
			ConfigurationProperty prop = ConfigurationManager
					.getInstance()
					.getProperty(ConfigurationManager.getModule("gui"),
							new ConfigurationNamespace(CONTEXT_MENU_MAPPINGS_NAME));
			List<ConfigurationProperty> reposList = prop.getProperties("name", true);
			// it = getContextMenuMappingsResBundle().getKeys();
			it = reposList.iterator();

			while (it.hasNext()) {
				// String nextKey = (String) (it.next());
				ConfigurationProperty cp = (ConfigurationProperty) it.next();
				String nextKey = cp.getValue();
				String nextVal = cp.getParent().getProperty("value").getValue();

				// System.out.println("key: " + nextKey + " val: " + nextVal +
				// " menuBaseName: " + _menuBaseName);

				if (nextKey == null || !nextKey.startsWith(_menuBaseName)) {
					continue;
				}
				if (isDebugging) {
					log.debug("nextKey: " + nextKey);
				}

				if (isDebugging) {
					log.debug("nextVal: " + nextVal);
				}

				if (nextVal == null || nextVal.trim().length() < 1) {
					if (isDebugging) {
						log.warn("no menu mapping found for key: " + nextKey);
					}
					// System.out.println("no menu mapping found for key: " +
					// nextKey);
					continue;
				}

				Action action = null;

				if (nextKey.indexOf(MenuMapper.MENU_SEPARATOR_KEY) < 0) {

					action = MenuMapper.getActionFor(nextVal,
							ptiiMenuActionsMap, tFrameInstance);
					if (action == null) {
						if (isDebugging) {
							log.warn("null action for value " + nextVal);
						}
						// System.out.println("WARNING: null action for context menu item: "
						// + nextVal);
						continue;
					}
				}
				// get rid of prefix - like "ACTOR->", "DIRECTOR->" etc
				if (nextKey.startsWith(_menuBaseName)) {
					nextKey = nextKey.substring(menuPathPrefixLength);
				}

				// System.out.println("adding menu for key: " + nextKey +
				// " action: " + action );
				MenuMapper.addMenuFor(nextKey, action, contextMenu,
						keplerCtxtMenuMap);
			}

		} catch (Exception ex) {
			if (isDebugging) {
				log.warn("Exception opening menu mappings: " + ex
						+ "\nDefaulting to PTII menus");
				if (isDebugging) {
					ex.printStackTrace();
				}
				return null;
			}
		}
		log.debug("***************\nEND KEPLER CONTEXT MENUS:\n***************\n");

		return contextMenu;
	}

	/**
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
	protected Map<String, Action> getOriginalMenuItemsMap(NamedObj object, boolean isWorkflow) {

		Map<String, Action> retMap = new HashMap<String, Action>();
		if (isWorkflow) {
			_menuBaseName = WORKFLOW_BASE_NAME;
		} else if (object instanceof Director) {
			_menuBaseName = DIRECTOR_BASE_NAME;
		} else if (object instanceof Attribute) {
			_menuBaseName = ATTRIB_BASE_NAME;
		} else if (object instanceof ComponentEntity) {
			_menuBaseName = ACTOR_BASE_NAME;
		} else if (object instanceof Port) {
			_menuBaseName = PORT_BASE_NAME;
		} else if (object instanceof Relation) {
			_menuBaseName = LINK_BASE_NAME;
		} else { // catch-all
			_menuBaseName = "UNKNOWN";
			if (isDebugging) {
				log.error("KeplerContextMenuFactory was asked to handle a NamedObj "
						+ "type that was not recognized: "
						+ object.getClassName());
			}
		}
		menuPathPrefixLength = _menuBaseName.length()
				+ MenuMapper.MENU_PATH_DELIMITER.length();

		Iterator i = menuItemFactoryList().iterator();

		int n = 0;
		while (i.hasNext()) {
			MenuItemFactory factory = (MenuItemFactory) i.next();
			JMenuItem menuItem = factory.create(menuItemHolder, object);

			if(menuItem != null) {
				StringBuffer pathBuff = new StringBuffer(_menuBaseName);
				// System.out.println("ptii context menu item found: "+
				// menuItem.getText());
				if (isDebugging) {
					log.debug("Found PTII context-menu item: "
							+ menuItem.getText());
				}
				MenuMapper.storePTIIMenuItems(menuItem, pathBuff,
						MenuMapper.MENU_PATH_DELIMITER, retMap);
			}
			n++;
		}
		return retMap;
	}

    protected Component getParent(Figure figure) {

		if (figure != null) {
			CanvasLayer layer = figure.getLayer();
			GraphPane pane = (GraphPane) layer.getCanvasPane();
			return pane.getCanvas();
		} else {
			BasicGraphFrame bgf = null;
			try {
				bgf = ((BasicGraphController) controller).getFrame();
			} catch (Exception ex) {
				bgf = null;
			}
			return (Component) bgf;
		}
	}

	private TableauFrame getTableauFrame(Figure figure) {

		Component parent = getParent(figure);

		if (parent != null) {
			while (parent.getParent() != null) {
				parent = parent.getParent();
			}
			if (parent instanceof TableauFrame) {
				log.debug("TABLEAUFRAME FOUND");
				return (TableauFrame) parent;
			}
		}
		log.warn("getTableauFrame() returning NULL!!");
		return null;
	}

	// /////////////////////////////////////////////////////////////////
	// // private members ////

	/** The name of the configuration file containing the context
	 *  menu mappings for Kepler.
	 */
	private static final String CONTEXT_MENU_MAPPINGS_NAME = "uiContextMenuMappings";

	private static final Log log = LogFactory
			.getLog(KeplerContextMenuFactory.class.getName());

	private static final boolean isDebugging = log.isDebugEnabled();

	// the popup menu associated with this particular instance.
	private JContextMenu menu = null;

	// a dummy popup menu to hold all the previously-added menu items associated
	// with this particular instance, so we can rearrange them to suit our needs
	private JContextMenu menuItemHolder = null;

	protected String _menuBaseName = null;

	// the length of the first section of the menu path, which identifies the
	// type of context menu - eg the prefix part of "ACTOR->Look Inside" would
	// be
	// "ACTOR->", and the menuPathPrefixLength for this would be 7
	private int menuPathPrefixLength;
	private GraphController controller;

	private final static String DIRECTOR_BASE_NAME = "DIRECTOR";
	private final static String ACTOR_BASE_NAME = "ACTOR";
	private final static String ATTRIB_BASE_NAME = "ATTRIBUTE";
	private final static String PORT_BASE_NAME = "PORT";
	private final static String LINK_BASE_NAME = "LINK";
	private final static String WORKFLOW_BASE_NAME = "WORKFLOW";

	// /////////////////////////////////////////////////////////////////
	// // inner classes ////

	/**
	 * A factory that creates the KeplerContextMenuFactory - used by the config
	 * 
	 * @author Matthew Brooke
	 */
	public static class Factory extends ContextMenuFactoryCreator {

		/**
		 * Create an factory with the given name and container.
		 * 
		 * @param container
		 *            The container.
		 * @param name
		 *            The name of the entity.
		 * @exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 * @exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		public MenuFactory createContextMenuFactory(GraphController controller) {
			return new KeplerContextMenuFactory(controller);
		}
	}

}
