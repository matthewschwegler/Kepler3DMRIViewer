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
package org.kepler.gui.controller;

import org.kepler.gui.TabbedLookInsideAction;

import ptolemy.actor.gui.Configuration;
import ptolemy.vergil.actor.ActorInstanceController;
import diva.graph.GraphController;
import diva.graph.JGraph;
import diva.gui.GUIUtilities;

/** An actor controller that provides an action to open actors in a tab.
 * 
 *  @author Daniel Crawl
 *  @version $Id: TabbedActorController.java 31122 2012-11-26 22:22:25Z crawl $
 */
public class TabbedActorController extends ActorInstanceController {

    public TabbedActorController(GraphController controller) {
        super(controller);
        //System.out.println("new TabbedCompositeActorController for graph controller " + controller);
    }

    public TabbedActorController(GraphController controller,
            Access access) {
        super(controller, access);
        //System.out.println("new TabbedCompositeActorController for graph controller " + controller);
    }
    
    /**
     * Add hot keys to the actions in the given JGraph. It would be better that
     * this method was added higher in the hierarchy. Now most controllers
     *
     * @param jgraph
     *            The JGraph to which hot keys are to be added.
     */
    public void addHotKeys(JGraph jgraph) {
        super.addHotKeys(jgraph);
        GUIUtilities.addHotKey(jgraph, _openInTabAction);
    }    
    
    public void setConfiguration(Configuration configuration) {
        super.setConfiguration(configuration);
        _openInTabAction.setConfiguration(configuration);
    }
    
    protected TabbedLookInsideAction _openInTabAction =
        new TabbedLookInsideAction("Open Actor in Tab");
}
