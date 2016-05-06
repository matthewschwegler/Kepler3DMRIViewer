/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2011-03-18 19:24:12 -0700 (Fri, 18 Mar 2011) $' 
 * '$Revision: 27324 $'
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;

public class ViewPaneFactory extends Attribute {

	private static final Log log = LogFactory.getLog(ViewPaneFactory.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public ViewPaneFactory(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Create and initialize the ViewPane for each known ViewPaneFactory.
	 * 
	 * @return True if successfully initialized, false otherwise.
	 */
	public boolean createViewPanes(TableauFrame parent) {
		boolean success = false;
		try {
			ConfigurationProperty guiProp = ConfigurationManager.getInstance()
					.getProperty(ConfigurationManager.getModule("gui"));
			ConfigurationProperty viewPaneProp = guiProp
					.getProperty("viewPaneFactory");
			Iterator<ConfigurationProperty> factories = viewPaneProp
					.getProperties().iterator();

			ViewManager m = ViewManager.getInstance();
			while (factories.hasNext()) {
				ConfigurationProperty prop = (ConfigurationProperty) factories
						.next();
				String classname = prop.getProperty("class").getValue();
				String panename = prop.getProperty("name").getValue();
				try {
					String tableau = prop.getProperty("tableau").getValue();
					if (!parent.getTableau().getClass().getName()
							.endsWith(tableau)) {
						continue;
					}
				} catch (NullPointerException ignored) {
				}

				if (isDebugging)
					log.debug("classname: " + classname);
				if (isDebugging)
					log.debug("panename: " + panename);

				ViewPaneFactory vpf = (ViewPaneFactory) parent
						.getConfiguration().getAttribute(this.getName());
				if (vpf != null) {
					vpf = (ViewPaneFactory) vpf.getAttribute(panename);
				}

				if (vpf == null) {
					Class factoryClass = Class.forName(classname);
					Class[] args = new Class[] { NamedObj.class, String.class };
					Constructor constructor = factoryClass.getConstructor(args);
					Object[] argsImpl = new Object[] { this, panename };
					vpf = (ViewPaneFactory) constructor.newInstance(argsImpl);
				}

				ViewPane vp = vpf.createViewPane(parent);
				vp.setParentFrame(parent);
				vp.initializeView();
				m.addViewPane(vp);
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
	 * */
	public ViewPane createViewPane(TableauFrame parent) {
		return null;
	}
}
