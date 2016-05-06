/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-09-14 17:00:20 -0700 (Fri, 14 Sep 2012) $' 
 * '$Revision: 30685 $'
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

package org.kepler.gui.popups;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.SQLException;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.AnnotatedPTree;
import org.kepler.gui.AnnotatedPTreePopupListener;
import org.kepler.gui.KeplerGraphFrame;
import org.kepler.gui.ShowDocumentationAction;
import org.kepler.moml.DownloadableKAREntityLibrary;
import org.kepler.moml.FolderEntityLibrary;
import org.kepler.moml.KAREntityLibrary;
import org.kepler.moml.KARErrorEntityLibrary;
import org.kepler.moml.OntologyEntityLibrary;
import org.kepler.moml.RemoteKARErrorEntityLibrary;
import org.kepler.moml.RemoteRepositoryEntityLibrary;
import org.kepler.objectmanager.library.LibIndex;
import org.kepler.objectmanager.library.LibItem;
import org.kepler.objectmanager.library.LibraryManager;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.PtolemyFrame;
import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.moml.EntityLibrary;
import ptolemy.util.MessageHandler;
import ptolemy.vergil.basic.BasicGraphFrame;

public class LibraryPopupListener extends AnnotatedPTreePopupListener {

	private static final Log log = LogFactory.getLog(LibraryPopupListener.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public LibraryPopupListener(AnnotatedPTree aptree) {
		super(aptree);
	}

	/**
	 * Description of the Method
	 * 
	 *@param e
	 *            Description of the Parameter
	 */
	public void mouseReleased(MouseEvent e) {
		maybeShowPopup(e);
	}

	/**
	 * 
	 * @param selPath
	 * @param e
	 */
	private void handlePopupInsideKar(TreePath selPath, MouseEvent e) {

		Object ob = selPath.getLastPathComponent();
		if (isDebugging)
			log.debug(ob.getClass().getName());

		if (ob instanceof FolderEntityLibrary) {
			// User clicked on a Folder inside of a KAR
			KARFolderPopup kfp = new KARFolderPopup(selPath, _aptree
					.getParentComponent());
			kfp.initialize();
			_aptree.add(kfp);
			_aptree.setSelectionPath(selPath);
			kfp.show(e.getComponent(), e.getX(), e.getY());

		} else if (!(ob instanceof ptolemy.moml.EntityLibrary)) {
			// User clicked on a Component inside a KAR
			KARComponentPopup kcp = new KARComponentPopup(selPath, _aptree
					.getParentComponent());
			kcp.initialize();
			_aptree.add(kcp);
			_aptree.setSelectionPath(selPath);
			kcp.show(e.getComponent(), e.getX(), e.getY());

		}
	}

	/**
	 * 
	 * @param selPath
	 * @param e
	 */
	private void handlePopupOutsideKar(TreePath selPath, MouseEvent e) {

		Object ob = selPath.getLastPathComponent();
		if (isDebugging)
			log.debug(ob.getClass().getName());

		if (ob instanceof KAREntityLibrary) {
			// User clicked on a KAR
			if (isDebugging)
				log.debug("KAREntityLibrary selected");
			KARPopup kp = new KARPopup(selPath, _aptree.getParentComponent());
			kp.initialize();
			_aptree.add(kp);
			_aptree.setSelectionPath(selPath);
			kp.show(e.getComponent(), e.getX(), e.getY());
		} else if (ob instanceof DownloadableKAREntityLibrary) {
			DownloadableKARPopup kp = new DownloadableKARPopup(selPath, _aptree.getParentComponent());
			kp.initialize();
			_aptree.add(kp);
			_aptree.setSelectionPath(selPath);
			kp.show(e.getComponent(), e.getX(), e.getY());
		} else if (ob instanceof RemoteRepositoryEntityLibrary) {
//			System.out.println("Context click on RREL");

		} else if (ob instanceof FolderEntityLibrary) {
			// User clicked on a Folder
			FolderPopup fp = new FolderPopup(selPath, _aptree
					.getParentComponent());
			fp.initialize();
			_aptree.add(fp);
			_aptree.setSelectionPath(selPath);
			fp.show(e.getComponent(), e.getX(), e.getY());

		} else if (ob instanceof OntologyEntityLibrary) {
			// User clicked on an ontology
			OntologyPopup op = new OntologyPopup(selPath, _aptree
					.getParentComponent());
			op.initialize();
			_aptree.add(op);
			_aptree.setSelectionPath(selPath);
			op.show(e.getComponent(), e.getX(), e.getY());

		} else if (ob instanceof KARErrorEntityLibrary) {
			KARErrorPopup kep = new KARErrorPopup(selPath, _aptree
					.getParentComponent());
			kep.initialize();
			_aptree.add(kep);
			_aptree.setSelectionPath(selPath);
			kep.show(e.getComponent(), e.getX(), e.getY());

		} else if (ob instanceof RemoteKARErrorEntityLibrary) {
			RemoteKARErrorPopup kep = new RemoteKARErrorPopup(selPath, _aptree
					.getParentComponent());
			kep.initialize();
			_aptree.add(kep);
			_aptree.setSelectionPath(selPath);
			kep.show(e.getComponent(), e.getX(), e.getY());

		} else if (!(ob instanceof ptolemy.moml.EntityLibrary)) {
			// User clicked on a Component
			if (!(ob instanceof NamedObj)) {
				// Shouldn't happen
				return;
			}
			NamedObj no = (NamedObj) ob;
			String alternateLibraryPopupClassName = _getAlternateLibraryPopupClassName(no);
			if (alternateLibraryPopupClassName == null) {
				//System.out.println("Using default context menu stuff for class: " + ob.getClass().getName());
				OntologyComponentPopup ocp = new OntologyComponentPopup(
						selPath, _aptree.getParentComponent());
				ocp.initialize();
				_aptree.add(ocp);
				_aptree.setSelectionPath(selPath);
				ocp.show(e.getComponent(), e.getX(), e.getY());
			} else {
				try {
					Class<?> libraryPopupClass = Class
							.forName(alternateLibraryPopupClassName);
					Object object = libraryPopupClass.newInstance();
					Method getPopupMethod = libraryPopupClass.getMethod(
							"getPopup", JTree.class, MouseEvent.class,
							TreePath.class, Component.class);
					getPopupMethod.invoke(object, _aptree, e, selPath, _aptree
							.getParentComponent());
				} catch (Exception w) {
					log.error("Error creating alternateGetLibraryPopup!", w);
				}
			}
		}
	}
	
	/** Handle a double-click action. */
	private void handleDoubleClickOutsideKar(TreePath selPath, MouseEvent event) {
	       Object ob = selPath.getLastPathComponent();

	       if (ob instanceof EntityLibrary) {
	            return;
	       } else {

	            int liid = LibraryManager.getLiidFor((ComponentEntity) ob);
	            LibItem li = null;
                try {
                    li = LibraryManager.getInstance().getPopulatedLibItem(liid);
                } catch (SQLException e) {
                    MessageHandler.error("Error accessinc library item.", e);
                    return;
                }
                
                // open it if it's a MoML
	            String filePath = li.getAttributeValue(LibIndex.ATT_XMLFILE);
	            if(filePath != null) {
	                Component component = _aptree.getParentComponent();
	                while (component != null && !(component instanceof TableauFrame)) {
	                    component = component.getParent();
	                }
	                if(component == null) {
	                    MessageHandler.error("Could not find TableauFrame.");
	                    return;
	                }
	                Configuration configuration = ((TableauFrame) component).getConfiguration();
	                try {
	                    URL url = new File(filePath).toURI().toURL();
	                    configuration.openModel(url, url, url.toExternalForm());
	                } catch(Exception e) {
	                    MessageHandler.error("Error opening " + filePath, e);
	                }
	                
	                // if we successfully opened a file, update the history menu and
	                // set the last directory.

	                // update the history menu
                    if(component instanceof KeplerGraphFrame) {
                        try {
                            ((KeplerGraphFrame)component).updateHistory(filePath);
                        } catch (IOException exception) {
                            MessageHandler.error("Unable to update history menu.", exception);
                        }
                    }
                    if(component instanceof BasicGraphFrame) {
                        ((BasicGraphFrame)component).setLastDirectory(new File(filePath).getParentFile());
                    }
                    
	            } else if(li.getLsid() != null) {
	                // if it has an lsid, show the documentation
	                Component component = _aptree.getParentComponent();
                    while (component != null && !(component instanceof PtolemyFrame)) {
                        component = component.getParent();
                    }
                    if(component == null) {
                        MessageHandler.error("Could not find TableauFrame.");
                        return;
                    }
                    ShowDocumentationAction action = new ShowDocumentationAction(selPath, _aptree.getParentComponent());
	                action.setLsidToView(li.getLsid());
	                action.setPtolemyFrame((PtolemyFrame) component);

	                action.actionPerformed(new ActionEvent(this,
	                        ActionEvent.ACTION_FIRST, "open"));
	            }
	        }
	}

	/**
	 * Description of the Method
	 * 
	 *@param e
	 *            Description of the Parameter
	 */
	private void maybeShowPopup(MouseEvent e) {
		if (isDebugging)
			log.debug("maybeShowPopup(" + e.toString() + ")");
		if (e.isPopupTrigger() || _trigger) {
			_trigger = false;
			TreePath selPath = _aptree.getPathForLocation(e.getX(), e.getY());
			if (isDebugging)
				log.debug(selPath.toString());
			if ((selPath != null)) {

				if (isDebugging)
					log.debug(selPath.getLastPathComponent().getClass().getName());

				if (_isPathInsideKAR(selPath)) {
					handlePopupInsideKar(selPath, e);
				} else {
					handlePopupOutsideKar(selPath, e);

				}
			}
		// handle double clicks
		} else if(e.getClickCount() == 2) {
		    TreePath selPath = _aptree.getPathForLocation(e.getX(), e.getY());
            if (isDebugging)
                log.debug(selPath.toString());
            if ((selPath != null)) {

                if (isDebugging)
                    log.debug(selPath.getLastPathComponent().getClass().getName());

                if (_isPathInsideKAR(selPath)) {
                    handlePopupInsideKar(selPath, e);
                } else {
                    handleDoubleClickOutsideKar(selPath, e);
                }
            }
		}
	}
	
	
    /** Determine if this object is contained within a KAR
     *  by checking all of the parent objects to see if they
     *  are a KAREntityLibrary. do not check the object itself
	 */
	private boolean _isPathInsideKAR(TreePath selPath) {
        for (int i = (selPath.getPathCount() - 2); i >= 0; i--) {
            if (selPath.getPathComponent(i) instanceof KAREntityLibrary) {
                return true;
            }
        }
        return false;
	}

	private String _getAlternateLibraryPopupClassName(NamedObj namedObj) {
		Attribute attribute = namedObj
				.getAttribute(AnnotatedPTree.ALTERNATE_LIBRARY_POPUP_ATTRIBUTE_NAME);
		if (attribute == null) {
			return null;
		}
		try {
			StringAttribute sa = (StringAttribute) attribute;
			return sa.getExpression();
		} catch (ClassCastException ex) {
			log.warn(AnnotatedPTree.ALTERNATE_LIBRARY_POPUP_ATTRIBUTE_NAME
					+ " should be a StringAttribute"
					+ " specifying a class that should"
					+ " handle context popups from this object", ex);
		}
		return null;
	}
	
	public void closing() {
		
	}
	
}

