/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: aschultz $'
 * '$Date: 2010-12-23 11:01:04 -0800 (Thu, 23 Dec 2010) $' 
 * '$Revision: 26600 $'
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

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;

import org.kepler.util.StaticResources;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.toolbox.FigureAction;


/**
 * Adding a new PreferencesAction to a component will open up the preferences
 * frame to the desired preferences tab.
 * 
 * @author Aaron Schultz
 * 
 */
public class PreferencesAction extends FigureAction {
	private String _initialOpenTab;
	private TableauFrame _parent;

	public PreferencesAction(String initialOpenTab) {
		super("Preferences");
		_initialOpenTab = initialOpenTab;
	}

	public PreferencesAction(TableauFrame parent) {
		super("Preferences");
		_parent = parent;
	}

	public void actionPerformed(ActionEvent e) {

		PreferencesFrame frame = null;

		PreferencesFrameTracker pft = PreferencesFrameTracker.getInstance();
		if (pft.isOpen()) {

			// There is a Preferences Frame already open, so lets use that one
			frame = pft.getPreferencesFrame();

		} else {
		  Window window = null;
			// There is not an open Preferences Frame so lets create one
		  
		  	if (_parent != null){
		  		window = _parent;
		  	}
		  	else{
				window =  GUIUtil.getParentWindow((Component) e.getSource());
			}
			
			if(window == null || !(window instanceof TableauFrame)) {
				window = GUIUtil.getParentTableauFrame((Component) e.getSource());
			}
			
			if (window != null && window instanceof TableauFrame) {
				_parent = (TableauFrame)window;
				TableauFrame _parentFrame = _parent;
				frame = new PreferencesFrame(
						StaticResources.getDisplayString(
								"preferences.title", 
								"Preferences"), 
								_parentFrame);
				frame.setLocation(_parentFrame.getLocation());
				frame.setVisible(true);
				pft.setOpen(frame);
			} else {
				MessageHandler.error("Couldn't open the Preferences frame - the action is not fired in a TableauFrame object");
			}

		}

		if (frame != null) {
			frame.setSelectedTab(_initialOpenTab);
			frame.toFront();
		}

	}
}