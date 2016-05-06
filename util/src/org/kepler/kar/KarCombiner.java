/*
 * Copyright (c) 2008-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-11-26 14:24:50 -0800 (Mon, 26 Nov 2012) $' 
 * '$Revision: 31131 $'
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
package org.kepler.kar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.kepler.objectmanager.lsid.KeplerLSID;

/**
 * This class is a helper class for combining many kars together into one kar.
 * To use this class, hard coded strings in the constructor must be changed
 * before running.
 * 
 * @author Aaron Schultz
 */
public class KarCombiner {
	boolean deb = true;

	private File _karDir;

	private Vector<KARFile> _karsIn;
	private File _karOutFile;

	private LinkedHashMap<KAREntry, InputStream> _karItems;

	private KARManifest _manifest;

	private KeplerLSID _karLsid;

	/**
	 * 
	 */
	public KarCombiner() {
		
		// The directory that contains the kar files to be combined
		String karsInDirStr = "/Users/aaron/KeplerData/modules/actors/kar";
		
		// The file to write the new KAR file out to
		String karOutFileStr = "/Users/aaron/CoreActors.kar";
		
		// The LSID for the new KAR file
		String karLsid = "urn:lsid:kepler-project.org:corekar:1:1";

		try {
			_karLsid = new KeplerLSID(karLsid);
		} catch (Exception e) {
			e.printStackTrace();
		}

		_karDir = new File(karsInDirStr);

		_karsIn = new Vector<KARFile>();

		_karItems = new LinkedHashMap<KAREntry, InputStream>();
		_manifest = new KARManifest();

		_karOutFile = new File(karOutFileStr);
	}

	/**
	 *
	 */
	private void writeKar() throws IOException {

		JarOutputStream jos = new JarOutputStream(new FileOutputStream(
				_karOutFile), _manifest);
		Iterator<KAREntry> li = _karItems.keySet().iterator();
		while (li.hasNext()) {
			KAREntry entry = (KAREntry) li.next();
			if (deb)
				System.out.println("Writing " + entry.getName());
			try {
				jos.putNextEntry(entry);

				if (_karItems.get(entry) instanceof InputStream) {
					// inputstream from a bin file
					byte[] b = new byte[1024];
					InputStream is = (InputStream) _karItems.get(entry);
					int numread = is.read(b, 0, 1024);
					while (numread != -1) {
						jos.write(b, 0, numread);
						numread = is.read(b, 0, 1024);
					}
					is.close();
					// jos.flush();
					jos.closeEntry();
				}
			} catch (IOException ioe) {
				System.out.println(" Tried to write Duplicate Entry to kar "
						+ entry.getName() + " " + entry.getLSID());
				ioe.printStackTrace();
			}
		}
		jos.flush();
		jos.close();

		System.out.println("done writing KAR file to "
				+ _karOutFile.getAbsolutePath());
	}

	private void prepareManifest() {

		// main attributes
		_manifest
				.addMainAttribute(KARFile.LSID.toString(), _karLsid.toString());

		// entry attributes
		for (KAREntry ke : _karItems.keySet()) {
			if (deb)
				System.out.println(ke.getName());

			Attributes atts = ke.getAttributes();

			for (Object att : atts.keySet()) {
				if (deb)
					System.out.println(att.toString());
				if (att instanceof Name) {

					Name attrName = (Name) att;
					String value = atts.getValue(attrName);

					_manifest.addEntryAttribute(ke, attrName.toString(), value);

				}
			}

		}

	}

	private void getKarItems() {

		for (KARFile kf : _karsIn) {

			for (KAREntry ke : kf.karEntries()) {

				try {
					InputStream is = kf.getInputStream((ZipEntry) ke);
					_karItems.put(ke, is);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		}

	}

	/**
	 * 
	 */
	private void getKars() {
		FilenameFilter ff = new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.toLowerCase().endsWith(".kar");
			}
		};
		File[] karFiles = _karDir.listFiles(ff);
		for (int i = 0; i < karFiles.length; i++) {
			File f = karFiles[i];
			try {
				KARFile kf = new KARFile(f);
				if (deb)
					System.out.println(kf.getName());
				_karsIn.add(kf);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		KarCombiner kc = new KarCombiner();
		kc.getKars();
		kc.getKarItems();
		kc.prepareManifest();
		try {
			kc.writeKar();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}