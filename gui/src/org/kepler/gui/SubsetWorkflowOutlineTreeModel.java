/*
 * Copyright (c) 2012 The Regents of the University of California.
 * All rights reserved.
 *
 * '$Author: crawl $'
 * '$Date: 2012-08-30 11:51:48 -0700 (Thu, 30 Aug 2012) $' 
 * '$Revision: 30577 $'
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

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.NamedObj;

/** A tree model that can display a subset of a workflow. Use addToSubset()
 *  to select which NamedObjs are in the tree, and then call showSubset()
 *  to activate.
 * 
 *  @author Daniel Crawl
 *  @version $Id: SubsetWorkflowOutlineTreeModel.java 30577 2012-08-30 18:51:48Z crawl $
 */
public class SubsetWorkflowOutlineTreeModel extends WorkflowOutlineTreeModel {

    /** Create a new SubsetWorkflowOutlineTreeModel with the specified root. */
    public SubsetWorkflowOutlineTreeModel(CompositeEntity root) {
        super(root);
    }

    /** Add an object to the subset tree. NOTE: the subset tree is
     *  not activated until showSubset() is called.
     */
    public void addToSubset(NamedObj namedObj) {

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(namedObj);
        _nodeMap.put(namedObj, node);
        
        NamedObj parentNamedObj = namedObj.getContainer();
        while(parentNamedObj != null) {
            
            // see if container is already in tree
            boolean parentInTree = true;
            DefaultMutableTreeNode parentNode = _nodeMap.get(parentNamedObj);
            if(parentNode == null) {
                parentNode = new DefaultMutableTreeNode(parentNamedObj);
                _nodeMap.put(parentNamedObj, parentNode);
                parentInTree = false;
            }
            
            // set parent/child relationships
            parentNode.insert(node, parentNode.getChildCount());
            //System.out.println(parentNamedObj.getFullName() + " --> " + namedObj.getFullName());
            
            if(parentInTree) {
                break;
            }
            parentNamedObj = parentNamedObj.getContainer();
            node = parentNode;
        }
    }

    /** Get the child of the given parent at the given index. If the child does
     *  not exist, then return null.
     */
    @Override
    public Object getChild(Object parent, int index) {
        
        if(!_showSubset) {
            return super.getChild(parent, index);
        }
        
        TreeNode node = _nodeMap.get(parent);
        if(node == null) {
            return null;
        }
        //System.out.println(((NamedObj) parent).getFullName() + " child " + index + " is " +
                //((DefaultMutableTreeNode) node.getChildAt(index)).getUserObject());
        return ((DefaultMutableTreeNode) node.getChildAt(index)).getUserObject();
    }

    /** Returns the number of children of the given parent. */ 
    @Override
    public int getChildCount(Object parent) {
        
        if(!_showSubset) {
            return super.getChildCount(parent);     
        }
        
        TreeNode node = _nodeMap.get(parent);
        if(node == null) {
            return 0;
        }
        //System.out.println("child count " + ((NamedObj) parent).getFullName() + " is " + node.getChildCount());
        return node.getChildCount();
    }
    
    /** Return the index of the given child within the given parent. If the
     *  parent is not contained in the child, return -1.
     */
    @Override
    public int getIndexOfChild(Object parent, Object child) {
        
        if(!_showSubset) {
            super.getIndexOfChild(parent, child);
        }
        
        TreeNode parentNode = _nodeMap.get(parent);
        TreeNode childNode = _nodeMap.get(child);
        if(parentNode == null || childNode == null) {
            return -1;
        }
        return parentNode.getIndex(childNode);
    }

    /** Returns true if the object is a leaf node. */
    @Override
    public boolean isLeaf(Object object) {

        if(!_showSubset) {
            return super.isLeaf(object);
        }
        
        TreeNode node = _nodeMap.get(object);
        if(node == null) {
            return true;
        }
        return node.isLeaf();
    }

    /** Show everything in the tree. */
    public void showAll() {
        _nodeMap.clear();
        _showSubset = false;
    }
    
    /** Show only the nodes containing objects that were called with addToSubset().
     *  Do not call this method until everything to be displayed has been added
     *  with addToSubset().
     */
    public void showSubset() {
        _showSubset = true;
    }    
    
    /** A mapping from user object (NamedObj) to a tree node. */
    private Map<Object,DefaultMutableTreeNode> _nodeMap = new HashMap<Object,DefaultMutableTreeNode>();
    
    /** If true, show the subset tree. */
    private boolean _showSubset = false;
}
