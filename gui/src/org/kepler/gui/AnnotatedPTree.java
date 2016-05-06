/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-03-14 10:52:19 -0700 (Wed, 14 Mar 2012) $' 
 * '$Revision: 29554 $'
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.moml.DownloadableKAREntityLibrary;
import org.kepler.moml.FolderEntityLibrary;
import org.kepler.moml.KAREntityLibrary;
import org.kepler.moml.KARErrorEntityLibrary;
import org.kepler.moml.OntologyEntityLibrary;
import org.kepler.moml.RemoteKARErrorEntityLibrary;
import org.kepler.moml.RemoteRepositoryEntityLibrary;
import org.kepler.moml.SearchEntityLibrary;
import org.kepler.util.StaticResources;

import ptolemy.actor.gui.Configuration;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.moml.EntityLibrary;
import ptolemy.vergil.tree.PTree;
import ptolemy.vergil.tree.PtolemyTreeCellRenderer;

/**
 * A Wrapper class that adds right click support and custom Kepler Ontology
 * icons to a PTree
 * 
 * @author Chad Berkley
 * @since February 17, 2005
 * @version $Id: AnnotatedPTree.java 29554 2012-03-14 17:52:19Z crawl $
 * @since Kepler 1.0
 */
public class AnnotatedPTree extends PTree {
	private static final Log log = LogFactory.getLog(AnnotatedPTree.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public static final String ALTERNATE_LIBRARY_POPUP_ATTRIBUTE_NAME = "_alternateGetPopupAction";

	private Vector<WeakReference<ActionListener>> listeners;
	private Component parent;
	Configuration config = null;
	private boolean showRootIcon = true;
	public boolean isSearch = false;

	private MouseListener mouseListener = null;

	/**
	 * Constructor
	 * 
	 * @param model
	 *            the model to build the tree out of
	 */
	public AnnotatedPTree(TreeModel model, Component parent) {
		super(model);
		setParentComponent(parent);
	}

	public AnnotatedPTree(TreeModel model, Component parent,
			boolean showRootIcon) {
		super(model);
		setParentComponent(parent);
		setShowRootIcon(showRootIcon);
	}

	public void setShowRootIcon(boolean showRootIcon) {
		this.showRootIcon = showRootIcon;
	}

	/**
	 * To use a different Mouse listener set it here before calling
	 * initAnnotatedPTree();
	 * 
	 * @param listener
	 */
	public void setMouseListener(MouseListener listener) {
		this.mouseListener = listener;
		this.addMouseListener(mouseListener);
	}

	public Component getParentComponent() {
		return this.parent;
	}

	public void setParentComponent(Component parent) {
		this.parent = parent;
	}

	public void initAnotatedPTree() {
		if (isDebugging)
			log.debug("initAnotatedPTree()");
		if (mouseListener == null) {
			MouseListener popupListener = new AnnotatedPTreePopupListener(this);
			setMouseListener(popupListener);
		}
		listeners = new Vector<WeakReference<ActionListener>>();
		this.setShowsRootHandles(true);

		// On Windows the row height is too short
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Win")) {
			int rowHeight = this.getRowHeight();
			this.setRowHeight(rowHeight + 2);
		}

		OntologyTreeCellRenderer otcr = new OntologyTreeCellRenderer(
				showRootIcon);
		this.setCellRenderer(otcr);
	}

	/**
	 * add a listener for events
	 */
	public void addListener(ActionListener listener) {
		listeners.addElement(new WeakReference(listener));
	}

	/**
	 * notify this tree that the mouse is dragging an object over the x,y
	 * position
	 */
	public void notifyDragOver(int x, int y) {
		TreePath location = this.getClosestPathForLocation(x, y);
		this.clearSelection();
		this.setSelectionPath(location);
	}

	/**
	 * notify all listeners of events
	 */
	private void notifyListeners(ActionEvent event) {
		for (int i = 0; i < listeners.size(); i++) {
			ActionListener listener = (ActionListener) listeners.elementAt(i);
			listener.actionPerformed(event);
		}
	}

	/**
	 * Listener used to changes from the NewFolderFrame
	 */
	private class ActionHandler implements ActionListener {
		/**
		 * Description of the Method
		 * 
		 * @param event
		 *            Description of Parameter
		 */
		public void actionPerformed(ActionEvent event) {
			String command = event.getActionCommand();
			if (command.equals("new_folder_created")
					|| command.equals("new_actor_created")) {
				// notify the TabbedLibraryPane that a changeRequest needs to be
				// filed
				notifyListeners(event);
			}
		}
	}
}

class OntologyTreeCellRenderer extends PtolemyTreeCellRenderer {
	private static final Log log = LogFactory
			.getLog(OntologyTreeCellRenderer.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private ImageIcon _ontIconClosed, _ontIconOpen, _generalIcon, _searchIcon,
			_folderIconClosed, _folderIconOpen, _packIconOpen, _packIconClosed,
			_packIconError, _remoteIcon;

	private final String CAT_IMG = StaticResources.getSettingsString(
			"ONTOL_CLASS_TREEICON_PATH", "");

	private final String ONT_IMG_OPEN = StaticResources.getSettingsString(
			"ONTOLOGY_TREEICON_OPEN_PATH", "");

	private final String ONT_IMG_CLOSED = StaticResources.getSettingsString(
			"ONTOLOGY_TREEICON_CLOSED_PATH", "");

	private final String SEARCH_IMG = StaticResources.getSettingsString(
			"MAGNIFIER_SILKICON_PATH", "");

	private final String FOL_IMG_OPEN = StaticResources.getSettingsString(
			"FOLDER_TREEICON_OPEN_PATH", "");

	private final String FOL_IMG_CLOSED = StaticResources.getSettingsString(
			"FOLDER_TREEICON_OPEN_PATH", "");

	private final String PACK_IMG_OPEN = StaticResources.getSettingsString(
			"PACKAGE_SILKICON_OPEN_PATH", "");
	private final String PACK_IMG_CLOSED = StaticResources.getSettingsString(
			"PACKAGE_SILKICON_CLOSED_PATH", "");
	private final String PACK_IMG_ERROR = StaticResources.getSettingsString(
			"PACKAGE_SILKICON_ERROR_PATH", "");
	private final String REMOTE_IMG = StaticResources.getSettingsString(
			"REMOTE_TREEICON_PATH", "");

	/**
	 * Constructor - creates a new TreeCellRenderer
	 */
	public OntologyTreeCellRenderer() {
		this(true);
	}

	public OntologyTreeCellRenderer(boolean showIcons) {
		if (isDebugging)
			log.debug("OntologyTreeCellRenderer(" + showIcons + ")");

		URL catImgURL = null;
		// OntologyTreeCellRenderer.class.getResource(CAT_IMG);
		if (catImgURL != null) {
			_generalIcon = new ImageIcon(catImgURL);
		}
		URL searchImgURL = OntologyTreeCellRenderer.class
				.getResource(SEARCH_IMG);
		if (searchImgURL != null) {
			_searchIcon = new ImageIcon(searchImgURL);
		}
		if (showIcons) {
			URL ontImgURLOpen = OntologyTreeCellRenderer.class
					.getResource(ONT_IMG_OPEN);
			if (ontImgURLOpen != null) {
				_ontIconOpen = new ImageIcon(ontImgURLOpen);
			}
			URL ontImgURLClosed = OntologyTreeCellRenderer.class
					.getResource(ONT_IMG_CLOSED);
			if (ontImgURLClosed != null) {
				_ontIconClosed = new ImageIcon(ontImgURLClosed);
			}
			URL folImgURLOpen = OntologyTreeCellRenderer.class
					.getResource(FOL_IMG_OPEN);
			if (folImgURLOpen != null) {
				_folderIconOpen = new ImageIcon(folImgURLOpen);
			}
			URL folImgURLClosed = OntologyTreeCellRenderer.class
					.getResource(FOL_IMG_CLOSED);
			if (folImgURLClosed != null) {
				_folderIconClosed = new ImageIcon(folImgURLClosed);
			}
			URL packImgURLOpen = OntologyTreeCellRenderer.class
					.getResource(PACK_IMG_OPEN);
			if (packImgURLOpen != null) {
				_packIconOpen = new ImageIcon(packImgURLOpen);
			}
			URL packImgURLClosed = OntologyTreeCellRenderer.class
					.getResource(PACK_IMG_CLOSED);
			if (packImgURLClosed != null) {
				_packIconClosed = new ImageIcon(packImgURLClosed);
			}
			URL packImgURLError = OntologyTreeCellRenderer.class
					.getResource(PACK_IMG_ERROR);
			if (packImgURLError != null) {
				_packIconError = new ImageIcon(packImgURLError);
			}
			URL remoteImgURL = OntologyTreeCellRenderer.class
					.getResource(REMOTE_IMG);
			if (remoteImgURL != null) {
				_remoteIcon = new ImageIcon(remoteImgURL);
			}
		} else {
			_ontIconOpen = new ImageIcon(catImgURL);
		}

	}

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		if (isDebugging) {
			log.debug("getTreeCellRendererComponent("
					+ tree.getClass().getName() + ", "
					+ value.getClass().getName() + ", " + selected + ", "
					+ expanded + ", " + leaf + ", " + row + ", " + hasFocus
					+ ")");
			log.debug(tree.getShowsRootHandles());
		}

		if (value instanceof ptolemy.moml.EntityLibrary) {
			EntityLibrary el = (EntityLibrary) value;
			if (isDebugging) {
				log.debug(el.getName() + " " + el.getClass().getName());
			}

			if (el instanceof KAREntityLibrary) {
				setOpenIcon(_packIconOpen);
				setClosedIcon(_packIconClosed);
			} else if (el instanceof FolderEntityLibrary) {
				setOpenIcon(_folderIconOpen);
				setClosedIcon(_folderIconClosed);
			} else if (el instanceof OntologyEntityLibrary) {
				setOpenIcon(_ontIconOpen);
				setClosedIcon(_ontIconClosed);
			} else if (el instanceof SearchEntityLibrary) {
				setOpenIcon(_searchIcon);
				setClosedIcon(_searchIcon);
			} else if (el instanceof KARErrorEntityLibrary || el instanceof RemoteKARErrorEntityLibrary) {
				setOpenIcon(_packIconError);
				setClosedIcon(_packIconError);
			} else if (el instanceof DownloadableKAREntityLibrary) {
				// Icons for the KAR files - trash cans?
				setOpenIcon(_packIconOpen);
				setClosedIcon(_packIconClosed);				
			} else if (el instanceof RemoteRepositoryEntityLibrary) {
				// Icons for the Remote repositories - computers
				setOpenIcon(_remoteIcon);
				setClosedIcon(_remoteIcon);
				// TODO: Also make sure that the top-level "Remote Components" is a globe
			} else {
				setOpenIcon(_generalIcon);
				setClosedIcon(_generalIcon);
			}
		} else {
			if (isDebugging)
				log.debug("set general icon");
			setOpenIcon(_generalIcon);
			setClosedIcon(_generalIcon);
		}
		Component c = super.getTreeCellRendererComponent(tree, value, selected,
				expanded, leaf, row, hasFocus);
		
		// if the object is a NamedObj, use display name for the label
		// since the display name is used on the canvas. the component returned
		// by super.getTreeCellRenderer() uses the display name for settables.
		if(!(value instanceof Settable) && (value instanceof NamedObj) &&
		        (c instanceof DefaultTreeCellRenderer)) {
		    ((DefaultTreeCellRenderer)c).setText(((NamedObj)value).getDisplayName());
		}
		
		if (isDebugging)
			log.debug("Component: " + c.getClass().getName());
		return c;
	}

	/**
	 * Sets the icon used to represent non-leaf nodes that are expanded.
	 */
	public void setOpenIcon(Icon newIcon) {
		if (newIcon != null) {
			super.setOpenIcon(newIcon);
		} else {
			// super.setOpenIcon(UIManager.getIcon("Tree.openIcon"));
			super.setOpenIcon(null);
		}
	}

	/**
	 * Sets the icon used to represent non-leaf nodes that are not expanded.
	 */
	public void setClosedIcon(Icon newIcon) {
		if (newIcon != null) {
			super.setClosedIcon(newIcon);
		} else {
			// super.setClosedIcon(UIManager.getIcon("Tree.closeIcon"));
			super.setClosedIcon(null);
		}
	}
}