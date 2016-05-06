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

/**
 * 
 */
package org.kepler.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * A ViewPane consisting of 4 ViewPaneLocations.
 * 
 * @author Aaron Schultz
 * 
 */
public class SingleViewPane extends JPanel implements ViewPane {

	private TableauFrame _frame;
	private String _viewName;
	private Vector<ViewPaneLocation> _locations;

	/*
	 * The top level split pane that divides the JPanel into left and right
	 * halves.
	 */

	private JTabbedPane _ctp;

	/**
	 * Constructor. Initializes available locations.
	 */
	public SingleViewPane() {
		_locations = new Vector<ViewPaneLocation>();
		_locations.add(new ViewPaneLocation("C"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.ViewPane#getParentFrame()
	 */
	public TableauFrame getParentFrame() {
		return _frame;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kepler.gui.ViewPane#setParentFrame(ptolemy.actor.gui.TableauFrame)
	 */
	public void setParentFrame(TableauFrame parent) {
		_frame = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.ViewPane#getViewName()
	 */
	public String getViewName() {
		return _viewName;
	}

	public void setViewName(String viewName) {
		_viewName = viewName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.ViewPane#initializeView()
	 */
	public void initializeView() throws Exception {

		_ctp = new JTabbedPane();

		this.setLayout(new BorderLayout());
		this.add(_ctp, BorderLayout.CENTER);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.ViewPane#getAvailableLocations()
	 */
	public Vector<ViewPaneLocation> getAvailableLocations() {
		return _locations;
	}

	public boolean hasLocation(String locationName) {
		for (int i = 0; i < _locations.size(); i++) {
			if (_locations.elementAt(i).getName().equals(locationName)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kepler.gui.ViewPane#addTabPane(org.kepler.gui.TabPane,
	 * org.kepler.gui.ViewPaneLocation)
	 */
	public void addTabPane(TabPane tabPane, ViewPaneLocation location)
			throws Exception {

		if (location.getName() == "C") {
			_ctp.add(tabPane.getTabName(), (Component) tabPane);
		} else {
			throw new Exception(
					"Unable to add "
							+ tabPane.getTabName()
							+ " TabPane to "
							+ getViewName()
							+ " ViewPane. "
							+ " The supplied ViewPaneLocation does not exist for this ViewPane.");
		}

	}

	public Container getLocationContainer(String locationName) throws Exception {

		if (locationName.equals("C")) {
			return _ctp;
		} else {
			throw new Exception(
					"Unable to find "
							+ locationName
							+ " ViewPaneLocation in the "
							+ getViewName()
							+ " ViewPane. "
							+ " The supplied ViewPaneLocation does not exist for this ViewPane.");
		}

	}

	/**
	 * A factory that creates the library panel for the editors.
	 * 
	 *@author Aaron Schultz
	 */
	public static class Factory extends ViewPaneFactory {
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
		public ViewPane createViewPane(TableauFrame parent) {

			SingleViewPane vp = new SingleViewPane();
			vp.setParentFrame(parent);

			// use the name specified in the configuration to name the view
			vp.setViewName(this.getName());

			return vp;
		}
	}

	public List<TabPane> getTabPanes(String tabName) throws Exception {
		List<TabPane> panes = new ArrayList<TabPane>();
		int index = -1;
		index = _ctp.indexOfTab(tabName);
		if (index > -1) {
			panes.add((TabPane) _ctp.getComponentAt(index));
		}
		return panes;
	}
}
