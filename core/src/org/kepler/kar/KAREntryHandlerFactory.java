/**
 *  '$RCSfile$'
 *  '$Author: welker $'
 *  '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $'
 *  '$Revision: 24234 $'
 *
 *  For Details:
 *  http://www.kepler-project.org
 *
 *  Copyright (c) 2010 The Regents of the
 *  University of California. All rights reserved. Permission is hereby granted,
 *  without written agreement and without license or royalty fees, to use, copy,
 *  modify, and distribute this software and its documentation for any purpose,
 *  provided that the above copyright notice and the following two paragraphs
 *  appear in all copies of this software. IN NO EVENT SHALL THE UNIVERSITY OF
 *  CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 *  OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
 *  DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY
 *  DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE
 *  SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 */

package org.kepler.kar;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.cache.CacheManager;

import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;

public class KAREntryHandlerFactory extends Attribute {

	private static final Log log = LogFactory
			.getLog(KAREntryHandlerFactory.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * Constructor
	 * 
	 * @param container
	 * @param name
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public KAREntryHandlerFactory(NamedObj container, String name)
			throws IllegalActionException, NameDuplicationException {
		super(container, name);
		if (isDebugging)
			log.debug("Construct " + this.getClassName());
	}

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Get all the KAREntryHandlers that are specified in the configuration and
	 * add them to the CacheManager.
	 */
	public boolean registerKAREntryHandlers() {
		if (isDebugging)
			log.debug("registerKAREntryHandlers()");
		boolean success = false;
		try {
			// Iterator<KAREntryHandlerFactory> factories = attributeList(
			// KAREntryHandlerFactory.class).iterator();
			CacheManager c = CacheManager.getInstance();

			ConfigurationProperty coreProp = ConfigurationManager.getInstance()
					.getProperty(ConfigurationManager.getModule("core"));
			ConfigurationProperty kehProp = coreProp
					.getProperty("karEntryHandlerFactory");

			List<ConfigurationProperty> handlers = kehProp.getProperties("karHandler");
			for (ConfigurationProperty handler : handlers) {
				String classname = handler.getProperty("class").getValue();
				String handlername = handler.getProperty("name").getValue();
				if(this.getAttribute(handlername) != null)
				{ //this handler has already been added
				  return true;
				}
				Class factoryClass = Class.forName(classname);
				Class[] args = new Class[] { NamedObj.class, String.class };
				Constructor constructor = factoryClass.getConstructor(args);
				Object[] argsImpl = new Object[] { this, handlername };
				KAREntryHandlerFactory kef = (KAREntryHandlerFactory) constructor
						.newInstance(argsImpl);

				// KAREntryHandlerFactory factory = factories.next();
				if (isDebugging)
					log.debug(kef.toString());
				KAREntryHandler keh = kef.createKAREntryHandler();
				keh.initialize();
				c.addKAREntryHandler(keh);
			}
			success = true;
		} 
		catch (Exception e) 
		{
			try {
				MessageHandler.warning("Could not create KAREntryHandler.", e);
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
	public KAREntryHandler createKAREntryHandler() {
		return null;
	}
}
