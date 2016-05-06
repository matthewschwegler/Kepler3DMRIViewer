/*
 * Copyright (c) 2004-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2014-04-09 11:01:44 -0700 (Wed, 09 Apr 2014) $' 
 * '$Revision: 32650 $'
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

package org.kepler.objectmanager.library;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.FilteredVisibleTreeModel;
import org.kepler.icon.ComponentEntityConfig;
import org.kepler.kar.KARCacheContent;
import org.kepler.kar.KARCacheManager;
import org.kepler.kar.KARFile;
import org.kepler.moml.FolderEntityLibrary;
import org.kepler.moml.KAREntityLibrary;
import org.kepler.moml.KARErrorEntityLibrary;
import org.kepler.moml.NamedObjId;
import org.kepler.moml.OntologyEntityLibrary;
import org.kepler.objectmanager.cache.CacheManager;
import org.kepler.objectmanager.lsid.KeplerLSID;
import org.kepler.util.sql.DatabaseFactory;

import ptolemy.actor.gui.TableauFrame;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.EntityLibrary;
import ptolemy.vergil.tree.EntityTreeModel;
import ptolemy.vergil.tree.VisibleTreeModel;

/**
 * The LibraryManager is used for managing the Actor Library. It maintains an
 * index for increased performance. Any code that adds, updates, or removes
 * items in the library should use the LibraryManager class directly.
 * 
 *@author Aaron Schultz
 */
public class LibraryManager implements TreeExpansionListener {
	private static class LibraryManagerHolder {
		private static final LibraryManager INSTANCE = new LibraryManager();
	}

	private static final Log log = LogFactory.getLog(LibraryManager.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	/**
	 * The LIID_LABEL is used as the name of the String Attribute that is
	 * attached to Tree Items to keep track of their Library Index ID.
	 */
	public static final String LIID_LABEL = "_LIID";

	/** A mapping of tree to a list of tree paths that are expanded in it. */
	private static final Map<JTree,List<TreePath>> _treeExpansionMap = new WeakHashMap<JTree,List<TreePath>>();
	
	/**
	 * Method for getting an instance of this singleton class.
	 */
	public static LibraryManager getInstance() {
		return LibraryManagerHolder.INSTANCE;
	}

	/**
	 * Return the Library Index ID for the given NamedObj or -1 if an ID cannot
	 * be found.
	 * 
	 * @param obj
	 *            a NamedObj from the Library tree.
	 * @return int Library Index ID or
	 */
	public static int getLiidFor(ComponentEntity obj) {
		if (isDebugging)
			log.debug("getLiidFor(" + obj.getName() + ")");
		int liid = -1;
		try {
			Attribute liidAttribute = obj
					.getAttribute(LibraryManager.LIID_LABEL);
			if (liidAttribute == null) {
				return -1;
			}
			if (liidAttribute instanceof StringAttribute) {
				StringAttribute liidSA = (StringAttribute) liidAttribute;
				liid = Integer.parseInt(liidSA.getExpression());
			} else {
				liid = -1;
			}
		} catch (Exception e) {
			liid = -1;
		}
		return liid;
	}

	/**
	 * This is the composite entity version of the library.
	 */
	private CompositeEntity _actorLibrary;

	/**
	 * This is the workspace passed to us from
	 * ptolemy.actor.gui.UserActorLibrary It is the top level workspace.
	 */
	private Workspace _actorLibraryWorkspace = null;

	/**
	 * The LibIndex is used to build the Actor Library.
	 */
	private LibIndex _libIndex;

	/**
	 * This is the EntityTreeModel version of the library, used in
	 * AnnotatedPTree.
	 */
	private VisibleTreeModel _libraryModel;

	/**
	 * Convenience reference
	 */
	private Connection _conn;

	/**
	 * Convenience reference. Make sure to close your ResultSets since we're
	 * reusing the Statement.
	 */
	private Statement _stmt;

	/**
	 * JTrees that contain the library can be added to the library manager so
	 * they can be refreshed whenever the library changes.
	 */
//	private Vector<JTree> _trees = new Vector<JTree>();	
	private Vector<WeakReference<JTree>> _trees = new Vector<WeakReference<JTree>>();

	// PreparedStatements
    private PreparedStatement _getPopulateInfoForLIIDPrepStmt;
    private PreparedStatement _getLSIDForLIIDPrepStmt;
    private PreparedStatement _getNameValueForLIIDPrepStmt;

	/**
	 * constructor called from getInstance()
	 */
	public LibraryManager() {
		if (isDebugging)
			log.debug("Instantiate LibraryManager");

		try {
			_conn = DatabaseFactory.getDBConnection();
			_stmt = _conn.createStatement();
			
            _getPopulateInfoForLIIDPrepStmt = _conn.prepareStatement(
                    "SELECT PARENT,LFT,RGT,LEVEL,LSID,TYPE,NAME FROM "
                    + LibIndex.LIBRARY_INDEX_TABLE_NAME + " WHERE LIID = ?");

            _getLSIDForLIIDPrepStmt = _conn.prepareStatement("SELECT LSID FROM "
                    + LibIndex.LIBRARY_LSIDS_TABLE_NAME + " WHERE LIID = ?");

            _getNameValueForLIIDPrepStmt = _conn.prepareStatement("SELECT NAME,VALUE FROM "
                    + LibIndex.LIBRARY_ATTRIBUTES_TABLE_NAME + " WHERE LIID = ?");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Given the LSID of a KAR that has been successfully cached this method
	 * will add all of the contents of that KAR into the Library by first
	 * updating the LibIndex and then updating the tree model.
	 * 
	 * @param f
	 * @throws SQLException
	 */
	public void addKAR(File karFile) throws SQLException {

		KARCacheManager kcm = KARCacheManager.getInstance();
		Vector<KARCacheContent> contents = kcm.getKARCacheContents(karFile);
		getIndex().setOrderedInsert(true);
		for (KARCacheContent content : contents) {

			// Update the ontologies in the library index
			Vector<LibItem> ontItems = getIndex().assureOntologyComponent(
					content);
			// update the ontologies in the library tree model
			for (LibItem ontLI : ontItems) {
				ComponentEntity ce = assureTreeItem(ontLI);
				if (ce == null) {
					// Problem adding item to the tree
					// Here we could rebuild the library completely...
					log.error("Component not added to the library properly");
				}
			}

			// Update the folders in the library index
			LibItem folderLI = getIndex().assureKarEntry(content);
			// Update the folders in the library tree model
			assureTreeItem(folderLI);
		}

		getIndex().setOrderedInsert(false);
	}

	/**
	 * Update the LibIndex and tree model for a XML file.
     * 
     * @param f
     * @throws SQLException
     */
    public void addXML(File xmlFile) throws SQLException {

        LibIndex index = getIndex();
        index.setOrderedInsert(true);
        
        LibItem xmlLI = index.assureXML(xmlFile);
        assureTreeItem(xmlLI);
        
        index.setOrderedInsert(false);
    }

	/**
	 * Add a reference to a JTree that is using the Library Tree Model so that
	 * it can be updated when the tree model changes. Whenever getTreeModel() is
	 * used in a gui component, the JTree reference should be added using this
	 * method so that it can get refreshed when the library model changes using
	 * the refreshJTrees() method.
	 * 
	 *@param ptree
	 *            The feature to be added to the LibraryComponent attribute
	 */
	public void addLibraryJTree(JTree ptree) {
		if (isDebugging) {
			log.debug("addLibraryComponent(" + ptree.getName() + ")");
		}
		if (isDebugging)
			log.debug("addLibraryComponent(" + ptree + ")");
		WeakReference<JTree> ptreeWR =  new WeakReference<JTree>(ptree);
		_trees.addElement(ptreeWR);
		if (isDebugging)
			log.debug("_trees size:" + _trees.size());
		
		// add this tree to the map and register for expansion events
		_treeExpansionMap.put(ptree, new LinkedList<TreePath>());
		ptree.addTreeExpansionListener(this);
	}

	/**
	 * Passing a LibItem that has been populated from the database will assure
	 * that the appropriate object is represented in the Library Tree. If the
	 * item already exists it will be returned. If it does not exist then it
	 * will be created along with any parent path components that do not already
	 * exist. The newly created ComponentEntity will then be returned.
	 * 
	 * This method is private because API users should only be using addKAR and
	 * deleteKAR methods to modify the Library.
	 * 
	 * @param li
	 * @return
	 */
	private ComponentEntity assureTreeItem(LibItem li) {
		if (isDebugging)
			log.debug("assureTreeItem(" + li.getName() + ")");
		ComponentEntity ce = null;
		try {
			CompositeEntity current = (CompositeEntity) getTreeModel()
					.getRoot();
			Vector<LibItem> pathItems = _libIndex.getPath(li);
			for (int i = 0; i < pathItems.size(); i++) {
				LibItem pathItem = pathItems.elementAt(i);

				int pathLiid = pathItem.getLiid();
				if (isDebugging) {
					log.debug(pathItem.getName());
					log.debug(current.getName());
				}

				ComponentEntity existingCE = null;
				List children = current.entityList(ComponentEntity.class);
				for (Object child : children) {
					if (child instanceof ComponentEntity) {
						ComponentEntity childCE = (ComponentEntity) child;
						int childLiid = LibraryManager.getLiidFor(childCE);
						if (childLiid == pathLiid) {
							existingCE = childCE;
							break;
						}
					} else {
						log.error("Child was not a ComponentEntity!");
					}
				}
				if (existingCE == null) {
					// add this puppy to the tree
					ComponentEntity newCE = createAndAddTreeItem(current,
							pathItem);
					if (newCE != null) {
						if (newCE instanceof CompositeEntity) {
							current = (CompositeEntity) newCE;
							ce = current;
						} else {
							// ComponentEntity leaf node
							ce = newCE;
							break;
						}
					} else {
						log.debug("fail");
						ce = null;
					}
				} else if (existingCE instanceof CompositeEntity) {
					current = (CompositeEntity) existingCE;
					ce = current;
				}
			} // end for loop
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ce;
	}

    /** Create an empty library. When KeplerGraphFrame starts, it initializes
     * ComponentLibraryTab, which in turn requires a library. Building the
     * library with buildLibrary() can take a long time, so this method can
     * be used to open a KeplerGraphFrame quickly.
     */
    public void buildEmptyLibrary() {
        
        EntityLibrary el = new EntityLibrary(_actorLibraryWorkspace);
        
        LibItem li = new LibItem();
        li.setName("Empty Library");
        li.setType(LibIndex.TYPE_CONCEPT);

        try {
            createAndAddTreeItem(el, li);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        this.setActorLibrary(el);

    }

	/**
	 * This method will synchronize the cache with local repositories. Then it
	 * will rebuild the LibIndex if needed. Then it will generate the Actor
	 * Library from the LibIndex. To force the rebuilding of the LibIndex before
	 * calling this method call getIndex().clear()
	 */
	public void buildLibrary() {
		if (isDebugging)
			log.debug("buildLibrary()");
		log.info("Building Library...");

		try {

			KARCacheManager kcm = KARCacheManager.getInstance();

			// Synchronize the Cache with Local Repositories
			boolean changed = kcm.synchronizeKARCacheWithLocalRepositories();

			// Rebuild the Library Index if needed
			_libIndex = new LibIndex(_conn);

			long start = System.currentTimeMillis();
			if (_libIndex.countItems() <= 0) {
				// rebuild the index if it is empty
				_libIndex.rebuild();
			} else if (!_libIndex.verifyPreorderValues()) {
				// rebuild the index if the preorder values don't add up
				// correctly
				log
						.error("\n\nPreorder values are corrupt! Rebuilding index.\n\n");
				_libIndex.rebuild();
			} else if (changed) {
				// rebuild the index if the KARs in the local repositories have
				// changed
				_libIndex.rebuild();
			}
			if (isDebugging)
				log.debug("\n\nTIME " + (System.currentTimeMillis() - start)
						+ "\n\n");

			// Generate the actor library using the library index
			LibraryGenerator lg = new LibraryGenerator();
			
			// we're using the same workspace so we need to remove the entities
			clearActorLibrary();
			
			CompositeEntity newLibrary = lg.generate(_actorLibraryWorkspace,
					_libIndex);
			this.setActorLibrary(newLibrary);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void removeAllFrameTabs(TableauFrame parent) {
		if (isDebugging)
		{
			log.debug("parent in _trees is:" + parent);
			for (int i = 0; i < _trees.size(); i++) {
				JTree treeEle = _trees.elementAt(i).get();
				log.debug("treeEle in _trees element at " + i + " is:" + treeEle);
			}
			log.debug("_libraryModel:" + _libraryModel);
		}
	}

	/**
	 * Create and add the appropriate type of placeholder object to the parent
	 * CompositeEntity that is in the tree model. Return the object when
	 * finished.
	 * 
	 * Only subtypes of ComponentEntity may be returned. However, the tree item
	 * can represent any type of object, depending on what the type is in the
	 * LibIndex.
	 * 
	 * @param li
	 * @return
	 * @throws NameDuplicationException
	 * @throws IllegalActionException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public ComponentEntity createAndAddTreeItem(CompositeEntity parent,
			LibItem li) throws IllegalActionException,
			NameDuplicationException, IllegalArgumentException, IOException {
		ComponentEntity ce = null;

		// EditorDropTarget in ptolemy requires an entityId so we set one here
		// even though we don't ever use it for anything, the LSID should
		// always be gotten from the LibItem
		NamedObjId lsidSA = null;

		int type = li.getType();
		String displayName = li.getName();
		
		String name = displayName;
		// remove periods from the name since they are not allowed
		if(name.contains(".")) {
			name = name.replaceAll("\\.", ",");
		}
		li.setName(name);

		switch (type) {
		case LibIndex.TYPE_COMPONENT:
			ce = new ComponentEntity(parent, li.getName());
			ce.setDisplayName(displayName);
			// This is needed for ptolemy at the moment
			KeplerLSID lsid = li.getLsid();
			if(lsid != null) {
    			lsidSA = new NamedObjId(ce, NamedObjId.NAME);
    			lsidSA.setExpression(li.getLsid());
			}
			// disable drag and drop for MoML files
			if(li.getAttributeValue(LibIndex.ATT_XMLFILE) != null) {
			    Parameter parameter = new Parameter(ce, "_notDraggable");
			    parameter.setExpression("true");
			}
			ce.setClassName(li.getAttributeValue(LibIndex.ATT_CLASSNAME));
			ComponentEntityConfig.addSVGIconTo(ce);
			copyAttributes(li, ce);
			break;
		case LibIndex.TYPE_NAMED_OBJ:
			ce = new ComponentEntity(parent, li.getName());
			ce.setDisplayName(displayName);
			lsidSA = new NamedObjId(ce, NamedObjId.NAME);
			lsidSA.setExpression(li.getLsid());
			// ce.setClassName(li.getAttributeValue(LibIndex.ATT_CLASSNAME));
			ComponentEntityConfig.addSVGIconTo(ce);
			// To catch WorkflowRuns
			StringAttribute notDraggableAttribute = new StringAttribute(ce, "_notDraggable");
			notDraggableAttribute.setExpression("true");			
			copyAttributes(li, ce);
			break;
		case LibIndex.TYPE_ONTOLOGY:
			ce = new OntologyEntityLibrary(parent, li.getName());
			ce.setDisplayName(displayName);
			break;
		case LibIndex.TYPE_CONCEPT:
			ce = new EntityLibrary(parent, li.getName());
			ce.setDisplayName(displayName);
			break;
		case LibIndex.TYPE_FOLDER:
			ce = new FolderEntityLibrary(parent, li.getName());
			ce.setDisplayName(displayName);
			break;
		case LibIndex.TYPE_LOCALREPO:
			ce = new FolderEntityLibrary(parent, li.getName());
			ce.setDisplayName(displayName);
			break;
		case LibIndex.TYPE_KAR:
			ce = new KAREntityLibrary(parent, li.getName());
			ce.setDisplayName(displayName);
			lsidSA = new NamedObjId(ce, NamedObjId.NAME);
			lsidSA.setExpression(li.getLsid());
			break;
		case LibIndex.TYPE_KAR_ERROR:
			ce = new KARErrorEntityLibrary(parent, li.getName());
			ce.setDisplayName(displayName);
			lsidSA = new NamedObjId(ce, NamedObjId.NAME);
			lsidSA.setExpression(li.getLsid());
			break;
		case LibIndex.TYPE_KARFOLDER:
			ce = new FolderEntityLibrary(parent, li.getName());
			ce.setDisplayName(displayName);
			break;
		case LibIndex.TYPE_OBJECT:
			ce = new ComponentEntity(parent, li.getName());
			ce.setDisplayName(displayName);
			lsidSA = new NamedObjId(ce, NamedObjId.NAME);
			lsidSA.setExpression(li.getLsid());
			// ce.setClassName(li.getAttributeValue(LibIndex.ATT_CLASSNAME));
			ComponentEntityConfig.addSVGIconTo(ce);
			notDraggableAttribute = new StringAttribute(ce, "_notDraggable");
			notDraggableAttribute.setExpression("true");
		}
		if (ce != null) {
			StringAttribute liidSA = new StringAttribute(ce,
					LibraryManager.LIID_LABEL);
			liidSA.setExpression("" + li.getLiid());
		}
		return ce;
	}

	private void copyAttributes(LibItem li, ComponentEntity ce) {
		Hashtable<String,String> attributes = li.getAttributes();
		for (String key : attributes.keySet()) {
			String value = attributes.get(key);
			try {
				StringAttribute sa = new StringAttribute(ce, key);
				sa.setExpression(value);
			}
			catch(IllegalActionException e) {
				e.printStackTrace();
			}
			catch(NameDuplicationException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Remove a KAR file from the Component Library and from the user's Disk.
	 * 
	 * @param File
	 *            aKarFile
	 */
	public void deleteKAR(File aKarFile) {

		KARCacheManager kcm = KARCacheManager.getInstance();
		if (!kcm.isCached(aKarFile)) {
			// this file is not in the cache.
			return;
		}

		KARFile karf;
		try {

			karf = new KARFile(aKarFile);
			File karFileLocation = karf.getFileLocation();
			karf.close(); // free up the file pointer

			// remember what was in the kar file before we remove it all
			Vector<KARCacheContent> entries = kcm
					.getKARCacheContents(karFileLocation);

			// Remove it from the Index
			LibItem karItem = getIndex().findKar(karFileLocation);
			// make sure KAR is in library
			if(karItem != null) {
    			getIndex().removeItem(karItem.getLiid());
    
    			// Remove it from the Library
    			ComponentEntity item = findTreeItem(karItem.getLiid());
    			if (item == null)
    				return;
    			item.setContainer(null);
			}

			// Remove it from the cache
			kcm.removeKARFromCache(karFileLocation);

			// verify that it is gone before doing the rest
			if (!kcm.isCached(karFileLocation)) {

				// Remove the kar file from the file system
				boolean success = karFileLocation.delete();
				if (success) {
					log.info(karFileLocation.toString() + " was deleted.");
				} else {
					System.out.println("Unable to delete "
							+ karFileLocation.toString());
				}

				// Update the Ontologies if no other KARs contain the entries
				Vector<KeplerLSID> lsidsToRemoveFromOntologies = new Vector<KeplerLSID>();
				CacheManager cache = CacheManager.getInstance();
				for (KARCacheContent entry : entries) {
					if (isDebugging)
						log.debug(entry.getLsid());
					if (cache.isContained(entry.getLsid())) {
						// Some other KAR has this object in it too
						// Don't remove it from the ontologies
					} else {
						// This LSID is no longer in the cache
						lsidsToRemoveFromOntologies.add(entry.getLsid());
					}
				}
				// remove ontology ComponentEntities that are no longer in the
				// cache
				for (KeplerLSID lsid : lsidsToRemoveFromOntologies) {
					// now we check each Liid to make sure there are no other
					// LSIDs
					// that exist for it

					// Remove it from the Index
					Vector<Integer> liidsRemovedFromIndex = getIndex()
							.removeItemsByLsid(lsid);

					// Remove them from the Library too
					for (Integer removedLiid : liidsRemovedFromIndex) {
						ComponentEntity ontItem = findTreeItem(removedLiid
								.intValue());
						if (ontItem != null) {
							ontItem.setContainer(null);
						} else {
							log.warn("ontItem not found for " + removedLiid);
						}
					}
				}
			}

			// update the JTrees
			refreshJTrees();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Look only at the children of the given parent to match the given LIID. Do
	 * not recurse the children here, use findTreeItemDeep() for that.
	 * 
	 * @param parent
	 * @param liid
	 * @return
	 */
	public ComponentEntity findTreeItem(CompositeEntity parent, int liid) {
		if (isDebugging)
			log.debug("findTreeItem(" + parent.getName() + "," + liid + ")");
		ComponentEntity ce = null;
		List<ComponentEntity> children = parent
				.entityList(ComponentEntity.class);
		for (ComponentEntity child : children) {
			int childLiid = LibraryManager.getLiidFor(child);
			if (isDebugging)
				log.debug("   " + childLiid + " " + child.getName());
			if (childLiid == liid) {
				ce = child;
				// break;
			}
		}
		return ce;
	}

	/**
	 * Look in the entire tree and return the ComponentEntity that matches the
	 * given LIID.
	 * 
	 * @param liid
	 * @return
	 */
	public ComponentEntity findTreeItem(int liid) {
		if (isDebugging)
			log.debug("findTreeItem(" + liid + ")");
		CompositeEntity root = (CompositeEntity) getTreeModel().getRoot();
		ComponentEntity ce = findTreeItemDeep(root, liid);
		if (ce == null) {
			log.debug("Unable to find item " + liid + " in Library Tree");
		}
		return ce;
	}

	/**
	 * Recurse all children under parent to match the given LIID.
	 * 
	 * @param parent
	 * @param liid
	 * @return
	 */
	public ComponentEntity findTreeItemDeep(CompositeEntity parent, int liid) {
		if (isDebugging)
			log
					.debug("findTreeItemDeep(" + parent.getName() + "," + liid
							+ ")");
		// check the children of this parent to see if any of them match the
		// liid
		ComponentEntity ce = findTreeItem(parent, liid);
		if (ce == null) {
			// recurse the children if we don't find it
			List<CompositeEntity> children = parent
					.entityList(CompositeEntity.class);
			for (CompositeEntity child : children) {
				ce = findTreeItemDeep(child, liid);
				if (ce != null) {
					break;
				}
			}
		}
		return ce;
	}

	/**
	 * generate the EntityTreeModel version of the library
	 * 
	 *@return Description of the Return Value
	 *@exception IllegalActionException
	 *                Description of the Exception
	 */
	private void generateTreeModel() {
		if (isDebugging) {
			log.debug("generateTreeModel()");
		}

		_libraryModel = new VisibleTreeModel(_actorLibrary);
	}
	
	private FilteredVisibleTreeModel generateTreeModel(File filterFile) {
		FilteredVisibleTreeModel treeModel = new FilteredVisibleTreeModel(_libraryModel, filterFile);
		return treeModel;
	}

	/**
	 * Get the CompositeEntity version of the library.
	 * 
	 * */
	public CompositeEntity getActorLibrary() {

		return _actorLibrary;
	}

	private void setActorLibrary(CompositeEntity ce) {
		_actorLibrary = ce;
		generateTreeModel();
	}

	/**
	 * @return LibIndex object that the manager is using
	 */
	public LibIndex getIndex() {
		return _libIndex;
	}

	/**
	 * Return the LIIDs for the given set of LSIDs.
	 * 
	 * @param lsids
	 * @return
	 */
	public Vector<Integer> getLiidsFor(Vector<KeplerLSID> lsids) {
		Vector<Integer> liids = new Vector<Integer>();

		for (KeplerLSID lsid : lsids) {
			try {
				String query = "SELECT LIID FROM "
						+ LibIndex.LIBRARY_LSIDS_TABLE_NAME + " WHERE LSID = '"
						+ lsid + "'";
				ResultSet rs = _stmt.executeQuery(query);
				if (rs == null)
					throw new SQLException("Query Failed: " + query);
				while (rs.next()) {
					int liid = rs.getInt(1);
					liids.add(new Integer(liid));
				}
				rs.close();
			} catch (SQLException sqle) {
				sqle.printStackTrace();
			}
		}
		return liids;
	}

	/**
	 * Return a populated LibItem for the given liid.
	 * 
	 * @param liid
	 * @return LibItem
	 */
	public LibItem getTreeItemIndexInformation(int liid) {
		LibItem li = null;
		try {
			li = getPopulatedLibItem(liid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return li;
	}

	private void clearActorLibrary() {
		CompositeEntity root = getActorLibrary();
		if (root != null) {
			List childen = root.entityList();
			for (Object child : childen) {
				if (child instanceof ComponentEntity) {
					try {
						((ComponentEntity) child).setContainer(null);
					} catch (IllegalActionException e) {
						e.printStackTrace();
					} catch (NameDuplicationException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	
	/**
	 * @return the TreeModel version of the library.
	 */
	public EntityTreeModel getTreeModel() {
		if (_libraryModel == null) {
			generateTreeModel();
		}
		return _libraryModel;
	}
	
	public EntityTreeModel getTreeModel(File filterFile) {
		return generateTreeModel(filterFile);
	}

	/**
	 * Refresh and redraw any JTrees that the LibraryManager is keeping track
	 * of.
	 * 
	 *@exception IllegalActionException
	 *                Description of the Exception
	 */
	public void refreshJTrees() throws IllegalActionException {
		if (isDebugging) {
			log.debug("refresh");
		}

		// Iterate over all the registered JTrees and reset their models.
		//Iterator<WeakReference> treeItt = _trees.iterator();
		
		int size = _trees.size();
		int i = 0;
		while (i < size){
			WeakReference<JTree> wf = _trees.elementAt(i);
			JTree ptree = (JTree)wf.get();
			if (ptree == null)
			{
				_trees.remove(wf);
				size--;
			}
			else {
				ptree.setModel(getTreeModel());

				// stop listening for expansion events since we are going to
				// expand the paths and do not want to modify the list
				ptree.removeTreeExpansionListener(this);
				
				// expand all the paths that were previously expanded
				final List<TreePath> expansions = _treeExpansionMap.get(ptree);
				for(TreePath path : expansions) {
				    //System.out.println("expanding " + path);
				    ptree.expandPath(path);
				}
				
				// start listening again for expansion events
				ptree.addTreeExpansionListener(this);
				
				/*
				EntityTreeModel etm = (EntityTreeModel) ptree.getModel();

				// Of course this would be easier if
				// ptolemy.vergil.tree.EntityTreeModel
				// had a reload method similar to DefaultTreeModel
				// that did this for us
				// TODO etm.reload()

				ArrayList<NamedObj> path = new ArrayList<NamedObj>();
				path.add(0, (NamedObj) etm.getRoot());
				TreePath tp = new TreePath(path);
				etm.valueForPathChanged(tp, etm.getRoot());
				*/
				i++;
			}
		}
	}

	/**
	 * Set the workspace for the actor library and regenerate
	 * 
	 * @param ws
	 */
	public void setActorLibraryWorkspace(Workspace ws) {
		if (isDebugging)
			log.debug("setActorLibraryWorkspace(" + ws.getName() + ")");
		_actorLibraryWorkspace = ws;
	}
	
	   /**
     * Populate this LibItem Object from the database using the given Library
     * Index ID.
     * 
     * @param liid
     * @throws SQLException
     */
    public LibItem getPopulatedLibItem(int liid) throws SQLException {
        
        LibItem li = new LibItem();
        _getPopulateInfoForLIIDPrepStmt.setInt(1, liid);
        ResultSet rs = _getPopulateInfoForLIIDPrepStmt.executeQuery();
        if (rs == null) throw new SQLException("Query Failed: " + _getPopulateInfoForLIIDPrepStmt);
        if (rs.next()) {

            li.setLiid(liid);

            int parent = rs.getInt(1);
            if (rs.wasNull()) {
                li.setParent(null);
            } else {
                li.setParent(new Integer(parent));
            }

            li.setLeft(rs.getInt(2));
            li.setRight(rs.getInt(3));
            li.setLevel(rs.getInt(4));

            String lsid = rs.getString(5);
            if (rs.wasNull()) {
                li.setLsid(null);
            } else {
                try {
                    li.setLsid(new KeplerLSID(lsid));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            li.setType(rs.getInt(6));
            li.setName(rs.getString(7));

            // double check there is only one row returned for this liid
            if (rs.next()) {
                throw new SQLException(LibIndex.LIBRARY_INDEX_TABLE_NAME
                        + " is corrupt"
                        + " more than one row returned for primary key "
                        + li.getLiid());
            }
        }
        rs.close();

        populateAttributes(li, liid);
        populateLsids(li, liid);
        
        return li;

    }

    /**
     * Populate this LibItem with the corresponding Attributes stored in the
     * LIBRARY_ATTRIBUTES table for the given LIID.
     * 
     * @param liid
     * @param stmt
     * @throws SQLException
     */
    private void populateAttributes(LibItem li, int liid)
            throws SQLException {

        _getNameValueForLIIDPrepStmt.setInt(1, liid);
        ResultSet rs = _getNameValueForLIIDPrepStmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (value == null) {
                    value = new String();
                }
                li.addAttribute(name, value);
            }
        }
        rs.close();

    }

    /**
     * Populate this LibItem with the corresponding LSID values stored in the
     * LIBRARY_LSIDS table for the given LIID.
     * 
     * @param liid
     * @param stmt
     * @throws SQLException
     */
    private void populateLsids(LibItem li, int liid) throws SQLException {

        _getLSIDForLIIDPrepStmt.setInt(1, liid);
        ResultSet rs = _getLSIDForLIIDPrepStmt.executeQuery();
        if (rs != null) {
            while (rs.next()) {
                String lsidStr = rs.getString(1);
                KeplerLSID lsid;
                try {
                    lsid = new KeplerLSID(lsidStr);
                    li.addLsid(lsid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        rs.close();

    }

    /** Called whenever an item in the tree is expanded. */
    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        final Object tree = event.getSource();
        final List<TreePath> expansions = _treeExpansionMap.get(tree);
        if(expansions != null) {
            //System.out.println("expanded " + event.getPath());
            expansions.add(event.getPath());
        }
    }


    /** Called whenever an item in the tree is collapsed. */
    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        final Object tree = event.getSource();
        final List<TreePath> expansions = _treeExpansionMap.get(tree);
        if(expansions != null) {
            //System.out.println("collapsed " + event.getPath());
            expansions.remove(event.getPath());
        }
    }


}
