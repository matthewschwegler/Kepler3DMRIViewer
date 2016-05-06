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

package org.kepler.objectmanager.repository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.ecoinformatics.ecogrid.client.IdentifierServiceClient;

/**
 * This class bootstraps an ecogrid repository. It uploads all kar files in the
 * cache to the repository and includes methods to keep the kepler cache and the
 * repository in sync.
 * 
 * @author Chad Berkley
 */
public class EcogridRepositoryBootstrap {
	/**
	 * Constructor
	 */
	public EcogridRepositoryBootstrap() {
	}

	/**
	 * upload all kar files in the cache to the repository. this can be called
	 * from main()
	 */
	public void bootstrap(String sessionid) throws Exception {

		/* whoa
		File placeHolderFile = new File("placeholder.txt");
		if (!placeHolderFile.exists()) {
			placeHolderFile.createNewFile();
		}

		// keep track of where we are so if something dies, we can startup where
		// we left off.
		Vector placeVector = readPlaceHolderFile(placeHolderFile);
		Repository repository = getRepository();
		IdentifierServiceClient lsidClient = getLSIDClient();

		CacheManager manager = CacheManager.getInstance();
		// get an iterator over all of the cache objects
		Iterator cacheObjects = manager.getCacheObjectIterator();
		// find all of the kar objects, create a metadata file and upload
		// both to the repository
		while (cacheObjects.hasNext()) {
			CacheObject co = (CacheObject) cacheObjects.next();
			if (co instanceof KARCacheObject) {
				// create the metadata file, then upload
				KARCacheObject kco = (KARCacheObject) co;
				KeplerLSID KARLsid = kco.getLSID();
				if (placeVectorContains(KARLsid.toString(), placeVector)) {
					continue; // skip this kar file
				}
				KeplerLSID[] actorLSIDs = kco.getActors();
				for (int i = 0; i < actorLSIDs.length; i++) { // upload each
																// actor for
																// this kar file
					ActorCacheObject aco = (ActorCacheObject) manager
							.getObject(actorLSIDs[i]);
					if (aco == null)
						continue;
					ActorMetadata am = aco.getMetadata();
					// add the karId attributes to the metadata before uploading
					StringAttribute karId = new StringAttribute();
					karId.setName("karId"); // put the id of the kar file into
											// the AM
					karId.setExpression(KARLsid.toString());
					am.addAttribute(karId);
					UploadToRepository.addDocumentation(am);
					// put the actor metadata

					try {
						System.out.println("uploading actor " + am.getName());
						System.out.println("lsid: " + actorLSIDs[i]);
            
						repository.put(am.toString(), actorLSIDs[i], sessionid);
						// create and upload an access file to make the actor
						// public
						KeplerLSID accessLSID = LSIDGenerator.getInstance().getNewLSID();
						System.out
								.println("uploading access doc " + accessLSID);
						repository
								.put(UploadToRepository
										.buildAccessDocument(actorLSIDs[i]),
										accessLSID, sessionid);
					} catch (Exception e) {
						try {
							File errorLog = new File("ERROR.LOG");
							FileOutputStream fos = new FileOutputStream(
									errorLog, true);
							String errormsg = "";
							errormsg += "Classname: " + am.getClassName()
									+ "\n";
							errormsg += e.getMessage();
							errormsg += "=================================\n=================================\n";
							byte[] b = errormsg.getBytes();
							fos.write(b, 0, b.length);
							fos.flush();
							fos.close();
						} catch (Exception ee) {
							System.out
									.println("could not write to the error log: "
											+ ee.getMessage());
						}

					}
				}

				// upload the kar file and access document
				try {
					File karFile = (File) kco.getObject();
					System.out.println("uploading kar file " + kco.getLSID());
					repository.put(karFile, kco.getLSID(), sessionid);
					KeplerLSID accessLSID = LSIDGenerator.getInstance().getNewLSID();
					System.out.println("upload access doc " + accessLSID);
					repository.put(UploadToRepository.buildAccessDocument(kco
							.getLSID()), accessLSID, sessionid);
				} catch (Exception e) {
					// accession number in use, ignore.
				}

				// if something dies, this lets us start up where we left off
				// instead
				// of redoing the whole process
				writePlaceHolderFile(KARLsid.toString(), placeHolderFile);
			}
			System.out.println();
		}*/

	}

	/**
	 * main method to be called from ant.
	 */
	public static void main(String[] args) {
		try {
			EcogridRepositoryBootstrap erb = new EcogridRepositoryBootstrap();
      if(args.length != 1)
      {
        System.out.println("Error, must pass an authenticated sessionid.");
      }
      String sessionid = args[0];
			erb.bootstrap(sessionid);
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * get the lsid client library and return it
	 */
	private static IdentifierServiceClient getLSIDClient() throws Exception {
		// search the repository for everything
		Repository repository = getRepository();
		String LSIDServer = repository.getLSIDServerURL();
		IdentifierServiceClient lsidClient = new IdentifierServiceClient(
				LSIDServer);
		return lsidClient;
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

	/**
	 * reads the place holder file into a vector
	 */
	private Vector readPlaceHolderFile(File f) throws IOException {
		FileReader fr = new FileReader(f);
		StringBuffer sb = new StringBuffer();
		char[] c = new char[1024];
		int numread = fr.read(c, 0, 1024);
		while (numread != -1) {
			String s = new String(c, 0, numread);
			sb.append(s);
			numread = fr.read(c, 0, 1024);
		}

		StringTokenizer st = new StringTokenizer(sb.toString(), "\n");
		Vector v = new Vector();
		while (st.hasMoreElements()) {
			v.addElement(st.nextElement());
		}
		return v;
	}

	/**
	 * writes an lsid to the place holder file
	 */
	private void writePlaceHolderFile(String lsid, File f) throws IOException {
		FileWriter fw = new FileWriter(f, true);
		fw.write(lsid + "\n", 0, lsid.length() + 1);
		fw.flush();
		fw.close();
	}

	/**
	 * returns true if the vector v contains s
	 */
	private boolean placeVectorContains(String s, Vector v) {
		for (int i = 0; i < v.size(); i++) {
			String vs = (String) v.elementAt(i);
			if (vs.trim().equals(s.trim())) {
				return true;
			}
		}
		return false;
	}
}