/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: brooks $'
 * '$Date: 2012-09-20 21:23:19 -0700 (Thu, 20 Sep 2012) $' 
 * '$Revision: 30728 $'
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

import ptolemy.actor.gui.PtolemyEffigy;
import ptolemy.actor.gui.Tableau;
import ptolemy.gui.Top;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.LibraryAttribute;

//////////////////////////////////////////////////////////////////////////
//// MultiCompositeTableau

/** An editor tableau for MultiCompositeActors.
 * 
 @author Daniel Crawl, Possibly based on CaseGraphTableau by Edward A. Lee.
 @version $Id: MultiCompositeTableau.java 30728 2012-09-21 04:23:19Z brooks $
 */
public abstract class MultiCompositeTableau extends Tableau {
    /** Create a new multicomposite editor tableau with the specified
     *  container and name.
     *  @param container The container.
     *  @param name The name.
     *  @exception IllegalActionException If the model associated with
     *   the container effigy is not an instance of CompositeEntity.
     *  @exception NameDuplicationException If the container already
     *   contains an object with the specified name.
     */
    public MultiCompositeTableau(PtolemyEffigy container, String name)
            throws IllegalActionException, NameDuplicationException {
        this(container, name, null);
    }

    /** Create a new multicomposite editor tableau with the specified
     *  container, name, and default library.
     *  @param container The container.
     *  @param name The name.
     *  @param defaultLibrary The default library, or null to not specify one.
     *  @exception IllegalActionException If the model associated with
     *   the container effigy is not an instance of CompositeEntity.
     *  @exception NameDuplicationException If the container already
     *   contains an object with the specified name.
     */
    public MultiCompositeTableau(PtolemyEffigy container, String name,
            LibraryAttribute defaultLibrary) throws IllegalActionException,
            NameDuplicationException {
        super(container, name);

        NamedObj model = container.getModel();

        if (!(model instanceof CompositeEntity)) {
            throw new IllegalActionException(this,
                    "Cannot edit a model that is not a CompositeEntity.");
        }

        createGraphFrame((CompositeEntity)model, defaultLibrary);
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
    abstract public void createGraphFrame(CompositeEntity model,
            LibraryAttribute defaultLibrary);
        
    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Configure a frame. */
    protected void _configureFrame(Top frame)
    {
        try {
            setFrame(frame);
        } catch (IllegalActionException ex) {
            throw new InternalErrorException(ex);
        }

        frame.setBackground(BACKGROUND_COLOR);
        frame.pack();
        frame.centerOnScreen();
        frame.setVisible(true);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /** Background color. */
    private static Color BACKGROUND_COLOR = new Color(0xe5e5e5);
}