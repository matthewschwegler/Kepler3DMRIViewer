/*
 * Copyright (c) 1998-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-04-12 17:02:38 -0700 (Fri, 12 Apr 2013) $' 
 * '$Revision: 31915 $'
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.modules.ModuleTree;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationNamespace;
import org.kepler.configuration.ConfigurationProperty;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.gui.MemoryCleaner;
import diva.gui.GUIUtilities;
import diva.gui.toolbox.JContextMenu;

/**
 * MenuMapper
 * 
 * A menu creation Runnable that creates and adds a new JMenuBar that provides a
 * 'view' (in a loose sense) of the ptii menus. It enumerates all the existing
 * ptii menu items at runtime, and re-uses them, rearranged (and optionally
 * renamed) - all set via mappings in a localizable resourcebundle properties
 * (text) file.
 * 
 * If/when new menu items get added to ptii, they are immediately available for
 * use here, just by adding the relevant text mapping to the properties file.
 * 
 * 
 * @author Matthew Brooke
 * @version $Id: MenuMapper.java 31915 2013-04-13 00:02:38Z jianwu $
 * @since
 * @Pt.ProposedRating
 * @Pt.AcceptedRating
 */
public class MenuMapper {

	// /////////////////////////////////////////////////////////////////
	// // public variables ////

	public final static String MENUITEM_TYPE = "MENUITEM_TYPE";

	public final static String CHECKBOX_MENUITEM_TYPE = "CHECKBOX_MENUITEM_TYPE";

	public final static String NEW_JMENUITEM_KEY = "NEW_JMENUITEM";

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Debugging method
	 */
	public void printDebugInfo() {
		System.out.println("_ptiiMenuActionsMap: " + _ptiiMenuActionsMap.size());
		for (String s : _ptiiMenuActionsMap.keySet()) {
			Action a = _ptiiMenuActionsMap.get(s);
			System.out.println(s + " : " + a.toString());
		}
	}
	
	/**
	 * constructor
	 * 
	 * @param ptiiMenubar
	 *            JMenuBar the existing ptii menubar containing the original
	 *            menus
	 * @param tableauFrameInstance
	 *            TableauFrame the frame from which the ptii menu bar will be
	 *            hidden, and to which the new menu bar will be added
	 */
	public MenuMapper(final TableauFrame tableauFrameInstance) {
		this._tableauFrameInstance = tableauFrameInstance;
		_mappers.add(this);
		init();
	}

	public void init() {

		if (_tableauFrameInstance == null) {
			log.error("MenuMapper cannot proceed, due to one or more NULL values:"
					+ "\nptiiMenubar = "
					+ _ptiiMenubar
					+ "\ntableauFrameInstance = "
					+ _tableauFrameInstance
					+ "\ndefaulting to PTII menus");

			return;
		}

		// First, we need to make sure PTII has finished constructing its menus.
		// Those menus are created and assembled in a thread that is started in
		// the
		// pack() method of ptolemy.gui.Top. As that thread finishes, it adds
		// the
		// new ptii JMenuBar to the frame - so we test for that here, and don't
		// proceed until the frame has the new JMenuBar added
		// For safety, so we don't have an infinite loop, we also set a safety
		// counter:
		// maxWaitMS is the longest the app waits for the ptii
		// menus to be added, before it continues anyway.
		final int maxWaitMS = 500;
		// sleepTimeMS is the amount of time it sleeps per loop:
		final int sleepTimeMS = 5;
		// ////
		final int maxLoops = maxWaitMS / sleepTimeMS;
		int safetyCounter = 0;

		while (safetyCounter++ < maxLoops
				&& !_tableauFrameInstance.isMenuPopulated()) {

			if (isDebugging) {
				log.debug("Waiting for PTII menus to be created... "
						+ safetyCounter);
			}
			try {
				Thread.sleep(sleepTimeMS);
			} catch (Exception e) {
				// ignore
			}
		}
		
		JMenuBar _keplerMenubar = null;
		if(_ptiiMenubar == null) {
			_ptiiMenubar = _tableauFrameInstance.getJMenuBar();
		}
		
		if (_ptiiMenubar != null) {
			
			// gets here if a PTII menubar has been added to frame...

			// 1) Now PTII has finished constructing its menus, get all
			// menu items and put them in a Map for easier access later...
			_ptiiMenuActionsMap = getPTIIMenuActionsMap();
			Map<String, Action> ptiiMenuActionsMap = _ptiiMenuActionsMap;

			// 2) Now we have all the PTII menu items, get the
			// Kepler-specific menu mappings from the preferences file,
			// then go thru the Kepler menu mappings and
			// populate the new menubar with Kepler menus,
			// creating any new menu items that don't exist yet

			// this is a Map that will be used to keep track of
			// what we have added to the menus, and in what order
			_keplerMenubar = createKeplerMenuBar(ptiiMenuActionsMap);

			if (_keplerMenubar != null) {

				// First, look to see if any menus are empty. If
				// they are, remove the top-level menu from the menubar...
				// ** NOTE - do these by counting *down* to zero, otherwise the
				// menus'
				// indices change dynamically as we remove menus, causing
				// errors!
				for (int m = _keplerMenubar.getMenuCount() - 1; m >= 0; m--) {
					JMenu nextMenu = _keplerMenubar.getMenu(m);
					if (nextMenu.getMenuComponentCount() < 1) {
						if (isDebugging) {
							log.debug("deleting empty menu: "
									+ nextMenu.getText());
						}
						_keplerMenubar.remove(nextMenu);
					}
				}
				// hide the ptii menubar
				_tableauFrameInstance.hideMenuBar();

				// add the new menu bar
				_tableauFrameInstance.setJMenuBar(_keplerMenubar);
			} else {
				log.error("Problem creating Kepler menus - defaulting to PTII menus");
			}

		} else {
			// gets here if a PTII menubar has *NOT* been added to frame...
			// Therefore, this frame doesn't have a menubar by default,
			// so we probably shouldn't add one, for now, at least

			// hide the ptii menubar (may not be necessary)
			_tableauFrameInstance.hideMenuBar();

			// add the new menu bar (may not be necessary)
			_tableauFrameInstance.setJMenuBar(null);
		}
	}

	public static Action getActionFor(String key,
			Map<String, Action> menuActionsMap,
			TableauFrame tableauFrameInstance) {
		if (isDebugging) {
			log.debug("getActionFor(" + key + "," + menuActionsMap.toString()
					+ ")");
		}

		Action action = null;

		// it's a mapping...
		// NOTE that all keys in ptiiMenuActionsMap are
		// uppercase, to make the ptii value entries in the
		// menu mappings props file case-insensitive
		String uKey = key.toUpperCase();

		if (key.indexOf(MENU_PATH_DELIMITER) > -1) {
			Object val = menuActionsMap.get(uKey);
			if (val instanceof Action) {
				action = (Action) val;
			}
		} else {
			// it's a class or a separator

			if (uKey.equals(MENU_SEPARATOR_KEY.toUpperCase())) {

				// it's a separator
				return null;

			} else {

				// it's a class - try to instantiate...
				Object actionObj = null;
				boolean actionInitialized = true;
				try {
					// create the class
					String classSearchName = key.trim();
					ClassLoader thisClassLoader = MenuMapper.class.getClassLoader();
					Class<?> classDefinition = thisClassLoader.loadClass(classSearchName);

					// add the arg types
					Class<?>[] args = new Class[] { TableauFrame.class };

					// create a constructor
					Constructor<?> constructor;
					try {
						constructor = classDefinition.getConstructor(args);
					} catch (java.lang.NoSuchMethodException nsme) {
						args = new Class[] { ptolemy.vergil.basic.BasicGraphFrame.class };
						constructor = classDefinition.getConstructor(args);
					}

					// set the args
					Object[] argImp = new Object[] { tableauFrameInstance };

					// create the object
					//if (isDebugging) log.debug("BEFORE");
					actionObj = constructor.newInstance(argImp);
					//if (isDebugging) log.debug("AFTER");

					// if the action object is an InitializableAction, try to
					// initialize it. 
					if(actionObj instanceof InitializableAction) {
					    actionInitialized = ((InitializableAction)actionObj).initialize(menuActionsMap);
					}
					
				} catch (Exception e) {
					log.warn("Exception trying to create an Action for classname: <"
							+ key + ">:\n" + e.getCause() + " (" + e + ")");
					actionObj = null;
				}

				if (actionObj == null) {
					// System.out.println("2Error creating action for class: " +
					// key);
					if (isDebugging) {
						log.error("Problem trying to create an Action for classname: <"
								+ key
								+ ">\nPossible reasons:\n"
								+ "1) Should be a fully-qualified classname, including "
								+ "the package - Check carefully for mistakes.\n"
								+ "2) class must implement javax.swing.Action, and must "
								+ "have a constructor of the form: \n"
								+ "  MyConstructor(ptolemy.actor.gui.TableauFrame)\n"
								+ "Returning NULL Action for classname: " + key);
					}
					return null;
				// see if the initialization failed. if so, set the actionObj
				// to null so this action is not put on the menu bar.
				} else if(!actionInitialized) {
				    actionObj = null;
                }
				action = (Action) actionObj;
			}
		}
		return action;
	}

	/**
	 * Recurse through all the submenu heirarchy beneath the passed JMenu
	 * parameter, and for each "leaf node" (ie a menu item that is not a
	 * container for other menu items), add the Action and its menu path to the
	 * passed Map
	 * 
	 * @param nextMenuItem
	 *            the JMenu to recurse into
	 * @param menuPathBuff
	 *            a delimited String representation of the hierarchical "path"
	 *            to this menu item. This will be used as the key in the
	 *            actionsMap. For example, the "Graph Editor" menu item beneath
	 *            the "New" item on the "File" menu would have a menuPath of
	 *            File->New->Graph Editor. Delimeter is "->" (no quotes), and
	 *            spaces are allowed within menu text strings, but not around
	 *            the delimiters; i.e: New->Graph Editor is OK, but File ->New
	 *            is not.
	 * @param MENU_PATH_DELIMITER
	 *            String
	 * @param actionsMap
	 *            the Map containing key => value pairs of the form: menuPath
	 *            (as described above) => Action (the javax.swing.Action
	 *            assigned to this menu item)
	 */
	public static void storePTIIMenuItems(JMenuItem nextMenuItem,
			StringBuffer menuPathBuff, final String MENU_PATH_DELIMITER,
			Map<String, Action> actionsMap) {

		menuPathBuff.append(MENU_PATH_DELIMITER);
		if (nextMenuItem != null && nextMenuItem.getText() != null) {
			String str = nextMenuItem.getText();
			// do not make the recent files menu item upper case since
			// it contains paths in the file system.
			if (menuPathBuff.toString().startsWith("FILE->RECENT FILES->")) {
				menuPathBuff = new StringBuffer("File->Recent Files->");
			} else {
				str = str.toUpperCase();
			}
			menuPathBuff.append(str);			
		}

		if(isDebugging) {
			log.debug(menuPathBuff.toString());
		}
		// System.out.println(menuPathBuff.toString());

		if (nextMenuItem instanceof JMenu) {
			storePTIITopLevelMenus((JMenu) nextMenuItem,
					menuPathBuff.toString(), MENU_PATH_DELIMITER, actionsMap);
		} else {
			Action nextAction = nextMenuItem.getAction();
			// if there is no Action, look for an ActionListener
			// System.out.println("Processing menu " + nextMenuItem.getText());
			if (nextAction == null) {
				final ActionListener[] actionListeners = nextMenuItem
						.getActionListeners();
				// System.out.println("No Action for " + nextMenuItem.getText()
				// + "; found " + actionListeners.length
				// + " ActionListeners");
				if (actionListeners.length > 0) {
					if (isDebugging) {
						log.debug(actionListeners[0].getClass().getName());
					}
					// ASSUMPTION: there is only one ActionListener
					nextAction = new AbstractAction() {
						public void actionPerformed(ActionEvent a) {
							actionListeners[0].actionPerformed(a);
						}
					};
					// add all these values - @see diva.gui.GUIUtilities
					nextAction.putValue(Action.NAME, nextMenuItem.getText());
					// System.out.println("storing ptII action for menu " +
					// nextMenuItem.getText());
					nextAction.putValue(GUIUtilities.LARGE_ICON,
							nextMenuItem.getIcon());
					nextAction.putValue(GUIUtilities.MNEMONIC_KEY, new Integer(
							nextMenuItem.getMnemonic()));
					nextAction.putValue("tooltip",
							nextMenuItem.getToolTipText());
					nextAction.putValue(GUIUtilities.ACCELERATOR_KEY,
							nextMenuItem.getAccelerator());
					nextAction.putValue("menuItem", nextMenuItem);
				} else {
					if (isDebugging) {
						log.warn("No Action or ActionListener found for "
								+ nextMenuItem.getText());
					}
				}
			}
			if (!actionsMap.containsValue(nextAction)) {
				actionsMap.put(menuPathBuff.toString(), nextAction);
				if (isDebugging) {
					log.debug(menuPathBuff.toString() + " :: ACTION: "
							+ nextAction);
				}
			}
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////

	private JMenuBar createKeplerMenuBar(Map<String, Action> ptiiMenuActionsMap) {

		final LinkedHashMap<String, JMenuItem> keplerMenuMap = new LinkedHashMap<String, JMenuItem>();
		final JMenuBar _keplerMenubar = new JMenuBar();

		if(isDebugging) {
			log.debug("***************\nKEPLER MENUS:\n***************\n");
		}

		Iterator<ConfigurationProperty> it = null;
		try {

			ConfigurationProperty prop = ConfigurationManager.getInstance()
					.getProperty(ConfigurationManager.getModule("gui"),
							new ConfigurationNamespace("uiMenuMappings"));

			if (prop == null) {
				log.warn("Could not load uiMenuMappings configuration file.");
				return null;
			}

			List<ConfigurationProperty> reposList = prop.getProperties("name",
					true);
			it = reposList.iterator();

			if (it == null) {
				log.warn("Menu mappings do not contain any valid assignments");
				return null;
			}

			separatorJustAdded = false;

			while (it.hasNext()) {

				ConfigurationProperty cp = it.next();
				String nextKey = cp.getValue();
				String nextVal = cp.getParent().getProperty("value").getValue();
				ConfigurationProperty moduleProperty = cp.getParent()
						.getProperty("module");
				if (moduleProperty != null) {
					String nextModule = moduleProperty.getValue();
					ModuleTree mt = ModuleTree.instance();
					if (!mt.contains(nextModule)) {
						if(isDebugging) {
							log.debug("Skipping this entry (" + nextKey
								+ ") because '" + nextModule
								+ "' is not active");
						}
						continue;
					} else if(isDebugging) {
						log.debug("Processing this entry (" + nextKey
								+ ") explicitly because '" + nextModule
								+ "' is active");
					}
				} else if(isDebugging) {
					log.debug(nextKey + " has no module associated with it");
				}
				// String nextKey = (String) (it.next());

				if (isDebugging) {
					log.debug("nextKey: " + nextKey);
				}
				if (nextKey == null || nextKey.trim().length() < 1) {
					continue;
				}

				if (isDebugging) {
					log.debug("nextVal: " + nextVal);
				}

				if (nextVal == null || nextVal.trim().length() < 1) {
					log.warn("no menu mapping found for key: " + nextKey);
					continue;
				}

				Action action = null;

				if (nextKey.indexOf(MENU_SEPARATOR_KEY) < 0) {

					// if it's the recent files menu item, add all
					// the existing entries.
					// NOTE: we can't put the entries in the menu mappings
					// configuration file since they are dynamic.
					if (nextVal.equals("File->Recent Files")) {
						for (Map.Entry<String, Action> entry : ptiiMenuActionsMap
								.entrySet()) {
							if (entry.getKey().startsWith(
									"File->Recent Files->")) {
								addMenuFor(entry.getKey(), entry.getValue(),
										_keplerMenubar, keplerMenuMap);
								//System.out.println("adding recent entry: " + entry.getKey());
							}
						}
						continue;
					}

					action = getActionFor(nextVal, ptiiMenuActionsMap,
							_tableauFrameInstance);

					if (action == null) {
						if (isDebugging) {
							log.warn("null action for value " + nextVal);
						}
						continue;
					}
					// action exists, and it's not a separator
					// menuItemCount++;
				}

				addMenuFor(nextKey, action, _keplerMenubar, keplerMenuMap);
				// prevTopLevel
				// = _keplerMenubar.getMenu(_keplerMenubar.getMenuCount() -
				// 1).getText();
			}

		} catch (Exception ex) {
			if (isDebugging) {
				log.warn("Exception creating Kepler menus: " + ex
						+ "\nDefaulting to PTII menus");
				if (isDebugging) {
					ex.printStackTrace();
				}
				return _keplerMenubar;
			}
		}

		separatorJustAdded = false;

		if(isDebugging) {
			log.debug("***************\nEND KEPLER MENUS:\n***************\n");
		}
		return _keplerMenubar;
	}

	public Map<String, Action> getPTIIMenuActionsMap() {

		if (_ptiiMenubar == null) {
			_ptiiMenubar = _tableauFrameInstance.getJMenuBar();
		}

		if (_ptiiMenuActionsMap == null || _reloadPtolemyMenus) {
			if(isDebugging) {
				log.debug("**************\nEXISTING PTII MENUS:\n**************\n");
			}

			// NOTE: use a LinkedHashMap to preserve the order of the
			// recently opened files list
			_ptiiMenuActionsMap = new LinkedHashMap<String, Action>();

			for (int m = 0; m < _ptiiMenubar.getMenuCount(); m++) {
				JMenu nextMenu = _ptiiMenubar.getMenu(m);

				storePTIITopLevelMenus(nextMenu, nextMenu.getText()
						.toUpperCase(), MENU_PATH_DELIMITER,
						_ptiiMenuActionsMap);
			}
			_reloadPtolemyMenus = false;
			if(isDebugging) {
				log.debug("\n**************\nEND PTII MENUS:\n*****************\n");
			}
		}
		return _ptiiMenuActionsMap;
	}

	private static void storePTIITopLevelMenus(JMenu nextMenu, String menuPath,
			final String MENU_PATH_DELIMITER,
			Map<String, Action> ptiiMenuActionsMap) {

		int totMenuItems = nextMenu.getMenuComponentCount();

		for (int n = 0; n < totMenuItems; n++) {
			Component nextComponent = nextMenu.getMenuComponent(n);
			if (nextComponent instanceof JMenuItem) {
				storePTIIMenuItems((JMenuItem) nextComponent, new StringBuffer(
						menuPath), MENU_PATH_DELIMITER, ptiiMenuActionsMap);			
			}
			// (if it's not an instanceof JMenuItem, it must
			// be a separator, and can therefore be ignored)
		}
	}

	public static JMenuItem addMenuFor(String key, Action action,
			JComponent topLvlContainer, Map<String, JMenuItem> keplerMenuMap) {

		if (topLvlContainer == null) {
			if(isDebugging) {
				log.debug("NULL container received (eg JMenuBar) - returning NULL");
			}
			return null;
		}
		if (key == null) {
			if(isDebugging) {
				log.debug("NULL key received");
			}
			return null;
		}
		key = key.trim();

		if (key.length() < 1) {
			if(isDebugging) {
				log.debug("BLANK key received");
			}
			return null;
		}
		if (action == null && key.indexOf(MENU_SEPARATOR_KEY) < 0) {
			if (isDebugging) {
				log.debug("NULL action received, but was not a separator: "
						+ key);
			}
			return null;
		}

		if (keplerMenuMap.containsKey(key)) {
			if (isDebugging) {
				log.debug("Menu already added; skipping: " + key);
			}
			return null;
		}

		// split delimited parts and ensure menus all exist
		String[] menuLevel = key.split(MENU_PATH_DELIMITER);

		int totLevels = menuLevel.length;

		// create a menu for each "menuLevel" if it doesn't already exist
		final StringBuffer nextLevelBuff = new StringBuffer();
		String prevLevelStr = null;
		JMenuItem leafMenuItem = null;

		for (int levelIdx = 0; levelIdx < totLevels; levelIdx++) {

			// save previous value
			prevLevelStr = nextLevelBuff.toString();

			String nextLevelStr = menuLevel[levelIdx];
			// get the index of the first MNEMONIC_SYMBOL
			int mnemonicIdx = nextLevelStr.indexOf(MNEMONIC_SYMBOL);
			char mnemonicChar = 0;

			// if an MNEMONIC_SYMBOL exists, remove all underscores. Then, idx
			// of
			// first underscore becomes idx of letter it used to precede - this
			// is the mnemonic letter
			if (mnemonicIdx > -1) {
				nextLevelStr = nextLevelStr.replaceAll(MNEMONIC_SYMBOL, "");
				mnemonicChar = nextLevelStr.charAt(mnemonicIdx);
			}
			if (levelIdx != 0) {
				nextLevelBuff.append(MENU_PATH_DELIMITER);
			}
			nextLevelBuff.append(nextLevelStr);

			// don't add multiple separators together...
			if (nextLevelStr.indexOf(MENU_SEPARATOR_KEY) > -1) {
				if (separatorJustAdded == false) {

					// Check if we're at the top level, since this makes sense
					// only for
					// context menu - we can't add a separator to a JMenuBar
					if (levelIdx == 0) {
						if (topLvlContainer instanceof JContextMenu) {
							((JContextMenu) topLvlContainer).addSeparator();
						}
					} else {
						JMenu parent = (JMenu) keplerMenuMap.get(prevLevelStr);

						if (parent != null) {
							if (parent.getMenuComponentCount() < 1) {
								if(isDebugging) {
									log.debug("------ NOT adding separator to parent "
										+ parent.getText()
										+ ", since it does not contain any menu items");
								}
							} else {
								if(isDebugging) {
									log.debug("------ adding separator to parent "
										+ parent.getText());
								}
								// add separator to parent
								parent.addSeparator();
								separatorJustAdded = true;
							}
						}
					}
				}
			} else if (!keplerMenuMap.containsKey(nextLevelBuff.toString())) {
				// If menu has not already been created, we need
				// to create it and then add it to the parent level...

				JMenuItem menuItem = null;

				// if we're at a "leaf node" - need to create a JMenuItem
				if (levelIdx == totLevels - 1) {

					// save old display name to use as actionCommand on
					// menuitem,
					// since some parts of PTII still
					// use "if (actionCommand.equals("SaveAs")) {..." etc
					String oldDisplayName = (String) action
							.getValue(Action.NAME);

					// action.putValue(Action.NAME, nextLevelStr);

					if (mnemonicChar > 0) {
						action.putValue(GUIUtilities.MNEMONIC_KEY, new Integer(
								mnemonicChar));
					}

					// Now we look to see if it's a checkbox
					// menu item, or just a regular one
					String menuItemType = (String) (action
							.getValue(MENUITEM_TYPE));

					if (menuItemType != null
							&& menuItemType == CHECKBOX_MENUITEM_TYPE) {
						menuItem = new JCheckBoxMenuItem(action);
					} else {
						menuItem = new JMenuItem(action);
					}

					// --------------------------------------------------------------
					/** @todo - setting menu names - TEMPORARY FIX - FIXME */
					// Currently, if we use the "proper" way of setting menu
					// names -
					// ie by using action.putValue(Action.NAME, "somename");,
					// then
					// the name appears on the port buttons on the toolbar,
					// making
					// them huge. As a temporary stop-gap, I am just setting the
					// new
					// display name using setText() instead of
					// action.putValue(..,
					// but this needs to be fixed elsewhere - we want to be able
					// to
					// use action.putValue(Action.NAME (ie uncomment the line
					// above
					// that reads:
					// action.putValue(Action.NAME, nextLevelStr);
					// and delete the line below that reads:
					// menuItem.setText(nextLevelStr);
					// otherwise this may bite us in future...
					menuItem.setText(nextLevelStr);
					// --------------------------------------------------------------

					// set old display name as actionCommand on
					// menuitem, for ptii backward-compatibility
					menuItem.setActionCommand(oldDisplayName);

					// add JMenuItem to the Action, so it can be accessed by
					// Action code
					action.putValue(NEW_JMENUITEM_KEY, menuItem);
					leafMenuItem = menuItem;
				} else {
					// if we're *not* at a "leaf node" - need to create a JMenu
					menuItem = new JMenu(nextLevelStr);
					if (mnemonicChar > 0) {
						menuItem.setMnemonic(mnemonicChar);
					}
				}
				// level 0 is a special case, since the container (JMenuBar or
				// JContextMenu) is not a JMenu or a JMenuItem, so we can't
				// use the same code to add child to parent...
				if (levelIdx == 0) {
					if (topLvlContainer instanceof JMenuBar) {
						// this handles JMenuBar menus
						((JMenuBar) topLvlContainer).add(menuItem);
					} else if (topLvlContainer instanceof JContextMenu) {
						// this handles popup context menus
						((JContextMenu) topLvlContainer).add(menuItem);
					}
					// add to Map
					keplerMenuMap.put(nextLevelBuff.toString(), menuItem);
					separatorJustAdded = false;
				} else {
					JMenu parent = (JMenu) keplerMenuMap.get(prevLevelStr);
					if (parent != null) {
						// add to parent
						parent.add(menuItem);
						// add to Map
						keplerMenuMap.put(nextLevelBuff.toString(), menuItem);
						separatorJustAdded = false;
					} else {
						if (isDebugging) {
							log.debug("Parent menu is NULL" + prevLevelStr);
						}
					}
				}
			}
		}
		return leafMenuItem;
	}
	
	public void clear() {
        /*int removed =*/ MemoryCleaner.removeActionListeners(_ptiiMenubar);
        //System.out.println("MenuMapper menubar action listeners removed: " + removed);
        _ptiiMenuActionsMap.clear();
        _tableauFrameInstance = null;
        _mappers.remove(this);
	}
	
	/** Remap all open menubars. */
	public static void reloadAllMenubars() {
		for(MenuMapper mapper : _mappers) {
			mapper.reloadPtolemyMenus();
		}		
	}
	
	/** Remap the menu. */
	public void reloadPtolemyMenus() {
		_reloadPtolemyMenus = true;
		init();
		_tableauFrameInstance.validate();
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	private static final Log log = LogFactory
			.getLog(MenuMapper.class.getName());

	private static final boolean isDebugging = log.isDebugEnabled();

	private JMenuBar _ptiiMenubar;

	private TableauFrame _tableauFrameInstance;

	private Map<String, Action> _ptiiMenuActionsMap;

	private static boolean separatorJustAdded = false;

	// NOTE - MUST NOT contain the char defined in MNEMONIC_SYMBOL,
	// or the String defined as MENU_PATH_DELIMITER
	public static final String MENU_PATH_DELIMITER = "->";

	// NOTE - MUST NOT contain the String defined in MNEMONIC_SYMBOL,
	// or the String defined as MENU_PATH_DELIMITER
	public static final String MENU_SEPARATOR_KEY = "MENU_SEPARATOR";

	// NOTE - MUST NOT match the String defined as MENU_PATH_DELIMITER,
	// or the String defined as MENU_SEPARATOR_KEY
	public static final String MNEMONIC_SYMBOL = "~";
	
	/** A collection of all MenuMapper objects. */
	private static Set<MenuMapper> _mappers = Collections.synchronizedSet(new HashSet<MenuMapper>());
	
	/** If true, reload the Ptolemy menu. */
	private boolean _reloadPtolemyMenus = false;
}
