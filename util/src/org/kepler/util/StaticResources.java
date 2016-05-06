/*
 * Copyright (c) 2006-2010 The Regents of the University of California.
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

package org.kepler.util;

import java.io.IOException;
import java.util.List;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationNamespace;
import org.kepler.configuration.ConfigurationProperty;

//////////////////////////////////////////////////////////////////////////
//// StaticResources

/**
 *
 * Static resources for accessing ResourceBundles.
 *
 * @author Matthew Brooke
 * @version $Id: StaticResources.java 24000 2010-04-28 00:12:36Z berkley $
 * @since Ptolemy II 7.1
 * @Pt.ProposedRating
 * @Pt.AcceptedRating
 */
public class StaticResources {
	// kepler.gui.StaticGUIResources contains GUI specific code,
	// this class should _not_ import GUI code.

	// protected constructor - non-instantiable
	protected StaticResources() {
	}

	///////////////////////////////////////////////////////////////////
	////                       public methods                      ////
	///////////////////////////////////////////////////////////////////

	/**
	 * Search the uiDisplayText resourcebundle for the property specified by the
	 * key parameter. Return the boolean value of the property String
	 * corresponding to the specified key. If the property is not found, the
	 * defaultVal parameter is returned.
	 *
	 * @param key
	 *            the properties key identifying the value to be found
	 * @param defaultVal
	 *            - the default boolean value to be returned if the requested
	 *            property cannot be found or read
	 * @return boolean value of the property String corresponding to the
	 *         specified key. If the property is not found, the defaultVal
	 *         parameter is returned.
	 */
	public static boolean getBoolean(String key, boolean defaultVal) {

		boolean val = defaultVal;
		try {
			val = Boolean.valueOf(_getString(key, getUISettingsProperty()))
					.booleanValue();
		} catch (Exception ex) {
			if (_isDebugging) {
				System.out
						.println("StaticResources could not find the property for the key: "
								+ key
								+ "\n; returning default value: "
								+ defaultVal);
			}
		}
		return val;
	}

	/**
	 * Search the uiDisplayText resourcebundle for the property specified by the
	 * key parameter. Return the String value of the property value specified.
	 * If the property is not found, the default defaultString parameter is
	 * returned.
	 *
	 * @param key
	 *            the properties key for the String to be found
	 * @param defaultString
	 *            - the default String to be returned if the property cannot be
	 *            found or read
	 * @return String value associated with the specified key, or the
	 *         defaultString parameter if the property is not found.
	 */
	public static String getDisplayString(String key, String defaultString) {

		String result = null;
		try {
			result = _getString(key, getDisplayTextProperty());
		} catch (Exception ex) {
			if (_isDebugging) {
				System.out
						.println("StaticResources could not find String property for the key: "
								+ key
								+ "\n; returning default String: "
								+ defaultString);
			}
			return defaultString;
		}
		return result;
	}

	/**
	 * Search for the ResourceBundle containing the UI Display Text.
	 * @return The resource bundle that corresponds with
	 * {@link #UI_DISPLAY_TEXT_BUNDLE}
	 * @exception IOException If the bundle that corresponds with
	 * {@link #UI_DISPLAY_TEXT_BUNDLE} cannot be loaded.
	 */
	public static ConfigurationProperty getDisplayTextProperty()
			throws IOException {

		if (diplayTextProp == null) {
			ConfigurationNamespace ns = new ConfigurationNamespace(
					"uiDisplayText");
			diplayTextProp = ConfigurationManager.getInstance().getProperty(
					ConfigurationManager.getModule("gui"), ns);
		}
		return diplayTextProp;
	}

	/**
	 * Search the uiSettings resourcebundle for the property specified by the
	 * key parameter. Return the String value of the property value specified.
	 * If the property is not found, the default defaultString parameter is
	 * returned.
	 *
	 * @param key
	 *            the properties key for the String to be found
	 * @param defaultString
	 *            - the default String to be returned if the property cannot be
	 *            found or read
	 * @return String value associated with the specified key, or the
	 *         defaultString parameter if the property is not found.
	 */
	public static String getSettingsString(String key, String defaultString) {
		String result = null;
		try {
			result = _getString(key, getUISettingsProperty());
		} catch (Exception ex) {
			if (_isDebugging) {
				System.out
						.println("StaticResources could not find String property for the key: "
								+ key
								+ "\n; returning default String: "
								+ defaultString);
			}
			return defaultString;
		}
		return result;
	}

	/**
	 * Search the uiSettings resourcebundle for the size property specified by
	 * the sizeKey. Return the integer (int) value of the size specified. If the
	 * property is not found, the defaultSize parameter is returned.
	 *
	 * @param sizeKey
	 *            the properties key String for the size setting
	 * @param defaultSize
	 *            - the default size to be used if the property cannot be found
	 * @return integer (int) value of the size specified. If the property is not
	 *         found, the defaultSize parameter is returned.
	 */
	public static int getSize(String sizeKey, int defaultSize) {

		int size = 0;
		try {
			size = _getInt(sizeKey, getUISettingsProperty());
		} catch (Exception ex) {
			if (_isDebugging) {
				System.out
						.println("StaticResources could not find size property for the key: "
								+ sizeKey
								+ "\n; returning default size: "
								+ defaultSize);
			}
			return defaultSize;
		}
		return size;
	}

	/**
	 * Search for the ResourceBundle containing the ui settings.
	 * @return The resource bundle that corresponds with
	 * {@link #UI_SETTINGS_BUNDLE}
	 * @exception IOException If the bundle that corresponds with
	 * {@link #UI_SETTINGS_BUNDLE} cannot be loaded.
	 */
	public static ConfigurationProperty getUISettingsProperty()
			throws IOException {

		if (uiSettingsProp == null) {
			ConfigurationNamespace ns = new ConfigurationNamespace("uiSettings");
			uiSettingsProp = ConfigurationManager.getInstance().getProperty(
					ConfigurationManager.getModule("gui"), ns);
		}
		return uiSettingsProp;
	}

	/**
	 * get the configuration property for the svg icon class mappings
	 */
	public static ConfigurationProperty getUiSVGIconMappingsByClass() {
		if (uiSVGIconMappingsByClassProp == null) {
			ConfigurationNamespace ns = new ConfigurationNamespace(
					"uiSVGIconMappingsByClass");
			uiSVGIconMappingsByClassProp = ConfigurationManager.getInstance()
					.getProperty(ConfigurationManager.getModule("gui"), ns);
		}
		return uiSVGIconMappingsByClassProp;
	}

	/**
	 * get the configuraiton property for the svg icon lsid mappings
	 */
	public static ConfigurationProperty getUiSVGIconMappingsByLSID() {
		if (uiSVGIconMappingsByLSIDProp == null) {
			ConfigurationNamespace ns = new ConfigurationNamespace(
					"uiSVGIconMappingsByLSID");
			uiSVGIconMappingsByLSIDProp = ConfigurationManager.getInstance()
					.getProperty(ConfigurationManager.getModule("gui"), ns);
		}
		return uiSVGIconMappingsByLSIDProp;
	}

	/**
	 * get the value of a property based on the name
	 */
	public static String getValueFromName(ConfigurationProperty prop,
			String name) {
		//System.out.println("finding name=" + name + " in prop " + prop.getName());
		List l = prop.findProperties("name", name, true);
		if (l != null && l.size() > 0) {
			ConfigurationProperty p = (ConfigurationProperty) l.get(0);
			String value = p.getProperty("value").getValue();
			return value;
		}
		return null;
	}

	///////////////////////////////////////////////////////////////////
	////                      public variables                     ////
	///////////////////////////////////////////////////////////////////

	static {
		try {
			getUISettingsProperty();
		} catch (IOException ex) {
			// no worries - just try again when we actually need it
		}
		try {
			getDisplayTextProperty();
		} catch (IOException ex) {
			// no worries - just try again when we actually need it
		}
	}

	///////////////////////////////////////////////////////////////////
	////                      protected methods                      ////
	///////////////////////////////////////////////////////////////////

	/**
	 * Get the String property denoted by the propertyKey.
	 *
	 * @param propertyKey
	 *            the properties key String identifying the property
	 *
	 * @param property
	 *            the ConfigurationProperty in which to search
	 *
	 * @return the String value identified by the propertyKey
	 *
	 * @exception java.lang.Exception
	 *             if key is not found or cannot be read
	 */
	protected static String _getString(String propertyKey,
			ConfigurationProperty property) throws Exception {
		String s = getValueFromName(property, propertyKey);
		return s;
	}

	/**
	 * Get the integer (int) property denoted by the propertyKey.
	 *
	 * @param propertyKey
	 *            the properties key String identifying the property
	 *
	 * @param property
	 *            the ConfigurationProperty in which to search
	 *
	 * @return the int value identified by the propertyKey
	 *
	 * @exception Exception
	 *             if key is not found, cannot be read, or cannot be parsed as
	 *             an integer
	 */
	protected static int _getInt(String propertyKey,
			ConfigurationProperty property) throws Exception {
		String val = _getString(propertyKey, property);
		return new Integer(val).intValue();
	}

	//TODO: get this path out of the code.
	public final static String RESOURCEBUNDLE_DIR = "ptolemy/configs/kepler";

	///////////////////////////////////////////////////////////////////
	////                        protected variables                ////
	///////////////////////////////////////////////////////////////////

	/** Set to true and recompile for debugging such as error messages.*/
	protected final static boolean _isDebugging = false;

	///////////////////////////////////////////////////////////////////
	////                        public variables                  ////
	///////////////////////////////////////////////////////////////////

	private static ConfigurationProperty uiSettingsProp;
	private static ConfigurationProperty diplayTextProp;
	private static ConfigurationProperty uiSVGIconMappingsByClassProp;
	private static ConfigurationProperty uiSVGIconMappingsByLSIDProp;
}