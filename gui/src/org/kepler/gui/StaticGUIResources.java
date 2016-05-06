/*
 * Copyright (c) 2006-2010 The Regents of the University of California.
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

import java.awt.Color;
import java.awt.Dimension;
import java.util.MissingResourceException;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.kepler.configuration.ConfigurationProperty;
import org.kepler.util.StaticResources;


//////////////////////////////////////////////////////////////////////////
//// StaticResources

/**
 * 
 * Static resources for accessing ResourceBundles etc.
 * <p>
 * FIXME: this class imports awt classes, so it should not be in kernel.util.
 * 
 * @author Matthew Brooke
 * @version $Id: StaticGUIResources.java 24234 2010-05-06 05:21:26Z welker $
 * @since Ptolemy II 7.2
 * @Pt.ProposedRating
 * @Pt.AcceptedRating
 */

public class StaticGUIResources extends StaticResources {
    
    // private constructor - non-instantiable
    private StaticGUIResources() {
    }

    ///////////////////////////////////////////////////////////////////
    ////                       public methods                      ////
    ///////////////////////////////////////////////////////////////////
    
    /**
     * Search the uiSettings resourcebundle for the 3 color components specified
     * by the redComponent, greenComponent and blueComponent properties. Return
     * a java.awt.Color object representing the color specified. If any of the 3
     * properties are not found, null is returned
     * 
     * @param redComponent
     *            String the properties key String for the red component
     * 
     * @param greenComponent
     *            String the properties key String for the green component
     * 
     * @param blueComponent
     *            String the properties key String for the blue component
     * 
     * @return a java.awt.Color object representing the color specified. If any
     *         of the 3 properties are not found, null is returned
     */
    public static Color getColor(String redComponent, String greenComponent,
            String blueComponent) {
        int red = 0;
        int green = 0;
        int blue = 0;
        try {
            red = _getInt(redComponent, getUISettingsProperty());
            green = _getInt(greenComponent, getUISettingsProperty());
            blue = _getInt(blueComponent, getUISettingsProperty());
        } catch (Exception ex) {
            if (_isDebugging) {
                System.out
                        .println("StaticResources could not find Color component(s) for the keys:\n "
                                + redComponent
                                + ", "
                                + greenComponent
                                + " and/or "
                                + blueComponent
                                + "\n; returning NULL!");
            }
            return null;
        }
        return new Color(red, green, blue);
    }

    /**
     * Search the uiSettings resourcebundle for the width and height specified
     * by the widthKey and heightKey properties. Return a java.awt.Dimension
     * object with the width and height specified . If either or both of the
     * properties are not found, a Dimension object is returned with the width
     * and height specified by the defaultWidth and defaultHeight parameters.
     * This method should never return null
     * 
     * @param widthKey
     *            the properties key String for the width setting
     * 
     * @param heightKey
     *            the properties key String for the height setting
     * 
     * @param defaultWidth
     *            int - the default width to be used if the property cannot be
     *            found
     * 
     * @param defaultHeight
     *            int - the default height to be used if the property cannot be
     *            found
     * 
     * @return Dimension object with the width and height specified by the
     *         widthKey and heightID properties in the uiSettings
     *         resourcebundle. If either or both of the properties are not
     *         found, a Dimension object is returned with the width and height
     *         specified by the defaultWidth and defaultHeight parameters. This
     *         method should never return null
     */
    public static Dimension getDimension(String widthKey, String heightKey,
            int defaultWidth, int defaultHeight) {

        int width = 0;
        int height = 0;
        try {
            width = _getInt(widthKey, getUISettingsProperty());
            height = _getInt(heightKey, getUISettingsProperty());
        } catch (Exception ex) {
            if (_isDebugging) {
                System.out
                        .println("StaticResources could not find Dimension(s) for the keys "
                                + widthKey
                                + " and/or "
                                + heightKey
                                + "\n; returning default dimensions: "
                                + defaultWidth + " x " + defaultHeight);
            }
            return new Dimension(defaultWidth, defaultHeight);
        }
        return new Dimension(width, height);
    }
    
    /**
     * Get the platform on which this application is running.
     * 
     * @return one of the following positive int values representing the
     *         platform: StaticGUIResource.WINDOWS StaticGUIResource.MAC_OSX
     *         StaticGUIResource.LINUX or -1 if the platform is unknown
     */
    public static int getPlatform() {
        return _platform;
    }

   
    public static short getSVGRenderingMethod() {

        if (svgRenderingMethod == SVG_RENDERING_NOT_SET) {

            System.out
                    .println("*** Attempting to get ResourceBundle for SVG defaults ***");
            ConfigurationProperty defaultsBundle = null;
            try {
                defaultsBundle = getUISettingsProperty();
            } catch (Exception ex) {
                if (_isDebugging) {
                    System.out.println("Exception getting defaultsBundle: "
                            + ex + "\nDefaulting to DIVA rendering");
                }
                svgRenderingMethod = SVG_DIVA_RENDERING;
                return svgRenderingMethod;
            }

            if (defaultsBundle == null) {
                if (_isDebugging) {
                    System.out
                            .println("defaultsBundle==null; Defaulting to DIVA rendering");
                }
                svgRenderingMethod = SVG_DIVA_RENDERING;
                return svgRenderingMethod;
            }

            String isBatikStr = null;
            try {
                isBatikStr = getSettingsString("SVG_RENDERING_IS_BATIK", null);
            } catch (MissingResourceException mre) {
                if (_isDebugging) {
                    System.out.println("MissingResourceException getting "
                            + "SVG_RENDERING_IS_BATIK"
                            + "\nDefaulting to DIVA rendering");
                }
                svgRenderingMethod = SVG_DIVA_RENDERING;
                return svgRenderingMethod;
            }

            if (isBatikStr != null
                    && isBatikStr.trim().equalsIgnoreCase("true")) {
                svgRenderingMethod = SVG_BATIK_RENDERING;
                System.out
                        .println("*** svgRenderingMethod = SVG_BATIK_RENDERING ***");
            } else {
                svgRenderingMethod = SVG_DIVA_RENDERING;
                System.out
                        .println("*** svgRenderingMethod = SVG_DIVA_RENDERING ***");
            }
        }
        return svgRenderingMethod;
    }
    
    /**
     * Set the look & feel - first check if a user-specified L&F exists in the file
     * whose path is obtained from StaticResources.UI_SETTINGS_BUNDLE. If not,
     * use the default platform L&F.
     */
    public static void setLookAndFeel() {

        // override ptii look & feel
        String propsLNF = null;
        String lnfClassName = null;
        try {
            //ResourceBundle uiSettingsBundle = ResourceBundle
            //        .getBundle(StaticResources.UI_SETTINGS_BUNDLE);
            lnfClassName = UIManager.getSystemLookAndFeelClassName();

            if (lnfClassName.indexOf("windows") > -1
                    || lnfClassName.indexOf("Windows") > -1) {
                _platform = WINDOWS;
                propsLNF = getSettingsString("WINDOWS_LNF", "\n\r");
            } else if (lnfClassName.indexOf("apple") > -1
                    || lnfClassName.indexOf("Aqua") > -1) {
                _platform = MAC_OSX;
                propsLNF = getSettingsString("MACOSX_LNF", "\n");
            } else {
                _platform = LINUX;
                propsLNF = getSettingsString("LINUX_LNF", "\n");
            }
            Class classDefinition = Class.forName(propsLNF);
            UIManager.setLookAndFeel((LookAndFeel) classDefinition
                    .newInstance());
            return;
        } catch (Exception e) {
            // Ignore exceptions, which only result in the wrong look and feel.
        }
        // gets here only if a custom L&F was not found,
        // or was found but not successfully assigned
        try {
            UIManager.setLookAndFeel(lnfClassName);
        } catch (Exception ex) {
            // Ignore exceptions, which only result in the wrong look and feel.
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                      public variables                     ////
    ///////////////////////////////////////////////////////////////////
    
    public static final short SVG_RENDERING_NOT_SET = 0;
    public static final short SVG_DIVA_RENDERING = 1;
    public static final short SVG_BATIK_RENDERING = 2;

    public static final int WINDOWS = 1;
    public static final int MAC_OSX = 2;
    public static final int LINUX = 3;
    
    ///////////////////////////////////////////////////////////////////
    ////                        private variables                  ////
    ///////////////////////////////////////////////////////////////////

    private static short svgRenderingMethod = SVG_RENDERING_NOT_SET;
    
    private static int _platform = -1;
}