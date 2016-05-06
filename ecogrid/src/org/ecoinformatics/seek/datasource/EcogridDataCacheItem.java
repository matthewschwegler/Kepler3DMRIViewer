/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
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

package org.ecoinformatics.seek.datasource;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ecoinformatics.ecogrid.authenticatedqueryservice.AuthenticatedQueryServiceGetToStreamClient;
import org.ecoinformatics.ecogrid.queryservice.QueryServiceGetToStreamClient;
import org.ecoinformatics.seek.ecogrid.EcoGridService;
import org.ecoinformatics.seek.ecogrid.EcoGridServicesController;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.cache.BufferedDataCacheObject;
import org.kepler.objectmanager.cache.CacheManager;

import ptolemy.util.CancelException;
import ptolemy.util.MessageHandler;

/**
 * This class is not as generic as the name may indicate. It is designed to get
 * the metadata to determine where the data is stored and then does a Ecogrid
 * "get" to the data.
 */
public class EcogridDataCacheItem extends BufferedDataCacheObject {
	protected String mEndPoint = null;
	private boolean mIsTarFile = false;
	private String mEntityIdentifier = null;

	private static Log log;
	static {
		log = LogFactory
				.getLog("org.ecoinformatics.seek.datasource.EcogridDataCacheItem");
	}

	/**
	 * Default constructor
	 * 
	 */
	public EcogridDataCacheItem() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ecoinformatics.seek.datasource.DataCacheItem#doWork()
	 */
	public int doWork() {
		log.debug("EcogridDataCacheItem - doing Work mStatus " + getStatus());
		return downloadDataFromSource();
	}

	/**
	 * This method will download data base on distribution url in entity object
	 */
	protected int downloadDataFromSource() {

		String resourceName = getResourceName();
		return getContentFromSource(resourceName);

	}

	/*
	 * Method to get content from given source.
	 */
	private int getContentFromSource(String resourceName) {
		log.debug("download data from EcogridDataCacheItem URL : "
				+ resourceName);
		if (resourceName.startsWith("http://")
				|| resourceName.startsWith("file://")
				|| resourceName.startsWith("ftp://")) {
			// get the data from a URL
			FileOutputStream osw = null;
			try {
				URL url = new URL(resourceName);
				if (url != null) {
					URLConnection conn = url.openConnection();
					if (conn != null) {
						InputStream filestream = url.openStream();
						if (filestream != null) {

							// String type = conn.getContentType();

							// Crate a new Cache Filename and write the
							// resultsets directly to the cached file
							File localFile = getFile();
							osw = new FileOutputStream(localFile);
							if (osw != null) {
								byte[] c = new byte[1024];
								int bread = filestream.read(c, 0, 1024);
								while (bread != -1) {
									osw.write(c, 0, bread);
									bread = filestream.read(c, 0, 1024);
								}
								osw.close();
								return CACHE_COMPLETE;
							}

						}
					}
				}
				log
						.debug("EcogridDataCacheItem - error connecting to http/file ");
				cleanUpCache(osw);
				return CACHE_ERROR;
			} catch (IOException ioe) {
				cleanUpCache(osw);
				return CACHE_ERROR;
			}

			// We will use ecogrid client to handle both ecogrid and srb
			// protocol
		} else if (resourceName.startsWith("ecogrid://")) {
			// get the docid from url
			int start = resourceName.indexOf("/", 11) + 1;
			log.debug("start: " + start);
			int end = resourceName.indexOf("/", start);
			if (end == -1) {
				end = resourceName.length();
			}
			log.debug("end: " + end);
			String identifier = resourceName.substring(start, end);
			// pass this docid and get data item
			return getDataItemFromEcoGrid(mEndPoint, identifier);
		} else if (resourceName.startsWith("srb://")) {
			// get srb docid from the url
			String identifier = transformSRBurlToDocid(resourceName);
			// reset endpoint for srb (This is hack we need to figure ou
			// elegent way to do this
			
      ConfigurationProperty commonProperty = ConfigurationManager
        .getInstance().getProperty(ConfigurationManager.getModule("ecogrid"));
      mEndPoint = commonProperty.getProperty("srb.endPoint").getValue();
			// pass this docid and get data item
			log.debug("before get srb data@@@@@@@@@@@@@@@@@@@@@@@@@@");
			return getDataItemFromEcoGrid(mEndPoint, identifier);
		} else {
			return CACHE_ERROR;
		}
	}

	/**
	 * 
	 * 	 */
	protected String getAttrsForXML() {
		return "   endpoint=\"" + mEndPoint + "\"\n";

	}

	/**
	 * @return Returns the mEndPoint.
	 */
	public String getEndPoint() {
		return mEndPoint;
	}

	/**
	 * @param endPoint
	 *            The mEndPoint to set.
	 */
	public void setEndPoint(String endPoint) {
		mEndPoint = endPoint;
	}

	/**
	 * If this item is a tar file
	 * 
	 * @return boolean
	 */
	public boolean getIsTarFile() {
		return mIsTarFile;
	}

	/**
	 * Set this item is tar file or not
	 * 
	 * @param isTarFile
	 *            boolean
	 */
	public void setIsTarFile(boolean isTarFile) {
		mIsTarFile = isTarFile;
	}

	/*
	 * This method will transfer a srb url to srb docid in ecogrid ecogrid srb
	 * id should look like:
	 * srb://seek:/home/beam.seek/IPCC_climate/Present/ccld6190.dat and
	 * correspond docid looks like:
	 * srb://testuser:TESTUSER@orion.sdsc.edu/home/beam
	 * .seek/IPCC_climate/Present/ccld6190.dat
	 */
	private String transformSRBurlToDocid(String srbURL) {
		String docid = null;
		if (srbURL == null) {
			return docid;
		}
		String regex = "seek:";
		srbURL = srbURL.trim();
		log.debug("The srb url is " + srbURL);
		// get user name , passwd and machine namefrom configure file
    ConfigurationProperty commonProperty = ConfigurationManager
        .getInstance().getProperty(ConfigurationManager.getModule("ecogrid"));
    String user = commonProperty.getProperty("srb.user").getValue();
    String passwd = commonProperty.getProperty("srb.passwd").getValue();
    String machineName = commonProperty.getProperty("srb.machineName").getValue();
    
		String replacement = user + ":" + passwd + "@" + machineName;
		docid = srbURL.replaceFirst(regex, replacement);
		log.debug("The srb id is " + docid);
		return docid;
	}

	/**
	 * Set an entity to describe the data cache
	 * 
	 * @param mEntity
	 *            Entity
	 */
	public void setEntityIdentifier(String identifier) {
		this.mEntityIdentifier = identifier;
	}

	/**
	 * Get an entity to describe the data cache
	 * 
	 * @return Entity
	 */
	public String getEntityIdentifier() {
		return mEntityIdentifier;
	}

	/*
	 * This method will get data from ecogrid server base on given docid. This
	 * method will handle the distribution url is ecogrid or srb protocol
	 */
	/**
	 * Gets the dataItemFromEcoGrid attribute of the DataCacheObject object
	 * 
	 *@param endPoint
	 *@param identifier
	 */
	protected int getDataItemFromEcoGrid(String endPoint, String identifier) {

		// create a ecogrid client object and get the full record from the
		// client
		if (endPoint != null && identifier != null) {
			log.debug("Get " + identifier + " from " + endPoint);
			BufferedOutputStream bos = null;
			try {
				// factory
				log.debug("This is instance pattern");

				URL endPointURL = new URL(endPoint);

				log.debug("Get from EcoGrid: " + identifier);
				// log.warn("the local file name is "+getFile());
				bos = new BufferedOutputStream(new FileOutputStream(getFile()));
				// log.warn("after create output stream ");
				// get the service from the controller
				EcoGridService service = EcoGridServicesController
						.getInstance().getService(endPoint);
				// check if we need to authenticate for it
				if (service != null
						&& service
								.getServiceType()
								.equals(
										EcoGridServicesController.AUTHENTICATEDQUERYSERVICETYPE)) {
					// get a credential (hopefully)
					String sessionId = EcoGridServicesController.getInstance()
							.authenticateForService(service);
					log.info("using authenticated ecogrid get() method");
					AuthenticatedQueryServiceGetToStreamClient authGetClient = new AuthenticatedQueryServiceGetToStreamClient(
							endPointURL);
					authGetClient.get(identifier, sessionId, bos);
				} else {
					// just get using the public version
					// log.warn("in public version branch ");
					QueryServiceGetToStreamClient ecogridClient = new QueryServiceGetToStreamClient(
							endPointURL);
					// log.warn("after create service client ");
					ecogridClient.get(identifier, bos);
					// log.warn("after stream data  ");
				}
				// log.warn("before fulsh and close output stream ");
				bos.flush();
				bos.close();
				// log.warn("afer fulsh and close output stream ");
				return CACHE_COMPLETE;

			} catch (Exception ee) {
				log.error(
						"EcogridDataCacheItem - error connecting to Ecogrid ",
						ee);
				cleanUpCache(bos);
				// Alert user about possible permission issue (BRL:20070918)
				try {
					MessageHandler
							.warning("There has been a problem accessing the remote data:\n"
									+ ee.getMessage());
				} catch (CancelException e) {
					// do nothing
				}

				return CACHE_ERROR;
			}

		} else {
			// System.out.println("in else path of get data from other source");
			// this is not ecogrid source, we need download by other protocol
			return getContentFromSource(identifier);

		}
	}

	/*
	 * Cleanup the cache object if some error happens during the search.
	 */
	private void cleanUpCache(OutputStream bos) {

		// clean up the cache object
		try {
			// log.warn("close the input stream and delete existed file");
			bos.close();
			// getFile().delete();
			if (this.getLSID() != null) {
				CacheManager.getInstance().removeObject(this.getLSID());
			}

		} catch (Exception e) {
			log.warn("Couldn't close the output stream to cache file "
					+ e.getMessage());
		}

	}

}