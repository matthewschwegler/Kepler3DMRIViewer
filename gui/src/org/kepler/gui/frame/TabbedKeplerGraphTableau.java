/*
 * Copyright (c) 2012 The Regents of the University of California.
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

package org.kepler.gui.frame;

import java.awt.Color;

import org.kepler.gui.ModelToFrameManager;
import org.kepler.gui.TabbedLookInsideAction;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFactory;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.LibraryAttribute;

/** An editor tableau for MultiCompositeActors.
 * 
 @author Daniel Crawl
 @version $Id: TabbedKeplerGraphTableau.java 31122 2012-11-26 22:22:25Z crawl $
 */
public class TabbedKeplerGraphTableau extends KeplerGraphTableau {
    /** Create a new TabbedKeplerGraphTableau with the specified
     *  container and name.
     *  @param container The container.
     *  @param name The name.
     *  @exception IllegalActionException If the model associated with
     *   the container effigy is not an instance of CompositeEntity.
     *  @exception NameDuplicationException If the container already
     *   contains an object with the specified name.
     */
    public TabbedKeplerGraphTableau(PtolemyEffigy container, String name)
            throws IllegalActionException, NameDuplicationException {
        this(container, name, null);
    }

    /** Create a new TabbedKeplerGraphTableau with the specified
     *  container, name, and default library.
     *  @param container The container.
     *  @param name The name.
     *  @param defaultLibrary The default library, or null to not specify one.
     *  @exception IllegalActionException If the model associated with
     *   the container effigy is not an instance of CompositeEntity.
     *  @exception NameDuplicationException If the container already
     *   contains an object with the specified name.
     */
    public TabbedKeplerGraphTableau(PtolemyEffigy container, String name,
            LibraryAttribute defaultLibrary) throws IllegalActionException,
            NameDuplicationException {
        super(container, name);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Create the graph frame that displays the model associated with
     *  this tableau. This method creates a MultiCompositeGraphFrame.
     *  @param model The Ptolemy II model to display in the graph frame.
     */
    public void createGraphFrame(CompositeEntity model) {
        createGraphFrame(model, null);
    }
    
    /** Create the graph frame that displays the model associated with
     *  this tableau together with the specified library.
     *  @param model The Ptolemy II model to display in the graph frame.
     *  @param defaultLibrary The default library, or null to not specify
     *   one.
     */
     public void createGraphFrame(CompositeEntity model,
            LibraryAttribute defaultLibrary)
     {
        TabbedKeplerGraphFrame frame;
        NamedObj container = model.getContainer();
 
        // if model is top-level or the action for opening in new tab is not
        // running, then open in a new frames
        if(container == null || !TabbedLookInsideAction.actionIsRunning()) {
            frame = new TabbedKeplerGraphFrame((CompositeActor) model, this,
                defaultLibrary);
        } else {
            frame = (TabbedKeplerGraphFrame) ModelToFrameManager.getInstance().getFrame(container);
            if(frame == null) {
                throw new InternalErrorException("Unable to find frame for " + model.getFullName());
            }
            frame.addComposite(model, this, defaultLibrary);
            
            // set this tableau to be a master so that when it is closed, the
            // associated frame is not closed in Tableau.setContainer().
            // the associated frame contains the tabbed pane and we do not
            // want to close everything when one of the composite tabs closes.
            setMaster(true);
        }
        
        // setFrame() sets the title to be the name of the model. if the model
        // is a tab in an existing frame, set the name back to the top level model
        String oldTitle = null;
        if(container != null) {
            oldTitle = frame.getTitle();
        }
         
        try {
            setFrame(frame);
        } catch (IllegalActionException e) {
            throw new InternalErrorException(e);
        }
        
        if(container != null) {
            frame.setTitle(oldTitle);
        }
         
        frame.setBackground(BACKGROUND_COLOR);         
     }
    
     /** Make this tableau visible by calling setVisible(true), and
      *  raising or deiconifying its window.
      *  If no frame has been set, then do nothing.
      */
     public void show() {
         
         // make the tableau visible
         super.show();
         
         // set the selected tab to the model for this tableau.
         // if the model is not one of the tabs, then setSelectedTab() does nothing.
         final PtolemyEffigy effigy = (PtolemyEffigy)getContainer();
         final NamedObj model = effigy.getModel();
         final TabbedKeplerGraphFrame frame =
             (TabbedKeplerGraphFrame) ModelToFrameManager.getInstance().getFrame(model);
         frame.setSelectedTab(model);

     }
     
    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /** Background color. */
    private final static Color BACKGROUND_COLOR = new Color(0xe5e5e5);

    // /////////////////////////////////////////////////////////////////
    // // public inner classes ////

    /**
     * A factory that creates graph editing tableaux for Ptolemy models.
     */
    public static class Factory extends TableauFactory {
        /**
         * Create an factory with the given name and container.
         * 
         * @param container
         *            The container.
         * @param name
         *            The name.
         * @exception IllegalActionException
         *                If the container is incompatible with this attribute.
         * @exception NameDuplicationException
         *                If the name coincides with an attribute already in the
         *                container.
         */
        public Factory(NamedObj container, String name)
                throws IllegalActionException, NameDuplicationException {
            super(container, name);
        }

        /**
         * Create a tableau in the default workspace with no name for the given
         * Effigy. The tableau will created with a new unique name in the given
         * model effigy. If this factory cannot create a tableau for the given
         * effigy (perhaps because the effigy is not of the appropriate
         * subclass) then return null. It is the responsibility of callers of
         * this method to check the return value and call show().
         * 
         * @param effigy
         *            The model effigy.
         * @return A new KeplerGraphTableau, if the effigy is a PtolemyEffigy,
         *         or null otherwise.
         * @exception Exception
         *                If an exception occurs when creating the tableau.
         */
        public Tableau createTableau(Effigy effigy) throws Exception {
            if (effigy instanceof PtolemyEffigy) {
                // First see whether the effigy already contains a graphTableau.
                TabbedKeplerGraphTableau tableau = (TabbedKeplerGraphTableau) effigy
                        .getEntity("graphTableau");

                if (tableau == null) {
                    // Check to see whether this factory contains a
                    // default library.
                    LibraryAttribute library = (LibraryAttribute) getAttribute(
                            "_library", LibraryAttribute.class);
                    tableau = new TabbedKeplerGraphTableau((PtolemyEffigy) effigy,
                            "graphTableau", library);
                }

                // Don't call show() here, it is called for us in
                // TableauFrame.ViewMenuListener.actionPerformed()
                return tableau;
            } else {
                return null;
            }
        }
    }
}