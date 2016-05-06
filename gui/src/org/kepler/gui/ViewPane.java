/*
 * Copyright (c) 2010 The Regents of the University of California.
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

import java.awt.Container;
import java.util.List;

import ptolemy.actor.gui.TableauFrame;

/**
 * This interface is to be implemented by anyone wanting to add a view to the
 * Kepler. The ViewPaneFactory generates instances of ViewPane.
 * 
 * @author Aaron Schultz
 */
public interface ViewPane {

	/**
	 * Initialization of the view should be done at some point after the
	 * constructor is called. The setParentFrame() method is called by the
	 * factory before calling initializeView(). This also allows for more easily
	 * adding new functions to this interface later on.
	 * 
	 * @throws Exception
	 */
	public abstract void initializeView() throws Exception;

	/**
	 * This method should return the name of the view which is used to label the
	 * view in the View selection list.
	 */
	public abstract String getViewName();

	/**
	 * This method must return the TableauFrame this view is associated with.
	 */
	public abstract TableauFrame getParentFrame();

	/**
	 * This method sets the TableauFrame this view is associated with.
	 */
	public abstract void setParentFrame(TableauFrame parent);

	/**
	 * This method must return true if the supplied location name matches the
	 * name of one of the supported ViewPaneLocations for this ViewPane.
	 */
	public abstract boolean hasLocation(String locationName);

	/**
	 * This method is used to add a TabPane to the ViewPane specifying the
	 * location in which the TabPane should be placed.
	 */
	public abstract void addTabPane(TabPane tabPane, ViewPaneLocation location)
			throws Exception;

	/**
	 * This method must return the Container that represents the specified
	 * ViewPaneLocation.
	 * 
	 * @param location
	 */
	public abstract Container getLocationContainer(String locationName)
			throws Exception;
	
	/**
	 * Look up the tab by the given name
	 * @param tabName
	 * 	 * @throws Exception
	 */
	public abstract List<TabPane> getTabPanes(String tabName)
		throws Exception;
	
	

}
