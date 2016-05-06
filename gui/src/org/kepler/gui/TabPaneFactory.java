/*
 * Copyright (c) 1997-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;

//////////////////////////////////////////////////////////////////////////
//// TableauFactory
/**
 * This class is an attribute that creates a LibraryPaneTab for the
 * TabbedLibraryPane.
 * 
 *@author Aaron Schultz
 */
public class TabPaneFactory extends Attribute {

	/**
	 * Create a factory with the given name and container.
	 * 
	 *@param container
	 *            The container.
	 *@param name
	 *            The name.
	 *@exception IllegalActionException
	 *                If the container is incompatible with this attribute.
	 *@exception NameDuplicationException
	 *                If the name coincides with an attribute already in the
	 *                container.
	 */
	public TabPaneFactory(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	/**
	 * 
	 * @param parent
	 **/
	public boolean createTabPaneTabs(TableauFrame parent) {
		boolean success = false;
		try {
			ConfigurationProperty guiProp = ConfigurationManager.getInstance()
					.getProperty(ConfigurationManager.getModule("gui"));
			ConfigurationProperty tabProp = guiProp
					.getProperty("tabPaneFactory");
			Iterator<ConfigurationProperty> factories = tabProp.getProperties()
					.iterator();

			TabManager m = TabManager.getInstance();
			while (factories.hasNext()) {
				ConfigurationProperty prop = factories.next();
				String classname = prop.getProperty("class").getValue();
				String tabname = prop.getProperty("name").getValue();

				TabPaneFactory tpf = (TabPaneFactory) parent.getConfiguration()
						.getAttribute(this.getName());
				if (tpf != null) {
					tpf = (TabPaneFactory) tpf.getAttribute(tabname);
				}

				if (tpf == null) {
					// get the factory with reflections
					Class factoryClass = Class.forName(classname);
					Class[] args = new Class[] { NamedObj.class, String.class };
					Constructor constructor = factoryClass.getConstructor(args);
					Object[] argsImpl = new Object[] { this, tabname };
					tpf = (TabPaneFactory) constructor.newInstance(argsImpl);
				}

				TabPane tp = tpf.createTabPane(parent);
				tp.setParentFrame(parent);
				tp.initializeTab();
				m.addTabPane(tp);
			}
			success = true;
		} catch (Exception e) {
			try {
				e.printStackTrace();
				MessageHandler.warning("Could not create tab pane.", e);
			} catch (Exception ce) {
				// Do nothing
			}
			success = false;
		}
		return success;
	}

	/**
	 * Always returns null, this method should be overridden by the factory
	 * instance that extends this factory.
	 * 
	 **/
	public TabPane createTabPane(TableauFrame parent) {
		System.out.println("WARNING: returning null in createTabPane.");
		return null;
	}
}