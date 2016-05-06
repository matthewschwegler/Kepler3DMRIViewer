/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
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

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import org.kepler.objectmanager.library.LibraryManager;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.tree.EntityTreeModel;
import ptolemy.vergil.tree.PTree;

/**
 * A simple pane used to display the contents of the library tree.
 * 
 * @author Matt Jones
 * @version $Id: BasicLibraryPane.java 26600 2010-12-23 19:01:04Z aschultz $
 * @since Kepler 1.0
 */
public class BasicLibraryPane extends JPanel implements TabPane {
	
	private String _name;

	private JTree _library;
	private JScrollPane _libraryScrollPane;
	
	private TableauFrame _frame;
	
	/**
	 * Construct a new library pane for displaying the tree of actors that can
	 * be dragged to the graph editor.
	 * 
	 * @param _libraryModel
	 *            the model containing the library to be displayed
	 */
	public BasicLibraryPane() {
		super();
	}
	
	/**
	 * Implementation of TabPane getName()
	 */
	public String getTabName() {
		return _name;
	}
	
	public void setTabName(String name) {
		_name = name;
	}
	
	/**
	 * Implementation of TabPane setParentFrame(TableauFrame)
	 */	
	public void setParentFrame(TableauFrame parent) {
		_frame = parent;
	}

	/**
	 * Implementation of TabPane getParentFrame()
	 */
	public TableauFrame getParentFrame() {
		return _frame;
	}

	/**
	 * Implementation of TabPane initializeTab()
	 */
	public void initializeTab() throws Exception {
		
		// get the tree model from the LibraryManager
		LibraryManager lman = LibraryManager.getInstance();
		EntityTreeModel libraryModel = lman.getTreeModel();

		_library = new PTree(libraryModel);
		_library.setRootVisible(false);
		_libraryScrollPane = new JScrollPane(_library);
		_libraryScrollPane.setMinimumSize(new Dimension(200, 200));
		_libraryScrollPane.setPreferredSize(new Dimension(200, 325));
		this.add("Components", _libraryScrollPane);
		
	}

	/**
	 * A factory that creates the library panel for the editors.
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
		public TabPane createTabPane() {
			BasicLibraryPane blp = new BasicLibraryPane();
			blp.setTabName(this.getName());
			return blp;
		}
	}
}