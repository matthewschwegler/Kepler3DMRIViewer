/*
 * Copyright (c) 2004-2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2013-01-15 13:33:01 -0800 (Tue, 15 Jan 2013) $' 
 * '$Revision: 31328 $'
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

package org.kepler.gui.kar;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.KeplerGraphFrame;
import org.kepler.kar.KARFile;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.gui.ExtensionFilenameFilter;
import ptolemy.gui.JFileChooserBugFix;
import ptolemy.gui.PtFileChooser;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.Nameable;
import ptolemy.vergil.basic.BasicGraphFrame;
import ptolemy.vergil.toolbox.FigureAction;
import diva.gui.GUIUtilities;

/**
 * This action opens a kar file to the system. It is called from File -> Open.
 *
 * @author Ben Leinfelder, Christopher Brooks
 * @since 05/28/2009
 */
public class OpenArchiveAction extends FigureAction
{

    private static String DISPLAY_NAME = "Open";
    private static String TOOLTIP = "Open a KAR file archive.";
    private static ImageIcon LARGE_ICON = null;
    private static KeyStroke ACCELERATOR_KEYSTROKE = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O,
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

    // //////////////////////////////////////////////////////////////////////////////

    private TableauFrame parent;

    private final static Log log = LogFactory.getLog(OpenArchiveAction.class);
    private static final boolean isDebugging = log.isDebugEnabled();

    private File archiveFileToOpen = null;
    private boolean updateHistoryAndLastDirectory = true;

    private ActionEvent actionEvent = null;
    
    /** The exception generated when trying to open the KAR. */
    private Exception _openException = null;
    
    /**
     * Constructor
     *
     * @param parent the "frame" (derived from ptolemy.gui.Top) where the menu is
     *               being added.
     */
    public OpenArchiveAction(TableauFrame parent)
    {
        super("Open...");

        if (parent == null)
        {
            IllegalArgumentException iae = new IllegalArgumentException(
                    "OpenArchiveAction constructor received NULL argument for TableauFrame");
            iae.fillInStackTrace();
            throw iae;
        }
        this.parent = parent;

        this.putValue(Action.NAME, DISPLAY_NAME);
        this.putValue(GUIUtilities.LARGE_ICON, LARGE_ICON);
        this.putValue("tooltip", TOOLTIP);
        this.putValue(GUIUtilities.ACCELERATOR_KEY, ACCELERATOR_KEYSTROKE);
    }

    /**
     * Explicitly set the Archive file that the action will open. If not file is
     * set a File chooser dialog is displayed to the user.
     *
     * @param archiveFile
     */
    public void setArchiveFileToOpen(File archiveFile)
    {
        archiveFileToOpen = archiveFile;
    }
    
    /**
     * Change whether or not to update Kepler's Recent Files menu, and 
     * last directory setting.
     * Default is true.
     *
     * @param updateHistoryAndLastDirectory
     */
    public void updateHistoryAndLastDirectory(boolean updateHistoryAndLastDirectory)
    {
        this.updateHistoryAndLastDirectory = updateHistoryAndLastDirectory;
    }

    /**
     * Attempt to open the KAR. Any exception that is generated can be
     * retrieved with getOpenException().
     *
     * @param e ActionEvent
     */
    public void actionPerformed(ActionEvent e)
    {

        // must call this first...
        super.actionPerformed(e);
        actionEvent = e;
        // ...before calling this:
        // NamedObj target = super.getTarget();

        File karFile = null;
        if (archiveFileToOpen != null)
        {
            karFile = archiveFileToOpen;
        }
        else
        {
            // Create a file filter that accepts .kar files.
            ExtensionFilenameFilter filter = new ExtensionFilenameFilter(new String[]{"kar", "xml", "moml"});
            
			// Avoid white boxes in file chooser, see
			// http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3801
			JFileChooserBugFix jFileChooserBugFix = new JFileChooserBugFix();
			Color background = null;
			PtFileChooser chooser = null;

			try {
				background = jFileChooserBugFix.saveBackground();
				chooser = new PtFileChooser(parent, "Open",
						JFileChooser.OPEN_DIALOG);
				if(parent instanceof BasicGraphFrame && updateHistoryAndLastDirectory) {
					chooser.setCurrentDirectory(((BasicGraphFrame)parent).getLastDirectory());
				}
				chooser.addChoosableFileFilter(filter);

				int returnVal = chooser.showDialog(parent, "Open");
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					// process the given file
					karFile = chooser.getSelectedFile();
				}
			} finally {
				jFileChooserBugFix.restoreBackground(background);
			}
            
        }
        if (karFile != null)
        {
            String karFileName = karFile.getName().toLowerCase();
            if ( karFileName.endsWith(".kar") )
            {
                //System.out.println("DEBUG: Opening KAR");
                try
                {
                    openKAR(karFile, false, updateHistoryAndLastDirectory);
                }
                catch (Exception exc)
                {
                	_openException = exc;
                }
            }
            else if( karFileName.endsWith(".xml") || karFileName.endsWith(".moml") )
            {
                //System.out.println("DEBUG: Opening XML");
                _open(karFile);
            }
        }
    }

    /** Return any exception generated while trying to open the KAR. */
    public Exception getOpenException() {
    	return _openException;
    }

    /**
     * Process the new kar file into the actor library.
     *
     * @param karFile the file to process
     * @param forceOpen
     * @parem updateHistory update recently opened history menu. Default true.
     */
    public void openKAR(File karFile, boolean forceOpen, boolean updateHistory) throws Exception
    {
    	updateHistoryAndLastDirectory(updateHistory);
    	
        if (isDebugging)
            log.debug("openKAR(" + karFile.toString() + ")");

        // Read in the KAR file
        KARFile karf = new KARFile(karFile);

        if (forceOpen || karf.isOpenable())
        {
            boolean opened = karf.openKARContents(parent, forceOpen);
            log.debug("called openKARContents. opened:"+opened);

            if (!opened)
            {
                JOptionPane.showMessageDialog(null, "The contents of this KAR are not openable.");
            }
            else
            {
                // update the history menu
                if(parent instanceof KeplerGraphFrame && updateHistoryAndLastDirectory) {
                	((KeplerGraphFrame)parent).updateHistory(karFile.getAbsolutePath());
                }
				if(parent instanceof BasicGraphFrame && updateHistoryAndLastDirectory) {
					((BasicGraphFrame)parent).setLastDirectory(karFile.getParentFile());
				}
            }

        }
        else
        {
            if (!karf.areAllModuleDependenciesSatisfied())
            {
                log.debug("KAR file module dependencies are not satisfied, calling ImportModuleDependenciesAction");

                //module-dependencies aren't satisfied, invoke ImportModuleDependenciesAction
                ImportModuleDependenciesAction imda = new ImportModuleDependenciesAction(parent);
                imda.setArchiveFile(karFile);
                imda.actionPerformed(actionEvent);
            }
            else
            {
                JOptionPane.showMessageDialog(null, "This KAR is not openable.");
            }

        }

        karf.close();

    }


    /**
     * Open a file dialog to identify a file to be opened, and then call
     * _read() to open the file.
     */
    protected void _open(File file)
    {

        try
        {
            // NOTE: It would be nice if it were possible to enter
            // a URL in the file chooser, but Java's file chooser does
            // not permit this, regrettably.  So we have a separate
            // menu item for this.

            // Report on the time it takes to open the model.
            long startTime = System.currentTimeMillis();
            _read(file.toURI().toURL());
            long endTime = System.currentTimeMillis();
            if (endTime > startTime + 10000)
            {
                // Only print the time if it is more than 10
                // seconds See also PtolemyEffigy.  Perhaps
                // this code should be in PtolemyEffigy, but
                // if it is here, we get the time it takes to
                // read any file, not just a Ptolemy model.
                System.out.println("Opened " + file + " in "
                        + (System.currentTimeMillis() - startTime)
                        + " ms.");
            }
            // update the history menu
            if(parent instanceof KeplerGraphFrame && updateHistoryAndLastDirectory) {
            	((KeplerGraphFrame)parent).updateHistory(file.getAbsolutePath());
            }
			if(parent instanceof BasicGraphFrame && updateHistoryAndLastDirectory) {
				((BasicGraphFrame)parent).setLastDirectory(file.getParentFile());
			}
        }
        catch (Error error)
        {
            // Be sure to catch Error here so that if we throw an
            // Error, then we will report it to with a window.
            try
            {
                throw new RuntimeException(error);
            }
            catch (Exception ex2)
            {
                ex2.printStackTrace();
            }
        }
        catch (Exception ex)
        {
            // NOTE: The XML parser can only throw an
            // XmlException.  It signals that it is a user
            // cancellation with the special string pattern
            // "*** Canceled." in the message.

            if ((ex.getMessage() != null)
                    && !ex.getMessage().startsWith("*** Canceled."))
            {
                // No need to report a CancelException, since
                // it results from the user clicking a
                // "Cancel" button.
                ex.printStackTrace();
            }
        }
    }

    /**
     * Read the specified URL.  This delegates to the ModelDirectory
     * to ensure that the preferred tableau of the model is opened, and
     * that a model is not opened more than once.
     *
     * @param url The URL to read.
     * @throws Exception If the URL cannot be read, or if there is no
     *                   tableau.
     */
    protected void _read(URL url) throws Exception
    {
        Tableau _tableau = parent.getTableau();
        if (_tableau == null)
        {
            throw new Exception("No associated Tableau!"
                    + " Can't open a file.");
        }

        // NOTE: Used to use for the first argument the following, but
        // it seems to not work for relative file references:
        // new URL("file", null, _directory.getAbsolutePath()
        Nameable configuration = _tableau.toplevel();

        if (configuration instanceof Configuration)
        {
            ((Configuration) configuration).openModel(url, url, url
                    .toExternalForm());
        }
        else
        {
            throw new InternalErrorException(
                    "Expected top-level to be a Configuration: "
                            + _tableau.toplevel().getFullName());
        }
    }
    
    /** True if a message about failing to find last directory was printed. */
    private static boolean _printedFailedLastDirectoryMessage = false;
}
