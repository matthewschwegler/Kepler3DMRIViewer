/*
 * Copyright (c) 2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-03-14 11:43:34 -0700 (Wed, 14 Mar 2012) $' 
 * '$Revision: 29555 $'
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
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.AnnotatedPTree;
import org.kepler.gui.AnnotatedPTreePopupListener;
import org.kepler.moml.KAREntityLibrary;

import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;

/** A mouse listener for the Outline view.
 * 
 * @author Philippe Huynh
 *
 */

public class OutlinePopupListener extends AnnotatedPTreePopupListener {

	private static final Log log = LogFactory.getLog(OutlinePopupListener.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public OutlinePopupListener(AnnotatedPTree aptree) {
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
	private void handlePopupOutsideKar(TreePath selPath, MouseEvent e) {

		Object ob = selPath.getLastPathComponent();
		if (isDebugging)
			log.debug(ob.getClass().getName());

		if (ob instanceof NamedObj) {

		    NamedObj no = (NamedObj) ob;
			String alternateLibraryPopupClassName = _getAlternateLibraryPopupClassName(no);
			if (alternateLibraryPopupClassName == null) {                               
                    OutlineComponentPopup kcp = new OutlineComponentPopup(selPath,
                        _aptree.getParentComponent());
			        kcp.initialize();
			        _aptree.add(kcp);
			        _aptree.setSelectionPath(selPath);
			        kcp.show(e.getComponent(), e.getX(), e.getY());
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

				// determine if this object is contained within a KAR
				// by checking all of the parent objects to see
				// if they are a KAREntityLibrary
				// do not check the object itself
				boolean inKAR = false;
				Object ob = null;
				for (int i = (selPath.getPathCount() - 2); i >= 0; i--) {
					ob = selPath.getPathComponent(i);
					if (ob instanceof KAREntityLibrary) {
						inKAR = true;
						break;
					}
				}

				ob = selPath.getLastPathComponent();
				if (isDebugging)
					log.debug(ob.getClass().getName());

				if (!inKAR) {
					handlePopupOutsideKar(selPath, e);
				}
			}
		}
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
