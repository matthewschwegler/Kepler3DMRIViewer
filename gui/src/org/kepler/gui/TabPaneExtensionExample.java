/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: berkley $'
 * '$Date: 2010-04-27 17:12:36 -0700 (Tue, 27 Apr 2010) $' 
 * '$Revision: 24000 $'
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

/**
 * 
 */
package org.kepler.gui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * This is an example template for extending the TabPane extension in Kepler.
 * 
 * @author Aaron Schultz
 * 
 */
public class TabPaneExtensionExample extends JPanel implements TabPane {
	
	private TableauFrame _frame;
	private String _tabName;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.TabPane#getParentFrame()
	 */
	public TableauFrame getParentFrame() {
		return _frame;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.TabPane#getTabName()
	 */
	public String getTabName() {
		return _tabName;
	}

	public void setTabName(String name) {
		_tabName = name;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.TabPane#initializeTab()
	 */
	public void initializeTab() throws Exception {
		// Add components to the JPanel here.
		JLabel jl = new JLabel("This is an example of implementing an extension to the TabPane extension point.");
		this.setLayout(new BorderLayout());
		this.add(jl, BorderLayout.NORTH);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kepler.gui.TabPane#setParentFrame(ptolemy.actor.gui.TableauFrame)
	 */
	public void setParentFrame(TableauFrame parent) {
		_frame = parent;
	}

	/**
	 * A factory that creates a TabPane.
	 * 
	 *@author Aaron Schultz
	 */
	public static class Factory extends TabPaneFactory {
		/**
		 * Create a factory with the given name and container.
		 * 
		 *@param container
		 *            The container.
		 *@param name
		 *            The name of the entity.
		 *@exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 *@exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a library pane that displays the given library of actors.
		 * 
		 * @return A new LibraryPaneTab that displays the library
		 */
		public TabPane createTabPane(TableauFrame parent) {
			TabPaneExtensionExample tpee = new TabPaneExtensionExample();

			/*
			 * Optionally you can create a method called setTabName and use the
			 * "name" value from the configuration.xml file by calling
			 * this.getName(). For Example if you have <property
			 * name="randomTestTab"
			 * class="org.kepler.gui.TabPaneExtensionExample$Factory" /> in
			 * configuration.xml then the name of the tab in the GUI becomes
			 * randomTestTab
			 */
			tpee.setTabName(this.getName());

			return tpee;
		}
	}

}
