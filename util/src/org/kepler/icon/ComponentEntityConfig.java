/*
 * Copyright (c) 1997-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-03-27 11:47:49 -0700 (Wed, 27 Mar 2013) $' 
 * '$Revision: 31770 $'
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

package org.kepler.icon;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationNamespace;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.cache.CacheException;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.StaticResources;

import ptolemy.actor.CompositeActor;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.ConfigurableAttribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.util.MessageHandler;

//////////////////////////////////////////////////////////////////////////
//// ComponentEntityConfig

/**
 * Configuration Utilities for ComponentEntity class, which is a base class for
 * all actors.
 * 
 * @author Matthew Brooke
 * @version $Id: ComponentEntityConfig.java 31770 2013-03-27 18:47:49Z crawl $
 * @since Ptolemy II 0.2
 * @Pt.ProposedRating
 * @Pt.AcceptedRating
 */
public class ComponentEntityConfig {

	/**
	 * attribute name for attribute added to actors to describe
	 * (batik-renderable) SVG image file location
	 */
	public static final String SVG_ICON_ATTRIB_NAME = "_svgIcon";

	/**
	 * attribute name for attribute added to actors to describe raster thumbnail
	 * image file location
	 */
	public static final String RASTER_THUMB_ATTRIB_NAME = "_thumbnailRasterIcon";

	// /////////////////////////////////////////////////////////////////

	/**
	 * Key for finding default actor-icon name in resource bundle (example value
	 * associated with this key: "basic-actor")
	 */
	private static final String DEFAULT_ACTOR_ICON_KEY = "DEFAULT_ACTOR_ICON";

	/**
	 * Key for finding default director-icon name in resource bundle (example
	 * value associated with this key: "director")
	 */
	private static final String DEFAULT_DIRECTOR_ICON_KEY = "DEFAULT_DIRECTOR_ICON";

	private static String DEFAULT_ACTOR_ICON_BASENAME;
	private static String DEFAULT_DIRECTOR_ICON_BASENAME;

	private static Log log;
	private static boolean isDebugging;

	static {
		// logging...
		log = LogFactory.getLog("SVG." + ComponentEntityConfig.class.getName());
		isDebugging = log.isDebugEnabled();

		try {
			DEFAULT_ACTOR_ICON_BASENAME = StaticResources.getSettingsString(
					DEFAULT_ACTOR_ICON_KEY, null);
		} catch (Exception ex) {
			if (isDebugging) {
				log.debug("EXCEPTION GETTING DEFAULT_ACTOR_ICON_BASENAME: "
						+ ex.getMessage());
			}
		}
		try {
			DEFAULT_DIRECTOR_ICON_BASENAME = StaticResources.getSettingsString(
					DEFAULT_DIRECTOR_ICON_KEY, null);
		} catch (Exception ex) {
			if (isDebugging) {
				log.debug("EXCEPTION GETTING DEFAULT_DIRECTOR_ICON_BASENAME: "
						+ ex.getMessage());
			}
		}
	}

	private static final String PERIOD = ".";

	// /////////////////////////////////////////////////////////////////
	// // public methods ////
	// /////////////////////////////////////////////////////////////////

	public static synchronized void addSVGIconTo(NamedObj namedObj)
			throws IOException, IllegalArgumentException,
			NameDuplicationException, IllegalActionException {
		if (isDebugging)
			log.debug("addSVGIconTo(" + namedObj + ")");

		if (StaticResources.getUISettingsProperty() == null) {
			throw new IOException("Could not access actor icon mappings.");
		}
		if (namedObj == null) {
			throw new IllegalArgumentException(
					"addSVGIconTo() received NULL arg");
		}

		// try to assign an icon to this object based on it's LSID
		boolean assigned = tryToAssignIconByLSID(namedObj);
		if (assigned) {
			return;
		}

		// try to get a generic icon definition from byClass resourcebundle:
		String iconBaseName = tryToAssignIconByClass(namedObj);

		// if we could not find an icon using the lsid or class, try
		// the semantic type
        if (iconBaseName == null || iconBaseName.trim().length() < 1) {
            iconBaseName = tryToAssignIconBySemanticType(namedObj);
        }

        // get a default icon
		if (iconBaseName == null || iconBaseName.trim().length() < 1) {
			iconBaseName = getDefaultIconBaseName(namedObj);
		}

		if (iconBaseName == null) {
			if (isDebugging) {
				log.warn("Couldn't assign batik icon for: "
						+ namedObj.getClassName());
			}
			return;
		}
		addRasterAndSVGAttributes(namedObj, iconBaseName);
	}

	/**
	 * Look for an LSID in an Attribute, and if it exists, try to find an icon
	 * mapping in the appropriate resource bundle
	 * 
	 * @param container
	 *            NamedObj
	 * @throws IOException
	 * @throws NameDuplicationException
	 * @throws IllegalActionException
	 */
	public static boolean tryToAssignIconByLSID(NamedObj container)
			throws IOException, NameDuplicationException,
			IllegalActionException {
		boolean success = false;

		if (container == null) {
			return success;
		}

		NamedObjId lsid = NamedObjId.getIdAttributeFor(container);
		String iconBaseName = null;
		if (lsid != null) {

			if (isDebugging) {
				log.debug("\n\n*** FOUND LSID (" + lsid.getExpression()
						+ ") for: " + container.getClassName());
			}
			try {

				iconBaseName = StaticResources.getValueFromName(
						getByLSIDResBundle(), lsid.getExpression());
				if (isDebugging) {
					log.debug("*** icon (" + iconBaseName
							+ ") selected by LSID (" + lsid.getExpression()
							+ ") for: " + container.getClassName());
				}
			} catch (Exception ex) {
				log.debug(ex.getMessage());
				iconBaseName = null;
			}
		}

		if (iconBaseName == null) {
			return success;
		}

		addRasterAndSVGAttributes(container, iconBaseName);
		return success;
	}

	/**
	 * Try to find an icon by class using mapping in the appropriate resource
	 * bundle
	 * 
	 * @param container
	 *            NamedObj
	 * @throws IOException
	 * @throws NameDuplicationException
	 * @throws IllegalActionException
	 */
	public static String tryToAssignIconByClass(NamedObj namedObj)
			throws IOException {

		return tryToAssignIconByClassName(namedObj, namedObj.getClassName());

	}

	public static String tryToAssignIconByClassName(NamedObj namedObj,
			String className) throws IOException {
		String iconBaseName = null;
		if (getByClassResBundle() != null) {
			try {
				iconBaseName = StaticResources.getValueFromName(
						getByClassResBundle(), className);
				addRasterAndSVGAttributes(namedObj, iconBaseName);
				if (isDebugging) {
					log.debug("icon (" + iconBaseName
							+ ") selected by ClassName for: "
							+ namedObj.getClassName());
				}
			} catch (Exception ex1) {
				//ex1.printStackTrace();
				log.debug(ex1.getMessage());
				iconBaseName = null;
			}
		}
		return iconBaseName;
	}
	
	/** Try to assign the icon based on the semantic type. */
	public static String tryToAssignIconBySemanticType(NamedObj namedObj) {
	    
       if (_iconForSemanticTypeProp == null) {
            ConfigurationNamespace ns = new ConfigurationNamespace(
                    "uiSVGIconMappingsBySemanticType");
            _iconForSemanticTypeProp = ConfigurationManager.getInstance()
                    .getProperty(ConfigurationManager.getModule("gui"), ns);
            if(_iconForSemanticTypeProp == null) {
                MessageHandler.error("Could not find uiSVGIconMappingsBySemanticType.xml.");
                return null;
            }
       }
       
       String iconBaseName = null;
       
       CacheManager cacheManager = null;
       try {
           cacheManager = CacheManager.getInstance();
       } catch(CacheException e) {
           MessageHandler.error("Error accessing cache manager.", e);
           return null;
       }

       KeplerLSID lsid = null;
       // try to get the lsid from the object
       NamedObjId namedObjId = NamedObjId.getIdAttributeFor(namedObj);
       if(namedObjId == null) {
           // try to get the lsid from the cache based on the class name
           List<KeplerLSID> lsids = null;
        try {
            lsids = cacheManager.getCachedLsidsForClass(namedObj.getClassName());
        } catch (Exception e) {
            System.err.println("Exception query cache for LSIDs of " + namedObj.getClassName());
            return null;
        }
           if(lsids != null && !lsids.isEmpty()) {
               lsid = lsids.get(0);
           }
       } else {
           lsid = namedObjId.getId();
       }
           
       // if we found the lsid, get the semantic types
       if(lsid != null) {
           List<KeplerLSID> semanticTypes = cacheManager.getSemanticTypesFor(lsid);
           for(KeplerLSID semanticType : semanticTypes) {
               iconBaseName = StaticResources.getValueFromName(_iconForSemanticTypeProp, semanticType.toString());
               if(iconBaseName != null) {
                   return iconBaseName;
               }
           }
       }
	   return null;
	}

	// /////////////////////////////////////////////////////////////////
	// // private methods ////
	// /////////////////////////////////////////////////////////////////

	private static void addRasterAndSVGAttributes(NamedObj namedObj,
			String iconBaseName) throws IllegalActionException,
			NameDuplicationException, IOException {

		// If iconBaseName is null or empty, just abort; either a previously-
		// assigned icon, or default icon from Ptolemy base classes will be
		// used.
		if (iconBaseName == null || iconBaseName.trim().length() < 1) {
			if (isDebugging) {
				log.debug("Using base class default icon for: "
						+ namedObj.getClassName());
			}
			return;
		} else {
			if (isDebugging) {
				log.debug("*** FOUND ICON MAPPING (" + iconBaseName + ") FOR: "
						+ namedObj.getClassName());
			}
		}

		// gets here only if iconBaseName is not null or empty...

		// add base SVG icon
		_addIconAttributeToNamedObj(namedObj, SVG_ICON_ATTRIB_NAME,
				"SVG_ICON_BASE_PATH", iconBaseName, "SVG_BASE_ICON_EXT");

		// now add raster thumbnail icon
		_addIconAttributeToNamedObj(namedObj, RASTER_THUMB_ATTRIB_NAME,
				"RASTER_THUMBNAIL_BASE_PATH", iconBaseName,
				"RASTER_THUMBNAIL_EXT");
	}

	private static String getDefaultIconBaseName(NamedObj namedObj) {

		String iconBaseName = null;

		//System.out.println("using default icon for " + namedObj.getFullName());
		
		if (namedObj instanceof ptolemy.actor.Director) {
			if (isDebugging) {
				log.debug("getDefaultIconBaseName() found a DIRECTOR: "
						+ namedObj.getClassName());
			}
			iconBaseName = DEFAULT_DIRECTOR_ICON_BASENAME;
		} else if (namedObj instanceof CompositeActor) {
		    iconBaseName = "composite";
		} else if (!(namedObj instanceof Attribute)) {
			if (isDebugging) {
				log.debug("getDefaultIconBaseName() found an ACTOR: "
						+ namedObj.getClassName());
			}
			iconBaseName = DEFAULT_ACTOR_ICON_BASENAME;
		}
		return iconBaseName;
	}

	private static void _addIconAttributeToNamedObj(NamedObj namedObj,
			String attribName, String iconBasePathKey, String iconBaseName,
			String iconExtensionKey) throws IOException,
			NameDuplicationException, IllegalActionException {

		if (isDebugging) {
			log.debug("_addIconAttributeToNamedObj() received: "
					+ "\n namedObj:" + namedObj.getClassName()
					+ "\n attribName:" + attribName + "\n iconBasePathKey:"
					+ iconBasePathKey + "\n iconBaseName:" + iconBaseName
					+ "\n iconExtensionKey:" + iconExtensionKey);
		}

		String iconBasePath = StaticResources.getSettingsString(
				iconBasePathKey, null);
		if (iconBasePath == null) {
			throw new IOException("Could not get icon base path (key = "
					+ iconBasePathKey + ") from configuration file");
		}
		String iconExtension = StaticResources.getSettingsString(
				iconExtensionKey, null);
		if (iconExtension == null) {
			throw new IOException("Could not get icon extension (key = "
					+ iconExtensionKey + ") from configuration file");
		}

		// make sure we haven't already got an attribute with this name; if we
		// have, get it instead of trying to create a new one
		ConfigurableAttribute svgIconAttrib = getExistingOrNewAttribute(
				namedObj, attribName);

		StringBuffer buff = new StringBuffer(iconBasePath);

		// ensure last char of iconBasePath is a slash:
		if (!iconBasePath.endsWith("/")) {
			buff.append("/");
		}

		buff.append(iconBaseName.trim());

		// ensure iconExtension contains a ".":
		if (iconExtension.indexOf(PERIOD) < 0) {
			buff.append(PERIOD);
		}

		buff.append(iconExtension);
		if (isDebugging) {
			log.debug("_addIconAttributeToNamedObj(): " + "\n actor is: "
					+ namedObj.getClassName() + "\n attribute name: "
					+ attribName + "\n setExpression:" + buff.toString());
		}
		svgIconAttrib.setExpression(buff.toString());

		// set non-persistent so it doesn't get saved in MOML
		svgIconAttrib.setPersistent(false);

		if (isDebugging) {
			log.debug("from actual svgIconAttrib: "
					+ "\n svgIconAttrib.getExpression() = "
					+ svgIconAttrib.getExpression()
					+ "\n svgIconAttrib.getContainer() = "
					+ svgIconAttrib.getContainer().getClass().getName());
		}
	}

	private static ConfigurableAttribute getExistingOrNewAttribute(
			NamedObj namedObj, String attribName) {

		ConfigurableAttribute attrib = null;
		try {
			attrib = (ConfigurableAttribute) namedObj.getAttribute(attribName);
		} catch (Exception ex) {
			ex.printStackTrace();
			if (isDebugging) {
				log.warn(namedObj.getClass().getName()
						+ ") : exception getting svgIcon attribute: "
						+ attribName + "; exception was: " + ex.getMessage()
						+ "\n\n");
			}
			attrib = null;
		}
		if (attrib != null) {
			return attrib;
		}

		try {
			// the Attribute is automatically added to namedObj as an attribute,
			// simply by passing namedObj to the constructor
			attrib = new ConfigurableAttribute(namedObj, attribName);
		} catch (Exception ex) {
			ex.printStackTrace();
			if (isDebugging) {
				log.warn(namedObj.getClass().getName()
						+ ") : exception getting svgIcon attribute: "
						+ attribName + "; exception was: " + ex.getMessage()
						+ "\n\n");
			}
			attrib = null;
		}
		return attrib;
	}

	private static ConfigurationProperty getByClassResBundle()
			throws IOException {

		//System.out.println("byClassProperty: " + byClassProperty);
		if (byClassProperty == null) {
			byClassProperty = StaticResources.getUiSVGIconMappingsByClass();
		}
		return byClassProperty;
	}

	private static ConfigurationProperty getByLSIDResBundle()
			throws IOException {

		if (byLSIDProperty == null) {
			byLSIDProperty = StaticResources.getUiSVGIconMappingsByLSID();
		}
		return byLSIDProperty;
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////
	// /////////////////////////////////////////////////////////////////

	private static ConfigurationProperty byClassProperty;
	private static ConfigurationProperty byLSIDProperty;
	private static ConfigurationProperty _iconForSemanticTypeProp;
}