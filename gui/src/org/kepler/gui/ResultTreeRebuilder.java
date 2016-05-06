/*
 * Copyright (c) 2009-2010 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2013-01-23 14:17:38 -0800 (Wed, 23 Jan 2013) $' 
 * '$Revision: 31362 $'
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

package org.kepler.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kepler.gui.popups.LibraryPopupListener;
import org.kepler.moml.DownloadableKAREntityLibrary;
import org.kepler.moml.FolderEntityLibrary;
import org.kepler.moml.KAREntityLibrary;
import org.kepler.moml.KARErrorEntityLibrary;
import org.kepler.moml.OntologyEntityLibrary;
import org.kepler.moml.RemoteKARErrorEntityLibrary;
import org.kepler.moml.RemoteRepositoryEntityLibrary;
import org.kepler.moml.SearchEntityLibrary;
import org.kepler.objectmanager.library.LibraryManager;
import org.kepler.util.StaticResources;

import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.EntityLibrary;
import ptolemy.vergil.tree.PTree;
import ptolemy.vergil.tree.VisibleTreeModel;

/**
 * This class builds the search results by traversing the tree and trimming any
 * sub-nodes that do not have a result in them. This leaves a minimum tree with
 * only the search results present.
 * 
 *@author Chad Berkley
 *@author Shawn Bowers 
 *@author Aaron Schultz (... last editor ...)
 */
public class ResultTreeRebuilder extends LibrarySearchResultPane {
	private static final Log log = LogFactory.getLog(ResultTreeRebuilder.class
			.getName());
	private static final boolean isDebugging = log.isDebugEnabled();

	private VisibleTreeModel trimmedLibrary;
	private PTree resultsTree;
	private Workspace workspace;
	private JPanel resultCounterPane;
	private JLabel resultCounterLabel;
	private TreeSelectionListener treeSingleSelectionlistener = null;

	/**
	 * the constructor passes in the library to highlight the results in and the
	 * results to highlight. if results is null, the tree is built fully
	 * collapsed with no highlights.
	 * 
	 *@param library
	 *            the library to highlight the results in
	 *@param results
	 *            the results to highlight
	 *@exception IllegalActionException
	 *                Description of the Exception
	 */
	public ResultTreeRebuilder(PTree library, LibrarySearchResults results)
			throws IllegalActionException {
		super(library, results);
		this.workspace = ((CompositeEntity) library.getModel().getRoot())
				.workspace();
	}

	/** Update the tree to display only a specific root. */
	public void update(EntityLibrary root) throws IllegalActionException {
		if (isDebugging) {
			log.debug("update(" + root.getName() + " "
					+ root.getClass().getName() + ")");
		}
		this.removeAll();

		trimmedLibrary = new VisibleTreeModel(root);
		
		AnnotatedPTree rTree = new AnnotatedPTree(trimmedLibrary, this);
		MouseListener mListener = new LibraryPopupListener(rTree);
		rTree.setMouseListener(mListener);
		if(treeSingleSelectionlistener != null)
		{
		  rTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		  rTree.addTreeSelectionListener(treeSingleSelectionlistener);
		}
		rTree.initAnotatedPTree();
		resultsTree = rTree;

		JScrollPane newpane = new JScrollPane(resultsTree);
		newpane.setPreferredSize(new Dimension(200, 200));
		this.add(newpane, BorderLayout.CENTER);

		// add the search results counter stuff
		resultCounterPane = new JPanel();
		resultCounterPane.setBackground(TabManager.BGCOLOR);
		resultCounterPane.setLayout(new BorderLayout());
		resultCounterLabel = new JLabel("");
		resultCounterPane.removeAll();
		resultCounterPane.add(resultCounterLabel, BorderLayout.CENTER);
		this.add(resultCounterPane, BorderLayout.SOUTH);

		this.repaint();
		this.validate();

	}
	
	
	/**
	 * Set the tree selection listener (single selection mode will be set as well)
	 * @param listener
	 *              the listener will be set
	 */
	public void setSingleTreeSelectionListener(TreeSelectionListener listener)
	{
	  this.treeSingleSelectionlistener = listener;
	}
	
	/**
	 * Get the result tree
	 * @return
	 */
	public JTree getResultTree()
	{
	  return resultsTree;
	}

	/**
	 * this method allows the search results to be updated in the panel
	 * 
	 *@param results
	 *            the results to update to
	 *@exception IllegalActionException
	 *                Description of the Exception
	 */
	public void update(LibrarySearchResults results)
			throws IllegalActionException {
		if (isDebugging) {
			log.debug("update(" + results + ")");
		}
		this.results = results;
		int resultcount;
		if (results == null || results.size() == 0) {
			resultcount = 0;
		} else {
			resultcount = results.size();
		}

		this.removeAll();

		// add the results if there are any
		if (resultcount > 0) {
			// add the results tree.
			EntityLibrary newRoot = new SearchEntityLibrary(workspace);

			try {
				newRoot.setName(
						StaticResources.getDisplayString("components.searchResults", "Search Results"));
			} catch (IllegalActionException iae) {
				throw iae;
			} catch (NameDuplicationException nde) {
				throw new IllegalActionException("name duplication exception: "
						+ nde);
			}
			buildResultTree(newRoot);
			trimmedLibrary = new VisibleTreeModel(newRoot);
			
			AnnotatedPTree rTree = new AnnotatedPTree(trimmedLibrary, this);
			MouseListener mListener = new LibraryPopupListener(rTree);
			rTree.setMouseListener(mListener);
			if(treeSingleSelectionlistener != null)
			{
			    rTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			    rTree.addTreeSelectionListener(treeSingleSelectionlistener);
			}
			rTree.initAnotatedPTree();
			resultsTree = rTree;
			rTree.setShowsRootHandles(false);
			
			JScrollPane newpane = new JScrollPane(resultsTree);
			newpane.setPreferredSize(new Dimension(200, 200));
			this.add(newpane, BorderLayout.CENTER);
			expandAll(resultsTree);
		} else {
			// if there are no results, just add the library back
			if (library.getModel() != null) {
				library.setModel(new NoRemoteComponentsTreeModel(library.getModel()));
			}
			this.add(new JScrollPane(library), BorderLayout.CENTER);
		}

		// add the search results counter stuff
		resultCounterPane = new JPanel();
		resultCounterPane.setBackground(TabManager.BGCOLOR);
		resultCounterPane.setLayout(new BorderLayout());
		resultCounterLabel = new JLabel(
				resultcount 
				+ " " 
				+ StaticResources.getDisplayString("components.resultsFound", "results found."));
		resultCounterPane.removeAll();
		resultCounterPane.add(resultCounterLabel, BorderLayout.CENTER);
		this.add(resultCounterPane, BorderLayout.SOUTH);

		this.repaint();
		this.validate();
	}

	/**
	 * clears the tree of any items contained by newRoot
	 * 
	 *@exception IllegalActionException
	 *                Description of the Exception
	 *
	public void clearTree() throws IllegalActionException {
		if (newRoot != null && newRoot.entityList() != null) {
			List children = newRoot.entityList();
			for (Object child : children ) {
				try {
					if (child instanceof ComponentEntity) {
						ComponentEntity childCE = (ComponentEntity) child;
						childCE.setContainer(null);
					} else if (child instanceof Attribute) {
						Attribute childAtt = (Attribute) child;
						childAtt.setContainer(null);
					} else {
						log.error("Unhandled library type: " + child.getClass());
					}
				} catch (Exception e) {
					// do nothing...just go onto the next node
				}
			}
		}
	}*/

	/*
	 * build the result tree
	 */
	/**
	 * Description of the Method
	 * 
	 *@exception IllegalActionException
	 *                Description of the Exception
	 */
	private void buildResultTree(EntityLibrary newRoot) throws IllegalActionException {
		if (isDebugging)
			log.debug("buildResultTree()");

		// iterate over each treepath in results
		for (int i = 0; i < results.size(); i++) {
			
			try {
				
				TreePath currentPath = results.getTreePath(i);
				if (isDebugging)
					log.debug(currentPath);
				
				EntityLibrary treeCurrent = newRoot;
				for (int j = 1; j < currentPath.getPathCount(); j++) {

					NamedObj pathCurrent = (NamedObj) currentPath.getPathComponent(j);
					
					if (pathCurrent instanceof EntityLibrary) {
					
						List<EntityLibrary> children = treeCurrent.entityList(EntityLibrary.class);
						
						boolean alreadyThere = false;
						for (EntityLibrary child : children) {
							if (isDebugging) log.debug(child.getName());
							if (child.getName().equals(pathCurrent.getName())) {
								// this EntityLibrary is already there
								treeCurrent = child;
								alreadyThere = true;
								break;
							}
						}
						if (!alreadyThere) {
							// create it
							EntityLibrary newEntity = copyEntityLibrary((EntityLibrary) pathCurrent);
							setContainer(treeCurrent,newEntity);
							treeCurrent = newEntity;
						}
						
					} else {
						List<NamedObj> children = treeCurrent.entityList(NamedObj.class);
						
						boolean alreadyThere = false;
						for (NamedObj child : children) {
							if (child.getName().equals(pathCurrent.getName())) {
								// this NamedObj is already there
								alreadyThere = true;
								break;
							}
						}
						if (!alreadyThere) {
							// create it
							NamedObj newEntity = cloneEntity((NamedObj)pathCurrent);
							setContainer(treeCurrent,newEntity);
							// Leaf node, all done
							break;
						}
					}
				}
				
			} catch (IllegalActionException iae) {
				throw new IllegalActionException(
						"cannot build search result tree: " + iae);
			} catch (NameDuplicationException nde) {
				log.error("EXCEPTION CAUGHT: " + nde.getMessage());
			} catch (Exception e) {
				log.error("EXCEPTION CAUGHT: " + e.getMessage());
			}
		} // end for loop
	}

	/**
	 * puts entity in container
	 * 
	 *@param container
	 *            The new container value
	 *@param entity
	 *            The new container value
	 *@exception NameDuplicationException
	 *                Description of the Exception
	 *@exception IllegalActionException
	 *                Description of the Exception
	 */
	private static void setContainer(CompositeEntity container, NamedObj entity)
			throws NameDuplicationException, IllegalActionException {
		if (entity instanceof Attribute) {
			((Attribute) entity).setContainer(container);
		} else if (entity instanceof ComponentEntity) {
			((ComponentEntity) entity).setContainer(container);
		}
	}

	/**
	 * 
	 * 
	 *@param entity
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 *@exception IllegalActionException
	 *                Description of the Exception
	 */
	private EntityLibrary copyEntityLibrary(EntityLibrary entity)
			throws IllegalActionException {
		if (entity == null)
			return null;
		try {
			if (isDebugging)
				log.debug("copyCompositeEntity(" + entity.getName() + ")");
			if (isDebugging)
				log.debug(entity.getClass().getName());
			EntityLibrary el = null;

			if (entity instanceof OntologyEntityLibrary) {
				el = new OntologyEntityLibrary(new CompositeEntity(
						this.workspace), entity.getName());
			} else if (entity instanceof KAREntityLibrary) {
				el = new KAREntityLibrary(new CompositeEntity(
						this.workspace), entity.getName());
			} else if (entity instanceof FolderEntityLibrary) {
				el = new FolderEntityLibrary(new CompositeEntity(
						this.workspace), entity.getName());
			} else if (entity instanceof DownloadableKAREntityLibrary) {
				el = new DownloadableKAREntityLibrary(new CompositeEntity(
						this.workspace), (DownloadableKAREntityLibrary) entity);
			} else if (entity instanceof RemoteRepositoryEntityLibrary) {
				el = new RemoteRepositoryEntityLibrary(new CompositeEntity(
						this.workspace), entity.getName());
			} else if (entity instanceof KARErrorEntityLibrary) {
				el = new KARErrorEntityLibrary(new CompositeEntity(
						this.workspace), entity.getName());
			} else if (entity instanceof RemoteKARErrorEntityLibrary) {
				el = new RemoteKARErrorEntityLibrary(new CompositeEntity(
						this.workspace), entity.getName());
				((RemoteKARErrorEntityLibrary) el).setKarXml(((RemoteKARErrorEntityLibrary) entity).getKarXml());
			} else {
				el = new EntityLibrary(new CompositeEntity(this.workspace),
						entity.getName());
			}
			if (el == null) {
				log.warn("Unrecognized Composite Entity");
			} else {
			    
			    // set the display name to be the source's display name.
			    // the display name may be different than the name returned
			    // by getName(), e.g., the name may end in ",kar" but the
			    // display name ends in ".kar".
			    el.setDisplayName(entity.getDisplayName());
			    
				int liid = LibraryManager.getLiidFor(entity);
				StringAttribute liidSA = new StringAttribute(el,
						LibraryManager.LIID_LABEL);
				liidSA.setExpression("" + liid);
				if (isDebugging)
					log.debug(el.getClass().getName());
			}
			return el;
		} catch (IllegalActionException iae) {
			throw new IllegalActionException("cannot copy composite entity: "
					+ iae);
		} catch (NameDuplicationException nde) {
			throw new IllegalActionException(
					"cannot set container because the name "
							+ "already exists: " + nde);
		}
	}

	/**
	 * clone the entity into the new workspace.
	 * 
	 *@param entity
	 *            Description of the Parameter
	 *@return Description of the Return Value
	 *@exception IllegalActionException
	 *                Description of the Exception
	 */
	// private ComponentEntity cloneEntity(ComponentEntity entity) throws
	// IllegalActionException {
	private NamedObj cloneEntity(NamedObj entity) throws IllegalActionException {
		try {
			return (NamedObj) entity.clone(this.workspace);
		} catch (java.lang.CloneNotSupportedException cnse) {
			throw new IllegalActionException("clone not supported: " + cnse);
		}
	}

	/**
	 * A factory that creates the searcher to search the library
	 * 
	 *@author berkley
	 *@since February 17, 2005
	 */
	public static class Factory extends LibrarySearchResultPaneFactory {
		/**
		 * Create an factory with the given name and container.
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
		 * creates a ResultTreeBuilder and returns it.
		 * 
		 *@param library
		 *            Description of the Parameter
		 *@param results
		 *            Description of the Parameter
		 *@return A new LibraryPane that displays the library
		 *@exception IllegalActionException
		 *                Description of the Exception
		 */
		public LibrarySearchResultPane createLibrarySearchResultPane(
				PTree library, LibrarySearchResults results)
				throws IllegalActionException {
			return new ResultTreeRebuilder(library, results);
		}
	}

	private class NoRemoteComponentsTreeModel implements TreeModel {
		
		private boolean isRemoteComponentsNode(Object node) {
			// TODO: A more robust test would be good here. Probably just make
			// a subclass of EntityLibrary used only for the
			// "Remote Components" node and then test with instanceof.
			return "ptolemy.moml.EntityLibrary {.kepler actor library.Remote Components}".equals(node.toString());
		}
		
		public NoRemoteComponentsTreeModel(TreeModel model) {
			this.model = model;
			for (int i = 0; i < model.getChildCount(model.getRoot()); i++) {
				Object node = model.getChild(model.getRoot(), i);
				if (isRemoteComponentsNode(node)) {
					this.remoteComponentsNode = node;
					this.remoteComponentsNodeIndex = i;
					
				}
			}
		}
		
		public Object getRoot() {
			return model.getRoot();
		}

		public Object getChild(Object parent, int index) {
			return model.getChild(parent, index);
		}

		public int getChildCount(Object parent) {
			if (parent == getRoot() && remoteComponentsNode != null) {
				// Don't include the Remote Components
				return model.getChildCount(parent) - 1;
			}
			else {
				return model.getChildCount(parent);
			}
		}

		public boolean isLeaf(Object node) {
			return model.isLeaf(node);
		}

		public void valueForPathChanged(TreePath path, Object newValue) {
			model.valueForPathChanged(path, newValue);
		}

		public int getIndexOfChild(Object parent, Object child) {
			if (parent != getRoot() || remoteComponentsNode == null) {
				return model.getIndexOfChild(parent, child);
			}
			// No index translation required if index < remote component index
			int index = model.getIndexOfChild(parent, child);
			if (index > this.remoteComponentsNodeIndex) {
				return index - 1;
			}
			else if (index == this.remoteComponentsNodeIndex) {
				System.out.println("Shouldn't happen!");
				return 0;
			}
			else {
				return index;
			}
		}

		public void addTreeModelListener(TreeModelListener l) {
			model.addTreeModelListener(l);
		}

		public void removeTreeModelListener(TreeModelListener l) {
			model.removeTreeModelListener(l);
		}

		private TreeModel model;
		private Object remoteComponentsNode = null;
		private int remoteComponentsNodeIndex = -1;
	}
}