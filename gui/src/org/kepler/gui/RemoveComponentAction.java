/*
 * Copyright (c) 2007-2010 The Regents of the University of California.
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

package org.kepler.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

import org.kepler.moml.NamedObjId;
import org.kepler.objectmanager.ActorMetadata;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.PtolemyFrame;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.basic.BasicGraphFrame;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * This action allows an actor to be removed from the tree Created to display
 * when actor in library tree is right clicked.
 * 
 *@author Chad Berkley
 *@since 3/13/2008
 */
public class RemoveComponentAction extends FigureAction {
	private final static String LABEL = "Remove Component";
	private TreePath path;
	private Component parent;
	//private NewActorFrame naFrame;
	//private GetDocumentationAction gda;
	private PtolemyFrame pFrame = null;

	/**
	 * Constructor
	 * 
	 *@param path
	 *            the TreePath where the actor is being removed.
	 */
	public RemoveComponentAction(TreePath path, Component parent) {
		super(LABEL);
		this.path = path;
		this.parent = parent;
	}

	/**
	 * set the config
	 */
	public void setConfiguration(Configuration config) {
	}

	/**
	 * Invoked when an action occurs.
	 * 
	 *@param e
	 *            ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		String msg = "This will remove the component from the tree entirely.  "
				+ "Are you sure you want to do this?";
		int userInput = JOptionPane.showConfirmDialog(null, msg, "Warning",
				JOptionPane.YES_NO_OPTION);
		if (userInput == JOptionPane.NO_OPTION) {
			return;
		}
		Component current = parent;
		while (parent != null && !(parent instanceof BasicGraphFrame)) {
			parent = current.getParent();
			current = parent;
		}
		Component comp = (Component) e.getSource();
		NamedObj actor = null;
		ActorMetadata am = null;
		ComponentEntity ce = (ComponentEntity) path.getLastPathComponent();
		try {
			CacheManager cacheMan = CacheManager.getInstance();
			String lsidString = ((NamedObjId) (ce.getAttribute("entityId")))
					.getExpression();
			ActorCacheObject aco = (ActorCacheObject) cacheMan
					.getObject(new KeplerLSID(lsidString));
			am = aco.getMetadata();
      GraphicalActorMetadata gam = new GraphicalActorMetadata(am);
			actor = gam.getActorAsNamedObj(null);
			// System.out.println("removing actor: " + actor.getName());

			cacheMan.removeObject(aco.getLSID());

			/*
			 * Hashtable semTypeHash = am.getSemanticTypeHash(); Enumeration
			 * keys = semTypeHash.keys(); while(keys.hasMoreElements()) { String
			 * key = (String)keys.nextElement(); String value =
			 * (String)semTypeHash.get(key); am.removeSemanticType(key);
			 * System.out.println("semtype: " + key + " : " + value); }
			 */

			LibraryManager library = LibraryManager.getInstance();
			library.buildLibrary();
			library.refreshJTrees();

			// System.out.println(actor.getName() + " removed");

		} catch (Exception ee) {
			System.out
					.println("Error in getting actor in RemoveComponentAction");
		}
	}

	/**
	 * allows you to set the ptolemyFrame that should be used as the parent of
	 * this action.
	 */
	public void setPtolemyFrame(PtolemyFrame pFrame) {
		this.pFrame = pFrame;
	}
}