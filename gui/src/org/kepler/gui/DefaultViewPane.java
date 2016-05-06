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
import javax.swing.JSplitPane;
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
public class DefaultViewPane extends JPanel implements ViewPane {

	private TableauFrame _frame;
	private String _viewName;
	private Vector<ViewPaneLocation> _locations;

	/*
	 * The top level split pane that divides the JPanel into left and right
	 * halves.
	 */
	private JSplitPane _westEastSplitPane;

	/*
	 * The 2 split panes that divide the left and right sides into top and
	 * bottom.
	 */
	private JSplitPane _westSplitPane;
	private JSplitPane _eastSplitPane;

	/*
	 * The four JTabbedPanes that make up the quadrants. These are the
	 * containers that the TabPanes are added to and each corresponds to a
	 * ViewPaneLocation.
	 */
	private JTabbedPane _nwtp;
	private JTabbedPane _netp;
	private JTabbedPane _swtp;
	private JTabbedPane _setp;

	/**
	 * Constructor. Initializes available locations.
	 */
	public DefaultViewPane() {
		_locations = new Vector<ViewPaneLocation>();
		_locations.add(new ViewPaneLocation("NW"));
		_locations.add(new ViewPaneLocation("NE"));
		_locations.add(new ViewPaneLocation("SW"));
		_locations.add(new ViewPaneLocation("SE"));
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

		_nwtp = new JTabbedPane();
		_netp = new JTabbedPane();
		_swtp = new JTabbedPane();
		_setp = new JTabbedPane();

		_westSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		_westSplitPane.setOneTouchExpandable(true);
		_westSplitPane.setResizeWeight(1);
		_westSplitPane.setTopComponent(_nwtp);
		_westSplitPane.setBottomComponent(_swtp);

		_eastSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		_eastSplitPane.setOneTouchExpandable(true);
		_eastSplitPane.setResizeWeight(1);
		_eastSplitPane.setTopComponent(_netp);
		_eastSplitPane.setBottomComponent(_setp);

		_westEastSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		_westEastSplitPane.setOneTouchExpandable(true);
		_westEastSplitPane.setRightComponent(_eastSplitPane);
		_westEastSplitPane.setLeftComponent(_westSplitPane);

		this.setLayout(new BorderLayout());
		this.add(_westEastSplitPane, BorderLayout.CENTER);

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

		if (location.getName() == "NW") {
			_nwtp.add(tabPane.getTabName(), (Component) tabPane);
		} else if (location.getName() == "NE") {
			_netp.add(tabPane.getTabName(), (Component) tabPane);
		} else if (location.getName() == "SW") {
			_swtp.add(tabPane.getTabName(), (Component) tabPane);
		} else if (location.getName() == "SE") {
			_setp.add(tabPane.getTabName(), (Component) tabPane);
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

	public Container getLocationContainer(String locationName)
			throws Exception {

		if (locationName.equals("NW")) {
			return _nwtp;
		} else if (locationName.equals("NE")) {
			return _netp;
		} else if (locationName.equals("SW")) {
			return _swtp;
		} else if (locationName.equals("SE")) {
			return _setp;
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

			DefaultViewPane vp = new DefaultViewPane();
			vp.setParentFrame(parent);

			// use the name specified in the configuration to name the view
			vp.setViewName(this.getName());

			return vp;
		}
	}

	public List<TabPane> getTabPanes(String tabName) throws Exception {
		List<TabPane> panes = new ArrayList<TabPane>();
		int index = -1;
		index = _netp.indexOfTab(tabName);
		if (index > -1) {
			panes.add((TabPane) _netp.getComponentAt(index));
		}
		index = _nwtp.indexOfTab(tabName);
		if (index > -1) {
			panes.add((TabPane) _nwtp.getComponentAt(index));
		}
		index = _setp.indexOfTab(tabName);
		if (index > -1) {
			panes.add((TabPane) _setp.getComponentAt(index));
		}
		index = _swtp.indexOfTab(tabName);
		if (index > -1) {
			panes.add((TabPane) _swtp.getComponentAt(index));
		}
		return panes;
	}

}
