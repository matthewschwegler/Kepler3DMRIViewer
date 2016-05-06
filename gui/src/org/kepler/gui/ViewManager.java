/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: jianwu $'
 * '$Date: 2013-04-10 09:59:02 -0700 (Wed, 10 Apr 2013) $' 
 * '$Revision: 31882 $'
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

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Vector;
import java.util.WeakHashMap;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.gui.state.StateChangeMonitor;
import org.kepler.gui.state.ViewStateChangeEvent;

import ptolemy.actor.gui.PtolemyFrame;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.NamedObj;

/**
 * The ViewManager keeps track of configured ViewPanes.
 * 
 * @author Aaron Schultz
 * 
 */
public class ViewManager {

	private static final Log log = LogFactory.getLog(ViewManager.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	//_viewPanes needs to be WeakReference because some of its elements might be GCed during Kepler GUI start.
	//see bug: https://projects.ecoinformatics.org/ecoinfo/issues/5901
	protected Vector<ViewPane> _viewPanes; 
	protected WeakHashMap<TableauFrame, JPanel> _viewAreas;
	protected WeakHashMap<TableauFrame, JComboBox> _viewComboBoxes;

	/**
	 * Constructor.
	 */
	public ViewManager() {
		_viewPanes = new Vector<ViewPane>();
		_viewAreas = new WeakHashMap<TableauFrame, JPanel>();
		_viewComboBoxes = new WeakHashMap<TableauFrame, JComboBox>();
	}

	/**
	 * Instantiate all of the ViewPanes that are specified in configuration.xml
	 * 
	 * @param parent
	 */
	public void initializeViews(TableauFrame parent) {
		try {
			ViewPaneFactory VPfactory = (ViewPaneFactory) parent
					.getConfiguration().getAttribute("ViewPaneFactory");
			if (VPfactory == null) {
				VPfactory = new ViewPaneFactory(parent.getConfiguration(),
						"ViewPaneFactory");
			}

			if (VPfactory != null) {
				boolean success = VPfactory.createViewPanes(parent);
				if (!success) {
					System.out
							.println("error: ViewPane is null.  "
									+ "This "
									+ "problem can be fixed by adding a viewPaneFactory "
									+ "property in the configuration.xml file.");
				}
			} else {
				System.out.println("error: ViewPane is " + "null.  This "
						+ "problem can be fixed by adding a viewPaneFactory "
						+ "property in the configuration.xml file.");
			}
		} catch (ptolemy.kernel.util.NameDuplicationException nde) {

		} catch (Exception e) {
			System.out.println("Could not create the ViewPaneFactory: "
					+ e.getMessage());
			e.printStackTrace();
			return;
		}

		// Create the view area and add all the viewpanes to it
		JPanel viewArea = new JPanel(new CardLayout());
		Vector<ViewPane> frameViews = getFrameViews(parent);
		String[] viewsList = new String[frameViews.size()];
		for (int i = 0; i < frameViews.size(); i++) {
			ViewPane vp = frameViews.elementAt(i);
			viewArea.add((Component) vp, vp.getViewName());
			viewsList[i] = vp.getViewName();
			if (isDebugging)
				log.debug("add one element to viewsList:" + viewsList[i]);
		}

		try {
			addConfiguredTabPanes(parent);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// while( e. hasMoreElements() ){
		// TableauFrame tableFrame = (TableauFrame)(e.nextElement());
		// System.out.println("getContainer in _viewAreas:"+
		// tableFrame.getTableau().getContainer());
		// System.out.println("isMaster in _viewAreas:"+
		// tableFrame.getTableau().isMaster());
		// if (tableFrame.getTableau().getContainer()==null &&
		// tableFrame.getTableau().isMaster())
		// {
		// _viewAreas.remove(tableFrame);
		// System.out.println("one element is in _viewAreas removed");
		// _viewComboBoxes.remove(tableFrame);
		// System.out.println("one element is in _viewComboBoxes removed");
		// }
		//
		// }

		_viewAreas.put(parent, viewArea);
		if (isDebugging) {
			log.debug("_viewAreas:" + _viewAreas.size());
			log.debug("_viewAreas key set:" + _viewAreas.keySet());
		}
		
		

		JComboBox viewComboBox = new JComboBox(viewsList);
		if (viewComboBox != null && viewComboBox.getItemCount() > 0) {
			viewComboBox.setSelectedIndex(0);
			viewComboBox.addActionListener(new ViewComboBoxListener());
			_viewComboBoxes.put(parent, viewComboBox);
			if (isDebugging)
				log.debug("_viewComboBoxes:" + _viewComboBoxes.size());
		}

	}

	public JPanel getViewArea(TableauFrame parent) {
		return _viewAreas.get(parent);
	}

	/**
	 * Method to return a JComponent as a view selector. If there is only one
	 * view for the specified frame then an empty JLabel is returned otherwise a
	 * JComboBox is returned.
	 * 
	 * @param parent
	 * @return JComponent
	 */
	public JComponent getViewSelector(TableauFrame parent) {
		JComboBox c = getViewComboBox(parent);
		if (c.getItemCount() == 1) {
			// return new JLabel( c.getItemAt(0).toString() );
			return new JLabel("");
		} else {
			return c;
		}
	}

	/**
	 * Return a combo box that contains all of the views for the specified
	 * parent frame.
	 * 
	 * @param parent
	 * @return JComboBox
	 */
	public JComboBox getViewComboBox(TableauFrame parent) {
		return _viewComboBoxes.get(parent);
	}

	/**
	 * Set the JComboBox for a specific TableauFrame
	 * 
	 * @param parent
	 * @param jcb
	 */
	public void setViewComboBox(TableauFrame parent, JComboBox jcb) {
		_viewComboBoxes.put(parent, jcb);
	}

	public void showView(TableauFrame parent, String viewName) {
		JPanel viewArea = _viewAreas.get(parent);
		CardLayout cl = (CardLayout) (viewArea.getLayout());
		cl.show(viewArea, viewName);
		NamedObj reference = ((PtolemyFrame) parent).getModel();
		ViewPane viewPane = getViewPane(parent, viewName);
		StateChangeMonitor.getInstance().notifyStateChange(
				new ViewStateChangeEvent((Component) viewPane,
						ViewStateChangeEvent.SHOW_VIEW, reference, viewName));
	}

	/**
   *
   */
	public void addCanvasToLocation(Component canvas, TableauFrame parent)
			throws Exception {
		ConfigurationProperty commonProperty = ConfigurationManager
				.getInstance().getProperty(
						ConfigurationManager.getModule("common"));
		// get //canvasViewPaneLocation/viewPane
		List<ConfigurationProperty> viewPaneList = commonProperty
				.getProperties("canvasViewPaneLocation.viewPane");
		// for each viewPane
		for (int i = 0; i < viewPaneList.size(); i++) {
			ConfigurationProperty viewPaneProp = (ConfigurationProperty) viewPaneList
					.get(i);
			// get viewPane.name
			String viewPaneName = viewPaneProp.getProperty("name").getValue();

			// ViewPane theViewPane = getViewPane(parent, viewPane.name);
			ViewPane theViewPane = getViewPane(parent, viewPaneName);
			// if theViewPane == null, throw exception
			if (theViewPane == null) {
				// Check for the case where the ViewPane *is* actually defined
				// in configuration.xml, but it was not parsed from that file
				// because it was specified along with a tableau filter, which
				// the current tableau does not pass.
				if (isInConfigurationFile(viewPaneName)) {
					continue;
				}
				throw new Exception(
						viewPaneName
								+ " ViewPane specified in "
								+ viewPaneProp.getModule()
								+ " configuration.xml"
								+ " was not found in the ViewManager."
								+ " Make sure you have specified this in the configuration.xml file.");
			}
			List<ConfigurationProperty> viewPaneLocationList = viewPaneProp
					.getProperties("viewPaneLocation");
			// for each viewPane.viewPaneLocation
			for (int j = 0; j < viewPaneLocationList.size(); j++) {
				ConfigurationProperty viewPaneLocationProp = (ConfigurationProperty) viewPaneLocationList
						.get(j);
				// get viewPane.viewPaneLocation.name
				String viewPaneLocationName = viewPaneLocationProp.getProperty(
						"name").getValue();
				// if !theViewPane.hasLocation(viewPane.viewPaneLocation.name)
				// throw exception
				if (!theViewPane.hasLocation(viewPaneLocationName)) {
					throw new Exception("The ViewPaneLocation, "
							+ viewPaneLocationName
							+ ", is not an available location of ViewPane, "
							+ viewPaneName);
				}
				// theViewPane.getLocationContainer(viewPane.viewPaneLocation.name).add("Workflow",
				// canvas);

				// see if the tab name for the canvas is specified
				String canvasTabPaneName = "Workflow";
				ConfigurationProperty tabPaneProp = viewPaneProp
						.getProperty("tabPanename");
				if (tabPaneProp != null) {
					canvasTabPaneName = tabPaneProp.getValue();
				}

				canvas.setName(canvasTabPaneName);

				// add the canvas as the first tab in the view.
				theViewPane.getLocationContainer(viewPaneLocationName).add(
						canvas, 0);

				// if the canvas is part of a tabbed pane, make sure it is
				// selected
				Component container = canvas.getParent();
				if (container instanceof JTabbedPane) {
					((JTabbedPane) container).setSelectedIndex(0);
				}
			}
		}
	}

	private boolean isInConfigurationFile(String viewPaneName) {
		if (viewPaneName == null) {
			return false;
		}

		ConfigurationProperty guiProperty = ConfigurationManager.getInstance()
				.getProperty(ConfigurationManager.getModule("gui"));
		List<ConfigurationProperty> paneList = guiProperty
				.getProperties("viewPaneFactory.viewPane");
		for (ConfigurationProperty property : paneList) {
			ConfigurationProperty nameProperty = property.getProperty("name");
			if (nameProperty != null
					&& viewPaneName.equals(nameProperty.getValue())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Register a ViewPane with the ViewManager. ViewPanes must be subclasses of
	 * java.awt.Container
	 * 
	 * @param vp
	 * @throws ClassCastException
	 */
	public void addViewPane(ViewPane vp) throws ClassCastException {
		if (vp instanceof Container) {

			// Iterator _viewPaneIt = _viewPanes.iterator();
			// while (_viewPaneIt.hasNext()) {
			// ViewPane vpEle = (ViewPane)_viewPaneIt.next();
			// System.out.println("getContainer in _viewPanes:"+
			// vpEle.getParentFrame().getTableau().getContainer());
			// System.out.println("isMaster in _viewPanes:"+
			// vpEle.getParentFrame().getTableau().isMaster());
			// if (vpEle.getParentFrame().getTableau().getContainer()==null &&
			// vpEle.getParentFrame().getTableau().isMaster())
			// {
			// _viewPanes.remove(vpEle);
			// System.out.println("one element in _viewPanes is removed");
			// }
			//
			// }

			// for (ViewPane vpEle : _viewPanes)
			// {
			// System.out.println("getContainer in _viewPanes:"+
			// vpEle.getParentFrame().getTableau().getContainer());
			// System.out.println("isMaster in _viewPanes:"+
			// vpEle.getParentFrame().getTableau().isMaster());
			// if (vpEle.getParentFrame().getTableau().getContainer()==null &&
			// vpEle.getParentFrame().getTableau().isMaster())
			// {
			// _viewPanes.remove(vpEle);
			// System.out.println("one element in _viewPanes is removed");
			// }
			// }

			_viewPanes.add(vp);
			if (isDebugging)
				log.debug("_viewPanes:" + _viewPanes.size());
		} else {
			throw new ClassCastException(vp.getViewName()
					+ " ViewPane is not a subclass of java.awt.Container");
		}
	}

	/**
	 * Return a ViewPane reference for the given TableauFrame and viewName.
	 * 
	 * @param parent
	 * @param viewName
	 * */
	public ViewPane getViewPane(TableauFrame parent, String viewName) {
		viewName = viewName.trim();
		for (int i = 0; i < _viewPanes.size(); i++) {
			ViewPane pane = _viewPanes.elementAt(i);
			if (pane != null && pane.getParentFrame() == parent) {
				if (pane.getViewName().equals(viewName)) {
					if (pane instanceof Container) {
						return pane;
					}
				}
			} else if (pane == null) {
				_viewPanes.removeElementAt(i);
				i--;
			}
		}
		return null;
	}

	/**
	 * Return a vector of ViewPane objects for the specified TableauFrame.
	 * 
	 * @param parent
	 * */
	public Vector<ViewPane> getFrameViews(TableauFrame parent) {
		Vector<ViewPane> frameViewPanes = new Vector<ViewPane>();
		for (int i = 0; i < _viewPanes.size(); i++) {
			ViewPane pane = _viewPanes.elementAt(i);
			if (pane != null && pane.getParentFrame() == parent) {
				frameViewPanes.add(pane);
			} else if (pane == null) {
				_viewPanes.removeElementAt(i);
				if (isDebugging)
					log.debug("remove pane number " + i + " from _viewPanes because pane == null.");
				i--;
			}

		}
		return frameViewPanes;
	}

	/**
	 * Read in ViewPane locations of TabPanes from Config and add them.
	 * 
	 * &lt;viewPane&gt; &lt;name&gt;example&lt;/name&gt;
	 * &lt;viewPaneLocation&gt; &lt;name&gt;NORTH&lt;/name&gt; &lt;tabPane&gt;
	 * &lt;name&gt;tabname&lt;/name&gt; &lt;/tabPane&gt;
	 * &lt;/viewPaneLocation&gt; &lt;/viewPane&gt;
	 * 
	 * @throws Exception
	 */
	private void addConfiguredTabPanes(TableauFrame parent) throws Exception {
		TabManager tabman = TabManager.getInstance();
		ConfigurationProperty commonProperty = ConfigurationManager
				.getInstance().getProperty(
						ConfigurationManager.getModule("common"));

		// get the viewpanes
		List<ConfigurationProperty> viewPaneList = commonProperty
				.getProperties("viewPaneTabPanes.viewPane");
		// for each viewpane get the name
		for (int i = 0; i < viewPaneList.size(); i++) {
			ConfigurationProperty viewPaneProp = (ConfigurationProperty) viewPaneList
					.get(i);
			String viewPaneName = viewPaneProp.getProperty("name").getValue();
			// call getViewPane(parent, name)
			ViewPane theViewPane = getViewPane(parent, viewPaneName);
			// get the viewPaneLocation children of viewpane
			List<ConfigurationProperty> viewPaneLocationList = viewPaneProp
					.getProperties("viewPaneLocation");
			// for each viewpanelocation of viewpane
			for (int j = 0; j < viewPaneLocationList.size(); j++) {
				ConfigurationProperty viewPaneLocationProp = (ConfigurationProperty) viewPaneLocationList
						.get(j);
				// get viewpanelocation.name
				String viewPaneLocationName = viewPaneLocationProp.getProperty(
						"name").getValue();
				// check theViewPane.hasLocation(viewpanelocation.name)
				if (theViewPane != null
						&& !theViewPane.hasLocation(viewPaneLocationName)) {
					throw new Exception("The ViewPaneLocation, "
							+ viewPaneLocationName
							+ ", is not an available location of ViewPane, "
							+ viewPaneName);
				} else if (theViewPane != null) {
					// viewPaneContainer =
					// theViewPane.getLocationContainer(viewpanelocation.name)
					Container viewPaneContainer = theViewPane
							.getLocationContainer(viewPaneLocationName);
					List<ConfigurationProperty> tabPaneList = viewPaneLocationProp
							.getProperties("tabPane");
					// for each tabpane in viewpanelocation
					for (int k = 0; k < tabPaneList.size(); k++) {
						ConfigurationProperty tabPaneProperty = (ConfigurationProperty) tabPaneList
								.get(k);
						// get the name of the tabpane
						String tabPaneName = tabPaneProperty
								.getProperty("name").getValue();
						// System.out.println("getting tab pane: " +
						// tabPaneName);
						TabPane theTabPane = tabman.getTab(parent, tabPaneName);
						if (theTabPane == null) {
							System.out
									.println("ERROR: no tab named "
											+ tabPaneName
											+ " in the view "
											+ viewPaneName
											+ ". (Perhaps the tab's getTabName() does not match"
											+ " the name given in configuration.xml?)");
						} else {
							viewPaneContainer.add(theTabPane.getTabName(),
									(Component) theTabPane);
						}
					}
				}
			}
		}
	}

	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static ViewManager getInstance() {
		return ViewManagerHolder.INSTANCE;
	}

	/**
	 * Method for remove .
	 */
	public void removeOpenFrame(TableauFrame parent) {

		Object[] keyArray = _viewComboBoxes.keySet().toArray();
		for (int i = 0; i < keyArray.length; i++) {
			if (keyArray[i] == null) {
				if (isDebugging)
					log.debug("keyArray[i] == null");
			} else {
				TableauFrame tableauFrame = (TableauFrame) keyArray[i];
				if (isDebugging) {
					log.debug("getContainer in _viewAreas:"
							+ tableauFrame.getTableau().getContainer());
					log.debug("isMaster in _viewAreas:"
							+ tableauFrame.getTableau().isMaster());
				}
				// if (tableFrame.getTableau().getContainer()==null &&
				// tableFrame.getTableau().isMaster())
				if (tableauFrame == parent) {
					_viewAreas.remove(tableauFrame);
					_viewComboBoxes.remove(tableauFrame);
					if (isDebugging) {
						log.debug("one element is in _viewAreas removed:" + i);
						log.debug("one element is in _viewComboBoxes removed:"
								+ i);
					}
				}
			}
		}

		if (isDebugging) {
			log.debug("the size of _viewAreas after removing:"
					+ _viewAreas.size());
			log.debug("the size of _viewComboBoxes after removing:"
					+ _viewComboBoxes.size());
		}

		for (int i = 0; i < _viewPanes.size(); i++) {
			ViewPane pane = _viewPanes.elementAt(i);
			if (pane != null && pane.getParentFrame() == parent) {
				pane.setParentFrame(null);
				_viewPanes.removeElementAt(i);
				if (isDebugging)
					log.debug("one element is in _viewPanes removed:" + i);
				i--;
			} else if (pane == null) {
				_viewPanes.removeElementAt(i);
				if (isDebugging)
					log.debug("one element is in _viewPanes removed 2:" + i);
				i--;
			}

		}

		if (isDebugging)
			log.debug("the size of _viewPanes after removing:"
					+ _viewPanes.size());

	}

	private static class ViewManagerHolder {
		private static final ViewManager INSTANCE = new ViewManager();
	}

	private class ViewComboBoxListener implements ActionListener {

		/** Action for changing the view. */
		public void actionPerformed(ActionEvent e) {
			Object c = e.getSource();
			if (c instanceof JComboBox) {
				JComboBox jc = (JComboBox) c;
				Object s = jc.getSelectedItem();
				if (s instanceof String) {
					String viewName = (String) s;
					// NOTE: we need to get the parent TableauFrame here instead
					// of Window, since the detached toolbar is a Window.
					TableauFrame frame = GUIUtil.getParentTableauFrame(jc);
					if (frame == null) {
						System.out
								.println("ERROR: could not find parent tableau frame.");
					} else {
						showView(frame, viewName);
					}
				}
			}
		}
	}
}
