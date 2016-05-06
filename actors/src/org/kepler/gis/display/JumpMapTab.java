/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: welker $'
 * '$Date: 2010-05-05 22:21:26 -0700 (Wed, 05 May 2010) $' 
 * '$Revision: 24234 $'
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

package org.kepler.gis.display;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.LayerManager;
import com.vividsolutions.jump.workbench.model.LayerTreeModel;
import com.vividsolutions.jump.workbench.ui.ErrorHandler;
import com.vividsolutions.jump.workbench.ui.LayerNamePanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanel;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelContext;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelListener;
import com.vividsolutions.jump.workbench.ui.LayerViewPanelProxy;
import com.vividsolutions.jump.workbench.ui.TreeLayerNamePanel;
import com.vividsolutions.jump.workbench.ui.Viewport;
import com.vividsolutions.jump.workbench.ui.WorkbenchToolBar;
import com.vividsolutions.jump.workbench.ui.zoom.PanTool;
import com.vividsolutions.jump.workbench.ui.zoom.ZoomTool;

/**
 * Name: JumpMapTab.java Purpose:A customized GUI simailar to JUMP workbench for
 * displaying vector GIS data. It was painful to find that we can only use the
 * built in class of ZoomTool and PanTool becasue they do not refer to
 * JUMPWokrbench class directly. All other frequently used tools refer to
 * JUMPWokrbench class while this is not. Thus we have to add our own tool icons
 * and redict the three map operation requests (zoom to previous extent, zoom to
 * next extent and zoom to full extent) to Viewport through WorkbenchContext.
 * Known problem(s): We could have used images instead of texts for the three
 * map operation buttons (zoom to previous extent, zoom to next extent and zoom
 * to full extent). But I couldn't find a way to refer to the images in JUMP
 * package. Knwon problem: c.f. ENMPCPFrame.java Author: Jianting Zhang Date:
 * August, 2005
 */
public class JumpMapTab extends JPanel implements LayerViewPanelContext {

	private JLabel statusLabel = new JLabel();
	private ErrorHandler errorHandler;
	private JumpToolBar usrToolBar = new JumpToolBar();
	private WorkbenchToolBar sysToolBar = new WorkbenchToolBar(
			new LayerViewPanelProxy() {
				public LayerViewPanel getLayerViewPanel() {
					return layerViewPanel;
				}
			});
	private LayerManager layerManager = new LayerManager();
	private TreeLayerNamePanel layerNamePanel;
	private LayerViewPanel layerViewPanel;
	private JPanel toolbarPanel = new JPanel();

	private WorkbenchContext context = new WorkbenchContext() {
		public ErrorHandler getErrorHandler() {
			return JumpMapTab.this;
		}

		public LayerNamePanel getLayerNamePanel() {
			return layerNamePanel;
		}

		public LayerViewPanel getLayerViewPanel() {
			return layerViewPanel;
		}

		public LayerManager getLayerManager() {
			return layerManager;
		}

	};

	/**
	 * Gets the workbenchContext attribute of the JumpMapTab object
	 * 
	 *@return The workbenchContext value
	 */
	public WorkbenchContext getWorkbenchContext() {
		return context;
	}

	/**
	 * Constructor for the JumpMapTab object
	 * 
	 *@param errorHandler
	 *            Description of the Parameter
	 */
	public JumpMapTab(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
		layerViewPanel = new LayerViewPanel(layerManager, this);
		layerNamePanel = new TreeLayerNamePanel(layerViewPanel,
				new LayerTreeModel(layerViewPanel), layerViewPanel
						.getRenderingManager(), new HashMap());

		try {
			jbInit();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		layerViewPanel.addListener(new LayerViewPanelListener() {
			public void painted(Graphics graphics) {
			}

			public void selectionChanged() {
			}

			public void cursorPositionChanged(String x, String y) {
				setStatusMessage("(" + x + ", " + y + ")");
			}
		});
	}

	/**
	 * Description of the Method
	 * 
	 *@param t
	 *            Description of the Parameter
	 */
	public void handleThrowable(final Throwable t) {
		errorHandler.handleThrowable(t);
	}

	/**
	 * Description of the Method
	 * 
	 *@param warning
	 *            Description of the Parameter
	 */
	public void warnUser(String warning) {
		setStatusMessage(warning);
	}

	/**
	 * Sets the statusMessage attribute of the JumpMapTab object
	 * 
	 *@param message
	 *            The new statusMessage value
	 */
	public void setStatusMessage(String message) {
		// Make message at least a space so that status bar won't collapse [Jon
		// Aquino]
		statusLabel
				.setText(((message == null) || (message.length() == 0)) ? " "
						: message);
	}

	/**
	 * Description of the Method
	 * 
	 *@exception Exception
	 *                Description of the Exception
	 */
	void jbInit() throws Exception {

		this.setLayout(new BorderLayout());

		toolbarPanel.setLayout(new GridBagLayout());
		toolbarPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		GridBagConstraints ct = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
						0, 0, 0, 0), 0, 0);
		toolbarPanel.add(sysToolBar, ct);
		toolbarPanel.add(usrToolBar);
		add(toolbarPanel, BorderLayout.NORTH);

		layerViewPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		layerNamePanel.setBorder(BorderFactory.createLoweredBevelBorder());

		JSplitPane _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				true);
		_splitPane.setLeftComponent(layerNamePanel);
		_splitPane.setRightComponent(layerViewPanel);
		_splitPane.setOneTouchExpandable(true);
		add(_splitPane, BorderLayout.CENTER);

		statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
		statusLabel.setText(" ");
		add(statusLabel, BorderLayout.SOUTH);
	}

	/**
	 * Description of the Method
	 * 
	 *@exception Exception
	 *                Description of the Exception
	 */
	public void initialize() throws Exception {
		sysToolBar.addCursorTool("Zoom In/Out", new ZoomTool());
		sysToolBar.addCursorTool("Pan", new PanTool());
	}

	/**
	 * Description of the Class
	 * 
	 *@author berkley
	 *@created June 9, 2005
	 */
	private class JumpToolBar extends JPanel implements ActionListener {
		JButton zoom, pan, zoomprev, zoomnext, zoomextent;

		/** Constructor for the JumpToolBar object */
		JumpToolBar() {

			// zoomprev =new JButton(new ImageIcon("Prev.gif"));
			zoomprev = new JButton("Prev");
			zoomprev.addActionListener(JumpToolBar.this);
			// zoomnext =new JButton(new ImageIcon("Right.gif"));
			zoomnext = new JButton("Next");
			zoomnext.addActionListener(JumpToolBar.this);
			// zoomextent=new JButton(new ImageIcon("World.gif"));
			zoomextent = new JButton("Full");
			zoomextent.addActionListener(JumpToolBar.this);
			add(zoomprev);
			add(zoomnext);
			add(zoomextent);
		}

		/**
		 * Description of the Method
		 * 
		 *@param e
		 *            Description of the Parameter
		 */
		public void actionPerformed(ActionEvent e) {
			try {
				if (e.getSource() == zoomnext) {
					Viewport viewport = context.getLayerViewPanel()
							.getViewport();
					if (viewport.getZoomHistory().hasNext()) {
						viewport.getZoomHistory().setAdding(false);
						viewport.zoom(viewport.getZoomHistory().next());
						viewport.getZoomHistory().setAdding(true);
					}
				} else if (e.getSource() == zoomprev) {
					Viewport viewport = context.getLayerViewPanel()
							.getViewport();
					if (viewport.getZoomHistory().hasPrev()) {
						viewport.getZoomHistory().setAdding(false);
						viewport.zoom(viewport.getZoomHistory().prev());
						viewport.getZoomHistory().setAdding(true);
					}
				} else if (e.getSource() == zoomextent) {
					context.getLayerViewPanel().getViewport()
							.zoomToFullExtent();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}