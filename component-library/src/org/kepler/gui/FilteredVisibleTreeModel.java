/*
 * Copyright (c) 2010 The Regents of the University of California.
 * All rights reserved.
 *
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;
import ptolemy.moml.EntityLibrary;
import ptolemy.vergil.tree.VisibleTreeModel;

/**
 * Created by IntelliJ IDEA.
 * User: sean
 * Date: Jun 21, 2010
 * Time: 2:17:52 PM
 */

public class FilteredVisibleTreeModel extends VisibleTreeModel {
	
	public FilteredVisibleTreeModel(TreeModel model, File filterFile) {
		super(new CompositeEntity());
		this.model = model;
		this.filterFile = filterFile;
		if (this.filterFile == null) {
			//System.out.println("Filter file is null");
			return;
		}
		if (filterFile.exists()) {
			_whitelistManager = new WhitelistManager(filterFile);
		}
	}

	private Map<Object, TreeNode> nodesByObject = new HashMap<Object, TreeNode>();
		
	/**
	 * Returns a list of children of object (as defined by the TreeModel
	 * model) that are themselves or have as children concrete filtered
	 * actors.
	 * @param object
	 * @param model
	 * @return
	 */
	private List<Object> getPrunedChildren(Object object, TreeModel model) {
		
		if (nodesByObject.containsKey(object)) {
			TreeNode node = nodesByObject.get(object);
			return _getFilteredChildren(node);
		}

		OpaqueTreeNode node = OpaqueTreeNode.getNode(object);
		
		List<Object> children = _getAllChildren(object, model);
		
		if (children.isEmpty()) {
			nodesByObject.put(object, node);
			return Collections.emptyList();
		}
		
		// Iterate over all children
		// Filter out all children that have no non-EntityLibrary children
		// Remember to record that information into the cache
		
		List<Object> prunedItems = new ArrayList<Object>();
		
		for (Object child : children) {
			if (child instanceof EntityLibrary) {
				List<Object> grandchildren = getPrunedChildren(child, model);
				if (!grandchildren.isEmpty()) {
					node.add(OpaqueTreeNode.getNode(child));
					prunedItems.add(child);
				}
			}
			else if (passesFilter(child)) {
				node.add(OpaqueTreeNode.getNode(child));
				prunedItems.add(child);
			}
			nodesByObject.put(object, node);
		}

		return prunedItems;
	}
		
	private boolean passesFilter(Object object) {
		
		NamedObj no = (NamedObj) object;
		return checkAgainstWhitelist(no);
	}

	private boolean checkAgainstWhitelist(NamedObj no) {
		
		boolean success = _whitelistManager.checkLSID(((StringAttribute) no.getAttribute("entityId")).getExpression());
		if (success) return true;
		success = _whitelistManager.checkClassname(no.getClassName());
		return success;
	}
	
	private List<Object> _getAllChildren(Object node, TreeModel model) {
		List<Object> list = new ArrayList<Object>();
		for (int i = 0; i < model.getChildCount(node); i++) {
			list.add(model.getChild(node, i));
		}
		return list;
	}
	
	private List<Object> _getFilteredChildren(TreeNode node) {
		// Recover children from the cache.
		List<Object> list = new ArrayList<Object>();
		Enumeration children = node.children();
		while (children.hasMoreElements()) {
			list.add(((DefaultMutableTreeNode) children.nextElement()).getUserObject());
		}
		return list;
	}

	public Object getChild(Object parent, int index) {

		if (filterFile == null) {			
			return model.getChild(parent, index);
		}

		List<Object> children = getPrunedChildren(parent, model);
		return children.get(index);
	}

	public int getChildCount(Object parent) {
		if (filterFile == null) {			
			return model.getChildCount(parent);
		}

		List<Object> children = getPrunedChildren(parent, model);
		return children.size();
	}

	public int getIndexOfChild(Object parent, Object child) {
		if (filterFile == null) {			
			return model.getIndexOfChild(parent, child);
		}

		List<Object> children = getPrunedChildren(parent, model);
		return children.indexOf(child);
	}
		
	@Override
	public Object getRoot() {
		return model.getRoot();
	}

	@Override
	public boolean isLeaf(Object node) {
		return model.isLeaf(node);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		model.valueForPathChanged(path, newValue);
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		model.addTreeModelListener(l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		model.removeTreeModelListener(l);
	}

	private WhitelistManager _whitelistManager = null;

	private static class OpaqueTreeNode extends DefaultMutableTreeNode {
		private OpaqueTreeNode(Object object, boolean allowsChildren) {
			super(object, allowsChildren);
		}
		
		public static OpaqueTreeNode getNode(Object object) {
			return new OpaqueTreeNode(object, object instanceof EntityLibrary);
		}
	}
	
	private File filterFile;
	private TreeModel model;

	private class WhitelistManager {
		public WhitelistManager(File filterFile) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(filterFile));
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.length() == 0) continue;
					if (line.startsWith("urn:")) {
						lsids.add(line);
					}
					else {
						classes.add(line);
					}
				}
			}
			catch(IOException ex) {
				ex.printStackTrace();
			}
			finally {
				if (br != null) {
					try {
						br.close();
					}
					catch(IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		}

		public boolean checkLSID(String s) {
			return lsids.contains(s);
		}

		public boolean checkClassname(String s) {
			return classes.contains(s);
		}

		private List<String> lsids = new ArrayList<String>();
		private List<String> classes = new ArrayList<String>();
	}
}
