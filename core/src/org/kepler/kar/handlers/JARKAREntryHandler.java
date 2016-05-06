/*
 *  The node controller for actor instances.
 *  Copyright (c) 2010 The Regents of the University of California.
 *  All rights reserved.
 *  Permission is hereby granted, without written agreement and without
 *  license or royalty fees, to use, copy, modify, and distribute this
 *  software and its documentation for any purpose, provided that the above
 *  copyright notice and the following two paragraphs appear in all copies
 *  of this software.
 *  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 *  FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 *  ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 *  THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 *  PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 *  CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 *  ENHANCEMENTS, OR MODIFICATIONS.
 *  PT_COPYRIGHT_VERSION_2
 *  COPYRIGHTENDKEY
 */
package org.kepler.kar.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.kar.KAREntry;
import org.kepler.kar.KAREntryHandler;
import org.kepler.kar.KAREntryHandlerFactory;
import org.kepler.kar.KARFile;
import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.DotKeplerManager;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import util.ClassPathMunger;

/**
 * A KAREntryHandler extension that saves JAR files that are in KAR files to
 * somewhere on disk.  
 * 
 * @author Aaron Schultz
 * 
 */
public class JARKAREntryHandler implements KAREntryHandler {

	private static final Log log = LogFactory.getLog(JARKAREntryHandler.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private File _jarDirectory;

	public JARKAREntryHandler() {
		if (isDebugging) {
			log.debug("new JARKAREntryHandler()");
		}
	}

	/**
	 * KAR version 1.0 type name.
	 * 
	 * @see org.kepler.kar.KAREntryHandler#getTypeName()
	 */
	public String getTypeName() {
		return "jar";
	}

	/**
	 * JARs are not supported in KAR version 2.0 and 2.1
	 * 
	 * @see org.kepler.kar.KAREntryHandler#handlesType(java.lang.String)
	 */
	public boolean handlesType(String typeName) {
		return false;
	}

	/*
	 * Initialize the directory that jars are copied to.
	 * 
	 * @see org.kepler.kar.KAREntryHandler#initialize()
	 */
	public void initialize() {
		if (isDebugging) {
			log.debug("initialize()");
		}

		String jardir = DotKeplerManager.getDotKeplerPath();
		jardir += "kar" + File.separator;
		jardir += "jar" + File.separator;
		if (isDebugging)
			log.debug(jardir);

		_jarDirectory = new File(jardir);
		if (!_jarDirectory.exists()) {
			if (isDebugging)
				log.debug("jarDirectory does not exist, try creating it");
			try {
				_jarDirectory.mkdirs();
			} catch (Exception e) {
				log.error("Unable to create jar directory");
			}
		}
	}
	
	public boolean open(KARFile karFile, KAREntry entry, TableauFrame tableauFrame) throws Exception {
		return false;
	}

	/*
	 * JAR files that are put in KAR files do not get cached in the CacheManager
	 * they are just copied to disk and added to the classpath.
	 * 
	 * @see org.kepler.kar.KAREntryHandler#open(org.kepler.kar.KARFile,
	 * java.util.jar.JarEntry)
	 */
	public CacheObject cache(KARFile karFile, KAREntry entry) throws Exception {
		if (isDebugging) {
			log
					.debug("open(" + karFile.getName() + "," + entry.getName()
							+ ")");
		}

		String outputFileName = _jarDirectory + File.separator
				+ entry.getName();
		if (isDebugging) {
			log.debug(outputFileName);
		}
		File outputFile = new File(outputFileName);
		if (isDebugging) {
			log.debug(outputFile.toString());
			log.debug(outputFile.exists());
		}
		if (!outputFile.exists()) {
			FileOutputStream fos = new FileOutputStream(outputFile);
			InputStream in = karFile.getInputStream(entry);
			for (int c = in.read(); c != -1; c = in.read()) {
				fos.write(c);
			}
			in.close();
			fos.close();

			// add the jar to the classpath
			ClassPathMunger.addFile(outputFile);
			if (isDebugging) {
				log.debug(outputFile.toString());
			}
		}
		return null; // No CacheObject created for this KAREntry

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.kepler.kar.KAREntryHandler#save(org.kepler.objectmanager.lsid.KeplerLSID
	 * )
	 */
	public Hashtable<KAREntry, InputStream> save(Vector<KeplerLSID> lsids, KeplerLSID karLsid,
			TableauFrame tableauFrame)
			throws Exception {
		return null;
	}

	/**
	 * A factory that creates a KAREntryHandler object.
	 * 
	 *@author Aaron Schultz
	 */
	public static class Factory extends KAREntryHandlerFactory {
		/**
		 * Create a factory with the given name and container.
		 * 
		 *@param container
		 *            The container.
		 *@param name
		 *            The name of the entity.
		 *@exception IllegalActionException
		 *                If the container is incompatible with this attribute.
		 *@exception NameDuplicationException
		 *                If the name coincides with an attribute already in the
		 *                container.
		 */
		public Factory(NamedObj container, String name)
				throws IllegalActionException, NameDuplicationException {
			super(container, name);
		}

		/**
		 * Create a library pane that displays the given library of actors.
		 * 
		 * @return A new LibraryPaneTab that displays the library
		 */
		public KAREntryHandler createKAREntryHandler() {
			if (isDebugging)
				log.debug("createKAREntryHandler()");
			JARKAREntryHandler jkeh = new JARKAREntryHandler();
			return jkeh;
		}
	}

}
