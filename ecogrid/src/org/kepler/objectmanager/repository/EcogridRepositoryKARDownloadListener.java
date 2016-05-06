/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: barseghian $'
 * '$Date: 2011-01-25 15:30:42 -0800 (Tue, 25 Jan 2011) $' 
 * '$Revision: 26836 $'
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

package org.kepler.objectmanager.repository;

import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;

import org.kepler.gui.ComponentLibraryTab;
import org.kepler.kar.KARCacheManager;
import org.kepler.kar.KARFile;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.cache.CacheUtil;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.vergil.toolbox.PtolemyTransferable;

/**
 * This class represents a single result from a search of the ecogrid
 * repository. If there are many results (as in a resultset), then multiple
 * EcogridRepositoryResults can be put into an iterator.
 * 
 * @author Chad Berkley
 */
public class EcogridRepositoryKARDownloadListener implements DropTargetListener {
	/**
	 * called when a drag enters the canvas
	 */
	public void dragEnter(DropTargetDragEvent dtde) {

	}

	/**
	 * called when a drag exits the canvas
	 */
	public void dragExit(DropTargetEvent dtde) {

	}

	/**
	 * called when the drag moves over the canvas
	 */
	public void dragOver(DropTargetDragEvent dtde) {

	}

	/**
	 * called when a drag is dropped over the canvas
	 */
	public void drop(DropTargetDropEvent dtde) { 
		if (!org.kepler.gui.SearchUIJPanel.SEARCHREPOS) {
			// if we're not searching the repository, don't do this
			return;
		}
		try {
			// get the iterator of objects being dragged
			Iterator iterator = null;
			try {
				iterator = (Iterator) dtde.getTransferable().getTransferData(
						PtolemyTransferable.namedObjFlavor);
			} catch (Exception e) {
				System.out.println("e: " + e.getMessage());
			}

			NamedObj dropObj = null;
			while (iterator.hasNext()) { // get the namedobj from the dragged
											// obj
				dropObj = (NamedObj) iterator.next();
				// System.out.println("namedObj: " + dropObj.getName());
			}

			// check to see if the obj has a karid attribute
			StringAttribute karIdAtt = (StringAttribute) dropObj
					.getAttribute("karId");
			if (karIdAtt == null) { 
				// if there is no karId then ignore this actor. 
				// all remote actors should have the karId attribute
				return;
			}

			KeplerLSID karLSID = null;
			karLSID = new KeplerLSID(karIdAtt.getExpression());

			System.out.println("karId: " + karLSID);

			// if it does, check to see if that kar is already cached
			KARCacheManager kcm = KARCacheManager.getInstance();
			if (kcm.isCached(karLSID)) {
				// we've already got it
			} else {
				// if it isn't, download it and cache it.
				Repository repository = getRepository();				
				InputStream karIs = repository.get(karLSID, ComponentLibraryTab.AUTHENTICATE);
				File tmpFile = CacheManager.getInstance().getTempFile();
				FileOutputStream fos = new FileOutputStream(tmpFile);
				// write the kar file to a temp file
				CacheUtil.writeInputStreamToOutputStream(karIs, fos);
				// put the kar file into the cache.
				KARFile karf = new KARFile(tmpFile);
				LocalRepositoryManager.getInstance().getSaveRepository();
				
				// TODO redo this whole method!  it's crap
				
				// done
			}
		} catch (Exception e) {
			System.out.println("Error getting kar file on actor drop: "
					+ e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	/**
	 * called when a drag action changes
	 */
	public void dropActionChanged(DropTargetDragEvent dtde) {

	}

	/**
	 * get the ecogrid repository
	 */
	private static Repository getRepository() throws Exception {
		RepositoryManager rmanager = RepositoryManager.getInstance();
		Iterator repoList = rmanager.repositoryList();
		// HACKALERT: get the first repository
		Repository repository = (Repository) repoList.next();
		if (!(repository instanceof EcogridRepository)) {
			throw new Exception("Right now, we can only sync ecogrid "
					+ "repositories.  Sorry.");
		}
		return repository;
	}
}