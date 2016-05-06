/* A state change event for multi composite graph frames.
 * 
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2010-05-25 10:20:59 -0700 (Tue, 25 May 2010) $' 
 * '$Revision: 24562 $'
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

import java.awt.Component;

import org.kepler.gui.state.StateChangeEvent;

import ptolemy.kernel.util.NamedObj;

/** An event that represents a state change in MultiCompositeGraphFrame.
 * 
 *   @author Daniel Crawl
 *   @version $Id: MultiCompositeStateChangeEvent.java 24562 2010-05-25 17:20:59Z crawl $
 */

public class MultiCompositeStateChangeEvent extends StateChangeEvent {

    /** Construct a new MultiCompositeStateChangeEvent event.
     * @param source the source of the event
     * @param frame the frame in which the state change occurs
     * @param model the newly selected model
     */
    public MultiCompositeStateChangeEvent(Component source, 
            MultiCompositeGraphFrame frame, NamedObj model) {
        super(source, CHANGE_COMPOSITE, model);
        _frame = frame;
    }
    
    /** Get the frame in which the state change occurs. */
    public MultiCompositeGraphFrame getFrame() {
        return _frame;
    }

    /** Constant to denote the selected composite has been changed. */
    public static final String CHANGE_COMPOSITE = "CHANGE_COMPOSITE";
    
    ///////////////////////////////////////////////////////////////////
    ////                     private variables                     ////

    /** The frame in which the state change occurs. */
    private MultiCompositeGraphFrame _frame;
}
