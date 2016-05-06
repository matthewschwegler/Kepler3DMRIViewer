/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: tao $'
 * '$Date: 2011-08-05 13:57:09 -0700 (Fri, 05 Aug 2011) $' 
 * '$Revision: 28219 $'
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

import java.awt.Color;
import java.awt.Component;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ptolemy.actor.gui.TableauFrame;

/**
 * This singleton class can be used to hold references to java components that
 * implement the TabPane interface so they can be accessed and controlled from
 * anywhere. Add methods to this class as you need them.
 */
public class TabManager {

	private Vector<TabPane> _tabPanes;

	private Vector<TabPaneActionListener> _tabPaneListeners;

	private static final Log log = LogFactory
			.getLog(TabManager.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * Due to slight color change in the background area of a tab this color
	 * should be used to set the background color of components inside the tab.
	 */
	//public static final Color BGCOLOR = new Color(221, 221, 221);
	public static final Color BGCOLOR = SystemColor.window;

	// public static final Color BGCOLOR = new Color(255, 255, 255);

	/**
	 * Constructor.
	 */
	protected TabManager() {
		_tabPanes = new Vector<TabPane>();
		_tabPaneListeners = new Vector<TabPaneActionListener>();
	}

	/**
	 * Instantiate all of the TabPanes that are specified in configuration.xml
	 * 
	 * @param parent
	 */
	public void initializeTabs(TableauFrame parent) {
		try {
			TabPaneFactory TPfactory = (TabPaneFactory) parent
					.getConfiguration().getAttribute("tabPaneFactory");
			if (TPfactory == null) {
				TPfactory = new TabPaneFactory(parent.getConfiguration(),
						"tabPaneFactory");
			}
			if (TPfactory != null) {
				boolean success = TPfactory.createTabPaneTabs(parent);
				if (!success) {
					System.out
							.println("error: TabPane is null.  "
									+ "This "
									+ "problem can be fixed by adding a tabPaneFactory "
									+ "property in the configuration.xml file.");
				}
			} else {
				System.out.println("error: TabPane is " + "null.  This "
						+ "problem can be fixed by adding a tabPaneFactory "
						+ "property in the configuration.xml file.");
			}
		} catch (Exception e) {
			System.out.println("Error creating the tabpanefactory: "
					+ e.getMessage());
			e.printStackTrace();
		}

	}

	public void removeAllFrameTabs(TableauFrame parent) {
		for (int i = 0; i < _tabPanes.size(); i++) {
			TabPane tabPane = _tabPanes.elementAt(i);
			TableauFrame tableauFrame = tabPane.getParentFrame();
			if (tableauFrame == parent) {
				if (isDebugging)
					log.debug("tabmanager removing at " + i);
				tabPane.setParentFrame(null);
				_tabPanes.removeElementAt(i);
				i--;
			}
		}
		for (int i = 0; i < _tabPaneListeners.size(); i++) {
			if (_tabPaneListeners.get(i).getParentFrame() == parent) {
				_tabPaneListeners.removeElementAt(i);
				i--;
			}
		}
	}

	public void removeFrameTab(TableauFrame parent, String name) {
		for (int i = 0; i < _tabPanes.size(); i++) {
			TabPane tabPane = _tabPanes.elementAt(i);
			TableauFrame tableauFrame = tabPane.getParentFrame();
			if (tableauFrame == parent) {
				String tabName = tabPane.getTabName();
				if (tabName.equals(name)) {
					tabPane.setParentFrame(null);
					_tabPanes.removeElementAt(i);
					i--;
				}
			}
		}

	}

	/**
	 * Return a vector of tab pane objects for the specified TableauFrame.
	 * 
	 * @param parent
	 * */
	public Vector<TabPane> getFrameTabs(TableauFrame parent) {
		Vector<TabPane> frameTabPanes = new Vector<TabPane>();
		for (int i = 0; i < _tabPanes.size(); i++) {
			if (_tabPanes.elementAt(i).getParentFrame() == parent) {
				frameTabPanes.add(_tabPanes.elementAt(i));
			}
		}
		return frameTabPanes;
	}

	/**
	 * Register a TabPane with the TabManager. TabPanes must be subclasses of
	 * java.awt.Component
	 * 
	 * @param tp
	 * @throws ClassCastException
	 */
	public void addTabPane(TabPane tp) throws ClassCastException {
		if (tp instanceof Component) {
			_tabPanes.add(tp);
		} else {
			throw new ClassCastException(tp.getTabName()
					+ " TabPane must be a subclass of java.awt.Component");
		}
	}

	public boolean tabExists(TableauFrame parent, String tabName) {
		boolean exists = false;
		tabName = tabName.trim();
		for (int i = 0; i < _tabPanes.size(); i++) {
			if (_tabPanes.elementAt(i).getParentFrame() == parent) {
				if (_tabPanes.elementAt(i).getTabName().equals(tabName)) {
					exists = true;
				}
			}
		}
		return exists;
	}

	public TabPane getTab(TableauFrame parent, String tabName) {
		tabName = tabName.trim();
		for (int i = 0; i < _tabPanes.size(); i++) {
			if (_tabPanes.elementAt(i).getParentFrame() == parent) {
				if (_tabPanes.elementAt(i).getTabName().equals(tabName)) {
					return _tabPanes.elementAt(i);
				}
			}
		}
		return null;
	}

	/**
	 * returns the FIRST tab matching the given class param
	 * 
	 * @param parent
	 * @param tabClass
	 * @return
	 */
	public TabPane getTab(TableauFrame parent, Class tabClass) {
		for (int i = 0; i < _tabPanes.size(); i++) {
			if (_tabPanes.elementAt(i).getParentFrame() == parent) {
				if (tabClass.isInstance(_tabPanes.elementAt(i))) {
					return _tabPanes.elementAt(i);
				}
			}
		}
		return null;
	}

	public void addTabPaneListener(TableauFrame parent,
			TabPaneActionListener tpl) {
		_tabPaneListeners.add(tpl);
	}

	public void removeTabPaneListener(TabPaneActionListener tpl) {
		_tabPaneListeners.remove(tpl);
	}

	public void tabEvent(TableauFrame parent, ActionEvent ae) {
		for (int i = 0; i < _tabPaneListeners.size(); i++) {
			if (_tabPaneListeners.get(i).getParentFrame() == parent) {
				_tabPaneListeners.get(i).actionPerformed(ae);
			}
		}
	}

	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static TabManager getInstance() {
		return TabManagerHolder.INSTANCE;
	}

	private static class TabManagerHolder {
		private static final TabManager INSTANCE = new TabManager();
	}

}
