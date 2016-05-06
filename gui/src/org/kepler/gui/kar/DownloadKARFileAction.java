/*
 * Copyright (c) 2010-2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:22:25 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31122 $'
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

package org.kepler.gui.kar;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.ComponentLibraryTab;
import org.kepler.kar.KARFile;
import org.kepler.kar.karxml.KarXml;
import org.kepler.moml.DownloadableKAREntityLibrary;
import org.kepler.objectmanager.cache.LocalRepositoryManager;
import org.kepler.objectmanager.library.LibraryManager;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.vergil.toolbox.FigureAction;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Mar 23, 2010
 * Time: 5:05:29 PM
 */

public class DownloadKARFileAction extends FigureAction {

	public DownloadKARFileAction(TableauFrame parent) {
		super("Download");
		this.parent = parent;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// Due to module issues, I can't directly invoke KARDownloader
		KarXml karXml = entityLibrary.getKarXml();
		try {
			Class karDownloaderClass = Class.forName("org.kepler.objectmanager.repository.KARDownloader");
			Method getInstanceMethod = karDownloaderClass.getMethod("getInstance");
			Object karDownloaderInstance = getInstanceMethod.invoke(null);
			
			// Tell the KAR file where to end up
			Method setKarPathMethod = karDownloaderClass.getMethod("setKarPath", String.class);
			File localSaveRepo = LocalRepositoryManager.getInstance().getSaveRepository();
			setKarPathMethod.invoke(karDownloaderInstance, localSaveRepo.getAbsolutePath());
			
			Method setKarNameMethod = karDownloaderClass.getMethod("setKarName", String.class);
			String filename = karXml.getName();
			if (!filename.endsWith(".kar")) {
				filename += ".kar";
			}
			setKarNameMethod.invoke(karDownloaderInstance, filename);
			
			Method downloadMethod = karDownloaderClass.getMethod("download", KarXml.class, boolean.class);
			File karFile = (File) downloadMethod.invoke(
					karDownloaderInstance, karXml, ComponentLibraryTab.AUTHENTICATE);
			try {
				KARFile kf = new KARFile(karFile);
				kf.cacheKARContents();
			}
			catch(IOException ex) {
				log.error("Error creating KARFile", ex);
			}
			catch(Exception ex) {
				log.error("Error caching KAR contents", ex);
			}
			
			
			try {
				LibraryManager.getInstance().addKAR(karFile);
				
				// Call buildLibrary() to fix not showing MyWorkflows dir
				// when this is first KAR placed in MyWorkflows. See bug#5581.
				// Also fixes at least one case of Remote Components entry
				// hanging around when it shouldn't. See bug#4953.
				// XXX This is expensive, about ~2s, and is used elsewhere
				// in the same fashion (before refreshJTrees()).
				LibraryManager.getInstance().buildLibrary();
				
				LibraryManager.getInstance().refreshJTrees();
			}
			catch(SQLException ex) {
				log.error("Failed to add to index", ex);
			}
			catch(IllegalActionException ex) {
				log.error("Failed to refresh trees", ex);
			}
		}
		catch(ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		catch(NoSuchMethodException ex) {
			ex.printStackTrace();
		}
		catch(InvocationTargetException ex) {
			ex.printStackTrace();
		}
		catch(IllegalAccessException ex) {
			ex.printStackTrace();
		}
	}
	
	
	public void setEntityLibrary(DownloadableKAREntityLibrary entityLibrary) {
		this.entityLibrary = entityLibrary;
	}

	private TableauFrame parent;
	private DownloadableKAREntityLibrary entityLibrary;
	private static final Log log = LogFactory.getLog(DownloadKARFileAction.class);

}
