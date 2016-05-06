/*
 * Copyright (c) 2003-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-02-21 11:20:44 -0800 (Thu, 21 Feb 2013) $' 
 * '$Revision: 31475 $'
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.util.DotKeplerManager;

/**
 * This class provides functionality to upload and retrieve objects from the
 * Ecogrid repository for Kepler actor/kar objects.
 * 
 * @author Chad Berkley
 */
public class RepositoryManager {
	private static final Log log = LogFactory.getLog(RepositoryManager.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public static final String RESOURCEBUNDLE_DIR = "ptolemy/configs/kepler";

	/**
	 * The singleton instance.
	 */
	private static RepositoryManager _manager;

	/**
	 * The main vector where repository information is stored.
	 */
	private Vector<Repository> _repositoryVector = new Vector<Repository>();

	/**
	 * The name of the repository to use for saving KARs to.
	 */
	private String _saveRepositoryName;

	/**
	 * The actual repository object for saving KARs to.
	 */
	private Repository _saveRepository;

	/**
	 * The file in the module readwrite area where we'll save the name of the
	 * user selected remote repository to save KAR files to.
	 */
	private String _remoteSaveRepoFileName;

	/**
	 * default constructor
	 */
	public RepositoryManager() throws IOException, Exception {
		if (isDebugging)
			log.debug("RepositoryManager()");

		// Set up file name for storing default local save directory
		File modDir = DotKeplerManager.getInstance()
				.getTransientModuleDirectory("util");
		if (modDir != null) {
			_remoteSaveRepoFileName = modDir.toString();
		} else {
			_remoteSaveRepoFileName = System.getProperty("KEPLER");
		}
		if (!_remoteSaveRepoFileName.endsWith(File.separator)) {
			_remoteSaveRepoFileName += File.separator;
		}
		_remoteSaveRepoFileName += "RemoteSaveRepository";

		ConfigurationProperty prop = ConfigurationManager.getInstance()
				.getProperty(ConfigurationManager.getModule("repository"));
		List reposList = prop.getProperties("repository");
		for (int i = 0; i < reposList.size(); i++) {
			ConfigurationProperty cp = (ConfigurationProperty) reposList.get(i);
			String name = cp.getProperty("name").getValue();

			log.info("adding repository " + name);

			String repository = cp.getProperty("repository").getValue();
			String putPath = cp.getProperty("putpath").getValue();
			String authProtocol = cp.getProperty("authprotocol").getValue();
			String authDomain = cp.getProperty("authdomain").getValue();
			String lsidPath = cp.getProperty("lsidpath").getValue();
			String queryPath = cp.getProperty("querypath").getValue();
			String authenticatedQueryPath = cp.getProperty("authenticatedquerypath").getValue();
			String registry = cp.getProperty("registrypath").getValue();
			String registryauth = cp.getProperty("registryauth").getValue();
			String repClass = cp.getProperty("class").getValue();
			String lsidAuthority = cp.getProperty("lsidAuthority").getValue();
			String searchRepository = cp.getProperty("searchRepository").getValue();
			ConfigurationProperty authorizationPathProperty = cp.getProperty("authorizationPath");
			String authorizationPath = null;
			if (authorizationPathProperty == null)
			{
			  log.warn("The repository "+name+ " doesn't have the authorization path. "+
			   " Please modify the repository configuration file");
			  continue;
			}
			else
			{
			  authorizationPath= authorizationPathProperty.getValue();
			}
			
			//System.out.println("searchRepository: " + searchRepository);
			// create the repository with the given class using reflections
			String[] s = new String[12];
			s[0] = name;
			s[1] = repository;
			s[2] = putPath;
			s[3] = authDomain;
			s[4] = lsidPath;
			s[5] = queryPath;
			s[6] = authenticatedQueryPath;
			s[7] = authorizationPath;
			s[8] = registry;
			s[9] = registryauth;
			s[10] = authProtocol;
			s[11] = lsidAuthority;
			Repository r = createInstance(Class.forName(repClass), s);
			if(searchRepository != null && (searchRepository.equals("true") || searchRepository.equals("TRUE")))
			{
			  r.setIncludeInSearch(true);
			}
			else
			{
			  r.setIncludeInSearch(false);
			}
			_repositoryVector.addElement(r);
		}

		// set the default save repository
		// first check the configuration to see if one is defined there
		/*
		 * if(Configuration.configurations().iterator().hasNext()) {
		 * Configuration config =
		 * (Configuration)Configuration.configurations().iterator().next(); }
		 * 
		 * String saveRepositoryName = null; StringAttribute sa =
		 * (StringAttribute)config.getAttribute("_repository"); if (sa != null)
		 * { saveRepositoryName = sa.getExpression(); //use this override
		 * setSaveRepository(getRepository(saveRepositoryName)); }
		 */
		// do we have an old one?
		initRemoteSaveRepo();
	}

	/**
	 * First we check to see if there is a configuration file containing the
	 * name of the default remote save repository. Then we check the
	 * configuration file.
	 */
	private void initRemoteSaveRepo() {
		if (isDebugging)
			log.debug("initRemoteSaveRepo()");
		File remoteSaveRepoFile = new File(_remoteSaveRepoFileName);

		if (!remoteSaveRepoFile.exists()) {
			setSaveRepository(null);
		} else {
			if (isDebugging) {
				log.debug("remoteSaveRepo exists: "
						+ remoteSaveRepoFile.toString());
			}

			try {
				InputStream is = null;
				ObjectInput oi = null;
				try {
					is = new FileInputStream(remoteSaveRepoFile);
					oi = new ObjectInputStream(is);
					Object newObj = oi.readObject();
				
					String repoName = (String) newObj;
					Repository saveRepo = getRepository(repoName);
					if (saveRepo != null) {
						setSaveRepository(saveRepo);
					}

					return;

				} finally {
					if(oi != null) {
						oi.close();
					}
					if(is != null) {
						is.close();
					}
				}

			} catch (Exception e1) {
				// problem reading file, try to delete it
				log.warn("Exception while reading localSaveRepoFile: "
						+ e1.getMessage());
				try {
					remoteSaveRepoFile.delete();
				} catch (Exception e2) {
					log.warn("Unable to delete localSaveRepoFile: "
							+ e2.getMessage());
				}
			}
		}

	}

	/**
	 * Returns a singleton instance of this class
	 */
	public static RepositoryManager getInstance() throws IOException, Exception {
		if (_manager == null) {
			_manager = new RepositoryManager();
		}
		return _manager;
	}

	/**
	 * return the list of repositories
	 */
	public Iterator<Repository> repositoryList() {
		return _repositoryVector.iterator();
	}

	/**
	 * return the list of repositories
	 */
	public Vector<Repository> getRepositories() {
		return _repositoryVector;
	}

	public void setSearchNone() {
		for (Repository r : getRepositories()) {
			r.setIncludeInSearch(false);
		}
	}

	/**
	 * returns the repository with the given name. if it doesn't exist, this
	 * method returns null.
	 */
	public Repository getRepository(String name) {
		if (isDebugging)
			log
					.debug("getRepository(" + name + ") "
							+ _repositoryVector.size());

		if (name == null)
			return null;

		for (int i = 0; i < _repositoryVector.size(); i++) {
			Repository r = (Repository) _repositoryVector.elementAt(i);
			if (r != null) {
				if (isDebugging)
					log.debug(r.getName());
				if (r.getName().trim().equals(name.trim())) {
					return r;
				}
			}
		}
		return null;
	}

	/**
	 * return the repository that is currently marked as the save repository
	 */
	public Repository getSaveRepository() {
		return _saveRepository;
	}

	/**
	 * set the repository that should be saved to.
	 */
	public void setSaveRepository(Repository rep) {
		if (rep == null) {
			if (isDebugging)
				log.debug("setSaveRepository(null)");
			_saveRepository = null;
			_saveRepositoryName = null;
		} else {
			if (isDebugging)
				log.debug("setSaveRepository(" + rep.getName() + ")");
			_saveRepository = rep;
			_saveRepositoryName = rep.getName();
		}
		serializeRemoteSaveRepo();
	}

	/**
	 * creates and instance of a repository
	 * 
	 *@param newClass
	 *@param arguments
	 *@return Repository
	 */
	private Repository createInstance(Class newClass, Object[] arguments)
			throws Exception {
		Constructor[] constructors = newClass.getConstructors();
		for (int i = 0; i < constructors.length; i++) {
			Constructor constructor = constructors[i];
			Class[] parameterTypes = constructor.getParameterTypes();

			for (int j = 0; j < parameterTypes.length; j++) {
				Class c = parameterTypes[j];
			}

			if (parameterTypes.length != arguments.length) {
				continue;
			}

			boolean match = true;

			for (int j = 0; j < parameterTypes.length; j++) {
				if (!(parameterTypes[j].isInstance(arguments[j]))) {
					match = false;
					break;
				}
			}

			if (match) {
				Repository newRepository = (Repository) constructor
						.newInstance(arguments);
				return newRepository;
			}
		}

		// If we get here, then there is no matching constructor.
		// Generate a StringBuffer containing what we were looking for.
		StringBuffer argumentBuffer = new StringBuffer();

		for (int i = 0; i < arguments.length; i++) {
			argumentBuffer.append(arguments[i].getClass() + " = \""
					+ arguments[i].toString() + "\"");

			if (i < (arguments.length - 1)) {
				argumentBuffer.append(", ");
			}
		}

		throw new Exception("Cannot find a suitable constructor ("
				+ arguments.length + " args) (" + argumentBuffer + ") for '"
				+ newClass.getName() + "'");
	}

	/**
	 * Serialize the remote save repository name to a file on disk so it can be
	 * loaded the next time Kepler starts.
	 */
	private void serializeRemoteSaveRepo() {
		if (isDebugging)
			log.debug("serializeRemoteSaveRepo()");

		File remoteSaveRepoFile = new File(_remoteSaveRepoFileName);
		if (remoteSaveRepoFile.exists()) {
			if (isDebugging)
				log.debug("delete " + remoteSaveRepoFile);
			remoteSaveRepoFile.delete();
		}
		if (_saveRepositoryName != null) {
			try {
				OutputStream os = new FileOutputStream(remoteSaveRepoFile);
				ObjectOutputStream oos = null;
				try {
				    oos = new ObjectOutputStream(os);
    				oos.writeObject(_saveRepositoryName);
    				oos.flush();
    				if (isDebugging) {
    					log.debug("wrote " + remoteSaveRepoFile);
    				}
				} finally {
				    if(oos != null) {
				        oos.close();
				    }
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}