/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 11:13:44 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31472 $'
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

/**
 * 
 */
package org.kepler.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.modules.Module;
import org.kepler.build.project.ProjectLocator;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.objectmanager.lsid.LSIDGenerator;

/**
 * This class contains an Authority and a Namespace to be used for uniquely
 * generating KeplerLSIDs.
 * 
 * @author Aaron Schultz
 */
public class AuthNamespace {
	private static final Log log = LogFactory.getLog(AuthNamespace.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * The authority that assigned the namespace to this Kepler instance.
	 */
	private String _authority;

	/**
	 * The namespace that uniquely identifies this Kepler instance along with
	 * the authority
	 */
	private String _namespace;

	/**
	 * The list of configured authorities to try to get a unique authNamespace
	 * from. This is populated from the config.xml file.
	 */
	private Vector<String> _configuredAuthorities;

	/**
	 * The filename where the authority and namesapce are stored. Store the
	 * AuthorizedNamespace file in the KEPLER directory, not the cache
	 * directory.
	 */
	private String _anFileName;

	private boolean _initialized = false;

	private final String _saveFileName = "AuthorizedNamespace";

	/** singleton instance **/
	private static AuthNamespace instance = null;

	/**
	 * Constructor.
	 */
	protected AuthNamespace() {

		File persistentDir = DotKeplerManager.getInstance()
				.getPersistentModuleDirectory("core");
		_anFileName = persistentDir.toString();
		if (!_anFileName.endsWith(File.separator)) {
			_anFileName += File.separator;
		}
		_anFileName += _saveFileName;
		if (isDebugging)
			log.debug(_anFileName);
		_configuredAuthorities = new Vector<String>(1);

		// make sure the userDirPath exists
		new File(DotKeplerManager.getDotKeplerPath()).mkdirs();

	}

	/**
	 * @return the Authority
	 */
	public String getAuthority() {
		return _authority;
	}

	private void setAuthority(String authority) throws Exception {
		int ind;
		if (authority.startsWith("http://")) {
			authority = authority.substring(7);
		} else if (authority.startsWith("https://")) {
			authority = authority.substring(8);
		} else if ( (ind = authority.indexOf("://")) >= 0) {
			authority = authority.substring(ind+3);
		}
		if (authority.indexOf(':') >= 0) {
			throw new Exception("authority can not contain a colon");
		}
		_authority = authority;
	}

	/**
	 * @return the Namespace
	 */
	public String getNamespace() {
		return _namespace;
	}

	private void setNamespace(String namespace) throws Exception {
		if (namespace.indexOf(':') >= 0) {
			throw new Exception("namespace can not contain a colon");
		}
		_namespace = namespace;
	}

	public String getAuthNamespace() {
		return getAuthority() + ":" + getNamespace();
	}

	/**
	 * The purpose here is to return a unique string based on the authNamespace
	 * string that can be used for including in the name of Instance specific
	 * strings such as the .kepler directory name. This is a one way operation
	 * such that the HashValue can be obtained from the AuthNamespace string but
	 * probably not the other way around.
	 * 
	 */
	public String getFileNameForm() {
		String filenameFriendly = getAuthNamespace();
		filenameFriendly = filenameFriendly.replaceAll("/", ".");
		filenameFriendly = filenameFriendly.replaceAll(":", ".");
		filenameFriendly = filenameFriendly.replaceAll("\\.\\.", ".");
		// log.debug(filenameFriendly);
		return filenameFriendly;
	}

	/**
	 * Set the unique Authority and Namespace for this Kepler Instance.
	 */
	public void initialize() {
		log.debug("initialize()");
		if (_initialized)
			return;
		_initialized = true; // never do this twice.

		/**
		 * Configure the list of authNamespace services that should be used to
		 * get a unique namespace from.
		 */
		readAuthNamespaceServicesConfiguration();

		/*
		 * If the AuthNamespace file does not exist, create it.
		 */
		File ianFile = new File(_anFileName);
		if (!ianFile.exists()) {
			generateAuthNamespace();
		}

		refreshAuthNamespace();

		if (getAuthority() == "uuid") {
			/*
			 * TODO warn the user they are working with a uuid and ask if they'd
			 * like to verify a new namespace from an authority.
			 * 
			 * This should be farmed out to a class in the gui module. If there
			 * is no gui around then an alternate path should be created for
			 * doing this.
			 */
		}
	}
	
	/**
	 * Don't use this method.  Only if the LSID_GENERATOR table is corrupt
	 * or deleted do we want to get a new Authorized Namespace.
	 */
	public void getNewAuthorizedNamespace() {
		File anFile = new File(_anFileName);
		anFile.delete();
		generateAuthNamespace();
		refreshAuthNamespace();
	}

	/**
	 * This initializer method is only to be used for JUnit testing.
	 */
	public void initializeForTesting(String authority, String namespace) {
		log.debug("initializeForTests()");
		if (_initialized)
			return;
		_initialized = true;

		// use a different file for testing
		_anFileName = ProjectLocator.getProjectDir().toString();
		if (!_anFileName.endsWith(File.separator)) {
			_anFileName += File.separator;
		}
		_anFileName += "AuthorizedNamespaceTesting";
		if (isDebugging)
			log.debug(_anFileName);

		// use a generic authority and namespace for testing
		_authority = authority;
		_namespace = namespace;

		try {
			writeAuthorizedNamespace();
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
		_initialized = true;
	}

	/**
	 * Populate the configuredAuthorities list from the config.xml file.
	 */
	private void readAuthNamespaceServicesConfiguration() {
		ConfigurationManager cm = ConfigurationManager.getInstance();
		Module cmMod = ConfigurationManager.getModule("configuration-manager");
		String path = "authNamespaceServices.authNamespaceService";
		List<ConfigurationProperty> anProps = cm.getProperties(cmMod,
				path);
		Hashtable<Integer,String> addys = new Hashtable<Integer,String>();
		for (ConfigurationProperty anProp : anProps) {
			ConfigurationProperty ordProp = anProp.getProperty("ordering");
			ConfigurationProperty urlProp = anProp.getProperty("url");
			
			String ordStr = ordProp.getValue();
			Integer ordInt = Integer.parseInt(ordStr);
			String urlStr = urlProp.getValue();
			
			addys.put(ordInt,urlStr);
		}
		for (int i = 1; i <= addys.size(); i++) {
			Integer I = new Integer(i);
			String addy = addys.get(I);
			if (isDebugging) log.debug(addy);
			_configuredAuthorities.add(addy);
		}
	}

	/**
	 * Try to query the configured authorities to get an assigned namespace.
	 * Otherwise generate a uuid as the namespace.
	 */
	private void generateAuthNamespace() {
		boolean verified = false;

		/*
		 * First, try to connect to a server to get a verified namespace.
		 */
		try {

			for (int i = 0; i < _configuredAuthorities.size(); i++) {
				String currentAuthority = _configuredAuthorities.get(i);
				verified = queryAuthorizedNamespace(currentAuthority);
				if (_authority == null || _namespace == null) {
					verified = false;
				}
				if (verified) {
					log.info("A unique namespace, " + _namespace
							+ ", was successfully retrieved from authority "
							+ _authority);
					break;
				}
			}

		} catch (Exception e1) {
			log.error(e1.toString());
			e1.printStackTrace();
		}

		/*
		 * If a verified namespace cannot be retrieved, generate one.
		 */
		if (!verified) {
			/*
			 * Generate a probabilistically unique random ID for the namespace
			 * and set uuid as the authority.
			 */
			log.warn("Generating UUID based AuthNamespace");

			try {
				setAuthority("uuid");
				setNamespace(UUID.randomUUID().toString());

				writeAuthorizedNamespace();
			} catch (Exception e) {
				log.error("Unable to write InstanceAuthNamespace file: "
						+ _anFileName);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Read in the Authority and Namespace from the AuthorizedNamespace file.
	 */
	public void refreshAuthNamespace() {
		if (isDebugging)
			log.debug("refreshAuthNamespace()");

		try {
			InputStream is = null;
			ObjectInput oi = null;
			try {
				is = new FileInputStream(_anFileName);
				oi = new ObjectInputStream(is);
    			Object newObj = oi.readObject();
    
    			String theString = (String) newObj;
    			int firstColon = theString.indexOf(':');
    			setAuthority(theString.substring(0, firstColon));
    			setNamespace(theString.substring(firstColon + 1));
			} finally {
			    if(oi != null) {
			        oi.close();
			    }
			    if(is != null) {
			    	is.close();
			    }
			}
		} catch (FileNotFoundException e) {
			log.error(_saveFileName + " file was not found: " + _anFileName);
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Unable to create ObjectInputStream");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			log.error("Unable to read from ObjectInput");
			e.printStackTrace();
		} catch (Exception e) {
			log.error(e.toString());
			e.printStackTrace();
		}

		if (isDebugging) {
			log.debug("Instance Authority: " + getAuthority());
			log.debug("Instance Namespace: " + getNamespace());
			log.debug("Instance AuthNamespace: " + getAuthNamespace());
			log.debug("Instance Hash Value: " + getFileNameForm());
		}

	}

	/**
	 * Serialize the authority:namespace String to the AuthorizedNamespace file.
	 */
	private void writeAuthorizedNamespace() throws Exception {
		if (isDebugging)
			log.debug("writeAuthorizedNamespace()");

		File ianFile = new File(_anFileName);
		if (!ianFile.exists()) {
			ianFile.delete();
		}
		String authNamespace = getAuthNamespace();
		if (authNamespace == null) {
			throw new Exception("authNamespace is null");
		}
		OutputStream os = new FileOutputStream(_anFileName);
		ObjectOutputStream oos = null;
		try {
		    oos = new ObjectOutputStream(os);
		    oos.writeObject(authNamespace);
		    oos.flush();
		} finally {
		    if(oos != null) {
		        oos.close();
		    }
		}

		try {
            // Every time we write a new AuthNamespace make sure there is
            // at least one row added to the LSID_GENERATOR table for the
            // new Authority and Namespace, this is how LSIDGenerator
            // can tell if the LSID_GENERATOR table has been deleted since
            // the last time it accessed it.
            LSIDGenerator.insertRow(getAuthority(), getNamespace(), 0, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/**
     * Return a unique namespace from the provided authority string.
     * 
	 * @param authority
	 * @return boolean true if success
	 * @throws Exception
	 */
	private boolean queryAuthorizedNamespace(String authority) throws Exception {
		if (isDebugging)
			log.debug("queryAuthorizedNamespace( " + authority + ")");

		// thwart malicious robots
		String data = URLEncoder.encode("username", "UTF-8") + "="
				+ URLEncoder.encode("kepler", "UTF-8"); 
		data += "&" + URLEncoder.encode("password", "UTF-8") + "="
				+ URLEncoder.encode("kepler", "UTF-8");

		try {
			// Send the request
			URL url = new URL(authority);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStream os = conn.getOutputStream();
			OutputStreamWriter wr = new OutputStreamWriter(os);
			wr.write(data);
			wr.flush();

			// Get the response as a set of Properties
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn
					.getInputStream()));
			String line;
			Properties props = new Properties();
			while ((line = rd.readLine()) != null) {
				if (isDebugging) log.debug(line);
				line = line.trim();
				if (line.equals(""))
					continue;
				int equalsIndex = line.indexOf("=");
				if (equalsIndex > 0) {
					String key = line.substring(0, equalsIndex).trim();
					String value = line.substring(equalsIndex + 1).trim();
					props.setProperty(key, value);
				}
			}
			wr.close();
			rd.close();

			// Check to see if there was an error
			String errorString = props.getProperty("error");
			if (errorString != null) {
				log.warn(errorString);
			} else {
				// Check and set the returned namespace
				String newNamespace = props.getProperty("namespace");
				if (newNamespace != null && !newNamespace.equals("")) {
					setAuthority(authority);
					setNamespace(newNamespace);

					try {
						writeAuthorizedNamespace();
						return true;
					} catch (Exception e) {
						log.error("Unable to write " + _saveFileName
								+ " file: " + _anFileName);
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}

		return false;
	}

	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static AuthNamespace getInstance() {
		if (instance == null) {
			instance = new AuthNamespace();
			instance.initialize();
		}
		return instance;
	}
}
