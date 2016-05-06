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
package org.kepler.gui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.KeyStroke;

import org.kepler.gui.frame.TabbedKeplerGraphFrame;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.basic.LookInsideAction;
import diva.gui.GUIUtilities;

/** An action to open a composite actor in a tab.
 * 
 *  @author Daniel Crawl
 *  @version $Id
 */
public class TabbedLookInsideAction extends LookInsideAction {

    /** Create a new TabbedLookInsideAction object with the given
     *  frame as it's container.
     *  @param menuActionLabel The label of the menu action to be displayed in
     *   the GUI context menus.
     */
    public TabbedLookInsideAction(TableauFrame parent) {
        this("Open Actor in Tab");
        _parent = parent;
        putValue(GUIUtilities.ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }
    
    /** Create a new TabbedLookInsideAction object with the given
     *  string as its menu action label.
     *  @param menuActionLabel The label of the menu action to be displayed in
     *   the GUI context menus.
     */
    public TabbedLookInsideAction(String menuActionLabel) {
        super(menuActionLabel);
        putValue(GUIUtilities.ACCELERATOR_KEY, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /** Execute the look inside action command received from the event sent from
     *  the user interface.
     *  @param event The event received to execute the look inside action.
     */
    public void actionPerformed(ActionEvent event) {

        //System.out.println("actionPerformed in TabbedLookInsideActionAction");
        
        _actionIsRunning.set(true);

        if(_parent != null) {
            setConfiguration(_parent.getConfiguration());
        }
                
        // call the parent class to make sure effigy and tableau exist for the
        // actor
        super.actionPerformed(event);
        
        // see if we're opening a composite actor
        // XXX what about atomic actors?
        NamedObj object = getTarget();
        //System.out.println("target is : " + object.getFullName());
        if(object instanceof CompositeActor) {
            if(_parent instanceof TabbedKeplerGraphFrame) {
                ((TabbedKeplerGraphFrame)_parent).setSelectedTab(object);
            } else if(_parent == null) {
                KeplerGraphFrame frame = ModelToFrameManager.getInstance().getFrame(object);
                if(frame instanceof TabbedKeplerGraphFrame) {
                    ((TabbedKeplerGraphFrame)frame).setSelectedTab(object);
                }
            }
        }
        
        _actionIsRunning.set(false);
    }
    
    /** Returns true if the action is being performed. */
    public static boolean actionIsRunning() {
        return _actionIsRunning.get();
    }
    
    /** True when the action is being performed. */
    private static AtomicBoolean _actionIsRunning = new AtomicBoolean(false);
    
    /** The containing frame. */
    private TableauFrame _parent;
}
