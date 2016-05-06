/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:22:25 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31122 $'
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

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.kar.ExportArchiveAction;
import org.kepler.kar.KARFile;
import org.kepler.kar.KARManager;
import org.kepler.objectmanager.ObjectManager;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.util.FileUtil;
import org.kepler.util.RenameUtil;
import org.kepler.util.ShutdownNotifier;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.ModelDirectory;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.PtolemyPreferences;
import ptolemy.actor.gui.SizeAttribute;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.gui.MemoryCleaner;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.LibraryAttribute;
import ptolemy.util.CancelException;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.actor.ActorGraphFrame;
import ptolemy.vergil.basic.BasicGraphFrame;
import ptolemy.vergil.basic.BasicGraphFrameExtension;
import diva.graph.GraphController;
import diva.gui.toolbox.JCanvasPanner;
import diva.util.java2d.ShapeUtilities;

/** Kepler-specific overrides for the graph frame.
 * 
 */
public class KeplerGraphFrame extends ActorGraphFrame {

    /**
     * Constructor.
     * 
     * @param entity
     * @param tableau
     */
    public KeplerGraphFrame(CompositeEntity entity, Tableau tableau) {
        this(entity, tableau, null);
    }

    /**
     * Constructor
     * 
     * @param entity
     * @param tableau
     * @param defaultLibrary
     */
    public KeplerGraphFrame(CompositeEntity entity, Tableau tableau,
            LibraryAttribute defaultLibrary) {
        super(entity, tableau, defaultLibrary);
        _initKeplerGraphFrame();
    }

    /**
     * Override the dispose method to unattach any listeners that may keep this
     * model from getting garbage collected. Also remove KARManager's mapping of
     * this JFrame to KARfile if necessary.
     */
    @Override
    public void dispose() {

        if (_topPack instanceof KeplerMenuHandler) {
            KeplerMenuHandler topPack = (KeplerMenuHandler) _topPack;
            MenuMapper mm = topPack.menuMapper;
            mm.clear();
            /*
             * mm.printDebugInfo(); Map<String,Action> m =
             * mm.getPTIIMenuActionsMap(); m.clear(); KeplerMenuHandler kmh =
             * (KeplerMenuHandler) _topPack; kmh.clear(); _topPack = null;
             */
        }

        JMenuBar keplerMenuBar = getJMenuBar();
        /* int removed = */MemoryCleaner.removeActionListeners(keplerMenuBar);
        // System.out.println("KeplerGraphFrame menubar action listeners removed: "
        // + removed);

        CanvasDropTargetListener listener = CanvasDropTargetListener
                .getInstance();
        if (listener != null && _dropTarget != null) {
            _dropTarget.deRegisterAdditionalListener(listener);
        }

        if (_horizontalScrollBarListener != null
                && _horizontalScrollBar != null) {
            _horizontalScrollBar
                    .removeAdjustmentListener(_horizontalScrollBarListener);
            _horizontalScrollBarListener = null;
        }
        if (_verticalScrollBarListener != null
                && _verticalScrollBarListener != null) {
            _verticalScrollBar
                    .removeAdjustmentListener(_verticalScrollBarListener);
            _verticalScrollBarListener = null;
        }

        // remove JFrame => KARFile mapping from KARManager
        KARManager karManager = KARManager.getInstance();
        karManager.remove(this);

        TabManager tabManager = TabManager.getInstance();
        tabManager.removeAllFrameTabs(this);

        // this isn't safe. see:
        // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5095#c14
        // it is safe now after changes in r26444.
        ObjectManager.getInstance().removeNamedObjs(this.getModel());
        ViewManager.getInstance().removeOpenFrame(this);
        LibraryManager.getInstance().removeAllFrameTabs(this);

        // now call dispose on updaters in order
        synchronized (_updaterSet) {
            Iterator<KeplerGraphFrameUpdater> itr = _updaterSet.iterator();
            while (itr.hasNext()) {
                KeplerGraphFrameUpdater updater = itr.next();
                updater.dispose(this);
            }
        }

        ModelToFrameManager m2fm = ModelToFrameManager.getInstance();
        m2fm.remove(this);

        // see if this was the last window
        if (m2fm.numberOfOpenFrames() == 0) {
            ShutdownNotifier.shutdown();
        }

        KeyboardFocusManager focusManager = KeyboardFocusManager
                .getCurrentKeyboardFocusManager();
        focusManager.clearGlobalFocusOwner();
        focusManager.downFocusCycle();

        super.dispose();
    }

    /**
     * Go to full screen.
     */
    @Override
    public void fullScreen() {

        // FIXME: do nothing since returning from full screen currently crashes
        // kepler. this is due to cancelFullScreen() dereferencing _splitPane,
        // which is never initialized.
    }

    // commented out since zoom() is commented out, and hides a super class _zoomFlag
    //protected boolean _zoomFlag = false;

    /** Get the graph controller. */
    public GraphController getGraphController() {
        return _controller;
    }

    public JToolBar getToolBar() {
        return this._toolbar;
    }

    /**
     * Return the size of the visible part of the canvas, in canvas coordinates.
     * 
     * @return Rectangle2D
     */
    public Rectangle2D getVisibleSize() {
        AffineTransform current = _jgraph.getGraphPane().getCanvas()
                .getCanvasPane().getTransformContext().getTransform();
        AffineTransform inverse;
        try {
            inverse = current.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e.toString());
        }
        Dimension size = _jgraph.getGraphPane().getCanvas().getSize();
        Rectangle2D visibleRect = new Rectangle2D.Double(0, 0, size.getWidth(),
                size.getHeight());
        return ShapeUtilities.transformBounds(visibleRect, inverse);
    }

    /** Set the model for this frame. */
    @Override
    public void setModel(NamedObj model) {
        super.setModel(model);

        // FIXME: ModelToFrameManager is probably unnecessary, this
        // information is in the Configuration.
        ModelToFrameManager m2fm = ModelToFrameManager.getInstance();
        m2fm.remove(this);
        m2fm.add(getModel(), this);
    }

    /** Update the History menu. */
    public void updateHistory(String absolutePath) throws IOException {
        _updateHistory(absolutePath, false);
    }

    /**
     * Create the menus that are used by this frame. It is essential that
     * _createGraphPane() be called before this.
     */
    @Override
    protected void _addMenus() {
        super._addMenus();

        // remove Open File... O accelerator.
        // Open... (for KARs) uses this accelerator now.
        for (int i = 0; i < _fileMenuItems.length; i++) {
            JMenuItem menuItem = _fileMenuItems[i];
            if (menuItem != null) {
                String menuItemText = menuItem.getText();
                if (menuItemText != null && menuItemText.equals("Open File")) {
                    // int removed =
                    // MemoryCleaner.removeActionListeners(menuItem);
                    // System.out.println("KeplerGraphFrame _fileMenuItems["+i+"] action listeners removed: "
                    // + removed);
                    menuItem.setAccelerator(null);
                }
            }
        }

        // see if the effigy for the top level workflow is called "Unnamed"
        // if so, renamed it to "Unnamed1"

        NamedObj namedObj = getModel();
        if (namedObj.getContainer() == null && namedObj.getName().length() == 0) {
            Effigy effigy = getEffigy();
            if (effigy != null) {
                String name = effigy.identifier.getExpression();
                if (name.equals("Unnamed")) {
                    String newName = name + _nextUnnamed;
                    _nextUnnamed++;
                    try {
                        // set the identifier, which is what shows up at the top
                        // of the window
                        effigy.identifier.setExpression(newName);
                    } catch (Exception e) {
                        report("Error setting effigy name.", e);
                    }

                    try {
                        namedObj.setName(newName);
                    } catch (Exception e) {
                        report("Error setting workflow name to " + newName
                                + ".", e);
                    }

                }
            }
        }

        // let any KeplerGraphFrameUpdaters perform updates.
        Components components = new Components();

        // now call updateFrameComponents on updaters in order
        synchronized (_updaterSet) {
            Iterator<KeplerGraphFrameUpdater> itr = _updaterSet.iterator();
            while (itr.hasNext()) {
                KeplerGraphFrameUpdater updater = itr.next();
                updater.updateFrameComponents(components);
            }
        }

    }

    /**
     * Get the name of this object. If the parent class returns "Unnamed", use
     * the effigy's identifier as the name.
     * 
     * @return The name.
     */
    @Override
    protected String _getName() {
        String retval = super._getName();
        if (retval.equals("Unnamed")) {
            Effigy effigy = getEffigy();
            if (effigy != null) {
                retval = effigy.identifier.getExpression();
            }
        }
        return retval;
    }

    /**
     * Get the component whose size is to be recorded in the model when it is
     * saved into an output file. In this class, the return of this function is
     * the same as that of {@link #_getRightComponent()}. A subclass may
     * override this function to return a different component, such as a tab of
     * a tabbed pane, whose size is to be recorded instead.
     * 
     * @return The component whose size is to be recorded.
     */
    protected JComponent _getSizeComponent() {
        return _getRightComponent();
    }

    /**
     * Override BasicGraphFrame._initBasicGraphFrame()
     */
    @Override
    protected void _initBasicGraphFrame() {

        /**
         * @todo - FIXME - Need to move this further up the hierarchy, so other
         *       types of frames use it too. Don't put it in a launcher class
         *       like KeplerApplication, because it then gets overridden later,
         *       elsewhere in PTII
         */
        StaticGUIResources.setLookAndFeel();

        _initBasicGraphFrameInitialization();

        _dropTarget = BasicGraphFrameExtension.getDropTarget(_jgraph);

        // add a CanvasDropTargetListener so that other classes can get
        CanvasDropTargetListener listener = CanvasDropTargetListener
                .getInstance();
        _dropTarget.registerAdditionalListener(listener);

        ActionListener deletionListener = new DeletionListener();

        _rightComponent.registerKeyboardAction(deletionListener, "Delete",
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        _rightComponent.registerKeyboardAction(deletionListener, "BackSpace",
                KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        _initBasicGraphFrameRightComponent();

        _jgraph.setRequestFocusEnabled(true);

        // Background color is parameterizable by preferences.
        Configuration configuration = getConfiguration();

        if (configuration != null) {
            try {
                // Set the PtolemyPreference to the desired background.
                // See
                // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=2321#c14
                PtolemyPreferences preferences = PtolemyPreferences
                        .getPtolemyPreferencesWithinConfiguration(configuration);
                if (_isDebugging) {
                    _log.debug("bg: " + BACKGROUND_COLOR);
                }
                if (preferences != null) {
                    float[] components = new float[4];
                    // Make sure we get only 4 elements in case the color space
                    // is bigger than 4
                    components = BACKGROUND_COLOR.getComponents(components);
                    preferences.backgroundColor.setExpression("{"
                            + components[0] + "," + components[1] + ","
                            + components[2] + "," + components[3] + "}");
                    _rightComponent.setBackground(preferences.backgroundColor
                            .asColor());
                    if (_isDebugging) {
                        _log.debug("desired background: " + BACKGROUND_COLOR
                                + " actual background:  "
                                + preferences.backgroundColor.asColor());
                    }
                }
            } catch (IllegalActionException e1) {
                // Ignore the exception and use the default color.
            }
        }

        _initBasicGraphFrameRightComponentMouseListeners();

        try {
            // The SizeAttribute property is used to specify the size
            // of the JGraph component. Unfortunately, with Swing's
            // mysterious and undocumented handling of component sizes,
            // there appears to be no way to control the size of the
            // JGraph from the size of the Frame, which is specified
            // by the WindowPropertiesAttribute.
            SizeAttribute size = (SizeAttribute) getModel().getAttribute(
                    "_vergilSize", SizeAttribute.class);
            if (size != null) {
                size.setSize(_jgraph);
            } else {
                // Set the default size.
                // Note that the location is of the frame, while the size
                // is of the scrollpane.
                _jgraph.setPreferredSize(new Dimension(600, 400));
            }

            _initBasicGraphFrameSetZoomAndPan();
        } catch (Exception ex) {
            // Ignore problems here. Errors simply result in a default
            // size and location.
        }

        // Create the panner.
        _graphPanner = new JCanvasPanner(_jgraph);

        _horizontalScrollBar = new JScrollBar(Adjustable.HORIZONTAL);
        _verticalScrollBar = new JScrollBar(Adjustable.VERTICAL);

        // see if we want scrollbars on the canvas or not
        // the answer defaults to 'no'
        CanvasNavigationModifierFactory CNMfactory = (CanvasNavigationModifierFactory) getConfiguration()
                .getAttribute("canvasNavigationModifier");
        if (CNMfactory != null) { // get the scrollbar flag from the factory if
            // it exists in the config
            ScrollBarModifier modifier = CNMfactory.createScrollBarModifier();
            _scrollBarFlag = modifier.getScrollBarModifier();
        }

        _canvasPanel = new JPanel();

        _canvasPanel.setBorder(null);
        _canvasPanel.setLayout(new BorderLayout());

        if (_scrollBarFlag) {
            _canvasPanel.add(_horizontalScrollBar, BorderLayout.SOUTH);
            _canvasPanel.add(_verticalScrollBar, BorderLayout.EAST);
            _horizontalScrollBar.setModel(_jgraph.getGraphPane().getCanvas()
                    .getHorizontalRangeModel());
            _verticalScrollBar.setModel(_jgraph.getGraphPane().getCanvas()
                    .getVerticalRangeModel());
            _horizontalScrollBarListener = new ScrollBarListener(
                    _horizontalScrollBar);
            _verticalScrollBarListener = new ScrollBarListener(
                    _verticalScrollBar);
            _horizontalScrollBar
                    .addAdjustmentListener(_horizontalScrollBarListener);
            _verticalScrollBar
                    .addAdjustmentListener(_verticalScrollBarListener);
        }

        // NOTE: add _rightComponent instead of _jgraph since _rightComponent
        // may be sub-divided into tabbed panes.
        // see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=3708
        _canvasPanel.add(_rightComponent, BorderLayout.CENTER);

        TabManager tabman = TabManager.getInstance();
        tabman.initializeTabs(this);

        ViewManager viewman = ViewManager.getInstance();
        viewman.initializeViews(this);
        try {
            viewman.addCanvasToLocation(_canvasPanel, this);
        } catch (Exception e) {
            throw new RuntimeException("Could not add canvas panel: "
                    + e.getMessage());
        }

        // _jgraph.setMinimumSize(new Dimension(0, 0));

        getContentPane().add(viewman.getViewArea(this), BorderLayout.CENTER);

        // The toolbar panel is the container that contains the main toolbar and
        // any additional toolbars
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS)); // They
        // stack
        _toolbar = new JToolBar(); // The main Kepler toolbar
        toolbarPanel.add(_toolbar);
        getContentPane().add(toolbarPanel, BorderLayout.NORTH); // Place the
        // toolbar panel
        _initBasicGraphFrameToolBarZoomButtons();

        _initBasicGraphFrameActions();

        // Add a weak reference to this to keep track of all
        // the graph frames that have been created.
        _openGraphFrames.add(this);

        System.gc();
    }

    /**
     * KeplerGraphFrame Initializer method
     */
    protected void _initKeplerGraphFrame() {
        if (_isDebugging) {
            _log.debug("_initKeplerGraphFrame()");
        }
        ModelToFrameManager m2fm = ModelToFrameManager.getInstance();
        m2fm.add(getModel(), this);
    }

    /**
     * Open a dialog to prompt the user to save a KAR. Return false if the user
     * clicks "cancel", and otherwise return true.
     * 
     * Overrides Top._queryForSave()
     * 
     * @return _SAVED if the file is saved, _DISCARDED if the modifications are
     *         discarded, _CANCELED if the operation is canceled by the user,
     *         and _FAILED if the user selects save and the save fails.
     */
    @Override
    protected int _queryForSave() {
        Object[] options = { "Save", "Discard changes", "Cancel" };

        // making more generic since other items to go in the KAR
        // may be the reason for querying to save
        // String query = "Save changes to " + StringUtilities.split(_getName())
        // + "?";
        String query = "Save changes to KAR?";

        // Show the MODAL dialog
        int selected = JOptionPane.showOptionDialog(this, query,
                "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (selected == JOptionPane.YES_OPTION) {

            JButton saveButton = new JButton("Save Kar");
            ExportArchiveAction eaa = new ExportArchiveAction(this);
            eaa.setRefreshFrameAfterSave(false);
            // if the file already exists, call ExportArchiveAction.setSave()
            // so that the save as file dialog does is not used.
            KARFile karFile = KARManager.getInstance().get(this);
            if (karFile != null) {
                File file = karFile.getFileLocation();
                if (file.canWrite()) {
                    eaa.setSaveFile(file);
                }
            }
            saveButton.addActionListener(eaa);
            saveButton.doClick();

            if (eaa.saveSucceeded()) {
                setModified(false);
                return _SAVED;
            } else {
                return _FAILED;
            }

        }

        if (selected == JOptionPane.NO_OPTION) {
            return _DISCARDED;
        }

        return _CANCELED;
    }

    /**
     * Query the user for a filename, save the model to that file, and open a
     * new window to view the model. This overrides the base class to close the
     * old effigy if its name matches "UnnamedN". It also assigns a new LSID to
     * the new workflow unless it is unnamed.
     * 
     * @param extension
     *            If non-null, then the extension that is appended to the file
     *            name if there is no extension.
     * @return True if the save succeeds.
     */
    @Override
    protected boolean _saveAs(String extension) {
        boolean isUnnamed = false;

        // If the tableau was unnamed before, then we need
        // to close this window after doing the save.
        Effigy effigy = getEffigy();
        if (effigy != null) {
            String id = effigy.identifier.getExpression();

            // see if effigy id matches unnamed regex.
            Matcher matcher = RenameUtil.unnamedIdPattern.matcher(id);
            if (matcher.matches()) {
                isUnnamed = true;
            }
        }

        NamedObj model = getModel();

        // When we do a File -> Save As, the new and original
        // workflows need to have different LSIDs. This is taken
        // care of by RenameUtil.renameComponentEntity below.

        // NOTE: get the model directory before calling _saveAsHelper
        // since _saveAsHelper may dispose the effigy and thereby close
        // all associated tableaux
        ModelDirectory directory = (ModelDirectory) getConfiguration()
                .getEntity(Configuration._DIRECTORY_NAME);

        // print a warning the first time this is called
        if (!_printedWarningFirstTime) {
            try {
                MessageHandler
                        .warning("Exporting the workflow to XML does not save any associated workflow "
                                + "artifacts such as the report layout or module dependencies.");
            } catch (CancelException e) {
                // user clicked cancel button, so do not save.
                return false;
            }
            _printedWarningFirstTime = true;
        }

        // perform the save
        URL newModelURL = super._saveAsHelper(extension);

        // see if we successfully saved
        if (newModelURL != null) {

            // get the new model
            File newModelFile = new File(newModelURL.getFile());
            String newName = FileUtil.getFileNameWithoutExtension(newModelFile
                    .getName());
            PtolemyEffigy newEffigy = (PtolemyEffigy) directory
                    .getEffigy(newModelURL.toString());
            NamedObj newModel = newEffigy.getModel();

            // set newModel name back to the original name so we can
            // send it through renameComponentEntity
            try {
                newModel.setName(model.getName());
            } catch (IllegalActionException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (NameDuplicationException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            if (isUnnamed) {
                try {
                    // This will have the effect of closing all the
                    // tableaux associated with the unnamed model.
                    effigy.setContainer(null);

                    // remove the workflow from the object manager since
                    // its window went away.
                    ObjectManager objectManager = ObjectManager.getInstance();
                    objectManager.removeNamedObj(model);
                } catch (Exception e) {
                    report("Error closing effigy.", e);
                    return false;
                }
            }

            try {
                RenameUtil.renameComponentEntity((ComponentEntity) newModel,
                        newName);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // After the MoML has been saved to disk we add it to the library
            // and refresh the JTrees if it is saved in a local repository update the library
            LocalRepositoryManager lrm = LocalRepositoryManager.getInstance();
            if (lrm.isInLocalRepository(newModelFile)) {

                //_savekar.saveToCache();

                LibraryManager lm = LibraryManager.getInstance();
                try {
                    lm.addXML(newModelFile);
                } catch (Exception e2) {
                    MessageHandler.error("Error adding workflow to library.",
                            e2);
                }
                try {
                    lm.refreshJTrees();
                } catch (IllegalActionException e2) {
                    MessageHandler.error("Error updating tree.", e2);
                }
            }

        }

        return true;
    }

    /**
     * Add the name of the last file open or set the name to the first position
     * if already in the list
     * 
     * @param file
     *            name of the file to add
     * @param delete
     *            If true, remove from the history list, otherwise the file is
     *            added to the beginning.
     */
    @Override
    protected void _updateHistory(String file, boolean delete)
            throws IOException {
        super._updateHistory(file, delete);
        // reload the menus in all open windows.
        MenuMapper.reloadAllMenubars();
    }

    /** A class to give KeplerGraphFrameUpdaters access to GUI components. */
    public class Components {
        /** Construct a new Component. Only KeplerGraphFrame may do this. */
        private Components() {
        }

        /** Get the KeplerGraphFrame. */
        public KeplerGraphFrame getFrame() {
            return KeplerGraphFrame.this;
        }

        /** Get the menu. */
        public JMenu getMenu() {
            return _graphMenu;
        }

        /** Get the toolbar. */
        public JToolBar getToolBar() {
            return _toolbar;
        }
    }

    /**
     * Listener for scrollbar events.
     */
    public class ScrollBarListener implements java.awt.event.AdjustmentListener {
        /**
         * constructor
         * 
         * @param sb
         *            JScrollBar
         */
        public ScrollBarListener(JScrollBar sb) {
            if (sb.getOrientation() == Adjustable.HORIZONTAL) {
                orientation = "h";
            } else {
                orientation = "v";
            }
        }

        /**
         * translate the model when the scrollbars move
         * 
         * @param e
         *            AdjustmentEvent
         */
        @Override
        public void adjustmentValueChanged(java.awt.event.AdjustmentEvent e) {
            int val = e.getValue();

            if (orientation.equals("h")) {
                if (_zoomFlag) {
                    return;
                }
                Rectangle2D visibleRect = getVisibleSize();
                Point2D newLeft = new Point2D.Double(val, 0);
                AffineTransform newTransform = _jgraph.getGraphPane()
                        .getCanvas().getCanvasPane().getTransformContext()
                        .getTransform();

                newTransform.translate(visibleRect.getX() - newLeft.getX(), 0);

                _jgraph.getGraphPane().getCanvas().getCanvasPane()
                        .setTransform(newTransform);

                if (_graphPanner != null) {
                    _graphPanner.repaint();
                }
            } else {
                if (_zoomFlag) {
                    return;
                }
                Rectangle2D visibleRect = getVisibleSize();
                Point2D newTop = new Point2D.Double(0, val);
                AffineTransform newTransform = _jgraph.getGraphPane()
                        .getCanvas().getCanvasPane().getTransformContext()
                        .getTransform();

                newTransform.translate(0, visibleRect.getY() - newTop.getY());

                _jgraph.getGraphPane().getCanvas().getCanvasPane()
                        .setTransform(newTransform);

                if (_graphPanner != null) {
                    _graphPanner.repaint();
                }
            }
        }

        private String orientation = "";
    }

    /** An ActionListener for handling deletion events. */
    private class DeletionListener implements ActionListener {
        /**
         * Delete any nodes or edges from the graph that are currently selected.
         * In addition, delete any edges that are connected to any deleted
         * nodes.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            delete();
        }
    }

    /** Override the BasicGraphFrame Background color **/
    public static final Color BACKGROUND_COLOR = new Color(255, 255, 255);

    private static final Log _log = LogFactory.getLog(KeplerGraphFrame.class
            .getName());

    private static final boolean _isDebugging = _log.isDebugEnabled();

    /** The number of the next "Unnamed" effigy. */
    private static int _nextUnnamed = 1;

    /** If true, we've printed the warning about exporting to MoML. */
    private static Boolean _printedWarningFirstTime = false;

    /** A synchronizedSortedSet TreeSet of controller updaters. */
    private static SortedSet<KeplerGraphFrameUpdater> _updaterSet = Collections
            .synchronizedSortedSet(new TreeSet<KeplerGraphFrameUpdater>());

    /**
     * Add a KeplerGraphFrameUpdater to the set of updaters that can make
     * changes to components, e.g., the toolbar or menus. The ordering of
     * updates can be specified by implementing the Comparable interface.
     */
    public static void addUpdater(KeplerGraphFrameUpdater updater) {
        boolean addUpdater = _updaterSet.add(updater);
        if (!addUpdater) {
            // if this happens even when the set doesn't contain
            // your updater, check your updater's compareTo value
            _log.warn("KeplerGraphFrame addUpdater(" + updater.getClass()
                    + ") returned false.");
        }
    }

    /** Clear the set of KeplerGraphFrameUpdaters. */
    public static void clearUpdaters() {
        _updaterSet.clear();
    }

    public static Vector<TableauFrame> getOpenFrames() {
        Vector<TableauFrame> frames = new Vector<TableauFrame>(
                _openGraphFrames.size());
        for (BasicGraphFrame bgf : _openGraphFrames) {
            frames.add(bgf);
        }
        return frames;
    }

    /** Remove an KeplerGraphFrameUpdater from the set of updaters. */
    public static void removeUpdater(KeplerGraphFrameUpdater updater) {
        // _updaterSet.remove doesn't move the
        // org.kepler.reporting.gui.ReportViewerPanel object correctly
        // when the first window is closed. Switching to use iterator remove.
        // http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5095#c19
        // _updaterSet.remove(updater);
        synchronized (_updaterSet) {
            Iterator<KeplerGraphFrameUpdater> itr = _updaterSet.iterator();
            while (itr.hasNext()) {
                KeplerGraphFrameUpdater _updater = itr.next();
                if (_updater == updater) {
                    itr.remove();
                }
            }
        }
    }

    // Listeners

    /** A panel for the canvas. */
    protected JPanel _canvasPanel;

    /** Horizontal scrollbar. */
    protected JScrollBar _horizontalScrollBar;

    private ScrollBarListener _horizontalScrollBarListener;

    /** Flag to determine whether to render scrollbars on the canvas. */
    private boolean _scrollBarFlag = false;

    /** Vertical scrollbar. */
    protected JScrollBar _verticalScrollBar;

    private ScrollBarListener _verticalScrollBarListener;


    // 	private class ExecutionMoMLChangeRequest extends OffsetMoMLChangeRequest {
    // // FIXME: This was only called from within createHierarchy()?  It may have been called within copy() as well.
    // 		private String _name;

    // 		public ExecutionMoMLChangeRequest(Object o, NamedObj n, String s) {
    // 			super(o, n, s);
    // 			_name = new String();
    // 		}

    // 		public void setName(String name) {
    // 			_name = name;
    // 		}

    // 		protected void _execute() throws Exception {
    // 			super._execute();
    // 			CompositeEntity container = (CompositeEntity) getContext();
    // 			NamedObj newObject = container.getEntity(_name);

    // 			// _setLocation(compositeActor, point);

    // 			// Kepler wants a different icon.
    // 			// FIXME: It seems a little strange to do this inside
    // 			// a change request, but the change request inner class
    // 			// was already here, and perhaps this helps undo work
    // 			// better or something?
    // 			IconLoader _iconLoader = MoMLParser.getIconLoader();
    // 			if (_iconLoader != null) {
    // 				_iconLoader.loadIconForClass(
    // 						"ptolemy.actor.TypedCompositeActor", newObject);
    // 			}
    // 		}
    // 	}
}
