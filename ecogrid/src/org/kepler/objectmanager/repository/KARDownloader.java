/*
 * Copyright (c) 2010-2011 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:22:04 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31121 $'
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.kepler.kar.karxml.KarXml;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Mar 24, 2010
 * Time: 11:48:05 AM
 */

public class KARDownloader {
	
	private String karPath = null;
	private String karName = null;
	private static KARDownloader instance = null;
	private JDialog dialog;
	private JProgressBar progressBar;
	private long downloadedByteTotal = 0;
	private long karFileSize;

	public static KARDownloader getInstance() {
		if (instance == null) {
			instance = new KARDownloader();
		}
		return instance;
	}
	
	private KARDownloader() {
		setupProgressBar();
	}
	
	private void setupProgressBar() {
		progressBar = new JProgressBar(0, 100);
		progressBar.setPreferredSize(new Dimension(175, 20));
		progressBar.setString("Downloading");
		progressBar.setStringPainted(false);
		progressBar.setValue(0);
		
		JLabel label = new JLabel("Progress: ");
		JPanel panel = new JPanel();
		
		panel.add(label);
		panel.add(progressBar);
		
		dialog = new JDialog((Frame) null, "Downloading...", false);
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		dialog.pack();
	}

	public void updateDownloadProgress(int bytes) {
		if (bytes == -1) {
			// The stream is over
			progressBar.setValue(100);
			dialog.setVisible(false);
//			dialog.dispose();
			return;
		}
		
		// Update downloaded total
		downloadedByteTotal += bytes;
		progressBar.setValue((int) ((downloadedByteTotal * 100) / karFileSize));		
	}
	
	/**
	 * Download a KAR. 
	 * @param karLSID
	 * @param karSize
	 * @param repositoryName
	 * @return downloaded File, or null.
	 */
	public File download(String karLSID, Long karSize, String repositoryName, boolean authenticate){

		karFileSize = karSize;
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		
		if (karLSID == null || karLSID.length()==0 || 
				repositoryName == null || repositoryName.length()==0){
			System.out.println("KARDownloader download(karLSID, karSize, repositoryName) got null or empty lsid " +
					"or repositoryName!");
			return null;
		}
		
		try {
			progressBar.setValue(0);
			EcogridRepositoryResults result = new EcogridRepositoryResults(karLSID, repositoryName, authenticate);
			result.setDownloader(this);
			File local = result.cacheKAR(authenticate);
			return local;
		}
		catch(RepositoryException ex) {
			System.out.println("Caught repository exception!");
			dialog.setVisible(false);
			ex.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Given karXML for a KAR, download KAR.
	 * @see download(String karLSID, Long karSize, String repositoryName) for alternative 
	 * you may want to use to avoid having to download karXml.
	 * @param karXml
	 * @return kar File
	 */
	public File download(KarXml karXml, boolean authenticate) {
		
		karFileSize = karXml.getSize();
		
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);

		String lsid = karXml.getLsid();
		String repositoryName = karXml.getRepositoryName();
		
		if (lsid == null || lsid.length()==0 || 
				repositoryName == null || repositoryName.length()==0){
			System.out.println("KARDownloader download(karxml) got null or empty lsid " +
					"or repositoryName from karXml!");
			return null;
		}
		
		try {
			progressBar.setValue(0);
			EcogridRepositoryResults result = 
				new EcogridRepositoryResults(lsid, repositoryName, authenticate);
			result.setDownloader(this);
			File local = result.cacheKAR(authenticate);
			return local;
		}
		catch(RepositoryException ex) {
			System.out.println("Caught repository exception!");
			ex.printStackTrace();
		}
		
		return null;
	}

	public void setKarPath(String karPath) {
		System.out.println("KARDownloader setKarPath() - " + karPath);
		this.karPath = karPath;
	}

	public String getKarPath() {
		return karPath;
	}
	
	public void setKarName(String karName) {
		System.out.println("KARDownloader setKarName() - " + karName);
		this.karName = karName;
	}
	
	public String getKarName() {
		return karName;
	}
}
