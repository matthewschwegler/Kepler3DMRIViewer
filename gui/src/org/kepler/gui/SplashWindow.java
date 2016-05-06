/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-03-27 11:42:53 -0700 (Wed, 27 Mar 2013) $' 
 * '$Revision: 31766 $'
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
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.SwingUtilities;

import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;

/**
 * A Splash window.
 * <p>
 * 
 * Usage: MyApplication is your application class. Create a Splasher class which
 * opens the splash window, invokes the main method of your Application class,
 * and disposes the splash window afterwards. Please note that we want to keep
 * the Splasher class and the SplashWindow class as small as possible. The less
 * code and the less classes must be loaded into the JVM to open the splash
 * screen, the faster it will appear.
 * 
 * <pre>
 * class Splasher {
 *     public static void main(String[] args) {
 *         SplashWindow.splash(Startup.class.getResource(&quot;splash.gif&quot;));
 *         MyApplication.main(args);
 *         SplashWindow.disposeSplash();
 *     }
 * }
 * </pre>
 * 
 * @author Werner Randelshofer
 * @version 2.1 2005-04-03 Revised.
 */
public class SplashWindow extends Window {
    /**
     * The current instance of the splash window. (Singleton design pattern).
     */
    private static SplashWindow instance;

    /**
     * The splash image which is displayed on the splash window.
     */
    private Image image;

    /**
     * The version number to be displayed in the window.
     */
    private String version;
    private String versionLabel;

    /**
     * This attribute indicates whether the method paint(Graphics) has been
     * called at least once since the construction of this window.<br>
     * This attribute is used to notify method splash(Image) that the window has
     * been drawn at least once by the AWT event dispatcher thread.<br>
     * This attribute acts like a latch. Once set to true, it will never be
     * changed back to false again.
     * 
     * @see #paint
     * @see #splash
     */
    private boolean paintCalled = false;

    /**
     * Creates a new instance.
     * 
     * @param parent
     *            the parent of the window.
     * @param image
     *            the splash image.
     */
    private SplashWindow(Frame parent, Image image) {
        super(parent);
        this.image = image;

        // Load the image
        MediaTracker mt = new MediaTracker(this);
        mt.addImage(image, 0);

        try {
            mt.waitForID(0);
        } catch (InterruptedException ie) {
            // Ignored
        }

        // Center the window on the screen
        int imgWidth = image.getWidth(this);
        int imgHeight = image.getHeight(this);
        setSize(imgWidth, imgHeight);

        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - imgWidth) / 2,
                (screenDim.height - imgHeight) / 2);

        // Users shall be able to close the splash window by
        // clicking on its display area. This mouse listener
        // listens for mouse clicks and disposes the splash window.
        MouseAdapter disposeOnClick = new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                // Note: To avoid that method splash hangs, we
                // must set paintCalled to true and call notifyAll.
                // This is necessary because the mouse click may
                // occur before the contents of the window
                // has been painted.
                synchronized (SplashWindow.this) {
                    SplashWindow.this.paintCalled = true;
                    SplashWindow.this.notifyAll();
                }
                dispose();
            }
        };
        addMouseListener(disposeOnClick);

        ConfigurationProperty commonProperty = ConfigurationManager
                .getInstance().getProperty(
                        ConfigurationManager.getModule("common"));
        versionLabel = commonProperty.getProperty("splash.versionLabel")
                .getValue();
        version = commonProperty.getProperty("kepler.version").getValue();
    }

    /**
     * Updates the display area of the window.
     */
    public void update(Graphics g) {
        // Note: Since the paint method is going to draw an
        // image that covers the complete area of the component we
        // do not fill the component with its background color
        // here. This avoids flickering.
        paint(g);
    }

    /**
     * Paints the image on the window.
     */
    public void paint(Graphics g) {
        g.drawImage(image, 0, 0, this);
        Font saveFont = g.getFont();
        Color saveColor = g.getColor();

        Font font = new Font("SanSerif", Font.BOLD, 24);
        g.setFont(font);
        Color c = new Color(23, 133, 102);
        g.setColor(c);

        int imgWidth = image.getWidth(this);
        String versionString = versionLabel + " " + version;
        int x = getHorizontalStartPosition(versionString, imgWidth, g);
        g.drawString(versionString, x, 200);

        // Notify method splash that the window
        // has been painted.
        // Note: To improve performance we do not enter
        // the synchronized block unless we have to.
        if (!paintCalled) {
            paintCalled = true;
            synchronized (this) {
                notifyAll();
            }
        }
        g.setFont(saveFont);
        g.setColor(saveColor);
    }

    /**
     * Calculate the x position at which to start drawing a string to center it
     * within a given width.
     * 
     * @param s
     *            the string to be centered
     * @param w
     *            the width over which the string should be centered
     * @param g
     *            the graphics context used to draw the string
     * @return the x position to start drawing the string
     */
    int getHorizontalStartPosition(String s, int width, Graphics g) {
        FontMetrics fm = g.getFontMetrics();
        int xpos = (width - fm.stringWidth(s)) / 2;
        return xpos;
    }

    /**
     * Open's a splash window using the specified image.
     * 
     * @param image
     *            The splash image.
     */
    public static void splash(Image image) {
        splash(image, false);
    }

    /**
   *
   */
    public static void splash(Image image, boolean reset) {
        if (reset) {
            instance = null;
        }
        if (instance == null && image != null) {
            Frame f = new Frame();

            // Create the splash image
            instance = new SplashWindow(f, image);
            if (reset) {
                instance.paintCalled = false;
            }
            // Show the window.
            instance.show();

            // Note: To make sure the user gets a chance to see the
            // splash window we wait until its paint method has been
            // called at least once by the AWT event dispatcher thread.
            // If more than one processor is available, we don't wait,
            // and maximize CPU throughput instead.
            if (!EventQueue.isDispatchThread()
                    && Runtime.getRuntime().availableProcessors() == 1) {
                synchronized (instance) {
                    while (!instance.paintCalled) {
                        try {
                            instance.wait();
                        } catch (InterruptedException e) {
                            // Ignored.
                        }
                    }
                }
            }
        }
    }

    /**
     * Opens a splash window using the specified image.
     * 
     * @param imageURL
     *            The url of the splash image.
     */
    public static void splash(URL imageURL) {
        splash(imageURL, false);
    }

    /**
     * splash the url. if reset is true, allow the splash to be shown more than
     * once
     */
    public static void splash(URL imageURL, boolean reset) {
        if (imageURL != null) {
            splash(Toolkit.getDefaultToolkit().createImage(imageURL), reset);
        }
    }

    /**
     * Closes the splash window.
     */
    public static void disposeSplash() {
        if (instance != null) {
            instance.getOwner().dispose();
            instance = null;
        }
    }

    /**
     * Invokes the main method of the provided class name.
     * 
     * @param args
     *            the command line arguments
     */
    public static void invokeMain(final String className, final String[] args) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        Class.forName(className)
                                .getMethod("main",
                                        new Class[] { String[].class })
                                .invoke(null, new Object[] { args });
                    } catch (Exception e) {
                        InternalError error = new InternalError(
                                "Failed to invoke main method");

                        error.initCause(e);
                        throw error;
                    }
                }
            });
        } catch (Exception e) {
            InternalError error = new InternalError(
                    "Failed to invoke main method");
            error.initCause(e);
            throw error;
        }
    }

}
