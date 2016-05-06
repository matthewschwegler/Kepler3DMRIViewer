/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-02-11 09:32:24 -0800 (Tue, 11 Feb 2014) $' 
 * '$Revision: 32584 $'
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

package org.kepler.objectmanager.cache;

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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.configuration.ConfigurationManager;
import org.kepler.configuration.ConfigurationProperty;
import org.kepler.sms.NamedOntModel;
import org.kepler.sms.OntologyCatalog;
import org.kepler.util.DotKeplerManager;
import org.kepler.util.FileUtil;
import org.kepler.util.sql.DatabaseFactory;

import ptolemy.util.MessageHandler;

public class LocalRepositoryManager {
	private static final Log log = LogFactory
			.getLog(LocalRepositoryManager.class.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	public static final String KAR_LOCAL_REPOS_TABLE_NAME = "KAR_LOCAL_REPOS";

	/** The name in the configuration file containing the display name
	 *  for modules.
	 */
	private static final String MODULE_DISPLAY_NAME = "moduleDisplayName";
	
	/**
	 * The list of KAR files that make up the library.
	 */
	private Vector<File> _karFiles;
	
	/** List of MoML files that make up the library. */
	private Vector<File> _xmlFiles;

	private Connection _conn;
	private Statement _stmt;
	private PreparedStatement _insertPrepStmt;
	private PreparedStatement _deletePrepStmt;
	private PreparedStatement _updateNamePrepStmt;
	private PreparedStatement _updatePathPrepStmt;
	
	private LinkedHashMap<String, TreeModel> _folderModel;

	public TreeModel getFolderModel(String name) {
		return _folderModel.get(name);
	}

	public void setFolderModel(LinkedHashMap<String, TreeModel> folderModel) {
		_folderModel = folderModel;
	}

	/**
	 * The local repositories that the cache is built from. Keys are the
	 * repository names, values are the directory files.
	 */
	private LinkedHashMap<LocalRepository, String> _localRepositories;

	/**
	 * This is the folder that we save KARs to by default.
	 */
	private LocalRepository _localSaveRepo;

	/**
	 * The file in the module readwrite area where we'll save the name of the
	 * user selected local repository to save KAR files in by default.
	 */
	private String _localSaveRepoFileName;

	/**
	 * We keep a copy of the initial local repositories so we can see if they
	 * have changed.
	 */
	private LinkedHashMap<LocalRepository, String> _checkpointRepos;

	private File _defaultUserWorkflowDirectory;

	/**
	 * Empty Constructor.
	 */
	public LocalRepositoryManager() {
		
		DotKeplerManager dkm = DotKeplerManager.getInstance();

		// Set up file name for storing default local save directory
		File modDir = dkm.getTransientModuleDirectory("core");
		if (modDir != null) {
			_localSaveRepoFileName = modDir.toString();
		} else {
			_localSaveRepoFileName = System.getProperty("KEPLER");
		}
		if (!_localSaveRepoFileName.endsWith(File.separator)) {
			_localSaveRepoFileName += File.separator;
		}
		_localSaveRepoFileName += "LocalSaveRepository";

		// Set up the location of the default workflows directory
		_defaultUserWorkflowDirectory = dkm.getPersistentUserWorkflowsDir();//new File(persistentDir, "workflows");
		if (!_defaultUserWorkflowDirectory.exists()) {
			_defaultUserWorkflowDirectory.mkdirs();
		}

		// Set up prepared statements for select,insert,update,delete
		// local repository information
		try {
			_conn = DatabaseFactory.getDBConnection();
		} catch (Exception e) {
		    MessageHandler.error("Error opening cache database.", e);
		    return;
		}

		try {
			_stmt = _conn.createStatement();
			_insertPrepStmt = _conn.prepareStatement("insert into "
					+ KAR_LOCAL_REPOS_TABLE_NAME
					+ " (name,path) values ( ?, ? ) ");
			_deletePrepStmt = _conn.prepareStatement("DELETE FROM "
					+ KAR_LOCAL_REPOS_TABLE_NAME + " WHERE PATH = ? ");
			_updateNamePrepStmt = _conn.prepareStatement("UPDATE "
					+ KAR_LOCAL_REPOS_TABLE_NAME
					+ " SET NAME = ? WHERE PATH = ? ");
			_updatePathPrepStmt = _conn.prepareStatement("UPDATE "
			        + KAR_LOCAL_REPOS_TABLE_NAME
			        + " SET PATH = ? WHERE NAME = ? ");

		} catch (SQLException e) {
			e.printStackTrace();
		}

		initLocalRepos();
		initLocalSaveRepo();
	}

	/**
	 * Initialize local repositories that contain KAR files.
	 */
	private void initLocalRepos() {

		// Check to see if there are any local repositories
		try {
			String query = "SELECT count(*) FROM " + KAR_LOCAL_REPOS_TABLE_NAME;
			if (isDebugging)
				log.debug(query);
			ResultSet rs = null;
			try {
				rs = _stmt.executeQuery(query);
				if (rs != null && rs.next()) {
					int cnt = rs.getInt(1);
					if (cnt <= 0) {
						// Set the defaults if there are no local repositories in
						// the database
						this.setDefaultLocalRepos();
					}
				}
			} finally {
				if(rs != null) {
					rs.close();
				}
			}
		} catch (SQLException sqle) {
			log.error(sqle.getMessage());
		}

		refreshReposFromDB();

	}
	
	public LinkedHashMap<LocalRepository, String> selectReposFromDB() {
		LinkedHashMap<LocalRepository, String> localRepos = new LinkedHashMap<LocalRepository, String>();
		try {
			String query = "SELECT name,path FROM "
					+ KAR_LOCAL_REPOS_TABLE_NAME + " order by name";
			if (isDebugging)
				log.debug(query);
			ResultSet rs = null;
			try {
				rs = _stmt.executeQuery(query);
				if (rs != null) {
					while (rs.next()) {
						String theName = rs.getString(1);
						String paths = rs.getString(2);
						LocalRepository repo = new LocalRepository(paths);
						localRepos.put(repo, theName);
					}
				}
			} finally {
				if(rs != null) {
					rs.close();
				}
			}
		} catch (SQLException sqle) {
			log.error(sqle.getMessage());
			sqle.printStackTrace();
		}
		return localRepos;
	}

	/**
	 * Repopulate our local hashtable from the database.
	 */
	private void refreshReposFromDB() {
		_localRepositories = selectReposFromDB();
	}

	/**
	 * Return a list of all the KAR files that were found after calling
	 * scanReposForKarFiles()
	 * 
	 * @return Vector of File objects pointing to KAR files
	 */
	public Vector<File> getKarFiles() {
		return _karFiles;
	}
	
	/** Return a list of all the XML files that were found after calling
	 *  scanReposForXMLFiles()
	 */
	public Vector<File> getXMLFiles() {
	    //return new Vector<File>();
	    return _xmlFiles;
	}

	/**
	 * Search for Kar files in local Kar Repositories and build a list of all
	 * the KAR files that are found. This list can be retrieved using
	 * getKarFiles()
	 */
	public void scanReposForKarFiles() {
		log.info("Scanning Local Repositories for KAR files...");
		_karFiles = new Vector<File>();
		_xmlFiles = new Vector<File>();
		
		LinkedHashMap<String, TreeModel> folderModel = new LinkedHashMap<String, TreeModel>();
		setFolderModel(folderModel);

		for (LocalRepository repoRoot : getLocalRepositories().keySet()) {
			if (isDebugging) {
				log.debug("Recursing for Kar files in local repository: "
						+ repoRoot.toString());
			}
			refreshFolderModelForRepo(repoRoot);
		}
	}

	/**
	 * Refresh the folder model for the specified local repository.
	 * 
	 * @param repo
	 */
	public void refreshFolderModelForRepo(LocalRepository repo) {
		if (isDebugging) log.debug("refreshFolderModelForRepo("+repo.toString()+")");

		String repoName = getLocalRepositories().get(repo);
		if (repoName == null) {
			log.warn("Error: not a local repository: " + repo);
			return;
		}

		TreeModel tm = getFolderModel(repoName);
		if (tm == null) {
			tm = new DefaultTreeModel(new DefaultMutableTreeNode(repo));
			_folderModel.put(repoName, tm);
		}

		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tm.getRoot();
		root.removeAllChildren();

		for(File dir : repo.getDirectories()) {
		    findKarsRecursive(dir, 20, root);
		}		
	}
	
	/**
	 * Given the File object for a folder in a local repository, return the
	 * corresponding DefaultMutableTreeNode object from the Folder model.
	 * 
	 * @param folder
	 * @return
	 */
	public DefaultMutableTreeNode getFolderModelNode(File folder) {
		String folderStr = folder.toString();
		for (LocalRepository repo : getLocalRepositories().keySet()) {
		    for(File repoRootDir : repo.getDirectories()) {
    			if (folderStr.equals(repoRootDir.toString())) {
    				return (DefaultMutableTreeNode) getFolderModel(
    						getLocalRepositories().get(repo)).getRoot();
    			} else if (folderStr.startsWith(repoRootDir.toString())) {
    				String remainder = folderStr.substring(repoRootDir.toString().length());
    				if (remainder.startsWith(File.separator)) {
    					remainder = remainder.substring(1);
    				}
    				StringTokenizer st = new StringTokenizer(remainder,
    						File.separator);
    
    				TreeModel tm = getFolderModel(getLocalRepositories().get(repo));
    				DefaultMutableTreeNode root = (DefaultMutableTreeNode) tm
    						.getRoot();
    				String dir = null;
    				File current = repoRootDir;
    				int count = st.countTokens();
    				while (st.hasMoreTokens()) {
    					dir = st.nextToken();
    					current = new File(current.toString(), dir);
    					DefaultMutableTreeNode dmtn = checkChildren(root, current);
    					if (dmtn == null) {
    						return null;
    					}
    					if (count == 1) {
    						return dmtn;
    					} else {
    						root = dmtn;
    					}
    					count--;
    				}
    			}
		    }
		}

		return null;
	}

	/**
	 * 
	 * @param dmtn
	 * @param dir
	 * @return the TreeNode that corresponds to the given directory name
	 */
	private DefaultMutableTreeNode checkChildren(DefaultMutableTreeNode dmtn,
			File folder) {

		Enumeration<?> children = dmtn.children();
		while (children.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) children
					.nextElement();
			if (child.getUserObject().equals(folder)) {
				return child;
			}

		}
		return null;
	}

	public void refreshFolderModelForFolder(File folder) {

		// find the corresponding TreeNode
		DefaultMutableTreeNode dmtn = getFolderModelNode(folder);
		if (dmtn == null) {
			return;
		}

		// remove the children of that TreeNode
		dmtn.removeAllChildren();

		// rebuild the children of the TreeNode
		findKarsRecursive(folder, 20, dmtn);

	}

	/**
	 * Recursive function for finding files that end in ".kar" (case
	 * insensitive).
	 * 
	 * @param dir
	 *            The root of the local repository that contains KAR files
	 * @param depth
	 *            The maximum recursion depth
	 */
	private void findKarsRecursive(File dir, int depth,
			DefaultMutableTreeNode tn) {
		if (isDebugging)
			log.debug(depth + ": " + dir.toString());
		if (!dir.exists()) {
			log.warn(dir.toString() + " does not exist");
			return;
		}
		if (!dir.isDirectory()) {
			log.warn(dir.toString() + " is not a directory");
			return;
		}
		if (depth < 0) {
			log.warn(dir.toString() + " is too deep");
			return;
		}

		File[] listing = dir.listFiles();
		for (int i = 0; i < listing.length; i++) {
			File currentListing = listing[i];
			if (currentListing.isDirectory()) {
				if (currentListing.getName().equals(".svn")) {
					// skip .svn folders
				} else if (currentListing.getName().contains(".")) {
					// skip any folders that contain periods
					// ptolemy cannot handle periods in NamedObj Names.
				    System.out.println("WARNING: skipping due to periods: " + currentListing);
				} else {
					DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(
							currentListing);
					tn.add(dmtn);
					findKarsRecursive(currentListing, (depth - 1), dmtn);
				}
			} else {
				if (currentListing.getName().toLowerCase().endsWith(".kar")) {
					_karFiles.addElement(currentListing);
				} else if(currentListing.getName().toLowerCase().endsWith(".xml")) {
				    _xmlFiles.addElement(currentListing);
				}
			}
		}
	}

	/**
	 * 
	 */
	private void initLocalSaveRepo() {
		File localSaveRepoFile = new File(_localSaveRepoFileName);

		if (localSaveRepoFile.exists()) {
			if (isDebugging) {
				log.debug("localSaveRepo exists: "
						+ localSaveRepoFile.toString());
			}

			try {
				InputStream is = null;
				ObjectInput oi = null;
				try {
					is = new FileInputStream(localSaveRepoFile);
				    oi = new ObjectInputStream(is);
				    Object newObj = oi.readObject();
				    setLocalSaveRepo((File) newObj);
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
					localSaveRepoFile.delete();
				} catch (Exception e2) {
					log.warn("Unable to delete localSaveRepoFile: "
							+ e2.getMessage());
				}
			}
		}
		try {
			setDefaultSaveRepo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param directory
	 */
	public void setLocalSaveRepo(File dir) {
		if (isDebugging)
			log.debug("setLocalSaveRepo(" + dir + ")");
		if (getRepositoryForFile(dir) != null) {
			_localSaveRepo = new LocalRepository(dir);
			serializeLocalSaveRepo();
		}
	}

	/**
	 * Set the default Save repository.
	 * 
	 * @throws Exception
	 */
	public void setDefaultSaveRepo() {
		if (isDebugging)
			log.debug("setDefaultSaveRepo()");

		// Use the default workflows directory
		
		// see if it's the root of a local repository
		LocalRepository repo = getRepositoryForFile(_defaultUserWorkflowDirectory);
		if(repo == null) {
		    // see if it's a subdirectory in a local repository
		    repo = getContainingLocalRepository(_defaultUserWorkflowDirectory);
		}
		
		if(repo != null) {
			setLocalSaveRepo(repo.getDefaultDirectory());
			return;
		}

		// If there is no default workflows directory
		// Set the save repo to the first local repo in the list
		for (LocalRepository localRepo : _localRepositories.keySet()) {
			setLocalSaveRepo(localRepo.getDefaultDirectory());
			break;
		}
	}

	/**
	 * Set the default local repositories to be the kar directories for each of
	 * the modules in the system along with a default workflows directory.
	 */
	public void setDefaultLocalRepos() {
		if (isDebugging)
			log.debug("setDefaultLocalRepos()");
		

		// Set up a default list of local repository directories
		_localRepositories = new LinkedHashMap<LocalRepository, String>();

		try {
			String deleteAll = "delete from " + KAR_LOCAL_REPOS_TABLE_NAME;
			if (isDebugging)
				log.debug(deleteAll);
			_stmt.executeUpdate(deleteAll);
		} catch (SQLException sqle) {
			log.error(sqle.getMessage());
			sqle.printStackTrace();
		}
		
		final DotKeplerManager dkm = DotKeplerManager.getInstance();

		for (Module module : ModuleTree.instance()) {
			if (isDebugging) log.debug("Checking for kar directory in " + module.getStemName());
			//String modName = m.getName();
			String modName = module.getStemName();
			File modDir = dkm.getPersistentModuleDirectory(modName);
			File karDir = new File(modDir, "kar");

			if (karDir.isDirectory() && karDir.exists()) {
				if (isDebugging)
					log.debug(karDir + " " + modName);
				try {
					String repoName = getLocalRepositoryName(modName);
					addLocalRepoRootDir(karDir, repoName);
				} catch (Exception e) {
				    MessageHandler.error("Error adding local repository " + karDir, e);
				}
			}
			
			// NOTE: use the fully versioned name of the module instead of
			// the name without a version since the demo workflows can 
			// change between versions of the same module.
			// 
            File workflowDir = dkm.getPersistentModuleWorkflowsDir(module.getName());
            
            // only add the demos to the library.
            // <module>/workflows/data may contain XML files that generate
            // errors when parsed.
            // see http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5643
            File demoDir = new File(workflowDir, "demos");
			if(demoDir.isDirectory() && demoDir.exists()) {
                if (isDebugging)
                    log.debug(demoDir + " " + modName);
                try {
					String repoName = getLocalRepositoryName(modName);
                    addLocalRepoRootDir(demoDir, repoName);
                } catch (Exception e) {
                    MessageHandler.error("Error adding local repository " + karDir, e);
                }			    
			}
			/*
			else if (modName.equals(Module.PTOLEMY) || modName.matches(Module.PTOLEMY+"-\\d+\\.\\d+") 
	                || modName.matches(Module.PTOLEMY_KEPLER+"-\\d+\\.\\d+")) {
			    
                Project project = ProjectLocator.getAntProject();
                // NOTE: getAntProject() may return null; in this case
                // create a new one.
                if (project == null) {
                    project = new Project();
                }
                final FileSet fileSet = new FileSet();
                fileSet.setProject(project);
                fileSet.setDir(module.getSrc());
                XXX space added to prevent closing comment
                fileSet.setIncludes("** /demo");
                fileSet.setExcludesfile(new File(project.getBaseDir(),
                        "build-area/settings/ptolemy-excludes"));
                final String[] files = fileSet.getDirectoryScanner()
                        .getIncludedDirectories();
                for (String name : files) {
                    String repoName = modName;
                    repoName = repoName.substring(0, 1).toUpperCase()
                            + repoName.substring(1);
                    try {
                        System.out.println(name);
                        addLocalRepoRootDir(new File(module.getSrc(), name),
                                repoName);
                    } catch (Exception e) {
                        MessageHandler.error("Error adding local repository " + karDir, e);
                    }
                }
			}
		   */
		}

		// Include a default workflows directory
		try {
			addLocalRepoRootDir(_defaultUserWorkflowDirectory, dkm.getPersistentUserWorkflowsDirName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (getLocalRepositories().size() <= 0) {
			log.error("No local repositories specified.");
		}

	}

	/**
	 * Serialize the local save repository to a file on disk so it can be loaded
	 * the next time Kepler starts.
	 */
	private void serializeLocalSaveRepo() {
		if (isDebugging)
			log.debug("serializeLocalSaveRepo()");

		File localSaveRepoFile = new File(_localSaveRepoFileName);
		if (localSaveRepoFile.exists()) {
			if (isDebugging)
				log.debug("delete " + localSaveRepoFile);
			localSaveRepoFile.delete();
		}
		try {
			OutputStream os = new FileOutputStream(localSaveRepoFile);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(_localSaveRepo.getDefaultDirectory());
			oos.flush();
			oos.close();
			if (isDebugging) {
				log.debug("wrote " + localSaveRepoFile);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Save the local repo dirs to a private variable so we can determine when
	 * they have changed between build points.
	 */
	public void setCheckpoint() {
		_checkpointRepos = (LinkedHashMap<LocalRepository, String>) getLocalRepositories()
				.clone();
	}

	/**
	 * Reset the list of local repositories to be what it was the last time the
	 * setCheckpoint() method was called.
	 */
	public void restoreCheckpoint() {
		_localRepositories = (LinkedHashMap<LocalRepository, String>) _checkpointRepos
				.clone();
	}

	/**
	 * Check to see if the LocalRepositories have changed since the last time
	 * setCheckpoint() was called.
	 * 
	 * @return true if the local repositories have changed
	 */
	public boolean changedSinceCheckpoint() {

		if (!_checkpointRepos.equals(getLocalRepositories())) {
			return true;
		}
		return false;
	}

	public boolean isLocalRepositoryName(String name) {
		for (String l : getLocalRepositories().values()) {
			if (l.equals(name)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * This method only removes the given directory from the in-memory repository
	 * list.  To update the database you must call the synchronizeDB() method.
	 * 
	 * @param directory
	 * @throws Exception
	 *             if the directory could not be removed
	 */
	public void removeLocalRepoRootDir(File directory) throws Exception {
		if (isDebugging)
			log.debug("removeLocalRepoRootDir(" + directory + ")");

		// do not remove anything if there is only one local repo left
		if (_localRepositories.size() == 1) {
			throw new Exception(
					"There must always be at least one local repository directory");
		}
		LocalRepository repo = getRepositoryForFile(directory);
		if(repo != null) {
			boolean isSaveDir = false;
			if (getSaveRepository().equals(directory)) {
				isSaveDir = true;
			}
			
			int numLeft = repo.removeDir(directory);
			if(numLeft == 0) {
			    _localRepositories.remove(repo);
			}
			
			if (isSaveDir) {
				setDefaultSaveRepo();
			}
		} else {
			throw new Exception(
					"Unable to remove directory "
							+ directory
							+ "\n No Local Repository directory matching that name was found.");
		}
	}
	
	/**
	 * Synchronize the KAR_LOCAL_REPOS table with the _localRepositories private variable list.
	 * This method only removes rows from the table that are not in the list.
	 * It does not add rows to the table for extra entries that are in the list.
	 */
	public void synchronizeDB() {

		LinkedHashMap<LocalRepository, String> localRepos = selectReposFromDB();
		for (LocalRepository repo : localRepos.keySet()) {
			if (!_localRepositories.containsKey(repo)) {
				try {
					// this will cascade deletion of KARs and KAR contents
					// from the tables
					_deletePrepStmt.setString(1, repo.toString());
					_deletePrepStmt.executeUpdate();
					_conn.commit();
				} catch (SQLException sqle) {
					sqle.printStackTrace();
				}
			}
		}
	}

	/**
	 * Change the name of a local repository.
	 * 
	 * @param directory the default root directory of the repository
	 * @param name
	 * @throws Exception
	 */
	public void setLocalRepoName(File directory, String name) throws Exception {
		if (_localRepositories.containsValue(name)) {
			throw new Exception(
					"This name is already assigned to a local repository directory.");
		}
		Iterator<NamedOntModel> models = OntologyCatalog.instance()
				.getNamedOntModels();
		while (models.hasNext()) {
			if (models.next().getName().equals(name)) {
				throw new Exception(
						"This name is already being used for an ontology.");
			}
		}
		LocalRepository repo = getRepositoryForFile(directory);
		if(repo == null) {
		    throw new Exception("No repository found with directory " + directory);
		}
		try {
			_updateNamePrepStmt.setString(1, name);
			_updateNamePrepStmt.setString(2, repo.getDirectoriesAsString());
			_updateNamePrepStmt.executeUpdate();
			_conn.commit();
			String oldName = _localRepositories.put(repo, name);
			if (isDebugging)
				log.debug(oldName + " was changed to " + name);
		} catch (SQLException sqle) {
			log.warn(sqle.getMessage());
		}
	}

	/**
	 * Given a file, return true if it is in a local repository, false if it is
	 * not.
	 * 
	 * @param aFile
	 * @return
	 */
	public boolean isInLocalRepository(File aFile) {
	    LocalRepository containingRepo = getContainingLocalRepository(aFile);
		if (containingRepo != null) {
			return true;
		}
		return false;
	}

	/**
	 * Given a file, return the local repository that it is in or null if it is
	 * not in a local repository.  This also returns null if the file passed in
	 * is a local repository.
	 * 
	 * @param aFile
	 * @return
	 */
	public LocalRepository getContainingLocalRepository(File aFile) {
		for (LocalRepository repo : getLocalRepositories().keySet()) {
		    for(File repoRootDir : repo.getDirectories()) {
    			try {
    				if (FileUtil.isSubdirectory(repoRootDir, aFile)) {
    					return repo;
    				}
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
		    }
		}
		return null;
	}

	/**
	 * Convenience method for addLocalRepoRootDir(File, String)
	 * 
	 * @param directory
	 * @throws Exception
	 */
	public void addLocalRepoRootDir(File directory) throws Exception {
		addLocalRepoRootDir(directory, directory.getName());
	}

	/** Add a local repository for a given root directory and name. If
	 *  a repository with that name already exists, the directory is added
	 *  to the list of root directories for that repository.
	 * @param directory
	 * @throws Exception
	 */
	public void addLocalRepoRootDir(File directory, String name)
			throws Exception {
		if (isDebugging)
			log.debug("addLocalRepoRootDir(" + directory + ", " + name + ")");
		if (!directory.isDirectory()) {
			throw new Exception(
					"The specified local repository root must be a directory");
		}
		String selFileName = FileUtil.clean(directory);
		
		LocalRepository existingRepo = null;
		
		// check to make sure this directory is not a sub directory
		// of any existing local repositories
		for (Map.Entry<LocalRepository, String> entry : getLocalRepositories().entrySet()) {
			
		    final LocalRepository repo = entry.getKey();
		    
		    for(File localDir : repo.getDirectories()) {
    			// make sure this selection doesn't match an existing local repository exactly
    			if (FileUtil.clean(localDir).equals(selFileName)) {
    				throw new Exception(
    						"Local repository root directory was not added because \n"
    								+ directory
    								+ "\n is already listed as a local repository root directory.");	
    			}
    			
    			// make sure this selection is not a subdirecctory of an existing local repository
    			boolean selectionIsSub = FileUtil.isSubdirectory(localDir, directory);
    			if (selectionIsSub) {
    				throw new Exception(
    						"Local repository was not added because \n"
    								+ directory + "\n is a subdirectory of \n"
    								+ localDir);
    			}
    			
    			// make sure this selection does not contain an existing local repository as a subdirectory
    			boolean repoIsSub = FileUtil.isSubdirectory(directory, localDir);
    			if (repoIsSub) {
    				throw new Exception(
    						"Local repository was not added because \n"
    								+ localDir + "\n is a subdirectory of \n"
    								+ directory);
    			}
		    }
		    
		    final String repoName = entry.getValue();
		    if(repoName.equals(name)) {
		        existingRepo = repo;
		    }
		}

        try {

    		// see if a repository with that name already exists
    		if(existingRepo == null) {
    			_insertPrepStmt.setString(1, name);
    			_insertPrepStmt.setString(2, directory.toString());
    			_insertPrepStmt.executeUpdate();
    			_localRepositories.put(new LocalRepository(directory), name);
    		} else {
    		    String paths = existingRepo.addDirectory(directory);
    		    _updatePathPrepStmt.setString(1, paths);
    		    _updatePathPrepStmt.setString(2, name);
                _updatePathPrepStmt.executeUpdate();
    		}

    		_conn.commit();
    		
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
	}

	public LinkedHashMap<LocalRepository, String> getLocalRepositories() {
		return _localRepositories;
	}

	public File getSaveRepository() {
		if(_localSaveRepo != null) {
		    List<File> dirs = _localSaveRepo.getDirectories();
		    if(!dirs.isEmpty()) {
		        return dirs.get(0);
		    }
		}
	    return null;
	}

	/** Get the repository for a file. Returns null if no repository
     *  has a root directory matching this path.
     */
    public LocalRepository getRepositoryForFile(File file) {
        for(LocalRepository repo : getLocalRepositories().keySet()) {
            if(repo.isFileRepoDirectory(file)) {
                return repo;
            }
        }
        return null;
    }

	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static LocalRepositoryManager getInstance() {
		return LocalRepositoryManagerHolder.INSTANCE;
	}

	private static class LocalRepositoryManagerHolder {
		private static final LocalRepositoryManager INSTANCE = new LocalRepositoryManager();
	}
	
	/** Get the display name of a local repository for a module. */
	public static String getLocalRepositoryName(String moduleName) throws Exception {
		
		String name = null;
		
		// see if the module name is customized
		Module module = ModuleTree.instance().getModuleByStemName(moduleName);
		if(module != null) {
			ConfigurationProperty property = ConfigurationManager.getInstance().getProperty(module);
			if(property != null) {
				ConfigurationProperty nameProperty = property.getProperty(MODULE_DISPLAY_NAME);
				if(nameProperty != null) {
					name = nameProperty.getValue();
				}
			}
		}
		
		// the module name was not customized, so return a name the same as the
		// module name with the first letter upper-case.
		if(name == null) {
			name = moduleName;
			name = name.substring(0, 1).toUpperCase() + name.substring(1);
		}
		
		return name;
	}
	
	/** A repository on the local disk. The repository may have more than one
	 *  root directory.
	 */
	public static class LocalRepository {

	    /** Get the default directory. */
        public File getDefaultDirectory() {
            return _directories.get(0);
        }

        /** Returns true if the given file is one of the directories of this repository. */ 
        public boolean isFileRepoDirectory(File file) {
            for(File dir : _directories) {
                if(dir.equals(file)) {
                    return true;
                }
            }
            return false;
        }

        /** Get the absolute path of the default root directory. */
        @Override
        public String toString() {
            //System.out.println("in toString from: " + new Exception().getStackTrace()[1]);
            return getDefaultDirectory().toString();
        }
        
        /** Returns true iff the given object is a LocalRepository containing
         *  the same directories this LocalRepository.
         */
        @Override
        public boolean equals(Object object) {
            if(object == null || !(object instanceof LocalRepository)) {
            	return false;
            } else {
            	// see if the directories are the same
            	return _directoriesString.equals(((LocalRepository)object)._directoriesString);
            }
        }
        
        /** Get the directories in separated by File.pathSeparator character. */
        private String getDirectoriesAsString() {
        	return _directoriesString;
        }
        
        /** Get the hash code of this LocalRepository. */
        @Override
        public int hashCode() {
        	// use the hash code of the directories in this repository
        	return _directoriesString.hashCode();
        }
        
        /** Remove a root directory.
         *  @return the number of remaining root directories.
         */
        public int removeDir(File directory) {
            _directories.remove(directory);
            _updateDirectoriesString();
            return _directories.size();
        }

        /** Create a new Repository with a root directory. */
        private LocalRepository(File dir) {
        	addDirectory(dir);
        }

        /** Create a new Repository with a set of root directories. 
         * @param paths A string of paths separated by File.pathSeparator.
         */
        private LocalRepository(String paths) {
            String[] parts = paths.split(File.pathSeparator);
            for(String part : parts) {
            	addDirectory(new File(part));
            }
        }

        /** Add a root directory. 
         * @return A string of paths separated by File.pathSeparator.
         */
        private String addDirectory(File directory) {
            _directories.add(directory);
            _updateDirectoriesString();
            return _directoriesString;
        }

        /** Get the root directories. */
        private List<File> getDirectories() {
            return new LinkedList<File>(_directories);
        }
        
        /** Update _directoriesString to be the sorted list of directories
         *  in _directories separated by File.pathSeparator. */
        private void _updateDirectoriesString() {
        	// get the directories as an array and sort lexicographically
        	final File[] array = _directories.toArray(new File[_directories.size()]);
        	Arrays.sort(array);
        	
        	StringBuilder buf = new StringBuilder();
            for(int i = 0; i < array.length - 1; i++) {
                buf.append(array[i].getAbsolutePath());
                buf.append(File.pathSeparatorChar);
            }
            buf.append(array[array.length - 1]);
            _directoriesString = buf.toString();
        }
        
        /** The root directories. */
        private List<File> _directories = new LinkedList<File>();
        
        /** The root directories separated by File.pathSeparator character. */
        private String _directoriesString = "";
	}
	
}