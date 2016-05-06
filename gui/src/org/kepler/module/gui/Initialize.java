/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-06-25 10:28:28 -0700 (Wed, 25 Jun 2014) $' 
 * '$Revision: 32779 $'
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

package org.kepler.module.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.kepler.gui.KeplerGraphFrame;
import org.kepler.gui.KeplerGraphFrameUpdater;
import org.kepler.gui.ViewManager;
import org.kepler.module.ModuleInitializer;
import org.kepler.moml.filter.KeplerBackwardCompatibility;

import ptolemy.data.expr.Constants;

/** Perform initialization for the GUI module. 
 *
 * This class implements KeplerGraphFrameUpdater so that it can add
 * a view selector to the toolbar.
 *
 * Additionally, it initializes the moml filters for actors in the
 * util module.
 *
 * @author Daniel Crawl
 * @version $Id: Initialize.java 32779 2014-06-25 17:28:28Z crawl $
 *
 */

public class Initialize implements KeplerGraphFrameUpdater,
		ModuleInitializer {
	/**
	 * Compares this object with the specified object for order.
	 */
	public int compareTo(KeplerGraphFrameUpdater updater) {
		// we want our toolbar modifications on the right end, i.e.
		// applied last, so we're always greater
		return 1;
	}

	/**
	 * Perform any module-specific initializations.
	 */
	public void initializeModule() {
		
		
    	// *****************
    	// ANY CHANGES IN THIS CLASS LIKELY MUST ALSO BE MADE IN TAGGING MODULE'S
    	// OVERRIDE
    	// ************
		
		// add ourself as an updater so we can add the view selector
		KeplerGraphFrame.addUpdater(this);

		// initialize the backwards-compatibility moml filters.
		KeplerBackwardCompatibility.initialize();
	}
    
    /** 
     * Update the components. 
     */
    public void updateFrameComponents(KeplerGraphFrame.Components components)
    {
        KeplerGraphFrame frame = components.getFrame();
        JToolBar toolbar = components.getToolBar();

        JPanel ddHolder = new JPanel(new BorderLayout());
        ViewManager vman = ViewManager.getInstance();
        JComponent vsel = vman.getViewSelector(frame);
        ddHolder.add(vsel, BorderLayout.EAST);
        toolbar.add(ddHolder);
        

        //remove the full screen button for the 2.0 release.
        //this is a kind of lame way to do this, but since the name
        //of each component in the toolbar has not been set, there is no
        //good way to figure out which component we're looking at.  If
        //components are added before the 4th item then this will break.
	
        Component[] comps = toolbar.getComponents();
        comps[4].setVisible(false); 

        //ALSO IMPORTANT Note: This is overridden in this module and
		// several others. As of 02/10/10 the classes where the full screen
        // button was hidden are as follows:
	
	    // org.kepler.module.util.Initialize
        // org.kepler.module.tagging.Initialize
        // org.kepler.module.provenance.Initialize
        // org.kepler.module.reporting.Initialize
        // org.kepler.module.workflowrunmanager.Initialize
        // org.kepler.module.wrp.Initialize

        
    }
    
	public void dispose(KeplerGraphFrame frame) {

	}
}