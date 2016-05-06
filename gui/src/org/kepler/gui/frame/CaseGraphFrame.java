/*
 * Copyright (c) 2006-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-21 15:42:51 -0700 (Fri, 21 Sep 2012) $' 
 * '$Revision: 30737 $'
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

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.event.ChangeListener;

import ptolemy.actor.gui.Tableau;
import ptolemy.actor.lib.hoc.Case;
import ptolemy.kernel.Port;
import ptolemy.moml.LibraryAttribute;
import diva.gui.GUIUtilities;

//////////////////////////////////////////////////////////////////////////
//// CaseGraphFrame

/**
 This is a graph editor frame for ptolemy case models.

 @author Daniel Crawl, based on ptolemy.vergil.modal.CaseGraphFrame by Edward A. Lee
 @version $Id: CaseGraphFrame.java 30737 2012-09-21 22:42:51Z crawl $
 @since Ptolemy II 7.1
 @Pt.ProposedRating Yellow (eal)
 @Pt.AcceptedRating Red (johnr)
 */
public class CaseGraphFrame extends MultiCompositeGraphFrame implements ChangeListener {
    /** Construct a frame associated with the specified case actor.
     *  After constructing this, it is necessary
     *  to call setVisible(true) to make the frame appear.
     *  This is typically done by calling show() on the controlling tableau.
     *  This constructor results in a graph frame that obtains its library
     *  either from the model (if it has one) or the default library defined
     *  in the configuration.
     *  @see Tableau#show()
     *  @param entity The model to put in this frame.
     *  @param tableau The tableau responsible for this frame.
     */
    public CaseGraphFrame(Case entity, Tableau tableau) {
        this(entity, tableau, null);
    }

    /** Construct a frame associated with the specified case actor.
     *  After constructing this, it is necessary
     *  to call setVisible(true) to make the frame appear.
     *  This is typically done by calling show() on the controlling tableau.
     *  This constructor results in a graph frame that obtains its library
     *  either from the model (if it has one), or the <i>defaultLibrary</i>
     *  argument (if it is non-null), or the default library defined
     *  in the configuration.
     *  @see Tableau#show()
     *  @param entity The model to put in this frame.
     *  @param tableau The tableau responsible for this frame.
     *  @param defaultLibrary An attribute specifying the default library
     *   to use if the model does not have a library.
     */
    public CaseGraphFrame(Case entity, Tableau tableau,
            LibraryAttribute defaultLibrary) {
        super(entity, tableau, defaultLibrary);

        _case = entity;
        _addCaseAction = new AddCaseAction();
        _removeCaseAction = new RemoveRefinementAction("case", "cases");

        // Override the default help file.
        // FIXME
        // helpFile = "ptolemy/configs/doc/vergilFsmEditorHelp.htm";
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Create the menus that are used by this frame.
     *  It is essential that _createGraphPane() be called before this.
     */
    @Override
    protected void _addMenus() {
        super._addMenus();
        JMenu menu = new JMenu("Case");
        menu.setMnemonic(KeyEvent.VK_C);
        _menubar.add(menu);
        GUIUtilities.addHotKey(_getRightComponent(), _addCaseAction);
        GUIUtilities.addMenuItem(menu, _addCaseAction);
        GUIUtilities.addHotKey(_getRightComponent(), _removeCaseAction);
        GUIUtilities.addMenuItem(menu, _removeCaseAction);
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /** The action to add a case. */
    private AddCaseAction _addCaseAction;

    /** The Case actor displayed by this frame. */
    private Case _case;
    
    /** The action to remove a refinement. */
    private RemoveRefinementAction _removeCaseAction;

    ///////////////////////////////////////////////////////////////////
    ////                     public inner classes                  ////

    /** Class implementing the Add Case menu command. */
    public class AddCaseAction extends AddRefinementAction {

        /** Create a case action with label "Add Case". */
        public AddCaseAction() {
            super("Add Case", "case",
                "Pattern that the control input must match");
        }
                
        /** Returns true if port should be mirrored. */
        public boolean mirrorPort(Port port)
        {
            // do not mirror the control port
            if(port == _case.control.getPort())
            {
                return false;
            }
            return true;
        }
    }
}