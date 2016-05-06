/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 13:33:59 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31478 $'
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.queryservice.resultset.ResultsetTypeRecord;
import org.kepler.gui.GraphicalActorMetadata;
import org.kepler.kar.karxml.KarXml;
import org.kepler.objectmanager.cache.ActorCacheObject;
import org.kepler.objectmanager.cache.CacheException;
import org.kepler.objectmanager.cache.CacheObject;
import org.kepler.objectmanager.lsid.KeplerLSID;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;
/**
 * This class represents a single result from a search of the ecogrid
 * repository. If there are many results (as in a resultset), then multiple
 * EcogridRepositoryResults can be put into an iterator.
 * 
 * @author Chad Berkley
 */
public class EcogridRepositoryResults {
	private static final Log log = LogFactory.getLog(EcogridRepositoryResults.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();
	
	private String docid;
	private String name;
	private KarXml.KarEntry karEntry;
	private String lsid;
	private String karLSID;
	private Vector<String> semTypes;
	private ComponentEntity component;
	private CacheObject cachedComponent;
	private AtomicBoolean isReady = new AtomicBoolean(false);
	private AtomicBoolean hasException = new AtomicBoolean(false);
	private String repositoryName;
	private File karFile;
	private Integer indexValue = null;
	private org.kepler.objectmanager.repository.KARDownloader downloader;

	public static List<EcogridRepositoryResults> parseKarXml(String docid,
			String repositoryName, int indexValue, boolean authenticate) throws RepositoryException {
		//System.out
		//		.println("EcogridRepositoryResults parseKarXml Parsing KAR XML: "
		//				+ docid);
		List<EcogridRepositoryResults> results = new ArrayList<EcogridRepositoryResults>();
		try {
			RepositoryManager rm = RepositoryManager.getInstance();
			Repository rep = rm.getRepository(repositoryName);
			if (rep instanceof EcogridRepository) {
			} else {
				log.error("EcogridRepositoryResults is trying to search "
						+ "a non-Ecogrid Repository");
				return null;
			}
			EcogridRepository erep = (EcogridRepository) rep;
			InputStream is = erep.get(docid, authenticate);

			KarXml karXml = KarXml.of(is);
			String karLSID = karXml.getLsid();
			if (karXml != null) {
				karXml.setRepositoryName(repositoryName);
				for (KarXml.KarEntry entry : karXml.getKarEntries()) {
					EcogridRepositoryResults ecogridResult = new EcogridRepositoryResults(
							entry, repositoryName, indexValue, karLSID);
					ecogridResult.setDocId(docid);
					results.add(ecogridResult);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return results;
	}
	
	public EcogridRepositoryResults(String karLSID, String repositoryName, boolean authenticate) {
		// Initialize just the karLsid and repositoryName variables. This is
		// intended to be used just to cache the KAR to local storage.
		this.karLSID = karLSID;
		this.repositoryName = repositoryName;
	}
	
	public EcogridRepositoryResults(KarXml.KarEntry karEntry, String repositoryName, 
			int indexValue, String karLSID) throws RepositoryException {
		this.indexValue = indexValue;
		this.karEntry = karEntry;
		this.repositoryName = repositoryName;
		this.karLSID = karLSID;
		try {
			if (karEntry.getType().endsWith(".TypedCompositeActor")) {
				cachedComponent = new ActorCacheObject(karEntry.asInputStream());
			}
		}
		catch(CacheException ex) {
			log.error("Cache exception", ex);
		}
		name = karEntry.getName();
		lsid = karEntry.getLsid();		
		semTypes = new Vector<String>(karEntry.getSemanticTypes());
	}

	/**
	 * All EcogridRepositoryResults produced by a single call to parseKarXml()
	 * will be assigned the same index value so they can later be put under a
	 * common KAR file. The index value is guaranteed to be unique to the
	 * contents of one KAR file only within the results produced by a single
	 * call to EcogridRepository.search(). Subsequent calls *will* produce
	 * colliding values.
	 * @return The index value shared by all EcogridRepositoryResults instances
	 * generated from the same KAR file. Calls to this method by
	 * EcogridRepositoryResults instances that were not generated through
	 * parsing a KAR XML file will produce null. 
	 */
	public Integer getIndexValue() {
		return indexValue;
	}
	
	public KarXml.KarEntry getKarEntry() {
		return karEntry;
	}
	/**
	 * default constructor
	 */
	public EcogridRepositoryResults(ResultsetTypeRecord record,
			String repositoryName, boolean authenticate) throws RepositoryException {
		this.repositoryName = repositoryName;
		try {
			semTypes = new Vector<String>();
			docid = record.getIdentifier();
			// get the document from the repository and parse it into an
			// actormetadata
			// object
			
			RepositoryManager rm = RepositoryManager.getInstance();
			Repository rep = rm.getRepository(repositoryName);
			if (rep instanceof EcogridRepository) {
			} else {
				log.error("EcogridRepositoryResults is trying to search "
						+ "a non-Ecogrid Repository");
				return;
			}
			
			EcogridRepository erep = (EcogridRepository) rep;
			
			InputStream is = erep.get(docid, authenticate);
			
			GraphicalActorMetadata gam = new GraphicalActorMetadata(is);
			NamedObj obj = gam.getActorAsNamedObj(null);
			
			component = (ComponentEntity) obj;
			name = gam.getName();
			lsid = gam.getId();
			
			StringAttribute sa = (StringAttribute) obj.getAttribute("karId");
			if (sa != null) {
				karLSID = sa.getExpression();

				if (isDebugging) {
					log.debug("karLSID: " + karLSID);
				}
				// possibly here, kick off a thread to start downloading the kar
				// files into
				// the classpath
			}

			semTypes = gam.getSemanticTypes();
		} catch (Exception e) {
			// e.printStackTrace();
			throw new RepositoryException("Error getting actor metadata for "
					+ "resultset document: " + e.getMessage());
		}
	}

	/**
	 * download the kar file and get it ready to be imported into the local
	 * system.
	 */
	public File cacheKAR(boolean authenticate) throws RepositoryException {
		// break off a new thread
		// run the download in that thread
		// set isReady to true;
		(new KARDownloader(downloader, authenticate)).start();
		while (!isReady()) {
		} // wait for the download. this can be done away with when the
			// interface
		// is fixed to download upon drag n drop

		if (hasException()) {
			throw new RepositoryException("Error caching the kar file for the "
					+ "component " + lsid);
		}
		return karFile;
	}

	/**
	 * returns true of the kar file from this result is ready for use
	 */
	public boolean isReady() {
		return isReady.get();
	}

	/**
	 * returns true if the kar caching thread has thrown an exception
	 */
	public boolean hasException() {
		return hasException.get();
	}

	/**
	 * return a string rep of this object
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{name: " + name);
		sb.append(", docid: " + docid);
		sb.append(", lsid: " + lsid);
		sb.append(", karId: " + karLSID);
		sb.append(", semanticTypes: " + semTypes.toString());
		sb.append("}");
		return sb.toString();
	}

	/**
	 * return the docid of the result
	 */
	public String getDocid() {
		return docid;
	}

	/**
	 * return the name of the component
	 */
	public String getName() {
		return name;
	}

	/**
	 * return the lsid (entityId) of the component
	 */
	public String getLSID() {
		return lsid;
	}

	/**
	 * return the lsid of the kar file associated with this component
	 */
	public String getKarLSID() {
		return karLSID;
	}

	/**
	 * return any semantic types associated with this component
	 */
	public Vector<String> getSemanticTypes() {
		return semTypes;
	}
	
	public CacheObject getCacheObject() {
		return cachedComponent;
	}

	/**
	 * return the instantiated
	 */
	public ComponentEntity getComponent() {
		return component;
	}
	
	/**
	 * Set the docid (kar xml id)
	 * @param docid
	 */
	public void setDocId(String docid)
	{
	  this.docid =docid;
	}

	public void setDownloader(org.kepler.objectmanager.repository.KARDownloader downloader) {
		this.downloader = downloader;
	}

	/**
	 * a class to download the kar file in a new thread.
	 */
	private class KARDownloader extends Thread {
		// location to download the kar file to
		String tempPath = org.kepler.objectmanager.cache.CacheManager.tmpPath;
		private org.kepler.objectmanager.repository.KARDownloader downloader;

		private boolean authenticate = false;
		
		public void setAuthenticate(boolean authenticate){
			this.authenticate = authenticate;
		}
		public boolean getAuthenticate(){
			return authenticate;
		}
		
		public KARDownloader() {
			super();
		}
		
		public KARDownloader(org.kepler.objectmanager.repository.KARDownloader downloader, boolean authenticate) {
			this();
			this.downloader = downloader;
			this.authenticate = authenticate;
		}

		/**
		 * get the kar and download it to the cache
		 */
		public void run() {
			try {
				if (karLSID == null){
					System.out.println("EcogridRepositoryResults KARDownloader.run() ERROR karLSID is null");
				}
				KeplerLSID karlsid = new KeplerLSID(karLSID);
				String karSavePath = this.downloader.getKarPath();
				if (karSavePath == null) {
					karSavePath = tempPath;
				}
				String karFilename = this.downloader.getKarName();
				if (karFilename == null) {
					karFilename = karlsid.createFilename() + ".kar";
					System.out.println("No KAR name found in XML, using LSID for filename instead:"+karFilename);
				}
				File karPath = new File(karSavePath);
				karFile = new File(karPath, karFilename);
				karPath.mkdirs();
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(karFile);
					EcogridRepository repository = (EcogridRepository) RepositoryManager
							.getInstance().getRepository(repositoryName);
					InputStream is = repository.get(karlsid, authenticate);
					byte[] b = new byte[1024];
					int numread = is.read(b, 0, 1024);
					downloader.updateDownloadProgress(numread);
					while (numread != -1) {
						fos.write(b, 0, numread);
						numread = is.read(b, 0, 1024);
						downloader.updateDownloadProgress(numread);
					}
					fos.flush();
					isReady.set(true);
				} finally {
					if(fos != null) {
						fos.close();
					}
				}
			} catch (Exception e) {
				System.out.println("Error downloading kar file: "
						+ e.getMessage());
				// e.printStackTrace();
				hasException.set(true);
				isReady.set(true);
			}
		}
	}
}